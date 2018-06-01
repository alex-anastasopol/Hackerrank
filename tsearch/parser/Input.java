package ro.cst.tsearch.parser;

public class Input {
	
	public String name = "";
	public String id = "";
	public String value = "";
	public String type = "";
	public String tagName = "";
	private String action = "";

	public Input() {
		this.name = "";
		this.id = "";
		this.value = "";
		this.type = "";
		this.tagName = "";
	}

	public Input(String name, String id, String value) {
		super();
		this.name = name;
		this.id = id;
		this.value = value;
	}
	public String toStringNameValue(){
		return this.name+"="+this.value;
	}
	public Input(String name, String id, String value, String type,String tagName, String action) {
		super();
		this.name = name;
		this.id = id;
		this.value = value;
		this.type=type;
		this.tagName=tagName;
		this.action=action;
	}
	public boolean compareTo(Input in){
		if(this.name!=null&&in.name!=null){
			if(!this.name.equalsIgnoreCase(in.name)){
				return false;
			}
		}
		if(this.id!=null&&in.id!=null){
			if(!this.id.equalsIgnoreCase(in.id)){
				return false;
			}
		}
		if(this.value!=null&&in.value!=null){
			if(!this.value.equalsIgnoreCase(in.value)){
				return false;
			}
		}
		if(this.type!=null&&in.type!=null){
			if(!this.type.equalsIgnoreCase(in.type)){
				return false;
			}
		}
		if(this.tagName!=null&&in.tagName!=null){
			if(!this.tagName.equalsIgnoreCase(in.tagName)){
				return false;
			}
		}
		if(this.action!=null&&in.action!=null){
			if(!this.action.equalsIgnoreCase(in.action)){
				return false;
			}
						
		}
		return true;
	}
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTagName() {
		return this.tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}
	
}
