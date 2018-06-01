package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.fornext.ParcelNumberFilterResponseForNext;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterForNext;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;



public class TNSullivanTR extends TSServer {
	
	private static final long serialVersionUID = 1L;
	
	private static final Pattern NEXT_PAT = Pattern.compile("(?i)href=\\\"([^\\\"]+)\\\">More\\b");
	private static final Pattern DEED_BOOK_PAT = Pattern.compile("(?is)\\bDeed\\s+Book\\s*:\\s*(\\w+)\\s*Page\\s*:\\s*(\\w+)\\s*Date\\s*:\\s*(\\w+)\\b");

	
	public TNSullivanTR(long searchId){
		super(searchId);
	}
	
	public TNSullivanTR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,	int miServerID){
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		TSServerInfoModule module = null;
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.80d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , module);
		FilterResponse pinFilter = new PinFilterResponse(SearchAttributes.LD_PARCELNO, searchId){
			
			private static final long serialVersionUID = 1L;
			
			@Override
			protected Set<String> getRefPin(){
				Set<String> ret = new HashSet<String>();
				ret.addAll(Arrays.asList(sa.getAtribute(saKey).trim().split(",")));
				
				Set<String> retPrepared = new HashSet<String>();
				for (String parcel : ret) {
					if (StringUtils.isNotEmpty(parcel) && !parcel.startsWith("-")){
						parcel = parcel.substring(parcel.indexOf("-"));
						parcel = parcel.replaceAll("\\p{Punct}", "");
						retPrepared.add(parcel);
					}
				}
				
				if (retPrepared.size() > 0){
					ret.addAll(retPrepared);
				}

				parcelNumber = ret;
				return ret;
			}
		};
		
		// search by MapNo from PIN
		String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
		
		if(hasPin()) {
//	    	String [] parts = pin.split("-");
//	    	if (parts.length > 3){
//	    		
//	    		String ctrlMap	= parts[2];
//		    	String grp     	= parts[1];
//		    	String parcel  	= parts[3];
		    	
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_PLAT_MODULE_IDX));			
				module.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO_CTRL_MAP_GENERIC_TR);
				module.getFunction(9).setSaKey(SearchAttributes.LD_PARCELNO_GROUP_GENERIC_TR);
				module.getFunction(10).setSaKey(SearchAttributes.LD_PARCELNO_PARCEL_GENERIC_TR);
				module.addFilterForNext(new ParcelNumberFilterResponseForNext(pin.substring(pin.indexOf("-")).replaceAll("\\p{Punct}", ""), searchId));
				module.addFilter(addressFilter);
				module.addFilter(pinFilter);
				module.addFilter(nameFilterHybrid);
				modules.add(module);
//			}
		}
		
		// search by Address
		if(hasStreet() && hasStreetNo()){
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			
			module.getFunction(0).setSaKey(SearchAttributes.P_STREET_NA_SU_NO);
			module.addFilterForNextType(FilterResponse.TYPE_ADDRESS_FOR_NEXT);
			module.addFilter(addressFilter);
			module.addFilter(pinFilter);
			module.addFilter(nameFilterHybrid);
			modules.add(module);			
		}
		
		// search by name
		if(hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));

			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.addFilterForNext(new NameFilterForNext(module.getSaObjKey(), searchId, module, false));
			module.addFilter(addressFilter);
			module.addFilter(pinFilter);
			module.addFilter(nameFilterHybrid);
			
			module.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
															.getConfigurableNameIterator(module, searchId, new String[] {"L F;;"});
			module.addIterator(nameIterator);
			
			modules.add(module);			
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
	
	/**
	 * @param rsResponse
	 * @param viParseID
	 */
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException{
		
		String rsResponse = Response.getResult();
		
		if (rsResponse.indexOf("No Records Found") >= 0 || rsResponse.contains("KeyMore=\"")) {
			Response.getParsedResponse().setError("No Records Found.");
			return;
		}
		
		switch (viParseID) {
		case ID_SEARCH_BY_NAME:			
			try {
				 
				rsResponse = Tidy.tidyParse(rsResponse, null);
				
				StringBuilder outputTable = new StringBuilder();
				ParsedResponse parsedResponse = Response.getParsedResponse();
																	
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);
				
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
	            }
				
			} catch(Exception e) {
				e.printStackTrace();
			}
			break;
			
		case ID_SAVE_TO_TSD :
		case ID_DETAILS :
			
        	String details = getDetails(rsResponse);
        	
        	StringBuffer accountId = new StringBuffer();
        	String taxYearString = "";
        	
        	try {
    			
    			HtmlParser3 parser= new HtmlParser3(details.replaceAll("<th(.*?)>", "<td$1>").replaceAll("</th>", "</td>").replaceAll("</b>", ""));
    			NodeList nodeList = parser.getNodeList();
    			
    			String ctlmap = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "CtlMap"), "", false).trim();
    			String grp = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Grp"), "", false).trim();
    			    			
    			String parcel = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Parcel"), "", false).trim();
    			
    			String si = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "S/I"), "", false).trim();
    			String mapno = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "MapNo"), "", false).trim();
    			
    			accountId.append(mapno).append("-").append(grp).append("-").append(ctlmap).append("-").append(parcel).append("--").append(si);
    			
    			String title = parser.getNodeByTypeAndAttribute("p", "class", "Title", true).getFirstChild().toPlainTextString();
    			if (StringUtils.isNotEmpty(title)){
    				title = title.replaceAll("(?is)&nbsp;", " ");
    				taxYearString = title.substring(0, title.indexOf("-"));
    				taxYearString = taxYearString.replaceAll("(?is)[\\(\\)-]+", "").trim();
    			}
    			
    		} catch (Exception e){
    			e.printStackTrace();
    		}
        	
			if (viParseID == ID_SAVE_TO_TSD){
				
				smartParseDetails(Response, details);
				
                msSaveToTSDFileName = accountId.toString().trim() + ".html";
                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
                
            } else{
            	           	
            	String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
				
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "CNTYTAX");
				data.put("year", taxYearString);

				if (isInstrumentSaved(accountId.toString().trim(), null, data)){
					details += CreateFileAlreadyInTSD();
				} else{
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);
            }
			
			break;
			
		case ID_GET_LINK:
			if (sAction.indexOf("navigate=display") >= 0 || Response.getQuerry().contains("KeyMore")) {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			} else if (Response.getQuerry().indexOf("Action=Detail") >= 0){
				ParseResponse(sAction, Response, ID_DETAILS);
			}
			break;
		default:
			break;
		}
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			 			
			org.htmlparser.Parser tableParser = org.htmlparser.Parser.createParser(table, null);
			NodeList mainTableList = tableParser.parse(new TagNameFilter("table"));
			
			String header = "";
			TableTag mainTable = null;
			
			if (mainTableList != null){
				for(int i = 0; i < mainTableList.size(); i++){
					String eachTable = mainTableList.elementAt(i).toHtml().toLowerCase();
					if (eachTable.contains("property address")){
						header = mainTableList.elementAt(i).toHtml();
					} else if (eachTable.contains("unpd") || eachTable.contains("paid") || eachTable.contains("delq")){
						mainTable = (TableTag) mainTableList.elementAt(i);
					}
					if (StringUtils.isNotEmpty(header) && mainTable != null){
						break;
					}
				}
				
				if (mainTable != null){
					TableRow[] rows = mainTable.getRows();
					for(TableRow row : rows ) {
						if(row.getColumnCount() > 10){
							
							String link = HtmlParser3.getFirstTag(row.getColumns()[0].getChildren(), LinkTag.class, true).getLink();		
							
							if (getSearch().getSearchType() == Search.AUTOMATIC_SEARCH){
								link = link.replaceAll("(?is)&amp;", "&");
							}
							link = link.replaceFirst("\\?", "?dummy=true&");
							link = CreatePartialLink(TSConnectionURL.idGET) + link;
							String rowHtml = row.toHtml();
							
							rowHtml = rowHtml.replaceAll("(?i)</?font[^>]*>", "");
							rowHtml = rowHtml.replaceAll("(?i)<a href.*?Action=ShowPTGrp[^>]*>([^<]*)</a>", "");
							rowHtml = rowHtml.replaceAll("(?s)<a.*?>(.*?)</a>", "<a href=\"" + Matcher.quoteReplacement(link) + "\">$1</a>");
							rowHtml = rowHtml.replaceAll("(?is)<!--.*?-->", "");
							
							ParsedResponse currentResponse = new ParsedResponse();
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
							currentResponse.setOnlyResponse(rowHtml);
							currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
							
							ResultMap m = parseIntermediaryRow(row, searchId);
							Bridge bridge = new Bridge(currentResponse, m ,searchId);
							
							DocumentI document = (TaxDocumentI) bridge.importData();				
							currentResponse.setDocument(document);
							
							intermediaryResponse.add(currentResponse);
						}
					}
				}
			}
		
			Form form = new SimpleHtmlParser(table).getForm("form1");
			if (form != null){
				Map<String, String> params = new HashMap<String, String>();
				String session = form.getParams().get("SESSION");
				
				int seq = getSeq();
				
				if (StringUtils.isNotEmpty(session)){
					params.put("SESSION", session);
					mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
				}
			}
			
			header = header.replaceAll("(?is)<!--.*?-->", "").replaceAll("(?is)</table>", "").replaceAll("(?is)<table([^>]*>)", "<table border=\"1\" $1");
			
//			if (!response.getQuerry().contains("Action=LookUp-More")){
//				mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsLinks:", null);
//			}
			Matcher mat = NEXT_PAT.matcher(table);
			if (mat.find()){
				String link = CreatePartialLink(TSConnectionURL.idGET) + mat.group(1);
				String nextLink = "<a href=\"" + link + "\">Next</a>";
				if (StringUtils.isNotEmpty(link)){
					header = "&nbsp;&nbsp;&nbsp;" + nextLink + "<br><br><br>" + header;
					
					if (getSearch().getSearchType() == Search.AUTOMATIC_SEARCH){
						response.getParsedResponse().setNextLink(nextLink);
					}
					
//					String keymoreFromQuery = ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(response.getQuerry(), "KeyMore");
//					keymoreFromQuery = keymoreFromQuery.replaceAll("(?is)\\s", "+");
//					
//					String keymore = ro.cst.tsearch.utils.StringUtils.extractParameterFromUrl(link, "KeyMore");
//
//					Map<String, String> params = (Map<String, String>) mSearch.getAdditionalInfo(getCurrentServerName() + ":paramsLinks:");
//					if (params == null){
//						params = new HashMap<String, String>();
//					} else{
//						String prevLink = params.get(keymoreFromQuery);
//						if (StringUtils.isNotEmpty(prevLink)){
//							prevLink = "<a href=\"" + prevLink + "\">Previous</a>";
//							header = "&nbsp;&nbsp;&nbsp;" + prevLink + header;
//						}
//					}
//					if (StringUtils.isNotEmpty(keymore) && StringUtils.isNotEmpty(link)){
//						params.put(keymore, link);
//					}
//					mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsLinks:", params);
				}
			}
			
			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ){
				response.getParsedResponse().setHeader(header);
				response.getParsedResponse().setFooter("</table>");
		    }
			
			outputTable.append(table);
				
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}

	private String getDetails(String response) {
		
		/* If from memory - use it as is */
		if(!response.contains("<html")){
			return response;
		}
		
		String result = "";
		response = Tidy.tidyParse(response, null);
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainList = htmlParser.parse(new TagNameFilter("body"));
			
			if (mainList != null){
				mainList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				
				result = mainList.toHtml();
				
				result = result.replaceAll("(?is)</?a[^>]*>", "");
				result = result.replaceAll("(?is)<img[^>]*>", "");
				result = result.replaceAll("(?is)<!--.*?-->", "");
				result = result.replaceAll("(?is)\\bborder=\\\"\\d+\\\"", "");
				result = result.replaceAll("(?is)<table ", "<table border=\"1\" ");
			}
		} catch (Exception e){
			e.printStackTrace();
		}

		return result;
		
	}

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
		
		TableColumn[] cols = row.getColumns();
		
		if (row != null){
			String grp = "", ctlmap = "", parcel = "", si = "";
			
			for(int i = 0; i < cols.length; i++){
				String contents = "";
				org.htmlparser.Node colText = null;
				try {
					colText = HtmlParser3.getFirstTag(cols[i].getChildren(), TextNode.class, true);
					if (colText != null){
						contents = colText.getText();
						contents = contents.replaceAll("(?is)&amp;", "&").replaceAll("(?is)&nbsp;", " ");
					}
				} catch (Exception e) {
					contents = "";
				}
				
				switch(i){
					case 0:
						resultMap.put(TaxHistorySetKey.RECEIPT_NUMBER.getKeyName(), contents.trim());
						break;
									
					case 1:
						resultMap.put(PropertyIdentificationSetKey.STATUS.getKeyName(), contents.trim());
						break;
			
					case 3:
						resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), contents.trim());
									
						List<List> body = new ArrayList<List>();
						String[] names = { "", "", "", "", "", "" };
						String[] suffixes, types, otherTypes;
						names = StringFormats.parseNameNashville(contents.trim(), true);
						
						types = GenericFunctions.extractAllNamesType(names);
						otherTypes = GenericFunctions.extractAllNamesOtherType(names);
						suffixes = GenericFunctions.extractNameSuffixes(names);
									
						GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
								NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
									
						try {
							GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
						} catch (Exception e) {
							e.printStackTrace();
						}
									
						break;
								
					case 4:
						resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(contents.trim()));
						resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(contents.trim()));
						break;
						
					case 5:
						ctlmap = contents.trim();
						break;
						
					case 6:
						grp = contents.trim();
						break;
						
					case 7:
						parcel = contents.trim();
						break;
									
					case 8:
						si = contents.trim();
						break;
									
					case 9:
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), contents.trim());
						break;
									
					case 10:
						String lot = contents.trim();
						lot = lot.replaceAll("(?is)[-|&]\\s*$", "");
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
						break;
									
		//			case 11:
		//				resultMap.put(PropertyIdentificationSetKey.PROPERTY_TYPE.getKeyName(), contents.trim());
		//				break;
									
					case 12:
						resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), contents.trim().replaceAll("(?is)[\\$,]+", ""));
						break;
					}
				}
			
				//on first position before first dash must be mapNo, but this value doesn't exist in intermediary results.
				String pid = "-" + grp + "-" + ctlmap + "-" + parcel + "--" + si;
				if (StringUtils.isNotEmpty(pid)){
					pid = pid.replaceAll("\\p{Punct}", "");
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pid.trim());
				}
			}
			
		
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object parseAndFillResultMap(ServerResponse serverResponse, String detailsHtml, ResultMap resultMap) {
		
		try {
			detailsHtml = detailsHtml.replaceAll("<th(.*?)>", "<td$1>").replaceAll("</th>", "</td>").replaceAll("</b>", "");
			
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(),"TR");
			
			int taxYearInt = -1;
			String taxYearString = "";
			
			HtmlParser3 parser= new HtmlParser3(detailsHtml);
			NodeList nodeList = parser.getNodeList();
			
			String ctlmap = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "CtlMap"), "", false).trim();
			String grp = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Grp"), "", false).trim();
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID_GROUP.getKeyName(), grp);
			
			String parcel = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Parcel"), "", false).trim();
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID_PARCEL.getKeyName(), parcel);
			
			String si = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "S/I"), "", false).trim();
			String dis = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Dis"), "", false).trim();
			String mapno = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "MapNo"), "", false).trim();
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID_MAP.getKeyName(), mapno);
			
			String parcelNo = mapno + "-" + grp + "-" + ctlmap + "-" + parcel + "--" + si;
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelNo);
			
			String landValue = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "Land Market Value:"), "", false).trim();
			if (StringUtils.isNotEmpty(landValue)){
				landValue = landValue.replaceAll("(?is)[\\$,]", "");
				resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), landValue);
			}
			String improvementValue = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(nodeList, "Improvement Value:"), "", false).trim();
			if (StringUtils.isNotEmpty(improvementValue)){
				improvementValue = improvementValue.replaceAll("(?is)[\\$,]", "");
				resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvementValue);
			}
			
			String title = parser.getNodeByTypeAndAttribute("p", "class", "Title", true).getFirstChild().toPlainTextString();
			if (StringUtils.isNotEmpty(title)){
				title = title.replaceAll("(?is)&nbsp;", " ");
				taxYearString = title.substring(0, title.indexOf("-"));
				taxYearString = taxYearString.replaceAll("(?is)[\\(\\)-]+", "").trim();
				resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYearString);
				
				try {
					taxYearInt = Integer.parseInt(taxYearString);
				} catch (Exception e) {	}
			}
			
			NodeList tableList = HtmlParser3.getNodeListByType(nodeList, "table", true);
			
			if (tableList != null){
				for (int i = 0; i < tableList.size(); i++){
					String table = tableList.elementAt(i).toHtml();
					
					if (table.toLowerCase().contains("mailing address")){
						TableTag ownerTable = (TableTag) tableList.elementAt(i);
						if (ownerTable != null){
							StringBuffer tmpOwnerAndAddress = new StringBuffer();
							TableRow[] rows = ownerTable.getRows();
							if (rows != null){
								for (int r = 1; r < rows.length; r++){
									TableColumn[] cols = rows[r].getColumns();
									if (cols.length == 1){
										String text = cols[0].getStringText();
										text = text.replaceAll("(?is)&amp;", "&");
										text = text.replaceAll("(?is)&nbsp;", " ");
										tmpOwnerAndAddress.append(text).append("  ");
									}
								}
								resultMap.put("tmpOwnerNameAddress", tmpOwnerAndAddress.toString());
							}
						}
					} else if (table.toLowerCase().contains("property class")){
						TableTag propertyTable = (TableTag) tableList.elementAt(i);
						if (propertyTable != null){
							TableRow[] rows = propertyTable.getRows();
							if (rows != null){
								for (int r = 0; r < rows.length; r++){
									TableColumn[] cols = rows[r].getColumns();
									if (cols.length == 1){
										String text = cols[0].getStringText();
										if (r == 3){
											text = text.replaceAll("(?is)&amp;", "&");
											text = text.replaceAll("(?is)&nbsp;", " ").trim();
											text = text.replaceAll("\\s*&\\s*", "-");
											String streetName = StringFormats.StreetName(text);
											String streetNo = StringFormats.StreetNo(text);
											streetNo = streetNo.replaceAll("\\s*-\\s*", " & ");
											
											resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
											resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
											resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), text);
										}
										if (text.contains("Deed Acres")){
											text = text.replaceFirst("(?is)\\bDeed Acres:\\s*", "").trim();
											if (text.startsWith(".")){
												text = "0" + text;
											}
											resultMap.put(PropertyIdentificationSetKey.ACRES.getKeyName(), text);
										} else if (text.contains("Deed Book")){
												Matcher mat = DEED_BOOK_PAT.matcher(text);
												if (mat.find()){
													List<String> list = new ArrayList<String>();
													list.add(StringUtils.stripStart(mat.group(1), "0"));
													list.add(StringUtils.stripStart(mat.group(2), "0"));
													String date = mat.group(3);
													if (date.length() == 6){
														String year = date.substring(4);
														int intValue = Integer.parseInt(year);
														if (intValue < 60){
															year = "20" + year;
														} else{
															year = "19" + year;
														}
														date = date.replaceFirst("(?is)\\d{2}$", year).replaceAll("(?is)(\\d{2})(\\d{2})(\\d{4})", "$1/$2/$3");
													} else{
														date = date.replaceAll("(?is)(\\d{2})(\\d{2})(\\d{4})", "$1/$2/$3");
													}
													list.add(date);
													List<List> body = new ArrayList<List>();
													body.add(list);
													
													if (!body.isEmpty() && body.size() > 0){
														String[] header= new String[]{"Book", "Page", "InstrumentDate"};
														ResultTable rt = new ResultTable();
														rt = GenericFunctions2.createResultTable(body, header);
														resultMap.put(SaleDataSet.class.getSimpleName(), rt);
													}
												}
											}
										}
									}
								}
							}
						} else if (table.toLowerCase().contains("subdivision data")){
							TableTag legalTable = (TableTag) tableList.elementAt(i);
							if (legalTable != null){
								String subName = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Subdivision Name"), "", false).trim();
								if (StringUtils.isNotEmpty(subName)){
									resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subName);
								}
								
								String platBook = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Plat Bk"), "", false).trim();
								String platPage = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(legalTable.getChildren(), "Page"), "", false).trim();
								if (StringUtils.isNotEmpty(platBook) && StringUtils.isNotEmpty(platPage)){
									resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), platBook);
									resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), platPage);
								}
								
								String block = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Block"), "", false).trim();
								if (StringUtils.isNotEmpty(subName)){
									resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
								}
								
								String lot = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(nodeList, "Lot"), "", false).trim();
								if (StringUtils.isNotEmpty(subName)){
									
									String lot2 = HtmlParser3.getValueFromAbsoluteCell(2, 0, HtmlParser3.findNode(nodeList, "Lot"), "", false).trim();
									if (StringUtils.isNotEmpty(lot2)){
										lot += "  " + lot2;
									}
									resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
								}
							}
						} else if (table.toLowerCase().contains("payment history")){
							TableTag taxTable = (TableTag) tableList.elementAt(i);
							if (taxTable != null){
								List<List<String>> taxHistory = HtmlParser3.getTableAsList(taxTable, false);
								List<String> taxList = null;
								List<List> taxBody = new ArrayList<List>();
								
								for (List<String> list : taxHistory){
									if (list.get(0).contains("Year"))
										continue;
									
									if (StringUtils.isNotEmpty(taxYearString) && taxYearString.equals(list.get(0))){
										String baseAmount = list.get(2);
										if (StringUtils.isNotEmpty(baseAmount)){
											baseAmount = baseAmount.replaceAll("[\\$,]+", "");
											resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount.trim());
										}
										String amountPaid = list.get(3);
										if (StringUtils.isNotEmpty(amountPaid)){
											amountPaid = amountPaid.replaceAll("[\\$,]+", "");
											resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid.trim());
											
											taxList = new ArrayList<String>();
											
											taxList.add(list.get(0));
											taxList.add(amountPaid);
											taxList.add(list.get(4));
											taxList.add(list.get(5));
											taxBody.add(taxList);
										}
									} else{
										String amountPaid = list.get(3);
										if (StringUtils.isNotEmpty(amountPaid)){
											taxList = new ArrayList<String>();
											taxList.add(list.get(0));
											taxList.add(list.get(3).replaceAll("[\\$,]+", ""));
											taxList.add(list.get(4));
											taxList.add(list.get(5));
											taxBody.add(taxList);
										}
									}
								}
								if (!taxBody.isEmpty() && taxBody.size() > 0){
									String[] header= new String[]{"Year", "ReceiptAmount", "ReceiptNumber", "ReceiptDate"};
									ResultTable rt = new ResultTable();
									rt = GenericFunctions2.createResultTable(taxBody, header);
									resultMap.put(TaxHistorySet.class.getSimpleName(), rt);
								}
							}
						} else if (table.toLowerCase().contains("interest and penalty")){
							TableTag dueTable = (TableTag) tableList.elementAt(i);
							if (dueTable != null){
								List<List<String>> dueList = HtmlParser3.getTableAsList(dueTable, false);
								Calendar firstDate = null;
								Calendar secondDate = null;
								Calendar now = Calendar.getInstance();
								
								String priorDelinq = "";
								
								for (List<String> dueRow : dueList){
									if (dueRow.get(0).contains("Year")){
										String firstDue = dueRow.get(4);
										firstDue = firstDue.replaceFirst("(?is)Due", "").trim();
										firstDate = FormatDate.getCalendarFromFormattedString(firstDue, FormatDate.MONTH_YEAR);
										
										String secDue = dueRow.get(7);
										secDue = secDue.replaceFirst("(?is)Due", "").trim();
										secondDate = FormatDate.getCalendarFromFormattedString(secDue, FormatDate.MONTH_YEAR);
										continue;
									} else if (dueRow.get(0).toLowerCase().contains("total")){
										break;
									}
									
									if (StringUtils.isNotEmpty(taxYearString) && taxYearString.equals(dueRow.get(0))){
										String totalDue = dueRow.get(4);
										
										if (firstDate != null && (now.get(Calendar.MONTH)) <= (firstDate.get(Calendar.MONTH))){
											if (StringUtils.isNotEmpty(totalDue)){
												totalDue = totalDue.replaceAll("[\\$,]+", "");
												resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue.trim());
											}
										} else{
											totalDue = dueRow.get(7);
											if (StringUtils.isNotEmpty(totalDue)){
												totalDue = totalDue.replaceAll("[\\$,]+", "");
												resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue.trim());
											}
										}
									} else{
										String totalDue = dueRow.get(4);
										
										if (firstDate != null && (now.get(Calendar.MONTH)) <= (firstDate.get(Calendar.MONTH))){
											if (StringUtils.isNotEmpty(totalDue)){
												priorDelinq += "+" + totalDue.replaceAll("[\\$,]+", "");
											}
										} else{
											totalDue = dueRow.get(7);
											if (StringUtils.isNotEmpty(totalDue)){
												priorDelinq += "+" + totalDue.replaceAll("[\\$,]+", "");
											}
										}
									}
								}
								if (StringUtils.isNotEmpty(priorDelinq)){
									resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), GenericFunctions.sum(priorDelinq, -1));
								}
							}
						}
					}
				}
			
			
			parseOwnerNames(resultMap, searchId);
		} catch(Exception e){
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static void parseOwnerNames(ResultMap m, long searchId) throws Exception {
		   
		   String ownerNameAddr = (String) m.get("tmpOwnerNameAddress");
		   if (StringUtils.isEmpty(ownerNameAddr))
			   return;
		   	   
		   // owner name and address are on consecutive lines, separated by <br>, transformed into \s\s
		   String[] lines = ownerNameAddr.split("\\s{2}");
		   String temp = "" ;

		   Matcher poBox = GenericFunctions.poBox.matcher(lines[1]);
		   if (lines[1].matches("\\s*\\d+(.*)") || lines[1].matches("(?i)PMB\\s+\\d+(.*)") || 	//PMB=Private mailbox
				   lines[1].matches("(?i)PO\\s+BX\\s+\\d+(.*)") || poBox.find()){
				   temp = lines[0];
		   } else{
			   if (lines[0].endsWith("TRUSTEE") && !lines[1].matches("\\A\\s*%.*")){
				   temp = lines[0] + " @@ " + lines[1];
			   } else{
				   temp = lines[0] + " " + lines[1];
			   }
		   }
		   
		   String s = temp;
		   s = s.replaceAll("\\s+\\(?LE\\)?\\s+", " ");
		   s = s.replaceAll("\\bET\\s+UX\\b", "ETUX");
		   s = s.replaceAll("\\bET\\s+AL\\b", "ETAL");
		   s = s.replaceAll("\\bETAK\\b", "ETAL");
		   s = s.replaceAll("\\bETVI\\b", "ETVIR");
		   
		   s = s.replaceAll("(?is)\\A\\s*(\\w+\\s+\\w+\\s+ETUX\\s+\\w+)\\s+(LIVING\\s+TR(?:UST)?)\\b", "$1 @@ $1 $2");//SMITH TOMMY ETUX ALBERTA LIVING TRUST
		   
		   s = s.replaceAll("\\b(.+)\\s+(LIVING\\s+TR)\\s+(TRUSTEES)\\b", "$1 $3 @@ $1 $2");//SMITH HAROLD D ETUX DESSIE LIVING TR TRUSTEES
		   s = s.replaceAll("\\bETUX\\s+(\\w+\\s+LIVING\\s+TR(?:UST)?)\\b", " #@# $1");//SMITH HAROLD D ETUX DESSIE LIVING TR TRUSTEES
		   
		   s = s.replaceAll("(.+)\\s+(ETUX)\\s+((?:REVOCABLE\\s+)?LIVING\\s+TRUST)\\b", "$1 $2 @@ $1 $3");//SMITH THOMAS A ETUX  REVOCABLE LIVING TRUST

		   s = s.replaceAll("\\b(TRUSTEE\\s+OF\\s+THE.*)\\s+TR\\b", "$1 TRUST");
		   s = s.replaceAll("\\b(TRUSTEE)\\s+OF\\s+THE\\s+", "$1 @@ ");
		   
		   s = s.replaceAll("\\bTR\\s+", " TR @@ ");
		   s = s.replaceAll("\\b(LIVING)\\s+TR\\b", " $1 TRUST"); 
		   s = s.replaceAll("%", " @@ ");
		   s = s.replaceAll("(?is)\\bD\\s*/\\s*B\\s*/\\s*A\\b", " @@ ");
		   s = s.replaceAll("\\s+", " ");
		   
		   s = s.replaceAll("\\bL/E\\b", "LIFE ESTATE");

		   s = s.replaceAll("C/O", "@@");
		   if (s.matches("\\s*(.+)\\s*&\\s+([A-Z]+)\\s+([A-Z]{1})?\\s+&\\s+(.+)")) {
			   s = s.replaceAll("\\s*&\\s+([A-Z]+)\\s+([A-Z]{1})?\\s+&", " & $1 $2 &");
			   s = s.replaceAll("&", "@@");
		   }
		   
		   s = s.replaceAll("\\b(ETAL\\s+\\w+)\\s+&\\s+(\\w+\\s+\\w+\\s*$)", " $1 @@ $2");
		   s = s.replaceAll("\\bR/M\\b", "");
		   
		   String[] owners ;
		   String[] own = null;
		   owners = s.split("@@");
		   
		   if (s.contains("&")){
			   String[] ownersTemp = s.split("\\s*&\\s*");
			   if (ownersTemp.length == 2 && ownersTemp[1].split("\\s+").length > 2){
				   owners = s.split("\\s*&\\s*");
			   }
		   }
		  
		   List<List> body = new ArrayList<List>();
		   String[] names = {"", "", "", "", "", ""};
		   String[] suffixes, type, otherType;
				
		   String ln = "";
				
		   for (int i = 0; i < owners.length; i++){
			   String ow = owners[i];

			   if (StringUtils.isNotEmpty(ow.trim())){
				   if (i == 0){
					   names = StringFormats.parseNameNashville(ow, true);
					   if (NameUtils.isNotCompany(names[2])){
						   ln = names[2];
						}
					} 
				   // SMITH CHRISTY MICHELLE & LENORD H ETUX NELLIE L
				   if (ow.matches("&\\s*([A-Z]{1,})\\s+([A-Z]{1,})\\s+ETUX\\s+([A-Z]{1,})\\s+([A-Z]{1,})")) {
					   ow = ow.replaceAll("(.+)(\\s+ETUX.*)", "& $1 " + ln + "$2");
				   }
				   if ((i > 0) && (!temp.contains("%"))){
					   if (ow.matches("\\s*([A-Z]{1,})\\s+([A-Z]{1,})\\s+&\\s+([A-Z]{1,})\\s+([A-Z]{1,}(\\s+[A-Z]{1,})?)")) {
						   own = ow.split("&");
						   try {
							   for (int j = 0; j < own.length; j++) {
								   ow = own[j];
								   if (j == 0) {
									   names = StringFormats.parseNameDesotoRO(ow, true);
								   } else {
									   String[] name = StringFormats.parseNameDesotoRO(ow, true);
									   names[3] = name[0];
									   names[4] = name[1];
									   names[5] = name[2];
									   if (names[2].length()==1){	//J R & TERESA WILLIAMS 
										   names[1] = names[2];
										   names[2] = names[5];
									   }
									}
								}
							} catch (Exception e){
								e.printStackTrace();
							}
						} else if (ow.matches("\\s*([A-Z]{1,})\\s+&\\s+([A-Z]{1,})\\s+([A-Z]{1,})")) {
							ow = ow.replaceAll("([A-Z]{1,})\\s+&\\s+([A-Z]{1,})\\s+([A-Z]{1,})", "$2 $3 & $1");
							names = StringFormats.parseNameDesotoRO(ow, true);
						} else if (!NameUtils.isCompany(ow) && ow.matches("\\s*([A-Z]+)\\s+&\\s+([A-Z]+)")){
							String[] coOwner = ow.split("&");
							names[0] = "";
							names[2] = coOwner[0].trim();
							names[5] = coOwner[1].trim();
						} else {
							if (NameUtils.isCompany(ow)){
								names = new String[6];
								names[2] = ow.trim();
								names[0] = names[1] = names[3] = names[4] = names[5] = "";  
							} else{
								String[] split = ow.trim().split("\\s+");
								int len = split.length;
								//JACKSON CHERYL J
								if (len > 2 && split[len-1].length() == 1 && split[0].length() > 1 && LastNameUtils.isLastName(split[0])){
									names = StringFormats.parseNameNashville(ow, true);
								} else{
									names = StringFormats.parseNameDesotoRO(ow, true);
								}
							}
						}
							
				   } else{
					   if (i > 0){
						   names = StringFormats.parseNameDesotoRO(ow, true);
					   }
				   }
				   if (!StringUtils.isEmpty(ln) && names[2].length() == 1 && names[1].length() == 0 && ow.matches("\\s*\\b\\w{2,}\\s+\\w\\s*\\b")){
					   names[1] = names[2];
					   names[2] = ln;
				   }
				   if (i > 0 && names[0].length()>0 && names[1].length()==0 && 
						   (names[2].equalsIgnoreCase("ETAL") || (LastNameUtils.isNotLastName(names[2]) && FirstNameUtils.isFirstName(names[2])) )){
					   names[1] = names[2];
					   names[2] = ln;
					}
				   if (i > 0 && names[2].length() == 1 ){
					   names[1] += " " + names[2];
					   names[1] = names[1].trim();
					   names[2] = ln;
					}
				   if (i > 0 && LastNameUtils.isNotLastName(names[2]) && LastNameUtils.isLastName(names[0]) && NameUtils.isNotCompany(names[2])){
					   String aux = names[2];
					   names[2] = names[0];
					   names[0] = aux;
				   }
				   if (i > 0 && NameUtils.isNotCompany(names[2]) && names[0].length() == 0 && names[1].length() == 0){//DAVIDSON DONALD LEE & GRACE
					   names[0] = names[2];
					   names[2] = ln;
				   }
				   names[2] = names[2].replaceAll("#@#", "AND");
				   type = GenericFunctions.extractAllNamesType(names);
				   otherType = GenericFunctions.extractAllNamesOtherType(names);
				   suffixes = GenericFunctions.extractNameSuffixes(names);
				   GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
				}
		   }
			GenericFunctions.storeOwnerInPartyNames(m, body, true);
	   }
}
