create table if not exists ftp_user(
    id serial primary key,
    username varchar(255) not null,
    password varchar (255) not null,
    enabled bool default false,
    admin bool default false
);