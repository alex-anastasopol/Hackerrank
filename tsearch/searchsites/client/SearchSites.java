package ro.cst.tsearch.searchsites.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Cristian Stochina
 */
public class SearchSites implements EntryPoint {

	/* State -> County -> STCountySRV -> GwtDataSite map */
	static Map<String,
			Map<String,
				Map<String,
					GWTDataSite>>> allSiteData = null;

	private static String[] allStatesAbrev = null;

	private static String[] allSitesAbrev = null;

	private static String[] allCounties = null;
	
	private static Map<Integer, String> allCommunities = null;
	
	private static int currentCommId = 0;

	public static final SearchSitesServiceAsync searchSitesService = (SearchSitesServiceAsync)GWT.create(SearchSitesService.class);
	
	public static final String SITE_ENABLE = "Site";
	public static final String SITE_AUTO_SALE = "Automatic Sale";
	public static final String SITE_AUTO_CRT_OWNER = "Automatic Current Owner";
	public static final String SITE_AUTO_CONSTRUCTION = "Automatic Construction";
	public static final String SITE_AUTO_COMMERCIAL = "Automatic Commercial";
	public static final String SITE_AUTO_REFINANCE = "Automatic Refinance";
	public static final String SITE_AUTO_OE_HELOC = "Automatic OE";
	public static final String SITE_AUTO_LIENS = "Automatic Liens";
	public static final String SITE_AUTO_ACREAGE = "Automatic Acreage";
	public static final String SITE_AUTO_SUBLOT = "Automatic Sublot";
	public static final String SITE_AUTO_UPDATE = "Automatic Update";
	public static final String SITE_AUTO_FVS = "Automatic FVS Update";
	public static final String SITE_OCR = "OCR";
	public static final String SITE_NAME_BOOTSTRAP = "Name Bootstrap";
	public static final String SITE_ADDRESS_BOOTSTRAP = "Address Bootstrap";
	public static final String SITE_LEGAL_BOOTSTRAP = "Legal Bootstrap";
	public static final String SITE_NAME_DERIVATION = "Name Derivation";
	public static final String SITE_INCLUDE_DOC_IN_TSR = "Include in TSR";
	public static final String SITE_INCLUDE_IMAGE_IN_TSR = "Include Image TSR";

	public static int											SITE_POS_ENABLE				= 0;

	public static int											SITE_POS_AUTO_SALE			= 1;
	public static int											SITE_POS_AUTO_CRT_OWNER		= 2;
	public static int											SITE_POS_AUTO_CONSTRUCTION	= 3;
	public static int											SITE_POS_AUTO_COMMERCIAL	= 4;
	public static int											SITE_POS_AUTO_REFINANCE		= 5;
	public static int											SITE_POS_AUTO_OE_HELOC		= 6;
	public static int											SITE_POS_AUTO_LIENS			= 7;
	public static int											SITE_POS_AUTO_ACREAGE		= 8;
	public static int											SITE_POS_AUTO_SUBLOT		= 9;
	public static int											SITE_POS_AUTO_UPDATE		= 10;
	public static int											SITE_POS_AUTO_FVS_UPDATE	= 11;

	public static int											SITE_POS_BOOTSTRAP_NAME		= 12;
	public static int											SITE_POS_BOOTSTRAP_ADDRESS	= 13;
	public static int											SITE_POS_BOOTSTRAP_LEGAL	= 14;

	public static int											SITE_POS_DERIV_NAME			= 15;
	public static int											SITE_POS_OCR				= 16;
	public static int											SITE_POS_INCLUDE_IN_TSR		= 17;
	public static int											SITE_POS_INCL_IMAGE_IN_TSR	= 18;

	public static int											SITE_POS_CHECK_ALL			= 19;
	
	
	public void onModuleLoad() {

		ServiceDefTarget endpoint = (ServiceDefTarget) searchSitesService;
		endpoint.setServiceEntryPoint("/title-search/SearchSitesServer");
		currentCommId = getCommId();
		
		init();
	}

	private static PopupPanel waitingPopUP = null;

	public static void init() {
		init(currentCommId);
	}
	public static void init(int commId) {
        
		waitingPopUP = new PopupPanel();
		waitingPopUP.add(new HTML(Util.WAITING_MESSAGE));
		waitingPopUP.setPopupPosition(Util.WAITING_POPUP_COL,Util.WAITING_POPUP_ROW);
		waitingPopUP.setModal(true);
		
		RootPanel.get("slotGWT").clear();
		RootPanel.get("siteFilterGWT").clear();
		RootPanel.get("siteFilterGWT").add(new HTML("<center><H3>Loading ...</H3></center>"));
		RootPanel.get("communitySlotGWT").clear();
		
		SitesDataArived sda = new SitesDataArived(commId);
		SearchSites.searchSitesService.getDataCompact( sda );

		VerticalPanel vertpanel = new VerticalPanel();
		vertpanel.add(sda.getSitesTable());
		RootPanel.get("slotGWT").add(vertpanel);
		
		VerticalPanel vertpanelSettings = new VerticalPanel();
		vertpanelSettings.add(sda.getSettingsTable());
		RootPanel.get("communitySlotGWT").add(vertpanelSettings);

	}


	public static void setSiteData(Map<String, Map<String, Map<String, GWTDataSite>>> siteData) {
		SearchSites.allSiteData = siteData;
	}
	
	public static Map<String, Map<String, Map<String, GWTDataSite>>> getSiteData() {
		return SearchSites.allSiteData;
	}

	
	static Collection<GWTDataSite> getSiteData(String state, String county) {
		
		Collection<GWTDataSite> sites = new ArrayList<GWTDataSite>();
		
		for(Entry<String, Map<String, Map<String, GWTDataSite>>> stateCountyEntry : SearchSites.getSiteData().entrySet()) {
			String currentState = stateCountyEntry.getKey();
			Map<String,Map<String,GWTDataSite>> countySiteMap = stateCountyEntry.getValue();
			
			if("ALL".equals(state) || currentState.equals(state)) {
				for(Entry<String, Map<String, GWTDataSite>> countySiteEntry : countySiteMap.entrySet()) {
					String currentCounty = countySiteEntry.getKey();
					Map<String,GWTDataSite> siteMap = countySiteEntry.getValue();
					if("ALL".equals(county) || currentCounty.equals(county)) {
						sites.addAll(siteMap.values());
					}
				}
			}
		}
				
		return sites;
	}

	public static void fillSiteTableWithDataSite(final FlexTable sitesTable, final GWTDataSite[] siteData) {
		int count = sitesTable.getRowCount();
		addHeader(sitesTable, siteData, false);
		if (siteData.length < count) { // just update for performance reason
			int i = 0;
			for (; i < siteData.length; i++) {
				fillRow(i + 1, sitesTable, siteData[i]);
			}
			for (int j = count - 1; j > i; j--) {
				sitesTable.removeRow(j);
			}
		} else {
			int i = 0;
			if (count == 0) {
				// addHeader(sitesTable,siteData);
			} else {
				int to = count - 1;
				for (; i < to; i++) {
					fillRow(i + 1, sitesTable, siteData[i]);
				}
			}
			for (; i < siteData.length; i++) {
				addRow(i + 1, sitesTable, siteData[i], false);
			}
		}
			
	}
	
	public static void fillSettingsTableWithDataSite(final FlexTable settingsTable, final GWTDataSite[] siteData, int commId) {

		int count = settingsTable.getRowCount();
		addHeader(settingsTable, siteData, true);
		
			
		Map<Integer, String> allCommunities = SearchSites.getAllCommunities();
		Set<Integer> commIds = new HashSet<Integer>();
		if(allCommunities.containsKey(commId)) {
			commIds.add(commId);
		} else {
			commIds.addAll(allCommunities.keySet());
		}
		
		int rowsEdited = 1;		//first row is the header
		for (int i = 0; i < siteData.length; i++) {
			rowsEdited = fillSettingsRow(settingsTable, rowsEdited, siteData[i], commIds);
		}
		//I edited just a few rows so I need to clean the old table
		if(rowsEdited < count) {
			for (int j = count - 1; j >= rowsEdited; j--) {
				settingsTable.removeRow(j);
			}
		}
		
	}

	public static void addHeader(final FlexTable sitesTable, final GWTDataSite[] siteData, boolean isSettingsTable) {
		int i=0;
		if(isSettingsTable) {
			sitesTable.setWidget( 0, i++, new HTML( "<font color='blue'><b><a href='#''>Community</a></b></font>" ) );
			((HTML) sitesTable.getWidget(0, i-1)).addClickHandler(
					new ClickHandler() {
						public void onClick(ClickEvent arg0) {
							SearchSites.getWaitingPopUP().show();
							Arrays.sort( siteData, new GWTDataSite.StateAbrvComparator() );
							SearchSites.fillSiteTableWithDataSite( sitesTable, siteData );
							SearchSites.getWaitingPopUP().hide();
						}
					}
			);
		}
		
		sitesTable.setWidget( 0, i++, new HTML( "<font color='blue'><b><a href='#''>St</a></b></font>" ) );
		((HTML) sitesTable.getWidget(0, i-1)).addClickHandler(
				new ClickHandler() {
					public void onClick(ClickEvent arg0) {
						SearchSites.getWaitingPopUP().show();
						Arrays.sort( siteData, new GWTDataSite.StateAbrvComparator() );
						SearchSites.fillSiteTableWithDataSite( sitesTable, siteData );
						SearchSites.getWaitingPopUP().hide();
					}
				}
		);

		sitesTable.setWidget(0, i++, new HTML( "<font color='blue'><b><a href='#''>County</a></b></font>") );
		((HTML) sitesTable.getWidget(0, i-1)).addClickHandler(
				new ClickHandler() {
					public void onClick(ClickEvent arg0) {
						SearchSites.getWaitingPopUP().show();
						Arrays.sort( siteData, new GWTDataSite.CountyNameComparator() );
						SearchSites.fillSiteTableWithDataSite( sitesTable, siteData );
						SearchSites.getWaitingPopUP().hide();
					}
				}
		);

		sitesTable.setWidget(0, i++, new HTML("<font color='blue'><b><a href='#''>Site</a></b></font>"));
		((HTML) sitesTable.getWidget(0, i-1)).addClickHandler(
				new ClickHandler() {
					public void onClick(ClickEvent arg0) {
						SearchSites.getWaitingPopUP().show();
						Arrays.sort( siteData, new GWTDataSite.SiteAbrvComparator() );
						SearchSites.fillSiteTableWithDataSite(sitesTable, siteData);
						SearchSites.getWaitingPopUP().hide();
					}
				}
		);
		
		if(isSettingsTable) {
			VerticalPanel vp = new VerticalPanel();
			vp.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
			vp.add(new HTML("<font color='blue'><b>Enable features</b></font>"));
			vp.add(getEnableAllCheckboxesPanel(sitesTable));
			sitesTable.setWidget(0, i++, vp);
		} else {
			sitesTable.setWidget(0, i++, new HTML( "<font color='blue'><b><a href='#''>Parser File</a></b></font>" ) );
			((HTML) sitesTable.getWidget(0, i-1)).addClickHandler(
					new ClickHandler() {
						public void onClick(ClickEvent arg0) {
							SearchSites.getWaitingPopUP().show();
							Arrays.sort( siteData, new GWTDataSite.ParserFilenameComparator() );
							SearchSites.fillSiteTableWithDataSite( sitesTable, siteData );
							SearchSites.getWaitingPopUP().hide();
						}
					}
			);
			
	
			sitesTable.setWidget(0, i++, new HTML( "<font color='blue'><b><a href='#''>Search File</a></b></font>" ) );
			((HTML) sitesTable.getWidget(0, i-1)).addClickHandler(
						new ClickHandler() {
							public void onClick(ClickEvent arg0) {
								SearchSites.getWaitingPopUP().show();
								Arrays.sort(siteData,new GWTDataSite.ClassFilenameComparator());
								SearchSites.fillSiteTableWithDataSite(sitesTable,siteData);
								SearchSites.getWaitingPopUP().hide();
							}
						}
			);
			sitesTable.setWidget(0, i++, new HTML("<font color='blue'><b><a href='#''>Connection File</a></b></font>") );
			((HTML) sitesTable.getWidget(0, i-1)).addClickHandler(
					new ClickHandler() {
						public void onClick(ClickEvent arg0) {
							SearchSites.getWaitingPopUP().show();
							Arrays.sort(siteData,new GWTDataSite.ClassConnFilenameComparator());
							SearchSites.fillSiteTableWithDataSite(sitesTable, siteData);
							SearchSites.getWaitingPopUP().hide();
						}
					}
			);
			sitesTable.setWidget(0, i++, new HTML("<font color='blue'><b><a href='#''>HTTP Link</a></b></font>"));
			((HTML) sitesTable.getWidget(0, i-1)) .addClickHandler(
					new ClickHandler() {
						public void onClick(ClickEvent arg0) {
							SearchSites.getWaitingPopUP().show();
							Arrays.sort(siteData, new GWTDataSite.LinkComparator());
							SearchSites.fillSiteTableWithDataSite(sitesTable, siteData);
							SearchSites.getWaitingPopUP().hide();
						}
					}
			);
			sitesTable.setWidget(0, i++, new HeaderFocusPanel("<font color='blue'><b>Ses</b></font>", "Max Sessions", true));
			sitesTable.setWidget(0, i++, new HeaderFocusPanel("<font color='blue'><b>Timeout</b></font>", "Connection TimeOut  / Search Timeout", true));
			sitesTable.setWidget(0, i++, new HeaderFocusPanel("<font color='blue'><b>TBR</b></font>","Time between requests", true));
			sitesTable.setWidget(0, i++, new HeaderFocusPanel("<font color='blue'><b>MRPTU</b></font>", "Max Request / Time units", true));
			i++;
			sitesTable.setWidget(0, i++, new HeaderFocusPanel("<font color='blue'><b>Dates(MM/dd/yyyy)</b></font>", "Effective Start Date", true));
			sitesTable.setWidget(0, i++, new HeaderFocusPanel("<font color='blue'><b>Doctype</b></font>", "Doctype in Database", true));
				
			
			VerticalPanel vp1 = new VerticalPanel();
			vp1.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
			vp1.add(new HTML("<font color='blue'><b><a href='#''>Certified</a></b></font>"));
			vp1.add(getCheckAllCheckboxesCertifiedPanel(sitesTable));
			sitesTable.setWidget(0, i++, vp1);
			
		}
	}

	public static void fillRow(int row, FlexTable sitesTable, GWTDataSite cur) {
		String  tempDueDate=null;
		String  tempPayDate=null;
		String tempCityName =null;
		int tempTaxYearMode = TaxSiteData.TAX_YEAR_PD_YEAR; 
		if(cur instanceof TaxSiteData){
			TaxSiteData temp = (TaxSiteData)cur;
			tempDueDate = temp.getDueDate();
			tempPayDate = temp.getPayDate();
			tempCityName = temp.getCityName();
			tempTaxYearMode = temp.getTaxYearMode();
			if(tempDueDate == null) tempDueDate = "";
			if(tempPayDate == null) tempPayDate = "";
			if(tempCityName == null ) tempCityName = ""; 
		}
		
		final int numberOfYears = cur.getNumberOfYears();
		
		final String dueDate = tempDueDate;
		final String payDate = tempPayDate;
		final String cityName = tempCityName;
		final int taxYearMode = tempTaxYearMode;
		
		final String addressToken = cur.getAdressToken();
		final String addressMiss = cur.getAdressTokenMiss();
		final String alternateLink = cur.getAlternateLink();
		final String connType = cur.getConnType()+"";

		String className = cur.getClassFilename();
		final String countyName = cur.getCountyName();
		String parserFileName = cur.getParserFileNameSuffix();
		String connectionClassName = cur.getClassConnFilename();
		final String stateAbrev = cur.getStateAbrv();
		final String siteTypeAbrev = cur.getSiteTypeAbrv();
		
		String link = cur.getLink();
		final boolean isRoLikeSite = cur.isRoLikeSite();
		
		String sessionNr = cur.getMaxSessions() + "";
		String tbrText = cur.getTimeBetweenRequests() / 1000 + "";
		String timeUnitText = cur.getUnits() / 1000 + "";
		String requetPerTimeUnitText = cur.getMaxRequestsPerSecond()/* /1000 */
				+ "";

		String connTimeOut = (cur.getConnectionTimeout() < 0 ? cur
				.getConnectionTimeout() : cur.getConnectionTimeout() / 1000)
				+ "";
		String searchTimeOut = cur.getSearchTimeout() / 1000 + "";

//		((Label) sitesTable.getWidget(row, 0)).setText(communityName);
		
		int i=0; //widget index
		
		((Label) sitesTable.getWidget(row, i++)).setText(stateAbrev);

		CountyNamePanel countypanel = (CountyNamePanel) sitesTable.getWidget(
				row, i++);
		countypanel.setCountyName(countyName, row + "");

		HandlerRegistration handlerRegistration = countypanel.getHandlerRegistration();
		if(handlerRegistration != null) {
			handlerRegistration.removeHandler();
		}

		countypanel.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				
				Widget source = (Widget)event.getSource();

				AdvancedDialog box = new AdvancedDialog(stateAbrev, countyName,
						siteTypeAbrev, cityName, addressToken, addressMiss,	source.getAbsoluteLeft() + 30, source.getAbsoluteTop() - 150);
				
				box.setStyleName("gwt-DialogBox");

				if(dueDate!=null){
					box.setDueDate(dueDate);
				}
				if(payDate!=null){
					box.setPayDate(payDate);
				}
				
				if (cityName != null ){
					box.setCityName(cityName);
				}
				
				if(alternateLink!=null) {
					box.setAlternateLink(alternateLink);
				}
								
				box.setIsRoLikeSite(isRoLikeSite);
								
				if(connType!=null) {
					box.setConnType(Integer.parseInt(connType));
				}				
				
				box.setTaxYearMode(taxYearMode);
				
				if (numberOfYears > 0){
					box.setNumberOfYears(numberOfYears);
				}
				
				box.show();
			}

		});
		
		((Label) sitesTable.getWidget(row, 2)).setText(siteTypeAbrev);
		
		((TextBox) sitesTable.getWidget(row, 3)).setText(parserFileName);
		((TextBox) sitesTable.getWidget(row, 4)).setText(className);
		((TextBox) sitesTable.getWidget(row, 5))
				.setText(connectionClassName/* connection class name */);
		((TextBox) sitesTable.getWidget(row, 6)).setText(link);
		((TextBox) sitesTable.getWidget(row, 7)).setText(sessionNr/* max session per site */);

		/* timeout panel */
		HorizontalPanel panelTimeOut = (HorizontalPanel) sitesTable.getWidget(row, 8);
		((TextBox) panelTimeOut.getWidget(0)).setText(connTimeOut);
		((TextBox) panelTimeOut.getWidget(1)).setText(searchTimeOut);

		((TextBox) sitesTable.getWidget(row, 9)).setText(tbrText);

		/* max connection per unit time */
		HorizontalPanel maxConPerTimeUnitPanel = (HorizontalPanel) sitesTable.getWidget(row, 10);
		((TextBox) maxConPerTimeUnitPanel.getWidget(0)).setText(requetPerTimeUnitText);
		((TextBox) maxConPerTimeUnitPanel.getWidget(1)).setText(timeUnitText);

		HorizontalPanel panel = (HorizontalPanel) sitesTable.getWidget(row, 11);
		panel.clear();
		
		TextBox effectiveStartDateTextBox = (TextBox) sitesTable.getWidget(row, 12);
		effectiveStartDateTextBox.setText(cur.getEffectiveStartDateString());
		if(cur.isRoLikeSite()) {
			effectiveStartDateTextBox.setEnabled(true);
			effectiveStartDateTextBox.setStyleName("gwt-enabledTextBox");
		} else {
			effectiveStartDateTextBox.setEnabled(false);
			effectiveStartDateTextBox.setStyleName("gwt-disabledTextBox");
		}
		
		TextBox docTypeTextBox = (TextBox) sitesTable.getWidget(row, 13);
		docTypeTextBox.setText(cur.getDocTypeToShow());
		docTypeTextBox.setEnabled(false);
		docTypeTextBox.setStyleName("gwt-disabledTextBox");

		((FocusCheckBoxPanel) sitesTable.getWidget(row, 14)).setChecked(Util.isSiteCertified(cur.getSiteCertified()));
	}
	
	
	public static int fillSettingsRow(FlexTable sitesTable, int rowStart, GWTDataSite cur, Set<Integer> commIds) {
		int row = rowStart;
		
		for (Integer commId : commIds) {
			Integer enabledObject = cur.getCommunityActivation().get(commId);
			int dbenabled = enabledObject == null? 0 : enabledObject;
			
			boolean enabled = Util.isSiteEnabled(dbenabled);
			boolean automaticEnabled1 = Util.isSiteEnabledAutomaticForProduct(1, dbenabled);
			boolean automaticEnabled2 = Util.isSiteEnabledAutomaticForProduct(2, dbenabled);
			boolean automaticEnabled3 = Util.isSiteEnabledAutomaticForProduct(3, dbenabled);
			boolean automaticEnabled4 = Util.isSiteEnabledAutomaticForProduct(4, dbenabled);
			boolean automaticEnabled5 = Util.isSiteEnabledAutomaticForProduct(5, dbenabled);
			boolean automaticEnabled6 = Util.isSiteEnabledAutomaticForProduct(6, dbenabled);
			boolean automaticEnabled7 = Util.isSiteEnabledAutomaticForProduct(7, dbenabled);
			boolean automaticEnabled8 = Util.isSiteEnabledAutomaticForProduct(8, dbenabled);
			boolean automaticEnabled9 = Util.isSiteEnabledAutomaticForProduct(9, dbenabled);
			boolean automaticEnabled10 = Util.isSiteEnabledAutomaticForProduct(10, dbenabled);
			boolean automaticEnabled12 = Util.isSiteEnabledAutomaticForProduct(12, dbenabled);
			boolean nameBootstrapEnabled = Util.isSiteEnabledNameBootstrap(dbenabled);
			boolean addressBootstrapEnabled = Util.isSiteEnabledAddressBootstrap(dbenabled);
			boolean legalBootstrapEnabled = Util.isSiteEnabledLegalBootstrap(dbenabled);
			boolean namederivationEnabled = Util.isSiteEnabledDerivation(dbenabled);
			boolean ocrEnabled = Util.isSiteEnabledOCR(dbenabled);
			boolean includeInTsrEnabled = Util.isSiteEnabledIncludeInTsr(dbenabled);
			boolean includeImageInTsrEnabled = Util.isSiteEnabledIncludeImageInTsr(dbenabled);
			
			final String countyName = cur.getCountyName();
			
			
			final String stateAbrev = cur.getStateAbrv();
			final String siteTypeAbrev = cur.getSiteTypeAbrv();
			final String communityName = allCommunities.get(commId);
			
			sitesTable.setWidget(row, Util.CS_COLUMN_COMMUNITY_NAME, new Label(communityName));
			sitesTable.setWidget(row, Util.CS_COLUMN_STATE_ABREV, new Label(stateAbrev));
			sitesTable.setWidget(row, Util.CS_COLUMN_COUNTY_NAME, new Label(countyName));
			sitesTable.setWidget(row, Util.CS_COLUMN_SITE_TYPE_ABREV, new Label(siteTypeAbrev));

			sitesTable.setWidget(row, Util.CS_COLUMN_COMMUNITY_ID, new Hidden(commId + ""));
			sitesTable.setWidget(row, Util.CS_COLUMN_COUNTY_ID, new Hidden(cur.getCountyId() + ""));
			
			if(sitesTable.getRowCount() > row && sitesTable.getWidget(row, Util.CS_COLUMN_ENABLE_STATUS) != null) {
				
				HorizontalPanel panel = (HorizontalPanel) sitesTable.getWidget(row, Util.CS_COLUMN_ENABLE_STATUS);
				
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_ENABLE)).setChecked(enabled);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_AUTO_SALE)).setChecked(automaticEnabled1);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_AUTO_CRT_OWNER)).setChecked(automaticEnabled2);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_AUTO_CONSTRUCTION)).setChecked(automaticEnabled3);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_AUTO_COMMERCIAL)).setChecked(automaticEnabled4);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_AUTO_REFINANCE)).setChecked(automaticEnabled5);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_AUTO_OE_HELOC)).setChecked(automaticEnabled6);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_AUTO_LIENS)).setChecked(automaticEnabled7);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_AUTO_ACREAGE)).setChecked(automaticEnabled8);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_AUTO_SUBLOT)).setChecked(automaticEnabled9);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_AUTO_UPDATE)).setChecked(automaticEnabled10);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_AUTO_FVS_UPDATE)).setChecked(automaticEnabled12);
				
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_BOOTSTRAP_NAME)).setChecked(nameBootstrapEnabled);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_BOOTSTRAP_ADDRESS)).setChecked(addressBootstrapEnabled);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_BOOTSTRAP_LEGAL)).setChecked(legalBootstrapEnabled);
				
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_DERIV_NAME)).setChecked(namederivationEnabled);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_OCR)).setChecked(ocrEnabled);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_INCLUDE_IN_TSR)).setChecked(includeInTsrEnabled);
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_INCL_IMAGE_IN_TSR)).setChecked(includeImageInTsrEnabled);
				
				((FocusCheckBoxPanel) panel.getWidget(SITE_POS_CHECK_ALL)).setChecked(false);
			
			} else {
				
				HorizontalPanel enablepanel = new HorizontalPanel();
		
				FocusCheckBoxPanel siteEnabledCustomCheckBox = new FocusCheckBoxPanel(enabled, SITE_ENABLE);
				FocusCheckBoxPanel siteEnableAOCustomCheckBox1 = new FocusCheckBoxPanel(automaticEnabled1, SITE_AUTO_SALE);
				FocusCheckBoxPanel siteEnableAOCustomCheckBox2 = new FocusCheckBoxPanel(automaticEnabled2, SITE_AUTO_CRT_OWNER);
				FocusCheckBoxPanel siteEnableAOCustomCheckBox3 = new FocusCheckBoxPanel(automaticEnabled3, SITE_AUTO_CONSTRUCTION);
				FocusCheckBoxPanel siteEnableAOCustomCheckBox4 = new FocusCheckBoxPanel(automaticEnabled4, SITE_AUTO_COMMERCIAL);
				FocusCheckBoxPanel siteEnableAOCustomCheckBox5 = new FocusCheckBoxPanel(automaticEnabled5, SITE_AUTO_REFINANCE);
				FocusCheckBoxPanel siteEnableAOCustomCheckBox6 = new FocusCheckBoxPanel(automaticEnabled6, SITE_AUTO_OE_HELOC);
				FocusCheckBoxPanel siteEnableAOCustomCheckBox7 = new FocusCheckBoxPanel(automaticEnabled7, SITE_AUTO_LIENS);
				FocusCheckBoxPanel siteEnableAOCustomCheckBox8 = new FocusCheckBoxPanel(automaticEnabled8, SITE_AUTO_ACREAGE);
				FocusCheckBoxPanel siteEnableAOCustomCheckBox9 = new FocusCheckBoxPanel(automaticEnabled9, SITE_AUTO_SUBLOT);
				FocusCheckBoxPanel siteEnableAOCustomCheckBox10 = new FocusCheckBoxPanel(automaticEnabled10, SITE_AUTO_UPDATE);
				FocusCheckBoxPanel siteEnableAOCustomCheckBox12 = new FocusCheckBoxPanel(automaticEnabled12, SITE_AUTO_FVS);
				
				FocusCheckBoxPanel siteEnableNameBootStrapCustomCheckBox = new FocusCheckBoxPanel(nameBootstrapEnabled, SITE_NAME_BOOTSTRAP);
				FocusCheckBoxPanel siteEnableAddressBootStrapCustomCheckBox = new FocusCheckBoxPanel(addressBootstrapEnabled, SITE_ADDRESS_BOOTSTRAP);
				FocusCheckBoxPanel siteEnableLegalBootStrapCustomCheckBox = new FocusCheckBoxPanel(legalBootstrapEnabled, SITE_LEGAL_BOOTSTRAP);
				FocusCheckBoxPanel siteEnableNameDerivationCustomCheckBox = new FocusCheckBoxPanel(namederivationEnabled, SITE_NAME_DERIVATION);
				FocusCheckBoxPanel siteEnableOCRCustomCheckBox = new FocusCheckBoxPanel(ocrEnabled, SITE_OCR);
				FocusCheckBoxPanel siteEnableIncludeInTsrCustomCheckBox = new FocusCheckBoxPanel(includeInTsrEnabled, SITE_INCLUDE_DOC_IN_TSR);
				FocusCheckBoxPanel siteEnableIncludeImageInTsrCustomCheckBox = new FocusCheckBoxPanel(includeImageInTsrEnabled, SITE_INCLUDE_IMAGE_IN_TSR);
				
				//SITE_POS_ENABLE
				enablepanel.add(siteEnabledCustomCheckBox);
				
				//SITE_POS_AUTO_SALE
				enablepanel.add(siteEnableAOCustomCheckBox1);
				enablepanel.add(siteEnableAOCustomCheckBox2);
				enablepanel.add(siteEnableAOCustomCheckBox3);
				enablepanel.add(siteEnableAOCustomCheckBox4);
				enablepanel.add(siteEnableAOCustomCheckBox5);
				enablepanel.add(siteEnableAOCustomCheckBox6);
				enablepanel.add(siteEnableAOCustomCheckBox7);
				enablepanel.add(siteEnableAOCustomCheckBox8);
				enablepanel.add(siteEnableAOCustomCheckBox9);
				enablepanel.add(siteEnableAOCustomCheckBox10);
				enablepanel.add(siteEnableAOCustomCheckBox12);
				
				//SITE_POS_BOOTSTRAP_NAME
				enablepanel.add(siteEnableNameBootStrapCustomCheckBox);
				enablepanel.add(siteEnableAddressBootStrapCustomCheckBox);
				enablepanel.add(siteEnableLegalBootStrapCustomCheckBox);
				//SITE_POS_DERIV_NAME
				enablepanel.add(siteEnableNameDerivationCustomCheckBox);
				enablepanel.add(siteEnableOCRCustomCheckBox);
				enablepanel.add(siteEnableIncludeInTsrCustomCheckBox);
				enablepanel.add(siteEnableIncludeImageInTsrCustomCheckBox);
				//SITE_POS_CHECK_ALL
				enablepanel.add(getCheckAllSettingsPanelHorizontal(sitesTable, row, 4));
				
				sitesTable.setWidget(row, Util.CS_COLUMN_ENABLE_STATUS, enablepanel);
				
			}
			
			row++;
		}
				
		return row;
		
	}

	public static void addRow(int row, FlexTable sitesTable, GWTDataSite cur, boolean showEnableFeatures) {
		final String countyName = cur.getCountyName();
		final String stateAbrev = cur.getStateAbrv();
		final String siteTypeAbrev = cur.getSiteTypeAbrv();
		
		sitesTable.setWidget(row, 0, new Label(stateAbrev));
		
		

		String  tempDueDate=null;
		String  tempPayDate=null;
		String  tempCityName = null;
		int tempTaxYearMode = TaxSiteData.TAX_YEAR_PD_YEAR;
		if(cur instanceof TaxSiteData){
			TaxSiteData temp = (TaxSiteData)cur;
			tempDueDate = temp.getDueDate();
			tempPayDate = temp.getPayDate();
			tempCityName = temp.getCityName();
			tempTaxYearMode = temp.getTaxYearMode();
			if(tempDueDate == null) tempDueDate = "";
			if(tempPayDate == null) tempPayDate = "";
			if(tempCityName == null ) tempCityName = "";
		}
		
		final int numberOfYears = cur.getNumberOfYears();
		
		final  String  dueDate=tempDueDate;
		final  String  payDate=tempPayDate;
		final  String  cityName = tempCityName;
		final  int  taxYearMode = tempTaxYearMode;
		
		String className = cur.getClassFilename();
		String connectionClassName = cur.getClassConnFilename();
		
		String parserFileName = cur.getParserFileNameSuffix();
		
		String link = cur.getLink();

		String sessionNr = cur.getMaxSessions() + "";
		String tbrText = cur.getTimeBetweenRequests() / 1000 + "";
		String timeUnitText = cur.getUnits() / 1000 + "";
		String requetPerTimeUnitText = cur.getMaxRequestsPerSecond()/* /1000 */
				+ "";

		String connTimeOut = (cur.getConnectionTimeout() < 0 ? cur
				.getConnectionTimeout() : cur.getConnectionTimeout() / 1000)
				+ "";
		String searchTimeOut = cur.getSearchTimeout() / 1000 + "";

		final String alternateLink = cur.getAlternateLink();
		final String connType = cur.getConnType()+"";
		
		CountyNamePanel panel = new CountyNamePanel(countyName, "Advanced",
				true, row + "");

		final String addressToken = cur.getAdressToken();
		final String addressMiss = cur.getAdressTokenMiss();

		final boolean isRoLikeSite = cur.isRoLikeSite();
		
		if (cur.isNewRow()) {
			
			panel.addClickHandler(new ClickHandler() {
				
				@Override
				public void onClick(ClickEvent event) {
					Window.alert("Use Apply Settings first");
				}
			});
			
		} else {
			
			
			panel.addClickHandler(new ClickHandler() {
				
				@Override
				public void onClick(ClickEvent event) {
					
					Widget source = (Widget)event.getSource();
					
					AdvancedDialog box = new AdvancedDialog(stateAbrev,
							countyName, siteTypeAbrev, cityName,
							addressToken, addressMiss, 
									source.getAbsoluteLeft() + 30, 
									source.getAbsoluteTop() - 150);
					if(dueDate!=null){
						box.setDueDate(dueDate);
					}
					if(payDate!=null){
						box.setPayDate(payDate);
					}
					box.setTaxYearMode(taxYearMode);
								
					if(alternateLink!=null) {
						box.setAlternateLink(alternateLink);
					}
					
					if ( cityName!=null ) {
						box.setCityName(cityName);
					}
					
					box.setIsRoLikeSite(isRoLikeSite);
					
					if(connType!=null) {
					box.setConnType(Integer.parseInt(connType));
					}			
					
					if (numberOfYears > 0){
						box.setNumberOfYears(numberOfYears);
					}
					
					box.setStyleName("gwt-DialogBox");
					box.show();

				}

			});
		}
		sitesTable.setWidget(row, 1, panel);

		sitesTable.setWidget(row, 2, new Label(siteTypeAbrev));
		
		TextBox parserNameTextBox = new TextBox();
		parserNameTextBox.setText(parserFileName);
		parserNameTextBox.setVisibleLength(12);

		TextBox classNameTextBox = new TextBox();
		classNameTextBox.setText(className);
		classNameTextBox.setVisibleLength(12);

		TextBox connectionClassNameTextBox = new TextBox();
		connectionClassNameTextBox.setText(connectionClassName);
		connectionClassNameTextBox.setVisibleLength(12);

		TextBox linkTextBox = new TextBox();
		linkTextBox.setText(link);
		linkTextBox.setVisibleLength(30);

		TextBox tbrTextBox = new TextBox();

		tbrTextBox.setText(tbrText);
		tbrTextBox.setVisibleLength(2);

		TextBox sessionsTextBox = new TextBox();
		sessionsTextBox.setText(sessionNr);
		sessionsTextBox.setVisibleLength(2);

		HorizontalPanel enablepanel = new HorizontalPanel();

		if(showEnableFeatures) {
			FocusCheckBoxPanel siteEnabledCustomCheckBox = new FocusCheckBoxPanel(false, SITE_ENABLE);
			
			FocusCheckBoxPanel siteEnableAOCustomCheckBox1 = new FocusCheckBoxPanel(false, SITE_AUTO_SALE);
			FocusCheckBoxPanel siteEnableAOCustomCheckBox2 = new FocusCheckBoxPanel(false, SITE_AUTO_CRT_OWNER);
			FocusCheckBoxPanel siteEnableAOCustomCheckBox3 = new FocusCheckBoxPanel(false, SITE_AUTO_CONSTRUCTION);
			FocusCheckBoxPanel siteEnableAOCustomCheckBox4 = new FocusCheckBoxPanel(false, SITE_AUTO_COMMERCIAL);
			FocusCheckBoxPanel siteEnableAOCustomCheckBox5 = new FocusCheckBoxPanel(false, SITE_AUTO_REFINANCE);
			FocusCheckBoxPanel siteEnableAOCustomCheckBox6 = new FocusCheckBoxPanel(false, SITE_AUTO_OE_HELOC);
			FocusCheckBoxPanel siteEnableAOCustomCheckBox7 = new FocusCheckBoxPanel(false, SITE_AUTO_LIENS);
			FocusCheckBoxPanel siteEnableAOCustomCheckBox8 = new FocusCheckBoxPanel(false, SITE_AUTO_ACREAGE);
			FocusCheckBoxPanel siteEnableAOCustomCheckBox9 = new FocusCheckBoxPanel(false, SITE_AUTO_SUBLOT);
			FocusCheckBoxPanel siteEnableAOCustomCheckBox10 = new FocusCheckBoxPanel(false, SITE_AUTO_UPDATE);
			FocusCheckBoxPanel siteEnableAOCustomCheckBox12 = new FocusCheckBoxPanel(false, SITE_AUTO_FVS);

			FocusCheckBoxPanel siteEnableNameBootStrapCustomCheckBox = new FocusCheckBoxPanel(false, SITE_NAME_BOOTSTRAP);
			FocusCheckBoxPanel siteEnableAddressBootStrapCustomCheckBox = new FocusCheckBoxPanel(false, SITE_ADDRESS_BOOTSTRAP);
			FocusCheckBoxPanel siteEnableLegalBootStrapCustomCheckBox = new FocusCheckBoxPanel(false, SITE_LEGAL_BOOTSTRAP);
			FocusCheckBoxPanel siteEnableNameDerivationCustomCheckBox = new FocusCheckBoxPanel(false, SITE_NAME_DERIVATION);
			FocusCheckBoxPanel siteEnableOCRCustomCheckBox = new FocusCheckBoxPanel(false, SITE_OCR);

			FocusCheckBoxPanel siteEnableIncludeInTsrCustomCheckBox = new FocusCheckBoxPanel(false, SITE_INCLUDE_DOC_IN_TSR);
			FocusCheckBoxPanel siteEnableIncludeOnlyIndexInTsrCustomCheckBox = new FocusCheckBoxPanel(false, SITE_INCLUDE_IMAGE_IN_TSR);
			
			enablepanel.add(siteEnabledCustomCheckBox);
			
			enablepanel.add(siteEnableAOCustomCheckBox1);
			enablepanel.add(siteEnableAOCustomCheckBox2);
			enablepanel.add(siteEnableAOCustomCheckBox3);
			enablepanel.add(siteEnableAOCustomCheckBox4);
			enablepanel.add(siteEnableAOCustomCheckBox5);
			enablepanel.add(siteEnableAOCustomCheckBox6);
			enablepanel.add(siteEnableAOCustomCheckBox7);
			enablepanel.add(siteEnableAOCustomCheckBox8);
			enablepanel.add(siteEnableAOCustomCheckBox9);
			enablepanel.add(siteEnableAOCustomCheckBox10);
			enablepanel.add(siteEnableAOCustomCheckBox12);
			
			enablepanel.add(siteEnableNameBootStrapCustomCheckBox);
			enablepanel.add(siteEnableAddressBootStrapCustomCheckBox);
			enablepanel.add(siteEnableLegalBootStrapCustomCheckBox);
			enablepanel.add(siteEnableNameDerivationCustomCheckBox);
			enablepanel.add(siteEnableOCRCustomCheckBox);
			enablepanel.add(siteEnableIncludeInTsrCustomCheckBox);
			enablepanel.add(siteEnableIncludeOnlyIndexInTsrCustomCheckBox);
			enablepanel.add(getCheckAllSettingsPanelHorizontal(sitesTable, row, 11));
		}
		
		TextBox effectiveStartDateTextBox = new TextBox();
		effectiveStartDateTextBox.setText(cur.getEffectiveStartDateString());
		effectiveStartDateTextBox.setVisibleLength(12);
		effectiveStartDateTextBox.setTitle("Effective Date Start (MM/dd/yyyy only)");
		if(!cur.isRoLikeSite()) {
			effectiveStartDateTextBox.setEnabled(false);
			effectiveStartDateTextBox.setStyleName("gwt-disabledTextBox");
		} else {
			effectiveStartDateTextBox.setStyleName("gwt-enabledTextBox");
		}
		
		TextBox doctypeTextBox = new TextBox();
		
		doctypeTextBox.setText(cur.getDocTypeToShow());
		doctypeTextBox.setEnabled(false);
		doctypeTextBox.setStyleName("gwt-disabledTextBox");
		
		
		
		
		HorizontalPanel timeoutPanel = new HorizontalPanel();
		TextBox timeOutConnTextBox = new TextBox();
		timeOutConnTextBox.setVisibleLength(2);
		timeOutConnTextBox.setText(connTimeOut);

		TextBox timeOutSearchCheckBox = new TextBox();
		timeOutSearchCheckBox.setVisibleLength(2);
		timeOutSearchCheckBox.setText(searchTimeOut);

		timeoutPanel.add(timeOutConnTextBox);
		timeoutPanel.add(timeOutSearchCheckBox);

		TextBox timeUnitTextBox = new TextBox();
		TextBox requetPerTimeUnitTextBox = new TextBox();
		timeUnitTextBox.setVisibleLength(2);
		requetPerTimeUnitTextBox.setVisibleLength(2);
		timeUnitTextBox.setText(timeUnitText);
		requetPerTimeUnitTextBox.setText(requetPerTimeUnitText);

		HorizontalPanel mrprtuPanel = new HorizontalPanel();

		mrprtuPanel.add(requetPerTimeUnitTextBox);
		mrprtuPanel.add(timeUnitTextBox);

		sitesTable.setWidget(row, 3, parserNameTextBox);
		sitesTable.setWidget(row, 4, classNameTextBox);
		sitesTable.setWidget(row, 5, connectionClassNameTextBox);
		sitesTable.setWidget(row, 6, linkTextBox);

		sitesTable.setWidget(row, 7, sessionsTextBox);
		sitesTable.setWidget(row, 8, timeoutPanel);

		sitesTable.setWidget(row, 9, tbrTextBox);
		sitesTable.setWidget(row, 10, mrprtuPanel);

		sitesTable.setWidget(row, 11, enablepanel);
		sitesTable.setWidget(row, 12, effectiveStartDateTextBox);
		sitesTable.setWidget(row, 13, doctypeTextBox);
		
		FocusCheckBoxPanel siteEnableCertifiedCustomCheckBox = new FocusCheckBoxPanel(Util.isSiteCertified(cur.getSiteCertified()), "Certified");
		
		sitesTable.setWidget(row, 14, siteEnableCertifiedCustomCheckBox);
		
	
		
	}
	
	static String[] getAllCounties() {
		return allCounties;
	}

	static void setAllCounties(String[] currentCounties) {
		// Window.alert(currentCounties+"");
		SearchSites.allCounties = currentCounties;
	}

	static String[] getAllStatesAbrev() {
		return allStatesAbrev;
	}

	static void setAllStatesAbrev(String[] allStatesAbrev) {
		SearchSites.allStatesAbrev = allStatesAbrev;
	}

	static String[] getAllSitesAbrev() {
		return allSitesAbrev;
	}

	static void setAllSitesAbrev(String[] allSitesAbrev) {
		SearchSites.allSitesAbrev = allSitesAbrev;
	}

	public static PopupPanel getWaitingPopUP() {
		return waitingPopUP;
	}

	public static Vector<String> getAllSitesNames(String stateAbrev, String countyName) {
		GWTDataSite[] dat = getAllSites(stateAbrev, countyName).toArray(new GWTDataSite[0]);
		
		Vector<String> vec1 = new Vector<String>();
		for (int i = 0; i < dat.length; i++) {
			vec1.add( stateAbrev + countyName+ ((GWTDataSite) dat[i]).getSiteTypeAbrv() );
		}

		return vec1;
	} 
	
	public static Vector<GWTDataSite> getAllSites(String stateAbrev, String countyName) {
		Vector<GWTDataSite> vec = new Vector<GWTDataSite>();
		 
		for (GWTDataSite cur : allSiteData.get(stateAbrev).get(countyName).values()) {
			vec.add(cur);
		}

		GWTDataSite[] dat = (GWTDataSite[]) vec.toArray(new GWTDataSite[0]);
		Arrays.sort(dat, new GWTDataSite.AutomaticOrderComparator());
		
		return new Vector<GWTDataSite>(Arrays.asList(dat));
	}
	
	private static class EnableAllHandler implements ClickHandler {
		
		private static final int SETTINGS_CHECKALL_VERTICAL = 1;
		private static final int SETTINGS_CHECKALL_HORIZONTAL = 2;
		private static final int SITE_CHECKALLCERTIFIED = 3;
		private static final int SETTINGS_CHECKALL = 4;
		
		private int type = SETTINGS_CHECKALL_VERTICAL;
		private int index = -1;
		FocusCheckBoxPanel fcbp = null;
		FlexTable settingsTable = new FlexTable();
		private int widgetIndex = -1;
		
		public EnableAllHandler(int index,FocusCheckBoxPanel fcbp, FlexTable settingsTable) {
			super();
			this.index = index;
			this.fcbp = fcbp;
			this.settingsTable = settingsTable;
			this.widgetIndex = 4;
		}
		
		public EnableAllHandler(int index,FocusCheckBoxPanel fcbp, FlexTable settingsTable, int widgetIndex, int enableAllHandlerType) {
			super();
			this.index = index;
			this.fcbp = fcbp;
			this.settingsTable = settingsTable;
			this.widgetIndex = widgetIndex;
			this.type = enableAllHandlerType;
		}

		public void onClick(ClickEvent event) {
			SearchSites.getWaitingPopUP().show();
						
			switch (type) {
			case SETTINGS_CHECKALL_VERTICAL:
				for (int row = 1; row < settingsTable.getRowCount(); row++) {
					HorizontalPanel panel1 = (HorizontalPanel) settingsTable.getWidget(row, widgetIndex);
					FocusCheckBoxPanel curent = (FocusCheckBoxPanel) panel1.getWidget(index);
					curent.setChecked(fcbp.isChecked());
				}
				break;
			case SITE_CHECKALLCERTIFIED:
				for (int row = 1; row < settingsTable.getRowCount(); row++) {
					FocusCheckBoxPanel curent = (FocusCheckBoxPanel) settingsTable.getWidget(row, widgetIndex);
					curent.setChecked(fcbp.isChecked());
				}
				break;
			case SETTINGS_CHECKALL_HORIZONTAL:
				HorizontalPanel panel1 = (HorizontalPanel) settingsTable.getWidget(index, widgetIndex);
				for(int wCount = 0; wCount < panel1.getWidgetCount(); wCount++){
					FocusCheckBoxPanel curent = (FocusCheckBoxPanel) panel1.getWidget(wCount);
					curent.setChecked(fcbp.isChecked());
				}
				break;
			case SETTINGS_CHECKALL:
				for (int row = 0; row < settingsTable.getRowCount(); row++) {
					HorizontalPanel panel2 = null;
					if(row == 0){
						VerticalPanel vp = (VerticalPanel) settingsTable.getWidget(row, widgetIndex);
						if(vp.getWidgetCount() == 2){
							panel2 = (HorizontalPanel) vp.getWidget(1);
						}
					} else {
						panel2 = (HorizontalPanel) settingsTable.getWidget(row, widgetIndex);
					}

					if(panel2 != null){
						for(int col=0; col<panel2.getWidgetCount(); col++){
							FocusCheckBoxPanel curent = (FocusCheckBoxPanel) panel2.getWidget(col);
							curent.setChecked(fcbp.isChecked());
						}
					}
				}
				break;
			}
			
			SearchSites.getWaitingPopUP().hide();
		}
		
	}
	
	
	public static FocusCheckBoxPanel getCheckAllSettingsPanelHorizontal(FlexTable siteTable, int row, int widgetIndex) {
		FocusCheckBoxPanel siteCheckAllCustomCheckBox = new FocusCheckBoxPanel(false, "Check All");
		
		siteCheckAllCustomCheckBox.addClickHandler(new EnableAllHandler(row,siteCheckAllCustomCheckBox,siteTable, widgetIndex, EnableAllHandler.SETTINGS_CHECKALL_HORIZONTAL));
		
		return siteCheckAllCustomCheckBox;
	}
	
	
	public static HorizontalPanel getCheckAllCheckboxesCertifiedPanel(FlexTable siteTable) {
		HorizontalPanel enablepanel = new HorizontalPanel();
		
		FocusCheckBoxPanel siteCheckAllCustomCheckBox = new FocusCheckBoxPanel(false, "Check All");
		
		enablepanel.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
		enablepanel.add(siteCheckAllCustomCheckBox);
		
		siteCheckAllCustomCheckBox.addClickHandler(new EnableAllHandler(0,siteCheckAllCustomCheckBox,siteTable, 14, EnableAllHandler.SITE_CHECKALLCERTIFIED));
		
		return enablepanel;
	}
	
	public static HorizontalPanel getEnableAllCheckboxesPanel(FlexTable settingsTable) {
		HorizontalPanel enablepanel = new HorizontalPanel();

		FocusCheckBoxPanel siteEnabledCustomCheckBox = new FocusCheckBoxPanel(false, SITE_ENABLE);
		
		
		FocusCheckBoxPanel siteEnableAOCustomCheckBox1 = new FocusCheckBoxPanel(false, SITE_AUTO_SALE);
		FocusCheckBoxPanel siteEnableAOCustomCheckBox2 = new FocusCheckBoxPanel(false, SITE_AUTO_CRT_OWNER);
		FocusCheckBoxPanel siteEnableAOCustomCheckBox3 = new FocusCheckBoxPanel(false, SITE_AUTO_CONSTRUCTION);
		FocusCheckBoxPanel siteEnableAOCustomCheckBox4 = new FocusCheckBoxPanel(false, SITE_AUTO_COMMERCIAL);
		FocusCheckBoxPanel siteEnableAOCustomCheckBox5 = new FocusCheckBoxPanel(false, SITE_AUTO_REFINANCE);
		FocusCheckBoxPanel siteEnableAOCustomCheckBox6 = new FocusCheckBoxPanel(false, SITE_AUTO_OE_HELOC);
		FocusCheckBoxPanel siteEnableAOCustomCheckBox7 = new FocusCheckBoxPanel(false, SITE_AUTO_LIENS);
		FocusCheckBoxPanel siteEnableAOCustomCheckBox8 = new FocusCheckBoxPanel(false, SITE_AUTO_ACREAGE);
		FocusCheckBoxPanel siteEnableAOCustomCheckBox9 = new FocusCheckBoxPanel(false, SITE_AUTO_SUBLOT);
		FocusCheckBoxPanel siteEnableAOCustomCheckBox10 = new FocusCheckBoxPanel(false, SITE_AUTO_UPDATE);
		FocusCheckBoxPanel siteEnableAOCustomCheckBox12 = new FocusCheckBoxPanel(false, SITE_AUTO_FVS);
		
		FocusCheckBoxPanel siteEnableNameBootStrapCustomCheckBox = new FocusCheckBoxPanel(false, SITE_NAME_BOOTSTRAP);
		FocusCheckBoxPanel siteEnableAddressBootStrapCustomCheckBox = new FocusCheckBoxPanel(false, SITE_ADDRESS_BOOTSTRAP);
		FocusCheckBoxPanel siteEnableLegalBootStrapCustomCheckBox = new FocusCheckBoxPanel(false, SITE_LEGAL_BOOTSTRAP);
		FocusCheckBoxPanel siteEnableNameDerivationCustomCheckBox = new FocusCheckBoxPanel(false, SITE_NAME_DERIVATION);
		FocusCheckBoxPanel siteEnableOCRCustomCheckBox = new FocusCheckBoxPanel(false, SITE_OCR);
		FocusCheckBoxPanel siteEnableIncludeInTsrCustomCheckBox = new FocusCheckBoxPanel(false, SITE_INCLUDE_DOC_IN_TSR);
		FocusCheckBoxPanel siteEnableIncludeOnlyIndexInTsrCustomCheckBox = new FocusCheckBoxPanel(false, SITE_INCLUDE_IMAGE_IN_TSR);
		
		FocusCheckBoxPanel siteCheckAll = new FocusCheckBoxPanel(false, "Check All");
		
		enablepanel.add(siteEnabledCustomCheckBox);
		
		enablepanel.add(siteEnableAOCustomCheckBox1);
		enablepanel.add(siteEnableAOCustomCheckBox2);
		enablepanel.add(siteEnableAOCustomCheckBox3);
		enablepanel.add(siteEnableAOCustomCheckBox4);
		enablepanel.add(siteEnableAOCustomCheckBox5);
		enablepanel.add(siteEnableAOCustomCheckBox6);
		enablepanel.add(siteEnableAOCustomCheckBox7);
		enablepanel.add(siteEnableAOCustomCheckBox8);
		enablepanel.add(siteEnableAOCustomCheckBox9);
		enablepanel.add(siteEnableAOCustomCheckBox10);
		enablepanel.add(siteEnableAOCustomCheckBox12);
		
		enablepanel.add(siteEnableNameBootStrapCustomCheckBox);
		enablepanel.add(siteEnableAddressBootStrapCustomCheckBox);
		enablepanel.add(siteEnableLegalBootStrapCustomCheckBox);
		enablepanel.add(siteEnableNameDerivationCustomCheckBox);
		enablepanel.add(siteEnableOCRCustomCheckBox);
//		enablepanel.add(siteEnableLinkOnlyCustomCheckBox);
		enablepanel.add(siteEnableIncludeInTsrCustomCheckBox);
		enablepanel.add(siteEnableIncludeOnlyIndexInTsrCustomCheckBox);
		enablepanel.add(siteCheckAll);
		
		siteEnabledCustomCheckBox.addClickHandler(new EnableAllHandler(0,siteEnabledCustomCheckBox,settingsTable));
		
		siteEnableAOCustomCheckBox1.addClickHandler(new EnableAllHandler(1,siteEnableAOCustomCheckBox1,settingsTable));
		siteEnableAOCustomCheckBox2.addClickHandler(new EnableAllHandler(2,siteEnableAOCustomCheckBox2,settingsTable));
		siteEnableAOCustomCheckBox3.addClickHandler(new EnableAllHandler(3,siteEnableAOCustomCheckBox3,settingsTable));
		siteEnableAOCustomCheckBox4.addClickHandler(new EnableAllHandler(4,siteEnableAOCustomCheckBox4,settingsTable));
		siteEnableAOCustomCheckBox5.addClickHandler(new EnableAllHandler(5,siteEnableAOCustomCheckBox5,settingsTable));
		siteEnableAOCustomCheckBox6.addClickHandler(new EnableAllHandler(6,siteEnableAOCustomCheckBox6,settingsTable));
		siteEnableAOCustomCheckBox7.addClickHandler(new EnableAllHandler(7,siteEnableAOCustomCheckBox7,settingsTable));
		siteEnableAOCustomCheckBox8.addClickHandler(new EnableAllHandler(8,siteEnableAOCustomCheckBox8,settingsTable));
		siteEnableAOCustomCheckBox9.addClickHandler(new EnableAllHandler(9,siteEnableAOCustomCheckBox9,settingsTable));
		siteEnableAOCustomCheckBox10.addClickHandler(new EnableAllHandler(10,siteEnableAOCustomCheckBox10,settingsTable));
		siteEnableAOCustomCheckBox12.addClickHandler(new EnableAllHandler(11,siteEnableAOCustomCheckBox12,settingsTable));
		
		siteEnableNameBootStrapCustomCheckBox.addClickHandler(new EnableAllHandler(12,siteEnableNameBootStrapCustomCheckBox,settingsTable));
		siteEnableAddressBootStrapCustomCheckBox.addClickHandler(new EnableAllHandler(13,siteEnableAddressBootStrapCustomCheckBox,settingsTable));
		siteEnableLegalBootStrapCustomCheckBox.addClickHandler(new EnableAllHandler(14,siteEnableLegalBootStrapCustomCheckBox,settingsTable));
		siteEnableNameDerivationCustomCheckBox.addClickHandler(new EnableAllHandler(15,siteEnableNameDerivationCustomCheckBox,settingsTable));
		siteEnableOCRCustomCheckBox.addClickHandler(new EnableAllHandler(16,siteEnableOCRCustomCheckBox,settingsTable));
//		siteEnableLinkOnlyCustomCheckBox.addClickHandler(new EnableAllHandler(17,siteEnableLinkOnlyCustomCheckBox,settingsTable));
		siteEnableIncludeInTsrCustomCheckBox.addClickHandler(new EnableAllHandler(17,siteEnableIncludeInTsrCustomCheckBox,settingsTable));
		siteCheckAll.addClickHandler(new EnableAllHandler(18,siteCheckAll,settingsTable,4,EnableAllHandler.SETTINGS_CHECKALL));
		
		return enablepanel;
	}
	
	/**
	 * @return map with <commId,commName>
	 */
	public static Map<Integer, String> getAllCommunities() {
		return allCommunities;
	}

	public static void setAllCommunities(Map<Integer, String> allCommunities) {
		SearchSites.allCommunities = allCommunities;
	}

	
	public static native int getModuleType() /*-{
	return $wnd.getModuleType();
	}-*/;
	
	public static native int getCommId() /*-{
	return $wnd.getCommId();
	}-*/;

}
