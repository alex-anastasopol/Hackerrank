package ro.cst.tsearch.servers.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult.Status;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.TiffConcatenator;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.Image;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.ImageI.IType;

@SuppressWarnings("deprecation")
public class KSJohnsonAOM extends TSServer {

	/**
	 * @author MihaiB
	 * 
	 */
	private static final long serialVersionUID = -1221988288078469076L;
	
	private static final String TYPE	= "Type";
	private static final String NAME 	= "Name";
	private static final String VALUE 	= "Value";
	
	private static final String ADDRESS_TYPE 		= "Address";
	private static final String NAME_TYPE 			= "Owner";
	@SuppressWarnings("unused")
	private static final String KUPN_TYPE 			= "KUPN";
	@SuppressWarnings("unused")
	private static final String PROPERTY_ID_TYPE 	= "Property ID";
	
	private static final String BUILDING_IMAGES_KEY = "BUILDING_IMAGES_KEY";
	
	private static final String REQUEST_FOR_PICS 	= "pics";
	private static final String REQUEST_FOR_COORDS 	= "getpolys";
	private static final String[] TYPES_FOR_REQUESTS = new String[4];
	static{
		TYPES_FOR_REQUESTS[0] = "additionalowners";
		TYPES_FOR_REQUESTS[1] = "detail";
		TYPES_FOR_REQUESTS[2] = "specials";
		TYPES_FOR_REQUESTS[3] = "near";
	}

	public KSJohnsonAOM(long searchId) {
		super(searchId);
	}

	public KSJohnsonAOM(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {

		int serverInfoModuleID = module.getModuleIdx();
		if (serverInfoModuleID == 0) {
			String sParcelNo = module.getFunction(0).getParamValue();
			if (sParcelNo != null && sParcelNo.matches("(?i)\\s*[\\dA-Z]{8}\\s+[\\dA-Z]{3,5}\\s*")) {
				// replace YYYYYYYY XXXX with YYYYYYYY-XXXX
				// e.g. replace "3F221328 2006" with "3F221328-2006"
				// otherwise results aren't fetched
				String[] parcelTokens = sParcelNo.trim().split("\\s+");
				sParcelNo = parcelTokens[0] + "-" + parcelTokens[1];
				module.getFunction(0).setParamValue(sParcelNo);
			}
		}
		return super.SearchBy(module, sd);
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;

		FilterResponse addressFilter = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		
		PinFilterResponse  pinFilter = new PinFilterResponse(SearchAttributes.LD_PARCELNO, searchId);

		// search by parcel ID
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();			
			module.setSaKey(0, SearchAttributes.LD_PARCELNO_GENERIC_AO);
			module.addFilter(pinFilter);
			modules.add(module);
		}

		// search by address
		if (hasStreet()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaKey(0, SearchAttributes.P_FULL_ADDRESS_N_D_N_S );
			module.addFilter(pinFilter);
			module.addFilter(addressFilter);
			modules.add(module);
		}

		// search by owner
		if (hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);

			module.addFilter(pinFilter);
			module.addFilter(nameFilterHybrid);

			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
														.getConfigurableNameIterator(module, searchId, new String[] {"L, F;;"});
			module.addIterator(nameIterator);

			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String result = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		if (result.contains("Could not find any information for")) {
			Response.getParsedResponse().setError("No Records Found.");
			return;
		}
		
		switch (viParseID) {
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_ADDRESS:
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, result, outputTable);

				if (smartParsedResponses.size() == 0) {
					return;
				}

				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());

				break;
								
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				
				String details = getDetails(result, Response);
				
				String docNo = "";
				HtmlParser3 htmlparser3 = new HtmlParser3(details);
				NodeList mySpanList = htmlparser3.getNodeById("spanTaxPropertyID").getChildren();
				if (mySpanList != null && mySpanList.size() > 0){
					docNo = mySpanList.elementAt(0).toPlainTextString().trim();
					docNo = docNo.replaceAll("\\s+", "");
					
//					String linkImage = CreatePartialLink(TSConnectionURL.idGET) + "look_for_images&id=" + docNo;
					LinkedHashSet<String> links = (LinkedHashSet<String>) getSearch().getAdditionalInfo(BUILDING_IMAGES_KEY + docNo);
					if (links != null && links.size() > 0){
						for (String link : links) {
							Response.getParsedResponse().addImageLink(new ImageLinkInPage(link, docNo + ".tiff"));
						}
					}
//					Response.getParsedResponse().addImageLink(new ImageLinkInPage (linkImage, docNo + ".tiff" ));
				}
					
				if (viParseID == ID_SAVE_TO_TSD){
					
					DocumentI doc = smartParseDetails(Response, details);
					doc.setSearchType(SearchType.CS);
					LinkedHashSet<String> links = (LinkedHashSet<String>) getSearch().getAdditionalInfo(BUILDING_IMAGES_KEY + docNo);
					if (links != null && links.size() > 0){
						ImageI image = doc.getImage();
						if (image == null){
							image =  new Image();
						}
						image.setLinks(links);
						for (String link : links) {
							getSearch().addImagesToDocument(doc, link);
						}
						doc.setImage(image);
					}
					
	                msSaveToTSDFileName = docNo.trim() + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
	                
	            } else{
	            	
	            	String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type", "ASSESSOR");
						
					if (isInstrumentSaved(docNo, null, data)){
	                	details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
					
					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
		
	            }
				break;
			
			case ID_GET_LINK:
				ParseResponse(sAction, Response, ID_DETAILS);
				break;
		}
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String result, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		ParsedResponse parsedResponse = response.getParsedResponse();

		outputTable.append("<table BORDER='1' CELLPADDING='2'>");
		
		try {
			JSONObject jsonResponse = new JSONObject(result);
			
			if (jsonResponse != null){
				JSONArray jsonRows = jsonResponse.getJSONArray("d");
				if (jsonRows != null && jsonRows.length() > 0){
					StringBuffer header = new StringBuffer();
					for (int i = 0; i < jsonRows.length(); i++){
						JSONObject jsonObject = jsonRows.getJSONObject(i);
						if (jsonObject != null){
							if (jsonObject.has(TYPE) && header.length() == 0){
								header.append("<tr><th>").append((String) jsonObject.get(TYPE)).append("</th><th>Property ID</th></tr>");
							}
							if (jsonObject.has(NAME) && jsonObject.has(VALUE)){
								String name = (String) jsonObject.get(NAME);
								String value = (String) jsonObject.get(VALUE);
								
								StringBuffer row = new StringBuffer();
								StringBuffer link = new StringBuffer();
								link.append(CreatePartialLink(TSConnectionURL.idPOST)).append("/ajaxreq.aspx?id=").append(value);
								row.append("<tr><td>").append(name).append("</td><td>")
									.append("<a href=\"").append(link).append("\">").append(value).append("</a></td></tr>");
								
								ParsedResponse currentResponse = new ParsedResponse();
								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toString());
								currentResponse.setOnlyResponse(row.toString());
								currentResponse.setPageLink(new LinkInPage(link.toString(), link.toString(), TSServer.REQUEST_SAVE_TO_TSD));
								currentResponse.setUseDocumentForSearchLogRow(true);

								ResultMap resultMap = new ResultMap();
								resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
								resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), value.replaceAll("\\s+", ""));
								
								Calendar cal = Calendar.getInstance();
								String year = FormatDate.getDateFormat(FormatDate.PATTERN_yyyy).format(cal.getTime());
								resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
								
								name = name.replaceFirst("(?is)([^\\|]+).*", "$1");
								name = name.trim();
								
								if (ADDRESS_TYPE.equalsIgnoreCase(jsonObject.getString(TYPE))){
									resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), name);
									resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(name));
									resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(name));
								} else if (NAME_TYPE.equalsIgnoreCase(jsonObject.getString(TYPE))){
									parseNames(name, resultMap);
								}
								
								Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
								
								DocumentI document = (AssessorDocumentI) bridge.importData();
								document.setSearchType(SearchType.CS);
								currentResponse.setDocument(document);
								
								intermediaryResponse.add(currentResponse);
								
								outputTable.append(currentResponse.getResponse());
							}
						}
					}
					parsedResponse.setHeader("<table id=\"intermediaryResults\" align=\"center\" cellspacing=\"0\" cellpadding=\"2\" border=\"1\" width=\"65%\">"
							+ header.toString());
					
					parsedResponse.setFooter("</table>");
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		outputTable.append("</table>");
		
		return intermediaryResponse;
	}

	/**
	 * @param name
	 * @param resultMap
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public void parseNames(String name, ResultMap resultMap) throws Exception {
		ArrayList<List> body = new ArrayList<List>();
		
		String[] owners = name.split("##@@##");
		for (String ow: owners) {
			if (!StringUtils.isEmpty(ow)) {
				String[] names = StringFormats.parseNameNashville(ow, true);

				String[] type = GenericFunctions.extractAllNamesType(names);
				String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
				String[] suffixes = GenericFunctions.extractNameSuffixes(names);

				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1],
						type, otherType, NameUtils.isCompany(names[2]),
						NameUtils.isCompany(names[5]), body);
			}
		}
		
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		name = name.replaceAll("(?is)\\s*##@@##\\s*", " & ");
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
		
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
		
		detailsHtml = detailsHtml.replaceAll("(?is)</?a[^>]*>", "");
		
		HtmlParser3 htmlparser3 = new HtmlParser3(detailsHtml);
		NodeList pidSpanList = htmlparser3.getNodeById("spanTaxPropertyID").getChildren();
		if (pidSpanList != null && pidSpanList.size() > 0){
			String pid = pidSpanList.elementAt(0).toPlainTextString().trim();
			pid = pid.replaceAll("\\s+", "");
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pid);
		}
		NodeList legalSpanList = htmlparser3.getNodeById("spanLegalDesc").getChildren();
		if (legalSpanList != null && legalSpanList.size() > 0){
			String legal = legalSpanList.elementAt(0).toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal);
			resultMap.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), legal);
		}

		NodeList subdSpanList = htmlparser3.getNodeById("spanPlatName").getChildren();
		if (subdSpanList != null && subdSpanList.size() > 0){
			String subd = subdSpanList.elementAt(0).toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subd);
		}
		
		NodeList blockSpanList = htmlparser3.getNodeById("spanGeoBlock").getChildren();
		if (blockSpanList != null && blockSpanList.size() > 0){
			String block = blockSpanList.elementAt(0).toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), StringUtils.stripStart(block, "0"));
		}
		NodeList lotSpanList = htmlparser3.getNodeById("spanGeoLot").getChildren();
		if (lotSpanList != null && lotSpanList.size() > 0){
			String lot = lotSpanList.elementAt(0).toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), StringUtils.stripStart(lot, "0"));
		}
		
		NodeList pbSpanList = htmlparser3.getNodeById("spanRODBook").getChildren();
		if (pbSpanList != null && pbSpanList.size() > 0){
			String pb = pbSpanList.elementAt(0).toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), StringUtils.stripStart(pb, "0"));
		}
		
		NodeList ppSpanList = htmlparser3.getNodeById("spanRODPage").getChildren();
		if (ppSpanList != null && ppSpanList.size() > 0){
			String pp = ppSpanList.elementAt(0).toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), StringUtils.stripStart(pp, "0"));
		}
		
		NodeList strSpanList = htmlparser3.getNodeById("spanTRS").getChildren();
		if (strSpanList != null && strSpanList.size() > 0){
			String str = strSpanList.elementAt(0).toPlainTextString().trim();
			String[] strs = str.split("\\s*-\\s*");
			if (strs.length == 3){
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), StringUtils.stripStart(strs[0], "0"));
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), StringUtils.stripStart(strs[1], "0"));
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), StringUtils.stripStart(strs[2], "0"));
			}
		}
		
		NodeList addressSpanList = htmlparser3.getNodeById("spanSitAddline1").getChildren();
		if (addressSpanList != null && addressSpanList.size() > 0){
			String address = addressSpanList.elementAt(0).toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			
			resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
			resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
		}
		
		StringBuffer owners = new StringBuffer();
		NodeList firstOwnerSpanList = htmlparser3.getNodeById("spanOwner1FullName").getChildren();
		if (firstOwnerSpanList != null && firstOwnerSpanList.size() > 0){
			String owner = firstOwnerSpanList.elementAt(0).toPlainTextString().trim();
			owners.append(owner);
		}
		
		NodeList secondOwnerSpanList = htmlparser3.getNodeById("spanOwner2FullName").getChildren();
		if (secondOwnerSpanList != null && secondOwnerSpanList.size() > 0){
			String owner = secondOwnerSpanList.elementAt(0).toPlainTextString().trim();
			if (StringUtils.isNotEmpty(owner)){
				if (owner.contains(",")){
					owners.append("##@@##").append(owner);
				} else{
					owners.append(" ").append(owner);
				}
			}
		}
		if (owners.length() > 0){
			try {
				parseNames(owners.toString(), resultMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		Calendar cal = Calendar.getInstance();
		String year = FormatDate.getDateFormat(FormatDate.PATTERN_yyyy).format(cal.getTime());
		resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), year);
		
		return null;
	}
	
	protected String getDetails(String response, ServerResponse Response){
		
		// if from memory - use it as is
		if (response.toLowerCase().contains("tblinfo")){
			return response;
		}
		
		if (!response.contains("<TaxPropertyID>")){
			return "";
		}
		HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
		String formular = "";
		LinkedHashSet<String> imageLinks =  new LinkedHashSet<String>();
		StringBuffer LatLong = new StringBuffer();
		StringBuffer coords = new StringBuffer();
		
		String pid = "";
		//first bring the blank form
		if (site != null) {
			try {
				
				String link = dataSite.getLink();
				String rsp = ((ro.cst.tsearch.connection.http3.KSJohnsonAOM) site).getPage(link, null);
				if (StringUtils.isNotEmpty(rsp)){
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsp, null);
					NodeList mainList = htmlParser.parse(null);
					if (mainList != null && mainList.size() > 0){
						NodeList tableList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
														.extractAllNodesThatMatch(new HasAttributeFilter("id", "tblInfo"), true);
						if (tableList != null && tableList.size() > 0){
							formular = tableList.elementAt(0).toHtml();
						}
					}
				}
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
				NodeList mainList = htmlParser.parse(null);
				if (mainList != null && mainList.size() > 0){
					mainList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
					if (mainList.size() > 0){
						NodeList mininodes = mainList.elementAt(0).getChildren();
						for (int i = 0; i < mininodes.size(); i++){
							Node node = mininodes.elementAt(i);
							if (node instanceof TagNode){
								String nod = node.getText();
								if (nod.startsWith("/")){
									continue;
								}
								String value = node.getNextSibling().toPlainTextString();
								formular = formular.replaceFirst("(?is)(<span id=\\\"span" + nod + "\\\"[^>]*>)\\s*(</span>)", "$1" + value + "$2");
								if ("TaxPropertyID".equalsIgnoreCase(nod)){
									pid = value;
								}
							}
						}
					}
				}
				
				for (int i = 0; i < TYPES_FOR_REQUESTS.length; i++){
					rsp = ((ro.cst.tsearch.connection.http3.KSJohnsonAOM) site).getPage(link + "/ajaxreq.aspx?id=" + pid + "&type=" + TYPES_FOR_REQUESTS[i], null);
					if (StringUtils.isNotEmpty(rsp)){
						htmlParser = org.htmlparser.Parser.createParser(rsp, null);
						mainList = htmlParser.parse(null);
						
						if (mainList != null){
							mainList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
							if (mainList.size() > 0){
								
								if (TYPES_FOR_REQUESTS[i].equalsIgnoreCase(TYPES_FOR_REQUESTS[2])){
									try {
										StringBuffer specialInfoTable = new StringBuffer();
										for (int s = 0; s < mainList.size(); s++) {
											specialInfoTable.append("<tr>");
											NodeList mininodes = mainList.elementAt(s).getChildren();
											String specialCode = "", specialDescShort = "", specialDescLong = "", principal = "";
											for (int n = 0; n < mininodes.size(); n++) {
												Node node = mininodes.elementAt(n);
												if (node instanceof TagNode) {
													String nod = node.getText();
													if (nod.startsWith("/")) {
														continue;
													} else if ("SpecialCode".equalsIgnoreCase(nod)) {
														specialCode = node.getNextSibling().toPlainTextString();
													} else if ("SpecialDescShort".equalsIgnoreCase(nod)) {
														specialDescShort = node.getNextSibling().toPlainTextString();
													} else if ("SpecialDescLong".equalsIgnoreCase(nod)) {
														specialDescLong = node.getNextSibling().toPlainTextString();
													} else if ("Principal".equalsIgnoreCase(nod)) {
														principal = node.getNextSibling().toPlainTextString();
													}
												}
											}
											specialInfoTable.append("<td width=\"30%\" title=\"" + specialDescLong + "\">").append("<label>")
													.append(specialDescShort).append(" (").append(specialCode).append("):</label></td>");
											specialInfoTable.append("<td><label>$").append(principal.replaceFirst("(\\.\\d{2})\\d{1,2}$", "$1"))
													.append("</label></td>");
											specialInfoTable.append("</tr>");
										}
										if (specialInfoTable.length() > 0) {
											formular = formular.replaceFirst("(?is)(id=\\\"tblSpecialsContent\\\"[^>]*>)",
													"$1" + Matcher.quoteReplacement(specialInfoTable.toString()));
										}
									} catch (Exception e) {
										logger.error("Error on retrieving or parsing html for Special Information: " + searchId + " :\n " + e);
									}
								} else{
									NodeList mininodes = mainList.elementAt(0).getChildren();
									for (int n = 0; n < mininodes.size(); n++){
										Node node = mininodes.elementAt(n);
										if (node instanceof TagNode){
											String nod = node.getText();
											if (nod.startsWith("/")){
												continue;
											}
											String value = node.getNextSibling().toPlainTextString();
											formular = formular.replaceFirst("(?is)(<span id=\\\"span" + nod + "\\\"[^>]*>)\\s*(</span>)", "$1" + value + "$2");
											
											if ("Latitude".equalsIgnoreCase(nod)){
												LatLong.append(value).append(",");
											} else if ("Longitude".equalsIgnoreCase(nod)){
												LatLong.append(value);
											}
										}
									}
								}
							}
						}
					}
				}
				
				//get coords
				rsp = ((ro.cst.tsearch.connection.http3.KSJohnsonAOM) site).getPage(link + "/ajaxreq.aspx?id=" + pid + "&type=" + REQUEST_FOR_COORDS, null);
				if (StringUtils.isNotEmpty(rsp)){
					htmlParser = org.htmlparser.Parser.createParser(rsp, null);
					mainList = htmlParser.parse(null);
					
					if (mainList != null){
						mainList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
						if (mainList.size() > 0){
							NodeList mininodes = mainList.elementAt(0).getChildren();
							for (int n = 0; n < mininodes.size(); n++){
								Node node = mininodes.elementAt(n);
								if (node instanceof TagNode){
									String nod = node.getText();
									if (nod.startsWith("/")){
										continue;
									}
									if ("Coords".equalsIgnoreCase(nod)){
										String value = node.getNextSibling().toPlainTextString();
										if (StringUtils.isNotEmpty(value)){
											value = value.replaceAll("POLYGON \\(\\(", "");
											value = value.replaceAll("\\)\\)", "");
											String[] coor = value.split("\\s*,\\s*");
											for (String coordPeer : coor){
												String[] LL = coordPeer.split("\\s+");
												if (LL.length == 2){
													coords.append("%7C").append(LL[1]).append(",").append(LL[0]);
												}
											}
										}
									}
								}
							}
						}
					}
				}
				String linkGM = "http://maps.googleapis.com/maps/api/staticmap?sensor=false&center=" + LatLong.toString() 
						+ "&scale=1&zoom=18&maptype=roadmap&format=jpg-baseline&markers=size:mid%7Ccolor:green%7Clabel:A%7C" + LatLong.toString()
						+ "&size=640x640&path=color:0x5756D4ff%7Cweight:3" + coords.toString();
				imageLinks.add(linkGM);
				
				//get building images links
				rsp = ((ro.cst.tsearch.connection.http3.KSJohnsonAOM) site).getPage(link + "/ajaxreq.aspx?id=" + pid + "&type=" + REQUEST_FOR_PICS, null);
				if (StringUtils.isNotEmpty(rsp)){
					htmlParser = org.htmlparser.Parser.createParser(rsp, null);
					mainList = htmlParser.parse(null);
					if (mainList != null){
						mainList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
						if (mainList.size() > 0){
							for (int i = 0; i < mainList.size(); i++) {							
								NodeList mininodes = mainList.elementAt(i).getChildren();
								for (int n = 0; n < mininodes.size(); n++){
									Node node = mininodes.elementAt(n);
									if (node instanceof TagNode){
										String nod = node.getText();
										if (nod.startsWith("/")){
											continue;
										}
										String value = node.getNextSibling().toPlainTextString();
										if ("Source".equalsIgnoreCase(nod) && "Pics".equalsIgnoreCase(value)){
											break;
										}
										if ("DocumentType".equalsIgnoreCase(nod) && !"Front Elevation".equalsIgnoreCase(value)){
											break;
										}
										if ("FullPath".equalsIgnoreCase(nod)){
											imageLinks.add(value);
										}
									}
								}
							}
						}
					}
				}
			} catch (ParserException e) {
				e.printStackTrace();
			} finally {
				// always release the HttpSite
				HttpManager3.releaseSite(site);
			}
		}
		
		if (imageLinks.size() > 0){
			getSearch().setAdditionalInfo(BUILDING_IMAGES_KEY + pid.replaceAll("\\s+", ""), imageLinks);
		}
		formular = formular.replaceAll("(?is)&lt;", "<");
		formular = formular.replaceAll("(?is)&gt;", ">");
		formular = formular.replaceAll("(?is)<img[^>]*>", "");
		formular = formular.replaceAll("(?is)</?body[^>]*>", "");
		formular = formular.replaceAll("(?is)<a[^>]*>([^<]*)</a>", "$1");
		formular = formular.replaceAll("(?is)display:none;", "");
		formular = formular.replaceAll("(?i)>.*?click[^<]*", ">");
		formular = formular.replaceAll("<span[^>]*>\\s*\\[Collapse All\\]\\s*</span>", "");
		formular = formular.replaceAll("(?is)(<td[^>]*>)\\s*(</?td[^>]*>)", "$1&nbsp;$2");
		
		formular = formular.replaceAll("(?i)<table ", "<table border=\"1\" ");
		formular = formular.replaceAll("(?i)<table ", "<table style=\"width:970px;margin-left:auto;margin-right:auto;\" ");
		formular = formular.replaceAll("(?is)(<td[^>]*>[^<]*<iframe.*?</td>\\s*)</tr>\\s*</table>", "</td></tr></table>");
		//formular = formular.replaceAll("(?is)(<td[^>]*>[^<]*<iframe.*?</td>\\s*)</tr>\\s*</table>", "<td><iframe width=\"900\" height=\"500\" frameborder=\"0\" scrolling=\"no\" marginheight=\"0\" marginwidth=\"0\" src=\"http://maps.jocogov.org/ims/frame.aspx?z=17&ll=38.8812,-94.82005&op=0.5&ls=vb,yq&bm=0&w=900&h=500\"></iframe></td></tr></table>");
		String linkImage = CreatePartialLink(TSConnectionURL.idGET) + "look_for_images&id=" + pid.replaceAll("\\s+", "");
		formular += "<br><br><br><a href=\"" + linkImage +"\" target=\"_blank\">View Image</a>";
		
		return formular;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ServerResponse GetLink(String vsRequest, boolean vbEncoded)throws ServerResponseException {
		 ServerResponse sr = null;
		 if (vsRequest.contains("look_for_images")){
			 String pid = ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(vsRequest, "id");
			 if (StringUtils.isNotEmpty(pid)){
				 ImageI i = new Image();
				 i.setContentType("image/tiff");
				 String filename = "" + (new Random()).nextInt(900000000) + ".tiff";
				 i.setFileName(filename);
				 i.setPath(getSearch().getImagesTempDir() + filename);
				 i.setType(IType.TIFF);
				 i.setExtension("tiff");
				 Set<String> links =  (Set<String>) getSearch().getAdditionalInfo(BUILDING_IMAGES_KEY + pid);
				 i.setLinks(links);
				 
				 DownloadImageResult result = saveImage(i);
				 
				 if(result != null && Status.OK.equals(result.getStatus())){
					 writeImageToClient(i.getPath(), i.getContentType());
					 return null;
				 }
			 }
		 } else {
			 return super.GetLink(vsRequest, vbEncoded);
		 }
		 return sr;
	}
	
	@Override
	protected DownloadImageResult saveImage(ImageI image) throws ServerResponseException {
		try{
			
			Set<String> links = image.getLinks();
			List<byte[]> pagesPIC = new ArrayList<byte[]>();
			List<byte[]> pagesGM = new ArrayList<byte[]>();
			
			HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
			if (site != null) {
				try {
					for (String link : links) {
						try {
							HTTPResponse response = ((ro.cst.tsearch.connection.http3.KSJohnsonAOM) site).process(new HTTPRequest(link));
							if (link.contains("maps.googleapis.com")){
								pagesGM.add(response.getResponseAsByte());
							} else{
								pagesPIC.add(response.getResponseAsByte());
							}
						} catch (Exception e) {
						}
					}
				} finally {
					// always release the HttpSite
					HttpManager3.releaseSite(site);
				}
			}
			
			DownloadImageResult imageResult = new DownloadImageResult();

			pagesGM.addAll(pagesPIC);
			imageResult.setContentType("image/tiff");
			imageResult.setImageContent(TiffConcatenator.concatenatePngInTiff(pagesGM, "JPEG", 0.3f));
			imageResult.setStatus(DownloadImageResult.Status.OK);
			
			if (StringUtils.isEmpty(image.getPath())){
				image.setPath(getSearch().getImageDirectory() + File.separator + image.getFileName());
			}
			FileUtils.writeByteArrayToFile(new File(image.getPath()), imageResult.getImageContent());
			
			return imageResult;
		} catch (Exception e) {
			if (image == null || image.getLinks() == null || image.getLinks().isEmpty()) {
				logger.error("Could not download image because there is no data ", e);
			}
		}
		return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
	}
}
