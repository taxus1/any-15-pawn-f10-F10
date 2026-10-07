package com.somepro.infrastructure.persistence.ledger;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 月度类别台账汇总 SQL 的投影行（基础设施层）。
 *
 * 不是任何一张表的 PO、没有 @TableName —— 它是「月份格子 × 类别格子 LEFT JOIN 四档汇总」后
 * 一行台账的列形状，由 MyBatis 按列别名填充。只在基础设施层内使用。
 * 没业务的格子由 SQL 补零，故所有数值列都不会为 null。
 */
@Getter
@Setter
public class MonthlyLedgerRow {

    /** 自然月，yyyy-MM。 */
    private String month;
    /** 类别代码。 */
    private String category;

    /** 本月本类别新开当票张数。 */
    private Long newTicketCount;
    /** 本月新放当金合计。 */
    private BigDecimal newPawnAmount;
    /** 本月续当次数。 */
    private Long renewCount;
    /** 本月赎当笔数。 */
    private Long redeemCount;
    /** 本月赎当收回本金合计（逐笔赎当对应票面当金）。 */
    private BigDecimal redeemPrincipalAmount;
    /** 本月赎当费用合计（逐笔赎当结出来的 fee_amount）。 */
    private BigDecimal redeemFeeAmount;
    /** 本月绝当笔数。 */
    private Long forfeitCount;
}
