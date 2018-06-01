package ro.cst.tsearch.tags;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ro.cst.tsearch.data.County;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.data.sort.StateCountyComparator;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.SelectTag;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.utils.RequestParams;

public class CountiesSelect extends SelectTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static final Logger logger = Logger.getLogger(CountiesSelect.class);

	private int[] reportCounty = {-2};	
	public void setReportCounty(String s) {
		reportCounty = Util.extractArrayFromString(s);
	}	
	public String getReportCounty() {
		return Util.getStringFromArray(reportCounty);
	}

	private int[] reportState = {-1};	
	public void setReportState(String s) {
		reportState = Util.extractArrayFromString(s);
	}	
	public String getReportState() {
		return Util.getStringFromArray(reportState);
	}

	/**
	 * @see ro.cst.tsearch.generic.tag.SelectTag#createOptions()
	 */
	protected String createOptions() throws Exception {
   		loadAttribute(RequestParams.REPORTS_STATE);
		loadAttribute(RequestParams.REPORTS_COUNTY);
   		
		StateCountyManager stateCountyManager = StateCountyManager.getInstance();
		List<County> counties = null;
		if(Util.isValueInArray(-1, reportState)) {
			State allStates[] = DBManager.getAllStatesForSelect();
			int[] allStateIds = new int[allStates.length];
			for (int i = 0; i < allStates.length; i++) {
				allStateIds[i] = allStates[i].getStateId().intValue();
			}
			counties = stateCountyManager.getCountiesForStateIds(allStateIds);
		} else {
			counties = stateCountyManager.getCountiesForStateIds(reportState);
		}
		
		Set<Long> selectedCounties = new HashSet<Long>();
		for (Integer integer : reportCounty) {
			selectedCounties.add(integer.longValue());
		}
		
		Collections.sort(counties, new StateCountyComparator());
		StringBuffer sb = new StringBuffer();
		sb.append(noOption(reportCounty));
		sb.append(allOption(reportCounty));
		boolean singleStateSelected = reportState.length == 1 && reportState[0] > 0;
		
		for (County county : counties) {
			sb.append("<option ")
				.append(selectedCounties.contains(county.getId()) ? "selected" : "")
				.append(" value='")
				.append(county.getId()).append("'>");
			if(!singleStateSelected) {
				sb.append(county.getStateAbv()).append(", ");
			}
			sb.append(county.getName()).append("</option>");
		}
		
   		return sb.toString();
	}

	protected  String allOption(int[] id)	throws Exception {
		if(all) {
			return "<option "+(Util.isValueInArray(-1, id)?"selected":"")+" value='-1'>All Counties</option>" ;
		} else {
			return "";
		}
	}
	
	protected  String noOption(int[] id)	throws Exception {
		if(none) {
			return "<option "+(Util.isValueInArray(-2, id)?"selected":"")+" value='-2'>No Counties</option>" ;
		} else {
			return "";
		}
	}
	
}
