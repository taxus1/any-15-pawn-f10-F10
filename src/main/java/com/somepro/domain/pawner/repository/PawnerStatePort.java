package com.somepro.domain.pawner.repository;

import com.somepro.domain.pawner.model.PawnerStatus;
import reactor.core.publisher.Mono;

/**
 * 当户状态端口：开票、续当等模块在办理前向当户模块要的一个事实 —— 这名当户眼下是什么状态。
 *
 * 冻结门禁以当户档案 status 为权威口径（NORMAL / FROZEN / CLOSED 三个值，不另造新值）：
 * 冻住的当户名下不能开新票、不能续当；查空（档案不存在）交回调用方按自己的「找不到」口径处理。
 *
 * 这里给的是办理前的普通预检；开票/续当与冻结并发时，以各自写库事务内的行锁锁定读为最终门禁，
 * 防止「预检通过、落库前一刻被冻」从缝里漏过去。
 */
public interface PawnerStatePort {

    /**
     * 取当户当前状态；档案不存在（含已逻辑删除）时返回空 Mono。
     */
    Mono<PawnerStatus> findStatus(Long pawnerId);
}
