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
 * 只做协议适配（参数解析、VO 转换、Result 包装），串线编排在 {@link TicketTimelineAppService}。
 * 客户来吵或上面来核账时用：一张票从谁拿来什么东西、开了多少钱、续过几回、
 * 最后是赎了还是绝当了，一条线摆清；哪一环对不上，issues 里记着。
 */
@RestController
@RequestMapping("/api/timeline")
public class TimelineController {

    private final TicketTimelineAppService ticketTimelineAppService;

    public TimelineController(TicketTimelineAppService ticketTimelineAppService) {
        this.ticketTimelineAppService = ticketTimelineAppService;
    }

    /**
     * 按当票号串时间线：当户建档、当物登记、当票开立、每次续当、赎当或绝当一段不缺，
     * 各段带自己的编号与当时落库的数，按业务发生时刻从新到旧排。
     * 查无此票（含票已销掉）回一份空时间线，不报错。
     */
    @GetMapping
    public Mono<Result<TicketTimelineVO>> timeline(@RequestParam(required = false) String ticketNo) {
        return ticketTimelineAppService.timeline(ticketNo)
                .map(TicketTimelineVoConverter::toVo)
                .map(Result::ok);
    }
}
