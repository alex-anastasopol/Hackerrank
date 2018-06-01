package ro.cst.tsearch.searchsites.client;

import java.util.Vector;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;

public class IncreaseDueOrPayDate implements ClickHandler {

	public static final int FIELD_DUE_DATE_YEAR_INCREASE = 1;
	public static final int FIELD_PAY_DATE_YEAR_INCREASE = 2;
	
	private FlexTable sitesTable ;
	private int fieldType;
	private int amount;
	
	public IncreaseDueOrPayDate(FlexTable sitesTable, int fieldType, int amount) {
		this.sitesTable = sitesTable;
		this.fieldType = fieldType;
		this.amount = amount;
	}
	
	@Override
	public void onClick(ClickEvent event) {
		SearchSites.getWaitingPopUP().show();
		
		Vector<String> siteData =  new Vector<String>();
		for (int row = 1;row<sitesTable.getRowCount();row++){
			String stateAbrev = ((Label)sitesTable.getWidget(row, 0)).getText();
			String countyName = ((CountyNamePanel)sitesTable.getWidget(row, 1)).getCountyName();
			String siteTypeAbrev = ((Label)sitesTable.getWidget(row, 2)).getText();
			
			siteData.add(stateAbrev + countyName + siteTypeAbrev);
		}
		
		SearchSites.searchSitesService.increaseDueOrPayDate(siteData, fieldType, amount, new AsyncCallback<Integer>() {
			
			@Override
			public void onSuccess(Integer result) {
				if(SearchSites.getWaitingPopUP().isVisible()){
					SearchSites.getWaitingPopUP().hide();
				}
				Window.alert("Data updated");
			}
			
			@Override
			public void onFailure(Throwable caught) {
				if(SearchSites.getWaitingPopUP().isVisible()){
					SearchSites.getWaitingPopUP().hide();
				}
				Window.alert(caught.getMessage());
				
			}
		});
		
		
		
		if (SearchSites.getWaitingPopUP().isVisible()){
			SearchSites.getWaitingPopUP().hide();
		}

	}

}
