package com.somepro.domain.ledger.repository;

import com.somepro.domain.ledger.model.LedgerQuery;
import com.somepro.domain.ledger.model.MonthlyCategoryLedger;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 月度类别台账仓储端口（领域层）：只读汇总，不做任何写入。
 *
 * 实现方要保证：
 * - 月份 × 类别格子即使一档业务都没有，也补一行零值，库里一条业务数据都没有时返回零值页而非报错；
 * - 已打删除标记的票 / 续当 / 赎当 / 绝当不进任何一档；
 * - 分页一页一页走，行数多也不一次性全塞回来。
 */
public interface MonthlyLedgerRepository {

    /**
     * 分页翻台账。排序固定：月份倒序（近月在前）、同月按类别代码升序，翻页稳定。
     */
    Mono<PageResult<MonthlyCategoryLedger>> page(int pageNum, int pageSize, LedgerQuery query);
}
