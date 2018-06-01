package ro.cst.tsearch.reports.tags;

import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

import ro.cst.tsearch.SearchFlags.CREATION_SOURCE_TYPES;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBReports;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.reports.data.DayReportLineData;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;

public class DayReportLoopTag extends GenericReportsLoopTag{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(DayReportLoopTag.class);
	
	protected int invoice = 0;
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

		DayReportLineData[] ReportData = new DayReportLineData[0];
		if (logger.isDebugEnabled())
			logger.debug("createObjectList Start ...");

		//getting current user and community
		User currentUser = (User) ses.getAttribute(SessionParams.CURRENT_USER);
		UserAttributes ua = currentUser.getUserAttributes();
		int commId = -1;
		String commIdStr=(String)ses.getAttribute("commId");
		commId = Integer.parseInt(commIdStr);
		
		//setting current time
		Date now = new Date(System.currentTimeMillis());
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		yearReport = c.get(Calendar.YEAR);
		monthReport = c.get(Calendar.MONTH) + 1;
		dayReport = c.get(Calendar.DAY_OF_MONTH);

		//loading attributes from request
		loadAttribute(RequestParams.REPORTS_STATUS);
		loadAttribute(RequestParams.REPORTS_STATE);
		//if (!(reportState.length == 1 && reportState[0] < 0))
		loadAttribute(RequestParams.REPORTS_COUNTY);
		if (!UserUtils.isAgent(ua)){	
			loadAttribute(RequestParams.REPORTS_AGENT);
			loadAttribute(RequestParams.REPORTS_ABSTRACTOR);
		}else{
			reportAbstractor[0] = ua.getID().intValue();
			reportAgent[0] = -1;
		}
		loadAttribute(RequestParams.REPORTS_COMPANY_AGENT);
		loadAttribute(RequestParams.REPORTS_DAY);
		loadAttribute(RequestParams.REPORTS_MONTH);
		loadAttribute(RequestParams.REPORTS_YEAR);
		loadAttribute(RequestParams.REPORTS_ORDER_BY);
		loadAttribute(RequestParams.REPORTS_ORDER_TYPE);
		loadAttribute(RequestParams.REPORTS_PAGE);
		loadAttribute(RequestParams.REPORTS_ROWS_PER_PAGE);

		if( Util.isValueInArray( 14, reportStatus ) )
		{
		//    reportStatus = Util.NandTnotO( reportStatus );
		}
		if( Util.isValueInArray( 15, reportStatus ) )
		{
		//    reportStatus = Util.DandKnotI( reportStatus );
		}
		if (logger.isDebugEnabled()){
			logger.debug("Date: " + yearReport + "-" + monthReport  + "-" + dayReport);		
			logger.debug("Agent/Abstractor: " + reportAgent.toString() + "-" + reportAbstractor.toString());		
			logger.debug("County/State: " + reportCounty.toString() + "-" + reportState.toString());		
			logger.debug("Status filter: " + reportStatus);
		}
		loadAttribute(RequestParams.REPORTS_DATE_TYPE);


		rowsPerPage = ((rowsPerPage>0)?rowsPerPage:(ua.getDASHBOARD_ROWS_PER_PAGE()).intValue());
		int offset = (reportPage - 1)*rowsPerPage;
		
		//reading data from the DB
		/*ReportData = DBReports.getDayReportData(reportCounty, reportAbstractor, 
				reportAgent, reportState, reportCompanyAgent, 
				dayReport, monthReport, yearReport, orderBy, orderType, commId, 
				reportStatus, invoice,offset,rowsPerPage,
				((User)ses.getAttribute(SessionParams.CURRENT_USER)).getUserAttributes());*/
		
		ReportData = DBReports.getIntervalReportData(
				reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, 
				dayReport, monthReport, yearReport, 
				dayReport, monthReport, yearReport, 
				orderBy, orderType, commId, reportStatus, invoice,offset,rowsPerPage,ua, reportDateType);

		/*
		if(invoice == 1) {
			for (DayReportLineData dayReportLineData : ReportData) {
				CREATION_SOURCE_TYPES creationSourceTypes = dayReportLineData.getSearchFlags().getCreationSourceType();
	    		if(CREATION_SOURCE_TYPES.CLONED.equals(creationSourceTypes)) {
	    			dayReportLineData.setProductName(dayReportLineData.getProductName() + "(C)");
	    		} else if(CREATION_SOURCE_TYPES.REOPENED.equals(creationSourceTypes)) {
	    			dayReportLineData.setProductName(dayReportLineData.getProductName() + "(R)");
	    		} 
			}
    		
    	}
    	*/
		
		//filterResults is now done in the database in the getFilterInterval function		
		// ReportData = CommAdminNotifier.filterResults( ReportData, reportStatus, new BigDecimal(String.valueOf(commId)) );

		
		if (logger.isDebugEnabled())
			logger.debug("End.");
		return ReportData;
	}

}
