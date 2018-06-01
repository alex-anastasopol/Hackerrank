package ro.cst.tsearch.utils;

import static ro.cst.tsearch.utils.XmlUtils.getChildren;
import static ro.cst.tsearch.utils.XmlUtils.parseXml;

import java.io.File;
import java.util.HashSet;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ro.cst.tsearch.servlet.BaseServlet;

public class CrossRefInitSingleton {
	
	private HashSet<String> noValidation = null;
	private static String FILE_NAME = BaseServlet.REAL_PATH
			+ "/WEB-INF/classes/resource/utils/crossRefExceptions.xml";

	private static class SingletonHolder {
		private static CrossRefInitSingleton instance = new CrossRefInitSingleton();
	} 
	
	public static CrossRefInitSingleton getInstance() {
		CrossRefInitSingleton crtInstance = SingletonHolder.instance;
		return crtInstance;
	}
	
	private CrossRefInitSingleton(){
		noValidation = new HashSet<String>();
		Document doc = parseXml(new File(FILE_NAME));
		for (Node root : getChildren(doc)) {
			if (root.getNodeName().equals("exceptions")) {
				for (Node entry : getChildren(root)) {
					if (entry.getNodeName().equals("entry")) {
						for (Node node : getChildren(entry)) {
							if("name".equals(node.getNodeName())){
								String name = XmlUtils.getNodeValue(node);
								if(!StringUtils.isEmpty(name))
									noValidation.add(name);
							}
						}
					}
				}
			}
		}
	}
	
	public boolean isNoValidation(String source){
		return noValidation.contains(source);
	}
}
