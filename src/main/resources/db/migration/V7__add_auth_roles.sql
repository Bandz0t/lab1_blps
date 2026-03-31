alter table users
    add column if not exists password_hash varchar(255) not null default '{noop}change-me',
    add column if not exists role varchar(30) not null default 'AUTHOR';

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
