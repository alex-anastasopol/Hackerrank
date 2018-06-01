package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.OffenderInformationDocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;

/**
 * 
 * @author Oprina George
 * 
 *         Apr 28, 2011
 */

public class TXOffenderInformationOI extends TSServer {

	private static final long serialVersionUID = 2160353244685289938L;

	public TXOffenderInformationOI(String rsRequestSolverName,
			String rsSitePath, String rsServerID, String rsPrmNameLink,
			long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);

		resultType = MULTIPLE_RESULT_TYPE;
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();

		int local_viParseID = viParseID;

		// no results
		if (rsResponse
				.indexOf("No Offenders were found that meet the search criteria.") > -1) {
			Response.getParsedResponse()
					.setError(
							"No results found for your query! Please change your search criteria and try again.");
			return;
		}

		// session expired
		if (rsResponse.indexOf("Your session has closed.") > -1) {
			Response.getParsedResponse().setError(
					"Your session has closed. Please start a new search!.");
			return;
		}

		if (rsResponse.contains("Offender Information Detail"))
			local_viParseID = ID_DETAILS;

		ParsedResponse parsedResponse = Response.getParsedResponse();

		switch (local_viParseID) {
		case ID_SEARCH_BY_NAME:

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(
					Response, rsResponse, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}

			parsedResponse.setHeader("<table border=\"1\">");
			parsedResponse.setFooter("</table>");

			parsedResponse.setResultRows(new Vector<ParsedResponse>(
					smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());

			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
				String header = CreateSaveToTSDFormHeader(
						URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
				String footer = "";

				header += "<TABLE cellspacing=\"1\" cellpadding=\"1\" border=\"1\" width=\"100%\">"
						+ "<TR bgcolor=\"#6699CC\" valign=\"top\">"
						+ "<TH ALIGN=Left>" + SELECT_ALL_CHECKBOXES + "</TH>"
						+ "<TH ALIGN=Left><FONT SIZE=-1><B>Name</B></FONT></TH>"
						+ "<TH ALIGN=Left><FONT SIZE=-1><B>TDCJ<BR>Number</B></FONT></TH>"
						+ "<TH ALIGN=Left><FONT SIZE=-1><B>Race</B></FONT></TH>"
						+ "<TH ALIGN=Left><FONT SIZE=-1><B>Sex</B></FONT></TH>"
						+ "<TH ALIGN=Left><FONT SIZE=-1><B>Projected Release<BR>Date</B></FONT></TH>"
						+ "<TH ALIGN=Left><FONT SIZE=-1><B>Unit Of<BR>Assignment</B></FONT></TH>"
						+ "<TH ALIGN=Left><FONT SIZE=-1><B>Age</B></FONT></TH>"
						+ "</TR>";

				Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);

				// set prev next buttons
				String prev_next = ""; // getPrevNext(rsResponse);

				if (numberOfUnsavedDocument != null
						&& numberOfUnsavedDocument instanceof Integer) {
					footer = "\n</table><br>"
							+ prev_next
							+ CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL,
									viParseID,
									(Integer) numberOfUnsavedDocument);
				} else {
					footer = "\n</table><br>"
							+ prev_next
							+ CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL,
									viParseID, -1);
				}

				parsedResponse.setHeader(header);
				parsedResponse.setFooter(footer);
			}

			break;
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
			StringBuilder instrNo = new StringBuilder();
			String details = getDetails(rsResponse, instrNo);

			String filename = instrNo + ".html";

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&"
						+ Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST)
						+ originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				if (isInstrumentSaved(instrNo.toString(), null, data)) {
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
					rsResponse.contains("Offender Information Detail") ? ID_DETAILS
							: ID_SEARCH_BY_NAME);
			break;
		}
	}

	@SuppressWarnings("unused")
	private String getPrevNext(String table) {
		String res = "";
		try {
			TableTag page_x_of_y = ro.cst.tsearch.servers.functions.TXOffenderInformationOI
					.getTableFromList(
							org.htmlparser.Parser
									.createParser(table, null)
									.parse(null)
									.extractAllNodesThatMatch(
											new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(
											new HasAttributeFilter("width",
													"100%"), true),
							"Search Results:");
			res += page_x_of_y.toHtml();
			NodeList forms = org.htmlparser.Parser
					.createParser(table, null)
					.parse(null)
					.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("name", "navigate"), true);

			FormTag navigate_form = null;

			if (forms != null)
				navigate_form = (FormTag) org.htmlparser.Parser
						.createParser(table, null)
						.parse(null)
						.extractAllNodesThatMatch(new TagNameFilter("form"),
								true)
						.extractAllNodesThatMatch(
								new HasAttributeFilter("name", "navigate"),
								true).elementAt(0);

			// get links
			if (navigate_form != null) {
				InputTag prev = null;
				InputTag next = null;

				String link = CreatePartialLink(TSConnectionURL.idGET);

				for (int i = 0; i < navigate_form.getChildCount(); i++) {
					// String s = navigate_form.childAt(i).toHtml();
					if (navigate_form.childAt(i).toHtml().toLowerCase()
							.contains("<input type=\"button\"")
							&& navigate_form.childAt(i).toHtml()
									.contains("value=\"Previous\""))
						prev = (InputTag) navigate_form.childAt(i);
					if (navigate_form.childAt(i).toHtml().toLowerCase()
							.contains("<input type=\"button\"")
							&& navigate_form.childAt(i).toHtml()
									.contains("value=\"Next\""))
						next = (InputTag) navigate_form.childAt(i);
				}

				String links_table = "\n<table id=\"prev_next_links\">\n<tr>";

				if (prev != null) {
					String prev_link = prev.getAttribute("onclick").replaceAll(
							"(?ism).*'(http:[^,']*)',.*", "$1");
					links_table += "\n<td><a href=\"" + link + prev_link
							+ "\">Previous</a></td>";
				}

				if (next != null) {
					String next_link = next.getAttribute("onclick").replaceAll(
							"(?ism).*'(http:[^,']*)',.*", "$1");
					links_table += "\n<td><a href=\"" + link + next_link
							+ "\">Next</a></td>";
				}

				links_table += "\n</tr></table>\n";
				res += links_table;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "MISCELLANEOUS");
			data.put("dataSource", "OI");
		}
	}

	private String getDetails(String rsResponse, StringBuilder instrNo) {
		// clean resp & get instrNo

		String form = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(rsResponse, null);

			NodeList nodeList = htmlParser.parse(null);

			if (!(nodeList.size() > 0))
				return "";

			NodeList forms = nodeList.extractAllNodesThatMatch(
					new TagNameFilter("form"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("name", "details"));

			if (!(forms.size() > 0))
				return "";

			FormTag post_form = (FormTag) forms.elementAt(0);

			for (int i = 0; i < post_form.getChildCount(); i++) {
				if (post_form.getChild(i) instanceof InputTag)
					post_form.removeChild(i);
			}

			// extract instrNo from post_form
			String instr = "";

			for (int i = 0; i < post_form.getChildCount(); i++) {
				if (post_form.getChild(i) instanceof Div) {
					if (org.apache.commons.lang.StringUtils.strip(
							post_form.getChild(i).toPlainTextString()
									.replaceAll("\\s+", " ")).equals(
							"TDCJ Number:")) {
						instr = org.apache.commons.lang.StringUtils
								.strip(post_form.getChild(i + 2)
										.toPlainTextString()
										.replaceAll("\\s+", " "));
					}
				}
			}

			instrNo.append(instr);

			form = "<h1 class=\"header\"><strong>Offender Information Details</strong></h1>";

			form += post_form.toHtml().replaceAll("(?ism)<img[^>]*>", "");

			form = form.replaceAll(
					"<input type=\"submit\" value=\"Return to Search list\"/>",
					"");

			form = form
					.replaceAll(
							"<div class=\"basic_os_left_column\">([^<]*)</div>",
							"<div class=\"basic_os_left_column\"><strong>$1</strong></div>");

			form = form.replaceAll(
					"<div class=\"basic_os_left_column\">([^<]*)<strong>",
					"<br><div class=\"basic_os_left_column\">$1<strong>");

			form = form
					.replaceAll(
							"<table class=\"ws\" cellspacing=\"0\" cellpadding=\"2\"",
							"<table class=\"ws\" border=\"1\" cellspacing=\"1\" cellpadding=\"2\"");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return form.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1");
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {

		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		int numberOfUncheckedElements = 0;

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser
					.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);
			NodeList mainTableList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("class", "ws"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("summary"), true);

			if (!mainTableList.toHtml().contains("TDCJ Number")) {
				return intermediaryResponse;
			}

			TableTag tableTag = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows = tableTag.getRows();
			for (int i = 1; i < rows.length; i++) {
				// parse rows
				TableRow row = rows[i];
				if (row.getColumnCount() == 7) {
					LinkTag linkTag = ((LinkTag) row.getColumns()[0]
							.getChild(1));
					linkTag.removeAttribute("onclick");

					String link_prefix = CreatePartialLink(TSConnectionURL.idPOST);
					String link = super.getDefaultServerInfo()
							.getModule(TSServerInfo.NAME_MODULE_IDX)
							.getDestinationPage()
							+ "?page=POsearchList&selectRow=" + i;

					linkTag.setLink(link_prefix + link);
					ParsedResponse currentResponse = new ParsedResponse();
					ResultMap m = ro.cst.tsearch.servers.functions.TXOffenderInformationOI
							.parseIntermediaryRow(row, searchId);

					String instrNo = (String) m
							.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName());

					ResultMap m1 = m;
					m.removeTempDef();

					Bridge bridge = new Bridge(currentResponse, m, searchId);

					RegisterDocumentI regDoc = (RegisterDocumentI) bridge
							.importData();
					OffenderInformationDocumentI offenderDoc = ro.cst.tsearch.servers.functions.TXOffenderInformationOI
							.makeOffenderDoc(m1, regDoc);
					currentResponse.setDocument(offenderDoc);

					String checkBox = "checked";
					if (isInstrumentSaved(instrNo, offenderDoc, null)
							&& !Boolean.TRUE.equals(getSearch()
									.getAdditionalInfo("RESAVE_DOCUMENT"))) {
						checkBox = "saved";
					} else {
						numberOfUncheckedElements++;
						LinkInPage linkInPage = new LinkInPage(link_prefix
								+ link, link_prefix + link,
								TSServer.REQUEST_SAVE_TO_TSD);
						checkBox = "<input type='checkbox' name='docLink' value='"
								+ link_prefix + link + "'>";
						if (getSearch().getInMemoryDoc(link) == null) {
							getSearch().addInMemoryDoc(link, currentResponse);
						}
						currentResponse.setPageLink(linkInPage);
					}
					String rowHtml = "<tr><td>"
							+ checkBox
							+ "</td>"
							+ row.toHtml().replaceAll("(?ism)<tr[^>]*>", "")
									.replaceAll("(?ism)</tr>", "") + "</tr>";

					currentResponse.setAttribute(
							ParsedResponse.SERVER_ROW_RESPONSE, rowHtml
									.replaceAll(
											"(?ism)<a [^>]*>([^<]*)<[^>]*>",
											"$1"));
					currentResponse.setOnlyResponse(rowHtml);
					// currentResponse.setPageLink(new LinkInPage(link,
					// link,TSServer.REQUEST_SAVE_TO_TSD));

					intermediaryResponse.add(currentResponse);
				}
			}
			outputTable.append(table);
		} catch (Exception e) {
			e.printStackTrace();
		}
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
		return intermediaryResponse;
	}

	@Override
	public DocumentI smartParseDetails(ServerResponse response,
			String detailsHtml, boolean fillServerResponse) {

		DocumentI document = null;
		try {
			document = ro.cst.tsearch.servers.functions.TXOffenderInformationOI
					.parseAndFillResultMap(response, detailsHtml, searchId);

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (fillServerResponse) {
			response.getParsedResponse().setResponse(detailsHtml);
			if (document != null) {
				response.getParsedResponse().setDocument(document);
			}
		}

		return document;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

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
					FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(1,
					FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId,
							new String[] { "L;F;" }));
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}

	public static void main(String[] args) {

	}
}
