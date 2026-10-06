package com.somepro.infrastructure.persistence.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.ticket.po.PawnRatePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 费率配置表只读 Mapper（基础设施层）：当票模块只查「该类别当前生效的那一行」，不做任何写入。
 *
 * 阻塞 JDBC API，只能在仓储适配器的 blocking(...) 桥接里调用。
 */
@Mapper
public interface PawnRateMapper extends BaseMapper<PawnRatePO> {
}
