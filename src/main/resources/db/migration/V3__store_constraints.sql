alter table store
    alter column status set default 'SETUP';

alter table store
    add constraint chk_store_status
        check (status in ('SETUP', 'ACTIVE', 'SUSPENDED'));

create index if not exists idx_store_status on store (status);
create index if not exists idx_store_cnpj on store (cnpj);
