package com.somepro.domain.forfeit.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import com.somepro.domain.ticket.model.PawnTicket;
import com.somepro.domain.ticket.model.TicketStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 绝当处置聚合根（纯领域对象，不带任何持久化注解）。
 *
 * 一条记录 = 一次绝当处置。核心不变量：
 * 1. 只有在当（ACTIVE）的当票才轮得到绝当；已赎回 / 已绝当 / 已撤销都是定了案的历史票，不收；
 * 2. 到期日子必须已经过了今天，并且逾期满三十天 —— 还没到期的、刚到期的（逾期不足三十天）
 *    都不给办，该先催、该等续/赎；「逾期天数 = 办理日 − 到期日」，满三十天即 &ge; 30；
 * 3. 处置方式只认 AUCTION 拍卖 / CONSIGN 变卖 / WRITE_OFF 核销三种；
 * 4. 处置回款写实际到手的金额：可以是零（核销、流拍），但不能是负数，写负数一律挡回；
 * 5. 押的哪件当物以办理当下库里票面挂的 collateralId 为准，不由人另指 ——
 *    绝的就是这张票押的那件东西；
 * 6. 处置时刻不接受前端指定，由应用层补服务端行里时区当下时刻，原样落账；
 * 7. 同一张票只许绝当一回 —— 这是跨聚合（绝当处置 + 当票状态 + 当物状态）的并发约束，
 *    由仓储在写锁内对当票做「仍在当」的条件更新来保证，本聚合只管单笔自身的规则。
 *
 * 绝当处置单号 forfeitNo（JD-2026-0001 样式）由仓储按当年序号生成，全局唯一、一单一号。
 * 绝当办成后，当票从在当转已绝当、当物从已典当转已绝当，两处状态由仓储同事务一起翻。
 *
 * 「到处置那天客户欠行里多少本息、处置回款抵完是盈是亏」不在本聚合落库
 * （t_pawn_forfeit 表没有这些列，建表口径不动），翻单时由 {@link ForfeitView} 按
 * {@link com.somepro.domain.shared.model.PawnSettlement} 的赎当口径现算带上。
 */
@Getter
@Setter
public class PawnForfeit extends BaseEntity {

    /** 逾期满多少天才轮得到绝当：行里规矩，到期后留三十天给客户赎/续。 */
    public static final int FORFEIT_OVERDUE_DAYS = 30;

    private Long id;

    /** 绝当处置单号，如 JD-2026-0001；办理时由仓储生成，业务上不可改。 */
    private String forfeitNo;

    /** 绝的是哪张当票（t_pawn_ticket.id）。 */
    private Long ticketId;

    /** 押的哪件当物（t_collateral.id）：办理当下从票面定格抄录，不接受另指。 */
    private Long collateralId;

    /** 绝当处置时刻，由应用层按行里时区补当下时刻，原样落账。 */
    private LocalDateTime forfeitedAt;

    /** 处置方式：拍卖 / 变卖 / 核销。 */
    private DisposeMethod disposeMethod;

    /** 处置回款（元）：实际到手金额，可以是零，不能是负数。 */
    private BigDecimal recoverAmount;

    /**
     * 工厂方法：办理一次绝当处置。
     *
     * @param ticket         办理当下从库里读出的当票（状态、到期日、押的当物以它为准）
     * @param disposeMethod  处置方式：AUCTION / CONSIGN / WRITE_OFF
     * @param recoverAmount  处置回款（元），实际到手；零可、负数不可
     * @param forfeitedAt    绝当处置时刻（行里时区，由应用层补服务端当下时间，不接受前端指定）
     */
    public static PawnForfeit apply(PawnTicket ticket, DisposeMethod disposeMethod,
                                    BigDecimal recoverAmount, LocalDateTime forfeitedAt) {
        if (ticket == null || ticket.getId() == null) {
            throw new BizException("必须指定绝的是哪张当票");
        }
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new BizException("只有在当的当票才能办绝当，当前状态："
                    + (ticket.getStatus() == null ? "-" : ticket.getStatus().label()));
        }
        LocalDate dueDate = ticket.getDueDate();
        if (dueDate == null) {
            throw new BizException("当票到期日期缺失，不能办绝当");
        }
        if (forfeitedAt == null) {
            throw new BizException("绝当处置时刻缺失，不能办绝当");
        }
        if (disposeMethod == null) {
            throw new BizException("必须指定处置方式：AUCTION 拍卖 / CONSIGN 变卖 / WRITE_OFF 核销");
        }
        if (recoverAmount == null) {
            throw new BizException("必须填处置回款：实际到手多少写多少，没有回款填 0");
        }
        if (recoverAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("处置回款不能是负数：实际到手多少写多少，没有回款填 0");
        }

        // 到期日子得已经过了今天，且逾期满三十天。逾期天数 = 办理日 − 到期日。
        // 还没到期的、到期当天的、逾期不足三十天的，都还轮不到绝当。
        LocalDate forfeitDate = forfeitedAt.toLocalDate();
        long overdueDays = ChronoUnit.DAYS.between(dueDate, forfeitDate);
        if (overdueDays <= 0) {
            throw new BizException("当票还没到到期日期（" + dueDate + "），不能办绝当");
        }
        if (overdueDays < FORFEIT_OVERDUE_DAYS) {
            throw new BizException("当票逾期未满三十天（到期日 " + dueDate + "，需逾期满 "
                    + FORFEIT_OVERDUE_DAYS + " 天），还不能办绝当");
        }
        if (ticket.getCollateralId() == null) {
            throw new BizException("票面抵押当物缺失，不能办绝当");
        }

        PawnForfeit forfeit = new PawnForfeit();
        forfeit.ticketId = ticket.getId();
        forfeit.collateralId = ticket.getCollateralId();
        forfeit.forfeitedAt = forfeitedAt;
        forfeit.disposeMethod = disposeMethod;
        // 金额一律按两位落账，与库里 DECIMAL(14,2) 对齐
        forfeit.recoverAmount = recoverAmount.setScale(2, java.math.RoundingMode.HALF_UP);
        return forfeit;
    }
}
