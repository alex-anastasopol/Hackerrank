package ro.cst.tsearch.connection.http.auth.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;


public class TLSSocketFactory implements SecureProtocolSocketFactory {
	private SSLContext sslcontext = null;
	private String[] enabledProtocols = new String[] { "SSLv2Hello" , "SSLv3" };
	
	
	
	private SSLContext createSSLContext() {
		SSLContext sslcontext = null;
		try {
			sslcontext = SSLContext.getInstance("SSL");
			sslcontext.init(null,
					new TrustManager[] { new TrustAnyTrustManager() },
					new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
		return sslcontext;
	}

	private SSLContext getSSLContext() {
		if (this.sslcontext == null) {
			this.sslcontext = createSSLContext();
		}
		return this.sslcontext;
	}

	public Socket createSocket(Socket socket, String host, int port,
			boolean autoClose) throws IOException, UnknownHostException {
		Socket sslSocket = getSSLContext().getSocketFactory().createSocket(socket, host,
						port, autoClose);
		if(sslSocket instanceof SSLSocket) {
			((SSLSocket)sslSocket).setEnabledProtocols(enabledProtocols);
		}
		return sslSocket;
	}

	public Socket createSocket(String host, int port) throws IOException,
			UnknownHostException {
		Socket sslSocket = getSSLContext().getSocketFactory().createSocket(host, port);
		if(sslSocket instanceof SSLSocket) {
			((SSLSocket)sslSocket).setEnabledProtocols(enabledProtocols);
		}
		return sslSocket;
	}

	public Socket createSocket(String host, int port, InetAddress clientHost,
			int clientPort) throws IOException, UnknownHostException {
		Socket sslSocket = getSSLContext().getSocketFactory().createSocket(host, port,
				clientHost, clientPort);
		if(sslSocket instanceof SSLSocket) {
			((SSLSocket)sslSocket).setEnabledProtocols(enabledProtocols);
		}
		return sslSocket;
	}

	public Socket createSocket(String host, int port, InetAddress localAddress,
			int localPort, HttpConnectionParams params) throws IOException,
			UnknownHostException, ConnectTimeoutException {
		if (params == null) {
			throw new IllegalArgumentException("Parameters may not be null");
		}
		int timeout = params.getConnectionTimeout();
		SocketFactory socketfactory = getSSLContext().getSocketFactory();
		Socket sslSocket = null;
		if (timeout == 0) {
			sslSocket = socketfactory.createSocket(host, port, localAddress, localPort);
		} else {
			sslSocket = socketfactory.createSocket();
			SocketAddress localaddr = new InetSocketAddress(localAddress,
					localPort);
			SocketAddress remoteaddr = new InetSocketAddress(host, port);
			sslSocket.bind(localaddr);
			sslSocket.connect(remoteaddr, timeout);
		}
		if(sslSocket instanceof SSLSocket) {
			((SSLSocket)sslSocket).setEnabledProtocols(enabledProtocols);
		}
		return sslSocket;
	}

	private static class TrustAnyTrustManager implements X509TrustManager {
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}

		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[] {};
		}
	}
}