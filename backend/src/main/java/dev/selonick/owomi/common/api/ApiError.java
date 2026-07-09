package dev.selonick.owomi.common.api;

import java.util.List;

/**
 * Détail d'une erreur API : code métier, message utilisateur et détails éventuels.
 */
public record ApiError(
        String code,
        String message,
        List<String> details
) {
}
