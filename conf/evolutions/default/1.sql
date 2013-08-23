# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table service_access_token (
  user_id                   varchar(100),
  node_id                   varchar(20),
  access_token              varchar(100),
  refresh_token             varchar(100),
  expiration_time           datetime,
  constraint pk_service_access_token primary key (user_id, node_id))
;

create table user (
  user_id                   varchar(100) not null,
  name                      varchar(100),
  email                     varchar(100),
  create_timestamp          datetime,
  constraint uq_user_email unique (email),
  constraint pk_user primary key (user_id))
;




# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table service_access_token;

drop table user;

SET FOREIGN_KEY_CHECKS=1;
