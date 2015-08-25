package be.nabu.libs.http.core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import be.nabu.libs.resources.api.FiniteResource;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.LimitedReadableContainer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.mime.api.ContentPart;
import be.nabu.utils.mime.api.Header;
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
	
	public static HTTPResponse newResponse(ReadableResource resource, Header...headers) throws IOException {
		HTTPResponse newResponse = newResponse(resource.getContentType(), new ResourceReadableContainer(resource), headers);
		if (resource instanceof FiniteResource && MimeUtils.getHeader("Content-Length", newResponse.getContent().getHeaders()) == null) {
			newResponse.getContent().setHeader(new MimeHeader("Content-Length", new Long(((FiniteResource) resource).getSize()).toString()));
		}
		return newResponse;
	}
	
	public static HTTPResponse newEmptyResponse(Header...headers) {
		List<Header> allHeaders = new ArrayList<Header>(Arrays.asList(headers));
		allHeaders.add(new MimeHeader("Content-Length", "0"));
		return new DefaultHTTPResponse(200, "OK", new PlainMimeEmptyPart(null,
			allHeaders.toArray(new Header[0])
		));
	}
	
	public static HTTPResponse newResponse(String contentType, ReadableContainer<ByteBuffer> content, Header...headers) throws IOException {
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
		return new DefaultHTTPResponse(200, "OK", new PlainMimeContentPart(null, content, allHeaders.toArray(new Header[0])));
	}
	
	public static URI getURI(HTTPRequest request, boolean secure) throws FormatException {
		Header hostHeader = MimeUtils.getHeader("Host", request.getContent().getHeaders());
		try {
			if (request.getTarget().startsWith("https://") || request.getTarget().startsWith("http://"))
				return new URI(request.getTarget());
			else if (hostHeader == null)
				throw new FormatException("No 'host' header is present and the target is not complete");
			else
				return new URI((secure ? "https" : "http") + "://" + hostHeader.getValue() + (request.getTarget().startsWith("/") ? request.getTarget() : ""));
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
		Header connectionHeader = entity.getContent() != null ? MimeUtils.getHeader("Connection", entity.getContent().getHeaders()) : null;
		return connectionHeader != null 
			? connectionHeader.getValue().equalsIgnoreCase("Keep-Alive")
			: entity.getVersion() > 1.0;
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
	
	public static AuthenticationHeader getAuthenticationHeader(HTTPEntity entity) {
		if (entity.getContent() != null) {
			for (Header header : entity.getContent().getHeaders()) {
				if (header instanceof AuthenticationHeader) {
					return (AuthenticationHeader) header;
				}
			}
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
	public static Header newSetCookieHeader(String key, String value) {
		return new MimeHeader("Set-Cookie", key + "=" + value);
	}
	
	public static Header newCookieHeader(String key, String value) {
		return new MimeHeader("Cookie", key + "=" + value);
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
	
	public static List<String> getAcceptedContentTypes(Header...headers) {
		List<String> list = new ArrayList<String>();
		Header acceptHeader = MimeUtils.getHeader("Accept", headers);
		if (acceptHeader != null) {
			list.addAll(Arrays.asList(acceptHeader.getValue().split("[\\s]*,[\\s]*")));
		}
		return list;
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
}
