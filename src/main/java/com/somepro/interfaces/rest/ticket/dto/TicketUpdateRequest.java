package com.somepro.interfaces.rest.ticket.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 修改当票入参（用户接口层）。
 *
 * 当金 / 起当日期 / 当期月数任一项留空表示该项不动；到期日期由服务端按起当日期 + 当期月数重算。
 * 票号、当户、当物、类别、估值与利率费率快照不允许通过本接口改动，故这里不出现这些字段。
 */
@Getter
@Setter
public class TicketUpdateRequest {

    private Long id;

    private String pawnAmount;

    private String startDate;

    private Integer termMonths;
}
