package ro.cst.tsearch.servers.parentsite;

import java.util.HashMap;
import java.util.Map;

public class ModuleCommentWrapper {
	Map<Integer, Map<Integer, String>> comments;
	Map<Integer, Map<Integer, String>> comboValues;
	Map<Integer, Map<Integer, String>> htmlString;
	
	public static enum TYPE{
        COMMENT(0),
        COMBO_VALUE(1),
        HTML_STRING(2);
        
        private final int value;

        public int getValue() { return value; }
        
        private TYPE(final int newValue) {
            value = newValue;
        }
    }
		
	
	public ModuleCommentWrapper() {
		comments = new HashMap<Integer, Map<Integer,String>>();
		comboValues = new HashMap<Integer, Map<Integer,String>>();
		htmlString = new HashMap<Integer, Map<Integer,String>>();
	}
		
	public String getValueForModuleAndFunction(int moduleIndex, int functionIndex, int type) {
		
		Map<Integer, String> commForModule = null;
		switch (type) {
			case 0:
				commForModule = comments.get(moduleIndex);
				break;
			case 1:
				commForModule = comboValues.get(moduleIndex);
				break;
			case 2:
				commForModule = htmlString.get(moduleIndex);
				break;
			default:
				break;
		}
		
		if (commForModule == null) {
			return "";
		}
		String result = commForModule.get(functionIndex);
		if (result == null) {
			return "";
		}
		return result;
	}
	
	public String setValue(int moduleIndex, int functionIndex, String value) {
		Map<Integer, String> commForModule = comments.get(moduleIndex);
		if(commForModule == null) {
			commForModule = new HashMap<Integer, String>();
			comments.put(moduleIndex, commForModule);
		}
		return commForModule.put(functionIndex, value);
	}
	
	public String setComboValue(int moduleIndex, int functionIndex, String value) {
		Map<Integer, String> comboForModule = comboValues.get(moduleIndex);
		if (comboForModule == null) {
			comboForModule = new HashMap<Integer, String>();
			comboValues.put(moduleIndex, comboForModule);
		}
		return comboForModule.put(functionIndex, value);
	}
	
	public String setHtmlStringValue(int moduleIndex, int functionIndex, String value) {
		Map<Integer, String> htmlStringForModule = htmlString.get(moduleIndex);
		if (htmlStringForModule == null) {
			htmlStringForModule = new HashMap<Integer, String>();
			htmlString.put(moduleIndex, htmlStringForModule);
		}
		return htmlStringForModule.put(functionIndex, value);
	}
}
