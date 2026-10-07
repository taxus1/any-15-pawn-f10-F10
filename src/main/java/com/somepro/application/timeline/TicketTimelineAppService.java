package com.somepro.application.timeline;

import com.somepro.common.exception.BizException;
import com.somepro.domain.timeline.model.TicketTimeline;
import com.somepro.domain.timeline.repository.TicketTimelineRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 当票时间线应用服务：输入一个当票号，回一份从头到尾的时间线。
 *
 * 编排只有一步：仓储按票号取回原料，领域对象 {@link TicketTimeline#assemble} 负责
 * 摆段、排序与自洽校验。查无此票（含票本身已销掉）给一份空时间线，不报错。
 */
@Service
public class TicketTimelineAppService {

    private final TicketTimelineRepository ticketTimelineRepository;

    public TicketTimelineAppService(TicketTimelineRepository ticketTimelineRepository) {
        this.ticketTimelineRepository = ticketTimelineRepository;
    }

    /**
     * 按当票号串时间线：当户建档、当物登记、当票开立、每次续当、赎当或绝当一段不缺，
     * 按业务发生时刻从新到旧排；哪一环对不上在 issues 里记明。
     *
     * @param ticketNo 当票号（DP-编号）；空白挡回，查无此票给空时间线
     */
    public Mono<TicketTimeline> timeline(String ticketNo) {
        if (ticketNo == null || ticketNo.isBlank()) {
            return Mono.error(new BizException("必须指定要查的当票号"));
        }
        String no = ticketNo.trim();
        return ticketTimelineRepository.findSourceByTicketNo(no)
                .map(TicketTimeline::assemble)
                // 查无此票（含票已销掉）：给一份空时间线，不报错
                .defaultIfEmpty(TicketTimeline.empty(no));
    }
}
