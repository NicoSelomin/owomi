package dev.selonick.owomi.transaction.projection;

import java.math.BigDecimal;

public interface MonthlyBalanceSummary {

    Integer getYear();

    Integer getMonth();

    BigDecimal getIncomeTotal();

    BigDecimal getExpenseTotal();

    BigDecimal getBalance();
}
