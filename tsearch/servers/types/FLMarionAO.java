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
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

public class FLMarionAO extends TSServer {
		
	private static final long serialVersionUID = 1L;
	
	public FLMarionAO(long searchId) {
		super(searchId);
	}
	
	public FLMarionAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) {
		case ID_SEARCH_BY_PARCEL:						//search by parcel 1
		case ID_SEARCH_BY_INSTRUMENT_NO:				//search by parcel 2
			
			// no result
			/*if (rsResponse.indexOf("0 records found.") > -1) {
				Response.getParsedResponse().setError("0 records found.");
				return;
			}*/
			
			//transform intermediary response into a table format
			int startPosition = rsResponse.indexOf("<pre>") + "<pre>".length();
			int endPosition = rsResponse.indexOf("</pre>");
			String table = rsResponse.substring(startPosition, endPosition);
			String columns[] = table.split("(?i)<br>");
			String newTable = "<table id=\"table1\">";
			for (int i=0; i<columns.length-1; i++) newTable += "<tr>" + columns[i] + "</tr>";
			newTable += "</table>";
			rsResponse = rsResponse.substring(0, startPosition) + newTable + rsResponse.substring(endPosition);
			
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
			
			StringBuilder serialNumber = new StringBuilder();
			String details = getDetails(rsResponse, serialNumber);
			String filename = serialNumber + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","ASSESSOR");
				data.put("dataSource","AO");
				if (isInstrumentSaved(serialNumber.toString(),null,data)){
					details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse( details );
				
			} else {
				smartParseDetails(Response,details);
				
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName( getServerTypeDirectory() + msSaveToTSDFileName);	
				Response.getParsedResponse().setResponse( details );
				
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
		
	protected String getDetails(String rsResponse, StringBuilder parcelNumber) {
		try {
			
			StringBuilder details = new StringBuilder();
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
									
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl03_lblParcel"));
				if (parcelList.size()>0) {
					String parcelID = parcelList.elementAt(0).toPlainTextString().trim();
					parcelNumber.append(parcelID);
				}
								
				return rsResponse;
			}
			
			//if there is a button "More Owners" make a new request
			if (rsResponse.indexOf("More Owners") != -1)
			{
				String[] values  = {"", "", "", "", "", "", "", "", "", ""};
				NodeList keyList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl03_lblPrimeKey"));
				String key = keyList.elementAt(0).toPlainTextString().replaceAll("Prime Key:", "").trim();
				NodeList yearList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl03_lblRollYearHeader"));
				String year = yearList.elementAt(0).toPlainTextString().substring(0, 4);
				NodeList inputList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("type", "hidden"));
				if (inputList.size()>=1)
				{
					String string = inputList.elementAt(0).toHtml();
					Matcher matcher = Pattern.compile(".*value=\"(.*?)\"").matcher(string);
					if (matcher.find()) values[0] = matcher.group(1);
				}
				if (inputList.size()>=2)
				{
					String string = inputList.elementAt(1).toHtml();
					Matcher matcher = Pattern.compile(".*value=\"(.*?)\"").matcher(string);
					if (matcher.find()) values[1] = matcher.group(1);
				}
				if (inputList.size()>=3)
				{
					String string = inputList.elementAt(2).toHtml();
					Matcher matcher = Pattern.compile(".*value=\"(.*?)\"").matcher(string);
					if (matcher.find()) values[2] = matcher.group(1);
				}
				if (inputList.size()>=4)
				{
					String string = inputList.elementAt(3).toHtml();
					Matcher matcher = Pattern.compile(".*value=\"(.*?)\"").matcher(string);
					if (matcher.find()) values[9] = matcher.group(1);
				}
				values[3] = "More Owners";
				NodeList nameList = nodeList.extractAllNodesThatMatch(new TagNameFilter("textarea"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl03_txtName"));
				if (nameList.size()>=1)
				{
					String name = nameList.elementAt(0).toPlainTextString();
					values[4] = name;
				}
				NodeList situsList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl03_txtSitus"));
				if (situsList.size()>=1)
				{
					String situs = situsList.elementAt(0).toHtml();
					Matcher matcher = Pattern.compile(".*value=\"(.*?)\"").matcher(situs);
					if (matcher.find()) values[5] = matcher.group(1);
				}
				NodeList descList = nodeList.extractAllNodesThatMatch(new TagNameFilter("textarea"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl07_txtDesc"));
				if (descList.size()>=1)
				{
					String desc = descList.elementAt(0).toPlainTextString();
					values[6] = desc;
				}
				NodeList traverseList = nodeList.extractAllNodesThatMatch(new TagNameFilter("textarea"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl09_ctl00_txtTraverse"));
				if (traverseList.size()>=1)
				{
					String traverse = traverseList.elementAt(0).toPlainTextString();
					values[7] = traverse;
				}
				NodeList notesList = nodeList.extractAllNodesThatMatch(new TagNameFilter("textarea"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl11_txtNotes"));
				if (notesList.size()>=1)
				{
					String notes = notesList.elementAt(0).toPlainTextString();
					values[8] = notes;
				}
				
				String link = "http://216.255.243.135" + "/DEFAULT.aspx?Key=" + key + "&YR=" + year;
				HttpSite detailsPage = HttpManager.getSite(getCurrentServerName(), searchId);	
				try {
					
					String newDetails = ((ro.cst.tsearch.connection.http2.FLMarionAO)detailsPage).getPage(link, values);
					rsResponse = newDetails;
					rsResponse = rsResponse.replaceAll("(?is)<input[^>]*value=\"More Owners\"[^>]*>", "");
					rsResponse = rsResponse.replaceAll("(?is)<input[^>]*value=\"Less Owners\"[^>]*>", "");
					htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
					nodeList = htmlParser.parse(null);
					
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					HttpManager.releaseSite(detailsPage);
				}
			}
			
			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl03_lblParcel"));
			if (parcelList.size()>0) {
				String parcelID = parcelList.elementAt(0).toPlainTextString().replaceAll("\\s-\\sTangible", "").trim();
				parcelNumber.append(parcelID);
			}
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"));
			
			NodeList traverseLabels = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl09_lblBldgCnt"));
			
			NodeList traverseTexts = nodeList.extractAllNodesThatMatch(new TagNameFilter("textarea"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl09_ctl00_txtTraverse"));
	
			Node mstuNode = HtmlParser3.getNodeByID("ctl03_hypeMSTU", nodeList, true);
			String link = "";
			if (mstuNode != null)					//some parcels don't have M.S.T.U. details
			{
				link = mstuNode.toHtml().replaceAll("&amp;", "&");
				Matcher ma = Pattern.compile("(?is)href=\"(http.*?)\"").matcher(link);
				if (ma.find()) link = ma.group(1);
			}
			
			//"Previous Parcel" and "Next Parcel" links
			/*details.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>");
			details.append("<td>" + tables.elementAt(3).toHtml().replaceAll("http://216.255.243.135", CreatePartialLink(TSConnectionURL.idGET)) + "</td>");
			details.append("<td style=\"width:30px;\"></td>");
			details.append("<td>" + tables.elementAt(4).toHtml().replaceAll("http://216.255.243.135", CreatePartialLink(TSConnectionURL.idGET)) + "</td>");
			details.append("</tr></table>");*/
			
			NodeList firstTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("style", "width:100%;"));
			
			if(firstTable.size() == 0) { 
				return null;
			}
						
			details.append(firstTable.elementAt(0).toHtml().replaceAll("(?is)<a[^>]*(style=\"[^\"]+\")[^>]*>([^<]+)</a>", "<span $1>$2</span>")
					.replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1")
					.replaceAll("(?is)MAP\\s+IT", "").replaceAll("(?is)M\\.S\\.T\\.U\\.", "")
					.replaceAll("(?is)<tr>\\s*<td\\s*colspan=\"5\">\\s*<hr\\s*/>\\s*</td>\\s*</tr>", ""));
			
			int limit = 15;
			if (tables.size()<15) limit = tables.size(); 
			for (int i=9; i<limit; i++)	{
				details.append("<hr>" + tables.elementAt(i).toHtml().replaceAll("(?is)<a[^>]*(style=\"[^\"]+\")[^>]*>([^<]+)</a>", "<span $1>$2</span>")
						.replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1"));
			}
			
			if (traverseLabels.size() >0 && traverseTexts.size() > 0)
				details.append("<hr><span style=\"display:inline-block;width:100%;text-align: center\">Traverse</span>")
					.append(traverseLabels.elementAt(0).toHtml())
					.append(traverseTexts.elementAt(0).toHtml());
			
			for (int i=15; i<tables.size(); i++)	
				if (tables.elementAt(i).toHtml().contains("Structure Type"))		//for Building Characteristics
					details.append("<hr><span style=\"display:inline-block;width:100%;text-align: center\">Building Characteristics</span>"
						+ tables.elementAt(i).toHtml().replaceAll("(?is)<a[^>]*(style=\"[^\"]+\")[^>]*>([^<]+)</a>", "<span $1>$2</span>")
						.replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1"));
				else 
					details.append("<hr>" + tables.elementAt(i).toHtml().replaceAll("(?is)<a[^>]*(style=\"[^\"]+\")[^>]*>([^<]+)</a>", "<span $1>$2</span>")
							.replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1"));
			
			if (link.length() != 0)
			{
				HttpSite mstuPage = HttpManager.getSite(getCurrentServerName(), searchId);		//get M.S.T.U. details
				try {
					
					String mstuDetails = ((ro.cst.tsearch.connection.http2.FLMarionAO)mstuPage).getPage(link, new String[1]);
					mstuDetails = Tidy.tidyParse(mstuDetails, null);
					org.htmlparser.Parser htmlParserTaxDtails = org.htmlparser.Parser.createParser(mstuDetails, null);
					NodeList nodeMSTUDetails = htmlParserTaxDtails.parse(null);
					NodeList divs = nodeMSTUDetails.extractAllNodesThatMatch(new TagNameFilter("div"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "pnlParcelAssessmentInfo"));
					if (divs.size()>0) {
						NodeList innerDivs = divs.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("div"), false);
						if (innerDivs.size()>0) {
							for (int i=0;i<innerDivs.size()-1;i++) {
								details.append(innerDivs.elementAt(i).toHtml());
							}
						}
					}
					
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					HttpManager.releaseSite(mstuPage);
				}
			}
			
			return details.toString();
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
		
	protected String createPartialLink(int iActionType, int dispatcher) {
		StringBuilder build = new StringBuilder(msRequestSolverName);
		build.append("?");
		build.append(msServerID);
		build.append("&");
		build.append(RequestParams.SEARCH_ID);
		build.append("=");
		build.append(mSearch.getSearchID());
		build.append("&");
		build.append("dispatcher=");
		build.append(dispatcher);
		return build.toString();
	}
		
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		TableTag resultsTable = null;
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id","table1"), true);
			
			resultsTable = (TableTag)mainTable.elementAt(0); 
			
			//if there are results
			if (resultsTable != null && resultsTable.getRowCount() != 2) 	//there are always 2 rows,		
			{																//even if there are no results
				TableRow[] rows  = resultsTable.getRows();
				String link = "";
				
				for (int i = 0; i < rows.length-2 ; i++)					//last two rows are "Neighborhood" and "Millage"
				{
					TableRow row = rows[i];
										
					String htmlRow = row.toHtml();
					ParsedResponse currentResponse = new ParsedResponse();
					
					htmlRow += "<br>";
					
					//remove "Map it" link
					htmlRow = htmlRow.replaceAll("(?is)<a href=http[^>]+/Freeance/Client/PublicAccess1/index.html(.*)</a>", "");
					
					Matcher ma = Pattern.compile("(?is)href=\"(.*)/default.aspx\\?key=(\\d+)&yr=(\\d+)").matcher(htmlRow);
					if (ma.find()) link = CreatePartialLink(TSConnectionURL.idGET) + 
										"/default.aspx?key=" + ma.group(2) + "&yr=" + ma.group(3);
					//replace the links
					htmlRow = htmlRow.replaceAll("(?is)href=\".*\"", "href=" + link);
					
					htmlRow = htmlRow.replaceAll("(?is)\\btarget\\s*=\\s*[\\s\\\"']?window[\\s\\\"']?", "");
										
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
									
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,htmlRow);
					currentResponse.setOnlyResponse(htmlRow);
					
					ResultMap m = ro.cst.tsearch.servers.functions.FLMarionAO.parseIntermediaryRow( row, searchId ); 
					Bridge bridge = new Bridge(currentResponse, m, searchId);
					
					DocumentI document = (AssessorDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
					 
				}
				
				String header = "<table><tr>" +
					"<td><strong><font face=\"arial, helvecta\" size=2>Assessed Name</font></strong></td>" +
					"<td align=\"center\"><strong><font face=\"arial, helvecta\" size=2>#</font></strong></td>" +
					"<td><strong><font face=\"arial, helvecta\" size=2>Street</font></strong></td>" +
					"<td align=\"right\"><strong><font face=\"arial, helvecta\" size=2>Parcel Number</font></strong></td>" +
					"</tr>";
				header = "<pre><table>";
				response.getParsedResponse().setHeader(header);
				
				response.getParsedResponse().setFooter("<tr>&nbsp;                                                    "
						+ "Neighborhood____________|    |</tr><br><tr>&nbsp;                                                    "
						+ "Millage______________________|</tr>"
						+ "</table></pre>");
												
				outputTable.append(table);
			}
			
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
				
		try {
					
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
						
			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl03_lblParcel"));
			String parcelID = parcelList.elementAt(0).toPlainTextString().replaceAll("\\s*-\\s*Tangible", "").trim();
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
			
			NodeList ownerList = nodeList.extractAllNodesThatMatch(new TagNameFilter("textarea"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "ctl03$txtName"));
			String owner = ownerList.elementAt(0).toPlainTextString().replaceAll("&amp;", "&").trim();
			String [] tmpOwners = owner.split("\n");
			if (tmpOwners.length >= 2) 
			{
				owner = "";
				for (int i=0; i<tmpOwners.length - 2 ; i++)		//on last two lines is the address of the owner 
					owner += tmpOwners[i];
			}
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
			
			NodeList addressList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "ctl03$txtSitus"));
			if (addressList.size() != 0)
			{
				String address = ((InputTag)addressList.elementAt(0)).getAttribute("value").trim();
				if (address.contains("ET AL"))					//e.g. "1  ET AL-MARTY SMITH & ANN CRAGGS"
					address = "";
				else if (address.contains("&"))					//e.g. "1  T-TONY & S-SHARON"
					address = "";
				else {
					if(address.contains(":")) {
						address = address.replaceFirst(".*?:\\s*", "");
					}
					if(address.contains(",")) {
						String zip = address.replaceFirst(".*?,\\s*", "");
						if(zip.matches("[\\d-]+")) {
							resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
							address = address.replaceFirst("(.*),\\s*" + zip, "$1");
						}
						
					}
					resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
				}
			}
			
			String legal = "";
			NodeList legalList = nodeList.extractAllNodesThatMatch(new TagNameFilter("textarea"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("name", "ctl06$txtDesc"));
			if (legalList.size() != 0)
			{
				legal = legalList.elementAt(0).toPlainTextString().trim();
			}
			else
			{
				legalList = nodeList.extractAllNodesThatMatch(new TagNameFilter("textarea"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("name", "ctl07$txtDesc"));
				if (legalList.size() != 0)
				{
					legal = legalList.elementAt(0).toPlainTextString().trim();
				}
			}
			if (legal.length() != 0)
				resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
			
			NodeList assessmentList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl03_lblTaxes"));
			String assesment = assessmentList.elementAt(0).toPlainTextString().trim().replaceAll("[,\\$]", "");
			resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assesment);
			
			ResultTable transactionHistory = new ResultTable();			//transaction history table
			@SuppressWarnings("rawtypes")
			List<List> tablebody = new ArrayList<List>();
			List<String> list;
			
			TableTag transactionHistoryTable = (TableTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl06_gvSales")).elementAt(0);
			if (transactionHistoryTable != null)
			{
				int rowsNumber = transactionHistoryTable.getRowCount();
				String bookPage = "";
				String date = "";
				String instrument = "";
				String price = "";
				String[] bookAndPage;
				
				for (int i=1; i<rowsNumber; i++)			//row 0 is the header
				{
					TableRow row = transactionHistoryTable.getRow(i);
					bookPage = row.getColumns()[0].toPlainTextString().trim();
					bookAndPage = bookPage.split("/");
					date = row.getColumns()[2].toPlainTextString().trim();
					instrument = row.getColumns()[3].toPlainTextString().trim();
					price = row.getColumns()[7].toPlainTextString().trim().replaceAll("[,\\$]", "");
					
					list = new ArrayList<String>();
					list.add(StringUtils.removeLeadingZeroes(bookAndPage[0].replaceFirst("(?is)UNREC", "")));
					list.add(StringUtils.removeLeadingZeroes(bookAndPage[1].replaceFirst("(?is)INST", "")));
					list.add(date);
					list.add(instrument);
					list.add(price);
					tablebody.add(list);
				}
				
				String[] header = {SaleDataSetKey.BOOK.getShortKeyName(), SaleDataSetKey.PAGE.getShortKeyName(), 
						SaleDataSetKey.RECORDED_DATE.getShortKeyName(), SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(),
						SaleDataSetKey.SALES_PRICE.getShortKeyName()};
				transactionHistory = GenericFunctions2.createResultTable(tablebody, header);
				if (transactionHistory != null){
					resultMap.put("SaleDataSet", transactionHistory);
				}
			}
						
			ro.cst.tsearch.servers.functions.FLMarionAO.parseNames(resultMap, searchId);
			ro.cst.tsearch.servers.functions.FLMarionAO.parseAddress(resultMap);
			ro.cst.tsearch.servers.functions.FLMarionAO.parseLegalSummary(resultMap);							
		
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
		
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		
		if(hasPin()) {			//search by Parcel_ID_Roll_1 and split PIN in 5-3-2 format
			String parcelID = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			if (parcelID.length() == 10)
			{
				parcelID = parcelID.substring(0, 5) + "-" + parcelID.substring(5, 8) 
					+ "-" + parcelID.substring(8, 10);
			}	
			FilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKey(0);
			module.forceValue(0, parcelID);
			module.addFilter(pinFilter);
			moduleList.add(module);
		}
		
		if(hasPin()) {			//search by Parcel_ID_Roll_1 and split PIN in 4-4-2 format
			String parcelID = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			if (parcelID.length() == 10)
			{
				parcelID = parcelID.substring(0, 4) + "-" + parcelID.substring(4, 8) 
					+ "-" + parcelID.substring(8, 10);
			}	
			FilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKey(0);
			module.forceValue(0, parcelID);
			module.addFilter(pinFilter);
			moduleList.add(module);
		}
		
		if(hasPin()) {			//search by Parcel_ID_Roll_1 and split PIN in 4-3-3 format
			String parcelID = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			if (parcelID.length() == 10)
			{
				parcelID = parcelID.substring(0, 4) + "-" + parcelID.substring(4, 7) 
					+ "-" + parcelID.substring(7, 10);
			}	
			FilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKey(0);
			module.forceValue(0, parcelID);
			module.addFilter(pinFilter);
			moduleList.add(module);
		}
		
		if(hasPin()) {			//search by Parcel_ID_Roll_2
			FilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.clearSaKey(0);
			module.setSaKey(0, SearchAttributes.LD_PARCELNO);
			module.addFilter(pinFilter);
			moduleList.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(moduleList);
	}
}
