package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import static ro.cst.tsearch.connection.http.HTTPRequest.POST;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.io.IOUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.auth.ssl.EasySSLProtocolSocketFactory;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.utils.StringUtils;

/**
 * 
 * @author Oprina George
 * 
 *         Oct 14, 2011
 */

public class TXBexarRO extends HttpSite {
	private String			certDateResp	= "";

	public static String	SERVER_LINK		= "";

	public String getCertDateResp() {
		return certDateResp;
	}

	@Override
	public LoginResponse onLogin() {
		@SuppressWarnings("deprecation")
		Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
		Protocol.registerProtocol("https", easyhttps);

		// get server_link
		HTTPRequest req4Login = new HTTPRequest(getDataSite().getLink());

		String resp4Login = execute(req4Login);

		if (StringUtils.isEmpty(resp4Login)) {
			return LoginResponse.getDefaultFailureResponse();
		}

		try {
			NodeList inputs = new HtmlParser3(resp4Login).getNodeList().extractAllNodesThatMatch(new TagNameFilter("input"), true);

			if (inputs != null) {
				inputs = inputs.extractAllNodesThatMatch(new HasAttributeFilter("src", "Images/button_login.gif"));

				if (inputs.size() == 1) {
					InputTag in = (InputTag) inputs.elementAt(0);
					String newSR = in.getAttribute("onclick").replaceAll("[^\\(]+\\(([^\\)]+)\\)[^$]+", "$1").split(",")[0].replace("'", "");
					if (StringUtils.isNotEmpty(newSR)) {
						SERVER_LINK = newSR;
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (StringUtils.isEmpty(SERVER_LINK)) {
			return LoginResponse.getDefaultFailureResponse();
		}

		// pentru cookies
		HTTPRequest reqGetLoginForm = new HTTPRequest(SERVER_LINK + "loginForm.asp?iWAMid=3");

		String respGetLogin = execute(reqGetLoginForm);

		if (!(respGetLogin.contains("User ID") && respGetLogin.contains("Password"))) {
			return LoginResponse.getDefaultFailureResponse();
		}

		HTTPRequest reqLogin = new HTTPRequest(SERVER_LINK + "loginVerify.asp", POST);

		String user = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TXBexarRO", "user");
		String pass = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "TXBexarRO", "password");

		if (StringUtils.isEmpty(user) || StringUtils.isEmpty(pass)) {
			return new LoginResponse(LoginStatus.STATUS_INVALID_CREDENTIALS, "Couldn't get credentials from database");
		}

		reqLogin.setPostParameter("iWAMid", "3");
		reqLogin.setPostParameter("txtUserName", user);
		reqLogin.setPostParameter("txtPassword", pass);
		reqLogin.setPostParameter("validationCode", "");
		reqLogin.setPostParameter("x", "10");
		reqLogin.setPostParameter("y", "5");

		String respLogin = execute(reqLogin);

		if (respLogin.contains("Error"))
			return LoginResponse.getDefaultInvalidCredentialsResponse();

		this.certDateResp = respLogin;

		return LoginResponse.getDefaultSuccessResponse();
	}

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		String url = req.getURL();

		if (req.getMethod() == GET) {

		} else if (req.getMethod() == POST) {

			// merge param searchDocType
			ParametersVector vect = req.getPostParameter("SearchDocType");

			if (vect != null && vect.size() > 1) {
				req.removePostParameters("SearchDocType");
				req.setPostParameter("SearchDocType", vect.toString());
				// req.removePostParameters("availSearchDocType");
				// req.setPostParameter("availSearchDocType", vect.lastElement().toString());
			}

			if (url.equals(SERVER_LINK + "SearchResults.asp")) {
				// get intermediary results
				String typeOfSearch = req.getPostFirstParameter("iSearchNum");

				if (StringUtils.isNotEmpty(typeOfSearch)) {

					// get search criteria
					HTTPRequest reqGetCriteria = new HTTPRequest(SERVER_LINK + "SearchCriteria.asp?iApplNum=17001&iSearchNum=" + typeOfSearch);

					HTTPResponse respGetCriteria = process(reqGetCriteria);

					// get frmCriteria
					FormTag frmCriteria = getFormFromResponse("frmCriteria", respGetCriteria);
					FormTag frmSavedCriteria = getFormFromResponse("frmSavedCriteria", respGetCriteria);

					// post to search criteria for land records
					HTTPRequest reqPostCriteria = new HTTPRequest(SERVER_LINK + "SearchCriteria.asp", POST);
					if (frmCriteria != null && frmSavedCriteria != null) {
						// put parameters to request
						NodeList inputs = frmCriteria.getFormInputs();

						reqPostCriteria.setPostParameters(new HashMap<String, HTTPRequest.ParametersVector>(req.getPostParameters()));
						if (inputs.size() > 0) {
							for (int i = 0; i < inputs.size(); i++) {
								String name = org.apache.commons.lang.StringUtils.defaultString(((InputTag) inputs.elementAt(i)).getAttribute("name"));
								String value = org.apache.commons.lang.StringUtils.defaultString(((InputTag) inputs.elementAt(i)).getAttribute("value"));
								if (StringUtils.isNotEmpty(name) && !reqPostCriteria.hasPostParameter(name)) {
									reqPostCriteria.setPostParameter(name, value);
								}
							}
						}
						reqPostCriteria.setPostParameter("iRecordsPerPage", "100");
						reqPostCriteria.setPostParameter("iUserSearchID", "0");
						reqPostCriteria.setPostParameter("iDataSetNum", reqPostCriteria.getPostFirstParameter("iApplNum"));
						reqPostCriteria.setPostParameter("tiBatch", "0");
						reqPostCriteria.setPostParameter("tiPhoneBk", "0");
						reqPostCriteria.setPostParameter("x", "12");
						reqPostCriteria.setPostParameter("y", "3");

						if (typeOfSearch.equals("17004")) {
							reqPostCriteria.removePostParameters("x");
							reqPostCriteria.removePostParameters("y");
							reqPostCriteria.removePostParameters("SearchbyGtrGte");

							String searchDocType = reqPostCriteria.getPostFirstParameter("SearchDocType");
							if (searchDocType.contains("ALL")) {
								reqPostCriteria.removePostParameters("SearchDocType");
								reqPostCriteria.setPostParameter("SearchDocType", "");
							}

						}
					} else {
						return;
					}

					HTTPResponse respPostCriteria = process(reqPostCriteria);

					FormTag frmCriteria1 = getFormFromResponse("frmCriteria", respPostCriteria);
					FormTag frmSavedCriteria1 = getFormFromResponse("frmSavedCriteria", respPostCriteria);

					String respPostCriteriaString = respPostCriteria.getResponseAsString();

					if (respPostCriteriaString.contains("Your search returned no records.")) {
						respPostCriteria.body = respPostCriteriaString;
						respPostCriteria.contentLenght = respPostCriteriaString.length();
						respPostCriteria.is = org.apache.commons.io.IOUtils.toInputStream(respPostCriteriaString);
						req.setBypassResponse(respPostCriteria);
						return;
					}

					// post to search summary
					HTTPRequest reqPostSearchSummary = new HTTPRequest(SERVER_LINK + "SearchSummary.asp", POST);
					if (frmCriteria1 != null && frmSavedCriteria1 != null) {
						// put parameters to request
						NodeList inputs = frmCriteria1.getFormInputs();

						reqPostSearchSummary.setPostParameters(req.getPostParameters());

						if (inputs.size() > 0) {
							for (int i = 0; i < inputs.size(); i++) {
								String name = org.apache.commons.lang.StringUtils.defaultString(((InputTag) inputs.elementAt(i)).getAttribute("name"));
								String value = org.apache.commons.lang.StringUtils.defaultString(((InputTag) inputs.elementAt(i)).getAttribute("value"));
								if (StringUtils.isNotEmpty(name) && !reqPostSearchSummary.hasPostParameter(name)) {
									reqPostSearchSummary.setPostParameter(name, value);
								}

							}
						}
						reqPostSearchSummary.setPostParameter("iRecordsPerPage", "100");
						reqPostSearchSummary.setPostParameter("iUserSearchID", "0");
						reqPostSearchSummary.setPostParameter("iDataSetNum", reqPostSearchSummary.getPostFirstParameter("iApplNum"));
						reqPostSearchSummary.setPostParameter("tiBatch", "0");
						reqPostSearchSummary.setPostParameter("tiPhoneBk", "0");
						reqPostSearchSummary.setPostParameter("x", "12");
						reqPostSearchSummary.setPostParameter("y", "3");

						if (typeOfSearch.equals("17004")) {
							String searchDocType = reqPostSearchSummary.getPostFirstParameter("SearchDocType");
							if (searchDocType.contains("ALL,")) {
								reqPostSearchSummary.removePostParameters("SearchDocType");
								reqPostSearchSummary.setPostParameter("SearchDocType", "ALL");
							}

						}
					} else {
						return;
					}

					HTTPResponse respPostSearchSummary = process(reqPostSearchSummary);

					FormTag frmResult = getFormFromResponse("frmResult", respPostSearchSummary);

					// post to search results => make this request ok
					if (frmResult != null) {
						// put parameters to request
						NodeList inputs = frmResult.getFormInputs();
						if (inputs.size() > 0) {
							for (int i = 0; i < inputs.size(); i++) {
								String name = org.apache.commons.lang.StringUtils.defaultString(((InputTag) inputs.elementAt(i)).getAttribute("name"));
								String value = org.apache.commons.lang.StringUtils.defaultString(((InputTag) inputs.elementAt(i)).getAttribute("value"));
								if (!req.hasPostParameter(name)) {
									req.setPostParameter(name, value);
								}
							}
						}
					} else {
						HTTPResponse resp = new HTTPResponse();

						resp.body = "<html><head></head><body><div>Error<div></body></html>";
						resp.contentLenght = resp.body.length();
						resp.contentType = "text/html;";
						resp.is = IOUtils.toInputStream(resp.body);
						resp.returnCode = 200;
						
						req.setBypassResponse(resp);
					}
				}
			} else if (url.contains("viewImageFake.asp")) {
				// do post
				HTTPRequest reqViewImg = new HTTPRequest(url.replace("viewImageFake.asp", "viewImage.asp"), POST);

				reqViewImg.setPostParameters(new HashMap<String, HTTPRequest.ParametersVector>(req.getPostParameters()));

				HTTPResponse respViewImg = process(reqViewImg);

				FormTag frmImg = getFormFromResponse("frmViewImage", respViewImg);

				if (frmImg != null) {
					NodeList inputs = frmImg.getFormInputs();
					if (inputs.size() > 0) {

						// get temp image
						HTTPRequest reqImg = new HTTPRequest(SERVER_LINK + "/ViewTIFFJava/viewDocument_WAM3service.asp", POST);

						for (int i = 0; i < inputs.size(); i++) {
							String name = org.apache.commons.lang.StringUtils.defaultString(((InputTag) inputs.elementAt(i)).getAttribute("name"));
							String value = org.apache.commons.lang.StringUtils.defaultString(((InputTag) inputs.elementAt(i)).getAttribute("value"));
							reqImg.setPostParameter(name, value);
						}

						HTTPResponse respImg = process(reqImg);

						HashMap<String, String> image_params = getImageParams(respImg.getResponseAsString());

						if (StringUtils.isNotEmpty(image_params.get("docname")) && StringUtils.isNotEmpty(image_params.get("BufferVol"))) {
							HTTPRequest reqBytes = new HTTPRequest(image_params.get("BufferVol") + "/" + image_params.get("docname"), GET);

							HTTPResponse respBytes = process(reqBytes);
							respBytes.is = new ByteArrayInputStream(respBytes.getResponseAsByte());

							req.setBypassResponse(respBytes);
						}
					}
				}
			}
		}
	}

	private FormTag getFormFromResponse(String name, HTTPResponse resp) {
		FormTag f = null;

		String respString = resp.getResponseAsString();

		if (StringUtils.isNotEmpty(respString)) {
			NodeList nodes = new HtmlParser3(respString).getNodeList();
			try {
				nodes = nodes.extractAllNodesThatMatch(new TagNameFilter("form"), true).extractAllNodesThatMatch(new HasAttributeFilter("name", name));
				if (nodes.size() > 0) {
					f = (FormTag) nodes.elementAt(0);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return f;
	}

	private int	tries	= 0;

	@Override
	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {

		if (res != null &&
				res.getLastURI() != null &&
				res.getLastURI().getEscapedURI() != null &&
				(res.getLastURI().getEscapedURI().contains("ProcessDroppedSession.asp") ||
				res.getLastURI().getEscapedURI().contains("Error") ||
				res.returnCode == 500)) {
			if (tries == 2) {
				tries = 0;
				
				res.is = IOUtils.toInputStream("<html>error</html>");
				res.body = "<html>error</html>";
				res.contentLenght = res.body.length();
				res.returnCode = 200;
				
				return;
			}

			onLogin();
			tries++;
			HTTPResponse resp = process(req);

			res.body = resp.body;
			res.contentLenght = resp.contentLenght;
			res.returnCode = resp.returnCode;
			res.is = new ByteArrayInputStream(resp.getResponseAsByte());
		}
	}

	public byte[] getImage(String lnk) {
		try {
			if (lnk.contains("book") && lnk.contains("page") && lnk.contains("type")) {
				// make a BP search
				HTTPRequest reqSearchBP = new HTTPRequest(SERVER_LINK + "SearchResults.asp", HTTPRequest.POST);

				lnk = lnk + "&";

				reqSearchBP.setPostParameter("SearchbyBook", lnk.replaceAll(".*book=([^&]*)\\&.*", "$1").trim());
				reqSearchBP.setPostParameter("SearchbyPageFrom", lnk.replaceAll(".*page=([^&]*)\\&.*", "$1").trim());
				reqSearchBP.setPostParameter("SearchDocType", lnk.replaceAll(".*type=([^&]*)\\&.*", "$1").trim());

				reqSearchBP.setPostParameter("SearchbyPageTo", "");
				reqSearchBP.setPostParameter("SearchbyDateFrom", "");
				reqSearchBP.setPostParameter("SearchbyDateTo", "");
				reqSearchBP.setPostParameter("SearchbyGtrGte", "");
				reqSearchBP.setPostParameter("iSearchNum", "17003");
				reqSearchBP.setPostParameter("SearchBookType", "ALL");

				HTTPResponse respSearchBP = process(reqSearchBP);

				FormTag frmResult = getFormFromResponse("frmResult", respSearchBP);

				if (frmResult != null) {
					HTTPRequest reqImg = new HTTPRequest(SERVER_LINK + "viewImageFake.asp", POST);

					NodeList inputs = frmResult.getFormInputs();

					if (inputs.size() > 0) {
						for (int i = 0; i < inputs.size(); i++) {
							String name = org.apache.commons.lang.StringUtils.defaultString(((InputTag) inputs.elementAt(i)).getAttribute("name"));
							String value = org.apache.commons.lang.StringUtils.defaultString(((InputTag) inputs.elementAt(i)).getAttribute("value"));
							reqImg.setPostParameter(name, value);
						}

						NodeList nodes = new HtmlParser3(respSearchBP.getResponseAsString()).getNodeList();
						NodeList mainTableList = nodes
								.extractAllNodesThatMatch(new TagNameFilter("div"), true)
								.extractAllNodesThatMatch(new HasAttributeFilter("id", "results"), true)
								.extractAllNodesThatMatch(new TagNameFilter("table"), true);
						if (mainTableList.size() > 0) {
							TableTag t = (TableTag) mainTableList.elementAt(0);
							TableRow[] rows = t.getRows();

							if (rows.length > 1 && rows[1].getColumnCount() == 14) {
								String link = rows[1].getColumns()[1].toHtml();

								String linkAux = link.replaceAll("(?ism).*<a href=\"([^\"]*)\"[^>]*>.*", "$1");

								String[] seq_num = linkAux.replaceAll("javascript:viewImage\\(([^)]*)\\);", "$1").replace("'", "").split(",");

								if (seq_num.length == 3) {
									reqImg.removePostParameters("iSequence");
									reqImg.setPostParameter("iSequence", seq_num[0]);
									reqImg.removePostParameters("iRowNumber");
									reqImg.setPostParameter("iRowNumber", seq_num[1]);
								}
							}
						}
					}

					HTTPResponse resImg = process(reqImg);

					if (resImg.returnCode != 200)
						return null;
					return resImg.getResponseAsByte();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private HashMap<String, String> getImageParams(String resp) {
		HashMap<String, String> params = new HashMap<String, String>();

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp, null);
			NodeList nodeList = htmlParser.parse(null);

			nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("param"), true);

			if (nodeList.size() > 0) {
				for (int i = 0; i < nodeList.size(); i++) {
					if (nodeList.elementAt(i) instanceof TagNode) {
						TagNode node = (TagNode) nodeList.elementAt(i);
						params.put(node.getAttribute("name") != null ? node.getAttribute("name") : "",
								node.getAttribute("value") != null ? node.getAttribute("value") : "");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return params;
	}

	public String getCertDate() {
		if (this.status != STATUS_LOGGED_IN)
			onLogin();
		return getCertDateResp();
	}
}
