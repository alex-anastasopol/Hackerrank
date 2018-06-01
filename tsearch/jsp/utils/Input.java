package ro.cst.tsearch.jsp.utils;


/**
 * 
 * @author Florin Cazacu
 * Base for generators of input html code 
 * negative int values, empty strings, false values will not print the attribute
 *
 */
public abstract class Input {
	protected static String TYPE_TEXT = "text";
	protected static String TYPE_PASSWORD = "password";
	protected static String TYPE_CHECKBOX = "checkbox";
	protected static String TYPE_RADIO = "radio";
	protected static String TYPE_SUBMIT = "submit";
	protected static String TYPE_RESET = "reset";
	protected static String TYPE_FILE = "file";
	protected static String TYPE_HIDDEN = "hidden";
	protected static String TYPE_IMAGE = "image";
	protected static String TYPE_BUTTON = "button";
	
	private String type = TYPE_TEXT;
	private String name = "";
	private String value = "";
	private boolean disabled = false;
	private boolean readonly = false;
	private String accessKey = "";
	private int tabIndex = -1;
	private String onFocus = "";
	private String onChange = "";
	private String onBlur = "";
	private String onSelect = "";
	private String id = "";
	private String styleClass = "";
	private String style = "";
	private String title = "";
	private String lang = "";
	private String dir = "ltr";
	/**
	 * This string will be pasted as it is to the input
	*/
	private String extra = "";
	
	/**
	 * Constructs an input element with the default values, and type type
	*/
	public Input(String type){
		this.type = type;
	}
	
	/**
	 * Constructs an input element with the default values, and the provided name, type and value
	*/
	public Input(String type, String name, String value){
		this(type);
		this.name = name;
		this.value = value;
	}
	
	/**
	 * Constructs an input element with the default values, and the provided name, type, value, id and styleClass
	 * empty strings for id and styleClass with skip this attributes from input
	*/
	public Input(String type, String name, String value, String id, String styleClass){
		this(type, name, value);
		this.id = id;
		this.styleClass = styleClass;
	}
	
	/**
	 * Constructs an input element with the default values, and the provided name, type, value, id and styleClass
	 * and the events onFocus, onchange, onblur, onselect
	 * empty strings for id and styleClass and events with skip this attributes from input
	*/
	public Input(String type, String name, String value, String id, String styleClass, String onFocus, String onChange, String onBlur, String onSelect){
		this(type, name, value, id, styleClass);
		this.onFocus = onFocus;
		this.onBlur = onBlur;
		this.onChange = onChange;
		this.onSelect = onSelect;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public void setValue(String value){
		this.value = value;
	}
	
	public void setDisabled(boolean disabled){
		this.disabled = disabled;
	}
	
	public void setReadonly(boolean readonly){
		this.readonly = readonly;
	}
	
	public void setAccessKey(String accessKey){
		this.accessKey = accessKey;
	}
	
	public void setTabIndex(int tabIndex){
		this.tabIndex = tabIndex;
	}
	
	public void setOnFocus(String onFocus){
		this.onFocus = onFocus;
	}
	
	public void setOnChange(String onChange){
		this.onChange = onChange;
	}
	
	public void setOnBlur(String onBlur){
		this.onBlur = onBlur;
	}
	
	public void setOnSelect(String onSelect){
		this.onSelect = onSelect;
	}
	
	public void setId(String id){
		this.id = id;
	}
	
	public void setStyleClass(String styleClass){
		this.styleClass = styleClass;
	}
	
	public void setStyle(String style){
		this.style = style;
	}
	
	public void setTitle(String title){
		this.title = title;
	}
	
	public void setLang(String lang){
		this.lang = lang;
	}
	
	public void setDir(String dir){
		this.dir = dir;
	}
	
	public void setExtra(String extra){
		this.extra = extra;
	}
	
	protected String generateHtml(String extra){
		String htmlInput = "";
		htmlInput += "<input type=\"" + type + "\" ";
		if (!"".equals(name)){
			htmlInput += "name=\"" + name + "\" ";
		}
		if (!"".equals(value)){
			htmlInput += "value=\"" + value + "\" ";
		}
		if (disabled){
			htmlInput += "disabled ";
		}
		if (readonly){
			htmlInput += "readonly ";
		}
		if (!"".equals(accessKey)){
			htmlInput += "accesskey=\"" + accessKey + "\" ";
		}
		if (tabIndex >= 0){
			htmlInput += "tabindex=\"" + tabIndex + "\" ";
		}
		if (!"".equals(onFocus)){
			htmlInput += "onfocus=\"" + onFocus + "\" ";
		}
		if (!"".equals(onChange)){
			htmlInput += "onchange=\"" + onChange + "\" ";
		}
		if (!"".equals(onBlur)){
			htmlInput += "onblur=\"" + onBlur + "\" ";
		}
		if (!"".equals(onSelect)){
			htmlInput += "onselect=\"" + onSelect+ "\" ";
		}
		if (!"".equals(id)){
			htmlInput += "id=\"" + id + "\" ";
		}
		if (!"".equals(styleClass)){
			htmlInput += "class=\"" + styleClass + "\" ";
		}
		if (!"".equals(style)){
			htmlInput += "style=\"" + style + "\" ";
		}
		if (!"".equals(title)){
			htmlInput += "title=\"" + title + "\" ";
		}
		if (!"".equals(lang)){
			htmlInput += "lang=\"" + lang + "\" ";
		}
		if (!"".equals(dir)){
			htmlInput += "dir=\"" + dir + "\" ";
		}
		if (!"".equals(this.extra)){
			htmlInput += this.extra + " ";
		}
		if (!"".equals(extra)){
			htmlInput += " " + extra + " ";
		}
		htmlInput += ">";
		return htmlInput;
	}
	
	abstract public String display();
	abstract public String display(String name, String value);
}
