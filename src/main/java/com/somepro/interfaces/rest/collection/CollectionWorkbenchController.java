package com.somepro.interfaces.rest.collection;

import com.somepro.application.collection.CollectionWorkbenchAppService;
import com.somepro.common.Result;
import com.somepro.domain.collection.model.CollectionBucket;
import com.somepro.interfaces.rest.collection.converter.CollectionWorkItemVoConverter;
import com.somepro.interfaces.rest.collection.vo.CollectionWorkItemVO;
import com.somepro.interfaces.rest.common.vo.PageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 催收工作台用户接口层：一屏翻出快到期与已逾期两拨在当票。
 *
 * 只做协议适配（参数解析、VO 转换、Result 包装），业务编排在 {@link CollectionWorkbenchAppService}。
 * 接口只读：清单里的赎当总额 / 续当新到期日都是当着客户面的试算，不会落库、不动票与当物状态。
 */
@RestController
@RequestMapping("/api/collection/workbench")
public class CollectionWorkbenchController {

    private final CollectionWorkbenchAppService collectionWorkbenchAppService;

    public CollectionWorkbenchController(CollectionWorkbenchAppService collectionWorkbenchAppService) {
        this.collectionWorkbenchAppService = collectionWorkbenchAppService;
    }

    /**
     * 催收清单：只含在当票，按库里当前到期日分两档 —— EXPIRING 今天起 7 天内到期（含今天、含第七天）、
     * OVERDUE 到期日已过今天；已赎 / 已绝当 / 已撤销 / 删除标记的票不出现，无数据时返回空页。
     *
     * bucket 不传两拨都要；排序为到期日从近到远、同日按票号；pageNum/pageSize 一页页走。
     */
    @GetMapping("/list")
    public Mono<Result<PageVO<CollectionWorkItemVO>>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String bucket) {
        CollectionBucket bucketFilter = CollectionBucket.ofCode(bucket);
        return collectionWorkbenchAppService.page(pageNum, pageSize, bucketFilter)
                .map(CollectionWorkItemVoConverter::toPageVo)
                .map(Result::ok);
    }
}
