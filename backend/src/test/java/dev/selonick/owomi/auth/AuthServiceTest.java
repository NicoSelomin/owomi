package dev.selonick.owomi.auth;

import dev.selonick.owomi.auth.dto.AuthResponse;
import dev.selonick.owomi.auth.dto.LoginRequest;
import dev.selonick.owomi.auth.dto.RegisterRequest;
import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.currency.Currency;
import dev.selonick.owomi.currency.CurrencyRepository;
import dev.selonick.owomi.user.Role;
import dev.selonick.owomi.user.User;
import dev.selonick.owomi.user.UserMapper;
import dev.selonick.owomi.user.UserRepository;
import dev.selonick.owomi.user.dto.UserDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private AuthService authService;

    // --- Helpers ---

    private Currency buildCurrency() {
        return new Currency("XOF", "Franc CFA UEMOA", "FCFA", "fr-BJ");
    }

    private User buildUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Nicodème");
        user.setEmail("test@owomi.dev");
        user.setPassword("$2a$12$hashedpassword");
        user.setCurrency(buildCurrency());
        user.setRole(Role.USER);
        user.setEmailVerified(true);
        return user;
    }

    // --- register ---

    @Test
    @DisplayName("Inscription : email déjà existant → BusinessException EMAIL_ALREADY_EXISTS")
    void register_EmailAlreadyExists_ShouldThrow() {
        RegisterRequest request = new RegisterRequest(
                "Nicodème", "test@owomi.dev", "Password1!", "BJ", "XOF");
        when(userRepository.existsByEmail("test@owomi.dev")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Inscription : requête valide → crée l'utilisateur et renvoie des tokens")
    void register_ValidRequest_ShouldCreateUser() {
        RegisterRequest request = new RegisterRequest(
                "Nicodème", "test@owomi.dev", "Password1!", "BJ", "XOF");
        User saved = buildUser();

        when(userRepository.existsByEmail("test@owomi.dev")).thenReturn(false);
        when(currencyRepository.findById("XOF")).thenReturn(Optional.of(buildCurrency()));
        when(passwordEncoder.encode("Password1!")).thenReturn("$2a$12$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.generateAccessToken(saved)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(saved)).thenReturn("refresh-token");
        when(userMapper.toDto(saved))
                .thenReturn(new UserDTO(saved.getId(), saved.getName(), saved.getEmail(), null));

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("test@owomi.dev");
        verify(userRepository).save(any(User.class));
        verify(refreshTokenService).create(saved, "refresh-token");
        // Un email de vérification doit être généré et envoyé après l'inscription
        verify(emailVerificationService).createAndSend(saved);
    }

    @Test
    @DisplayName("Inscription : devise inconnue → BusinessException VALIDATION_ERROR")
    void register_UnknownCurrency_ShouldThrowValidationError() {
        RegisterRequest request = new RegisterRequest(
                "Nicodème", "test@owomi.dev", "Password1!", "BJ", "ZZZ");

        when(userRepository.existsByEmail("test@owomi.dev")).thenReturn(false);
        when(currencyRepository.findById("ZZZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).create(any(), anyString());
    }

    @Test
    @DisplayName("Inscription : normalise l'email et trim le nom avant persistance")
    void register_ShouldNormalizeEmailAndTrimName() {
        RegisterRequest request = new RegisterRequest(
                "  Nicodème  ", "  Test@OWOMI.Dev  ", "Password1!", "BJ", "XOF");

        when(userRepository.existsByEmail("test@owomi.dev")).thenReturn(false);
        when(currencyRepository.findById("XOF")).thenReturn(Optional.of(buildCurrency()));
        when(passwordEncoder.encode("Password1!")).thenReturn("$2a$12$hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(userMapper.toDto(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return new UserDTO(user.getId(), user.getName(), user.getEmail(), null);
        });

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertThat(saved.getName()).isEqualTo("Nicodème");
        assertThat(saved.getEmail()).isEqualTo("test@owomi.dev");
        assertThat(response.user().email()).isEqualTo("test@owomi.dev");
        verify(userRepository).existsByEmail("test@owomi.dev");
    }

    // --- login ---

    @Test
    @DisplayName("Connexion : identifiants incorrects → BusinessException INVALID_CREDENTIALS")
    void login_InvalidCredentials_ShouldThrow() {
        LoginRequest request = new LoginRequest("test@owomi.dev", "WrongPassword");
        User user = buildUser();

        when(userRepository.findByEmail("test@owomi.dev")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Connexion : identifiants valides → renvoie access et refresh tokens")
    void login_ValidCredentials_ShouldReturnTokens() {
        LoginRequest request = new LoginRequest("test@owomi.dev", "Password1!");
        User user = buildUser();

        when(userRepository.findByEmail("test@owomi.dev")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1!", user.getPassword())).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(userMapper.toDto(user))
                .thenReturn(new UserDTO(user.getId(), user.getName(), user.getEmail(), null));

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenService).create(user, "refresh-token");
    }

    @Test
    @DisplayName("Connexion : email inconnu → BusinessException INVALID_CREDENTIALS")
    void login_UnknownEmail_ShouldThrow() {
        LoginRequest request = new LoginRequest("unknown@owomi.dev", "Password1!");
        when(userRepository.findByEmail("unknown@owomi.dev")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Connexion : normalise l'email avant recherche")
    void login_ShouldNormalizeEmail() {
        LoginRequest request = new LoginRequest("  Test@OWOMI.Dev  ", "Password1!");
        User user = buildUser();

        when(userRepository.findByEmail("test@owomi.dev")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1!", user.getPassword())).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(userMapper.toDto(user))
                .thenReturn(new UserDTO(user.getId(), user.getName(), user.getEmail(), null));

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(userRepository).findByEmail("test@owomi.dev");
    }

    @Test
    @DisplayName("Connexion : email non vérifié → BusinessException EMAIL_NOT_VERIFIED sans générer de tokens")
    void login_UnverifiedEmail_ShouldThrowEmailNotVerified() {
        LoginRequest request = new LoginRequest("test@owomi.dev", "Password1!");
        User user = buildUser();
        user.setEmailVerified(false);

        when(userRepository.findByEmail("test@owomi.dev")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1!", user.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
                    assertThat(businessException.getMessage()).isEqualTo(
                            "Votre adresse email n'est pas encore vérifiée. Vous pouvez demander un nouveau lien de vérification.");
                });

        verify(jwtService, never()).generateAccessToken(any());
        verify(jwtService, never()).generateRefreshToken(any());
        verify(refreshTokenService, never()).create(any(), anyString());
    }

    // --- refresh ---

    @Test
    @DisplayName("Refresh : token valide → renvoie un nouvel access token et conserve le refresh token")
    void refresh_ValidRefreshToken_ShouldReturnNewAccessTokenAndSameRefreshToken() {
        User user = buildUser();
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setToken("refresh-token");

        when(refreshTokenService.verify("refresh-token")).thenReturn(stored);
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(userMapper.toDto(user))
                .thenReturn(new UserDTO(user.getId(), user.getName(), user.getEmail(), null));

        AuthResponse response = authService.refresh("refresh-token");

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(jwtService, never()).generateRefreshToken(any());
    }

    @Test
    @DisplayName("Refresh : token inconnu → propage BusinessException TOKEN_INVALID")
    void refresh_InvalidRefreshToken_ShouldThrow() {
        when(refreshTokenService.verify("invalid-token"))
                .thenThrow(new BusinessException(ErrorCode.TOKEN_INVALID, "Refresh token invalide."));

        assertThatThrownBy(() -> authService.refresh("invalid-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_INVALID);

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Refresh : token expiré ou révoqué → propage BusinessException TOKEN_EXPIRED")
    void refresh_ExpiredRefreshToken_ShouldThrow() {
        when(refreshTokenService.verify("expired-token"))
                .thenThrow(new BusinessException(ErrorCode.TOKEN_EXPIRED,
                        "Refresh token expiré ou révoqué. Veuillez vous reconnecter."));

        assertThatThrownBy(() -> authService.refresh("expired-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);

        verify(jwtService, never()).generateAccessToken(any());
    }

    // --- logout ---

    @Test
    @DisplayName("Déconnexion : délègue la révocation au RefreshTokenService")
    void logout_ShouldDelegateToRefreshTokenService() {
        authService.logout("refresh-token");

        verify(refreshTokenService).logout("refresh-token");
    }

    // --- email verification / reset password ---

    @Test
    @DisplayName("Renvoi vérification : normalise l'email avant délégation")
    void resendVerification_ShouldNormalizeEmail() {
        authService.resendVerification("  Test@OWOMI.Dev  ");

        verify(emailVerificationService).resend("test@owomi.dev");
    }

    @Test
    @DisplayName("Mot de passe oublié : normalise l'email avant délégation")
    void forgotPassword_ShouldNormalizeEmail() {
        authService.forgotPassword("  Test@OWOMI.Dev  ");

        verify(passwordResetService).requestReset("test@owomi.dev");
    }
}
