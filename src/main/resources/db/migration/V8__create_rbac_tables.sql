create table roles (
    id bigserial primary key,
    name varchar(50) not null unique,
    description varchar(255)
);

create table privileges (
    id bigserial primary key,
    name varchar(80) not null unique,
    description varchar(255)
);

create table role_privileges (
    role_id bigint not null references roles(id) on delete cascade,
    privilege_id bigint not null references privileges(id) on delete cascade,
    primary key (role_id, privilege_id)
);

create table user_roles (
    user_id bigint not null references users(id) on delete cascade,
    role_id bigint not null references roles(id) on delete cascade,
    primary key (user_id, role_id)
);

insert into roles (name, description)
select 'AUTHOR', 'Video author'
where not exists (select 1 from roles where name = 'AUTHOR');

insert into roles (name, description)
select 'MODERATOR', 'Copyright moderator'
where not exists (select 1 from roles where name = 'MODERATOR');

insert into roles (name, description)
select 'ADMIN', 'System administrator'
where not exists (select 1 from roles where name = 'ADMIN');

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
