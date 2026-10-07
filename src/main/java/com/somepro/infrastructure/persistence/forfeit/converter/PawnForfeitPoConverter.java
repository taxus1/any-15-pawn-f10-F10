package com.somepro.infrastructure.persistence.forfeit.converter;

import com.somepro.domain.forfeit.model.DisposeMethod;
import com.somepro.domain.forfeit.model.PawnForfeit;
import com.somepro.infrastructure.persistence.forfeit.po.PawnForfeitPO;

/**
 * PawnForfeitPO（表）↔ PawnForfeit（领域）转换器（基础设施层），PO 不外泄。
 * dispose_method 列存枚举名，读出时 valueOf 还原；库里的值受写入端约束，必为合法处置方式之一。
 */
public final class PawnForfeitPoConverter {

    private PawnForfeitPoConverter() {
    }

    public static PawnForfeitPO toPo(PawnForfeit domain) {
        PawnForfeitPO po = new PawnForfeitPO();
        po.setId(domain.getId());
        po.setForfeitNo(domain.getForfeitNo());
        po.setTicketId(domain.getTicketId());
        po.setCollateralId(domain.getCollateralId());
        po.setForfeitedAt(domain.getForfeitedAt());
        po.setDisposeMethod(domain.getDisposeMethod() == null ? null : domain.getDisposeMethod().code());
        po.setRecoverAmount(domain.getRecoverAmount());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static PawnForfeit toDomain(PawnForfeitPO po) {
        PawnForfeit domain = new PawnForfeit();
        domain.setId(po.getId());
        domain.setForfeitNo(po.getForfeitNo());
        domain.setTicketId(po.getTicketId());
        domain.setCollateralId(po.getCollateralId());
        domain.setForfeitedAt(po.getForfeitedAt());
        domain.setDisposeMethod(po.getDisposeMethod() == null ? null : DisposeMethod.valueOf(po.getDisposeMethod()));
        domain.setRecoverAmount(po.getRecoverAmount());
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
