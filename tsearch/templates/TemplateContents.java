package ro.cst.tsearch.templates;

import java.util.regex.Pattern;

public interface TemplateContents {
	
	public String toString();
	
	public void replaceAll(String what, String replacement);
	
	public void findAll(Pattern p);
	
	public void findAll(String what);
	
	public boolean find();
	
	public String group() throws Exception;

	void replaceCurrentMatch(String replacement);

	void resetFind();

	void saveToFile(String filename, String extension) throws Exception;

	void saveToFile(String filename) throws Exception;

	void replaceCurrentMatchEscape(String replacement);

	public void replaceCurrentMatchEscapeHtml(String replacement);
}
