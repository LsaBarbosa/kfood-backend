alter table store
    add column if not exists category varchar(40);

alter table store
    add column if not exists address_zip_code varchar(8);

alter table store
    add column if not exists address_street varchar(160);

alter table store
    add column if not exists address_number varchar(20);

alter table store
    add column if not exists address_district varchar(100);

alter table store
    add column if not exists address_city varchar(100);

alter table store
    add column if not exists address_state varchar(2);

alter table store
    drop constraint if exists chk_store_category;

alter table store
    add constraint chk_store_category
        check (category is null or category in ('PIZZARIA'));
