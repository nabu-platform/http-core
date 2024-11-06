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

import java.util.Date;

import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.LinkableHTTPResponse;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.Container;
import be.nabu.utils.mime.api.ModifiablePart;

public class DefaultHTTPResponse implements LinkableHTTPResponse {
	private int code;
	private String message, protocol;
	private ModifiablePart content;
	private double version;
	private HTTPRequest request;
	private Date created = new Date();
	
	public DefaultHTTPResponse(HTTPRequest request, int code, String message, ModifiablePart content, double version) {
		this("HTTP", request, code, message, content, version);
	}
	
	public DefaultHTTPResponse(String protocol, HTTPRequest request, int code, String message, ModifiablePart content, double version) {
		this.protocol = protocol.toUpperCase();
		this.request = request;
		this.code = code;
		this.message = message;
		this.content = content;
		this.version = version;
	}
	
	public DefaultHTTPResponse(HTTPRequest request, int code, String message, ModifiablePart content) {
		this(request, code, message, content, 1.1);
	}
	
	public DefaultHTTPResponse(int code, String message, ModifiablePart content) {
		this(null, code, message, content, 1.1);
	}
	public DefaultHTTPResponse(int code, String message, ModifiablePart content, double version) {
		this(null, code, message, content, version);
	}
	@Override
	public int getCode() {
		return code;
	}
	@Override
	public String getMessage() {
		return message;
	}
	@Override
	public ModifiablePart getContent() {
		return content;
	}
	@Override
	public double getVersion() {
		return version;
	}
	@Override
	public String toString() {
		HTTPFormatter formatter = new HTTPFormatter();
		Container<ByteBuffer> target = IOUtils.newByteBuffer();
		try {
			formatter.formatResponse(this, target);
			return new String(IOUtils.toBytes(target), "UTF-8");
		}
		catch (Exception e) {
			return e.getMessage() + ": " + super.toString();
		}
	}
	
	@Override
	public HTTPRequest getRequest() {
		return request;
	}
	@Override
	public void setRequest(HTTPRequest request) {
		this.request = request;
	}

	public Date getCreated() {
		return created;
	}

	public String getProtocol() {
		return protocol;
	}

}
