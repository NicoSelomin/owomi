package dev.selonick.owomi.auth;

import dev.selonick.owomi.user.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Envoi des emails transactionnels (vérification d'adresse, réinitialisation de mot de passe).
 * Les templates HTML respectent la charte OWOMI (fond sombre #0C131E, accent or #D49E10).
 * Les échecs d'envoi sont journalisés sans interrompre le parcours utilisateur — jamais de
 * donnée sensible (token, mot de passe) dans les logs.
 */
@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;
    private final String frontendUrl;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.mail.from-name}") String fromName,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        // Retrait d'un éventuel slash final pour éviter les doubles slashes dans les liens
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    /** Email de confirmation d'inscription, avec lien de vérification (valable 24h). */
    public void sendVerificationEmail(User user, String token) {
        String link = frontendUrl + "/auth/verify-email?token=" + token;
        String html = buildVerificationHtml(user.getName(), link);
        send(user.getEmail(), "Confirmez votre adresse email — OWOMI", html);
    }

    /** Email de réinitialisation de mot de passe, avec lien (valable 1h). */
    public void sendPasswordResetEmail(User user, String token) {
        String link = frontendUrl + "/auth/reset-password?token=" + token;
        String html = buildPasswordResetHtml(user.getName(), link);
        send(user.getEmail(), "Réinitialisation de votre mot de passe — OWOMI", html);
    }

    /** Envoi bas niveau d'un email HTML. Les erreurs sont journalisées, sans rethrow. */
    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Transactional email sent: subject='{}', to={}", subject, to);
        } catch (MessagingException | UnsupportedEncodingException | RuntimeException e) {
            // On ne bloque pas le parcours utilisateur si l'envoi échoue ; on journalise sans token.
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    // --- Templates HTML ---

    private String buildVerificationHtml(String name, String link) {
        return layout(
                "Bienvenue sur OWOMI 👋",
                "Bonjour " + escape(name) + ",",
                "Merci d'avoir créé votre compte OWOMI. Pour activer toutes les fonctionnalités, "
                        + "veuillez confirmer votre adresse email en cliquant sur le bouton ci-dessous.",
                "Confirmer mon adresse email",
                link,
                "Ce lien est valable 24 heures. Si vous n'êtes pas à l'origine de cette inscription, "
                        + "vous pouvez ignorer cet email."
        );
    }

    private String buildPasswordResetHtml(String name, String link) {
        return layout(
                "Réinitialisation du mot de passe",
                "Bonjour " + escape(name) + ",",
                "Vous avez demandé la réinitialisation de votre mot de passe OWOMI. "
                        + "Cliquez sur le bouton ci-dessous pour en définir un nouveau.",
                "Réinitialiser mon mot de passe",
                link,
                "Ce lien est valable 1 heure. Si vous n'êtes pas à l'origine de cette demande, "
                        + "ignorez cet email : votre mot de passe restera inchangé."
        );
    }

    /** Gabarit HTML commun aux emails OWOMI (responsive, compatible clients mail). */
    private String layout(String title, String greeting, String body,
                          String ctaLabel, String ctaLink, String footerNote) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
                <body style="margin:0;padding:0;background-color:#0C131E;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#0C131E;padding:32px 16px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;background-color:#111A28;border:1px solid rgba(212,158,16,0.18);border-radius:16px;overflow:hidden;">
                        <tr><td style="height:4px;background-color:#D49E10;"></td></tr>
                        <tr><td style="padding:32px 32px 8px 32px;">
                          <div style="font-size:22px;font-weight:700;color:#FFFFFF;letter-spacing:1px;">OWO<span style="color:#D49E10;">MI</span></div>
                        </td></tr>
                        <tr><td style="padding:8px 32px 0 32px;">
                          <h1 style="margin:0 0 16px 0;font-size:20px;color:#FFFFFF;">%s</h1>
                          <p style="margin:0 0 12px 0;font-size:15px;color:#D8DEE9;">%s</p>
                          <p style="margin:0 0 24px 0;font-size:15px;line-height:1.6;color:#9AA6B5;">%s</p>
                        </td></tr>
                        <tr><td align="center" style="padding:0 32px 8px 32px;">
                          <a href="%s" style="display:inline-block;background-color:#D49E10;color:#0C131E;font-size:15px;font-weight:700;text-decoration:none;padding:14px 28px;border-radius:10px;">%s</a>
                        </td></tr>
                        <tr><td style="padding:16px 32px 0 32px;">
                          <p style="margin:0;font-size:12px;color:#6B7585;">Ou copiez ce lien dans votre navigateur :</p>
                          <p style="margin:4px 0 0 0;font-size:12px;word-break:break-all;"><a href="%s" style="color:#D49E10;">%s</a></p>
                        </td></tr>
                        <tr><td style="padding:24px 32px 32px 32px;">
                          <hr style="border:none;border-top:1px solid rgba(255,255,255,0.08);margin:0 0 16px 0;">
                          <p style="margin:0;font-size:12px;line-height:1.6;color:#6B7585;">%s</p>
                          <p style="margin:16px 0 0 0;font-size:11px;color:#4A5568;">OWOMI — Gestion de budget personnelle, gratuite. Cotonou, Bénin.</p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escape(title), greeting, escape(body),
                ctaLink, escape(ctaLabel), ctaLink, ctaLink, escape(footerNote));
    }

    /** Échappement HTML minimal pour le contenu dynamique (prévention XSS dans le mail). */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
