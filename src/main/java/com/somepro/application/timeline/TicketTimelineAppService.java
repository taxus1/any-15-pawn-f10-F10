package com.somepro.application.timeline;

import com.somepro.domain.timeline.model.TicketTimeline;
import com.somepro.domain.timeline.repository.TicketTimelineRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 当票时间线应用服务：编排「拿一张当票号把整条线倒出来」这一个用例。
 *
 * 客户来吵 / 上面核账时，输入当票号，回这张票从当户建档、当物登记、开票、
 * 每一次续当到赎当结清或绝当处置的完整时间线（按业务时刻从新到旧），
 * 并带自洽核对结论。出入参用领域对象，不认识 PO 与 VO；全程只读，不动任何票与单据。
 *
 * 号不存在 / 当票已销 → 空结果（empty），由接口层转成 data=null，不报错。
 */
@Service
public class TicketTimelineAppService {

    private final TicketTimelineRepository ticketTimelineRepository;

    public TicketTimelineAppService(TicketTimelineRepository ticketTimelineRepository) {
        this.ticketTimelineRepository = ticketTimelineRepository;
    }

    /**
     * 按当票号倒整条时间线。
     *
     * @param ticketNo 当票号；空白入参等同查不到，交空结果
     * @return 排好序、核过账的时间线；查无此票时为 empty
     */
    public Mono<TicketTimeline> timeline(String ticketNo) {
        return ticketTimelineRepository.findByTicketNo(ticketNo);
    }
}
