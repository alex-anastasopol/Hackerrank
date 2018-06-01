package ro.cst.tsearch.servers.types;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.html.parser.HtmlHelper;
import org.htmlparser.Node;
import org.htmlparser.Tag;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;

public class FLLeonTR extends TSServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8668764514221605407L;
	String action;

	public FLLeonTR(long searchID) {
		super(searchID);
	}

	public FLLeonTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	private static int seq = 0;

	protected synchronized static int getSeq() {
		return seq++;
	}

	String[] postParameters = { "__VIEWSTATE", "__EVENTVALIDATION" };

	@Override
	protected void ParseResponse(String action, ServerResponse response, int viParseID) throws ServerResponseException {
		String serverResult = response.getResult().replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");
		boolean serverError = serverResult.contains("A serious server error has occurred. The webmaster has been informed.");

		if (serverError) {
			response.setError(serverResult);
			return;
		}

		String viewState = StringUtils.getTextBetweenDelimiters("\"__VIEWSTATE\" value=\"", "\"", serverResult);
		String eventValidation = StringUtils.getTextBetweenDelimiters("\"__EVENTVALIDATION\" value=\"", "\"", serverResult);
		if ((StringUtils.isEmpty(viewState) || StringUtils.isEmpty(eventValidation)) && viParseID != ID_SAVE_TO_TSD) {
			return;
		}
		boolean dataNotFound = serverResult.contains("Some account data is not viewable or is unavailable at this time");
		if (dataNotFound) {
			response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
			return;
		}

		response.setResult(cleanServerResult(response.getResult()));
		serverResult = response.getResult();
		switch (viParseID) {
		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_MODULE19:
		case ID_SEARCH_BY_SUBDIVISION_NAME:
		case ID_SEARCH_BY_ADDRESS:
		case ID_INTERMEDIARY:
			StringBuilder outputTable = new StringBuilder();
			setAction(action);
			Collection<ParsedResponse> intermediary = smartParseIntermediary(response, serverResult, outputTable);
			if (intermediary.size() > 0) {
				response.getParsedResponse().setResultRows(new Vector<ParsedResponse>(intermediary));
				response.getParsedResponse().setOnlyResponse(outputTable.toString());
				response.getParsedResponse().setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
			}
			break;
			
		case ID_SAVE_TO_TSD:
		case ID_DETAILS:
			serverResult = buildDetailsPageWithExtraTaxInfo(serverResult);
			response.setResult(serverResult);
			String accountNumber = "";
			String currentYear = "";
			HtmlParser3 parser = new HtmlParser3(serverResult);
			try {
				Node currentAccountDetails = HtmlParser3.getNodeByID("tbl3", parser.getNodeList(), true);
				Node taxYear = HtmlParser3.getNodeByID("_ctl0_ContentPlaceHolder1_lblDetTaxYear", currentAccountDetails.getChildren(), true);
				accountNumber = HtmlParser3.getNodeByID("_ctl0_ContentPlaceHolder1_lblDetTaxParcel", currentAccountDetails.getChildren(), true).toPlainTextString()
						.replaceAll("&nbsp;", " ").trim();
				if (NumberUtils.isNumber(taxYear.getFirstChild().toPlainTextString())) {
					currentYear = taxYear.getFirstChild().toPlainTextString();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			String filename = accountNumber + ".html";
			if (viParseID == ID_DETAILS) {
				String originalLink = action + "&" + response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "CNTYTAX");
				if (StringUtils.isNotEmpty(currentYear)){
					data.put("year", currentYear);
				}

				if (isInstrumentSaved(accountNumber, null, data)) {
					serverResult += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, serverResult);

					serverResult = addSaveToTsdButton(serverResult, sSave2TSDLink, viParseID);
				}

				response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				response.getParsedResponse().setResponse(serverResult);

			} else {
				smartParseDetails(response, serverResult);

				msSaveToTSDFileName = filename;
				response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				response.getParsedResponse().setResponse(serverResult);

				msSaveToTSDResponce = serverResult + CreateFileAlreadyInTSD();
			}
			break;
		case ID_GET_LINK:
			if (serverResult.contains("Tax Roll Search Results")) {
				ParseResponse(action, response, ID_INTERMEDIARY);
			} else if (serverResult.contains("Tax Roll Property Summary")) {
				ParseResponse(action, response, ID_DETAILS);
			}
			break;

		}
	}

	private String cleanServerResult(String result) {
		// clean "Add to carts"
		result = result.replaceAll("(?is)<a (id=\"lnkCart.?\"|href='javascript:__doPostBack\\(\"lnkCart).*?</a>", "");
		result = result.replaceAll("(?is)<a (id=\\\"_ctl0_ContentPlaceHolder1_lnkCart\\d+\\\"|href=['\\\"]javascript:__doPostBack\\([\\\"']_ctl0[\\$:]ContentPlaceHolder1[\\$:]lnkCart\\d+)[^>]+>"
				+ "(?:<img[^>]+>\\s*)?(?:<div[^>]+>\\s*)?Add(?:\\s|&nbsp;)+to(?:\\s|&nbsp;)+Cart(?:\\s|&nbsp;)*(?:</div>)?\\s*</a>", "");
		result = result.replaceAll("(?is)<input type='checkbox' id='chkPayOnline.*?</label>", "");
		result = result.replaceAll("<input [\\w\\s'=]*id='chkPayOnline\\d+[^>]+>", "");
		result = result.replaceAll("(?is)(?:<span[^>]*>\\s*)?<input [\\w\\s='\\\"]*name=['\\\"]chkPayOnline\\d+['\\\"][^>]*>\\s*", "");
		result = result.replaceAll("(?is)<img[^>]+>\\s*<div[^>]*>\\s*Add(?:\\s|&nbsp;)+to(?:\\s|&nbsp;)Cart(?:\\s|&nbsp;)*\\s*</div>\\s*(?:</span>)?","");
		
		
		// clean from details links to Tax bill, Certificate etc
		result = result.replaceAll("(?is)<a title=\"(Regular tax bill|Installment payments|Tax certificate).*?</a>", "");
		result = result.replaceAll("(?is)<a id=\"lnkAcctBill2\".*?</a>", "");
//		String regex = "(?is)<a href=.*?(Prior Payments Due) - View Detail</a>";
//		Pattern.matches("(?im)\\s<a href='PropertyDetails.aspx\\?Acctno=.*?(Prior Payments Due) - View Detail</a>\\s", result);
		result = result.replaceAll("(?im)<a href='PropertyDetails.aspx\\?Acctno=.*?(Prior Payments Due) - View Detail</a>", "$1");

		// clean from links New Search
		result = result.replaceAll("(?is)<a id=\"lnkSearch.*?</a>", "");
		result = result.replaceAll("(?is)<a id=\"lnkHelp.*?</a>", "");
		result = result.replaceAll("(?is)<a id=\"lnkSummary.*?</a>", "");

		return result;
	}

	private String buildDetailsPageWithExtraTaxInfo(String serverResult) {
		// am serverResult
		
		if (!serverResult.toLowerCase().contains("<html")){
			return serverResult;
		}
		HtmlParser3 parser = new HtmlParser3(serverResult);
		NodeList root = parser.getNodeList();

		// get all the year links, sort by year
		TableTag taxHistoryTable = (TableTag) root.extractAllNodesThatMatch(new HasAttributeFilter("id", "_ctl0_ContentPlaceHolder1_tblSummary"), true).elementAt(0);
		TableRow[] taxYearRows = taxHistoryTable.getRows();
		List<String> linkList = new LinkedList<String>();

		for (int i = 1; i < taxYearRows.length; i++) {
			NodeList children = taxYearRows[i].getChildren();
			Node firstChild = children.elementAt(1).getFirstChild();
			if (firstChild instanceof LinkTag) {
				LinkTag link = (LinkTag) firstChild;
				String uri = link.getLink();
				linkList.add(uri);
			}
		}

		// get Bills table for each link
		StringBuffer billsTables = new StringBuffer();
		for (String string : linkList) {
			String billsPage = "";
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			try {
				String yearLink = string;
				billsPage = ((ro.cst.tsearch.connection.http2.FLLeonTR) site).getCurrentAccountDetails(yearLink);
				// iterate through links and add response to list
				Node billTableTag = getTableTagListFromHtml(billsPage).extractAllNodesThatMatch(new HasAttributeFilter("id", "tbl5"), true).elementAt(0);

				HtmlHelper.addTagToTag(root, HtmlHelper.createHR(), FormTag.class);
				Tag headerTableTag = HtmlHelper.createTableTag();
				headerTableTag.setAttribute("style", "width:750px;border-collapse:collapse;border-collapse:seperate;");
				headerTableTag.setAttribute("align", "center");
				Tag tr = HtmlHelper.createTableRowTag();
				Tag tc = HtmlHelper.createTableColumnTag();

				Pattern pattern = Pattern.compile("(?is)(?<=Acctyear=)\\d{4}");
				Matcher matcher = pattern.matcher(yearLink);
				tc.setAttribute("align", "center");
				if (matcher.find()) {
					tc = HtmlHelper.addTagToTag(tc, HtmlHelper.createBoldTag(matcher.group()));
				}

				tr = HtmlHelper.addTagToTag(tr, tc);

				if (tr != null) {
					billTableTag.getChildren().prepend(tr);
				}
				billsTables.append(billTableTag.toHtml());

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager.releaseSite(site);
			}
		}
		String styleFixedText=".fixedtext { font: 9pt courier new; color: black; background-color: #ffffff; }";
		String styleColoredFixedText=".colorfixedtext{ font: 9pt courier new; color: black; background-color: #d5d5d5; } ";
		// remove bottom links ("Links of Interest")
		Node interestLinks = HtmlParser3.getNodeByID("tbl6", root, true);
		String link = "";
		if (interestLinks != null) {
			NodeList tag = HtmlParser3.getTag(interestLinks.getChildren(), new LinkTag(), true);
			if (tag.size() > 1) {
				if (tag.elementAt(1) instanceof LinkTag) {
					link = ((LinkTag) tag.elementAt(1)).getLink();
				}
			}
		}

		// make a request to get Appraissal info for SaleDataSet
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		String appraiserPage = "";
		try {
			if (StringUtils.isNotEmpty(link)) {
				appraiserPage = ((ro.cst.tsearch.connection.http2.FLLeonTR) site).getAppraiserPage(link);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}
		// clean the appraiser PAge
		appraiserPage = appraiserPage.replaceAll("(?is)<span class=\"mediumFontRED\">All in.*?</span>", "");
		// clean the book page links
		appraiserPage = appraiserPage.replaceAll("(?is)HREF=\"http://image.clerk.leon.fl.us.*?target=\"_blank\"", "");

		boolean isError = appraiserPage.contains("Error Occurred While Processing Request") || appraiserPage.contains("Server Error");
		HtmlParser3 appraiserParser = null;
		
		String divSalesTable = "";
		if (!isError) {
			appraiserParser = new HtmlParser3(appraiserPage);
			Node recentSalesDiv = HtmlParser3.getNodeByID("recentSales", appraiserParser.getNodeList(), true);
			if (recentSalesDiv != null){
				divSalesTable = recentSalesDiv.toHtml();
				divSalesTable = divSalesTable.replaceAll("(?is)(<table[^>]*\\s+)width=\"[^\"]+\"([^>]*>)", "$1width=\"750\"$2")
						.replaceAll("(?is)(<div[^>]*\\s+)style=\"[^\"]+\"", "$1 ")
						.replaceAll("(?is)(<table[^>]*\\s+)border=\"?[\\da-z%]+\"?([^>]*>)", "$1border=\"1\" $2");
			}
		}
		StringBuffer res = new StringBuffer();
		
		res.append(root.extractAllNodesThatMatch(new HasAttributeFilter("id", "tbl1"), true).toHtml());
		res.append("<div align=\"center\">");
		res.append(root.extractAllNodesThatMatch(new HasAttributeFilter("id", "_ctl0_ContentPlaceHolder1_tblSummary"), true).toHtml().replaceAll("(?is)(<table[^>]*\\s+)(?:width=\"[^\"]+\")?([^>]*>)", "$1 width=\"750\" $2"));
		res.append("</div><div align=\"center\">");
		res.append(root.extractAllNodesThatMatch(new HasAttributeFilter("id", "tbl7"), true).toHtml());
		res.append("</div>");
		res.append(root.extractAllNodesThatMatch(new HasAttributeFilter("id", "tbl3"), true).toHtml().replaceFirst("(?is)(<table)", "$1 border=\"1\""));
		res.append(root.extractAllNodesThatMatch(new HasAttributeFilter("id", "tbl4"), true).toHtml());
		res.append(root.extractAllNodesThatMatch(new HasAttributeFilter("id", "tbl5"), true).toHtml());
		res.append(billsTables);
		res.append(divSalesTable);
		
		String result = res.toString();

		result = result.replaceAll("(?is)<a title=\"Click to see account details for.*?(\\d+)</a>", "$1");
		result = result.replaceAll("(?is)</?a[^>]*>", "");
		
		//add styles to result for proper formating in Receipt tables
		result = String.format("<style>%s</style>", styleColoredFixedText + " " + styleFixedText) + " " + result;
		return result.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "")
				.replaceAll("(?is)(<table[^>]*\\s+)cellspacing=\"?[\\d]+\"?([^>]*>)", "$1$2")
				.replaceAll("(?is)(<table[^>]*\\s+)rules=\"?[^\"]+\"?([^>]*>)", "$1$2")
				.replaceAll("(?is)(<table[^>]*\\s+)style=\"[^\"]+\"([^>]*>)", "$1$2");
	}

	private NodeList getTableTagListFromHtml(String serverResult) {
		HtmlParser3 parser = new HtmlParser3(serverResult);

		NodeList tables = HtmlParser3.getTag(parser.getNodeList(), new TableTag(), true);
		return tables;
	}

	@Override
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml) {
		return super.smartParseDetails(response, detailsHtml);
	}

	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		// get the "prepared" input data
		HtmlParser3 parser = new HtmlParser3(response.getResult());

		NodeList tableTags = HtmlParser3.getTag(parser.getNodeList(), new TableTag(), true);
		// get the result table
		TableTag responseTable = (TableTag) tableTags.elementAt(25);

		TableRow[] rows = responseTable.getRows();

		// split the serverResult in interesting data that will be parsed into a document
		response.getParsedResponse().setHeader("<div><table>" + rows[1].toHtml() + "</table></div>" +
												"<table cellspacing=\"2\" cellpadding=\"2\" width=\"95%\" align=\"center\" border=\"1\"> ");
		int typeOfParse = 0;
		if (rows[1].toPlainTextString().contains("Physical Property Address")) {
			typeOfParse = 2;
		}
		for (int i = 2; i < rows.length; i = i + 4) {
			intermediaryResponse.add(parseRow(rows, i, typeOfParse));
		}

		NodeList tag = HtmlParser3.getTag(parser.getNodeList(), new FormTag(), true);
		FormTag formTag = (FormTag) tag.elementAt(0);

		int size = tableTags.size();
//		TableTag linkTable = (TableTag) tableTags.elementAt(size - 1);
//		outputTable.append(tableTags.elementAt(1).toHtml());
		TableTag linkTable = (TableTag) tableTags.elementAt(size - 2);
		outputTable.append(tableTags.elementAt(25).toHtml());
		boolean hasPagingLinks = false;
		if (parser.getNodeById("_ctl0_ContentPlaceHolder1_lnkNext") != null || parser.getNodeById("_ctl0_ContentPlaceHolder1_lnkPrevious") != null) {
			hasPagingLinks = true;
		}
		String processLinks = processLinks(response, formTag, linkTable, hasPagingLinks);
		
		outputTable.append(processLinks);
		response.getParsedResponse().setFooter("</table>" + processLinks);

		return intermediaryResponse;

	}

	private String processLinks(ServerResponse response, FormTag formTag, TableTag linkTable, boolean hasMoreThan3Rows) {
		String nextLink = "", prevLink = "";
		boolean setOnlyPrev = false;

		int lnk = getSeq();
		
		if (!hasMoreThan3Rows) {
			if (lnk > 2) {
				hasMoreThan3Rows = true;
				setOnlyPrev = true;
			}
		}

		if (hasMoreThan3Rows) {
			String currentLink = formTag.getFormLocation();
			currentLink = currentLink.replaceAll("&amp;", "&");
			String querryString = currentLink.substring(currentLink.indexOf("?") + 1);
			Map<String, String> parameters = StringUtils.extractParametersFromQuery(querryString);
			
			//String[] ownerNameSearchParameters = { "Owner", "Search", "Year" };
			String[] split = currentLink.split("\\?");
			if (split.length == 2) {
				currentLink = split[0];
			}
			// parameters = keepParameters(parameters, ownerNameSearchParameters);
			currentLink = StringUtils.addParametersToUrl("/itm/" + currentLink + "?", parameters, true);
			nextLink = CreatePartialLink(TSConnectionURL.idPOST) + currentLink;

			nextLink = nextLink.replaceAll("&amp;", "&");

			Map<String, String> tempLinkAttrMap = new HashMap<String, String>();
			tempLinkAttrMap.put("ats_link_attributes_key", "" + lnk);
			nextLink = StringUtils.addParametersToUrl(nextLink, tempLinkAttrMap, true);

			NodeList linkTags = HtmlParser3.getTag(linkTable.getChildren(), new LinkTag(), true);
			
			LinkTag linkTagNext = new LinkTag();
			if (linkTags.size() > 1) {
				linkTagNext = (LinkTag) linkTags.elementAt(0);
			}
			boolean isNextLinkDisabled = "disabled".equals(linkTagNext.getAttribute("disabled"));
			
			LinkTag linkTagPrevious = new LinkTag();
			if (linkTags.size() > 1) {
				linkTagPrevious = (LinkTag) linkTags.elementAt(1);
			}
			
			String previousEventTarget = "";
			boolean isPrevLinkDisabled = "disabled".equals(org.apache.commons.lang.StringUtils.defaultString(linkTagPrevious.getAttribute("disabled")));
			if (!isPrevLinkDisabled) {
				String linkText = linkTagPrevious.getLink();
				String[] parameterValue = linkText.split("'");
				previousEventTarget = parameterValue[1];
				linkTagPrevious.removeAttribute("id");
			}
			
			if (linkTags.size() == 2 && !setOnlyPrev) {

				// if (!("disabled".equals(linkTagNext.getAttribute("disabled")))) {
				String linkText = linkTagNext.getLink();

				// put __EVENTTARGET and __EVENTARGUMENT values
				String[] parameterValue = linkText.split("'");
				Map<String, String> params = HttpUtils.getInputsFromFormTag(formTag);
				String nextEventTarget = "";
				try {
					if (!isNextLinkDisabled) {
						nextEventTarget = java.net.URLDecoder.decode(parameterValue[1], "UTF-8");
						params.put("__EVENTARGUMENT", java.net.URLDecoder.decode(parameterValue[3], "UTF-8"));
					}
					params.put("__VIEWSTATE", java.net.URLDecoder.decode(params.get("__VIEWSTATE"), "UTF-8"));
					params.put("__EVENTVALIDATION", java.net.URLDecoder.decode(params.get("__EVENTVALIDATION"), "UTF-8"));

				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				params.put("form_action", currentLink.replaceAll("&amp;", "&"));

				String key1 = getCurrentServerName() + ":params:" + lnk;
				String key2 = getCurrentServerName() + ":request_querry:" + lnk;
				mSearch.setAdditionalInfo(key1, params);
				mSearch.setAdditionalInfo(key2, querryString);

				Map<String, String> tempParam = new HashMap<String, String>();
				if (!isPrevLinkDisabled) {
					tempParam.put("__EVENTTARGET", previousEventTarget);
					prevLink = StringUtils.addParametersToUrl(nextLink, tempParam);
					linkTagPrevious.setLink(prevLink);
				}
				if (!isNextLinkDisabled) {
					tempParam.put("__EVENTTARGET", nextEventTarget);
					nextLink = StringUtils.addParametersToUrl(nextLink, tempParam);

					linkTagNext.setLink(nextLink);
					linkTagNext.removeAttribute("id");

					response.getParsedResponse().setNextLink(linkTagNext.toHtml());
				}
			
			} 
		
		} else {
			return "";
		}
		// outputTable.append(linkTable.toHtml());

		String intermHtmlPg = linkTable.toHtml();
		intermHtmlPg = intermHtmlPg.replaceFirst("(?is)(<a[^>]+>\\s*Next\\s*</a>)(?:\\s|&nbsp;)*(<a[^>]+>\\s*Previous\\s*</a>)", "$2" + "&nbsp;&nbsp;|&nbsp;&nbsp;" + "$1");
		intermHtmlPg = intermHtmlPg.replaceAll("(?is)<a[\\w_\\\"\\s=]+disabled\\s*=\\s*['\\\"]disabled['\\\"][^>]*>\\s*(\\w+)\\s*</a>", "$1");
		intermHtmlPg = intermHtmlPg.replaceAll("(?is)<a[\\w\\\"\\s_'=]+href=['\\\"]javascript[^>]+>(\\w+)\\s*</a>", "$1");
		intermHtmlPg = intermHtmlPg.replaceFirst("(?is)\\s*Previous(?:\\s|&nbsp;)+\\|(?:\\s|&nbsp;)+Next\\s*", "");
		
		return intermHtmlPg;
	}

	@SuppressWarnings("unused")
	private Map<String, String> keepParameters(Map<String, String> parameters, String[] ownerNameSearchParameters) {
		Object[] array = parameters.keySet().toArray();
		HashMap<String, String> hashMap = new HashMap<String, String>();
		for (String key : ownerNameSearchParameters) {
			hashMap.put(key, parameters.get(key));
		}
		return hashMap;
	}

	private ParsedResponse parseRow(TableRow[] rows, int i, int typeOfParse) {
		TableRow firstRow = rows[i];
		TableRow secondRow = rows[i + 1];
		TableRow thirdRow = rows[i + 2];

		ResultMap resultMap = new ResultMap();
		// set type of search
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
				
		Object defaultValue = new TextNode("");

		Node ownerName = firstRow.childAt(3).getLastChild();
		ownerName = (Node) ObjectUtils.defaultIfNull(ownerName, defaultValue);

		ParsedResponse currentResponse = new ParsedResponse();
		
		TableTag acccountInfo = (TableTag) firstRow.childAt(7).getChildren().elementAt(1);
		if (acccountInfo != null) {
			Node accountNumber = acccountInfo.getRow(0).getColumns()[0].getChild(1);
			if (accountNumber != null) {
				// rewrite the link
				LinkTag linkTag = null;
				if (accountNumber instanceof LinkTag) {
					linkTag = (LinkTag) accountNumber;
					String atsLink = CreatePartialLink(TSConnectionURL.idGET) + "/itm/" + linkTag.getLink();
					atsLink = atsLink.replaceAll("&amp;", "&");
					linkTag.setLink(atsLink);
					currentResponse.setPageLink(new LinkInPage(atsLink, atsLink, TSServer.REQUEST_SAVE_TO_TSD));
					
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNumber.getFirstChild().toPlainTextString().trim());
				}
			}
		}
		
		Node physicalAddress = secondRow.childAt(3).getLastChild();
		physicalAddress = (Node) ObjectUtils.defaultIfNull(physicalAddress, defaultValue);
		String address = physicalAddress.toPlainTextString().trim();

		String tmpOwnerName = ownerName.toPlainTextString();
		if (typeOfParse == ID_SEARCH_BY_ADDRESS) {
			String temp = "";
			temp = tmpOwnerName;
			tmpOwnerName = address;
			address = temp;
		}
		
		address = address.replaceAll("(?is)&nbsp;", " ");
		resultMap.put("tmpOwnerName", tmpOwnerName);
		resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
		ro.cst.tsearch.servers.functions.FLLeonTR.parseName(resultMap, tmpOwnerName);
		ro.cst.tsearch.servers.functions.FLLeonTR.parseAddress(resultMap, address);
		Node legalDescription = thirdRow.getChildren().elementAt(3);
		legalDescription = (Node) ObjectUtils.defaultIfNull(legalDescription, defaultValue);
		String tmpLegalDescription = legalDescription.toPlainTextString().replaceAll("&nbsp;", "").trim();
		resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), tmpLegalDescription);
		ro.cst.tsearch.servers.functions.FLLeonTR.parseLegalDescription(resultMap, tmpLegalDescription);
		String rowHtml = "<tr>" + firstRow.toHtml() + "</tr>" + "<tr>" + secondRow.toHtml() + "</tr>" + "<tr>" + thirdRow.toHtml()
				+ "</tr>" + "<tr>" + rows[i + 3].toHtml() + "</tr>";
		rowHtml = cleanServerResult(rowHtml);
		currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
		currentResponse.setOnlyResponse(rowHtml);

		resultMap.removeTempDef();
		Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
		DocumentI document = null;
		try {
			document = bridge.importData();
		} catch (Exception e) {
			e.printStackTrace();
		}
		currentResponse.setDocument(document);
		return currentResponse;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		HtmlParser3 parser = new HtmlParser3(detailsHtml);

		NodeList tableList = HtmlParser3.getTag(parser.getNodeList(), new TableTag(), true);

		// account number, address, sec,twn, rng status, subdivision
		Node generalData = tableList.elementAt(0);
		ro.cst.tsearch.servers.functions.FLLeonTR.parseDetailsGeneralData(generalData, map);

		// tax data
		ro.cst.tsearch.servers.functions.FLLeonTR.parseTaxHistorySet(parser.getNodeList(), map);

		String tmpLegalDescription = getLegalDescription(parser);
		ro.cst.tsearch.servers.functions.FLLeonTR.parseLegalDescription(map, tmpLegalDescription);
		List<String> ownerInformation = getOwnerInformation(parser);
		String tmpNames = org.apache.commons.lang.StringUtils.join(ownerInformation.toArray(), " AND ");
		ro.cst.tsearch.servers.functions.FLLeonTR.parseName(map, tmpNames);

		ro.cst.tsearch.servers.functions.FLLeonTR.parseSaleDataSet(parser.getNodeList(), map);

		// remove book page from CrossrefSet if it already exists in SaleDataSet
		if (map.get("CrossRefSet")!=null && map.get("SaleDataSet")!=null){
			ResultTable crossRefSet = (ResultTable) map.get("CrossRefSet");
			String[][] body = crossRefSet.getBody();

			String[][] saleDataSet = ((ResultTable) map.get("SaleDataSet")).getBodyRef();

			List<String> bookPageList = new ArrayList<String>();
			for (String[] bp : body) {
				boolean found = false;
				for (String[] row : saleDataSet) {
					String book = row[0];
					String page = row[1];
					if (book.equals(bp[1]) && page.equals(bp[2])) {
						found = true;
					}
				}
				if (!found) {
					bookPageList.add("" + bp[1] + "/" + bp[2]);
				}
			}
			ro.cst.tsearch.servers.functions.FLLeonTR.buildCrossRefSet(map, bookPageList);
		}

		return null;
	}

	private String getLegalDescription(HtmlParser3 parser) {
		TableTag legaldescrTable = (TableTag) HtmlParser3.getNodeByID("Table2a", parser.getNodeList(), true);

		TableRow[] rows = legaldescrTable.getRows();
		StringBuilder buildLegal = new StringBuilder();
		for (TableRow tableRow : rows) {
			Node firstChild = tableRow.getChild(1);
			buildLegal.append(" " + firstChild.toPlainTextString().replaceAll("&nbsp;", " ").trim());
		}
		return buildLegal.toString();
	}

	private List<String> getOwnerInformation(HtmlParser3 parser) {
		TableTag ownerInfoTable = (TableTag) HtmlParser3.getNodeByID("Table2b", parser.getNodeList(), true);

		TableRow[] rows = ownerInfoTable.getRows();
		List<String> linkedList = new LinkedList<String>();
		int i = 0;
		while (i < rows.length-2) {
			Node firstChild = rows[i].getChild(1).getFirstChild();
			String plainTextString = firstChild.toPlainTextString();
			plainTextString = plainTextString.replaceAll("&nbsp;", " ").trim();
//			Pattern compile = Pattern.compile("\\d+");
//			Matcher matcher = compile.matcher(plainTextString);
//			boolean matches = matcher.find();// Pattern.matches("\\d+",
			// plainTextString);;
//			if (!matches) {
				linkedList.add(plainTextString);
				i++;
//			} else {
//				i = rows.length;
//			}
		}
		return linkedList;
	}

	public String getAction() {
		synchronized (this) {
			return action;
		}
	}

	public void setAction(String action) {
		synchronized (this) {
			this.action = action;
		}
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		//String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
		//String streetDirection = getSearch().getSa().getAtribute(SearchAttributes.P_STREETDIRECTION);
		//String aptUnit = getSearch().getSa().getAtribute(SearchAttributes.P_STREETUNIT);

		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;

		TaxYearFilterResponse frYear = new TaxYearFilterResponse(searchId);
		frYear.setThreshold(new BigDecimal("0.95"));

		PinFilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d);
		GenericLegal legalUnitFilter = (GenericLegal) LegalFilterFactory.getDefaultUnitFilter(searchId);
		legalUnitFilter.setEnableLotUnitFullEquivalence(true);
		FilterResponse nameFilterNoSinonims = NameFilterFactory.getDefaultNameFilterNoSinonims(SearchAttributes.OWNER_OBJECT, searchId, null);

		if (hasPin()) {

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.addFilter(pinFilter);
			modules.add(module);
		}

		if (hasStreet()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.addFilterForNextType(FilterResponse.TYPE_ADDRESS_FOR_NEXT);
			module.addFilter(legalUnitFilter);
			module.addFilter(addressFilter);
			module.addFilter(pinFilter);
			//module.getFunction(0).forceValue(streetNo);
			module.getFunction(1).forceValue(streetName);
			//module.getFunction(3).forceValue(streetDirection);
			//module.getFunction(4).forceValue(aptUnit);
			module.getFunction(2).forceValue(" ");
			module.getFunction(5).forceValue(" ");

			modules.add(module);
		}

		if (hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
			// module.addFilter(frYear);
			module.addFilter(nameFilterNoSinonims);
			// module.addFilter(pinFilter);
			module.addFilter(legalUnitFilter);
			module.addFilter(addressFilter);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(
					module, searchId, new String[] { "L;F;" });

			module.addIterator(nameIterator);
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
}
