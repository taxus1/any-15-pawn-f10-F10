package com.somepro.infrastructure.persistence.ticket.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * t_pawn_ticket 表的持久化对象（PO，基础设施层）。只描述表结构，不放业务规则。
 *
 * 表已由 doc/schema/pawn.sql 建好，列名即契约，本类不做任何建表/改表动作。
 * category / status 两列直接存枚举名，由 Converter 与领域枚举互转；
 * 利率、费率、估值都是开票当下的快照列，落库后不回写。
 */
@Getter
@Setter
@TableName("t_pawn_ticket")
public class PawnTicketPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("ticket_no")
    private String ticketNo;

    @TableField("pawner_id")
    private Long pawnerId;

    @TableField("collateral_id")
    private Long collateralId;

    @TableField("category")
    private String category;

    @TableField("pawn_amount")
    private BigDecimal pawnAmount;

    @TableField("appraised_value")
    private BigDecimal appraisedValue;

    @TableField("monthly_rate")
    private BigDecimal monthlyRate;

    @TableField("service_rate")
    private BigDecimal serviceRate;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("due_date")
    private LocalDate dueDate;

    @TableField("term_months")
    private Integer termMonths;

    @TableField("status")
    private String status;
}
