/*
 * Copyright 2012 James Moger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.maxtk;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.maxtk.maxml.Maxml;
import com.maxtk.maxml.MaxmlException;
import com.maxtk.maxml.MaxmlMap;
import com.maxtk.utils.Base64;
import com.maxtk.utils.FileUtils;

/**
 * Represents Maxilla user settings.
 * 
 * @author James Moger
 * 
 */
public class Settings implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final File maxillaSettings = new File(System.getProperty("user.home") + "/.maxilla/settings.maxml");
	
	enum Key {
		proxies;
	}
	
	File file;

	List<Proxy> proxies;
		
	Set<Repository> repositories;
	
	public static Settings load() throws IOException, MaxmlException {
		return load(null);
	}
	
	public static Settings load(File file) throws IOException, MaxmlException {
		if (file == null) {
			maxillaSettings.getParentFile().mkdirs();
			if (!maxillaSettings.exists()) {
				// write default maxilla settings
				FileWriter writer = new FileWriter(maxillaSettings);
				writer.append("proxies:\n- { id: myproxy, active: false, protocol: http, host:proxy.somewhere.com, port:8080, username: proxyuser, password: somepassword }");
				// TODO write repositories
				writer.close();
			}
			file = maxillaSettings;
		}
		return new Settings().parse(file);
	}

	private Settings() {
		// default configuration
		proxies = new ArrayList<Proxy>();
		repositories = new LinkedHashSet<Repository>();
	}
	
	@SuppressWarnings("unchecked")
	Settings parse(File file) throws IOException, MaxmlException {
		if (!file.exists()) {
			throw new MaxmlException(MessageFormat.format("{0} does not exist!", file));
		}

		this.file = file;
		String content = FileUtils.readContent(file, "\n").trim();
		Map<String, Object> map = Maxml.parse(content);

		// parse proxies
		if (map.containsKey(Key.proxies.name())) {
			List<MaxmlMap> values = (List<MaxmlMap>) map.get(Key.proxies.name());
			List<Proxy> ps = new ArrayList<Proxy>();
			for (MaxmlMap definition : values) {
				Proxy proxy = new Proxy();
				proxy.id = definition.getString("id", "");
				proxy.active = definition.getBoolean("active", false);
				proxy.protocol = definition.getString("protocol", "http");
				proxy.host = definition.getString("host", "");
				proxy.port = definition.getInt("port", 80);
				proxy.username = definition.getString("username", "");
				proxy.password = definition.getString("password", "");
				proxy.nonProxyHosts = definition.getStrings("nonProxyHosts", new ArrayList<String>());
				ps.add(proxy);
			}
			proxies = ps;
		}
		return this;
	}
	
	void add(Repository repository) {
		repositories.add(repository);
	}

	public List<Proxy> getActiveProxies() {
		List<Proxy> activeProxies = new ArrayList<Proxy>();
		for (Proxy proxy : proxies) {
			if (proxy.active) {
				activeProxies.add(proxy);
			}
		}
		return activeProxies;
	}

	public java.net.Proxy getProxy(String url) {
		if (proxies.size() == 0) {
			return java.net.Proxy.NO_PROXY;
		}
		for (Proxy proxy : proxies) {
			if (proxy.active && proxy.matches(url)) {
				return new java.net.Proxy(java.net.Proxy.Type.HTTP, proxy.getSocketAddress());
			}
		}
		return java.net.Proxy.NO_PROXY;
	}

	public String getProxyAuthorization(String url) {
		for (Proxy proxy : proxies) {
			if (proxy.active && proxy.matches(url)) {
				return "Basic " + Base64.encodeBytes((proxy.username + ":" + proxy.password).getBytes());
			}
		}
		return "";
	}
}