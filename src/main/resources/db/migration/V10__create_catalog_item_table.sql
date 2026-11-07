CREATE TABLE catalog_item
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100)  NOT NULL UNIQUE,
    description     VARCHAR(500)  NOT NULL,
    base_price      DECIMAL(8, 2) NOT NULL CHECK ( base_price >= 0.00),
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estimated_hours DECIMAL(5, 2) NOT NULL DEFAULT 0.00 CHECK ( estimated_hours > 0.00)
);