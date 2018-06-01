package ro.cst.tsearch.connection.http2;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.CountyConstants;

public class FLGenericMFCConnRO2 extends HttpSite {
	
	private static HashMap<Integer, String> ID_COUNTY = new HashMap<Integer, String>();
	
	static {
		ID_COUNTY.put(CountyConstants.FL_Alachua, 		"01");
		ID_COUNTY.put(CountyConstants.FL_Baker, 		"02");
		ID_COUNTY.put(CountyConstants.FL_Bay, 			"03");
		ID_COUNTY.put(CountyConstants.FL_Bradford, 		"04");
		ID_COUNTY.put(CountyConstants.FL_Calhoun, 		"07");
		ID_COUNTY.put(CountyConstants.FL_Charlotte, 	"08");
		ID_COUNTY.put(CountyConstants.FL_Citrus, 		"09");
		ID_COUNTY.put(CountyConstants.FL_Clay, 			"10");
		ID_COUNTY.put(CountyConstants.FL_Collier, 		"11");
		ID_COUNTY.put(CountyConstants.FL_Columbia, 		"12");
		ID_COUNTY.put(CountyConstants.FL_DeSoto, 		"14");
		ID_COUNTY.put(CountyConstants.FL_Dixie, 		"15");
		ID_COUNTY.put(CountyConstants.FL_Duval, 		"16");
		ID_COUNTY.put(CountyConstants.FL_Escambia, 		"17");
		ID_COUNTY.put(CountyConstants.FL_Flagler, 		"18");
		ID_COUNTY.put(CountyConstants.FL_Franklin, 		"19");
		ID_COUNTY.put(CountyConstants.FL_Gadsden, 		"20");
		ID_COUNTY.put(CountyConstants.FL_Gilchrist, 	"21");
		ID_COUNTY.put(CountyConstants.FL_Glades, 		"22");
		ID_COUNTY.put(CountyConstants.FL_Gulf, 			"23");
		ID_COUNTY.put(CountyConstants.FL_Hamilton, 		"24");
		ID_COUNTY.put(CountyConstants.FL_Hardee, 		"25");
		ID_COUNTY.put(CountyConstants.FL_Hendry, 		"26");
		ID_COUNTY.put(CountyConstants.FL_Hernando, 		"27");
		ID_COUNTY.put(CountyConstants.FL_Highlands, 	"28");
		ID_COUNTY.put(CountyConstants.FL_Hillsborough, 	"29");
		ID_COUNTY.put(CountyConstants.FL_Holmes, 		"30");
		ID_COUNTY.put(CountyConstants.FL_Jackson, 		"32");
		ID_COUNTY.put(CountyConstants.FL_Jefferson,		"33");
		ID_COUNTY.put(CountyConstants.FL_Lafayette, 	"34");
		ID_COUNTY.put(CountyConstants.FL_Lake, 			"35");
		ID_COUNTY.put(CountyConstants.FL_Lee, 			"36");
		ID_COUNTY.put(CountyConstants.FL_Leon, 			"37");
		ID_COUNTY.put(CountyConstants.FL_Levy, 			"38");
		ID_COUNTY.put(CountyConstants.FL_Liberty, 		"39");
		ID_COUNTY.put(CountyConstants.FL_Madison, 		"40");
		ID_COUNTY.put(CountyConstants.FL_Manatee, 		"41");
		ID_COUNTY.put(CountyConstants.FL_Marion, 		"42");
		ID_COUNTY.put(CountyConstants.FL_Martin, 		"43");
		ID_COUNTY.put(CountyConstants.FL_Nassau, 		"45");
		ID_COUNTY.put(CountyConstants.FL_Okaloosa, 		"46");
		ID_COUNTY.put(CountyConstants.FL_Okeechobee, 	"47");
		ID_COUNTY.put(CountyConstants.FL_Osceola, 		"49");
		ID_COUNTY.put(CountyConstants.FL_Palm_Beach, 	"50");
		ID_COUNTY.put(CountyConstants.FL_Pasco, 		"51");
		ID_COUNTY.put(CountyConstants.FL_Pinellas, 		"52");
		ID_COUNTY.put(CountyConstants.FL_Polk, 			"53");
		ID_COUNTY.put(CountyConstants.FL_Putnam, 		"54");
		ID_COUNTY.put(CountyConstants.FL_St_Johns, 		"55");
		ID_COUNTY.put(CountyConstants.FL_St_Lucie, 		"56");
		ID_COUNTY.put(CountyConstants.FL_Santa_Rosa, 	"57");
		ID_COUNTY.put(CountyConstants.FL_Sarasota, 		"58");
		ID_COUNTY.put(CountyConstants.FL_Sumter, 		"60");
		ID_COUNTY.put(CountyConstants.FL_Suwannee, 		"61");
		ID_COUNTY.put(CountyConstants.FL_Taylor, 		"62");
		ID_COUNTY.put(CountyConstants.FL_Union, 		"63");
		ID_COUNTY.put(CountyConstants.FL_Volusia, 		"64");
		ID_COUNTY.put(CountyConstants.FL_Wakulla, 		"65");
		ID_COUNTY.put(CountyConstants.FL_Walton, 		"66");
		ID_COUNTY.put(CountyConstants.FL_Washington, 	"67");
	}
	public LoginResponse onLogin() {
		setAttribute(SINGLE_LINE_COOKIES_IN_REDIRECT, true);
		// get the search page
		HTTPRequest req = new HTTPRequest(getSiteLink());
		
		HTTPResponse res = process(req);
		String response = res.getResponseAsString();

		if (response.indexOf("Official Records") < 0){
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Page not found");
		} else {
			Pattern pat = Pattern.compile("(?)href=\\\"([^\\\"]+)\\\"[^>]*>\\s*.*?Official Records<");
			Matcher mat = pat.matcher(response);
			if (mat.find()){
				req = new HTTPRequest(getSiteLink() + mat.group(1));
				res = process(req);
				response = res.getResponseAsString();
				
				if (response.toLowerCase().contains("search now")){
					
					pat = Pattern.compile("(?s)href=\\\"([^\\\"]+)\\\"[^>]*>\\s*<img.*?Search Now");
					mat.reset();
					mat = pat.matcher(response);
					
					if (mat.find()){
						req = new HTTPRequest(getSiteLink() + mat.group(1));
						res = process(req);
						response = res.getResponseAsString();
						
						return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
					} else{
						return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Page not found");
					}
				} else{
					return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Page not found");
				}
			} else{
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Page not found");
			}
		}

	}

	@Override
	public void onBeforeRequestExcl(HTTPRequest req) {
		
		String url = req.getURL();	
		url = url.replaceAll("&amp;(amp;)*","&");
		if (req.getMethod() == HTTPRequest.POST){
			if (req.getPostFirstParameter("county") != null){
				String county = req.getPostFirstParameter("county");
				if (StringUtils.isNotEmpty(county) && county.length() > 2){
					int countyInt = 0;
					try {
						countyInt = Integer.parseInt(county);
					} catch (Exception e) {
					}
					if (countyInt > 0){
						req.removePostParameters("county");
						req.setPostParameter("county", ID_COUNTY.get(countyInt));
					}
				}
			}
			if (req.getPostFirstParameter("startDate") != null){
				String fromDate = req.getPostFirstParameter("startDate");
				if (!"".equals(fromDate)){
					String[] dateParts = fromDate.split("/");
					if (dateParts.length == 3){
						req.setPostParameter("startMonth", StringUtils.stripStart(dateParts[0], "0"));
						req.setPostParameter("startDay", StringUtils.stripStart(dateParts[1].replace(",", ""), "0"));
						req.setPostParameter("startYear", dateParts[2]);
						req.removePostParameters("startDate");
					}
				}
			}
			if (req.getPostFirstParameter("endDate") != null){
				String toDate = req.getPostFirstParameter("endDate");
				if (!"".equals(toDate)){
					String[] dateParts = toDate.split("/");
					if (dateParts.length == 3){
						req.setPostParameter("endMonth", StringUtils.stripStart(dateParts[0], "0"));
						req.setPostParameter("endDay", StringUtils.stripStart(dateParts[1].replace(",", ""), "0"));
						req.setPostParameter("endYear", dateParts[2]);
						req.removePostParameters("endDate");
					}
				}
			}
			if (req.getPostFirstParameter("documentTypes") != null){
				if ("".equals(req.getPostFirstParameter("documentTypes"))){
					req.removePostParameters("documentTypes");
				}
			}
			if (req.getPostFirstParameter("businessName") != null){
				if ("".equals(req.getPostFirstParameter("businessName"))){
					req.removePostParameters("businessName");
					req.setPostParameter("businessName", "business");
				}
			}
			if (req.getPostFirstParameter("lastName") != null){
				if ("".equals(req.getPostFirstParameter("lastName"))){
					req.removePostParameters("lastName");
					req.setPostParameter("lastName", "last");
				}
			}
			if (req.getPostFirstParameter("firstName") != null){
				if ("".equals(req.getPostFirstParameter("firstName"))){
					req.removePostParameters("firstName");
					req.setPostParameter("firstName", "first");
				}
			}
			if (req.getPostFirstParameter("book") != null){
				if ("".equals(req.getPostFirstParameter("book"))){
					req.removePostParameters("book");
					req.setPostParameter("book", "Book");
				}
			}
			if (req.getPostFirstParameter("page") != null){
				if ("".equals(req.getPostFirstParameter("page"))){
					req.removePostParameters("page");
					req.setPostParameter("page", "Page");
				}
			}
			if (req.getPostFirstParameter("navigate") != null){
				req.setURL(url + "?navigate=" + req.getPostFirstParameter("navigate"));
			}
//			if (req.getPostFirstParameter("navig") != null){
//				Map<String,String> navParams = (Map<String, String>)getTransientSearchAttribute("paramsNav:");
//				req.removePostParameters("navig");
//				if (navParams != null){
//					req.addPostParameters(navParams);
//				}
//			}
				// trim search fields
			if (req.getPostFirstParameter("firstName") != null)
				if (req.getPostFirstParameter("firstName").isEmpty() == false)
				{
					String FirstName = req.getPostFirstParameter("firstName").toString().trim();
					req.setPostParameter("firstName", FirstName, true);
				}
			if (req.getPostFirstParameter("lastName") != null)
				if (req.getPostFirstParameter("lastName").isEmpty() == false)
				{
					String FirstName = req.getPostFirstParameter("lastName").toString().trim();
					req.setPostParameter("lastName", FirstName, true);
				}
			if (req.getPostFirstParameter("instrumentNumber") != null)
				if (req.getPostFirstParameter("instrumentNumber").isEmpty() == false)
				{
					String FirstName = req.getPostFirstParameter("instrumentNumber").toString().trim();
					req.setPostParameter("instrumentNumber", FirstName, true);
				}
			if (req.getPostFirstParameter("book") != null)
				if (req.getPostFirstParameter("book").isEmpty() == false)
				{
					String FirstName = req.getPostFirstParameter("book").toString().trim();
					req.setPostParameter("book", FirstName, true);
				}
			if (req.getPostFirstParameter("page") != null)
				if (req.getPostFirstParameter("page").isEmpty() == false)
				{
					String FirstName = req.getPostFirstParameter("page").toString().trim();
					req.setPostParameter("page", FirstName, true);
				}
		}
	}
	public byte[] getImage(String docNo, String book, String page){

		//docNo = "20120001329";
		int countyID = dataSite.getCountyId();
		String countyCode = ID_COUNTY.get(countyID);
		
		if (StringUtils.isEmpty(countyCode)){
			return "This county is not available on RO2".getBytes();
		} else{
			HTTPRequest req = new HTTPRequest(getSiteLink() + "/ori/search.do", HTTPRequest.POST);
			
			if (StringUtils.isNotEmpty(docNo)){
				req.setPostParameter("instrumentNumber", docNo);
				req.setPostParameter("book", "Book");
				req.setPostParameter("page", "Page");
			} else if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)){
				req.setPostParameter("instrumentNumber", "");
				req.setPostParameter("book", book);
				req.setPostParameter("page", page);
			}
			req.setPostParameter("county", countyCode);
			req.setPostParameter("locationType", "COUNTY");
			req.setPostParameter("nametype", "i");
			req.setPostParameter("lastName", "last");
			req.setPostParameter("firstName", "first");
			req.setPostParameter("businessName", "business");
			req.setPostParameter("circuit", "500");
			req.setPostParameter("region", "500");
			req.setPostParameter("startMonth", "0");
			req.setPostParameter("startDay", "0");
			req.setPostParameter("startYear", "");
			req.setPostParameter("endMonth", "0");
			req.setPostParameter("endDay", "0");
			req.setPostParameter("endYear", "");
			req.setPostParameter("percisesearchtype", "i");
			req.setPostParameter("x", "36");
			req.setPostParameter("y", "14");
			
			
			String resp = execute(req);
			
			if (!resp.contains("\">View Image</a>")){
				return "Image unavailable on RO2".getBytes();
			} else{
			
				String link = getSiteLink() + "/ori/image.do?instrumentNumber=" + docNo;
				link = link.replaceAll("(?is)([A-Z]+)//([A-Z]+)", "$1/$2");
				req = new HTTPRequest(link, HTTPRequest.GET);
				
				return process(req).getResponseAsByte();
			}
		}
	}
	
}
