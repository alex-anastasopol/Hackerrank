package ro.cst.tsearch.servers.types;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

/**
 * @author mihaib
  */

public class ILWillTR extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 

	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
	private static final Pattern fiveYearTaxInquiryPat = Pattern.compile("(?is)<a\\s+href\\s*=\\s*\\\"(CCALM11[^\\\"]+)");
	private static final Pattern taxDetailInquiryPat = Pattern.compile("(?is)<a\\s+href\\s*=\\s*\\\"(CCALM12[^\\\"]+)");
	private static final String link = "http://willtax.willcountydata.com/maintax/"; 
	private static final Pattern taxYear = Pattern.compile("(?is)(\\d+)\\s*Levy\\s*Real\\s*Estate\\s*Tax");
	
	
	public ILWillTR(long searchId) {
		super(searchId);
	}

	public ILWillTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	public static void splitResultRows(
			Parser p,
			ParsedResponse pr,
			String htmlString,
			int pageId,
			String linkStart,
			int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException
			{
			
			p.splitResultRows(
				pr,
				htmlString,
				pageId,
				"<tr><td>",
				"</tr>",
				linkStart,
				action);
		}
		
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if (Search.AUTOMATIC_SEARCH == searchType){
			// P0 - search by multiple PINs
			Collection<String> pins = getSearchAttributes().getPins(-1);
			if(pins.size() > 1){			
				for(String pin: pins){
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
					module.clearSaKeys();
					module.getFunction(0).forceValue(pin);
					modules.add(module);	
				}			
				// set list for automatic search 
				serverInfo.setModulesForAutoSearch(modules);
				resultType = MULTIPLE_RESULT_TYPE;
				return;
			}
		}
				
		if (hasPin()){
			//Search by ParcelNumber
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
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
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {	
		
		String response = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		response = response.replaceAll("(?is)(<table[^>]*)(<)", "$1>$2");// a table has its opening tag unclosed(missing ">")
				
		switch(viParseID){
		
		case ID_SEARCH_BY_TAX_BIL_NO:
		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
			
			response = response.replaceAll("[\\x00]+", "");
			
			if(response.matches("(?is).*PARCEL\\s+IS\\s+NOT\\s+ON\\s+FILE.*")){
				parsedResponse.setError("<font color=\"red\">No results found</font>");
				return;
			}
			if(response.contains("Web server received an invalid response") || response.contains("404 Program Not Found")){
				parsedResponse.setError("<font color=\"red\">Official site error</font>");
				return;
			}

			String details = getDetails(response);
			
			if(details == null){
				parsedResponse.setError("<font color=\"red\">Error retrieving page</font>");
				return;
			}
			
			// isolate pin number
			String keyCode = "File";
			
			String parcelNo = StringUtils.extractParameterFromUrl(Response.getRawQuerry(), "PARCEL");
			
			parcelNo = parcelNo.replaceAll("-","");
			
			Matcher mat = taxYear.matcher(details);
			if (mat.find()){
				parcelNo += mat.group(1);
			}
			
			if(StringUtils.isNotEmpty(parcelNo))
			{
				keyCode = parcelNo.trim();
				//keyCode = keyCode.replaceAll("-","");
			} 
			
			if ((!downloadingForSave))
			{
                
                String qry_aux = Response.getRawQuerry();
				qry_aux = "dummy=" + keyCode + "&" + qry_aux;
				String originalLink = sAction + "&" + qry_aux;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
				
                if (FileAlreadyExist(keyCode + ".html")) 
				{
                	details += CreateFileAlreadyInTSD();
				}
				else 
				{
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
				}

				parsedResponse.setPageLink(
					new LinkInPage(
						sSave2TSDLink,
						originalLink,
						TSServer.REQUEST_SAVE_TO_TSD));
				parsedResponse.setResponse(details);
            } 
			else 
            {      
                msSaveToTSDFileName = keyCode + ".html";
                parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                parsedResponse.setResponse(details);
                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();                
                smartParseDetails(Response, details);	                                
			}
			break;
		
		case ID_GET_LINK :
			
			break;
		
		case ID_SAVE_TO_TSD:
			
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;				
			break;	
		default:
			break;
		}
	}
	
	protected String getDetails(String response){
		
		// if from memory - use it as is
		if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(response, "<HTML")) {
			return response;
		}
		String contents = response;
		contents = contents.replaceAll("(?is)<a[^>]*>.*?</a>", "");
		contents = contents.replaceAll("(?is)<form[^>]+>.*?</form>", "");
		contents = contents.replaceFirst("(?is)<table[^>]*>", "<table>").replaceFirst("(?is)(<table)(>)", "$1 border=\"1\" width=\"85%\" $2");
		
		List<String> toRemove = new ArrayList<String>();
		Matcher ma = Pattern.compile("(?is)(<table[^>]*>(?:\\s*<[^>]*>)*VISA\\s*/\\s*MASTERCARD(?:.*?</table>)*)(?:\\s*<[^>]*>)+\\s*$").matcher(contents);
		while (ma.find()) {
			String table = ma.group(1);
			if (table.contains("VISA/Mastercard") || table.contains("ACH/eCheck Payment")
					|| table.contains("American Express Payment")) {
				toRemove.add(table);
			}
		}
		for (int i=0;i<toRemove.size();i++) {
			contents = contents.replace(toRemove.get(i), "");
		}
		
		contents = addDetails(contents, response);
		contents = contents.replaceAll("(?is)</?div[^>]*>", "");
		contents = contents.replaceAll("[\\x00]+", "");	
		contents = Tidy.tidyParse(contents, null);
		contents = contents.replaceAll("(?is)<br\\b[^>]*>", "");
		contents = contents.replaceAll("(?is)</?(html|body|head)[^>]*>", "").replaceFirst("(?s)^.*?(<)", "$1");
		contents = contents.replaceAll("(?is)Will\\s*County\\s*,\\s*Illinois", "");
		return contents;
	}
	
	private String addDetails(String contents, String response) {
		
		Matcher linkMat = fiveYearTaxInquiryPat.matcher(response);
		HTTPResponse res = null;
		HTTPRequest req = null;
		String resp = "";
		if (linkMat.find()){
			req = new HTTPRequest(link + linkMat.group(1));
			
        	HttpSite site = HttpManager.getSite("ILWillTR", searchId);
			try
			{
				res = site.process(req);
			} finally 
			{
				HttpManager.releaseSite(site);
			}
			resp = res.getResponseAsString();
			resp = resp.replaceFirst("(?is)<table[^>]*>", "<table>").replaceFirst("(?is)(<table)(>)", "$1 width=\"100%\" $2");
			contents += resp;
		}
		
		linkMat.reset();
		linkMat = taxDetailInquiryPat.matcher(response);
		if (linkMat.find()){
			req = new HTTPRequest(link + linkMat.group(1));
			HttpSite site = HttpManager.getSite("ILWillTR", searchId);
			try
			{
				res = site.process(req);
			} finally 
			{
				HttpManager.releaseSite(site);
			}
			resp = res.getResponseAsString();
			resp = resp.replaceFirst("(?is)<table[^>]*>", "<table>").replaceFirst("(?is)(<table)(>)", "$1 width=\"100%\" $2");
			contents += resp;
		}
		
		return contents;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
		ro.cst.tsearch.servers.functions.ILWillTR.parseAndFillResultMap(detailsHtml, resultMap, searchId);
		return null;
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