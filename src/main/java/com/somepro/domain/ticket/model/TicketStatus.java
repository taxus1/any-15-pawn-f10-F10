package com.somepro.domain.ticket.model;

import com.somepro.common.exception.BizException;

/**
 * 当票状态（纯领域枚举，不依赖任何框架）。
 * <ul>
 *   <li>{@link #ACTIVE} 在当：新开的票默认落这个状态，占着当物、锁着估值</li>
 *   <li>{@link #REDEEMED} 已赎：当户赎当结清（由赎当结算推进，本模块不直接置）</li>
 *   <li>{@link #FORFEITED} 已绝当：到期未赎走绝当处置（由绝当处置推进，本模块不直接置）</li>
 *   <li>{@link #CANCELLED} 已撤销：开错的票作废，当物回到在库</li>
 * </ul>
 * 用枚举名落库（status 列直接存这些字符串）。
 *
 * ACTIVE 是唯一「没结清」的状态：一件当物被 ACTIVE 票占着时不能再开新票；
 * REDEEMED / FORFEITED / CANCELLED 都算结清，当物随之可以再开新票。
 */
public enum TicketStatus {

    ACTIVE("ACTIVE", "在当"),
    REDEEMED("REDEEMED", "已赎"),
    FORFEITED("FORFEITED", "已绝当"),
    CANCELLED("CANCELLED", "已撤销");

    private final String code;
    private final String label;

    TicketStatus(String code, String label) {
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
     * 由外部传入值解析枚举：只认上述四个 code（大小写敏感，列里就是这么存的）。
     * 传 null 返回 null（翻票时表示不按状态筛）；传空串或其它写法都算非法入参，直接挡回。
     */
    public static TicketStatus ofCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        for (TicketStatus status : values()) {
            if (status.code.equals(trimmed)) {
                return status;
            }
        }
        throw new BizException("状态只支持 ACTIVE / REDEEMED / FORFEITED / CANCELLED：" + code);
    }
}
