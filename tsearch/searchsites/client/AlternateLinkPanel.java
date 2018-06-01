package ro.cst.tsearch.searchsites.client;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class AlternateLinkPanel extends VerticalPanel{
	String alternateLink="";
	String title ="";
	TextBox cDBox = null;
	
	public AlternateLinkPanel(String title,String alternateLink) {
		super();
		this.alternateLink = alternateLink;
		this.title = title;
		
		this.add(new HTML(title));
		
		HorizontalPanel firstPanel = new HorizontalPanel ();
		firstPanel.add(new Label(" Alternate link  : "));
		firstPanel.setSpacing(3);
		cDBox =  new TextBox( );
		cDBox.setWidth("200");
		cDBox.setText(alternateLink);
		firstPanel.add(cDBox);
		
		this.add(firstPanel);		
	}
	
	public String  getAlternateLink(){
		return cDBox.getText();
	}
}
