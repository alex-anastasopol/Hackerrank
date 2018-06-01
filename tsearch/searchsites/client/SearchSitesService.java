package ro.cst.tsearch.searchsites.client;

import java.util.Vector;

import ro.cst.tsearch.searchsites.client.Util.DataCompact;
import ro.cst.tsearch.searchsites.client.Util.SiteDataCompact;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * @author Cristian Stochina
 */
public interface SearchSitesService extends RemoteService{

	public String[] getAllSiteTypesAbrev();
	
	public String[] getAllCounties();
	
	public String[] getAllCounties(String stateAbrev);
	
	public String[] getAllStateAbrev();
	
	public SiteDataCompact getAllImplementedSitesData(int commId,String stateAbv, String county, String serverType);
	
	public DataCompact getDataCompact();
	
	public void applyChanges(int commId, Vector<GWTDataSite> changedSites, Vector<GWTCommunitySite> changedCommunitySites) throws SearchSitesException ;
	
	public void  updateDataFromDB() ;
		
	public void applyAdvancedChanges(AdvancedDataStruct struct ) throws SearchSitesException;
	
	public Integer increaseDueOrPayDate(Vector<String> siteData, int fieldType, int amount);
	
}
