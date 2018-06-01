package ro.cst.tsearch.reports.tags;

import org.apache.log4j.Logger;

import ro.cst.tsearch.database.DBUser;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.GenericTag;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;

public class GenericPaginateAssignTag extends GenericTag {

	private static final Logger logger = Logger.getLogger(GenericPaginateAssignTag.class);
	
	//county methods
	protected int[] reportCounty = {-2};	
	public void setReportCounty(String s) {
		reportCounty = Util.extractArrayFromString(s);
	}	
	public String getReportCounty() {
		return Util.getStringFromArray(reportCounty);
	}
	//state methods
	protected int[] reportState = {-2};	
	public void setReportState(String s) {
		reportState = Util.extractArrayFromString(s);
	}	
	public String getReportState() {
		return Util.getStringFromArray(reportState);
	}
	
	protected int[] reportUser = {-2};	
	public void setReportUser(String s) {
		reportUser = Util.extractArrayFromString(s);
	}	
	public String getReportUser() {
		return Util.getStringFromArray(reportUser);
	}
	
	
	//state methods
	protected int allowedFilter = -1;	
	public void setAllowedFilter(String s) {
		allowedFilter = Integer.parseInt(s);
	}	
	public String getAllowedFilter() {
		return allowedFilter+"";
	}
	
	//state methods
	protected String[] c2arates = {"-1"};	
	public void setC2arates(String s) {
		c2arates = Util.extractStringArrayFromString(s);
	}	
	public String getC2arates() {
		return Util.getStringFromStringArray(c2arates);
	}
	
	//state methods
	protected String[] a2crates = {"-1"};	
	public void setA2crates(String s) {
		a2crates = Util.extractStringArrayFromString(s);
	}	
	public String getA2crates() {
		return Util.getStringFromStringArray(a2crates);
	}
	
	//order by and order type methods
	protected String sortBy = "";
	protected String sortOrder = "";

	public String getSortBy() {
		return sortBy;
	}
	public String getSortOrder() {
		return sortOrder;
	}
	public void setSortBy(String string) {
		sortBy = string;
	}
	public void setSortOrder(String string) {
		sortOrder = string;
	}
	
	//page number
	protected int reportPage = 1;
	public void setReportPage(String reportPage) {
		this.reportPage = Integer.parseInt(reportPage);
	}
	public String getReportPage() {
		return String.valueOf(reportPage);
	}

	//rows per page
	protected int rowsPerPage = -1;
	public void setRowsPerPage(String rowsPerPage) {
		this.rowsPerPage = Integer.parseInt(rowsPerPage);
	}
	public String getRowsPerPage() {
		return String.valueOf(rowsPerPage);
	}
	
	protected String jsp = "";
	 public String getJsp() {
		return jsp;
	}
	public void setJsp(String jsp) {
		this.jsp = jsp;
	}
	
	public int doStartTag() {
	        try {
	            //logger.debug("Debug: SelectTag#doStartTag start " + selectName );
	            initialize();

	            //the number of rows that the dashboard would have without pagination
	            int count = 0 ;
	            
	    		loadAttribute(RequestParams.REPORTS_STATE);
	    		loadAttribute(RequestParams.REPORTS_COUNTY);
	    		loadAttribute(RequestParams.USER_COUNTY_SORT_BY);
	    		loadAttribute(RequestParams.USER_COUNTY_SORT_ORDER);
	    		loadAttribute(RequestParams.REPORTS_PAGE);
	    		loadAttribute(RequestParams.REPORTS_ROWS_PER_PAGE);
	    		
	    		loadAttribute("reportUser");
	    		loadAttribute("allowedFilter");
	    		loadAttribute("c2arates");
	    		loadAttribute("a2crates");
	    		
	    		int commId = -1;
	    		try {
	    			String commIdStr=(String)ses.getAttribute("commId");
	    			commId = Integer.parseInt(commIdStr);
	    		}catch(Exception ignored) {}
	            
	    		
	    		StringBuffer sb = new StringBuffer(3000);
	    		 
	    		rowsPerPage = (rowsPerPage>0)?rowsPerPage:RequestParamsValues.ASSIGN_DEFAULT_ROWS_PER_PAGE;
	    		int offset = (reportPage - 1)*rowsPerPage;
	    		
	    		//reading total row count from the DB
	    		if("orders".equalsIgnoreCase(jsp)) {
	    			count = DBUser.getAssignedCountiesForStateFilterCount(reportState, reportCounty, reportUser, commId, sortBy, sortOrder,allowedFilter, offset, rowsPerPage);
	    		}else {
	    			count = DBUser.getAllCountiesForStateRatesFilterCount(reportState, reportCounty, 
	    				c2arates, a2crates, reportUser, commId, sortBy, sortOrder, allowedFilter, offset,rowsPerPage, 0);
	    		}

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
