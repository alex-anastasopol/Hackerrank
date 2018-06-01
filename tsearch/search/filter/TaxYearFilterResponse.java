/**
 * 
 */
package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.servers.response.ParsedResponse;

/**
 * @author radu bacrau
 */
@SuppressWarnings("serial")
public class TaxYearFilterResponse extends FilterResponse {

	private static final Category logger 		= Logger.getLogger(TaxYearFilterResponse.class);
	private static final Category loggerDetails = Logger.getLogger(TaxYearFilterResponse.class);

	/**
	 * @param searchId
	 */
	public TaxYearFilterResponse(long searchId) {
		super(searchId);		
		setThreshold(new BigDecimal("0.95"));
	}

	public TaxYearFilterResponse(long searchId, boolean multipleYears, int numberOfYearsAllowed, boolean sortDesc) {
		super(searchId);		
		setThreshold(new BigDecimal("0.95"));
		this.multipleYears = multipleYears;
		this.numberOfYearsAllowed = numberOfYearsAllowed;
		this.sortDesc = sortDesc;
	}
	/**
	 * maximum tax year
	 */
	private int maxYear = 0;
	
	private int numberOfYears = 1;
	
	private int numberOfYearsAllowed = 0;

	private boolean multipleYears = false;
	
	private boolean sortDesc = false;
	/**
	 * Get tax year from a parse response
	 * @param pr
	 * @return tax year or 0 if not found
	 */
	private int getTaxYear(ParsedResponse pr){
		if(pr.getTaxHistorySetsCount() == 0){
			return 0;
		}
		String yearStr = pr.getTaxHistorySet(0).getAtribute("ReceiptDate");
		if(!yearStr.matches("\\d+")){
			yearStr = pr.getTaxHistorySet(0).getAtribute("Year");
			if(!yearStr.matches("\\d+")){
				return 0;
			}
		}
		return Integer.parseInt(yearStr);		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void computeScores(Vector rows){
		
		if (sortDesc){
			Collections.sort(rows,
					new Comparator<ParsedResponse>() {
						@Override
						public int compare(ParsedResponse p1, ParsedResponse p2) {
							try {
								int pos1 = p1.getYear();
								int pos2 = p2.getYear();
								return (((Integer)pos1).compareTo(pos2)) * -1;
							}
							catch(Exception e) {
								e.printStackTrace();
								return -1;
							}
						}
					} 
	        );
		}
		
		// reset maxYear in case filter is reused
		maxYear = 0;
		// make a first pass to determine highest tax year
		for (int i = 0; i < rows.size(); i++){
			ParsedResponse row = (ParsedResponse)rows.get(i);
			int year = getTaxYear(row);
			if(year > maxYear){
				maxYear = year;
			}
		}
		logger.debug("Max year=" + maxYear);
		loggerDetails.info("Max year=" + maxYear);
		// compute scores as usual
		super.computeScores(rows);
	}
	
	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row){
		int year = getTaxYear(row);
		int score = (year == maxYear) ? 1 : 0;
		
		if (numberOfYearsAllowed > 1){
			if (score == 1){
				numberOfYears++;
			} else if (score == 0){
				if ((numberOfYears > 1 && numberOfYears <= numberOfYearsAllowed)
						|| (numberOfYearsAllowed > 1 && (maxYear - numberOfYearsAllowed + 1 == year))){
					score = 1;
					numberOfYears++;
				}
			}
		}
		logger.debug("Crt year=" + year + ". Max year=" + maxYear + ". Score=" + score);
		loggerDetails.info("Crt year=" + year + ". Max year=" + maxYear + ". Score=" + score);
		return new BigDecimal(score);
	}
	
	@Override
    public String getFilterCriteria(){
		if (multipleYears && numberOfYearsAllowed > 1){
			return "Multiple years allowed: " + numberOfYearsAllowed + " . Years found " + ((numberOfYears > 1) ? (numberOfYears - 1) : numberOfYears);
		} else {
			return "Year='" + maxYear + "'";
		}
    }
}
