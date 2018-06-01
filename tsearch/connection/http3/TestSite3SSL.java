package ro.cst.tsearch.connection.http3;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;

public class TestSite3SSL {
	public static void main(String[] args) {
		DefaultHttpClient httpClient = new DefaultHttpClient();

		HttpUriRequest httpget = new HttpGet("https://vpn3030.insnoc.com/CACHE/sdesktop/install/start.htm");

		SSLSocketFactory sf = null;

		X509TrustManager tm = new X509TrustManager() {

			public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
				System.out.println("checkClientTrusted");
			}

			public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
				System.out.println("checkServerTrusted");
			}

			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};

		X509HostnameVerifier verifier = new X509HostnameVerifier() {

			@Override
			public boolean verify(String arg0, SSLSession arg1) {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public void verify(String arg0, SSLSocket arg1) throws IOException {
				// TODO Auto-generated method stub

			}

			@Override
			public void verify(String arg0, X509Certificate arg1) throws SSLException {
				// TODO Auto-generated method stub

			}

			@Override
			public void verify(String arg0, String[] arg1, String[] arg2) throws SSLException {
				// TODO Auto-generated method stub

			}
		};
		
		TrustStrategy ts = new TrustStrategy() {
			@Override
			public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
				return true;
			}
		};
		
		
		try {

			X509HostnameVerifier verifier2 = null;
			
			SSLContext ctx = SSLContext.getInstance("SSL");
			ctx.init(null, new TrustManager[] { tm }, new java.security.SecureRandom());
			sf = new SSLSocketFactory(ctx, verifier);
//			sf = new SSLSocketFactory(ctx, verifier);
			
//			sf = new SSLSocketFactory(ts, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			
			Scheme https = new Scheme("https", 443, sf);
			SchemeRegistry sr = httpClient.getConnectionManager().getSchemeRegistry();
			sr.register(https);

			httpClient.execute(httpget);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

		}
	}
}
