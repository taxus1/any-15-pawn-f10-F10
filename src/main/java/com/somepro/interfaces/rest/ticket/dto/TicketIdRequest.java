package com.somepro.interfaces.rest.ticket.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 按 id 操作当票的入参（撤销）。POST 表单体在 WebFlux 下需经 @ModelAttribute 绑定。
 */
@Getter
@Setter
public class TicketIdRequest {

    private Long id;
}
