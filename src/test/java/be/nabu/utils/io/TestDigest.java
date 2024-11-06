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
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.Container;
import be.nabu.utils.io.containers.bytes.ByteContainerDigest;
import be.nabu.utils.io.containers.chars.HexReadableCharContainer;
import junit.framework.TestCase;

public class TestDigest extends TestCase {
	public void testDigest() throws NoSuchAlgorithmException, IOException {
		Container<ByteBuffer> byteContainer = new ByteContainerDigest(IOUtils.newByteBuffer(), MessageDigest.getInstance("MD5"));
		Container<CharBuffer> charContainer = IOUtils.wrap(
			new HexReadableCharContainer(byteContainer),
			IOUtils.wrapWritable(byteContainer, Charset.forName("UTF-8"))
		);
		charContainer.write(IOUtils.wrap("this is a test"));
		// need to close to create the digest
		charContainer.close();
		// the md5 was verified with external sources
		assertEquals("54b0c58c7ce9f2a8b551351102ee0938", IOUtils.toString(charContainer));
	}
	
	
	public void testLongDigest() throws NoSuchAlgorithmException, IOException {
		Container<ByteBuffer> byteContainer = new ByteContainerDigest(IOUtils.newByteBuffer(), MessageDigest.getInstance("MD5"));
		Container<CharBuffer> charContainer = IOUtils.wrap(
			new HexReadableCharContainer(byteContainer),
			IOUtils.wrapWritable(byteContainer, Charset.forName("UTF-8"))
		);
		charContainer.write(IOUtils.wrap("this is a slightly longer test that is designed to go over the md5 result size"));
		// need to close to create the digest
		charContainer.close();
		assertEquals("43390f6ddcc1e488b3184317bc0d172e", IOUtils.toString(charContainer));
	}
}
