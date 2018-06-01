package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.Map.Entry;

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

public class FLOrangeAO extends HttpSite {

	private HashMap<String, String>	aspxParams	= new HashMap<String, String>();

	public static String[]			paramsNames	= new String[] { "__EVENTTARGET", "__EVENTARGUMENT", "__LASTFOCUS", "__VIEWSTATE", "__VIEWSTATEENCRYPTED",
												"__ASYNCPOST" };

	public LoginResponse onLogin() {
		try {
			HTTPRequest req = new HTTPRequest(getDataSite().getServerHomeLink() + "searches/ParcelSearch.aspx");

			HTTPResponse resp = process(req);

			String respString = resp.getResponseAsString();

			// save params

			FormTag f = getFormFromResponse("aspnetForm", resp);

			if (f != null) {

				NodeList inputs = f.getFormInputs();

				for (int i = 0; i < inputs.size(); i++) {
					String name = ((InputTag) inputs.elementAt(i)).getAttribute("name");
					String value = ((InputTag) inputs.elementAt(i)).getAttribute("value");

					if (StringUtils.isNotEmpty(name)) {
						aspxParams.put(name, StringUtils.defaultString(value));
					}
				}

				if (respString.contains("Quick Searches"))
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

	private void putParams(HTTPRequest req, boolean withEventParams) {
		req.setPostParameter(
				"ctl00_ctl00_ctl00_ctl00_ToolkitScriptManager1_HiddenField",
				";;AjaxControlToolkit, Version=4.1.60501.15526, Culture=neutral, PublicKeyToken=28f01b0e84b6d53e:en-US:dae6eecb-d8f6-4977-bc30-6932e2055ee3:475a4ef5:effe2a26:8e94f951:1d3ed089:5546a2b:d2e10b12:37e2e5c9:5a682656:12bbc599:2a35a54f:4355a41");
		req.setPostParameter(
				"ctl00_ctl00_ctl00_ctl00_ContentMain_ContentMain_ContentMain_ContentMain_TabContainer1_ClientState",
				"{\"ActiveTabIndex\":0,\"TabState\":[true,true,true]}");
		req.setPostParameter(
				"ctl00_ctl00_ctl00_ctl00_ContentMain_ContentMain_ContentMain_ContentMain_TabContainer1_Searches_SubTabContainer1_ClientState",
				"{\"ActiveTabIndex\":0,\"TabState\":[true,true,true,true,true,true]}");
		req.setPostParameter(
				"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$PropertyUseSearch$PropertyUseSearch1$ValidationGroupPanel1$ValueType",
				"assessed");
		req.setPostParameter(
				"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$VacantPropertySearch$PropertyUseSearch2$ValidationGroupPanel1$DORUseCodes1$PropertyUseCodes",
				"1000|1019|1003|7000|4000|0000-0099|1004");
		req.setPostParameter(
				"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$VacantPropertySearch$PropertyUseSearch2$ValidationGroupPanel1$ValueType",
				"assessed");
		req.setPostParameter(
				"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$ForeclosureSearch$ForeclosureByZipSearch1$ValidationGroupPanel1$RecordType",
				"S");
		req.setPostParameter(
				"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$SalesSearch$SalesSearch1$ValidationPanel1$SaleType",
				"All");
		req.setPostParameter(
				"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$SalesSearch$SalesSearch1$ValidationPanel1$IncludeSmallSales",
				"0");
		req.setPostParameter(
				"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$SalesSearch$SalesSearch1$ValidationPanel1$DateType",
				"Period");
		req.setPostParameter(
				"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$SalesSearch$SalesSearch1$ValidationPanel1$SalesRange",
				"12");

		req.setPostParameter("hiddenInputToUpdateATBuffer_CommonToolkitScripts", "0");
		req.setPostParameter("ctl00$ctl00$ctl00$ctl00$ContentMain$ctl06$PopupPanel1$ctl01", "false");

		if (withEventParams) {
			for (String s : paramsNames) {
				req.setPostParameter(s, StringUtils.defaultString(aspxParams.get(s)));
			}
		}
	}

	public void onBeforeRequest(HTTPRequest req) {
		if (req.getMethod() == HTTPRequest.POST) {
			if (req.hasPostParameter("seq")) {
				try {
					String seq = req.getPostFirstParameter("seq");
					req.removePostParameters("seq");

					@SuppressWarnings("unchecked")
					HashMap<String, String> params = (HashMap<String, String>) getTransientSearchAttribute("params:" + seq);

					for (Entry<String, String> e : params.entrySet()) {
						req.setPostParameter(StringUtils.defaultString(e.getKey()), StringUtils.defaultString(e.getValue()));
					}

					putParams(req, false);

					String eventTar = req.getPostFirstParameter("eventTarget");
					String eventArg = req.getPostFirstParameter("eventArg");

					req.removePostParameters("eventTarget");
					req.removePostParameters("eventArg");
					req.removePostParameters("__EVENTTARGET");
					req.removePostParameters("__EVENTARGUMENT");

					req.setPostParameter("__EVENTTARGET", eventTar);
					req.setPostParameter("__EVENTARGUMENT", eventArg);

				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				if (req.hasPostParameter("ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$OwnerNameSearch1$ctl00$OwnerName")) {
					req.setPostParameter(
							"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$OwnerNameSearch1$ctl00$ActionButton1",
							"Search");
					req.setPostParameter(
							"ctl00$ctl00$ctl00$ctl00$ToolkitScriptManager1",
							"ctl00$ctl00$ctl00$ctl00$ToolkitScriptManager1|ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$OwnerNameSearch1$ctl00$ActionButton1");

					putParams(req, true);
				} else if (req
						.hasPostParameter("ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$CompositAddressSearch1$AddressSearch1$ctl00$Address")) {
					req.setPostParameter(
							"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$CompositAddressSearch1$AddressSearch1$ctl00$ActionButton1",
							"Search");
					putParams(req, true);

					req.setPostParameter(
							"ctl00$ctl00$ctl00$ctl00$ToolkitScriptManager1",
							"ctl00$ctl00$ctl00$ctl00$ToolkitScriptManager1|ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$CompositAddressSearch1$AddressSearch1$ctl00$ActionButton1");
				} else if (req
						.hasPostParameter("ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$ParcelIDSearch1$ctl00$FullParcel")) {
					req.setPostParameter(
							"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$ParcelIDSearch1$ctl00$ActionButton1",
							"Search");
					putParams(req, true);
				} else if (req
						.hasPostParameter("ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$PropertyNameSearch1$ctl00$PropertyName")) {
					req.setPostParameter(
							"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$PropertyNameSearch1$ctl00$ActionButton1",
							"Search");
					req.setPostParameter(
							"ctl00$ctl00$ctl00$ctl00$ToolkitScriptManager1",
							"ctl00$ctl00$ctl00$ctl00$ToolkitScriptManager1|ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$PropertyNameSearch1$ctl00$ActionButton1");
					putParams(req, true);
				} else if (req
						.hasPostParameter("ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$SubdivisionNameSearch1$Panel1$SubdivisionName")) {
					req.setPostParameter(
							"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$SubdivisionNameSearch1$Panel1$ActionButton1",
							"Search");
					req.setPostParameter(
							"ctl00$ctl00$ctl00$ctl00$ToolkitScriptManager1",
							"ctl00$ctl00$ctl00$ctl00$ToolkitScriptManager1|ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$SubdivisionNameSearch1$Panel1$ActionButton1");
					putParams(req, true);
				} else if (req
						.hasPostParameter("ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$CondoUnitNumberSearch1$UnitNumberPanel$UnitNumber")) {
					req.setPostParameter(
							"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$CondoUnitNumberSearch1$UnitNumberPanel$UnitNumberSearch",
							"Search");
					req.setPostParameter(
							"ctl00$ctl00$ctl00$ctl00$ToolkitScriptManager1",
							"ctl00$ctl00$ctl00$ctl00$ToolkitScriptManager1|ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$CondoUnitNumberSearch1$UnitNumberPanel$UnitNumberSearch");
					putParams(req, true);

				} else if (req
						.hasPostParameter("ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$BookpageInstrumentSearch1$ctl00$BookNumber")) {
					req.setPostParameter(
							"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$BookpageInstrumentSearch1$ctl00$SearchButton1",
							"Search");
					req.setPostParameter(
							"ctl00$ctl00$ctl00$ctl00$ToolkitScriptManager1",
							"ctl00$ctl00$ctl00$ctl00$ToolkitScriptManager1|ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$BookpageInstrumentSearch1$ctl00$SearchButton1");
					putParams(req, true);
				} else if (req
						.hasPostParameter("ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$BookpageInstrumentSearch2$ValidationGroupPanel1$InstrumentNumber")) {
					req.setPostParameter(
							"ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$BookpageInstrumentSearch2$ValidationGroupPanel1$SearchButton2",
							"Search");
					req.setPostParameter(
							"ctl00$ctl00$ctl00$ctl00$ToolkitScriptManager1",
							"ctl00$ctl00$ctl00$ctl00$ToolkitScriptManager1|ctl00$ctl00$ctl00$ctl00$ContentMain$ContentMain$ContentMain$ContentMain$TabContainer1$Searches$SubTabContainer1$QuickSearches$BookpageInstrumentSearch2$ValidationGroupPanel1$SearchButton2");
					putParams(req, true);
				}
			}
		}
	}

	public void onAfterRequest(HTTPRequest req, HTTPResponse res) {
	}

	public HashMap<String, String> getAspxParams() {
		return aspxParams;
	}

	public void setAspxParams(HashMap<String, String> aspxParams) {
		this.aspxParams = aspxParams;
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
