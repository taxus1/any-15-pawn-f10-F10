package com.somepro.interfaces.rest.collection.converter;

import com.somepro.domain.collection.model.CollectionWorkItem;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.collection.vo.CollectionWorkItemVO;
import com.somepro.interfaces.rest.common.vo.PageVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 催收工作台领域视图 → VO 转换器（用户接口层）。Controller 不直接把领域对象塞进 Result。
 */
public final class CollectionWorkItemVoConverter {

    private CollectionWorkItemVoConverter() {
    }

    public static CollectionWorkItemVO toVo(CollectionWorkItem item) {
        return new CollectionWorkItemVO(
                item.ticketId(),
                item.ticketNo(),
                item.pawnerId(),
                item.pawnerName(),
                item.pawnerPhone(),
                item.collateralId(),
                item.collateralName(),
                item.dueDate(),
                item.bucket() == null ? null : item.bucket().code(),
                item.daysRemaining(),
                item.overdueDays(),
                item.renewCount(),
                item.redeemTotalAmount(),
                item.redeemFeeAmount(),
                item.redeemUsedDays(),
                item.nextDueDate(),
                item.extendMonths());
    }

    public static PageVO<CollectionWorkItemVO> toPageVo(PageResult<CollectionWorkItem> page) {
        List<CollectionWorkItemVO> content = page.content().stream()
                .map(CollectionWorkItemVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
