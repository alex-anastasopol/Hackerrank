package ro.cst.tsearch.reports.invoice;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public class InvoicedSearch implements ParameterizedRowMapper<InvoicedSearch>{
	
	public static final int SEARCH_NOT_INVOICED = 0;
	public static final int SEARCH_INVOICED_FINISHED = 1;
	public static final int SEARCH_INVOICED_INDEX = 2;
	public static final int SEARCH_INVOICED_INDEX_AND_FINISHED = 3;
	
	private long searchId;
	private int invoiced;
	/**
	 * @return the searchId
	 */
	public long getSearchId() {
		return searchId;
	}
	/**
	 * @param searchId the searchId to set
	 */
	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}
	/**
	 * @return the invoiced
	 */
	public int getInvoiced() {
		return invoiced;
	}
	/**
	 * @param invoiced the invoiced to set
	 */
	public void setInvoiced(int invoiced) {
		this.invoiced = invoiced;
	}
	@Override
	public InvoicedSearch mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		InvoicedSearch is = new InvoicedSearch();
		is.setSearchId(resultSet.getLong("searchId"));
		is.setInvoiced(resultSet.getInt("invoicedFlag"));
		return is;
	}
	
}
