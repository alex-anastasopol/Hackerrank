package ro.cst.tsearch.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import ro.cst.tsearch.utils.StringUtils;

/**
 * @author Cornel HTML Parser
 * 
 */
public class HtmlParser2 {
	private int sesion = 0;
	public int ish = 0;
	public static int FILTER_TABLE_INDEX = 1;
	int tableIndex = 1;
	int filter = 0;
	int filterPosition = 0;
	boolean filterStatus = false;
	private Map<String, TagStruct> getId = new HashMap<String, TagStruct>();
	private Map<String, TagStruct> getName = new HashMap<String, TagStruct>();
	public Map<Integer, TagStruct> getTableIndex = new HashMap<Integer, TagStruct>();
	private Map<String, String> indexFilter = new HashMap<String, String>();
	private Map<String, String> filterIgnore = new HashMap<String, String>();
	private LinkedList<Integer> tagOp=new LinkedList<Integer>(); 
	public StringBuffer onlyText = new StringBuffer();
	private boolean isFilter = false;
	private String content = "";

	private LinkedList<TagStruct> tag = null;

	public boolean isFilter() {
		return isFilter;
	}

	public void setFilter(boolean isFilter) {
		this.isFilter = isFilter;
	}

	public void addFilter(String fillter) {
		this.isFilter = true;
		String[] filterr = fillter.split(",");
		for (int i = 0; i < filterr.length; i++) {
			this.filterIgnore.put(filterr[i].toUpperCase(), filterr[i]
					.toUpperCase());
		}
	}
	/**
	 * 
	 * @param content Html page
	 * @param filter containts tag name for exclude on this object Ex:"b,font,div"
	 */
	public HtmlParser2(String content, String filter) {
		//System.out.println("---->>>>>>    HTML Page Parser");
		if (content == null) {
			return;
		}
		addFilter(filter);
		if (StringUtils.isEmpty(content)) {
			return;

		}
		this.content = content.replaceAll("(?is)<STYLE.*?</STYLE[^>]*>", "");
		compile();

	}
	public void replaceTagAttribute(String tagName, String attribName, String replace){
		if(replace==null||tagName==null||attribName==null)
				return;
		for(int i=0;i<this.tag.size();i++){
			if(this.tag.get(i).tagName.equalsIgnoreCase(tagName)){
				if(replace.startsWith("%value%")){
					this.tag.get(i).setAttributes(attribName, replace.replaceAll("%value%",this.tag.get(i).getTagAttrib(attribName)));
				}
			}
		}
		
	}
	public void clearTagAttribExcept(String tags, String atribExcepted){
		String[] tmp = tags.split(",");
		int i = 0;
		TagStruct tmpTag = null;
		Map<String, String> attrib = new HashMap<String, String>();
		for (i = 0; i < tmp.length; i++) {
			attrib.put(tmp[i].toUpperCase(), tmp[i].toUpperCase());
		}
		for (i = 0; i < this.tag.size(); i++) {
			tmpTag = this.tag.get(i);
			if (tmpTag.tagType != TagStruct.TEXT_TAG) {
				if (attrib.get(tmpTag.tagName.toUpperCase()) != null) {
					this.tag.get(i).removeAttributesExcepted(atribExcepted);
				}
			}
		}


	}
	public TagStruct getTagByIndex(int index){
		return this.tag.get(index);
	}
	public void removeTag(TagStruct tag){
		if(tag==null)
				return;
		if(tag.tagType==TagStruct.BEGIN_TAG){
		for(int i=tag.endIndex;i>=tag.beginIndex;i--){
			this.tag.remove(i);
		}
			this.compileTag();	
		}
		
		else{
			if(tag.tagType==TagStruct.COMENT_TAG||tag.tagType==TagStruct.SIMPLE_TAG||tag.tagType==TagStruct.TEXT_TAG){
				this.tag.remove(tag.index);
				this.compileTag();
			}
		}
		
	}
	public TagStruct getTableByIndex(int index){
		return this.getTableIndex.get(index);
	}
	/**
	 *  
	 * @param tags multiple Tags EX: "td,tr,a" delete All attributes for all TD, TR, A tags 
	 */

	public void clearTagAtribb(String tags) {
		String[] tmp = tags.split(",");
		int i = 0;
		TagStruct tmpTag = null;
		Map<String, String> attrib = new HashMap<String, String>();
		for (i = 0; i < tmp.length; i++) {
			attrib.put(tmp[i].toUpperCase(), tmp[i].toUpperCase());
		}
		for (i = 0; i < this.tag.size(); i++) {
			tmpTag = this.tag.get(i);
			if (tmpTag.tagType != TagStruct.TEXT_TAG) {
				if (attrib.get(tmpTag.tagName.toUpperCase()) != null) {
					this.tag.get(i).removeAllAttributes();
				}
			}
		}
	}
	/**
	 * 
	 * @param content Html page
	 */
	public HtmlParser2(String content) {
		System.out.println("---->>>>>>    HTML Page Parser");
		if (content == null) {
			return;
		}
		if (StringUtils.isEmpty(content)) {
			return;

		}
		this.content = content.replaceAll("(?is)<STYLE.*?</STYLE[^>]*>", "");
		compile();
	}

	public void compile() {
		filter = FILTER_TABLE_INDEX;
		tableIndex = 1;
		this.indexFilter.put("table", "");
		this.indexFilter.put("td", "");
		this.indexFilter.put("tr", "");
		this.indexFilter.put("a", "");
		tag = new LinkedList<TagStruct>();
		if (!this.content.contains("<html")) {
			this.content = "<html>" + this.content + "</html>";
		}
		Reader r = new BufferedReader(new StringReader(this.content));
		HTMLEditorKit.Parser parser = new ParserDelegator();
		HTMLParseLister htmlParser = new HTMLParseLister();
		try {
			parser.parse(r, htmlParser, true);
			r.close();
			htmlParser = null;
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.content = "";
		// System.out.println(this.onlyText);
		compileTag();

	}
	public String toStringOpTagNameAndValue(){
		StringBuilder tmp= new StringBuilder();
		for (int i = 0; i < this.tagOp.size(); i++) {
			tmp.append(this.tag.get(this.tagOp.get(i)).getTagAttrib("name"));
			tmp.append("=");
			tmp.append(this.tag.get(this.tagOp.get(i)).getTagAttrib("value"));
			if(i<this.tagOp.size()-1)
				tmp.append("&");
		}

		return tmp.toString();
	}

	
	public String toStringOpTag(){
		StringBuilder tmp= new StringBuilder();
		for (int i = 0; i < this.tagOp.size(); i++) {
			tmp.append(this.tag.get(this.tagOp.get(i)).toStringBuilder());
			tmp.append("\n");
		}

		return tmp.toString();
	}

	public boolean operationSelect(String tagName, String attributes, String text){
		TagStruct tmp=null;
		tagOp=new LinkedList<Integer>();
		for(int i=0;i<this.tag.size();i++){
			tmp=this.tag.get(i);
			if(tmp.tagName.equalsIgnoreCase(tagName)){
				if(!tmp.getTagAttrib(attributes).equals(tmp.getTagAttrib(attributes).replaceAll(text, ""))){
					this.tagOp.addLast(tmp.index);
	//				
				}
			}
		}
		
		return true;
	}
	/**
	 * 
	 * @param parentTag handleStartTag(multiple tag)
	 * @param leaf text
	 * @return boolean true if leaf it is containts on children tag on parentTag
	 */
	public boolean isLeaf(TagStruct parentTag, String leaf) {
		TagStruct tmpTag;
		for (int i = parentTag.beginIndex + 1; i < parentTag.endIndex; i++) {
			tmpTag = this.tag.get(i);
			if (tmpTag.tagType == TagStruct.TEXT_TAG) {
				if (tmpTag.tagName.contains(leaf)) {
					return true;
				}
			}

		}
		return false;
	}
	public boolean isLeafRegEx(TagStruct parentTag, String leaf) {
		TagStruct tmpTag;
		for (int i = parentTag.beginIndex + 1; i < parentTag.endIndex; i++) {
			tmpTag = this.tag.get(i);
			if (tmpTag.tagType == TagStruct.TEXT_TAG) {
				if (!tmpTag.tagName.equals(tmpTag.tagName.replaceAll(leaf, ""))) {
					return true;
				}
			}

		}
		return false;
	}

	/**
	 * 
	 * @param parentTag handleStartTag(multiple tag)
	 * @param leaf text
	 * @return Tag leaf children on tag parentTag
	 */
	public TagStruct getTagLeafCondition(String tagName, String leaf) {
		TagStruct tmpTag;
		for (int i = 0; i < this.tag.size(); i++) {
			tmpTag = this.tag.get(i);
			if (tmpTag.tagName.equalsIgnoreCase(tagName)) {
				if (isLeaf(tmpTag, leaf)) {
					return tmpTag;
				}
			}
		}
		return new TagStruct();
	}

	public TagStruct getTagLeafRegEx(String tagName, String regEx) {
		TagStruct tmpTag;
		for (int i = 0; i < this.tag.size(); i++) {
			tmpTag = this.tag.get(i);
			if (tmpTag.tagName.equalsIgnoreCase(tagName)) {
				if (isLeafRegEx(tmpTag, regEx)) {
					return tmpTag;
				}
			}
		}
		return new TagStruct();
	}
	public TagStruct getParent(int parentLevel,TagStruct childrean){
		int k=childrean.index-1;
		if(k>=0){
			for(k=childrean.index-1;k>0;k--){
				if(childrean.tabTag==this.tag.get(k).tabTag+parentLevel){
					return this.tag.get(k);
					}
				}
			}
		return new TagStruct();
	}
	private int getEndIndex(int startIndex) {
		TagStruct tmpTag = null;
		TagStruct tmpTag2 = this.tag.get(startIndex);
		if (this.tag.get(startIndex).tagType == TagStruct.BEGIN_TAG) {
			int k = 0;
			k = startIndex + 1;
			int sesion2 = 1;
			while ((sesion2 > 0) && (this.tag.size() > k)) {
				tmpTag = this.tag.get(k);
				if ((tmpTag2.tagName.equalsIgnoreCase(tmpTag.tagName))
						&& (TagStruct.BEGIN_TAG == tmpTag.tagType)) {
					++sesion2;
				}
				if ((tmpTag2.tagName.equalsIgnoreCase(tmpTag.tagName))
						&& (tmpTag.tagType == TagStruct.END_TAG)) {
					--sesion2;
				}

				++k;
			}
			return k;
		} else {
			return startIndex;
		}

	}
	public StringBuilder toStringBuilder(){
		StringBuilder tmp= new StringBuilder();
		for (int i = 0; i < this.tag.size(); i++) {
			for (int j = 0; j < this.tag.get(i).tabTag; j++) {
				tmp.append( "\t");
			}
			tmp.append(this.tag.get(i).toStringBuilder());
			tmp.append("\n");
		}

		return tmp;
	}
	public StringBuilder toStringBuilder2(){
		StringBuilder tmp= new StringBuilder();
		for (int i = 1; i < this.tag.size()-1; i++) {
			for (int j = 0; j < this.tag.get(i).tabTag; j++) {
				tmp.append( "\t");
			}
			tmp.append(this.tag.get(i).toStringBuilder());
			tmp.append("\n");
		}

		return tmp;
	}
	/**
	 * convert this object to String
	 */
	public String toString() {
		/*
		String tmp = "";
		for (int i = 0; i < this.tag.size(); i++) {
			for (int j = 0; j < this.tag.get(i).tabTag; j++) {
				tmp += "\t";
			}
			tmp += this.tag.get(i).toString() + "\n";
		}
		return tmp;*/
		return this.toStringBuilder().toString();
	}
	public String toString2() {
		/*
		String tmp = "";
		for (int i = 0; i < this.tag.size(); i++) {
			for (int j = 0; j < this.tag.get(i).tabTag; j++) {
				tmp += "\t";
			}
			tmp += this.tag.get(i).toString() + "\n";
		}
		return tmp;*/
		return this.toStringBuilder2().toString();
	}
	/**
	 * convert tag to String
	 * @param tag Tag childrean on this object 
	 * @return String if is multiple tag convert All chialdren to String
	 */
	public String toStringTag(TagStruct tag) {
		/*
		String tmp = "";
		if (tag != null) {
			for (int i = tag.beginIndex; i < tag.endIndex; i++) {
				for (int j = tag.tabTag; j < this.tag.get(i).tabTag; j++) {
					tmp += "\t";
				}
				tmp += this.tag.get(i).toString() + "\n";
			}
		}
		return tmp;*/
		return toStringBuilderTag(tag).toString();
	}
	public StringBuilder toStringBuilderTag(TagStruct tag){
		StringBuilder tmp = new StringBuilder();
		if (tag != null) {
			for (int i = tag.beginIndex; i < tag.endIndex; i++) {
				for (int j = tag.tabTag; j < this.tag.get(i).tabTag; j++) {
					tmp.append("\t");
				}
				tmp.append(this.tag.get(i).toStringBuilder());
				tmp.append("\n");
			}
		}

		return tmp;
	}
	
	/**
	 * 
	 * @param id (valueId) from id Attribute tag  
	 * @return tag Object
	 */
	public TagStruct getElementByID(String id) {
		if (this.getId.get(id) == null) {
			return new TagStruct();
		} else {
			return this.getId.get(id);
		}
	}
	/**
	 * 
	 * @param index tag
	 * @return
	 */
	public TagStruct getElementByTableOnIndex(int index) {
		if (this.getTableIndex.get(index) == null) {
			return new TagStruct();
		} else {
			return this.getTableIndex.get(index);
		}
	}
	/**
	 * remove multiple tag with index>start and index<exd
	 * @param start index
	 * @param end index
	 */
	public void triateStruct(int start, int end) {
		LinkedList<TagStruct> tag1 = new LinkedList<TagStruct>();
		for (int i = start; i <= end; i++) {
			tag1.add(this.tag.get(i));
		}
		this.tag = tag1;
		compileTag();
	}

	private void compileTag() {
		TagStruct tmpTag = null;

		getName = new HashMap<String, TagStruct>();
		getId = new HashMap<String, TagStruct>();
		getTableIndex = new HashMap<Integer, TagStruct>();
		int i = 0;
		int tableIndex = 0;
		for (i = 0; i < this.tag.size(); i++) {
			tmpTag = this.tag.get(i);
			tmpTag.index = i;

			if (tmpTag.tagType == TagStruct.BEGIN_TAG) {
				tmpTag.beginIndex = i;
				tmpTag.endIndex = getEndIndex(i);
			}
			if ((tmpTag.tagType == TagStruct.BEGIN_TAG)
					|| (tmpTag.tagType == TagStruct.SIMPLE_TAG)) {
				tmpTag.name = tmpTag.getTagAttrib("name");
				tmpTag.id = tmpTag.getTagAttrib("id");
				getId.put(tmpTag.id, tmpTag);
				getName.put(tmpTag.name, tmpTag);
				if (tmpTag.tagName.equalsIgnoreCase("table")) {
					getTableIndex.put(tableIndex, tmpTag);
					++tableIndex;
				}
			}
		}

	}
	/**
	 * @param name = name, id
	 * @param value =myNameTag, myIdTag
	 * @return TagStruct
	 */
	public TagStruct getTagBy(String name, String value) {
		for (int i = 0; i < this.tag.size(); i++) {
			if (this.tag.get(i).attributes.size() > 0) {
				if (this.tag.get(i).getTagAttrib(name).compareToIgnoreCase(
						value) == 0) {
					return this.tag.get(i);
				}
			}
		}
		return new TagStruct();
	}

	/**
	 * 
	 * @param tagName = name, id
	 * @param tagValue = myNameTag, myIdTag
	 * @return all text tag  into handleStartTag 
	 */
	public String getOnlyTextIntoTag(String tagName, String tagValue) {
		String tmpString = "";
		LinkedList<TagStruct> tmpList = this.getAllStructByTagName(tagName,
				tagValue);
		if (tmpList != null) {
			for (int i = 0; i < tmpList.size(); i++) {
				if (tmpList.get(i).tagType == TagStruct.TEXT_TAG) {
					if (tmpList.get(i).tagName != null)
						tmpString += tmpList.get(i).tagName;
				}
			}

		}
		return tmpString;
	}
	/**
	 * 
	 * @param tagName id, mane
	 * @param tagValue myTagID, myTagName
	 * @return list with All children tag 
	 */
	public LinkedList<TagStruct> getAllStructByTagName(String tagName,
			String tagValue) {
		TagStruct tagTmp = this.getTagBy(tagName, tagValue);
		LinkedList<TagStruct> listTmp = new LinkedList<TagStruct>();
		if (tagTmp != null) {
			listTmp.add(tagTmp);
			if (tagTmp.tagType == TagStruct.BEGIN_TAG) {
				int k = 0;
				k = tagTmp.index + 1;
				this.sesion = 1;
				while ((sesion > 0) && (this.tag.size() > k)) {
					if ((tagTmp.tagName
							.equalsIgnoreCase(this.tag.get(k).tagName))
							&& (TagStruct.BEGIN_TAG == this.tag.get(k).tagType)) {
						++this.sesion;
					}
					if ((tagTmp.tagName
							.equalsIgnoreCase(this.tag.get(k).tagName))
							&& (this.tag.get(k).tagType == TagStruct.END_TAG)) {
						--this.sesion;
					}
					listTmp.add(this.tag.get(k));
					++k;
				}
			}
			return listTmp;
		}

		return null;

	}

	class HTMLParseLister extends HTMLEditorKit.ParserCallback {
		public int ind = 0;
		public int tabTag = 0;

		public void handleText(char[] data, int pos) {
			// System.err.println("text >> "+data.toString());

			onlyText.append(data);
			TagStruct tagTmp = new TagStruct();
			tagTmp.tagName = new String(data);
			tagTmp.tagName=tagTmp.tagName.replaceAll("&", "&amp;");
			tagTmp.tagName=tagTmp.tagName.replaceAll("<", "&lt;");
			tagTmp.tagName=tagTmp.tagName.replaceAll(">", "&gt;");
			//tagTmp.tagName=tagTmp.tagName.replaceAll("(?is)[^\\sa-zA-Z0-9<>,./?'\";:}{\\[\\]\\(\\)-_+=~`!@#$%^&*]+", "");
			
			tagTmp.tagType = TagStruct.TEXT_TAG;
			tagTmp.index = ind;
			tagTmp.tabTag = tabTag;
			tag.add(tagTmp);

			++ind;

		}

		public void handleComment(char[] data, int pos) {

			TagStruct tagTmp = new TagStruct();
			tagTmp.tagName = data.toString();

			tagTmp.tagType = TagStruct.COMENT_TAG;
			tagTmp.index = ind;
			tagTmp.tabTag = tabTag;
			tag.add(tagTmp);
			++ind;

		}

		@SuppressWarnings("static-access")
		public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
			if (filterIgnore.get(t.toString().toUpperCase()) == null) {
				if (t == t.HTML) {
					ish = 1;
				}
				if (ish == 1) {
					TagStruct tagTmp = new TagStruct();
					// System.out.println(">>tag _"+t.toString()+"___" );
					tagTmp
							.addElement(t.toString(), TagStruct.BEGIN_TAG, a,
									ind);
					tag.add(tagTmp);
					tagTmp.tabTag = tabTag;
					++ind;
					++tabTag;
				}
			}
		}

		@SuppressWarnings("static-access")
		public void handleEndTag(HTML.Tag t, int pos) {
			if (filterIgnore.get(t.toString().toUpperCase()) == null) {
				if (ish == 1) {
					TagStruct tagTmp = new TagStruct();
					tagTmp.tagName = t.toString();
					tagTmp.tagType = TagStruct.END_TAG;
					tagTmp.index = ind;
					tagTmp.tabTag = tabTag - 1;
					tag.add(tagTmp);
					++ind;

					if (t == t.HTML) {
						ish = 0;
					}
				}
				--tabTag;
			}
		}

		public void handleError(String errorMsg, int pos) {
			//System.err.println("IGNORE Error TAG>>>   "+errorMsg);
			String ss=errorMsg;
		}

		public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
			if (filterIgnore.get(t.toString().toUpperCase()) == null) {
				if (ish == 1) {
					TagStruct tagTmp = new TagStruct();
					tagTmp.addElement(t.toString(), TagStruct.SIMPLE_TAG, a,
							ind);

					tagTmp.tabTag = tabTag;
					tag.add(tagTmp);
					++ind;
				}
			}
		}
	}

	public LinkedList<Integer> getTagOp() {
		return tagOp;
	}

	public void setTagOp(LinkedList<Integer> tagOp) {
		this.tagOp = tagOp;
	}

}