package com.somepro.interfaces.rest.rate.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 停用费率配置入参（用户接口层）。只认配置 id。
 */
@Getter
@Setter
public class RateIdRequest {

    /** 要停用的是哪条配置（t_pawn_rate.id）。 */
    private Long id;
}
