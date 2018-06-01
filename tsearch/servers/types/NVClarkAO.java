package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
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
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

/**
 * 
 * @author Oprina George
 * 
 *         May 16, 2011
 */

public class NVClarkAO extends TemplatedServer {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 6815395686184869702L;

	private int					header				= -1;

	public NVClarkAO(long searchId) {
		super(searchId);
		setSuper();
	}

	public NVClarkAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
		setSuper();
	}

	public void setSuper() {
		int[] intermediary_cases = new int[] { ID_SEARCH_BY_NAME,
				ID_SEARCH_BY_ADDRESS };
		super.setIntermediaryCases(intermediary_cases);
		int[] details_cases = new int[] { ID_SEARCH_BY_PARCEL, ID_DETAILS };
		super.setDetailsCases(details_cases);
		String[] details_message = { "Real Property Parcel Record" };
		super.setDetailsMessages(details_message);
	}

	@Override
	protected void setMessages() {
		getErrorMessages().addNoResultsMessages("No Records Returned");
		getErrorMessages().addNoResultsMessages("No record found for your selection.");
	}

	@Override
	protected void ParseResponse(String action, ServerResponse response,
			int viParseID) throws ServerResponseException {
		if (isError(response)) {
			response.setError("No data found for this criteria!");
			response.setResult("");
			return;
		}
		int local_parse_ID = viParseID;
		if (action
				.equals("http://redrock.clarkcountynv.gov/AssrRealProp/ownerList.aspx?inst=pcl7")) {
			local_parse_ID = ID_SEARCH_BY_NAME;

		} else if (action
				.equals("http://redrock.clarkcountynv.gov/AssrRealProp/siteList.aspx?inst=pcl7")) {
			local_parse_ID = ID_SEARCH_BY_ADDRESS;
		}

		this.header = local_parse_ID;

		if (response.getResult().contains("Parcel number inquiry")
				|| viParseID == ID_SAVE_TO_TSD)
			super.ParseResponse(action, response, local_parse_ID);
		else if (response.getResult().contains("Real Property Parcel Record"))
			super.ParseResponse(action, response, ID_DETAILS);
	}

	@Override
	protected HashMap<String, String> putAdditionalData(String serverResult) {
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type", "ASSESSOR");
		data.put("dataSource", "AO");
		return data;
	}

	@Override
	protected String getAccountNumber(String serverResult) {
		try {
			NodeList nodeList = org.htmlparser.Parser.createParser(
					serverResult, null).parse(null);

			nodeList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("span"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "lblParcel"));
			if (nodeList != null && nodeList.size() > 0) {
				Span parcel = (Span) nodeList.elementAt(0);

				String account = parcel.toPlainTextString();
				return account;
			} else
				return "";
		} catch (ParserException e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	protected String clean(String response) {
		return response;
	}

	@Override
	protected String cleanDetails(String response) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(response, null);
			NodeList nodeList = htmlParser.parse(null);

			// if from memory
			if (!response.contains("<html"))
				return response;

			// extract second page
			NodeList href = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("a"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "linkOwnerHistory"), true);

			if (href.size() == 0)
				return "";

			String link = "http://sandgate.co.clark.nv.us/AssrRealProp/"
					+ ((LinkTag) href.elementAt(0)).getLink();

			String second_page = getLinkContents(link);

			// get the 3 tables

			TableTag assesor_description = null;
			TableTag current = null;
			TableTag history = null;

			Div current_div = null;
			Div history_div = null;

			org.htmlparser.Parser second_htmlParser = org.htmlparser.Parser
					.createParser(second_page, null);
			NodeList second_nodeList = second_htmlParser.parse(null);

			NodeList divs = second_nodeList
					.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("id",
									"ctl00_ContentPlaceHolder1_pnlCurrentParcel"),
							true);

			if (divs.size() > 0) {
				current_div = (Div) divs.elementAt(0);
				if (current_div.getChildCount() == 5) {
					assesor_description = (TableTag) current_div.getChild(1);
					current = (TableTag) current_div.getChild(3);
				}
			}

			divs = second_nodeList.extractAllNodesThatMatch(
					new TagNameFilter("div"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id",
							"ctl00_ContentPlaceHolder1_pnlHistory"), true);

			if (divs.size() > 0) {
				history_div = (Div) divs.elementAt(0);
				if (history_div.getChildCount() > 0) {
					history = (TableTag) history_div.getChild(1);
				}
			}

			// get divs from base document
			Div printReady = null;
			Div printReady2 = null;

			divs = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"),
					true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "printReady"), true);

			if (divs.size() > 0) {
				printReady = (Div) divs.elementAt(0);
			}

			divs = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"),
					true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "printReady2"), true);

			if (divs.size() > 0) {
				printReady2 = (Div) divs.elementAt(0);
			}

			response = "<p align=\"center\"><font style=\"font-size:xx-large;\"><b>Real Property Parcel Record</b></font></p>"
					+ "<br>";
			if (printReady != null)
				response += printReady.toHtml() + "<br>\n";

			if (printReady2 != null)
				response += printReady2.toHtml() + "<br>\n";

			if (assesor_description != null)
				response += assesor_description
						.toHtml()
						.replace(
								"style=\"width: 100%; border-collapse:collapse",
								"id=\"assesor_description\" cellspacing=\"0\" cellpadding=\"2\" bordercolor=\"#cc9966\" border=\"1\" align=\"center\" style=\"width: 650;  border-collapse:collapse;")
						+ "<br>\n";

			if (current != null)
				response += current
						.toHtml()
						.replace(
								"style=\"width: 100%; border-collapse:collapse",
								"id=\"current\" cellspacing=\"0\" cellpadding=\"2\" bordercolor=\"#cc9966\" border=\"1\" align=\"center\" style=\"width: 650;  border-collapse:collapse;")
						+ "<br>\n";

			if (history != null)
				response += history
						.toHtml()
						.replace(
								"style=\"width: 100%; border-collapse:collapse",
								"id=\"history\" cellspacing=\"0\" cellpadding=\"2\" bordercolor=\"#cc9966\" border=\"1\" align=\"center\" style=\"width: 650;  border-collapse:collapse;")
						+ "<br>\n";

			response = response.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>",
					"$1");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return response;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
		try {

			ro.cst.tsearch.servers.functions.NVClarkAO.putSearchType(resultMap,
					"AO");

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml, null);

			NodeList nodeList = htmlParser.parse(null);

			String parcel_no = HtmlParser3
					.getValueFromAbsoluteCell(1, 0,
							HtmlParser3.findNode(nodeList, "Parcel No."), "",
							true).replaceAll("<[^>]*>", "")
					.replaceAll("[^\\d,^-]", "");

			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
					parcel_no);
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(),
					parcel_no.replace("-", ""));

			String owner = HtmlParser3
					.getValueFromAbsoluteCell(1, 0,
							HtmlParser3.findNode(nodeList, "Current Owner"),
							"", true).replaceAll("(?i)\\s*<br>\\s*", " && ")
					.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ");

			ro.cst.tsearch.servers.functions.NVClarkAO.parseNames(resultMap,
					owner);

			NodeList addr = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("span"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "lblLocation"), true);

			if (addr.size() > 0) {
				String address = addr.elementAt(0).toPlainTextString();
				ro.cst.tsearch.servers.functions.NVClarkAO.parseAddress(
						resultMap, address);
			}

			NodeList twn = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("span"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "lblTown"), true);

			// Bug 8137 - do not parse the city
//			if (twn.size() > 0) {
//				String town = twn.elementAt(0).toPlainTextString();
//				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(),
//						town);
//			}

			NodeList printReady = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("div"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "printReady"), true);
			if (printReady.size() > 0) {
				NodeList printR = printReady.elementAt(0).getChildren()
						.extractAllNodesThatMatch(new TagNameFilter("table"));

				TableTag general_t = (TableTag) printR.elementAt(0);

				String description = general_t.getRow(4).getColumns()[1]
						.toHtml();

				ro.cst.tsearch.servers.functions.NVClarkAO
						.parseLegalDescription(resultMap, description);
			}

			NodeList current = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "current"), true);
			NodeList history = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "history"), true);

			if (current.size() > 0 || history.size() > 0) {
				ro.cst.tsearch.servers.functions.NVClarkAO.parseSaleDataSet(
						resultMap, current, history);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {

			Form form = new SimpleHtmlParser(table).getForm("Form1");
			int seq = -1;
			if (form != null) {
				Map<String, String> params = form.getParams();
				seq = getSeq();
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:"
						+ seq, params);
			}

			NodeList nodeList = new HtmlParser3(table).getNodeList();
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "DataGrid1"), true);

			if (mainTableList.size() == 0) {
				return intermediaryResponse;
			}

			// parse rows
			TableTag tableTag = (TableTag) mainTableList.elementAt(0);

			TableRow[] rows = tableTag.getRows();

			String navigationPanel = "";

			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if (row.getColumnCount() == 3 || row.getColumnCount() == 4) {
					LinkTag linkTag = ((LinkTag) row.getColumns()[row
							.getColumnCount() - 1].getChild(1));

					String link = CreatePartialLink(TSConnectionURL.idGET)
							+ linkTag.extractLink().trim()
									.replaceAll("\\s", "%20");

					linkTag.setLink(link);

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(
							ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(link, link,
							TSServer.REQUEST_SAVE_TO_TSD));

					ResultMap m = ro.cst.tsearch.servers.functions.NVClarkAO
							.parseIntermediaryRow(row, searchId);
					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (AssessorDocumentI) bridge
							.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);
				}
				if (row.getColumnCount() == 1 && i > 1) {
					// other pages
					row.removeAttribute("style");

					TableColumn col = row.getColumns()[0];

					String link = CreatePartialLink(TSConnectionURL.idPOST);
					String server_link = "";

					if (header == ID_SEARCH_BY_NAME)
						server_link = "http://redrock.clarkcountynv.gov/AssrRealProp/ownerList.aspx?inst=pcl7";
					else if (header == ID_SEARCH_BY_ADDRESS)
						server_link = "http://redrock.clarkcountynv.gov/AssrRealProp/siteList.aspx?inst=pcl7";

					for (int j = 0; j < col.getChildCount(); j++) {
						if (col.getChild(j).toHtml().contains("a href")
								&& seq != -1) {
							LinkTag l = (LinkTag) col.getChild(j);

							String new_link = link
									+ server_link
									+ "&page="
									+ l.getLink().replaceAll(
											".*\\('([^']*)'.*", "$1") + "&seq="
									+ seq;

							l.setLink(new_link);
						}
					}

					navigationPanel += "<table align=\"center\">"
							+ row.toHtml() + "</table>";

				}
			}

			if (header == ID_SEARCH_BY_NAME) {
				response.getParsedResponse()
						.setHeader(
							//	navigationPanel
										"<br />"
										+ "<table id=\"DataGrid1\" align=\"center\" cellspacing=\"0\" cellpadding=\"1\" bordercolor=\"#CC9966\" border=\"2\" style=\"border-color:#CC9966;border-width:2px;border-style:solid;width:613px;border-collapse:collapse;\">"
										+ "<tr>"
										+ "<td style=\"font-weight:bold;width:220px;\">OWNER NAME</td>"
										+ "<td style=\"font-weight:bold;width:220px;\">OWNER NAME 2</td>"
										+ "<td style=\"font-weight:bold;width:95px;\">TAX DISTRICT</td>"
										+ "<td style=\"font-weight:bold;width:115px;\">PARCEL NUMBER</td>"
										+ "</tr>");
			}

			if (header == ID_SEARCH_BY_ADDRESS) {
				response.getParsedResponse()
						.setHeader(
							//	navigationPanel
										"<br />"
										+ "<table id=\"DataGrid1\" align=\"center\" cellspacing=\"0\" cellpadding=\"1\" bordercolor=\"#CC9966\" border=\"2\" style=\"border-color:#CC9966;border-width:2px;border-style:solid;width:613px;border-collapse:collapse;\">"
										+ "<tr>"
										+ "<td style=\"font-weight:bold;\">LOCATION ADDRESS</td>"
										+ "<td style=\"font-weight:bold;\">CITY</td>"
										+ "<td style=\"font-weight:bold;\">PARCEL NUMBER</td>"
										+ "</tr>");
			}
			response.getParsedResponse().setFooter(
					"</table><br />" + navigationPanel);
			outputTable.append(table);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return intermediaryResponse;
	}

	@Override
	protected ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent, boolean forceOverritten) {
		ADD_DOCUMENT_RESULT_TYPES result = super.addDocumentInATS(response, htmlContent, forceOverritten);

		/*
		 * NvClarkAOMConn imageConection = new NvClarkAOMConn();
		 * 
		 * DocumentI doc = response.getParsedResponse().getDocument();
		 * if(doc!=null){ for(PropertyI prop:doc.getProperties()){
		 * if(prop.hasSubdividedLegal()){ SubdivisionI subdivision =
		 * prop.getLegal().getSubdivision(); String book =
		 * subdivision.getPlatBook(); String page = subdivision.getPlatPage();
		 * 
		 * imageConection.downloadMap(book, page, NvClarkAOMConn.MapType.PL);;
		 * 
		 * } } }
		 */
		return result;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		// address
		FilterResponse addressFilter = AddressFilterFactory
				.getAddressHybridFilter(searchId, 0.8d, true);

		if (hasPin()) {
			String parcelno = getSearchAttribute(SearchAttributes.LD_PARCELNO_GENERIC_AO);

			if (parcelno.length() == 14) {
				module = new TSServerInfoModule(
						serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.forceValue(0, parcelno);
				moduleList.add(module);
			}
		}

		if (hasStreet()) {
			FilterResponse nameFilterHybrid = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,
							searchId, module);

			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();

			if (hasStreetNo()) {
				module.forceValue(1,
						getSearchAttribute(SearchAttributes.P_STREETNO));
			}

			module.forceValue(3,
					getSearchAttribute(SearchAttributes.P_STREETNAME));

			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybrid);
			moduleList.add(module);

		}

		// owner
		if (hasOwner()) {

			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,
							searchId, module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);

			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();

			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0,
					FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId,
							new String[] { "L;F;", "L;F;M" }));
			moduleList.add(module);

		}
		serverInfo.setModulesForAutoSearch(moduleList);
	}
}
