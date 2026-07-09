package dev.selonick.owomi.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.selonick.owomi.auth.dto.AuthResponse;
import dev.selonick.owomi.auth.dto.ForgotPasswordRequest;
import dev.selonick.owomi.auth.dto.LoginRequest;
import dev.selonick.owomi.auth.dto.RegisterRequest;
import dev.selonick.owomi.auth.dto.ResendVerificationRequest;
import dev.selonick.owomi.auth.dto.ResetPasswordRequest;
import dev.selonick.owomi.auth.dto.TokenRequest;
import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.common.exception.GlobalExceptionHandler;
import dev.selonick.owomi.user.dto.UserDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    @DisplayName("POST /api/auth/register : retourne 201 et ApiResponse<AuthResponse>")
    void register_ValidRequest_ShouldReturnCreatedApiResponse() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Nicodème",
                "test@owomi.dev",
                "Password1!",
                "BJ",
                "XOF"
        );
        when(authService.register(request)).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.user.email").value("test@owomi.dev"))
                .andExpect(jsonPath("$.message").value("Compte créé avec succès."))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/auth/login : retourne 200 et ApiResponse<AuthResponse>")
    void login_ValidRequest_ShouldReturnOkApiResponse() throws Exception {
        LoginRequest request = new LoginRequest("test@owomi.dev", "Password1!");
        when(authService.login(request)).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.message").value("Connexion réussie."));
    }

    @Test
    @DisplayName("POST /api/auth/refresh : retourne un nouvel access token")
    void refresh_ValidRequest_ShouldReturnOkApiResponse() throws Exception {
        TokenRequest request = new TokenRequest("refresh-token");
        when(authService.refresh("refresh-token")).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.message").value("Token rafraîchi."));
    }

    @Test
    @DisplayName("POST /api/auth/logout : retourne une ApiResponse de succès")
    void logout_ValidRequest_ShouldReturnOkApiResponse() throws Exception {
        TokenRequest request = new TokenRequest("refresh-token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Déconnexion réussie."));

        verify(authService).logout("refresh-token");
    }

    @Test
    @DisplayName("POST /api/auth/register : requête invalide → 400 VALIDATION_ERROR")
    void register_InvalidRequest_ShouldReturnValidationError() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "",
                "email-invalide",
                "weak",
                "BEN",
                "XO"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Données invalides."))
                .andExpect(jsonPath("$.error.details").isArray())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/auth/refresh : refreshToken manquant → 400 VALIDATION_ERROR")
    void refresh_MissingToken_ShouldReturnValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0]").value("Le refresh token est obligatoire."));
    }

    @Test
    @DisplayName("POST /api/auth/login : erreur métier → ApiResponse d'erreur conforme")
    void login_InvalidCredentials_ShouldReturnBusinessError() throws Exception {
        LoginRequest request = new LoginRequest("test@owomi.dev", "WrongPassword");
        when(authService.login(request)).thenThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").value("Email ou mot de passe incorrect."))
                .andExpect(jsonPath("$.error.details").isArray())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/auth/verify-email : token valide → succès")
    void verifyEmail_ValidToken_ShouldReturnOkApiResponse() throws Exception {
        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", "verification-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Adresse email vérifiée avec succès."))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        verify(authService).verifyEmail("verification-token");
    }

    @Test
    @DisplayName("GET /api/auth/verify-email : token invalide → erreur métier")
    void verifyEmail_InvalidToken_ShouldReturnBusinessError() throws Exception {
        doThrow(new BusinessException(ErrorCode.VERIFICATION_TOKEN_INVALID))
                .when(authService).verifyEmail("invalid-token");

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VERIFICATION_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("GET /api/auth/verify-email : token expiré → erreur métier")
    void verifyEmail_ExpiredToken_ShouldReturnBusinessError() throws Exception {
        doThrow(new BusinessException(ErrorCode.VERIFICATION_TOKEN_EXPIRED))
                .when(authService).verifyEmail("expired-token");

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", "expired-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VERIFICATION_TOKEN_EXPIRED"));
    }

    @Test
    @DisplayName("GET /api/auth/verify-email : token manquant → 400 VALIDATION_ERROR")
    void verifyEmail_MissingToken_ShouldReturnValidationError() throws Exception {
        mockMvc.perform(get("/api/auth/verify-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/auth/resend-verification : requête valide → réponse générique")
    void resendVerification_ValidRequest_ShouldReturnGenericSuccess() throws Exception {
        ResendVerificationRequest request = new ResendVerificationRequest("test@owomi.dev");

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Si un compte non vérifié est associé à cet email, un nouveau lien a été envoyé."));

        verify(authService).resendVerification("test@owomi.dev");
    }

    @Test
    @DisplayName("POST /api/auth/resend-verification : email invalide → 400 VALIDATION_ERROR")
    void resendVerification_InvalidEmail_ShouldReturnValidationError() throws Exception {
        ResendVerificationRequest request = new ResendVerificationRequest("email-invalide");

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password : requête valide → réponse générique")
    void forgotPassword_ValidRequest_ShouldReturnGenericSuccess() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@owomi.dev");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Si un compte est associé à cet email, un lien de réinitialisation a été envoyé."));

        verify(authService).forgotPassword("test@owomi.dev");
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password : email invalide → 400 VALIDATION_ERROR")
    void forgotPassword_InvalidEmail_ShouldReturnValidationError() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("email-invalide");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password : requête valide → succès")
    void resetPassword_ValidRequest_ShouldReturnSuccess() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "NewPassword1!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Mot de passe réinitialisé avec succès. Vous pouvez vous connecter."));

        verify(authService).resetPassword("reset-token", "NewPassword1!");
    }

    @Test
    @DisplayName("POST /api/auth/reset-password : mot de passe faible → 400 VALIDATION_ERROR")
    void resetPassword_WeakPassword_ShouldReturnValidationError() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "weak");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password : token invalide → erreur métier")
    void resetPassword_InvalidToken_ShouldReturnBusinessError() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("invalid-token", "NewPassword1!");
        doThrow(new BusinessException(ErrorCode.RESET_TOKEN_INVALID))
                .when(authService).resetPassword("invalid-token", "NewPassword1!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESET_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password : token expiré → erreur métier")
    void resetPassword_ExpiredToken_ShouldReturnBusinessError() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "NewPassword1!");
        doThrow(new BusinessException(ErrorCode.RESET_TOKEN_EXPIRED))
                .when(authService).resetPassword("expired-token", "NewPassword1!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESET_TOKEN_EXPIRED"));
    }

    private AuthResponse authResponse() {
        UserDTO user = new UserDTO(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Nicodème",
                "test@owomi.dev",
                null
        );
        return new AuthResponse("access-token", "refresh-token", user);
    }
}
