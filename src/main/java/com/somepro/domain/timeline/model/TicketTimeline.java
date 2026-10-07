package com.somepro.domain.timeline.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 一张当票从头到尾的时间线（只读领域视图，不可变）：输入一个当票号，回这一条完整的线。
 *
 * 线上六段（建档 / 登记 / 开票 / 续当 ×N / 赎当结清 / 绝当处置，按这张票实际走过的出现）
 * 各带各的编号与各段当初落库的原值，由仓储从六张表原样点回来；本视图不新建任何数据、
 * 不按现在挂的费率配置重算任何数。
 *
 * 两个关键职责：
 * 1. 排序：按业务发生时刻从新到旧（最新在前）。排序只认各段业务时刻
 *    （建档/登记/开票取 create_time，续当取 renewed_at，赎当取 redeemed_at，绝当取 forfeited_at），
 *    不认落库时刻 —— 补录单 create_time 在后，业务上却是早的。
 *    同一时刻用固定的稳定次序：业务生命周期靠后的段在前（结局段 → 续当 → 开票 → 登记 → 建档），
 *    再同则按段 id 升序，次序钉死、不依赖数据库返回顺序。
 * 2. 自洽（consistent）：把各段串起来核一遍账，对不上就 consistent=false 并把问题列在 issues，
 *    线照样整条返回（不能因为账串了就把段藏起来），让核账的人一眼看到错在哪：
 *    - 续当链一环扣一环：相邻两次续当，前段的 newDueDate 必须等于后段的 oldDueDate；
 *    - 赎当应还总额 = 票面本金（开票段 pawnAmount）+ 该赎当段记的费用 feeAmount；
 *    - 同一张票不能又挂赎当段又挂绝当段。
 *
 * @param ticketNo   当票号（查询入参，原样回带）
 * @param status     票走到今天的状态码（取自开票段：ACTIVE / REDEEMED / FORFEITED / CANCELLED）
 * @param segments   已按业务时刻从新到旧排稳的全部段
 * @param consistent 线上各段账是否自洽
 * @param issues     自洽核对发现的问题（人话描述）；无问题为空列表
 */
public record TicketTimeline(String ticketNo,
                             String status,
                             List<TimelineSegment> segments,
                             boolean consistent,
                             List<String> issues) {

    /**
     * 由仓储点齐的各段（未排序、已过滤逻辑删除）装一条时间线：
     * 排序 + 自洽核对都在这里做，仓储只负责把各段按表原样喂进来。
     *
     * @param ticketNo 当票号
     * @param raw      仓储点回的全部段；至少含一个开票段（调用方按票号查到票才会进来），
     *                 建档/登记段在档案缺失时允许缺席
     */
    public static TicketTimeline assemble(String ticketNo, List<TimelineSegment> raw) {
        List<TimelineSegment> segments = new ArrayList<>(raw == null ? List.of() : raw);

        TicketIssueSegment issue = findFirst(segments, TicketIssueSegment.class);
        String status = issue == null ? null : issue.status();

        // 排序：业务时刻倒序 → 同刻按生命周期靠后的段在前 → 再同按 id 升序。
        segments.sort(TIMELINE_ORDER);

        List<String> issues = new ArrayList<>();
        checkRenewChain(segments, issues);
        checkRedeemTotal(issue, segments, issues);
        checkRedeemForfeitMutualExclusive(segments, issues);

        return new TicketTimeline(ticketNo, status, List.copyOf(segments), issues.isEmpty(), List.copyOf(issues));
    }

    /**
     * 续当链一环扣一环：按业务时刻从早到晚（同刻按 id）排好后，
     * 相邻两段前段续完的日子（newDueDate）必须就是后段续起的日子（oldDueDate），断一天都记一笔。
     */
    private static void checkRenewChain(List<TimelineSegment> all, List<String> issues) {
        List<RenewSegment> renews = all.stream()
                .filter(RenewSegment.class::isInstance)
                .map(RenewSegment.class::cast)
                .sorted(CHRONOLOGICAL_ORDER)
                .toList();
        for (int i = 1; i < renews.size(); i++) {
            RenewSegment prev = renews.get(i - 1);
            RenewSegment next = renews.get(i);
            // 顺序按业务时刻排死（同刻按 id），相邻两段前段续完的日子必须就是后段续起的日子。
            if (prev.newDueDate() != null && next.oldDueDate() != null
                    && !prev.newDueDate().equals(next.oldDueDate())) {
                issues.add("续当链断档：续当单 " + prev.renewNo()
                        + " 续完的到期日 " + prev.newDueDate()
                        + " 与续当单 " + next.renewNo()
                        + " 续起的到期日 " + next.oldDueDate() + " 对不上");
            }
        }
    }

    /**
     * 赎当应还总额 = 它自己记的费用 + 票面本金（开票段的当金快照）。
     * 金额两位小数按 compareTo 核（DECIMAL(14,2)，等值不看 scale）。每段赎当各核一笔。
     */
    private static void checkRedeemTotal(TicketIssueSegment issue,
                                         List<TimelineSegment> all, List<String> issues) {
        if (issue == null || issue.pawnAmount() == null) {
            // 连开票段或票面本金都没有，后续互斥等校验仍可做，这里无从核起。
            return;
        }
        for (TimelineSegment seg : all) {
            if (seg instanceof RedeemSegment redeem) {
                if (redeem.feeAmount() == null || redeem.totalAmount() == null) {
                    issues.add("赎当单 " + redeem.redeemNo() + " 费用或应还总额缺失，无法核对");
                    continue;
                }
                BigDecimal expected = issue.pawnAmount().add(redeem.feeAmount());
                if (redeem.totalAmount().compareTo(expected) != 0) {
                    issues.add("赎当单 " + redeem.redeemNo() + " 账串了：应还总额 "
                            + redeem.totalAmount().toPlainString()
                            + " ≠ 票面本金 " + issue.pawnAmount().toPlainString()
                            + " + 费用 " + redeem.feeAmount().toPlainString()
                            + "（应为 " + expected.toPlainString() + "）");
                }
            }
        }
    }

    /** 同一张票不能又挂在赎当段又挂在绝当段：两类段各至多 0 或 1，两边都有就是账串了。 */
    private static void checkRedeemForfeitMutualExclusive(List<TimelineSegment> all, List<String> issues) {
        List<String> redeemNos = all.stream()
                .filter(RedeemSegment.class::isInstance).map(RedeemSegment.class::cast)
                .map(RedeemSegment::redeemNo).toList();
        List<String> forfeitNos = all.stream()
                .filter(ForfeitSegment.class::isInstance).map(ForfeitSegment.class::cast)
                .map(ForfeitSegment::forfeitNo).toList();
        if (!redeemNos.isEmpty() && !forfeitNos.isEmpty()) {
            issues.add("同一张票同时挂了赎当与绝当：赎当单 " + String.join("、", redeemNos)
                    + "；绝当单 " + String.join("、", forfeitNos) + "，二者互斥");
        }
    }

    private static <T> T findFirst(List<TimelineSegment> segments, Class<T> type) {
        return type.cast(segments.stream().filter(type::isInstance).findFirst().orElse(null));
    }

    /**
     * 时间线展示次序：业务时刻从新到旧；同一业务时刻按段类型在业务生命周期里的
     * 倒序（FORFEIT/REDEEM=5/4 → RENEW=3 → TICKET_ISSUE=2 → COLLATERAL=1 → PAWNER=0）；
     * 再同按段 id 升序。时刻为 null 的脏数据沉底，不报错。
     */
    private static final Comparator<TimelineSegment> TIMELINE_ORDER =
            Comparator.comparing(TimelineSegment::bizTime,
                            Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder().reversed()))
                    .thenComparing(Comparator.comparingInt((TimelineSegment s) -> s.type().ordinal()).reversed())
                    .thenComparing(TimelineSegment::segmentId,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    /** 续当链核对用的从早到晚次序：业务时刻升序，同刻按 id 升序钉死先后。 */
    private static final Comparator<RenewSegment> CHRONOLOGICAL_ORDER =
            Comparator.comparing(RenewSegment::bizTime,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(RenewSegment::segmentId,
                            Comparator.nullsLast(Comparator.naturalOrder()));
}
