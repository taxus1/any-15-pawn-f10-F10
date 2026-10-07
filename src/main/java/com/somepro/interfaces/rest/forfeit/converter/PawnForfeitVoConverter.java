package com.somepro.interfaces.rest.forfeit.converter;

import com.somepro.domain.forfeit.model.ForfeitView;
import com.somepro.domain.forfeit.model.PawnForfeit;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.forfeit.vo.ForfeitViewVO;
import com.somepro.interfaces.rest.forfeit.vo.PawnForfeitVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 绝当领域对象 → VO 转换器（用户接口层）。Controller 不直接把领域对象塞进 Result。
 *
 * 办理回单走 {@link #toVo}（处置单落库列）；详情 / 翻单走 {@link #toViewVo}（含欠款与盈亏对账列）。
 */
public final class PawnForfeitVoConverter {

    private PawnForfeitVoConverter() {
    }

    public static PawnForfeitVO toVo(PawnForfeit domain) {
        return new PawnForfeitVO(
                domain.getId(),
                domain.getForfeitNo(),
                domain.getTicketId(),
                domain.getCollateralId(),
                domain.getForfeitedAt(),
                domain.getDisposeMethod() == null ? null : domain.getDisposeMethod().code(),
                domain.getRecoverAmount(),
                domain.getCreateTime());
    }

    public static ForfeitViewVO toViewVo(ForfeitView view) {
        return new ForfeitViewVO(
                view.id(),
                view.forfeitNo(),
                view.ticketId(),
                view.ticketNo(),
                view.collateralId(),
                view.itemNo(),
                view.itemName(),
                view.forfeitedAt(),
                view.disposeMethod(),
                view.disposeMethodLabel(),
                view.recoverAmount(),
                view.owedUsedDays(),
                view.owedFeeAmount(),
                view.owedAmount(),
                view.profitLossAmount(),
                view.profitLoss(),
                view.createTime());
    }

    public static PageVO<ForfeitViewVO> toPageVo(PageResult<ForfeitView> page) {
        List<ForfeitViewVO> content = page.content().stream()
                .map(PawnForfeitVoConverter::toViewVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
