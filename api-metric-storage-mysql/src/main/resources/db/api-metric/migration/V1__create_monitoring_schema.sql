CREATE TABLE IF NOT EXISTS api_metric_api_inventory (
    id BIGINT NOT NULL AUTO_INCREMENT,
    api_key VARCHAR(512) NOT NULL,
    http_method VARCHAR(16) NOT NULL,
    api_path VARCHAR(512) NOT NULL,
    controller_name VARCHAR(512) NOT NULL,
    handler_method VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NULL,
    first_discovered_at DATETIME(3) NOT NULL,
    last_discovered_at DATETIME(3) NOT NULL,
    last_accessed_at DATETIME(3) NULL,
    total_hits BIGINT NOT NULL DEFAULT 0,
    successful_hits BIGINT NOT NULL DEFAULT 0,
    failed_hits BIGINT NOT NULL DEFAULT 0,
    is_currently_discovered BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_api_metric_api_key (api_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS api_metric_request_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    api_id BIGINT NOT NULL,
    correlation_id VARCHAR(128) NULL,
    http_status INT NOT NULL,
    duration_ms BIGINT NOT NULL,
    request_payload LONGTEXT NULL,
    response_payload LONGTEXT NULL,
    request_content_type VARCHAR(255) NULL,
    response_content_type VARCHAR(255) NULL,
    request_capture_status VARCHAR(32) NOT NULL,
    response_capture_status VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_api_metric_request_api_created (api_id, created_at),
    KEY idx_api_metric_request_created (created_at),
    KEY idx_api_metric_request_status_created (http_status, created_at),
    CONSTRAINT fk_api_metric_request_api
        FOREIGN KEY (api_id) REFERENCES api_metric_api_inventory (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS api_metric_usage_aggregate (
    api_id BIGINT NOT NULL,
    bucket_start DATETIME(3) NOT NULL,
    bucket_size VARCHAR(16) NOT NULL,
    total_hits BIGINT NOT NULL,
    successful_hits BIGINT NOT NULL,
    failed_hits BIGINT NOT NULL,
    total_duration_ms BIGINT NOT NULL,
    min_duration_ms BIGINT NOT NULL,
    max_duration_ms BIGINT NOT NULL,
    PRIMARY KEY (api_id, bucket_size, bucket_start),
    CONSTRAINT fk_api_metric_aggregate_api
        FOREIGN KEY (api_id) REFERENCES api_metric_api_inventory (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS api_metric_latency_bucket (
    api_id BIGINT NOT NULL,
    bucket_start DATETIME(3) NOT NULL,
    bucket_size VARCHAR(16) NOT NULL,
    upper_bound_ms BIGINT NOT NULL,
    hit_count BIGINT NOT NULL,
    PRIMARY KEY (api_id, bucket_size, bucket_start, upper_bound_ms),
    CONSTRAINT fk_api_metric_latency_api
        FOREIGN KEY (api_id) REFERENCES api_metric_api_inventory (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
