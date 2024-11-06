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

import junit.framework.TestCase;

public class TestContentTypeMap extends TestCase {
	
	private ContentTypeMap map = ContentTypeMap.getInstance();
	
	public void testContentTypes() {
		assertEquals("application/edifact", (map.getContentTypeFor("test.edifact")));
		assertEquals("text/plain", map.getContentTypeFor("test.txt"));
	}

	public void testCompoundExtensions() {
		assertEquals("application/x-tgz", map.getContentTypeFor("test.tar.gz"));
	}

	/**
	 * Make sure the order of extensions is preserved if multiple are defined for a single content type
	 * Note that currently the reverse (multiple content types for a single extension) currently has no guaranteed order!
	 */
	public void testExtensionsOrdering() {
		assertEquals("txt", map.getExtensionFor("text/plain"));
	}
	
	public void testContentTypeForDotless() {
		assertNull(map.getContentTypeFor("test"));
	}
}
