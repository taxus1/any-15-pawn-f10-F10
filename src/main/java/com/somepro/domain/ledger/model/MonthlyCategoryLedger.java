package com.somepro.domain.ledger.model;

import java.math.BigDecimal;

/**
 * 月度类别台账一行（纯领域值对象，不可变 record）：一个自然月 × 一个当物类别的汇总。
 *
 * 一行落这几档数，各档按各自的办理时刻归月、按票面上的类别快照归类，互不相串：
 * <ul>
 *   <li>{@code newTicketCount} —— 本月本类别新开当票张数（开票按当票办理时刻归月）；</li>
 *   <li>{@code newPawnAmount} —— 本月新放当金合计 = 这些新票票面当金之和；</li>
 *   <li>{@code renewCount} —— 本月办理续当次数（按续当办理时刻归月）；</li>
 *   <li>{@code redeemCount} —— 本月赎当笔数（按赎当办理时刻归月）；</li>
 *   <li>{@code redeemPrincipalAmount} —— 赎当收回本金合计，按每笔赎当对应票面当金累加；</li>
 *   <li>{@code redeemFeeAmount} —— 赎当费用合计，按每笔赎当结出来的费用逐笔累加，
 *       与本金同一套「逐笔赎当」口径；</li>
 *   <li>{@code forfeitCount} —— 本月绝当笔数（按绝当处置时刻归月）。</li>
 * </ul>
 * 已打删除标记（del_flag=1）的票 / 续当 / 赎当 / 绝当一律不进台账。
 * 没有任何业务的「月份 × 类别」格子也占一行，各档为 0，由仓储层补齐。
 *
 * @param month                  自然月，yyyy-MM（月底最后一天的业务算当月，次月头一天算次月）
 * @param category               类别代码（JEWELRY / WATCH / ELECTRONICS / VEHICLE / OTHER）
 * @param newTicketCount         新开当票张数
 * @param newPawnAmount          新放当金合计（元）
 * @param renewCount             续当次数
 * @param redeemCount            赎当笔数
 * @param redeemPrincipalAmount  赎当收回本金合计（元）
 * @param redeemFeeAmount        赎当费用合计（元）
 * @param forfeitCount           绝当笔数
 */
public record MonthlyCategoryLedger(String month,
                                    String category,
                                    long newTicketCount,
                                    BigDecimal newPawnAmount,
                                    long renewCount,
                                    long redeemCount,
                                    BigDecimal redeemPrincipalAmount,
                                    BigDecimal redeemFeeAmount,
                                    long forfeitCount) {
}
