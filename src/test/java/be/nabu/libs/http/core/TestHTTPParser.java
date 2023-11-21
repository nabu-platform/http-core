package be.nabu.libs.http.core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.ParseException;

import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.resources.ResourceFactory;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.mime.api.Part;
import junit.framework.TestCase;

public class TestHTTPParser extends TestCase {
	
	public static ReadableResource getResource(URI uri) throws IOException {
		return (ReadableResource) ResourceFactory.getInstance().resolve(uri, null);
	}
	
	public void testParseMultipart() throws URISyntaxException, IOException, ParseException {
		URI uri = new URI("classpath:/multipart2.http");
		HTTPParser parser = new HTTPParser(new DefaultDynamicResourceProvider(), true);
		HTTPResponse parseResponse = parser.parseResponse(getResource(uri).getReadable());
		((MultiPart) parseResponse.getContent());
		System.out.println("response is: " + parseResponse.getContent());
	}
	
	public static String toString(Part part) throws IOException {
		ReadableContainer<ByteBuffer> input = ((ReadableResource) part).getReadable();
		try {
			return IOUtils.toString(IOUtils.wrapReadable(input, Charset.forName("UTF-8")));
		}
		finally {
			input.close();
		}
	}
}
