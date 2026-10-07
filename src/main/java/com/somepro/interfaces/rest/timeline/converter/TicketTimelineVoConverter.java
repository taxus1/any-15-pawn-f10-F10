package com.somepro.interfaces.rest.timeline.converter;

import com.somepro.domain.timeline.model.TicketTimeline;
import com.somepro.domain.timeline.model.TimelineEvent;
import com.somepro.domain.timeline.model.TimelineEventType;
import com.somepro.interfaces.rest.timeline.vo.TicketTimelineVO;
import com.somepro.interfaces.rest.timeline.vo.TimelineEventVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 时间线领域对象 → VO 转换器（用户接口层）。Controller 不直接把领域对象塞进 Result。
 *
 * 枚举在 VO 里展开成 code + label 两个字段（类别/品相/处置方式），
 * 段类型展开成 eventType + eventTypeLabel，前端照着摆即可。
 */
public final class TicketTimelineVoConverter {

    private TicketTimelineVoConverter() {
    }

    public static TicketTimelineVO toVo(TicketTimeline timeline) {
        List<TimelineEventVO> events = timeline.events().stream()
                .map(TicketTimelineVoConverter::toEventVo)
                .collect(Collectors.toList());
        return new TicketTimelineVO(
                timeline.ticketNo(),
                timeline.ticketStatus() == null ? null : timeline.ticketStatus().code(),
                timeline.ticketStatus() == null ? null : timeline.ticketStatus().label(),
                events,
                timeline.consistent(),
                timeline.issues());
    }

    public static TimelineEventVO toEventVo(TimelineEvent event) {
        TimelineEventType type = event.type();
        return new TimelineEventVO(
                type.name(),
                type.label(),
                event.bizTime(),
                event.bizNo(),
                event.pawnerName(),
                event.pawnerIdCard(),
                event.pawnerPhone(),
                event.itemName(),
                event.category() == null ? null : event.category().code(),
                event.category() == null ? null : event.category().label(),
                event.brand(),
                event.conditionLevel() == null ? null : event.conditionLevel().code(),
                event.conditionLevel() == null ? null : event.conditionLevel().label(),
                event.appraisedValue(),
                event.pawnAmount(),
                event.startDate(),
                event.dueDate(),
                event.termMonths(),
                event.oldDueDate(),
                event.newDueDate(),
                event.extendMonths(),
                event.usedDays(),
                event.feeAmount(),
                event.totalAmount(),
                event.disposeMethod() == null ? null : event.disposeMethod().code(),
                event.disposeMethod() == null ? null : event.disposeMethod().label(),
                event.recoverAmount());
    }
}
