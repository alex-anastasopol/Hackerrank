package ro.cst.tsearch.parser;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.text.MutableAttributeSet;

public class TagStruct {
	public static int TEXT_TAG = 1;
	public static int COMENT_TAG = 2;
	public static int BEGIN_TAG = 3;
	public static int END_TAG = 4;
	public static int SIMPLE_TAG = 5;
	public int tagType = 0;
	public int index = 0;
	public int beginIndex = 0;
	public int endIndex = 0;
	public int tabTag = 0;
	public String id = "";
	public String name = "";
	public String tagName = "";
	LinkedList<Attrib> attributes = null;

	public TagStruct() {
		super();
		tagName = new String();
		tagType = 0;
		index = 0;
		attributes = new LinkedList<Attrib>();
	}
	/**
	 * 
	 * @param tagName name tag input, table ...
	 * @param tagType beginTag, simpleTag
	 * @param a attributes Tag
	 * @param index Index tag on list tag
	 */
	public void addElement(String tagName, int tagType, MutableAttributeSet a,
			int index) {
		this.tagName = tagName;
		this.tagType = tagType;
		this.index = index;
		Enumeration e = null;
		for (e = a.getAttributeNames(); e.hasMoreElements();) {
			Object obj = e.nextElement();
			Attrib at = new Attrib();
			at.name = obj.toString();
			at.value = a.getAttribute(obj).toString();
			this.attributes.add(at);
		}
	}
	
	public void setAttributes(String name,String value){
		for(int i=0;i<this.attributes.size();i++){
			if(name.equalsIgnoreCase(this.attributes.get(i).name)){
				this.attributes.get(i).name=name;
				this.attributes.get(i).value=value;
				return;
			}
			this.attributes.add(new Attrib(name,value));
		}
	}
	/**
	 * 
	 * @param nameAtrib delete attribue Tag
	 */
	public void removeAttributes(String nameAtrib) {
		for (int i = 0; i < this.attributes.size(); i++) {
			if (this.attributes.get(i).name.compareToIgnoreCase(nameAtrib) == 0) {
				this.attributes.remove(i);

			}
		}
	}
	public void removeAttributesExcepted(String atributtesEx){
		String[] tmp = atributtesEx.split(",");
		int i = 0;
		Map<String, String> attrib = new HashMap<String, String>();
		for (i = 0; i < tmp.length; i++) {
			attrib.put(tmp[i].toUpperCase(), tmp[i].toUpperCase());
		}
		for(i=this.attributes.size()-1;i>=0;i--){
			if(attrib.get(this.attributes.get(i).name.toUpperCase())==null){
				this.attributes.remove(i);
			}
		}

	}
	/**
	 * delete all attributes 
	 */
	
	public void removeAllAttributes() {
		this.attributes =  new LinkedList<Attrib>();
	}
	/**
	 * 
	 * @param name attributes
	 * @return value attributes
	 */
	public String getTagAttrib(String name) {
		Attrib atrib = null;
		for (int i = 0; i < this.attributes.size(); i++) {
			atrib = this.attributes.get(i);
			if (name.compareToIgnoreCase(atrib.name) == 0) {
				return atrib.value;
			}
		}
		return "";
	}
	/**
	 * convert to String all Atributes 
	 */
	public String toStringAttributes() {
		String temp = "";
		for (int i = 0; i < this.attributes.size(); i++) {
			temp += this.attributes.get(i).toString();
		}
		return temp;
	}
	public StringBuilder toStringBuilderAttributes(){
		StringBuilder tmp= new StringBuilder();
		for (int i = 0; i < this.attributes.size(); i++) {
			tmp.append(this.attributes.get(i).toStringBuilder());
		}
		
		return tmp;
	}
	/**
	 * convert to String Tag
	 */
	public StringBuilder toStringBuilder(){
		StringBuilder tmp = new StringBuilder();
		if (this.tagType == TagStruct.TEXT_TAG) {
			tmp.append(this.tagName);
			return tmp;
		}
		if (this.tagType == TagStruct.COMENT_TAG) {
			tmp.append("<!--");
			tmp.append(this.tagName);
			tmp.append("-->");
			return tmp;
		}
		if ((this.tagType == TagStruct.SIMPLE_TAG)
				|| (this.tagType == TagStruct.BEGIN_TAG)) {
			tmp.append("<");
			tmp.append(this.tagName);
			tmp.append(" ");
			tmp.append(this.toStringBuilderAttributes());
			tmp.append(">");
			return tmp;
		}
		if (this.tagType == TagStruct.END_TAG) {
			tmp.append("</");
			tmp.append(this.tagName);
			tmp.append(">");
			return tmp;
		}
		
		return new StringBuilder("");
	}
	public String toString() {
		if (this.tagType == TagStruct.TEXT_TAG) {
			return this.tagName;
		}
		if (this.tagType == TagStruct.COMENT_TAG) {
			return "<!--" + this.tagName + "-->";
		}
		if ((this.tagType == TagStruct.SIMPLE_TAG)
				|| (this.tagType == TagStruct.BEGIN_TAG)) {
			return "<" + this.tagName + " " + this.toStringAttributes() + ">";
		}
		if (this.tagType == TagStruct.END_TAG) {
			return "</" + this.tagName + ">";
		}
		return "";
	}
}

class Attrib {
	Attrib() {
		super();
		name = new String();
		value = new String();

	}
	Attrib(String name, String value) {
		super();
		this.name = name;
		this.value = value;

	}
	public String name = "";
	public String value = "";
	public StringBuilder toStringBuilder(){
		StringBuilder tmp = new StringBuilder();
		if (!"".equalsIgnoreCase(name)) {
			tmp.append(" ");
			tmp.append(this.name);
			tmp.append("=\"");
			tmp.append(this.value);
			tmp.append("\" ");
			
		} else {
			tmp.append("");
		}
	
		return tmp;
	}
	public String toString() {
		if (!"".equalsIgnoreCase(name)) {
			return " " + this.name + "=\"" + this.value + "\" ";
		} else {
			return "";
		}
	}
}