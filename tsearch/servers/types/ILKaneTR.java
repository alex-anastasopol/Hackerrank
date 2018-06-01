package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.MultiplePinFilterResponse;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StreetNameCorrespondences;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author mihaib
  */

public class ILKaneTR extends TSServerAssessorandTaxLike {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 

	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
	public ILKaneTR(long searchId) {
		super(searchId);
	}

	public ILKaneTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}


	public static void splitResultRows(
			Parser p,
			ParsedResponse pr,
			String htmlString,
			int pageId,
			String linkStart,
			int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException
			{
			
			p.splitResultRows(
				pr,
				htmlString,
				pageId,
				"<tr><td>",
				"</tr>",
				linkStart,
				action);
		}
	
	private FilterResponse getMultiPinFilter(){
		return new MultiplePinFilterResponse(searchId);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
		FilterResponse cityFilter 		= CityFilterFactory.getCityFilter(searchId, 0.6d);
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d );
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if (Search.AUTOMATIC_SEARCH == searchType){
		// P0 - search by multiple PINs
			Collection<String> pins = getSearchAttributes().getPins(-1);
			if(pins.size() > 1){
				Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				search.setAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN, Boolean.TRUE);
				
				for(String pin: pins){
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
					module.clearSaKeys();
					module.getFunction(0).forceValue(pin);
					modules.add(module);	
				}			
				// set list for automatic search 
				serverInfo.setModulesForAutoSearch(modules);
				resultType = MULTIPLE_RESULT_TYPE;
				return;
			}
		}
		
		// P2 - search by PIN
		if(hasPin()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO);  
			modules.add(module);		
		}
		
		
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(streetName);
			module.addFilter(cityFilter);
			module.addFilter(addressFilter);
			module.addFilter(getMultiPinFilter());
			modules.add(module);
			
			//Search by Property Address with correspondence
			
			if (StreetNameCorrespondences.getInstance(searchId).hasCorrespondence(streetName)){
				String streetCorresp = StreetNameCorrespondences.getInstance(searchId).getCorrespondent(streetName);
				getSearch().getSa().setAtribute(SearchAttributes.P_STREETNAME, streetCorresp);
				addressFilter 	= AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d );
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(0).forceValue(streetCorresp);
				module.addFilter(cityFilter);
				module.addFilter(addressFilter);
				modules.add(module);
			}
		}
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	@Override
	protected int getResultType(){
		if(mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) == Boolean.TRUE ||
				(mSearch.getSa().getPins(-1).size() > 1 && (Search.AUTOMATIC_SEARCH == mSearch.getSearchType()))){
			return MULTIPLE_RESULT_TYPE; 
		} else {
			return UNIQUE_RESULT_TYPE;
		}
	}
	
	@Override
    public boolean anotherSearchForThisServer(ServerResponse sr) {
		boolean result = mSearch.getSa().getPins(-1).size() > 1 &&
			    		 mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) != Boolean.TRUE;
		return result?true:super.anotherSearchForThisServer(sr);
	}
	
	
	private String getContents(String response){
		
		String contents = "";
		// cleanup
		contents = response.replaceAll("(?is)<a\\s+href\\s*=\\s*\\\"javascript:\\(printReady.*?Print Version\\s*</font>\\s*</a>","");
		contents = contents.replaceAll("(?is)</?blockquote>","");

		return contents;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {	
		
		String response = Response.getResult();
		//String initialResponse = response;
		String contents = null;
		String sTmp = CreatePartialLink(TSConnectionURL.idGET);
				
		switch(viParseID){
		
		case ID_SEARCH_BY_ADDRESS:
			
			if(!response.matches("(?is).*Treasurer\\.aspx.*")){
				Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
				return;
			}
			if (response.indexOf("alert('No parcels found with that number')") != -1){
				Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
				return;
			}
			
			if (response.indexOf("An error has occurred") != -1){
				Response.getParsedResponse().setError("<font color=\"red\">An error has occurred on Document Server.</font>");
				return;
			}
			
			Node tbl = HtmlParserTidy.getNodeById(response, "gvStreetListingT", "table");
			String html = "";
			try {
				html = HtmlParserTidy.getHtmlFromNode(tbl);
			} catch (TransformerException e) {
				e.printStackTrace();
			}
			// extract contents
			contents = getContents(html);
			
			
			if(contents == null){
				Response.getParsedResponse().setError("<font color=\"red\">Error retrieving page</font>");
				return;
			}
			
			contents = contents.replaceAll("(?is)<tr>\\s*<td>", "<tr><td>");
			contents = contents.replaceAll("(?is)(<a\\s+href\\s*=\\s*\\\")([^\\\"]+)\\\">", "$1" + sTmp + "/TaxAssessment/$2\">");
			
		    parser.Parse(
					Response.getParsedResponse(),
					contents,
					Parser.PAGE_ROWS,
					getLinkPrefix(TSConnectionURL.idGET),
					TSServer.REQUEST_SAVE_TO_TSD);
			
			break;
			
		case ID_SEARCH_BY_PARCEL:
			if (response.indexOf("parcels selected") != -1){
				ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
				break;
			} else {
				ParseResponse(sAction, Response, ID_DETAILS);
				break;
			}
		
		case ID_DETAILS:
			
			if (response.indexOf("alert('No parcels found with that number')") != -1){
				Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
				return;
			}
			
			if (response.indexOf("An error has occurred") != -1){
				Response.getParsedResponse().setError("<font color=\"red\">An error has occurred on Document Server.</font>");
				return;
			}
			
			String details = getDetails(response);
			
			// isolate pin number
			String keyCode = "File";
			
			String parcelNo = "";
			Document doc = Tidy.tidyParse(response);

			parcelNo = HtmlParserTidy.getValueFromTagById(doc, "lblParcelNumber", "span");
			parcelNo = parcelNo.replaceAll("-","").trim();
			
			String year = HtmlParserTidy.getValueFromTagById(doc, "lblTaxHeading", "span");
			
			if (StringUtils.isNotEmpty(year)){
				year = year.replaceAll("(?is).*?County\\s+(\\d+)\\s+.*", "$1").trim();
			}
			
			if(StringUtils.isNotEmpty(parcelNo))
			{
				keyCode = parcelNo.trim();
				//keyCode = keyCode.replaceAll("-","");
			} 
			
			if ((!downloadingForSave)){
                
                String qry_aux = Response.getRawQuerry();
				qry_aux = "dummy=" + keyCode + "&" + qry_aux;
				String originalLink = sAction + "&" + qry_aux;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "CNTYTAX");
				
				if (StringUtils.isNotEmpty(year) && year.matches("\\d{4}")){
					data.put("year", year);
				}
				
				if(isInstrumentSaved(parcelNo, null, data)){
                	details += CreateFileAlreadyInTSD();
				} else{
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
				}

				Response.getParsedResponse().setPageLink(
					new LinkInPage(
						sSave2TSDLink,
						originalLink,
						TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(
					Response.getParsedResponse(),
					details,
					Parser.NO_PARSE); 
            } else{      
				smartParseDetails(Response,details);
                msSaveToTSDFileName = keyCode + ".html";
                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();                
                
			}
			break;
		
		case ID_GET_LINK :
			if (response.contains("tblTaxes"))
				ParseResponse(sAction, Response, ID_DETAILS);
			else if (response.contains("gvStreetListingT"))
				ParseResponse(sAction, Response, ID_SEARCH_BY_ADDRESS);
			break;
		
		case ID_SAVE_TO_TSD:
			
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;				
			break;	
		default:
			break;
		}
	}
	
	protected String getDetails(String response){
		
		// if from memory - use it as is
		if(!response.contains("<html")){
			return response;
		}
		
		HtmlParser3 parser = new HtmlParser3(response);
		
		StringBuilder sb = new StringBuilder();
		
		org.htmlparser.Node nodeById = parser.getNodeById("lblTaxHeading");
		
		sb.append("<div>").append(nodeById.toHtml()).append("</div>");
		
		sb.append("<div>").append(parser.getNodeById("Label14").toHtml()).append("&nbsp;&nbsp;&nbsp;").append(parser.getNodeById("lblParcelNumber").toHtml()).append("</div>");
		
		NodeList normalTextTables = parser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true)
			.extractAllNodesThatMatch(new HasAttributeFilter("class", "NormalText"))
			.extractAllNodesThatMatch(new NotFilter(new HasAttributeFilter("width")))
			.extractAllNodesThatMatch(new NotFilter(new HasAttributeFilter("id")));

		if(normalTextTables.size() > 0) {
			sb.append(normalTextTables.elementAt(0).toHtml().replaceAll("(;?)width: (\\d+)%;?", "$1"));
		}
		
		org.htmlparser.Node tblTaxes = parser.getNodeById("tblTaxes");
		if(tblTaxes != null) {
			String year = nodeById.toPlainTextString();
			if (StringUtils.isNotEmpty(year)){
				year = year.replaceAll("(?is).*?County\\s+(\\d+)\\s+.*", "$1").trim();
			}
			
			String taxHistDetailed = addYearsDetail(response, year);
			
			sb.append(tblTaxes.toHtml()
					.replaceAll("(;?)width: (\\d+)%;?", "$1")
					.replaceAll("\\bwidth\\s*=\\s*\"?\\d+%\"?(\\b|>)", "$1")
					.replaceAll("(?is)\\bwidth\\s*=\\s*\"?auto\"?(\\b|>)", "$1")
					.replaceAll("(?is)<\\s*p[^>]+>\\s*Kane\\s+County\\s+Clerk.*?</p>", "")
					.replaceAll("(?is)</?a[^>]*>", "")
					.replaceAll("(?is)<div\\s+style\\s*=\\s*\\\"overflow[^>]+>", "<div>")
					.replaceAll("(?is)</table>$", taxHistDetailed + "</table>")
					);
		}
		
		return sb.toString();
	}
	
	private String addYearsDetail(String response, String currentYear) {
		
		Pattern pattern = Pattern.compile("(?is)<a\\s+href\\s*=\\s*\\\"(Treasurer.aspx[^\\\"]+)\\\"\\s*>\\s*([^<]+)");
		Matcher matcher = pattern.matcher(response);
		List<String> yearLinks = new ArrayList<String>();
		String taxDetails = "<tr><td colspan=2 style=\"background-color:Black; height: 1px; width: 100%; margin: 0\"></td></tr>"
							+ "<tr height=80px class=\"NormalText\">"
							+ "<td rowspan=\"3\" style=\"height: 30px; font-weight: bold; font-size: 14px; font-family: Arial; text-align: center;" 
							+ "padding-left:10px;\" valign=\"top\" class=\"NormalText\"> Tax History Details<br /><br />"
							+ "<div ><div>"
							+ "<table class=\"NormalText\" cellspacing=\"0\" rules=\"all\" border=\"0\" id=\"gvTaxHistoryDetails\">"
							+ "<tr><th scope=\"col\">Paid Date</th><th scope=\"col\">Paid Amount</th><th scope=\"col\">Amount Due</th></tr>";

		while(matcher.find()){
			String link = matcher.group(1);
			
			int currentYearInt = Integer.parseInt(currentYear);
			if (currentYearInt > 0){
				String yearLink = StringUtils.extractParameterFromUrl(link.replaceAll("(?is)&amp;", "&"), "TaxYear").trim();
				if (StringUtils.isNotEmpty(yearLink) && yearLink.matches("\\d+")){
					int yearLinkInt = Integer.parseInt(yearLink);
					if (yearLinkInt > 0){
						if ((currentYearInt + 1) >= yearLinkInt){
							yearLinks.add(matcher.group(1));
						}
					}
				}
				
			}
		}
		if(yearLinks.size() > 0) {
			//Collections.reverse(yearLinks);
		
			String amPaid1 = "", amPaid2 = "", amDue1 = "", amDue2 = "", paidDate1 = "", paidDate2 = "";
			Document doc = null;
			String yearContent = "";
			for (String yearLink : yearLinks) {
				yearContent = getLinkContents(getCrtServerLink() + "/TaxAssessment/" +yearLink.replaceAll("&amp;", "&"));
				doc = Tidy.tidyParse(yearContent);
				try {
					amPaid1 = HtmlParserTidy.getHtmlFromNode(HtmlParserTidy.getNodeById(doc, "lblFirstPaidAmount" , "span"));
					amPaid2 = HtmlParserTidy.getHtmlFromNode(HtmlParserTidy.getNodeById(doc, "lblSecondPaidAmount" , "span"));
					amDue1 = HtmlParserTidy.getHtmlFromNode(HtmlParserTidy.getNodeById(doc, "lblFirstAmountDue" , "span"));
					amDue2 = HtmlParserTidy.getHtmlFromNode(HtmlParserTidy.getNodeById(doc, "lblSecondAmountDue" , "span"));
					paidDate1 = HtmlParserTidy.getHtmlFromNode(HtmlParserTidy.getNodeById(doc, "lblFirstPaidDate" , "span"));
					paidDate2 = HtmlParserTidy.getHtmlFromNode(HtmlParserTidy.getNodeById(doc, "lblSecondPaidDate" , "span"));
					if (!paidDate1.contains("UNPAID")){
						amDue1 = "0.00";
					}
					if (!paidDate2.contains("UNPAID")){
						amDue2 = "0.00";
					}
					if (!amPaid2.contains("0.00")) {
						taxDetails += "<tr >"
								+ "<td style=\"width: 25%; border-top-style: none; border-right-style: none; border-left-style: none; border-bottom-style: none; \" align=\"right\">"
								+ paidDate2 + "</td>"
								+ "<td style=\"width: 25%; border-top-style: none; border-right-style: none; border-left-style: none; border-bottom-style: none; \" align=\"right\">"
								+ amPaid2 + "</td>"
								+ "<td style=\"width: 25%; border-top-style: none; border-right-style: none; border-left-style: none; border-bottom-style: none; \" align=\"right\">"
								+ amDue2 + "</td></tr>";
					}
					if (!amPaid1.contains("0.00")){
						taxDetails += "<tr >" 
								+ "<td style=\"width: 25%; border-top-style: none; border-right-style: none; border-left-style: none; border-bottom-style: none; \" align=\"right\">"
								+ paidDate1 + "</td>"
								+ "<td style=\"width: 25%; border-top-style: none; border-right-style: none; border-left-style: none; border-bottom-style: none; \" align=\"right\">"
								+ amPaid1 + "</td>"
								+ "<td style=\"width: 25%; border-top-style: none; border-right-style: none; border-left-style: none; border-bottom-style: none; \" align=\"right\">"
								+ amDue1 + "</td></tr>";
					}
				} catch (TransformerException e) {
					e.printStackTrace();
				}
			}
		}
		
		taxDetails += "</table><br></div><br></div><br></td></tr>";
		taxDetails = taxDetails.replaceAll("(?is)(<span.*?)\\s*id\\s*=\\s*\\\"[^\\\"]+\\\"", "$1");
		return taxDetails;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.ILKaneTR.parseAndFillResultMap(detailsHtml, map, searchId);
		return null;
	}
	
	public void  addAdditionalDocuments(DocumentI doc, ServerResponse response) {
		if (numberOfYearsAllowed > 1 && mSearch.getSearchType() == Search.AUTOMATIC_SEARCH){
        	numberOfYearsAllowed--; 
            try {
				addDocument(response, ((TaxDocumentI) doc).getInstno(), numberOfYearsAllowed, ((TaxDocumentI) doc).getYear());
			} catch (ServerResponseException e) {
				e.printStackTrace();
			}
        }
	}
	protected void addDocument(ServerResponse response, String pid, int numberOfYearsAllowed, int yearInt) throws ServerResponseException{
		
		String sAction = "/TaxAssessment/Treasurer.aspx?parcelnumber=" + pid + "&TaxYear=" + Integer.toString(yearInt--);
		String yearContent = getLinkContents("http://www.co.kane.il.us" + sAction);
		ServerResponse Response = new ServerResponse();
		Response.setResult(yearContent);
		
		yearContent = getDetails(yearContent);
			
		super.solveHtmlResponse(sAction, ID_SAVE_TO_TSD, "SaveToTSD", Response, yearContent);
	}
	
	/**
	 * Get the current server link
	 * @return
	 */
	private String getCrtServerLink(){
		String link = getDataSite().getLink();
		int idx = link.indexOf("/TaxAssessment");
		if(idx == -1){
			throw new RuntimeException("County " + getCurrentServerName() + " not supported by this class!");
		}
		return link.substring(0, idx);
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

}