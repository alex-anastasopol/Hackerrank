/**
 * 
 */
package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_TEXT_FIELD;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setHiddenParamMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setJustifyFieldMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setRequiredCriticalMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.ORIENTATION_HORIZONTAL;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.PIXELS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.MortgageI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.property.PinI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * @author radu bacrau
 */
public class ILCookTU extends TSServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private boolean downloadingForSave = false;
	
	private static final int PIN_MODULE_IDX = 10;
	private static final int ID_SEARCH = 100;
	
	
	/**
	 * @param searchId
	 */
	public ILCookTU(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 * @param miServerID
	 */
	public ILCookTU(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
		setRepeatDataSource(true);
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo si = new TSServerInfo(1);
		si.setServerAddress("www.taxesunlimitedonline.com");
		si.setServerIP("www.taxesunlimitedonline.com");
		si.setServerLink("http://www.taxesunlimitedonline.com");
		
		// get order number
		String order = getSearchAttribute(SearchAttributes.ABSTRACTOR_FILENO); // e.g. AFWILG~543125
		order = order.replaceFirst("[^~]*~", "").replaceAll("\\s+", "");
		if(order.length() < 4){
			order = "" + System.nanoTime();
			order = order.substring(order.length() - 8);
		}

		// PIN Search
		{
			TSServerInfoModule 		
			sim = si.ActivateModule(PIN_MODULE_IDX, 13);
			sim.setName("PIN");
			sim.setDestinationPage("/result.asp");
			sim.setRequestMethod(TSConnectionURL.idPOST);
			sim.setParserID(ID_SEARCH);
			sim.setSearchType("PN");

			PageZone pz = new PageZone("SearchByPIN", "PIN Tax Search", ORIENTATION_HORIZONTAL, null, 500, 50, PIXELS , true);

			try{				
	            HTMLControl 
	            ord   = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1,18, sim.getFunction(0), "ordno", "Order", order, searchId),
	            pin1  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  2,  2,18, sim.getFunction(1), "tx1", "PIN 1", null, searchId),
	            pin2  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  3,  3,18, sim.getFunction(2), "tx2", "PIN 2", null, searchId),
	            pin3  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  4,  4,18, sim.getFunction(3), "tx3", "PIN 3", null, searchId),
	            pin4  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  5,  5,18, sim.getFunction(4), "tx4", "PIN 4", null, searchId),
	            pin5  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  6,  6,18, sim.getFunction(5), "tx5", "PIN 5", null, searchId),
	            pin6  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  7,  7,18, sim.getFunction(6), "tx6", "PIN 6", null, searchId),
	            pin7  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  8,  8,18, sim.getFunction(7), "tx7", "PIN 7", null, searchId),
	            pin8  = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  9,  9,18, sim.getFunction(8), "tx8", "PIN 8", null, searchId),
	            pin9  = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 10, 10,18, sim.getFunction(9), "tx9", "PIN 9", null, searchId),
	            pin10 = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 11, 11,18, sim.getFunction(10),"tx10","PIN 10",null, searchId),
	            nPins = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 1, sim.getFunction(11),"NumOfPins", "", null, searchId),
	            sbmit = new HTMLControl(HTML_TEXT_FIELD, 1, 1,  1,  1, 1, sim.getFunction(12),"Submit","", "Perform Search", searchId);
	            
	            setHiddenParamMulti(true, nPins, sbmit);
	            setRequiredCriticalMulti(true, ord, pin1);	
	            ord.setFieldNote("(digits after ~ from file number; random if empty file number)");
	            pin1.setFieldNote("(e.g. 32-29-103-014-0000 or 32291030140000)");
	            
	            setJustifyFieldMulti(false, ord, pin1, pin2, pin3, pin4, pin5, pin6, pin7, pin8, pin9, pin10);	            
	            pz.addHTMLObjectMulti(ord, pin1, pin2, pin3, pin4, pin5, pin6, pin7, pin8, pin9, pin10);	            
	            sim.getFunction(1).setSaKey(SearchAttributes.LD_PARCELNO);
	            
			}catch(FormatException e){
				e.printStackTrace();
			}
			
			sim.setModuleParentSiteLayout(pz);
		}
		
		si.setupParameterAliases();
		setModulesForAutoSearch(si);
		
		return si;	

	}
	
	@Override
    public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		int numOfPins = 0;
		String errors = "";
		Pattern pidPattern = Pattern.compile("(\\d{2})-?(\\d{2})-?(\\d{3})-?(\\d{3})(?:-?(\\d{4}))?");
		StringBuilder pins = new StringBuilder();
		
		String pinsAS = "";
		List<String> pinsASList = new ArrayList<String>();
		int numOfNewPins = 0;
		
		if(mSearch.getSearchType() != Search.PARENT_SITE_SEARCH){
			pinsAS = (String) mSearch.getAdditionalInfo("ALREADY_SEARCHED_PINS_TU");
			if (pinsAS == null){
				pinsAS = "";
			} else {
				pinsASList.addAll(Arrays.asList(pinsAS.split(",")));
			}
		}
        for(int i=1; i<=10; i++){
        	TSServerInfoFunction function = module.getFunction(i);
        	String val = function.getParamValue();
        	if(StringUtils.isEmpty(val)){
        		continue;
        	}
        	Matcher matcher = pidPattern.matcher(val);
        	if(matcher.matches()){
        		String pin = matcher.group(1) + "-" +
        		 			 matcher.group(2) + "-" +
        		 			 matcher.group(3) + "-" +
        		 			 matcher.group(4) + "-";
        		if(matcher.group(5) == null){
        			pin += "0000";
        		} else {
        			pin += matcher.group(5);
        		}
	        	function.setParamValue(pin);
	        	numOfPins ++;
	        	pins.append(pin).append(",");
	        	if (!pinsASList.contains(pin)) {
	        		numOfNewPins++;
	        	}
        	} else {
        		errors += "PIN: <b>" + val + "</b> is invalid.";
        	}
        }
        module.getFunction(11).setParamValue("" + numOfPins);
        if(!StringUtils.isEmpty(errors)){
        	return ServerResponse.createErrorResponse(errors);
        }
        if(numOfPins == 0){
        	return ServerResponse.createErrorResponse("At least one PIN must be entered!");
        }
        
		if (numOfNewPins ==0 ){					//there are no new PINs (all were already searched with before)
			return new ServerResponse();
		}
		
		mSearch.setAdditionalInfo("ALREADY_SEARCHED_PINS_TU", pins.toString().replaceAll(",$", ""));
		
        return super.SearchBy(module, sd);
    }
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;

		// P1 : search by PIN
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		Search global = getSearch();
		DocumentsManagerI manager = global.getDocManager();
		try {
			manager.getAccess();
			//List<DocumentI> documents = manager.getDocumentsWithDocType("TRANSFER", "MORTGAGE");
				//if (documents != null){
					StringBuffer newPins = new StringBuffer();
					//for (DocumentI documentI : documents) {
					TransferI lastRealTransfer = manager.getLastRealTransfer();
					if (lastRealTransfer != null){
						Set<PropertyI> prop = lastRealTransfer.getProperties();
						if (prop != null){
							for (Iterator<PropertyI> iterator = prop.iterator(); iterator.hasNext();) {
								PropertyI propertyI = (PropertyI) iterator.next();
								String pid = propertyI.getPin().getPin(PinI.PinType.PID);
								if(pid != null && !pin.contains(pid) && !newPins.toString().contains(pid)) {
									newPins.append(",").append(pid);
								}
							}
						}
					}
					MortgageI lastMortgage = manager.getLastMortgageForOwner();
					if (lastMortgage != null){
						Set<PropertyI> prop = lastMortgage.getProperties();
						if (prop != null){
							for (Iterator<PropertyI> iterator = prop.iterator(); iterator.hasNext();) {
								PropertyI propertyI = (PropertyI) iterator.next();
								String pid = propertyI.getPin().getPin(PinI.PinType.PID);
								if(pid != null && !pin.contains(pid) && !newPins.toString().contains(pid)) {
									newPins.append(",").append(pid);
								}
							}
						}
					}
					//}
					if (newPins.length() > 0){
						pin = newPins.toString();
					}
				//}
			}  catch (Throwable t) {
				logger.error("Error while creating derivation list", t);
			} finally {
				manager.releaseAccess();
			}
			
		if(!"".equals(pin)){
			pin = pin.replaceAll("\\s+","").trim();
			pin = pin.replaceFirst("^,", "");
			pin = pin.replaceFirst(",$", "");			
			String [] pins = pin.split(",");
			int length = pins.length;
			if(length > 10){
				length = 10;
			}			
			module = new TSServerInfoModule(serverInfo.getModule(PIN_MODULE_IDX));
			module.clearSaKeys();
			for(int i=0; i<length; i++){
				module.getFunction(i+1).forceValue(pins[i]);
			}
			modules.add(module);		
		}
		
		// set list for automatic search 
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	@Override
	protected void ParseResponse(String action, ServerResponse Response, int viParseID) throws ServerResponseException {
		String response = Response.getResult();
		ParsedResponse pr = Response.getParsedResponse();
		
		switch (viParseID) {
		
		case ID_SEARCH:
			
			// get intermediate results
			String interm = getInterm(response);
			if(StringUtils.isEmpty(interm)){
				return;
			}
			
			pr.setResponse(interm);
			parser.Parse(Response.getParsedResponse(), interm, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), REQUEST_SAVE_TO_TSD);
			
			// treat header, footer
        	String header = pr.getHeader();
           	String footer = pr.getFooter();                           	
        	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");        	
        	header += "\n<table width=\"568\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">" +
            		    "<tr bgcolor=\"#cccccc\">" +	            			    
	            			"<th width=\"1%\">" + SELECT_ALL_CHECKBOXES + "</th>" +
	            			"<th width=\"99%\" align=\"left\">PIN</th>" +
            			"</tr>";
        	footer = "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, interm.contains("checkbox")?-1:0);        	            	
        	pr.setHeader(header);
        	pr.setFooter(footer);
			break;
			
		case ID_DETAILS:
			String details = getDetails(response);
			if(StringUtils.isEmpty(details)){
				Response.getParsedResponse().setError("Invalid details page!");
				return;
			}
			details = details.replaceAll("(?is)<a[^>]*>[^<]*</a>\\s*\\|?", "");
			details = details.replaceAll("(?is)(<div class=footerlink>)\\s*[^<]+<br>[^<]*</div>", "");
			
			String pin = StringUtils.extractParameter(details, "<b>\\s*PIN:\\s*</b>\\s*([0-9-]+)\\s*</td>");
			if(StringUtils.isEmpty(pin)){
				Response.getParsedResponse().setError("Could not identify the property PIN!");
				return;
			}			
			if(!downloadingForSave) {		
                String qry = Response.getRawQuerry();
                qry = "dummy=" + pin + "&" + qry;
                String originalLink = action + "&" + qry;
                String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
                
                HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","CNTYTAX");
				data.put("dataSource", "TU");
				if(isInstrumentSaved(pin, null, data)){
					details += CreateFileAlreadyInTSD();
				} else {
	                mSearch.addInMemoryDoc(sSave2TSDLink, response);
	                details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
				}
                
                
                LinkInPage lip = new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD);
                Response.getParsedResponse().setPageLink(lip);
                Response.getParsedResponse().setResponse(details);
			} else {
				msSaveToTSDFileName = pin + "_tu.html";				
                parser.Parse(Response.getParsedResponse(), details, Parser.PAGE_DETAILS, 
                		getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
			}

			break;
			
		case ID_GET_LINK :
			ParseResponse(action, Response, ID_DETAILS);
			break;
			
        case ID_SAVE_TO_TSD :
            downloadingForSave = true;
            ParseResponse(action, Response, ID_DETAILS);
            downloadingForSave = false;
            break; 
            
		}
	}
	
	/**
	 * Extract details page
	 * @param page
	 * @return
	 */
	private String getDetails(String page){
		int istart = page.indexOf("Cook County Tax Search Report");
		if(istart == -1){ return ""; }
		istart = page.lastIndexOf("<table", istart);
		if(istart == -1){ return ""; }
		int iend = page.lastIndexOf("Search Taxes</a>");
		if(iend == -1 || iend < istart){ return ""; }
		iend += "Search Taxes</a>".length();
		return page.substring(istart, iend);
	}
	
	/**
	 * Extract intermediate results
	 * @param page
	 * @return
	 */
	private String getInterm(String page){
		
		Pattern intermPattern = Pattern.compile("<a href=\"(reportb\\.asp\\?[^\"]+)\">([0-9-]+)</a>");
		
		int istart = page.indexOf("Here are your search result(s) for Order No:");
		if(istart == -1){ return ""; }
		istart = page.indexOf("<div", istart);
		if(istart == -1){ return ""; }
		istart = page.indexOf(">", istart);
		if(istart == -1){ return ""; }
		istart += ">".length();
		int iend = page.indexOf("</div>", istart);
		if(iend == -1){ return ""; }
		String interm = page.substring(istart, iend);
		StringBuilder sb = new StringBuilder();

		istart = interm.indexOf("<p>");
		iend = interm.indexOf("</p>", istart);
		while(istart != -1 && iend != -1){
			iend += "</p>".length();
			String row = interm.substring(istart, iend);
			Matcher intermMatcher = intermPattern.matcher(row);
			if(intermMatcher.find()){
				String origLink = intermMatcher.group(1);
				String pin = intermMatcher.group(2);
				String sSave2TSDLink = CreatePartialLink(TSConnectionURL.idGET) + origLink.replace("?", "&");
				String checkBox = "";
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","CNTYTAX");
				data.put("dataSource", "TU");
				if(isInstrumentSaved(pin, null, data)){
					checkBox = "saved";
				} else {
					checkBox = "<input type='checkbox' name='docLink' value='" + sSave2TSDLink + "'>";	
				}
				
				
				String atsLink = "<a href='" + sSave2TSDLink + "'>" + pin + "</a>";
				String html = "<tr><td>" + checkBox + "</td><td>" + atsLink + "</td></tr>";
				sb.append(html);
			} else {
				sb.append("<tr><td><input type='checkbox' disabled/></td><td>" + row + "</td></tr>");
			}
			
			istart = interm.indexOf("<p>", iend);
			iend = interm.indexOf("</p>", istart);			
		}
		
		return sb.toString();
		
	}
	@Override
	protected String getFileNameFromLink(String link){
		return link;
	}

	/**
	 * Called by the parser through reflection
	 */
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
    throws ro.cst.tsearch.exceptions.ServerResponseException {        
        p.splitResultRows(pr, htmlString, pageId, "<tr", "</tr>", linkStart, action);
    }

}
