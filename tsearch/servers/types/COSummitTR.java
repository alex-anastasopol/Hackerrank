/**
 * 
 */
package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.recoverdocument.ModuleShortDescription;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.log.SearchLogFactory;
import ro.cst.tsearch.log.SearchLogPage;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/**
 * @author oliviav
 *
 */
public class COSummitTR extends TSServer {

	private static final long serialVersionUID = 1L;
	//private static final String FORM_NAME = "frmDefault";
	private static final String FORM_NAME = "aspnetForm";

	public static HashMap<String, String> propertyTypeSelect = new HashMap<String, String>();
	
	static {
		propertyTypeSelect.put("0", "Real");
		propertyTypeSelect.put("1", "Personal");
		propertyTypeSelect.put("2", "Mobile Home");
		propertyTypeSelect.put("99", "All");
	}
	
	public COSummitTR(long searchId) {
		super(searchId);
	}
	
	public COSummitTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	
	private String processLinks(String id, HtmlParser3 parser, int seq) {
		String detailsInfo = "";
		
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		if (site != null) {
			Map<String, String> params;
			try {
				String lnk = site.getSiteLink() + "?";
				params = HttpSite.fillConnectionParams(parser.getHtml(), ((ro.cst.tsearch.connection.http2.COSummitTR) site)
						.getTargetArgumentParameters(), FORM_NAME);
				//contentBody_TabContainer1_ClientState	{"ActiveTabIndex":2,"TabState":[true,true,true]}
				if ("ctl00$contentBody$TabContainer1$pnlViewer$btnPrintStatement".equals(id)) {
					params.put("ctl00$contentBody$TabContainer1$pnlViewer$btnPrintStatement", "Print Account Statement");
					
				}else if("ctl00$contentBody$TabContainer1$pnlViewer$btnPrintTaxNotice".equals(id)) {
					params.put("ctl00$contentBody$TabContainer1$pnlViewer$btnPrintTaxNotice", "Print Tax Notice");
				}
				
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
				String rsResponse = ((ro.cst.tsearch.connection.http2.COSummitTR) site).getPage(lnk, params);
				
				if ("ctl00$contentBody$TabContainer1$pnlViewer$btnPrintStatement".equals(id)) {
					params = null;
					//GET  /ACTionTreasurer/AccountStatementViewer.aspx
					lnk =  site.getSiteLink();
					lnk = lnk.replaceFirst("(.*/ACTionTreasurer/).*", "$1") + "AccountStatementViewer.aspx";
					rsResponse = ((ro.cst.tsearch.connection.http2.COSummitTR) site).getPage(lnk, params);
					detailsInfo = getTaxAccountStatement(rsResponse);
					
				}else if("ctl00$contentBody$TabContainer1$pnlViewer$btnPrintTaxNotice".equals(id)) {
					params = null;
					//GET  /ACTionTreasurer/TaxNoticeViewer.aspx
					lnk =  site.getSiteLink();
					lnk = lnk.replaceFirst("(.*/ACTionTreasurer/).*", "$1") + "TaxNoticeViewer.aspx";
					rsResponse = ((ro.cst.tsearch.connection.http2.COSummitTR) site).getPage(lnk, params);
					detailsInfo = getTaxNotice(rsResponse);
				}
				
			} finally {
				// always release the HttpSite
				HttpManager.releaseSite(site);
			}
		}

		return detailsInfo;
	}

	protected void logSearchBy(TSServerInfoModule module, Map<String, String> params) {

		if (module.isVisible() || "GB_MANAGER_OBJECT".equals(module.getSaObjKey())) {// B 4511

			// get parameters formatted properly
			Map<String, String> moduleParams = params;
			if (moduleParams == null) {
				moduleParams = module.getParamsForLog();
			}
			Search search = getSearch();
			// determine whether it's an automatic search
			boolean automatic = (search.getSearchType() != Search.PARENT_SITE_SEARCH) || (GPMaster.getThread(searchId) != null);
			boolean imageSearch = module.getLabel().equalsIgnoreCase("image search") || module.getModuleIdx() == TSServerInfo.IMG_MODULE_IDX;

			// create the message
			StringBuilder sb = new StringBuilder();
			SearchLogFactory sharedInstance = SearchLogFactory.getSharedInstance();
			SearchLogPage searchLogPage = sharedInstance.getSearchLogPage(searchId);
			sb.append("</div>");

			Object additional = GetAttribute("additional");
			if (Boolean.TRUE != additional) {
				searchLogPage.addHR();
				sb.append("<hr/>");
			}
			int fromRemoveForDB = sb.length();

			// searchLogPage.
			sb.append("<span class='serverName'>");
			String serverName = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName();
			sb.append(serverName);
			sb.append("</span> ");

			sb.append(automatic ? "automatic" : "manual");
			Object info = module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION);
			if (StringUtils.isNotEmpty(module.getLabel())) {

				if (info != null) {
					sb.append(" - " + info + "<br>");
				}
				sb.append(" <span class='searchName'>");
				sb.append(module.getLabel());
			} else {
				sb.append(" <span class='searchName'>");
				if (info != null) {
					sb.append(" - " + info + "<br>");
				}
			}
			sb.append("</span> by ");

			boolean firstTime = true;
			for (Entry<String, String> entry : moduleParams.entrySet()) {
				String value = entry.getValue();
				value = value.replaceAll("(, )+$", "");
				
				if ("Property type:".equals(entry.getKey())) {
					if (value.matches("\\d+")) {
						value = org.apache.commons.lang.StringUtils.defaultString(propertyTypeSelect.get(value));
					}
				}
				if ("Tax Year".equals(entry.getKey()) && "0".equals(value)) {
					value = "All";
				}
				
				if (!firstTime) {
					sb.append(", ");
				} else {
					firstTime = false;
				}
				sb.append(entry.getKey().replaceAll("&lt;br&gt;", "") + " = <b>" + value + "</b>");
			}
			//because ALL does not have a value.
			if (!moduleParams.containsKey("File Type")){
				sb.append(", ").append("File Type = <b>ALL</b>");
			}
			
			int toRemoveForDB = sb.length();
			// log time when manual is starting
			if (!automatic || imageSearch) {
				sb.append(" ");
				sb.append(SearchLogger.getTimeStamp(searchId));
			}
			sb.append(":<br/>");

			// log the message
			SearchLogger.info(sb.toString(), searchId);
			ModuleShortDescription moduleShortDescription = new ModuleShortDescription();
			moduleShortDescription.setDescription(sb.substring(fromRemoveForDB, toRemoveForDB));
			moduleShortDescription.setSearchModuleId(module.getModuleIdx());
			search.setAdditionalInfo(TSServerInfoConstants.TS_SERVER_INFO_MODULE_DESCRIPTION, moduleShortDescription);
			String user = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getAttribute(1).toString();
			SearchLogger.info(ro.cst.tsearch.utils.StringUtils.createCollapsibleHeader(), searchId);
			searchLogPage.addModuleSearchParameters(serverName, additional, info, moduleParams, module.getLabel(), automatic, imageSearch, user);
		}
	}
	
	
	private String getTaxAccountStatement(String rsResponse) {
		try {
			rsResponse = new String (rsResponse.getBytes(),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		HtmlParser3 htmlParser = new HtmlParser3(rsResponse);
		NodeList nodeList = htmlParser.getNodeList();
		nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true) ;
		String cleanedRsp = "";
		
		if (nodeList != null && nodeList.size() > 1) {
			nodeList = nodeList.elementAt(1).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);
			nodeList.remove(2);
			
			if (nodeList.size() > 1) {
				TableTag accStmtTable = (TableTag) nodeList.elementAt(2);
				if (accStmtTable != null) {
					TableRow[] rows = accStmtTable.getRows();
					if (rows.length == 4) {
						TableRow row = rows[3];
						TableColumn col = row.getColumns()[0];
						if (col != null && col.getChildCount() > 1) {
							TableTag taxInfoTable = (TableTag)col.getChild(1);
							taxInfoTable.setAttribute("id", "\"taxDetailsTable\"");
						}
					}				}
				
				cleanedRsp = accStmtTable.toHtml();
				cleanedRsp = cleanedRsp.replaceAll("(?is)\\s*�\\s*", " ");
				cleanedRsp = cleanedRsp.replaceAll("(?is)&Acirc;", "");
			}
		}
		
		return cleanedRsp;
	}

	
	private String getTaxNotice(String rsResponse) {
		try {
			rsResponse = new String (rsResponse.getBytes(),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		HtmlParser3 htmlParser = new HtmlParser3(rsResponse);
		NodeList nodeList = htmlParser.getNodeList();
		nodeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true) ;
		
		String cleanedRsp = "";
		
		if (nodeList != null && nodeList.size() > 2) {
			nodeList.remove(2);
			nodeList.remove(0);
			
			nodeList = nodeList.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true);
			TableTag taxNoticeTable = (TableTag)nodeList.elementAt(0);
			if (taxNoticeTable != null) {
				TableRow row = taxNoticeTable.getRow(0);
				if (row.getColumnCount() > 2) {
					TableColumn col = row.getColumns()[1];
					if (col != null && col.getChildCount() >= 1) {
						taxNoticeTable = (TableTag) col.getChildren().elementAt(1);
						if (taxNoticeTable.getChildCount() > 2) {
							taxNoticeTable.removeChild(2);
							taxNoticeTable.removeChild(1);
							taxNoticeTable.removeChild(0);
						}
					}
				}
			}
				
			cleanedRsp = taxNoticeTable.toHtml();
			cleanedRsp = cleanedRsp.replaceAll("(?is)\\s*�\\s*", " ");
			cleanedRsp = cleanedRsp.replaceAll("(?is)&Acirc;", "");
			cleanedRsp = cleanedRsp.replaceAll("(?is)<img src=[^>]+>", "@");
			cleanedRsp = cleanedRsp.replaceFirst("(?is)<span[^,]+,[^/]+/span>", "@");
			cleanedRsp = cleanedRsp.replaceFirst("(?is)<tr[^>]+>\\s*<td[^>]+>\\s*@[^@]+@\\s*</td>\\s*</tr>", "");
			cleanedRsp = cleanedRsp.replaceAll("(?is)(</?h)2>", "$1"+"4>");
			cleanedRsp = cleanedRsp.replaceFirst("(?is)<td[^>]+>[^<]+<table[^>]+>\\s*<tr[^>]+>\\s*<td[^>]+>\\s*(<h4[^/]+/h4>)\\s*</td>\\s*</tr>\\s*</table>", "<td align=\"center\" colspan=\"2\"> $1");
		}
		
		return cleanedRsp;
	}
	
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {						
			Map<String, String> params = null;			 			
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			if (site != null) {
				try {
					params = HttpSite.fillConnectionParams(table,((ro.cst.tsearch.connection.http2.COSummitTR) site)
							.getTargetArgumentParameters(), FORM_NAME);
					
				} finally {
					// always release the HttpSite
					HttpManager.releaseSite(site);
				}
			}

			table = table.replaceAll("(?is)(\\s*<td[^>]+>\\s*<input[^>]+>\\s*)(</td>)", "$1" + "<p> </p>" + "$2");
			
			HtmlParser3 htmlParser = new HtmlParser3(table);
			NodeList nodeList = htmlParser.getNodeList();
			
			FormTag form = (FormTag) nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", FORM_NAME)).elementAt(0);
			form.setAttribute("name", FORM_NAME);
			String action = form.getFormLocation();	
			
//			nodeList = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true)
//					.extractAllNodesThatMatch(new HasAttributeFilter("id", "contentBody_TabContainer1_pnlResults_gvSearchResults"), true);
			nodeList = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_contentBody_TabContainer1_pnlResults_gvSearchResults"), true);
			if(nodeList.size() == 0) {
				return intermediaryResponse;
			}
			
			TableTag tableTag = (TableTag) nodeList.elementAt(0);
			tableTag.setAttribute("border", "1");
			
			TableRow[] rows  = tableTag.getRows();
			int seq = getSeq();
			
			for (int i = 1; i < rows.length; i++) {
				TableRow row = rows[i];
				if(row.getColumnCount() == 8) {
					ParsedResponse currentResponse = new ParsedResponse();					
					TableColumn col = row.getColumns()[0];
					
					if (col != null) {
						InputTag linkTag = ((InputTag)col.getChildren().extractAllNodesThatMatch(new TagNameFilter("input")).elementAt(0));
						String tmpLnk = linkTag.getAttribute("onclick").trim();
						tmpLnk = tmpLnk.replaceAll("(?is)[^(]+\\(([^)]+)\\)", "$1");	
						tmpLnk = tmpLnk.replaceAll("&#39;", "");
						String eventTargetValue = tmpLnk;
						eventTargetValue = eventTargetValue.replaceFirst("(?is)([^,]+),\\s*.*", "$1").trim();
						String eventArgumentValue = tmpLnk;
						eventArgumentValue = eventArgumentValue.replaceFirst("(?is)[^,]+,\\s*(.*)", "$1").trim();
					
						
						mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
						
						String link = (CreatePartialLink(TSConnectionURL.idPOST) + "/ACTionTreasurer/" + action
								+ "?__EVENTARGUMENT=" + eventArgumentValue + "&__EVENTTARGET=" + eventTargetValue + "&seq=" + seq)
								.replaceAll("\\s", "%20");
						
						LinkTag linkRow = new LinkTag();
						linkRow.setLink(link);
					
						String tmp = linkRow.toHtml() + "View" + "</A>";
					
						col.getChild(1).getFirstChild().setText(tmp);
						col.removeChild(0);
					
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
						currentResponse.setOnlyResponse(row.toHtml());
						currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					}
					
					ResultMap map = parseIntermediaryRow(row, searchId);
					Bridge bridge = new Bridge(currentResponse, map, searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);					
				}
			}
			
			response.getParsedResponse().setHeader("<table border=\"1\" cellspacing=\"0\" cellpadding=\"8\">\n" + 
					"<tr><th>Select</th> 			<th>Schedule</th> 		<th>Tax</br>Year</th> 	<th>Property</br>Type</th>" +
					" <th>Account</br>Status</th> 	<th>Description</th> 	<th>Primary Owner</th> 	<th>Address</th> </tr>");			
			response.getParsedResponse().setFooter("</table>");
			
			outputTable.append(table);
						
		} catch (Throwable t){
			logger.error("Error while parsing intermediary data", t);
		}
		
		return intermediaryResponse;
	}
	
	
	private static void extractNames (String owners, List<List> body) {
		if (body == null) {
			body = new ArrayList<List>();
		}
		
		String[] own = owners.split("<br>");
		if (own.length > 0) {
			for (int i=0; i< own.length; i++) {
				String s = own [i].trim();
				if (StringUtils.isNotEmpty(s)) {
					String[] names = null;
					s = s.replaceAll("\\s*/\\s*", " & ");
					s = s.replaceFirst("Co-Trustee", "Trustee");
					names = StringFormats.parseNameDesotoRO(s, true);
							
					String[] types = GenericFunctions.extractAllNamesType(names);
					String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
								
					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
							NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
				}
			}
		}
	}
	
	
	@SuppressWarnings("rawtypes")
	public static void parseNamesIntermediary(ResultMap resultMap, long searchId) throws Exception {
		   List<List> body = new ArrayList<List>();
		   String owner = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		   owner = owner.replaceFirst("(?is)\\s*Trustee-Trustor\\s*", " Trustee ");
		   
		   if (StringUtils.isEmpty(owner))
			   return;
		   
		   else {
				String[] names = null;
				owner = owner.replaceAll("\\s*/\\s*", " & ");
				names = StringFormats.parseNameDesotoRO(owner);
						
				String[] types = GenericFunctions.extractAllNamesType(names);
				String[] otherTypes = GenericFunctions.extractAllNamesOtherType(names);
				String[] suffixes = GenericFunctions.extractAllNamesSufixes(names);
							
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			}
		   
		   try {
				GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		     
	   }
	
	
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 8) {	
			String apn 		= cols[1].toPlainTextString().trim();
			String taxYear 	= cols[2].toPlainTextString().trim();
			String legalDesc = cols[5].toPlainTextString().trim();
			String owner	 = cols[6].toPlainTextString().trim();
			String address = cols[7].toPlainTextString().trim();
			
			if (StringUtils.isNotEmpty(address)) {
				String adrUnitNo = "";
				String regExp = "(?is)(.*)(?:\\(CR\\d+\\))?,(\\s*Unit\\s*[^,]+),\\s*(County|Breckenridge|Blue River|Cove|Dillon|Frisco|Montezuma|Silverthorne|Unknown)\\s*";
				Pattern p = Pattern.compile(regExp);
				Matcher m = p.matcher(address);
				
				if (m.find()) {
					adrUnitNo = m.group(2).replaceFirst("(?is)\\bUnit\\b\\s*([^,]+).*", "$1").trim();
					address = address.replaceFirst(regExp, m.group(1));
				} else {
					regExp = "(?is)(.*)\\(CR\\d+\\),\\s*(?:County|Breckenridge|Blue River|Cove|Dillon|Frisco|Montezuma|Silverthorne|Unknown)\\s*";
					p = Pattern.compile(regExp);
					m = p.matcher(address);
					if (m.find()) {
						address = address.replaceFirst(regExp, m.group(1));
					}
					
					regExp = "(?is).*Unit\\\\s*([A-Z]|\\d+).*";
					p = Pattern.compile(regExp);
					m = p.matcher(address);
					if (m.find()) {
						adrUnitNo = m.group(1);
						address = address.replaceFirst("(?is)(.*)Unit\\\\s*[A-Z]|\\d+(.*)", "$1 $2").trim();
					}
				}
					
				if (StringUtils.isNotEmpty(adrUnitNo)) {
					address = address.trim() + " #" + adrUnitNo.trim();
				}
				
				resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
				resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			}
				
			if (StringUtils.isNotEmpty(legalDesc)) {
				legalDesc = legalDesc.replaceAll("&#39;", "'");
				resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legalDesc);
				parseLegalDesc(legalDesc, resultMap, searchId);
			}
					
			if (StringUtils.isNotEmpty(owner)) {
				resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
				try {
					parseNamesIntermediary (resultMap, searchId);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			if (StringUtils.isNotEmpty(apn)) {
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), apn);
			}
			if (StringUtils.isNotEmpty(taxYear)) {
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
			}
		}
		
		return resultMap;
	}
		
	
	private static void parseLegalDesc(String legalDesc, ResultMap resultMap, long searchId) {
		String phase = "";
		String unit = "";
		String lot = "";
		String block = "";
		String bldg = "";
		String subdivision = "";
		
		legalDesc = legalDesc.replaceFirst("(?is)Townhome (Unit.*)", "$1");
		legalDesc = legalDesc.replaceFirst("(?is)Tract by", "");
		
		
		if (legalDesc.contains(" Phase")) {
			phase = legalDesc;
			phase = phase.replaceFirst("(?is).*Phase\\s*(\\d+|[IVX]+)\\s*", "$1").trim();
			phase = GenericFunctions1.convertFromRomanToArab(phase);
			legalDesc = legalDesc.replaceFirst("(?is)(.*)\\s*Phase\\s*(?:\\d+|[IVX]+)\\s*","$1").trim();
		}
		if (legalDesc.contains("Unit ")) {
			unit = legalDesc;
			String unitPattern = "(?is)\\s*Unit\\s*((?:[A-Z]-?)?\\d*(?:-?\\s*[A-Z]\\b)?)(.*)";
			unit = unit.replaceFirst(unitPattern, "$1");
			legalDesc = legalDesc.replaceFirst(unitPattern, "$2").trim();
		}
		if (legalDesc.contains("Lot ")) {
			Pattern lotPattern = Pattern.compile("(?is)\\bLO?TS?\\s*(\\d+\\s*[A-Z]?\\b\\s*(?:(?:[,\\s&-])\\d*\\s*[A-Z]?\\b)*)");
			Matcher lotMatcher = lotPattern.matcher(StringUtils.transformNull(legalDesc));
			while (lotMatcher.find()) {
				lot += lotMatcher.group(1).replaceAll("[&,]", " ").replaceAll("\\s{2,}", " ").trim() + " ";
				legalDesc = legalDesc.replaceAll(lotMatcher.group(0), "").trim();
			}
			if (lot.contains("-")) {
				lot = ro.cst.tsearch.extractor.xml.StringFormats.ReplaceIntervalWithEnumeration(lot);
			}
			lot = lot.replaceAll("(?s)\\b0+(\\d+)\\b", "$1");
		}
		if (legalDesc.contains("Bldg ")) {
			bldg = legalDesc;
			bldg = bldg.replaceFirst("(?is)Bldg\\s*([A-Z]|\\d+).*", "$1");
			legalDesc = legalDesc.replaceFirst("(?is)\\s*Bldg\\s*(?:[A-Z]|\\d+)(.*)","$1").trim();
		}
		if (legalDesc.contains("Block ") || legalDesc.contains("Blk ")) {
			block = legalDesc;
			block = block.replaceAll("(?is)Bl(?:oc)?k\\s*([A-Z]|\\d+).*", "$1").trim();
			legalDesc = legalDesc.replaceAll("(?is)\\s*Bl(?:oc)?k\\s*(?:[A-Z]|\\d+)(.*)","$1");
		}
		
		legalDesc = legalDesc.replaceFirst("(?is)\\s*\\bResub.*", "");
		legalDesc = legalDesc.replaceFirst("(?is)\\s*b\\And Garage.*","");
		legalDesc = legalDesc.replaceAll("(?is)\\bAka\\b", "");
		legalDesc = legalDesc.replaceAll("(?is)\\bSub\\b\\s*(Replat Of|Cont|\\d{1,2}-\\d{1,2}-\\d{1,2})\\s*.*","Sub");
		legalDesc = legalDesc.replaceFirst("(?is)bounded on.*","");
		
		String regExp = "(?is).*(Government Tracts [\\d-]+) Sub\\s*(?:.*)?";
		Pattern p = Pattern.compile(regExp);
		Matcher m = p.matcher(legalDesc);
		
		if (m.find()) {
			subdivision = m.group(1).replaceFirst("\\s*\\bSub(?:division)?\\b", "").trim();
			legalDesc = legalDesc.replaceFirst("(?is)(.*)Government Tracts [\\d-]+\\s*Sub\\s*", "$1");
		}
		
		legalDesc = legalDesc.replaceAll("(?is)\\s*\\b(Subdivision|Sub|Condominiums|Condominium|Condos|Condo)\\b\\s*(?:IV|III|II|IX|I|VIII|VII|VI|V|X)?", "");
		subdivision = legalDesc.trim();
		subdivision = GenericFunctions2.cleanSubdivNameCOSummitAOLike(subdivision);
		
		if (StringUtils.isNotEmpty(unit)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), unit);
		}
		if (StringUtils.isNotEmpty(phase)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), phase);
		}
		if (StringUtils.isNotEmpty(lot)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
		}
		if (StringUtils.isNotEmpty(bldg)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getKeyName(), bldg);
		}
		if (StringUtils.isNotEmpty(block)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
		}
		if (StringUtils.isNotEmpty(subdivision)) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
		}
	}
	
	protected void loadDataHash(HashMap<String, String> data, String taxYear) {
		if (data != null) {
			data.put("type", "CNTYTAX");
			data.put("year", taxYear);
		}
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		try {
			rsResponse = new String (rsResponse.getBytes(),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		switch (viParseID) 
		{
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_PARCEL:	
				// no result
				if ((rsResponse.indexOf("Warning: ResultSet Exceeds the Maximum Threshold (200)") > -1)  || (rsResponse.contains("Schedule is Required") && (rsResponse.contains("Last Name is Required")))) {
					Response.getParsedResponse().setError("Warning: ResultSet Exceeds the Maximum Threshold (200). Please change your search criteria and try again!");
					return;
				} else if (rsResponse.indexOf("Last Name is Required") > -1) {
					Response.getParsedResponse().setError("Error: Last Name is Required!");
					return;
				}
				if (rsResponse.indexOf("Property Tax Information for Summit County") > -1) {
					ParseResponse(sAction, Response, ID_DETAILS);
					return;
				}
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() == 0) {
					return;
				}
				// parse and store parameters on search
				Form form = new SimpleHtmlParser(rsResponse).getForm(FORM_NAME);
				Map<String, String> params = form.getParams();
//				params.remove("__EVENTTARGET");
//				params.remove("__EVENTARGUMENT");
				int seq = getSeq();
				mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
				
				StringBuilder footer = new StringBuilder("<tr><td colspan=\"8\" align=\"center\">");
				Response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\">\n" +
						"<tr><th> </th> <th>Schedule</th> <th>Tax</br>Year</th> <th>Property</br>Type</th> <th>Account</br>Status</th>" +
						"<th>Description</th> <th>Primary Owner</th> <th>Address</th> </tr>");
				Response.getParsedResponse().setFooter(footer.toString() + "</table>");
				
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				
				break;
			case ID_GET_LINK:
				if (rsResponse.indexOf("Property Tax Information for Summit County") > -1) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				break;
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				
				StringBuilder accountId = new StringBuilder();
				String details = "";
				String taxYear = "";
				
				details = getDetails(rsResponse, accountId);
				details = details.replaceAll("\u00A0", " "); // issue with � on doc index from TSRI - task 8696
				
				if (details.contains("accountNo")) {
					taxYear = getTaxYear(details);
				} 
				
				String filename = accountId + ".html";
				
				if (viParseID != ID_SAVE_TO_TSD) {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

					HashMap<String, String> data = new HashMap<String, String>();
					loadDataHash(data, taxYear);
					if (isInstrumentSaved(accountId.toString(),null,data)){
						details += CreateFileAlreadyInTSD();
					}
					else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}

					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
					
				} else {
					smartParseDetails(Response,details);
					
					msSaveToTSDFileName = filename;
					Response.getParsedResponse().setFileName( getServerTypeDirectory() + msSaveToTSDFileName);	
					Response.getParsedResponse().setResponse( details );
					
					msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				}
				break;
		}
	}
	
	private String getTaxYear(String details) {
		String taxYear = "";
		HtmlParser3 parser = new HtmlParser3(details);
		NodeList list = parser.getNodeList();
		
		if (list != null) {
			String info = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(list, "Property Type:"), "", true);
			
			if (StringUtils.isNotEmpty(info)) {
				info = info.replaceFirst("(?is)[^\\d]+(\\d+)\\s*", "$1");
				taxYear = info;
			}
		}
		
		return taxYear;
	}

	private String getDetails(String rsResponse, StringBuilder accountId) {
		try {
			/* If from memory - use it as is */
			if(!rsResponse.contains("<html")){
				if (rsResponse.contains("id=\"accountNo\"")) { 
					String apn = rsResponse;
					apn = apn.replaceAll("&nbsp;", " ");
					apn = apn.replaceAll("(?is).*<tr\\s*(?:valign\\s*=\\s*\\\"top\\\"\\s*)?id\\s*=\\s*\\\"accountNo\\\"\\s*>[^<]+<td[^>]+>\\s*Property\\s*Schedule\\s*:\\s*(\\d+)\\s*</?br\\s*/?>.*","$1");
					accountId.append(apn);
					
					return rsResponse;
				}
			
			} else {
				StringBuilder details = new StringBuilder();
				HtmlParser3 htmlParser = new HtmlParser3(rsResponse);
				NodeList nodeList = htmlParser.getNodeList();
				//NodeList detailsInfo = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "contentBody_TabContainer1_pnlViewer"),true);
				NodeList detailsInfo = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_contentBody_TabContainer1_pnlViewer"),true);
				TableTag table = (TableTag) detailsInfo.extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
				String apn = null;
				
				if (table != null) {
					TableRow row = table.getRow(0);
					TableColumn col = row.getColumns()[1];
					if (col != null) {
						TableTag tmpTable = (TableTag) col.getChildren().elementAt(1);
						row = tmpTable.getRow(1);
						row.setAttribute("id","\"accountNo\"");
						col = row.getColumns()[0];
						apn = col.getChildrenHTML().trim();
						apn = apn.replaceFirst("(?is)Property Schedule:&nbsp;(\\d+).*", "$1");
						if (StringUtils.isNotEmpty(apn)) {
							accountId.append(apn);		
						}
						
						row = tmpTable.getRow(4);
						col = row.getColumns()[0];
						TableTag tbl = (TableTag) col.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
//						if (tbl.toHtml().contains("contentBody_TabContainer1_pnlViewer_trNoAccountSummary")) {
						if (tbl.toHtml().contains("ctl00_contentBody_TabContainer1_pnlViewer_trNoAccountSummary")) {
							//no Tax History
							details.append("<table width=\"90%\" align=\"center\" border=\"1\"><tr><td>");
							details.append(tmpTable.toHtml());
							details.append("</td></tr>");
							
						} else {
							tmpTable = (TableTag)tbl.getRow(0).getColumns()[1].getChildren().elementAt(1);
							tmpTable.setAttribute("id","\"taxesInfoTable\"");
							
							details.append("<table width=\"90%\" align=\"center\" border=\"1\"><tr><td>");
							String personalInfo = detailsInfo.toHtml();
							if (personalInfo.contains("divAccountSummaryPaymentInstructions")) {
								personalInfo = personalInfo.replaceFirst("(?is)<div id=\"divAccountSummaryPaymentInstructions\"" +
										"[^/]+/>[^/]+/>(?:\\s*<br\\s*/>[\\w'\\s\\.]+)?\\s*<div[^>]+>(?:\\s*<input[^/]+/>)?\\s*</div>\\s*</div>", "");
							}
							
							NodeList linksToAppend = detailsInfo.extractAllNodesThatMatch(new HasAttributeFilter
//									("id", "contentBody_TabContainer1_pnlViewer_divPrintDocuments"), true)
									("id", "ctl00_contentBody_TabContainer1_pnlViewer_divPrintDocuments"), true)
									.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true);
							
							personalInfo= personalInfo.replaceFirst("(?is)<div\\s*id=\\\"ctl00_contentBody_TabContainer1_pnlViewer_divPrintDocuments\\\"[^>]+>\\s*<input[^>]+>\\s*(?:<input[^>]+>\\s*)?</div>", "");
							details.append(personalInfo);
							
							if (linksToAppend != null) {
								//we have Account Statment and maybe also Tax Notice buttons
								details.append("</td></tr><tr><td>");
								details.append("<div align=\"center\" style=\"color: blue;\"><b> :: Account Statement for Property Schedule No " + apn + " :: </b> </div> <br/></br>");
								//append Account Statement
								int seq = getSeq();
								String accountStatementTable = processLinks("ctl00$contentBody$TabContainer1$pnlViewer$btnPrintStatement",htmlParser, seq);
								details.append(accountStatementTable);
								details.append("</td></tr>");
								
								if (linksToAppend.size() == 2) {
									details.append("<tr><td><div align=\"center\" style=\"color: blue;\"><b> :: Tax Notice for Property Schedule No " + apn + " :: </b> </div> <br/></br>");
									//append Tax Notice
									seq = getSeq();
									String taxNoticeTable = processLinks("ctl00$contentBody$TabContainer1$pnlViewer$btnPrintTaxNotice",htmlParser, seq);
									details.append(taxNoticeTable);
									details.append("</td></tr>");
								}
							}
						}
					}
				}
				
				
				details.append("</table>");
				
				return details.toString();
			}
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
		
	}
	
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
	
		try {
			detailsHtml = detailsHtml.replaceAll("<br>", "\n");
			detailsHtml = detailsHtml.replaceAll("&nbsp;", " ");
			HtmlParser3 htmlParser = new HtmlParser3(detailsHtml);
			NodeList nodeList = htmlParser.getNodeList();
			boolean hasTaxHistory = true;
			
			if (detailsHtml.contains("contentBody_TabContainer1_pnlViewer_trNoAccountSummary")) {
				hasTaxHistory = false;
			}
			
			TableTag table1 = null;
			
			if (hasTaxHistory) {
				table1 = (TableTag)nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "divViewercontent"), true)
						.elementAt(0).getChildren().elementAt(1);
			} else {
				table1 = (TableTag) nodeList.elementAt(0);
			}
			
			table1.setAttribute("id", "\"mainTable\"");
			
			if (table1 != null) {
				TableRow row = table1.getRow(0);
				TableColumn col = null; 
				if (hasTaxHistory) {
					col = row.getColumns()[1];
				} else {
					col = row.getColumns()[0];
				}
				
				if (col != null) {
					TableTag tmpTable = null;
					if (hasTaxHistory) {
						tmpTable = (TableTag) col.getChildren().elementAt(1);
					} else {
						tmpTable = (TableTag) col.getChildren().elementAt(0);
					}
					if (tmpTable != null && tmpTable.getRowCount() > 4) {
						row = tmpTable.getRow(1);
						if (row.getColumnCount() == 3) {
							//APN info
							col = row.getColumns()[0];
							if (col != null) {
								String apn = col.getChildrenHTML().trim();
								apn = apn.replaceFirst("(?is)Property Schedule:(?:&nbsp;|\\s*)(\\d+).*", "$1").trim();
								if (StringUtils.isNotEmpty(apn)) {
									resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), apn);	
								}
							}
							//Tax year info
							col = row.getColumns()[1];
							if (col != null) {
								String taxYear = col.getChildrenHTML().trim();
								taxYear = taxYear.replaceFirst("(?is)Tax Year:(?:&nbsp;|\\s*)(\\d+).*", "$1").trim();
								if (StringUtils.isNotEmpty(taxYear)) {
									resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);	
								}
							}
							//Address info
							col = row.getColumns()[2];
							if (col != null) {
								String address = col.getChildrenHTML().trim();
								address = address.replaceFirst("(?is)Street Address:(?:&nbsp;|\\s*)(.*)", "$1").trim();
								if (StringUtils.isNotEmpty(address)) {
									resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);	
									String adrUnitNo = "";
									String regExp = "(?is)(.*)(?:\\(CR\\d+\\))?,(\\s*Unit\\s*[^,]+),\\s*(County|Breckenridge|Blue River|Cove|Dillon|Frisco|Montezuma|Silverthorne|Unknown)\\s*";
									Pattern p = Pattern.compile(regExp);
									Matcher m = p.matcher(address);
										
									if (m.find()) {
										adrUnitNo = m.group(2).replaceFirst("(?is)\\bUnit\\b\\s*([^,]+).*", "$1").trim();
										address = address.replaceFirst(regExp, m.group(1));
									} else {
										regExp = "(?is)(.*)\\(CR\\d+\\),\\s*(?:County|Breckenridge|Blue River|Cove|Dillon|Frisco|Montezuma|Silverthorne|Unknown)\\s*";
										p = Pattern.compile(regExp);
										m = p.matcher(address);
										if (m.find()) {
											address = address.replaceFirst(regExp, m.group(1));
										}
											
										regExp = "(?is).*Unit\\s*((?:[A-Z]-?)\\d+|[A-Z]|\\d+).*";
										p = Pattern.compile(regExp);
										m = p.matcher(address);
										if (m.find()) {
											adrUnitNo = m.group(1);
											address = address.replaceFirst("(?is)(.*)Unit\\s*((?:[A-Z]-?)\\d+|[A-Z]|\\d+)(.*)", "$1 $3").trim();
										}
									}
									if (StringUtils.isNotEmpty(adrUnitNo)) {
										address = address.trim() + " #" + adrUnitNo.trim();
									}
								}
								address = address.replaceFirst("(?is)(.*[^,]+),\\s*(County|Breckenridge|Blue River|Cove|Dillon|Frisco|Montezuma|Silverthorne|Unknown)\\s*", "$1").replaceAll(",","");
								resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
								resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
							}
						}
						
						row = tmpTable.getRow(3);
						if (row.getColumnCount() == 1) {
							TableTag tmpTable1 = (TableTag) row.getColumns()[0].getChild(1);
							row = tmpTable1.getRow(1);
							if (row != null && row.getColumnCount() == 2) {
								//Owner info
								col = row.getColumns()[0];
								String owners = col.getChildrenHTML().trim();
								owners = owners.replaceFirst("(?is)(.*)<br\\s*/>\\s*(?:PO BOX)?\\s*\\d+.*", "$1");
								owners = owners.replaceAll("(?is)<br\\s*/*>", "<br>"); 
								owners = owners.replaceAll("(?is)\\s*Trustee-Trustor\\s*", " Trustee ");
								
								List<List> body = new ArrayList<List>();
								
								if (!owners.contains("<br>")) {
									owners += "<br>";
								}
								extractNames(owners, body);
								try {
									GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
									
								} catch (Exception e) {
									e.printStackTrace();
								}
								
								//Legal Description info
								col = row.getColumns()[1];
								String legalDesc = col.getChildrenHTML().trim();
								parseLegalDesc(legalDesc, resultMap, searchId);
							}
						}
						
						row = tmpTable.getRow(4);
						col = row.getColumns()[0];
						if (col != null) {
							//Tax info
							TableTag tmpTable1 = (TableTag) col.getChildren().extractAllNodesThatMatch(new HasAttributeFilter
									("id", "taxesInfoTable"), true).elementAt(0);
							if (tmpTable1 != null) {
								row = tmpTable1.getRow(1);
								if (tmpTable1.getRowCount() == 4) {
									if (row.toHtml().contains("Current Due")) { 
										//BA value
										col = row.getColumns()[1];
										String baseAmount = col.getChildrenHTML().trim().replaceAll("[$,]", "");
										if (StringUtils.isNotEmpty(baseAmount)) {
											resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
										}
									}
									
									//AP value
									row = tmpTable1.getRow(2);
									col = row.getColumns()[1];
									String amountPaid = col.getChildrenHTML().trim().replaceAll("[$,]", "");
									if (StringUtils.isNotEmpty(amountPaid)) {
										resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
									}
									
									//AD value
									row = tmpTable1.getRow(3);
									col = row.getColumns()[1];
									String amountDue = col.getChildrenHTML().trim().replaceAll("[$,]", "");
									if (StringUtils.isNotEmpty(amountDue)) {
										resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);
									}
								}
							}
						}
					}
				}
			}
			
			resultMap.put("OtherInformationSet.SrcType","TR");
			
		} catch (Exception e) {
			logger.error("Error while getting details", e);
		}
		return null;
	}
	
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();		
		
		FilterResponse taxYearFilter = new TaxYearFilterResponse(searchId);
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		
		// search by PIN
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		if(!isEmpty(pin)){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));			
			module.clearSaKeys();
			module.getFunction(2).forceValue(pin); 
			module.getFunction(5).forceValue("0");
			module.getFunction(6).forceValue("0");
			module.addFilter(taxYearFilter);
			modules.add(module);
		}
		
		// search by Address
		String strNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		String strName = getSearchAttribute(SearchAttributes.P_STREETNAME);
		boolean hasAddress = !isEmpty(strNo) && !isEmpty(strName);
		if(hasAddress){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(2).forceValue(strNo);  
			module.getFunction(3).forceValue(strName);
			module.getFunction(5).forceValue("All");
			module.getFunction(7).forceValue("0");
			module.getFunction(8).forceValue("0");
			module.addFilter(addressFilter);
			module.addFilter(taxYearFilter);
			modules.add(module);			
		}
		if(!isEmpty(strName)){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(3).forceValue(strName);
			module.getFunction(5).forceValue("All");
			module.getFunction(7).forceValue("0");
			module.getFunction(8).forceValue("0");
			module.addFilter(addressFilter);
			module.addFilter(taxYearFilter);
			modules.add(module);			
		}
		
		// search by name - filter by address
		if(hasOwner()) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.getFunction(5).forceValue("0");
			module.getFunction(6).forceValue("0");
			module.addFilter(NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null));
			module.addFilter(addressFilter);
			module.addFilter(taxYearFilter);
			
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(module, searchId, new String[] {"L;F;",});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		} 

		serverInfo.setModulesForAutoSearch(modules);
	}
}
