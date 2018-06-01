package ro.cst.tsearch.tags;

import org.apache.log4j.Logger;

import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.SelectTag;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;

public class StatesSelect extends SelectTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(StatesSelect.class);

	private int[] reportState = {-1};	
	public void setReportState(String s) {
		reportState = Util.extractArrayFromString(s);
	}	
	public String getReportState() {
		return Util.getStringFromArray(reportState);
	}
	
	private boolean showAllOptions = false;
	public void setShowAllOptions(String s) {
		showAllOptions = "true".equalsIgnoreCase(s);
	}
	public String getShowAllOptions(){
		return String.valueOf(showAllOptions);
	}

	/**
	 * @see ro.cst.tsearch.generic.tag.SelectTag#createOptions()
	 */
	protected String createOptions() throws Exception {

		State allStates[] = DBManager.getAllStatesForSelect();
		User currentUser = (User) ses.getAttribute(SessionParams.CURRENT_USER);
		UserAttributes ua = currentUser.getUserAttributes();

		loadAttribute(RequestParams.REPORTS_STATE);
		logger.debug("stateId=" + Util.getStringFromArray(reportState));

		StringBuffer sb = new StringBuffer(3000);		
		sb.append(noOption(reportState));
		sb.append(allOption(reportState));
		for (int i = 0; i < allStates.length; i++) {
			if (showAllOptions || ua.isAllowedState(allStates[i].getStateId())){
				sb.append(
						"<option "
						+ (Util.isValueInArray(allStates[i].getStateId().intValue(), reportState) ? "selected" : "")
						+ " value='"
						+ allStates[i].getStateId().intValue()
						+ "'>"
						+ allStates[i].getStateAbv()
						+ "</option>");
			}
		}
		return sb.toString();
	}

	protected  String allOption(int[] id)	throws Exception {

		if(all) {
			return "<option "+(Util.isValueInArray(-1, id)?"selected":"")+" value='-1'>All States</option>" ;
		} else {
			return "";
		}
	}
	
	protected  String noOption(int[] id)	throws Exception {

		if(none) {
			return "<option "+(Util.isValueInArray(-2, id)?"selected":"")+" value='-2'>No States</option>" ;
		} else {
			return "";
		}
	}

}
