/*
 * Created on Apr 6, 2004
 */
package ro.cst.tsearch.connection;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Logme;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.connection.http.HTTPManager;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.exceptions.LoginException;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class BridgeConn {
    
    protected static final Category logger = Logger.getLogger(BridgeConn.class);

    //TODO: Delete this please :)
    public static HashSet<String> oldlink = new HashSet<String>();// boost server communication
    
    public static Object syncaccessMontgomeryTR = new Object();
    public static Object syncaccessSumnerTR = new Object();
//    public static Object syncaccessJohnsonRO = new Object();
    public static Object syncaccessShelbyRO = new Object();
    
    static {
    	
    	oldlink.add(CountyConstants.TN_Davidson_STRING + "-" + GWTDataSite.AO_TYPE);
    	oldlink.add(CountyConstants.TN_Rutherford_STRING + "-" + GWTDataSite.AO_TYPE);
    	oldlink.add(CountyConstants.TN_Sumner_STRING + "-" + GWTDataSite.AO_TYPE);
    	oldlink.add(CountyConstants.TN_Williamson_STRING + "-" + GWTDataSite.AO_TYPE);
    	oldlink.add(CountyConstants.TN_Hamilton_STRING + "-" + GWTDataSite.AO_TYPE);
    	oldlink.add(CountyConstants.TN_Knox_STRING + "-" + GWTDataSite.AO_TYPE);
    	
    	oldlink.add(CountyConstants.TN_Davidson_STRING + "-" + GWTDataSite.TR_TYPE);
    	oldlink.add(CountyConstants.TN_Hamilton_STRING + "-" + GWTDataSite.TR_TYPE);
    	
    	oldlink.add(CountyConstants.TN_Montgomery_STRING + "-" + GWTDataSite.YA_TYPE);
    	oldlink.add(CountyConstants.TN_Hamilton_STRING + "-" + GWTDataSite.YA_TYPE);
    	
    	oldlink.add(CountyConstants.TN_Hamilton_STRING + "-" + GWTDataSite.RO_TYPE);
//    	oldlink.add(CountyConstants.KS_Johnson_STRING + "-" + GWTDataSite.RO_TYPE);

    }

    private TSConnectionURL tsc;
    private String link;
    private String encqry;
    private String pg;
    //private String classname;
    private int conntype; // GET/POST
    //private String key;
    private DataSite site;

    private boolean followRedirects = true;

    private long searchId=-1;
    private int commId = -1;
    
    int miServerID =-1;
    
    public BridgeConn(
    		TSConnectionURL tsconn,
    		//String clsname, 
    		String qry,
            String page, 
            int type, 
            String action, 
            //String k,
            long searchId,
            int miServerID) {
    	this.miServerID = miServerID;
    	this.searchId = searchId;
        tsc = tsconn;
        encqry = qry;
        pg = page;
        link = "";

        if (type == TSConnectionURL.idGET)
            conntype = ATSConnConstants.GET;
        if (type == TSConnectionURL.idPOST)
            conntype = ATSConnConstants.POST;

        commId = InstanceManager.getManager().getCommunityId(searchId);
        site = HashCountyToIndex.getDateSiteForMIServerID(commId, miServerID);
        
        log("BridgeConn>>>>>>>>>>>>>SearchId [" + searchId + "]");
        log("BridgeConn>>>>>>>>>>>>>qry [" + qry + "]");
        log("BridgeConn>>>>>>>>>>>>>page [" + page + "]");
        log("BridgeConn>>>>>>>>>>>>>type [" + type + "]");
        log("BridgeConn>>>>>>>>>>>>>action [" + action + "]");
        log("BridgeConn>>>>>>>>>>>>>serverID [" + miServerID + "]");

    }

    public void process() throws ServerResponseException {
        long btime = System.currentTimeMillis();
        long etime = System.currentTimeMillis();
        if (site.getCountyId() == CountyConstants.TN_Knox 
        		&& site.getSiteTypeInt() == GWTDataSite.RO_TYPE) {
                pprocess();
                etime = System.currentTimeMillis();
                log(">>>>>>>" + site.getName() + " connect time = " + (etime - btime));
            return;
        }
        if (site.getCountyId() == CountyConstants.TN_Shelby 
        		&& site.getSiteTypeInt() == GWTDataSite.RO_TYPE) {
            synchronized (syncaccessShelbyRO) {
                pprocess();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return;
        }
      
        pprocess();
        
        etime = System.currentTimeMillis();
        log(">>>>>>>" + site.getName() + " connect time = " + (etime - btime));
    }

    private void pprocess() throws ServerResponseException {

        try {
            HashMap props = new HashMap();

            String siteName = site.getName();
            int dbTimeout = site.getConnectionTimeout();
          
            ///setup
            if (!pg.startsWith("http")) ///in case pg contains the full link
                link = tsc.getHostName();
            link += pg;
            if (!link.startsWith("http"))
                link = "http://" + link;

            log("BridgeConn>>>>>>>>>>>>>>Link: [" + link + "]");

            props.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
            props.put("Host", tsc.getHostName());

            /////key for current session+search
            String cookie = tsc.GetCookie();
            //////
            if (cookie != null && cookie.trim().length() > 0
                    && !(site.getCountyId() == CountyConstants.TN_Montgomery && site.getSiteTypeInt() == GWTDataSite.YA_TYPE)            
                    ) {
                props.put("Cookie", cookie);
            }
           
            else
            {

                cookie = CookieManager.getCookie(Integer.toString(miServerID));
                if (cookie != null)
                    props.put("Cookie", cookie);
            }
            
            props.put("Referer", tsc.getMsReferer());
            ////////////////////////////////////////
            ///fixup for POST requests
            int idx = link.indexOf("?");
            String params;
            if (conntype == ATSConnConstants.POST && idx != -1
                    && !(site.getCountyId() == CountyConstants.TN_Montgomery && site.getSiteTypeInt() == GWTDataSite.YA_TYPE)
                    && !(site.getCountyId() == CountyConstants.KS_Jefferson && site.getSiteTypeInt() == GWTDataSite.MS_TYPE)) {
                params = link.substring(idx + 1);
                
                //there are links like https://pcl.uscourts.gov/cgi-bin/DktRpt.pl?158708736891508-L_920_0-1
                //the last one must remain on the link
                if(params.contains("=")) { 
	                link = link.substring(0, idx);
	                if (encqry == null)
	                    encqry = "";
	                encqry += "&" + params;
                }
            }
            
            if (site.getCountyId() == CountyConstants.KS_Johnson && site.getSiteTypeInt() == GWTDataSite.RO_TYPE) 
            {
                encqry = encqry.replaceAll("(SUBMIT=Detail\\WData\\W\\d+[-])",
                        "$1" + "?");
                encqry = encqry.replaceAll("(DocTypeCats\\d+)", "DocTypeCats");
                encqry = encqry.replaceAll("&test=\\d*&", "&");
                encqry = encqry.replaceAll("&DocTypes=$", "");
                encqry = encqry.replaceAll("&DocTypes=&", "&");
                
            }
            if (site.getCountyId() == CountyConstants.TN_Rutherford && site.getSiteTypeInt() == GWTDataSite.YA_TYPE) {
                encqry = encqry.replaceAll("\\+\\+", "+");
            }
          
            if (site.getSiteTypeInt() == GWTDataSite.PA_TYPE) {
                Search s = InstanceManager.getManager().getCurrentInstance(this.searchId).getCrtSearchContext();
                String lastName = encqry.replaceAll(".*?lastName=([^&]*).*", "$1");
                String firstName = encqry.replaceAll(".*?firstName=([^&]*).*", "$1");
                String name = lastName + ", " + firstName;
                if( name.startsWith( ", " ) )
                {
                    name = name.substring( 2 );
                }
                
                if( name.endsWith( ", " ) )
                {
                    name = name.substring( 0, name.length() - 2 );
                }
                name = name.replaceAll("\\+", " ");
                name = name.replaceAll("%2C", ",");
                name = URLDecoder.decode(name, "UTF-8");
                s.setPatriotSearchName(name.toUpperCase());                
            }
            
            /////////////////////////////////
            log("COOKIE : " + (String) props.get("Cookie"));
            
            ///do connection
            ATSConn c = new ATSConn(666, link, conntype, null, props, followRedirects,searchId,miServerID);
            c.setUsefastlink(!oldlink.contains(site.getCountyId() + "-" + site.getSiteType()));
            c.setTimeout( dbTimeout );
            
            if (site.getCountyId() == CountyConstants.TN_Williamson && site.getSiteTypeInt() == GWTDataSite.AO_TYPE) {

                String streetNo = ro.cst.tsearch.utils.StringUtils
                        .getTextBetweenDelimiters("PropertyStreetNumber=", "&",
                                encqry);
                encqry = encqry.replaceAll("PropertyStreetNumber=\\d*[&]", "");
                if (!ro.cst.tsearch.utils.StringUtils.isStringBlank(streetNo))
                    encqry = encqry.replaceAll("(PropertyStreetName=[^&]*)",
                            "$1+" + streetNo);

                if (encqry != null && encqry.length() > 0)
                    c.setEncQuerry(encqry);
                //c.setTimeout(timeout);
                c.doConnection();

            } else if (site.getCountyId() == CountyConstants.TN_Hamilton && site.getSiteTypeInt() == GWTDataSite.TR_TYPE) {
            	
            	if ( encqry.indexOf("b=") > -1 )
            	{
                    String bVal = StringUtils.getTextBetweenDelimiters("b=", "&" , encqry);
            		link += "&b=" + bVal; 
            		props.put("Referer", link);
            	}
            	c = new ATSConn(666, link, conntype, null, props,searchId,miServerID);
                c.setTimeout( dbTimeout );
                c.setUsefastlink(false);
                c.setFollowRedirects(false);
                
                if (encqry != null && encqry.length() > 0) {
                    c.setEncQuerry(encqry);
                }

                c.doConnection();
                
            } else if (
            		(site.getCountyId() == CountyConstants.MO_Jackson && site.getSiteTypeInt() == GWTDataSite.YA_TYPE) ||
            		(site.getCountyId() == CountyConstants.MO_Jackson && site.getSiteTypeInt() == GWTDataSite.AO_TYPE) || 
                    (site.getCountyId() == CountyConstants.MO_Clay && site.getSiteTypeInt() == GWTDataSite.YA_TYPE)) { 
                
                c.setUsefastlink(false);
                c.setFollowRedirects(true);
                if (encqry != null && encqry.length() > 0)
                {
                    encqry = encqry.replaceAll("parcel_origin\\d=([A-Z])", "parcel_origin=$1");
                    encqry = encqry.replaceAll("parcel_origin\\d=", "");
                    encqry = encqry.replaceAll( "SearchInstructions=", "" );
                    encqry = encqry.replaceAll( "parcel_origin=&", "" );
                    if( encqry.endsWith( "parcel_origin=" ) )
                    {
                        encqry = encqry.replaceAll( "parcel_origin=", "" );
                    }
                    encqry = encqry.replaceAll("&&", "&");
                    
                    c.setEncQuerry(encqry);
                }
                
                if( (site.getCountyId() == CountyConstants.MO_Jackson && site.getSiteTypeInt() == GWTDataSite.YA_TYPE) || 
                		(site.getCountyId() == CountyConstants.MO_Clay && site.getSiteTypeInt() == GWTDataSite.YA_TYPE) )
                {
                    if( encqry.indexOf( "fa=aclladdr" ) != -1 )
                    {
                        HashMap connectionProperties = c.getHReqProps();
                        connectionProperties.put( "User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; .NET CLR 1.1.4322)" );
                        connectionProperties.put( "Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5" );
                        connectionProperties.remove( "Referer" );
                        connectionProperties.remove( "Cookie" );
                        c.setHReqProps( connectionProperties );
                    }
                }
                
                //c.setTimeout(timeout);
                c.doConnection();

            } 
            else {
                
                if( HTTPManager.inst.getHTTPSiteManager(siteName ) != null )
                {
                    c.setSiteId( siteName );
                } else if(HttpManager.isSiteSupported(siteName)){
                	c.setSiteId(siteName);
                } else if(HttpManager3.isSiteSupported(siteName)) {
                	c.setSiteId(siteName);
                }
               
                if (encqry != null && encqry.length() > 0)
                    c.setEncQuerry(encqry);
                
                c.setTimeout( dbTimeout );
                c.doConnection();
            }

            /////port results
            tsc.setMiErrorCode(c.getReturnCode());
            tsc.setMsContentType(c.getResultMimeType());
            tsc.setMiContentLength(c.getContentlength());
          
            tsc.setHmethod(c.getHm());
            tsc.setMbisResponse(new BufferedInputStream(c.getOriginalinput()));
            tsc.setQuery(c.getQuerry());
            tsc.setLastURI(c.getLastURI());
            if (c.getError() != null){
            	if (c.getError() instanceof LoginException){
            		tsc.setErrorMessage("LoginException: " + c.getError().getMessage());
            	}
            }

            Object ck = null;

            try {
                ck = c.getResultHeaders().get("Set-Cookie");
            } catch (NullPointerException npe) {}
            
            if (ck != null) {
                
                String sck = ck.toString();
                sck = sck.replaceAll("\\[", "");
                sck = sck.replaceAll("\\]", "");
                int idxend = sck.indexOf(";");
                if (idxend != -1)
                    sck = sck.substring(0, idxend);

                CookieManager.addCookie(Integer.toString(miServerID), sck);
                tsc.SetCookie(sck); //for compatibility :))
            }

            log("BridgeConn>>>>>>>>>>>HTTP_CODE :" + c.getReturnCode()
                    + " Mime:" + c.getResultMimeType());

            Logme.log("BridgeConn Query : " + c.getEncQuerry() + "\n\n",
                    new Long(searchId));
            Logme.log("BridgeConn Response HTTP_CODE :" + c.getReturnCode()
                    + " Mime:" + c.getResultMimeType() + "\n\n", new Long(
                    searchId));

        } catch (Throwable e) {
            ServerResponse sr = new ServerResponse();
            sr.setError("Invalid Response", ServerResponse.CONNECTION_IO_ERROR);
            e.printStackTrace();
            throw new ServerResponseException(sr);
        }

        checkError();
    }

    public void checkError() throws ServerResponseException {
        if (tsc.GetErrorCode() != TSConnectionURL.OK_CODE) {
            String sTmp = tsc.GetErrorMessage();
            if (sTmp == null)
                sTmp = "Invalid Response";
            ServerResponse Response = new ServerResponse();
            Response.setResult(sTmp);
            Response.setError("Error code: " + tsc.GetErrorCode()
                    + " received by class: " + site.getName());
            int httpInternalError = HttpURLConnection.HTTP_INTERNAL_ERROR;
            if (tsc.GetErrorCode() == ServerResponse.CONNECTION_IO_ERROR && sTmp.startsWith("LoginException")){
            	Response.setResult(sTmp.replaceAll("(?is)<", "&lt;").replaceAll("(?is)>", "&gt;"));	
            } else if (tsc.GetErrorCode() == ServerResponse.CONNECTION_IO_ERROR || tsc.GetErrorCode() == httpInternalError){
            	Response.getParsedResponse().setError("Could not perform search!");
            }
            throw new ServerResponseException(Response);
        }
    }

    /**
     * @return
     */
    public TSConnectionURL getTsc() {
        return tsc;
    }

    /**
     * @param connectionURL
     */
    public void setTsc(TSConnectionURL connectionURL) {
        tsc = connectionURL;
    }

    /**
     * @return
     */
    public boolean isFollowRedirects() {
        return followRedirects;
    }

    /**
     * @param b
     */
    public void setFollowRedirects(boolean b) {
        followRedirects = b;
    }

    private void log(String s) {
        //logger.debug(s);
    }

}