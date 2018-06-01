package ro.cst.tsearch.templates.tsdedit.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowCloseListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.gwt.components.client.ButtonWithTooltip;

/**
 * clasa EntryPoint for TSDedit.jsp
 * @author Cristian Stochina
 */
public class TSDTemplateEditClient  implements EntryPoint {
	private static final String submitMouseOver =  "Save and return to previous page";
	
	private static final String applyMouseOver =  "Save and continue editing";

	private static final String deleteMouseOver =  "Delete ALL changes and reset template to the initial state";

	private static final String closeMouseOver = "Close window, losing all unsaved changes";
	public static final int 	SEND_BY_MAIL = 0, VIEW_PDF = 1;
	
	public static final  String TSD_TEMPLATE_NAME = "TSDTemplate";

	String searchId = "";
	
	String userId = "";
	
	long searchIdLong = 0;
	
	long userIdLong = 0;
	 
	static final String  TSD_TEMPLATE_CODE="";
	
	boolean isSearchTakeByOther = false;
	
	static final TSDTemplateEditServiceAsync templateService = (TSDTemplateEditServiceAsync ) GWT.create(TSDTemplateEditService.class);
	
	class TSDWindowCloseListener implements WindowCloseListener{
		public void onWindowClosed() {	
		}

		//if we close the window and make refresh to the parent
		public String onWindowClosing() {
			String templateName =  com.google.gwt.user.client.Window.Location.getParameter("templateName");
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
		
	public void init() {
		
		Window.addWindowCloseListener(new TSDWindowCloseListener() );
		ServiceDefTarget endpoint = (ServiceDefTarget) templateService;
		endpoint.setServiceEntryPoint( "/title-search/TSDTemplateServlet" );

		String[] searchIdandUserId = getSearchIdAndUserId().split(":");
		
		searchId = searchIdandUserId[0];
		
		userId = searchIdandUserId[1];
		
		searchIdLong = Long.parseLong(searchId);
		
		userIdLong = Long.parseLong(userId );
		
		final AsyncCallback getTSDTemplateContentCallback = new AsyncCallback(){
			public void onSuccess(Object result) {
				
				if(result!=null){
					String str=(String )result;
					tinyMceSetFocus();
					tinyMCEInsertContent(str);
				}
				else{
					tinyMCEInsertContent("");
				}
			}
			
			public void onFailure(Throwable caught) {
				if(caught instanceof TSDTemplateSearchPermisionException){
					Window.alert(caught.getMessage());
					isSearchTakeByOther = true;
				}
				else{
					Window.alert("Error when contact server");
				}
				tinyMCEInsertContent("");
			}
		};
		HorizontalPanel fpanel = new HorizontalPanel();
		
		ButtonWithTooltip butApply =  ButtonWithTooltip.getInstance("Apply", new ButApplyListener(),
											applyMouseOver, "button", "gwt-toolTip", -1);
		ButtonWithTooltip butSubmit = ButtonWithTooltip.getInstance("Submit", new ButSaveListener(),
											submitMouseOver, "button", "gwt-toolTip", -1);
		ButtonWithTooltip butCancel = ButtonWithTooltip.getInstance("Reset template", new ButDeleteListener(),
											deleteMouseOver, "button", "gwt-toolTip", -1);
		ButtonWithTooltip butClose = ButtonWithTooltip.getInstance("Cancel Changes Since Last Save", new ButOnlyCloseListner(),
											closeMouseOver, "button", "gwt-toolTip", -1);
		fpanel.add(butApply);
		fpanel.add(butSubmit);
		fpanel.add(butCancel);
		fpanel.add(butClose);
		fpanel.addStyleName("gwt-tableCenter");
		RootPanel.get("TSD_slot_buttons").add(fpanel);
		
		final String templateName =  com.google.gwt.user.client.Window.Location.getParameter("templateName");
		new Timer() {
			public void run() {
				if(isReady()) {
					templateService.getTSDTemplateContent(searchIdLong,userIdLong,templateName,getTSDTemplateContentCallback);
					cancel();
				}
			}
		}.scheduleRepeating(500);					
	}
	
	@SuppressWarnings("unchecked")
	public void onModuleLoad() {
		init();
	}
	
	class ButSaveListener extends SimplePanel implements ClickListener{
		
		
		public void onClick(Widget sender) {
			if(sender instanceof Button){
				Button but = (Button) sender;
				disableButtons();
				but.setText("Saving...");
				String TSDcontent = tinyMCEGetContent();
				TSDcontent = TSDcontent.replaceAll("<strong>", "<b>");
				TSDcontent = TSDcontent.replaceAll("</strong>", "</b>");
				int templateId = -1;
				try { templateId = Integer.parseInt(getTemplateId()); } catch (Exception e){}
				templateService.saveTemplate(searchIdLong, userIdLong,TSD_TEMPLATE_CODE,templateId,TSDcontent, "Submit", new TSDSaveButtonAsync ());
			}
		}
	}

	class ButApplyListener extends SimplePanel implements ClickListener{
		
		public void onClick(Widget sender) {
			if(sender instanceof Button){
				Button but = (Button) sender;
				disableButtons();
				but.setText("Saving...");
				String TSDcontent = tinyMCEGetContent();
				TSDcontent = TSDcontent.replaceAll("<strong>", "<b>");
				TSDcontent = TSDcontent.replaceAll("</strong>", "</b>");
				int templateId = -1;
				try { templateId = Integer.parseInt(getTemplateId()); } catch (Exception e){}
				templateService.saveTemplate(searchIdLong, userIdLong,TSD_TEMPLATE_CODE,templateId,TSDcontent, "Apply", new TSDApplyButtonAsync(but, "Apply"));
			}
		}
	}
	
	class ButCloseListener extends SimplePanel implements ClickListener{

		public void onClick(Widget sender) {
			boolean force=false;
			//apel RPC 
			templateService.deleteGeneratedTemplate(searchIdLong,userIdLong,TSD_TEMPLATE_CODE,-1,force,new TSDCloseButtonAsync());
		}
		
	}
	
	class ButOnlyCloseListner extends SimplePanel implements ClickListener{
		
		public void onClick(Widget sender){
			String message = "</div><div><b>TSD Template </b> changes were canceled (reverted to last save) ";
			templateService.logMessage(
					searchIdLong, 
					message,
						new AsyncCallback<Void>() {

							public void onFailure(Throwable arg0) {
								Window.alert(arg0.getMessage());
							}

							public void onSuccess(Void arg0) {
								destroy();
							}
						});
			
		}
	}
	
	class ButDeleteListener extends SimplePanel  implements ClickListener{
		public void onClick(Widget sender) {
			if (Window.confirm("Are you sure you want to reset the template to its original form and lose any changes made?")) {
				if(sender instanceof Button){
					Button but = (Button) sender;
					but.setEnabled(false);
					but.setText("Resetting...");
					boolean force=true;
					//apel RPC 
					templateService.deleteGeneratedTemplate(searchIdLong,userIdLong,TSD_TEMPLATE_CODE,-1,force,new TSDDeleteButtonAsync());
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	class TSDSaveButtonAsync implements AsyncCallback {
		
		public void onFailure(Throwable caught) {
			if(caught instanceof TSDTemplateSearchPermisionException){
				Window.alert(caught.getMessage());
				isSearchTakeByOther = true;
			}
			else{
				Window.alert("Error when contact server");
			}
			destroy();
		}
		
		public void onSuccess(Object result) {
			String message = "</div><div><b>TSD Template </b> was saved (using Submit button) ";
			templateService.logMessage(
					searchIdLong, 
					message,
						new AsyncCallback<Void>() {

							public void onFailure(Throwable arg0) {
								Window.alert(arg0.getMessage());
							}

							public void onSuccess(Void arg0) {
								destroy();
							}
						});
		}
	}
	
	@SuppressWarnings("unchecked")
	class TSDApplyButtonAsync implements AsyncCallback {
		Button b = null;
		String label = "";
		
		public TSDApplyButtonAsync(Button b, String label){
			this.b = b;
			this.label = label;
		}
		
		public void onFailure(Throwable caught) {
			if(caught instanceof TSDTemplateSearchPermisionException){
				Window.alert(caught.getMessage());
				isSearchTakeByOther = true;
			}
			else{
				Window.alert("Error when contact server");
			}
		}

		public void onSuccess(Object result) {
			if (b != null){
				enableButtons();
				b.setText(label);
				String message = "</div><div><b>TSD Template </b> was saved (using Apply button) ";
				templateService.logMessage(
						searchIdLong, 
						message,
							new AsyncCallback<Void>() {

								public void onFailure(Throwable arg0) {
									Window.alert(arg0.getMessage());
								}

								public void onSuccess(Void arg0) {
									//Window.alert("The changes since last save were canceled");
								}
							});
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	class TSDDeleteButtonAsync implements AsyncCallback{
		
		public void onFailure(Throwable caught) {
			if(caught instanceof TSDTemplateSearchPermisionException){
				Window.alert(caught.getMessage());
				isSearchTakeByOther = true;
			}
			else{
				Window.alert("Error when contact server");
			}
			destroy();
		}
		
		public void onSuccess(Object result) {
			String message = "</div><div>The <b>TSD Template </b> was reset to the original form ";
			templateService.logMessage(
					searchIdLong, 
					message,
						new AsyncCallback<Void>() {

							public void onFailure(Throwable arg0) {
								Window.alert(arg0.getMessage());
							}

							public void onSuccess(Void arg0) {
								destroy();
							}
						});
		}
	}
	
	@SuppressWarnings("unchecked")
	class TSDCloseButtonAsync implements AsyncCallback{
		
		public void onFailure(Throwable caught) {
			if(caught instanceof TSDTemplateSearchPermisionException){
				Window.alert(caught.getMessage());
				isSearchTakeByOther = true;
			}
			else{
				Window.alert("Error when contact server");
			}
			destroy();
		}
		
		public void onSuccess(Object result) {
			destroy();
		}
	}
	
	public static void disableButtons() {
		NodeList<Element> elements = RootPanel.getBodyElement().getElementsByTagName("button");

		for(int i = 0; i < elements.getLength(); i++) {
			elements.getItem(i).setAttribute("disabled", "disabled");
		}
	}
	
	public static void enableButtons() {
		NodeList<Element> elements = RootPanel.getBodyElement().getElementsByTagName("button");
		
		for(int i = 0; i < elements.getLength(); i++) {
			elements.getItem(i).removeAttribute("disabled");
		}
	}
		
	public static native void tinyMCEInsertContent(String content)/*-{
		$wnd.tinyMCEInsertContent(content);
	}-*/;
	
	public static native String getSearchIdAndUserId()/*-{
		return $wnd.getSearchIdAndUserId();
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
	
	public static native void resetParentSearchForce()/*-{
		$wnd.resetParentSearchForce();
	}-*/;
	
	public static native void focusOnTextArea()/*-{
		$wnd.focusOnTextArea();
	}-*/;
	
	public static native String  navigatorName()/*-{
		return $wnd.navigatorName();
	}-*/;
	
	public static native  void tinyMceSetFocus()/*-{
		$wnd.tinyMceSetFocus();
	}-*/;
	
	public static native boolean isReady()/*-{
		return $wnd.isReady();
	}-*/;
	
	public static native void refreshTemplateState(String name, int id)/*-{
		$wnd.refreshTemplateState(name,id);
	}-*/;
	
	public static native String getName()/*-{
		return $wnd.getName();
	}-*/;
	
	public static native String getTemplateId()/*-{
		return $wnd.getTemplateIdString();
	}-*/;
	
}
 
