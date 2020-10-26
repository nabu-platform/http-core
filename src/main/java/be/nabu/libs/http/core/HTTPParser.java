package be.nabu.libs.http.core;

import java.io.IOException;
import java.text.ParseException;

import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.server.HTTPExpectContinueHandler;
import be.nabu.libs.resources.api.ContextualDynamicResourceProvider;
import be.nabu.libs.resources.api.DynamicResourceProvider;
import be.nabu.libs.resources.api.LocatableResource;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.containers.chars.ReadableStraightByteToCharContainer;
import be.nabu.utils.mime.api.ExpectContinueHandler;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.impl.MimeParser;
import be.nabu.utils.mime.impl.MimeUtils;

public class HTTPParser {
	
	private boolean isBlocking;
	private DynamicResourceProvider dynamicResourceProvider;
	
	public HTTPParser(DynamicResourceProvider dynamicResourceProvider, boolean isBlocking) {
		this.dynamicResourceProvider = dynamicResourceProvider;
		this.isBlocking = isBlocking;
	}
	
	public HTTPRequest parseRequest(ReadableContainer<ByteBuffer> container, ExpectContinueHandler expectContinueHandler) throws ParseException, IOException {
		return parseRequest(container, expectContinueHandler, "HTTP");
	}
	
	/**
	 * 
	 * @param container
	 * @param expectContinueHandler Pass along a HTTPExpectContinueHandler if you want access to the request line
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 */
	@SuppressWarnings({ "unchecked", "resource" })
	public HTTPRequest parseRequest(ReadableContainer<ByteBuffer> container, ExpectContinueHandler expectContinueHandler, String protocol) throws ParseException, IOException {		
		ReadableContainer<CharBuffer> charContainer = new ReadableStraightByteToCharContainer(container);
		charContainer = IOUtils.delimit(charContainer, "\n");
		String request = IOUtils.toString(charContainer).trim();
		// if there is no request, might be a leftover (like a final linefeed) from a previous request that was parsed/sent incorrectly
		if (request.isEmpty())
			return null;
		// the first space delimits the method
		int firstSpaceIndex = request.indexOf(' ');
		int protocolIndex = request.lastIndexOf(protocol + "/");
		
		String method = request.substring(0, firstSpaceIndex);
		// make sure multiple slashes are replaced by a single one
		String target = request.substring(firstSpaceIndex + 1, protocolIndex).trim().replaceFirst("[/]{2,}", "/");
		double version = new Double(request.substring(protocolIndex).replaceFirst(protocol + "/", "").trim());

		MimeParser parser = new MimeParser();
		// if the IO is blocking, we need to know the content length
		// otherwise we might end up with a blocking read due to unknown lengths
		parser.setRequireKnownContentLength(isBlocking);
		if (expectContinueHandler != null) {
			if (expectContinueHandler instanceof HTTPExpectContinueHandler)
				((HTTPExpectContinueHandler) expectContinueHandler).setRequestLine(method, target);
			parser.setExpectContinueHandler(expectContinueHandler);
		}
		ReadableResource dynamicResource = dynamicResourceProvider instanceof ContextualDynamicResourceProvider
			? ((ContextualDynamicResourceProvider<String>) dynamicResourceProvider).createDynamicResource(target, container, protocol.toLowerCase() + "-request", "application/octet-stream", false)
			: dynamicResourceProvider.createDynamicResource(container, protocol.toLowerCase() + "-request", "application/octet-stream", false);
		ModifiablePart content = MimeUtils.wrapModifiable(parser.parse(dynamicResource));
		if (dynamicResource instanceof LocatableResource) {
			HTTPUtils.setHeader(content, ServerHeader.RESOURCE_URI, ((LocatableResource) dynamicResource).getUri().toString());
		}
		return new DefaultHTTPRequest(protocol, method, target, content, version);
	}

	public boolean isBlocking() {
		return isBlocking;
	}
	
	public HTTPResponse parseResponse(ReadableContainer<ByteBuffer> container) throws IOException, ParseException {
		return parseResponse(container, "HTTP");
	}
	
	@SuppressWarnings("unchecked")
	public HTTPResponse parseResponse(ReadableContainer<ByteBuffer> container, String protocol) throws IOException, ParseException {
		ReadableContainer<CharBuffer> charContainer = new ReadableStraightByteToCharContainer(container);
		ReadableContainer<CharBuffer> delimitedCharContainer = IOUtils.delimit(charContainer, "\n");
		String request = IOUtils.toString(delimitedCharContainer).trim();
		// the first space delimits the method
		int firstSpaceIndex = request.indexOf(' ');
		if (firstSpaceIndex < 0)
			throw new ParseException("Could not parse response line: " + request, 0);
		int secondSpaceIndex = request.indexOf(' ', firstSpaceIndex + 1);
		double version = new Double(request.substring(0, firstSpaceIndex).replaceFirst(protocol + "/", "").trim());
		int code = new Integer(secondSpaceIndex < 0 ? request.substring(firstSpaceIndex + 1).trim() : request.substring(firstSpaceIndex + 1, secondSpaceIndex).trim());
		String message;
		if (secondSpaceIndex >= 0) {
			message = request.substring(secondSpaceIndex + 1);
		}
		else {
			message = HTTPCodes.getMessage(code);
		}
		
		// now that we have parsed the HTTP start, we need to check if there is any content and if so parse it
		CharBuffer buffer = IOUtils.newCharBuffer(2, false);
		charContainer.read(buffer);
		String readChars = IOUtils.toString(buffer);
		HTTPResponse response = null;
		if (readChars.equals("\n") || readChars.equals("\r\n"))
			response = new DefaultHTTPResponse(protocol, null, code, message, null, version);
		else {
			MimeParser parser = new MimeParser();
			parser.setRequireKnownContentLength(isBlocking);
			// for responses this is allowed
			parser.setAllowNoMessageSizeForClosedConnections(true);
			ReadableResource dynamicResource = dynamicResourceProvider instanceof ContextualDynamicResourceProvider
				? ((ContextualDynamicResourceProvider<Integer>) dynamicResourceProvider).createDynamicResource(code, IOUtils.chain(false, IOUtils.wrap(readChars.getBytes("ASCII"), true), container), protocol.toLowerCase() + "-response", "application/octet-stream", false)
				: dynamicResourceProvider.createDynamicResource(IOUtils.chain(false, IOUtils.wrap(readChars.getBytes("ASCII"), true), container), protocol.toLowerCase() + "-response", "application/octet-stream", false);
			ModifiablePart content = MimeUtils.wrapModifiable(parser.parse(dynamicResource));
			if (dynamicResource instanceof LocatableResource) {
				HTTPUtils.setHeader(content, ServerHeader.RESOURCE_URI, ((LocatableResource) dynamicResource).getUri().toString());
			}
			response = new DefaultHTTPResponse(protocol, null, code, message, content, version);
		}
		return response;
	}
	
}
