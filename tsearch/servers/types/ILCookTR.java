package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_TEXT_FIELD;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setHiddenParamLoggableMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setJustifyFieldMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setRequiredCriticalMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.ORIENTATION_HORIZONTAL;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.PIXELS;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPManager;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSite;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.pdftiff.util.Util;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

public class ILCookTR extends TSServer {

	public static final long serialVersionUID = 10000000L;
	
	private boolean downloadingForSave; 

	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	private static final Pattern pinPattern = Pattern.compile("(?is)ctl00_ContentPlaceHolder1_PaymentResultsControl1_lblPIN[^>]+[^0-9]+([0-9][0-9-]+[0-9])[^0-9]");																				  
//	private static final Pattern imageLinkPattern = Pattern.compile("(?i)<span id=\"_ctl0_ContentPlaceHolder1_PaymentSearchControl1_PaymentSearchBoxControl1_securePicture\"><IMG border=1 src=JpegImage\\.aspx\\?guid=([A-Z]+)\\s*/></span>");
	private static final Pattern viewStatePattern = Pattern.compile("(?is)<input type=\"hidden\" name=\"__VIEWSTATE\" (?:id=\"__VIEWSTATE\" )?value=\"([^\"]+)\"");
	private static final Pattern eventValidationPattern = Pattern.compile("(?is)<input type=\"hidden\" name=\"__EVENTVALIDATION\" (?:id=\"__EVENTVALIDATION\" )?value=\"([^\"]+)\"");
	
	private static final String MAINTENANCE_MESSAGE = "maintenanceMessage";
	
	private static final int PIN_MODULE_IDX = 1;
	
	private static final int ID_SEARCH   = 102;
	
	
	public ILCookTR(long searchId) {
		super(searchId);
	}
	
	public ILCookTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo si = new TSServerInfo(1);
		si.setServerAddress("www.cookcountytreasurer.com");
		si.setServerIP("www.cookcountytreasurer.com");
		si.setServerLink("http://www.cookcountytreasurer.com");
		
		Credentials credentials = getImageCredentials();
		String imglink = "";
		String viewState = "";
		String eventValidation = "";
		String prevPage = "";
		String securityCode = "";
				
		if (credentials!= null) {
			imglink = "<img border='1' src='/title-search/fs?f=" + credentials.fileName + "&searchId=" + searchId + "'/>"; 
			viewState = credentials.viewState;
			eventValidation = credentials.eventValidation; 
			prevPage = credentials.prevPage;
			securityCode = credentials.securityCode;
		} else {
			Object messageObj = getSearch().getAdditionalInfo(MAINTENANCE_MESSAGE);
			if (messageObj != null) {
				String message = messageObj.toString();
				if (!message.isEmpty()) {
					imglink = "<span style=\"color:red;width:80px;\"> <b> " + message + "</b></span>";
				}
			} else {
				imglink = "<span style=\"color:red;width:30px;\"> <b> Image unavailable! </b> </span>"; 
			}
		}
		
		
		// PIN Search
		{
			TSServerInfoModule 		
			sim = si.ActivateModule(PIN_MODULE_IDX, 16);
			sim.setName("PIN");
			sim.setDestinationPage("/payment.aspx?ntopicid=3");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(ID_SEARCH);
			sim.setSearchType(DocumentI.SearchType.PN.toString());

			PageZone pz = new PageZone("SearchByPIN", "PIN Property Search", ORIENTATION_HORIZONTAL, null, 100, 50, PIXELS , true);

			try{				
	            HTMLControl 
	            pin1  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1,18, sim.getFunction(0), "ctl00$ContentPlaceHolder1$PaymentSearchBox1$PIN1", "PIN", null, searchId),
	            pin2  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1,18, sim.getFunction(1), "ctl00$ContentPlaceHolder1$PaymentSearchBox1$PIN2", "PIN2", null, searchId),
	            pin3  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1,18, sim.getFunction(2), "ctl00$ContentPlaceHolder1$PaymentSearchBox1$PIN3", "PIN3", null, searchId),
	            pin4  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1,18, sim.getFunction(3), "ctl00$ContentPlaceHolder1$PaymentSearchBox1$PIN4", "PIN4", null, searchId),
	            pin5  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1,18, sim.getFunction(4), "ctl00$ContentPlaceHolder1$PaymentSearchBox1$PIN5", "PIN5", null, searchId),
	            
	            text  = new HTMLControl(HTML_TEXT_FIELD, 2, 2,  2,  2,18, sim.getFunction(5), "ctl00$ContentPlaceHolder1$PaymentSearchBox1$EnteredSecurityCode", "Enter Text From Image", "", searchId),	            
        	    image = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  2,  2,18, sim.getFunction(6), "xyztuy", "Image", null, searchId);
	            
	            HTMLControl 
	            	hid1 = HTMLControl.createHiddenHTMLControl(sim.getFunction(7), "__EVENTTARGET", "", "", searchId),
	            	hid2 = HTMLControl.createHiddenHTMLControl(sim.getFunction(8), "__EVENTARGUMENT", "", "", searchId),
	            	hid3 = HTMLControl.createHiddenHTMLControl(sim.getFunction(9), "__VIEWSTATE", "", viewState, searchId),
	            	hid4 = HTMLControl.createHiddenHTMLControl(sim.getFunction(10), "ctl00$ContentPlaceHolder1$PaymentSearchBox1$cmdSearch", "", "Search", searchId),
	            	hid5 = HTMLControl.createHiddenHTMLControl(sim.getFunction(11), "ctl00$ContentPlaceHolder1$HF_BankType", "", "1", searchId),
	            	hid6 = HTMLControl.createHiddenHTMLControl(sim.getFunction(12), "ctl00$ContentPlaceHolder1$PaymentSearchBox1$GeneratedSecurityCode", "", securityCode, searchId),
	            	hid7 = HTMLControl.createHiddenHTMLControl(sim.getFunction(13), "__PREVIOUSPAGE", "", prevPage, searchId),
	            	hid8 = HTMLControl.createHiddenHTMLControl(sim.getFunction(14), "__EVENTVALIDATION", "", eventValidation, searchId),
	            	hid9 = HTMLControl.createHiddenHTMLControl(sim.getFunction(15), "__LASTFOCUS", "", "", searchId);
	            		            
	            sim.getFunction(6).setHtmlformat(imglink);
	           
	            pin1.setFieldNote("(e.g. 32291030140000)");
	            
	            setRequiredCriticalMulti(true, pin1, text);
 	            setHiddenParamLoggableMulti(true, pin2, pin3, pin4, pin5);
            	setJustifyFieldMulti(false, pin1, text, image);
	            
	            pz.addHTMLObjectMulti(pin1, pin2, pin3, pin4, pin5, text, image);
	            pz.addHTMLObjectMulti(hid1, hid2, hid3, hid4, hid5, hid6, hid7, hid8, hid9);    
	            
	            sim.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);
	            
			}catch(FormatException e){
				e.printStackTrace();
			}
			
			sim.setModuleParentSiteLayout(pz);
		}
		
		si.setupParameterAliases();
		setModulesForAutoSearch(si);
		
		return si;	

	}
	/**
	 * Immutable class for storing credentials
	 * @author Dumitru Bacrau
	 */
	private static class Credentials{
		
		public String securityCode;
		public String imageId;
		public String fileName;
		public String viewState;
		public String eventValidation;
		public String prevPage;
		
		public Credentials(String imageId, String fileName, String viewState, String eventValidation, String prevPage, String securityCode){
			this.imageId = imageId;
			this.fileName = fileName;
			this.viewState = viewState;
			this.eventValidation = eventValidation;
			this.prevPage = prevPage;
			this.securityCode = securityCode;
		}
		
		public String toString(){
			return "Credentials(imageId=" + imageId +",fileName=" + fileName + ",viewState=" + viewState + ",eventValiadtion=" + eventValidation + ",prevPage=" + prevPage + ",securityCode=" + securityCode + ")";
		}
		
	}
	
	/**
	 * Obtain the credentials by trying ma 5 times
	 * @return null if failed
	 */ 
	private Credentials getImageCredentials(){
		getSearch().removeAdditionalInfo(MAINTENANCE_MESSAGE);
		
		Credentials cr = (Credentials) getSearch().getAdditionalInfo("ILCookTR-credentials");
		for(int i=0; i<5 && cr==null; i++){
			cr = createImageCredentials();
			if (getSearch().getAdditionalInfo(MAINTENANCE_MESSAGE) != null && cr == null) {
				break;
			}
		}
		if(cr != null){
			getSearch().setAdditionalInfo("ILCookTR-credentials", cr);
		}
		return cr;
	}
	
	/**
	 * Try to obtain credentials once
	 * @return null if failed
	 */
	private Credentials createImageCredentials(){

		// get the http site
        HTTPSiteInterface httpSite = getSearchHttpSite();
        if(httpSite == null){
        	logger.error("Cannot obtain httpSite!");
        	return null;
        }        
		
        // retrieve the search page in order to obtain the viewState and image
        String link = "http://www.cookcountytreasurer.com/payment.aspx?ntopicid=3";
		String response = null;
		for(int i=0; i<2; i++){
			try{
				response = httpSite.process(new HTTPRequest(link)).getResponseAsString();
				break;
			}catch(RuntimeException e){
				logger.error(e);
			}
		}
		if(response == null){			
			logger.error("Cannot obtain search page!");
			return null;
		}
		
		// extract image Link
		/*
		Matcher imageLinkMatcher = imageLinkPattern.matcher(response);
		if(!imageLinkMatcher.find()){
			logger.error("Cannot obtain image link!");
			return null;
		}
		String imageId = imageLinkMatcher.group(1);
		*/
		
		String imageId = StringUtils.extractParameter(response, "(?i)<img[^>]*?src='securityimage\\.aspx\\?securitycode=([^ '/>]*)");
		if(StringUtils.isEmpty(imageId)){
			
			try {
				org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(response, null);
				NodeList imageList = parser
					.extractAllNodesThatMatch(new TagNameFilter("span"))
					.extractAllNodesThatMatch(new HasAttributeFilter("id","ContentPlaceHolder1_PaymentSearchAgainBox1_SecurityCodeImage"))
					.extractAllNodesThatMatch(new TagNameFilter("img"),true);
				if(imageList.size() > 0) {
					ImageTag image = (ImageTag)imageList.elementAt(0);
					String imageScr = image.getImageURL();
					imageId = StringUtils.extractParameter(imageScr, "(?i)securitycode=([^ />]*)");
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (getSearch().getAdditionalInfo(MAINTENANCE_MESSAGE) == null) {
				String maintenanceMessage = StringUtils
						.extractParameter(response, "(The\\s+Cook\\s+County\\s+Treasurer\\s+Online\\s+Payment\\s+Service\\s+is\\s+currently"
								+ "\\s+unavailable\\s+due\\s+to\\s+scheduled\\s+maintenance.)");
				if (!maintenanceMessage.isEmpty()) {
					getSearch().setAdditionalInfo("maintenanceMessage", "The site is currently unavailable due to scheduled maintenance.");
				}
			}

			if(StringUtils.isEmpty(imageId)) {
				return null;
			}
		}
		String securityCode = StringUtils.extractParameter(response, "_GeneratedSecurityCode\" value=\"([^\"]*)\"");			
		// extract viewState		
		Matcher viewStateMatcher = viewStatePattern.matcher(response);
		if(!viewStateMatcher.find()){
			logger.error("Cannot obtain viewState!");
			return null;
		}
		String viewState = viewStateMatcher.group(1);		
		String prevPage = StringUtils.extractParameter(response, "(?i)<input type=\"hidden\" name=\"__PREVIOUSPAGE\" id=\"__PREVIOUSPAGE\" value=\"([^\"]*)\"");

		// extract eventValidation		
		Matcher eventValidationMatcher = eventValidationPattern.matcher(response);
		if(!eventValidationMatcher.find()){
			logger.error("Cannot obtain eventValidation!");
			return null;
		}
		String eventValidation = eventValidationMatcher.group(1);
		
		// download image 
		// make sure we have the folder: B3147
		String folderName = getCrtSearchDir() + "temp";
		new File(folderName).mkdirs();
    	String fileName = folderName + File.separator + securityCode.replaceAll("[^\\w]", "_") + ".jpg";
    	String imageLink = "http://www.cookcountytreasurer.com/securityimage.aspx?securitycode=" + imageId;
    	
    	HTTPResponse httpResponse = httpSite.process(new HTTPRequest(imageLink));
    	if(!httpResponse.getContentType().contains("image/")){
    		logger.error("Did not obtain \"image/\");");
    		return null;
    	}
    		    
    	InputStream inputStream = httpResponse.getResponseAsStream();
    	FileUtils.writeStreamToFile(inputStream, fileName);	
    	
    	if(!FileUtils.existPath(fileName)){
    		logger.error("Image was not downloaded!");
    		return null;
    	}
    	
		// create relative fileName
    	if(fileName.startsWith(ServerConfig.getFilePath())){
			fileName = fileName.substring(ServerConfig.getFilePath().length());
		}
		if(fileName.contains("\\")){
			fileName = fileName.replace('\\', '/');
		}
		
				
		return new Credentials (imageId, fileName, viewState, eventValidation, prevPage, securityCode);
	}
	
	@Override
    public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException
    {
		// expand pin
		String [] pins = ILCookAO.extractPins(module.getFunction(0).getParamValue().trim());
		if(pins == null){
			return ServerResponse.createErrorResponse("Invalid PIN!");
		}
        for(int i=0; i<5; i++){
        	module.getFunction(i).setParamValue(pins[i]);
        } 
        
        // remove image function
        module.removeFunction(6);

        // perform usual search, but always discard the current credentials
        Credentials cr = (Credentials)getSearch().getAdditionalInfo("ILCookTR-credentials");
        if(cr == null && getSearch().getSearchType() == Search.PARENT_SITE_SEARCH)
        {//let the search continue in case it is an Automatic Search
        	return ServerResponse.createErrorResponse("Authentication Image Missing");
        }
        try{
        	// remove credentials
        	if (cr != null)
        	{
        		getSearch().removeAdditionalInfo("ILCookTR-credentials");
        	}
        	// perform search
        	return super.SearchBy(module, sd);
        } finally {
        	if (cr != null)
        	{// delete credentials file
        		new File(ServerConfig.getFilePath() + cr.fileName).delete();
        	}
        }
    }
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		boolean emptyPid = "".equals( sa.getAtribute( SearchAttributes.LD_PARCELNO ) );

		if( !emptyPid )
		{//Search by multiple PINs (Parcel Numbers)
			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
			
			pin = pin.replaceAll("\\s+","");
			pin = pin.replaceFirst("^,", "");
			pin = pin.replaceFirst(",$", "");			
			String[] pins = pin.split(",");
			int length = pins.length;
			if(length > 5)
			{// allocated support for multiple PIN search (using maximum 5 PINs) - see getDefaultServerInfo()
				length = 5;
			}			

			for(int i=0; i<length; i++)
			{
				TSServerInfoModule m = new TSServerInfoModule(serverInfo.getModule(PIN_MODULE_IDX));
				m.clearSaKeys();
				m.getFunction(0).forceValue(pins[i]);
				l.add(m);
			}
		}
		
		serverInfo.setModulesForAutoSearch(l);		
	}
	
	private String getContents(String response){
		// return if doc is form memory
		if(response.toLowerCase().indexOf("<html ") < 0) {
			return response;
		}
			
		// determine PIN
		String pin = StringUtils.extractParameter(response, "<span id=\"ContentPlaceHolder1_lblPIN\" class=\"blacktextbold\">([^<]*)</span>");
		if(StringUtils.isEmpty(pin)){
			return null;
		}
		
		String contents = "";
		String taxInfoPrevYear = "";
		
		int offs = response.indexOf("<div style=\"margin-top: 4px;\">");
		if(offs != -1){
			String info = StringUtils.extractTagContents(offs, response, "div");
			if(info != null){
				//info = info.replaceFirst("(?i)<table", "<table align=\"center\"");
				contents += info;
			}
		}
		
		offs = response.indexOf("<div id=\"ContentPlaceHolder1_panPriorTaxYearOnline\">");
		if(offs != -1) {
			taxInfoPrevYear = StringUtils.extractTagContents(offs, response, "div");
			taxInfoPrevYear = taxInfoPrevYear.replaceFirst("(?is)<div[^>]*>\\s*<input[^>]+>\\s*</div>\\s*(?:<div[^>]*>\\s*</div>)?", "");
			taxInfoPrevYear = taxInfoPrevYear.replaceFirst("(?is)<div style\\s*=\\s*\\\"float:\\s*left;\\\">","<div id=\"prevTaxInfo\">");
		}
//		offs = response.indexOf("<div style=\"border: 1px solid #000099; padding: 2px\">", offs + 1);
//		if(offs != -1){
//			String info = StringUtils.extractTagContents(offs, response, "div");
//			if(info != null){
//				//info = info.replaceFirst("(?i)<table", "<table align=\"center\"");
//				contents += info;
//			}
//		}
		
		if(response.contains("ContentPlaceHolder1_panCurrentTaxYearOnline") || "".equals(contents)) {
			try {
				// get the details by "pressing" the "Click Here" button
				DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
				String siteLink = dataSite.getLink() + "paymentresults.aspx?paymenttype=current";
				
				HTTPRequest reqP = new HTTPRequest(siteLink);
		    	reqP.setMethod(HTTPRequest.POST);
		    	
		    	org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(response, null);
				NodeList nodeList = parser.parse(null);
				NodeList inputs = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("type", "hidden"), false);
				
		    	for(int i = 0; i < inputs.size(); i++) {
		    		InputTag input = (InputTag)inputs.elementAt(i);
		    		String id = input.getAttribute("name");
		    		String value = input.getAttribute("value") == null ? "" : input.getAttribute("value");
		    		reqP.setPostParameter(id, value);
		    	}
		    	
		    	String detailsPage = "";
		    	HTTPSite site = HTTPManager.getSite(getCurrentServerName(), searchId, miServerID);
	        	try {
	        		HTTPResponse resP = site.process(reqP);
	        		detailsPage = resP.getResponseAsString();
	        	} finally {
	        		HTTPManager.releaseSite(site);
				}

	        	if(!"".equals(detailsPage)) {
	        		parser.setInputHTML(detailsPage);
	        		nodeList = parser.parse(null); 
	        		nodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ContentPlaceHolder1_panOnline"), true);
	        				
	        		if(nodeList.size()>0){		
//	        			nodeList = nodeList.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("div"))
//	        					.extractAllNodesThatMatch(new HasAttributeFilter("style","margin-top: 4px;"));
//	        			
//	        			if(nodeList!=null)
	        				contents = nodeList.toHtml();
	        				contents += "<span style=\"color:darkblue; font-weight: bold; font-size: 15px;\"> Tax information of previous tax year: </span>" + taxInfoPrevYear;
	        		}
	        	}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		// cleanup
		contents = contents.replaceAll("<input type=\"submit\"[^>]*>","");
		contents = contents.replaceAll("(?is)</?img[^>]*>","");
		contents = contents.replaceAll("(?is) onclick=\"[^\"]*\"","");
		contents = contents.replaceAll("(?i)<a(\\s|\n)*", "<a ");
		contents = contents.replaceAll("(?is)<a[^>]*>","");
		contents = contents.replaceAll("(?is)</a>","");
		//contents = contents.replaceAll("(?is)color:[ a-zA-Z]+;","");
		contents = contents.replaceAll("(?is)border-collapse:[^;]+;","");
		contents = contents.replace("Cook County Property Tax and Payment Information",
				"<div><p id=\"fakeHeader\" align=\"center\" width=100%><font style=\"font-size:xx-large;\"><b>Cook County Property Tax and Payment Information</b></font></p> <br><br></div>");
		contents = contents.replaceFirst("(?is)<span[^<]+id=\"ContentPlaceHolder1_lblPIN\"[^<]*</span>","<span id=\"ctl00_ContentPlaceHolder1_PaymentResultsControl1_lblPIN\"><b>" + pin + "</b></span>");
		contents = contents.replaceFirst("(?is)The Cook County Clerk's office can help you with redemption.*?http://www.cookcountyclerk.com","");
		
		return contents;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {	
		String response = Response.getResult();
		String contents = null;
		
		switch(viParseID){
		
		case ID_DETAILS:
		case ID_SEARCH:
			
			// security code error
			if(response.matches("(?is).*Security\\s*Code\\s*was\\s*NOT\\s*valid.*")){
				Response.getParsedResponse().setError("<font color=\"red\">Security Code was NOT valid.</font> Please try again.");
				return;
			}
			
			// pin not found
			if(response.matches("(?is).*No\\s*property\\s*was\\s*found\\s*under\\s*this\\s*PIN.*")){
				Response.getParsedResponse().setError("<font color=\"red\">No results found under this PIN.</font>  Please search again. <br>" +
						"Please Note: Newly divided PINs for which there is no first installment bill will NOT appear on the payment list.");
				return;
			}
			
			// extract contents
			contents = getContents(response);
						
			if(contents == null){
				Response.getParsedResponse().setError("<font color=\"red\">Error retrieving page</font>");
				return;
			}
			contents = "<table border=\"0\" align=\"center\" cellspacing=\"0\" cellpadding=\"0\"><tr><td>" +					  
			           contents +
			           "</td></tr></table>";
			Response.getParsedResponse().setResponse(contents);
			
			// isolate pin number
			String keyCode = "File";
			Matcher pinMatcher = pinPattern.matcher(contents);
			if(pinMatcher.find())
			{
				keyCode = pinMatcher.group(1);
				//keyCode = keyCode.replaceAll("-","");
			} 
			
			if ((!downloadingForSave))
			{

                String originalLink = sAction + "&dummy=" + keyCode + "&" + "shortened=true"; //Response.getQuerry();
                String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
                
                HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","CNTYTAX");
				data.put("dataSource", "TR");
				if(isInstrumentSaved(keyCode, null, data)){
					contents += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, contents);
					contents = addSaveToTsdButton(contents, sSave2TSDLink, viParseID);
	            }

                Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
                Response.getParsedResponse().setResponse(contents);
                
            } 
			else 
            {            	
                msSaveToTSDFileName = keyCode + ".html";
                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                msSaveToTSDResponce = contents + CreateFileAlreadyInTSD();                
                smartParseDetails(Response,contents);
			}
			
			break;
		
		case ID_SAVE_TO_TSD:
			
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;				
			break;		
		}

	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
		try {
			HtmlParser3 htmlParser3 = new HtmlParser3(detailsHtml);
			NodeList nodeList = htmlParser3.getNodeList();
			NodeList spans = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true);
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
					"ctl00_ContentPlaceHolder1_PaymentResultsControl1_lblPIN"));
			String tempInfo = null;
			if(nodeList.size() > 0) {
				map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), nodeList.elementAt(0).toPlainTextString());
			}
			
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
					"ContentPlaceHolder1_PaymentTaxYearInformation1_lblTaxYear"));
			if(nodeList.size() > 0) {
				map.put(TaxHistorySetKey.YEAR.getKeyName(), nodeList.elementAt(0).toPlainTextString());
			}
			
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
					"ContentPlaceHolder1_PaymentTaxYearInformation1_lblVolume"));
			if(nodeList.size() > 0) {
				map.put(TaxHistorySetKey.TAX_VOLUME.getKeyName(), nodeList.elementAt(0).toPlainTextString());
			}
			
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
				"ContentPlaceHolder1_PaymentTaxYearInformation1_lblPropertyAddress"));
			if(nodeList.size() > 0) {
				tempInfo = nodeList.elementAt(0).toPlainTextString();
				map.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(tempInfo));
				map.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(tempInfo));
			}
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
				"ContentPlaceHolder1_PaymentTaxYearInformation1_lblPropertyCityStateZipCode"));
			if(nodeList.size() > 0) {
				tempInfo = nodeList.elementAt(0).toPlainTextString();
				map.put(PropertyIdentificationSetKey.CITY.getKeyName(), tempInfo.replaceAll("(.*),.*", "$1"));
				map.put(PropertyIdentificationSetKey.STATE.getKeyName(), tempInfo.replaceAll(".*,\\s*(\\w\\w)\\b.*", "$1"));
				map.put(PropertyIdentificationSetKey.ZIP.getKeyName(), tempInfo.replaceAll(".*?([\\d-]+)$", "$1"));
			}
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
					"ContentPlaceHolder1_PaymentTaxYearInformation1_lblMailingName"));
			if(nodeList.size() > 0) {
				tempInfo = nodeList.elementAt(0).toPlainTextString().trim();
				map.put("tmpOwnerFullName", tempInfo);
				map.put(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName(), tempInfo);
			}
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
					"ContentPlaceHolder1_PaymentTaxYearInformation1_lblMailingAddress"));
			if(nodeList.size() > 0) {
				map.put("tmpMailingAddrLine1", nodeList.elementAt(0).toPlainTextString().trim());
			}
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
					"ContentPlaceHolder1_PaymentTaxYearInformation1_lblMailingCityStateZipCode"));
			if(nodeList.size() > 0) {
				map.put("tmpMailingAddrLine2", nodeList.elementAt(0).toPlainTextString().trim());
			}
			
			
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
					"ContentPlaceHolder1_PaymentTaxYearInformation1_lblTaxValue1"));
			if(nodeList.size() > 0) {
				map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), nodeList.elementAt(0).toPlainTextString().replaceAll("[$,]", ""));
			} else {
				NodeList installment1 = spans.extractAllNodesThatMatch(new HasAttributeFilter("id", 
						"ContentPlaceHolder1_PaymentTaxYearInformation1_lblTaxAmountBilled1"), true);
				NodeList installment2 = spans.extractAllNodesThatMatch(new HasAttributeFilter("id", 
						"ContentPlaceHolder1_PaymentTaxYearInformation1_lblTaxAmountBilled2"), true);
				String instValue1 = "";
				String instValue2 = "";
				if (installment1.size() > 0) {
					instValue1 = installment1.elementAt(0).toPlainTextString().replaceAll("[$,]", "");
					map.put("tmp1stTaxBillValue", instValue1);
				}
				if (installment2.size() > 0) {
					instValue2 = installment2.elementAt(0).toPlainTextString().replaceAll("[$,]", "");
					map.put("tmp2ndtTaxBillValue", instValue2);
				}
				if (!instValue1.isEmpty() || !instValue2.isEmpty()) {
					map.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), GenericFunctions.sum(instValue1 + "+" + instValue2, searchId));
				}
			}
			
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
					"ContentPlaceHolder1_PaymentTaxYearInformation1_lblTaxDueDate1"));
			if(nodeList.size() > 0) {
				map.put("tmp1stTaxDueDate", nodeList.elementAt(0).toPlainTextString());
			}
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
					"ContentPlaceHolder1_PaymentTaxYearInformation1_lblTaxDueDate2"));
			if(nodeList.size() > 0) {
				map.put("tmp2ndTaxDueDate", nodeList.elementAt(0).toPlainTextString());
			}
			
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
					"ContentPlaceHolder1_PaymentTaxYearInformation1_lblAmountReceived1"));
			if(nodeList.size() > 0) {
				map.put("tmp1stAmtPaid", nodeList.elementAt(0).toPlainTextString().replaceAll("[$,]", ""));
			}
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
					"ContentPlaceHolder1_PaymentTaxYearInformation1_lblDateReceived1"));
			if(nodeList.size() > 0) {
				map.put("tmp1stTaxPaidDate", nodeList.elementAt(0).toPlainTextString());
			}
			
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
					"ContentPlaceHolder1_PaymentTaxYearInformation1_lblAmountReceived2"));
			if(nodeList.size() > 0) {
				map.put("tmp2ndAmtPaid", nodeList.elementAt(0).toPlainTextString().replaceAll("[$,]", ""));
			}
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
					"ContentPlaceHolder1_PaymentTaxYearInformation1_lblDateReceived2"));
			if(nodeList.size() > 0) {
				map.put("tmp2ndTaxPaidDate", nodeList.elementAt(0).toPlainTextString());
			}
			
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
					"ContentPlaceHolder1_PaymentTaxYearInformation1_lblTotalTaxDue"));
			if(nodeList.size() > 0) {
				map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), nodeList.elementAt(0).toPlainTextString().replaceAll("[$,]", ""));
				map.put(TaxHistorySetKey.CURRENT_YEAR_DUE.getKeyName(), nodeList.elementAt(0).toPlainTextString().replaceAll("[$,]", ""));
			} else {
				nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
						"ContentPlaceHolder1_PaymentTaxYearInformation1_lblBalanceDue"));
				if(nodeList.size() > 0) {
					map.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), nodeList.elementAt(0).toPlainTextString().replaceAll("[$,]", ""));
				}
				
				nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
						"ContentPlaceHolder1_PaymentTaxYearInformation1_lblTotalAmount1"));
				if(nodeList.size() > 0) {
					map.put("tmp1stTotalDue", nodeList.elementAt(0).toPlainTextString().replaceAll("[$,]", ""));
				}
				nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
						"ContentPlaceHolder1_PaymentTaxYearInformation1_lblTotalAmount2"));
				if(nodeList.size() > 0) {
					map.put("tmp2ndTotalDue", nodeList.elementAt(0).toPlainTextString().replaceAll("[$,]", ""));
				}
			}
			
			nodeList = spans.extractAllNodesThatMatch(new HasAttributeFilter("id",
					"ContentPlaceHolder1_lblPriorTaxYearAmount"));
			if(nodeList.size() > 0) {
				map.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), nodeList.elementAt(0).toPlainTextString().replaceAll("[^\\d.]",""));
			}
					
			ro.cst.tsearch.servers.functions.ILCookTR.stdMPisILCookTR(map, searchId);
			ro.cst.tsearch.servers.functions.ILCookTR.partyNamesILCookTR(map, searchId);
			ro.cst.tsearch.servers.functions.ILCookTR.taxILCookTR(map, searchId);

		} catch (Exception e) {
			logger.error(getSearch().getID() + ": Error while parsing ILCookAO detail page ", e);
		}
		map.removeTempDef();
		return null;
	}
	
    /**
     * get file name from link
     */
	@Override
	protected String getFileNameFromLink(String link)
	{
		String fileName = "File.html";
		Matcher dummyMatcher = dummyPattern.matcher(link);
		if(dummyMatcher.find())
		{
			fileName = dummyMatcher.group(1);
			fileName = fileName + ".html";
		}
		
        return fileName;
    }
	
	@Override
	public void addDocumentAdditionalProcessing(DocumentI doc, ServerResponse response) {
		try {
			if(doc.is(DType.TAX) && !doc.hasImage()) {
				Util.uploadImageToDocument(doc,mSearch,searchId);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}


}
