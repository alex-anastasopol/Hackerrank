package ro.cst.tsearch.connection.http;


import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mortbay.util.UrlEncoded;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.utils.InstanceManager;

public class MIWayneEP extends HTTPSite{

    static public final  Pattern patPostHiddenParameters = Pattern.compile("(?i)<input[ ]+type=\"[^\"]+\"[ ]+name=\"([^\"]+)\"[ ]+value=\"([^\"]*)\"[ ]*/>");
	
	static private final Pattern patDetroytLink = Pattern.compile("(?i)City[ ]+of[ ]+Detroit[ ]*[^>]+>[^>]+>[^>]+>[^>]+>[^\"]+\"[^']+'([^']*)'[ ]*,[ ]*'([^']*)'");
	
	static private final Pattern patAssesing= Pattern.compile("(?i)<td[^=]*=[^=]*=[^=]*=[^=]*=[^=]*=[ ]*'([^']+)'[^>]*>[^>]*>Assessing");
	
	static private final Pattern propertyLandSearch = Pattern.compile("(?i)<td[^=]*=[^=]*=[^=]*=[^=]*=[^=]*='([^']*)'[^>]*>Property[ ]*and[ ]*Land[ &nbsp;]*Search");
	
	public static final  String []removePostParametersLink = { "from", "unit" , "sna" , "snf", "appid" , "i" , "on"};
	
	private boolean onLogin = false;
	private boolean onBeforeRequest = false;
	
	private int currentPoz = 0;
	
	private String __EVENTTARGET_SEARCH_PAGE="";
	private String __EVENTARGUMENT_SEARCH_PAGE="";
	private String __VIEWSTATE_SEARCH_PAGE="";
	
	private String __EVENTTARGET_PARTIAL_NEXT="";
	private String __EVENTTARGET_PARTIAL_PREV="";
	
	private String __EVENTARGUMENT_PARTIAL="";
	private String __VIEWSTATE_PARTIAL="";
	
	
	public String get__EVENTARGUMENT() {
		return __EVENTARGUMENT_SEARCH_PAGE;
	}

	public void set__EVENTARGUMENT(String __eventargument) {
		__EVENTARGUMENT_SEARCH_PAGE = __eventargument;
	}

	public String get__EVENTTARGET() {
		return __EVENTTARGET_SEARCH_PAGE;
	}

	public void set__EVENTTARGET(String __eventtarget) {
		__EVENTTARGET_SEARCH_PAGE = __eventtarget;
	}

	public String get__VIEWSTATE() {
		return __VIEWSTATE_SEARCH_PAGE;
	}

	public void set__VIEWSTATE(String __viewstate) {
		__VIEWSTATE_SEARCH_PAGE = __viewstate;
	}

	public LoginResponse onLogin() {
		    setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "MIWayneEP" );
			onLogin = true;
			String userName = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "MIWayneEP", "user" );
			String password = SitesPasswords.getInstance().getPasswordValue( getCurrentCommunityId(), "MIWayneEP", "password");

			HTTPRequest req = new HTTPRequest( "https://is.bsasoftware.com/bsa.is/login.aspx" );
			HTTPResponse res = process( req );
			String response = res.getResponseAsString();
			
			req = new HTTPRequest("https://is.bsasoftware.com/bsa.is/login.aspx");
			req.setMethod(HTTPRequest.POST);
			req.setHeader("Referer","https://is.bsasoftware.com/bsa.is/login.aspx");
			
			int poz1 = response.indexOf("<form");
			int poz2 = response.indexOf("</form>");
			if(poz1<0||poz2<0||poz1>=poz2){
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
			}
			
			String res1 = response.substring(poz1,poz2);
			
			Matcher mat  = patPostHiddenParameters.matcher(res1);
			
			boolean findParameters = false;
			while(mat.find()){
				findParameters  = true;
				__VIEWSTATE_SEARCH_PAGE = mat.group(2)    ;
				req.setPostParameter(  mat.group(1) ,  __VIEWSTATE_SEARCH_PAGE);
			}
			if(!findParameters){
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			}
			
			req.setPostParameter("TextBoxUser", userName);
			req.setPostParameter("TextBoxPW", password);
			req.setPostParameter("ButtonLogin", "Login");
			
			res = process( req );
			response = res.getResponseAsString();
			
			req = new HTTPRequest("https://is.bsasoftware.com/bsa.is/SelectUnit.aspx");
			req.setMethod(HTTPRequest.POST);
			req.setHeader("Referer","https://is.bsasoftware.com/bsa.is/SelectUnit.aspx");
			Matcher matDetroy = patDetroytLink.matcher(response);
			mat = patPostHiddenParameters.matcher(response);
			
			String []keys = new String[2];
			int index=0;
			findParameters = false;
			while(mat .find()){
				if(index<2){
					keys [index ] = mat.group(1);
					index++;
				}
				else{
					 __VIEWSTATE_SEARCH_PAGE = mat.group(2)  ;
					req.setPostParameter(  mat.group(1) ,   __VIEWSTATE_SEARCH_PAGE  );
					findParameters = true;
				}
			}
			if(!findParameters){
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid parameters");
			}
			
			if(matDetroy.find()){
				req.setPostParameter(keys[0], matDetroy.group(1).replaceAll("[$]", ":"));
				req.setPostParameter(keys[1], matDetroy.group(2));
			}
			else {
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
			}
			
			res = process( req );
			response = res.getResponseAsString();
			
			mat = patAssesing.matcher(response);
			
			String searchLink="";
			if (mat.find()){
				searchLink = mat.group(1).replaceAll("&amp;", "&");
			}
			else{
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
			}
			
			req = new HTTPRequest("https://is.bsasoftware.com"+searchLink);
			req.setMethod(HTTPRequest.GET);
			
			res = process( req );
			response = res.getResponseAsString();
			
			mat = propertyLandSearch.matcher(response);
			
			if (mat.find()){
				searchLink = mat.group(1).replaceAll("&amp;", "&");
			}
			else{
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid response");
			}
			
			req = new HTTPRequest("https://is.bsasoftware.com"+searchLink);
			req.setMethod(HTTPRequest.GET);
			
			res = process( req );
			response = res.getResponseAsString();
			
			
			mat = patPostHiddenParameters.matcher(response);
			
			while(mat.find()){
				if(mat.group(1).toUpperCase().equals("__EVENTTARGET")){
					__EVENTTARGET_SEARCH_PAGE = mat.group(2);
				}
				else if(mat.group(1).toUpperCase().equals("__EVENTARGUMENT")){
					__EVENTARGUMENT_SEARCH_PAGE =  mat.group(2);
				}
				else if(mat.group(1).toUpperCase().equals("__VIEWSTATE")){
					__VIEWSTATE_SEARCH_PAGE = mat.group(2);
				}
			}
		
			onLogin = false;
			
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
	
	
	  public void onBeforeRequest(HTTPRequest req) {
		
		  setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "MIWayneEP" );
		  if(!onLogin && !onBeforeRequest){
			  onBeforeRequest = true;
			  String url = req.getURL();
			  
			  if(  url.contains("ServiceAssessingSearch.aspx")  ){
				  currentPoz = 0;
				  req.URL =  req.URL+"?i=1&appid=0";
				  
				  req.removePostParameters("StreetFrom_p");
				  req.removePostParameters("StreetTo_p");
				
				  req.setPostParameter("StreetFrom_p", req.getPostFirstParameter("StreetFrom"))  ;
				  req.setPostParameter("StreetTo_p", req.getPostFirstParameter("StreetTo"))  ;
				  
				  String str1 = req.getPostFirstParameter("MaskedParcelNumber_p");
				  req.removePostParameters("MaskedParcelNumber_p");
				  String str2 = req.getPostFirstParameter("MaskedParcelNumber");
				  
				  String owner = req.getPostFirstParameter("TextBoxOwnerName_on");
				  if(owner ==null || owner.length()==0){
					  if(str2!=null  ){
						  if(str2.length()>0){
							  str1 = str1.substring(str2.length()-1);
							  str1 = str2+str1;
						  }
					  }
			     }
				  req.setPostParameter("MaskedParcelNumber_p", str1 );
				  
			  }
			  else if(url.contains("ServiceAssessingSearchResults.aspx") ){
				  
				  Set set = req.getPostParameters().keySet();
				  Iterator it = set.iterator();
				  String query = "?";
				  
				  while(it.hasNext()){
					  	String key = (String) it.next();
					  	if(key !=null){
					  		if(!(key.equalsIgnoreCase("__EVENTTARGET") || key.equalsIgnoreCase("__EVENTARGUMENT") || key.equalsIgnoreCase("__VIEWSTATE")	)  ){
					  			String value = req.getPostFirstParameter(key);
					  			if(!key.equals("from")){
					  				query +=key +"="+  UrlEncoded.encodeString(value, "UTF-8") +"&";
					  			}
					  			else{
					  				//query +=key +"="+  value +"&";
					  			}
					  		}
					  	}
				  }
				  query = query .substring(0,query .length()-1);
				  req.URL =  req.URL+query;
				
				  if(req.getPostFirstParameter("from").equals("__next__")){
					  req.setPostParameter("__EVENTTARGET", __EVENTTARGET_PARTIAL_NEXT);
				  }
				  else{
					  req.setPostParameter("__EVENTTARGET", __EVENTTARGET_PARTIAL_PREV);
				  }
				  req.setPostParameter("__EVENTARGUMENT", __EVENTARGUMENT_PARTIAL);
				  req.setPostParameter("__VIEWSTATE", __VIEWSTATE_PARTIAL);				 
				 
				  for(int i=0;i<removePostParametersLink.length;i++){
					  req.removePostParameters(removePostParametersLink[i]);
				  }
			  }
			  else if(url.contains("ServiceAssessingDetails.aspx")&&(!url.contains("ConfirmTransaction.aspx"))){
				req.setReplaceSpaceOnredirect(true);
			  }
			  onBeforeRequest = false;
		  }
		 
	  }

	  
	public int getCurrentPoz() {
		return currentPoz;
	}

	public void setCurrentPoz(int currentPoz) {
		this.currentPoz = currentPoz;
	}

	public String get__EVENTARGUMENT_PARTIAL() {
		return __EVENTARGUMENT_PARTIAL;
	}

	public void set__EVENTARGUMENT_PARTIAL(String __eventargument_partial) {
		__EVENTARGUMENT_PARTIAL = __eventargument_partial;
	}

	public String get__EVENTTARGET_PARTIAL_NEXT() {
		return __EVENTTARGET_PARTIAL_NEXT;
	}

	public void set__EVENTTARGET_PARTIAL_NEXT(String __eventtarget_partial) {
		__EVENTTARGET_PARTIAL_NEXT = __eventtarget_partial;
	}
	
	public String get__EVENTTARGET_PARTIAL_PREV() {
		return __EVENTTARGET_PARTIAL_PREV;
	}

	public void set__EVENTTARGET_PARTIAL_PREV(String __eventtarget_partial) {
		__EVENTTARGET_PARTIAL_PREV = __eventtarget_partial;
	}

	public String get__VIEWSTATE_PARTIAL() {
		return __VIEWSTATE_PARTIAL;
	}

	public void set__VIEWSTATE_PARTIAL(String __viewstate_partial) {
		__VIEWSTATE_PARTIAL = __viewstate_partial;
	}
	  

}
