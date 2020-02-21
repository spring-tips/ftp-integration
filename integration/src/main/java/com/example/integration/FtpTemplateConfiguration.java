package com.example.integration;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;

import java.io.File;
import java.io.FileOutputStream;

//@Profile("template")
@Log4j2
@Configuration
class FtpTemplateConfiguration {

	@Bean
	InitializingBean initializingBean(FtpRemoteFileTemplate template) {
		return () -> template
			.execute(session -> {
				var file = new File(new File(System.getProperty("user.home"), "Desktop"), "hello-local.txt");
				try (var fout = new FileOutputStream(file)) {
					session.read("hello.txt", fout);
				}
				log.info("read " + file.getAbsolutePath());
				return null;
			});
	}

	@Bean
	DefaultFtpSessionFactory defaultFtpSessionFactory(
		@Value("${ftp1.username}") String username,
		@Value("${ftp1.password}") String pw,
		@Value("${ftp1.host}") String host,
		@Value("${ftp1.port}") int port) {
		DefaultFtpSessionFactory defaultFtpSessionFactory = new DefaultFtpSessionFactory();
		defaultFtpSessionFactory.setPassword(pw);
		defaultFtpSessionFactory.setUsername(username);
		defaultFtpSessionFactory.setHost(host);
		defaultFtpSessionFactory.setPort(port);
		return defaultFtpSessionFactory;
	}

	@Bean
	FtpRemoteFileTemplate ftpRemoteFileTemplate(DefaultFtpSessionFactory dsf) {
		return new FtpRemoteFileTemplate(dsf);
	}
}
