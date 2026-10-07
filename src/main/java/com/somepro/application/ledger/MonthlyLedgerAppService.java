package com.somepro.application.ledger;

import com.somepro.common.exception.BizException;
import com.somepro.domain.collateral.model.Category;
import com.somepro.domain.ledger.model.LedgerQuery;
import com.somepro.domain.ledger.model.MonthlyCategoryLedger;
import com.somepro.domain.ledger.repository.MonthlyLedgerRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 月度类别台账应用服务：编排「月底老板翻台账」这一个用例 ——
 * 按自然月 × 当物类别铺开一页汇总，支持按月份、按类别筛选，分页一页页走。
 *
 * 出入参用领域对象/基础类型，不认识 PO 与 VO。全程只读，不动任何票与业务单据。
 */
@Service
public class MonthlyLedgerAppService {

    /** 业务日期统一按行里所在时区算，避免容器 UTC 下把自然月归偏。 */
    private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");

    private final MonthlyLedgerRepository monthlyLedgerRepository;

    public MonthlyLedgerAppService(MonthlyLedgerRepository monthlyLedgerRepository) {
        this.monthlyLedgerRepository = monthlyLedgerRepository;
    }

    /**
     * 翻台账。
     *
     * @param pageNum  页码，从 1 起
     * @param pageSize 每页条数
     * @param month    自然月 yyyy-MM；null 表示从最早业务月铺到当前月（无业务的月份补零行）
     * @param category 类别代码；null 表示五个类别都要
     */
    public Mono<PageResult<MonthlyCategoryLedger>> page(int pageNum, int pageSize, String month, Category category) {
        if (pageNum < 1 || pageSize < 1) {
            return Mono.error(new BizException("页码与每页条数必须为正整数"));
        }
        LocalDate today = LocalDate.now(BIZ_ZONE);
        return monthlyLedgerRepository.page(pageNum, pageSize, LedgerQuery.of(month, category, today));
    }
}
