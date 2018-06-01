package ro.cst.tsearch.servers.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.Span;
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
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterForNext;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.CourtDocumentIdentificationSet.CourtDocumentIdentificationSetKey;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class OHCuyahogaCO extends TSServer {
		
	private static final long serialVersionUID = 464410777353499984L;
	
	public static final String CV_TYPE_URL = "/CV_CaseInformation";
	public static final String CR_TYPE_URL = "/CR_CaseInformation";
	public static final String COA_TYPE_URL = "/COA_CaseInformation";

	public OHCuyahogaCO(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	public OHCuyahogaCO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		if (viParseID!=ID_SAVE_TO_TSD && rsResponse.contains("Case Summary")) {
			viParseID = ID_DETAILS;
		}
		
		String stringURI = ro.cst.tsearch.connection.http3.OHCuyahogaCO.getStringURI(Response.getLastURI());
				
		switch (viParseID) {
								
		case ID_SEARCH_BY_INSTRUMENT_NO:	//CIVIL SEARCH BY CASE
		case ID_SEARCH_BY_NAME:				//CIVIL SEARCH BY NAME
		case ID_SEARCH_BY_MODULE40:			//FORECLOSURE SEARCH
		case ID_SEARCH_BY_MODULE41:			//CRIMINAL SEARCH BY CASE
		case ID_SEARCH_BY_MODULE42:			//CRIMINAL SEARCH BY NAME
		case ID_SEARCH_BY_MODULE44:			//COURT OF APPEALS SEARCH BY CASE
		case ID_SEARCH_BY_MODULE45:			//COURT OF APPEALS SEARCH BY NAME
			
			if (rsResponse.contains("Server Error in '/' Application.")) {
				Response.getParsedResponse().setError("Server error!");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			String s = "/Error.aspx?error=";
			int index = stringURI.indexOf(s);
			if (index>-1 && stringURI.length()>index+s.length()) {
				String error = stringURI.substring(index+s.length());
				if (error.length()>0) {
					Response.getParsedResponse().setError(error);
					Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
					return;
				}
			}
						
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
			
			if(smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				if (viParseID==ID_SEARCH_BY_MODULE42 && getSearch().getSearchType()!=Search.PARENT_SITE_SEARCH) {
					parsedResponse.setAttribute(ParsedResponse.SERVER_RECURSIVE_ANALYZE, true);
				}
            } 
			
			Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
			
			String header = parsedResponse.getHeader();
			String footer = parsedResponse.getFooter();
			
			header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + header;
			
			if (numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
				footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument);
			} else {
				footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
			}

			parsedResponse.setHeader(header);
			parsedResponse.setFooter(footer);
			
			break;	
				
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
			
			StringBuilder serialNumber = new StringBuilder();
			String details = getDetails(Response, stringURI, serialNumber);
			String filename = serialNumber + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("dataSource", getDataSite().getSiteTypeAbrev());
				if (stringURI.contains(CV_TYPE_URL)||stringURI.contains(COA_TYPE_URL)) {
					data.put("type", "CIVIL");
				} else if (stringURI.contains(CR_TYPE_URL)) {
					data.put("type", "CRIMINAL");
				}
				if (isInstrumentSaved(serialNumber.toString(), null, data)){
					details += CreateFileAlreadyInTSD();
					sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + "/Details.aspx" + "?caseno=" + serialNumber;
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);
				
			} else {
				smartParseDetails(Response,details);
				String detailsWithLinks = details;
								
				//remove links
				details = details.replaceAll("(?is)<a[^>]+href=[^>]*>([^<]*)</a>", "$1");
				
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
				Response.getParsedResponse().setResponse(details);
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				saveRelatedDocuments(Response, detailsWithLinks);
				
			}
			break;
		
		case ID_GET_LINK :
			ParseResponse(sAction, Response, rsResponse.contains("Case Summary")?ID_DETAILS:ID_SEARCH_BY_INSTRUMENT_NO);
			break;	
			
		default:
			break;
		}
	}
	
	protected void saveRelatedDocuments(ServerResponse Response, String detailsWithLinks) {
		
		Matcher ma = Pattern.compile("(?is)href=\"([^\"]+)\"").matcher(detailsWithLinks);
		while (ma.find()) {
			ParsedResponse prChild = new ParsedResponse();
			String link = ma.group(1) + "&isSubResult=true";
			LinkInPage pl = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
			prChild.setPageLink(pl);
			Response.getParsedResponse().addOneResultRowOnly(prChild);
		}
		
	}
	
	public static String getDiv(String response) {
		String result = "";
		
		try {
			
			if (!StringUtils.isEmpty(response)) {
				org.htmlparser.Parser htmlParser2 = org.htmlparser.Parser.createParser(response, null);
				NodeList nodeList2 = htmlParser2.parse(null);
				NodeList divs = nodeList2.extractAllNodesThatMatch(new TagNameFilter("div"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "art-postcontent"));
				if (divs.size()>0) {
					Node div = divs.elementAt(0);
					NodeList children = div.getChildren();
					for (int i=0;i<children.size();i++) {
						Node ch = children.elementAt(i);
						if (ch instanceof Div) {
							Div d = (Div)ch;
							if ("art-post".equals(d.getAttribute("class"))) {
								children.remove(i);
								break;
							}
						}
					}
					children = div.getChildren();
					for (int i=0;i<children.size();i++) {
						Node ch = children.elementAt(i);
						if (ch instanceof Div) {
							Div d = (Div)ch;
							if ("text-align: right".equals(d.getAttribute("class"))) {
								children.remove(i);
								break;
							}
						}
					}
					return div.toHtml();
				}
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}	
		
		return result;
	}
	
	protected String getDetails(ServerResponse Response, String stringURI, StringBuilder instrNumber) {
		try {
			
			String rsResponse = Response.getResult().replaceAll("&#39;", "'");
			
			StringBuilder sb = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
									
			/* If from memory - use it as is */
			if(!rsResponse.toLowerCase().contains("<html")) {
				String instNo = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseHeader_lblCaseNumHeader", nodeList, true);
				instrNumber.append(instNo.trim());
				return rsResponse;
			}
			
			String instNo = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseHeader_lblCaseNumHeader", nodeList, true);
			instrNumber.append(instNo.trim());
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "art-post-inner"));
			
			if (tables.size()>0) {
				sb.append(tables.elementAt(0).toHtml());
			}
			
			if (stringURI.contains(CV_TYPE_URL)||stringURI.contains(COA_TYPE_URL)) {		//for Civil Cases add 'Property' info
				
				Matcher ma = Pattern.compile("(?is)<a[^>]+href=\"javascript:__doPostBack\\('([^']+)',''\\)\"[^>]*>Property</a>").matcher(rsResponse);
				if (ma.find()) {
					String viewState = ro.cst.tsearch.connection.http3.OHCuyahogaCO
						.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaCO.VIEW_STATE_PARAM, rsResponse);
					String eventValidation = ro.cst.tsearch.connection.http3.OHCuyahogaCO
						.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_VALIDATION_PARAM, rsResponse);
					HTTPRequest req = new HTTPRequest(stringURI, HTTPRequest.POST);
					req.setPostParameter(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_TARGET_PARAM, ma.group(1));
					req.setPostParameter(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_ARGUMENT_PARAM, "");
					req.setPostParameter(ro.cst.tsearch.connection.http3.OHCuyahogaCO.VIEW_STATE_PARAM, viewState);
					req.setPostParameter(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_VALIDATION_PARAM, eventValidation);
					String response = "";
					HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
					try {
						HTTPResponse resp = site.process(req);
						response = resp.getResponseAsString();
					} catch(Exception e) {
						e.printStackTrace();
					} finally {
						HttpManager3.releaseSite(site);
					}
					sb.append("<br/>").append(getDiv(response));
					
				}
				
				sb.append("<span id=\"documentType\" style=\"visibility:hidden;display:none\">Civil</span>");
				
			} else if (stringURI.contains(CR_TYPE_URL)) {		//for Criminal Cases add 'Docket', 'Costs', 'Defendant' and 'Attorney' info
				
				List<String> targets = new ArrayList<String>();
				Matcher ma = Pattern.compile("(?is)<span[^>]+id=\"SheetContentPlaceHolder_caseHeader_Label\\d+\"[^>]*>[^<]+</span>\\s*<a[^>]+href=\"javascript:__doPostBack\\('([^']+)',''\\)\"[^>]*>([^<]+)</a>")
					.matcher(rsResponse);
				while (ma.find()) {
					if (!ma.group(2).matches("(?is)\\s*New\\s+Search\\s*")) {
						targets.add(ma.group(1));						//links to other pages ('Docket', 'Costs', 'Defendant' and 'Attorney')
					}
				}
				
				String viewState = ro.cst.tsearch.connection.http3.OHCuyahogaCO
					.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaCO.VIEW_STATE_PARAM, rsResponse);
				String eventValidation = ro.cst.tsearch.connection.http3.OHCuyahogaCO
					.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_VALIDATION_PARAM, rsResponse);
				
				for (String s: targets) {
					
					HTTPRequest req = new HTTPRequest(stringURI, HTTPRequest.POST);
					req.setPostParameter(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_TARGET_PARAM, s);
					req.setPostParameter(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_ARGUMENT_PARAM, "");
					req.setPostParameter(ro.cst.tsearch.connection.http3.OHCuyahogaCO.VIEW_STATE_PARAM, viewState);
					req.setPostParameter(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_VALIDATION_PARAM, eventValidation);
					String response = "";
					HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
					try {
						HTTPResponse resp = site.process(req);
						response = resp.getResponseAsString();
						stringURI = ro.cst.tsearch.connection.http3.OHCuyahogaCO.getStringURI(resp.getLastURI());
					} catch(Exception e) {
						e.printStackTrace();
					} finally {
						HttpManager3.releaseSite(site);
					}
					
					String det = getDiv(response);
					
					if (stringURI.contains("/CR_CaseInformation_Costs.aspx")) {
						org.htmlparser.Parser htmlParser2 = org.htmlparser.Parser.createParser(det, null);
						NodeList nodeList2 = htmlParser2.parse(null);
						NodeList costTables = HtmlParser3.getNodesByID("SheetContentPlaceHolder_caseCosts_gvCosts", nodeList2, true);	
						if (costTables.size()>0) {
							TableTag costsTable = (TableTag)costTables.elementAt(0);
							int rowCount = costsTable.getRowCount(); 
							if (rowCount>1) {
								TableRow lastRow = costsTable.getRow(rowCount-1);
								if (lastRow.getColumnCount()==1) {		//last row contains links to previous/next pages 
									String lastRowString = lastRow.toHtml();
									
									StringBuilder additionalRows = new StringBuilder();
									
									viewState = ro.cst.tsearch.connection.http3.OHCuyahogaCO
										.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaCO.VIEW_STATE_PARAM, response);
									eventValidation = ro.cst.tsearch.connection.http3.OHCuyahogaCO
										.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_VALIDATION_PARAM, response);
									
									Matcher ma2 = Pattern.compile("(?is)<a[^>]+href=\"javascript:__doPostBack\\('([^']+)','([^']+)'\\)\">").matcher(lastRowString.replaceAll("&#39;", "'"));
									while (ma2.find()) {			//next pages
										HTTPRequest req2 = new HTTPRequest(stringURI, HTTPRequest.POST);
										req2.setPostParameter(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_TARGET_PARAM, ma2.group(1));
										req2.setPostParameter(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_ARGUMENT_PARAM, ma2.group(2));
										req2.setPostParameter(ro.cst.tsearch.connection.http3.OHCuyahogaCO.VIEW_STATE_PARAM, viewState);
										req2.setPostParameter(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_VALIDATION_PARAM, eventValidation);
										String response2 = "";
										HttpSite3 site2 = HttpManager3.getSite(getCurrentServerName(), searchId);
										try {
											HTTPResponse resp2 = site2.process(req2);
											response2 = resp2.getResponseAsString();
											stringURI = ro.cst.tsearch.connection.http3.OHCuyahogaCO.getStringURI(resp2.getLastURI());
										} catch(Exception e) {
											e.printStackTrace();
										} finally {
											HttpManager3.releaseSite(site);
										}
										viewState = ro.cst.tsearch.connection.http3.OHCuyahogaCO
											.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaCO.VIEW_STATE_PARAM, response);
										eventValidation = ro.cst.tsearch.connection.http3.OHCuyahogaCO
											.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_VALIDATION_PARAM, response);
										
										org.htmlparser.Parser htmlParser3 = org.htmlparser.Parser.createParser(response2, null);
										NodeList nodeList3 = htmlParser3.parse(null);
										NodeList costTables2 = HtmlParser3.getNodesByID("SheetContentPlaceHolder_caseCosts_gvCosts", nodeList3, true);
										
										if (costTables2.size()>0) {
											TableTag costsTable2 = (TableTag)costTables2.elementAt(0);
											for (int i=1;i<costsTable2.getRowCount();i++) {
												TableRow tr = costsTable2.getRow(i);
												if (tr.getColumnCount()>1) {
													additionalRows.append(tr.toHtml());
												}
											}
										}	
										
									}
									
									det = det.replace(lastRowString, additionalRows.toString());
								}
							}
						}
					} else if (stringURI.contains("/CR_CaseInformation_Defendant.aspx")) {
						det = det.replaceAll("(?is)(<span[^>]+id=\"SheetContentPlaceHolder_def[A-Z]+_Label1\"[^>]*>)([^<]+)(</span>)", "$1 Defendant $2 $3");
					}
					
					viewState = ro.cst.tsearch.connection.http3.OHCuyahogaCO
						.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaCO.VIEW_STATE_PARAM, response);
					eventValidation = ro.cst.tsearch.connection.http3.OHCuyahogaCO
						.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_VALIDATION_PARAM, response);
					
					sb.append(det);
					
				}
				
				sb.append("<span id=\"documentType\" style=\"visibility:hidden;display:none\">Criminal</span>");
				
			}
			
			String details = sb.toString();
			
			details = details.replaceAll("(?is)<a[^>]+>\\s*<img[^<]+>\\s*</a>", "");
			details = details.replaceAll("(?is)<table[^>]+class=\"hide_print\"[^>]*>.*?</table>", "");
			details = details.replaceAll("(?is)<div[^>]*>\\s*<a[^>]+>\\s*Printer\\s+Friendly\\s+Version\\s*</a>\\s*</div>", "");
			details = details.replaceAll("(?is)<div[^>]+>\\s*</div>", "");
			details = details.replaceAll("(?is)<div>\\s*<center>\\s*<table[^>]+width=\"50%\"[^>]*>.*?</table>\\s*</center>\\s*</div>", "");
			details = details.replaceAll("(?is)<div[^>]+id=\"SheetContentPlaceHolder_caseDocket_pnlFilters\"[^>]*>.*?</div>", "");
			details = details.replaceAll("(?is)<a[^>]+href\\s*=\"([^\"]+)\">([^<]+)</a>", "$2");
			
			StringBuffer stringBuffer = new StringBuffer();
			Matcher ma = Pattern.compile("(?is)<a[^>]+href\\s*='([^']+)'>([^<]+)</a>").matcher(details);
			while (ma.find()) {
				
				if (ma.group(1).startsWith("CR_CaseInformation_Summary.aspx") && ma.group(2).matches("(?is)[A-Z]{2}-\\d{2}-\\d+(-[A-Z]+)?")) {
					String linkString = CreatePartialLink(TSConnectionURL.idGET) + ma.group(1);
					ma.appendReplacement(stringBuffer, "<a href=\"" + linkString + "\">" + ma.group(2) + "</a>");
				} else {
					ma.appendReplacement(stringBuffer, ma.group(2));
				}
				
			}
			ma.appendTail(stringBuffer);
			details = stringBuffer.toString();
			
			return details;
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
		
	@SuppressWarnings("unchecked")
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		int numberOfUncheckedElements = 0;
		
		TableTag resultsTable = null;
		
		String baseLink = "";
		URI lastURI = response.getLastURI();
		if (lastURI!=null) {
			try {
				baseLink = lastURI.getURI();
				int index = baseLink.lastIndexOf("/");
				if (index>-1) {
					baseLink = baseLink.substring(index);
				}
			} catch (URIException e) {
				e.printStackTrace();
			}
		}
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTable = null;
			
			int moduleIdx = -1;
			TSServerInfoModule moduleSource = null;
			Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
			if (objectModuleSource != null) {
				if (objectModuleSource instanceof TSServerInfoModule) {
					moduleSource = (TSServerInfoModule) objectModuleSource;
				}
			}
			if (moduleSource!=null) {
				moduleIdx = moduleSource.getModuleIdx();
			} else if (baseLink.contains("/CaseInfoByName.aspx")) {
				moduleIdx = TSServerInfo.MODULE_IDX43;
			} else {
				LinkInPage lip = response.getParsedResponse().getPageLink();
				if (lip!=null) {
					String link = lip.getLink();
					String seq = StringUtils.extractParameter(link, ro.cst.tsearch.connection.http3.OHCuyahogaCO.SEQ_PARAM + "=([^&?]*)");
					if (!StringUtils.isEmpty(seq)) {
						Map<String, String> params = (Map<String, String>)mSearch.getAdditionalInfo(getCurrentServerName() + ":params:" + seq);
						String idx = params.get("moduleIdx");
						try {
							moduleIdx = Integer.parseInt(idx);
						} catch (NumberFormatException nfe) {}
					}
				} 
			}
			
			int moduleType = -1;
			
			if (moduleIdx==TSServerInfo.INSTR_NO_MODULE_IDX || moduleIdx==TSServerInfo.MODULE_IDX44) {		//CIVIL SEARCH BY CASE, COURT OF APPEALS SEARCH BY CASE
				mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id","SheetContentPlaceHolder_ctl00_gvMyCases"), true);
				moduleType = 0; //civil
			} else if (moduleIdx==TSServerInfo.NAME_MODULE_IDX || moduleIdx==TSServerInfo.MODULE_IDX42 || moduleIdx==TSServerInfo.MODULE_IDX45) {	//CIVIL SEARCH BY NAME
				mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)														//CRIMINAL SEARCH BY NAME (first level)
					.extractAllNodesThatMatch(new HasAttributeFilter("id","SheetContentPlaceHolder_ctl00_gvNameResults"), true);					//COURT OF APPEALS SEARCH BY NAME
				if (moduleIdx==TSServerInfo.NAME_MODULE_IDX || moduleIdx==TSServerInfo.MODULE_IDX45) {
					moduleType = 0; //civil
				} else {
					moduleType = 1; //criminal
				}
			} else if (moduleIdx==TSServerInfo.MODULE_IDX40) {												//FORECLOSURE SEARCH
				mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id","SheetContentPlaceHolder_ctl00_gvForeclosureResutls"), true);
				moduleType = 0; //civil
			} else if (moduleIdx==TSServerInfo.MODULE_IDX41) {												//CRIMINAL SEARCH BY CASE
				mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id","SheetContentPlaceHolder_ctl00_gvCaseResults"), true);
				moduleType = 1; //criminal
			} else if (moduleIdx==TSServerInfo.MODULE_IDX43) {												//CRIMINAL SEARCH BY NAME (second level)
				mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id","SheetContentPlaceHolder_info_gvCaseResults"), true);
				moduleType = 1; //criminal
			}
			
			if (mainTable!=null && mainTable.size()>0) {
				resultsTable = (TableTag)mainTable.elementAt(0);
			}
			
			if (resultsTable != null && resultsTable.getRowCount()>1) {
				
				String hrefPatt = "(?is)href=\"([^\"]+)\"";
				
				TableRow[] rows  = resultsTable.getRows();
				String header = rows[0].toHtml().replaceAll("(?is)<a[^>]+href=[^>]*>([^<]*)</a>", "$1");
				String footer = "";
				
				Set<String> addedColumns = new HashSet<String>(); 
				
				int len = rows.length;
				for (int i=1;i<len;i++) {
					
					TableRow row = rows[i];
					
					if (row.getColumnCount()==1) { 	//row with previous/next links
						
						String nextLink = "";
						String rowHtml = row.toHtml().replaceAll("&#39;", "'");
						
						int currentPageIndex = -1;
						Matcher ma1 = Pattern.compile("(?is)<td>\\s*<span>\\s*\\d+\\s*</span>\\s*</td>").matcher(rowHtml);
						if (ma1.find()) {
							currentPageIndex = rowHtml.indexOf(ma1.group(0));
						}
						
						StringBuffer sb = new StringBuffer();
						Matcher ma2 = Pattern.compile("(?is)<a[^>]+href=\"javascript:__doPostBack\\('([^\"]+)','([^\"]+)'\\)\">").matcher(rowHtml);
						while (ma2.find()) {
							
							int seq = getSeq();
							String link = CreatePartialLink(TSConnectionURL.idPOST) + baseLink + "?" + ro.cst.tsearch.connection.http3.OHCuyahogaCO.SEQ_PARAM + "=" + seq;
							if (StringUtils.isEmpty(nextLink) && rowHtml.indexOf(ma2.group(0))>currentPageIndex) {
								nextLink = link;
							}
							
							String viewState = ro.cst.tsearch.connection.http3.OHCuyahogaCO
								.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaCO.VIEW_STATE_PARAM, table);
							String eventValidation = ro.cst.tsearch.connection.http3.OHCuyahogaCO
								.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_VALIDATION_PARAM, table);
							Map<String, String> params = new HashMap<String, String>();
							params.put(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_TARGET_PARAM, ma2.group(1));
							params.put(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_ARGUMENT_PARAM, ma2.group(2));
							params.put(ro.cst.tsearch.connection.http3.OHCuyahogaCO.VIEW_STATE_PARAM, viewState);
							params.put(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_VALIDATION_PARAM, eventValidation);
							params.put("moduleIdx", Integer.toString(moduleIdx));
							mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
							
							ma2.appendReplacement(sb, "<a href=\"" + link + "\">");
							
						}
						ma2.appendTail(sb);
						footer = sb.toString();
						
						if (!StringUtils.isEmpty(nextLink)) {
							response.getParsedResponse().setNextLink("<a href=\"" + nextLink + "\">Next</a>");
						}
						
					} else {						//ordinary row
						
						String link = "";
						String htmlRow = row.toHtml();
						
						ResultMap m = ro.cst.tsearch.servers.functions.OHCuyahogaCO.parseIntermediaryRow(row, moduleType);
						m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
						String caseno = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()));
						
						if (moduleIdx!=TSServerInfo.MODULE_IDX42) {		//CRIMINAL SEARCH BY NAME (first level) does not have case number in intermediary
							if (addedColumns.contains(caseno)) {
								continue;
							}
						}
						
						addedColumns.add(caseno);
						
						//civil: when clicking on details link, make a search with the case number using 'CIVIL SEARCH BY CASE' module (the result is unique)
						//this is needed in automatic search when there are more pages of results 
						if (moduleType==0) {
							link = CreatePartialLink(TSConnectionURL.idPOST) + "/Details.aspx" + "?caseno=" + caseno;
						} else {					//criminal: the result with case number using 'CRIMINAL SEARCH BY NAME' module is not unique (e.g. CR-08-514348)
							int seq = getSeq();		//also, in automatic search criminal search is not used 
							Map<String, String> params = new HashMap<String, String>();
							String viewState = ro.cst.tsearch.connection.http3.OHCuyahogaCO
								.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaCO.VIEW_STATE_PARAM, table);
							String eventValidation = ro.cst.tsearch.connection.http3.OHCuyahogaCO
								.getParameterValueFromHtml(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_VALIDATION_PARAM, table);
							params.put(ro.cst.tsearch.connection.http3.OHCuyahogaCO.VIEW_STATE_PARAM, viewState);
							params.put(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_VALIDATION_PARAM, eventValidation);
							params.put(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_ARGUMENT_PARAM, "");
							
							link = CreatePartialLink(TSConnectionURL.idPOST) + baseLink + "?" + ro.cst.tsearch.connection.http3.OHCuyahogaCO.SEQ_PARAM + "=" + seq;
							
							htmlRow = htmlRow.replaceAll("&#39;", "'");
								
							Matcher matcher = Pattern.compile("(?is)<a[^>]+href=\"javascript:__doPostBack\\('([^']+)',''\\)\"[^>]*>").matcher(htmlRow);
							if (matcher.find()) {
								params.put(ro.cst.tsearch.connection.http3.OHCuyahogaCO.EVENT_TARGET_PARAM, matcher.group(1));
							}
							mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
						}
							
						htmlRow = htmlRow.replaceAll(hrefPatt, Matcher.quoteReplacement("href=\"" + link + "\""));
							
						ParsedResponse currentResponse = new ParsedResponse();
							
						if (moduleIdx==TSServerInfo.MODULE_IDX42) {
							currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_GO_TO_LINK));
						} else {
							currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
						}
											
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,htmlRow);
						currentResponse.setOnlyResponse(htmlRow);
							
						Bridge bridge = new Bridge(currentResponse, m, searchId);
							
						DocumentI document = (RegisterDocumentI)bridge.importData();
						currentResponse.setDocument(document);
							
						String checkBox = "checked";
						String instrNo = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()));
						HashMap<String, String> data = new HashMap<String, String>();
						data.put("instrno", instrNo);
						data.put("type", org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName())));
						
						String rowType = "1";
						if (i%2!=0) {
							rowType = "2";
						}
						
						if (isInstrumentSaved(instrNo, document, data, false)) {
							checkBox = "saved";
						} else {
							numberOfUncheckedElements++;
							LinkInPage linkInPage = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
							checkBox = "<input type='checkbox' name='docLink' value='" + link + "'>";
							currentResponse.setPageLink(linkInPage);
						}
						String replacement = Matcher.quoteReplacement("<tr class=\"row" + rowType + "\">" + (moduleIdx!=TSServerInfo.MODULE_IDX42?"<td align=\"center\">" + checkBox + "</td>":""));
						htmlRow = htmlRow.replaceFirst("(?is)<tr[^>]*>", replacement);
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
						currentResponse.setOnlyResponse(htmlRow);
						intermediaryResponse.add(currentResponse);
					}
					 
				}
				
				header = header.replaceFirst("(?is)<tr[^>]*>","");
				header = "<table align=\"center\" width=\"965px\"><tr><td><table style=\"width:100%;border:1px;border-collapse:collapse\" align=\"center\"><tr>" 
					+ (moduleIdx!=TSServerInfo.MODULE_IDX42?"<th align=\"center\">" + SELECT_ALL_CHECKBOXES + "</th>":"") + header;
						
				footer += "</table></td></tr></table><br>";
				
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter(footer);
												
				outputTable.append(table);
				SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
				
			}
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,	String detailsHtml, ResultMap resultMap) {
				
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
			
			String instNo = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseHeader_lblCaseNumHeader", nodeList, true);
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instNo.trim());
			
			String civilCriminal = HtmlParser3.getNodeValueByID("documentType", nodeList, true);
			
			if ("Civil".equals(civilCriminal)) {
				
				resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "CIVIL");
				
				String recordedDate = "";
				if (response.getQuerry().contains("=coacase")) {
					recordedDate = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseSummary_lblNoticeOfAppealDate", nodeList, true);
				} else {
					recordedDate = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseSummary_lblFilingDate", nodeList, true);
				}
				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate.trim());
				
				String judgeName = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseSummary_lblJudge", nodeList, true);
				resultMap.put(CourtDocumentIdentificationSetKey.JUDGE_NAME.getKeyName(), judgeName.trim());
				
				String parcelNumber = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseProperty_gvProperties_lblParcel_0", nodeList, true);
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelNumber.trim());
				
				String streetNo = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseProperty_gvProperties_lblStreetNbr_0", nodeList, true);
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo.trim());
				
				String streetName = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseProperty_gvProperties_lblStreetName_0", nodeList, true);
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName.trim());
				
				String city = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseProperty_gvProperties_lblCity_0", nodeList, true);
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city.trim());
				
				String zip = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseProperty_gvProperties_lblZip_0", nodeList, true);
				resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip.trim());
				
				StringBuilder grantor = new StringBuilder();
				StringBuilder grantee = new StringBuilder();
				
				Node parties = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "table", "id", "SheetContentPlaceHolder_caseParties_grdCaseParties", true);
				if (parties!=null && parties instanceof TableTag) {
					TableTag table = (TableTag)parties;
					for (int i=0;i<table.getRowCount();i++) {
						TableRow row = table.getRow(i);
						if (row.getColumnCount()>1) {
							String label = row.getColumns()[0].toPlainTextString();
							String s = "";
							NodeList nl = row.getColumns()[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true);
							for (int j=0;j<2;j++) {
								if (nl.size()>j) {
									Span node = (Span)nl.elementAt(j);
									String attr = node.getAttribute("id");
									if (attr.startsWith("SheetContentPlaceHolder_caseParties_grdCaseParties_lblName_")||
											attr.equals("SheetContentPlaceHolder_caseParties_grdCaseParties_lblAddressLine1_3")) {
										s = node.toPlainTextString().trim();
									}
								}
								if (!StringUtils.isEmpty(s)) {
									if (label.contains("PLAINTIFF") || label.contains("CREDITOR") || label.contains("APPELLANT")) {
										grantor.append(s).append(" / ");
									} else if (label.contains("DEFENDANT") || label.contains("DEBTOR") || label.contains("APPELLEE")) {
										grantee.append(s).append(" / ");
									}
								}
							}
						}
					}
				}
				
				String grantorFL = grantor.toString().replaceFirst(" / $", "").trim();
				String granteeFL = grantee.toString().replaceFirst(" / $", "").trim();
				if (!StringUtils.isEmpty(grantorFL)) {
					resultMap.put("tmpGrantorFL", grantorFL);
				}
				if (!StringUtils.isEmpty(granteeFL)) {
					resultMap.put("tmpGranteeFL", granteeFL);
				}
				
				
			} else if ("Criminal".equals(civilCriminal)) {
				
				resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "CRIMINAL");
				
				String judgeName = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseSummary_lblJudgeName", nodeList, true);
				resultMap.put(CourtDocumentIdentificationSetKey.JUDGE_NAME.getKeyName(), judgeName.trim());
				
				String references = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseSummary_otherCases_lblOtherCases", nodeList, true).trim();
				String[] split = references.split(",");
				if (split.length>0) {
					List<List> bodyCR = new ArrayList<List>();
					List<String> line;
					for (String s: split) {
						if (!s.matches("(?is)\\s*N/A\\s*")) {
							line = new ArrayList<String>();
							line.add(s.trim());
							bodyCR.add(line);	
						}
					}
					if (bodyCR.size() > 0) {
						String[] header = { CrossRefSetKey.INSTRUMENT_NUMBER.getShortKeyName()};
						ResultTable rt = GenericFunctions2.createResultTable(bodyCR, header);
						resultMap.put("CrossRefSet", rt);
					}
				}
				
				List<String> list = new ArrayList<String>();
				
				String caseTitle = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseHeader_lblCaseTitleHeader", nodeList, true);
				String[] spl = caseTitle.split("\\bvs\\.");
				if (spl.length==2) {
					resultMap.put("tmpGrantorFL", spl[0].trim());
					list.add(spl[1].trim().replaceFirst("\\s{2}", " "));
				}
				
				StringBuilder grantee = new StringBuilder();
				String defName = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseSummary_lblName", nodeList, true).replaceFirst("\\s{2}", " ");
				if (!StringUtils.isEmpty(defName)) {
					list.add(defName);
				}
				Node alias = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "table", "id", "SheetContentPlaceHolder_defAlias_gvAlias", true);
				if (alias!=null && alias instanceof TableTag) {
					TableTag table = (TableTag)alias;
					for (int i=1;i<table.getRowCount();i++) {
						String s = table.getRow(i).getColumns()[0].toPlainTextString().trim().replaceFirst("\\s{2}", " ");
						if (!list.contains(s)) {
							list.add(s);
						}
					}
				}
				String coDefName = HtmlParser3.getNodeValueByID("SheetContentPlaceHolder_caseSummary_otherDefs_lblCoDefs", nodeList, true);
				if (!StringUtils.isEmpty(coDefName) && !coDefName.matches("(?is)\\s*N/A\\s*")) {
					String[] splt = coDefName.split(",");
					for (int i=0;i<splt.length;i++) {
						list.add(splt[i].replaceFirst("(?s)\\(.*?\\)\\s*$", ""));
					}
				}
				if (list.size()>0) {
					for (int i=0;i<list.size()-1;i++) {
						grantee.append(list.get(i)).append(" / ");
					}
					grantee.append(list.get(list.size()-1));
				}
				resultMap.put("tmpGranteeFL", grantee.toString());
				
			}
			
			ro.cst.tsearch.servers.functions.OHCuyahogaCO.parseNames(resultMap);
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
		
	public Date parseDate(String value) {
		Date date = null;
		if (value!=null) {
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
			try {
				date = sdf.parse(value);
			} catch (ParseException pe) {}
		}
		return date;
	}
	
	@Override
    public ServerResponse GetLink(String vsRequest, boolean vbEncodedOrIsParentSite) throws ServerResponseException {
    	    	
    	String link = StringUtils.extractParameter(vsRequest, "Link=(.*)");
    	if (link.contains("/Details.aspx")) {
    		
    		SearchDataWrapper searchDataWrapper = new SearchDataWrapper();
    		int moduleIdx = TSServerInfo.INSTR_NO_MODULE_IDX;
    		
    		String caseno = StringUtils.extractParameter(vsRequest, "caseno=([^&?]*)");
    		String type = "";
			String year = "";
			String no = "";
			String[] split = caseno.split("-");
			if (split.length==3) {
				type = split[0];
				year = split[1];
				try {
					int yearInt = Integer.parseInt(year);
					if (yearInt<50) {
						year = "20" + year;
					} else {
						year = "19" + year;
					}
				} catch (NumberFormatException nfe) {}
				no = split[2];
				if ("CA".equals(split[0])) {
					moduleIdx = TSServerInfo.MODULE_IDX44;
				}
			}
			
    		
			TSServerInfoModule module = getCurrentClassServerInfo().getModuleForSearch(moduleIdx, searchDataWrapper);
			module.setData(0, type);
			module.setData(1, year);
			module.setData(2, no);
			module.setVisible(false);
			setDoNotLogSearch(true);
			try {
				TSServerInfoModule lastModule = getSearch().getSearchRecord().getModule();
				ServerResponse response = performAction(REQUEST_SEARCH_BY, vsRequest, module, searchDataWrapper);
				getSearch().getSearchRecord().setModule(lastModule);	//keep the original module (necessary for the validators which are applied only if the results were not filtered)
				return response;
			} finally {}
    		
        }
    	
    	return super.GetLink(vsRequest, vbEncodedOrIsParentSite);
    	
    }
	
	@Override
	public Object getCachedDocument(String key) {
		if (key.contains("/Details.aspx")) {
			try {
				ServerResponse sr = GetLink("Link=" + key, true);
				String s = sr.getParsedResponse().getResponse();
				s = s.replaceAll("(?is)</?form[^>]*>", "");
				s = s.replaceAll("(?is)<input[^>]+>", "");
				s = s.replaceFirst("(?is)<hr>\\s*$", "");
				return s;
			} catch (ServerResponseException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
        if (module.getModuleIdx() == TSServerInfo.INSTR_NO_MODULE_IDX || module.getModuleIdx() == TSServerInfo.MODULE_IDX44) {	//CIVIL SEARCH BY CASE, COURT OF APPEALS SEARCH BY CASE
        	String caseNumber = module.getFunction(2).getParamValue();
        	if (!StringUtils.isEmpty(caseNumber) && !caseNumber.matches("\\d{1,6}")) {		//invalid Case Number
        		logSearchBy(module);
       		 	ServerResponse response = new ServerResponse();
                response.getParsedResponse().setError("<html>Invalid Case Number! It must have maximum 6 digits!</html>");
                logInitialResponse(response);
                return response;
        	}
        } else if (module.getModuleIdx() == TSServerInfo.MODULE_IDX40) {					//FORECLOSURE SEARCH
        	String parcelNumber = module.getFunction(0).getParamValue();
        	if (!StringUtils.isEmpty(parcelNumber) && !parcelNumber.matches("\\d{3,8}")) {	//invalid Parcel Number
        		logSearchBy(module);
       		 	ServerResponse response = new ServerResponse();
                response.getParsedResponse().setError("<html>Invalid Case Number! It must have between 3 and 8 digits!</html>");
                logInitialResponse(response);
                return response;
        	}
        	String filingDate = module.getFunction(4).getParamValue();
        	if (!StringUtils.isEmpty(filingDate)) {											//invalid Filing Date
        		Date date1 = parseDate(filingDate);
            	if (date1==null) {
            		logSearchBy(module);
           		 	ServerResponse response = new ServerResponse();
                    response.getParsedResponse().setError("<html>Invalid Filing Date!</html>");
                    logInitialResponse(response);
                    return response;
            	}
        	}
        	String toDate = module.getFunction(5).getParamValue();
        	if (!StringUtils.isEmpty(toDate)) {												//invalid to Date
        		Date date2 = parseDate(toDate);
            	if (date2==null) {
            		logSearchBy(module);
           		 	ServerResponse response = new ServerResponse();
                    response.getParsedResponse().setError("<html>Invalid to Date!</html>");
                    logInitialResponse(response);
                    return response;
            	}
        	}
        } else if (module.getModuleIdx() == TSServerInfo.MODULE_IDX41) {					//CRIMINAL SEARCH BY CASE
        	String caseNumber = module.getFunction(2).getParamValue();
        	if (!StringUtils.isEmpty(caseNumber) && !caseNumber.matches(".{1,6}")) {		//invalid Case Number
        		logSearchBy(module);
       		 	ServerResponse response = new ServerResponse();
                response.getParsedResponse().setError("<html>Invalid Case Number! It must have maximum 6 characters!</html>");
                logInitialResponse(response);
                return response;
        	}
        }
        return super.SearchBy(module, sd);
    }
	
	@Override
	public ServerResponse SaveToTSD (String vsRequest, Map<String, Object> extraParams) throws ServerResponseException {
		String link = StringUtils.extractParameter(vsRequest, "Link=(.*)");
    	if (link.contains("/Details.aspx")) {
    		String caseno = StringUtils.extractParameter(vsRequest, "caseno=(.*)");
    		DocumentsManagerI manager = getSearch().getDocManager();
    		try {
    			manager.getAccess();
    			List<DocumentI> list = manager.getDocumentsWithDataSource(false, getDataSite().getSiteTypeAbrev());
    			for (DocumentI document: list) {
    				if (caseno.equals(document.getInstno())) {
    					SearchLogger.info(getPrettyFollowedLink(link), searchId);
    					return new ServerResponse();
    				}
    			}
    		} finally {
    			manager.releaseAccess();
    		}
    	}
		return super.SaveToTSD(vsRequest, extraParams);
    }
			
	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		Search search = getSearch();
		
		if (search.getSearchType() == Search.GO_BACK_ONE_LEVEL_SEARCH) {
			
			int[] moduleIndexes = new int[]{TSServerInfo.NAME_MODULE_IDX, TSServerInfo.MODULE_IDX45};
			
			ConfigurableNameIterator nameIterator = null;
			SearchAttributes sa = search.getSa();
			TSServerInfoModule module;
			GBManager gbm = (GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

			for (String id : gbm.getGbTransfers()) {

				//person
				{
					for (int index: moduleIndexes) {
						module = new TSServerInfoModule(serverInfo.getModule(index));
						module.setIndexInGB(id);
						module.setTypeSearchGB("grantor");
						module.clearSaKeys();
						module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
						
						GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
						defaultNameFilter.setIgnoreMiddleOnEmpty(true);
						defaultNameFilter.setUseArrangements(false);
						module.addFilter(defaultNameFilter);
						DocsValidator defaultNameValidator = defaultNameFilter.getValidator();
						defaultNameValidator.setOnlyIfNotFiltered(true);
						module.addValidator(defaultNameValidator);
						
						module.addFilterForNext(new NameFilterForNext(searchId));
						
						module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
						module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
						nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] {"L;F;" });
						nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
						nameIterator.clearSearchedNames();
						nameIterator.setInitAgain(true);
						module.addIterator(nameIterator);
						
						modules.add(module);
					}
				}
				
				//company
				{
					for (int index: moduleIndexes) {
						module = new TSServerInfoModule(serverInfo.getModule(index));
						module.setIndexInGB(id);
						module.setTypeSearchGB("grantor");
						module.clearSaKeys();
						module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
						
						module.addFilterForNext(new NameFilterForNext(searchId));
						
						GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
						defaultNameFilter.setIgnoreMiddleOnEmpty(true);
						defaultNameFilter.setUseArrangements(false);
						module.addFilter(defaultNameFilter);
						DocsValidator defaultNameValidator = defaultNameFilter.getValidator();
						defaultNameValidator.setOnlyIfNotFiltered(true);
						module.addValidator(defaultNameValidator);
						
						module.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
						nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,	new String[] {"L;F;" });
						nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
						nameIterator.clearSearchedNames();
						nameIterator.setInitAgain(true);
						module.addIterator(nameIterator);
						
						modules.add(module);
					}
				}
				
				if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
					
					//person
					{
						for (int index: moduleIndexes) {
							module = new TSServerInfoModule(serverInfo.getModule(index));
							module.setIndexInGB(id);
							module.setTypeSearchGB("grantee");
							module.clearSaKeys();
							module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
							
							module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
							module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
							
							module.addFilterForNext(new NameFilterForNext(searchId));
							
							GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
							defaultNameFilter.setIgnoreMiddleOnEmpty(true);
							defaultNameFilter.setUseArrangements(false);
							module.addFilter(defaultNameFilter);
							DocsValidator defaultNameValidator = defaultNameFilter.getValidator();
							defaultNameValidator.setOnlyIfNotFiltered(true);
							module.addValidator(defaultNameValidator);
							
							nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,	new String[] {"L;F;" });
							nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
							nameIterator.clearSearchedNames();
							nameIterator.setInitAgain(true);
							module.addIterator(nameIterator);
							
							modules.add(module);
						}
					}
					
					//company
					{
						for (int index: moduleIndexes) {
							module = new TSServerInfoModule(serverInfo.getModule(index));
							module.setIndexInGB(id);
							module.setTypeSearchGB("grantee");
							module.clearSaKeys();
							module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
							
							module.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
							
							module.addFilterForNext(new NameFilterForNext(searchId));
							
							GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
							module.addFilter(defaultNameFilter);
							DocsValidator defaultNameValidator = defaultNameFilter.getValidator();
							defaultNameValidator.setOnlyIfNotFiltered(true);
							module.addValidator(defaultNameValidator);
							
							nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,	new String[] {"L;F;" });
							nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
							nameIterator.clearSearchedNames();
							nameIterator.setInitAgain(true);
							module.addIterator(nameIterator);
							
							modules.add(module);
						}
					}
					
				}
			}
		
		}
		
		serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}
			
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
				
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		if(getSearch().getSearchType() == Search.AUTOMATIC_SEARCH) {
			
			String[] keys = new String[]{SearchAttributes.OWNER_OBJECT, SearchAttributes.BUYER_OBJECT};
			int[] moduleIndexes = new int[]{TSServerInfo.NAME_MODULE_IDX, TSServerInfo.MODULE_IDX45};	//CIVIL SEARCH BY NAME, COURT OF APPEALS SEARCH BY NAME
			
			for (String key: keys) {
				//person
				{
					for (int index: moduleIndexes) {
						TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(index));
						module.clearSaKeys();
						module.setSaObjKey(key);
						
						module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
						module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
						ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, true, new String[]{"L;F;"});
						iterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
						iterator.setInitAgain(true);
						module.addIterator(iterator);
						
						module.addFilterForNext(new NameFilterForNext(searchId));
						GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(key, searchId, module);
						module.addFilter(nameFilter);
						BetweenDatesFilterResponse betweenDatesFilter = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module);
						module.addFilter(betweenDatesFilter);
						DocsValidator nameValidator = nameFilter.getValidator();
						nameValidator.setOnlyIfNotFiltered(true);
						module.addValidator(nameValidator);
						DocsValidator betweenDatesValidator = betweenDatesFilter.getValidator();
						betweenDatesValidator.setOnlyIfNotFiltered(true);
						module.addValidator(betweenDatesValidator);
									
						modules.add(module);
					}
				}
				//company
				{
					for (int index: moduleIndexes) {
						TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(index));
						module.clearSaKeys();
						module.setSaObjKey(key);
						
						module.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
						ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, true, new String[]{"L;F;"});
						iterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
						iterator.setInitAgain(true);
						module.addIterator(iterator);
						
						module.addFilterForNext(new NameFilterForNext(searchId));
						GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(key, searchId, module);
						module.addFilter(nameFilter);
						BetweenDatesFilterResponse betweenDatesFilter = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module);
						module.addFilter(betweenDatesFilter);
						DocsValidator nameValidator = nameFilter.getValidator();
						nameValidator.setOnlyIfNotFiltered(true);
						module.addValidator(nameValidator);
						DocsValidator betweenDatesValidator = betweenDatesFilter.getValidator();
						betweenDatesValidator.setOnlyIfNotFiltered(true);
						module.addValidator(betweenDatesValidator);
						
						modules.add(module);
					}
				}
			}
			
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}
	
}
