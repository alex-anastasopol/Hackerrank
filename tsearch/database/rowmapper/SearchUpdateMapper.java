package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.utils.DBConstants;

public class SearchUpdateMapper implements ParameterizedRowMapper<SearchUpdateMapper> , Comparable<SearchUpdateMapper>{

	private long searchId;
	private int searchType;
	private Date tsrCreated;
	
	@Override
	public SearchUpdateMapper mapRow(ResultSet rs, int rowNum)
			throws SQLException {
		SearchUpdateMapper sum = new SearchUpdateMapper();
		sum.setSearchId(rs.getLong(DBConstants.FIELD_SEARCH_ID));
		sum.setSearchType(rs.getInt(DBConstants.FIELD_SEARCH_TYPE));
		sum.setTsrCreated(rs.getDate(DBConstants.FIELD_SEARCH_TSR_DATE));
		return sum;
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	public int getSearchType() {
		return searchType;
	}

	public void setSearchType(int searchType) {
		this.searchType = searchType;
	}
	
	public Date getTsrCreated() {
		return tsrCreated;
	}

	public void setTsrCreated(Date tsrCreated) {
		this.tsrCreated = tsrCreated;
	}

	public boolean isUpdate(){
		if(getSearchType() == SearchAttributes.SEARCH_PROD_UPDATE)
			return true;
		return false;
	}

	@Override
	public int compareTo(SearchUpdateMapper o) {
		int ret = - tsrCreated.compareTo(o.tsrCreated);
		if(ret==0){
			if(searchId < o.searchId){
				return 1;
			}
			if(searchId > o.searchId){
				return -1;
			}
			return 0;
		}
		return ret;
	}


}
