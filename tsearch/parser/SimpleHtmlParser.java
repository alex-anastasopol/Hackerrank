package ro.cst.tsearch.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

import ro.cst.tsearch.utils.StringUtils;

import ro.cst.tsearch.utils.FileUtils;

/**
 * Class to be used for invalid HTML
 * 
 * @author radu bacrau
 */
public class SimpleHtmlParser {

	private static final Pattern startFormPattern = Pattern.compile("(?i)(<form[^>]*>)");
	private static final Pattern endFormPattern = Pattern.compile("(?i)</form>");
	private static final Pattern startSelectPattern = Pattern.compile("(?i)(<select[^>]*>)");
	private static final Pattern endSelectPattern = Pattern.compile("(?i)</select>");
	private static final Pattern inputPattern = Pattern.compile("(?i)(<input[^>]*>)");
	private static final Pattern optionPattern = Pattern.compile("(?i)(<option[^>]*>)([^>]*)</option>");
	private static final Pattern namePattern = Pattern.compile("(?i)name\\s*=\\s*[\"']?([^\"']*)[\"']?");
	private static final Pattern actionPattern = Pattern.compile("(?i)action\\s*=\\s*[\"']?([^\"']*)[\"']?");
	private static final Pattern methodPattern = Pattern.compile("(?i)method\\s*=\\s*[\"']?([^\"']*)[\"']?");
	private static final Pattern valuePattern = Pattern.compile("(?i)value\\s*=\\s*[\"]?([^\">]*)[\">]");
	private static final Pattern valuePatternApos = Pattern.compile("(?i)value\\s*=\\s*[']?([^'>]*)['>]");
	private static final Pattern typePattern = Pattern.compile("(?i)type\\s*=\\s*[\"']([^\"']*)[\"']");
	private static final Pattern idPattern = Pattern.compile("(?i)id\\s*=\\s*[\"']([^\"']*)[\"']");
	private static final Pattern attrPattern = Pattern.compile("(?i)([a-z0-9_-]+)=[\"']([^\"']*)[\"']");
	
	public List<Form> forms = new ArrayList<Form>();

	public static class Option {

		public String value = "";
		public String text = "";
		public boolean selected = false;
		public int index = 0;
		public Map<String,String> attributes = new LinkedHashMap<String,String>();
		
		public String toString() {
			return "\nOption(index=" + index + ",text=" + text + ",attributes=" + attributes + ",selected=" + selected +")";
		}

		public Option(String text, int index) {
			this.index = index;
			Matcher optionMatcher = optionPattern.matcher(text);
			if (optionMatcher.find()) {
				this.text = optionMatcher.group(2);
				value = extractParameter(optionMatcher.group(1), valuePattern);
			}
			Matcher attrMatcher = attrPattern.matcher(text);
			while(attrMatcher.find()){
				attributes.put(attrMatcher.group(1), attrMatcher.group(2));
			}	
			text = text.toLowerCase();
			selected = text.contains("selected=\"selected\"") || text.contains(" selected");
		}
	}

	public static class Select {

		public String name = "";
		public List<Option> options = new ArrayList<Option>();

		public String toString() {
			return "\nSelect(name=" + name + ", options=" + options + ")";
		}

		public Select(String header, String contents) {

			name = extractParameter(header, namePattern);

			contents = contents.replaceAll("[\n\r]", " ");
			contents = contents.replaceAll(">\\s+<", "><");
			contents = contents.replaceAll("\\s*([<>])\\s*", "$1");

			Matcher optionMatcher = optionPattern.matcher(contents);
			int idx = 0;
			while (optionMatcher.find()) {				
				options.add(new Option(optionMatcher.group(0), idx++));
			}
		}
		
		public Option getOption(String name){
			for(Option option: options){
				if(option.text.toLowerCase().contains(name.toLowerCase())){
					return option;
				}
			}
			return null;
		}
		
		public String getValue(){			
			for(Option option: options){
				if(option.selected){
					return option.value;
				}
			}
			return "";			
		}
	}

	public static class Input {

		public String name = "";
		public String value = "";
		public String id = "";
		public String type = "";

		public String toString() {
			return "\nInput(name=" + name + ", value=" + value + ",type=" + type + ")";
		}

		public Input(String str) {
			name = extractParameter(str, namePattern);
			if (str.matches("(?is).*?value\\s*=\\s*[\"].*")){
				value = extractParameter(str, valuePattern);
			} else {
				value = extractParameter(str, valuePatternApos);
			}
			type = extractParameter(str, typePattern).toLowerCase();
			id = extractParameter(str, idPattern);
		}
	}

	public static class Form {

		public String name = "";
		public String method = "";
		public String action = "";
		public String id = "";
		public List<Input> inputs = new ArrayList<Input>();
		public List<Select> selects = new ArrayList<Select>();

		public String toString() {
			return "\nForm(name=" + name + ",method=" + method + ",action=" + action + ",inputs=" + inputs + ",selects=" + selects + ")";
		}
		
		public Map<String,String> getParams(){
			Map<String,String> params = new LinkedHashMap<String,String>();
			for(Input input: inputs){
				if(!StringUtils.isEmpty(input.name)){
					params.put(input.name, StringEscapeUtils.unescapeHtml(input.value));
				}
			}
			for(Select select: selects){
				params.put(select.name, StringEscapeUtils.unescapeHtml(select.getValue()));
			}
			return params;
			
		}

		public Form(String html){
			
			Matcher startFormMat = startFormPattern.matcher(html);
			Matcher endFormMat = endFormPattern.matcher(html);
			
			if (!startFormMat.find() || !endFormMat.find() || startFormMat.end() >= endFormMat.start()) {
				return;
			}
			
			String header = startFormMat.group(1);
			String contents = html.substring(startFormMat.end(), endFormMat.start());

			// parse form header
			name = extractParameter(header, namePattern);
			action = extractParameter(header, actionPattern);
			method = extractParameter(header, methodPattern);
			id = extractParameter(header, idPattern);

			// parse inputs
			Matcher inputMatcher = inputPattern.matcher(contents);
			while (inputMatcher.find()) {
				String str = inputMatcher.group(1);
				inputs.add(new Input(str));
			}

			// parse selects
			Matcher startMat = startSelectPattern.matcher(contents);
			Matcher endMat = endSelectPattern.matcher(contents);
			while (startMat.find() && endMat.find()) {
				if (startMat.end() < endMat.start()) {
					String selectString = startMat.group(1);
					String selectContents = contents.substring(startMat.end(), endMat.start());
					selects.add(new Select(selectString, selectContents));
				}
			}
		}
		
		public Select getSelect(String name){
			for(Select select: selects){
				if(name.equalsIgnoreCase(select.name)){
					return select;
				}
			}
			return null;
		}
		
		public Collection<String> getValues(String name){
			Collection<String> list = new LinkedList<String>();
			for(Input input: inputs){
				if(name.equals(input.name)){
					list.add(input.value);
				}
			}
			return list;
		}
	}
	
	public SimpleHtmlParser(String html) {
		Matcher startMat = startFormPattern.matcher(html);
		Matcher endMat = endFormPattern.matcher(html);
		while (startMat.find() && endMat.find()) {
			if (startMat.end() < endMat.start()) {
				forms.add(new Form(html.substring(startMat.start(), endMat.end())));
			}
		}
	}
	
	private static String extractParameter(String input, Pattern pattern) {
		Matcher matcher = pattern.matcher(input);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return "";
		}
	}

	public String toString() {
		return "HtmlParser(Forms=" + forms + ")";
	}

	public Form getForm(int idx) {
		if (forms.size() > idx) {
			return forms.get(idx);
		} else {
			return null;
		}
	}

	public Form getForm(String name) {
		for (Form form : forms) {
			if (form.name.equals(name)) {
				return form;
			}
			if (form.action.equals(name)) {
				return form;
			}
			if (form.id.equals(name)) {
				return form;
			}
		}
		return null;
	}

	public static final void main(String args[]) {
		String html = FileUtils.readTextFile("d:/name_search_main.html");		
		SimpleHtmlParser parser = new SimpleHtmlParser(html);
		FileUtils.writeTextFile("d:/name_search_main.txt", parser.toString());
		
		Form form = parser.getForm("nameSearchForm");
		FileUtils.writeTextFile("d:/name_search_main.form.txt", form.toString());
		
		Map<String,String>params = form.getParams();
		FileUtils.writeTextFile("d:/name_search_main.form_filled.txt", params.toString());
	}
	
}
