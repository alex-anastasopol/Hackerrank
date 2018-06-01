package ro.cst.tsearch.search.filter.newfilters.address;

import java.math.BigDecimal;
import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.servers.response.InfSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

public class OwnerZipCodeFilter extends FilterResponse {

	/**
	 * @author Olivia
	 */
	private static final long serialVersionUID = 1L;
	private String ownerZipCode = sa.getAtribute(SearchAttributes.OWNER_ZIP);
	/**
	 * matching results logging class. use the logger from
	 * ro.cst.tsearch.search.address.AddressMatcher
	 */
	private static final Category matcherLoggerDetails = Logger.getLogger(Log.DETAILS_PREFIX + MatchAlgorithm.class.getName());

	public OwnerZipCodeFilter(long searchId) {
		super(searchId);
	}

	public OwnerZipCodeFilter(String key, long searchid) {
		super(key, searchid);
	}

	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		BigDecimal score;
		String candOwnerZipCodeString = getCandidateOwnerZipCode(row);
		String refOwnerZipCodeString = ownerZipCode;

		if (StringUtils.isEmpty(candOwnerZipCodeString)) { 
			score = ATSDecimalNumberFormat.ONE; // no candidate zip code => score = 1.0
												
		} else if (StringUtils.isEmpty(refOwnerZipCodeString)) {
			score = ATSDecimalNumberFormat.ONE; // no reference address => score = 1.0
											
		} else {
			refOwnerZipCodeString = refOwnerZipCodeString.replaceAll("\\s+", "").trim();
			candOwnerZipCodeString = candOwnerZipCodeString.replaceAll("\\s+", "").trim();
			int refZip = 0;
			int candZip = 0;
			
			if (refOwnerZipCodeString.matches("\\d+\\s*-\\s*\\d+")) 
				refZip = Integer.parseInt(refOwnerZipCodeString.substring(0, refOwnerZipCodeString.indexOf("-")).trim());
			else
				refZip = Integer.parseInt(refOwnerZipCodeString);
			
			candZip = Integer.parseInt(candOwnerZipCodeString);	
			
			score = (refZip == candZip) ? ATSDecimalNumberFormat.ONE : ATSDecimalNumberFormat.ZERO;
			
			matcherLoggerDetails.debug("matching ref=[" + refOwnerZipCodeString + "] vs cand=[" + candOwnerZipCodeString + "]= " + score);
			IndividualLogger.info("matching ref=[" + refOwnerZipCodeString + "] vs cand=[" + candOwnerZipCodeString + "]= " + score, searchId);
		}
		return score;
	}

	/**
	 * get the Owner Zip Code from the ParsedResponse that should be compared 
	 * with the one entered in the search page
	 * 
	 * @param ParseResponse pr
	 * @return String ownerZipCode
	 */
	@SuppressWarnings("unchecked")
	private String getCandidateOwnerZipCode(ParsedResponse pr) {
		String zipCode = null;
		// trying to extract city from PIS structure
		if (pr.getPropertyIdentificationSet().size() > 0) {
			PropertyIdentificationSet pis = (PropertyIdentificationSet) pr.getPropertyIdentificationSet().get(0);
			if (pis != null) {
				zipCode = pis.getAtribute("OwnerZipCode");
			}
		} else {
			try {
				for (InfSet pis : (Vector<InfSet>) pr.infVectorSets.get("PropertyIdentificationSet")) {
					zipCode = pis.getAtribute("OwnerZipCode");
					if(StringUtils.isNotEmpty(zipCode)) {
						break;
					}
				}
			} catch (Exception e) {
				logger.error("Error while getting Owner Zip Code!", e);
			}
		}
		return zipCode;
	}

	@Override
	public String getFilterName() {
		return "Filter by Owner Zip Code";
	}

	@Override
	public String getFilterCriteria() {
		String zipCode = ownerZipCode;
		if (zipCode.matches("\\d+\\s*-\\s*\\d+")) {
			int idx = zipCode.indexOf("-");
			zipCode = zipCode.substring(0, idx).trim();
		}
			
		return "Owner Zip Code='" + zipCode + " ";
	}

}
