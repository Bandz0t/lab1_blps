create table scheduler_locks (
    lock_name varchar(100) primary key,
    owner_id varchar(100) not null,
    locked_until timestamp not null,
    updated_at timestamp not null
);
