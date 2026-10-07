package com.somepro.infrastructure.persistence.rate;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.collateral.model.Category;
import com.somepro.domain.rate.model.PawnRate;
import com.somepro.domain.rate.model.PawnRateQuery;
import com.somepro.domain.rate.repository.PawnRateRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.rate.converter.PawnRatePoConverter;
import com.somepro.infrastructure.persistence.ticket.PawnRateMapper;
import com.somepro.infrastructure.persistence.ticket.po.PawnRatePO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 费率配置仓储适配器（基础设施层）：MyBatis-Plus 阻塞 JDBC 经 blocking(...) 桥接进响应式链路。
 * 与当票模块共用一张 t_pawn_rate：当票模块只读（开票/改当金取数），本模块负责录入/修改/停用。
 *
 * 本类两处关键业务语义：
 *
 * 1. 一个类别只留一条配置（含并发）
 *    t_pawn_rate.uk_category 是唯一索引，同一类别物理上只装得下一行 —— 这就是
 *    「别让同一个类别同时挂着两条能用的配置」的结构兜底，不需要再抢命名锁。
 *    录入前先点一遍给出明确业务提示；两人同时录同一类别，先点都落空时由唯一索引
 *    把后到那笔挡成 DuplicateKeyException，同样翻译成业务异常，不甩底层错给柜台。
 *
 * 2. 改配置与柜台开票的「整套快照」衔接
 *    修改/停用都是按 id 一条 UPDATE 把三个数（月利率/月综合费率/折当率上限）连同状态
 *    整体落库，单行 UPDATE 在 InnoDB 里原子生效；开票侧是单行 SELECT 现查现抄 ——
 *    读到的要么是改动前整套、要么是改动后整套，绝不会一半旧一半新。
 *    改完只影响之后新开的票；已开出的票上是开票当刻抄走的快照，这里不回写。
 */
@Repository
public class PawnRateRepositoryImpl implements PawnRateRepository {

    private final PawnRateMapper pawnRateMapper;

    public PawnRateRepositoryImpl(PawnRateMapper pawnRateMapper) {
        this.pawnRateMapper = pawnRateMapper;
    }

    @Override
    public Mono<PawnRate> insert(PawnRate rate) {
        return blocking(() -> {
            // 先点一遍给出明确提示；@TableLogic 只看未删除的行
            Long exists = pawnRateMapper.selectCount(Wrappers.<PawnRatePO>lambdaQuery()
                    .eq(PawnRatePO::getCategory, rate.getCategory().code()));
            if (exists != null && exists > 0) {
                throw duplicate(rate.getCategory());
            }
            PawnRatePO po = PawnRatePoConverter.toPo(rate);
            po.setId(IdUtil.getSnowflakeNextId());
            try {
                pawnRateMapper.insert(po);
            } catch (DuplicateKeyException e) {
                // uk_category 兜底：并发同时录同一类别，后到那笔在这里挡回
                throw duplicate(rate.getCategory());
            }
            return PawnRatePoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PawnRate> update(PawnRate rate) {
        return blocking(() -> {
            PawnRatePO po = PawnRatePoConverter.toPo(rate);
            // 一条 UPDATE 整体落库（含状态），单行原子 —— 开票侧不会抄到一半旧一半新
            int rows = pawnRateMapper.updateById(po);
            if (rows == 0) {
                throw new BizException("费率配置不存在");
            }
            // 回读一趟，把审计填充的 update_by / update_time 带回来
            PawnRatePO refreshed = pawnRateMapper.selectById(rate.getId());
            return PawnRatePoConverter.toDomain(Objects.requireNonNullElse(refreshed, po));
        });
    }

    @Override
    public Mono<PawnRate> findById(Long id) {
        return blocking(() -> {
            PawnRatePO po = pawnRateMapper.selectById(id);
            return po == null ? null : PawnRatePoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PawnRate> findByCategory(Category category) {
        return blocking(() -> {
            PawnRatePO po = pawnRateMapper.selectOne(Wrappers.<PawnRatePO>lambdaQuery()
                    .eq(PawnRatePO::getCategory, category.code()));
            return po == null ? null : PawnRatePoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PageResult<PawnRate>> page(int pageNum, int pageSize, PawnRateQuery query) {
        return this.<PageResult<PawnRate>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<PawnRatePO> wrapper = Wrappers.<PawnRatePO>lambdaQuery();
                if (query.category() != null) {
                    wrapper.eq(PawnRatePO::getCategory, query.category().code());
                }
                if (query.status() != null) {
                    wrapper.eq(PawnRatePO::getStatus, query.status().code());
                }
                // 稳定排序：一页页往后翻不会重复、不会跳条
                wrapper.orderByAsc(PawnRatePO::getId);
                List<PawnRatePO> rows = pawnRateMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<PawnRate> content = rows.stream()
                        .map(PawnRatePoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                // PageHelper 靠 ThreadLocal 传分页参数，必须清，避免污染线程池下一次调用
                PageHelper.clearPage();
            }
        });
    }

    private static BizException duplicate(Category category) {
        return new BizException("该类别（" + category.label() + "）已存在费率配置，不能重复录入；要调整请走修改");
    }

    /**
     * 阻塞 DB 调用 → 响应式链路桥接器：先从 Reactor Context 取操作人，再切到 boundedElastic，
     * 操作人放进 AuditContextHolder 供审计填充（与其它模块同一套约定，顺序不能颠倒）。
     */
    private <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
