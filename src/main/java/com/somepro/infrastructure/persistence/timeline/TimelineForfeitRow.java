package com.somepro.infrastructure.persistence.timeline;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 时间线绝当段投影行：t_pawn_forfeit 中某张票的有效绝当处置（del_flag=0）。
 * 处置方式与处置回款照处置当初落库的原值点回。只在基础设施层内使用。
 */
@Getter
@Setter
public class TimelineForfeitRow {

    private Long id;
    private String forfeitNo;
    private LocalDateTime forfeitedAt;
    private String disposeMethod;
    private BigDecimal recoverAmount;
}
