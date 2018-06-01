package ro.cst.tsearch.servers.types;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterForNext;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.CourtDocumentIdentificationSet.CourtDocumentIdentificationSetKey;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;

public class OHGenericCourtViewCO extends TSServer {
		
	private static final long serialVersionUID = 2910706085415699989L;
	
	public OHGenericCourtViewCO(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	public OHGenericCourtViewCO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) {
								
		case ID_SEARCH_BY_NAME:				//Name Search
		case ID_SEARCH_BY_INSTRUMENT_NO:	//Case Number Search
			
			if (rsResponse.contains("No Matches Found")) {
				Response.getParsedResponse().setError(NO_DATA_FOUND);
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			if (rsResponse.contains("Last Name and First Name or Company Name is required.")) {
				Response.getParsedResponse().setError("Last Name and First Name or Company Name is required.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			if (rsResponse.contains("Last Name and First Name and Company Name should not all be populated.")) {
				Response.getParsedResponse().setError("Last Name and First Name and Company Name should not all be populated.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			if (rsResponse.contains("is shorter than the minimum of 2 characters.")) {
				Response.getParsedResponse().setError("Last Name is shorter than the minimum of 2 characters.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			if (rsResponse.contains("Please enter the date in the following format:")) {
				Response.getParsedResponse().setError("Please enter the date in the following format: 'MM/dd/yyyy'.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
			
			if(smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
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
			StringBuilder caseType = new StringBuilder();
			String details = getDetails(Response, serialNumber, caseType);
			String filename = serialNumber + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("dataSource", "CO");
				data.put("type", caseType.toString());
				if (isInstrumentSaved(serialNumber.toString(), null, data)){
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);
				
			} else {
				smartParseDetails(Response,details);
								
				//remove links
				details = details.replaceAll("(?is)<a[^>]+href=[^>]*>([^<]*)</a>", "$1");
				
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
				Response.getParsedResponse().setResponse(details);
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				
			}
			break;
		
		case ID_GET_LINK :
			ParseResponse(sAction, Response, rsResponse.contains("Back to Search Results")?ID_DETAILS:ID_SEARCH_BY_INSTRUMENT_NO);
			break;	
			
		default:
			break;
		}
	}
	
	protected String getDetails(ServerResponse Response, StringBuilder instrNumber, StringBuilder caseType) {
		try {
			
			String rsResponse = Response.getResult();
			
			//formatted text from Docket Information
			StringBuffer stringBuffer = new StringBuffer();
			Matcher ma = Pattern.compile("(?is)<td[^>]+class=\"formattedText\"[^>]*>(.*?)</td>").matcher(rsResponse);
			while (ma.find()) {
				String text = ma.group(1);
				text = text.replace("\r\n", "<br>");
				text = text.replace("\r", "<br>");
				text = text.replace("\n", "<br>");
				text = Matcher.quoteReplacement(text);
				ma.appendReplacement(stringBuffer, "<td>" + text + "</td>");
			}
			ma.appendTail(stringBuffer);
			rsResponse = stringBuffer.toString();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
									
			/* If from memory - use it as is */
			if(!rsResponse.toLowerCase().contains("<html")) {
				String instNo = "";
				String caseTypeString = "";
				if (CountyConstants.OH_Licking==getDataSite().getCountyId()) {
					Node node = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "div", "id", "caseInfo", true);
					if (node!=null) {
						NodeList list = HtmlParser3.getNodeListByType(node.getChildren(), "h2", true);
						if (list.size()>0) {
							instNo = list.elementAt(0).toPlainTextString();
						}
						Node caseTypeNode = HtmlParser3.getNodeByTypeAndAttribute(node.getChildren(), "table", "id", "caseInfoItems", true);  
						if (caseTypeNode!=null) {
							list = HtmlParser3.getNodeListByType(node.getChildren(), "td", true);
							if (list.size()>1) {
								caseTypeString = correctCaseType(list.elementAt(1).toPlainTextString());
							}
						}
					}
				} else if (CountyConstants.OH_Fairfield==getDataSite().getCountyId()) {
					Node node = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "div", "id", "caseDetailHeader", true);
					if (node!=null) {
						Node instNoNode = HtmlParser3.getNodeByTypeAndAttribute(node.getChildren(), "div", "class", "sectionHeader", true);
						if (instNoNode!=null) {
							instNo = instNoNode.toPlainTextString();
						}
						Node caseTypeNode = HtmlParser3.getNodeByTypeAndAttribute(node.getChildren(), "table", "class", "caseInfoItems", true);  
						if (caseTypeNode!=null) {
							NodeList list = HtmlParser3.getNodeListByType(node.getChildren(), "td", true);
							if (list.size()>1) {
								caseTypeString = correctCaseType(list.elementAt(1).toPlainTextString());
							}
						}
					}
				}
				instrNumber.append(instNo.replaceAll("\\s", ""));
				caseType.append(caseTypeString.trim());
				return rsResponse;
			}
			
			rsResponse = Tidy.tidyParse(rsResponse, null);
			
			htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			nodeList = htmlParser.parse(null);
			
			String instNo = "";
			String caseTypeString = "";
			if (CountyConstants.OH_Licking==getDataSite().getCountyId()) {
				Node node = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "div", "id", "caseInfo", true);
				if (node!=null) {
					NodeList list = HtmlParser3.getNodeListByType(node.getChildren(), "h2", true);
					if (list.size()>0) {
						instNo = list.elementAt(0).toPlainTextString();
					}
					Node caseTypeNode = HtmlParser3.getNodeByTypeAndAttribute(node.getChildren(), "div", "id", "caseInfoItems", true);  
					if (caseTypeNode!=null) {
						list = HtmlParser3.getNodeListByType(node.getChildren(), "div", true);
						if (list.size()>1) {
							Node caseTypeNode2 = list.elementAt(1);
							list = HtmlParser3.getNodeListByType(caseTypeNode2.getChildren(), "span", true);
							if (list.size()>0) {
								caseTypeString = list.elementAt(0).toPlainTextString();
							}
						}
					}
				}
			} else if (CountyConstants.OH_Fairfield==getDataSite().getCountyId()) {
				Node node = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "div", "id", "caseDetailHeader", true);
				if (node!=null) {
					Node instNoNode = HtmlParser3.getNodeByTypeAndAttribute(node.getChildren(), "div", "class", "sectionHeader", true);
					if (instNoNode!=null) {
						instNo = instNoNode.toPlainTextString();
					}
					Node caseTypeNode = HtmlParser3.getNodeByTypeAndAttribute(node.getChildren(), "div", "class", "caseInfoItems", true);  
					if (caseTypeNode!=null) {
						NodeList list = HtmlParser3.getNodeListByType(node.getChildren(), "dd", true);
						if (list.size()>0) {
							caseTypeString = list.elementAt(0).toPlainTextString();
						}
					}
				}
			}
			instrNumber.append(instNo.replaceAll("\\s", ""));
			caseType.append(caseTypeString.trim());
			
			NodeList divs = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "mainContent"));
			
			String details = "";
			
			if (divs.size()>0) {
				details = divs.elementAt(0).toHtml();
				
				details = details.replaceAll("(?is)<div[^>]+class=\"tab-row\"[^>]*>.*?</div>", "");
				details = details.replaceAll("<!--.*?-->", "");
				
				//Case Type, Case Status, Case Judge, ... e.g. Licking 1991 CV 90352
				HtmlParser3 parser = new HtmlParser3(details);
				Node caseInformationNode = HtmlParser3.getNodeByID("caseInfoItems", parser.getNodeList(), true);
				if (caseInformationNode!=null) {
					String caseInformationNodeString = caseInformationNode.toHtml();
					String newCaseInformationNodeString = caseInformationNodeString;
					
					newCaseInformationNodeString = newCaseInformationNodeString.replaceAll("(?is)<div>\\s*<label>([^<]*)</label>\\s*<span>([^<]*)</span>\\s*</div>", "<tr><td>$1</td><td>$2</td></tr>");
					newCaseInformationNodeString = newCaseInformationNodeString.replaceAll("(?is)<div([^>]+id=\"caseInfoItems\"[^>]*>.*?)</div>", "<table$1</table>");
					
					details = details.replaceFirst(Pattern.quote(caseInformationNodeString), Matcher.quoteReplacement(newCaseInformationNodeString));
				}
				
				//Case Type, Case Status, Case Judge, ... e.g. Fairfield 2002 CV 00490
				parser = new HtmlParser3(details);
				caseInformationNode = HtmlParser3.getNodeByTypeAndAttribute(parser.getNodeList(), "div", "class", "caseInfoItems", true);
				if (caseInformationNode!=null) {
					String caseInformationNodeString = caseInformationNode.toHtml();
					String newCaseInformationNodeString = caseInformationNodeString;
					
					newCaseInformationNodeString = newCaseInformationNodeString.replaceAll("(?is)<dt>([^<]*)</dt>\\s*<dd>([^<]*)</dd>", "<tr><td>$1</td><td>$2</td></tr>");
					newCaseInformationNodeString = newCaseInformationNodeString.replaceAll("(?is)<div([^>]+class=\"caseInfoItems\"[^>]*>)\\s*<dl>(.*?)</dl>\\s*</div>", "<table$1$2</table>");
					
					details = details.replaceFirst(Pattern.quote(caseInformationNodeString), Matcher.quoteReplacement(newCaseInformationNodeString));
				}
				
				//Party Information
				parser = new HtmlParser3(details);
				Node partyInformationNode = HtmlParser3.getNodeByID("ptyContainer", parser.getNodeList(), true);
				if (partyInformationNode!=null) {
					String partyInformationString = partyInformationNode.toHtml();
					String newPartyInformationString = partyInformationString;
					
					if (newPartyInformationString.contains("class=\"subSectionHeader2\"")) {	//e.g. Fairfield 2002 CV 00490, 2004 CR 00061
						
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div[^>]+class=\"addrLn[^\"]+\"[^>]*>([^<]*)</div>", "<tr><td>$1</td></tr>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<dt>([^<]*)</dt>\\s*<dd[^>]+class=\"displayData[^>]*>(.*?)(<span>.*?</span>)\\s*</dd>", 
								"<tr><td valign=\"top\">$1</td><td><table id=\"addressTable\">$2<tr><td>$3</td></tr></table></td></tr>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<dt>(\\s*Address|Phone\\s*)</dt>", "<tr><td valign=\"top\">$1</td><td>&nbsp;</td></tr>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div([^>]+class=\"box ptyContact\"[^>]*>)\\s*<dl>(.*?)</dl>\\s*</div>", 
								"<td valign=\"top\"$1<table>$2</table></td>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<dt>([^<]*)</dt>\\s*<dd[^>]*>([^<]*)</dd>", "<tr><td>$1</td><td>$2</td></tr>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<dt>([^<]*)</dt>", "<tr><td>$1</td><td>&nbsp;</td></tr>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div([^>]+class=\"box ptyPersInfo\"[^>]*>)\\s*<dl>(.*?)</dl>\\s*</div>", 
								"<tr><td><table width=\"100%\"><tr><td valign=\"top\"><table$1$2</table></td>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div[^>]+class=\"moreInfo ptyMore\"[^>]*>.*?</div>", "");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div[^>]+style=\"display:none;\"[^>]*>.*?</div>", "");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<span[^>]+class=\"ptyType\"[^>]*>([^<]*)</span>", "$1");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div[^>]+class=\"subSectionHeader2\"[^>]*>\\s*<h5>(.*?)</h5>\\s*</div>", "<tr><td><b>$1</b></td></tr>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<caption>[^<]*</caption>", "");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div[^>]+class=\"box ptyatty\"[^>]*>(.*?)</div>", "<td valign=\"top\">$1</td>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<dt([^>]+class=\"ptyAfflDscr\"[^>]*)>([^<]*)</dt>\\s*<dd([^>]+class=\"ptyAfflName\"[^>]*)>([^<]*)</dd>", 
								"<tr><td$1>$2</td><td$3>$4</td></tr>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div[^>]+class=\"box ptyAffl\"[^>]*>\\s*<h5>([^<]*)</h5>\\s*<dl>(.*?)</dl>\\s*</div>", 
								"<td valign=\"top\"><table><tr><td>$1</td><td>&nbsp;</td></tr>$2</table></td>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div[^>]+class=\"box ptyAffl\"[^>]*>\\s*<h5>([^<]*)</h5>\\s*</div>", 
								"<td valign=\"top\"><table><tr><td>$1</td><td>&nbsp;</td></tr></table></td>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div([^>]+class=\"row)odd(\"[^>]*>)(.*?)</div>", 
								"<tr$1" + "1$2<td><table width=\"100%\">$3</tr></table></td></tr></table></td></tr>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div([^>]+class=\"row)even(\"[^>]*>)(.*?)</div>", 
								"<tr$1" + "2$2<td><table width=\"100%\">$3</tr></table></td></tr></table></td></tr>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div([^>]+id=\"ptyContainer\"[^>]+>.*?)</div>", "<table width=\"100%\"$1</table>");
						
					} else {																	//e.g. Licking 1991 CV 90352
						
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div([^>]+class=\"subItem ptyAttyName\"[^>]*>.*?)</div>", "<td$1</td>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div([^>]+class=\"ptyAttyPhn\"[^>]*>.*?)</div>", "<td$1</td>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div([^>]+class=\"ptyAttySection\"[^>]*>.*?)</div>", "<tr$1</tr>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div>(\\s*)<label([^>]+class=\"ptyAttyName\"[^>]*>.*?)</label>(\\s*)<label([^>]+class=\"ptyAttyPhn\"[^>]*>.*?)</label>(.*?)</div>", 
								"<table>$1<tr><td$2</td>$3<td$4</td></tr>$5</table>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div([^>]+class=\"box ptyAtty\"[^>]*>.*?)</div>", "<td valign=\"top\"$1</td>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div[^>]+class=\"moreInfo ptyMore\"[^>]*>.*?</div>", "");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div([^>]+class=\"subItem ptyAfflDscr\"[^>]*>.*?)</div>", "<td$1</td>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div([^>]+class=\"ptyAfflName\"[^>]*>.*?)</div>", "<td$1</td>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div[^>]+class=\"row(?:odd|even)\"[^>]*>(\\s*<td[^>]+class=\"subItem ptyAfflDscr\"[^>]*>.*?)</div>", 
								"<tr>$1</tr>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div([^>]+class=\"box ptyAffl\"[^>]*>\\s*)<label>([^<]*)</label>(\\s*)<div>(.*?)</div>(\\s*)</div>", 
								"<td valign=\"top\"$1<table><tr><td>$2</td><td>&nbsp;</td></tr>$3$4$5</table></td>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div([^>]+class=\"box ptyAffl\"[^>]*>\\s*)<label>([^<]*)</label>(\\s*)</div>", 
								"<td valign=\"top\"$1<table><tr><td>$2</td></tr>$3</table></td>");
						
						newPartyInformationString = correctDetails(newPartyInformationString, "box ptyContact", "subItem ptyAddr", "subItem ptyAddrsubItem ptyPhn");
						newPartyInformationString = correctDetails(newPartyInformationString, "box ptyPersInfo", "subItem ptyDspDscr", "subItem ptyDspDt");
						
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<h5>\\s*<span>\\s*</span>\\s*</h5>", "");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div>\\s*<h5>([^>]*)<span[^>]*>([^<]*)</span>\\s*</h5>\\s*<div>\\s*<h5>([^<]*)</h5>\\s*</div>\\s*</div>", 
								"<tr><td colspan=\"4\">$1$2$3</td></tr><tr>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div[^>]+style=\"display:none;\"[^>]*>.*?</div>(\\s*</div>\\s*</div>)", "</tr>$1");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div[^>]+class=\"infoInstance ptyInstance\"[^>]*>(.*?)</div>", 
								"<td><table width=\"100%\" border=\"1\" style=\"border-collapse: collapse\">$1</table></td>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div([^>]+class=\"row)odd(\"[^>]*>.*?)</div>", "<tr$1" + "1$2</tr>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div([^>]+class=\"row)even(\"[^>]*>.*?)</div>", "<tr$1" + "2$2</tr>");
						newPartyInformationString = newPartyInformationString.replaceAll("(?is)<div[^>]+id=\"ptyContainer\"[^>]*>(.*?)</div>", "<table width=\"100%\">$1</table>");
						
					}
					
					details = details.replaceFirst(Pattern.quote(partyInformationString), Matcher.quoteReplacement(newPartyInformationString));
				}
				
				//Party Charge Information
				parser = new HtmlParser3(details);
				Node chargeContainerNode = HtmlParser3.getNodeByID("chgContainer", parser.getNodeList(), true);
				if (chargeContainerNode!=null) {
					String chargeContainerString = chargeContainerNode.toHtml();
					String newChargeContainerString = chargeContainerString;
					
					if (newChargeContainerString.contains("class=\"subSectionHeader2\"")) {
						
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div[^>]+class=\"moreInfo dspSentencing\"[^>]*>.*?</div>", "");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<dt>([^<]*)</dt>\\s*<dd[^>]*>([^<]*)</dd>", "<tr><td>$1</td><td>$2</td></tr>");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<dt>([^<]*)</dt>", "<tr><td>$1</td><td>&nbsp;</td></tr>");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div([^>]+class=\"box chgPhase\"[^>]*>)\\s*<dl>(.*?)</dl>\\s*</div>", "<tr><td><table$1$2</table></td></tr>");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div[^>]+class=\"chgData\"[^>]*>(.*?)</div>", "$1");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<span[^>]*>([^<]*)</span>", "$1");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div([^<]+class=\"chgHeadInfo\"[^<]*>)\\s*<div[^<]+class=\"chgLbl\"[^<]*>([^<]*)</div>([^<]*)</div>", 
								"<tr$1<td>$2&nbsp;$3</td></tr>");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div([^<]+class=\"subSectionHeader2\"[^<]*>)\\s*<h5>([^<]*)</h5>(.*?)(<tr.*?)</div>", 
								"<tr$1$2$3</tr>$4");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div([^>]+class=\"row)odd(\"[^>]*>)(.*?)</div>", "<tr$1" + "1$2<td>$3</td></tr>");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div([^>]+class=\"row)even(\"[^>]*>)(.*?)</div>", "<tr$1" + "2$2<td>$3</td></tr>");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div([^>]+id=\"chgContainer\"[^>]*>.*?)</div>", "<table$1</table>");
						
					} else {
						
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div([^>]+class=\"subItem (?:orgChg|indChg|amdChg|domViolRelated)\"[^>]*>\\s*)<label>([^<]*)</label>\\s*<div>([^<]*)</div>\\s*</div>", 
								"<tr$1<td>$2</td><td>$3</td></tr>");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div([^>]+class=\"subItem (?:orgChg|indChg|amdChg|domViolRelated)\"[^>]*>\\s*)<label>([^<]*)</label>\\s*</div>", 
								"<tr$1<td>$2</td><td>&nbsp;</td></tr>");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div([^>]+class=\"divPanel chgPanel\"[^>]*>)\\s*<div[^>]*>\\s*<div[^>]*>\\s*<div[^>]*>(.*?)</div>\\s*</div>\\s*</div>\\s*</div>", 
								"<tr><td><table$1$2</table></td></tr>");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<h6>\\s*</h6>", "");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div>\\s*<h6>(.*?)</h6>\\s*</div>", "<tr><td>$1</td></tr>");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div[^>]+class=\"chgData\"[^>]*>(.*?)</div>", "$1");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<h5>\\s*<span>\\s*<label>([^<]*)</label>([^<]*)</span>([^<]*)<span[^>]*>([^<]*)</span>\\s*</h5>", 
								"<tr><td><b>$1</b>&nbsp;<b>$2$3$4</b></td></tr>");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div[^>]+class=\"moreInfo dspSentencing\"[^>]*>.*?</div>", "");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div([^>]+class=\"infoInstance chgInstance\"[^>]*>.*?)</div>", "<table$1</table>");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div([^>]+class=\"row)odd\"([^>]*>)(.*?)</div>", "<tr$1" + "1$2<td>$3</td></tr>");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div([^>]+class=\"row)even\"([^>]*>)(.*?)</div>", "<tr$1" + "2$2<td>$3</td></tr>");
						newChargeContainerString = newChargeContainerString.replaceAll("(?is)<div[^>]+id=\"chgContainer\"[^>]*>(.*?)</div>", 
								"<table width=\"100%\" border=\"1\" style=\"border-collapse: collapse\">$1</table>");
						
					}
					
					details = details.replaceFirst(Pattern.quote(chargeContainerString), Matcher.quoteReplacement(newChargeContainerString));
				}
				
				//Events
				details = details.replaceFirst("(?is)(<table)([^>]+summary=\"Table displaying the events[^\"]*\"[^>]*>)", "$1 width=\"100%\"$2");
				details = details.replaceAll("(?is)(<th[^>]+)scope=\"col\"([^>]*>)", "$1 align=\"left\"$2");
				
				//Docket Information
				Matcher maDock = Pattern.compile("(?is)<div[^>]+id=\"docketInfo\"[^>]*>.*?(<table[^>]*>.*?</table>)").matcher(details);
				if (maDock.find()) {
					String dockTable = maDock.group(1);
					int start = maDock.start(1);
					int end = maDock.end(1);
					if (start>-1 && end>-1) {
						dockTable = dockTable.replaceFirst("(?is)(<table)", "$1 width=\"100%\"");
						dockTable = dockTable.replaceFirst("(?is)(<tr)", "$1 align=\"left\"");
						dockTable = dockTable.replaceAll("(?is)(<tr[^>]+class=\"row)odd(\"[^>]*>)", "$1" + "1" + "$2");
						dockTable = dockTable.replaceAll("(?is)(<tr[^>]+class=\"row)even(\"[^>]*>)", "$1" + "2" + "$2");
						details = details.substring(0, start) + dockTable + details.substring(end);
					}
				}
				
				//Financial Summary
				details = details.replaceFirst("(?is)(<table)([^>]+summary=\"Table displaying the dockets[^\"]*\"[^>]*>)", "$1 width=\"100%\"$2");
				details = details.replaceAll("(?is)(<th[^>]+)class=\"currency\"([^>]*>)", "$1 align=\"left\"$2");
				
				//Case Disposition
				Matcher maDisp = Pattern.compile("(?is)<table[^>]+summary=\"[^\"]+\\bdispositions?[^\"]*[^>]*>.*?</table>").matcher(details);
				if (maDisp.find()) {
					String dispTable = maDisp.group(0);
					int start = maDisp.start();
					int end = maDisp.end();
					dispTable = dispTable.replaceFirst("(?is)(<table)", "$1 width=\"100%\"");
					dispTable = dispTable.replaceFirst("(?is)(<tr)", "$1 align=\"left\"");
					details = details.substring(0, start) + dispTable + details.substring(end);
				}
				
			}	
			
			return details;
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	public static String correctDetails(String details, String class1, String class2, String class3) {
		HtmlParser3 parser = new HtmlParser3(details);
		NodeList list = HtmlParser3.getNodeListByTypeAndAttribute(parser.getNodeList(), "div", "class", class1, true);
		List<String> listBefore = new ArrayList<String>();
		List<String> listAfter = new ArrayList<String>();
		for (int i=0;i<list.size();i++) {
			String s = list.elementAt(i).toHtml();
			if (!listBefore.contains(s)) {
				listBefore.add(s);
				s = s.replaceAll("(?is)<div([^>]+class=\"(?:" + class2 + "|" + class3 + ")\"[^>]*>\\s*)<label>([^<]*)</label>(\\s*)(<div>\\s*</div>\\s*)?</div>", "<tr$1<td>$2</td><td>&nbsp;</td>$3</tr>");
				s = s.replaceAll("(?is)<div>\\s*</div>", "");
				s = s.replaceAll("(?is)<div>([^<]+)</div>", "<tr><td>$1</td></tr>");
				s = s.replaceFirst("(?is)<span>", "@@@");
				s = s.replaceAll("(?is)</?span>", "");
				s = s.replaceAll("(?is)@@@(.*?)(</div>)", "<tr><td>$1</td></tr>$2");
				s = s.replaceAll("(?is)<div>\\s*<div>(.*?)</div>\\s*</div>", "<table id=\"" + class2 + "\">$1</table>");
				s = s.replaceAll("(?is)<div[^>]+class=\"(?:" + class2 + "|" + class3 + ")\"[^>]*>(\\s*)<label>(.*?)</label>(\\s*)(<table[^>]*>.*?</table>\\s*)</div>", 
						"<tr><td><table>$1<tr><td valign=\"top\">$2</td>$3<td>$4</td></tr></table></td></tr>");
				s = s.replaceAll("(?is)<div([^>]+class=\"subItem ptyPhn\"[^>]*>\\s*)<label>([^<]*)</label>(\\s*)</div>\\s*<div>(.*?)</div>", 
						"<tr$1<td><table><tr><td>$2</td>$3<td>$4</td></tr></table></td></tr>");
				s = s.replaceAll("(?is)<div([^>]+class=\"subItem ptyPhn\"[^>]*>\\s*)<label>([^<]*)</label>(\\s*)</div>", "<tr$1<td><table><tr><td>$2</td>$3<td>&nbsp;</td></tr></table></td></tr>");
				s = s.replaceAll("(?is)</?div[^>]*>", "");
				s = "<td valign=\"top\" class=\"" + class1 + "\"><table>" + s + "</table></td>";
				listAfter.add(s);
			}
		}
		for (int i=0;i<listBefore.size();i++) {
			details = details.replaceFirst(Pattern.quote(listBefore.get(i)), Matcher.quoteReplacement(listAfter.get(i)));
		}
		return details;
	}
		
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		int offset = 0;								//0 for Licking 
		if (CountyConstants.OH_Fairfield==getDataSite().getCountyId()) {
			offset = 1;								//1 for Fairfield
		}
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		int numberOfUncheckedElements = 0;
		
		try {
			TableTag resultsTable = null;
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("summary", "Search Results Grid"), true);
			
			if (mainTableList.size()>0) {
				resultsTable = (TableTag)mainTableList.elementAt(0);
			}
			
			if (resultsTable!=null && resultsTable.getRowCount()>1) {
				
				//group the rows which have the same Case Number
				LinkedHashMap<String, List<TableRow>> rowsMap = new  LinkedHashMap<String, List<TableRow>>();
				
				TableRow[] rows  = resultsTable.getRows();
				
				int len = rows.length;
				for (int i=1;i<len;i++) {
					
					TableRow row = rows[i];
					
					if (row.getColumnCount()>6+offset) {
						String key = row.getColumns()[0].toPlainTextString().trim();
						if (rowsMap.containsKey(key)) {		//row already added
							List<TableRow> value = rowsMap.get(key);
							value.add(row);
						} else {							//add row
							List<TableRow> value = new ArrayList<TableRow>();
							value.add(row);
							rowsMap.put(key, value);
						}
					}
					
				}
				
				int i=1;
				Iterator<Entry<String, List<TableRow>>> it = rowsMap.entrySet().iterator();
				while (it.hasNext()) {
				
					Map.Entry<String, List<TableRow>> pair = (Map.Entry<String, List<TableRow>>)it.next();
					List<TableRow> value = pair.getValue();
					
					StringBuilder sb = new StringBuilder();
					
					int noFirstColumns = 3 + offset;
					
					int idx = 0;
					String detailsLink = CreatePartialLink(TSConnectionURL.idGET) + getBaseLink();
					String caseNumberColumn = value.get(0).getColumns()[idx++].toHtml();
					Matcher ma = Pattern.compile("(?is)<a[^>]+href=\"([^\"]+)\"[^>]*>").matcher(caseNumberColumn);
					if (ma.find()) {
						 detailsLink += ma.group(1);
					}
					caseNumberColumn = caseNumberColumn.replaceFirst("(?is)<a[^>]+href=\"[^\"]+\"[^>]*>", "<a href=\"" + detailsLink + "\">");
					sb.append(caseNumberColumn);
					
					//Case Type, File Date, Initiating Action (only for Licking)
					while (idx<noFirstColumns) {							
						sb.append(value.get(0).getColumns()[idx++].toHtml().replaceAll("(?is)<a[^>]+>\\s*(?:<span>)?([^<]+)(?:</span>)?\\s*</a>", "$1"));	
					}
					
					//Party/Company
					StringBuilder sb1 = new StringBuilder();
					sb1.append("<td><table>");
					for (int j=0;j<value.size();j++) {
						sb1.append("<tr>");
						sb1.append(value.get(j).getColumns()[idx].toHtml().replaceAll("(?is)<a[^>]+>\\s*(?:<span>)?([^<]+)(?:</span>)?\\s*</a>", "$1"));
						sb1.append("</tr>");
					}
					sb1.append("</table></td>");
					sb.append(sb1);
					idx++;
					
					//Party Type
					StringBuilder sb2 = new StringBuilder();
					sb2.append("<td><table>");
					for (int j=0;j<value.size();j++) {
						sb2.append("<tr>");
						sb2.append(value.get(j).getColumns()[idx].toHtml().replaceAll("(?is)<a[^>]+>\\s*(?:<span>)?([^<]+)(?:</span>)?\\s*</a>", "$1"));
						sb2.append("</tr>");
					}
					sb2.append("</table></td>");
					sb.append(sb2);
					idx++;
					
					//Date of Birth
					StringBuilder sb3 = new StringBuilder();
					sb3.append("<td><table>");
					for (int j=0;j<value.size();j++) {
						sb3.append("<tr>");
						sb3.append(value.get(j).getColumns()[idx].toHtml().replaceAll("(?is)<a[^>]+>\\s*(?:<span>)?([^<]+)(?:</span>)?\\s*</a>", "$1"));
						sb3.append("</tr>");
					}
					sb3.append("</table></td>");
					sb.append(sb3);
					idx++;
					
					//Case Status
					sb.append(value.get(0).getColumns()[idx].toHtml().replaceAll("(?is)<a[^>]+>\\s*(?:<span>)?([^<]+)(?:</span>)?\\s*</a>", "$1"));
					
					String htmlRow = sb.toString();
					htmlRow = htmlRow.replaceAll("(?is)<td[^>]+>", "<td>");
					
					ResultMap m = ro.cst.tsearch.servers.functions.OHGenericCourtViewCO.parseIntermediaryRow(htmlRow, offset);
					
					String rowType = "1";
					if (i%2==0) {
						rowType = "2";
					}
						
					ParsedResponse currentResponse = new ParsedResponse();
						
					currentResponse.setPageLink(new LinkInPage(detailsLink, detailsLink, TSServer.REQUEST_SAVE_TO_TSD));
					
					String instrNo = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()));
					String type = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName()));
					String date = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.RECORDED_DATE.getKeyName()));
					String year = "";
					int index = date.lastIndexOf("/");
					if (index>-1 && index<date.length()-1) {
						year = date.substring(index+1);
					}
					
					String checkBox = "checked";
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("instrno", instrNo);
					data.put("type", type);
					data.put("year", year);
					
					if (isInstrumentSaved(instrNo, null, data, false)) {
						checkBox = "saved";
					} else {
						numberOfUncheckedElements++;
						LinkInPage linkInPage = new LinkInPage(detailsLink, detailsLink, TSServer.REQUEST_SAVE_TO_TSD);
						checkBox = "<input type='checkbox' name='docLink' value='" + detailsLink + "'>";
						currentResponse.setPageLink(linkInPage);
					}
					String rowHtml = "<tr class=\"row" + rowType + "\"><td align=\"center\">" + checkBox + "</td>" + htmlRow + "</tr>";
										
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					currentResponse.setOnlyResponse(rowHtml);
					 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
						
					DocumentI document = (RegisterDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
					
					i++;
					
				}	
				
				String headerRow =  "<tr bgcolor=\"#7DA7D9\">" +
									"<td align=\"center\">" + SELECT_ALL_CHECKBOXES + "</td>" +
									"<td><b>Case Number</b></td>" +
									"<td><b>Case Type</b></td>" +
									"<td><b>File Date</b></td>";
				if (offset==1) {
					headerRow +=	"<td><b>Initiating Action</b></td>";
				}
				headerRow +=		"<td><b>Party/Company</b></td>" +
									"<td><b>Party Type</b></td>" +
									"<td><b>Date of Birth</b></td>" +
									"<td><b>Case Status</b></td></tr>";
				
				String prevNextString = "";
				NodeList prevNextList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "navigator"), true);
				if (prevNextList.size()>0) {
					prevNextString = prevNextList.elementAt(0).toHtml();
					String prevNextLink = CreatePartialLink(TSConnectionURL.idGET) + getBaseLink();
					prevNextString = prevNextString.replaceAll("(?is)<a[^>]+href=\"([^\"]+)\"[^>]*>", "<a href=\"" +  prevNextLink + "$1\">");
					prevNextString = "<tr><td align=\"right\">" + prevNextString + "</td></tr>";
				}
				
				String header = "<table width=\"100%\"><tr><td><table width=\"100%\">" + headerRow;
				String footer = "</table></td></tr>" + prevNextString  + "</table>";
				
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter(footer);
				
				Matcher ma = Pattern.compile("(?is)<a[^>]+href=\"[^\"]+\"[^>]*>\\s*&gt;\\s*</a>").matcher(prevNextString);
				if (ma.find()) {
					response.getParsedResponse().setNextLink(ma.group(0));
				}
				
				outputTable.append(table);
				SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			}

		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}

		return intermediaryResponse;
	}
	
	public static String correctCaseType(String caseType) {
		if (caseType.equalsIgnoreCase("Miscellaneous")) {
			caseType+= "Court";
		}
		return caseType;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,	String detailsHtml, ResultMap resultMap) {
				
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "CO");
			
			String instNo = "";
			String caseType = "";
			String recordedDate = "";
			String judgeName = "";
			List<String> partyCompanyListLF = new ArrayList<String>();
			List<String> partyTypeListLF = new ArrayList<String>();
			List<String> partyCompanyListFL = new ArrayList<String>();
			List<String> partyTypeListFL = new ArrayList<String>();
			
			if (CountyConstants.OH_Licking==getDataSite().getCountyId()) {
				Node node = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "div", "id", "caseInfo", true);
				if (node!=null) {
					NodeList list = HtmlParser3.getNodeListByType(node.getChildren(), "h2", true);
					if (list.size()>0) {
						instNo = list.elementAt(0).toPlainTextString();
					}
					Node caseTypeNode = HtmlParser3.getNodeByTypeAndAttribute(node.getChildren(), "table", "id", "caseInfoItems", true);  
					if (caseTypeNode!=null) {
						caseType = correctCaseType(HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(caseTypeNode.getChildren(), "Case Type:"), "", true).trim());
						recordedDate = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(caseTypeNode.getChildren(), "File Date:"), "", true).trim();
						judgeName = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(caseTypeNode.getChildren(), "Case Judge:"), "", true).trim();
					}
				}
				Node partyInformationNode = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "div", "id", "ptyInfo", true);
				if (partyInformationNode!=null) {
					Node partyTableNode1 = HtmlParser3.getNodeByTypeAndAttribute(partyInformationNode.getChildren(), "table", "width", "100%", true);
					if (partyTableNode1!=null) {
						if (partyTableNode1 instanceof TableTag) {
							TableTag partyTable1 = (TableTag)partyTableNode1;
							for (int i=0;i<partyTable1.getRowCount();i++) {
								TableRow row1 = partyTable1.getRow(i);
								if (row1.getColumnCount()>0) {
									TableColumn col1 = row1.getColumns()[0];
									Node partyTableNode2 = HtmlParser3.getNodeByTypeAndAttribute(col1.getChildren(), "table", "width", "100%", true);
									if (partyTableNode2!=null) {
										if (partyTableNode2 instanceof TableTag) {
											TableTag partyTable2 = (TableTag)partyTableNode2;
											if (partyTable2.getRowCount()>0) {
												TableRow row2 = partyTable2.getRow(0);
												if (row2.getColumnCount()>0) {
													String s = row2.getColumns()[0].toPlainTextString().trim();
													int index = s.lastIndexOf("-");
													if (index>-1 && index<s.length()-1) {
														String name = s.substring(0, index).trim();
														String type = s.substring(index+1).trim();
														partyCompanyListLF.add(name);
														partyTypeListLF.add(type);
														Node partyTableNode3 = HtmlParser3.getNodeByTypeAndAttribute(partyTable2.getChildren(), "table", "id", "subItem ptyAddr", true);
														if (partyTableNode3!=null) {
															if (partyTableNode3 instanceof TableTag) {
																TableTag partyTable3 = (TableTag)partyTableNode3;
																if (partyTable3.getRowCount()>0) {
																	TableRow row3 = partyTable3.getRow(0);
																	if (row3.getColumnCount()>0) {
																		//first line from Address
																		name = row3.getColumns()[0].toPlainTextString().trim();
																		if (name.matches("(?is)^(C/O|%)\\s+.*+")) {
																			partyCompanyListFL.add(name.replaceFirst("(?is)^(C/O|%)\\s+", ""));
																			partyTypeListFL.add(type);
																		}
																	}
																}
															}
														}
														//Alias
														NodeList list = HtmlParser3.getNodeListByTypeAndAttribute(partyTable2.getChildren(), "td", "class", "ptyAfflName", true);
														for (int j=0;j<list.size();j++) {
															name = list.elementAt(j).toPlainTextString().trim();
															if (!partyCompanyListLF.contains(name)) {
																partyCompanyListLF.add(name);
																partyTypeListLF.add(type);
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			} else if (CountyConstants.OH_Fairfield==getDataSite().getCountyId()) {
				Node node = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "div", "id", "caseDetailHeader", true);
				if (node!=null) {
					Node instNoNode = HtmlParser3.getNodeByTypeAndAttribute(node.getChildren(), "div", "class", "sectionHeader", true);
					if (instNoNode!=null) {
						instNo = instNoNode.toPlainTextString();
					}
					Node caseTypeNode = HtmlParser3.getNodeByTypeAndAttribute(node.getChildren(), "table", "class", "caseInfoItems", true);  
					if (caseTypeNode!=null) {
						caseType = correctCaseType(HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(caseTypeNode.getChildren(), "Case Type:"), "", true).trim());
						recordedDate = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(caseTypeNode.getChildren(), "File Date:"), "", true).trim();
						judgeName = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(caseTypeNode.getChildren(), "Case Judge:"), "", true).trim();
					}
				}
				Node partyInformationNode = HtmlParser3.getNodeByTypeAndAttribute(nodeList, "div", "id", "ptyInfo", true);
				if (partyInformationNode!=null) {
					Node partyTableNode1 = HtmlParser3.getNodeByTypeAndAttribute(partyInformationNode.getChildren(), "table", "id", "ptyContainer", true);
					if (partyTableNode1!=null) {
						if (partyTableNode1 instanceof TableTag) {
							TableTag partyTable1 = (TableTag)partyTableNode1;
							for (int i=0;i<partyTable1.getRowCount();i++) {
								TableRow row1 = partyTable1.getRow(i);
								if (row1.getColumnCount()>0) {
									TableColumn col1 = row1.getColumns()[0];
									Node partyTableNode2 = HtmlParser3.getNodeByTypeAndAttribute(col1.getChildren(), "table", "width", "100%", true);
									if (partyTableNode2!=null) {
										if (partyTableNode2 instanceof TableTag) {
											TableTag partyTable2 = (TableTag)partyTableNode2;
											if (partyTable2.getRowCount()>0) {
												TableRow row2 = partyTable2.getRow(0);
												if (row2.getColumnCount()>0) {
													String s = row2.getColumns()[0].toPlainTextString().trim();
													int index = s.lastIndexOf("-");
													if (index>-1 && index<s.length()-1) {
														String name = s.substring(0, index).trim();
														String type = s.substring(index+1).trim();
														partyCompanyListLF.add(name);
														partyTypeListLF.add(type);
														Node partyTableNode3 = HtmlParser3.getNodeByTypeAndAttribute(partyTable2.getChildren(), "table", "id", "addressTable", true);
														if (partyTableNode3!=null) {
															if (partyTableNode3 instanceof TableTag) {
																TableTag partyTable3 = (TableTag)partyTableNode3;
																if (partyTable3.getRowCount()>0) {
																	TableRow row3 = partyTable3.getRow(0);
																	if (row3.getColumnCount()>0) {
																		//first line from Address
																		name = row3.getColumns()[0].toPlainTextString().trim();
																		if (name.matches("(?is)^(C/O|%)\\s+.*+")) {
																			partyCompanyListFL.add(name.replaceFirst("(?is)^(C/O|%)\\s+", ""));
																			partyTypeListFL.add(type);
																		}
																	}
																}
															}
														}
														//Alias
														NodeList list = HtmlParser3.getNodeListByTypeAndAttribute(partyTable2.getChildren(), "td", "class", "ptyAfflName", true);
														for (int j=0;j<list.size();j++) {
															name = list.elementAt(j).toPlainTextString().trim();
															if (!partyCompanyListLF.contains(name)) {
																partyCompanyListLF.add(name);
																partyTypeListLF.add(type);
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instNo.replaceAll("\\s", ""));
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), caseType.trim());
			resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate.trim());
			resultMap.put(CourtDocumentIdentificationSetKey.JUDGE_NAME.getKeyName(), judgeName.trim());
			
			putNames(partyCompanyListLF, partyTypeListLF, "tmpGrantorLF", "tmpGranteeLF", resultMap);
			putNames(partyCompanyListFL, partyTypeListFL, "tmpGrantorFL", "tmpGranteeFL", resultMap);
			
			ro.cst.tsearch.servers.functions.OHGenericCourtViewCO.parseNames(resultMap);
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	public static void putNames(List<String> partyCompanyList, List<String> partyTypeList, String grantorId, String granteeId, ResultMap resultMap) {
		
		StringBuilder grantor = new StringBuilder();
		StringBuilder grantee = new StringBuilder();
		
		for (int i=0;i<partyCompanyList.size();i++) {
			String partyCompany = partyCompanyList.get(i);
			if (i<partyTypeList.size()) {
				String partyType = partyTypeList.get(i);
				if (partyType.matches("(?is).*\\b(PLAINTIFF|PETITIONER|APPELLANT|CREDITOR)\\b.*")) {
					grantor.append(partyCompany).append(" / ");
				} else if (partyType.matches("(?is).*\\b(DEFENDANT|RESPONDENT|APPELLEE|DEBTOR)\\b.*")) {
					grantee.append(partyCompany).append(" / ");
				}
			}
		}
		
		String grantorString = grantor.toString().replaceFirst(" / $", "");
		String granteeString = grantee.toString().replaceFirst(" / $", "");
		
		if (!StringUtils.isEmpty(grantorString)) {
			resultMap.put(grantorId, grantorString);
		}
		if (!StringUtils.isEmpty(granteeString)) {
			resultMap.put(granteeId, granteeString);
		}
		
	}
		
	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		ConfigurableNameIterator nameIterator = null;
		String endDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		for (String id : gbm.getGbTransfers()) {

			//person
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantor");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				
				String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
				if (date != null) {
					module.getFunction(17).forceValue(date);
				}
				module.setValue(18, endDate);
				module.forceValue(21, "2");
				
				GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
				defaultNameFilter.setIgnoreMiddleOnEmpty(true);
				defaultNameFilter.setUseArrangements(false);
				module.addFilter(defaultNameFilter);
				
				module.addFilterForNext(new NameFilterForNext(searchId));
				
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] {"L;F;" });
				nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
				nameIterator.clearSearchedNames();
				nameIterator.setInitAgain(true);
				module.addIterator(nameIterator);
				
				modules.add(module);
			}
			
			//company
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantor");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				
				String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
				if (date != null) {
					module.getFunction(17).forceValue(date);
				}
				module.setValue(18, endDate);
				module.forceValue(21, "2");
				
				GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
				module.addFilter(defaultNameFilter);
				
				module.addFilterForNext(new NameFilterForNext(searchId));
				
				module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
				
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,	new String[] {"L;F;" });
				nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
				nameIterator.clearSearchedNames();
				nameIterator.setInitAgain(true);
				module.addIterator(nameIterator);
				
				modules.add(module);
			}
			
			

			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
				
				//person
				{
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
					module.setIndexInGB(id);
					module.setTypeSearchGB("grantee");
					module.clearSaKeys();
					module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
					
					String date = gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
					if (date != null) {
						module.getFunction(17).forceValue(date);
					}
					module.setValue(18, endDate);
					module.forceValue(21, "2");
					
					GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
					defaultNameFilter.setIgnoreMiddleOnEmpty(true);
					defaultNameFilter.setUseArrangements(false);
					module.addFilter(defaultNameFilter);
					
					module.addFilterForNext(new NameFilterForNext(searchId));
					
					module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
					module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
					
					nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,	new String[] {"L;F;" });
					nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
					nameIterator.clearSearchedNames();
					nameIterator.setInitAgain(true);
					module.addIterator(nameIterator);
					
					modules.add(module);
				}
				
				//company
				{
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
					module.setIndexInGB(id);
					module.setTypeSearchGB("grantee");
					module.clearSaKeys();
					module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
					
					String date = gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
					if (date != null) {
						module.getFunction(17).forceValue(date);
					}
					module.setValue(18, endDate);
					module.forceValue(21, "2");
					
					module.addFilterForNext(new NameFilterForNext(searchId));
					
					module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
					
					GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
					module.addFilter(defaultNameFilter);
					
					nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,	new String[] {"L;F;" });
					nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
					nameIterator.clearSearchedNames();
					nameIterator.setInitAgain(true);
					module.addIterator(nameIterator);
					
					modules.add(module);
				}
				
			}
		}
		serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}
			
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
				
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		//person
		{
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, true, new String[]{"L;F;"});
			iterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
			iterator.setInitAgain(true);
			module.addIterator(iterator);
			
			module.addFilterForNext(new NameFilterForNext(searchId));
			GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			module.addFilter(nameFilter);
			addBetweenDateTest(module, true, true, true);
			module.addValidator(nameFilter.getValidator());
			
			module.forceValue(21, "2");
						
			modules.add(module);
		}
		//company
		{
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
			ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, true, new String[]{"L;F;"});
			iterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
			iterator.setInitAgain(true);
			module.addIterator(iterator);
			
			module.addFilterForNext(new NameFilterForNext(searchId));
			GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			module.addFilter(nameFilter);
			addBetweenDateTest(module, true, true, true);
			module.addValidator(nameFilter.getValidator());
			
			module.forceValue(21, "2");
						
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}
	
}
