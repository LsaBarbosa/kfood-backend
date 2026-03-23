CREATE TABLE processed_event (
    id UUID PRIMARY KEY,
    consumer_name VARCHAR(120) NOT NULL,
    event_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    aggregate_id VARCHAR(80),
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_processed_event_consumer_event
    ON processed_event (consumer_name, event_id);
