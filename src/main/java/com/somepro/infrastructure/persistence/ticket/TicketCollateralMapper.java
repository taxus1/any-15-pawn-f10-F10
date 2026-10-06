package com.somepro.infrastructure.persistence.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.ticket.po.TicketCollateralPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 当票模块读写当物表的 Mapper（基础设施层）：开票读快照、开票置「已典当」、撤销回「在库」。
 *
 * 状态联动全部走 BaseMapper 的条件更新（entity + wrapper），审计字段由 MetaObjectHandler 自动填充；
 * 与当票的写入在同一事务里执行（见 PawnTicketRepositoryImpl），票和物的状态不会各走各的。
 *
 * 阻塞 JDBC API，只能在仓储适配器的 blocking(...) 桥接里调用。
 */
@Mapper
public interface TicketCollateralMapper extends BaseMapper<TicketCollateralPO> {
}
