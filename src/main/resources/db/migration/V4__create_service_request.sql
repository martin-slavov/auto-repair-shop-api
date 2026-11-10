CREATE TABLE service_request
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    description VARCHAR(500) NOT NULL,
    status      ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    type        ENUM('SERVICE', 'INSPECTION', 'CONSULTATION') NOT NULL DEFAULT 'SERVICE',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    preferred_date DATE      NULL,
    vehicle_id  BIGINT       NOT NULL,
    FOREIGN KEY (vehicle_id) REFERENCES vehicle (id)
);