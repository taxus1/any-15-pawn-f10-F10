package com.somepro.domain.collection.repository;

import com.somepro.domain.collection.model.CollectionQuery;
import com.somepro.domain.collection.model.CollectionWorkItem;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 催收工作台只读端口（领域层定义，基础设施层实现）。
 *
 * 实现只允许做一件事：把符合 {@link CollectionQuery} 口径的在当票连同当户、当物、续当次数
 * 点出来分页返回。全程只读，不生成任何单据、不回写当票到期日、不翻票与当物的状态。
 * 库里一条都没有时返回空页（content 为空、total=0），不报错。
 *
 * 排序固定：到期日从近到远（升序），同一天按当票号升序 —— 先催最紧迫的，翻页也不会重复跳条。
 */
public interface CollectionWorkbenchRepository {

    /**
     * 按催收口径分页翻台。每行带票号 / 当户 / 当物 / 到期日 / 剩余天数 / 逾期天数 / 续当次数，
     * 以及当天赎当试算总额与当天续当试算新到期日（逾期票该栏为空）。
     */
    Mono<PageResult<CollectionWorkItem>> page(int pageNum, int pageSize, CollectionQuery query);
}
