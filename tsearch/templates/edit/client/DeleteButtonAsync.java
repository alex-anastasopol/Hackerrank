package ro.cst.tsearch.templates.edit.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;

/**
 * descrie ce se intampla cand a venit raspunsul dupa ce ai apasat Delete button
 * si s-a efectuat apel RPC la 	deleteTemplate(...,true)
 * @author cristi
 */
class DeleteButtonAsync implements AsyncCallback{
	
	Label labelState;
	String templateName="";
	private int templateId = -1;
	
	DeleteButtonAsync(){}
	
	DeleteButtonAsync(Label labelState,String templateName, int templateId){
		this.labelState=labelState;
		this.templateName=templateName;
		this.templateId = templateId;
	}
	
	public void onSuccess(Object result) {
		
		Boolean ret=(Boolean)result;
		
		//	apel RPC care seteaza state-ul 
		TemplateEditClient.templateService.getState(TemplateEditClient.getSearchId(),templateName,templateId,new FindStateAsync(labelState));
		
		if(ret.equals(Boolean.TRUE)){
			//aici s-a efectuat apelul Delete cu succes si fisierul generat intermediar s-a sters indiferent de starea lui 
		}
		else{
			//Window.alert("Can't delete file, but reset the state");
		}
		
		TemplateEditClient.destroy();
	}
	
	public void onFailure(Throwable caught) {
		//Window.alert("Error: can't delete the modified file");
		if(caught instanceof TemplateSearchPermisionException){
			Window.alert(caught.getMessage());
			TemplateEditClient.refreshParent();
		}
	}		 
}