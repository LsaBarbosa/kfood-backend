alter table store_terms_acceptance
    add column if not exists request_ip varchar(45) not null default '0.0.0.0';
