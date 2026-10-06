package com.somepro.infrastructure.persistence.ticket;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.somepro.domain.collateral.model.Category;
import com.somepro.domain.ticket.model.RateConfig;
import com.somepro.domain.ticket.repository.RateConfigPort;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.ticket.po.PawnRatePO;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * 费率配置端口适配器（基础设施层）：每次开票/改当金都实时去 t_pawn_rate 点
 * 该类别当前生效（ENABLED、未删除）的那一行 —— 抄进票里的是这一刻的配置，
 * 日后配置改了不影响已开出的票。只读不写。
 */
@Component
public class RateConfigAdapter implements RateConfigPort {

    /** 生效口径：t_pawn_rate.status = ENABLED（停用的配置不参与开票/改当金）。 */
    private static final String STATUS_ENABLED = "ENABLED";

    private final PawnRateMapper pawnRateMapper;

    public RateConfigAdapter(PawnRateMapper pawnRateMapper) {
        this.pawnRateMapper = pawnRateMapper;
    }

    @Override
    public Mono<RateConfig> findEnabled(Category category) {
        return blocking(() -> {
            PawnRatePO po = pawnRateMapper.selectOne(Wrappers.<PawnRatePO>lambdaQuery()
                    .eq(PawnRatePO::getCategory, category.code())
                    .eq(PawnRatePO::getStatus, STATUS_ENABLED));
            return po == null ? null : new RateConfig(
                    Category.valueOf(po.getCategory()),
                    po.getMonthlyRate(),
                    po.getServiceRate(),
                    po.getMaxLoanRatio());
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
