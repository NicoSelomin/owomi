package dev.selonick.owomi.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception métier portant un ErrorCode et un message utilisateur (français).
 * Interceptée par le GlobalExceptionHandler pour produire une réponse uniforme.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return errorCode.getHttpStatus();
    }
}
