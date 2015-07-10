package be.nabu.libs.http.core;

import java.io.IOException;

import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.mime.impl.FormatException;
import be.nabu.utils.mime.impl.MimeFormatter;

public class HTTPFormatter {
	
	private MimeFormatter formatter;
	
	public HTTPFormatter() {
		this.formatter = new MimeFormatter();
		this.formatter.setIncludeMainContentTrailingLineFeeds(false);
		this.formatter.setAllowBinary(true);
		// ignore internal headers
		for (ServerHeader serverHeader : ServerHeader.values()) {
			this.formatter.ignoreHeaders(serverHeader.getName());
		}
	}
	
	private void formatRequestLine(HTTPRequest request, WritableContainer<ByteBuffer> output) throws IOException {
		String firstLine = request.getMethod() + " " + request.getTarget() + " HTTP/" + request.getVersion() + "\r\n";
		output.write(IOUtils.wrap(firstLine.getBytes("ASCII"), true));
	}
	
	public void formatRequestHeaders(HTTPRequest request, WritableContainer<ByteBuffer> output) throws IOException, FormatException {
		formatRequestLine(request, output);
		if (request.getContent() == null)
			output.write(IOUtils.wrap("\r\n".getBytes(), true));
		else
			formatter.formatHeaders(request.getContent(), output);
	}
	public void formatRequestContent(HTTPRequest request, WritableContainer<ByteBuffer> output) throws IOException, FormatException {
		if (request.getContent() != null)
			formatter.formatContent(request.getContent(), output);
	}
	
	public void formatRequest(HTTPRequest request, WritableContainer<ByteBuffer> output) throws IOException, FormatException {
		formatRequestLine(request, output);
		if (request.getContent() == null)
			output.write(IOUtils.wrap("\r\n".getBytes(), true));
		else
			formatter.format(request.getContent(), output);
	}
	
	public void formatResponse(HTTPResponse response, WritableContainer<ByteBuffer> output) throws IOException, FormatException {
		String firstLine = "HTTP/" + response.getVersion() + " " + response.getCode() + " " + response.getMessage() + "\r\n";
		output.write(IOUtils.wrap(firstLine.getBytes("ASCII"), true));
		if (response.getContent() == null)
			output.write(IOUtils.wrap("\r\n".getBytes(), true));
		else
			formatter.format(response.getContent(), output);
	}
}
