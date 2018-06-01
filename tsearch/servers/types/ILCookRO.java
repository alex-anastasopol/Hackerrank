package ro.cst.tsearch.servers.types;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.recoverdocument.ModuleShortDescription;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.log.SearchLogFactory;
import ro.cst.tsearch.log.SearchLogPage;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeSimpleFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.ExactDoctypeFilterResponse;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.Tidy;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.Image;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.ImageI.IType;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;

/**
 * 
 * @author Oprina George
 * 
 *         Nov 12, 2012
 */

@SuppressWarnings("deprecation")
public class ILCookRO extends TSServerROLike {

	public static final long	serialVersionUID	= 10000000L;

	public ILCookRO(long searchId) {
		super(searchId);
	}

	public ILCookRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	private FilterResponse getRemoveDeclarationsFilter() {
		ExactDoctypeFilterResponse filter = new ExactDoctypeFilterResponse(searchId);
		filter.addRejected("DECLARATION");
		return filter;
	}

	private FilterResponse getIntervalFilter() {
		BetweenDatesFilterResponse filter = new BetweenDatesFilterResponse(searchId);
		filter.setThreshold(new BigDecimal("0.90"));
		return filter;
	}

	private void setupGeneralFiltersAndValidators(TSServerInfoModule module) {
		module.addValidator(getIntervalFilter().getValidator());
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		if (getSearch().getSearchType() != Search.AUTOMATIC_SEARCH) {
			return;
		}

		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;

		// P1 : search by PIN
		for(String pin: getSearchAttributes().getPins(-1)){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(pin);
			setupGeneralFiltersAndValidators(module);
			modules.add(module);		
		}
		
		// Search instrument list search from AO

		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);

		InstrumentGenericIterator instrumentNoInterator = getInstrumentIterator();
		module.addIterator(instrumentNoInterator);
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);

		// add module only if we have something to search with
		if (!instrumentNoInterator.createDerrivations().isEmpty()) {
			setupGeneralFiltersAndValidators(module);
			modules.add(module);
		}

		// Search for cross reference instruments from RO
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_SIMULATE_CROSSREF, Boolean.TRUE);
		module.setSaObjKey(SearchAttributes.RO_CROSS_REF_INSTR_LIST);
		module.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST_NOT_AGAIN);
		module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
		setupGeneralFiltersAndValidators(module);
		module.addFilter(getRemoveDeclarationsFilter());
		modules.add(module);

		// OCR last transfer - instrument search
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
		setupGeneralFiltersAndValidators(module);
		module.addFilter(getRemoveDeclarationsFilter());
		modules.add(module);

		// set list for automatic search
		serverInfo.setModulesForAutoSearch(modules);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		if (viParseID != ID_SAVE_TO_TSD && viParseID != ID_DETAILS)
			mSearch.setAdditionalInfo("viParseID", viParseID);
		
		if (rsResponse.contains("No records selected") || rsResponse.contains("Search criteria resulted in 0 hits.")) {
			Response.getParsedResponse().setError("No Results Found!");
			Response.getParsedResponse().setResponse("");
			return;
		}
		
		if (rsResponse.contains("Your search has reached the configured timeout period.  Please narrow your search criteria by clicking on")) {
			Response.getParsedResponse().setError("Your search has reached the configured timeout period.  Please narrow your search criteria!");
			Response.getParsedResponse().setResponse("");
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_SEARCH_BY_TAX_BIL_NO:
		case ID_SEARCH_BY_SUBDIVISION_PLAT:
		case ID_SEARCH_BY_SUBDIVISION_NAME:
		case ID_SEARCH_BY_BOOK_AND_PAGE:
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
				String sSave2TSDLink = "";
				try {
					sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + URLDecoder.decode(originalLink, "ASCII");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				if (isInstrumentSaved(accountName, null, data, false)) {
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
				Response.getParsedResponse().setResponse(details.replaceAll("(?ism)</?a[^>]*>", ""));

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;
		case ID_GET_LINK:
			ParseResponse(sAction, Response, rsResponse.contains("DocDetails1_ContentContainer1") ? ID_DETAILS : ID_INTERMEDIARY);
			break;
		default:
			break;
		}
	}
	
	protected void logSearchBy(TSServerInfoModule module, Map<String, String> params) {

		if (module.isVisible() || "GB_MANAGER_OBJECT".equals(module.getSaObjKey())) {// B 4511

			// get parameters formatted properly
			Map<String, String> moduleParams = params;
			if (moduleParams == null) {
				moduleParams = module.getParamsForLog();
			}
			Search search = getSearch();
			// determine whether it's an automatic search
			boolean automatic = (search.getSearchType() != Search.PARENT_SITE_SEARCH)
					|| (GPMaster.getThread(searchId) != null);
			boolean imageSearch = module.getLabel().equalsIgnoreCase("image search") ||
					module.getModuleIdx() == TSServerInfo.IMG_MODULE_IDX;

			// create the message
			StringBuilder sb = new StringBuilder();
			SearchLogFactory sharedInstance = SearchLogFactory.getSharedInstance();
			SearchLogPage searchLogPage = sharedInstance.getSearchLogPage(searchId);
			sb.append("</div>");

			Object additional = GetAttribute("additional");
			if (Boolean.TRUE != additional) {
				searchLogPage.addHR();
				sb.append("<hr/>");
			}
			int fromRemoveForDB = sb.length();

			// searchLogPage.
			sb.append("<span class='serverName'>");
			String serverName = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName();
			sb.append(serverName);
			sb.append("</span> ");

			sb.append(automatic ? "automatic" : "manual");
			Object info = module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION);
			if (StringUtils.isNotEmpty(module.getLabel())) {

				if (info != null) {
					sb.append(" - " + info + "<br>");
				}
				sb.append(" <span class='searchName'>");
				sb.append(module.getLabel());
			} else {
				sb.append(" <span class='searchName'>");
				if (info != null) {
					sb.append(" - " + info + "<br>");
				}
			}
			sb.append("</span> by ");

			boolean firstTime = true;
			for (Entry<String, String> entry : moduleParams.entrySet()) {
				String value = entry.getValue();
				value = value.replaceAll("(, )+$", "");
				
				if ("Document Type:".equals(entry.getKey())) {
					value = StringUtils.defaultString(ro.cst.tsearch.servers.functions.ILCookRO.docTypeSelect.get(value));
				}
				
				if (!firstTime) {
					sb.append(", ");
				} else {
					firstTime = false;
				}
				sb.append(entry.getKey().replaceAll("&lt;br&gt;", "") + " = <b>" + value + "</b>");
			}
			int toRemoveForDB = sb.length();
			// log time when manual is starting
			if (!automatic || imageSearch) {
				sb.append(" ");
				sb.append(SearchLogger.getTimeStamp(searchId));
			}
			sb.append(":<br/>");

			// log the message
			SearchLogger.info(sb.toString(), searchId);
			ModuleShortDescription moduleShortDescription = new ModuleShortDescription();
			moduleShortDescription.setDescription(sb.substring(fromRemoveForDB, toRemoveForDB));
			moduleShortDescription.setSearchModuleId(module.getModuleIdx());
			search.setAdditionalInfo(TSServerInfoConstants.TS_SERVER_INFO_MODULE_DESCRIPTION, moduleShortDescription);
			String user = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getAttribute(1).toString();
			SearchLogger.info(ro.cst.tsearch.utils.StringUtils.createCollapsibleHeader(), searchId);
			searchLogPage.addModuleSearchParameters(serverName, additional, info, moduleParams, module.getLabel(), automatic, imageSearch, user);
		}

	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			int numberOfUncheckedElements = 0;
			Integer viParseId = (Integer) mSearch.getAdditionalInfo("viParseID");

			/**
			 * We need to find what was the original search module in case we need some info from it like in the new PS interface
			 */
			TSServerInfoModule moduleSource = null;
			Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
			if (objectModuleSource != null) {
				if (objectModuleSource instanceof TSServerInfoModule) {
					moduleSource = (TSServerInfoModule) objectModuleSource;
				}
			} else {
				objectModuleSource = getSearch().getAdditionalInfo(this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
				if (objectModuleSource instanceof TSServerInfoModule) {
					moduleSource = (TSServerInfoModule) objectModuleSource;
				}
			}

			ParsedResponse parsedResponse = response.getParsedResponse();
			String rsResponse = response.getResult();

			NodeList nodes = new HtmlParser3(rsResponse.replaceAll("(?ism)&nbsp;", " ")).getNodeList();

			NodeList divs = nodes.extractAllNodesThatMatch(new TagNameFilter("div"), true);

			NodeList auxNodes = divs.extractAllNodesThatMatch(new HasAttributeFilter("id", "DocList1_ContentContainer1"));

			outputTable.append(rsResponse);

			if (auxNodes.size() > 0) {
				// intermediary results
				if (auxNodes.elementAt(0) instanceof Div) {
					Div auxD = (Div) auxNodes.elementAt(0);

					auxNodes = auxD.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "DocList1_GridView_Document"));

					if (auxNodes.size() > 0) {
						TableTag t = (TableTag) auxNodes.elementAt(0);

						TableRow[] rows = t.getRows();

						int index = -1;

						if (rows[0].getChildCount() == 9) {
							if (rows[0].getChild(1).toPlainTextString().contains("Recorded Date")) {
								index = 1; // name search
							}
						} else if (rows[0].getChildCount() == 8) {
							if (rows[0].getChild(1).toPlainTextString().contains("Grantor")) {
								index = 0; // pin search
							} else if (rows[0].getChild(1).toPlainTextString().contains("Doc")) {
								index = 2; // trust search
							}
						} else if (rows[0].getChildCount() == 5) {
							if (rows[0].getChild(1).toPlainTextString().contains("Doc")) {
								index = 3; // instrument number search
							} else if (rows[0].getChild(1).toPlainTextString().contains("Legal")) {
								index = 4; // legal search
							} else if (rows[0].getChild(1).toPlainTextString().contains("Date")) {
								index = 5; // doctype search
							}
						}

						for (int i = 1; i < rows.length; i++) {
							TableRow row = rows[i];
							if (row.getColumnCount() == 3 || row.getColumnCount() == 6 || row.getColumnCount() == 7 || row.getColumnCount() == 8) {

								TableColumn[] cols = row.getColumns();

								ResultMap map = ro.cst.tsearch.servers.functions.ILCookRO.parseIntermediaryRow(row, index);
								map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

								// index link
								String linkPrefix = CreatePartialLink(TSConnectionURL.idPOST);
								String link = "";
								String docLink = "";

								int numberOfCols = row.getColumnCount();
								for (int c = 0; c < numberOfCols; c++) {
									if (cols[c].getChildren().size() > 0) {
										Node possibleLink = row.getColumns()[c].getChild(0);
										if (possibleLink instanceof LinkTag){
											LinkTag linkTag = (LinkTag) possibleLink;
											docLink = linkTag.getLink().replace("&amp;", "&").replaceFirst("\\?", "&");
											docLink = docLink.split(",")[0].replaceAll(".*'([^']*)'.*", "$1");
											docLink = "i2/default.aspx?detailsSearch=true&EVENTTARGET=" + URLEncoder.encode(docLink, "ASCII");
	
											String lnk = linkPrefix + docLink;
											if (lnk.contains("ButtonRow_Doc")){
												link = lnk;
											}
											linkTag.setLink(lnk);
										}
									}
								}
//								switch (index) {
//								case 0:
//									if (cols[5].getChildren().size() > 0) {
//										LinkTag linkTag = (LinkTag) row.getColumns()[5].getChild(0);
//
//										docLink = linkTag.getLink().replace("&amp;", "&").replaceFirst("\\?", "&");
//										docLink = docLink.split(",")[0].replaceAll(".*'([^']*)'.*", "$1");
//										docLink = "i2/default.aspx?detailsSearch=true&EVENTTARGET=" + URLEncoder.encode(docLink, "ASCII");
//
//										link = linkPrefix + docLink;
//
//										linkTag.setLink(link);
//									}
//									break;
//								case 1:
//									if (cols[1].getChildren().size() > 0) {
//										LinkTag linkTag = (LinkTag) row.getColumns()[1].getChild(0);
//
//										docLink = linkTag.getLink().replace("&amp;", "&").replaceFirst("\\?", "&");
//										docLink = docLink.split(",")[0].replaceAll(".*'([^']*)'.*", "$1");
//										docLink = "i2/default.aspx?detailsSearch=true&EVENTTARGET=" + URLEncoder.encode(docLink, "ASCII");
//
//										link = linkPrefix + docLink;
//
//										linkTag.setLink(link);
//									}
//									break;
//								case 2:
//									if (cols[3].getChildren().size() > 0) {
//										LinkTag linkTag = (LinkTag) row.getColumns()[3].getChild(0);
//
//										docLink = linkTag.getLink().replace("&amp;", "&").replaceFirst("\\?", "&");
//										docLink = docLink.split(",")[0].replaceAll(".*'([^']*)'.*", "$1");
//										docLink = "i2/default.aspx?detailsSearch=true&EVENTTARGET=" + URLEncoder.encode(docLink, "ASCII");
//
//										link = linkPrefix + docLink;
//
//										linkTag.setLink(link);
//									}
//									break;
//								case 3:
//									if (cols[0].getChildren().size() > 0) {
//										LinkTag linkTag = (LinkTag) row.getColumns()[0].getChild(0);
//
//										docLink = linkTag.getLink().replace("&amp;", "&").replaceFirst("\\?", "&");
//										docLink = docLink.split(",")[0].replaceAll(".*'([^']*)'.*", "$1");
//										docLink = "i2/default.aspx?detailsSearch=true&EVENTTARGET=" + URLEncoder.encode(docLink, "ASCII");
//
//										link = linkPrefix + docLink;
//
//										linkTag.setLink(link);
//									}
//									break;
//								case 5:
//									if (cols[2].getChildren().size() > 0) {
//										LinkTag linkTag = (LinkTag) row.getColumns()[2].getChild(0);
//
//										docLink = linkTag.getLink().replace("&amp;", "&").replaceFirst("\\?", "&");
//										docLink = docLink.split(",")[0].replaceAll(".*'([^']*)'.*", "$1");
//										docLink = "i2/default.aspx?detailsSearch=true&EVENTTARGET=" + URLEncoder.encode(docLink, "ASCII");
//
//										link = linkPrefix + docLink;
//
//										linkTag.setLink(link);
//									}
//									break;
//								default:
//									break;
//								}

								String cleanRow = row.toHtml().replaceAll("(?ism)<a[^>]*doPostBack[^>]*>([^<]*)</a>", "$1");

								String checkBox = "";

								ParsedResponse currentResponse = new ParsedResponse();

								Bridge bridge = new Bridge(currentResponse, map, searchId);

								DocumentI document = (RegisterDocumentI) bridge.importData();
								currentResponse.setDocument(document);

								HashMap<String, String> data = new HashMap<String, String>();
								data.put("type", document.getDocType());
								data.put("docno", document.getDocno());

								if (index != 4) {
									if (isInstrumentSaved(document.getDocno(), null, data, false)
											&& !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
										checkBox = "saved";
									} else {
										numberOfUncheckedElements++;
										checkBox = "<input type='checkbox' name='docLink' value='" + link + "'>";
										if (getSearch().getInMemoryDoc(docLink) == null) {
											getSearch().addInMemoryDoc(docLink, currentResponse);
										}
										
										link = URLDecoder.decode(link,"ASCII");
										currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

										/**
										 * Save module in key in additional info. The key is instrument number that should be always available.
										 */
										String keyForSavingModules = this
												.getKeyForSavingInIntermediary(getInstrumentNumberForSavingInFinalResults(document));
										getSearch().setAdditionalInfo(keyForSavingModules, moduleSource);
									}

									cleanRow = "<tr><td width=5%>" + checkBox + "</td>"
											+ cleanRow.replaceAll("(?ism)<tr[^>]*>", "").replaceAll("(?ism)</tr>", "")
											+ "</tr>";

								}

								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, cleanRow.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"));
								currentResponse.setOnlyResponse(cleanRow);

								intermediaryResponse.add(currentResponse);
							}
						}

						if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
							parsedResponse.setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST")
									+ makeHeader(t, viParseId, index != 4 ? true : false));
							parsedResponse.setFooter("\n</table><br>" + getPrevNext(nodes)
									+ CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseId, (Integer) numberOfUncheckedElements));
						} else {
							parsedResponse.setHeader("<table border=\"1\">");
							parsedResponse.setFooter("</table>");
						}

						SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
					}
				}

			} else {
				auxNodes = divs.extractAllNodesThatMatch(new HasAttributeFilter("id", "NameList1_ContentContainer1"));

				if (auxNodes.size() > 0) {
					// intermediary results to other intermediary results

					Div auxD = (Div) auxNodes.elementAt(0);

					auxNodes = auxD.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "NameList1_GridView_NameListGroup"));

					if (auxNodes.size() == 0) {
						auxNodes = auxD.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "NameList1_GridView_TrustGroup"));
					}

					if (auxNodes.size() > 0) {
						TableTag t = (TableTag) auxNodes.elementAt(0);

						TableRow[] rows = t.getRows();

						for (int i = 1; i < rows.length; i++) {
							TableRow row = rows[i];
							if (row.getColumnCount() == 4) {
								TableColumn[] cols = row.getColumns();

								ResultMap map = new ResultMap();
								map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

								String name = cols[0].toPlainTextString();

								ArrayList<String> names = new ArrayList<String>();
								names.add(name);

								ro.cst.tsearch.servers.functions.ILCookRO.parseNames(map, names, "GrantorSet");

								// index link
								String linkPrefix = CreatePartialLink(TSConnectionURL.idPOST);
								String docLink = "";

								if (row.getColumns()[1].getChildren().size() > 0 && row.getColumns()[1].getChild(0) instanceof LinkTag) {
									LinkTag linkTag = (LinkTag) row.getColumns()[1].getChild(0);

									docLink = linkTag.getLink().replace("&amp;", "&").replaceFirst("\\?", "&");

									docLink = docLink.split(",")[0].replaceAll(".*'([^']*)'.*", "$1");

									linkTag.setLink(linkPrefix + getDataSite().getServerHomeLink() + "i2/default.aspx?intermediarySearch=true&EVENTTARGET="
											+ docLink);
								}

								if (row.getColumns()[2].getChildren().size() > 0 && row.getColumns()[2].getChild(0) instanceof LinkTag) {
									LinkTag linkTag = (LinkTag) row.getColumns()[2].getChild(0);

									docLink = linkTag.getLink().replace("&amp;", "&").replaceFirst("\\?", "&");

									docLink = docLink.split(",")[0].replaceAll(".*'([^']*)'.*", "$1");

									linkTag.setLink(linkPrefix + getDataSite().getServerHomeLink() + "i2/default.aspx?intermediarySearch=true&EVENTTARGET="
											+ docLink);
								}

								if (row.getColumns()[3].getChildren().size() > 0 && row.getColumns()[3].getChild(0) instanceof LinkTag) {
									LinkTag linkTag = (LinkTag) row.getColumns()[3].getChild(0);

									docLink = linkTag.getLink().replace("&amp;", "&").replaceFirst("\\?", "&");

									docLink = docLink.split(",")[0].replaceAll(".*'([^']*)'.*", "$1");

									linkTag.setLink(linkPrefix + getDataSite().getServerHomeLink() + "i2/default.aspx?intermediarySearch=true&EVENTTARGET="
											+ docLink);
								}

								String cleanRow = row.toHtml();

								ParsedResponse currentResponse = new ParsedResponse();
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, cleanRow);
								currentResponse.setOnlyResponse(cleanRow);
								// currentResponse.setPageLink(new LinkInPage(docLink, docLink, TSServer.REQUEST_GO_TO_LINK));

								Bridge bridge = new Bridge(currentResponse, map, searchId);

								DocumentI document = (RegisterDocumentI) bridge.importData();
								currentResponse.setDocument(document);

								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, cleanRow.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"));
								currentResponse.setOnlyResponse(cleanRow);

								intermediaryResponse.add(currentResponse);
							}
						}

						if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
							parsedResponse.setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST")
									+ makeHeader(t, viParseId, false));
							parsedResponse.setFooter("\n</table><br>" + getPrevNext(nodes)
									+ CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseId, (Integer) numberOfUncheckedElements));
						} else {
							parsedResponse.setHeader("<table border=\"1\">");
							parsedResponse.setFooter("</table>");
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return intermediaryResponse;
	}

	private String getPrevNext(NodeList nodes) {
		try {
			NodeList auxNodes = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true).extractAllNodesThatMatch(
					new HasAttributeFilter("id", "DocList1_ctl07"));
			
			if (auxNodes.size()==0) {
				auxNodes = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true).extractAllNodesThatMatch(
						new HasAttributeFilter("id", "NameList1_ctl01"));
			}

			if (auxNodes.size() > 0) {
				TableTag t = (TableTag) auxNodes.elementAt(0);

				auxNodes = t.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);

				for (int i = 0; i < auxNodes.size(); i++) {
					LinkTag l = (LinkTag) auxNodes.elementAt(i);
					String link = l.getLink().replace("&amp;", "&").replaceFirst("\\?", "&");
					link = link.split(",")[0].replaceAll(".*'([^']*)'.*", "$1");

					l.removeAttribute("class");
					l.removeAttribute("style");

					l.setLink(CreatePartialLink(TSConnectionURL.idPOST) + getDataSite().getServerHomeLink()
							+ "i2/default.aspx?intermediarySearch=true&EVENTTARGET=" + link);
				}

				t.removeAttribute("style");
				t.setAttribute("width", "30%");
				t.setAttribute("align", "center");

				return t.toHtml().replaceAll("[Â]", "").replaceAll("(?ism)<a[^>]*disabled[^>]*>([^<]*)</a>", "$1") + "<br><br>";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String makeHeader(TableTag resultsTable, int viParseID, boolean addCheckAll) {
		if (resultsTable == null)
			return "";

		String header = "<table id=\"intermediaryResults\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"50%\">";

		TableRow r = resultsTable.getRow(0);

		if (addCheckAll) {
			header = "<table id=\"intermediaryResults\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"80%\">";
			header += r.toHtml().replaceAll("(?ism)<input[^>]*>", "").replaceFirst("(?ism)(<tr[^>]*>)", "$1<td>" + SELECT_ALL_CHECKBOXES + "</td>");
		} else {
			header += r.toHtml().replaceAll("(?ism)<input[^>]*>", "");
			;
		}

		header = header.replaceAll("</?a[^>]*>", "");

		return header.replaceAll("</?a[^>]*>", "");
	}

	protected String getDetails(ServerResponse response, String rsResponse, StringBuilder accountId, HashMap<String, String> data) {
		try {
			NodeList nodes = new HtmlParser3(Tidy.tidyParse(
					rsResponse.replaceAll("(?ism)&nbsp;", " ").replaceAll("(?ism)<th", "<td").replaceAll("(?ism)</th", "</td"), null)).getNodeList();

			NodeList divs = nodes.extractAllNodesThatMatch(new TagNameFilter("div"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "DocDetails1_WidgetContainer"));

			if (divs.size() == 0) {
				divs = nodes.extractAllNodesThatMatch(new TagNameFilter("div"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "DocDetails1_ContentContainer1"));
			}

			if (divs.size() > 0) {
				Div d = (Div) divs.elementAt(0);

				NodeList auxNodes = null;

				auxNodes = d.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "DocDetails1_GridView_Details_ctl02_ctl00"), true);

				if (auxNodes.size() > 0) {
					String docno = auxNodes.elementAt(0).toPlainTextString();
					data.put("docno", docno);
					accountId.append(docno);
				}
				
				auxNodes = d.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "DocDetails1_GridView_Details_ctl02_ctl01"), true);
				String year = "";
				if (auxNodes.size() > 0) {
					String instrumentDate = auxNodes.elementAt(0).toPlainTextString();
					if (StringUtils.isNotBlank(instrumentDate)){
						year = instrumentDate.substring(instrumentDate.lastIndexOf("/") + 1);
					}
				}
				if (year.length() < 4){
					auxNodes = d.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "DocDetails1_GridView_Details_ctl02_ctl02"), true);
					if (auxNodes.size() > 0) {
						String recordedDate = auxNodes.elementAt(0).toPlainTextString();
						if (StringUtils.isNotBlank(recordedDate)){
							year = recordedDate.substring(recordedDate.lastIndexOf("/") + 1);
						}
					}
				}
				
				auxNodes = d.getChildren().extractAllNodesThatMatch(new HasAttributeFilter("id", "DocDetails1_GridView_Details_ctl02_ctl03"), true);

				if (auxNodes.size() > 0) {
					String doctype = auxNodes.elementAt(0).toPlainTextString();
					data.put("type", doctype);
				}
				String imageLink = getDataSite().getServerHomeLink() + "i2/default.aspx?imageSearch=true&docno=" + data.get("docno") + "&year=" + year;
				
				if (rsResponse.contains("View Images")){
					response.getParsedResponse().addImageLink(new ImageLinkInPage(imageLink, accountId.toString() + ".tiff"));
				}

				/* If from memory - use it as is */
				if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "updatePanel|UpdatePanelRefreshButtons")) {
					return rsResponse;
				}

				// fake related links
				auxNodes = d.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "DocDetails1_GridView_Referance"));

				if (auxNodes.size() > 0) {
					auxNodes = auxNodes.extractAllNodesThatMatch(new TagNameFilter("a"), true);

					for (int i = 0; i < auxNodes.size(); i++) {
						LinkTag l = (LinkTag) auxNodes.elementAt(i);
						String link = l.getLink().replace("&amp;", "&").replaceFirst("\\?", "&");
						link = link.split(",")[0].replaceAll(".*'([^']*)'.*", "$1");

						l.setLink(CreatePartialLink(TSConnectionURL.idPOST) + getDataSite().getServerHomeLink()
								+ "i2/default.aspx?detailsSearch=true&EVENTTARGET=" + link);
					}
				}

				String res = "<table align=center width=95% id=summaryTable><tr><td>"
						+ d.toHtml().replaceAll("(?sim)class=\"[^\"]*\"", "").replaceAll("(?sim)style=\"[^\"]*\"", "") + "</td></tr>";
				if (rsResponse.contains("View Images")){
					res += "<tr><td><a href=" + CreatePartialLink(TSConnectionURL.idPOST) + imageLink + ">View Image</a></td></tr>";
				}
				res += "</table>";

				res = res.replaceAll("(?is)<div\\s+id=\"DocDetails1_ToolBarContainer1\"\\s*>.*?</div>", "");
				
				// remove links
				res = res.replaceAll("(?ism)<a[^>]*DocDetails1_GridView_LegalDescription_[^>]*>([^<]*)</a>", "$1")
						.replaceAll("(?ism)<a[^>]*DocDetails1_GridView_Property_[^>]*>([^<]*)</a>", "$1")
						.replaceAll("(?ism)<a[^>]*DocDetails1_ButAddToBasket[^>]*>[^<]*</a>", "")
						.replaceAll("(?ism)<a[^>]*DocDetails1_PrintDocBut[^>]*>[^<]*</a>", "")
						.replaceAll("(?ism)<a[^>]*DocDetails1_GridView_Grantor_[^>]*>([^<]*)</a>", "$1")
						.replaceAll("(?ism)<a[^>]*DocDetails1_GridView_Grantee_[^>]*>([^<]*)</a>", "$1");

				return res;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		ro.cst.tsearch.servers.functions.ILCookRO.parseAndFillResultMap(response, detailsHtml, resultMap);
		return null;
	}

	private InstrumentGenericIterator getInstrumentIterator() {
		InstrumentGenericIterator iterator = new InstrumentGenericIterator(searchId) {
			private static final long	serialVersionUID	= 1L;

			@Override
			protected String cleanInstrumentNo(String inst, int year) {
				return inst;
			}

			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				if (StringUtils.isNotEmpty(state.getInstno())) {
					if (filterCriteria != null) {
						filterCriteria.put("InstrumentNumber", state.getInstno());
					}
				}
				return state.getInstno();
			}
		};
		return iterator;
	}

	@Override
    public ServerResponse GetLink(String vsRequest, boolean vbEncodedOrIsParentSite) throws ServerResponseException {
    	    	
    	String link = ro.cst.tsearch.utils.StringUtils.extractParameter(vsRequest, "Link=(.*)");
    	String imageSearch = ro.cst.tsearch.utils.StringUtils.extractParameter(vsRequest, "imageSearch=([^&?]*)");
    	String docno = ro.cst.tsearch.utils.StringUtils.extractParameter(vsRequest, "docno=([^&?]*)");
    	if ("true".equals(imageSearch) && !StringUtils.isEmpty(docno)) {
    		return GetImageLink(link, docno, vbEncodedOrIsParentSite);
    	}
    	return super.GetLink(vsRequest, vbEncodedOrIsParentSite); 
    }
	
	public ServerResponse GetImageLink(String link, String name, boolean writeImageToClient) throws ServerResponseException {
    	
		String folderName = getImageDirectory() + File.separator + searchId + File.separator;
		new File(folderName).mkdirs();
    	
		String fileName = folderName + name + ".tiff";
		boolean existTiff = FileUtils.existPath(fileName);
		byte[] imageBytes = null;
		
		if(!existTiff){
			String docno = ro.cst.tsearch.utils.StringUtils.extractParameter(link, "docno=([^&?]*)");
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				imageBytes = ((ro.cst.tsearch.connection.http2.ILCookRO)site).downloadImage(docno, fileName);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(site);
			}
		}
		
		ServerResponse resp = new ServerResponse();
		if (imageBytes != null) {
			resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, "image/tiff"));
	
			if (!ro.cst.tsearch.utils.FileUtils.existPath(fileName)) {
				FileUtils.writeByteArrayToFile(resp.getImageResult().getImageContent(), fileName);
			}
		}
		
		existTiff = FileUtils.existPath(fileName);
			
    	// write the image to the client web-browser
		boolean imageOK = false;
		if (existTiff){
			imageOK = writeImageToClient(fileName, "image/tiff");
		} 
		
		// image not retrieved
		if(!imageOK){ 
	        // return error message
			ParsedResponse pr = new ParsedResponse();
			pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
			throw new ServerResponseException(pr);			
		}
		// return solved response
		return ServerResponse.createSolvedResponse();  
    }
    
    protected static boolean retrieveImage(String docno, String fileName, int miServerId, long searchId) {
    	
    	byte[] imageBytes = null;

		HttpSite site = HttpManager.getSite("ILCookRO", searchId);
		try {
			imageBytes = ((ro.cst.tsearch.connection.http2.ILCookRO)site).getImage(docno, fileName);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}

		ServerResponse resp = new ServerResponse();

		if (imageBytes == null) {
			return false;
		}

		resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, "image/tiff"));

		if (!ro.cst.tsearch.utils.FileUtils.existPath(fileName)) {
			FileUtils.writeByteArrayToFile(resp.getImageResult().getImageContent(), fileName);
		}

		return true;
    }
	
	@Override
    protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
    	
		String link = image.getLink().replaceFirst("Link=", "");
		String docno = ro.cst.tsearch.utils.StringUtils.extractParameter(link, "docno=([^&?]*)");
		String year = ro.cst.tsearch.utils.StringUtils.extractParameter(link, "year=([^&?]*)");
    	String fileName = image.getPath();
    	if (ILCookImageRetriever.INSTANCE.retrieveImage(docno, fileName, "", year, searchId)) {
    		byte[] b = FileUtils.readBinaryFile(fileName);
    		return new DownloadImageResult(DownloadImageResult.Status.OK, b, image.getContentType());
    	}
    	
    	return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
    }

	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
	}

	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if (restoreDocumentDataI == null) {
			return null;
		}
		TSServerInfoModule module = null;

		if (StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());

			DocTypeSimpleFilter docTypeSimpleFilter = new DocTypeSimpleFilter(getSearch().getID());
			docTypeSimpleFilter.setDocTypes(new String[] { restoreDocumentDataI.getCategory() });
			module.addFilter(docTypeSimpleFilter);

		}
		return module;
	}

	public Object getImageDownloader(RestoreDocumentDataI document) {
		return getRecoverModuleFrom(document);
	}
	
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	if(StringUtils.isEmpty(instrumentNo))
    		return false;
    	
    	/* The AO,TR document must be re-saved in date down searches: Bug 4584 */
    	if(mSearch.getSa().isDateDown() && isAssessorOrTaxServer()) {
    		return false;
    	}
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			if(documentManager.getDocument(documentToCheck.getInstrument()) != null) {
    				return true;
    			} else if(!checkMiServerId) {
    				List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheck.getInstrument());
    				if(almostLike != null && !almostLike.isEmpty()) {
    					return true;
    				} else {
    					documentToCheck.setInstno(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(documentToCheck.getInstno()));
    					almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheck.getInstrument());
    					if(almostLike != null && !almostLike.isEmpty()) {
	    					return true;
	    				}
    				}
    			}
    		} else {
	    		InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		if(data != null) {
		    		if(!StringUtils.isEmpty(data.get("type"))) {
		        		String serverDocType = data.get("type");
		    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
		            	instr.setDocType(docCateg);
		            	instr.setDocSubType(DocumentTypes.getDocumentCategory("MISC", searchId));
		    		}
		    		
		    		instr.setBook(data.get("book"));
		    		instr.setPage(data.get("page"));
		    		instr.setDocno(data.get("docno"));
	    		}
	    		
	    		try {
	    			instr.setYear(Integer.parseInt(data.get("year")));
	    		} catch (Exception e) {}
	    		
	    		if(documentManager.getDocument(instr) != null) {
	    			return true;
	    		} else {
	    			List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    			
	    			if(checkMiServerId) {
		    			boolean foundMssServerId = false;
	    				for (DocumentI documentI : almostLike) {
	    					if(miServerID==documentI.getSiteId()){
	    						foundMssServerId  = true;
	    						break;
	    					}
	    				}
		    			
	    				if(!foundMssServerId){
	    					return false;
	    				}
	    			}
	    			
	    			if(almostLike.isEmpty()){
	    				instr.setInstno(ro.cst.tsearch.utils.StringUtils.removeLeadingZeroes(instr.getInstno()));
	    				almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    			}
	    			
    				if(data!=null) {
    					if(!StringUtils.isEmpty(data.get("type"))){
			        		String serverDocType = data.get("type"); 
			    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
			    	    	String dataSource = data.get("dataSource");
			    	    	for (DocumentI documentI : almostLike) {
			    	    		if (serverDocType.equals("ASSESSOR") && dataSource != null) {
									if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))//B 4435, must save NDB and ISI doc of the same instrNo
										return true;
			    	    		} else if (serverDocType.equals("CNTYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		} else if (serverDocType.equals("CITYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		}else if( (!checkMiServerId || miServerID==documentI.getSiteId()) && documentI.getDocType().equals(docCateg)){
									return true;
			    	    		}
							}	
    					}
		    		} else {
		    			EmailClient email = new EmailClient();
		    			email.addTo(MailConfig.getExceptionEmail());
		    			email.setSubject("isInstrumentNoSaved problem on " + URLMaping.INSTANCE_DIR + this.getClass().getName());
		    			email.addContent("We should at least have type!!!!\nSearchId=" + searchId);
		    			email.sendAsynchronous();
		    		}
	    		}
    		}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	return false;
    }
	
	@Override
	protected void setCertificationDate() {
		try {
			
			if (!CertificationDateManager.isCertificationDateInCache(dataSite)){
				
				String html = "";
				HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
				try{
					html = site.process(new HTTPRequest(dataSite.getLink())).getResponseAsString();
		    	} catch(RuntimeException e){
		    		e.printStackTrace();
		    	} finally {
		    		HttpManager.releaseSite(site);
		    	}   
		        	
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(html, null);
				NodeList mainList = htmlParser.parse(null);
				String plainText = HtmlParser3.getNodeByID("SearchInfo1_ACSLabel_LastRecordDoc", mainList, true).toPlainTextString();
				
				Pattern cdPattern = Pattern.compile("(\\d{2})-(\\d{2})-(\\d{4})");
				Matcher cdMatcher = cdPattern.matcher(plainText);
				if(cdMatcher.find()) {
					CertificationDateManager.cacheCertificationDate(dataSite, 
							cdMatcher.group(1) + "/" + cdMatcher.group(2) + "/" + cdMatcher.group(3));
				} else {
					CertificationDateManager.getLogger().error(
							"Error setting certification date on " + getDataSite().getName() + " because plainText is " + plainText);
				}
				
			}
			String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
			getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			

        } catch (Exception e) {
        	CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName(), e);
        }
	}
	
	@Override
	public void addDocumentAdditionalPostProcessing(DocumentI doc, ServerResponse response){
		super.addDocumentAdditionalPostProcessing(doc, response);

		DocumentsManagerI manager = getSearch().getDocManager();
		try {
			manager.getAccess();
			if (manager.contains(doc)) {
				if (doc instanceof RegisterDocumentI){
					RegisterDocumentI regDoc = (RegisterDocumentI)doc;
					
					if (!regDoc.hasImage()){
						StringBuilder toLog = new StringBuilder();
						try {
							TSInterface tsi = TSServersFactory.GetServerInstance((int)doc.getSiteId(), searchId);
							DownloadImageResult res = tsi.lookupForImage(doc);
							if (res.getStatus() != DownloadImageResult.Status.OK){
								if (doc.getImage() != null){
									doc.getImage().setSaved(false);
								}
								toLog.append("<br>Image of document with following instrument number was not successfully retrieved: ").append(doc.prettyPrint());
							} else{
								doc.getImage().setSaved(true);
								toLog.append("<br>Image of document with following instrument number was successfully retrieved: <a href='")
										.append(doc.getImage().getSsfLink())
										.append("'>")
										.append(doc.prettyPrint())
										.append("</a>");
							}
						} catch (Exception e){
							doc.getImage().setSaved(false);
							toLog.append("<br>Image of document with following instrument number was not successfully retrieved:")
								.append(doc.prettyPrint());
							doc.setImage(null);
							logger.error("performAdditionalProcessingAfterRunningAutomatic", e);
						}
						if (toLog.length() > 0){
							toLog.append("<br>");
							SearchLogger.info(toLog.toString(), searchId);
						}
					}
				}
			}
		} catch (Throwable t) {
			logger.error("Error while post processing document", t);
		} finally {
			manager.releaseAccess();
		}
	}
	
	@Override
	 public DownloadImageResult lookupForImage(DocumentI doc, String documentId) throws ServerResponseException{	
		 
		 if (doc != null){
			 InstrumentI i = doc.getInstrument();
			 getSearch();
			 ImageI image = doc.getImage();
			 boolean docWithoutImage = false;
			 if (image == null){
				 docWithoutImage = true;
				 image = new Image();
					
				 String imageExtension = "tiff";
				 if (StringUtils.isNotEmpty(image.getExtension())){
					 imageExtension = image.getExtension();
				 }
					
				 String imageDirectory = getSearch().getImageDirectory();
				 ro.cst.tsearch.utils.FileUtils.CreateOutputDir(imageDirectory);
				 String fileName = doc.getId() + "." + imageExtension;
				 String path = imageDirectory + File.separator + fileName;
				 if (StringUtils.isEmpty(image.getPath())){
					 image.setPath(path);
				 }
				 image.setFileName(fileName);
				 image.setContentType("IMAGE/TIFF");
				 image.setExtension("tiff");
				 image.setType(IType.TIFF);
				 doc.setImage(image);
				 doc.setIncludeImage(true);
			 }
			 String year = "";
			 if (i.getYear() != SimpleChapterUtils.UNDEFINED_YEAR){
				 year = i.getYear() + "";
			 } else{
				 if (i.getDate() != null){
					 String date = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(i.getDate());
					 if (StringUtils.isNotEmpty(date)){
						 year = date.substring(date.lastIndexOf("/") + 1);
					 }
				 }
			 }
			 String imageLink = getDataSite().getServerHomeLink() + "i2/default.aspx?imageSearch=true&docno=" + i.getInstno() + "&year=" + year;
			 
			 Set<String> links = image.getLinks();
			 if (links.size() == 0){
				 links.add(imageLink);
				 image.setLinks(links);
			 }
				
			 DownloadImageResult dldImageResult = downloadImage(image, doc.getId(), doc);
			 if (dldImageResult.getStatus().equals(DownloadImageResult.Status.ERROR) && docWithoutImage){
				 doc.setImage(null);
			 }
			  
			 return dldImageResult;
		 }
			
		 return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], "");
	 }

}
