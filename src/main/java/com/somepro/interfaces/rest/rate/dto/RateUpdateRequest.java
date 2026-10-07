package com.somepro.interfaces.rest.rate.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 修改费率配置入参（用户接口层）。
 *
 * 三个数任一项留空表示该项不动；非空的项照常校验。改完只影响以后新开的票，
 * 已经在当的老票照旧按票上快照走。状态不走这里改，停用走 /disable。
 */
@Getter
@Setter
public class RateUpdateRequest {

    /** 要改的是哪条配置（t_pawn_rate.id）。 */
    private Long id;

    /** 月利率，小数比例；留空不动。 */
    private String monthlyRate;

    /** 月综合费率，小数比例；留空不动。 */
    private String serviceRate;

    /** 折当率上限，0 到 1 之间的小数；留空不动。 */
    private String maxLoanRatio;
}
