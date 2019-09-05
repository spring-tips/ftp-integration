package integration;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.ftp.dsl.Ftp;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;

import java.io.File;

@Log4j2
@Configuration
@Profile("simple-inbound-ftp")
class SimpleInboundFtpAdapterConfiguration {

	@Bean
	IntegrationFlow inboundFtpFlow(
		@Qualifier(FTP_SESSION_FACTORY) DefaultFtpSessionFactory sf,
		@Value("${ftp.local-directory}") File localDirectoryToSyncRemoteFilesHere) {

		var ftpSpec = Ftp
			.inboundAdapter(sf)
			.localDirectory(localDirectoryToSyncRemoteFilesHere)
			.autoCreateLocalDirectory(true);

		return IntegrationFlows
			.from(ftpSpec, pm -> pm.poller(pc -> pc.fixedRate(1000)))
			.handle(File.class, (file, messageHeaders) -> {
				log.info("there's a new file: " + file.getAbsolutePath() + ".");
				return null;
			})
			.get();
	}

	private final static String FTP_SESSION_FACTORY = "ftpSessionFactory";

	@Bean(FTP_SESSION_FACTORY)
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


}
