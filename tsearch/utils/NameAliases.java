package ro.cst.tsearch.utils;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ro.cst.tsearch.servlet.BaseServlet;

import static ro.cst.tsearch.utils.StringUtils.*;
import static ro.cst.tsearch.utils.XmlUtils.*;

public class NameAliases {

	/**
	 * prevent instantiation
	 */
	private NameAliases() {
	}

	/**
	 * map with all aliases
	 */
	private final static Map<String, Collection<String>> aliases = new HashMap<String, Collection<String>>();

	/**
	 * load the aliases from XML file
	 */
	static {
		String FILE_NAME = BaseServlet.REAL_PATH
				+ "/WEB-INF/classes/resource/utils/name_aliases.xml";
		Document doc = parseXml(new File(FILE_NAME));
		for (Node n0 : getChildren(doc)) {
			if (n0.getNodeName().equals("aliases")) {
				for (Node n1 : getChildren(n0)) {
					if (n1.getNodeName().equals("entry")) {
						String name = null;
						Set<String> nicks = new LinkedHashSet<String>();
						for (Node n2 : getChildren(n1)) {
							String n2Name = n2.getNodeName();
							if ("name".equals(n2Name)) {
								name = getNodeValue(n2);
							} else if ("nicknames".equals(n2Name)) {
								for (Node n3 : getChildren(n2)) {
									if ("name".equals(n3.getNodeName())) {
										String nick = getNodeValue(n3);
										if (!isEmpty(nick)) {
											nicks.add(nick.toUpperCase());
										}
									}
								}
							}
						}
						if (!StringUtils.isEmpty(name) && nicks.size() != 0) {
							aliases.put(name.toUpperCase(), nicks);
						}
					}
				}
			}
		}
	}

	/**
	 * Get name aliases
	 * 
	 * @param seed
	 *            original name
	 * @return collection of aliases, empty if no aliases defined
	 */
	public static Collection<String> getAliases(String seed) {
		Collection<String> col = aliases.get(seed.toUpperCase());
		if (col == null) {
			return new LinkedList<String>();
		}
		return Collections.unmodifiableCollection(col);
	}

}
