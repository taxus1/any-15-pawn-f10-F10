package com.somepro.infrastructure.persistence.timeline;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 时间线「票头」连表查询投影行（基础设施层）。
 *
 * 不是任何一张表的 PO、没有 @TableName —— 它是当票 t 按当票号点出后
 * LEFT JOIN 当户 p、当物 c 的一屏列形状，由 MyBatis 按列别名（驼峰映射）填充。
 * 只在基础设施层内使用，不外泄到领域 / 接口层。
 *
 * 当户 / 当物走 LEFT JOIN：时间线一段都不能缺，档案后来被销（del_flag=1）时
 * 仍把编号名字带出来、用 pawnerDelFlag/collateralDelFlag 标出，而不是把整行吞掉。
 */
@Getter
@Setter
public class TimelineHeadRow {

    // ---- 当票（主表，必为 del_flag=0）----
    private Long ticketId;
    private String ticketNo;
    private Long pawnerId;
    private Long collateralId;
    private String category;
    private BigDecimal pawnAmount;
    private BigDecimal appraisedValue;
    private BigDecimal monthlyRate;
    private BigDecimal serviceRate;
    private LocalDate startDate;
    private LocalDate dueDate;
    private Integer termMonths;
    private String status;
    private LocalDateTime ticketCreateTime;

    // ---- 当户（LEFT JOIN，档案缺失或已销时为空 / 带删除标记）----
    private String pawnerNo;
    private String pawnerName;
    private LocalDateTime pawnerCreateTime;
    private Integer pawnerDelFlag;

    // ---- 当物（LEFT JOIN，档案缺失或已销时为空 / 带删除标记）----
    private String itemNo;
    private String collateralCategory;
    private String itemName;
    private String brand;
    private BigDecimal itemAppraisedValue;
    private LocalDateTime collateralCreateTime;
    private Integer collateralDelFlag;
}
