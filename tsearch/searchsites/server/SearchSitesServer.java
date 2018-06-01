package ro.cst.tsearch.searchsites.server;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.community.CommunityManager;
import ro.cst.tsearch.data.GenericCounty;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.searchsites.client.AdvancedDataStruct;
import ro.cst.tsearch.searchsites.client.FilterUtils;
import ro.cst.tsearch.searchsites.client.GWTCommunitySite;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.searchsites.client.SearchSitesBuckets;
import ro.cst.tsearch.searchsites.client.SearchSitesException;
import ro.cst.tsearch.searchsites.client.SearchSitesService;
import ro.cst.tsearch.searchsites.client.Util;
import ro.cst.tsearch.searchsites.client.Util.DataCompact;
import ro.cst.tsearch.searchsites.client.Util.SiteDataCompact;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.parentsite.CountyWithState;
import ro.cst.tsearch.servers.parentsite.State;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * @author cristi stochina
 */
public class SearchSitesServer extends RemoteServiceServlet  implements SearchSitesService{

	private static final long serialVersionUID = 5361963195650018524L;
	
	public String[] getAllCounties() {
		GenericCounty counties[] =  DBManager.getAllCountiesSortedWithoutDuplicate();
		String allCountyNames[] = new String[counties.length];
		for(int i=0;i<allCountyNames.length;i++){
			allCountyNames[i] = counties[i].getName().replaceAll("&nbsp;", " ");
		}
		return allCountyNames;
	}

	public String[] getAllCounties(String stateAbrev) {
		List<CountyWithState> list =  DBManager.getAllCountiesForStateAbrev(stateAbrev);
		
		SortedSet<CountyWithState> t =  new TreeSet<CountyWithState>();
        t.addAll(   list );
        CountyWithState[] all = (CountyWithState []) t.toArray(new CountyWithState[0]);
     
		String allCountyNames[] = new String[all.length];
	
		for(int i=0;i<all.length;i++){
			allCountyNames[i]  = all[i].getCountyName().replaceAll("&nbsp;", " ");
		}
		
		return allCountyNames;
	}

	public String[] getAllSiteTypesAbrev() {
		return Search.getReadOnlyServerTypesAbrev();		
	}

	public String[] getAllStateAbrev() {
		return State.getStateAbrevVectorreadOnly().toArray(new String[State.NR_OF_STATES_SUA]);
	}

	public Map<Integer, String> getAllCommunities() {
		Map<String, String> allCommunityBasicData = CommunityManager.getAllCommunityBasicData(false);
		Map<Integer, String> allCommunities = new HashMap<Integer, String>();
		for (String commId : allCommunityBasicData.keySet()) {
			allCommunities.put(Integer.parseInt(commId), allCommunityBasicData.get(commId));
		}
		return allCommunities;
	}
	
	public SiteDataCompact getAllImplementedSitesData(int commId,String stateAbv, String county, String serverType){
		return HashCountyToIndex.getAllDataSitesForSearchSites(commId, stateAbv, county, serverType);		
	}
	
	public DataCompact getDataCompact() {
		DataCompact compact = new Util.DataCompact();
		compact.setCommunities(getAllCommunities());
		compact.setStates(getAllStateAbrev());
		compact.setTypes(getAllSiteTypesAbrev());
		compact.setCounties(getAllCounties(FilterUtils.ALL));
		return compact;
	}

	public void applyChanges(int commId, Vector<GWTDataSite> changedSites, Vector<GWTCommunitySite> changedCommunitySites) throws SearchSitesException {
		try{
			HashCountyToIndex.addNewSitesOrUpdates(commId, changedSites, changedCommunitySites);
		}
		catch(Exception e){
			e.printStackTrace();
			throw new SearchSitesException(e,e.getMessage());
		}
	}

	
	
	public void updateDataFromDB() {
		Search.initSitesConfiguration();
	}

	public void applyAdvancedChanges(AdvancedDataStruct struct) throws SearchSitesException{
		
		String[] siteNameOrder = struct.siteNameOrder;
		String adressToken = struct.adressToken;
		String adressTokenMiss = struct.adressTokenMiss;
		String cityName = struct.cityName;
		String alternateLink = struct.alternateLink;
			
		int connType = struct.connType;
		int taxYearMode = struct.taxYearMode;
				
		Date dueDate = null;
		Date payDate = null;
		if(struct.dueDate!=null){
			dueDate = ro.cst.tsearch.generic.Util.dateParser3(struct.dueDate);
			if(dueDate ==null && !"*".equals(struct.dueDate)){
				throw new SearchSitesException("Due Date incorect format ! Use mm/dd/yyyy or \"*\"");
			}
			payDate = ro.cst.tsearch.generic.Util.dateParser3(struct.payDate);
			if(payDate ==null && !"*".equals(struct.payDate)){
				throw new SearchSitesException("Pay Date incorect format ! Use mm/dd/yyyy or \"*\"");
			}
		}
		
		if (struct.cityName!=null&&"".equals(struct.cityName)){
			throw new SearchSitesException("Fill in a city name !");
		}
		
		Vector<DataSite> vec = HashCountyToIndex.getAllDataSites(
				HashCountyToIndex.ANY_COMMUNITY, 
				struct.stateAbbreviation,
				struct.countyName, false, HashCountyToIndex.ANY_PRODUCT);
		
		
		int i=0;
		for(;i<vec.size();i++){
			DataSite dat = vec.get(i);
            String cursiteName = dat.getName();
			for(int j=0;j<siteNameOrder.length;j++){
				if(cursiteName.equalsIgnoreCase(siteNameOrder[j])){
					dat.setAutpos(SearchSitesBuckets.getSiteOrderIndexInBucket(dat.getSiteTypeInt(), j)); //add sites to buckets
				}
			}
            if(cursiteName.equals(struct.getName())){
            	dat.setAdressToken(adressToken);
            	dat.setAdressTokenMiss(adressTokenMiss);
            	dat.setCityName(StringUtils.defaultIfEmpty(cityName,""));
            	
            	if(struct.alternateLink!=null)
            		dat.setAlternateLink(alternateLink);

            	dat.setConnType(connType);
            	if(struct.dueDate!=null){
            		dat.setDueDate(dueDate);
            		dat.setPayDate(payDate);
	            	dat.setTaxYearMode(taxYearMode);
            	}
            	dat.setNumberOfYears(struct.numberOfYears);
            }
		}
		
		Vector <GWTDataSite> sitesData =  new Vector<GWTDataSite>();
		
		for(i=0;i<vec.size();i++){
			sitesData .add(vec.get(i).toSiteData());
		}
		
		try{
			HashCountyToIndex.addNewSitesOrUpdates( HashCountyToIndex.ANY_COMMUNITY, sitesData, null);
		}
		catch(Exception e){
			throw new SearchSitesException(e.getMessage());
		}
		
	}

	@Override
	public Integer increaseDueOrPayDate(Vector<String> siteData,
			int fieldType, int amount) {
		
		int updated = 0;
		
		for (String key : siteData) {
			try {
				updated += DBManager.updateDueOrPayDate(key, fieldType, amount);
			} catch (Throwable t) {
				DBManager.getLogger().error("Error while increasing DueOrPayDate", t);
			}
		}
		
		return updated;
	}
	

}
