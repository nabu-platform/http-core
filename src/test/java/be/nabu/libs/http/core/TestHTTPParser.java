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
import java.nio.charset.Charset;
import java.text.ParseException;

import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.resources.ResourceFactory;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.mime.api.MultiPart;
import be.nabu.utils.mime.api.Part;
import junit.framework.TestCase;

public class TestHTTPParser extends TestCase {
	
	public static ReadableResource getResource(URI uri) throws IOException {
		return (ReadableResource) ResourceFactory.getInstance().resolve(uri, null);
	}
	
	public void testParseMultipart() throws URISyntaxException, IOException, ParseException {
		URI uri = new URI("classpath:/multipart3.http");
		HTTPParser parser = new HTTPParser(new DefaultDynamicResourceProvider(), true);
		HTTPResponse parseResponse = parser.parseResponse(getResource(uri).getReadable());
		System.out.println("parsed: " + parseResponse);
		MultiPart multipart = ((MultiPart) parseResponse.getContent());
		//System.out.println("response is: " + parseResponse.getContent());
		String content = toString(multipart.getChild("part0"));
//		System.out.println(content);
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
