package com.somepro.infrastructure.persistence.timeline;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.somepro.domain.collateral.model.Collateral;
import com.somepro.domain.forfeit.model.PawnForfeit;
import com.somepro.domain.pawner.model.Pawner;
import com.somepro.domain.redeem.model.PawnRedeem;
import com.somepro.domain.renew.model.PawnRenew;
import com.somepro.domain.ticket.model.PawnTicket;
import com.somepro.domain.timeline.model.TimelineSource;
import com.somepro.domain.timeline.repository.TicketTimelineRepository;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.collateral.CollateralMapper;
import com.somepro.infrastructure.persistence.collateral.converter.CollateralPoConverter;
import com.somepro.infrastructure.persistence.collateral.po.CollateralPO;
import com.somepro.infrastructure.persistence.forfeit.PawnForfeitMapper;
import com.somepro.infrastructure.persistence.forfeit.converter.PawnForfeitPoConverter;
import com.somepro.infrastructure.persistence.forfeit.po.PawnForfeitPO;
import com.somepro.infrastructure.persistence.pawner.PawnerMapper;
import com.somepro.infrastructure.persistence.pawner.converter.PawnerPoConverter;
import com.somepro.infrastructure.persistence.pawner.po.PawnerPO;
import com.somepro.infrastructure.persistence.redeem.PawnRedeemMapper;
import com.somepro.infrastructure.persistence.redeem.converter.PawnRedeemPoConverter;
import com.somepro.infrastructure.persistence.redeem.po.PawnRedeemPO;
import com.somepro.infrastructure.persistence.renew.PawnRenewMapper;
import com.somepro.infrastructure.persistence.renew.converter.PawnRenewPoConverter;
import com.somepro.infrastructure.persistence.renew.po.PawnRenewPO;
import com.somepro.infrastructure.persistence.ticket.PawnTicketMapper;
import com.somepro.infrastructure.persistence.ticket.converter.PawnTicketPoConverter;
import com.somepro.infrastructure.persistence.ticket.po.PawnTicketPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 当票时间线仓储适配器（基础设施层）：只读，不写库。
 *
 * 按票号一次取回整条线的原料：先按 ticket_no 点出当票（uk_ticket_no 唯一），
 * 再按票面挂的 pawnerId / collateralId 带出当户与当物档案，最后把这张票名下的
 * 续当 / 赎当 / 绝当三摞记录全捞出来，转成领域对象交给应用层装配。
 *
 * 逻辑删除全靠 BasePO 上的 @TableLogic：已销掉的续/赎/绝查询时自动过滤（不进时间线），
 * 已销掉的当票 selectOne 直接落空（查无此票，返回空 Mono 由应用层给空时间线）。
 * 当户 / 当物档案若已销掉同样落空，原料里记 null，由领域装配记一笔对不上的账。
 *
 * 排序不在 SQL 层做：业务时刻排序与自洽校验是领域规矩，统一在 TicketTimeline.assemble 里收口。
 */
@Repository
public class TicketTimelineRepositoryImpl implements TicketTimelineRepository {

    private final PawnTicketMapper pawnTicketMapper;
    private final PawnerMapper pawnerMapper;
    private final CollateralMapper collateralMapper;
    private final PawnRenewMapper pawnRenewMapper;
    private final PawnRedeemMapper pawnRedeemMapper;
    private final PawnForfeitMapper pawnForfeitMapper;

    public TicketTimelineRepositoryImpl(PawnTicketMapper pawnTicketMapper,
                                        PawnerMapper pawnerMapper,
                                        CollateralMapper collateralMapper,
                                        PawnRenewMapper pawnRenewMapper,
                                        PawnRedeemMapper pawnRedeemMapper,
                                        PawnForfeitMapper pawnForfeitMapper) {
        this.pawnTicketMapper = pawnTicketMapper;
        this.pawnerMapper = pawnerMapper;
        this.collateralMapper = collateralMapper;
        this.pawnRenewMapper = pawnRenewMapper;
        this.pawnRedeemMapper = pawnRedeemMapper;
        this.pawnForfeitMapper = pawnForfeitMapper;
    }

    @Override
    public Mono<TimelineSource> findSourceByTicketNo(String ticketNo) {
        return blocking(() -> {
            // del_flag = 0 由 @TableLogic 自动拼上：已销掉的票在这里就落空
            PawnTicketPO ticketPo = pawnTicketMapper.selectOne(
                    Wrappers.<PawnTicketPO>lambdaQuery().eq(PawnTicketPO::getTicketNo, ticketNo));
            if (ticketPo == null) {
                return null;
            }
            PawnerPO pawnerPo = pawnerMapper.selectById(ticketPo.getPawnerId());
            CollateralPO collateralPo = collateralMapper.selectById(ticketPo.getCollateralId());
            List<PawnRenewPO> renewPos = pawnRenewMapper.selectList(
                    Wrappers.<PawnRenewPO>lambdaQuery().eq(PawnRenewPO::getTicketId, ticketPo.getId()));
            List<PawnRedeemPO> redeemPos = pawnRedeemMapper.selectList(
                    Wrappers.<PawnRedeemPO>lambdaQuery().eq(PawnRedeemPO::getTicketId, ticketPo.getId()));
            List<PawnForfeitPO> forfeitPos = pawnForfeitMapper.selectList(
                    Wrappers.<PawnForfeitPO>lambdaQuery().eq(PawnForfeitPO::getTicketId, ticketPo.getId()));

            PawnTicket ticket = PawnTicketPoConverter.toDomain(ticketPo);
            Pawner pawner = pawnerPo == null ? null : PawnerPoConverter.toDomain(pawnerPo);
            Collateral collateral = collateralPo == null ? null : CollateralPoConverter.toDomain(collateralPo);
            List<PawnRenew> renews = renewPos.stream()
                    .map(PawnRenewPoConverter::toDomain).collect(Collectors.toList());
            List<PawnRedeem> redeems = redeemPos.stream()
                    .map(PawnRedeemPoConverter::toDomain).collect(Collectors.toList());
            List<PawnForfeit> forfeits = forfeitPos.stream()
                    .map(PawnForfeitPoConverter::toDomain).collect(Collectors.toList());
            return new TimelineSource(ticket, pawner, collateral, renews, redeems, forfeits);
        });
    }

    /**
     * 阻塞 DB 调用 → 响应式链路桥接器：先从 Reactor Context 取操作人，再切到 boundedElastic，
     * 操作人放进 AuditContextHolder 供审计填充（与各模块同一套约定，顺序不能颠倒）。
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
