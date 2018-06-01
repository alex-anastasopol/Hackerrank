package ro.cst.tsearch.servers.types;



import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;


/**
* @author mihaib
**/

public class FLLeonAO extends TSServer {

	public static final long serialVersionUID = 10003453000L;
	private boolean downloadingForSave; 
	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
	private static int seq = 0;

	protected synchronized static int getSeq() {
		return seq++;
	}
	
	public FLLeonAO(long searchId) {
		super(searchId);
	}

	public FLLeonAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
			
		if(tsServerInfoModule != null) {
            setupSelectBox(tsServerInfoModule.getFunction(6), DIRECTION_SELECT);
            tsServerInfoModule.getFunction(4).setRequired(true);
            setupSelectBox(tsServerInfoModule.getFunction(8), SUFFIX_SELECT);
            tsServerInfoModule.getFunction(6).setRequired(true);
            setupSelectBox(tsServerInfoModule.getFunction(22), SUBDIV_SELECT);
            tsServerInfoModule.getFunction(22).setRequired(true);
            setupSelectBox(tsServerInfoModule.getFunction(23), PROPERTY_USE_SELECT);
            tsServerInfoModule.getFunction(23).setRequired(true);
            
		}
		
		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		String streetNO = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
	
		GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		FilterResponse defaultNameFilter = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module );

		if (hasPin()) {
			//Search by Parcel/Schedule Number
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.LD_PARCELNO_GENERIC_AO);
			modules.add(module);
		}
		
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(5, streetNO);
			module.forceValue(7, streetName);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			modules.add(module);
		}		
		
		if (hasOwner()) {
			//Search by owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
			module.addIterator(nameIterator);
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;

		if ((rsResponse.indexOf("Previous 100 Records") != -1 || rsResponse.indexOf("Next 100 Records") != -1 || (rsResponse.indexOf("Records") != -1 && rsResponse.indexOf(" thru ") != -1))
			&& rsResponse.indexOf("Parcel Information") == -1) {
				viParseID = ID_GET_LINK;
		} 
		
		if (rsResponse.indexOf("No records were found in your search") != -1){
			Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
			return;
		}
		
		switch (viParseID) {		
			case ID_SEARCH_BY_NAME :
				try {
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
					
					rsResponse = rsResponse.replaceAll("(?is)</span[^>]*>", "");
					String table = "";
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
					NodeList mainList = htmlParser.parse(null);
					NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("width", "95%"))
							.extractAllNodesThatMatch(new HasAttributeFilter("CELLSPACING", "0"));
					for (int k = 0 ; k < tables.size(); k++){
//						if (tables.elementAt(k).toHtml().contains("Owner ")){
//							table += tables.elementAt(k).toHtml() + "\r\n";
//						}
						if (tables.elementAt(k).toHtml().contains(" title=\"Click for Parcel Details\"")){
							table += tables.elementAt(k).toHtml() + "\r\n";
						}
					}
					
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, table, outputTable);
											
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
			case ID_SAVE_TO_TSD:
				String details = "";
				details = getDetails(rsResponse);				
				String pid = "";
				try {
					String detailsForParser = details.replaceAll("(?is)(</?t)h([^>]*>)", "$1d$2");
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsForParser, null);
					NodeList mainList = htmlParser.parse(null);
					pid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Parcel ID"), "", false);
				} catch(Exception e) {
					e.printStackTrace();
				}

				if (viParseID != ID_SAVE_TO_TSD) {
					String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + pid + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","ASSESSOR");
					data.put("dataSource","AO");
					
					if(isInstrumentSaved(pid, null, data)){
		                details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
					
	                Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
	                Response.getParsedResponse().setResponse(details);
	                
				} else {
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = pid + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();   
				}
				
				break;	
			
			case ID_GET_LINK :
				if (sAction.contains("TabContainer$tpResults$Repeater_Prop$")) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				break;
		}
	}
	
	protected String getDetails(String rsResponse){
		if (rsResponse.toLowerCase().indexOf("<html") == -1)
			return rsResponse;
		
		rsResponse = rsResponse.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", "&");
		
		String contents = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList mainList = htmlParser.parse(null);

			contents += HtmlParser3.getNodeByID("parcel_Info", mainList, true).toHtml();
			contents += HtmlParser3.getNodeByID("recentSales", mainList, true).toHtml();
			contents += HtmlParser3.getNodeByID("taxValues", mainList, true).toHtml();
			contents += HtmlParser3.getNodeByID("physical", mainList, true).toHtml();
//			contents = contents.replaceAll("(?is)\\s*</?div[^>]*>\\s*", "");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");
		contents = contents.replaceAll("(?is)<img[^>]+>", "");
		contents = contents.replaceAll("(?is)<input[^>]+>", "");
		contents = contents.replaceAll("(?is)<div\\s+style[^>]+>", "");
		
		return contents;
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			// parse and store params for search
			String serverResult = response.getResult();
			String viewState = StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"", "\"", serverResult);
			String eventValidation = StringUtils.getTextBetweenDelimiters("\"__EVENTVALIDATION\" value=\"", "\"", serverResult);
			Map<String, String> params = new HashMap<String, String>();
			params.put("__VIEWSTATE", viewState);
			params.put("__EVENTVALIDATION", eventValidation);
			
			table = table.replaceAll("(?is)&amp;", "&").replaceAll("(?is)&nbsp;", " ");
					
			Pattern TBODY_PAT = Pattern.compile("(?is)(<table[^>]*>.*?</table>)");
			Matcher mat = TBODY_PAT.matcher(table);
			int seq = getSeq();
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
			
			while (mat.find()){
				String tbody = mat.group(1).replaceAll("(?is)<tbody[^>]*>", "<table>").replaceAll("(?is)</tbody[^>]*>", "</table>");
				org.htmlparser.Parser htmlParserT = org.htmlparser.Parser.createParser(tbody, null);
				NodeList mainTableListT = htmlParserT.parse(null);
				TableTag mainTableT = (TableTag)mainTableListT.elementAt(0);
				
				TableRow[] rows = mainTableT.getRows();

				ResultMap resultMap = new ResultMap();
				
				for (int i = 0; i < rows.length; i++) {
					TableColumn[] cols = rows[i].getColumns();
					
					String parcelNo = cols[0].toHtml().replaceAll("(?is)</?td[^>]*>", "").replaceAll("(?is)</?a[^>]*>", "").trim();
					String ownerName = cols[1].getChildrenHTML().replaceAll("(?is)</?font[^>]*>", "").trim();
					String address = cols[2].getChildrenHTML().replaceAll("(?is)</?font[^>]*>", "").trim();
					resultMap.put("tmpAddress", address);
					resultMap.put("PropertyIdentificationSet.ParcelID", parcelNo);
					resultMap.put("tmpOwner", ownerName);
					
				}
					String rowHtml =  mat.group(1);
					rowHtml = rowHtml.replaceAll("(?is)<td[^>]*>\\s*<input[\\s\\\"=A-Z\\$_\\d]* id=\\\"ShowOnMap\\\"[^>]*>\\s*</td>", "");
					
					String link = rowHtml.replaceAll("(?is).*?href[^\\\"]+\\\"([^\\\"]+).*", "$1");
					String accNoPattern = "(?is)javascript:__doPostBack\\((?:&#39;|&quot;)*([A-Z\\$_\\d]+)(?:&#39;,?|&quot;)";
					String accNoId = RegExUtils.getFirstMatch(accNoPattern, link, 1);

					String urlInfo = response.getLastURI().toString();
					link = CreatePartialLink(TSConnectionURL.idPOST) + urlInfo + "?__EVENTTARGET=" + accNoId + "&__EVENTARGUMENT=" + "&seq=" + seq;
					link = Matcher.quoteReplacement(link);
					rowHtml = rowHtml.replaceFirst("(?is)href=\\\"([^\\\"]+)[^>]+>", " href=\"" + link + "\">" ) + "<hr align=\"center\" width=\"100%\" size=\"1\">";
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
					
					resultMap.put("OtherInformationSet.SrcType", "AO");										
					ro.cst.tsearch.servers.functions.FLLeonAO.partyNamesFLLeonAO(resultMap, getSearch().getID());
					ro.cst.tsearch.servers.functions.FLLeonAO.parseAddressFLLeonAO(resultMap, getSearch().getID());

					resultMap.removeTempDef();
					
					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					DocumentI document = (AssessorDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
			}
			
		String header0 = "<br>";
		String prevNextLinks = getPrevNextLinks(response, params, seq);
		String tableRec = "";
		
		org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response.getResult(), null);
		NodeList mainList = htmlParser.parse(null);
		NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
		for (int k = tables.size() - 1; k > 0; k--){
			if (tables.elementAt(k).toHtml().contains("Records")){
				tableRec = tables.elementAt(k).toHtml() + "\r\n";
				break;
			}
		}
			
		header0 += tableRec;
		header0 = header0.replaceAll("(?is)<input [^>]+>","");
		String header1 = "<table BORDER=\"0\" CELLPADDING=\"0\" CELLSPACING=\"0\" WIDTH=\"95%\">" +
						"<tr>" +
						"<th WIDTH=\"20%\" valign=\"top\" align=\"LEFT\">&nbsp;<span class=\"mediumFont\">Parcel ID </font></span></th>" +
						"<th WIDTH=\"45%\" valign=\"top\" align=\"LEFT\">&nbsp;<span class=\"mediumFont\">Owner </font></span></th>" +
						"<th WIDTH=\"35%\" align=\"LEFT\" valign=\"top\" class=\"mediumFont\">&nbsp;Location </font></th></tr></table>" +
						"<hr align=\"center\" width=\"100%\" size=\"1\">";
	
		header1 = header1.replaceAll("(?is)</?a[^>]*>","");
		response.getParsedResponse().setHeader(header0 + header1);	
		response.getParsedResponse().setFooter(prevNextLinks + "</table>");

		outputTable.append(table);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	private String getPrevNextLinks(ServerResponse response, Map<String, String> params, int seq) {
		String linkN = "";
		String linkP = "";
		String links = "";
		StringBuilder footer = new StringBuilder("");
		String url = response.getLastURI().toString();
		
		try {
			String rsResponse = response.getResult();
			HtmlParser3 parser = new HtmlParser3(rsResponse);
			NodeList inputs = parser.getNodeList();
			
			if (inputs != null) {
				inputs = inputs.extractAllNodesThatMatch(new TagNameFilter("input"), true);
				if (inputs.size() > 0) {
					
					Node inputTag = HtmlParser3.getNodeByAttribute(inputs, "id", "ibPrevious1", true);
					if (inputTag != null) {
						linkP = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + url +
								"?TabContainer$tpResults$ibPrevious.x=23&TabContainer$tpResults$ibPrevious.y=15&__EVENTTARGET=&__EVENTARGUMENT=" + 
								"&seq=" + seq +
								"\"> Prev </a>"; 
 
					} else {
						linkP = "<a style=\"text-decoration: none; color: lightgrey;\"> Prev </a>";
					}
					
					inputTag = HtmlParser3.getNodeByAttribute(inputs, "id", "ibNext1", true);
					if (inputTag != null) {
						linkN = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + url +
								"?TabContainer$tpResults$ibNext.x=17&TabContainer$tpResults$ibNext.y=16&__EVENTTARGET=&__EVENTARGUMENT=" + 
								"&seq=" + seq + 
								"\"> Next </a>"; 
					} else {
						linkN = "<a style=\"text-decoration: none; color: lightgrey;\"> Next </a>";
					}
				}
			}
			
			if (StringUtils.isNotEmpty(linkP) && StringUtils.isNotEmpty(linkN)) {
				links =  linkP + "&nbsp; &nbsp;" + linkN + "&nbsp; &nbsp;";
				response.getParsedResponse().setNextLink(linkN);
			}
			
			if (StringUtils.isNotEmpty(links)) {
				footer.append("<tr><td colspan=\"13\"> </br> &nbsp; &nbsp;");
				footer.append(links);
				footer.append(" </td> </tr>");
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return footer.toString();
	}
		
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
			ro.cst.tsearch.servers.functions.FLLeonAO.parseAndFillResultMap(detailsHtml, map, searchId, miServerID);
		return null;
	}
	
	private static final String DIRECTION_SELECT = 
		"<select name=TabContainer$tpSearch$ddlDirection size = 1>" +
		"<option selected>" +
		"<option>N" +
		"<option>S" +
		"<option>E" +
		"<option>W" +
		"<option>NE" +
		"<option>SE" +
		"<option>NW" +
		"<option>SW" +
		"</select>";
	
	private static final String SUFFIX_SELECT = 
		"<select name=TabContainer$tpSearch$ddlSuffix size = 1>" +
		"<option selected>" +
		"<option>AVE" +
		"<option>BLVD" +
		"<option>CIR" +
		"<option>CT" +
		"<option>DR" +
		"<option>HWY" +
		"<option>LN" +
		"<option>LOOP" +
		"<option>PASS" +
		"<option>PKWY" +
		"<option>PL" +
		"<option>RD" +
		"<option>RDG" +
		"<option>RUN" +
		"<option>ST" +
		"<option>SQ" +
		"<option>TER" +
		"<option>TRCE" +
		"<option>TRL" +
		"<option>WAY" +
		"</select>";
	
	private static String SUBDIV_SELECT = "";
	private static String PROPERTY_USE_SELECT = "";
	static {
		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath	+ "] does not exist. Module Information not loaded!");
		}
		try {
			PROPERTY_USE_SELECT = FileUtils.readFileToString(new File(folderPath	+ File.separator + "FLLeonAOAllPropertyUseTypes.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			SUBDIV_SELECT = FileUtils.readFileToString(new File(folderPath	+ File.separator + "FLLeonAOAllSubdivisionNames.xml"));
		} catch (Exception e) {
			e.printStackTrace();
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