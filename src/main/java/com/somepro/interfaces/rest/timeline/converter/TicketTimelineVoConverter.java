package com.somepro.interfaces.rest.timeline.converter;

import com.somepro.domain.timeline.model.CollateralRegisterSegment;
import com.somepro.domain.timeline.model.ForfeitSegment;
import com.somepro.domain.timeline.model.PawnerRegisterSegment;
import com.somepro.domain.timeline.model.RedeemSegment;
import com.somepro.domain.timeline.model.RenewSegment;
import com.somepro.domain.timeline.model.TicketIssueSegment;
import com.somepro.domain.timeline.model.TicketTimeline;
import com.somepro.domain.timeline.model.TimelineSegment;
import com.somepro.interfaces.rest.timeline.vo.TicketTimelineSegmentVO;
import com.somepro.interfaces.rest.timeline.vo.TicketTimelineVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 当票时间线 领域 → VO 转换器（用户接口层）。Controller 不直接把领域对象塞进 Result。
 *
 * 六类段共用一个宽 record VO，按 sealed 接口各实现类型分别装字段；
 * 无关字段一律留 null（Jackson non_null 自动省略），段类型与编号、业务时刻每段必有。
 */
public final class TicketTimelineVoConverter {

    private TicketTimelineVoConverter() {
    }

    public static TicketTimelineVO toVo(TicketTimeline timeline) {
        List<TicketTimelineSegmentVO> segments = timeline.segments().stream()
                .map(TicketTimelineVoConverter::toSegmentVo)
                .collect(Collectors.toList());
        return new TicketTimelineVO(
                timeline.ticketNo(),
                timeline.status(),
                segments,
                timeline.consistent(),
                timeline.issues());
    }

    private static TicketTimelineSegmentVO toSegmentVo(TimelineSegment seg) {
        if (seg instanceof PawnerRegisterSegment s) {
            return new TicketTimelineSegmentVO(
                    s.type().code(), s.type().label(), s.bizNo(), s.bizTime(),
                    s.deleted() ? Boolean.TRUE : null,
                    s.name(),
                    null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, null,
                    null, null, null,
                    null, null, null,
                    null, null, null);
        }
        if (seg instanceof CollateralRegisterSegment s) {
            return new TicketTimelineSegmentVO(
                    s.type().code(), s.type().label(), s.bizNo(), s.bizTime(),
                    s.deleted() ? Boolean.TRUE : null,
                    null,
                    s.category(), s.categoryLabel(), s.itemName(), s.brand(), s.appraisedValue(),
                    null, null, null, null, null, null, null,
                    null, null,
                    null, null, null,
                    null, null, null,
                    null, null, null);
        }
        if (seg instanceof TicketIssueSegment s) {
            return new TicketTimelineSegmentVO(
                    s.type().code(), s.type().label(), s.bizNo(), s.bizTime(),
                    null,
                    null,
                    s.category(), s.categoryLabel(), null, null, null,
                    s.pawnAmount(), s.appraisedValue(), s.monthlyRate(), s.serviceRate(),
                    s.startDate(), s.dueDate(), s.termMonths(),
                    s.status(), s.statusLabel(),
                    null, null, null,
                    null, null, null,
                    null, null, null);
        }
        if (seg instanceof RenewSegment s) {
            return new TicketTimelineSegmentVO(
                    s.type().code(), s.type().label(), s.bizNo(), s.bizTime(),
                    null,
                    null,
                    null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, null,
                    s.oldDueDate(), s.newDueDate(), s.extendMonths(),
                    null, null, null,
                    null, null, null);
        }
        if (seg instanceof RedeemSegment s) {
            return new TicketTimelineSegmentVO(
                    s.type().code(), s.type().label(), s.bizNo(), s.bizTime(),
                    null,
                    null,
                    null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, null,
                    null, null, null,
                    s.usedDays(), s.feeAmount(), s.totalAmount(),
                    null, null, null);
        }
        if (seg instanceof ForfeitSegment s) {
            return new TicketTimelineSegmentVO(
                    s.type().code(), s.type().label(), s.bizNo(), s.bizTime(),
                    null,
                    null,
                    null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, null,
                    null, null, null,
                    null, null, null,
                    s.disposeMethod(), s.disposeMethodLabel(), s.recoverAmount());
        }
        throw new IllegalStateException("未知的时间线段类型：" + seg);
    }
}
