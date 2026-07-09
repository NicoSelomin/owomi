package dev.selonick.owomi.common.exception;

import dev.selonick.owomi.common.api.ApiResponse;
import dev.selonick.owomi.common.enums.TransactionType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("BusinessException : retourne le statut et le code métier")
    void handleBusinessException_ShouldReturnBusinessError() {
        BusinessException exception = new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "Ressource introuvable."
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().error().message()).isEqualTo("Ressource introuvable.");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException : retourne VALIDATION_ERROR avec détails")
    void handleValidation_ShouldReturnValidationDetails() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("create", TestRequest.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                new TestRequest(),
                "request"
        );
        bindingResult.addError(new FieldError(
                "request",
                "name",
                "Le nom est obligatoire."
        ));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                parameter,
                bindingResult
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().error().details()).containsExactly("Le nom est obligatoire.");
    }

    @Test
    @DisplayName("ConstraintViolationException : retourne VALIDATION_ERROR avec détails")
    void handleConstraintViolation_ShouldReturnValidationDetails() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<InvalidRequest>> violations = validator.validate(new InvalidRequest());
        ConstraintViolationException exception = new ConstraintViolationException(violations);

        ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().error().details())
                .containsExactly("Le nom est obligatoire.");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException : retourne VALIDATION_ERROR")
    void handleTypeMismatch_ShouldReturnValidationError() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("list", TransactionType.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "BAD",
                TransactionType.class,
                "type",
                parameter,
                new IllegalArgumentException("No enum constant")
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleTypeMismatch(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().error().message()).isEqualTo("Données invalides.");
        assertThat(response.getBody().error().details()).containsExactly("Paramètre invalide : type");
    }

    @Test
    @DisplayName("AccessDeniedException : retourne ACCESS_DENIED")
    void handleAccessDenied_ShouldReturnForbidden() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(
                new AccessDeniedException("Forbidden")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    @DisplayName("AuthenticationException : retourne TOKEN_INVALID")
    void handleAuthentication_ShouldReturnUnauthorized() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleAuthentication(
                new BadCredentialsException("Bad credentials")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("TOKEN_INVALID");
    }

    @Test
    @DisplayName("Exception : retourne INTERNAL_ERROR sans exposer le détail technique")
    void handleGeneric_ShouldReturnInternalError() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(
                new RuntimeException("Database stack trace")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().error().message())
                .isEqualTo("Une erreur inattendue est survenue. Réessayez.");
    }

    private static final class TestController {
        @SuppressWarnings("unused")
        private void create(TestRequest request) {
        }

        @SuppressWarnings("unused")
        private void list(TransactionType type) {
        }
    }

    private static final class TestRequest {
    }

    private static final class InvalidRequest {

        @NotBlank(message = "Le nom est obligatoire.")
        private final String name = "";
    }
}
