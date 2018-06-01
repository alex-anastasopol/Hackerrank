/*
 * Created on May 26, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ro.cst.tsearch.connection.ftp;

/**
 * @author alfred
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SetProxy {
	
	public void setProxyFTP(String proxy){
        System.getProperties().put( "ftp.proxySet","true");
        System.getProperties().put( "ftp.proxyHost", proxy);
        //System.getProperties().put( "ftp.proxyPort ", port);
	}
	

}
