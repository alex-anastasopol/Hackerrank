package ro.cst.tsearch.searchsites.client;

import java.util.HashMap;
import java.util.Vector;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class AdvancedDialog extends DialogBox {

	private String addressTokenWeight = "";
	private String addressTokenMiss = "";
	private String dueDate =null;
	private String payDate =null;
	private String countyName;
	private String cityName;
	private String siteType;
	private String stateAbrev;
	private String alternateLink =null;
	private boolean isRoLikeSite = false;
	private int  connType = GWTDataSite.HTTP_CONNECTION_3;
	private int taxYearMode = TaxSiteData.TAX_YEAR_PD_YEAR;
	private int numberOfYears = 0;
			
	public void show(){
		
		VerticalPanel vertPanel = new VerticalPanel();

		Vector<String> allSitesNames = SearchSites.getAllSitesNames(stateAbrev, countyName);
		int specialPoz = allSitesNames.indexOf(stateAbrev + countyName+ siteType);

		// create and add automatic order panel
		final AutomaticOrderPanel automaticOrderPanel = new AutomaticOrderPanel(SearchSites.getAllSites(stateAbrev, countyName), specialPoz);
		automaticOrderPanel.setSpacing(5);
		vertPanel.add(new HTML("<br/><b>&nbsp;&nbsp;Set automatic search order:</b>"));
		vertPanel.add(automaticOrderPanel);

		// create address token Weights panel
		final AddressTokenPanel panelW= new AddressTokenPanel("<br/><b>&nbsp;&nbsp;Adrees Token Weights:</b>", addressTokenWeight);
		vertPanel.add(panelW);
		
		//create Address Token Missing Scores panel
		final AddressTokenPanel panelM=  new AddressTokenPanel("<b>&nbsp;&nbsp;Address Token Missing Scores:</b>", addressTokenMiss);
		vertPanel.add(panelM);
		
		final AlternateLinkPanel alp = new AlternateLinkPanel(" <br/><b>&nbsp;&nbsp;Alternate type ",alternateLink);
		vertPanel.add(alp);
		
		//create Connection Type panel
		final ConnTypePanel connTypePanel=  new ConnTypePanel("<br/><b>&nbsp;&nbsp;Connection Type:</b>", connType);
		vertPanel.add(connTypePanel);
		
		//create city text area
		CityNamePanel tempCityNamePanel = null;
		if (cityName != null && (!Util.isEmpty(siteType)&& siteType.startsWith("Y"))){
			tempCityNamePanel = new CityNamePanel ("<b>City</b>", cityName);			
			vertPanel.add(tempCityNamePanel);
		}
		final CityNamePanel cityNamePanel = tempCityNamePanel;
		
		//create DD and DP dates panel
		TaxDatesPanel tempTaxPanel=null;
		if(dueDate!=null && payDate!=null){ //taxSite
			tempTaxPanel= new TaxDatesPanel ("<br/><b>&nbsp;&nbsp;Tax Dates:</b>",dueDate,payDate, taxYearMode);
			vertPanel.add(tempTaxPanel);
		}
		
		final TaxDatesPanel taxPanel= tempTaxPanel;
		
		//create AssessorTaxCommon that contains common settings for Assessor and Tax sites
		AssessorTaxCommonPanel tempAtcPanel = null;
		if (numberOfYears > 0){
			tempAtcPanel  = new AssessorTaxCommonPanel("<br/><b>&nbsp;&nbsp;Assessor/Tax Options:</b>", numberOfYears);
			vertPanel.add(tempAtcPanel);
		}
		
		final AssessorTaxCommonPanel atcPanel = tempAtcPanel;
		
		vertPanel.add(new HTML("<br/><b>&nbsp;&nbsp;Proxy settings</b>"));
		FlexTable tableProxy = new FlexTable();
		tableProxy.setWidget(0, 0, new Label("Enable proxy"));
		CheckBox boxProxy = new CheckBox();
		boxProxy.setValue(true);
		boxProxy.setEnabled(false);
		tableProxy.setWidget(0, 1, boxProxy);
		tableProxy.setWidget(1, 0, new Label("IP"));
		tableProxy.setWidget(1, 1, new Label("Port"));
		TextBox txtproxy = new TextBox();
		txtproxy.setEnabled(false);
		txtproxy.setVisibleLength(12);
		TextBox txtproxyport = new TextBox();
		txtproxyport.setEnabled(false);
		txtproxyport.setVisibleLength(3);

		tableProxy.setWidget(2, 0, txtproxy);
		tableProxy.setWidget(2, 1, txtproxyport);
		vertPanel.add(tableProxy);

		vertPanel.add(new HTML("<br/><b>&nbsp;&nbsp;Doctype settings:</b>"));
		DoctypePanel doctypePanel = new DoctypePanel(stateAbrev + countyName
				+ siteType, new Vector(), new Vector(), 0);
		vertPanel.add(doctypePanel);

		vertPanel.add(new HTML("<br/><b>&nbsp;&nbsp;Password settings:</b>"));
		HashMap map = new HashMap();
		Vector vec = new Vector();
		PasswordsPanel passwordsPanel = new PasswordsPanel(stateAbrev
				+ countyName + siteType, map, vec, 0);
		vertPanel.add(passwordsPanel);
		vertPanel.add(new HTML("<br/></br>"));

		DockPanel docPanel = new DockPanel();
		Button saveBut = new Button("Save");
		saveBut.addClickHandler(new ClickHandler() {

			public void onClick(ClickEvent arg0) {
				String newSiteNamesOrder[] = automaticOrderPanel
						.getSitesNamesOrdered();

				StringBuffer bufW = new StringBuffer("");
				StringBuffer bufWm = new StringBuffer("");

				for (int i = 0; i < 7; i++) {

					String a = "1.0";
					String b = "0.0";

					try {
						a = Double.parseDouble(((TextBox) panelW.getGridTableW().getWidget(1, i)).getText())+ "";
					} catch (Exception e) {
					}
					try {
						b = Double.parseDouble(((TextBox) panelM.getGridTableW().getWidget(1, i)).getText())+ "";
					} catch (Exception e) {
					}

					bufW.append("," + a);
					bufWm.append("," + b);
				}

				String newAdressToken = bufW.toString().substring(1);
				String newAdressTokenMiss = bufWm.toString().substring(1);

				AdvancedDataStruct struct = new AdvancedDataStruct();
				struct.stateAbbreviation = stateAbrev;
				struct.countyName = countyName;
				struct.siteTypeAbbreviation = siteType;
				struct.adressToken = newAdressToken;
				struct.adressTokenMiss = newAdressTokenMiss;
				struct.siteNameOrder = newSiteNamesOrder;
				struct.alternateLink = alp.getAlternateLink();
				struct.connType = connTypePanel.getConnType();
				if(cityNamePanel!=null){
					struct.cityName = cityNamePanel.getCityName(); 
				}
				if(taxPanel!=null){
					if(taxPanel.getDueDate() == null || "".equals(taxPanel.getDueDate())){
						struct.dueDate = "*";
					} else {
						struct.dueDate = taxPanel.getDueDate();
					}
					
					if(taxPanel.getPayDate() == null || "".equals(taxPanel.getPayDate())){
						struct.payDate = "*";
					} else {
						struct.payDate = taxPanel.getPayDate();
					}
					struct.taxYearMode = taxPanel.getTaxYearMode();
				}
				if(atcPanel != null){
					struct.numberOfYears = atcPanel.getNumberOfYears(); 
				}
				
				SearchSites.searchSitesService.applyAdvancedChanges(struct,new AsyncCallback() {

							public void onFailure(Throwable arg0) {
								if (SearchSites.getWaitingPopUP().isVisible()) {
									SearchSites.getWaitingPopUP().hide();
								}
								Window.alert(arg0.getMessage());
							}

							public void onSuccess(Object arg0) {
								// Window.alert("Data updated");
								if (SearchSites.getWaitingPopUP().isVisible()) {
									SearchSites.getWaitingPopUP().hide();
								}
								SearchSites.init();
								AdvancedDialog.this.hide();
							}
						});
			}


		});
		saveBut.setStyleName("button");
		docPanel.add(saveBut, DockPanel.EAST);
		Button closeBut = new Button("Close");
		closeBut.setStyleName("button");
		closeBut.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent arg0) {
				AdvancedDialog.this.hide();
			}
		});
		docPanel.add(closeBut, DockPanel.WEST);
		vertPanel.add(docPanel);
		setWidget(vertPanel);
		super.show();
	}
	
	public AdvancedDialog(String stateAbrev, String countyName,
			String siteType, String cityName,
			String addressTokenWeight, String addressTokenMiss, int x, int y) {
		super();
		this.addressTokenMiss = addressTokenMiss;
		this.addressTokenWeight = addressTokenWeight;
		this.countyName = countyName;
		this.siteType = siteType;
		this.stateAbrev = stateAbrev;
		this.cityName = cityName;
		setPopupPosition(x, y);
		// Set the dialog box's caption.
		setText("Advanced settings for " + stateAbrev + countyName + siteType);
	}

	public String getDueDate() {
		return dueDate;
	}

	public void setDueDate(String dueDate) {
		this.dueDate = dueDate;
	}

	public String getPayDate() {
		return payDate;
	}

	public void setPayDate(String payDate) {
		this.payDate = payDate;
	}

	public boolean getIsRoLikeSite() {
		return isRoLikeSite;
	}

	public void setIsRoLikeSite(boolean isRoLikeSite) {
		this.isRoLikeSite = isRoLikeSite;
	}
	
	public int getConnType() {
		return connType;
	}

	public void setConnType(int connType) {
		this.connType = connType;
	}

	public String getAlternateLink() {
		return alternateLink;
	}

	public void setAlternateLink(String alternateLink) {
		this.alternateLink = alternateLink;
	}
	
	public String getCityName() {
		return cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public int getTaxYearMode() {
		return taxYearMode;
	}

	public void setTaxYearMode(int taxYearMode) {
		this.taxYearMode = taxYearMode;
	}

	public int getNumberOfYears() {
		return numberOfYears;
	}

	public void setNumberOfYears(int noOfYears) {
		this.numberOfYears = noOfYears;
	}
}
