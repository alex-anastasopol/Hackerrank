//Soft realizat de Tremurici Cornel Elvis 


package ro.cst.tsearch.templates.edit.client;


//clasa structura e o clasa pentru lucru cu structura

//setPatfile seteaza calea fisierului  
//extractType  face operatiile pe sir;
import java.io.*;

import java.util.Vector;

import com.google.gwt.user.client.Window;

public class EditorElement {
	

	private static int lstart = "<!--".length();
	private static int lstop = "-->".length();
	
	
	private String content ="";
	
	
	private boolean editTable = false;

	public EditorElement( String content, boolean editTable){
		this.content = content;
		this.editTable = editTable;
	}
	
	public EditorElement(){
		
	}
	
	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public boolean isEditTable() {
		return editTable;
	}

	public void setEditTable(boolean editTable) {
		this.editTable = editTable;
	}


	public String toString(){
		return " content = " +this.content + " editTable ="+this.editTable;
	}
	
	private  static Vector splitElements(String str,boolean removeEmptyLine){
		Vector  ret = new Vector();
		ret.add(new EditorElement(str,false));
		/*
		String  elem[] =  str.split(TemplateUtils.editorLineSeparator);
		
		for(int i=0;i<elem.length;i++){
			if (!(removeEmptyLine)||(elem[i].length()!=0)){
				//if(i != elem.length-1){
					if(i!=elem.length-1) {
						ret.add(new EditorElement(elem[i]+TemplateUtils.editorLineSeparator,false));
					}else {
						ret.add(new EditorElement(elem[i],false));
					}
				//}
				//else{
				//	ret.add(new EditorElement(elem[i],false));
				//}
			}
		}
		*/
		return ret;
	}
	
	
	public static Vector getEditorElements(String fileContent,boolean removeEmptyLine,boolean replaceNewLineForUneditableElements){
		Vector  vi=new Vector ();
		
		String s1="";
		String s2="";
	
		int poz=0;
		int nEnd=-1;
		int nBegin=-1;
		
		
	do{
		nBegin=fileContent.indexOf("<!--",poz);
		nEnd=fileContent.indexOf("-->",poz);
		
		if( nBegin < 0  ||  nEnd < 0 ){
			s1=	fileContent.substring(poz,  fileContent.length());
			if(replaceNewLineForUneditableElements){
				s1 = s1.replaceAll("\n", "");
			}
			vi.addAll(splitElements(s1,removeEmptyLine));
		}
		else
		{
			s1=	fileContent.substring(poz, nBegin);
			if(replaceNewLineForUneditableElements){
				s1 = s1.replaceAll("\n", "");
			}
			vi.addAll(splitElements(s1,removeEmptyLine));
			
			s2 = fileContent.substring(nBegin+lstart, nEnd);
			vi.add(new EditorElement(s2,true));
			poz = nEnd+lstop;
		}
		
		
	}while((nBegin>0)&&(nEnd>0));
	return vi;

}
	
}


