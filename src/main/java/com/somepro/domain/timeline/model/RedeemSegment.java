package com.somepro.domain.timeline.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 时间线结局段之一：赎当结清（最后是客户把东西赎回去了）。
 *
 * 编号带赎当单号 redeemNo（SD-年份-序号）。带出的全是这笔赎当当初定格落库的原值，
 * 不按现在挂的费率重算：
 * <ul>
 *   <li>usedDays 计费天数（起当日期到赎当日，至少 1）；</li>
 *   <li>feeAmount 利息与综合费合计；</li>
 *   <li>totalAmount 应还总额 = 当金 + 费用 —— 这里的「当金」以开票段票面 pawnAmount 为准，
 *       时间线自洽校验就是拿票面本金 + 本费用去对这笔总额，对不上即为账串了。</li>
 * </ul>
 * 业务时刻取 redeemed_at（赎当办理时刻），不是 create_time（补录口径同续当段）。
 */
public record RedeemSegment(Long id,
                            String redeemNo,
                            LocalDateTime redeemedAt,
                            Integer usedDays,
                            BigDecimal feeAmount,
                            BigDecimal totalAmount) implements TimelineSegment {

    @Override
    public SegmentType type() {
        return SegmentType.REDEEM;
    }

    @Override
    public String bizNo() {
        return redeemNo;
    }

    @Override
    public LocalDateTime bizTime() {
        return redeemedAt;
    }

    @Override
    public Long segmentId() {
        return id;
    }
}
