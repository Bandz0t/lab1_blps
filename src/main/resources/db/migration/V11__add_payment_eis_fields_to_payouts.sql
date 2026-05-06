alter table payouts add column external_payment_id varchar(100);
alter table payouts add column attempts integer not null default 0;
alter table payouts add column last_error text;

create index idx_payouts_external_payment_id on payouts(external_payment_id);
