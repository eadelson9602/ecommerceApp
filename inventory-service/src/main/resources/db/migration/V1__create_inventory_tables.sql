CREATE TABLE inventory (
    product_id UUID PRIMARY KEY,
    available INT NOT NULL DEFAULT 0 CHECK (available >= 0),
    reserved INT NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE purchase (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    idempotency_key VARCHAR(255) UNIQUE,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE purchase ADD CONSTRAINT fk_purchase_product
    FOREIGN KEY (product_id) REFERENCES inventory(product_id);

CREATE TABLE idempotency (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    response_status INT NOT NULL,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_purchase_product_id ON purchase(product_id);
CREATE INDEX idx_purchase_idempotency_key ON purchase(idempotency_key);
