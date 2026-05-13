-- Day schedules, adherence and exports all look up a user's doses by time.
create index dose_logs_user_time_idx on dose_logs (user_id, scheduled_at);
