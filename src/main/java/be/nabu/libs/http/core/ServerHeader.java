/*
* Copyright (C) 2015 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.libs.http.core;

import be.nabu.libs.http.api.server.CustomHeader;

public enum ServerHeader implements CustomHeader {
	
	REMOTE_USER("X-Remote-User", false),
	REMOTE_HOST("X-Remote-Host", false),
	REMOTE_ADDRESS("X-Remote-Address", false),
	REMOTE_PORT("X-Remote-Port", false),
	REMOTE_IS_LOCAL("X-Remote-Is-Local", false),
	REQUEST_PROTOCOL("X-Request-Protocol", false),
	REQUEST_URI("X-Request-URI", false),
	RESOURCE_URI("X-Resource-URI", true), // set to true because it is injected by the parser _before_ it is processed and such headers are removed. the parser makes sure we have no user values here
	REQUEST_SECURITY("X-Request-Security", false),
	AUTHENTICATION_SCHEME("X-Authentication-Scheme", false),
	LOCAL_PORT("X-Local-Port", false),
	REQUEST_RELATIVE_URI("X-Request-Relative-URI", false),
	REQUEST_RECEIVED("X-Request-Received", true), // set to true because it is injected by the parser _before_ it is processed and such headers are removed. the parser makes sure we have no user values here
	// possible values: ssr (stands for server side rendering)
	REQUEST_TYPE("X-Request-Type", false),
	// the proxy may have its own additional path to expose the application
	PROXY_PATH("X-Proxy-Path", false);
	
	public static final String NAME_REMOTE_USER = "X-Remote-User";
	public static final String NAME_REMOTE_HOST = "X-Remote-Host";
	public static final String NAME_REMOTE_ADDRESS = "X-Remote-Address";
	public static final String NAME_REMOTE_PORT = "X-Remote-Port";
	public static final String NAME_REMOTE_IS_LOCAL = "X-Remote-Is-Local";
	public static final String NAME_REQUEST_URI = "X-Request-URI";
	public static final String NAME_REQUEST_PROTOCOL = "X-Request-Protocol";
	public static final String NAME_RESOURCE_URI = "X-Resource-URI";
	public static final String NAME_REQUEST_RELATIVE_URI = "X-Request-Relative-URI";
	public static final String NAME_AUTHENTICATION_SCHEME = "X-Authentication-Scheme";
	public static final String NAME_REQUEST_SECURITY = "X-Request-Security";
	public static final String NAME_LOCAL_PORT = "X-Local-Port";
	public static final String NAME_REQUEST_RECEIVED = "X-Request-Received";
	public static final String NAME_REQUEST_TYPE = "X-Request-Type";
	public static final String NAME_PROXY_PATH = "X-Proxy-Path";
	
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
