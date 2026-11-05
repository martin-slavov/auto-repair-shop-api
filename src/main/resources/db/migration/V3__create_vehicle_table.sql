CREATE TABLE vehicle
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    license_plate VARCHAR(8)  NOT NULL UNIQUE,
    manufacturer  VARCHAR(50) NOT NULL,
    model         VARCHAR(80) NOT NULL,
    year          INT         NOT NULL CHECK (year >= 1886),
    first_registration_date DATE NULL,
    owner_id      BIGINT      NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES app_user (id)
);