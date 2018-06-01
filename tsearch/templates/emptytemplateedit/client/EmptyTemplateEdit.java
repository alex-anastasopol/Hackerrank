package ro.cst.tsearch.templates.emptytemplateedit.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class EmptyTemplateEdit {

	static final EmptyTemplateEditServiceAsync  templateService = (EmptyTemplateEditServiceAsync ) GWT.create(EmptyTemplateEditService.class);
	

	public void onModuleLoad() {

		//		asociez servlet-ul care sa raspunda la cererile RPC
		ServiceDefTarget endpoint = (ServiceDefTarget) templateService;
		endpoint.setServiceEntryPoint( "/title-search/EmptyTemplateEditServlet" );

		Button butSave = new Button("Save");
		butSave.addClickListener(new ButSaveListener());
		Button butDelete = new Button("Delete");
		butDelete.addClickListener(new ButDeleteListener());
		Button butClose = new Button("Close");
		butClose.addClickListener(new ButCloseListener());
		
		//		bloc de initializare style-uri pentru butoane
		{	
			butSave.setStyleName("button");
			butDelete.setStyleName("button");
			butClose.setStyleName("button");
		}
		
		RootPanel.get("TSD_slot_save").add(butSave);
		RootPanel.get("TSD_slot_delete").add(butDelete);
		RootPanel.get("TSD_slot_close").add(butClose);

		final AsyncCallback getTemplateContentAsync = new AsyncCallback(){
			public void onSuccess(Object result) {
				
				if(result!=null){		
					String str=result.toString();					
					tinyMceSetFocus();
					tinyMceInitEditorType();
					tinyMCEInsertContent(str);
					
				}
				else{
					Window.alert("Server returned null responce");
					tinyMCEInsertContent("");
				}
			}
			
			public void onFailure(Throwable caught) {
				//Window.alert("apel fara succes");
				if(caught instanceof EmptyTemplateEditException){
					Window.alert(caught.getMessage());
				}
				else{
					Window.alert("Error when contact server");
				}
				tinyMCEInsertContent("");
			}
		};
		
		new Timer() {
			public void run() {
				if(isReady()) {
					templateService.getTemplateContent(getTemplatePolicyId(), getTemplateContentAsync);
					cancel();
				}
			}
		}.scheduleRepeating(500);		
				
	}
	
	class TSDSaveButtonAsync implements AsyncCallback {

		public void onFailure(Throwable caught) {
			if(caught instanceof EmptyTemplateEditException){
				Window.alert(caught.getMessage());
			}
			else{
				Window.alert("Error when contact server");
			}
			destroy();
		}

		public void onSuccess(Object result) {
			Boolean boolResult = (Boolean)result;			
			if(boolResult.booleanValue()){
				Window.alert("File has been successfully modified!");
			}
			else{
				Window.alert("Error saving data ...");
			}
			
			destroy();
		}
		
	}
	
	class TSDDeleteButtonAsync implements AsyncCallback{
		
		public void onFailure(Throwable caught) {
			if(caught instanceof EmptyTemplateEditException){
				Window.alert(caught.getMessage());
			}
			else{
				Window.alert("Error when contact server");
			}
			destroy();
		}
		
		public void onSuccess(Object result) {
			Boolean boolResult = (Boolean)result;
			if(boolResult.booleanValue()){
				Window.alert("File has been deleted!");
			}
			else{
				Window.alert("Error deleting data ...");
			}
			destroy();
		}
	}

	class ButSaveListener implements ClickListener{
		
		public void onClick(Widget sender) {
			templateService.saveTemplate( getTemplatePolicyId(),getTemplateCommId(),getTemplatePolicyName(), getTemplateShortPolicyName(), tinyMCEGetContent(), new TSDSaveButtonAsync ());
		}
		
	}

	class ButCloseListener implements ClickListener{

		public void onClick(Widget sender) {			
			destroy();
		}
		
	}
	
	class ButDeleteListener implements ClickListener{

		public void onClick(Widget sender) {
			if (Window.confirm("Are you sure you want to delete "+getTemplatePolicyName()+" ?"))
				
			   templateService.deleteTemplate(getTemplateCommId(),getTemplatePolicyId(),new TSDDeleteButtonAsync());
		}
		
	}
	
	
	public static native void tinyMCEInsertContent(String content)/*-{
		$wnd.tinyMCEInsertContent(content);
	}-*/;
	
	public static native void destroy()/*-{
		$wnd.destroy();
	}-*/;
	
	public static native String tinyMCEGetContent()/*-{
		return $wnd.tinyMCEGetContent();
	}-*/;
	
	public static native void refreshParent()/*-{
		$wnd.refreshParent();
	}-*/;
	
	public static native void focusOnTextArea()/*-{
		$wnd.focusOnTextArea();
	}-*/;
	
	public static native  void tinyMceInitEditorType()/*-{
	$wnd.tinyMceInitEditorType();
}-*/;	
	
	public static native  void tinyMceSetFocus()/*-{
		$wnd.tinyMceSetFocus();
	}-*/;
		
	public static native  String  getTemplatePolicyName()/*-{
		return $wnd.getTemplatePolicyName();
	}-*/;
	
	public static native  int getTemplateCommId()/*-{
		return $wnd.getTemplateCommId();
	}-*/;
	
	public static native  int getTemplatePolicyId()/*-{
		return $wnd.getTemplatePolicyId();
	}-*/;
	
	public static native  String getTemplateShortPolicyName()/*-{
		return $wnd.getTemplateShortPolicyName();
	}-*/;
	
	public static native boolean isReady()/*-{
		return $wnd.isReady();
	}-*/;
}
