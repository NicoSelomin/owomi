package dev.selonick.owomi.dashboard;

import dev.selonick.owomi.common.enums.TransactionType;
import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.dashboard.dto.CategoryExpenseResponse;
import dev.selonick.owomi.dashboard.dto.DashboardSummaryResponse;
import dev.selonick.owomi.dashboard.dto.MonthlyBalanceResponse;
import dev.selonick.owomi.transaction.TransactionRepository;
import dev.selonick.owomi.transaction.projection.CategoryExpenseSummary;
import dev.selonick.owomi.transaction.projection.MonthlyBalanceSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("summary : sans dates utilise exactement le mois courant")
    void getSummary_WithoutDates_ShouldUseCurrentMonth() {
        UUID userId = UUID.randomUUID();
        YearMonth currentMonth = YearMonth.now();
        LocalDate expectedStart = currentMonth.atDay(1);
        LocalDate expectedEnd = currentMonth.atEndOfMonth();

        when(transactionRepository.sumAmountByUserAndTypeAndPeriod(
                userId,
                TransactionType.INCOME,
                expectedStart,
                expectedEnd
        )).thenReturn(new BigDecimal("1000.00"));
        when(transactionRepository.sumAmountByUserAndTypeAndPeriod(
                userId,
                TransactionType.EXPENSE,
                expectedStart,
                expectedEnd
        )).thenReturn(new BigDecimal("250.00"));
        when(transactionRepository.countByUserIdAndPeriod(userId, expectedStart, expectedEnd)).thenReturn(3L);

        DashboardSummaryResponse result = dashboardService.getSummary(userId, null, null);

        assertThat(result.startDate()).isEqualTo(expectedStart);
        assertThat(result.endDate()).isEqualTo(expectedEnd);
        assertThat(result.incomeTotal()).isEqualByComparingTo("1000.00");
        assertThat(result.expenseTotal()).isEqualByComparingTo("250.00");
        assertThat(result.balance()).isEqualByComparingTo("750.00");
        assertThat(result.transactionCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("summary : période explicite valide calcule revenus, dépenses, solde et count")
    void getSummary_WithExplicitPeriod_ShouldReturnAggregates() {
        UUID userId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(transactionRepository.sumAmountByUserAndTypeAndPeriod(
                userId,
                TransactionType.INCOME,
                startDate,
                endDate
        )).thenReturn(new BigDecimal("500.00"));
        when(transactionRepository.sumAmountByUserAndTypeAndPeriod(
                userId,
                TransactionType.EXPENSE,
                startDate,
                endDate
        )).thenReturn(new BigDecimal("125.50"));
        when(transactionRepository.countByUserIdAndPeriod(userId, startDate, endDate)).thenReturn(4L);

        DashboardSummaryResponse result = dashboardService.getSummary(userId, startDate, endDate);

        assertThat(result.incomeTotal()).isEqualByComparingTo("500.00");
        assertThat(result.expenseTotal()).isEqualByComparingTo("125.50");
        assertThat(result.balance()).isEqualByComparingTo("374.50");
        assertThat(result.transactionCount()).isEqualTo(4L);
        assertThat(result.startDate()).isEqualTo(startDate);
        assertThat(result.endDate()).isEqualTo(endDate);
    }

    @Test
    @DisplayName("summary : une seule date fournie → VALIDATION_ERROR")
    void getSummary_WithOnlyOneDate_ShouldThrowValidationError() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> dashboardService.getSummary(userId, LocalDate.of(2026, 1, 1), null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> dashboardService.getSummary(userId, null, LocalDate.of(2026, 1, 31)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("summary : période inversée → VALIDATION_ERROR")
    void getSummary_WithInvertedPeriod_ShouldThrowValidationError() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> dashboardService.getSummary(
                userId,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 31)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("summary : période supérieure à 24 mois calendaires → VALIDATION_ERROR")
    void getSummary_WithMoreThan24CalendarMonths_ShouldThrowValidationError() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> dashboardService.getSummary(
                userId,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2026, 1, 1)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("summary : période exactement égale à 24 mois calendaires acceptée")
    void getSummary_WithExactly24CalendarMonths_ShouldBeAccepted() {
        UUID userId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);

        when(transactionRepository.sumAmountByUserAndTypeAndPeriod(
                userId,
                TransactionType.INCOME,
                startDate,
                endDate
        )).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumAmountByUserAndTypeAndPeriod(
                userId,
                TransactionType.EXPENSE,
                startDate,
                endDate
        )).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.countByUserIdAndPeriod(userId, startDate, endDate)).thenReturn(0L);

        DashboardSummaryResponse result = dashboardService.getSummary(userId, startDate, endDate);

        assertThat(result.startDate()).isEqualTo(startDate);
        assertThat(result.endDate()).isEqualTo(endDate);
    }

    @Test
    @DisplayName("summary : agrégations null converties en zéro")
    void getSummary_NullAggregates_ShouldReturnZeroAmounts() {
        UUID userId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(transactionRepository.sumAmountByUserAndTypeAndPeriod(
                userId,
                TransactionType.INCOME,
                startDate,
                endDate
        )).thenReturn(null);
        when(transactionRepository.sumAmountByUserAndTypeAndPeriod(
                userId,
                TransactionType.EXPENSE,
                startDate,
                endDate
        )).thenReturn(null);
        when(transactionRepository.countByUserIdAndPeriod(userId, startDate, endDate)).thenReturn(0L);

        DashboardSummaryResponse result = dashboardService.getSummary(userId, startDate, endDate);

        assertThat(result.incomeTotal()).isEqualByComparingTo("0.00");
        assertThat(result.expenseTotal()).isEqualByComparingTo("0.00");
        assertThat(result.balance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("categoryExpenses : mappe les dépenses par catégorie")
    void getCategoryExpenses_ShouldReturnMappedResponses() {
        UUID userId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(transactionRepository.findExpenseTotalsByCategory(userId, startDate, endDate))
                .thenReturn(List.of(categorySummary(10L, "Transport", "#654321", new BigDecimal("125.50"), 2L)));

        List<CategoryExpenseResponse> result = dashboardService.getCategoryExpenses(userId, startDate, endDate);

        assertThat(result).singleElement()
                .satisfies(item -> {
                    assertThat(item.categoryId()).isEqualTo(10L);
                    assertThat(item.categoryName()).isEqualTo("Transport");
                    assertThat(item.categoryColor()).isEqualTo("#654321");
                    assertThat(item.totalAmount()).isEqualByComparingTo("125.50");
                    assertThat(item.transactionCount()).isEqualTo(2L);
                });
    }

    @Test
    @DisplayName("categoryExpenses : montant et count null convertis en zéro")
    void getCategoryExpenses_NullAggregates_ShouldReturnZero() {
        UUID userId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(transactionRepository.findExpenseTotalsByCategory(userId, startDate, endDate))
                .thenReturn(List.of(categorySummary(10L, "Transport", "#654321", null, null)));

        CategoryExpenseResponse result = dashboardService.getCategoryExpenses(userId, startDate, endDate).getFirst();

        assertThat(result.totalAmount()).isEqualByComparingTo("0.00");
        assertThat(result.transactionCount()).isZero();
    }

    @Test
    @DisplayName("monthlyBalances : mappe les balances mensuelles")
    void getMonthlyBalances_ShouldReturnMappedResponses() {
        UUID userId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 28);

        when(transactionRepository.findMonthlyBalance(userId, startDate, endDate))
                .thenReturn(List.of(monthlySummary(
                        2026,
                        1,
                        new BigDecimal("1000.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("700.00")
                )));

        List<MonthlyBalanceResponse> result = dashboardService.getMonthlyBalances(userId, startDate, endDate);

        assertThat(result).singleElement()
                .satisfies(item -> {
                    assertThat(item.year()).isEqualTo(2026);
                    assertThat(item.month()).isEqualTo(1);
                    assertThat(item.incomeTotal()).isEqualByComparingTo("1000.00");
                    assertThat(item.expenseTotal()).isEqualByComparingTo("300.00");
                    assertThat(item.balance()).isEqualByComparingTo("700.00");
                });
    }

    @Test
    @DisplayName("monthlyBalances : vérifie que le userId transmis au repository vient du service")
    void getMonthlyBalances_ShouldPassUserIdToRepository() {
        UUID userId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(transactionRepository.findMonthlyBalance(userId, startDate, endDate)).thenReturn(List.of());

        dashboardService.getMonthlyBalances(userId, startDate, endDate);

        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(transactionRepository).findMonthlyBalance(userIdCaptor.capture(), org.mockito.ArgumentMatchers.eq(startDate),
                org.mockito.ArgumentMatchers.eq(endDate));
        assertThat(userIdCaptor.getValue()).isEqualTo(userId);
    }

    private CategoryExpenseSummary categorySummary(
            Long categoryId,
            String categoryName,
            String categoryColor,
            BigDecimal totalAmount,
            Long transactionCount
    ) {
        return new CategoryExpenseSummary() {
            @Override
            public Long getCategoryId() {
                return categoryId;
            }

            @Override
            public String getCategoryName() {
                return categoryName;
            }

            @Override
            public String getCategoryColor() {
                return categoryColor;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return totalAmount;
            }

            @Override
            public Long getTransactionCount() {
                return transactionCount;
            }
        };
    }

    private MonthlyBalanceSummary monthlySummary(
            Integer year,
            Integer month,
            BigDecimal incomeTotal,
            BigDecimal expenseTotal,
            BigDecimal balance
    ) {
        return new MonthlyBalanceSummary() {
            @Override
            public Integer getYear() {
                return year;
            }

            @Override
            public Integer getMonth() {
                return month;
            }

            @Override
            public BigDecimal getIncomeTotal() {
                return incomeTotal;
            }

            @Override
            public BigDecimal getExpenseTotal() {
                return expenseTotal;
            }

            @Override
            public BigDecimal getBalance() {
                return balance;
            }
        };
    }
}
