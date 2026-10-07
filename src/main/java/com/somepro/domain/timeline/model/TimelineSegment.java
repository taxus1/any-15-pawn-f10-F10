package com.somepro.domain.timeline.model;

import java.time.LocalDateTime;

/**
 * 当票时间线上的一段（只读领域值对象，不可变）。
 *
 * 一段 = 这张票生命周期里发生过的一件事：当户建档 / 当物登记 / 开票 / 某次续当 /
 * 赎当结清 / 绝当处置。每段必带三样「对号」用的公共件：
 * <ul>
 *   <li>{@link #type()} 段类型，区分这段办的是哪一类事；</li>
 *   <li>{@link #bizNo()} 这段自己的编号 —— 当户编号 / 当物编号 / 当票号 /
 *       续当单号 / 赎当单号 / 绝当单号，各段带各段的号，一眼对得上；</li>
 *   <li>{@link #bizTime()} 业务发生时刻 —— 排序只认它，不认落库时刻
 *       （补录的单子 create_time 在后，业务上却是早的）。</li>
 * </ul>
 * 各段特有的业务数（当金、到期日期、费用、回款……）由各实现 record 自己带，
 * 一律照各表当初落库的原值，不按现在挂的配置重算。
 */
public sealed interface TimelineSegment
        permits PawnerRegisterSegment, CollateralRegisterSegment, TicketIssueSegment,
                RenewSegment, RedeemSegment, ForfeitSegment {

    /** 段类型：办的是哪一类事。 */
    SegmentType type();

    /** 这段自己的业务编号（对号用）。 */
    String bizNo();

    /**
     * 业务发生时刻（排序唯一口径）。
     * 建档 / 登记 / 开票取各自 create_time（办理当下落账）；
     * 续当取 renewed_at、赎当取 redeemed_at、绝当取 forfeited_at（业务时刻，可补录）。
     * 理论上非空；极端脏数据为 null 时排序沉到最后，不报错。
     */
    LocalDateTime bizTime();

    /**
     * 行 id：同一业务时刻、同一类型下的最后一级稳定排序键，
     * 也用来在同刻多段（如补录时刻撞点）时把次序钉死、翻页稳定。
     */
    Long segmentId();
}
