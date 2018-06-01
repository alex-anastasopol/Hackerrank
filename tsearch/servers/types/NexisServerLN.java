package ro.cst.tsearch.servers.types;


import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.LinkRegexFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import resource.module.comments.NexisSelectsListLN;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.recoverdocument.ModuleShortDescription;
import ro.cst.tsearch.connection.http2.NexisConnLN;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.log.SearchLogFactory;
import ro.cst.tsearch.log.SearchLogPage;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.LexisNexisDocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SavedFromType;

/**
 * @author mihaib
*/

@SuppressWarnings("deprecation")
public class NexisServerLN extends TSServer{

	private final BigInteger numberToDivide = new BigInteger("4932916139");
	
	private static final long serialVersionUID = 1L;
	private boolean downloadingForSave;
	
	private static final Pattern NEXT_PARAM_PAT = Pattern.compile("(?is)<input[^>]+name=\\\"(ctl00\\$MainContent\\$toolbar\\$nextButton)\\\"[^>]*>");
	private static final Pattern PREV_PARAM_PAT = Pattern.compile("(?is)<input[^>]+name=\\\"(ctl00\\$MainContent\\$toolbar\\$previousButton)\\\"[^>]*>");
	private static final Pattern CURR_PAGE_PAT = Pattern.compile("(?is)<span[^>]+class=\\\"pagerNumber\\\"[^>]*>([^<]*)</span");

	private static final String LEXISFORM = "LexisForm";
	
	private static String DPPA_USE = "";
	private static String GLBA_USE = "";
	
	static {
		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		try {
			DPPA_USE = FileUtils.readFileToString(new File(folderPath + File.separator + "DPPAUse.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			GLBA_USE = FileUtils.readFileToString(new File(folderPath + File.separator + "GLBAUse.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public NexisServerLN(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public NexisServerLN(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,	int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		String state = getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV);
		if(tsServerInfoModule != null) {
			if (tsServerInfoModule.getModuleParentSiteLayout() != null){
		        PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
				for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
					String functionName = htmlControl.getCurrentTSSiFunc().getName();
					if(StringUtils.isNotEmpty(functionName)) {
						if(functionName.toLowerCase().contains("state")) {
							setupSelectBox(tsServerInfoModule.getFunction(9), 
									"<select name=\"ctl00$MainContent$AgeLow\">" + NexisSelectsListLN.SELECT_AGE);
					        tsServerInfoModule.getFunction(9).setRequired(true);
					        
					        setupSelectBox(tsServerInfoModule.getFunction(10), 
									"<select name=\"ctl00$MainContent$AgeHigh\">" + NexisSelectsListLN.SELECT_AGE);
					        tsServerInfoModule.getFunction(10).setRequired(true);
					        
							setupSelectBox(tsServerInfoModule.getFunction(16), 
									"<select name=\"ctl00$MainContent$State$stateList\">" + NexisSelectsListLN.SELECT_STATE.replaceAll("(?is)(value=\\\"" + state + ")", "selected=\"selected\" $1"));
					        tsServerInfoModule.getFunction(16).setRequired(true);
					        
					        setupSelectBox(tsServerInfoModule.getFunction(19), 
									"<select name=\"ctl00$MainContent$PreviousState$stateList\">" + NexisSelectsListLN.SELECT_STATE);
					        tsServerInfoModule.getFunction(19).setRequired(true);
					        
					        setupSelectBox(tsServerInfoModule.getFunction(20), 
									"<select name=\"ctl00$MainContent$OtherPreviousState$stateList\">" + NexisSelectsListLN.SELECT_STATE);
					        tsServerInfoModule.getFunction(20).setRequired(true);
					        
					        setupSelectBox(tsServerInfoModule.getFunction(21), NexisSelectsListLN.SELECT_RADIUS);
					        tsServerInfoModule.getFunction(21).setRequired(true);
					        
					        if (!StringUtils.isEmpty(DPPA_USE)) {
								if (tsServerInfoModule.getFunctionCount()>23) {
									setupSelectBox(tsServerInfoModule.getFunction(23), DPPA_USE);
								}
							}
							if (!StringUtils.isEmpty(GLBA_USE)) {
								if (tsServerInfoModule.getFunctionCount()>24) {
									setupSelectBox(tsServerInfoModule.getFunction(24), GLBA_USE);
								}
							}
					        
						}
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if(tsServerInfoModule != null) {
			if (tsServerInfoModule.getModuleParentSiteLayout() != null){
				PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
				for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
					String functionName = htmlControl.getCurrentTSSiFunc().getName();
					if(StringUtils.isNotEmpty(functionName)) {
						if(functionName.toLowerCase().contains("state")) {
							setupSelectBox(tsServerInfoModule.getFunction(11), 
									"<select name=\"ctl00$MainContent$State$stateList\">" + NexisSelectsListLN.SELECT_STATE.replaceAll("(?is)(value=\\\"" + state + ")", "selected=\"selected\" $1"));
					        tsServerInfoModule.getFunction(11).setRequired(true);
					        
					        if (!StringUtils.isEmpty(DPPA_USE)) {
								if (tsServerInfoModule.getFunctionCount()>14) {
									setupSelectBox(tsServerInfoModule.getFunction(14), DPPA_USE);
								}
							}
							if (!StringUtils.isEmpty(GLBA_USE)) {
								if (tsServerInfoModule.getFunctionCount()>15) {
									setupSelectBox(tsServerInfoModule.getFunction(15), GLBA_USE);
								}
							}
					        
						}
					}
				}
			}
		}
		
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
		
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m = null;
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();	
		
		FilterResponse nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter(
															SearchAttributes.OWNER_OBJECT , searchId , m);
		nameFilterHybridDoNotSkipUnique.setStrategyType(FilterResponse.STRATEGY_TYPE_BEST_RESULTS);

		FilterResponse addressFilter = AddressFilterFactory.getGenericAddressHighPassFilter(searchId, 0.80d);
		((GenericAddressFilter)addressFilter).setEnableUnit(false);
		
		if (hasOwner()){
			{	
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
						TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
				m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				m.clearSaKeys();
		
				m.forceValue(1, "on");
				m.forceValue(14, global.getSa().getAtribute(SearchAttributes.P_STREET_NO_NAME));
				m.forceValue(16, global.getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
				m.forceValue(19, "ALL");
				m.forceValue(20, "ALL");
				m.forceValue(21, "0");//No radius
				m.forceValue(23, "5");//Insurer
				m.forceValue(24, "1");//Fraud Prevention and Detection
				((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setIgnoreMiddleOnEmpty(true);
				((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setUseArrangements(false);
				((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setUseSynonymsForCandidates(false);
				((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setInitAgain(true);
				m.addFilter(nameFilterHybridDoNotSkipUnique);
				m.addFilter(addressFilter);
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				
				ConfigurableNameIterator nameIterator = new ConfigurableNameIterator(searchId, new String[]{"L;F;"});
				nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
				nameIterator.setInitAgain(true);
				m.addIterator(nameIterator);
				
				modules.add(m);
	    	}
			{	
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
						TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
				m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				m.clearSaKeys();
		
				m.forceValue(0, "on");
				//m.forceValue(8, global.getSa().getAtribute(SearchAttributes.P_STREET_NO_NAME));
				m.forceValue(10, global.getSa().getAtribute(SearchAttributes.P_COUNTY_NAME));
				m.forceValue(11, global.getSa().getAtribute(SearchAttributes.P_STATE_ABREV));
				
				m.forceValue(14, "5");//Insurer
				m.forceValue(15, "1");//Fraud Prevention and Detection
				((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setIgnoreMiddleOnEmpty(true);
				((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setUseArrangements(false);
				((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setUseSynonymsForCandidates(false);
				((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setInitAgain(true);
				m.addFilter(nameFilterHybridDoNotSkipUnique);
				m.addFilter(addressFilter);
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
				ConfigurableNameIterator nameIterator = new ConfigurableNameIterator(searchId, new String[]{"L;F;"});
				nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
				nameIterator.setInitAgain(true);
				m.addIterator(nameIterator);
				modules.add(m);
	    	}
			
			if (StringUtils.isNotEmpty(global.getSa().getAtribute(SearchAttributes.OWNER_ZIP))){
				String zip = global.getSa().getAtribute(SearchAttributes.OWNER_ZIP);
				String zipPlus = "";
					
				if (zip.length() > 5){
					zip = zip.replaceAll("\\-", "");
					zipPlus = zip.substring(5, zip.length());
					zipPlus = org.apache.commons.lang.StringUtils.leftPad(zipPlus, 4, "0");
					
					zip = zip.substring(0, 5) + "-" + zipPlus;
					 
				}
				{
					m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			    	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
			    			TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			    	m.addExtraInformation("SEARCH", "SECOND_SEARCH");
					m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
					m.clearSaKeys();
	
					m.forceValue(1, "on");
					m.forceValue(16, "ALL");
					m.forceValue(17, zip);
					m.forceValue(19, "ALL");
					m.forceValue(20, "ALL");
					m.forceValue(21, "0");//No radius
					m.forceValue(23, "5");//Insurer
					m.forceValue(24, "1");//Fraud Prevention and Detection
					((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setIgnoreMiddleOnEmpty(true);
					((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setUseArrangements(false);
					((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setUseSynonymsForCandidates(false);
					((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setInitAgain(true);
					m.addFilter(nameFilterHybridDoNotSkipUnique);
					m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
					m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
					
					ConfigurableNameIterator nameIterator = new ConfigurableNameIterator(searchId, new String[]{"L;F;"});
					nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.PERSON_NAME);
					nameIterator.setInitAgain(true);
					m.addIterator(nameIterator);
					modules.add(m);
				}
				
				{	
					m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
					m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
							TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
					m.addExtraInformation("SEARCH", "SECOND_SEARCH");
					m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
					m.clearSaKeys();
					
					m.forceValue(0, "on");
					m.forceValue(11, "ALL");
					m.forceValue(12, zip);
					
					m.forceValue(14, "5");//Insurer
					m.forceValue(15, "1");//Fraud Prevention and Detection
					((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setIgnoreMiddleOnEmpty(true);
					((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setUseArrangements(false);
					((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setUseSynonymsForCandidates(false);
					((GenericNameFilter) nameFilterHybridDoNotSkipUnique).setInitAgain(true);
					m.addFilter(nameFilterHybridDoNotSkipUnique);
					m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
					ConfigurableNameIterator nameIterator = new ConfigurableNameIterator(searchId, new String[]{"L;F;"});
					nameIterator.setSearchWithType(ConfigurableNameIterator.SEARCH_WITH_TYPE.COMPANY_NAME);
					nameIterator.setInitAgain(true);
					m.addIterator(nameIterator);
					modules.add(m);
		    	}
			}
		}
	    serverInfo.setModulesForAutoSearch(modules);
	}
	
	@Override
	protected ServerResponse SearchBy(boolean resetQuery, TSServerInfoModule module, Object sd)throws ServerResponseException {
		
		if (mSearch.getSearchType() == Search.AUTOMATIC_SEARCH){
			if (TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS.equals(module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION))) {
				if ("SECOND_SEARCH".equals(module.getExtraInformation("SEARCH"))){
					Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
					DocumentsManagerI m = global.getDocManager();
					if (mSearch.getAdditionalInfo("alreadySearched") == null){
						try{
							m.getAccess();
							List<RegisterDocumentI> listRodocs = m.getRoLikeDocumentList(true);
							List<LexisNexisDocumentI> lnList = new ArrayList<LexisNexisDocumentI>();
							for(DocumentI doc : listRodocs){
								if(doc instanceof LexisNexisDocumentI && doc.isSavedFrom(SavedFromType.AUTOMATIC)){
									lnList.add((LexisNexisDocumentI)doc);
								}
							}
							if (lnList.isEmpty()){
								mSearch.setAdditionalInfo("alreadySearched", true);
								return super.SearchBy(resetQuery, module, sd);
							} else {
								return new ServerResponse();
							}
						}
						finally{
							m.releaseAccess();
						}
					} else {
						if ((Boolean) mSearch.getAdditionalInfo("alreadySearched")){
							return new ServerResponse();
						}
					}
				}
			}
		}
		
		return super.SearchBy(resetQuery, module, sd);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		
		String rsResponse = initialResponse;
	
		switch (viParseID) {
				
			case ID_SEARCH_BY_ADDRESS :
			case ID_SEARCH_BY_NAME :
				
				if (rsResponse.indexOf("Please enter information in at least one field") != -1){
					Response.getParsedResponse().setError("Please enter information in at least one field");
					return;
				} else if (rsResponse.indexOf("System Error") != -1){
					Response.getParsedResponse().setError("System Error");
					return;
				} else if (rsResponse.indexOf("We did not find any directory results for") != -1){
					Response.getParsedResponse().setError("No results found");
					return;
				} else if (rsResponse.indexOf("No Documents Found") != -1){
					Response.getParsedResponse().setError("No results found");
					return;
				} else if (rsResponse.matches("(?is).*More Than \\d+ Results.*")){
					try {
						org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
						
						NodeList nodeList = htmlParser.parse(null);
						NodeList divList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "information centered"), true);
						if (divList != null){
							if (divList.size() > 0){
								divList.elementAt(0).getChildren().keepAllNodesThatMatch(new NotFilter(new TagNameFilter("table")));
								String response = divList.elementAt(0).toHtml();
								response = response.replaceAll("<img[^>]+>", "").replaceAll("<input[^>]+>", "")
																.replaceAll("(?is)<div[^>]+>\\s*</div>", "");
								Response.getParsedResponse().setError(response);
							}
						}
					}  catch(Exception e) {
						e.printStackTrace();
					}
					return;
				}
				
				try {
					 
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
				
			case ID_DETAILS :
				
				if (sAction.contains("noDetails=true")){
					ParsedResponse parsedResponse = Response.getParsedResponse();
					DocumentI document = parsedResponse.getDocument();
					
					if(document!= null) {
						msSaveToTSDFileName = document.getInstno() + ".html";
						Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
						msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
					}
				} else {
					String details = "";
					details = getDetails(rsResponse, Response);
					
					String docNo = "0000";
					try {
						org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);
						NodeList mainList = htmlParser.parse(null);
						docNo = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Link ID"), "", true).trim();
						if (StringUtils.isEmpty(docNo)){
							docNo = StringUtils.extractParameterFromUrl(Response.getQuerry(), "docNumber");
						}
			
					} catch (Exception e) {
						e.printStackTrace();
					}
					
	
					if ((!downloadingForSave))
					{	
		                String qry_aux = Response.getRawQuerry();
						qry_aux = "dummy=" + docNo + "&" + qry_aux;
						String originalLink = sAction + "&" + qry_aux;
						String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
						
						HashMap<String, String> data = new HashMap<String, String>();
	    				data.put("type", "LEXISNEXIS");
		    				
						if (isInstrumentSaved(docNo, null, data)){
		                	details += CreateFileAlreadyInTSD();
						} else {
							mSearch.addInMemoryDoc(sSave2TSDLink, details);
							details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
						}
						parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
		            } 
					else 
		            {      
						smartParseDetails(Response, details);
		                msSaveToTSDFileName = docNo + ".html";
		                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
		                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
		               
					}
				}
				break;	
			
			case ID_GET_LINK :
				if (sAction.indexOf("ClickSearch.aspx") != -1){
						ParseResponse(sAction, Response, ID_DETAILS);
				} else if (sAction.indexOf("/people/") != -1 || sAction.indexOf("Results.aspx") != -1) {
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				}
				
				break;
			case ID_SAVE_TO_TSD :
				downloadingForSave = true;
					ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;

			
		}
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		try {
			//table = table.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			String navRow = "";
			
			NodeList nodeList = htmlParser.parse(null);
			NodeList tableList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", "resultscontent"), true);
			
			int numberOfUncheckedElements = 0;
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='0' CELLPADDING='2'>");
			
			TableRow[] rows = ((TableTag)tableList.elementAt(0)).getRows();
			
			String tableHeader = rows[0].toHtml(); 
			newTable.append(tableHeader);
			
			boolean isCompanySearch = false;
			if (tableHeader.contains("FEIN")){
				isCompanySearch = true;
			}

			for(int i = 1; i < rows.length; i++ ) {
				TableRow row = rows[i];
				String classAttribute = row.getAttribute("class");
				if (!("oddrow".equals(classAttribute) || "evenrow".equals(classAttribute))){
					continue;
				}
				if(row.getColumnCount() > 0) {
					
					TableColumn[] cols = row.getColumns();
					String lnk = "";
					NodeList aList = null;
					try {
						aList = cols[1].getChildren().extractAllNodesThatMatch(
								new TagNameFilter("a"));
					} catch (Exception e) {
						logger.error("Unhandled exception while getting aList", e);
					}
					if ((aList != null && aList.size() == 0) || aList == null) {
						lnk = "noDetails=true";
					} else {
						lnk = "/nexisprma/US/" + ((LinkTag) aList.elementAt(0)).getLink().replaceAll("\\s", "").replaceAll("&amp;", "&");
						//String id = StringUtils.extractParameterFromUrl(lnk, "a") + StringUtils.extractParameterFromUrl(lnk, "d");
					}
						String documentNumber = "";
						try {
							documentNumber = cols[3].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true)
										.extractAllNodesThatMatch(new LinkRegexFilter("LinkIDClick")).elementAt(0).getChildren().elementAt(0)
										.toHtml();
						} catch (Exception e) {
							logger.error("Unhandled exception while getting documentNumber", e);
						}
						
						String tmpPartyGtor = "";
						if (lnk.contains("noDetails=true")){
							if (cols[1].getChildCount() > 0){
								tmpPartyGtor = cols[1].getChildren().toHtml();
							}
						} else {
							tmpPartyGtor = cols[1].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true)
												.elementAt(0).getChildren().toHtml();
						}
						
						tmpPartyGtor = tmpPartyGtor.replaceAll("(?is)&nbsp;", " ");
						
						String addresses = "";
						if (isCompanySearch){							
							TableTag addrTable = (TableTag)cols[3].getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
													.extractAllNodesThatMatch(new HasAttributeFilter("class", "addrTable"), true).elementAt(0);
							if (addrTable != null){
								TableRow[] addressRows = addrTable.getRows();
								for(TableRow addrRow : addressRows){
									TableColumn[] colus = addrRow.getColumns();
									if (colus.length > 0){
										if (colus[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).size() > 0){
											addresses += colus[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"))
														.extractAllNodesThatMatch(new HasAttributeFilter("class", "dropdownlinkaddress"), true)
														.extractAllNodesThatMatch(new TagNameFilter("span"), true).toHtml();
										} else {
											addresses += colus[0].getChildren()
														.extractAllNodesThatMatch(new NotFilter(new HasAttributeFilter("class", "citeTextSmall"))).toHtml() + "@@#@@";
										}
									}
								}
							} else {
								TableColumn colus = cols[3];
								if (colus != null){
									if (colus.getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).size() > 0){
										addresses += colus.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"))
													.extractAllNodesThatMatch(new HasAttributeFilter("class", "dropdownlinkaddress"), true)
													.extractAllNodesThatMatch(new TagNameFilter("span"), true).toHtml();
									} else {
										addresses += colus.getChildren()
													.extractAllNodesThatMatch(new NotFilter(new HasAttributeFilter("class", "citeTextSmall"))).toHtml() + "@@#@@";
									}
								}
							}
						} else {
							TableRow[] addressRows = ((TableTag)cols[2].getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
												.extractAllNodesThatMatch(new HasAttributeFilter("class", "addrTable"), true).elementAt(0)).getRows();
							for(TableRow addrRow : addressRows){
								TableColumn[] colus = addrRow.getColumns();
								if (colus.length > 0){
									if (colus[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a")).size() > 0){
										addresses += colus[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"))
													.extractAllNodesThatMatch(new HasAttributeFilter("class", "dropdownlinkaddress"), true)
													.extractAllNodesThatMatch(new TagNameFilter("span"), true).toHtml();
									} else {
										addresses += colus[0].getChildren()
													.extractAllNodesThatMatch(new NotFilter(new HasAttributeFilter("class", "citeTextSmall"))).toHtml() + "@@#@@";
									}
								}
							}
						}
						
						if (StringUtils.isEmpty(documentNumber)){
							try {
								documentNumber = getInstrNoFromBigIntGhertzo(tmpPartyGtor, addresses);
							} catch (Exception e) {
								logger.error("Unhandled exception while getting documentNumber", e);
							}
						}

						if (isCompanySearch || lnk.contains("noDetails=true")){
							lnk += "&docNumber=" + documentNumber;
						}
						String key = documentNumber.trim();
							
						ParsedResponse currentResponse = responses.get(key);							 
						if(currentResponse == null) {
							currentResponse = new ParsedResponse();
							responses.put(key, currentResponse);
						}
							
						RegisterDocumentI document = (RegisterDocumentI)currentResponse.getDocument();
						
						ResultMap resultMap = new ResultMap();
							
						if (StringUtils.isNotEmpty(addresses)){
							addresses = addresses.replaceAll("(?is)<span[^>]*>", "");
							addresses = addresses.replaceAll("(?is)</span[^>]*>", "@@#@@").replaceAll("(?is)&nbsp;", " ");
							resultMap.put("tmpAddresses", addresses);
						}
						
						String link = CreatePartialLink(TSConnectionURL.idGET) + lnk;
						if(document == null) {	//first time we find this document
							int count = 1;
							
							String rowHtml =  row.toHtml();
							if ("oddrow".equals(classAttribute)){
								rowHtml = rowHtml.replaceAll("(?is)(<tr[^>]+)>", "$1 bgcolor=\"#F5F5DC\">");
							}
							rowHtml = rowHtml.replaceAll("(?is)<img[^>]+>", "");
							rowHtml = rowHtml.replaceAll("(?is)<a onClick[^>]+>\\s*</a>", "");

							//delete links for phone, ssn and linkId
							rowHtml = rowHtml.replaceAll("(?is)<a[^>]*\\s*(?:LinkIDClick|PhoneClick|SsnClick2)[^>]*>([^<]+)</a>", "$1<br>");
							
							rowHtml = rowHtml.replaceAll("(?is)<a[^>]+dropdownlinkaddress[^>]*>", "");
							rowHtml = rowHtml.replaceAll("(?is)<span[^>]*>", "&nbsp;");
							rowHtml = rowHtml.replaceAll("(?is)</span>\\s*</a>", "");
							rowHtml = rowHtml.replaceFirst("(?is)(href=\\\")[^\\\"]+", "$1" + link);
							
							rowHtml = rowHtml.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
							rowHtml = rowHtml.replaceAll("(?is)(<table[^>]+class=\\\"addrTable[^>]*)>", "$1 border=\"0\">");
							
							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "LN");
							tmpPartyGtor = StringEscapeUtils.unescapeHtml(tmpPartyGtor);
							resultMap.put("tmpPartyGtor", tmpPartyGtor);

							resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), tmpPartyGtor.replaceAll("(?is)\\s*<br/?>\\s*", " / "));
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNumber);
							
							SimpleDateFormat formatter = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY);
					        String sDate = formatter.format(Calendar.getInstance().getTime());
							resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), sDate);
							resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), sDate);
							
							resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "LEXISNEXIS");
							resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), "Lexis Nexis");

							try {
								parseAddresses(resultMap, searchId);
								parseNames(resultMap, searchId);
								
							} catch (Exception e) {
								e.printStackTrace();
							}
							resultMap.removeTempDef();
			    				
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<table BORDER='1' CELLPADDING='2'>" + 
									rowHtml + "</table>");
								
							Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
							document = (RegisterDocumentI)bridge.importData();		
								
							currentResponse.setDocument(document);
							
							HashMap<String, String> data = new HashMap<String, String>();
							data.put("type", "LEXISNEXIS");
							
							String checkBox = "checked";
							if (isInstrumentSaved(documentNumber, null, data) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
								checkBox = "saved";
					    	} else {
					    		numberOfUncheckedElements++;
					    		LinkInPage linkInPage = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
					    		checkBox = "<input type=\"checkbox\" name=\"docLink\" value=\"" + link + "\">";
					    		if (link.contains("noDetails=true")){
						    		if(getSearch().getInMemoryDoc(link)==null){
			            				getSearch().addInMemoryDoc(link, currentResponse);
			            			}
					    		}
					    		currentResponse.setPageLink(linkInPage);
					    	}
							rowHtml = rowHtml.replaceAll("(?is)<td[^>]*>\\s*<input type=\\\"checkbox\\\"[^>]+>", 
									"<td  align=\"justify\" width=\"5%\" nowrap><font face=\"Verdana\" size=\"1\" rowspan=" + count + ">" + checkBox);
							currentResponse.setOnlyResponse(rowHtml);
							newTable.append(currentResponse.getResponse());
								
							count++;
							intermediaryResponse.add(currentResponse);
							
						} else {
							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "LN");
							tmpPartyGtor = StringEscapeUtils.unescapeHtml(tmpPartyGtor);
							resultMap.put("tmpPartyGtor", tmpPartyGtor);
							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNumber);
							
							try {

							} catch (Exception e) {
								e.printStackTrace();
							}
							resultMap.removeTempDef();
							Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
							RegisterDocumentI documentTemp = (RegisterDocumentI)bridge.importData();
			    			
							for(NameI nameI : documentTemp.getGrantor().getNames()) {
								if(!document.getGrantor().contains(nameI)) {
									document.getGrantor().add(nameI);
			    				}
			    			}
							for(NameI nameI : documentTemp.getGrantor().getNames()) {
								if(!document.getGrantor().contains(nameI)) {
									document.getGrantor().add(nameI);
			    				}
			    			}
							String rawServerResponse = (String)currentResponse.getAttribute(ParsedResponse.SERVER_ROW_RESPONSE);
			    				
							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rawServerResponse);
			    				
							String responseHtml = currentResponse.getResponse();
							String countString = StringUtils.extractParameter(responseHtml, "rowspan=(\\d)+");
							try {
								int count = Integer.parseInt(countString);
								responseHtml = responseHtml.replaceAll("rowspan=(\\d)+", "rowspan=" + (count + 1));
			    					
								currentResponse.setOnlyResponse(responseHtml);
			    			} catch (Exception e) {
			    				e.printStackTrace();
							}	
						}

				}
			}
			String resp = response.getResult();
			String action = "";
			Form form = new SimpleHtmlParser(resp).getForm(LEXISFORM);
			if (form != null){
				action = form.action;
				if (StringUtils.isNotEmpty(action)){
					Matcher mat = PREV_PARAM_PAT.matcher(resp);
					if (mat.find()){
						String prevLink =  "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/nexisprma/US/" + action + "&nav=" + mat.group(1) + "\">Previous</a>";
						navRow += prevLink;
					}
					mat.reset();
					mat = CURR_PAGE_PAT.matcher(resp);
					if (mat.find()){
						navRow += "&nbsp;&nbsp;Page " + mat.group(1) + "&nbsp;&nbsp;";
					}
					
					mat.reset();
					mat = NEXT_PARAM_PAT.matcher(resp);
					if (mat.find()){
						String nextLink = "<a href=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/nexisprma/US/" + action + "&nav=" + mat.group(1) + "\">Next</a>";
						navRow += nextLink;
						
						if (mSearch.getSearchType() == Search.AUTOMATIC_SEARCH){
					    	response.getParsedResponse().setNextLink(nextLink);
					    }
					}
				}
				navRow += "<br>";
				//isolate parameters
				Map<String,String> addParams = NexisConnLN.isolateParams(resp, LEXISFORM);
				if (NexisConnLN.checkParams(addParams)){
					mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsNav:", addParams);
				}
			}
			String report = "";
			NodeList tableReportList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("class", "SearchDetails"), true);
			if (tableReportList.size() > 1){
				report = tableReportList.elementAt(1).toHtml();
			} else if (tableReportList.size() == 1){
				report = tableReportList.elementAt(0).toHtml();
			}
			if (StringUtils.isNotEmpty(report)){
				report = report.replaceAll("(?is)<a[^>]+>\\s*(?:Edit|New)\\s+Search\\s*</a>", "");
				report = report.replaceAll("(?is)</?a[^>]*>", "");
				report = report.replaceAll("(?is)</?img[^>]*>", "");
				report = report.replaceAll("(?is)\\s*\\(\\s*\\|\\s*\\)\\s*", "");
			}
			
			String header1 = rows[0].toHtml().replaceAll("(?is)<input type=\\\"checkbox\\\"[^>]+>", SELECT_ALL_CHECKBOXES);
				
			response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "GET") 
								+ "<br>" + report + "<br><br><br>" + navRow + "<br><br>"
								+ "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
				
			response.getParsedResponse().setFooter("</table>" + 
										"<br>" + navRow + "<br><br>" + report + "<br><br>" +  CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101, -1));
		
		newTable.append("</table>");
		outputTable.append(newTable);
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	 public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
			DocumentI document = null;
			StringBuilder justResponse = new StringBuilder(detailsHtml);
			try {
				ResultMap map = new ResultMap();
								
				parseAndFillResultMap(response, detailsHtml, map);
				
				String tmpDob = (String) map.get("tmpDob");
				String tmpSsn = (String) map.get("tmpSsn");
				map.removeTempDef();
				
				Bridge bridge = new Bridge(response.getParsedResponse(), map, searchId);
				
				document = bridge.importData();
				((LexisNexisDocumentI)document).setDOB(tmpDob);
				((LexisNexisDocumentI)document).setSsn(tmpSsn);
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			if(fillServerResponse) {
				response.getParsedResponse().setOnlyResponse(justResponse.toString());
				if(document!=null) {
					response.getParsedResponse().setDocument(document);
				}
			}
			
			return document;
		}
	 
	 private String getInstrNoFromBigIntGhertzo(String name, String address) {
		 try {
			 BigInteger nameBigInt = new BigInteger("0");
			 BigInteger addressBigInt = new BigInteger("0");
			 name = name.replaceAll("(?is)</?span[^>]*>", "").replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)</?br[^>]*>", "")
			 							.replaceAll("\\p{Punct}", "").replaceAll("\\s", "");
			 address = address.replaceAll("(?is)</?span[^>]*>", "").replaceAll("(?is)</?br[^>]*>", "").replaceAll("(?is)&nbsp;", " ")
									.replaceAll("\\p{Punct}", "").replaceAll("\\s", "");
			 if (StringUtils.isNotEmpty(name)){
				 nameBigInt = new BigInteger(name.getBytes());
			 }
			 if (StringUtils.isNotEmpty(address)){
				 addressBigInt = new BigInteger(address.getBytes());
			 }
			 return (addressBigInt.add(nameBigInt)).mod(numberToDivide).toString();
		 } catch (Exception e) {
			 logger.error("biginteger bigoperation failed", e);
			 return "0000";
		}
	}
	 
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "LN");
			detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", " ");
			SimpleDateFormat formatter = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY);
	        String sDate = formatter.format(Calendar.getInstance().getTime());
			resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), sDate);
			resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), sDate);
			
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			String docNo = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "LexID(sm)"), "", true).trim();
			if (StringUtils.isEmpty(docNo)){
				docNo = StringUtils.extractParameterFromUrl(response.getQuerry(), "docNumber");
			}
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), docNo);
										
			String dob = "", address = "", name = "", ssn = "";
			
			dob = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "DOB"), "", true).trim();
			address = HtmlParser3.getValueFromAbsoluteCell(1, 1, HtmlParser3.findNode(mainList, "Full Name"), "", true).trim();
			name = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Full Name"), "", true).trim();
			if (StringUtils.isEmpty(address) || StringUtils.isEmpty(name)){//for company search
				Div div = (Div) mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "SubjectDetailSection_EXPSEC_SubSum"), true).elementAt(0);
				if (div != null){
					TableTag table = (TableTag) div.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
												.extractAllNodesThatMatch((new HasAttributeFilter("class", "main")), true).elementAt(0);
					if (table != null){
						TableRow[] rows = table.getRows();
						if (rows.length > 0){
							TableColumn[] cols = rows[0].getColumns();
							if (cols.length > 1){
								if (StringUtils.isEmpty(name)){
									name = cols[0].childAt(0).toHtml().trim();
								}
								if (StringUtils.isEmpty(address)){
									address = cols[1].getChildren().toHtml().trim();
									resultMap.put("tmpAddresses", address);
								}
							}
						}
					}
				}
			}
			ssn = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "SSN"), "", true).trim();
			
			if (mainList != null && mainList.size() > 0){
				Div div = (Div) mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "Addresses_EXPSEC_AddrSum"), true).elementAt(0);
				if (div != null){
					TableTag table = (TableTag) div.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true)
										.extractAllNodesThatMatch(new HasAttributeFilter("class", "main"), true).elementAt(0);
					if (table != null){
						TableRow[] rows = table.getRows();
						StringBuilder addresses = new StringBuilder();
						for(int i = 1; i < rows.length; i++ ) {
							TableRow row = rows[i];
							if(row.getColumnCount() > 1) {
								TableColumn[] cols = row.getColumns();
								addresses.append(cols[1].getChildren().toHtml().replaceAll("(?is)<div>.*?</div>", "")).append("@@#@@");
							}
						}
						
						if (addresses.length() > 0){
							resultMap.put("tmpAddresses", addresses.toString());
						}
					}
				}  
			}
						
			dob = dob.replaceAll("(?is)</?div[^>]*>", " ");
			dob = dob.replaceFirst("(?is)\\(\\s*Age\\s*:\\d+\\s*\\)", "");
			resultMap.put("tmpDob", dob.trim());
			
			ssn = ssn.replaceAll("(?is)</?div[^>]*>", " ");
			ssn = ssn.replaceFirst("<.*", "");
			resultMap.put("tmpSsn", ssn.trim());
			
			if (StringUtils.isNotEmpty(name)){
				name = name.replaceAll("(?is)&nbsp;", " ").trim();
				resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), name);
			}
			
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "LEXISNEXIS");
			resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), "Lexis Nexis");
			
			parseNames(resultMap, searchId);
			parseAddresses(resultMap, searchId);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected String getDetails(String response, ServerResponse Response){
		
		// if from memory - use it as is
		if(!response.toLowerCase().contains("<html")){
			return response;
		}
		
		String details = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainList = htmlParser.parse(null);

			if (mainList != null && mainList.size() > 0){
				Div div = (Div) mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "main"), true).elementAt(0);
				if (div != null){
					div.getChildren().keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("id", "topRptHeader")), true);
					div.getChildren().keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("id", "footer")), true);
					div.getChildren().keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("class", "key")), true);
					details = div.toHtml();
					details = details.replaceFirst("(?is)(<div)\\s+", "$1 align=\"center\"");
					details = details.replaceAll("(?is)style=\\\"[^\\\"]+\\\"", "");
					details = details.replaceAll("(?is)•", " - ");
				}  
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		details = details.replaceAll("(?is)<img[^>]*>", "");
		details = details.replaceAll("(?is)</?a[^>]*>", "");
		
		return details;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap m, long searchId) throws Exception{
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		ArrayList<List> grantor = new ArrayList<List>();
		
		String tmpPartyGtor = (String)m.get(SaleDataSetKey.GRANTOR.getKeyName());
		if (StringUtils.isNotEmpty(tmpPartyGtor)){
			
			String[] grantors = tmpPartyGtor.split("\\s*/\\s*");
			for (String gtor : grantors) {
			
				names = StringFormats.parseNameNashville(gtor, true);
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
					
				GenericFunctions.addOwnerNames(gtor, names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), grantor);
			}
		}
			
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
	}
	
	public static void parseAddresses(ResultMap m, long searchId) throws Exception{
		
		String tmpAddresses = (String) m.get("tmpAddresses");
		
		Vector<PropertyIdentificationSet> pisVector = new Vector<PropertyIdentificationSet>();
		if (StringUtils.isNotEmpty(tmpAddresses)){
			String[] addresses = tmpAddresses.split("@@#@@");
			for (String address : addresses) {
				String[] items = address.split("(?is)<br/?>");
				PropertyIdentificationSet tmpPis = new PropertyIdentificationSet();
				for (int i = 0; i < items.length; i++) {
					if (i == 0){
						tmpPis.setAtribute("StreetName", StringFormats.StreetName(items[i]));
						tmpPis.setAtribute("StreetNo", StringFormats.StreetNo(items[i]));
					} else if (i == 1){
						tmpPis.setAtribute("City", items[i].replaceAll("(?is)([^,]+),.*", "$1"));
					} else if (i == 2){
						tmpPis.setAtribute("County", items[i].replaceAll("(?is)([^$]+)\\s+COUNTY", "$1"));
					}
				}
				pisVector.add(tmpPis);
			}
			m.put("PropertyIdentificationSet", pisVector);
		}
		
	}
	
	@Override
	protected void logSearchBy(TSServerInfoModule module, Map<String, String> params){
    	
    	if(module.isVisible() || "GB_MANAGER_OBJECT".equals(module.getSaObjKey())) {//B 4511
        
	    	// get parameters formatted properly
	        Map<String,String> moduleParams = params;
	        if(moduleParams == null){
	        	moduleParams = module.getParamsForLog();
	        }
	        Search search = getSearch();
	        // determine whether it's an automatic search
	        boolean automatic = (search.getSearchType() != Search.PARENT_SITE_SEARCH) 
	        		|| (GPMaster.getThread(searchId) != null);
	        boolean imageSearch = module.getLabel().equalsIgnoreCase("image search") || 
	                              module.getModuleIdx() == TSServerInfo.IMG_MODULE_IDX;
	        
	        // create the message
	        StringBuilder sb = new StringBuilder();
	        SearchLogFactory sharedInstance = SearchLogFactory.getSharedInstance();
	        SearchLogPage searchLogPage = sharedInstance.getSearchLogPage(searchId);
	        sb.append("</div>");
	        
	        Object additional = GetAttribute("additional");
			if(Boolean.TRUE != additional){
	        	searchLogPage.addHR();
	        	sb.append("<hr/>");	
	        }
			int fromRemoveForDB = sb.length();
	        
			//searchLogPage.
	        sb.append("<span class='serverName'>");
	        String serverName = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName();
			sb.append(serverName);
	        sb.append("</span> ");
	
	       	sb.append(automatic? "automatic":"manual");
	       	Object info = module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION);
	       	if(StringUtils.isNotEmpty(module.getLabel())) {
		        
		        if(info!=null){
		        	sb.append(" - " + info + "<br>");
		        }
		        sb.append(" <span class='searchName'>");
		        sb.append(module.getLabel());
	       	} else {
	       		sb.append(" <span class='searchName'>");
		        if(info!=null){
		        	sb.append(" - " + info + "<br>");
		        }
	       	}
	        sb.append("</span> by ");
	        
	        boolean firstTime = true;
	        for(Entry<String,String> entry : moduleParams.entrySet() ){
	        	String key = entry.getKey();
	        	String value = entry.getValue();
	        	value = value.replaceAll("(, )+$","");
	        	if (key.contains("DPPA") || key.contains("GLBA")) {
	        		String select = DPPA_USE;
	        		if (key.contains("GLBA")) {
	        			select = GLBA_USE;
	        		}
	        		Matcher ma = Pattern.compile("(?is)<option[^>]+value=\"" + value + "\"[^>]+>([^<]+)<").matcher(select);
	        		if (ma.find()) {
	        			value = ma.group(1);
	        		}
	        	}if(!firstTime){
	        		sb.append(", ");
	        	} else {
	        		firstTime = false;
	        	}
	        	sb.append(entry.getKey().replaceAll("&lt;br&gt;", "") + " = <b>" + value + "</b>");
	        } 
	        int toRemoveForDB = sb.length();
	        //log time when manual is starting        
	        if (!automatic || imageSearch){
	        	sb.append(" ");
	        	sb.append(SearchLogger.getTimeStamp(searchId));
	        }
	        sb.append(":<br/>");
	        
	        // log the message
	        SearchLogger.info(sb.toString(),searchId);   
	        ModuleShortDescription moduleShortDescription = new ModuleShortDescription();
	        moduleShortDescription.setDescription(sb.substring(fromRemoveForDB, toRemoveForDB));
	        moduleShortDescription.setSearchModuleId(module.getModuleIdx());
	        search.setAdditionalInfo(TSServerInfoConstants.TS_SERVER_INFO_MODULE_DESCRIPTION, moduleShortDescription);
	        String user=InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getAttribute(1).toString();
	        SearchLogger.info(StringUtils.createCollapsibleHeader(),searchId);
	        searchLogPage.addModuleSearchParameters(serverName,additional, info, moduleParams,module.getLabel(), automatic, imageSearch,user);
    	}  
        
    }

}
		
