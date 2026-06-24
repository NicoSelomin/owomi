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
 * Tests unitaires de la vérification d'adresse email :
 * token valide, token expiré, token déjà utilisé.
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User buildUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Nicodème");
        user.setEmail("test@owomi.dev");
        user.setEmailVerified(false);
        return user;
    }

    private EmailVerificationToken buildToken(User user, Instant expiresAt, boolean used) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken("verif-token");
        token.setExpiresAt(expiresAt);
        token.setUsed(used);
        return token;
    }

    // --- createAndSend ---

    @Test
    @DisplayName("Génération : persiste un token non utilisé, à 24h, et déclenche l'envoi de l'email")
    void createAndSend_ShouldPersistTokenAndSendEmail() {
        User user = buildUser();

        emailVerificationService.createAndSend(user);

        ArgumentCaptor<EmailVerificationToken> captor =
                ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailVerificationToken saved = captor.getValue();

        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getToken()).isNotBlank();
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(23, ChronoUnit.HOURS));
        verify(emailService).sendVerificationEmail(user, saved.getToken());
    }

    // --- verify ---

    @Test
    @DisplayName("Vérification : token valide → marque l'email vérifié et le token utilisé")
    void verify_ValidToken_ShouldMarkVerified() {
        User user = buildUser();
        EmailVerificationToken token =
                buildToken(user, Instant.now().plus(1, ChronoUnit.HOURS), false);
        when(tokenRepository.findByToken("verif-token")).thenReturn(Optional.of(token));

        emailVerificationService.verify("verif-token");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    @DisplayName("Vérification : token inconnu → BusinessException VERIFICATION_TOKEN_INVALID")
    void verify_UnknownToken_ShouldThrow() {
        when(tokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verify("unknown"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VERIFICATION_TOKEN_INVALID);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Vérification : token déjà utilisé → BusinessException VERIFICATION_TOKEN_INVALID")
    void verify_UsedToken_ShouldThrow() {
        User user = buildUser();
        EmailVerificationToken token =
                buildToken(user, Instant.now().plus(1, ChronoUnit.HOURS), true);
        when(tokenRepository.findByToken("verif-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> emailVerificationService.verify("verif-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VERIFICATION_TOKEN_INVALID);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Vérification : token expiré → BusinessException VERIFICATION_TOKEN_EXPIRED")
    void verify_ExpiredToken_ShouldThrow() {
        User user = buildUser();
        EmailVerificationToken token =
                buildToken(user, Instant.now().minus(1, ChronoUnit.HOURS), false);
        when(tokenRepository.findByToken("verif-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> emailVerificationService.verify("verif-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VERIFICATION_TOKEN_EXPIRED);

        assertThat(user.isEmailVerified()).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Vérification : token vide → BusinessException VERIFICATION_TOKEN_INVALID")
    void verify_BlankToken_ShouldThrow() {
        when(tokenRepository.findByToken("")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verify(""))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VERIFICATION_TOKEN_INVALID);

        verify(emailService, never()).sendVerificationEmail(any(), anyString());
    }

    // --- resend ---

    @Test
    @DisplayName("Renvoi : email non vérifié → invalide les anciens tokens, en émet un nouveau et envoie")
    void resend_UnverifiedEmail_ShouldReissueAndSend() {
        User user = buildUser();
        when(userRepository.findByEmail("test@owomi.dev")).thenReturn(Optional.of(user));

        emailVerificationService.resend("test@owomi.dev");

        verify(tokenRepository).invalidateAllForUser(user);
        ArgumentCaptor<EmailVerificationToken> captor =
                ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        verify(emailService).sendVerificationEmail(user, captor.getValue().getToken());
    }

    @Test
    @DisplayName("Renvoi : email déjà vérifié → aucun token, aucun email")
    void resend_AlreadyVerified_ShouldDoNothing() {
        User user = buildUser();
        user.setEmailVerified(true);
        when(userRepository.findByEmail("test@owomi.dev")).thenReturn(Optional.of(user));

        emailVerificationService.resend("test@owomi.dev");

        verify(tokenRepository, never()).invalidateAllForUser(any());
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(any(), anyString());
    }

    @Test
    @DisplayName("Renvoi : email inconnu → aucune erreur, aucun email (anti-énumération)")
    void resend_UnknownEmail_ShouldDoNothing() {
        when(userRepository.findByEmail("unknown@owomi.dev")).thenReturn(Optional.empty());

        emailVerificationService.resend("unknown@owomi.dev");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(any(), anyString());
    }

    @Test
    @DisplayName("Renvoi : deuxième appel dans la minute → limité, un seul email envoyé")
    void resend_WithinCooldown_ShouldRateLimit() {
        User user = buildUser();
        when(userRepository.findByEmail("test@owomi.dev")).thenReturn(Optional.of(user));

        emailVerificationService.resend("test@owomi.dev");
        // Deuxième demande immédiate pour la même adresse
        emailVerificationService.resend("test@owomi.dev");

        // Un seul envoi malgré deux appels
        verify(emailService).sendVerificationEmail(any(), anyString());
        verify(tokenRepository).invalidateAllForUser(user);
    }
}
