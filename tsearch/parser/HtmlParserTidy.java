package ro.cst.tsearch.parser;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.utils.Tidy;

public class HtmlParserTidy {
	
	/**
	 * From the s html string extracts the node of type tag with id id<br>
	 * I had to add tag because getElementById is not working for me
	 * @param s
	 * @param id
	 * @param tag
	 * @return
	 */
	public static Node getNodeById(String s, String id, String tag){
		Document doc = Tidy.tidyParse(s);
		return getNodeById(doc,id,tag);
	}
	
	public static Node getNodeById(Document doc, String id, String tag) {
		if (doc == null){
			return null;
		}
		NodeList nl = doc.getElementsByTagName(tag);
		Node n = null;
		for (int i = 0; i< nl.getLength(); i++){
			n = nl.item(i);
			if (n.getAttributes().getNamedItem("id") != null
				&& n.getAttributes().getNamedItem("id").getNodeValue().equalsIgnoreCase(id)){
				return n;
			}
				
		}
		return null;
	}
	
	/**
	 * From the s html string extracts the node of type tag with attribute attr having the value attrValue<br>
	 */
	public static Node getNodeByTagAndAttr(String s, String tag, String attr, String attrValue){
		Document doc = Tidy.tidyParse(s);
		return getNodeByTagAndAttrFromDocument(doc,tag,attr,attrValue);
	}	
	
	/**
	 * 
	 * @param s
	 * @param tag
	 * @param attr
	 * @param attrValue
	 * @return
	 */
	public static Node getNodeByTagAndAttrFromDocument(Document doc, String tag, String attr, String attrValue){
		if (doc == null){
			return null;
		}
		NodeList nl = doc.getElementsByTagName(tag);
		Node n = null;
		for (int i = 0; i< nl.getLength(); i++){
			n = nl.item(i);
			if (n.getAttributes().getNamedItem(attr) != null
				&& n.getAttributes().getNamedItem(attr).getNodeValue().equalsIgnoreCase(attrValue)){
				return n;
			}
				
		}
		return null;
	}
	
	/**
	 * From the s html string extracts ALL the nodes of type tag with attribute attr having the value attrValue<br>
	 * @param s
	 * @param tag
	 * @param attr
	 * @param attrValue
	 * @return
	 */
	public static List<Node> getNodeListByTagAndAttr(String s, String tag, String attr, String attrValue){
		Document doc = Tidy.tidyParse(s);
		return getNodeListByTagAndAttr(doc,tag,attr,attrValue);
	}
	
	public static List<Node> getNodeListByTagAndAttr(Document doc, String tag, String attr, String attrValue){
		List<Node> l = new ArrayList<Node>();
		if (doc == null){
			return null;
		}
		NodeList nl = doc.getElementsByTagName(tag);
		Node n = null;
		for (int i = 0; i< nl.getLength(); i++){
			n = nl.item(i);
			if (n.getAttributes().getNamedItem(attr) != null
				&& n.getAttributes().getNamedItem(attr).getNodeValue().equalsIgnoreCase(attrValue)){
					l.add(n);
			}
				
		}
		return l;
	}
	
	public static String getAttributeValueFromNode(Node n, String attr) {
		if (n.getAttributes().getNamedItem(attr) != null) {
			return  n.getAttributes().getNamedItem(attr).getNodeValue();
		}
		return "";
	}
	
	/**
	 * returns the html of a Node or empty string if error occured
	 * @param cn
	 * @return
	 * @throws TransformerException
	 */
	public static String getHtmlFromNode(Node cn) throws TransformerException{
		if (cn == null)
			return "";
		
		DOMSource docClean = new DOMSource(cn);
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		transformer.setOutputProperty("method", "html");
		OutputStream os = new ByteArrayOutputStream();
		StreamResult sr = new StreamResult(os);
		if (docClean!=null){
			transformer.transform(docClean, sr);
			return os.toString();
		} else {
			return "";
		}
	}
	
	/**
	 * tags needs to be lowercase
	 * @param cn - start node
	 * @param tags - list of tags to be removed, it has to be lowercase
	 * @return
	 */
	public static Node removeTags(Node cn, HashSet<String> tags){
		NodeList nl = cn.getChildNodes();
		for (int i =0; i< nl.getLength(); i++){
			Node n = nl.item(i);
			if (tags.contains(n.getNodeName().toLowerCase())){
				cn.removeChild(n);
			} else {
				if (n.hasChildNodes()){
					n = removeTags(n, tags);
				}
			}
		}
		return cn;
	}
	
	/**
	 * Returns the value from a &lt;input name='name' value='<b>value</b>'&gt; tag
	 * @param doc
	 * @param name
	 * @return
	 */
	public static String getValueFromInputByName(Document doc, String name) {
		return HtmlParserTidy.getAttributeValueFromNode(HtmlParserTidy
				.getNodeByTagAndAttrFromDocument(doc, "input", "name",
						name), "value");
	}
	
	/**
	 * Returns the value from a &lt;tag name='name'&gt;<b>value</b>&lt;/tag&gt; tag
	 * @param doc
	 * @param name
	 * @return
	 */
	public static String getValueFromTagByName(Document doc, String name, String tag) {
		return getValueFromTagByAttr(doc,"name",name,tag);
	}
	
	/**
	 * Returns the value from a &lt;tag name='name'&gt;<b>value</b>&lt;/tag&gt; tag
	 * @param doc
	 * @param data.name
	 * @return
	 */
	public static String getValueFromTagById(Document doc, String id, String tag) {
		return getValueFromTagByAttr(doc,"id",id,tag);
	}
	
	/**
	 * Returns the value from a &lt;tag name='name'&gt;<b>value</b>&lt;/tag&gt; tag
	 * @param doc
	 * @param data.name
	 * @return
	 */
	public static String getValueFromTagByAttr(Document doc, String attr , String attrValue, String tag) {
		try {
			return HtmlParserTidy.getHtmlFromNode(HtmlParserTidy.getNodeByTagAndAttrFromDocument(doc, tag, attr,attrValue)).replaceAll("<"+tag+"[^>]*?>(.*?)</"+tag+">","$1");
		} catch (TransformerException e) { }
		return "";
	}
	
}
