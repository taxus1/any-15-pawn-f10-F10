package com.somepro.domain.timeline.model;

/**
 * 时间线段落类型（纯领域枚举，不依赖任何框架）。
 *
 * 一张当票从头到尾会经过的六段，业务先后次序固定：
 * 当户建档 → 当物登记 → 当票开立 → 续当（可多次）→ 赎当结清 / 绝当处置（二选一）。
 *
 * sequence 就是这次序的编号：同一时间线按业务发生时刻从新到旧排，
 * 恰好落在同一时刻的段落拿它定稳定次序（业务上越晚的段排越前）。
 */
public enum TimelineEventType {

    PAWNER_REGISTER(1, "当户建档"),
    COLLATERAL_REGISTER(2, "当物登记"),
    TICKET_ISSUE(3, "当票开立"),
    RENEW(4, "续当"),
    REDEEM(5, "赎当结清"),
    FORFEIT(6, "绝当处置");

    private final int sequence;
    private final String label;

    TimelineEventType(int sequence, String label) {
        this.sequence = sequence;
        this.label = label;
    }

    /** 业务先后次序：数字越小业务上越早。 */
    public int sequence() {
        return sequence;
    }

    public String label() {
        return label;
    }
}
