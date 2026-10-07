package com.somepro.interfaces.rest.ledger.converter;

import com.somepro.domain.collateral.model.Category;
import com.somepro.domain.ledger.model.MonthlyCategoryLedger;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.ledger.vo.MonthlyCategoryLedgerVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 月度类别台账领域行 → VO 转换器（用户接口层）。Controller 不直接把领域对象塞进 Result。
 */
public final class MonthlyLedgerVoConverter {

    private MonthlyLedgerVoConverter() {
    }

    public static MonthlyCategoryLedgerVO toVo(MonthlyCategoryLedger row) {
        String label = null;
        if (row.category() != null) {
            // 类别格子由领域枚举生成，正常必能解析；保留 null 兜底，不让脏类别码把整页带挂
            Category category = Category.ofCodeOrNull(row.category());
            if (category != null) {
                label = category.label();
            }
        }
        return new MonthlyCategoryLedgerVO(
                row.month(),
                row.category(),
                label,
                row.newTicketCount(),
                row.newPawnAmount(),
                row.renewCount(),
                row.redeemCount(),
                row.redeemPrincipalAmount(),
                row.redeemFeeAmount(),
                row.forfeitCount());
    }

    public static PageVO<MonthlyCategoryLedgerVO> toPageVo(PageResult<MonthlyCategoryLedger> page) {
        List<MonthlyCategoryLedgerVO> content = page.content().stream()
                .map(MonthlyLedgerVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
