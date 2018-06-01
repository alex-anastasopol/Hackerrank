package ro.cst.tsearch.searchsites.client;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class CityNamePanel extends VerticalPanel {
	TextBox tb = new TextBox();

	public CityNamePanel(String title, String cityName) {
		super();
		HTML label = new HTML(title);
		this.add(label);

		HorizontalPanel panel = new HorizontalPanel();
		panel.add(new Label("City:"));

		tb.setText(cityName);

		panel.add(tb);

		this.add(panel);

	}

	public void setCityName(String name) {
		tb.setName(name);
	}

	public String getCityName() {
		return tb.getText();
	}
}
