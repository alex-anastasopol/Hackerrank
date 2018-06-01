package ro.cst.tsearch.connection.http3;

import java.io.ByteArrayInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.Tidy;

public class OHFairfieldRO extends HttpSite3 {
	String	link			= "";
	String	pdfContentType	= "application/pdf";

	@Override
	public LoginResponse onLogin() {
		DataSite dataSite = getDataSite();
		HTTPRequest request = new HTTPRequest(dataSite.getLink());
		HTTPResponse res = process(request);

		String response = res.getResponseAsString();
		if (!response.isEmpty()) {

			HtmlParser3 htmlParser3 = new HtmlParser3(response);
			NodeList nodeList = htmlParser3.getNodeList();
			if (nodeList.size() > 0) {
				request = new HTTPRequest(dataSite.getServerHomeLink(), HTTPRequest.POST);
				Node form = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("name", "commonForm"), true).elementAt(0);
				if (form != null) {
					link = form.toHtml().replaceFirst("(?is)<form[^>]*ACTION=\"([^\\\"]*)\"[^>]*>.*", "$1");
					link = request.getURL() + link.substring(1, link.length());
					request.setURL(link);
				}
				request.setPostParameter("officeid", "1");// first 4 modules in PS search in Recorded land office type
				request.setPostParameter("optflag", "MenuCommand");
				request.setPostParameter("commandflag", "index.jsp");
				request.setPostParameter("countypage", "true");

				res = process(request);
				response = res.getResponseAsString();
				if (response.contains("Welcome to the Fairfield County Real Property") ||
						(response.contains("Business/Last Name") && response.contains("Name Search"))) {
					return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
				}
			}
		}

		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		Object officeid = "";
		String url = req.getURL();
		if ((officeid = req.getPostParameter("officeid")) != null && status == STATUS_LOGGED_IN && req.getMethod() == HTTPRequest.POST) {
			req.removePostParameters("officeid");
			// get search form for Recorded Land/UCC office type
			getSearchForm(officeid.toString());
		}
		else if (req.getMethod() == HTTPRequest.GET && status == STATUS_LOGGED_IN
				&& ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(req.getURL(), "commandflag").equals("getDocument")) {

			getImageLink(req);
			req.setReferer(url);
		}
	}

	private void getImageLink(HTTPRequest req) {
		// get PDF image link
		String url = req.getURL();
		HTTPRequest initialRequest = new HTTPRequest(url);
		HTTPResponse initialResponse = process(initialRequest);

		HtmlParser3 htmlParser3 = new HtmlParser3(initialResponse.getResponseAsString());
		NodeList nodeList = htmlParser3.getNodeList();
		if (nodeList.size() > 0) {
			Node iframe = nodeList.extractAllNodesThatMatch(new TagNameFilter("iframe"), true).elementAt(0);

			if (iframe != null) {
				String imageLink = iframe.toHtml().replaceFirst("(?is).*?src=\"([^\"]*)\".*", "$1");
				req.setURL(dataSite.getServerHomeLink() + imageLink.substring(1));
			}
		}
	}

	public byte[] getImage(String imageLink, boolean saveForDTG) {
		byte[] imageBytes = null;
		try {
			onLogin();

			String serverHomeLink = dataSite.getServerHomeLink();

			String year = "";
			String url = serverHomeLink + "ohlrf3/controller";
			String instrumentNumber = "";

			if (saveForDTG) {// there are two cases: get image for RO or for DTG;
				year = ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(imageLink, "year");
			}
			else {
				instrumentNumber = ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(imageLink, "instrumentnumber");
				year = instrumentNumber.substring(0, 4);
			}

			HTTPRequest intermediaryRequest = new HTTPRequest(url);

			intermediaryRequest.setMethod(TSConnectionURL.idPOST);
			intermediaryRequest.setPostParameter("optflag", "SearchCommand");
			intermediaryRequest.setPostParameter("doctype", "All");
			intermediaryRequest.setPostParameter("inputbutton", "Search Now");
			intermediaryRequest.setPostParameter("rowincrement", "25");

			if (saveForDTG) {
				String book = ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(imageLink, "book");
				String page = ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(imageLink, "page");

				intermediaryRequest.setPostParameter("volume", book.replaceFirst("\\A[A-Z]", ""));
				intermediaryRequest.setPostParameter("pagefrom", page);
				intermediaryRequest.setPostParameter("pageto", page);
				intermediaryRequest.setPostParameter("searchType", "searchByBookPage");
				intermediaryRequest.setPostParameter("commandflag", "searchByBookPage");

			}
			else {
				intermediaryRequest.setPostParameter("instfrom", instrumentNumber.replaceFirst("^\\d{4}0*", ""));
				intermediaryRequest.setPostParameter("searchType", "searchByInstrument");
				intermediaryRequest.setPostParameter("commandflag", "searchByInstrument");

			}
			HTTPResponse intermediaryResponse = process(intermediaryRequest);
			String intermediariesAsString = intermediaryResponse.getResponseAsString();

			if (!StringUtils.defaultString(intermediariesAsString).trim().isEmpty()) {
				intermediariesAsString = Tidy.tidyParse(intermediariesAsString, null);
				intermediariesAsString = StringEscapeUtils.unescapeHtml(intermediariesAsString).replaceAll("[^\\p{Alnum}\\p{Punct}\\s]", "");

				HtmlParser3 htmlParser3 = new HtmlParser3(intermediariesAsString);

				Node row1 = htmlParser3.getNodeById("myRow1");
				if (row1 != null) {
					TableTag resultsTable = (TableTag) HtmlParser3.getFirstParentTag(row1, TableTag.class);
					TableRow[] resultRows = resultsTable.getRows();

					for (int i = 1; i < resultRows.length; i++) {

						TableColumn[] columns = resultRows[i].getColumns();
						String date = columns[3].toPlainTextString().trim();
						if (date.matches("\\d+/\\d+/\\d+")) {
							if (date.replaceFirst(".*/(\\d{4})$", "$1").equals(year)) {
								Node detailsLinkNode = columns[1].getFirstChild();
								if (detailsLinkNode != null && detailsLinkNode instanceof LinkTag) {
									String detailsLink = ((LinkTag) detailsLinkNode).getLink();
									HTTPRequest detailsRequest = new HTTPRequest(serverHomeLink + detailsLink);
									HTTPResponse detailsResponse = process(detailsRequest);
									String detailsAsString = detailsResponse.getResponseAsString();
									if (!StringUtils.defaultString(detailsAsString).trim().isEmpty()) {
										String pdfImageLink = getPdfLinkFromDetails(detailsAsString);

										if (!pdfImageLink.isEmpty()) {

											HTTPRequest pdfImageRequest = new HTTPRequest(serverHomeLink + pdfImageLink);
											HTTPResponse pdfImageResponse = process(pdfImageRequest);

											imageBytes = pdfImageResponse.getResponseAsByte();
											if (imageBytes.length > 0) {
												String pdfString = new String(imageBytes).trim();
												if (pdfString.length() >= 10) {
													pdfString = pdfString.substring(0, 10).toUpperCase();
												}

												if (pdfString.startsWith("%PDF")) {
													pdfImageResponse.contentType = pdfContentType;
													pdfImageResponse.is = new ByteArrayInputStream(imageBytes);
												}
											}
										}
									}
								}
								break;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return imageBytes;
	}

	private void getSearchForm(String officeid) {
		if (!link.isEmpty()) {
			HTTPRequest request = new HTTPRequest(dataSite.getServerHomeLink(), HTTPRequest.POST);
			request.setPostParameter("officeid", officeid);
			request.setPostParameter("optflag", "MenuCommand");
			request.setPostParameter("commandflag", "index.jsp");
			request.setPostParameter("countypage", "true");
			request.setURL(link);
			process(request);
		}
	}

	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
		if (req.getURL().contains("imageCacheOHLR/PdfCache") && req.getMethod() == HTTPRequest.GET && status == STATUS_LOGGED_IN) {

			// pdf image response from server doesn't have content type header
			byte[] pdfBytes = res.getResponseAsByte();
			if (pdfBytes.length > 0) {
				String pdfString = new String(pdfBytes).trim();
				if (pdfString.length() >= 10) {
					pdfString = pdfString.substring(0, 10).toUpperCase();
				}

				if (!res.getContentType().equals(pdfContentType) && pdfString.startsWith("%PDF")) {
					res.contentType = pdfContentType;
					res.is = new ByteArrayInputStream(pdfBytes);
				}
			}
		}
	}

	public String getPdfLinkFromDetails(String rsResponse) {
		// get PDF image link
		String pdfLink = "";
		HtmlParser3 htmlParser3 = new HtmlParser3(rsResponse);
		Node formDetails = htmlParser3.getNodeById("frmdetail");
		if (formDetails != null) {
			NodeList inputList = formDetails.getChildren().extractAllNodesThatMatch(new TagNameFilter("input"));
			int inputListSize = inputList.size();
			if (inputListSize > 0) {
				String formParams = "";

				for (int i = 0; i < inputListSize; i++) {
					Node input = inputList.elementAt(i);
					if (input != null) {
						formParams += input.toHtml().replaceFirst("(?is)<input[^>]*name=\"([^\"]*)\"[^>]*value=\"?([^\">]*)\"?.*", "&$1=$2");
					}
				}

				Pattern pat = Pattern.compile("(?is)var\\s+url\\s*=\\s*'([^']*)'[^']*'([^']*?)&?'");
				Matcher mat = pat.matcher(rsResponse);

				if (mat.find()) {
					pdfLink += "ohlrf3/" + mat.group(1) + mat.group(2) + formParams;

				}
			}
		}
		return pdfLink;
	}
}