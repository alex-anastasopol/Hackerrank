package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.connection.http2.MIGenericOaklandAOTR.TAX_PRODUCT;

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
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;

public class MIOaklandTR extends MIGenericOaklandAOTR {

	protected static final long serialVersionUID = -8566121136648703791L;
	public static Pattern detailPattern = Pattern.compile( "(?is)<FORM ACTION=\"taxrec1.cfm\".*?</form>" );

	/**
	 * Constructor
	 * @param searchId
	 */
	public MIOaklandTR(long searchId) {
		super(searchId);
	}

	/**
	 * Constructor
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 */
	public MIOaklandTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@SuppressWarnings("deprecation")
	protected String getFileNameFromLink(String link) {
		String parcelId = org.apache.commons.lang.StringUtils.getNestedString(link, "dummy=", "&");
		return parcelId + ".html";
	}

	@Override
	public TSServerInfo getDefaultServerInfo(){
		
		TSServerInfo si = new TSServerInfo(2);
		
		si.setServerAddress("http://ea2.co.oakland.mi.us");
		si.setServerIP("ea2.co.oakland.mi.us");		
		si.setServerLink("http://ea2.co.oakland.mi.us/eap_js/products/tax_rec/findit.cfm" );
				
		setupAddressSearchModule(si, TAX_ADDRESS_MODULE_IDX, "Tax Property Search by Address", TAX_PRODUCT + "_1", true, "/eap_js/products/tax_rec/findit.cfm");
		setupPinSearchModule    (si, TAX_PIN_MODULE_IDX,     "Tax Property Search by PIN",     TAX_PRODUCT + "_2", true, "/eap_js/products/tax_rec/findit.cfm");
		
		si.setupParameterAliases();
		setModulesForAutoSearch(si);
		
		return si;
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();

		String initialResponse = rsResponse;
		String keyNumber = "";
		String sTmp;

		int istart, iend;

		switch (viParseID) {
		
		case ID_SEARCH_BY_ADDRESS:
		case ID_SEARCH_BY_PARCEL:

			// no data found
			if (rsResponse.contains("No parcels matching your criteria were found.")) {
				return;
			}
			// no data found
			if (rsResponse.contains("You did not select a municipality")) {
				Response.getParsedResponse().setError("You did not select a municipality.");
				return;
			}

			sTmp = CreatePartialLink(TSConnectionURL.idPOST);
			istart = rsResponse.indexOf("Parcels found:");
			istart = rsResponse.indexOf("<TABLE", istart + 1);

			iend = rsResponse.indexOf("</TABLE>", istart);

			if (istart >= 0 && iend >= 0) {
				rsResponse = rsResponse.substring(istart, iend + 8);
			}
			Matcher resultsMatcher = detailPattern.matcher( rsResponse );
			while( resultsMatcher.find() ){
				String formStr = resultsMatcher.group( 0 );
				
				HashMap<String, String> params = HttpUtils.getFormParams( formStr );
				
				String docLink = "<TD><a href='" + sTmp + "/eap_js/products/tax_rec/taxrec1.cfm";
				
				Iterator<String> paramIterator = params.keySet().iterator();
				while( paramIterator.hasNext() ){
					String paramName = paramIterator.next();
					String paramValue = params.get( paramName );					
					docLink += "&" + paramName + "=" + paramValue;
				}
				
				docLink += "&purchase.x=44&purchase.y=4'>View</a></TD>";
				rsResponse = rsResponse.replace( formStr , docLink);
			}

			rsResponse = rsResponse.replaceAll("(?is)<TD [^>]*><HR></TD>", "");
			rsResponse = rsResponse.replaceAll("(?is)<TR>\\s+</TR>", "");

			parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idPOST),
					TSServer.REQUEST_SAVE_TO_TSD);

			break;
			
		case ID_DETAILS:

			// no data found
			if (rsResponse.contains("Current tax information is not available.")) {
				Response.getParsedResponse().setError("Current tax information is not available.");
				SearchLogger.info("<br/><span class='error'>Error appeared: Current tax information is not available!" + "</span>", searchId);
				return;
			}
			// no data found
			if (rsResponse.contains("You did not select a municipality")) {
				Response.getParsedResponse().setError("You did not select a municipality.");
				return;
			}
			// remove the village taxes link
			rsResponse = rsResponse.replaceAll("(?si)Please <A HREF=\"taxrecv\\.cfm\\?user_id=[^>]+>click here</A> to view the Village Taxes", "");

			sTmp = CreatePartialLink(TSConnectionURL.idPOST);

			istart = rsResponse.indexOf("buttons.");
			istart = rsResponse.indexOf("<TABLE", istart + 1);

			iend = rsResponse.lastIndexOf("<TABLE WIDTH=\"760\" BORDER=\"0\" ALIGN=\"CENTER\">");
			iend = rsResponse.lastIndexOf("</TABLE>", iend);

			if (istart < 0 && iend < 0) {
				return;
			}

			rsResponse = rsResponse.substring(istart, iend + 8);

			//get parcel id

			istart = rsResponse.indexOf("Parcel ID");
			istart = rsResponse.indexOf("<TD", istart + 1);
			istart = rsResponse.indexOf(">", istart + 1);
			iend = rsResponse.indexOf("</", istart + 1);

			if (istart >= 0 && iend >= 0) {
				keyNumber = rsResponse.substring(istart + 1, iend);
				keyNumber = keyNumber.replaceAll("-", "");
			}

			if (!downloadingForSave) {
				//not saving to TSR

				String qry = Response.getQuerry();
				qry = "dummy=" + keyNumber + "&" + qry;
				Response.setQuerry(qry);
				String originalLink = sAction + "&" + qry;
				String sSave2TSDLink = CreatePartialLink(TSConnectionURL.idPOST) + originalLink;

				if (FileAlreadyExist(keyNumber + ".html") ) {
					rsResponse += CreateFileAlreadyInTSD();
				} else {
					rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);
					mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
				
			} else {

				//saving
				msSaveToTSDFileName = keyNumber + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();

				// HTML correction for docs with condominium
				rsResponse = rsResponse
						.replaceAll(
								"(?s)(<table width=100% cellspacing=0 cellpadding=0 border=1>.?<tr><td  colspan=6><b>Condominium Description Sequence #([2-9]|1\\d))",
								"</table>$1");
				rsResponse = Tidy.tidyParse(rsResponse, null);

				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idGET),
						TSServer.REQUEST_SAVE_TO_TSD);
			}
			break;
			
		case ID_SAVE_TO_TSD:
			
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;
			break;
			
		case ID_GET_LINK:
			ParseResponse(sAction, Response, ID_DETAILS);
			break;
		}
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();

		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		String cityCode = getCityCode();

		// search by PIN
		if (!StringUtils.isEmpty(pin) && !StringUtils.isEmpty(cityCode)) {		
			TSServerInfoModule m = new TSServerInfoModule(serverInfo.getModule(TAX_PIN_MODULE_IDX));
			m.getFunction(2).forceValue(cityCode);
			l.add(m);
		}
		
		// search by address
		setAddressAutomaticSearch(serverInfo, l, TAX_ADDRESS_MODULE_IDX);

		// use list
		serverInfo.setModulesForAutoSearch(l);
	}

}
