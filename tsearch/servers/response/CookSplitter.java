package ro.cst.tsearch.servers.response;

import java.util.*;
import java.io.*;
import java.util.regex.*;

import org.apache.log4j.Category;

public class CookSplitter {
	protected static final Category logger= Category.getInstance(CookSplitter.class.getName());
	protected String html, links;
	protected ArrayList bounds=new ArrayList();
	protected boolean hasNext, hasPrev;
	
	public void setDoc(String s) {
		html=s;
		parse();
	}
	
	public String getDoc() {
		return html;
	}
	
	public String getSplitDoc(int i) throws Exception {
		if (i>=getSplitNo() || i<0)
			throw new Exception("DavidsonSplit : Index out of range : "+i+" from "+getSplitNo());
		int b=((Integer)bounds.get(i)).intValue(),
		e=((Integer)bounds.get(i+1)).intValue();
		StringBuffer sb=new StringBuffer();
		sb.append(html.substring(b + "-----------------".length(), e));
		return sb.toString();
	}
	
	public int getSplitNo() {
		return bounds.size()-1;
	}
	
	private void parse() {
		
		int i=0;
		while ((i=html.indexOf("-----------------", i))!=-1) {
			bounds.add(new Integer(i));
			i+="-----------------".length();
		}
	}
}
