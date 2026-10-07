package com.somepro.domain.rate.repository;

import com.somepro.domain.collateral.model.Category;
import com.somepro.domain.rate.model.PawnRate;
import com.somepro.domain.rate.model.PawnRateQuery;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 费率配置仓储端口（领域层定义，基础设施层实现）。
 *
 * {@link #insert} 的实现保证「一个类别只留一条配置」：t_pawn_rate.uk_category 唯一索引兜底，
 * 并发同时录同一类别也只有一条落得进去，重复来的（含已停用再重录）都挡回，
 * 不会出现同一类别同时挂着两条能用的配置。
 *
 * {@link #update} 的实现把三个数（月利率/月综合费率/折当率上限）连同状态一条 UPDATE 整体落库：
 * 开票读到的要么是改动前整套、要么是改动后整套，不会一半旧一半新。
 */
public interface PawnRateRepository {

    /**
     * 录入落库：分配雪花 id。该类别已有配置（不论启用停用）时抛业务异常，不落第二条。
     */
    Mono<PawnRate> insert(PawnRate rate);

    /**
     * 修改/停用落库：按 id 单条 UPDATE 整体写入。配置不存在时抛业务异常。
     */
    Mono<PawnRate> update(PawnRate rate);

    Mono<PawnRate> findById(Long id);

    /** 按类别点那条唯一的配置（不论启用停用）；没配过时返回空。 */
    Mono<PawnRate> findByCategory(Category category);

    /** 按类别/状态翻配置清单；逻辑删除的不出现，稳定按 id 升序分页，每行带类别。 */
    Mono<PageResult<PawnRate>> page(int pageNum, int pageSize, PawnRateQuery query);
}
