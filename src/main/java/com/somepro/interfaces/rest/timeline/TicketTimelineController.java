package com.somepro.interfaces.rest.timeline;

import com.somepro.application.timeline.TicketTimelineAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.timeline.converter.TicketTimelineVoConverter;
import com.somepro.interfaces.rest.timeline.vo.TicketTimelineVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 当票时间线用户接口层：输入一个当票号，回一份从头到尾的时间线。
 *
 * 只做协议适配（参数解析、VO 转换、Result 包装），业务编排在 {@link TicketTimelineAppService}。
 * 接口只读，不动任何票与业务单据。
 */
@RestController
@RequestMapping("/api/ticket")
public class TicketTimelineController {

    private final TicketTimelineAppService ticketTimelineAppService;

    public TicketTimelineController(TicketTimelineAppService ticketTimelineAppService) {
        this.ticketTimelineAppService = ticketTimelineAppService;
    }

    /**
     * 倒线：
     * - 线上按业务发生时刻从新到旧排：当户建档、当物登记、当票开立、每一次续当、
     *   赎当结清或绝当处置，每段带各自编号与当初落库的原值；
     * - consistent / issues 给出整线自洽核对结论（续当链、赎当本息、赎当/绝当互斥）；
     * - 已销掉的续当 / 赎当 / 绝当不出现；没结清、已赎、已绝当的票都照查；
     * - 当票号不存在、或当票本身已销：data 为 null（空结果），不报错。
     */
    @GetMapping("/timeline")
    public Mono<Result<TicketTimelineVO>> timeline(@RequestParam String ticketNo) {
        return ticketTimelineAppService.timeline(ticketNo)
                .map(TicketTimelineVoConverter::toVo)
                .map(Result::ok)
                .defaultIfEmpty(Result.ok());
    }
}
