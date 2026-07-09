package dev.selonick.owomi.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ApiResponseTest {

    @Test
    @DisplayName("success : construit une réponse uniforme avec data, message et timestamp")
    void success_ShouldBuildApiResponse() {
        ApiResponse<String> response = ApiResponse.success("ok", "Opération réussie.");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("ok");
        assertThat(response.message()).isEqualTo("Opération réussie.");
        assertThat(response.error()).isNull();
        assertThat(response.timestamp()).isNotBlank();
        assertThatCode(() -> Instant.parse(response.timestamp())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("error : construit une réponse d'erreur sans détails explicites")
    void error_WithoutDetails_ShouldBuildApiResponse() {
        ApiResponse<Void> response = ApiResponse.error("VALIDATION_ERROR", "Données invalides.");

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.message()).isNull();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.error().message()).isEqualTo("Données invalides.");
        assertThat(response.error().details()).isEmpty();
        assertThat(response.timestamp()).isNotBlank();
    }

    @Test
    @DisplayName("error : conserve les détails de validation transmis")
    void error_WithDetails_ShouldKeepDetails() {
        List<String> details = List.of("Le montant est obligatoire.");

        ApiResponse<Void> response = ApiResponse.error(
                "VALIDATION_ERROR",
                "Données invalides.",
                details
        );

        assertThat(response.success()).isFalse();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().details()).containsExactly("Le montant est obligatoire.");
    }
}
