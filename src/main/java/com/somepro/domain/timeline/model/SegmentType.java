package com.somepro.domain.timeline.model;

/**
 * 当票时间线段类型（纯领域枚举，不依赖任何框架）。
 *
 * 一张票从建档到结清，线上只可能出现这六段，顺序与业务生命周期一致：
 * <ul>
 *   <li>{@link #PAWNER_REGISTER} 当户建档：谁拿来的（当户编号对号）；</li>
 *   <li>{@link #COLLATERAL_REGISTER} 当物登记：拿来的是什么（当物编号对号）；</li>
 *   <li>{@link #TICKET_ISSUE} 当票开立：开了多少钱、起当与到期日期；</li>
 *   <li>{@link #RENEW} 续当：每次顺延之前/之后的到期日期与顺延月数；</li>
 *   <li>{@link #REDEEM} 赎当结清：计费天数、利息与综合费、应还总额；</li>
 *   <li>{@link #FORFEIT} 绝当处置：处置方式与处置回款。</li>
 * </ul>
 * 一段一种类型；一张票只该有一段建档、一段登记、一段开票，续当可有多段，
 * 赎当与绝当至多各一段且二者互斥（互斥性由时间线自洽校验钉，见 {@link TicketTimeline}）。
 */
public enum SegmentType {

    PAWNER_REGISTER("PAWNER_REGISTER", "当户建档"),
    COLLATERAL_REGISTER("COLLATERAL_REGISTER", "当物登记"),
    TICKET_ISSUE("TICKET_ISSUE", "当票开立"),
    RENEW("RENEW", "续当"),
    REDEEM("REDEEM", "赎当结清"),
    FORFEIT("FORFEIT", "绝当处置");

    private final String code;
    private final String label;

    SegmentType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }
}
