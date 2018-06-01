package ro.cst.tsearch.servers.types;

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
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
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
import ro.cst.tsearch.titledocument.abstracts.TaxUtilsNew;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

public class FLCollierTR extends TSServer {
	
	static final long serialVersionUID = 10000000;
	
	private boolean downloadingForSave;
    /* ------------ -------------- ---------- pt restul detaliilor din pagina de detalii pt un anumit rezultat adus ------------------ ------------------ ------------------ */       
    private static final Pattern ParcelInfoPreviousYears = Pattern.compile("(?is)window\\.open\\('(view_prev\\.php\\?PARCEL_ID=[0-9A-Z]+[^=]+=(20[0-9]{2}))");
    private static final Pattern instrNoPattern = Pattern.compile("(?is)PARCELID[^>]+>([^&]+)");
    private static final Pattern taxYearPattern = Pattern.compile("(?is)(\\d+)\\s+Tax\\s+Roll\\s+Inquiry\\s+System");
    private static final Pattern backToListButtonPattern = Pattern.compile("(?is)Back\\s*To\\s*List\\\"\\s*onclick\\s*=\\s*\\\"\\s*(location\\s*=\\s*'\\s*[^']+)");
    private static final int YEAR_COUNT = 5;
    int currentYear =  new TaxUtilsNew(searchId, DType.TAX,"FL", "Collier", null).getCurrentTaxYear();
    public String backToListButton = "";

   /* ------------ -------------- ----------- --------------------------------- ------------------------------------------------- ------------------ ------------------- ----------------- */
	public void setServerID(int ServerID){
		super.setServerID(ServerID);
	}
	
	public FLCollierTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid){
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);		
	}

	public String getYearSelect(String id, String name){
		String select  = "<select id=\"" + id + "\" name=\"" + name + "\" size=\"1\">\n";
		for (int i = currentYear; i > currentYear -YEAR_COUNT; i--){
			select += "\t<option ";
			if (i == currentYear){
				select += " selected ";
			}
			select += "value=\"" + i + "\">" + i + "</option>\n";
		}
		select += "</select>";
		
		return select;
	}
	
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX).getFunction(1).setHtmlformat(getYearSelect("param_0_1", "param_0_1"));
		msiServerInfoDefault.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX).getFunction(1).setHtmlformat(getYearSelect("param_2_1", "param_2_1"));
		msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX).getFunction(1).setHtmlformat(getYearSelect("param_1_1", "param_1_1"));
		return msiServerInfoDefault;

	}

	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException
	{
			String rsResponse = Response.getResult();
			String initialResponse = rsResponse;
			ParsedResponse parsedResponse = Response.getParsedResponse();	

			// daca am un sg rezultat adus de ATS, atunci sunt redirectionat catre pag de detalii a rezultatului respectiv 
			if (!rsResponse.contains("Below are the results") && viParseID != ID_SAVE_TO_TSD) 
				viParseID = ID_DETAILS;
			
			switch (viParseID)
			{
			
				case ID_SEARCH_BY_NAME :
				case ID_SEARCH_BY_PARCEL:
				case ID_SEARCH_BY_ADDRESS:	
					
					StringBuilder outputTable = new StringBuilder();
					
				    if (rsResponse.matches("(?is).*Total\\s*Results\\s*Found:\\s*0.*"))
				    {
						rsResponse ="<table><th><b>No records match your search.</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
		                return;
				    }
				    try
				    {
				    	rsResponse = new String (rsResponse.getBytes(),"ISO-8859-1");
				    	initialResponse = new String (initialResponse.getBytes(),"ISO-8859-1");
				    }
				    catch (Exception e)
				    {
				    	e.printStackTrace();
				    }
				    
				    try {
						Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
						
						if(smartParsedResponses.size() > 0) {
							parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
							parsedResponse.setOnlyResponse(outputTable.toString());
							parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			            }
					} catch(Exception e) {
						e.printStackTrace();
					}
					break;
				case ID_DETAILS:
				   if (rsResponse.matches("(?is).*Contact\\s*Delinquent\\s*Taxes\\s*Immediately.*"))
				  {
						rsResponse ="<table><th><b>Contact Delinquent Taxes Immediately!</b></th></table>";
						Response.getParsedResponse().setOnlyResponse(rsResponse);
					    return;
				  }
				  else if (rsResponse.matches("(?is).*No\\s*records\\s*match\\s*your\\s*search.*"))
			      {
			    	  	rsResponse ="<table><th><b>No records match your search.</b></th></table>";
			    	  	Response.getParsedResponse().setOnlyResponse(rsResponse);
			    	  	return;
			      }
           				   
				  rsResponse = getDetails(rsResponse);
				  
				    //get detailed document addressing code
				  String keyCode = "File"; 
				  //sb = new StringBuffer(rsResponse);
				  Matcher instrNoMat = instrNoPattern.matcher(rsResponse);
				  if (instrNoMat.find()){
				    	keyCode = instrNoMat.group(1);
				  }
				  String taxYear = "";
				  Matcher taxYearMatcher = taxYearPattern.matcher(rsResponse);
				  if (taxYearMatcher.find()){
				    	taxYear = taxYearMatcher.group(1).trim();
				  }
				    
				  if ((!downloadingForSave)) {
						String qry_aux = Response.getRawQuerry();
						qry_aux = "dummy=" + keyCode + "&" + qry_aux;
						String originalLink = sAction + "&" + qry_aux;
						String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
						HashMap<String, String> data = new HashMap<String, String>();
						data.put("type","CNTYTAX");
						if (StringUtils.isNotEmpty(taxYear)){
							data.put("year", taxYear);	
						}

						if(isInstrumentSaved(keyCode, null, data)){
						    rsResponse += CreateFileAlreadyInTSD();
						}
						else 
						{
						    rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);
							mSearch.addInMemoryDoc(sSave2TSDLink, rsResponse);
						}

						Response.getParsedResponse().setPageLink(
							new LinkInPage(
								sSave2TSDLink,
								originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
						parser.Parse(
							Response.getParsedResponse(),
							rsResponse,
							Parser.NO_PARSE);
					} else {
						smartParseDetails(Response, rsResponse);
						
						msSaveToTSDFileName = keyCode + ".html";
						Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
						msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
					}
					break;

				case ID_GET_LINK :
					if (rsResponse.matches("(?is).*Collier\\s*County\\s*Tax\\s*Collector.*"))
						ParseResponse(sAction, Response, ID_DETAILS);
					else
						ParseResponse(sAction, Response, ID_SEARCH_BY_PARCEL);							
				break;
				case ID_SAVE_TO_TSD :
					if (sAction.equals("/PropertySearch.aspx"))
						ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
					else
					{// on save
						downloadingForSave = true;
						ParseResponse(sAction, Response, ID_DETAILS);
						downloadingForSave = false;
					}
					break;
				default:
					break;
			}
		}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String rsResponse, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();;
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList mainTableList = htmlParser.parse(null);
			NodeList tableList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (tableList.size() > 0){
				TableTag mainTable = (TableTag)tableList.elementAt(0);
				String table = mainTable.toHtml();
					
				TableRow[] rows = mainTable.getRows();
				for(TableRow row : rows ) {
					if(row.getColumnCount() > 1) {
							
						TableColumn[] cols = row.getColumns();
						NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						if (aList.size() > 0){
							String link = CreatePartialLink(TSConnectionURL.idGET) + "/search/" + ((LinkTag) aList.elementAt(0)).extractLink();
								
							String rowHtml =  row.toHtml();
							rowHtml = rowHtml.replaceAll("<a[^>]*>", "<a href=\"" + link + "\">");
							ParsedResponse currentResponse = new ParsedResponse();
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
							currentResponse.setOnlyResponse(rowHtml);
							currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
								
							ResultMap m = parseIntermediaryRow(row, searchId);
							m.removeTempDef();
							Bridge bridge = new Bridge(currentResponse, m, searchId);
								
							DocumentI document = (TaxDocumentI)bridge.importData();				
							currentResponse.setDocument(document);
							
							intermediaryResponse.add(currentResponse);
						}
					}
				}
				response.getParsedResponse().setHeader(table.substring(table.indexOf("<table"), table.indexOf(">") + 1) + rows[0].toHtml());
				response.getParsedResponse().setFooter("</table><br><br>");			
				
				outputTable.append(table);
				}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

	protected String getDetails(String rsResponse){
		
		// if from memory - use it as is
		if(!rsResponse.toLowerCase().contains("<html")){
			if (rsResponse.toLowerCase().contains("<form ")) {
				rsResponse = rsResponse.replaceAll("(?is)(?:<hr>\\s*<input[^>]+>\\s*)*</?form[^>]*>(?:\\s*<input type=\\\"hidden\\\"\\s[^>]+>)*", "");
			}
			return rsResponse;
		}
		
		String contents = "";
		StringBuffer sbContents = new StringBuffer();
		
		//B3806
		Matcher backToListButtonMatcher = backToListButtonPattern.matcher(rsResponse);
		if (backToListButtonMatcher.find()) {
			backToListButton = backToListButtonMatcher.group(1);
		}		   
		  
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList mainList = htmlParser.parse(null);
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (tables.size() > 0){
				for (int i = 0; i < tables.size(); i++){
					if (tables.elementAt(i).toHtml().contains("Collier County Tax Collector")){
						contents = "<div align=\"center\">" + tables.elementAt(i).toHtml();
					} else if (tables.elementAt(i).toHtml().contains("OWNER INFORMATION")){
						contents += tables.elementAt(i).toHtml();
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		contents = contents.replaceAll("(?is)</?a[^>]*>","");
		contents = contents.replaceAll("(?is)<img[^>]*>","");
		contents = contents.replaceAll("(?is)<!--\\s+-->","");
		contents += "<br> <!--" +backToListButton + "--> <br>"; //B3806

		sbContents.append(contents);
		sbContents.append("<table id=\"taxHistory\" align=\"center\">");
		   /* -------------------- preluarea restului de detalii --------------------------- */	   
		Matcher parcelInfoPreviousYearsMat = ParcelInfoPreviousYears.matcher(rsResponse);					
		
		while(parcelInfoPreviousYearsMat.find()) {
				
			String prevYearInfo = "";
			String prevYearLink = parcelInfoPreviousYearsMat.group(1);
			String year =  parcelInfoPreviousYearsMat.group(2);
				  
			String url = getBaseLink() + "search/" + prevYearLink;
			HTTPRequest req1 = new HTTPRequest(url);
			req1.setMethod( HTTPRequest.GET );
			req1.setHeader("Referer", url);
					
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {		
				prevYearInfo = site.process(req1).getResponseAsString();
				prevYearInfo = new String (prevYearInfo.getBytes(),"ISO-8859-1");// UTF-8
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				HttpManager.releaseSite(site);
			}
			
			if (!prevYearInfo.contains("OWNER INFORMATION"))  {
				continue;	
			} else  {
					try {
						org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(prevYearInfo, null);
						NodeList mainTableList = htmlParser.parse(null);
						NodeList tableList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
						if (tableList.size() > 0){
							for (int i = 3; i < tableList.size(); i++){
								if (tableList.elementAt(i).toHtml().contains("PAY TERMS")){
									TableTag  ptTable = (TableTag) tableList.elementAt(i);
									ptTable.setAttribute("id", "payTerms");
									ptTable.removeAttribute("width");
									ptTable.setAttribute("width", "25%");
									sbContents.append("<tr><td colspan=2>" + year + " Tax Roll Inquiry System</td><td>").append(ptTable.toHtml()).append("</td>");
								}
								if (tableList.elementAt(i).toHtml().contains("PAYMENT INFO")){
									TableTag  ptTable = (TableTag) tableList.elementAt(i);
									ptTable.setAttribute("id", "paymentInfo");
									ptTable.removeAttribute("width");
									ptTable.setAttribute("width", "25%");
									ptTable.removeAttribute("valign");
									ptTable.setAttribute("align", "center");
									sbContents.append("<td>").append(ptTable.toHtml()).append("</td></tr>");
								}
							}
						}
						
					}catch(Exception e) {
						e.printStackTrace();
					}
				  }	
		     }
		sbContents.append("</table> <br> <br>");
		contents = sbContents.toString();
		contents = contents.replaceAll("<center>\\s*<a[^/]+/a>[^/]+/a>[^/]+/a>[^/]+/a>\\s*</center>","");
		contents = contents.replaceAll("<p\\s+name=[^>]+>(.*)</p>", "$1");
		
//		contents += "</tr></table>";
		contents += "</table> </div>";
				
		return contents.trim();
	}

	protected ResultMap parseIntermediaryRow(TableRow row, long searchId) throws Exception {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");
		
		TableColumn[] cols = row.getColumns();
		int count = 0;
		String contents = "";
		for(TableColumn col : cols) {
			
			if (count < 4){
				switch (count) {
				case 0:
					contents = col.getChildren().elementAt(0).toHtml();
					contents = contents.replaceAll("(?is)\\s*<a[^>]+>([^<]*)</a>.*", "$1");
					resultMap.put("PropertyIdentificationSet.ParcelID", contents.trim());

					break;
				case 1:
					contents = col.getChildren().elementAt(0).toHtml();
					if (StringUtils.isNotEmpty(contents)){
						contents = contents.replaceAll("(?is)\\s*<a[^>]+>([^<]*)</a>.*", "$1");
						//resultMap.put("tmpOwner", contents.trim());
						resultMap.put("tmpOwnerInterm", contents.trim());
					}
					break;
				case 2:
					contents = col.getChildren().elementAt(0).toHtml();
					if (StringUtils.isNotEmpty(contents)){
						contents = contents.replaceAll("(?is)\\s*<a[^>]+>([^<]*)</a>.*", "$1");
						resultMap.put("tmpAddress", contents.trim());
					}
					break;
				default:
					break;
				}
				count++;
					
			}
		}
		ro.cst.tsearch.servers.functions.FLCollierTR.partyNamesFLCollierTR(resultMap, searchId);
		ro.cst.tsearch.servers.functions.FLCollierTR.parseAddress(resultMap, searchId);
		
		return resultMap;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.FLCollierTR.parseAndFillResultMap(detailsHtml, map, searchId);
		return null;
	}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m;
		int currentYear = new TaxUtilsNew(searchId, DType.TAX, "FL", "Collier", null).getCurrentTaxYear();

		for (int year = currentYear; year > currentYear - YEAR_COUNT; year--) {
			if (hasPin()) {// search by Parcel Number
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				m.getFunction(1).setSaKey("");
				//m.getFunction(1).setData(Integer.toString(currentYear));
				m.getFunction(1).setData(Integer.toString(year));
				
				l.add(m);
			}
			
			if (hasStreet()) { // Search by Address
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
				GenericNameFilter nameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
				m.clearSaKeys();
				m.setSaKey(0, SearchAttributes.P_STREET_FULL_NAME_NO_SUFFIX);
				m.getFunction(1).setSaKey("");
				//m.getFunction(1).setData(Integer.toString(currentYear));
				m.getFunction(1).setData(Integer.toString(year));
				m.addFilter(addressFilter);
				m.addFilter(nameFilter);
				
				l.add(m);
			}
			
			if (hasOwner()) { // Search by Owners Name
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
				m.clearSaKeys();
				//m.setData(1, Integer.toString(currentYear));
				m.setData(1, Integer.toString(year));
				m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				GenericNameFilter nameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
				nameFilter.setUseSynonymsForCandidates(false);
				m.addFilter(nameFilter);
				m.addFilter(addressFilter);
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
				ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId,
						new String[] { "L;F;", "L;f;", "L;M;" });
				m.addIterator(nameIterator);

				l.add(m);
			}
		}
		
		serverInfo.setModulesForAutoSearch(l);
	}

	
	protected String getFileNameFromLink(String url) {
		String keyCode = "File";
		if (url.contains("dummy="))
			keyCode = org.apache.commons.lang.StringUtils.substringBetween(
				url, "dummy=", "&");
		
		return keyCode+".html";
	}
}

