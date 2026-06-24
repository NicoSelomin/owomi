package dev.selonick.owomi.auth;

import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.user.User;
import dev.selonick.owomi.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Réinitialisation de mot de passe : demande (génération + email) et application du nouveau mot de passe.
 * Token UUID v4 (SecureRandom via UUID.randomUUID), à usage unique, valable 1h.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    /** Durée de validité d'un token de réinitialisation. */
    private static final Duration TOKEN_VALIDITY = Duration.ofHours(1);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    /**
     * Demande de réinitialisation. Pour ne pas révéler l'existence d'un compte,
     * cette méthode ne lève jamais d'erreur si l'email est inconnu (réponse générique côté contrôleur).
     */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email).ifPresentOrElse(
                user -> {
                    String token = UUID.randomUUID().toString();
                    PasswordResetToken resetToken = new PasswordResetToken();
                    resetToken.setUser(user);
                    resetToken.setToken(token);
                    resetToken.setExpiresAt(Instant.now().plus(TOKEN_VALIDITY));
                    resetToken.setUsed(false);
                    tokenRepository.save(resetToken);

                    emailService.sendPasswordResetEmail(user, token);
                    log.info("Password reset email queued for user: id={}", user.getId());
                },
                // Email inconnu : on ne fait rien, mais on journalise (sans révéler à l'appelant)
                () -> log.info("Password reset requested for unknown email")
        );
    }

    /**
     * Applique un nouveau mot de passe à partir d'un token valide.
     * Vérifie l'existence, l'usage unique et l'expiration, puis invalide tous les refresh tokens.
     */
    @Transactional
    public void reset(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESET_TOKEN_INVALID));

        if (resetToken.isUsed()) {
            throw new BusinessException(ErrorCode.RESET_TOKEN_INVALID);
        }
        if (resetToken.isExpired()) {
            throw new BusinessException(ErrorCode.RESET_TOKEN_EXPIRED);
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        resetToken.setUsed(true);
        userRepository.save(user);
        tokenRepository.save(resetToken);

        // Sécurité : toute session existante est invalidée après changement de mot de passe
        refreshTokenService.revokeAllForUser(user);

        log.info("Password reset completed for user: id={}", user.getId());
    }
}
