alter table sales_order
    add column if not exists delivery_address_label varchar(60);

alter table sales_order
    add column if not exists delivery_address_zip_code varchar(8);

alter table sales_order
    add column if not exists delivery_address_street varchar(160);

alter table sales_order
    add column if not exists delivery_address_number varchar(20);

alter table sales_order
    add column if not exists delivery_address_district varchar(100);

alter table sales_order
    add column if not exists delivery_address_city varchar(100);

alter table sales_order
    add column if not exists delivery_address_state varchar(2);

alter table sales_order
    add column if not exists delivery_address_complement varchar(120);
