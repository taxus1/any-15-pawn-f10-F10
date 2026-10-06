package com.somepro.interfaces.rest.renew.vo;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 续当单对外对象（不可变 record）：办理/查看/翻记录共用。
 *
 * 每行都带 renewNo（XD-年份-序号），方便柜台跟续当凭证对号；
 * oldDueDate/newDueDate/extendMonths 是办理当下定格的快照，原样外放。
 * 刻意不暴露 delFlag / createBy / updateBy 等内部字段。
 */
public record PawnRenewVO(Long id,
                          String renewNo,
                          Long ticketId,
                          LocalDate oldDueDate,
                          LocalDate newDueDate,
                          Integer extendMonths,
                          LocalDateTime renewedAt,
                          LocalDateTime createTime) implements Serializable {
}
