alter table outbox_event
    add column dedup_key varchar(160);

create unique index ux_outbox_event_dedup_key
    on outbox_event (dedup_key);
