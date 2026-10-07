package com.somepro.application.collection;

import com.somepro.common.exception.BizException;
import com.somepro.domain.collection.model.CollectionBucket;
import com.somepro.domain.collection.model.CollectionQuery;
import com.somepro.domain.collection.model.CollectionWorkItem;
import com.somepro.domain.collection.repository.CollectionWorkbenchRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 催收工作台应用服务：编排「柜台上班头一件事」这一个用例 —— 把快到期与已逾期两拨在当票
 * 一屏翻出来，每行现成摆着今天来赎要还多少、今天续当后新到期日。
 *
 * 出入参用领域对象/基础类型，不认识 PO 与 VO。全程只读：试算在领域装配里完成，
 * 不调用赎当 / 续当的办理落库链，翻多少遍清单都不动票与当物。
 */
@Service
public class CollectionWorkbenchAppService {

    /** 业务日期统一按行里所在时区算，避免容器 UTC 下把到期档位算偏一天。 */
    private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");

    private final CollectionWorkbenchRepository collectionWorkbenchRepository;

    public CollectionWorkbenchAppService(CollectionWorkbenchRepository collectionWorkbenchRepository) {
        this.collectionWorkbenchRepository = collectionWorkbenchRepository;
    }

    /**
     * 翻催收工作台。
     *
     * @param pageNum  页码，从 1 起
     * @param pageSize 每页条数
     * @param bucket   档位：EXPIRING 只看快到期 / OVERDUE 只看已逾期 / null 两拨都要
     *                 （统一按到期日从近到远、同日按票号排）
     */
    public Mono<PageResult<CollectionWorkItem>> page(int pageNum, int pageSize, CollectionBucket bucket) {
        if (pageNum < 1 || pageSize < 1) {
            return Mono.error(new BizException("页码与每页条数必须为正整数"));
        }
        // 「今天」以服务端行里时区为准，不接受前端指定 —— 档位与两笔试算都锚定这一天
        LocalDate today = LocalDate.now(BIZ_ZONE);
        return collectionWorkbenchRepository.page(pageNum, pageSize, CollectionQuery.of(today, bucket));
    }
}
