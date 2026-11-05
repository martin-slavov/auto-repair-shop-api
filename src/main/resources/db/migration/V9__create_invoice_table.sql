CREATE TABLE invoice
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_number VARCHAR(70)   NOT NULL UNIQUE,
    visit_id       BIGINT        NOT NULL UNIQUE,
    parts_price    DECIMAL(8, 2) NOT NULL CHECK ( parts_price >= 0.00 ),
    labor_price    DECIMAL(8, 2) NOT NULL CHECK ( labor_price >= 0.00 ),
    total_price    DECIMAL(8, 2) NOT NULL CHECK ( total_price >= 0.00 ),
    issued_date    TIMESTAMP     NOT NULL,
    paid           BOOLEAN       NOT NULL DEFAULT FALSE,
    FOREIGN KEY (visit_id) REFERENCES service_visit (id)
);