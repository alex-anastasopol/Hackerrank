package ro.cst.tsearch.reports.throughputs;

public class ThroughputOpCode {
	public static final String OPCODE 				= "opCode";
	public static final String THROUGHPUT_BEAN 		= "throughputBean";
	public static final String INCOME_BEAN			= "incomeBean";
	public static final String SELECT_OBJECT 		= "selObj";
	public static final String COLOR_OBJECT 		= "colObj";
	public static final String NAME_OBJECT			= "namObj";
	
	public static final String TIME_GENERAL			= "General";
	public static final String TIME_YEARS			= "Year";
	public static final String TIME_MONTHS			= "Month";
	
	public static final String STACKED_TIME_GENERAL	= "StackedGeneral";
	public static final String STACKED_TIME_YEARS	= "StackedYear";
	public static final String STACKED_TIME_MONTHS	= "StackedMonth";
	
	public static final String GROUPS				= "Groups";
	public static final String COMMUNITIES			= "Communities";
	public static final String STATES				= "States";
	public static final String COUNTIES				= "Counties";
	public static final String PRODUCTS				= "Products";
	public static final String ABSTRACTORS			= "Abstractors";
	public static final String AGENTS				= "Agents";
	
	//basic pie chart operations
	public static final int FILTER_PRODUCTS 		= 1;
	public static final int FILTER_STATES 			= 2;
	public static final int FILTER_COUNTIES 		= 3;
	public static final int FILTER_ABSTRACTORS 		= 4;
	public static final int FILTER_AGENTS 			= 5;
	public static final int DRILL_GROUPS 			= 6;
	public static final int DRILL_COMMUNITIES 		= 7;
	public static final int DRILL_STATES 			= 8;
	public static final int FILTER_COMMUNITIES		= 9;
	public static final int FILTER_GROUPS			= 19;
	
	public static final int BACK_TO_GROUPS			= 10;
	public static final int BACK_TO_COMMUNITIES		= 11;
	public static final int BACK_TO_STATES			= 12;
	
	public static final int RESET_FILTERS			= 20;
	public static final int RESET_PRODUCTS			= 21;
	public static final int RESET_STATES			= 22;
	public static final int RESET_COUNTIES			= 23;
	public static final int RESET_ABSTRACTORS		= 24;
	public static final int RESET_AGENTS			= 25;
	public static final int RESET_COMMUNITIES		= 26;
	public static final int RESET_GROUPS			= 27;
	
	public static final int GO_BACK_PRODUCTS		= 30;
	public static final int GO_BACK_STATES			= 31;
	public static final int GO_BACK_ABSTRACTORS		= 32;
	public static final int GO_BACK_AGENTS			= 33;
	public static final int GO_BACK_COUNTIES		= 34;
	public static final int GO_BACK_COMMUNITIES		= 35;
	public static final int GO_BACK_GROUPS			= 36;
	
	public static final int GO_FW_PRODUCTS			= 40;
	public static final int GO_FW_STATES			= 41;
	public static final int GO_FW_ABSTRACTORS		= 42;
	public static final int GO_FW_AGENTS			= 43;
	public static final int GO_FW_COUNTIES			= 44;
	public static final int GO_FW_COMMUNITIES		= 45;
	public static final int GO_FW_GROUPS			= 46;
	
	public static final int APPLY_REFRESH			= 50;
	public static final int LOAD_PARAMETERS			= 51;
	
	public static final int EXPORT_COUNTIES			= 60;
	public static final int EXPORT_STATES			= 61;
	public static final int EXPORT_ABSTRACTORS		= 62;
	public static final int EXPORT_AGENTS			= 63;
	public static final int EXPORT_PRODUCTS			= 64;
	
}
