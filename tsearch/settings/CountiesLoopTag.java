package ro.cst.tsearch.settings;

import java.util.Vector;

import org.apache.log4j.Category;

import ro.cst.tsearch.data.CountyState;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.generic.tag.LoopTag;
import ro.cst.tsearch.utils.RequestParams;

public class CountiesLoopTag extends LoopTag{

	private static final long serialVersionUID = -633363568856885689L;
	protected static final Category logger= Category.getInstance(CountiesLoopTag.class.getName());
	//state methods
	private long reportState = -1;	// default state All States 
	public void setReportState(String stateReport) {
		this.reportState = Long.parseLong(stateReport);
	}
	public String getReportState() {
		return String.valueOf(reportState);
	}
	
	protected Object[] createObjectList() throws Exception {

		Vector<CountyState> counties = new Vector<CountyState>();
		if (logger.isDebugEnabled())
			logger.debug("Debug CountiesLoopTag#createObjectList Start ...");

		loadAttribute( RequestParams.REPORTS_STATE );
		
		if (logger.isDebugEnabled())
			logger.debug("Debug:CountiesLoopTag param: " + reportState);		
		counties = DBManager.getCounties(reportState);
		if (logger.isDebugEnabled())
			logger.debug("Debug:CountiesLoopTag End.");
		return counties.toArray();
	}

}
