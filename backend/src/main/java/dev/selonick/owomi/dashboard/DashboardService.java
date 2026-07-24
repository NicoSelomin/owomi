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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Statistiques financières de l'utilisateur authentifié.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int MAX_CALENDAR_MONTHS = 24;
    private static final int MONEY_SCALE = 2;
    private static final String PARTIAL_PERIOD_MESSAGE =
            "La date de début et la date de fin doivent être fournies ensemble.";
    private static final String INVERTED_PERIOD_MESSAGE =
            "La date de début doit être antérieure ou égale à la date de fin.";
    private static final String PERIOD_TOO_LONG_MESSAGE =
            "La période ne peut pas dépasser 24 mois calendaires.";

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(UUID userId, LocalDate startDate, LocalDate endDate) {
        Period period = resolvePeriod(startDate, endDate);
        BigDecimal incomeTotal = money(transactionRepository.sumAmountByUserAndTypeAndPeriod(
                userId,
                TransactionType.INCOME,
                period.startDate(),
                period.endDate()
        ));
        BigDecimal expenseTotal = money(transactionRepository.sumAmountByUserAndTypeAndPeriod(
                userId,
                TransactionType.EXPENSE,
                period.startDate(),
                period.endDate()
        ));
        long transactionCount = transactionRepository.countByUserIdAndPeriod(
                userId,
                period.startDate(),
                period.endDate()
        );

        return new DashboardSummaryResponse(
                incomeTotal,
                expenseTotal,
                money(incomeTotal.subtract(expenseTotal)),
                transactionCount,
                period.startDate(),
                period.endDate()
        );
    }

    @Transactional(readOnly = true)
    public List<MonthlyBalanceResponse> getMonthlyBalances(UUID userId, LocalDate startDate, LocalDate endDate) {
        Period period = resolvePeriod(startDate, endDate);
        return transactionRepository.findMonthlyBalance(userId, period.startDate(), period.endDate())
                .stream()
                .map(this::toMonthlyBalanceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryExpenseResponse> getCategoryExpenses(UUID userId, LocalDate startDate, LocalDate endDate) {
        Period period = resolvePeriod(startDate, endDate);
        return transactionRepository.findExpenseTotalsByCategory(userId, period.startDate(), period.endDate())
                .stream()
                .map(this::toCategoryExpenseResponse)
                .toList();
    }

    private Period resolvePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            YearMonth currentMonth = YearMonth.now();
            return new Period(currentMonth.atDay(1), currentMonth.atEndOfMonth());
        }
        if (startDate == null || endDate == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, PARTIAL_PERIOD_MESSAGE);
        }
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, INVERTED_PERIOD_MESSAGE);
        }

        long calendarMonths = ChronoUnit.MONTHS.between(YearMonth.from(startDate), YearMonth.from(endDate)) + 1;
        if (calendarMonths > MAX_CALENDAR_MONTHS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, PERIOD_TOO_LONG_MESSAGE);
        }
        return new Period(startDate, endDate);
    }

    private MonthlyBalanceResponse toMonthlyBalanceResponse(MonthlyBalanceSummary summary) {
        return new MonthlyBalanceResponse(
                summary.getYear(),
                summary.getMonth(),
                money(summary.getIncomeTotal()),
                money(summary.getExpenseTotal()),
                money(summary.getBalance())
        );
    }

    private CategoryExpenseResponse toCategoryExpenseResponse(CategoryExpenseSummary summary) {
        return new CategoryExpenseResponse(
                summary.getCategoryId(),
                summary.getCategoryName(),
                summary.getCategoryColor(),
                money(summary.getTotalAmount()),
                summary.getTransactionCount() == null ? 0L : summary.getTransactionCount()
        );
    }

    private BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private record Period(LocalDate startDate, LocalDate endDate) {
    }
}
