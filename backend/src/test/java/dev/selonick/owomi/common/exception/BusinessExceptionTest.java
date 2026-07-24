package dev.selonick.owomi.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    @DisplayName("constructeur par ErrorCode : expose le message par défaut et le statut HTTP")
    void constructor_WithErrorCode_ShouldExposeDefaultMessageAndHttpStatus() {
        BusinessException exception = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("Ressource introuvable.");
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("constructeur avec message : expose le message personnalisé")
    void constructor_WithCustomMessage_ShouldExposeCustomMessage() {
        BusinessException exception = new BusinessException(
                ErrorCode.ACCESS_DENIED,
                "Accès refusé."
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(exception.getMessage()).isEqualTo("Accès refusé.");
        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
