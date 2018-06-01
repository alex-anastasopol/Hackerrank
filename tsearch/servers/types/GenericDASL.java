package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_TEXT_FIELD;

import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.ORIENTATION_HORIZONTAL;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.PIXELS;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ro.cst.tsearch.connection.dasl.DaslConnectionSiteInterface;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.SearchLogger;

/**
 * @author cristi stochina
 */
public class GenericDASL extends TSServerDASLAdapter {
		
	private static final  String TEMPLATE_TEXT_AREA = "<textarea name=\"templateTextArea\"  id=\"templateTextArea\" cols=\"100\" rows=\"40\" wrap=\"hard\"> " ;
	private static final  String TEMPLATE_TEXT_AREA_END = 		"</textarea>";

	private static final  String TEMPLATE_SELECT ="<select name=\"templateSelect\"  id=\"templateSelect\"  size=\"1\"  onchange=\"javascript:  changeTemplate( );\" >" +
			"<option value=\"0\">NDB</option>" +
			"<option value=\"1\">DT</option>" +
			"<option value=\"2\">RV</option>" +
			"</select>";

	private String curentTemplateType = null;
	
    private static final long serialVersionUID = 7424565818026610710L;

    public GenericDASL(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid)
    {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
    }
    
    
    @Override
    public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
    	
    	// log the search in the SearchLogger
    	logSearchBy(module);
    	
    	// get search parameters
    	Map<String,String> params = getNonEmptyParams(module, null);

    	String xmlQuery =params.get("templateTextArea");
    	
    	ro.cst.tsearch.connection.dasl.DaslConnectionSiteInterface.DaslResponse daslResponse = 
    			getDaslSite().performSearch(xmlQuery, searchId);
    	
    	// order not received in time
    	if(daslResponse.status == DaslConnectionSiteInterface.ORDER_PLACED){
    		SearchLogger.info("<font color=\"red\">DASL order not completed in time! Gave up.</font>", searchId);
    		return ServerResponse.createErrorResponse("Order Not Completed in Time!");
    	}
    	
    	// error appeared with order
    	if(daslResponse.status == DaslConnectionSiteInterface.ORDER_ERROR){
    		SearchLogger.info("<font color=\"red\">DASL error! Gave up.</font>", searchId);    		
    		return ServerResponse.createErrorResponse("DASL response:");
    	}    	
	    // create & populate server response
    	ServerResponse sr = new ServerResponse();
        sr.setResult(daslResponse.xmlResponse);
    	
        // solve response
        solveHtmlResponse("", Parser.PAGE_DETAILS, "SearchBy", sr, sr.getResult());
        
        // log number of results found
        SearchLogger.info("Found <span class='number'>" + sr.getParsedResponse().getResultsCount() + "</span> <span class='rtype'>intermediate</span> results.<br/>", searchId);
        
        // return response
        return sr;
    }


	@Override
	protected String getInstrFromXML(Node doc, HashMap<String, String> data) {
		// TODO Auto-generated method stub
		return null;
	}
     
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo si = new TSServerInfo(1);
		si.setServerAddress("");
		si.setServerIP("");
		si.setServerLink("" );

		// get order number
		String order = getSearch().getUniqueIdentifier();
		int orderLength = order.length();
		if(orderLength > 10){
			order = order.substring(0, 10); 
		}
		
		// Order Images
		{
			TSServerInfoModule 		
			sim = si.ActivateModule(TSServerInfo.DASL_GENERAL_SEARCH_MODULE_IDX, 2);
			sim.setName("DASL_GENERAL_MODULE");
			sim.setDestinationPage("/"); 
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(Parser.PAGE_DETAILS);
			
			PageZone pz = new PageZone("OrderImages", "Order Images", ORIENTATION_HORIZONTAL, null, 710, 50, PIXELS , true);

			try{				
	            HTMLControl  template = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1, 1, 50, sim.getFunction(0), "templateTextArea", "", "", searchId),
	            type   = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  1, 1, 60, sim.getFunction(1), "templateSelect", "", "", searchId);	            
	          
	            this.curentTemplateType = GenericDASLNDB.DASL_TEMPLATE_CODE;
	            
	             
	            sim.getFunction(0).setHtmlformat( fillTextArea() )  ;
	            
	            sim.getFunction(1).setHtmlformat( TEMPLATE_SELECT)  ;
	            
	            
	            pz.addHTMLObjectMulti(template, type);
	            
			}catch(FormatException e){
				e.printStackTrace();
			}
			
			sim.setModuleParentSiteLayout(pz);
		}
		
		si.setupParameterAliases();
		
		return si;
	}
	
	private   String fillTextArea(){
		String templateContent = buildSearchQuery(new HashMap<String, String>() , 0);
		return TEMPLATE_TEXT_AREA+"\n"+templateContent+"\n"+TEMPLATE_TEXT_AREA_END;
	}
	
	
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
    	ParsedResponse pr = Response.getParsedResponse();
    	
    	 String txt = TEMPLATE_TEXT_AREA+"\n"+prettyPrint(Response.getResult())+"\n"+TEMPLATE_TEXT_AREA_END;
    	 
    	 txt  = "<center>"+txt+"</center>";
       pr.setResponse(txt);
           
	}
	
	
	 private static final String XML_TAG_END = "[<][^/>]*[/][^/>]*[>]" ;
	    private static final Pattern PAT_XML_TAG_END = Pattern.compile(XML_TAG_END );
	    
	    private static String prettyPrint(String xmlContent){
	    	StringBuffer strBuf = new StringBuffer(xmlContent);
	    	Matcher mat = PAT_XML_TAG_END.matcher(strBuf);
	    	
	    	int i =0;
	    	Vector<Integer> vec = new Vector<Integer>();
	    	while(mat.find()){
	    		vec.add(mat.end()+i);
	    		i+=2;
	    	}
	    	
	    	for(int j=0;j<vec.size();j++){
	    		int poz = vec.get(j);
	    		strBuf.insert(poz, "\r\n");
	    	}
	    	
	    	return strBuf.toString();
	    }

	    protected String getDASLTemplateName() {
			return this.curentTemplateType;
		}
	    
		@Override
		protected ServerPersonalData getServerPersonalData() {
			// TODO Auto-generated method stub
			return null;
		}


		@Override
		public void specificParseLegalDescription(Document doc, ParsedResponse item, ResultMap resultMap) {
		}
		
		@Override
		public void specificParseGrantorGrantee(Document doc, ParsedResponse item, ResultMap resultMap) {
		}


		@Override
		public void specificParseAddress(Document doc, ParsedResponse item,ResultMap resultMap) {
		}
	
}