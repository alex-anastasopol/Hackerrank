package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FrameTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadyPresentFilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.CourtDocumentIdentificationSet.CourtDocumentIdentificationSetKey;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.MOGenericCaseNetCO.NameFilter;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.PDFUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.name.NameI;


public class ARGenericCourtConnectCO extends TSServer{

	/**
	 * @author mihaib
	 */
	private static final long serialVersionUID = -6550207391242759488L;
	private static final Pattern NEXT_PAT = Pattern.compile("(?is)href=\\\"([^\\\"]+)\\\"[^>]*>\\s*Next");
	private static final Pattern PRIOR_PAT = Pattern.compile("(?is)href=\\\"([^\\\"]+)\\\"[^>]*>\\s*<\\s*-\\s*Previous");
	private static final Pattern DETAILS_BODY_PAT = Pattern.compile("(?is)<BODY[^>]*>(.*?)</body>");
	private static final HashMap<String,String> counties;
	
	static {
		//the key is miServerID
		counties = new HashMap<String,String>();
		counties.put("FAULKNER", "23 - FAULKNER");
		counties.put("GARLAND", "26 - GARLAND");
		counties.put("HOT SPRING", "30 - HOT SPRING");
		counties.put("339108", "60 - PULASKI");
		counties.put("SEARCY", "65 - SEARCY");
		counties.put("VAN BUREN", "71 - VAN BUREN");
	}
	public ARGenericCourtConnectCO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {

		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	
	@Override
	protected void ParseResponse(String sAction, ServerResponse response, int viParseID) throws ServerResponseException {
		
		String rsResponse = response.getResult();
		ParsedResponse parsedResponse = response.getParsedResponse();
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_NAME:
				
				// no result
				if (rsResponse.indexOf("No records found.") > -1) {
					response.getParsedResponse().setError("No results found");
					return;
				}else if (rsResponse.indexOf("Please check either Phonetic or Partial Last Name Search") > -1) {//don't be checked both
					response.getParsedResponse().setError("<H1>Search Error</H1>Please check either Phonetic or Partial Last Name Search.");
					return;
				}
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() == 0) {
					return;
				}
				
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
	            }
				
				break;
			case ID_GET_LINK:
				if (sAction.indexOf("ck_public_qry_doct.cp_dktrpt_frames") > -1) {
					ParseResponse(sAction, response, ID_DETAILS);
				} else {
					ParseResponse(sAction, response, ID_SEARCH_BY_NAME);
				}
				break;
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				
				StringBuilder instrumentNumber = new StringBuilder();
				String details = getDetails(rsResponse, instrumentNumber);
				
				String type = "", lnk = "";
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
					NodeList nodeList = htmlParser.parse(null);
					type = HtmlParser3.getValueFromNextCell(nodeList,"Type:", "", false).replaceAll("(?is)&nbsp;", " ").trim();
					if (instrumentNumber.length() == 0){
						instrumentNumber.append(HtmlParser3.getValueFromNextCell(nodeList, "Case ID", "", false).replaceAll("(?is)&nbsp;", " ").trim());
					}
					int i = 1;
					NodeList aList = null;

					do {//for Images: links
						aList = new NodeList();
						if (HtmlParser3.getAbsoluteCell(i, 1, HtmlParser3.findNode(nodeList, "Status:")) != null){
							TableColumn links = HtmlParser3.getAbsoluteCell(i, 1, HtmlParser3.findNode(nodeList, "Status:"));
							aList = links.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"));
							
							lnk += ((LinkTag) aList.elementAt(0)).getLink() + "###";
							i++;
							if (viParseID == ID_SAVE_TO_TSD){
								details = details.replace(links.getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).toHtml(), 
										links.getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0).getChildren().toHtml().trim());
							}
						}
					} while (aList.size() > 0);
					
				}catch(Exception e) {
					e.printStackTrace();
				}
				
				String instNo = instrumentNumber.toString();
				if (StringUtils.isNotEmpty(lnk.trim())){
					response.getParsedResponse().addImageLink(new ImageLinkInPage(lnk, instNo + ".pdf"));
				}
				
				if(viParseID == ID_DETAILS) {
					String originalLink = sAction.replace("?", "&") + "&" + response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type", type);
					data.put("dataSource", "CO");
					
					if(isInstrumentSaved(instNo, null, data)){
						details += CreateFileAlreadyInTSD();
					}
					else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
	
					parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					parsedResponse.setResponse(details);
				} else {
					details = details.replaceAll("(?i)<a.*?cp_personcase_details_idx[^>]*>([^<]+)(?:</a>)?", "$1");
					String filename = instrumentNumber.toString().replaceAll("/", "") + ".html";
					smartParseDetails(response, details);
					
					msSaveToTSDFileName = filename;
					parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
					parsedResponse.setResponse(details);
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				}
		}
		
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nl = htmlParser.parse(new TagNameFilter("frameset"));
			if (nl != null){
				if (nl.extractAllNodesThatMatch(new HasAttributeFilter("name", "Big"), true).elementAt(0) != null){
					FrameTag frameSRC = (FrameTag) nl.extractAllNodesThatMatch(new HasAttributeFilter("name", "Big"), true).elementAt(0);
					String nextLinkToFollow = frameSRC.getAttribute("src").toString();
					nextLinkToFollow = nextLinkToFollow.replaceAll("(?is)\n", "");
					String newLink = getBaseLink().substring(0, getBaseLink().indexOf("ck_public"));
					HTTPRequest req = new HTTPRequest(newLink + nextLinkToFollow);
					HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
					try {
						HTTPResponse resp = ((ro.cst.tsearch.connection.http3.ARGenericCourtConnectCO)site).process(req); 
						table = resp.getResponseAsString();
					}catch(Exception e) {
						e.printStackTrace();
					}finally {
						HttpManager3.releaseSite(site);
					}
				}
			}
	
			htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList mainTableList = htmlParser.parse(new TagNameFilter("table"));
			String pageheader = "", pageRecords = "";
			TableTag mainTable = null;
			for(int i = 0; i < mainTableList.size(); i++){
				if (mainTableList.elementAt(i).toHtml().contains("county")){
					pageheader = mainTableList.elementAt(i).toHtml();
				} else if (mainTableList.elementAt(i).toHtml().contains("Party Type")){
					mainTable = (TableTag)mainTableList.elementAt(i);
				} else if (mainTableList.elementAt(i).toHtml().contains("Records")){
					pageRecords = mainTableList.elementAt(i).toHtml();
				}
			}
			 
			int numberOfUncheckedElements = 0;
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>");
			
			TableRow[] rows = mainTable.getRows();
			newTable.append(rows[0].toHtml());

			for(int i = 1; i < rows.length; i++ ) {
				TableRow row = rows[i];
				if(row.getColumnCount() > 0) {
					
					TableColumn[] cols = row.getColumns();
					
					NodeList aList = cols[2].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"));
					if (aList.size() == 0) {
						continue;
					} else {
						String lnk = ((LinkTag) aList.elementAt(0)).getLink().replaceAll("\\s", "");
						String documentNumber = StringUtils.extractParameterFromUrl(lnk, "case_id");
						int index = lnk.indexOf("/cconnect");
						if (index > -1 ){
							lnk = lnk.substring(index, lnk.length());
						} else{
							lnk = "/cconnect/PROD/public/" + lnk;
						}
						String partyName = cols[1].toPlainTextString().replaceAll("&nbsp;", " ").trim();
						String partyType = cols[3].toPlainTextString().replaceAll("&nbsp;", " ").trim();
						String fillingDate = cols[4].toPlainTextString().replaceAll("&nbsp;", " ").trim();
						String circuitName = cols[5].toPlainTextString().replaceAll("&nbsp;", " ").replaceAll("(?is)[^-]+-([^$]*)", "$1").trim();
						String key = cols[0].toPlainTextString().replaceAll("&nbsp;", " ").trim();
						key += "_" + documentNumber;
							
						ParsedResponse currentResponse = responses.get(key);							 
						if(currentResponse == null) {
							currentResponse = new ParsedResponse();
							responses.put(key, currentResponse);
						}
							
						RegisterDocumentI document = (RegisterDocumentI)currentResponse.getDocument();
						String tmpPartyGtor = "", tmpPartyGtee = "";
						String parties = cols[2].toPlainTextString().replaceAll("&nbsp;", " ").trim();
						parties = parties.replaceAll("(?is)\\ACase\\s*:\\s{2,}[A-Z\\d-]+\\s{2,}([^$]+)", "$1");
						String[] partiesV = parties.split("\\s+VS?\\s+");
						if (partiesV.length > 0){
							tmpPartyGtor = partiesV[0].trim();
							if (partiesV.length > 1){
								tmpPartyGtee = partiesV[1].trim();
							}
						}
						ResultMap resultMap = new ResultMap();
							
						String link = CreatePartialLink(TSConnectionURL.idGET) + lnk;
						if(document == null) {	//first time we find this document
							int count = 1;
							
							String rowHtml =  row.toHtml().replaceFirst("(?is)(href=\\\")[^\\\"]+","$1" + link);

							tmpPartyGtor = StringEscapeUtils.unescapeHtml(tmpPartyGtor);
							tmpPartyGtee = StringEscapeUtils.unescapeHtml(tmpPartyGtee);
							resultMap.put("tmpPartyGtor", tmpPartyGtor);
							resultMap.put("tmpPartyGtee", tmpPartyGtee);
							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "CO");
							resultMap.put(CourtDocumentIdentificationSetKey.PARTY_NAME.getKeyName(), partyName);
							resultMap.put(CourtDocumentIdentificationSetKey.PARTY_TYPE.getKeyName(), partyType);
							resultMap.put(CourtDocumentIdentificationSetKey.CASE_NUMBER.getKeyName(), documentNumber);
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNumber);
							
							Date date = FormatDate.getDateFromFormatedString(fillingDate, "dd-MMM-yy");
							SimpleDateFormat formatter = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY);
					        String sDate = formatter.format(date);
							resultMap.put(CourtDocumentIdentificationSetKey.FILLING_DATE.getKeyName(), sDate);
							resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), sDate);
							
							resultMap.put(CourtDocumentIdentificationSetKey.CIRCUIT.getKeyName(), circuitName);
							try {
								partyNames(resultMap);
							} catch (Exception e) {
								e.printStackTrace();
							}
							resultMap.removeTempDef();
			    				
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<table BORDER='1' CELLPADDING='2'>" + 
									row.toHtml() + "</table>");
								
							resultMap = parseIntermediaryRow(resultMap, row, searchId);
								
							Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
							document = (RegisterDocumentI)bridge.importData();		
								
							currentResponse.setDocument(document);
							//
							RegisterDocumentI docMisc = document.clone();
							docMisc.getInstrument().setDocType("MISCELLANEOUS");
							docMisc.getInstrument().setDocSubType("Miscellaneous");
							RegisterDocumentI docCourt = document.clone();
							docCourt.getInstrument().setDocType("COURT");
							docCourt.getInstrument().setDocSubType("Court");
							//
							String checkBox = "checked";
							if (isInstrumentSaved(documentNumber, docMisc, null) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
								checkBox = "saved";
					    	} else if (isInstrumentSaved(documentNumber, docCourt, null) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
								checkBox = "saved";
					    	} else {
					    		numberOfUncheckedElements++;
					    		LinkInPage linkInPage = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
					    		checkBox = "<input type=\"checkbox\" name=\"docLink\" value=\"" + link + "\">";
					    		currentResponse.setPageLink(linkInPage);
					    	}
							rowHtml = rowHtml.replaceAll("(?is)<tr[^>]*>", 
									"<tr><td  align=\"justify\" width=\"5%\" nowrap><font face=\"Verdana\" size=\"1\" rowspan=" + count + ">" + checkBox + "</td>");
							currentResponse.setOnlyResponse(rowHtml);
							newTable.append(currentResponse.getResponse());
								
							count++;
							intermediaryResponse.add(currentResponse);
							
						} else {
							tmpPartyGtor = StringEscapeUtils.unescapeHtml(tmpPartyGtor);
							tmpPartyGtee = StringEscapeUtils.unescapeHtml(tmpPartyGtee);
							resultMap.put("tmpPartyGtor", tmpPartyGtor);
							resultMap.put("tmpPartyGtee", tmpPartyGtee);
							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "CO");
							resultMap.put(CourtDocumentIdentificationSetKey.PARTY_NAME.getKeyName(), partyName);
							resultMap.put(CourtDocumentIdentificationSetKey.PARTY_TYPE.getKeyName(), partyType);
							resultMap.put(CourtDocumentIdentificationSetKey.CASE_NUMBER.getKeyName(), documentNumber);
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNumber);
							resultMap.put(CourtDocumentIdentificationSetKey.FILLING_DATE.getKeyName(), fillingDate);
							resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), fillingDate);
							resultMap.put(CourtDocumentIdentificationSetKey.CIRCUIT.getKeyName(), circuitName);
							try {
								partyNames(resultMap);
							} catch (Exception e) {
								e.printStackTrace();
							}
							resultMap.removeTempDef();
							Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
							RegisterDocumentI documentTemp = (RegisterDocumentI)bridge.importData();
			    			
							for(NameI nameI : documentTemp.getGrantor().getNames()) {
								if(!document.getGrantor().contains(nameI)) {
									document.getGrantor().add(nameI);
			    				}
			    			}
							for(NameI nameI : documentTemp.getGrantor().getNames()) {
								if(!document.getGrantor().contains(nameI)) {
									document.getGrantor().add(nameI);
			    				}
			    			}
							String rawServerResponse = (String)currentResponse.getAttribute(ParsedResponse.SERVER_ROW_RESPONSE);
			    				
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rawServerResponse);
			    				
							String responseHtml = currentResponse.getResponse();
							String countString = StringUtils.extractParameter(responseHtml, "rowspan=(\\d)+");
							try {
								int count = Integer.parseInt(countString);
								responseHtml = responseHtml.replaceAll("rowspan=(\\d)+", "rowspan=" + (count + 1));
			    					
								currentResponse.setOnlyResponse(responseHtml);
			    			} catch (Exception e) {
			    				e.printStackTrace();
							}	
						}
					}

				}
			}
			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
				
				String navLinks = proccessLinks(response) + "<br><br><br>";
				String header0 = "<div>" + pageheader + pageRecords;
				
				header0 += "&nbsp;&nbsp;"+ navLinks  + "</div>";
				String header1 = rows[0].toHtml();
				header1 = header1.replaceAll("(?is)<tr[^>]*>", 
									"<TR><TH  align=\"justify\" width=\"5%\" nowrap><font face=\"Verdana\" size=\"1\" >" + SELECT_ALL_CHECKBOXES +"</TH>");

				response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "GET") + header0
											+ "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
				
				response.getParsedResponse().setFooter("</table>" + header0 + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101, -1));
			}	
		
		outputTable.append(newTable);
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

	private String getDetails(String page, StringBuilder instrumentNumber) {
		
		if(!page.toLowerCase().contains("<html")){
			return page;
		}
		
		StringBuilder details = new StringBuilder();
		String baseLink = getBaseLink();
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nl = htmlParser.parse(new TagNameFilter("frameset"));
			nl = nl.extractAllNodesThatMatch(new TagNameFilter("frame"), true);
			if (nl != null){
				if (nl.extractAllNodesThatMatch(new HasAttributeFilter("name", "main"), true).elementAt(0) != null){
					FrameTag frameSRC = (FrameTag) nl.extractAllNodesThatMatch(new HasAttributeFilter("name", "main"), true).elementAt(0);
					nl.keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("name")), true);
					nl.keepAllNodesThatMatch(new HasAttributeFilter("src"), true);
					FrameTag frameAnchorLink = null;
					String anchorLink = "";
					try {
						frameAnchorLink = (FrameTag) nl.extractAllNodesThatMatch(new TagNameFilter("frame"), true).elementAt(0);
					} catch (Exception e) {
						logger.error("Nu s-a gasit linkul pt ancore" + searchId);
					}
					if (frameAnchorLink != null){
						anchorLink = frameAnchorLink.getAttribute("src").toString();
					}
					String nextLinkToFollow = frameSRC.getAttribute("src").toString();
					nextLinkToFollow = nextLinkToFollow.replaceAll("(?is)\n", "");
					String newLink = baseLink.substring(0, baseLink.indexOf("ck_public"));
					HTTPRequest req = new HTTPRequest(newLink + nextLinkToFollow);
					HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
					try {
						HTTPResponse response = ((ro.cst.tsearch.connection.http3.ARGenericCourtConnectCO)site).process(req); 
						page = response.getResponseAsString();
					}catch(Exception e) {
						e.printStackTrace();
					}finally {
						HttpManager3.releaseSite(site);
					}
					Matcher BODY_MAT = DETAILS_BODY_PAT.matcher(page);
					if (BODY_MAT.find()){
						page = BODY_MAT.group(1);
						page = page.replaceAll("(?is)</?base[^>]*>", "").replaceAll("(?is)</a[^>]*>", "");
						page = page.replaceAll("(?is)(<td[^>]*>)", "</td>$1");
						page = page.replaceAll("(?is)(</td[^>]*>)(<tr[^>]*>)", "$1</tr>$2");
						page = page.replaceAll("(?is)(<tr[^>]*>)\\s*(</td[^>]*>)", "$1");
						page = page.replaceAll("(?is)(<tr[^>]*>)", "</tr>$1");
						page = page.replaceAll("(?is)(</table[^>]*>)", "</tr>$1");
						page = page.replaceAll("(?is)</td[^>]*>\\s*(</td[^>]*>)", "$1").replaceAll("(?is)</tr[^>]*>\\s*(</tr[^>]*>)", "$1");
						page = page.replaceAll("(?is)(</tr[^>]*>)", "</td>$1");
						page = page.replaceAll("(?is)</td[^>]*>\\s*(</td[^>]*>)", "$1").replaceAll("(?is)</tr[^>]*>\\s*(</tr[^>]*>)", "$1");
						page = page.replaceAll("(?is)(<table[^>]*>)(?:\\s*</td>)?\\s*</tr>", "$1");
						
						page = page.replaceAll("(?is)(<A NAME=\\\"parties\\\">.*?)(<table)", "$1</a>$2");
						
						page = page.replaceAll("(?is)</td[^>]*>\\s*(</td[^>]*>)", "$1").replaceAll("(?is)</tr[^>]*>\\s*(</tr[^>]*>)", "$1");
						page = page.replaceAll("(?is)(</u[^>]*>)\\s*(</b[^>]*>)", "$1$2</a>");
						page = page.replaceAll("(?is)(<a\\s+href[^>]*>)([^<]*)\\s*(</td>)", "$1$2</a>$3");
						page = page.replaceAll("(?is)\\bHREF\\s*=\\s*\\\"(ck_public)", "href=\"" 
											+ CreatePartialLink(TSConnectionURL.idGET) + "/cconnect/PROD/public/$1");
						details.append(page);
						
						htmlParser = org.htmlparser.Parser.createParser(page, null);
						nl = htmlParser.parse(new TagNameFilter("table"));
						try {
							instrumentNumber.append(HtmlParser3.getValueFromNextCell(nl, "Case ID", "", false).replaceAll("(?is)&nbsp;", " ").trim());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if (StringUtils.isNotEmpty(anchorLink)) {
						req = new HTTPRequest(newLink + anchorLink);
						try {
							HTTPResponse response = ((ro.cst.tsearch.connection.http3.ARGenericCourtConnectCO) site).process(req);
							page = response.getResponseAsString();
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							HttpManager3.releaseSite(site);
						}
						htmlParser = org.htmlparser.Parser.createParser(page, null);
						nl = htmlParser.parse(new TagNameFilter("table"));
						if (nl != null){
							page = nl.elementAt(0).toHtml();
							page = page.replaceAll("(?is)<a[^>]+>\\s*<img.*?name\\s*=\\s*\\\"menu[12]\\s*\\\"[^>]*>\\s*</a>", "");
							page = page.replaceAll("(?is)(<a[^>]+>\\s*)<img.*?alt\\s*=\\s*\\\"(.*?)\\s*\\\"[^>]*>(\\s*</a>)", "$1$2$3");
							page = page.replaceAll("(?is)<a.*?href=\\\"[^#]+(#[^\\\"]+)\\\"[^>]+>", "<a href=\"$1\">");
							details.append(page);
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return details.toString();
	}
	

	protected ResultMap parseIntermediaryRow(ResultMap resultMap, TableRow row, long searchId) {
		resultMap.put("OtherInformationSet.SrcType", "CO");
		return resultMap;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "CO");
			
			detailsHtml = detailsHtml.replaceAll("&nbsp;", " ");
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			String type = HtmlParser3.getValueFromNextCell(mainList, "Type:", "", false).trim();
			//type = type.replaceAll("(?is)([^-]*).*", "$1").trim();
			resultMap.put(CourtDocumentIdentificationSetKey.PARTY_TYPE.getKeyName(), type);
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), type);
			String instrumentNumber = HtmlParser3.getValueFromNextCell(mainList, "Case ID", "", false).trim();
			
			resultMap.put(CourtDocumentIdentificationSetKey.CASE_NUMBER.getKeyName(), instrumentNumber);
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrumentNumber);
			
			String fillingDate = HtmlParser3.getValueFromNextCell(mainList, "Filing Date:", "", false).trim();
			fillingDate = fillingDate.replaceAll("(?is)(\\d+)\\s*(?:st|nd|rd|th)", "$1").replaceAll("(?is)\\s{2,}", " ").replaceAll("(?is)\\s+,\\s+", ", ").trim();
			if(org.apache.commons.lang.StringUtils.isNotBlank(fillingDate)){
				Date date = FormatDate.getDateFromFormatedString(fillingDate, FormatDate.BUDGET_FORMAT);
				SimpleDateFormat formatter = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY);
		        String sDate = formatter.format(date);
				resultMap.put(CourtDocumentIdentificationSetKey.FILLING_DATE.getKeyName(), sDate);
				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), sDate);
			}
			
			String tmpPartyGtor = "", tmpPartyGtee = "";
			String[] parties = HtmlParser3.getValueFromAbsoluteCell(-1, 1, HtmlParser3.findNode(mainList, "Filing Date"), "", true).trim().split("\n");
			String names = "";
			if (parties.length > 1){
				names = parties[1].replaceAll("(?is)\\A\\s*-\\s*", "").replaceAll("\\bET\\sAL\\b", "ETAL");
			}
			
			resultMap.put(CourtDocumentIdentificationSetKey.PARTY_NAME.getKeyName(), names);
			
			names = names.replaceAll("\\bIN\\s+RE\\b", "").trim();
			String[] partiesV = names.split("\\s+VS?\\s+");
			if (partiesV.length > 0){
				tmpPartyGtor = partiesV[0].trim();
				if (partiesV.length > 1){
					tmpPartyGtee = partiesV[1].trim();
				}
			}
			resultMap.put("tmpPartyGtor", tmpPartyGtor);
			resultMap.put("tmpPartyGtee", tmpPartyGtee);
			
			NodeList trList =  mainList.extractAllNodesThatMatch(new TagNameFilter("tr"));
			TableRow row;
			if (trList != null){
				String partyGrantor = "", partyGrantee = "";
				for (int i = 0; i < trList.size(); i++){
					row = (TableRow) trList.elementAt(i);
					if (row.getColumnCount() > 5)
					{	
						if ("JUDGE".equals(row.getColumns()[3].toPlainTextString().trim())){
							String name = row.getColumns()[5].toPlainTextString().trim();
							name = name.replaceAll("(?is)\\AHON\\.?\\s*", "");
							String[] judgeAndCircuit = name.split("-");
							String judgeName = judgeAndCircuit[0].trim();
							resultMap.put(CourtDocumentIdentificationSetKey.JUDGE_NAME.getKeyName(), judgeName);
							if (judgeAndCircuit.length > 1){
								resultMap.put(CourtDocumentIdentificationSetKey.CIRCUIT.getKeyName(), judgeAndCircuit[1].trim());
							}
						} else if (row.getColumns()[3].toPlainTextString().trim().matches("(?is)(MINOR )?(CREDITOR|PLAINTIFF|PETITIONER|INCAPACITATED|APPLICANT)")){
							partyGrantor += row.getColumns()[5].toPlainTextString().trim() + "\n";
						} else if (row.getColumns()[3].toPlainTextString().trim().matches("(?is)(DEBTOR|DEFENDANT|RESPONDENT|DECEDENT|GUARDIAN)")){
							partyGrantee += row.getColumns()[5].toPlainTextString().trim() + "\n";
						}
					}	
				}
				resultMap.put("tmpPartyGtorFromParties", partyGrantor.replaceAll(",\\s*,", ",").replaceAll("\\s*,", ","));
				resultMap.put("tmpPartyGteeFromParties", partyGrantee.replaceAll(",\\s*,", ",").replaceAll("\\s*,", ","));
			}
			
			partyNames(resultMap);
			
	    } catch (Exception e) {
			e.printStackTrace();
		}
			return null;
		}

	@SuppressWarnings("rawtypes")
	private ResultMap partyNames(ResultMap resultMap) throws RuntimeException, Exception {

		ArrayList<List> grantorBody = new ArrayList<List>();
		ArrayList<List> granteeBody = new ArrayList<List>();
		String[] suffixes, type, otherType;
		String[] names = {"", "", "", "", "", ""};
		
		String tmpGtor = (String) resultMap.get("tmpPartyGtor");
		String tmpGtee = (String) resultMap.get("tmpPartyGtee");
		
		//grantor from Case ID
		if (StringUtils.isNotEmpty(tmpGtor)){
			
			names = StringFormats.parseNameDesotoRO(tmpGtor, true);
			
			suffixes = GenericFunctions.extractAllNamesSufixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			
			GenericFunctions.addOwnerNames(tmpGtor, names, suffixes[0], suffixes[1], type, otherType, 
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), grantorBody);
		}
		//grantee from CaseID
		if (StringUtils.isNotEmpty(tmpGtee)){
			
			names = StringFormats.parseNameDesotoRO(tmpGtee, true);
			
			suffixes = GenericFunctions.extractAllNamesSufixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			
			GenericFunctions.addOwnerNames(tmpGtee, names, suffixes[0], suffixes[1], type, otherType, 
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), granteeBody);
		}
		
		String partyGrantor = (String) resultMap.get("tmpPartyGtorFromParties");
		String partyGrantee = (String) resultMap.get("tmpPartyGteeFromParties");
		
		//grantor from Case Parties table
		if (StringUtils.isNotEmpty(partyGrantor)){
			String[] parties = partyGrantor.split("\n");
			for(int i = 0; i< parties.length; i++){
				
				names = StringFormats.parseNameNashville(parties[i], true);
				
				suffixes = GenericFunctions.extractAllNamesSufixes(names);
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				
				GenericFunctions.addOwnerNames(parties[i], names, suffixes[0], suffixes[1], type, otherType, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), grantorBody);
			}
		}
		//grantee from Case Parties table
		if (StringUtils.isNotEmpty(partyGrantee)){
			String[] parties = partyGrantee.split("\n");
			for(int i = 0; i< parties.length; i++){
				
				names = StringFormats.parseNameNashville(parties[i], true);
				
				suffixes = GenericFunctions.extractAllNamesSufixes(names);
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				
				GenericFunctions.addOwnerNames(parties[i], names, suffixes[0], suffixes[1], type, otherType, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), granteeBody);
			}
		}
		
		if (!grantorBody.isEmpty()){
			resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantorBody, true));
		}
		if (!granteeBody.isEmpty()){
			resultMap.put("GranteeSet", GenericFunctions.storeOwnerInSet(granteeBody, true));
		}
		return resultMap;
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static void parseNameInterARGenericCourtConnectCO(ResultMap m, long searchId) throws Exception{
		
			String names[] = {"", "", "", "", "", ""};
			ArrayList<List> grantor = new ArrayList<List>();
			ArrayList<List> grantee = new ArrayList<List>();
			
			String tmpPartyGtor = (String)m.get("tmpPartyGtor");
			if (StringUtils.isNotEmpty(tmpPartyGtor)){
				tmpPartyGtor = tmpPartyGtor.replaceAll("\\sDBA\\s+", " / ");
				String[] gtors = tmpPartyGtor.split("/");
				for (String grantorName : gtors){
					grantorName = grantorName.replaceAll("\\bDECEASED\\b", "");
					names = StringFormats.parseNameNashville(grantorName);
					GenericFunctions.addOwnerNames(tmpPartyGtor, names, grantor);
				}
				
				m.put("SaleDataSet.Grantor", tmpPartyGtor);
				m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor));
			}
			
			String tmpPartyGtee = (String)m.get("tmpPartyGtee");
			if (StringUtils.isNotEmpty(tmpPartyGtee)){
				tmpPartyGtee = tmpPartyGtee.replaceAll("\\sDBA\\s+", " / ");
				String[] gtee = tmpPartyGtee.split("/");
				for (String granteeName : gtee){
					granteeName = granteeName.replaceAll("\\bDECEASED\\b", "");
					names = StringFormats.parseNameNashville(granteeName);
					GenericFunctions.addOwnerNames(tmpPartyGtee, names, grantee);
				}
				
				m.put("SaleDataSet.Grantee", tmpPartyGtee);
				m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee));
				
			}
		
	}
	
	private String proccessLinks(ServerResponse response) {
		String nextLink = "", prevLink = "";
		String footer = "";
		
		try {
			//String qry = response.getQuerry();
			String rsResponse = response.getResult();
			
			Matcher nextMat = NEXT_PAT.matcher(rsResponse);
			if (nextMat.find()){
				nextLink = CreatePartialLink(TSConnectionURL.idGET) + "/cconnect/PROD/public/" + nextMat.group(1);
			}
			
			Matcher priorMat = PRIOR_PAT.matcher(rsResponse);
			if (priorMat.find()){
				String nextPageNo = StringUtils.extractParameterFromUrl(nextLink, "PageNo");
				int pageNo = 1;
				try {
					pageNo = Integer.parseInt(nextPageNo);
				} catch (Exception e) {
					logger.error("Cannot obtain the next pageNo");
				}
				if (pageNo > 2){
					pageNo = pageNo - 2;
				}
				prevLink = nextLink.replaceAll("(?is)&PageNo=\\d+", "&PageNo=" + Integer.toString(pageNo));
			}
			
			if (StringUtils.isNotEmpty(prevLink)){
				footer = "<a href=\"" + prevLink + "\">Previous</a>&nbsp;&nbsp;&nbsp;";
			}
			if (StringUtils.isNotEmpty(nextLink)){
				footer += "&nbsp;&nbsp;&nbsp;<a href=\"" + nextLink + "\">Next</a>";
			}
			
			response.getParsedResponse().setNextLink( "<a href='"+nextLink+"'>Next</a>" );
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return footer;
	}
	
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
			
		ConfigurableNameIterator nameIterator = null;
				
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;	
		GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
		        
		FilterResponse alreadyPresentFilter = new RejectAlreadyPresentFilterResponse(searchId);		
		
		for (String id : gbm.getGbTransfers()) {
					  		   	    	 
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
			module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			module.getFunction(9).forceValue(counties.get(Integer.toString(miServerID)));
			module.addFilter(alreadyPresentFilter);
			addBetweenDateTest(module, true, false, true);
			module.addFilter( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module) );
			nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
			module.addIterator(nameIterator);
			
			modules.add(module);
				    	     
			if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				module.getFunction(9).forceValue(counties.get(Integer.toString(miServerID)));
				module.addFilter(alreadyPresentFilter);
				addBetweenDateTest(module, true, false, true);
				module.addFilter( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module) );
				nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
				module.addIterator(nameIterator);
				modules.add(module);
					 
			}
			
		}
		
		serverInfo.setModulesForGoBackOneLevelSearch(modules);	    
	}
			
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
				
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
				
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));				
		
		module.getFunction(9).forceValue(counties.get(Integer.toString(miServerID)));
		
		NameFilter nameFilter = new NameFilter(SearchAttributes.OWNER_OBJECT, searchId);
		nameFilter.setUseSynonymsForCandidates(true);
		module.addFilter(nameFilter);
		module.addFilter(new RejectAlreadySavedDocumentsFilterResponse(searchId));
		addBetweenDateTest(module, true, true, true);
		addFilterForUpdate(module, true);
		
		ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, true, new String[]{"L;F;"});
		iterator.setInitAgain(true);
		module.addIterator(iterator);
		
		modules.add(module);
		
		serverInfo.setModulesForAutoSearch(modules);
	}
				
	@SuppressWarnings("deprecation")
	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {

			String link = image.getLink();
			String[] links = link.split("###");
			
			byte[] imageBytes = null;
			byte[] eachImageByte = null;

	    	HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
			try {
				for (int i = 0; i < links.length; i++){
					eachImageByte = ((ro.cst.tsearch.connection.http3.ARGenericCourtConnectCO)site).getImage(links[i]);
					if (imageBytes == null){
						imageBytes = eachImageByte;
					} else {
						imageBytes = PDFUtils.mergePDFs(imageBytes, eachImageByte, true);
					}
				}
				
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				HttpManager3.releaseSite(site);
			}
			
			ServerResponse resp = new ServerResponse();
				
			if(imageBytes != null) {
				afterDownloadImage(true);
			}
			
			String imageName = image.getPath();
			if(FileUtils.existPath(imageName)){
				imageBytes = FileUtils.readBinaryFile(imageName);
			   		return new DownloadImageResult( DownloadImageResult.Status.OK, imageBytes, image.getContentType() );
			}
			    	
			resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes,
						((ImageLinkInPage)image).getContentType()));

			DownloadImageResult dres = resp.getImageResult();
			
			//System.out.println("image");

		return dres;
	}
	
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
			
		if(tsServerInfoModule != null) {
            setupSelectBox(tsServerInfoModule.getFunction(7), CASE_TYPE_SELECT);
            tsServerInfoModule.getFunction(7).setRequired(true);
            
            setupSelectBox(tsServerInfoModule.getFunction(8), PERSON_TYPE_SELECT);
            tsServerInfoModule.getFunction(8).setRequired(true);
            
            //setupSelectBox(tsServerInfoModule.getFunction(9), COUNTY_CODE_SELECT);
            //tsServerInfoModule.getFunction(9).setRequired(true);
            PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String paramName = htmlControl.getCurrentTSSiFunc().getParamName();
				if(StringUtils.isNotEmpty(paramName)) {
					if ("county_code".equals(paramName)){
						if (miServerID == 339108){//Pulaski
							setupSelectBox(tsServerInfoModule.getFunction(9), "<select name=\"county_code\"><OPTION value=\"60 - PULASKI\">60 - PULASKI</option></select>");
			            }
						//for new county modify 00000 from if condition with the corresponding of miServerID value
						/*else if (miServerID == 000000){//Faulkner
							setupSelectBox(tsServerInfoModule.getFunction(9), "<select name=\"county_code\"><OPTION value=\"23 - FAULKNER\">23 - FAULKNER</option></select>");
			            } else if (miServerID == 00000){//Garland
							setupSelectBox(tsServerInfoModule.getFunction(9), "<select name=\"county_code\"><OPTION value=\"26 - GARLAND\">26 - GARLAND</option></select>");
			            } else if (miServerID == 00000){//Hot Spring
							setupSelectBox(tsServerInfoModule.getFunction(9), "<select name=\"county_code\"><OPTION value=\"30 - HOT SPRING\">30 - HOT SPRING</option></select>");
			            } else if (miServerID == 0000){//Searcy
							setupSelectBox(tsServerInfoModule.getFunction(9), "<select name=\"county_code\"><OPTION value=\"65 - SEARCY\">65 - SEARCY</option></select>");
			            } else if (miServerID == 0000){//Van Buren
							setupSelectBox(tsServerInfoModule.getFunction(9), "<select name=\"county_code\"><OPTION value=\"71 - VAN BUREN\">71 - VAN BUREN</option></select>");
			            }*/
						tsServerInfoModule.getFunction(9).setRequired(true);
					}
				}
			}
            
            tsServerInfoModule.getFunction(11).setParamType(TSServerInfoFunction.idCheckBox);
            tsServerInfoModule.getFunction(11).setHtmlformat(
					"<INPUT TYPE=\"checkbox\" NAME=\"soundex_ind\" value=\"checked\">");
            
            tsServerInfoModule.getFunction(12).setParamType(TSServerInfoFunction.idCheckBox);
            tsServerInfoModule.getFunction(12).setHtmlformat(
					"<INPUT TYPE=\"checkbox\" NAME=\"partial_ind\" value=\"checked\">");
		}
		
		setModulesForAutoSearch(msiServerInfoDefault);
		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	private static final String CASE_TYPE_SELECT = 
		"<select name=\"case_type\">" +
		"<OPTION value=\"ALL\">ALL</option>" +
		"<OPTION value=\"11 - CRIMINAL (DO NOT USE)\">11 - CRIMINAL (DO NOT USE)</option>" +
		"<OPTION value=\"12 - CIVIL (DO NOT USE)\">12 - CIVIL (DO NOT USE)</option>" +
		"<OPTION value=\"13 - JUVENILE (DO NOT USE)\">13 - JUVENILE (DO NOT USE)</option>" +
		"<OPTION value=\"14 - DOMESTIC RELATIONS(DO NOT USE)\">14 - DOMESTIC RELATIONS(DO NOT USE)</option>" +
		"<OPTION value=\"15 - PROBATE (DO NOT USE)\">15 - PROBATE (DO NOT USE)</option>" +
		"<OPTION value=\"16 - DRUG COURT (DO NOT USE)\">16 - DRUG COURT (DO NOT USE)</option>" +
		"<OPTION value=\"30 - CRIMINAL (DO NOT USE)\">30 - CRIMINAL (DO NOT USE)</option>" +
		"<OPTION value=\"31 - TRAFFIC (DO NOT USE)\">31 - TRAFFIC (DO NOT USE)</option>" +
		"<OPTION value=\"32 - CIVIL (DO NOT USE)\">32 - CIVIL (DO NOT USE)</option>" +
		"<OPTION value=\"33 - SMALL CLAIMS (DO NOT USE)\">33 - SMALL CLAIMS (DO NOT USE)</option>" +
		"<OPTION value=\"34 - CITY DOCKET (DO NOT USE)\">34 - CITY DOCKET (DO NOT USE)</option>" +
		"<OPTION value=\"35 - COUNTY DOCKET (DO NOT USE)\">35 - COUNTY DOCKET (DO NOT USE)</option>" +
		"<OPTION value=\"41 - PROSECUTING ATTORNEY (DONTUSE)\">41 - PROSECUTING ATTORNEY (DONTUSE)</option>" +
		"<OPTION value=\"51 - PUBLIC DEFENDER (DO NOT USE)\">51 - PUBLIC DEFENDER (DO NOT USE)</option>" +
		"<OPTION value=\"52 - CONFLICTS OFFICE (DO NOT USE)\">52 - CONFLICTS OFFICE (DO NOT USE)</option>" +
		"<OPTION value=\"AA - ANCILLARY ADMINISTRATION\">AA - ANCILLARY ADMINISTRATION</option>" +
		"<OPTION value=\"AB - ARBITRATION AWARD\">AB - ARBITRATION AWARD</option>" +
		"<OPTION value=\"AD - ADOPTION\">AD - ADOPTION</option>" +
		"<OPTION value=\"AL - ALCOHOLIC COMMITMENT\">AL - ALCOHOLIC COMMITMENT</option>" +
		"<OPTION value=\"AN - ANNULMENT\">AN - ANNULMENT</option>" +
		"<OPTION value=\"AP - ADMINISTRATIVE APPEAL\">AP - ADMINISTRATIVE APPEAL</option>" +
		"<OPTION value=\"BA - BODY ATTACHMENT\">BA - BODY ATTACHMENT</option>" +
		"<OPTION value=\"BF - BAD FAITH\">BF - BAD FAITH</option>" +
		"<OPTION value=\"BW - BENCH WARRANT\">BW - BENCH WARRANT</option>" +
		"<OPTION value=\"C - CITY DOCKET TRAFFIC/CRIMINAL\">C - CITY DOCKET TRAFFIC/CRIMINAL</option>" +
		"<OPTION value=\"CD - CONDEMNATION\">CD - CONDEMNATION</option>" +
		"<OPTION value=\"CF - PROPERTY FORFEITURE\">CF - PROPERTY FORFEITURE</option>" +
		"<OPTION value=\"CG - CV-DAMAGE TO PERSONAL PROPERTY\">CG - CV-DAMAGE TO PERSONAL PROPERTY</option>" +
		"<OPTION value=\"CJ - CV-FOREIGN JUDGMENT\">CJ - CV-FOREIGN JUDGMENT</option>" +
		"<OPTION value=\"CK - CV-CONTRACT\">CK - CV-CONTRACT</option>" +
		"<OPTION value=\"CL - CV-LEGACY SYSTEM CASES\">CL - CV-LEGACY SYSTEM CASES</option>" +
		"<OPTION value=\"CN - CV-REPLEVIN\">CN - CV-REPLEVIN</option>" +
		"<OPTION value=\"CP - CONSERVATORSHIP\">CP - CONSERVATORSHIP</option>" +
		"<OPTION value=\"CR - CRIMINAL\">CR - CRIMINAL</option>" +
		"<OPTION value=\"CS - CUSTODY/VISITATION\">CS - CUSTODY/VISITATION</option>" +
		"<OPTION value=\"CT - CONTEMPT-DOMESTIC RELATIONS\">CT - CONTEMPT-DOMESTIC RELATIONS</option>" +
		"<OPTION value=\"CV - CIVIL COMMITMENT\">CV - CIVIL COMMITMENT</option>" +
		"<OPTION value=\"DA - DOMESTIC ABUSE\">DA - DOMESTIC ABUSE</option>" +
		"<OPTION value=\"DC - NARCOTIC COMMITMENT\">DC - NARCOTIC COMMITMENT</option>" +
		"<OPTION value=\"DD - DELINQUENCY DIVERSION\">DD - DELINQUENCY DIVERSION</option>" +
		"<OPTION value=\"DE - DECEDENT ESTATE\">DE - DECEDENT ESTATE</option>" +
		"<OPTION value=\"DI - CRIMINAL DIRECT FILED\">DI - CRIMINAL DIRECT FILED</option>" +
		"<OPTION value=\"DJ - DECLARATORY JUDGMENT\">DJ - DECLARATORY JUDGMENT</option>" +
		"<OPTION value=\"DM - DEMAND NOTICE\">DM - DEMAND NOTICE</option>" +
		"<OPTION value=\"DN - DEPENDENT/NEGLECT\">DN - DEPENDENT/NEGLECT</option>" +
		"<OPTION value=\"DO - DEBT:OPEN ACCOUNT\">DO - DEBT:OPEN ACCOUNT</option>" +
		"<OPTION value=\"DS - DIVORCE W/ SUPPORT\">DS - DIVORCE W/ SUPPORT</option>" +
		"<OPTION value=\"DV - DIVORCE\">DV - DIVORCE</option>" +
		"<OPTION value=\"EJ - EXTENDED JUVENILE JURISDICTION\">EJ - EXTENDED JUVENILE JURISDICTION</option>" +
		"<OPTION value=\"EL - ELECTION\">EL - ELECTION</option>" +
		"<OPTION value=\"EM - EMPLOYMENT\">EM - EMPLOYMENT</option>" +
		"<OPTION value=\"EV - EVICTION\">EV - EVICTION</option>" +
		"<OPTION value=\"EX - EXTRADITION\">EX - EXTRADITION</option>" +
		"<OPTION value=\"FC - FORECLOSURE\">FC - FORECLOSURE</option>" +
		"<OPTION value=\"FD - FINS DIVERSION\">FD - FINS DIVERSION</option>" +
		"<OPTION value=\"FJ - FOREIGN JUDGMENT-DR\">FJ - FOREIGN JUDGMENT-DR</option>" +
		"<OPTION value=\"FR - FRAUD\">FR - FRAUD</option>" +
		"<OPTION value=\"FS - FAMILY IN NEED OF SERVICES\">FS - FAMILY IN NEED OF SERVICES</option>" +
		"<OPTION value=\"FT - FINS TRUANCY\">FT - FINS TRUANCY</option>" +
		"<OPTION value=\"FV - FOREIGN JUDGMENT-CIVIL\">FV - FOREIGN JUDGMENT-CIVIL</option>" +
		"<OPTION value=\"GD - GUARDIANSHIP-PROBATE\">GD - GUARDIANSHIP-PROBATE</option>" +
		"<OPTION value=\"IJ - INJUNCTION\">IJ - INJUNCTION</option>" +
		"<OPTION value=\"IN - INCORPORATION\">IN - INCORPORATION</option>" +
		"<OPTION value=\"IS - INSURANCE\">IS - INSURANCE</option>" +
		"<OPTION value=\"JA - JUVENILE ADOPTION\">JA - JUVENILE ADOPTION</option>" +
		"<OPTION value=\"JC - JUVENILE CIVIL COMMITMENT\">JC - JUVENILE CIVIL COMMITMENT</option>" +
		"<OPTION value=\"JD - DELINQUENCY\">JD - DELINQUENCY</option>" +
		"<OPTION value=\"JF - JUVENILE FOREIGN JUDGMENT\">JF - JUVENILE FOREIGN JUDGMENT</option>" +
		"<OPTION value=\"JG - JUVENILE GUARDIANSHIP\">JG - JUVENILE GUARDIANSHIP</option>" +
		"<OPTION value=\"JI - JUVENILE INTAKE\">JI - JUVENILE INTAKE</option>" +
		"<OPTION value=\"JM - JUVENILE CONTEMPT\">JM - JUVENILE CONTEMPT</option>" +
		"<OPTION value=\"JO - JUVENILE OTHER\">JO - JUVENILE OTHER</option>" +
		"<OPTION value=\"JR - JUVENILE DRUG COURT\">JR - JUVENILE DRUG COURT</option>" +
		"<OPTION value=\"JS - JUVENILE CUSTODY/SUPPORT\">JS - JUVENILE CUSTODY/SUPPORT</option>" +
		"<OPTION value=\"JT - JUVENILE PATERNITY\">JT - JUVENILE PATERNITY</option>" +
		"<OPTION value=\"JW - JUVENILE WARRANT\">JW - JUVENILE WARRANT</option>" +
		"<OPTION value=\"K - COUNTY DOCKET TRAFFIC/CRIMINAL\">K - COUNTY DOCKET TRAFFIC/CRIMINAL</option>" +
		"<OPTION value=\"MA - MISDEMEANOR APPEAL\">MA - MISDEMEANOR APPEAL</option>" +
		"<OPTION value=\"MP - MALPRACTICE\">MP - MALPRACTICE</option>" +
		"<OPTION value=\"NC - NAME CHANGE\">NC - NAME CHANGE</option>" +
		"<OPTION value=\"NM - NEGLIGENCE-MOTOR VEHICLE\">NM - NEGLIGENCE-MOTOR VEHICLE</option>" +
		"<OPTION value=\"NO - NEGLIGENCE-OTHER\">NO - NEGLIGENCE-OTHER</option>" +
		"<OPTION value=\"OC - OTHER-CIVIL CONTRACTS\">OC - OTHER-CIVIL CONTRACTS</option>" +
		"<OPTION value=\"OD - OTHER-TORT\">OD - OTHER-TORT</option>" +
		"<OPTION value=\"OE - OTHER EQUITY\">OE - OTHER EQUITY</option>" +
		"<OPTION value=\"OM - OTHER-CIVIL MISC\">OM - OTHER-CIVIL MISC</option>" +
		"<OPTION value=\"OP - OTHER-PROBATE\">OP - OTHER-PROBATE</option>" +
		"<OPTION value=\"OT - OTHER-DOMESTIC RELATIONS\">OT - OTHER-DOMESTIC RELATIONS</option>" +
		"<OPTION value=\"P - COUNTY DOCKET TRAFFIC/CRIMINAL\">P - COUNTY DOCKET TRAFFIC/CRIMINAL</option>" +
		"<OPTION value=\"PA - PARTITION\">PA - PARTITION</option>" +
		"<OPTION value=\"PC - ADULT PROTECTIVE CUSTODY\">PC - ADULT PROTECTIVE CUSTODY</option>" +
		"<OPTION value=\"PF - PROBATION - FOREIGN JURISDICT\">PF - PROBATION - FOREIGN JURISDICT</option>" +
		"<OPTION value=\"PL - PRODUCT LIABILITY\">PL - PRODUCT LIABILITY</option>" +
		"<OPTION value=\"PN - DEBT:PROMISSORY NOTE\">PN - DEBT:PROMISSORY NOTE</option>" +
		"<OPTION value=\"PR - PROBATION REVOCATION\">PR - PROBATION REVOCATION</option>" +
		"<OPTION value=\"PS - PATERNITY SUPPORT\">PS - PATERNITY SUPPORT</option>" +
		"<OPTION value=\"PT - PATERNITY\">PT - PATERNITY</option>" +
		"<OPTION value=\"QT - QUIET TITLE\">QT - QUIET TITLE</option>" +
		"<OPTION value=\"RD - REMOVE DISABILITIES\">RD - REMOVE DISABILITIES</option>" +
		"<OPTION value=\"RE - REPLEVIN\">RE - REPLEVIN</option>" +
		"<OPTION value=\"RH - REVIEW HEARING\">RH - REVIEW HEARING</option>" +
		"<OPTION value=\"RR - REOPEN CRIMINAL\">RR - REOPEN CRIMINAL</option>" +
		"<OPTION value=\"RX - DRUG COURT CASE\">RX - DRUG COURT CASE</option>" +
		"<OPTION value=\"S - COUNTY DOCKET TRAFFIC/CRIMINAL\">S - COUNTY DOCKET TRAFFIC/CRIMINAL</option>" +
		"<OPTION value=\"SE - SMALL ESTATE\">SE - SMALL ESTATE</option>" +
		"<OPTION value=\"SF - SC-FOREIGN JUDGMENT\">SF - SC-FOREIGN JUDGMENT</option>" +
		"<OPTION value=\"SG - SC-DAMAGE TO PERSONAL PROPERTY\">SG - SC-DAMAGE TO PERSONAL PROPERTY</option>" +
		"<OPTION value=\"SI - CITY CRIMINAL SUMMONS\">SI - CITY CRIMINAL SUMMONS</option>" +
		"<OPTION value=\"SK - SC-CONTRACT\">SK - SC-CONTRACT</option>" +
		"<OPTION value=\"SL - SC-LEGACY SYSTEM CASES\">SL - SC-LEGACY SYSTEM CASES</option>" +
		"<OPTION value=\"SM - SEPARATE MAINTENANCE\">SM - SEPARATE MAINTENANCE</option>" +
		"<OPTION value=\"SN - SC-REPLEVIN\">SN - SC-REPLEVIN</option>" +
		"<OPTION value=\"SO - COUNTY CRIMINAL SUMMONS\">SO - COUNTY CRIMINAL SUMMONS</option>" +
		"<OPTION value=\"SP - CHILD SUPPORT\">SP - CHILD SUPPORT</option>" +
		"<OPTION value=\"SW - SEARCH WARRANT\">SW - SEARCH WARRANT</option>" +
		"<OPTION value=\"TA - TRUST ADMINISTRATION\">TA - TRUST ADMINISTRATION</option>" +
		"<OPTION value=\"TC - TEEN COURT\">TC - TEEN COURT</option>" +
		"<OPTION value=\"TI - TRANSFER IN\">TI - TRANSFER IN</option>" +
		"<OPTION value=\"TP - TERMINATION OF PARENTAL RIGHTS\">TP - TERMINATION OF PARENTAL RIGHTS</option>" +
		"<OPTION value=\"TR - TRAFFIC\">TR - TRAFFIC</option>" +
		"<OPTION value=\"UD - UNLAWFUL DETAINER\">UD - UNLAWFUL DETAINER</option>" +
		"<OPTION value=\"WI - CITY ARREST WARRANT\">WI - CITY ARREST WARRANT</option>" +
		"<OPTION value=\"WO - COUNTY ARREST WARRANT\">WO - COUNTY ARREST WARRANT</option>" +
		"<OPTION value=\"WT - WRITS\">WT - WRITS</option>" +
		"<OPTION value=\"X1 - PA GENERAL\">X1 - PA GENERAL</option>" +
		"<OPTION value=\"X2 - PA DOM VIOLENCE/SEX ASSAULT\">X2 - PA DOM VIOLENCE/SEX ASSAULT</option>" +
		"<OPTION value=\"X3 - PA DRUGS\">X3 - PA DRUGS</option>" +
		"<OPTION value=\"X4 - PA GANGS\">X4 - PA GANGS</option>" +
		"<OPTION value=\"X5 - PA HOMICIDE\">X5 - PA HOMICIDE</option>" +
		"<OPTION value=\"X6 - PA CAPITAL\">X6 - PA CAPITAL</option>" +
		"<OPTION value=\"X7 - PA HOT CHECK\">X7 - PA HOT CHECK</option>" +
		"<OPTION value=\"X8 - PA JUVENILE\">X8 - PA JUVENILE</option>" +
		"<OPTION value=\"Y1 - PD ADULT PROT SERVICE\">Y1 - PD ADULT PROT SERVICE</option>" +
		"<OPTION value=\"Y2 - PD JUVENILE\">Y2 - PD JUVENILE</option>" +
		"<OPTION value=\"Y3 - PD CRIMINAL\">Y3 - PD CRIMINAL</option>" +
		"<OPTION value=\"Y4 - PD CRIMINAL - FELONY\">Y4 - PD CRIMINAL - FELONY</option>" +
		"<OPTION value=\"Y5 - PD CRIMINAL - HOMICIDE\">Y5 - PD CRIMINAL - HOMICIDE</option>" +
		"<OPTION value=\"Y6 - PD CRIMINAL - CAPITAL\">Y6 - PD CRIMINAL - CAPITAL</option>" +
		"<OPTION value=\"Y7 - PD MISDEMEANOR\">Y7 - PD MISDEMEANOR</option>" +
		"<OPTION value=\"Y8 - PD DISTRICT COURT MISDEMEANOR\">Y8 - PD DISTRICT COURT MISDEMEANOR</option>" +
		"<OPTION value=\"Y9 - PD DISTRICT COURT FELONY\">Y9 - PD DISTRICT COURT FELONY</option>" +
		"<OPTION value=\"YA - CO ADULT PROT SERVICE\">YA - CO ADULT PROT SERVICE</option>" +
		"<OPTION value=\"YB - CO JUVENILE\">YB - CO JUVENILE</option>" +
		"<OPTION value=\"YC - CO CRIMINAL\">YC - CO CRIMINAL</option>" +
		"<OPTION value=\"YD - CO CRIMINAL - FELONY\">YD - CO CRIMINAL - FELONY</option>" +
		"<OPTION value=\"YE - CO CRIMINAL - HOMICIDE\">YE - CO CRIMINAL - HOMICIDE</option>" +
		"<OPTION value=\"YF - CO CRIMINAL - CAPITAL\">YF - CO CRIMINAL - CAPITAL</option>" +
		"<OPTION value=\"YG - CO MISDEMEANOR\">YG - CO MISDEMEANOR</option>" +
		"<OPTION value=\"Z0 - EQUITY\">Z0 - EQUITY</option>" +
		"<OPTION value=\"Z1 - INJUNCTION\">Z1 - INJUNCTION</option>" +
		"<OPTION value=\"Z2 - NAME CHANGE\">Z2 - NAME CHANGE</option>" +
		"<OPTION value=\"Z3 - QUIET TITLE\">Z3 - QUIET TITLE</option>" +
		"<OPTION value=\"Z4 - REMOVAL OF DISABILITIES\">Z4 - REMOVAL OF DISABILITIES</option>" +
		"<OPTION value=\"Z5 - URESA IN\">Z5 - URESA IN</option>" +
		"<OPTION value=\"Z6 - URESA OUT\">Z6 - URESA OUT</option>" +
		"<OPTION value=\"Z7 - CERTIFICATE OF ASSESSMENT\">Z7 - CERTIFICATE OF ASSESSMENT</option>" +
		"<OPTION value=\"Z8 - CERTIFICATE OF INDEBTEDNESS\">Z8 - CERTIFICATE OF INDEBTEDNESS</option>" +
		"<OPTION value=\"Z9 - OTHER NON-CV MISCELLANEOUS\">Z9 - OTHER NON-CV MISCELLANEOUS</option>" +
		"<OPTION value=\"ZA - LICENSED PROCESS SERVER\">ZA - LICENSED PROCESS SERVER</option>" +
		"<OPTION value=\"ZB - CIVIL FILEBOX\">ZB - CIVIL FILEBOX</option>" +
		"<OPTION value=\"ZC - CRIMINAL FILEBOX\">ZC - CRIMINAL FILEBOX</option>" +
		"<OPTION value=\"ZD - JUVENILE FILEBOX\">ZD - JUVENILE FILEBOX</option>" +
		"<OPTION value=\"ZE - PROBATE FILEBOX\">ZE - PROBATE FILEBOX</option>" +
		"<OPTION value=\"ZF - OTHER FILEBOX\">ZF - OTHER FILEBOX</option>" +
		"<OPTION value=\"ZG - BACKGROUND CHECK\">ZG - BACKGROUND CHECK</option>" +
		"<OPTION value=\"ZH - OLD CASES INDEX\">ZH - OLD CASES INDEX</option>" +
		"<OPTION value=\"ZI - WILLS FOR SAFEKEEPING\">ZI - WILLS FOR SAFEKEEPING</option>" +
		"<OPTION value=\"ZJ - SUPPORT PAYMENTS\">ZJ - SUPPORT PAYMENTS</option>" +
		"</select>";
	
	private static final String PERSON_TYPE_SELECT = 
		"<select name=\"person_type\">" +
		"<OPTION value=\"ALL\">ALL</option>" +
		"<OPTION value=\"A - ATTORNEY\">A - ATTORNEY</option>" +
		"<OPTION value=\"AAL - ATTORNEY AD LITEM\">AAL - ATTORNEY AD LITEM</option>" +
		"<OPTION value=\"AD3 - 3RD PARTY DEFENDANT ATTORNEY\">AD3 - 3RD PARTY DEFENDANT ATTORNEY</option>" +
		"<OPTION value=\"ADF - DEFENDANT ATTORNEY\">ADF - DEFENDANT ATTORNEY</option>" +
		"<OPTION value=\"ADIN - INTERVENOR DEFENDANT ATTORNEY\">ADIN - INTERVENOR DEFENDANT ATTORNEY</option>" +
		"<OPTION value=\"ADMN - ADMINISTRATOR / ADMINISTRATRIX\">ADMN - ADMINISTRATOR / ADMINISTRATRIX</option>" +
		"<OPTION value=\"ADOS - ADOPTION SPECIALIST\">ADOS - ADOPTION SPECIALIST</option>" +
		"<OPTION value=\"AFF - AFFIANT\">AFF - AFFIANT</option>" +
		"<OPTION value=\"AGRN - GARNISHEE ATTORNEY\">AGRN - GARNISHEE ATTORNEY</option>" +
		"<OPTION value=\"AIF - ATTORNEY-IN-FACT\">AIF - ATTORNEY-IN-FACT</option>" +
		"<OPTION value=\"AINT - INTERVENOR ATTORNEY\">AINT - INTERVENOR ATTORNEY</option>" +
		"<OPTION value=\"AJV - JUVENILE ATTORNEY\">AJV - JUVENILE ATTORNEY</option>" +
		"<OPTION value=\"APA - PROSECUTING ATTORNEY\">APA - PROSECUTING ATTORNEY</option>" +
		"<OPTION value=\"APD - PUBLIC DEFENDER\">APD - PUBLIC DEFENDER</option>" +
		"<OPTION value=\"APIN - INTERVENOR PLAINTIFF ATTORNEY\">APIN - INTERVENOR PLAINTIFF ATTORNEY</option>" +
		"<OPTION value=\"APL - PLAINTIFF ATTORNEY\">APL - PLAINTIFF ATTORNEY</option>" +
		"<OPTION value=\"APRO - PRO SE\">APRO - PRO SE</option>" +
		"<OPTION value=\"ASGR - ASSIGNOR\">ASGR - ASSIGNOR</option>" +
		"<OPTION value=\"B - BONDSMAN\">B - BONDSMAN</option>" +
		"<OPTION value=\"BBAG - BAIL BOND AGENT\">BBAG - BAIL BOND AGENT</option>" +
		"<OPTION value=\"BC - BACKGROUND CHECK\">BC - BACKGROUND CHECK</option>" +
		"<OPTION value=\"BD - BRIDE\">BD - BRIDE</option>" +
		"<OPTION value=\"BLFF - BAILIFF\">BLFF - BAILIFF</option>" +
		"<OPTION value=\"BOND - BAIL BOND COMPANY\">BOND - BAIL BOND COMPANY</option>" +
		"<OPTION value=\"BU - BUSINESS\">BU - BUSINESS</option>" +
		"<OPTION value=\"CAD - CHILD FOR ADOPTION\">CAD - CHILD FOR ADOPTION</option>" +
		"<OPTION value=\"CASA - COURT APPT SPECIAL ADVOCATE\">CASA - COURT APPT SPECIAL ADVOCATE</option>" +
		"<OPTION value=\"CASS - CASA SUPERVISOR\">CASS - CASA SUPERVISOR</option>" +
		"<OPTION value=\"CDE - CHILD DOM REL/EQUITY\">CDE - CHILD DOM REL/EQUITY</option>" +
		"<OPTION value=\"CHIL - CHILD\">CHIL - CHILD</option>" +
		"<OPTION value=\"CLAM - CLAIMANT\">CLAM - CLAIMANT</option>" +
		"<OPTION value=\"COMP - COMPLAINANT\">COMP - COMPLAINANT</option>" +
		"<OPTION value=\"CONS - CONSERVATOR\">CONS - CONSERVATOR</option>" +
		"<OPTION value=\"CORR - CORRECTIONAL FACILITY\">CORR - CORRECTIONAL FACILITY</option>" +
		"<OPTION value=\"COUN - COUNSELING PROVIDER(S)\">COUN - COUNSELING PROVIDER(S)</option>" +
		"<OPTION value=\"CTRC - COUNTER CLAIMANT\">CTRC - COUNTER CLAIMANT</option>" +
		"<OPTION value=\"CTRD - COUNTER DEFENDANT\">CTRD - COUNTER DEFEaNDANT</option>" +
		"<OPTION value=\"CTRP - COUNTER PLAINTIFF\">CTRP - COUNTER PLAINTIFF</option>" +
		"<OPTION value=\"CUST - CUSTODIAN\">CUST - CUSTODIAN</option>" +
		"<OPTION value=\"D - DEFENDANT\">D - DEFENDANT</option>" +
		"<OPTION value=\"D3D - 3RD PARTY DEFENDANT\">D3D - 3RD PARTY DEFENDANT</option>" +
		"<OPTION value=\"DCF1 - DCFS SUPERVISOR\">DCF1 - DCFS SUPERVISOR</option>" +
		"<OPTION value=\"DCF2 - DCFS SECONDARY CASEWORKER\">DCF2 - DCFS SECONDARY CASEWORKER</option>" +
		"<OPTION value=\"DCFS - DCFS CASEWORKER\">DCFS - DCFS CASEWORKER</option>" +
		"<OPTION value=\"DECD - DECEDENT\">DECD - DECEDENT</option>" +
		"<OPTION value=\"DIN - INTERVENOR DEFENDANT\">DIN - INTERVENOR DEFENDANT</option>" +
		"<OPTION value=\"DM - DEMAND FOR NOTICE\">DM - DEMAND FOR NOTICE</option>" +
		"<OPTION value=\"DMAP - DEMAND NOTICE APPLICANT\">DMAP - DEMAND NOTICE APPLICANT</option>" +
		"<OPTION value=\"DPRO - PRO SE DEFENDANT\">DPRO - PRO SE DEFENDANT</option>" +
		"<OPTION value=\"EDR - EDUCATIONAL REPRESENTATIVE\">EDR - EDUCATIONAL REPRESENTATIVE</option>" +
		"<OPTION value=\"EXEC - EXECUTOR / EXECUTRIX\">EXEC - EXECUTOR / EXECUTRIX</option>" +
		"<OPTION value=\"FAD - ADOPTIVE FATHER\">FAD - ADOPTIVE FATHER</option>" +
		"<OPTION value=\"FATH - FATHER\">FATH - FATHER</option>" +
		"<OPTION value=\"FID - FIDUCIARY\">FID - FIDUCIARY</option>" +
		"<OPTION value=\"FOST - FOSTER CARE FACILITY\">FOST - FOSTER CARE FACILITY</option>" +
		"<OPTION value=\"FPAR - FOSTER PARENTS\">FPAR - FOSTER PARENTS</option>" +
		"<OPTION value=\"GARN - GARNISHEE\">GARN - GARNISHEE</option>" +
		"<OPTION value=\"GM - GROOM\">GM - GROOM</option>" +
		"<OPTION value=\"GPAR - GRANDPARENT\">GPAR - GRANDPARENT</option>" +
		"<OPTION value=\"GUAL - GUARDIAN AD LITEM\">GUAL - GUARDIAN AD LITEM</option>" +
		"<OPTION value=\"GUAR - GUARDIAN\">GUAR - GUARDIAN</option>" +
		"<OPTION value=\"HEIR - HEIR / DEVISEE\">HEIR - HEIR / DEVISEE</option>" +
		"<OPTION value=\"I - INTERPRETER\">I - INTERPRETER</option>" +
		"<OPTION value=\"INCP - INCAPACITATED / INCOMPETENT\">INCP - INCAPACITATED / INCOMPETENT</option>" +
		"<OPTION value=\"INT - INTERVENOR\">INT - INTERVENOR</option>" +
		"<OPTION value=\"INTN - INTERN\">INTN - INTERN</option>" +
		"<OPTION value=\"J - JUDGE\">J - JUDGE</option>" +
		"<OPTION value=\"JDCO - JUVENILE DRUG COURT OFFICER\">JDCO - JUVENILE DRUG COURT OFFICER</option>" +
		"<OPTION value=\"JREF - SMALL CLAIMS REFEREE\">JREF - SMALL CLAIMS REFEREE</option>" +
		"<OPTION value=\"JSPE - SPECIAL JUDGE\">JSPE - SPECIAL JUDGE</option>" +
		"<OPTION value=\"JUV - JUVENILE\">JUV - JUVENILE</option>" +
		"<OPTION value=\"M - MEDIATOR\">M - MEDIATOR</option>" +
		"<OPTION value=\"MAD - ADOPTIVE MOTHER\">MAD - ADOPTIVE MOTHER</option>" +
		"<OPTION value=\"MI - MINISTER\">MI - MINISTER</option>" +
		"<OPTION value=\"MIN - MINOR\">MIN - MINOR</option>" +
		"<OPTION value=\"MOT - MOTHER\">MOT - MOTHER</option>" +
		"<OPTION value=\"O - OFFICER\">O - OFFICER</option>" +
		"<OPTION value=\"OCC - OFFICE OF CHIEF COUNSEL\">OCC - OFFICE OF CHIEF COUNSEL</option>" +
		"<OPTION value=\"OTHR - OTHER\">OTHR - OTHER</option>" +
		"<OPTION value=\"OW - OWNER\">OW - OWNER</option>" +
		"<OPTION value=\"P - PLAINTIFF\">P - PLAINTIFF</option>" +
		"<OPTION value=\"P3P - 3RD PARTY PLAINTIFF\">P3P - 3RD PARTY PLAINTIFF</option>" +
		"<OPTION value=\"PADO - PRE-ADOPTIVE PARENT\">PADO - PRE-ADOPTIVE PARENT</option>" +
		"<OPTION value=\"PAR - PARENT\">PAR - PARENT</option>" +
		"<OPTION value=\"PARO - PAROLE OFFICER\">PARO - PAROLE OFFICER</option>" +
		"<OPTION value=\"PC - PARENT COUNSEL\">PC - PARENT COUNSEL</option>" +
		"<OPTION value=\"PCA - PARENT COUNSEL APPOINTED\">PCA - PARENT COUNSEL APPOINTED</option>" +
		"<OPTION value=\"PCP - PARENT COUNSEL PRIVATE\">PCP - PARENT COUNSEL PRIVATE</option>" +
		"<OPTION value=\"PERS - PERSONAL REPRESENTATIVE\">PERS - PERSONAL REPRESENTATIVE</option>" +
		"<OPTION value=\"PETR - PETITIONER\">PETR - PETITIONER</option>" +
		"<OPTION value=\"PG - PARENT/GUARDIAN\">PG - PARENT/GUARDIAN</option>" +
		"<OPTION value=\"PIN - INTERVENOR PLAINTIFF\">PIN - INTERVENOR PLAINTIFF</option>" +
		"<OPTION value=\"PINV - PRIVATE INVESTIGATOR\">PINV - PRIVATE INVESTIGATOR</option>" +
		"<OPTION value=\"PPRO - PRO SE PLAINTIFF\">PPRO - PRO SE PLAINTIFF</option>" +
		"<OPTION value=\"PRO - PROBATION OFFICER\">PRO - PROBATION OFFICER</option>" +
		"<OPTION value=\"PS - PROCESS SERVER\">PS - PROCESS SERVER</option>" +
		"<OPTION value=\"PUFA - PUTATIVE FATHER\">PUFA - PUTATIVE FATHER</option>" +
		"<OPTION value=\"PUP - PUTATIVE PARENT\">PUP - PUTATIVE PARENT</option>" +
		"<OPTION value=\"REF - REFEREE\">REF - REFEREE</option>" +
		"<OPTION value=\"RELC - RELATIVE CAREGIVER\">RELC - RELATIVE CAREGIVER</option>" +
		"<OPTION value=\"RELO - RELATIVE (OTHER)\">RELO - RELATIVE (OTHER)</option>" +
		"<OPTION value=\"REP - COURT REPORTER\">REP - COURT REPORTER</option>" +
		"<OPTION value=\"RESP - RESPONDENT\">RESP - RESPONDENT</option>" +
		"<OPTION value=\"SCHL - SCHOOL\">SCHL - SCHOOL</option>" +
		"<OPTION value=\"SIBL - SIBLING\">SIBL - SIBLING</option>" +
		"<OPTION value=\"SPAG - SERVICE PROCESS AGENT\">SPAG - SERVICE PROCESS AGENT</option>" +
		"<OPTION value=\"SS - SPECIAL SERVICES\">SS - SPECIAL SERVICES</option>" +
		"<OPTION value=\"STFA - STEP-FATHER\">STFA - STEP-FATHER</option>" +
		"<OPTION value=\"STMO - STEP-MOTHER\">STMO - STEP-MOTHER</option>" +
		"<OPTION value=\"STPA - STEP-PARENT\">STPA - STEP-PARENT</option>" +
		"<OPTION value=\"TRMT - TREATMENT PROVIDER\">TRMT - TREATMENT PROVIDER</option>" +
		"<OPTION value=\"TRST - TRUSTEE\">TRST - TRUSTEE</option>" +
		"<OPTION value=\"V - VICTIM\">V - VICTIM</option>" +
		"<OPTION value=\"VPO - VOLUNTEER PROBATION OFFICER\">VPO - VOLUNTEER PROBATION OFFICER</option>" +
		"<OPTION value=\"W - WITNESS\">W - WITNESS</option>" +
		"<OPTION value=\"WARD - WARD\">WARD - WARD</option>" +
		"<OPTION value=\"WILL - WILL FOR SAFEKEEPING\">WILL - WILL FOR SAFEKEEPING</option>" +
		"<OPTION value=\"WITX - EXPERT WITNESS\">WITX - EXPERT WITNESS</option>" +
		"<OPTION value=\"XCLM - CROSS CLAIMANT\">XCLM - CROSS CLAIMANT</option>" +
		"<OPTION value=\"XD - CROSS DEFENDANT\">XD - CROSS DEFENDANT</option>" +
		"<OPTION value=\"XP - CROSS PLAINTIFF\">XP - CROSS PLAINTIFF</option>" +
		"<OPTION value=\"ZAPF - AFFILIATED PARTY\">ZAPF - AFFILIATED PARTY</option>" +
		"</select>";
		
}


	
