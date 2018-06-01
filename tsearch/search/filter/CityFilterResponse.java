package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.property.PropertyI;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.servers.response.InfSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.CityStringEquivalents;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

public class CityFilterResponse extends FilterResponse {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * matching results logging class. use the logger from
	 * ro.cst.tsearch.search.address.AddressMatcher
	 */
	private static final Category matcherLoggerDetails = Logger.getLogger(Log.DETAILS_PREFIX + MatchAlgorithm.class.getName());

	public CityFilterResponse(long searchId) {
		super(searchId);
	}

	public CityFilterResponse(String key, long searchid) {
		super(key, searchid);
	}

	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		BigDecimal score;
		String candCityString = getCandidateCity(row);
		String refCityString = sa.getAtribute(SearchAttributes.P_CITY);

		candCityString = CityStringEquivalents.getInstance().getEquivalentToken(candCityString, refCityString);
		refCityString = CityStringEquivalents.getInstance().getEquivalentToken(refCityString, candCityString);

		if (StringUtils.isEmpty(candCityString)) { // fix for bug #2688
			score = ATSDecimalNumberFormat.ONE; // no candidate address => score
												// = 1.0
		} else if (StringUtils.isEmpty(refCityString)) {
			score = ATSDecimalNumberFormat.ONE; // no reference address => score
												// = 1.0
		} else {
			score = (MatchAlgorithm.getInstance(MatchAlgorithm.TYPE_DEFAULT, refCityString.replaceAll("\\s+", ""),
					candCityString.replaceAll("\\s+", ""), searchId)).getScore();			
			matcherLoggerDetails.debug("matching ref=[" + refCityString + "] vs cand=[" + candCityString + "]= " + score);
			IndividualLogger.info("matching ref=[" + refCityString + "] vs cand=[" + candCityString + "]= " + score, searchId);
			// addressLogger.logString("|" + refAddress + "| vs |" + candAddress
			// + "| = |" + score + "|");
		}
		return score;
	}

	/**
	 * get the city from the ParsedResponse that should be compared with the one
	 * entered in the search page
	 * 
	 * @param pr
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private String getCandidateCity(ParsedResponse pr) {
		String city = null;
		DocumentI documentI = pr.getDocument();
		if (documentI != null) {
			for (PropertyI property : documentI.getProperties()) {
				AddressI addressI = property.getAddress();
				if (addressI!=null) {
					city = addressI.getCity();
					if (StringUtils.isNotEmpty(city)) {
						break;
					}
				}
			}
		} else {
			// trying to extract city from PIS structure
			if (pr.getPropertyIdentificationSet().size() > 0) {
				PropertyIdentificationSet pis = (PropertyIdentificationSet) pr.getPropertyIdentificationSet().get(0);
				if (pis != null) {
					city = pis.getAtribute("City");
				}
			} else {
				try {
					for (InfSet pis : (Vector<InfSet>) pr.infVectorSets.get("PropertyIdentificationSet")) {
						city = pis.getAtribute("City");
						if(StringUtils.isNotEmpty(city)) {
							break;
						}
					}
				} catch (Exception e) {
					logger.error("Error while getting city!", e);
				}
			}
		}
		return city;
	}

	@Override
	public String getFilterName() {
		return "Filter by City";
	}

	@Override
	public String getFilterCriteria() {
		return "City='" + sa.getAtribute(SearchAttributes.P_CITY) + "'";
	}

}
