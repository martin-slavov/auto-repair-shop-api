ALTER TABLE service_request
    ADD COLUMN preferred_period ENUM('MORNING', 'AFTERNOON', 'ANY') NULL;

UPDATE service_request
SET preferred_period = 'ANY'
WHERE preferred_date IS NOT NULL;
