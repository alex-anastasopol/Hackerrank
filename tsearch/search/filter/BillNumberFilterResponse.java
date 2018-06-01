package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
*
* rejects the documents with the Bill Number having a certain length
* and (optional) starting with some characters
* 
*/

public class BillNumberFilterResponse extends FilterResponse {

	private static final long serialVersionUID = 3129360115476070722L;
	
	private int rejectLength = 0;
	private String startsWith = "";
	
	protected static final Category loggerDetails = Logger.getLogger(Log.DETAILS_PREFIX + BillNumberFilterResponse.class.getName());

	public BillNumberFilterResponse(long searchId, int rejectLength) {
		super(searchId);
		threshold = new BigDecimal("0.90");
		this.rejectLength = rejectLength;
	}
	
	public BillNumberFilterResponse(long searchId, int rejectLength, String startsWith) {
		this(searchId, rejectLength);
		this.startsWith = startsWith;
	}
	
	@Override
	public String getFilterCriteria() {
		return "Reject the documents with the Bill Number having a certain length" +
				" and(optional) starting with some characters";
	}
	
	@SuppressWarnings("unchecked")
	protected Set<String> getCandBillNumbers(ParsedResponse row) {
		
		Set<String> billNumbers = new HashSet<String>();
		
		if(row.getTaxHistorySet() == null || row.getTaxHistorySetsCount() == 0) {
			return billNumbers;
		}
		
		for(TaxHistorySet ths: (Vector<TaxHistorySet>)row.getTaxHistorySet()) {
			String billNumber = ths.getAtribute("TaxBillNumber");
			if(!StringUtils.isEmpty(billNumber)){
				billNumbers.add(billNumber.trim());
			}
		}
		
		return billNumbers;
	}
	
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		double score = getScoreOneRowInternal(row);
		loggerDetails.debug("[" + getCandBillNumbers(row) + "]  score=" + score);		
        IndividualLogger.info("[" + getCandBillNumbers(row) + "]  score=" + score, searchId);
		return new BigDecimal(score);
	}
	
	private double getScoreOneRowInternal(ParsedResponse row) {
		Set<String> candBillNumbers = getCandBillNumbers(row);
		if (candBillNumbers.size()!=1) {		//a row has only a Bill Number
			return 0.0d;
		} 		
		Iterator<String> it = candBillNumbers.iterator();
		String billNumber = it.next();
		boolean toBeRejected = (billNumber.length()==rejectLength);
		if (!StringUtils.isEmpty(startsWith)) {
			toBeRejected  = toBeRejected && (billNumber.startsWith(startsWith));
		}
		if (toBeRejected) {
			return 0.0d;
		} 
		return 1.0d;
	}

}
