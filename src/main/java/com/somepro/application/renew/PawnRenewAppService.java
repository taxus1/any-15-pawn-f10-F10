package com.somepro.application.renew;

import com.somepro.common.exception.BizException;
import com.somepro.domain.renew.model.PawnRenew;
import com.somepro.domain.renew.repository.PawnRenewRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.ticket.repository.PawnTicketRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 续当应用服务：编排办理续当、查看续当、按当票翻续当记录三个用例，不写表映射。
 *
 * 出入参用领域对象/基础类型，不认识 PO 与 VO。
 *
 * 办理这条链在这里收口：认票（当票仓储读出最新票面）→ 聚合卡状态与到期日子、
 * 按原当期算顺延月数与新到期日期 → 仓储在写锁内条件推进票期、生成续当单号、同事务落续当登记。
 * 续当单号唯一与「同票同时点只续一条」的并发约束在仓储里；单笔自身规则在 PawnRenew 聚合里。
 */
@Service
public class PawnRenewAppService {

    /** 业务时刻统一按行里所在时区算，避免容器 UTC 下把到期日临界的办理算偏一天。 */
    private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");

    private final PawnRenewRepository pawnRenewRepository;
    private final PawnTicketRepository pawnTicketRepository;

    public PawnRenewAppService(PawnRenewRepository pawnRenewRepository,
                               PawnTicketRepository pawnTicketRepository) {
        this.pawnRenewRepository = pawnRenewRepository;
        this.pawnTicketRepository = pawnTicketRepository;
    }

    /**
     * 办理续当：只认当票 id；顺延月数按票面原当期走，新到期日期从原到期日期往后推，
     * 票续完仍留在当。只有在当、且赶在到期日当天或之前的票办得了；
     * 同一时点重复递交只成一次（仓储写锁内条件更新兜底）。续当单号由仓储按 XD-年份-序号 生成。
     */
    public Mono<PawnRenew> renew(Long ticketId) {
        if (ticketId == null) {
            return Mono.error(new BizException("必须指定续的是哪张当票"));
        }
        // 办理时刻以服务端行里时区为准，不接受前端指定
        LocalDateTime renewedAt = LocalDateTime.now(BIZ_ZONE);
        return pawnTicketRepository.findById(ticketId)
                .switchIfEmpty(Mono.error(new BizException("当票不存在")))
                .flatMap(ticket -> pawnRenewRepository.insert(PawnRenew.apply(ticket, renewedAt)));
    }

    /** 查看续当单：id 或 renewNo（XD-编号）任一指定。 */
    public Mono<PawnRenew> detail(Long id, String renewNo) {
        if (id != null) {
            return pawnRenewRepository.findById(id)
                    .switchIfEmpty(Mono.error(new BizException("续当单不存在")));
        }
        if (renewNo != null && !renewNo.isBlank()) {
            return pawnRenewRepository.findByRenewNo(renewNo.trim())
                    .switchIfEmpty(Mono.error(new BizException("续当单不存在")));
        }
        return Mono.error(new BizException("请指定要查看的续当单（id 或 renewNo）"));
    }

    /** 按当票翻续当记录：必须指定当票，一页一页走，每行带续当单号，稳定按办理次序排列。 */
    public Mono<PageResult<PawnRenew>> pageByTicket(int pageNum, int pageSize, Long ticketId) {
        if (pageNum < 1 || pageSize < 1) {
            return Mono.error(new BizException("页码与每页条数必须为正整数"));
        }
        if (ticketId == null) {
            return Mono.error(new BizException("必须指定按哪张当票翻续当记录"));
        }
        return pawnRenewRepository.pageByTicket(pageNum, pageSize, ticketId);
    }
}
