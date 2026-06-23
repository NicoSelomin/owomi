-- V6__create_transactions.sql
CREATE TABLE transactions (
    id          BIGSERIAL PRIMARY KEY,
    amount      DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    type        VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    note        TEXT,
    date        DATE NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_date_not_future CHECK (date <= CURRENT_DATE)
);

CREATE INDEX idx_transactions_user ON transactions(user_id);
CREATE INDEX idx_transactions_user_date ON transactions(user_id, date DESC);
CREATE INDEX idx_transactions_category ON transactions(category_id);
