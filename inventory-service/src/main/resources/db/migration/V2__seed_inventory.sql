-- Inventario inicial para los productos básicos del products-service (mismos UUIDs que V3__seed_products).
-- ON CONFLICT (product_id) DO NOTHING evita fallos si ya existen filas (ej. reinicio).
INSERT INTO inventory (product_id, available, reserved, version) VALUES
    ('a0000001-0000-0000-0000-000000000001', 10, 0, 0),
    ('a0000002-0000-0000-0000-000000000002', 50, 0, 0),
    ('a0000003-0000-0000-0000-000000000003', 30, 0, 0),
    ('a0000004-0000-0000-0000-000000000004', 15, 0, 0),
    ('a0000005-0000-0000-0000-000000000005', 25, 0, 0)
ON CONFLICT (product_id) DO NOTHING;
