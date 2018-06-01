package ro.cst.tsearch.parentsitedescribe;

import java.util.LinkedList;

public class ListParam {
	public  LinkedList<ComboValue> list=null;
	boolean isElement;

	public ListParam() {
		super();
		this.isElement = false;
		list=new LinkedList<ComboValue>();
	}
public void add(String element,String value){
	ComboValue comb=new ComboValue();
	comb.setName(element);

	comb.setValue(value);
	this.list.add(comb);
}
public String getParameter(String name){
	for(int i=0;i<this.list.size();i++){
		if(name.compareToIgnoreCase(list.get(i).getName() )==0){
			return list.get(i).getValue();
		}
	}
	return "";
	
}
public boolean isInList(String element){
	this.isElement=false;
	for(int i=0;i<this.list.size();i++){
		if((element.compareToIgnoreCase(list.get(i).getName() )==0)&&(!"".equals(list.get(i).getValue()))){
			return true;
		}
	}
	return false;
}
}
