/*
 * Created on Jun 27, 2005
 *
 */
package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectNonRealEstate;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.Ostermiller.util.Base64;
import com.stewart.ats.tsrindex.server.UtilForGwtServer;
public class MOJacksonTR extends TSServer {

	private static final long serialVersionUID = 1L;

	public static final Pattern addresNoPattern = Pattern
			.compile("(\\d+)\\s+(\\w+)");

	public static final Pattern pidPattern = Pattern
//			.compile("(?is)Parcel Number.*?<span[^>]*>(.*?)</span>");
			.compile("(?is).*<span[^>]*>Parcel Number.*?<span[^>]*>(.*?)</span>.*");

	public static final String startDelim = "<table";

	public static final String endDelim = "</table>";


	private boolean downloadingForSave;

	public MOJacksonTR(long searchId) {
		super(searchId);
	}

	public MOJacksonTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	public ServerResponse SearchBy(TSServerInfoModule module,
			Object sd) throws ServerResponseException {
		int serverInfoModuleID = module.getModuleIdx();

		if (serverInfoModuleID == TSServerInfo.NAME_MODULE_IDX
				&& InstanceManager.getManager().getCurrentInstance(searchId)
						.getCrtSearchContext().getSearchType() == Search.AUTOMATIC_SEARCH) {
			// set the * needed for automatic search at the end of the name
			// field
			// module.getFunction(1).setParamValue(module.getFunction(1).getParamValue() + "*");
			
			String last = module.getFunction(0).getParamValue();
			String first = module.getFunction(1).getParamValue();
			String name = (last + " " + first + "*").trim();			
			module.getFunction(1).setParamValue(name);			
			module.removeFunction(0);
		}
		
		if (serverInfoModuleID == TSServerInfo.PARCEL_ID_MODULE_IDX) {
			
			String parcelID = module.getFunction(0).getParamValue();
			if (!parcelID.matches("\\d{2,2}-\\d{3,3}-\\d{2,2}-\\d{2,2}-\\d{2,2}-\\d{1,1}-\\d{2,2}-\\d{3,3}")&&parcelID.length()==17){
				parcelID = parcelID.replaceAll("(\\d{2,2})(\\d{3,3})(\\d{2,2})(\\d{2,2})(\\d{2,2})(\\d{1,1})(\\d{2,2})(\\d{3,3})", "$1-$2-$3-$4-$5-$6-$7-$8");
//				sa.setAtribute(SearchAttributes.LD_PARCELNO, parcelID);
			}
			module.getFunction(0).setParamValue(parcelID);			
		}

		return super.SearchBy(module, sd);
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m = null;
		
		FilterResponse cityFilter 		= CityFilterFactory.getCityFilter(searchId, 0.6d);
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.7d );
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		FilterResponse nameFilterHybridDoNotSkipUnique = null;
		
		RejectNonRealEstate propertyTypeFilter = new RejectNonRealEstate(searchId);
		propertyTypeFilter.setThreshold(new BigDecimal("0.95"));
		
		Search s = InstanceManager.getManager().getCurrentInstance(searchId)
				.getCrtSearchContext();
		SearchAttributes sa = s.getSa();			

		String streetNo = sa.getAtribute(SearchAttributes.P_STREETNO);
		String streetName = sa.getAtribute(SearchAttributes.P_STREETNAME);

		// search by parcel ID
		if (hasPin()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			l.add(m);
		}

		// search by property address
		// <Street#><*><Street name><*>
		if (hasStreet()|| hasStreetNo()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			m.getFunction(0).setSaKey("");
			m.getFunction(1).setSaKey("");
			m.getFunction(1).setParamValue(streetNo + "* " + streetName + "*");
			m.addFilter(propertyTypeFilter);
			m.addFilter(cityFilter);
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			m.addValidator(defaultLegalValidator);
			l.add(m);
		}

		// search by owner name
		if (hasOwner()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			nameFilterHybridDoNotSkipUnique = 
            	NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , m );
            nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
            
            m.addFilter(propertyTypeFilter);
			m.addFilter(cityFilter);
			m.addFilter(nameFilterHybridDoNotSkipUnique);
			m.addFilter(addressFilter);
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(m, searchId, new String[] {"L;F;", "L;f;", "L;M;", "L;m;"});
			m.addIterator(nameIterator);
			
			l.add(m);
			
		}

		serverInfo.setModulesForAutoSearch(l);
	}

	protected String getFileNameFromLink(String link) {
		String parcelId = org.apache.commons.lang.StringUtils.substringBetween(
				link, "dummy=", "&");
		parcelId = parcelId.replaceAll("-", "");
		return parcelId + ".html";
	}

	@SuppressWarnings("rawtypes")
	protected void ParseResponse(String sAction, ServerResponse Response,
			int viParseID) throws ServerResponseException {

		String sTmp = "";
		String rsResponce = Response.getResult();
		//String initialResponse = rsResponce;

		int istart = -1, iend = -1;

		rsResponce = rsResponce.replaceAll("<TR>", "<tr>");
		rsResponce = rsResponce.replaceAll("</TR>", "</tr>");
		rsResponce = rsResponce.replaceAll("<TABLE>", "<table>");
		rsResponce = rsResponce.replaceAll("</TABLE>", "</table>");

		sTmp = CreatePartialLink(TSConnectionURL.idPOST);

		if (rsResponce.indexOf("system is currently unavailable") >= 0) {
			// system error --> return
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:

			// Parcel Number

			Map params = parseIntermPage(rsResponce);
			String allParams = "";
			mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsDetails:", params);
			try {
				allParams = "ASCENDWEB_SESSION_CODE="
						+ params.get("ASCENDWEB_SESSION_CODE") + "&"
						+ "__POST_BACK_VARIABLES_DATA="
						+ params.get("__POST_BACK_VARIABLES_DATA") + "&"
						+ "__VIEWSTATE=" /* + params.get( "__VIEWSTATE" ) */;
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (rsResponce
					.indexOf(" 0 records returned from your search input") >= 0) {
				return;
			}

			try {
				istart = rsResponce.indexOf("Parcel Number");
				istart = rsResponce.lastIndexOf(startDelim, istart);

				iend = rsResponce.indexOf(endDelim, istart);
				if (iend >= 0) {
					iend += endDelim.length();
				}

				rsResponce = rsResponce.substring(istart, iend);

			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

			rsResponce = rsResponce
					.replaceAll(
							"(?i)<a href=\"javascript:__doPostBack\\('(mResultscontrol:mGrid)','parcel_number=([^']*)'\\)\" target=\"_self\">",
							"<a href=\""
									+ sTmp
									+ "/ascend/result.aspx&__EVENTTARGET=mResultscontrol:mGrid&__EVENTARGUMENT=parcel_number=$2&dummy=$2&"
									+ allParams + "\">");

			/*
			 * rsResponce = rsResponce.replaceAll( "(?i)<a
			 * href=\"javascript:__doPostBack\\('(mResultscontrol:mGrid)','parcel_number=([^']*)'\\)\"
			 * target=\"_self\">" , "<input type=\"radio\" name=\"docLink\"
			 * value=\"" + sTmp +
			 * "/ascend/result.aspx&__EVENTTARGET=mResultscontrol:mGrid&__EVENTARGUMENT=parcel_number=$2&dummy=$2&" +
			 * allParams + "\"\"><a href=\"" + sTmp +
			 * "/ascend/result.aspx&__EVENTTARGET=mResultscontrol:mGrid&__EVENTARGUMENT=parcel_number=$2&dummy=$2&" +
			 * allParams + "\">");
			 * 
			 * rsResponce =
			 * CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION,
			 * "POST") + rsResponce + "<br>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL);
			 */
			
			rsResponce = rsResponce.replaceAll("(?is)<a[^>]+>\\s*(\\d{9})\\s*</a>","$1");
			
			parser.Parse(Response.getParsedResponse(), rsResponce,
					Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idPOST),
					TSServer.REQUEST_SAVE_TO_TSD);
			
			break;
		case ID_DETAILS:
		case ID_SEARCH_BY_PARCEL:
			
			Pattern compile = Pattern.compile("(?is)The\\s+Parcel\\s*ID[-\\[\\]\\s\\d]*+does\\s+not\\s+exist.\\s+Please\\s+type\\s+another\\s+one\\s*");
			Matcher matcher = compile.matcher(rsResponce);
			if (matcher.find() && matcher.find()) {
				return;
			}
			String receiptNo = "";
			String calculatedTaxes = "";

			Matcher pidMatcher = pidPattern.matcher(rsResponce);

			// if we have a PID, do a search for the calculated taxes
			 if(pidMatcher.find()){
       		  	receiptNo = pidMatcher.group(1);
		        
       		  	//don't make all the requests once again if we have the doc in memory
       		  	if (rsResponce.toLowerCase().contains("<html")){
					try {
						HTTPRequest req = new HTTPRequest(
								"https://ascendweb.jacksongov.org/ascend/injected/TaxesBalancePaymentCalculator.aspx?parcel_number="
										+ receiptNo/* 03-800-03-36-00-0-00-000 */);
	
						HTTPResponse res = null;
			        	HttpSite site = HttpManager.getSite(HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName(), searchId);
						try
						{
							res = site.process(req);
						} finally 
						{
							HttpManager.releaseSite(site);
						}
	
						calculatedTaxes = res.getResponseAsString();
	
						calculatedTaxes = StringUtils.getTextBetweenDelimiters(
								"decode64( '", "'", calculatedTaxes);
						calculatedTaxes = Base64.decode(calculatedTaxes);
	
						calculatedTaxes = calculatedTaxes.replace("$", "\\$");
						calculatedTaxes = calculatedTaxes
								.replaceAll(
										"(?is)<!--DONT_PRINT_START-->.*?<!--DONT_PRINT_FINISH-->",
										"");
						calculatedTaxes = calculatedTaxes.replaceAll(
								"(?is)<td[^>]*>Select to Pay</td>", "");
						calculatedTaxes = calculatedTaxes.replaceAll(
								"(?is)<td[^>]*><input type=.*?</td>", "");
					} catch (Exception e) {
	
					}
	
					//receiptNo = receiptNo.replaceAll("-", "");
	
					// If no results found by parcel id, returns
					if (receiptNo.equals(""))
						return;
		
					rsResponce = rsResponce.replaceAll(
							"(?is)<TD id=\"TaxesBalancePaymentCalculator\">.*?</TD>",
							"<TD id=\"TaxesBalancePaymentCalculator\">"
									+ calculatedTaxes + "</TD>");
		
					try {
						istart = rsResponce.indexOf("Property Account Summary");
						istart = rsResponce.lastIndexOf("<tr", istart);
		
						iend = rsResponce.indexOf("MainFooter", istart);
						iend = rsResponce.lastIndexOf("</tr>", iend);
						if (iend >= 0) {
							iend += 5;
						}
		
						rsResponce = rsResponce.substring(istart, iend);
					} catch (Exception e) {
						e.printStackTrace();
					}
		
					rsResponce = addReceipts(Response.getResult(), rsResponce);
					
					rsResponce = rsResponce.replaceAll("(?i)<a [^>]*>(.*?)</a>", "$1");
		
					rsResponce = rsResponce.replaceAll("(?is)<script .*?</script>", "");
       		  	}
			 }

			if ((!downloadingForSave)) {
				String qry = Response.getQuerry();
				qry = "dummy=" + receiptNo + "&" + qry;
				String originalLink = sAction + "&" + qry;
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST)
						+ originalLink;
				
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","CNTYTAX");
				
				if(isInstrumentSaved(receiptNo, null, data)){
					rsResponce += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, rsResponce);
					rsResponce = addSaveToTsdButton(rsResponce, sSave2TSDLink, viParseID);
				}
				Response.getParsedResponse().setPageLink(
						new LinkInPage(sSave2TSDLink, originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), rsResponce,
						Parser.NO_PARSE);
			} else {
				
				rsResponce = rsResponce.replaceAll("(?is)<span[^>]*>", "");
				rsResponce = rsResponce.replaceAll("(?is)</span>", "");
				// for html
				msSaveToTSDFileName = receiptNo + ".html";
				Response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD();
				parser.Parse(Response.getParsedResponse(), rsResponce,
						Parser.PAGE_DETAILS);

			}
			break;
		case ID_GET_LINK:
			if (sAction.indexOf("/result.aspx") >= 0)
				ParseResponse(sAction, Response, ID_DETAILS);

			break;
		case ID_SAVE_TO_TSD:
			// on save
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;

			break;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map parseIntermPage(String s) {
		Map rez = new HashMap();
		try {
			String tmp = ro.cst.tsearch.utils.StringUtils
					.getTextBetweenDelimiters(
							"<input type=\"hidden\" name=\"ASCENDWEB_SESSION_CODE\" value=\"",
							"\"", s);
			rez.put("ASCENDWEB_SESSION_CODE", URLEncoder.encode(tmp, "UTF-8"));

			tmp = ro.cst.tsearch.utils.StringUtils
					.getTextBetweenDelimiters(
							"<input type=\"hidden\" name=\"__POST_BACK_VARIABLES_DATA\" value=\"",
							"\"", s);
			rez.put("__POST_BACK_VARIABLES_DATA", tmp);

			tmp = ro.cst.tsearch.utils.StringUtils.getTextBetweenDelimiters(
					"<input type=\"hidden\" name=\"__VIEWSTATE\" value=\"",
					"\"", s);
			rez.put("__VIEWSTATE", tmp);
		} catch (Exception e) {

		}
		return rez;

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void splitResultRows(Parser p, ParsedResponse pr,
			String htmlString, int pageId, String linkStart, int action,
			long searchId)
			throws ro.cst.tsearch.exceptions.ServerResponseException {

		p.splitResultRows(pr, htmlString, pageId, "<tr", "</table>", linkStart,
				action);
		
		// remove table header and rows with subdivision with no -
		Vector rows = pr.getResultRows();

		boolean deleted = false;
		rows = pr.getResultRows();
		try {
			for (int i = 0; i < rows.size(); i++) {
				ParsedResponse row = (ParsedResponse) rows.get(i);
				PropertyIdentificationSet data = row
						.getPropertyIdentificationSet(0);
				String pid = data.getAtribute("ParcelID");
				if (pid.indexOf("-") == -1 && pid.length() != 9) {
					rows.setElementAt(null, i);
					deleted = true;
				} else {
					String lnk = row.getResponse();
					Pattern pp = Pattern.compile("<a href=\"(.*?)\">");
					Matcher m = pp.matcher(lnk);
					if (m.find()) 
						lnk = m.group(1);
					if (pid.length() == 9) {
						data.setAtribute("PropertyType", "Personal Property");
					} else {
						data.setAtribute("PropertyType", "Real Estate");
					}
					row.setPageLink(new LinkInPage(lnk, lnk, TSServer.REQUEST_SAVE_TO_TSD));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (deleted) {
			while (rows.contains(null)) {
				for (int i = 0; i < rows.size(); i++)
					if (rows.get(i) == null)
						rows.remove(i);
			}
		}
		
		if (rows.size() == 0
				&& !"".equals(pr.getNextLink())
				&& InstanceManager.getManager().getCurrentInstance(searchId)
						.getCrtSearchContext().getSearchType() == Search.PARENT_SITE_SEARCH) {
			/*
			 * if we have a next link, but we don't have any valid result
			 * 
			 * instruct the user to go to next page
			 */

			ParsedResponse goToNextPage = new ParsedResponse();
			goToNextPage
					.setOnlyResponse("<tr><td><BR><font style=\"font-size:20px; color:red\"><B>This page does not contain any real estate properties. Please go to next page.</B></font><BR><BR><BR></td></tr>");
			pr.setHeader("<table>");
			String footer = pr.getFooter();
			// footer = footer.replaceAll("</table>", "");
			pr.setFooter(footer);

			rows.add(goToNextPage);

		}

		pr.setResultRows(rows);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String addReceipts(String fullResponse, String response) {
		StringBuilder newResponse = new StringBuilder(response);
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "mTabGroup_Receipts_mReceipts_mGrid_RealDataGrid"));
			if (tableList.size()>0) {
				String link = "https://ascendweb.jacksongov.org/ascend/parcelinfo.aspx";
				String table = tableList.toHtml();
				Map params = parseIntermPage(fullResponse);
				params.put("__EVENTTARGET", "mTabGroup:Receipts:mReceipts:mGrid");
				Matcher matcher = Pattern.compile("(?is)<a.*?href=\"javascript:__doPostBack\\('mTabGroup:Receipts:mReceipts:mGrid','([^']*)'\\)\"")
					.matcher(table);
				while (matcher.find()) {
					params.put("__EVENTARGUMENT", matcher.group(1));
					mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsReceipts:", params);
					
					String result = getLinkContents(link);
					result = extractTable(result);
					
					newResponse.append(result);
				}
			}
		} catch (Exception e) {
			logger.error("Error while getting receipts", e);
		}
		
		return newResponse.toString();
	}
	
	public String extractTable(String result) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(result, null);
			NodeList nodeList = htmlParser.parse(null);
			
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "mPanelTable"));
			if (tableList.size()>0) {
				String content = tableList.elementAt(0).toHtml();
				content = content.replaceFirst("Official Tax Payment Receipt", "<b>$0</b>");
				return content; 
			}
		} catch (Exception e) {
			logger.error("Error while getting receipts", e);
		}
		
		return "";
	}
	
	@Override
	protected ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent,
			boolean forceOverritten) {
		ADD_DOCUMENT_RESULT_TYPES result =  super.addDocumentInATS(response, htmlContent, forceOverritten);
		try {
			if(result.equals(ADD_DOCUMENT_RESULT_TYPES.ADDED)) {
				UtilForGwtServer.uploadDocumentToSSF(searchId, response.getParsedResponse().getDocument());
			}
		} catch (Exception e) {
			logger.error("Error while saving index for " + searchId, e);
		}
		return result;
	}

	public static void main(String[] args) {
	}
}
