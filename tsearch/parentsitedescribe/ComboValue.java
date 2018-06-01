package ro.cst.tsearch.parentsitedescribe;

public class ComboValue {
	private String name="";
	private String value="";
	public ComboValue(String name, String value) {
		super();
		this.name = name;
		this.value = value;
	}
	public ComboValue() {
		super();
		// TODO Auto-generated constructor stub
	}
	public ComboValue(String name) {
		super();
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

}
