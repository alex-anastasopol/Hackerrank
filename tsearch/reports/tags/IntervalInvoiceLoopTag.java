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
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;

public class IntervalInvoiceLoopTag extends GenericReportsLoopTag{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(IntervalInvoiceLoopTag.class);
	
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

		DayReportLineData[] ReportData = new DayReportLineData[0];
		if (logger.isDebugEnabled())
			logger.debug("Debug IntervalInvoiceLoopTag#createObjectList Start ...");

		//getting current user and community
		//User currentUser = (User) ses.getAttribute(SessionParams.CURRENT_USER);
		int commId = -1;
		String commIdStr=(String)ses.getAttribute("commId");
		commId = Integer.parseInt(commIdStr);
		//setting current time
		Date now = new Date(System.currentTimeMillis());
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		fromYear = c.get(Calendar.YEAR);
		fromMonth = c.get(Calendar.MONTH) + 1;
		fromDay = c.get(Calendar.DAY_OF_MONTH);
		toYear = c.get(Calendar.YEAR);
		toMonth = c.get(Calendar.MONTH) + 1;
		toDay = c.get(Calendar.DAY_OF_MONTH);

		//loading attributes from request
		loadAttribute(RequestParams.REPORTS_STATUS);
		loadAttribute(RequestParams.REPORTS_STATE);
		if (!(reportState.length == 1 && reportState[0] < 0))
			loadAttribute(RequestParams.REPORTS_COUNTY);
		loadAttribute(RequestParams.REPORTS_AGENT);
		loadAttribute(RequestParams.REPORTS_ABSTRACTOR);
		loadAttribute(RequestParams.REPORTS_COMPANY_AGENT);
		loadAttribute(RequestParams.INVOICE_FROM_DAY);
		loadAttribute(RequestParams.INVOICE_FROM_MONTH);
		loadAttribute(RequestParams.INVOICE_FROM_YEAR);
		loadAttribute(RequestParams.INVOICE_TO_DAY);
		loadAttribute(RequestParams.INVOICE_TO_MONTH);
		loadAttribute(RequestParams.INVOICE_TO_YEAR);
		loadAttribute(RequestParams.REPORTS_ORDER_BY);
		loadAttribute(RequestParams.REPORTS_ORDER_TYPE);
		loadAttribute(RequestParams.REPORTS_PAGE);
		loadAttribute(RequestParams.REPORTS_ROWS_PER_PAGE);
		
		if( Util.isValueInArray( 14, reportStatus ) )
		{
		  //  reportStatus = Util.NandTnotO( reportStatus );
		}
		if( Util.isValueInArray( 15, reportStatus ) )
		{
		  //  reportStatus = Util.DandKnotI( reportStatus );
		}
		
		if (logger.isDebugEnabled()){
			logger.debug("Date: " + fromDay + "-" + fromMonth + "-" + fromYear);		
			logger.debug("Date: " + toDay + "-" + toMonth + "-" + toYear);		
			logger.debug("Agent/Abstractor: \"" + Util.getStringFromArray(reportAgent) + "\"/\"" + Util.getStringFromArray(reportAbstractor) + "\"");		
			logger.debug("County/State: \"" + Util.getStringFromArray(reportCounty) + "\"/\"" + Util.getStringFromArray(reportState) + "\"");		
		}
		User currentUser = (User) ses.getAttribute(SessionParams.CURRENT_USER);
		UserAttributes ua = currentUser.getUserAttributes();
		
		rowsPerPage = ((rowsPerPage>0)?rowsPerPage:(ua.getDASHBOARD_ROWS_PER_PAGE()).intValue());
		int offset = (reportPage - 1)*rowsPerPage;
		loadAttribute(RequestParams.REPORTS_DATE_TYPE);
		
		//reading data from the DB
		ReportData = DBReports.getIntervalReportData(reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, fromDay, fromMonth, fromYear, toDay, toMonth, toYear, orderBy, orderType, commId, reportStatus, invoice,offset,rowsPerPage,ua, reportDateType);
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
		//filterResults is now done in the database in the getFilterInterval function
		//ReportData = CommAdminNotifier.filterResults( ReportData, reportStatus, new BigDecimal(String.valueOf(commId)) );
		
		if (logger.isDebugEnabled())
			logger.debug("Debug:IntervalInvoiceLoopTag End.");
		return ReportData;
	}

}
