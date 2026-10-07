package com.somepro.infrastructure.persistence.collection;

import com.somepro.infrastructure.persistence.collection.po.CollectionWorkRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 催收工作台只读 Mapper（基础设施层）。
 *
 * 一条连表 SQL 把催收一屏要点的东西一次点齐：在当票（主）+ 当户姓名电话 + 当物名称 + 续当次数。
 * 不做任何写入。阻塞 JDBC API，只能在仓储适配器的 blocking(...) 桥接里调用；
 * 分页由 PageHelper 在调用前 {@code PageHelper.startPage} 织入（含 count）。
 *
 * 入箱口径（与 CollectionQuery 对齐）：
 * - 主表只认 status='ACTIVE' 且 del_flag=0 的票 —— 已赎 / 已绝当 / 已撤销 / 删除标记一概不进；
 * - 到期日落在两档之一：早于今天（已逾期），或今天到 today+7（含两端，快到期）；
 * - 当户、当物走 LEFT JOIN 且只取未删除档案：催收的筛选基准是票，档案异常不应把票吞掉。
 * - 续当次数只点 t_pawn_renew 有效行（del_flag=0）。
 *
 * 排序：到期日升序（近的在前），同一天按票号升序，翻页稳定。
 */
@Mapper
public interface CollectionWorkbenchMapper {

    @Select("""
            <script>
            SELECT t.id                AS ticket_id,
                   t.ticket_no         AS ticket_no,
                   t.pawner_id         AS pawner_id,
                   t.collateral_id     AS collateral_id,
                   t.pawn_amount       AS pawn_amount,
                   t.start_date        AS start_date,
                   t.due_date          AS due_date,
                   t.monthly_rate      AS monthly_rate,
                   t.service_rate      AS service_rate,
                   t.term_months       AS term_months,
                   p.name              AS pawner_name,
                   p.phone             AS pawner_phone,
                   c.item_name         AS collateral_name,
                   (SELECT COUNT(1) FROM t_pawn_renew r
                       WHERE r.ticket_id = t.id AND r.del_flag = 0) AS renew_count
            FROM t_pawn_ticket t
            LEFT JOIN t_pawner p ON p.id = t.pawner_id AND p.del_flag = 0
            LEFT JOIN t_collateral c ON c.id = t.collateral_id AND c.del_flag = 0
            WHERE t.status = 'ACTIVE' AND t.del_flag = 0
              AND (
                (#{overdue} = 1 AND t.due_date &lt; #{today})
                OR
                (#{expiring} = 1 AND t.due_date &gt;= #{today} AND t.due_date &lt;= #{deadline})
              )
            ORDER BY t.due_date ASC, t.ticket_no ASC
            </script>
            """)
    List<CollectionWorkRow> selectWorkbenchPage(@Param("today") LocalDate today,
                                                @Param("deadline") LocalDate deadline,
                                                @Param("overdue") int overdue,
                                                @Param("expiring") int expiring);
}
