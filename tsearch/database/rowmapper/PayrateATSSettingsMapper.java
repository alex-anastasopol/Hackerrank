package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.data.Payrate;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;

public class PayrateATSSettingsMapper implements
		ParameterizedRowMapper<Payrate> {

	@Override
	public Payrate mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		Payrate payrate = new Payrate();
		try {
			payrate.setId(resultSet.getLong(Payrate.FIELD_ID));
		} catch (Exception e) {}
		try {
			payrate.setStartDate(FormatDate.getDateFromFormatedStringGMT(
        		resultSet.getString(Payrate.FIELD_START_DATE), FormatDate.TIMESTAMP));
		} catch (Exception e) {}
		try {
			payrate.setEndDate(FormatDate.getDateFromFormatedStringGMT(
        		resultSet.getString(Payrate.FIELD_END_DATE), FormatDate.TIMESTAMP));   
		} catch (Exception e) {}
		try {
			if(resultSet.getObject(Payrate.FIELD_COMMUNITY_ID) != null) {
				payrate.setCommId(resultSet.getLong(Payrate.FIELD_COMMUNITY_ID));
			}
		} catch (Exception e) {}
		try {
			if(resultSet.getObject(Payrate.FIELD_COUNTY_ID) != null) {
				payrate.setCountyId(resultSet.getLong(Payrate.FIELD_COUNTY_ID));
			}
		} catch (Exception e) {}
		try {
			payrate.setCountyName(resultSet.getString(DBConstants.FIELD_COUNTY_NAME));                
		} catch (Exception e) {}
		try {
			payrate.setStateAbv(resultSet.getString(DBConstants.FIELD_STATE_ABV));
		} catch (Exception e) {}
		try {
			payrate.setDueDate(FormatDate.getDateFromFormatedStringGMT(
        		resultSet.getString("due_date"), FormatDate.TIMESTAMP));
		} catch (Exception e) {}
		try {
			payrate.setCityDueDate(FormatDate.getDateFromFormatedStringGMT(
        		resultSet.getString("CITY_DUE_DATE"), FormatDate.TIMESTAMP));                                                
        
		} catch (Exception e) {}
		
		try {
			if(resultSet.getObject(Payrate.FIELD_FULL_SEARCH_C2A) != null) {
				payrate.setSearchValue(resultSet.getDouble(Payrate.FIELD_FULL_SEARCH_C2A));
			}
		} catch (Exception e) {}
		try {
			if(resultSet.getObject(Payrate.FIELD_FULL_SEARCH_A2C) != null) {
				payrate.setSearchCost(resultSet.getDouble(Payrate.FIELD_FULL_SEARCH_A2C));                                
			}
		} catch (Exception e) {}
		
		try {
			if(resultSet.getObject(Payrate.FIELD_UPDATE_C2A) != null) {
				payrate.setUpdateValue(resultSet.getDouble(Payrate.FIELD_UPDATE_C2A));
			}
		} catch (Exception e) {}
		try {
			if(resultSet.getObject(Payrate.FIELD_UPDATE_A2C) != null) {
				payrate.setUpdateCost(resultSet.getDouble(Payrate.FIELD_UPDATE_A2C));
			}
		} catch (Exception e) {}
		//-------------
		try {
			if(resultSet.getObject(Payrate.FIELD_CRTOWNER_C2A) != null) {
				payrate.setCurrentOwnerValue(resultSet.getDouble(Payrate.FIELD_CRTOWNER_C2A));
			}
		} catch (Exception e) {}
		try {
			if(resultSet.getObject(Payrate.FIELD_CRTOWNER_A2C) != null) {
				payrate.setCurrentOwnerCost(resultSet.getDouble(Payrate.FIELD_CRTOWNER_A2C));
			}
		} catch (Exception e) {}
		//-------------
		try {
			if(resultSet.getObject(Payrate.FIELD_REFINANCE_C2A) != null) {
				payrate.setRefinanceValue(resultSet.getDouble(Payrate.FIELD_REFINANCE_C2A));
			}
		} catch (Exception e) {}
		try {
			if(resultSet.getObject(Payrate.FIELD_REFINANCE_A2C) != null) {
				payrate.setRefinanceCost(resultSet.getDouble(Payrate.FIELD_REFINANCE_A2C));
			}
		} catch (Exception e) {}
		//-------------
		try {
			if(resultSet.getObject(Payrate.FIELD_CONSTRUCTION_C2A) != null) {
				payrate.setConstructionValue(resultSet.getDouble(Payrate.FIELD_CONSTRUCTION_C2A));
			}
		} catch (Exception e) {}
		try {
			if(resultSet.getObject(Payrate.FIELD_CONSTRUCTION_A2C) != null) {
				payrate.setConstructionCost(resultSet.getDouble(Payrate.FIELD_CONSTRUCTION_A2C));
			}
		} catch (Exception e) {}
		//-------------
		try {
			if(resultSet.getObject(Payrate.FIELD_COMMERCIAL_C2A) != null) {
				payrate.setCommercialValue(resultSet.getDouble(Payrate.FIELD_COMMERCIAL_C2A));
			}
		} catch (Exception e) {}
		try {
			if(resultSet.getObject(Payrate.FIELD_COMMERCIAL_A2C) != null) {
				payrate.setCommercialCost(resultSet.getDouble(Payrate.FIELD_COMMERCIAL_A2C));
			}
		} catch (Exception e) {}
		//-------------
		try {
			if(resultSet.getObject(Payrate.FIELD_OE_C2A) != null) {
				payrate.setOEValue(resultSet.getDouble(Payrate.FIELD_OE_C2A));
			}
		} catch (Exception e) {}
		try {
			if(resultSet.getObject(Payrate.FIELD_OE_A2C) != null) {
				payrate.setOECost(resultSet.getDouble(Payrate.FIELD_OE_A2C));
			}
		} catch (Exception e) {}
		//-------------
		try {
			if(resultSet.getObject(Payrate.FIELD_LIENS_C2A) != null) {
				payrate.setLiensValue(resultSet.getDouble(Payrate.FIELD_LIENS_C2A));
			}
		} catch (Exception e) {}
		try {
			if(resultSet.getObject(Payrate.FIELD_LIENS_A2C) != null) {
				payrate.setLiensCost(resultSet.getDouble(Payrate.FIELD_LIENS_A2C));
			}
		} catch (Exception e) {}
		//-------------
		try {
			if(resultSet.getObject(Payrate.FIELD_ACREAGE_C2A) != null) {
				payrate.setAcreageValue(resultSet.getDouble(Payrate.FIELD_ACREAGE_C2A));
			}
		} catch (Exception e) {}
		try {
			if(resultSet.getObject(Payrate.FIELD_ACREAGE_A2C) != null) {
				payrate.setAcreageCost(resultSet.getDouble(Payrate.FIELD_ACREAGE_A2C));
			}        
		} catch (Exception e) {}
		//-------------
		try {
			if(resultSet.getObject(Payrate.FIELD_SUBLOT_C2A) != null) {
				payrate.setSublotValue(resultSet.getDouble(Payrate.FIELD_SUBLOT_C2A));
			}
		} catch (Exception e) {}
		try {
			if(resultSet.getObject(Payrate.FIELD_SUBLOT_A2C) != null) {
				payrate.setSublotCost(resultSet.getDouble(Payrate.FIELD_SUBLOT_A2C));
			}
		} catch (Exception e) {}
		//-------------
		try {
			if(resultSet.getObject(Payrate.FIELD_INDEX_C2A) != null) {
				payrate.setIndexC2A(resultSet.getDouble(Payrate.FIELD_INDEX_C2A));
			}
		} catch (Exception e) {}
		try {
			if(resultSet.getObject(Payrate.FIELD_INDEX_A2C) != null) {
				payrate.setIndexA2C(resultSet.getDouble(Payrate.FIELD_INDEX_A2C));
			}
		} catch (Exception e) {}
		//-------------
		try {
			if(resultSet.getObject(DBConstants.FIELD_COUNTY_COMMUNITY_DEFAULT_CERTIFICATION_DATE_OFFSET) != null) {
				payrate.setCertificationDateOffset(resultSet.getString(
					DBConstants.FIELD_COUNTY_COMMUNITY_DEFAULT_CERTIFICATION_DATE_OFFSET));
			} else {
				payrate.setCertificationDateOffset("");
			}
		} catch (Exception e) {}
		try {
			payrate.setOfficialStartDateOffset(resultSet.getInt(
					DBConstants.FIELD_COUNTY_COMMUNITY_DEFAULT_OFFICIAL_START_DATE_OFFSET));
		} catch (Exception e) {}
		//-------------
		try {
			if(resultSet.getObject(Payrate.FIELD_FVS_C2A) != null) {
				payrate.setFvsValue(resultSet.getDouble(Payrate.FIELD_FVS_C2A));
			}
		} catch (Exception e) {}
		try {
			if(resultSet.getObject(Payrate.FIELD_FVS_A2C) != null) {
				payrate.setFvsCost(resultSet.getDouble(Payrate.FIELD_FVS_A2C));
			}        
		} catch (Exception e) {}
		
		return payrate;
	}

}
