package dev.selonick.owomi.transaction.export;

import dev.selonick.owomi.common.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionCsvExporterTest {

    private final TransactionCsvExporter exporter = new TransactionCsvExporter();

    @Test
    @DisplayName("export : génère un CSV UTF-8 avec en-têtes stables")
    void export_ShouldGenerateUtf8CsvWithStableHeaders() {
        String csv = csv(new TransactionExportRow(
                LocalDate.of(2026, 1, 10),
                TransactionType.EXPENSE,
                "Épicerie",
                new BigDecimal("12.30"),
                "XOF",
                "note",
                LocalDateTime.of(2026, 1, 10, 8, 30),
                LocalDateTime.of(2026, 1, 11, 9, 45)
        ));

        assertThat(csv).startsWith("Date,Type,Catégorie,Montant,Devise,Note,Créée le,Modifiée le\n");
        assertThat(csv).contains("2026-01-10,EXPENSE,Épicerie,12.30,XOF,note,2026-01-10T08:30,2026-01-11T09:45");
    }

    @Test
    @DisplayName("export : conserve la précision BigDecimal et gère les valeurs null")
    void export_ShouldKeepBigDecimalPrecisionAndHandleNulls() {
        String csv = csv(new TransactionExportRow(
                null,
                null,
                null,
                new BigDecimal("1234567890.1200"),
                null,
                null,
                null,
                null
        ));

        assertThat(csv).contains("\n,,,1234567890.1200,,,,\n");
    }

    @Test
    @DisplayName("export : échappe séparateur, guillemets et retours à la ligne")
    void export_ShouldEscapeCsvSpecialCharacters() {
        String csv = csv(new TransactionExportRow(
                LocalDate.of(2026, 1, 10),
                TransactionType.EXPENSE,
                "Maison, travaux",
                new BigDecimal("10.00"),
                "XOF",
                "Texte \"cité\"\nligne 2",
                null,
                null
        ));

        assertThat(csv).contains("\"Maison, travaux\"");
        assertThat(csv).contains("\"Texte \"\"cité\"\"\nligne 2\"");
    }

    @Test
    @DisplayName("export : neutralise les valeurs texte compatibles CSV injection")
    void export_ShouldNeutralizeCsvInjectionValues() {
        String csv = csv(
                row("=HYPERLINK(\"http://example.test\")"),
                row("+cmd"),
                row("-1+1"),
                row("@SUM(A1:A2)"),
                row("   =SUM(A1:A2)"),
                row("\t=SUM(A1:A2)")
        );

        assertThat(csv).contains("'=HYPERLINK");
        assertThat(csv).contains("'+cmd");
        assertThat(csv).contains("'-1+1");
        assertThat(csv).contains("'@SUM");
        assertThat(csv).contains("'   =SUM");
        assertThat(csv).contains("'\t=SUM");
    }

    private String csv(TransactionExportRow... rows) {
        return new String(exporter.export(List.of(rows)), StandardCharsets.UTF_8);
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
