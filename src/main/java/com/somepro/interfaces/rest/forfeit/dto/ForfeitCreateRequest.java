package com.somepro.interfaces.rest.forfeit.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 办理绝当处置入参（用户接口层）。
 *
 * 用可变 bean + @ModelAttribute：WebFlux 下 application/x-www-form-urlencoded 表单、
 * query string 都能直接绑定。只认当票 id、处置方式、处置回款：
 * 处置单号由服务端按 JD-年份-序号 生成、处置时刻取服务端当下、押的哪件当物照票面带出，
 * 统统不接受外部指定。回款用字符串接收，由应用层解析（非数字/负数给明确业务提示，零可）。
 */
@Getter
@Setter
public class ForfeitCreateRequest {

    /** 绝的是哪张当票（t_pawn_ticket.id）。 */
    private Long ticketId;

    /** 处置方式：AUCTION 拍卖 / CONSIGN 变卖 / WRITE_OFF 核销。 */
    private String disposeMethod;

    /** 处置回款（元）：实际到手多少写多少，没有回款填 0，负数不收。 */
    private String recoverAmount;
}
