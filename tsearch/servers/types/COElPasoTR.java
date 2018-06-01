package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * 
 * @author Oprina George
 * 
 *         May 21, 2012
 */

public class COElPasoTR extends TSServer {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -409012124349462696L;

	public COElPasoTR(long searchId) {
		super(searchId);
	}

	public COElPasoTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		if (rsResponse.contains("is not valid. Valid range is")) {
			Response.getParsedResponse().setError("Schedule range is not valid. Valid range is 100000000 - 9999999999.");
			Response.getParsedResponse().setResponse("");
			return;
		}

		if (rsResponse.contains("Unable to retrieve data at this time. Please try back again later.") || rsResponse.contains("No records were selected.")) {
			Response.getParsedResponse().setError("No records found!");
			Response.getParsedResponse().setResponse("");
			return;
		}

		if (rsResponse.contains("Error ")) {
			Response.getParsedResponse().setError("An error has occured! Pleas try again!");
			Response.getParsedResponse().setResponse("");
			return;
		}

		if (rsResponse.contains("Property Tax Details") && viParseID == ID_SEARCH_BY_NAME)
			viParseID = ID_DETAILS;

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}

			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());

			break;

		case ID_DETAILS:
		case ID_SEARCH_BY_PARCEL:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			HashMap<String, String> data = new HashMap<String, String>();
			String details = getDetails(Response, rsResponse, accountId, data);
			String accountName = accountId.toString();

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				if (isInstrumentSaved(accountName, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
				}
				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = accountName + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;
		case ID_GET_LINK:
			ParseResponse(sAction, Response, rsResponse.contains("Property Tax Details") ? ID_DETAILS : ID_INTERMEDIARY);
			break;
		default:
			break;
		}

	}

	private static int	seq	= 0;

	protected synchronized static int getSeq() {
		return seq++;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			ParsedResponse parsedResponse = response.getParsedResponse();
			String rsResponse = response.getResult();

			NodeList nodes = new HtmlParser3(rsResponse).getNodeList();

			NodeList tableList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ContentPlaceHolder1_gvSearchResults"), true);

			// save req params
			Map<String, String> params = ro.cst.tsearch.connection.http2.COElPasoTR.isolateParams(response.getResult(), "form1");
			int seq = getSeq();
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);

			if (tableList.size() > 0) {

				String serverLink = ro.cst.tsearch.connection.http2.COElPasoTR.SERVER_LINK;

				if (tableList.size() == 1) {
					TableTag intermediary = (TableTag) tableList.elementAt(0);
					TableRow[] rows = intermediary.getRows();
					for (int i = 1; i < rows.length; i++) {
						TableRow row = rows[i];
						if (row.getColumnCount() == 3) {
							String link = "";
							for (TableColumn c : row.getColumns()) {
								if (c.getChildCount() > 0 && c.childAt(0) instanceof LinkTag) {
									LinkTag linkTag = (LinkTag) c.childAt(0);
									link = CreatePartialLink(TSConnectionURL.idGET) + serverLink + linkTag.getLink();
									linkTag.setLink(link);
								}
							}

							ParsedResponse currentResponse = new ParsedResponse();
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml().replaceAll("(?ism)</?font[^>]*>", ""));
							currentResponse.setOnlyResponse(row.toHtml());
							currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

							ResultMap m = ro.cst.tsearch.servers.functions.COElPasoTR.parseIntermediaryRow(row, 0);
							Bridge bridge = new Bridge(currentResponse, m, searchId);

							DocumentI document = (TaxDocumentI) bridge.importData();
							currentResponse.setDocument(document);

							intermediaryResponse.add(currentResponse);
						}
					}
				}
			}

			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
				parsedResponse.setHeader(makeHeader(0));
				parsedResponse.setFooter("\n</table><br>");
			} else {
				parsedResponse.setHeader("<table border=\"1\">");
				parsedResponse.setFooter("</table>");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return intermediaryResponse;
	}

	private String makeHeader(int viParseID) {
		String header = "<table id=\"intermediaryResults\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"95%\">";

		header += "<tr>"
				+ "<th>Schedule #</th>"
				+ "<th>Name</th>"
				+ "<th>Location</th>"
				+ "</tr>";

		return header;
	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "CNTYTAX");
		}
	}

	protected String getDetails(ServerResponse response, String rsResponse, StringBuilder accountId, HashMap<String, String> data) {
		try {
			loadDataHash(data);

			StringBuilder details = new StringBuilder();

			NodeList nodes = new HtmlParser3(rsResponse).getNodeList();
			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ContentPlaceHolder1_pnlPropertyTaxDetails"), true);

			if (tables.size() > 0) {
				String id = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "Schedule Number:"), "", true)
						.replaceAll("<[^>]*>", "").trim();
				accountId.append(id);
			} else
				return null;

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}

			details.append("<table id=details width='95%' align='center'><tr><td>");
			details.append(tables.elementAt(0).toHtml()
					.replaceAll("(?ism)</?a [^>]*>", "")
					.replaceAll("(?ism)<input [^>]*>", "")
					.replaceAll("(?ism)<td[^>]+class=\"aspNetDisabled\"[^>]+>\\s+</td>", "")
					.replaceAll("(?ism)<td[^>]+>False</td>", "")
					.replaceAll("(?ism)<(/)?textarea", "<$1div")
					.replaceAll("(?ism)Property Tax Details", "<b>Property Tax Details El Paso, Colorado</b>"));
			details.append("</td></tr></table>");

			return details.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		return ro.cst.tsearch.servers.functions.COElPasoTR.parseAndFillResultMap(response, detailsHtml, map);
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		if (hasPin()) {
			moduleList.add(new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX)));
		}

		if (hasStreet() || hasOwner()) {
			FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			
			if(hasStreet()){
				String streetNo = getSearchAttribute(SearchAttributes.P_STREETNO);
				if(StringUtils.isEmpty(streetNo)){
					module.clearSaKeys();
					module.forceValue(1, "0");
					module.forceValue(2, "999999");
					module.forceValue(4, getSearchAttribute(SearchAttributes.P_STREETNAME));
					module.setSaKey(6, SearchAttributes.OWNER_LNAME);
					module.setSaKey(7, SearchAttributes.OWNER_FNAME);
				}				
			}
			
			if(hasOwner()){
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				module.addFilter(addressFilter);
				module.addFilter(nameFilterHybridDoNotSkipUnique);
				module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" }));
			}
			
			moduleList.add(module);
		}

		serverInfo.setModulesForAutoSearch(moduleList);
	}

}
