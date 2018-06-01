package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

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
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * 
 * @author Oprina George
 * 
 *         Mar 16, 2012
 */

public class OHFranklinTR extends TSServer {
	private static final long	serialVersionUID	= 1L;

	public OHFranklinTR(long searchId) {
		super(searchId);
	}

	public OHFranklinTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		mSearch.setAdditionalInfo("viParseID", viParseID);

		int localParseID = viParseID;

		if (viParseID != ID_SAVE_TO_TSD && rsResponse.contains("REAL ESTATE TAX AND PAYMENT INFORMATION")) {
			localParseID = ID_DETAILS;
		}

		switch (localParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:
		case ID_INTERMEDIARY:

			if (rsResponse.contains("No Records Found")) {
				Response.getParsedResponse().setError("No Results Found!");
				Response.getParsedResponse().setResponse("");
				return;
			}
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}

			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());

			break;

		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			HashMap<String, String> data = new HashMap<String, String>();
			String details = getDetails(Response, rsResponse, accountId, data);
			String accountName = accountId.toString();

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
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

				msSaveToTSDFileName = accountName + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;
		case ID_GET_LINK:
			ParseResponse(sAction, Response, rsResponse.contains("REAL ESTATE TAX AND PAYMENT INFORMATION") ? ID_DETAILS : ID_INTERMEDIARY);
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
		try {
			loadDataHash(data);
			StringBuilder details = new StringBuilder();

			NodeList nodes = new HtmlParser3(Tidy.tidyParse(rsResponse, null)).getNodeList();
			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "0"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "0"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("bordercolor", "Silver"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("height", "100%"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("valign", "TOP"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"), true);

			if (tables.size() > 0) {
				String id = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(tables, "Parcel ID"), "", true)
						.replaceAll("<[^>]*>", "").trim();
				accountId.append(id);
			} else
				return null;

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}

			TableTag detailsTable = (TableTag) tables.elementAt(0);
			detailsTable.setAttribute("id", "detailsTable");
			detailsTable.setAttribute("align", "center");

			details.append(detailsTable.toHtml() + "\n");

			return details.toString().replaceAll("(?ism)</?a [^>]*>", "");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String makeHeader(int viParseID) {
		String header = "<table id=\"intermediaryResults\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"95%\">";

		switch (viParseID) {
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:
			header += "<tr bgcolor=\"244564\">"
					+ "<th><font color=\"FFFFFF\">Parcel ID</font></th>"
					+ "<th><font color=\"FFFFFF\">Address</font></th>"
					+ "<th><font color=\"FFFFFF\">Owner</font></th>"
					+ "<th><font color=\"FFFFFF\">Legal Description</font></th>"
					+ "</tr>";
			break;
		case ID_SEARCH_BY_NAME:
			header += "<tr bgcolor=\"244564\">"
					+ "<th><font color=\"FFFFFF\">Owner</font></th>"
					+ "<th><font color=\"FFFFFF\">Parcel ID</font></th>"
					+ "<th><font color=\"FFFFFF\">Address</font></th>"
					+ "<th><font color=\"FFFFFF\">Legal Description</font></th>"
					+ "</tr>";
			break;
		}
		return header;
	}

	private String getPrevNext(ServerResponse resp, NodeList nodes) {
		try {
			if (nodes.size() == 0)
				return "";

			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("align", "CENTER"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("valign", "MIDDLE"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("bordercolor", "GREEN"), true);

			if (tables != null && tables.size() > 0) {
				TableTag t = (TableTag) tables.elementAt(0);
				if (t.getRowCount() > 0) {
					TableRow r = t.getRow(0);
					if (r.getColumnCount() > 2) {
						StringBuffer res = new StringBuffer("<table id=prev_next width=130 align=center><tr>");
						for (int i = 1; i < r.getColumnCount() - 1; i++) {
							res.append(r.getColumns()[i].toHtml()
									.replaceAll("(?ism)nowrap", "")
									.replace("../../", CreatePartialLink(TSConnectionURL.idGET) + "/propertymax/")
									.replace("list_address.asp", CreatePartialLink(TSConnectionURL.idGET) + "/propertymax/list_address.asp")
									.replace("list_parcelid.asp", CreatePartialLink(TSConnectionURL.idGET) + "/propertymax/list_parcelid.asp"));
						}
						res.append("<td></td></tr></table>");

						return res.toString();
					}
				}
			}

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

			NodeList tableList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "2"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "0"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("bordercolor"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("valign", "TOP"), true);

			if (tableList.size() > 0) {

				TableTag intermediary = (TableTag) tableList.elementAt(0);
				TableRow[] rows = intermediary.getRows();

				// get type of search from first row
				if (rows[0].getColumnCount() == 4) {
					if (rows[0].getColumns()[0].toPlainTextString().contains("Parcel ID")) {
						viParseId = ID_SEARCH_BY_PARCEL;
					} else {
						viParseId = ID_SEARCH_BY_NAME;
					}
				}

				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];
					if (row.getColumnCount() == 4) {
						String link = "";
						TableColumn c = row.getColumns()[0];
						if (c.getChildCount() > 4) {
							LinkTag linkTag = null;
							if (c.childAt(4) instanceof LinkTag)
								linkTag = (LinkTag) c.childAt(4);
							if (c.childAt(2) instanceof LinkTag)
								linkTag = (LinkTag) c.childAt(2);

							if (linkTag != null) {
								link = CreatePartialLink(TSConnectionURL.idGET) + linkTag.getLink().replaceAll("(?ism)^[./]*(agency)", "/propertymax/$1");
								linkTag.setLink(link);
							}
						}

						ParsedResponse currentResponse = new ParsedResponse();
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml().replaceAll("(?ism)</?font[^>]*>", ""));
						currentResponse.setOnlyResponse(row.toHtml());
						currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

						ResultMap m = ro.cst.tsearch.servers.functions.OHFranklinTR.parseIntermediaryRow(row, viParseId);
						Bridge bridge = new Bridge(currentResponse, m, searchId);

						DocumentI document = (TaxDocumentI) bridge.importData();
						currentResponse.setDocument(document);

						intermediaryResponse.add(currentResponse);

					}
				}
			}

			parsedResponse.setHeader(makeHeader(viParseId));
			parsedResponse.setFooter("\n</table><br>" + getPrevNext(response, nodes));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return intermediaryResponse;
	}

	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		ro.cst.tsearch.servers.functions.OHFranklinTR.parseAndFillResultMap(response, detailsHtml, resultMap);
		return null;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m;

		FilterResponse addressFilter = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);

		// search by parcel ID
		if (hasPin()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			l.add(m);
		}

		// search by address
		if (hasStreet()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			l.add(m);
		}

		// search by owner
		if (hasOwner()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			// m.setIteratorType(ModuleStatesIterator.TYPE_AO_KYJEFFERSON);
			// m.getFunction( 0 ).setIteratorType( FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE );

			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);

			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(m, searchId, new String[] { "L F M;;", "L;F;", "L;M;", "L;f;", "L;m;" });
			m.addIterator(nameIterator);

			l.add(m);
		}

		serverInfo.setModulesForAutoSearch(l);
	}

}
