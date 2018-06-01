package ro.cst.tsearch.templates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParseDocumentsTagResponse {
	private String							parsedItem;
	private boolean							parseError;
	private String							sortBy;
	private int								monthsBack					= -1;
	private String							disableBoilerPlatesText;
	private List<List<String>>				contentsDocTypesAndSubTypes	= new ArrayList<List<String>>();
	private Map<String, String>				contents					= new Hashtable<String, String>();
	private Set<String>						codeBookCodes				= new LinkedHashSet<String>();
	private Map<String, Collection<String>>	types						= new Hashtable<String, Collection<String>>();

	public String getParsedItem() {
		return parsedItem;
	}

	public void setParsedItem(String parsedItem) {
		this.parsedItem = parsedItem;
	}

	public boolean isParseError() {
		return parseError;
	}

	public void setParseError(boolean parseError) {
		this.parseError = parseError;
	}

	public String getSortBy() {
		return sortBy;
	}

	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;
	}

	public int getMonthsBack() {
		return monthsBack;
	}

	public void setMonthsBack(int monthsBack) {
		this.monthsBack = monthsBack;
	}

	public String getDisableBoilerPlatesText() {
		return disableBoilerPlatesText;
	}

	public void setDisableBoilerPlatesText(String disableBoilerPlatesText) {
		this.disableBoilerPlatesText = disableBoilerPlatesText;
	}

	public List<List<String>> getContentsDocTypesAndSubTypes() {
		return contentsDocTypesAndSubTypes;
	}

	public Map<String, String> getContents() {
		return contents;
	}

	public Set<String> getCodeBookCodes() {
		return codeBookCodes;
	}

	public Map<String, Collection<String>> getTypes() {
		return types;
	}
}
