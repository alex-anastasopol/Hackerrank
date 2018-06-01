package ro.cst.tsearch.searchsites.client;

import java.util.Collection;
import java.util.Vector;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class ApplyChangesClickListener implements ClickHandler {

	FlexTable sitesTable ;
	CommunitySitesFilter  filterPanel;
	private int commId;
	
	public ApplyChangesClickListener(int commId, FlexTable sitesTable,CommunitySitesFilter  filterPanel ){
		this.sitesTable = sitesTable;
		this.filterPanel = filterPanel;
		this.commId = commId;
	}
	
	public void onClick(ClickEvent event) {
		SearchSites.getWaitingPopUP().show();
		
		boolean success = true;
		
		Vector<GWTDataSite> changedSites = new Vector<GWTDataSite>();
		Vector<GWTCommunitySite> changedCommunitySites = new Vector<GWTCommunitySite>();
		
		
		for (int row = 1; row < filterPanel.getSda().getSitesTable().getRowCount(); row++) {
			success = success && fillChangedSites(changedSites, filterPanel.getSda().getSitesTable(), row);
		}
		
		for (int row = 1; row < filterPanel.getSda().getSettingsTable().getRowCount(); row++) {
			success = success && fillChangedCommunitySites(changedCommunitySites, filterPanel.getSda().getSettingsTable(), row);
		}
		
		if(changedSites.isEmpty() && changedCommunitySites.isEmpty()) {
			if (SearchSites.getWaitingPopUP().isVisible()){
				SearchSites.getWaitingPopUP().hide();
			}
			Window.alert("Nothing to be updated");
			return;
		}
		
		if(!success){
			if (SearchSites.getWaitingPopUP().isVisible()){
				SearchSites.getWaitingPopUP().hide();
			}
			return;
		}
		
		SearchSites.searchSitesService.applyChanges( commId, changedSites, changedCommunitySites, new AsyncCallback<Void>(){

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
				Window.alert("Data updated");
			}
			
		});
	}
	
	private boolean invalidCharacterEntered(String name){
		if(name.matches("[.a-zA-Z0-9-]*")){
			return false;
		}
		return true;
	}
	
	private boolean fillChangedCommunitySites(Vector<GWTCommunitySite> changedCommunitySites, FlexTable settingsTable, int row) {
		if (settingsTable.getWidget(row, Util.CS_COLUMN_STATE_ABREV) == null) {
			return false;
		}
		
		GWTCommunitySite site = new GWTCommunitySite();
		
		site.setStateAbrev(((Label)settingsTable.getWidget(row, Util.CS_COLUMN_STATE_ABREV)).getText());
		site.setCountyName(((Label)settingsTable.getWidget(row, Util.CS_COLUMN_COUNTY_NAME)).getText());
		site.setSiteTypeAbrev(((Label)settingsTable.getWidget(row, Util.CS_COLUMN_SITE_TYPE_ABREV)).getText());
		int commId = Integer.parseInt(((Hidden)settingsTable.getWidget(row, Util.CS_COLUMN_COMMUNITY_ID)).getName());
		site.setCommId(commId);
		site.setCountyId(Integer.parseInt(((Hidden)settingsTable.getWidget(row, Util.CS_COLUMN_COUNTY_ID)).getName()));
		
		int dbenabled1 = 0;
		HorizontalPanel panel1 = (HorizontalPanel) settingsTable.getWidget(row, Util.CS_COLUMN_ENABLE_STATUS);
		int nrcheckBoxs1 = panel1.getWidgetCount();
		if (nrcheckBoxs1 == 0) {
			dbenabled1 = -1;
		} else {
			dbenabled1 = getEnableValue(panel1);
		}
		
		site.setEnableStatus(dbenabled1);
		
		Collection<GWTDataSite> siteData = SearchSites.getSiteData(filterPanel.getSelectedState(), filterPanel.getSelectedCounty());
		
		for (GWTDataSite gwtDataSite : siteData) {
			if(gwtDataSite.equals(site)) {
				Integer oldEnableValue = gwtDataSite.getCommunityActivation().get(commId);
				if(oldEnableValue == null) {
					oldEnableValue = 0;
				}
				if(oldEnableValue != site.getEnableStatus()) {
					changedCommunitySites.add(site);
				}
			}
		}
		
		return true;
	}

	protected int getEnableValue(HorizontalPanel horPanel) {
		int enableStatus = 0;
		FocusCheckBoxPanel curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_ENABLE);
		if(curent.isChecked()) {
			enableStatus = Util.enableSite(enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_AUTO_SALE);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteAutomaticForProduct(1, enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_AUTO_CRT_OWNER);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteAutomaticForProduct(2, enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_AUTO_CONSTRUCTION);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteAutomaticForProduct(3, enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_AUTO_COMMERCIAL);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteAutomaticForProduct(4, enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_AUTO_REFINANCE);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteAutomaticForProduct(5, enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_AUTO_OE_HELOC);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteAutomaticForProduct(6, enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_AUTO_LIENS);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteAutomaticForProduct(7, enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_AUTO_ACREAGE);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteAutomaticForProduct(8, enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_AUTO_SUBLOT);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteAutomaticForProduct(9, enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_AUTO_UPDATE);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteAutomaticForProduct(10, enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_AUTO_FVS_UPDATE);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteAutomaticForProduct(12, enableStatus);
		}
		
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_BOOTSTRAP_NAME);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteNameBootstrap(enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_BOOTSTRAP_ADDRESS);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteAddressBootstrap(enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_BOOTSTRAP_LEGAL);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteLegalBootstrap(enableStatus);
		}
		
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_DERIV_NAME);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteNameDerivation(enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_OCR);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteOCR(enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_INCLUDE_IN_TSR);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteIncludeInTsr(enableStatus);
		}
		curent = (FocusCheckBoxPanel)horPanel.getWidget(SearchSites.SITE_POS_INCL_IMAGE_IN_TSR);
		if(curent.isChecked()) {
			enableStatus = Util.enableSiteIncludeImageInTsr(enableStatus);
		}
		return enableStatus;
	}

	private boolean fillChangedSites(Vector<GWTDataSite> changedSites, FlexTable mainSitesTable, int row) {
		//don't know why
		if(mainSitesTable.getWidget(row, 0) == null) {
			return false;
		}
		
		String parserFileName = ((TextBox) mainSitesTable.getWidget(row, Util.SS_COLUMN_PARSER_NAME)).getText();
		if (invalidCharacterEntered(parserFileName)) {
			Window.alert("Invalid Character Entered ,  Parser File = " + parserFileName);
			return false;
		}

		String serverClassName = ((TextBox) mainSitesTable.getWidget(row, Util.SS_COLUMN_SERVER_CLASS_NAME)).getText();
		if (invalidCharacterEntered(serverClassName)) {
			Window.alert("Invalid Character Entered ,  Search File = " + serverClassName);
			return false;
		}

		String classConnFilename = ((TextBox) mainSitesTable.getWidget(row, Util.SS_COLUMN_CONN_CLASS_NAME)).getText();
		if (invalidCharacterEntered(classConnFilename)) {
			Window.alert("Invalid Character Entered ,  Connection File = " + classConnFilename);
			return false;
		}
		

		GWTDataSite site = new GWTDataSite();
		site.setParserFileNameSuffix(parserFileName);
		site.setClassFilename(serverClassName);
		site.setClassConnFilename(classConnFilename);
		
		CountyNamePanel countypanel = (CountyNamePanel) mainSitesTable.getWidget(row, Util.SS_COLUMN_COUNTY_NAME);
		site.setCountyName(countypanel.getCountyName());
		
		site.setStateAbrv(((Label) mainSitesTable.getWidget(row, Util.SS_COLUMN_STATE_ABREV)).getText());
		site.setSiteTypeAbrv(((Label) mainSitesTable.getWidget(row, Util.SS_COLUMN_SITE_TYPE_ABREV)).getText());
		site.setLink(((TextBox) mainSitesTable.getWidget(row, Util.SS_COLUMN_LINK)).getText());

		try {
			site.setMaxSessions(Integer.parseInt(((TextBox) mainSitesTable.getWidget(row, Util.SS_COLUMN_MAX_SESSIONS)).getText()));
		} catch (Exception e) {
		}

		int connectionTimeout = -1;
		int searchTimeout = 0;
		HorizontalPanel timeoutPanel = ((HorizontalPanel) mainSitesTable.getWidget(row, Util.SS_COLUMN_TIMEOUT_PANEL));
		try {
			connectionTimeout = Integer.parseInt(((TextBox) timeoutPanel.getWidget(0)).getText()) * 1000;
			searchTimeout = Integer.parseInt((((TextBox) timeoutPanel.getWidget(1)).getText())) * 1000;
		} catch (Exception e) {
		}
		if (connectionTimeout < 0) {
			connectionTimeout = -1;
		}
		site.setConnectionTimeout(connectionTimeout);
		site.setSearchTimeout(searchTimeout);

		try {
			site.setTimeBetweenRequests(Integer.parseInt(((TextBox) mainSitesTable.getWidget(row, Util.SS_COLUMN_TIME_BETWEEN_REQUETS)).getText()) * 1000);
		} catch (Exception e) {
		}

		HorizontalPanel mrpsPanel = ((HorizontalPanel) mainSitesTable.getWidget(row, Util.SS_COLUMN_MAX_REQUESTS_PANEL));

		int maxRequestsPerSecond = 0;
		int units = 1;
		try {
			maxRequestsPerSecond = Integer.parseInt(((TextBox) mrpsPanel.getWidget(0)).getText());
			units = Integer.parseInt((((TextBox) mrpsPanel.getWidget(1)).getText())) * 1000;
		} catch (Exception e) {
		}
		site.setMaxRequestsPerSecond(maxRequestsPerSecond);
		site.setUnits(units);
		
		
		// set certified
		if (((FocusCheckBoxPanel) mainSitesTable.getWidget(row, Util.SS_COLUMN_CERTIFIED)).isChecked()) {
			site.setSiteCertified(Util.enableCertified(0));
		}

		try {
			String effectiveStartDateString = ((TextBox) mainSitesTable.getWidget(row, Util.SS_COLUMN_EFFECTIVE_START_DATE)).getText();
			if (effectiveStartDateString != null && effectiveStartDateString.trim().length() > 0) {
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
				site.setEffectiveStartDate(sdf.parse(effectiveStartDateString.trim()));
			}
		} catch (Exception e) {
		}
		
		int enableStatus = 0;
		HorizontalPanel panel = (HorizontalPanel) mainSitesTable.getWidget(row, Util.SS_COLUMN_ENABLE_STATUS);
		int nrcheckBoxs = panel.getWidgetCount();
		if (nrcheckBoxs == 0) {
			enableStatus = -1;
			
			Collection<GWTDataSite> sitesForCounty = SearchSites.getSiteData(filterPanel.getSelectedState(), filterPanel.getSelectedCounty());
			
			for (GWTDataSite gwtDataSite : sitesForCounty) {
				if (gwtDataSite.equals(site)) {
					if(gwtDataSite.isChanged(site)) {
						changedSites.add(site);
						// this data must be loaded from gwtDataSite because they don't exists in the table
						site.setAdressToken(gwtDataSite.getAdressToken());
						site.setAdressTokenMiss(gwtDataSite.getAdressTokenMiss());
						site.setAutpos(gwtDataSite.getAutpos());
						site.setAlternateLink(gwtDataSite.getAlternateLink());
						site.setConnType(gwtDataSite.getConnType());
						// dat.setAbsTimeBetweenRequests(gwtDataSite.getAbsTimeBetweenRequests());
						site.setDefaultCertificationDateOffset(gwtDataSite.getDefaultCertificationDateOffset());
						site.setNumberOfYears(gwtDataSite.getNumberOfYears());
						site.setDocType(gwtDataSite.getDocType());
						site.setPasswordCode(gwtDataSite.getPasswordCode());
					}
					break;
				}
			}
			
			
		} else {
			
			enableStatus = getEnableValue(panel);
			
			for (Integer commId : SearchSites.getAllCommunities().keySet()) {
				site.getCommunityActivation().put(commId, enableStatus);
			}
			
			//since it's new site just add it
			changedSites.add(site);
			
		}
		
		return true;
	}
    
}
