package ftp;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class FtpUser implements User {

	private final String name, pw;
	private final boolean enabled;
	private final int maxIdleTime;
	private final List<Authority> authorities = new ArrayList<>();
	private final File homeDirectory;

	FtpUser(String name, String pw, boolean enabled, List<? extends Authority> auths, int maxIdleTime, File homeDirectory) {
		this.name = name;
		this.maxIdleTime = maxIdleTime == -1 ?
			60_000 : maxIdleTime;
		this.homeDirectory = homeDirectory;
		this.pw = pw;
		this.enabled = enabled;
		if (auths != null) {
			this.authorities.addAll(auths);
		}
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getPassword() {
		return this.pw;
	}

	@Override
	public List<? extends Authority> getAuthorities() {
		return this.authorities;
	}

	@Override
	public List<? extends Authority> getAuthorities(Class<? extends Authority> aClass) {
		return this.authorities.stream().filter(a -> a.getClass().isAssignableFrom(aClass)).collect(Collectors.toList());
	}

	@Override
	public AuthorizationRequest authorize(AuthorizationRequest req) {
		return this.getAuthorities()
			.stream()
			.filter(a -> a.canAuthorize(req))
			.map(a -> a.authorize(req))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	@Override
	public int getMaxIdleTime() {
		return this.maxIdleTime;
	}

	@Override
	public boolean getEnabled() {
		return this.enabled;
	}

	@Override
	public String getHomeDirectory() {
		return this.homeDirectory.getAbsolutePath();
	}
}
