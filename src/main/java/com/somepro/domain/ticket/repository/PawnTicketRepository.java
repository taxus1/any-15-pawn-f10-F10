package com.somepro.domain.ticket.repository;

import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.ticket.model.PawnTicket;
import com.somepro.domain.ticket.model.PawnTicketQuery;
import reactor.core.publisher.Mono;

/**
 * 当票聚合的仓储端口（领域层定义，基础设施层实现）。
 *
 * 两条硬约束由 {@link #insert} 的实现在写临界区里保证：
 * 1. 票号全局唯一（DP-年份-序号，一票一号）：MySQL 命名锁内取当年最大序号 +1，
 *    唯一索引兜底，撞号整段重试，不把底层冲突甩给柜台；
 * 2. 一件当物只挂一张没结清的票：锁内先点该当物有没有 ACTIVE 票再写入，
 *    两个人前后脚拿同一件东西来开票，也只落得了一张。
 * 开票与撤销都要联动当物状态（开票置已典当、撤销回在库），与当票写入同一事务落库。
 */
public interface PawnTicketRepository {

    /**
     * 开立：分配雪花 id、生成全局唯一票号（DP-年份-序号）、落库，并把当物联动为已典当。
     * 该当物已有在当票时在锁内挡回。返回回填票号与审计字段后的领域对象。
     */
    Mono<PawnTicket> insert(PawnTicket ticket);

    /**
     * 修改：票号、当户、当物、类别、估值与利率费率快照永不改，只更新当金/起当日期/当期月数/到期日期。
     * 目标不存在（含已删除）时抛业务异常。
     */
    Mono<PawnTicket> update(PawnTicket ticket);

    /**
     * 撤销：票置已撤销、当物联动回在库，同一事务落库；
     * 票已不在当（并发重复撤销或已被结清）时挡回。
     */
    Mono<PawnTicket> cancel(PawnTicket ticket);

    Mono<PawnTicket> findById(Long id);

    Mono<PawnTicket> findByTicketNo(String ticketNo);

    /** 按条件翻票；逻辑删除的不出现，稳定按 id 升序分页。 */
    Mono<PageResult<PawnTicket>> page(int pageNum, int pageSize, PawnTicketQuery query);
}
