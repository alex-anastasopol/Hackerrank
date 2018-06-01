package ro.cst.tsearch.servers.types;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;


/**
 * @author olivia
  */

@SuppressWarnings("deprecation")
public class COAdamsTR extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 

	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
	private static final Map<String,String> suffAbbreviations = new HashMap<String,String>();
	static{

		suffAbbreviations.put("AVENUE","Ave");
		suffAbbreviations.put("BOULEVARD","Blvd");
		suffAbbreviations.put("CIRCLE","Cir");
		suffAbbreviations.put("COURT","Ct");
		suffAbbreviations.put("DRIVE","Dr");
		suffAbbreviations.put("LANE","Ln");
		suffAbbreviations.put("LOOP","Lp");
		suffAbbreviations.put("PARKWAY","Pkwy");
		suffAbbreviations.put("PLACE","Pl");
		suffAbbreviations.put("RIDGE","Rdg");
		suffAbbreviations.put("ROAD","Rd");
		suffAbbreviations.put("STREET","St");
		suffAbbreviations.put("TRAIL","Trl");
		suffAbbreviations.put("WAY","Way");//CirN, CirS, DrN, DrS

	}
	
	public COAdamsTR(long searchId) {
		super(searchId);
	}

	public COAdamsTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		String pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
		
		FilterResponse adressFilter 	= AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true, true);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , module );
		FilterResponse defaultLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
//		DocsValidator nameValidatorHybrid = nameFilterHybrid.getValidator();
//		nameValidatorHybrid.setOnlyIfNotFiltered(true);
		FilterResponse rejectNonRealEstateFilter = new RejectNonRealEstate(SearchAttributes.OWNER_OBJECT, searchId);
		rejectNonRealEstateFilter.setThreshold(new BigDecimal("0.65"));
		TaxYearFilterResponse frYr = new TaxYearFilterResponse(searchId, true, numberOfYearsAllowed, true);
		frYr.setThreshold(new BigDecimal("0.95"));
		
//		AddressFromDocumentFilterForNext addressFilterForNext = new AddressFromDocumentFilterForNext(
//	    		getSearch().getSa().getAtribute(SearchAttributes.P_STREET_NO_NAME),searchId);
//		addressFilterForNext.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
//    	addressFilterForNext.setThreshold(new BigDecimal(0.78));
		
		if (hasPin()) {
			//Search by Pin
			if (pid.matches("[A-Z]\\d{7}")){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.clearSaKeys();
				module.setData(0, pid);
				module.addFilter(rejectNonRealEstateFilter);
				module.addFilter(frYr);
				modules.add(module);
			} else {
				if (pid.length() == 12){
					pid = "0" + pid;
				}
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(0).forceValue(getSearch().getSa().getAtribute(SearchAttributes.LD_CO_PARCEL_NO));
				module.addFilter(rejectNonRealEstateFilter);
				module.addFilter(frYr);
				modules.add(module);
			}
		}
		
		if (hasStreetNo()) {
			//Search by Property Address - only street no (more restrictive than full address search
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO).trim());
			module.addFilter(rejectNonRealEstateFilter);
			module.addFilter(adressFilter);
			module.addFilter(nameFilterHybrid);
			module.addFilter(defaultLegalFilter);
			modules.add(module);
		}
		
		if (hasStreet()) {
			//if no result found/ limit of 500 docs is reached, search only by street name
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME).trim());
			module.addFilter(rejectNonRealEstateFilter);
			adressFilter.setThreshold(new BigDecimal(0.7d));
			module.addFilter(adressFilter);
			module.addFilter(nameFilterHybrid);
			module.addFilter(defaultLegalFilter);
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;

		switch (viParseID) {		
			case ID_SEARCH_BY_INSTRUMENT_NO :
			case ID_SEARCH_BY_PARCEL :
			case ID_SEARCH_BY_ADDRESS :
				if (rsResponse.indexOf("Nothing found to display") != -1){
					Response.getParsedResponse().setError("<font color=\"black\">No results found</font>");
					return;
				}
				
				try {
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
					 
					String table = "";
					HtmlParser3 parser = new HtmlParser3(rsResponse);
					table = parser.getNodeById("searchResultsTable").toHtml();
													
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
				String details = "";
				details = getDetails(rsResponse);
				
				if (StringUtils.isEmpty(details)){
					Response.getParsedResponse().setError("<font color=\"red\">Empty data</font>");
					return;
				}
				String accountId = "";
				try {
					HtmlParser3 htmlParser = new HtmlParser3(details);	
					NodeList mainList = htmlParser.getNodeList();
					
					//TableColumn tc = (TableColumn) HtmlParser3.findNode(mainList, "Account Id", true).getParent().getParent();
					TableColumn tc = ((TableTag)mainList.extractAllNodesThatMatch(new HasAttributeFilter("id", "taxAccountSummary"), true)
							.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0)).getRows()[0].getColumns()[1];
					accountId = tc.getChildrenHTML().trim();
				} catch(Exception e) {
					e.printStackTrace();
				}

				if ((!downloadingForSave))
				{	
	                String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + accountId + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					
					if(isInstrumentSaved(accountId, null, data)){
		                details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
					
	                Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
	            
				} else {      
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = accountId + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();                
				}
				
				break;	
			
			case ID_GET_LINK :
				ParseResponse(sAction, Response, sAction.contains("page=") ? ID_SEARCH_BY_ADDRESS : ID_DETAILS);
				break;
			
			case ID_SAVE_TO_TSD :
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;
		}
	}
	
	protected String getDetails(String contents) {
		StringBuilder detPgInfo =  new StringBuilder();
		
		/* If from memory - use it as is */
		if(!contents.contains("<html")) {
			return contents;
		
		} else {
			String details = "";
			String[] followLink = {"", "", ""};
			int noOfLink = 0;
			
			detPgInfo.append("<table id=\"detailsInfo\" align=\"center\" border=\"1\" width=\"600px;\"> <tr> <td> <h1> Tax Account Information </h1> </td> </tr>");
			
			try {
				HtmlParser3 htmlParser = new HtmlParser3(contents);	
				NodeList mainList = htmlParser.getNodeList();
				Node info =  HtmlParser3.getNodeByID("taxAccountSummary", mainList, true);
				if (info != null) {
					detPgInfo.append("<tr> <td>");
					details = info.toHtml();
					detPgInfo.append(details);
					detPgInfo.append("<br>");
				}
				
				info = HtmlParser3.getNodeByID("paymentLinks", mainList, true);
				if (info != null) {
					details= info.toHtml();
					details = details.replaceAll("(?is)<a[^>]+>\\s*([^<]+)</a>", "$1");
					detPgInfo.append(details);
					detPgInfo.append("<br>");
				}
				
				info = HtmlParser3.getNodeByID("totals", mainList, true);
				if (info != null) {
					details= info.toHtml();
					detPgInfo.append(details);
				}
				detPgInfo.append("<br> </td> </tr>");
				
				info = HtmlParser3.getNodeByID("taxAccountValueSummary", mainList, true);
				if (info != null) {
					detPgInfo.append("<tr> <td>");
					details = info.toHtml();
					
					String link = details.replaceFirst("(?is).*<a href\\s*=\\s*'..([^']+)'[^>]*>.*", "$1");
					if (StringUtils.isNotEmpty(link)) {
						followLink[noOfLink] = getBaseLink() + "/treasurer" + link;
					}
					
					details = details.replaceFirst("(?is)<a[^>]+>\\s*(<h1>[^/]+/h1>)\\s*</a>", "$1");
					detPgInfo.append(details);
					detPgInfo.append("<br> </td> </tr>");
				}
				
				info =  HtmlParser3.getNodeByID("accountLinks", mainList, true);
				if (info != null) {
					Matcher mLink = Pattern.compile("(?is)<a href\\s*=\\s*'\\.\\./([^']+)'[^>]*>\\s*([^<]+)</a>").matcher(info.toHtml());
					while (mLink.find()) {
						if ("Transaction Detail".equals(mLink.group(2).trim())) {
							noOfLink ++;
							followLink[noOfLink] = getBaseLink() + "/treasurer/" + mLink.group(1);
						}
					}
				}
				
				if (followLink.length > 0) {
					for (int i=0; i < followLink.length; i++) {
						if (StringUtils.isNotEmpty(followLink[i])) {
							detPgInfo.append("<tr> <td>");
							details = getOtherTaxInfo(followLink[i]);
							detPgInfo.append(details);
							detPgInfo.append("<br> </td> </tr>");
						}
					}
				}
				
				detPgInfo.append("</table>");

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return detPgInfo.toString();
	}
	
	
	private String getOtherTaxInfo(String url) {
		String htmlInfo = getLinkContents(url);
		HtmlParser3 htmlParser = new HtmlParser3(htmlInfo);
		NodeList mainList = htmlParser.getNodeList();
		String headerText = "<h1> ";
		String id = "";
		boolean hasTaxHistory = false;
		
		if (url.contains("action=billing")) {
			headerText += "Billing details";
			id = "billingTable";
		} else if (url.contains("action=tx")) {
			headerText += "Tax History Details";
			id = "taxHistTable";
			hasTaxHistory = true;
		}
		headerText += " <br><br> </h1>";
		
		Node info =  HtmlParser3.getNodeByID("middle", mainList, true);
		if (info != null) {
			htmlInfo = info.toHtml();
			htmlInfo = htmlInfo.replaceAll("(?is)<a[^>]+>\\s*([^<]+)</a>", "");
			htmlInfo = htmlInfo.replaceAll("(?is)<h1>[\\w\\s<>]+</h1>", headerText);
			htmlInfo = htmlInfo.replaceAll("(?is)<p[^>]*>\\s*.*</p>", "");
			htmlInfo = htmlInfo.replaceFirst("(?is)(<table class='account stripe')", "$1 id=\""+ id + "\"");
			if (hasTaxHistory) {
				htmlInfo = htmlInfo.replaceFirst("(?is)(<table class='account')", "$1 id=\"taxSummary\"");
			}
		}
		
		return htmlInfo;
	}

	public StringBuilder getPrevAndNextLinks (ServerResponse response, HtmlParser3 htmlParser, String sourceTag) {
		String linkN = "";
		String linkP = "";
		String links = "";
		// create links for Next/Prev buttons
		StringBuilder footer = new StringBuilder("<tr><td colspan=\"2\"> </br> &nbsp; &nbsp;");
		
		htmlParser =  new HtmlParser3(response.getResult());
		NodeList nodeList = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "pagelinks"), true); 						 				
			
		if (nodeList.size() > 0) {
			Span prevNextInfo = (Span) nodeList.elementAt(1);
			
			Matcher matPrev = Pattern.compile("(?is)\\[\\s*<a[^>]+>\\s*First\\s*</a>\\s*/\\s*<a href=\\\"([^\\\"]+)\\\"[^>]*>\\s*Prev\\s*</a>\\s*]")
					.matcher(prevNextInfo.getChildrenHTML());
			Matcher matNext = Pattern.compile("(?is)\\[\\s*<a href=\\\"([^\\\"]+)\\\"[^>]*>\\s*Next\\s*</a>\\s*/\\s*\\s*<a[^>]+>\\s*Last\\s*</a>\\s*")
					.matcher(prevNextInfo.getChildrenHTML());
			
			if (matPrev.find()) {
				linkP = matPrev.group(1).replaceFirst("(?is)\\w+/\\.\\./", "");
				//linkP = getBaseLink() + linkP;
				linkP = linkP.replaceAll("&amp;", "&");
				linkP = CreatePartialLink(TSConnectionURL.idGET) + linkP;
			}
			if (matNext.find()) {
				linkN = matNext.group(1).replaceFirst("(?is)\\w+/\\.\\./", "");
//				linkN = getBaseLink() + linkN;
				linkN = linkN.replaceAll("&amp;", "&");
//				linkN = linkN.replaceFirst("(?is)(page=\\d+)&(searchId=\\d+)", "$2" + "&" + "$1");
				linkN = CreatePartialLink(TSConnectionURL.idGET) + linkN;
			}
			
			if (StringUtils.isNotEmpty(linkP)) {
				links = links + "<a href=\"" + linkP + "\"> Prev </a> &nbsp; &nbsp;";
			}
			 
			if (StringUtils.isNotEmpty(linkN)) {
				links = links + "<a href=\"" + linkN + "\"> Next </a> &nbsp; &nbsp;";
				response.getParsedResponse().setNextLink("<a href=\"" + linkN + "\">Next</a>");
				String qry_aux = response.getRawQuerry();
				if (qry_aux.contains("searchId")) {
					qry_aux = qry_aux + "&" + "dummy=yes";
					response.setQuerry(qry_aux);
				}
			}	
			
			footer.append(links);
		}
		
		return footer;
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();

		try {
			ResultMap resultMap = new ResultMap();
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
			
			HtmlParser3 htmlParser = new HtmlParser3(table);	
			NodeList mainTableList = htmlParser.getNodeList();
			
			TableTag mainTable = (TableTag) mainTableList.elementAt(0);

			TableRow[] rows = mainTable.getRows();
			for (TableRow row : rows) {
				if (row.getColumnCount() > 0) {
					TableColumn[] cols = row.getColumns();
					String accountNo = "";
					String link = "";
					
					if (cols[0].getChildCount() == 3) {
						accountNo = cols[0].getChild(1).toHtml();
					} else if (cols[0].getChildCount() == 2){
						accountNo = cols[0].getChild(01).getText();
					}
					if (StringUtils.isNotEmpty(accountNo)) {
						link = accountNo.replaceFirst("(?is)<a[^']+'([^']+)'[^>]*>\\s*ACCOUNT\\s*(?:<\\s*/?\\s*br\\s*/?\\s*>\\s*)?([^<]+)</a>", "$1");
						accountNo = accountNo.replaceFirst("(?is)<a[^']+'([^']+)'[^>]*>\\s*ACCOUNT\\s*(?:<\\s*/?\\s*br\\s*/?\\s*>\\s*)?([^<]+)</a>", "$2");
						if (accountNo.matches("R\\d{6,}")) {
							resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), "Real Estate");
						}
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNo);
					}
					
					if (cols[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true).size() > 0) {
						TableTag propInfoTbl = (TableTag) cols[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
						if (propInfoTbl.getRowCount() == 1) {
							if (propInfoTbl.getRow(0).getColumnCount() == 4) {
								TableColumn[] columns = propInfoTbl.getRow(0).getColumns();
								TableColumn c = columns[0]; //ParcelNo
								String parcelNo = c.getChildrenHTML();
								
								Matcher m = Pattern.compile("(?is)(?:<b>\\s*)?(?:<font[^>]+>)?(\\d+)\\s*<\\s*/?\\s*br\\s*/?\\s*>(?:\\s*</font>)?"
										+ "(?:\\s*</b>)?\\s*(?:<b>)?Balance\\s*:\\s*([\\d\\.]+)(?:\\s*</b>)?").matcher(parcelNo);
								if (m.find()) {
									parcelNo = m.group(1).trim();
									if (StringUtils.isNotEmpty(parcelNo)) {
										resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), parcelNo);
									}
								}
								
								c = columns[1]; //address
								String address = c.getChildrenHTML().trim().replaceAll("</?b>", "");
								address = address.replaceAll("_", " ");
								address = address.replaceFirst("(?is)#\\s*(\\b[A-Z]\\b)\\s+(\\d+)", "#$2$1");
								address = address.replaceFirst("(?is)#\\s*(\\d+)\\s+(\\b[A-Z]\\b)", "#$1$2");
//								address = address.replaceFirst("(?is)([\\sA-Z]+)(?:\\s0+)?<br>\\s*\\1", "$1");
								address = address.replaceFirst("(?is)(\\w+(?:\\s+\\w+)?)(?:\\b0+)?\\s*<br>\\s*\\1", "$1");
								address = address.replaceAll("\\s+0+", "");
								
								resultMap.put("tmpAddress", address);
								
								c = columns[2]; //owner info
								String ownerName = c.getChildrenHTML().trim().replaceAll("</?span[^>]*>", "");
//								ownerName = ownerName.replaceAll("(?is)(\\w+\\s+\\w+\\s+(:?\\w\\s+)?)(\\w+\\s+\\w+(?:\\s+\\w\\s+)?)$", "$1 AND $2");
								resultMap.put("tmpOwner", ownerName);
								
								c = columns[3]; //LD info
								String legDesc = c.getChildrenHTML().trim().replaceAll("</?b>", "");
								if (StringUtils.isNotEmpty(legDesc)) {
									resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legDesc);
								}
							}
						}
					}
					

					String rowHtml = row.toHtml().replaceAll("<a\\s*href='([^']+)[^>]*>", 
							"<a href=\"" + CreatePartialLink(TSConnectionURL.idGET) + "/treasurer/treasurerweb/" + link +"\">");
					link = rowHtml.replaceAll("(?is).*?<a[^\\\"]+\\\"([^\\\"]+).*", "$1");

					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));


					ro.cst.tsearch.servers.functions.COAdamsTR.partyNamesCOAdamsTR(resultMap, getSearch().getID());
					ro.cst.tsearch.servers.functions.COAdamsTR.parseAddressCOAdamsTR(resultMap, getSearch().getID());
					ro.cst.tsearch.servers.functions.COAdamsTR.parseLegalCOAdamsTR(resultMap, getSearch().getID());

					resultMap.removeTempDef();

					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);

					DocumentI document = (TaxDocumentI) bridge.importData();
					currentResponse.setDocument(document);

					intermediaryResponse.add(currentResponse);
				}
			}

			String header1 = rows[0].toHtml();
			String header0 = "<table>";
			
			header1 = header1.replaceAll("(?is)</?a[^>]*>", "");
			Pattern tablePat = Pattern.compile("<table[^>]+>");
			Matcher mat = tablePat.matcher(table);
			if (mat.find()) {
				header0 = mat.group(0);
			}
			
			StringBuilder footer =  getPrevAndNextLinks (response, htmlParser, "frmSearchResults"); 
			
			response.getParsedResponse().setHeader(header0 + header1);
			response.getParsedResponse().setFooter(footer + "</table>");

			outputTable.append(table);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return intermediaryResponse;
	}
		
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.COAdamsTR.parseAndFillResultMap(detailsHtml, map, searchId, miServerID);
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

}