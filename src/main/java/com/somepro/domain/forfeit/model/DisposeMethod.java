package com.somepro.domain.forfeit.model;

import com.somepro.common.exception.BizException;

/**
 * 绝当处置方式（纯领域枚举，不依赖任何框架）。
 * <ul>
 *   <li>{@link #AUCTION} 拍卖：交拍卖行公开竞价处置</li>
 *   <li>{@link #CONSIGN} 变卖：交寄卖行等渠道折价变卖</li>
 *   <li>{@link #WRITE_OFF} 核销：卖不出 / 无价值，行里按账销案</li>
 * </ul>
 * 用枚举名落库（dispose_method 列直接存这些字符串）。
 */
public enum DisposeMethod {

    AUCTION("AUCTION", "拍卖"),
    CONSIGN("CONSIGN", "变卖"),
    WRITE_OFF("WRITE_OFF", "核销");

    private final String code;
    private final String label;

    DisposeMethod(String code, String label) {
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
     * 由外部传入值解析枚举：只认上述三个 code（大小写敏感，列里就是这么存的）。
     * 传 null 返回 null（翻单时表示不按处置方式筛）；传空串或其它写法都算非法入参，直接挡回。
     */
    public static DisposeMethod ofCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        for (DisposeMethod method : values()) {
            if (method.code.equals(trimmed)) {
                return method;
            }
        }
        throw new BizException("处置方式只支持 AUCTION 拍卖 / CONSIGN 变卖 / WRITE_OFF 核销：" + code);
    }
}
