package ro.cst.tsearch.parser;

public class IndexTag{
	public int startTag;
	public int endTag;
	public int col;
	public int line;
	IndexTag(){
		super();
		col=0;
		line=0;
		startTag=0;
		endTag=0;
	}
}