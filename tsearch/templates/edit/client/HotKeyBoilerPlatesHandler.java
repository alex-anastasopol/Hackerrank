package ro.cst.tsearch.templates.edit.client;

import com.bfr.client.selection.Range;
import com.bfr.client.selection.Selection;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.Widget;
import com.gwt.utils.client.UtilsAtsGwt;

/**
 * Class that handles the  hotkeys when typing in the templates textareas
 * @see Bug 3078
 * @author Mihai D.
 */
class HotKeyBoilerPlatesHandler implements   KeyDownHandler{

	
	private RichTextArea ta = null;
	private TemplateComboList comboList = null;
	private FindEvent eventPreview = null;
	
	public FindEvent getEventPreview() {
		return eventPreview;
	}

	public void setEventPreview(FindEvent eventPreview) {
		this.eventPreview = eventPreview;
	}

	
	@Override
	public void onKeyDown(KeyDownEvent event) {
			int keyCode = event.getNativeKeyCode();
			
			//Window.alert(keyCode+"");
			if( FindEvent.F3 == keyCode ){
				IFrameElement frame = ta.getElement().cast();
				Range range = Selection.getSelection( FindEvent.getWindow(frame)).getRange();
				if(range!=null){
					String selected = range.getText();
					if(selected!=null){
						comboList.requestContentsCustom(selected,-1,-1,-1);
					}
				}
				
				try{event.preventDefault();}catch(Throwable e){}
				try{event.stopPropagation();}catch(Throwable e){}
				try{UtilsAtsGwt.cancelIframeEvent(event.getNativeEvent());}catch(Throwable e){}
			}else if(FindEvent.F6 == keyCode){
				eventPreview.f6Down();
				
				try{event.preventDefault();}catch(Throwable e){}
				try{event.stopPropagation();}catch(Throwable e){}
				try{UtilsAtsGwt.cancelIframeEvent(event.getNativeEvent());}catch(Throwable e){}
			}else if(FindEvent.F7 == keyCode){
				eventPreview.f7Down();
				
				try{event.preventDefault();}catch(Throwable e){}
				try{event.stopPropagation();}catch(Throwable e){}
				try{UtilsAtsGwt.cancelIframeEvent(event.getNativeEvent());}catch(Throwable e){}
			}else if(FindEvent.F5 == keyCode){
				
				try{event.preventDefault();}catch(Throwable e){}
				try{event.stopPropagation();}catch(Throwable e){}
				try{UtilsAtsGwt.cancelIframeEvent(event.getNativeEvent());}catch(Throwable e){}
			}
	}
	
	
	public HotKeyBoilerPlatesHandler (){
	}
	
	public RichTextArea getTa() {
		return ta;
	}

	public void setTa(RichTextArea ta) {
		this.ta = ta;
	}

	public void setTa(Widget w) {
		this.ta = (RichTextArea)w;
	}


	public TemplateComboList getComboList() {
		return comboList;
	}

	public void setComboList(TemplateComboList comboList) {
		this.comboList = comboList;
	}

	
}
