package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.RegExUtils;
public class COBoulderTR extends COGenericTylerTechTR{ 
	
	private static final Pattern TOWNSHIP_PATTERN = Pattern.compile("(\\d+)-([\\dA-Z]+)-(\\d+)");
	
	private static final long serialVersionUID = 1L;
	private static int unqDmyNmbr = 0;
	public COBoulderTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);

	}
	protected int idGetLink(String rsResponse) {
		int viParseID = ID_DETAILS;
		if (rsResponse.matches("(?is).*<body id=\"PAGE_TAX_ACCOUNT_SEARCH\">.*?<h1>.*?Search\\s+Result.*?</h1>.*")) {
			viParseID = ID_SEARCH_BY_NAME;
		}
		return viParseID;
	}
	
	public String getDetailsPrefix() {
		return "/treasurer/treasurerweb/";
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap m) {

		try {
			HtmlParser3 parser = new HtmlParser3(detailsHtml);
			NodeList nodeList = parser.getNodeList();
			String accountNo = ro.cst.tsearch.servers.functions.COWeldTR.clean(StringUtils.prepareStringForHTML(
					HtmlParser3.getValueFromNextCell(
							HtmlParser3.findNode(nodeList, "Account&nbsp;Id"), "", true)));
			String parcelNo = ro.cst.tsearch.servers.functions.COWeldTR.clean(StringUtils.prepareStringForHTML(
					HtmlParser3.getValueFromNextCell(
							HtmlParser3.findNode(nodeList, "Parcel&nbsp;Number"), "", true)));
			String address = ro.cst.tsearch.servers.functions.COWeldTR.clean(StringUtils.prepareStringForHTML(
					HtmlParser3.getValueFromNextCell(
							HtmlParser3.findNode(nodeList, "Situs&nbsp;Address"), "", true))).replaceAll("\\b0*\\b", "");

			String legal = ro.cst.tsearch.servers.functions.COWeldTR.clean(StringUtils.prepareStringForHTML(
					HtmlParser3.getValueFromNextCell(
							HtmlParser3.findNode(nodeList, "Legal"), "", true)));

			m.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNo);
			m.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), parcelNo);
			m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");

			RegExUtils.extractInfoIntoMap(m, legal, "\\b(?:BLDG|BUILDING) ((?:[A-Z\\d]+)\\b(?:[A-Z]|\\d+$)?)",
					PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName());
			RegExUtils.extractInfoIntoMap(m, legal, "(?is)\\bTRACT\\s+(\\d+)",
					PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName());
			RegExUtils.extractInfoIntoMap(m, legal, "\\bPH(?:ASE)? (\\d+)\\b",
					PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName());
			RegExUtils.extractInfoIntoMap(m, legal, "\\b(?:UNIT|APT(?:\\s+NO)?) ([-\\w]+)\\b",
					PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName());
			
			//cross references
			List<String> line = null;
			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();
			Pattern pattern = Pattern.compile("(?is)\\s+REC\\s*(?:NO)?\\s*(\\d+)\\s*((\\d{2}/\\d{2}/\\d{2,4})\\s*([a-z]+))?");
			Matcher matcher = pattern.matcher(legal);
			
			while (matcher.find()) {
				line = new ArrayList<String>();
				line.add(matcher.group(1));
				line.add(matcher.group(2));
				line.add(matcher.group(3));
				body.add(line);
			}
			
			if (body != null && body.size() > 0) {
				ResultTable rt = new ResultTable();
				String[] header = { "InstrumentNumber", "InstrumentDate", "DocumentType" };
				rt = GenericFunctions2.createResultTable(body, header);
				m.put("SaleDataSet", rt);
			}
			
			// subdivision
			matcher = TOWNSHIP_PATTERN.matcher(legal);
			if (matcher.find()) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), matcher.group(1));
				m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), matcher.group(2));
				m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), matcher.group(3));
			}
			legal = legal.replaceAll("(?is)\\\\|&.*\\s+common\\s+elements", "");
			legal = legal.replaceAll("(?is)\\bLO?TS?\\s+(?:[-\\d\\s&,]+)", "");
			legal = legal.replaceAll("(?is)\\bDEEDS?\\s+(?:[-\\d\\s&,/]+)", "");
			legal = legal.replaceAll("(?is)\\bBL?O?C?K\\s+(?:\\d+|[A-Z])", "");
			legal = legal.replaceAll("(?is)\\b(?:BLDG|BUILDING)\\s+(?:\\d+|[A-Z])(?:\\s*&\\s*(?:UND)?\\s*.?\\d*)?", "");
			legal = legal.replaceAll("(?is)\\bUNIT\\s+(?:\\d+|[A-Z])", "");
			legal = legal.replaceAll("(?is)\\bFLG|REPLAT|MHP|PH(?:ASE)|per?\\b.*", "");
			legal = legal.replaceAll("(?is)^\\s*Personal\\s+Property.*", "");
			legal = legal.replaceFirst("(?is)\\s*TR(?:ACT)?\\s*(?:[-\\d\\s&,]+)", "");
			legal = legal.replaceAll("(?is)[NEWS]+\\s*\\d*/\\d*", "");
			legal = legal.replaceAll("(?is)&?\\s*PT\\s+.*", "");
			legal = legal.replaceAll("(?is)(?:^|\\s+)LESS\\s+.*", "");
			legal = legal.replaceAll("(?is)\\s+SPLIT\\s+(?:FROM)?(?:\\s+ID)?", "");
			legal = legal.replaceAll("(?is)\\s+CORNER\\s+(?:[NEWS]+)?", "");
			legal = legal.replaceAll("(?is)\\s+PROPERTY\\s+ADDRESS\\b.*", "");
			legal = legal.replaceAll("(?is)\\s+CONDOS?\\b*", "");
			
			Pattern subdNamePattern = Pattern.compile("(?is).*?((?:[A-Z]{4,})(?:\\s*[A-Z]{2,})?(?:\\s*[A-Z]{4,})?(?:\\s*[A-Z]{4,})?(?:\\s*\\d*)?).*");
			Matcher subdNameMatcher = subdNamePattern.matcher(legal);
			if (subdNameMatcher.find()) {
				legal = subdNameMatcher.group(1);
			}
			else {
				
				subdNamePattern = Pattern.compile("(?is).*?((?:[A-Z]{3,})(?:\\s*[A-Z]{4,})(?:\\s*[A-Z]{4,})?(?:\\s*[A-Z]{4,})?(?:\\s*\\d*)?).*");
				subdNameMatcher = subdNamePattern.matcher(legal);
				if (subdNameMatcher.find()) {
					legal = subdNameMatcher.group(1);
				}
				else {
					legal = "";
				}
			}
			if (!legal.isEmpty()) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), legal);
			}

			// propertyAppraisalSet
			NodeList propertyAppraisalTable = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "tax-value"), true);
			String land = "";
			String improvement = "";
			String totalAppraisal = "";

			if (propertyAppraisalTable.size() > 0)
			{
				land = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(propertyAppraisalTable, "land"), "", true).replaceAll("[ $,-]", "")
						.trim();
				improvement = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(propertyAppraisalTable, "improvement"), "", true).replaceAll(
						"[ $,-]", "").trim();
				totalAppraisal = HtmlParser3.getValueFromAbsoluteCell(0, 2, HtmlParser3.findNode(propertyAppraisalTable, "Total"), "", true).replaceAll(
						"[ $,-]", "").trim();

				if (!land.isEmpty()) {
					m.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land);
				}
				if (!improvement.isEmpty()) {
					m.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvement);
				}
				if (!totalAppraisal.isEmpty()) {
					m.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), totalAppraisal);
					m.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), totalAppraisal);
				}
			}

			// city
			Pattern pat = Pattern.compile("(?is).*\\s+(\\w+)\\s*$");
			Matcher mat = pat.matcher(address);
			String city = "";
			if (mat.find()) {
				city = mat.group(1);
				address = address.replaceAll(city, "").trim();
				m.put(PropertyIdentificationSetKey.CITY.getKeyName(), city.replaceAll("_", " "));
			}

			m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);

			try {
				GenericFunctions.parseAddressOnServer(m);
				ro.cst.tsearch.servers.functions.COBoulderTR.parseNames(parser, m, searchId, this);
				ro.cst.tsearch.servers.functions.COWeldTR.parseLegal(parser, m, searchId, this);
				ro.cst.tsearch.servers.functions.COWeldTR.parseTaxInfo(parser, m, searchId);
				ro.cst.tsearch.servers.functions.COWeldTR.parseSaleDataInfo(parser, m, searchId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}	
	public static Node getNodeByTypeAttributeDescription(NodeList nl, String type, String attributeName, String attributeValue, String[] description,
			boolean recursive) {
		NodeList returnList = null;

		if (!StringUtils.isEmpty(attributeName))
			returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive).extractAllNodesThatMatch(
					new HasAttributeFilter(attributeName, attributeValue), recursive);
		else
			returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive);

		for (int i = returnList.size() - 1; i >= 0; i--) {
			boolean flag = true;
			for (String s : description) {
				if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(returnList.elementAt(i).toHtml(), s))
					flag = false;
			}
			if (flag)
				return returnList.elementAt(i);
		}

		return null;
	}
		
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {

		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			HtmlParser3 parser = new HtmlParser3(table);
			TableTag mainTable = (TableTag) parser.getNodeById("searchResultsTable", true);
			TableRow[] rows = mainTable.getRows();
			for (TableRow row : rows) {
				if (row.getColumnCount() > 0) {

					String link = HtmlParser3.getFirstTag(row.getColumns()[0].getChildren(), LinkTag.class, true).getLink();

					link = link.replaceFirst("\\?", "?dummy=true&");
					link = CreatePartialLink(TSConnectionURL.idGET) + getDetailsPrefix() + link;// + "&print=true";
					String rowHtml = row.toHtml().replaceAll("(?s)<a.*?>(.*?)</a>", "<a href=\"" + Matcher.quoteReplacement(link) + "\">$1</a>");

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

					ResultMap m = ro.cst.tsearch.servers.functions.COBoulderTR.parseIntermediaryRow(row);
					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);
				}
			}

			String header0 = "<tr><td colspan=2>" + ((Span) parser.getNodeByAttribute("class", "pagebanner", true)).toHtml()
					+ proccessLinks(response, (Span) parser.getNodeByAttribute("class", "pagelinks", true)) + "</td></tr>";
			String header1 = rows[0].toHtml();

			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
				response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header0 + header1);
				response.getParsedResponse().setFooter("</table>");
			}

			outputTable.append(table);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return intermediaryResponse;
	}

	private String proccessLinks(ServerResponse response, Span linksSpan) {

		try {
			Node[] links = linksSpan.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).toNodeArray();
			for (Node n : links) {
				LinkTag lnk = (LinkTag) n;
				String link = lnk.getLink();
				link = link.replace("/treasurerweb/..", "");
				lnk.setLink(CreatePartialLink(TSConnectionURL.idGET) + link);
				if (lnk.getChildrenHTML().contains("Next")) {

					if (InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchType() != Search.PARENT_SITE_SEARCH) {
						lnk.setLink(lnk.getLink() + "&unqDmyNmbr=" + unqDmyNmbr++);
					}
					String linkNext = lnk.toHtml().replaceAll("(?i)&amp;", "&");
					response.getParsedResponse().setNextLink(linkNext);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return linksSpan.toHtml();
	}

	@Override
	protected String getDetails(String response, String originalLink, String accountNo) {
		String result = super.getDetails(response, originalLink, accountNo);
		return result.replaceAll("(?i)<a.*?>(.*?)</a>", "$1");
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m;

		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.7d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);

		if (hasPin()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
			l.add(m);
		}

		if (StringUtils.isNotEmpty(getSearchAttribute(SearchAttributes.LD_PARCELNO2))) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(4).setSaKey(SearchAttributes.LD_PARCELNO2);
			l.add(m);
		}

		if (hasStreet()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(4).setSaKey(SearchAttributes.P_STREET_NO_NAME);
			if (hasOwner()) {
				m.addFilter(nameFilterHybrid);
			}
			m.addFilter(addressFilter);
			m.addFilter(defaultLegalFilter);
			l.add(m);
		}

		if (hasOwner())
		{
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.getFunction(4).setSaKey(SearchAttributes.P_STREET_NO_NAME);
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			m.addFilter(NameFilterFactory.getNameFilterWithScore(SearchAttributes.OWNER_OBJECT, searchId, m, 0.66d));
			if (hasStreet()) {
				m.addFilter(addressFilter);
			}
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId,
					new String[] {"L;F;"});
			m.addIterator(nameIterator);
			l.add(m);
		}
		serverInfo.setModulesForAutoSearch(l);
	}

}
