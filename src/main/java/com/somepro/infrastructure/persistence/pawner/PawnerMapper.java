package com.somepro.infrastructure.persistence.pawner;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.pawner.po.PawnerPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 当户档案 Mapper（基础设施层）。
 *
 * BaseMapper 覆盖常规 CRUD；编号生成、身份证唯一性检查需要自定义语义，用注解 SQL 写死，不建 XML。
 * 写临界区的命名锁不走 MyBatis（要用独立于事务的连接持锁），见 PawnerRepositoryImpl#inWriteLock。
 *
 * 阻塞 JDBC API，只能在仓储适配器的 blocking(...) 桥接里调用。
 */
@Mapper
public interface PawnerMapper extends BaseMapper<PawnerPO> {

    /**
     * 统计该身份证名下未注销的有效档案数（业务唯一性检查）。
     * 建表脚本没给「id_card + 未注销」的唯一索引（历史上允许存在多条已注销档案），唯一性由
     * 仓储里的 MySQL 命名锁 GET_LOCK 保证：所有写入在全实例串行，临界区内普通读即可，
     * 故这里不加 FOR UPDATE —— id_card 上是普通二级索引，锁定读会加间隙锁，
     * 不同证号并发插入时间隙锁与插入意向锁互锁反而死锁。
     */
    @Select("SELECT COUNT(*) FROM t_pawner WHERE id_card = #{idCard} AND status <> 'CLOSED' AND del_flag = 0")
    long countActiveByIdCard(@Param("idCard") String idCard);

    /**
     * 同上但排除自身：修改身份证时用。
     */
    @Select("SELECT COUNT(*) FROM t_pawner WHERE id_card = #{idCard} AND status <> 'CLOSED' AND del_flag = 0 "
            + "AND id <> #{excludeId}")
    long countActiveByIdCardExclude(@Param("idCard") String idCard, @Param("excludeId") Long excludeId);

    /**
     * 取某年全部当户编号，序号在 Java 侧取最大（只选 pawner_no 一列，数据量小）。
     * 不能直接 ORDER BY 序号 DESC LIMIT 1：字符串排序下 DH-2026-9999 会排在 DH-2026-10000 前面。
     */
    @Select("SELECT pawner_no FROM t_pawner WHERE pawner_no LIKE #{prefix} AND del_flag = 0")
    List<String> findPawnerNosByPrefix(@Param("prefix") String prefix);

    /**
     * 行锁锁定读当户状态：只给开票 / 续当的写库事务在落库前做冻结门禁用。
     * 必须在事务里调用 —— FOR UPDATE 的行锁随事务提交才释放，与冻结/解冻的条件更新抢同一行锁，
     * 从而把「预检通过、落库前一刻被冻」的并发缝彻底关上（普通预检走 BaseMapper 的 selectById 即可）。
     * 查不到（档案不存在或已逻辑删除）返回 null，由调用方按自己的口径处理。
     */
    @Select("SELECT status FROM t_pawner WHERE id = #{id} AND del_flag = 0 FOR UPDATE")
    String selectStatusForUpdate(@Param("id") Long id);
}
