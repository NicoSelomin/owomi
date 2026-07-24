package dev.selonick.owomi.transaction.export;

import dev.selonick.owomi.category.CategoryRepository;
import dev.selonick.owomi.common.enums.TransactionType;
import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Prépare l'export CSV des transactions de l'utilisateur authentifié.
 */
@Service
@RequiredArgsConstructor
public class TransactionExportService {

    private static final int MAX_CALENDAR_MONTHS = 24;
    private static final int MAX_SAFE_EXPORT_ROWS = 100_000;
    private static final String PARTIAL_PERIOD_MESSAGE =
            "La date de début et la date de fin doivent être fournies ensemble.";
    private static final String INVERTED_PERIOD_MESSAGE =
            "La date de début doit être antérieure ou égale à la date de fin.";
    private static final String PERIOD_TOO_LONG_MESSAGE =
            "La période ne peut pas dépasser 24 mois calendaires.";
    private static final String LIMIT_EXCEEDED_MESSAGE =
            "L'export dépasse la limite autorisée. Veuillez réduire la période ou les filtres.";

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionCsvExporter csvExporter;

    @Value("${owomi.export.transactions.max-rows:10000}")
    private int maxRows;

    @Transactional(readOnly = true)
    public TransactionExportCsv exportCsv(
            UUID userId,
            TransactionType type,
            Long categoryId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Period period = resolvePeriod(startDate, endDate);
        if (categoryId != null) {
            validateAccessibleCategory(categoryId, userId);
        }

        int effectiveMaxRows = Math.min(Math.max(1, maxRows), MAX_SAFE_EXPORT_ROWS);
        List<TransactionExportRow> rows = transactionRepository.findForExportByUserAndFilters(
                userId,
                type,
                categoryId,
                period.startDate(),
                period.endDate(),
                PageRequest.of(0, effectiveMaxRows + 1)
        );
        if (rows.size() > effectiveMaxRows) {
            throw new BusinessException(ErrorCode.EXPORT_LIMIT_EXCEEDED, LIMIT_EXCEEDED_MESSAGE);
        }

        return new TransactionExportCsv(filename(period), csvExporter.export(rows));
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

    private void validateAccessibleCategory(Long categoryId, UUID userId) {
        if (categoryRepository.findAccessibleById(categoryId, userId).isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private String filename(Period period) {
        return "owomi-transactions-" + period.startDate() + "-" + period.endDate() + ".csv";
    }

    private record Period(LocalDate startDate, LocalDate endDate) {
    }
}
