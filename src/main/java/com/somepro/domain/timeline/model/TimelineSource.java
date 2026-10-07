package com.somepro.domain.timeline.model;

import com.somepro.domain.collateral.model.Collateral;
import com.somepro.domain.forfeit.model.PawnForfeit;
import com.somepro.domain.pawner.model.Pawner;
import com.somepro.domain.redeem.model.PawnRedeem;
import com.somepro.domain.renew.model.PawnRenew;
import com.somepro.domain.ticket.model.PawnTicket;

import java.util.List;

/**
 * 串一条时间线的原料（只读领域视图，不可变 record）：一张票 + 它挂的当户、当物、
 * 以及这张票名下全部未销掉的续当 / 赎当 / 绝当记录。
 *
 * 由仓储按票号一次取回；当户或当物档案缺失（已销户/已销掉）时为 null，
 * 由 {@link TicketTimeline#assemble} 记一笔对不上的账，而不是让整条线出不来。
 * 续当 / 赎当 / 绝当三摞都按列表接：正常账下赎当与绝当各自至多一笔，
 * 真挂出多笔正是「账串了」，要原样摆出来并记上，不能悄悄只取一条。
 */
public record TimelineSource(PawnTicket ticket,
                             Pawner pawner,
                             Collateral collateral,
                             List<PawnRenew> renews,
                             List<PawnRedeem> redeems,
                             List<PawnForfeit> forfeits) {

    /** 紧凑构造：三摞记录 null 一律归一成空列表，装配侧不用处处判空。 */
    public TimelineSource {
        renews = renews == null ? List.of() : List.copyOf(renews);
        redeems = redeems == null ? List.of() : List.copyOf(redeems);
        forfeits = forfeits == null ? List.of() : List.copyOf(forfeits);
    }
}
