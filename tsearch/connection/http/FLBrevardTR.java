package ro.cst.tsearch.connection.http;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.utils.InstanceManager;

public class FLBrevardTR extends HTTPSite {
    private boolean loggingIn = false;
    String sid = "04270100F7254A2786ADC64CBB0D9C3B";
	
	public void setMiServerId(long miServerId){
		super.setMiServerId(miServerId);
	}
	
	public LoginResponse onLogin( ) 
	{
        loggingIn = true;
        
		setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "FLBrevardTR");
	
		HTTPRequest req = new HTTPRequest( "http://brevardtaxcollector.governmaxa.com/collectmax/collect30.asp" );
		req.setHeader("Host", "brevardtaxcollector.governmaxa.com");
        req.setMethod( HTTPRequest.GET );
        HTTPResponse res = process( req );
        String resp = res.getResponseAsString(); 
        sid = resp.replaceFirst("(?is).*sid=['\\\"]?([a-z0-9]+)['\\\"]?.*", "$1");
        
        loggingIn = false;

        return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
	}

	public void onBeforeRequest(HTTPRequest req) {
		setUserData(InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchID() + "FLBrevardTR");
        if( loggingIn )
        {
            return;
        }
		String url = req.getURL();
		url = url.replaceAll("\\?l_cr", "&l_cr");
		req.setHeader("Host", "brevardtaxcollector.governmaxa.com");

		if (url.contains("list_collect_v5-5.asp") && !url.contains("l_mv=next") && !url.contains("l_mv=previous") && url.contains("name_search"))
		{
			loggingIn = true;

			req.noRedirects=false;
			String owner = url.replaceFirst("(?is).*[&]?owner=([^&]*).*", "$1");
			String nameSearch = url.replaceFirst("(?is).*[&]?name_search=([^&]*).*", "$1");
			String listView = url.replaceFirst("(?is).*[&]?listview=([^&]*).*", "$1");
			String paidFlag = url.replaceFirst("(?is).*[&]?paidflag=([^&]*).*", "$1");
			if (paidFlag.equals("%25"))
				paidFlag = "%";
			String owner_uc = owner;
 			owner_uc = owner_uc.toUpperCase();
			url += "&l_wc=|owner="+owner_uc+"|name_search="+nameSearch+"|listview="+listView+"|paidflag="+paidFlag;
			String strLNm = url.replaceFirst("(?is).*[&?](l_nm=[^&]+).*", "$1");
			String strLWc = url.replaceFirst("(?is).*[&?](l_wc=[^&]+).*", "$1");
			String strOwner = "owner="+owner_uc;
			String strNameSearch = url.replaceFirst("(?is).*[&?](name_search=[^&]+).*", "$1");
			String strListview = url.replaceFirst("(?is).*[&?](listview=[^&]+).*", "$1");
			String strPaidflag = url.replaceFirst("(?is).*[&?](paidflag=[^&]+).*", "$1");
			if (strPaidflag.contains("%25"))
				strPaidflag = strPaidflag.replace("%25","%");
			
			url = "http://brevardtaxcollector.governmaxa.com/collectmax/list_collect_v5-5.asp?sid="+sid+
				"&"+strLNm+"&"+strLWc+"&"+strOwner+"&"+strNameSearch+"&"+strListview+"&"+strPaidflag;
			
			req.modifyURL(url);
			req.setHeader("Referer", "http://brevardtaxcollector.governmaxa.com/collectmax/search_collect.asp?l_nm=owner&site=collect_search&sid="+sid);

			
			HTTPRequest req1 = new HTTPRequest("http://brevardtaxcollector.governmaxa.com/collectmax/search_collect.asp?go.x=1");
			req1.setMethod( HTTPRequest.POST);
			req1.setHeader("Host", "brevardtaxcollector.governmaxa.com");
			req1.setHeader("Referer", "http://brevardtaxcollector.governmaxa.com/search_collect.asp?l_nm=owner&site=collect_search&sid="+sid);
			req1.setPostParameter("owner", owner);
			req1.setPostParameter("name_search", nameSearch);
			if(listView.equalsIgnoreCase("DETAIL"))
					listView = "Detail";
			else if(listView.equalsIgnoreCase("SUMMARY"))
					listView = "Summary";

			req1.setPostParameter("listview", listView);
			req1.setPostParameter("paidflag", paidFlag);
			req1.setPostParameter("go","GO");
			req1.setPostParameter("site","collect_search");
			req1.setPostParameter("l_nm","owner");
			req1.setPostParameter("sid", sid);
			req1.toString();
			req1.noRedirects = true;

			HTTPResponse resp = process(req1);
			String respStr = resp.getResponseAsString();
			
			loggingIn = false;
		}
		else if (url.contains("list_collect_v5-5.asp") && !url.contains("l_mv=next") && !url.contains("l_mv=previous") && url.contains("streetaddress"))
		{
			loggingIn = true;
			
			String streetname= url.replaceFirst("(?is).*[^a-z]streetname=([^&=]*).*", "$1");
			String listview= url.replaceFirst("(?is).*[^a-z]listview=([^&=]*).*", "$1");
			String paidflag= url.replaceFirst("(?is).*[^a-z]paidflag=([^&=]*).*", "$1");
			if (paidflag.contains("%25"))
				paidflag = paidflag.replace("%25","%");
			
			url = "http://brevardtaxcollector.governmaxa.com/collectmax/list_collect_v5-5.asp?sid="+sid+"&l_nm=streetaddress&"+
			"l_wc=|streetname="+streetname+"|listview="+listview+"|paidflag="+paidflag+
			"&streetname="+streetname+"&listview="+listview+"&paidflag="+paidflag;
			req.modifyURL(url);
			String referer = "http://brevardtaxcollector.governmaxa.com/collectmax/search_collect.asp?l_nm=streetaddress&form=searchform&formelement=0&sid="+sid;
			req.setHeader("Referer", referer);
			
			HTTPRequest req1 = new HTTPRequest("http://brevardtaxcollector.governmaxa.com/collectmax/search_collect.asp?go.x=1");
			req1.setMethod( HTTPRequest.POST);
			req1.setHeader("Host", "brevardtaxcollector.governmaxa.com");
			req1.setHeader("Referer", referer);
			req1.setPostParameter("streetname", streetname);
			if(listview.equalsIgnoreCase("DETAIL"))
					listview = "Detail";
			else if(listview.equalsIgnoreCase("SUMMARY"))
					listview = "Summary";
			req1.setPostParameter("listview", listview);
			req1.setPostParameter("paidflag", paidflag);
			req1.setPostParameter("go","GO");
			req1.setPostParameter("site","collect_search");
			req1.setPostParameter("l_nm","streetaddress");
			req1.setPostParameter("sid", sid);
			req1.noRedirects = true;

			HTTPResponse resp = process(req1);
			loggingIn = false;
		}
		
		else if (url.contains("tab_collect_mvptaxV5.4.asp") && !url.contains("l_mv=next") && !url.contains("l_mv=previous") && url.contains("account"))
		{

			loggingIn = true;

			String account= url.replaceFirst("(?is).*[^a-z]account=([^&=]*).*", "$1");
			String listview= url.replaceFirst("(?is).*[^a-z]listview=([^&=]*).*", "$1");
			String paidflag= url.replaceFirst("(?is).*[^a-z]paidflag=([^&=]*).*", "$1");
			if (paidflag.contains("%25"))
				paidflag = paidflag.replace("%25","%");
			if(listview.equalsIgnoreCase("DETAIL"))
				listview = "Detail";
			else if(listview.equalsIgnoreCase("SUMMARY"))
				listview = "Summary";

			String referer = "http://brevardtaxcollector.governmaxa.com/collectmax/search_collect.asp?l_nm=account&form=searchform&formelement=0&sid="+sid;
			HTTPRequest req1 = new HTTPRequest("http://brevardtaxcollector.governmaxa.com/collectmax/search_collect.asp?go.x=1");
			req1.setMethod( HTTPRequest.POST);
			req1.setHeader("Host", "brevardtaxcollector.governmaxa.com");
			req1.setHeader("Referer", referer);
			req1.setPostParameter("account",account);
			req1.setPostParameter("listview",listview);
			req1.setPostParameter("paidflag",paidflag);
			req1.setPostParameter("go","GO");
			req1.setPostParameter("site","collect_search");
			req1.setPostParameter("l_nm","account");
			req1.setPostParameter("sid", sid);
			req1.noRedirects = true;
			
			HTTPResponse resp = process(req1);
			String respStr = resp.getResponseAsString();
			url = respStr.replaceFirst("(?is).*[^a-z](list_collect_v5-5.asp[^\\\">]+).*", "$1");
			url = url.replaceAll("(?is)amp[;]","");
			url = "http://brevardtaxcollector.governmaxa.com/collectmax/"+url;
			req.modifyURL(url);
			req.setHeader("Referer", referer);
			
			resp = process(req);
			respStr = resp.getResponseAsString();
			url = respStr.replaceFirst("(?is).*[^a-z](tab_collect_mvptaxV5.4.asp[^'\\\")]+).*", "$1");
			url = url.replaceAll("\\s", "+");
			url = "http://brevardtaxcollector.governmaxa.com/collectmax/"+url;
			req.modifyURL(url);
			//req.setHeader("Referer", referer);
			loggingIn = false;
		}
		/*else if (url.contains("tab_collect_mvptaxV5.4.asp") && !url.contains("l_mv=next") && !url.contains("l_mv=previous") && url.contains("parcelid"))
		{

			loggingIn = true;
			String parcelid= url.replaceFirst("(?is).*[^a-z]parcelid=([^|]*).*", "$1");
			String listview= url.replaceFirst("(?is).*[^a-z]listview=([^&=]*).*", "$1");
			String paidflag= url.replaceFirst("(?is).*[^a-z]paidflag=([^&=]*).*", "$1");
			if (paidflag.contains("%25"))
				paidflag = paidflag.replace("%25","%");
			if(listview.equalsIgnoreCase("DETAIL"))
				listview = "Detail";
			else if(listview.equalsIgnoreCase("SUMMARY"))
				listview = "Summary";

			String referer = "http://brevardtaxcollector.governmaxa.com/collectmax/search_collect.asp?l_nm=parcelid&form=searchform&formelement=0&sid="+sid;
			HTTPRequest req1 = new HTTPRequest("http://brevardtaxcollector.governmaxa.com/collectmax/search_collect.asp?go.x=1");
			req1.setMethod( HTTPRequest.POST);
			req1.setHeader("Host", "brevardtaxcollector.governmaxa.com");
			req1.setHeader("Referer", referer);
			req1.setPostParameter("parcelid",parcelid);
			req1.setPostParameter("listview",listview);
			req1.setPostParameter("paidflag",paidflag);
			req1.setPostParameter("go","GO");
			req1.setPostParameter("site","collect_search");
			req1.setPostParameter("l_nm","parcelid");
			req1.setPostParameter("sid", sid);
			req1.noRedirects = true;
			
			HTTPResponse resp = process(req1);
			String respStr = resp.getResponseAsString();
			url = respStr.replaceFirst("(?is).*[^a-z](list_collect_v5-5.asp[^\\\">]+).*", "$1");
			url = url.replaceAll("(?is)amp[;]","");
			url = "http://brevardtaxcollector.governmaxa.com/collectmax/"+url;
			req.modifyURL(url);
			req.setHeader("Referer", referer);
			
			resp = process(req);
			respStr = resp.getResponseAsString();
			url = respStr.replaceFirst("(?is).*[^a-z](tab_collect_mvptaxV5.4.asp[^'\\\")]+).*", "$1");
			url = url.replaceAll("\\s", "+");
			url = "http://brevardtaxcollector.governmaxa.com/collectmax/"+url;
			req.modifyURL(url);
			//req.setHeader("Referer", referer);
			loggingIn = false;
		}*/
		else if (url.contains("list_collect_v5-5.asp") && !url.contains("l_mv=next") && !url.contains("l_mv=previous") && url.contains("geo_number"))
		{
			loggingIn = true;
			
			String geo_number= url.replaceFirst("(?is).*[^a-z]geo_number=([^&=]*).*", "$1");
			String listview= url.replaceFirst("(?is).*[^a-z]listview=([^&=]*).*", "$1");
			String paidflag= url.replaceFirst("(?is).*[^a-z]paidflag=([^&=]*).*", "$1");
			if (paidflag.contains("%25"))
				paidflag = paidflag.replace("%25","%");
			
			url = "http://brevardtaxcollector.governmaxa.com/collectmax/list_collect_v5-5.asp?sid="+sid+"&l_nm=geo_number&"+
			"l_wc=|geo_number="+geo_number+"|listview="+listview+"|paidflag="+paidflag+
			"&geo_number="+geo_number+"&listview="+listview+"&paidflag="+paidflag;
			req.modifyURL(url);
			String referer = "http://brevardtaxcollector.governmaxa.com/collectmax/search_collect.asp?l_nm=streetaddress&form=searchform&formelement=0&sid="+sid;
			req.setHeader("Referer", referer);
			
			HTTPRequest req1 = new HTTPRequest("http://brevardtaxcollector.governmaxa.com/collectmax/search_collect.asp?go.x=1");
			req1.setMethod( HTTPRequest.POST);
			req1.setHeader("Host", "brevardtaxcollector.governmaxa.com");
			req1.setHeader("Referer", referer);
			req1.setPostParameter("geo_number", geo_number);
			if(listview.equalsIgnoreCase("DETAIL"))
					listview = "Detail";
			else if(listview.equalsIgnoreCase("SUMMARY"))
					listview = "Summary";
			req1.setPostParameter("listview", listview);
			req1.setPostParameter("paidflag", paidflag);
			req1.setPostParameter("go","GO");
			req1.setPostParameter("site","collect_search");
			req1.setPostParameter("l_nm","geo_number");
			req1.setPostParameter("sid", sid);
			req1.noRedirects = true;

			HTTPResponse resp = process(req1);
			loggingIn = false;
		}
		else if(url.contains("tab_collect_mvptaxV5.4.asp") && url.contains("&qry="))
		{
			loggingIn = true;

			String qry = url.replaceFirst("(?is).*&qry=(.*)", "$1");
			url = url.substring(0, url.indexOf("&qry="));
			url = url.replace("?l_cr", "&l_cr");
			qry=qry.replaceAll("(?is)[%]3D", "=");

			String nameSearch = qry.replaceFirst("(?is).*[&]?name_search=([^&]*).*", "$1");
			String listView = qry.replaceFirst("(?is).*[&]?listview=([^&]*).*", "$1");
			String paidFlag = qry.replaceFirst("(?is).*[&]?paidflag=([^&]*).*", "$1");
			String owner = qry.replaceFirst("(?is).*[&]?owner=([^&]*).*", "$1");
			String owner_uc = owner;
			owner_uc = owner_uc.toUpperCase();
			String referer ="http://brevardtaxcollector.governmaxa.com/collectmax/list_collect_v5-5.asp?"+
				"sid="+url.replaceFirst("(?is).*[^a-z]sid=([0-9a-z]+).*", "$1")+
				"&"+qry.replaceFirst("(?is)(.*&owner=)([+0-9a-z]+)(.*)", "$1"+owner_uc+"$3")+
				"&l_wc=|owner="+owner_uc+"|name_search="+nameSearch+"|listview="+listView+"|paidflag="+paidFlag;

			url = url.replaceAll("(?is)[%]7C", "|");
			url = url.replaceAll("(?is)[%]3D", "=");
			url = url.replaceAll("(?is)=([0-9a-z]+)[-]([0-9a-z]+)[-]([0-9a-z]+)", "=" + "$1" + "%2D" + "$2" + "%2D" + "$3");
			url = url.replaceFirst("(?is)([|]parcelid[^.]+)([.])", "$1"+"%2E");		
			req.modifyURL(url);
			req.setHeader("Referer", referer);
			loggingIn = false;
		}
		else if(url.contains("tab_collect_mvptaxV5.4.asp") && url.contains("&parentSite=true"))
		{
			loggingIn = true;
			String referer = url.replaceFirst("(?is)(.*)[^a-z]dummy=[0-9-]+(.*)&parentSite=[a-z]+(.*)", "$1"+"$2"+"$3");
			url = url.replaceFirst("(?is)(.*)&l_cr=(.*)", "$1"+"&wait=done&l_cr="+"$2");
			req.modifyURL(url);
			req.setHeader("Referer", referer);
			loggingIn = false;
		}
		else if (url.contains("list_collect_v5-5.asp") && url.contains("l_mv=next"))
		{
			loggingIn = true;
			String referer = url.replaceFirst("(?is)(.*asp[?]).*(sid=.*)","$1"+"$2");
			url = url.replaceFirst("(?is)[?]l_mv", "&l_mv");
			req.modifyURL(url);
			req.setHeader("Referer", referer);
			loggingIn = false;
		}
		else if (url.contains("list_collect_v5-5.asp") && url.contains("l_mv=previous"))
		{
			loggingIn = true;
			String referer = url.replaceFirst("(?is)(.*)l_mv=previous(.*)","$1"+"l_mv=next"+"$2");
			url = url.replaceFirst("(?is)[?]l_mv", "&l_mv");
			req.modifyURL(url);
			req.setHeader("Referer", referer);
			loggingIn = false;
		}
	}
}

