-- Free-form tags on symptom entries ("after coffee", "stress", "weather") to look for triggers.
create table symptom_tags (
    symptom_id bigint      not null references symptoms (id) on delete cascade,
    tag        varchar(30) not null,
    primary key (symptom_id, tag)
);

create index symptom_tags_tag_idx on symptom_tags (tag);
