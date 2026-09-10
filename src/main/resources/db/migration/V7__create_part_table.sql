CREATE TABLE part
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(255)  NOT NULL,
    serial_number   VARCHAR(255)  NOT NULL UNIQUE,
    unit_cost       DECIMAL(8, 2) NOT NULL,
    unit_sell_price DECIMAL(8, 2) NOT NULL
);