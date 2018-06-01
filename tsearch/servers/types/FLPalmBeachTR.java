package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class FLPalmBeachTR extends TemplatedServer {

	private static final long serialVersionUID = -1890358020597112978L;

	public FLPalmBeachTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);

		int[] intermediary_cases = { ID_INTERMEDIARY, TSServer.ID_SEARCH_BY_NAME, ID_SEARCH_BY_ADDRESS, ID_SEARCH_BY_PARCEL,
				ID_SEARCH_BY_MODULE20 };
		setIntermediaryCases(intermediary_cases);
  
		int[] details_cases = { TSServer.ID_DETAILS };
		setDetailsCases(details_cases);

		int[] save_cases = { TSServer.ID_SAVE_TO_TSD };
		setSAVE_CASES(save_cases);
		
		setDetailsMessage("Account Detail");
		
		CharSequence[] intermediary_message = new String[] { "Total records:", "Your search has returned more than" };
		setIntermermediaryMessages(intermediary_message);
	}

	private String getAOLinkSalesInfo() {
		return "http://www.co.palm-beach.fl.us/papa/aspx/web/allsales.aspx?entity_id=";
	}
	
	@Override
	protected String clean(String response) {
		return super.clean(response);
	}

	@Override
	protected String cleanDetails(String response) {
		StringBuilder sb = new StringBuilder();
		String owner2="";
		Matcher mat = Pattern.compile("(?is)owner2=([\\w+]*)").matcher(response);
		if (mat.find()) owner2 = mat.group(1);
		owner2=owner2.replaceAll("\\+", " ");
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList divList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "dnn_ctr506_ContentPane"), true);
			if (divList.size()>0)
				sb.append("<b>Account Information</b><br><br>").append(divList.elementAt(0).toHtml()
						.replaceFirst("(?is)<table", "<table id=\"accountInformation\""));			
			divList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "lxT508"), true);
			if (divList.size()>0)
				sb.append("<br><br><b>Tax Bills</b><br><br>").append(divList.elementAt(0).toHtml()
						.replaceFirst("(?is)<table", "<table id=\"taxBills\""));
			
			NodeList aList = nodeList.extractAllNodesThatMatch(new TagNameFilter("a"), true);
			for (int i=0;i<aList.size();i++) {
				String link = "";
				Matcher matcher = Pattern.compile("(?is)<a.*?href=\"(.*?)\"").matcher(aList.elementAt(i).toHtml());
				if (matcher.find()) {
					link = matcher.group(1);
					if (link.matches("(?is).*\\by=\\d{4}\\b.*")) {
						String billDet = getBillDetails(link);
						sb.append(billDet);
					}
						
				}
			}
				
		} catch (ParserException e) {
			logger.error("Error while getting details: ", e);
		}
		
		String accountNumber = getAccountNumber(response);
		String saleInfoTable = getSaleInfoTable(accountNumber.replaceAll("-", ""));
		sb.append(saleInfoTable);
		
		response = sb.toString();
		
		if (!owner2.isEmpty())
		{
			Matcher m = Pattern.compile("(?is)(ui-widget\\s*top.*?owner.*?<br/>.*?)</td>").matcher(response);
			if (m.find())
				response = response.replace(m.group(1), m.group(1) + " " + owner2);
		}
		response = response.replaceAll("(?is)<a.*?>(.*?)</a>", "$1");
		
		return response;
	}

	private ro.cst.tsearch.servers.functions.FLPalmBeachTR instance = ro.cst.tsearch.servers.functions.FLPalmBeachTR.getInstance();

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), getAccountNumber(detailsHtml));
		instance.parseDetails(response.getResult(), searchId, map);
		return null;
	}

	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type", "CNTYTAX");
		String year = instance.getCurrentYear(serverResult);
		data.put("year", year);
		return data;
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> parseIntermediary = new Vector<ParsedResponse>();
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "grm-search"), true);
			String intermediaryTable = "";
			if (tableList.size()>0)
				intermediaryTable =  tableList.elementAt(0).toHtml();
			MessageFormat link = createPartialLinkFormat();
			parseIntermediary = instance.parseIntermediary(response, intermediaryTable, searchId, link);

			String numberOfResults = RegExUtils.getFirstMatch("(?is)(?<=<label>)Total records:.*?(\\d+).*(?=</span>)", table, 1);
			String tooManyResultsMessage = "";
			if (StringUtils.isEmpty(numberOfResults)) {// check to see if it reaches
													   // the limit by the original
													   // site
				numberOfResults = RegExUtils.getFirstMatch("Your search has returned more than (\\d+) records", intermediaryTable, 1);
				tooManyResultsMessage = "<br/>" + RegExUtils.getFirstMatch("<i>.*</i>", table, 0);
			}
			int resultCount = 0;
			if (org.apache.commons.lang.math.NumberUtils.isNumber(numberOfResults)) {
				resultCount = Integer.valueOf(numberOfResults);
			}

			String footer = response.getParsedResponse().getFooter();
			if (numberOfResults.trim().length()!=0)
				footer += "<br/>" + numberOfResults + " record(s) found.";

			String links = processLinks(response, resultCount);
			footer += "<br/>" + links;
			footer += tooManyResultsMessage;

			response.getParsedResponse().setFooter(footer);
			
		} catch (ParserException e) {
			logger.error("Error while parsing intermediary data: ", e);
		}
		
		return parseIntermediary;
	}

	private String processLinks(ServerResponse response, int resultCount) {
		try {
			String baseLink = CreatePartialLink(TSConnectionURL.idGET);
			String links = "";
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response.getResult(), null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList linksList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "search-results-bar"), true);
			if (linksList.size()>0)
				links = linksList.elementAt(0).toHtml();
				links = links.replaceAll("(?is)<script.*?</script>", "");
				links = links.replaceAll("(?is)class=\"[^\"]+\"", "");
				links = links.replaceAll("(?is)onclick=\"[^\"]+\"", "");
				links = links.replaceAll("(?is)href=\"", "href=\"" + baseLink + "/Tabs/PropertyTax.aspx");
				links = links.replaceAll("(?is)>..\\s*(Prev(:?ious)?)\\s*<", ">&lt;&lt;$1<");
				links = links.replaceAll("(?is)>\\s*Next\\s*..<", ">Next&gt;&gt;<");
				links = links.replaceAll("(?is)</a><a", "</a>&nbsp;<a");
				links = links.replaceAll("(?is)href=\"[^\\s]+#\"", "");
				links = links.replaceAll("(?is)<a\\s*>(.*?)</a>", "$1");
				return links;
		} catch (ParserException e) {
			logger.error("Error while getting navigation links: ", e);
		}
		return "";
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.7d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		TaxYearFilterResponse fr = new TaxYearFilterResponse(searchId);
		fr.setThreshold(new BigDecimal("0.95"));
		
		TSServerInfoModule module = null;
		if (hasPin()){
			String parcelID = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			if (parcelID.length() == 17) 					// insert dashes in parcel ID
			{
				parcelID = parcelID.substring(0, 2) + "-"
						 + parcelID.substring(2, 4) + "-"
						 + parcelID.substring(4, 6) + "-"
						 + parcelID.substring(6, 8) + "-"
						 + parcelID.substring(8, 10) + "-" 
						 + parcelID.substring(10, 13) + "-"
						 + parcelID.substring(13, 17);
			}
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();			
			module.forceValue(0, parcelID);
			l.add(module);
		}
		
		if (hasStreet()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.P_STREET_NO_NAME);
			
			module.addFilter(pinFilter);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			
			l.add(module);
		}
		
		if (hasOwner()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			module.addFilter(pinFilter);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
//			
//			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(
//					module, searchId, new String[] { "L; F M;", "L; f m;", "L; f;", "L; m;" });
//			
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
			module.addIterator(nameIterator);
			l.add(module);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX45));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			module.addFilter(pinFilter);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator1 = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
				module.addIterator(nameIterator1);
				l.add(module);	
		}
		
		serverInfo.setModulesForAutoSearch(l);
	}

	protected void setMessages() {
		getErrorMessages().addNoResultsMessages("No records found");
	}

	protected String getAccountNumber(String serverResult) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(serverResult, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "ui-widget-content ui-table"), true);
			if (tableList.size()>0) {
				TableTag table = (TableTag)tableList.elementAt(0);
				String accountNumber = table.getRows()[1].getColumns()[0].toPlainTextString().trim();
				return accountNumber;
			}
		} catch (ParserException e) {
			logger.error("Error while parsing details: ", e);
		}
		return "";
	}

	protected String getLinkPrefix(int type) {
		return ACTION_TYPE_LINK + "=" + type + "&" + msPrmNameLink + "=";
	}

	protected String getBillDetails(String address) {
		StringBuilder sb = new StringBuilder();
		String billDetailPage = getLinkContents(address);
		sb.append("<br><br>");
		Matcher matcher = Pattern.compile("(?is).*\\by=(\\d{4}).*").matcher(address);
		if (matcher.find()) {
			sb.append("<b>").append(matcher.group(1)).append("</b><br><br>");
		}
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(billDetailPage, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList list = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "lxT512"), true)
				.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (list.size()>0)
				sb.append("<b> Tax & Assessment</b><br>")
					.append(list.elementAt(0).toHtml()).append("<br>");
			list = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "installments"), true);
			if (list.size()>0)
				sb.append("<b>Tax Installment</b>\n").append(list.elementAt(0).toHtml()).append("<br>");
			list = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "lxT514"), true);
			if (list.size()>0)
				sb.append("<b>Tax Payment</b>\n")
					.append(list.elementAt(0).toHtml().replaceFirst("(?is)<table", "<table id=\"payments\""));
			
		} catch (ParserException e) {
			logger.error("Error while getting details: ", e);
		}
		
		return sb.toString();
	}
	
	protected String getSaleInfoTable(String accountId) {
		
		String assessorPage = getLinkContents(getAOLinkSalesInfo() + accountId);
		
		List<String> matches = RegExUtils.getMatches("(?is)<table.*?</table>", assessorPage, 0);

		String html = "";
		if (matches.size() == 2) {
			html = "<br><br>" + matches.get(1);
			// clean it
			html = html.replaceAll("(?is)<a.*?>", "");
			html = html.replaceAll("(?is)<TABLE", "<TABLE width=\"100%\" class=\"saleDataInfo\"");
		}
		return html;
	}
}
