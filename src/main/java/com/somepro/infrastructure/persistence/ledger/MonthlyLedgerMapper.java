package com.somepro.infrastructure.persistence.ledger;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 月度类别台账只读 Mapper（基础设施层）。
 *
 * 一条 SQL 出整页台账，分四段：
 * - {@code bounds}：四类业务办理时刻的最早 / 最晚值（MIN/MAX 自动跳过 NULL），
 *   用来确定「不按月筛」时要铺到哪个月为止；
 * - {@code months}：MySQL 8 递归 CTE 按月步进，把起止区间内每个自然月铺全，没有业务的月份也在；
 * - {@code cats}：要出现的类别（由领域枚举 Category 传入）；
 * - 四档汇总（开票 / 续当 / 赎当 / 绝当）各自独立 GROUP BY 月+类别，最后 LEFT JOIN 到格子上，
 *   缺档补 0。四档分开算，办理时刻、归属类别各算各的，不串月、不串类别。
 *
 * 归月口径（自然月，看办理时刻）：
 * - 新开票：t_pawn_ticket.create_time（开票办理时刻），类别用票面 category 快照；
 * - 续当：t_pawn_renew.renewed_at（续当办理时刻），类别经当票带出；
 * - 赎当：t_pawn_redeem.redeemed_at（赎当办理时刻），本金 SUM(t.pawn_amount)、
 *   费用 SUM(d.fee_amount) —— 都是「逐笔赎当」同一口径，逐笔结出来再累加；
 * - 绝当：t_pawn_forfeit.forfeited_at（处置时刻）。
 *
 * 已销掉的一律不算：各业务表只认 del_flag=0；续当 / 赎当 / 绝当 INNER JOIN 当票且
 * 当票也必须 del_flag=0，当票被销的业务事件不进台账。
 * 月底最后一天的业务归当月、次月头一天归次月，全靠 DATE_FORMAT(时刻,'%Y-%m') 取自然月。
 *
 * 阻塞 JDBC API，只能在仓储适配器的 blocking(...) 桥接里调用；分页由 PageHelper
 * 在调用前 {@code PageHelper.startPage} 织入（含 count）。
 */
@Mapper
public interface MonthlyLedgerMapper {

    @Select("""
            <script>
            WITH RECURSIVE bounds AS (
                SELECT MIN(ev.biz_time) AS at_min, MAX(ev.biz_time) AS at_max
                FROM (
                    SELECT t.create_time AS biz_time FROM t_pawn_ticket t
                        WHERE t.del_flag = 0
                    UNION ALL
                    SELECT r.renewed_at FROM t_pawn_renew r
                        INNER JOIN t_pawn_ticket t ON t.id = r.ticket_id AND t.del_flag = 0
                        WHERE r.del_flag = 0
                    UNION ALL
                    SELECT d.redeemed_at FROM t_pawn_redeem d
                        INNER JOIN t_pawn_ticket t ON t.id = d.ticket_id AND t.del_flag = 0
                        WHERE d.del_flag = 0
                    UNION ALL
                    SELECT f.forfeited_at FROM t_pawn_forfeit f
                        INNER JOIN t_pawn_ticket t ON t.id = f.ticket_id AND t.del_flag = 0
                        WHERE f.del_flag = 0
                ) ev
            ),
            months AS (
                SELECT COALESCE(#{month}, DATE_FORMAT(b.at_min, '%Y-%m'), #{todayMonth}) AS m
                FROM bounds b
                UNION ALL
                SELECT DATE_FORMAT(DATE_ADD(STR_TO_DATE(CONCAT(m, '-01'), '%Y-%m-%d'), INTERVAL 1 MONTH), '%Y-%m')
                FROM months, bounds b
                WHERE m &lt; LEAST(
                          COALESCE(#{month}, #{todayMonth}),
                          COALESCE(DATE_FORMAT(b.at_max, '%Y-%m'), #{todayMonth})
                      )
            ),
            cats AS (
                <foreach collection="categories" item="code" separator=" UNION ALL ">
                    SELECT #{code} AS category
                </foreach>
            ),
            ticket_stat AS (
                SELECT DATE_FORMAT(t.create_time, '%Y-%m') AS m,
                       t.category AS c,
                       COUNT(*) AS cnt,
                       SUM(t.pawn_amount) AS amount
                FROM t_pawn_ticket t
                WHERE t.del_flag = 0 AND t.create_time IS NOT NULL
                GROUP BY m, c
            ),
            renew_stat AS (
                SELECT DATE_FORMAT(r.renewed_at, '%Y-%m') AS m,
                       t.category AS c,
                       COUNT(*) AS cnt
                FROM t_pawn_renew r
                INNER JOIN t_pawn_ticket t ON t.id = r.ticket_id AND t.del_flag = 0
                WHERE r.del_flag = 0 AND r.renewed_at IS NOT NULL
                GROUP BY m, c
            ),
            redeem_stat AS (
                SELECT DATE_FORMAT(d.redeemed_at, '%Y-%m') AS m,
                       t.category AS c,
                       COUNT(*) AS cnt,
                       SUM(t.pawn_amount) AS principal,
                       SUM(d.fee_amount) AS fee
                FROM t_pawn_redeem d
                INNER JOIN t_pawn_ticket t ON t.id = d.ticket_id AND t.del_flag = 0
                WHERE d.del_flag = 0 AND d.redeemed_at IS NOT NULL
                GROUP BY m, c
            ),
            forfeit_stat AS (
                SELECT DATE_FORMAT(f.forfeited_at, '%Y-%m') AS m,
                       t.category AS c,
                       COUNT(*) AS cnt
                FROM t_pawn_forfeit f
                INNER JOIN t_pawn_ticket t ON t.id = f.ticket_id AND t.del_flag = 0
                WHERE f.del_flag = 0 AND f.forfeited_at IS NOT NULL
                GROUP BY m, c
            )
            SELECT mo.m AS month,
                   ca.category AS category,
                   COALESCE(ts.cnt, 0)       AS newTicketCount,
                   COALESCE(ts.amount, 0)    AS newPawnAmount,
                   COALESCE(rs.cnt, 0)       AS renewCount,
                   COALESCE(ds.cnt, 0)       AS redeemCount,
                   COALESCE(ds.principal, 0) AS redeemPrincipalAmount,
                   COALESCE(ds.fee, 0)       AS redeemFeeAmount,
                   COALESCE(fs.cnt, 0)       AS forfeitCount
            FROM months mo
            CROSS JOIN cats ca
            LEFT JOIN ticket_stat  ts ON ts.m = mo.m AND ts.c = ca.category
            LEFT JOIN renew_stat   rs ON rs.m = mo.m AND rs.c = ca.category
            LEFT JOIN redeem_stat  ds ON ds.m = mo.m AND ds.c = ca.category
            LEFT JOIN forfeit_stat fs ON fs.m = mo.m AND fs.c = ca.category
            ORDER BY mo.m DESC, ca.category ASC
            </script>
            """)
    List<MonthlyLedgerRow> selectLedgerPage(@Param("month") String month,
                                            @Param("todayMonth") String todayMonth,
                                            @Param("categories") List<String> categories);
}
