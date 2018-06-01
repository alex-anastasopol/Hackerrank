package ro.cst.tsearch.servers.types;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
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
import ro.cst.tsearch.parser.HtmlParser3;
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
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author mihaib
  */

public class TXGenericSWData extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 
	
	//private static final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
	
	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
	private static final Pattern LINK_PAT = Pattern.compile("(?is)\\(\\s*[^,]*,\\s*[^,]*,\\s*[^,]*,\\s*[^,]*,\\s*\\\"([^\\\"]+)\\\"");
	
	
	public TXGenericSWData(long searchId) {
		super(searchId);
	}

	public TXGenericSWData(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module );
		
		addPinModule(serverInfo, modules);
		
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			modules.add(module);
		}
		
		if (hasOwner()){
			//Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
										.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
			module.addIterator(nameIterator);
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	protected void addPinModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules) {

		TSServerInfoModule module;
		String pid = "";
		pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO_GENERIC_TR).trim();
		if (hasPin()) {
			if (pid.matches("(?i)[A-Z]\\w{9,13}")) {// if pin matches PROPERTY ID format (e.g. R000096005)
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.clearSaKeys();
				module.setData(0, pid);
				modules.add(module);
			} else if ((pid.replaceAll("[-\\s\\.]", "")).matches("\\d{11,13}")) {// if pin matches Geographic ID format (e.g. 16950.00A.007)
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.setData(0, pid);
				modules.add(module);
			}
		}
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;
		String contents = "";
		ParsedResponse parsedResponse = Response.getParsedResponse();	
		
		if (rsResponse.indexOf("Your search did not match any records") != -1){
			Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
			return;
		}
		
		switch (viParseID) {
		
			case ID_SEARCH_BY_NAME :
			case ID_SEARCH_BY_ADDRESS :
			case ID_SEARCH_BY_PARCEL :
				
				StringBuilder outputTable = new StringBuilder();					
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
					NodeList mainList = htmlParser.parse(null);
					
					Node resultTable = HtmlParser3.getNodeByID("searchResults", mainList, true);
					if (resultTable != null){
						contents = resultTable.toHtml();
					} else {
						NodeList divList = mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
														.extractAllNodesThatMatch(new HasAttributeFilter("id", "dvPrimary"), true);
						if (divList != null && divList.size() > 0){
							contents = divList.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true).toHtml();
						}
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
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
				
				String details = "", pid = "";
				details = getDetails(rsResponse);
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
					NodeList mainList = htmlParser.parse(null);
					pid = getPID(details, mainList, "ucidentification_webprop_geoid");
					
				} catch(Exception e) {
					e.printStackTrace();
				}
							
				//details = details.replaceAll("(?is)<a\\s+href='([^']*)'", "<a href='" + linkStart + "/client/$1' ");
				if (!details.contains("pdf")){
					int iStart = details.indexOf("<a href='");
					if (iStart >= 0){
						iStart += 9;
						String imageLink = details.substring(iStart, details.indexOf("' target"));
						
						String baseLink = getBaseLink();
						baseLink = baseLink.substring(0, baseLink.indexOf("/corp/")) + "/client/" + imageLink.substring(imageLink.indexOf("mapdefault"), imageLink.length());
						HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			    		try{
			    			String resp = site.process(new HTTPRequest(baseLink, HTTPRequest.GET)).getResponseAsString();
			    			String newUrl = StringUtils.extractParameter(resp, "(?is)name=\\\"frMapMain\\\"\\s+src\\s*=\\s*\\\"([^\\\"]*)");
			    			if (StringUtils.isNotEmpty(newUrl)){
			    				imageLink = getBaseLink().substring(0, getBaseLink().indexOf("/corp/")) + "/client/" + newUrl;
			    				details = details.replaceAll("(?is)(<a href=')[^']*", "$1" + imageLink);
			    			}
			    		} catch(RuntimeException e){
			    			e.printStackTrace();
			    		} finally {
			    			HttpManager.releaseSite(site);
			    		}   
			    		
						//Response.getParsedResponse().addImageLink(
							//	new ImageLinkInPage(imageLink, imageLink.substring(imageLink.lastIndexOf("/") + 1, imageLink.length())));
					}
				}
				if ((!downloadingForSave))
				{	
					
	                String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + pid + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

					HashMap<String, String> data = new HashMap<String, String>();
					if (TSServersFactory.isCountyTax(miServerID)){
						data.put("type","CNTYTAX");
						data.put("dataSource","TR");
					} else if (TSServersFactory.isAssesor(miServerID)){
						data.put("type","ASSESSOR");
						data.put("dataSource","AO");
					}
					
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
					Response.setResult(details);
					details = details.replaceAll("<a[^>]*?>CLICK HERE TO PAY PROPERTY TAXES</a>", "");
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = pid + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
	                //setImageLink(Response);
	                //addPlatDocFromTR(Response, pid);
				}
				
				break;	
			
			case ID_GET_LINK :
				if (sAction.indexOf("webProperty.aspx") != -1){
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
	
	protected String getDetails(String response){
		
		// if from memory - use it as is
		if(!response.toLowerCase().contains("<html")){
			return response;
		}
		
		String contents = "";
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainList = htmlParser.parse(null);
			NodeList pidTables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "ucidentification_tblIdentification"));
			if (pidTables.size() > 0){
				contents += pidTables.elementAt(0).toHtml();
			}
			
			NodeList divList = mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "dvPrimary"), true);
			if (divList != null && divList.size() > 0){
				contents += divList.elementAt(0).toHtml();
			}
			
			NodeList aList = mainList.extractAllNodesThatMatch(new TagNameFilter("a"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "ucTabs_lbPropertyTaxes"), true);
			if (aList.size() > 0){
				String lnk = ((LinkTag) aList.elementAt(0)).extractLink();
				lnk = StringUtils.prepareStringForHTML(Pattern.quote(lnk));
				Matcher mat = LINK_PAT.matcher(lnk);
				if (mat.find()){
					lnk = "http://www.isouthwestdata.com/client/" + mat.group(1);
					HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
					try {
						response = ((ro.cst.tsearch.connection.http2.TXGenericSWData) site).getPage("/client/" + mat.group(1));
						htmlParser = org.htmlparser.Parser.createParser(response, null);
						mainList = htmlParser.parse(null);
						divList = mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
												.extractAllNodesThatMatch(new HasAttributeFilter("id", "dvPrimary"), true);
						if (divList != null && divList.size() > 0){
							contents += divList.elementAt(0).toHtml();
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						HttpManager.releaseSite(site);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		contents = contents.replaceAll("(?is)</?body[^>]*>", "");
		contents = contents.replaceAll("(?is)</?form[^>]*>", "");
		contents = contents.replaceAll("(?is)(?is)<script.*?</script>", "");
		contents = contents.replaceAll("(?is)<input[^>]*>", "");
		contents = contents.replaceAll("(?is)<a[^>]*>[^<]*</a>", "");
		contents = contents.replaceAll("(?is)<p[^>]*>.*?</p>", "");
		contents = contents.replaceAll("(?is)<img[^>]*>", "");
		contents = contents.replaceAll("(?is)<div[^>]*>\\s*</div>", "");
				
		return contents.trim();
	}

	public String getPID(String details, NodeList mainList, String id) {
		String pid = "";
		try {
			pid = HtmlParser3.getNodeByID(id, mainList, true).getChildren().toHtml();
			
		} catch(Exception e) {
			pid = HtmlParser3.getNodeByID("ucidentification_webprop_id", mainList, true).getChildren().toHtml();
		}
		return pid.trim();
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			
			if (table.contains("Owner Name")){
				String srcType = getDataSite().getSiteTypeAbrev();
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
				NodeList mainTableList = htmlParser.parse(null);
				NodeList tableList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				if (tableList.size() > 0){
					TableTag mainTable = (TableTag)tableList.elementAt(0);
					
					TableRow[] rows = mainTable.getRows();

					String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName().toLowerCase();
					for(TableRow row : rows ) {
						if(row.getColumnCount() > 1) {
							
							TableColumn[] cols = row.getColumns();
							NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
							if (aList.size() > 0){
								String lnk = ((LinkTag) aList.elementAt(0)).extractLink();
								lnk = StringUtils.prepareStringForHTML(Pattern.quote(lnk));
								Matcher mat = LINK_PAT.matcher(lnk);
								if (mat.find()){
									lnk = mat.group(1);
								}
								lnk = lnk.replaceAll("\\|", "%7C");
								String link = CreatePartialLink(TSConnectionURL.idGET) + "/client/"  + lnk;
								
								String rowHtml =  row.toHtml();
								rowHtml = rowHtml.replaceAll("<a[^>]*>", "<a href=\"" + link + "\">");
								ParsedResponse currentResponse = new ParsedResponse();
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
								currentResponse.setOnlyResponse(rowHtml);
								currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
								
								ResultMap m = parseIntermediaryRow(row, searchId, miServerID);
								m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), srcType);
								m.removeTempDef();
								Bridge bridge = new Bridge(currentResponse, m, searchId);
								
								DocumentI document = null;
								if (crtCounty.toLowerCase().contains("parker") || TSServersFactory.isCountyTax(miServerID)){
									document = (TaxDocumentI)bridge.importData();
								} else if (TSServersFactory.isAssesor(miServerID)){
									document = (AssessorDocumentI)bridge.importData();
								}
								currentResponse.setDocument(document);
								
								intermediaryResponse.add(currentResponse);
							}
						}
					}
					response.getParsedResponse().setHeader(table.substring(table.indexOf("<table"), table.indexOf(">") + 1)
															+ rows[0].toHtml());
					response.getParsedResponse().setFooter("</table><br><br>");			
				
					outputTable.append(table);
				}
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	protected ResultMap parseIntermediaryRow(TableRow row, long searchId, int miServerId) throws Exception {
		return ro.cst.tsearch.servers.functions.TXGenericSWData.parseIntermediaryRowTXGenericSWData(row, searchId, miServerID);
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.TXGenericSWData.parseAndFillResultMap(detailsHtml, map, searchId);
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