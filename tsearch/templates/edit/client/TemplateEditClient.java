package ro.cst.tsearch.templates.edit.client;

import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowCloseListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RichTextAreaWithSpellChecker;
import com.google.gwt.user.client.ui.RootPanel;
import com.gwt.utils.client.UtilsAtsGwt;

public class TemplateEditClient  implements EntryPoint{


	public static boolean isSearchTakeByOther = false;
	
	public static String ATS_CURRENT_SERVER_LINK_FOR_DOC_RETREIVER = null;
	
	private String content = "";
	private String path   = ""; 
	private String templateId = "";
    private TemplateInfoResultG templateInfo = null;
    AsyncCallback<TemplateInfoResultG> getTemplateInfoCallback;
	/**
	 * obiectul prin care pot accesa STUB-ul generat de GWT , folosit  pentru a apela metodele RPC
	 */
	static final TemplateEditServiceAsync templateService = (TemplateEditServiceAsync ) GWT.create(TemplateEditService.class);

	class TemplateWindowCloseListener implements WindowCloseListener{
		public void onWindowClosed() {	
		}

		//if we close the window and make refresh to the parent
		public String onWindowClosing() {
			String templateName =  com.google.gwt.user.client.Window.Location.getParameter("name");
			String templateIdStr =  com.google.gwt.user.client.Window.Location.getParameter("templateId");
			int templateId = -1;
			try {
				templateId = Integer.parseInt(templateIdStr); 
			} catch (NumberFormatException nfe) {}
			if (templateId!=-1) {
				refreshTemplateState(templateName, templateId);
			}
			//refreshParent();
			return null;
		}
	}	
	public void onModuleLoad() {	

		Window.addWindowCloseListener(new TemplateWindowCloseListener() );
		//Window.alert("adaaaaa");
		final HTML labelload = new HTML(TemplateUtils.LOADING_TEMPLATE_TEXT);
		
		RootPanel.get("slotLoad").add(labelload);
		//asociez servlet-ul care sa raspunda la cererile RPC
		ServiceDefTarget endpoint = (ServiceDefTarget) templateService;
		endpoint.setServiceEntryPoint( "/title-search/TemplatesServlet" );
		
		// initializez interfata de editare
		getTemplateInfoCallback = new AsyncCallback<TemplateInfoResultG>() {
			public void onSuccess(TemplateInfoResultG result) {
				templateInfo = result;
				path = templateInfo.getTemplateNewPath();
				content = templateInfo.getTemplateContent();
				templateId = templateInfo.getTemplateId();
				ATS_CURRENT_SERVER_LINK_FOR_DOC_RETREIVER = templateInfo.getServerURL();

				RichTextAreaWithSpellChecker txtA = new RichTextAreaWithSpellChecker();
				txtA.setStyleName("gwt-Texta");
				
				
				boolean changeBack = false;
				if(UtilsAtsGwt.getUserAgent().contains("msie") ) {
					String curExt = TemplateUtils.getFileExtension(path);
					if(curExt.toLowerCase().equals(TemplateUtils.TemplateExtensions.ATS_SMALL_EXTENSION)) {
						changeBack = true;
						content = content.replaceAll("\t", "mihai_si_cristi_au_plecat_in_concediu_si_eu_am_ramas_sa_rezolv_bugurile");
					}
				}
				
				txtA.setText(content);

				if(changeBack) {
					content = content.replaceAll("mihai_si_cristi_au_plecat_in_concediu_si_eu_am_ramas_sa_rezolv_bugurile", "\t");
				}
				
				// creez si afisej EditDialog-ul
				EditTemplateDialog dlg = new EditTemplateDialog(path,
						getName(), Integer.parseInt(templateId), txtA,
						new Label(getLabelState()), templateInfo.getStatementsText());

				if (isStarter()) {
					dlg.setStyleName("gwt-DialogBoxYellowBg");
				} else {
					dlg.setStyleName("gwt-DialogBox");
				}
				RootPanel.get("slotDialog").add(dlg);

				RootPanel.get("slotLoad").remove(labelload);

			}

			public void onFailure(Throwable caught) {
				if (caught instanceof TemplateSearchPermisionException) {
					Window.alert(caught.getMessage());
					isSearchTakeByOther = true;
				} else {
					Window.alert("Error when contact server");
				}
			}
		};
		
		// solicit datele din template-ul BoilerPlates.
		//Daca cererea a fost efectuata cu success, incep procedura de initiere a interfetei de editare
		AsyncCallback labelListCallback = new AsyncCallback(){			
		public void onSuccess (Object result) {
				TemplateUtils.dataFromBoilerPlates = (List)result;
				String templateName =  com.google.gwt.user.client.Window.Location.getParameter("name");
				templateService.getTemplateInfo(getSearchId(), getCurrentUserId(),templateName, getTemplateInfoCallback);
				
			}
			 public void onFailure (Throwable caught){
				 String templateName =  com.google.gwt.user.client.Window.Location.getParameter("name");
				 String templateIdStr =  com.google.gwt.user.client.Window.Location.getParameter("templateId");
				 int templateId = -1;
				 try {
					 templateId = Integer.parseInt(templateIdStr); 
				 } catch (NumberFormatException nfe) {}
				 if (templateId!=-1) {
					 refreshTemplateState(templateName,templateId); 
				 }
				 Window.alert("Error when trying to init templates ...");
			}
		};
		templateService.getLabelList(TemplateEditClient.getSearchId(),TemplateEditClient.getCurrentUserId(),(AsyncCallback)labelListCallback);
	}
	
	
	/**
	 * functie javaScript nativa care intoarce searchID-ul pentru pagina curenta
	 * @return  search ID
	 */
	public static native int getSearchId() /*-{
		return $wnd.getSearchId();
	}-*/;
	
	public static native int getCurrentUserId() /*-{
		return $wnd.getCurrentUserId();
	}-*/;
	
	public static native boolean isStarter() /*-{
		return $wnd.isStarter();
	}-*/;
	
	public static native String getLabelState()/*-{
		return $wnd.getLabelState();
	}-*/;
	
	public static native String getName()/*-{
		return $wnd.getName();
	}-*/;
	
	public static native String getPolicyName()/*-{
		return $wnd.getPolicyName();
	}-*/;
		
	public static native int getWindowTop()/*-{
		return $wnd.getWindowTop();
	}-*/;
	
	public static native int getWindowLeft()/*-{
		return $wnd.getWindowLeft();
	}-*/;
	
	public static native int getScrollLeft()/*-{
		return $wnd.getScrollLeft();
	}-*/;
	
	public static native int getScrollTop()/*-{
		return $wnd.getScrollTop();
	}-*/;
			
	
	public static native void refreshParent()/*-{
		$wnd.refreshParent();
	}-*/;
	
	public static native void updateTemplateState(String name, String state)/*-{
		$wnd.updateTemplateState(name,state);
	}-*/;
	
	public static native void refreshTemplateState(String name, int id)/*-{
		$wnd.refreshTemplateState(name,id);
	}-*/;
	
	public static native void destroy()/*-{
		$wnd.destroy();
	}-*/;
	public static native boolean isShowSavePanel() /*-{
		return $wnd.isShowSavePanel();
	}-*/;
	public static native String getDocDataSource()/*-{
		return $wnd.getDocDataSource();
	}-*/;
	public static native boolean isDisableBPControls() /*-{
		return $wnd.isDisableBPControls();
	}-*/;
}
