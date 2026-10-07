package com.somepro.infrastructure.persistence.timeline;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 时间线赎当段投影行：t_pawn_redeem 中某张票的有效赎当（del_flag=0）。
 * 计费天数 / 费用 / 应还总额照这笔赎当当初定格落库的原值点回，不按当前费率重算。
 * 只在基础设施层内使用。
 */
@Getter
@Setter
public class TimelineRedeemRow {

    private Long id;
    private String redeemNo;
    private LocalDateTime redeemedAt;
    private Integer usedDays;
    private BigDecimal feeAmount;
    private BigDecimal totalAmount;
}
