package com.somepro.interfaces.rest.timeline.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 时间线段落对外对象（不可变 record）。
 *
 * 一段只带自己那类字段，其余为 null；全局 Jackson non_null，null 字段不序列化，
 * 看的人只见到这段该有的内容。每段必带：eventType/eventTypeLabel（哪一段）、
 * bizTime（业务发生时刻）、bizNo（这段自己的编号：当户编号/当物编号/当票号/
 * 续当单号/赎当单号/绝当单号）。各段的数照当初落库的值原样带出。
 */
public record TimelineEventVO(
        // ---- 段头 ----
        String eventType,
        String eventTypeLabel,
        LocalDateTime bizTime,
        String bizNo,
        // ---- 当户建档段 ----
        String pawnerName,
        String pawnerIdCard,
        String pawnerPhone,
        // ---- 当物登记段 ----
        String itemName,
        String category,
        String categoryLabel,
        String brand,
        String conditionLevel,
        String conditionLevelLabel,
        BigDecimal appraisedValue,
        // ---- 当票开立段 ----
        BigDecimal pawnAmount,
        LocalDate startDate,
        LocalDate dueDate,
        Integer termMonths,
        // ---- 续当段 ----
        LocalDate oldDueDate,
        LocalDate newDueDate,
        Integer extendMonths,
        // ---- 赎当结清段 ----
        Integer usedDays,
        BigDecimal feeAmount,
        BigDecimal totalAmount,
        // ---- 绝当处置段 ----
        String disposeMethod,
        String disposeMethodLabel,
        BigDecimal recoverAmount) implements Serializable {
}
