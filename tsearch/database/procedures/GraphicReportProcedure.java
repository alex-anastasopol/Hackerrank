package ro.cst.tsearch.database.procedures;

import java.math.BigDecimal;
import java.sql.Types;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.reports.throughputs.BarDatasetEntry;
import ro.cst.tsearch.reports.throughputs.LineDatasetEntry;

public class GraphicReportProcedure extends StoredProcedure {
	
	public static final String SP_NAME = "getGraphicReport";
	
	public static final String STATE_DATA = "stateData";
	public static final String GROUP_DATA = "groupData";
	public static final String PRODUCT_DATA = "productData";
	public static final String COMMUNITY_DATA = "commData";
	public static final String COUNTY_DATA = "countyData";
	public static final String ABSTRACTOR_DATA = "abstrData";
	public static final String AGENT_DATA = "agentData";
	public static final String BAR_REPORT_DATA = "barReportData";
	public static final String LINE_REPORT_DATA = "lineReportData";
	
	public static final String COLUMN_START_YEAR = "startYear";
	public static final String COLUMN_START_MONTH = "startMonth";
	public static final String COLUMN_START_DAY = "startDay";
	
	public static final String COLUMN_FINISH_YEAR = "finishYear";
	public static final String COLUMN_FINISH_MONTH = "finishMonth";
	public static final String COLUMN_FINISH_DAY = "finishDay";
	
	private static final String COLUMN_COUNT = "noOf";
	private static final String COLUMN_TIME_ELAPSED = "timeElapsed";
	private static final String COLUMN_GROUP_ID = "groupId";
	private static final String COLUMN_COMMUNITY_ID = "commId";
	private static final String COLUMN_STATE_ID = "stateId";
	private static final String COLUMN_COUNTY_ID = "countyId";
	private static final String COLUMN_PRODUCT_ID = "prodId";
	private static final String COLUMN_AGENT_ID = "agentId";
	private static final String COLUMN_ABSTRACTOR_ID = "abstractorId";
	
	
	public static enum INTERVAL_TYPES {
		MONTH,
		YEAR,
		GENERAL
	}
	
	public static enum CHART_TYPES {
		THROUGHPUT,
		INCOME
	}
		
	public GraphicReportProcedure(JdbcTemplate jdbct) {
		super(jdbct, SP_NAME);
		declareParameter(new SqlParameter("chartType", Types.INTEGER));
		declareParameter(new SqlParameter("countyId", Types.VARCHAR));
		declareParameter(new SqlParameter("abstractorId",Types.VARCHAR));
		declareParameter(new SqlParameter("agentId", Types.VARCHAR));
		declareParameter(new SqlParameter("stateId", Types.VARCHAR));
		declareParameter(new SqlParameter("compName", Types.VARCHAR));
		declareParameter(new SqlParameter("status", Types.VARCHAR));
		declareParameter(new SqlParameter("productId", Types.VARCHAR));
		declareParameter(new SqlParameter("isTSAdmin", Types.INTEGER));
		declareParameter(new SqlParameter("groupId", Types.INTEGER));
		declareParameter(new SqlParameter("commId", Types.VARCHAR));
		declareParameter(new SqlParameter("fromDay", Types.INTEGER));
		declareParameter(new SqlParameter("fromMonth", Types.INTEGER));
		declareParameter(new SqlParameter("fromYear", Types.INTEGER));
		declareParameter(new SqlParameter("toDay", Types.INTEGER));
		declareParameter(new SqlParameter("toMonth", Types.INTEGER));
		declareParameter(new SqlParameter("toYear", Types.INTEGER));
		declareParameter(new SqlParameter("selectProductId", Types.BOOLEAN)); 
		declareParameter(new SqlParameter("selectGroupId", Types.BOOLEAN));
		declareParameter(new SqlParameter("selectCommunityId", Types.BOOLEAN));
		declareParameter(new SqlParameter("selectStateId", Types.BOOLEAN));
		declareParameter(new SqlParameter("selectCountyId", Types.BOOLEAN));
		declareParameter(new SqlParameter("selectAgentId", Types.BOOLEAN));
		declareParameter(new SqlParameter("selectAbstractorId", Types.BOOLEAN));
		declareParameter(new SqlParameter("selectYear", Types.BOOLEAN));
		declareParameter(new SqlParameter("selectMonth", Types.BOOLEAN));
		declareParameter(new SqlParameter("selectDay", Types.BOOLEAN));
		compile();
	}
	
	/**
	 * Creates the map that will hold the parameters sent to the database
	 * @param chartType
	 * @param countyId
	 * @param abstractorId
	 * @param agentId
	 * @param stateId
	 * @param compName
	 * @param status
	 * @param productType
	 * @param isTSAdmin
	 * @param groupId
	 * @param commId
	 * @param fromCalendar
	 * @param toCalendar
	 * @param intervalType
	 * @param selectProductId
	 * @param selectGroupId
	 * @param selectCommunityId
	 * @param selectStateId
	 * @param selectCountyId
	 * @param selectAgentId
	 * @param selectAbstractorId
	 * @return
	 */
	private Map<String, Object> initInParams(CHART_TYPES chartType, String countyId, String abstractorId, String agentId, String stateId, String compName,
			String status, String productType, boolean isTSAdmin, int groupId, String commId,   
			Calendar fromCalendar, Calendar toCalendar, INTERVAL_TYPES intervalType,
			boolean selectProductId, boolean selectGroupId, boolean selectCommunityId, 
			boolean selectStateId, boolean selectCountyId, boolean selectAgentId, 
			boolean selectAbstractorId) {
		
		Map<String, Object> inParams = new HashMap<String, Object>();
		
		if(chartType.equals(CHART_TYPES.INCOME)) {
			inParams.put("chartType", 1);
		} else {
			inParams.put("chartType", 0);	
		}
		
		inParams.put("countyId", countyId);
		inParams.put("abstractorId", abstractorId);
		inParams.put("agentId", agentId);
		inParams.put("stateId", stateId);
		inParams.put("compName", compName);
		inParams.put("status", status);
		inParams.put("productId", productType);
		inParams.put("isTSAdmin", isTSAdmin);
		inParams.put("groupId", groupId);
		inParams.put("commId", commId);
		
		inParams.put("fromDay", fromCalendar.get(Calendar.DAY_OF_MONTH));
		inParams.put("fromMonth", fromCalendar.get(Calendar.MONTH) + 1);
		inParams.put("fromYear", fromCalendar.get(Calendar.YEAR));
		inParams.put("toDay", toCalendar.get(Calendar.DAY_OF_MONTH));
		inParams.put("toMonth", toCalendar.get(Calendar.MONTH) + 1);
		inParams.put("toYear", toCalendar.get(Calendar.YEAR));
		
		inParams.put("selectProductId", selectProductId); 
		inParams.put("selectGroupId", selectGroupId);
		inParams.put("selectCommunityId", selectCommunityId);
		inParams.put("selectStateId", selectStateId);
		inParams.put("selectCountyId", selectCountyId);
		inParams.put("selectAgentId", selectAgentId);
		inParams.put("selectAbstractorId", selectAbstractorId);
		inParams.put("selectYear", false);
		inParams.put("selectMonth", false);
		inParams.put("selectDay", false);
		
		
		
		if(intervalType == INTERVAL_TYPES.GENERAL) {
			inParams.put("selectYear", true);	
		} else if (intervalType == INTERVAL_TYPES.YEAR){
			inParams.put("selectYear", true);
			inParams.put("selectMonth", true);
		} else if (intervalType == INTERVAL_TYPES.MONTH) {
			inParams.put("selectYear", true);
			inParams.put("selectMonth", true);
			inParams.put("selectDay", true);	
		}
		
		return inParams;
	}
	
	private HashMap<String, Object> initResultMap(CHART_TYPES chartType, INTERVAL_TYPES intervalType,
			boolean selectProductId, boolean selectGroupId, boolean selectCommunityId, 
			boolean selectStateId, boolean selectCountyId, boolean selectAgentId, 
			boolean selectAbstractorId) {
		HashMap<String, Object> results = new HashMap<String, Object>();
				
		if(selectProductId) {
			results.put(PRODUCT_DATA, new HashMap<Long, Long>());
		}
		if(selectGroupId) {
			results.put(GROUP_DATA, new HashMap<Long, Long>());
		}
		if(selectAbstractorId) {
			results.put(ABSTRACTOR_DATA, new HashMap<Long, Long>());
		}
		if(selectAgentId) {
			results.put(AGENT_DATA, new HashMap<Long, Long>());
		}
		if(selectCommunityId) {
			results.put(COMMUNITY_DATA, new HashMap<Long, Long>());
		}
		if(selectStateId){
			results.put(STATE_DATA, new HashMap<Long, Long>());
		}
		if(selectCountyId){
			results.put(COUNTY_DATA, new HashMap<Long, Long>());
		}
		
		return results;
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<String, Object> execute(	CHART_TYPES chartType, String countyId, 
			String abstractorId, String agentId, String stateId, String compName,
			String status, String productType, boolean isTSAdmin, int groupId, String commId,   
			Calendar fromCalendar, Calendar toCalendar, INTERVAL_TYPES intervalType,
			boolean selectProductId, boolean selectGroupId, boolean selectCommunityId, 
			boolean selectStateId, boolean selectCountyId, boolean selectAgentId, 
			boolean selectAbstractorId) throws DataAccessException {
		
		long startTime = System.currentTimeMillis();
		long tempTime = 0;
		Map<String, Object> inParams = initInParams(chartType, countyId, 
				abstractorId, agentId, stateId, compName, status, 
				productType, isTSAdmin, groupId, commId, 
				fromCalendar, toCalendar, intervalType, 
				selectProductId, selectGroupId, selectCommunityId, 
				selectStateId, selectCountyId, selectAgentId, selectAbstractorId);
		logger.debug("InitInParams took:"  + (System.currentTimeMillis() - startTime));
		tempTime = System.currentTimeMillis();
		
		Map mapResult = super.execute(inParams);
		logger.debug("super.execute(inParams) took:"  + (System.currentTimeMillis() - tempTime));
		tempTime = System.currentTimeMillis();
		HashMap<String, Object> results = initResultMap(chartType, intervalType, 
				selectProductId, selectGroupId, selectCommunityId, 
				selectStateId, selectCountyId, selectAgentId, selectAbstractorId);
		logger.debug("initResultMap took:"  + (System.currentTimeMillis() - tempTime));
		tempTime = System.currentTimeMillis();
		List<Map<String, Object>> rowList = (ArrayList)mapResult.get("#result-set-1");
		
		
		if(rowList != null) {
			if(chartType.equals(CHART_TYPES.THROUGHPUT)){
				loadThroughputData(rowList, fromCalendar, toCalendar, intervalType, 
						selectProductId, selectGroupId, selectCommunityId, 
						selectStateId, selectCountyId, selectAgentId, selectAbstractorId, results);
			} else {
				loadIncomeData(rowList, fromCalendar, toCalendar, intervalType, 
						selectProductId, selectGroupId, selectCommunityId, 
						selectStateId, selectCountyId, selectAgentId, selectAbstractorId, results);
			}
			
		}
		
		logger.debug("loadData took:"  + (System.currentTimeMillis() - tempTime));
		logger.debug("full execution took :"  + (System.currentTimeMillis() - startTime));
		return results;
	}
	
	private void loadThroughputData(List<Map<String, Object>> sourceRowList, 
			Calendar fromCalendar, Calendar toCalendar, INTERVAL_TYPES intervalType,
			boolean selectProductId, boolean selectGroupId, boolean selectCommunityId, 
			boolean selectStateId, boolean selectCountyId, boolean selectAgentId, 
			boolean selectAbstractorId, HashMap<String, Object> dataDestination) {
		if(sourceRowList != null) {
			HashMap<Long, Long> groupData = null;
			HashMap<Long, Long> communityData = null;
			HashMap<Long, Long> stateData = null;
			HashMap<Long, Long> countyData = null;
			HashMap<Long, Long> productData = null;
			HashMap<Long, Long> abstractorData = null;
			HashMap<Long, Long> agentData = null;
			TreeMap<String, BarDatasetEntry> barData = new TreeMap<String, BarDatasetEntry>();
			dataDestination.put(BAR_REPORT_DATA, barData);
			TreeMap<String, LineDatasetEntry> lineData = new TreeMap<String, LineDatasetEntry>();
			dataDestination.put(LINE_REPORT_DATA, lineData);
			if(selectGroupId) {
				groupData = new HashMap<Long, Long>();
				dataDestination.put(GROUP_DATA, groupData);
			}
			if(selectStateId) {
				stateData = new HashMap<Long, Long>();
				dataDestination.put(STATE_DATA, stateData);
			}
			if(selectCommunityId) {
				communityData = new HashMap<Long, Long>();
				dataDestination.put(COMMUNITY_DATA, communityData);
			}
			if(selectProductId) {
				productData = new HashMap<Long, Long>();
				dataDestination.put(PRODUCT_DATA, productData);
			}
			if(selectCountyId) {
				countyData = new HashMap<Long, Long>();
				dataDestination.put(COUNTY_DATA, countyData);
			}
			if(selectAbstractorId) {
				abstractorData = new HashMap<Long, Long>();
				dataDestination.put(ABSTRACTOR_DATA, abstractorData);
			}
			if(selectAgentId) {
				agentData = new HashMap<Long, Long>();
				dataDestination.put(AGENT_DATA, agentData);
			}
			//---- end declaration zone
			
			for (Map<String, Object> row : sourceRowList) {
				Long count = (Long)row.get(COLUMN_COUNT);
				
				boolean validStartDate = false;
				if(intervalType.equals(INTERVAL_TYPES.GENERAL)) {
					validStartDate = true;
					String startYear = ((Number)row.get(COLUMN_START_YEAR)).toString();
					Number endYear = (Number)row.get(COLUMN_FINISH_YEAR);
					
					BarDatasetEntry barDatasetEntry = barData.get(startYear);
					if(barDatasetEntry == null) {
						barDatasetEntry = new BarDatasetEntry(startYear);
						barDatasetEntry.setKey(startYear);
						barData.put(startYear, barDatasetEntry);
					}
					
					barDatasetEntry.increaseValueFirstColumn(count);
					if (endYear != null) {
						barDatasetEntry = barData.get(endYear.toString());
						if(barDatasetEntry == null) {
							barDatasetEntry = new BarDatasetEntry(endYear.toString());
							barData.put(endYear.toString(), barDatasetEntry);
						}
						barDatasetEntry.increaseValueSecondColumn(count);
						
						LineDatasetEntry lineDatasetEntry = lineData.get(endYear.toString());
						if(lineDatasetEntry == null) {
							lineDatasetEntry = new LineDatasetEntry(endYear.toString());
							lineData.put(endYear.toString(), lineDatasetEntry);
						}
						lineDatasetEntry.updateInfo(count, ((BigDecimal)row.get(COLUMN_TIME_ELAPSED)).longValue());
					}
					
					
				} else if(intervalType.equals(INTERVAL_TYPES.YEAR)) {
					Number endYear = (Number)row.get(COLUMN_FINISH_YEAR);
					
					if(fromCalendar.get(Calendar.YEAR) == (Integer)row.get(COLUMN_START_YEAR)) {
						validStartDate = true;
						
						String startMonth = ((Number)row.get(COLUMN_START_MONTH)).toString();
						Number endMonth = (Number)row.get(COLUMN_FINISH_MONTH);
						
						BarDatasetEntry barDatasetEntry = barData.get(startMonth);
						if(barDatasetEntry == null) {
							barDatasetEntry = new BarDatasetEntry(startMonth);
							barDatasetEntry.setKey(startMonth);
							barData.put(startMonth, barDatasetEntry);
						}
						
						barDatasetEntry.increaseValueFirstColumn(count);
						if (endYear != null && fromCalendar.get(Calendar.YEAR) == endYear.intValue()) {
							barDatasetEntry = barData.get(endMonth.toString());
							if(barDatasetEntry == null) {
								barDatasetEntry = new BarDatasetEntry(endMonth.toString());
								barData.put(endMonth.toString(), barDatasetEntry);
							}
							barDatasetEntry.increaseValueSecondColumn(count);
							
							LineDatasetEntry lineDatasetEntry = lineData.get(endMonth.toString());
							if(lineDatasetEntry == null) {
								lineDatasetEntry = new LineDatasetEntry(endMonth.toString());
								lineData.put(endMonth.toString(), lineDatasetEntry);
							}
							lineDatasetEntry.updateInfo(count, ((BigDecimal)row.get(COLUMN_TIME_ELAPSED)).longValue());
						}
						
					} else if(fromCalendar.get(Calendar.YEAR) == endYear.intValue()) {
						Number endMonth = (Number)row.get(COLUMN_FINISH_MONTH);
						BarDatasetEntry barDatasetEntry = barData.get(endMonth.toString());
						if(barDatasetEntry == null) {
							barDatasetEntry = new BarDatasetEntry(endMonth.toString());
							barData.put(endMonth.toString(), barDatasetEntry);
						}
						barDatasetEntry.increaseValueSecondColumn(count);
						
						LineDatasetEntry lineDatasetEntry = lineData.get(endMonth.toString());
						if(lineDatasetEntry == null) {
							lineDatasetEntry = new LineDatasetEntry(endMonth.toString());
							lineData.put(endMonth.toString(), lineDatasetEntry);
						}
						lineDatasetEntry.updateInfo(count, ((BigDecimal)row.get(COLUMN_TIME_ELAPSED)).longValue());
					}
				} else if(intervalType.equals(INTERVAL_TYPES.MONTH)) {
					Number endYear = (Number)row.get(COLUMN_FINISH_YEAR);
					int fromMonth = fromCalendar.get(Calendar.MONTH) + 1;
					if(fromCalendar.get(Calendar.YEAR) == (Integer)row.get(COLUMN_START_YEAR) 
							&& fromMonth == (Integer)row.get(COLUMN_START_MONTH)) {
						
						validStartDate = true;
												
						String startDay = ((Number)row.get(COLUMN_START_DAY)).toString();
						Number endDay = (Number)row.get(COLUMN_FINISH_DAY);
						
						BarDatasetEntry barDatasetEntry = barData.get(startDay);
						if(barDatasetEntry == null) {
							barDatasetEntry = new BarDatasetEntry(startDay);
							barDatasetEntry.setKey(startDay);
							barData.put(startDay, barDatasetEntry);
						}
						barDatasetEntry.increaseValueFirstColumn(count);
						if (endYear != null && 
								fromCalendar.get(Calendar.YEAR) == endYear.intValue() &&
								fromMonth == (Integer)row.get(COLUMN_FINISH_MONTH)) {
							barDatasetEntry = barData.get(endDay.toString());
							if(barDatasetEntry == null) {
								barDatasetEntry = new BarDatasetEntry(endDay.toString());
								barData.put(endDay.toString(), barDatasetEntry);
							}
							barDatasetEntry.increaseValueSecondColumn(count);
							
							LineDatasetEntry lineDatasetEntry = lineData.get(endDay.toString());
							if(lineDatasetEntry == null) {
								lineDatasetEntry = new LineDatasetEntry(endDay.toString());
								lineData.put(endDay.toString(), lineDatasetEntry);
							}
							lineDatasetEntry.updateInfo(count, ((BigDecimal)row.get(COLUMN_TIME_ELAPSED)).longValue());
						}
						
					} else if(endYear != null && 
							fromCalendar.get(Calendar.YEAR) == endYear.intValue() &&
							fromMonth == (Integer)row.get(COLUMN_FINISH_MONTH)) {
						Number endDay = (Number)row.get(COLUMN_FINISH_DAY);
						BarDatasetEntry barDatasetEntry = barData.get(endDay.toString());
						if(barDatasetEntry == null) {
							barDatasetEntry = new BarDatasetEntry(endDay.toString());
							barData.put(endDay.toString(), barDatasetEntry);
						}
						barDatasetEntry.increaseValueSecondColumn(count);
						
						LineDatasetEntry lineDatasetEntry = lineData.get(endDay.toString());
						if(lineDatasetEntry == null) {
							lineDatasetEntry = new LineDatasetEntry(endDay.toString());
							lineData.put(endDay.toString(), lineDatasetEntry);
						}
						lineDatasetEntry.updateInfo(count, ((BigDecimal)row.get(COLUMN_TIME_ELAPSED)).longValue());
					}
				}
				
				if(validStartDate){
					
					
					if(groupData != null) {
						Number key = (Number)row.get(COLUMN_GROUP_ID);
						Long oldValue = groupData.get(key.longValue());
						if(oldValue == null) {
							groupData.put(key.longValue(), count);
						} else {
							groupData.put(key.longValue(), oldValue + count);
						}
					}						
					
					if(stateData != null){
						Number key = (Number)row.get(COLUMN_STATE_ID);
						Long oldValue = stateData.get(key.longValue());
						if(oldValue == null) {
							stateData.put(key.longValue(), count);
						} else {
							stateData.put(key.longValue(), oldValue + count);
						}
					}
					if(productData != null){
						Number key = (Number)row.get(COLUMN_PRODUCT_ID);
						Long oldValue = productData.get(key.longValue());
						if(oldValue == null) {
							productData.put(key.longValue(), count);
						} else {
							productData.put(key.longValue(), oldValue + count);
						}
					}
					if(communityData != null){
						Number key = (Number)row.get(COLUMN_COMMUNITY_ID);
						Long oldValue = communityData.get(key.longValue());
						if(oldValue == null) {
							communityData.put(key.longValue(), count);
						} else {
							communityData.put(key.longValue(), oldValue + count);
						}
					}
					if(countyData != null){
						Number key = (Number)row.get(COLUMN_COUNTY_ID);
						Long oldValue = countyData.get(key.longValue());
						if(oldValue == null) {
							countyData.put(key.longValue(), count);
						} else {
							countyData.put(key.longValue(), oldValue + count);
						}
					}
					if(abstractorData != null){
						Number key = (Number)row.get(COLUMN_ABSTRACTOR_ID);
						Long oldValue = abstractorData.get(key.longValue());
						if(oldValue == null) {
							abstractorData.put(key.longValue(), count);
						} else {
							abstractorData.put(key.longValue(), oldValue + count);
						}
					}
					if(agentData != null){
						Number key = (Number)row.get(COLUMN_AGENT_ID);
						Long oldValue = agentData.get(key.longValue());
						if(oldValue == null) {
							agentData.put(key.longValue(), count);
						} else {
							agentData.put(key.longValue(), oldValue + count);
						}
					}
					
				}
			}
			try {
				int lineMin = 10000; 
				int lineMax = 0;
				
				for (String lineKey : barData.keySet()) {
					int lineKeyInt = Integer.parseInt(lineKey);
					if(lineMin > lineKeyInt) {
						lineMin = lineKeyInt;
					}
					if(lineMax < lineKeyInt) {
						lineMax = lineKeyInt;
					}
				}
				if(intervalType.equals(INTERVAL_TYPES.MONTH)) {
					lineMin = 1;
				}
				for (int i = lineMin; i <= lineMax; i++) {
					String key = String.valueOf(i);
					if(barData.get(String.valueOf(i)) == null) {
						if(key.length() == 1) {
							key = "0" + key;
						}
						barData.put(key, new BarDatasetEntry(String.valueOf(i)));
					} else {
						if(key.length() == 1) {
							barData.put("0" + key, barData.get(key));
							barData.remove(key);
						}
					}
				}
				for (int i = lineMin; i <= lineMax; i++) {
					if(lineData.get(String.valueOf(i)) == null) {
						lineData.put(String.valueOf(i), new LineDatasetEntry(String.valueOf(i)));
					}
				}
				if(intervalType.equals(INTERVAL_TYPES.YEAR)) {
					DateFormatSymbols dfs = new DateFormatSymbols();
					TreeMap<String, BarDatasetEntry> barDataSorted = new TreeMap<String, BarDatasetEntry>();
					for (BarDatasetEntry databaseEntry : barData.values()) {
						String oldKey = databaseEntry.getKey();
						if(oldKey.length() == 1) {
							oldKey = "0" + oldKey;
						}
						databaseEntry.setKey(dfs.getMonths()[Integer.parseInt(databaseEntry.getKey())-1]);
						barDataSorted.put(oldKey, databaseEntry);
					}
					dataDestination.put(BAR_REPORT_DATA, barDataSorted);
					TreeMap<String, LineDatasetEntry> lineDataSorted = new TreeMap<String, LineDatasetEntry>();
					for (LineDatasetEntry databaseEntry : lineData.values()) {
						String oldKey = databaseEntry.getKey();
						if(oldKey.length() == 1) {
							oldKey = "0" + oldKey;
						}
						databaseEntry.setKey(dfs.getMonths()[Integer.parseInt(databaseEntry.getKey())-1]);
						lineDataSorted.put(oldKey, databaseEntry);
					}
					dataDestination.put(LINE_REPORT_DATA, lineDataSorted);
				} else if(intervalType.equals(INTERVAL_TYPES.YEAR)) {
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void loadIncomeData(List<Map<String, Object>> sourceRowList, 
			Calendar fromCalendar, Calendar toCalendar, INTERVAL_TYPES intervalType,
			boolean selectProductId, boolean selectGroupId, boolean selectCommunityId, 
			boolean selectStateId, boolean selectCountyId, boolean selectAgentId, 
			boolean selectAbstractorId, HashMap<String, Object> dataDestination) {
		if(sourceRowList != null) {
			for (Map<String, Object> row : sourceRowList) {
				
			}
		}
	}
	
	public static void main(String[] args) {
		GraphicReportProcedure procedure = new GraphicReportProcedure(DBManager.getJdbcTemplate());
		
		Calendar fromCalendar = Calendar.getInstance();
		Calendar toCalendar = Calendar.getInstance();
		fromCalendar.set(Calendar.YEAR,2000);
		
		long startTime = System.currentTimeMillis();
		
		HashMap<String, Object> result = procedure.execute(CHART_TYPES.THROUGHPUT,",-1,",",-1,",",-1,",",-1,",",-1,",",-1,",",-1,",true,-1,",-1,",
				fromCalendar,toCalendar, INTERVAL_TYPES.GENERAL,
				true,true,false,true,false, false,false);
		
		System.out.println("Execution took " + (System.currentTimeMillis() - startTime) + " milliseconds");
		
		System.out.println(result);
		
	}

}
