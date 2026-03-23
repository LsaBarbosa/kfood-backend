create table outbox_event (
    id uuid primary key,
    aggregate_type varchar(80) not null,
    aggregate_id varchar(80) not null,
    event_type varchar(120) not null,
    routing_key varchar(120) not null,
    payload text not null,
    publication_status varchar(30) not null,
    attempts integer not null default 0,
    last_error varchar(500),
    created_at timestamp with time zone not null default current_timestamp,
    published_at timestamp with time zone
);

create index idx_outbox_event_status_created_at
    on outbox_event (publication_status, created_at);

create index idx_outbox_event_aggregate
    on outbox_event (aggregate_type, aggregate_id);
