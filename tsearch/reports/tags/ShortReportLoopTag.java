package ro.cst.tsearch.reports.tags;

import java.util.Calendar;

import org.apache.log4j.Logger;

import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBReports;
import ro.cst.tsearch.database.procedures.TableReportProcedure;
import ro.cst.tsearch.reports.data.ReportLineData;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;

public class ShortReportLoopTag extends GenericReportsLoopTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(ShortReportLoopTag.class);
	
	private int invoice = 0;
	public String getInvoice() {
	    return String.valueOf(invoice);
	}
	public void setInvoice(String invoice) {
	    try {
	        this.invoice = Integer.parseInt(invoice);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	protected Object[] createObjectList() throws Exception {
		long startTime = System.currentTimeMillis();
		ReportLineData[] shortReportLineData = new ReportLineData[3];
		if (logger.isDebugEnabled())
			logger.debug("createObjectList Start ...");

		//getting current user and community
		User currentUser = (User) ses.getAttribute(SessionParams.CURRENT_USER);
		UserAttributes ua = currentUser.getUserAttributes();
		
		int commId = -1;
		String commIdStr=(String)ses.getAttribute("commId");
		commId = Integer.parseInt(commIdStr);
		//setting current time
		Calendar c = Calendar.getInstance();
		yearReport = c.get(Calendar.YEAR);

		//loading attributes from request
		loadAttribute(RequestParams.REPORTS_STATE);
		if (reportState.length == 1 && reportState[0] != -1)
			loadAttribute(RequestParams.REPORTS_COUNTY);
		if (!UserUtils.isAgent(ua)){	
			loadAttribute(RequestParams.REPORTS_AGENT);
			loadAttribute(RequestParams.REPORTS_ABSTRACTOR);
		}else{
			reportAbstractor[0] = ua.getID().intValue();
			reportAgent[0] = -1;
		}
		loadAttribute(RequestParams.REPORTS_COMPANY_AGENT);
		loadAttribute(RequestParams.REPORTS_YEAR);
		loadAttribute(RequestParams.REPORTS_MONTH);
		
		
		Calendar fromCalendar = Calendar.getInstance();
		Calendar toCalendar = Calendar.getInstance();
		fromCalendar.set(yearReport, 0, 1);
		if(monthReport - 1 == 0) {
			fromCalendar.add(Calendar.MONTH, -1);	//the start of the interval (must show prev month also)
		}
		toCalendar.set(yearReport, 11, 31);		//the end of this year
		toCalendar.set(Calendar.HOUR_OF_DAY, 23);
		toCalendar.set(Calendar.MINUTE, 59);
		toCalendar.set(Calendar.SECOND, 59);
		
		
		shortReportLineData = DBReports.getTableReportData(reportCounty, reportAbstractor, reportAgent, 
				reportState, reportCompanyAgent, 
				fromCalendar, toCalendar, 
				commId, ua, TableReportProcedure.INTERVAL_TYPES.MONTH_SHORT_REPORT, monthReport - 1);
		
		if (logger.isDebugEnabled())
			logger.debug("Total time for ShortReportLoopTag:" + (System.currentTimeMillis()-startTime));
		return shortReportLineData;
	}

}
