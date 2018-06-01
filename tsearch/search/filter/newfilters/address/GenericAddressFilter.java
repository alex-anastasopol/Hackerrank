package ro.cst.tsearch.search.filter.newfilters.address;

import java.math.BigDecimal;
import java.util.Set;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.address.AddressMatcher;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.property.PropertyI;

public class GenericAddressFilter extends FilterResponse{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static final Category logger = Logger.getLogger(GenericAddressFilter.class);
	protected int miServerID = -1;
	protected AddressMatcher addressMatcher;	
	protected boolean emptyRefAddress = false; // used for returning true when comparing
	protected StandardAddress refAddress; 
	
	protected boolean enableNumber = true;
	protected boolean enableName = true;
	protected boolean enableDirection = true;
	protected boolean enableSuffix = true;
	protected boolean enablePostDirection = true;
	protected boolean enableUnit = true;
	protected boolean enableMissingUnit = false;
	
	protected boolean markIfCandidatesAreEmpty = false;
	protected boolean tryAddressFromDocument = false;
	
	public GenericAddressFilter(long searchId){
		super(searchId);
		this.threshold = new BigDecimal("0.8");
		strategyType = STRATEGY_TYPE_BEST_RESULTS;
		setInitAgain(true);
	}
	
	public GenericAddressFilter(String key, long searchId){
		this(key, new BigDecimal("0.8"), searchId);
		setInitAgain(true);
	}

	public GenericAddressFilter(String key, BigDecimal threshold, long searchId) {
		super(key,searchId);
		this.threshold = threshold;
		strategyType = STRATEGY_TYPE_BEST_RESULTS;
		setInitAgain(true);
	}
	
	public void disableAll() {
		enableDirection = false;
		enableName = false;
		enableNumber = false;
		enablePostDirection = false;
		enableSuffix = false;
		enableUnit = false;
		enableMissingUnit = false;
	}
	
	public int getMiServerID() {
		return miServerID;
	}

	public void setMiServerID(int miServerID) {
		this.miServerID = miServerID;
	}
	
	@Override
	public void init() {
		init(getReferenceAddressString());
	}
	
	public void init(String reference) {
		refAddress = new StandardAddress(reference);
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
	 * Gets reference address.
	 * 
	 * @return Reference address as a string.
	 */
	protected String getReferenceAddressString() {
		if(refAddress!=null){
			return refAddress.getAddrInputString();
		}
		
		return defaultReferenceAddressString(sa,isEnableNumber(),isEnableUnit());
		
	}
	
	public static String defaultReferenceAddressString(SearchAttributes sa,boolean enableNumber, boolean enableUnit){
		String streetNo = sa.getAtribute(SearchAttributes.P_STREETNO); 
		String streetDir = sa.getAtribute(SearchAttributes.P_STREETDIRECTION);
		String streetName = sa.getAtribute(SearchAttributes.P_STREETNAME);
		String streetSufix = sa.getAtribute(SearchAttributes.P_STREETSUFIX);
		String streetPostDirection = sa.getAtribute(SearchAttributes.P_STREET_POST_DIRECTION);
		String streetUnit = sa.getAtribute(SearchAttributes.P_STREETUNIT);
		
		String addrString = 
			(streetNo == null || !enableNumber? "" : streetNo) + " " +
			(streetDir == null ? "" : streetDir) + " " +
			(streetName == null ? "" : streetName) + " " +
			(streetSufix == null ? "" : streetSufix) + " " +
			(streetPostDirection == null ? "" : streetPostDirection) + " " +
			(streetUnit == null || !enableUnit? "" : streetUnit);
		return addrString;
	}

	public StandardAddress getRefAddress() {
		return refAddress;
	}

	public void setRefAddress(StandardAddress refAddress) {
		this.refAddress = refAddress;
	}

	public boolean isEnableNumber() {
		return enableNumber;
	}

	public void setEnableNumber(boolean enableNumber) {
		this.enableNumber = enableNumber;
	}

	public boolean isEnableName() {
		return enableName;
	}

	public void setEnableName(boolean enableName) {
		this.enableName = enableName;
	}

	public boolean isEnableDirection() {
		return enableDirection;
	}

	public void setEnableDirection(boolean enableDirection) {
		this.enableDirection = enableDirection;
	}

	public boolean isEnableSuffix() {
		return enableSuffix;
	}

	public void setEnableSuffix(boolean enableSuffix) {
		this.enableSuffix = enableSuffix;
	}

	public boolean isEnablePostDirection() {
		return enablePostDirection;
	}

	public void setEnablePostDirection(boolean enablePostDirection) {
		this.enablePostDirection = enablePostDirection;
	}

	public boolean isEnableUnit() {
		return enableUnit;
	}	

	public void setEnableUnit(boolean enableUnit) {
		this.enableUnit = enableUnit;
	}
	//this is for ILKaneRO, to validate docs by address unit, if candAddress doesn't have unit and candAddress != reffAddress, docs WILL be validated
	public boolean isEnableMissingUnit() {
		return enableMissingUnit;
	}
	
	public void setEnableMissingUnit(boolean enableMissingUnit) {
		this.enableMissingUnit = enableMissingUnit;
	}
	
	/**
	 * Evaluate candidate for address matching.
	 * 
	 * @see ro.cst.tsearch.search.filter.FilterResponse#getScoreOneRow(ro.cst.tsearch.servers.response.ParsedResponse)
	 */ 
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		BigDecimal score;

		String candAddressString = getCandidateAddress(row);
		if ( StringUtils.isEmpty(candAddressString) ) {
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
			StandardAddress candAddress = new StandardAddress(candAddressString);
			
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
				logger.debug("matching ref=[" + refAddress + 
						"] vs cand=[" + candAddress + "]= " + score);
				
				if(tryAddressFromDocument && score.compareTo(getThreshold()) < 0) {
					candAddress = getShortAddressFromDocument(candAddress, row.getDocument());
					if(candAddress != null) {
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
						
						score = new BigDecimal(addressMatcher.matches(candAddress));
					}
				}
				if (isEnableMissingUnit() && StringUtils.isEmpty(candAddress.getAddressElement(5)) && score.compareTo(getThreshold()) >= 0){
					score = ATSDecimalNumberFormat.ONE;;
				}
				
			}

		}
		return score;
	}
	
	private StandardAddress getShortAddressFromDocument(StandardAddress originalAddress, DocumentI documentI) {
		if(documentI == null) {
			return null;
		}
		Set<PropertyI> properties = documentI.getProperties();
		for (PropertyI property : properties) {
			originalAddress.addressElements[2] = (property.getAddress().getStreetName() + " " + property.getAddress().getSuffix()).trim();
			originalAddress.addressElements[3] = null;
			break;
		}
		
		return originalAddress;
	}

	/**
	 * Gets candidate address.
	 * 
	 * @param pr ParsedResponse row.
	 * 
	 * @return Candidate address for matching.
	 */
	public String getCandidateAddress(ParsedResponse pr)
	{
		// try to get the address token list from the parsed response
		
		String addrString = pr.getAddressString();
		// if the address token list was not found in parsed response, then get it from pis
		if(addrString == null) {
			// trying to extract address from PIS structure 
			// check all elements from PIS until an address is found (fix for bug #2638)
			int pisLen = pr.getPropertyIdentificationSet().size(); 
			if(pisLen > 0){
				int i = 0;
				boolean foundAddr =  false;
				while ((i < pisLen) && !foundAddr){
					PropertyIdentificationSet pis = (
							PropertyIdentificationSet)pr.getPropertyIdentificationSet().get(i);
					if(pis != null) {
						String strNo = pis.getAtribute("StreetNo");
						strNo = (strNo == null ? "" : strNo);
						String strName = pis.getAtribute("StreetName");
						strName = (strName == null ? "" : strName);
						addrString = strNo + " " + strName;
						addrString = addrString.replaceAll("(?i)&nbsp;?"," ").trim();
						foundAddr = addrString.length() != 0;
					}
					i ++;
				}
			}
		}
		// if the address string is empty, then turn it into a null, to simplify further code
		if(addrString != null && addrString.length() == 0){
			addrString = "";
		}
		return addrString;
	}
	
	@Override
    public String getFilterName(){
    	return "Filter by Address(Generic Address Filter)";
    }
	
	@Override
	public String getFilterCriteria(){
    	return isEnableMissingUnit() ? "Addr Unit='"  +  getReferenceAddressString() + "'" : "Addr='" + getReferenceAddressString() + "'";
    }

	public boolean isMarkIfCandidatesAreEmpty() {
		return markIfCandidatesAreEmpty;
	}

	public void setMarkIfCandidatesAreEmpty(boolean markIfCandidatesAreEmpty) {
		this.markIfCandidatesAreEmpty = markIfCandidatesAreEmpty;
	}

	public boolean isTryAddressFromDocument() {
		return tryAddressFromDocument;
	}

	public void setTryAddressFromDocument(boolean tryAddressFromDocument) {
		this.tryAddressFromDocument = tryAddressFromDocument;
	}
	
}
