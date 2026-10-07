package com.somepro.domain.timeline.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 时间线续当段：办过的每一次续当各占一段（已销掉的续当不在线上）。
 *
 * 编号带续当单号 renewNo（XD-年份-序号）。带出的全是这条续当当初定格落库的原值：
 * <ul>
 *   <li>oldDueDate 这次顺延【之前】的到期日期（办理当下从票面定格抄入）；</li>
 *   <li>newDueDate 这次顺延【之后】的到期日期；</li>
 *   <li>extendMonths 本次顺延了几个月（按当票原当期月数走）。</li>
 * </ul>
 * 业务时刻取 renewed_at（续当办理时刻），不是 create_time —— 补录的续当落库在后、
 * 业务上却是早的，排序只认 renewed_at。
 */
public record RenewSegment(Long id,
                           String renewNo,
                           LocalDate oldDueDate,
                           LocalDate newDueDate,
                           Integer extendMonths,
                           LocalDateTime renewedAt) implements TimelineSegment {

    @Override
    public SegmentType type() {
        return SegmentType.RENEW;
    }

    @Override
    public String bizNo() {
        return renewNo;
    }

    @Override
    public LocalDateTime bizTime() {
        return renewedAt;
    }

    @Override
    public Long segmentId() {
        return id;
    }
}
