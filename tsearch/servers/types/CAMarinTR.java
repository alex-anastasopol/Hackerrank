package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

public class CAMarinTR extends TSServer {

	private static final long	serialVersionUID	= 6158345641321796125L;

	public CAMarinTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	public CAMarinTR(long searchId) {
		super(searchId);
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		rsResponse = rsResponse.replaceAll("(?is)<script[^>]*>.*?</script>", "");
		ParsedResponse parsedResponse = Response.getParsedResponse();

		String message = ro.cst.tsearch.utils.StringUtils
				.extractParameter(rsResponse,
						"(?is)(APN\\s+was\\s+not\\s+found;?.?\\s+please\\s+try\\s+again\\s+with\\s+valid\\s+APN.)");
		if (!message.isEmpty()) {
			parsedResponse.setError(message);
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_PARCEL:

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			}

			break;

		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			String parcelId = "";
			String year = "";
			String details = getDetails(rsResponse);

			NodeList nodeList = new HtmlParser3(details).getNodeList();
			// get parcel id
			parcelId = ro.cst.tsearch.servers.functions.CAMarinTR.getValueForLabel(nodeList, "Parcel Number", 1)
					.replaceAll("[^\\d-]", "");
			// get year
			year = ro.cst.tsearch.servers.functions.CAMarinTR.getValueForLabel(nodeList, "Tax Roll Year", 1);
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				data.put("year", year);

				if (isInstrumentSaved(parcelId, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				parsedResponse.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parsedResponse.setResponse(details);

			} else {
				String filename = parcelId + ".html";
				smartParseDetails(Response, details);

				msSaveToTSDFileName = filename;
				parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				parsedResponse.setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}

			break;

		case ID_GET_LINK:
			ParseResponse(sAction, Response, ID_DETAILS);
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

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			ParsedResponse parsedResponse = response.getParsedResponse();
			HtmlParser3 htmlParser3 = new HtmlParser3(table);
			Node node = htmlParser3.getNodeById("taxbill-app");

			if (node != null) {
				NodeList tableList = HtmlParser3.getTag(node.getChildren(), TableTag.class, true);
				int tableListSize = tableList.size();
				int index = 0;
				TableRow[] rows = new TableRow[0];
				if (tableListSize > 0) {

					TableTag tableTag = (TableTag) tableList.elementAt(index);
					if (tableTag != null) {
						rows = tableTag.getRows();

						// ignore table with unpaid bills if present - get the usual table(it contains everything needed):
						if (rows[0].getColumnCount() <= 4 && StringUtils.containsIgnoreCase(rows[0].toHtml(), "To Pay Online")) {
							rows = new TableRow[0];
							index++;
							if (index < tableListSize) {
								tableTag = (TableTag) tableList.elementAt(index);
								if (tableTag != null) {
									rows = tableTag.getRows();
								}
							}
						}

						String header = "<table width=\"800\" align=\"center\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">" + rows[0].toHtml();
						for (int i = 1; i < rows.length; i++) {
							TableRow row = rows[i];
							int columnCount = row.getColumnCount();

							if (columnCount >= 6) {
								Node linkNode = (Node) row.getColumns()[1];
								linkNode = linkNode.getFirstChild();
								if (linkNode instanceof LinkTag) {
									LinkTag linkTag = (LinkTag) linkNode;
									String link = CreatePartialLink(TSConnectionURL.idGET) + linkTag.getLink();
									linkTag.setLink(link);
									String fullRow = row.toHtml();

									ParsedResponse currentResponse = new ParsedResponse();
									currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, fullRow);
									currentResponse.setOnlyResponse(fullRow);
									currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

									ResultMap resultMap = ro.cst.tsearch.servers.functions.CAMarinTR.parseIntermediaryRow(row);
									resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());

									String taxBillNumber = StringUtils.defaultString((String) resultMap.get(TaxHistorySetKey.TAX_BILL_NUMBER.getKeyName()));
									if (!taxBillNumber.isEmpty()) {// to get statuses for installments and parse them in details
										mSearch.setAdditionalInfo(taxBillNumber, fullRow);
									}

									Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
									DocumentI document = (TaxDocumentI) bridge.importData();
									currentResponse.setDocument(document);

									intermediaryResponse.add(currentResponse);
								}
							} else {

							}
						}

						parsedResponse.setHeader(header);
						parsedResponse.setFooter("</table>");
						outputTable.append(table);
					}
				}
			}
		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}
		return intermediaryResponse;
	}

	protected String getDetails(String rsResponse) {
		try {
			/* If from memory - use it as is */
			if (!rsResponse.contains("<html")) {
				return rsResponse;
			}

			StringBuilder detailsSB = new StringBuilder();
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);

			Node mainNode = htmlParser3.getNodeById("taxbill-app");
			if (mainNode != null) {
				String taxBillNumber = ro.cst.tsearch.servers.functions.CAMarinTR.getValueForLabel(mainNode.getChildren(), "Bill Number", 1);
				String intermediariesContent = "";
				String[] installmentStatus = new String[2];
				if (!taxBillNumber.isEmpty()) {// get statuses for installments from intermediaries and put them in details for parsing
					intermediariesContent = (String) mSearch.getAdditionalInfo(taxBillNumber);
					if (intermediariesContent != null) {
						installmentStatus[0] = ro.cst.tsearch.utils.StringUtils.extractParameter(intermediariesContent,
								"(?is)<td[^>]*>\\s*([\\w]+)\\s*</td>\\s*</tr>");
						installmentStatus[1] = ro.cst.tsearch.utils.StringUtils.extractParameter(intermediariesContent,
								"(?is)<td[^>]*>\\s*([\\w]+)\\s*</td>\\s*<td[^>]*>[^<]*</td>\\s*</tr>\\s*");

						detailsSB.append("<div id=\"installment1Status\" style=\"display: none\">" + installmentStatus[0] + "</div>");
						detailsSB.append("<div id=\"installment2Status\" style=\"display: none\">" + installmentStatus[1] + "</div>");
					}
				}

				detailsSB.append(mainNode.toHtml());
			}
			String details = "<table id=\"" + StringUtils.defaultString(getClass().getSimpleName())
					+ "Details\" border=\"1\" align=\"center\" style=\"min-width:300px;\"><tr><td align=\"center\">"
					+ detailsSB.toString().replaceAll("(?is)<a[^>]*>.*?</a>", "").replaceAll("(?is)<input[^>]*>", "")
					+ "</td></tr></table>";
			return details;
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
		ro.cst.tsearch.servers.functions.CAMarinTR.parseAndFillResultMap(detailsHtml, resultMap, searchId);
		return null;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();

		TSServerInfoModule module = null;

		TaxYearFilterResponse taxYearFilter = new TaxYearFilterResponse(searchId);
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.LD_PARCELNO);
			module.addFilter(taxYearFilter);
			moduleList.add(module);
		}
		serverInfo.setModulesForAutoSearch(moduleList);
	}
}
