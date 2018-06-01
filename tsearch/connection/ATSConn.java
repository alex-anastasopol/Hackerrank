/*
 * Created on Mar 30, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.connection;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.connection.http.HTTPManager;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSite;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.LoginException;
import ro.cst.tsearch.parentsitedescribe.DSMXMLReader;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.threads.ASThread;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.URLMaping;


public class ATSConn {

    private static final Category logger = Logger.getLogger(TSConnectionURL.class);
    private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
    
    private boolean usefastlink = false;
    
    private int timeout = 40000;
    
    /////////////Request settings////////////////////////
    private String link; //page link like "http://www.go.ro/submit.asp"

    private int sendType; // POST|GET --- see ATSConnConstants

    private HashMap hParams; //pairs of (parameter_name,value)

    private HashMap hReqProps; //request headers that need to be set

    // e.g. Cookie,Referer,User Agent
    private int siteID; // an unique id for each server

    private String ssiteID; ///string siteid ; temporary used

    private boolean useEncoding; // should the parameter values be encoded?

    private String querry; // string of concatenated pairs

    // prm1=val1&prm2=val2...; val(i) is NOT URL-Encoded
    private String encQuerry; ///querry whith encoded parm values

    private boolean followRedirects = true;

    //////////////////Response settings////////////////////////
    private OutputStream response;

    private InputStream errstream;

    private InputStream originalinput;

    private HashMap resHeaders;

    private String mimeType;

    private Throwable error;

    private int returnCode;

    private int contentlength;

    private HttpMethod hm = null;
    
    private URI lastURI = null;

    //////////////////////////////////////////////////////////////////////////////
    public ATSConn(int iid, String llink, int type, HashMap hpar,
            HashMap hprop, boolean redir, long searchId,int miServerId) {
        this(iid, llink, type, hpar, hprop,searchId, miServerId);
        followRedirects = redir;
    }
    
    long searchId=-1;
    int miServerId;
    public ATSConn(int iid, String llink, int type, HashMap hpar, HashMap hprop,long searchId,int miServerId) {
    	this.searchId = searchId;
        this.miServerId =miServerId;
    	siteID = iid;
        link = llink;
        hParams = hpar;
        hReqProps = hprop;
        sendType = type;
        error = null;
        ///building querrys
        querry = null;
        encQuerry = null;
        ssiteID = "";
        try {
            if (hpar != null) {
                querry = "";
                encQuerry = "";
                Set s = hpar.keySet();
                Iterator i = s.iterator();
                while (i.hasNext()) {
                    String p = (String) i.next();
                    String v = (hpar.get(p)).toString();
                    querry += p + "=" + v;
                    encQuerry += p + "=" + URLEncoder.encode(v, "UTF-8");
                    if (i.hasNext()) {
                        querry += "&";
                        encQuerry += "&";
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    
    }

    public void close() {
        
        DataInputStream input = null;

        if (returnCode != ATSConnConstants.HTTP_OK && errstream != null)
            input = new DataInputStream(errstream);
        else
            input = new DataInputStream(originalinput);
        
        
        try {
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public OutputStream getResult() {
        //      Get response data.
        DataInputStream input = null;

        if (returnCode != ATSConnConstants.HTTP_OK && errstream != null)
            input = new DataInputStream(errstream);
        else
            input = new DataInputStream(originalinput);

        response = new ByteArrayOutputStream();
        byte[] b = new byte[10000];
        int n;
        try {
            
            while ((n = input.read(b)) > 0)
                response.write(b, 0, n);
            
            input.close();
            response.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        catch ( NullPointerException npe )
        {
            npe.printStackTrace();
        }
        
        if (urlConn != null)
            ((HttpURLConnection) urlConn).disconnect();
        
        if (hm != null)
            hm.releaseConnection();
        
        return response;
    }

    public HashMap getResultHeaders() {
        return resHeaders;
    }

    public String getResultMimeType() {
        return mimeType;
    }
    
    public void doConnection() {

		returnCode = 1;
		for(int i=0; i<ATSConnConstants.MAX_TRY_COUNT && returnCode != ATSConnConstants.HTTP_OK; i++) {
			
			try{
				if (HttpManager.isSiteSupported(ssiteID)) {
					doThirdConnection();
				} else if(ro.cst.tsearch.connection.http3.HttpManager3.isSiteSupported(ssiteID)){
					doFourthConnection();
				} else {	
					doOldConnections();
				}				
				
				error = null;
				Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				if (search != null && searchId == search.getSearchID()){
					ASThread thread = ASMaster.getSearch(search);
					if (thread != null){
						if (thread.getSkipCurrentSite() || thread.getStopAutomaticSearch()){
							i = ATSConnConstants.MAX_TRY_COUNT;
						}
			        }
				}
				break;
			} catch (LoginException e){
				i = ATSConnConstants.MAX_TRY_COUNT;
				returnCode = ServerResponse.CONNECTION_IO_ERROR;
				error = e;
				
				e.printStackTrace();
				System.err.println("Link = " + link);
				System.err.println("EncQuerry = " + encQuerry);
			}catch (Throwable e) {
                
                error = e;

                if (i == ATSConnConstants.MAX_TRY_COUNT - 1) {
                    returnCode = ServerResponse.CONNECTION_IO_ERROR;
                    mimeType = "ERROR!";
                    response = new ByteArrayOutputStream();
                    try {
                        response.write(("Internal Error:" + e.toString() + "\n").getBytes());
                        response.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                e.printStackTrace();
                System.err.println("Link = " + link);
                System.err.println("EncQuerry = " + encQuerry);                
            } 
		}
    }
    
    /**
     * Handle Third Connection
     * @throws Exception
     */
    private void doThirdConnection() throws Exception {
		HttpSite site = HttpManager.getSite(ssiteID, searchId);
		try{
			fetchPage3(site);
		}finally{
			HttpManager.releaseSite(site);
		}
    }
    
    /**
     * Handle Fourth Connection
     * @throws Exception
     */
    private void doFourthConnection() throws Exception {
		ro.cst.tsearch.connection.http3.HttpSite3 site = ro.cst.tsearch.connection.http3.HttpManager3.getSite(ssiteID, searchId);
		try{
			fetchPage4(site);
		}finally{
			ro.cst.tsearch.connection.http3.HttpManager3.releaseSite(site);
		}
    }
		
    public void doOldConnections() throws Throwable {

		HTTPSite site = null;
		boolean userDataSet = ssiteID.endsWith("PA") || ssiteID.equals("TNShelbyPC") || ssiteID.equals("KSJohnsonPC")
				|| ssiteID.equals("KYJeffersonPC") || ssiteID.equals("OHFranklinPC") || ssiteID.equals("MIOaklandPC")
				|| ssiteID.equals("MOClayRO") || ssiteID.equals("TNMontgomeryTR") 
				|| ssiteID.equals("TNShelbyRO") || ssiteID.equals("KYJeffersonRO") || ssiteID.equals("KYJeffersonTR")
				|| ssiteID.equals("OHFranklinAO") || ssiteID.equals("OHFranklinTR") || ssiteID.equals("OHFranklinRO")
				|| ssiteID.equals("OHFranklinCO") || ssiteID.equals("KYJeffersonMS") || ssiteID.equals("MDBaltimorePC")
				|| ssiteID.equals("TNSumnerTR") || ssiteID.equals("MOJacksonTR") || ssiteID.equals("MOClayTR")
				|| ssiteID.equals("FLHillsboroughDT") || ssiteID.equals("MIOaklandDT") || ssiteID.equals("MIMacombDT")
				|| ssiteID.equals("MIWayneDT") || ssiteID.equals("TNShelbyDT") || ssiteID.equals("MIOaklandRO")
				|| ssiteID.equals("MIMacombCO") || ssiteID.equals("MIMacombRO") || ssiteID.equals("MIOaklandCO")
				|| ssiteID.equals("MIWayneTR") || ssiteID.equals("MIWayneEP") || ssiteID.equals("TNWilliamsonTR") || ssiteID.equals("TNWilsonTR")
				|| ssiteID.equals("MIMacombTR") || ssiteID.equals("MIWaynePR") || ssiteID.equals("ILCookRO")
				|| ssiteID.equals("ILCookAO") || ssiteID.equals("ILCookIS") || ssiteID.equals("ILCookTR")
				|| ssiteID.equals("ILCookPC") || ssiteID.equals("MIWaynePC") || ssiteID.equals("TNAndersonAO")
				|| ssiteID.equals("TNBedfordAO") || ssiteID.equals("TNBentonAO") || ssiteID.equals("TNBledsoeAO")
				|| ssiteID.equals("TNBlountAO") || ssiteID.equals("TNBradleyAO") || ssiteID.equals("TNCampbellAO")
				|| ssiteID.equals("TNCannonAO") || ssiteID.equals("TNCarrollAO") || ssiteID.equals("TNCarterAO")
				|| ssiteID.equals("TNCheathamAO") || ssiteID.equals("TNChesterAO") || ssiteID.equals("TNClaiborneAO")
				|| ssiteID.equals("TNClayAO") || ssiteID.equals("TNCockeAO") || ssiteID.equals("TNCoffeeAO")
				|| ssiteID.equals("TNCrockettAO") || ssiteID.equals("TNCumberlandAO") || ssiteID.equals("TNDecaturAO")
				|| ssiteID.equals("TNDe KalbAO") || ssiteID.equals("TNDeKalbAO") || ssiteID.equals("TNDicksonAO")
				|| ssiteID.equals("TNDyerAO") || ssiteID.equals("TNFayetteAO") || ssiteID.equals("TNFentressAO")
				|| ssiteID.equals("TNFranklinAO") || ssiteID.equals("TNGibsonAO") || ssiteID.equals("TNGilesAO")
				|| ssiteID.equals("TNGraingerAO") || ssiteID.equals("TNGreeneAO") || ssiteID.equals("TNGrundyAO")
				|| ssiteID.equals("TNHamblenAO") || ssiteID.equals("TNHancockAO") || ssiteID.equals("TNHardemanAO")
				|| ssiteID.equals("TNHardinAO") || ssiteID.equals("TNHawkinsAO") || ssiteID.equals("TNHaywoodAO")
				|| ssiteID.equals("TNHendersonAO") || ssiteID.equals("TNHenryAO") || ssiteID.equals("TNHickmanAO")
				|| ssiteID.equals("TNHoustonAO") || ssiteID.equals("TNHumphreysAO") || ssiteID.equals("TNJacksonAO")
				|| ssiteID.equals("TNJeffersonAO") || ssiteID.equals("TNJohnsonAO") || ssiteID.equals("TNLakeAO")
				|| ssiteID.equals("TNLauderdaleAO") || ssiteID.equals("TNLawrenceAO") || ssiteID.equals("TNLewisAO")
				|| ssiteID.equals("TNLincolnAO") || ssiteID.equals("TNLoudonAO") || ssiteID.equals("TNMaconAO")
				|| ssiteID.equals("TNMadisonAO") || ssiteID.equals("TNMarionAO") || ssiteID.equals("TNMarshallAO")
				|| ssiteID.equals("TNMauryAO") || ssiteID.equals("TNMcMinnAO") || ssiteID.equals("TNMcNairyAO")
				|| ssiteID.equals("TNMeigsAO") || ssiteID.equals("TNMonroeAO") || ssiteID.equals("TNMooreAO")
				|| ssiteID.equals("TNMorganAO") || ssiteID.equals("TNObionAO") || ssiteID.equals("TNOvertonAO")
				|| ssiteID.equals("TNPerryAO") || ssiteID.equals("TNPickettAO") || ssiteID.equals("TNPolkAO")
				|| ssiteID.equals("TNPutnamAO") || ssiteID.equals("TNRheaAO") || ssiteID.equals("TNRoaneAO")
				|| ssiteID.equals("TNRobertsonAO") || ssiteID.equals("TNScottAO") || ssiteID.equals("TNSequatchieAO")
				|| ssiteID.equals("TNSevierAO") || ssiteID.equals("TNSmithAO") || ssiteID.equals("TNStewartAO")
				|| ssiteID.equals("TNSullivanAO") || ssiteID.equals("TNTiptonAO") || ssiteID.equals("TNTrousdaleAO")
				|| ssiteID.equals("TNUnicoiAO") || ssiteID.equals("TNUnionAO") || ssiteID.equals("TNVan BurenAO")
				|| ssiteID.equals("TNVanBurenAO") || ssiteID.equals("TNWarrenAO") || ssiteID.equals("TNWashingtonAO")
				|| ssiteID.equals("TNWayneAO") || ssiteID.equals("TNWeakleyAO") || ssiteID.equals("TNWhiteAO")
				|| ssiteID.equals("ILCookIM") || ssiteID.equals("TNFranklinTR") || ssiteID.equals("TNLakeTR")
				|| ssiteID.equals("TNObionTR") || ssiteID.equals("TNWeakleyTR") || ssiteID.equals("TNHenryTR")
				|| ssiteID.equals("TNDyerTR") || ssiteID.equals("TNGibsonTR") || ssiteID.equals("TNTiptonTR")
				|| ssiteID.equals("TNMadisonTR") || ssiteID.equals("TNChesterTR") || ssiteID.equals("TNFayetteTR")
				|| ssiteID.equals("TNHardemanTR") || ssiteID.equals("TNDicksonTR") || ssiteID.equals("TNLawrenceTR")
				|| ssiteID.equals("TNMauryTR") || ssiteID.equals("TNTrousdaleTR") || ssiteID.equals("TNRutherfordTR")
				|| ssiteID.equals("TNCoffeeTR") || ssiteID.equals("TNMooreTR") || ssiteID.equals("TNWarrenTR")
				|| ssiteID.equals("TNPutnamTR") || ssiteID.equals("TNMarionTR") || ssiteID.equals("TNBradleyTR")
				|| ssiteID.equals("TNPolkTR") || ssiteID.equals("TNMonroeTR") || ssiteID.equals("TNLoudonTR")
				|| ssiteID.equals("TNSevierTR") || ssiteID.equals("TNObionTR") || ssiteID.equals("TNCrockettTR")
				|| ssiteID.equals("TNStewartTR") || ssiteID.equals("TNDecaturTR") || ssiteID.equals("TNHickmanTR")
				|| ssiteID.equals("TNWhiteTR") || ssiteID.equals("TNGreeneTR") || ssiteID.equals("TNHamblenTR")
				|| ssiteID.equals("TNScottTR")
				|| ssiteID.equals("FLMiamiDadeTR") || ssiteID.equals("FLMiami-DadeTR")
				|| ssiteID.equals("FLPalmBeachTR") || ssiteID.equals("FLPalm BeachTR")
				|| ssiteID.equals("FLHillsboroughTR") || ssiteID.equals("FLHillsboroughPC")
				|| ssiteID.equals("FLSarasotaTR") || ssiteID.equals("FLOrangeTR") || ssiteID.equals("FLDuvalTR")|| ssiteID.equals("FLDeSotoTR") || ssiteID.equals("FLOkaloosaTR") 
				|| ssiteID.equals("FLVolusiaTR") || ssiteID.equals("FLLakeTR") || ssiteID.equals("FLSt JohnsTR") || ssiteID.equals("FLStJohnsTR") || ssiteID.equals("FLSt. JohnsTR")
				|| ssiteID.equals("FLIndian RiverTR") || ssiteID.equals("FLIndianRiverTR") || ssiteID.equals("FLSt LucieTR") || ssiteID.equals("FLStLucieTR")
				|| ssiteID.equals("FLOsceolaTR") || ssiteID.equals("FLMartinTR")
				|| ssiteID.equals("MIMacombPC") || ssiteID.equals("FLHillsboroughRO") || ssiteID.equals("FLPinellasTR")
				|| ssiteID.equals( "TNHamiltonRO" ) || ssiteID.equals( "TNKnoxYA" ) || ssiteID.equals( "FLSeminoleTR" ) || ssiteID.equals("FLPascoTR")
				|| ssiteID.equals("ILCookTU") || ssiteID.equals("FLLeeTR") || ssiteID.equals("FLNassauTR") || ssiteID.equals("FLCharlotteTR")
				|| ssiteID.equals("FLBrevardTR") || ssiteID.equals("FLEscambiaTR")||DSMXMLReader.isSite(ssiteID)|| ssiteID.equals("FLLeeRV") || ssiteID.equals("FLCollierTR") 
				|| ssiteID.equals("FLWaltonTR") || ssiteID.equals("FLSumterTR") ;
		/* || ssiteID.equals("MIWayneRO"); */
		//DSMXMLReader.isSite() - if is xml file associated whit site nama (ssiteID) then return true.  problems is whit site whence containts char ". space" ex FLSt. lucieTR

		try {

			if (!ssiteID.equals("")) {

				// site id is set
				if (userDataSet) {
					String siteKey = searchId + ssiteID;

					site = InstanceManager.getManager().getCurrentInstance(searchId).getSite(siteKey);

					if (site == null) {
						site = HTTPManager.getSite(ssiteID, siteKey, searchId, miServerId);

						InstanceManager.getManager().getCurrentInstance(searchId).setSite(siteKey, site);
					}

					fetchPage3(site);
				} else {
					site = HTTPManager.getSite(ssiteID, searchId, miServerId);

					fetchPage3(site);
				}

			} else {
				// normal fetch
				if (link.indexOf("register.hamiltontn.gov") != -1) {
					fetchPage3(site = HTTPManager.getSite("TNHamiltonRO", searchId, miServerId));
				} else if (link.indexOf("hamiltontn.gov/Trustee") != -1) {
					fetchPage3(site = HTTPManager.getSite("TNHamiltonTR", searchId, miServerId));
				} else if (link.indexOf("propertytax.chattanooga.gov") != -1) {
					fetchPage3(site = HTTPManager.getSite("TNHamiltonEP", searchId, miServerId));
				} else if (link.indexOf("tn-davidson-taxcollector.governmax.com") != -1) {
					fetchPage3(site = HTTPManager.getSite("TNDavidsonTR", searchId, miServerId));
				}

				else if (isUsefastlink()) {
					// BridgeConn.logger.debug("Using Jakarta HttpClient!");
					fetchPage2();
				} else {
					// BridgeConn.logger.debug("Using Java URLConnection!");
					fetchPage();
				}

			}

		} finally {

			if (site != null && site.getUserData() != null && !userDataSet)
				site.unlock();
		}

	}
    
    private void fetchPage4(HTTPSiteInterface site) throws Exception 
    {   
        HTTPRequest req = null;
    
        if (sendType == ATSConnConstants.GET) 
        {   
            String q = "";
            if (encQuerry != null && querry.indexOf(" ") != -1) {
            	if("".equals(encQuerry)) {
            		q = "";
            	} else {
            		if(encQuerry.startsWith("?")) {
            			if(link.contains("?")) {
            				q = encQuerry.replaceFirst("?", "&");
            			} else {
            				q = encQuerry;
            			}
            		} else {
            			if(link.contains("?")) {
            				q = "&" + encQuerry;
            			} else {
            				q = "?" + encQuerry;	
            			}
            			
            		}
            	}
            	
                //q = ("".equals(encQuerry) ? "" : (encQuerry.startsWith("?") ? encQuerry : "?" + encQuerry) );
                
            }
            else if (querry != null) {
            	if("".equals(querry)) {
            		q = "";
            	} else {
            		if(querry.startsWith("?")) {
            			q = querry;
            		} else if(link.contains("?")) {
            			if(!link.endsWith("&")) {
            				q = "&" + querry;
            			} else {
            				q = querry;
            			}
            		} else {
            			q = "?" + querry;
            		}
            	}
                
            }
            
            req = new HTTPRequest(link + q);
            req.setMethod(HTTPRequest.GET);
        } 
        else if (sendType == ATSConnConstants.POST) 
        {
            req = new HTTPRequest(link);
            req.setMethod(HTTPRequest.POST);

            if (encQuerry != null) {
                String toks[] = StringUtils.split(encQuerry, "&");
    
                for (int i = 0; i < toks.length; i++) 
                {
                    if (!toks[i].startsWith("=")) {
                        req.setPostParameter( URLDecoder.decode(toks[i].substring(0, toks[i].indexOf("=")), "UTF-8"),
                                    URLDecoder.decode(toks[i].substring(toks[i].indexOf("=") + 1), "UTF-8"));
                    }                               
                }
            }
        }
        

        HTTPResponse res = site.process(req);
        //System.out.println(res.getResponseAsString());

        if (res != null) {
        
            resHeaders = res.getHeaders();
            mimeType = res.getContentType();
            returnCode = res.getReturnCode();
            contentlength = (int) res.getContentLenght();
            originalinput = res.getResponseAsStream();
            lastURI = res.getLastURI();
        
            String url = req.getURL();
            if (url != null) {
                int index = url.indexOf("?");
                if (index > 0)
                    querry = url.substring(index + 1);
            }
        }
    }

    private void fetchPage3(HTTPSiteInterface site) throws Exception 
    {   
        HTTPRequest req = null;
    
        if (sendType == ATSConnConstants.GET) 
        {   
            String q = "";
            if (encQuerry != null && querry.indexOf(" ") != -1) {
            	if("".equals(encQuerry)) {
            		q = "";
            	} else {
            		if(encQuerry.startsWith("?")) {
            			if(link.contains("?")) {
            				q = encQuerry.replaceFirst("?", "&");
            			} else {
            				q = encQuerry;
            			}
            		} else {
            			if(link.contains("?")) {
            				q = "&" + encQuerry;
            			} else {
            				q = "?" + encQuerry;	
            			}
            			
            		}
            	}
            	
                //q = ("".equals(encQuerry) ? "" : (encQuerry.startsWith("?") ? encQuerry : "?" + encQuerry) );
                
            }
            else if (querry != null) {
            	if("".equals(querry)) {
            		q = "";
            	} else {
            		if(querry.startsWith("?")) {
            			q = querry;
            		} else if(link.contains("?")) {
            			if(!link.endsWith("&")) {
            				q = "&" + querry;
            			} else {
            				q = querry;
            			}
            		} else {
            			q = "?" + querry;
            		}
            	}
                
            }
            
            req = new HTTPRequest(link + q);
            req.setMethod(HTTPRequest.GET);
        } 
        else if (sendType == ATSConnConstants.POST) 
        {
            req = new HTTPRequest(link);
            req.setMethod(HTTPRequest.POST);

            if (encQuerry != null) {
                String toks[] = StringUtils.split(encQuerry, "&");
    
                for (int i = 0; i < toks.length; i++) 
                {
                    if (!toks[i].startsWith("=")) {
                        req.setPostParameter( URLDecoder.decode(toks[i].substring(0, toks[i].indexOf("=")), "UTF-8"),
                                    URLDecoder.decode(toks[i].substring(toks[i].indexOf("=") + 1), "UTF-8"));
                    }                               
                }
            }
        }
        

        HTTPResponse res = site.process(req);
        //System.out.println(res.getResponseAsString());

        if (res != null) {
        
            resHeaders = res.getHeaders();
            mimeType = res.getContentType();
            returnCode = res.getReturnCode();
            contentlength = (int) res.getContentLenght();
            originalinput = res.getResponseAsStream();
            lastURI = res.getLastURI();
        
            String url = req.getURL();
            if (url != null) {
                int index = url.indexOf("?");
                if (index > 0)
                    querry = url.substring(index + 1);
            }
        }
    }
    
    private void fetchPage2() throws Exception {
        //config method
        if (sendType == ATSConnConstants.GET) {
            hm = new GetMethod(link);
            hm.setQueryString(encQuerry);
        } else if (sendType == ATSConnConstants.POST) {
            hm = new PostMethod(link);
            String toks[] = StringUtils.split(encQuerry, "&");
            NameValuePair[] data = new NameValuePair[toks.length];
            for (int i = 0; i < toks.length; i++) {
                data[i] = new NameValuePair();
                /// prevent fucked up querrys
                if (!toks[i].startsWith("=")) {
                	String paramName = toks[i].substring(0, toks[i].indexOf("="));
                	if( link.indexOf("stewartpriorfiles") >= 0 ){
                		paramName = URLDecoder.decode( paramName , "UTF-8");
                	}
                    data[i].setName( paramName );
                    data[i].setValue(URLDecoder.decode(toks[i]
                            .substring(toks[i].indexOf("=") + 1), "UTF-8"));
                }
            }
            ((PostMethod) hm).setRequestBody(data);
        }
        if (hReqProps != null) {
            Set s = hReqProps.keySet();
            Iterator i = s.iterator();
            while (i.hasNext()) {
                String p = (String) i.next();
                String v = (hReqProps.get(p)).toString();
                hm.addRequestHeader(p, v);
            }
        }
        //hm.setFollowRedirects(isFollowRedirects());
        //setTimeout

        HttpMethodParams hmpar = new HttpMethodParams();
        hmpar.setSoTimeout(timeout);
        HttpMethodRetryHandler retryhandler = new DefaultHttpMethodRetryHandler(0, true);
        hmpar.setParameter(HttpMethodParams.RETRY_HANDLER, retryhandler);
        hm.setParams(hmpar);
        //execute request
        try {
            HttpClient hclient = new HttpClient();
            
            try{
            	if(ServerConfig.isBurbProxyEnabled()) {
                	HostConfiguration config = hclient.getHostConfiguration();
        	        config.setProxy("127.0.0.1", ServerConfig.getBurbProxyPort(8081));
//                	config.setProxy("beta", 8888);
                }
            	
            }
            catch( Exception e ){
            	e.printStackTrace();
            }
            int code = hclient.executeMethod(hm);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error while performing Jakarta HTTP Method! Parent site most likely down! Link: " + link);
        }
        //save results
        //      saving connection headers
        Header[] hdrs = hm.getResponseHeaders();
        resHeaders = new HashMap();
        if (hdrs != null && hdrs.length > 0) {
            for (int i = 0; i < hdrs.length; i++)
                resHeaders.put(hdrs[i].getName(), hdrs[i].getValue());
        
            //getting mime type & other info
            Header contentType = hm.getResponseHeader("Content-Type");
            if (contentType != null && contentType.getValue() != null) 
                mimeType = contentType.getValue().trim();
            returnCode = hm.getStatusCode();
            Header lngth = hm.getResponseHeader("Content-Length");
            if (lngth != null)
                contentlength = Integer.parseInt(lngth.getValue().trim());
            else
                contentlength = 0;
        }
        //if(returnCode==ATSConnConstants.HTTP_OK)
        originalinput = hm.getResponseBodyAsStream();
        if (returnCode != ATSConnConstants.HTTP_OK && originalinput == null) {
            originalinput = errstream;
        }
        
        //hm.releaseConnection();
    }

    public URLConnection urlConn;
    private void fetchPage() throws Exception {
        URL url;
        
        String page = link;
        String siteName =  HashCountyToIndex.getDateSiteForMIServerID(
        		InstanceManager.getManager().getCommunityId(searchId),
        		miServerId).getName();
        if (siteName.contains("KSJohnsonRO") && page.contains("Search.asp"))
        	sendType = 1; 
        if (encQuerry != null && encQuerry.length() > 0)
            if (sendType == ATSConnConstants.GET)
                page += "?" + encQuerry;

        url = new URL(page);
        
        //please do not delete this
        if(ServerConfig.isBurbProxyEnabled()) {
        	Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", ServerConfig.getBurbProxyPort(8081)));
//        	Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("beta", 8888));
        	urlConn = url.openConnection(proxy);
        } else {
        	urlConn = url.openConnection();
        }
        
        urlConn.setConnectTimeout( timeout );
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        
        //follow redirects?
        ((HttpURLConnection) urlConn).setInstanceFollowRedirects(isFollowRedirects());
        //configuring properties
        if (hReqProps != null) {
            Set s = hReqProps.keySet();
            Iterator i = s.iterator();
            while (i.hasNext()) {
                String p = (String) i.next();
                String v = (hReqProps.get(p)).toString();
                v = v.replaceAll("(?is)(ASP[^;]+;)(.*)(Session0.*)(Session1.*)(Session2[^;]+)(; Session2[^;]+;) (Session2[^;]+;.*)", "$1" + "$3" + "$4" + "$5");
                urlConn.setRequestProperty(p, v);
            }
        }

        //setting action type POST/GET and sending params
        if (sendType == ATSConnConstants.GET)
            ((HttpURLConnection) urlConn).setRequestMethod("GET");
        if (sendType == ATSConnConstants.POST) {
            ((HttpURLConnection) urlConn).setRequestMethod("POST");
            if (encQuerry != null) {
                
                //System.err.println("POST query:   " + encQuerry);
                ((HttpURLConnection) urlConn).setRequestProperty(
                        "Content-Length", new Long(encQuerry.getBytes().length)
                                .toString());
                //sending parameters through POST
                DataOutputStream printout = new DataOutputStream(urlConn
                        .getOutputStream());
                printout.writeBytes(encQuerry);
                printout.flush();
                printout.close();
            }

        }

        //saving connection headers
        resHeaders = new HashMap(urlConn.getHeaderFields());

        //getting mime type & other info
        mimeType = urlConn.getContentType();
        returnCode = ((HttpURLConnection) urlConn).getResponseCode();
        contentlength = urlConn.getContentLength();
        originalinput = null;
        
        if (returnCode == ATSConnConstants.HTTP_OK)
            originalinput = urlConn.getInputStream();
        
        if (returnCode != ATSConnConstants.HTTP_OK && originalinput == null) {
            errstream = ((HttpURLConnection) urlConn).getErrorStream();
            originalinput = errstream;
        }
        
        //((HttpURLConnection) urlConn).disconnect();
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

    /**
     * @return
     */
    public int getReturnCode() {
        return returnCode;
    }
    
    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    /**
     * @return
     */
    public String getEncQuerry() {
        return encQuerry;
    }

    /**
     * @return
     */
    public String getQuerry() {
        return querry;
    }

    /**
     * @param string
     */
    public void setEncQuerry(String string) {
        encQuerry = string;

        try {
            querry = URLDecoder.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @param string
     */
    public void setQuerry(String string) {
        querry = string;
        try {
            encQuerry = URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @return
     */
    public int getContentlength() {
        return contentlength;
    }

    /**
     * @return
     */
    public InputStream getErrstream() {
        return errstream;
    }

    public Throwable getError(){
    	return error;
    }
    /**
     * @return
     */
    public InputStream getOriginalinput() {
        return originalinput;
    }

    /**
     * @param stream
     */
    public void setOriginalinput(InputStream stream) {
        originalinput = stream;
    }

    /**
     * @return
     */
    public boolean isUsefastlink() {
        return usefastlink;
    }

    /**
     * @param b
     */
    public void setUsefastlink(boolean b) {
        usefastlink = b;
    }

    /**
     * @return
     */
    public HttpMethod getHm() {
        return hm;
    }

    /**
     * @param method
     */
    public void setHm(HttpMethod method) {
        hm = method;
    }

    /**
     * @return
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * @param i
     */
    public void setTimeout(int i) {
        timeout = i;
    }

    /**
     * @return Returns the hReqProps.
     */
    public HashMap getHReqProps() {
        return hReqProps;
    }

    /**
     * @param reqProps The hReqProps to set.
     */
    public void setHReqProps(HashMap reqProps) {
        hReqProps = reqProps;
    }
    
    
    public void setSiteId( String siteId )
    {
        if( siteId != null )
        {
            ssiteID = siteId;
        }
    }

	public URI getLastURI() {
		return lastURI;
	}

	public void setLastURI(URI lastURI) {
		this.lastURI = lastURI;
	}
}