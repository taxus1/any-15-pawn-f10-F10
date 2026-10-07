package com.somepro.infrastructure.persistence.pawner;

import com.somepro.domain.pawner.model.PawnerStatus;
import com.somepro.domain.pawner.repository.PawnerStatePort;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.pawner.po.PawnerPO;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * 当户状态端口适配器（基础设施层）：办理前实时去 t_pawner 点当户状态，供开票/续当做冻结预检。
 *
 * 这里是普通读（不加锁），只负责把明显冻着的挡在前面；与冻结并发的最终门禁在开票/续当
 * 写库事务内用 {@link PawnerMapper#selectStatusForUpdate} 行锁锁定读兜底。
 */
@Component
public class PawnerStateAdapter implements PawnerStatePort {

    private final PawnerMapper pawnerMapper;

    public PawnerStateAdapter(PawnerMapper pawnerMapper) {
        this.pawnerMapper = pawnerMapper;
    }

    @Override
    public Mono<PawnerStatus> findStatus(Long pawnerId) {
        return blocking(() -> {
            PawnerPO po = pawnerMapper.selectById(pawnerId);
            return po == null || po.getStatus() == null ? null : PawnerStatus.valueOf(po.getStatus());
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
