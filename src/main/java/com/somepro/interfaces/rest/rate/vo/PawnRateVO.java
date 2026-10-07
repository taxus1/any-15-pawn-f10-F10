package com.somepro.interfaces.rest.rate.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费率配置对外对象（不可变 record）：录入/修改/查看/停用/翻清单共用。
 *
 * 每行都带类别（code + 中文名），柜面对清单时一眼对得上是哪类东西的口径。
 * updateTime 也外放：费率表改没改过、什么时候改的，直接决定新开票抄哪套数，
 * 是有意暴露的口径信息。刻意不暴露 delFlag / createBy / updateBy 等内部字段。
 */
public record PawnRateVO(Long id,
                         String category,
                         String categoryLabel,
                         BigDecimal monthlyRate,
                         BigDecimal serviceRate,
                         BigDecimal maxLoanRatio,
                         String status,
                         String statusLabel,
                         LocalDateTime createTime,
                         LocalDateTime updateTime) implements Serializable {
}
