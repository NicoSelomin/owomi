-- V10__add_email_verified_to_users.sql
-- Indicateur de vérification de l'adresse email. Les comptes existants sont
-- considérés non vérifiés par défaut.
ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
