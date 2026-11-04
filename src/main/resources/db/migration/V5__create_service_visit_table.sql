CREATE TABLE service_visit
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_request_id BIGINT UNIQUE NOT NULL,
    vehicle_id         BIGINT    NOT NULL,
    status             ENUM('SCHEDULED', 'IN_PROGRESS', 'COMPLETED','INVOICED') NOT NULL DEFAULT 'SCHEDULED',
    scheduled_date     TIMESTAMP NOT NULL,
    completed_date     TIMESTAMP,
    technical_notes    VARCHAR(5000),
    FOREIGN KEY (service_request_id) REFERENCES service_request (id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle (id)
);