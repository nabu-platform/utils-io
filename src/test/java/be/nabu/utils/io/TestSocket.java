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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.Container;

/**
 * Note that this is not actual a runnable testcase because available of network (with or without proxy) is not a given
 * The code is however tested in a specific environment and should provide a clue as to how it should work
 */
public class TestSocket { // extends TestCase
	
	public void testProxy() throws KeyManagementException, NoSuchAlgorithmException, IOException {
		// let's try google
		String host = "google.com";
		int port = 443;
		String proxy = "<proxy>";
		// first connect to a proxy
		Container<ByteBuffer> proxySocket = IOUtils.connect(proxy, 8080);
		// request a tunnel to host
		IOUtils.copyBytes(
			IOUtils.wrap(("CONNECT " + host + ":" + port + " HTTP/1.1\r\n"
					+ "Host: " + host + "\r\n"
					+ "Proxy-Connection: Keep-Alive\r\n"
					+ "\r\n").getBytes(), true),
			IOUtils.blockUntilWritten(proxySocket)
		);
		
		// let's check the reply of the proxy server, we should validate that it sends back HTTP/1.1 200 Connection Established
		System.out.println(new String(IOUtils.toBytes(proxySocket)));
		
		// starts ssl in client mode
		Container<ByteBuffer> secureSocket = IOUtils.secure(proxySocket, createTrustAllContext(), true);
		
		// write a GET request for the root
		IOUtils.copyBytes(	
			IOUtils.wrap(("GET / HTTP/1.1\r\nHost: " + host + "\r\n"
					+ "\r\n").getBytes(), true), 
			IOUtils.blockUntilWritten(secureSocket)
		);
		
		// check the response of google
		// in my test it returned "HTTP/1.1 301 Moved Permanently" with a few more headers and a basic html page
		System.out.println(new String(IOUtils.toBytes(IOUtils.blockUntilRead(secureSocket))));

		secureSocket.close();
	}
	
	public static X509TrustManager createTrustAllManager() {
		return new X509TrustManager() {

			@Override
			public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				// do nothing					
			}

			@Override
			public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				// do nothing
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};
	}
	
	/**
	 * Use only for testing purposes!
	 * @param type
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 */
	public static SSLContext createTrustAllContext() throws KeyManagementException, NoSuchAlgorithmException {
		SSLContext context = SSLContext.getInstance("TLS");
		TrustManager[] trustAllCerts = new TrustManager[] { createTrustAllManager() };
		context.init(null, trustAllCerts, null);
		return context;
	}
}
