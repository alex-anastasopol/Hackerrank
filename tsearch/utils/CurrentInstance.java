package ro.cst.tsearch.utils;

import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.connection.http.HTTPSite;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.user.UserAttributes;

public class CurrentInstance {
		
	protected static final Category logger = Logger.getLogger(CurrentInstance.class);
	
	private UserAttributes ua;
	private CommunityAttributes ca;

	private HttpServletResponse response;
	
	private Search crtSearchContext;
	
    private Hashtable attributes = null; 	

	public  void setCurrentUser(UserAttributes ua) {
		this.ua = ua;
	}
	public   UserAttributes getCurrentUser() {
		return ua;
	}	
	
	public  CommunityAttributes getCurrentCommunity() {
		return ca;
	}
	
	public int getCommunityId() {
		return ca.getID().intValue();
	}
	public  void setCurrentCommunity(CommunityAttributes ca) {
		this.ca = ca;
	}
		
	public  County getCurrentCounty() {
		try{
			return County.getCounty(new BigDecimal(crtSearchContext.getSa().getAtribute(SearchAttributes.P_COUNTY)));
		}catch(Exception e){
			logger.error ("Error gettting the county ", e);
			
			/*
			 * construct a dummy state and county and return it in case of exception
			 * this will avoid a NullPointerException to be thrown when using the return value of this function without checking
			 * the result value
			 */

			
			State dummyState = new State();
			dummyState.setStateAbv("");
			dummyState.setName("");
			dummyState.setStateId( new BigDecimal(-1) );
			
	        County dummyCounty = new County();
	        dummyCounty.setCountyId(new BigDecimal(-1));
	        dummyCounty.setName("");
	        dummyCounty.setState( dummyState );
	        
			return dummyCounty;
		}
	}

	public  State getCurrentState() {
		try{
			return State.getState(new BigDecimal(crtSearchContext.getSa().getAtribute(SearchAttributes.P_STATE)));
		}catch(Exception e){
			logger.error ("Error gettting the county ", e);
			return null;
		}
	}

	
	private HttpSession session=null;
	
	public   void setup(User currentUser, HttpServletRequest request, HttpServletResponse response, HttpSession session) throws BaseException {
		UserAttributes ua = currentUser.getUserAttributes();				
		setCurrentUser(ua);
		if(request != null) {
			session = request.getSession(true);
		}
		setHttpResponse(response);
		CommunityAttributes ca = CommunityUtils.getCommunityFromId(((BigDecimal)session.getAttribute(CommunityAttributes.COMMUNITY_ID)).longValue());
		setCurrentCommunity( ca);
		crtSearchContext = (Search) currentUser.getSearch(request);
		
	}
	
	public   void setup(User currentUser, long searchId, HttpServletResponse response, HttpSession session) throws BaseException {
		UserAttributes ua = currentUser.getUserAttributes();				
		setCurrentUser(ua);
		setHttpResponse(response);
		CommunityAttributes ca = CommunityUtils.getCommunityFromId(((BigDecimal)session.getAttribute(CommunityAttributes.COMMUNITY_ID)).longValue());
		setCurrentCommunity( ca);
		crtSearchContext = (Search) SearchManager.getSearch(searchId);
		
	}

	public  HttpSession getCurrentSession(){
		return  session;
	}
	
	public  Search getCrtSearchContext() {
		
		//we need this just for six month since now 1.03.2007
		/*if( crtSearchContext != null ){
			InstanceManager.getManager().setCurrentInstance( crtSearchContext.getSearchID() , this);
			InstanceManager.getManager().setCurrentInstance( crtSearchContext.getID() , this);
		}*/
		
		return crtSearchContext;
	}
	public   void setCrtSearchContext(Search s1) {
		
		crtSearchContext = s1;
		if(s1!=null){
			SearchAttributes sa=s1.getSa();
			if(sa!=null){
				sa.setSearchId(s1.getID());
			}
		}
	}

	public  HttpServletResponse getHttpResponse() {
		return response;
	}
	public  void setHttpResponse(HttpServletResponse response) {
		this.response = response;
	}
	
    public  HTTPSite getSite( Object key )
    {
        //get the site associated with the key
        
        if( attributes == null )
        {
            return null;
        }
        
        return ( HTTPSite ) attributes.get( key );
    }

    public  void setSite( Object key, HTTPSiteInterface site )
    {
        if( attributes == null )
        {
            attributes = new Hashtable();
        }
        
        attributes.put( key, site );
    }
    
    public  void removeAllSites( long searchId )
    {
        String keyStart = searchId + "";
        
        if( attributes == null )
        {
            return;
        }
        
        Iterator it = attributes.keySet().iterator();
        
        while( it.hasNext() )
        {
            String key = (String) it.next();
            
            if( key.startsWith( keyStart ) )
            {
                it.remove();
            }
        }
    }
    
    /*
     * bookPageType are used just for KYJeffersonRO when searching with ReferTO module
     * in crossRefJeffersonRO generic function we check for not null bookPageType
     * and add it as a reference from the current document
     */
    private boolean bookPageTypeEnabled = true;    
    
    public  void enableBookPageType(){
    	bookPageTypeEnabled = true; 
    }
    
    public   void disableBookPageType(){
    	bookPageTypeEnabled = false; 
    }
  
    private String [] bookPageType = null;
    
    public  String[] getBookPageType(){ 
    	if(!bookPageTypeEnabled){
    		return null;
    	} else {
    		return bookPageType;
    	}
    }
    
    public  void setBookPageType(String book, String page, String type){
    	bookPageType = new String[]{book, page, type};
    }
    
    public  void clearBookPageType(){
    	bookPageType = null;
    }
}
