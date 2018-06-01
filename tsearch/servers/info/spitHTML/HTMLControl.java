package ro.cst.tsearch.servers.info.spitHTML;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.parentsitedescribe.ComboValue;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class HTMLControl extends HTMLObject {

	public static final int HTML_TEXT_FIELD    = 0;
	public static final int HTML_RADIO_BUTTON  = 1;
	public static final int HTML_CHECK_BOX     = 2;
	public static final int HTML_SELECT_BOX    = 3;
	public static final int HTML_LABEL         = 4;
	public static final int HTML_ACTION_TEXT   = 5;
	public static final int HTML_SELECT_LIST   = 6;
	public static final int HTML_BUTTON   = 7;
	private LinkedList<ComboValue> comboList = null;
	
	public static final Pattern CHECKBOX_VALUE_PATTERN = Pattern.compile("(?is)\\bvalue\\s*=[\\s'\"]*([^'\"]+)['\"]*");
	
	public static class FieldType {
		
		public static final FieldType 	NOT_EMPTY = new FieldType("/^.+$/", " not empty"),
								  	NUMERIC = new FieldType("/^\\d+$/", " numeric"),
								  	NO_SPACES = new FieldType("/^[^ ]+$/", " without spaces"),
								  	EMAIL = new FieldType("/^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})$/", " email format"),
								  	DATE = new FieldType("/^[01]{1}[0-9]{1}\\/[0123]{1}[0-9]{1}\\/[12]{1}[0789]{1}\\d\\d$/", " date format mm/dd/yyyy");
		
		
		private String jsRegex;
		private String reqFormat;
		
		public FieldType(String jsRegex, String reqFormat)
		{

			this.jsRegex = jsRegex;
			this.reqFormat = reqFormat;
		}
		
		public boolean equals(FieldType t)
		{
			return jsRegex.equals(t.getJsRegex());
		}

		/**
		 * @return Returns the jsRegex.
		 */
		public String getJsRegex() {
			return jsRegex;
		}

		/**
		 * @param jsRegex The jsRegex to set.
		 */
		public void setJsRegex(String jsRegex) {
			this.jsRegex = jsRegex;
		}

		/**
		 * @return Returns the reqFormat.
		 */
		public String getReqFormat() {
			return reqFormat;
		}

		/**
		 * @param reqFormat The reqFormat to set.
		 */
		public void setReqFormat(String reqFormat) {
			this.reqFormat = reqFormat;
		}
	}

	private FieldType fieldContentType;
	
	private int controlType = -1;
	private Object defaultValue = null;
	private String fieldNote = "";
	private boolean valueRequired = false;
	private boolean requiredExcl = false;
	private boolean requiredCritical = false;
    private boolean horizontalRadioButton = false;
	private boolean justifyField = false;
//private String restriction= "";
	private boolean hidenparam=false;
    private String radioDefaultChecked = "";
	private TSServerInfoFunction currentTSSiFunc;
	private String JSFunction = "";
	private List<String> eventHandlers = null;
	private String extraClass = "";
	private boolean defaultOnReplicate = false;
	
	public void addEventHandler(String eventHandler){
		if(eventHandlers == null){
			this.eventHandlers = new LinkedList<String>();
		}
		eventHandlers.add(eventHandler);
	}
	
	private long searchId=-1;
	public HTMLControl(
			int controlType,
			int colStart,
			int colEnd,
			int rowStart,
			int rowEnd,
			int size,
			TSServerInfoFunction tssFunction,
			String name,
			String label,
			Object defaultValue, long searchId			
			)
			throws FormatException
	{	
		this(controlType, name, label, colStart, colEnd, rowStart, rowEnd, size, defaultValue, tssFunction, searchId);
		setJustifyField(true);
	}

	public static HTMLControl createHiddenHTMLControl(
			TSServerInfoFunction tssFunction,
			String name,
			String label,
			Object defaultValue, long searchId			
			)
			throws FormatException
	{	
		HTMLControl control = new HTMLControl(HTML_TEXT_FIELD, name, label, 1, 1, 1, 1, 1, defaultValue, tssFunction, searchId);
		control.setJustifyField(true);
		control.setHiddenParam(true);
		return control;
	}
	
	public HTMLControl(
			int controlType,
			String name,
			String label,
			int colStart,
			int colEnd,
			int rowStart,
			int rowEnd,
			int size,
			Object defaultValue,
			TSServerInfoFunction tssFunction, long searchId)
			throws FormatException
	{
		this.searchId = searchId;
		if(defaultValue!=null){
			tssFunction.setDefaultValue(defaultValue.toString());
		}
		
		this.controlType = controlType;
		this.name = name;
		this.label = label;
		this.size = size;
		this.defaultValue = defaultValue;
		int[] colIndex;
		int[] rowIndex;
		if (colStart == colEnd){
			colIndex = new int[1];
			colIndex[0] = colStart;
		} else {
			colIndex = new int[2];
			colIndex[0] = colStart;
			colIndex[1] = colEnd;
		}
		if (rowStart == rowEnd){
			rowIndex = new int[1];
			rowIndex[0] = rowStart;
		} else {
			rowIndex = new int[2];
			rowIndex[0] = rowStart;
			rowIndex[1] = rowEnd;
		}
		this.setColIndex(colIndex);
		this.setRowIndex(rowIndex);
		
	    tssFunction.setParamName(name);
	    tssFunction.setName(name);
	    tssFunction.setLabel(label);
		currentTSSiFunc = tssFunction;
	}

	
	public HTMLControl(
			int controlType,
			String name,
			String label,
			int colStart,
			int colEnd,
			int rowStart,
			int rowEnd,
			int size,
			Object defaultValue,String restriction,
			TSServerInfoFunction tssFunction, long searchId)
			throws FormatException
	{
		this.searchId = searchId;
		if(defaultValue!=null){
			tssFunction.setDefaultValue(defaultValue.toString());
		}
		
		this.controlType = controlType;
		this.name = name;
		this.label = label;
		this.size = size;
		this.defaultValue = defaultValue;
		super.restriction=restriction;
		int[] colIndex;
		int[] rowIndex;
		if (colStart == colEnd){
			colIndex = new int[1];
			colIndex[0] = colStart;
		} else {
			colIndex = new int[2];
			colIndex[0] = colStart;
			colIndex[1] = colEnd;
		}
		if (rowStart == rowEnd){
			rowIndex = new int[1];
			rowIndex[0] = rowStart;
		} else {
			rowIndex = new int[2];
			rowIndex[0] = rowStart;
			rowIndex[1] = rowEnd;
		}
		this.setColIndex(colIndex);
		this.setRowIndex(rowIndex);
		
	    tssFunction.setParamName(name);
	    tssFunction.setName(name);
	    tssFunction.setLabel(label);
		currentTSSiFunc = tssFunction;
	}

//	tremurici
		public HTMLControl(
				int controlType,LinkedList<ComboValue> list,
				String name,
				String label,
				int colStart,
				int colEnd,
				int rowStart,
				int rowEnd,
				int size,
				Object defaultValue,
				TSServerInfoFunction tssFunction, long searchId)
				throws FormatException{
			super();
				this.comboList=list;
				this.searchId = searchId;
				if(defaultValue!=null){
					tssFunction.setDefaultValue(defaultValue.toString());
				}
				
				this.controlType = controlType;
				this.name = name;
				this.label = label;
				this.size = size;
				this.defaultValue = defaultValue;
				int[] colIndex;
				int[] rowIndex;
				if (colStart == colEnd){
					colIndex = new int[1];
					colIndex[0] = colStart;
				} else {
					colIndex = new int[2];
					colIndex[0] = colStart;
					colIndex[1] = colEnd;
				}
				if (rowStart == rowEnd){
					rowIndex = new int[1];
					rowIndex[0] = rowStart;
				} else {
					rowIndex = new int[2];
					rowIndex[0] = rowStart;
					rowIndex[1] = rowEnd;
				}
				this.setColIndex(colIndex);
				this.setRowIndex(rowIndex);
				
			    tssFunction.setParamName(name);
			    tssFunction.setName(name);
			    tssFunction.setLabel(label);
				currentTSSiFunc = tssFunction;
		}
		
	public void setColAndRow(int colStart, int colEnd, int rowStart, int rowEnd) throws FormatException{
		int[] colIndex;
		int[] rowIndex;
		if (colStart == colEnd) {
			colIndex = new int[1];
			colIndex[0] = colStart;
		} else {
			colIndex = new int[2];
			colIndex[0] = colStart;
			colIndex[1] = colEnd;
		}
		if (rowStart == rowEnd) {
			rowIndex = new int[1];
			rowIndex[0] = rowStart;
		} else {
			rowIndex = new int[2];
			rowIndex[0] = rowStart;
			rowIndex[1] = rowEnd;
		}
		this.setColIndex(colIndex);
		this.setRowIndex(rowIndex);
	}
	
	public String render() {
		StringBuffer outData = new StringBuffer();
		String defaultValueString = "";
		
		String saKey = org.apache.commons.lang.StringUtils.defaultString(currentTSSiFunc.getSaKey());
		
		if(currentTSSiFunc.getSaKey() != null
				&& !"".equals(currentTSSiFunc.getSaKey())){
			SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
			defaultValueString = sa.getAtribute(currentTSSiFunc.getSaKey());
            
            defaultValueString = defaultValueString.replaceAll( "\'", "&#39;" );
		}
		String tableStr = "<table " + "width='" + getWidthFactorAsPercent() + "%' " +
				"height='" + getHeightFactorAsPercent() + "%' " +
				"cellpadding='0' cellspacing='0'>"+ "\n";
		
		// TDul se deschide si se inchide in renderul superior
		// fara colspan. Deci trebuie sa-l despartim in doua aici
		String labelClass = justifyField ? "indentLabelData":"labelData";
		
		if( label.equals( "" ) )
		{
		    labelClass = "noLabel";
		}
		
		if( controlType == HTML_LABEL )
		{
		    labelClass = "noFixedWidth" + " " + extraClass;
		}
		
		outData.append(tableStr);
	//	outData.append("<tr><td nowrap align='right' class='" + labelClass + "'><>");
		outData.append("<tr><td nowrap align='right' class='" + labelClass + "'>"+ "\n");
		outData.append(label.replaceAll("&apos;", "&#39;") + "&nbsp;");
		
		if( controlType != HTML_LABEL )
		{
			outData.append("</td><td class='bodyData_inner'>"+ "\n");
			outData.append("<table width='100%' cellpadding='0' cellspacing='0'>");
			if (currentTSSiFunc.getHtmlformat() != null
					&& !("").equals(currentTSSiFunc.getHtmlformat()))
			{
			    outData.append("<tr><td width='10'>"+ "\n");
				String htmlControlData = currentTSSiFunc.getHtmlformat();
				Matcher matcher = null;
				if(controlType == HTML_CHECK_BOX) {
					matcher = CHECKBOX_VALUE_PATTERN.matcher(htmlControlData);
					if(matcher.find()) {
						htmlControlData = htmlControlData.replace(matcher.group(), "cornele_m_ai_nenorocit");
					} else {
						matcher = null;
					}
				}
				
				htmlControlData = htmlControlData.replaceAll(currentTSSiFunc.getParamName(), currentTSSiFunc.getParamAlias());
				htmlControlData = htmlControlData.replaceAll("(?i)name=(\"|')(.*?)(\"|')", "NAME=\"$2\" ID=\"$2\"");
				
				if(matcher != null) {
					htmlControlData = htmlControlData.replace("cornele_m_ai_nenorocit", matcher.group());
				}
				
				outData.append(htmlControlData);
			}
			else
			{
				switch(this.controlType) 
				{
				case HTML_SELECT_BOX:
				{
				    outData.append("<tr><td width='10'>"+ "\n");
					if (defaultValue != null
							&& defaultValue instanceof String)
					{
						defaultValueString = (String) defaultValue;
					}
					String onBlur = "";
					if (fieldContentType != null)
					{
						onBlur = " onBlur='validate(this, " + 
						this.fieldContentType.jsRegex + ", " + 
						valueRequired + ", " +
						"\"" + label + "\", " +
						"\"" + this.fieldContentType.reqFormat + "\");" +
						" ' ";
					}
					
					String onChange = "";
					if(StringUtils.isNotEmpty(JSFunction)) {
						onChange = " onchange='"+JSFunction+"' ";
					}
					
					outData.append("<select  size='1'" +
							" value='" + defaultValueString + "'" +
							onBlur + " " + onChange +
							" id='" + currentTSSiFunc.getParamAlias() + "'" + 
							" name='" + currentTSSiFunc.getParamAlias() + "' sakey='"+saKey+"'>");
			
			
				//	outData.append("<select name='" +comboList.get(0).getName()+"'   size='"+comboList.get(0).getValue()+"' "+" id='" + currentTSSiFunc.getParamAlias() + "'"+ 
					//		" name='" + currentTSSiFunc.getParamAlias()+"'  >");
					for (int i=1;i<comboList.size();i++){
						if(defaultValueString.compareTo(comboList.get(i).getName())==0){
							outData.append("<option value=\"" +comboList.get(i).getName()+"\"  selected>"+comboList.get(i).getValue() + "</option>");
						}
						else{
							outData.append("<option value=\"" +comboList.get(i).getName()+"\" >"+comboList.get(i).getValue() + "</option>");
						}
					}
					outData.append("</select>" );
					if(eventHandlers != null){
						for(String handler: eventHandlers){
							outData.append(" " + handler.replaceAll("@id@",currentTSSiFunc.getParamAlias()) + " ");
						}
					}

				}
					break;

				case HTML_SELECT_LIST:
				    outData.append("<tr><td width='10'>"+ "\n");
					if (defaultValue != null
							&& defaultValue instanceof String)
					{
						defaultValueString = (String) defaultValue;
					}
					String onBlur1 = "";
					if (fieldContentType != null)
					{
						onBlur1 = " onBlur='validate(this, " + 
						this.fieldContentType.jsRegex + ", " + 
						valueRequired + ", " +
						"\"" + label + "\", " +
						"\"" + this.fieldContentType.reqFormat + "\");" +
						" ' ";
					}
					
					String onChange = "";
					if(StringUtils.isNotEmpty(JSFunction)) {
						if ("disabled".equals(JSFunction)){
							onChange = " disabled='"+JSFunction+"' ";
						} else {
							onChange = " onchange='"+JSFunction+"' ";
						}
					}
					outData.append("<select  size='"+comboList.get(0).getValue()+"'" +
							" value='" + defaultValueString + "'" +
							onBlur1 + " " + onChange +
							" id='" + currentTSSiFunc.getParamAlias() + "'" + 
							" name='" + currentTSSiFunc.getParamAlias() + "'MULTIPLE sakey='"+saKey+"'>");
			
			
				//	outData.append("<select name='" +comboList.get(0).getName()+"'   size='"+comboList.get(0).getValue()+"' "+" id='" + currentTSSiFunc.getParamAlias() + "'"+ 
					//		" name='" + currentTSSiFunc.getParamAlias()+"'  >");
					for (int i=1;i<comboList.size();i++){
						if(defaultValue.toString().compareTo(comboList.get(i).getName())==0){
							outData.append("<option value=\"" +comboList.get(i).getName()+"\"  selected>"+comboList.get(i).getValue() );
						}
						else{
							outData.append("<option value=\"" +comboList.get(i).getName()+"\" >"+comboList.get(i).getValue() );
						}
					}
					outData.append("</select>" );
					if(eventHandlers != null){
						for(String handler: eventHandlers){
							outData.append(" " + handler.replaceAll("@id@",currentTSSiFunc.getParamAlias()) + " ");
						}
					}

					
					break;

					
					
					case HTML_TEXT_FIELD:
								    outData.append("<tr><td width='10'>"+ "\n");
						if (defaultValue != null
								&& defaultValue instanceof String)
						{
							defaultValueString = (String) defaultValue;
						}
						String onBlur = " onBlur='";
						if (fieldContentType != null)
						{
							onBlur = "validate(this, " + 
							this.fieldContentType.jsRegex + ", " + 
							valueRequired + ", " +
							"\"" + label + "\", " +
							"\"" + this.fieldContentType.reqFormat + "\");";
							
						}
						
						String JSfunction = JSFunction.replaceAll("\\&apos;", "\""); // this is for IE 8
						
						String zoneNumber = currentTSSiFunc.getParamAlias().replace("param_", "").replaceAll("^(\\d+)_.*", "$1");

						if(JSfunction.contains("addAditionalRow")){
							if(zoneNumber.matches("\\d+")){
								JSfunction = JSfunction.replaceAll("addAditionalRow\\(([^)]*)\\)", "addAditionalRow($1,"+zoneNumber+")");
							}
						}
							
						if(JSfunction.contains("addAditionalFunction")){
							// we should have addAditionalFunction("PageZoneX",zoneNumber,event);
							
							if(JSfunction.contains("addAditionalFunction()")){
								//new empty function, let's add some parameters
								JSfunction = JSfunction.replaceAll("addAditionalFunction\\(([^)]*)\\)", "addAditionalFunction(\""+this.parent.name+"\")");
							}
								
							if(zoneNumber.matches("\\d+")){
								JSfunction = JSfunction.replaceAll("addAditionalFunction\\(([^)]*)\\)", "addAditionalFunction($1,"+zoneNumber+")");
							}
							JSfunction = JSfunction.replaceAll("addAditionalFunction\\(([^)]*)\\)", "addAditionalFunction($1,event)");
						}
						
						
						
						//boolean isAutoAdd = JSfunction.matches("addAditionalFunction\\(\"PageZone\\d+\",\"\\w+\",\"\\w+\"\\);"); //Auto Add bugs 7136 / 6987
						
						//onBlur += (isAutoAdd ? JSfunction: "") + " ' ";
						onBlur += " ' ";
						
						outData.append("<input class='edit "+extraClass+"' type='text'" +
								" size='" + size + "'" +
								" value='" + defaultValueString + "'" +
								onBlur +
								" id='" + currentTSSiFunc.getParamAlias() + "'" + 
								" name='" + currentTSSiFunc.getParamAlias() + "'" + 
								" onkeydown='" + JSfunction + "'" +
								" onkeyup='" + JSfunction + "'"); //bug 7185
						if(eventHandlers != null){
							for(String handler: eventHandlers){
								outData.append(" " + handler.replaceAll("@id@",currentTSSiFunc.getParamAlias()) + " ");
							}
						}
						outData.append(" sakey='"+saKey+"'"); 
						if(saKey.startsWith(SearchAttributes.FROMDATE) ||  saKey.startsWith(SearchAttributes.TODATE)) {
							outData.append(" defaultOnReplicate='true'/>");	
						} else {
							outData.append(" defaultOnReplicate='"+isDefaultOnReplicate()+"'/>");
						}
						break;
					case HTML_CHECK_BOX:
					    outData.append("<tr><td>");
					    if (defaultValue != null
								&& defaultValue instanceof String)
						{
							defaultValueString = (String) defaultValue;
						}
						
						outData.append("<input class='edit' type='checkbox'" +
								" size='" + size + "'" +
								" value='" + defaultValueString + "'" +	
								" id='" + currentTSSiFunc.getParamAlias() + "'" + 
								" name='" + currentTSSiFunc.getParamAlias() + "' sakey='"+saKey+"'>"+ "\n");
						break;
					case HTML_RADIO_BUTTON:
					    outData.append("<tr><td>"+ "\n");
						if (defaultValue != null
								&& defaultValue instanceof HashMap)
						{
							Iterator i = ((HashMap) defaultValue).entrySet().iterator();
							String radioLabel = "", radioValue = "";
							outData.append("<p>" + "\n");
							boolean hasDef = false;
							while(i.hasNext())
							{
								Entry element = (Entry) i.next();
								radioLabel = (String) element.getKey();
								radioValue = (String) element.getValue();
                                
                                if( radioLabel.equals( radioDefaultChecked ) )
                                {
                                    hasDef = true;
                                }
                                
								outData.append("<label>" + "\n");
								outData.append("<input type='radio'" +
										" name='" + currentTSSiFunc.getParamAlias() + "'" +
										" id='" + currentTSSiFunc.getParamAlias() + "'" +
										(hasDef ? "checked" : "") + 
										" value='" + radioValue + "'>" +
										radioLabel + "</label>\n");
                                if( !horizontalRadioButton )
                                {
                                    outData.append( "<br>" );
                                }
								hasDef = false;
							}
							outData.append("</p>" + "\n");
						}
						break;
					case HTML_LABEL:
					    outData.append("<tr><td> &nbsp;\n");
					    break;
					
					case HTML_ACTION_TEXT:
						outData.append("<tr><td width='10'>");
						if (defaultValue != null
								&& defaultValue instanceof String)
						{
							defaultValueString = (String) defaultValue;
						}
						
						outData.append("<input class='edit' type='text'" +
								" size='" + size + "'" +
								" value='" + defaultValueString + "'" +	
								" id='" + currentTSSiFunc.getParamAlias() + "'" + 
								" name='" + currentTSSiFunc.getParamAlias() + "' sakey='"+saKey+"'/>"+ "\n");
						outData.append("<input class='button' type='button'" +
								" value='Go'" +" onclick='"+JSFunction+"'" +
								"/>"+ "\n");
						break;
					
					case HTML_BUTTON:
						outData.append("<tr><td width='10'>");
						if (defaultValue != null
								&& defaultValue instanceof String)
						{
							defaultValueString = (String) defaultValue;
						}
						
						outData.append("<input class='button' type='button'" +
								" value='"+defaultValueString+"'" +" onclick='"+JSFunction+"'" +
								" sakey='"+saKey+"'/>"+ "\n");
						
						break;
				}
			}
		}
		outData.append("</td>"+ "\n");

		if (requiredCritical || StringUtils.isNotEmpty(fieldNote))
		{
		    outData.append("<td class='noteData'>"+ "\n");
		
			if (requiredCritical)
			{
				outData.append("<font color='red'><b>*</b></font>&nbsp;&nbsp;"+ "\n");
			}
			
			if( !fieldNote.equals( "" ) )
			{
			    outData.append(fieldNote);
			}
			
			outData.append("</td>"+ "\n");
		}else {
			outData.append("<td class='noteData'>"+ "\n");
			outData.append(" "+ "\n");
			outData.append("</td>"+ "\n");
		}
		
		outData.append("</tr></table>");
		
		if( controlType != HTML_LABEL )
		{
		    outData.append("</td></tr></table>"+ "\n");
		}
//		outData.append("</tr>");
		//		randam si fieldurile hidden
		Iterator i = getAttachedHiddenFields().keySet().iterator();
		while (i.hasNext())
		{
			try {
				HTMLObject ho = (HTMLObject) getAttachedHiddenFields().get(i.next());
				outData.append(ho.render());
			} catch (FormatException e) {
				e.printStackTrace();
			}
		}
		String retData = outData.toString();
		if (this.isHiddenParam())
		{
			retData = "<input type=\"hidden\" name=\"" + currentTSSiFunc.getParamAlias() + "\"" +
					" id=\"" + currentTSSiFunc.getParamAlias() + "\"" + 
					" value='" + currentTSSiFunc.getDefaultValue() + "'/>" + "\n";
		}
		
		return retData;
	}
	
	
	public String getAliasName() {
		return currentTSSiFunc.getParamAlias();
	}

	/**
	 * @return Returns the defaultValue.
	 */
	public Object getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @param defaultValue The defaultValue to set.
	 */
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	
	/**
	 * @return Returns the fieldNote.
	 */
	public String getFieldNote() {
		return fieldNote;
	}

	/**
	 * @param fieldNote The fieldNote to set.
	 */
	public void setFieldNote(String fieldNote) {
		this.fieldNote = fieldNote;
	}

	/**
	 * @return Returns the valueRequired.
	 */
	public boolean isValueRequired() {
		return valueRequired;
	}

	/**
	 * @param valueRequired The valueRequired to set.
	 */
	public void setValueRequired(boolean valueRequired) {
		if (valueRequired)
		{
			this.fieldContentType = FieldType.NOT_EMPTY;
		}
		this.valueRequired = valueRequired;
	}
	/**
	 * Retrieve a set JSFunction
	 * @return String JSFunction
	 */
	
	public String getJSFunction(){
	  return JSFunction; 
	}
	
	/**
	 * set a javascript string function
	 * @param String JS
	 */
	public void setJSFunction (String JS){
		this.JSFunction = JS;
	}

	/**
	 * @return Returns the fieldContentType.
	 */
	public FieldType getFieldContentType() {
		return fieldContentType;
	}

	/**
	 * @param fieldContentType The fieldContentType to set.
	 */
	public void setFieldContentType(FieldType fieldContentType) {
		this.fieldContentType = fieldContentType;
	}

	/**
	 * @return Returns the justifyField.
	 */
	public boolean isJustifyField() {
		return justifyField;
	}

	/**
	 * @param justifyField The justifyField to set.
	 */
	public void setJustifyField(boolean justifyField) {
		this.justifyField = justifyField;
	}

	public static void setJustifyFieldMulti(boolean justifyField, HTMLControl... controls){
		for(HTMLControl control: controls){
			control.justifyField = justifyField;
		}
	}
	/**
	 * @return Returns the requiredExcl.
	 */
	public boolean isRequiredExcl() {
		return requiredExcl;
	}

	/**
	 * @param requiredExcl The requiredExcl to set.
	 */
	public void setRequiredExcl(boolean requiredExcl) {
		this.requiredExcl = requiredExcl;
	}
	
	/**
	 * @param requiredExcl The requiredExcl to set.
	 */
	public static void setRequiredExclMulti(boolean requiredExcl, HTMLControl... controls){
		for(HTMLControl control: controls){
			control.setRequiredExcl(requiredExcl);
		}
	}

	/**
	 * @return Returns the requiredCritical.
	 */
	public boolean isRequiredCritical() {
		return requiredCritical;
	}
    
    public void setHorizontalRadioButton( boolean value )
    {
        horizontalRadioButton = value;
    }
    
    public boolean isHorizontalRadioButton()
    {
        return horizontalRadioButton;
    }
    
    public void setDefaultRadio( String value )
    {
        radioDefaultChecked = value;
    }

	/**
	 * @param requiredCritical The requiredCritical to set.
	 */
	public void setRequiredCritical(boolean requiredCritical) {
		this.requiredCritical = requiredCritical;
	}
	
	public static void setRequiredCriticalMulti(boolean requiredCritical, HTMLControl... controls){
		for(HTMLControl control: controls){
			control.requiredCritical = requiredCritical;
		}
	}
	
	/**
	 * if we want this HTMLControl hidden, then mark the corresponding function as not loggable
	 *  this is used by the logging mechanism in order to ignore this function when logging
	 */
	@Override
	public void setHiddenParam(boolean hiddenParam) {
		super.setHiddenParam(hiddenParam);
		if(currentTSSiFunc != null){
			currentTSSiFunc.setLoggable(!hiddenParam); // do not log hidden parameters
		}
	}
	
	public static void setHiddenParamMulti(boolean hiddenParam, HTMLControl... controls){
		for(HTMLControl control: controls){
			control.setHiddenParam(hiddenParam);
		}
	}
	
	public void setHiddenParamLoggable(boolean hiddenParam) {
		super.setHiddenParam(hiddenParam);
	}
	
	public static void setHiddenParamLoggableMulti(boolean hiddenParam, HTMLControl... controls){
		for(HTMLControl control: controls){
			control.setHiddenParamLoggable(hiddenParam);
		}
	}


	public String getRestriction() {
		return super.restriction;
	}


	public void setRestriction(String restriction) {
		super.restriction = restriction;
	}


	public TSServerInfoFunction getCurrentTSSiFunc() {
		return currentTSSiFunc;
	}


	public void setCurrentTSSiFunc(TSServerInfoFunction currentTSSiFunc) {
		this.currentTSSiFunc = currentTSSiFunc;
	}

	public String getExtraClass() {
		return extraClass;
	}

	public void setExtraClass(String extraClass) {
		this.extraClass = extraClass;
	}

	public LinkedList<ComboValue> getComboList() {
		return comboList;
	}
	
	public LinkedList<ComboValue> setComboList(LinkedList<ComboValue> comboList) {
		return this.comboList = comboList;
	}
	
	public int getControlType() {
		return controlType;
	}

	public boolean isDefaultOnReplicate() {
		return defaultOnReplicate;
	}

	public void setDefaultOnReplicate(boolean defaultOnReplicate) {
		this.defaultOnReplicate = defaultOnReplicate;
	}

}
