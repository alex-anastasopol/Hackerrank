/*
 * Created on Jan 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.utils;

import java.net.*;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Category;

import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.data.SitesPasswords;

/**
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 * 
 * This class is used to auto-login o sites with SSL Authentication 
 */
public class WebAuth extends Authenticator {	

	protected static final Category logger= Category.getInstance(WebAuth.class.getName());

	private Map<String,String> pswds = new HashMap<String,String>();
	private long searchId;

	private WebAuth(long searchId){
	  this.searchId = searchId; 	
      pswds.put("70.159.150.166", "TNKnoxRO");   // store SiteName for Knox ro IP
      pswds.put("205.152.84.193", "TNWilsonAO"); // store SiteName for Wilson AO IP	      
	}

	
	public  static WebAuth getInstance(long searchId){		
	    return new WebAuth(searchId);
	}
	
	protected PasswordAuthentication getPasswordAuthentication() {
	    
		String ipkey = getRequestingSite().getHostAddress();
	    String siteName = pswds.get(ipkey); 
	    String u = "", p = "";
	    
		
	    if(siteName != null){
	        u = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), siteName, "user");
	        p = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), siteName, "password");
	    }
	    
		return new PasswordAuthentication(u,p.toCharArray());
		
	}
	
	public String getCurrentCommunityId(){
		CommunityAttributes communityAttributes = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity();
		String string = communityAttributes.getID().toString();
   		return  string;
	}
	
}
