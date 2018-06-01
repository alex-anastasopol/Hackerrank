package ro.cst.tsearch.searchsites.client;

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

public class DoctypePanel extends VerticalPanel{
	
	
	//table doctypes
    FlexTable flexDocssTable = new FlexTable();
	
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
	
    TextBox keyTxt = new TextBox();
    TextBox valueTxt = new TextBox();
    
    int selectedDoctypeCode = 0;
	
	DoctypePanel(String siteName,Vector C,Vector allPassCodes,int selectedDoctypeCode){
		
		allCodesList.addItem("___"/*siteName*/);
		
		if(allPassCodes.contains(siteName)){
			allPassCodes .remove(siteName);
			this.selectedDoctypeCode = selectedDoctypeCode;
		}
		else if(allPassCodes.size()>0){
			this.selectedDoctypeCode = selectedDoctypeCode + 1 ;
		}
		
		if(selectedDoctypeCode<0){
			this.selectedDoctypeCode = 0;
		}
		
		for(int i=0;i<allPassCodes.size();i++){
			allCodesList.addItem((String)allPassCodes.get(i));
		}
		
		allCodesList.setSelectedIndex(this.selectedDoctypeCode);
		
		nameFlexTable.setWidget(0, 0,new Label("DocType inheritance:") );
		nameFlexTable.setWidget(0, 1, allCodesList);
		nameFlexTable.setWidget(1, 0, new Label("Doctype Name") );
		//nameFlexTable.setWidget(1, 1, new Label("Value") );
		nameFlexTable.setWidget(2, 0, keyTxt );
		//nameFlexTable.setWidget(2, 1, valueTxt );
		nameFlexTable.setWidget(2, 1, but );
		nameFlexTable.setWidget(2, 2, butRemove );
		
		keyTxt.setEnabled(false);
		but.setEnabled(false);
		allCodesList.setEnabled(false);
		butRemove.setEnabled(false);
		/*Set  allkeys = allPasswords.keySet();
		Iterator  it = allkeys .iterator();
		int i=0;
		while(it.hasNext()){
			String key = (String)it.next();
			String value = (String)allPasswords.get(key);
			flexPassTable.setWidget(i, 0, new Label(key));
			flexPassTable.setWidget(i, 1, new Label(value));
			i++;
		}*/
		
		this.add(nameFlexTable);
		this.add(flexDocssTable);
		
		but.addClickListener(new ClickListener(){

			public void onClick(Widget arg0) {
				int count = flexDocssTable.getRowCount();
				String key = keyTxt.getText();
				String value = valueTxt.getText();
				if(key!=null&&value!=null){
					if((!"".equals(key))&&(!"".equals(value))){
						flexDocssTable.setWidget(count, 0, new Label(key));
						flexDocssTable.setWidget(count, 1, new Label(value));
					}
				}
				Window.alert("Warning:  Save Not Yet Implemented for passwords");
			}
			
		});
	}
	
}