package com.somepro.infrastructure.persistence.collection.po;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 催收工作台连表查询投影行（基础设施层）。
 *
 * 不是任何一张表的 PO、没有 @TableName —— 它只是把当票（主）LEFT JOIN 当户、当物，
 * 再带上「续当次数」子查询的一屏列形状装回来，由 MyBatis 按列名（驼峰映射）填充。
 * 只在基础设施层内使用，不外泄到领域 / 接口层。
 *
 * 当票字段只挑催收试算与展示用得上的：当金、起当/到期日期、月利率与月综合费率快照、
 * 当期月数（票必为 ACTIVE，状态无需再带）。
 */
@Getter
@Setter
public class CollectionWorkRow {

    // ---- 当票 ----
    private Long ticketId;
    private String ticketNo;
    private Long pawnerId;
    private Long collateralId;
    private BigDecimal pawnAmount;
    private LocalDate startDate;
    private LocalDate dueDate;
    private BigDecimal monthlyRate;
    private BigDecimal serviceRate;
    private Integer termMonths;

    // ---- 当户（LEFT JOIN，理论上必有；档案缺失时为空）----
    private String pawnerName;
    private String pawnerPhone;

    // ---- 当物（LEFT JOIN，理论上必有；档案缺失时为空）----
    private String collateralName;

    // ---- 续当次数（子查询，无续当为 0）----
    private Integer renewCount;
}
