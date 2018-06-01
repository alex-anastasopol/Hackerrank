package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.parser.HtmlParser3;

/**
 * 
 * @author Oprina George
 * 
 *         Nov 1, 2012
 */

public class ALMontgomeryTR extends HttpSite {

	public static String[]			paramsNames	= new String[] { "__EVENTTARGET", "__EVENTARGUMENT", "__LASTFOCUS", "__VIEWSTATE", "__EVENTVALIDATION" };

	private HashMap<String, String>	aspxParams	= new HashMap<String, String>();

	public LoginResponse onLogin() {
		try {
			HTTPRequest req = new HTTPRequest(getDataSite().getServerHomeLink() + "CA_PropertyTaxSearch.aspx");

			HTTPResponse resp = process(req);

			if (resp.getResponseAsString().contains("Parcel #")) {

				FormTag f = getFormFromResponse("thisForm", resp);

				if (f != null) {
					NodeList inputs = f.getFormInputs();

					for (int i = 0; i < inputs.size(); i++) {
						String name = ((InputTag) inputs.elementAt(i)).getAttribute("name");
						String value = ((InputTag) inputs.elementAt(i)).getAttribute("value");

						if (StringUtils.isNotEmpty(name)) {
							aspxParams.put(name, StringUtils.defaultString(value));
						}
					}
				}
				return LoginResponse.getDefaultSuccessResponse();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return LoginResponse.getDefaultFailureResponse();
	}

	public void onBeforeRequestExcl(HTTPRequest req) {
		return;
	}

	public void onBeforeRequest(HTTPRequest req) {
		if (req.getMethod() == HTTPRequest.POST) {
			if (req.hasPostParameter("seq") && req.hasPostParameter("fakeParam")) {
				String[] otherParams = new String[] { "HiddenVal", "HidParcelNo", "SearchText", "SearchRadio", "TaxYear" };

				String seq = req.getPostFirstParameter("seq");
				req.removePostParameters("seq");

				@SuppressWarnings("unchecked")
				Map<String, String> params = (Map<String, String>) getTransientSearchAttribute("params:" + seq);

				for (String s : paramsNames) {
					req.setPostParameter(s, StringUtils.defaultString(params.get(s)));
				}

				for (String s : otherParams) {
					req.setPostParameter(s, StringUtils.defaultString(params.get(s)));
				}

				String NextRecord = req.getPostFirstParameter("fakeParam");

				req.removePostParameters("fakeParam");

				req.setPostParameter("NextRecord", NextRecord);

			} else {
				for (String s : paramsNames) {
					req.setPostParameter(s, StringUtils.defaultString(aspxParams.get(s)));
				}

				if (req.hasPostParameter("SearchRadio") && req.getPostFirstParameter("SearchRadio").equals("SearchByParcel")) {
					req.removePostParameters("HidParcelNo");

					String parcel = req.getPostFirstParameter("SearchText");

					if (StringUtils.isNotEmpty(parcel)) {
						String hidParcelNo = parcel.substring(0, 2) + " " +
								(parcel.length() >= 4 ? parcel.substring(2, 4) + " " : "") +
								(parcel.length() >= 6 ? parcel.substring(4, 6) + " " : "") +
								(parcel.length() >= 7 ? parcel.substring(6, 7) + " " : "") +
								(parcel.length() >= 10 ? parcel.substring(7, 10) + " " : "") +
								(parcel.length() >= 13 ? parcel.substring(10, 13) + "." : "") +
								(parcel.length() >= 13 ? parcel.substring(13) : "");

						req.setPostParameter("HidParcelNo", hidParcelNo);
					}
				}
			}
		} else {
			if (req.getURL().contains("ParcelNo")) {

			}
		}
	}

	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
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
}
