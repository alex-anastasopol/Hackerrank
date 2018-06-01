package ro.cst.tsearch.search.filter.newfilters.address;

import java.math.BigDecimal;
import java.util.Set;

import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.property.PropertyI;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.address.AddressMatcher;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.StringUtils;

public class GenericAddressDocumentFilter extends GenericAddressFilter {

	private static final long serialVersionUID = 1L;

	public GenericAddressDocumentFilter(long searchId) {
		super(searchId);
		setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
	}

	public GenericAddressDocumentFilter(String key, BigDecimal threshold, long searchId) {
		super(key, threshold, searchId);
		setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
	}

	public GenericAddressDocumentFilter(String key, long searchId) {
		super(key, searchId);
		setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
	}
	
	@Override
	public void init() {
		refAddress = new StandardAddress(getReferenceAddressString());
		refAddress.addressElements[StandardAddress.STREET_NUMBER] = sa.getAtribute(SearchAttributes.P_STREETNO);
		refAddress.addressElements[StandardAddress.STREET_NAME] = sa.getAtribute(SearchAttributes.P_STREETNAME);
		refAddress.addressElements[StandardAddress.STREET_SUFFIX] = sa.getAtribute(SearchAttributes.P_STREETSUFIX);
		refAddress.addressElements[StandardAddress.STREET_PREDIRECTIONAL] = sa.getAtribute(SearchAttributes.P_STREETDIRECTION);
		refAddress.addressElements[StandardAddress.STREET_POSTDIRECTIONAL] = sa.getAtribute(SearchAttributes.P_STREET_POST_DIRECTION);
		refAddress.addressElements[StandardAddress.STREET_SEC_ADDR_IDENT] = sa.getAtribute(SearchAttributes.P_STREETUNIT);
		
		if(!isEnableNumber()) {
			refAddress.clear(StandardAddress.STREET_NUMBER);
		}
		if(!isEnableUnit()) {
			refAddress.clear(StandardAddress.STREET_SEC_ADDR_IDENT);
			refAddress.clear(StandardAddress.STREET_SEC_ADDR_RANGE);
		}
		if(!isEnableDirection()) {
			refAddress.clear(StandardAddress.STREET_PREDIRECTIONAL);
		}
		if(!isEnableName()) {
			refAddress.clear(StandardAddress.STREET_NAME);
		}
		if(!isEnablePostDirection()) {
			refAddress.clear(StandardAddress.STREET_POSTDIRECTIONAL);
		}
		if(!isEnableSuffix()) {
			refAddress.clear(StandardAddress.STREET_SUFFIX);
		}
		
		for (int i = 0; i < refAddress.addressElements.length; i++) {
			if(StringUtils.isEmpty(refAddress.addressElements[i])) {
				refAddress.addressElements[i] = null;
			}
		}
		
		addressMatcher = new AddressMatcher(refAddress);
		emptyRefAddress = refAddress.isEmpty();
		
		try{
			DataSite server = null;
			if(miServerID > 0) {
				server = HashCountyToIndex.getDateSiteForMIServerID(getSearch().getCommId(), miServerID);
			} else {
				server = HashCountyToIndex.getCrtServer(searchId, false);
			}			
			addressMatcher.setTokenWeights(server.getAddressTokenWeights());
			addressMatcher.setMissingValues(server.getAddressTokenMissing());
		}catch(Exception e){ 
			// if there's an exception, we simply use the default weights
			logger.error("Error setting token weights for searchId " + searchId, e);
		}
	}

	/**
	 * Evaluate candidate for address matching.
	 * 
	 * @see ro.cst.tsearch.search.filter.FilterResponse#getScoreOneRow(ro.cst.tsearch.servers.response.ParsedResponse)
	 */ 
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		BigDecimal score;

		StandardAddress candAddress = getCandidateAddress(row.getDocument());
		if ( candAddress == null ) {
			if(markIfCandidatesAreEmpty) {
				score = ATSDecimalNumberFormat.NA;
			} else {
				score = ATSDecimalNumberFormat.ONE;
			} // no candidate address => score = 1.0
		} else if (emptyRefAddress) {
			if(markIfCandidatesAreEmpty) {
				score = ATSDecimalNumberFormat.NA;
			} else {
				score = ATSDecimalNumberFormat.ONE;
			} // no reference address => score = 1.0
		} else {
			
			if(!isEnableNumber()) {
				candAddress.clear(StandardAddress.STREET_NUMBER);
			}
			if(!isEnableUnit()) {
				candAddress.clear(StandardAddress.STREET_SEC_ADDR_IDENT);
				candAddress.clear(StandardAddress.STREET_SEC_ADDR_RANGE);
			}
			if(!isEnableDirection()) {
				candAddress.clear(StandardAddress.STREET_PREDIRECTIONAL);
			}
			if(!isEnableName()) {
				candAddress.clear(StandardAddress.STREET_NAME);
			}
			if(!isEnablePostDirection()) {
				candAddress.clear(StandardAddress.STREET_POSTDIRECTIONAL);
			}
			if(!isEnableSuffix()) {
				candAddress.clear(StandardAddress.STREET_SUFFIX);
			}
			
			if(candAddress.isEmpty()) {
				if(markIfCandidatesAreEmpty) {
					score = ATSDecimalNumberFormat.NA;
				} else {
					score = ATSDecimalNumberFormat.ONE;
				}
			} else {
			
				score = new BigDecimal(addressMatcher.matches(candAddress));
				logger.debug("matching ref=[" + refAddress + "] vs cand=[" + candAddress + "]= " + score);
				
				
				if (isEnableMissingUnit() && StringUtils.isEmpty(candAddress.getAddressElement(5)) && score.compareTo(getThreshold()) >= 0){
					score = ATSDecimalNumberFormat.ONE;;
				}
				
			}

		}
		return score;
	}

	private StandardAddress getCandidateAddress(DocumentI documentI) {
		if(documentI == null) {
			return null;
		}
		StandardAddress address = null;
		Set<PropertyI> properties = documentI.getProperties();
		for (PropertyI property : properties) {
			AddressI addressI = property.getAddress();
			
			if(addressI != null) {
				address = new StandardAddress("");
				address.addressElements[StandardAddress.STREET_NUMBER] = addressI.getNumber().toUpperCase();
				address.addressElements[StandardAddress.STREET_NAME] = addressI.getStreetName().toUpperCase();
				address.addressElements[StandardAddress.STREET_SUFFIX] = addressI.getSuffix().toUpperCase();
				address.addressElements[StandardAddress.STREET_PREDIRECTIONAL] = addressI.getPreDiretion().toUpperCase();
				address.addressElements[StandardAddress.STREET_POSTDIRECTIONAL] = addressI.getPostDirection().toUpperCase();
				address.addressElements[StandardAddress.STREET_SEC_ADDR_IDENT] = addressI.getIdentifierNumber().toUpperCase();
				
				for (int i = 0; i < address.addressElements.length; i++) {
					if(StringUtils.isEmpty(address.addressElements[i])) {
						address.addressElements[i] = null;
					}
				}
				
				if(!address.isEmpty()) {
					return address;
				}
			}
		}
		return null;
	}
	
}
