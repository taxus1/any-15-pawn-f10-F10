package com.somepro.interfaces.rest.collection.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 催收工作台一行对外对象（不可变 record）。
 *
 * 柜台一屏要看到的都在这里：票号 / 当户（含电话，顺手就能拨）/ 当物 / 当前到期日 /
 * 离到期还剩几天（daysRemaining，逾期为负）/ 逾期拖了多少天（overdueDays，仅逾期票出现）/
 * 已续当几回（renewCount）。
 *
 * 客户常问两句的现成答案：
 * - redeemTotalAmount 今天来赎要还的总额（配套 redeemFeeAmount 费息、redeemUsedDays 计费天数，便于解释）；
 * - nextDueDate 今天来办续当之后的新到期日（配套 extendMonths 顺延月数）；逾期票今天续不了，此栏不出现。
 *
 * bucket 为档位：EXPIRING 快到期 / OVERDUE 已逾期。
 * 全局 Jackson 配置 non_null：null 字段（如逾期票的 nextDueDate、快到期票的 overdueDays、未登记电话）
 * 不序列化。刻意不暴露 delFlag / 审计字段。
 */
public record CollectionWorkItemVO(Long ticketId,
                                   String ticketNo,
                                   Long pawnerId,
                                   String pawnerName,
                                   String pawnerPhone,
                                   Long collateralId,
                                   String collateralName,
                                   LocalDate dueDate,
                                   String bucket,
                                   Integer daysRemaining,
                                   Integer overdueDays,
                                   Integer renewCount,
                                   BigDecimal redeemTotalAmount,
                                   BigDecimal redeemFeeAmount,
                                   Integer redeemUsedDays,
                                   LocalDate nextDueDate,
                                   Integer extendMonths) implements Serializable {
}
