package com.somepro.domain.shared.model;

import com.somepro.common.exception.BizException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 赎当应还本息试算（纯领域值逻辑，不落任何状态）。
 *
 * 这是行里「真要是那天客户来赎，柜台该收多少钱」的唯一口径 —— 赎当办理与绝当翻单都走这里，
 * 不允许任何模块另立一套，不然柜台跟拍卖行、寄卖行对账时两笔数对不上。
 *
 * 规矩（与赎当办理聚合 PawnRedeem 历来的算法分毫不差）：
 * 1. 日费率 =（票面上的月利率快照 + 月综合费率快照）÷ 30 —— 月口径折天口径固定除以 30；
 * 2. 计费天数 = 结算日期 − 起当日期的自然日数，不足一天按一天算（至少 1 天）；
 *    晚于到期日期来赎/算到绝当日也照实际天数算，不额外加罚；
 * 3. 费用（利息与综合费合计）= 当金 × 日费率 × 计费天数；
 *    应还总额 = 当金 + 费用；金额一律保留两位小数、四舍五入；
 * 4. 利率费率一律照票面上的快照算 —— 那是开票当时抄下来的，不读现在的费率配置，
 *    不然老票的账会跟着新配置乱跳。
 *
 * @param usedDays    计费天数（至少 1）
 * @param feeAmount   利息与综合费合计（元，两位小数）
 * @param totalAmount 应还总额 = 当金 + 费用（元，两位小数）
 */
public record PawnSettlement(int usedDays, BigDecimal feeAmount, BigDecimal totalAmount) {

    /** 日费率分母：月利率、月综合费率都是「每月」口径，折成每天除以 30。 */
    private static final BigDecimal DAYS_PER_MONTH = BigDecimal.valueOf(30);
    /** 中间除法（日费率）保留足够多位，只在最后金额上两位四舍五入。 */
    private static final int RATE_SCALE = 10;

    /**
     * 按票面快照算到指定日期为止客户该还的本息。
     *
     * @param pawnAmount    票面当金快照（元），必须为正数
     * @param monthlyRate   票面月利率快照
     * @param serviceRate   票面月综合费率快照
     * @param startDate     票面起当日期
     * @param settleDate    结算日期（赎当办理日 / 绝当处置日）
     */
    public static PawnSettlement settle(BigDecimal pawnAmount, BigDecimal monthlyRate, BigDecimal serviceRate,
                                        LocalDate startDate, LocalDate settleDate) {
        if (pawnAmount == null || pawnAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("当票当金异常，无法结算本息");
        }
        if (monthlyRate == null || serviceRate == null) {
            throw new BizException("当票利率费率快照缺失，无法结算本息");
        }
        if (startDate == null) {
            throw new BizException("当票起当日期缺失，无法结算本息");
        }
        if (settleDate == null) {
            throw new BizException("结算日期缺失，无法结算本息");
        }

        // 计费天数：结算日期 − 起当日期的自然日数，不足一天按一天算。
        // 晚于到期日期也算到实际天数，不加罚 —— 赎当、绝当欠款同一把尺子。
        int usedDays = (int) Math.max(1L, ChronoUnit.DAYS.between(startDate, settleDate));

        // 日费率 =（月利率快照 + 月综合费率快照）÷ 30；费用 = 当金 × 日费率 × 计费天数。
        BigDecimal dailyRate = monthlyRate.add(serviceRate)
                .divide(DAYS_PER_MONTH, RATE_SCALE, RoundingMode.HALF_UP);
        BigDecimal fee = pawnAmount
                .multiply(dailyRate)
                .multiply(BigDecimal.valueOf(usedDays))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = pawnAmount.add(fee).setScale(2, RoundingMode.HALF_UP);
        return new PawnSettlement(usedDays, fee, total);
    }
}
