-- V5__seed_default_categories.sql
-- Catégories par défaut (user_id NULL = partagées par tous les utilisateurs)
INSERT INTO categories (name, icon, color, type, is_default) VALUES
    ('Alimentation', 'restaurant-outline',      '#D85A30', 'EXPENSE', TRUE),
    ('Transport',    'car-outline',             '#185FA5', 'EXPENSE', TRUE),
    ('Logement',     'home-outline',            '#854F0B', 'EXPENSE', TRUE),
    ('Santé',        'medical-outline',         '#1D9E75', 'EXPENSE', TRUE),
    ('Loisirs',      'game-controller-outline', '#888780', 'EXPENSE', TRUE),
    ('Revenus',      'cash-outline',            '#1D9E75', 'INCOME',  TRUE);
