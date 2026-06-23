package dev.selonick.owomi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Renvoie une réponse JSON uniforme (401) lorsqu'une ressource protégée est appelée sans
 * authentification valide, plutôt que la page d'erreur par défaut.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> body = ApiResponse.error(
                ErrorCode.TOKEN_INVALID.name(),
                "Authentification requise.");

        objectMapper.writeValue(response.getWriter(), body);
    }
}
