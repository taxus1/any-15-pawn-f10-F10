package com.somepro.infrastructure.persistence.renew;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.renew.po.PawnRenewPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 续当登记 Mapper（基础设施层）。
 *
 * BaseMapper 覆盖常规 CRUD（含当票到期日期推进的条件 update）；续当单号生成需要自定义查询，
 * 用注解 SQL 写死，不建 XML。写临界区的命名锁不走 MyBatis（要用独立于事务的连接持锁），
 * 见 PawnRenewRepositoryImpl#inWriteLock。
 *
 * 阻塞 JDBC API，只能在仓储适配器的 blocking(...) 桥接里调用。
 */
@Mapper
public interface PawnRenewMapper extends BaseMapper<PawnRenewPO> {

    /**
     * 取某年全部续当单号（序号在 Java 侧取最大，只选 renew_no 一列，数据量小）。
     *
     * 刻意不带 del_flag = 0：单号一经分配永久占用 —— 哪怕那条续当后来被删除，
     * 它的号也不能再发给新单（否则同一 XD 号在账上先后指向两笔办理）。
     * 不能直接 ORDER BY 字符串 DESC LIMIT 1：字符串排序下 XD-2026-9999 会排在 XD-2026-10000 前面。
     */
    @Select("SELECT renew_no FROM t_pawn_renew WHERE renew_no LIKE #{prefix}")
    List<String> findRenewNosByPrefix(@Param("prefix") String prefix);
}
