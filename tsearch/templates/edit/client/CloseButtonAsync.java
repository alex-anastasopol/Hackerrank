package ro.cst.tsearch.templates.edit.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;


/**
 * descrie ce se intampla cand a venit raspunsul dupa ce ai apasat Cancel button
 * si s-a efectuat apel RPC la 	deleteTemplate(...,false)
 * @author cristi
 */
public class CloseButtonAsync implements AsyncCallback{
	
	Label labelState = null;
	String templateName = "";
	private int templateId = -1;
	
	CloseButtonAsync(){}
	
	CloseButtonAsync(Label labelState,String templateName, int templateId){
		this.templateName=templateName;
		this.labelState=labelState;
		this.templateId = templateId;
	}
	
	public void onSuccess(Object result) {
		Boolean ret=(Boolean)result;
		
		//	apel RPC care seteaza state-ul 
		TemplateEditClient.templateService.getState(TemplateEditClient.getSearchId(),templateName,templateId,new FindStateAsync(labelState));
		
		if( ret.equals(Boolean.TRUE) ){
			//aici s-a efectuat apelul de Cancel cu succes si fisierul generat intermediar s-a sters
		}
		else{
			//aici nu s-a putut sterge cel generat intermediar pentru ca era modificat
		}
	}
	
	public void onFailure(Throwable caught) {
		//Window.alert("ERROR: cancel");
		if(caught instanceof TemplateSearchPermisionException){
			Window.alert(caught.getMessage());
			TemplateEditClient.refreshParent();
		}
	}
	
}