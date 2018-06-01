package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupMultipleSelectBox;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
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
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.InstanceManager;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

/**
 * @author Oprina George
 * 
 *         Mar 4, 2011
 */
@SuppressWarnings("deprecation")
public class ARCarrollAO extends TSServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// for subdivision
	private static String SUB_SELECT = "";

	static {
		String folderPath = ServerConfig
				.getModuleDescriptionFolder(BaseServlet.REAL_PATH
						+ "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath
					+ "] does not exist. Module Information not loaded!");
		}
		try {
			String selects = FileUtils.readFileToString(new File(folderPath
					+ File.separator + "ARCarrollAOSubdivision.xml"));
			SUB_SELECT = selects;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getSUB_SELECT() {
		return SUB_SELECT;
	}

	public ARCarrollAO(long searchId) {
		super(searchId);
	}

	public ARCarrollAO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
	}

	private static final String FORM_NAME = "aspnetForm";

	private static int seq = 0;

	protected synchronized static int getSeq() {
		return seq++;
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault
				.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX);

		if (tsServerInfoModule != null) {
			setupMultipleSelectBox(tsServerInfoModule.getFunction(0),
					SUB_SELECT);
		}

		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_SUBDIVISION_NAME:
		case ID_SEARCH_BY_PROP_NO:
		case ID_SEARCH_BY_SECTION_LAND:
			if (rsResponse.indexOf("No results were found.") > -1
					|| (rsResponse.contains("runtime") && rsResponse
							.contains("error"))) {
				Response.getParsedResponse()
						.setError(
								"No results found for your query! Please change your search criteria and try again.");
				return;
			}

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(
					Response, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(
						smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,
						outputTable.toString());
			}
			break;
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			String details = getDetails(rsResponse, accountId);

			String accountNum = accountId.toString();

			String filename = accountNum + ".html";

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&"
						+ Response.getRawQuerry();
				// originalLink =originalLink.replace("%3D","=");
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST)
						+ originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				if (data != null) {
					data.put("type", "ASSESSOR");
					data.put("dataSource", "AO");
				}

				if (isInstrumentSaved(accountNum, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink,
							ID_DETAILS);
				}
				Response.getParsedResponse().setPageLink(
						new LinkInPage(sSave2TSDLink, originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;
		case ID_GET_LINK:
			ParseResponse(
					sAction,
					Response,
					rsResponse
							.contains("Please choose a property by clicking on the property's ID.") ? ID_SEARCH_BY_NAME
							: ID_DETAILS);
			break;
		default:
			break;
		}

	}

	protected String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			StringBuilder details = new StringBuilder();
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(rsResponse, null);

			NodeList nodeList = htmlParser.parse(null);

			// ctl00_MainContent_ParcelLabel
			// get account ID
			String id = HtmlParser3
					.getValueFromAbsoluteCell(0, 1,
							HtmlParser3.findNode(nodeList, "Parcel:"), "", true)
					.replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1").trim();
			accountId.append(id);

			/* If from memory - use it as is */
			if (!rsResponse.contains("<html")) {
				NodeList headerList = nodeList.extractAllNodesThatMatch(
						new TagNameFilter("table"), true);

				if (headerList.size() == 0) {
					return null;
				}
				return rsResponse;
			}

			// tables
			NodeList tableList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true);

			if (!(tableList.size() > 0)) {
				return null;
			}

			ro.cst.tsearch.servers.functions.ARCarrollAO.getNodes(nodeList,
					details);

			return details.toString().replaceAll(
					"(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id",
							"ctl00_MainContent_ctl01_gvSearchResults"), true);

			if (mainTableList.size() != 1) {
				return intermediaryResponse;
			}

			// parse and store params for search
			Form form = new SimpleHtmlParser(table).getForm(FORM_NAME);
			Map<String, String> params = form.getParams();
			params.remove("__EVENTTARGET");
			params.remove("__EVENTARGUMENT");
			int seq = getSeq();
			mSearch.setAdditionalInfo(
					getCurrentServerName() + ":params:" + seq, params);

			TableTag tableTag = (TableTag) mainTableList.elementAt(0);

			TableRow[] rows = tableTag.getRows();

			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				// remove first 2 cols from every row
				if (row.getColumnCount() == 10) {
					if (row.childAt(1) instanceof TableColumn) {
						row.removeChild(1);
						row.removeChild(1);
					}
				}

				if (row.getColumnCount() == 8) {

					LinkTag linkTag = (LinkTag) (((Span) row.getColumns()[0]
							.getChild(1)).getChild(0));

					String link = CreatePartialLink(TSConnectionURL.idGET)
							+ "http://actdatascout.com/"
							+ linkTag.extractLink().trim()
									.replaceAll("\\s", "%20");// + "&seq=" +
																// seq;

					linkTag.setLink(link);

					row.removeAttribute("style");
					row.setAttribute(
							"style",
							"background-color:#EFEFEF;border-color:Black;border-width:1px;border-style:solid;");
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(
							ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(row.toHtml());
					currentResponse.setPageLink(new LinkInPage(link, link,
							TSServer.REQUEST_SAVE_TO_TSD));

					ResultMap m = ro.cst.tsearch.servers.functions.ARCarrollAO
							.parseIntermediaryRow(row, searchId);
					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (AssessorDocumentI) bridge
							.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);
				}
			}

			response.getParsedResponse()
					.setHeader(
							"<table align=\"center\" cellspacing=\"0\" cellpadding=\"3\" rules=\"cols\" border=\"1\" id=\"ctl00_MainContent_ctl01_gv\" style=\"background-color:#EFEFEF;border-color:Black;font-size:X-Small;width:85%;\">\n"
									+ "<tr style=\"color:#636466;background-color:#DBDBDB;font-weight:bold;\">"
									+ "<th scope=\"col\">ID</th><th scope=\"col\">Parcel</th>"
									+ "<th scope=\"col\">Owner Name</th><th scope=\"col\">Address</th>"
									+ "<th scope=\"col\">S-T-R</th><th scope=\"col\">Subdivision</th>"
									+ "<th scope=\"col\">Legal</th><th scope=\"col\">Acres</th>"
									+ "</tr>");
			response.getParsedResponse().setFooter("</table>");
			outputTable.append(table);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return intermediaryResponse;
	}

	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {

		try {

			ro.cst.tsearch.servers.functions.ARCarrollAO.putSearchType(
					resultMap, "AO");

			// Span countyS
			// TableTag infoT
			// Span legalS
			// Span transferS
			// TableTag transferT
			// Span landS
			// TableTag landT
			// Span buildingS
			// TableTag buildingT1
			// TableTag buildingT2
			// TableTag buildingT3
			ArrayList<Node> nodes = new ArrayList<Node>();

			StringBuilder details = new StringBuilder();

			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(detailsHtml, null);

			NodeList nodeList = htmlParser.parse(null);

			nodes = ro.cst.tsearch.servers.functions.ARCarrollAO.getNodes(
					nodeList, details);

			if (nodes.size() != 11)
				throw new Exception("Error getting result nodes!");

			// set parcels
			NodeList list = new NodeList();

			list = new NodeList(nodes.get(1));

			if (list != null) {
				String parcelID = HtmlParser3
						.getValueFromAbsoluteCell(0, 1,
								HtmlParser3.findNode(list, "Property ID:"), "",
								true).replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1")
						.trim();
				String parcel = HtmlParser3
						.getValueFromAbsoluteCell(0, 1,
								HtmlParser3.findNode(list, "Parcel:"), "", true)
						.replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1").trim();
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID_PARCEL
						.getKeyName(), parcelID.trim());
				resultMap.put(
						PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
						parcel.trim());
				resultMap.put(
						PropertyIdentificationSetKey.PARCEL_ID3.getKeyName(),
						parcel.replace("-", "").trim());

				// address

				String address = HtmlParser3
						.getValueFromAbsoluteCell(0, 1,
								HtmlParser3.findNode(list, "Location:"), "",
								true).replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1")
						.trim();

				ro.cst.tsearch.servers.functions.ARCarrollAO.parseAddress(
						resultMap, address);

				// owner
				String name = "";

				NodeList l = list.extractAllNodesThatMatch(
						new TagNameFilter("span"), true)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("id",
										"ctl00_MainContent_OwnerLabel"), true);

				if (l != null) {
					Span nameS = (Span) l.elementAt(0);
					name = nameS.toHtml().replaceAll("<[^>]*>", " && ")
							.replaceAll("\\s+", " ").trim();
					// name = name.substring(1, name.indexOf("|", 1)).trim();
				}

				ro.cst.tsearch.servers.functions.ARCarrollAO.parseNames(
						resultMap, name);

				// get S T R
				String str = HtmlParser3
						.getValueFromAbsoluteCell(0, 1,
								HtmlParser3.findNode(list, "S-T-R:"), "", true)
						.replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1").trim();

				String s_t_r[] = str.replaceAll("[^\\d,-]", "").trim()
						.split("-");
				if (s_t_r.length == 3) {
					resultMap.put(
							PropertyIdentificationSetKey.SUBDIVISION_SECTION
									.getKeyName(),
							ro.cst.tsearch.utils.StringUtils
									.removeLeadingZeroes(s_t_r[0]));
					resultMap.put(
							PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP
									.getKeyName(),
							ro.cst.tsearch.utils.StringUtils
									.removeLeadingZeroes(s_t_r[1]));
					resultMap.put(
							PropertyIdentificationSetKey.SUBDIVISION_RANGE
									.getKeyName(),
							ro.cst.tsearch.utils.StringUtils
									.removeLeadingZeroes(s_t_r[2]));
				}

				// get subdivision
				String sub = HtmlParser3
						.getValueFromAbsoluteCell(0, 1,
								HtmlParser3.findNode(list, "Subdivison:"), "",
								true).replaceAll("<[^>]*>([^<]*)<[^>]*>", "$1")
						.trim();
				String subdivision = ro.cst.tsearch.servers.functions.ARCarrollAO
						.getSubdivision(sub);

				if (!subdivision.equals(""))
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME
							.getKeyName(), subdivision);

				// parse legal
				Span legalS = (Span) nodes.get(2);
				String legal = "";
				legal = sub.replace(subdivision, "").replace(str, "")
						.replaceAll("\\s+", " ")
						+ " && ";
				if (legalS != null) {
					legal += legalS.toPlainTextString()
							.replaceAll("<[^>]*>", "").replace("Legal:", "")
							.replaceAll("\\s+", " ").trim();
				}

				ro.cst.tsearch.servers.functions.ARCarrollAO
						.parseLegalDescription(resultMap, legal);

			}

			// get sale data set
			list = new NodeList(nodes.get(4));
			if (list != null) {
				ResultTable sale_doc = new ResultTable();
				String[] sale_doc_header = { "InstrumentDate", "Book", "Page",
						"DocumentType", "SalesPrice", "Grantee" };
				Map<String, String[]> doc_map = new HashMap<String, String[]>();
				doc_map.put("InstrumentDate", new String[] { "InstrumentDate",
						"" });
				doc_map.put("Book", new String[] { "Book", "" });
				doc_map.put("Page", new String[] { "Page", "" });
				doc_map.put("DocumentType", new String[] { "DocumentType", "" });
				doc_map.put("SalesPrice", new String[] { "SalesPrice", "" });
				doc_map.put("Grantee", new String[] { "Grantee", "" });

				List<List<String>> doc_body = new ArrayList<List<String>>();

				TableTag saleDataSet = (TableTag) list.elementAt(0);
				TableRow[] saleRows = saleDataSet.getRows();

				if (saleRows.length > 0)
					saleRows[0] = new TableRow();

				for (TableRow r : saleRows) {
					if (r.getColumnCount() == 7) {
						String pages_interval = " "
								+ r.getColumns()[2].toPlainTextString()
										.replace("&nbsp;", " ")
										.replaceAll("\\s+", " ") + " ";
						if (pages_interval.contains("-")
								|| pages_interval.contains("/")) {

							// interval case 001-04591-003 545-46=545-546
							// bug 6465
							if (pages_interval.matches(" \\d\\d\\d[-/]\\d\\d "))
								pages_interval = pages_interval
										.replaceAll(
												"(\\d)(\\d\\d[-/])(\\d\\d)",
												"$1$2$1$3");

							pages_interval = StringUtils.strip(pages_interval);

							if (pages_interval.contains("-"))
								pages_interval = StringFormats
										.ReplaceIntervalWithEnumeration(pages_interval);

							if (pages_interval.contains("/"))
								pages_interval = pages_interval.replace("/",
										" ");

							String[] pages = pages_interval.split(" ");
							for (String page : pages) {
								List<String> doc_row = new ArrayList<String>();
								doc_row.add(r.getColumns()[0]
										.toPlainTextString()
										.replace("&nbsp;", "")
										.replaceAll("\\s+", " ").trim());
								doc_row.add(r.getColumns()[1]
										.toPlainTextString()
										.replace("&nbsp;", "")
										.replaceAll("\\s+", " ").trim());
								doc_row.add(page);
								doc_row.add(r.getColumns()[3]
										.toPlainTextString()
										.replace("&nbsp;", "")
										.replaceAll("\\s+", " ").trim());
								doc_row.add(r.getColumns()[5]
										.toPlainTextString()
										.replace("&nbsp;", "")
										.replaceAll("\\s+", " ").trim());
								doc_row.add(r.getColumns()[6]
										.toPlainTextString()
										.replace("&nbsp;", "")
										.replaceAll("\\s+", " ").trim());
								doc_body.add(doc_row);
							}

						} else {

							List<String> doc_row = new ArrayList<String>();
							doc_row.add(r.getColumns()[0].toPlainTextString()
									.replace("&nbsp;", "")
									.replaceAll("\\s+", " ").trim());
							doc_row.add(r.getColumns()[1].toPlainTextString()
									.replace("&nbsp;", "")
									.replaceAll("\\s+", " ").trim());
							doc_row.add(r.getColumns()[2].toPlainTextString()
									.replace("&nbsp;", "")
									.replaceAll("\\s+", " ").trim());
							doc_row.add(r.getColumns()[3].toPlainTextString()
									.replace("&nbsp;", "")
									.replaceAll("\\s+", " ").trim());
							doc_row.add(r.getColumns()[5].toPlainTextString()
									.replace("&nbsp;", "")
									.replaceAll("\\s+", " ").trim());
							doc_row.add(r.getColumns()[6].toPlainTextString()
									.replace("&nbsp;", "")
									.replaceAll("\\s+", " ").trim());
							doc_body.add(doc_row);
						}
					}
				}
				// remove duplicates from doc_body
				List<List<String>> doc_body_set = new ArrayList<List<String>>();

				for (List<String> ldb : doc_body) {
					boolean flag = false;
					for (List<String> ldbs : doc_body_set) {
						// if book & page are the same keep only the first doc
						// found
						if (ldbs.get(1).equals(ldb.get(1))
								&& ldbs.get(2).equals(ldb.get(2))) {
							flag = true;
							break;
						}
					}
					if (!flag)
						doc_body_set.add(ldb);
				}

				sale_doc.setHead(sale_doc_header);
				sale_doc.setMap(doc_map);
				sale_doc.setBody(doc_body_set);
				sale_doc.setReadOnly();
				resultMap.put("SaleDataSet", sale_doc);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		// pin
		if (hasPin()) {
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();

			String parcelno = getSearchAttribute(SearchAttributes.LD_PARCELNO);

			if (parcelno.length() == 13 && parcelno.contains("-")) {
				module.forceValue(0, parcelno);
				moduleList.add(module);
			}
		}

		// address
		FilterResponse addressFilter = AddressFilterFactory
				.getAddressHybridFilter(searchId, 0.8d, true);

		if (hasStreet()) {
			FilterResponse nameFilterHybrid = NameFilterFactory
					.getHybridNameFilter(SearchAttributes.OWNER_OBJECT,
							searchId, module);

			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();

			if (hasStreetNo()) {
				module.setSaKey(0, SearchAttributes.P_STREETNO);
			}

			module.setSaKey(2, SearchAttributes.P_STREETNAME);
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
					FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId,
							new String[] { "L;F;" }));
			moduleList.add(module);
		}
		serverInfo.setModulesForAutoSearch(moduleList);
	}

}
