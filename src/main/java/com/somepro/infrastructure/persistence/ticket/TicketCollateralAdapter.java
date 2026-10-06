package com.somepro.infrastructure.persistence.ticket;

import com.somepro.domain.collateral.model.Category;
import com.somepro.domain.ticket.model.CollateralSnapshot;
import com.somepro.domain.ticket.repository.TicketCollateralPort;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.ticket.po.TicketCollateralPO;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * 当物快照端口适配器（基础设施层）：开票当下实时去 t_collateral 读这件东西的
 * 归属、类别与估值 —— 票面上的当户、类别快照、估值快照都以这一读为准。
 * 只读不写；状态联动写在当票仓储的事务里。
 */
@Component
public class TicketCollateralAdapter implements TicketCollateralPort {

    private final TicketCollateralMapper ticketCollateralMapper;

    public TicketCollateralAdapter(TicketCollateralMapper ticketCollateralMapper) {
        this.ticketCollateralMapper = ticketCollateralMapper;
    }

    @Override
    public Mono<CollateralSnapshot> findSnapshot(Long collateralId) {
        return blocking(() -> {
            TicketCollateralPO po = ticketCollateralMapper.selectById(collateralId);
            return po == null ? null : new CollateralSnapshot(
                    po.getId(),
                    po.getPawnerId(),
                    Category.valueOf(po.getCategory()),
                    po.getAppraisedValue());
        });
    }

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
