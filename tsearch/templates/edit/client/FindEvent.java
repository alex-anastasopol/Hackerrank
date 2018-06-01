package ro.cst.tsearch.templates.edit.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.bfr.client.selection.Range;
import com.bfr.client.selection.RangeEndPoint;
import com.bfr.client.selection.Selection;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventPreview;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.TextBox;

public class FindEvent implements EventPreview {
	
	public static final int F1 = 112;
	public static final int F3 = 114;
	public static final int F5 = 116;
	public static final int F6 = 117;
	public static final int F7 = 118;
	public static final int F12 = 123; 
	
	private Vector<RichTextArea> allTextAreas = null;
	private TextBox findTextBox = null;
	private String searched = "*";
	
	private int currIndexTextArea = 0;
	private int currentIndex = -1;
	private int currentTextElemet = 0;
   

	public int getCurrIndexTextArea() {
		return currIndexTextArea;
	}


	/*public void setCurrIndexTextArea(int currIndexTextArea) {
		this.currIndexTextArea = currIndexTextArea;
	}*/

	public boolean onEventPreview(Event event) {
    	int type = DOM.eventGetType(event);
    	
    	if(type==Event.ONKEYDOWN) 
    		{
            int keyCode    = DOM.eventGetKeyCode(event);

            switch(keyCode) {
	            case F5:
	            {
	            	//disable F5 key, so the user doesn't accidentally refreshes the page
	            	return false; 
	            }
	            case F7: 
	            {
	            	f7Down();
	            	return false;
	            }
	            case F6: 
	            {
	            	f6Down();
	            	return false; 
	            }
            }	
        }  
        return true;
    }

    
	public Vector<RichTextArea> getAllTextAreas() {
		return allTextAreas;
	}

	public void setAllTextAreas(Vector<RichTextArea> allTextAreas) {
		this.allTextAreas = allTextAreas;
	}

	public String getSearched() {
		return searched;
	}

	public void setSearched(String searched) {
		this.searched = searched;
	}

	public TextBox getFindTextBox() {
		return findTextBox;
	}

	public void setFindTextBox(TextBox findTextBox) {
		this.findTextBox = findTextBox;
	}
	
	public int f7Down(){
    	currIndexTextArea = 0;
    	currentTextElemet = 0; 
    	currentIndex = -1;
    	return f6Down();
	}
	 
    public int f6Down(){
    	try{
	    	for(int indexTextArea = currIndexTextArea;indexTextArea<allTextAreas.size();indexTextArea++) {
	    		RichTextArea ta = allTextAreas.get(indexTextArea);
				Text txt = null;
				IFrameElement frame = ta.getElement().cast();		
				Node node = frame.getContentDocument().getFirstChild();
				
				if(node!=null){
					txt = Range.getAdjacentTextElement(node, node, true, false);
					if(txt!=null){ 
		 
						List<Text> allTextElements = getTextAndAdjacentTextElements(txt,node);
						
						for(int i=currentTextElemet;i<allTextElements.size(); i++){
							currentTextElemet = i;
							String str = allTextElements.get(i).getNodeValue();
							int offsetStart = -1;
							int offsetEnd = -1;
							while((offsetStart = str.indexOf(searched.toLowerCase(),currentIndex+1))>=0 ){
		    					currentIndex = offsetStart;
		    					offsetEnd = offsetStart+searched.length();
		    					
		    					RangeEndPoint start = new RangeEndPoint(allTextElements.get(i), offsetStart);
		    	    			RangeEndPoint end = new RangeEndPoint(allTextElements.get(i), offsetEnd);
		    	    			ta.setFocus(true);
		    	    			// fire a mouse event, so the text area will expand
		    	    			NativeEvent event = Document.get().createClickEvent(1, 
	    	    						ta.getAbsoluteLeft(), ta.getAbsoluteTop(),
	    	    						0, 0, true, true, true, true);
	    	    				DomEvent.fireNativeEvent(event, ta);
		    	    			if(getUserAgent().contains("msie")) { // IE wins this time :)
		    	    				Selection.getSelection( getWindow(frame) ).setRange(new Range(start,end));		    	    				
		    	    			} else {
		    	    				searchString(searched, frame, allTextElements.get(i), offsetStart);
		    	    			}
		    	    			currIndexTextArea = indexTextArea;
		    	    			
		    	    			return currIndexTextArea;
		    					
		    				}
							currentIndex=-1;
						}
					}
				}
				
				
				currentTextElemet = 0; 
				currentIndex=-1;
	    	}
    	}catch(Exception e){
    		GWT.log("f6Down()", e);
    	}
    	
    	currIndexTextArea = 0;
    	currentTextElemet = 0; 
    	currentIndex = -1;
    	Window.alert("No more occurrences of the string '"+searched+"' were found.");
    	
    	return 0;
    }
    
  
    
    private List<Text> getTextAndAdjacentTextElements(Text txt, Node node) {
    	List<Text> all = new ArrayList<Text>();
    	all.add(txt);
    	
    	while((txt = Range.getAdjacentTextElement(txt, node, true, false ))!=null){
    		all.add(txt);
    	}
    	
		return all;
	}


	public static native JavaScriptObject getWindow(JavaScriptObject iframe)/*-{
		var wnd1 = iframe.contentWindow || iframe.contentDocument;
		return wnd1;
	}-*/;
	
	public static native String getUserAgent() /*-{
		return navigator.userAgent.toLowerCase();
	}-*/;
	
	public static native void searchString(String searched, JavaScriptObject iframe, JavaScriptObject textNode, int offset)/*-{
		var wnd = iframe.contentWindow;
		var doc = iframe.contentDocument;
		var sel = wnd.getSelection();
		var range = doc.createRange();
		
		range.setStart(textNode, offset);
		range.setEnd(textNode, offset);
		
		sel.removeAllRanges();
		sel.addRange(range);
		
		wnd.find(searched);
	}-*/;
    
	/*
	public static native boolean convertF3toCtrlSpace(Event e) /*-{
    var isIE = ( navigator.userAgent.toLowerCase().indexOf("msie") != -1 );
    var isChrome = ( navigator.userAgent.toLowerCase().indexOf("chrome") != -1 );
     
    keyCode = e.keyCode; 
	if ( keyCode == 114 ) { 
		if( isIE ) { 
			e.keyCode = 505; 
			var evt = $doc.createEventObject();
		    evt.type = "keypress";
		    evt.ctrlKey = true;
		    evt.altKey = false;
		    evt.shiftKey = false;
		    evt.metaKey = false;
		    evt.keyCode = 32;
		    evt.charCode = 0;
		    e.srcElement.fireEvent("on" + evt.type, evt);
		    return false;
		}else if(isChrome) {
			var evt = document.createEvent("Events");
			evt.initEvent("keypress", true, true);
		    evt.view = null;
		    evt.altKey = false;
		    evt.ctrlKey = true;
		    evt.shiftKey = false;
		    evt.metaKey = false;
		    evt.keyCode = 32;
		    evt.charCode = 0;
		    e.target.dispatchEvent(evt);
		    e.preventDefault();
		    e.stopPropagation();
		}else { 
			var evt = $doc.createEvent('KeyEvents');
    		evt.initKeyEvent("keypress", true, true, null, true, false,false, false, 32, 0);
    		e.target.dispatchEvent(evt);
    		e.cancelBubble = true;
    		e.preventDefault();
		} 
		return true;
	} */ 
	//}-*/;
    
    
}
