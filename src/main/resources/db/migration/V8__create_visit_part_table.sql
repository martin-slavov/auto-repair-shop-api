CREATE TABLE visit_part
(
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_visit_id     BIGINT        NOT NULL,
    part_id              BIGINT        NOT NULL,
    quantity             INT           NOT NULL CHECK ( quantity > 0 ),
    price_at_time_of_use DECIMAL(8, 2) NOT NULL CHECK ( price_at_time_of_use >= 0.00 ),
    added_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (service_visit_id) REFERENCES service_visit (id),
    FOREIGN KEY (part_id) REFERENCES part (id)
);