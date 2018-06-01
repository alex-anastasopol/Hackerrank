package ro.cst.tsearch.reports.tags;

import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.LoopTag;
import ro.cst.tsearch.utils.RequestParamsValues;

public abstract class GenericReportsLoopTag extends LoopTag {

	//date type
	protected int reportDateType = RequestParamsValues.REPORTS_DATE_TYPE_COMBINED; //default to lastest date
	public void setReportsDateType(String reportDateType){
		try{
			this.reportDateType = Integer.parseInt(reportDateType);
		} catch(NumberFormatException e){
			this.reportDateType = RequestParamsValues.REPORTS_DATE_TYPE_COMBINED;
		}
	}
	
	public int getReportsDateType(){
		return reportDateType;
	}
	//day methods
	protected int dayReport = -1;
	public void setDayReport(String dayReport) {
		this.dayReport = Integer.parseInt(dayReport);
	}
	public String getDayReport() {
		return String.valueOf(dayReport);
	}
	//month methods
	protected int monthReport = -1;
	public void setMonthReport(String monthReport) {
		this.monthReport = Integer.parseInt(monthReport);
	}
	public String getMonthReport() {
		return String.valueOf(monthReport);
	}
	//year methods
	protected int yearReport = -1;
	public void setYearReport(String yearReport) {
		this.yearReport = Integer.parseInt(yearReport);
	}
	public String getYearReport() {
		return String.valueOf(yearReport);
	}
	//county methods
	protected int[] reportCounty = {-1};	
	public void setReportCounty(String s) {
		reportCounty = Util.extractArrayFromString(s);
	}	
	public String getReportCounty() {
		return Util.getStringFromArray(reportCounty);
	}
	//state methods
	protected int[] reportState = {-1};	
	public void setReportState(String s) {
		reportState = Util.extractArrayFromString(s);
	}	
	public String getReportState() {
		return Util.getStringFromArray(reportState);
	}
	//abstractor methods
	protected int[] reportAbstractor = {-1};
	public void setReportAbstractor(String s) {
		reportAbstractor = Util.extractArrayFromString(s);
	}
	public String getReportAbstractor() {
		return Util.getStringFromArray(reportAbstractor);
	}
	//agent methods
	protected int[] reportAgent = {-1};
	public void setReportAgent(String s) {
		reportAgent = Util.extractArrayFromString(s);
	}
	public String getReportAgent() {
		return Util.getStringFromArray(reportAgent);
	}
	protected String[] reportCompanyAgent = {"-1"};
	public void setReportCompanyAgent(String s){
		reportCompanyAgent = Util.extractStringArrayFromString(s);
	}
	public String getReportCompanyAgent(){
		return Util.getStringFromStringArray(reportCompanyAgent);
	}
	//status methods
	protected int[] reportStatus = {-1};
	public void setReportStatus(String s) {
		reportStatus = Util.extractArrayFromString(s);
	}
	public String getReportStatus() {
		return String.valueOf(reportStatus);
	}
	//order by and order type methods
	protected String orderBy = "";
	protected String orderType = "";

	public String getOrderBy() {
		return orderBy;
	}
	public String getOrderType() {
		return orderType;
	}
	public void setOrderBy(String string) {
		orderBy = string;
	}
	public void setOrderType(String string) {
		orderType = string;
	}

	//from day methods
	protected int fromDay = -1;
	public void setFromDay(String fromDay) {
		this.fromDay = Integer.parseInt(fromDay);
	}
	public String getFromDay() {
		return String.valueOf(fromDay);
	}
	//from month methods
	protected int fromMonth = -1;
	public void setFromMonth(String fromMonth) {
		this.fromMonth = Integer.parseInt(fromMonth);
	}
	public String getFromMonth() {
		return String.valueOf(fromMonth);
	}
	//from year methods
	protected int fromYear = -1;
	public void setFromYear(String fromYear) {
		this.fromYear = Integer.parseInt(fromYear);
	}
	public String getFromYear() {
		return String.valueOf(fromYear);
	}
	//to day methods
	protected int toDay = -1;
	public void setToDay(String dayReport) {
		this.toDay = Integer.parseInt(dayReport);
	}
	public String getToDay() {
		return String.valueOf(toDay);
	}
	//to month methods
	protected int toMonth = -1;
	public void setToMonth(String monthReport) {
		this.toMonth = Integer.parseInt(monthReport);
	}
	public String getToMonth() {
		return String.valueOf(toMonth);
	}
	//to year methods
	protected int toYear = -1;
	public void setToYear(String yearReport) {
		this.toYear = Integer.parseInt(yearReport);
	}
	public String getToYear() {
		return String.valueOf(toYear);
	}
	
	// searchString methods
	protected String TSRsearchString = "";
	public void setTSRsearchString(String s) {
		if(getReportsSearchField().equalsIgnoreCase("Legal"))
			TSRsearchString = s;
		else
			TSRsearchString = Util.getFirstStringFromList(s);
	}
	public String getTSRsearchString() {
		return TSRsearchString;
	}

	protected String searchType = "";
	public void setSearchType(String s) {
		searchType = s;
	}
	public String getSearchType() {
		return searchType;
	}
	
	protected String reportsSearchAll = "";
	public void setReportsSearchAll(String s) {
		reportsSearchAll = s;
	}
	public String getReportsSearchAll() {
		return reportsSearchAll;
	}
	
	protected String reportsSearchField="";
		public void setReportsSearchField(String s) {
		reportsSearchField = s;
	}
	public String getReportsSearchField() {
		return reportsSearchField;
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
	
	
}
