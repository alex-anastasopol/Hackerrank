package ro.cst.tsearch.servers.types;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.htmlparser.util.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;

import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class CODenverTR extends TSServer{

	static final long serialVersionUID = 10000000;

	private boolean downloadingForSave; 

	public CODenverTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	public CODenverTR(long searchId){
		super(searchId);
	}
	
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException
    {
		String pin;
		int ServerInfoModuleID = module.getModuleIdx();

		if (ServerInfoModuleID == 2) //search by pin 
		{
			pin = module.getFunction(0).getParamValue();
			if (pin != null) {
				pin = pin.replaceAll("\\p{Punct}", "");
				module.getFunction(0).setParamValue(pin);
			}
		}
		return super.SearchBy(module, sd);
    }
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		String streetName = getSearchAttribute(SearchAttributes.P_STREETNAME);
		String streetNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHybridFilter( searchId , 0.8d );
		
		if (!pin.isEmpty()){//Search by PINs (Parcel Numbers)
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			modules.add(module);
		}
		
		if (hasStreet()) {
			//Search by Property Address
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			GenericNameFilter defaultNameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , module );
			defaultNameFilter.setUseSynonymsForCandidates(false);
			module.clearSaKeys();
			module.addFilter(addressFilter);
			module.addFilter(defaultNameFilter);
			module.setData( 0 , streetNo );
			module.setData( 3 , streetName );
			modules.add(module);
		}
		
	    serverInfo.setModulesForAutoSearch(modules);
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
				"<tr>",
				"</tr>",
				linkStart,
				action);
		}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		String rsResponse = initialResponse;
		String contents = "";
		ParsedResponse parsedResponse = Response.getParsedResponse();	
		String linkStart = CreatePartialLink(TSConnectionURL.idGET);
		
		if (rsResponse.indexOf("We're sorry, but the system is unable to locate this property information") != -1){
			Response.getParsedResponse().setError("<font color=\"red\">No results found</font>");
			return;
		}
		
		switch (viParseID) {
		
			case ID_SEARCH_BY_ADDRESS :
				if (rsResponse.indexOf("Select a name from the list") == -1){
					ParseResponse(sAction, Response, ID_DETAILS);
					return;
				}
				
				if (rsResponse.indexOf("<form") != -1){	
					contents = rsResponse.substring(rsResponse.indexOf("<form"), rsResponse.length());
					contents = contents.replaceAll("(?is)</?form[^>]*>", "");
					contents = contents.replaceAll("(?is)<table[^>]+>\\s*<tr>\\s*<td>.*?</table>", "");
					contents = contents.replaceAll("(?is)<input.*", "");
					contents = contents.replaceAll("(?is)(<a\\s+href\\s*=\\s*\\\")([^\\\"]+)", "$1" + linkStart + "/apps/treasurypt/$2");
				}
				
			   
			   parser.Parse(parsedResponse, contents, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);

				break;
				
			case ID_SEARCH_BY_PARCEL:	
			case ID_DETAILS :
				
				String details = "";
				details = getDetails(rsResponse);
				HtmlParser3 htmlParser3 = new HtmlParser3(details);
				 NodeList nodeList = htmlParser3.getNodeList();
				 String pid = "";
				 if (nodeList.size() > 0) {
					 pid = HtmlParser3.getValueFromAbsoluteCell(0, 1, HtmlParser3.findNode(nodeList, "PARCEL NUMBER"), "", true).trim();
				 }
				 
				if ((!downloadingForSave))
				{	
	                String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + pid + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","CNTYTAX");
					data.put("dataSource", "TR");
					
					if(isInstrumentSaved(pid, null, data))
					{
	                	details += CreateFileAlreadyInTSD();
					}
					else 
					{
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
					//smartParseDetails(Response, details);
	                msSaveToTSDFileName = pid + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();                
	                parser.Parse(Response.getParsedResponse(), details, Parser.PAGE_DETAILS);	 

				}
				
				break;	
			
			case ID_GET_LINK :
				if (rsResponse.indexOf("Installment") != -1){
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
	
	protected String getDetails(String response){
		
		// if from memory - use it as is
		if(!(response.contains("<html") || response.contains("<HTML"))){
			return response;
		}
	
		String details = "";
		
		details = response.replaceAll("(?is).*?(<table[^>]+>\\s*<tr>\\s*<td>.*?)<script\\s+language.*", "$1");
		details = details.replaceAll("(?is)<img[^>]+>", "");
		details = details.replaceAll("(?is)<a[^>]+>", "");
		details = details.replaceAll("(?is)</a>", "");
		details = details.replaceAll("(?is)&amp;", "&");
		details = details.replaceAll("(?is)Please[^<]+<[^<]+", "");
		details = details.replaceAll("(?is)Link\\s*To\\s*Assessor", "");

				
		return details;
	}

}
