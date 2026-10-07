package com.somepro.infrastructure.persistence.forfeit;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 绝当翻单连表查询投影行（基础设施层）。
 *
 * 不是任何一张表的 PO、没有 @TableName —— 它是处置单 f INNER JOIN 当票 t、
 * LEFT JOIN 当物 c 后的一屏列形状，由 MyBatis 按列别名（驼峰映射）填充。只在基础设施层内使用。
 *
 * 列分三块：处置单本身（含 f.create_time）、欠款试算要用到的票面快照
 * （当金、起当日期、月利率与月综合费率）、对号用的票号与当物编号名称。
 */
@Getter
@Setter
public class ForfeitListRow {

    // ---- 处置单 ----
    private Long id;
    private String forfeitNo;
    private Long ticketId;
    private Long collateralId;
    private LocalDateTime forfeitedAt;
    private String disposeMethod;
    private BigDecimal recoverAmount;
    private LocalDateTime createTime;

    // ---- 当票（INNER JOIN，必有）----
    private String ticketNo;
    private BigDecimal pawnAmount;
    private LocalDate startDate;
    private BigDecimal monthlyRate;
    private BigDecimal serviceRate;

    // ---- 当物（LEFT JOIN，档案缺失时为空）----
    private String itemNo;
    private String itemName;
}
