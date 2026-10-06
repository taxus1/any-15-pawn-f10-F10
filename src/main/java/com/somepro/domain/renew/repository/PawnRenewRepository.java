package com.somepro.domain.renew.repository;

import com.somepro.domain.renew.model.PawnRenew;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 续当仓储端口（领域层定义，基础设施层实现）。
 *
 * {@link #insert} 的实现在写临界区（MySQL 命名锁 + 同一事务）里保证三件事：
 * 1. 续当单号全局唯一（XD-年份-序号，一单一号）：锁内取当年最大序号 +1，唯一索引兜底，
 *    撞号整段重试，不把底层冲突甩给柜台；
 * 2. 同一张票同一时点只续出一条：锁内对当票做条件更新（id + 到期日期仍是办理前那一天）
 *    才推进到期日期 —— 柜台手快把同一笔续当重复递进来，后到那笔条件已不成立，挡回，
 *    续当记录里不会平白多出一笔；票在办理瞬间被撤销/结清（状态离开 ACTIVE）同样挡回；
 * 3. 当票到期日期推进（状态仍留在当）与续当登记写入同一事务，要么一起成、要么一起回滚。
 */
public interface PawnRenewRepository {

    /**
     * 办理落库：分配雪花 id、生成全局唯一续当单号（XD-年份-序号），
     * 在同一事务内把当票到期日期从 oldDueDate 推进到 newDueDate（票仍留在当），再写续当登记。
     * 当票状态已变化或到期日期已被人推进（重复递交/并发办理）时抛业务异常，两边都不落。
     */
    Mono<PawnRenew> insert(PawnRenew renew);

    Mono<PawnRenew> findById(Long id);

    Mono<PawnRenew> findByRenewNo(String renewNo);

    /** 按当票翻续当记录；逻辑删除的不出现，稳定按 id 升序分页，每行带续当单号。 */
    Mono<PageResult<PawnRenew>> pageByTicket(int pageNum, int pageSize, Long ticketId);
}
