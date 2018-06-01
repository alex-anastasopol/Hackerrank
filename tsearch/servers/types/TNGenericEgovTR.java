/**
 * 
 */
package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BillNumberFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.MultipleYearIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.PDFUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author vladb
 *
 */
public class TNGenericEgovTR extends TSServerAssessorandTaxLike{

	private static final long serialVersionUID = 1L;
//	private static Pattern pidPattern = Pattern.compile("Property Tax Details for Bill #(\\d+)");

	public TNGenericEgovTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
		
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		// no details info on official site
		if (rsResponse.indexOf("You are eligible for state or county tax relief") > -1) {
			Response.getParsedResponse().setError("Account eligible for state or county tax relief.");
			return;
		}
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_TAX_BIL_NO:
			case ID_SEARCH_BY_ADDRESS:	
			case ID_SEARCH_BY_PARCEL:
			case ID_SEARCH_BY_MODULE25:		//Search by Company Name
				
				// no result
				if (rsResponse.indexOf("0 results matched your search criteria") > -1) {
					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() == 0) {
					return;
				}
				
				parsedResponse.setHeader("<table width=\"100%\" border=\"1\">" + extractHeader(rsResponse));	
				parsedResponse.setFooter("</table>");
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_GET_LINK:
				
				ParseResponse(sAction, Response, ID_DETAILS);
				break;
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				
				StringBuilder accountId = new StringBuilder();
				String details = getDetails(rsResponse, accountId);
				
				if(viParseID == ID_DETAILS) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					
					Pattern yearPat = Pattern.compile("(?is) \\bfor Bill\\s*\\(\\s*(\\d{4})\\s*\\)\\s*#");
					Matcher mat = yearPat.matcher(details);
					if (mat.find()){
						data.put("year", mat.group(1).trim());
					}
					
					
					if (isInstrumentSaved(accountId.toString().trim(), null, data)){
						details += CreateFileAlreadyInTSD();
					}
					else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
	
					parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					parsedResponse.setResponse(details);
				} else {
					String filename = accountId + ".html";
					smartParseDetails(Response,details);
					
					msSaveToTSDFileName = filename;
					parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
					parsedResponse.setResponse(details);
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				}
				
				break;
		}
	}
	
	private String addZeros(String s) {
		Matcher m = Pattern.compile("(\\d*)(.*)").matcher(s);
		if(m.find()) {
			String num = m.group(1);
			String rest = m.group(2);
			if(StringUtils.isNotEmpty(num)) {
				while(num.length() < 3) {
					num = "0" + num;
				}
			}
			return num + rest;
		}
		return s;
	}
	
	private String getDetails(String page, StringBuilder accountId) {
		
		StringBuilder details = new StringBuilder();
		/*Matcher m = pidPattern.matcher(page);
		if(m.find()) {
			accountId.append(m.group(1));
		}
		if(!page.matches("(?is).*<html .*")) { // if from memory
			return page;
		}*/
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList error = null;	
			NodeList tables = null;
			NodeList allTables = null;
			
			try {
				error = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "error message"));
				tables = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "content"), true)
					.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), false);
				allTables = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "content"), true)
					.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);
			} catch (Exception e) {
				allTables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			}
			
			String pid = "";
			String map = "";
			String group = "";
			String parcel = "";
			String pi = "";
			String si = "";
			for(int i = 0; i < allTables.size(); i++) {
				TableTag table = (TableTag) allTables.elementAt(i);
				if(table.toPlainTextString().indexOf("Mailing Address") > -1) {
					TableRow[] rows = table.getRows();
					for(TableRow row : rows) {
						if(row.getColumnCount() == 2) {
							String col1 = row.getColumns()[0].toPlainTextString().replace("&nbsp;", "").trim();
							String col2 = row.getColumns()[1].toPlainTextString().replace("&nbsp;", "").trim();
							if(col1.startsWith("Control Map")) {
								map = addZeros(col2);
							} else if(col1.startsWith("Group")) {
								group = addZeros(col2);
							} else if(col1.startsWith("Parcel")) {
								parcel = addZeros(col2);
							} else if(col1.startsWith("P/I")) {
								pi = addZeros(col2);
							} else if(col1.startsWith("S/I")) {
								si = addZeros(col2);
							}
						}
					}
				}
				table.setAttribute("border", "1");
			}
			pid = map + "-" + group + "-" + map + "-" + parcel + "-" + pi + "-" + si;
			accountId.append(pid);
 			
			if(!page.matches("(?is).*<html .*")) { // if from memory
				return page;
			}
			
			NodeList paymentTable = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "payment_table"), true);
			String paymentTableStr = "";
			if (paymentTable.size()>0) {
				paymentTableStr = paymentTable.elementAt(0).toHtml();
			}
			
			String errorMessage = "";
			if (error.size()>0) {
				errorMessage = error.elementAt(0).toHtml();
				errorMessage = errorMessage.replaceAll("(?is)<img[^>]+>", "");
				errorMessage = errorMessage.replaceAll("(?is)(<div[^>]+)>([^<]+)(</div>)", "$1 align=\"center\" style=\"color:Red\"><b>$2</b>$3");
				errorMessage += "<br>";
			}
			
			String tablesHtml = tables.toHtml();
			tablesHtml = tablesHtml.replace(paymentTableStr, "");
			tablesHtml = tablesHtml.replaceAll("(?is)<a[^>]*>(.*?)</a>", "$1");
			tablesHtml = tablesHtml.replaceAll("(?is)<img[^>]+>", "");
			tablesHtml = tablesHtml.replaceAll("(?is)<div[^>]+>\\s*<br>\\s*(Receipt|Bill|Parcel History)\\s*</div>", "");
			tablesHtml = tablesHtml.replaceAll("(?is)<table[^>]+>\\s*<tr>\\s*<td>\\s*</td>\\s*<td>\\s*</td>\\s*(<td>\\s*</td>\\s*)?</tr>\\s*</table>", "");
			details.append(errorMessage);
			details.append(tablesHtml);
			details.append(getPdf(page));
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error while getting details");
		}
		return details.toString();
	}
	
	private String getPdf(String page) {
		String res = "";
		
		Matcher ma = Pattern.compile("(?is)<a[^>]+href=\"([^\"]+)\"[^>]*>\\s*<img[^>]+>\\s*<br>Parcel History</a>").matcher(page);
		if (ma.find()) {
			String link = getBaseLink().replaceFirst("\\.com.*", ".com/") + ma.group(1);
			
			HTTPRequest reqP = new HTTPRequest(link);
			HttpSite site = HttpManager.getSite(HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName(), searchId);
			HTTPResponse resP = site.process(reqP);
			HttpManager.releaseSite(site);
			try {
				res = PDFUtils.extractTextFromPDF(resP.getResponseAsStream());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		StringBuilder sb = new StringBuilder();
		
		res = res.replaceAll("(?i)(Date:)(\\s*---)", "$1@@@$2");
		
		Matcher mat1 = Pattern.compile("(?i)((?:.+?Date:\\s*(?:[\\d-]+|N/A|@@@))|(?:---))\\s+(.+?)\\s+(Property Address.+|Subdivision.+|Dimensions.+|Calculated Acres.+|---\\s*)")
				.matcher(res);
		while (mat1.find()) {
			sb.append("<tr><td align=\"center\">").append(mat1.group(1)).append("</td><td align=\"center\">")
				.append(mat1.group(2)).append("</td><td align=\"center\">").append(mat1.group(3)).append("</td></tr>");
		}
		if (sb.length()>0) {
			sb.insert(0, "<br><table><tr><th>Parcel Details</th><tr><td><table id=\"fromPdf\" border=\"1\"><tr><th>Deed Information</th>" + 
					"<th>Plat Information</th><th>Property Information</th></tr>");
			sb.append("</table></td></tr></table>");
		}
	
		return sb.toString().replaceAll("(?i)(Date:)@@@", "$1");
	}
	
	private String extractHeader(String page) {
		
		String header = "";

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = (TableTag) nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "data"), true)
				.elementAt(0);
			
			TableRow[] rows = interTable.getRows();
			
			for(int i = 0; i < rows.length; i++) {
				
				TableRow row = rows[i];
				
				if(row.toPlainTextString().toUpperCase().indexOf("ADDRESS") > -1) {
					
					header = row.toHtml()
						.replaceAll("<a[^>]*>(.*?)</a>", "$1")
						.replaceAll("<td", "<th")
						.replaceAll("</td", "</th");
					break;
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return header;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = (TableTag) nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "data"), true)
				.elementAt(0);
			
			if(interTable == null) {
				return intermediaryResponse;
			}
			
			TableRow[] rows = interTable.getRows();
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY,miServerID);
			
			for(int i = 0; i < rows.length; i++) {
				
				TableRow row = rows[i];
				
				if(row.toPlainTextString().toUpperCase().indexOf("ADDRESS") > -1) {
					continue;
				}
				
				LinkTag linkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
				String link = CreatePartialLink(TSConnectionURL.idGET) + linkTag.extractLink().trim().replaceAll("\\s", "%20");
				link = link.replaceFirst(dataSite.getLink() + ":443", "");
				linkTag.setLink(link);
				
				String htmlRow = row.toHtml();
				htmlRow = htmlRow.replaceAll("(?is)<span[^>]*>\\s*<img.*?>\\s*</span>", "");
				
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
				currentResponse.setOnlyResponse(htmlRow);
				currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
				
				ResultMap resultMap = parseSpecificIntermediaryRow(row);
				resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
				resultMap.removeTempDef();
				Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
				
				DocumentI document = (TaxDocumentI)bridge.importData();				
				currentResponse.setDocument(document);
				
				intermediaryResponse.add(currentResponse);
			}
			outputTable.append(table);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

	protected ResultMap parseSpecificIntermediaryRow(TableRow row) {
		/* Implement me in the derived class */
		return null;
	}
	
	protected void parseSpecificDetails(ResultMap resultMap) {
		/* implement me in the derived class (parse address, owner, legal, etc.) */
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String page, ResultMap resultMap) {

		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
/*		Matcher m = pidPattern.matcher(page);
		if(m.find()) {
			resultMap.put("PropertyIdentificationSet.ParcelID", m.group(1));
		}*/
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);

			String year = "";
			String map = "";
			String group = "";
			String parcel = "";
			String pi = "";
			String si = "";
			String pid = "";
			String city = "";
			double delinqAmount = 0d;
			boolean isAnotherYear = false;
			
			//if searched year was another than the year found
			//e.g. the search is with year 2012, but 2011 taxes are not paid
			//in this case, 2011 document is brought
			Matcher ma = Pattern.compile("You still have a balance on your \\d{4} taxes\\.\\s*You must pay this before paying (\\d{4}) taxes\\.").matcher(page);
			if (ma.find()) {
				year = ma.group(1);
				isAnotherYear = true;
			}
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			for(int i = 0; i < tables.size(); i++) {
				TableTag table = (TableTag) tables.elementAt(i);
				if(table.toPlainTextString().indexOf("Property Tax Details") < 0) { // General Information Table
					if(table.toPlainTextString().indexOf("Mailing Address") > -1) {
						TableRow[] rows = table.getRows();
						for(TableRow row : rows) {
							if(row.getColumnCount() == 2) {
								String col1 = row.getColumns()[0].toPlainTextString().replace("&nbsp;", "").trim();
								String col2 = row.getColumns()[1].toPlainTextString().replace("&nbsp;", "").trim();
								if(col1.startsWith("Property")) {
									resultMap.put("tmpAddress", col2);
								} else if(col1.startsWith("Owner")) {
									resultMap.put("tmpOwner", col2);
								} else if(col1.startsWith("Control Map")) {
									map = addZeros(col2);
									resultMap.put("PropertyIdentificationSet.ParcelIDMap", map);
								} else if(col1.startsWith("Group")) {
									group = addZeros(col2);
									resultMap.put("PropertyIdentificationSet.ParcelIDGroup", group);
								} else if(col1.startsWith("Parcel")) {
									parcel = addZeros(col2);
									resultMap.put("PropertyIdentificationSet.ParcelIDParcel", parcel);
								} else if(col1.startsWith("P/I")) {
									pi = addZeros(col2);
								} else if(col1.startsWith("S/I")) {
									si = addZeros(col2);
								} else if(col1.startsWith("City Code")) {
									city = col2.replaceFirst("^\\d+\\s*", "");;
									if (!StringUtils.isEmpty(city)) {
										resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
									}
								}
							}
						}
					} else if(table.toPlainTextString().indexOf("Appraisal Year") > -1) { // Appraisal Information Table
						if (!isAnotherYear) {		//parse only if is not another year
							TableRow[] rows = table.getRows();
							for(TableRow row : rows) {
								if(row.getColumnCount() == 2) {
									String col1 = row.getColumns()[0].toPlainTextString().replace("&nbsp;", "").trim();
									String col2 = row.getColumns()[1].toPlainTextString().replaceAll("&nbsp;|,", "").trim();
									if(col1.startsWith("Land Value")) {
										resultMap.put("PropertyAppraisalSet.LandAppraisal", col2);
									} else if(col1.startsWith("Improvement Value")) {
										resultMap.put("PropertyAppraisalSet.ImprovementAppraisal", col2);
									} else if(col1.startsWith("Total Property Value")) {
										resultMap.put("PropertyAppraisalSet.TotalAppraisal", col2);
									} 
								}
							}
						}
					} else if(table.toPlainTextString().indexOf("Assessed Taxable Value") > -1) { // Tax Information Table
						if (!isAnotherYear) {		//parse only if is not another year
							TableRow[] rows = table.getRows();
							for(TableRow row : rows) {
								if(row.getColumnCount() == 3) {
									String col1 = row.getColumns()[0].toPlainTextString().replace("&nbsp;", "").trim();
									String col2 = row.getColumns()[1].toPlainTextString().replaceAll("&nbsp;|,", "").trim();
									if(col1.startsWith("Assessed Taxable Value")) {
										resultMap.put("PropertyAppraisalSet.TotalAssessment", col2);
									} if(col1.matches("\\d+ Tax Levy.*")) {
										resultMap.put("TaxHistorySet.BaseAmount", col2);
										year = col1.replaceFirst("(\\d+) Tax Levy.*", "$1");
										resultMap.put("TaxHistorySet.Year", year);
									}
								}
							}
						}
					}
				}
			}
			pid = map + "-" + group + "-" + map + "-" + parcel + "-" + pi + "-" + si;
			resultMap.put("PropertyIdentificationSet.ParcelID", pid);
			
			// parse tax tables
			NodeList additionalTables = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "data"), true);
			for(int i = 0; i < additionalTables.size(); i++) {
				TableTag table = (TableTag) additionalTables.elementAt(i);
				if(table.toPlainTextString().indexOf("Amount") > -1) {
					if (!isAnotherYear) {		//parse only if is not another year
						TableRow[] rows = table.getRows();
						for(TableRow row : rows) {
							if(row.getColumnCount() == 5) {
								String col2 = row.getColumns()[1].toPlainTextString().replace("&nbsp;", "").trim();
								String col5 = row.getColumns()[4].toPlainTextString().replaceAll("&nbsp;|,", "").trim();
								if(StringUtils.isNotEmpty(col2)) {
									resultMap.put("TaxHistorySet.DatePaid", col2);
								} else {
									resultMap.put("TaxHistorySet.AmountPaid", col5);
								}
							}
						}
					}
				} else if(table.toPlainTextString().indexOf("Rcpt") > -1) {
					int yearInt = Integer.parseInt(year);
					TableRow[] rows = table.getRows();
					for(TableRow row : rows) {
						if(row.getColumnCount() == 5) {
							String col2 = row.getColumns()[1].toPlainTextString().replace("&nbsp;", "").trim();
							String col3 = row.getColumns()[2].toPlainTextString().replace("&nbsp;", "").trim();
							String col5 = row.getColumns()[4].toPlainTextString().replaceAll("&nbsp;|,", "").trim();
							if (col2.matches("\\d+\\s*\\(C\\)") || col3.matches("\\d+\\s*\\(C\\)")) {	//ignore city taxes
								col2 = "";
							}
							col2 = col2.replaceAll("\\([^)]+\\)", "").trim();
							if(StringUtils.isNotEmpty(col2)) {
								if(col2.equals(year)) {
									resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), col5);
									if (isAnotherYear) {		//parse only if is another year
										resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
										resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), col5);
									}
								} else if (Integer.parseInt(col2)<yearInt) {
									double val = 0.0;
									try {
										val = Double.valueOf(col5); 
									} catch (NumberFormatException nfe) {}
									delinqAmount += val; 
								}
							}
						}
					}
				}
			}
			
			resultMap.put("TaxHistorySet.PriorDelinquent", String.valueOf(delinqAmount));
			parseSpecificDetails(resultMap);
			
			
			if (StringUtils.isEmpty(year)){
				Pattern yearPat = Pattern.compile("(?is) \\bfor Bill\\s*\\(\\s*(\\d{4})\\s*\\)\\s*#");
				Matcher mat = yearPat.matcher(page);
				if (mat.find()){
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), mat.group(1).trim());
				}
			}
			
			parseAdditional(resultMap, page);
			
		} catch (Exception e) {
			logger.error("Error while parsing details");
//			e.printStackTrace();
		}
		
		return null;
	}

	//parse additional data extracted from PDF
	private void parseAdditional(ResultMap resultMap, String page) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			ResultTable crossRef = new ResultTable();
			@SuppressWarnings("rawtypes")
			List<List> tablebodyRef = new ArrayList<List>();
			List<String> list;
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "fromPdf"), true);
			if (tables.size()>0) {
				TableTag table = (TableTag)tables.elementAt(0);
				for (int i=1;i<table.getRowCount();i++) {
					TableRow row = table.getRow(i);
					if (row.getColumnCount()==3) {

						String col0 = row.getColumns()[0].toPlainTextString();
						Matcher ma1 = Pattern.compile("(?i)Book:(.+?)Page:(.+?)Date").matcher(col0);
						while (ma1.find()) {
							list = new ArrayList<String>();
							String book = ma1.group(1).replaceFirst("^\\s*V", "").trim();
							String pg = ma1.group(2).trim();
							book = org.apache.commons.lang.StringUtils.stripStart(book, "0");
							pg = org.apache.commons.lang.StringUtils.stripStart(pg, "0");

							list.add(book);
							list.add(pg);
							tablebodyRef.add(list);
						}
						
						String notAvailablePattern = "(?i)\\bN\\s*/?\\s*(?:A|R)\\b|/(?:A|R)?\\b";
						String col1 = row.getColumns()[1].toPlainTextString();
						Matcher ma2 = Pattern.compile("(?i)Book:(.+?)Page:(.+?)Block:(.+?)Lot:(.+)").matcher(col1);
						if (ma2.find()) {
							String platbook = org.apache.commons.lang.StringUtils.stripStart(ma2.group(1).replaceAll(notAvailablePattern, "").trim(),"0");
							String platpage = org.apache.commons.lang.StringUtils.stripStart(ma2.group(2).replaceAll(notAvailablePattern, "").trim(),"0");
							String block = org.apache.commons.lang.StringUtils.stripStart(ma2.group(3).replaceAll(notAvailablePattern, "").trim(),"0");
							String lot = org.apache.commons.lang.StringUtils.stripStart(ma2.group(4).replaceAll(notAvailablePattern, "").replaceAll("(?i)LIST", "").replaceAll("(?i)\\bPT\\b", "").trim().replaceFirst("-+$", ""),"0");
							if (!StringUtils.isEmpty(platbook)) {
								resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), platbook);
							}
							if (!StringUtils.isEmpty(platpage)) {
								resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), platpage);
							}
							if (block.matches("(?i)\\s*NULL\\s*")) {
								block = "";
							}
							if (!StringUtils.isEmpty(block)) {
								resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
							}
							if (!StringUtils.isEmpty(lot)) {
								resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
							}
						}
						
						String col2 = row.getColumns()[2].toPlainTextString();
						Matcher ma3 = Pattern.compile("(?i)(?i)Subdivision:(.+)").matcher(col2);
						if (ma3.find()) {
							String subdivision = ma3.group(1).replaceAll(notAvailablePattern, "").trim();
							subdivision = subdivision.replaceFirst("(?is)\\bSUB(D(IV(ISION)?)?)?\\s*$", "");
							subdivision = subdivision.replaceFirst("(?is)(.+[NSEW])\\s+S$", "$1");			//CUMBERLAND PLACE N S (TN Sumner 148I-A-148I-020.00--000)
							if (!StringUtils.isEmpty(subdivision)) {
								subdivision = subdivision.replaceAll("(?is)\\b(SEC(?:TION)?)(\\d+|[A-Z])\\b", "$1 $2");
								String section = ro.cst.tsearch.extractor.legal.LegalDescription.extractSectionFromText(subdivision);
								if (!StringUtils.isEmpty(section)) {
									resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
									subdivision = subdivision.replaceFirst("\\bSEC\\.?(?:TION:?)?\\b\\s*[\\w&,-\\.]*(?:\\s|$)", "").trim();
								}
								if (!StringUtils.isEmpty(subdivision)) {									
									subdivision = subdivision.replaceFirst(",?\\s*\\bSEC(?:TION|\\.)?\\s*$", "");//B9286 - issue 4
									subdivision = cleanSubdivisionName(subdivision);
									
									resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
								}
								if (!StringUtils.isEmpty(subdivision)) {
									String phPatt = "\\bPH(?:ASE)?\\s*(\\d+|[A-Z])\\b";
									Matcher ma = Pattern.compile(phPatt).matcher(subdivision);
									if (ma.find()) {
										String phase = ma.group(1);
										resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
										subdivision = subdivision.replaceAll(phPatt, "").trim();
										subdivision = cleanSubdivisionName(subdivision);
										resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
									}
								}
							}
						}
						
					}
				}
				
				String[] headerRef = {CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName()};
				crossRef = GenericFunctions2.createResultTable(tablebodyRef, headerRef);
				if (crossRef != null && tablebodyRef.size()>0){
					resultMap.put("CrossRefSet", crossRef);
				}
			}
			
		} catch (Exception e) {
			logger.error("Error while parsing details");
		}	
	}

	protected String cleanSubdivisionName(String subdivision) {
		if(subdivision.matches(".*\\s+-LEN")) {
			subdivision = subdivision.substring(0, subdivision.length() - 4);
		}
		subdivision = subdivision.replaceAll("\\bSD$", "");
		return subdivision.trim();
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		TSServerInfoModule module;
		FilterResponse billNumberFilter = new BillNumberFilterResponse(searchId, 8, "10");
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		
		addPinModule(serverInfo, modules, billNumberFilter, addressFilter, sa);
		
		// search by Address
		String strName = getSearchAttribute(SearchAttributes.P_STREETNAME);
		String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		if(!isEmpty(strName) && !isEmpty(strNo)){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(1, SearchAttributes.CURRENT_TAX_YEAR);
			module.getFunction(4).forceValue(strNo + " " + strName);
			
			module.addFilter(billNumberFilter);
			module.addFilter(addressFilter);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
			MultipleYearIterator yearIterator = (MultipleYearIterator) ModuleStatesIteratorFactory.getMultipleYearIterator(module, searchId, numberOfYearsAllowed, getCurrentTaxYear());
			module.addIterator(yearIterator);
			
			modules.add(module);			
		}
		
		// search by name - filter by address
		
		if(hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			//module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			module.addFilter(billNumberFilter);
			
			FilterResponse nameFilter = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module );
			nameFilter.setSkipUnique(false);
			module.addFilter(nameFilter);
			
			module.addFilter(addressFilter);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
			module.setIteratorType(2,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(numberOfYearsAllowed, module, searchId, new String[] {"L;F;", "L;M;"});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

	protected void addPinModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules, FilterResponse billNumberFilter, FilterResponse addressFilter,
			SearchAttributes sa) {
		TSServerInfoModule module;
		// search by account number
		if(hasPin()){
			String pid = sa.getAtribute(SearchAttributes.LD_PARCELNO);
	    	String [] parts = pid.split("-");
			boolean parcelIsValid = false;
			String ctrlMap = "";
			String grp = "";
			String parcel = "";

			if (parts.length > 3) {
				parcelIsValid = true;
				ctrlMap = parts[2];
				grp = parts[1];
				parcel = parts[3];
			} else if (parts.length > 1) {
				parcelIsValid = true;
				ctrlMap = parts[0];
				parcel = parts[parts.length - 1];
				if (parts[1].matches("(?i)^[A-Z]$")) {
					grp = parts[1];
				} else if (parts[1].matches("(?i)^[0-9.]$")) {
					parcel = parts[parts.length];
				}
			}

			if (parcelIsValid) {
	    		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(1, SearchAttributes.CURRENT_TAX_YEAR);
				
				module.getFunction(6).forceValue(ctrlMap.replaceAll("^0+", ""));
				module.getFunction(7).forceValue(grp.replaceAll("^0+", ""));
				module.getFunction(8).forceValue(parcel.replaceAll("^0+", ""));
				
				sa.setAtribute(SearchAttributes.LD_PARCELNO_MAP, ctrlMap);
		    	sa.setAtribute(SearchAttributes.LD_PARCELNO_GROUP, grp);
		    	sa.setAtribute(SearchAttributes.LD_PARCELNO_PARCEL, parcel);
		    				
		    	module.addFilter(billNumberFilter);
		    	module.addFilter(addressFilter);
		    	
		    	module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
				MultipleYearIterator yearIterator = (MultipleYearIterator) ModuleStatesIteratorFactory.getMultipleYearIterator(module, searchId, numberOfYearsAllowed, getCurrentTaxYear());
				module.addIterator(yearIterator);
				
				modules.add(module);				
	    	}		
		}
	}
}
