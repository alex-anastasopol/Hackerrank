package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import java.util.Vector;

import org.apache.log4j.Category;

import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.Log;

/**
 * 
 * @author Oprina George
 * 
 *         Aug 22, 2011
 */

public class FirstDocumentFilterResponse extends FilterResponse {
	/**
	 * 
	 */
	private static final long serialVersionUID = -940126300483335003L;
	/**
	 * retains only the first document found -> used for Public Data Site (PD on Texas)
	 */

	@SuppressWarnings("deprecation")
	private static final Category logger = Category
			.getInstance(DuplicateInstrumentFilterResponse.class.getName());
	@SuppressWarnings("deprecation")
	private static final Category loggerDetails = Category
			.getInstance(Log.DETAILS_PREFIX
					+ DuplicateInstrumentFilterResponse.class.getName());

	/**
	 * @param searchId
	 */
	public FirstDocumentFilterResponse(long searchId) {
		super(searchId);
		setThreshold(new BigDecimal("0.95"));
	}

	private boolean first_row = false;

	@Override
	public void computeScores(Vector rows) {
		//if filter is reused
		first_row = false;
		super.computeScores(rows);
	}

	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		int score=0;
		if (!first_row) {
			score = 1;
			first_row = true;
		} 

		logger.info("Crt Documet Score=" + score);
		loggerDetails.info("Crt Documet Score=" + score);

		return new BigDecimal(score);
	}

	@Override
	public String getFilterCriteria() {
		return "Keep just the first document";
	}
}
