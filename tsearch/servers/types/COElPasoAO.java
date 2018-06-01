package ro.cst.tsearch.servers.types;



import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
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
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;


/**
* @author mihaib
**/

public class COElPasoAO extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 
	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
		
	public COElPasoAO(long searchId) {
		super(searchId);
	}

	public COElPasoAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
			
		if(tsServerInfoModule != null) {
            setupSelectBox(tsServerInfoModule.getFunction(3), STREET_SUFFIX);
            setupSelectBox(tsServerInfoModule.getFunction(4), STREET_DIR);
		}
		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		String pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO_GENERIC_AO);
		String streetNO = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
	
		GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module );

		if (hasPin()) {
			//Search by Parcel/Schedule Number
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setData(0, pid);
			modules.add(module);
		}
		
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(0, streetNO);
			module.forceValue(2, streetName);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			modules.add(module);
		}
		
		if (hasOwner()) {
			//Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
													.getConfigurableNameIterator(module, searchId, new String[] {"L;F S;"});
			module.addIterator(nameIterator);
			modules.add(module);
		}
		
		
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;

		switch (viParseID) {		
		
			case ID_SEARCH_BY_NAME :
				
				if (rsResponse.indexOf("No Information matching the specified criteria was found") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				}
				if (rsResponse.indexOf("alert('Please enter EITHER a schedule number OR a Street Name and/or a Last Name") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">Please enter a Street Name and/or a Last Name.</font>");
					return;
				}
				
				try {
					 
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
					 
					String table = "";
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
					NodeList mainList = htmlParser.parse(null);
					table = HtmlParser3.getNodeByID("ctl00_ContentPlaceHolder1_gvSearchResults", mainList, true).toHtml();
					
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
				
				if (rsResponse.indexOf("Schedule not found or invalid schedule number") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				}
				String details = "";
				details = getDetails(rsResponse);				
				
				if (StringUtils.isEmpty(details)){
					Response.getParsedResponse().setError("<font color=\"red\">No details found! Please try again!</font>");
					return;
				}
				
				String pid = "";
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
					NodeList mainList = htmlParser.parse(null);
					pid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Schedule No:"), "", true).replaceAll("</?strong[^>]*>", "").trim();
					pid = pid.replaceAll("(?is)Inactive\\s+on.*", "").trim();
				} catch(Exception e) {
					e.printStackTrace();
				}

				if ((!downloadingForSave))
				{	
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
					
	                Response.getParsedResponse().setPageLink(
							new LinkInPage(
								sSave2TSDLink,
								originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
					parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
	            } 
				else 
	            {      
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = pid + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();                
				}
				
				break;	
			
			case ID_GET_LINK :
				if (sAction.indexOf("ScheduleDisplay.aspx") != -1){
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
	
	protected String getDetails(String rsResponse){
		
		if (rsResponse.toLowerCase().indexOf("<html") == -1)
			return rsResponse;
		
		rsResponse = rsResponse.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", "&");
		
		String contents = "", salesTable = "";
		List<String> links = new ArrayList<String>();
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList mainList = htmlParser.parse(null);

			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);			
			for (int k = tables.size() - 1; k > 0; k--){
				if (tables.elementAt(k).toHtml().contains("Personal Information")){
					contents= tables.elementAt(k).toHtml();
					break;
				}
			}
			
			try {
				salesTable = HtmlParser3.getNodeByID("ctl00_ContentPlaceHolder1_gvSaleInformation",	mainList, true).toHtml();
				if (StringUtils.isNotEmpty(salesTable)) {
					htmlParser = org.htmlparser.Parser.createParser(salesTable,	null);
					mainList = htmlParser.parse(null);

					TableTag mainTable = (TableTag) mainList.elementAt(0);
					TableRow[] rows = mainTable.getRows();

					for (int i = 1; i < rows.length; i++) {
						if (rows[i].getColumnCount() > 1) {
							TableColumn[] cols = rows[i].getColumns();
							links.add(((LinkTag) cols[0].childAt(0)).extractLink());
						}
					}
				}
			} catch (Exception e) {
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (!links.isEmpty()){ // get sales information from each link from Sale Information table
			salesTable = "<table cellspacing=\"0\" cellpadding=\"4\" rules=\"all\" border=\"1\" " +
							"id=\"ctl00_ContentPlaceHolder1_saleTable\" style=\"border-collapse:collapse;\">" +
							"<tr style=\"background-color:Tan;\">" +
							"<td scope=\"col\">Reception</td><td scope=\"col\">Book</td><td scope=\"col\">Page</td>" +
							"<td scope=\"col\">Sale Amount</td><td scope=\"col\">Date</td><td scope=\"col\">Grantee</td>" +
							"<td scope=\"col\">Grantor</td><td scope=\"col\">Deed Type</td></tr>";
			int lineCounter = 1;
			for(String link : links){
				HTTPRequest reqP = new HTTPRequest("http://land.elpasoco.com/" + link);
			    reqP.setMethod(HTTPRequest.GET);
			    	
			    HTTPResponse resP = null;
		        HttpSite site = HttpManager.getSite("COEl PasoAO", searchId);
				try
				{
					resP = site.process(reqP);
				} finally 
				{
					HttpManager.releaseSite(site);
				}	
				String resp = resP.getResponseAsString();
				resp = resp.replaceAll("&amp;", "&").replaceAll("&nbsp;", " ");
				
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp, null);
					NodeList mainList = htmlParser.parse(null);
					TableColumn tc = (TableColumn) HtmlParser3.findNode(mainList, "Reception:").getParent();
					String receptionNO = HtmlParser3.getValueFromCell(tc, "(?is)<strong>[^<]*</strong>(.*)", true).trim();
					
					tc = (TableColumn) HtmlParser3.findNode(mainList, "Book:").getParent();
					String book = HtmlParser3.getValueFromCell(tc, "(?is)<strong>[^<]*</strong>(.*)", true).trim();
					
					tc = (TableColumn) HtmlParser3.findNode(mainList, "Page:").getParent();
					String page = HtmlParser3.getValueFromCell(tc, "(?is)<strong>[^<]*</strong>(.*)", true).trim();
					
					if ((StringUtils.isNotEmpty(receptionNO) && !"0".equals(receptionNO.trim()) 
								|| (StringUtils.isNotEmpty(book) && !"0".equals(book.trim()) && StringUtils.isNotEmpty(page) && !"0".equals(page.trim())))){
						
						tc = (TableColumn) HtmlParser3.findNode(mainList, "Sale Amount:").getParent();
						String saleAmount = HtmlParser3.getValueFromCell(tc, "(?is)<strong>[^<]*</strong>(.*)", true).trim();
						saleAmount = saleAmount.replaceAll("\\$", "");
						
						tc = (TableColumn) HtmlParser3.findNode(mainList, "Date:").getParent();
						String date = HtmlParser3.getValueFromCell(tc, "(?is)<strong>[^<]*</strong>(.*)", true).trim();
						
						tc = (TableColumn) HtmlParser3.findNode(mainList, "Deed Type:").getParent();
						String deedType = HtmlParser3.getValueFromCell(tc, "(?is)<strong>[^<]*</strong>(.*)", true).trim();
						
						tc = (TableColumn) HtmlParser3.findNode(mainList, "Grantee:").getParent();
						String grantee = HtmlParser3.getValueFromCell(tc, "(?is)<strong>[^<]*</strong>(.*)", true).trim();
						grantee = grantee.replaceAll("(?is)</?select[^>]*>", "").replaceAll("(?is)<option[^>]*>", "").replaceAll("(?is)</option[^>]*>", "/").trim();
						
						tc = (TableColumn) HtmlParser3.findNode(mainList, "Grantor:").getParent();
						String grantor = HtmlParser3.getValueFromCell(tc, "(?is)<strong>[^<]*</strong>(.*)", true).trim();
						grantor = grantor.replaceAll("(?is)</?select[^>]*>", "").replaceAll("(?is)<option[^>]*>", "").replaceAll("(?is)</option[^>]*>", "/").trim();
						
						if (lineCounter%2 == 0){
							salesTable += "<tr style=\"background-color:WhiteSmoke;\">";
						} else {
							salesTable += "<tr>";
						}
						salesTable += "<td>" + receptionNO + "</td><td>"  + book + "</td><td>" + page + "</td><td>" + saleAmount + "</td><td>"
						 					+ date + "</td><td>" + grantee + "</td><td>" + grantor + "</td><td>" + deedType + "</td></tr>";
						lineCounter++;
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			salesTable += "</table>";
		}
		
		contents = contents.replaceAll("(?is)(<select[^>]*)>", "$1 size=\"5\">");
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");
		contents = contents.replaceAll("(?is)<img[^>]*>", "");
		contents = contents.replaceAll("(?is)<form.*?</form>", "");
		contents = contents.replaceAll("(?is)<font[^>]*>\\s*Click on[^<]+</font>", "");
		if (StringUtils.isNotEmpty(salesTable)){
			contents = contents.replaceAll("(?is)(ctl00_ContentPlaceHolder1_gvSaleInformation.*?</table>\\s*</div>\\s*</td>\\s*</tr>)", 
						"$1<tr id=\"ctl00_ContentPlaceHolder1_rowSaleTable\"><td colspan=\"2\">"
						+ "<br/><h3 style=\"font-family:Arial\">Sale Information Details</h3></td></tr>"
						+ "<tr id=\"ctl00_ContentPlaceHolder1_rowSaleTable\"><td colspan=\"2\"><div>" + salesTable + "</div></td></tr>");
		}
		
		return contents;
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			table = table.replaceAll("\\s*</?font[^>]*>\\s*", "").replaceAll("(?is)&amp;", "&").replaceAll("(?is)&nbsp;", " ");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList mainTableList = htmlParser.parse(null);
			TableTag mainTable = (TableTag)mainTableList.elementAt(0);
			
			TableRow[] rows = mainTable.getRows();
						
			for(int i = 1; i < rows.length; i++ ) {
				if(rows[i].getColumnCount() > 1) {
					ResultMap resultMap = new ResultMap();
					
					TableColumn[] cols = rows[i].getColumns();
					String parcelNo = cols[0].toHtml().replaceAll("(?is)</?td[^>]*>", "").replaceAll("(?is)</?a[^>]*>", "").trim();
					String ownerName = cols[1].getChildrenHTML().trim();
					String address = cols[2].getChildrenHTML().trim();
					resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address));
					resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address));
					
					
					String rowHtml =  rows[i].toHtml().replaceFirst("(?is)href=[\\\"'](ScheduleDisplay[^\\\"']+)[^>]+>",
										" href=\"" + CreatePartialLink(TSConnectionURL.idGET) + "/$1\">" ).replaceAll("&amp;", "&");
					rowHtml = rowHtml.replaceAll("(?is)<a[^>]+>\\s*(View[^<]*)</a>", "$1");
					
					String link = rowHtml.replaceAll("(?is).*?href[^\\\"]+\\\"([^\\\"]+).*", "$1");
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rows[i].toHtml());
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
					
					resultMap.put("OtherInformationSet.SrcType", "AO");
					resultMap.put("PropertyIdentificationSet.ParcelID", parcelNo);
					resultMap.put("tmpOwner", ownerName);
										
					ro.cst.tsearch.servers.functions.COElPasoAO.partyNamesCOElPasoAO(resultMap, getSearch().getID());

					resultMap.removeTempDef();
					
					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					
					DocumentI document = (AssessorDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
			}
			
		
		String header1 = "<table cellspacing=\"0\" cellpadding=\"2\" rules=\"all\" border=\"1\" " +
							"id=\"ctl00_ContentPlaceHolder1_gvSearchResults\" style=\"width:98%;border-collapse:collapse;\">" + rows[0].toHtml();
	
		header1 = header1.replaceAll("(?is)</?a[^>]*>","");
		response.getParsedResponse().setHeader(header1);	
		response.getParsedResponse().setFooter("</table>");

		
		outputTable.append(table);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
		
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
			ro.cst.tsearch.servers.functions.COElPasoAO.parseAndFillResultMap(detailsHtml, map, searchId, miServerID);
		return null;
	}
	
	private static final String STREET_SUFFIX = 
		"<select name=\"ctl00$ContentPlaceHolder1$ddlType\" tabindex=\"5\">" +
		"<option selected=\"selected\" value=\"\"></option>" +
		"<option value=\"ALY\">ALY</option>" +
		"<option value=\"AVE\">AVE</option>" +
		"<option value=\"BLVD\">BLVD</option>" +
		"<option value=\"CIR\">CIR</option>" +
		"<option value=\"CT\">CT</option>" +
		"<option value=\"CV\">CV</option>" +
		"<option value=\"DR\">DR</option>" +
		"<option value=\"EXPY\">EXPY</option>" +
		"<option value=\"GRV\">GRV</option>" +
		"<option value=\"HTS\">HTS</option>" +
		"<option value=\"HWY\">HWY</option>" +
		"<option value=\"LN\">LN</option>" +
		"<option value=\"LOOP\">LOOP</option>" +
		"<option value=\"PATH\">PATH</option>" +
		"<option value=\"PKWY\">PKWY</option>" +
		"<option value=\"PL\">PL</option>" +
		"<option value=\"PLZ\">PLZ</option>" +
		"<option value=\"PT\">PT</option>" +
		"<option value=\"RD\">RD</option>" +
		"<option value=\"SQ\">SQ</option>" +
		"<option value=\"ST\">ST</option>" +
		"<option value=\"TER\">TER</option>" +
		"<option value=\"TRL\">TRL</option>" +
		"<option value=\"VW\">VW</option>" +
		"<option value=\"WAY\">WAY</option>" +
		"</select>";
	
	private static final String STREET_DIR = 
		"<select name=\"ctl00$ContentPlaceHolder1$ddlDir\" tabindex=\"6\">" +
		"<option selected=\"selected\" value=\"\"></option>" +
		"<option value=\"E\">E</option>" +
		"<option value=\"N\">N</option>" +
		"<option value=\"NE\">NE</option>" +
		"<option value=\"NW\">NW</option>" +
		"<option value=\"S\">S</option>" +
		"<option value=\"SE\">SE</option>" +
		"<option value=\"SW\">SW</option>" +
		"<option value=\"W\">W</option>" +
		"</select>";
	
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