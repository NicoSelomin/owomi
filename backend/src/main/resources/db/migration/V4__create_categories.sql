-- V4__create_categories.sql
CREATE TABLE categories (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    icon       VARCHAR(50) NOT NULL,
    color      VARCHAR(7) NOT NULL,
    type       VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    user_id    UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_category_name_user UNIQUE (name, user_id)
);

CREATE INDEX idx_categories_user ON categories(user_id);
