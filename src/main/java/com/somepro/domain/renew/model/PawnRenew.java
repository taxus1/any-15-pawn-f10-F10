package com.somepro.domain.renew.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import com.somepro.domain.ticket.model.PawnTicket;
import com.somepro.domain.ticket.model.TicketStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 续当登记聚合根（纯领域对象，不带任何持久化注解）。
 *
 * 一条记录 = 一次续当。核心不变量：
 * 1. 只有在当（ACTIVE）的当票才轮得到续；已赎回 / 已绝当 / 已撤销都是定了案的历史票，不收；
 * 2. 必须赶在到期日当天或之前来办 —— 办理日期晚于票面上的到期日期，就该走绝当等别的路子，不能再续；
 * 3. 顺延月数不由人填，按当票原来的当期月数走；新到期日期 = 原到期日期 + 这么多个月（plusMonths）；
 * 4. 原到期日期在办理当下从票面定格抄进来（oldDueDate），续完后的到期日期定格成 newDueDate，
 *    日后票再续、再改，这一条记录都不回写，对账才对得平；
 * 5. 同一张票同一时点只该续出一条 —— 这是跨聚合（续当登记 + 当票到期日期）的并发约束，
 *    由仓储在写锁内对当票做「到期日期仍是办理前那一天」的条件更新来保证，本聚合只管单笔自身的规则。
 *
 * 续当单号 renewNo（XD-2026-0001 样式）由仓储按当年序号生成，全局唯一、一单一号。
 * 续当不改当票状态，票续完仍留在当（ACTIVE）。
 */
@Getter
@Setter
public class PawnRenew extends BaseEntity {

    private Long id;

    /** 续当单号，如 XD-2026-0001；办理时由仓储生成，业务上不可改。 */
    private String renewNo;

    /** 续的是哪张当票（t_pawn_ticket.id）。 */
    private Long ticketId;

    /** 续当前到期日期：办理当下从票面上定格抄录。 */
    private LocalDate oldDueDate;

    /** 续当后到期日期 = oldDueDate + 顺延月数。 */
    private LocalDate newDueDate;

    /** 本次顺延月数，按当票原来的当期月数走，不接受指定。 */
    private Integer extendMonths;

    /** 续当办理时刻，由应用层按行里时区补当下时刻，原样落账。 */
    private LocalDateTime renewedAt;

    /**
     * 工厂方法：办理一次续当。
     *
     * @param ticket     办理当下从库里读出的当票（状态、到期日期、当期月数以它为准）
     * @param renewedAt 续当办理时刻（行里时区，由应用层补服务端当下时间，不接受前端指定）
     */
    public static PawnRenew apply(PawnTicket ticket, LocalDateTime renewedAt) {
        if (ticket == null || ticket.getId() == null) {
            throw new BizException("必须指定续的是哪张当票");
        }
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new BizException("只有在当的当票才能续当，当前状态："
                    + (ticket.getStatus() == null ? "-" : ticket.getStatus().label()));
        }
        Integer termMonths = ticket.getTermMonths();
        if (termMonths == null || termMonths < 1) {
            throw new BizException("当票当期月数异常，无法按原当期顺延");
        }
        LocalDate oldDue = ticket.getDueDate();
        if (oldDue == null) {
            throw new BizException("当票到期日期缺失，不能续当");
        }
        if (renewedAt == null) {
            throw new BizException("续当办理时刻缺失，不能续当");
        }
        // 到期日当天还办得了；过了到期日哪怕一天，也请走绝当等别的路子
        LocalDate renewDate = renewedAt.toLocalDate();
        if (renewDate.isAfter(oldDue)) {
            throw new BizException("已过当票到期日期（" + oldDue + "），不能再续当，请走绝当等其他办理路子");
        }

        PawnRenew renew = new PawnRenew();
        renew.ticketId = ticket.getId();
        renew.oldDueDate = oldDue;
        renew.extendMonths = termMonths;
        renew.newDueDate = oldDue.plusMonths(termMonths);
        renew.renewedAt = renewedAt;
        return renew;
    }
}
