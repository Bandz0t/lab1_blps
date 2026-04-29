alter table users
    add column if not exists password_hash varchar(255) not null default '{noop}change-me';

alter table users
    add column if not exists role varchar(30) not null default 'AUTHOR';

create table if not exists roles (
    id bigserial primary key,
    name varchar(50) not null unique,
    description varchar(255)
);

create table if not exists privileges (
    id bigserial primary key,
    name varchar(80) not null unique,
    description varchar(255)
);

create table if not exists role_privileges (
    role_id bigint not null references roles(id) on delete cascade,
    privilege_id bigint not null references privileges(id) on delete cascade,
    primary key (role_id, privilege_id)
);

create table if not exists user_roles (
    user_id bigint not null references users(id) on delete cascade,
    role_id bigint not null references roles(id) on delete cascade,
    primary key (user_id, role_id)
);

update users
set password_hash = '{noop}demo12345',
    role = 'AUTHOR'
where username = 'demo_author';

insert into users (username, email, full_name, channel_name, payment_account, password_hash, role, created_at)
select 'admin', 'admin@studio.local', 'System Admin', 'Platform Operations', 'ops-account-001', '{noop}admin12345', 'ADMIN', current_timestamp
where not exists (select 1 from users where username = 'admin');

insert into users (username, email, full_name, channel_name, payment_account, password_hash, role, created_at)
select 'moderator', 'moderator@studio.local', 'Content Moderator', 'Moderation Team', 'moderation-account-001', '{noop}moderator12345', 'MODERATOR', current_timestamp
where not exists (select 1 from users where username = 'moderator');

insert into roles (name, description)
select role_name, role_description
from (
    values
        ('AUTHOR', 'Video author'),
        ('MODERATOR', 'Copyright moderator'),
        ('ADMIN', 'System administrator')
) as r(role_name, role_description)
where not exists (select 1 from roles where name = role_name);

insert into privileges (name, description)
select privilege_name, privilege_name
from (
    values
        ('VIDEO_CREATE'),
        ('VIDEO_READ_OWN'),
        ('VIDEO_READ_ALL'),
        ('VIDEO_EDIT_OWN'),
        ('VIDEO_EDIT_ALL'),
        ('VIDEO_MONETIZE_OWN'),
        ('VIDEO_MONETIZE_ALL'),
        ('COPYRIGHT_REVIEW'),
        ('PROCESS_READ_OWN'),
        ('PROCESS_READ_ALL'),
        ('REVENUE_READ_OWN'),
        ('REVENUE_READ_ALL'),
        ('PAYOUT_READ_OWN'),
        ('PAYOUT_READ_ALL'),
        ('MONTHLY_PROCESS_RUN'),
        ('ADMIN_PANEL_ACCESS'),
        ('USER_READ_ALL')
) as p(privilege_name)
where not exists (select 1 from privileges where name = privilege_name);

insert into role_privileges (role_id, privilege_id)
select r.id, p.id
from roles r
join privileges p on p.name in (
    'VIDEO_CREATE',
    'VIDEO_READ_OWN',
    'VIDEO_EDIT_OWN',
    'VIDEO_MONETIZE_OWN',
    'PROCESS_READ_OWN',
    'REVENUE_READ_OWN',
    'PAYOUT_READ_OWN'
)
where r.name = 'AUTHOR'
  and not exists (
    select 1 from role_privileges rp where rp.role_id = r.id and rp.privilege_id = p.id
  );

insert into role_privileges (role_id, privilege_id)
select r.id, p.id
from roles r
join privileges p on p.name in (
    'VIDEO_READ_ALL',
    'COPYRIGHT_REVIEW',
    'PROCESS_READ_ALL'
)
where r.name = 'MODERATOR'
  and not exists (
    select 1 from role_privileges rp where rp.role_id = r.id and rp.privilege_id = p.id
  );

insert into role_privileges (role_id, privilege_id)
select r.id, p.id
from roles r
join privileges p on 1 = 1
where r.name = 'ADMIN'
  and not exists (
    select 1 from role_privileges rp where rp.role_id = r.id and rp.privilege_id = p.id
  );

insert into user_roles (user_id, role_id)
select u.id, r.id
from users u
join roles r on r.name = u.role
where not exists (
    select 1 from user_roles ur where ur.user_id = u.id and ur.role_id = r.id
);
