package dev.selonick.owomi.auth;

import dev.selonick.owomi.auth.dto.AuthResponse;
import dev.selonick.owomi.auth.dto.ForgotPasswordRequest;
import dev.selonick.owomi.auth.dto.LoginRequest;
import dev.selonick.owomi.auth.dto.RegisterRequest;
import dev.selonick.owomi.auth.dto.ResendVerificationRequest;
import dev.selonick.owomi.auth.dto.ResetPasswordRequest;
import dev.selonick.owomi.auth.dto.TokenRequest;
import dev.selonick.owomi.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

/**
 * Endpoints d'authentification (publics).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentification", description = "Inscription, connexion, rafraîchissement et déconnexion")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Inscription d'un nouvel utilisateur")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Compte créé avec succès."));
    }

    @Operation(summary = "Connexion par email et mot de passe")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Connexion réussie."));
    }

    @Operation(summary = "Rafraîchissement de l'access token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody TokenRequest request) {
        AuthResponse response = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Token rafraîchi."));
    }

    @Operation(summary = "Déconnexion : révocation des refresh tokens")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody TokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Déconnexion réussie."));
    }

    @Operation(summary = "Vérification de l'adresse email via le token reçu par email")
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam @NotBlank(message = "Le jeton est obligatoire.") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Adresse email vérifiée avec succès."));
    }

    @Operation(summary = "Renvoi de l'email de vérification (réponse toujours générique)")
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.email());
        // Réponse identique que l'email existe / soit déjà vérifié ou non — pas de fuite d'information.
        return ResponseEntity.ok(ApiResponse.success(null,
                "Si un compte non vérifié est associé à cet email, un nouveau lien a été envoyé."));
    }

    @Operation(summary = "Demande de réinitialisation de mot de passe (réponse toujours générique)")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        // Réponse identique que l'email existe ou non — pas de fuite d'information.
        return ResponseEntity.ok(ApiResponse.success(null,
                "Si un compte est associé à cet email, un lien de réinitialisation a été envoyé."));
    }

    @Operation(summary = "Réinitialisation du mot de passe à partir d'un token valide")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null,
                "Mot de passe réinitialisé avec succès. Vous pouvez vous connecter."));
    }
}
