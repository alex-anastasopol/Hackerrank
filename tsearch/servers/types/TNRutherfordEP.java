/*

 * Created on Aug 25, 2004
 */
package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.RegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author cozmin
 * 
 */
public class TNRutherfordEP extends TSServer {
	//fromCOEagleTR
	protected static final Pattern TRANFERS_BOOK_PAGE_PATTERN = 
			Pattern.compile("\\s*B:\\s*(\\d+)\\s*P:\\s*(\\d+)\\s*");
	
	//endFromCOEagleTR
	private static final long serialVersionUID = -4538149638180187817L;

	private boolean downloadingForSave;

	private static int seq = 0;

	protected synchronized static int getSeq() {
		return seq++;
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m;
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();

		boolean emptyStreet = "".equals(sa.getAtribute(SearchAttributes.P_STREETNAME));
		String pid = sa.getAtribute(SearchAttributes.LD_PARCELNONDB);
		if (StringUtils.isEmpty(pid))
			pid = sa.getAtribute(SearchAttributes.LD_PARCELNO);

		FilterResponse addressFilter = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.65d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		FilterResponse nameFilterHybridDoNotSkipUnique = null;
		String city = getSearchAttribute(SearchAttributes.P_CITY).toUpperCase();
		if (!StringUtils.isEmpty(city)) {
			if (!city.contains("MURFREESBORO")) {
				return;
			}
		}

		if (!StringUtils.isEmpty(pid)) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.setLabel("Search City");
			m.clearSaKeys();
			m.setData(9, pid);
			l.add(m);
		}
		if (!emptyStreet) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.setLabel("Search City");
			m.clearSaKeys();
			m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_DEFAULT);
			m.getFunction(1).setSaKey(SearchAttributes.P_STREET_FULL_NAME_EX);

			m.addFilter(addressFilter);// see MOClayTR, setModulesForAutoSearch
			m.addFilter(nameFilterHybrid);
			l.add(m);
		}

		ConfigurableNameIterator nameIterator = null; // fix for B3503
		if (hasOwner()) {
			nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);

			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.setLabel("Search City");
			m.getFunction(1).setLabel("Owner Name");
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.setSaKey(9, "");

			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybridDoNotSkipUnique);

			m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			m.clearSaKey(1);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId,
					new String[] { "L F;;" });
			m.addIterator(nameIterator);// fix for B3503
			l.add(m);
		}
		serverInfo.setModulesForAutoSearch(l);
	}

	public TNRutherfordEP(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	protected String getValue(String s, String pm) {
		int i = s.indexOf(" " + pm + "=") + pm.length() + 2;
		if (i == -1)
			return null;
		int b, e;
		if (s.charAt(i) == '"') {
			e = s.indexOf('"', i + 1);
			b = i + 1;
		} else if (s.charAt(i) == '\'') {
			e = s.indexOf('\'', i + 1);
			b = i + 1;
		} else {
			e = s.indexOf(' ', i + 1);
			if (e == -1)
				e = s.length();
			b = i;
		}
		return s.substring(b, e);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Map parseForm(String s) {
		int i = s.indexOf(">");
		String form = s.substring(0, i);
		Map ret = new HashMap();
		String val = getValue(form, "NAME");
		if (val != null)
			ret.put("NAME", val);
		val = getValue(form, "METHOD");
		if (val != null)
			ret.put("METHOD", val);
		val = getValue(form, "ACTION");
		if (val != null)
			ret.put("ACTION", val);
		Map params = new HashMap();
		ret.put("params", params);
		int b, e;
		for (i++; (b = s.indexOf("<INPUT", i)) != -1; i = e + 1) {
			e = s.indexOf('>', b);
			String input = s.substring(b, e);
			String name = getValue(input, "NAME");
			val = getValue(input, "VALUE");
			if (val == null)
				val = "";
			params.put(name, val);
		}
		return ret;
	}

	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String sTmp;
		String keyNumber = "";

		String response = Response.getResult();

		String rsResponce = response;
		String initialResponse = rsResponce;
		String qry = Response.getQuerry();

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_ADDRESS:

			sTmp = CreatePartialLink(TSConnectionURL.idPOST);
			if (!rsResponce.contains("Current Owner")) {
				rsResponce = "<b>No results Found</b>";
			} else {
				rsResponce = rsResponce
						.replaceAll("(?is).*<div>\\s*(<table.*id=\\\"ctl00_ContentPlaceHolder1_GridView1.*</table>).*", "$1");
				String upList = "";
				if (rsResponce.contains("Page")) {
					upList = rsResponce.replaceFirst("(?is).*>\\s*(<tr[^>]+>\\s*<td[^>]+>\\s*<table.*?</table>\\s*</td>\\s*</tr>).*", "$1");
					upList = upList.replaceAll("(?is).*(<table.*</table>).*", "$1");
					if (upList.contains("javascript")) {
						upList = upList.replaceAll("(?is)\\$", ":");
						qry = qry.replaceAll("(?is)(.*)&__EVENTTARGET.*", "$1");
						qry = qry.replaceAll("(?is)\\$", ":");

						upList = upList.replaceAll("(?is)(<a href=\\\")javascript[^']+'([^']+)\\s*'\\s*,\\s*'\\s*([^']+)[^\\\"]+", "$1"
								+ sTmp + "/taxrecords2.aspx?" + qry + "&__EVENTTARGET=$2&__EVENTARGUMENT=$3");
						upList = upList.replaceAll(":", "%24");
						upList = upList.replaceAll("<tr>", "<tr >");
					}
				}

				rsResponce = rsResponce.replaceAll("(?is)(<a\\s+href\\s*=\\s*\\\")(totals2.aspx[^\\\"]+)", "$1" + sTmp + "/$2");
				rsResponce = rsResponce.replaceAll("&amp;", "&");
				rsResponce = rsResponce.replaceFirst("(?is)(>)\\s*<tr[^>]+>\\s*<td[^>]+>\\s*<table.*?</table>\\s*</td>\\s*</tr>", "$1");
				rsResponce = rsResponce.replaceFirst("(?is)(>)\\s*<tr[^>]+>\\s*<td[^>]+>\\s*<table.*?</table>\\s*</td>\\s*</tr>", "$1");
				rsResponce = rsResponce.replaceAll("(?is)<a href\\s*=\\s*\\\"[^,]+,\\s*'\\s*Sort[^>]+>", "");
				rsResponce = rsResponce.replaceAll("(?is)(<tr)\\s+style=\\\"back[^>]+>", "$1>");

				rsResponce = upList + rsResponce;

				String value = "";
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
					NodeList nodeList = htmlParser.parse(null);
					NodeList viewStateList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true).extractAllNodesThatMatch(
							new HasAttributeFilter("id", "__VIEWSTATE"));
					if (viewStateList.size() > 0) {
						value = viewStateList.elementAt(0).toHtml();
						Matcher matcher = Pattern.compile(".*value=\"(.*?)\"").matcher(value);
						if (matcher.find())
							value = matcher.group(1);
					}

				} catch (ParserException e) {
					e.printStackTrace();
				}

				int seq = getSeq();
				Map<String, String> params = ro.cst.tsearch.connection.http2.TNRutherfordEP.isolateParams(response, "aspnetForm");
				params.remove("__EVENTTARGET");
				params.remove("__EVENTARGUMENT");
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);

				rsResponce = rsResponce.replaceAll("(?is)(<a href=\\\")javascript[^']+'([^']+)\\s*'\\s*,\\s*'\\s*([^']+)[^\\\"]+", "$1"
						+ sTmp + "/taxrecords2.aspx?" + "__EVENTTARGET=$2&__EVENTARGUMENT=$3&seq=" + seq);

				int parseRowsId = Parser.PAGE_ROWS;

				parser.Parse(Response.getParsedResponse(), rsResponce, parseRowsId, getLinkPrefix(TSConnectionURL.idPOST),
						TSServer.REQUEST_SAVE_TO_TSD);
			}
			break;

		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			String details = getDetails(rsResponce,accountId);
			String filename = accountId + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				loadDataHash(data);
				if (isInstrumentSaved(accountId.toString(),null,data)){
					details += CreateFileAlreadyInTSD();
				}
				else {
					
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();

			}
			break;

		case ID_GET_LINK:

			if (sAction.contains("/taxrecords2.aspx") && !sAction.contains("__EVENTTARGET")
					&& mSearch.getSearchType() != Search.AUTOMATIC_SEARCH)
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			else if (viParseID == ID_GET_LINK || mSearch.getSearchType() == Search.AUTOMATIC_SEARCH)
				ParseResponse(sAction, Response, ID_DETAILS);
			else {// on save
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
			}
			break;

		default:
			break;
		}
	}

	protected String getFileNameFromLink(String url) {
		String parcelId = StringUtils.getTextBetweenDelimiters("fake=", "&", url).trim();
		return parcelId + ".html";
	}

	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException {
		if (pageId == Parser.ONE_ROW)
			p.splitResultRows(pr, htmlString, pageId, "<tr>", "</tr>", linkStart, action);
	}

	protected String getDetails(String rsResponse,StringBuilder accountId) {
		try {
			
			StringBuilder details = new StringBuilder();
			HtmlParser3 parser3 = new HtmlParser3(rsResponse);

			NodeList nodeList = parser3.getNodeList();
			TableTag tabletag1 = (TableTag) parser3.getNodeById("ctl00_ContentPlaceHolder1_GridView2");
			TableTag tabletag2 = (TableTag) parser3.getNodeById("ctl00_ContentPlaceHolder1_GridView4");
			TableTag duePaidTag = (TableTag) parser3.getNodeById("ctl00_ContentPlaceHolder1_GridView5");
			
			
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				NodeList headerList = nodeList
						.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_ContentPlaceHolder1_GridView4"), true)
						.extractAllNodesThatMatch(new TagNameFilter("td"), true);
				if(headerList.size() == 0) {
					return null;
				} else {
					String account = headerList.elementAt(0).toPlainTextString()
						.replace("Account:", "")
						.replace("&nbsp;", "")
						.replaceAll("\\s+", "");
					accountId.append(account);	
				}
				return rsResponse;
			}
			
			NodeList headerList = nodeList
					.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_ContentPlaceHolder1_GridView4"), true)
					.extractAllNodesThatMatch(new TagNameFilter("td"), true);
				if(headerList.size() == 0) {
					return null;
				} else {
					
					String account = headerList.elementAt(0).toPlainTextString()
												
						.replace("Account:", "")
						.replace("&nbsp;", "")
						.replaceAll("\\s+", "");
					accountId.append(account);	
				}
				
			NodeList heading = nodeList.extractAllNodesThatMatch(new TagNameFilter("h2"), true);

			details.append("<table align=\"center\" border=\"1\" style=\"width:100%\"><tr align=\"center\">")
					.append(tabletag1.toHtml().replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"))
					.append("</tr></table>");
			details.append("<table align=\"center\" border=\"1\" style=\"width:100%\"><tr align=\"center\">")
					.append(tabletag2.toHtml().replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"))
					.append("</tr></table>");
			details.append("<table align=\"center\" border=\"1\" style=\"width:100%\"><tr align=\"center\">")
					.append(heading.elementAt(0).toHtml())
					.append(duePaidTag.toHtml().replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"))
					.append("</tr></table>");
			
			// get data from Appraised Value Link at bottom of page
			String link = getDataSite().getServerHomeLink() + "Appraised2.aspx";
			String response = getLinkContents(link);
			parser3 = new HtmlParser3(response);
			TableTag appraisedValueTag = (TableTag) parser3.getNodeById("ctl00_ContentPlaceHolder1_GridView6");

			nodeList = parser3.getNodeList();
			heading = nodeList.extractAllNodesThatMatch(new TagNameFilter("h2"), true);

			details.append("<table align=\"center\" border=\"1\" style=\"width:100%\"><tr align=\"center\">")
					.append(heading.elementAt(0).toHtml())
					.append(appraisedValueTag.toHtml().replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"))
					.append("</tr></table>");
		
			// get data from Assessed Value Link at bottom of page
			link = getDataSite().getServerHomeLink() + "Assessed2.aspx";
			response = getLinkContents(link);
			parser3 = new HtmlParser3(response);
			TableTag assessedValueTag = (TableTag) parser3.getNodeById("ctl00_ContentPlaceHolder1_GridView7");

			nodeList = parser3.getNodeList();
			heading = nodeList.extractAllNodesThatMatch(new TagNameFilter("h2"), true);

			details.append("<table align=\"center\" border=\"1\" style=\"width:100%\"><tr align=\"center\">")
					.append(heading.elementAt(0).toHtml())
					.append(assessedValueTag.toHtml().replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"))
					.append("</tr></table>");

			// get data from Owner Link at bottom of page
			link = getDataSite().getServerHomeLink() + "Owner2.aspx";
			response = getLinkContents(link);
			parser3 = new HtmlParser3(response);
			TableTag ownerTag = (TableTag) parser3.getNodeById("ctl00_ContentPlaceHolder1_GridView1");

			nodeList = parser3.getNodeList();
			heading = nodeList.extractAllNodesThatMatch(new TagNameFilter("h2"), true);

			details.append("<table align=\"center\" border=\"1\" style=\"width:100%\"><tr align=\"center\">")
					.append(heading.elementAt(0).toHtml())
					.append(ownerTag.toHtml().replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"))
					.append("</tr></table>");

			return details.toString();

		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	protected void loadDataHash(HashMap<String, String> data) {
		if(data != null) {
			data.put("type","CITYTAX");
		}
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap){
	try {
		org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
		NodeList nodeList = htmlParser.parse(null);

		NodeList headerList = nodeList
				.extractAllNodesThatMatch(new HasAttributeFilter("id","ctl00_ContentPlaceHolder1_GridView4"), true)
 				.extractAllNodesThatMatch(new TagNameFilter("td"), true);

		String accountId = null;
		if(headerList.size() == 0) {
			return null;
		} else {
			accountId = headerList.elementAt(0).toPlainTextString()
				.replace("Account:", "")
				.replace("&nbsp;", "")
				.replaceAll("\\s+", "");
				
		}
	
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountId);
		resultMap.put("OtherInformationSet.SrcType","YA");
		
			NodeList someNodeList = nodeList
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_GridView2"), true)
					.extractAllNodesThatMatch(new TagNameFilter("tr"), true);

			someNodeList.add(nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_GridView4"), true)
					.extractAllNodesThatMatch(new TagNameFilter("tr"), true));
			someNodeList.add(nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_GridView5"), true)
					.extractAllNodesThatMatch(new TagNameFilter("tr"), true));
			someNodeList.add(nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_GridView6"), true)
					.extractAllNodesThatMatch(new TagNameFilter("tr"), true));
			someNodeList.add(nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_GridView7"), true)
					.extractAllNodesThatMatch(new TagNameFilter("tr"), true));
			someNodeList.add(nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_GridView1"), true)
					.extractAllNodesThatMatch(new TagNameFilter("tr"), true));

		String ownerName = "";
		
			for (int i = 0; i < someNodeList.size(); i++) {
				TableRow row = (TableRow) someNodeList.elementAt(i);
				String plainText = row.toPlainTextString().trim();
				if (plainText.startsWith("Parcel ID")) {
					plainText = plainText.replace("Parcel ID", "").trim().replaceAll("-", "");
				
					TableRow row1 = (TableRow) someNodeList.elementAt(i+1);
					String parcelId = row1.getChild(1).toPlainTextString().trim();

					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID_PARCEL.getKeyName(),
							parcelId);

				}
				if (plainText.startsWith("Address")) {

					plainText = plainText.replace("Address", "").trim().replaceAll("-", "");
					TableRow row1 = (TableRow) someNodeList.elementAt(i+1);
					String address = row1.getChild(2).toPlainTextString().trim();
					String streetName = address.replaceFirst("(\\d+ )", "");
					String streetNo = address.replaceAll("( \\D*)", "");

					resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);

				}
				if (plainText.contains("Deed")) {
					
					plainText = plainText.replace("Deed", "").trim().replaceAll("-", "");
					TableRow row1 = (TableRow) someNodeList.elementAt(i+1);
					String deed = row1.getChild(2).toPlainTextString().trim();
					resultMap.put("tmpDeed", deed);
					ro.cst.tsearch.servers.functions.TNRutherfordEP.parseDeedInformationYA(resultMap, searchId);
					
				}

				if (plainText.contains("Plat")) {
					plainText = plainText.replace("Plat", "").trim();
					TableRow row1 = (TableRow) someNodeList.elementAt(i + 1);
					String plat = row1.getChild(3).toPlainTextString().trim();
					resultMap.put("tmpPlat",plat);
					
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(),
							RegExUtils.getFirstMatch(".*-(\\d[\\dA-Z]*)", plat, 1));
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(),
							RegExUtils.getFirstMatch(".*-(\\d*)-\\d*", plat, 1));
					resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(),
							RegExUtils.getFirstMatch("(\\d*)-.*", plat, 1));
					resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(),
							RegExUtils.getFirstMatch("\\d*-(\\d*)-.*", plat, 1));
				}
				if (plainText.contains("Bill #Tax")) {
					ro.cst.tsearch.servers.functions.TNRutherfordEP.parseTaxes(nodeList, resultMap, searchId);
					
				}
				if (plainText.startsWith("Subdivision")) {
					plainText = plainText.replace("Subdivision", "").trim();
					TableRow row1 = (TableRow) someNodeList.elementAt(i+1);
					String subdivision = row1.getChild(3).toPlainTextString().trim();	
					resultMap.put("tmpSubdivision",	StringFormats.SubdivisionNashvilleAO(subdivision));

					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(),
							StringFormats.SubdivisionNashvilleAO(subdivision));
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),
							StringFormats.SectionNashvilleAO(subdivision));
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(),
							StringFormats.PhaseNashvilleAO(subdivision));
					
				}
	
				if (plainText.contains("Owner Name")) {
					plainText = plainText.replace("Owner Name", "").trim();
				
					TableRow row1 = (TableRow) someNodeList.elementAt(i+1);
					ownerName = row1.getChild(2).toPlainTextString().trim();
					ownerName= org.apache.commons.lang.StringEscapeUtils.unescapeHtml(ownerName);
					resultMap.put(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName(), ownerName);
		
				} 

				if (plainText.contains("Assessed Value")) {
					plainText=plainText.replace("Assessed Value", "").trim().replaceAll("-", "");
					TableRow row1 = (TableRow) someNodeList.elementAt(i+1);
					String assessedValue = row1.getChild(7).toPlainTextString().trim();
					resultMap.put(PropertyAppraisalSet.PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(),
							assessedValue);

				}
				if (plainText.contains("Pers. Prop.Total")) {
	
					
					TableRow row1 = (TableRow) someNodeList.elementAt(i+1);
					String totalAppraisal = row1.getChild(5).toPlainTextString().trim();
					resultMap.put(PropertyAppraisalSet.PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(),
							totalAppraisal);
				}
			}
			
		GenericFunctions.composeSubdivision(resultMap, searchId);
		GenericFunctions.stdPisRutherfordEP(resultMap, searchId);
		GenericFunctions.taxRutherfordEP(resultMap, searchId);
		GenericFunctions.setPropertyDescription(resultMap, searchId);
		GenericFunctions.parseDeedInformation(resultMap, searchId);
		GenericFunctions.partyNamesTNRutherfordEP(resultMap,searchId);
		
	} catch (Exception e) {
		logger.error("Error while getting details", e);
	}
	return null;
}
}
