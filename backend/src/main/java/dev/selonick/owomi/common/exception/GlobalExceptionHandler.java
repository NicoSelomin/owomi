package dev.selonick.owomi.common.exception;

import dev.selonick.owomi.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Gestion centralisée des exceptions : transforme toute erreur en réponse ApiResponse uniforme.
 * Les messages utilisateurs sont en français ; les détails techniques restent dans les logs.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Erreurs métier personnalisées. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getErrorCode().name(), ex.getMessage()));
    }

    /** Erreurs de validation @Valid sur les @RequestBody. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.name(), "Données invalides.", details));
    }

    /** Erreurs de validation sur les paramètres (@RequestParam annotés, @Validated sur le contrôleur). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(jakarta.validation.ConstraintViolation::getMessage)
                .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.name(), "Données invalides.", details));
    }

    /** Paramètre de requête obligatoire manquant. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.name(),
                        "Paramètre obligatoire manquant : " + ex.getParameterName()));
    }

    /** Erreurs de conversion des paramètres (ex : enum invalide dans une query string). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = "Paramètre invalide : " + ex.getName();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.name(), "Données invalides.", List.of(detail)));
    }

    /** Content-Type absent ou non supporté : réponse uniforme, sans détail d'implémentation. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type");
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(
                        ErrorCode.UNSUPPORTED_MEDIA_TYPE.name(),
                        ErrorCode.UNSUPPORTED_MEDIA_TYPE.getDefaultMessage()
                ));
    }

    /** Corps JSON illisible ou malformé : ne jamais exposer le message Jackson au client. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Invalid request body");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.name(),
                        "Le corps de la requête est invalide."));
    }

    /** Accès refusé par Spring Security (403). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.ACCESS_DENIED.name(), ErrorCode.ACCESS_DENIED.getDefaultMessage()));
    }

    /** Échec d'authentification Spring Security (401). */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.TOKEN_INVALID.name(), ErrorCode.TOKEN_INVALID.getDefaultMessage()));
    }

    /** Erreur générique — ne jamais exposer le détail technique à l'utilisateur. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
    }
}
