package dev.selonick.owomi.transaction.export;

/**
 * Fichier CSV prêt à être retourné par l'API.
 */
public record TransactionExportCsv(
        String filename,
        byte[] content
) {
}
