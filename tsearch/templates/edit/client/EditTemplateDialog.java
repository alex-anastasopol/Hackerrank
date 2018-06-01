package ro.cst.tsearch.templates.edit.client;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

import org.gwt.mosaic.ui.client.Caption;
import org.gwt.mosaic.ui.client.Caption.CaptionRegion;
import org.gwt.mosaic.ui.client.ImageButton;
import org.gwt.mosaic.ui.client.WindowPanel;
import org.gwt.mosaic.ui.client.WindowPanel.WindowState;
import org.gwt.mosaic.ui.client.WindowPanel.WindowStateListener;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.RichTextAreaWithSpellChecker;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.gwt.components.client.ButtonWithTooltip;
import com.gwt.utils.client.Print;
import com.gwt.utils.client.RichEditorKeyHandler;
import com.gwt.utils.client.UtilsAtsGwt;

/**
 * @author cristi
 */
public class EditTemplateDialog extends SimplePanel implements ClickListener{
	
	private static int timeout = 90000;
	
	public static String REQUIREMENTS = "REQUIREMENTS";
	public static String EXCEPTIONS = "EXCEPTIONS";
	public static String LEGAL_DESC = "LEGAL_DESC";
	
	public static class MaxInactiveAsync implements AsyncCallback{
		public void onFailure(Throwable arg0) {	
		}
		public void onSuccess(Object arg0) {
			Integer i = (Integer)(arg0);
			timeout = i.intValue()*1000;
		}
	}
		
	static {
		TemplateEditClient.templateService.getMaxInactiveIntervalForTemplateEditing(new MaxInactiveAsync());
	}
	
	RichTextArea txt = null;
	String newpath = "";
	String templateName = "";
	int templateId;
	Label labelState = null;
	private boolean isElementEditor = false;
	private boolean isExpandedState = false;
	private boolean isStarter;
	private boolean showSavePanel = true;
	private String docDataSource = "";
	
	final private Vector<RichTextArea> allTextAreas = new Vector<RichTextArea>();
	
	private FlexTable table =null;
	
	private static final String submitMouseOver =  "Save and return to previous page";
	ButtonWithTooltip submit = ButtonWithTooltip.getInstance("Submit", this,submitMouseOver, "button", "gwt-toolTip", -1);
	
	private static final String applyMouseOver =  "Save and continue editing";
	ButtonWithTooltip apply =  ButtonWithTooltip.getInstance("Apply", this,applyMouseOver ,"button", "gwt-toolTip", -1 );
	
	private static final String uploadToSSfMouseOver =  "Upload to Stewart Starter Files";
	ButtonWithTooltip uploadToSSf =  ButtonWithTooltip.getInstance("Upload to SSF", this,uploadToSSfMouseOver ,"button", "gwt-toolTip", -1 );
	
	private static final String deleteMouseOver =  "Delete ALL changes and reset template to the initial state";
	ButtonWithTooltip delete =  ButtonWithTooltip.getInstance("Reset template", this, deleteMouseOver, "button", "gwt-toolTip", -1);
	
	private static final String cancelMouseOver =  "Cancel Changes Since Last Save";
	private static final String previewMouseOver = "Preview template";
	ButtonWithTooltip cancel =  ButtonWithTooltip.getInstance("Cancel Changes Since Last Save", this,cancelMouseOver, "button", "gwt-toolTip",  -1);	
	private static final String expandString =  "Expand all textboxes";
	private static final String colapseString =  "Collapse all textboxes";
	private static final String findOptionsString = "Show/hide the find options";
	private static final String findOptionsLabel = "Find keyword: ";
	ButtonWithTooltip expandButton =  ButtonWithTooltip.getInstance("Expand", this, expandString, "button", "gwt-toolTip", -1);
	ButtonWithTooltip colapseButton =  ButtonWithTooltip.getInstance("Collapse", this, colapseString, "button", "gwt-toolTip", -1);
	
	ButtonWithTooltip saveButtonUP =  ButtonWithTooltip.getInstance("Submit", this,submitMouseOver, "button", "gwt-toolTip",  -1);
	ButtonWithTooltip cancelButtonUP =  ButtonWithTooltip.getInstance("Cancel Changes Since Last Save", this,cancelMouseOver, "button", "gwt-toolTip",  -1);
	ButtonWithTooltip deleteButonUP =  ButtonWithTooltip.getInstance("Reset template", this, deleteMouseOver, "button", "gwt-toolTip", -1);
	
	ButtonWithTooltip applyUpButton =  ButtonWithTooltip.getInstance("Apply", this, applyMouseOver, "button", "gwt-toolTip", -1);
	ButtonWithTooltip uploadToSSfUp =  ButtonWithTooltip.getInstance("Upload to SSF", this,uploadToSSfMouseOver ,"button", "gwt-toolTip", -1 );
	
	ButtonWithTooltip previewButtonUp = ButtonWithTooltip.getInstance("Preview", this, previewMouseOver, "button", "gwt-toolTip", -1);
	ButtonWithTooltip previewButtonDown = ButtonWithTooltip.getInstance("Preview", this, previewMouseOver, "button", "gwt-toolTip", -1);
	ButtonWithTooltip expandButtonUP =  ButtonWithTooltip.getInstance("Expand", this, expandString, "button", "gwt-toolTip", -1);
	ButtonWithTooltip colapseButtonUP =  ButtonWithTooltip.getInstance("Collapse", this,colapseString, "button", "gwt-toolTip", -1);

	ButtonWithTooltip findOptionsButtonUP =  ButtonWithTooltip.getInstance("Find options", this,findOptionsString, "button", "gwt-toolTip", -1);
	TextBox findTextBox = new TextBox(); 
	VerticalPanel findOptionsPanel = new VerticalPanel();
	
	public final HotKeyBoilerPlatesHandler hotkey = new HotKeyBoilerPlatesHandler();
	
	private ArrayList<CheckBox> checkboxes = new ArrayList<CheckBox>();
	
	private int calculateVisibleLines(RichTextArea txt){
		
		String Html = txt.getHTML();
		int dim = txt.getOffsetWidth()/10+1; //Approximation
		
		String[] lines = Html.split("<br>");
		String[] lines1 = Html.split("<br/>");
		
		if(lines.length<lines1.length){
			lines = lines1;
		}
		
		int nrLines = 0;
		for(int i=0;i<lines.length;i++){
			String current = lines[i];
			int length = current.length();
			nrLines = nrLines  + (  ((int)((length+1)/dim)) + 1  );
		}
		
		nrLines+=2;
		
		if(nrLines<TemplateUtils.DIM_SMALL_TEXT_AREA){
			nrLines = TemplateUtils.DIM_SMALL_TEXT_AREA;
		}
		
		if(nrLines>TemplateUtils.DIM_LARGE_TEXT_AREA){
			nrLines = TemplateUtils.DIM_LARGE_TEXT_AREA;
		}
		
		return nrLines;
	}
	
	private void toggleTextAreas(boolean expand) {

		int rows = table.getRowCount();

		for (int i = 0; i < rows; i++) {
			try {
				Widget curent = table.getWidget(i, 0);
				if (curent instanceof ComplexPanel) {
					Iterator<Widget> it = ((ComplexPanel) curent).iterator();
					while (it.hasNext()) {
						Widget ta = it.next();
						if (ta instanceof RichTextArea) {
							int lines = 0;
							if (expand) {
								lines = calculateVisibleLines((RichTextArea) ta);
							} else {
								lines = TemplateUtils.DIM_SMALL_TEXT_AREA;
							}
							((RichTextArea) ta).setHeight(lines+"em");
						}
					}
				}
			} catch (Exception e) {
				continue;
			}
		}
	}
	
	private void collapseAll(){
		toggleTextAreas(false);
	}
	
	private void expandAll(){
		toggleTextAreas(true);
	}
	
	class TextAreaListener implements FocusListener,ClickListener{

		private int row=-1;
		private int col = -1;
		FlexTable table=null;
		Widget boilerPlatesComposite = null;
		TemplateComboList templateComboList = null;
		RichTextArea ttt = null;
		FindEvent findEv;
		
		TextAreaListener(int row,int col,FlexTable table, RichTextArea ttt,FindEvent findEv){
			this.row = row;
			this.col = col;
			this.table = table;
			this.ttt = ttt;
			this.findEv = findEv;
		}
		
		public void onClick(Widget arg0) { 
			perform(arg0,false); 
		}

		@Override
		public void onFocus(Widget sender) {
			perform(sender,true);
		}

		private void perform(Widget sender, boolean focus ){
			if(boilerPlatesComposite==null) {
				TemplateComboList tCombo1 = new TemplateComboList(ttt,TemplateUtils.dataFromBoilerPlates);;
				HorizontalPanel hpanel1 = tCombo1.getCompositeCombo();
				this.boilerPlatesComposite = hpanel1;
				this.templateComboList = tCombo1;
			}
			hotkey.setTa(sender);
			hotkey.setComboList(this.templateComboList);
			
			int dim_small = TemplateUtils.DIM_SMALL_TEXT_AREA;
			int dim_large = TemplateUtils.DIM_LARGE_TEXT_AREA;
			//Window.alert("out");
			if( (sender instanceof RichTextArea) ){
				//Window.alert("in");
				int rows = table.getRowCount();
				
				int i=0; 
				for(;i<rows;i++){	
					   try{
							Widget curent = table.getWidget(i, col);
							if(curent instanceof HorizontalPanel){
								break;
							}
					   }
					   catch(Exception e){
						   continue;
					   }
				}
				//remove boiler plate entry
				if(i!=rows){
					table.removeRow(i);
					rows = rows  -1;
				}
				
				i=0;
				for(;i<rows;i++){
					  try{
							Widget curent = table.getWidget(i, col);
							if(curent instanceof RichTextArea){
								if(isExpandedState){
									dim_small = calculateVisibleLines((RichTextArea)curent );
								}
								((RichTextArea)curent ).setHeight(dim_small+"em");
							}
					  }
					  catch(Exception e){
						   continue;
					   }
					
				}
				
				((RichTextArea)sender ).setHeight( dim_large+"em");
				table.insertRow( row );
				table.setWidget(row, col, boilerPlatesComposite);
				//Window.alert(boilerPlatesComposite+"");
						
			}
		}
		
		@Override
		public void onLostFocus(Widget sender) {
			// TODO Auto-generated method stub
		}

	}
	
	/**
	 * @param newpath		calea catre template-ul recent generat
	 * @param name			numele template-ului
	 * @param id 			template Id
	 * @param txtarea		Txt Area
	 * @param labelState	label-ul care identifica starea 
	 */
	public EditTemplateDialog(String newpath,String name,int id, RichTextArea txtarea,Label labelState, String statements){
		
		boolean readOnly = false;//B 4389
		//because of the GWT include mechanism I cannot include constants from TemplatesInitUtils
		if (name.contains("BoilerPlates") || name.contains("CBLibrary")){
			readOnly = true;
		}
		
		txtarea.setEnabled(!readOnly);
	
		this.newpath = newpath;
		this.txt = txtarea;
		this.labelState = labelState;
		this.templateName = name;
		this.templateId = id;
		this.isStarter = TemplateEditClient.isStarter();
		this.showSavePanel = TemplateEditClient.isShowSavePanel();
		this.docDataSource = TemplateEditClient.getDocDataSource();
		
		//		combo box section - init objects
		TemplateComboList tCombo = null;
		HorizontalPanel hpanel = null;
		
		//combo box section - set values
		tCombo = new TemplateComboList(txt,TemplateUtils.dataFromBoilerPlates);
		hpanel = tCombo.getCompositeCombo();
		
		//		creez un flow panel la care adaug butoanele
		HorizontalPanel fpanelUP = new HorizontalPanel();
		fpanelUP.add(previewButtonUp);
		fpanelUP.add(applyUpButton);
		fpanelUP.add(saveButtonUP);
		fpanelUP.add(new HTML("&nbsp;&nbsp;&nbsp;&nbsp;"));
		if(isStarter && docDataSource.equals("SF")){
			fpanelUP.add(uploadToSSfUp);
		}
		
		if(!docDataSource.equals("ATS")) {
			fpanelUP.add(cancelButtonUP);
			fpanelUP.add(deleteButonUP);
		}
		
	
		HorizontalPanel fpanelUP1 = new HorizontalPanel();
		fpanelUP1.add(expandButtonUP);
		fpanelUP1.add(colapseButtonUP);
		
		HorizontalPanel fpanelUP2 = new HorizontalPanel();
		fpanelUP2.add(findOptionsButtonUP);
		findTextBox.setWidth("50px");
		HorizontalPanel keyword = new HorizontalPanel();
		keyword.add(new Label(findOptionsLabel));
		keyword.add(findTextBox);
		findOptionsPanel.add(keyword);
		findOptionsPanel.setVisible(false);
		fpanelUP2.add(findOptionsPanel);
		
		DockPanel dockPanelUP = new DockPanel();
		if (!readOnly) {
			dockPanelUP.add(fpanelUP, DockPanel.WEST);
		}
		dockPanelUP.add(new HTML(TemplateUtils.EMPTY_SPACES),DockPanel.CENTER);
		dockPanelUP.add(fpanelUP2, DockPanel.EAST);
		dockPanelUP.add(fpanelUP1, DockPanel.EAST);
		
		HorizontalPanel fpanel = new HorizontalPanel();
		
		fpanel.add(previewButtonDown);
		fpanel.add(apply);
		
		fpanel.add(submit);
		fpanel.add(new HTML("&nbsp;&nbsp;&nbsp;&nbsp;"));
		if(isStarter && docDataSource.equals("SF")){
			fpanel.add(uploadToSSf);
		}
		if(!docDataSource.equals("ATS")) {
			fpanel.add(cancel);
			fpanel.add(delete);
		}
		
		HorizontalPanel fpanel1 = new HorizontalPanel();
		fpanel1.add(expandButton);
		fpanel1.add(colapseButton);
		
		DockPanel dockPanel = new DockPanel();
		if (!readOnly) {
			dockPanel.add(fpanel, DockPanel.WEST);
		}
		dockPanel.add(new HTML(TemplateUtils.EMPTY_SPACES),DockPanel.CENTER);
		dockPanel.add(fpanel1, DockPanel.EAST);
		
		Vector vec = EditorElement.getEditorElements(txtarea.getText(), false,false);
		
		DockPanel dock = new DockPanel();
		
		boolean isForEditorElemet = TemplateUtils.isForEditorElement( name );
		final TimerSaveListener timerSave = new TimerSaveListener(this);
		final FindEvent find = new FindEvent();
		boolean lastContainText = false;
		
		LinkedHashMap<String, String> reqMap = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> legalMap = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> excMap = new LinkedHashMap<String, String>();
		
		if(statements != null) {
			String[] statementGroups = UtilsAtsGwt.htmlCorrection(statements).split("#@#&069%3#");
		
			for(String stmtGroup : statementGroups) {
				String[] stmtGroupParts = stmtGroup.split("=", 2);
				String categ = stmtGroupParts[0];
				String[] stmts = stmtGroupParts[1].split("%@%314zda%");
				
				if(categ.equals(REQUIREMENTS)) {
					for(String stmt : stmts) {
						if(stmt.indexOf("=") > -1) {
							//String[] stmtParts = stmt.split("=");
							reqMap.put(stmt.substring(0, stmt.lastIndexOf("=")), stmt.substring(stmt.lastIndexOf("=") + 1));
						}
					}
				} else if(categ.equals(EXCEPTIONS)) {
					for(String stmt : stmts) {
						if(stmt.indexOf("=") > -1) {
							//String[] stmtParts = stmt.split("=");
							excMap.put(stmt.substring(0, stmt.lastIndexOf("=")), stmt.substring(stmt.lastIndexOf("=") + 1));
						}
					}
				} else if(categ.equals(LEGAL_DESC)) {
					for(String stmt : stmts) {
						if(stmt.indexOf("=") > -1) {
							//String[] stmtParts = stmt.split("=");
							legalMap.put(stmt.substring(0, stmt.lastIndexOf("=")), stmt.substring(stmt.lastIndexOf("=") + 1));
						}
					}
				}
			}
		}
		
		if(isForEditorElemet){
			
			table = new FlexTable();
			boolean legalsSection = false;
			boolean requirementsSection = false;
			boolean exceptionsSection = false;
			
			for(int i=0,k=0; i<vec.size();	i++){
				EditorElement element = (EditorElement )vec.get(i);
				
				if(element.isEditTable()){
					String html = element.getContent().trim().replaceAll("\r\n", "\n").replaceAll("\n", "<br>").replaceAll("\r", "<br>");
					
					String labelText = null;
					if(i > 0 && docDataSource.equals("ATS")) {
						labelText = ((EditorElement )vec.get(i - 1)).getContent().replaceAll("\\s+", " ").trim();
						
						if(labelText.matches(".*<Legal[^>]*>.*")) {
							legalsSection = true;
						}
						if(labelText.matches(".*</Legal[^>]*>.*")) {
							legalsSection = false;
						}
						
						if(labelText.matches(".*<Requirements[^>]*>.*")) {
							requirementsSection = true;
						}
						if(labelText.matches(".*</Requirements[^>]*>.*")) {
							requirementsSection = false;
						}
						
						if(labelText.matches(".*<Exceptions[^>]*>.*")) {
							exceptionsSection = true;
						}
						if(labelText.matches(".*</Exceptions[^>]*>.*")) {
							exceptionsSection = false;
						}
						
						labelText = labelText.replaceFirst("</[^>]*>", "");
					}
					if(i > 1 && docDataSource.equals("SF")) {
						labelText = ((EditorElement )vec.get(i - 2)).getContent();
						labelText = labelText.replaceAll("\\s+", " ");
					}	
					
					if(statements != null && labelText != null && 
							(labelText.matches(".*(Requirements|Exceptions|Legal Description).*")
									|| legalsSection || requirementsSection || exceptionsSection)) {
						html = UtilsAtsGwt.htmlCorrection(html);
						String[] paragraphs = html.split("(<br>){2,}");
						final FlexTable stmtTable = new FlexTable();
						int j = 1;
						int row = k;
						String stmt = "";
						boolean composedStmt = false;
						ArrayList<String> stmts = new ArrayList<String>();
						final String _labelText = labelText;
						final boolean _legalsSection = legalsSection;
						final boolean _requirementsSection = requirementsSection;
						final boolean _exceptionsSection = exceptionsSection;

						if(!table.isCellPresent(row, 0)) {
							table.setWidget( row, 0, new VerticalPanel());
						}
						Panel p = (Panel)table.getWidget(row, 0);	

						p.add(stmtTable);
						
						Button addStmtBut = new Button("Add Statement", new ClickHandler() {
							@Override
							public void onClick(ClickEvent event) {
								int row = stmtTable.getRowCount();
								CheckBox checkBox = new CheckBox();
								RichTextAreaWithSpellChecker ttt = prepareTextArea("", row, 1, stmtTable, find, timerSave);
								
								if(_labelText.contains("Requirements") || _requirementsSection) {
									checkBox.setName("req" + row);
								} else if(_labelText.contains("Exceptions") || _exceptionsSection) {
									checkBox.setName("exc" + row);
								} else if(_labelText.contains("Legal") || _legalsSection) {
									checkBox.setName("legal" + row);
								}
								checkBox.setValue(true);
								checkboxes.add(checkBox);
								stmtTable.setWidget(row, 0, checkBox);
								stmtTable.setWidget(row, 1, ttt);
							}
						});
						addStmtBut.setStyleName("button");
						stmtTable.setWidget(0, 1, addStmtBut);
						
						final CheckBox bigCheckbox = new CheckBox();
						bigCheckbox.setStyleName("checkbox");
						
						if(_labelText.contains("Requirements") || _requirementsSection) {
							bigCheckbox.setName("req_all");
						} else if(_labelText.contains("Exceptions") || _exceptionsSection) {
							bigCheckbox.setName("exc_all");
						} else if(_labelText.contains("Legal") || _legalsSection) {
							bigCheckbox.setName("legal_all");
						}
						bigCheckbox.addClickHandler(new ClickHandler() {
							@Override
							public void onClick(ClickEvent event) {
								String name = bigCheckbox.getName();
								name = name.substring(0, name.indexOf("_"));
								
								int row = stmtTable.getRowCount();
								for (int i = 1; i < row; i++){
									Widget widg = stmtTable.getWidget(i, 0);
									if (widg instanceof CheckBox){
										CheckBox checkbox = (CheckBox)widg;
										if (checkbox.getName().startsWith(name)){
											checkbox.setValue(bigCheckbox.getValue());
										}
										doTheSameActionForSameStatementFromAnotherZone(checkbox, stmtTable.getWidget(i, 1));
									}
								}
							}
						});

						stmtTable.setWidget(0, 0, bigCheckbox);
						
						// parse the statements
						// the code below HAS TO MATCH the code from the method splitTextIntoStatements in SelectableStatement class,
						// otherwise the statements won't match
						for(String para : paragraphs) {
							para = para.replace("<A href=", "<a href=").replace("<A HREF=", "<a href=").replaceAll("</A>", "</a>");
							
							if(para.contains("href='")) {
								para = para.replaceAll("href='([^']+)'", "href=\"$1\"");
							}
							
							if(para.endsWith(":")) {
								if(!stmt.equals("")) {
									stmts.add(stmt);
									stmt = "";
								}
								composedStmt = true;
								stmt += para;
							} else if(composedStmt && para.trim().matches("[a-z1-9]\\).*")) {
								stmt += "<br>" + para;
							} else {
								if(!stmt.equals("")) {
									stmts.add(stmt);
									stmt = "";
									composedStmt = false;
								}
								if(!para.equals("")) {
									stmts.add(para);
								}
							}
						}
						
						for(String st : stmts) {
							final CheckBox checkBox = new CheckBox();
							
							if(labelText.contains("Requirements") || requirementsSection) {
								checkBox.setName("req" + j);
							} else if(labelText.contains("Exceptions") || exceptionsSection) {
								checkBox.setName("exc" + j);
							} else if(labelText.contains("Legal") || legalsSection) {
								checkBox.setName("legal" + j);
							}
							
							checkBox.addClickHandler(new ClickHandler() {
								@Override
								public void onClick(ClickEvent event) {									
									int row = stmtTable.getRowCount();
									for (int i = 1; i < row; i++){
										Widget widgCheckBox = stmtTable.getWidget(i, 0);
										
										if (checkBox.equals(widgCheckBox)){
											Widget widg = stmtTable.getWidget(i, 1);
											doTheSameActionForSameStatementFromAnotherZone(checkBox, widg);
											break;
										}
									}
									
								}
							});
							
							RichTextAreaWithSpellChecker ttt = prepareTextArea(st, j, 1, stmtTable, find, timerSave);
							
							stmtTable.setWidget(j, 0, checkBox);
							stmtTable.setWidget(j, 1, ttt);
							
							st = st.replace("<br>", "\n").replace("<BR>", "\n").trim();
							
							if((labelText.contains("Requirements") || requirementsSection)) {
								
								String value = reqMap.get(st);
								if(value == null) {
									value = reqMap.get(TemplateUtils.revertStringFromHtmlEncoding(st));
								}
								if(value == null) {
									value = "false";
								}
								checkBox.setValue(new Boolean(value));
								
//								if(reqMap.containsKey(st)) {
//									checkBox.setValue(new Boolean(reqMap.get(st)));
//								} else {
//									checkBox.setValue(false);
//								}
								
							} else if((labelText.contains("Exceptions") || exceptionsSection)) {
								
								String value = excMap.get(st);
								if(value == null) {
									value = excMap.get(TemplateUtils.revertStringFromHtmlEncoding(st));
								}
								if(value == null) {
									value = "false";
								}
								checkBox.setValue(new Boolean(value));
								
//								if(excMap.containsKey(st)) {
//									checkBox.setValue(new Boolean(excMap.get(st)));
//								} else {
//									checkBox.setValue(false);
//								}
							} else if((labelText.contains("Legal") || legalsSection)) {
								
								String value = legalMap.get(st);
								if(value == null) {
									value = legalMap.get(TemplateUtils.revertStringFromHtmlEncoding(st));
								}
								if(value == null) {
									value = "false";
								}
								checkBox.setValue(new Boolean(value));
								
//								if(legalMap.containsKey(st)) {
//									checkBox.setValue(new Boolean(legalMap.get(st)));
//								} else {
//									checkBox.setValue(false);
//								}	
							} else {
								checkBox.setValue(false);
							}
							checkboxes.add(checkBox);
							j++;
						}
						
						lastContainText = false;
					} else {
						int row = k;

						if(!table.isCellPresent(row, 0)) {
							table.setWidget( row, 0, new VerticalPanel());
						}
						Panel p = (Panel)table.getWidget(row, 0);	

						RichTextAreaWithSpellChecker ttt = prepareTextArea(html, row, 0, table, find, timerSave);
						
						p.add(ttt);

						lastContainText = false;
					}
				}
				else{
					
					String content = element.getContent();
					
					lastContainText = content.toUpperCase().contains("<TEXT>");
					
					String labelText = "";
					content = content.replaceAll("\\s+", " ").trim();
					if(content.matches(".*<[^/][^>]*>")) { // field is element
						labelText = "<b>" + content.replaceFirst(".*<([^\\s>]*).*>", "$1") + "</b>";
					} else { // field is attribute
						String[] tokens = content.replaceFirst (".*[<>]", "")
								.replaceAll("[\"=]", "")
								.trim().split("\\s+", 2);
						
						if(tokens.length == 2) {
							if(tokens[0].equals(tokens[1])) {
								labelText = "<b>" + tokens[0] + "</b>";
							} else {
								labelText = "<b>" + tokens[0] + "</b><br>" + tokens[1];
							}
						} else {
							if(tokens[0].matches("\\s*") && !content.matches(".*</[^>]*>")) {
								labelText = element.getContent();
							} else {
								labelText = tokens[0];
							}
						}
					}
					
					if (isStarter && "SF".equals(docDataSource)) {
						labelText = "<span style=\"background-color:#FFCCFF\">" + labelText + "</span>";
					}
					
					HTML label = new HTML( "<input type=\"hidden\" value=\"" + URL.encode(element.getContent()) + "\" />" + labelText) ;
					
					String curExt = TemplateUtils.getFileExtension(newpath);
					
					if(  curExt.toLowerCase().equals(TemplateUtils.TemplateExtensions.ATS_SMALL_EXTENSION)  ) {
						label.setWidth("30em");
					}
					else if( curExt.toLowerCase().equals(TemplateUtils.TemplateExtensions.PXT_SMALL_EXTENSION)  ){
						label.setWidth("10em");
					}
					
					label.setWordWrap(true);
					table.setWidget(k, 0,label );
				}
				
				k++;
			}
				
			this.isElementEditor = true;
			
			dock.add(dockPanelUP, DockPanel.NORTH);
			dock.setCellHorizontalAlignment(dockPanelUP,DockPanel.ALIGN_CENTER);	
			
			if (!readOnly) {
				dock.add(dockPanel, DockPanel.SOUTH);
				dock.setCellHorizontalAlignment(dockPanel,DockPanel.ALIGN_CENTER);
			}
									
			dock.add(table, DockPanel.CENTER);
			dock.setCellHorizontalAlignment(table,DockPanel.ALIGN_CENTER);	
			
		}
		else{	
			if (!readOnly) {
				dock.add(fpanel, DockPanel.SOUTH);
				
				//combo box panel
				dock.add(hpanel,DockPanel.NORTH);
				dock.setCellHorizontalAlignment(hpanel,DockPanel.ALIGN_RIGHT);
			}
			txt.setHTML(txt.getHTML().replaceAll("\r\n", "\n").replaceAll("\n", "<br>"));
			dock.add( txt, DockPanel.CENTER);
			
			//setari de afisaj
			if (!readOnly) {
				dock.setCellHorizontalAlignment(fpanel, DockPanel.ALIGN_CENTER);
			}
			dock.setCellWidth(txt, "100%");
			dock.setWidth("100%");
			
			this.txt.setWidth("85em");
			this.txt.setHeight("40em");
		}

		find.setAllTextAreas(allTextAreas);
		findTextBox.setText(find.getSearched());
		find.setFindTextBox(findTextBox);
	    DOM.addEventPreview(find);
	    hotkey.setEventPreview(find);
		//adaug dockpanel la EditDialog
		super.setWidget(dock);
		saveTemplate(false, false, null, null, false, true, false);
	}
		
	/**
	 * @param checkBox - the checkbox clicked by the user
	 * @param widg - checkbox for a statement equals to the statement of the clicked checkbox
	 */
	public void doTheSameActionForSameStatementFromAnotherZone(final CheckBox checkBox, Widget widg) {
		if (widg instanceof RichTextAreaWithSpellChecker){
			String name = checkBox.getName();
			name = name.substring(0, 3);
			String html = ((RichTextAreaWithSpellChecker)widg).getHTML();
			
			if (table != null){
				Iterator it = table.iterator();
				while (it.hasNext()){
					Object curent = it.next();
					
					if (curent instanceof ComplexPanel) {
						Iterator<Widget> it1 = ((ComplexPanel) curent).iterator();
						while (it1.hasNext()) {
							Widget wid = it1.next();
							if (wid instanceof FlexTable) { // we have multiple statements
								FlexTable newStmtTable = (FlexTable) wid;
								
								for (int rows = 0; rows < newStmtTable.getRowCount(); rows++) {
									if (newStmtTable.getCellCount(rows) > 1 && newStmtTable.getWidget(rows, 1) instanceof RichTextArea) {
										CheckBox newCheckBox = (CheckBox) newStmtTable.getWidget(rows, 0);
										RichTextArea rta = (RichTextArea) newStmtTable.getWidget(rows, 1);
										String content = rta.getHTML();
//										System.out.println(content);
										String newName = newCheckBox.getName();
										if (content.equals(html) && newName.startsWith(name)){
											newCheckBox.setValue(checkBox.getValue());
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	private RichTextAreaWithSpellChecker prepareTextArea(String html, int row, int col, 
			FlexTable stmtTable, FindEvent find, TimerSaveListener timerSave) {
		html = UtilsAtsGwt.cleanAfterLinkEditor(html,true);

		if(UtilsAtsGwt.getUserAgent().contains("msie")) {
			html = html.replaceAll("mihai_si_cristi_au_plecat_in_concediu_si_eu_am_ramas_sa_rezolv_bugurile", "\t");
		}
		//these must be always &nbsp; to be correctly displayed in browsers. on saving the &nbsp; must be transformed in spaces " "
		//this must be done here because in UtilsAtsGwt.cleanAfterLinkEditor(html,true) the &nbsp; are transformed in spaces
		html = html.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");

		//					html = processStatements(html, )

		RichTextAreaWithSpellChecker ttt = new RichTextAreaWithSpellChecker();
		ttt.setStyleName("txtarea");
		ttt.setStylePrimaryName("txtarea");
		allTextAreas.add(ttt);
		ttt.setHeight(TemplateUtils.DIM_SMALL_TEXT_AREA+"em");
		String curExt = TemplateUtils.getFileExtension(newpath);	

		if(  curExt.toLowerCase().equals(TemplateUtils.TemplateExtensions.ATS_SMALL_EXTENSION)||"".equalsIgnoreCase(curExt)) {
			ttt.setWidth(TemplateUtils.NR_COLS_IN_EDITOR_FOR_ATS+"em");
			ttt.setHTML(html);
		}
		else if( curExt.toLowerCase().equals(TemplateUtils.TemplateExtensions.PXT_SMALL_EXTENSION)  ){
			ttt.setWidth(TemplateUtils.NR_COLS_IN_EDITOR_FOR_PXT+"em");
			ttt.setHTML( html);
		}
		else{
			ttt.setHTML( html );
		}

		ttt .addClickListener(new TextAreaListener(row,col,stmtTable,ttt,find));
		ttt.addFocusListener(new TextAreaListener(row,col,stmtTable,ttt,find));
		ttt.addKeyboardListener(timerSave);
		ttt.addKeyDownHandler(hotkey);
		ttt.addKeyDownHandler(new RichEditorKeyHandler(ttt));
		ttt.addKeyUpHandler(new RichEditorKeyHandler(ttt));
		
		return ttt;
	}

	/**
	 * apelata cand se apasa unul din cele trei butoane (Save, Delete , Cancel)
	 */
	public void onClick(Widget sender) {
		Button but = (Button ) sender;
		String policyName = TemplateEditClient.getPolicyName();
		boolean force = false;
		
		if( but.getText().indexOf("Apply")>=0){
			but.setText( "Saving..." );
			disableButtons();
			saveTemplate( false, true, but, "Apply", false, false , false);
		}
		else if( but.getText().indexOf("Submit")>=0){
			but.setText( "Saving..." );
			disableButtons();
			saveTemplate( true, false, but, "Submit" , false,false , false);
		}
		else if( but.getText().indexOf("Reset template")>=0){
			if (Window.confirm("Are you sure you want to reset the template to its original form and lose any changes made ?")) {
				force=true;
				String message = "</div><div>The <b>Template " +  "&lt;" + policyName + "&gt;</b> was reset to the original form ";
				TemplateEditClient.templateService.logMessage(
						TemplateEditClient.getSearchId(), 
						message,
							new AsyncCallback<Void>() {

								public void onFailure(Throwable arg0) {
									Window.alert(arg0.getMessage());
								}

								public void onSuccess(Void arg0) {
									//Window.alert("The changes since last save were canceled");
								}
							});
				TemplateEditClient.templateService.deleteGeneratedTemplate(TemplateEditClient.getSearchId(),TemplateEditClient.getCurrentUserId(),newpath,templateId,force,new DeleteButtonAsync(labelState,templateName,templateId));
			}
		}
		else if(  (but.getText().indexOf("Cancel Changes")>=0)  ){
			String message = "</div><div><b>Template " +  "&lt;" + policyName + "&gt;</b> changes were canceled (reverted to last save) ";
			TemplateEditClient.templateService.logMessage(
					TemplateEditClient.getSearchId(), 
					message,
						new AsyncCallback<Void>() {

							public void onFailure(Throwable arg0) {
								Window.alert(arg0.getMessage());
							}

							public void onSuccess(Void arg0) {
								//Window.alert("The changes since last save were canceled");
							}
						});
			
			TemplateEditClient.templateService.restoreTempBackup(
						TemplateEditClient.getSearchId(),TemplateEditClient.getCurrentUserId(),this.templateName, this.templateId,
							new AsyncCallback() {
	
								public void onFailure(Throwable arg0) {
									Window.alert(arg0.getMessage());
								}
	
								public void onSuccess(Object arg0) {
									TemplateEditClient.destroy();
								}
							});
		}else if(but.getText().indexOf("Preview")>=0) { 
			
			previewTemplate();
		}
		else if(  (but.getText().indexOf("Expand")>=0)  ){
			isExpandedState = true;
			expandAll();
		}
		else if(  (but.getText().indexOf("Collapse")>=0)  ){
			isExpandedState = false;
			collapseAll();
		}
		else if( (but.getText().indexOf("Find options")>=0) ){
			findOptionsPanel.setVisible(!findOptionsPanel.isVisible());
		}else if( (but.getText().toUpperCase().indexOf("SSF")>=0)&&(but.getText().toUpperCase().indexOf("UPLOAD")>=0) ){
			disableButtons();
			but.setText( "Uploading..." );
			saveTemplate( false, true, but, "Upload to SSF", false, false , true);
		}
	}
	
	public void previewTemplate(){
			
		StringBuffer strBuff = new StringBuffer ("");
		if(table!=null){
			Iterator<Widget>  it = table.iterator();
			while(it .hasNext()){
				Widget curent = it.next();
				if (curent instanceof ComplexPanel) {
					Iterator<Widget> it1 = ((ComplexPanel) curent).iterator();
					while (it1.hasNext()) {
						Widget ta = it1.next();
						if (ta instanceof RichTextArea) {	
							String curExt = TemplateUtils.getFileExtension(newpath);	
							if(  curExt.toLowerCase().equals(TemplateUtils.TemplateExtensions.ATS_SMALL_EXTENSION)||"".equalsIgnoreCase(curExt)  ) {
								strBuff.append(     
										TemplateUtils.cleanStringForAIM(((RichTextArea)ta).getHTML(),true/* replace & if should */, true /* ignore escaped */)
								);
							}
							else if( curExt.toLowerCase().equals(TemplateUtils.TemplateExtensions.PXT_SMALL_EXTENSION)  ){
								String curText = ( (RichTextArea)ta).getHTML()   ;
								strBuff.append( curText  );
								strBuff.append("\n");
							}
						} else if (ta instanceof FlexTable){//for statements with checkboxes
							Iterator<Widget> it2 = ((FlexTable) ta).iterator();
							while (it2.hasNext()) {
								Widget rta = it2.next();
								if (rta instanceof RichTextArea) {
									strBuff.append(     
											TemplateUtils.cleanStringForAIM(((RichTextArea)rta).getHTML(),true/* replace & if should */, true /* ignore escaped */)
									);
									strBuff.append("\n");
								}
							}
						}
					}
				}
				else if(curent  instanceof HTML){
					String label = ((HTML)curent).getHTML();
					if(label.matches("<(input|INPUT).*(value|VALUE)=\"?([^\\s\">]*).*")) {
						label = label.replaceAll("\\s+", " ").replaceFirst("<(input|INPUT).*(value|VALUE)=\"?([^\\s\">]*).*", "$3");
						label = URL.decode(label);
						strBuff.append(label);
					}
				}
			}
		}
		
		String content = strBuff.toString();
		
		TemplateEditClient.templateService.previewTemplate(templateName, content,TemplateEditClient.getSearchId(), new AsyncCallback<String>() {

			public void onFailure(Throwable caught) { Window.alert("Cannot preview template:" + caught.getMessage()); }

			public void onSuccess(final String result) {
				
				try {				
					String res = result.replaceAll("<div>", "<div style='width:"+(3*Window.getClientWidth()/4-150)+"px'>");
					
					HorizontalPanel hp = new HorizontalPanel();
					HorizontalPanel left = new HorizontalPanel();
					HorizontalPanel right = new HorizontalPanel();
					final HTML htmlPreview = new HTML(res);
					
					//for IE (task 5225, comment 7)
					String html = htmlPreview.getHTML();
					html = html.replaceAll("&lt;", "<");
					html = html.replaceAll("&LT;", "<");
					html = html.replaceAll("&gt;", ">");
					html = html.replaceAll("&GT;", ">");
					htmlPreview.setHTML(html);
					
					right.add(htmlPreview);
					left.setWidth("50px");
					hp.add(left);
					hp.add(right);
					ScrollPanel sp = new ScrollPanel(hp);
					right.setWidth( 3*Window.getClientHeight()/4-130 + "px");
					
					final WindowPanel previewWindow = new WindowPanel();
					final int width = 3*Window.getClientWidth()/4+10;
					final int height = 3*Window.getClientHeight()/4+50;
					previewWindow.setPixelSize(width, height);
					previewWindow.setCaption("Preview");
					previewWindow.add(sp);
					
					final ImageButton datachedBtn = new ImageButton(new Image("/title-search/web-resources/images/pop_up.gif"));
					
					datachedBtn.addClickHandler(new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							openPreviewWindow(htmlPreview, width, height);
							previewWindow.hide();
						}
					});
					 
					final ImageButton printBtn = new ImageButton(new Image("/title-search/web-resources/images/ico_square_prn.gif"));
	
	
					printBtn.addClickListener(new ClickListener() {
						public void onClick(Widget sender) {
							Print.it("<link rel='StyleSheet' type='text/css' media='paper' href='/title-search/web-resources/css/tsrindex/tsrindex.css'>", htmlPreview.toString());
						}
					});
					addMaximizeButton(previewWindow, CaptionRegion.RIGHT);
					previewWindow.getHeader().add(printBtn, CaptionRegion.RIGHT);
					previewWindow.getHeader().add(datachedBtn, CaptionRegion.LEFT);
					
					goToWindowTop();
					previewWindow.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
						public void setPosition(int offsetWidth, int offsetHeight) {
							int left = 50;
							int top = 50;
							previewWindow.setPopupPosition(left, top);
						}
					});
					
					previewWindow.addWindowResizeListener(new WindowResizeListener() {
						public void onWindowResized(int width, int height) {
							String res = result.replaceAll("<div>", "<div style='width:"+(width-160)+"px'>");
							htmlPreview.setHTML(res);
						}					
					});
				}catch(Exception e) {
					Window.alert(e.getMessage());
				}
			}
			
		});
	}
	
	public void saveTemplate( boolean andClose, boolean showMessage, Button but, String newButText, boolean autoSave, boolean onlyBackup, boolean uploadToSSf){
		StringBuffer strBuff = new StringBuffer ("");
		HashMap<String, HashMap<String, Boolean>> stmtMap = new HashMap<String, HashMap<String, Boolean>>();
		HashMap<String, Boolean> reqMap = new HashMap<String, Boolean>();
		HashMap<String, Boolean> legalMap = new HashMap<String, Boolean>();
		HashMap<String, Boolean> excMap = new HashMap<String, Boolean>();
		
		stmtMap.put(REQUIREMENTS, reqMap);
		stmtMap.put(EXCEPTIONS, excMap);
		stmtMap.put(LEGAL_DESC, legalMap);
		
		if(table!=null){
			Iterator  it = table.iterator();
			while(it .hasNext()){
				Object curent = it.next();
				
				if (curent instanceof ComplexPanel) {
					Iterator<Widget> it1 = ((ComplexPanel) curent).iterator();
					while (it1.hasNext()) {
						Widget wid = it1.next();
						if (wid instanceof RichTextArea) {	
							strBuff.append("<!--");
							String curExt = TemplateUtils.getFileExtension(newpath);	
							if(  curExt.toLowerCase().equals(TemplateUtils.TemplateExtensions.ATS_SMALL_EXTENSION)||"".equalsIgnoreCase(curExt)  ) {
								String content = ((RichTextArea)wid).getHTML();
								content = cleanContentForATS(content);
								strBuff.append(     
										TemplateUtils.cleanStringForAIM(content,true/* replace & if should */, true /* ignore escaped */)
								);
							}
							else if( curExt.toLowerCase().equals(TemplateUtils.TemplateExtensions.PXT_SMALL_EXTENSION)  ){
								String curText = ( (RichTextArea)wid).getHTML()   ;
								curText = cleanContentForPXT(curText);
								strBuff.append( curText  );
							}
							if( curExt.toLowerCase().equals(TemplateUtils.TemplateExtensions.PXT_SMALL_EXTENSION)  ){
								strBuff.append("-->\n");
							}
							else{
								strBuff.append("-->");
							}
						} else if(wid instanceof FlexTable) { // we have multiple statements
							FlexTable stmtTable = (FlexTable) wid;
							strBuff.append("<!--");
							String curExt = TemplateUtils.getFileExtension(newpath);
							
							for(int row = 0; row < stmtTable.getRowCount(); row++) {
								if(stmtTable.getCellCount(row) > 1 && stmtTable.getWidget(row, 1) instanceof RichTextArea) {
									CheckBox checkBox = (CheckBox) stmtTable.getWidget(row, 0);
									RichTextArea rta = (RichTextArea) stmtTable.getWidget(row, 1);
									String content = rta.getHTML();
										
									if(  curExt.toLowerCase().equals(TemplateUtils.TemplateExtensions.ATS_SMALL_EXTENSION)||"".equalsIgnoreCase(curExt)  ) {
										content = cleanContentForATS(content);
										strBuff.append(     
												TemplateUtils.cleanStringForAIM(content,true/* replace & if should */, true /* ignore escaped */)
										);
									}
									else if( curExt.toLowerCase().equals(TemplateUtils.TemplateExtensions.PXT_SMALL_EXTENSION)  ){
										String curText = ( (RichTextArea)wid).getHTML()   ;
										curText = cleanContentForPXT(curText);
										strBuff.append( curText  );
									}
									strBuff.append("\n\n");
									content = content.trim();
									
									if(checkBox.getName().contains("req")) {
										reqMap.put(content, checkBox.getValue());
									} else if(checkBox.getName().contains("exc")) {
										excMap.put(content, checkBox.getValue());
									} else if(checkBox.getName().contains("legal")) {
										legalMap.put(content, checkBox.getValue());
									}
								}
							}
							
							if( curExt.toLowerCase().equals(TemplateUtils.TemplateExtensions.PXT_SMALL_EXTENSION)  ){
								strBuff.append("-->\n");
							}
							else{
								strBuff.append("-->");
							}
						}
					}
				}
				else if(curent  instanceof HTML){
					String label = ((HTML)curent).getHTML();
					if(label.matches("<(input|INPUT).*(value|VALUE)=\"?([^\\s\">]*).*")) {
						label = label.replaceAll("\\s+", " ").replaceFirst("<(input|INPUT).*(value|VALUE)=\"?([^\\s\">]*).*", "$3");
						label = URL.decode(label);
						strBuff.append(label);
					}
				}
			}
		}
		final String content = strBuff.toString();
		final AsyncCallback callBack = new SaveButtonAsync ( labelState, templateName, templateId, andClose, showMessage, uploadToSSf,but, newButText  );
		
		//apel RPC 
		if(!autoSave) {
			if(!onlyBackup) {
				TemplateEditClient.templateService.saveTemplate(
						TemplateEditClient.getSearchId(),
						TemplateEditClient.getCurrentUserId(),
						this.templateName,
						this.templateId,
						isElementEditor?content:txt.getText(),
						newButText, 
						true,
						stmtMap,
						callBack);
			} else {
				TemplateEditClient.templateService.makeTempBackup(
						TemplateEditClient.getSearchId(),
						TemplateEditClient.getCurrentUserId(),
						this.templateName,
						this.templateId,
						isElementEditor?content:txt.getText(),
						callBack);
			}
		} else if(!onlyBackup) {
			TemplateEditClient.templateService.saveTemplate(
					TemplateEditClient.getSearchId(),
					TemplateEditClient.getCurrentUserId(),
					this.templateName,
					this.templateId,
					isElementEditor?content:txt.getText(),
					newButText, 
					false,
					stmtMap,
					callBack);
		}
	}

	private String cleanContentForPXT(String curText) {
		if(curText .startsWith(">")){
			curText  = " "+curText  ;
		}
		if(curText .endsWith("<")){
			curText  = curText  + " ";
		}
		
		curText = curText.replaceAll("\r\n", "\n").replaceAll("\n", "");
		curText = curText.replaceAll("<br>", "\n");
		curText = curText.replaceAll("<br/>", "\n");
		curText = curText.replaceAll("<BR>", "\n");
		curText = curText.replaceAll("<BR/>", "\n");
		curText = curText.replaceAll("<P/>", "");
		curText = curText.replaceAll("<P>", "");
		curText = curText.replaceAll("<p/>", "");
		curText = curText.replaceAll("<p>", "");

		//&nbsp; must be transformed in spaces when saving
		curText = curText.replaceAll("&amp;nbsp;", "&nbsp;");
		curText = curText.replaceAll("&nbsp;", " ");
		curText = UtilsAtsGwt.cleanAfterLinkEditor(curText,false);
		
		return curText;
	}

	private String cleanContentForATS(String content) {
		content = content.replaceAll("\r\n", "\n").replaceAll("\n", " ");
		content = content.replaceAll("<br>", "\n");
		content = content.replaceAll("<br/>", "\n");
		content = content.replaceAll("<BR>", "\n");
		content = content.replaceAll("<BR/>", "\n");
		content = content.replaceAll("</P>","\n");
		content = content.replaceAll("</p>","\n");
		//6 &nbsp; means tab \t
		content = content.replaceAll("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", "\t");
		content = content.replaceAll("&nbsp;", " ");
		
		content = content.replaceAll("(\\.\\./)+DocumentDataRetreiver", TemplateEditClient.ATS_CURRENT_SERVER_LINK_FOR_DOC_RETREIVER);
		
		content = UtilsAtsGwt.cleanAfterLinkEditor(content,false);
		
		return content;
	}

	public static int getTimeout() {
		return timeout;
	}
	
	private void addMaximizeButton(final WindowPanel windowPanel, CaptionRegion captionRegion) {
		final ImageButton maximizeBtn = new ImageButton(Caption.IMAGES.windowMaximize());
	    maximizeBtn.addClickListener(new ClickListener() {
	      public void onClick(Widget sender) {
	        if (windowPanel.getWindowState() == WindowState.MAXIMIZED) {
	          windowPanel.setWindowState(WindowState.NORMAL);
	        } else {
	          windowPanel.setWindowState(WindowState.MAXIMIZED);
	        }
	      }
	    });
	    windowPanel.addWindowStateListener(new WindowStateListener() {
	    	public void onWindowStateChange(WindowPanel sender,
				WindowState oldWindowState, WindowState newWindowState) {
	        if (sender.getWindowState() == WindowState.MAXIMIZED) {
		          maximizeBtn.setImage(Caption.IMAGES.windowRestore().createImage());
		        } else {
		          maximizeBtn.setImage(Caption.IMAGES.windowMaximize().createImage());
		        }
		      }
	    });
	    windowPanel.getHeader().add(maximizeBtn, captionRegion);
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
	
	public static native String getUserAgent() /*-{
		return navigator.userAgent.toLowerCase();
	}-*/;
	
	public static native void goToWindowTop() /*-{
		$wnd.scrollTo(0,0);
	}-*/;
	
	public static native void openPreviewWindow(HTML htmlPreview, int wid, int heig)/*-{
		var win = $wnd.open('', '', 'width=(wid),height=(heig), left=0,top=0,toolbar=1,resizable=1,scrollbars=yes');
		win.document.open("text/html", "replace");
		win.document.write(htmlPreview);
		win.document.close();
	}-*/;

	}

