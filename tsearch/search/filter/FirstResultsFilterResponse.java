package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;

import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

/**
 * Used to keep track of the number of validated documents <br>
 * It also has an option to include the number of already saved documents of a given type when validating<br>
 * <br>
 * <b>IMPORTANT</b>: Should be the <b>last</b> filter/validator used for a given module because it must apply on ready to save documents<br>
 *  
 * @author Andrei
 *
 */
public class FirstResultsFilterResponse extends FilterResponse {

	private static final long serialVersionUID = 1L;
	
	private int maxNumberOfDocumentsToPass = 0;	//0 - means all documents pass
	private boolean applyToPriorFiles = true;
	
	private boolean countSavedInTSRI = false;
	private String checkDSinTSRI = null;		//null - means check all documents
	
	private int numberOfDocumentsFilteredAndPassed = 0;

	public FirstResultsFilterResponse(long searchId) {
		super(searchId);
		setThreshold(ATSDecimalNumberFormat.ONE);
	}

	public FirstResultsFilterResponse(long searchId, int maxNumberOfDocumentsToPass) {
		super(searchId);
		setThreshold(ATSDecimalNumberFormat.ONE);
		this.maxNumberOfDocumentsToPass = maxNumberOfDocumentsToPass;
	}
	
	public boolean isApplyToPriorFiles() {
		return applyToPriorFiles;
	}

	public void setApplyToPriorFiles(boolean applyToPriorFiles) {
		this.applyToPriorFiles = applyToPriorFiles;
	}

	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		
		//TODO implement checking TSRIndex documents
		
		if(numberOfDocumentsFilteredAndPassed < maxNumberOfDocumentsToPass 
				|| maxNumberOfDocumentsToPass == 0) {
			numberOfDocumentsFilteredAndPassed++;
			return ATSDecimalNumberFormat.ONE;
		}
		return ATSDecimalNumberFormat.ZERO;
	}
	
	public String getFilterCriteria() {
		return "Keep first " + maxNumberOfDocumentsToPass + " documents";
	}

}
