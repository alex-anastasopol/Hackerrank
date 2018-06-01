package ro.cst.tsearch.searchsites.client;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class AssessorTaxCommonPanel extends VerticalPanel {
	TextBox numberOfYears = new TextBox();

	public AssessorTaxCommonPanel(String title, int noOfYears) {
		super();
		HTML label = new HTML(title);
		this.add(label);

		HorizontalPanel firstPanel = new HorizontalPanel();
		firstPanel.add(new Label("  Number of Years:      "));
		numberOfYears =  new TextBox( );
		numberOfYears.setWidth("20");
		numberOfYears.setText(Integer.toString(noOfYears));
		firstPanel.add(numberOfYears);

		this.add(firstPanel);

	}

	public int getNumberOfYears(){
		String value = numberOfYears.getText();
		
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return TaxSiteData.NUMBER_OF_TAX_YEARS;
	}
	
	public void setNumberOfYears(int noOfYears){
		numberOfYears.setText(Integer.toString(noOfYears));
	}
}
