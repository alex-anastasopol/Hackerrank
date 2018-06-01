package ro.cst.tsearch.servers.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class OHFairfieldPR extends TSServer {
		
	private static final long	serialVersionUID	= -6957915556240901493L;

	public OHFairfieldPR(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	public OHFairfieldPR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
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
		case ID_SEARCH_BY_MODULE38:			//File Date Search
			
			if (rsResponse.contains("No Matches Displayed")) {
				Response.getParsedResponse().setError(NO_DATA_FOUND);
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
			StringBuilder date = new StringBuilder();
			String details = getDetails(Response, serialNumber, caseType, date);
			String filename = serialNumber + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("dataSource", getDataSite().getSiteTypeAbrev());
				data.put("type", caseType.toString());
				data.put("date", date.toString());
				if (isInstrumentSaved(serialNumber.toString(), null, data, false)){
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);
				
			} else {
				smartParseDetails(Response,details);
								
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
				Response.getParsedResponse().setResponse(details);
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				
			}
			break;
		
		case ID_GET_LINK :
			ParseResponse(sAction, Response, ID_DETAILS);
			break;	
			
		default:
			break;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void getCaseNoTypeDate(ServerResponse Response, StringBuilder instrNumber, StringBuilder caseType, StringBuilder date, StringBuilder grantor, StringBuilder grantee) {
		String query = Response.getQuerry();
		if (!StringUtils.isEmpty(query)) {
			String seq = StringUtils.extractParameter(query, "seq=([^&?]*)");
			if (!StringUtils.isEmpty(seq)) {
				Map<String,String> addParams = (Map<String,String>)mSearch.getAdditionalInfo(getCurrentServerName() + ":params:" + seq); 
				if (addParams!=null) {
					instrNumber.append(org.apache.commons.lang.StringUtils.defaultString((String)addParams.get(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName())));
					caseType.append(org.apache.commons.lang.StringUtils.defaultString((String)addParams.get(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName())));
					date.append(org.apache.commons.lang.StringUtils.defaultString((String)addParams.get(SaleDataSetKey.RECORDED_DATE.getShortKeyName())));
					grantor.append(org.apache.commons.lang.StringUtils.defaultString((String)addParams.get(SaleDataSetKey.GRANTOR.getShortKeyName())));
					grantee.append(org.apache.commons.lang.StringUtils.defaultString((String)addParams.get(SaleDataSetKey.GRANTEE.getShortKeyName())));
				}		
			}
		}
	}
	
	protected String getDetails(ServerResponse Response, StringBuilder instrNumber, StringBuilder caseType, StringBuilder date) {
		try {
			
			String rsResponse = Response.getResult();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
									
			/* If from memory - use it as is */
			if(!rsResponse.toLowerCase().contains("<html")) {
				getCaseNoTypeDate(Response, instrNumber, caseType, date, new StringBuilder(), new StringBuilder());
				return rsResponse;
			}
			
			getCaseNoTypeDate(Response, instrNumber, caseType, date, new StringBuilder(), new StringBuilder());
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "3"));
			
			String details = "";
			
			if (tables.size()>1) {
				details = tables.elementAt(1).toHtml();
				
				//headers
				details = details.replaceAll("(?is)(<td[^>]+)class=\"section([^>]*>)", "$1bgcolor=\"#7DA7D9\"$2");
				
				//copyright line
				details = details.replaceAll("(?is)<tr>\\s*<td[^>]+colspan=\"2\"[^>]*>\\s*<p[^>]*>.*?</p>\\s*</td>\\s*</tr>", "");
				
				details = "<table width=\"600\"><tr><td>" + details + "</td></tr></table>";
				
			}
			
			String patt = "(?is)<tr>\\s*<td[^>]*>\\s*<a[^>]+href=\"([^\"]+)\"[^>]*>Click for Docket Entries</a>\\s*</td>\\s*</?tr>";
			Matcher ma = Pattern.compile(patt).matcher(details);
			if (ma.find()) {
				String link = ma.group(1);
				details = details.replaceFirst(patt, "");
				String docket = getLinkContents(getDataSite().getServerHomeLink() + link.replaceFirst("^/", ""));
				org.htmlparser.Parser htmlParser2 = org.htmlparser.Parser.createParser(docket, null);
				NodeList nodeList2 = htmlParser2.parse(null);
				NodeList tables2 = nodeList2.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "3"));
				if (tables2.size()>1) {
					String docketDetasilsString = tables2.elementAt(1).toHtml();
					docketDetasilsString = docketDetasilsString.replaceFirst("(?is)(<td[^>]+)class=\"text2([^>]*>)", "$1bgcolor=\"#DDDDFF\"$2");
					docketDetasilsString = docketDetasilsString.replaceAll("(?is)<tr>\\s*<td[^>]+colspan=\"2\"[^>]*>\\s*<p[^>]*>.*?</p>\\s*</td>\\s*</tr>", "");
					docketDetasilsString = docketDetasilsString.replaceFirst("(?is)<tr>\\s*<td[^>]*>\\s*<a[^>]+href=\"([^\"]+)\"[^>]*>Click for Case Information</a>\\s*</td>\\s*</tr>", "");
					details += "<table width=\"600\"><tr><td><h3>Docket Entries</h3s></td></tr><tr><td>" + docketDetasilsString + "</td></tr></table>";
				}
			}
			
			details = details.replaceAll("(?is)<a[^>]+href=[^>]*>([^<]*)</a>", "$1");
			
			return details;
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	public static String extractCaseNoFromInterm(String column) {
		String result = "";
		String[] brs = column.split("(?is)<br>");
		Matcher ma1 = Pattern.compile("(?is)\\bCase:.*?<a[^<]+href=\"[^\"]+sub=([^\"&]*)[^<]+type=([^\"&]*)[^<]*>([^<]*)</a>").matcher(brs[0]);
		if (ma1.find()) {
			return ma1.group(3).trim();
		} else {
			Matcher ma2 = Pattern.compile("(?is)\\bCase:(.*)").matcher(brs[0]);
			if (ma2.find()) {
				return ma2.group(1).replaceAll("<.*?>", "").replaceAll("(?is)\\([^)]+\\)", "").trim();
			}
		}
		return result;
	}
	
	public static String extractCaseTypeFromInterm(String column) {
		Matcher ma = Pattern.compile("(?i)\\bCase\\s+Type:(.*)").matcher(column);
		if (ma.find()) {
			String res = ma.group(1).replaceAll("<.*?>", "").trim();
			if (res.equalsIgnoreCase("Miscellaneous")||res.equalsIgnoreCase("Civil")) {
				res+= "Probate";
			}
			return res;
		}
		return "";
	}
	
	public static String extractDateFromInterm(String column) {
		Matcher ma = Pattern.compile("(?i)\\bFiled:(.*)").matcher(column);
		if (ma.find()) {
			return ma.group(1).replaceAll("<.*?>", "").trim().replaceAll("/", "-");
		}
		return "";
	}
		
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		int numberOfUncheckedElements = 0;
		
		try {
			TableTag resultsTable = null;
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList tableList1 = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "600"), true);
			if (tableList1.size()>1) {
				NodeList tableList2 = tableList1.elementAt(1).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true);
				if (tableList2.size()>0) {
					resultsTable = (TableTag)tableList2.elementAt(0);
				}
			}
			
			if (resultsTable!=null && resultsTable.getRowCount()>1) {
				
				Map<String, String> rowsMap = new  LinkedHashMap<String, String>();
				
				TableRow[] rows  = resultsTable.getRows();
				
				int len = rows.length;
				for (int i=0;i<len;i++) {
					TableRow row = rows[i];
					if (row.getColumnCount()>2) {
						String col1 = row.getColumns()[1].toHtml();
						String col2 = row.getColumns()[2].toHtml();
						String caseType = extractCaseTypeFromInterm(col2).toLowerCase();
						String key = extractCaseNoFromInterm(col2) + "_" + caseType + "_" + extractDateFromInterm(col1);
						String value = rowsMap.get(key);  
						if (value!=null) {
							if (!"marriage".equals(caseType)) {
								value = updateRow(value, row.toHtml(), "Concerning");
								value = updateRow(value, row.toHtml(), "Also");
								rowsMap.put(key, value);
							}
							continue;
						} else {
							rowsMap.put(key, row.toHtml());
						}
					}
				}
				
				int i=1;
				Iterator<Entry<String, String>> it = rowsMap.entrySet().iterator();
				while (it.hasNext()) {
				
					Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
					String value = pair.getValue();
					String[] cols = value.split("(?is)</?td>");
					String col2 = cols[2];
					
					ResultMap m = ro.cst.tsearch.servers.functions.OHFairfieldPR.parseIntermediaryRow(value);
					m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
					String instrNo = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()));
					String type = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName()));
					String date = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.RECORDED_DATE.getKeyName()));
					String grantor = org.apache.commons.lang.StringUtils.defaultString((String)m.get("tmpGrantor"));
					String grantee = org.apache.commons.lang.StringUtils.defaultString((String)m.get("tmpGrantee"));
					m.removeTempDef();
					
					String detailsLink = "";
					String[] brs = col2.split("(?is)<br>");
					Matcher ma2 = Pattern.compile("(?is)<a[^>]+href=\"([^\"]+)\"[^>]*>").matcher(brs[0]);
					if (ma2.find()) {
						Map<String, String> params = new HashMap<String, String>();
						params.put(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), instrNo);
						params.put(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(), type);
						params.put(SaleDataSetKey.RECORDED_DATE.getShortKeyName(), date);
						params.put(SaleDataSetKey.GRANTOR.getShortKeyName(), grantor);
						params.put(SaleDataSetKey.GRANTEE.getShortKeyName(), grantee);
						int seq = getSeq();
						mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
						detailsLink = CreatePartialLink(TSConnectionURL.idGET) + "/cgi-bin/" + ma2.group(1) + "&seq=" + seq;
					}
					
					String htmlRow = value;
					htmlRow = htmlRow.replaceAll("(?is)<td[^>]+>", "<td>");
					htmlRow = htmlRow.replaceAll("(?is)\\b(Docket:.*?)<a[^>]+href=\"[^\"]+\"[^>]*>[^<]*</a>", "$1");
					htmlRow = htmlRow.replaceFirst("(?is)<a[^>]+href=\"[^\"]+\"[^>]*>([^<]*)</a>", "<a href=\"" + detailsLink + "\">$1</a>");
					
					String rowType = "1";
					if (i%2==0) {
						rowType = "2";
					}
						
					ParsedResponse currentResponse = new ParsedResponse();
							
					currentResponse.setPageLink(new LinkInPage(detailsLink, detailsLink, TSServer.REQUEST_SAVE_TO_TSD));
					
					String checkBox = "checked";
					
					if ("".equals(detailsLink)) {		//no details
						checkBox = "&nbsp;";
					} else {
						HashMap<String, String> data = new HashMap<String, String>();
						data.put("dataSource", getDataSite().getSiteTypeAbrev());
						data.put("type", type);
						data.put("date", date);
							
						if (isInstrumentSaved(instrNo, null, data, false)) {
							checkBox = "saved";
						} else {
							numberOfUncheckedElements++;
							LinkInPage linkInPage = new LinkInPage(detailsLink, detailsLink, TSServer.REQUEST_SAVE_TO_TSD);
							checkBox = "<input type='checkbox' name='docLink' value='" + detailsLink + "'>";
							currentResponse.setPageLink(linkInPage);
						}
					}
						
					htmlRow = htmlRow.replaceFirst("(?is)<tr[^>]*>\\s*<td>.*?</td>(.*)", "<tr class=\"row" + rowType + "\"><td align=\"center\">" + checkBox + "</td>$1");
											
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
					currentResponse.setOnlyResponse(htmlRow);
						 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
						
					DocumentI document = (RegisterDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
					
					i++;
					
				}	
								
				String headerRow =  "<tr bgcolor=\"#7DA7D9\">" +
									"<td align=\"center\">" + SELECT_ALL_CHECKBOXES + "</td>" +
									"<td><b>&nbsp;</b></td>" +
									"<td><b>&nbsp;</b></td>";
				
				String header = "<table width=\"600\">" + headerRow;
				String footer = "</table>";
				
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter(footer);
				
				outputTable.append(table);
				SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			}

		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}

		return intermediaryResponse;
	}
	
	public static String updateRow(String row, String newText, String label) {
		String patt = "(?is)<b>\\s*" + label + ":\\s*</b>(.*?)<br>";
		List<String> matches1 = RegExUtils.getMatches(patt, row, 1);
		List<String> matches2 = new ArrayList<String>();  
		for (String s: matches1) {
			matches2.add(s.trim());
		}
		Matcher ma = Pattern.compile(patt).matcher(newText);
		if (ma.find()) {
			String gr1 = ma.group(1);
			if (!"".equals(gr1.trim())) {
				if (!matches2.contains(gr1)) {		//name not already added
					String valueLower = row.toLowerCase();
					int idx1 = valueLower.lastIndexOf(label.toLowerCase());
					if (idx1>-1) {
						int idx2 = valueLower.indexOf("<br>", idx1);
						if (idx2>-1) {
							idx2+= "<br>".length();
							row = row.substring(0, idx2) + "\r\n" + ma.group(0) + row.substring(idx2);
						}
					}
				}
			}
		}
		return row;

	}
	
	public static void addName(String patt, String details, StringBuilder sb, boolean isList) {
		if (isList) {
			List<String> list = RegExUtils.getMatches(patt, details, 1);
			for (String s: list) {
				s = s.trim();
				if (!"".equals(s)) {
					sb.append(s).append(" / ");
				}
			}
		} else {
			String s = RegExUtils.getFirstMatch(patt, details, 1);
			s = s.trim();
			if (!"".equals(s)) {
				sb.append(s).append(" / ");
			}
		}
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,	String detailsHtml, ResultMap resultMap) {
				
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
			
		StringBuilder instrNumber = new StringBuilder();
		StringBuilder caseType = new StringBuilder();
		StringBuilder date = new StringBuilder();
		StringBuilder gtr = new StringBuilder();
		StringBuilder gte = new StringBuilder();
		getCaseNoTypeDate(response, instrNumber, caseType, date, gtr, gte);
		
		StringBuilder grantor = new StringBuilder();
		StringBuilder grantee = new StringBuilder();
			
		detailsHtml = detailsHtml.replaceAll("(?is)<!--.*?-->", "");
			
		//Marriage
		addName("(?is)\\bGroom:\\s*</b>(.*?)<br>", detailsHtml, grantor, false);
		addName("(?is)\\bBride:\\s*</b>(.*?)<br>", detailsHtml, grantor, false);
			
		//Estate
		addName("(?is)\\bDecedent:\\s*</b>(.*?)<br>", detailsHtml, grantor, false);
		addName("(?is)\\bFiduciary\\s+#\\d+:\\s*</b>(.*?)<br>", detailsHtml, grantor, true);
			
		//Miscellaneous
		addName("(?is)\\bConcerning:\\s*</b>(.*?)<br>", detailsHtml, grantor, false);
		addName("(?is)\\bFormer\\s+Name:\\s*</b>(.*?)<br>", detailsHtml, grantor, false);
		addName("(?is)\\bApplicant:\\s*</b>(.*?)<br>", detailsHtml, grantor, false);
			
		//Minor's Settlement
		addName("(?is)\\bWard:\\s*</b>(.*?)<br>", detailsHtml, grantor, false);
		addName("(?is)\\bGuardian:\\s*</b>(.*?)<br>", detailsHtml, grantor, false);
			
		//Civil
		addName("(?is)\\bPlaintiff:\\s*</b>(.*?)<br>", detailsHtml, grantor, false);
		addName("(?is)\\bDefendant:\\s*</b>(.*?)<br>", detailsHtml, grantee, false);
			
		String grantorString = grantor.toString();
		String granteeString = grantee.toString();
			
		String split1[] = grantorString.split(" / ");
		String split2[] = gtr.toString().split(" / ");
		List<String> list1 = Arrays.asList(split1);
		for (String s: split2) {
			if (!list1.contains(s)) {
				grantor.append(s).append(" / ");
			}
		}
		
		String split3[] = granteeString.split(" / ");
		String split4[] = gte.toString().split(" / ");
		List<String> list2 = Arrays.asList(split3);
		for (String s: split4) {
			if (!list2.contains(s)) {
				grantee.append(s).append(" / ");
			}
		}
		grantorString = grantor.toString().replaceFirst(" / $", "");
		granteeString = grantee.toString().replaceFirst(" / $", "");
		
		resultMap.put("tmpGrantor", grantorString);
		resultMap.put("tmpGrantee", granteeString);
			
		resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNumber.toString());
		resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), caseType.toString());
		resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), date.toString());
		
		ro.cst.tsearch.servers.functions.OHFairfieldPR.parseNames(resultMap);
			
		return null;
	}
	
	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		ConfigurableNameIterator nameIterator = null;
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		for (String id : gbm.getGbTransfers()) {

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				
			GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module);
			module.addFilter(defaultNameFilter);
				
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] {"L, F;;" });
			nameIterator.clearSearchedNames();
			nameIterator.setInitAgain(true);
			module.addIterator(nameIterator);
				
			modules.add(module);
			
			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
				
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
					
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] {"L, F;;" });
				nameIterator.clearSearchedNames();
				nameIterator.setInitAgain(true);
				module.addIterator(nameIterator);
					
				modules.add(module);
				
			}
		}
		serverInfo.setModulesForGoBackOneLevelSearch(modules);
		
	}
			
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
				
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		String [] keys = new String[] {
			SearchAttributes.OWNER_OBJECT, 
			SearchAttributes.BUYER_OBJECT
		};
			
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			
		for(String key: keys){
			if(!(SearchAttributes.BUYER_OBJECT.equals(key)&&search.isProductType(SearchAttributes.SEARCH_PROD_REFINANCE))){
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.setSaObjKey(key);
				module.clearIteratorTypes();
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
				ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, new String[] {"L, F;;"});
				iterator.clearSearchedNames();
				iterator.setInitAgain(true);
				module.addIterator(iterator);
			    GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(key, searchId, module);
			    module.addFilter(nameFilter);
			    addBetweenDateTest(module, true, true, true);
			    modules.add(module);
			}
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	if(StringUtils.isEmpty(instrumentNo))
    		return false;
    	
    	/* The AO,TR document must be re-saved in date down searches: Bug 4584 */
    	if(mSearch.getSa().isDateDown() && isAssessorOrTaxServer()) {
    		return false;
    	}
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			if(documentManager.getDocument(documentToCheck.getInstrument()) != null) {
    				if (GWTDataSite.isRealRoLike(dataSite.getSiteTypeInt())){
	    				RegisterDocumentI docFound = (RegisterDocumentI) documentManager.getDocument(documentToCheck.getInstrument());
	    				RegisterDocumentI docToCheck = (RegisterDocumentI) documentToCheck;
	    				
	    				docToCheck.mergeDocumentsInformation(docFound, searchId, true, false);
    				}
    				return true;
    			} else if(!checkMiServerId) {
    				List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheck.getInstrument());
    				if(almostLike != null && !almostLike.isEmpty()) {
    					return true;
    				}
    			}
    		} else {
	    		InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		if(data != null) {
		    		if(!StringUtils.isEmpty(data.get("type"))) {
		        		String serverDocType = data.get("type");
		    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
		            	instr.setDocType(docCateg);
		            	instr.setDocSubType(DocumentTypes.getDocumentSubcategory(serverDocType, searchId));
		    		}
		    		
		    		instr.setBook(data.get("book"));
		    		instr.setPage(data.get("page"));
		    		instr.setDocno(data.get("docno"));
		    		
		    		try {
		    			instr.setYear(Integer.parseInt(data.get("year")));
		    		} catch (Exception e) {}
	    		}
	    		
	    		if(documentManager.getDocument(instr) != null) {
	    			return true;
	    		} else {
	    			List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    			
	    			if(checkMiServerId) {
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
	    			}
	    			
    				if(data!=null) {
    					if(!StringUtils.isEmpty(data.get("type"))){
			        		String serverDocType = data.get("type"); 
			    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
			    	    	String dataSource = data.get("dataSource");
			    	    	String dt = data.get("date");
			    	    	Date date = null;
			    	    	if (dt!=null) {
			    	    		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
			    	    		try {
			    	    			date = sdf.parse(dt);
			    	    		} catch (ParseException pe) {}
			    	    	}
			    	    	for (DocumentI documentI : almostLike) {
			    	    		if (serverDocType.equals("ASSESSOR") && dataSource != null) {
									if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))//B 4435, must save NDB and ISI doc of the same instrNo
										return true;
			    	    		} else if (serverDocType.equals("CNTYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		} else if (serverDocType.equals("CITYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		}else if( (!checkMiServerId || miServerID==documentI.getSiteId()) && documentI.getDocType().equals(docCateg) && 
			    	    				(date==null||documentI.getDate()==null||date.equals(documentI.getDate()))){
									return true;
			    	    		}
							}	
    					}
		    		} else {
		    			EmailClient email = new EmailClient();
		    			email.addTo(MailConfig.getExceptionEmail());
		    			email.setSubject("isInstrumentNoSaved problem on " + URLMaping.INSTANCE_DIR + this.getClass().getName());
		    			email.addContent("We should at least have type!!!!\nSearchId=" + searchId);
		    			email.sendAsynchronous();
		    		}
	    		}
    		}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	return false;
    }
	
}
