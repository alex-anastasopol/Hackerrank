package ro.cst.tsearch.templates.edit.client;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Widget;

class TimerSaveListener implements   KeyboardListener{
	
	
	private EditTemplateDialog edit = null;
	private boolean on = false;
	
	public TimerSaveListener (EditTemplateDialog dialod){
		this.edit = dialod;
	}
	
	public void onKeyDown(Widget arg0, char arg1, int arg2) {		
	}

	public void onKeyPress(Widget arg0, char arg1, int arg2) {
	}
	
	
	public void onKeyUp(Widget arg0, char arg1, int arg2) {
		//do not need synchronize we are in java script and have only one thread
		if( !on ){
			Timer t = new Timer() {
		      public void run() {
		    	edit.saveTemplate(false, false, null, "", true, false, false);
		        on = false;
		      }
		    };
		    //Window.alert(""+edit.getTimeout());
		    // Schedule the timer to run once in 5 seconds.
		    t.schedule(edit.getTimeout());
		    on =true;
		}
	}
	
}
