package com.somepro.application.rate;

import com.somepro.common.exception.BizException;
import com.somepro.domain.collateral.model.Category;
import com.somepro.domain.rate.model.PawnRate;
import com.somepro.domain.rate.model.PawnRateQuery;
import com.somepro.domain.rate.model.RateStatus;
import com.somepro.domain.rate.repository.PawnRateRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 费率配置应用服务：编排录入、修改、查看、停用、翻配置清单五个用例，不写表映射。
 *
 * 出入参用领域对象/基础类型，不认识 PO 与 VO。
 *
 * 单条配置自身的规则（比例范围、小数位、默认启用）在 PawnRate 聚合里；
 * 「一个类别只留一条」在仓储里（uk_category 唯一索引兜底）。
 * 与当票模块的衔接：开票/改当金按类别现查 ENABLED 的那条抄快照 ——
 * 这里改完、停完只影响以后新开的票，已经在当的老票照旧按票上快照走。
 */
@Service
public class PawnRateAppService {

    private final PawnRateRepository pawnRateRepository;

    public PawnRateAppService(PawnRateRepository pawnRateRepository) {
        this.pawnRateRepository = pawnRateRepository;
    }

    /**
     * 录入：类别 + 月利率 + 月综合费率 + 折当率上限，新配的默认启用。
     * 类别只认当物那五种；同类别已有配置（不论启用停用）由仓储挡回，不重开第二条。
     */
    public Mono<PawnRate> create(String category, String monthlyRate,
                                 String serviceRate, String maxLoanRatio) {
        Category cat = parseCategory(category);
        PawnRate rate = PawnRate.open(cat,
                parseRatio(monthlyRate, "月利率"),
                parseRatio(serviceRate, "月综合费率"),
                parseRatio(maxLoanRatio, "折当率上限"));
        return pawnRateRepository.insert(rate);
    }

    /**
     * 修改：月利率 / 月综合费率 / 折当率上限，任一项留空表示该项不动。
     * 三个数一条 UPDATE 整体落库，开票侧抄到的不会一半旧一半新；
     * 只影响以后新开的票，老票快照不动。
     */
    public Mono<PawnRate> update(Long id, String monthlyRate, String serviceRate, String maxLoanRatio) {
        BigDecimal monthly = isBlank(monthlyRate) ? null : parseRatio(monthlyRate, "月利率");
        BigDecimal service = isBlank(serviceRate) ? null : parseRatio(serviceRate, "月综合费率");
        BigDecimal ratio = isBlank(maxLoanRatio) ? null : parseRatio(maxLoanRatio, "折当率上限");
        return requireRate(id).flatMap(rate -> {
            rate.revise(monthly, service, ratio);
            return pawnRateRepository.update(rate);
        });
    }

    /** 查看：id 或 category（类别即业务主键）任一指定。 */
    public Mono<PawnRate> detail(Long id, String category) {
        if (id != null) {
            return requireRate(id);
        }
        Category cat = Category.ofCode(blankToNull(category));
        if (cat != null) {
            return pawnRateRepository.findByCategory(cat)
                    .switchIfEmpty(Mono.error(new BizException("该类别（" + cat.label() + "）还没有费率配置")));
        }
        return Mono.error(new BizException("请指定要查看的费率配置（id 或 category）"));
    }

    /**
     * 停用：该类别新开票随即卡住（开票只取 ENABLED 的配置）；
     * 已开出的票不受影响，仍按票上快照结算。已停用的不能重复停用。
     */
    public Mono<PawnRate> disable(Long id) {
        return requireRate(id).flatMap(rate -> {
            rate.disable();
            return pawnRateRepository.update(rate);
        });
    }

    /** 翻配置清单：类别/状态随意拼，都不填翻整份；一页一页走，每行带类别。 */
    public Mono<PageResult<PawnRate>> page(int pageNum, int pageSize, String category, String status) {
        if (pageNum < 1 || pageSize < 1) {
            return Mono.error(new BizException("页码与每页条数必须为正整数"));
        }
        PawnRateQuery query = PawnRateQuery.of(
                Category.ofCode(blankToNull(category)),
                RateStatus.ofCode(blankToNull(status)));
        return pawnRateRepository.page(pageNum, pageSize, query);
    }

    private Mono<PawnRate> requireRate(Long id) {
        if (id == null) {
            return Mono.error(new BizException("必须指定费率配置 id"));
        }
        return pawnRateRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("费率配置不存在")));
    }

    /** 录入时类别必填：只认当物那五种，别的写法（含留空）一律挡回。 */
    private Category parseCategory(String raw) {
        Category cat = Category.ofCode(blankToNull(raw));
        if (cat == null) {
            throw new BizException("必须指定适用类别（JEWELRY / WATCH / ELECTRONICS / VEHICLE / OTHER）");
        }
        return cat;
    }

    /** 比例入参解析：必须是小数；范围与小数位由聚合统一卡。 */
    private BigDecimal parseRatio(String raw, String name) {
        if (isBlank(raw)) {
            throw new BizException(name + "不能为空");
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new BizException(name + "必须是小数比例：" + raw);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
