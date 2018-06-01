package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.w3c.dom.Node;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParserTidy;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.CRC;

import com.stewart.ats.base.document.DeathRecordsDocumentI;
import com.stewart.ats.base.document.DocumentI;

@SuppressWarnings("deprecation")
public class TXGenericDR extends TSServer {
	
	static final long serialVersionUID = 48301942L;
		
	//for Year combo box
	private static String YEAR = "";

	static {
		String folderPath = ServerConfig
				.getModuleDescriptionFolder(BaseServlet.REAL_PATH
						+ "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath
					+ "] does not exist. Module Information not loaded!");
		}
		try {
			String selects = FileUtils.readFileToString(new File(folderPath
					+ File.separator + "TXGenericDRYear.xml"));
			YEAR = selects;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getYEAR() {
		return YEAR;
	}
	
	public TXGenericDR(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	public TXGenericDR(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault
				.getModule(TSServerInfo.NAME_MODULE_IDX);

		if (tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(4),
					YEAR);
		}

		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	@Override
	protected TSServer.ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent, boolean forceOverritten) {
		TSServer.ADD_DOCUMENT_RESULT_TYPES status = super.addDocumentInATS(response, htmlContent, forceOverritten);
		if(mSearch.getSearchType() == Search.AUTOMATIC_SEARCH && status == ADD_DOCUMENT_RESULT_TYPES.ADDED) {
			mSearch.setAdditionalInfo(getCurrentServerName() + ":docsSavedInAutomatic:"
					+ response.getParsedResponse().getGrantorNameSet(0).getAtribute("OwnerLastName").toUpperCase()	//owner surname
					+ response.getParsedResponse().getGrantorNameSet(0).getAtribute("OwnerFirstName").toUpperCase()	//owner given name
					, new Boolean(true));
		}
		
		return status;
	};
	
	/*if there is at least one document saved when searching with name (exact) and county, the following two searches aren't performed anymore
	if there is at least one document saved when searching with name (metaphone) and county, the following search isn't performed anymore*/
	@Override
	protected ServerResponse SearchBy(boolean resetQuery, TSServerInfoModule module, Object sd)throws ServerResponseException {
		
		if (mSearch.getSearchType() == Search.AUTOMATIC_SEARCH){
			Object extraInfo = module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION);
			if (extraInfo != null && extraInfo.equals(TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS)) {
				Boolean docsSavedInAutomatic = (Boolean) mSearch.getAdditionalInfo(getCurrentServerName() + ":docsSavedInAutomatic:"
						+ module.getFunction(0).getParamValue().toUpperCase() 		//owner surname
						+ module.getFunction(2).getParamValue().toUpperCase());		//owner given name
				if(docsSavedInAutomatic == null || docsSavedInAutomatic.booleanValue() == false) {
					return super.SearchBy(resetQuery, module, sd);
				} else {
					return new ServerResponse();
				}
			}
		}
		
		return super.SearchBy(resetQuery, module, sd);
	}
		
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> moduleList = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		
		boolean hasOwner = hasOwner();
								
		if(hasOwner) {		//search with name (exact) and county, filter by name
			FilterResponse nameFilterOwner = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
			nameFilterOwner.setSkipUnique(false);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.forceValue(1, "Exact");
			module.setSaKey(3, SearchAttributes.P_COUNTY_NAME);
			module.addFilter(nameFilterOwner);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;","L;M;"}));
			moduleList.add(module);
		}
		
		if(hasOwner) {		//search with name (metaphone) and county, filter by name
			FilterResponse nameFilterOwner = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
			nameFilterOwner.setSkipUnique(false);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.forceValue(1, "Metaphone");
			module.setSaKey(3, SearchAttributes.P_COUNTY_NAME);
			module.addFilter(nameFilterOwner);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;","L;M;"}));
			moduleList.add(module);
		}
		
		if(hasOwner) {	//search with name (exact), filter by name
			FilterResponse nameFilterOwner = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
			nameFilterOwner.setSkipUnique(false);
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.forceValue(1, "Exact");
			module.addFilter(nameFilterOwner);
			module.addIterator((ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId, new String[] {"L;F;","L;M;"}));
			moduleList.add(module);
		}
				
		serverInfo.setModulesForAutoSearch(moduleList);
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();	
		
		switch (viParseID) {
		
			case ID_SEARCH_BY_NAME :
			    
			    if (rsResponse.indexOf("Nothing found matching your search criteria") != -1){
			    	Response.getParsedResponse().setError("No results found");
	            	return;
	            }
	            
			    //start and end indexes of navigation links (Next, Previous etc.)
			    int navigationStart = 0;
			    int navigationEnd = 0;
			    
			    //table with intermediary results
			    String table = "";
			    Node tableNode = HtmlParserTidy.getNodeByTagAndAttr(rsResponse, "table", "border", "1");
			    if (tableNode != null)
			    {
			    	try {
			    		table = HtmlParserTidy.getHtmlFromNode(tableNode);
			    		navigationStart = rsResponse.indexOf("table") + table.length();
						table = table.replaceAll("(?i)<a href=\".*?\">(.*?)</a>", "$1");		//remove links
					} catch (TransformerException e) {
						e.printStackTrace();
					}
			    }
			    
			    //navigation links
			    navigationStart = rsResponse.indexOf("<a", navigationStart);
			    navigationEnd = rsResponse.indexOf("<br>", navigationStart);
			    String navLinks = rsResponse.substring(navigationStart, navigationEnd);
			    //if there is no "Next" or "Previous" link, the results are on a single page, so there are no navigation links 
			    if (navLinks.indexOf("Next")==-1 && navLinks.indexOf("Previous")==-1) navLinks = "";
			    else 
			    {
			    	String link = CreatePartialLink(TSConnectionURL.idGET);
					navLinks = navLinks.replaceAll("href=\"(.*?)\"", "href=\"" + link + "$1\"");		//replace links
					navLinks = "<br><center>"+ navLinks + "</center>";
			    }
			    
				StringBuilder outputTable = new StringBuilder();
				
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, 
						table, outputTable);
				
				if(smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					
		            if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = parsedResponse.getHeader();
		               	String footer = parsedResponse.getFooter();                           	
		            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		            	header += "<table border=\"0\" width=\"100%\"><tr><td>";
		               	header += "\n<table width=\"100%\" cellspacing=\"0\">\n" +
		            			"<tr><th rowspan=1>" + SELECT_ALL_CHECKBOXES + "</th>" +
		            	        "<th align=\"left\">Last Name</th>" +
		            			"<th align=\"left\">First Name</th>" +
		            			"<th align=\"left\">Middle Name</th>" +
		            			"<th align=\"left\">Suffix</th>" +
		            			"<th align=\"left\">Date</th>" +
		            			"<th align=\"left\">County</th>" +
		            			"<th align=\"left\">Sex</th>" +
		            			"<th align=\"left\">Marriage Status</th></tr>";
		            	
		            	footer += "</td></tr></table></td></tr></table>" + navLinks;
		            	
		            	Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
		            	if(numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
		            		footer += "<br>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer)numberOfUnsavedDocument);
		            	} else {
		            		footer += "<br>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
		            	}
		            	
		            	parsedResponse.setHeader(header);
		            	parsedResponse.setFooter(footer);
		            }
					
					
				} 
				break;
				
			case ID_DETAILS :
				
				DocumentI document = parsedResponse.getDocument();
				
				if(document!= null) {
					msSaveToTSDFileName = document.getInstno() + ".html";
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
				}
				break;
			
			case ID_GET_LINK :
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				break;
				
			case ID_SAVE_TO_TSD :
				document = parsedResponse.getDocument();
				
				if(document!= null) {
					msSaveToTSDFileName = document.getInstno() + ".html";
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
				} else {
					ParseResponse(sAction, Response, ID_DETAILS);
				}
				break;

		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
		StringBuilder newTable = new StringBuilder();
		newTable.append("<table width=\"100%\"");
		int numberOfUncheckedElements = 0;
		
		if(table != null) {
			try {
				org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(table, null);
				NodeList rows = parser.extractAllNodesThatMatch(new TagNameFilter("tr"));
				newTable.append("<tr><th>Last Name</th><th>First Name</th><th>Middle Name</th><th>Suffix</th><th>Date</th>"  +
						"<th>County</th><th>Sex</th><th>Marriage Status</th>");
				
				for (int i = 1; i < rows.size(); i++) {
					NodeList tdList = rows.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("td"));
					
					String lastName = tdList.elementAt(0).toPlainTextString().replaceAll("&nbsp;", "");
					String firstName = tdList.elementAt(1).toPlainTextString().replaceAll("&nbsp;", "");
					String middleName = tdList.elementAt(2).toPlainTextString().replaceAll("&nbsp;", "");
					String suffix = tdList.elementAt(3).toPlainTextString().replaceAll("&nbsp;", "");
					String date = tdList.elementAt(4).toPlainTextString().replaceAll("&nbsp;", "");
					String county = tdList.elementAt(5).toPlainTextString().replaceAll("&nbsp;", "");
					String sex = tdList.elementAt(6).toPlainTextString().replaceAll("&nbsp;", "");
					String marriageStatus = tdList.elementAt(7).toPlainTextString().replaceAll("&nbsp;", "");
					
					String text = "LastName:" + lastName + "_FirstName:" + firstName + "_" + middleName + "_" + suffix + "_" + 
						date + "_" + county + "_" + sex + "_" + marriageStatus;
					String documentNumber = "dr" + CRC.quick(text);
					ParsedResponse currentResponse = new ParsedResponse();
					responses.put(documentNumber, currentResponse);
					ResultMap resultMap = new ResultMap();
							
					String responseHtml = "<tr>" +
						tdList.elementAt(0).toHtml() +
						tdList.elementAt(1).toHtml() +
						tdList.elementAt(2).toHtml() +
						tdList.elementAt(3).toHtml() + 
						tdList.elementAt(4).toHtml() +
						tdList.elementAt(5).toHtml() +
						tdList.elementAt(6).toHtml() +
						tdList.elementAt(7).toHtml() + "</tr>"; 
										
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNumber);
			    	resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), date);
			    	resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "DEATH CERTIFICATE");
			    		
			    	String grantorName = lastName + ", " + firstName;
			    	if (middleName.length()!=0) grantorName += " "  + middleName;
			    	if (suffix.length()!=0) grantorName += " "  + suffix;
			    	if (!grantorName.trim().equals(",")) resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), grantorName);
			    	ArrayList<List> grantor = new ArrayList<List>();
					String [] names = {firstName, middleName, lastName, "", "" , ""};
			    	String [] nothingArray = {"", ""};
			    	GenericFunctions.addOwnerNames(grantorName, names, suffix, "", nothingArray, nothingArray, false, false, grantor);
			    	resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			    	
					String granteeName = "County of " + county;
					resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), granteeName);
					ArrayList<List> grantee = new ArrayList<List>();
					GenericFunctions.addOwnerNames(new String[] {granteeName, "", ""} , "", false, grantee);
					resultMap.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
												
			    	resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), granteeName);
			    			
			    	resultMap.put("OtherInformationSet.SrcType", "DR");
			    						    				
					Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
															
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<table BORDER='1' CELLPADDING='2'>" +
							rows.elementAt(0).toHtml() + responseHtml + "</table>");
					DeathRecordsDocumentI document = (DeathRecordsDocumentI)bridge.importData();
					document.setSex(sex);
					document.setMarriageStatus(marriageStatus);
							
					currentResponse.setDocument(document);
							
					String checkBox = "checked";
					if (isInstrumentSaved(documentNumber, document, null)) {
						checkBox = "saved";
					} else {
						numberOfUncheckedElements++;
						String linkPrefix = getLinkPrefix(TSConnectionURL.idPOST);
						LinkInPage linkInPage = new LinkInPage(
								linkPrefix + documentNumber, 
								linkPrefix + documentNumber, 
								TSServer.REQUEST_SAVE_TO_TSD);
						checkBox = "<input type='checkbox' name='docLink' value='" + 
						linkPrefix + documentNumber + "'>";
					   	
						if(getSearch().getInMemoryDoc(linkPrefix + documentNumber)==null){
							getSearch().addInMemoryDoc(linkPrefix + documentNumber, currentResponse);
						}
						currentResponse.setPageLink(linkInPage);
			            						    			
				 	}
					currentResponse.setOnlyResponse("<tr><th rowspan=1>" + checkBox + "</th>" + responseHtml.substring(responseHtml.indexOf("<tr>") + 4));
					newTable.append(currentResponse.getResponse());
										
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}		
		}
		newTable.append("</table>");
		outputTable.append(newTable);
		SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
		return responses.values();
	}
}
