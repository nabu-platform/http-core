package be.nabu.libs.http.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPEntity;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.client.ClientAuthenticationHandler;
import be.nabu.libs.http.api.server.AuthenticationHeader;
import be.nabu.libs.http.api.server.CustomHeader;
import be.nabu.libs.http.api.server.SecurityHeader;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.resources.api.FiniteResource;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.LimitedReadableContainer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.mime.api.ContentPart;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.api.ModifiableHeader;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.api.MultiPart;
import be.nabu.utils.mime.api.Part;
import be.nabu.utils.mime.impl.FormatException;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeContentPart;
import be.nabu.utils.mime.impl.PlainMimeEmptyPart;

public class HTTPUtils {

	public static final String PROXY_AUTHENTICATE_REQUEST = "Proxy-Authenticate";
	public static final String PROXY_AUTHENTICATE_RESPONSE = "Proxy-Authorization";
	public static final String SERVER_AUTHENTICATE_REQUEST = "WWW-Authenticate";
	public static final String SERVER_AUTHENTICATE_RESPONSE = "Authorization";
	
	private static ThreadLocal<SimpleDateFormat> formatter = new ThreadLocal<SimpleDateFormat>();

	public static HTTPRequest get(URI target, Header...headers) {
		List<Header> allHeaders = new ArrayList<Header>(Arrays.asList(headers));
		if (MimeUtils.getHeader("Content-Length", headers) == null) {
			allHeaders.add(new MimeHeader("Content-Length", "0"));
		}
		if (MimeUtils.getHeader("Host", headers) == null) {
			allHeaders.add(new MimeHeader("Host", target.getAuthority()));
		}
		return new DefaultHTTPRequest(
			"GET",
			target.getPath(),
			new PlainMimeEmptyPart(null,
				new MimeHeader("Content-Length", "0"),
				new MimeHeader("Host", target.getAuthority())
			)
		);
	}
	
	@Deprecated
	public static HTTPResponse newResponse(ReadableResource resource, Header...headers) throws IOException {
		return newResponse(null, resource, headers);
	}
	
	public static HTTPResponse newResponse(HTTPRequest request, ReadableResource resource, Header...headers) throws IOException {
		HTTPResponse newResponse = newResponse(request, resource.getContentType(), new ResourceReadableContainer(resource), headers);
		if (resource instanceof FiniteResource && MimeUtils.getHeader("Content-Length", newResponse.getContent().getHeaders()) == null) {
			newResponse.getContent().setHeader(new MimeHeader("Content-Length", new Long(((FiniteResource) resource).getSize()).toString()));
		}
		return newResponse;
	}
	
	@Deprecated
	public static HTTPResponse newEmptyResponse(Header...headers) {
		return newEmptyResponse(null, headers);
	}
	
	public static HTTPResponse newEmptyResponse(HTTPRequest request, Header...headers) {
		List<Header> allHeaders = new ArrayList<Header>(Arrays.asList(headers));
		allHeaders.add(new MimeHeader("Content-Length", "0"));
		return new DefaultHTTPResponse(request, 200, "OK", new PlainMimeEmptyPart(null,
			allHeaders.toArray(new Header[0])
		));
	}
	
	@Deprecated
	public static HTTPResponse newResponse(String contentType, ReadableContainer<ByteBuffer> content, Header...headers) throws IOException {
		return newResponse(null, contentType, content, headers);
	}
	
	public static HTTPResponse newResponse(HTTPRequest request, String contentType, ReadableContainer<ByteBuffer> content, Header...headers) throws IOException {
		List<Header> allHeaders = new ArrayList<Header>(Arrays.asList(headers));
		if (MimeUtils.getHeader("Content-Length", headers) == null) {
			long size = 0;
			if (content instanceof LimitedReadableContainer) {
				size = ((LimitedReadableContainer<ByteBuffer>) content).remainingData();
			}
			else {
				byte [] bytes;
				try {
					bytes = IOUtils.toBytes(content);
				}
				finally {
					content.close();
				}
				size = bytes.length;
				content = IOUtils.wrap(bytes, true);
			}
			allHeaders.add(new MimeHeader("Content-Length", new Long(size).toString()));
		}
		if (MimeUtils.getHeader("Content-Type", headers) == null && contentType != null) {
			allHeaders.add(new MimeHeader("Content-Type", contentType));
		}
		return new DefaultHTTPResponse(request, 200, "OK", new PlainMimeContentPart(null, content, allHeaders.toArray(new Header[0])));
	}
	
	public static URI getURI(HTTPRequest request, boolean secure) throws FormatException {
		try {
			if (request.getTarget().startsWith("https://") || request.getTarget().startsWith("http://")) {
				return new URI(URIUtils.encodeURI(request.getTarget()));
			}
			else {
				Header hostHeader = MimeUtils.getHeader("Host", request.getContent().getHeaders());
				String uri;
				String target = request.getTarget();
				if (!target.startsWith("/")) {
					target = "/" + target;
				}
				if (hostHeader == null) {
					if (request.getVersion() > 1.0) {
						throw new FormatException("No 'host' header is present and the target is not complete");	
					}
					try {
						String hostName = InetAddress.getLocalHost().getHostName();
						uri = (secure ? "https" : "http") + "://" + hostName + target; 
					}
					catch (UnknownHostException e) {
						throw new RuntimeException(e);
					}
				}
				else {
					uri = (secure ? "https" : "http") + "://" + hostHeader.getValue() + target;
				}
				return new URI(URIUtils.encodeURI(uri));
			}
		}
		catch (URISyntaxException e) {
			throw new FormatException(e);
		}	
	}
	
	public static HTTPRequest redirect(HTTPRequest original, URI uri, boolean absolute) {
		String target = absolute 
			? uri.toString() 
			: (uri.getPath().isEmpty() ? "/" : uri.getPath());
		
		HTTPRequest newRequest = new DefaultHTTPRequest(original.getMethod(), target, original.getContent());
		newRequest.getContent().removeHeader("Host");
		if (!absolute)
			newRequest.getContent().setHeader(new MimeHeader("Host", uri.getAuthority()));
		return newRequest;
	}
	
	public static Header authenticateServer(HTTPResponse response, Principal principal, ClientAuthenticationHandler authenticationHandler) {
		if (authenticationHandler != null) {
			for (Header wwwAuthenticateHeader : MimeUtils.getHeaders(SERVER_AUTHENTICATE_REQUEST, response.getContent().getHeaders())) {
				if (wwwAuthenticateHeader != null) {
					String handshake = authenticationHandler.authenticate(principal, wwwAuthenticateHeader.getValue());
					if (handshake != null)
						return new MimeHeader(SERVER_AUTHENTICATE_RESPONSE, handshake);
				}
			}
		}
		return null;
	}
	
	public static Header authenticateProxy(HTTPResponse response, Principal principal, ClientAuthenticationHandler authenticationHandler) {
		if (authenticationHandler != null) {
			for (Header proxyAuthenticateHeader : MimeUtils.getHeaders(PROXY_AUTHENTICATE_REQUEST, response.getContent().getHeaders())) {
				if (proxyAuthenticateHeader != null) {
					String handshake = authenticationHandler.authenticate(principal, proxyAuthenticateHeader.getValue());
					if (handshake != null)
						return new MimeHeader(PROXY_AUTHENTICATE_RESPONSE, handshake);
				}
			}
		}
		return null;
	}
	
	public static boolean keepAlive(HTTPEntity entity) {
		List<String> values = entity.getContent() != null ? MimeUtils.getValues("Connection", entity.getContent().getHeaders()) : null;
		if (values == null) {
			return entity.getVersion() >= 1.1;
		}
		else {
			boolean explicitClose = false;
			boolean explicitKeepAlive = false;
			for (String value : values) {
				if ("Keep-Alive".equalsIgnoreCase(value)) {
					explicitKeepAlive = true;
				}
				else if ("Close".equalsIgnoreCase(value)) {
					explicitClose = true;
				}
			}
			if (explicitClose) {
				return false;
			}
			else if (explicitKeepAlive) {
				return true;
			}
			else {
				return entity.getVersion() >= 1.1;
			}
		}
	}
	
	public static void setHeader(ModifiablePart part, CustomHeader header, String value) throws HTTPException {
		if (part != null) {
			if (value == null) {
				part.removeHeader(header.getName());
			}
			else {
				if (!header.isUserValueAllowed() && MimeUtils.getHeader(header.getName(), part.getHeaders()) != null) {
					throw new HTTPException(400, "Header not allowed: " + header.getName());
				}
				part.setHeader(new MimeHeader(header.getName(), value));
			}
		}
	}
	
	public static AuthenticationHeader getAuthenticationHeader(Header...headers) {
		for (Header header : headers) {
			if (header instanceof AuthenticationHeader) {
				return (AuthenticationHeader) header;
			}
		}
		return null;
	}
	
	public static AuthenticationHeader getAuthenticationHeader(HTTPEntity entity) {
		if (entity.getContent() != null) {
			return getAuthenticationHeader(entity.getContent().getHeaders());
		}
		return null;
	}
	
	public static SecurityHeader getSecurityHeader(HTTPEntity entity) {
		if (entity.getContent() != null) {
			for (Header header : entity.getContent().getHeaders()) {
				if (header instanceof SecurityHeader) {
					return (SecurityHeader) header;
				}
			}
		}
		return null;
	}
	
	/**
	 * TODO: add stuff like timeout, domain, security-only...
	 */
	public static ModifiableHeader newSetCookieHeader(String key, String value) {
		return new MimeHeader("Set-Cookie", key + "=" + value);
	}
	
	public static ModifiableHeader newCookieHeader(String key, String value) {
		return new MimeHeader("Cookie", key + "=" + value);
	}
	
	public static ModifiableHeader newSetCookieHeader(String key, String value, Date expires, String path, String domain, Boolean secure, Boolean httpOnly) {
		MimeHeader header = new MimeHeader("Set-Cookie", key + "=" + value);
		if (expires != null) {
			if (formatter.get() == null) {
				SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss 'GMT'", Locale.US);
				dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
				formatter.set(dateFormatter);
			}
			header.addComment("Expires=" + formatter.get().format(expires));
		}
		if (path != null) {
			header.addComment("Path=" + path);
		}
		if (domain != null) {
			header.addComment("Domain=" + domain);
		}
		if (secure != null && secure) {
			header.addComment("Secure");
		}
		if (httpOnly != null && httpOnly) {
			header.addComment("HttpOnly");
		}
		return header;
	}
	
	public static Map<String, List<String>> getCookies(Header...headers) {
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		for (Header header : headers) {
			if (header.getName().equalsIgnoreCase("Cookie")) {
				List<String> cookies = new ArrayList<String>();
				cookies.add(header.getValue());
				cookies.addAll(Arrays.asList(header.getComments()));
				for (String cookie : cookies) {
					int index = cookie.indexOf('=');
					if (index > 0) {
						String name = cookie.substring(0, index).trim();
						String value = cookie.substring(index + 1).trim();
						if (!result.containsKey(name)) {
							result.put(name, new ArrayList<String>());
						}
						if (!value.isEmpty()) {
							result.get(name).add(value);
						}
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Described in: https://www.ietf.org/rfc/rfc2388.txt
	 * Example: http://stackoverflow.com/questions/4526273/what-does-enctype-multipart-form-data-mean
	 * Important: there is no support yet for multipart/mixed, this "should" be if multiple files are selected for one file input, not for multiple file inputs
	 */
	public static Map<String, List<ContentPart>> getMultipartFormData(HTTPEntity entity) {
		Map<String, List<ContentPart>> formData = new HashMap<String, List<ContentPart>>();
		// check if we are indeed dealing with a multipart form data
		if (entity.getContent() instanceof MultiPart && "multipart/form-data".equals(MimeUtils.getContentType(entity.getContent().getHeaders()))) {
			// basically each part in the multipart is a value
			// the "files" (the reason you usually go multipart) should have a file name in the content disposition
			// the content-disposition itself is set to "form-data"
			// the files should also have a content type set
			for (Part part : (MultiPart) entity.getContent()) {
				if (part instanceof ContentPart) {
					Map<String, String> header = MimeUtils.getHeaderAsValues("Content-Disposition", part.getHeaders());
					if (header != null && "form-data".equals(header.get("value"))) {
						String name = header.get("name");
						if (!formData.containsKey(name)) {
							formData.put(name, new ArrayList<ContentPart>());
						}
						formData.get(name).add((ContentPart) part);
					}
				}
				else {
					throw new IllegalArgumentException("Nested multiparts are not yet supported");
				}
			}
		}
		return formData;
	}
	
	public static Date getIfModifiedSince(Header...headers) throws ParseException {
		Header header = MimeUtils.getHeader("If-Modified-Since", headers);
		return header == null ? null : parseDate(header.getValue());
	}
	
	public static Date parseDate(String value) throws ParseException {
		return value == null ? null : getDateFormatter().parse(value);
	}
	
	public static String formatDate(Date date) {
		return date == null ? null : getDateFormatter().format(date);
	}
	
	private static ThreadLocal<SimpleDateFormat> dateParser = new ThreadLocal<SimpleDateFormat>();
	
	private static SimpleDateFormat getDateFormatter() {
		if (dateParser.get() == null) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
			simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			dateParser.set(simpleDateFormat);
		}
		return dateParser.get();
	}
	
	public static void setContentEncoding(ModifiablePart part, Header...requestHeaders) {
		Long contentLength = MimeUtils.getContentLength(part.getHeaders());
		// don't set any encoding headers if there is no data
		if (contentLength != null && contentLength == 0) {
			return;
		}
		// TODO: we should also check if the readable is null... but that risks potentially opening a "heavy" resource
		String contentEncoding = null;
		List<String> acceptedEncodings = MimeUtils.getAcceptedEncodings(requestHeaders);
		if (acceptedEncodings.contains("gzip")) {
			contentEncoding = "gzip";
		}
		else if (acceptedEncodings.contains("deflate")) {
			contentEncoding = "deflate";
		}
		// if we have gzip/deflate content encoding, we need to remove any mention of content length (as it changes due to zipping)
		// additionally we need to make sure the transfer encoding is set to "chunked"
		if (contentEncoding != null) {
			part.setHeader(new MimeHeader("Content-Encoding", contentEncoding));
			part.removeHeader("Content-Length");
			part.removeHeader("Transfer-Encoding");
			part.setHeader(new MimeHeader("Transfer-Encoding", "chunked"));
		}
	}

}
