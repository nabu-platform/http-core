package be.nabu.libs.http.core;

import be.nabu.libs.resources.DynamicResource;
import be.nabu.libs.resources.api.DynamicResourceProvider;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;

public class DefaultDynamicResourceProvider implements DynamicResourceProvider {

	@Override
	public ReadableResource createDynamicResource(ReadableContainer<ByteBuffer> originalContent, String name, String contentType, boolean shouldClose) {
		return new DynamicResource(originalContent, name, contentType, shouldClose);
	}

}
