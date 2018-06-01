package ro.cst.tsearch.templates;

/**
 * 
 * @author Oprina George
 *
 * Apr 10, 2013 <br>
 * 
 *  helper class, it represents a boiler completed statement which has a start and end text around a <documents=...> tag <br>
 *  docIDStatementsMap - holds document statements by docId
 */
public class CodeBookStatementObject {

	private String rawText = null;
	
	public CodeBookStatementObject(String rawText) {
		this.rawText = rawText;
	}
	
	public String getValue(){
		return rawText.replaceAll(
				"(?s)" +TemplateUtils.BOILER_MAP_SEPARATOR + "\\d+_\\d+" + TemplateUtils.BOILER_MAP_SEPARATOR_EQUAL + 
				"(.*?)" + TemplateUtils.BOILER_MAP_SEPARATOR, 
				"$1");
		
	}
	
	public String getValueForDocId(String docId){
		String result = rawText.replaceAll(
				"(?s)" +TemplateUtils.BOILER_MAP_SEPARATOR + docId + TemplateUtils.BOILER_MAP_SEPARATOR_EQUAL + 
				"(.*?)" + TemplateUtils.BOILER_MAP_SEPARATOR, 
				"$1");
		
		
		
		return result.replaceAll(
				"(?s)" +TemplateUtils.BOILER_MAP_SEPARATOR + "\\d+_\\d+" + TemplateUtils.BOILER_MAP_SEPARATOR_EQUAL + 
				".*?" + TemplateUtils.BOILER_MAP_SEPARATOR, 
				"");
	}
	
}
