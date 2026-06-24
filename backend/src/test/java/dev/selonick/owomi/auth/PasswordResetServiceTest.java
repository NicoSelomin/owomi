package dev.selonick.owomi.auth;

import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.user.User;
import dev.selonick.owomi.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de la réinitialisation de mot de passe :
 * reset valide, token expiré, token déjà utilisé, email inexistant.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User buildUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Nicodème");
        user.setEmail("test@owomi.dev");
        user.setPassword("$2a$12$oldhash");
        return user;
    }

    private PasswordResetToken buildToken(User user, Instant expiresAt, boolean used) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken("reset-token");
        token.setExpiresAt(expiresAt);
        token.setUsed(used);
        return token;
    }

    // --- requestReset ---

    @Test
    @DisplayName("Demande : email existant → persiste un token et envoie l'email")
    void requestReset_ExistingEmail_ShouldSendEmail() {
        User user = buildUser();
        when(userRepository.findByEmail("test@owomi.dev")).thenReturn(Optional.of(user));

        passwordResetService.requestReset("test@owomi.dev");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken saved = captor.getValue();

        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getToken()).isNotBlank();
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
        verify(emailService).sendPasswordResetEmail(user, saved.getToken());
    }

    @Test
    @DisplayName("Demande : email inexistant → aucune erreur, aucun token, aucun email (anti-énumération)")
    void requestReset_UnknownEmail_ShouldDoNothing() {
        when(userRepository.findByEmail("unknown@owomi.dev")).thenReturn(Optional.empty());

        passwordResetService.requestReset("unknown@owomi.dev");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), anyString());
    }

    // --- reset ---

    @Test
    @DisplayName("Reset : token valide → change le mot de passe, marque le token utilisé, révoque les sessions")
    void reset_ValidToken_ShouldChangePassword() {
        User user = buildUser();
        PasswordResetToken token = buildToken(user, Instant.now().plus(30, ChronoUnit.MINUTES), false);
        when(tokenRepository.findByToken("reset-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPassword1!")).thenReturn("$2a$12$newhash");

        passwordResetService.reset("reset-token", "NewPassword1!");

        assertThat(user.getPassword()).isEqualTo("$2a$12$newhash");
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
        verify(refreshTokenService).revokeAllForUser(user);
    }

    @Test
    @DisplayName("Reset : token inconnu → BusinessException RESET_TOKEN_INVALID")
    void reset_UnknownToken_ShouldThrow() {
        when(tokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.reset("unknown", "NewPassword1!"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESET_TOKEN_INVALID);

        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    @DisplayName("Reset : token déjà utilisé → BusinessException RESET_TOKEN_INVALID")
    void reset_UsedToken_ShouldThrow() {
        User user = buildUser();
        PasswordResetToken token = buildToken(user, Instant.now().plus(30, ChronoUnit.MINUTES), true);
        when(tokenRepository.findByToken("reset-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.reset("reset-token", "NewPassword1!"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESET_TOKEN_INVALID);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reset : token expiré → BusinessException RESET_TOKEN_EXPIRED")
    void reset_ExpiredToken_ShouldThrow() {
        User user = buildUser();
        PasswordResetToken token = buildToken(user, Instant.now().minus(1, ChronoUnit.MINUTES), false);
        when(tokenRepository.findByToken("reset-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.reset("reset-token", "NewPassword1!"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESET_TOKEN_EXPIRED);

        assertThat(user.getPassword()).isEqualTo("$2a$12$oldhash");
        verify(userRepository, never()).save(any());
    }
}
