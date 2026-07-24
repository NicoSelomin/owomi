package dev.selonick.owomi.transaction.export;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Génère le CSV des transactions avec échappement et neutralisation CSV/Excel injection.
 */
@Component
public class TransactionCsvExporter {

    private static final String[] HEADERS = {
            "Date",
            "Type",
            "Catégorie",
            "Montant",
            "Devise",
            "Note",
            "Créée le",
            "Modifiée le"
    };

    public byte[] export(List<TransactionExportRow> rows) {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, List.of(HEADERS));
        rows.forEach(row -> appendRow(csv, List.of(
                formatDate(row.date()),
                formatText(row.type() == null ? null : row.type().name()),
                formatText(row.categoryName()),
                formatAmount(row.amount()),
                formatText(row.currencyCode()),
                formatText(row.note()),
                formatDateTime(row.createdAt()),
                formatDateTime(row.updatedAt())
        )));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendRow(StringBuilder csv, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(values.get(i)));
        }
        csv.append('\n');
    }

    private String escape(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains("\"") || safe.contains(",") || safe.contains("\n") || safe.contains("\r")) {
            return '"' + safe.replace("\"", "\"\"") + '"';
        }
        return safe;
    }

    private String formatText(String value) {
        if (value == null) {
            return "";
        }
        return isFormulaLike(value) ? "'" + value : value;
    }

    private boolean isFormulaLike(String value) {
        int index = 0;
        while (index < value.length() && value.charAt(index) == ' ') {
            index++;
        }
        if (index >= value.length()) {
            return false;
        }
        char first = value.charAt(index);
        return first == '=' || first == '+' || first == '-' || first == '@'
                || first == '\t' || first == '\r';
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "" : amount.toPlainString();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.toString();
    }
}
