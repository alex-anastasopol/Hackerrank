package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
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
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;

public class FLBrevardAO extends TSServer {

	private static final long	serialVersionUID	= 1L;
	
	private static final String PIN_FORMAT = "(\\w+)-(\\w+)-(\\w+)-(\\w+)-([\\w.]+)-([\\w.]+)";

	public FLBrevardAO(long searchId) {
		super(searchId);
	}

	public FLBrevardAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();

		try {

			TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SECTION_LAND_MODULE_IDX);

			if (hasPin()) {
				String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
				String cleanedPin = pin.replaceAll("[-,]", "");
				Matcher matcher = Pattern.compile(PIN_FORMAT).matcher(pin);
				if (pin.length() == 22 || matcher.find()) {
					
					String twp = "";
					String rng = "";
					String sec = "";
					String subn = "";
					String blk = "";
					String lot = "";
					
					if (cleanedPin.length() == 22) {		//PIN bootstrapped from NB, e.g. 253723DN00000.00020.00
						twp = cleanedPin.substring(0, 2);
						rng = cleanedPin.substring(2, 4);
						sec = cleanedPin.substring(4, 6);
						subn = cleanedPin.substring(6, 8);
						blk = cleanedPin.substring(8, 15);
						lot = cleanedPin.substring(15);
					} else {								//PIN from Search Page, e.g. 20G-34-02-AI-00001.0-0004.01
						twp = matcher.group(1);
						rng = matcher.group(2);
						sec = matcher.group(3);
						subn = matcher.group(4);
						blk = matcher.group(5);
						lot = matcher.group(6);
					}
					
					blk = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(blk);
					blk = blk.replaceAll("\\.0", "");

					lot = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(lot);
					lot = lot.replaceAll("(\\w+)(\\w\\w)", "$1.$2");
					lot = lot.replaceAll("\\.00", "");

					if (tsServerInfoModule != null) {
						PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
						for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
							String paramName = htmlControl.getCurrentTSSiFunc().getParamName();

							if ("twp".equals(paramName)) {
								htmlControl.setDefaultValue(twp);
							} else if ("rng".equals(paramName)) {
								htmlControl.setDefaultValue(rng);
							} else if ("sec".equals(paramName)) {
								htmlControl.setDefaultValue(sec);
							} else if ("subn".equals(paramName)) {
								htmlControl.setDefaultValue(subn);
							} else if ("blk".equals(paramName)) {
								htmlControl.setDefaultValue(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(blk));
							} else if ("lot".equals(paramName)) {
								htmlControl.setDefaultValue(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(lot));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return msiServerInfoDefault;
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		mSearch.setAdditionalInfo("viParseID", viParseID);

		if (rsResponse.contains("No records selected")) {
			Response.getParsedResponse().setError("No Results Found!");
			Response.getParsedResponse().setResponse("");
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_SECTION_LAND:
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_SUBDIVISION_NAME:
		case ID_SEARCH_BY_SUBDIVISION_PLAT:
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
			if (rsResponse.contains("Real Estate Records Search") && rsResponse.contains("Plat Book/Page")) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_SUBDIVISION_PLAT);
			} else if (StringUtils.containsIgnoreCase(sAction, "/Show_parcel.asp")) {
				ParseResponse(sAction, Response, ID_DETAILS);
			} else {
				ParseResponse(sAction, Response, ID_INTERMEDIARY);
			}
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
			StringBuilder detailsSB = new StringBuilder();
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			NodeList nodes = htmlParser3.getNodeList();
			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			if (tables.size() > 0) {
				String id = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(tables, "Parcel ID:", true), "", true)
						.replaceAll("<[^>]*>", "").trim();
				accountId.append(id);
			} else
				return null;

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}
			Node mainTable = htmlParser3.getNodeById("tContent");
			if (mainTable != null && mainTable instanceof TableTag) {
				TableRow[] rows = ((TableTag) mainTable).getRows();
				if (rows.length > 0) {
					TableColumn[] columns = rows[0].getColumns();
					if (columns.length > 1)
						detailsSB.append(columns[1].toHtml());
				}
			}
			String details = "<table align=center width=95% id=\"summaryTable\" style=\"max-width:95%\"><tr>"  + detailsSB.toString().replaceAll("(?ism)</?a [^>]*>", "") + "</tr></table>";
			return details;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String makeHeader(int viParseID) {
		String header = "<table id=\"intermediaryResults\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"95%\">";

		switch (viParseID) {
		case ID_SEARCH_BY_SUBDIVISION_PLAT:
			header += "<tr bgcolor=\"9d9ed9\">"
					+ "<th>Account</th>"
					+ "<th>Owner Name</th>"
					+ "<th>Property Address</th>"
					+ "<th>Parcel Id</th>"
					+ "<th>Plat Book/Page</th>"
					+ "</tr>";
			break;

		default:
			header += "<tr bgcolor=\"9d9ed9\">"
					+ "<th>Account</th>"
					+ "<th>Owner Name</th>"
					+ "<th>Property Address</th>"
					+ "<th>Parcel Id</th>"
					+ "</tr>";
			break;
		}

		return header;
	}

	private String getPrevNext(ServerResponse resp, NodeList nodes) {
		try {

			nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("form"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("action", "find_property.asp"));

			String nextLink = "";

			if (nodes.size() > 0) {
				FormTag form = (FormTag) nodes.elementAt(0);

				NodeList inputs = form.getFormInputs();

				StringBuffer params = new StringBuffer();

				for (int i = 0; i < inputs.size() - 1; i++) {
					InputTag in = (InputTag) inputs.elementAt(i);
					params.append(in.getAttribute("name") + "=" + in.getAttribute("value") + "&");
				}

				nextLink = CreatePartialLink(TSConnectionURL.idPOST) + form.getAttribute("action") + "?"
						+ params.toString().trim().replaceAll("&$", "").replaceAll(" ", "+");
			}

			boolean hasPrev = resp.getResult().toLowerCase().contains("<input type=\"button\" value=\"Prev\" onclick='history.back()'>".toLowerCase());

			String res = "<table id=prevNext width=20% cellspacing=0 align=left><tr><td align=right>" +
					(hasPrev ? "<a onclick=history.back()>Prev</a></td><td>" : "</td><td>") +
					(StringUtils.isNotEmpty(nextLink) ? "<a href=" + nextLink + ">Next</a>" : "") +
					"</td></tr></table>";

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

			NodeList tableList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("border", "1"), true);

			if (tableList.size() > 0) {
				TableTag resultsTable = (TableTag) tableList.elementAt(0);
				
				if (resultsTable != null) {
					TableRow[] rows = resultsTable.getRows();
					
					for (int i = 0; i < rows.length; i++) {
						TableRow row = rows[i];
						if (i==0) {
							if (row.getChildCount() == 13) {
								for (int idx = 4; idx >= 0; idx--)
									row.getChildren().remove(idx);
							}
						} else {
							int noOfCols = row.getColumnCount();
							if (noOfCols == 6 || noOfCols == 7) { //exception: @Search by Plat Bk_Pg, no of cols = 7!
								String link = "";
								if (row.getChildCount() >= 13) {
									for (int idx = 6; idx >= 0; idx--)
										row.getChildren().remove(idx);
								}
								TableColumn col = row.getColumns()[0];
								if (col.toHtml().contains("Aerial")) {
									row.getColumns()[0].getChildren().removeAll();
									row.removeChild(0);
								}
								
								col = row.getColumns()[0];
								if (col.getChildCount() > 0 && col.childAt(0) instanceof LinkTag) {
									LinkTag linkTag = (LinkTag) col.childAt(0);
									link = CreatePartialLink(TSConnectionURL.idGET) + linkTag.getLink();
									link = link.replace("&amp;", "&");
									linkTag.setLink(link);
								}

								

								ParsedResponse currentResponse = new ParsedResponse();
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
								currentResponse.setOnlyResponse(row.toHtml());
								currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

								ResultMap m = ro.cst.tsearch.servers.functions.FLBrevardAO.parseIntermediaryRow(row, viParseId);
								Bridge bridge = new Bridge(currentResponse, m, searchId);

								DocumentI document = (AssessorDocumentI) bridge.importData();
								currentResponse.setDocument(document);

								intermediaryResponse.add(currentResponse);
							}
						}
					}
					
					outputTable.append(resultsTable.toHtml());
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
		ro.cst.tsearch.servers.functions.FLBrevardAO.parseAndFillResultMap(response, detailsHtml, resultMap);
		return null;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		// pin
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SECTION_LAND_MODULE_IDX));
			module.clearSaKeys();

			String parcelno = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			Matcher matcher = Pattern.compile(PIN_FORMAT).matcher(parcelno);
			if (parcelno.contains(".")) {
				String cleanedParcelno = parcelno.replaceAll("[-,]", "");

				if (parcelno.length() == 22 || matcher.find()) {
					
					String twp = "";
					String rng = "";
					String sec = "";
					String subn = "";
					String blk = "";
					String lot = "";
					
					if (cleanedParcelno.length() == 22) {		//PIN bootstrapped from NB, e.g. 253723DN00000.00020.00
						twp = cleanedParcelno.substring(0, 2);
						rng = cleanedParcelno.substring(2, 4);
						sec = cleanedParcelno.substring(4, 6);
						subn = cleanedParcelno.substring(6, 8);
						blk = cleanedParcelno.substring(8, 15);
						lot = cleanedParcelno.substring(15);
					} else {								//PIN from Search Page, e.g. 20G-34-02-AI-00001.0-0004.01
						twp = matcher.group(1);
						rng = matcher.group(2);
						sec = matcher.group(3);
						subn = matcher.group(4);
						blk = matcher.group(5);
						lot = matcher.group(6);
					}

					blk = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(blk);
					blk = blk.replaceAll("\\.0", "");

					lot = ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(lot);
					lot = lot.replaceAll("(\\w+)(\\w\\w)", "$1.$2");
					lot = lot.replaceAll("\\.00", "");

					module.forceValue(0, twp);
					module.forceValue(1, rng);
					module.forceValue(2, sec);
					module.forceValue(3, subn);
					module.forceValue(4, blk);
					module.forceValue(5, lot);
					
					FilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
					module.addFilter(pinFilter);

					moduleList.add(module);
				}
			}

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
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			module.addFilter(addressFilter);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;" }));
			moduleList.add(module);
		}
		serverInfo.setModulesForAutoSearch(moduleList);

	}
}
