package com.somepro.domain.collection.model;

import com.somepro.common.exception.BizException;

/**
 * 催收档位（纯领域枚举，不依赖任何框架）。清单只认还在当的票，按这张票库里现在挂的到期日子分两档：
 * <ul>
 *   <li>{@link #EXPIRING} 快到期：从今天起往后七天以内到期（含今天、含第七天）；</li>
 *   <li>{@link #OVERDUE} 已逾期：到期日子已经过了今天（今天之前）。</li>
 * </ul>
 * 注意两档互斥且以【今天】为界：到期日 = 今天 落在快到期（剩余 0 天、可当天续当），不算逾期。
 */
public enum CollectionBucket {

    EXPIRING("EXPIRING", "快到期"),
    OVERDUE("OVERDUE", "已逾期");

    private final String code;
    private final String label;

    CollectionBucket(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    /**
     * 由外部传入值解析枚举：只认上述两个 code（大小写敏感）。
     * 传 null 表示不按档位挑（两拨票都要）；传空串或其它写法都算非法入参，直接挡回。
     */
    public static CollectionBucket ofCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        for (CollectionBucket bucket : values()) {
            if (bucket.code.equals(trimmed)) {
                return bucket;
            }
        }
        throw new BizException("催收档位只支持 EXPIRING（快到期）/ OVERDUE（已逾期）：" + code);
    }
}
