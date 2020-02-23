# FTP Integration 

Hi, Spring fans! In this installment of Spring Tips, we look at a topic that's near and dear to my heart: integration! And yes, you may recall that the very first installment of _Spring Tips_ looked at Spring Integration. If you haven't already watched that one, [you should](https://www.youtube.com/watch?v=MTKlk8_9aAw&list=PLgGXSWYM2FpPw8rV0tZoMiJYSCiLhPnOc&index=69). So, while we're not going to revisit Spring Integration fundamentals, we're going to take a deep dive into one area fo support in Spring Integration: FTP. FTP is all about file synchronization. Broadly, in the world of Enterprise Application Integration (EAI), we have four types of integration: file synchronization, RPC, database synchronization, and messaging. 

File synchronization is definitely not what most people think of when they think of cloud native applications, but you'd be surprised just how much of the world of finance is run by file synchronization (FTP, SFTP, AS2, FTPS, NFS, SMB, etc.) integrations. Sure, most of them use the more secure variants, but the point is still valid. In this video we look at how to use Spring Integration's FTP support, and once you understand that, it's easy enought to apply it to other variants. 

Please indulge me in a bit of chest-thumping here: I thought that I knew everything I'd needed to know about Spring Integration's FTP support, since I had a major role in polishing off Iwein Fuld's original prototype code more than a decade ago, and since I contributed the original FTPS and SFTP adapters. In the intervening decade, surprising nobody, the Spring Integration team has added a _ton_ of new capabilities and fixed all the bugs in my original code! I love what's been introduced. 

So, first things first: we need to setup an FTP server. Most of Spring Integration's support works as a client to an already installed FTP server. So, it doesn't matter what FTP server you use. However, I'd recommend you use the [Apache FTPServer project](https://mina.apache.org/ftpserver-project/). It's a project that's a sub-project of the Apache Mina project, which is,  just so you know, the precursor to the Netty project. The Apache FTP Server is a super scalable, lightweight, all-Java implementation of the FTP protocol. And, you can easily embed it inside a Spring application. I've done so in the [Github repository for this video](http://github.com/spring-tips/ftp-integration). I defined a custom `UserManager` class to manage FTP user acounts. The custom `UserManager` that talks to a local PostgreSQL database with a simple table `ftp_user`, whose schema is defined thusly:

```sql
create table if not exists ftp_user(
    id serial primary key,
    username varchar(255) not null,
    password varchar (255) not null,
    enabled bool default false,
    admin bool default false
);
```

I've got two users in there, `jlong` and `grussell`, both of which have a password of `pw`. I've set `enabled` and `admin` to `true` for both records. We use these two accounts later, so make sure you insert them into the table, like this. 

```sql
insert into ftp_user(username, password, enabled, admin) values ('jlong', 'pw', true, true);
insert into ftp_user(username, password, enabled, admin) values ('grussell', 'pw', true, true);
```


