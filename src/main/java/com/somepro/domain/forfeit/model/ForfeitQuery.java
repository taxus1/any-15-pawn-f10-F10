package com.somepro.domain.forfeit.model;

/**
 * 绝当处置单翻单条件（不可变值对象）。
 *
 * 当票（ticketId）、处置方式随意拼，任一项为 null 即不参与过滤；全 null 翻整份处置单。
 * 处置方式在进入本对象前由 {@link DisposeMethod#ofCode} 解析过，非法写法已在解析阶段挡回。
 */
public record ForfeitQuery(Long ticketId, DisposeMethod disposeMethod) {

    public static ForfeitQuery of(Long ticketId, DisposeMethod disposeMethod) {
        return new ForfeitQuery(ticketId, disposeMethod);
    }
}
