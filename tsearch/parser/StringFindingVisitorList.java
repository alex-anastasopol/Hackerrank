package ro.cst.tsearch.parser;

import org.htmlparser.Text;
import org.htmlparser.util.NodeList;
import org.htmlparser.visitors.NodeVisitor;

public class StringFindingVisitorList extends NodeVisitor {

	protected NodeList occurences = new NodeList();
	protected String stringToFind = "";
	protected boolean caseSensitive = true;
	protected boolean needExactMatch = false; 
	
	public StringFindingVisitorList(String stringToFind) {
		this.stringToFind = stringToFind;	
		
	}
	
	public StringFindingVisitorList(String stringToFind, boolean caseSensitive) {
		this(stringToFind);
		this.caseSensitive = caseSensitive;
		if(caseSensitive) {
			this.stringToFind = this.stringToFind.toUpperCase();
		}
	}	
	
	@Override
    public void visitStringNode(Text stringNode)
    {
        String stringToBeSearched = stringNode.getText();
        if(!caseSensitive) {
        	stringToBeSearched = stringToBeSearched .toUpperCase();
        }
        if(needExactMatch) {
        	if (stringToBeSearched.equals(stringToFind)) {
                occurences.add(stringNode);
            }
        }else {
        	if (stringToBeSearched.indexOf(stringToFind) != -1) {
                occurences.add(stringNode);
            }	
        }
        
    }

	public NodeList getOccurences() {
		return occurences;
	}
	
	public Text getFirstOccurence() {
		if(occurences.size()>0) {
			return (Text) occurences.elementAt(0);
		}else {
			return null;
		}
	}

	public boolean isNeedExactMatch() {
		return needExactMatch;
	}

	public void setNeedExactMatch(boolean needExactMatch) {
		this.needExactMatch = needExactMatch;
	}
	
	
}
