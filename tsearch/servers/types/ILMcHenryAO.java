package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.SimpleNodeIterator;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.recoverdocument.ModuleShortDescription;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.log.SearchLogFactory;
import ro.cst.tsearch.log.SearchLogPage;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.MultiplePinFilterResponse;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;

public class ILMcHenryAO extends TemplatedServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -193060779573724270L;
	
	private static int seq = 0;
	protected synchronized static int getSeq() {
		return seq++;
	}
	
	public ILMcHenryAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	@Override
	protected void setMessages() {
		getErrorMessages().addServerErrorMessage("0 record(s) returned");
		getErrorMessages().addNoResultsMessages("0 record(s) returned");
	}

	@Override
	protected void ParseResponse(String action, ServerResponse response, int viParseID) throws ServerResponseException {
		if (!isError(response)) {
			String result = response.getResult();
			switch (viParseID) {
			case ID_INTERMEDIARY:
			case ID_SEARCH_BY_ADDRESS:
			case ID_SEARCH_BY_PARCEL:
				result = clean(result);
				response.setResult(result);
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> intermediary = smartParseIntermediary(response, result, outputTable);
				if (intermediary.size() > 0) {
					response.getParsedResponse().setResultRows(new Vector<ParsedResponse>(intermediary));
					response.getParsedResponse().setOnlyResponse(outputTable.toString());
					response.getParsedResponse().setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,
					outputTable.toString());
				}
				break;
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				String id = getUniqueID(result);
				String fileName = id + ".html";
				
				if (viParseID == ID_DETAILS) {
					result = cleanDetailsResponse(result);
					response.setResult(result);
					// construct the link for save
					String originalLink = getLinkPrefix(TSConnectionURL.idGET) + action;

					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type", "ASSESSOR");
					
					// construct the Save part of the form
					if (isInstrumentSaved(id, null, data)) {
						result += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(originalLink, result);
						result = addSaveToTsdButton(result, originalLink, viParseID);
					}
					
					response.getParsedResponse().setPageLink(new LinkInPage(action, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					response.getParsedResponse().setResponse(result);
					
				} else {
					if (!isParentSite()){
						result = cleanDetailsResponse(result);
					}
					smartParseDetails(response, result);
					msSaveToTSDFileName = fileName;
					response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					response.getParsedResponse().setResponse(result);

					msSaveToTSDResponce = result + CreateFileAlreadyInTSD();
				}
				break;
				
			case ID_GET_LINK:
				// message for direction to ID_INTERMEDIARY ; ID_DETAILS
				String directToDetailsMessage = "Property Information";
				if (result.contains(directToDetailsMessage)) {
					ParseResponse(action, response, ID_DETAILS);
				} else {
					ParseResponse(action, response, ID_INTERMEDIARY);
				}

				break;
			}
		}
	}

	private String getUniqueID(String result) {
		// get account number
		String id = StringUtils.parseByRegEx(result, "\\d{2,2}-\\d{2,2}-\\d{3,3}-\\d{3,3}", 0);
		return id;
	}

	private String cleanDetailsResponse(String result) {
		HtmlParser3 parser = new HtmlParser3(result);
		NodeList nodeList = HtmlParser3.getTag(parser.getNodeList(), new TableTag(), true);
		String cleanResult = "";
		
		if (nodeList.size() > 0) {
			cleanResult = "<div align=\"left\"> ";
			for (int i=0; i< nodeList.size(); i++) {
				cleanResult +=  "<div style=\"border:1px solid grey;\">" + nodeList.elementAt(i).toHtml() + "<br/> </div>";
				cleanResult = cleanResult.replaceAll("(?is)(<a[^>]+>\\s*)?<img.*?/>(\\s*</a>)?", "");
				cleanResult = cleanResult.replaceAll("(?is)\\s+style=\\\"width: 100%;\\\"","");
				cleanResult = cleanResult.replaceAll("(?is)width: 100%;","");
			}
			cleanResult += "</div> <br/> <br/>";
			
			String textToRemove = "";
			if (cleanResult.contains("The web site you are accessing has experienced an unexpected error.")) {
				NodeList tag = HtmlParser3.getTag(nodeList.elementAt(2).getChildren(), new TableTag(), true);
				textToRemove = tag.elementAt(tag.size() - 1).toHtml();
			}
			cleanResult = cleanResult.replace(textToRemove, "");
		}
		
		return cleanResult;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		String uniqueID = getUniqueID(detailsHtml);
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), uniqueID);
		String lotNumber = "";
		String cityStateZip = "";
		String serverLegalDescription = "";
		String serverOwnerName = "";
		
		HtmlParser3 parser = new HtmlParser3(detailsHtml);
		NodeList nodeList = HtmlParser3.getTag(parser.getNodeList(), new TableTag(), true);
		
		if (nodeList.size() > 6) {
			
		}
		
		// get "Property Information" table
		TableTag propertyInformationTable = (TableTag) nodeList.elementAt(0);
		if (propertyInformationTable != null) {
			TableRow[] rows = propertyInformationTable.getRows();
			if (rows.length >= 5) {
				if (rows[2].getColumnCount() == 2 ) {
					String serverAddress = StringUtils.cleanHtml(rows[2].getColumns()[1].toPlainTextString().trim());
					ro.cst.tsearch.servers.functions.ILMcHenryAO.parseAddress(resultMap, serverAddress);
				}
				if (rows[3].getColumnCount() == 2 ) {
					cityStateZip = StringUtils.cleanHtml(rows[3].getColumns()[1].toPlainTextString().trim());
					ro.cst.tsearch.servers.functions.ILMcHenryAO.parseCityStateZip(resultMap, cityStateZip);
				}
				if (rows[5].getColumnCount() == 2 ) {
					lotNumber = StringUtils.cleanHtml(rows[5].getColumns()[1].toPlainTextString().trim());
				}
			}
		}

		// get "Legal Information" table
		TableTag legalInformation = (TableTag) nodeList.elementAt(3);
		if (legalInformation != null) {
			TableRow[] rows = legalInformation.getRows();
			if (rows.length > 2) {
				serverLegalDescription = StringUtils.cleanHtml(rows[2].getColumns()[1].toPlainTextString());
				serverLegalDescription = org.apache.commons.lang.StringUtils.strip(serverLegalDescription);
				ro.cst.tsearch.servers.functions.ILMcHenryAO.parseLegalDescription(resultMap, serverLegalDescription);
				ro.cst.tsearch.servers.functions.ILMcHenryAO.parseLotNumber(resultMap, lotNumber);
			}
			
		}
		// get "Tax Name" table
		TableTag taxName = (TableTag) nodeList.elementAt(4);
		if (taxName != null) {
			TableRow[] rows = taxName.getRows();
			if (rows.length > 2) {
				serverOwnerName = StringUtils.cleanHtml(rows[1].getColumns()[1].toPlainTextString());
				ro.cst.tsearch.servers.functions.ILMcHenryAO.parseName(resultMap, serverOwnerName);
			}
			
		}

		TableTag assessmentInfo = (TableTag) nodeList.elementAt(5);
		if (assessmentInfo != null) {
			if (assessmentInfo.getRowCount() > 2) {
				TableRow row = assessmentInfo.getRow(2);
				if (row.getColumnCount() == 7) {
					resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), row.getColumns()[4].toPlainTextString().trim().replaceAll("[\\$,]", ""));
					resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), row.getColumns()[5].toPlainTextString().trim().replaceAll("[\\$,]", ""));
					resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), row.getColumns()[6].toPlainTextString().trim().replaceAll("[\\$,]", ""));
				}
			}
		}
		
		Node salesRecords = nodeList.elementAt(1);
		if (salesRecords != null) {
			ro.cst.tsearch.servers.functions.ILMcHenryAO.parseSaleDataSet(new NodeList(salesRecords), resultMap);
		}

		return null;
	}

	@Override
	protected ServerResponse SearchBy(boolean resetQuery, TSServerInfoModule module, Object sd) throws ServerResponseException {
		return super.SearchBy(resetQuery, module, sd);
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		String result = response.getResult();
		HtmlParser3 parser = new HtmlParser3(result);
		NodeList list = HtmlParser3.getTag(parser.getNodeList(), new TableTag(), true);
		TableTag resultTable = (TableTag) list.elementAt(0);
		outputTable.append(resultTable);
		
		if (resultTable != null) {
			TableRow[] rows = resultTable.getRows();
			String header = "";
			
			if (rows!=null && rows.length > 0) {
				header = rows[0].toHtml().replaceAll("(?is)(<\\s*/\\s*)?th([^>]*>)", "$1" + "td" + "$2");
			}
			
			for (int j = 1; j < rows.length; j++) {
				TableColumn[] columns = rows[j].getColumns();
				if (columns.length == 4) {
					ResultMap resultMap = new ResultMap();
					resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), dataSite.getSiteTypeAbrev());

					LinkTag linkTag = (LinkTag) columns[0].getChildren()
							.extractAllNodesThatMatch(new TagNameFilter("a"), false).elementAt(0);
					String addressOnServer = columns[1].toPlainTextString();
					ro.cst.tsearch.servers.functions.ILMcHenryAO.parseAddress(resultMap, addressOnServer);
					resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), linkTag.toPlainTextString().trim());
					resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), columns[2].toPlainTextString().trim());
					resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), columns[3].toPlainTextString().trim());
					
					// set page link
					ParsedResponse currentParsedResponse = new ParsedResponse();

					String atsLink = CreatePartialLink(TSConnectionURL.idGET) + "/" + linkTag.extractLink();
					currentParsedResponse.setPageLink(new LinkInPage(atsLink, atsLink, TSServer.REQUEST_SAVE_TO_TSD));
					linkTag.setLink(atsLink);
					String currentRowHtml = rows[j].toHtml();
					currentParsedResponse.setOnlyResponse(currentRowHtml);
					currentParsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, currentRowHtml);

					// bridge it
					resultMap.removeTempDef();
					Bridge bridge = new Bridge(currentParsedResponse, resultMap, searchId);
					DocumentI document = null;
					try {
						document = bridge.importData();
					} catch (Exception e) {
						e.printStackTrace();
					}
					currentParsedResponse.setDocument(document);
					//
					intermediaryResponse.add(currentParsedResponse);
				}
			}
			
			response.getParsedResponse().setHeader("<table width='300px'>" + header);
			Map<String, String> params = new HashMap<String, String>();			 			
			params = HttpSite3.fillConnectionParams(result, ro.cst.tsearch.connection.http3.ILMcHenryAO.getTargetArgumentParameters(), "form1");
			
			String footer = "</table>" + processLinks(response, params, response.getQuerry());
			response.getParsedResponse().setFooter(footer);
			//response.getParsedResponse().setNextLink("<a href=\"" + linkN + "\">Next</a>");
		}
		
		return intermediaryResponse;
	}

	private String processLinks(ServerResponse response, Map<String, String> params, String paramsOfSearchReq) {
		String linksTable = "";
		String paramsOfSearch = paramsOfSearchReq;
		HtmlParser3 parser = new HtmlParser3(response.getResult());
		NodeList tag = HtmlParser3.getTag(parser.getNodeList(), new TableTag(), true);
		int seq = getSeq();
		
		if (tag.size() == 2) {
			TableTag table = (TableTag) tag.elementAt(1);
			NodeList linkTags = HtmlParser3.getTag(table.getChildren(), new LinkTag(), true);
			SimpleNodeIterator iterator = linkTags.elements();

			Map<String,String> paramsOfReq = new HashMap<String, String>();
			paramsOfReq.putAll(params);
			
			while (iterator.hasMoreNodes()) {
				LinkTag nextNode = (LinkTag) iterator.nextNode();
				Matcher matchLink = Pattern.compile("(?is)\\s*__doPostBack\\(['&#\\d;]+([A-Z]+)['&#\\d;]+\\s*,\\s*['&#\\d;]+(Page\\$\\d+)['&#\\d;]+\\)\\s*")
						.matcher(nextNode.getLink());
				if (matchLink.matches()) {
					String eventTarget = matchLink.group(1).trim();
					String eventArgument = matchLink.group(2).trim();
					String atsLink = CreatePartialLink(TSConnectionURL.idPOST) + "/default.aspx?" + 
							"__EVENTTARGET=" + eventTarget + "&" + 
							"__EVENTARGUMENT=" + eventArgument;
					if (StringUtils.isNotEmpty(paramsOfSearch)) {
						paramsOfSearch = paramsOfSearch.replaceFirst("(?is).*(txtSearchAddress=[^&]+).*", "$1");
						atsLink += "&" + paramsOfSearch + "&seq=" + seq;
					}
					mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, paramsOfReq);
					nextNode.setLink(atsLink);
				}
			}
			linksTable = table.toHtml();
		}

		return linksTable;
	}

	protected String clean(String response) {
		HtmlParser3 parser = new HtmlParser3(response);
		NodeList list = HtmlParser3.getTag(parser.getNodeList(), new TableTag(), true);
		String returnResult = response;
		if (list.size() > 3) {
			returnResult = list.elementAt(2).toHtml() + list.elementAt(3).toHtml();
		}

		return returnResult;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		String streetNo = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNO);
		String streetName = getSearch().getSa().getAtribute(SearchAttributes.P_STREETNAME);
		String pin = getSearch().getSa().getAtribute(SearchAttributes.LD_PARCELNO);

		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		
		FilterResponse multiplePINFilter = new MultiplePinFilterResponse(searchId);
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if (Search.AUTOMATIC_SEARCH == searchType) {
			Collection<String> pins = getSearchAttributes().getPins(-1);
			if(pins.size() > 1){			
				for(String pid: pins) {
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
					module.clearSaKeys();
					pid = pid.replaceAll("(\\d{2})(\\d{2})(\\d{3})(\\d{3})", "$1-$2-$3-$4");
					String[] splitPin = pid.split("-");
					if (splitPin.length == 4) {
						module.getFunction(0).forceValue(splitPin[0]);
						module.getFunction(1).forceValue(splitPin[1]);
						module.getFunction(2).forceValue(splitPin[2]);
						module.getFunction(3).forceValue(splitPin[3]);
					}
					modules.add(module);	
				}			
				// set list for automatic search 
				serverInfo.setModulesForAutoSearch(modules);
				resultType = MULTIPLE_RESULT_TYPE;
				return;
			}
		}
		
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			pin = pin.replaceAll("(\\d{2})(\\d{2})(\\d{3})(\\d{3})", "$1-$2-$3-$4");
			String[] splitPin = pin.split("-");
			if (splitPin.length == 4) {
				module.getFunction(0).forceValue(splitPin[0]);
				module.getFunction(1).forceValue(splitPin[1]);
				module.getFunction(2).forceValue(splitPin[2]);
				module.getFunction(3).forceValue(splitPin[3]);
			}
			modules.add(module);
		}

		if (hasStreet() || hasStreetNo()) {
			FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter (searchId , 0.8d , true);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.clearSaKeys();
			module.getFunction(0).forceValue(streetNo + " " + streetName);
			module.addFilter(addressFilter);
			module.addFilter(multiplePINFilter);
			modules.add(module);
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	@Override
	protected int getResultType(){
		if(mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) == Boolean.TRUE ||
				(mSearch.getSa().getPins(-1).size() > 1 && (Search.AUTOMATIC_SEARCH == mSearch.getSearchType()))){
			return MULTIPLE_RESULT_TYPE; 
		} else {
			return UNIQUE_RESULT_TYPE;
		}
	}
	
	@Override
    public boolean anotherSearchForThisServer(ServerResponse sr) {
		boolean result = mSearch.getSa().getPins(-1).size() > 1 &&
			    		 mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) != Boolean.TRUE;
		return result?true:super.anotherSearchForThisServer(sr);
	}
	
	@Override
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
				
				if (!firstTime && !entry.getKey().matches("txtSearchPin\\d")) {
					sb.append(", ");
				} else {
					firstTime = false;
				}
				if ("PIN".equals(entry.getKey()) || "Property Address".equals(entry.getKey())) {
					sb.append(entry.getKey() + " = <b>" + value + "</b>");
				} else if (entry.getKey().matches("txtSearchPin\\d")) 
					sb.append("<b>-" + value + "</b>");
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

	
}
