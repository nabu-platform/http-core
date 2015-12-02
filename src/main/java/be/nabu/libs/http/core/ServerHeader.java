package be.nabu.libs.http.core;

import be.nabu.libs.http.api.server.CustomHeader;

public enum ServerHeader implements CustomHeader {
	
	REMOTE_USER("X-Remote-User", false),
	REMOTE_HOST("X-Remote-Host", false),
	REMOTE_ADDRESS("X-Remote-Address", false),
	REMOTE_PORT("X-Remote-Port", false),
	REMOTE_IS_LOCAL("X-Remote-Is-Local", false),
	REQUEST_URI("X-Request-URI", false),
	RESOURCE_URI("X-Resource-URI", true),
	REQUEST_SECURITY("X-Request-Security", false),
	AUTHENTICATION_SCHEME("X-Authentication-Scheme", false),
	REQUEST_RELATIVE_URI("X-Request-Relative-URI", false);
	
	public static final String NAME_REMOTE_USER = "X-Remote-User";
	public static final String NAME_REMOTE_HOST = "X-Remote-Host";
	public static final String NAME_REMOTE_ADDRESS = "X-Remote-Address";
	public static final String NAME_REMOTE_PORT = "X-Remote-Port";
	public static final String NAME_REMOTE_IS_LOCAL = "X-Remote-Is-Local";
	public static final String NAME_REQUEST_URI = "X-Request-URI";
	public static final String NAME_RESOURCE_URI = "X-Resource-URI";
	public static final String NAME_REQUEST_RELATIVE_URI = "X-Request-Relative-URI";
	public static final String NAME_AUTHENTICATION_SCHEME = "X-Authentication-Scheme";
	public static final String NAME_REQUEST_SECURITY = "X-Request-Security";
	
	private String name;
	private boolean isUserValueAllowed;

	private ServerHeader(String name, boolean isUserValueAllowed) {
		this.name = name;
		this.isUserValueAllowed = isUserValueAllowed;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean isUserValueAllowed() {
		return isUserValueAllowed;
	}
}
