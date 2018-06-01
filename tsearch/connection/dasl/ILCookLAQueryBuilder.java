package ro.cst.tsearch.connection.dasl;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.types.ILCookAO;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import static ro.cst.tsearch.datatrace.Utils.*;

/**
 * Utility class for building a ILCookLA query
 * @author radu bacrau
 */
public class ILCookLAQueryBuilder {

	public static final String PIN_COUNT = "PIN_COUNT";
	public static final String IND_COUNT = "IND_COUNT";
	public static final String BUS_COUNT = "BUS_COUNT";
	
	private static String RF = 
		BaseServlet.REAL_PATH + File.separator + "WEB-INF" + File.separator +  
		"classes" + File.separator + "resource" + File.separator + 
		"DASL" + File.separator + "ILCook" + File.separator;	

	private static String SEARCH_REQUEST_TEMPLATE = FileUtils.readXMLFile(RF + "search_request_body.xml");
	private static String IMAGE_REQUEST_TEMPLATE  = FileUtils.readXMLFile(RF + "image_request_body.xml");

	/**
	 * 
	 * @param docTypes
	 * @return
	 */
	private static String createDocTypesQuery(String docTypes){
		
		String docTypesQuery = "";
		for(String docType: StringUtils.splitCommaList(docTypes)){
			docTypesQuery += "<InstrumentType>" + docType + "</InstrumentType>";
		}
		
		if(!"".equals(docTypesQuery)){
			docTypesQuery = "<DocumentImageSearch><DocumentInfo>" + docTypesQuery + "</DocumentInfo></DocumentImageSearch>";
		} 		
		   	   
		return docTypesQuery;
	}

	/**
	 * 
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	private static String createDateQuery(String fromDate, String toDate){

		if(fromDate == null || toDate == null){ return ""; }
		if(!fromDate.matches("\\d\\d/\\d\\d/\\d\\d\\d\\d")){ return ""; }
		if(!toDate.matches("\\d\\d/\\d\\d/\\d\\d\\d\\d")){ return ""; }
		
		return "<DateRange><FromValue><Date>" + fromDate + "</Date></FromValue>" +
        		"<ToValue><Date>" + toDate + "</Date></ToValue></DateRange>"; 
	}

	/**
	 * 
	 * @param key
	 * @param params
	 * @param defaultValue
	 * @return
	 */
	private static int getIntParam(String key, Map<String,String>params, int defaultValue){
		String strVal = params.get(key);
		if(strVal != null && strVal.matches("\\d+")){
			return Integer.parseInt(strVal);
		} else {
			return defaultValue;
		}
	}
	
	/**
	 * 
	 * @param params
	 * @param dateQuery
	 * @return
	 */
	private static String createPropertySearches(Map<String, String> params, String dateQuery){

		String queries = "";

		// APN
		String apns = "";
		int maxApns = getIntParam(PIN_COUNT, params, 4);
		for(int i=1; i<=maxApns; i++){
			String apn = params.get("APN" + i);
			if(apn != null){
				String [] parts = ILCookAO.extractPins(apn);
				if(parts != null){
					apns += "<APN>";
					for(String s: parts){ apns += s; }
					apns += "</APN>";
				}
			}
		}
		if(!"".equals(apns)){
			apns = "<APNs>" + apns + "</APNs>";   		
		}		
		queries += apns;

		// Legal - S
		String legals = "";
		for(int i=1; i<=4; i++){
			String  lot = params.get("Lot" + i);
			String  block = params.get("Block" + i);
			String plat = params.get("Plat" + i);
			if(!StringUtils.isEmpty(plat) || !StringUtils.isEmpty(lot) || !StringUtils.isEmpty(block)){
				String legal = "<LotBlock>";
				if(!StringUtils.isEmpty(lot)){ 
					legal += "<Lot>" + lot + "</Lot>";
				}
				if(!StringUtils.isEmpty(block)){ 
					legal += "<Block>" + block + "</Block>";
				}
				String [] units = StringUtils.splitCommaList(params.get("Units" + i));
				if(units.length > 0){
					String unit = "";
					for(String u: units){
						unit += "<Value>" + u + "</Value>";
					}
					unit = "<Units>" + unit + "</Units>";
					legal += unit;
				}
				legal+="</LotBlock>";
				if(!StringUtils.isEmpty(plat)){
					legal += "<Plat><Plat_DocumentNumber>" + plat + "</Plat_DocumentNumber></Plat>";
				}
				legal = "<LegalDescription>" + legal + "<LegalDescriptionCode>S</LegalDescriptionCode></LegalDescription>";
				legals += legal;
			}
		}
		queries += legals;
		
		// Legal - A
		legals = "";
		for(int i=1; i<=4; i++){
			String sections[] = StringUtils.splitCommaList(params.get("Sections" + i));
			String township = params.get("Township" + i);
			String range = params.get("Range" + i);    		
			String quarterString = params.get("Quarters" + i);
			if(sections.length != 0 || !StringUtils.isEmpty(township) || !StringUtils.isEmpty(range)){
				String legal = "";
				// sections
				if(sections.length != 0){
					legal += "<Sections>";
					for(String section: sections){
						legal += "<Value>" + section + "</Value>";
					}
					legal += "</Sections>";
				}
				// township
				if(!StringUtils.isEmpty(township)){
					legal += "<Township>" + township + "</Township>";
				}
				// range
				if(!StringUtils.isEmpty(range)){
					legal += "<Range>" + range + "</Range>";
				}
				// quarters
				String [] quarters = StringUtils.splitCommaList(quarterString);
				if(quarters.length > 0){
					legal += "<Quarters>";
					for(String quarter: quarters){
						legal += "<Quarter><QuaterValue>" + quarter + "</QuaterValue></Quarter>";
					}
					legal += "</Quarters>";    			
				}
				if(!"".equals(legal)){
					legal = legal + "<LegalDescriptionCode>A</LegalDescriptionCode>";
					legal = "<LegalDescription>" + legal + "</LegalDescription>";
					legals += legal;
				}
			}
		}    	
		queries += legals;
		
		// 
		if(!StringUtils.isEmpty(queries)){
			queries = "<PropertySearch><SearchType>I</SearchType>" + dateQuery + queries + "</PropertySearch>";   
		}
		
		return queries;
	}

	/**
	 * 
	 * @param params
	 * @param dateQuery
	 * @return
	 */
	private static String createPartySearches(Map<String, String> params, String dateQuery){

		String query = "";

		// individuals
		Set<String> individualParties = new LinkedHashSet<String>();		
		int maxIndiv = getIntParam(IND_COUNT, params, 4);
		for(int i=1; i<=maxIndiv; i++){
			String last = params.get("LastName" + i);
			String first = params.get("FirstName" + i);
			String middle = params.get("MiddleName" + i);
			if(!StringUtils.isEmpty(last)){
				String party = "<PartyInfo><Party>";
				if(!StringUtils.isEmpty(first)){
					party += "<FirstName>" + first + "</FirstName>";
				}
				if(!StringUtils.isEmpty(middle)){
					party += "<MiddleName>" + middle + "</MiddleName>";
				}
				if(!StringUtils.isEmpty(last)){
					party += "<LastName>" + last + "</LastName></Party></PartyInfo>";
				}
				individualParties.add(party);
			}
		}
		if(individualParties.size() != 0){
			query += "<PartySearch><SearchType>I</SearchType>" + dateQuery + "<Parties>";
			for(String party: individualParties){
				query += party;
			}
			query += "</Parties></PartySearch>";
		}

		// businesses
		Set<String> businessParties = new LinkedHashSet<String>();
		int maxBus = getIntParam(BUS_COUNT, params, 4);
		for(int i=1; i<=maxBus; i++){
			String full = params.get("FullName" + i);

			if(!StringUtils.isEmpty(full)){
				String party = "<PartyInfo><Party><FullName>" + full + "</FullName></Party></PartyInfo>";
				businessParties.add(party);
			}
		}
		if(businessParties.size() != 0){
			query += "<PartySearch><SearchType>B</SearchType>" + dateQuery + "<Parties>";
			for(String party: businessParties){
				query += party;
			}			
			query += "</Parties></PartySearch>";
		}

		// trusts
		Set<String> trustsParties = new LinkedHashSet<String>();
		for(int i=1; i<=4; i++){
			
			String no = params.get("TPartyID" + i);
			String date = params.get("TDate" + i);
			String full = params.get("TFullName" + i);

			if(date != null && !date.matches("\\d\\d?/\\d\\d?/\\d\\d\\d\\d")){
				date = null;
			}
			
			if(!StringUtils.isEmpty(no) /*|| !StringUtils.isEmpty(full) */){
				String party = "";
				if(!StringUtils.isEmpty(no)){
					party += "<PartyID>" + no + "</PartyID>";
				}
				if(!StringUtils.isEmpty(date)) { 
					party += "<Date>" + date + "</Date>"; 
				}
				if(!StringUtils.isEmpty(full)){
					party += "<Party><FullName>" + full + "</FullName></Party>";
				}
				party = "<PartyInfo>" + party + "</PartyInfo>";
				trustsParties.add(party);
			}
		}
		if(trustsParties.size() != 0){
			query += "<PartySearch><SearchType>T</SearchType>" + dateQuery + "<Parties>";
			for(String party: trustsParties){
				query += party;
			}
			query += "</Parties></PartySearch>";
		}    	    	

		return query;
	}

	/**
	 * Create a DASL/TP3 query by using the parameters from search module
	 * @param params
	 * @return
	 */
	public static String buildSearchQuery(Map<String,String> params, long searchId){

		// searches
		String propertyDateQuery = createDateQuery(params.get("FromDateProperty"), params.get("ToDateProperty"));
		String partyDateQuery = createDateQuery(params.get("FromDateParty"), params.get("ToDateParty"));
		String searches = createPropertySearches(params, propertyDateQuery) + createPartySearches(params, partyDateQuery);
		if(StringUtils.isEmpty(searches)){
			return "";
		}    	
		String query = SEARCH_REQUEST_TEMPLATE;
		query = query.replace("@@SEARCHES@@", searches);

		// doctypes
		query = query.replace("@@DOCTYPE_FILTER@@", createDocTypesQuery(params.get("DocTypes")));
		
		// fill the order id
		String orderId = getOrderId(searchId);
		query = fillXMLParameter(query, "ClientTransactionReference", orderId);
		query = fillXMLParameter(query, "ClientReference", orderId);
		
		return query;
	}
	
	/**
	 * Create order id for the given search id
	 * @param searchId
	 * @return
	 */
	private static String getOrderId(long searchId){

		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
    	String orderId = search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
    	orderId = orderId.replaceFirst("[^~]*~", "").replaceAll("\\s+", "");
    	if(orderId.length() < 4){
    		orderId += searchId;
    	}  		
    	return orderId;
	}
		
	/**
	 * Create a DASL/TP3 query by using the parameters from search module
	 * @param params
	 * @return
	 */
	public static String buildImageQuery(Map<String,String> params, long searchId){
		
		// check parameters
		if(params.size() == 0){
			throw new IllegalArgumentException("Empty parameters map!");
		}
		
		// build query
		String query ="";
		
		for(Map.Entry<String,String> entry : params.entrySet()){
			String paramName = entry.getKey();
			String paramValue = entry.getValue();
			query += "<" + paramName + ">" + paramValue + "</" + paramName + ">";
		}
		query += "<PageNumber>0</PageNumber>";
		
		// add query to the request body
		query = fillXMLParameter(IMAGE_REQUEST_TEMPLATE, "IMAGE_SEARCH_CRITERIA", query);
		
		// fill the order id
		String orderId = getOrderId(searchId);
		query = fillXMLParameter(query, "ClientTransactionReference", orderId);
		query = fillXMLParameter(query, "ClientReference", orderId);
		
		return query;
	}
	
}
