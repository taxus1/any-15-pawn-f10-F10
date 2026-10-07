package com.somepro.domain.timeline.repository;

import com.somepro.domain.timeline.model.TimelineSource;
import reactor.core.publisher.Mono;

/**
 * 当票时间线的仓储端口（领域层定义，基础设施层实现）。
 *
 * 只读不写：按票号把串一条线要的原料一次取回。逻辑删除由 @TableLogic 兜底 ——
 * 已销掉的续当/赎当/绝当取不出来（不进时间线），已销掉的当票本身也取不出来
 * （查无此票，调用方给空结果）。
 */
public interface TicketTimelineRepository {

    /**
     * 按当票号取原料：票 + 当户 + 当物 + 该票名下全部未销掉的续当/赎当/绝当。
     * 票不存在（含已销掉）时返回空 Mono，由应用层给空时间线，不报错。
     */
    Mono<TimelineSource> findSourceByTicketNo(String ticketNo);
}
