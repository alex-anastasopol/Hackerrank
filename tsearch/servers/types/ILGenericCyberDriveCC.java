package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.RegexFilter;
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
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RestoreDocumentDataI;

public class ILGenericCyberDriveCC extends TSServerROLike {

	private static final long	serialVersionUID	= 1L;
	private static final String	DOCTYPE_PARAM_PREFIX		= "docType_";

	public ILGenericCyberDriveCC(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = StringEscapeUtils.unescapeHtml(Response.getResult());
		rsResponse = rsResponse.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");
		ParsedResponse parsedResponse = Response.getParsedResponse();
		mSearch.setAdditionalInfo("viParseID", viParseID);

		String unavailableMessage = "The System is temporarily unavailable. Please try again later.";
		if (rsResponse.indexOf("UCC Search Error") > -1 || rsResponse.indexOf(unavailableMessage) > -1) {
			parsedResponse.setError(unavailableMessage);
			parsedResponse.setResponse("");
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_MODULE38: // UCC Search - Debtor Personal Name
		case ID_SEARCH_BY_MODULE39: // UCC Search - Debtor Business Name
		case ID_SEARCH_BY_MODULE40: // UCC Search - Debtor Business Name Word
		case ID_SEARCH_BY_MODULE41: // UCC search - File Number
		case ID_SEARCH_BY_MODULE42: // Federal Tax Lien
		case ID_INTERMEDIARY:
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			}

			if (smartParsedResponses.size() == 0) {
				return;
			}
			break;

		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
			String instrumentNumber = "";
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			Node instrumentNoNode = null;

			instrumentNoNode = htmlParser3.getNodeById("instrumentNumber");
			if (instrumentNoNode != null) {
				instrumentNumber = instrumentNoNode.toHtml().replaceFirst("(?is)<input[^>]*\\bvalue=\"([^\"]+)\"[^>]*>", "$1");
			}

			if (instrumentNumber.isEmpty() || (!instrumentNumber.matches("[\\s\\d-]+"))) {
				instrumentNoNode = htmlParser3.getNodeList().extractAllNodesThatMatch(new RegexFilter("(?s)\\bFile\\s+Number\\b"), true).elementAt(0);
				if (instrumentNoNode == null) {
					instrumentNoNode = htmlParser3.getNodeList().extractAllNodesThatMatch(new RegexFilter("(?s)\\bFile\\s*#\\b"), true).elementAt(0);
				}
				if (instrumentNoNode != null) {
					instrumentNoNode = HtmlParser3.getFirstParentTag(instrumentNoNode, TableTag.class);
					if (instrumentNoNode != null) {
						TableTag instrumentNoTable = (TableTag) instrumentNoNode;
						TableRow row = instrumentNoTable.getRow(0);
						if (row.getColumnCount() > 0) {
							instrumentNumber = row.getColumns()[0].toPlainTextString().replaceFirst("(?is)\\s*File\\s+Number\\s*:?\\s*([\\w-]+)\\s*", "$1");
							if (instrumentNumber.isEmpty() || !instrumentNumber.matches("[\\s\\d-]+")) {
								row = instrumentNoTable.getRow(1);
								if (row.getColumnCount() > 0) {
									instrumentNumber = row.toPlainTextString().replaceFirst("(?is)\\s*File\\s+Number\\s*:?\\s*([\\w-]+)\\s*", "$1");
								}
							}
						}
					}
				}
			}

			if ((instrumentNumber.isEmpty() || (!instrumentNumber.matches("[\\s\\d-]+"))) && !StringUtils.defaultString(msLastLink).isEmpty()) {
				instrumentNumber = ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(msLastLink, "fileNbr");
			}

			String details = getDetails(Response, rsResponse, instrumentNumber);

			if (viParseID != ID_SAVE_TO_TSD) {

				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data, DOCTYPE_PARAM_PREFIX + instrumentNumber);

				if (isInstrumentSaved(instrumentNumber, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS)
							.replaceAll("(?is)<input[^>]*>\\s*Save\\s+with\\s+(search\\s+parameters|cross-references)\\s*(?:<br>)?", "");
				}
				parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parsedResponse.setResponse(details);

			} else {

				smartParseDetails(Response, details);
				msSaveToTSDFileName = instrumentNumber + ".html";
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				parsedResponse.setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;
		case ID_GET_LINK:

			if (sAction.contains("/searchresults.do?history=N")) {
				ParseResponse(sAction, Response, ID_INTERMEDIARY);
			}
			else if (sAction.contains("searchresults.do?history=H")) {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			break;
		default:
			break;
		}

	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String rsResponse, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			ParsedResponse parsedResponse = response.getParsedResponse();
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
			NodeList nodeList = htmlParser3.getNodeList();
			Node theTable = htmlParser3.getNodeById("theTable");
			Node resultsIntervalNode = nodeList.extractAllNodesThatMatch(new RegexFilter("(?is)Showing\\s+Results"), true).elementAt(0);
			boolean isFTLSearch = false;
			String resultsInterval = "";
			if (resultsIntervalNode != null) {
				resultsInterval = resultsIntervalNode.getParent().toHtml().replaceAll("(?is)\\s*</?p[^>]*>\\s*", "");
			}
			if (theTable == null) {
				return intermediaryResponse;
			}

			TableTag tableTag = (TableTag) theTable;
			TableRow[] rows = tableTag.getRows();
			if (rows.length > 0) {
				String formParams = getIntermFormParams(theTable);
				String noOfRecords = StringUtils.defaultString(ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(formParams, "totalRecords"));
				String toParam = StringUtils.defaultString(ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(formParams, "to"));

				String pageLink = "searchresults.do?";
				Pattern pat = Pattern.compile("(?is)\\bFederal\\s+Tax\\s+Lien\\s+Search");
				Matcher mat = pat.matcher(rsResponse);
				if (mat.find()) {
					isFTLSearch = true;
					pageLink = "ftlsearchresults.do?";
				}

				String linkStart = CreatePartialLink(TSConnectionURL.idPOST)
						+ pageLink + formParams.replaceAll("(?s)\\s+", "+");
				String nextLink = ("<a href=" + linkStart + "&more=More Results" + " style=\"font-size:larger;\">More Results</a>").replaceFirst(
						"\\bhistory=.*?(&|$)", "history=N" + "$1");
				if (toParam.equals(noOfRecords) && !toParam.isEmpty()) {
					// don't show next link if it's the last page
					nextLink = "";
				} else {
					parsedResponse.setNextLink(nextLink);
				}
				mSearch.setAdditionalInfo("intermediariesHeader", rows[0].toHtml());

				for (int i = 0; i < rows.length; i++) {
					TableRow row = rows[i];
					if (row.getColumnCount() > 0) {
						TableColumn[] columns = row.getColumns();
						LinkTag linkTag = ((LinkTag) columns[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true).elementAt(0));
						if (linkTag != null) {
							String detailsLink = linkStart;
							if (isFTLSearch) {
								pat = Pattern.compile("(?is).*?form\\(\\s*'([^']*)'\\s*,\\s*'([^']*)'\\s*,\\s*'([^']*)'\\)");
								mat = pat.matcher(linkTag.getLink());
								if (mat.find()) {
									detailsLink = detailsLink.replaceFirst("\\brefilingDate=.*?(&|$)", "refilingDate=" + mat.group(1) + "$1")
											.replaceFirst("\\blienAmt=.*?(&|$)", "lienAmt=" + mat.group(2) + "$1")
											.replaceFirst("\\bftlFileNbr=.*?(&|$)", "ftlFileNbr=" + mat.group(3) + "$1")
											.replaceFirst("\\bhistory=.*?(&|$)", "history=H" + "$1");
								}
							} else {
								detailsLink = detailsLink.replaceFirst("\\bfileNbr=.*?(&|$)", "fileNbr=" + linkTag.getLink()
										.replaceFirst("(?is).*?\\(\\s*'([^']*)'\\s*\\).*", "$1") + "$1")
										.replaceFirst("\\bhistory=.*?(&|$)", "history=H" + "$1");
							}
							linkTag.setLink(detailsLink);
							ResultMap resultMap = ro.cst.tsearch.servers.functions.ILGenericCyberDriveCC.parseIntermediaryRow(row);
							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
							String rowString = row.toHtml();
							mSearch.setAdditionalInfo(resultMap.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()).toString(), rowString);

							String docType = (String) resultMap.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName());
							if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(docType)) {
								mSearch.setAdditionalInfo(DOCTYPE_PARAM_PREFIX + resultMap.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()).toString(), docType);
							}

							ParsedResponse currentResponse = new ParsedResponse();
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowString);
							currentResponse.setOnlyResponse(rowString);
							currentResponse.setPageLink(new LinkInPage(detailsLink, detailsLink, TSServer.REQUEST_SAVE_TO_TSD));

							Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
							DocumentI document = (RegisterDocument) bridge.importData();
							currentResponse.setDocument(document);
							intermediaryResponse.add(currentResponse);
						}
					}
				}

				parsedResponse.setHeader(
						"<p align=\"center\"><b>Total results: " + noOfRecords + ". " + resultsInterval +
								"&nbsp;&nbsp;&nbsp;&nbsp;" + nextLink + "</b></p><table width=\"900px\" border=\"1\" align=\"center\">" + rows[0].toHtml());
				parsedResponse.setFooter("</table>");

				outputTable.append(theTable.toHtml());
			}
		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}
		return intermediaryResponse;
	}

	protected String getDetails(ServerResponse Response, String rsResponse, String instrumentNumber) {
		try {
			StringBuilder detailsSB = new StringBuilder();
			String details = "";
			String intermediariesContent = StringUtils.defaultString((String) mSearch.getAdditionalInfo(instrumentNumber));
			String intermediariesHeader = StringUtils.defaultString((String) mSearch.getAdditionalInfo("intermediariesHeader"));
			detailsSB.append("<input type=\"hidden\" id=\"instrumentNumber\" value=\"" + instrumentNumber + "\">");
			detailsSB.append("<tr id=\"intermediariesContent\"><td><table>" + intermediariesHeader + intermediariesContent + "</table></td></tr>");

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}
			Pattern nothingFoundPattern = Pattern.compile("(?is)No\\s+UCC\\s+Amendments\\s+Found");
			Matcher nothingFoundMatcher = nothingFoundPattern.matcher(rsResponse);

			if (!nothingFoundMatcher.find()) {
				HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
				NodeList nodeList = htmlParser3.getNodeList();

				Node ftlExtraDetails = nodeList.extractAllNodesThatMatch(new RegexFilter("\\bLien\\s+Amount\\b"), true).elementAt(0);
				if (ftlExtraDetails != null) {
					ftlExtraDetails = HtmlParser3.getFirstParentTag(ftlExtraDetails, TableTag.class);
					detailsSB.append("<tr><td>" + ftlExtraDetails.toHtml().replaceAll("(?is)\\s*<(td|table)[^>]*>\\s*", "<$1 align=\"center\">")
							+ "</td></tr>");
				}

				Node theTable = htmlParser3.getNodeById("theTable");
				if (theTable != null && theTable instanceof TableTag) {
					detailsSB.append("<tr id=\"details\"><td>"
							+ theTable.toHtml().replaceAll("(?is)\\s*<(table)[^>]*>\\s*", "<$1 align=\"center\" width=\"100%\">") + "</td></tr>");
				}

			}
			if (detailsSB.length() > 0) {
				details = "<table border=\"1\" align=\"center\" width=\"900px\">" + detailsSB.toString() + "</table>";
			}

			return details.replaceAll("(?ism)</?a[^>]*>", "");

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String getIntermFormParams(Node theTable) {
		StringBuilder paramsSB = new StringBuilder();
		String params = "";
		try {
			HtmlParser3 htmlParser3 = new HtmlParser3(theTable.toHtml());
			NodeList inputs = htmlParser3.getNodeList().extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("type", "hidden"));
			if (inputs.size() > 0) {
				for (int i = 0; i < inputs.size(); i++) {
					Pattern pat = Pattern.compile("(?is)\\bname=\"([^\"]*)\"[^>]*\\bvalue=\"([^\"]*)\"");
					Matcher mat = pat.matcher(inputs.elementAt(i).toHtml());
					if (mat.find()) {
						paramsSB.append(mat.group(1) + "=" + mat.group(2) + "&");
					}
				}
			}

			params = paramsSB.toString();

			if (!params.isEmpty()) {
				params = params.substring(0, params.lastIndexOf("&"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return params;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		ro.cst.tsearch.servers.functions.ILGenericCyberDriveCC.parseAndFillResultMap(response, detailsHtml, resultMap);
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(
				SearchAttributes.OWNER_OBJECT, searchId, module);
		if (hasOwner()) {
			// UCC Search - Debtor Personal Name
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX38));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);

			((GenericNameFilter) defaultNameFilter).setUseArrangements(false);
			((GenericNameFilter) defaultNameFilter).setInitAgain(true);

			module.addFilter(defaultNameFilter);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = new ConfigurableNameIterator(searchId, new String[] { "L;F;" });
			nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
			nameIterator.clearSearchedNames();
			nameIterator.setInitAgain(true);
			module.addIterator(nameIterator);
			modules.add(module);

			// UCC Search - Debtor Business Name
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX39));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);

			((GenericNameFilter) defaultNameFilter).setUseArrangements(false);
			((GenericNameFilter) defaultNameFilter).setInitAgain(true);

			module.addFilter(defaultNameFilter);
			module.setIteratorType(8, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
			nameIterator = new ConfigurableNameIterator(searchId, new String[] { "L;;" });
			nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
			nameIterator.clearSearchedNames();
			nameIterator.setInitAgain(true);
			module.addIterator(nameIterator);
			modules.add(module);
		}
		serverInfo.setModulesForAutoSearch(modules);
	}

	protected void loadDataHash(HashMap<String, String> data, String docTypeParam) {
		String docType = (String) mSearch.getAdditionalInfo(docTypeParam);
		if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(docType)) {
			if (data != null) {
				data.put("type", docType);
			}
		}
	}

	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI document) {
		return null;
	}
}
