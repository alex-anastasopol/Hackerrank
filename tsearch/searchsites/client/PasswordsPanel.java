package ro.cst.tsearch.searchsites.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class PasswordsPanel extends VerticalPanel{
	
	
	//table passwords
    FlexTable flexPassTable = new FlexTable();
	
    //
    FlexTable nameFlexTable = new FlexTable();
	
    ListBox allCodesList =  new ListBox();
    {
    	allCodesList .setVisibleItemCount(1);
    }
    
    Button but = new Button("Add");
    {
    	but.setStyleName("button");
    }
    
    Button butRemove = new Button("Remove");
    {
    	butRemove.setStyleName("button");
    }
    
    HashMap allPasswords = null;
	
    TextBox keyTxt = new TextBox();
    TextBox valueTxt = new TextBox();
    
    int selectedPassCode = 0;
	
	PasswordsPanel(String siteName,HashMap allPasswords,Vector allPassCodes,int selectedPassCode){
		
		//Window.alert("ada");
		this.allPasswords = allPasswords;
		
		allCodesList.addItem("___"/*siteName*/);
		
		if(allPassCodes.contains(siteName)){
			allPassCodes .remove(siteName);
			this.selectedPassCode = selectedPassCode;
		}
		else if(allPassCodes.size()>0){
			this.selectedPassCode = selectedPassCode + 1 ;
		}
		
		if(selectedPassCode<0){
			this.selectedPassCode = 0;
		}
		
		for(int i=0;i<allPassCodes.size();i++){
			allCodesList.addItem((String)allPassCodes.get(i));
		}
		
		//not yet implemented
		keyTxt.setEnabled(false);
		valueTxt .setEnabled(false);
		allCodesList.setEnabled(false);
//		not yet implemented
		but.setEnabled(false);
		butRemove.setEnabled(false);
		
		allCodesList.setSelectedIndex(this.selectedPassCode);
		
		nameFlexTable.setWidget(0, 0,new Label("Passwords inheritance:") );
		nameFlexTable.setWidget(0, 1, allCodesList);
		nameFlexTable.setWidget(1, 0, new Label("Key") );
		nameFlexTable.setWidget(1, 1, new Label("Value") );
		
		nameFlexTable.setWidget(2, 0, keyTxt );
		
		nameFlexTable.setWidget(2, 1, valueTxt );
		nameFlexTable.setWidget(2, 2, but );
		nameFlexTable.setWidget(2, 3, butRemove );
		
		Set  allkeys = allPasswords.keySet();
		Iterator  it = allkeys .iterator();
		int i=0;
		while(it.hasNext()){
			String key = (String)it.next();
			String value = (String)allPasswords.get(key);
			flexPassTable.setWidget(i, 0, new Label(key));
			flexPassTable.setWidget(i, 1, new Label(value));
			i++;
		}
		
		this.add(nameFlexTable);
		this.add(flexPassTable);
		
		but.addClickListener(new ClickListener(){

			public void onClick(Widget arg0) {
				int count = flexPassTable.getRowCount();
				String key = keyTxt.getText();
				String value = valueTxt.getText();
				if(key!=null&&value!=null){
					if((!"".equals(key))&&(!"".equals(value))){
						flexPassTable.setWidget(count, 0, new Label(key));
						flexPassTable.setWidget(count, 1, new Label(value));
					}
				}
				Window.alert("Warning:  Save Not Yet Implemented for passwords");
			}
			
		});
		
		
		
	}
}