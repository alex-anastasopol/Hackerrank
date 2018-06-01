/**
 * 
 */
package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.InstanceManager;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author vladb
 *
 */
public class COGenericVisualGovTR extends TSServer {

	
	private static final long serialVersionUID = 1L;
	private static final String FORM_NAME = "aspnetForm";
	private static int seq = 0;

	public COGenericVisualGovTR(long searchId) 
	{
		super(searchId);
	}
	
	public COGenericVisualGovTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) 
	{
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
				if (rsResponse.indexOf("No Property Tax records found") > -1) {
					Response.getParsedResponse().setError("No results found for your query! Please change your search criteria and try again.");
					return;
				}
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() == 0) {
					return;
				}
				
				// parse and store parameters on search
				Form form = new SimpleHtmlParser(rsResponse).getForm(FORM_NAME);
				Map<String, String> params = form.getParams();
				params.remove("__EVENTTARGET");
				params.remove("__EVENTARGUMENT");
				int seq = getSeq();
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
				
				// get navigation links
				String nextLink = getNavLink(Response.getResult(), form.action, seq, "Next >>", "Next");
				String prevLink = getNavLink(Response.getResult(), form.action, seq, "<< Prev", "Previous");
				
				StringBuilder footer = new StringBuilder("<tr><td colspan=\"3\" align=\"center\">");
				if(!prevLink.equals("")) {
					footer.append(prevLink);
					footer.append("&nbsp;&nbsp;&nbsp;");
				}
				footer.append(nextLink);
				
				Response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\">\n" +
						"<tr><th>Schedule</th><th>Name</th><th>Site Address</th></tr>");
				Response.getParsedResponse().setFooter(footer.toString() + "</table>");
				Response.getParsedResponse().setNextLink(nextLink);
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_GET_LINK:
				if (rsResponse.indexOf("TAX/NOTICE RECEIPT") > -1) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				break;
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				
				StringBuilder accountId = new StringBuilder();
				DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(
						HashCountyToIndex.ANY_COMMUNITY,miServerID);
				String siteLink = dataSite.getLink().replace("/SearchSelect.aspx", "");
				String details = getDetails(rsResponse, accountId, siteLink + (sAction + "&" + Response.getRawQuerry()));
				String filename = accountId + ".html";
				
				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					data.put("year", accountId.toString().split("-")[1]);
					if (isInstrumentSaved(accountId.toString(),null,data)){
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
					Response.getParsedResponse().setFileName( getServerTypeDirectory() + msSaveToTSDFileName);	
					Response.getParsedResponse().setResponse( details );
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				}
				break;
		}
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_ContentPlaceHolder1_grdResults"), true);
			
			if(mainTableList.size() != 1) {
				return intermediaryResponse;
			}
			
			String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			TableTag tableTag = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows  = tableTag.getRows();
		
			for (int i = 0; i < rows.length; i++) {
				TableRow row = rows[i];
				if(row.getColumnCount() == 3) {
					if(row.toPlainTextString().toLowerCase().indexOf("schedule") > -1) {
						continue; //skip the headers row
					}
				
					LinkTag linkTag = ((LinkTag)row.getColumns()[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).elementAt(0));
					String link = CreatePartialLink(TSConnectionURL.idGET) + "/" + linkTag.extractLink().trim().replaceAll("\\s", "%20");
					linkTag.setLink(link);
					
					String rowHtml = row.toHtml().replaceAll("(?is)<tr[^>]*>", "<tr>");
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap resultMap = null;
					if("clear creek".equals(crtCounty.toLowerCase())) {
						resultMap = ro.cst.tsearch.servers.functions.COClearCreekTR.parseIntermediaryRow(row, searchId);
					} else if("summit".equals(crtCounty.toLowerCase())) {
						resultMap = ro.cst.tsearch.servers.functions.COSummitTR.parseIntermediaryRow(row, searchId);
					} else if("san miguel".equals(crtCounty.toLowerCase())) {
						resultMap = ro.cst.tsearch.servers.functions.COSanMiguelTR.parseIntermediaryRow(row, searchId);
					}
					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
			}
			
			outputTable.append(table);
		}
		catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	protected String getNavLink(String response, String action, int seq, String key, String name) {
		
		String link="";
		try {
			Pattern pattern = Pattern.compile("__doPostBack\\('([^']*)','([^']*)'\\)\">" + key);
			Matcher matcher = pattern.matcher(response);
			if (!matcher.find()) {
				return "";
			}
			String target = matcher.group(1);
			String argument = matcher.group(2);
			String linkStart = CreatePartialLink(TSConnectionURL.idPOST);
			link = "<a href=\"" + linkStart + "/" + URLEncoder.encode(action,"UTF-8") + "&__EVENTTARGET=" + target + "&__EVENTARGUMENT=" + argument + "&seq=" + seq +"\">" + name + "</a>";
		} 
		catch (Throwable t){
			logger.error("Error while getting navigation link", t);
		}
		return link;
	}
	
	protected synchronized static int getSeq(){
		return seq++;
	}
	
	private String getDetails(String rsResponse, StringBuilder accountId, String currentYearDocLink) {
	
		try {
			StringBuilder details = new StringBuilder();
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList taxTables = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "Table3"),true);
			
			TableTag table1 = (TableTag)nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "AutoNumber1"),true)
				.elementAt(0);
			TableTag table2 = (TableTag)nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "table11"),true)
				.elementAt(0);
			TableTag table3 = (TableTag)nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "table10"),true)
				.elementAt(0);
			TableTag table4 = (TableTag)taxTables.elementAt(0);
			
			NodeList paymentDetails = nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_plPayment"),true);
			
			String[] accountArray = table1.getChildren()
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_lblTaxBillNumber"), true)
				.elementAt(0).toPlainTextString().split("\\s+");
			
			details.append("<table width=\"90%\" align=\"center\" border=\"1\" cellpadding=\"15\"><tr><td>");
			
			if(accountId != null) { //details for last year
				accountId.append(accountArray[1] + "-" + accountArray[2]);
				
				if(taxTables.size() > 1) {  //details already formatted
					return rsResponse;
				}
				
				details.append(table1.toHtml());
				details.append(table2.toHtml());
				details.append(table3.toHtml());
			}
			
			NodeList nodeList1 = taxTables.extractAllNodesThatMatch(new TagNameFilter("td"),true);
			boolean isDelinquent = false;
			for (int i = 0; i < nodeList1.size(); i++) {
				String plainText = nodeList1.elementAt(i).toPlainTextString().trim();
				if(plainText.toUpperCase().indexOf("UNPAID BALANCE:") > -1) {
					i+=2;
					plainText = nodeList1.elementAt(i).toPlainTextString().replaceAll("[,$]", "").trim();
					if(!plainText.matches("0+[.]0+")) {
						isDelinquent = true;
					}
				}
			}
			
			if(accountId != null || isDelinquent) {
				details.append("<h3>Year " + accountArray[2] + "</h3><br>");
				details.append(table4.toHtml());
				if(paymentDetails.size() > 0) {
					details.append("<br>" + paymentDetails.elementAt(0).toHtml() + "<br>");
				}
			}
			
			if(rsResponse.toUpperCase().contains("***PRIOR YEARS TAX DUE***")) {
				Pattern pattern = Pattern.compile(".*TaxYear=([0-9]{4}).*");
				Matcher matcher = pattern.matcher(currentYearDocLink);
				if(matcher.matches()) {
					String prevYearDocLink = currentYearDocLink
						.replaceAll("TaxYear=[0-9]{4}", "TaxYear=" + (Integer.valueOf(matcher.group(1)) - 1));
					String prevYearDocHtml = getLinkContents(prevYearDocLink);
					details.append(getDetails(prevYearDocHtml, null, prevYearDocLink));
				}
			}
			details.append("</td></tr></table>");
			
			return details.toString();
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
		
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
	
		try {
			detailsHtml = detailsHtml.replaceAll("<br>", "\n");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			TableTag table1 = (TableTag)nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "AutoNumber1"),true)
				.elementAt(0);
			
			String[] accountArray = table1.getChildren()
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_lblTaxBillNumber"), true)
				.elementAt(0).toPlainTextString().split("\\s+");
			
			resultMap.put("PropertyIdentificationSet.ParcelID", accountArray[1]);
			resultMap.put("TaxHistorySet.Year", accountArray[2]);
			resultMap.put("OtherInformationSet.SrcType","TR");
			
			NodeList nodeList1 = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "table11"),true)
				.extractAllNodesThatMatch(new TagNameFilter("td"),true);
			String address = "";
			for (int i = 0; i < nodeList1.size(); i++) {
				String plainText = nodeList1.elementAt(i).toPlainTextString().trim();
				if(plainText.startsWith("PROPERTY ADDRESS:")) {
					address = plainText.replace("PROPERTY ADDRESS:", "").trim();
					resultMap.put("PropertyIdentificationSet.AddressOnServer", address);
				} else if(plainText.startsWith("TAXABLE VALUE:")) {
					plainText = nodeList1.elementAt(++i).toPlainTextString().trim();
					resultMap.put("PropertyAppraisalSet.TotalAssessment",
							plainText.replaceAll("[,$]","").trim());
				} else if(plainText.startsWith("ACTUAL VALUE:")) {
					plainText = nodeList1.elementAt(++i).toPlainTextString().trim();
					resultMap.put("PropertyAppraisalSet.TotalAppraisal",
							plainText.replaceAll("[,$]","").trim());
				}
			}
			
			NodeList nodeList2 = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "table10"),true)
				.extractAllNodesThatMatch(new TagNameFilter("td"),true);
 			TableColumn col1 = (TableColumn)nodeList2.elementAt(2);
			String fullName = col1.toPlainTextString().trim();
			resultMap.put("PropertyIdentificationSet.NameOnServer", fullName);
			
			TableColumn col2 = (TableColumn)nodeList2.elementAt(3);
			String legal = "";
			if(col2.toPlainTextString().trim().toUpperCase().startsWith("DELINQUENT")) { // CO Clear Creek TR
				legal = col2.toPlainTextString().trim().split("\n",2)[1];
				resultMap.put("PropertyIdentificationSet.PropertyDescription", legal);
			} else {
				legal = col2.toPlainTextString().trim();
				resultMap.put("PropertyIdentificationSet.PropertyDescription", legal); 
			}
			
			NodeList nodeList3 = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "Table3"),true)
				.extractAllNodesThatMatch(new TagNameFilter("td"),true);
			boolean lastYear = true;
			double priorDelinquent = 0.0;
			for (int i = 0; i < nodeList3.size(); i++) {
				String plainText = nodeList3.elementAt(i).toPlainTextString().trim();
				if(lastYear && plainText.startsWith("TAX:")) {
					i+=2;
					plainText = nodeList3.elementAt(i).toPlainTextString().trim();
					resultMap.put("TaxHistorySet.BaseAmount",
						plainText.replaceAll("[,$]","").trim());
				} else if(plainText.startsWith("UNPAID BALANCE:")) {
					i+=2;
					plainText = nodeList3.elementAt(i).toPlainTextString().trim();
					if(lastYear) {
						resultMap.put("TaxHistorySet.TotalDue", plainText.replaceAll("[,$]","").trim());
					} else {
						priorDelinquent += Double.valueOf(plainText.replaceAll("[,$]","").trim());
					}
					lastYear = false;
				}
			}
			resultMap.put("TaxHistorySet.PriorDelinquent", String.valueOf(priorDelinquent));
			
			NodeList nodeList4 = nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_lblLastPaid"),true);
			if(nodeList4.size() > 0) {
				resultMap.put("TaxHistorySet.DatePaid", nodeList4.elementAt(0).toPlainTextString().trim());
			}
			
			String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			if("san miguel".equals(crtCounty.toLowerCase())) {
				double amountPaid = 0.0;
				NodeList nodeList5 = nodeList
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_lblTotal"),true);
				if(nodeList5.size() > 0) amountPaid = Double.parseDouble(nodeList5.elementAt(0).toPlainTextString().replaceAll("[,$]","").trim());
				NodeList nodeList6 = nodeList
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_lblUnpaidBalance"),true);
				if(nodeList6.size() > 0) amountPaid = amountPaid -= Double.parseDouble(nodeList6.elementAt(0).toPlainTextString().replaceAll("[,$]","").trim());
				resultMap.put("TaxHistorySet.AmountPaid", Double.toString(amountPaid));
			}
			else {
				NodeList nodeList5 = nodeList
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_lblAmountPaid"),true);
				if(nodeList5.size() > 0) {
					resultMap.put("TaxHistorySet.AmountPaid", nodeList5.elementAt(0).toPlainTextString().replaceAll("[,$]","").trim());
				}
			}
			
			if("clear creek".equals(crtCounty.toLowerCase())) {
				ro.cst.tsearch.servers.functions.COClearCreekTR.parseNames(resultMap, fullName, true);
				ro.cst.tsearch.servers.functions.COClearCreekTR.parseLegalSummary(resultMap, searchId);
				ro.cst.tsearch.servers.functions.COClearCreekTR.parseAddress(resultMap, address, searchId);
			} else if("summit".equals(crtCounty.toLowerCase())) {
				ro.cst.tsearch.servers.functions.COSummitTR.parseNames(resultMap, fullName, true);
				ro.cst.tsearch.servers.functions.COSummitTR.parseLegalSummary(resultMap, legal);
				ro.cst.tsearch.servers.functions.COSummitTR.parseAddress(resultMap, address);
			} else if("san miguel".equals(crtCounty.toLowerCase())) {
				ro.cst.tsearch.servers.functions.COSanMiguelTR.parseAddress(resultMap, searchId);
				ro.cst.tsearch.servers.functions.COSanMiguelTR.parseLegalSummary(resultMap, searchId);
				ro.cst.tsearch.servers.functions.COSanMiguelTR.parseNames(resultMap, searchId);
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		
		FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId);
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		
		// search by PIN
		String pin = getParcelNo();
		
		if(!isEmpty(pin)){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));			
			module.clearSaKeys();
			module.getFunction(0).forceValue(pin);  
			module.addFilter(taxYearFilter);
			modules.add(module);
		}
		
		// search by Address
		String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		String strName = getSearchAttribute(SearchAttributes.P_STREETNAME);
		boolean hasAddress = !isEmpty(strNo) && !isEmpty(strName);
		if(hasAddress){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(strNo);  
			module.getFunction(1).forceValue(strName);	
			module.addFilter(addressFilter);
			module.addFilter(taxYearFilter);
			modules.add(module);			
		}
		
		// search by name - filter by address
		if(hasOwner()) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, module));
			module.addFilter(addressFilter);
			module.addFilter(taxYearFilter);
			
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L F M;;", "L, F M;;", "L F;;", "L, F;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

	protected String getParcelNo() {
		return getSearchAttribute(SearchAttributes.LD_PARCELNO);
	}
}
