-- DAILY: every day. EVERY_N_DAYS: every interval_days days counted from start_date.
-- WEEKLY: on the days listed in days_of_week (e.g. 'MONDAY,THURSDAY').
alter table medications add column schedule_type varchar(16) not null default 'DAILY';
alter table medications add column interval_days integer check (interval_days between 2 and 90);
alter table medications add column days_of_week varchar(80);
