CREATE TABLE IF NOT EXISTS reading (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    ts          BIGINT      NOT NULL, -- epoch millis
    source      VARCHAR(16) NOT NULL,
    temperature DOUBLE,
    humidity    DOUBLE,
    wind_speed  DOUBLE
);

CREATE INDEX IF NOT EXISTS reading_ts ON reading (ts);
