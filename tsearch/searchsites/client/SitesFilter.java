package ro.cst.tsearch.searchsites.client;


import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class SitesFilter extends HorizontalPanel{
	
	public static final String COMMUNITY_LIST_BOX_NAME = "COMMUNITY_LIST_BOX";
	public static final String STATE_LIST_BOX_NAME = "STATE_LIST_BOX";
	public static final String COUNTY_LIST_BOX_NAME = "COUNTY_LIST_BOX";
	public static final String SITE_LIST_BOX_NAME = "SITE_LIST_BOX";
	
	ListBox stateListBox1 = null;
	ListBox countyListBox1 = null;
	ListBox siteListBox = null;
	FlexTable 	flexTable = null;
	ChangeListener listener = null;
	
	public SitesFilter(ListBox stateListBox, ListBox countyListBox,final  ListBox siteListBox, FlexTable flexTable) {
		super();
		this.stateListBox1 = stateListBox;
		this.countyListBox1 = countyListBox;
		this.siteListBox = siteListBox;
		VerticalPanel vpState = new VerticalPanel();
		vpState.add(new Label("State:"));
		vpState.add(stateListBox);
		super.add(vpState);
		VerticalPanel vpCounty = new VerticalPanel();
		vpCounty.add(new Label("County:"));
		vpCounty.add(countyListBox);
		super.add(vpCounty);
		VerticalPanel vpDs = new VerticalPanel();
		vpDs.add(new Label("DS:"));
		vpDs.add(siteListBox);
		super.add(vpDs);
		this.flexTable = flexTable;
		stateListBox.setName(SitesFilter.STATE_LIST_BOX_NAME);
		countyListBox.setName(SitesFilter.COUNTY_LIST_BOX_NAME);
		siteListBox.setName(SitesFilter.SITE_LIST_BOX_NAME);
	}

	
	
	public void updateCountyListBox(ListBox countyListBox){
		this.countyListBox1 = countyListBox;
		super.clear();
		VerticalPanel vpState = new VerticalPanel();
		vpState.add(new Label("State:"));
		vpState.add(stateListBox1);
		super.add(vpState);
		VerticalPanel vpCounty = new VerticalPanel();
		vpCounty.add(new Label("County:"));
		vpCounty.add(countyListBox1);
		super.add(vpCounty);
		VerticalPanel vpDs = new VerticalPanel();
		vpDs.add(new Label("DS:"));
		vpDs.add(siteListBox);
		super.add(vpDs);
			
		this.countyListBox1.setName(SitesFilter.COUNTY_LIST_BOX_NAME);
		this.countyListBox1.addChangeListener(listener );
	}
	
	public SitesFilter(FlexTable flexTable){
		this.flexTable = flexTable;
	}
	
	public void addChangeListener(ChangeListener listener){
		stateListBox1.addChangeListener(listener );
		countyListBox1.addChangeListener(listener );
		siteListBox.addChangeListener(listener );
		this.listener = listener;
	}
	

	
	public String getSelectedState(){
		return stateListBox1.getItemText(stateListBox1.getSelectedIndex());
	}
	
	public ListBox getStateListBox(){
		return stateListBox1;
	}
	
	public String getSelectedCounty(){
		return countyListBox1.getItemText(countyListBox1.getSelectedIndex());
	}
	
	public String getSelectedType(){
		return siteListBox.getItemText(siteListBox.getSelectedIndex());
	}
	
	public FlexTable getSitesTable(){
		return flexTable;
	}

	public ListBox getCountyListBox() {
		return countyListBox1;
	}

	public void setCountyListBox(ListBox countyListBox) {
		this.countyListBox1 = countyListBox;
	}
	
	public ListBox getSiteListBox(){
		return this.siteListBox;
	}
	
}
