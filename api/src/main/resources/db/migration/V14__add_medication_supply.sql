-- Optional supply tracking: how many units (tablets, puffs, ...) are left, how many one dose uses
-- and how many days ahead to warn about a refill. Null supply means it is not tracked.
alter table medications add column supply numeric(7, 1) check (supply >= 0);
alter table medications add column dose_quantity numeric(5, 2) not null default 1 check (dose_quantity > 0);
alter table medications add column refill_warning_days integer not null default 7 check (refill_warning_days between 0 and 90);
