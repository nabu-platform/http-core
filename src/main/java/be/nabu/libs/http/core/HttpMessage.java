package be.nabu.libs.http.core;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HttpMessage {
	private String message;
	private boolean partial;

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public boolean isPartial() {
		return partial;
	}
	public void setPartial(boolean partial) {
		this.partial = partial;
	}
}