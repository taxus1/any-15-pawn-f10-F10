package com.somepro.infrastructure.persistence.timeline;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 当票时间线只读 Mapper（基础设施层）。
 *
 * 一条线点四次：票头一次（当票 INNER 主表 + 当户 / 当物 LEFT JOIN），
 * 续当 / 赎当 / 绝当各点一次有效事件。不做任何写入。
 * 阻塞 JDBC API，只能在仓储适配器的 blocking(...) 桥接里调用。
 *
 * 删除口径（与需求钉死）：
 * - 当票主表必须 del_flag=0：当票本身已销掉的不再拿号查，票头点空，整条线返回空；
 * - 续当 / 赎当 / 绝当三表都钉 del_flag=0，已销掉的单子不串进时间线；
 * - 当户 / 当物档案走 LEFT JOIN 且【不】钉 del_flag=0：建档与登记是线上固定两段，
 *   档案后来被销也要把编号、名字原样带出来（用 p.del_flag / c.del_flag 标出），
 *   不因档案缺失把历史一段吞掉。
 *
 * 排序不在这里做：排序口径只认业务时刻（补录单 create_time 在后、业务时刻却早），
 * 统一在领域装配 {@code TicketTimeline#assemble} 里排，避免误用落库时刻。
 */
@Mapper
public interface TicketTimelineMapper {

    /**
     * 票头：按当票号点一张有效票，并带出当户、当物档案（LEFT JOIN，含已销档案）。
     * 点不到（号不存在 / 票已销）返回 null。
     */
    @Select("""
            SELECT t.id                AS ticket_id,
                   t.ticket_no         AS ticket_no,
                   t.pawner_id         AS pawner_id,
                   t.collateral_id     AS collateral_id,
                   t.category          AS category,
                   t.pawn_amount       AS pawn_amount,
                   t.appraised_value   AS appraised_value,
                   t.monthly_rate      AS monthly_rate,
                   t.service_rate      AS service_rate,
                   t.start_date        AS start_date,
                   t.due_date          AS due_date,
                   t.term_months       AS term_months,
                   t.status            AS status,
                   t.create_time       AS ticket_create_time,
                   p.pawner_no         AS pawner_no,
                   p.name              AS pawner_name,
                   p.create_time       AS pawner_create_time,
                   p.del_flag          AS pawner_del_flag,
                   c.item_no           AS item_no,
                   c.category          AS collateral_category,
                   c.item_name         AS item_name,
                   c.brand             AS brand,
                   c.appraised_value   AS item_appraised_value,
                   c.create_time       AS collateral_create_time,
                   c.del_flag          AS collateral_del_flag
            FROM t_pawn_ticket t
            LEFT JOIN t_pawner p ON p.id = t.pawner_id
            LEFT JOIN t_collateral c ON c.id = t.collateral_id
            WHERE t.ticket_no = #{ticketNo} AND t.del_flag = 0
            LIMIT 1
            """)
    TimelineHeadRow selectHeadByTicketNo(@Param("ticketNo") String ticketNo);

    /** 这张票的有效续当（del_flag=0），列全是当初定格的原值。 */
    @Select("""
            SELECT id, renew_no, old_due_date, new_due_date, extend_months, renewed_at
            FROM t_pawn_renew
            WHERE ticket_id = #{ticketId} AND del_flag = 0
            """)
    List<TimelineRenewRow> selectRenews(@Param("ticketId") Long ticketId);

    /** 这张票的有效赎当（del_flag=0），计费天数/费用/总额照原值。 */
    @Select("""
            SELECT id, redeem_no, redeemed_at, used_days, fee_amount, total_amount
            FROM t_pawn_redeem
            WHERE ticket_id = #{ticketId} AND del_flag = 0
            """)
    List<TimelineRedeemRow> selectRedeems(@Param("ticketId") Long ticketId);

    /** 这张票的有效绝当处置（del_flag=0），处置方式/回款照原值。 */
    @Select("""
            SELECT id, forfeit_no, forfeited_at, dispose_method, recover_amount
            FROM t_pawn_forfeit
            WHERE ticket_id = #{ticketId} AND del_flag = 0
            """)
    List<TimelineForfeitRow> selectForfeits(@Param("ticketId") Long ticketId);
}
