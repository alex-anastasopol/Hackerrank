package ro.cst.tsearch.searchsites.client;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class CommunitySitesFilter extends SitesFilter {
	
	public static final String COMMUNITY_LIST_BOX_NAME = "COMMUNITY_LIST_BOX";
	private SitesDataArived 	sda = null;
	private ListBox communityListBox;
	
	public ListBox getCommunityListBox() {
		return communityListBox;
	}

	public void setCommunityListBox(ListBox communityListBox) {
		this.communityListBox = communityListBox;
	}

	public CommunitySitesFilter(ListBox communityListBox,ListBox stateListBox, ListBox countyListBox, ListBox siteListBox, FlexTable flexTable) {
		super(stateListBox, countyListBox, siteListBox, flexTable);
		this.communityListBox = communityListBox;
		super.insert(communityListBox,0);
		communityListBox.setName(SitesFilter.COMMUNITY_LIST_BOX_NAME);
	}
	
	public CommunitySitesFilter(ListBox communityListBox,ListBox stateListBox, ListBox countyListBox, ListBox siteListBox, SitesDataArived sda) {
		super(stateListBox, countyListBox, siteListBox, null);
		this.sda = sda;
		this.communityListBox = communityListBox;
		VerticalPanel vpCommunity = new VerticalPanel();
		vpCommunity.add(new Label("Community:"));
		vpCommunity.add(communityListBox);
		super.insert(vpCommunity,0);
		communityListBox.setName(SitesFilter.COMMUNITY_LIST_BOX_NAME);
	}

	public String getSelectedCommunity(){
		return communityListBox.getValue(communityListBox.getSelectedIndex());
	}
	
	public String getSelectedCommunityLabel(){
		return communityListBox.getItemText(communityListBox.getSelectedIndex());
	}
	
	@Override
	public void updateCountyListBox(ListBox countyListBox) {
		super.updateCountyListBox(countyListBox);
		VerticalPanel vpCommunity = new VerticalPanel();
		vpCommunity.add(new Label("Community:"));
		vpCommunity.add(communityListBox);
		super.insert(vpCommunity,0);
	}
	
	public void addChangeListener(ChangeListener listener){
		super.addChangeListener(listener);
		communityListBox.addChangeListener(listener);
	}

	public SitesDataArived getSda() {
		return sda;
	}

	public void setSda(SitesDataArived sda) {
		this.sda = sda;
	}
	
	
}
