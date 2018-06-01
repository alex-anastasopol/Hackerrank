package ro.cst.tsearch.tags;

import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.UserFilterMapper;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.SelectTag;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servlet.user.ManageCountyList;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;

public class SubcategorySelect extends SelectTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String[] subcategorySelect = {"-1"};
	
	public void setSubcategorySelect(String s) {
		subcategorySelect = Util.extractStringArrayFromString(s);
	}	
	public String getSubcategorySelect() {
		return Util.getStringFromStringArray(subcategorySelect);
	}
	
	private String[] categorySelect = {"-1"};
	
	public void setCategorySelect(String s) {
		categorySelect = Util.extractStringArrayFromString(s);
	}	
	public String getCategorySelect() {
		return Util.getStringFromStringArray(categorySelect);
	}
	
	private int[] reportState = {-2};	
	public void setReportState(String s) {
		reportState = Util.extractArrayFromString(s);
	}	
	public String getReportState() {
		return Util.getStringFromArray(reportState);
	}
	private boolean readOnly = false;
	public void setReadOnly(String s) {
		if("true".equalsIgnoreCase(s))
			readOnly = true;
	}
	public String getReadOnly(){
		if(readOnly)
			return "true";
		return "false";
	}
	
	@Override
	protected String createOptions() throws Exception {
		loadAttribute(RequestParams.REPORTS_STATE);
		loadAttribute(RequestParams.CATEGORY_SELECT);
		loadAttribute(RequestParams.SUBCATEGORY_SELECT);
		boolean allSelected = Util.isValueInArray("-1", categorySelect);
		if( getRequest().getParameter(RequestParams.SUBCATEGORY_SELECT) == null || (allSelected && disabled)) {
			try {
				setSubcategorySelect(UserFilterMapper.getAttributesAsString(
						Integer.parseInt(getRequest().getParameter(UserAttributes.USER_ID)), 
						UserManager.dashboardFilterTypes.get("CategAndSubcateg")));
			} catch (Exception e) {
				loadAttribute(RequestParams.SUBCATEGORY_SELECT);
			}
		} else {
			if(ManageCountyList.RESET_ALL_FILTERS.equals(getRequest().getParameter("operation") ) ||
					ManageCountyList.RESET_CATEGORIES_AND_SUBCATEGORIES.equals(getRequest().getParameter("operation"))) {
				setSubcategorySelect("-1");
			} else {
				loadAttribute(RequestParams.SUBCATEGORY_SELECT);	
			}
		}
		
		StringBuffer sb = new StringBuffer(3000);
		State allStates[] = DBManager.getAllStatesForSelect();

		String[] stateAbr = null;
		stateAbr = new String[allStates.length];
		for (int i = 0; i < allStates.length; i++) {
			stateAbr[i] = allStates[i].getStateAbv();		
		}
		
		TreeMap<String, TreeSet<String>> categoriesWithSubcategories = 
			DocumentTypes.getAllCategoriesAndSubcategories(stateAbr);
		Set<String> categories = categoriesWithSubcategories.keySet();
		TreeSet<String> subcategories = null;
		String aux = null;
		String aux2 = null;
		
		for (String category : categories) {
			subcategories = categoriesWithSubcategories.get(category);
			if(allSelected || Util.isValueInArray(category, categorySelect))
			for (String subcategory : subcategories) {
				aux = category +"_" + subcategory;
				aux2 = category + ", " + subcategory;
				if(aux2.length() > 50)
					aux2 = aux2.substring(0,50) + "..";
				if(readOnly) {
					if(Util.isValueInArray(aux, subcategorySelect)) {
						sb.append(
								"<option value='" +
								StringUtils.HTMLEntityEncode(aux) + "' selected >" +
								StringUtils.HTMLEntityEncode((allSelected || categorySelect.length > 1)?aux2:subcategory) +
								"</option>"
						);
					}
				} else {
					sb.append(
							"<option value='" +
							StringUtils.HTMLEntityEncode(aux) + "'" + 
							(Util.isValueInArray(aux, subcategorySelect) ? " selected >" : ">") +
							StringUtils.HTMLEntityEncode((allSelected || categorySelect.length > 1)?aux2:subcategory) +
							"</option>"
					);
				}
			}
		}
		
		if(sb.length() == 0) {
			sb.append(
					"<option value='-1' >" +
					"Nothing to display" +
					"</option>");
		}

		return sb.toString();
	}
	
	protected  String allOption(String[] subcategoryName)	throws Exception {
		if(all) {
			if(readOnly) {
				if(Util.isValueInArray("-1", subcategoryName)) {
					return "<option selected value='-1'>All Subcategories</option>\n" ;
				}
			} else {
				return "<option "+(Util.isValueInArray("-1", subcategoryName)?"selected":"") +
					" value='-1'>All Subcategories</option>\n" ;
			}
		}
		return "";
	}

}
