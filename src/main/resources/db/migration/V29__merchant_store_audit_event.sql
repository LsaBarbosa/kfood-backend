create table merchant_store_audit_event (
    id uuid primary key,
    store_id uuid not null,
    actor_user_id uuid not null,
    event_type varchar(40) not null,
    entity_type varchar(40) not null,
    entity_id uuid not null,
    occurred_at timestamp with time zone not null,
    before_status varchar(20),
    after_status varchar(20),
    document_type varchar(40),
    document_version varchar(40),
    accepted_at timestamp with time zone,
    constraint fk_merchant_store_audit_event_store
        foreign key (store_id) references store (id),
    constraint fk_merchant_store_audit_event_actor_user
        foreign key (actor_user_id) references identity_user (id),
    constraint ck_merchant_store_audit_event_type_allowed
        check (event_type in ('LEGAL_TERMS_ACCEPTED', 'STORE_STATUS_CHANGED')),
    constraint ck_merchant_store_audit_entity_type_allowed
        check (entity_type in ('STORE', 'STORE_TERMS_ACCEPTANCE')),
    constraint ck_merchant_store_audit_before_status_allowed
        check (before_status is null or before_status in ('SETUP', 'ACTIVE', 'SUSPENDED')),
    constraint ck_merchant_store_audit_after_status_allowed
        check (after_status is null or after_status in ('SETUP', 'ACTIVE', 'SUSPENDED')),
    constraint ck_merchant_store_audit_document_type_allowed
        check (document_type is null or document_type in ('TERMS_OF_USE', 'PRIVACY_POLICY')),
    constraint ck_merchant_store_audit_document_version_not_blank
        check (document_version is null or char_length(trim(document_version)) > 0),
    constraint ck_merchant_store_audit_event_shape
        check (
            (event_type = 'LEGAL_TERMS_ACCEPTED'
                and entity_type = 'STORE_TERMS_ACCEPTANCE'
                and before_status is null
                and after_status is null
                and document_type is not null
                and document_version is not null
                and accepted_at is not null)
            or
            (event_type = 'STORE_STATUS_CHANGED'
                and entity_type = 'STORE'
                and before_status is not null
                and after_status is not null
                and document_type is null
                and document_version is null
                and accepted_at is null)
        )
);

create index ix_merchant_store_audit_event_store_occurred_at
    on merchant_store_audit_event (store_id, occurred_at asc);

create index ix_merchant_store_audit_event_entity
    on merchant_store_audit_event (entity_type, entity_id);
