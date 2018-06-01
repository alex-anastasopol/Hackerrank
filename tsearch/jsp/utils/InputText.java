package ro.cst.tsearch.jsp.utils;


/**
 * 
 * @author Florin Cazacu
 * Base for generators of input type text html code 
 * negative int values, empty strings, false values will not print the attribute
 *
 */
public class InputText extends Input {
	private int size = -1;
	private int maxLength = -1;
	
	public static InputText getInstance(int maxLength, int size){
		InputText tmp = new InputText();
		tmp.setMaxLength(maxLength);
		tmp.setSize(size);
		return tmp;
	}
	
	public InputText(){
		super(TYPE_TEXT);
	}
	/**
	 * Constructs an input element with the default values, and the provided name, type and value
	*/
	public InputText(String name, String value) {
		super(TYPE_TEXT, name, value);
	}

	/**
	 * Constructs an input element with the default values, and the provided name, type, value, id and styleClass
	 * empty strings for id and styleClass with skip this attributes from input
	*/
	public InputText(String name, String value, String id,
			String styleClass) {
		super(TYPE_TEXT, name, value, id, styleClass);
	}

	/**
	 * Constructs an input element with the default values, and the provided name, type, value, id and styleClass
	 * and the events onFocus, onchange, onblur, onselect
	 * empty strings for id and styleClass and events with skip this attributes from input
	*/
	public InputText(String name, String value, String id,
			String styleClass, String onFocus, String onChange, String onBlur,
			String onSelect) {
		super(TYPE_TEXT, name, value, id, styleClass, onFocus, onChange, onBlur,
				onSelect);
	}

	/**
	 * returns the html for the defined input
	 */
	public String display(){
		return generateHml();
	}
	
	/**
	 * sets the name and value of the input and 
	 * returns the html for the defined attributes
	 */
	public String display(String name, String value){
		setName(name);
		setValue(value);
		return display();
	}
	
	public void setSize(int size){
		this.size = size;
	}
	
	public String generateHml(){
		String htmlInput = "";
		if (maxLength >= 0){
			htmlInput += " maxLength=\"" + maxLength + "\" ";
		}
		if (size >= 0){
			htmlInput += " size=\"" + size + "\" ";
		}
		return super.generateHtml(htmlInput);
	}
	public void setMaxLength(int maxLength){
		this.maxLength = maxLength;
	}
	
	public String toString(){
		return display();
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		InputText t = new InputText();
		t.setMaxLength(2);
		t.setSize(2);
		t.setStyleClass("textClass");
		t.setOnChange("javascript: onChange();");
		System.out.println(t.display("name", "value"));
		
	}

}
