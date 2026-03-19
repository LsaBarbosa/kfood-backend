create table if not exists test_audit_entity (
    id bigserial primary key,
    name varchar(255) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
