package ro.cst.tsearch.servers.types;



import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;


/**
 * @author mihaib
 *
 *for TX Collin, Denton, Johnson, Tarrant -like sites        ADD here the new county implemented with this Generic
 */


public class TXGenericSMediaTR extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 
	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
	private static final Pattern PREV_PAT = Pattern.compile("(?is)<a\\s*href=\\\"(searchresults\\.asp[^\\\"]+)[^\\[]+\\['Previous[^>]+>");
	private static final Pattern NEXT_PAT = Pattern.compile("(?is)<a\\s*href=\\\"(searchresults\\.asp[^\\\"]+)[^\\[]+\\['Next[^>]+>");
	private static final Pattern ALL_YEARS_PAT = Pattern.compile("(?is)\\\"(accountInfo[^\\\"]+)\\\"");
	
		
	public TXGenericSMediaTR(long searchId) {
		super(searchId);
	}

	public TXGenericSMediaTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		
		String address = getSearch().getSa().getAtribute(SearchAttributes.P_STREET_FULL_NAME_EX);
	
		GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
		FilterResponse defaultNameFilter = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module );
		FilterResponse rnre = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		rnre.setThreshold(new BigDecimal("0.65"));
		
		addPinModule(serverInfo, modules);
		
		if (hasStreet()) {
			//Search by Property Address
			address = address.replaceAll("(?is)\\bcounty\\s+road\\b", "cr");
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.forceValue(0, address);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.addFilter(rnre);
			modules.add(module);
		}
		
		if (hasOwner()) {
			//Search by Owner
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilter(rnre);
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			if(dataSite.getCountyId() == CountyConstants.TX_Tarrant) {
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			} else {
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			}
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
			module.addIterator(nameIterator);
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);		
	}

	protected void addPinModule(TSServerInfo serverInfo, List<TSServerInfoModule> modules) {
		TSServerInfoModule module;
		String pid = "";
		int countyId = dataSite.getCountyId();

		if (countyId == CountyConstants.TX_Denton) {
			pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO2).replaceAll("[-\\s]", "") + "DEN";
			if (pid.equals("DEN")) {
				pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO) + "DEN";
				pid = pid.replace("DENDEN", "DEN");
			}
		} else {
			pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
		}

		if (hasPin()) {
			// Search by Account No.
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setData(0, pid);
			modules.add(module);
		}
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;

		switch (viParseID) {		
		
			case ID_SEARCH_BY_NAME :
				
				if (rsResponse.indexOf("no records were found") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				}
				
				try {
					 
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
					 
					String table = "";
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
					NodeList mainList = htmlParser.parse(null);
					NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
					for (int k = tables.size() - 1; k < tables.size(); k--){
						if (tables.elementAt(k).toHtml().contains("Legal")){
							table = tables.elementAt(k).toHtml();
							break;
						}
					}
							
					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, table, outputTable);
											
					if(smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
						parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
		            }

				}catch(Exception e) {
					e.printStackTrace();
				}
								
				break;
				
			case ID_DETAILS :
				
				if (rsResponse.indexOf("Please try your search again") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">Please try your search again</font>");
					return;
				}
				
				String details = "";
				details = getDetails(rsResponse);				
				String pid = "";
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
					NodeList mainList = htmlParser.parse(null);
					pid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Account:"), "", true).replaceAll("</?font[^>]*>", "").trim();
				} catch(Exception e) {
					e.printStackTrace();
				}

				if ((!downloadingForSave)){	
	                String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + pid + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					
					if(isInstrumentSaved(pid, null, data)){
		                details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
					
	                Response.getParsedResponse().setPageLink(
							new LinkInPage(
								sSave2TSDLink,
								originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
					parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
	            } else{      
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = pid + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();                
				}
				
				break;	
			
			case ID_GET_LINK :
				if (sAction.indexOf("accountInfo.asp") != -1){
						ParseResponse(sAction, Response, ID_DETAILS);
				} else if (sAction.indexOf("searchresults.asp") != -1){
						ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				
				break;
			
			case ID_SAVE_TO_TSD :

				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;
		}
	}
	
	private String proccessLinks(ServerResponse response) {
		String nextLink = "", prevLink = "";
		String header = "";
		
		try {
			//String qry = response.getQuerry();
			String rsResponse = response.getResult();
			Matcher prevMat = PREV_PAT.matcher(rsResponse);
			if (prevMat.find()){
				prevLink = CreatePartialLink(TSConnectionURL.idGET);
				
				if (this instanceof TXCollinTR){
					prevLink += "/taxweb/";
				}
				prevLink += prevMat.group(1);
				prevLink = prevLink.replaceAll("&amp;", "&");
			}
			
			Matcher nextMat = NEXT_PAT.matcher(rsResponse);
			if (nextMat.find()){
				nextLink = CreatePartialLink(TSConnectionURL.idGET);
				if (this instanceof TXCollinTR){
					nextLink += "/taxweb/";
				}
				nextLink += nextMat.group(1);
				nextLink = nextLink.replaceAll("&amp;", "&");
			}
			
			if (StringUtils.isNotEmpty(prevLink)){
				header = "&nbsp;&nbsp;&nbsp;<a href=\"" + prevLink + "\">Prev</a>&nbsp;&nbsp;&nbsp;";
			}
			if (StringUtils.isNotEmpty(nextLink)){
				header += "&nbsp;&nbsp;&nbsp;<a href=\"" + nextLink + "\">Next</a>";
				
				String moduleIndexParam = StringUtils.extractParameterFromUrl(response.getQuerry(), "cboSearch");
				boolean setNextLink = true;
				if (StringUtils.isNotEmpty(moduleIndexParam) && moduleIndexParam.equals("5")) {
					// if it's a search by PIDN in automatic(the param cboSearch is 5) then don't set Next link
					setNextLink = false;
				}
				if (setNextLink) {
					response.getParsedResponse().setNextLink("<a href='" + nextLink + "'>Next</a>");
				}
			}

		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return header;
	}
	
	protected String getDetails(String rsResponse){
		
		if (rsResponse.toLowerCase().indexOf("<body") == -1)
			return rsResponse;
		
		String contents = "";
		
		Matcher mat = ALL_YEARS_PAT.matcher(rsResponse);
		if (mat.find()){
			String link = cleanBaseLink();
			if (this instanceof TXCollinTR){
				link += "/taxweb/";
			}
			HTTPRequest reqP = new HTTPRequest(link + mat.group(1).replaceAll("&amp;", "&"));
	    	reqP.setMethod(HTTPRequest.GET);
	    	
	    	HTTPResponse resP = null;
        	HttpSite site = HttpManager.getSite(HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName(), searchId);
			try{
				resP = site.process(reqP);
			} finally{
				HttpManager.releaseSite(site);
			}	
			rsResponse = resP.getResponseAsString();			
		}
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList mainList = htmlParser.parse(null);
			
			NodeList tables = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
			for (int k = 0; k < tables.size(); k++){
				if (tables.elementAt(k).toHtml().contains("New Search")){
					contents = tables.elementAt(k).toHtml();
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		contents = contents.replaceAll("</?a[^>]*>", "");
		contents = contents.replaceAll("<img[^>]*>", "");
		contents = contents.replaceAll("(?is)<form.*?</form>", "");
		contents = contents.replaceAll("(?is)<font[^>]*>\\s*Click on[^<]+</font>", "");
		
		return contents;
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			table = table.replaceAll("\\s*</?font[^>]*>\\s*", "").replaceAll("(?is)<!--[^-]+-->", "");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList mainTableList = htmlParser.parse(null);
			TableTag mainTable = (TableTag)mainTableList.elementAt(0);
			
			TableRow[] rows = mainTable.getRows();
						
			for(int i = 1; i < rows.length; i++ ) {
				if(rows[i].getColumnCount() > 1) {
					ResultMap resultMap = new ResultMap();
					
					TableColumn[] cols = rows[i].getColumns();
					String parcelNo = cols[0].toHtml().replaceAll("</?td[^>]*>", "").replaceAll("</?a[^>]*>", "").trim();
					String ownerName = cols[1].getChildrenHTML().trim().replaceAll("(?is)&nbsp;", " ");
					String address = cols[2].getChildrenHTML().trim().replaceAll("(?is)&nbsp;", " ");
					String legal = cols[3].getChildrenHTML().trim().replaceAll("(?is)&nbsp;", " ");
					String parcelNo2 = "";

					if (this instanceof TXCollinTR || this instanceof TXTarrantTR || this instanceof TXDentonTR){
						 	ownerName = cols[2].getChildrenHTML().trim().replaceAll("(?is)&nbsp;", " ");
							address = cols[3].getChildrenHTML().trim().replaceAll("(?is)&nbsp;", " ");
							legal = cols[4].getChildrenHTML().trim().replaceAll("(?is)&nbsp;", " ");
							parcelNo2 = cols[1].toPlainTextString().trim().replaceAll("(?is)&nbsp;", "");
					}
					
					String link = CreatePartialLink(TSConnectionURL.idGET);
					if (this instanceof TXCollinTR){
						link += "/taxweb/";
					}
					
					link += HtmlParser3.getFirstTag(rows[i].getColumns()[0].getChildren(), LinkTag.class, true).getLink();
					if (link.contains("row=")) {
						link = link.replaceFirst("(?is)row=\\d+", "accountNumber=" + parcelNo);
					}
					
					String rowHtml =  rows[i].toHtml().replaceFirst("(?is)href=[\\\"']([^\\\"']+)[^>]+>",
							" href=\"" + link + "\">" ).replaceAll("&amp;", "&");
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rows[i].toHtml());
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
					
					resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
					resultMap.put("tmpOwner", ownerName);
					resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
					if (StringUtils.isNotEmpty(address)){
						resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
						resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
					}
					if (this instanceof TXCollinTR){
						if (parcelNo.trim().startsWith("R")){
							resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "REAL ESTATE");
						}
						ro.cst.tsearch.servers.functions.TXCollinTR.partyNamesTXCollinTR(resultMap, getSearch().getID());
						ro.cst.tsearch.servers.functions.TXCollinTR.parseLegalTXCollinTR(resultMap, getSearch().getID());
						if (StringUtils.isNotEmpty(parcelNo2)) {
							// on TX Collin the parsed parcel ID is the PIDN, not the Account No.
							parcelNo = parcelNo2;
						}
					} else if (this instanceof TXTarrantTR){
						if (!(legal.toLowerCase().contains("mineral") 
								&& legal.toLowerCase().contains("gas well") && legal.toLowerCase().contains("personal property"))){
							resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "REAL ESTATE");
						}
						ro.cst.tsearch.servers.functions.TXCollinTR.partyNamesTXCollinTR(resultMap, getSearch().getID());
						ro.cst.tsearch.servers.functions.TXCollinTR.parseLegalTXTarrantTR(resultMap, getSearch().getID());
					} else if (this instanceof TXJohnsonTR){
						if (!parcelNo.trim().startsWith("000")){
							resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "REAL ESTATE");
						}
						ro.cst.tsearch.servers.functions.TXJohnsonTR.partyNamesTXJohnsonTR(resultMap, getSearch().getID());
						ro.cst.tsearch.servers.functions.TXJohnsonTR.parseLegalTXJohnsonTR(resultMap, getSearch().getID());
					} else if (this instanceof TXDentonTR){
						if (!(legal.toLowerCase().contains("mineral") && legal.toLowerCase().contains(" mhp") 
								&& legal.toLowerCase().contains(" gas") && legal.toLowerCase().contains("personal property"))){
							resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "REAL ESTATE");
						}
						ro.cst.tsearch.servers.functions.TXCollinTR.partyNamesTXCollinTR(resultMap, getSearch().getID());
						ro.cst.tsearch.servers.functions.TXCollinTR.parseLegalTXCollinTR(resultMap, getSearch().getID());
					}
					
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelNo);
					resultMap.removeTempDef();
					
					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
			}
			
		
		String header1 = rows[0].toHtml();
		String header0 = proccessLinks(response) + "<br><br><br>";
			
		header1 = header1.replaceAll("(?is)</?a[^>]*>","");
		Pattern tablePat = Pattern.compile("<table[^>]+>");
		Matcher mat = tablePat.matcher(table);
		if (mat.find()){
			header0 += mat.group(0);
		}
		response.getParsedResponse().setHeader(header0 +  header1);
				
		response.getParsedResponse().setFooter("</table>");

		
		outputTable.append(table);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
		
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		
		detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", " ");
		detailsHtml = detailsHtml.replaceAll("(?is)<CAPTION[^>]*>", "<tr><td>").replaceAll("(?is)</CAPTION[^>]*>", "</td></tr>")
									.replaceAll("(?is)<th", "<td").replaceAll("(?is)</th>", "</td>").replaceAll("\n", "")
									.replaceAll("</?div[^>]*>", "").replaceAll("(?is)<\\s*/?\\s*b\\s*>", "").replaceAll("(?is)</?font[^>]*>", "");
		
		map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");

		try {		
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			String pid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Account:"), "", true).replaceAll("</?font[^>]*>", "").trim();
			String apd = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "APD:"), "", true).replaceAll("</?font[^>]*>", "").trim();

			map.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), StringUtils.isNotEmpty(pid) ? pid : "");
			if (StringUtils.isNotEmpty(apd)){
				map.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), StringUtils.isNotEmpty(apd.replaceAll("-", "")) ? apd : "");
			} else {
				map.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), StringUtils.isNotEmpty(pid.replaceAll("-", "")) ? pid : "");
			}

			String ownerName = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Owner:"), "", true).replaceAll("</?font[^>]*>", "").trim();
			ownerName = ownerName.replaceAll("(?is)</?span[^>]*>", "").replaceAll("(?is)\r\n", "");
			map.put("tmpOwner", StringUtils.isNotEmpty(ownerName) ? ownerName : "");
			
			String siteAddress = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Location:"), "", true).replaceAll("</?font[^>]*>", "").trim();
			if (StringUtils.isNotEmpty(siteAddress)){
				siteAddress = siteAddress.replaceAll("\\A0+", "");
				map.put("tmpAddress", siteAddress);
			}
			
			String legal = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Legal:"), "", true).replaceAll("</?font[^>]*>", "").trim();
			map.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), StringUtils.isNotEmpty(legal) ? legal : "");
			
			String landValue = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Land"),"", true).trim();
			if (StringUtils.isNotEmpty(landValue)){
				landValue = landValue.replaceAll("<[^>]+>", "");
				landValue = landValue.replaceAll("[\\$,]", "").trim();
				map.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), landValue);
			}
			
			String improvementValue= HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Improvement"),"", true).trim();
			if (StringUtils.isNotEmpty(improvementValue)){
				improvementValue = improvementValue.replaceAll("<[^>]+>", "");
				improvementValue = improvementValue.replaceAll("[\\$,]", "").trim();
				map.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvementValue);
			}
			
			String assessed = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Total Market Value"),"", true).trim();
			if (StringUtils.isNotEmpty(assessed)){
				assessed = assessed.replaceAll("(?is)([\\d\\.]+).*", "$1");
				map.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessed);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		if (this instanceof TXJohnsonTR){
			ro.cst.tsearch.servers.functions.TXJohnsonTR.parseAndFillResultMap(detailsHtml, map, searchId, miServerID);
		} else if (this instanceof TXCollinTR || this instanceof TXTarrantTR || this instanceof TXDentonTR){
			ro.cst.tsearch.servers.functions.TXCollinTR.parseAndFillResultMap(detailsHtml, map, searchId, miServerID, crtCounty);
		}
			
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
	/**
	 * override in each subclass to clean the base link
	 */
	
	protected String cleanBaseLink() {
		return getBaseLink();
		
	}

}