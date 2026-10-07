package com.somepro.domain.timeline.model;

import java.time.LocalDateTime;

/**
 * 时间线第一段：当户建档（谁拿来的）。
 *
 * 编号带当户编号 pawnerNo（DH-年份-序号）；建档时的姓名原样带出。
 * 业务时刻取档案 create_time —— 建档没有单独的「办理时刻」列，落账时刻即建档时刻。
 * 当户档案若已被逻辑删除（当票查链路是 LEFT JOIN 出来的兜底情形），
 * 段仍要在线上、不把历史抹掉，以 deleted=true 标出。
 */
public record PawnerRegisterSegment(Long id,
                                    String pawnerNo,
                                    String name,
                                    LocalDateTime registeredAt,
                                    boolean deleted) implements TimelineSegment {

    @Override
    public SegmentType type() {
        return SegmentType.PAWNER_REGISTER;
    }

    @Override
    public String bizNo() {
        return pawnerNo;
    }

    @Override
    public LocalDateTime bizTime() {
        return registeredAt;
    }

    @Override
    public Long segmentId() {
        return id;
    }
}
