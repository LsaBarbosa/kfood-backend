create index if not exists idx_store_terms_acceptance_store_doc
    on store_terms_acceptance (store_id, document_type);

create index if not exists idx_store_terms_acceptance_store_accepted_at
    on store_terms_acceptance (store_id, accepted_at desc);

create index if not exists idx_delivery_zone_store_active_zone_name
    on delivery_zone (store_id, active, zone_name);
