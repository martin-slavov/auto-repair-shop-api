CREATE TABLE part
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100)  NOT NULL,
    serial_number   VARCHAR(60)   NOT NULL UNIQUE,
    unit_cost       DECIMAL(8, 2) NOT NULL CHECK ( unit_cost >= 0.00),
    unit_sell_price DECIMAL(8, 2) NOT NULL CHECK ( unit_sell_price >= 0.00 ),
    active          BOOLEAN       NOT NULL DEFAULT TRUE
);