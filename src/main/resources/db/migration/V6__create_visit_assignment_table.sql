CREATE TABLE visit_assignment
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_visit_id BIGINT NOT NULL,
    mechanic_id      BIGINT NOT NULL,
    hours_worked DOUBLE NOT NULL DEFAULT 0.00,
    role_in_visit    ENUM('LEAD', 'ASSISTANT') NOT NULL,
    FOREIGN KEY (service_visit_id) REFERENCES service_visit (id),
    FOREIGN KEY (mechanic_id) REFERENCES app_user (id)
);