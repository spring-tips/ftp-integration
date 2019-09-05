package integration;

import org.springframework.util.Assert;

import java.io.File;

abstract class FileUtils {

	public static File createDesktopFolder(String folder) {
		var home = System.getProperty("user.home");
		return new File(new File(home, "Desktop"), folder);
	}

	public static void ensureDirectoryExists(File file) {
		Assert.isTrue(file.exists() || file.mkdirs(), "the directory " + file.getAbsolutePath() + " must exist.");
	}
}
