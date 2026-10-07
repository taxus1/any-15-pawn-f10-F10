package com.somepro.infrastructure.persistence.forfeit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.forfeit.po.PawnForfeitPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 绝当处置 Mapper（基础设施层）。
 *
 * BaseMapper 覆盖处置单常规 CRUD（含当票/当物状态翻转的条件 update 在各自 Mapper 上）；
 * 处置单号生成与翻单连表查询需要自定义语义，用注解 SQL 写死，不建 XML。
 * 写临界区的命名锁不走 MyBatis（要用独立于事务的连接持锁），见 PawnForfeitRepositoryImpl#inWriteLock。
 *
 * 阻塞 JDBC API，只能在仓储适配器的 blocking(...) 桥接里调用。
 */
@Mapper
public interface PawnForfeitMapper extends BaseMapper<PawnForfeitPO> {

    /**
     * 取某年全部处置单号（序号在 Java 侧取最大，只选 forfeit_no 一列，数据量小）。
     *
     * 刻意不带 del_flag = 0：单号一经分配永久占用 —— 哪怕那条处置单后来被删除，
     * 它的号也不能再发给新单（否则同一 JD 号在账上先后指向两笔处置）。
     * 不能直接 ORDER BY 字符串 DESC LIMIT 1：字符串排序下 JD-2026-9999 会排在 JD-2026-10000 前面。
     */
    @Select("SELECT forfeit_no FROM t_pawn_forfeit WHERE forfeit_no LIKE #{prefix}")
    List<String> findForfeitNosByPrefix(@Param("prefix") String prefix);

    /**
     * 翻处置单（含欠款/盈亏试算要用到的票面快照、对号要用的票号与当物信息）。
     *
     * 主表是处置单 f；INNER JOIN 当票 t（处置单必对一张票，拿票号与当金/起当日/利率费率快照）；
     * LEFT JOIN 当物 c（当物档案若被删不应把处置单吞掉，故 LEFT，仅取编号名称对号）。
     * del_flag 由 @TableLogic 管常规查询，这里是手写 SQL，主表的 f.del_flag = 0 要自己钉上；
     * 关联档案只取未删除的。
     *
     * 动态条件：ticketId / 处置方式集合任一为空即不拼（用 wrapper 的 apply/condition 风格在 XML 脚本里
     * 判空）。排序固定 f.id 升序，翻页稳定、也对得上办理次序。
     * PageHelper 在调用前 startPage，分页与 count 自动织入。
     */
    @Select("""
            <script>
            SELECT f.id               AS id,
                   f.forfeit_no       AS forfeit_no,
                   f.ticket_id        AS ticket_id,
                   f.collateral_id    AS collateral_id,
                   f.forfeited_at     AS forfeited_at,
                   f.dispose_method   AS dispose_method,
                   f.recover_amount   AS recover_amount,
                   f.create_time      AS create_time,
                   t.ticket_no        AS ticket_no,
                   t.pawn_amount      AS pawn_amount,
                   t.start_date       AS start_date,
                   t.monthly_rate     AS monthly_rate,
                   t.service_rate     AS service_rate,
                   c.item_no          AS item_no,
                   c.item_name        AS item_name
            FROM t_pawn_forfeit f
            INNER JOIN t_pawn_ticket t ON t.id = f.ticket_id
            LEFT JOIN t_collateral c ON c.id = f.collateral_id AND c.del_flag = 0
            WHERE f.del_flag = 0
            <if test="ticketId != null">
                AND f.ticket_id = #{ticketId}
            </if>
            <if test="methods != null and methods.size() > 0">
                AND f.dispose_method IN
                <foreach collection="methods" item="m" open="(" separator="," close=")">
                    #{m}
                </foreach>
            </if>
            ORDER BY f.id ASC
            </script>
            """)
    List<ForfeitListRow> selectViewPage(@Param("ticketId") Long ticketId,
                                        @Param("methods") Collection<String> methods);

    /** 单条对账视图：与翻单同一套 JOIN，只点一张处置单。 */
    @Select("""
            SELECT f.id               AS id,
                   f.forfeit_no       AS forfeit_no,
                   f.ticket_id        AS ticket_id,
                   f.collateral_id    AS collateral_id,
                   f.forfeited_at     AS forfeited_at,
                   f.dispose_method   AS dispose_method,
                   f.recover_amount   AS recover_amount,
                   f.create_time      AS create_time,
                   t.ticket_no        AS ticket_no,
                   t.pawn_amount      AS pawn_amount,
                   t.start_date       AS start_date,
                   t.monthly_rate     AS monthly_rate,
                   t.service_rate     AS service_rate,
                   c.item_no          AS item_no,
                   c.item_name        AS item_name
            FROM t_pawn_forfeit f
            INNER JOIN t_pawn_ticket t ON t.id = f.ticket_id
            LEFT JOIN t_collateral c ON c.id = f.collateral_id AND c.del_flag = 0
            WHERE f.del_flag = 0 AND f.id = #{id}
            """)
    ForfeitListRow selectViewById(@Param("id") Long id);

    /** 按处置单号点单条对账视图。 */
    @Select("""
            SELECT f.id               AS id,
                   f.forfeit_no       AS forfeit_no,
                   f.ticket_id        AS ticket_id,
                   f.collateral_id    AS collateral_id,
                   f.forfeited_at     AS forfeited_at,
                   f.dispose_method   AS dispose_method,
                   f.recover_amount   AS recover_amount,
                   f.create_time      AS create_time,
                   t.ticket_no        AS ticket_no,
                   t.pawn_amount      AS pawn_amount,
                   t.start_date       AS start_date,
                   t.monthly_rate     AS monthly_rate,
                   t.service_rate     AS service_rate,
                   c.item_no          AS item_no,
                   c.item_name        AS item_name
            FROM t_pawn_forfeit f
            INNER JOIN t_pawn_ticket t ON t.id = f.ticket_id
            LEFT JOIN t_collateral c ON c.id = f.collateral_id AND c.del_flag = 0
            WHERE f.del_flag = 0 AND f.forfeit_no = #{forfeitNo}
            """)
    ForfeitListRow selectViewByForfeitNo(@Param("forfeitNo") String forfeitNo);
}
