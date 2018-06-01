package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Span;
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
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

public class FLOrangeAO extends TSServer {

	private static final long	serialVersionUID	= 1L;

	public FLOrangeAO(long searchId) {
		super(searchId);
	}

	public FLOrangeAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {
		return super.getDefaultServerInfo();
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Tidy.tidyParse(Response.getResult(), null);
		ParsedResponse parsedResponse = Response.getParsedResponse();

		mSearch.setAdditionalInfo("viParseID", viParseID);

		if (rsResponse.contains("Your search did not return any results.")) {
			Response.getParsedResponse().setError("No Results Found!");
			Response.getParsedResponse().setResponse("");
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_SUBDIVISION_NAME:
		case ID_SEARCH_BY_CONDO_NAME:
		case ID_SEARCH_BY_BOOK_AND_PAGE:
		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_SEARCH_BY_PROP_NO:
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
			ParseResponse(sAction, Response, rsResponse.contains("Update Information") ? ID_DETAILS : ID_INTERMEDIARY);
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
			NodeList divs = nodes.extractAllNodesThatMatch(new TagNameFilter("div"), true);

			NodeList pinDivList = divs.extractAllNodesThatMatch(
					new HasAttributeFilter("class", "DetailsSummary_TitleContainer"));

			if (pinDivList.size() > 0) {
				NodeList auxpinDivList = pinDivList.extractAllNodesThatMatch(new TagNameFilter("span"), true).extractAllNodesThatMatch(
						new HasAttributeFilter("style"));

				if (auxpinDivList.size() > 0) {
					String pin = auxpinDivList.elementAt(0).toPlainTextString().replaceAll("[ <>\n\r\t;]", "").replaceAll("&gt", "").replaceAll("&lt", "");
					accountId.append(pin);
				}
			} else
				return null;

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}

			// clean nodes
			NodeList cleanNodes = new NodeList();

			if (tables.size() > 0) {
				NodeList header = tables.extractAllNodesThatMatch(new HasAttributeFilter("class", "ParcelDetails_Header"));

				if (header.size() > 0) {
					cleanNodes.add(header.elementAt(0));
				} else {
					header = tables.extractAllNodesThatMatch(new HasAttributeFilter("class", "ParcelDetails_Header NoPaddingOrSpacing"));

					if (header.size() > 0) {
						cleanNodes.add(header.elementAt(0));
					}
				}
			}

			NodeList auxNodeList = divs
					.extractAllNodesThatMatch(new HasAttributeFilter("id",
							"ctl00_ctl00_ctl00_ctl00_ContentMain_ContentMain_ContentMain_ContentMain_TabContainer1_DetailsTab_DetailsController1_ctl00_TabContainer1_PropertyFeature"));

			if (auxNodeList.size() > 0) {
				Div d = (Div) auxNodeList.elementAt(0);
				d.removeAttribute("style");
				cleanNodes.add(d);
			}

			auxNodeList = divs
					.extractAllNodesThatMatch(new HasAttributeFilter(
							"id",
							"ctl00_ctl00_ctl00_ctl00_ContentMain_ContentMain_ContentMain_ContentMain_TabContainer1_DetailsTab_DetailsController1_ctl00_TabContainer1_ValueTax_ValuesTaxes1_UpdatePanel1"));

			if (auxNodeList.size() > 0) {
				Div d = (Div) auxNodeList.elementAt(0);
				d.removeAttribute("style");
				cleanNodes.add(d);
			}

			auxNodeList = divs
					.extractAllNodesThatMatch(new HasAttributeFilter("id",
							"ctl00_ctl00_ctl00_ctl00_ContentMain_ContentMain_ContentMain_ContentMain_TabContainer1_DetailsTab_DetailsController1_ctl00_TabContainer1_SaleAnalysis"));

			if (auxNodeList.size() > 0) {
				Div d = (Div) auxNodeList.elementAt(0);
				d.removeAttribute("style");
				cleanNodes.add(d);
			}

			// auxNodeList = divs
			// .extractAllNodesThatMatch(new HasAttributeFilter("id",
			// "ctl00_ctl00_ctl00_ctl00_ContentMain_ContentMain_ContentMain_ContentMain_TabContainer1_DetailsTab_DetailsController1_ctl00_TabContainer1_ServiceLocation"));
			//
			// if (auxNodeList.size() > 0) {
			// Div d = (Div) auxNodeList.elementAt(0);
			// d.removeAttribute("style");
			// cleanNodes.add(d);
			// }

			details.append("<table align=center width=95% id=summaryTable><tr><td>"
					+ "<p id=\"fakeHeader\" align=\"center\" width=100%><font style=\"font-size:xx-medium;\"><b>Document Index Detail <br>"
					+
					"FL Orange County Property Appraiser</b></font></p>"
					+ "<br></td></tr><tr><td>"
					+ cleanNodes.toHtml().replaceAll("(?ism)<p>.*?</p>", "")
							// .replaceAll("(?ism)<p>If listed as[^<]*</p>", "").replaceAll("(?ism)<p>The Total Land[^<]*</p>", "")
							// .replaceAll("(?ism)<legend>The Property Name[^<]*</legend>", "").replaceAll("(?ism)<legend>The Property Name[^<]*</legend>", "")
							.replaceAll("(?ism)<img[^>]*>", "")
							.replaceAll("(?ism)<span", "&nbsp;<span")
							.replaceAll("(?ism)</th", "&nbsp;</th")
					+ "</td></tr></table>");
			return details.toString().replaceAll("(?ism)</?a[^>]*>", "");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String makeHeader(int viParseID) {
		String header = "<table id=\"intermediaryResults\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"95%\">";

		if (viParseID == 2) {
			header += "<tr bgcolor=\"9d9ed9\">"
					+ "<th>Subdivision Name</th>"
					+ "</tr>";
		} else {
			header += "<tr bgcolor=\"9d9ed9\">"
					+ "<th>Owner Name</th>"
					+ "<th>Property Address</th>"
					+ "<th>Homestead</th>"
					+ "<th>Parcel Id</th>"
					+ "</tr>";
		}

		return header;
	}

	private String getPrevNext(ServerResponse resp, NodeList nodes, int seq, boolean getOnlyNextLink) {
		try {

			NodeList auxNodes = nodes
					.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("id",
									"ctl00_ctl00_ctl00_ctl00_ContentMain_ContentMain_ContentMain_ContentMain_TabContainer1_ResultsTab_Pager1"));
			if (auxNodes.size() > 0) {
				auxNodes = auxNodes.elementAt(0).getParent().getChildren().extractAllNodesThatMatch(
						new HasAttributeFilter("class", "PagerItemContainer"), true);
			}
			String res = "";
			NodeList goodNodes = new NodeList();

			if (auxNodes.size() > 0) {
				auxNodes = auxNodes.elementAt(0).getChildren();

				if (auxNodes != null && auxNodes.size() > 0) {
					for (int i = 0; i < auxNodes.size(); i++) {
						Node n = auxNodes.elementAt(i);

						if (!n.toHtml().contains("<img")) {
							n = n.getFirstChild();
							if (n instanceof LinkTag) {
								LinkTag link = (LinkTag) n;
								String onClick = link.getAttribute("onclick");

								if (StringUtils.isNotEmpty(onClick)) {
									onClick = onClick.replaceAll(".*?\\(([^)]+)\\).*", "$1").replaceAll("&#39;", "'");
									link.removeAttribute("onclick");

									String eventTarget = onClick.replaceAll("'([\\w&|$]+)','([\\w&|$]+)'", "$1");
									String eventArg = onClick.replaceAll("'([\\w&|$]+)','([\\w&|$]+)'", "$2");

									link.setLink(CreatePartialLink(TSConnectionURL.idPOST) + "/searches/ParcelSearch.aspx?eventTarget=" + eventTarget
											+ "&eventArg="
											+ eventArg + "&seq=" + seq);

									goodNodes.add(link);
								}
							} else if (n instanceof Span) {
								goodNodes.add(n);
							}
						} else if (getOnlyNextLink && i == auxNodes.size() - 1) {// last img tag is next link; the first one is prev(except for the first page,
																					// which doesn't have prev)

							n = n.getFirstChild();
							if (n instanceof ImageTag) {
								ImageTag imageTag = (ImageTag) n;
								String onClick = imageTag.getAttribute("onclick");

								if (StringUtils.isNotEmpty(onClick)) {
									onClick = onClick.replaceAll(".*?\\(([^)]+)\\).*", "$1").replaceAll("&#39;", "'");
									imageTag.removeAttribute("onclick");

									String eventTarget = onClick.replaceAll("'([\\w&|$]+)','([\\w&|$]+)'", "$1");
									String eventArg = onClick.replaceAll("'([\\w&|$]+)','([\\w&|$]+)'", "$2");
									LinkTag linkTag = new LinkTag();
									linkTag.setLink(CreatePartialLink(TSConnectionURL.idPOST) + "/searches/ParcelSearch.aspx?eventTarget=" + eventTarget
											+ "&eventArg="
											+ eventArg + "&seq=" + seq);

									res = "<a href=" + linkTag.getLink() + ">Next</a>";
									break;
								}
							}

						}
					}
				}
			}

			if (!getOnlyNextLink) {
				res = "<table id=prevNext width=20% cellspacing=0 align=left><tr><td align=left>"
						+ goodNodes.toHtml().replaceAll("(?ism)<a", "&nbsp;<a").replaceAll("(?ism)a>", "a>&nbsp;") + "</td></tr></table>";
			}

			return res;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String rsResponse, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			ParsedResponse parsedResponse = response.getParsedResponse();
			Integer viParseId = (Integer) mSearch.getAdditionalInfo("viParseID");

			NodeList nodes = new HtmlParser3(rsResponse).getNodeList();

			NodeList tableList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(
							new HasAttributeFilter("id",
									"ctl00_ctl00_ctl00_ctl00_ContentMain_ContentMain_ContentMain_ContentMain_TabContainer1_ResultsTab_ResultsGrid"), true);

			int seq = getSeq();

			// extract params
			try {
				NodeList inputs = nodes.extractAllNodesThatMatch(new TagNameFilter("input"), true);

				HashMap<String, String> params = new HashMap<String, String>();

				List<String> eventParamsNames = Arrays.asList(ro.cst.tsearch.connection.http2.FLOrangeAO.paramsNames);

				String[] psParams = new String[] { "OwnerName", "Address", "FullParcel", "PropertyName", "SubdivisionName", "UnitNumber", "BookNumber",
						"PageNumber", "InstrumentNumber" };

				if (inputs.size() > 0) {
					for (int i = 0; i < inputs.size(); i++) {
						String name = StringUtils.defaultString(((InputTag) inputs.elementAt(i)).getAttribute("name"));
						String value = StringUtils.defaultString(((InputTag) inputs.elementAt(i)).getAttribute("value")).replace("&quot;", "'");

						if (eventParamsNames.contains(name)) {
							params.put(name, value);
						}

						if (StringUtils.endsWithAny(name, psParams)) {
							params.put(name, value);
						}
					}
				}

				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
			} catch (Exception e) {
				e.printStackTrace();
			}

			int colsCount = 0;

			if (tableList.size() > 0) {

				TableTag intermediary = (TableTag) tableList.elementAt(0);

				outputTable.append(intermediary.toHtml());

				TableRow[] rows = intermediary.getRows();
				for (int i = 1; i < rows.length; i++) {
					TableRow row = rows[i];
					colsCount = row.getColumnCount();
					if (row.getColumnCount() >= 4 || row.getColumnCount() == 2) {
						String link = "";

						String onClick = row.getAttribute("onClick");

						row.removeAttribute("onClick");
						row.removeAttribute("class");

						if (StringUtils.isNotEmpty(onClick)) {
							onClick = onClick.replaceAll(".*?\\(([^)]+)\\)", "$1").replaceAll("&#39;", "'");

							String eventTarget = onClick.replaceAll("'([\\w&|$]+)','([\\w&|$]+)'", "$1");
							String eventArg = onClick.replaceAll("'([\\w&|$]+)','([\\w&|$]+)'", "$2");

							link = CreatePartialLink(TSConnectionURL.idPOST) + "/searches/ParcelSearch.aspx?eventTarget=" + eventTarget + "&eventArg="
									+ eventArg + "&seq=" + seq;
						}

						if (colsCount >= 5) {
							row.removeChild(row.findPositionOf(row.getColumns()[4]));
						}

						if (colsCount==2) {
							row.removeChild(row.findPositionOf(row.getColumns()[0]));
						}

						String rowHTML = row.toHtml();
						if (rowHTML.matches("(?ism).*\\d{2}-\\d{2}-\\d{2}-\\d{4}-\\d{2}-\\d{3}.*")) {
							rowHTML = rowHTML.replaceAll("(?ism)(\\d{2}-\\d{2}-\\d{2}-\\d{4}-\\d{2}-\\d{3})", "<a href=>$1</a>")
									.replace("<a href=", "<a href=" + link);
						} else if (row.getColumnCount() == 1) {
							rowHTML = rowHTML.replaceAll("(?ism)<td>(.*)</td>", "<td><a href=>$1</a></td>")
									.replace("<a href=", "<a href=" + link);
						}

						ParsedResponse currentResponse = new ParsedResponse();
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHTML);
						currentResponse.setOnlyResponse(rowHTML);

						if (row.getColumnCount() == 2) {
							currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_GO_TO_LINK));
						} else {
							currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
						}

						ResultMap m = ro.cst.tsearch.servers.functions.FLOrangeAO.parseIntermediaryRow(row, viParseId);
						Bridge bridge = new Bridge(currentResponse, m, searchId);

						DocumentI document = (AssessorDocumentI) bridge.importData();
						currentResponse.setDocument(document);

						intermediaryResponse.add(currentResponse);

					}
				}

			}

			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
				parsedResponse.setHeader(makeHeader(colsCount));
				parsedResponse.setFooter("\n</table><br>" + getPrevNext(response, nodes, seq, false));
			} else {
				parsedResponse.setHeader("<table border=\"1\">");
				parsedResponse.setNextLink(getPrevNext(response, nodes, seq, true));
				parsedResponse.setFooter("</table>");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return intermediaryResponse;
	}

	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		ro.cst.tsearch.servers.functions.FLOrangeAO.parseAndFillResultMap(response, detailsHtml, resultMap);
		return null;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		// pin
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
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
