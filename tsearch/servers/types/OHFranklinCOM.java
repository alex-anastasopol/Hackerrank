package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator.SEARCH_WITH_TYPE;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

import com.stewart.ats.base.document.DocumentI;

public class OHFranklinCOM extends TSServer {

	private static final long serialVersionUID = 1860214563562034490L;
	
	public OHFranklinCOM(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public OHFranklinCOM(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "COM");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 6) {	
			String instrNo = cols[0].getChildrenHTML().trim();
			String partyType = cols[2].getChildrenHTML().trim();
			String name = cols[3].getChildrenHTML().trim();
			
			if (StringUtils.isNotEmpty(instrNo)) {
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
				Matcher m = Pattern.compile("(?is)\\d{4}\\s*([A-Z]{2}\\s*[A-Z])\\s*\\d{6}").matcher(instrNo);
				if (m.find()) {
					String propType = m.group(1).trim();
					if (propType.matches("ER\\s*[A-Z]")) {
						resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Environmental Records");
					} else {
						resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Non Environmental Records");
					}
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), propType);
				}
			}
			
			if (StringUtils.isNotEmpty(partyType) && StringUtils.isNotEmpty(name)) {
				if ("DEFENDANT".equals(partyType.toUpperCase())) {
					resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), name);
				
				} else if ("PLAINTIFF".equals(partyType.toUpperCase()) || 
						   "OFFICER".equals(partyType.toUpperCase()) ||
						   "OFFICER COMPLAINANT".equals(partyType.toUpperCase())) {
					resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), name);
				}
				
				try {
					parseNames(resultMap, searchId);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return resultMap;
	}
	
	boolean existResultsWithSameCaseNo(int rowIndex, TableTag table) {
		int noOfRows = table.getRowCount();
		if (rowIndex < noOfRows) {
			TableRow referenceRow = table.getRow(rowIndex);
			TableRow candidateRow = table.getRow(rowIndex + 1);
			if (referenceRow.getColumnCount() == 6 && candidateRow.getColumnCount() == 6) {
				TableColumn refCol = referenceRow.getColumns()[0];
				TableColumn candCol = candidateRow.getColumns()[0];
				if (refCol != null && candCol != null) {
					String refCaseNo = refCol.getChildrenHTML().trim();
					String candCaseNo = candCol.getChildrenHTML().trim();
					if (candCaseNo.equals(refCaseNo)) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	int getNumberOfRowsWithSameCaseNo(int rowIndex, TableTag table) {
		int count = 0;
		int tableSize = table.getRowCount();
		
		if (existResultsWithSameCaseNo(rowIndex, table)) {
			int index = rowIndex + 1;
			TableRow referenceRow = table.getRow(rowIndex);
			TableRow candidateRow = table.getRow(index);
			TableColumn refCol = referenceRow.getColumns()[0];
			TableColumn candCol = candidateRow.getColumns()[0];
			String refCaseNo = refCol.getChildrenHTML().trim();
			String candCaseNo = candCol.getChildrenHTML().trim();
			
			
			while (candCaseNo.equals(refCaseNo) && index <= tableSize - 1) {
				count ++;
				index ++;
				if (index == tableSize)
					break;
				else {
					candidateRow = table.getRow(index);
					candCol = candidateRow.getColumns()[0];
					candCaseNo = candCol.getChildrenHTML().trim();
				}
			}
		}
		
		return count;
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		if (table.contains("NO RESULTS FOUND!")) {
			return intermediaryResponse;
			
		} else
		try {
			HtmlParser3 htmlParser = new HtmlParser3(table);	
			NodeList nodeList = htmlParser.getNodeList()
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "results_table"), true);
						
			if(nodeList.size() == 0) {
				return intermediaryResponse;
			}
			
			TableTag tableTag = (TableTag) nodeList.elementAt(0);
			
			TableRow[] rows  = tableTag.getRows();
			
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if(row.getColumnCount() == 6) {
					ParsedResponse currentResponse = new ParsedResponse();
					LinkTag linkTag = new LinkTag();
					
					NodeList list = row.getColumns()[5].getChildren().extractAllNodesThatMatch(new TagNameFilter("form"))
							.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true);
					String params = "";
					
					if (list != null) {
						for (int j = 0; j < list.size(); j++) {
							String input = list.elementAt(j).toHtml();
							String regExp = "(?is)<input .*name=\\s*\\\"([A-Z_-]+)\\\"\\s*value=\\\"([\\dA-Z]+)\\\"\\s*[^>]+>";
							Matcher m = Pattern.compile(regExp).matcher(input);
							
							if (m.find()) {
								params += m.group(1) + "=" + m.group(2);
								if (j != list.size()-1)
									params += "&";
							}
						}
						
						if (params != null) {
							String link =  CreatePartialLink(TSConnectionURL.idPOST) + dataSite.getDocumentServerLink().replaceAll("\\s", "%20")
									.replaceFirst("(?is)(.*/)[a-z\\.]+", "$1") + "case.php" + "?" + params;
							linkTag.setLink(link);
							currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
						}
						
						row.getColumns()[5].getChildren().remove(1);
						row.getColumns()[5].getChildren().remove(0);
						row.getColumns()[5].getChildren().elementAt(0).setText(linkTag.toHtml() + "View" + "</A>");
					}
					 
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
						
					ResultMap map = parseIntermediaryRow(row, searchId); 
					Bridge bridge = new Bridge(currentResponse, map, searchId);
					DocumentI document = bridge.importData();
					
					currentResponse.setDocument(document);
					intermediaryResponse.add(currentResponse);	
				}
			}

			response.getParsedResponse().setHeader("<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + "<tr><th>CASE NUMBER</th> <th>STATUS</th> <th>PARTY TYPE</th> <th>NAME</th> <th>DOB</th> <th>VIEW DETAILS</th> </tr>");
			response.getParsedResponse().setFooter("</table>");
		
			outputTable.append(table);
						
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		// 	no result returned - error message on official site
		if (rsResponse.contains("An unexpected error has occurred:")) {
			Response.getParsedResponse().setError("Official site not functional");
    		return;
    	} else if (rsResponse.indexOf("Error - Not A Valid Case Number") > -1 ||
    			   rsResponse.indexOf("Error: No Case Number") > -1) {
			Response.getParsedResponse().setError(NO_DATA_FOUND);
			return;
		} 
		
		switch (viParseID) {			
			case ID_SEARCH_BY_NAME:	
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
	            }			
				break;
				
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
									
				StringBuilder accountId = new StringBuilder();
				String details = "";
				
				details = getDetails(rsResponse, accountId);
				String filename = accountId + ".html";
				
				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","COURT");
					
					if (isInstrumentSaved(accountId.toString(),null,data)){
						details += CreateFileAlreadyInTSD();
					
					} else {
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

	
	protected String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				if (rsResponse.contains("id=\"propInfoTable\"")) { 
					Matcher m = Pattern.compile("(?is).*Case No\\.?\\s*([\\d\\s*(?:(?:C|T)R\\s*[A-Z]{1}|CV\\s*[A-Z]{1})]+).*").matcher(rsResponse);
					if (m.find()) {
						accountId.append(m.group(1).trim());
					}
				
					return rsResponse;
				}
			
			} else {
				try {
					StringBuilder details = new StringBuilder();
					HtmlParser3 htmlParser = new HtmlParser3(rsResponse);
					
					NodeList nodeList = htmlParser.getNodeList();
					NodeList tmp = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "help_information"));
					if (tmp != null) {
						nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true).extractAllNodesThatMatch(new HasAttributeFilter("id", "main"), true)
							.elementAt(0).getChildren().remove(tmp.elementAt(0));
					}
					
					details.append("<table border = \"1\" align=\"center\" id=\"mainTable\"> \n");
					
					TableTag firstTable = (TableTag)nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "main"), true).elementAt(0).getChildren()
							.extractAllNodesThatMatch(new TagNameFilter("table"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("class", "data_table"), true).elementAt(0);
					if (firstTable != null) {
						firstTable.setAttribute("id", "\"propInfoTable\"");
						//owner info + case Number (YYYY XXX dddddd, X = letter, d =digit, YYYY = year)
						if (firstTable.getRowCount() == 2) {
							TableRow row = firstTable.getRow(0);
							String text = row.getColumns()[0].getChildrenHTML();
							text = "\n <div align=\"center\" style=\"background-color: aliceblue;\"> <h2>" + text + "</h2> </div> \n";
							firstTable.getRow(0).getColumns()[0].getChild(0).setText(text);
							row = firstTable.getRow(1);
							if (row.getColumnCount() >= 2) {
								TableColumn col = row.getColumns()[1];
								if (col != null) {
									String apn = col.getChildrenHTML().trim();
									//e.g: Case No. 2006 CVE 027834; Case No. 2009 CVF 036639; Case No. 1995 CVG 008732; Case No. 1994 CVH 025382; Case No. 2009 CVI 019725;
									//     Case No. 2003 CR A 029028; Case No. 2009 TR D 213441
									apn = apn.replaceFirst("(?is)Case No\\.?\\s*(\\d{4}\\s*[A-Z]{2}\\s*[A-Z]\\s*\\d+).*", "$1").trim();
									if (apn.matches("\\d{4}\\s*[A-Z]{2}\\s*[A-Z]\\s*\\d{6}")) {
										accountId.append(apn);
									}
								}
							}
						}
					}
					
					details.append("<tr align=\"left\"> \n <td> \n");	
					details.append(firstTable.toHtml());
					details.append("</td> </tr>");
					nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("class", "toggler-c opened"), true);
					
					if(nodeList.size() == 0) {
						return rsResponse;
					
					} else {	
						int size = nodeList.size();
						size = size / 3;
						for (int i=0; i < size;  i++) {
							String title = nodeList.elementAt(i).getText();
							title = title.replaceFirst("(?is).*title\\s*=\\s*\\\"([^\\\"]+)\\\"", "$1");
							String divHeader = "<div style=\"background-color:aliceblue;\"> <h3> " + title + " </h3> </div> \n";
							TableTag table = (TableTag) nodeList.elementAt(i).getChildren()
									.extractAllNodesThatMatch(new HasAttributeFilter("class", "data_table"), true)
									.elementAt(0);
							table.setAttribute("align", "\"left\"");
							
							String htmlContent = table.toHtml();
							htmlContent = htmlContent.replaceAll("(?is)<a[^>]+>\\s*[^/]+/\\s*a>(?:\\s*-\\s*)?", "");
							htmlContent = htmlContent.replaceAll("(?is)(?:<a[^>]+>)?\\s*<img[^>]+>\\s*(?:</a>)?", "");
							details.append("<tr align=\"left\"> \n <td> \n");	
							details.append(divHeader);
							details.append(htmlContent);
							details.append("\n </td> </tr>");
						}
						
						details.append("</td> </tr> </table>");
					}
					
					return details.toString();
						
					} catch (Throwable t){
						logger.error("Error while parsing details page data", t);
					}
				}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return rsResponse;
	}

	
	public static void parseNamesInterm(ResultMap resultMap, long searchId, String partyTypeSet) {
		String unparsedName = "";
//		unparsedName = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if ("GrantorSet".equals(partyTypeSet)) {
			unparsedName = (String) resultMap.get(SaleDataSetKey.GRANTOR.getKeyName());
		} else if ("GranteeSet".equals(partyTypeSet)) {
			unparsedName = (String) resultMap.get(SaleDataSetKey.GRANTEE.getKeyName());
		}
		
		ArrayList<List> body = new ArrayList<List>();
		
		if(StringUtils.isNotEmpty(unparsedName)) {
			unparsedName = unparsedName.replaceAll("\\s*-\\s*(ETAL),?\\s*", " $1 - ");
			unparsedName = unparsedName.replaceAll("(?is)\\(\\b\\s*DEC\\s*\\b\\)*", "");
			unparsedName = unparsedName.replaceAll("(?is)\\b(A|F)KA\\b\\s*", "");
			
			String[] mainTokens = unparsedName.split(";");
											
			for (int i = 0; i < mainTokens.length; i++) {
				String currentToken = mainTokens[i];
				String[] names = StringFormats.parseNameNashville(currentToken, true);	
				String[] types = GenericFunctions.extractAllNamesType(names);
				String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
					
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);												
			}
			if ("GrantorSet".equals(partyTypeSet)) {
				try {
					GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
					resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), body.toString());
					//resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(body, true));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if ("GranteeSet".equals(partyTypeSet)) {
				try {
					//GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
					resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), body.toString());
					resultMap.put("GranteeSet", GenericFunctions.storeOwnerInSet(body, true));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void parseNames(ResultMap m, long searchId) throws Exception {
		String grantor = (String) m.get("SaleDataSet.Grantor");
		String grantee = (String) m.get("SaleDataSet.Grantee");
	
		if(StringUtils.isEmpty(grantee) && StringUtils.isNotEmpty((String) m.get("SaleDataSet.GranteeLander"))) {
			grantee = (String) m.get("SaleDataSet.GranteeLander");
		}
		grantor = ro.cst.tsearch.utils.StringUtils.prepareStringForHTML(grantor);
		grantee = ro.cst.tsearch.utils.StringUtils.prepareStringForHTML(grantee);
		grantor = NameCleaner.cleanFreeformName(grantor);
		grantee = NameCleaner.cleanFreeformName(grantee);
		
		ArrayList<List> grantorList = new ArrayList<List>();
		ArrayList<List> granteeList = new ArrayList<List>();
		
		parseNameInner(m, grantor, grantorList, searchId, false);
		parseNameInner(m, grantee, granteeList, searchId, true);
				
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantorList, true));
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(granteeList, true));
//		GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
//		
//		GenericFunctions.checkTNCountyROForMERSForMortgage(m, searchId);
	}

	public static void parseNameInner(ResultMap m, String name, ArrayList<List> namesList, long searchId, boolean isGrantee) {
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;
	
		name = name.replaceAll("(?is)\\b[FAN]\\s*/\\s*K\\s*/\\s*A\\b", "\n");
		name = name.replaceAll("(?is)\\bADM\\b", "");//means ADMINISTRATOR
		name = name.replaceAll("(?is)\\bSU[CB]\\s+(TRUSTEE)\\b", " $1");//means ADMINISTRATOR
		
		String[] nameItems = name.split("\\s*/\\s*");
		for (int i = 0; i < nameItems.length; i++){
			names = StringFormats.parseNameNashville(nameItems[i], true);
						
			type = GenericFunctions.extractNameType(names);
			otherType = GenericFunctions.extractNameOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(nameItems[i], names, suffixes[0], suffixes[1], type, otherType, 
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), namesList);
		}
	}
	
	

	@Override
	protected Object parseAndFillResultMap(ServerResponse rsResponse, String detailsHtml, ResultMap map) {
		try {	
			String accountID = "";
			String grantors = "";
			String grantees = "";
			String instrDate = "";
			
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"COM");
			
			HtmlParser3 htmlParser = new HtmlParser3(detailsHtml);
			NodeList list = htmlParser.getNodeList();		
			
			TableTag tmpTable = (TableTag) list.extractAllNodesThatMatch(new HasAttributeFilter("id", "propInfoTable"), true).elementAt(0);
			
			if (tmpTable != null) {
				if (tmpTable.getRowCount() == 2) {
					TableRow row = tmpTable.getRows()[1];
					TableColumn col = row.getColumns()[0];
					String partiesNames = col.getChildrenHTML().trim();
					String regExp = "(?is)(.*)<br\\s*/>\\s*<sup>\\s*<i>Plaintiff\\s*</i>\\s*</sup>\\s*<br\\s*/>\\s*<span[^>]+>.*Vs[^>]+>([^<]+(?:<i>\\s*et al\\s*</i>\\s*)?)<br\\s*/>\\s*<sup>\\s*<i>Defendant\\s*</i>\\s*</sup>\\s*";
					Matcher m = Pattern.compile(regExp).matcher(partiesNames);
					if (m.find()) {
						grantors = m.group(1).replaceAll("<\\s*/?i\\s*>", "");
						grantees = m.group(2).replaceAll("<\\s*/?i\\s*>", "") ;
						
						if (grantors != null) {
//							resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
							map.put(SaleDataSetKey.GRANTOR.getKeyName(), grantors);
						}
						if (grantees != null) {
							map.put(SaleDataSetKey.GRANTEE.getKeyName(), grantees);
						}
						
						parseNames(map, searchId);
					}
					
					col = row.getColumns()[1]; 
					accountID = col.getChildren().toHtml().trim();
					accountID = accountID.replaceFirst("(?is)\\s*Case No\\.?\\s*(\\d{4}\\s*[A-Z]{2}\\s*[A-Z]\\s*\\d+).*", "$1").trim();
					if (StringUtils.isNotEmpty(accountID)) {
						map.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), accountID);
						String docType = accountID.replaceFirst("(?is)\\d{4}\\s+([A-Z]{2}\\s*[A-Z])\\s+\\d+", "$1").trim();
						if (StringUtils.isNotEmpty(docType)) {
							map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
							if (docType.matches("ER\\s*[A-Z]")) {
								map.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Environmental Records");
//								map.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real Estate");
							} else {
								map.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Non Environmental Records");
//								map.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Other type");
							}
						}
					}
					
					instrDate = col.getChildrenHTML().trim();
					instrDate = instrDate.replaceFirst("(?is).*Filed\\s*:\\s*(\\d{2}\\s*/\\s*\\d{2}\\s*/\\s*\\d{4}).*", "$1").replaceAll("\\s", "");
					if (StringUtils.isNotEmpty(instrDate)) {
						map.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), instrDate);
					}
				}
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		
		return null;	
	}
	
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		RejectNonRealEstate propertyTypeFilter = new RejectNonRealEstate(searchId) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 5928139360531679148L;

			@SuppressWarnings("unchecked")
			@Override
			public BigDecimal getScoreOneRow(ParsedResponse row) {
				for(PropertyIdentificationSet pis: (Vector<PropertyIdentificationSet>)row.getPropertyIdentificationSet()){
					if(pis.getAtribute("PropertyType").toLowerCase().equals("environmental records")){
						return  ATSDecimalNumberFormat.ONE;
					} 
				}
				return  ATSDecimalNumberFormat.ZERO;
			}
			
			 @Override
			public String getFilterCriteria(){
			   	return "Type='Environmental Records'";
			}
		};
		propertyTypeFilter.setThreshold(new BigDecimal("0.95"));
		
		if (hasOwner()){
			// search with person name
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter( 
					SearchAttributes.OWNER_OBJECT, searchId, module);
			module.addFilter(propertyTypeFilter);
			module.addFilter(defaultNameFilter);
			addFilterForUpdate(module, true);
			
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = new ConfigurableNameIterator(searchId, new String[] {"L;F;"});
			nameIterator.setSearchWithType(SEARCH_WITH_TYPE.PERSON_NAME);
			nameIterator.setInitAgain(true);
			module.addIterator(nameIterator);
			
			modules.add(module);
			
			
			// search with Organization / Business Name
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			defaultNameFilter = NameFilterFactory.getDefaultNameFilter( 
					SearchAttributes.OWNER_OBJECT, searchId, module);
			module.addFilter(propertyTypeFilter);
			module.addFilter(defaultNameFilter);
			addFilterForUpdate(module, true);
			
			module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
			nameIterator = new ConfigurableNameIterator(searchId, new String[] {"L;;"});
			nameIterator.setSearchWithType(SEARCH_WITH_TYPE.COMPANY_NAME);
			nameIterator.setInitAgain(true);
			module.addIterator(nameIterator);
			
			modules.add(module);
    	}
			
	    serverInfo.setModulesForAutoSearch(modules);
	}
}
