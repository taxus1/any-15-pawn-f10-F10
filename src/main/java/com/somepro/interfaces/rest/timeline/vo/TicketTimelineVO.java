package com.somepro.interfaces.rest.timeline.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 一张当票的完整时间线（对外 VO，不可变 record）。
 *
 * - ticketNo 当票号（查询入参原样回带）；status 票走到今天的结果码；
 * - segments 时间线各段，已按业务发生时刻从新到旧排好（同刻按固定稳定次序）；
 * - consistent 线上各段账是否自洽；issues 对不上的具体问题（人话），自洽时为空数组。
 *
 * 查无此票（号不存在 / 当票已销）时整个 data 为 null，由调用方按空结果处理。
 */
public record TicketTimelineVO(String ticketNo,
                               String status,
                               List<TicketTimelineSegmentVO> segments,
                               boolean consistent,
                               List<String> issues) implements Serializable {
}
