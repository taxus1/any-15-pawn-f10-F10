package com.somepro.domain.timeline.model;

import com.somepro.domain.redeem.model.PawnRedeem;
import com.somepro.domain.renew.model.PawnRenew;
import com.somepro.domain.ticket.model.PawnTicket;
import com.somepro.domain.ticket.model.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 一张当票从头到尾的时间线（只读领域视图，不可变 record）。
 *
 * 串线规矩：
 * 1. 一段不缺 —— 当户建档、当物登记、当票开立、每一次续当、赎当结清或绝当处置，
 *    各段带自己的编号与当时落库的数，原样带出，不按现在的配置重算；
 * 2. 排序只认业务发生的时刻（续/赎/绝认 renewed_at / redeemed_at / forfeited_at，
 *    不认落库时刻 —— 补录的单子落库在后、业务上是早的），从新到旧排，最新的排最前；
 *    同一时刻的按「业务先后次序倒序、再按单号倒序」的稳定次序摆；
 * 3. 整条线得能自圆其说，对不上就在 issues 里记一笔（consistent 置 false），
 *    但线照摆 —— 核账的人要看到的就是哪里串了：
 *    a. 续当链一环扣一环：头一环接在「起当日期 + 当期月数」上，前一环续到的日子就是
 *       后一环续起的日子，最后一环续到的日子就是票面上现在挂的到期日期；
 *    b. 赎当段的应还总额 = 票面当金 + 它自己记的利息与综合费；
 *    c. 同一张票不该既挂赎当段又挂绝当段，赎/绝各自也只该有一笔。
 */
public record TicketTimeline(String ticketNo,
                             TicketStatus ticketStatus,
                             List<TimelineEvent> events,
                             boolean consistent,
                             List<String> issues) {

    /**
     * 段落排序：业务发生时刻从新到旧（时刻缺失的排最末），同一时刻按业务先后次序倒序，
     * 再同一次序按单号倒序 —— 稳定、可重复，翻几遍都一个样。
     */
    private static final Comparator<TimelineEvent> EVENT_ORDER =
            Comparator.comparing(TimelineEvent::bizTime,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Comparator.comparingInt(
                            (TimelineEvent e) -> e.type().sequence()).reversed())
                    .thenComparing(Comparator.comparing(TimelineEvent::bizNo,
                            Comparator.nullsLast(Comparator.reverseOrder())));

    /** 续当链校验时的行走次序：办理时刻从旧到新，同一时刻按单号正序（与展示次序正好相反）。 */
    private static final Comparator<PawnRenew> CHAIN_ORDER =
            Comparator.comparing(PawnRenew::getRenewedAt,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(PawnRenew::getRenewNo,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    /** 查无此票（含票已销掉）时的空时间线：不报错，段落为空、账目一栏干净。 */
    public static TicketTimeline empty(String ticketNo) {
        return new TicketTimeline(ticketNo, null, List.of(), true, List.of());
    }

    /**
     * 把原料串成一条线：摆段、排序、验自洽。原料里缺什么、哪一环对不上，
     * 都记进 issues，线本身照摆。
     */
    public static TicketTimeline assemble(TimelineSource source) {
        if (source == null || source.ticket() == null) {
            throw new IllegalArgumentException("串时间线必须先从库里取出当票");
        }
        PawnTicket ticket = source.ticket();
        List<String> issues = new ArrayList<>();
        List<TimelineEvent> events = new ArrayList<>();

        // 当户建档 / 当物登记：档案被销掉时这两段串不出来，记一笔接着摆后面的段
        if (source.pawner() == null) {
            issues.add("当户档案缺失（可能已销户），当户建档这一段串不出来，账对不上");
        } else {
            events.add(TimelineEvent.pawnerRegister(source.pawner()));
        }
        if (source.collateral() == null) {
            issues.add("当物档案缺失（可能已销掉），当物登记这一段串不出来，账对不上");
        } else {
            events.add(TimelineEvent.collateralRegister(source.collateral()));
        }

        events.add(TimelineEvent.ticketIssue(ticket));
        source.renews().forEach(renew -> events.add(TimelineEvent.renew(renew)));
        source.redeems().forEach(redeem -> events.add(TimelineEvent.redeem(redeem)));
        source.forfeits().forEach(forfeit -> events.add(TimelineEvent.forfeit(forfeit)));
        events.sort(EVENT_ORDER);

        checkRenewChain(ticket, source.renews(), issues);
        checkRedeem(ticket, source.redeems(), issues);
        checkSettlement(source, issues);

        return new TicketTimeline(ticket.getTicketNo(), ticket.getStatus(),
                List.copyOf(events), issues.isEmpty(), List.copyOf(issues));
    }

    /**
     * 续当链：头一环的「续当前到期日期」要接在「起当日期 + 当期月数」上，
     * 前一环续到的日子就是后一环续起的日子，最后一环续到的日子要等于票面上的到期日期。
     * 哪一环对不上就记一笔；记录不全（日期缺失）也照记，链走到断处为止。
     */
    private static void checkRenewChain(PawnTicket ticket, List<PawnRenew> renews, List<String> issues) {
        LocalDate expected = null;
        if (ticket.getStartDate() == null || ticket.getTermMonths() == null) {
            issues.add("票面起当日期或当期月数缺失，续当链没有头可接，账对不上");
        } else {
            expected = ticket.getStartDate().plusMonths(ticket.getTermMonths());
        }

        List<PawnRenew> chain = renews.stream().sorted(CHAIN_ORDER).toList();
        for (PawnRenew renew : chain) {
            if (renew.getOldDueDate() == null || renew.getNewDueDate() == null) {
                issues.add("续当单 " + renew.getRenewNo() + " 的到期日期记录不全，续当链在这一环断开了");
                expected = null;
                break;
            }
            if (expected != null && !renew.getOldDueDate().equals(expected)) {
                issues.add("续当链断开：续当单 " + renew.getRenewNo() + " 记的续当前到期日期是 "
                        + renew.getOldDueDate() + "，按上一环应接在 " + expected + "，账串了");
            }
            expected = renew.getNewDueDate();
        }

        if (expected == null) {
            return;
        }
        if (ticket.getDueDate() == null) {
            issues.add("票面到期日期缺失，续当链的尾巴没处对，账对不上");
        } else if (!expected.equals(ticket.getDueDate())) {
            if (chain.isEmpty()) {
                issues.add("票面自洽对不上：到期日期 " + ticket.getDueDate()
                        + " 不等于起当日期加当期月数（应得 " + expected + "），账串了");
            } else {
                issues.add("续当链的尾巴对不上票面：最后一环续到的到期日期是 " + expected
                        + "，票面上挂的是 " + ticket.getDueDate() + "，账串了");
            }
        }
    }

    /** 赎当段：应还总额要等于票面当金加它自己记的利息与综合费，对不上就是账串了。 */
    private static void checkRedeem(PawnTicket ticket, List<PawnRedeem> redeems, List<String> issues) {
        for (PawnRedeem redeem : redeems) {
            if (ticket.getPawnAmount() == null || redeem.getFeeAmount() == null
                    || redeem.getTotalAmount() == null) {
                issues.add("赎当单 " + redeem.getRedeemNo() + " 或票面的金额记录不全，应还总额没法自洽");
                continue;
            }
            BigDecimal expectedTotal = ticket.getPawnAmount().add(redeem.getFeeAmount());
            if (redeem.getTotalAmount().compareTo(expectedTotal) != 0) {
                issues.add("赎当单 " + redeem.getRedeemNo() + " 记的应还总额 " + redeem.getTotalAmount()
                        + " 不等于本金加费用（" + ticket.getPawnAmount() + " + " + redeem.getFeeAmount()
                        + " = " + expectedTotal + "），账串了");
            }
        }
    }

    /** 结清口径：一票只该结一回 —— 赎当与绝当不该同时挂上，各自也只该有一笔。 */
    private static void checkSettlement(TimelineSource source, List<String> issues) {
        if (!source.redeems().isEmpty() && !source.forfeits().isEmpty()) {
            issues.add("同一张票既挂了赎当单又挂了绝当处置单，一票不能两结，账串了");
        }
        if (source.redeems().size() > 1) {
            issues.add("同一张票挂了 " + source.redeems().size() + " 笔赎当单，一票只该赎一回，账串了");
        }
        if (source.forfeits().size() > 1) {
            issues.add("同一张票挂了 " + source.forfeits().size() + " 笔绝当处置单，一票只该绝一回，账串了");
        }
    }
}
