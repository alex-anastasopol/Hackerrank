package ro.cst.tsearch.searchsites.client;


import java.util.Arrays;

import ro.cst.tsearch.searchsites.client.Util.SiteDataCompact;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class FilterChangeListener implements ChangeListener{
	
	CommunitySitesFilter filterPanel = null;
	
	public FilterChangeListener(CommunitySitesFilter filterPanel) {
		super();
		this.filterPanel = filterPanel;
	}

	public void onChange(Widget arg0) {
	
		if(arg0 instanceof ListBox){
			SearchSites.getWaitingPopUP().show();
			
			ListBox list = ((ListBox)arg0);
			
			String listName = list.getName(); 
			CommunitySitesFilter communitySitesFilter=null;
			//int moduleType = SearchSites.getModuleType();
			String selectedCommunity=null;
			//if (filterPanel instanceof CommunitySitesFilter ){
				communitySitesFilter = (CommunitySitesFilter) filterPanel;
				selectedCommunity = communitySitesFilter.getSelectedCommunityLabel();
			//}
			final String selectedCommunityValue = communitySitesFilter==null ? null:selectedCommunity;			
			final  String state = filterPanel.getSelectedState();
			String selectedCounty = filterPanel.getSelectedCounty();
			
			final String type = filterPanel.getSelectedType();
			
			if(SitesFilter.STATE_LIST_BOX_NAME.equals(listName)){
				selectedCounty = FilterUtils.NONE;
			}
			
			
			final  String county = selectedCounty;
			final int commId = "ALL".equalsIgnoreCase(selectedCommunity)?-1:Integer.parseInt(communitySitesFilter.getSelectedCommunity());
			
			if("NONE".equalsIgnoreCase(county) || "NONE".equalsIgnoreCase(state) || "NONE".equalsIgnoreCase(selectedCommunityValue)) {
				SearchSites.getWaitingPopUP().hide();
				return;
			}
				
			if(selectedCounty != FilterUtils.NONE) {
				SearchSites.searchSitesService.getAllImplementedSitesData(commId, state, county, type,  new AsyncCallback<SiteDataCompact>() {
					
					@Override
					public void onSuccess(SiteDataCompact result) {
						FlexTable sitesTable = filterPanel.getSda().getSitesTable();
						FlexTable settingsTable = filterPanel.getSda().getSettingsTable();
	
						SearchSites.setSiteData(result.getSitesData());
						
						GWTDataSite[] siteData = (SearchSites.getSiteData(state,county)).toArray(new GWTDataSite[1]);
						GWTDataSite[] siteDataNew = null;
						try {
							siteDataNew = FilterUtils.filter(siteData, selectedCommunityValue, state, county, type);
							Arrays.sort( siteDataNew, new GWTDataSite.CountyNameComparator() );
							SearchSites.fillSiteTableWithDataSite(sitesTable, siteDataNew);
							
							SearchSites.fillSettingsTableWithDataSite(settingsTable, siteDataNew, commId);
							
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						if( siteDataNew==null || siteDataNew.length  == 0){ //add a new site
							
							boolean addSite = !(FilterUtils.ALL.equals(county)||FilterUtils.NONE.equals(county)
									||FilterUtils.ALL.equals(state)||FilterUtils.NONE.equals(state)
									||FilterUtils.ALL.equals(type));
							if(addSite ){
								GWTDataSite cur = new GWTDataSite(selectedCommunityValue,state,county,type,true, 0);
								cur.setType(result.getSiteType());
								
								SearchSites.addRow(1, sitesTable, cur, true);
								SearchSites.addHeader(sitesTable, siteData, false);
							}
							filterPanel.getSda().getSitesTable().setHTML(0, 11,"<font color='blue'><b>Enable features</b></font>");
							try {
								filterPanel.getSda().getSettingsTable().clearCell(0, 4);
							} catch (Exception e) {
								Window.alert("Cannot draw enable features: "+ e.getMessage());
							}
						}else {
							sitesTable.setHTML(0, 11,"");
							VerticalPanel vp = new VerticalPanel();
							vp.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
							vp.add(new HTML("<font color='blue'><b>Enable features</b></font>"));
							vp.add(SearchSites.getEnableAllCheckboxesPanel(filterPanel.getSda().getSettingsTable()));
							filterPanel.getSda().getSettingsTable().setWidget(0, 4, vp);
						
						}
						
						SearchSites.getWaitingPopUP().hide();
						
					}
					
					@Override
					public void onFailure(Throwable caught) {
						Window.alert("Error when loading data: "+ caught.getMessage());
						
					}
				});
			}
			
		}
	}
	
}
