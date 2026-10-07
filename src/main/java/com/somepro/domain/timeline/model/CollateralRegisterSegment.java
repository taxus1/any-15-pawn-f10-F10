package com.somepro.domain.timeline.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 时间线第二段：当物登记（拿来的是什么东西）。
 *
 * 编号带当物编号 itemNo（DW-年份-序号）；类别、名称、品牌或成色、登记时评估价值原样带出。
 * 业务时刻取登记记录 create_time —— 登记没有单独的「办理时刻」列，落账时刻即登记时刻。
 * 当物档案后来被逻辑删除的，段仍在线上（历史一段不抹），以 deleted=true 标出。
 * 注意这里的估值是登记底账上的值；开票段另带一份折当估值快照，两份各归各、不互相回写。
 */
public record CollateralRegisterSegment(Long id,
                                        String itemNo,
                                        String category,
                                        String categoryLabel,
                                        String itemName,
                                        String brand,
                                        BigDecimal appraisedValue,
                                        LocalDateTime registeredAt,
                                        boolean deleted) implements TimelineSegment {

    @Override
    public SegmentType type() {
        return SegmentType.COLLATERAL_REGISTER;
    }

    @Override
    public String bizNo() {
        return itemNo;
    }

    @Override
    public LocalDateTime bizTime() {
        return registeredAt;
    }

    @Override
    public Long segmentId() {
        return id;
    }
}
