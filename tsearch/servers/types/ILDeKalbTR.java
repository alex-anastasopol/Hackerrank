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
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.MultiplePinFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class ILDeKalbTR extends TemplatedServer {

	private static final long serialVersionUID = 74927104837294L;
	
	public static final String YEAR_PATTERN = "(?is)(\\d{4})\\s+\\(payable\\s+\\d{4}\\)\\s+Tax Bill\\s+Details";

	public ILDeKalbTR(long searchId) {
		super(searchId);
		setSuper();
	}

	public ILDeKalbTR(String rsRequestSolverName, String rsSitePath,String rsServerID, String rsPrmNameLink, long searchId,	int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		setSuper();
	}

	public void setSuper() {
		int[] intermediary_cases = new int[] { ID_SEARCH_BY_PARCEL, ID_SEARCH_BY_NAME, ID_SEARCH_BY_ADDRESS };
		super.setIntermediaryCases(intermediary_cases);
		int[] details_cases = new int[] { ID_DETAILS };
		super.setDetailsCases(details_cases);
		super.setDetailsMessage("Tax Bill Address Information");
	}

	@Override
	protected void setMessages() {
		getErrorMessages().addServerErrorMessage("The page cannot be displayed because an internal server error has occurred.");
		getErrorMessages().addNoResultsMessages("No Results Were Found");
	}

	@Override
	protected void ParseResponse(String action, ServerResponse response,
			int viParseID) throws ServerResponseException {
		if (isError(response)) {
			response.setError("<font color=\"red\">No results found.</font>");
			response.setResult("");
			return;
		}
		if (response.getResult().contains("CLICK HERE TO VIEW PARCEL INFORMATION") || viParseID == ID_SAVE_TO_TSD)
			super.ParseResponse(action, response, viParseID);
		else if (response.getResult().contains("Tax Bill Address Information") || response.getResult().contains("Current Address and Tax Bill Information"))
			super.ParseResponse(action, response, ID_DETAILS);
	}

	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		String year = "";
		Matcher matcher = Pattern.compile(YEAR_PATTERN).matcher(serverResult); 
		if (matcher.find()) year = matcher.group(1);
		if (year.length()!=0) data.put("year", year);
		data.put("type", "CNTYTAX");
		
		return data;
	}

	@Override
	protected String getAccountNumber(String serverResult) {
		String parcelID = "";
		try {
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(serverResult, null);
				NodeList nodeList = htmlParser.parse(null);
				NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "parcelID"), true);
				if (parcelList.size()!=0) parcelID = parcelList.elementAt(0).toPlainTextString();
			
			return parcelID;
		} catch (ParserException e) {
			e.printStackTrace();
		}
		return parcelID;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainSelectList = nodeList.extractAllNodesThatMatch(new TagNameFilter("select"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("name", "id"), true);

			if (mainSelectList.size() == 0) {
				return intermediaryResponse;
			}

			//process the drop-down list (transform its every option into a table row)
			htmlParser = org.htmlparser.Parser.createParser(mainSelectList.elementAt(0).toHtml(), null);
			nodeList = htmlParser.parse(null);
			NodeList valuesList = nodeList.extractAllNodesThatMatch(new TagNameFilter("option"), true);
			for (int i=0;i<valuesList.size();i++) {
				String value = valuesList.elementAt(i).toPlainTextString();
				String row = "<tr>";
				String cols[] = value.split(" , ");
				String link = CreatePartialLink(TSConnectionURL.idPOST) + "/QTASResults.asp?id="	+ cols[0];
				row += "<td>" + "<a href=" + link + ">" + cols[0] + "</td>"; 
				for (int j=1;j<cols.length;j++) row+= "<td>" + cols[j].trim() + "</td>";
				for (int j=cols.length;j<4;j++) row+= "<td></td>";
				row += "</tr>";
				
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row);
				currentResponse.setOnlyResponse(row);
				currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
				
				ResultMap m = ro.cst.tsearch.servers.functions.ILDeKalbTR.parseIntermediaryRow(row, searchId);
				Bridge bridge = new Bridge(currentResponse, m, searchId);

				DocumentI document = (TaxDocumentI) bridge.importData();
				currentResponse.setDocument(document);

				intermediaryResponse.add(currentResponse);
			}
		
			response.getParsedResponse().setHeader("<table border=\"1\">\n"
														+ "<th align=\"left\" width=\"25%\">Parcel #</th>"
														+ "<th align=\"left\" width=\"25%\">Owner Name</th>"
														+ "<th align=\"left\" width=\"25%\">Address</th>"
														+ "<th align=\"left\" width=\"25%\">City</th>"
														+ "</tr>");
			response.getParsedResponse().setFooter("</table>");
			outputTable.append(table);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return intermediaryResponse;
	}

	@Override
	protected String clean(String response) {
		return response;
	}

	@Override
	protected String cleanDetails(String response) {
		
		String cleanResp = "";
		
		//find the index of the tag <div id="mainConyent">
		int index = response.indexOf("mainContent");
		if (index == -1) index = 0;
			else index = response.lastIndexOf("<", index);
		cleanResp = response.substring(index);
		
		String link = "http://gisweb.co.de-kalb.il.us/qtas";
		Matcher ma = Pattern.compile("(?is)href=\"\\.(/TaxPaymenthistory.asp\\?property_key='.*?')").matcher(response);
		if (ma.find()) link += ma.group(1);
		if (link.length()!=0) {
			String paymentsDetails = "";
			HttpSite paymentsPage = HttpManager.getSite(getCurrentServerName(), searchId);		//get last year payments
			try {
				paymentsDetails = ((ro.cst.tsearch.connection.http2.ILDeKalbTR)paymentsPage).getPage(link);
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(paymentsPage);
			}
			if (paymentsDetails.length()!=0) {
					cleanResp += "<br><br>";
					String lowercasePaymentsDetails = paymentsDetails.toLowerCase();
					int index1 = lowercasePaymentsDetails.indexOf("payment history details for");
					if (index1!=-1) index1 = lowercasePaymentsDetails.lastIndexOf("<h1>",index1);
					int index2 = lowercasePaymentsDetails.indexOf("total:");
					if (index2!=-1) index2 = lowercasePaymentsDetails.indexOf("<hr>",index2);
					if (index1!=-1 && index2!=-1) cleanResp += paymentsDetails.substring(index1, index2)
						.replaceFirst("(?is)<HR>", "").replaceFirst("(?is)<table", "<table id=\"lastYearPayments\"");
			}
		}
		
		link = "http://gisweb.co.de-kalb.il.us/qtas";
		ma = Pattern.compile("(?is)href=\"\\.(/SalesHistoryDetails.asp\\?parcel_number='.*?')").matcher(response);
		if (ma.find()) link += ma.group(1);
		if (link.length()!=0) {
			String transactionsDetails = "";
			HttpSite transactionsPage = HttpManager.getSite(getCurrentServerName(), searchId);		//get transactions
			try {
				transactionsDetails = ((ro.cst.tsearch.connection.http2.ILDeKalbTR)transactionsPage).getPage(link);
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(transactionsPage);
			}
			if (transactionsDetails.length()!=0) {
					cleanResp += "<br><br>";
					String lowercaseTransactionsDetails = transactionsDetails.toLowerCase();
					int index1 = lowercaseTransactionsDetails.indexOf("sales history details for");
					if (index1!=-1) index1 = lowercaseTransactionsDetails.lastIndexOf("<h1>",index1);
					int index2 = lowercaseTransactionsDetails.indexOf("property type");
					if (index2!=-1) index2 = lowercaseTransactionsDetails.indexOf("<hr>",index2);
					if (index1!=-1 && index2!=-1) cleanResp += transactionsDetails.substring(index1, index2)
						.replaceFirst("(?is)<HR>", "").replaceFirst("(?is)<table", "<table id=\"transactions\"");
			}
		}

		//clean links and texts for links
		cleanResp = cleanResp.replaceAll("(?is)<a.*?</a>", "");
		cleanResp = cleanResp.replaceFirst("Click here for a map of this property", "");
		cleanResp = cleanResp.replaceFirst("\\* Click the link for Tax Code Details for tax code descriptions", "");
		cleanResp = cleanResp.replaceAll("\\(Board of Review Equalized\\)\\*", "(Board of Review Equalized)");
		cleanResp = cleanResp.replaceAll("\\*Please call 815-895-7120 to confirm assessment information", "");
		cleanResp = cleanResp.replaceAll("\\*Click the link for Sales History for more details", "");
		cleanResp = cleanResp.replaceAll("\\*Click the link for Tax Code Details for tax code descriptions", "");
		
		if (cleanResp.equals("")) 
			cleanResp = "No Results Found";
		
		//replace the font where parcel ID is found
		cleanResp = cleanResp.replaceFirst("(?is)<font color=#383798>([^<]+)", "<span id=\"parcelID\">$1</span>");
		//replace all fonts
		cleanResp = cleanResp.replaceAll("(?is)</?font.*?>", "");
		
		//replace tr, th and td tags which are not between <table> and </table>
		int i = 0; 				//current table tag
		int j = 0;				//previous table tag
		boolean opened = false;	//table tag opened
		String newCleanResp = "";
		while (i<cleanResp.length()) {
			i = cleanResp.toLowerCase().indexOf("table",i+1);
			if (i==-1) break;
			if (cleanResp.charAt(i-1)=='/') {
				if (!opened) {									//</table> tag without <table> tag
					newCleanResp += cleanResp.substring(0, i-2).replaceAll("(?is)</?t[rhd]>", "");
					j = i+"</table>".length();		
				}
				else {
					newCleanResp += cleanResp.substring(j,i);
					opened = false;
					j = i;
				}
			}
			else {
				newCleanResp += cleanResp.substring(j,i).replaceAll("(?is)</?t[rhd]>", "");
				opened = true;
				j = i;
			}
		} 
		
		if (newCleanResp.endsWith("/")) newCleanResp += "table>";
		
		return newCleanResp;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,	String detailsHtml, ResultMap resultMap) {

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response.getResult(), null);
			NodeList nodeList = htmlParser.parse(null);
			
			String parcel = getAccountNumber(detailsHtml);
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),parcel.trim());
			
			String year = "";
			Matcher matcher = Pattern.compile(YEAR_PATTERN).matcher(detailsHtml); 
			if (matcher.find()) year = matcher.group(1);
			if (year.length()!=0) resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);

			String lowercaseDetailsHtml = detailsHtml.toLowerCase();
			
			String owner = "";
			int index = lowercaseDetailsHtml.indexOf("current owner");
			if (index!=-1) {
				int index2 = lowercaseDetailsHtml.indexOf("tax bill payments");
				if (index2!=-1) {
					String fragment = detailsHtml.substring(index,index2);
					Matcher ma = Pattern.compile("(?is)</h2>(.*?)</p>").matcher(fragment);
					if (ma.find())
						owner = ma.group(1).trim().replaceAll("&nbsp;", "");
				}
				
			}
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),owner.replaceAll("<[^b].*?>", ""));
			ro.cst.tsearch.servers.functions.ILDeKalbTR.parseNamesDetails(resultMap);

			String address = "";
			index = lowercaseDetailsHtml.indexOf("site address");
			if (index!=-1) {
				Matcher ma = Pattern.compile("(?is)</h2>(.*?)<h2>").matcher(detailsHtml.substring(index));
				if (ma.find()) 
					address = ma.group(1).trim().replaceAll("&nbsp;", "");
			}
			
			if (address.length()!=0) resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(),address.replaceAll("<.*?>", ""));
			ro.cst.tsearch.servers.functions.ILDeKalbTR.parseAddress(resultMap);
			
			String legal = "";
			index = lowercaseDetailsHtml.indexOf("brief property description");
			if (index!=-1) {
				Matcher ma = Pattern.compile("(?is)</h2>(.*?)</p>").matcher(detailsHtml.substring(index));
					if (ma.find()) 
						legal = ma.group(1).trim().replaceAll("&nbsp;", "");
			}
			resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(),legal.replaceAll("<.*?>", ""));
			ro.cst.tsearch.servers.functions.ILDeKalbTR.parseLegalSummary(resultMap);
			
			ro.cst.tsearch.servers.functions.ILDeKalbTR.parseTaxes(resultMap, detailsHtml);
			
			ResultTable rt = new ResultTable();			//tax history table
			@SuppressWarnings("rawtypes")
			List<List> tablebody = new ArrayList<List>();
			List<String> list;
			
			//last year tax history
			Node nodeTaxes = null;
			NodeList nodeTaxesList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "lastYearPayments"));
			if (nodeTaxesList.size()>=1) nodeTaxes = nodeTaxesList.elementAt(0);
			if (nodeTaxes != null)
			{
				String lastYear = "";
				if (StringUtils.isNotEmpty(year) && year.trim().matches("\\d+")){
					Integer.toString(Integer.parseInt(year)-1);
				}
				String receiptDate = "";
				String receiptAmount = "";
				TableTag table = (TableTag)nodeTaxes;
				for (int i=1;i<table.getRowCount();i++)		//row 0 is the header
				{
					TableRow row = table.getRow(i);
					receiptDate = row.getColumns()[1].toPlainTextString().trim();
					receiptAmount = row.getColumns()[2].toPlainTextString();
					list = new ArrayList<String>();
					list.add(lastYear);
					list.add(receiptDate);
					list.add(receiptAmount);
					tablebody.add(list);
				}	
			}
			//current year tax history
			nodeTaxes = null;
			nodeTaxesList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
				.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "3"));
			if (nodeTaxesList.size()>=1) nodeTaxes = nodeTaxesList.elementAt(0);
			if (nodeTaxes != null)
			{
				String receiptDate = "";
				String receiptAmount = "";
				TableTag table = (TableTag)nodeTaxes;
				for (int i=1;i<table.getRowCount();i++)		//row 0 is the header
				{
					TableRow row = table.getRow(i);
					receiptDate = row.getColumns()[1].toPlainTextString().trim();
					receiptAmount = row.getColumns()[2].toPlainTextString().replaceAll("[\\$,]", "");
					list = new ArrayList<String>();
					list.add(year);
					list.add(receiptDate);
					list.add(receiptAmount);
					tablebody.add(list);
				}	
			}
			String[] header = {TaxHistorySetKey.YEAR.getShortKeyName(), TaxHistorySetKey.RECEIPT_DATE.getShortKeyName(), 
					TaxHistorySetKey.RECEIPT_AMOUNT.getShortKeyName()};
			rt = GenericFunctions2.createResultTable(tablebody, header);
			if (rt != null){
				resultMap.put("TaxHistorySet", rt);
			}
			
			if (detailsHtml.indexOf("Sales Information does not exist for this property")==-1) {
				ResultTable transactionHistory = new ResultTable();			//transaction history table
				@SuppressWarnings("rawtypes")
				List<List> tablebodytrans = new ArrayList<List>();
				List<String> listtrans;
				
				NodeList nodeTransactionTable = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)	
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "transactions"));
				if (nodeTransactionTable.size() >=1 )
				{
					TableTag transactionHistoryTable = (TableTag) nodeTransactionTable.elementAt(0);
					int rowsNumber = transactionHistoryTable.getRowCount();
					String date = "";
					String price = "";
					String number = "";
					
					for (int i=1; i<rowsNumber; i++)			//row 0 is the header
					{
						TableRow row = transactionHistoryTable.getRow(i);
						date = row.getColumns()[0].toPlainTextString();
						price = row.getColumns()[1].toPlainTextString().replaceAll("[,\\$]", "").trim();
						number = row.getColumns()[3].toPlainTextString().replaceFirst("Y{3,}-", "").trim();
						
						listtrans = new ArrayList<String>();
						listtrans.add(date);
						listtrans.add(price);
						listtrans.add(number);
						tablebodytrans.add(listtrans);
					}
					
					String[] headertrans = {SaleDataSetKey.RECORDED_DATE.getShortKeyName(),	SaleDataSetKey.SALES_PRICE.getShortKeyName(),
							SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName()};
					transactionHistory = GenericFunctions2.createResultTable(tablebodytrans, headertrans);
					if (transactionHistory != null){
						resultMap.put("SaleDataSet", transactionHistory);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		
		FilterResponse multiplePINFilter = new MultiplePinFilterResponse(searchId);
		FilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if (Search.AUTOMATIC_SEARCH == searchType){
			Collection<String> pins = getSearchAttributes().getPins(-1);
			if(pins.size() > 1){			
				for(String pin: pins){
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
					module.clearSaKeys();
					module.getFunction(0).forceValue(pin);
					module.addFilter(pinFilter);
					moduleList.add(module);	
				}			
				// set list for automatic search 
				serverInfo.setModulesForAutoSearch(moduleList);
				resultType = MULTIPLE_RESULT_TYPE;
				return;
			}
		}
		
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId , 0.8d , true);
		if(hasPin()) {
			
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.LD_PARCELNO);
			module.addFilter(pinFilter);
			module.addFilter(addressFilter);
			moduleList.add(module);
		}
						
				
		if(hasStreet()) {
			FilterResponse nameFilter = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(4, SearchAttributes.P_STREETNO);
			module.setSaKey(5, SearchAttributes.P_STREETNAME);
			module.addFilter(addressFilter);
			module.addFilter(nameFilter);
			module.addFilter(multiplePINFilter);
			moduleList.add(module);
		}
								
		if(hasOwner()) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;","L;M;"}));
			moduleList.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(moduleList);
	}
}
