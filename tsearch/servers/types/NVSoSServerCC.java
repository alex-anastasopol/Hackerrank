package ro.cst.tsearch.servers.types;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.LienI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * @author mihaib
*/

public class NVSoSServerCC extends TSServer{
	
	private static final long serialVersionUID = 1L;
	private boolean downloadingForSave;
	
	private static final Pattern ACT_AMD_SEARCH_PAT = Pattern.compile("(?is)href\\s*=\\s*\\\"(corpActions\\.aspx[^\\\"]+)\\\"[^>]*>\\s*");
	private static final Pattern PAGES_LINKS_PAT = Pattern.compile("(?is)href\\s*=\\s*\\\"javascript:__doPostBack[^\\(]*\\(\\s*'([^']+)[^\\)]+\\)[^\\\"]*\\\"");
	private static final Pattern NEXT_LINK_PAT = Pattern.compile("(?is)</span>[^<]*(<a[^>]+>)");

	public NVSoSServerCC(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public NVSoSServerCC(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,	int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();

		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
		
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		
		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter( 
				SearchAttributes.OWNER_OBJECT, searchId, module);
		if (hasOwner()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
		
			module.forceValue(2, "on");
			
			((GenericNameFilter) defaultNameFilter).setUseArrangements(false);
			((GenericNameFilter) defaultNameFilter).setInitAgain(true);
			
			module.addFilter(defaultNameFilter);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = new ConfigurableNameIterator(searchId, new String[]{"F M L;;"});
			//nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
			nameIterator.setInitAgain(true);
			module.addIterator(nameIterator);
				
			modules.add(module);
    	}
			
	    serverInfo.setModulesForAutoSearch(modules);
	}
	
	@Override
	protected ServerResponse SearchBy(boolean resetQuery, TSServerInfoModule module, Object sd)throws ServerResponseException {
		
		
		return super.SearchBy(resetQuery, module, sd);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		
		String rsResponse = initialResponse;
	
		if (rsResponse.indexOf("This business search is offline for routine maintenance") != -1){
			Response.getParsedResponse().setError("This business search is offline for routine maintenance");
			return;
		} else if (rsResponse.indexOf("System Error") != -1){
			Response.getParsedResponse().setError("System Error");
			return;
		} else if (rsResponse.indexOf("We did not find any directory results for") != -1){
			Response.getParsedResponse().setError("No results found");
			return;
		} else if (rsResponse.indexOf("No results") != -1){
			Response.getParsedResponse().setError("No results found");
			return;
		}
		
		switch (viParseID) {
				
			case ID_SEARCH_BY_ADDRESS :
			case ID_SEARCH_BY_NAME :
								
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
				StringBuilder docNoB = new StringBuilder();
					
				details = getDetails(rsResponse, Response, docNoB);
				String docNo = "";
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
					NodeList mainList = htmlParser.parse(null);

					if (mainList != null && mainList.size() > 0){
						docNo = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainList, "NV Business ID:"), "", true, false).trim();
						docNo = docNo.replaceAll("(?is)</?span[^>]*>", "").replaceAll("(?is)&nbsp;", " ").trim();
					}
						
				} catch (Exception e) {
					e.printStackTrace();
				}
					
				if ((!downloadingForSave)){	
					String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + docNo + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
						
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type", "UCC");
		    				
					if (isInstrumentSaved(docNo, null, data)){
						details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
					parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
				} else{      
					smartParseDetails(Response, details);
					msSaveToTSDFileName = docNo + ".html";
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				}
				
				break;	
			case ID_DETAILS1:
				
				details = "";
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
					NodeList mainList = htmlParser.parse(null);

					if (mainList != null && mainList.size() > 0){
						NodeList span = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
											.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_lblRAName"), true);
						if (span != null && span.size() > 0){					
							details = "<center><h2>" + span.elementAt(0).toHtml() + "</h2></center>";
						}
						NodeList table = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
										.extractAllNodesThatMatch(new HasAttributeFilter("class", "entrybox"));
						if (table != null && table.size() > 0){					
							details += table.toHtml();
							details = details.replaceAll("(?is)(href=\")([^\"]+)", "$1" + CreatePartialLink(TSConnectionURL.idGET) + "$2");
						}
					}					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
				
				break;
			case ID_GET_LINK :
				if (sAction.indexOf("CorpDetails.aspx") != -1){
						ParseResponse(sAction, Response, ID_DETAILS);
				} if (sAction.indexOf("RADetails.aspx") != -1){
						ParseResponse(sAction, Response, ID_DETAILS1);
				} else if (sAction.indexOf("RACorps.aspx") != -1 || Response.getQuerry().contains("pageParam")) {
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
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			
			NodeList nodeList = htmlParser.parse(null);
			NodeList tableList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_objSearchGrid_dgCorpSearchResults"), true);
			
			
			if (tableList != null && tableList.size() > 0){
				int numberOfUncheckedElements = 0;
				StringBuilder newTable = new StringBuilder();
				newTable.append("<table BORDER='1' CELLPADDING='2'>");
				
				TableRow[] rows = ((TableTag)tableList.elementAt(0)).getRows();
				
				String tableHeader = rows[0].toHtml(); 
				newTable.append(tableHeader);
				
				boolean isOfficerorAgentSearch = false;
				if (tableHeader.contains("Officer Name") || tableHeader.contains("Registered Agent Name")){
					isOfficerorAgentSearch = true;
				}
				String navRow = "";
				for(int i = 1; i < rows.length; i++ ) {
					TableRow row = rows[i];
					if(row.getColumnCount() > 3 && !isOfficerorAgentSearch) {
						
						TableColumn[] cols = row.getColumns();
						String lnk = "";
						NodeList aList = null;
						String documentNumber = "";
						try {
							if (cols.length > 1){
								aList = cols[1].getChildren().extractAllNodesThatMatch(
										new TagNameFilter("a"));
							}
						} catch (Exception e) {
							logger.error("Unhandled exception while getting aList", e);
						}
						if (aList != null && aList.size() > 0) {
							lnk = "/sosentitysearch/" + ((LinkTag) aList.elementAt(0)).getLink().replaceAll("\\s", "").replaceAll("&amp;", "&");
							
							try {
								NodeList chil = aList.elementAt(0).getChildren();
								if (chil != null && chil.size() > 0){
									documentNumber = chil.elementAt(0).toHtml();
								}
							} catch (Exception e) {
								logger.error("Unhandled exception while getting documentNumber", e);
							}
						}
						
						String tmpPartyGtor = "";
						if (lnk.contains("noDetails=true")){
							if (cols[1].getChildCount() > 0){
								tmpPartyGtor = cols[1].getChildren().toHtml();
							}
						} else {
							tmpPartyGtor = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true)
													.elementAt(0).getChildren().toHtml();
						}
							
						tmpPartyGtor = tmpPartyGtor.replaceAll("(?is)&nbsp;", " ");
		
						String key = StringUtils.extractParameterFromUrl(lnk, "lx8nvq");
								
						ParsedResponse currentResponse = responses.get(key);							 
						if(currentResponse == null) {
							currentResponse = new ParsedResponse();
							responses.put(key, currentResponse);
						}
								
						RegisterDocumentI document = (RegisterDocumentI)currentResponse.getDocument();
						
						ResultMap resultMap = new ResultMap();
							
						String link = CreatePartialLink(TSConnectionURL.idGET) + lnk;
						if(document == null) {	//first time we find this document
							int count = 1;
								
							String rowHtml =  row.toHtml();
								
							rowHtml = rowHtml.replaceAll("(?is)(href=\\\")[^\\\"]+", "$1" + link);
								
							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "CC");
							tmpPartyGtor = StringEscapeUtils.unescapeHtml(tmpPartyGtor);
							resultMap.put("tmpPartyGtor", tmpPartyGtor);
	
							resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), tmpPartyGtor);
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNumber);
								
							resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "UCC");
							resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), "Nevada Secretary of State");
	
							try {
								parseNames(resultMap, searchId);
							} catch (Exception e) {
								e.printStackTrace();
							}
							resultMap.removeTempDef();
				    				
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<table BORDER='1' CELLPADDING='2'>" + 
										rowHtml + "</table>");
									
							Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
							document = (RegisterDocumentI)bridge.importData();		
									
							currentResponse.setDocument(document);
								
							HashMap<String, String> data = new HashMap<String, String>();
							data.put("type", "UCC");
								
							String checkBox = "checked";
							if (isInstrumentSaved(documentNumber, null, data) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
								checkBox = "saved";
						    } else {
						    	numberOfUncheckedElements++;
						    	checkBox = "<input type=\"checkbox\" name=\"docLink\" value=\"" + link + "\">";
						    	
						    	LinkInPage linkInPage = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
					    		currentResponse.setPageLink(linkInPage);
						    	
						    }
							rowHtml = rowHtml.replaceAll("(?is)(<tr[^>]*>)", 
									"$1 <td  align=\"justify\" width=\"5%\" nowrap><font face=\"Verdana\" size=\"1\" rowspan=" + count + ">" + checkBox);
							currentResponse.setOnlyResponse(rowHtml);
							newTable.append(currentResponse.getResponse());
									
							count++;
							intermediaryResponse.add(currentResponse);
						}
					} else if((row.getColumnCount() > 1 && row.getColumnCount() <= 3) && isOfficerorAgentSearch) {
						TableColumn[] cols = row.getColumns();
						String lnk = "";
						NodeList aList = null;
						try {
							aList = cols[1].getChildren().extractAllNodesThatMatch(
									new TagNameFilter("a"));
						} catch (Exception e) {
							logger.error("Unhandled exception while getting aList", e);
						}
						if (aList != null && aList.size() > 0) {
							lnk = "/sosentitysearch/" + ((LinkTag) aList.elementAt(0)).getLink().replaceAll("\\s", "").replaceAll("&amp;", "&");
						}
	
						String link = CreatePartialLink(TSConnectionURL.idGET) + lnk;
		
						String rowHtml =  row.toHtml();
								
						rowHtml = rowHtml.replaceAll("(?is)(href=\\\")[^\\\"]+", "$1" + link);
						ParsedResponse currentResponse = new ParsedResponse();
		
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<table BORDER='1' CELLPADDING='2'>" + 
								rowHtml + "</table>");
									
						currentResponse.setOnlyResponse(rowHtml);
						newTable.append(currentResponse.getResponse());
	
						intermediaryResponse.add(currentResponse);
					} else if(row.getColumnCount() == 1){//last row with one td with pages links
						navRow = row.getColumns()[0].toHtml();
						String query = response.getRawQuerry();
						query = query.replaceFirst("(?is)&pageParam=.*", "").replaceAll("(?is)%24", "\\$");
						FormTag form = (FormTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true ).elementAt(0);
						if (form != null){
							Map<String,String> paramsForNav = new HashMap<String, String>();
							NodeList inputs = form.getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true);
							for (int j = 0; j < inputs.size(); j++){
								InputTag input = (InputTag) inputs.elementAt(j);
								if ("hidden".equals(input.getAttribute("type"))){
									if (input.getAttribute("name") != null && input.getAttribute("value") != null){
										if (input.getAttribute("name").startsWith("__")){
											paramsForNav.put(input.getAttribute("name"), input.getAttribute("value"));
										}
									}
								} else if ("text".equals(input.getAttribute("type"))){
									if (input.getAttribute("name") != null && input.getAttribute("value") != null){
										if (input.getAttribute("name").startsWith("ctl00") && !input.getAttribute("name").contains("lbl")){
											paramsForNav.put(input.getAttribute("name"), input.getAttribute("value").replaceAll("&amp;", "&"));
										}
									}
								} else if ("checkbox".equals(input.getAttribute("type"))){
									if (input.getAttribute("checked") != null && "checked".equals(input.getAttribute("checked"))){
										paramsForNav.put(input.getAttribute("name"), "on");
									} else {
										paramsForNav.put(input.getAttribute("name"), "off");
									}
								} else if ("radio".equals(input.getAttribute("type"))){
									if (input.getAttribute("checked") != null && "checked".equals(input.getAttribute("checked"))){
										paramsForNav.put(input.getAttribute("name"),  input.getAttribute("value"));
									}
								}
							}
							NodeList selects = form.getChildren().extractAllNodesThatMatch(new TagNameFilter("select"), true);
							for (int j = 0; j < selects.size(); j++){
								SelectTag select = (SelectTag) selects.elementAt(j);
								if (select.getAttribute("name") != null){
									NodeList options = select.getChildren();
									options.keepAllNodesThatMatch(new TagNameFilter("option"), true);
									for (int k = 0; k < options.size(); k++){
										OptionTag option = (OptionTag) options.elementAt(k);
										if (option.getAttribute("selected") != null && "selected".equals(option.getAttribute("selected"))){
											paramsForNav.put(select.getAttribute("name"), option.getAttribute("value"));
											break;
										}
									}
								}
							}
							if (!paramsForNav.isEmpty()){
								mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsNav:", paramsForNav);
							}
						}

						Matcher mat = PAGES_LINKS_PAT.matcher(Pattern.quote(navRow));
						while (mat.find()){
							String lastUri = response.getLastURI().toString();
							lastUri = lastUri.replace("http://nvsos.gov", "");
							String replacement = Matcher.quoteReplacement("&pageParam=" + mat.group(1));
							navRow = navRow.replaceFirst(PAGES_LINKS_PAT.pattern(), " href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + lastUri + "?" + replacement + "\"");
							navRow = navRow.replaceAll("(?is)<td[^>]*>", "<div>").replaceAll("(?is)</td[^>]*>", "</div>");
						}
					}
				}		
				
				if (mSearch.getSearchType() == Search.AUTOMATIC_SEARCH){
					Matcher mat = NEXT_LINK_PAT.matcher(navRow);
					if (mat.find()){
						response.getParsedResponse().setNextLink(mat.group(1) + "Next</a>");
					}
			    }
				
				if (isOfficerorAgentSearch){
					String header1 = rows[0].toHtml();
					
					response.getParsedResponse().setHeader("<br>" + navRow + "<br><br><table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
						
					response.getParsedResponse().setFooter("</table><br>" + navRow + "<br>");
				} else {
					String header1 = rows[0].toHtml().replaceAll("(?is)(<tr[^>]*>)", "$1<td>" + SELECT_ALL_CHECKBOXES + "</td>");
						
					response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "GET") 
										+ "<br>" + navRow + "<br><table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
						
					response.getParsedResponse().setFooter("</table><br>" + navRow + "<br>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101, -1));
				}
			
			newTable.append("</table>");
			outputTable.append(newTable);
			SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
		}
	} catch(Exception e) {
		e.printStackTrace();
	}
		
		return intermediaryResponse;
	}
	
	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck,
			HashMap<String, String> data) {
		if(super.isInstrumentSaved(instrumentNo, documentToCheck, data)) {
			return true;
		}
		if(instrumentNo.indexOf("-") > -1) { // Bug 6781
			DocumentsManagerI documentManager = getSearch().getDocManager();
			try {
	    		documentManager.getAccess();
	    		
	    		InstrumentI instr = new com.stewart.ats.base.document.Instrument("");
	    		String docCateg = "";
	    		if(!StringUtils.isEmpty(data.get("type"))) {
	        		String serverDocType = data.get("type");
	    	    	docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
	            	instr.setDocType(docCateg);
	            	instr.setDocSubType(DocumentTypes.getDocumentSubcategory(serverDocType, searchId));
	    		}
	    		instr.setDocno(instrumentNo);
	    		try {
	    			instr.setYear(Integer.parseInt(instrumentNo.replaceFirst(".*-", "")));
	    		} catch(Exception e) {};
	    		List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
    			
	    		boolean foundMssServerId = false;
	    		for (DocumentI documentI : almostLike) {
	    			if(miServerID==documentI.getSiteId()){
	    				foundMssServerId  = true;
	    				break;
	    			}
	    		}
	    		if(!foundMssServerId){
	    			return false;
	    		}
    			
	    		for (DocumentI documentI : almostLike) {
	    			if(miServerID==documentI.getSiteId() && documentI.getDocType().equals(docCateg)){
	    				return true;
	    			}
	    		}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				documentManager.releaseAccess();
			}
		}
		
		return false;
	}
	
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
		DocumentI document = null;
		StringBuilder justResponse = new StringBuilder(detailsHtml);
		try {
			ResultMap map = new ResultMap();
							
			parseAndFillResultMap(response, detailsHtml, map);
			
			String status = (String) map.get("tmpStatus");
			map.removeTempDef();
			
			Bridge bridge = new Bridge(response.getParsedResponse(), map, searchId);
			
			document = bridge.importData();
			((LienI)document).setStatus(status);
			
			
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
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "CC");
			detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", " ");
			
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			String instrNo = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainList, "NV Business ID:"), "", true, false).trim();
			instrNo = instrNo.replaceAll("(?is)</?span[^>]*>", "").replaceAll("(?is)&nbsp;", " ").trim();
			
			String docNo = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainList, "Entity Number:"), "", true, false).trim();
			docNo = docNo.replaceAll("(?is)</?span[^>]*>", "").replaceAll("(?is)&nbsp;", " ").trim();
			
			if (StringUtils.isEmpty(instrNo)){
				instrNo = docNo;
				docNo = "";
			}
			
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
			resultMap.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), docNo);
			
			String instrDate = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainList, "File Date:"), "", true, false).trim();
			resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), instrDate);
													
			String name = "";
			
			NodeList span = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_ctl00_lblCompanyName"), true);
			if (span != null && span.size() > 0){
				if (span.elementAt(0).getChildren() != null && span.elementAt(0).getChildren().size() > 0){
					name = span.elementAt(0).getChildren().elementAt(0).toHtml();
				}
			}
			
			if (StringUtils.isNotEmpty(name)){
				name = name.replaceAll("(?is)&nbsp;", " ").trim();
				resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), name);
			}
			
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "UCC");
			resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), "Nevada Secretary of State");
			
			parseNames(resultMap, searchId);
			
			String status = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainList, "Status:"), "", true, false);
			status = status.replaceAll("</?span.*?>", "").trim();
			resultMap.put("tmpStatus", status);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected String getDetails(String response, ServerResponse Response, StringBuilder docNoB){
		
		// if from memory - use it as is
		if(!response.toLowerCase().contains("<html")){
			return response;
		}
		
		String details = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainList = htmlParser.parse(null);

			if (mainList != null && mainList.size() > 0){
				NodeList span = mainList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_MainContent_ctl00_lblCompanyName"), true);
				if (span != null && span.size() > 0){					
					details = "<center><h2>" + span.elementAt(0).toHtml() + "</h2></center>";
				}
				NodeList table = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("class", "entrybox"));
				if (table != null && table.size() > 0){					
					details += table.toHtml();
				}
				String docNo = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(mainList, "NV Business ID:"), "", true, false).trim();
				docNo = docNo.replaceAll("(?is)&nbsp;", " ").trim();
				docNoB.append(docNo);
			}
			Matcher mat = ACT_AMD_SEARCH_PAT.matcher(response);
			if (mat.find()) {
				HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
				try {
					response = ((ro.cst.tsearch.connection.http2.NVSoSConnCC) site).getPage("/sosentitysearch/" + mat.group(1));
					htmlParser = org.htmlparser.Parser.createParser(response, null);
					mainList = htmlParser.parse(null);
					NodeList table = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("class", "entrybox"));
					if (table != null && table.size() > 0){					
						details += table.toHtml();
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					HttpManager.releaseSite(site);
				}
			}

			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//details = details.replaceAll("(?is)<img[^>]*>", "");
		details = details.replaceAll("(?is)</?a[^>]*>", "").replaceAll("(?is)<input[^>]*>", "");
		
		return details;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap m, long searchId) throws Exception{
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""}, type= {"", ""}, otherType= {"", ""};
		
		ArrayList<List> grantor = new ArrayList<List>();
		
		String tmpPartyGtor = (String)m.get(SaleDataSetKey.GRANTOR.getKeyName());
		if (StringUtils.isNotEmpty(tmpPartyGtor)){
			
			String[] grantors = tmpPartyGtor.split("\\s*/\\s*");
			for (String gtor : grantors) {
				if (NameUtils.isCompany(gtor)){
					names[2] = gtor;
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
				} else {
					names = StringFormats.parseNameDesotoRO(gtor, true);
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
				}
					
				GenericFunctions.addOwnerNames(gtor, names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), grantor);
			}
		}
			
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
	}
	
	public static void parseAddresses(ResultMap m, long searchId) throws Exception{
		
		String tmpAddresses = (String) m.get("tmpAddresses");
		
		Vector<PropertyIdentificationSet> pisVector = new Vector<PropertyIdentificationSet>();
		if (StringUtils.isNotEmpty(tmpAddresses)){
			String[] addresses = tmpAddresses.split("@@#@@");
			for (String address : addresses) {
				String[] items = address.split("(?is)<br/?>");
				PropertyIdentificationSet tmpPis = new PropertyIdentificationSet();
				for (int i = 0; i < items.length; i++) {
					if (i == 0){
						tmpPis.setAtribute("StreetName", StringFormats.StreetName(items[i]));
						tmpPis.setAtribute("StreetNo", StringFormats.StreetNo(items[i]));
					} else if (i == 1){
						tmpPis.setAtribute("City", items[i].replaceAll("(?is)([^,]+),.*", "$1"));
					} else if (i == 2){
						tmpPis.setAtribute("County", items[i].replaceAll("(?is)([^$]+)\\s+COUNTY", "$1"));
					}
				}
				pisVector.add(tmpPis);
			}
			m.put("PropertyIdentificationSet", pisVector);
		}
		
	}

}
		
