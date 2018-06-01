package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
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
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

/**
 * 
 * @author Oprina George
 * 
 *         Oct 5, 2012
 */

public class FLHillsboroughAO extends TSServer {
	private static final long	serialVersionUID	= 1L;

	public FLHillsboroughAO(long searchId) {
		super(searchId);
	}

	public FLHillsboroughAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		mSearch.setAdditionalInfo("viParseID", viParseID);

		if (rsResponse.contains("0 Matches found")) {
			Response.getParsedResponse().setError("No Results Found!");
			Response.getParsedResponse().setResponse("");
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PROP_NO:
		case ID_SEARCH_BY_PARCEL:
		case ID_INTERMEDIARY:

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}

			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());
			parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());

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
			ParseResponse(
					sAction,
					Response,
					rsResponse
							.contains("Please note that property values on this site are continually being updated and are a work in progress throughout the year. The final values are certified in October of each year.") ? ID_DETAILS
							: ID_INTERMEDIARY);
			break;
		default:
			break;
		}
	}

	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "ASSESSOR");
			data.put("dataSource", "AO");
		}
	}

	protected String getDetails(ServerResponse response, String rsResponse, StringBuilder accountId, HashMap<String, String> data) {
		try {
			loadDataHash(data);
			StringBuilder details = new StringBuilder();

			NodeList nodes = new HtmlParser3(Tidy.tidyParse(rsResponse, null)).getNodeList();
			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			if (tables.size() > 0) {
				String id = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "Folio:", true), "", true).replaceAll("<[^>]*>", "")
						.trim();
				accountId.append(id);
			} else
				return null;

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}

			// clean nodes
			nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "CamaDisplayArea"));

			if (nodes.size() > 0) {

				NodeList goodNodes = new NodeList();

				NodeList nds = null;
				Node n = null;
				if ((nds = nodes.extractAllNodesThatMatch(new HasAttributeFilter("id", "displayParcelInfo"), true)) != null && nds.size() > 0) {
					goodNodes.add(nds.elementAt(0));
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "h3", "", "", "VALUE SUMMARY", true)) != null) {
					goodNodes.add(n);
				}

				if ((nds = nodes.extractAllNodesThatMatch(new HasAttributeFilter("id", "displayValueOther"), true)) != null && nds.size() > 0) {
					goodNodes.add(nds.elementAt(0));
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "h3", "", "", "Sales History", true)) != null) {
					goodNodes.add(n);
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "table", "class", "dataGrid", new String[] { "Book", "Page", "Qualified", "Sales Price" },
								true)) != null) {
					((TableTag) n).setAttribute("id", "references");
					((TableTag) n).setAttribute("border", "1");
					goodNodes.add(n);
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "h3", "", "", "BUILDING", true)) != null) {
					goodNodes.add(n);
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "table", "", "", new String[] { "Building Characteristics", "Type", "Element",
								"Code", "Construction Detail" },
								true)) != null) {
					((TableTag) n).setAttribute("id", "building");
					((TableTag) n).setAttribute("border", "1");
					goodNodes.add(n);
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "table", "cellpadding", "1", new String[] { "Area Type", "Gross Area", "Heated Area" },
								true)) != null) {
					((TableTag) n).setAttribute("id", "area");
					((TableTag) n).setAttribute("border", "1");
					goodNodes.add(n);
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "h3", "", "", "Extra Features", true)) != null) {
					goodNodes.add(n);
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "table", "class", "dataGrid", new String[] { "Description", "OB/XF Code", "Year on Roll" },
								true)) != null) {
					((TableTag) n).setAttribute("id", "extra");
					((TableTag) n).setAttribute("border", "1");
					goodNodes.add(n);
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "h3", "", "", "Land Lines", true)) != null) {
					goodNodes.add(n);
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "table", "class", "dataGrid", new String[] { "Use Code", "Zone", "Total Land Units" },
								true)) != null) {
					((TableTag) n).setAttribute("id", "land");
					((TableTag) n).setAttribute("border", "1");
					goodNodes.add(n);
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "h3", "", "", "Legal Lines", true)) != null) {
					goodNodes.add(n);
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "table", "class", "dataGrid", new String[] { "Legal Description" },
								true)) != null) {
					((TableTag) n).setAttribute("id", "legal");
					((TableTag) n).setAttribute("border", "1");
					goodNodes.add(n);
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "h3", "", "", "TAXING AUTHORITY TAX INFORMATION", true)) != null) {
					goodNodes.add(n);
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "table", "class", "dataGrid", new String[] { "TAXABLE VALUE", "Taxing Authority",
								"YOUR TAX RATE AND TAXES" },
								true)) != null) {
					((TableTag) n).setAttribute("id", "taxes");
					((TableTag) n).setAttribute("border", "1");
					goodNodes.add(n);
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "h3", "", "", "PROPERTY APPRAISER VALUE INFORMATION", true)) != null) {
					goodNodes.add(n);
				}

				if ((n = ro.cst.tsearch.servers.functions.FLHillsboroughAO
						.getNodeByTypeAttributeDescription(nodes, "table", "class", "dataGrid", new String[] { "COUNTY", "PUBLIC SCHOOLS",
								"MUNICIPAL", "OTHER DISTRICTS" },
								true)) != null) {
					((TableTag) n).setAttribute("id", "taxes");
					((TableTag) n).setAttribute("border", "1");
					goodNodes.add(n);
				}

				details.append("<table align=center width=95% id=summaryTable><tr><td>"
						+ "<p id=\"fakeHeader\" align=\"center\" width=100%><font style=\"font-size:xx-medium;\"><b>Document Index Detail <br>" +
						"FL Hillsborough County Property Appraiser</b></font></p>"
						+ "<br></td></tr><tr><td>"
						+ goodNodes.toHtml()
						+ "</td></tr></table>");

				return details.toString().replaceAll("(?ism)</?a [^>]*>", "").replaceAll("(?ism)<img[^>]*>", "");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String makeHeader(int viParseID) {
		String header = "<table id=\"intermediaryResults\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"95%\">";

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
			header += "<tr bgcolor=\"9d9ed9\">"
					+ "<th>No.</th>"
					+ "<th>Owner Name</th>"
					+ "<th>Parcel Id</th>"
					+ "<th>Folio</th>"
					+ "<th>Property Address</th>"
					+ "<th>City</th>"
					+ "</tr>";
			break;
		case ID_SEARCH_BY_ADDRESS:
			header += "<tr bgcolor=\"9d9ed9\">"
					+ "<th>No.</th>"
					+ "<th>Property Address</th>"
					+ "<th>Parcel Id</th>"
					+ "<th>Folio</th>"
					+ "<th>Owner Name</th>"
					+ "<th>City</th>"
					+ "</tr>";
			break;
		case ID_SEARCH_BY_PROP_NO:
			header += "<tr bgcolor=\"9d9ed9\">"
					+ "<th>No.</th>"
					+ "<th>Folio</th>"
					+ "<th>Parcel Id</th>"
					+ "<th>Owner Name</th>"
					+ "<th>Property Address</th>"
					+ "<th>City</th>"
					+ "</tr>";
			break;
		case ID_SEARCH_BY_PARCEL:
			header += "<tr bgcolor=\"9d9ed9\">"
					+ "<th>No.</th>"
					+ "<th>Parcel Id</th>"
					+ "<th>Folio</th>"
					+ "<th>Owner Name</th>"
					+ "<th>Property Address</th>"
					+ "<th>City</th>"
					+ "</tr>";
			break;
		}

		return header;
	}

	private String getPrevNext(ServerResponse resp, NodeList nodes) {
		try {

			NodeList newNodes = nodes.extractAllNodesThatMatch(new TagNameFilter("form"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("action", "CamaDisplay.aspx"));

			NodeList links = nodes.extractAllNodesThatMatch(new TagNameFilter("a"), true);

			LinkTag prev = null;
			LinkTag next = null;

			if (links.size() > 0) {
				for (int i = 0; i < links.size(); i++) {
					if (links.elementAt(i) instanceof LinkTag && links.elementAt(i).toPlainTextString().contains("Prior")) {
						prev = (LinkTag) links.elementAt(i);
					}
					if (links.elementAt(i) instanceof LinkTag && links.elementAt(i).toPlainTextString().contains("Next")) {
						next = (LinkTag) links.elementAt(i);
					}

					if (prev != null && next != null)
						break;
				}
			}

			String prevLink = "";
			String nextLink = "";

			if (newNodes.size() > 0) {
				FormTag form = (FormTag) newNodes.elementAt(0);

				NodeList inputs = form.getFormInputs();

				StringBuffer params = new StringBuffer();

				for (int i = 0; i < inputs.size(); i++) {
					InputTag in = (InputTag) inputs.elementAt(i);
					params.append(in.getAttribute("name") + "=" + in.getAttribute("value") + "&");
				}

				if (next != null) {
					String currentIndex = next.getAttribute("onclick").replaceAll("(?ism)[^(]*\\(([^)]*)\\)", "$1").replace(";", "");

					nextLink = CreatePartialLink(TSConnectionURL.idPOST)
							+ "/"
							+ form.getAttribute("action")
							+ "?"
							+ params.toString().trim().replaceAll("&$", "").replaceAll(" ", "+")
									.replaceAll("(?ism)currentIndex=\\d+", "currentIndex=" + currentIndex);
				}

				if (prev != null) {
					String currentIndex = prev.getAttribute("onclick").replaceAll("(?ism)[^(]*\\(([^)]*)\\)", "$1").replace(";", "");

					prevLink = CreatePartialLink(TSConnectionURL.idPOST)
							+ "/"
							+ form.getAttribute("action")
							+ "?"
							+ params.toString().trim().replaceAll("&$", "").replaceAll(" ", "+")
									.replaceAll("(?ism)currentIndex=\\d+", "currentIndex=" + currentIndex);
				}
			}

			String res = "<table id=prevNext width=20% cellspacing=0 align=left><tr><td align=right>" +
					(StringUtils.isNotEmpty(prevLink) ? "<a href=" + prevLink + ">Prev</a></td><td>" : "</td><td>") +
					(StringUtils.isNotEmpty(nextLink) ? "<a href=" + nextLink + ">Next</a>" : "") + "</td></tr></table>";

			return res;
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

			NodeList nodes = new HtmlParser3(rsResponse).getNodeList();

			NodeList tableList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "3px"), true);

			if (tableList.size() > 0) {

				TableTag intermediary = (TableTag) tableList.elementAt(0);

				outputTable.append(intermediary.toHtml());

				TableRow[] rows = intermediary.getRows();
				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];
					if (row.getColumnCount() == 6) {

						String link = "";

						TableColumn c = row.getColumns()[2];
						if (!(c.getChildCount() > 0 && c.childAt(0) instanceof LinkTag)) {
							c = row.getColumns()[1];
						}

						if (c.getChildCount() > 0 && c.childAt(0) instanceof LinkTag) {
							LinkTag linkTag = (LinkTag) c.childAt(0);
							linkTag.removeAttribute("onclick");
							link = CreatePartialLink(TSConnectionURL.idGET) + "/" + linkTag.getLink();
							link = link.replace("&amp;", "&");
							linkTag.setLink(link);
						}

						ParsedResponse currentResponse = new ParsedResponse();
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
						currentResponse.setOnlyResponse(row.toHtml());
						currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

						ResultMap m = ro.cst.tsearch.servers.functions.FLHillsboroughAO.parseIntermediaryRow(row, viParseId);
						Bridge bridge = new Bridge(currentResponse, m, searchId);

						DocumentI document = (AssessorDocumentI) bridge.importData();
						currentResponse.setDocument(document);

						intermediaryResponse.add(currentResponse);

					}
				}

			}

			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
				parsedResponse.setHeader(makeHeader(viParseId));
				parsedResponse.setFooter("\n</table><br>" + getPrevNext(response, nodes));
			} else {
				parsedResponse.setHeader("<table border=\"1\">");
				parsedResponse.setFooter("</table>");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return intermediaryResponse;
	}

	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
		ro.cst.tsearch.servers.functions.FLHillsboroughAO.parseAndFillResultMap(response, detailsHtml, resultMap);
		return null;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		// pin
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PROP_NO_IDX));
			moduleList.add(module);
		}

		// address
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);

		if (hasStreet()) {
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
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
}
