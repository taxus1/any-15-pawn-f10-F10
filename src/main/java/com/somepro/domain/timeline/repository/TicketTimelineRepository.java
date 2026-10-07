package com.somepro.domain.timeline.repository;

import com.somepro.domain.timeline.model.TicketTimeline;
import reactor.core.publisher.Mono;

/**
 * 当票时间线查询端口（领域层）：只读，不动任何票与业务单据。
 *
 * 实现方要保证：
 * - 输入当票号，回这张票建档 / 登记 / 开票 / 每一次续当 / 赎当或绝当整条线，一段不缺；
 * - 各段数值照各表当初落库的原值带出，不按当前费率配置重算；
 * - 已打删除标记（del_flag=1）的当票查不到（返回空 Mono）；当票被销，不再拿它的号查；
 * - 已销掉的续当 / 赎当 / 绝当不串进来；没结清的、走过绝当的、已赎回的都照样能查；
 * - 压根不存在的当票号返回空 Mono，不报错。
 */
public interface TicketTimelineRepository {

    /**
     * 按当票号倒整条时间线。
     *
     * @param ticketNo 当票号（DP-年份-序号）
     * @return 排好序、核过账的时间线；当票不存在或已被逻辑删除时为 empty
     */
    Mono<TicketTimeline> findByTicketNo(String ticketNo);
}
