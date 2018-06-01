package ro.cst.tsearch.searchsites.client;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FocusPanel;

public class FocusCheckBoxPanel  extends FocusPanel {
	
	private CheckBox myCheckBox = null;
//	private boolean showMessage = false;
//	PopupPanel pl  = null;
	
	public FocusCheckBoxPanel(boolean ischecked, final String messageOnMouseEnter) {
		super();
//		super.setTitle(name);
		super.setTitle(messageOnMouseEnter);
		 myCheckBox = new CheckBox();
		 myCheckBox.setValue(ischecked);
		this.add(myCheckBox);
		
//		super.addMouseListener(new MYMouseListener(this));
		
//		 pl = new PopupPanel(true);
//		 
//		 pl.add(new HTML("<div style=\"display: block; background-color: white; border: thin solid black; color: black; padding: 1px;\">" +
//		 		"<font color='red'>"+"<b>"+messageOnMouseEnter+"</b>"+"</font></div>"));
		
//		 this.showMessage = showMessage;
	}

	public void setChecked(boolean isChecked){
		
		myCheckBox.setValue(isChecked);
	}

	
//	private class MYMouseListener implements MouseListener{
//		
//		FocusCheckBoxPanel checkBox;
//		
//		public void onMouseDown(Widget arg0, int arg1, int arg2) {
//			//pl.hide();
//		}
//
//		public void onMouseEnter(Widget arg0) {
//			if(showMessage){
//				pl.setPopupPosition(checkBox.getAbsoluteLeft()-10, checkBox.getAbsoluteTop()-15);
//				pl.show();
//				clearSelection();
//			}
//			}
//
//		public void onMouseLeave(Widget arg0) {
//			pl.hide();
//		}
//
//		public void onMouseMove(Widget arg0, int arg1, int arg2) {
//		//	pl.hide();
//		}
//
//		public void onMouseUp(Widget arg0, int arg1, int arg2) {
//			//pl.hide();
//		}
//
//		public MYMouseListener(FocusCheckBoxPanel checkBox) {
//			super();
//			this.checkBox = checkBox;
//		}
//	}

	public CheckBox getMyCheckBox() {
		return myCheckBox;
	}
	
	public boolean isChecked() {
		return myCheckBox.isChecked();
	}
	

//	public boolean isShowMessage() {
//		return showMessage;
//	}
//
//	public void setShowMessage(boolean showMessage) {
//		this.showMessage = showMessage;
//	}
	
	public static native void clearSelection() /*-{
	try {
		if ($doc.selection) {
			$doc.selection.empty();
		}
		else if ($wnd.getSelection) {
			$wnd.getSelection().removeAllRanges();
		}
	}catch(e) {
		
	}
	}-*/;
	
}
