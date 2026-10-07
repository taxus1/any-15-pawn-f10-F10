package com.somepro.infrastructure.persistence.ledger;

import com.github.pagehelper.PageHelper;
import com.somepro.domain.collateral.model.Category;
import com.somepro.domain.ledger.model.LedgerQuery;
import com.somepro.domain.ledger.model.MonthlyCategoryLedger;
import com.somepro.domain.ledger.repository.MonthlyLedgerRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 月度类别台账仓储适配器（基础设施层）：只读汇总，经 blocking(...) 桥接进响应式链路。
 *
 * 汇总 SQL 一次点齐四档数并补零行；本适配器只负责：类别清单（与领域枚举 Category 同源，
 * 不用库里出现过的类别，避免某类别整月缺席时格子消失）、PageHelper 分页织入/清理、
 * 投影行到领域台账行的装配。
 */
@Repository
public class MonthlyLedgerRepositoryImpl implements MonthlyLedgerRepository {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MonthlyLedgerMapper monthlyLedgerMapper;

    public MonthlyLedgerRepositoryImpl(MonthlyLedgerMapper monthlyLedgerMapper) {
        this.monthlyLedgerMapper = monthlyLedgerMapper;
    }

    @Override
    public Mono<PageResult<MonthlyCategoryLedger>> page(int pageNum, int pageSize, LedgerQuery query) {
        // 类别格子与领域枚举同源：筛选时只给这一个，不筛时五个全给。
        List<String> categories = query.category() == null
                ? Arrays.stream(Category.values()).map(Category::code).collect(Collectors.toList())
                : List.of(query.category().code());
        String todayMonth = query.today().format(MONTH_FORMATTER);

        return this.<PageResult<MonthlyCategoryLedger>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<MonthlyLedgerRow> rows = monthlyLedgerMapper.selectLedgerPage(
                        query.month(), todayMonth, categories);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<MonthlyCategoryLedger> content = rows.stream()
                        .map(MonthlyLedgerRepositoryImpl::toDomain)
                        .collect(Collectors.toList());
                // 库里一条业务都没有时：SQL 仍会铺出「当前月 × 类别」零值行；
                // 极端情况下连格子都没有就交空页，同样不报错。
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                // PageHelper 靠 ThreadLocal 传分页参数，必须清，避免污染线程池下一次调用
                PageHelper.clearPage();
            }
        });
    }

    private static MonthlyCategoryLedger toDomain(MonthlyLedgerRow row) {
        return new MonthlyCategoryLedger(
                row.getMonth(),
                row.getCategory(),
                nz(row.getNewTicketCount()),
                nz(row.getNewPawnAmount()),
                nz(row.getRenewCount()),
                nz(row.getRedeemCount()),
                nz(row.getRedeemPrincipalAmount()),
                nz(row.getRedeemFeeAmount()),
                nz(row.getForfeitCount()));
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 阻塞 DB 调用 → 响应式链路桥接器：先从 Reactor Context 取操作人，再切到 boundedElastic
     * （汇总只读不写审计，取操作人仅为与其它只读端口保持同一套桥接约定）。
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
