package ro.cst.tsearch.searchsites.client;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class AddressTokenPanel extends VerticalPanel{
	
	private  Grid gridTableW = null; 
	
	public  AddressTokenPanel (String title,String weight){
		this.add(new HTML(title));
		gridTableW = new Grid(2, 7);
		gridTableW.setWidget(0, 0, new Label("Number"));
		gridTableW.setWidget(0, 1, new Label("Predir"));
		gridTableW.setWidget(0, 2, new Label("Name"));
		gridTableW.setWidget(0, 3, new Label("Suffix"));
		gridTableW.setWidget(0, 4, new Label("Postdir"));
		gridTableW.setWidget(0, 5, new Label("#"));
		gridTableW.setWidget(0, 6, new Label("Unit"));
		TextBox txtNumber = new TextBox();
		TextBox txtPreDir = new TextBox();
		TextBox txtName = new TextBox();
		TextBox txtSuffix = new TextBox();
		TextBox txtPostdir = new TextBox();
		TextBox txtUnit = new TextBox();
		TextBox txtRange = new TextBox();
		double db[] = Util.createDoubleArrayFromString(weight);
		if (db.length >= 7) {
			txtNumber.setText(db[0] + "");
			txtPreDir.setText(db[1] + "");
			txtName.setText(db[2] + "");
			txtSuffix.setText(db[3] + "");
			txtPostdir.setText(db[4] + "");
			txtUnit.setText(db[5] + "");
			txtRange.setText(db[6] + "");
		}
		txtNumber.setVisibleLength(3);
		txtPreDir.setVisibleLength(3);
		txtName.setVisibleLength(3);
		txtSuffix.setVisibleLength(3);
		txtPostdir.setVisibleLength(3);
		txtUnit.setVisibleLength(3);
		txtRange.setVisibleLength(3);
		gridTableW.setWidget(1, 0, txtNumber);
		gridTableW.setWidget(1, 1, txtPreDir);
		gridTableW.setWidget(1, 2, txtName);
		gridTableW.setWidget(1, 3, txtSuffix);
		gridTableW.setWidget(1, 4, txtPostdir);
		gridTableW.setWidget(1, 5, txtUnit);
		gridTableW.setWidget(1, 6, txtRange);
		this.add(gridTableW);
	}

	public Grid getGridTableW() {
		return gridTableW;
	}

	public void setGridTableW(Grid gridTableW) {
		this.gridTableW = gridTableW;
	}
	
}
