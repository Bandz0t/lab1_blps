insert into users (username, email, full_name, channel_name, payment_account, created_at)
select 'demo_author', 'demo@author.local', 'Demo Author', 'Demo Channel', 'demo-account-001', current_timestamp
where not exists (
    select 1 from users where username = 'demo_author'
);
