-- Mínimo 500 productos para pruebas (UUIDs deterministas para referenciarlos desde inventory-service).
INSERT INTO products (id, sku, name, price, status, created_at, updated_at)
SELECT
    ('a0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0'))::uuid,
    'SKU-' || lpad(i::text, 5, '0'),
    'Producto ' || i,
    (10 + (i % 490))::decimal(19, 4),
    CASE WHEN i % 10 = 0 THEN 'INACTIVE' ELSE 'ACTIVE' END,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM generate_series(1, 500) AS i
ON CONFLICT (id) DO NOTHING;
