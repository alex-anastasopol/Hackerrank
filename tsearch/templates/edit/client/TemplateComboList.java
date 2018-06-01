package ro.cst.tsearch.templates.edit.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.Vector;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.core.client.GWT;

public class TemplateComboList extends Composite {

	private TextBox textBox;
	private ListBox listBox;
	private Button undoButton;
	private HorizontalPanel piece1, piece2;
	private HorizontalPanel dock;
	private HTML html;

	private List arrayList = new ArrayList();
	private Vector undoStack;
	private Vector instrumentVector = new Vector();
	private int instrumentCount = 0;

	private RichTextArea txtArea;
	private RichTextEditTemplateToolbar toolbar ;
	private VerticalPanel piece3;
	private Button addMoreButton;

	static final TemplateEditServiceAsync templateService = (TemplateEditServiceAsync) GWT
			.create(TemplateEditService.class);

	// combo class constructor
	public TemplateComboList(RichTextArea txt, List arrayList) {
		this.arrayList = arrayList;
		// setez obiectul TextArea
		txtArea = txt;
		listBox = new ListBox();
		textBox = new TextBox();
		undoButton = new Button();
		piece1 = new HorizontalPanel();
		piece2 = new HorizontalPanel();
		dock = new HorizontalPanel();
		toolbar = new RichTextEditTemplateToolbar(txt, getUserAgent());
		html = new HTML();
		undoStack = new Vector();

		piece3 = new VerticalPanel();
		for (int i = 0; i < instrumentCount; i++) {
			instrumentVector.add(new TextBox());
			instrumentVector.add(new TextBox());
			instrumentVector.add(new TextBox());
			instrumentVector.add(new TextBox());
		}

		addMoreButton = new Button();

		if (arrayList.size() > 1) {
			dock = addCompositeCombo();
		}
	}

	// adds widghest inside the container
	public HorizontalPanel addCompositeCombo() {

		undoButton.setText("Undo");
		undoButton.setStyleName("button");
		undoButton.setEnabled(false);
		textBox.setWidth("5em");

		piece1.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
		piece2.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);

		// adds objects into the Panel
		piece1.add(html);
		if(!TemplateEditClient.isDisableBPControls()) {
			piece2.add(listBox);
			piece2.add(textBox);
		}
		piece3.setBorderWidth(0);

		addMoreButton.setText("Add instrument");
		addMoreButton.setStyleName("button");
		
		piece3.add(addMoreButton);

		for (int i = 0; i < instrumentCount * 4; i++) {
			((TextBox) instrumentVector.get(i)).setWidth("4em");
			((TextBox) instrumentVector.get(i)).setHeight("20px");
		}

		for (int i = 0; i < instrumentCount; i += 4) {
			HorizontalPanel bookPanel = new HorizontalPanel();
			bookPanel.add(new Label(" Book:   "));
			bookPanel.add((TextBox) instrumentVector.get(i));

			HorizontalPanel pagePanel = new HorizontalPanel();
			pagePanel.add(new Label(" Page:   "));
			pagePanel.add((TextBox) instrumentVector.get(i + 1));

			HorizontalPanel instrNoPanel = new HorizontalPanel();
			instrNoPanel.add(new Label(" Instr. No:   "));
			instrNoPanel.add((TextBox) instrumentVector.get(i + 2));

			HorizontalPanel docNoPanel = new HorizontalPanel();
			docNoPanel.add(new Label(" Doc. No:   "));
			docNoPanel.add((TextBox) instrumentVector.get(i + 3));

			HorizontalPanel p = new HorizontalPanel();
			p.add(bookPanel);
			p.add(pagePanel);
			p.add(instrNoPanel);
			p.add(docNoPanel);
			piece3.add(p);
		}
		if(!TemplateEditClient.isDisableBPControls()) {
			piece2.add(undoButton);
		}
		piece2.add(toolbar);
		// updates combo box
		updateCombo("");

		textBox.addKeyboardListener(new KeyboardListenerAdapter() {
			public void onKeyUp(Widget sender, char keyCode, int modifiers) {

				updateCombo(textBox.getText());

			}
		});

		listBox.addChangeListener(new ChangeListener() {
			public void onChange(Widget sender) {
				final String hashIndex = listBox.getItemText(listBox
						.getSelectedIndex());
				requestContents(hashIndex);
			}
		});

		txtArea.addKeyboardListener(new KeyboardListenerAdapter() {
			public void onKeyUp(Widget sender, char keyCode, int modifier) {
				if (modifier == KeyboardListener.MODIFIER_CTRL
						&& keyCode == ' ') {
					String hashIndex = listBox.getItemText(listBox
							.getSelectedIndex());
					requestContents(hashIndex);
				}
			}
		});

		undoButton.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {

				// pops last element
				txtArea.setHTML(undoStack.remove(undoStack.size() - 1).toString());
				refreshUndoButtonState();

			}
		});

		addMoreButton.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {

				instrumentCount++;

				instrumentVector.add(new TextBox());
				instrumentVector.add(new TextBox());
				instrumentVector.add(new TextBox());
				instrumentVector.add(new TextBox());

				for (int i = (instrumentCount - 1) * 4; i < instrumentCount * 4; i++) {
					((TextBox) instrumentVector.get(i)).setWidth("4em");
					((TextBox) instrumentVector.get(i)).setHeight("20px");
				}

				int i = (instrumentCount - 1) * 4;
				HorizontalPanel bookPanel = new HorizontalPanel();
				bookPanel.add(new Label(" Book:   "));
				bookPanel.add((TextBox) instrumentVector.get(i));

				HorizontalPanel pagePanel = new HorizontalPanel();
				pagePanel.add(new Label(" Page:   "));
				pagePanel.add((TextBox) instrumentVector.get(i + 1));

				HorizontalPanel instrNoPanel = new HorizontalPanel();
				instrNoPanel.add(new Label(" Instr. No:   "));
				instrNoPanel.add((TextBox) instrumentVector.get(i + 2));

				HorizontalPanel docNoPanel = new HorizontalPanel();
				docNoPanel.add(new Label(" Doc. No:   "));
				docNoPanel.add((TextBox) instrumentVector.get(i + 3));

				HorizontalPanel p = new HorizontalPanel();
				p.add(bookPanel);
				p.add(pagePanel);
				p.add(instrNoPanel);
				p.add(docNoPanel);
				piece3.add(p);

			}
		});

		dock.add(piece1);
		dock.add(piece2);	
		if(!TemplateEditClient.isDisableBPControls()) {
			dock.add(piece3);
		}
		return dock;
	}

	/**
	 * Updatest combo box content.Use a filter to let only values starting with
	 * filterKey
	 * 
	 * @param filterKey
	 */

	public void updateCombo(String filterKey) {
		List set = arrayList;
		Iterator it = set.iterator();
		Vector list = new Vector();

		// set list empty
		listBox.clear();

		while (it.hasNext()) {
			String me = (String) it.next();

			// if the filter was found in first position
			if ((me.toLowerCase().indexOf(filterKey.toLowerCase()) == 0)
					|| (me.startsWith(" ")))
				list.add(me);
		}

		// convert to a normal array which will be sorted
		Object[] listToPutInCombo = list.toArray();
		Arrays.sort(listToPutInCombo);

		// adds items into the combo box
		for (int i = 0; i < listToPutInCombo.length; i++) {
			listBox.addItem(listToPutInCombo[i].toString());
		}

		if (!"".equals(filterKey) && listToPutInCombo.length == 2) {
			String hashIndex = listToPutInCombo[1].toString();
			requestContents(hashIndex);
		}
	}

	/**
	 * Makes a RPC request to the server to get the compiled code for the
	 * specified element Place the text at the current position
	 * 
	 * @param element
	 */
	public void requestContents(String element) {
		requestContentsCustom(element, -1, -1, -1);
	}

	/**
	 * Makes a RPC request to the server to get the compiled code for the
	 * specified element
	 * 
	 * @param element
	 *            - the element to request
	 * @param where
	 *            - where to insert the text ( -1 is interpreted as the current
	 *            position )
	 * @param deleteFrom
	 *            , deleteTo - if these are not -1, delete the text between this
	 *            positions
	 */
	public void requestContentsCustom(String element, final int where, final int deleteFrom, final int deleteTo) {
		List/* <InstrumentStructForUndefined> */instrumentList = new ArrayList/*<InstrumentStructForUndefined>*/();

		ServiceDefTarget endpoint = (ServiceDefTarget) templateService;
		endpoint.setServiceEntryPoint("/title-search/TemplatesServlet");

		for (int i = 0; i < instrumentVector.size(); i += 4) {
			String book = ((TextBox) instrumentVector.get(i)).getText();
			String page = ((TextBox) instrumentVector.get(i + 1)).getText();
			String instNo = ((TextBox) instrumentVector.get(i + 2)).getText();
			String docNo = ((TextBox) instrumentVector.get(i + 3)).getText();

			if ((!book.equals("") && !page.equals("")) || !instNo.equals("")
					|| !docNo.equals("")) {
				InstrumentStructForUndefined ilfu = new InstrumentStructForUndefined(
						book, page, instNo, docNo, "");
				instrumentList.add(ilfu);
			}
		}

		// request the data from the BoilerPlates template, for the selected
		// element (we also send the instrument list for UNDEFINED elements)
		AsyncCallback labelListCallback = new AsyncCallback() {
			public void onSuccess(Object result) {
				String contents = (String) result;
				if(contents!=null) {
					contents = contents.replaceAll("\r\n", "\n").replaceAll("\n", "<br>");
				}
				if ((contents != null && !contents.trim().equals(""))
						&& deleteFrom >= 0 && deleteTo >= 0) {
					// if we have any contents, delete what we wanted
					String text = txtArea.getHTML();
					txtArea.setText(text.substring(0, deleteFrom)
							+ text.substring(deleteTo, text.length()));
				}
				if (contents == null || contents.length() == 0) {
					if (where != -1 && deleteTo != -1){
						insertAt(txtArea, contents);
					}
				}
				if (contents != null && contents.length() > 0) {
					insertAt(txtArea, contents);
				}
			}

			public void onFailure(Throwable caught) {
				Window.alert("Error when trying to get boilerplates code...");
				Window.alert(caught.getMessage());
			}
		};
		templateService.getElement(TemplateEditClient.getSearchId(),
				TemplateEditClient.getCurrentUserId(), element, instrumentList,
				(AsyncCallback) labelListCallback);

	}

	/**
	 *Enable/Disable undo button based upon stack size
	 */
	public void refreshUndoButtonState() {
		if (undoStack.size() < 1)
			undoButton.setEnabled(false);
		else
			undoButton.setEnabled(true);
	}

	public List getMapValues() {
		return arrayList;
	}

	public ListBox getListBox() {
		return listBox;
	}

	public HorizontalPanel getCompositeCombo() {
		return dock;
	}

	public void setCompositeCombo(HorizontalPanel hPanel) {
		dock = hPanel;
	}

	public void setMapValues(List hList) {
		arrayList = hList;
	}

	public void setListBox(ListBox lBox) {
		listBox = lBox;
	}

	public native int getCursorPosition(Element elem)/*-{
	    try {
	      var tr = elem.document.selection.createRange();
	      if (tr.parentElement().uniqueID != elem.uniqueID)
	        return -1;
	      return -tr.move("character", -65535);
	    }
	    catch (e) {
	      return 0;
	    }
	   }-*/;

	public static native int getInternetExplorerVersion()/*-{
		// Returns the version of Internet Explorer or a -1
		// (indicating the use of another browser).
		  var rv = -1; // Return value assumes failure.
		  if (navigator.appName == 'Microsoft Internet Explorer')
		  {
		    var ua = navigator.userAgent;
		    var re  = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
		    if (re.exec(ua) != null)
		      rv = parseFloat( RegExp.$1 );
		  }
		  return rv;
		}-*/;

	public int getRealCursorPosition(TextArea txt) {
		int ieVersion =getInternetExplorerVersion(); 
		if ( ieVersion== -1 || ieVersion==8 || ieVersion==7 || ieVersion==6) {
			return txt.getCursorPos();
		}
		int a = getCursorPosition(((TextArea) txt).getElement());
		txt.setCursorPos(0);
		int b = getCursorPosition(((TextArea) txt).getElement());
		int totalCount = a - b;
		txt.setCursorPos(totalCount);
		return totalCount;
	}

	public void insertAt(RichTextArea txt, String value) {
		txt.setFocus(true);
		int ieVersion =getInternetExplorerVersion();
		String str = txt.getHTML();
		if ( ieVersion==8 || ieVersion==7 || ieVersion==6 ) {
			
		}else {
			value = value.replaceAll("\r\n", "\n");
			str = txt.getHTML().replaceAll("\r\n", "\n");	
		}
		
		try{
			html.setHTML("");
			// updates undo stack
			undoStack.add(str);
			refreshUndoButtonState();
			txt.getFormatter().insertHTML(value);
			txt.setFocus(true);
		}catch(Exception e){
			String message = "<font color='#ef0c67'>Mouse cursor must be placed in text editor!!!</font>";
			html.setHTML(message);
			html.setWidth("500px");
		}

	}
	
	public static native String getUserAgent() /*-{
		return navigator.userAgent.toLowerCase();
	}-*/;

}
