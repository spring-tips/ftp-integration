package integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.DefaultSessionFactoryLocator;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.dsl.Ftp;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.function.Function;

import static org.springframework.web.servlet.function.RouterFunctions.route;

@SpringBootApplication
public class IntegrationApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(IntegrationApplication.class, args);
	}
}

@Log4j2
@Configuration
@Profile("delegating-session-factory")
class DelegatingSessionFactoryOutboundFlowConfiguration {

	private final static String HEADER_SESSION_FACTORY_KEY = "headerKey";
	private final static String SF = "sessionFactory";
	private final static String SF_JOSH = SF + "josh";
	private final static String SF_GARY = SF + "gary";

	@Bean
	RouterFunction<ServerResponse> httpRoutes() {
		var channel = in();
		var sessionFactoryNameParamName = "sfn";
		return route()
			.POST("/put/{" + sessionFactoryNameParamName + "}", request -> {
				var sessionFactoryName = request.pathVariable(sessionFactoryNameParamName);
				var msg = MessageBuilder
					.withPayload("Hello, " + sessionFactoryName + "!")
					.setHeaderIfAbsent(HEADER_SESSION_FACTORY_KEY, SF + (sessionFactoryName.toLowerCase()))
					.build();
				var sent = channel.send(msg);
				Assert.isTrue(sent, "the message should've been published");
				return ServerResponse.ok().build();
			})
			.build();
	}

	@Bean
	MessageChannel in() {
		return MessageChannels.direct().get();
	}

	@Bean
	IntegrationFlow ftpGatewayFlow(FtpRemoteFileTemplate ftpRemoteFileTemplate,
	                               DelegatingSessionFactory<FTPFile> dsf) {
		return f -> f
			.channel(in())
			.handle((GenericHandler<Object>) (payload, headers) -> {
				Object key = headers.get(HEADER_SESSION_FACTORY_KEY);
				dsf.setThreadKey(key);
				return key;
			})
			.handle(
				Ftp
					.outboundGateway(ftpRemoteFileTemplate, AbstractRemoteFileOutboundGateway.Command.PUT, "payload")
					.options(AbstractRemoteFileOutboundGateway.Option.RECURSIVE)
					.fileExistsMode(FileExistsMode.IGNORE )
			)
			.handle((GenericHandler<Object>) (payload, headers) -> {
				dsf.clearThreadKey();
				return null;
			});
	}

	@Bean
	FtpRemoteFileTemplate template(DelegatingSessionFactory dsf) {
		FtpRemoteFileTemplate template = new FtpRemoteFileTemplate(dsf);
		template.setRemoteDirectoryExpression(new LiteralExpression(""));
		return template;
	}

	@Bean
	DelegatingSessionFactory<FTPFile> dsf(
		@Qualifier(SF_GARY) DefaultFtpSessionFactory gary,
		@Qualifier(SF_JOSH) DefaultFtpSessionFactory josh) {
		Map<Object, SessionFactory<FTPFile>> sessionFactories = Map.of(SF_GARY, gary, SF_JOSH, josh);
		DefaultSessionFactoryLocator<FTPFile> sfl = new DefaultSessionFactoryLocator<>(sessionFactories);
		return new DelegatingSessionFactory<>(sfl);
	}

	private DefaultFtpSessionFactory sessionFactory(String username, String password, String host, int port) {
		DefaultFtpSessionFactory ftp = new DefaultFtpSessionFactory();
		ftp.setHost(host);
		ftp.setPort(port);
		ftp.setPassword(password);
		ftp.setUsername(username);
		return ftp;
	}

	@Bean
	@Qualifier(SF_GARY)
	DefaultFtpSessionFactory garyFtpSessionFactory(
		@Value("${ftp2.port}") int port,
		@Value("${ftp2.username}") String user,
		@Value("${ftp2.password}") String pw,
		@Value("${ftp2.host}") String host) {
		return this.sessionFactory(user, pw, host, port);
	}

	@Bean
	@Qualifier(SF_JOSH)
	DefaultFtpSessionFactory joshFtpSessionFactory(
		@Value("${ftp1.port}") int port,
		@Value("${ftp1.username}") String user,
		@Value("${ftp1.password}") String pw,
		@Value("${ftp1.host}") String host) {
		return this.sessionFactory(user, pw, host, port);
	}
}


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

abstract class FileUtils {

	public static File createDesktopFolder(String folder) {
		var home = System.getProperty("user.home");
		return new File(new File(home, "Desktop"), folder);
	}

	public static void ensureDirectoryExists(File file) {
		Assert.isTrue(file.exists() || file.mkdirs(), "the directory " + file.getAbsolutePath() + " must exist.");
	}
}

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