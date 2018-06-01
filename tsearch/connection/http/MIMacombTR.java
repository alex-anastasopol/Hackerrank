package ro.cst.tsearch.connection.http;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.InstanceManager;


public class MIMacombTR extends HTTPSite {

	private boolean loggingIn = false;
	
	public static String sessionId = "";;
	
	public static final Pattern sessionIdPattern = Pattern.compile( "(?is)<string>([a-zA-Z0-9]{30,})</string>" );
	
	public static final String initGISSearch = "<?xml version=\"1.0\"?>" + 
												"<methodCall>"+
												"<methodName>GIS.Session.initialize</methodName>" +
												"<params>" +
													"<param>" +
														"<value><string></string></value>" +
													"</param>" +
													"<param>" +
														"<value><array><data>" +
																	"<value><array><data>" +
																				"<value><string>Delinquent</string></value>" +
																				"<value><i4>10</i4></value>" +
																				"<value><i4>897</i4></value>" +
																				"<value><i4>568</i4></value>" +
																				"<value><boolean>0</boolean></value>" +
																			"</data></array>" +
																	"</value>" +
																	/*
																	"<value><array><data>" +
																				"<value><string>municipal_boundaries</string></value>" +
																				"<value><i4>0</i4></value>" +
																				"<value><i4>124</i4></value>" +
																				"<value><i4>95</i4></value>" +
																				"<value><boolean>1</boolean></value>" +
																				"<value><i4>0</i4></value>" +
																				"<value><string>STATICVMAP</string></value>" +
																				"<value><double>0.065</double></value>" +
																				"<value><string>RedVicinityMap</string></value>" +
																			"</data></array>" +
																	"</value>" +
																	*/
																"</data></array>" +
														"</value>" +
													"</param>" +
												"</params>" +
											"</methodCall>";
	
	public LoginResponse onLogin() {
		
		loggingIn = true;
		
		setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "MIMacombTR" );
		
		//go to disclaimer page
		HTTPRequest req = new HTTPRequest( "http://gis.macombcountymi.gov/Delinquent%20Disclaimer.htm" );
		HTTPResponse res = process( req );
		
		String response = res.getResponseAsString();
		
		//accept disclaimer
		req = new HTTPRequest( "http://gis.macombcountymi.gov/freeance/Client/PublicAccess1/index.html?appconfig=delinquent_tax2" );
		res = process( req );
		
		response = res.getResponseAsString();
		
		loggingIn = false;
		
		//get sessionID
		req = new HTTPRequest( "http://gis.macombcountymi.gov/freeance/Server/Dzeims3.php" );
		req.setMethod( HTTPRequest.POST );
		req.setXmlPostData( initGISSearch );
		res = process( req );
		
		response = res.getResponseAsString();
		
		Matcher sessionIdMatcher = sessionIdPattern.matcher( response );
		
		if( sessionIdMatcher.find() ){
			sessionId = sessionIdMatcher.group( 1 );
		}
		
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
	
    public void onBeforeRequest(HTTPRequest req)
    {
    	if( loggingIn ){
    		return;
    	}
    	
        setUserData( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "MIMacombTR" );
        
        String xmlPostData = null;
        int page = 1;
        if (req.getPostParameter("page") != null){
        	try {
        		page = Integer.parseInt(req.getPostFirstParameter("page"));
        	} catch (NumberFormatException e){}
        }
        	
        if( req.getPostFirstParameter( "addressName" ) != null ){
        	//search by address
        	
        	String addressNo = req.getPostFirstParameter( "addressNo" );
        	String addressName = req.getPostFirstParameter( "addressName" );
        	String community = req.getPostFirstParameter( "community" );
        	
        	xmlPostData = "<methodCall>" +
							"<methodName>SQL.execute.definedPagedQuery</methodName>" +
							"<params>" +
							"<param>" +
							"<value><string>Address</string></value>" +
							"</param>" +
							"<param>" +
							"<value><i4>50</i4></value>" +
							"</param>" +
							"<param>" +
							"<value><i4>" + page + "</i4></value>" +
							"</param>" +
							"<param>" +
							"<value><array><data>" +
							"<value><array><data>" +
							"<value><string>address</string></value>" +
							"<value><string>" + (addressNo + " " + addressName).trim() +  "</string></value>" +
							"</data></array>" +
							"</value>" +
							"<value><array><data>" +
							"<value><string>community</string></value>" +
							"<value><string>" + community + "</string></value>" +
							"</data></array>" +
							"</value>" +
							"</data></array>" +
							"</value>" +
							"</param>" +
							"</params>" +
							"</methodCall>";
        }
        else if( req.getPostFirstParameter( "pid" ) != null ){
        	//search by pid
        	String parcelId = req.getPostFirstParameter( "pid" );
        	
        	xmlPostData = "<methodCall>" +
							"<methodName>SQL.execute.definedPagedQuery</methodName>" +
							"<params>" +
							"<param>" +
							"<value><string>Tax_ID</string></value>" +
							"</param>" +
							"<param>" +
							"<value><i4>50</i4></value>" +
							"</param>" +
							"<param>" +
							"<value><i4>" + page + "</i4></value>" +
							"</param>" +
							"<param>" +
							"<value><array><data>" +
							"<value><array><data>" +
							"<value><string>Tax_ID</string></value>" +
							"<value><string>" + parcelId + "</string></value>" +
							"</data></array>" +
							"</value>" +
							"</data></array>" +
							"</value>" +
							"</param>" +
							"</params>" +
							"</methodCall>";
        }
        else if( req.getPostFirstParameter( "taxPayerName" ) != null ){
        	//search by name
        	String taxPayerName = req.getPostFirstParameter( "taxPayerName" );
        	
        	xmlPostData = "<methodCall>" +
				"<methodName>SQL.execute.definedPagedQuery</methodName>" +
				"<params>" +
				"<param>" +
				"<value><string>Delinquent_owner</string></value>" +
				"</param>" +
				"<param>" +
				"<value><i4>50</i4></value>" +
				"</param>" +
				"<param>" +
				"<value><i4>" + page + "</i4></value>" +
				"</param>" +
				"<param>" +
				"<value><array><data>" +
				"<value><array><data>" +
				"<value><string>owner</string></value>" +
				"<value><string>" + taxPayerName + "</string></value>" +
				"</data></array>" +
				"</value>" +
				"</data></array>" +
				"</value>" +
				"</param>" +
				"</params>" +
				"</methodCall>";
        }
        else if( req.getPostFirstParameter( "viewTaxDetails" ) != null ){
        	String viewTaxDetails = req.getPostFirstParameter( "viewTaxDetails" );
        	
        	xmlPostData = "<methodCall>" +
				"<methodName>GIS.Zoom.to.entities</methodName>" +
        		"<params>" +
				"<param>" +
				"<value><string>" + sessionId + "</string></value>" +
				"</param>" +
				"<param>" +
				"<value><i4>0</i4></value>" +
				"</param>" +
				"<param>" +
				"<value><i4>996</i4></value>" +
				"</param>" +
				"<param>" +
				"<value><i4>710</i4></value>" +
				"</param>" +
				"<param>" +
				"<value><string>0</string></value>" +
				"</param>" +
				"<param>" +
				"<value><string>TAX_ID,ADDRESS,CVT_NAME</string></value>" +
				"</param>" +
				"<param>" +
				"<value><string>(TAX_ID = '"+ viewTaxDetails +"')</string></value>" +
				"</param>" +
				"<param>" +
				"<value><i4>1.8</i4></value>" +
				"</param>" +
				"<param>" +
				"<value><i4>1</i4></value>" +
				"</param>" +
				"<param>" +
				"<value><boolean>1</boolean></value>" +
				"</param>" +
				"<param>" +
				"<value><boolean>1</boolean></value>" +
				"</param>" +
				"<param>" +
				"<value><boolean>0</boolean></value>" +
				"</param>" +
				"<param>" +
				"<value><string>Deliquent Parcel</string></value>" +
				"</param>" +
				"<param>" +
				"<value><array><data>" +
				"<value><array><data>" +
				"<value><string>Legal</string></value>" +
				"<value><array><data>" +
				"<value><array><data>" +
				"<value><string>TAX_ID</string></value>" +
				"<value><string>taxid</string></value>" +
				"</data></array>" +
				"</value>" +
				"</data></array>" +
				"</value>" +
				"</data></array>" +
				"</value>" +
				"<value><array><data>" +
				"<value><string>weblinks</string></value>" +
				"<value><array><data>" +
				"<value><array><data>" +
				"<value><string>TAX_ID</string></value>" +
				"<value><string>taxid</string></value>" +
				"</data></array>" +
				"</value>" +
				"</data></array>" +
				"</value>" +
				"</data></array>" +
				"</value>" +
				"</data></array>" +
				"</value>" +
				"</param>" +
				"</params>" +
				"</methodCall>";
        }
        
        if( xmlPostData != null ){
        	req.setXmlPostData( xmlPostData );
        	
        	req.setHeader( "Content-Type" , "application/xml");
        }
        
        if( req.getXmlPostData() != null ){
        	req.setHeader( "Content-Type" , "application/xml");
        }
    }
}
