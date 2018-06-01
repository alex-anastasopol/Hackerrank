package ro.cst.tsearch.connection.http.auth.ssl;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLSocket;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.params.HttpParams;


public class CustomSSLSocketFactory extends SSLSocketFactory {
	private String[] enabledProtocols = new String[] { "SSLv2Hello" , "SSLv3" };
	
	public CustomSSLSocketFactory(TrustStrategy ts, X509HostnameVerifier allowAllHostnameVerifier) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
		 super(SSL, null, null, null, null, ts, allowAllHostnameVerifier);
	}
	
	@Override
	public Socket createSocket(HttpParams params) throws IOException {
		 SSLSocket sock = (SSLSocket) this.createSocket();
	        prepareSocket(sock);
	        ((SSLSocket)sock).setEnabledProtocols(enabledProtocols);
	        return sock;
	}
}