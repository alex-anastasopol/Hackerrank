package ro.cst.tsearch.tags;


import java.util.Arrays;

import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.SelectTag;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;

public class CategorySelect extends SelectTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String[] categorySelect = {"-1"};
	
	public void setCategorySelect(String s) {
		categorySelect = Util.extractStringArrayFromString(s);
	}	
	public String getCategorySelect() {
		return Util.getStringFromStringArray(categorySelect);
	}

	@Override
	protected String createOptions() throws Exception {
		loadAttribute(RequestParams.CATEGORY_SELECT);
		StringBuffer sb = new StringBuffer(allOption(categorySelect));
		String[] allCategories = DocumentTypes.getAllAvailableCategories();
		Arrays.sort(allCategories);
		for (int i = 0; i < allCategories.length; i++) {
			sb.append(
					"<option "
						+ (Util.isValueInArray(allCategories[i],categorySelect) ? "selected" : "")
						+ " value='" + allCategories[i] + "'>"
						+ StringUtils.HTMLEntityEncode(allCategories[i])
						+ "</option>\n");
		}
		return sb.toString();
	}
	

	protected  String allOption(String[] compName)	throws Exception {
		if(all) {
			return "<option "+(Util.isValueInArray("-1", compName)?"selected":"") +
				" value='-1'>Display All Categories</option>\n" ;
		} else {
			return "";
		}
	}

}
