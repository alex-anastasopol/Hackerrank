/**
 * 
 */
package ro.cst.tsearch.servers.types;

import static org.apache.commons.lang.StringUtils.getNestedString;
import static ro.cst.tsearch.utils.StringUtils.extractParameter;
import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

/**
 * @author radu bacrau
 * 
 * 
 * @category is used by FLBrevard, FLDuval, FLEscambia,  
						FLNassau, FLPolk, FLSantaRosa, FLStJohns, FLSumter, FLWalton, TNDavidsonTR,
						FLFlaglerTR, FLColumbiaTR, FLGilchristTR, FLHendryTR
	if you add a new county, PLEASE add it here in this list
	if you modify this generic class PLEASE check all the counties from this list.
	Thank you.
 */

public class FLGenericGovernmaxTR extends TSServer {

	private static final long serialVersionUID = 23423452353454L;

	protected static Logger logger = Logger.getLogger(FLGenericGovernmaxTR.class);
	
	private boolean downloadingForSave = false;
	
	
	private static final Pattern firstPattern = Pattern.compile("(?i)<A HREF=\"(?:\\.\\./\\.\\./)?((?:agency/[^/]+/)?list_collect[a-z0-9._-]+asp)\\?(r=[^&]*&l_mv=first[^\"]+)\"");
	private static final Pattern prevPattern = Pattern.compile("(?i)<A HREF=\"(?:\\.\\./\\.\\./)?((?:agency/[^/]+/)?list_collect[a-z0-9._-]+asp)\\?(r=[^&]*&l_mv=previous[^\"]+)\"");
	private static final Pattern nextPattern = Pattern.compile("(?i)<A HREF=\"(?:\\.\\./\\.\\./)?((?:agency/[^/]+/)?list_collect[a-z0-9._-]+asp)\\?(r=[^&]*&l_mv=next[^\"]+)\"");
	private static final Pattern lastPattern = Pattern.compile("(?i)<A HREF=\"(?:\\.\\./\\.\\./)?((?:agency/[^/]+/)?list_collect[a-z0-9._-]+asp)\\?(r=[^&]*&l_mv=last[^\"]+)\"");
	private static final Pattern legalPattern = Pattern.compile("(?i)<A HREF=\"(?:\\.\\./\\.\\./)?((?:agency/[^/]+/)?tab_collect_mvplgl[a-z0-9._-]+asp\\?[^\"]+)\"");		
	private static final Pattern historyPattern = Pattern.compile("(?i)<A HREF=\\\"(?:\\.\\./\\.\\./)?((?:agency/[^/]+/)?tab_collect_payhist[A-Za-z0-9._-]+asp\\?[^\\\"]+)\\\"");
	private static final Pattern historyPatternNew = Pattern.compile("(?ism)<A HREF=\"([^\"]*tab_collect_payhist[A-Za-z0-9._-]+asp\\?[^\"]*)\"");
	
	protected CheckTangible checkTangible = null;
	
	protected interface CheckTangible {
		public boolean isTangible(String row);
	}
	
	/**
	 * Get the current server link
	 * @return
	 */
	private String getCrtServerLink(){
		String link = getDataSite().getLink();
		int idx = link.indexOf("/collectmax/collect30.asp");
		if(idx == -1){
			throw new RuntimeException("County " + getCurrentServerName() + " not supported by this class!");
		}
		return link.substring(0, idx);		
	}

	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 * @param miServerID
	 */
	public FLGenericGovernmaxTR(String rsRequestSolverName, String rsSitePath, String rsServerID,
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
		String county = dataSite.getCountyName();
		
		if ("Sumter".equals(county)) { //task 9221
			TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			if (tsServerInfoModule != null) {
				tsServerInfoModule.setVisible(false); 
			}
		}
		
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
		module.clearSaKeys();
		module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
		module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
		ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
		.getConfigurableNameIterator(module, searchId, new String[] {"L F M;;","L;F;","L;f;"});
		module.addIterator(nameIterator);
		modules.add(module);
		
		serverInfo.setModulesForAutoSearch(modules);
	}

	
	@SuppressWarnings("deprecation")
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID)
			throws ServerResponseException {
		
		Response.getParsedResponse().setAttribute("checkTangible", checkTangible);
		
		// get the HTML and make a copy
		String response = Response.getResult();
		String initialResponse = response;
		String linkStart = CreatePartialLink(TSConnectionURL.idGET);
		
	    try
	    {
	    	response = new String (response.getBytes(),"UTF-8");//ISO-8859-1
	    	initialResponse = new String (initialResponse.getBytes(),"UTF-8");//ISO-8859-1
	    }
	    catch (Exception e)
	    {
			Response.getParsedResponse().setError("Error occured when converting to UTF-8.");
			logger.error("ParseResponse END. Error occured: " + e.getStackTrace().toString());
	    	return;
	    }

		// check for no results
		if(response.matches("(?is).*No\\s*records\\s*found.*")){
			return;
		}
		
		// check for errors
		String error = getNestedString(response, "<FONT COLOR=Red Size=-1>", "</FONT>");
		if(!isEmpty(error)){
			Response.getParsedResponse().setError("Error occured: " + error);
			logger.error("ParseResponse END. Error occured: " + error);
			return;
		}
		
		switch(viParseID){
		
		case ID_SEARCH_BY_PARCEL: // Control Number
		case ID_SEARCH_BY_INSTRUMENT_NO: // Geo Number // Certificate for DUVAL
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_SUBDIVISION_NAME:
		case ID_SEARCH_BY_TAX_BIL_NO:
		case ID_GET_LINK:
					
			// check if it is a details document
			if(response.matches("(?is).*Tax\\s*Record.*")||response.matches("(?is).*Tax\\s+Districts\\s+Detail.*")){
				ParseResponse(sAction, Response, ID_DETAILS);				
				return;
			} 
									
			// extract intermediate results table
			String results = getIntermResults(response, linkStart);
			if(isEmpty(results)){
				return;
			}
			
			// parse the results
			if (this instanceof FLColumbiaTR || this instanceof FLGilchristTR
					|| this instanceof FLHendryTR){
				StringBuilder outputTable = new StringBuilder();
				smartParseIntermediary(Response, results, outputTable);
			}else{
				results = results.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");
				parser.Parse( Response.getParsedResponse(), results, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
			}
			
			
			// treat navigation
			String firstLink = "";
			String prevLink = "";
			String nextLink = extractLink(initialResponse, nextPattern, linkStart, "Next&gt;");
			String lastLink = "";

			if (mSearch.getSearchType() != Search.AUTOMATIC_SEARCH)
			{
				String countInfo = "";
				firstLink = extractLink(initialResponse, firstPattern, linkStart, "&lt;&lt;First");
				prevLink = extractLink(initialResponse, prevPattern, linkStart, "&lt;Prev");
				lastLink = extractLink(initialResponse, lastPattern, linkStart, "Last&gt;&gt;");
				String pos = extractParameter(initialResponse, "Page<BR>(\\d+&nbsp;of&nbsp;\\d+)");
				if(!isEmpty(pos)){ 
					countInfo += "page " + pos + "";
				}
				if(prevLink != null){
					countInfo = prevLink + "&nbsp;&nbsp;" + countInfo;
				} else {
					countInfo = "<font color='#DDDDDD'>&lt;Prev</font>" + "&nbsp;&nbsp;" + countInfo;
				}
				if(firstLink != null){
					countInfo = firstLink + "&nbsp;&nbsp;" + countInfo;
				} else {
					countInfo = "<font color='#DDDDDD'>&lt;&lt;First</font>" + "&nbsp;&nbsp;" + countInfo;
				}
				if(nextLink != null){
					countInfo = countInfo + "&nbsp;&nbsp;" + nextLink;
				} else {
					countInfo = countInfo + "&nbsp;&nbsp;" + "<font color='#DDDDDD'>Next&gt;</font>";
				}
				if(lastLink != null){
					countInfo = countInfo + "&nbsp;&nbsp;" + lastLink;
				} else {
					countInfo = countInfo + "&nbsp;&nbsp;" + "<font color='#DDDDDD'>Last&gt;&gt;</font>";
				}
				countInfo = "<b>" + countInfo + "</b>";			
				
				Response.getParsedResponse().setFooter(Response.getParsedResponse().getFooter() + countInfo);
				Response.getParsedResponse().setHeader(Response.getParsedResponse().getHeader() + countInfo + "<br/>");
			}
			
			if (mSearch.getSearchType() == Search.AUTOMATIC_SEARCH)
			{
				Response.getParsedResponse().setNextLink(nextLink);
			}
			
			break;
			
		case ID_DETAILS:
			
			String details = response;
			int crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getCountyId().intValue();
			response = response.replaceAll("(?is)if.?paid.?by", "If Paid By");
			
			// make sure we do not get the legal and history twice
			if(response.matches("(?is).*<\\s*html\\s*>.*")){
				details = getDetails(response).replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");
				if(isEmpty(details)){
					Response.getParsedResponse().setError("Details not found!");
					logger.error("ParseResponse END: Details NOT found! (" + searchId + ")");
					return;
				}
			}
			HtmlParser3 htmlParser3 = new HtmlParser3(details);
			NodeList nodeList = htmlParser3.getNodeList();
			
			// get key
			String keyNumber = getKeyNumber(details);
			if(isEmpty(keyNumber)){
				Response.getParsedResponse().setError("No results found!");
				logger.error("ParseResponse END: Account Number NOT Found!(" + searchId + ")");
				return;
			}
			
			if (crtCounty==CountyConstants.FL_Brevard || crtCounty==CountyConstants.FL_Hendry){
				keyNumber = getKeyNumberFromGEONumber(details, nodeList, Response);
				if(isEmpty(keyNumber)){
					Response.getParsedResponse().setError("No results found!");
					logger.error("ParseResponse END: GEO  Number NOT Found!(" + searchId + ")");
					return;
				}
			} else if (crtCounty==CountyConstants.FL_Escambia){
				keyNumber = keyNumber.replaceAll("-", "");
			}
			
			if (!downloadingForSave) {
				
				// not saving
				String qry = Response.getQuerry().replace("%&", "%25&");
				String originalLink = sAction + "&" + qry + "&dummy=" + keyNumber;
				originalLink = originalLink.replace("%&", "%25&");
				String sSave2TSDLink = linkStart + originalLink;
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","CNTYTAX");
				String year = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Tax Year"), "", false).trim();
				if (StringUtils.isNotEmpty(year)) {
					data.put("year", year);
				}

				if(isInstrumentSaved(keyNumber, null, data)){
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);					
				}
				
				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
			
				parser.setTsserver(this);
				parser.Parse(Response.getParsedResponse(), details, Parser.NO_PARSE);
				
				//parser.Parse(Response.getParsedResponse(), details, Parser.PAGE_DETAILS);
				
			} else {
				
        		//saving
                msSaveToTSDFileName = keyNumber + ".html" ;
                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();       
                
                
                
                if (this instanceof FLColumbiaTR || this instanceof FLGilchristTR
                		|| this instanceof FLHendryTR){
                	smartParseDetails(Response, details);
                }
                else{
                	parser.setTsserver(this);
                	parser.Parse(Response.getParsedResponse(),details,Parser.PAGE_DETAILS,getLinkPrefix(TSConnectionURL.idGET),TSServer.REQUEST_SAVE_TO_TSD);
                }
                
                Response.getParsedResponse().setOnlyResponse(details);
			}
			
			break;

        case ID_SAVE_TO_TSD :     
        	
            downloadingForSave = true;
            ParseResponse(sAction, Response, ID_DETAILS);
            downloadingForSave = false;
            
            break;    		
		}
		
	}

	/**
	 * Extract legal description
	 * @param page
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private String getLegal(String html){
		String page = getNestedString(html, "<!--START TAB-->", "<!-- End section_main Table -->");
		int istart = page.indexOf("<B>Legal Description");
		if (istart == -1) {
			return "";
		}
		int iend = page.indexOf("</TABLE>", istart);
		if (iend == -1) {
			return "";
		}
		iend += "</TABLE>".length();
		istart = page.lastIndexOf("<TABLE", istart);
		if (istart == -1) {
			return "";
		}
		String legal = page.substring(istart, iend);
		return legal;
	}
	
	/**
	 * Extract intermediate results table
	 * @param response
	 * @return
	 */
	protected String getIntermResults(String response, String linkStart){
		
		int istart, iend;
		istart = response.indexOf("<!--START LIST-->"); 
		istart = response.indexOf("<TABLE", istart);    
		istart += "<TABLE".length();
		istart = response.indexOf("<TABLE", istart);    
		iend = response.indexOf("<!--END LIST-->");     
		iend = response.lastIndexOf("</TABLE", iend);   
		iend -= 1;
		iend = response.lastIndexOf("</TABLE>", iend);  
		response = response.replaceAll("(?is)\\bTEST 2\\b", "");
		if(istart > 0 && iend > 0 && istart < iend){	
			// isolate table
			String retVal = response.substring(istart, iend + "</TABLE>".length());
			
			// rewrite details links
			retVal = retVal.replaceAll(
					//tab_collect_taxmgr3.asp?t_nm=collect_taxmgr
					"(?i)<A HREF=\"(?:\\.\\./\\.\\./)?((?:agency/[^/]+/)?tab_collect_[a-z0-9._-]+asp)\\?([^\"]+)\"\\s+CLASS=\"listlink\">",
					"<a href=\""+linkStart+"/collectmax/$1&$2&useRawLink=true\">");
			
			// clean up formatting
			retVal = retVal.replaceAll("\\s*BGCOLOR=\"[^\"]+\"", "");
			
			// fix non-breaking spaces
			retVal = retVal.replace('\u00a0', ' ');
			
			// fix the underline and colors
			retVal = retVal.replaceAll("(?i)</?u>", "");
			retVal = retVal.replaceAll("(?i) COLOR=\"[^\"]+\"", "");
			// remove all img tags
			retVal = retVal.replaceAll("(?is)<img[^>]*>", "");
			if(this instanceof TNDavidsonTR) {
				retVal = Tidy.tidyParse(retVal, null);
			}
			if (CountyConstants.FL_Gilchrist==getDataSite().getCountyId()) {
				retVal = retVal.replaceAll("(?is)(<a[^>]+href=\"[^\"]*)tab_collect_payhistv5\\.4\\.asp([^\"]*\"[^>]*>)", "$1tab_collect_mvptaxV5.6.asp$2");
			}
			return retVal;
		} else {
			return "";
		}
	}
	
	protected String cleanup(String page){
		String cleaned = page;
		cleaned = cleaned.replaceAll("(?s)>[^<]+<", "><");
		cleaned = cleaned.replaceAll("(?i)<\\s*/?(B|FONT|INPUT|FORM)[^>]*>", "");
		cleaned = cleaned.replaceAll("(?i)<TD[^>]*></TD>", "");
		cleaned = cleaned.replaceAll("(?i)<TR[^>]*></TR>", "");
		cleaned = cleaned.replaceAll("(?i)<TABLE[^>]*></TABLE>", "");
		cleaned = cleaned.replaceAll("(?i)<TABLE[^>]*><TR[^>]*><TD[^>]*></TABLE>", "");
		cleaned = cleaned.replaceFirst("(?i)<!--[^-]+--></TD></TR>(</TABLE>)", "$1");
		return cleaned;
	}
	
	@SuppressWarnings("deprecation")
	private String getHistoryContents(String page){
		//long timestamp = System.nanoTime();
		
		String retVal = getNestedString(page, "<!--START TAB-->", "<!-- End section_main Table -->");
		
		//FileUtils.writeTextFile("d:/history_" + timestamp  + "_1.html", retVal);
		int idx1 = retVal.indexOf("Account Number");
		if(idx1 == -1){
			idx1 = retVal.indexOf("Account#");
		}		
		if(idx1 == -1){
			return retVal;
		}
		idx1 = retVal.lastIndexOf("<TABLE", idx1);
		if(idx1 == -1){
			return retVal;
		}
		
		String part1 = retVal.substring(0, idx1);
		String part2 = retVal.substring(idx1);
		
		retVal = cleanup(part1) +  part2;
		//FileUtils.writeTextFile("d:/history_" + timestamp  + "_2.html", retVal);
		return retVal;
		
	}
	
	/**
	 * Get the relevant contents of a page
	 * @param page
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private String getMainContents(String page){
		
		//long timestamp = System.nanoTime();
		
		String retVal = getNestedString(page, "<!--START TAB-->", "<!-- End section_main Table -->");
		
		//FileUtils.writeTextFile("d:/main_" + timestamp  + "_1.html", retVal);
		
		int idx1 = retVal.indexOf("Account Number");
		if(idx1 == -1){
			idx1 = retVal.indexOf("Account#");
		}
		if(idx1 == -1){
			return retVal;
		}
		idx1 = retVal.lastIndexOf("<TABLE", idx1);
		if(idx1 == -1){
			return retVal;
		}
		
		int idx2 = retVal.lastIndexOf("Prior Years Due");
		int tmp = retVal.lastIndexOf("Prior Year Taxes Due");
		if(tmp > idx2){
			idx2 = tmp;
			idx2 = retVal.indexOf("<TABLE", idx2);
		}
		if(idx2 == -1){
			return retVal;
		}
		idx2 = retVal.indexOf("<TABLE", idx2 + 1);
		if(idx2 == -1){
			return retVal;
		}
		
		String part1 = retVal.substring(0, idx1);
		String part2 = retVal.substring(idx1, idx2);
		String part3 = retVal.substring(idx2);

		retVal = cleanup(part1) + part2 + cleanup(part3);
		
		//FileUtils.writeTextFile("d:/main_" + timestamp  + "_2.html", retVal);
		
		return retVal; 
	}
	
	/**
	 * 
	 * @param response
	 * @return
	 */
	protected String getDetails(String response){
		
		// isolate details
		String details = getMainContents(response);		
		if(isEmpty(details)){
			return details;
		}

		// remove all links
		details = details.replaceAll("(?i)</?\\s*a[^>]*>", "");	
		
		// remove all input controls
		details = details.replaceAll("(?i)<input[^>]*>", "");
		details = details.replaceAll("(?i)</?form[^>]*>", "");
		
		// deal with legal description
		Matcher legalMatcher =  legalPattern.matcher(response);
		if(legalMatcher.find()){
			String legalLink = getCrtServerLink() + "/collectmax/" + legalMatcher.group(1);
			String legal = getLegal(getLinkContents(legalLink));
			//FileUtils.writeTextFile("d://prior.html", details);
			//FileUtils.writeTextFile("d://legal.html", legal);
			details = details.replaceFirst(
					"(?i)Legal Description(\\( click for full description\\))?",
					"</B></FONT>" + legal + "<FONT COLOR=\"black\"><B>");
			details = details.replaceAll("(?i)\\(click for full description\\)", "");
			//FileUtils.writeTextFile("d://after.html", details);
		}
		
		// deal with tax history extract tax history from an idenfied link in response
		Matcher historyMatcher = taxHistoryPattern(response);
		
		String historyLink = "";
		
		if(historyMatcher.find()){			
			historyLink = getCrtServerLink() + "/collectmax/" + historyMatcher.group(1);
		} else {
			historyMatcher = historyPatternNew.matcher(response);
			if(historyMatcher.find()){	
				historyLink = historyMatcher.group(1);
			}
		}
		
		if(StringUtils.isNotEmpty(historyLink)){
			String history = getHistoryContents(getLinkContents(historyLink));
			details = details.replace("Prior Years Payment History", "<b><font color='black'>See below the Prior Years Payment History</font></b>");
			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(history, null);
				NodeList tableList = htmlParser.extractAllNodesThatMatch(new TagNameFilter("table"));
				tableList.remove(0);
				for (int i = 0; i < tableList.size(); i++){
					TableTag table = (TableTag) tableList.elementAt(i);
					if (table.toPlainTextString().toLowerCase().contains("folio")){
						table.setAttribute("id", "paymentHistory");
					}
					table.removeAttribute("width");
					table.setAttribute("width", "600");
				}
				history = tableList.toHtml();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			details += "<br/><b><font color='black'>Prior Years Payment History</font></b>" + history;
		}
		
		// remove all links
		details = details.replaceAll("(?i)</?(A|FORM|INPUT)[^>]*>", "");
		
		// fix non-breaking spaces
		details = details.replace('\u00a0', ' ');
		
		// remove all img tags
		details = details.replaceAll("(?is)<img[^>]*>", "");
		
		details = details.replace('\uFFFD', ' ');
		
		return details;
	}

	protected Matcher taxHistoryPattern(String response) {
		return historyPattern.matcher(response);
	}
	/**
	 * 
	 * @param response
	 * @param pattern
	 * @param linkStart
	 * @param label
	 * @return
	 */
	protected String extractLink(String response, Pattern pattern, String linkStart, String label){
		Matcher matcher = pattern.matcher(response);
		if(!matcher.find()){
			return null;
		} else {
			return "<a href='" + linkStart + "/collectmax/" + matcher.group(1) + "&" + matcher.group(2) + "'>" + label + "</a>";
		}
	}
	
	/**
	 * 
	 * @param page
	 * @return
	 */
	protected String getKeyNumber(String page){
		int istart = page.indexOf("<B>Tax Year&nbsp;");  if(istart == -1){ return ""; }
		istart = page.indexOf("<B>", istart + 1);        if(istart ==-1){ return ""; }
		int iend = page.indexOf("</B>", istart);         if(iend == -1){ return ""; }
		String info = page.substring(istart, iend);
		info = info.replaceAll("[^><a-zA-Z0-9-]", "");
		info = info.replaceAll("<[^>]*>","");
		return info;
	}
	
	protected String getKeyNumberFromGEONumber(String page, NodeList nodeList, ServerResponse serverResponse){
		int istart = page.indexOf("<B>GEO Number");  if(istart == -1){ return ""; }
		istart = page.indexOf("<BR>", istart + 1);        if(istart ==-1){ return ""; }
		int iend = page.indexOf("</FONT>", istart);         if(iend == -1){ return ""; }
		String info = page.substring(istart, iend);
		info = info.replaceAll("[^><a-zA-Z0-9-]", "");
		info = info.replaceAll("<[^>]*>","");
		return info;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected String getFileNameFromLink(String url) {
		String keyCode = "";
		if (url.contains("dummy=")){
			keyCode = getNestedString(url, "dummy=", "&");
		} else {
			
		}
		return keyCode + ".html";
	}
	
	/**
	 * 
	 * @param p
	 * @param pr
	 * @param htmlString
	 * @param pageId
	 * @param linkStart
	 * @param action
	 * @throws ro.cst.tsearch.exceptions.ServerResponseException
	 */
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId,
			String linkStart, int action) throws ro.cst.tsearch.exceptions.ServerResponseException {

		String newHtmlString = cleanHtmlResponseTable(pr, htmlString);
		p.splitResultRows(pr, newHtmlString, pageId, "<Tr>", "</table>", linkStart, action);
	}

	public static String cleanHtmlResponseTable(ParsedResponse pr, String htmlString) {
		CheckTangible checkTangible = (CheckTangible)pr.getAttribute("checkTangible");
		
		StringBuilder sb = new StringBuilder();
		
		int i1 = htmlString.indexOf("a href="); 
		if(i1 == -1){
			i1 = htmlString.indexOf("A HREF=");
		}
		i1 = htmlString.lastIndexOf("<TR>", i1);
		int i2 = htmlString.indexOf("<HR>", i1);
		i2 = htmlString.lastIndexOf("</TR", i2);
		
		sb.append("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=3>");
		while(i1 > 0 && i2 > 0 && i1 < i2){
			String row = "<Tr>" + htmlString.substring(i1 + "<TR>".length(), i2) + "</Tr>";
			
			if(checkTangible.isTangible(row)){
				// row = row.replaceFirst("(?is)<a\\s*[^>]*>",""); - links not removed anymore. approved by TM
				row = row.replaceFirst("(?is)</a>","- <b>TANGIBLE</b>");				
			}
			
			sb.append(row);			
			i1 = htmlString.indexOf("a href", i2);
			if(i1 == -1){
				i1 = htmlString.indexOf("A HREF=", i2);
			}			
			i1 = htmlString.lastIndexOf("<TR>", i1);
			i2 = htmlString.indexOf("<HR>", i1);
			i2 = htmlString.lastIndexOf("</TR", i2);						
		}
		
		sb.append("</table>");
		String newHtmlString = sb.toString();
		return newHtmlString;
	}

	@Override
    public ServerResponse GetLink(String vsRequest, boolean vbEncoded) throws ServerResponseException {
		ServerResponse sr = super.GetLink(vsRequest, vbEncoded);
    	return sr;
    }
	
	protected static String getSearchdat5(String row){
		String searchdat5 = extractParameter(row, "searchdat5=([^&]+)");
		searchdat5 = searchdat5.replace("+","");
		searchdat5 = searchdat5.replace("%2D","-");
		searchdat5 = searchdat5.replace("%2E",".");	
		searchdat5 = searchdat5.replace("%2F","/");
		return searchdat5;
	}
	
	protected static String getLinkText(String row){
		String searchdat5 = extractParameter(row, "(?is)\\s*<a[^>]*>\\s*([^<]+)\\s*</a>");
		searchdat5 = searchdat5.replaceAll("[\n\r\t]", " ").trim();
		searchdat5 = searchdat5.replace("+","");
		searchdat5 = searchdat5.replace("%2D","-");
		searchdat5 = searchdat5.replace("%2E",".");	
		searchdat5 = searchdat5.replace("%2F","/");
		return searchdat5;
	}
	
	protected boolean hasAddress(){
		return !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.P_STREET_FULL_NAME_EX));
	}
	
	protected boolean hasName(){
		return !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.OWNER_LFM_NAME));	
	}
	
	protected boolean hasLegal(){
		return GenericLegal.hasLegal(getSearchAttributes());		
	}
}
