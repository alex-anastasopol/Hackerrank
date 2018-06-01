/**
 * 
 */
package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.ResultBodyUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PropertyI;

/**
 * @author vladb
 *
 */
public class COGunnisonAO extends TSServer {

	private static final long serialVersionUID = 1L;

	public COGunnisonAO(long searchId) {
		super(searchId);
	}

	public COGunnisonAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_PARCEL:	
			case ID_SEARCH_BY_PROP_NO:
			case ID_SEARCH_BY_SUBDIVISION_NAME:
			case ID_SEARCH_BY_SUBDIVISION_PLAT:
			case ID_SEARCH_BY_BOOK_AND_PAGE:
			case ID_SEARCH_BY_CONDO_NAME:
				// no result
				if (rsResponse.indexOf("No Records Found") > -1 
						|| rsResponse.indexOf("No Data Found") > -1) {
					Response.getParsedResponse().setError("No Data Found!");
					return;
				} else if (rsResponse.indexOf("Could Not Read Data") > -1) {
					Response.getParsedResponse().setError("Invalid parameter(s)!");
					return;
				}
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() == 0) {
					return;
				}
				
				String nextLink = processLink(rsResponse);
				
				String header = extractHeader(rsResponse);
				String colspan = "4";
				Matcher ma = Pattern.compile("(?s)(\\d+)@@@(.*)").matcher(header);
				if (ma.matches()) {
					colspan = ma.group(1);
					header = ma.group(2);
				}
				
				parsedResponse.setHeader("<table width=\"100%\" border=\"1\">" + header);	
				parsedResponse.setFooter(nextLink==null ? "</table>" : "<tr><td colspan=\"" + colspan + "\" align=\"center\">" + nextLink + "</td></tr></table>");
				parsedResponse.setNextLink(nextLink);
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_GET_LINK:
				if (rsResponse.indexOf("Owner and Parcel Information") > -1) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				break;
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				
				StringBuilder accountId = new StringBuilder();
				String details = getDetails(rsResponse, accountId);
				
				if(viParseID == ID_DETAILS) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
	
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","ASSESSOR");
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
	
	private String extractHeader(String page) {
		
		String header = "";

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = (TableTag) nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.elementAt(1);
			
			TableRow[] rows = interTable.getRows();
			
			for(int i = 0; i < rows.length; i++) {
				
				TableRow row = rows[i];
				
				//remove 'GIS Map' column
				int index = -1;
				for (int j=0;j<row.getChildCount()-1;j++) {
					if ("GIS Map".equalsIgnoreCase(row.childAt(j).toPlainTextString().replaceAll("(?i)&nbsp;", "").trim()) && 
							"".equals(row.childAt(j+1).toPlainTextString().replaceAll("(?i)&nbsp;", "").trim())) {
						index = j;
						break;
					}
				}
				if (index!=-1) {
					row.removeChild(index);
					row.removeChild(index);
				}
				
				if((row.toPlainTextString().toUpperCase().indexOf("ACCOUNT NUMBER") > -1
					|| row.toPlainTextString().toUpperCase().indexOf("\\/ACCOUNT") > -1)
					&& row.getColumnCount() > 1) {
					
					header = row.getColumnCount() + "@@@" + row.toHtml()
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
	
	private String getDetails(String page, StringBuilder accountId) {
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "table_class"));
			
			StringBuilder details = new StringBuilder();
			details.append(
				"<style type=\"text/css\">" +
					"td {}" +
					".table_header {text-align: center; font-size: 14px; font-weight: bold; }" +
				"</style>");
			for(int i = 0; i < tables.size(); i++) {
				TableTag table = (TableTag) tables.elementAt(i);
				
				if(table.toPlainTextString().indexOf("Gunnison Assessor Home") > -1
					|| table.toPlainTextString().indexOf("The Gunnison County Assessor's Office") > -1
					|| table.toPlainTextString().matches("\\s*")) {
						
					continue;
				}
				
				if(table.toPlainTextString().indexOf("Owner and Parcel Information") > -1) {
					
					String accId = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Account Number:"), "", true);
					accountId.append(accId.replace("&nbsp;", "").trim());
				}
				
				table.setAttribute("border", "1");
				table.setAttribute("width", "100%");
				details.append(table.toHtml() + "<br><br><br>");
			}
			//remove links
			String detailsString = details.toString().replaceAll("(?is)<a\\s+.*?>(.*?)</a>", "$1");
			//remove images
			detailsString = detailsString.replaceAll("(?is)<img\\s+.*?>", "");
			//perform some cleaning
			detailsString = detailsString.replaceAll("(?is)bgcolor=\\W+", "")
				.replace("Hide Building Details", "");
			
			return detailsString;
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private String processLink(String page) {
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			int index = 0;
			boolean found = false;
			
			NodeList links = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "cell_value"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("colspan", "15"), true);
			for (int i=0;i<links.size();i++) {
				NodeList children = links.elementAt(i).getChildren();
				if (children.size()>index) {
					LinkTag linkTag = (LinkTag) children.elementAt(index);
					if(linkTag.toPlainTextString().indexOf("Search Next 50 Parcels") > -1) {
						found = true;
						break;
					}
				}
			}
			
			if (!found) {
				links = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "header_link"), true);
				index = 1;
			}
			
			for(int i = 0; i < links.size(); i++) {
				NodeList children = links.elementAt(i).getChildren();
				if (children.size()>index) {
					LinkTag linkTag = (LinkTag) children.elementAt(index);
					if(linkTag.toPlainTextString().indexOf("Search Next 50 Parcels") > -1) {
						String link = CreatePartialLink(TSConnectionURL.idGET) + linkTag.extractLink().trim().replaceAll("\\s", "%20");
						return "<a href=\"" + link + "\">Next 50 Results</a>";
					}
				}
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		
		resultMap.put("OtherInformationSet.SrcType","AO");
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList tables = nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true);
			
			String ownerName = "";
			String address = "";
			String legal = "";
			String accountId = "";
			
			ownerName = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Owner Name & Mailing Address"), "", true);
			ownerName = ownerName.replaceAll("(?is)&nbsp;", "").trim();
			String[] split = ownerName.split("\n");		//keep only name, without address
			ownerName = split[0];
			String businessName = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Business Name:"), "", true);
			businessName = businessName.replaceAll("(?is)&nbsp;", "").trim().replaceFirst("(?is)^NA$", "");
			if (!StringUtils.isEmpty(businessName)) {
				ownerName += "<BR>" + businessName;
			}
			ownerName = ownerName.replaceAll("(?is)<BR>{2,}", "");
			if (!StringUtils.isEmpty(ownerName)) {
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), ownerName);
			}
			
			String year = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Appraisal Year:"), "", true);
			year = year.replaceAll("(?is)&nbsp;", "").trim();
			if (!StringUtils.isEmpty(year)) {
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
			}
			
			accountId = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Account Number:"), "", true);
			accountId = accountId.replaceAll("(?is)&nbsp;", "").trim();
			if (!StringUtils.isEmpty(accountId)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountId);
			}
			
			String parcelNumber = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Parcel Number:"), "", true);
			parcelNumber = parcelNumber.replaceAll("(?is)&nbsp;", "").trim().replaceAll("-", "");
			if (!StringUtils.isEmpty(parcelNumber)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), parcelNumber);
			}
			
			address = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Property Location:"), "", true);
			address = address.replaceAll("(?is)&nbsp;", "").trim();
			address = address.replaceFirst("^,$", "");
			if (!StringUtils.isEmpty(address)) {
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			}
			
			String subdivision = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Subdivision:"), "", true);
			subdivision = subdivision.replaceAll("(?is)&nbsp;", "").trim();
			subdivision = subdivision.replaceAll("PHASE\\s+[A-Z\\d]+", "").trim();
			if (!StringUtils.isEmpty(subdivision)) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
			}
			
			String condo = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Condo:"), "", true);
			condo = condo.replaceAll("(?is)&nbsp;", "").trim();
			if (!StringUtils.isEmpty(condo)) {
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), condo);
			}
			
			legal = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Legal Description:"), "", true);
			legal = legal.replaceAll("(?is)&nbsp;", "").trim();
			String parcelNotes = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "Parcel Notes"), "", true);
			parcelNotes = parcelNotes.replaceAll("(?is)&nbsp;", "").trim();
			if (!StringUtils.isEmpty(parcelNotes)) {
				legal += " " + parcelNotes;
			}
			if (!StringUtils.isEmpty(legal)) {
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
			}
			
			String landValue = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Land Value"), "", true);
			landValue = landValue.replaceAll("(?is)&nbsp;", "").replaceAll("[,$]", "").trim();
			if (!StringUtils.isEmpty(landValue)) {
				resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), landValue);
			}
			
			String buildingValue = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Building Value"), "", true);
			buildingValue = buildingValue.replaceAll("(?is)&nbsp;", "").replaceAll("[,$]", "").trim();
			if (!StringUtils.isEmpty(buildingValue)) {
				resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), buildingValue);
			}
			
			String assessedValue = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Assessed Value"), "", true);
			assessedValue = assessedValue.replaceAll("(?is)&nbsp;", "").replaceAll("[,$]", "").trim();
			if (!StringUtils.isEmpty(assessedValue)) {
				resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessedValue);
			}
			
			for(int i = 0; i < tables.size(); i++) {
				TableTag table = (TableTag) tables.elementAt(i);
				
				if(table.toPlainTextString().indexOf("Sales Information") > -1
						&& table.toPlainTextString().indexOf("No sales associated with this parcel") < 0) {
				
					int index = 0;
					boolean contains = table.getChild(index).toPlainTextString().contains("Sale Date") || 
						table.getChild(index).toPlainTextString().contains("SaleDate") || table.getChild(index).toPlainTextString().contains("Amount");
					while (!contains && table.getRows().length > index){
						table.removeChild(0);
						contains = table.getChild(index).toPlainTextString().contains("Sale Date") || 
							table.getChild(index).toPlainTextString().contains("SaleDate") || table.getChild(index).toPlainTextString().contains("Amount");
						index ++;
					}
					
					List<HashMap<String,String>> tableAsList = HtmlParser3.getTableAsListMap(table);
										
					String saleDateKey = "SaleDate";
					String amountKey = "SaleAmount";
					String grantorKey = "Grantor";
					String granteeKey = "Grantee";
					String instrKey = "Reception #";
					String docTypeKey = "DeedType";
					
					for (HashMap<String, String> hashMap : tableAsList) {
						String string = hashMap.get(amountKey);
						if (StringUtils.isNotEmpty(string)){
							hashMap.put(amountKey, ro.cst.tsearch.utils.StringUtils.cleanAmount(string).trim());
						}
					}
					
					String[] header = {SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName(), SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(), SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), 
							SaleDataSetKey.GRANTOR.getShortKeyName(), SaleDataSetKey.GRANTEE.getShortKeyName(), SaleDataSetKey.SALES_PRICE.getShortKeyName()  };					
					Map<String, String> resultBodyHeaderToSourceTableHeader = new HashMap<String,String>();
					resultBodyHeaderToSourceTableHeader.put(SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName(), saleDateKey);
					resultBodyHeaderToSourceTableHeader.put(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(), docTypeKey);
					resultBodyHeaderToSourceTableHeader.put(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), instrKey);
					resultBodyHeaderToSourceTableHeader.put(SaleDataSetKey.GRANTOR.getShortKeyName(), grantorKey);
					resultBodyHeaderToSourceTableHeader.put(SaleDataSetKey.GRANTEE.getShortKeyName(), granteeKey);
					resultBodyHeaderToSourceTableHeader.put(SaleDataSetKey.SALES_PRICE.getShortKeyName(), amountKey);
					
					ResultBodyUtils.buildInfSet(resultMap, tableAsList, header , resultBodyHeaderToSourceTableHeader , SaleDataSet.class);
					
				/*	List<List> body = new ArrayList<List>();
					for(int k = 2; k < table.getRowCount(); k++) {
						List<String> line = new ArrayList<String>();
						TableRow row = table.getRow(k);
						TableColumn[] columns = row.getColumns();
						for(int j = 0; j < columns.length; j++) {
							if(j == 5 || j == 6) {
								continue;
							}
							String value = columns[j].toPlainTextString().replaceAll("[,$]|&nbsp;","").trim();
							line.add(value);
						}
						String instr = line.get(2);
						String book = line.get(3);
						String page = line.get(4);
						boolean validBook = true;
						boolean validPage = true;
						if(book.matches("19\\d{2}|20\\d{2}")) {  //  year
							validBook = false;
						}
						if(page.matches("[A-Z]\\d{6}")) {
							validPage = false;
						}
						Matcher m = Pattern.compile("B(\\d*)P(\\d*)").matcher(instr);
						if(m.find()) {
							instr = "";
							if(!StringUtils.isEmpty(m.group(1)) && !StringUtils.isEmpty(m.group(2))
									&& !validBook && !validPage) {
								book = m.group(1);
								page = m.group(2);
								
							} else if(!StringUtils.isEmpty(m.group(1)) && StringUtils.isEmpty(m.group(2))
									&& validBook && !validPage) {  //  R012515
								if(book.equals(m.group(1))) {
									instr = book;
									book = "";
									page = "";
								}
							}
						} else if(!validPage) {
							book = "";
							page = "";
						}
						
						book = book.replaceAll("^0+", "");
						page = page.replaceAll("^0+", "");
						line.set(2, instr);
						line.set(3, book);
						line.set(4, page);
						body.add(line);
					}
					if(body != null && body.size() > 0) {
						ResultTable resultTable = new ResultTable();
						String[] header = {"InstrumentDate", "DocumentType", "InstrumentNumber", "Book", "Page", "Grantor", "Grantee", "SalesPrice"};
						resultTable = GenericFunctions2.createResultTable(body, header);
						resultMap.put("SaleDataSet", resultTable);
					}*/
				}
			}
			
			ro.cst.tsearch.servers.functions.COGunnisonAO.parseNames(resultMap, ownerName);
			ro.cst.tsearch.servers.functions.COGunnisonAO.parseAddress(resultMap, address);
			ro.cst.tsearch.servers.functions.COGunnisonAO.parseLegalSummary(resultMap, legal);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		try {
			
			table = Tidy.tidyParse(table, null);
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = (TableTag) nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.elementAt(1);
			
			if(interTable == null) {
				return intermediaryResponse;
			}
			
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(
					HashCountyToIndex.ANY_COMMUNITY,miServerID);
			
			String siteLink = dataSite.getLink();
			
			TableRow[] rows = interTable.getRows();
			TableRow header = null;
			
			for(int i = 0; i < rows.length; i++) {
				
				TableRow row = rows[i];
				
				//remove 'Map It'/'Map Not Available' column
				int index = -1;
				for (int j=0;j<row.getChildCount()-1;j++) {
					String colJ = row.childAt(j).toPlainTextString().replaceAll("(?i)&nbsp;", "").trim();
					String colJPlusOne = row.childAt(j+1).toPlainTextString().replaceAll("(?i)&nbsp;", "").trim();
					if ( ("Map It".equalsIgnoreCase(colJ) || "Map Not Available".equalsIgnoreCase(colJ) ) && 
							"".equals(colJPlusOne)) {
						index = j;
						break;
					}
				}
				if (index!=-1) {
					row.removeChild(index);
					row.removeChild(index);
				}
				
				String rowAsPlainText = row.toPlainTextString().toUpperCase();
				
				if(rowAsPlainText.indexOf("SEARCH CRITERIA") > -1  // skip additional rows
					|| rowAsPlainText.indexOf("SEARCH PRODUCED THE FOLLOWING RESULTS") > -1
					|| rowAsPlainText.indexOf("ASSESSOR HOME") > -1
					|| rowAsPlainText.indexOf("THE GUNNISON COUNTY ASSESSOR'S OFFICE") > -1
					|| rowAsPlainText.indexOf("RETURN TO MAIN SEARCH") > -1
					|| rowAsPlainText.indexOf("TO SORT A COLUMN") > -1
					|| rowAsPlainText.indexOf("OPEN RESULTS IN EXCEL") > -1
					|| rowAsPlainText.indexOf("PRINT MAILING LABELS") > -1
					|| rowAsPlainText.indexOf("RETURN TO MAIN SEARCH PAGE") > -1
					|| rowAsPlainText.indexOf("SEARCH NEXT 50 PARCELS") > -1
					|| rowAsPlainText.matches("\\s*")
					|| row.getColumnCount()==1) {
					
					continue;
				}
				
				if((rowAsPlainText.indexOf("ACCOUNT NUMBER") > -1
					|| rowAsPlainText.indexOf("\\/ACCOUNT") > -1)
					&& row.getColumnCount() > 1) {
					
					header = row;
					continue;
				}
				
				NodeList links = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
				LinkTag linkTag = (LinkTag) links.elementAt(0);
				String documentNumber = linkTag.getLinkText();
				
				ParsedResponse currentResponse = responses.get(documentNumber);							 
				if(currentResponse == null) {
					currentResponse = new ParsedResponse();
					responses.put(documentNumber, currentResponse);
				}
				
				AssessorDocumentI document = (AssessorDocumentI)currentResponse.getDocument();
				
				if(document == null || "".equals(documentNumber) || "0".equals(documentNumber)) {	//first occurrence
					
					String link = CreatePartialLink(TSConnectionURL.idGET) +
						linkTag.extractLink().replaceAll("\\s", "%20").replace(siteLink, "");
					linkTag.setLink(link);
					
					String rowHtml = row.toHtml();
					if(links.size() > 1) {
						rowHtml = rowHtml.replaceAll("(?is)(.*)<a[^>]*>(.*?)</a>", "$1$2");
					}
					
					rowHtml = rowHtml.replaceAll("(?is)(<tr[^>]+)onmouseover=\"[^\"]+\"", "$1");
					rowHtml = rowHtml.replaceAll("(?is)(<td[^>]+)nowrap", "$1");
					
					if ("".equals(documentNumber) || "0".equals(documentNumber)) {
						rowHtml = rowHtml.replaceAll("(?is)<a[^>]+>([^<]*)</a>", "$1");
					}
					
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap m = ro.cst.tsearch.servers.functions.COGunnisonAO.parseIntermediaryRow(row, header, searchId);
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					document = (AssessorDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				} else {
					TableColumn headerColumns[] =  header.getColumns();
					int col = -1;
					String patt = "";
					if (headerColumns.length > 1 && row.getColumnCount() > 1 && "Owner Name".equals(header.getColumns()[1].toPlainTextString().trim())) {
						col = 1;
						patt = "(</a>(?:&nbsp;)?\\s*</td>\\s*<td[^>]*>)([^<]+)<";
					} else if (headerColumns.length > 3 && row.getColumnCount() > 3 && "Owner".equals(header.getColumns()[3].toPlainTextString().trim())) {
						col = 3;
						patt = "(<tr>\\s*<td>.+?</td>\\s*<td>.+?</td>\\s*<td>.+?</td>\\s*<td>)(.+?)<";
					} 
					if(col>-1) {
						ResultMap m = ro.cst.tsearch.servers.functions.COGunnisonAO.parseIntermediaryRow(row, header, searchId);
						Bridge bridge = new Bridge(currentResponse, m, searchId);
						AssessorDocumentI tempDocument = (AssessorDocumentI)bridge.importData();
						PropertyI tempProperty = tempDocument.getProperty();
						PropertyI origProperty = document.getProperty();
						if(tempProperty != null && origProperty !=null) {
							for(NameI nameI : tempProperty.getOwner().getNames()){
			    				if(!origProperty.getOwner().contains(nameI)) {
			    					origProperty.getOwner().add(nameI);
			    				}
			    			}
							String oldRowHtml = currentResponse.getResponse();
							oldRowHtml = oldRowHtml.replaceFirst(patt, "$1$2" + "<br>" + row.getColumns()[col].getChildrenHTML() + "<");
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, oldRowHtml);
							currentResponse.setOnlyResponse(oldRowHtml);
						}
					}
				}
			}
			outputTable.append(table);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		TSServerInfoModule module;
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		
		// search by account number
		if(hasPin()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PROP_NO_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(pin);
			modules.add(module);
		}
		// search by parcel id
		if(hasPin()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(pin.replaceAll("-", ""));
			modules.add(module);
		}
		
		// search by Address
		String strName = getSearchAttribute(SearchAttributes.P_STREETNAME);
		String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		if(!isEmpty(strName) && !isEmpty(strNo)){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(strNo);
			module.getFunction(5).forceValue(strName);
			module.addFilter(addressFilter);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			modules.add(module);			
		}
		
		// search by name - filter by address
		if(hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			module.addFilter(addressFilter);
			
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L F M;;","L F;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
	

}
