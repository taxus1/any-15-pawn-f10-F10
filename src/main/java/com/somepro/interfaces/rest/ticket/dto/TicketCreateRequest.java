package com.somepro.interfaces.rest.ticket.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 开立当票入参（用户接口层）。
 *
 * 用可变 bean + @ModelAttribute：WebFlux 下 application/x-www-form-urlencoded 表单、
 * query string 都能直接绑定。当金用字符串接收，由应用层解析（非数字/零/负数给明确业务提示）。
 * 起当日期可不传（按行里当天算）；票号由服务端生成，当户/类别/估值/利率费率都由服务端
 * 随当物与费率配置带出，不接受外部指定。
 */
@Getter
@Setter
public class TicketCreateRequest {

    /** 押的是哪件当物（t_collateral.id），必须是在册的当物。 */
    private Long collateralId;

    private String pawnAmount;

    /** 起当日期，yyyy-MM-dd；不传按当天算。 */
    private String startDate;

    private Integer termMonths;
}
