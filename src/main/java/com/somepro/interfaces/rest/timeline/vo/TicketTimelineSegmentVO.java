package com.somepro.interfaces.rest.timeline.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 当票时间线一段（对外 VO，不可变 record）。
 *
 * 六类段共用这一个宽 record，用 {@code type} 区分本段是哪类事；各段特有的数放在对应字段上，
 * 与本类段无关的字段为 null，Jackson 全局 non_null 序列化时自动省略。
 *
 * 公共件：
 * <ul>
 *   <li>type / typeLabel 段类型码与中文名；</li>
 *   <li>bizNo 这段自己的编号：建档段=当户编号、登记段=当物编号、开票段=当票号、
 *       续当段=续当单号、赎当段=赎当单号、绝当段=绝当单号；</li>
 *   <li>bizTime 业务发生时刻（排序唯一口径，最新的段排在最前）；</li>
 *   <li>deleted 仅建档 / 登记段可能为 true：档案后来被销，段仍在线上。</li>
 * </ul>
 * 开票段：pawnAmount 当金、appraisedValue 折当估值快照、monthlyRate/serviceRate 费率快照、
 * startDate 起当日期、dueDate 到期日期、termMonths 当期月数、status/statusLabel 票当前结果。
 * 登记段另带 category/categoryLabel、itemName、brand、collateralAppraisedValue。
 * 续当段：oldDueDate/newDueDate 顺延前/后到期日期、extendMonths 顺延月数。
 * 赎当段：usedDays 计费天数、feeAmount 利息与综合费、totalAmount 应还总额。
 * 绝当段：disposeMethod/disposeMethodLabel 处置方式、recoverAmount 处置回款。
 * 金额单位均为元；所有数照各段当初落库原值带出，非现算。
 */
public record TicketTimelineSegmentVO(String type,
                                      String typeLabel,
                                      String bizNo,
                                      LocalDateTime bizTime,
                                      Boolean deleted,
                                      String name,
                                      String category,
                                      String categoryLabel,
                                      String itemName,
                                      String brand,
                                      BigDecimal collateralAppraisedValue,
                                      BigDecimal pawnAmount,
                                      BigDecimal appraisedValue,
                                      BigDecimal monthlyRate,
                                      BigDecimal serviceRate,
                                      LocalDate startDate,
                                      LocalDate dueDate,
                                      Integer termMonths,
                                      String status,
                                      String statusLabel,
                                      LocalDate oldDueDate,
                                      LocalDate newDueDate,
                                      Integer extendMonths,
                                      Integer usedDays,
                                      BigDecimal feeAmount,
                                      BigDecimal totalAmount,
                                      String disposeMethod,
                                      String disposeMethodLabel,
                                      BigDecimal recoverAmount) implements Serializable {
}
