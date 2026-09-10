CREATE TABLE visit_part
(
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_visit_id     BIGINT        NOT NULL,
    part_id              BIGINT        NOT NULL,
    quantity             INT           NOT NULL,
    price_at_time_of_use DECIMAL(8, 2) NOT NULL,
    FOREIGN KEY (service_visit_id) REFERENCES service_visit (id),
    FOREIGN KEY (part_id) REFERENCES part (id)
);