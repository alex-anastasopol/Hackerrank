package ro.cst.tsearch.templates.edit.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;


/**
 * descrie ce se intampla cand a venit raspunsul dupa ce ai apasat Save button
 * si s-a efectuat apel RPC la 	saveTemplate(...)
 * @author cristi
 */
public class SaveButtonAsync implements AsyncCallback{
	private Label labelState;
	private String templateName="";
	private int templateId = -1;
	private boolean andClose = false;
	private boolean showMessage = false;
	private Button but = null;
	private String newButLabel ="";
	private boolean uploadToSSf;
	private boolean oneReceived;
	
	
	private SaveButtonAsync(){}
	
	SaveButtonAsync( Label labelState, String templateName, int templateId, boolean andClose, boolean changeButLabel,boolean uploadToSSf ,Button but, String newButLabel ){
		this.labelState=labelState;
		this.templateName=templateName;
		this.templateId = templateId;
		this.andClose = andClose;
		this.showMessage = changeButLabel;
		this.but = but;
		this.newButLabel = newButLabel;
		this.uploadToSSf = uploadToSSf;
	}
	
	public void onSuccess(Object result) {
		
		if(!oneReceived){
			oneReceived = true;
			//apel RPC care seteaza state-ul 
			TemplateEditClient.templateService.getState(TemplateEditClient.getSearchId(),templateName,templateId, new FindStateAsync(labelState));
		
			Boolean res = (Boolean) result;
			if ( res.equals( Boolean.TRUE ) ){
				//aici s-a efectuat apelul de salvare cu succes
				if(!andClose){
					//Window.alert("Template was automaticaly saved because of inactivity");
				}
				
				if(uploadToSSf){
					TemplateEditClient.templateService.uploadToSSf(TemplateEditClient.getSearchId(),TemplateEditClient.getCurrentUserId(),templateName, new AsyncCallback<Boolean>() {
	
						@Override
						public void onFailure(Throwable caught) {
							Window.alert(caught.getMessage());
						}
	
						@Override
						public void onSuccess(Boolean result) {
							if(result!=true){
								Window.alert("Could not upload to SSF. Please save the file and try uploading later !");
							}else{
								Window.alert("Success uploading file on SSF!");
							}
						}
							
					});
				}
				
				if(andClose){
					TemplateEditClient.destroy();
				}
				else if( showMessage ){
					if( but!=null ){
						EditTemplateDialog.enableButtons();
						if( newButLabel!=null ){
							but.setText(newButLabel);
						}
					}
				}
			}
			else{
				if( showMessage ){
					Window.alert("INFO: The file can't be saved");
					
					if( but!=null ){
						EditTemplateDialog.enableButtons();
						if( newButLabel!=null ){
							but.setText(newButLabel);
						}
					}
				}
			}
			
			
		}
	}
	
	public void onFailure(Throwable caught) {
		//Window.alert("ERROR: The file can't be saved \n"+caught.getMessage());
		if(caught instanceof TemplateSearchPermisionException){
			Window.alert(caught.getMessage());
			TemplateEditClient.refreshParent();
		}
	}
	
}


