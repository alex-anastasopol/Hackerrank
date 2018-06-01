package ro.cst.tsearch.connection.http2;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.http.HTTPRequest;

public class MOPlatteAO extends SimplestSite {

	@Override
	public void onBeforeRequest(HTTPRequest req) {
		// have to construct h_pid_entry before making the request for Parcel
		// search
		String pidPostParameterKey = "h_pid1Entry";
		String atsPidParameterKey = "ats_parcel_number";

		// for subdivision search
		String subDivLetterKey = "h_subdivletter";
		String subdivletter = StringUtils.defaultIfEmpty(req.getPostFirstParameter(subDivLetterKey), "");
		if (subdivletter.equals("!")) {
			req.removePostParameters(subDivLetterKey);
			req.setPostParameter(subDivLetterKey, "A");
		}
		// for subdivision search
		String subDivisionNameKey = "h_subdivname";
		String subdivName = StringUtils.defaultIfEmpty(req.getPostFirstParameter(subDivisionNameKey), "");
		if (subdivName.equals("undefined")) {
			req.removePostParameters(subDivisionNameKey);
		}

		String atsPIDParameterValue = req.getPostFirstParameter(atsPidParameterKey);

		if (StringUtils.isNotEmpty(atsPIDParameterValue)) {

			if (!atsPIDParameterValue.contains("-")) {
				atsPIDParameterValue = ro.cst.tsearch.servers.types.MOPlatteAO.convertPinToDashesFormat(atsPIDParameterValue);
			}

			String[] h_p = atsPIDParameterValue.split("-");
			int i = 1;
			for (String pid_rec : h_p) {
				req.setPostParameter("h_p" + i, pid_rec);
				i++;
			}

			req.removePostParameters(pidPostParameterKey);
			req.setPostParameter(pidPostParameterKey, atsPIDParameterValue.toString()); 
		}

		super.onBeforeRequest(req);
	}

	public String getDeedIntermediary(String deedLink) {
		String link = getCrtServerLink();
		HTTPRequest req;
		req = new HTTPRequest(link + deedLink);
		return execute(req);
	}

	private String getCrtServerLink() {
		return getSiteLink();
	}

}
