package com.somepro.domain.rate.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.collateral.model.Category;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 费率配置聚合根（纯领域对象，不带任何持久化注解）。
 *
 * 一条配置 = 一个当物类别的取数口径：月利率、月综合费率、折当率上限。核心不变量：
 * 1. 月利率、月综合费率写成小数比例（0 ≤ 值 &lt; 1，最多五位小数，与列的 DECIMAL(8,5) 对齐，
 *    超出五位会被静默四舍五入，直接挡回，不让账面口径和录入口径打架）；
 * 2. 折当率上限是 0 到 1 之间的小数（0 &lt; 值 ≤ 1，最多四位小数，与 DECIMAL(5,4) 对齐）：
 *    写 0.7 就是最多按估值的七成放当金；写 0 等于这个类别一钱不放，没意义，不收；
 * 3. 新配的默认启用（ENABLED）；停用（DISABLED）的配置不参与开票取数，
 *    该类别新开票随之卡住，已开出的票仍按票上快照走，不回写；
 * 4. 「一个类别只留一条配置」是跨聚合的唯一性，由仓储落库时保证
 *    （t_pawn_rate.uk_category 唯一索引兜底），本聚合只管单条配置自身的规则。
 *
 * 三个数（月利率/月综合费率/折当率上限）是一套：修改时一条 UPDATE 整体落库，
 * 开票读到的要么是改动前整套、要么是改动后整套，不会一半旧一半新。
 */
@Getter
@Setter
public class PawnRate extends BaseEntity {

    /** 月利率/月综合费率最多五位小数（与 t_pawn_rate 的 DECIMAL(8,5) 对齐）。 */
    private static final int RATE_SCALE = 5;

    /** 折当率上限最多四位小数（与 t_pawn_rate 的 DECIMAL(5,4) 对齐）。 */
    private static final int RATIO_SCALE = 4;

    private Long id;

    /** 适用类别：只认当物那五种（JEWELRY / WATCH / ELECTRONICS / VEHICLE / OTHER）。 */
    private Category category;

    /** 月利率，小数比例（如 0.005 即月息千分之五）。 */
    private BigDecimal monthlyRate;

    /** 月综合费率，小数比例。 */
    private BigDecimal serviceRate;

    /** 折当率上限（0~1）：当金不得超过估值 × 该值。 */
    private BigDecimal maxLoanRatio;

    private RateStatus status;

    /**
     * 工厂方法：新录一条费率配置。默认启用（ENABLED），状态不接受外部指定。
     * 「同类别不重复」不在这里（跨聚合查库），由仓储在落库时挡回。
     */
    public static PawnRate open(Category category, BigDecimal monthlyRate,
                                BigDecimal serviceRate, BigDecimal maxLoanRatio) {
        PawnRate rate = new PawnRate();
        rate.applyRates(category, monthlyRate, serviceRate, maxLoanRatio);
        rate.status = RateStatus.ENABLED;
        return rate;
    }

    /**
     * 修改三个数：任一项传 null 表示该项不动，非 null 的项照常校验。
     * 只影响以后新开的票；已经在当的老票按票上快照走，不回写。
     * 停用的配置也改得（改完仍是停用，不参与开票取数）。
     */
    public void revise(BigDecimal newMonthlyRate, BigDecimal newServiceRate, BigDecimal newMaxLoanRatio) {
        applyRates(this.category,
                newMonthlyRate == null ? this.monthlyRate : newMonthlyRate,
                newServiceRate == null ? this.serviceRate : newServiceRate,
                newMaxLoanRatio == null ? this.maxLoanRatio : newMaxLoanRatio);
    }

    /**
     * 停用：该类别新开票随即卡住（开票只取 ENABLED 的配置）；
     * 已开出的票不受影响，仍按票上快照结算。
     */
    public void disable() {
        if (this.status == RateStatus.DISABLED) {
            throw new BizException("该类别费率配置已停用，不能重复停用");
        }
        this.status = RateStatus.DISABLED;
    }

    public boolean isEnabled() {
        return status == RateStatus.ENABLED;
    }

    /** 公共赋值逻辑：统一做必填/范围/小数位校验。 */
    private void applyRates(Category category, BigDecimal monthlyRate,
                            BigDecimal serviceRate, BigDecimal maxLoanRatio) {
        if (category == null) {
            throw new BizException("必须指定适用类别");
        }
        requireRate(monthlyRate, "月利率");
        requireRate(serviceRate, "月综合费率");
        requireRatio(maxLoanRatio);
        this.category = category;
        this.monthlyRate = monthlyRate;
        this.serviceRate = serviceRate;
        this.maxLoanRatio = maxLoanRatio;
    }

    /** 利率/费率：小数比例，0 ≤ 值 &lt; 1，最多五位小数（超了落库会被静默四舍五入，宁可挡回）。 */
    private static void requireRate(BigDecimal value, String name) {
        if (value == null) {
            throw new BizException(name + "不能为空");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) >= 0) {
            throw new BizException(name + "必须是 0 到 1 之间的小数比例：" + value.toPlainString());
        }
        if (value.stripTrailingZeros().scale() > RATE_SCALE) {
            throw new BizException(name + "最多五位小数：" + value.toPlainString());
        }
    }

    /** 折当率上限：0 &lt; 值 ≤ 1（写 0.7 就是最多按估值七成放当金），最多四位小数。 */
    private static void requireRatio(BigDecimal value) {
        if (value == null) {
            throw new BizException("折当率上限不能为空");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new BizException("折当率上限必须是 0 到 1 之间的小数（大于 0、不超过 1）："
                    + value.toPlainString());
        }
        if (value.stripTrailingZeros().scale() > RATIO_SCALE) {
            throw new BizException("折当率上限最多四位小数：" + value.toPlainString());
        }
    }
}
