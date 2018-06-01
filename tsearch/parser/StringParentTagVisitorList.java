package ro.cst.tsearch.parser;

import org.htmlparser.Node;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.util.NodeList;

public class StringParentTagVisitorList extends StringFindingVisitorList {
	
	private String enclosingTag = "";

	public StringParentTagVisitorList(String enclosingTag) {
		super("", true);
		this.enclosingTag = enclosingTag;
	}
	
	public StringParentTagVisitorList(String stringToFind, String enclosingTag) {
		super(stringToFind);
		
		this.stringToFind = stringToFind;
		this.enclosingTag = enclosingTag;
	}

	public StringParentTagVisitorList(String stringToFind, String enclosingTag, boolean caseSensitive) {
		this(stringToFind, enclosingTag);
		this.caseSensitive = caseSensitive;
		if (caseSensitive) {
			this.stringToFind = this.stringToFind.toUpperCase();
		}
	}
	
	@Override
	public void visitTag(Tag tag) {
		super.visitTag(tag);
		tag.getEndTag();
	}
	

	@Override
	public void visitStringNode(Text stringNode) {
		Node nextSibling = stringNode.getNextSibling();
		Node previousSibling = stringNode.getPreviousSibling();
		
		String prevText = previousSibling!= null ?previousSibling.getText():"";
		String nextText = nextSibling != null ? nextSibling.getText() : "";
		if (enclosingTag.equals(prevText) && ("/"+enclosingTag).equals(nextText)) {
			super.visitStringNode(stringNode);
		}
	}
	
}
