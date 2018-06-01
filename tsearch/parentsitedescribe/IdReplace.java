package ro.cst.tsearch.parentsitedescribe;

public class IdReplace {
	private String name;
	private HtmlControlMap html;
	
	
	public HtmlControlMap getHtml() {
		return html;
	}
	public void setHtml(HtmlControlMap html) {
		this.html = html;
	}
	public String getName(){
		return this.name;
	}
	public void setName(String name){
		this.name=name;
	}
	public IdReplace() {
		super();
		name=new String();
		html=new HtmlControlMap();
		// TODO Auto-generated constructor stub
	}
	public IdReplace(String name, Object o) {
		super();
		this.name = name;
		
	}
}
