package ro.cst.tsearch.reports.tags;

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import org.apache.log4j.Logger;

import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBReports;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;


public class PaginateIntervalReportsTag extends GenericPaginateReportsTag {
    
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(PaginateIntervalReportsTag.class);
	
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
	
    public int doStartTag() {
        try {
            //logger.debug("Debug: SelectTag#doStartTag start " + selectName );
            initialize();

            //the number of rows that the dashboard would have without pagination
            int count = 0 ;
            
    		//getting current user and community
    		User currentUser = (User) ses.getAttribute(SessionParams.CURRENT_USER);
    		int commId = -1;
    		
    		UserAttributes ua = currentUser.getUserAttributes();
    		
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
    		loadAttribute(RequestParams.REPORTS_DATE_TYPE);
    		loadAttribute(RequestParams.REPORTS_STATUS);
    		loadAttribute(RequestParams.REPORTS_STATE);
    		if (!(reportState.length == 1 && reportState[0] < 0))
    			loadAttribute(RequestParams.REPORTS_COUNTY);
    		
    		if(reportCounty.length ==1 && reportCounty[0]<0 && 
    				(ua.isAgent() || ua.isAbstractor())){
    			//no county selected load counties from allowed counties
    			Vector<County> allowed = ua.getAllowedCountyList();
    			if(allowed.size()>0){
    				reportCounty = new int[allowed.size()];
    				int i = 0;
    				for (County county : allowed) {
						reportCounty[i++] = county.getCountyId().intValue();
					}
    			}
    		}
 
    		loadAttribute(RequestParams.REPORTS_AGENT);
    		loadAttribute(RequestParams.REPORTS_ABSTRACTOR);
    		loadAttribute(RequestParams.REPORTS_COMPANY_AGENT);
    		loadAttribute(RequestParams.REPORTS_FROM_DAY);
    		loadAttribute(RequestParams.REPORTS_FROM_MONTH);
    		loadAttribute(RequestParams.REPORTS_FROM_YEAR);
    		loadAttribute(RequestParams.REPORTS_TO_DAY);
    		loadAttribute(RequestParams.REPORTS_TO_MONTH);
    		loadAttribute(RequestParams.REPORTS_TO_YEAR);
    		loadAttribute(RequestParams.REPORTS_ORDER_BY);
    		loadAttribute(RequestParams.REPORTS_ORDER_TYPE);
    		loadAttribute(RequestParams.REPORTS_PAGE);
    		loadAttribute(RequestParams.REPORTS_ROWS_PER_PAGE);
            
    		StringBuffer sb = new StringBuffer(3000);
    		 
    		rowsPerPage = ((rowsPerPage>0)?rowsPerPage:(ua.getDASHBOARD_ROWS_PER_PAGE()).intValue());
    		int offset = (reportPage - 1)*rowsPerPage;
    		
    		//reading total row count from the DB
    		count = DBReports.getIntervalReportDataCount(reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, fromDay, fromMonth, fromYear, toDay, toMonth, toYear, orderBy, orderType, commId, reportStatus, invoice,offset,rowsPerPage,ua, reportDateType);

    		sb.append("<input type='hidden' name='"+RequestParams.REPORTS_PAGE + "' value='" + reportPage +"'>");

    		if(count > rowsPerPage) {
	    		sb.append("<br>");
	    		
	    		if(reportPage > 1) {
	    		sb.append("<input class='submitLinkBlue' type='button' value='Previous'  name='next' "+ 
						  "onClick=\"javascript:	" +
						  "document.forms[0].reset(); " +
						  "document.forms[0]." +RequestParams.REPORTS_PAGE +".value = '" +
						  Integer.valueOf(reportPage - 1) +
						  "'; " +
						  "document.forms[0].submit();" +
						  "\">&nbsp;");
	    		}
	    		
	    		for(int i=0;i<Math.ceil((double)((double)count/(double)rowsPerPage));i++) {
	    			if(i==reportPage-1) {sb.append("<font style='FONT-SIZE: 11px'>"+ reportPage + "</font>"+ "&nbsp;");continue;}
	        		sb.append("<input class='submitLinkBlue' type='button' value='"+
	        				 new Integer(i + 1) +
	        				  "'  name='next' "+ 
							  "onClick=\"javascript:	" +
							  "document.forms[0].reset(); " +
							  "document.forms[0]." +RequestParams.REPORTS_PAGE +".value = '" +
							  new Integer(i + 1) +
							  "';" +
							  "document.forms[0].submit();" +
							  "\">&nbsp;");
	    		}
	    		
	    		if(reportPage < Math.ceil((double)((double)count/(double)rowsPerPage))) {
	    		sb.append("<input class='submitLinkBlue' type='button' value='Next'  name='next' "+ 
	    						  "onClick=\"javascript:	" +
	    						  "document.forms[0].reset(); " +
	    						  "document.forms[0]." +RequestParams.REPORTS_PAGE +".value = '" +
	    						  new Integer(reportPage + 1) +
	    						  "'; " +
	    						  "document.forms[0].submit();" +
	    						  "\">&nbsp;");
	    		}
    		}
    		
    		sb.append("<font style='FONT-SIZE: 11px'>"+
					 "<br>Total results: "+
					 count +
					 "</font>"
					 );
    		
    		sb.append("<br><br>" +
    				"<font style='FONT-SIZE: 11px'>"+
    				"Rows per page: "+
    				"</font>"+
    				"<INPUT type='text' name='"+RequestParams.REPORTS_ROWS_PER_PAGE+"'" +
    				" value='" + rowsPerPage + "'"+
    				" size='2'>"
					 );
    		
    		sb.append("<input class='submitLinkBlue' type='button' value='Apply'  name='ApplyRowsPerPage' "+ 
					  "onClick=\"javascript:	" +
					  "document.forms[0]." +RequestParams.REPORTS_PAGE +".value = '" +
					  "1"+
					  "'; " +
					  "document.forms[0].submit();" +
					  "\">&nbsp;");
    		
            pageContext.getOut().print(sb.toString());
			//logger.debug("Debug: SelectTag#doStartTag done " + selectName);
            return(SKIP_BODY);
        }
        catch (Exception e) {
            e.printStackTrace();
			logger.error(this.getClass().toString()+"#doStartTag Exception in Tag " + this.getClass().toString()); 
        }
               
        return(SKIP_BODY);
    }
}
