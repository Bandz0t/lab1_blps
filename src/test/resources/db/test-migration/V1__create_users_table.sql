create table users (
    id bigserial primary key,
    username varchar(100) not null unique,
    email varchar(150) not null unique,
    full_name varchar(150) not null,
    channel_name varchar(150) not null,
    payment_account varchar(255),
    created_at timestamp not null
);
