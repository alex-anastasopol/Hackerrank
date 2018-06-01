package ro.cst.tsearch.parser;
import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author Cornel
 * Small HTML parser 
 * Is not thread safe
 */
public class HtmlParser {

	private Map<String,Input> nodeMap = new HashMap<String,Input>();
	private Map<String,Input> allNodeMap = new HashMap<String,Input>();
	public LinkedList<Input> filter(Input input){
		LinkedList<Input> list = new LinkedList<Input>();
		Object[] obj = this.allNodeMap.keySet().toArray();
		for(int i=0;i<obj.length;i++){
			if(this.allNodeMap.get(obj[i]).compareTo(input)){
				list.add(this.allNodeMap.get(obj[i]));
			}
		}
		return list;
	}
	public static String toStringList(String separator,LinkedList<Input> list){
		String tmp="";
		if(list.size()>0){
			tmp=list.get(0).name+"="+list.get(0).value;
		}
			
		for(int i=1;i<list.size();i++){
			tmp=tmp+separator+list.get(i).name+"="+list.get(i).value;
		}
		return tmp;
	}
	public HtmlParser(String content) {
		if (StringUtils.isEmpty(content)) {
			return;
		}
		Reader r = new BufferedReader(new StringReader(content));
		HTMLEditorKit.Parser parser = new ParserDelegator();
		HTMLParseLister htmlParser = new HTMLParseLister();
		try {
			parser.parse(r, htmlParser, true);
			r.close();
			htmlParser = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Input getElementByName(String tagName) {
		if(nodeMap.get(tagName.toUpperCase())!=null){
			return nodeMap.get(tagName.toUpperCase());
		}
		else{
			Input tmp=new Input();
			tmp.id="";
			tmp.name="";
			tmp.tagName="";
			tmp.type="";
			tmp.value="";
			return tmp;
		}
	}

	public Input getElementById(String tagId) {
		if(nodeMap.get(tagId.toUpperCase())!=null){
			return nodeMap.get(tagId.toUpperCase());
		}
		else{
			Input tmp=new Input();
			tmp.id=null;
			tmp.name=null;
			tmp.tagName=null;
			tmp.type=null;
			tmp.value=null;
			return tmp;
		}
		
	}

	private class HTMLParseLister extends HTMLEditorKit.ParserCallback {

		public void handleText(char[] data, int pos) {
		}
		public void handleComment(char[] data, int pos) {
		}
		public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
			handleSimpleTag(t,a,pos);
		}
		public void handleEndTag(HTML.Tag t, int pos) {
		}
		public void handleError(String errorMsg, int pos) {
		}
		
		public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet a, int pos) {
			Enumeration e = null;
			Input list =null;
			String id="";
			String name="";
			list = new Input();
			list.setTagName(tag.toString());

			for (e = a.getAttributeNames(); e.hasMoreElements();) {

				Object obj = e.nextElement();
				String typeTag = obj.toString();
				if (typeTag.equalsIgnoreCase("id")) {
					id = a.getAttribute(obj).toString();
					list.setId(id);
				}
				if (typeTag.equalsIgnoreCase("value")) {
					list.setValue(a.getAttribute(obj).toString());
				}
				if (typeTag.equalsIgnoreCase("name")) {
					name = a.getAttribute(obj).toString();
					list.setName(name);
				}
				if (typeTag.equalsIgnoreCase("type")) {
					list.setType(a.getAttribute(obj).toString());
				}
				if (typeTag.equalsIgnoreCase("action")) {
					list.setAction(a.getAttribute(obj).toString());
				}
			}
			if ( tag.toString().equalsIgnoreCase("INPUT") ) {
					nodeMap.put(id.toUpperCase(),list);
					nodeMap.put(name.toUpperCase(),list);
			}
			allNodeMap.put(id.toUpperCase(),list);
			allNodeMap.put(name.toUpperCase(),list);
				
			

		}
	}
}
