package ftp;

import lombok.extern.log4j.Log4j2;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Configuration
class FtpServerConfiguration {

	@Bean
	FileSystemFactory fileSystemFactory() {
		NativeFileSystemFactory fileSystemFactory = new NativeFileSystemFactory();
		fileSystemFactory.setCreateHome(true);
		fileSystemFactory.setCaseInsensitive(false);
		return fileSystemFactory::createFileSystemView;
	}

	@Bean
	Listener nioListener(@Value("${ftp.port:7777}") int port) {
		ListenerFactory listenerFactory = new ListenerFactory();
		listenerFactory.setPort(port);
		return listenerFactory.createListener();
	}

	@Bean
	FtpServer ftpServer(Map<String, Ftplet> ftpletMap, UserManager userManager, Listener nioListener, FileSystemFactory fileSystemFactory) {
		FtpServerFactory ftpServerFactory = new FtpServerFactory();
		ftpServerFactory.setListeners(Collections.singletonMap("default", nioListener));
		ftpServerFactory.setFileSystem(fileSystemFactory);
		ftpServerFactory.setFtplets(ftpletMap);
		ftpServerFactory.setUserManager(userManager);
		return ftpServerFactory.createServer();
	}

	@Bean
	DisposableBean destroysFtpServer(FtpServer ftpServer) {
		return ftpServer::stop;
	}

	@Bean
	InitializingBean startsFtpServer(FtpServer ftpServer) {
		return ftpServer::start;
	}

	@Bean
	UserManager userManager(@Value("${ftp.root:${HOME}/Desktop/root}") File root, JdbcTemplate template) {
		Assert.isTrue(root.exists() || root.mkdirs(), "the root directory must exist.");
		return new FtpUserManager(root, template);
	}
}
