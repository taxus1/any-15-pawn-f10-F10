package com.somepro.infrastructure.persistence.rate.converter;

import com.somepro.domain.collateral.model.Category;
import com.somepro.domain.rate.model.PawnRate;
import com.somepro.domain.rate.model.RateStatus;
import com.somepro.infrastructure.persistence.ticket.po.PawnRatePO;

/**
 * PawnRatePO（表）↔ PawnRate（领域）转换器（基础设施层），PO 不外泄。
 * 类别、状态是枚举列：列里直接存枚举名，转换时按枚举名解析。
 */
public final class PawnRatePoConverter {

    private PawnRatePoConverter() {
    }

    public static PawnRatePO toPo(PawnRate domain) {
        PawnRatePO po = new PawnRatePO();
        po.setId(domain.getId());
        po.setCategory(domain.getCategory() == null ? null : domain.getCategory().code());
        po.setMonthlyRate(domain.getMonthlyRate());
        po.setServiceRate(domain.getServiceRate());
        po.setMaxLoanRatio(domain.getMaxLoanRatio());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().code());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static PawnRate toDomain(PawnRatePO po) {
        PawnRate domain = new PawnRate();
        domain.setId(po.getId());
        domain.setCategory(po.getCategory() == null ? null : Category.valueOf(po.getCategory()));
        domain.setMonthlyRate(po.getMonthlyRate());
        domain.setServiceRate(po.getServiceRate());
        domain.setMaxLoanRatio(po.getMaxLoanRatio());
        domain.setStatus(po.getStatus() == null ? null : RateStatus.valueOf(po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
