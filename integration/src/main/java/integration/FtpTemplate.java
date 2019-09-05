package integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;

@Profile("template")
@Log4j2
@Configuration
class FtpTemplate {

	@Bean
	FtpRemoteFileTemplate template(DefaultFtpSessionFactory ftpSessionFactory) {
		return new FtpRemoteFileTemplate(ftpSessionFactory);
	}

	@Bean
	DefaultFtpSessionFactory ftpSessionFactory(@Value("${ftp1.port}") int port,
	                                           @Value("${ftp1.username}") String user,
	                                           @Value("${ftp1.password}") String pw,
	                                           @Value("${ftp1.host}") String host) {
		DefaultFtpSessionFactory ftp = new DefaultFtpSessionFactory();
		ftp.setHost(host);
		ftp.setPort(port);
		ftp.setPassword(pw);
		ftp.setUsername(user);
		return ftp;
	}

	@Component
	@Profile("template")
	@RequiredArgsConstructor
	public static class Runner {

		private final FtpRemoteFileTemplate ftpRemoteFileTemplate;

		@EventListener(ApplicationReadyEvent.class)
		public void ready() {
			File newLocalFile = this.ftpRemoteFileTemplate
				.execute(session -> {
					var file = new File(FileUtils.createDesktopFolder("template-local"), "my-local-file.txt");
					FileUtils.ensureDirectoryExists(file);
					try (var fileOutputStream = new FileOutputStream(file)) {
						session.read("a-file.txt", fileOutputStream);
					}
					return file;
				});
			log.info("remote file saved to " + newLocalFile.getAbsolutePath());
		}

	}
}
