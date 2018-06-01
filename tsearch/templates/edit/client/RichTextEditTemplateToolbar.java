package ro.cst.tsearch.templates.edit.client;

import com.bfr.client.selection.Range;
import com.bfr.client.selection.Selection;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.gwt.utils.client.UtilsAtsGwt;

public class RichTextEditTemplateToolbar extends Composite {

	
	
	private EventHandler handler = new EventHandler();

	private RichTextArea richText;

	private VerticalPanel outer = new VerticalPanel();
	private HorizontalPanel topPanel = new HorizontalPanel();

	private Button pasteText;
	private Button createLink;
	private Button createPdfLink;
	private Button removeLink;
	//private Button boostrap;

	
	private class EventHandler implements ClickHandler, ChangeHandler, KeyUpHandler, KeyDownHandler {
		public void onChange(ChangeEvent event) {
			//Widget sender = (Widget) event.getSource();
		}

		public void onClick(ClickEvent event) {
			RichTextEditTemplateToolbar.this.onClick(event);
		}

		public void onKeyUp(KeyUpEvent event) {
			//Widget sender = (Widget) event.getSource();
		}

		@Override
		public void onKeyDown(KeyDownEvent event) {
			int keyCode = event.getNativeKeyCode();
			
			if(UtilsAtsGwt.F8 == keyCode){
				
				Widget sender = (Widget) event.getSource();
				if (sender instanceof RichTextArea) {
					RichTextArea richTextArea = (RichTextArea) sender;
					
					richTextArea.getFormatter().insertHTML("&deg;");
					
					try{event.preventDefault();}catch(Throwable e){}
					try{event.stopPropagation();}catch(Throwable e){}
					try{UtilsAtsGwt.cancelIframeEvent(event.getNativeEvent());}catch(Throwable e){}
					
				}
				
				
			}
			
			
			
		}
	}

	/**
	 * Creates a new toolbar that drives the given rich text area.
	 * @param richText  the rich text area to be controlled
	 * @param userAgent 
	 */	 
	public RichTextEditTemplateToolbar(RichTextArea richText, String userAgent) {
		this.richText = richText;
		outer.add(topPanel);
		topPanel.setWidth("100%");

		initWidget(outer);
		//setStyleName("gwt-RichTextToolbar");
		//richText.addStyleName("hasRichTextToolbar");

		boolean pasteTextFullyFunctional = isPasteTextFullyFunctional(userAgent);
			
		pasteText = new Button("Paste Text");
		
		if(pasteTextFullyFunctional) {
			pasteText.addClickHandler(handler);
			pasteText.setStylePrimaryName("button");
		} else {
			pasteText.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					Window.alert("Function not supported by your browser. Please use Ctrl+Shift+V instead");
				}
			});
			pasteText.setStylePrimaryName("button-disabled-like");
		}
		
		
//		int version = -1;
//		
//		try {
//			if(!"".equals(UtilsAtsGwt.getBrowserVersion(userAgent)) && userAgent.toLowerCase().contains("firefox")){
//				version = Integer.parseInt(UtilsAtsGwt.getBrowserVersion(userAgent).split("\\.")[0]);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		if((userAgent == null || !userAgent.toLowerCase().contains("chrome")) && 
//				(userAgent.toLowerCase().contains("firefox") && version < 16)) {
			topPanel.add(pasteText );	
//		}
		
		
		createLink = new Button("Paste Link",handler);
		createLink.setStylePrimaryName("button");
		topPanel.add(createLink );
		
		createPdfLink = new Button("Paste PDF Link",handler);
		createPdfLink.setStylePrimaryName("button");
		topPanel.add(createPdfLink );
		
		removeLink = new Button("Remove Link",handler);
		removeLink.setStylePrimaryName("button");
		//boostrap = new Button("Boostap",handler);
		//boostrap.setStylePrimaryName("button");
		topPanel.add(removeLink);
		//topPanel.add(boostrap);
		
		richText.addKeyUpHandler(handler);
		richText.addClickHandler(handler);
	}

	private boolean isPasteTextFullyFunctional(String userAgent) {
		if(userAgent != null) {
			int version = -1;
			try {
				
				String browserVersion = UtilsAtsGwt.getBrowserVersion(userAgent);
				
				if(!"".equals(browserVersion) && userAgent.toLowerCase().contains("firefox")){
					version = Integer.parseInt(browserVersion.split("\\.")[0]);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		
			if(userAgent.toLowerCase().contains("chrome")) {
				return false;
			} else if(userAgent.toLowerCase().contains("firefox") && version > 16) {
				return false;
			}
		}
		
		return true;
	}

	protected void onClick(ClickEvent event){
		Widget sender = (Widget) event.getSource();
		
		if (sender == createLink || sender==createPdfLink) {
			
			IFrameElement frame = richText.getElement().cast();
			Range range = Selection.getSelection( FindEvent.getWindow(frame)).getRange();
			if(range!=null){
				String selected = range.getHtmlText();
				if(selected!=null){
					if(selected.contains("<br>")||selected.contains("<br/>")
							||selected.contains("<BR>")||selected.contains("<BR/>")){
						Window.alert("Multiline Selection is not allowed !");
						return;
					}
				}
			}
			
			TemplateEditClient.templateService.getLinkForLastCopiedChapter(TemplateEditClient.getSearchId(),sender==createPdfLink,
					new AsyncCallback<String[]>() {
						
						@Override
						public void onSuccess(String []result) {
							if(Window.confirm("Link to \""+ result[1]+"\" ?" )){
								richText.getFormatter().createLink(result[0]);
							}
						}
						
						@Override
						public void onFailure(Throwable caught) {
							Window.alert(caught.getMessage());
						}
					});
		} else if (sender == removeLink) {
			richText.getFormatter().removeLink();
		} else if (sender == richText) {

		}else if (sender == pasteText){
			richText.setFocus(true);
			richText.getFormatter().insertHTML(UtilsAtsGwt.replaceNewLinesWithBreaks(UtilsAtsGwt.getTextClipBoardContent()));
		}
	}
}
