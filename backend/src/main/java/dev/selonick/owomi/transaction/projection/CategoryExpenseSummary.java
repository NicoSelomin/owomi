package dev.selonick.owomi.transaction.projection;

import java.math.BigDecimal;

public interface CategoryExpenseSummary {

    Long getCategoryId();

    String getCategoryName();

    String getCategoryColor();

    BigDecimal getTotalAmount();

    Long getTransactionCount();
}
