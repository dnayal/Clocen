# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table beta_user (
  email                     varchar(100) not null,
  invite_email_sent         tinyint(1) default 0,
  registered                tinyint(1) default 0,
  create_timestamp          datetime,
  constraint pk_beta_user primary key (email))
;

create table process (
  process_id                varchar(100) not null,
  user_id                   varchar(100),
  version                   varchar(10),
  trigger_node              varchar(25),
  trigger_type              varchar(10),
  process_data              text,
  paused                    tinyint(1) default 0,
  create_timestamp          datetime,
  constraint pk_process primary key (process_id))
;

create table service_access_token (
  user_id                   varchar(100),
  node_id                   varchar(20),
  access_token              varchar(100),
  refresh_token             varchar(100),
  expiration_time           datetime,
  create_timestamp          datetime,
  constraint pk_service_access_token primary key (user_id, node_id))
;

create table user (
  user_id                   varchar(100) not null,
  name                      varchar(100),
  email                     varchar(100),
  password                  varchar(100),
  country                   varchar(100),
  role                      varchar(20),
  create_timestamp          datetime,
  constraint uq_user_email unique (email),
  constraint pk_user primary key (user_id))
;




# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table beta_user;

drop table process;

drop table service_access_token;

drop table user;

SET FOREIGN_KEY_CHECKS=1;

