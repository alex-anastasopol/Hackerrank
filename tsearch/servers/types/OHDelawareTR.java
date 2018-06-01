package ro.cst.tsearch.servers.types;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
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
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class OHDelawareTR extends TSServer {
	private static final long		serialVersionUID	= 1L;
	private static final Pattern	NEXT_LINK			= Pattern.compile("(?is)<a[^>]*href=\"([^\"]+)\"[^>]*>\\s*next");

	public OHDelawareTR(long searchId) {
		super(searchId);
	}

	public OHDelawareTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		mSearch.setAdditionalInfo("viParseID", viParseID);

		if (viParseID != ID_DETAILS && viParseID != ID_DETAILS1 && viParseID != ID_SAVE_TO_TSD && rsResponse.contains("Summary"))
			viParseID = ID_DETAILS1;
		if (rsResponse.contains("No Results Found")) {
			Response.getParsedResponse().setError("No Results Found!");
			return;
		}
		
		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}

			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());

			break;
		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
		case ID_DETAILS1:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			HashMap<String, String> data = new HashMap<String, String>();
			String details = getDetails(Response, rsResponse, accountId, data);
			String accountName = accountId.toString();

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&").replace("/..", "").replace("|", "%7C") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				if (isInstrumentSaved(accountName, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}
				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(Response, details);
				details = details.replaceAll("(?ism)<a href[^>]*>([^<]*)</a>", "$1");
				msSaveToTSDFileName = accountName + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();

			}

			break;
		case ID_GET_LINK:
			int parserID = 0;
			if(rsResponse.matches("(?is).*Property\\s+Info.*"))
				parserID = ID_DETAILS;
			else if(sAction.matches("(?i).*address.*"))
				parserID = ID_SEARCH_BY_ADDRESS;
			else if(sAction.matches("(?i).*Owner.*"))
				parserID = ID_SEARCH_BY_NAME;
			ParseResponse(sAction, Response, parserID);
			break;
		default:
			break;
		}
	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "CNTYTAX");
		}
	}

	protected String getDetails(ServerResponse response, String rsResponse, StringBuilder accountId, HashMap<String, String> data) {
		NodeList nodes = new HtmlParser3(rsResponse).getNodeList();

		/* If from memory - use it as is */
		if (!StringUtils.containsIgnoreCase(rsResponse, "<html")) {
			if (nodes.size() > 0) {
				accountId.append(ro.cst.tsearch.servers.functions.OHDelawareTR.getParcelNoFromHtml(nodes));
			}

			else
			{
				return null;
			}

			return rsResponse;
		}

		try {
			loadDataHash(data);
			StringBuilder details = new StringBuilder();
			String lastURI = "";

			Pattern p = java.util.regex.Pattern.compile("(?is)<title[^>]*>\\s*Property\\s+Number\\s+Search\\s*</title>");
			Matcher m = p.matcher(rsResponse);
			if (m.find())
			{
				p = java.util.regex.Pattern.compile("(?is)<a[^>]*class=\\s*\"buttonhover\"[^>]*href=\\s*\"([^\"]*)\"");
				m = p.matcher(rsResponse);
				if (m.find())
				{
					rsResponse = getLinkContents(m.group(1));
					lastURI = m.group(1);
				}
				
			}
			if (lastURI == "" && response.getLastURI() != null)
			{
				lastURI = response.getLastURI().toString();
			}

			p = java.util.regex.Pattern.compile("(?is)ows.create\\(([^>]*'(s[^']*lxsrc:[^']*)'[^\\)]*propertyinfo[^\\)]*','([^']*)'.*?)\\)");
			m = p.matcher(rsResponse);
			String detailsLink = "";
			String detailsResponse = "";

			if (m.find())
			{
				String GETparams = lastURI.substring(lastURI.indexOf("Property.aspx") + 14, lastURI.length() - 1);
				detailsLink = dataSite.getLink() + m.group(3) + "IM.aspx?" + GETparams + "&_OWS_=" + m.group(2);
				detailsResponse = getLinkContents(detailsLink);
			}

			// 9203
			String ownerNameIntermediary = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodes, "Owner Name"), "", true);

			nodes = new HtmlParser3(detailsResponse).getNodeList();
			String ownerInDetails = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "Owner Name"), "", true);

			if (!ownerNameIntermediary.isEmpty() && !ownerNameIntermediary.trim().equals(ownerInDetails.trim()) && !ownerInDetails.contains("&")) {
				detailsResponse = detailsResponse.replaceFirst(ownerNameIntermediary, ownerNameIntermediary + " & ");
			}

			nodes = new HtmlParser3(detailsResponse).getNodeList();
			NodeList tables = nodes
					.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("align", "center"), true);

			accountId.append(ro.cst.tsearch.servers.functions.OHDelawareTR.getParcelNoFromHtml(nodes));

			if (tables.size() > 0)
			{
				TableTag tablesTag = (TableTag) tables.elementAt(0).getFirstChild().getNextSibling();
				tablesTag.setAttribute("align", "center");

				details.append("<table id=\"finalResults\" width=\"800\" align=\"center\" border=\"2\">");
				details.append("<tr id=\"Summary\"><td>" + tablesTag
						.toHtml() + "</td></tr>");
			}

			p = java.util.regex.Pattern.compile("(?is)ows.create\\(([^>]*'(s[^']*lxsrc:[^']*)'[^\\)]*parcelbanner[^\\)]*','([^']*)'.*?)\\)");
			m = p.matcher(rsResponse);
			detailsLink = "";
			detailsResponse = "";

			if (m.find())
			{
				String GETparams = lastURI.substring(lastURI.indexOf("Property.aspx") + 14, lastURI.length() - 1);
				detailsLink = dataSite.getLink() + m.group(3) + "IM.aspx?" + GETparams + "&_OWS_=" + m.group(2);
				detailsResponse = getLinkContents(detailsLink);
			}

			nodes = new HtmlParser3(detailsResponse).getNodeList();
			Node n = HtmlParser3.getNodeByTypeAttributeDescription(nodes, "div", "class", "ui-buttonset",
					new String[] { "General Info", "Land", "Levy" }, true);

			details.append("<tr><td><p id=\"fakeHeader\" align=\"center\" ><b>Document Index Detail <br>OH Delaware County</b></p>"
					+ "<br></td></tr>");

			if (n != null) {
				NodeList labels = nodes
						.extractAllNodesThatMatch(new TagNameFilter("label"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("role", "button"), true);

				for (int i = 0; i < labels.size(); i++) {

					if (labels.elementAt(i).toPlainTextString().contains("Tax") && !labels.elementAt(i).toPlainTextString().contains("Estimator")) {
						LinkTag a = (LinkTag) labels.extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(i);
						details.append("<table class=\"taxes\" width=\"800\" align=\"center\" border=\"2\">" + getTax(a.getLink()) + "</table>");
					}

					if (labels.elementAt(i).toPlainTextString().contains("Transfer History"))
					{
						LinkTag a = (LinkTag) labels.extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(i);
						details.append("<table class=\"tandVHistory\" width=\"800\" align=\"center\" border=\"2\"><tr><td>"
								+ getTransferAndValueHistory(a.getLink())
								+ "</td></tr>");
					}

					if (labels.elementAt(i).toPlainTextString().contains("Value History"))
					{
						LinkTag a = (LinkTag) labels.extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(i);
						details.append("<tr><td>" + getTransferAndValueHistory(a.getLink()) + "</td></tr></table>");
					}

					if (labels.elementAt(i).toPlainTextString().contains("Land")) {
						LinkTag a = (LinkTag) labels.extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(i);
						details.append("<table class=\"land\" width=\"800\" align=\"center\" border=\"2\"><tr><td align=\"center\"><strong>Land</strong></td></tr><tr><td>"
								+ getLand(a.getLink()) + "</td></tr>");
					}

					if (labels.elementAt(i).toPlainTextString().contains("Improvements"))
					{
						LinkTag a = (LinkTag) labels.extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(i);
						String improvements = getImprovements(a.getLink());
						if (!improvements.isEmpty())
						{
							if (!improvements.contains("is currently not available")){
								details.append("<tr><td class=\"improvements\">" + improvements + "</td></tr>");
							}
						}
					}
					if (labels.elementAt(i).toPlainTextString().contains("Distribution"))
					{
						LinkTag a = (LinkTag) labels.extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(i);
						String distribInfo = getTaxDistribution(a.getLink());
						if (StringUtils.isNotBlank(distribInfo)){
							if (!distribInfo.contains("is currently not available")){
								details.append("<tr><td class=\"distribution\">" + distribInfo + "</td></tr>");
							}
						}
					}
					if (labels.elementAt(i).toPlainTextString().contains("Levy"))
					{
						LinkTag a = (LinkTag) labels.extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(i);
						String levyInfo = getLevy(a.getLink());
						if (StringUtils.isNotBlank(levyInfo)){
							if (!levyInfo.contains("is currently not available")){
								details.append("<tr><td class=\"levy\">" + levyInfo + "</td></tr>");
							}
						}
					}
				}
				details.append("</table>");
			}

			details.append("</table>");
			return details
					.toString()
					.replaceAll("(?ism)<a href[^>]*>([^<]*)</a>", "$1")
					.replaceAll("(?is)<div[^>]*ui-button ui-button-text-only ui-widget ui-state-default ui-corner-all[^>]*>.*?</div>", "")
					.replaceAll("(?is)<span[^>]*BannerColor[^>]*>.*?</span>", "")
					.replaceAll("(?is)background-color:[^;]*;?", "");
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	private String makeHeader(int viParseID) {

		String header = "<table id=\"intermediaryResults\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"95%\">";

		if (viParseID == ID_SEARCH_BY_ADDRESS) {
			header += "<TR bgcolor=\"#6699CC\" valign=\"top\">"
					+ "<th>Parcel ID</th>"
					+ "<th>Unit Address</th>"
					+ "<th>Owner(Current)</th>"
					+ "</tr>";
		}

		if (viParseID == ID_SEARCH_BY_NAME) {
			header += "<TR bgcolor=\"#6699CC\" valign=\"top\">"
					+ "<th>Owner(Current)</th>"
					+ "<th>Parcel ID</th>"
					+ "<th>Unit Address</th>"
					+ "</tr>";
		}

		if (viParseID == ID_SEARCH_BY_PARCEL)
		{
			header += "<TR bgcolor=\"#6699CC\" valign=\"top\">"
					+ "<th>Parcel ID</th>"
					+ "<th>Owner(Current)</th>"
					+ "<th>Legal Description</th>"
					+ "<th>Unit Address</th>"
					+ "</tr>";
		}
		return header;
	}

	private String getPrevNext(ServerResponse resp, NodeList nodes) {
		try {
			NodeList pagingDiv = nodes.extractAllNodesThatMatch(new HasAttributeFilter("class", "search-results-bar"), true);
			String paging = "";
			if (pagingDiv.size() == 0)
			{
				return "";
			}
			else
			{
				paging = pagingDiv.elementAt(0).getFirstChild().getNextSibling().toHtml();
				Pattern p = Pattern.compile("(?is)<a\\s*[^\"]*href=\"([^\"]*)\"[^>]*>.*?</a>");
				Matcher m = p.matcher(paging);
				String currentPage = resp.getLastURI().toString();
				currentPage = currentPage.substring(currentPage.lastIndexOf("/") + 1, currentPage.length());
				while (m.find())
				{

					if (m.group(1).length() == 1)
						continue;
					String link = CreatePartialLink(TSConnectionURL.idGET) + dataSite.getLink() + currentPage.replaceAll("\\?.*", "") + m.group(1);
					paging = paging.replace(m.group(1), link);
				}

				m = NEXT_LINK.matcher(paging);

				if (m.find())
				{
					String nextLink = URLEncoder.encode(
							CreatePartialLink(TSConnectionURL.idGET) + dataSite.getLink() + currentPage.replaceAll("\\?.*", "") + m.group(1), "UTF-8");
					resp.getParsedResponse().setNextLink("<a href=" + nextLink + ">Next</a>");
				}

				paging = paging.replaceAll("((?is)class=\\\".*?\\\")|((?is)class='.*?')", "class=\"intermediary\"")
						.replaceAll("(?is)onclick=\"[^>]*\"", "")
						.replaceFirst("(?is)(<a[^>]*>)[^<]*Prev[^<]*<", "$1" + " Prev <")
						.replaceFirst("(?is)(<a[^>]*>)[^<]*Next[^<]*<", "$1" + " Next <")
						.replaceAll("(?is)(<a[^>]*>)\\s*(\\d*)\\s*(<)", "$1" + "  $2  <")
						.replaceFirst("(?is)(<a[^>]*href=\\s*\"\\s*#\\s*\"\\s*[^>]*>)([^<]*)</a>", "$1" + "<strong>" + "$2" + "</strong></a>");
				;
			}
			return paging;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			ParsedResponse parsedResponse = response.getParsedResponse();
			Integer viParseId = (Integer) mSearch.getAdditionalInfo("viParseID");
			String rsResponse = response.getResult();

			NodeList nodes = new HtmlParser3(Tidy.tidyParse(rsResponse, null)).getNodeList();

			NodeList tableList = nodes
					.extractAllNodesThatMatch(new HasAttributeFilter("class", "searchResult ui-widget-content ui-table ui-corner-all"), true);

			if (tableList.size() > 0) {
				for (int i = 0; i < tableList.size(); i++) {
					TableRow row = (TableRow) ((TableTag) tableList.elementAt(i)).getRow(0);
					String link = "";
					if (row.getColumns()[0].getChildCount() > 0 && row.getColumns().length > 2) {

						String ownerNameIntermediaries = "";
						ownerNameIntermediaries = row.getColumns()[0].toPlainTextString();
						if(ownerNameIntermediaries.matches("[\\d-.]+")){//if address intermediaries
							ownerNameIntermediaries = row.getColumns()[2].toPlainTextString();
						}
						String ownerParam = "&ownerNameIntermediary=" + ownerNameIntermediaries;

						LinkTag linkTag = getLinkFromTC(row.getColumns()[0]);
						if (linkTag != null) {
							link = CreatePartialLink(TSConnectionURL.idGET) + linkTag.extractLink().trim() + ownerParam;
							linkTag.setLink(link);
						}
						linkTag = getLinkFromTC(row.getColumns()[1]);
						if (linkTag != null) {
							link = CreatePartialLink(TSConnectionURL.idGET) + linkTag.extractLink().trim() + ownerParam;
							linkTag.setLink(link);
						}
						linkTag = getLinkFromTC(row.getColumns()[2]);
						if (linkTag != null) {
							link = CreatePartialLink(TSConnectionURL.idGET) + linkTag.extractLink().trim() + ownerParam;
							linkTag.setLink(link);
						}
					}
					//search by address brings 4 columns/row, and last one empty; 
					if(row.getColumnCount()==4)
					{
						row.removeChild(row.getChildCount()-2);
					}
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

					ResultMap resultMap = new ResultMap();
					resultMap = ro.cst.tsearch.servers.functions.OHDelawareTR.parseIntermediaryRow(row, viParseId);
					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);

					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);
					intermediaryResponse.add(currentResponse);
				}
			}
			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
				parsedResponse.setHeader(makeHeader(viParseId));
				parsedResponse.setFooter("\n</table><br>" + getPrevNext(response, nodes));
			} else {
				parsedResponse.setHeader("<table border=\"1\">");

				Matcher m = NEXT_LINK.matcher(nodes.toHtml());
				if (m.find())
				{
					String currentPage = response.getLastURI().toString();
					currentPage = currentPage.substring(currentPage.lastIndexOf("/") + 1, currentPage.length());
					String nextLink = CreatePartialLink(TSConnectionURL.idGET) + dataSite.getLink() + currentPage.replaceAll("\\?.*", "")
							+ m.group(1);

					// change parameters order for automatic search to follow all next links:
					Pattern p = Pattern.compile("(?i)(.*/.*\\?)(.*?)&(.*)");
					m = p.matcher(nextLink);

					if (m.find())
					{
						nextLink = m.group(1) + m.group(3) + "&" + m.group(2);
					}

					parsedResponse.setNextLink("<a href=\"" + nextLink + "\">Next</a>");
				}

				parsedResponse.setFooter("</table>");
			}
			outputTable.append(table);
		} catch (Exception e) {
			logger.error("Error while parsing intermediary data", e);
		}
		return intermediaryResponse;
	}

	private LinkTag getLinkFromTC(TableColumn tc) {
		for (int j = 0; j < tc.getChildren().size(); j++) {
			if (tc.getChild(j) instanceof LinkTag)
				return (LinkTag) tc.getChild(j);
		}
		LinkTag l = new LinkTag();
		l.setLink("");
		return l;
	}

	private String getLand(String link) {
		if (StringUtils.isEmpty(link))
		{
			return "";
		}

		try {
			String landPage = getLinkContents(link.replace("|", "%7C"));

			if (StringUtils.isNotEmpty(landPage)) {
				NodeList nodes = new HtmlParser3(landPage).getNodeList();

				nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("class", "ui-widget-content ui-table ui-corner-all"), true);

				if (nodes.size() > 0) {
					TableTag t = (TableTag) nodes.elementAt(0);
					t.setAttribute("id", "land");
					t.setAttribute("align", "center");
					if (t != null) {
						return t.toHtml().replaceAll("(?is)class=\"[^\"]*\"", "");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	private String getLevy(String link) {
		if (StringUtils.isEmpty(link))
		{
			return "";
		}
		try {
			String levyPage = getLinkContents(link.replace("|", "%7C"));
			if (levyPage.matches("(?is).*The\\s+tax\\s+estimator\\s+is\\s+under\\s+revision\\s+to\\s+incorporate\\s+the\\s+changes\\s+to\\s+state.*"))
			{
				levyPage = "";
			}
			return levyPage;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	private String getImprovements(String link) {
		if (StringUtils.isEmpty(link))
		{
			return "";
		}

		try {
			String improvementsPage = getLinkContents(link.replace("|", "%7C"));

			String improvements = improvementsPage
					.replaceAll(
							"(?is).*(<div[^>]*class=\"ui-widget\\s+ui-widget-header\\s+ui-corner-top\"[^>]*>.*?<table[^>]*width=\"720\"[^>]*class=\"ui-widget-content ui-table ui-corner-all\"[^>]*>.*?</table>).*",
							"$1");
			if (!improvements.isEmpty() && !improvementsPage.matches("(?is).*<font>\\s*No\\s+Improvements\\s+Found\\s*</font>.*"))
			{
				return improvements.replaceAll("(?is)class=\"[^\"]*\"", "");
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		return "";
	}

	private String getTransferAndValueHistory(String link) {
		if (StringUtils.isEmpty(link))
		{
			return "";
		}
		try {
			String page = getLinkContents(link.replace("|", "%7C"));

			if (StringUtils.isNotEmpty(page)) {
				NodeList nodes = new HtmlParser3(page).getNodeList();

				nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("class", "ui-corner-all"), true);
				if (nodes.size() > 0) {
					TableTag t = (TableTag) nodes.elementAt(0);
					t.setAttribute("id", "transfer");
					t.setAttribute("align", "center");
					if(t!=null){
						return t.toHtml();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	private String getTax(String link) {
		if (StringUtils.isEmpty(link))
		{
			return "";
		}
		try {
			String paymentsPage = getLinkContents(link.replace("|", "%7C"));

			// Detail of special assessment table
			StringBuilder tax = new StringBuilder();
			TableTag specialAssessmentTTag = null;
			if (!paymentsPage.matches("(?is).*<font[^>]*>\\s*no\\s+special\\s+assessments\\s+found.\\s*</font>.*"))
			{
				NodeList specialAssessment = new HtmlParser3(paymentsPage).getNodeList();
				specialAssessment = specialAssessment.extractAllNodesThatMatch(new HasAttributeFilter("class", "ui-widget ui-table ui-corner-all"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("align", "center"), true)
						.extractAllNodesThatMatch(new TagNameFilter("table"), true);

				if (specialAssessment.size() > 0)
				{
					specialAssessmentTTag = (TableTag) specialAssessment.elementAt(0);
					specialAssessmentTTag.setAttribute("id", "sAssessment");
					specialAssessmentTTag.setAttribute("align", "center");
					specialAssessmentTTag.setAttribute("class", "specialAssessment");
				}
			}

			// tax details table
			TableTag taxDetailsTTag = null;
			Pattern p = java.util.regex.Pattern.compile("(?is)ows.create\\(([^>]*'(s[^']*lxsrc:[^']*)'[^\\)]*taxdetail[^\\)]*','([^']*)'.*?)\\)");
			Matcher m = p.matcher(paymentsPage);
			String detailsLink = "";
			String detailsResponse = "";

			if (m.find())
			{
				String GETparams = link.substring(link.indexOf("Tax.aspx") + 9, link.length() - 1);
				detailsLink = dataSite.getLink() + m.group(3) + "IM.aspx?" + GETparams + "&_OWS_=" + m.group(2);
				detailsResponse = getLinkContents(detailsLink);
			}

			if (StringUtils.isNotEmpty(detailsResponse)) {
				NodeList taxDetails = new HtmlParser3(detailsResponse).getNodeList();
				taxDetails = taxDetails.extractAllNodesThatMatch(new HasAttributeFilter("class", "ui-widget ui-table ui-corner-all"), true)
						.extractAllNodesThatMatch(new TagNameFilter("table"), true);

				if (taxDetails.size() > 0)
				{
					taxDetailsTTag = (TableTag) taxDetails.elementAt(0);
					taxDetailsTTag.setAttribute("id", "taxDetails");
					taxDetailsTTag.setAttribute("align", "center");
				}
			}

			if (taxDetailsTTag != null)
			{
				tax.append("<tr><td>" + taxDetailsTTag.toHtml() + "</td></tr>");
			}

			// if it exists, append specialAssessment table after currentTax year detail table and before payment info, like on original site
			if (specialAssessmentTTag != null)
			{
				tax.append("<tr><td>" + specialAssessmentTTag.toHtml() + "</td></tr>");
			}

			// payments table:
			p = java.util.regex.Pattern.compile("(?is)ows.create\\(([^>]*'(s[^']*lxsrc:[^']*)'[^\\)]*Payments[^\\)]*','([^']*)'.*?)\\)");
			m = p.matcher(paymentsPage);
			detailsLink = "";
			detailsResponse = "";

			if (m.find())
			{
				String GETparams = link.substring(link.indexOf("Tax.aspx") + 9, link.length() - 1);
				detailsLink = dataSite.getLink() + m.group(3) + "IM.aspx?" + GETparams + "&_OWS_=" + m.group(2);
				detailsResponse = getLinkContents(detailsLink);

				if (detailsResponse.matches("(?is).*no\\s+payments\\s+information\\s+found.*"))
				{
					detailsResponse = " ";
				}
			}
			if (StringUtils.isNotEmpty(detailsResponse))
			{
				tax.append("<tr><td>" + detailsResponse
						.replaceAll("(?is).*(<table\\s*align=\"center\"\\s+width=\"720\"\\s*>(.*?(<table[^>]*>.*?</table>).*?)*</table>)", "$1")
						.replaceAll("tablealign", "table align")
						+ "</td></tr>");
			}

			return tax.toString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String getTaxDistribution(String link) {
		if (StringUtils.isEmpty(link))
		{
			return "";
		}
		try {
			String page = getLinkContents(link.replace("|", "%7C"));
			TableTag taxDistribution = null;
			// tax distribution table
			Pattern p = java.util.regex.Pattern.compile("(?is)ows.create\\(([^>]*'(s[^']*lxsrc:[^']*)'[^\\)]*taxdist[^\\)]*','([^']*)'.*?)\\)");
			Matcher m = p.matcher(page);
			String detailsLink = "";
			String detailsResponse = "";

			if (m.find())
			{
				String GETparams = link.substring(link.indexOf("TaxDistribution.aspx") + 21, link.length() - 1);
				detailsLink = dataSite.getLink() + m.group(3) + "IM.aspx?" + GETparams + "&_OWS_=" + m.group(2);
				detailsResponse = getLinkContents(detailsLink);
			}

			if (StringUtils.isNotEmpty(detailsResponse)) {
				NodeList nodes = new HtmlParser3(detailsResponse).getNodeList();
				nodes = nodes
						.extractAllNodesThatMatch(new HasAttributeFilter("class", "ui-widget ui-widget-table ui-corner-all"), true)
						.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				if (nodes.size() > 0) {
					taxDistribution = (TableTag) nodes.elementAt(0);
					taxDistribution.setAttribute("id", "TaxDistribution");
					taxDistribution.setAttribute("align", "center");
				}
			}
			if (taxDistribution != null) {
				return taxDistribution.toHtml();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		// get year from PD
		try {
			if (getDataSite().getPayDate() != null) {
				Calendar c = Calendar.getInstance();
				c.setTime(getDataSite().getPayDate());
				String year = Integer.toString(c.get(Calendar.YEAR));
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		ro.cst.tsearch.servers.functions.OHDelawareTR.parseAndFillResultMap(response, detailsHtml, resultMap);
		return null;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		// pin
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();

			String parcelno = getSearchAttribute(SearchAttributes.LD_PARCELNO_GENERIC_TR);
			module.forceValue(0, parcelno);
			moduleList.add(module);
		}

		// address
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);

		if (hasStreet()) {
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();

			if (hasStreetNo()) {
				module.setSaKey(0, SearchAttributes.P_STREETNO);
			}
			module.setSaKey(1, SearchAttributes.P_STREETNAME);
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			moduleList.add(module);
		}

		// owner
		if (hasOwner()) {
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();

			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" }));
			moduleList.add(module);
		}
		serverInfo.setModulesForAutoSearch(moduleList);
	}

	@Override
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse) {
		DocumentI document = null;
		try {
			ResultMap map = new ResultMap();
			parseAndFillResultMap(response, detailsHtml, map);
			map.removeTempDef();// this is for removing tmp items. we remove them here to not remove them in every place when we parse something.
			Bridge bridge = new Bridge(response.getParsedResponse(), map, searchId);
			try {
				String prevSrcType = (String) map.get(OtherInformationSetKey.SRC_TYPE.getKeyName());
				if (StringUtils.isEmpty(prevSrcType)) {
					map.getMap().put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			document = bridge.importData();

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (fillServerResponse) {
			response.getParsedResponse().setResponse(detailsHtml);
			if (document != null) {
				ParsedResponse newParsedResponse = new ParsedResponse();
				newParsedResponse.setDocument(document);

				getDocumentSearchType(document, true);

				newParsedResponse.setUseDocumentForSearchLogRow(true);
				response.getParsedResponse().addOneResultRowOnly(newParsedResponse);
				response.getParsedResponse().setDocument(document);
			}
		}
		response.getParsedResponse().setSearchId(this.searchId);
		response.getParsedResponse().setUseDocumentForSearchLogRow(true);

		NodeList nodes = new HtmlParser3(Tidy.tidyParse(detailsHtml.replaceAll("(?is)</?span[^>]*>", ""), null)).getNodeList();
		{
			StringBuffer remarks = new StringBuffer();
			boolean hasBOR = false, hasCAUV = false;

			// CAUV and Board of Revision

			NodeList tableCAUV = HtmlParser3.getNodeByTypeAttributeDescription(nodes, "table", "class", "ui-corner-all",
					new String[] { "Assessment Info", "Current Value", "Recent Transfer" },
					true).getChildren();
			String cauvStatus = StringUtils.strip(HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tableCAUV, "CAUV"), "", true)
					.replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1")).replaceAll("[\\s$,-]", "");
			String borStatus = StringUtils.strip(
					HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tableCAUV, "Board of Revision"), "", true)
							.replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1")).replaceAll("[\\s]", "");

			if (StringUtils.isNotEmpty(cauvStatus) && cauvStatus.matches("(?is)[\\d,]+")) {
				if (!"0".equals(cauvStatus)) {
					hasCAUV = true;
				}
				remarks.append("CAUV: ").append(cauvStatus);
			}

			if (StringUtils.isNotEmpty(borStatus) && borStatus.matches("(?is)[A-Z]+")) {
				if (remarks.length() > 0) {
					remarks.append("\n");
				}
				remarks.append("BOR: ").append(borStatus);
				if (borStatus.toLowerCase().contains("y")) {
					hasBOR = true;
				}
			}
			if (remarks.length() > 0) {
				document.setNote(remarks.toString());

				if (hasBOR || hasCAUV) {
					document.setTsrIndexColorClass("gwt-tax-bor-cauv-status");
				}
			}
		}
		return document;
	}
}
