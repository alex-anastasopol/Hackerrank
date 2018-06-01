package ro.cst.tsearch.webservices;

import static ro.cst.tsearch.bean.SearchAttributes.P_COUNTY;
import static ro.cst.tsearch.bean.SearchAttributes.P_STATE;
import static ro.cst.tsearch.bean.SearchAttributes.SEARCH_PRODUCT;
import static ro.cst.tsearch.utils.StringUtils.isEmpty;
import static ro.cst.tsearch.utils.StringUtils.isNotEmpty;
import static ro.cst.tsearch.utils.XmlUtils.getChildren;
import static ro.cst.tsearch.utils.XmlUtils.getNodeValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.GenericCounty;
import ro.cst.tsearch.data.GenericState;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;

/**
 * Get Data Sources service
 * @author MihaiB
 */
public class GetDataSourcesService extends AbstractService {
	
	private List<Map<String,Object>> sites = new ArrayList<Map<String,Object>>();
	
	private static Map<Integer, String> TRANSLATION_TABLE = new HashMap<Integer, String>();
	
	static {
		TRANSLATION_TABLE.put(1,  "1");
		TRANSLATION_TABLE.put(2,  "5");
		TRANSLATION_TABLE.put(3,  "2");
		TRANSLATION_TABLE.put(4,  "10");
		TRANSLATION_TABLE.put(5,  "4");
		TRANSLATION_TABLE.put(6,  "3");
		TRANSLATION_TABLE.put(7,  "7");
		TRANSLATION_TABLE.put(8,  "8");
		TRANSLATION_TABLE.put(9,  "6");
		TRANSLATION_TABLE.put(10, "9");		
	}

	/**
	 * Constructor
	 * @param appId
	 * @param userName
	 * @param password
	 * @param order
	 */
	public GetDataSourcesService(String appId, String userName, String password, String order, String dateReceived){
		super(appId, userName, password, order, dateReceived);		
	}

		
 
	@Override
	public void process(){
		
		// check for errors from constructor
		if (errors.size() > 0){
			return;
		}
		
		// create search attributes
		SearchAttributes sa = new SearchAttributes(-1);
		parseOrder(orderDoc, sa);
		
		if (errors.size() > 0){
			return;
		}
		
		try{
			
			this.sites = getCommSites(sa);
	
		} catch(Exception e){
			e.printStackTrace();
			errors.add("internal error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
	
	private void parseOrder(Node node, SearchAttributes sa){
		
		// isolate the order
		Node order = null;
		for (Node child : getChildren(node)){
			if ("ats".equalsIgnoreCase(child.getNodeName())){
				for (Node grand: getChildren(child)){
					if ("order".equalsIgnoreCase(grand.getNodeName())){
						order = grand;
						break;
					} else {
						warnings.add("Node ignored: " + grand.getNodeName());
					}
				}
			} else{
				warnings.add("Node ignored: " + child.getNodeName());
			}
		}
		if (order == null){
			errors.add("Could not find <ats><order>");
			return;
		}
				
		
		String stateStr = null;
		String countyStr = null;
		
		for (Node child: getChildren(order)){
			String childName = child.getNodeName();
			if("county".equalsIgnoreCase(childName)){				
				countyStr = getNodeValue(child);
			} else if("state".equalsIgnoreCase(childName)){
				stateStr = getNodeValue(child);
			} else if("product_type".equalsIgnoreCase(childName)){
				parseProductType(getNodeValue(child), sa);
			} else{			
				warnings.add("Node ignored: " + childName);
			}
		}
		
		// check the state string
		if (isEmpty(stateStr)){
			errors.add("State info missing");
			return;
		}
				
		// check actual state and county
		if (isNotEmpty(stateStr)){
			GenericState state = DBManager.getStateForAbvStrict(stateStr);
			if (state == null){
				errors.add("State " + stateStr + " not found! State must be in format XX");
				return;				
			}
			long stateId = state.getId();
			sa.setAtribute(P_STATE, "" + stateId);
			if (isNotEmpty(countyStr)){
				if ("AK".equalsIgnoreCase(state.getStateAbv())){
					if(countyStr.toLowerCase().contains("anchorage")){
						countyStr = "Anchorage Borough";
					}
				}
				GenericCounty county = DBManager.getCountyForNameAndStateIdStrict(countyStr, stateId,true);
				if (county == null){
					errors.add("County " + countyStr + " not found on state " + stateStr);
					return;
				}
				long countyId = county.getId();
				sa.setAtribute(P_COUNTY, "" + countyId);
			}
		}
	}
	
	private void parseProductType(String value, SearchAttributes sa){

		int product = 0;		
		value = value.trim();
		if (value.length() == 0){
			errors.add("Empty product type");	
			return;
		}
		if (value.matches("\\d+")){
			product = Integer.parseInt(value); 
		} else{
			errors.add("Incorrect product type: " + value);
		}		
		if (TRANSLATION_TABLE.containsKey(product)){
			sa.setAtribute(SEARCH_PRODUCT, TRANSLATION_TABLE.get(product));						
		} else{
			errors.add("Incorrect product type: " + value);
		}
	}
	
	private List<Map<String,Object>> getCommSites(SearchAttributes sa){
		String countyId = sa.getAtribute(P_COUNTY);
		
		String sql = "SELECT site_type FROM " + DBConstants.TABLE_COMMUNITY_SITES + " WHERE county_Id =? " + " and enableStatus != 0 group by site_type";
		
		List<Map<String,Object>> siteTypesPerCounty = DBManager.getSimpleTemplate().queryForList(sql, countyId);
		
		StringBuffer siteTypeList = new StringBuffer();
		for (Map<String,Object> map : siteTypesPerCounty){
			siteTypeList.append(map.get("site_type")).append(", ");
		}
		String siteTypes = siteTypeList.toString().replaceFirst(",\\s*$", "");
		
		String lastSql = "SELECT s.id_county, m.site_type, m.site_abrev, m.description, s.is_enabled, s.city_name, s.effective_start_date "
					+ " FROM " + DBConstants.TABLE_MAP_SITE_TYPE_TO_P2 + " m JOIN " + DBConstants.TS_SITES + " s ON (m.site_type = s.site_type) " 
					+ " WHERE s.id_county = ? AND s.site_type IN (" + siteTypes + ") AND m.site_type IN (" + siteTypes + ") GROUP BY site_type ORDER BY site_type";
		
		List<Map<String,Object>> siteTypesPer = DBManager.getSimpleTemplate().queryForList(lastSql, countyId);
		
		return siteTypesPer;
	}
	
	public String getResult(){
		
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("<ats><response>");
		
		if (this.sites.size() == 0){
			sb.append("<query_status>no results found</query_status>");
			// add errors
	        if(errors.size() != 0){
	        	sb.append("<b>Errors</b>:<br/>");
	        	for(String error: errors){
	        		sb.append("<li>" + error + "</li>");
	        	}
	        	sb.append("<hr/>");
	        	
	        }
		} else{
			sb.append("<datasources>");
			for (Map<String,Object> map : this.sites){
				sb.append("<datasource>");
					sb.append("<siteAbrev>").append(map.get("site_abrev")).append("</siteAbrev>");
					String description = (String) map.get("description");
					String cityName = (String) map.get("city_name");
					if (isNotEmpty(cityName)){
						description = cityName + " " + description.replaceFirst("(?is)\\s+Y[A-Z]\\s*$", "");//City Tax YC
					}
					sb.append("<description>").append(description).append("</description>");
					String effect_date = "";
					try {
						Date effective_date = (Date) map.get("effective_start_date");
						if (effective_date != null) {
							effect_date = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(effective_date);
						}
					} catch (Exception e) {}
					
					sb.append("<effectiveStartDate>").append(effect_date).append("</effectiveStartDate>");
					int certified = (Integer) map.get("is_enabled");
					sb.append("<certified>").append(certified == 1 ? "true" : "false").append("</certified>");
					
				sb.append("</datasource>");
			}
			sb.append("</datasources>");
		}

		sb.append("</response></ats>");
		
		return sb.toString();
	}
	
}
