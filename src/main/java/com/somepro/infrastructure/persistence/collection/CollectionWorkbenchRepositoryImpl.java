package com.somepro.infrastructure.persistence.collection;

import com.github.pagehelper.PageHelper;
import com.somepro.domain.collection.model.CollectionBucket;
import com.somepro.domain.collection.model.CollectionQuery;
import com.somepro.domain.collection.model.CollectionWorkItem;
import com.somepro.domain.collection.repository.CollectionWorkbenchRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.ticket.model.PawnTicket;
import com.somepro.domain.ticket.model.TicketStatus;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.collection.po.CollectionWorkRow;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 催收工作台仓储适配器（基础设施层）：只读连表查询，经 blocking(...) 桥接进响应式链路。
 *
 * 本适配器只点行、不写库：不生成赎当 / 续当单、不推进到期日期、不翻票与当物状态。
 * 两笔给客户的试算在领域装配（{@link CollectionWorkItem#assemble}）里走赎当 / 续当
 * 办理的同一套工厂规则算出，算完即弃。
 *
 * 档位到 SQL 开关的映射：
 * - bucket=null（两拨都要）：overdue=1、expiring=1；
 * - OVERDUE：overdue=1、expiring=0；
 * - EXPIRING：overdue=0、expiring=1。
 */
@Repository
public class CollectionWorkbenchRepositoryImpl implements CollectionWorkbenchRepository {

    private final CollectionWorkbenchMapper workbenchMapper;

    public CollectionWorkbenchRepositoryImpl(CollectionWorkbenchMapper workbenchMapper) {
        this.workbenchMapper = workbenchMapper;
    }

    @Override
    public Mono<PageResult<CollectionWorkItem>> page(int pageNum, int pageSize, CollectionQuery query) {
        int overdue = query.bucket() == null || query.bucket() == CollectionBucket.OVERDUE ? 1 : 0;
        int expiring = query.bucket() == null || query.bucket() == CollectionBucket.EXPIRING ? 1 : 0;

        return this.<PageResult<CollectionWorkItem>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<CollectionWorkRow> rows = workbenchMapper.selectWorkbenchPage(
                        query.today(), query.expiringDeadline(), overdue, expiring);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<CollectionWorkItem> content = rows.stream()
                        .map(row -> CollectionWorkItem.assemble(
                                toTicket(row), query.today(),
                                row.getPawnerName(), row.getPawnerPhone(),
                                row.getCollateralName(),
                                row.getRenewCount() == null ? 0 : row.getRenewCount()))
                        .collect(Collectors.toList());
                // 库里一条都没有时 content 为空、total=0：交回空页，不报错
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                // PageHelper 靠 ThreadLocal 传分页参数，必须清，避免污染线程池下一次调用
                PageHelper.clearPage();
            }
        });
    }

    /**
     * 投影行 → 当票领域对象：只填催收试算用得到的票面列。
     * 状态固定按 ACTIVE 填（SQL 已把死条件 status='ACTIVE' 钉在主表上），
     * 赎当 / 续当工厂方法都要先验「只有在当的票才办得动」，这里给它一个与库一致的状态。
     */
    private static PawnTicket toTicket(CollectionWorkRow row) {
        PawnTicket ticket = new PawnTicket();
        ticket.setId(row.getTicketId());
        ticket.setTicketNo(row.getTicketNo());
        ticket.setPawnerId(row.getPawnerId());
        ticket.setCollateralId(row.getCollateralId());
        ticket.setPawnAmount(row.getPawnAmount());
        ticket.setMonthlyRate(row.getMonthlyRate());
        ticket.setServiceRate(row.getServiceRate());
        ticket.setStartDate(row.getStartDate());
        ticket.setDueDate(row.getDueDate());
        ticket.setTermMonths(row.getTermMonths());
        ticket.setStatus(TicketStatus.ACTIVE);
        return ticket;
    }

    /**
     * 阻塞 DB 调用 → 响应式链路桥接器：先从 Reactor Context 取操作人，再切到 boundedElastic
     * （查询不写审计，取操作人仅为与其它只读端口保持同一套桥接约定）。
     */
    private <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
