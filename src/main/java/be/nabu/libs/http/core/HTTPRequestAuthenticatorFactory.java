package be.nabu.libs.http.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPRequestAuthenticator;
import be.nabu.libs.http.api.HTTPRequestAuthenticatorProvider;
import be.nabu.libs.http.api.HTTPResponse;

public class HTTPRequestAuthenticatorFactory {
	private static HTTPRequestAuthenticatorFactory instance;
	
	public static HTTPRequestAuthenticatorFactory getInstance() {
		if (instance == null) {
			instance = new HTTPRequestAuthenticatorFactory();
		}
		return instance;
	}
	
	private List<HTTPRequestAuthenticatorProvider> providers = new ArrayList<HTTPRequestAuthenticatorProvider>();
	
	private Map<String, List<HTTPRequestAuthenticator>> authenticators = new HashMap<String, List<HTTPRequestAuthenticator>>();
	
	public Runnable register(final String name, final HTTPRequestAuthenticator authenticator) {
		if (!authenticators.containsKey(name)) {
			synchronized(this) {
				if (!authenticators.containsKey(name)) {
					authenticators.put(name, new ArrayList<HTTPRequestAuthenticator>());
				}
			}
		}
		authenticators.get(name).add(authenticator);
		return new Runnable() {
			@Override
			public void run() {
				authenticators.get(name).remove(authenticator);		
			}
		};
	}
	
	public Runnable register(final HTTPRequestAuthenticatorProvider provider) {
		providers.add(provider);
		return new Runnable() {
			@Override
			public void run() {
				providers.remove(provider);
			}
		};
	}
	
	public HTTPRequestAuthenticator getAuthenticator(String name) {
		final List<HTTPRequestAuthenticator> authenticators = new ArrayList<HTTPRequestAuthenticator>();
		if (this.authenticators.containsKey(name)) {
			authenticators.addAll(this.authenticators.get(name));
		}
		for (HTTPRequestAuthenticatorProvider provider : providers) {
			HTTPRequestAuthenticator requestAuthenticator = provider.getRequestAuthenticator(name);
			if (requestAuthenticator != null) {
				authenticators.add(requestAuthenticator);
			}
		}
		return new HTTPRequestAuthenticator() {
			@Override
			public boolean authenticate(HTTPRequest request, String context, HTTPResponse trigger, boolean refresh) {
				for (HTTPRequestAuthenticator authenticator : authenticators) {
					if (authenticator.authenticate(request, context, trigger, refresh)) {
						return true;
					}
				}
				return false;
			}
		};
	}
}
