package ro.cst.tsearch.parentsitedescribe;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class HtmlControlMap {
	
	private static final Pattern COMBO_VALUE_PATTERN = Pattern.compile("name=(.*?)(?=_value)_value=([^,]*)");

	// LinkedList <Param> area;
	private int controlType = 0;
	private String name = "";
	private String label = "";
	private int colStart = 0;
	private int colEnd = 0;
	private int rowStart = 0;
	private int rowEnd = 0;
	private int size = 0;
	private int tssFunction = 0;
	private Object defaultValue = null;
	private String fieldNote = "";
	private boolean valueRequired = false;
	private boolean requiredExcl = false;
	private boolean requiredCritical = false;
	private boolean horizontalRadioButton = false;
	private boolean justifyField = false;
	private String radioDefaultChecked = "";
	private String JSFunction = "";
	private boolean hiddenparam = false;
	private LinkedList<ComboValue> comboValue = null;
	private String defaultSelect = "";
	private String comboValueString = "";
	private String htmlString = "";
	private String extraClass = "";
	private boolean defaultOnReplicate = false; //set the default value of this field when replicating the module

	public HtmlControlMap toEscape() {

		HtmlControlMap tmp = this;
		tmp.name = escapeString(tmp.name);
		tmp.label = escapeString(tmp.label);
		tmp.radioDefaultChecked = escapeString(tmp.radioDefaultChecked);
		tmp.JSFunction = escapeString(tmp.JSFunction);
		tmp.defaultSelect = escapeString(tmp.defaultSelect);
		tmp.comboValueString = escapeString(tmp.comboValueString);
		tmp.htmlString = escapeString(tmp.htmlString);
		tmp.fieldNote = escapeString(tmp.fieldNote);
		if (tmp.defaultValue != null) {
			tmp.defaultValue = escapeString(tmp.defaultValue.toString());
		}
		tmp.extraClass = escapeString(tmp.extraClass);
		return tmp;
	}

	public void replace(HtmlControlMap h) {
		if (h != null) {
			this.setControlType(h.getControlType());
			this.setName(h.getName());
			this.setLabel(h.getLabel());
			this.setColStart(h.getColStart());
			this.setColEnd(h.getColEnd());
			this.setRowStart(h.getRowStart());
			this.setRowEnd(h.getRowEnd());
			this.setSize(h.getSize());
			this.setTssFunction(h.getTssFunction());
			this.setDefaultValue(h.getDefaultValue());
			this.setFieldNote(h.getFieldNote());
			this.setValueRequired(h.getValueRequired());
			this.setRequiredCritical(h.getRequiredCritical());
			this.setRequiredExcl(h.getRequiredExcl());
			this.setHorizontalRadioButton(h.getHorizontalRadioButton());
			this.setJustifyField(h.getJustifyField());
			this.setRadioDefaultChecked(h.getRadioDefaultChecked());
			this.setJSFunction(h.getJSFunction());
			this.setHiddenparam(h.getHiddenparam());
			this.setDefaultSelect(h.getDefaultSelect());
			this.setComboValueString(h.getComboValueString());
			this.setHtmlString(h.getHtmlString());
			this.setComboValue(this.getComboValueString());
			this.setExtraClass(h.getExtraClass());
			this.setDefaultOnReplicate(h.isDefaultOnReplicate());
		}

	}

	public String escapeString(String escape) {

		escape = escape.replaceAll("&amp;", "&");
		escape = escape.replaceAll("&quot;", "\"");
		escape = escape.replaceAll("&apos;", "'");
		escape = escape.replaceAll("&lt;", "<");
		escape = escape.replaceAll("&gt;", ">");

		escape = escape.replaceAll("&", "&amp;");
		escape = escape.replaceAll("\"", "&quot;");
		escape = escape.replaceAll("'", "&apos;");
		escape = escape.replaceAll("<", "&lt;");
		escape = escape.replaceAll(">", "&gt;");

		return escape;
	}

	public void setHtml(HtmlControlMap h) {
		if (h != null) {
			this.setControlType(h.getControlType());
			this.setName(h.getName());
			this.setLabel(h.getLabel());
			this.setColStart(h.getColStart());
			this.setColEnd(h.getColEnd());
			this.setRowStart(h.getRowStart());
			this.setRowEnd(h.getRowEnd());
			this.setSize(h.getSize());
			// this.setTssFunction(h.getTssFunction());
			this.setDefaultValue(h.getDefaultValue());
			this.setFieldNote(h.getFieldNote());
			this.setValueRequired(h.getValueRequired());
			this.setRequiredCritical(h.getRequiredCritical());
			this.setRequiredExcl(h.getRequiredExcl());
			this.setHorizontalRadioButton(h.getHorizontalRadioButton());
			this.setJustifyField(h.getJustifyField());
			this.setRadioDefaultChecked(h.getRadioDefaultChecked());
			this.setJSFunction(h.getJSFunction());
			this.setHiddenparam(h.getHiddenparam());
			this.setDefaultSelect(h.getDefaultSelect());
			this.setComboValueString(h.getComboValueString());
			this.setHtmlString(h.getHtmlString());
			this.setExtraClass(h.getExtraClass());
			this.setDefaultOnReplicate(h.isDefaultOnReplicate());
			
		}

	}

	private Object DefValue = "";

	public String getHtmlString() {
		return this.htmlString;
	}

	public void setHtmlString(String htmlString) {
		this.htmlString = htmlString;
	}

	public String getComboValueString() {
		return comboValueString;
	}

	public void setComboValueString(String comboValueString) {
		this.comboValueString = comboValueString;
	}

	public boolean getHiddenparam() {
		return hiddenparam;
	}

	public void setHiddenparam(boolean hiddenparam) {
		this.hiddenparam = hiddenparam;
	}

	public HtmlControlMap(int controlType, String name, String label,
			int colStart, int colEnd, int rowStart, int rowEnd, int size,
			Object defaultValue, int tssFunction, String fieldNote) {
		super();
		this.controlType = controlType;
		this.name = name;
		this.label = label;
		this.colStart = colStart;
		this.colEnd = colEnd;
		this.rowStart = rowStart;
		this.rowEnd = rowEnd;
		this.size = size;
		this.defaultValue = defaultValue;
		this.tssFunction = tssFunction;

	}

	public HtmlControlMap() {
		super();
		controlType = 0;
		name = "";
		label = "";
		colStart = 1;
		colEnd = 1;
		rowStart = 1;
		rowEnd = 1;
		size = 0;
		// private String defaultValue=null;
		tssFunction = 0;
		// private int controlTypeLast = -1;
		defaultValue = "";
		fieldNote = "";
		valueRequired = false;
		requiredExcl = false;
		requiredCritical = false;
		horizontalRadioButton = false;
		justifyField = false;
		radioDefaultChecked = "";
		JSFunction = "";
		hiddenparam = false;
		extraClass = "";

		defaultSelect = "";
		comboValueString = "";
		htmlString = "";
		comboValue = new LinkedList<ComboValue>();
		// TODO Auto-generated constructor stub
	}

	public int getColEnd() {
		return colEnd;
	}

	public void setColEnd(int colEnd) {
		this.colEnd = colEnd;
	}

	public int getColStart() {
		return colStart;
	}

	public void setColStart(int colStart) {
		this.colStart = colStart;
	}

	public int getControlType() {
		return controlType;
	}

	public void setControlType(int controlType) {
		this.controlType = controlType;
	}

	public String getFieldNote() {
		return fieldNote;
	}

	public void setFieldNote(String fieldNote) {
		this.fieldNote = fieldNote;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getRowEnd() {
		return rowEnd;
	}

	public void setRowEnd(int rowEnd) {
		this.rowEnd = rowEnd;
	}

	public int getRowStart() {
		return rowStart;
	}

	public void setRowStart(int rowStart) {
		this.rowStart = rowStart;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getTssFunction() {
		return tssFunction;
	}

	public void setTssFunction(int tssFunction) {
		this.tssFunction = tssFunction;
	}

	public boolean getHorizontalRadioButton() {
		return horizontalRadioButton;
	}

	public void setHorizontalRadioButton(boolean horizontalRadioButton) {
		this.horizontalRadioButton = horizontalRadioButton;
	}

	public String getJSFunction() {
		return JSFunction;
	}

	public void setJSFunction(String function) {
		JSFunction = function;
	}

	public boolean getJustifyField() {
		return justifyField;
	}

	public void setJustifyField(boolean justifyField) {
		this.justifyField = justifyField;
	}

	public String getRadioDefaultChecked() {
		return radioDefaultChecked;
	}

	public void setRadioDefaultChecked(String radioDefaultChecked) {
		this.radioDefaultChecked = radioDefaultChecked;
	}

	public boolean getRequiredCritical() {
		return requiredCritical;
	}

	public void setRequiredCritical(boolean requiredCritical) {
		this.requiredCritical = requiredCritical;
	}

	public boolean getRequiredExcl() {
		return requiredExcl;
	}

	public void setRequiredExcl(boolean requiredExcl) {
		this.requiredExcl = requiredExcl;
	}

	public boolean getValueRequired() {
		return valueRequired;
	}

	public void setValueRequired(boolean valueRequired) {
		this.valueRequired = valueRequired;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Object getRadio() {
		HashMap<String, String> radioValues = new HashMap<String, String>();
		for (int i = 0; i < comboValue.size(); i++) {
			radioValues.put(comboValue.get(i).getName(), comboValue.get(i)
					.getValue());
		}
		return radioValues;
	}

	public Object getDefaultValue() {
		if (this.controlType == 1) {
			HashMap<String, String> radioValues = new HashMap<String, String>();
			for (int i = 0; i < comboValue.size(); i++) {
				radioValues.put(comboValue.get(i).getName(), comboValue.get(i)
						.getValue());
			}

			this.defaultValue = radioValues;
		}

		return defaultValue;
	}

	public String getDefaultSelect() {
		return defaultSelect;
	}

	public void setDefaultSelect(String defaultSelect) {
		this.defaultSelect = defaultSelect;
	}

	public LinkedList<ComboValue> getComboValue() {
		return this.comboValue;
	}

	public void setComboValue(String selectValue) {
		if ((selectValue != null) && (!"".equals(selectValue))) {
			comboValueString = selectValue;
		} else {
			comboValueString = getHtmlString();

		}
		
		
		Matcher matcher = COMBO_VALUE_PATTERN.matcher(selectValue);
		
		if( matcher.find() ) {
		
			LinkedList<ComboValue> combo = new LinkedList<ComboValue>();
			
			ComboValue unitar = new ComboValue();
			unitar.setName(matcher.group(1));
			unitar.setValue(matcher.group(2));
			combo.add(unitar);
			
			while(matcher.find()) {
				unitar = new ComboValue();
				unitar.setName(matcher.group(1));
				unitar.setValue(matcher.group(2));
				combo.add(unitar);
			}
			this.comboValue = combo;
		}
	}

	public Object getDefValue() {
		return DefValue;
	}

	public void setDefValue(Object defValue) {
		DefValue = defValue;
	}

	public String getExtraClass() {
		return extraClass;
	}

	public void setExtraClass(String extraClass) {
		this.extraClass = extraClass;
	}
	
	public boolean isDefaultOnReplicate(){
		return this.defaultOnReplicate;
	}
	
	public void setDefaultOnReplicate(boolean defaultOnReplicate){
		this.defaultOnReplicate = defaultOnReplicate;
	}
	
	public static LinkedList<ComboValue> createComboValueSet(String selectValue) {
		
		LinkedList<ComboValue> combo = new LinkedList<ComboValue>();
		
		if (StringUtils.isEmpty(selectValue)){
			return combo;
		}
		
		Matcher matcher = COMBO_VALUE_PATTERN.matcher(selectValue);
		
		if (matcher.find()) {
			
			ComboValue unitar = new ComboValue();
			unitar.setName(matcher.group(1));
			unitar.setValue(matcher.group(2));
			combo.add(unitar);
			
			while (matcher.find()) {
				unitar = new ComboValue();
				unitar.setName(matcher.group(1));
				unitar.setValue(matcher.group(2));
				combo.add(unitar);
			}
		}
		return combo;
	}
}
