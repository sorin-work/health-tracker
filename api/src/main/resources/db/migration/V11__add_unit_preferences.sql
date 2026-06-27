-- Values are always stored in metric units; these only change how the web app shows them.
alter table users add column unit_system  varchar(16) not null default 'METRIC';
alter table users add column glucose_unit varchar(16) not null default 'MG_DL';
