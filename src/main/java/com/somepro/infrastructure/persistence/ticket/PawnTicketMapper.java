package com.somepro.infrastructure.persistence.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.ticket.po.PawnTicketPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 当票 Mapper（基础设施层）。
 *
 * BaseMapper 覆盖常规 CRUD；票号生成与「一物一票」检查需要自定义语义，用注解 SQL 写死，不建 XML。
 * 写临界区的命名锁不走 MyBatis（要用独立于事务的连接持锁），见 PawnTicketRepositoryImpl#inWriteLock。
 *
 * 阻塞 JDBC API，只能在仓储适配器的 blocking(...) 桥接里调用。
 */
@Mapper
public interface PawnTicketMapper extends BaseMapper<PawnTicketPO> {

    /**
     * 取某年全部当票号（序号在 Java 侧取最大，只选 ticket_no 一列，数据量小）。
     *
     * 刻意不带 del_flag = 0：票号一经分配永久占用 —— 哪怕那张票后来被撤销、被删除，
     * 它的号也不能再发给新票（否则同一 DP 号在账上先后指向两笔生意）。
     * 不能直接 ORDER BY 字符串 DESC LIMIT 1：字符串排序下 DP-2026-9999 会排在 DP-2026-10000 前面。
     */
    @Select("SELECT ticket_no FROM t_pawn_ticket WHERE ticket_no LIKE #{prefix}")
    List<String> findTicketNosByPrefix(@Param("prefix") String prefix);

    /**
     * 该当物名下没结清（在当 ACTIVE、未删除）的当票数。
     * 只在写锁内调用：> 0 就挡回，一件当物同一时刻只准挂一张在当的票。
     */
    @Select("SELECT COUNT(*) FROM t_pawn_ticket WHERE collateral_id = #{collateralId} "
            + "AND status = 'ACTIVE' AND del_flag = 0")
    long countActiveByCollateral(@Param("collateralId") Long collateralId);
}
