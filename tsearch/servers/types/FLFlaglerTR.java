/**
 * 
 */

package ro.cst.tsearch.servers.types;

//import ro.cst.tsearch.connection.http2.FLGenericGovernmaxTR;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.HasChildFilter;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
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
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterResponse2;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author ats007
 * 
 */
public class FLFlaglerTR extends FLGenericGovernmaxTR {

	private static final long serialVersionUID = 1L;
	
	private static final Pattern prevPattern = Pattern.compile("(?i)<A HREF=\"(?:\\.\\./\\.\\./)?((?:agency/[^/]+/)?list_collect[a-z0-9._-]+asp)\\?(r=[^&]*&l_mv=previous[^\"]+)\"");
	private static final Pattern nextPattern = Pattern.compile("(?i)<A HREF=\"(?:\\.\\./\\.\\./)?((?:agency/[^/]+/)?list_collect[a-z0-9._-]+asp)\\?(r=[^&]*&l_mv=next[^\"]+)\"");
	private static String propertyInfoHtml = "";

	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 * @param miServerID
	 */
	
	private static final CheckTangible CHECK_TANGIBLE = new CheckTangible() {
		public boolean isTangible(String row){
			String searchdat5 = getSearchdat5(row);
			String linkText = getLinkText(row);
			return searchdat5.matches("0+") || linkText.matches("[6-9]+.*");
		}
	};

	public FLFlaglerTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		checkTangible = CHECK_TANGIBLE;
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		String details = "";
		String accountId = "";
		
		// no result
		if (rsResponse.indexOf("No records found") > -1) {
			Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:

			if(rsResponse.indexOf("Ad Valorem Taxes and Non-Ad Valorem Assessments") > -1) {
				ParseResponse(sAction, Response, ID_DETAILS);
				return;
			}

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if(smartParsedResponses.size() == 0) {
				return;
			}
			
			// treat navigation
			String prevLink = extractLink(rsResponse, prevPattern, CreatePartialLink(TSConnectionURL.idGET), "&lt;Prev");
			String nextLink = extractLink(rsResponse, nextPattern, CreatePartialLink(TSConnectionURL.idGET), "Next&gt;");
			if(prevLink == null) {
				prevLink = "&lt;Prev";
			}
			if(nextLink == null) {
				nextLink = "Next&gt;";
			}
			
			Response.getParsedResponse().setHeader("<table width=\"100%\">" 
					+ "<tr><th colspan=\"5\" bgcolor=\"#000000\">"
					+ "<font color=\"white\">Search Results</font></th></tr>");
			Response.getParsedResponse().setFooter("<tr><td colspan=\"5\">" 
					+ prevLink + "&nbsp;&nbsp;&nbsp;" + nextLink + "</td></tr></table>");
			Response.getParsedResponse().setNextLink(nextLink);
			
			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());
			parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			
			break;
		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
			
			propertyInfoHtml = extractPropInfoHtml(rsResponse);
			details = cleanDetails(getDetails(rsResponse));
			accountId = extractAccount(details).trim();
			
			String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
			String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

			HashMap<String, String> data = new HashMap<String, String>();
			data.put("type","CNTYTAX");
			if (isInstrumentSaved(accountId, null, data)){
				details += CreateFileAlreadyInTSD();
			}
			else {
				mSearch.addInMemoryDoc(sSave2TSDLink, details);
				details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
			}

			Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
			Response.getParsedResponse().setResponse(details);
			
			break;
		case ID_SAVE_TO_TSD:
			
			if(rsResponse.matches("(?is).*<\\s*html\\s*>.*")) {
				propertyInfoHtml = extractPropInfoHtml(rsResponse);
				details = cleanDetails(getDetails(rsResponse));
			} else {
				details = rsResponse;
			}
			accountId = extractAccount(details).trim();
			String filename = accountId + ".html";
			
			smartParseDetails(Response,details);
			
			msSaveToTSDFileName = filename;
			Response.getParsedResponse().setFileName( getServerTypeDirectory() + msSaveToTSDFileName);	
			Response.getParsedResponse().setResponse( details );
			
			msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			
			break;
		case ID_GET_LINK:
			if (rsResponse.indexOf("Ad Valorem Taxes and Non-Ad Valorem Assessments") > -1) {
				ParseResponse(sAction, Response, ID_DETAILS);
			} else {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			}
			break;
		}

		return;
	}
	
	private String extractPropInfoHtml(String detailsHtml) {
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			LinkTag link = (LinkTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("a"), true)
				.extractAllNodesThatMatch(new HasChildFilter(new HasAttributeFilter("src", "images/leavex.gif")))
				.elementAt(0);
			
			return getLinkContents(link.extractLink());
			
		} catch(Exception e) {
			
		}
		
		return null;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			
			resultMap.put("OtherInformationSet.SrcType","TR");
			
			detailsHtml = "<table>" + detailsHtml; //broken HTML
 			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList tables = nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), false);
			String currentYear = "";
			String ownerInfo = "";
			String legal = "";
			String address = "";
			String amountDue = "";
			
			for(int i = 0; i < tables.size(); i++) {
				TableTag table = (TableTag) tables.elementAt(i);
				
				if(table.toPlainTextString().indexOf("Mailing Address") > -1){
					TableTag table1 = (TableTag) table.getChildren()
						.extractAllNodesThatMatch(new HasAttributeFilter("width", "600"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("bordercolor", "#C0C0C0"), true)
						.elementAt(0);
					resultMap.put("PropertyIdentificationSet.ParcelID", 
							table1.getRows()[1].getColumns()[0].toPlainTextString().trim());
					if(table1.getRows()[1].getColumns()[1].toPlainTextString().trim().indexOf("REAL ESTATE") > -1) {
						resultMap.put("PropertyIdentificationSet.PropertyType", "Real Estate");
					} else {
						resultMap.put("PropertyIdentificationSet.PropertyType", "Tangible");
					}
					currentYear = table1.getRows()[1].getColumns()[2].toPlainTextString().trim();
					resultMap.put("TaxHistorySet.Year", currentYear);
					ownerInfo = table1.getRows()[2].toPlainTextString().replaceAll("&nbsp;", "");
					Pattern namePattern = Pattern.compile("(?is)\\s*Mailing\\s+Address(.*)Physical Address");
					Matcher nameMatcher = namePattern.matcher(ownerInfo);
					if(nameMatcher.find()) {
						ownerInfo = nameMatcher.group(1).trim().replaceAll("\\s*\n\\s*", "\n");
					}
					try { // 8212985 - No Legal
						TableTag table2 = (TableTag) table.getChildren()
							.extractAllNodesThatMatch(new HasAttributeFilter("width", "99%"), true)
							.elementAt(0);
						String legalDesc = table2.toPlainTextString().replaceAll("&nbsp;", "");
						Pattern legalPattern = Pattern.compile("(?i)\\s*Legal\\s+Description\\s+(.*)");
						Matcher legalMatcher = legalPattern.matcher(legalDesc);
						if(legalMatcher.find()) {
							legal = legalMatcher.group(1).trim();
							resultMap.put("PropertyIdentificationSet.PropertyDescription", legal);
						}
					} catch(Exception e) {
						// e.printStackTrace();
					}
					try {
						Node baseAmountNode = table.getChildren()
							.extractAllNodesThatMatch(new HasAttributeFilter("width", "21%"), true)
							.extractAllNodesThatMatch(new RegexFilter("(?is)\\s*[$][\\d,]+[.]\\d+(&nbsp;)?\\s*"), true)
							.elementAt(0);
						resultMap.put("TaxHistorySet.BaseAmount", baseAmountNode.toPlainTextString()
							.replaceAll("[,$]|(?:&nbsp;)", "").trim());
					} catch (Exception e) {
						// e.printStackTrace();
					}
				} else if(table.toPlainTextString().indexOf("Amount Due") > -1) {
					TableTag table1 = (TableTag) table.getChildren()
						.extractAllNodesThatMatch(new HasAttributeFilter("width", "53%"), true)
						.elementAt(0);
					TableRow[] rows = table1.getRows();
					for(TableRow row : rows) {
						if(row.toPlainTextString().indexOf("Amount Due") < 0) {
							if(StringUtils.isEmpty(amountDue) || row.toHtml().matches("(?is).*<b>.*</b>.*")) {
								amountDue = row.getColumns()[1].toPlainTextString().replaceAll("[$,]|&nbsp;", "").trim();
							}
						}
					}
				} else if(table.toPlainTextString().indexOf("Transaction") > -1) {
					try {  //table could be empty
						resultMap.put("TaxHistorySet.DatePaid", table.getRows()[1].getColumns()[0].toPlainTextString()
							.replaceAll("(?:&nbsp;)", "").trim());
						resultMap.put("TaxHistorySet.ReceiptDate", table.getRows()[1].getColumns()[0].toPlainTextString()
							.replaceAll("(?:&nbsp;)", "").trim());
						resultMap.put("TaxHistorySet.AmountPaid", table.getRows()[1].getColumns()[4].toPlainTextString()
							.replaceAll("[,$]|(?:&nbsp;)", "").trim());
					} catch (Exception e) {
//						e.printStackTrace();
					}
				} else if (table.toPlainTextString().indexOf("Prior Year Taxes Due") > -1 &&
							table.toPlainTextString().indexOf("NO DELINQUENT TAXES") < 0){
					TableTag table1 = (TableTag) table.getChildren()
						.extractAllNodesThatMatch(new HasAttributeFilter("width", "53%"), true)
						.elementAt(0);
					TableTag table2 = (TableTag) table.getChildren()
						.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true)
						.elementAt(0);
					double currentYearUnpaid = 0d;
					double totalUnpaid = Double.valueOf(table1.getRows()[0].getColumns()[1].toPlainTextString()
							.replaceAll("[,$]|(?:&nbsp;)", "").trim());
					for(int j = 0; j < table2.getRowCount(); j++) {
						if(table2.getRows()[j].getColumns()[0].toPlainTextString().trim().equals(currentYear)) {
							currentYearUnpaid = Double.valueOf(table2.getRows()[j].getColumns()[5].toPlainTextString().replaceAll("[,$]|(?:&nbsp;)", "").trim());
							break;
						}
					}
					resultMap.put("TaxHistorySet.PriorDelinquent", String.valueOf(totalUnpaid - currentYearUnpaid));
					if (StringUtils.isEmpty(amountDue))
						amountDue = String.valueOf(currentYearUnpaid);
				}
			}
			
			if (StringUtils.isEmpty(amountDue))
				amountDue = "0.0";
			resultMap.put("TaxHistorySet.TotalDue", amountDue); 
			
			NodeList addressTables = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "98%"), true);
			if (addressTables.size()>0) {
				TableTag addressTable = (TableTag)addressTables.elementAt(0);
				if (addressTable.getRowCount()>0) {
					TableRow row = addressTable.getRow(0);
					if (row.getColumnCount()>1) {
						address = row.getColumns()[1].toPlainTextString().replaceAll("(?is)Physical\\s+Address", "").replaceAll("&nbsp;", "").trim();
						resultMap.put("PropertyIdentificationSet.AddressOnServer", address);
					}
				}
			}
			
			ro.cst.tsearch.servers.functions.FLFlaglerTR.parseNames(resultMap, ownerInfo, searchId);
			ro.cst.tsearch.servers.functions.FLFlaglerTR.parseAddress(resultMap, address, searchId);
			ro.cst.tsearch.servers.functions.FLFlaglerTR.parseLegal(resultMap, legal);
			
			return null;
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private String extractAccount(String detailsHtml) {
		
		HtmlParser3 parser = new HtmlParser3(detailsHtml);
		return HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(parser.getNodeList(), "Account Number"), "", false);
	}
	
	private String cleanDetails(String detailsHtml) {
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			Node table = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "99%"), true)
				.elementAt(0);
			
			String duplicateBeg = table.toPlainTextString().replace("Legal Description&nbsp;", "").trim();
			if (duplicateBeg.length()>30) {
				duplicateBeg = duplicateBeg.substring(0, 30);
			}
			Matcher m = Pattern.compile("(?is)" + duplicateBeg + ".*?(?=</font>)").matcher(detailsHtml);
			m.find();
			String duplicate = "";
 			if(m.find()) {
				duplicate = m.group(0);
			}
			
 			if(duplicate.length() > 0) {
				StringBuilder sb = new StringBuilder(detailsHtml);
				sb.replace(detailsHtml.lastIndexOf(duplicate), 
						detailsHtml.lastIndexOf(duplicate) + duplicate.length(), "");
				detailsHtml = sb.toString();
 			}
			
			return detailsHtml;
		} catch(Exception e) {
			
		}
		return null;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {

		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("bordercolor", "Black"));

			if (mainTableList.size() != 1) {
				return intermediaryResponse;
			}

			TableTag tableTag = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows = tableTag.getRows();
			String recordHtml = "";
			String link = "";

			for (int i = 0; i < rows.length; i++) {
				TableRow row = rows[i];

				if (row.toPlainTextString().indexOf("Search Results") > -1 || i % 5 == 4) {
					continue;
				}
				
				if(i % 5 == 1) {
					LinkTag linkTag = (LinkTag)row.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0);
					link = CreatePartialLink(TSConnectionURL.idGET) + "/collectmax/" 
						+ linkTag.extractLink().trim().replaceAll("\\s", "%20");
					linkTag.setLink(link);
				}
				
				recordHtml += row.toHtml();

				if (i % 5 == 0) {

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, recordHtml);
					currentResponse.setOnlyResponse(recordHtml);
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

					ResultMap m = ro.cst.tsearch.servers.functions.FLFlaglerTR.parseIntermediaryRow(recordHtml, searchId);
					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);
					recordHtml = "";
				}
			}

			outputTable.append(table);
		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}

		return intermediaryResponse;
	} 
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		TSServerInfoModule module;
		
		FilterResponse rejectNonRealEstateFilter = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		rejectNonRealEstateFilter.setThreshold(new BigDecimal("0.65"));
		
		// search by PIN	
		if(hasPin()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);  
			module.addFilter(rejectNonRealEstateFilter);
			modules.add(module);		
		}
		
		// search by Address	
		if(hasAddress()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();	
			module.getFunction(0).setSaKey(SearchAttributes.P_STREETNO);
			module.getFunction(2).setSaKey(SearchAttributes.P_STREETNAME);
			module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_ST_N0_FAKE);
			module.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_ST_NAME_FAKE);
			module.addFilter(rejectNonRealEstateFilter);
			module.addFilter(new AddressFilterResponse2("",searchId));
			if(hasLegal()){
				module.addFilter(LegalFilterFactory.getDefaultLegalFilter(searchId));
			}			
			modules.add(module);		
		}
		
		// search by Owner Name	
		if(hasName()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			module.addFilter(rejectNonRealEstateFilter);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(
					SearchAttributes.OWNER_OBJECT, searchId, module));
			
			if(hasLegal()){
				module.addFilter(LegalFilterFactory.getDefaultLegalFilter(searchId));
			}
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L;F;M","L;F;"});
			
			module.addIterator(nameIterator);
			
			modules.add(module);		
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}
}
