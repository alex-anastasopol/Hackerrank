package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.CourtDocumentIdentificationSet.CourtDocumentIdentificationSetKey;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.MOGenericCaseNetCO.NameFilter;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;

public class KSGenericCO extends TSServerROLike {
		
	private static final long serialVersionUID = -2654769757179990231L;

	public KSGenericCO(long searchId) {
		super(searchId);
	}
	
	public KSGenericCO(String rsRequestSolverName, String rsSitePath, String rsServerID, 
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) 
			throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		if (rsResponse.indexOf("Client is not formatted correctly") > -1) {
			Response.getParsedResponse().setError("Client is not formatted correctly.");
			return;
		}
		
		switch (viParseID) {
								
		case ID_SEARCH_BY_MODULE38:		//Search by SSN
		case ID_SEARCH_BY_NAME:			//Search by Name
		case ID_SEARCH_BY_MODULE39:		//Search by Company Name
		case ID_SEARCH_BY_MODULE40:		//Search by Date
			
			if (rsResponse.indexOf("Social Security Number is not formatted correctly") > -1) {
				Response.getParsedResponse().setError("Social Security Number is not formatted correctly.");
				return;
			}
			
			if (rsResponse.indexOf("0 cases found") > -1) {
				Response.getParsedResponse().setError("No data found.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
			}
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
			
			if(smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
            } else {
            	Response.getParsedResponse().setError("No data found.");
				Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
				return;
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
				
		case ID_SEARCH_BY_INSTRUMENT_NO:	//Search by Case Number
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
			
			if (viParseID!=ID_SAVE_TO_TSD) {
				if (rsResponse.indexOf("Wyandotte County District Court Search - Case Display") == -1) {
					Response.getParsedResponse().setError("No data found.");
					Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
					return;
				}
			}
			
			StringBuilder serialNumber = new StringBuilder();
			String details = getDetails(rsResponse, serialNumber);
			String filename = serialNumber + ".html";
			String type = extractValue("Case Type", details);
			String subType = extractValue("Case Sub-type", details);
			type += " " + subType;
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", type.toUpperCase().trim());
				data.put("dataSource","CO");
				if (isInstrumentSaved(serialNumber.toString(),null,data)){
					details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
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
	
	private String extractValue(String label, String text) {
		String result = "";
		Matcher ma = Pattern.compile("(?is)<td[^>]*>\\s*" + label + "\\s*:\\s*<span[^>]*>([^<]*)</span>\\s*</td>").matcher(text);
		if (ma.find()) {
			result = ma.group(1).replaceAll("(?is)&nbsp;", " ").trim();
		}
		return result;
	}
	
	private String extractValueWithLink(String label, String text) {
		String result = "";
		Matcher ma = Pattern.compile("(?is)<td[^>]*>\\s*(?:<i[^>]*>\\s*</i>)?\\s*<a[^>]*>\\s*" + label + 
				"\\s*</a>\\s*:\\s*<span[^>]*>([^<]*)</span>\\s*</td>").matcher(text);
		if (ma.find()) {
			result = ma.group(1).replaceAll("(?is)&nbsp;", " ").trim();
		}
		return result;
	}
		
	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {
			
			String details = "";
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
									
			/* If from memory - use it as is */
			if(!rsResponse.toLowerCase().contains("<html")) {
				parcelNumber.append(extractValue("Case UID", rsResponse));
				return rsResponse;
			}
			
			parcelNumber.append(extractValueWithLink("Case UID", rsResponse));
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "span12"));
			
			if (tables.size()>1) {
				details = tables.elementAt(1).toHtml();
			}
			
			details = details.replaceAll("(?is)<hr>", "");				
			details = details.replaceAll("(?is)<ul(.*?)</ul>", "");				//list of links
			details = details.replaceAll("(?is)<i[^>]*>[^<]*</i>", "");			//icons
			details = details.replaceAll("(?is)<a[^>]*>\\s*top\\s*</a>", "");	//top links
			details = details.replaceAll("(?is)<a[^>]*>([^<]*)</a>", "$1");		//other links
			
			details = details.replaceAll("(?is)<table\\s+class=\"[^\"]+\"\\s*>", "<table width=\"100%\"");
			
			return details;
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
		
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		int numberOfUncheckedElements = 0;
		
		TableTag resultsTable = null;
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class","cases table table-bordered table-condensed"), true);
			
			if (mainTable.size()>0) {
				resultsTable = (TableTag)mainTable.elementAt(0);
			}
			
			if (resultsTable != null && resultsTable.getRowCount()>1) {
				
				String hrefPatt = "(?is)href=\"([^\"]+)\"";
				
				TableRow[] rows  = resultsTable.getRows();
				int len = rows.length;
				for (int i=1;i<len;i++) {
					
					TableRow row = rows[i];
					
					String link = "";
					String htmlRow = row.toHtml();
						
					Matcher matcher = Pattern.compile(hrefPatt).matcher(htmlRow);
					if (matcher.find()) {
						link = CreatePartialLink(TSConnectionURL.idGET) + matcher.group(1);
					}
					
					link = link.replaceAll("(?is)&amp;", "&");
						
					htmlRow = htmlRow.replaceAll(hrefPatt, "href=\"" + link + "\"");
						
					ParsedResponse currentResponse = new ParsedResponse();
						
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
										
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,htmlRow);
					currentResponse.setOnlyResponse(htmlRow);
						
					ResultMap m = ro.cst.tsearch.servers.functions.KSGenericCO.parseIntermediaryRow(row, searchId); 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
						
					DocumentI document = (RegisterDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
						
					String checkBox = "checked";
					String instrNo = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()));
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("instrno", instrNo);
					data.put("type", org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName())));
					
					if (isInstrumentSaved(instrNo, document, data, false)) {
						checkBox = "saved";
					} else {
						numberOfUncheckedElements++;
						LinkInPage linkInPage = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
						checkBox = "<input type='checkbox' name='docLink' value='" + link + "'>";
						currentResponse.setPageLink(linkInPage);
					}
					htmlRow = htmlRow.replaceFirst("<tr>", "<tr><td>" + checkBox + "</td>");
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
					currentResponse.setOnlyResponse(htmlRow);
					intermediaryResponse.add(currentResponse);
					 
				}
				
				String header = "<table border=\"1px\" style=\"border:1px;border-collapse:collapse\" align=\"center\">" + 
						"<tr><td>" + SELECT_ALL_CHECKBOXES + "</td><th>Case Number</th><th>Case Type</th><th>Case Subtype</th><th>Date Filed</th><th>Name</th></tr>";
				String footer = "</table>";
				
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
			
			String instrNo = "";
					
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"CO");
			
			int intemsDone = 0;
			String[] parts = detailsHtml.split("(?is)<h3[^>]*>");
			
			for (int i=0;i<parts.length;i++) {
				
				parts[i] = parts[i].trim();
						
				if (parts[i].startsWith("Case Number:")) {
					
					String[] newParts = parts[i].split("(?is)<h4[^>]*>");
					int len = newParts.length;
					
					instrNo = extractValue("Case UID", newParts[0]).trim();
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
									
					String type = extractValue("Case Type", newParts[0]).trim();
					String subType = extractValue("Case Sub-type", newParts[0]).trim();
					type += " " + subType;
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), type.toUpperCase().trim());
					
					String recordedDate = extractValue("Filed", newParts[0]).trim();
					resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate);
					
					List<List> bodyCR = new ArrayList<List>();
					List<String> line;
					int j = 1;
					while (j<len) {
						if (newParts[j].startsWith("Consolidated Cases")) {
							if (j<len-1) {
								j++;
								if (newParts[j].startsWith("Case Number:")) {
									String refInstNo = extractValue("Case UID", newParts[j]);
									String refType = extractValue("Case Type", newParts[j]).trim();
									String refSubType = extractValue("Case Sub Type", newParts[j]).trim();
									refType += " " + refSubType;
									line = new ArrayList<String>();
									line.add(refInstNo);
									line.add(refType.toUpperCase().trim());
									bodyCR.add(line);
								}
							}
						}
						j++;
					}
					if (bodyCR.size() > 0) {
						String[] header = { CrossRefSetKey.INSTRUMENT_NUMBER.getShortKeyName(), CrossRefSetKey.INSTRUMENT_REF_TYPE.getShortKeyName()};
						ResultTable rt = GenericFunctions2.createResultTable(bodyCR, header);
						resultMap.put("CrossRefSet", rt);
					}
					
					intemsDone++;
					
				} else if (parts[i].startsWith("Defendants")) {
					
					ArrayList<List> list = new ArrayList<List>();
					String[] newParts = parts[i].split("(?is)<h4[^>]*>");
					for (int j=0;j<newParts.length;j++) {
						ro.cst.tsearch.servers.functions.KSGenericCO.parseNamesDetails(instrNo, newParts[j], list);
					}
					ro.cst.tsearch.servers.functions.KSGenericCO.putNamesDetails(resultMap, list, 1);
					intemsDone++;
					
				} else if (parts[i].startsWith("Plaintiff")) {
					
					ArrayList<List> list = new ArrayList<List>();
					String[] newParts = parts[i].split("(?is)<h4[^>]*>");
					for (int j=0;j<newParts.length;j++) {
						ro.cst.tsearch.servers.functions.KSGenericCO.parseNamesDetails(instrNo, newParts[j], list);
					}
					ro.cst.tsearch.servers.functions.KSGenericCO.putNamesDetails(resultMap, list, 0);
					intemsDone++;
					
				} else if (parts[i].startsWith("Case Judge")) {
					
					parts[i] = parts[i].replaceFirst("(?is)^[^<]+</h3>", "").trim();
					
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(parts[i], null);
					NodeList nodeList = htmlParser.parse(null);
					
					NodeList tables =  nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
					if (tables.size()>0) {
						TableTag table = (TableTag)tables.elementAt(0);
						if (table.getRowCount()==1) {
							TableRow row = table.getRow(0);
							if (row.getColumnCount()==4) {
								String lastName = row.getColumns()[0].toPlainTextString().replaceAll("(?is)\\bLast\\s+Name\\s*:", "").trim();
								String instrNoModif = instrNo.replaceFirst("^\\d{2}", "").replaceAll("-0+", "").replaceAll("-", "");
								lastName = lastName.replaceFirst(instrNoModif + "$", "").trim();
								if (!"No Judge Assigned".equalsIgnoreCase(lastName)) {
									String firstName = row.getColumns()[1].toPlainTextString().replaceAll("(?is)\\bFirst\\s*:", "").trim();
									String middleName = row.getColumns()[2].toPlainTextString().replaceAll("(?is)\\bMiddle\\s*:", "").trim();
									String suffix = row.getColumns()[3].toPlainTextString().replaceAll("(?is)\\bSuffix\\s*:", "").trim();
									String judgeName = lastName + " " + firstName + " " + middleName + " " + suffix;
									resultMap.put(CourtDocumentIdentificationSetKey.JUDGE_NAME.getKeyName(), judgeName.replaceAll("\\s{2}", " ").trim());
								}
							}
						}
					}
					
					intemsDone++;
					
				}
				
				if (intemsDone==4) {
					break;
				}
			}
		
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
			
	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		
		ConfigurableNameIterator nameIterator = null;
				
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;	
		GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
		        
		FilterResponse rejectAlreadySavedDocumentsFilter = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		
		for (String id : gbm.getGbTransfers()) {
					  		   	    	 
			//Search by Name
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantor");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				
				module.forceValue(5, "on");
//				module.forceValue(7, "on");
//				module.forceValue(8, "on");
				
				module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				module.addFilter(rejectAlreadySavedDocumentsFilter);
				addBetweenDateTest(module, true, false, true);
				module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
				module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
				nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
				module.addIterator(nameIterator);
				
				modules.add(module);
			}
			
			//Search by Company Name
			{
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX39));
				
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantor");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				
				module.forceValue(2, "on");
//				module.forceValue(4, "on");
//				module.forceValue(5, "on");
				
				module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				module.addFilter(rejectAlreadySavedDocumentsFilter);
				addBetweenDateTest(module, true, false, true);
				module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
				module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
				nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
				nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
				module.addIterator(nameIterator);
				
				modules.add(module);
			}
			
				    	     
			if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
				
				//Search by Name
				{
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
					
					module.setIndexInGB(id);
					module.setTypeSearchGB("grantee");
					module.clearSaKeys();
					module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
					
					module.forceValue(5, "on");
//					module.forceValue(7, "on");
//					module.forceValue(8, "on");
					
					module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
					module.addFilter(rejectAlreadySavedDocumentsFilter);
					addBetweenDateTest(module, true, false, true);
					module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
					module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
					module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
					module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
					nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
					nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
					module.addIterator(nameIterator);
					
					modules.add(module);
				}
				
				//Search by Company Name
				{
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX39));
					
					module.setIndexInGB(id);
					module.setTypeSearchGB("grantee");
					module.clearSaKeys();
					module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
					
					module.forceValue(2, "on");
//					module.forceValue(4, "on");
//					module.forceValue(5, "on");
					
					module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
					module.addFilter(rejectAlreadySavedDocumentsFilter);
					addBetweenDateTest(module, true, false, true);
					module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
					module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
					module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
					nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
					nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
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
				
		//Search by Name
		{
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			module.forceValue(5, "on");
//			module.forceValue(7, "on");
//			module.forceValue(8, "on");
			
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, true, new String[]{"L;F;"});
			iterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
			iterator.setInitAgain(true);
			module.addIterator(iterator);
			
			NameFilter nameFilter = new NameFilter(SearchAttributes.OWNER_OBJECT, searchId);
			module.addFilter(nameFilter);
			module.addFilter(new RejectAlreadySavedDocumentsFilterResponse(searchId));
			addBetweenDateTest(module, true, true, true);
						
			modules.add(module);
		}
		
		//Search by Company Name
		{
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX39));
			
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			module.forceValue(2, "on");
//			module.forceValue(4, "on");
//			module.forceValue(5, "on");
			
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
			ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, true, new String[]{"L;F;"});
			iterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
			iterator.setInitAgain(true);
			module.addIterator(iterator);
			
			NameFilter nameFilter = new NameFilter(SearchAttributes.OWNER_OBJECT, searchId);
			module.addFilter(nameFilter);
			module.addFilter(new RejectAlreadySavedDocumentsFilterResponse(searchId));
			addBetweenDateTest(module, true, true, true);
						
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}

	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		
		if(restoreDocumentDataI == null) {
			return null;
		}
		
		String instrumentNumber = restoreDocumentDataI.getInstrumentNumber();
		
		TSServerInfoModule module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		module.forceValue(0, instrumentNumber);
		module.getFilterList().clear();
		
		return module;
	}
	
	@Override
	protected NameI getNameFromModule(TSServerInfoModule module) {
		NameI name = new Name();
		
		if (module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 2) {
			String lastName = module.getFunction(0).getParamValue();
			String firstName = module.getFunction(1).getParamValue();
			String middleName = module.getFunction(2).getParamValue();
			if (!StringUtils.isEmpty(lastName)) {
				name.setLastName(lastName);
				if (!StringUtils.isEmpty(firstName)) {
					name.setFirstName(firstName);
				}
				if (!StringUtils.isEmpty(middleName)) {
					name.setMiddleName(middleName);
				}
				return name;
			}
		} else if (module.getModuleIdx() == TSServerInfo.MODULE_IDX39 && module.getFunctionCount() > 1) {
			String companyName = module.getFunction(0).getParamValue();
			if (!StringUtils.isEmpty(companyName)) {
				name.setLastName(companyName);
				name.setCompany(true);
				return name;
			}	
			
		}	
			
		return null;
	}
	
}
