package ro.cst.tsearch.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.CountyCommunityMapper;
import ro.cst.tsearch.utils.DBConstants;

public class CountyCommunityManager {
	/**
	 * county_id -> commmunity_id -> CountyCommunityMapper
	 */
	private Map<Integer, HashMap<Integer, CountyCommunityMapper>> dataByCounty = null;
	/**
	 * commmunity_id -> county_id -> CountyCommunityMapper
	 */
	private Map<Integer, HashMap<Integer, CountyCommunityMapper>> dataByCommunity = null;
	
	private CountyCommunityManager() {
		dataByCounty = new HashMap<Integer, HashMap<Integer,CountyCommunityMapper>>();
		dataByCommunity = new HashMap<Integer, HashMap<Integer,CountyCommunityMapper>>();
		reloadCounties();
	}

	private static class SingletonHolder {
		private static CountyCommunityManager instance = new CountyCommunityManager();
	}
	
	public static CountyCommunityManager getInstance(){
		return SingletonHolder.instance;
	}
	
	private static final String SQL_GET_ALL_DATA = "SELECT * FROM " + 
		DBConstants.TABLE_COUNTY_COMMUNITY;
	
	public synchronized void reloadCounties() {
		List<CountyCommunityMapper> countyCommunityMappers = DBManager.getSimpleTemplate().query(
				SQL_GET_ALL_DATA, new CountyCommunityMapper());
		for (CountyCommunityMapper countyCommunityMapper : countyCommunityMappers) {
			
			//---- load dataByCommunity
			HashMap<Integer, CountyCommunityMapper> innerDataByCommunity = 
				dataByCommunity.get(countyCommunityMapper.getCommunityId());
			if(innerDataByCommunity == null) {
				innerDataByCommunity = new HashMap<Integer, CountyCommunityMapper>();
				dataByCommunity.put(
						countyCommunityMapper.getCommunityId(), 
						innerDataByCommunity);
			}
			innerDataByCommunity.put(countyCommunityMapper.getCountyId(), countyCommunityMapper);
			
			//---- load dataByCounty
			HashMap<Integer, CountyCommunityMapper> innerDataByCounty = 
				dataByCounty.get(countyCommunityMapper.getCountyId());
			if(innerDataByCounty == null) {
				innerDataByCounty = new HashMap<Integer, CountyCommunityMapper>();
				dataByCounty.put(
						countyCommunityMapper.getCountyId(), 
						innerDataByCounty);
			}
			innerDataByCounty.put(countyCommunityMapper.getCommunityId(), countyCommunityMapper);
		}
	}
	
	public CountyCommunityMapper getCountyCommunityMapper(int countyId, int commId) {
		return dataByCounty.get(countyId).get(commId);
	}
}
