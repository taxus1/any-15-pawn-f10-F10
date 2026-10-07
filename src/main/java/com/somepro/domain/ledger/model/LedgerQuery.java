package com.somepro.domain.ledger.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.collateral.model.Category;

import java.time.LocalDate;

/**
 * 台账翻表条件（不可变值对象）：按自然月、按类别筛选。
 *
 * 口径：
 * - {@code month} 为 null 表示不按月筛 —— 从最早一笔业务所在自然月铺到当前月，
 *   中间没有业务的月份也要补出零行；非 null 只看这一个月（该月全是零也照样返回零行）。
 * - {@code category} 为 null 表示不按类别筛 —— 五个类别格子都给；非 null 只看这一类别。
 *
 * @param month    自然月（yyyy-MM 字符串，归月只看办理时刻落在哪个自然月），可空
 * @param category 类别，可空
 * @param today    业务今天（行里时区，由应用层补，不接受前端指定）：无月份筛选时的右端点
 */
public record LedgerQuery(String month, Category category, LocalDate today) {

    /** 月份只认 yyyy-MM；传别的写法（含 yyyy-M、2026-13、2026-02-30）一律挡回。 */
    public static LedgerQuery of(String month, Category category, LocalDate today) {
        if (today == null) {
            throw new IllegalArgumentException("业务今天日期缺失，无法生成台账翻表条件");
        }
        String normalizedMonth = null;
        if (month != null) {
            String trimmed = month.trim();
            if (!trimmed.isEmpty()) {
                LocalDate parsed;
                try {
                    // LocalDate.parse('yyyy-MM-dd')：先补成月初再解析，非法月/日直接抛
                    parsed = LocalDate.parse(trimmed + "-01");
                } catch (Exception e) {
                    throw new BizException("月份只支持 yyyy-MM 写法：" + month);
                }
                normalizedMonth = trimmed;
            }
        }
        return new LedgerQuery(normalizedMonth, category, today);
    }
}
