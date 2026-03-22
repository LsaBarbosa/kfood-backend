alter table store
    add column if not exists cash_payment_enabled boolean not null default true;
