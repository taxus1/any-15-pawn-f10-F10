package com.somepro.interfaces.rest.ledger;

import com.somepro.application.ledger.MonthlyLedgerAppService;
import com.somepro.common.Result;
import com.somepro.domain.collateral.model.Category;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.ledger.converter.MonthlyLedgerVoConverter;
import com.somepro.interfaces.rest.ledger.vo.MonthlyCategoryLedgerVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 月度类别台账用户接口层：一行 = 一个自然月 × 一个当物类别的汇总。
 *
 * 只做协议适配（参数解析、VO 转换、Result 包装），业务编排在 {@link MonthlyLedgerAppService}。
 * 接口只读。
 */
@RestController
@RequestMapping("/api/ledger/monthly-category")
public class MonthlyLedgerController {

    private final MonthlyLedgerAppService monthlyLedgerAppService;

    public MonthlyLedgerController(MonthlyLedgerAppService monthlyLedgerAppService) {
        this.monthlyLedgerAppService = monthlyLedgerAppService;
    }

    /**
     * 翻台账：
     * - month 不传：从最早一笔业务所在自然月铺到当前月，中间没业务的月份也补零行；
     *   传 yyyy-MM：只看该月（整月无数据也返回零值行）；
     * - category 不传：五个类别格子都给；传单个类别代码只看该类别；
     * - 无任何业务数据时返回「当前月 × 类别」的零值页，不报错；
     * - 排序为月份倒序、同月类别升序；pageNum/pageSize 一页页走。
     */
    @GetMapping("/list")
    public Mono<Result<PageVO<MonthlyCategoryLedgerVO>>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String category) {
        Category categoryFilter = Category.ofCode(category);
        return monthlyLedgerAppService.page(pageNum, pageSize, month, categoryFilter)
                .map(MonthlyLedgerVoConverter::toPageVo)
                .map(Result::ok);
    }
}
