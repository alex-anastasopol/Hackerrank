package ro.cst.tsearch.searchsites.client;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ConnTypePanel extends VerticalPanel{
	
	ListBox cTBox = null;

	public ConnTypePanel(String title,int connType) {
		super();
	
		this.add(new HTML(title));
		
		HorizontalPanel firstPanel = new HorizontalPanel ();
		firstPanel.add(new Label(" Connection Type:"));
		cTBox =  new ListBox( );
	
		cTBox.addItem("Http Connection 1",new Integer(GWTDataSite.HTTP_CONNECTION).toString());
		cTBox.addItem("Http Connection 2",new Integer(GWTDataSite.HTTP_CONNECTION_2).toString());
		cTBox.addItem("Http Connection 3",new Integer(GWTDataSite.HTTP_CONNECTION_3).toString());
		cTBox.setItemSelected(connType-2, true);
		
		cTBox.setVisibleItemCount(1);
		firstPanel.add(cTBox);
		
		this.add(firstPanel);		
	}
	
	public int  getConnType(){
		return cTBox.getSelectedIndex()+2;
	}
	
}
