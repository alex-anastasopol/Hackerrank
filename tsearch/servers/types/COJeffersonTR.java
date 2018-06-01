package ro.cst.tsearch.servers.types;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;


/**
 * @author mihaib
  */

public class COJeffersonTR extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 
	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	private static final Pattern PAYM_HIST_PAT = Pattern.compile("PaymentHistory");
	private static final Pattern TAX_ALLOC_PAT = Pattern.compile("TaxAuthorities");
	private static final Pattern ASSESSOR_LINK_PAT = Pattern.compile("(?is)href\\s*=\\s*\\\"([^\\\"]+)[^>]*>Assessor");
	
	private static final Pattern PREV_PAT = Pattern.compile("(?is)<a\\s*href=\\\"([^\\\"]+)[^>]+>Prev");
	private static final Pattern NEXT_PAT = Pattern.compile("(?is)<a\\s*href=\\\"([^\\\"]+)[^>]+>Next");
	
	private static final Pattern PAGES_PAT = Pattern.compile("(?is)(Page\\s*\\d+\\s*of\\s*\\d+)");
		
	public COJeffersonTR(long searchId) {
		super(searchId);
	}

	public COJeffersonTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
	
		// minimize sch number if necessary
        if(module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX){
        	String schedNumebr = module.getFunction(0).getParamValue();
        	if (schedNumebr.length() > 6){
        		schedNumebr = schedNumebr.substring(0, 6);
			}
           	module.getFunction(0).setParamValue(schedNumebr);
          
        }
        return super.SearchBy(module, sd);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
		String pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
		
		GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module );

		if (hasPin()) {
			//Search by Pin
			if (pid.length() == 6 ){// search with Schedule No
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.clearSaKeys();
				module.setData(0, pid);
				module.addFilter(addressFilter);
				module.addFilter(defaultNameFilter);
				modules.add(module);
			} else {// search with Parcel Id
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(0).forceValue(getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO));
				module.addFilter(addressFilter);
				module.addFilter(defaultNameFilter);
				modules.add(module);
			}
		}
		
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(streetNo);
			module.getFunction(1).forceValue(streetName);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			modules.add(module);
		}
		
		
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;

		switch (viParseID) {		
		
			case ID_SEARCH_BY_ADDRESS :
			case ID_SEARCH_BY_PARCEL :
				
				if (rsResponse.indexOf("there are no data records to display") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				}
				
				try {
					 
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
					 
					String table = "";
					HtmlParser3 parser = new HtmlParser3(rsResponse);
					table = parser.getNodeByAttribute("class", "gridStyle-table", true).toHtml();
													
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
			case ID_SEARCH_BY_INSTRUMENT_NO :
				
				if (rsResponse.matches("(?is).*Schedule\\s*\\d+\\s*not\\s+found.*")){
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				}
				if (rsResponse.matches("(?is).*You\\s*must\\s*enter\\s*all\\s*6\\s*digits\\s*of\\s*the\\s*number\\s*,\\s*even\\s*if\\s*the\\s*first\\s*or\\s*last\\s*digits\\s*are\\s*zeros.*")){
					Response.getParsedResponse().setError("<font color=\"red\">The Schedule Number introduced is different from 6 digits.</font>");
					return;
				}
				
				String details = "";
				details = getDetails(rsResponse);				
				String pid = "";
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
					NodeList mainList = htmlParser.parse(null);
					pid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Schedule Number"),"", true).trim();
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
					data.put("type","CNTYTAX");
					
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
				if (sAction.indexOf("forwardSchedule.do") != -1){
						ParseResponse(sAction, Response, ID_DETAILS);
				} else if (sAction.indexOf("pageAction.do") != -1){
						ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
				} 
				
				break;
			
			case ID_SAVE_TO_TSD :

				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;
		}
	}
	
	private String proccessLinks(ServerResponse response) {
		String nextLink = "", prevLink = "";
		String footer = "";
		
		try {
			//String qry = response.getQuerry();
			String rsResponse = response.getResult();
			Matcher priorMat = PREV_PAT.matcher(rsResponse);
			if (priorMat.find()){
				prevLink = CreatePartialLink(TSConnectionURL.idGET) + priorMat.group(1);
			}
			
			Matcher nextMat = NEXT_PAT.matcher(rsResponse);
			if (nextMat.find()){
				nextLink = CreatePartialLink(TSConnectionURL.idGET) + nextMat.group(1);
			}
			
			if (StringUtils.isNotEmpty(prevLink)){
				footer = "&nbsp;&nbsp;&nbsp;<a href=\"" + prevLink + "\">Prev</a>&nbsp;&nbsp;&nbsp;";
			}
			if (StringUtils.isNotEmpty(nextLink)){
				footer += "&nbsp;&nbsp;&nbsp;<a href=\"" + nextLink + "\">Next</a>";
				
				response.getParsedResponse().setNextLink( "<a href='"+nextLink+"'>Next</a>" );
			}

		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return footer;
	}
	
	protected String getDetails(String rsResponse){
		
		if (rsResponse.indexOf("<html") == -1)
			return rsResponse;
		
		String contents = "";
		try {
			HtmlParser3 parser = new HtmlParser3(rsResponse);
			contents = parser.getNodeContentsById("content");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Matcher mat = PAYM_HIST_PAT.matcher(rsResponse);
		if (mat.find()){
			HTTPRequest reqP = new HTTPRequest("https://www.co.jefferson.co.us/ttpsintWeb/ui/PaymentHistory/PaymentHistoryController.jpf");
	    	reqP.setMethod(HTTPRequest.GET);
	    	
	    	HTTPResponse resP = null;
        	HttpSite site = HttpManager.getSite("COJeffersonTR", searchId);
			try
			{
				resP = site.process(reqP);
			} finally 
			{
				HttpManager.releaseSite(site);
			}	
			String rsp = resP.getResponseAsString();
			try {
				HtmlParser3 parser = new HtmlParser3(rsp);
				String tabTax = parser.getNodeContentsById("content");
				contents += "<br><br><br>" + tabTax + "<br>";
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		mat = TAX_ALLOC_PAT.matcher(rsResponse);
		if (mat.find()){
			HTTPRequest reqP = new HTTPRequest("https://www.co.jefferson.co.us/ttpsintWeb/ui/TaxAuthorities/TaxAuthoritiesController.jpf");
	    	reqP.setMethod(HTTPRequest.GET);
	    	
	    	HTTPResponse resP = null;
        	HttpSite site = HttpManager.getSite("COJeffersonTR", searchId);
			try
			{
				resP = site.process(reqP);
			} finally 
			{
				HttpManager.releaseSite(site);
			}	
			String rsp = resP.getResponseAsString();
			try {
				HtmlParser3 parser = new HtmlParser3(rsp);
				String tabTax = parser.getNodeContentsById("content");
				contents += "<br><br><br>" + tabTax + "<br>";
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		mat = ASSESSOR_LINK_PAT.matcher(rsResponse);
		if (mat.find()){
			HTTPRequest reqP = new HTTPRequest(mat.group(1));
	    	reqP.setMethod(HTTPRequest.GET);
	    	
	    	HTTPResponse resP = null;
        	HttpSite site = HttpManager.getSite("COJeffersonTR", searchId);
			try
			{
				resP = site.process(reqP);
			} finally 
			{
				HttpManager.releaseSite(site);
			}	
			String rsp = resP.getResponseAsString();
			try {
				HtmlParser3 parser = new HtmlParser3(rsp);
				String tabTax = parser.getNodeContentsById("content");
				tabTax = tabTax.replaceAll("(?is)<a\\s+href=\\\"?https://landrecords[^>]*>([^<]*)</a>", "$1");
				tabTax = tabTax.replaceAll("(?is)<a\\s+href=\\\"?displaysubdivisionschedule[^>]*>([^<]*)</a>", "$1");
				contents += "<br><br><h2><b>ASSESSOR INFORMATION</b></h2><br><br>" + tabTax + "<br>";
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		contents = contents.replaceAll("(?is)<img[^>]*>", "");
		contents = contents.replaceAll("(?is)<input[^>]*>", "");
		contents = contents.replaceAll("(?is)<fieldset.*?</fieldset>", "");
		contents = contents.replaceAll("(?is)<form.*?</form>", "");
		contents = contents.replaceAll("(?is)<a[^>]*>([^<]*)</a>", "");
		contents = contents.replaceAll("(?is)<style.*?</style>", "");
		contents = contents.replaceAll("(?is)<script.*?</script>", "");
		contents = contents.replaceAll("(?is)<table[^>]*>\\s*<tr[^>]*>\\s*<td[^>]*>\\s*</td>\\s*</tr>\\s*</table>", "");
		contents = contents.replaceAll("(?is)(\\s*<br>){5,}", "");
		contents = contents.replaceAll("(?is)<SELECT.*?<OPTION\\s*selected[^=]*=\\s*([^>]*)>.*?</OPTION>\\s*</SELECT>", "$1");
			
		return contents;
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
	Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
	try {
		
		org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
		NodeList mainTableList = htmlParser.parse(null);
		TableTag mainTable = (TableTag)mainTableList.elementAt(0);
			
		TableRow[] rows = mainTable.getRows();
		TableColumn[] headerCols = rows[0].getColumns();
		String secondHeader = headerCols[1].getChild(0).getText();
		if (headerCols[1].getChild(0).getFirstChild() != null){
			secondHeader = headerCols[1].getChild(0).getFirstChild().getText();
		}
			
		//group the rows which have the same schedule (parcel number)
		LinkedHashMap<String, List<TableRow>> rowsMap = new  LinkedHashMap<String, List<TableRow>>();
		
		for (int i=1;i<rows.length;i++) {
			TableRow row = rows[i];
			if(row.getColumnCount() > 2) {
				String key = row.getColumns()[0].toHtml();
				if (!"".equals(key)) {
					if (rowsMap.containsKey(key)) {			//row already added
						List<TableRow> value = rowsMap.get(key);
						value.add(row);
					} else {								//add new row
						List<TableRow> value = new ArrayList<TableRow>();
						value.add(row);
						rowsMap.put(key, value);
					}
				}
			}
		}
			
		int i=1;
		Iterator<Entry<String, List<TableRow>>> it = rowsMap.entrySet().iterator();
		while (it.hasNext()) {
		
			Map.Entry<String, List<TableRow>> entry = (Map.Entry<String, List<TableRow>>)it.next();
			List<TableRow> value = entry.getValue();
				
			String parcelNo = value.get(0).getColumns()[0].toPlainTextString().trim();
			String secondColumn = value.get(0).getColumns()[1].toPlainTextString().trim();
			StringBuilder owners = new StringBuilder();
			
			String rowType = "1";
			if (i%2==0) {
				rowType = "2";
			}
				
			StringBuilder sb = new StringBuilder();
			sb.append("<tr class=\"row" + rowType + "\">");
			sb.append(value.get(0).getColumns()[0].toHtml().replaceFirst("(?is)<td[^>]*>", "<td style=\"vertical-align: middle;\">"));	//SCHEDULE
			sb.append("<td style=\"vertical-align: middle;\">").append(secondColumn).append("</td>");									//PROPERTY ADDRESS or PARCEL ID
			sb.append("<td style=\"vertical-align: middle;\"><table>");																	//OWNER
			for (int j=0;j<value.size();j++) {
				String ow = value.get(j).getColumns()[2].toPlainTextString().trim();
				owners.append(ow).append(" @@@ ");
				sb.append("<tr><td>").append(ow).append("</td></tr>");
			}
			sb.append("</table></td></tr>");
				
			ResultMap resultMap = new ResultMap();
				
			if (secondHeader.toLowerCase().contains("parcel id")){
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), secondColumn);
			} else if (secondHeader.toLowerCase().contains("property address")){
				resultMap.put("tmpAddress", secondColumn);
			}
			
			String rowHtml =  sb.toString().replaceFirst("(?is)<a\\s*href=\\\"([^\\\"]+)[^>]+>",
					"<a href=\"" + CreatePartialLink(TSConnectionURL.idGET) + "$1\">" ).replaceAll("&amp;", "&");
			String link = rowHtml.replaceAll("(?is).*?<a[^\\\"]+\\\"([^\\\"]+).*", "$1");
			
			ParsedResponse currentResponse = new ParsedResponse();
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, sb.toString());
			currentResponse.setOnlyResponse(rowHtml);
			currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
			
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelNo);
			resultMap.put("tmpOwner", owners.toString());
			
			ro.cst.tsearch.servers.functions.COJeffersonTR.partyNamesCOJeffersonTR(resultMap, getSearch().getID());
			ro.cst.tsearch.servers.functions.COJeffersonTR.parseAddressCOJeffersonTR(resultMap, getSearch().getID());
			
			resultMap.removeTempDef();
			
			Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
				
			DocumentI document = (TaxDocumentI)bridge.importData();				
			currentResponse.setDocument(document);
			
			intermediaryResponse.add(currentResponse);
			
			i++;
				
		}
		
		String header1 = rows[0].toHtml();
		String header0 = "<table>";
			
		header1 = header1.replaceAll("(?is)</?a[^>]*>","").replaceFirst("(?is)<tr[^>]*>", "<tr bgcolor=\"#7DA7D9\" align=\"center\">");
		Pattern tablePat = Pattern.compile("<table[^>]+>");
		Matcher mat = tablePat.matcher(table);
		if (mat.find()){
			header0 = mat.group(0);
		}
		response.getParsedResponse().setHeader(header0 +  header1);
			
		String footer = proccessLinks(response) + "<br><br><br>";
		Matcher pagesMat = PAGES_PAT.matcher(response.getResult());
		if (pagesMat.find()){
			footer = "<div>" + pagesMat.group(1) + "&nbsp;&nbsp;"+ footer  + "</div>";
		}
		
		response.getParsedResponse().setFooter("</table>" + footer);
		
		outputTable.append(table);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
		
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.COJeffersonTR.parseAndFillResultMap(detailsHtml, map, searchId, miServerID);
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