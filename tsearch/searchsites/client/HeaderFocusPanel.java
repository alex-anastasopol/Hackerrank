package ro.cst.tsearch.searchsites.client;

import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

public class HeaderFocusPanel  extends FocusPanel {
	private HTML myTextBox = null;
	private boolean showMessage = false;
	PopupPanel pl  = null;
	int left = 20;
	int top =20;
	//String text = "";
	
	public HeaderFocusPanel(String text, final String messageOnMouseEnter,  final boolean showMessage) {
		super();
	//	this.text = text;
		myTextBox = new HTML(text);
		
		this.add(myTextBox);
		
		super.addMouseListener(new MYMouseListener(this));
		
		 pl = new PopupPanel(true);
		 pl.add(new HTML("<font color='red'>"+"<b>"+messageOnMouseEnter+"</b>"+"</font>"));
		
		 this.showMessage = showMessage;
	}
	
	public HeaderFocusPanel(String text, final String messageOnMouseEnter,  final boolean showMessage,int left,int top) {
		super();
		this.left = left;
		this.top = top;
		myTextBox = new HTML(text);
		//this.text = text;
		this.add(myTextBox);
		
		super.addMouseListener(new MYMouseListener(this));
		
		 pl = new PopupPanel(true);
		 pl.add(new HTML("<font color='red'>"+"<b>"+messageOnMouseEnter+"</b>"+"</font>"));
		
		 this.showMessage = showMessage;
	}
	
	public void setHtmlTex(String text){
		myTextBox = new HTML(text);
		this.clear();
		this.add(myTextBox);
	}
	
	private class MYMouseListener implements MouseListener{
		
		HeaderFocusPanel textBox;
		
		public void onMouseDown(Widget arg0, int arg1, int arg2) {
			//pl.hide();
		}

		public void onMouseEnter(Widget arg0) {
			if(showMessage){
				
				pl.setPopupPosition(textBox.getAbsoluteLeft()-left, textBox.getAbsoluteTop()-top);
				pl.show();
			
			}
			}

		public void onMouseLeave(Widget arg0) {
			pl.hide();
		}

		public void onMouseMove(Widget arg0, int arg1, int arg2) {
			//pl.hide();
		}

		public void onMouseUp(Widget arg0, int arg1, int arg2) {
			//pl.hide();
		}

		public MYMouseListener(HeaderFocusPanel checkBox) {
			super();
			this.textBox = checkBox;
		}
	}

	public HTML getMyHTML() {
		return myTextBox;
	}

	public boolean isShowMessage() {
		return showMessage;
	}

	public void setShowMessage(boolean showMessage) {
		this.showMessage = showMessage;
	}
	
	public void setHTML(String txt) {
		myTextBox.setHTML(txt);
	}

	
}
