package ro.cst.tsearch.servers.types;


import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableHeader;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.w3c.dom.Node;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.pdftiff.util.Util;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeAdvancedFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator.SEARCH_WITH_TYPE;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.parentsite.ModuleWrapperManager;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.CertificationDateDS.CDType;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.Image;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.ImageI.IType;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.datatree.DataTreeImageException;
import com.stewart.datatree.DataTreeStruct;

/**
 * @author mihaib
 * 
 */

public class FLGenericMFCServerRO2 extends TSServerROLike implements TSServerROLikeI {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final Pattern NAV_LINK_PAT = Pattern.compile("f:'([^']+)'\\s*,v:([^\\}]+)\\}");
	private static final Pattern IMAGE_LINK_PAT = Pattern.compile("(?is)href=\\\"javascript:openPdf\\('([^']+)'\\);");

	
//	private static final Category logger = Logger.getLogger(FLGenericMFCServerRO2.class);
	
	public FLGenericMFCServerRO2(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		DocTypeAdvancedFilter doctypeFilter = DoctypeFilterFactory.getDoctypeFilterForGeneralIndexOwnerNameSearch(searchId);
		
		if (hasOwner()) {
			// for person names
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.clearSaKeys();

			FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(	SearchAttributes.OWNER_OBJECT, searchId, module);
			((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
			((GenericNameFilter) defaultNameFilter).setSkipUnique(false);
			((GenericNameFilter) defaultNameFilter).setUseSynonymsForCandidates(true);
			module.addFilter(doctypeFilter);
			module.addFilter(defaultNameFilter);

			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.forceValue(4, dataSite.getCountyIdAsString());
			module.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			module.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);

			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" });
			nameIterator.setSearchWithType(SEARCH_WITH_TYPE.PERSON_NAME);
			nameIterator.clearSearchedNames();
			nameIterator.setInitAgain(true);

			module.addIterator(nameIterator);
			modules.add(module);

			// for company names
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.clearSaKeys();

			defaultNameFilter = NameFilterFactory.getDefaultNameFilter(	SearchAttributes.OWNER_OBJECT, searchId, module);
			((GenericNameFilter) defaultNameFilter).setIgnoreMiddleOnEmpty(true);
			((GenericNameFilter) defaultNameFilter).setSkipUnique(false);
			((GenericNameFilter) defaultNameFilter).setUseSynonymsForCandidates(true);
			
			module.addFilter(doctypeFilter);
			module.addFilter(defaultNameFilter);

			module.forceValue(0, "b");
			module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
			module.forceValue(4, dataSite.getCountyIdAsString());
			module.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			module.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);

			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] { "L;;" });
			nameIterator.setSearchWithType(SEARCH_WITH_TYPE.COMPANY_NAME);
			nameIterator.clearSearchedNames();
			nameIterator.setInitAgain(true);

			module.addIterator(nameIterator);
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
		 
	/**
	 * @param rsResponse
	 * @param viParseID
	 */
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		
		if (rsResponse.indexOf("No Records Found") >= 0) {
			Response.getParsedResponse().setError("No Records Found.");
			Response.getParsedResponse().setResponse("");
			return;
		}
		
		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SAVE_TO_TSD :
		case ID_DETAILS :
			
			if (viParseID == ID_SAVE_TO_TSD){	
				
				String tableHeader = "<table border=\"1\"><tr><th>Order</th><th>From</th><th>To</th><th>Date</th><th>Document Type</th><th>County</th><th>Instrument Number</th><th>Book/Page</th><th>Pages</th>"
									+ "<th>Description</th></tr>";		
				String instr = "";
				Pattern pat = Pattern.compile("(?is)\\bvalue=\\\"([^\\\"]+)");
				Matcher mat = pat.matcher(rsResponse);
				if (mat.find()){
					instr = mat.group(1);
					instr = instr.replaceFirst("(?is)[^_]*_", "");
				}
				rsResponse = rsResponse.replaceFirst("(?is)<input[^>]*>", "Document");
				
				Matcher imgMat = IMAGE_LINK_PAT.matcher(rsResponse);
				
				if (imgMat.find()){
					StringBuffer imageLink = new StringBuffer(CreatePartialLink(TSConnectionURL.idGET));
					imageLink.append("/ori/image.do?instrumentNumber=").append(imgMat.group(1));
					Response.getParsedResponse().addImageLink(new ImageLinkInPage(imageLink.toString(), instr + ".pdf"));
				}
				
				rsResponse = rsResponse.replaceAll("(?is)<a[^>]*>\\s*View Image\\s*</a[^>]*>", "");
				
				String result = tableHeader + rsResponse + "</table>";
				Response.setResult(result);
				
				smartParseDetails(Response, result);
				
                msSaveToTSDFileName = instr + ".html";
                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
                
            } else{

				Node tbl = HtmlParserTidy.getNodeById(rsResponse, "search_results1", "table");
				String html = "";
				try {
					html = HtmlParserTidy.getHtmlFromNode(tbl);
				} catch (TransformerException e) {
					e.printStackTrace();
				}
				
				try {
					 
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
																		
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, html, outputTable);
					
					if(smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
		            }
					
				} catch(Exception e) {
					e.printStackTrace();
				}
            }
			
			break;
			
		case ID_GET_LINK:
			if (sAction.indexOf("navigate=display") >= 0) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			} else if (sAction.indexOf("doc=") >= 0){
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			break;
		default:
			break;
		}
	}

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		Search search = this.getSearch();
		searchId = search.getID();
		
		/**
		 * We need to find what was the original search module
		 * in case we need some info from it like in the new PS interface
		 */
		TSServerInfoModule moduleSource = null;
		Object objectModuleSource = response.getParsedResponse().getAttribute(
				TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
		if(objectModuleSource != null) {
			if(objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			} 
		} else {
			objectModuleSource = search.getAdditionalInfo(
					this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
			if (objectModuleSource instanceof TSServerInfoModule) {
				moduleSource = (TSServerInfoModule) objectModuleSource;
			}
		}
		
		try {
			table = table.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
			
			int numberOfUncheckedElements = 0;
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>");
			String tableheader = "";
			
				if (StringUtils.isNotEmpty(table)){
					org.htmlparser.Parser tableParser = org.htmlparser.Parser.createParser(table, null);
					NodeList mainTableList = tableParser.parse(new TagNameFilter("table"));
					if (mainTableList != null && mainTableList.size() > 0){
						TableTag mainTable = (TableTag) mainTableList.elementAt(0);		
						if (mainTable != null){
							TableRow[] rows = mainTable.getRows();
							
							if (rows != null){
								for (TableRow row : rows){
									TableHeader[] th = row.getHeaders();
									if (th != null && th.length > 0){
										tableheader = row.toHtml();
										tableheader = tableheader.replaceAll("(?s)</?a[^>]*>", "");
									}
									TableColumn[] tc = row.getColumns();
									if (tc != null && tc.length > 9){
										String instr = HtmlParser3.getValueFromCell(tc[6], "", false);
										instr = instr.replaceAll("(?is)&nbsp;", " ").trim();
										
										String bookPage = HtmlParser3.getValueFromCell(tc[7], "", false);
										bookPage = bookPage.replaceAll("(?is)&nbsp;", " ").trim();
										
										String recDate = HtmlParser3.getValueFromCell(tc[3], "", false);
										recDate = recDate.replaceAll("(?is)&nbsp;", " ").trim();
										
										String docType = HtmlParser3.getValueFromCell(tc[4], "", false);
										docType = docType.replaceAll("(?is)&nbsp;", " ").trim();
										
										String grantors = HtmlParser3.getValueFromCell(tc[1], "", true);
										String grantees = HtmlParser3.getValueFromCell(tc[2], "", true);

										String legalDesc = HtmlParser3.getValueFromCell(tc[9], "", true);
										legalDesc = legalDesc.replaceAll("(?is)<a[^>]+>\\s*View Image\\s*</a>", "");
										legalDesc = legalDesc.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)<br/?>", " ");
											
										String key = instr + "_" + docType.replaceAll("\\s+", "_");
						
										ParsedResponse currentResponse = responses.get(key);							 
										if(currentResponse == null) {
											currentResponse = new ParsedResponse();
											responses.put(key, currentResponse);
										}
										String year = "";
										
										RegisterDocumentI document = (RegisterDocumentI)currentResponse.getDocument();				

										ResultMap resultMap = new ResultMap();
												
										String link = CreatePartialLink(TSConnectionURL.idGET) + "/ori/search.do?doc=" + key + "&useRawLink=true";
										
										if (document == null){
											
											resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDesc);
													
											String rowHtml =  row.toHtml();
													
											resultMap.put("tmpPartyGtor", grantors);
											resultMap.put("tmpPartyGtee", grantees);
											resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO2");
											resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instr);
											String[] bp = bookPage.split("\\s*/\\s*");
											if (bp.length == 2){
												resultMap.put(SaleDataSetKey.BOOK.getKeyName(), StringUtils.stripStart(bp[0], "0"));
												resultMap.put(SaleDataSetKey.PAGE.getKeyName(), StringUtils.stripStart(bp[1], "0"));
											}
											resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
											resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate.trim());
											year = ro.cst.tsearch.utils.StringUtils.extractParameter(recDate, "\\d+/\\d+/(\\d{2}(?:\\d{2})?)$");
											try {
												parseNamesRO(resultMap, searchId);
												parseLegal(resultMap, searchId);
											} catch (Exception e) {
												e.printStackTrace();
											}
											resultMap.removeTempDef();
								    				
											currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
																					
											Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
											document = (RegisterDocumentI) bridge.importData();
													
											currentResponse.setDocument(document);
											String checkBox = "checked";
											HashMap<String, String> data = new HashMap<String, String>();
											data.put("type", docType);
											data.put("year", year);
													
											if (isInstrumentSaved(instr, null, data) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
												checkBox = "saved";
											} else {
												numberOfUncheckedElements++;
												LinkInPage linkInPage = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
												currentResponse.setPageLink(linkInPage);
												checkBox = "<input type=\"checkbox\" name=\"docLink\" value=\"" + link + "\">";
												/**
												 * Save module in key in additional info. The key is instrument number that should be always available. 
												 */
												String keyForSavingModules = this.getKeyForSavingInIntermediary(instr);
												search.setAdditionalInfo(keyForSavingModules, moduleSource);
											}

											mSearch.addInMemoryDoc(link, rowHtml);
											
											rowHtml = rowHtml.replaceFirst("(?is)(<tr[^>]*>\\s*<td>)\\s*<input[^>]*>", "$1" + checkBox);
											
											Matcher imgMat = IMAGE_LINK_PAT.matcher(rowHtml);
											
											if (imgMat.find()){
												StringBuffer imageLink = new StringBuffer(CreatePartialLink(TSConnectionURL.idGET));
												imageLink.append("/ori/image.do?instrumentNumber=").append(imgMat.group(1));
												rowHtml = rowHtml.replaceFirst("(?is)\\bhref=\"[^\\\"]+\\\"", "href=\"" + imageLink.toString() + "\" target=\"_blank\"");
												currentResponse.addImageLink(new ImageLinkInPage(imageLink.toString(), instr + ".pdf"));

											}
											
											currentResponse.setOnlyResponse(rowHtml);
											newTable.append(currentResponse.getResponse());
											intermediaryResponse.add(currentResponse);
										}
									}								
								}
							}
						}
						newTable.append("</table>");
						outputTable.append(newTable);
						SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
					}
				}
				
			String spanNav = "";
			Pattern SPAN_NAV_PAT = Pattern.compile("(?is)(<span class=\\\"pagebanner\\\">[^<]+</span>\\s*<span class=\\\"pagelinks\\\">.*?</span>)");
			Matcher mat = SPAN_NAV_PAT.matcher(response.getResult());
			if (mat.find()){
				spanNav = mat.group(1);
				
				mat.reset();
				Pattern pat = Pattern.compile("(?is)href=\\\"(javascript:displaytagform[^\\\"]+)\\\"");
				mat = pat.matcher(spanNav);
				
				StringBuffer newLink = new StringBuffer(CreatePartialLink(TSConnectionURL.idPOST) + "/ori/ordercreate.do?navigate=display");
			
				while (mat.find()){
					String oldLink = mat.group(1);
					Matcher matc = NAV_LINK_PAT.matcher(oldLink);
					
					while (matc.find()){
						String paramName = matc.group(1);
						String paramValue = matc.group(2);
						
						if (StringUtils.isNotEmpty(paramName)){
							if (StringUtils.isNotEmpty(paramValue)){
								paramValue = paramValue.replaceAll("[\\[\\]']+", "");
								String[] paramValues = paramValue.split("\\s*,\\s*");
								if (paramValues.length > 1){
									for (String paramVal : paramValues) {
										newLink.append("&").append(paramName).append("=").append(paramVal);
									}
								} else{
									newLink.append("&").append(paramName).append("=").append(paramValue);
								}
							}
						}
					}
					spanNav = spanNav.replace(oldLink, newLink.toString());
					newLink = new StringBuffer(CreatePartialLink(TSConnectionURL.idPOST) + "/ori/ordercreate.do?navigate=display");
				}
			
			}
			
			String nextLink = ro.cst.tsearch.utils.StringUtils.extractParameter(spanNav, "(?is)(<a[^>]*>\\s*Next\\s*</a>)");
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(nextLink)) {
				response.getParsedResponse().setNextLink(nextLink);
			}
			
			String header1 = tableheader.replaceFirst("(?is)(<tr[^>]*>\\s*<th>)Order</th>", "$1" + SELECT_ALL_CHECKBOXES + "Check\\Uncheck All</th>");
			
			response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "GET") 
								+ "<br>" + spanNav + "<br><br>" 
					+ "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
				
			response.getParsedResponse().setFooter("</table>" +  
							"<br>" + spanNav + "<br><br>" +  CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101, -1));
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String rsResponse, ResultMap resultMap) {
		
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO2");
			
			org.htmlparser.Parser tableParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList rowList = null;
			try {
				rowList = tableParser.parse(new TagNameFilter("tr"));
			} catch (ParserException e) {
				e.printStackTrace();
			}
			if (rowList != null && rowList.size() > 1){
				TableRow row = (TableRow) rowList.elementAt(1);		
				if (row != null){
					TableColumn[] tc = row.getColumns();
					if (tc != null && tc.length > 0){
						String instr = HtmlParser3.getValueFromCell(tc[6], "", false);
						instr = instr.replaceAll("(?is)&nbsp;", " ").trim();
						if (!(StringUtils.containsIgnoreCase(instr, "BK") && StringUtils.containsIgnoreCase(instr, "PG"))) {
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instr);
						}
						
						String bookPage = HtmlParser3.getValueFromCell(tc[7], "", false);
						bookPage = bookPage.replaceAll("(?is)&nbsp;", " ").trim();
						String[] bp = bookPage.split("\\s*/\\s*");
						if (bp.length == 2){
							resultMap.put(SaleDataSetKey.BOOK.getKeyName(), StringUtils.stripStart(bp[0], "0"));
							resultMap.put(SaleDataSetKey.PAGE.getKeyName(), StringUtils.stripStart(bp[1], "0"));
						}
						
						String recDate = HtmlParser3.getValueFromCell(tc[3], "", false);
						recDate = recDate.replaceAll("(?is)&nbsp;", " ").trim();
						resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate);
								
						String docType = HtmlParser3.getValueFromCell(tc[4], "", false);
						docType = docType.replaceAll("(?is)&nbsp;", " ").trim();
						resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
						
						String grantors = HtmlParser3.getValueFromCell(tc[1], "", true);
						String grantees = HtmlParser3.getValueFromCell(tc[2], "", true);
						
						resultMap.put("tmpPartyGtor", grantors);
						resultMap.put("tmpPartyGtee", grantees);
						
						String legalDesc = HtmlParser3.getValueFromCell(tc[9], "", true);
						legalDesc = legalDesc.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)\\bNONE\\b", " ").replaceAll("(?is)<br/?>", " ");
						resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDesc);
						
						parseNamesRO(resultMap, searchId);
						parseLegal(resultMap, searchId);
					}
				}
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	@SuppressWarnings({ "rawtypes" })
	public static void parseNamesRO(ResultMap m, long searchId) throws Exception{
		
			String names[] = {"", "", "", "", "", ""};
			String[] suffixes = {"", ""}, type = {"", ""}, otherType = {"", ""};
			ArrayList<List> grantor = new ArrayList<List>();
			ArrayList<List> grantee = new ArrayList<List>();
			
			String tmpPartyGtor = StringUtils.defaultString((String) m.get("tmpPartyGtor")).replaceAll("&nbsp;", " ");
			if (StringUtils.isNotBlank(tmpPartyGtor)) {
				
				tmpPartyGtor = tmpPartyGtor.replaceAll("(?is)&amp;", "&");
				
				tmpPartyGtor = cleanName(tmpPartyGtor);
				
				String[] gtors = tmpPartyGtor.split("\\s*,\\s*");
				for (String grantorName : gtors){					
					names = StringFormats.parseNameNashville(grantorName, true);
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					if (NameUtils.isNotCompany(names[2])){
						suffixes = GenericFunctions.extractNameSuffixes(names);
					}
					
					GenericFunctions.addOwnerNames(grantorName, names, suffixes[0],
													suffixes[1], type, otherType,
													NameUtils.isCompany(names[2]),
													NameUtils.isCompany(names[5]), grantor);
				}
				
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), tmpPartyGtor);
				m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			}
			
			String tmpPartyGtee = StringUtils.defaultString((String)m.get("tmpPartyGtee")).replaceAll("&nbsp;", " ");
			if (StringUtils.isNotBlank(tmpPartyGtee)){
				
				tmpPartyGtee = tmpPartyGtee.replaceAll("(?is)&amp;", "&");
				tmpPartyGtee = cleanName(tmpPartyGtee);
				
				String[] gtee = tmpPartyGtee.split("\\s*,\\s*");
				for (String granteeName : gtee){
		
					names = StringFormats.parseNameNashville(granteeName, true);
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					if (NameUtils.isNotCompany(names[2])){
						suffixes = GenericFunctions.extractNameSuffixes(names);
					}
					
					GenericFunctions.addOwnerNames(granteeName, names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), grantee);
				}
				
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), tmpPartyGtee);
				m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
				
			}
			
			GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
		
	}
	
	public static String cleanName(String name){
		name = name.replaceAll("(?is)\\bDECEASED\\b", "");
		name = name.replaceAll("(?is)\\b(CO PER|PERSONAL|PERS) REP\\b", "");
		name = name.replaceAll("(?is)\\bGUARDIANSHIP\\b", "");
		name = name.replaceAll("(?is)\\bTO WHOM IT MAY CONCERN\\b", "");
		name = name.replaceAll("(?is)\\bATTY IN FACT\\b", "");
		name = name.replaceAll("(?is)\\bNKA\\b", "");
		name = name.replaceAll("(?is)\\b[A|F]/?K/?A\\b", "");
		
		
		return name;
	}
	@SuppressWarnings("rawtypes")
	public static void parseLegal(ResultMap resultMap, long searchId) throws Exception{
		
		String legal = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());

		if (StringUtils.isNotEmpty(legal)){

			legal = legal.trim();
			
			legal = legal.replaceAll("(?is)\\AN\\b", "");
			legal = legal.replaceAll("(?is)\\bNUIMBER\\b", "");
			legal = legal.replaceAll("(?is)\\bNUMBER\\b", "");
			legal = legal.replaceAll("(?is)\\b(REPLAT|UNREC|NONE|MISC EQUIPMENT|NEW DWELLING|N PT ACRS|ETC|BE INV HG)\\b", "");
			legal = legal.replaceAll("(?is)\\b(ASSIGNMENT AFD|FEDERAL TAX LIEN IRS|CORRECTIVE)\\b", "");
			legal = legal.replaceAll("(?is);", "");
			
			legal = GenericFunctions.replaceNumbers(legal);
			
			boolean hasLot = false, hasBlock = false, hasUnit = false;
			
			String legalTemp = legal;
			String lot = "", blk = "";

			Pattern p = Pattern.compile("(?is)\\b(LO?T?S?\\s+|L-?)([\\d&\\s-]+)\\b");
			Matcher	ma = p.matcher(legal);
			while (ma.find()) {
				lot = lot + " " + ma.group(2).trim().replaceAll("\\A0+$", "");
				legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
			}
			
			if (lot.trim().length() != 0) {
				hasLot = true;
				lot = lot.replaceAll("\\s*&\\s*", " ");
				lot = LegalDescription.cleanValues(lot, false, true);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot.trim());
			}
			
			String docType = (String)resultMap.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName());
			if (StringUtils.isNotEmpty(docType) 
					&& (DocumentTypes.isJudgementDocType(docType, searchId, false) 
							|| DocumentTypes.isLisPendensDocType(docType, searchId, false))){
				
				String refset = GenericFunctions.extractFromComment(legalTemp, "(?is)(?:CASE NO|Case#|JUDGMENT(?:\\s+DISSOLUTION)?|#)\\s*([\\d-]+\\s*(?:[A-Z]{1,2}|CFA?)(?:[\\d-]+[A-Z]?)?)\\b", 1);
				
				resultMap.put(OtherInformationSetKey.REMARKS.getKeyName(), refset);
			}
			legalTemp = legalTemp.replaceAll("(?is)(?:CASE NO|Case#|JUDGMENT(?:\\s+DISSOLUTION)?|#)\\s*([\\d-]+\\s*(?:[A-Z]{1,2}|CFA?)(?:[\\d-]+[A-Z]?)?)\\b", "");
			
			p = Pattern.compile("(?is)\\b(BLO?C?KS?\\s+|BL?-)([\\d-]+[A-Z]?|[A-Z])\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				blk = blk + " " + ma.group(2).trim().replaceAll("\\A0+", "");
				legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
			} else{
				p = Pattern.compile("(?is)\\b(B)([\\d-]+[A-Z]?|\\b[A-Z])\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					blk = blk + " " + ma.group(2).trim().replaceAll("\\A0+", "");
					legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
				}
			}

			if (blk.trim().length() != 0) {
				hasBlock = true;
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), blk);
			}
				
			p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s+(\\d+[A-Z]?)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), ma.group(2).trim());
				legal = legal.replaceFirst(ma.group(0), " ");
			}
				
			p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s+(\\d+[A-Z]?|[A-Z][\\d-]?)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), ma.group(2).trim());
				legal = legal.replaceFirst(ma.group(0), " ");
			}
				
			p = Pattern.compile("(?is)\\b(UNIT\\s+|U-?)([\\d-]+[A-Z]?|[A-Z])\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				hasUnit = true;
				legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), ma.group(2).trim());
			}
			
			p = Pattern.compile("(?is)\\b(PHASE)\\s+(\\d+|[A-Z]+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), ma.group(2).trim());
			}
			
			p = Pattern.compile("(?is)\\bSec\\s*(\\d+)\\s*(?:To?wn(?:shp)?)\\s*(\\d+[A-Z]?)\\s+RA?ngE?\\s*(\\d+[A-Z]?)");
			ma = p.matcher(legal);
			if (ma.find()) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),ma.group(1).trim());
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ma.group(2).trim());
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), ma.group(3).trim());
				legalTemp = legalTemp.replaceFirst(ma.group(0)," ");
			} else{
				p = Pattern.compile("(?is)\\b(\\d{1,3})\\s*-\\s*(\\d{1,3}[A-Z]?)\\s*-\\s*(\\d{1,3}[A-Z]?)\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),ma.group(1).trim());
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ma.group(2).trim());
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), ma.group(3).trim());
					legalTemp = legalTemp.replaceFirst(ma.group(0)," ");
				} else{
					p = Pattern.compile("(?is)\\bS(\\d{1,3})\\s*[-,]+\\s*T(\\d{1,3}[A-Z]?)\\s*[-,]+\\s*R(\\d{1,3}[A-Z]?)\\b");
					ma = p.matcher(legal);
					if (ma.find()) {
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),ma.group(1).trim());
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ma.group(2).trim());
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), ma.group(3).trim());
						legalTemp = legalTemp.replaceFirst(ma.group(0)," ");
					} else{//SEC 17 TWP 6S RNG 18E
						p = Pattern.compile("(?is)\\bSEC\\s*(\\d{1,3})\\s+(?:TWP\\s+)?(\\d{1,3}[A-Z]?)\\s+(?:RNG\\s+)?(\\d{1,3}[A-Z]?)\\b");
						ma = p.matcher(legal);
						if (ma.find()) {
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),ma.group(1).trim());
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), ma.group(2).trim());
							resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), ma.group(3).trim());
							legalTemp = legalTemp.replaceFirst(ma.group(0)," ");
						}
					}
				}
			}
			
			p = Pattern.compile("(?is)\\b(?:PLAT BOOK|(?:DB|PB)-)\\s*(\\d+|[A-Z])\\s*(?:PAGE|PG-)\\s*(\\d+[A-Z]?)(?:[/|&]\\d+[A-Z]?)?\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), ma.group(1).trim());
				resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), ma.group(2).trim());
				legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
			}
					
			p = Pattern.compile("(?is)\\b(?:ORB?|OFFICIAL\\s+RECORD?S?|ORSB|BK)\\s*(?:\\s+BOOK\\s*)?-?(\\d+)\\s*(?:PAGE|/|PG?)\\s*-?(\\d+)");
			ma = p.matcher(legal);
			if (ma.find()) {

				List<List> bodyCR = new ArrayList<List>();
				List<String> line = new ArrayList<String>();
				line.add("");
				line.add(StringUtils.stripStart(ma.group(1), "0"));
				line.add(StringUtils.stripStart(ma.group(2), "0"));
				bodyCR.add(line);
				
				if (!bodyCR.isEmpty()){
					String [] header = {"InstrumentNumber", "Book", "Page"};		   
					Map<String,String[]> map = new HashMap<String,String[]>();		   
					map.put("InstrumentNumber", new String[]{"InstrumentNumber", ""});
					map.put("Book", new String[]{"Book", ""});
					map.put("Page", new String[]{"Page", ""});
					
					ResultTable cr = new ResultTable();	
					cr.setHead(header);
					cr.setBody(bodyCR);
					cr.setMap(map);		   
					resultMap.put("CrossRefSet", cr);
				}
				
			legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
			}
			
			legalTemp = legalTemp.replaceAll("(?is)\\bSBD\\b", "");
			if (hasLot || hasBlock || hasUnit){
				legalTemp = legalTemp.replaceFirst("(?is)\\bPT\\b.*", "");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), legalTemp.trim());
			}
		}

	}

	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		TSServerInfoModule module = null;
		module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		module.setParamValue(4, "Detail Data");
		if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module.forceValue(1, book);
			module.forceValue(2, page);
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getDocumentNumber())) {
			module.forceValue(3, restoreDocumentDataI.getDocumentNumber());
		} else {
			module = null;
		}
		return module;
	}
	
	@Override
	protected NameI getNameFromModule(TSServerInfoModule module) {
		NameI name = new Name();
		if(module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 1) {
			String usedName = module.getFunction(1).getParamValue();
			if(StringUtils.isEmpty(usedName)) {
				return null;
			}
			String[] names = null;
			if(NameUtils.isCompany(usedName)) {
				names = new String[]{"", "", usedName, "", "", ""};
			} else {
				names = StringFormats.parseNameNashville(usedName, true);
			}
			name.setLastName(names[2]);
			name.setFirstName(names[0]);
			name.setMiddleName(names[1]);
			return name;
		}
		return null;
	}
	
	@Override
	protected DownloadImageResult saveImage(ImageI image) throws ServerResponseException {
		try {
			byte[] imageBytes = null;
			String imageLink = StringUtils.defaultString(image.getLink(0));
			HashMap<String, String> map = HttpUtils.getParamsFromLink(imageLink);
			boolean imageFoundOnRO2 = true;

			// if image link was found on RO2 link is like: // ...&Link=/ori/image.do?instrumentNumber=2003001517
			// if not, link looks like: ...&Link=look_for_dt_image&id=&description=&instr=85008231&book=214&page=587&year=1985&month=07&day=08
			if (imageLink.indexOf("look_for_dt_image") != -1 && StringUtils.containsIgnoreCase(image.getContentType(), "tif")) {
				imageFoundOnRO2 = false;
			}

			if (imageFoundOnRO2) {
				if (image != null && image.getLinks().size() > 0) {
					imageLink = getBaseLink() + imageLink.substring(imageLink.indexOf("/ori"));

					HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
					try {
						imageBytes = ((ro.cst.tsearch.connection.http2.FLGenericMFCConnRO2) site).process(new HTTPRequest(imageLink, HTTPRequest.GET))
								.getResponseAsByte();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						HttpManager.releaseSite(site);
					}
				}

				if (imageBytes != null) {

					try {
						String imageBytesToString = new String(imageBytes, "ISO-8859-1");
						if (!imageBytesToString.startsWith("%PDF")) {
							return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
						}
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}

					afterDownloadImage(true);
				} else {
					return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
				}
			} else {// if image not found on RO2
				boolean savedImage = false;
				String book = map.get("book");
				String page = map.get("page");
				String instNo = map.get("instr");
				String year = map.get("year");

				int yearInt = 0;
				yearInt = Integer.parseInt(year);

				// remove year from instr and leading zeros
				if (instNo.length() >= year.length() && year.equals(instNo.substring(0, year.length()))) {
					instNo = instNo.substring(4).replaceFirst("^0+", "");
				} else if (instNo.length() >= 2 && year.endsWith(instNo.substring(0, 2))) {
					instNo = instNo.substring(2).replaceFirst("^0+", "");
				}

				// we are here only if image wasn't taken from RO2
				SearchLogger.info("<br/>FAILED to take Image(searchId=" + searchId + ") for inst=" + instNo + " from RO2.<br/>", searchId);

				InstrumentI i = new Instrument();
				i.setInstno(instNo);
				i.setYear(yearInt);
				i.setBook(book);
				i.setPage(page);

				FLGenericDTG flGenericDTG = isCountyAvailableOnDTG();
				boolean isCountyAvailableOnDTG = false;
				List<DataTreeStruct> dataTreeList = null;

				if (flGenericDTG != null) {
					dataTreeList = flGenericDTG.initDataTreeStruct();
					isCountyAvailableOnDTG = true;
				}

				if (isCountyAvailableOnDTG) {
					try {
						savedImage = FLGenericDASLDT.downloadImageFromDataTree(i, dataTreeList, image.getPath(), getCommunityId() + "", null, null);
						if (savedImage) {
							afterDownloadImage(savedImage, GWTDataSite.DG_TYPE);
							SearchLogger.info("<br/>Image(searchId=" + searchId + ") for book=" + i.getBook() + " page=" +
									i.getPage() + "; inst=" + i.getInstno() + " year=" + year + " was taken from DataTree<br/>", searchId);
						} else {
							SearchLogger.info("<br/>FAILED to take Image(searchId=" + searchId + ") for book=" + i.getBook() + " page="
									+ i.getPage() + "; inst=" + i.getInstno() + " year=" + year + " from DataTree.<br/>", searchId);

						}
					} catch (DataTreeImageException e) {
						logger.error("Error while getting image ", e);
						SearchLogger.info(
								"<br/>FAILED to take Image(searchId=" + searchId + ") for book=" + i.getBook() + " page=" + i.getPage()
										+ "; inst=" + i.getInstno() + " year=" + year + " from DataTree. " +
										"Official Server Message: [" + e.getLocalizedMessage() + " (" + e.getStatus() + ") ]<br/>", searchId);
					}
				} else {
					SearchLogger.info("<br/>Image(searchId=" + searchId + " )book=" + i.getBook() + "page=" + i.getPage() + "inst=" + instNo
							+ " can't be taken from DT because this county is unavailable on this site.<br/>", searchId);
				}

				// if image not found on either on SF, RO2 or DT, search on PI
				if (!savedImage) {
					try {
						savedImage = FLGenericDASLDT.downloadImageFromPropertyInsight(image.getPath(), FLGenericDASLDT.getPiQuery(i, searchId), searchId).success;
						if (savedImage) {
							SearchLogger.info(
									"<br/>Image(searchId=" + searchId + ") for book=" + i.getBook() + " page=" + i.getPage()
											+ "; inst=" + i.getInstno() + " year=" + year + " was taken from PI.<br/>", searchId);
						} else {
							SearchLogger.info(
									"<br/>FAILED to take Image(searchId=" + searchId + ") for book=" + i.getBook() + " page="
											+ i.getPage() + "; inst=" + i.getInstno() + " year=" + year + " from PI.<br/>", searchId);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			String imageName = image.getPath();
			if (FileUtils.existPath(imageName)) {
				imageBytes = FileUtils.readBinaryFile(imageName);
				return new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType());
			}
			ServerResponse resp = new ServerResponse();
			resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType()));

			DownloadImageResult dres = resp.getImageResult();
			return dres;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected boolean downloadImageFromMyFlorida(InstrumentI i, ImageI image, String instrumentNumber, String fileName) throws IOException{
		boolean savedImage = false;
		
		byte[] imageBytes = null;   	
				
		HttpSite site = null;
		
		try {
			site = HttpManager.getSite(getCurrentServerName(), searchId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (site != null){
			try {
				imageBytes = ((ro.cst.tsearch.connection.http2.FLGenericMFCConnRO2)site).getImage(instrumentNumber, i.getBook(), i.getPage());
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				HttpManager.releaseSite(site);
			}
		}
		
		if (imageBytes != null) {
			try {
				String imageBytesToString = new String(imageBytes, "ISO-8859-1");
				if (!imageBytesToString.startsWith("%PDF")){
					if (imageBytesToString.contains("Image unavailable on RO2")){
						SearchLogger.info("<br/>Image(searchId=" + searchId + " )book=" + i.getBook() + "page=" + i.getPage() + "inst=" + instrumentNumber + " unavailable on RO2 site.<br/>", searchId);
					} else if (imageBytesToString.contains("This county is not available on RO2")){
						SearchLogger.info("<br/>This county is not available on RO2.<br/>", searchId);
					}
					
					return false;
				} else{
					if (image != null){
						if (image.getContentType().toLowerCase().endsWith("tiff")){
							image.setImageAsPdf();
						}
					}
					savedImage = true;
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			afterDownloadImage(true);
		} else {
			return false;
		}
		
		if (imageBytes != null && imageBytes.length>0){
	    		
			//image was downloaded -> mark this as soon as possible
			Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			search.countNewImage(GWTDataSite.R2_TYPE);
	    	if (image == null){
	    		if (StringUtils.isNotEmpty(fileName)){
	    			org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(fileName), imageBytes);
	    			fileName = Util.convertPDFToTIFF( fileName, "", ro.cst.tsearch.utils.FileUtils.changeExtension(fileName, "tiff"));
	    		}
	    	} else{
	    		String imagePath = image.getPath();
	    		org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(imagePath), imageBytes);
	    		imagePath = Util.convertPDFToTIFF(imagePath, "", ro.cst.tsearch.utils.FileUtils.changeExtension(imagePath, "tiff"));
	    		image.setImageAsTiff();
	    	}
			savedImage = true;
		}
		
		return savedImage;
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		ModuleWrapperManager moduleWrapperManager = ModuleWrapperManager.getInstance();
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID);
		String siteName = StateCountyManager.getInstance().getSTCounty(dataSite.getCountyId()) + dataSite.getSiteType();
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		
		if (tsServerInfoModule != null) {
			
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			
			if (tsServerInfoModule.getModuleParentSiteLayout() != null){
		        PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
				for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
					String functionName = htmlControl.getCurrentTSSiFunc().getName();
					if (StringUtils.isNotEmpty(functionName)) {
						String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.NAME_MODULE_IDX, nameToIndex.get(functionName));
						if (comment != null) {
							htmlControl.setFieldNote(comment);
						}
					}
				}
			}
		}
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	public HashMap<String, String> getDocumentFromRO2(InstrumentI instrument){
		
		HashMap<String, String> docItems = new HashMap<String, String>();
		
					
		String docNo = instrument.getInstno();
		String book = instrument.getBook();
		String page = instrument.getPage();
		int yearInt = instrument.getYear();
		int monthInt = 0;
		
		String date = "";
		if (instrument.getDate() != null){
			date = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(instrument.getDate());
			if (StringUtils.isNotEmpty(date)){
				date = date.replaceAll("/", "");
				String month = "";
				if (date.length() == 7){
	    			if (StringUtils.isEmpty(month)){
	    				month = date.substring(0, 1);
	    			}
	    		} else if (date.length() == 8){
	    			if (StringUtils.isEmpty(month)){
	    				month = date.substring(0, 2);
	    				month = StringUtils.stripStart(month, "0");
	    			}
	    		}
				if (StringUtils.isNotEmpty(month)){
					try {
						monthInt = Integer.parseInt(month);
					} catch (Exception e) {
					}
				}
			}
		}
		String response = "";
		
		if (CountyConstants.FL_Columbia == dataSite.getCountyId() && yearInt == 2007 && monthInt == 0){
			for (int i = 1; i < 13; i++){
				response = getResponse(docNo, book, page, yearInt, i);
				if (!response.contains("No Records Found") && response.contains("Search Results")){
					break;
				}
			}
		} else if (CountyConstants.FL_Gulf == dataSite.getCountyId() && yearInt == 2007 && monthInt == 0){
			for (int i = 1; i < 13; i++){
				response = getResponse(docNo, book, page, yearInt, i);
				if (!response.contains("No Records Found") && response.contains("Search Results")){
					break;
				}
			}
		} else if (CountyConstants.FL_Lee == dataSite.getCountyId() && yearInt == 2005 && monthInt == 0){
			for (int i = 1; i < 13; i++){
				response = getResponse(docNo, book, page, yearInt, i);
				if (!response.contains("No Records Found") && response.contains("Search Results")){
					break;
				}
			}
		} else if (CountyConstants.FL_Pinellas == dataSite.getCountyId() && (yearInt == 2001 || yearInt == 2003) && monthInt == 0){
			for (int i = 1; i < 13; i++){
				response = getResponse(docNo, book, page, yearInt, i);
				if (!response.contains("No Records Found") && response.contains("Search Results")){
					break;
				}
			}
		} else if (CountyConstants.FL_Putnam == dataSite.getCountyId() && yearInt == 2007 && monthInt == 0){
			for (int i = 1; i < 13; i++){
				response = getResponse(docNo, book, page, yearInt, i);
				if (!response.contains("No Records Found") && response.contains("Search Results")){
					break;
				}
			}
		} else if (CountyConstants.FL_Santa_Rosa == dataSite.getCountyId() && yearInt == 1984 && monthInt == 0){
			for (int i = 1; i < 13; i++){
				response = getResponse(docNo, book, page, yearInt, i);
				if (!response.contains("No Records Found") && response.contains("Search Results")){
					break;
				}
			}
		} else if (CountyConstants.FL_Sarasota == dataSite.getCountyId() && yearInt == 1998 && monthInt == 0){
			for (int i = 1; i < 13; i++){
				response = getResponse(docNo, book, page, yearInt, i);
				if (!response.contains("No Records Found") && response.contains("Search Results")){
					break;
				}
			}
		} else{
			response = getResponse(docNo, book, page, yearInt, monthInt);
		}
					
		
		if (!response.contains("No Records Found") && response.contains("Search Results")){
			org.htmlparser.Parser tableParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainTableList = null;
			try {
				mainTableList = tableParser.parse(new TagNameFilter("table")).extractAllNodesThatMatch(new HasAttributeFilter("id", "search_results1"), true);
			} catch (ParserException e) {
				e.printStackTrace();
			}
			if (mainTableList != null && mainTableList.size() > 0){
				TableTag mainTable = (TableTag) mainTableList.elementAt(0);		
				if (mainTable != null){
					TableRow[] rows = mainTable.getRows();
						
					if (rows != null){
						for (TableRow row : rows){
							
							TableColumn[] tc = row.getColumns();
							if (tc != null && tc.length > 9){
								String instr = HtmlParser3.getValueFromCell(tc[6], "", false);
								instr = instr.replaceAll("(?is)&nbsp;", " ").trim();
								docItems.put(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), instr);
									
								String bookPage = HtmlParser3.getValueFromCell(tc[7], "", false);
								bookPage = bookPage.replaceAll("(?is)&nbsp;", " ").trim();
								String[] bp = bookPage.split("\\s*/\\s*");
								if (bp.length == 2){
									docItems.put(SaleDataSetKey.BOOK.getShortKeyName(), StringUtils.stripStart(bp[0], "0"));
									docItems.put(SaleDataSetKey.PAGE.getShortKeyName(), StringUtils.stripStart(bp[1], "0"));
								}
									
								String recDate = HtmlParser3.getValueFromCell(tc[3], "", false);
								recDate = recDate.replaceAll("(?is)&nbsp;", " ").trim();
								docItems.put(SaleDataSetKey.RECORDED_DATE.getShortKeyName(), recDate);
									
								String docType = HtmlParser3.getValueFromCell(tc[4], "", false);
								docType = docType.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)\\s", "").trim();
								docItems.put(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(), docType);
									
								String grantors = HtmlParser3.getValueFromCell(tc[1], "", true);
								docItems.put(SaleDataSetKey.GRANTOR.getShortKeyName(), grantors);
									
								String grantees = HtmlParser3.getValueFromCell(tc[2], "", true);
								docItems.put(SaleDataSetKey.GRANTEE.getShortKeyName(), grantees);

								String legalDesc = HtmlParser3.getValueFromCell(tc[9], "", true);
								legalDesc = legalDesc.replaceAll("(?is)<a[^>]+>\\s*View Image\\s*</a>", "");
								legalDesc = legalDesc.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)<br/?>", " ");
								docItems.put(SaleDataSetKey.REMARKS.getShortKeyName(), legalDesc);
							}
						}
					}
				}
			}
		}
		
		if (docItems.size() > 0){
			SearchLogger.info("<br/>Document Index information(searchId=" + searchId + ") for book=" + instrument.getBook() 
					+ " page=" + instrument.getPage() + "; inst=" + instrument.getInstno() 
					+ " year=" + instrument.getYear() + " was taken from " + dataSite.getSiteTypeAbrev() + " .<br/>", searchId);
		} else{
			SearchLogger.info("<br/>Document Index information(searchId=" + searchId + ") for book=" + instrument.getBook() 
					+ " page=" + instrument.getPage() + "; inst=" + instrument.getInstno() 
					+ " year=" + instrument.getYear() + " can't be taken from " + dataSite.getSiteTypeAbrev() + " because no results was found.<br/>", searchId);
		}
		return docItems;
	}

	/**
	 * @param req
	 */
	public String getResponse(String docNo, String book, String page, int yearInt, int monthInt) {
		
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			HTTPRequest req = new HTTPRequest(getBaseLink() + "/ori/search.do", HTTPRequest.POST);
			
						
			if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)){
				req.setPostParameter("instrumentNumber", "");
				req.setPostParameter("book", book);
				req.setPostParameter("page", page);
			} else if (StringUtils.isNotEmpty(docNo) && yearInt > SimpleChapterUtils.UNDEFINED_YEAR){
				String year = "";
				try {
					year = Integer.toString(yearInt);
				} catch (Exception e) {
					SearchLogger.info("<br/>Document Index information(searchId=" + searchId + ") for book=" + book 
							+ " page=" + page + "; inst=" + docNo 
							+ " year=" + year + " can't be taken from " + dataSite.getSiteTypeAbrev() + " because year is missing.<br/>", searchId);
				}
				docNo = prepareInstrumentNumberForRO2(year + "." + docNo, yearInt, monthInt, dataSite.getCountyIdAsString());
				req.setPostParameter("instrumentNumber", docNo);
				req.setPostParameter("book", "Book");
				req.setPostParameter("page", "Page");
			}
			
			req.setPostParameter("county", dataSite.getCountyIdAsString());
			req.setPostParameter("locationType", "COUNTY");
			req.setPostParameter("nametype", "i");
			req.setPostParameter("lastName", "last");
			req.setPostParameter("firstName", "first");
			req.setPostParameter("businessName", "business");
			req.setPostParameter("circuit", "500");
			req.setPostParameter("region", "500");
			req.setPostParameter("startMonth", "0");
			req.setPostParameter("startDay", "0");
			req.setPostParameter("startYear", "");
			req.setPostParameter("endMonth", "0");
			req.setPostParameter("endDay", "0");
			req.setPostParameter("endYear", "");
			req.setPostParameter("percisesearchtype", "i");
			req.setPostParameter("x", "36");
			req.setPostParameter("y", "14");
			
			return ((ro.cst.tsearch.connection.http2.FLGenericMFCConnRO2)site).process(req).getResponseAsString();
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			HttpManager.releaseSite(site);
		}
		return "";
	}
	
	public static String prepareInstrumentNumberForRO2(String instrDesc, int yearInt, int monthInt, String county) {
		String[] parts = instrDesc.split("\\.");
    	if (parts.length == 2){
    		String instrument = parts[1];
    		
    		if (CountyConstants.FL_Alachua_STRING.equals(county)){
    			instrDesc = parts[1];
    		} else if (CountyConstants.FL_Baker_STRING.equals(county)){
    			if (instrument.length() == 12){
    				return instrument;
    			} else{
    				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 8, "0");
    			}
    		} else if (CountyConstants.FL_Bay_STRING.equals(county)){
    			if (instrument.length() == 10){
    				return instrument;
    			} else{
    				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
    			}
    		} else if (CountyConstants.FL_Bradford_STRING.equals(county)){
    			if (yearInt > 2007){
    				if (instrument.length() == 12){
        				return instrument;
        			} else{
	    				String instr = parts[1];
	    				if (instr.startsWith("04")){
	    					instr = instr.replaceFirst("\\A04", "");
	    				}
	    				instrDesc = parts[0] + "04" + StringUtils.leftPad(instr, 6, "0");
        			}
    			} else {
    				if (instrument.length() == 8){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Citrus_STRING.equals(county)){
    			if (yearInt > 2002){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			} else {
    				instrDesc = parts[1];
    			}
    		} else if (CountyConstants.FL_Collier_STRING.equals(county)){
    			instrDesc = parts[1];
    		} else if (CountyConstants.FL_Columbia_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 1999){
    				if (instrument.length() == 9){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    					instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 7, "0");
	    				} else if (parts[0].length() == 2){
	    					instrDesc = parts[0] + StringUtils.leftPad(parts[1], 7, "0");
	    				}
        			}
    			} else if (yearInt == 2000){
    				instrDesc = parts[1];
    			} else if (yearInt > 2000 && yearInt < 2007){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			} else if (yearInt == 2007 && monthInt > -1 && monthInt < 6){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			} else if (yearInt == 2007 && monthInt > -1 && monthInt > 5){
    				if (instrument.length() == 12){
        				return instrument;
        			} else{
	    				String instr = parts[1];
	    				if (instr.startsWith("12")){
	    					instr = instr.replaceFirst("\\A12", "");
	    				}
	    				instrDesc = parts[0] + "12" + StringUtils.leftPad(instr, 6, "0");
        			}
    			} else if (yearInt > 2007){
    				if (instrument.length() == 12){
        				return instrument;
        			} else{
	    				String instr = parts[1];
	    				if (instr.startsWith("12")){
	    					instr = instr.replaceFirst("\\A12", "");
	    				}
	    				instrDesc = parts[0] + "12" + StringUtils.leftPad(instr, 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_DeSoto_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 2000){
    				instrDesc = parts[1];
    			} else if (yearInt > 2000 && yearInt < 2008){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			} else if (yearInt > 2008){
    				instrDesc = parts[0] + parts[1];
    			}
    		} else if (CountyConstants.FL_Dixie_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 2008){
    				instrDesc = parts[1];
    			} else if (yearInt > -1 && yearInt > 2007){
    				instrDesc = parts[0] + parts[1];
    			}
    		} else if (CountyConstants.FL_Duval_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 2000){
    				if (instrument.length() == 8){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    					instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 6, "0");
	    				} else if (parts[0].length() == 2){
	    					instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
	    				}
        			}
    			} else if (yearInt > 1999){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Escambia_STRING.equals(county)){
    			if (instrument.length() == 10){
    				return instrument;
    			} else{
    				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
    			}
    		} else if (CountyConstants.FL_Flagler_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 2000){
    				if (instrument.length() == 8){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    					instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 6, "0");
	    				} else if (parts[0].length() == 2){
	    					instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
	    				}
        			}
    			} else if (yearInt == 2000){
    				instrDesc = parts[1];
    			} else if (yearInt == 2001){
    				if (instrument.length() == 7){
        				return instrument;
        			} else{
        				instrDesc = "1" + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			} else if (yearInt > 2001){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Franklin_STRING.equals(county)){
    			if (yearInt > -1 && ((yearInt < 1993) || (yearInt > 1993 && yearInt < 2000))){
    				if (instrument.length() == 6){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    					instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 4, "0");
	    				} else if (parts[0].length() == 2){
	    					instrDesc = parts[0] + StringUtils.leftPad(parts[1], 4, "0");
	    				}
        			}
    			} else if ((yearInt == 1993) || (yearInt > 1999 && yearInt < 2007)){
    				if (instrument.length() == 9){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 5, "0");
        			}
    			} else if (yearInt == 2007){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			} else if (yearInt > 2007){
    				if (instrument.length() == 12){
        				return instrument;
        			} else{
	    				String instr = parts[1];
	    				if (instr.startsWith("19")){
	    					instr = instr.replaceFirst("\\A19", "");
	    				}
	    				instrDesc = parts[0] + "19" + StringUtils.leftPad(instr, 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Gadsden_STRING.equals(county)){
    			if (instrument.length() == 9){
    				return instrument;
    			} else{
		    		if (parts[0].length() == 4){
		    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 7, "0");
		    		} else if (parts[0].length() == 2){
		    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 7, "0");
		    		}
    			}
    		} else if (CountyConstants.FL_Gilchrist_STRING.equals(county)){
    			if (instrument.length() == 10){
    				return instrument;
    			} else{
	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
    			}
    		} else if (CountyConstants.FL_Glades_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 2007){
    				instrDesc = parts[1];
    			} else if (yearInt > -1 && yearInt > 2006){
    				if (instrument.length() == 12){
        				return instrument;
        			} else{
	    				String instr = parts[1];
	    				if (instr.startsWith("22")){
	    					instr = instr.replaceFirst("\\A22", "");
	    				}
	    				instrDesc = parts[0] + "22" + StringUtils.leftPad(instr, 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Gulf_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 2000){
    				if (instrument.length() == 6){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 4, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 4, "0");
	    	    		}
        			}
    			} else if (yearInt > -1 && ((yearInt > 1999 && yearInt < 2007) || (yearInt == 2007 && monthInt < 7))){
    				if (instrument.length() == 8 || instrument.length() == 12){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 4, "0");
        			}
    			} else if (yearInt > -1 && ((yearInt > 2007) || (yearInt == 2007 && monthInt > 6))){
    				if (instrument.length() == 12){
        				return instrument;
        			} else{
	    				String instr = parts[1];
	    				if (instr.startsWith("23")){
	    					instr = instr.replaceFirst("\\A23", "");
	    				}
	    				instrDesc = parts[0] + "23" + StringUtils.leftPad(instr, 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Hamilton_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 2010){
    				if (instrument.length() == 9){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 5, "0");
        			}
    			} else if (yearInt > -1 && yearInt > 2009){
    				if (instrument.length() == 12){
        				return instrument;
        			} else{
	    				String instr = parts[1];
	    				if (instr.startsWith("24")){
	    					instr = instr.replaceFirst("\\A24", "");
	    				}
	    				instrDesc = parts[0] + "24" + StringUtils.leftPad(instr, 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Hardee_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 1990){
    				if (instrument.length() == 6){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 4, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 4, "0");
	    	    		}
        			}
    			} else if (yearInt > -1 && yearInt > 1989 && yearInt < 2000){
    				if (instrument.length() == 7){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 5, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 5, "0");
	    	    		}
        			}
    			} else if (yearInt > -1 && yearInt > 1999){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Hendry_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 2010){
    				if (instrument.length() == 9){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 5, "0");
        			}
    			} else if (yearInt > -1 && yearInt > 2009){
    				if (instrument.length() == 12){
        				return instrument;
        			} else{
	    				String instr = parts[1];
	    				if (instr.startsWith("26")){
	    					instr = instr.replaceFirst("\\A26", "");
	    				}
	    	    		instrDesc = parts[0] + "26" + StringUtils.leftPad(instr, 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Hernando_STRING.equals(county)){
    			if (instrument.length() == 10){
    				return instrument;
    			} else{
    				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
    			}
    		} else if (CountyConstants.FL_Highlands_STRING.equals(county)){
    			if (yearInt > -1 && ((yearInt < 1983) || (yearInt > 2005))){
    				instrDesc = parts[1];
    			} else if (yearInt > -1 && yearInt > 1982 && yearInt < 2006){
    				if (instrument.length() == 9){
        				return instrument;
        			} else{
        				instrDesc = StringUtils.leftPad(parts[1], 9, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Hillsborough_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 2000){
    				if (instrument.length() == 8){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 6, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
	    	    		}
        			}
    			} else if (yearInt > 1999){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Holmes_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 1984){
    				if (instrument.length() == 9){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 5, "0");
        			}
    			} else if (yearInt > -1  && yearInt > 1993 && yearInt < 2008){
    				instrDesc = parts[1];
    			} else if (yearInt > 2007){
    				if (instrument.length() == 12){
        				return instrument;
        			} else{
	    				String instr = parts[1];
	    				if (instr.startsWith("30")){
	    					instr = instr.replaceFirst("\\A30", "");
	    				}
	    				instrDesc = parts[0] + "30" + StringUtils.leftPad(instr, 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Jackson_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 2000){
    				if (instrument.length() == 8){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 6, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
	    	    		}
        			}
    			} else if (yearInt == 2000){
    				instrDesc = parts[1];
    			} else if (yearInt == 2001){
    				if (instrument.length() == 7){
        				return instrument;
        			} else{
        				instrDesc = "1" + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			} else if (yearInt > -1 && yearInt > 2001 && yearInt < 2010){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			} else if (yearInt > 2009){
    				if (instrument.length() == 12){
        				return instrument;
        			} else{
	    				String instr = parts[1];
	    				if (instr.startsWith("32")){
	    					instr = instr.replaceFirst("\\A32", "");
	    				}
	    				instrDesc = parts[0] + "32" + StringUtils.leftPad(instr, 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Jefferson_STRING.equals(county)){
    			if (yearInt > -1 && (yearInt < 1973 || yearInt > 2007)){
    				if (instrument.length() == 12){
        				return instrument;
        			} else{
	    				String instr = parts[1];
	    				if (instr.startsWith("33")){
	    					instr = instr.replaceFirst("\\A33", "");
	    				}
	    				instrDesc = parts[0] + "33" + StringUtils.leftPad(instr, 6, "0");
        			}
    			} else if (yearInt > -1 && yearInt > 1972 && yearInt < 2008){
    				instrDesc = parts[1];
    			}
    		} else if (CountyConstants.FL_Lafayette_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 1984){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 8, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 8, "0");
	    	    		}
        			}
    			} else if (yearInt > -1 && ((yearInt > 1983 && yearInt < 1997) || (yearInt > 2001 && yearInt < 2007))){
    				if (instrument.length() == 8){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 6, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
	    	    		}
        			}
    			} else if (yearInt > -1 && yearInt > 1996 && yearInt < 2000){
    				if (instrument.length() == 9){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 7, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 7, "0");
	    	    		}
        			}
    			} else if (yearInt == 2000){
    				instrDesc = parts[1];
    			} else if (yearInt > 2006){
    				if (instrument.length() == 12){
        				return instrument;
        			} else{
	    				String instr = parts[1];
	    				if (instr.startsWith("34")){
	    					instr = instr.replaceFirst("\\A34", "");
	    				}
	    				instrDesc = parts[0] + "34" + StringUtils.leftPad(instr, 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Lake_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 2000){
    				if (instrument.length() == 8){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 6, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
	    	    		}
        			}
    			} else if (yearInt > 1999){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Lee_STRING.equals(county)){
    			if (yearInt > -1 && ((yearInt < 2005) || (yearInt == 2005 && monthInt < 8))){
    	    		instrDesc = parts[1];
    			} else if (yearInt > 2005 || (yearInt == 2005 && monthInt > 7)){
    				if (instrument.length() == 13){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 9, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Leon_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 1975){
    	    		instrDesc = parts[1];
    			} else if (yearInt > 1975){
    				if (instrument.length() == 11){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 7, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Levy_STRING.equals(county)){
    			instrDesc = parts[1];
    		} else if (CountyConstants.FL_Liberty_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 2000){
    				if (instrument.length() == 6){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 4, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 4, "0");
	    	    		}
        			}
    			} else if (yearInt > 1999){
    				if (instrument.length() == 8){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 4, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Madison_STRING.equals(county)){
    			if (instrument.length() == 12){
    				return instrument;
    			} else{
    				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 8, "0");
    			}
    		} else if (CountyConstants.FL_Manatee_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 1997){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			} else if (yearInt > 1996){
    				if (instrument.length() == 11){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 7, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Marion_STRING.equals(county)){
    			if (instrument.length() == 10){
    				return instrument;
    			} else{
    				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
    			}
    		} else if (CountyConstants.FL_Martin_STRING.equals(county)){
    			if (instrument.length() == 6){
    				return instrument;
    			} else{
    				instrDesc = StringUtils.leftPad(parts[1], 6, "0");
    			}
    		} else if (CountyConstants.FL_Nassau_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 2000){
    				if (instrument.length() == 7){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 5, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 5, "0");
	    	    		}
        			}
    			} else if (yearInt > 1999){
    				if (instrument.length() == 9){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 5, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Okaloosa_STRING.equals(county)){
    			instrDesc = parts[1];
    		} else if (CountyConstants.FL_Okeechobee_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 2000){
    				if (instrument.length() == 9){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 7, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 7, "0");
	    	    		}
        			}
    			} else if (yearInt > 2000 && yearInt > -1 && yearInt < 2004){
    				instrDesc = parts[1];
    			} else if (yearInt > 2003){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Osceola_STRING.equals(county)){
    			if (yearInt > -1 && yearInt < 2000){
    				if (instrument.length() == 8){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 6, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
	    	    		}
        			}
    			} else if (yearInt > 1999){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Palm_Beach_STRING.equals(county)){
    			if (instrument.length() == 11){
    				return instrument;
    			} else{
    				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 7, "0");
    			}
    		} else if (CountyConstants.FL_Pasco_STRING.equals(county)){
    			if (instrument.length() == 10){
    				return instrument;
    			} else{
    				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
    			}
    		} else if (CountyConstants.FL_Pinellas_STRING.equals(county)){
    			if (yearInt < 2001 || (yearInt == 2001 && monthInt < 5)){
    				if (instrument.length() == 14 && instrument.startsWith("BP")){
        				return instrument;
        			} else{
        				instrDesc = "BP" + StringUtils.leftPad(parts[1], 14, "0");
        			}
    			} else if (yearInt == 2002 || (yearInt == 2001 && monthInt > 4) || (yearInt == 2003 && monthInt > -1 && monthInt < 10)){
    				if (instrument.length() == 16){
        				return instrument;
        			} else{
        				instrDesc = StringUtils.leftPad(parts[1], 16, "0");
        			}
    			} else if (yearInt > 2003 || (yearInt == 2003 && monthInt > 9)){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Polk_STRING.equals(county)){
    			if (yearInt < 2000 && yearInt > -1){
    				if (instrument.length() == 8){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 6, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
	    	    		}
        			}
    			} else if (yearInt > 1999){
    				if (instrument.length() == 6){
        				return instrument;
        			} else{
        				instrDesc = StringUtils.leftPad(parts[1], 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Putnam_STRING.equals(county)){
    			if ((yearInt < 2007 && yearInt > -1) || (yearInt == 2007 && monthInt < 10)){
    	    		instrDesc = parts[1];
    			} else if ((yearInt > 2007) || (yearInt == 2007 && monthInt > 9)){
    				if (instrument.length() == 12){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 8, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Santa_Rosa_STRING.equals(county)){
    			if ((yearInt < 1984 && yearInt > -1) || (yearInt == 1984 && monthInt > 4)){
    	    		instrDesc = parts[1];
    			} else if ((yearInt > 1984 && yearInt > -1 && yearInt < 2000) || (yearInt == 1984 && monthInt < 5)){
    				if (instrument.length() == 9){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 7, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 7, "0");
	    	    		}
        			}
    			} else if (yearInt > 1999){
    				if (instrument.length() == 9){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 5, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Sarasota_STRING.equals(county)){
    			if ((yearInt < 1998 && yearInt > -1) || (yearInt == 1998 && monthInt <6)){
    				if (instrument.length() == 8){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 6, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
	    	    		}
        			}
    			} else if ((yearInt > 1998) || (yearInt == 1998 && monthInt < 5)){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_St_Johns_STRING.equals(county)){
    			if (yearInt < 1990 && yearInt > -1){
    				if (instrument.length() == 7){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 5, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 5, "0");
	    	    		}
        			}
    			} else if ((yearInt > 1989 && yearInt < 2005 && yearInt > -1)){
    				if (instrument.length() == 8){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 6, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
	    	    		}
        			}
    			} else if (yearInt > 2004){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_St_Lucie_STRING.equals(county)){
    			instrDesc = parts[1];
    		} else if (CountyConstants.FL_Sumter_STRING.equals(county)){
    			if (instrument.length() == 12){
    				return instrument;
    			} else{
    				instrDesc = parts[0] + "6" + StringUtils.leftPad(parts[1], 7, "0");
    			}
    		} else if (CountyConstants.FL_Suwannee_STRING.equals(county)){
    			if (yearInt < 2008 && yearInt > -1){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 8, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 8, "0");
	    	    		}
        			}
    			} else if (yearInt > 2007){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			}
    		} else if (CountyConstants.FL_Taylor_STRING.equals(county)){
    			if ((yearInt < 1996 && yearInt > -1) || yearInt == 2000){
    				if (instrument.length() == 9){
        				return instrument;
        			} else{
        				instrDesc = StringUtils.leftPad(parts[1], 9, "0");
        			}
    			} else if ((yearInt > 1995 && yearInt < 2000 && yearInt > -1) || yearInt > 2000){
    				if (instrument.length() == 9){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 7, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 7, "0");
	    	    		}
        			}
    			}
    		} else if (CountyConstants.FL_Union_STRING.equals(county)){
    			if (instrument.length() == 11){
    				return instrument;
    			} else{
    				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 7, "0");
    			}
    		} else if (CountyConstants.FL_Volusia_STRING.equals(county)){
    			if (instrument.length() == 10){
    				return instrument;
    			} else{
    				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
    			}
    		} else if (CountyConstants.FL_Wakulla_STRING.equals(county)){
    			if (yearInt < 2006 && yearInt > -1){
    				if (instrument.length() == 7){
        				return instrument;
        			} else{
        				instrDesc = StringUtils.leftPad(parts[1], 7, "0");
        			}
    			} else if (yearInt > 2005){
    				instrDesc = parts[1];
    			}
    		} else if (CountyConstants.FL_Walton_STRING.equals(county)){
    			instrDesc = parts[1];
    		} else if (CountyConstants.FL_Washington_STRING.equals(county)){
    			if (yearInt < 1997 && yearInt > -1){
    				if (instrument.length() == 8){
        				return instrument;
        			} else{
	    				if (parts[0].length() == 4){
	    	    			instrDesc = parts[0].substring(2) + StringUtils.leftPad(parts[1], 6, "0");
	    	    		} else if (parts[0].length() == 2){
	    	    			instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
	    	    		}
        			}
    			} else if (yearInt > 1996 && yearInt < 2008 && yearInt > -1){
    				if (instrument.length() == 10){
        				return instrument;
        			} else{
        				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 6, "0");
        			}
    			} else if (yearInt > 2007){
    				if (instrument.length() == 12){
        				return instrument;
        			} else{
	    				String instr = parts[1];
	    				if (instr.startsWith("67")){
	    					instr = instr.replaceFirst("\\A67", "");
	    				}
	    				instrDesc = parts[0] + "67" + StringUtils.leftPad(instr, 6, "0");
        			}
    			}
    		} else{
    			if (instrument.length() == 11){
    				return instrument;
    			} else{
    				instrDesc = parts[0] + StringUtils.leftPad(parts[1], 7, "0");
    			}
    		}
    	}
    	
    	return instrDesc;
	}
	
	public FLGenericDTG isCountyAvailableOnDTG() {

		FLGenericDTG flGenericDTG = null;
		try {
			long siteId = TSServersFactory.getSiteId(getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV),
					getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY_NAME),
					"DG");
			DataSite data = null;

			try {
				data = HashCountyToIndex.getDateSiteForMIServerID(InstanceManager.getManager().getCommunityId(searchId), siteId);
			} catch (Exception e) {
				logger.error("Cannot obtain datasite for DG - it's not available for county:" + dataSite.getCountyName() + "; searchid=" + searchId);
			}
			if (data != null) {
				flGenericDTG = (FLGenericDTG) TSServersFactory.GetServerInstance((int) siteId, getSearch().getID());
			}
		} catch (Exception e) {
			logger.error("DG is not available for county:" + dataSite.getCountyName() + "; searchid=" + searchId);
		}

		return flGenericDTG;
	}
	
	@Override
	public void addDocumentAdditionalPostProcessing(DocumentI doc, ServerResponse response) {
		super.addDocumentAdditionalPostProcessing(doc, response);

		DocumentsManagerI manager = getSearch().getDocManager();
		try {
			manager.getAccess();
			if (manager.contains(doc)) {
				if (doc instanceof RegisterDocumentI) {
					RegisterDocumentI regDoc = (RegisterDocumentI) doc;
					
					if (!regDoc.hasImage() && regDoc.isNotOneOf(DocumentTypes.COURT)) {
						StringBuilder toLog = new StringBuilder();
						try {
							TSInterface tsi = TSServersFactory.GetServerInstance((int) doc.getSiteId(), searchId);
							DownloadImageResult res = tsi.lookupForImage(doc);
							if (res.getStatus() != DownloadImageResult.Status.OK) {
								if (doc.getImage() != null) {
									doc.getImage().setSaved(false);
								}
								toLog.append("<br>Image of document with following instrument number was not successfully retrieved: ")
										.append(doc.prettyPrint());
							} else {
								doc.getImage().setSaved(true);
								toLog.append("<br>Image of document with following instrument number was successfully retrieved: <a href='")
										.append(doc.getImage().getSsfLink())
										.append("'>")
										.append(doc.prettyPrint())
										.append("</a>");
							}
						} catch (Exception e) {
							doc.getImage().setSaved(false);
							toLog.append("<br>Image of document with following instrument number was not successfully retrieved:")
									.append(doc.prettyPrint());
							doc.setImage(null);
							logger.error("performAdditionalProcessingAfterRunningAutomatic", e);
						}
						if (toLog.length() > 0) {
							toLog.append("<br>");
							SearchLogger.info(toLog.toString(), searchId);
						}
					}
				}
			}
		} catch (Throwable t) {
			logger.error("Error while post processing document", t);
		} finally {
			manager.releaseAccess();
		}
	}
	
	 @Override
	public DownloadImageResult lookupForImage(DocumentI doc, String documentId) throws ServerResponseException {
		try {
			if (doc != null) {
				InstrumentI i = doc.getInstrument();
				getSearch();
				ImageI image = doc.getImage();
				boolean docWithoutImage = false;
				if (image == null) {
					docWithoutImage = true;
					image = new Image();

					String imageExtension = "tiff";
					if (StringUtils.isNotEmpty(image.getExtension())) {
						imageExtension = image.getExtension();
					}

					String imageDirectory = getSearch().getImageDirectory();
					ro.cst.tsearch.utils.FileUtils.CreateOutputDir(imageDirectory);
					String fileName = doc.getId() + "." + imageExtension;
					String path = imageDirectory + File.separator + fileName;
					if (StringUtils.isEmpty(image.getPath())) {
						image.setPath(path);
					}
					image.setFileName(fileName);
					image.setContentType("IMAGE/TIFF");
					image.setExtension("tiff");
					image.setType(IType.TIFF);
					doc.setImage(image);
					doc.setIncludeImage(true);
				}

				String imageLink = CreatePartialLink(TSConnectionURL.idGET) + "look_for_dt_image&id=&description=&instr=" + i.getInstno()
						+ "&book=" + i.getBook() + "&page=" + i.getPage() + "&year=" + i.getYear();
				if (i.getDate() != null) {
					String date = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(i.getDate());
					if (StringUtils.isNotEmpty(date)) {
						String month = date.substring(0, 2);
						if (StringUtils.isNotEmpty(month)) {
							imageLink += "&month=" + month;
						}
						String day = date.substring(date.indexOf("/") + 1, date.indexOf("/") + 3);
						if (StringUtils.isNotEmpty(day)) {
							imageLink += "&day=" + day;
						}
					}
				}
				Set<String> links = image.getLinks();
				if (links.size() == 0) {
					links.add(imageLink);
					image.setLinks(links);
				}

				DownloadImageResult dldImageResult = downloadImage(image, doc.getId(), doc);
				if (dldImageResult.getStatus().equals(DownloadImageResult.Status.ERROR) && docWithoutImage) {
					doc.setImage(null);
				}

				return dldImageResult;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], "");
	}
	
	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data) {
		if (StringUtils.isEmpty(instrumentNo)) {
			return false;
		}
		
		boolean isAlreadySaved = super.isInstrumentSaved(instrumentNo, documentToCheck, data);
		if (!isAlreadySaved) {// check if doc already saved on DG or ATI
			DocumentsManagerI documentManager = getSearch().getDocManager();
			try {
				documentManager.getAccess();
				InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
				if (data != null) {
					String docCateg = "";
					if (!StringUtils.isEmpty(data.get("type"))) {
						String serverDocType = data.get("type");
						docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
						instr.setDocType(docCateg);
					}

					// remove year and remaining leading zeros from instr
					String year = StringUtils.defaultString(data.get("year"));
					String instNoEquiv = "";

					if (instrumentNo.length() >= year.length() && year.equals(instrumentNo.substring(0, year.length()))) {
						instNoEquiv = instrumentNo.substring(4).replaceFirst("^0+", "");
					} else if (instrumentNo.length() >= 2 && year.endsWith(instrumentNo.substring(0, 2))) {
						instNoEquiv = instrumentNo.substring(2).replaceFirst("^0+", "");
					}
					if (!instNoEquiv.isEmpty()) {
						instr.setInstno(instNoEquiv);
					}

					instr.setBook(data.get("book"));
					instr.setPage(data.get("page"));
					instr.setDocno(data.get("docno"));

					instr.setYear(Integer.parseInt(year));

					List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);

					if (StringUtils.isNotEmpty(docCateg)) {
						for (DocumentI documentI : almostLike) {
							if (documentI.getDocType().equals(docCateg)) {
								return true;
							}
						}
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
	
	@Override
	protected void setCertificationDate() {
		try {
			
			if (!CertificationDateManager.isCertificationDateInCache(dataSite)){
				
				HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
				HTTPRequest req = new HTTPRequest(getDataSite().getServerHomeLink() + "/ori/dateRange.do?search=COUNTY");
				HTTPResponse res = null;
				try {
					res = site.process(req);
					
					if(res != null && res.getReturnCode() == HttpStatus.SC_OK && res.getContentType().contains("text/html")) {
						HtmlParser3 parser = new HtmlParser3(res.getResponseAsString());
						TableTag table = (TableTag)parser.getNodeById("county_range");
						TableColumn absoluteCell = HtmlParser3.getAbsoluteCell(0, 2, HtmlParser3.findNode(table.getChildren(), getDataSite().getCountyName().toUpperCase()), false);
						
						Date date = ro.cst.tsearch.generic.Util.dateParser3(absoluteCell.toPlainTextString().trim());
						if (date != null) {
							CertificationDateManager.cacheCertificationDate(dataSite, absoluteCell.toPlainTextString().trim());
							CertificationDateDS certificationDateDS = new CertificationDateDS(date, getDataSite().getSiteTypeInt());
							certificationDateDS.setSkipInCalculation(true);
							certificationDateDS.setType(CDType.GI);
							getSearch().getSa().updateCertificationDateObject(certificationDateDS, dataSite);
						}
						
					} else {
						logger.error("Cannot setCertificationDate because unexpected result for searchId " + searchId);	
					}
					
				} catch (Exception e) {
					logger.error("Cannot setCertificationDate for searchId " + searchId, e);
				} finally {
					HttpManager.releaseSite(site);
				}
				
			} else {
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				
				CertificationDateDS certificationDateDS = new CertificationDateDS(ro.cst.tsearch.generic.Util.dateParser3(date), getDataSite().getSiteTypeInt());
				certificationDateDS.setSkipInCalculation(true);
				certificationDateDS.setType(CDType.GI);
				getSearch().getSa().updateCertificationDateObject(certificationDateDS, dataSite);
				
			}

        } catch (Exception e) {
        	CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName(), e);
        }
	}
	
}