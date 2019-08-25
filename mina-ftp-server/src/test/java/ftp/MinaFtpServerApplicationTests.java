package ftp;

import org.apache.ftpserver.ftplet.UserManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;

@SpringBootTest
class MinaFtpServerApplicationTests {

	@Autowired
	UserManager userManager;

	@Test
	void contextLoads() throws Exception {
		this.userManager.save(new FtpUser("jlong", "pw", true,
			Collections.emptyList(), -1, null));
	}


}
