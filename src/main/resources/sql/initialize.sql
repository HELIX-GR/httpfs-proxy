
create table public."user" (
   "_id" bigint not null,
    "email" varchar(255) not null,
    "fullname" varchar(255),
    "username" varchar(255) not null,
    "hdfs_username" varchar(255) not null,
    "active" boolean,
    "blocked" boolean,
    "password" varchar(255),
    "registered_at" timestamp,
    "token" varchar(255),
    primary key ("_id")
);

create table public."user_role" (
   "_id" bigint not null,
    "role" varchar(24),
    "member" bigint,
    primary key ("_id")
);

alter table public."user" add constraint "uq_user_username" unique ("username");

alter table public."user_role" add constraint "uq_user_role_member_role" unique ("member", "role");

create sequence "user_id_seq" start with 1 increment by 1;

create sequence "user_role_id_seq" start with 1 increment by 1;

alter table public."user_role" add constraint "fk_user_role_member" 
    foreign key ("member") references public."user";
