package com.somepro.application.forfeit;

import com.somepro.common.exception.BizException;
import com.somepro.domain.forfeit.model.DisposeMethod;
import com.somepro.domain.forfeit.model.ForfeitQuery;
import com.somepro.domain.forfeit.model.ForfeitView;
import com.somepro.domain.forfeit.model.PawnForfeit;
import com.somepro.domain.forfeit.repository.PawnForfeitRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.ticket.repository.PawnTicketRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 绝当处置应用服务：编排办理绝当、查看处置单、按当票/处置方式翻处置单三个用例，不写表映射。
 *
 * 出入参用领域对象/基础类型，不认识 PO 与 VO。
 *
 * 办理这条链在这里收口：认票（当票仓储读出最新票面）→ 聚合卡「在当 + 已过期且逾期满三十天 +
 * 回款非负 + 处置方式合法」→ 仓储在写锁内把当票与当物状态一起翻（在当→已绝当 / 已典当→已绝当）、
 * 生成 JD 处置单号、同事务落处置单。单号唯一与「同票只绝一回」的并发约束在仓储里；
 * 单笔自身规则在 PawnForfeit 聚合里。
 *
 * 查看/翻单不读聚合：直接走仓储的对账视图，每张单按处置时刻照赎当同口径带欠款本息与盈亏差额。
 */
@Service
public class PawnForfeitAppService {

    /** 业务时刻统一按行里所在时区算，避免容器 UTC 下把逾期天数算偏一天。 */
    private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");

    private final PawnForfeitRepository pawnForfeitRepository;
    private final PawnTicketRepository pawnTicketRepository;

    public PawnForfeitAppService(PawnForfeitRepository pawnForfeitRepository,
                                 PawnTicketRepository pawnTicketRepository) {
        this.pawnForfeitRepository = pawnForfeitRepository;
        this.pawnTicketRepository = pawnTicketRepository;
    }

    /**
     * 办理绝当处置：只认当票 id、处置方式、处置回款；处置时刻取服务端行里时区当下。
     * 只有在当、且到期日已过今天并逾期满三十天的票办得了；还没到期 / 刚到期 /
     * 已赎回 / 已撤销 / 已绝当都挡回；回款可零不可负；同一时点重复递交只成一次。
     * 处置单号由仓储按 JD-年份-序号 生成；办成后当票转已绝当、当物转已绝当，两头一起翻。
     *
     * @param ticketId       绝的是哪张当票
     * @param disposeMethod  处置方式 code：AUCTION / CONSIGN / WRITE_OFF
     * @param recoverAmount  处置回款（元，字符串接收），实际到手；没有回款填 0
     */
    public Mono<PawnForfeit> forfeit(Long ticketId, String disposeMethod, String recoverAmount) {
        if (ticketId == null) {
            return Mono.error(new BizException("必须指定绝的是哪张当票"));
        }
        final DisposeMethod method = DisposeMethod.ofCode(disposeMethod);
        if (method == null) {
            return Mono.error(new BizException("必须指定处置方式：AUCTION 拍卖 / CONSIGN 变卖 / WRITE_OFF 核销"));
        }
        final BigDecimal recover = parseRecoverAmount(recoverAmount);
        // 办理时刻以服务端行里时区为准，不接受前端指定
        LocalDateTime forfeitedAt = LocalDateTime.now(BIZ_ZONE);
        return pawnTicketRepository.findById(ticketId)
                .switchIfEmpty(Mono.error(new BizException("当票不存在")))
                .flatMap(ticket ->
                        pawnForfeitRepository.insert(PawnForfeit.apply(ticket, method, recover, forfeitedAt)));
    }

    /** 查看处置单对账视图：id 或 forfeitNo（JD-编号）任一指定；带欠款本息与盈亏差额。 */
    public Mono<ForfeitView> detail(Long id, String forfeitNo) {
        if (id != null) {
            return pawnForfeitRepository.findViewById(id)
                    .switchIfEmpty(Mono.error(new BizException("绝当处置单不存在")));
        }
        if (forfeitNo != null && !forfeitNo.isBlank()) {
            return pawnForfeitRepository.findViewByForfeitNo(forfeitNo.trim())
                    .switchIfEmpty(Mono.error(new BizException("绝当处置单不存在")));
        }
        return Mono.error(new BizException("请指定要查看的绝当处置单（id 或 forfeitNo）"));
    }

    /**
     * 翻处置单：当票（ticketId）、处置方式随意拼，都不填翻整份；一页一页走，
     * 每行带 forfeitNo / ticketNo 便于与拍卖行、寄卖行回单对号，并带欠款与盈亏两笔账。
     */
    public Mono<PageResult<ForfeitView>> page(int pageNum, int pageSize, Long ticketId, String disposeMethod) {
        if (pageNum < 1 || pageSize < 1) {
            return Mono.error(new BizException("页码与每页条数必须为正整数"));
        }
        DisposeMethod method = DisposeMethod.ofCode(blankToNull(disposeMethod));
        return pawnForfeitRepository.page(pageNum, pageSize, ForfeitQuery.of(ticketId, method));
    }

    /**
     * 处置回款入参解析：必须是金额数字；实际到手多少写多少 —— 零放得行（核销、流拍），
     * 负数一律挡回。空串按「没填」挡回，避免被当成 0 悄悄核销。
     */
    private BigDecimal parseRecoverAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BizException("必须填处置回款：实际到手多少写多少，没有回款填 0");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new BizException("处置回款必须是金额数字：" + raw);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("处置回款不能是负数：实际到手多少写多少，没有回款填 0");
        }
        return value;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
