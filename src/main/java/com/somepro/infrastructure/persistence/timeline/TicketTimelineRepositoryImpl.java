package com.somepro.infrastructure.persistence.timeline;

import com.somepro.domain.collateral.model.Category;
import com.somepro.domain.forfeit.model.DisposeMethod;
import com.somepro.domain.ticket.model.TicketStatus;
import com.somepro.domain.timeline.model.CollateralRegisterSegment;
import com.somepro.domain.timeline.model.ForfeitSegment;
import com.somepro.domain.timeline.model.PawnerRegisterSegment;
import com.somepro.domain.timeline.model.RedeemSegment;
import com.somepro.domain.timeline.model.RenewSegment;
import com.somepro.domain.timeline.model.TicketIssueSegment;
import com.somepro.domain.timeline.model.TicketTimeline;
import com.somepro.domain.timeline.model.TimelineSegment;
import com.somepro.domain.timeline.repository.TicketTimelineRepository;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 当票时间线仓储适配器（基础设施层）：只读倒线，经 blocking(...) 桥接进响应式链路。
 *
 * 职责只到「按表把各段原值点齐、喂给领域装配」：票头点空（号不存在 / 票已销）直接交 empty；
 * 点到票就把建档 / 登记 / 开票三段连同全部有效续当 / 赎当 / 绝当事件一并装出，
 * 排序与自洽核对由 {@link TicketTimeline#assemble} 在领域层完成。
 *
 * 各段金额 / 日期 / 费率全部照投影行原值透传，本类不做任何重算。
 */
@Repository
public class TicketTimelineRepositoryImpl implements TicketTimelineRepository {

    private final TicketTimelineMapper timelineMapper;

    public TicketTimelineRepositoryImpl(TicketTimelineMapper timelineMapper) {
        this.timelineMapper = timelineMapper;
    }

    @Override
    public Mono<TicketTimeline> findByTicketNo(String ticketNo) {
        if (ticketNo == null || ticketNo.isBlank()) {
            return Mono.empty();
        }
        String no = ticketNo.trim();
        return this.<TicketTimeline>blocking(() -> {
            TimelineHeadRow head = timelineMapper.selectHeadByTicketNo(no);
            // 号压根不存在、或当票本身已销：空结果，不报错。
            if (head == null) {
                return null;
            }

            List<TimelineSegment> segments = new ArrayList<>();

            // 第一段：当户建档。LEFT JOIN 档案缺失（极端脏数据）时不带这段，但不吞掉整条线。
            if (head.getPawnerNo() != null) {
                segments.add(new PawnerRegisterSegment(
                        head.getPawnerId(),
                        head.getPawnerNo(),
                        head.getPawnerName(),
                        head.getPawnerCreateTime(),
                        Integer.valueOf(1).equals(head.getPawnerDelFlag())));
            }

            // 第二段：当物登记。档案已销也照带（deleted=true），历史一段不抹。
            if (head.getItemNo() != null) {
                String categoryCode = head.getCollateralCategory();
                segments.add(new CollateralRegisterSegment(
                        head.getCollateralId(),
                        head.getItemNo(),
                        categoryCode,
                        labelOfCategory(categoryCode),
                        head.getItemName(),
                        head.getBrand(),
                        head.getItemAppraisedValue(),
                        head.getCollateralCreateTime(),
                        Integer.valueOf(1).equals(head.getCollateralDelFlag())));
            }

            // 第三段：当票开立。票面快照原样带出。
            TicketStatus status = parseStatusOrNull(head.getStatus());
            segments.add(new TicketIssueSegment(
                    head.getTicketId(),
                    head.getTicketNo(),
                    head.getPawnerId(),
                    head.getCollateralId(),
                    head.getCategory(),
                    labelOfCategory(head.getCategory()),
                    head.getPawnAmount(),
                    head.getAppraisedValue(),
                    head.getMonthlyRate(),
                    head.getServiceRate(),
                    head.getStartDate(),
                    head.getDueDate(),
                    head.getTermMonths(),
                    head.getStatus(),
                    status == null ? null : status.label(),
                    head.getTicketCreateTime()));

            // 续当每笔一段（已销的 SQL 已过滤）。
            for (TimelineRenewRow row : timelineMapper.selectRenews(head.getTicketId())) {
                segments.add(new RenewSegment(
                        row.getId(),
                        row.getRenewNo(),
                        row.getOldDueDate(),
                        row.getNewDueDate(),
                        row.getExtendMonths(),
                        row.getRenewedAt()));
            }

            // 赎当结清段。
            for (TimelineRedeemRow row : timelineMapper.selectRedeems(head.getTicketId())) {
                segments.add(new RedeemSegment(
                        row.getId(),
                        row.getRedeemNo(),
                        row.getRedeemedAt(),
                        row.getUsedDays(),
                        row.getFeeAmount(),
                        row.getTotalAmount()));
            }

            // 绝当处置段。
            for (TimelineForfeitRow row : timelineMapper.selectForfeits(head.getTicketId())) {
                segments.add(new ForfeitSegment(
                        row.getId(),
                        row.getForfeitNo(),
                        row.getForfeitedAt(),
                        row.getDisposeMethod(),
                        labelOfDisposeMethod(row.getDisposeMethod()),
                        row.getRecoverAmount()));
            }

            return TicketTimeline.assemble(head.getTicketNo(), segments);
        });
    }

    /** 类别码 → 中文名；库里出现脏码时给 null 兜底，不让脏码把整条线带挂。 */
    private static String labelOfCategory(String code) {
        Category category = Category.ofCodeOrNull(code);
        return category == null ? null : category.label();
    }

    /** 处置方式码 → 中文名；脏码兜底 null（处置方式只有三值，正常必能解析）。 */
    private static String labelOfDisposeMethod(String code) {
        if (code == null) {
            return null;
        }
        for (DisposeMethod method : DisposeMethod.values()) {
            if (method.code().equals(code.trim())) {
                return method.label();
            }
        }
        return null;
    }

    /** 票状态码 → 枚举；脏码返回 null（状态码随段原样外放，label 留空兜底）。 */
    private static TicketStatus parseStatusOrNull(String code) {
        if (code == null) {
            return null;
        }
        for (TicketStatus status : TicketStatus.values()) {
            if (status.code().equals(code.trim())) {
                return status;
            }
        }
        return null;
    }

    /**
     * 阻塞 DB 调用 → 响应式链路桥接器：先从 Reactor Context 取操作人，再切到 boundedElastic
     * （倒线只读不写审计，取操作人仅为与其它只读端口保持同一套桥接约定）。
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
