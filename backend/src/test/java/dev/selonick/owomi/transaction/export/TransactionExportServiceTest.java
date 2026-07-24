package dev.selonick.owomi.transaction.export;

import dev.selonick.owomi.category.Category;
import dev.selonick.owomi.category.CategoryRepository;
import dev.selonick.owomi.common.enums.TransactionType;
import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.transaction.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionExportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionCsvExporter csvExporter;

    @InjectMocks
    private TransactionExportService exportService;

    @Test
    @DisplayName("exportCsv : période par défaut = mois courant")
    void exportCsv_DefaultPeriod_ShouldUseCurrentMonth() {
        UUID userId = UUID.randomUUID();
        YearMonth currentMonth = YearMonth.now();
        when(transactionRepository.findForExportByUserAndFilters(
                eq(userId),
                eq(null),
                eq(null),
                eq(currentMonth.atDay(1)),
                eq(currentMonth.atEndOfMonth()),
                any(Pageable.class)
        )).thenReturn(List.of(row("note")));
        when(csvExporter.export(any())).thenReturn("csv".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        TransactionExportCsv result = exportService.exportCsv(userId, null, null, null, null);

        assertThat(result.filename())
                .isEqualTo("owomi-transactions-" + currentMonth.atDay(1) + "-" + currentMonth.atEndOfMonth() + ".csv");
    }

    @Test
    @DisplayName("exportCsv : période explicite, type et catégorie sont transmis au repository")
    void exportCsv_WithFilters_ShouldDelegateParameterizedFiltersAndUserId() {
        UUID userId = UUID.randomUUID();
        Category category = new Category();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        when(categoryRepository.findAccessibleById(10L, userId)).thenReturn(Optional.of(category));
        when(transactionRepository.findForExportByUserAndFilters(
                eq(userId),
                eq(TransactionType.EXPENSE),
                eq(10L),
                eq(startDate),
                eq(endDate),
                any(Pageable.class)
        )).thenReturn(List.of(row("note")));
        when(csvExporter.export(any())).thenReturn("csv".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        exportService.exportCsv(userId, TransactionType.EXPENSE, 10L, startDate, endDate);

        verify(categoryRepository).findAccessibleById(10L, userId);
        verify(transactionRepository).findForExportByUserAndFilters(
                eq(userId),
                eq(TransactionType.EXPENSE),
                eq(10L),
                eq(startDate),
                eq(endDate),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("exportCsv : catégorie inexistante ou non autorisée → RESOURCE_NOT_FOUND")
    void exportCsv_InaccessibleCategory_ShouldThrowNotFound() {
        UUID userId = UUID.randomUUID();
        when(categoryRepository.findAccessibleById(10L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exportService.exportCsv(
                userId,
                null,
                10L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        verify(transactionRepository, never()).findForExportByUserAndFilters(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("exportCsv : une seule date fournie → VALIDATION_ERROR")
    void exportCsv_PartialPeriod_ShouldThrowValidationError() {
        assertValidationError(null, LocalDate.of(2026, 1, 31));
        assertValidationError(LocalDate.of(2026, 1, 1), null);
    }

    @Test
    @DisplayName("exportCsv : période inversée → VALIDATION_ERROR")
    void exportCsv_InvertedPeriod_ShouldThrowValidationError() {
        assertValidationError(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 31));
    }

    @Test
    @DisplayName("exportCsv : période supérieure à 24 mois → VALIDATION_ERROR")
    void exportCsv_TooLongPeriod_ShouldThrowValidationError() {
        assertValidationError(LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 1));
    }

    @Test
    @DisplayName("exportCsv : période exactement 24 mois acceptée")
    void exportCsv_ExactlyTwentyFourMonths_ShouldBeAccepted() {
        UUID userId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        when(transactionRepository.findForExportByUserAndFilters(
                eq(userId),
                eq(null),
                eq(null),
                eq(startDate),
                eq(endDate),
                any(Pageable.class)
        )).thenReturn(List.of());
        when(csvExporter.export(List.of())).thenReturn("header\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        TransactionExportCsv result = exportService.exportCsv(userId, null, null, startDate, endDate);

        assertThat(result.content()).isNotEmpty();
    }

    @Test
    @DisplayName("exportCsv : dépassement de limite → EXPORT_LIMIT_EXCEEDED")
    void exportCsv_LimitExceeded_ShouldThrowBusinessError() {
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(exportService, "maxRows", 1);
        when(transactionRepository.findForExportByUserAndFilters(
                eq(userId),
                eq(null),
                eq(null),
                eq(LocalDate.of(2026, 1, 1)),
                eq(LocalDate.of(2026, 1, 31)),
                any(Pageable.class)
        )).thenReturn(List.of(row("1"), row("2")));

        assertThatThrownBy(() -> exportService.exportCsv(
                userId,
                null,
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXPORT_LIMIT_EXCEEDED);

        verify(csvExporter, never()).export(any());
    }

    @Test
    @DisplayName("exportCsv : utilise une limite interne maxRows + 1")
    void exportCsv_ShouldUseInternalLimitPlusOne() {
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(exportService, "maxRows", 50);
        when(transactionRepository.findForExportByUserAndFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(csvExporter.export(List.of())).thenReturn("header\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        exportService.exportCsv(userId, null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findForExportByUserAndFilters(
                eq(userId),
                eq(null),
                eq(null),
                eq(LocalDate.of(2026, 1, 1)),
                eq(LocalDate.of(2026, 1, 31)),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(51);
    }

    @Test
    @DisplayName("exportCsv : borne maxRows pour éviter tout dépassement numérique")
    void exportCsv_MaxRowsTooHigh_ShouldUseSafeUpperBound() {
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(exportService, "maxRows", Integer.MAX_VALUE);
        when(transactionRepository.findForExportByUserAndFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(csvExporter.export(List.of())).thenReturn("header\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        exportService.exportCsv(userId, null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findForExportByUserAndFilters(
                eq(userId),
                eq(null),
                eq(null),
                eq(LocalDate.of(2026, 1, 1)),
                eq(LocalDate.of(2026, 1, 31)),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100_001);
    }

    private void assertValidationError(LocalDate startDate, LocalDate endDate) {
        assertThatThrownBy(() -> exportService.exportCsv(UUID.randomUUID(), null, null, startDate, endDate))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    private TransactionExportRow row(String note) {
        return new TransactionExportRow(
                LocalDate.of(2026, 1, 10),
                TransactionType.EXPENSE,
                "Catégorie",
                BigDecimal.ONE,
                "XOF",
                note,
                null,
                null
        );
    }
}
