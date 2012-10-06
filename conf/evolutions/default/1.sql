# --- !Ups

create table project (
  id                        bigint not null primary key,
  name                      varchar(255) not null
);

create table profile (
  id                        bigint not null primary key,
  project_id                bigint not null,
  name                      varchar(255) not null,
  foreign key(project_id)   references project(id) on delete cascade
);

create table tag (
  id                        bigint not null primary key,
  project_id                bigint not null,
  name                      varchar(255) not null,
  foreign key(project_id)   references project(id) on delete cascade
);

create table jar_file (
  id                        bigint not null primary key,
  project_id                bigint not null,
  filename                  varchar(255) not null,
  foreign key(project_id)   references project(id) on delete cascade
);

# --- !Downs

drop table if exists jar_file;
drop table if exists tag;
drop table if exists profile;
drop table if exists project;
