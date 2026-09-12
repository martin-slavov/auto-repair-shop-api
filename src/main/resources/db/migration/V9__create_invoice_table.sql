CREATE TABLE invoice
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_number VARCHAR(255)  NOT NULL UNIQUE,
    visit_id       BIGINT        NOT NULL UNIQUE,
    parts_price    DECIMAL(8, 2) NOT NULL,
    labor_price    DECIMAL(8, 2) NOT NULL,
    total_price    DECIMAL(8, 2) NOT NULL,
    issued_date    TIMESTAMP     NOT NULL,
    paid           BOOLEAN       NOT NULL DEFAULT FALSE,
    FOREIGN KEY (visit_id) REFERENCES service_visit (id)
);