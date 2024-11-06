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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.resources.URIUtils;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.mime.impl.FormatException;
import be.nabu.utils.mime.impl.HeaderEncoding;
import be.nabu.utils.mime.impl.MimeFormatter;

public class HTTPFormatter {
	
	private MimeFormatter formatter;
	
	public HTTPFormatter() {
		this(true);
	}
	
	public HTTPFormatter(boolean ignoreInternalHeaders) {
		this.formatter = new MimeFormatter();
		this.formatter.setHeaderEncoding(HeaderEncoding.RFC2231);
		this.formatter.setIncludeMainContentTrailingLineFeeds(false);
		this.formatter.setAllowBinary(true);
		// don't fold the header, IE does not support this
		this.formatter.setFoldHeader(false);
		if (ignoreInternalHeaders) {
			// ignore internal headers
			for (ServerHeader serverHeader : ServerHeader.values()) {
				this.formatter.ignoreHeaders(serverHeader.getName());
			}
		}
	}
	
	private void formatRequestLine(HTTPRequest request, WritableContainer<ByteBuffer> output) throws IOException {
		try {
			String firstLine = request.getMethod() + " " + new URI(URIUtils.encodeURI(request.getTarget(), false)).toASCIIString() + " " + request.getProtocol() + "/" + request.getVersion() + "\r\n";
			//		String firstLine = request.getMethod() + " " + URIUtils.encodeURI(request.getTarget(), false) + " " + request.getProtocol() + "/" + request.getVersion() + "\r\n";
			output.write(IOUtils.wrap(firstLine.getBytes("ASCII"), true));
		}
		catch (URISyntaxException e) {
			throw new IOException(e);
		}
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
		String firstLine = response.getProtocol() + "/" + response.getVersion() + " " + response.getCode() + " " + response.getMessage() + "\r\n";
		output.write(IOUtils.wrap(firstLine.getBytes("ASCII"), true));
		if (response.getContent() == null)
			output.write(IOUtils.wrap("\r\n".getBytes(), true));
		else
			formatter.format(response.getContent(), output);
	}

	public MimeFormatter getFormatter() {
		return formatter;
	}
	
}
