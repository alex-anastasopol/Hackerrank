package ro.cst.tsearch.connection.http3;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;

import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.connection.http.HTTPRequest;

public class CAKernTR extends AdvancedTemplateSite {

	public CAKernTR() {

		MAIN_PARAMETERS = new String[1];
		MAIN_PARAMETERS[0] = "__VIEWSTATE";
		MAIN_PARAMETERS_KEY = "search.params";
		FORM_NAME = "aspnetForm";

		TARGET_ARGUMENT_MIDDLE_KEY = ":params:";
		TARGET_ARGUMENT_PARAMETERS = new String[1];
		TARGET_ARGUMENT_PARAMETERS[0] = "__VIEWSTATE";
	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		String url = "";
		try {
			url = URLDecoder.decode(req.getURL(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String seqName = "seq";
		String seqValue = ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(url, Matcher.quoteReplacement(seqName));
		if (StringUtils.isNotEmpty(seqValue)) {
			req.setMethod(HTTPRequest.POST);
			req.setPostParameter(seqName, seqValue);
			url = url.replaceFirst("(?:&|\\?)" + seqName + "=.*?($|&)", "$1");
			req.setURL(url);
		}

		if (req.getMethod() == HTTPRequest.POST) {
			super.onBeforeRequestExcl(req);
			Object eventTarget = req.getPostParameter("__EVENTTARGET");
			String paramName = "NUMBER";
			Object paramValue = req.getPostParameter(paramName);
			String detailsL2ParamName = "ctl00$ContentPlaceHolder1$btnDetails";
			String detailsL2Param = ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(url, Matcher.quoteReplacement(detailsL2ParamName));

			String prevYearParamName = "ctl00$ContentPlaceHolder1$btnPreviousYear";
			String prevYearParam = StringUtils.extractParameterFromUrl(url, Matcher.quoteReplacement(prevYearParamName));

			if (eventTarget != null && org.apache.commons.lang.StringUtils.containsIgnoreCase(url, "ATNSummary.aspx") && paramValue != null) {
				// intermediaries L1 paging - ATN search module
				url += "&" + paramName + "=" + paramValue;
				req.removePostParameters(paramName);
				paramName = "NUM_TYPE";
				paramValue = req.getPostParameter(paramName);
				if (paramValue != null) {
					req.removePostParameters(paramName);
					url += "&" + paramName + "=" + paramValue;
				}
				url = url.replaceFirst("&", "\\?");
				req.setURL(url);
			} else if (StringUtils.isNotEmpty(detailsL2Param) && org.apache.commons.lang.StringUtils.containsIgnoreCase(url, "BillSummary.aspx")) {
				// extract details bill info(L2)
				url = url.replaceFirst("(?:&|\\?)" + Matcher.quoteReplacement(detailsL2ParamName) + "=.*?($|&)", "$1");
				req.setPostParameter(detailsL2ParamName, detailsL2Param);
				req.setURL(url);

			} else if (StringUtils.isNotEmpty(prevYearParam) && org.apache.commons.lang.StringUtils.containsIgnoreCase(url, "ATNDetails.aspx")) {
				// extract previous years - for details
				url = url.replaceFirst("(?:&|\\?)" + Matcher.quoteReplacement(prevYearParamName) + "=.*?($|&)", "$1");
				req.setPostParameter(prevYearParamName, prevYearParam);
				req.setURL(url);
			}
		} else if (req.getMethod() == HTTPRequest.GET) {
			if (org.apache.commons.lang.StringUtils.containsIgnoreCase(url, "AddressSummary.aspx")) {
				String eventTargetParamName = "__EVENTTARGET";
				String eventTarget = StringUtils.extractParameterFromUrl(url, eventTargetParamName);
				if (StringUtils.isNotEmpty(eventTarget)) {// intermediaries L1 paging - address search module
					req.setPostParameter(eventTargetParamName, eventTarget);
					url = url.replaceFirst("(?:&|\\?)" + eventTargetParamName + "=.*?($|&)", "$1");
					req.setURL(url);
				}
			}
		}
	}
}
