package com.somepro.interfaces.rest.timeline.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 当票时间线对外对象（不可变 record）：一张票从头到尾的一条线。
 *
 * - events 按业务发生时刻从新到旧排，最新的那段排最前；
 * - consistent 整条线是否自圆其说；对不上的地方逐条记在 issues 里（哪一环断了、
 *   哪笔数对不上），线本身照摆 —— 核账要看到的就是哪里串了；
 * - 查无此票（含票已销掉）时 events 为空、issues 为空，不报错。
 */
public record TicketTimelineVO(String ticketNo,
                               String ticketStatus,
                               String ticketStatusLabel,
                               List<TimelineEventVO> events,
                               boolean consistent,
                               List<String> issues) implements Serializable {
}
