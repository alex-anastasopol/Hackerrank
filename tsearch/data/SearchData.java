package ro.cst.tsearch.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;


public class SearchData implements ParameterizedRowMapper<SearchData>{

    private long id;
    private long searchId;
    private long abstractorId;
    
    
    /////////////////////////////////////////////////    
    public long getAbstractorId() {
        return abstractorId;
    }

    public void setAbstractorId(long abstractorId) {
        this.abstractorId = abstractorId;
    }

    public long getId() {
        return id;
    }

    
    /////////////////////////////////////////////////
    public void setId(long id) {
        this.id = id;
    }

    public long getSearchId() {
        return searchId;
    }

    public void setSearchId(long searchId) {
        this.searchId = searchId;
    }

	@Override
	public SearchData mapRow(ResultSet rs, int rowNum) throws SQLException {
		SearchData sd = new SearchData();
		sd.setSearchId(rs.getLong(DBConstants.FIELD_SEARCH_ID));
		sd.setAbstractorId(rs.getLong(DBConstants.FIELD_SEARCH_ABSTRACT_ID));
		sd.setId(sd.getSearchId());
		return sd;
	}
}
