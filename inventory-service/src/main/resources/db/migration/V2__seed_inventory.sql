-- Inventario inicial para los 500 productos del products-service (mismos UUIDs que V3__seed_products).
INSERT INTO inventory (product_id, available, reserved, version)
SELECT
    ('a0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0'))::uuid,
    10 + (i % 90),
    0,
    0
FROM generate_series(1, 500) AS i
ON CONFLICT (product_id) DO NOTHING;
