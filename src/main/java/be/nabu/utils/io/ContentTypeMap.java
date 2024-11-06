/*
* Copyright (C) 2014 Alexander Verbruggen
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

package be.nabu.utils.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class ContentTypeMap implements FileNameMap {
	
	/**
	 * This property can optionally contain a URL pointing to a custom mime properties file with the structure:
	 * mimeType = extension1,extension2,...
	 */
	public static final String CONTENT_TYPES = "be.nabu.io.contentTypes";
	
	private FileNameMap parent;
	
	private Map<String, Set<String>> extensions = new HashMap<String, Set<String>>();
	private Map<String, Set<String>> contentTypes = new HashMap<String, Set<String>>();
	
	private static ContentTypeMap instance;
	
	public static ContentTypeMap getInstance() {
		if (instance == null) {
			try {
				instance = new ContentTypeMap(URLConnection.getFileNameMap(), System.getProperty(CONTENT_TYPES) != null 
					? new URL(System.getProperty(CONTENT_TYPES))
					: Thread.currentThread().getContextClassLoader().getResource("mime.properties"));
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return instance;
	}
	
	public static void register() {
		URLConnection.setFileNameMap(getInstance());
	}
	
	public ContentTypeMap(FileNameMap parent, URL url) throws IOException {
		if (url == null)
			throw new NullPointerException("The url must point to a valid properties file containing mimetype to comma-separated extension mappings");
		this.parent = parent;
		InputStream input = url.openStream();
		try {
			byte[] bytes = IOUtils.toBytes(IOUtils.wrap(input));
			for (String line : new String(bytes, "UTF-8").split("[\r\n]+")) {
				if (line.trim().isEmpty() || line.trim().startsWith("#")) {
					continue;
				}
				int indexOf = line.indexOf('=');
				if (indexOf > 0) {
					String contentType = line.substring(0, indexOf);
					String extension = line.substring(indexOf + 1);
					registerContentType(contentType, extension.split("[\\s]*,[\\s]*"));
				}
			}
		}
		finally {
			input.close();
		}
	}

	@Override
	public String getContentTypeFor(String fileName) {
		Iterator<String> iterator = getAllContentTypesFor(fileName).iterator();
		return iterator.hasNext() ? iterator.next() : null;
	}
	
	public String getExtensionFor(String contentType) {
		Iterator<String> iterator = getAllExtensionsFor(contentType).iterator();
		return iterator.hasNext() ? iterator.next() : null;
	}
	
	public Set<String> getAllExtensionsFor(String contentType) {
		// lowercase the contentType
		contentType = contentType.toLowerCase();
		
		Set<String> extensions = new LinkedHashSet<String>();
		
		// local extensions
		if (contentTypes.containsKey(contentType))
			extensions.addAll(contentTypes.get(contentType));

		// parent extensions		
		if (parent != null && parent instanceof ContentTypeMap)
			extensions.addAll(((ContentTypeMap) parent).getAllExtensionsFor(contentType));
		
		return extensions;
	}
	
	public Set<String> getAllContentTypesFor(String fileName) {
		Set<String> contentTypes = new LinkedHashSet<String>();
		
		// if it has no dot at all, try as a whole
		if (fileName.indexOf('.') < 0) {
			if (extensions.containsKey(fileName.toLowerCase())) {
				contentTypes.addAll(extensions.get(fileName.toLowerCase()));
			}
		}
		else {
			// search local from longest possible extension to shortest
			int lastIndex = -1;
			while ((lastIndex = fileName.indexOf('.', lastIndex + 1)) != -1) {
				// the substring is +1 to skip the actual dot
				String possibleExtension = fileName.substring(lastIndex + 1).toLowerCase();
				if (extensions.containsKey(possibleExtension)) {
					contentTypes.addAll(extensions.get(possibleExtension));
					break;
				}
			}
		}

		// parent contentTypes
		if (parent != null) {
			if (parent instanceof ContentTypeMap)
				contentTypes.addAll(((ContentTypeMap) parent).getAllContentTypesFor(fileName));
			else {
				String possibleContentType = parent.getContentTypeFor(fileName);
				if (possibleContentType != null)
					contentTypes.add(possibleContentType);
			};
		}

		return contentTypes;
	}
	
	public void registerContentType(String contentType, String...extensions) {
		// make contentType lowercase
		contentType = contentType.toLowerCase();
		// make all extensions lowercase
		for (int i = 0; i < extensions.length; i++)
			extensions[i] = extensions[i].toLowerCase();
		
		// update content type map
		if (!contentTypes.containsKey(contentType))
			contentTypes.put(contentType, new LinkedHashSet<String>());
		contentTypes.get(contentType).addAll(Arrays.asList(extensions));
		// update extension map
		for (String extension : extensions) {
			if (!this.extensions.containsKey(extension))
				this.extensions.put(extension, new LinkedHashSet<String>());
			this.extensions.get(extension).add(contentType);
		}
	}
}