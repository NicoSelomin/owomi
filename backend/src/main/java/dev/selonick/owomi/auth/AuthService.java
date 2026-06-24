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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logique métier de l'authentification : inscription, connexion, rafraîchissement, déconnexion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CurrencyRepository currencyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    /** Inscription d'un nouvel utilisateur. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Currency currency = currencyRepository.findById(request.currencyCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Devise inconnue : " + request.currencyCode()));

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setCurrency(currency);
        user.setRole(Role.USER);

        User saved = userRepository.save(user);
        log.info("New user registered: id={}, email={}", saved.getId(), saved.getEmail());

        // Le compte est actif immédiatement ; l'email reste à vérifier via le lien envoyé.
        emailVerificationService.createAndSend(saved);

        return buildAuthResponse(saved);
    }

    /** Confirmation d'adresse email à partir du token reçu par email. */
    @Transactional
    public void verifyEmail(String token) {
        emailVerificationService.verify(token);
    }

    /**
     * Renvoi de l'email de vérification.
     * Ne révèle jamais si l'email existe ou est déjà vérifié (réponse générique côté contrôleur).
     */
    @Transactional
    public void resendVerification(String email) {
        emailVerificationService.resend(email);
    }

    /**
     * Demande de réinitialisation de mot de passe.
     * Ne révèle jamais si l'email existe (réponse générique côté contrôleur).
     */
    @Transactional
    public void forgotPassword(String email) {
        passwordResetService.requestReset(email);
    }

    /** Application d'un nouveau mot de passe à partir d'un token de réinitialisation. */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        passwordResetService.reset(token, newPassword);
    }

    /** Connexion par email / mot de passe. */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        log.info("User logged in: email={}", user.getEmail());
        return buildAuthResponse(user);
    }

    /** Rafraîchissement de l'access token à partir d'un refresh token valide. */
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken stored = refreshTokenService.verify(refreshToken);
        User user = stored.getUser();

        String newAccessToken = jwtService.generateAccessToken(user);
        // Le refresh token reste valide jusqu'à son expiration ; seul l'access token est renouvelé.
        return new AuthResponse(newAccessToken, refreshToken, userMapper.toDto(user));
    }

    /** Déconnexion : révocation des refresh tokens. */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.logout(refreshToken);
    }

    /** Génère les tokens et persiste le refresh token. */
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        refreshTokenService.create(user, refreshToken);
        return new AuthResponse(accessToken, refreshToken, userMapper.toDto(user));
    }
}
