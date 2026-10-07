package com.somepro.domain.collection.model;

import com.somepro.domain.redeem.model.PawnRedeem;
import com.somepro.domain.renew.model.PawnRenew;
import com.somepro.domain.ticket.model.PawnTicket;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 催收工作台一行（只读领域视图，不可变 record）：一张在当的票 + 柜台打电话要报的两笔现成数。
 *
 * 这不是新聚合、不落库：它只是把当票、当户、当物与续当次数拼到一屏，并在拼装【当下】
 * 当着客户面试算两笔。两笔算完即弃，翻十遍清单也不会落库、不会动票和当物的任何状态。
 *
 * 行字段口径：
 * - bucket   档位：EXPIRING 快到期（今天起 7 天内到期，含今天、含第七天）/ OVERDUE 已逾期（到期日早于今天）；
 * - daysRemaining 离到期还剩几天 = 到期日 − 今天的有符号天数：0~7 为快到期，逾期票为负数；
 * - overdueDays   拖了多少天：逾期票为 今天 − 到期日 的正数天数；快到期票为 null（不标负数）；
 * - renewCount    这张票带过几回续当（t_pawn_renew 有效行数，逻辑删除的不计）。
 *
 * 两笔试算与真去办一次【分毫不差】，规矩照赎当 / 续当两处办理的聚合走，本类不另立一套：
 * - redeemTotalAmount 今天来赎要还的总额：调 {@link PawnRedeem#apply}，
 *   按票面利率费率快照与起当日期、今天日期算（逾期票也照实际天数计费、不加罚）；
 *   同时带出 redeemUsedDays / redeemFeeAmount，方便柜台跟客户解释这笔总额怎么来的；
 * - nextDueDate 今天来办续当之后的新到期日：调 {@link PawnRenew#apply}，
 *   到期日 + 票面原当期月数。过了到期日的票今天续不了（{@link PawnRenew#apply} 会挡），
 *   这一栏连同 extendMonths 一起留空。
 */
public record CollectionWorkItem(Long ticketId,
                                 String ticketNo,
                                 Long pawnerId,
                                 String pawnerName,
                                 String pawnerPhone,
                                 Long collateralId,
                                 String collateralName,
                                 LocalDate dueDate,
                                 CollectionBucket bucket,
                                 int daysRemaining,
                                 Integer overdueDays,
                                 int renewCount,
                                 BigDecimal redeemTotalAmount,
                                 BigDecimal redeemFeeAmount,
                                 int redeemUsedDays,
                                 LocalDate nextDueDate,
                                 Integer extendMonths) {

    /** 快到期窗口：从今天起往后这么多天以内到期（含端点）。 */
    public static final int EXPIRING_WINDOW_DAYS = 7;

    /**
     * 由仓储拼好的一屏数据组装一行。
     *
     * @param ticket     按库里最新票面重建的在当当票（当金、起当/到期日期、利率费率快照、当期月数以它为准）
     * @param today      业务「今天」（行里时区，由应用层按服务端日期传入，不接受前端指定）
     * @param pawnerName 当户姓名（随票挂的当户带出）；当户档案缺失时可为 null
     * @param pawnerPhone 当户电话（催收要拨号）；未登记或档案缺失时可为 null
     * @param collateralName 当物名称；当物档案缺失时可为 null
     * @param renewCount 这张票已办理过的续当次数（不含逻辑删除）
     */
    public static CollectionWorkItem assemble(PawnTicket ticket, LocalDate today,
                                              String pawnerName, String pawnerPhone,
                                              String collateralName, int renewCount) {
        if (ticket == null || ticket.getId() == null) {
            throw new IllegalArgumentException("催收工作台行必须对应一张当票");
        }
        if (today == null) {
            throw new IllegalArgumentException("业务今天日期缺失，无法生成催收工作台行");
        }
        LocalDate dueDate = ticket.getDueDate();
        if (dueDate == null) {
            throw new IllegalArgumentException("当票到期日期缺失，无法生成催收工作台行");
        }

        // 有符号天数：到期日 − 今天。逾期为负、当天为 0、未来为正。
        long delta = ChronoUnit.DAYS.between(today, dueDate);
        CollectionBucket bucket;
        Integer overdueDays;
        if (delta < 0) {
            bucket = CollectionBucket.OVERDUE;
            overdueDays = (int) -delta;
        } else {
            // 仓储只捞「逾期 + 7 天内到期」两拨票，落到这里 delta 必在 [0,7]
            bucket = CollectionBucket.EXPIRING;
            overdueDays = null;
        }

        // 试算一：今天来赎要还多少。办理时刻取今天，金额照票面快照按实际天数算，逾期不加罚。
        // 直接走赎当办理的同一工厂方法，保证清单上的数与真办一次分毫不差；不落库、不翻状态。
        PawnRedeem redeemTrial = PawnRedeem.apply(ticket, today.atStartOfDay());

        // 试算二：今天再续一段到哪天。过了到期日的票续不了，工厂方法会挡 —— 这里把新到期日留空。
        LocalDate nextDueDate = null;
        Integer extendMonths = null;
        if (bucket == CollectionBucket.EXPIRING) {
            PawnRenew renewTrial = PawnRenew.apply(ticket, today.atStartOfDay());
            nextDueDate = renewTrial.getNewDueDate();
            extendMonths = renewTrial.getExtendMonths();
        }

        return new CollectionWorkItem(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getPawnerId(),
                pawnerName,
                pawnerPhone,
                ticket.getCollateralId(),
                collateralName,
                dueDate,
                bucket,
                (int) delta,
                overdueDays,
                renewCount,
                redeemTrial.getTotalAmount(),
                redeemTrial.getFeeAmount(),
                redeemTrial.getUsedDays(),
                nextDueDate,
                extendMonths);
    }
}
