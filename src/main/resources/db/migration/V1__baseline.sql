create table if not exists store (
    id uuid primary key,
    slug varchar(120) not null unique,
    name varchar(160) not null,
    cnpj varchar(20) not null,
    phone varchar(20) not null,
    status varchar(20) not null,
    timezone varchar(60) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table if not exists identity_user (
    id uuid primary key,
    store_id uuid null,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    status varchar(20) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_identity_user_store
        foreign key (store_id) references store (id)
);

create table if not exists identity_user_role (
    id uuid primary key,
    user_id uuid not null,
    store_id uuid null,
    role_name varchar(30) not null,
    created_at timestamp not null default current_timestamp,
    constraint fk_identity_user_role_user
        foreign key (user_id) references identity_user (id),
    constraint fk_identity_user_role_store
        foreign key (store_id) references store (id),
    constraint uk_identity_user_role unique (user_id, role_name, store_id)
);

create table if not exists store_hours (
    id uuid primary key,
    store_id uuid not null,
    day_of_week varchar(20) not null,
    open_time time null,
    close_time time null,
    is_closed boolean not null default false,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_store_hours_store
        foreign key (store_id) references store (id)
);

create table if not exists delivery_zone (
    id uuid primary key,
    store_id uuid not null,
    zone_name varchar(120) not null,
    fee_amount numeric(12, 2) not null,
    min_order_amount numeric(12, 2) not null,
    active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_delivery_zone_store
        foreign key (store_id) references store (id),
    constraint uk_delivery_zone_store_zone unique (store_id, zone_name)
);

create table if not exists store_terms_acceptance (
    id uuid primary key,
    store_id uuid not null,
    accepted_by_user_id uuid not null,
    document_type varchar(40) not null,
    document_version varchar(40) not null,
    accepted_at timestamp not null,
    created_at timestamp not null default current_timestamp,
    constraint fk_store_terms_acceptance_store
        foreign key (store_id) references store (id),
    constraint fk_store_terms_acceptance_user
        foreign key (accepted_by_user_id) references identity_user (id)
);

create index if not exists idx_identity_user_store_id on identity_user (store_id);
create index if not exists idx_identity_user_role_user_id on identity_user_role (user_id);
create index if not exists idx_identity_user_role_store_id on identity_user_role (store_id);
create index if not exists idx_store_hours_store_id on store_hours (store_id);
create index if not exists idx_delivery_zone_store_id on delivery_zone (store_id);
create index if not exists idx_store_terms_acceptance_store_id on store_terms_acceptance (store_id);
