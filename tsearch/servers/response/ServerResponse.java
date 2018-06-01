package ro.cst.tsearch.servers.response;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.httpclient.URI;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import ro.cst.tsearch.servers.types.TSInterface;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult;

public class ServerResponse implements Serializable {

	DownloadImageResult imageResult ;
	
	static final long serialVersionUID = 10000000;
    public static final int NO_ERROR 						= -1;
    public static final int DEFAULT_ERROR 					= 0;
    public static final int ZERO_MODULE_ITERATIONS_ERROR 	= 1;
    public static final int NOT_PERFECT_MATCH_WARNING 		= 2;
    public static final int NOT_VALID_DOC_ERROR 			= 3;
    public static final int CONNECTION_IO_ERROR 			= 4;
    public static final int NOT_PERFECT_MATCH_WARNING_FIRST = 5;

    private int errorCode = NO_ERROR;
    private boolean fakeResponse = false;
    
    private String msError 	= null;
    private String msResult = null;
    private String querry 	= "";
    private boolean isInGoBackOnLevel = false;
    
    public static final int DISPLAY_ALL						= 1;
    public static final int HIDE_BACK_BUTTON				= 2;
    public static final int HIDE_BACK_TO_PARENT_SITE_BUTTON	= 3;
    public static final int HIDE_ALL_CONTROLS				= 4;
    
    private int displayMode = DISPLAY_ALL;
    private int extraRowsToParse = 0;

    private ParsedResponse mParsedResponse = new ParsedResponse();
    private URI lastURI = null;

    private BigDecimal bestScore = new BigDecimal("1.00");
    private static Map errorMessages = new HashMap();
    private transient TSInterface tsInterface = null;
    private transient long creationTime = System.currentTimeMillis();
    
    static {
        errorMessages.put(new Integer(DEFAULT_ERROR), "Default Error");
        errorMessages.put(new Integer(ZERO_MODULE_ITERATIONS_ERROR),
                        "Search not perfomed. Possible cause: not enough values in search criteria");
        
        errorMessages.put(
                        new Integer(NOT_PERFECT_MATCH_WARNING),
                        "&nbsp;&nbsp;Below are the closest matches that were found. If one of them is correct, click on it, save it to TSR and continue the current search.<br>&nbsp;Otherwise go Back to the Search Page and check the input data.");
        
        errorMessages.put(new Integer(NOT_VALID_DOC_ERROR),
                "This is not a valid document.");
    }

    /**
     * Returns the error.
     * 
     * @return boolean
     */
    public boolean isError() {
        return errorCode != NO_ERROR;
    }

    /**
     * Returns the error.
     * 
     * @return String
     */
    public String getError() {
        return msError;
    }

    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the result.
     * 
     * @return String
     */
    public String getResult() {
        return msResult;
    }

    /**
     * Returns the parsed server response.
     * 
     * @return ParsedResponse
     */
    public ParsedResponse getParsedResponse() {
        return mParsedResponse;
    }

    public void setParsedResponse(ParsedResponse pr1) {
        mParsedResponse = pr1;
    }

    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    //set
    /**
     * Sets the error. 
     * @param error The error to set
     */
    public void setError(String sError) {
        msError = sError;
        errorCode = DEFAULT_ERROR;
    }

    public void setError(String sError, int errorCode) {
        msError = sError;
        this.errorCode = errorCode;
    }

    public void setError(int errorCode) {
        Integer i = new Integer(2);
        msError = (String) errorMessages.get(new Integer(errorCode));
        this.errorCode = errorCode;
    }

    public void clearError() {
        msError = null;
        errorCode = NO_ERROR;
    }

    public void setDisplayMode(int displayMode) {
    	this.displayMode = displayMode;
    }
    public int getDisplayMode() {
    	return displayMode;
    }
    /**
     * Sets the result.
     * 
     * @param result
     *            The result to set
     */
    public void setResult(String result) {
        msResult = result;
    }

    public String toString() {
        return "ServerResponse (" + mParsedResponse + ")";
    }

    /**
     * @return
     */
    public String getQuerry() {
        String q = querry;

        try {
            q = URLDecoder.decode(querry, "UTF-8");
        } 
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return q;
    }

    /**
     * @param string
     */
    public void setQuerry(String string) {
        querry = string;
    }

    public boolean isFakeResponse() {
        return fakeResponse;
    }

    public void setFakeResponse(boolean b) {
        fakeResponse = b;
    }
    
    private boolean checkForDocType = false;
    
    
    private boolean parentSiteSearch = false;

    public boolean isParentSiteSearch() {
        return parentSiteSearch;
    }

    public void setParentSiteSearch(boolean b) {
        parentSiteSearch = b;
    }

    /**
     * @return Returns the bestScore.
     */
    public BigDecimal getBestScore() {
        return bestScore;
    }
    /**
     * @param bestScore The bestScore to set.
     */
    public void setBestScore(BigDecimal bestScore) {
        this.bestScore = bestScore;
    }

	/**
	 * @return Returns the checkForDocType.
	 */
	public boolean isCheckForDocType() {
		return checkForDocType;
	}

	/**
	 * @param checkForDocType The checkForDocType to set.
	 */
	public void setCheckForDocType(boolean checkForDocType) {
		this.checkForDocType = checkForDocType;
	}
    
    public String getRawQuerry()
    {
        return querry;
    }
    
    /**
     * factory that creates serverResponse with a warning text
     * @param text
     * @return
     */
    public static ServerResponse createWarningResponse(String text){
    	ServerResponse serverResponse = new ServerResponse();      
	    serverResponse.getParsedResponse().setWarning(text);
    	serverResponse.getParsedResponse().setOnlyResultRows(new Vector());
        return serverResponse;
    }
    
    /**
     * creates empty response
     * @return
     */
    public static ServerResponse createEmptyResponse(){
    	ServerResponse serverResponse = new ServerResponse();      
    	serverResponse.getParsedResponse().setOnlyResultRows(new Vector());
        return serverResponse;
    }
    
    /**
     * creates solved response
     * @return
     */
    public static ServerResponse createSolvedResponse(){
    	ServerResponse serverResponse = new ServerResponse();      
    	serverResponse.getParsedResponse().setSolved(true);
        return serverResponse;    	
    }
    
    /**
     * Create error response
     * @param text
     * @return
     */
    public static ServerResponse createErrorResponse(String text){
		ServerResponse sr = new ServerResponse();
		sr.getParsedResponse().setError("<font color=\"red\">" + text + "</font>");
		sr.setError("<font color=\"red\">" + text + "</font>");
		return sr;
    }
    
    /**
     * Create error response
     * @param text
     * @return
     */
    public static ServerResponse createErrorResponseWithEmptyResult(String text){
		ServerResponse sr = createErrorResponse(text);
		sr.setResult("");
		return sr;
    }

	public boolean getInGoBackOnLevel() {
		return isInGoBackOnLevel;
	}

	public void setInGoBackOnLevel(boolean isInGoBackOnLevel) {
		this.isInGoBackOnLevel = isInGoBackOnLevel;
	}
	
	private String warning = "";
	
	public void setWarning(String warning){ 
		this.warning = warning; 
	}
	
	public String getWarning(){
		return warning;
	}
		
	private boolean filtered = false;

	public void setFiltered(boolean filtered) {
		this.filtered = filtered;
	}

	public boolean isFiltered() {
		return filtered;
	}

	public int getExtraRowsToParse() {
		return extraRowsToParse;
	}

	public void setExtraRowsToParse(int extraRowsToParse) {
		this.extraRowsToParse = extraRowsToParse;
	}
	
	public String getCrossRefSourceType(){
		String query = getQuerry();
		String type = "";
		int pos = query.indexOf("&crossRefSource=");
		if(pos>0)
			type = query.substring(pos + "&crossRefSource=".length());
		else		//we validate if we have no infe
			return "";	
		pos = type.indexOf("&");
		if(pos>0)
			type = type.substring(0,pos);
		return type;
	}
	
	public DownloadImageResult getImageResult() {
		return imageResult;
	}

	public void setImageResult(DownloadImageResult imageResult) {
		this.imageResult = imageResult;
	}
	
	private HtmlPage page;

	public HtmlPage getPage() {
		return page;
	}

	public void setPage(HtmlPage page) {
		this.page = page;
	}

	public TSInterface getTsInterface() {
		return tsInterface;
	}

	public void setTsInterface(TSInterface tsInterface) {
		this.tsInterface = tsInterface;
	}

	/**
	 * Gets the last URI followed to get this page (even after redirect)
	 * @return the last URI followed
	 */
	public URI getLastURI() {
		return lastURI;
	}

	public void setLastURI(URI lastURI) {
		this.lastURI = lastURI;
	}
	
	public long getCreationTime() {
		return creationTime;
	}
	
}