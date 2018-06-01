package ro.cst.tsearch.connection.dasl;

import java.io.File;
import java.util.Map;

import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.StringUtils;

/**
 * Utility class for building a DASL DataTrace queries
 * @author radu bacrau
 */
public class GenericDTQueryBuilder {

	private static String RF = 
		BaseServlet.REAL_PATH + File.separator + "WEB-INF" + File.separator +  
		"classes" + File.separator + "resource" + File.separator + 
		"DASL" + File.separator;

	private static final String SEARCH_REQUEST_TEMPLATE = FileUtils.readXMLFile(RF + "DaslGenericDT-RequestBody.xml");

	private static final String defaultFromDate = "00/00/0000";
	private static final String defaultToDate   = "99/99/9999";

	/**
	 * Create date interval query part
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	private static String createDateQuery(String fromDate, String toDate){

		if(fromDate == null || !fromDate.matches("\\d\\d/\\d\\d/\\d\\d\\d\\d")){ 
			fromDate = defaultFromDate; 
		}
		if(toDate == null || !toDate.matches("\\d\\d/\\d\\d/\\d\\d\\d\\d")){ 
			toDate = defaultToDate; 
		}		
		return "<DateRange><FromValue><Date>" + fromDate + "</Date></FromValue><ToValue><Date>" + 
				toDate + "</Date></ToValue></DateRange>"; 
	}

	/**
	 * create a search by APN query
	 * @param params searc parameters
	 * @return search query or empty string if not enough parameters provided
	 */
	private static String createApnsQuery(Map<String,String> params){
		
		// check that we have an APN
		String apn = params.get("APN");
		if(StringUtils.isEmpty(apn)){
			return "";
		}
		
		// check IncludeTaxFlag
		String includeTaxFlag = params.get("IncludeTaxFlag");
		if(StringUtils.isEmpty(includeTaxFlag)){ includeTaxFlag = "YES"; }

		// check PropertyChainOption
		String propertyChainOption = params.get("PropertyChainOption"); 
		if(StringUtils.isEmpty(propertyChainOption)){ propertyChainOption = "NO"; }

		return "<PropertySearch><SearchType>APN</SearchType>" +				
				createDateQuery(params.get("FromDate"), params.get("ToDate")) +
			   "<APNs><APN>" + apn + "</APN></APNs>" +
			   "<IncludeTaxFlag>" + includeTaxFlag + "</IncludeTaxFlag>" +
			   "<PropertyChainOption>" + propertyChainOption + "</PropertyChainOption></PropertySearch>";
		
	}
	
	
	/**
	 * create a search by ID query
	 * @param params searc parameters
	 * @return search query or empty string if not enough parameters provided
	 */
	private static String createIDQuery(Map<String,String> params){
		
		// check that we have an APN
		String id = params.get("ID");
		if(StringUtils.isEmpty(id)){
			return "";
		}
		return ("<PropertySearch><OtherSearchCriteria/><PropertyIDS><ID>"+id+"</ID></PropertyIDS><LegalDescription/></PropertySearch>");
		
	}
	
	/**
	 * create a search by Address query
	 * @param params searc parameters
	 * @return search query or empty string if not enough parameters provided
	 */
	private static String createAddressQuery(Map<String,String> params){
		
		String suffix = params.get("StreetSuffix");
		String number = params.get("StreetNumber");
		String name = params.get("StreetName");
		
		
		if(StringUtils.isEmpty(name)){
			return "";
		}
		
		String start = "<PropertySearch>"+(StringUtils.isEmpty(number)?"<OtherSearchCriteria><StartIndex>1</StartIndex><MaxRecords>10</MaxRecords></OtherSearchCriteria>":"" )+
				"<PropertyAddress>"+
		(StringUtils.isEmpty(number)?"":	"<StreetNumber>"+number+"</StreetNumber>")+
		(StringUtils.isEmpty(name)?"":	"<StreetName>"+name+"</StreetName>") +(StringUtils.isEmpty(suffix)?"":	"<StreetSuffix>"+suffix+"</StreetSuffix>")+ 
		"</PropertyAddress><LegalDescription/></PropertySearch>";
		
		
		
		
		return start;
		
	}
	
	
	
	/**
	 * 
	 * @param params
	 * @return
	 */
	private static String createPartyQuery(Map<String,String> params){
		
		String query = "";
		
		// Nickname
		String nickName = params.get("Nickname");
		if(StringUtils.isEmpty(nickName)){
			nickName = "NICKNAME";
		}
		
		// WithProperty
		String withProperty = params.get("WithProperty");
		if(StringUtils.isEmpty(withProperty)){
			withProperty = "ALL_NAMES";
		}
		
		// SoundexValue
		String soundexValue = params.get("Soundex");
		if(StringUtils.isEmpty(soundexValue)){
			soundexValue = "70";
		}
		
		String parties = "";
		for(int i=1; i<=4; i++){
			
			String first  = params.get("First" + i);
			String middle = params.get("Middle" + i);
			String last   = params.get("Last" + i);
			String role   = params.get("Role" + i);
			
			if(!StringUtils.isEmpty(last)){
				if(role == null){ role = ""; }
				if(first == null){ first = "";}
				if(middle == null){ middle = ""; }
				parties += "<PartyInfo><PartyID/><PartyRole>" + role + "</PartyRole>" + "<Party><FullName/>" +
				           "<FirstName>" + first + "</FirstName>" +
				           "<MiddleName>" + middle + "</MiddleName>" + 
				           "<LastName>" + last + "</LastName><SSN_BusID/></Party><VestingType/></PartyInfo>";							
			}			
		}
		if(!StringUtils.isEmpty(parties)){
			query = "<PartySearch><SearchType>GENERAL_INDEX</SearchType>" + createDateQuery(params.get("FromDate"), params.get("ToDate"))  
			+ "<PartySearchType/><Parties>" + parties + "</Parties><Nickname>" + nickName + "</Nickname><WithProperty>" + withProperty + 
			"</WithProperty><SoundexValue><Value>" + soundexValue + "</Value></SoundexValue></PartySearch>";			
		}		
		
		return query;
	}
	
	/**
	 * Create search query
	 * @param params
	 * @return search query or empty string if not enough parameters provided
	 */
	public static String buildSearchQuery(Map<String,String> params,int moduleIdx){
		
		String query ="";
		if( moduleIdx == 1) { //address search
			query =  createAddressQuery(params);
		}
		else if( moduleIdx == 2){//pid search
			query = createIDQuery(params) ;
		}
		else{
			query = createApnsQuery(params) + createPartyQuery(params);
		}
		
		if(!StringUtils.isEmpty(query)){
			return SEARCH_REQUEST_TEMPLATE.replace("@@SEARCHES@@", query) ;
		} else {
			return "";
		}
	
		
	}
}
