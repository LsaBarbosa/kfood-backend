alter table identity_user
    add constraint chk_identity_user_status
        check (status in ('ACTIVE', 'INACTIVE', 'LOCKED'));

alter table identity_user_role
    add constraint chk_identity_user_role_name
        check (role_name in ('OWNER', 'MANAGER', 'ATTENDANT', 'ADMIN'));
