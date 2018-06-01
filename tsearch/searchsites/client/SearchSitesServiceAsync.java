package ro.cst.tsearch.searchsites.client;

import java.util.Vector;

import ro.cst.tsearch.searchsites.client.Util.DataCompact;
import ro.cst.tsearch.searchsites.client.Util.SiteDataCompact;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SearchSitesServiceAsync {

	public  void getAllSiteTypesAbrev(AsyncCallback callback);
	
	public  void getAllCounties(AsyncCallback callback);
	
	public void getAllCounties(String stateAbrev,AsyncCallback callback);
	
	public void getAllStateAbrev(AsyncCallback callback);

	
	public void getAllImplementedSitesData(int commId,String stateId, String countyId, String serverType, AsyncCallback<SiteDataCompact> callback);
	
	public void getDataCompact(AsyncCallback<DataCompact> callback);
	
	public void  applyChanges(int commId, Vector<GWTDataSite> changedSites, Vector<GWTCommunitySite> changedCommunitySites, AsyncCallback<Void> callback)  ;
	
	public void  updateDataFromDB(AsyncCallback callback)  ;
	
	public void applyAdvancedChanges(AdvancedDataStruct struct, AsyncCallback callback) ;
	
	public void increaseDueOrPayDate(Vector<String> siteData, int fieldType, int amount, AsyncCallback<Integer> callback );
	
}
