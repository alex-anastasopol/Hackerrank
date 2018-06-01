package ro.cst.tsearch.searchsites.client;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import ro.cst.tsearch.searchsites.client.Util.DataCompact;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;

public class SitesDataArived implements AsyncCallback<DataCompact>{
	
	GWTDataSite[]	siteData		= null;
	private int		currentCommId;

	FlexTable		sitesTable		= new FlexTable();
	FlexTable		settingsTable	= new FlexTable();
	
	
	public SitesDataArived(int commId){
		this.currentCommId = commId;
	}
	
	public void onFailure(Throwable arg0) {
		Window.alert(arg0.getMessage());
	}

	public void onSuccess(DataCompact dat) {
		
		SearchSites.setAllCommunities(dat.getCommunities());
		SearchSites.setAllSitesAbrev(dat.getTypes());
		SearchSites.setAllStatesAbrev(dat.getStates());
		SearchSites.setAllCounties(dat.getCounties());

		
		ListBox communityFilterList = new ListBox();
		communityFilterList.addItem(FilterUtils.NONE);
		communityFilterList.addItem(FilterUtils.ALL);
		communityFilterList.setWidth("200px");
		
		Map<Integer, String> allCommunities = SearchSites.getAllCommunities();
		int currentCommunityIndex = 1;				//auto-select current community
		for (Entry<Integer, String> element : allCommunities.entrySet()) {
			communityFilterList.addItem(element.getValue(), element.getKey().toString());
			if (element.getKey() != currentCommId) {
				currentCommunityIndex++;
			} else {
				communityFilterList.setItemSelected(currentCommunityIndex + 1, true);
			}
		}
		
		ListBox stateFilterList = new ListBox();
		stateFilterList.addItem(FilterUtils.NONE);
		stateFilterList.addItem(FilterUtils.ALL);
		String[] str = SearchSites.getAllStatesAbrev();
		for(int i=0;i<str.length;i++){
			stateFilterList.addItem(str[i]);
		}
		
		ListBox countyList = new ListBox();
		countyList.setWidth("150px");
		countyList.addItem(FilterUtils.NONE);
		countyList.addItem(FilterUtils.ALL);
		
		ListBox typeFilterList = new ListBox();
		//typeFilterList.addItem(FilterUtils.NONE);
		typeFilterList.addItem(FilterUtils.ALL);
		str = SearchSites.getAllSitesAbrev();
		Arrays.sort(str);
		for(int i=0;i<str.length;i++){
			typeFilterList.addItem(str[i]);
		}
		//panel with buttons and select lists
		HorizontalPanel horPanel = new HorizontalPanel();
		
		Button updateFromDbButton = new Button("Reload");
		updateFromDbButton.setStyleName("button");
		horPanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
		updateFromDbButton .addClickHandler(new ClickHandler(){

			public void onClick(ClickEvent event) {
					SearchSites.getWaitingPopUP().show();
					SearchSites.searchSitesService.updateDataFromDB(new AsyncCallback<Void>(){

					public void onFailure(Throwable arg0) {
						if(SearchSites.getWaitingPopUP().isVisible()){
							SearchSites.getWaitingPopUP().hide();
						}
						Window.alert(arg0.getMessage());
					}
					public void onSuccess(Void arg0) {
						if(SearchSites.getWaitingPopUP().isVisible()){
							SearchSites.getWaitingPopUP().hide();
						}
						SearchSites.init();
					}
				});
			}
		});
		
		horPanel .add(updateFromDbButton );
		horPanel.add(new Label(" Select "));
		
		final CommunitySitesFilter  filterPanel = new CommunitySitesFilter(
				communityFilterList, 
				stateFilterList,
				countyList,	
				typeFilterList,	
				this);
		
		filterPanel.addChangeListener(new FilterChangeListener(filterPanel));
		
		horPanel .add(filterPanel);
		
		Button applyBut = new Button("Apply Changes");
		applyBut.addClickHandler(new ApplyChangesClickListener( currentCommId, sitesTable,filterPanel));
		applyBut.setStyleName("button");
		horPanel.setSpacing(5);
		horPanel.add(applyBut);
		
		Button increaseDueDateButton = new Button("Increase DueDate");
		increaseDueDateButton.addClickHandler(
				new IncreaseDueOrPayDate(sitesTable, IncreaseDueOrPayDate.FIELD_DUE_DATE_YEAR_INCREASE, 1));
		increaseDueDateButton.setStyleName("button");
		horPanel.add(increaseDueDateButton);
		
		Button increasePayDateButton = new Button("Increase PayDate");
		increasePayDateButton.addClickHandler(
				new IncreaseDueOrPayDate(sitesTable, IncreaseDueOrPayDate.FIELD_PAY_DATE_YEAR_INCREASE, 1));
		increasePayDateButton.setStyleName("button");
		horPanel.add(increasePayDateButton);
		
		String[] str1 = SearchSites.getAllCounties();
		for(int i=0;i<str1.length;i++){
			countyList.addItem(str1[i] );
		}
		
		filterPanel.getStateListBox().addChangeHandler(new ChangeHandler() {
			

			public void onChange(ChangeEvent event) {
					SearchSites.searchSitesService.getAllCounties(
							filterPanel.getStateListBox().getItemText(filterPanel.getStateListBox().getSelectedIndex()), 
							new AsyncCallback<String[]>(){

					public void onFailure(Throwable arg0) {
						if(SearchSites.getWaitingPopUP().isVisible()){
							SearchSites.getWaitingPopUP().hide();
						}
						Window.alert(arg0.getMessage());
					}

					public void onSuccess(String[] v) {
						SearchSites.getWaitingPopUP().show();
						ListBox newcountyList = new ListBox();
						newcountyList .addItem(FilterUtils.NONE);
						newcountyList .addItem(FilterUtils.ALL);
						for(int i=0;i<v.length;i++){
							newcountyList .addItem(v[i]);
						}
					    
						filterPanel.updateCountyListBox( newcountyList);
						SearchSites.getWaitingPopUP().hide();
					}
					
				});
			}}
		);
		
		RootPanel.get("siteFilterGWT").clear();
		RootPanel.get("siteFilterGWT").add(horPanel);
		
	}
		
	public FlexTable getSitesTable() {
		return sitesTable;
	}
	public void setSitesTable(FlexTable sitesTable) {
		this.sitesTable = sitesTable;
	}
	
	public FlexTable getSettingsTable() {
		return settingsTable;
	}

	public void setSettingsTable(FlexTable settingsTable) {
		this.settingsTable = settingsTable;
	}
}

