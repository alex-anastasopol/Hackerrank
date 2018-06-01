package ro.cst.tsearch.data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.database.DBManager;

/**
 * Holds passwords for all configured sites
 *
 */
public class SitesPasswords implements Serializable{
	
	public static String PASSWORD_COMMUNITY  = "communityId_";
	public static String PASSWORD_NAME_PREFIX  = "pwdN_";
	public static String PASSWORD_VALUE_PREFIX = "pwdV_";
	public static String PASSWORD_CHECK_PREFIX = "pwdCheck_";	
	public static String PASSWORD_SITE_PREFIX  = "pwdS_";
		
	public static final int OP_ADD	  = 0;
	public static final int OP_DELETE = 1;
	
	/**
	 * map used to cache the passwords from db
	 */
	private Map<String,Password> allPasswords = null;

	/**
	 * Immutable class storing a password
	 * @author radu bacrau
	 */
    public static class Password implements Serializable{
    	
    	public final String id;
    	public final String site;
    	/**
    	 * Site access username.
    	 */
    	public final String name;
    	/**
    	 * Site access password.
    	 */
    	public final String value;
    	public final String communityId;
    	
    	public Password(String site, String name, String value, String communityId, String passwordId){
    		this.site = site;
    		this.name = name;
    		this.value = value;
    		this.communityId = communityId;
    		this.id = passwordId;
    	}

		@Override
		public String toString() {
			return "Password [communityId=" + communityId
					+ ", name=" + name + ", site=" + site + ", value=" + value
					+ "]";
		}   
    	
    }
	
	/**
	 * private constructor
	 * loads all passwords from database for this server
	 */
	private SitesPasswords(){	
		load();
	}

	/**
	 * 
	 */
	private void load() {
		allPasswords = DBManager.getPasswords();
	}
	
    /**
     * safe and fast lazy initialization for getInstance()
     */
	private static class Holder{
		private static SitesPasswords instance = new SitesPasswords();
	}
	
	/**
	 * return instance of this class
	 * @return
	 */
	public static SitesPasswords getInstance(){
		return Holder.instance;
	}
	
	/**
	 * Obtain all passwords for the current machine
	 * @return
	 */
	@Deprecated
	public Map<String,Password> getPasswords(){		
		return allPasswords;
	}	
	
	/**
	 * Obtain all passwords for a given community.
	 * @return
	 */
	public Collection<Password> getPasswords(String communityId){
//		Map<String, Password> passwords = DBManager.getPasswords(communityId, false);
		load();
		Collection<Password> values = allPasswords.values();
		List<Password> communityPasswords = new ArrayList<Password>();
		for (Password password : values) {
			if (password.communityId.toString().equals(communityId)){
				communityPasswords.add(password);
			}
		}
		return communityPasswords;
	}
	
	/**
	 * Obtain all passwords for a given community and a given site.
	 * @return
	 */
	public Collection<Password> getPasswords(String communityId, String siteName){
		Map<String, Password> passwords = DBManager.getPasswords(communityId,siteName,false);
		return passwords.values();
	}
	
	/**
	 * Obtain a password value
	 * @param communityId TODO
	 * @param site site name
	 * @param name password name
	 * @return password value
	 */
	public String getPasswordValue(String communityId, String site, String name){
		try{
			return allPasswords.get(communityId+"."+site + "." + name).value;
		}
		catch( Exception e ){
			//e.printStackTrace();
		}		
		return "";
	}
	
	/**
	 * Obtain a password value
	 * @param communityId TODO
	 * @param site site name
	 * @param name password name
	 * @return password value
	 */
	public String getPasswordValue(long communityId, String site, String name){
		try{
			return allPasswords.get(communityId+"."+site.replaceAll("\\s", "") + "." + name).value;
		}
		catch( Exception e ){
			//e.printStackTrace();
		}		
		return "";
	}
	
	/**
	 * Delete password from database
	 * @param communityId 
	 * @param site site name
	 * @param name password name
	 */
	public void deletePassword(String communityId, String site, String name){
		DBManager.deletePassword(communityId, site, name);
		allPasswords.remove(communityId+"."+site + "." + name);
	}
	
	/**
	 * Add password to database
	 * @param site site name
	 * @param communityId 
	 * @param name password name
	 * @param value password value
	 */
	public void addPassword(String site, String communityId, String name, String value){
		Password newPassword = new Password(site, name, value, communityId, null);
        String key = communityId + "." + site + "." + name;
        boolean isNew = !allPasswords.containsKey(key);
        // check not to write it again
        if(!isNew){
        	String prevValue = allPasswords.get(key).value;
        	if(value.equals(prevValue)){
        		return;
        	}
        }
		DBManager.writePassword(newPassword, isNew);	
		allPasswords.put(key, newPassword);		
	}
	
	/**
	 * processes changes / adding of new passwords
	 * @param req httprequest taken from servlet / jsp
	 */
	public void doUpdates( HttpServletRequest req ){
			
		// get operation
		int operation = OP_ADD;
		try{
			operation = Integer.parseInt(req.getParameter("operation" ));
		}
		catch(RuntimeException e){
			return;
		}
		
		// perform operation on all records
		int i = 0;		
		while(true){
		
			String communityId = req.getParameter(PASSWORD_COMMUNITY+i);
			String site = req.getParameter( PASSWORD_SITE_PREFIX + i );
			String name = req.getParameter( PASSWORD_NAME_PREFIX + i );
			String value = req.getParameter( PASSWORD_VALUE_PREFIX + i );
		
			if(name == null || name == null || value == null){
				break;
			}
			
			switch(operation){
			case OP_ADD:
					if(!"".equals(site) && !"".equals(name)){						
						addPassword(site, communityId, name, value);
					}
				break;
			case OP_DELETE:
				if( req.getParameter( PASSWORD_CHECK_PREFIX + i ) != null ){
					deletePassword(communityId, site, name);
				}
				break;
			}
		
			i++;
		};
	}
	
	
}