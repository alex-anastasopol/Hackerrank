package ro.cst.tsearch.connection.http2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.StringUtils;

public class TXRockwallTR extends HttpSite {
	
	public String getUserAgentValue() {
		return "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:20.0) Gecko/20100101 Firefox/20.0";
	}
	
	public LoginResponse onLogin() {
		DataSite dataSite = getDataSite();
        try{	        
	        HTTPRequest req = new HTTPRequest(dataSite.getLink(), HTTPRequest.GET);	
	        String response = execute(req);
	        if(!response.contains("Property Search and Tax Payment")){
	        	return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Official site not functional");
	        }
	        
        } catch(Exception e){
        	e.printStackTrace();
        	logger.error("Problem connection on TX Rockwall TR; Exception is: ", e);
        	return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Error found");
        }
        
        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");    
	}
	
	public String showExactMatch (String url) {
		String exactMatch = url;
        exactMatch = exactMatch.replaceFirst(".*chkIsExactMatching=([^&]+).*","$1").trim();
		if ("on".equals(exactMatch)) {
			url = url.replaceFirst("(?is)(.*)chkIsExactMatching=[^&]+&(.*)", "$1"+"$2");
		} else {
			url = url.replaceFirst("(?is)&chkIsExactMatching=", "");
		}
		
		return url;
	}
	
	public String refineURL(String urlLink, boolean isAdvancedSearch) {
		String url = urlLink;
        String pageNo = "";
        String pageSize = "";
        String yearInfo = "";
        
		url = url.replaceAll("(?is)[=]", ":");
    	url = url.replaceFirst("(?is)(keywords):","$1=");
    	url = url.replaceFirst("\\s*=\\s*&", "=");
    	url = url.replaceAll("(?is)[A-Z]+:\\s*&", "");
    	url = url.replaceFirst("(?is)&[A-Z]+:\\s*$", "");
    	
    	String regExp = "(?is).*keywords=(Year:\\d{4}&).*";
    	Matcher m = Pattern.compile(regExp).matcher(url);
    	
    	if (url.contains("Year:")) {
        	if (m.find()) {
    			yearInfo = m.group(1).replaceFirst("&", "");
    			yearInfo = "+AND+" + yearInfo;
    			url = url.replaceFirst(m.group(1), "");
    		} else {
    			yearInfo = url.replaceFirst("(?is).*((?:&|\\+AND\\+)Year:\\d+{4}).*", "$1");
        		yearInfo = yearInfo.replaceFirst("&", "+AND+");
        		url = url.replaceFirst("(?:&|\\+AND\\+)Year:\\d+{4}", "");
    		}
    		
    	}
    	
    	regExp = "(?is)(.*keywords=)(page(?:Size)?:[^&]+)&(.*)";
    	m = Pattern.compile(regExp).matcher(url);
    	if (m.find()) {
    		url = m.group(1) + m.group(3) + "&" + m.group(2);
    	}
    	if (m.find()) { //must be performed twice this test!
    		url = m.group(1) + m.group(3) + "&" + m.group(2);
    	}
    	
    	
    	if (url.contains("page:")) { 
    		// prev or next links
    		pageNo = url.replaceFirst("(?is).*(&page:\\d+).*", "$1");
    		pageNo = pageNo.replaceFirst(":", "=");
    	} 
    	if (url.contains("pageSize")) {
    		pageSize = url.replaceFirst("(?is).*(&pageSize:\\d+).*", "$1");
    		pageSize = pageSize.replaceFirst(":", "=");
    	}
    	
    	url = url.replaceAll("(?is)(&page(?:Size)?:\\d+)", "");
    	url = url.replaceAll("&","+AND+");
    	if (StringUtils.isNotEmpty(yearInfo))
    		url = url + yearInfo;
    	if (StringUtils.isNotEmpty(pageSize))
    		url = url + pageSize;
    	if (StringUtils.isNotEmpty(pageNo))
    		url = url + pageNo;
		
    	if (!isAdvancedSearch) {
    		if (url.contains("StreetNumber:") && url.contains("StreetName:") && url.contains("Year:") && url.contains("pageSize")) {
        		url = url.replaceAll("\\+AND", "@\\+AND");
        		String strNo = url.replaceFirst(".*StreetNumber:([^@]+@\\+AND\\+).*", "$1").replaceFirst("@.*", "").trim();
        		String strName = url.replaceFirst(".*StreetName:([^@]+@\\+AND\\+).*", "$1").replaceFirst("@.*", "").trim();
        		url = url.replaceFirst("(?is)(.*keywords=)(?:StreetNumber:[^@]+@\\+AND\\+)?StreetName:[^@]+@\\+AND\\+(?:StreetNumber:[^@]+@\\+AND\\+)?(.*)", "$1" + strNo + "\\+" + strName + "\\+AND\\+" + "$2");
        	}
    	}
    		
		return url;
	}
	
	
	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		req.setHeader("User-Agent", getUserAgentValue());
		DataSite dataSite = getDataSite();
		String year = dataSite.getDueDate().toGMTString().replaceFirst("(?is)\\d{1,2}\\s*[A-Z]+\\s*(\\d{4}).*", "$1").trim();
		 
		boolean hasDefaultYear = true;
		boolean hasExactMatching = false;
		boolean searchByOneCriteria = true;
		
        String currentRequest = req.getURL(); 
        String url = currentRequest;
        String filterBy = "";

        String exactMatch = url;
        if (exactMatch.contains("chkIsExactMatching=on"))
        	hasExactMatching = true;
		url = showExactMatch(exactMatch);
        
		if (currentRequest.contains("Subdivision") && currentRequest.contains("GeoId")) {
			//Advanced search module
			url = refineURL(url, true);
			int idx = url.indexOf("Year:") + 5;
			String selectedTaxYear = url.substring(idx, idx + 4);
			if (!year.equals(selectedTaxYear))
				hasDefaultYear = false;
			url = url.replaceAll("(?is)(\\+AND\\+)", "@@@" + "$1");
			
			if (url.contains("OwnerName:")) {
				if (hasExactMatching && !(url.contains("StreetNumber") || url.contains("StreetName") || url.contains("GeoId") || url.contains("PropertyId") || url.contains("Subdivision") || url.contains("MobileHomePark")))
					url = url.replaceFirst("(?is)(OwnerName:)([^@]+)@@@", "$1" + "%22" + "$2" + "%22");
				if (url.contains("@@@+AND+OwnerName")) 
					url = url.replaceFirst("(?is)([^\\?]+\\?keywords=)(.*)@@@\\+AND\\+(OwnerName:[^@]+@@@\\+AND\\+)([^$]+)", "$1" + "$3" + "$2" + "@@@\\+AND\\+" + "$4");
			}
			
			if (url.contains("Subdivision:"))
				url = url.replaceFirst("(?is)(Subdivision:)([^@]+)@@@", "$1" + "%22" + "$2" + "%22");
			if (url.contains("MobileHomePark:"))
				url = url.replaceFirst("(?is)(MobileHomePark:)([^@]+)@@@", "$1" + "%22" + "$2" + "%22");
			if (url.contains("Abstract:"))
				url = url.replaceFirst("(?is)(Abstract:)([^@]+)@@@", "$1" + "%22" + "$2" + "%22");
			if (url.contains("Year:"))
				url = url.replaceFirst("(?is)(Year:)(\\d{4})", "$1" + "%22" + "$2" + "%22");
			url = url.replaceAll("@{1,4}", "");
			
    		req.setURL(url);
    		return;
		}
		
        if (currentRequest.contains("keywords=")) { //interm results
        	hasDefaultYear = true;
        	if (url.contains("page=")) {  // Next or Prev clicked
        		req.setMethod(HTTPRequest.GET);
        		req.setURL(url);
        		return;
        	}
        	
        	url = refineURL(url, false);
        	int idx = url.indexOf("Year:") + 5;
        	String selectedTaxYear = url.substring(idx, idx + 4);
			if (!year.equals(selectedTaxYear))
				hasDefaultYear = false;
			
        	int countSearchCriteria = 0;
        	if (url.contains("OwnerName"))
        		countSearchCriteria ++;
        	if (url.contains("PropertyId"))
        		countSearchCriteria ++;
			if (url.contains("StreetName"))
				countSearchCriteria ++;
			if (url.contains("StreetNumber"))
				countSearchCriteria ++;
			if (url.contains("DoingBusinessAs"))
				countSearchCriteria ++;	
	        
			if (countSearchCriteria == 1) {
				searchByOneCriteria = true;
				filterBy = url;
				filterBy = filterBy.replaceFirst("(?is).*(OwnerName|PropertyId|StreetNumber|StreetName|DoingBusinessAs).*", "$1");
			} else if (countSearchCriteria > 1) {
				searchByOneCriteria = false;
				filterBy = "";
			}
			
        	url = url.replaceAll("(?is)(?:OwnerName|PropertyId|StreetNumber|StreetName|DoingBusinessAs):", "");
        	
        	if (hasDefaultYear) {
        		if (searchByOneCriteria) {
        			if (StringUtils.isNotEmpty(filterBy)) {
        				//only when we have just one search criteria, results are filtered by that specific criteria
        				url += "&filter=" +  filterBy;
        			}
        		}
        	}
        	
        	if (hasExactMatching) {
        		url = url.replaceAll("\\+AND", "@@@\\+AND");
        		String tmp = url;
        		tmp = tmp.replaceFirst("(?is).*keywords=([^@]+)@@@\\+AND\\+Year.*","$1");
        		//tmp = tmp.substring(0, tmp.indexOf("+AND+Year"));
        		tmp = "%22" + tmp + "%22";
        		//url = url.replaceFirst("(?is)(.*keywords=)[^Y]+Year", "$1" + tmp + "+AND+Year");
        		url = url.replaceFirst("(?is)(.*keywords=)[^@]+@@@\\+AND\\+Year", "$1" + tmp + "+AND+Year");
        		url = url.replaceAll("@@@", "");
        		if (!hasDefaultYear) {
        			//filtering still remain if year == default value
        			//no filtering when year != default value
        			url = url.replaceFirst("(?is)(.*)&filter=.*", "$1");
        		} 
        	}
        	
        	req.setMethod(HTTPRequest.GET);
        	req.setURL(url);
        	return;
        
        } 
        
        if (currentRequest.contains("Property")) { // details page
        	url = currentRequest;
        	req.setMethod(HTTPRequest.GET);
        	req.setURL(url);
        	return;
        }
	}
	
}
