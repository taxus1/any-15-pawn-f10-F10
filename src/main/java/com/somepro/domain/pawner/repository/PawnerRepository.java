package com.somepro.domain.pawner.repository;

import com.somepro.domain.pawner.model.Pawner;
import com.somepro.domain.pawner.model.PawnerQuery;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 当户聚合的仓储端口（领域层定义，基础设施层实现）。
 *
 * 唯一性约束（同一张身份证在未注销档案里只准一份）由 {@link #insert} / {@link #update} 的实现保证：
 * 写入临界区由 MySQL 命名锁在全实例串行化，锁内做未注销计数检查，保证并发热点登记只落一份。
 */
public interface PawnerRepository {

    /**
     * 新建档案：生成全局唯一编号（DH-年份-序号）并落库。
     * 若该身份证已存在未注销（NORMAL/FROZEN）档案，抛业务异常挡回；
     * 历史上有已注销档案不拦截（允许同证重新建档）。
     */
    Mono<Pawner> insert(Pawner pawner);

    /**
     * 修改档案（含身份证变更）：与新建共用「同身份证 + 命名锁」的临界区，
     * 存在性检查排除自身；命中未注销的他人档案时抛业务异常。
     */
    Mono<Pawner> update(Pawner pawner);

    Mono<Pawner> findById(Long id);

    Mono<Pawner> findByPawnerNo(String pawnerNo);

    /**
     * 冻结：条件更新「NORMAL → FROZEN」。已经是 FROZEN 的重复点击不再落库（幂等，
     * 头一次冻结的办理时刻/经办人保留）；CLOSED 终态抛业务异常说明情况。
     * 并发冻结/解冻靠行级条件更新串行，库里最终只落一个确定状态，不会一笔盖一笔地乱翻。
     * 返回落库后的最新聚合（幂等命中时返回的仍是冻结态）。
     */
    Mono<Pawner> freeze(Long id);

    /**
     * 解冻：条件更新「FROZEN → NORMAL」。已经是 NORMAL 的重复点击不再落库（幂等）；
     * CLOSED 终态抛业务异常说明情况（注销的不能解）。并发语义同 {@link #freeze}。
     */
    Mono<Pawner> unfreeze(Long id);

    /** 按条件翻名单；条件中 status 为 null 时只出 NORMAL/FROZEN，不出 CLOSED。 */
    Mono<PageResult<Pawner>> page(int pageNum, int pageSize, PawnerQuery query);
}
