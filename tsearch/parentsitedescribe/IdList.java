package ro.cst.tsearch.parentsitedescribe;

import java.util.LinkedList;

public class IdList {
private static LinkedList <IdReplace> id=null;
private static LinkedList <IdValue> idValue=null;

public IdList() {
	super();
	if(id==null){
		id= new LinkedList<IdReplace>();
	}
	if(idValue==null){
		idValue= new LinkedList<IdValue>();
	}
	
}
public static void  addObject(String name,HtmlControlMap html){
	if(id==null){
		id= new LinkedList<IdReplace>();
	}
	if(idValue==null){
		idValue= new LinkedList<IdValue>();
	}

	
	boolean isList=false;
		IdReplace idTemp=new IdReplace();
		for(int i=0;i<id.size();i++){
			if(id.get(i).getHtml()==html){
				isList=true;
			}
			
		}
		if(!isList){
			idTemp.setHtml(html);
			idTemp.setName(name);
			id.add(idTemp);
		}
	}
public static  void setId(String name, String value){
if(value!=null){
	if(!"".equals(value)){
		if(id!=null){
		for(int i=0;i<id.size();i++){
			if(id.get(i).getName().compareToIgnoreCase(name)==0){
					id.get(i).getHtml().setDefaultValue(value);
				}
			}
		}
	}
	}
	if((name!=null)&&(value!=null)){
			addValue(name,value);
		}
}


public static void addValue(String name,String value){
	boolean isInList=false;
	if((name!=null)&&(value!=null)){
		if((!"".equals(name))&&(!"".equals(value))){
			for(int i=0;i<idValue.size();i++){
				if(idValue.get(i).getName().compareToIgnoreCase(name)==0){
					idValue.get(i).setValue(value);
					isInList=true;
				}
			}
		}
		
	}
	if(!isInList){
		if((name!=null)&&(value!=null)){
			IdValue idTempValue=new IdValue();
			idTempValue.setName(name);
			idTempValue.setValue(value);
			idValue.add(idTempValue);
		}
	}
}



}
