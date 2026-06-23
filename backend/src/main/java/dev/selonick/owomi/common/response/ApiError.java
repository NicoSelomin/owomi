package dev.selonick.owomi.common.response;

import java.util.List;

/**
 * Détail d'une erreur API : code métier, message utilisateur (français) et détails éventuels.
 */
public record ApiError(
        String code,
        String message,
        List<String> details
) {
}
