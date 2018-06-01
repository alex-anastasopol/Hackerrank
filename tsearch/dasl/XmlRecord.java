package ro.cst.tsearch.dasl;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Node;

import static ro.cst.tsearch.utils.XmlUtils.*;

public class XmlRecord {

	public String name = "";
	public String value = "";
	public Set<XmlRecord> children = new LinkedHashSet<XmlRecord>();

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof XmlRecord)) {
			return false;
		}
		XmlRecord other = (XmlRecord) obj;
		if (!name.equals(other.name)) {
			return false;
		}
		if (!value.equals(other.value)) {
			return false;
		}
		return children.equals(other.children);
	}

	@Override
	public int hashCode() {
		return 13 * name.hashCode() + 
			   19 * value.hashCode() + 
			   23 * children.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<" + name + ">");
		if (children.size() == 0) {
			sb.append(StringEscapeUtils.escapeXml(value));
		} else {
			for (XmlRecord child : children) {
				sb.append(child.toString());
			}
		}
		sb.append("</" + name + ">");
		return sb.toString();
	}

	/**
	 * Parse a Node into an XmlRecord
	 * @param node
	 * @return
	 */
	public static XmlRecord parseXmlRecord(Node node) {
		XmlRecord record = new XmlRecord();
		record.name = node.getNodeName();
		record.value = getNodeValue(node);
		for(Node child: getChildren(node)){
			if("#text".equals(child.getNodeName())){
				continue;
			}
			record.children.add(parseXmlRecord(child));
		}		
		return record;
	}
}