package ro.cst.tsearch.connection.http2;


import java.util.Map;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.utils.StringUtils;

public class AKAnchorageAO extends HttpSite {
		
	@Override
	public LoginResponse onLogin() {
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}
		
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		Map<String,String> addParams = null;
		addParams = (Map<String, String>)getTransientSearchAttribute("paramsDetails:");
		String url = req.getURL();

		if (req.getPostFirstParameter("ParcelId") != null){
			if (addParams != null){
				String parcel = req.getPostFirstParameter("ParcelId");
				req.removePostParameters("ParcelId");
				url = url.replaceAll("(?is)\\?parcel.*", "");;
				String action = addParams.get("action");
				if (StringUtils.isEmpty(action)){
					action = "/cics/cwba/gsweb";
				}
				if (url.endsWith("/")){
					url = url + action.replaceAll("\\A\\s*/", "");
				} else {
					url = url + action;
				}
				req.modifyURL(url);
				req.setMethod(HTTPRequest.POST);
				for(Map.Entry<String, String> entry: addParams.entrySet()){
					if (!entry.getKey().contains("action")){
						req.setPostParameter(entry.getKey(), entry.getValue());
					}
				}
				req.setPostParameter("Parcel", parcel + "       ");
			}
		} else if (req.getPostFirstParameter("ADSTREET") != null){
			String number = req.getPostFirstParameter("ADNBR");
			String dir = req.getPostFirstParameter("ADDIR");
			String street = req.getPostFirstParameter("ADSTREET");
			req.removePostParameters("ADSTREET");
			req.removePostParameters("ADNBR");
			req.removePostParameters("ADDIR");
			req.removePostParameters("ADTYPE");
			if (StringUtils.isNotEmpty(street)){
				String parcel = street;
				while (parcel.length() < 25){
					parcel += " ";
				}
				if (StringUtils.isNotEmpty(dir)){
					parcel += dir;
					while (parcel.length() < 27){
						parcel += " ";
					}
				} else {
					parcel += "  ";
				}
				if (StringUtils.isNotEmpty(number)){
					parcel += number;
					while (parcel.length() < 33){
						parcel += " ";
					}
				} else {
					parcel += "      ";
				}
				
				req.setPostParameter("Parcel", parcel);
			}
		} else if (req.getPostFirstParameter("Parcel") != null){
			
		} else if (url.contains("TX1P")){

		}else {
			addParams = (Map<String, String>)getTransientSearchAttribute("paramsForNext:");
			if (addParams != null){
				for(Map.Entry<String, String> entry: addParams.entrySet()){
					req.setPostParameter(entry.getKey(), entry.getValue());
				}
			}
		}
				
	}
}
