package com.somepro.domain.timeline.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 时间线结局段之一：绝当处置（最后是东西没赎回去、走了处置）。
 *
 * 编号带绝当单号 forfeitNo（JD-年份-序号）。带出处置当初落库的原值：
 * <ul>
 *   <li>disposeMethod / disposeMethodLabel 处置方式（AUCTION 拍卖 / CONSIGN 变卖 / WRITE_OFF 核销）；</li>
 *   <li>recoverAmount 处置回款（实际到手，可为零、不为负）。</li>
 * </ul>
 * 业务时刻取 forfeited_at（绝当处置时刻），不是 create_time（补录口径同续当段）。
 * 同一张票不该既挂赎当段又挂绝当段 —— 互斥性由时间线自洽校验钉。
 */
public record ForfeitSegment(Long id,
                             String forfeitNo,
                             LocalDateTime forfeitedAt,
                             String disposeMethod,
                             String disposeMethodLabel,
                             BigDecimal recoverAmount) implements TimelineSegment {

    @Override
    public SegmentType type() {
        return SegmentType.FORFEIT;
    }

    @Override
    public String bizNo() {
        return forfeitNo;
    }

    @Override
    public LocalDateTime bizTime() {
        return forfeitedAt;
    }

    @Override
    public Long segmentId() {
        return id;
    }
}
