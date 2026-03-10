-- Usuarios iniciales: admin y usuarios de nivel bajo (USER).
-- Contraseña por defecto para todos: "password" (cambiar en producción).
-- Hash BCrypt de "password": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (id, username, password_hash, role, created_at) VALUES
    ('b0000001-0000-0000-0000-000000000001', 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', CURRENT_TIMESTAMP),
    ('b0000002-0000-0000-0000-000000000002', 'operator', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', CURRENT_TIMESTAMP),
    ('b0000003-0000-0000-0000-000000000003', 'viewer', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
