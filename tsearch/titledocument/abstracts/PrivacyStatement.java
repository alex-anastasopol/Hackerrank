package ro.cst.tsearch.titledocument.abstracts;

import java.util.ResourceBundle;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;


public class PrivacyStatement {
	
	private String privacyStatement = "";
	public static final String privacyName ="PrivacyStatement";
	private String getPrivacyCompleteName="";
	
	private Search search = null;
	private CommunityAttributes ca = null;
	
	public PrivacyStatement(Search search, CommunityAttributes ca) {
	    
	    this.search = search;
	    this.ca = ca;
		this.privacyStatement = createPrivacyStatement();
	}
	private  ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	final String templatesPath=rbc.getString("templates.path");
	
	//		WE NEED TO PUT ALL THIS CODE IN A HTML FILE 
	private String createPrivacyStatement(){
		String content=null;
		try{
			if (!(ca.getIgnorePrivacyStatement().equals(Boolean.TRUE))){
				String comm_id = InstanceManager.getManager().getCurrentInstance(search.getID()).getCurrentCommunity().getID().longValue()+"";
				
				content = new String(DBManager.getFileContentsFromDb(comm_id, "ts_community_terms_of_use"));
			}
		
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		if(content==null){
			if (!(ca.getIgnorePrivacyStatement().equals(Boolean.TRUE))){
				content = StringUtils.fileToString(templatesPath+"/Privacy.html");
			}
		}
		
		return content;
		
		
	}
	
	public String getPrivacyStatement() {
		return privacyStatement;
	}
	

	/**
	 * @return
	 */
	public String getGetPrivacyCompleteName() {
		return getPrivacyCompleteName;
	}

}