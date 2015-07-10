package be.nabu.libs.http.core;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Without a cookiestore, the cookiemanager stores _nothing_
 */
public class CustomCookieStore implements CookieStore {

	private Map<URI, List<HttpCookie>> store = new HashMap<URI, List<HttpCookie>>();
	
	@Override
	public void add(URI uri, HttpCookie cookie) {
		// force a path to be set for the cookie due to strange logic in CookieManager.pathMatches()
		if (cookie.getPath() == null)
			cookie.setPath("/");
		if (!store.containsKey(uri))
			store.put(uri, new ArrayList<HttpCookie>());
		if (!store.get(uri).contains(cookie))
			store.get(uri).add(cookie);
	}

	@Override
	public List<HttpCookie> get(URI uri) {
		return store.containsKey(uri) ? store.get(uri) : new ArrayList<HttpCookie>();
	}

	@Override
	public List<HttpCookie> getCookies() {
		List<HttpCookie> result = new ArrayList<HttpCookie>();
		for (List<HttpCookie> list : store.values())
			result.addAll(list);
		return result;
	}

	@Override
	public List<URI> getURIs() {
		return new ArrayList<URI>(store.keySet());
	}

	@Override
	public boolean remove(URI uri, HttpCookie cookie) {
		if (store.containsKey(uri)) {
			store.get(uri).remove(cookie);
			return true;
		}
		else
			return false;
	}

	@Override
	public boolean removeAll() {
		store.clear();
		return true;
	}

}
