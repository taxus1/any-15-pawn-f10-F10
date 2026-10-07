package com.somepro.domain.timeline.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 时间线第三段：当票开立（开了多少钱、什么时候起到什么时候止）。
 *
 * 编号带当票号 ticketNo（DP-年份-序号）—— 也是整条线的查询入参。
 * 这段带出的数全部是票面当初落库的原值，照原样回放，不按现在挂的费率/估值重算：
 * <ul>
 *   <li>pawnAmount 当金（开了多少钱）；</li>
 *   <li>appraisedValue 折当估值快照（开票当下从当物抄录）；</li>
 *   <li>monthlyRate / serviceRate 月利率 / 月综合费率快照（开票当下从费率配置抄录）；</li>
 *   <li>startDate / dueDate 起当日期 / 到期日期；termMonths 当期月数。</li>
 * </ul>
 * 业务时刻取票面 create_time（开票办理时刻，台账同一口径）。
 * status 是票走到今天的结果（在当 / 已赎 / 已绝当 / 已撤销），原样带出便于一眼看到结局；
 * 时间线上「结局段」以赎当 / 绝当段为准，撤销的票没有结局段、只剩到开票为止。
 */
public record TicketIssueSegment(Long id,
                                 String ticketNo,
                                 Long pawnerId,
                                 Long collateralId,
                                 String category,
                                 String categoryLabel,
                                 BigDecimal pawnAmount,
                                 BigDecimal appraisedValue,
                                 BigDecimal monthlyRate,
                                 BigDecimal serviceRate,
                                 LocalDate startDate,
                                 LocalDate dueDate,
                                 Integer termMonths,
                                 String status,
                                 String statusLabel,
                                 LocalDateTime issuedAt) implements TimelineSegment {

    @Override
    public SegmentType type() {
        return SegmentType.TICKET_ISSUE;
    }

    @Override
    public String bizNo() {
        return ticketNo;
    }

    @Override
    public LocalDateTime bizTime() {
        return issuedAt;
    }

    @Override
    public Long segmentId() {
        return id;
    }
}
