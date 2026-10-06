package com.somepro.infrastructure.persistence.ticket.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 当票模块眼里的 t_collateral（PO，基础设施层）：只映射开票要读、状态联动要写的几列。
 *
 * 与当物模块自己的 CollateralPO 各管各的视角：这里只取 id / pawner_id / category /
 * appraised_value / status，用于开票时读快照，以及开票置「已典当」、撤销回「在库」的
 * 状态联动（与当票写入同一事务）。本类不做任何建表/改表动作。
 */
@Getter
@Setter
@TableName("t_collateral")
public class TicketCollateralPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("pawner_id")
    private Long pawnerId;

    @TableField("category")
    private String category;

    @TableField("appraised_value")
    private BigDecimal appraisedValue;

    @TableField("status")
    private String status;
}
