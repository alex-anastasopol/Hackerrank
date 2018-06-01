package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.axis2.AxisFault;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.CRC;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.MostCommonName;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.Patriots;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.connection.stewart.PAConn;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewart.specialalerts.SearchStub;
import com.stewart.specialalerts.SearchStub.AlertMatch;
import com.stewart.specialalerts.SearchStub.AlertMatches;
import com.stewart.specialalerts.SearchStub.AlertMatches_type0;
import com.stewart.specialalerts.SearchStub.Entry_type0;
import com.stewart.specialalerts.SearchStub.Entry_type1;
import com.stewart.specialalerts.SearchStub.Matches;
import com.stewart.specialalerts.SearchStub.SdnMatch;
import com.stewart.specialalerts.SearchStub.SdnMatches;
import com.stewart.specialalerts.SearchStub.SdnMatches_type0;
import com.stewart.specialalerts.SearchStub.SearchAlerts;
import com.stewart.specialalerts.SearchStub.SearchAlertsE;
import com.stewart.specialalerts.SearchStub.SearchAlertsRefined;
import com.stewart.specialalerts.SearchStub.SearchAlertsRefinedE;
import com.stewart.specialalerts.SearchStub.SearchAlertsRefinedResponse;
import com.stewart.specialalerts.SearchStub.SearchAlertsResponse;
import com.stewart.specialalerts.SearchStub.SearchE;
import com.stewart.specialalerts.SearchStub.SearchRefinedResponse;
import com.stewart.specialalerts.SearchStub.SearchResponse;
import com.stewart.specialalerts.SearchStub.SearchSdns;
import com.stewart.specialalerts.SearchStub.SearchSdnsE;
import com.stewart.specialalerts.SearchStub.SearchSdnsRefined;
import com.stewart.specialalerts.SearchStub.SearchSdnsRefinedE;
import com.stewart.specialalerts.SearchStub.SearchSdnsRefinedResponse;
import com.stewart.specialalerts.SearchStub.SearchSdnsResponse;

public class XXStewartPA extends TSServerPA {

    static final long serialVersionUID = 10000000;
    
    transient private PAConn searchStub;
    

    public XXStewartPA(long searchId) {
    	super(searchId);
        resultType = MULTIPLE_RESULT_TYPE;
    }
    
    public static final Pattern RESULT_TD = Pattern.compile("<td class=\"TdNameOdd\">(.*?)</td>");
    
    protected class Results{
    	private int numberOfResults = 0;
    	private String results = "";
		
    	protected Results(){
    	}
    	
    	protected Results(int numberOfResults, String results){
    		this.numberOfResults = numberOfResults;
    		this.results = results;
    	}
    	
    	int getNumberOfResults() {
			return numberOfResults;
		}
		void setNumberOfResults(int numberOfResults) {
			this.numberOfResults = numberOfResults;
		}
		
		String getResults() {
			return results;
		}
		void setResults(String results) {
			this.results = results;
		}
    }

    public XXStewartPA(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
       
        try {
			searchStub = new PAConn(dataSite, searchId);
			
		} catch (AxisFault e){
			e.printStackTrace();
		}
        resultType = MULTIPLE_RESULT_TYPE;
    }

    @Override
    public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException{
    	Search currentSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
    	String value1  = module.getFunction(0).getParamValue();
    	String value2 = module.getFunction(1).getParamValue();;
    	
        if (value1.length() == 0 && value2.length() == 0){
            ServerResponse sr=new ServerResponse();
            sr.setError("Search parameters invalid(empty)");            
        	throw new ServerResponseException(sr);
        }	
        if(!this.isParentSite()){
			boolean makeSearch = isValidSearch(currentSearch, value2, value1);
			if(!makeSearch ){
				SearchLogger.info("<!--Invalid parameters,search type ="+currentSearch.getSearchType()+"-->", searchId);
				return new ServerResponse();
			}
        }
		ServerResponse serverResponse = searchBy(module, sd, null);
		
        return serverResponse;
    }

    @Override
    protected ServerResponse searchBy(TSServerInfoModule module, Object sd, String fakeResult) throws ServerResponseException {
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		global.clearClickedDocuments();
		
		int parserID = module.getParserID();
		
		Map<String, String[]> multiParams = new HashMap<String, String[]>();
		List<TSServerInfoModule> modules = getMultipleModules(module, sd);
		
		if (modules.size() > 1){
			List<ServerResponse> serverResponses = new ArrayList<ServerResponse>();
			Vector<ParsedResponse> prs = new Vector<ParsedResponse>();
			for (int i = 0; i < modules.size(); i++){
				try {
					TSServerInfoModule mod = modules.get(i);
					global.clearClickedDocuments();
	
					Map<String, String> params = getNonEmptyParams(mod, multiParams);
					
					if (verifyModule(mod)){
						logSearchBy(mod);
						
						String result = performSearch(params, multiParams, mod.getModuleIdx(), fakeResult);
						
						ServerResponse resp = createPADocument(result);
						resp.setResult(result);
						
						serverResponses.add(resp);
						
						if (resp.getParsedResponse().getResultRows().size() > 0){
							prs.addAll(resp.getParsedResponse().getResultRows());
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			ServerResponse serverResponse = new ServerResponse();
			
			if (prs.size() > 0){
				serverResponse.getParsedResponse().setResultRows(prs);
				serverResponse.setResult("");
				solveHtmlResponse("", parserID, "SearchBy", serverResponse, serverResponse.getResult());
				
				return serverResponse;
			} else{
				return ServerResponse.createEmptyResponse();
			}
		}
		
		logSearchBy(module);
		
		Map<String, String> params = getNonEmptyParams(module, multiParams);
		
		String result = performSearch(params, multiParams, module.getModuleIdx(), fakeResult);
		ServerResponse response = createPADocument(result);
		response.setResult(result);	
		
		solveHtmlResponse("", parserID, "SearchBy", response, response.getResult());
		
		return response;
		
    }
    
    private ServerResponse createPADocument(String result){
    	
    	ServerResponse serverResponse = new ServerResponse();
    	ParsedResponse parsedResponse = new ParsedResponse();
    	Vector<ParsedResponse> parsedRows = new Vector<ParsedResponse>();
    	
    	long longKey = System.currentTimeMillis();
    	String keyNumber = "PA" + CRC.quick(result + longKey);
	        
    	InstrumentI instr = new com.stewart.ats.base.document.Instrument();	
    	instr.setDocType("PATRIOTS");
    	instr.setDocSubType("PATRIOTS");
    	instr.setInstno(keyNumber);
    	instr.setYear(Calendar.getInstance().get(Calendar.YEAR));
     		

    	Patriots patDoc = new Patriots(DocumentsManager.generateDocumentUniqueId(searchId, instr));
    	patDoc.setInstrument(instr);
     		
    	PartyI grantee1 = new Party(SimpleChapterUtils.PType.GRANTEE);
    	grantee1.add(new Name("", "", "Patriots ACT"));
     		
    	String grantor = "";
    	Pattern pat = Pattern.compile("(?is)\\bResults Found</td>\\s*</tr>\\s*<tr>\\s*<td[^>]*>(.*?)</td>");
    	Matcher mat = pat.matcher(result);
    	if (mat.find()){
    		grantor = mat.group(1).toUpperCase();
    	}
     		
    	PartyI grantor1 = new Party(SimpleChapterUtils.PType.GRANTOR);
    	Name gr = new Name("", "", grantor);
    	if (NameUtils.isCompany(grantor)){
    		gr.setCompany(true);
     	}
    	grantor1.add(gr);
    	patDoc.setGrantee(grantee1);
    	patDoc.setGrantor(grantor1);
    	patDoc.setServerDocType("PATRIOTS");
    	patDoc.setType(DType.ROLIKE);
    	patDoc.setDataSource("PA");
    	if (getSearch().getSa().getAtribute(SearchAttributes.P_STATE).equals("5")) {
    		patDoc.setChecked(false);		//b3328
    		patDoc.setIncludeImage(false);
    	} else{
    		patDoc.setChecked(true);
    		patDoc.setIncludeImage(true);
     	}
     		
    	patDoc.setSiteId(miServerID);
    	patDoc.setRecordedDate(Calendar.getInstance().getTime());
    	patDoc.setInstrumentDate(Calendar.getInstance().getTime());
         	
    	parsedResponse.setAttribute(PA_RECORD, patDoc);
    	parsedResponse.setResponse(result);
    	
        parsedRows.add(parsedResponse);
    	
        serverResponse.getParsedResponse().setResultRows(parsedRows);
        serverResponse.setResult("");
        
    	return serverResponse;
    }
    
    private String performSearch(Map<String, String> params,Map<String, String[]>  multiParams, int moduleIDX, String fakeResult){
    
    	String result = "";
    	switch (moduleIDX) {
    	case TSServerInfo.NAME_MODULE_IDX:

    		return search(params);

    	case TSServerInfo.TYPE_NAME_MODULE_IDX:
    		return searchRefined(params);
    		    		
		default:
			break;
    	}
		return result;
	}

	public String search(Map<String, String> params) {
		
		StringBuffer result = new StringBuffer();
		
		String firstName = params.get("firstName");
		String lastName = params.get("lastName");
		
		String name = lastName;
		if (org.apache.commons.lang.StringUtils.isNotEmpty(firstName)){
			name += ", " + firstName;
		}
		mSearch.setPatriotSearchName(name.toUpperCase());

		SearchResponse searchResponse = searchStub.search(params);
		
		if (searchResponse != null && searchResponse.getMatches() != null){
			Matches matches = searchResponse.getMatches();
			String currentDate = matches.getCurrentDate();
			String lastUpdated = matches.getLastUpdated();

			Results alertResults = getAlertMatchesAsString(matches, params);				
			int alertsFound = alertResults.getNumberOfResults();
			String alertRes = alertResults.getResults();
			
			Results sdnResults = getSdnMatchesAsString(matches, params);
			int sdnFound = sdnResults.getNumberOfResults();
			String sdnRes = sdnResults.getResults();
			
			int resultsFound = alertsFound + sdnFound;
			
			result.append("<table class=\"query-results\" border=\"0\" cellspacing=\"2\" cellpadding=\"2\" width=\"50%\">")
					.append("<tr><td width=\"70%\">Names Searched</td><td>Results Found</td></tr>");
			result.append("<tr><td class=\"TdNameOdd\">").append(lastName);

			if (org.apache.commons.lang.StringUtils.isNotEmpty(firstName)){
				result.append(", ").append(firstName);
			}
			result.append("</td>").append("<td class=\"TdNameOdd\">").append(resultsFound).append("</td></tr><tr><td><br><br>")
					.append(alertRes).append("<br><br>").append(sdnRes)
					.append("<br><br>").append("Current Date:&nbsp;").append(currentDate)
					.append("<br>").append("Last Updated:&nbsp;").append(lastUpdated).append("<br><br></td><td>&nbsp;</td></tr></table>");
		}
		
		return result.toString();
	}
	
	public String searchRefined(Map<String, String> params) {
		
		StringBuffer result = new StringBuffer();
		
		String firstName = params.get("firstName");
		String lastName = params.get("lastName");

		String name = lastName;
		if (org.apache.commons.lang.StringUtils.isNotEmpty(firstName)){
			name += ", " + firstName;
		}
		mSearch.setPatriotSearchName(name.toUpperCase());
		
		SearchRefinedResponse searchRefinedResponse = searchStub.searchRefined(params);
		
		if (searchRefinedResponse != null && searchRefinedResponse.getMatches() != null){
			Matches matches = searchRefinedResponse.getMatches();
			String currentDate = matches.getCurrentDate();
			String lastUpdated = matches.getLastUpdated();
			
			Results alertResults = getAlertMatchesAsString(matches, params);				
			int alertsFound = alertResults.getNumberOfResults();
			String alertRes = alertResults.getResults();
			
			Results sdnResults = getSdnMatchesAsString(matches, params);
			int sdnFound = sdnResults.getNumberOfResults();
			String sdnRes = sdnResults.getResults();
			
			int resultsFound = alertsFound + sdnFound;
			
			result.append("<table class=\"query-results\" border=\"0\" cellspacing=\"2\" cellpadding=\"2\" width=\"100%\">")
				.append("<tr><td>Names Searched</td><td>Results Found</td></tr>");
			result.append("<tr><td class=\"TdNameOdd\">").append(lastName);
			if (org.apache.commons.lang.StringUtils.isNotEmpty(firstName)){
				result.append(", ").append(firstName);
			}
			result.append("</td>").append("<td class=\"TdNameOdd\">").append(resultsFound).append("</td></tr></table><br><br>")
			
					.append(alertRes).append("<br><br>").append(sdnRes)
					.append("<br><br>").append("Current Date:&nbsp;").append(currentDate)
					.append("<br>").append("Last Updated:&nbsp;").append(lastUpdated);
		}
		return result.toString();
	}

	public Results getAlertMatchesAsString(Matches matches, Map<String, String> params) {
		
		String firstName = org.apache.commons.lang.StringUtils.defaultString(params.get("firstName"));
		String lastName = org.apache.commons.lang.StringUtils.defaultString(params.get("lastName"));
		String allName = (firstName.toLowerCase() + " " + lastName.toLowerCase()).trim();
		String allNameReverse = (lastName.toLowerCase() + " " + firstName.toLowerCase()).trim();
		Results results = new Results();
		
		String alertResults = "" ;
		int alertsFound = 0;
		
		AlertMatches_type0 alertMatches = matches.getAlertMatches();
		
		if (alertMatches.getEntry() == null){
			alertResults = "<b>No Results Found for Closing/Fraud Alerts</b>";
		} else{
			Entry_type0[] entryAlert = alertMatches.getEntry();
							
			AlertMatches valueAlert = entryAlert[0].getValue();
			AlertMatch[] alertMatchArray = valueAlert.getMatches();
			
			if (alertMatchArray == null){
				alertResults = "<b>No Results Found for Closing/Fraud Alerts</b>";
			} else{
				StringBuffer rows = new StringBuffer();
				String tableHeaderAlerts = "<table id=\"alerts\" border = \"1\" cellspacing=\"0\" cellpadding=\"10\" width=\"100%\"><tr><td  width=\"100%\"><b>Name</b></td>"
											+ "<td width=\"100%\"><b>Entity/Individual</b></td><td width=\"100%\"><b>Location</b></td>"
											+ "<td width=\"100%\"><b>Real Properties</b></td><td width=\"100%\"><b>Date</b></td><td width=\"100%\"><b>File</b></td></tr>";
				
				for (AlertMatch alertMatch : alertMatchArray){
					String name = alertMatch.getName();
					String description = alertMatch.getDescription();
					description = description.replaceAll("(?is)((\\r\n)+)", " ");
					description = description.replaceAll("(?is)</?\\s*br\\s*/?\\s*>", "; ");
					
					if (!name.toLowerCase().contains(allName) && !name.toLowerCase().contains(allNameReverse)
						 && !description.toLowerCase().contains(allName) && !description.toLowerCase().contains(allNameReverse)){
						continue;
					} else{
						alertsFound++;
						String date = alertMatch.getDate();
						String entity = alertMatch.getEntity();
						String file = alertMatch.getFile();
						if (file == null){
							file = "";
						}
						String href = alertMatch.getHref();
						String link = alertMatch.getLink();
						if (link == null){
							link = "";
						}
						String finalLink = link;
						if (org.apache.commons.lang.StringUtils.isNotEmpty(file)){
							finalLink = "<a target=\"_blank\" href=\"" + href + "\">Link</a>";
						} else if (org.apache.commons.lang.StringUtils.isNotEmpty(link)){
							finalLink = "<a target=\"_blank\" href=\"" + link + "\">Link</a>";
						}
						
						String location = alertMatch.getLocation();
						String realProperties = alertMatch.getRealProperties();
						
						rows.append("<tr><td>").append(tableHeaderAlerts);
						rows.append("<tr><td>").append(name).append("</td><td>").append(entity).append("</td><td>").append(location)
							.append("</td><td>").append(realProperties).append("</td><td>")
							.append(date).append("</td><td>").append(finalLink).append("</td></tr>")
							.append("<tr><td><b>Description</b></td><td colspan=\"5\">").append(description).append("</td></tr></table></td></tr>");
						
					}
				}
				StringBuffer table = new StringBuffer();
				if (alertsFound == 0){
					alertResults = "<b>No Results Found for Closing/Fraud Alerts</b>";
				} else{
					table.append("<br><br>")
							.append("<b>Closing/Fraud Alerts</b>").append("<br><br>")
							.append("<table border = \"0\" cellpadding=\"10\">")
							.append(rows.append("</table>").toString());
					
					alertResults = table.toString();
				}
			}
		}
		
		results.setNumberOfResults(alertsFound);
		results.setResults(alertResults);
		
		return results;
	}

	public Results getSdnMatchesAsString(Matches matches, Map<String, String> params) {
		
		String firstName = org.apache.commons.lang.StringUtils.defaultString(params.get("firstName"));
		String lastName = org.apache.commons.lang.StringUtils.defaultString(params.get("lastName"));
		String allName = (firstName.toLowerCase() + " " + lastName.toLowerCase()).trim();
		String allNameReverse = (lastName.toLowerCase() + " " + firstName.toLowerCase()).trim();
		
		String sdnResults = "";
		int sdnFound = 0;
		Results results = new Results();
		
		SdnMatches_type0 sdnMatches = matches.getSdnMatches();
				
		if (sdnMatches.getEntry() == null){
			sdnResults = "<b>No Results Found for Specially Designated Nationals</b>";
		} else{
			
			Entry_type1[] entrySdn = sdnMatches.getEntry();
			
			SdnMatches valueSdn = entrySdn[0].getValue();
			SdnMatch[] sdnMatchArray = valueSdn.getMatches();;
			
			if (sdnMatchArray == null){
				sdnResults = "<b>No Results Found for Specially Designated Nationals</b>";
			} else{
				StringBuffer rows = new StringBuffer();
				String tableHeaderSdn = "<table id=\"sdn\" border = \"1\" cellspacing=\"0\" cellpadding=\"10\" width=\"100%\"><tr><td><b>Name</b></td><td><b>Title Of Individual</b></td>"
						+ "<td><b>Alternate Identity</b></td><td><b>Street Address</b></td><td><b>City</b></td><td><b>Country</b></td><td><b>Sanctions Program</b></td></tr>";
				
				for (SdnMatch sdnMatch : sdnMatchArray) {
					String name = sdnMatch.getName();
					String alternateIdentity = sdnMatch.getAlternateIdentity();
					
					String titleOfIndividual = sdnMatch.getIndividual();
					String city = sdnMatch.getCity();
					String country = sdnMatch.getCountry();
					String sancProgram = sdnMatch.getSanctionsProgram();
					String streetAddress = sdnMatch.getStreetAddress();
					
					if (!name.replaceAll("\\s*,\\s*", " ").toLowerCase().contains(allName) 
								&& !name.replaceAll("\\s*,\\s*", " ").toLowerCase().contains(allNameReverse)
							 && !titleOfIndividual.replaceAll("\\s*,\\s*", " ").toLowerCase().contains(allName) 
							 		&& !titleOfIndividual.replaceAll("\\s*,\\s*", " ").toLowerCase().contains(allNameReverse)
							 && !alternateIdentity.replaceAll("\\s*,\\s*", " ").toLowerCase().contains(allName) 
							 		&& !alternateIdentity.replaceAll("\\s*,\\s*", " ").toLowerCase().contains(allNameReverse)){
							continue;
						} else{
						sdnFound++;
						
						rows.append("<tr><td>").append(tableHeaderSdn);
						rows.append("<tr><td>").append(name).append("</td><td>").append(titleOfIndividual).append("</td><td>").append(alternateIdentity)
							.append("</td><td>").append(streetAddress).append("</td><td>").append(city).append("</td><td>").append(country).append("</td><td>")
							.append(sancProgram).append("</td></tr></table>");
					}
				}
				if (sdnFound == 0){
					sdnResults = "<b>No Results Found for Specially Designated Nationals</b>";
				} else{
					StringBuffer table = new StringBuffer();
					table.append("<br><br>")
							.append("<b>Specially Designated Nationals</b>").append("<br><br>")
							.append("<table border = \"0\" cellpadding=\"10\">")
							.append(rows.append("</table>").toString());

					sdnResults = table.toString();
				}
			}
		}
		
		results.setNumberOfResults(sdnFound);
		results.setResults(sdnResults);
		
		return results;
	}
			    
	protected boolean isValidSearch(Search search,String firstname,String lastName){
    	try{
    		DocumentsManagerI manager = search.getDocManager();
    		try{
    			manager.getAccess();
		    	for(RegisterDocumentI doc:manager.getRoLikeDocumentList()){
					if("PATRIOTS".equals(doc.getDocType())){
						Date date = doc.getRecordedDate();
	                	GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
	                	cal.set(GregorianCalendar.MINUTE, 0);
	                	cal.set(GregorianCalendar.SECOND, 0);
	                	cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
	                	cal.set(GregorianCalendar.MILLISECOND, 0);
	                	
		   				if(date.getTime()==cal.getTime().getTime()){
		   					if(!StringUtils.isEmpty(firstname)){
			   					if( doc.getGrantorFreeForm().toUpperCase().startsWith((lastName+", "+firstname).toUpperCase()) ){
			   							return false;
			   					}
			   					if( doc.getGrantorFreeForm().toUpperCase().startsWith((lastName+", "+firstname).toUpperCase())){
				   						return false;
				   				}
		   					}
		   					else{
		   						if( doc.getGrantorFreeForm().equalsIgnoreCase(lastName.toUpperCase()) ){
		   							return false;
			   					}
			   					if(doc.getGrantorFreeForm().equalsIgnoreCase(lastName.toUpperCase())){
				   						return false;
				   				}
		   					}
		   				}
					}
				}
    		}
    		finally{
    			manager.releaseAccess();
    		}
    	}
		catch(Exception e){
			e.printStackTrace();
		}
    	return true;
    } 
    
    protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
    	
    	Search search = getSearch();
    	SearchAttributes sa = search.getSa();
    	List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
    	   	  	
    	// create list of different names
    	Set<Map<String,String>> names = new LinkedHashSet<Map<String,String>>();    	
    	
    	try {
    		Set<Map<String,String>> namesNew = new LinkedHashSet<Map<String,String>>();
    		
    		UserAttributes userAttrib = null;
    		try {
				userAttrib = search.getAgent();
			} catch (Exception e) {
				logger.error("Couldn't get agent for Patriots automatic search: " + searchId);
			}
			if (userAttrib != null){
	    		String agentFirstName = search.getAgent().getFIRSTNAME();
	        	String agentLastName = search.getAgent().getLASTNAME();
	        	if (StringUtils.isNotEmpty(agentLastName) && !MostCommonName.isMCLastName(agentLastName)){
	        		agentLastName = agentLastName.toUpperCase();
	        		
	        		Map<String,String> name = new LinkedHashMap<String,String>();
	        		name.put("last", agentLastName);
	        		
	        		if (StringUtils.isNotEmpty(agentFirstName) && agentFirstName.length() > 1){
	        			agentFirstName = agentFirstName.toUpperCase();
	        			
	        			if (!agentFirstName.equals(agentLastName)){
	        				name.put("first", agentFirstName);
	        			} else {
	        				name.put("first", "");
	        			}
	        		} else{
	        			name.put("first", "");
	        		}
	        		if (agentLastName.length() > 1){
	        			namesNew.add(name);
	        		}
	        	}
    		}
        	
        	String lender = sa.getAtribute(SearchAttributes.BM1_LENDERNAME);
        	if (StringUtils.isNotEmpty(lender)){
        		if (!lender.trim().toLowerCase().startsWith("to be ")){
	        		Map<String,String> name = new LinkedHashMap<String,String>();
	        		lender = lender.replaceAll("\\p{Punct}", " ").replaceAll("\\s+", " ").trim();
	        		name.put("last", lender.toUpperCase());
	        		name.put("first", "");
	        		
	        		if (lender.length() > 1 && !"TBD".equals(lender.trim())){
	        			namesNew.add(name);
	        		}
        		}
        	}
        	
    		if(namesNew.size() > 0) {
    			names.clear();
    			names.addAll(namesNew);
    		}
    	} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	// search with owners
    	TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
    	module.clearSaKeys();
    	module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
    	module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
		module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
		ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
													.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
		module.addIterator(nameIterator);
    	modules.add(module);
    	
    	// search with buyers
    	module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
    	module.clearSaKeys();
    	module.setSaObjKey(SearchAttributes.BUYER_OBJECT);
    	module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
		module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
													.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
		module.addIterator(nameIterator);
    	modules.add(module);
    	
    	// search with agent and lender 
    	for (Map<String,String> name: names){
    		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
    		module.clearSaKeys();
    		module.setData(0, name.get("last"));
    		module.setData(1, name.get("first"));
    		modules.add(module);
    	}
    	
    	serverInfo.setModulesForAutoSearch(modules);
    
    }

   protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
  
	   ConfigurableNameIterator nameIterator = null;
		
	   SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
	   List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	   TSServerInfoModule module;	
	   GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

	   for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	         module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
//		     module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
		 	 module.addIterator(nameIterator);
		
			 modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
			     module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
//				 module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;"} );
				 module.addIterator(nameIterator);
	
			 modules.add(module);
			 
		     }
	    }	 
	   serverInfo.setModulesForGoBackOneLevelSearch(modules);	     
    }

    protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
            
        String initialResponse = Response.getResult();
        String rsResponse = initialResponse;
        
        switch (viParseID){
        
        case ID_SEARCH_BY_NAME:
        case ID_SEARCH_BY_SUBDIVISION_NAME:
        	           
        	List<ParsedResponse> parsedResponses = (List<ParsedResponse>)Response.getParsedResponse().getResultRows();
        	if (parsedResponses.size()==0) {
        		Response.getParsedResponse().setResultRows(new Vector());	
 				return;
        	}
            
        	
        	if (parsedResponses.size() == 1){
        		ParsedResponse parsedResponse = parsedResponses.get(0);
        		
	        	Patriots doc = (Patriots) parsedResponse.getAttribute(PA_RECORD);
	        	parsedResponse.setDocument(doc);
	        	parsedResponse.setUseDocumentForSearchLogRow(true);
	        	
	        	String result = parsedResponse.getResponse();
	        	
	        	int poz = result.indexOf("No Results Found");
	        	if (!(poz > 0 && result.indexOf("No Results Found", poz + 5) > 0)){
	        		try{
	        			getSearch().addPatriotsAlertChapter(doc.getInstno());
	        		} catch(Exception e){
	        			e.printStackTrace();
	        		}
	        	}
	        	String keyNumber = doc.getInstno();
	        	
	        	String qry = Response.getQuerry();
                //din cauza ca se decodeaza qry-ul in Response.getQuerry(), "+" e inlocuit cu spatiu si va crapa la performLinkInPage e.g Bank of America in automatic FLLee
                qry = qry.replaceAll("\\s", "+");
				qry = "dummy=" + keyNumber + "&" + qry;
                String originalLink = sAction + "&" + qry;
                String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
                
                msSaveToTSDFileName = keyNumber + ".html";
                if (FileAlreadyExist(keyNumber + ".html") ) {
                	result += CreateFileAlreadyInTSD();
                } else {
                    mSearch.addInMemoryDoc(sSave2TSDLink, parsedResponse);
                    mSearch.addInMemoryDoc(sSave2TSDLink.replaceAll("\\+", " "), parsedResponse);
                    result = addSaveToTsdButton(result, sSave2TSDLink, viParseID);
                    
                    parsedResponse.setOnlyResponse(result);
                }

                LinkInPage lip = new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD);
                parsedResponse.setPageLink(lip);
                parsedResponse.setAttribute("viParseID", viParseID);
                
        	} else{
        		Vector<ParsedResponse> rows = new Vector<ParsedResponse>();
        		
        		StringBuilder newTable = new StringBuilder();
    			newTable.append("<table BORDER='1' CELLPADDING='2'>");
    			
        		for (ParsedResponse parsedResponse : parsedResponses) {
            		
    	        	Patriots doc = (Patriots) parsedResponse.getAttribute(PA_RECORD);
    	        	parsedResponse.setDocument(doc);
    	        	parsedResponse.setUseDocumentForSearchLogRow(true);
    	        	String result = parsedResponse.getResponse();
    	        	
    	        	int poz = result.indexOf("No Results Found");
    	        	if (!(poz > 0 && result.indexOf("No Results Found", poz + 5) > 0)){
    	        		try{
    	        			getSearch().addPatriotsAlertChapter(doc.getInstno());
    	        		} catch(Exception e){
    	        			e.printStackTrace();
    	        		}
    	        	}
    	        	String keyNumber = doc.getInstno();
    	        	
    	        	String qry = Response.getQuerry();
                    //din cauza ca se decodeaza qry-ul in Response.getQuerry(), "+" e inlocuit cu spatiu si va crapa la performLinkInPage e.g Bank of America in automatic FLLee
                    qry = qry.replaceAll("\\s", "+");
    				qry = "dummy=" + keyNumber + "&" + qry;
                    String originalLink = sAction + "&" + qry;
                    String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
                    
                    msSaveToTSDFileName = keyNumber + ".html";
                    if (FileAlreadyExist(keyNumber + ".html") ) {
                    	String row = "<tr><td>saved</td><td>" + result.toString() + "</td>" + "</tr>";
                    	parsedResponse.setOnlyResponse(row);
                    } else {
                        mSearch.addInMemoryDoc(sSave2TSDLink, parsedResponse);
                        mSearch.addInMemoryDoc(sSave2TSDLink.replaceAll("\\+", " "), parsedResponse);
                        
                        String checkBox = "<input type='checkbox' name='docLink' value='" + sSave2TSDLink + "'>";
        				String row = "<tr><td>" + checkBox + "</td><td>" + result.toString() + "</td>" + "</tr>";

        				parsedResponse.setOnlyResponse(row);
                    }

                    LinkInPage lip = new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD);
                    parsedResponse.setPageLink(lip);
                    parsedResponse.setAttribute("viParseID", viParseID);
                    
                    newTable.append(parsedResponse.getResponse());
                    rows.add(parsedResponse);
				}
        		
        		newTable.append("</table>");
        		
        		String header = CreateSaveToTSDFormHeader(
        				URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST")
        				+ "<TABLE cellspacing=\"1\" cellpadding=\"1\" border=\"1\" width=\"100%\">"
        				+ "<TR bgcolor=\"#6699CC\" valign=\"top\">"
        				+ "<TH ALIGN=Left>"
        				+ SELECT_ALL_CHECKBOXES
        				+ "</TH>"
        				+ "<TH ALIGN=Left><FONT SIZE=-1><B>Document</B></FONT></TH>"
        				+ "</TR>";

        		int nrUnsavedDoc = rows.size();

        		String footer = "\n</table><br>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, ID_SEARCH_BY_NAME, nrUnsavedDoc);

        		Response.getParsedResponse().setHeader(header);
        		Response.getParsedResponse().setFooter(footer);
        		
        		Response.getParsedResponse().setResultRows(rows);
        		Response.getParsedResponse().setOnlyResponse(newTable.toString());
        	}
            break;

        case ID_GET_LINK:
        case ID_SAVE_TO_TSD:
	        {// on save
	        	ParsedResponse parsedResponse = (ParsedResponse) Response.getParsedResponse();
	        	
	        	Patriots paDoc = (Patriots) parsedResponse.getAttribute(PA_RECORD);
	        	DocumentI document = parsedResponse.getDocument();
	        	
	        	if (paDoc == null && document != null){
	        		msSaveToTSDFileName = document.getInstno() + ".html";
		            
	        		parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	        	} else {
	        		parsedResponse.setDocument(paDoc);
		        	String keyNumber = paDoc.getInstno();
		        	
		        	String result = parsedResponse.getResponse();
		        	result = result.replaceAll("(?is)<input[^>]*>", "");
		        	result = result.replaceAll("(?is)</?form[^>]*>", "");
		        	parsedResponse.setResponse(result);
		        	
		        	msSaveToTSDFileName = keyNumber + ".html";
		        	parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
		        	msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
		                
		        	// add all image links
		        	for(ImageLinkInPage ilip: getImages(rsResponse, keyNumber)){
		        		parsedResponse.addImageLink(ilip);
		        	}
	        	}
	        }
            break;
        }

    }

    protected String getFileNameFromLink(String url) {
        return org.apache.commons.lang.StringUtils.substringBetween(url, "dummy=", "&");
    }
    
    public static int getNumberOfResultsFound(String input){
    	if (StringUtils.isEmpty(input)){
    		return 0;
    	}
    	Matcher matcher = RESULT_TD.matcher(input);
    	if (matcher.find()){
    		if (matcher.find()){
    			try {
					return Integer.parseInt(matcher.group(1));
				} catch (Exception e) {
					logger.error("Error while parsing number of results found", e);
					Log.sendExceptionViaEmail(e, "Full Input:\n" + input);
				}
    		}
    	}
    	return 0;
    }

    /**
     * Return list of image links
     * @param response
     * @return
     */
    private Collection<ImageLinkInPage> getImages(String response, String key){
    	
    	Collection<ImageLinkInPage> images = new LinkedList<ImageLinkInPage>();
    	Pattern pattern = Pattern.compile("(?i)<a\\s+(?:target=\"_blank\"\\s+)href=\"([^\"]+)\">([^<]+)</a>");
    	Matcher matcher = pattern.matcher(response);
    	while(matcher.find()){
    		String link = matcher.group(1);
    		String name = matcher.group(2);
    		if ("Link".equals(name)) {
    			int index = link.lastIndexOf("/");
        		if (index!=-1 && index<link.length()-1) {
        			name = link.substring(index+1);
        		}
    		}
    		if(!name.endsWith(".pdf") && !name.endsWith(".tiff")){
    			continue;
    		}
    		if(!link.startsWith("http://specialalerts.stewart.com")){
    			continue;
    		}
    		link = link.replaceFirst("http://specialalerts.stewart.com", "");
    		String atsLink = CreatePartialLink(TSConnectionURL.idGET) + link; 
    		String atsName = "patriots_" + name.replace(".pdf","") + "_" + key + ".pdf";
   			ImageLinkInPage ilip = new ImageLinkInPage(atsLink, atsName);
   			images.add(ilip);
    	}
    	
    	return images;
    }

	protected boolean verifyModule(TSServerInfoModule mod) {
		// check if last name is full
		if (StringUtils.isNotEmpty(mod.getParamValue(0))
				&& mod.getParamValue(0).length() >= 2)
			return true;
		
		return false;
	}
	
	@Override
	protected TSServer.ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent, boolean forceOverritten){
    	ParsedResponse pr = response.getParsedResponse();
    	DocumentsManagerI manager = mSearch.getDocManager();
    	DocumentI doc = pr.getDocument();
    	 try{
         	manager.getAccess();
	    	if (doc instanceof Patriots){
				Patriots patDoc = ((Patriots) doc);
				TSServer.calculateAndSetFreeForm(patDoc, PType.GRANTOR, searchId);
				String grantor = patDoc.getGrantorFreeForm();
				
				double score = 0;
	    		String id = "";
	    		if (!mSearch.getSa().isDateDown()) {
	    			for (DocumentI itE : manager.getRoLikeDocumentList()){
	    				if ("PATRIOTS".equals(itE.getDocType())){
	    					grantor = grantor.replaceAll(",\\s+", ",");
	    					if (NameUtils.isCompany(grantor)){
	    						score = GenericNameFilter.computeScoreForStrings(grantor, itE.getGrantorFreeForm());
	    					} else {
	    						score = GenericNameFilter.computeScoreForStrings(grantor, itE.getGrantorFreeForm(), true);
	    					}
	    					if (score >= 0.95){
	    						id = itE.getId();
	    						manager.remove(id);	
	    						break;   
	    					}
	    				}	
	    			}
	    		}
	    		if (mSearch.isAlertForPatriotsChapter(patDoc.getInstno())){
	        		patDoc.setHit(true);
	        	}
			}
    	 } catch(Exception e){  
         	e.printStackTrace(); 
         } finally{
         	manager.releaseAccess();
         }
    	
    	return super.addDocumentInATS(response, htmlContent,forceOverritten);
    }
	
	
	public void searchAlerts(Map<String, String> params) {
		SearchAlerts searchAlerts = new SearchAlerts();
		searchAlerts.setFirstName(params.get("firstName"));
		searchAlerts.setLastName(params.get("lastName"));
		searchAlerts.setAlternateIdentity("");
		
		SearchAlertsE searchAlertsE = new SearchAlertsE();
		searchAlertsE.setSearchAlerts(searchAlerts);

		
		SearchAlertsResponse searchAlertsResponse = searchStub.searchAlerts(params);
		if (searchAlertsResponse != null){
			searchAlertsResponse.getMatches();
		}
	}

	public void searchSDN(Map<String, String> params) {
		SearchSdns searchSdns = new SearchSdns();
		searchSdns.setFirstName(params.get("firstName"));
		searchSdns.setLastName(params.get("lastName"));
		searchSdns.setAlternateIdentity("");
				
		SearchSdnsE searchSdnsE = new SearchSdnsE();
		searchSdnsE.setSearchSdns(searchSdns);
				
		SearchSdnsResponse searchSdnsResponse = searchStub.searchSDN(params);
		if (searchSdnsResponse != null){
			searchSdnsResponse.getMatches();
		}
	}
	
	public void searchAlertsRefined(Map<String, String> params) {
		SearchAlertsRefined searchAlertsRefined = new SearchAlertsRefined();
		searchAlertsRefined.setFirstName(params.get("firstName"));
		searchAlertsRefined.setLastName(params.get("lastName"));
		searchAlertsRefined.setAlternateIdentity("");
		searchAlertsRefined.setMatchAll(false);
		searchAlertsRefined.setMatchWords(false);
		
		SearchAlertsRefinedE searchAlertsRefinedE = new SearchAlertsRefinedE();
		searchAlertsRefinedE.setSearchAlertsRefined(searchAlertsRefined);

		SearchAlertsRefinedResponse searchAlertsRefinedResponse = searchStub.searchAlertsRefined(params);
		if (searchAlertsRefinedResponse != null){
			searchAlertsRefinedResponse.getMatches();
		}
	}
	
	public void searchSdnsRefined(Map<String, String> params) {
		SearchSdnsRefined searchSdnsRefined = new SearchSdnsRefined();
		searchSdnsRefined.setFirstName(params.get("firstName"));
		searchSdnsRefined.setLastName(params.get("lastName"));
		searchSdnsRefined.setAlternateIdentity("");
		searchSdnsRefined.setMatchAll(false);
		searchSdnsRefined.setMatchWords(false);
		
		SearchSdnsRefinedE searchSdnsRefinedE = new SearchSdnsRefinedE();
		searchSdnsRefinedE.setSearchSdnsRefined(searchSdnsRefined);

		SearchSdnsRefinedResponse searchSdnsRefinedResponse = searchStub.searchSdnsRefined(params);
		if (searchSdnsRefinedResponse != null){
			searchSdnsRefinedResponse.getMatches();
		}
	}
	
	public static void main(String[] args) {
		try {
			
			SearchStub stub = new SearchStub("http://specialalerts.stewart.com/services/Search");
			stub._getServiceClient().getOptions().setTimeOutInMilliSeconds(654654654);
			stub._getServiceClient().getOptions().setSoapVersionURI(org.apache.axiom.soap.SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
				
			try {

				com.stewart.specialalerts.SearchStub.Search sar = new com.stewart.specialalerts.SearchStub.Search();
				sar.setFirstName("RICHARD RUSSELL");
				sar.setLastName("");
				sar.setAlternateIdentity("");
				
				SearchE sss = new SearchE();
				sss.setSearch(sar);
					
//				SearchResponse sasd = stub.search(sss).getSearchResponse();

					
					
//				SearchAlertsRefined sar = new SearchAlertsRefined();
//				sar.setFirstName("ABRIL CORTEZ");
//				sar.setLastName("");
//				sar.setAlternateIdentity("");
//				sar.setMatchAll(false);
//				sar.setMatchWords(false);
//					
//				SearchAlertsRefinedE sss = new SearchAlertsRefinedE();
//				sss.setSearchAlertsRefined(sar);
//					
//				SearchAlertsRefinedResponse sasd = stub.searchAlertsRefined(sss).getSearchAlertsRefinedResponse();
					
					
					
						
//				SearchSdnsRefined ssr = new SearchSdnsRefined();
//				ssr.setFirstName("");
//				ssr.setLastName("");
//				ssr.setAlternateIdentity("ABRIL CORTEZ");
//				ssr.setMatchAll(false);
//				ssr.setMatchWords(false);
//								
//				SearchSdnsRefinedE ssrre = new SearchSdnsRefinedE();
//				ssrre.setSearchSdnsRefined(ssr);
//					
//				SearchSdnsRefinedResponse ssrr = stub.searchSdnsRefined(ssrre).getSearchSdnsRefinedResponse();
					
					
					
//				SearchAlertsE ssd = new SearchAlertsE();
//				SearchAlerts sa = new SearchAlerts();
//				sa.setFirstName("RICHARD RUSSELL");
//				sa.setLastName("");
//				sa.setAlternateIdentity("");
//				ssd.setSearchAlerts(sa);
//
//				SearchAlertsResponseE ssdResponse = stub.searchAlerts(ssd);
					
					
					
										
//				SearchSdns ssd = new SearchSdns();
//				ssd.setFirstName("ABRIL CORTEZ");
//				ssd.setLastName("");
//				ssd.setAlternateIdentity("");
//					
//				SearchSdnsE searchSdns12 = new SearchSdnsE();
//				searchSdns12.setSearchSdns(ssd);
//					
//				SearchSdnsResponse ssdResponse = stub.searchSdns(searchSdns12).getSearchSdnsResponse();
				//SearchSdnsResponse response = ssdResponse.getSearchSdnsResponse();
			} catch (Exception e) {
				e.printStackTrace();
			}			
		} catch (AxisFault e) {
			e.printStackTrace();
		}
	}
}