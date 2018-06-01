package ro.cst.tsearch.servers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.log4j.Logger;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.monitor.TSConnectionTime;
import ro.cst.tsearch.servers.info.TSServerInfoParam;
import ro.cst.tsearch.servers.response.RawResponseWrapper;
import ro.cst.tsearch.servers.response.ServerResponse;

/** 
 * @author costin
 */
public class TSConnectionURL implements Serializable, Cloneable {

    static final long serialVersionUID = 10000000;

    private static final Logger logger = Logger.getLogger(TSConnectionURL.class);

//    private static final Logger loggerDetails = Logger.getLogger(Log.DETAILS_PREFIX + TSConnectionURL.class.getName());
//
//    private static final Logger loggerCookie = Logger.getLogger(Log.DETAILS_PREFIX + Cookie.class.getName());

    /////////////////////////////////////////////////////////////////////////
    /* CONSTANTS */
    /////////////////////////////////////////////////////////////////////////
    /////////////////////////////
    //PUBLIC
    public static final int idPOST = 1;

    public static final int idGET = 2;
    
    public static final int idDASL = 3;

    public static final int OK_CODE = 200;

    public static final String HTML_CONTENT_TYPE = "text/html";
    public static final String XML_CONTENT_TYPE = "text/xml";
    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String TEXT_PLAIN_CONTENT_TYPE = "text/plain";

    /////////////////////////////
    ///////Cookie parameter
    public static final String CookieParameter = "ATSSiteCookie";

    ////////////////////////////////////////
    //PRIVATE
    private final static int BUF_SIZE = 8192;

    /////////////////////////////////////////////////////////////////////////
    /* VARS */
    /////////////////////////////////////////////////////////////////////////
    /////////////////////////////
    //PRIVATE
    private String msHostName = "", //host Name adress REQUIRED
            msRHostName = "", //host Name adress REQUIRED for refresh action
            msHostIP = "",//autodetected when host name is set
            msRHostIP = "",//autodetected when host name is set for refresh
            // action
            msData = "",//parameters and values pased to host through GET
            // method
            msRData = "",//parameters and values pased to host through GET
            // method for refresh action
            msReferer = "",//address of page from which we making the request
            msRReferer = "",//address of page from which we making the request
            // for refresh action
            msContentType = "",//file type returned by server
            msInternalError = "",//store any intenal error appeared during a
            // server request
            msNewLocation = "",//if server answer obj moved then here will be
            // stored the new location
            msDestinationPage = "", msRDestinationPage = "",//destination page
            // for refresh
            // action
            msRSpeacialHead = "";//last special header passed....used for

    private boolean removeParamValueQuotes = true ;
    // refresh action

    private int miErrorCode = OK_CODE,//error number returned by server if
            // there is any
            miContentLength = 0,//file length returned by server
            miRCommunicationType = idGET;//Last comunication type passed...used

    // for refresh action

    private transient HttpURLConnection urlConnToSend = null;

    private transient HttpMethod hmethod = null;

    private transient BufferedInputStream mbisResponse = null;

    private Cookie cookie = new Cookie();

    private Cookie cookieRefresh = new Cookie();
    
    private URI lastURI = null;

    /////////////////////////////
    //PUBLIC
    /**
     * @author costin add parameter name and parameter value to request string
     * @param rbNewQuery=true
     *            then delete all previous set parameters and add this new one
     * @param rsParName--name
     *            of parameter if this is null then nothing is add
     * @param rsParValue--name
     *            of parameter if this is null then nothing is add
     *  
     */
    
    public void BuildQuery(String rsParName, String rsParValue,
            boolean rbNewQuery){
    	// the default behaviour is with encoding enabled so disableEncoding=false
    	BuildQuery(rsParName, rsParValue, rbNewQuery, false);
    }
    
    // the only place from which this function is called with disableEncoding=true is
    // from TSServer.GetRequestSettings() in case of parent search on OHFranklinPR
    public void BuildQuery(String rsParName, String rsParValue,
            boolean rbNewQuery, boolean disableEncoding) {
        if (rbNewQuery) {
            msData = "";
        }
		if (rsParName != null || rsParValue != null) {
			if (msData.length() > 0) {
				msData += '&';
			}
			try {
				if (rsParName == null) {
					msData += "="
							+ URLEncoder.encode(
									removeParamValueQuotes ? rsParValue.replaceAll("\"", "") : rsParValue, "UTF-8");
				} else if (rsParValue == null) {
					msData += URLEncoder.encode(
							removeParamValueQuotes ? rsParName.replaceAll("\"", "") : rsParName, "UTF-8");
				} else {
					if (!disableEncoding)
					{
						msData += URLEncoder.encode(removeParamValueQuotes ? rsParName.replaceAll("\"", "") : rsParName, "UTF-8")
								+ "="
								+ URLEncoder.encode(
										removeParamValueQuotes ? rsParValue.replaceAll("\"", "") : rsParValue, "UTF-8");
					} else {
						msData += rsParName + "=" + rsParValue.replaceAll(" ", "%20");
					}
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

        // manearala de manareala ...(ref Designing Patterns by Bruce Eckel)
        if (msData != null) {
            msData = msData.replaceAll("%2520", "%20");
        }
    }

    public void buildQuery(Map params, boolean newQuery) {
    	boolean disableEncoding = false;
    	
    	for (Iterator iter = params.entrySet().iterator(); iter.hasNext();) {
            Map.Entry pair = (Map.Entry) iter.next();
            String key = null, value = null;
            if (pair.getKey() instanceof String) {                
                key = (String) pair.getKey();
                value = (String) pair.getValue();                
            } else if (pair.getKey() instanceof TSServerInfoParam) {                
                TSServerInfoParam infoParam = (TSServerInfoParam) pair.getValue();
                key = infoParam.name;
                value = infoParam.value;
            }
            if(key.equals("disableEncoding") && value.equals("true")){
            	disableEncoding = true;
            	params.remove(pair.getKey());
            	break;
            }
    		
    	}
    	
        for (Iterator iter = params.entrySet().iterator(); iter.hasNext();) {
            
            Map.Entry pair = (Map.Entry) iter.next();
            
            if (pair.getKey() instanceof String) {
                
                BuildQuery((String) pair.getKey(), (String) pair.getValue(),
                        newQuery, disableEncoding);
                
            } else if (pair.getKey() instanceof TSServerInfoParam) {
                
                TSServerInfoParam infoParam = (TSServerInfoParam) pair.getValue();
                
                BuildQuery(infoParam.name, infoParam.value, newQuery, disableEncoding);
            }
        }
    }

    public void checkForSeverResponseError(String className)
            throws ServerResponseException {
        if (GetErrorCode() != TSConnectionURL.OK_CODE) {
            String sTmp = GetErrorMessage();
            if (sTmp == null)
                sTmp = "NULL RESPONSE";
            ServerResponse Response = new ServerResponse();
            Response.setResult(sTmp);
            Response.setError("Error code: " + GetErrorCode()
                    + " received by class: " + className);
            throw new ServerResponseException(Response);
        }
    }

    public String GetErrorMessage() {
        if (!msInternalError.equals("")) {
            return msInternalError;
        } else {
            if (mbisResponse == null)
                mbisResponse = new BufferedInputStream(urlConnToSend
                        .getErrorStream());
            return getTextFromBuffer(mbisResponse);
        }
    }
    
    public void setErrorMessage(String errorMesage) {
        msInternalError = errorMesage != null ? errorMesage : "";
    }

    public String getTextFromBuffer(BufferedInputStream buf) {
        if (buf == null)
            return null;

        int iReaded = 0;
        StringBuffer sBfr = new StringBuffer();
        boolean readSomething = false;
        byte[] baBuf = new byte[BUF_SIZE];
        //BufferedReader br=new BufferedReader(new
        // InputStreamReader(buf),8192);
        long t0 = System.currentTimeMillis();
        try {
            while ((iReaded = buf.read(baBuf)) != -1) {
//                logger.info( "Read " + iReaded + " bytes " );
                readSomething = true;
                sBfr.append(new String(baBuf, 0, iReaded));
            }
            /*
             * String line=null; while ((line=br.readLine())!=null) {
             * readSomething = true; sBfr.append(line); sBfr.append("\n"); }
             */
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(" Error reading from input stream buffer ", e);
        } finally {
            if (hmethod != null)
                hmethod.releaseConnection();
        }
        long t1 = System.currentTimeMillis();
        if (logger.isDebugEnabled())
            logger.debug("Text download time :" + (t1 - t0));
        //return readed string;
        if (readSomething)
            return sBfr.toString();
        else
            return null;
    }

    public RawResponseWrapper getResponseWrapper() {

        long startTime = System.currentTimeMillis();

        String textResponse = "";
        BufferedInputStream buf = null;
        if (msContentType.toLowerCase().indexOf(TSConnectionURL.HTML_CONTENT_TYPE) != -1 ||
        		msContentType.toLowerCase().indexOf(TSConnectionURL.XML_CONTENT_TYPE) != -1 ) {
            textResponse = getTextFromBuffer(GetResponseBuffer());
        } else {
            buf = GetResponseBuffer();
        }

        RawResponseWrapper rrw = new RawResponseWrapper(msContentType,
                miContentLength, textResponse, buf, hmethod);

        TSConnectionTime.update(System.currentTimeMillis() - startTime);

        return rrw;
    }

    /**
     * @author costin Get the response from the server.
     * @param b
     *            is the readed byte arrya
     * @return the number of bytes readed from server response string. If this
     *         function is called before any requests to the server to be made
     *         then it return -1.
     */
    /*
     * public int GetRespBn(byte[] b) { if (!msInternalError.equals("")) return
     * -1; try { if (mbisResponse ==null) mbisResponse=new
     * BufferedInputStream(urlConnToSend.getInputStream()); return
     * mbisResponse.read(b); } catch (IOException e) { e.printStackTrace();
     * return -1; } }
     */
    private BufferedInputStream GetResponseBuffer() {
        try {
            if (mbisResponse == null)
                mbisResponse = new BufferedInputStream(urlConnToSend
                        .getInputStream());
            return mbisResponse;
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(" Get Response Buffer Error ", e);
            return null;
        }
    }

    /////////////////////////////////////////////////////////////////////////
    /*PROPERTIES*/
    /////////////////////////////////////////////////////////////////////////
    /////////////////////////////
    //PUBLIC
    public String GetReferer() {
        return msReferer;
    }

    /**address of page from which we making the request*/
    public void SetReferer(String rsVal) {
        msReferer = rsVal;
    }

    /**@return received cookie*/
    public String GetCookie() {
        return cookie.getValue();
    }

    public void SetCookie(String rsVal) {
        cookie.setValue(rsVal);
    }

    public void setCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    /**@return error number returned by server if there is any by default it is 200=OK*/
    public int GetErrorCode() {
        return miErrorCode;
    }

    /**@return the new location if the server returned "object moved"*/
    public String NewLocation() {
        return msNewLocation;
    }

    /**
     * Returns the hostIP.
     * @return String
     */
    public String getHostIP() {
        return msHostIP;
    }

    /**
     * Returns the hostName.
     * @return String
     */
    public String getHostName() {
        return msHostName;
    }

    /**
     * Sets the hostIP.
     * @param hostIP The hostIP to set
     */
    public void setHostIP(String hostIP) {
        msHostIP = hostIP;
    }

    /**
     * Sets the hostName.
     * @param hostName The hostName to set
     */
    public void setHostName(String hostName) {
        msHostName = hostName;
    }

    /**
     * Returns the destinationPage.
     * @return String
     */
    public String getDestinationPage() {
        return msDestinationPage;
    }

    /**
     * Sets the destinationPage.
     * @param destinationPage The destinationPage to set
     */
    public void setDestinationPage(String destinationPage) {
        msDestinationPage = destinationPage;
    }

    /**
     * @return
     */
    public String getQuery() {
        return msData;
    }

    /**
     * This function setap the connection query, it's not recomadated to be used.
     * Please use BuildQuery Instead
     * @param svQuery warning: this param should be only a result of a getQuery call,
     * 						   this string should have a special encoding implemented in BuildQuery Function 
     */
    public void setQuery(String svQuery) {
        msData = svQuery;
    }

    /**
     * @return
     */
    public static String getCookieParameter() {
        return CookieParameter;
    }

    /**
     * @return
     */
    public static String getHTML_CONTENT_TYPE() {
        return HTML_CONTENT_TYPE;
    }

    /**
     * @return
     */
    public BufferedInputStream getMbisResponse() {
        return mbisResponse;
    }

    /**
     * @return
     */
    public int getMiErrorCode() {
        return miErrorCode;
    }

    /**
     * @param stream
     */
    public void setMbisResponse(BufferedInputStream stream) {
        mbisResponse = stream;
    }

    /**
     * @param i
     */
    public void setMiErrorCode(int i) {
        miErrorCode = i;
    }

    /**
     * @return
     */
    public String getMsReferer() {
        return msReferer;
    }

    /**
     * @param string
     */
    public void setMsReferer(String string) {
        msReferer = string;
    }

    /**
     * @return
     */
    public String getMsContentType() {
        return msContentType;
    }

    /**
     * @param string
     */
    public void setMsContentType(String string) {
        msContentType = string;
    }

    /**
     * @return
     */
    public int getMiContentLength() {
        return miContentLength;
    }

    /**
     * @param i
     */
    public void setMiContentLength(int i) {
        miContentLength = i;
    }

    /**
     * @return
     */
    public HttpMethod getHmethod() {
        return hmethod;
    }

    /**
     * @param method
     */
    public void setHmethod(HttpMethod method) {
        hmethod = method;
    }

    public synchronized Object clone() {

        try {

            TSConnectionURL connectionURL = (TSConnectionURL) super.clone();

            try {
                connectionURL.msHostName = new String(msHostName);
            } catch (Exception ignored) {
            }
            try {
                connectionURL.msRHostName = new String(msRHostName);
            } catch (Exception ignored) {
            }
            try {
                connectionURL.msHostIP = new String(msHostIP);
            } catch (Exception ignored) {
            }
            try {
                connectionURL.msRHostIP = new String(msRHostIP);
            } catch (Exception ignored) {
            }
            try {
                connectionURL.msData = new String(msData);
            } catch (Exception ignored) {
            }
            try {
                connectionURL.msRData = new String(msRData);
            } catch (Exception ignored) {
            }
            try {
                connectionURL.msReferer = new String(msReferer);
            } catch (Exception ignored) {
            }
            try {
                connectionURL.msRReferer = new String(msRReferer);
            } catch (Exception ignored) {
            }
            try {
                connectionURL.msContentType = new String(msContentType);
            } catch (Exception ignored) {
            }
            try {
                connectionURL.msInternalError = new String(msInternalError);
            } catch (Exception ignored) {
            }
            try {
                connectionURL.msNewLocation = new String(msNewLocation);
            } catch (Exception ignored) {
            }
            try {
                connectionURL.msDestinationPage = new String(msDestinationPage);
            } catch (Exception ignored) {
            }
            try {
                connectionURL.msRDestinationPage = new String(
                        msRDestinationPage);
            } catch (Exception ignored) {
            }
            try {
                connectionURL.msRSpeacialHead = new String(msRSpeacialHead);
            } catch (Exception ignored) {
            }

            connectionURL.miErrorCode = miErrorCode;
            connectionURL.miContentLength = miContentLength;
            connectionURL.miRCommunicationType = miRCommunicationType;

            // se refac pentru noua conexiune
            connectionURL.urlConnToSend = null;
            connectionURL.hmethod = null;
            connectionURL.mbisResponse = null;

            // aceeasi pentru toate conexiunile
            connectionURL.cookie = cookie;
            connectionURL.cookieRefresh = cookieRefresh;

            return connectionURL;

        } catch (CloneNotSupportedException cnse) {
            throw new InternalError();
        }
    }
    
    public String toString() {
        
        StringBuffer sb = new StringBuffer();
        
        sb.append("msHostName=[" + msHostName + "]\n");
        /*sb.append("msRHostName=[" + msRHostName + "]\n");
        sb.append("msHostIP=[" + msHostIP + "]\n");
        sb.append("msRHostIP=[" + msRHostIP + "]\n");
        sb.append("msData=[" + msData + "]\n");
        sb.append("msRData=[" + msRData + "]\n");
        sb.append("msReferer=[" + msReferer + "]\n");
        sb.append("msRReferer=[" + msRReferer + "]\n");
        sb.append("msContentType=[" + msContentType + "]\n");
        sb.append("msInternalError=[" + msInternalError + "]\n");
        sb.append("msNewLocation=[" + msNewLocation + "]\n");
        sb.append("msDestinationPage=[" + msDestinationPage + "]\n");
        sb.append("msRDestinationPage=[" + msRDestinationPage + "]\n");
        sb.append("msRSpeacialHead=[" + msRSpeacialHead + "]\n");*/
        
        return sb.toString();
    }

	public URI getLastURI() {
		return lastURI;
	}

	public void setLastURI(URI lastURI) {
		this.lastURI = lastURI;
	}
	public boolean getRemoveParamValueQuotes() {
		return removeParamValueQuotes;
	}
	public void setRemoveParamValueQuotes(boolean  removeParamValueQuotes) {
		this.removeParamValueQuotes = removeParamValueQuotes;
	}
}