package dev.selonick.owomi.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * Format de réponse uniforme pour toute l'API OWOMI.
 * Les champs nuls sont omis de la sérialisation JSON.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        ApiError error,
        String timestamp
) {

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(Instant.now().toString())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(new ApiError(code, message, List.of()))
                .timestamp(Instant.now().toString())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, List<String> details) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(new ApiError(code, message, details))
                .timestamp(Instant.now().toString())
                .build();
    }
}
