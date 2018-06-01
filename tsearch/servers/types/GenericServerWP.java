package ro.cst.tsearch.servers.types;


import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.OwnerZipCodeFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.document.WhitePagesDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SavedFromType;

/**
 * @author mihaib
*/

public class GenericServerWP extends TSServerROLike{

	private static final long serialVersionUID = 1L;
	private boolean downloadingForSave;
	
	private static final Pattern NEXT_LINK_PAT = Pattern.compile("(?i)href\\s*=\\s*\\\"([^\\\"]+)[^>]+>\\s*Next");

	public GenericServerWP(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public GenericServerWP(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,	int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		String state = getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV);
		if(tsServerInfoModule != null) {
			//tsServerInfoModule.setData(3, getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
			//tsServerInfoModule.setDefaultValue(3, getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
	        PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if(StringUtils.isNotEmpty(functionName)) {
					if(functionName.toLowerCase().contains("state")) {
						setupSelectBox(tsServerInfoModule.getFunction(3), STATE_SELECT.replaceAll("(?is)(value=\\\"" + state + ")", "selected=\"selected\" $1"));
				        tsServerInfoModule.getFunction(3).setRequired(true);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if(tsServerInfoModule != null) {
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if(StringUtils.isNotEmpty(functionName)) {
					if(functionName.toLowerCase().contains("state")) {
						setupSelectBox(tsServerInfoModule.getFunction(2), STATE_SELECT.replaceAll("(?is)(value=\\\"" + state + ")", "selected=\"selected\" $1"));
				        tsServerInfoModule.getFunction(2).setRequired(true);
					}
				}
			}
		}
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		TSServerInfoModule m = null;
		        
        FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter(
        		SearchAttributes.OWNER_OBJECT , searchId , m);
        nameFilterHybridDoNotSkipUnique.setStrategyType(FilterResponse.STRATEGY_TYPE_BEST_RESULTS);

		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		OwnerZipCodeFilter zipFilter = new OwnerZipCodeFilter(searchId);
		zipFilter.setThreshold(new BigDecimal("1.00"));

		//P1     name modules with names from search page.
		if (hasOwner()){
			{	
				String zip = global.getSa().getAtribute(SearchAttributes.OWNER_ZIP); //getZipFromNBDoc();
				if (StringUtils.isNotEmpty(zip)){
					
					if (zip.length() > 5){
						zip = zip.substring(0, 5);
					}
					m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
					m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, "Name module - searching with Owner(s) Name and Owner Zip Code");
					m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
					m.clearSaKeys();
		
					m.forceValue(2, zip);
					((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setIgnoreMiddleOnEmpty(false);
					((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setUseArrangements(false);
					((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setUseSynonymsForCandidates(false);
					((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setInitAgain(true);
					
					m.addFilter(zipFilter);
					m.addFilter(nameFilterHybridDoNotSkipUnique);
					
					m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
					m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
					ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
									.getConfigurableNameIterator(m, searchId, new String[] {"L;F;"});
					m.addIterator(nameIterator);
					modules.add(m);
				}
	    	}
			
			{
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
		    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
		    			TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
		    	m.addExtraInformation("SEARCH", "SECOND_SEARCH");
				m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				m.clearSaKeys();

				m.forceValue(2, global.getSa().getAtribute(SearchAttributes.P_CITY));
				m.forceValue(3, global.getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
				((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setIgnoreMiddleOnEmpty(false);
				((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setUseArrangements(false);
				((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setUseSynonymsForCandidates(false);
				((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setInitAgain(true);
				
				m.addFilter(zipFilter);
				m.addFilter(addressFilter);
				m.addFilter(nameFilterHybridDoNotSkipUnique);
				
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(m, searchId, new String[] {"L;F;"});
				m.addIterator(nameIterator);
				modules.add(m);	
			}
		}
		
	    serverInfo.setModulesForAutoSearch(modules);
	}
	
	@Override
	protected ServerResponse SearchBy(boolean resetQuery, TSServerInfoModule module, Object sd)throws ServerResponseException {
		
		if (mSearch.getSearchType() == Search.AUTOMATIC_SEARCH){
			if (TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION))) {
				if ("SECOND_SEARCH".equals(module.getExtraInformation("SEARCH"))){
					Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
					DocumentsManagerI m = global.getDocManager();
					if (mSearch.getAdditionalInfo("alreadySearched") == null){
						try{
							m.getAccess();
							List<RegisterDocumentI> listRodocs = m.getRoLikeDocumentList(true);
							List<WhitePagesDocumentI> wpList = new ArrayList<WhitePagesDocumentI>();
							for(DocumentI doc : listRodocs){
								if(doc instanceof WhitePagesDocumentI && doc.isSavedFrom(SavedFromType.AUTOMATIC)){
									wpList.add((WhitePagesDocumentI)doc);
								}
							}
							if (wpList.isEmpty()){
								mSearch.setAdditionalInfo("alreadySearched", true);
								return super.SearchBy(resetQuery, module, sd);
							} else {
								return new ServerResponse();
							}
						}
						finally{
							m.releaseAccess();
						}
					}
				}
			}
		}
		
		return super.SearchBy(resetQuery, module, sd);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		
		String rsResponse = initialResponse;
	
		switch (viParseID) {
		
			case ID_SEARCH_BY_ADDRESS :
			case ID_SEARCH_BY_NAME :
				
				if (rsResponse.indexOf("We did not find any directory results for") != -1){
					Response.getParsedResponse().setError("No results found");
					return;
				}
				
				if (rsResponse.indexOf("White Pages Listing") != -1 || rsResponse.indexOf("More information for") != -1){
					ParseResponse(sAction, Response, ID_DETAILS);
					return;
				}
				try {
					 
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
																		
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
											
					if(smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
		            }
					
				} catch(Exception e) {
					e.printStackTrace();
				}
				break;
				
			case ID_DETAILS :
				
				String details = "";
				details = getDetails(rsResponse, Response);
				
				String docNo = "0000";
				try {
					HtmlParser3 parser = new HtmlParser3(details);
					
					String name = parser.getNodeByAttribute("id", "name", true).toPlainTextString().trim();
					String address = parser.getNodeByAttribute("id", "address", true).toPlainTextString().trim();
					String phone = "";
					try {
						phone = parser.getNodeByAttribute("id", "phone", true).toPlainTextString().trim();
					} catch(Exception e) {
						// No phone number.
					}
					docNo = getInstrNoFromBigIntGhertzo(phone, address, name);
				} catch (Exception e) {
					e.printStackTrace();
				}
				

				if ((!downloadingForSave))
				{	
	                String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + docNo + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					originalLink = originalLink.replaceAll("(?is)&$", "");
					try {
						originalLink = URLDecoder.decode(originalLink, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
    				data.put("type", "WHITEPAGES");
	    				
					if (isInstrumentSaved(docNo, null, data)){
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
	                msSaveToTSDFileName = docNo + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
	               
				}
				break;	
			
			case ID_GET_LINK :
				if ((sAction.indexOf("/people/") != -1 || sAction.indexOf("/addr/") != -1) 
						&& !sAction.matches("(?is).*/p\\d+.*") && !rsResponse.contains("View More")){
						ParseResponse(sAction, Response, ID_DETAILS);
				} else if ((sAction.indexOf("/people/") != -1 || sAction.indexOf("/addr/") != -1)
						&& (sAction.matches("(?is).*/p\\d+.*") || rsResponse.contains("View More"))) {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				
				break;
			case ID_SAVE_TO_TSD :
				downloadingForSave = true;
					ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;

			
		}
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			table = table.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			String navRow = "";
			
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList navList = nodeList
								.extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.extractAllNodesThatMatch(new TagNameFilter("tr"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("class", "wp_listing_navbar"));
			if (navList != null && navList.size() > 0){
				navRow = navList.elementAt(0).toHtml();
				navRow = navRow.replaceAll("(?is)(href\\s*=\\s*\\\"?)", "$1" + CreatePartialLink(TSConnectionURL.idGET));
				navRow = navRow.replaceAll("(?i)<span class=\\\"pagination-misc-directional\\\">\\([^<]+</span>", "");
				navRow = navRow.replaceAll("(?is)>\\s*<", ">&nbsp;<");
			} else {
				htmlParser = org.htmlparser.Parser.createParser(table, null);
				navList = htmlParser.parse(new TagNameFilter("div")).extractAllNodesThatMatch(new HasAttributeFilter("class", "wp_pagination"));
				if (navList != null && navList.size() > 0){
					navList.keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("class", "pagination_elipses")), true);
					navRow = navList.elementAt(0).toHtml();
					navRow = navRow.replaceAll("(?is)&nbsp;", " ");
					navRow = navRow.replaceAll("(?is)(?:</a>)\\s*<div.*?class=\\\"pagination_left_active\\\"[^>]*>\\s*</div>\\s*</a>", "&lt; Prev</a>");
					navRow = navRow.replaceAll("(?is)(?:</a>)\\s*<div.*?class=\\\"pagination_right_active\\\"[^>]*>\\s*</div>\\s*</a>", "Next&gt;</a>");
					navRow = navRow.replaceAll("(?i)<div\\s*class=\\\"pagination_number pagination_current\\\"[^>]*>\\s*([^<]*)</div>", 
													"<span class=\"pagination-misc-number pagination-misc-current\">$1</span>");
					navRow = navRow.replaceAll("(?i)</?div[^>]*>", "");
					navRow = navRow.replaceAll("(?is)(href\\s*=\\s*\\\"?)", "$1" + CreatePartialLink(TSConnectionURL.idGET));
					navRow = "<tr class=\"wp_listing_navbar\"><td colspan=\"2\" align=\"right\">" + navRow + "</td></tr>";
					navRow = navRow.replaceAll("(?is)>\\s*<", ">&nbsp;<");
				}
			}
			if (StringUtils.isNotEmpty(navRow)){
				Matcher nextLinkMat = NEXT_LINK_PAT.matcher(navRow);
				if (nextLinkMat.find()){
					response.getParsedResponse().setNextLink("<a href='" + nextLinkMat.group(1) + "'>Next</a>");
				}
			}
			
			htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList mainTableList = htmlParser.parse(new TagNameFilter("table")).extractAllNodesThatMatch(new HasAttributeFilter("class", "wp_listing_table"));
			if (mainTableList.size() == 0){
				htmlParser = org.htmlparser.Parser.createParser(table, null);
				mainTableList = htmlParser.parse(new TagNameFilter("div")).extractAllNodesThatMatch(new HasAttributeFilter("id", "results_wp_inner"));
			}
						
			int numberOfUncheckedElements = 0;
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>")
					.append("<tr><th width=\"5%\" align=\"justify\">" + SELECT_ALL_CHECKBOXES + "</th>" +
									"<th>View</th><th>Name</th><th>Address</th><th>Phone</th></tr>");
			
			int count = 1;
			boolean siteIsWithDivsInsteadOfTables = false;
			
			if (mainTableList != null && mainTableList.size() > 0){
				if (mainTableList.size() == 1 && mainTableList.elementAt(0).getClass().getName().toLowerCase().endsWith("div")){
					Div mainDiv = (Div) mainTableList.extractAllNodesThatMatch(new TagNameFilter("div")).elementAt(0);
					mainTableList = mainDiv.getChildren();
					siteIsWithDivsInsteadOfTables = true;
				}
				
				String nameFromLink = "", row = "";
				String phone = "", address = "";
				
				for (int i = 0; i < mainTableList.size(); i++) {
					String link = CreatePartialLink(TSConnectionURL.idGET);
					if (siteIsWithDivsInsteadOfTables){
						if (mainTableList.elementAt(i).getClass().getName().toLowerCase().endsWith("div")){
							
							  Div divRow = (Div) mainTableList.elementAt(i);
							  if (divRow != null){ 
								  NodeList aList = divRow.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class", "wp_detail_name"), true)
								  										.extractAllNodesThatMatch(new TagNameFilter("a"), true); 
								  if (aList.size() == 0) { 
								  		continue; 
								  } else {
									  link += ((LinkTag) aList.elementAt(0)).getLink(); 
									  nameFromLink = ((LinkTag) aList.elementAt(0)).getChildrenHTML().trim();
									  NodeList list = divRow.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class","wp_detail_addr"), true)
									  							.extractAllNodesThatMatch(new HasAttributeFilter("class", "listing_header"), true); 

									  if (list != null){
										  phone = list.extractAllNodesThatMatch(new NodeClassFilter (TextNode.class), true).toHtml(); 
										  NodeList nl;
										  if((nl = list.extractAllNodesThatMatch(new NodeClassFilter (TextNode.class), true)).size()>0)
											  address = nl.elementAt(0).toHtml(); 
									  } 
									  if (list.extractAllNodesThatMatch(new NodeClassFilter (TextNode.class), true).size() > 1){ 
										  address += "<br>" + list.extractAllNodesThatMatch(new NodeClassFilter (TextNode.class), true).elementAt(1).toHtml(); 
									  }
									  list = divRow.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("class","wp_detail_addr"), true)
			  									.extractAllNodesThatMatch(new HasAttributeFilter("class", "listing_detail"), true);
									  if (list != null && list.size() > 0){
										  address = list.elementAt(0).getChildren().toHtml();
									  }
									  row = "<tr><td><a href=\"" + link + "\">View</a>" + "</td>" +
									  		"<td>" + nameFromLink + "</td>" +
									  		"<td>" + address + "</td><td>" +
									  		phone + "</td></tr>";
								  }
							  }
						} else {
							continue;
						}
					} else {
						TableTag mainTable = (TableTag) mainTableList.elementAt(i);
						if (mainTable != null) {
							TableRow[] rows = mainTable.getRows();
							
							if (rows.length > 0) {
								TableColumn[] cols = rows[0].getColumns();
								if (cols.length > 0) {
									NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true)
															.extractAllNodesThatMatch(new HasAttributeFilter("class", "wp_listing_name"), true);
									if (aList.size() == 0) {
										continue;
									} else {
										link += ((LinkTag) aList.elementAt(0)).getLink();
										nameFromLink = ((LinkTag) aList.elementAt(0)).getChildrenHTML().trim();
	
										row = "<tr><td><a href=\"" + link + "\">View</a></td>" + "<td>" + nameFromLink + "</td>";
										NodeList tdList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
												.extractAllNodesThatMatch(new TagNameFilter("td"), true)
												.extractAllNodesThatMatch(new HasAttributeFilter("class", "wp_listing_small_text"), true);
										
										if (tdList != null && tdList.size() > 0) {
											NodeList divList = tdList.extractAllNodesThatMatch(new TagNameFilter("div"), true);
											if (divList != null) {
												if (divList.size() > 0) {
													address = divList.elementAt(0).getChildren().toHtml();
													row += "<td>" + address + "</td>";
												} else {
													row += "<td>&nbsp;</td>";
												}
												if (divList.size() > 1) {
													phone = divList.elementAt(1).getChildren().toHtml();
													row += "<td>" + phone + "</td>";
												} else {
													row += "<td>&nbsp;</td>";
												}
											} else {
												row += "<td>&nbsp;</td><td>&nbsp;</td>";
											}
										} else {
											row += "<td>&nbsp;</td><td>&nbsp;</td>";
										}
										row += "</tr>";
									}
								}
							}
						}
					}

					String key = phone.replaceAll("\\p{Punct}", "").replaceAll("\\s", "") + i;
					String documentNumber = key;
					ParsedResponse currentResponse = new ParsedResponse();

					RegisterDocumentI document = (RegisterDocumentI) currentResponse.getDocument();
					
					ResultMap resultMap = new ResultMap();
					
					if(document == null) {
						resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "WP");
						resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "WHITEPAGES");
						resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), nameFromLink);
						parseNames(resultMap, searchId);
						String instrNo = "0000";
						instrNo = getInstrNoFromBigIntGhertzo(phone, address, nameFromLink);
						resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
						resultMap.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), instrNo);

						//address = address.replaceAll("(?is)([^<]*)<.*", "$1");
						address = address.replaceAll("<\\s*/?br\\s*/?\\s*>", ";");
						String strNameAndNo = address.substring(0, address.indexOf(";"));
						resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
						resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(strNameAndNo));
						resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(strNameAndNo));
						
						String ownerZipCode = address.substring(address.indexOf(";") + 1);
						if (StringUtils.isEmpty(ownerZipCode) || ownerZipCode.matches("(?is)[\\w\\s]+,\\s*[A-Z]{2}")) 
							ownerZipCode = "";
						else if (ownerZipCode.matches("(?is).*,\\s*[A-Z]{2}\\s*\\d+"))
							ownerZipCode = ownerZipCode.trim().replaceFirst("(?is)[A-Z,\\s]+(\\d+)", "$1");
						if (StringUtils.isNotEmpty(ownerZipCode)) 
							resultMap.put(PropertyIdentificationSetKey.OWNER_ZIP_CODE.getKeyName(), ownerZipCode);
						
						
						
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<table BORDER='1' CELLPADDING='2'>"
								+ row + "</table>");
						
						Bridge bridge = new Bridge(currentResponse, resultMap, getSearch().getID());
						document = (WhitePagesDocumentI) bridge.importData();
						if (StringUtils.isNotEmpty(phone)){
							((WhitePagesDocumentI)document).setPhone(phone);
						}
						if (StringUtils.isNotEmpty(address)){
							((WhitePagesDocumentI)document).setAddress(address.replaceAll("(?is)([^<]*)<.*", "$1"));
						}
						
						currentResponse.setDocument(document);
						String checkBox = "checked";
						HashMap<String, String> data = new HashMap<String, String>();
						data.put("type", "WHITEPAGES");
						
						if (isInstrumentSaved(documentNumber, null, data)
								&& !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
							checkBox = "saved";
						} else {
							numberOfUncheckedElements++;
							LinkInPage linkInPage = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
							checkBox = "<input type=\"checkbox\" name=\"docLink\" value=\"" + link + "\">";
							currentResponse.setPageLink(linkInPage);
							
						}
						row = row.replaceAll("(?is)(<tr[^>]*>)",
											"$1<td align=\"justify\" width=\"5%\" nowrap><font face=\"Verdana\" size=\"1\" rowspan="
											+ count + ">" + checkBox + "</td>");
						currentResponse.setOnlyResponse(row);
						newTable.append(currentResponse.getResponse());
						
						count++;
						intermediaryResponse.add(currentResponse);
						
					}
				}
			}
			
			String header1 = "<tr><th width=\"5%\" align=\"justify\">" + SELECT_ALL_CHECKBOXES + "</th>" +
									"<th>View</th><th>Name</th><th>Address</th><th>Phone</th></tr>";
				
			response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "GET") 
								+ "<br>" + navRow + "<br>"
								+ "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
				
			response.getParsedResponse().setFooter("</table>" + 
										"<br>" + navRow + "<br>" +  CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101, -1));
		
		newTable.append("</table>");
		outputTable.append(newTable);
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

	private String getInstrNoFromBigIntGhertzo(String phone, String address, String name) {
		try {
			name = name.replaceAll("(?is)([^<]*)<.*", "$1").trim();
			address = address.replaceAll("(?is)([^<]*)<.*", "$1").trim();
			phone = phone.replaceAll("\\p{Punct}", "").replaceAll("\\s", "").trim();

			BigInteger nameBigInt = BigInteger.ONE;
			BigInteger addressBigInt = BigInteger.ONE;
			BigInteger phoneBigInt = BigInteger.ONE;

			if (StringUtils.isNotEmpty(name))
				nameBigInt = new BigInteger(name.getBytes());
			if (StringUtils.isNotEmpty(address))
				addressBigInt = new BigInteger(address.getBytes());
			if (StringUtils.isNotEmpty(phone) && phone.matches("\\d+"))
				phoneBigInt = new BigInteger(phone);
			
			BigInteger res = BigInteger.ZERO;
			res = res.add(nameBigInt).add(addressBigInt).add(phoneBigInt);
			
			return org.apache.commons.lang.StringUtils.right(res.toString(), 10);
		} catch (Exception e) {
			logger.error("biginteger bigoperation failed", e);
			return "0000";
		}

	}

	 public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
			DocumentI document = null;
			StringBuilder justResponse = new StringBuilder(detailsHtml);
			try {
				ResultMap map = new ResultMap();
								
				parseAndFillResultMap(response, detailsHtml, map);
				
				String tmpAddress = (String) map.get("tmpAddress");
				String tmpPhone = (String) map.get("tmpPhone");
				
				map.removeTempDef();
				
				Bridge bridge = new Bridge(response.getParsedResponse(),map,searchId);
				
				document = bridge.importData();
				((WhitePagesDocumentI)document).setAddress(tmpAddress);
				((WhitePagesDocumentI)document).setPhone(tmpPhone);
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			if(fillServerResponse) {
				response.getParsedResponse().setOnlyResponse(justResponse.toString());
				if(document!=null) {
					response.getParsedResponse().setDocument(document);
				}
			}
			
			return document;
		}
	 
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "WP");
			detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", " ");
			SimpleDateFormat formatter = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY);
	        String sDate = formatter.format(Calendar.getInstance().getTime());
			resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), sDate);
			resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), sDate);
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(response.getQuerry(), "dummy"));
			
			HtmlParser3 parser = new HtmlParser3(detailsHtml);
			
			String name = parser.getNodeByAttribute("id", "name", true).toPlainTextString().trim();
			String address = parser.getNodeByAttribute("id", "address", true).toPlainTextString().trim();
			String phone = "";
			try {
				phone = parser.getNodeByAttribute("id", "phone", true).toPlainTextString().trim();
			} catch(Exception e) {
				// No phone number.
			}
			
			resultMap.put("tmpAddress", address);
			resultMap.put("tmpPhone", phone);
				
			if (StringUtils.isNotEmpty(name)){
				name = name.replaceAll("(?is)</?h1[^>]*>", "").trim();
				resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), name);
			}
			
			if (StringUtils.isEmpty(ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(response.getQuerry(), "dummy"))){
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), getInstrNoFromBigIntGhertzo(phone, address, name));
			}
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "WHITEPAGES");
			resultMap.put(SaleDataSetKey.DOC_SUBTYPE.getKeyName(), "WHITEPAGES");
			resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), "White Pages");
			
			String strNameAndNo = "";
			String zipCode = "";
			int idx = address.indexOf(";");
			if (idx != -1) {
				strNameAndNo = address.substring(0, idx).trim();
				if (address.matches("(?is).*;\\s*[\\w\\s]+,\\s*[A-Z]{2}\\s*\\d+")) {
					zipCode = address.substring(idx+1).trim().replaceFirst("(?is)[^\\d]+(\\d+)", "$1");
				} else {
					if (address.matches("(?is)[\\w\\s]+;\\s*[A-Z]{2}")) {
						strNameAndNo = "";
					}
					zipCode = "";
				}
					
			} else
				strNameAndNo = address;
			
			String streetNo = StringFormats.StreetNo(strNameAndNo);
			String streetName = StringFormats.StreetName(strNameAndNo);
			if (StringUtils.isEmpty(streetNo)) {
				// This happens when street no is at the end (eg. Po Box 92459)
				Matcher m = Pattern.compile("\\d+").matcher(address);
				if (m.find()) {
					streetNo = m.group();
					streetName = address.replace(streetNo, "").trim();
				}
			}
			
			resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address.replaceAll("(?is)([^<]*)<.*", "$1"));
			if (StringUtils.isNotEmpty(streetNo))
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
			if (StringUtils.isNotEmpty(streetName))
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
			
			if (StringUtils.isNotEmpty(zipCode))
				resultMap.put(PropertyIdentificationSetKey.OWNER_ZIP_CODE.getKeyName(), zipCode);
			
			parseNames(resultMap, searchId);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected String getDetails(String response, ServerResponse Response){
		
		// if from memory - use it as is
		if(!response.toLowerCase().contains("<html")){
			return response;
		}
		
		String details = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainList = htmlParser.parse(null);
			if (mainList != null && mainList.size() > 0){
				TableTag table = (TableTag) mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
											.extractAllNodesThatMatch(new HasAttributeFilter("class", "listing_details"), true).elementAt(0);
				if (table != null){
					details = table.toHtml();
				}  else {
					htmlParser = org.htmlparser.Parser.createParser(response, null);
					mainList = htmlParser.parse(new TagNameFilter("div")).extractAllNodesThatMatch(new HasAttributeFilter("class", "left person"), true);
					if (mainList != null && mainList.size() > 0){
						Div div = (Div) mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("class", "left person"), true).elementAt(0);
						if (div != null){
							details = "<table class=\"listing_details\"><tr><td>";
							if (div != null){
								NodeList nl = div.getChildren();							

								String name = nl.extractAllNodesThatMatch(new TagNameFilter("h1"), true).toHtml();
//								name = name.replaceAll("(?is)([^-]+)-[^<]*(<[^>]+>)", "$1$2");
//								name = name.replaceAll("(.*)(<span.*)", "$1").replaceAll("<h1.*>(.*)(</h1>)?", "$1");
								name = name.replaceFirst("(?is)<\\s*h1[^>]+>([^<]+)<\\s*/\\s*h1\\s*>", "$1");
								
								String address = ((Div) mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
										.extractAllNodesThatMatch(new HasAttributeFilter("class", "addy"), true).elementAt(0)).toHtml();
								address = address.replaceAll("(?is)</?span[^>]*>", "").replaceAll("(?is)</?h2[^>]*>", "")
												.replaceAll("(?is)</?h2[^>]*>", "").replaceFirst("(?is)(?is)([^,]+),", "$1<br>");
								address = address.replaceAll("<div.*\"\\s*>(.*)</div>", "$1").replaceAll("(.*)<br>(.*)", "$1" + "; " + "$2");
								String phone = "";
								try {
									phone = ((Div) mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
											.extractAllNodesThatMatch(new HasAttributeFilter("class", "phone"), true).elementAt(0)).toHtml();
									phone = phone.replaceAll("(.*)(<span.*)", "$1").replaceAll("<div.*>(.*)(</div>)?", "$1");
								} catch (Exception e) {
									// No phone number.
								}
								
								details += "<div id=\"name\">" + name + "</div>\n"
												+ "<table align=\"left\" cellpadding=\"0\" cellspacing=\"1\">\n"
												+ "<tr><td align=\"left\" class=\"wp_listing_small_text\">\n"
												+ "<div id=\"address\">" + address + "</div>\n"
												+ "<div id=\"phone\">" + phone + "</div>"
												+ "</td></tr></table></td></tr></table>";
							}
						}
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		details = details.replaceAll("(?is)<img[^>]*>", "");
		details = details.replaceAll("(?is)<a[^>]*>[^<]*</a>", "");
		details = details.replaceAll("(?is)\\|", "");
		
		return details;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap m, long searchId) throws Exception{
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		ArrayList<List> grantor = new ArrayList<List>();
		
		String tmpPartyGtor = (String)m.get(SaleDataSetKey.GRANTOR.getKeyName());
		if (StringUtils.isNotEmpty(tmpPartyGtor)){
			tmpPartyGtor = tmpPartyGtor.replaceAll("\\sDBA\\s+", " / ");
			
			names = StringFormats.parseNameDesotoRO(tmpPartyGtor, true);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
				
			GenericFunctions.addOwnerNames(tmpPartyGtor, names, suffixes[0],
						suffixes[1], type, otherType,
						NameUtils.isCompany(names[2]),
						NameUtils.isCompany(names[5]), grantor);
			}
			
			m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
	}

	private static String STATE_SELECT = 
		"<select id=\"qs788232511\" name=\"qs\" >" + 
		"<option value=\"\">All States</option>" + 
		"<option value=\"AL\">Alabama</option>" + 
		"<option value=\"AK\">Alaska</option>" + 
		"<option value=\"AZ\">Arizona</option>" + 
		"<option value=\"AR\">Arkansas</option>" + 
		"<option value=\"CA\">California</option>" + 
		"<option value=\"CO\">Colorado</option>" + 
		"<option value=\"CT\">Connecticut</option>" + 
		"<option value=\"DE\">Delaware</option>" + 
		"<option value=\"FL\">Florida</option>" + 
		"<option value=\"GA\">Georgia</option>" + 
		"<option value=\"HI\">Hawaii</option>" + 
		"<option value=\"ID\">Idaho</option>" + 
		"<option value=\"IL\">Illinois</option>" + 
		"<option value=\"IN\">Indiana</option>" + 
		"<option value=\"IA\">Iowa</option>" + 
		"<option value=\"KS\">Kansas</option>" + 
		"<option value=\"KY\">Kentucky</option>" + 
		"<option value=\"LA\">Louisiana</option>" + 
		"<option value=\"ME\">Maine</option>" + 
		"<option value=\"MD\">Maryland</option>" + 
		"<option value=\"MA\">Massachusetts</option>" + 
		"<option value=\"MI\">Michigan</option>" + 
		"<option value=\"MN\">Minnesota</option>" + 
		"<option value=\"MS\">Mississippi</option>" + 
		"<option value=\"MO\">Missouri</option>" + 
		"<option value=\"MT\">Montana</option>" + 
		"<option value=\"NE\">Nebraska</option>" + 
		"<option value=\"NV\">Nevada</option>" + 
		"<option value=\"NH\">New Hampshire</option>" + 
		"<option value=\"NJ\">New Jersey</option>" + 
		"<option value=\"NM\">New Mexico</option>" + 
		"<option value=\"NY\">New York</option>" + 
		"<option value=\"NC\">North Carolina</option>" + 
		"<option value=\"ND\">North Dakota</option>" + 
		"<option value=\"OH\">Ohio</option>" + 
		"<option value=\"OK\">Oklahoma</option>" + 
		"<option value=\"OR\">Oregon</option>" + 
		"<option value=\"PA\">Pennsylvania</option>" + 
		"<option value=\"PR\">Puerto Rico</option>" + 
		"<option value=\"RI\">Rhode Island</option>" + 
		"<option value=\"SC\">South Carolina</option>" + 
		"<option value=\"SD\">South Dakota</option>" + 
		"<option value=\"TN\">Tennessee</option>" + 
		"<option value=\"TX\">Texas</option>" + 
		"<option value=\"UT\">Utah</option>" + 
		"<option value=\"VT\">Vermont</option>" + 
		"<option value=\"VA\">Virginia</option>" + 
		"<option value=\"WA\">Washington</option>" + 
		"<option value=\"DC\">Washington D.C.</option>" + 
		"<option value=\"WV\">West Virginia</option>" + 
		"<option value=\"WI\">Wisconsin</option>" + 
		"<option value=\"WY\">Wyoming</option>" + 
		"</select>";

	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI document) {
		return null;
	}
}
		
