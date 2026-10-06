package com.somepro.domain.ticket.repository;

import com.somepro.domain.ticket.model.CollateralSnapshot;
import reactor.core.publisher.Mono;

/**
 * 当物快照端口：当票模块要向当物模块要的一个事实 —— 押的这件东西当下长什么样
 * （归谁名下、哪一类、估值多少）。
 *
 * 开票以这份快照为准票面：当户随当物底账带出、类别与估值抄作快照。
 * 只读不写；当物状态的联动（已典当/回在库）由当票仓储在写事务里一并落库。
 */
public interface TicketCollateralPort {

    /**
     * 按 id 读当物快照；当物不存在（含已销账）时返回空。
     */
    Mono<CollateralSnapshot> findSnapshot(Long collateralId);
}
