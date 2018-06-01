package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Vector;

import org.apache.log4j.Category;

import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.Log;

/**
 * 
 * @author Oprina George
 * 
 *         Apr 11, 2011
 */

public class DuplicateInstrumentFilterResponse extends FilterResponse {

	/**
	 *  removes duplicate instruments (those that have the same pin and year)
	 */
	
	private static final Category logger = Category
			.getInstance(DuplicateInstrumentFilterResponse.class.getName());
	private static final Category loggerDetails = Category
			.getInstance(Log.DETAILS_PREFIX
					+ DuplicateInstrumentFilterResponse.class.getName());

	/**
	 * @param searchId
	 */
	public DuplicateInstrumentFilterResponse(long searchId) {
		super(searchId);
		setThreshold(new BigDecimal("0.95"));
	}

	private HashSet<String> intrumentSet = new HashSet<String>();

	private String getInstrumentAndYear(ParsedResponse pr) {
		if (pr.getFirstPin().equals(""))
			return "";

		if (pr.getTaxHistorySetsCount() == 0) {
			return "";
		}

		String yearStr = pr.getTaxHistorySet(0).getAtribute("ReceiptDate");
		if (!yearStr.matches("\\d+")) {
			yearStr = pr.getTaxHistorySet(0).getAtribute("Year");
			if (!yearStr.matches("\\d+")) {
				return "";
			}
		}
		return pr.getFirstPin() + yearStr;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void computeScores(Vector rows) {
		// reset map in case filter is reused
		intrumentSet = new HashSet();

		for (int i = 0; i < rows.size(); i++) {
			ParsedResponse row = (ParsedResponse) rows.get(i);
			String instr_year = getInstrumentAndYear(row);
			intrumentSet.add(instr_year);
		}

		// compute scores as usual
		super.computeScores(rows);
	}

	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		String instr_year = getInstrumentAndYear(row);
		int score = 0;
		if (intrumentSet.contains(instr_year)) {
			score = 1;
			intrumentSet.remove(instr_year);
		}

		logger.info("Crt instrument_year=" + instr_year + ". Score=" + score);
		loggerDetails.info("Crt instrument_year=" + instr_year + ". Score="
				+ score);

		return new BigDecimal(score);
	}

	@Override
	public String getFilterCriteria() {
		return "Instrument_year duplicates";
	}
}
