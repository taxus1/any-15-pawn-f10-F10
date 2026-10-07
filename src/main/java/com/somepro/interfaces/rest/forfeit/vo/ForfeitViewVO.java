package com.somepro.interfaces.rest.forfeit.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 绝当处置单对账视图对外对象（不可变 record）：查看详情 / 翻单共用。
 *
 * 除处置单本身各列外，把对账要摆在一起的数都摆出来，柜台跟拍卖行、寄卖行对账一眼看清：
 * - forfeitNo / ticketNo / itemNo / itemName：处置单号、票号、当物编号名称，拿来跟回单对号；
 * - owedAmount：到处置那天为止这张票还欠行里的本息（真要是那天客户来赎柜台该收的就是这笔，
 *   口径照赎当办理同一套）；配套 owedFeeAmount 利息与综合费、owedUsedDays 计费天数；
 * - recoverAmount：处置回款（实际到手）；
 * - profitLossAmount：差额 = 回款 − 欠款，负亏正盈零持平；profitLoss：LOSS 亏 / PROFIT 盈 / BREAK_EVEN 持平。
 *
 * 全局 Jackson 配置 non_null，null 字段（如当物档案缺失的 itemNo/itemName）不序列化。
 * 刻意不暴露 delFlag / 审计字段。
 */
public record ForfeitViewVO(Long id,
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
                            Integer owedUsedDays,
                            BigDecimal owedFeeAmount,
                            BigDecimal owedAmount,
                            BigDecimal profitLossAmount,
                            String profitLoss,
                            LocalDateTime createTime) implements Serializable {
}
