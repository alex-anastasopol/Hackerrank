package ro.cst.tsearch.tags;

import java.util.HashMap;
import java.util.TreeSet;

import ro.cst.tsearch.database.rowmapper.UserFilterMapper;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.SelectTag;
import ro.cst.tsearch.servlet.user.ManageCountyList;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.utils.RequestParams;

import com.stewart.ats.base.warning.Warning;
import com.stewart.ats.base.warning.WarningManager;

public class WarningSelect extends SelectTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int[] warningSelect = {-1};
	public void setWarningSelect(String s) {
		warningSelect = Util.extractArrayFromString(s);
	}	
	public String getWarningSelect() {
		return Util.getStringFromArray(warningSelect);
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
		if( getRequest().getParameter(RequestParams.SEARCH_PRODUCT_TYPE) == null) {
			try {
				setWarningSelect(UserFilterMapper.getAttributesAsString(
						Integer.parseInt(getRequest().getParameter(UserAttributes.USER_ID)), 
						UserManager.dashboardFilterTypes.get("Warning")));
			} catch (Exception e) {
				loadAttribute(RequestParams.WARNING_SELECT);
			}
		} else {
			if(ManageCountyList.RESET_ALL_FILTERS.equals(getRequest().getParameter("operation")) ||
					ManageCountyList.RESET_WARNINGS.equals(getRequest().getParameter("operation"))) {
				setWarningSelect("-1");
			} else {
				loadAttribute(RequestParams.WARNING_SELECT);	
			}
		}
		
		HashMap<String, Warning> allWarnings = WarningManager.getInstance().getAllWarnings();
		TreeSet<String> keys = new TreeSet<String>(allWarnings.keySet());
		StringBuilder sb = new StringBuilder(allOption(warningSelect));
		for (String key : keys) {
			if(readOnly) {
				if(Util.isValueInArray(allWarnings.get(key).getId(), warningSelect)) {
					sb.append(
							"<option selected value='" + allWarnings.get(key).getId() + "'>" + key + "</option>\n");
				}
			} else {
				sb.append(
						"<option "
							+ (Util.isValueInArray(allWarnings.get(key).getId(), warningSelect) ? "selected" : "")
							+ " value='" + allWarnings.get(key).getId() + "'>" + key + "</option>\n");
			}
		}
		return sb.toString();
	}
	
	protected String allOption(int[] id) throws Exception {
		if(all) {
			if(readOnly) {
				if(Util.isValueInArray(-1, id)) {
					return "<option selected value='-1'>No Warnings</option>" ;
				}
			} else {
				return "<option "+(Util.isValueInArray(-1, id)?"selected":"") +
					" value='-1'>Set No Warnings</option>" ;
			}
		} 
		return "";
	}

}
