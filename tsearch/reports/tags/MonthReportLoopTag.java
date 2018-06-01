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

public class MonthReportLoopTag extends GenericReportsLoopTag{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(MonthReportLoopTag.class);
	
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

		//getting current user and community
		User currentUser = (User) ses.getAttribute(SessionParams.CURRENT_USER);
		UserAttributes ua = currentUser.getUserAttributes();
		int commId = -1;
		String commIdStr=(String)ses.getAttribute("commId");
		commId = Integer.parseInt(commIdStr);

		//initialize graph data
		Double graphData[] = new Double[31];
		for (int j = 0; j<31; j++)
			graphData[j] = new Double(0);

		
		
		
		//yearReport = c.get(Calendar.YEAR);
		//monthReport = c.get(Calendar.MONTH) + 1;

		ReportLineData[] reportLineData = new ReportLineData[0];
		if (logger.isDebugEnabled())
			logger.debug("createObjectList Start ...");

		//loading attributes from request
		loadAttribute(RequestParams.REPORTS_STATE);
		if (!(reportState.length == 1 && reportState[0] < 0))
			loadAttribute(RequestParams.REPORTS_COUNTY);

		if (!UserUtils.isAgent(ua)){	
			loadAttribute(RequestParams.REPORTS_AGENT);
			loadAttribute(RequestParams.REPORTS_ABSTRACTOR);
		}else{
			reportAbstractor[0] = ua.getID().intValue();
			reportAgent[0] = -1;
		}
		loadAttribute(RequestParams.REPORTS_MONTH);
		loadAttribute(RequestParams.REPORTS_YEAR);
		loadAttribute(RequestParams.REPORTS_COMPANY_AGENT);
		
		//setting current time
		Calendar fromCalendar = Calendar.getInstance();
		Calendar toCalendar = Calendar.getInstance();
		fromCalendar.set(yearReport, monthReport - 1, 1);
		toCalendar.set(yearReport, monthReport - 1, 1);
		toCalendar.set(Calendar.DAY_OF_MONTH, toCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		toCalendar.set(Calendar.HOUR_OF_DAY, 23);
		toCalendar.set(Calendar.MINUTE, 59);
		toCalendar.set(Calendar.SECOND, 59);

		//reading data from the DB
		reportLineData = DBReports.getTableReportData(reportCounty, reportAbstractor, reportAgent, 
				reportState, reportCompanyAgent, 
				fromCalendar, toCalendar, 
				commId, ua, TableReportProcedure.INTERVAL_TYPES.MONTH, null);

		//setting data for graph
		for (int i = 0; i < reportLineData.length - 1; i++)
			 if (reportLineData[i]!=null) graphData[Integer.parseInt(reportLineData[i].getIntervalName()) - 1] = new Double(reportLineData[i].getIncomeSearches());
		ses.setAttribute(SessionParams.GRAPH_DATA, graphData);
		
		return reportLineData;
	}

}
