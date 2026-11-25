ALTER TABLE api_metric_request_log
    ADD COLUMN request_headers LONGTEXT NULL,
    ADD COLUMN response_headers LONGTEXT NULL;
