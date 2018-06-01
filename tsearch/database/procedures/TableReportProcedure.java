package ro.cst.tsearch.database.procedures;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;

import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.reports.data.ReportLineData;
import ro.cst.tsearch.utils.DBConstants;

public class TableReportProcedure extends StoredProcedure {
	public static final String SP_NAME = "getTableReport";
	
	private static final String FIELD_INTERVAL_ENTRY = "intervalEntry";
	private static final String FIELD_COUNTY_ID = "countyId";
	private static final String FIELD_AGENT_ID = "agentId";
	private static final String FIELD_ABSTRACTOR_ID = "abstractorId";
	private static final String FIELD_PAID = "paid";
	private static final String FIELD_START_DATE = "startDate";
	private static final String FIELD_STARTER = "starter";
	private static final String FIELD_SHOW_DATE = "showDate";
	
	public static final String FIELD_PROD_ID = "prodId";
	public static final String FIELD_RATE_ID = "rateField";
	public static final String FIELD_DISCOUNT = "discountRatio";
	public static final String FIELD_INVOICED_FIELD = "invoicedField";
	public static final String FIELD_IS_CLOSED = DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED;
	public static final String FIELD_SOURCE_CREATION_TYPE = DBConstants.FIELD_SEARCH_FLAGS_SOURCE_CREATION_TYPE;
	public static final String FIELD_WAS_OPENED = DBConstants.FIELD_SEARCH_FLAGS_WAS_OPENED;
	
	public static enum INTERVAL_TYPES {
		MONTH,
		YEAR,
		GENERAL,
		MONTH_SHORT_REPORT,
		YEAR_SHORT_REPORT
	}
	
	public TableReportProcedure(JdbcTemplate jdbct) {
		super(jdbct, SP_NAME);
		declareParameter(new SqlParameter("countyId", Types.VARCHAR));
		declareParameter(new SqlParameter("abstractorId",Types.VARCHAR));
		declareParameter(new SqlParameter("agentId", Types.VARCHAR));
		declareParameter(new SqlParameter("stateId", Types.VARCHAR));
		declareParameter(new SqlParameter("compName", Types.VARCHAR));
		declareParameter(new SqlParameter("fromDay", Types.INTEGER));
		declareParameter(new SqlParameter("fromMonth", Types.INTEGER));
		declareParameter(new SqlParameter("fromYear", Types.INTEGER));
		declareParameter(new SqlParameter("toDay", Types.INTEGER));
		declareParameter(new SqlParameter("toMonth", Types.INTEGER));
		declareParameter(new SqlParameter("toYear", Types.INTEGER));
		declareParameter(new SqlParameter("commId", Types.INTEGER));
		declareParameter(new SqlParameter("isTSAdmin", Types.INTEGER));
		declareParameter(new SqlParameter("intervalType", Types.INTEGER));
		compile();
	}
	
	@SuppressWarnings("unchecked")
	public ReportLineData[] execute(String countyId, String abstractorId, String agentId, String stateId, String compName,
			Calendar fromCalendar,
			Calendar toCalendar,
			int commId, boolean isTSAdmin, INTERVAL_TYPES intervalType, Integer currentShortReportInterval) throws DataAccessException {
		Map<String, Object> inParams = new HashMap<String, Object>();
		inParams.put("countyId", countyId);
		inParams.put("abstractorId", abstractorId);
		inParams.put("agentId", agentId);
		inParams.put("stateId", stateId);
		inParams.put("compName", compName);
		inParams.put("fromDay", fromCalendar.get(Calendar.DAY_OF_MONTH));
		inParams.put("fromMonth", fromCalendar.get(Calendar.MONTH) + 1);
		inParams.put("fromYear", fromCalendar.get(Calendar.YEAR));
		inParams.put("toDay", toCalendar.get(Calendar.DAY_OF_MONTH));
		inParams.put("toMonth", toCalendar.get(Calendar.MONTH) + 1);
		inParams.put("toYear", toCalendar.get(Calendar.YEAR));
		inParams.put("commId", commId);
		inParams.put("isTSAdmin", isTSAdmin?1:0);
		int intervalTypeInt = 1;
		if(intervalType == INTERVAL_TYPES.YEAR) {
			intervalTypeInt = 2;
		} else if (intervalType == INTERVAL_TYPES.GENERAL){
			intervalTypeInt = 3;
		} else if (intervalType == INTERVAL_TYPES.MONTH_SHORT_REPORT) {
			intervalTypeInt = 2;
		}
		inParams.put("intervalType", intervalTypeInt);
		Map mapResult = super.execute(inParams);
		
		if(mapResult.containsKey("#result-set-1")) {
			List<Map<String, Object>> rowList = (List)mapResult.get("#result-set-1");
			TreeMap<Integer, ReportLineData> results = new TreeMap<Integer, ReportLineData>();
			for (Map<String, Object> row : rowList) {
				int entryType = ((Long)row.get(FIELD_INTERVAL_ENTRY)).intValue();
				if (intervalType == INTERVAL_TYPES.MONTH_SHORT_REPORT &&
						entryType == 12 &&
						fromCalendar.get(Calendar.YEAR) != toCalendar.get(Calendar.YEAR) ) {
					Date showDate = (Date)row.get(FIELD_SHOW_DATE);
					Calendar showCalendar = Calendar.getInstance();
					showCalendar.setTime(showDate);
					if(showCalendar.get(Calendar.YEAR) == fromCalendar.get(Calendar.YEAR)) {
						entryType -= 12;	//extract one year
					}
				}
				ReportLineData reportLine = results.get(entryType);
				if(reportLine == null) {
					reportLine = new ReportLineData();
					results.put(entryType, reportLine);
					reportLine.setIntervalName(entryType + "");
				}
				
				
				int productType = ((Long)row.get(FIELD_PROD_ID)).intValue();
				boolean tsrCreated = ((Long)row.get(DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED)) > 0;
				boolean isStarted = (Integer)row.get(FIELD_STARTER) > 0;
				int status = (Integer)row.get(DBConstants.FIELD_SEARCH_STATUS);
				boolean addRow = true;
				if(!tsrCreated && status != ro.cst.tsearch.Search.SEARCH_STATUS_K) {
					productType = Products.INDEX_PRODUCT;
				} else {
					//since this search is finished we must also update ordered counter for that day
					//e.g.: start on 1st and finish on 2nd
					Date startDate = (Date)row.get(FIELD_START_DATE);
					Calendar searchStartCalendar = Calendar.getInstance();
					searchStartCalendar.setTime(startDate);
					if(startDate.after(fromCalendar.getTime())) {
						if (intervalType == INTERVAL_TYPES.MONTH_SHORT_REPORT && 
								searchStartCalendar.get(Calendar.YEAR) != toCalendar.get(Calendar.YEAR) && 
								searchStartCalendar.get(Calendar.MONTH) != fromCalendar.get(Calendar.MONTH)) {
							//skip this part
						} else {
							int orderExtraType = searchStartCalendar.get(Calendar.DAY_OF_MONTH);
							if(intervalType == INTERVAL_TYPES.YEAR || intervalType == INTERVAL_TYPES.MONTH_SHORT_REPORT) {
								orderExtraType = searchStartCalendar.get(Calendar.MONTH) + 1;
								if (orderExtraType == 12 &&
										fromCalendar.get(Calendar.YEAR) != toCalendar.get(Calendar.YEAR) ) {
									Date showDate = (Date)row.get(FIELD_SHOW_DATE);
									Calendar showCalendar = Calendar.getInstance();
									showCalendar.setTime(showDate);
									if(showCalendar.get(Calendar.YEAR) == fromCalendar.get(Calendar.YEAR)) {
										orderExtraType -= 12;	//extract one year
									}
								}
								
							} else if(intervalType == INTERVAL_TYPES.GENERAL) {
								orderExtraType = searchStartCalendar.get(Calendar.YEAR);
							}
							ReportLineData orderReportLine = results.get(orderExtraType);
							if(orderReportLine == null) {
								orderReportLine = new ReportLineData();
								results.put(orderExtraType, orderReportLine);
								orderReportLine.setIntervalName(orderExtraType + "");
								
							}
							orderReportLine.setAllNoOfSearches(orderReportLine.getAllNoOfSearches() + 1);
							orderReportLine.addCountyId(((BigInteger)row.get(FIELD_COUNTY_ID)).longValue());
							orderReportLine.addAgentId((Long)row.get(FIELD_AGENT_ID));
							
							Object abstractorObject = row.get(FIELD_ABSTRACTOR_ID);
							if(abstractorObject instanceof BigInteger) {
								orderReportLine.addAbstractorId(((BigInteger)abstractorObject).longValue());	
							} else if(abstractorObject instanceof BigDecimal) {
								orderReportLine.addAbstractorId(((BigDecimal)abstractorObject).longValue());
							}
							
							
						}
					}
					
					Date finishDate = (Date)row.get("finishDate");
					
					if(finishDate==null) {
						try {
							System.err.println("Error: Search " +  (Long)row.get("id") + " has invalid tsr_date !!! " );
						}catch(Exception ignored) {
							
						}

					}else if(finishDate.after(toCalendar.getTime())) {
							addRow = false;
					}
				}
				
				if(addRow) {
					reportLine.addNumberOfSearchesFor(productType, 1);
					if(!isStarted) {
						double fee = DBManager.getSearchFee(row, isTSAdmin, false);
						reportLine.addPriceOfSearchesFor(productType, fee, ((Long)row.get(FIELD_PAID)) > 0);
					}
					
					reportLine.addCountyId(((BigInteger)row.get(FIELD_COUNTY_ID)).longValue());
					reportLine.addAgentId((Long)row.get(FIELD_AGENT_ID));
					Object abstractorObject = row.get(FIELD_ABSTRACTOR_ID);
					if(abstractorObject instanceof BigInteger) {
						reportLine.addAbstractorId(((BigInteger)abstractorObject).longValue());	
					} else if(abstractorObject instanceof BigDecimal) {
						reportLine.addAbstractorId(((BigDecimal)abstractorObject).longValue());
					}
				}
				
			}
			
			if(results.size() == 0) {
				return new ReportLineData[0];
			} else {
				if(intervalType == INTERVAL_TYPES.MONTH || 
						intervalType == INTERVAL_TYPES.YEAR || 
						intervalType == INTERVAL_TYPES.GENERAL) {
					ReportLineData[] result = new ReportLineData[results.size() + 1];
					int i = 0;
					
					ReportLineData totalLine = new ReportLineData();
					ReportLineData currentLine = null;
					totalLine.setIntervalName("Total");
					for (Integer key : results.keySet()) {
						currentLine = results.get(key);
						result[i++] = currentLine;
						totalLine.addLine(currentLine);
					}
					result[i] = totalLine;
					return result;
				
				} else if(intervalType == INTERVAL_TYPES.MONTH_SHORT_REPORT) {
					if(currentShortReportInterval == null) {
						currentShortReportInterval = 0;
					}
					ReportLineData[] result = new ReportLineData[] {
							new ReportLineData(), new ReportLineData(), new ReportLineData()};
					ReportLineData currentLine = null;
					ReportLineData totalLine = result[2];
					totalLine.setIntervalName("Crt YTD");
					for (Integer key : results.keySet()) {
						currentLine = results.get(key);
						if(key <= 0) {
							result[1] = currentLine;
							result[1].setIntervalName("Prev Month");
						} else {
							if(currentShortReportInterval + 1 == key) {
								result[0] = currentLine;
								result[0].setIntervalName("Crt Month");
							} else if(currentShortReportInterval == key) {
								result[1] = currentLine;
								result[1].setIntervalName("Prev Month");
							}
							totalLine.addLine(currentLine);
							
						}
					}
					
					return result;
				}
			}
		} 
		return new ReportLineData[0];
		
	}
}
