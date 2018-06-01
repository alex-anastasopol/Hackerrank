package ro.cst.tsearch.utils;

import java.util.GregorianCalendar;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DatabaseData;

public class MemoryAllocation {
	
	//instance holder
	private static class MemoryAllocationHolder {
		private static MemoryAllocation memoryAllocationInstance = new MemoryAllocation();
	}
	
	//instance getter
	public static MemoryAllocation getInstance() {
		return MemoryAllocationHolder.memoryAllocationInstance;
	}
	
	//24 h in 30 minute interval
	private MemoryAllocationData[] statsArray = null;
	
	public MemoryAllocation () {
		statsArray = new MemoryAllocationData[48];
		
		for( int i = 0 ; i < statsArray.length ; i ++ ) {
			statsArray[i] = new MemoryAllocationData();
		}
	}
	
	private int getDataIndex () {
		GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
		int hour = cal.get( GregorianCalendar.HOUR_OF_DAY );
		int minute = cal.get( GregorianCalendar.MINUTE );

		return 2*hour + minute / 30;
	}
	
	public void addStartedSearch() {
		MemoryAllocationData data = statsArray[ getDataIndex() ];
		data.increaseSearchesStarted();
		
		updateMemUsage( getMemFree() );
	}
	
	public void addEndedSearch() {
		MemoryAllocationData data = statsArray[ getDataIndex() ];
		data.increaseSearchesEnded();
	}
	
	public void addTsrsStarted() {
		MemoryAllocationData data = statsArray[ getDataIndex() ];
		data.increaseTsrsStarted();
	}
	
	public void addTsrsEnded() {
		MemoryAllocationData data = statsArray[ getDataIndex() ];
		data.increaseTsrsEnded();
	}
	
	public void updateMemUsage ( long memUsed ) {
		MemoryAllocationData data = statsArray[ getDataIndex() ];
		data.updatePeakMemory( memUsed );
	}
	
	public void addAllocatedSearch () {
		MemoryAllocationData data = statsArray[ getDataIndex() ];
		data.increaseAllocatedSearches();
	}
	
	public MemoryAllocationData[] getMemStatData() {
		return statsArray;
	}
	
	private long getMemFree () {
		String sqlSelect = "SELECT " + DBConstants.FIELD_USAGE_INFO_MEMORY_FREE + " from " + DBConstants.TABLE_USAGE_INFO + 
		" WHERE " + DBConstants.FIELD_USAGE_INFO_ID + "=  (select max(" + 
		DBConstants.FIELD_USAGE_INFO_ID +") FROM " + DBConstants.TABLE_USAGE_INFO + " WHERE " + 
		DBConstants.FIELD_USAGE_INFO_SERVER_NAME + " ='" + URLMaping.INSTANCE_DIR + "')";
	
		DBConnection conn = null;
		DatabaseData data = null;
		try{
			conn  = ConnectionPool.getInstance().requestConnection();
			data = conn.executeSQL(sqlSelect);
			
			Float memFree = (Float) data.getValue(DBConstants.FIELD_USAGE_INFO_MEMORY_FREE, 0); 
			if( memFree != null ) {
				return memFree.longValue();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return 0;
	}
}