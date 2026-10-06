package com.somepro.interfaces.rest.renew.converter;

import com.somepro.domain.renew.model.PawnRenew;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.renew.vo.PawnRenewVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 续当领域对象 → VO 转换器（用户接口层）。Controller 不直接把领域对象塞进 Result。
 */
public final class PawnRenewVoConverter {

    private PawnRenewVoConverter() {
    }

    public static PawnRenewVO toVo(PawnRenew domain) {
        return new PawnRenewVO(
                domain.getId(),
                domain.getRenewNo(),
                domain.getTicketId(),
                domain.getOldDueDate(),
                domain.getNewDueDate(),
                domain.getExtendMonths(),
                domain.getRenewedAt(),
                domain.getCreateTime());
    }

    public static PageVO<PawnRenewVO> toPageVo(PageResult<PawnRenew> page) {
        List<PawnRenewVO> content = page.content().stream()
                .map(PawnRenewVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
