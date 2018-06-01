/**
 * 
 */
package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.connection.http2.MIGenericOaklandAOTR.COM_PRODUCT;
import static ro.cst.tsearch.connection.http2.MIGenericOaklandAOTR.RES_PRODUCT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.HttpUtils;

/**
 * @author radu bacrau
 *
 */
public class MIOaklandAO extends MIGenericOaklandAOTR {

	private static final long serialVersionUID = -1451976616441247171L;
	
	protected static Pattern detailPattern = Pattern.compile( "(?is)<FORM[ ]*name=\"finditform\".*?</FORM>" );
	protected static Pattern startTableDetailPattern = Pattern.compile( "<TABLE WIDTH=\"[1-9][0-9]*\" BORDER=\"[1-9]+\" ALIGN=\"[^\"]*\" BGCOLOR=\"[^\"]*\">");	
	protected static Pattern parcelIdPatern = Pattern.compile("[0-9]+-[0-9]+-[0-9]+-[0-9]+"); //13-15-127-029

	/**
	 * 
	 * @param searchId
	 */
	public MIOaklandAO(long searchId) {
		super(searchId);
	}
	
	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 */
	public MIOaklandAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink,long searchId,int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,searchId,mid);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo(){
		
		TSServerInfo si = new TSServerInfo(4);
		
		si.setServerAddress("ea2.co.oakland.mi.us");
		si.setServerIP("ea2.co.oakland.mi.us");
		si.setServerLink("http://ea2.co.oakland.mi.us/eap_js/products/paq/findit.cfm" );
		
		setupAddressSearchModule(si, RESID_ADDRESS_MODULE_IDX, "Residential Property Search by Address", RES_PRODUCT + "_1", false,	"/eap_js/products/paq/findit.cfm");		
		setupAddressSearchModule(si, COMM_ADDRESS_MODULE_IDX, "Commercial Property Search by Address", COM_PRODUCT + "_1", false, "/eap_js/products/cipaq/findit.cfm");
		
		setupPinSearchModule(si, RESID_PIN_MODULE_IDX, "Residential Property Search by PIN", RES_PRODUCT + "_2", false,	"/eap_js/products/paq/findit.cfm");
		setupPinSearchModule(si, COMM_PIN_MODULE_IDX, "Commercial Property Search by PIN", COM_PRODUCT + "_2", false, "/eap_js/products/cipaq/findit.cfm");
		
		si.setupParameterAliases();
		setModulesForAutoSearch(si);
		
		return si;
	}
	

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();

		
		// search by parcel number
		if (!"".equals(getSearchAttribute(SearchAttributes.LD_PARCELNO))) {

			// residential
			l.add(new TSServerInfoModule(serverInfo.getModule(RESID_PIN_MODULE_IDX)));

			// industrial
			l.add(new TSServerInfoModule(serverInfo.getModule(COMM_PIN_MODULE_IDX)));
		}

		setAddressAutomaticSearch(serverInfo, l, RESID_ADDRESS_MODULE_IDX);
		setAddressAutomaticSearch(serverInfo, l, COMM_ADDRESS_MODULE_IDX);
	
		serverInfo.setModulesForAutoSearch(l);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponce = Response.getResult();
		String initialResponce = rsResponce;
		String linkStart = CreatePartialLink(TSConnectionURL.idPOST);
		
		if (rsResponce.indexOf("If you use the") > 0 && viParseID != ID_SAVE_TO_TSD) {
			viParseID = ID_DETAILS;
		}

		int start = -1;
		int end = -1;

		switch (viParseID) {

		case ID_SEARCH_BY_PARCEL:
		case ID_SEARCH_BY_ADDRESS:
			
			// no data found
			if (rsResponce.contains("No parcels matching your criteria were found.")) {
				return;
			}
			// no data found
			if (rsResponce.contains("The parcel searched is a commercial and industrial parcel")) {
				return;
			}
			// no data found
			if (rsResponce.contains("The parcel searched is a residential parcel")) {
				return;
			}

			int startUtilData = rsResponce.indexOf("Parcels found:");
			startUtilData = (startUtilData < 0) ? 0 : startUtilData;

			int endUtilData = rsResponce.lastIndexOf("<center>");
			endUtilData = (endUtilData < 0) ? rsResponce.length() : endUtilData;

			if (startUtilData > endUtilData) {
				endUtilData = rsResponce.length();
			} else {
				endUtilData = endUtilData + "<center>".length();
			}

			rsResponce = rsResponce.substring(startUtilData, endUtilData);
			rsResponce = "<center>\n<TABLE>\n<TR>\n<TD>" + rsResponce;

			Matcher resultsMatcher = detailPattern.matcher(rsResponce);
			while (resultsMatcher.find()) {
				String formStr = resultsMatcher.group(0);

				HashMap<String, String> params = HttpUtils.getFormParams(formStr);

				String docLink = "<a href='" + linkStart + "/eap_js/products/paq/paq.cfm&Ua_Id=7557";

				Iterator<String> paramIterator = params.keySet().iterator();
				while (paramIterator.hasNext()) {
					String paramName = paramIterator.next();
					String paramValue = params.get(paramName);
					docLink += "&" + paramName + "=" + paramValue;
				}

				docLink += "'>View</a>";

				rsResponce = rsResponce.replace(formStr, docLink);
			}

			start = rsResponce.indexOf("<TABLE>", 10);
			start = (start < 0) ? 0 : start;

			end = rsResponce.indexOf("</TABLE>") + "</TABLE>".length();
			end = (end < 0) ? rsResponce.length() : end;

			rsResponce = rsResponce.substring(start, end + 1);
			rsResponce = rsResponce.replaceAll("<a", "<td><a");
			rsResponce = rsResponce.replaceAll("</a>", "</a></td>");

			rsResponce = rsResponce.replaceAll("(?is)<TD [^>]*><HR></TD>", "");
			rsResponce = rsResponce.replaceAll("(?is)<TR>\\s+</TR>", "");
			rsResponce = rsResponce.replaceAll("(?is)<scrip.*?</script>", "");

			// call the parser
			parser.Parse(Response.getParsedResponse(), rsResponce, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idPOST),
					TSServer.REQUEST_SAVE_TO_TSD);

			break;
			
		case ID_DETAILS: 
			
			String imageStr = "<a[ ]*href=\"[^\"]*\">[^<]*</a>";

			end = rsResponce.indexOf("<TABLE BORDER=\"0\" ALIGN=\"CENTER\">");
			end = (end < 0) ? rsResponce.length() : end;
			rsResponce = rsResponce.substring(0, end);
			end = rsResponce.lastIndexOf("<HR WIDTH=\"25%\">");
			end = (end < 0) ? rsResponce.length() : end;
			rsResponce = rsResponce.substring(0, end);

			Matcher matchstartTable = MIOaklandAO.startTableDetailPattern.matcher(rsResponce);
			start = 0;
			if (matchstartTable.find()) {
				start = matchstartTable.start();
			}
			rsResponce = rsResponce.substring(start);

			// for now wee don't need the image link
			rsResponce = rsResponce.replaceAll(imageStr, "");

			String keyNumber = getParcelNumber(rsResponce);
			if ((!downloadingForSave)) {

				String originalLink = sAction + "&dummy=" + keyNumber + "&" + Response.getQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

				if (FileAlreadyExist(keyNumber + ".html") ) {
					rsResponce += CreateFileAlreadyInTSD();
				} else {
					rsResponce = addSaveToTsdButton(rsResponce, sSave2TSDLink, viParseID);
					mSearch.addInMemoryDoc(sSave2TSDLink, initialResponce);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), rsResponce, Parser.NO_PARSE);

			} else {
				
				// for html
				msSaveToTSDFileName = keyNumber + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD();
				parser.Parse(Response.getParsedResponse(), rsResponce, Parser.PAGE_DETAILS);
			}
			break;
			
		case ID_SAVE_TO_TSD:
			
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;		
			break;
			
		}

	}
	
	private String getParcelNumber(String document) {
		try {
			if (document == null) {
				return "";
			}
			Matcher mat = parcelIdPatern.matcher(document);

			if (mat.find()) {
				return mat.group();
			}
		} catch (Exception e) {
		}
		return "default";
	}

}
