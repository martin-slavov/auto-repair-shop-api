CREATE TABLE service_request
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    description VARCHAR(255) NOT NULL,
    status      ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vehicle_id  BIGINT       NOT NULL,
    FOREIGN KEY (vehicle_id) REFERENCES vehicle (id)
);