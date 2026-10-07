package com.somepro.domain.collection.model;

import java.time.LocalDate;

/**
 * 催收工作台翻台条件（不可变值对象）。
 *
 * 清单口径在本对象上写死：只翻还在当（ACTIVE）的票，到期日落在「逾期（到期日 &lt; 今天）」
 * 与「快到期（今天 ≤ 到期日 ≤ 今天 + 7 天）」两档之内；已赎 / 已绝当 / 已撤销 / 打了删除标记的
 * 票一概不进。
 *
 * @param today  业务今天（行里时区，由应用层按服务端日期补，不接受前端指定）
 * @param bucket 档位筛选；null 表示两拨票都要，非 null 只翻这一档
 */
public record CollectionQuery(LocalDate today, CollectionBucket bucket) {

    /** 快到期窗口远端（含）：今天 + 7 天。 */
    public LocalDate expiringDeadline() {
        return today.plusDays(CollectionWorkItem.EXPIRING_WINDOW_DAYS);
    }

    public static CollectionQuery of(LocalDate today, CollectionBucket bucket) {
        if (today == null) {
            throw new IllegalArgumentException("业务今天日期缺失，无法生成催收翻台条件");
        }
        return new CollectionQuery(today, bucket);
    }
}
