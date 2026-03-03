-- Copyright 2026 Matt Rajkowski, Licensed under the Apache License, Version 2.0

CREATE TABLE IF NOT EXISTS performance_metrics (
    metric_id BIGSERIAL NOT NULL,
    request_type VARCHAR(10) NOT NULL,
    status_code INT NOT NULL DEFAULT 200,
    duration_ms BIGINT NOT NULL,
    metric_date TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT performance_metrics_pkey PRIMARY KEY (metric_id)
);

CREATE INDEX IF NOT EXISTS performance_metrics_type_date_idx ON performance_metrics(request_type, metric_date);
CREATE INDEX IF NOT EXISTS performance_metrics_date_idx ON performance_metrics(metric_date);
