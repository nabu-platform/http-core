package be.nabu.libs.http.core;

import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.http.api.server.DeviceResolver;
import be.nabu.utils.mime.api.Header;

public class DeviceResolverImpl implements DeviceResolver {
	@Override
	public Device getDevice(Header...headers) {
		return HTTPUtils.getDevice(null, null, true, headers);
	}
}
