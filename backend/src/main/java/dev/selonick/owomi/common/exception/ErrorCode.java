package dev.selonick.owomi.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Codes d'erreur métier de l'application, chacun associé à un statut HTTP.
 * Cf. CLAUDE.md global, section 6.2.
 */
@Getter
public enum ErrorCode {

    INVALID_CREDENTIALS("Email ou mot de passe incorrect.", HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_EXISTS("Email déjà utilisé.", HttpStatus.CONFLICT),
    TOKEN_EXPIRED("Session expirée. Veuillez vous reconnecter.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("Jeton d'authentification invalide.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("Accès refusé.", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("Ressource introuvable.", HttpStatus.NOT_FOUND),
    VALIDATION_ERROR("Données invalides.", HttpStatus.BAD_REQUEST),
    AMOUNT_INVALID("Le montant doit être supérieur à 0.", HttpStatus.BAD_REQUEST),
    FUTURE_DATE("La date ne peut pas être dans le futur.", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_TRANSACTIONS("Cette catégorie est liée à des transactions.", HttpStatus.CONFLICT),
    CATEGORY_IS_DEFAULT("Cette catégorie par défaut ne peut pas être modifiée.", HttpStatus.CONFLICT),
    RATE_LIMIT_EXCEEDED("Trop de requêtes. Réessayez dans quelques instants.", HttpStatus.TOO_MANY_REQUESTS),
    EMAIL_NOT_VERIFIED("Votre adresse email n'est pas encore vérifiée.", HttpStatus.FORBIDDEN),
    VERIFICATION_TOKEN_INVALID("Lien de vérification invalide.", HttpStatus.BAD_REQUEST),
    VERIFICATION_TOKEN_EXPIRED("Lien de vérification expiré. Veuillez en demander un nouveau.", HttpStatus.BAD_REQUEST),
    RESET_TOKEN_INVALID("Lien de réinitialisation invalide.", HttpStatus.BAD_REQUEST),
    RESET_TOKEN_EXPIRED("Lien de réinitialisation expiré. Veuillez en demander un nouveau.", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("Une erreur inattendue est survenue. Réessayez.", HttpStatus.INTERNAL_SERVER_ERROR);

    /** Message par défaut affiché à l'utilisateur (français). */
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String defaultMessage, HttpStatus httpStatus) {
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
}
