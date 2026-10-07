package com.somepro.domain.forfeit.model;

import com.somepro.domain.shared.model.PawnSettlement;
import com.somepro.domain.ticket.model.PawnTicket;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 绝当处置单翻单一行（只读领域视图，不可变 record）：处置单本身 + 对账要摆在一起的三笔账。
 *
 * 这不是新聚合、不往 t_pawn_forfeit 落任何列（建表口径不动）：处置单列照表带出，
 * 票面快照随当票 JOIN 出来，欠款与盈亏在翻单【当下】照固定口径现算 ——
 * 用的是办理那一刻定格的 forfeitedAt 与票面快照，所以翻十遍、什么时候翻，数都一样。
 *
 * 三笔账的口径（柜台跟拍卖行、寄卖行对账一眼看清）：
 * - owedAmount 到处置那一天为止这张票还欠行里的本息合计：真要是那天客户来赎，柜台该收的就是这一笔。
 *   算法照行里赎当办理的同一套（{@link PawnSettlement#settle}），本类绝不另立；
 *   配套 owedFeeAmount（利息与综合费）/ owedUsedDays（计费天数）便于解释这笔钱怎么来的；
 * - recoverAmount 处置回款，实际到手（处置单上原样带出）；
 * - profitLossAmount 差额 = 处置回款 − 欠款：回款不够为负记亏、多出来为正记盈、恰好相抵为持平
 *   （profitLoss：LOSS 亏 / PROFIT 盈 / BREAK_EVEN 持平）。
 *
 * 每行必带 forfeitNo（JD-年份-序号）与 ticketNo，方便和拍卖行、寄卖行的回单对号；
 * 顺带带出当物编号与名称，押的哪件东西不必再回头查。
 */
public record ForfeitView(Long id,
                          String forfeitNo,
                          Long ticketId,
                          String ticketNo,
                          Long collateralId,
                          String itemNo,
                          String itemName,
                          LocalDateTime forfeitedAt,
                          String disposeMethod,
                          String disposeMethodLabel,
                          BigDecimal recoverAmount,
                          int owedUsedDays,
                          BigDecimal owedFeeAmount,
                          BigDecimal owedAmount,
                          BigDecimal profitLossAmount,
                          String profitLoss,
                          LocalDateTime createTime) {

    /**
     * 由处置单与库里最新票面（FORFEITED 历史票）、当物档案拼一行。
     *
     * @param forfeit      处置单领域对象（forfeitedAt / 处置方式 / 回款以它为准）
     * @param ticket       按票面快照重建的当票（当金、起当日期、利率费率快照以它为准）
     * @param itemNo       当物编号（当物档案缺失时可为 null）
     * @param itemName     当物名称（当物档案缺失时可为 null）
     */
    public static ForfeitView assemble(PawnForfeit forfeit, PawnTicket ticket,
                                       String itemNo, String itemName) {
        if (forfeit == null || forfeit.getId() == null) {
            throw new IllegalArgumentException("绝当翻单行必须对应一张处置单");
        }
        if (ticket == null || ticket.getId() == null) {
            throw new IllegalArgumentException("绝当翻单行必须带出对应当票");
        }
        if (forfeit.getForfeitedAt() == null) {
            throw new IllegalArgumentException("绝当处置时刻缺失，无法结算欠款");
        }

        // 欠款：照赎当办理同一套口径，算到「处置那一天」为止客户该还的本息。
        // 用处置时刻定格 + 票面快照，翻单时点不影响金额；票已绝当也照算（settle 不卡票状态）。
        PawnSettlement owed = PawnSettlement.settle(
                ticket.getPawnAmount(), ticket.getMonthlyRate(), ticket.getServiceRate(),
                ticket.getStartDate(), forfeit.getForfeitedAt().toLocalDate());

        // 差额 = 回款 − 欠款：负亏正盈零持平。
        BigDecimal diff = forfeit.getRecoverAmount().subtract(owed.totalAmount());
        String result = diff.compareTo(BigDecimal.ZERO) > 0 ? "PROFIT"
                : diff.compareTo(BigDecimal.ZERO) < 0 ? "LOSS" : "BREAK_EVEN";

        DisposeMethod method = forfeit.getDisposeMethod();
        return new ForfeitView(
                forfeit.getId(),
                forfeit.getForfeitNo(),
                forfeit.getTicketId(),
                ticket.getTicketNo(),
                forfeit.getCollateralId(),
                itemNo,
                itemName,
                forfeit.getForfeitedAt(),
                method == null ? null : method.code(),
                method == null ? null : method.label(),
                forfeit.getRecoverAmount(),
                owed.usedDays(),
                owed.feeAmount(),
                owed.totalAmount(),
                diff,
                result,
                forfeit.getCreateTime());
    }
}
