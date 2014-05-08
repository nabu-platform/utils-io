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
