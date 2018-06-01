package ro.cst.tsearch.servers.types;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;


/**
 * @author mihaib
  */

public class COBroomfieldTR extends TSServer {

	public static final long serialVersionUID = 10000000L;
	private boolean downloadingForSave; 

	private static final Pattern dummyPattern = Pattern.compile("&dummy=([0-9]+)&");
	
	
	public COBroomfieldTR(long searchId) {
		super(searchId);
	}

	public COBroomfieldTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;
		String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
		String pid = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);

		if (hasPin()) {
			//Search by Pin
			if (pid.matches("[A-Z]\\d{7}")){
				String acctType = pid.replaceAll("([A-Z])(\\d{7})", "$1");
				pid = pid.replaceAll("([A-Z])(\\d{7})", "$2");
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				module.clearSaKeys();
				module.setData(0, pid);
				module.setData(1, acctType);
				modules.add(module);
			} else {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.getFunction(0).forceValue(getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO));
				modules.add(module);
			}
		}
		
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(streetNo);
			module.getFunction(2).forceValue(streetName);
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
			case ID_SEARCH_BY_ADDRESS :
				
				if (rsResponse.indexOf("No Record Found For") != -1){
					Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
					return;
				}
				
				if (rsResponse.indexOf("Tax Assessment Search Results") != -1){
					ParseResponse(sAction, Response, ID_DETAILS);
					break;
				}
				
				break;
				
			case ID_DETAILS :
				
				String details = "";
				try {
					org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(rsResponse, null);
					NodeList tables = parser.extractAllNodesThatMatch(new TagNameFilter("table"));
					details = tables.elementAt(1).toHtml();

				} catch (Exception e) {
					e.printStackTrace();
				}	
				
				details = getDetails(details);
				
				String pid = "";
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
					NodeList mainList = htmlParser.parse(null);
					pid = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "Tax Account Number"),"", true).trim();
					pid = pid.replaceAll("</?font[^>]*>", "").trim();
				} catch(Exception e) {
					e.printStackTrace();
				}
				
				if ((!downloadingForSave))
				{	
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
	            } 
				else 
	            {      
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = pid + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();                
				}
				
				break;	
			
			case ID_GET_LINK :
				if (rsResponse.indexOf("General Parcel Information") != -1){
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
		
		contents = contents.replaceAll("(?is)</?a[^>]*>", "");
		contents = contents.replaceAll("(?is)<img[^>]*>", "");
		contents = contents.replaceAll("(?is)</?form[^>]*>", "");
		contents = contents.replaceAll("(?is)<input[^>]+>", "");
		contents = contents.replaceAll("(?is)bgcolor\\s*=\\s*\\\"[^\\\"]+\\\"", "");
		contents = contents.replaceAll("(?is)\\bcolor\\s*=\\s*\\\"[^\\\"]+\\\"", " color=\"#000000\"");
		contents = contents.replaceAll("</td>\\s*<tr>", "</td></tr><tr>");
						
		return contents;
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.COBroomfieldTR.parseAndFillResultMap(detailsHtml, map, searchId, miServerID);
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