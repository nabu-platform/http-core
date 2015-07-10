package be.nabu.libs.http.core;

import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.Container;
import be.nabu.utils.mime.api.ModifiablePart;

public class DefaultHTTPResponse implements HTTPResponse {
	private int code;
	private String message;
	private ModifiablePart content;
	private double version;
	
	public DefaultHTTPResponse(int code, String message, ModifiablePart content) {
		this(code, message, content, 1.1);
	}
	public DefaultHTTPResponse(int code, String message, ModifiablePart content, double version) {
		this.code = code;
		this.message = message;
		this.content = content;
		this.version = version;
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
}
