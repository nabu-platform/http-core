package be.nabu.libs.http.core;

import java.net.URI;
import java.util.Date;

import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.Container;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.impl.MimeHeader;

public class DefaultHTTPRequest implements HTTPRequest {
	
	private String method, target, protocol;
	private double version;
	private ModifiablePart content;
	private Date created = new Date();
	
	public static DefaultHTTPRequest newRequest(String method, URI uri, ModifiablePart content) {
		DefaultHTTPRequest request = new DefaultHTTPRequest(method, uri.getPath(), content);
		content.setHeader(new MimeHeader("Host", uri.getAuthority()));
		return request;
	}
	
	public DefaultHTTPRequest(String method, String target, ModifiablePart content) {
		this(method, target, content, 1.1);
	}
	
	public DefaultHTTPRequest(String method, String target, ModifiablePart content, double version) {
		this("HTTP", method, target, content, version);
	}
	
	public DefaultHTTPRequest(String protocol, String method, String target, ModifiablePart content, double version) {
		this.protocol = protocol.toUpperCase();
		this.method = method.toUpperCase();
		this.target = target;
		this.version = version;
		this.content = content;
	}
	@Override
	public String getMethod() {
		return method;
	}
	@Override
	public double getVersion() {
		return version;
	}
	@Override
	public String getTarget() {
		return target;
	}
	@Override
	public ModifiablePart getContent() {
		return content;
	}
	
	@Override
	public String toString() {
		HTTPFormatter formatter = new HTTPFormatter();
		Container<ByteBuffer> target = IOUtils.newByteBuffer();
		try {
			formatter.formatRequest(this, target);
			return new String(IOUtils.toBytes(target), "UTF-8");
		}
		catch (Exception e) {
			return super.toString();
		}
	}

	public Date getCreated() {
		return created;
	}

	@Override
	public String getProtocol() {
		return protocol;
	}
	
}
