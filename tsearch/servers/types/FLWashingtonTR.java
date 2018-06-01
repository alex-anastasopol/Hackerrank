
package ro.cst.tsearch.servers.types;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.HasChildFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
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
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author vladb
 */
public class FLWashingtonTR extends TSServer{


	private static final long serialVersionUID = 1L;
	private Pattern mainTablePattern = Pattern.compile("(?is)(<table.*</table>)\\s*<table\\s+width=\"100%\"");
	private Pattern pinPattern = Pattern.compile("PARCEL#\\s*([\\d-]+)");
	private Pattern yearPattern = Pattern.compile("YEAR\\s*(\\d+)");
	private Pattern baseAmountPattern = Pattern.compile("COMBINED TAXES & ASSESSMENTS:\\s*([\\d.]+)");

	public FLWashingtonTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
		
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
			
				// no result
				if (rsResponse.indexOf("No records found") > -1) {
					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				if(rsResponse.indexOf("REAL ESTATE TAX NOTICE") > -1) {
					ParseResponse(sAction, Response, ID_DETAILS);
					return;
				}
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() == 0) {
					return;
				}
				
				String nextLink = processLink(rsResponse);
				
				parsedResponse.setHeader("<table width=\"100%\" border=\"1\">" + extractHeader(rsResponse));	
				parsedResponse.setFooter("<tr><td colspan=\"3\" align=\"center\">" + (nextLink != null ? nextLink : "") + "</td></tr></table>");
				parsedResponse.setNextLink(nextLink);
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_GET_LINK:
				if (rsResponse.indexOf("REAL ESTATE TAX NOTICE") > -1) {
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
					data.put("type","CNTYTAX");
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
	
	private String getDetails(String page, StringBuilder accountId) {
		
		try {
			StringBuilder details = new StringBuilder();
			
			String text = page.replaceAll("(?is)<[^>]*?>", "");
			Matcher m = pinPattern.matcher(text);
			if(m.find()) {
				accountId.append(m.group(1));
			}
			if(!page.matches("(?is).*<html.*")) { // if from memory
				return page;
			}
			
			details.append("<center>");
			m = mainTablePattern.matcher(page);
			if(m.find()) {
				details.append(m.group(1)
						.replaceAll("(?is)<table", "<table align=\"center\"")
						.replaceFirst("(?is)<img[^>]*>", ""));
			}
			details.append("</center>");
			
			
			return details.toString();
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	private String extractHeader(String page) {
		
		String header = "";

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = (TableTag) nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true)
				.elementAt(0);
			
			TableRow[] rows = interTable.getRows();
			
			for(int i = 0; i < rows.length; i++) {
				
				TableRow row = rows[i];
				
				if((row.toPlainTextString().toUpperCase().indexOf("PROPERTY NUMBER") > -1)
					&& row.getHeaderCount() == 3) {
					
					header = row.toHtml();
					break;
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return header;
	}

	private String processLink(String page) {
		
		try {
			// parse and store parameters on search
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			FormTag formTag = (FormTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
				.elementAt(0);
			NodeList inputs = formTag.getFormInputs();
			Map<String, String> params = new HashMap<String, String>();
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(
					HashCountyToIndex.ANY_COMMUNITY, miServerID);
			String siteLink = dataSite.getLink();
			for(int i = 0; i < inputs.size(); i++) {
				InputTag input = (InputTag) inputs.elementAt(i);
				params.put(input.getAttribute("name"), input.getAttribute("value"));
			}
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:", params);
			
			return "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + formTag.getFormLocation().replace(siteLink, "") + "\">Next 50 Results</a>";
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			
			String page = table.replaceAll("(?is)<th ", "<td ")
				.replaceAll("(?is)</th>", "</td>")
				.replaceAll("(?is)</?b>", "");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag interTable = (TableTag) nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true)
				.elementAt(0);
			
			if(interTable == null) {
				return intermediaryResponse;
			}
			
			TableRow[] rows = interTable.getRows();
			
			DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(
					HashCountyToIndex.ANY_COMMUNITY, miServerID);
			String siteLink = dataSite.getLink();
			
			for(int i = 0; i < rows.length; i++) {
				
				TableRow row = rows[i];
				String rowText = row.toPlainTextString().toUpperCase();
				
				if(row.getColumnCount() != 3  // skip additional rows
					|| rowText.indexOf("PROPERTY NUMBER") > -1
					|| rowText.indexOf("RETURN TO MAIN SEARCH") > -1) {
					
					continue;
				}
				
				LinkTag linkTag = (LinkTag) row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
				String link = CreatePartialLink(TSConnectionURL.idGET) + linkTag.extractLink()
					.replace(siteLink, "").replaceAll("\\s", "%20").trim();
				linkTag.setLink(link);
				
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
				currentResponse.setOnlyResponse(row.toHtml());
				currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
				
				ResultMap m = ro.cst.tsearch.servers.functions.FLWashingtonTR.parseIntermediaryRow(row);
				Bridge bridge = new Bridge(currentResponse, m, searchId);
				
				DocumentI document = (TaxDocumentI)bridge.importData();				
				currentResponse.setDocument(document);
				
				intermediaryResponse.add(currentResponse);
			}
			outputTable.append(page);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		
		try {
			
			String page = Tidy.tidyParse(detailsHtml, null); // crap html
			page = page.replaceAll("(?is)<br>", "\n")
				.replaceAll("(?is)<th", "<td")
				.replaceAll("(?is)</th>", "</td>")
				.replace("&nbsp;", "");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(page, null);
			NodeList nodeList = htmlParser.parse(null);
			
			String owner = "";
			String legal = "";
			String year = "";
			String totalDue = "";
			
			NodeList tables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "700"));
			if(tables.size() == 0) {
				return null;
			}
			
			// extract general info
			TableTag table1 = (TableTag) tables.elementAt(0);
			TableRow[] rows = table1.getRows();
			for(int i = 0; i < table1.getRowCount(); i++) {
				TableRow row = rows[i];
				String rowText = row.toPlainTextString();
				if(rowText.indexOf("PARCEL#") > -1) {
					Matcher m = yearPattern.matcher(rowText);
					if(m.find()) {
						year = m.group(1);
						resultMap.put("TaxHistorySet.Year", year);
					}
					m = pinPattern.matcher(rowText);
					if(m.find()) {
						resultMap.put("PropertyIdentificationSet.ParcelID", m.group(1));
					}
					Node ownerNode = row.getChildren()
						.extractAllNodesThatMatch(new HasAttributeFilter("width", "40%"), true)
						.elementAt(0);
					owner = ownerNode.toPlainTextString().trim();
				} 
			}
			owner = owner.replaceAll("\\s*\n\\s*", "\n").trim();
			
			// extract tax info
			TableRow row1 = (TableRow) nodeList
				.extractAllNodesThatMatch(new HasChildFilter(new HasAttributeFilter("width", "55%")), true)
				.elementAt(0);
			Matcher m = baseAmountPattern.matcher(row1.getColumns()[0].toPlainTextString());
			if(m.find()) {
				resultMap.put("TaxHistorySet.BaseAmount", m.group(1));
			}
			
			NodeList otherInfos = row1.getColumns()[2].getChildren()
				.extractAllNodesThatMatch(new TagNameFilter("tr"), true);
			boolean foundLegal = false;
			List<List> bodyRT = new ArrayList<List>();
			double amountPaid = 0d;
			for(int i = 0; i < otherInfos.size(); i++) {
				TableRow row = (TableRow) otherInfos.elementAt(i);
				String rowText = row.toPlainTextString().replace("&nbsp;", "");
				if(StringUtils.isEmpty(rowText) && !StringUtils.isEmpty(legal)) {
					foundLegal = true;
				}
				if(!foundLegal) {
					legal += rowText;
				}
				m = Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{2})\\s*(\\d+)\\s*\\$(\\d+[.]\\d+)").matcher(rowText);
				if(m.find()) {
					List<String> line = new ArrayList<String>();
					line.add(m.group(2));
					line.add(m.group(3));
					line.add(m.group(1));
					bodyRT.add(line);
					
					amountPaid += Double.valueOf(m.group(3));
				}
			}
			resultMap.put("TaxHistorySet.AmountPaid", String.valueOf(amountPaid));
			
			if(amountPaid == 0) {
				
				Calendar date = Calendar.getInstance();
				SimpleDateFormat dateFormatMonth = new SimpleDateFormat("MMMMM");
				String format = dateFormatMonth.format(date.getTime());
				if (StringUtils.isNotEmpty(format)){
					totalDue = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, format ), "", true, false);
					if (StringUtils.isEmpty(totalDue)){
						Node totalDueNode = nodeList.
						extractAllNodesThatMatch(new HasAttributeFilter("bgcolor", "yellow"), true)
						.elementAt(0).getParent().getParent();
						totalDue = totalDueNode.toPlainTextString().replaceAll("[^.\\d]", "");
					}else{
						totalDue = totalDue.replaceAll("[^.\\d]", "");
						totalDue = StringUtils.cleanAmount(totalDue);
					}
				}
				
			}
			

			if (!bodyRT.isEmpty()) {
				ResultTable newRT = new ResultTable();
				String[] header = { "ReceiptNumber", "ReceiptAmount", "ReceiptDate" };
				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("ReceiptNumber", new String[] {"ReceiptNumber", "" });
				map.put("ReceiptAmount", new String[] {"ReceiptAmount", "" });
				map.put("ReceiptDate", new String[] { "ReceiptDate", "" });
				newRT.setHead(header);
				newRT.setMap(map);
				newRT.setBody(bodyRT);
				newRT.setReadOnly();
				resultMap.put("TaxHistorySet", newRT);
			}
			legal = legal.replaceAll("\\s*\n\\s*", "\n").trim();
			
			ro.cst.tsearch.servers.functions.FLWashingtonTR.parseNames(resultMap, owner, true);
			ro.cst.tsearch.servers.functions.FLWashingtonTR.parseLegalSummary(resultMap, legal);
			
			if(tables.size() < 2) {
				resultMap.put("TaxHistorySet.TotalDue", totalDue);
				return null;
			}
			// extract delinquent info
			TableTag table2 = (TableTag) tables.elementAt(1);
			rows = table2.getRows();
			double priorDel = 0d;
			for(int i = 0; i < table2.getRowCount(); i++) {
				TableRow row = rows[i];
				String rowText = row.toPlainTextString();
				if(row.getColumnCount() == 5 && rowText.indexOf("Tax Year") < 0) {
					if(row.getColumns()[4].toPlainTextString().indexOf("UNPAID") > -1) {
						if(row.getColumns()[0].toPlainTextString().indexOf(year) > -1) {
							totalDue = row.getColumns()[3].toPlainTextString().replace("*", "").trim();
						} else {
							priorDel += Double.valueOf(row.getColumns()[3].toPlainTextString().replace("*", "").trim());
						}
					}
				}
			}
			resultMap.put("TaxHistorySet.TotalDue", totalDue);
			resultMap.put("TaxHistorySet.PriorDelinquent", String.valueOf(priorDel));
			
		} catch (Exception e) {
			logger.error("Error while parsing details", e);
		}
		
		return null;
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		TSServerInfoModule module;
		
		// search by account number
		if(hasPin()){
			modules.add( new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX)));
		}
		
		// search by name
		if(hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L F M;;","L F;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}
