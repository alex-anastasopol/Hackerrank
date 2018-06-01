package ro.cst.tsearch.connection.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.InstanceManager;

public class OHFranklinPR extends HTTPSite{

	public LoginResponse onLogin() {
		setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "OHFranklinPR");
		return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	public void onBeforeRequest(HTTPRequest req) {
		
		String url = req.getURL();
		String date = "";
		String caseNumber = "";
		String caseSuffix = "";
		String caseType = "";
		String caseSubtype = "";
		
		url = url + "&";					//to match some regular expressions
		
		//search by case open date
		//translate date (e.g. from 01/02/2003 to 20030102) 
		Matcher ma = Pattern.compile("string=(\\d\\d)/(\\d\\d)/(\\d\\d\\d\\d)\\&").matcher(url);
		if (ma.find()) date = ma.group(3) + ma.group(1) + ma.group(2);
		if (date.length()>0) 
			url = url.replaceAll("\\?.*", "?string="+ date);
		
		//search by case number/suffix
		//concatenate case number with "!=" and with case suffix
		Matcher ma1 = Pattern.compile("caseNumber=(.*?)\\&").matcher(url);
		Matcher ma2 = Pattern.compile("caseSuffix=(.*?)\\&").matcher(url);
		if (ma1.find()) caseNumber = ma1.group(1);
		if (ma2.find()) caseSuffix = ma2.group(1);
		if (caseNumber.length()>0 || caseSuffix.length()>0) 
			url = url.replaceAll("\\?.*", "?string="+ caseNumber + "!=" + caseSuffix);
		
		//search by case type/subtype
		//concatenate case type with case subtpe
		Matcher ma3 = Pattern.compile("caseType=(.*?)\\&").matcher(url);
		Matcher ma4 = Pattern.compile("caseSubtype=(.*?)\\&").matcher(url);
		if (ma3.find()) caseType = ma3.group(1);
		if (ma4.find()) caseSubtype = ma4.group(1);
		if (caseType.length()>0 || caseSubtype.length()>0)
			url = url.replaceAll("\\?.*", "?string="+ caseType + caseSubtype);
		
		if (url.endsWith("&")) url = url.substring(0, url.length()-1);
		
		req.setURL(url);
		
		req.noRedirects=true;
		setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "OHFranklinPR");
	}
	
}
