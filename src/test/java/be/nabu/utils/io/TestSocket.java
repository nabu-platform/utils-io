package be.nabu.utils.io;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import be.nabu.utils.io.api.ByteContainer;

/**
 * Note that this is not actual a runnable testcase because available of network (with or without proxy) is not a given
 * The code is however tested in a specific environment and should provide a clue as to how it should work
 */
public class TestSocket {
	
	public void testProxy() throws KeyManagementException, NoSuchAlgorithmException {
		// let's try google
		String host = "google.com";
		int port = 443;
		
		// first connect to a proxy
		ByteContainer proxySocket = IOUtils.connect("<proxy>", 8080);
		// request a tunnel to google
		IOUtils.copy(
			IOUtils.wrap(("CONNECT " + host + ":" + port + " HTTP/1.1\r\n"
					+ "Host: " + host + "\r\n"
					+ "Proxy-Connection: Keep-Alive\r\n"
					+ "\r\n").getBytes()),
			IOUtils.blockUntilWritten(proxySocket)
		);
		
		// let's check the reply of the proxy server, should validate that it sends back HTTP/1.1 200 Connection Established
		System.out.println(new String(IOUtils.toBytes(proxySocket)));
		
		// starts ssl in client mode
		ByteContainer secureSocket = IOUtils.wrapSSL(proxySocket, createTrustAllContext(), true);
		
		// write a GET request for the root
		IOUtils.copy(	
			IOUtils.wrap(("GET / HTTP/1.1\r\nHost: " + host + "\r\n"
					+ "\r\n").getBytes()), 
			IOUtils.blockUntilWritten(secureSocket)
		);
		
		// check the response of google
		// in my test it returned "HTTP/1.1 301 Moved Permanently" with a few more headers and a basic html page
		System.out.println(new String(IOUtils.toBytes(IOUtils.blockUntilRead(secureSocket))));

		IOUtils.close(secureSocket);
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
