create sequence if not exists sales_order_number_seq
    start with 1
    increment by 1
    minvalue 1
    no maxvalue
    cache 1;

alter table sales_order
    alter column order_number set not null;
