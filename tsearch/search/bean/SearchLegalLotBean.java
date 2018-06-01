package ro.cst.tsearch.search.bean;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class SearchLegalLotBean implements Serializable, ParameterizedRowMapper<SearchLegalLotBean> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Long legalLotId;
	private Long searchId;
	private String lotValue;
	
	public SearchLegalLotBean(){
		
	}
	
	public SearchLegalLotBean(Long legalLotId, Long searchId, String lotValue) {
		this.legalLotId = legalLotId;
		this.searchId = searchId;
		this.lotValue = lotValue;
	}

	public Long getLegalLotId() {
		return legalLotId;
	}

	public void setLegalLotId(Long legalLotId) {
		this.legalLotId = legalLotId;
	}

	public Long getSearchId() {
		return searchId;
	}

	public void setSearchId(Long searchId) {
		this.searchId = searchId;
	}

	public String getLotValue() {
		return lotValue;
	}

	public void setLotValue(String lotValue) {
		this.lotValue = lotValue;
	}

	@Override
	public SearchLegalLotBean mapRow(ResultSet resultSet, int rowNum)
			throws SQLException {
		SearchLegalLotBean legalLotBean = new SearchLegalLotBean();
		legalLotBean.setLegalLotId(resultSet.getLong(DBConstants.FIELD_LEGAL_LOT_ID));
		legalLotBean.setSearchId(resultSet.getLong(DBConstants.FIELD_LEGAL_LOT_SEARCH_ID));
		legalLotBean.setLotValue(resultSet.getString(DBConstants.FIELD_LEGAL_LOT_VALUE));
		return legalLotBean;
	}
	
	

}
