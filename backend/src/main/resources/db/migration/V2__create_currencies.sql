-- V2__create_currencies.sql
CREATE TABLE currencies (
    code   VARCHAR(3) PRIMARY KEY,
    name   VARCHAR(100) NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    locale VARCHAR(10) NOT NULL
);

ALTER TABLE users
    ADD CONSTRAINT fk_users_currency
    FOREIGN KEY (currency_code) REFERENCES currencies(code);
