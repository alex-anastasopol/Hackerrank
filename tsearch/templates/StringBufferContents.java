package ro.cst.tsearch.templates;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import ro.cst.tsearch.utils.StringUtils;

public class StringBufferContents implements TemplateContents {

	StringBuffer str;
	Matcher m;
	String what = "";
	
	public StringBufferContents(StringBuffer str) {
		this.str = new StringBuffer(str);
	}

	public StringBufferContents(String str) {
		this.str = new StringBuffer(str);
	}
	
	@Override
	public boolean find() {
		return m.find();
	}

	@Override
	public void findAll(Pattern p) {
		m = p.matcher(str);
		what = p.toString();
	}

	@Override
	public void findAll(String what) {
		m = Pattern.compile(what).matcher(str);
		this.what = what;
	}

	@Override
	public String group() throws Exception {
		return m.group();
	}
	
	@Override
	public void replaceCurrentMatch(String replacement) {
		String match = m.group();
		replacement = match.replaceAll(what, replacement);
		str.replace(m.start(), m.end(), replacement);
	}
	
	@Override
	public void replaceCurrentMatchEscape(String replacement) {	
		str.replace(m.start(), m.end(), replacement);
	}	
	
	@Override
	public void resetFind() {
		m.reset();
	}

	@Override
	public void replaceAll(String what, String replacement) {
		String newStr = str.toString().replaceAll(what, replacement);
		str.delete(0, str.length());
		str.append(newStr);
	}
	
	@Override
	public String toString() {
		return str.toString();
	}

	public StringBuffer getStringBuffer() {
		return str;
	}

	@Override
	public void saveToFile(String filename, String extension) throws Exception {
		if ( !AddDocsTemplates.knownExtensions.containsKey(extension))
			throw new TemplatesException("No Format found for this extension");
		
		StringUtils.toFile(filename, str.toString());
	}

	@Override
	public void saveToFile(String filename) throws Exception {
		saveToFile(filename, filename.substring(filename.lastIndexOf('.')+1,filename.length()));
	}

	public Matcher getMatcher() {
		return m;
	}

	@Override
	public void replaceCurrentMatchEscapeHtml(String replacement) {
		replaceCurrentMatchEscape(replacement);
	}
}
