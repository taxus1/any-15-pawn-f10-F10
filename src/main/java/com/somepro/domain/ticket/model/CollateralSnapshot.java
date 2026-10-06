package com.somepro.domain.ticket.model;

import com.somepro.domain.collateral.model.Category;

import java.math.BigDecimal;

/**
 * 当物快照（不可变值对象）：开票当下从当物模块读到的当物事实。
 *
 * 当票只取四样：id（押的是哪件）、pawnerId（当户随当物带出，票面当户以当物底账为准，
 * 不接受柜台另报）、category（类别快照随当物带出）、appraisedValue（折当时估值快照）。
 * 读进来是什么样，票上就落什么样，之后当物档案再改也不回写已开出的票。
 */
public record CollateralSnapshot(Long id,
                                 Long pawnerId,
                                 Category category,
                                 BigDecimal appraisedValue) {
}
