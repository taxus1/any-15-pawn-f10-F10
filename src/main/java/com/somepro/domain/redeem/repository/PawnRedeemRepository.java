package com.somepro.domain.redeem.repository;

import com.somepro.domain.redeem.model.PawnRedeem;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 赎当仓储端口（领域层定义，基础设施层实现）。
 *
 * {@link #insert} 的实现在写临界区（MySQL 命名锁 + 同一事务）里保证三件事：
 * 1. 赎当单号全局唯一（SD-年份-序号，一单一号）：锁内取当年最大序号 +1，唯一索引兜底，
 *    撞号整段重试，不把底层冲突甩给柜台；
 * 2. 同一张票只赎一回：锁内对当票做条件更新（id + 状态仍是 ACTIVE 才翻成 REDEEMED）——
 *    柜台手快把同一笔赎当重复递进来，后到那笔条件已不成立，挡回，
 *    赎当记录里不会平白多出一笔；票在办理瞬间被撤销/绝当（状态离开 ACTIVE）同样挡回；
 * 3. 当票「在当 → 已赎」、当物「已典当 → 已赎回」与赎当结算写入同一事务，
 *    两处状态一起翻，要么一起成、要么一起回滚，不会只翻一处。
 */
public interface PawnRedeemRepository {

    /**
     * 办理落库：分配雪花 id、生成全局唯一赎当单号（SD-年份-序号），
     * 在同一事务内把当票从在当翻成已赎、把票押的当物从已典当翻成已赎回，再写赎当结算。
     * 当票或当物状态已变化（重复递交/并发办理）时抛业务异常，几处都不落。
     */
    Mono<PawnRedeem> insert(PawnRedeem redeem);

    Mono<PawnRedeem> findById(Long id);

    Mono<PawnRedeem> findByRedeemNo(String redeemNo);

    /** 按当票翻赎当记录；逻辑删除的不出现，稳定按 id 升序分页，每行带赎当单号。 */
    Mono<PageResult<PawnRedeem>> pageByTicket(int pageNum, int pageSize, Long ticketId);
}
