package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.ScriptTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.Node;

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
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

import com.stewart.ats.base.document.DocumentI;

/**
 * 
 * @author Oprina George
 * 
 *         Nov 1, 2012
 */

public class ALMontgomeryTR extends TSServer {

	private static final long	serialVersionUID	= 1L;
	private static HashMap<String, Integer> taxYears = new HashMap<String, Integer>();
	
	public ALMontgomeryTR(long searchId) {
		super(searchId);
	}

	public ALMontgomeryTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		if (viParseID != ID_INTERMEDIARY && viParseID != ID_DETAILS && viParseID != ID_SAVE_TO_TSD)
			mSearch.setAdditionalInfo("viParseID", viParseID);

		if (rsResponse.contains("No Records Found.")) {
			Response.getParsedResponse().setError("No Results Found!");
			Response.getParsedResponse().setResponse("");
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:
		case ID_INTERMEDIARY:

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}

			// save intermediary responses

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
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

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
			ParseResponse(sAction, Response, rsResponse.contains("Records Found.") ? ID_INTERMEDIARY : ID_DETAILS);
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
			StringBuilder details = new StringBuilder();

			NodeList nodes = new HtmlParser3(rsResponse).getNodeList();

			NodeList auxNodes = new NodeList();

			String parcel = "";
			String year = "";

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				if (nodes.size() > 0) {
					parcel = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "PARCEL #:"), "", false).trim();
					year = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodes, "Tax Year:"), "", false).trim();
					accountId.append(parcel.replaceAll("[^\\d]", ""));
				}

				loadDataHash(data);
				data.put("year", year);

				return rsResponse;
			}

			// get all pages

			// navigation page
			auxNodes = nodes.extractAllNodesThatMatch(new TagNameFilter("iframe"), true).extractAllNodesThatMatch(new HasAttributeFilter("id", "Iframe2"));

			if (auxNodes.size() > 0) {
				TagNode frame = (TagNode) auxNodes.elementAt(0);

				String link2 = frame.getAttribute("src");

				String parcelNavigationPage = getLinkContents(getDataSite().getServerHomeLink() + link2);

				if (StringUtils.isNotEmpty(parcelNavigationPage)) {
					// get links
					auxNodes = new HtmlParser3(parcelNavigationPage).getNodeList().extractAllNodesThatMatch(new HasAttributeFilter("script"), true);

					if (auxNodes.size() > 0) {
						ScriptTag script = null;

						for (int i = 0; i < auxNodes.size(); i++) {
							if (auxNodes.elementAt(i).toHtml().contains("NavigateToURL") && auxNodes.elementAt(i) instanceof ScriptTag) {
								script = (ScriptTag) auxNodes.elementAt(i);
							}
						}

						if (script == null) {
							return "";
						}

						String code = script.getScriptCode();

						// get what we need from javascript

						String summaryLink = code.replaceAll("(?ism).*if \\(lSelectedTabId.toString\\(\\) == \\'Summary\\'\\)[^{]*\\{([^}]*)\\}.*", "$1")
								.replaceAll(".*\"([^\"]*)\".*", "$1")
								.trim().replaceAll(" ", "%20");
						String landLink = code.replaceAll("(?ism).*if \\(lSelectedTabId == \\'Land\\'\\)[^{]*\\{([^}]*)\\}.*", "$1")
								.replaceAll(".*\"(CA_PTLand.aspx\\?LandId=[^;]*);.*", "$1")
								.trim().replaceAll("('|\") \\+ '", "").replaceAll("[\"']", "").replaceAll(" ", "%20");
						String buildingsLink = code.replaceAll("(?ism).*if \\(lSelectedTabId == \\'Buildings\\'\\)[^{]*\\{([^}]*)\\}.*", "$1")
								.replaceAll(".*\"([^\"]*)\".*", "$1")
								.trim().replaceAll(" ", "%20");
						String salesLink = code.replaceAll("(?ism).*if \\(lSelectedTabId == \\'Sales\\'\\)[^{]*\\{([^}]*)\\}.*", "$1")
								.replaceAll(".*\"([^\"]*)\".*", "$1")
								.trim().replaceAll(" ", "%20");

						String summary = getLinkContents(getDataSite().getServerHomeLink() + summaryLink);
						String land = getLinkContents(getDataSite().getServerHomeLink() + landLink);
						String building = getLinkContents(getDataSite().getServerHomeLink() + buildingsLink);
						String sales = getLinkContents(getDataSite().getServerHomeLink() + salesLink);

						// get parcel & year
						parcel = summaryLink.replaceAll(".*(?ism)ParcelNum=([^&]*)\\&.*", "$1").replaceAll("%20", "").replaceAll("[^\\d]", "");
						year = summaryLink.replaceAll(".*(?ism)RecordYear=([^&]*)\\&.*", "$1");

						accountId.append(parcel);
						loadDataHash(data);
						data.put("year", year);

						details.append("<table id=mainTable width=95%><tr><td>");

						auxNodes = new HtmlParser3(parcelNavigationPage).getNodeList();

						auxNodes = auxNodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainTable"));

						if (auxNodes.size() > 0) {
							auxNodes = auxNodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "1"))
									.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "1"));

							if (auxNodes.size() == 2) {
								details.append("<table id=navigation><tr><td>" + auxNodes.elementAt(0).toHtml() + "</td></tr><tr><td>"
										+ auxNodes.elementAt(1).toHtml() + "</td></tr></table>");
							}
						}

						details.append("<table id=year><tr><td>Tax Year:</td><td>" + year + "</td></tr></table>");

						auxNodes = new HtmlParser3(summary).getNodeList();

						auxNodes = auxNodes.extractAllNodesThatMatch(new TagNameFilter("form"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "thisForm"));

						if (auxNodes.size() > 0) {
							auxNodes = auxNodes.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"))
									.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "0"))
									.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "0"));

							if (auxNodes.size() > 0) {
								details.append("<table id=summary><tr><td>" + auxNodes.elementAt(0).toHtml() + "</td></tr></table>");
							}
						}

						auxNodes = new HtmlParser3(land).getNodeList();

						auxNodes = auxNodes.extractAllNodesThatMatch(new TagNameFilter("form"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "thisForm"));

						if (auxNodes.size() > 0) {
							auxNodes = auxNodes.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"))
									.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "1"))
									.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "0"));

							if (auxNodes.size() > 0) {
								details.append("<table id=land><tr><td>" + auxNodes.elementAt(0).toHtml() + "</td></tr></table>");
							}
						}

						auxNodes = new HtmlParser3(building).getNodeList();

						auxNodes = auxNodes.extractAllNodesThatMatch(new TagNameFilter("form"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "thisForm"));

						if (auxNodes.size() > 0) {
							auxNodes = auxNodes.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"))
									.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "1"))
									.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "0"));

							if (auxNodes.size() > 0) {
								details.append("<table id=building><tr><td>" + auxNodes.elementAt(0).toHtml() + "</td></tr></table>");
							}
						}

						auxNodes = new HtmlParser3(sales).getNodeList();

						auxNodes = auxNodes.extractAllNodesThatMatch(new TagNameFilter("form"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "thisForm"));

						if (auxNodes.size() > 0) {
							auxNodes = auxNodes.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"))
									.extractAllNodesThatMatch(new HasAttributeFilter("cellspacing", "1"))
									.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "0"));

							if (auxNodes.size() > 0) {
								details.append("<table id=sales><tr><td>" + auxNodes.elementAt(0).toHtml() + "</td></tr></table>");
							}
						}

						details.append("</td></tr></table>");

						return details.toString().replaceAll("</?a[^>]*>", "");
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String getPrevNext(ServerResponse resp, NodeList nodes, int seq) {
		try {
			if (nodes.size() > 0) {
				InputTag prevTag = null;
				InputTag nextTag = null;

				boolean hasPrev = false;
				boolean hasNext = false;

				NodeList auxNodes = nodes.extractAllNodesThatMatch(new TagNameFilter("input"), true).extractAllNodesThatMatch(
						new HasAttributeFilter("id", "Prev"));

				if (auxNodes.size() > 0) {
					prevTag = (InputTag) auxNodes.elementAt(0);
					hasPrev = StringUtils.isEmpty(prevTag.getAttribute("disabled"));
				}
				
				auxNodes = nodes.extractAllNodesThatMatch(new TagNameFilter("input"), true).extractAllNodesThatMatch(new HasAttributeFilter("id", "NextPage"));

				if (auxNodes.size() > 0) {
					nextTag = (InputTag) auxNodes.elementAt(0);
					hasNext = StringUtils.isEmpty(nextTag.getAttribute("disabled"));
				}
				
				String res = "<table id=prevNext width=95% cellspacing=0 align=center><tr>";

				int param = 0;
				
				String pageNumScript = "function NextRecordx(PageNum)";
				
				auxNodes = nodes.extractAllNodesThatMatch(new TagNameFilter("script"), true);
				
				if (auxNodes.size() > 0) {
					for (int i = 0; i < auxNodes.size(); i++) {
						if(auxNodes.elementAt(i).toHtml().contains("function NextRecordx(PageNum)")){
							pageNumScript = auxNodes.elementAt(i).toHtml();									
						}
					}
				}
				
				if(StringUtils.isNotEmpty(pageNumScript)){
					pageNumScript = pageNumScript.split("OnParcelNumKeyUp")[0].replaceAll("(?ism).*PageNum == 2[^{]*\\{([^}]*)\\}.*","$1")
							.replaceAll("\\s+", " ").replaceAll(".*(\\d+) - \\d+.*", "$1");
					
					if(pageNumScript.matches("\\d+"))
						param = Integer.parseInt(pageNumScript);
				}
				
				if (hasPrev && param != 0) {
					res += "<td><a href=" + CreatePartialLink(TSConnectionURL.idPOST) + "/CA_PropertyTaxSearch.aspx?fakeParam=" + (param-1)  + "&seq=" + seq
							+ "> Prev </td>";
				}

				if (hasNext && param != 0) {
					res += "<td><a href=" + CreatePartialLink(TSConnectionURL.idPOST) + "/CA_PropertyTaxSearch.aspx?fakeParam=" + (param+1) + "&seq=" + seq
							+ "> Next </td>";
				}
				
				res += "</tr>";

				auxNodes = nodes.extractAllNodesThatMatch(new TagNameFilter("span"), true).extractAllNodesThatMatch(
						new HasAttributeFilter("id", "TotalRecFound"));

				if (auxNodes.size() > 0) {
					res += "<tr>" + auxNodes.elementAt(0).toHtml() + "</tr>";
				}

				res += "</table>";

				return res;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private static HashMap<String, ResultMap>	intermediaryData	= new HashMap<String, ResultMap>();

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			ParsedResponse parsedResponse = response.getParsedResponse();
			Integer viParseId = (Integer) mSearch.getAdditionalInfo("viParseID");
			String rsResponse = response.getResult().replaceAll("(?sim)&nbsp;", " ");

			NodeList nodes = new HtmlParser3(rsResponse).getNodeList();

			NodeList tableList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "BodyTable"), true);

			NodeList formList = nodes.extractAllNodesThatMatch(new TagNameFilter("form"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "thisForm"), true);

			String year = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(nodes, "TAX YEAR:"), "", false)
					.replaceAll("TAX YEAR:", "")
					.replaceAll("\\s+", "");

			int seq = getSeq();

			if (formList.size() > 0) {
				Map<String, String> params = new HashMap<String, String>();

				FormTag f = (FormTag) formList.elementAt(0);

				NodeList inputs = f.getFormInputs();

				for (int i = 0; i < inputs.size(); i++) {
					String name = ((InputTag) inputs.elementAt(i)).getAttribute("name");
					String value = ((InputTag) inputs.elementAt(i)).getAttribute("value");

					if (StringUtils.isNotEmpty(name)) {
						params.put(name, StringUtils.defaultString(value));
					}
				}

				params.put("TaxYear", year);

				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
			}

			String header = "<table id=\"intermediaryResults\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"95%\">";

			if (tableList.size() > 0) {

				TableTag intermediary = (TableTag) tableList.elementAt(0);

				outputTable.append(intermediary.toHtml());

				TableRow[] rows = intermediary.getRows();

				for (int i = 0; i < rows.length; i++) {
					TableRow row = rows[i];
					if (row.getColumnCount() == 1) {
						String link = "";
						TableTag t = null;
						Span s = null;

						NodeList auxNodes = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);

						if (auxNodes.size() > 0) {
							t = (TableTag) auxNodes.elementAt(0);
						}

						auxNodes = row.getChildren().extractAllNodesThatMatch(new TagNameFilter("span"), true);

						String parcel = "";

						if (auxNodes.size() > 0) {
							s = (Span) auxNodes.elementAt(0);
							parcel = s.getAttribute("onclick");
							parcel = parcel.replaceAll("[^(]*\\(([^)]*)\\)", "$1").replaceAll("'", "").trim();

							link = CreatePartialLink(TSConnectionURL.idGET) + "CA_PropertyTaxParcelInfo.aspx?ParcelNo=" + parcel.replaceAll(" ", "%20")
									+ "&TaxYear=" + year;
						}

						if (t != null) {
							ParsedResponse currentResponse = new ParsedResponse();

							StringBuffer rowHtml = new StringBuffer("<tr><td>" + t.toHtml() + "</td><td><a href=" + link + ">" + parcel + "</a></td></tr>");

							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
							currentResponse.setOnlyResponse(rowHtml.toString());
							currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

							ResultMap m = ro.cst.tsearch.servers.functions.ALMontgomeryTR.parseIntermediaryRow(row, viParseId, parcel, t);
							m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
							m.put(TaxHistorySetKey.YEAR.getKeyName(), year);

							intermediaryData.put(parcel.replaceAll("[ -\\.]", ""), m);

							Bridge bridge = new Bridge(currentResponse, m, searchId);

							DocumentI document = bridge.importData();
							currentResponse.setDocument(document);

							intermediaryResponse.add(currentResponse);
						}
					}
				}
			}

			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
				parsedResponse.setHeader(header);
				parsedResponse.setFooter("\n</table><br>" + getPrevNext(response, nodes, seq));
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
		ro.cst.tsearch.servers.functions.ALMontgomeryTR.parseAndFillResultMap(response, detailsHtml, resultMap, intermediaryData);
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
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;M" }));
			moduleList.add(module);
		}
		serverInfo.setModulesForAutoSearch(moduleList);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();

		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
		String destinationPage = tsServerInfoModule.getDestinationPage();
		if (tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(1).setHtmlformat(getYearSelect("param_2_1", "param_2_1", destinationPage));
		}

		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		if (tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(1).setHtmlformat(getYearSelect("param_0_1", "param_0_1", destinationPage));
		}

		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if (tsServerInfoModule != null) {
			tsServerInfoModule.getFunction(1).setHtmlformat(getYearSelect("param_1_1", "param_1_1", destinationPage));
		}

		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}

	public String getYearSelect(String id, String name, String destinationPage) {
		String county = dataSite.getCountyName();
		getTaxYears(county, destinationPage);
		int lastTaxYear = taxYears.get("lastTaxYear" + county);
		int firstTaxYear = taxYears.get("firstTaxYear" + county);
		if (lastTaxYear <= 0 || firstTaxYear <= 0) {
			// No valid tax years.
			// This is going to happen when official site is down or it's going to change its layout.
			lastTaxYear = 2013;
			firstTaxYear = 1977;
		}

		// Generate input.
		StringBuilder select = new StringBuilder("<select id=\"" + id + "\" name=\"" + name + "\" size=\"1\">\n");
		for (int i = lastTaxYear; i >= firstTaxYear; i--) {
			select.append("<option ");
			select.append(i == lastTaxYear ? " selected " : "");
			select.append("value=\"" + i + "\">" + i + "</option>\n");
		}
		select.append("</select>");

		return select.toString();
	}

	/**
	 * Get tax year range from official site
	 */
	private void getTaxYears(String county, String destinationPage) {
		if (taxYears.containsKey("lastTaxYear" + county) && taxYears.containsKey("firstTaxYear" + county))
			return;

		// Get official site html response.
		String response = getLinkContents(dataSite.getServerHomeLink() + destinationPage);

		if (response != null) {
			HtmlParser3 parser = new HtmlParser3(response);
			Node selectList = parser.getNodeById("TaxYear");
			if (selectList == null) {
				// Unable to find the tax year select input.
				logger.error("Unable to parse tax years!");
				return;
			}

			// Get the first and last tax years.
			SelectTag selectTag = (SelectTag) selectList;
			OptionTag[] options = selectTag.getOptionTags();
			try {
				taxYears.put("lastTaxYear" + county, Integer.parseInt(options[0].getValue().trim()));
				taxYears.put("firstTaxYear" + county, Integer.parseInt(options[options.length - 1].getValue().trim()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
