package ro.cst.tsearch.servers.types;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasChildFilter;
import org.htmlparser.filters.StringFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

	/**
	 * @author mihaib
	 */

public class ILWillTU extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 

	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
	private static final Pattern EACH_YEAR_PATTERN = Pattern.compile("(?is)<a\\s+href\\s*=\\s*\\\"(CCWTX23\\?[^\\\"]{17,})");
	private static final String link = "http://willtax.willcountydata.com/maintax/"; 
	
	
	public ILWillTU(long searchId) {
		super(searchId);
	}

	public ILWillTU(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
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
				
		switch(viParseID){
		
		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
			
			response = response.replaceAll("[\\x00]+", "");
			
			if(response.matches("(?is).*PARCEL\\s+IS\\s+NOT\\s+ON\\s+FILE.*")){
				String message = RegExUtils.getFirstMatch("<font color=\"#FF0000\">(.*?)</font>", response, 1);
				if(StringUtils.isEmpty(message)) {
					message = "No results found.";
				}
				Response.getParsedResponse().setError("<font color=\"red\">" + message + "</font>");
				return;
			}

			String details = getDetails(response);
			
			if(details == null){
				Response.getParsedResponse().setError("<font color=\"red\">Error retrieving page</font>");
				return;
			}
			
			// isolate pin number
			String keyCode = "File";
			
			String parcelNo = StringUtils.extractParameterFromUrl(Response.getRawQuerry(), "PARCEL");
			
			parcelNo = parcelNo.replaceAll("-","");
			
			if(StringUtils.isNotEmpty(parcelNo)){
				keyCode = parcelNo.trim();
			} 
			
			if ((!downloadingForSave)){
                
                String qry_aux = Response.getRawQuerry();
				qry_aux = "dummy=" + keyCode + "&" + qry_aux;
				String originalLink = sAction + "&" + qry_aux;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "CNTYTAX");
				data.put("dataSource", "TU");
				
                if (isInstrumentSaved(parcelNo, null, data)){
                	details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
				}

				Response.getParsedResponse().setPageLink(
					new LinkInPage(
						sSave2TSDLink,
						originalLink,
						TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
            } 
			else {
				smartParseDetails(Response, details);
                
				msSaveToTSDFileName = keyCode + ".html";
                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();                                             
			}
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
		if(!response.contains("<HTML")){
			return response;
		}
		
		response = response.replaceAll("(?is)<html.*?<body>", "");
		response = response.replaceAll("(?is)</body>.*</html>", "");
		
		String contents = "";
		
		contents = response;
		contents = contents.replaceAll("(?is)<form[^>]+>.*?</form>", "");
		contents = contents.replaceAll("(?is)<a[^>]+>\\s*Return[^<]+</a>", "");
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");
		
		contents = addDetails(contents, response);
		
		return contents;
	}
	
	private String addDetails(String contents, String response) {
		
		Matcher linkMat = EACH_YEAR_PATTERN.matcher(response);

		String resp = "";
		while (linkMat.find()){
			String url = link + linkMat.group(1);
			
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				resp = ((ro.cst.tsearch.connection.http2.ILWillTU) site).getPage(url);
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(site);
			}
			if (StringUtils.isNotEmpty(resp)){
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp, null);
					NodeList mainList = htmlParser.parse(null);
					NodeList body = mainList.extractAllNodesThatMatch(new TagNameFilter("body"), true);
					if (body != null && body.size() > 0){
						String sds = body.elementAt(0).getChildren().toHtml();
						sds = sds.replaceAll("(?is).*<div[^>]*>\\s*(<CENTER[^>]*>\\s*<table.*</p>\\s*</td>\\s*</tr>\\s*</table>).*", "$1 </center>");
						contents += "<br/ ><br/ >" + sds;
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		}
		
		return contents;
	}

	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TU");
			
			if(detailsHtml != null) {
				try {
					org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(detailsHtml, null);
					NodeList nodeList = parser.parse(null);
					
					String pin = HtmlParser3.findNode(nodeList, "Permanent Index Number").getText();
					if (StringUtils.isNotEmpty(pin)){
						pin = pin.replaceAll("(?is)[^:]+:", "").replaceAll("(?is)\\p{Punct}", "");
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pin.trim());
					}
					
					NodeList amtPaidList = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), true);
					amtPaidList.keepAllNodesThatMatch(new HasChildFilter(new StringFilter("Total Amount Paid"), true));
					
					String amtPaid = "0.00";
					if (amtPaidList != null){
						for (int i = 0; i < amtPaidList.size(); i++) {
							amtPaid += "+" + amtPaidList.elementAt(i).getNextSibling().getChildren().extractAllNodesThatMatch(new StringFilter()).toHtml().replaceAll("[\\$,]", "").trim();
						}
						resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), GenericFunctions.sum(amtPaid, searchId));
					}
					
					NodeList totDueList = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), true);
					totDueList.keepAllNodesThatMatch(new HasChildFilter(new StringFilter("Total Amount to Redeem"), true));
					
					String totDue = "0.00";
					if (totDueList != null){
						for (int i = 0; i < totDueList.size(); i++) {
							totDue += "+" + totDueList.elementAt(i).getNextSibling().getChildren().extractAllNodesThatMatch(new StringFilter()).toHtml().replaceAll("[\\$,]", "").trim();
						}
						resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), GenericFunctions.sum(totDue, searchId));
					}

					
				} catch (Exception e) {
					e.printStackTrace();
				}		
			}
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