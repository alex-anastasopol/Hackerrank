package ro.cst.tsearch.templates.edit.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;

/**
 * descrie ce se intampla dupa un apel RPC 
 * la functia getState(...)
 * @author cristi
 */
class FindStateAsync implements AsyncCallback{
	
	Label labelState= null ;
	
	FindStateAsync(){
	}
	
	FindStateAsync(Label labelState){
		this.labelState=labelState;
	}
	
	public void onSuccess( Object result ) {
		String str=(String)result;
		//setez starea curenta
		labelState.setText(str);		
	}
	
	public void onFailure(Throwable caught) {
		//Window.alert("ERROR: can't find the template state");
	}
}