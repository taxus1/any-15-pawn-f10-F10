package com.somepro.interfaces.rest.ledger.vo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 月度类别台账一行（对外 VO，不可变 record）。
 *
 * category 是类别代码，categoryLabel 是给老板看的中文名（如 WATCH / 名表）。
 * 各档金额单位为元，两位小数。
 */
public record MonthlyCategoryLedgerVO(String month,
                                      String category,
                                      String categoryLabel,
                                      Long newTicketCount,
                                      BigDecimal newPawnAmount,
                                      Long renewCount,
                                      Long redeemCount,
                                      BigDecimal redeemPrincipalAmount,
                                      BigDecimal redeemFeeAmount,
                                      Long forfeitCount) implements Serializable {
}
