package dev.selonick.owomi.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class JwtSecurityHandlersTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("JwtAuthenticationEntryPoint : retourne une erreur JSON 401 uniforme")
    void authenticationEntryPoint_ShouldReturnUniformUnauthorizedJson() throws Exception {
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("Bad credentials")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").get("code").asText()).isEqualTo("TOKEN_INVALID");
    }

    @Test
    @DisplayName("JwtAccessDeniedHandler : retourne une erreur JSON 403 uniforme")
    void accessDeniedHandler_ShouldReturnUniformForbiddenJson() throws Exception {
        JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(objectMapper);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("Forbidden")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").get("code").asText()).isEqualTo("ACCESS_DENIED");
    }
}
