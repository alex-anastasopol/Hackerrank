package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
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
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterForNext;
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

public class FLPascoTR extends TSServer {
	
	static final long serialVersionUID = 10000000;
	
	private boolean downloadingForSave;

	public void setServerID(int ServerID){
		super.setServerID(ServerID);
	}
	
	public FLPascoTR(
			String rsRequestSolverName,
			String rsSitePath,
			String rsServerID,
			String rsPrmNameLink,
			long searchId, int mid) {
			super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		
	}

	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		if (rsResponse.contains("entered is not a valid. Please try your query again.") || rsResponse.contains("No records match your query, please try again")) {
			Response.getParsedResponse().setError("No records match your query, please try again");
			return;
		}

		switch (viParseID){
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_INTERMEDIARY:

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			}
			break;

		case ID_SEARCH_BY_PARCEL:
		case ID_DETAILS:
			StringBuilder keyCodeSB = new StringBuilder();
			String details = getDetails(rsResponse,keyCodeSB);
			String keyCode = keyCodeSB.toString();
			String fileName = keyCode + ".html";

			if ((!downloadingForSave)) {
				String originalLink = sAction + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);

				if (isInstrumentSaved(keyCode, null, data)) {
					details += CreateFileAlreadyInTSD();
				}
				else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}
				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

				if (isEmpty(keyCode)) {
					Response.getParsedResponse().setError("Parcel ID Not Found!");
					logger.error("ParseResponse END: Parcel ID NOT Found!");
					return;
				}

			}
			else {// for html
				smartParseDetails(Response, details);
				msSaveToTSDFileName = fileName;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}
			break;
			
		case ID_GET_LINK:
			if (org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "Real Estate Property Search Results")) {
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			else {
				ParseResponse(sAction, Response, ID_INTERMEDIARY);
			}
			break;

		case ID_SAVE_TO_TSD:
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;
			break;
		default:
			break;
		}
	}
	
	protected String getDetails(String rsResponse, StringBuilder keyCodeSB){
		
		// if from memory - use it as is
		if(!rsResponse.toLowerCase().contains("<html")){
			return rsResponse;
		}

		StringBuilder details = new StringBuilder();

		try {
			HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);

			NodeList nodeList = htmlParser3.getNodeList();
			if (nodeList.size() > 0) {
				Node mainDivNode = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("class", "main"), true).elementAt(0);

				if (mainDivNode != null) {
					String detailsHeader = "<table border=\"1\" align=\"center\" width=\"60%\"><tr><td align=\"center\"><h3>Real Estate Property Search Results<h3></td></tr><tr id=\"details\"><td>";
					NodeList parcelNodeList = mainDivNode.getChildren().extractAllNodesThatMatch(new RegexFilter("Parcel Id"), true);
					if (parcelNodeList.size() > 0) {
						keyCodeSB.append((HtmlParser3.getFirstParentTag(parcelNodeList.elementAt(0), Div.class)).getLastChild().toPlainTextString().trim());
						details.append(detailsHeader
								+ HtmlParser3.getFirstParentTag(parcelNodeList.elementAt(0), TableTag.class).toHtml() + "</td></tr>");
					}
					else {
						NodeList ownerOfRecord = mainDivNode.getChildren().extractAllNodesThatMatch(new RegexFilter("Owner of Record"), true);
						details.append(detailsHeader
								+ HtmlParser3.getFirstParentTag(ownerOfRecord.elementAt(0), TableTag.class).toHtml() + "</td></tr>");
					}

					// delinquent section
					// / - replace all style...
					Node delinquentNode = nodeList.extractAllNodesThatMatch(new RegexFilter("Delinquent Taxes"), true).elementAt(0);
					if (delinquentNode != null) {
						delinquentNode = HtmlParser3.getFirstParentTag(delinquentNode, Div.class);
						if (!delinquentNode.toPlainTextString().trim().endsWith("NONE")) {
							String delinquent = delinquentNode
									.toHtml()
									.trim()
									.replaceAll("(?is)<span[^>]*>As\\s+recommended\\s+by\\s+the\\s+state\\s+of\\s+Florida\\s+administrative\\s+code.*?</span>",
											"");
							details.append("<tr><td align=\"center\"><h3>Delinquent Taxes</h3></td></tr><tr id=\"delinquentTaxes\"><td>" + delinquent
									+ "</td></tr>");
						}
					}

					// payment hisyory
					Node paymentHistoryLinkNode = nodeList.extractAllNodesThatMatch(new RegexFilter("Payment History"), true).elementAt(0);
					if (paymentHistoryLinkNode != null) {
						paymentHistoryLinkNode = paymentHistoryLinkNode.getParent();
						if (paymentHistoryLinkNode instanceof LinkTag) {
							String paymentHistory = getLinkContents(dataSite.getServerHomeLink() + ((LinkTag) paymentHistoryLinkNode).getLink());
							htmlParser3 = new HtmlParser3(paymentHistory);
							NodeList paymentHistoryList = htmlParser3.getNodeList();
							Node payHistoryNode = HtmlParser3.getNodeByTypeAttributeDescription(paymentHistoryList, "table", "class", "paytable",
									new String[] { "TAX YEAR", "RECEIPT", "AMOUNT PAID", "MARCH AMOUNT", "DATE", "DOCUMENTS" },
									true);

							if (payHistoryNode != null) {
								TableTag payHistoryTable = (TableTag) payHistoryNode;
								TableRow[] payTableRows = payHistoryTable.getRows();
								payTableRows[0].setAttribute("style", "font-weight: bold");
								payHistoryTable.setAttribute("align", "center");
								details.append("<tr><td align=\"center\"><h3>Payment History</h3></td></tr><tr id=\"paymentHistory\"><td>"
										+ payHistoryTable.toHtml().replaceAll("<b[^>]*>.*?</b>", "")
												.replaceFirst("(?s)<td>\\s*DOCUMENTS\\s*</td>", "<td></td>")
										+ "</td></tr>");
							}
						}
					}
				}
			}
			
			details.append("</table>");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return details.toString().replaceAll("<a[^>]*>(.*?)</a>", "$1")
				.replaceAll("(?is)<input[^>]*>", "")
				.replaceAll("<script[^>]*>.*?</script>", "")
				.replaceAll("<img[^>]*>", "")
				.replaceAll("(?is)(<span[^>]*)(background-color:.*?;[^>]*>.*?</span>)", "$1 width:120px;$2")
				.replaceAll("(?is)(<span[^>]*>\\s*(?:Legal\\s*Description|Tax\\s*Lien).*?</span>)", "$1<br>")
				.replaceFirst("(?s)(\\(Homestead\\s*Denial\\))", "<small>$1</small>");
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
			ro.cst.tsearch.servers.functions.FLPascoTR.parseAndFillResultMap(detailsHtml, map, searchId, miServerID);
		return null;
	}
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
	
		// change account number if necessary
        if(module.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX){
        	String pid = module.getFunction(0).getParamValue();
        	pid = pid.replaceAll("\\s+", ""); 
			        				
           	module.getFunction(0).setParamValue(pid);
          
        }
        return super.SearchBy(module, sd);
	}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) 
	{
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m = null;
		GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		
		if(hasPin())
		{//search by Parcel Number
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			l.add(m);
		}
		
		if (hasStreet())
		{//search By Address, independent of Automatic Search by Name as the behavior on the official site is
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			m.setIteratorType(ModuleStatesIterator.TYPE_ADDRESS);
//			m.setSaKey(0, SearchAttributes.P_STREETNO);
//			m.setSaKey(1, SearchAttributes.P_STREETNAME);
		
			m.addFilter(addressFilter);
			m.setSaObjKey(SearchAttributes.NO_KEY);
			l.add(m);
		}

		if( hasOwner() )
        {//search by Owner
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
//			m.setSaKey(0, SearchAttributes.OWNER_LFM_NAME);
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.addFilterForNext(new NameFilterForNext(m.getSaObjKey(), searchId, m, false));
			m.addFilter(NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m));
			
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(m, searchId, new String[] {"L;F;","L;f;", "L;M;", "L;m;"});
			m.addIterator(nameIterator);

			l.add(m);
		}

		serverInfo.setModulesForAutoSearch(l);
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {

		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			table = StringEscapeUtils.unescapeHtml(table);
			HtmlParser3 htmlParser3 = new HtmlParser3(table);
			table = Tidy.tidyParse(table, null);

			NodeList nodeList = htmlParser3.getNodeList();
			NodeList mainTableList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true).extractAllNodesThatMatch(new HasAttributeFilter("class", "results"), true);
			NodeList pagingList = nodeList
					.extractAllNodesThatMatch(new TagNameFilter("table"), true).extractAllNodesThatMatch(new HasAttributeFilter("class", "rlInnerTable"), true);
			if (mainTableList.size() == 0) {
				return intermediaryResponse;
			}
			TableTag tableTag = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows = tableTag.getRows();
			boolean addresIntermediaries = false;

			if (rows.length > 2 && rows[2].getColumns()[0].toPlainTextString().trim().isEmpty()) {
				addresIntermediaries = true;
			}

			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];

				if (row.getColumnCount() == 3) {
					// remove last column in intermediaries
					if (row.getChildCount() >= 7) {
						row.removeChild(6);
						row.removeChild(5);
						row.removeChild(4);
					}
					// get details link
					LinkTag linkTag = (LinkTag) row.getColumns()[0].getFirstChild();
					linkTag.setLink(CreatePartialLink(TSConnectionURL.idGET) + "/search/" + linkTag.getLink());
					String link = linkTag.extractLink().trim();

					//
					String onlyResponse = row.toHtml();
					TableColumn addressColumn = null;
					if (addresIntermediaries == true) {// in addres intermediaries there are two extra lines per result
						addressColumn = rows[i + 1].getColumns()[1];
						onlyResponse = onlyResponse.replaceAll("(?is)<td[^>]*>\\s*</td>\\s*(</tr>)", "$1");
						i = i + 2;
					}
					//
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, onlyResponse);

					currentResponse.setOnlyResponse(onlyResponse);
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

					ResultMap m = ro.cst.tsearch.servers.functions.FLPascoTR.parseIntermediaryRow(row, searchId, addressColumn);

					Bridge bridge = new Bridge(currentResponse, m, searchId);

					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);
				}
			}

			// paging
			String headerPagingDiv = "";
			String footerPagingDiv = "";
			if (pagingList.size() > 0) {
				footerPagingDiv = processLinks(response, pagingList.elementAt(0));
				if (pagingList.size() > 1) {
					headerPagingDiv = processLinks(response, pagingList.elementAt(1));
				}
			}
			response.getParsedResponse().setHeader(footerPagingDiv + "<table border=\"1\" width=\"50%\"><tr><th>Parcel ID</th><th>Owner Name</th></tr>");
			response.getParsedResponse().setFooter(headerPagingDiv + "</table>");

			outputTable.append(table);
		} catch (Throwable t) {
			logger.error("Error while parsing intermediary data", t);
		}

		return intermediaryResponse;
	}

	public String processLinks(ServerResponse response, Node pagingList) {
		StringBuilder pagingDiv = new StringBuilder();
		if (pagingList != null) {
			pagingDiv.append("<table width=\"50%\" border=\"1\"><tr><td align=\"center\"><div class=\"paging\">");
			String startLink = CreatePartialLink(TSConnectionURL.idGET);
			NodeList linkList = pagingList.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
			for (int i = 0; i < linkList.size(); i++) {
				LinkTag linkTag = (LinkTag) linkList.elementAt(i);
				linkTag.setLink(startLink + linkTag.getLink());
			}
			pagingDiv.append(pagingList.toHtml());
		}
		return pagingDiv.toString().replaceAll("\\s*</?(?:li|ul)[^>]*>", "  ").replaceAll("(<a[^>]*>.*?</a>)", " $1 ") + "</div></td></tr></table>";
	}
	
	protected void loadDataHash(HashMap<String, String> data) {
		if (data != null) {
			data.put("type", "CNTYTAX");
		}
	}
}
