package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.w3c.dom.Node;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.MultiplePinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.functions.ILMcHenryTR;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 *  ADD here the new county implemented with this Generic
 *  
 * Generic class ILMcHenryTR, ILKendallTR.
 * 
 * @author mihaib
  */

public class ILGenericTR extends TSServerAssessorandTaxLike {

	public static final long serialVersionUID = 10000000L;
	private static HashMap<String, Integer> taxYears = new HashMap<String, Integer>();
	private boolean downloadingForSave; 

	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	private static final Pattern pinPattern = Pattern.compile("(?is)id=\\\"ctl00_lblParcelNumber\\\"[^>]+>\\s*([^<]+)");	
	private static final Pattern optionYears = Pattern.compile("(?is)<option\\s+value[^>]+>(\\d+)");
	
	private static final Pattern firstPattern = Pattern.compile("(?i)<input.*?name\\s*=\\s*\\\"\\s*([^\\\"]+)\\\".*?First[^/]+/>");
	private static final Pattern prevPattern = Pattern.compile("(?i)<input.*?name\\s*=\\s*\\\"\\s*([^\\\"]+)\\\".*?Previous[^/]+/>");
	private static final Pattern nextPattern = Pattern.compile("(?i)<input.*?name\\s*=\\s*\\\"\\s*([^\\\"]+)\\\".*?Next[^/]+/>");
	private static final Pattern lastPattern = Pattern.compile("(?i)<input.*?name\\s*=\\s*\\\"\\s*([^\\\"]+)\\\".*?btnLast[^/]+/>");
	
	private static final Pattern currPage = Pattern.compile("(?is)ddlPages\">.*<option.*selected.*?value[^>]+>([^<]+)</option>");
	private static final Pattern numberOfPages = Pattern.compile("(?is)<span id\\s*=\\s*\\\"ctl00_ContentPlaceHolder1_SearchResultsSum1_gvSearchResults_ctl\\d+_lblPageCount\\\"\\s*>\\s*<b>([^<]+)");
	
	//these needs for next/prev links, these params are on bottom on the page
	private static final Pattern param_sortExpression = Pattern.compile("(?is)__gvctl00_ContentPlaceHolder1_SearchResultsSum1_gvSearchResults\\.sortExpression\\s*=\\s*\\\"\\s*([^\\\"]+)\\\"");
	private static final Pattern param_pageIndex = Pattern.compile("(?is)__gvctl00_ContentPlaceHolder1_SearchResultsSum1_gvSearchResults\\.pageIndex\\s*=\\s*([^;]+)");
	private static final Pattern param_sortDirection = Pattern.compile("(?is)__gvctl00_ContentPlaceHolder1_SearchResultsSum1_gvSearchResults\\.sortDirection\\s*=\\s*([^;]+)");
	
	private static final Pattern FOR_ETARG_PAT = Pattern.compile("(?is)<a\\s+href\\s*=\\s*\\\"javascript:__doPostBack\\('([^']+)','([^\\']+)'\\)\\\">");
	
	private static final String FORM_NAME = "aspnetForm";
	public ILGenericTR(long searchId) {
		super(searchId);
	}

	public ILGenericTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		
		CurrentInstance instance = InstanceManager.getManager().getCurrentInstance(searchId);
		String county = instance.getCurrentCounty().getName();
		Search global = instance.getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
			String streetName = getSearchAttribute(SearchAttributes.P_STREETNAME);
			String streetNo = getSearchAttribute(SearchAttributes.P_STREETNO);
			String pid = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			
			String yearSearchTo = getSearchAttribute(SearchAttributes.CURRENT_TAX_YEAR);
			int lastYear = 2011;
			Integer lastYearFromMap = taxYears.get("lastTaxYear" + county);
			if (lastYearFromMap!=null) {
				lastYear = lastYearFromMap.intValue();
			}
			if (Integer.parseInt(yearSearchTo) > lastYear) {
				yearSearchTo = Integer.toString(lastYear);
			}
			
			String yearSearchFrom = yearSearchTo;
			if (StringUtils.isNotEmpty(yearSearchFrom) && numberOfYearsAllowed > 1){
				int intYearSearchFrom = Integer.parseInt(yearSearchFrom);
				yearSearchFrom = Integer.toString(intYearSearchFrom - (numberOfYearsAllowed - 1));
			}
			
			FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId, true, numberOfYearsAllowed, true);
			FilterResponse nameFilterHybridDoNotSkipUnique = null;
			FilterResponse multiplePINFilter = new MultiplePinFilterResponse(searchId);
			
			Collection<String> pins = getSearchAttributes().getPins(-1);
			if(pins.size() > 1){			
				for(String pin: pins){
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
					module.clearSaKeys();
					module.setData(0, "");
					module.setData(1, "");
					module.setData(2, "");
					module.getFunction(12).forceValue(pin);
					
					module.setData(10, yearSearchFrom);
					module.setData(11, yearSearchTo);
					module.setIteratorType(10, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
					module.setIteratorType(11, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
					module.setMutipleYears(true);
					module.addFilter(taxYearFilter);
					
					modules.add(module);	
				}			
				// set list for automatic search 
				serverInfo.setModulesForAutoSearch(modules);
				resultType = MULTIPLE_RESULT_TYPE;
				return;
			}
			
			if (hasPin()){
				//Search by PrcelNumber
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.setData( 0, "");
				module.setData( 1, "");
				module.setData( 2, "");
				module.setData( 12, pid);
				module.forceValue(10, yearSearchFrom);
				module.forceValue(11, yearSearchTo);
				
				module.setMutipleYears(true);
				
				module.setIteratorType(ModuleStatesIterator.TYPE_PARCEL_ID_FAKE);
				module.addFilter(taxYearFilter);
					
				modules.add(module);
			}
			
			if (hasStreet()) {
				//Search by Property Address
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				module.clearSaKeys();
				module.setData( 0, streetNo);
				module.setData( 3, streetName);
				module.setData(12, yearSearchFrom);
				module.setData(13, yearSearchTo);
				
				module.setIteratorType(12, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
				module.setIteratorType(13, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
				module.setMutipleYears(true);
				
				module.setIteratorType(ModuleStatesIterator.TYPE_ADDRESS);
				module.addFilter(taxYearFilter);
				module.addFilter(multiplePINFilter);
				
				
				modules.add(module);
			}
			
			if(hasOwner())
	        {//search by Owner
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
						SearchAttributes.OWNER_OBJECT , searchId , module);
				nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
				
				module.setData(10, yearSearchFrom);
				module.setData(11, yearSearchTo);
				
				module.setIteratorType(10, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
				module.setIteratorType(11, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
				module.setMutipleYears(true);
				
				module.addFilter(nameFilterHybridDoNotSkipUnique);
				module.addFilter(taxYearFilter);
				
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
															.getConfigurableNameIterator(module, searchId, new String[] {"L;F;", "L;f;"});
				module.addIterator(nameIterator);
				modules.add(module);
	        }
		}
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {	
		String response = Response.getResult();
		
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch(viParseID){
		
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
			
			// pin not found
			if(response.matches("(?is).*ErrorPage.aspx.*")){
				Response.getParsedResponse().setError("<font color=\"red\">There was an error on the Official Site or your connection expired.</font>  Please try again.");
				return;
			}
			
			if(!response.matches("(?is).*Year\\s*/\\s*Parcel.*")){
				Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
				return;
			}
			
			Node tbl = HtmlParserTidy.getNodeById(response, "ctl00_ContentPlaceHolder1_SearchResultsSum1_gvSearchResults", "table");
			String html = "";
			try {
				html = HtmlParserTidy.getHtmlFromNode(tbl);
			} catch (TransformerException e) {
				e.printStackTrace();
			}
			StringBuilder outputTable = new StringBuilder();
			try {
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, html, outputTable);
				
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
			
			String details = getDetails(response);
			
			// isolate pin number
			String keyCode = "File";
			Matcher pinMatcher = pinPattern.matcher(details);
			if(pinMatcher.find())
			{
				keyCode = pinMatcher.group(1).trim();
				//keyCode = keyCode.replaceAll("-","");
			} 
			String year = "";
			try {
				String detailsForParser = details.replaceAll("(?is)</?span[^>]*>", "");
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsForParser, null);
				NodeList mainList = htmlParser.parse(null);
				year = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Tax Year:"), "", true).replaceAll("(?is)&nbsp;", "").trim();
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			if ((!downloadingForSave)){
                
                String qry_aux = Response.getRawQuerry();
				qry_aux = "dummy=" + keyCode + "&" + qry_aux;
				String originalLink = sAction + "&" + qry_aux;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","CNTYTAX");
				data.put("datasource","TR");
				data.put("year", year);
				
				if(isInstrumentSaved(keyCode, null, data)){
                	details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
				}

				Response.getParsedResponse().setPageLink(
					new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);
 
            } else {            
				smartParseDetails(Response, details);
                msSaveToTSDFileName = keyCode + ".html";
                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}
			break;
		
		case ID_GET_LINK :
			if (response.contains("ctl00_contentBody_pnlParcelDetail"))
				ParseResponse(sAction, Response, ID_DETAILS);
			else if (response.contains("ctl00_ContentPlaceHolder1_SearchResultsSum1_gvSearchResults"))
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
		break;
		case ID_SAVE_TO_TSD:
			
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;				
			break;	
		default:
			break;
		}
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			
			String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName().toLowerCase();
			String html = response.getResult();
			Form form = new SimpleHtmlParser(response.getResult()).getForm("aspnetForm");
			Map<String, String> params = new SimpleHtmlParser(response.getResult()).getForm("aspnetForm").getParams();
				
			table = table.replaceAll("&amp;", "&");
				
			Map<String, String> paramsForDetails = new LinkedHashMap<String, String>();
			Map<String, String> paramsForNavigation = new LinkedHashMap<String, String>();
				
			Matcher pageIndexMat = param_pageIndex.matcher(html);
			Matcher sortDirectionMat = param_sortDirection.matcher(html);
			Matcher sortExpressionMat = param_sortExpression.matcher(html);
			String weirdExtraLongParam = "";
			if (pageIndexMat.find() && sortDirectionMat.find() && sortExpressionMat.find()) {
				weirdExtraLongParam = pageIndexMat.group(1) + "|" + sortDirectionMat.group(1) + "|" + sortExpressionMat.group(1) + "|";
				paramsForDetails.put("__gvctl00_ContentPlaceHolder1_SearchResultsSum1_gvSearchResults__hidden", weirdExtraLongParam);
			}
			paramsForDetails.put("ctl00$ContentPlaceHolder1$ddlYearFrom", params.get("ctl00$ContentPlaceHolder1$ddlYearFrom"));
			paramsForDetails.put("ctl00$ContentPlaceHolder1$ddlYearTo", params.get("ctl00$ContentPlaceHolder1$ddlYearTo"));
			paramsForDetails.put("ctl00$ContentPlaceHolder1$txtParcelNoFrom", params.get("ctl00$ContentPlaceHolder1$txtParcelNoFrom"));
			paramsForDetails.put("ctl00$ContentPlaceHolder1$txtParcelNoTo", params.get("ctl00$ContentPlaceHolder1$txtParcelNoTo"));
			paramsForDetails.put("ctl00$ContentPlaceHolder1$chkActiveParcels", "on");
			paramsForDetails.put("ctl00$ContentPlaceHolder1$hidMenuItem", "8");
			paramsForDetails.put("__VIEWSTATE", params.get("__VIEWSTATE"));
			paramsForDetails.put("__EVENTVALIDATION", params.get("__EVENTVALIDATION"));
			paramsForNavigation.putAll(paramsForDetails);
			    
			Matcher eTargetMat = FOR_ETARG_PAT.matcher(html);
			if (eTargetMat.find()){
				paramsForDetails.put("__EVENTTARGET", eTargetMat.group(1));
			}			    
				
			table = table.replaceAll("(?is)<a\\s+href\\s*=\\s*\\\"javascript:__doPostBack\\('([^']+)','([^\\']+)'\\)\\\">",
						"<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/forms/search.aspx?select=$2\">");
			
			//one results on two rows
			table = table.replaceAll("(?is)(<tr[^>]*>\\s*<td[^>]*>\\s*<a[^>]+>[\\d-\\s]+</a>\\s*</td>\\s*<td[^>]*>[^<]+)(</td>\\s*<td[^>]*>[^<]*)</td>\\s*</tr>\\s*<tr[^>]*>\\s*<td[^>]*>\\s*<a[^>]+>\\s*</a>\\s*</td>\\s*<td[^>]+>([^<]*)</td>\\s*<td[^>]*>([^<]*)</td>\\s*</tr>",
					"$1 /<br> $3 $2 /<br> $4</td></tr>");
					
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList mainTableList = htmlParser.parse(null);
			NodeList tableList = mainTableList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (tableList.size() > 0){
				TableTag mainTable = (TableTag)tableList.elementAt(0);
				TableRow[] rows = mainTable.getRows();
					
				for(TableRow row : rows ) {
					if(row.getColumnCount() > 1) {
						
						TableColumn[] cols = row.getColumns();
						NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						if (aList.size() > 0){
							String link = ((LinkTag) aList.elementAt(0)).extractLink();
							String parcel = "", name = "";
							try {
								parcel = aList.elementAt(0).getChildren().elementAt(0).toHtml();
								link += "&Parcel=" + parcel.replaceAll("\\s+", "");
							} catch (Exception e) {	}
							try {
								name =  cols[1].getStringText();
							} catch (Exception e) {
								e.printStackTrace();
							}
							String rowHtml =  row.toHtml().replace("$", "\\$");		//to avoid taking $ as group reference
							rowHtml = rowHtml.replaceAll("(?is)href=\\\"([^\\\"]+)", "href=\"" + link.replace("$", "\\$"));
								
							ParsedResponse currentResponse = new ParsedResponse();
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
							currentResponse.setOnlyResponse(rowHtml);
							currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
							
							ResultMap resultMap = new ResultMap();
							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
							
							if ("kendall".equals(crtCounty.toLowerCase())){
								resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), parcel.replaceAll("(?is)\\A(\\d{4})\\s+.*", "$1").trim());
								resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel.replaceAll("(?is)\\A(\\d{4})\\s+(.*?)", "$2").trim());
								ILMcHenryTR.partyNamesInterILKendallTR(resultMap, name);
							} if ("mchenry".equals(crtCounty.toLowerCase())){
								resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), parcel.replaceAll("(?is)\\A(\\d{4})\\s+.*", "$1").trim());
								resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcel.replaceAll("(?is)\\A(\\d{4})\\s+(.*?)", "$2").trim());
								ILMcHenryTR.partyNamesInterILMcHenryTR(resultMap, name);
							}
							resultMap.removeTempDef();
							Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
							
							DocumentI document = (TaxDocumentI)bridge.importData();
							
							currentResponse.setDocument(document);
							
							intermediaryResponse.add(currentResponse);
						}
					}
				}
				mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsDetails:", paramsForDetails);
					
				String ctlFirst = null, ctlPrev = null, ctlNext = null, ctlLast = null;
				String linkFirst = null, linkPrev = null, linkNext = null, linkLast = null;
					
				for(Map.Entry<String, String> entry: params.entrySet()){
					if (entry.getKey().contains("ddlPages")){
						paramsForNavigation.put(entry.getKey(), entry.getValue());
					} else if (entry.getKey().contains("btnFirst")) {
						ctlFirst = entry.getKey();
					}  else if (entry.getKey().contains("btnPrev")) {
						ctlPrev = entry.getKey();
				    } else if (entry.getKey().contains("btnNext")) {
				    	ctlNext = entry.getKey();
				    } else if (entry.getKey().contains("btnLast")) {
				    	ctlLast = entry.getKey();
				    }
				}
				Matcher firstMat = firstPattern.matcher(html);
				Matcher prevMat = prevPattern.matcher(html);
				Matcher nextMat = nextPattern.matcher(html);
				Matcher lastMat = lastPattern.matcher(html);
				if (firstMat.find()){
					if (!firstMat.group(0).toLowerCase().contains("disabled")){
						paramsForNavigation.put(ctlFirst, "<< First");
						linkFirst = getNavLink(html, form.action, "First");
				    }
				}
				if (prevMat.find()){
					if (!prevMat.group(0).toLowerCase().contains("disabled")){
						paramsForNavigation.put(ctlPrev, "< Previous");
						linkPrev = getNavLink(html, form.action, "Previous");
				    }
				}
				if (nextMat.find()){
					if (!nextMat.group(0).toLowerCase().contains("disabled")){
						paramsForNavigation.put(ctlNext, "Next >");
						linkNext = getNavLink(html, form.action, "Next");
				    }
				}
				if (lastMat.find()){
					if (!lastMat.group(0).toLowerCase().contains("disabled")){
						paramsForNavigation.put(ctlLast, "Last >>");
						linkLast = getNavLink(html, form.action, "Last");
					}		    		
				}
				String navigation = "<br/>";
				if (!isEmpty(linkFirst)) {
					navigation += linkFirst + "&nbsp;&nbsp;&nbsp;";
				}
				if (!isEmpty(linkPrev)) {
					navigation += linkPrev + "&nbsp;&nbsp;&nbsp;";
				}
				String pages = "", cPage = "";
				Matcher pagesMat = numberOfPages.matcher(html);
				if (pagesMat.find()){
					pages = pagesMat.group(1);
				} else {
					pages = "1";
				}
				pagesMat.reset();
				pagesMat = currPage.matcher(html);
				if (pagesMat.find()){
					cPage = pagesMat.group(1);
				} else {
					cPage = "1";
				}
				navigation += "Page " + cPage + " of " + pages + "&nbsp;&nbsp;&nbsp;";
					
				if (!isEmpty(linkNext)) {
					navigation += linkNext + "&nbsp;&nbsp;&nbsp;";
				}
				if (!isEmpty(linkLast)) {
					navigation += linkLast;
				}
					
				if (paramsForNavigation != null){
					mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsForNavigation:", paramsForNavigation);
				}
				
				if (linkNext != null){
					String nextLink = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + form.action + "\">Next</a>";
					response.getParsedResponse().setNextLink(nextLink);
				}
				response.getParsedResponse().setHeader("&nbsp;&nbsp;&nbsp;" + navigation + "<br><br>" 
									+ table.substring(table.indexOf("<table"), table.indexOf(">") + 1)
										+ rows[0].toHtml());
				response.getParsedResponse().setFooter("</table><br><br>" + navigation + "<br><br>");			
				
				outputTable.append(table);
				}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	protected ResultMap parseIntermediaryRow(TableRow row, long searchId, int miServerId) throws Exception {
		ResultMap resultMap = new ResultMap();
		
		return resultMap;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
			ro.cst.tsearch.servers.functions.ILMcHenryTR.parseAndFillResultMap(detailsHtml, map, searchId, miServerID);
		return null;
	}
	
	@SuppressWarnings("deprecation")
	protected String getNavLink(String response, String action, String name) {
		
		Pattern pattern = Pattern.compile("(?i)<input.*?name\\s*=\\s*\\\"\\s*([^\\\"]+)\\\".*?" + name + "[^/]+/>");
		Matcher matcher = pattern.matcher(response);
		if (!matcher.find()) {
			return "";
		}
		String linkStart = CreatePartialLink(TSConnectionURL.idPOST);
		String link = "<a href=\"" + linkStart + "/forms/" + URLEncoder.encode(action) + "&link=" + name + "\">" + name + "</a>";

		return link;
	}
	
	protected String getDetails(String response){
		
		// if from memory - use it as is
		if(!response.contains("<html")){
			return response;
		}
		
		Node form = HtmlParserTidy.getNodeById(response, "aspnetForm", "form");
		String htmlForm = "", contents = "";
		try {
			htmlForm = HtmlParserTidy.getHtmlFromNode(form);
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		//contents = htmlForm;
		htmlForm = htmlForm.replaceAll("(?is)</?form[^>]*>", "");
		htmlForm = htmlForm.replaceAll("(?is)<select.*?ctl00\\$ddYears.*?selected\\s*>\\s*(\\d+)\\s*</option>.*?</select>", "$1");	
		htmlForm = htmlForm.replaceAll("(?is)<input[^>]+>", "");
		htmlForm = htmlForm.replaceAll("(?is)<a\\s+href[^>]+>[^<]+</a>", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		contents = "<table><tr><td></td></tr><tr>";
		String[] divId = {"ctl00_pnlParcelInformation", "ctl00_contentBody_pnlParcelDetail", "ctl00_contentBody_pnlOverviewAssessments", "ctl00_contentBody_pnlTaxRates", 
							"ctl00_contentBody_pnlNames", "ctl00_contentBody_pnlExemptions", "ctl00_contentBody_pnlSales", "ctl00_contentBody_pnlTaxSales", 
							"ctl00_contentBody_pnlPayments", "ctl00_contentBody_pnlSiteAddress", "ctl00_contentBody_pnlLegal", "ctl00_contentBody_Panel8", 
							"ctl00_contentBody_pnlFarmland", "ctl00_contentBody_pnlPublicNotes"};
		String[] headers = {" Parcel Information ", " Parcel Detail ", "Assessments", " Tax Rates ", "Names", "Exemptions", "Sales", " Tax Sales ", 
							"Payments", " Site Address ", " Legal Description ", " Lot/Acres ",	"Farmland", " Public Notes "};
		int numaratoare = 0;
		for (int i = 0; i < divId.length; i++) {
			form = HtmlParserTidy.getNodeById(htmlForm, divId[i], "div");
			String res = "";
			try {
				res = HtmlParserTidy.getHtmlFromNode(form);
			} catch (TransformerException e) {
				e.printStackTrace();
			}
			if (numaratoare < 3){
				contents += "<td><br><br><br><br><b> " + headers[i] +"</b>"+ res + "</td>";
				numaratoare ++;
			} else {
				contents += "</tr><tr><td><br><br><br><br><b>" + headers[i] + "</b>" +res + "</td>";
				numaratoare = 1;
			}
		}
		contents += "</tr></table>";
		contents = contents.replaceAll("(?is)</?div[^>]*>", "");
		
		//#### TAX History####
		contents = contents + "<br><br><br><b>Tax History</b><br>";
		Form formu = new SimpleHtmlParser(response).getForm(FORM_NAME);
		Map<String, String> paras = formu.getParams();
		
		List<String> years = new ArrayList<String>();
		Matcher matc = optionYears.matcher(response);
		while (matc.find()) {
			years.add(matc.group(1));
		}
		Collections.sort(years, Collections.reverseOrder());
		String linko = getBaseLink();
		for (int i = 0; i < years.size(); i++){
	    	HTTPRequest reqP = new HTTPRequest(linko.substring(0, linko.indexOf("/forms")) + "/overview.aspx");
	    	reqP.setMethod(HTTPRequest.POST);
	    	reqP.setPostParameter("ctl00$ddYears", years.get(i));
	    	reqP.setPostParameter("__EVENTTARGET", "ctl00$ddYears");
	    	reqP.setPostParameter("__VIEWSTATE", paras.get("__VIEWSTATE"));
	    	reqP.setPostParameter("__EVENTVALIDATION", paras.get("__EVENTVALIDATION"));
	    			    	
	    	HTTPResponse resP = null;
        	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try
			{
				resP = site.process(reqP);
			} finally 
			{
				HttpManager.releaseSite(site);
			}	
			String rsp = resP.getResponseAsString();
			form = HtmlParserTidy.getNodeById(rsp, "ctl00_contentBody_gvPayments", "table");
			htmlForm = "";
			try {
				htmlForm = HtmlParserTidy.getHtmlFromNode(form);
			} catch (TransformerException e) {
				e.printStackTrace();
			}
			htmlForm = htmlForm.replaceAll("(?is)</?font[^>]*>", "");
			htmlForm = htmlForm.replaceAll("(?is)\\s*bgcolor[^>]+", "");
			htmlForm = htmlForm.replaceAll("(?is)<tr[^>]*>\\s*<td[^>]+>Tax\\s*Billed.*?</tr>", "");
			htmlForm = htmlForm.replaceAll("(?is)<tr[^>]*>\\s*<td[^>]+>Penalty\\s*Billed.*?</tr>", "");
			htmlForm = htmlForm.replaceAll("(?is)<tr[^>]*>\\s*<td[^>]+>Cost\\s*Billed.*?</tr>", "");
			htmlForm = htmlForm.replaceAll("(?is)<tr[^>]*>\\s*<td[^>]+>Drainage\\s*Billed.*?</tr>", "");
			htmlForm = htmlForm.replaceAll("(?is)<tr[^>]*>\\s*<td[^>]+>Batch\\s*Number.*?</tr>", "");
			contents += "<br><br>" +htmlForm;
		}
		//#### end   TAX History####
		
		return contents;
	}
	
	/**
	 * Get tax year range from official site,
	 */
	private void getTaxYears(String county) {
		if (taxYears.containsKey("lastTaxYear" + county)  && taxYears.containsKey("firstTaxYear" + county))
			return;
		
		// Get official site html response.
		String response = getLinkContents(getDataSite().getLink());
		if(org.apache.commons.lang.StringUtils.isNotEmpty(response)) {
			HtmlParser3 parser = new HtmlParser3(response);
			NodeList selectList = parser.getNodeList()
					.extractAllNodesThatMatch(new TagNameFilter("select"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("name", "ctl00$ContentPlaceHolder1$ddlYearFrom"));
			if(selectList == null || selectList.size() == 0) {
				// Unable to find the tax year select input.
				logger.error("Unable to parse tax years!");
				return;
			}
			
			// Get the first and last tax years.
			SelectTag selectTag = (SelectTag) selectList.elementAt(0);
			OptionTag[] options = selectTag.getOptionTags();
			try {
				taxYears.put("lastTaxYear"+county, Integer.parseInt(options[0].getValue().trim()));
				taxYears.put("firstTaxYear"+county, Integer.parseInt(options[options.length - 1].getValue().trim()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Generate a <select> input corresponding to the tax years
	 * @param id
	 * @param name
	 * @return
	 */
	public String getYearSelect(String id, String name){
		String county = dataSite.getCountyName();
		
		int lastTaxYear = -1;
		try {
			lastTaxYear = taxYears.get("lastTaxYear" + county);
		} catch (Exception e) {
		}
		int firstTaxYear = -1;
		try {
			firstTaxYear = taxYears.get("firstTaxYear" + county);
		} catch (Exception e) {
		}
		if (lastTaxYear <= 0 || firstTaxYear <= 0) {
			// No valid tax years.
			// This is going to happen when official site is down or it's going to change its layout.
			lastTaxYear = getCurrentTaxYear();
			firstTaxYear = 1977;
		}
			
		// Generate input.
		StringBuilder select  = new StringBuilder("<select id=\"" + id + "\" name=\"" + name + "\" size=\"1\">\n");
		for (int i = lastTaxYear; i >= firstTaxYear; i--){
			select.append("<option ");
			select.append(i == lastTaxYear ? " selected " : "");
			select.append("value=\"" + i + "\">" + i + "</option>\n");
		}
		select.append("</select>");
			
		return select.toString();
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		
		String link = getBaseLink();
		String serverAddress = link.replaceAll("(?is)[^:]+://", "");
		msiServerInfoDefault.setServerAddress(serverAddress.substring(0, serverAddress.indexOf("/")));
		msiServerInfoDefault.setServerIP(serverAddress.substring(0, serverAddress.indexOf("/")));
		msiServerInfoDefault.setServerLink(serverAddress.substring(0, serverAddress.indexOf("/")));
		
		getTaxYears(dataSite.getCountyName());
		
		if(tsServerInfoModule != null) {
			// Generate the select input corresponding to the tax years.
			tsServerInfoModule.getFunction(10).setHtmlformat(getYearSelect("param_0_10", "param_0_10"));
			tsServerInfoModule.getFunction(11).setHtmlformat(getYearSelect("param_0_11", "param_0_11"));
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if(StringUtils.isNotEmpty(functionName)) {
					if("Parcel From".equals(functionName)) {
						if (miServerID == 373602){//Kendall
							htmlControl.setFieldNote("(e.g. 01-09-427-001)");
						} else if (miServerID == 375202){ //McHenry
							htmlControl.setFieldNote("(e.g. 04-12-300-010)");
						}
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if(tsServerInfoModule != null) {
			// Generate the select input corresponding to the tax years.
			tsServerInfoModule.getFunction(12).setHtmlformat(getYearSelect("param_1_12", "param_1_12"));
			tsServerInfoModule.getFunction(13).setHtmlformat(getYearSelect("param_1_13", "param_1_13"));
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if(StringUtils.isNotEmpty(functionName)) {
					if("House Number".equals(functionName)) {
						if (miServerID == 373602){//Kendall
							htmlControl.setFieldNote("(e.g. 254)");
						} else if (miServerID == 375202){ //McHenry
							htmlControl.setFieldNote("(e.g. 7723)");
						}
					} else if("Street Name".equals(functionName)) {
						if (miServerID == 373602){//Kendall
							htmlControl.setFieldNote("(e.g. Woodland)");
						} else if (miServerID == 375202){ //McHenry
							htmlControl.setFieldNote("(e.g. Cedar)");
						}
					}
				}
			}
		}
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
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