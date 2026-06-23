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
}
