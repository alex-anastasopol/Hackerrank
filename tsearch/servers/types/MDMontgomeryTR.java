
package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author vladb
 *
 */
public class MDMontgomeryTR extends TSServer {

	private static final long serialVersionUID = 1L;

	public MDMontgomeryTR(long searchId) {
		super(searchId);
	}

	public MDMontgomeryTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_ADDRESS:
				// no result
				if (rsResponse.contains("No records found")) {
					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				
				StringBuilder outputTable = new StringBuilder();
				outputTable.append(viParseID);
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				if(smartParsedResponses.size() == 0) {
					return;
				}
				
				parsedResponse.setHeader("<table style='border-collapse: collapse' border='2' width='80%' align='center'>" +
						"<tr><th>Property Address</th><th>Property Description</th><th>Account#</th></tr>");
				if(StringUtils.isEmpty(parsedResponse.getFooter())) {
					parsedResponse.setFooter("</table>");
				}
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_SEARCH_BY_PARCEL:
			case ID_SEARCH_BY_TAX_BIL_NO:
				// no result
				if (rsResponse.contains("The account is not found") || rsResponse.contains("The bill is not found")) {
					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				
            	parsedResponse.setHeader("");
				parsedResponse.setFooter("");
				
				outputTable = new StringBuilder();
				outputTable.append(viParseID);
				smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				if(smartParsedResponses.size() == 0) {
					return;
				}
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());

				if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
					String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST")
							+ "<table style='border-collapse: collapse' border='2' width='80%' align='center'>"
							+ parsedResponse.getHeader();
					String footer = parsedResponse.getFooter() + "\n</table><br>";
					Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
	            	
					if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
	            		footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer)numberOfUnsavedDocument);
	            	} else {
	            		footer += CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
	            	}
					
					parsedResponse.setHeader(header);
					parsedResponse.setFooter(footer);
				}
				
				break;
			case ID_GET_LINK:
				if(rsResponse.contains("Bill Type") && rsResponse.contains("Balance")) {
					ParseResponse(sAction, Response, ID_SEARCH_BY_PARCEL);
				} else if(rsResponse.contains("REAL PROPERTY CONSOLIDATED") || rsResponse.contains("REDEMPTION INFORMATION")) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
				}
				break;
			case ID_DETAILS:
				StringBuilder accountId = new StringBuilder();
				StringBuilder year = new StringBuilder();
				String details = "";
				
				if(rsResponse.contains("REAL PROPERTY CONSOLIDATED")) {
					details = getDetails(rsResponse, accountId, year);
				} else if(rsResponse.contains("REDEMPTION INFORMATION")) {
					// TAX LIEN SALE (account 01927516)
					details = getTaxLienDetails(rsResponse, accountId, year);
				}
				
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","CNTYTAX");
				data.put("year", year.toString());
				if(details.contains("TAX LIEN SALE")) {
					data.put("dataSource", "TU");
				} else {
					data.put("dataSource", "TR");
				}
				
				if (isInstrumentSaved(accountId.toString().trim(), null, data, false)){
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parsedResponse.setResponse(details);
				
				break;
			case ID_SAVE_TO_TSD:
				accountId = new StringBuilder();
				year = new StringBuilder();
				details = "";
				
				if(rsResponse.contains("REAL PROPERTY CONSOLIDATED")) {
					details = getDetails(rsResponse, accountId, year);
				} else if(rsResponse.contains("REDEMPTION INFORMATION")) {
					// TAX LIEN SALE (account 01927516)
					details = getTaxLienDetails(rsResponse, accountId, year);
				} else if(rsResponse.contains("Bill Type") && rsResponse.contains("Balance")) {
					// searching by address in automatic
					details = getFirstYearDetails(rsResponse, accountId, year);
				}
				
				String filename = accountId + ".html";
				smartParseDetails(Response,details);
				
				msSaveToTSDFileName = filename;
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
				parsedResponse.setResponse(details);
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				
				break;
		}
	}

	/**
	 * get the details for records of type TLR
	 */
	private String getTaxLienDetails(String rsResponse,
			StringBuilder accountId, StringBuilder year) {
		StringBuilder details = new StringBuilder();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag table = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "650")).elementAt(0);
			
			if(table.getRowCount() > 3) {
				TableTag table1 = (TableTag) table.getRow(3).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
				
				if(table1.getRowCount() == 2) {
					TableRow row = table1.getRow(1);
					
					if(row.getColumnCount() == 7) {
						accountId.append(row.getColumns()[6].toPlainTextString().trim());
						String billNo = row.getColumns()[5].toPlainTextString().trim();
						String _year = (String) mSearch.getAdditionalInfo(billNo);
						
						if(_year != null) {
							details.append("<div align='center'><b>LEVY YEAR " + _year + "</b></div>");
							
							year.append(_year);
						}
					}
				}
			}
			
			details.append(table.toHtml());
		} catch(Exception e) {
			logger.error("Error while getting details", e);
		}
		
		String detailsHtml = details.toString();
		detailsHtml = detailsHtml.replaceAll("(?is)<a [^>]*>.*?</a>", "");
		detailsHtml = detailsHtml.replaceAll("(?is)<img [^>]*>", "");
		
		return detailsHtml;
	}

	/** 
	 * this is called when searching by address in automatic
	 */
	private String getFirstYearDetails(String rsResponse, StringBuilder accountId, StringBuilder year) {
		String lastYearPage = "";
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), false).elementAt(0);
			if(interTable == null) {
				return "";
			}

			TableRow[] rows = interTable.getRows();

			for(int i = 0; i < rows.length; i++) {
				TableRow row = rows[i];
				
				if(row.getColumnCount() == 6) {
					LinkTag linkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
					if(linkTag != null) {
						DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
						String lastYearPageLink = dataSite.getAlternateLink() + linkTag.getLink();
						lastYearPage = getLinkContents(lastYearPageLink);
						break;
					}
				}
			}
		} catch (Exception e) {
			logger.error("ERROR while parsing details");
		}
		
		return getDetails(lastYearPage, accountId, year);
	}
	
	private String getDetails(String rsResponse, StringBuilder accountId, StringBuilder year) {
		StringBuilder details = new StringBuilder();
		boolean startData = false;
		int crtYear = -1;
		String billNo = "";
		
		Matcher m = Pattern.compile("(?is)name=\"ParcelCode\"\\s+value=\"(\\d+)\"").matcher(rsResponse);
		if(m.find()) {
			accountId.append(m.group(1));
		}
		m = Pattern.compile("(?is)name=\"bill\"\\s+value=\"(\\d+)\"").matcher(rsResponse);
		if(m.find()) {
			billNo = m.group(1);
		}
		m = Pattern.compile("(?is)LEVY\\s+YEAR\\s*&nbsp;(\\d+)").matcher(rsResponse);
		if(m.find()) {
			year.append(m.group(1));
			crtYear = Integer.parseInt(m.group(1));
		}
		
		if(!rsResponse.toLowerCase().contains("<html")) { // doc from memory
			return rsResponse;
		}
		
		details.append("<table style='border-collapse: collapse' border='2' width='616' align='center'>");
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag detailsTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "616")).elementAt(0);
			
			TableRow[] rows = detailsTable.getRows();
			
			for(int i = 0; i < rows.length; i++) {
				TableRow row = rows[i];
				String rowText = row.toPlainTextString();
				
				if(startData) {
					details.append(row.toHtml());
				} else {
					if(rowText.contains("REAL PROPERTY CONSOLIDATED")) {
						details.append(row.toHtml());
						startData = true;
					}
				}
			}
			
			details.append("CRAP_HTML_MARK");
			
			//get tax data from previous years
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
			String billsPageLink = dataSite.getAlternateLink() + "ViewParcel.asp?ParcelCode=" + accountId.toString();
			String billsPage = getLinkContents(billsPageLink);
			
			htmlParser = org.htmlparser.Parser.createParser(billsPage, null);
			nodeList = htmlParser.parse(null);
			
			TableTag billsTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), false).elementAt(0);
			if(billsTable != null) {
				rows = billsTable.getRows();
				
				for(int i = 0; i < rows.length; i++) {
					TableRow row = rows[i];
					String rowText = row.toPlainTextString();
					
					if(row.getColumnCount() < 6 || rowText.contains("Levy Year")) {
						continue;
					}
					
					try {
						int yearInt = Integer.parseInt(row.getColumns()[0].toPlainTextString().trim());
						if(yearInt > crtYear) {
							continue;
						}
					} catch (Exception e) {
						logger.error("Error while getting details", e);
					}
					
					if(billNo.equals(row.getColumns()[2].toPlainTextString().trim())) {
						continue;
					}
					
					LinkTag linkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
					String billLink = dataSite.getAlternateLink() + linkTag.getLink();
					String billPage = getLinkContents(billLink);
					
					org.htmlparser.Parser htmlParser1 = org.htmlparser.Parser.createParser(billPage, null);
					NodeList nodeList1 = htmlParser1.parse(null);
					
					Node infoTable = nodeList1.extractAllNodesThatMatch(new TagNameFilter("td"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("width", "80%")).elementAt(0);
					Node taxTable = nodeList1.extractAllNodesThatMatch(new TagNameFilter("td"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("width", "80%")).elementAt(1);
					
					if(infoTable != null) {
						details.append("<tr>" + infoTable.toHtml() + "</tr>");
					}
					if(taxTable != null && taxTable.toPlainTextString().contains("TAX DESCRIPTION")) {
						details.append("<tr>" + taxTable.toHtml() + "</tr>");
					}
					
				}
			}
			
		} catch(Exception e) {
			logger.error("Error while getting details", e);
		}

		// some cleaning (the original html is pretty broken)
		String detailsHtml = details.toString().replaceFirst("(?is)<tr>\\s*<td\\s*align=center>\\s*<b>\\s*SELECT PAYMENT OPTION.*?CRAP_HTML_MARK",
				"<tr><td><table style='border-collapse: collapse' border='2' width='616' align='center' id='prevYears'>");
		detailsHtml += "</table></td></tr></table>";
		detailsHtml = detailsHtml.replaceAll("(?is)<img [^>]*>", "");
		
		return detailsHtml;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String page, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		ParsedResponse parsedResponse = response.getParsedResponse();
		int viParseID = Integer.parseInt(outputTable.toString());
		outputTable.setLength(0);
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			if(viParseID == ID_SEARCH_BY_ADDRESS) {
				TableTag interTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("width", "98%"), false).elementAt(1);
				if(interTable == null) {
					return intermediaryResponse;
				}
				
				TableRow[] rows = interTable.getRows();
				
				for(int i = 0; i < rows.length; i++) {
					TableRow row = rows[i];
					String rowText = row.toPlainTextString();
					
					if(rowText.contains("MORE>>") || rowText.contains("<<BACK")){
						NodeList links = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						
						for(int j = 0; j < links.size(); j++) {
							LinkTag linkTag = (LinkTag) links.elementAt(j);
							if(linkTag.toPlainTextString().contains("MORE>>") || linkTag.toPlainTextString().contains("<<BACK")) {
								linkTag.setLink(CreatePartialLink(TSConnectionURL.idGET) + "/apps/tax/" + linkTag.getLink());
							}
							if(linkTag.toPlainTextString().contains("MORE>>")) {
								parsedResponse.setNextLink(linkTag.getLink());
							}
						}
						
						parsedResponse.setFooter(row.toHtml() + "</table>");
						break;
					}
					
					LinkTag linkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
					if(linkTag == null) {
						continue;
					}
					linkTag.setLink(CreatePartialLink(TSConnectionURL.idGET) + "/apps/tax/" + linkTag.getLink());
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(linkTag.getLink(), linkTag.getLink(), TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap m = ro.cst.tsearch.servers.functions.MDMontgomeryTR.parseIntermediaryRow(row);
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
				
				outputTable.append(interTable.toHtml());
				
			} else {
				TableTag interTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), false).elementAt(0);
				if(interTable == null) {
					return intermediaryResponse;
				}
				
				String accountNo = "";
				String prevYear = "";
				String prevLink = "";
				int numberOfUncheckedElements = 0;
				TableRow[] rows = interTable.getRows();
				
				for(int i = 0; i < rows.length; i++) {
					TableRow row = rows[i];
					String rowText = row.toPlainTextString();
					ParsedResponse currentResponse = new ParsedResponse();
					
					if(row.getColumnCount() < 6 || rowText.contains("Levy Year")) {
						if(intermediaryResponse.size() == 0) {
							if(rowText.contains("Levy Year")) {
								parsedResponse.setHeader(parsedResponse.getHeader() 
										+ row.toHtml().replaceFirst("(?is)<tr>", "<tr><td>"+ SELECT_ALL_CHECKBOXES + "</td>"));
							} else {
								parsedResponse.setHeader(parsedResponse.getHeader() + row.toHtml().replace("colspan=6", "colspan=7"));
							}
						} else {
							parsedResponse.setFooter(parsedResponse.getFooter() + row.toHtml().replace("colspan=6", "colspan=7"));
						}
						
						if(rowText.contains("Account Number:")) {
							accountNo = rowText.replaceAll("[^\\d]+", "");
						}
						continue;
					}
					
					// we concatenate docs having type H, Q, T with docs having type A for the same year
					String year = row.getColumns()[0].toPlainTextString().trim();
					String type = row.getColumns()[1].toPlainTextString().trim();
					
					if(year.equals(prevYear) && !type.equals("TLR")) {
						currentResponse = intermediaryResponse.get(intermediaryResponse.size() - 1);
						
						NodeList links = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						LinkTag linkTag = null;
						for(int j = 0; j < links.size(); j++) {
							linkTag = (LinkTag) links.elementAt(j);
							linkTag.setLink(prevLink);
						}
						
						String prevRowHtml = (String) currentResponse.getAttribute(ParsedResponse.SERVER_ROW_RESPONSE);
						String rowHtml = "<tr>";
						for(int j = 1; j < 6; j++) {
							rowHtml += row.getColumns()[j].toHtml();
						}
						rowHtml += "</tr>";
						prevRowHtml = prevRowHtml.replace("</table>", rowHtml + "</table>");
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, prevRowHtml);
						currentResponse.setOnlyResponse(prevRowHtml);
					} else {
						NodeList links = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
						LinkTag linkTag = null;
						for(int j = 0; j < links.size(); j++) {
							linkTag = (LinkTag) links.elementAt(j);
							linkTag.setLink(CreatePartialLink(TSConnectionURL.idGET) + "/apps/tax/" + linkTag.getLink());
							prevLink = linkTag.getLink();
						}
						
						// TLR docs don't have year on details page, so we set it here
						mSearch.setAdditionalInfo(row.getColumns()[2].toPlainTextString().trim(), row.getColumns()[0].toPlainTextString().trim());
						
						HashMap<String, String> data = new HashMap<String, String>();
						data.put("type","CNTYTAX");
						data.put("year", row.getColumns()[0].toPlainTextString().trim());
						
						if(row.getColumns()[1].toPlainTextString().trim().equals("A")) {
							data.put("dataSource", "TR");
						} else if(row.getColumns()[1].toPlainTextString().trim().equals("TLR")) {
							data.put("dataSource", "TU");
						}
						
						String checkBox = "checked";
						
						if (isInstrumentSaved(accountNo, null, data, false) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
							checkBox = "saved";
						} else {
							numberOfUncheckedElements++;
							
							if(linkTag != null) {
								checkBox = "<input type='checkbox' name='docLink' value='" + linkTag.getLink() + "'>";
								currentResponse.setPageLink(new LinkInPage(linkTag.getLink(), linkTag.getLink(), TSServer.REQUEST_SAVE_TO_TSD));
							}
						}
						
						String innerTable = "<table style='border-collapse: collapse' border='1' width='100%' align='center'><tr>";
						for(int j = 1; j < 6; j++) {
							innerTable += row.getColumns()[j].toHtml();
						}
						innerTable += "</tr></table>";
						
						String rowHtml = "<tr><td>"+ checkBox + "</td><td align='center'>" + year + "</td><td colspan='5'>" + innerTable + "</td></tr>"; 
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
						currentResponse.setOnlyResponse(rowHtml);
						
						ResultMap m = ro.cst.tsearch.servers.functions.MDMontgomeryTR.parseIntermediaryRow(row);
						m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNo);
						Bridge bridge = new Bridge(currentResponse, m, searchId);
						
						DocumentI document = (TaxDocumentI)bridge.importData();				
						currentResponse.setDocument(document);
						
						intermediaryResponse.add(currentResponse);
					}
					prevYear = year;
					
				}
				
				outputTable.append(interTable.toHtml());
				SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			}
		} catch(Exception e) {
			logger.error("Error while parsing intermediary results", e);
		}
		
		return intermediaryResponse;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		
		if(detailsHtml.contains("REAL PROPERTY CONSOLIDATED")) {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			
			Matcher m = Pattern.compile("(?is)name=\"ParcelCode\"\\s+value=\"(\\d+)\"").matcher(detailsHtml);
			if(m.find()) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), m.group(1));
			}
			String crtYear = "";
			m = Pattern.compile("(?is)LEVY\\s+YEAR\\s*&nbsp;(\\d+)").matcher(detailsHtml);
			if(m.find()) {
				crtYear = m.group(1);
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), m.group(1));
			}
			
			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
				NodeList nodeList = htmlParser.parse(null);
				
				TableTag mainTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("width", "616"), false).elementAt(0);
				
				if(mainTable == null) {
					return null;
				}
				
				String address = "";
				String legal = "";
				String owner = "";
				String lot = "";
				String block = "";
				double delinq = 0.0;
				TableRow[] rows = mainTable.getRows();
				
				for(int i = 0; i < rows.length; i++) {
					TableRow row = rows[i];
					String rowText = row.toPlainTextString().replaceAll("(?is)&nbsp;", " ");
					
					if(rowText.contains("ACCOUNT NUMBER")) {
						TableTag innerTable = (TableTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.elementAt(0);
						TableTag innerTable1 = (TableTag) innerTable.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.elementAt(1);
						
						address = innerTable1.getRow(0).getColumns()[2].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
						address = address.replaceFirst("([\\d-]+)$", "#$1"); // mark the unit
						
						resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
						resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
						
						String billNo = innerTable1.getRow(0).getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
						resultMap.put(TaxHistorySetKey.TAX_BILL_NUMBER.getKeyName(), billNo);
					} else if(rowText.contains("PROPERTY DESCRIPTION")) {
						TableTag innerTable = (TableTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.elementAt(0);
						TableTag innerTable1 = (TableTag) innerTable.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.elementAt(0);
						TableTag innerTable2 = (TableTag) innerTable.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.elementAt(1);
						
						legal = innerTable1.getRow(1).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
						owner = innerTable2.getRow(0).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
						
						String addOwner = innerTable2.getRow(1).toPlainTextString().replaceAll("(?is)&nbsp;.*", "").trim();
						if(owner.endsWith("&") || owner.endsWith("TO THE USE OF") || addOwner.startsWith("C/O") || addOwner.startsWith("%")) {
							if(StringUtils.isNotEmpty(addOwner)) {
								owner = owner.replaceFirst("\\s+TO THE USE OF$", "");
								owner += "\n" + addOwner.replaceFirst("(C/O|%)\\s*", "");
							}
						}
					} else if(rowText.contains("TAX DESCRIPTION") && rowText.contains("REFUSE AREA")) {
						TableTag innerTable = (TableTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.elementAt(0);
						TableTag innerTable1 = (TableTag) innerTable.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.elementAt(0);
						TableTag innerTable2 = (TableTag) innerTable.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.elementAt(1);
						// parse lot/block table
						TableRow[] rows1 = innerTable1.getRows();
						for(int j = 0; j < rows1.length; j++) {
							TableRow row1 = rows1[j];
							
							if(row1.getColumnCount() != 2) {
								continue;
							}
							
							String col1 = row1.getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
							String col2 = row1.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
							
							if(col1.contains("LOT")) {
								lot = col2;
							} else if(col1.contains("BLOCK")) {
								block = col2;
							}
						}
						// parse tax description table
						TableRow[] rows2 = innerTable2.getRows();
						for(int j = 0; j < rows2.length; j++) {
							TableRow row2 = rows2[j];
							
							if(row2.getColumnCount() != 4) {
								continue;
							}
							
							String col1 = row2.getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
							String col4 = row2.getColumns()[3].toPlainTextString().replaceAll("(?is)&nbsp;", " ").replaceAll(",", "").trim();
							
							if(col1.contains("TOTAL AMOUNT")) {
								resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), col4);
							} else if(col1.matches("\\s*TOTAL\\s*")) {
								resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), col4);
							} else if(col1.contains("PRIOR PAYMENTS")) {
								resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), col4);
							}
						}
					}
				}
				// compute delinquent amount
				TableTag prevYearsTable = (TableTag) nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "prevYears"), true).elementAt(0);
				
				if(prevYearsTable != null) {
					rows = prevYearsTable.getRows();
					String year = "";
					
					for(int i = 0; i < rows.length; i++) {
						TableRow row = rows[i];
						String rowText = row.toPlainTextString().replaceAll("(?is)&nbsp;", " ");
						
						if(rowText.contains("LEVY YEAR")) {
							m = Pattern.compile("(?is)LEVY\\s+YEAR\\s*&nbsp;(\\d+)").matcher(row.toHtml());
							if(m.find()) {
								year = m.group(1);
							}
						} else if(rowText.contains("TAX DESCRIPTION")) {
							TableTag innerTable = (TableTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.elementAt(0);
							// parse delinquent amount
							TableRow[] rows1 = innerTable.getRows();
							for(int j = 0; j < rows1.length; j++) {
								TableRow row1 = rows1[j];
								
								if(row1.getColumnCount() != 4) {
									continue;
								}
								
								String col1 = row1.getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
								String col4 = row1.getColumns()[3].toPlainTextString().replaceAll("(?is)&nbsp;", " ").replaceAll(",", "").trim();
								
								if(!col4.matches(".*\\d+.*")) {
									continue;
								}
								
								if(year.equals(crtYear)) {
									// we can arrive here for bills of type H, Q, T (e.g. accNo 00479960)
									double col4Val = 0d;
									try {
										col4Val = Double.parseDouble(col4);
									
										if(col1.contains("TOTAL AMOUNT")) {
											col4Val += Double.parseDouble((String) resultMap.get(TaxHistorySetKey.TOTAL_DUE.getKeyName()));
											resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), col4Val + "");
										} else if(col1.matches("\\s*TOTAL\\s*")) {
											col4Val += Double.parseDouble((String) resultMap.get(TaxHistorySetKey.BASE_AMOUNT.getKeyName()));
											resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), col4Val + "");
										} else if(col1.contains("PRIOR PAYMENTS")) {
											col4Val += Double.parseDouble((String) resultMap.get(TaxHistorySetKey.AMOUNT_PAID.getKeyName()));
											resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), col4Val + "");
										}
									} catch (Exception e) {
										logger.error("ERROR while parsing details", e);
									}
								} else {
									if(col1.contains("TOTAL AMOUNT")) {
										try {
											delinq += Double.parseDouble(col4);
										} catch (Exception e) {
											logger.error("ERROR while parsing details", e);
										}
									}
								}
							}
						}
					}
				}
				
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
				resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), String.valueOf(delinq));
				
				ro.cst.tsearch.servers.functions.MDMontgomeryTR.parseLegal(resultMap, legal);
				ro.cst.tsearch.servers.functions.MDMontgomeryTR.parseOwners(resultMap, owner);
			} catch(Exception e) {
				logger.error("ERROR while parsing details", e);
			}
		} else if(detailsHtml.contains("TAX LIEN SALE")) {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TU");
			
			Matcher m = Pattern.compile("(?is)LEVY\\s+YEAR\\s+(\\d+)").matcher(detailsHtml);
			if(m.find()) {
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), m.group(1));
			}
			
			try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
				NodeList nodeList = htmlParser.parse(null);
				
				TableTag mainTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("width", "650"), false).elementAt(0);
				
				if(mainTable == null) {
					return null;
				}
				
				String address = "";
				String legal = "";
				String owner = "";
				String lot = "";
				String block = "";
				String accountNo = "";
				String billNo = "";
				TableRow[] rows = mainTable.getRows();
				
				for(int i = 0; i < rows.length; i++) {
					TableRow row = rows[i];
					String rowText = row.toPlainTextString().replaceAll("(?is)&nbsp;", " ");
					
					if(rowText.contains("Property Description")) {
						TableTag innerTable = (TableTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.elementAt(0);
						TableTag innerTable1 = (TableTag) innerTable.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.elementAt(0);
						TableTag innerTable2 = (TableTag) innerTable.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.elementAt(1);
						// parse owner
						owner = innerTable1.getRow(0).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();

						String addOwner = innerTable1.getRow(1).toPlainTextString().replaceAll("(?is)&nbsp;.*", "").trim();
						if(owner.endsWith("&") || owner.endsWith("TO THE USE OF") || addOwner.startsWith("C/O") || addOwner.startsWith("%")) {
							if(StringUtils.isNotEmpty(addOwner)) {
								owner = owner.replaceFirst("\\s+TO THE USE OF$", "");
								owner += "\n" + addOwner.replaceFirst("(C/O|%)\\s*", "");
							}
						}
						// parse legal
						legal = innerTable2.getRow(1).toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
					} else if(rowText.contains("Account Number")) {
						TableTag innerTable = (TableTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.elementAt(0);
						
						lot = innerTable.getRow(1).getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
						block = innerTable.getRow(1).getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
						accountNo = innerTable.getRow(1).getColumns()[6].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
						billNo = innerTable.getRow(1).getColumns()[5].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
						
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNo);
						resultMap.put(TaxHistorySetKey.TAX_BILL_NUMBER.getKeyName(), billNo);
					} else if(rowText.contains("Property Address")) {
						TableTag innerTable = (TableTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.elementAt(0);
						
						address = innerTable.getRow(1).getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
						address = address.replaceFirst("([\\d-]+)$", "#$1"); // mark the unit
						
						resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
						resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
					} else if(rowText.contains("Tax Lien Sale Amount")) {
						TableTag innerTable = (TableTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.elementAt(0);
						TableTag innerTable1 = (TableTag) innerTable.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.elementAt(0);
						
						TableRow[] rows1 = innerTable1.getRows();
						for(int j = 0; j < rows1.length; j++) {
							TableRow row1 = rows1[j];
							
							if(row1.getColumnCount() != 2) {
								continue;
							}
							
							String col1 = row1.getColumns()[0].toPlainTextString().replaceAll("(?is)&nbsp;", " ").trim();
							String col2 = row1.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", " ").replaceAll(",", "").trim();
							
							if(col1.contains("Tax Lien Sale Amount")) {
								resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), col2);
							} else if(col1.contains("Tax Lien Redemption Amount to Date")) {
								resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), col2);
								break;
							}
						}
					}
				}
				
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
				
				ro.cst.tsearch.servers.functions.MDMontgomeryTR.parseLegal(resultMap, legal);
				ro.cst.tsearch.servers.functions.MDMontgomeryTR.parseOwners(resultMap, owner);
			} catch(Exception e) {
				logger.error("ERROR while parsing details", e);
			}
		}
		
		return null;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		TSServerInfoModule module;
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId);
		
		// search by account number
		if(hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO_GENERIC_TR);
			module.addFilter(taxYearFilter);
			modules.add(module);
		}
		
		// search by Address
		if(hasStreet() && hasStreetNo()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.P_STREET_NO_NAME);

			module.addFilter(addressFilter);

			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	/**
	 * we save all years that have balance > 0 as separate docs
	 */
	@Override
	public void addAdditionalDocuments(DocumentI doc, ServerResponse response) {
		if(!(mSearch.getSearchType() == Search.AUTOMATIC_SEARCH)) {
			return;
		}
		
		TaxDocumentI taxDoc = (TaxDocumentI) doc;
		String billNo = taxDoc.getBillNumber();
		String accountId = taxDoc.getInstno();
		
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
		String billsPageLink = dataSite.getAlternateLink() + "ViewParcel.asp?ParcelCode=" + accountId.toString();
		String billsPage = getLinkContents(billsPageLink);
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(billsPage, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag billsTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), false).elementAt(0);
			if(billsTable != null) {
				TableRow[] rows = billsTable.getRows();
				boolean foundBill = false;
				
				for(int i = 0; i < rows.length; i++) {
					TableRow row = rows[i];
					
					if(row.getColumnCount() != 6 || !row.getColumns()[0].toPlainTextString().trim().matches("\\d+")) {
						continue;
					}
					
					String _billNo = row.getColumns()[2].toPlainTextString().trim();
					if(billNo.equals(_billNo)) {
						foundBill = true;
						continue;
					}
					
					if(!foundBill) {
						continue;
					}
					
					double balance = Double.parseDouble(row.getColumns()[4].toPlainTextString().trim().replaceAll(",", ""));
					if(balance > 0.0) {
						LinkTag linkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
						String billLink = dataSite.getAlternateLink() + linkTag.getLink();
						String billPage = getLinkContents(billLink);
						
						ServerResponse Response = new ServerResponse();
						Response.setResult(billPage);
						
						StringBuilder accountNo = new StringBuilder();
						StringBuilder year = new StringBuilder();
						String detailsHtml = "";
						
						if(billPage.contains("REAL PROPERTY CONSOLIDATED")) {
							detailsHtml = getDetails(billPage, accountNo, year);
						} else if(billPage.contains("REDEMPTION INFORMATION")) {
							// TAX LIEN SALE (account 01927516)
							detailsHtml = getTaxLienDetails(billPage, accountNo, year);
						}
						
						String sAction = "/" + billsPageLink;
							
						super.solveHtmlResponse(sAction, ID_SAVE_TO_TSD, "SaveToTSD", Response, detailsHtml);
						
						break;
					}
				}
			}
		} catch (Exception e) {
			logger.error("ERROR while saving additional docs", e);
		}
	}
}
