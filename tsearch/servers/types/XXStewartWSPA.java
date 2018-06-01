package ro.cst.tsearch.servers.types;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.axis2.AxisFault;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.pdftiff.util.Util;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
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
import ro.cst.tsearch.utils.TiffConcatenator;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.Patriots;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.connection.stewart.PAWSConn;
import com.stewart.ats.connection.sureclose.SureCloseConn;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewart.specialalerts.SearchWSStub.AlertMatch;
import com.stewart.specialalerts.SearchWSStub.ArrayOfAlertMatch;
import com.stewart.specialalerts.SearchWSStub.ArrayOfMatchesEntryOfAlertMatch;
import com.stewart.specialalerts.SearchWSStub.ArrayOfMatchesEntryOfSdnMatch;
import com.stewart.specialalerts.SearchWSStub.ArrayOfSdnMatch;
import com.stewart.specialalerts.SearchWSStub.Entry_type0;
import com.stewart.specialalerts.SearchWSStub.Entry_type1;
import com.stewart.specialalerts.SearchWSStub.Matches;
import com.stewart.specialalerts.SearchWSStub.SdnMatch;

public class XXStewartWSPA extends TSServerPA {

    static final long serialVersionUID = 10000000;
    
    transient private PAWSConn searchStub;
    

    public XXStewartWSPA(long searchId) {
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

    public XXStewartWSPA(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
        resultType = MULTIPLE_RESULT_TYPE;
    }
    
    public PAWSConn getSearchStub() {
    	if(searchStub == null) {
   			try {
				searchStub = new PAWSConn(dataSite, searchId);
			} catch (AxisFault e) {
				logger.error("Cannot create searchStub on search " + searchId, e);
			}
    	}
		return searchStub;
	}

    @Override
    public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException{
    	Search currentSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
    	String value1 = module.getFunction(0).getParamValue();
    	String value2 = module.getFunction(1).getParamValue();
    	String value5 = module.getFunction(5).getParamValue();
    	String value6 = module.getFunction(6).getParamValue();
    	
        if (value1.length() == 0 && value2.length() == 0 && value5.length() == 0 && value6.length() == 0){
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
    	
    	if(StringUtils.isBlank(result)) {
    		return serverResponse;
    	}
    	
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
     		
    	patDoc.setSiteId(miServerID);
    	patDoc.setRecordedDate(Calendar.getInstance().getTime());
    	patDoc.setInstrumentDate(Calendar.getInstance().getTime());
    	
    	Collection<String> images = getImages(result);
    	if(images != null && !images.isEmpty()) {
    		getSearch().addImagesToDocument(patDoc, images.toArray(new String[images.size()]));
    	}
         	
    	parsedResponse.setDocument(patDoc);
    	parsedResponse.setResponse(result);
    	
        parsedRows.add(parsedResponse);
    	
        serverResponse.getParsedResponse().setResultRows(parsedRows);
        serverResponse.setResult("");
        
    	return serverResponse;
    }
    
    private String performSearch(Map<String, String> params,Map<String, String[]>  multiParams, int moduleIDX, String fakeResult){
    
    	switch (moduleIDX) {
    	case TSServerInfo.NAME_MODULE_IDX:

    		return searchRefined(params);

		default:
			break;
    	}
		return "";
	}

	public String search(Map<String, String> params) {
		
		StringBuffer result = new StringBuffer();
		
		String firstName = StringUtils.defaultString(params.get("firstName"));
		String lastName = StringUtils.defaultString(params.get("lastName"));
		
		String name = lastName;
		if (StringUtils.isNotBlank(firstName)){
			name += ", " + firstName;
		}
		if (StringUtils.isNotBlank(name)){
			mSearch.setPatriotSearchName(name.toUpperCase());
		}

		Matches matches = getSearchStub().search(params);
		
		if(matches != null){
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

			if (StringUtils.isNotBlank(firstName)){
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
		
		String firstName = StringUtils.defaultString(params.get("firstName"));
		String lastName = StringUtils.defaultString(params.get("lastName"));
		String address = StringUtils.defaultString(params.get("streetAddress"));
		String alternateIdentity = StringUtils.defaultString(params.get("alternateIdentity"));

		String name = lastName;
		if (StringUtils.isNotBlank(firstName)){
			name += ", " + firstName;
		}
		mSearch.setPatriotSearchName(name.toUpperCase());
		
		Matches matches = getSearchStub().searchRefined(params);
		
		if (matches != null){
			String currentDate = matches.getCurrentDate();
			String lastUpdated = matches.getLastUpdated();
			
			Results alertResults = getAlertMatchesAsString(matches, params);
			int alertsFound = alertResults.getNumberOfResults();
			String alertRes = alertResults.getResults();
			
			Results sdnResults = getSdnMatchesAsString(matches, params);
			int sdnFound = sdnResults.getNumberOfResults();
			String sdnRes = sdnResults.getResults();
			
			int resultsFound = alertsFound + sdnFound;
			StringBuilder searchcriteria = new StringBuilder();
			if (StringUtils.isNotBlank(name)){
				searchcriteria.append(name);
			}
			if (StringUtils.isNotBlank(alternateIdentity)){
				if (searchcriteria.length() > 0){
					searchcriteria.append("; ");
				}
				searchcriteria.append(alternateIdentity);
			}
			if (StringUtils.isNotBlank(address)){
				if (searchcriteria.length() > 0){
					searchcriteria.append("; ");
				}
				searchcriteria.append(address);
			}
			
			StringBuilder refineflags = new StringBuilder();
			String matchWords = StringUtils.defaultString(params.get("matchWords"));
			if ("true".equalsIgnoreCase(matchWords)){
				refineflags.append("Match whole words only");
			}
			String matchAll = StringUtils.defaultString(params.get("matchAll"));
			if ("true".equalsIgnoreCase(matchAll)){
				if (refineflags.length() > 0){
					refineflags.append(" and ");
				}
				refineflags.append("Match All keywords");
			}
			
			if (refineflags.length() == 0){
				refineflags.append("Match Any Keyword");
			}
			
			result.append("<table class=\"query-results\" border=\"0\" cellspacing=\"2\" cellpadding=\"2\" width=\"100%\">")
				.append("<tr><td>Names Searched</td><td>Results Found</td></tr>");
			result.append("<tr><td class=\"TdNameOdd\">").append(searchcriteria).append("</td>")
						.append("<td class=\"TdNameOdd\">").append(resultsFound).append("</td></tr>");
			result.append("<tr><td class=\"TdNameOdd\" colspan=\"2\">").append("Results returned for user search on ").append(refineflags).append("</td></tr>")				
						.append("</table><br><br>")
			
					.append(alertRes).append("<br><br>").append(sdnRes)
					.append("<br><br>").append("Current Date:&nbsp;").append(currentDate)
					.append("<br>").append("Last Updated:&nbsp;").append(lastUpdated);
		}
		return result.toString();
	}

	public Results getAlertMatchesAsString(Matches matches, Map<String, String> params) {
		
		String firstName = StringUtils.defaultString(params.get("firstName"));
		String lastName = StringUtils.defaultString(params.get("lastName"));
		String allName = (firstName.toLowerCase() + " " + lastName.toLowerCase()).trim();
		String allNameReverse = (lastName.toLowerCase() + " " + firstName.toLowerCase()).trim();
		
		String address = StringUtils.defaultString(params.get("streetAddress"));
		String alternateIdentity = StringUtils.defaultString(params.get("alternateIdentity"));
		boolean isOnlyNameSearch = false;
		if (StringUtils.isNotBlank(lastName) && StringUtils.isNotBlank(firstName) && StringUtils.isBlank(address) && StringUtils.isBlank(alternateIdentity)){
			isOnlyNameSearch = true;
		}
		Results results = new Results();
		
		String alertResults = "" ;
		int alertsFound = 0;
		
		ArrayOfMatchesEntryOfAlertMatch alertMatches = matches.getAlertMatches();
		
		if (alertMatches == null || alertMatches.getEntry() == null){
			alertResults = "<b>No Results Found for Closing/Fraud Alerts</b>";
		} else{
			Entry_type0[] entryAlert = alertMatches.getEntry();
							
			ArrayOfAlertMatch valueAlert = entryAlert[0].getValue();
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
					
					if (isOnlyNameSearch && !name.toLowerCase().contains(allName) && !name.toLowerCase().contains(allNameReverse)
						 && !description.toLowerCase().contains(allName) && !description.toLowerCase().contains(allNameReverse)){
						continue;
					} else{
						alertsFound++;
						String date = alertMatch.getDate();
						String entity = StringUtils.defaultString(alertMatch.getEntity());
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
						if (StringUtils.isNotBlank(file)){
							finalLink = "<a target=\"_blank\" href=\"" + href + "\">Link</a>";
						} else if (StringUtils.isNotBlank(link)){
							finalLink = "<a target=\"_blank\" href=\"" + link + "\">Link</a>";
						}
						
						String location = StringUtils.defaultString(alertMatch.getLocation());
						String realProperties = StringUtils.defaultString(alertMatch.getRealProperties());
						
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
							.append("<b>Closing/Fraud Alerts Results</b>").append("<br><br>")
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
		
		String firstName = StringUtils.defaultString(params.get("firstName"));
		String lastName = StringUtils.defaultString(params.get("lastName"));
		String allName = (firstName.toLowerCase() + " " + lastName.toLowerCase()).trim();
		String allNameReverse = (lastName.toLowerCase() + " " + firstName.toLowerCase()).trim();
		
		String address = StringUtils.defaultString(params.get("streetAddress"));
		String alternatIdentity = StringUtils.defaultString(params.get("alternateIdentity"));
		boolean isOnlyNameSearch = false;
		if (StringUtils.isNotBlank(lastName) && StringUtils.isNotBlank(firstName) && StringUtils.isBlank(address) && StringUtils.isBlank(alternatIdentity)){
			isOnlyNameSearch = true;
		}
		
		String sdnResults = "";
		int sdnFound = 0;
		Results results = new Results();
		
		ArrayOfMatchesEntryOfSdnMatch sdnMatches = matches.getSdnMatches();
				
		if (sdnMatches == null || sdnMatches.getEntry() == null){
			sdnResults = "<b>No Results Found for Specially Designated Nationals</b>";
		} else{
			
			Entry_type1[] entrySdn = sdnMatches.getEntry();
			
			ArrayOfSdnMatch valueSdn = entrySdn[0].getValue();
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
					
					if (isOnlyNameSearch && !name.replaceAll("\\s*,\\s*", " ").toLowerCase().contains(allName) 
								&& !name.replaceAll("\\s*,\\s*", " ").toLowerCase().contains(allNameReverse)
							 && !titleOfIndividual.replaceAll("\\s*,\\s*", " ").toLowerCase().contains(allName) 
							 		&& !titleOfIndividual.replaceAll("\\s*,\\s*", " ").toLowerCase().contains(allNameReverse)
							 && !alternateIdentity.replaceAll("\\s*,\\s*", " ").toLowerCase().contains(allName) 
							 		&& !alternateIdentity.replaceAll("\\s*,\\s*", " ").toLowerCase().contains(allNameReverse)){
							continue;
						} else
					{
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
							.append("<b>Specially Designated Nationals Results</b>").append("<br><br>")
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
		   					if(StringUtils.isNotBlank(firstname)){
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
	        	if (StringUtils.isNotBlank(agentLastName) && !MostCommonName.isMCLastName(agentLastName)){
	        		agentLastName = agentLastName.toUpperCase();
	        		
	        		Map<String,String> name = new LinkedHashMap<String,String>();
	        		name.put("last", agentLastName);
	        		
	        		if (StringUtils.isNotBlank(agentFirstName) && agentFirstName.length() > 1){
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
        	if (StringUtils.isNotBlank(lender)){
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
        		Response.getParsedResponse().setResultRows(new Vector<ParsedResponse>());	
 				return;
        	}
            
        	
        	if (parsedResponses.size() == 1){
        		ParsedResponse parsedResponse = parsedResponses.get(0);
        		
	        	Patriots doc = (Patriots) parsedResponse.getDocument();
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
                
                mSearch.addInMemoryDoc(sSave2TSDLink, parsedResponse);
                mSearch.addInMemoryDoc(sSave2TSDLink.replaceAll("\\+", " "), parsedResponse);
                result = addSaveToTsdButton(result, sSave2TSDLink, viParseID);
                
                parsedResponse.setOnlyResponse(result);

                LinkInPage lip = new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD);
                parsedResponse.setPageLink(lip);
                parsedResponse.setAttribute("viParseID", viParseID);
                
        	} else{
        		Vector<ParsedResponse> rows = new Vector<ParsedResponse>();
        		
        		StringBuilder newTable = new StringBuilder();
    			newTable.append("<table BORDER='1' CELLPADDING='2'>");
    			
        		for (ParsedResponse parsedResponse : parsedResponses) {
            		
    	        	Patriots doc = (Patriots) parsedResponse.getDocument();
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
                    
                    mSearch.addInMemoryDoc(sSave2TSDLink, parsedResponse);
                    mSearch.addInMemoryDoc(sSave2TSDLink.replaceAll("\\+", " "), parsedResponse);
                    
                    String checkBox = "<input type='checkbox' name='docLink' value='" + sSave2TSDLink + "'>";
    				String row = "<tr><td>" + checkBox + "</td><td>" + result.toString() + "</td>" + "</tr>";

    				parsedResponse.setOnlyResponse(row);

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
	        	
	        	DocumentI document = parsedResponse.getDocument();
	        	
	        	String keyNumber = document.getInstno();
	        	
	        	String result = parsedResponse.getResponse();
	        	result = result.replaceAll("(?is)<input[^>]*>", "");
	        	result = result.replaceAll("(?is)</?form[^>]*>", "");
	        	parsedResponse.setResponse(result);
	        	
	        	msSaveToTSDFileName = keyNumber + ".html";
	        	parsedResponse.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	        	msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
	        }
            break;
        }
    }

    protected String getFileNameFromLink(String url) {
        return StringUtils.substringBetween(url, "dummy=", "&");
    }
    
    public static int getNumberOfResultsFound(String input){
    	if (StringUtils.isBlank(input)){
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
    
    @Override
    protected DownloadImageResult saveImage(ImageI image) throws ServerResponseException {
    	if (image != null) {

			if (image.getLinks().size() == 0) {
				return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
			}
			String tempFilesPath = getSearch().getSearchDir() + "temp" + File.separator;
			try {
				byte[] imageBytes = null;
				List<byte[]> tiffs = new ArrayList<byte[]>();
				for(String link : image.getLinks()){
					URL u = new URL(link);
					HttpURLConnection huc =  ( HttpURLConnection )  u.openConnection (); 
					huc.setRequestMethod ("GET");  //OR  huc.setRequestMethod ("HEAD"); 
					
					for (int i = 0; i < 3; i++) {
						try {
							huc.connect () ;
							i = 4;
						} catch (Exception e) {
							logger.error("Cannot get image for link " + link + " on try " + i, e);
							TimeUnit.SECONDS.sleep(1);
						} 	
					}
					
					int code = huc.getResponseCode();
					if (code == HttpURLConnection.HTTP_OK) {
						if ("image/tiff".equals(huc.getContentType())){
							byte[] imageByte = IOUtils.toByteArray(huc.getInputStream());
							if (imageByte.length > 0){
								tiffs.add(imageByte);
							}
						} else if ("application/pdf".equals(huc.getContentType())){
							
							String fileName = tempFilesPath + link.substring(link.lastIndexOf("/") + 1);
							File fOutputFile = new File(fileName);
							byte[] imageByte = IOUtils.toByteArray(huc.getInputStream());
							if (imageByte.length > 0){
								org.apache.commons.io.FileUtils.writeByteArrayToFile(fOutputFile, imageByte);
								fileName = Util.convertPDFToTIFF(fileName, "", ro.cst.tsearch.utils.FileUtils.changeExtension(fileName, "tiff"));
								byte[] imageTiffByte = org.apache.commons.io.FileUtils.readFileToByteArray(new File(fileName));
								if (imageTiffByte.length > 0){
									tiffs.add(imageTiffByte);
								}
							}
						}
					}
				}
				
				if (tiffs.size() == 0) {
					return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
				}
				
				try {
					org.apache.commons.io.FileUtils.cleanDirectory(new File(tempFilesPath));
				} catch (Exception e) {
					logger.error("Can't clean directory : " + tempFilesPath + " when image is downloaded on PA");
				}
				imageBytes = TiffConcatenator.concateTiff(tiffs);
				
				if (imageBytes != null && imageBytes.length > 0) {
					imageBytes = SureCloseConn.convertToPDF(imageBytes);
					DownloadImageResult downloadImageResult = new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType());
					afterDownloadImage(true);
					return downloadImageResult;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
    }

    private Collection<String> getImages(String response) {
    	Collection<String> imageLinks = new ArrayList<String>();
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
    		
    		imageLinks.add(link);
    	}
    	return imageLinks;
    }
    
	protected boolean verifyModule(TSServerInfoModule mod) {
		// check if last name is full
		if (StringUtils.isNotEmpty(mod.getParamValue(0))
				&& mod.getParamValue(0).length() >= 2)
			return true;
		
		return false;
	}
	
	@Override
	protected ServerResponse performRequest(String page, int methodType, String action, int parserId, String imagePath, String vbRequest,
			Map<String, Object> extraParams) throws ServerResponseException {
		if(vbRequest.contains("&dummy=PA")) {
			ServerResponse response = new ServerResponse();
			response.setError(ServerResponse.NOT_VALID_DOC_ERROR);
			return response;
		} else {
			return super.performRequest(page, methodType, action, parserId, imagePath,
					vbRequest, extraParams);
		}
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
	
}