package com.somepro.domain.timeline.model;

import com.somepro.domain.collateral.model.Category;
import com.somepro.domain.collateral.model.Collateral;
import com.somepro.domain.collateral.model.ConditionLevel;
import com.somepro.domain.forfeit.model.DisposeMethod;
import com.somepro.domain.forfeit.model.PawnForfeit;
import com.somepro.domain.pawner.model.Pawner;
import com.somepro.domain.redeem.model.PawnRedeem;
import com.somepro.domain.renew.model.PawnRenew;
import com.somepro.domain.ticket.model.PawnTicket;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 时间线上的一段（只读领域视图，不可变 record）：一件事、一个编号、一组当时落库的数。
 *
 * 一段只填自己那类字段，其余留 null（接口层按 non_null 口径不序列化，看的人只见得到
 * 这段该有的内容）。各段的编号与各段的数都照当初落库的值原样带出，不在此重算：
 * - 当户建档：pawnerNo + 姓名/身份证/电话，业务时刻取建档时刻（create_time）；
 * - 当物登记：itemNo + 名称/类别/品牌/品相/估值，业务时刻取登记时刻（create_time）；
 * - 当票开立：ticketNo + 当金/起当日期/到期日期/当期月数，业务时刻取开票时刻（create_time）；
 * - 续当：renewNo + 续当前/后到期日期/顺延月数，业务时刻取 renewed_at（办理时刻，不是落库时刻 ——
 *   补录的单子落库在后、业务上是早的）；
 * - 赎当结清：redeemNo + 计费天数/利息与综合费/应还总额，业务时刻取 redeemed_at；
 * - 绝当处置：forfeitNo + 处置方式/处置回款，业务时刻取 forfeited_at。
 */
public record TimelineEvent(
        // ---- 段头：类型 / 业务发生时刻 / 这段自己的编号 ----
        TimelineEventType type,
        LocalDateTime bizTime,
        String bizNo,
        // ---- 当户建档段 ----
        String pawnerName,
        String pawnerIdCard,
        String pawnerPhone,
        // ---- 当物登记段 ----
        String itemName,
        Category category,
        String brand,
        ConditionLevel conditionLevel,
        BigDecimal appraisedValue,
        // ---- 当票开立段 ----
        BigDecimal pawnAmount,
        LocalDate startDate,
        LocalDate dueDate,
        Integer termMonths,
        // ---- 续当段 ----
        LocalDate oldDueDate,
        LocalDate newDueDate,
        Integer extendMonths,
        // ---- 赎当结清段 ----
        Integer usedDays,
        BigDecimal feeAmount,
        BigDecimal totalAmount,
        // ---- 绝当处置段 ----
        DisposeMethod disposeMethod,
        BigDecimal recoverAmount) {

    /** 当户建档一段：编号取当户编号，时刻取建档落库时刻。 */
    public static TimelineEvent pawnerRegister(Pawner pawner) {
        return new TimelineEvent(
                TimelineEventType.PAWNER_REGISTER, pawner.getCreateTime(), pawner.getPawnerNo(),
                pawner.getName(), pawner.getIdCard(), pawner.getPhone(),
                null, null, null, null, null,
                null, null, null, null,
                null, null, null,
                null, null, null,
                null, null);
    }

    /** 当物登记一段：编号取当物编号，时刻取登记落库时刻。 */
    public static TimelineEvent collateralRegister(Collateral collateral) {
        return new TimelineEvent(
                TimelineEventType.COLLATERAL_REGISTER, collateral.getCreateTime(), collateral.getItemNo(),
                null, null, null,
                collateral.getItemName(), collateral.getCategory(), collateral.getBrand(),
                collateral.getConditionLevel(), collateral.getAppraisedValue(),
                null, null, null, null,
                null, null, null,
                null, null, null,
                null, null);
    }

    /** 当票开立一段：编号取当票号，带出当金与起当/到期日期，时刻取开票落库时刻。 */
    public static TimelineEvent ticketIssue(PawnTicket ticket) {
        return new TimelineEvent(
                TimelineEventType.TICKET_ISSUE, ticket.getCreateTime(), ticket.getTicketNo(),
                null, null, null,
                null, null, null, null, null,
                ticket.getPawnAmount(), ticket.getStartDate(), ticket.getDueDate(), ticket.getTermMonths(),
                null, null, null,
                null, null, null,
                null, null);
    }

    /** 续当一段：编号取续当单号，时刻取 renewed_at 办理时刻（不认落库时刻）。 */
    public static TimelineEvent renew(PawnRenew renew) {
        return new TimelineEvent(
                TimelineEventType.RENEW, renew.getRenewedAt(), renew.getRenewNo(),
                null, null, null,
                null, null, null, null, null,
                null, null, null, null,
                renew.getOldDueDate(), renew.getNewDueDate(), renew.getExtendMonths(),
                null, null, null,
                null, null);
    }

    /** 赎当结清一段：编号取赎当单号，时刻取 redeemed_at 办理时刻。 */
    public static TimelineEvent redeem(PawnRedeem redeem) {
        return new TimelineEvent(
                TimelineEventType.REDEEM, redeem.getRedeemedAt(), redeem.getRedeemNo(),
                null, null, null,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null,
                redeem.getUsedDays(), redeem.getFeeAmount(), redeem.getTotalAmount(),
                null, null);
    }

    /** 绝当处置一段：编号取绝当处置单号，时刻取 forfeited_at 处置时刻。 */
    public static TimelineEvent forfeit(PawnForfeit forfeit) {
        return new TimelineEvent(
                TimelineEventType.FORFEIT, forfeit.getForfeitedAt(), forfeit.getForfeitNo(),
                null, null, null,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null,
                null, null, null,
                forfeit.getDisposeMethod(), forfeit.getRecoverAmount());
    }
}
