package com.somepro.interfaces.rest.forfeit.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 绝当处置单对外对象（不可变 record）：办理成功回单用，只含处置单落库的列。
 *
 * 每行都带 forfeitNo（JD-年份-序号），方便柜台与拍卖行、寄卖行的回单对号。
 * 刻意不暴露 delFlag / createBy / updateBy 等内部字段。
 * 欠款本息与盈亏在对账视图 {@link ForfeitViewVO} 上，办理回单这里不带。
 */
public record PawnForfeitVO(Long id,
                            String forfeitNo,
                            Long ticketId,
                            Long collateralId,
                            LocalDateTime forfeitedAt,
                            String disposeMethod,
                            BigDecimal recoverAmount,
                            LocalDateTime createTime) implements Serializable {
}
