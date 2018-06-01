package ro.cst.tsearch.servers.types;


import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.htmlparser.Text;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;

/**
 * @author mihaib
 * 
 * for TX Bexar TR, Ellis TR, Dallas TR, Nueces TR, San Patricio TR -like sites        
 * ADD here the new county implemented with this Generic
 * 
 */

public class TXGenericACTTR extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 
	private static final Pattern PAYMENT_HISTORY_LINK = Pattern.compile("(?is)<a\\s+href\\s*=\\s*\"(reports/paymentinfo[^\\\"]+)");

	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
	
	public TXGenericACTTR(long searchId) {
		super(searchId);
	}

	public TXGenericACTTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
			
		if(tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(2), EXTRA_CRITERIA_SELECT);
			if (tsServerInfoModule.getFunctionCount() > 5){
				setupSelectBox(tsServerInfoModule.getFunction(5), IGNORE_ZERO_BALANCE_ACCOUNTS_SELECT);
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if(tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(2), EXTRA_CRITERIA_SELECT);
			if (tsServerInfoModule.getFunctionCount() > 5){
				setupSelectBox(tsServerInfoModule.getFunction(5), IGNORE_ZERO_BALANCE_ACCOUNTS_SELECT);
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
		if(tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(2), EXTRA_CRITERIA_SELECT);
			if (tsServerInfoModule.getFunctionCount() > 5){
				setupSelectBox(tsServerInfoModule.getFunction(5), IGNORE_ZERO_BALANCE_ACCOUNTS_SELECT);
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		if(tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(2), EXTRA_CRITERIA_SELECT);
			if (tsServerInfoModule.getFunctionCount() > 5){
				setupSelectBox(tsServerInfoModule.getFunction(5), IGNORE_ZERO_BALANCE_ACCOUNTS_SELECT);
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.TAX_BILL_MODULE_IDX);
		if(tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(2), EXTRA_CRITERIA_SELECT);
			if (tsServerInfoModule.getFunctionCount() > 5){
				setupSelectBox(tsServerInfoModule.getFunction(5), IGNORE_ZERO_BALANCE_ACCOUNTS_SELECT);
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.TYPE_NAME_MODULE_IDX);
		if(tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(2), EXTRA_CRITERIA_SELECT);
			if (tsServerInfoModule.getFunctionCount() > 5){
				setupSelectBox(tsServerInfoModule.getFunction(5), IGNORE_ZERO_BALANCE_ACCOUNTS_SELECT);
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX);
		if(tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(2), EXTRA_CRITERIA_SELECT);
			if (tsServerInfoModule.getFunctionCount() > 5){
				setupSelectBox(tsServerInfoModule.getFunction(5), IGNORE_ZERO_BALANCE_ACCOUNTS_SELECT);
			}
		}
		
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);

		String pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
		
		if(this instanceof TXEllisTR)
			pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO_GENERIC_TR);
		
		String ownLast = getSearchAttribute(SearchAttributes.OWNER_LNAME);
		GenericAddressFilter addressFilter = AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		
		if (hasPin()){
			//Search by PIN
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			modules.add(module);
			
			//Search also my Cad reference No when pin is present as NB pin <=> Cad refe no
			if (this instanceof TXNuecesTR || this instanceof TXEllisTR){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(0).forceValue(pin);
				modules.add(module);
			}
		}
		
		if (hasStreet() && hasOwner()) {
			//Search by Property Address and Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(streetNo + " " + streetName);
			module.getFunction(1).forceValue(ownLast);
			module.getFunction(2).forceValue("3");
			module.getFunction(4).forceValue("8");
			module.addFilter(addressFilter);
			modules.add(module);
		}
		
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(streetNo + " " + streetName);
			module.getFunction(2).forceValue("3");
			module.getFunction(4).forceValue("8");
			module.addFilter(addressFilter);
			modules.add(module);
		}
		
		if (hasOwner()){
			//Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(addressFilter);
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(module, searchId, new String[] {"L;F;", "L;m;"});
			module.addIterator(nameIterator);
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;
		String contents = "";
		ParsedResponse parsedResponse = Response.getParsedResponse();	
		String linkStart = CreatePartialLink(TSConnectionURL.idGET);
		switch (viParseID) {
		
			case ID_SEARCH_BY_NAME :
			case ID_SEARCH_BY_ADDRESS :
			case ID_SEARCH_BY_PARCEL :
				
				StringBuilder outputTable = new StringBuilder();
				
				if (rsResponse.indexOf("There are 0 matches") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				}
				
				if (rsResponse.indexOf("No Response from Application Web Server") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">Error on the official site. Please try again!</font>");
					return;
				}
				
				if (rsResponse.indexOf("Please use at least 3 characters when entering what you want to search by") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">Please use at least 3 characters when entering what you want to search by!</font>");
					return;
				}
				
				contents = cleanIntermediaryResponse(rsResponse, linkStart);
				
			   
				try {
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, contents, outputTable);
					
					if(smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
						parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
		            }

				}catch(Exception e) {
					e.printStackTrace();
				}

				break;
					
			case ID_DETAILS :
				
				String details = "";
				details = getDetails(rsResponse);
				String pid = getPID(details);
				
				 
				if ((!downloadingForSave))
				{	
	                String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + pid + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					
					if(isInstrumentSaved(pid, null, data)){
		                details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
					
	                Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
	            } 
				else 
	            {      
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = pid + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();                
	                //parser.Parse(Response.getParsedResponse(), details, Parser.PAGE_DETAILS);	 

				}
				
				break;	
			
			case ID_GET_LINK :
				if (sAction.indexOf("showdetail") != -1){
						ParseResponse(sAction, Response, ID_DETAILS);
				} 
				
				break;
			
			case ID_SAVE_TO_TSD :

				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;
		}
	}

	protected String getPID(String details) {
		String pid = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
			NodeList mainList = htmlParser.parse(null);
			NodeList tempList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			Text pidText = null;
			//TableRow[] rows  = ((TableTag)tempList.elementAt(0)).getRows();
			for (int i = 0; i < tempList.size(); i++){
				if (tempList.elementAt(i).toHtml().contains("Account Number")){
					//rows = ((TableTag)tempList.elementAt(i)).getRows();
					pidText = HtmlParser3.findNode(tempList, "Account Number");
					break;
				}
			}
			
			if (pidText != null){
				pid = pidText.toHtml();
				pid = pid.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)Account Number\\s*:?", " ").trim();
			}

		} catch(Exception e) {
			e.printStackTrace();
		}
		return pid;
	}

	/**
	 * Implement me for cleaning the unwanted hmtl that comes from the server. 
	 * @param rsResponse
	 * @param linkStart
	 * @return
	 */
	protected String cleanIntermediaryResponse(String rsResponse, String linkStart) {
		return rsResponse;
	}
	
	protected String getDetails(String response){
		
		// if from memory - use it as is
		if(!response.toLowerCase().contains("<html")){
			return response;
		}
		
		String contents = "";
		
		contents = cleanDetailsResponse(response);
		
		Matcher mat = PAYMENT_HISTORY_LINK.matcher(response);
		if (mat.find() && 
				(this instanceof TXBexarTR 
				|| this instanceof TXDallasTR 
				|| this instanceof TXEllisTR 
				|| this instanceof TXSanPatricioTR)){
			String siteName = TSServer.getCrtTSServerName(miServerID);
			String link = getDataSite().getLink();
			link = link.replaceAll("(?is)index\\.jsp", mat.group(1));
			HTTPRequest req = new HTTPRequest(link);
			HTTPResponse res = null;
			HttpSite site = HttpManager.getSite(siteName , searchId);
			try
			{
				res = site.process(req);
			} finally 
			{
				HttpManager.releaseSite(site);
			}	
			String rsp = res.getResponseAsString();
			rsp = rsp.replaceAll("(?is).*?(<h3><b>\\s*Account No.*</table>\\s*<br[^>]*>\\s*<br[^>]*>).*", "$1");
			contents += "<h2><br><b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Payment Information</b><br></h2><br>" + rsp + "<br><br><br><br><br><br>";
			
		}
		contents = contents.replaceAll("(?is)(<col[^>]*>)\\s*(<tr>)", "$1</colgroup>$2");
		contents = contents.replaceAll("(?is)\\s+>", ">");
		contents = contents.replaceAll("(?is)(<table.*?)align\\s*=\\s*\\\"\\s*left\\s*\\\"([^>]*>)", "$1 $2");
				
		return contents;
	}

	protected String cleanDetailsResponse(String response) {
		String contents="";
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainTableList = htmlParser.parse(null);
			NodeList nodeList = HtmlParser3.getTag(mainTableList, new TableTag(), true);
			//NodeList tag = HtmlParser3.getTag(mainTableList, new HeadingTag(), true);
			//Text tableHeading = HtmlParser3.findNode(mainTableList, "Property Tax Balance");
			mainTableList.remove(nodeList.elementAt(1));
			mainTableList.remove(nodeList.elementAt(3));
			nodeList.remove(3);
			contents =
				"<table>" + "\n" +
				"<tr><td><h6></td></tr></h6>" +  
				"\n" +"<tr><td>" + nodeList.elementAt(1).toHtml() + "</td></tr>" + 
				"\n"+ "<tr><td>" + nodeList.elementAt(2).toHtml() + "</td></tr>" +
				"\n" + "</table>"; 
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		contents = contents.replaceAll("(?is)<a[^>]+>[A-Z\\s]+</a>", "");
		contents = contents.replaceAll("(?is)<!--[^-]*-->", "");
		contents = contents.replaceAll("(?is)<table[^>]*>\\s*<tr[^>]*>\\s*<td[^>]*>\\s*<h3>.*?year and jurisdiction.*", "");
		contents = contents.replaceAll("(?is)<i>\\s*Make your check.*?</font>", "");
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");
		contents = contents.replaceAll("(?is)<img[^>]+>", "");
		contents = contents.replaceAll("(?is)<form[^>]+>.*</form>", "");
		//contents = contents.replaceAll("(?is)(Property\\s+Tax\\s+Balance)", "<b>$1</b>");
		contents = contents.replaceAll("(?is)h6", "h2");
		return contents;
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			if (table.contains("Account Number")){
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
				NodeList mainTableList = htmlParser.parse(null);
				NodeList tableList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				if (tableList.size() > 0){
					TableTag mainTable = (TableTag)tableList.elementAt(0);
					
					TableRow[] rows = mainTable.getRows();
					for(TableRow row : rows ) {
						if(row.getColumnCount() > 1) {
							
							TableColumn[] cols = row.getColumns();
							NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
							if (aList.size() > 0){
								String link = ((LinkTag) aList.elementAt(0)).extractLink();
								
								String rowHtml =  row.toHtml();
								
								ParsedResponse currentResponse = new ParsedResponse();
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
								currentResponse.setOnlyResponse(rowHtml);
								currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
								
								ResultMap m = ro.cst.tsearch.servers.functions.TXBexarTR.parseIntermediaryRowTXBexarTR(row, searchId);
								m.removeTempDef();
								Bridge bridge = new Bridge(currentResponse, m, searchId);
								
								DocumentI document = (TaxDocumentI)bridge.importData();				
								currentResponse.setDocument(document);
								
								intermediaryResponse.add(currentResponse);
							}
						}
					}
					
					int countyId = Integer.parseInt(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().getCountyId());
					
					String header = "<table width=\"90%\" cellpadding=\"6\" cellspacing=\"0\" border=\"1\">"
									+ "<tr bordercolor=\"#000000\">"
									+ "<td width=\"17%\" height=\"29\"><b>Account Number</b></td>"
									+ "<td width=\"24%\" height=\"29\"><b>Owner's Name &amp; Address</b></td>"
									+ "<td width=\"24%\" height=\"29\"><b>Property Site Address</b></td>"
									+ "<td width=\"19%\" height=\"29\"><b>Legal Description</b></td>";
					if (countyId != CountyConstants.TX_Dallas){
						header += "<td width=\"16%\" height=\"29\"><b>CAD Reference No.</b></td>";
					}
					
					if (countyId == CountyConstants.TX_San_Patricio) {
						if (table.contains("Only the first 100 accounts of") && table.contains("matches are shown")) {
							String usefulNote = rows[0].getColumns()[0].getChildrenHTML() + "<br/>";
							header = usefulNote + header;
							table = table.replaceFirst("(?is)<tr[^>]+>\\s*<td>\\s*<div.*</div>\\s*</td>\\s*</tr>", "");
						}
					}
					
					
					header += "</tr>";
					response.getParsedResponse().setHeader(header);
					response.getParsedResponse().setFooter("</table><br><br>");			
					
					outputTable.append(table);
				}
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.TXBexarTR.parseAndFillResultMap(detailsHtml, map, searchId);
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
	
	public static final String IGNORE_ZERO_BALANCE_ACCOUNTS_SELECT = 
		"<select name=\"ignore_zero_balance_accounts\" size=\"2\">" +
		"<option value=\"NO\" selected=\"\">ALL Matching Accounts </option>" +
		"<option value=\"YES\">ONLY Accounts with a Balance Due</option>" +
		"</select>";
	
	public static final String EXTRA_CRITERIA_SELECT = 
		"<select name=\"subsearchby\" size=\"6\">" +
		"<option value=\"3\">Owner Name (Containing these words)</option>" +
		"<option value=\"2\">Owner Name (Exact Phrase)</option>" +
		"<option value=\"4\">Account No.</option>" +
		"<option value=\"8\">Property Address(Containing these words)</option>" +
		"<option value=\"7\">Property Address(Exact Phrase)</option>" +
		"<option value=\"5\">CAD Reference No. </option>" +
		"</select>";

}