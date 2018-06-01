package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Node;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.pin.MultiplePinFilterResponse;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class ILLakeTR extends TSServer {

	public static final long serialVersionUID = 10000000L;
	
	private boolean downloadingForSave; 

	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	private static final Pattern pinPattern = Pattern.compile("(?is)Pin:[^<]+</td>\\s*<td>([^<]+)");		
	private static final Pattern historyLinkPattern = Pattern.compile("(?is)<a[^>]*href=\\s*\"([^\"]*taxyear=\\d{4}&pin=[^\"]*)\"[^>]*>");
	private static final Pattern detailedBillPattern = Pattern.compile("(?is)<a\\s+href\\s*=\\s*\\\"javascript:poptastic\\('(collbook[^']+')");
	private static final String linkPart = "http://apps01.lakecountyil.gov/sptreasurer/collbook/";
	
	public ILLakeTR(long searchId) {
		super(searchId);
	}

	public ILLakeTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException
    {
		String zip, pin;
		int ServerInfoModuleID = module.getModuleIdx();

		if (ServerInfoModuleID == 1) //search by address
		{
			zip = module.getFunction(4).getParamValue();
			if (zip != null)
			{
				zip = zip.replaceAll("(?is)(\\d{5})-\\d+", "$1");
				module.getFunction(4).setParamValue(zip);
			}
		} else if (ServerInfoModuleID == 2) //search by pin 
		{
			pin = module.getFunction(0).getParamValue();
			if (pin != null) {
				pin = pin.replaceAll("(?is)(\\d+)0{4}", "$1");
				module.getFunction(0).setParamValue(pin);
			}
		}
		return super.SearchBy(module, sd);
    }
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO_GENERIC_TR);
		String streetName = getSearchAttribute(SearchAttributes.P_STREETNAME);
		String streetNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		String zip = getSearchAttribute(SearchAttributes.P_ZIP);
			
		FilterResponse multiplePINFilter = new MultiplePinFilterResponse(searchId);
			
		if (Search.AUTOMATIC_SEARCH == searchType){
			Collection<String> pins = getSearchAttributes().getPins(-1);
			if(pins.size() > 1){			
				for(String pid: pins){
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
					module.clearSaKeys();
					if (!pid.contains("-")){
						if (pid.length() == 10){
							pid = pid.replaceAll("(\\d{2})(\\d{2})(\\d{3})(\\d{3})", "$1-$2-$3-$4");
						} else if (pid.length() == 14){
							pid = pid.replaceAll("(\\d{2})(\\d{2})(\\d{3})(\\d{3})(\\d{4})", "$1-$2-$3-$4-$5");
						}
					}
					module.getFunction(0).forceValue(pid);
					modules.add(module);	
				}			
				// set list for automatic search 
				serverInfo.setModulesForAutoSearch(modules);
				resultType = MULTIPLE_RESULT_TYPE;
				return;
			}
		}
			
		if (hasPin()){//Search by PINs (Parcel Numbers)
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setData( 0 , pin ); 
			modules.add(module);
		}
			
		if (hasStreet()){
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			zip = zip.replaceAll("(\\d+)-\\d+", "$1");//only first 5 digits needs on TR
			module.clearSaKeys();
			module.setData( 0 , streetNo );
			module.setData( 2 , streetName );
			module.setData( 4 , zip );
			module.addFilter(multiplePINFilter);
			modules.add(module);
		}
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	@Override
	protected int getResultType(){
		if(mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) == Boolean.TRUE ||
		   (mSearch.getSa().getPins(-1).size() > 1 && (Search.AUTOMATIC_SEARCH == mSearch.getSearchType()))){
			return MULTIPLE_RESULT_TYPE; 
		} else {
			return UNIQUE_RESULT_TYPE;
		}
	}
	
	@Override
    public boolean anotherSearchForThisServer(ServerResponse sr) {
		boolean result = mSearch.getSa().getPins(-1).size() > 1 &&
			    		 mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) != Boolean.TRUE;
		return result?true:super.anotherSearchForThisServer(sr);
	}
	
	private String getContents(String response, String serverResponse){
		
		// if from memory - use it as is
		if(!serverResponse.contains("<html")){
			return serverResponse;
		}
		
		String contents = "";
		
		// cleanup
		contents = response.replaceAll("(?is)<a\\s+href\\s*=\\s*\\\"javascript:\\(printReady.*?Print Version\\s*</font>\\s*</a>", "");
		contents = contents.replaceAll("(?is)</?blockquote>", "");
		Matcher taxBillMat = detailedBillPattern.matcher(serverResponse);
	    String taxBillResponse = "";
	    
	    if (taxBillMat.find()){
	    	String link = linkPart + taxBillMat.group(1);
	    	HTTPRequest reqP = new HTTPRequest(link);
	    	reqP.setMethod(HTTPRequest.GET);
	    	
	    	HTTPResponse resP = null;
        	HttpSite site = HttpManager.getSite("ILLakeTR", searchId);
			try
			{
				resP = site.process(reqP);
			} finally 
			{
				HttpManager.releaseSite(site);
			}
			taxBillResponse = resP.getResponseAsString();
		}
	    if (taxBillResponse != null) {
	    	//String ownerInfo = taxBillResponse.replaceAll("(?is).*</script>\\s*</font>\\s*</td>\\s*</tr>\\s*(<tr>\\s*<td[^>]+>.*?Pin.*BALANCE\\s*DUE.*?</td>\\s*</tr>).*", "$1");
	    	String taxDetails = taxBillResponse.replaceAll("(?is).*Print This Page.*?</div>(.*)", "$1");
	    	taxDetails = taxDetails.replaceAll("(?is)<img[^>]+>", "");
	    	taxDetails = taxDetails.replaceAll("(?is)<script language.*?</script>", "");
	    	taxDetails = taxDetails.replaceAll("(?is)</body>", "");
	    	taxDetails = taxDetails.replaceAll("(?is)align\\s*=\\s*\\\"\\s*center\\s*\\\"", "");
	    	//contents = contents + ownerInfo;
	    	contents = contents + taxDetails;
	    }

		Matcher taxHistMat = historyLinkPattern.matcher(response);
		String taxHistResponse = "";
		while (taxHistMat.find()) {
			String link = taxHistMat.group(1);
			taxHistResponse = getLinkContents(link);

			if (!taxHistResponse.isEmpty() && taxHistResponse.contains("Property Location")) {
				StringBuilder stringBuilder = new StringBuilder();
				HtmlParser3 htmlParser3 = new HtmlParser3(taxHistResponse);
				org.htmlparser.Node table1 = htmlParser3.getNodeById("tblPriorYearsBills");
				org.htmlparser.Node table2 = htmlParser3.getNodeById("tblLegalDescription");
				org.htmlparser.Node table3 = htmlParser3.getNodeById("tblData");
				stringBuilder.append("<hr>");
				
				if (table1 != null) {
					stringBuilder.append(table1.toHtml());
				}
				if (table2 != null) {
					stringBuilder.append(table2.toHtml());
				}
				if (table3 != null) {
					stringBuilder.append(table3.toHtml());
				}

				if (stringBuilder.length() > 0) {
					contents += stringBuilder.toString().replaceFirst("(?is)(Tax\\s+year\\s+\\d{4})", "<h2>$1</h2>");
				}
			}
		}
		
		contents = contents.replaceAll("(?is)<p align=\\\"right\\\">", "<p align=\"center\">");
		contents = contents.replaceAll("<td width=\\\"\\d+\\\"", "<td ");
		contents = contents.replaceAll("(?is)\\s+>", ">");
		contents = contents.replaceAll("(?is)</?span[^>]*>", "");
		contents = contents.replaceAll("(?is)-\\s*please\\s*call.*?any\\s*questions", "");
		contents = contents.replaceAll("(?is)&nbsp;?", " ");
		contents = contents.replaceAll("(?is)<a[^>]*>(.*?)</a>", "$1");
		contents = contents.replaceAll("(?is)<img[^>]*>", "");
		// contents = contents.replaceAll("(?is)&nbsp(&?[A-Z])", "&nbsp;$1");
		return contents;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {	
		String response = Response.getResult();
		String contents = null;
		
		switch(viParseID){
		
		case ID_DETAILS:
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_ADDRESS:
			
			// pin not found
			if(response.matches("(?is).*Requested\\s*Pin\\s*was\\s*not\\s*found.*")){
				Response.getParsedResponse().setError("<font color=\"red\">No results found under this PIN.</font>  Please search again.");
				return;
			}
			
			if(response.matches("(?is).*Requested\\s*Pin\\s*was\\s*not\\s*found.*")){
				Response.getParsedResponse().setError("<font color=\"red\">No results found under this PIN.</font>  Please search again.");
				return;
			}
			
			Node tbl = HtmlParserTidy.getNodeById(response, "printReady2", "div");
			String html = "";
			try {
				html = HtmlParserTidy.getHtmlFromNode(tbl);
			} catch (TransformerException e) {
				e.printStackTrace();
			}
			// extract contents
			contents = getContents(html, response);
			
			if(contents == null){
				Response.getParsedResponse().setError("<font color=\"red\">Error retrieving page</font>");
				return;
			}

			
		    
		    if (StringUtils.isEmpty(contents)){
			    Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
				return;
		    }
		    
			Response.getParsedResponse().setResponse(contents);
			
			// isolate pin number
			String keyCode = "File";
			Matcher pinMatcher = pinPattern.matcher(contents);
			if(pinMatcher.find())			{
				keyCode = pinMatcher.group(1);
				keyCode = keyCode.replaceAll("-","");
			} 
			
			if ((!downloadingForSave)){
                
                String qry_aux = Response.getRawQuerry();
				qry_aux = "dummy=" + keyCode + "&" + qry_aux;
				String originalLink = sAction + "&" + qry_aux;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
				
                if (FileAlreadyExist(keyCode + ".html")){
                	contents += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, contents);
					contents = addSaveToTsdButton(contents, sSave2TSDLink, viParseID);
				}

				Response.getParsedResponse().setPageLink(
					new LinkInPage(
						sSave2TSDLink,
						originalLink,
						TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(
					Response.getParsedResponse(),
					contents,
					Parser.NO_PARSE); 
            } 
			else {            	
                msSaveToTSDFileName = keyCode + ".html";
                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                msSaveToTSDResponce = contents + CreateFileAlreadyInTSD();                
                parser.Parse(Response.getParsedResponse(), contents, Parser.PAGE_DETAILS);	                                
			}
			break;
		
		case ID_SAVE_TO_TSD:
			
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;				
			break;		
		}
	}
	
    /**
     * get file name from link
     */
	@Override
	protected String getFileNameFromLink(String link)
	{
		String fileName = "File.html";
		Matcher dummyMatcher = dummyPattern.matcher(link);
		if(dummyMatcher.find())
		{
			fileName = dummyMatcher.group(1);
			fileName = fileName + ".html";
		}
        return fileName;
    }

}