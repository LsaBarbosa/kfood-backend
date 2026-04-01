alter table store
    add column if not exists cash_payment_enabled boolean not null default false;

alter table sales_order
    add column if not exists payment_method_snapshot varchar(20);

update sales_order
set payment_method_snapshot = payment_method
where payment_method_snapshot is null;

alter table sales_order
    alter column payment_method_snapshot set default 'PIX';

alter table sales_order
    alter column payment_method_snapshot set not null;
