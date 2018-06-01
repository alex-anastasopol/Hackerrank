package ro.cst.tsearch.searchsites.client;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class TaxDatesPanel extends VerticalPanel{
	TextBox pDBox = null;
	TextBox dDBox = null;
	ListBox taxYearModeBox = null;
	
	public TaxDatesPanel(String title,String dueDate, String payDate, int taxYearMode) {
		
		this.add(new HTML(title));
		HorizontalPanel firstPanel = new HorizontalPanel ();
		firstPanel.add(new Label(" PD:"));
		pDBox =  new TextBox( );
		pDBox.setText(payDate);
		firstPanel.add(pDBox);
		
		HorizontalPanel secondPanel = new HorizontalPanel ();
		secondPanel .add(new Label(" DD:"));
		dDBox =  new TextBox( );
		dDBox.setText(dueDate);
		secondPanel.add(dDBox);
		
		HorizontalPanel thirdPanel = new HorizontalPanel ();
		thirdPanel.add(new Label(" Tax Year Mode:"));
		taxYearModeBox =  new ListBox();
		taxYearModeBox.addItem("PD_YEAR-1", Integer.toString(TaxSiteData.TAX_YEAR_PD_YEAR_MINUS_1));
		taxYearModeBox.addItem("PD_YEAR", Integer.toString(TaxSiteData.TAX_YEAR_PD_YEAR));
		taxYearModeBox.addItem("PD_YEAR+1", Integer.toString(TaxSiteData.TAX_YEAR_PD_YEAR_PLUS_1));
		if(taxYearMode == TaxSiteData.TAX_YEAR_PD_YEAR) {
			taxYearModeBox.setItemSelected(1, true);
		} else if(taxYearMode == TaxSiteData.TAX_YEAR_PD_YEAR_MINUS_1) {
			taxYearModeBox.setItemSelected(0, true);
		} else {
			taxYearModeBox.setItemSelected(2, true);
		}
		thirdPanel.add(taxYearModeBox);
		
		this.add(firstPanel);
		this.add(secondPanel);
		this.add(thirdPanel);
	}
	
	public String  getDueDate(){
		return dDBox.getText();
	}
	
	public String  getPayDate(){
		return pDBox.getText();
	}
	
	public int  getTaxYearMode(){
		String value = taxYearModeBox.getValue(taxYearModeBox.getSelectedIndex());
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return TaxSiteData.TAX_YEAR_PD_YEAR;
	}
}
