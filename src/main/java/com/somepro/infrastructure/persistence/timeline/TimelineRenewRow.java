package com.somepro.infrastructure.persistence.timeline;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 时间线续当段投影行：t_pawn_renew 中某张票的有效续当（del_flag=0）。
 * 列照续当当初定格落库的原值点回，不重算、不回写。只在基础设施层内使用。
 */
@Getter
@Setter
public class TimelineRenewRow {

    private Long id;
    private String renewNo;
    private LocalDate oldDueDate;
    private LocalDate newDueDate;
    private Integer extendMonths;
    private LocalDateTime renewedAt;
}
