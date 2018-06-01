package ro.cst.tsearch.servers.types;


import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;


/**
 * @author mihaib
  */

@SuppressWarnings("deprecation")
public class COAdamsAO extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 
	private static String STREET_NAME_SELECT = "";

	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
	static {
		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if(!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		try {
			STREET_NAME_SELECT = FileUtils.readFileToString(new File(folderPath + File.separator + "COAdamsAOStreetList.xml"));
			
		} catch (Exception e) {
			e.printStackTrace();	
		}
	}
	
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
	
	public COAdamsAO(long searchId) {
		super(searchId);
	}

	public COAdamsAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d , true);
		
		String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
		
		String pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);
		
		if (hasPin()) {
			//Search by Pin
			if (pid.matches("[A-Z]\\d{7}")){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.clearSaKeys();
				module.setData(0, pid);
				modules.add(module);
			} else {
				if (pid.length() == 12){
					pid = "0" + pid;
				}
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(0).forceValue(getSearch().getSa().getAtribute(SearchAttributes.LD_CO_PARCEL_NO));
				modules.add(module);
			}
		}
		
		if (hasStreet()) {
			//Search by Property Address
			FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(streetNo);
			module.getFunction(1).forceValue(streetNo);
			module.getFunction(2).forceValue(streetName.toUpperCase());
			module.addFilter(addressFilter);
			module.addFilter(nameFilterHybridDoNotSkipUnique);
			modules.add(module);
		}
		
		
		serverInfo.setModulesForAutoSearch(modules);		
	}
	
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;

		switch (viParseID) {		
			case ID_SEARCH_BY_NAME :
				
				if (rsResponse.indexOf("there are no data records to display") != -1) {
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				} else if (rsResponse.indexOf("Invalid postback or callback argument") != -1) {
					Response.getParsedResponse().setWarning("<font color=\"black\">Wrong search criteria used to search with</font>");
					return;
				}
				
				try {
					 
					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();
					 
					String table = "";
					HtmlParser3 parser = new HtmlParser3(rsResponse);
					table = parser.getNodeById("ctl00_ContentPlaceHolder_QuickSearchResultsDisplay").toHtml();
													
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
//				String pid = "";
				String accountNo = "";
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
					NodeList mainList = htmlParser.parse(null);
//					TableColumn tc = (TableColumn) HtmlParser3.findNode(mainList, "Parcel Number").getParent().getParent();
//					pid = HtmlParser3.getValueFromCell(tc, ">(\\d{6,})<", true);
//					pid = pid.replaceAll("</?font[^>]*>", "").replaceAll("</?b>", "").trim();
					
					TableColumn tc = (TableColumn) HtmlParser3.findNode(mainList, "Account Number").getParent().getParent();
					accountNo = HtmlParser3.getValueFromCell(tc, ">([RPM]\\d{6,})<", true);
					accountNo = accountNo.replaceAll("</?font[^>]*>", "").replaceAll("</?b>", "").trim();
					
				} catch(Exception e) {
					e.printStackTrace();
				}

				if ((!downloadingForSave))
				{	
	                String qry_aux = Response.getRawQuerry();
//					qry_aux = "dummy=" + pid + "&" + qry_aux;
	                qry_aux = "dummy=" + accountNo + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","ASSESSOR");
					
//					if(isInstrumentSaved(pid, null, data)){
					if(isInstrumentSaved(accountNo, null, data)) {
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
	            } 
				else 
	            {      
					smartParseDetails(Response, details);
//	                msSaveToTSDFileName = pid + ".html";
	                msSaveToTSDFileName = accountNo  + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();                
				}
				
				break;	
			
			case ID_GET_LINK :
				if (sAction.indexOf("doreport.asp") != -1){
						ParseResponse(sAction, Response, ID_DETAILS);
				} 
				
				break;
			
			case ID_SAVE_TO_TSD :

				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;
		}
	}
	
	protected String getDetails(String contents){
		
		String details = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(contents, null);
			NodeList mainList = htmlParser.parse(null);
			
			details = HtmlParser3.getNodeByID("propertyReport", mainList, true).toHtml();

		} catch (Exception e) {
			e.printStackTrace();
		}

		details = details.replaceAll("(?is)</?form[^>]*>", "");
		details = details.replaceAll("(?is)<p\\s+class.*?</p>", "");
		details = details.replaceAll("(?is)</?body[^>]*>", "");
		details = details.replaceAll("(?is)</?a[^>]*>", "");
		//details = details.replaceAll("(?is)<b>Legal Disclaimer\\s*:\\s*</b>[^<]+", "");
		details = details.replaceAll("(?is)<span[^>]*>\\s*Legal\\s*Disclaimer\\s*:\\s*</span>\\s*<span[^>]*>[^<]*</span>", "");
		details = details.replaceAll("(?is)<b>Note\\s*:\\s*</b>[^<]+", "");
		details = details.replaceAll("(?is)<span[^>]*>\\s*Note\\s*:\\s*</span>\\s*<span[^>]*>[^<]*</span>", "");
		details = details.replaceAll("(?is)<b>All\\s+Credit\\s+Card.*?</b>", "");
		details = details.replaceAll("(?is)<input[^>]*>", "");
		details = details.replaceAll("(?is)<span[^>]*>\\s*<img[^>]*>[^<]*</span>", "");
		details = details.replaceFirst("(?is)<\\s*span\\s*>\\s*<\\s*hr\\s*/?\\s*>\\s*</span>\\s*<\\s*span\\s*>\\s*(?:<\\s*/?\\s*br\\s*/?\\s*>\\s*)*</span>\\s*"
				+ "<span>\\s*<div[^>]+>\\s*<span[^>]+>\\s*Please note\\s*:\\s*</span>\\s*[^/]+/span>\\s*</div>\\s*</span>", "");
		details = details.replaceFirst("(?is)\\s*<\\s*span\\s*>\\s*(?:<\\s*/?\\s*br\\s*/?\\s*>\\s*)*</span>\\s*<span>\\s*<div[^>]+>\\s*"
				+ "<span[^>]+>\\s*Click\\s*</span>\\s*[^/]+/span>\\s*</div>\\s*</span>", "");
		
		return details;
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList mainTableList = htmlParser.parse(null);
			TableTag mainTable = (TableTag)mainTableList.elementAt(0);
				
			TableRow[] rows = mainTable.getRows();
			for(TableRow row : rows ) {
				if(row.getColumnCount() > 0) {
					
					TableColumn[] cols = row.getColumns();
					String parcelNo = cols[0].getChild(0).getFirstChild().getText();
					String ownerName = cols[1].getChildrenHTML().trim().replaceAll("</?span[^>]*>", "");
					if (parcelNo.length() == 8){
						ownerName = ownerName.replaceAll("(?is)(\\w+\\s+\\w+\\s+(:?\\w\\s+)?)(\\w+\\s+\\w+(?:\\s+\\w\\s+)?)$", "$1 AND $2");
					}
					String address = cols[2].getChildrenHTML().trim().replaceAll("</?span[^>]*>", "");
								
					String rowHtml =  row.toHtml().replaceFirst("<a\\s*href=\\\"([^\\\"]+)[^>]+>",
										"<a href=\"" + CreatePartialLink(TSConnectionURL.idGET) + "/quicksearch/$1\">" ).replaceAll("&amp;", "&");
					String link = rowHtml.replaceAll("(?is).*?<a[^\\\"]+\\\"([^\\\"]+).*", "$1");
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap resultMap = new ResultMap();
					resultMap.put("OtherInformationSet.SrcType", "AO");
					resultMap.put("PropertyIdentificationSet.ParcelID", parcelNo);
					resultMap.put("tmpOwner", ownerName);
					resultMap.put("tmpAddress", address);
					
					ro.cst.tsearch.servers.functions.COAdamsAO.partyNamesCOAdamsAO(resultMap, getSearch().getID());
					ro.cst.tsearch.servers.functions.COAdamsAO.parseAddressCOAdamsAO(resultMap, getSearch().getID());
					
					resultMap.removeTempDef();
					
					Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
					
					DocumentI document = (AssessorDocumentI) bridge.importData();				
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
		response.getParsedResponse().setHeader(header0 + header1);
		response.getParsedResponse().setFooter("</table>");
		
		outputTable.append(table);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
		
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.COAdamsAO.parseAndFillResultMap(detailsHtml, map, searchId, getDataSite().getCountyId());
		return null;
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX);
			
		if(tsServerInfoModule != null) {
            setupSelectBox(tsServerInfoModule.getFunction(2), STREET_NAME_SELECT);
            tsServerInfoModule.getFunction(2).setRequired(true);
		}
		
		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
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