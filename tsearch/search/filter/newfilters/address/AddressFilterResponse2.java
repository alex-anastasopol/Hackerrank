
package ro.cst.tsearch.search.filter.newfilters.address;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.address.AddressMatcher;
import ro.cst.tsearch.search.address.AddressStringUtils;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.property.PropertyI;

/**
 * @author catalinc
 */
public class AddressFilterResponse2 extends FilterResponse {
	/**
	 * Version ID - in order to avoid the compiler warning 
	 */
	private static final long serialVersionUID = 1000000000L;
	
	/**
	 * Main logging class.
	 */
	protected static final Category logger = Logger.getLogger(AddressFilterResponse2.class);
			
	/**
	 * Details logging class.
	 */	
	protected static final Category loggerDetails = Logger.getLogger(Log.DETAILS_PREFIX + AddressFilterResponse2.class.getName());

	/**
	 * matching results logging class. use the logger from ro.cst.tsearch.search.address.AddressMatcher
	 */	
	private static final Category matcherLoggerDetails = Logger.getLogger(Log.DETAILS_PREFIX + AddressMatcher.class.getName());
	
	/**
	 * Address matching.
	 */
	protected AddressMatcher addressMatcher;	
	protected boolean emptyRefAddress = false; // used for returning true when comparing
	protected StandardAddress refAddress; 
	protected long searchId = -1;
	public AddressFilterResponse2(long searchId){
		this(SearchAttributes.NO_KEY, new BigDecimal("0.8"),searchId);
		this.searchId = searchId;
	}
	
	protected boolean ignoreUnitOnEmpty  = false;
	protected boolean checkSubdivisionForAddress = false;
	protected boolean ignoreUnitOnUniqueResult = false;
	protected boolean matchAddressOrSubdivision = false;
	/**
	 * Constructor.
	 * 
	 * @param searchAttributes Current SearchAttributes. 
	 * 
	 * @param key 
	 */
	public AddressFilterResponse2(String key,long searchid){
		this(key, new BigDecimal("0.8"),searchid);
		this.searchId = searchid;
	}
	
	/**
	 * Constructor.
	 * 
	 * @param searchAttributes Current SearchAttributes.
	 * 
	 * @param key
	 *  
	 * @param ap AddressParser used by this address filter.
	 * 
	 * @param am AddressMatcher used by this address filter.
	 */
	public AddressFilterResponse2(String  key, BigDecimal threshold,long searchId){
		super(key,searchId);
		this.threshold = threshold;
		this.searchId = searchId;
		strategyType = STRATEGY_TYPE_BEST_RESULTS;		
		refAddress = new StandardAddress(getReferenceAddressString());	
		
		addressMatcher = new AddressMatcher(refAddress);
		emptyRefAddress = AddressStringUtils.isEmptyAddress(refAddress);
	}
	
	
	/**
	 * indicates whether the token weights were set or not.  we need to set them at first usage of address 
	 * matcher, not in the constructor, because we use the current thread for getting the server name and 
	 * the thread might not have been started when the filter is created
	 */	
	
	protected boolean weightsNotSet = true;
	
	private boolean isUniqueResult = false;
	
	@SuppressWarnings("rawtypes")
	@Override
	public void computeScores(Vector rows){
		isUniqueResult = (rows.size() == 1);
		super.computeScores(rows);
	}
	
	/**
	 * Evaluate candidate for address matching.
	 * 
	 * @see ro.cst.tsearch.search.filter.FilterResponse#getScoreOneRow(ro.cst.tsearch.servers.response.ParsedResponse)
	 */ 
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		BigDecimal score=new BigDecimal(0);

		List<String> candAddressStringList = getCandidates(row);
		
		for (String candAddressString : candAddressStringList) {
			if ( StringUtils.isEmpty(candAddressString) ){
				score = threshold; // no candidate address => score = threshold
			}else if (emptyRefAddress){				
				score = threshold; // no reference address => score = threshold
			}else{	
				BigDecimal newScore = calculateScore(candAddressString);
				score = new BigDecimal(Math.max(score.doubleValue(),newScore.doubleValue()));
			}
		}
		
		return score;
	}
	
	public BigDecimal calculateScore(String candAddressString) {
		BigDecimal score=new BigDecimal(0); 
		StandardAddress candAddress = new StandardAddress(candAddressString.replace("UNIT:","UNIT "));
		// if the weights were not already set (a filter is created for each query, so we do not need to set them for each row)
		if(weightsNotSet){
			try{
				DataSite server = HashCountyToIndex.getCrtServer(searchId, false);				
				addressMatcher.setTokenWeights(server.getAddressTokenWeights());
				addressMatcher.setMissingValues(server.getAddressTokenMissing());
				if( (ignoreUnitOnEmpty && StringUtils.isEmpty(sa.getAtribute(SearchAttributes.P_STREETUNIT))) || 
					(ignoreUnitOnUniqueResult && isUniqueResult) ) {
					double[] weights = server.getAddressTokenWeights();
					try {
						weights[6] = 0;
						addressMatcher.setTokenWeights(weights);
						weights = server.getAddressTokenMissing();
						weights[6] = 0;
						addressMatcher.setMissingValues(weights);
					}catch(Exception e) {
						e.printStackTrace();
					}
				}
				weightsNotSet = false;
				//addressLogger.logString("Modified weights for server " + serverName + "|" + SearchSites.createStringFromDoubleArray(server.getAddressTokenWeights()) + "| | " + SearchSites.createStringFromDoubleArray(server.getAddressTokenMissing()) + "|");
			}catch(Exception e){ // if there's an exception, we simply use the pre-existing weights
				e.printStackTrace();
			}
		}
		score = new BigDecimal(addressMatcher.matches(candAddress));
		// use the logger from ro.cst.tsearch.search.address.AddressMatcher
		matcherLoggerDetails.debug("matching ref=[" +	refAddress +	"] vs cand=[" +	candAddress + "]= " + score);
        IndividualLogger.info( "matching ref=[" +   refAddress +    "] vs cand=[" + candAddress + "]= " + score,searchId );
		//addressLogger.logString("|" + refAddress +	"| vs |" +	candAddress + "| = |" + score + "|");
        return score;
	}
	/**
	 * Gets candidate address.
	 * 
	 * @param pr ParsedResponse row.
	 * 
	 * @return list of address candidates for matching.
	 */
	public List<String> getCandidateAddress(ParsedResponse pr)
	{
		List<String> addressCandidates = new ArrayList<String>();
		// try to get the address token list from the parsed response		
		String addrString = null;
		List<String> uncompleteAddressList = new ArrayList<String>();
		DocumentI documentI = pr.getDocument();
		if(documentI != null) {
			boolean foundAtLeastOneStreet = false;
			for (PropertyI property : documentI.getProperties()) {
				AddressI add = property.getAddress();
				String strNo = add.getNumber();
				strNo = (strNo == null ? "" : strNo);
				String strName = add.getStreetName();
				strName = (strName == null ? "" : strName);
				addrString = add.shortFormString();
				addrString = addrString.replaceAll("(?i)&nbsp;?"," ").trim();
				addrString = add.shortFormString();
				if(!addrString.trim().isEmpty()) {
					foundAtLeastOneStreet = true;
					addressCandidates.add(addrString);
					if (StringUtils.isEmpty(strName)||StringUtils.isEmpty(strNo)){
						uncompleteAddressList.add(addrString);
					}
				}
				
				if(StringUtils.isNotEmpty(add.getPreDiretion())) {
					if(StringUtils.isNotEmpty(add.getStreetName()) && add.getStreetName().matches("\\d+.*?")) {
						/* Bug 5270 */
						addrString = add.getFractio() + " " + add.getNumber() + add.getPreDiretion() + add.getStreetName() + " " + add.getSuffix() + " " + add.getPostDirection();
						if(!StringUtils.isEmpty(add.getBuilding()) ){
							addrString += " B:" + add.getBuilding();
						}	
						if( !StringUtils.isEmpty(add.getIdentifierNumber()) ){
							if(!StringUtils.isEmpty(add.getIdentifierType())){
								addrString+=" "+add.getIdentifierType()+":"+add.getIdentifierNumber();
							}
							else{
								addrString+=" #"+ add.getIdentifierNumber();
							}
						}
						addrString = addrString.replaceAll("\\s+", " ").trim();
					}
					if(!addrString.trim().isEmpty()) {
						foundAtLeastOneStreet = true;
						addressCandidates.add(addrString);
						if (StringUtils.isEmpty(strName)||StringUtils.isEmpty(strNo)){
							uncompleteAddressList.add(addrString);
						}
					}
				}
			}
			
			if(/*!foundAtLeastOneStreet &&*/ isCheckSubdivisionForAddress()) {
				//check to see if the subdivision is really an address.
				for (PropertyI property : documentI.getProperties()) {
					
					SubdivisionI subdivisionI = property.getLegal().getSubdivision();
					String subdivisionName = subdivisionI.getName();
					subdivisionI.setName(null);
					if(subdivisionI.isEmpty()) {
						addrString = subdivisionName;
						if(!addrString.trim().isEmpty()) {
							addressCandidates.add(addrString);
						}
					}
					subdivisionI.setName(subdivisionName);	//restore subdivision
					
				}
			}
			
		} else {
			addrString = pr.getAddressString();
			
			// if the address token list was not found in parsed response, then get it from pis
			if(addrString == null) {
				// trying to extract address from PIS structure - check all elements from PIS until an address is found (fix for bug #2638)
				int pisLen = pr.getPropertyIdentificationSet().size(); 
				if(pisLen > 0){
					int i = 0;
					boolean foundAddr =  false;
					List<PropertyIdentificationSet> allCandPis = new Vector<PropertyIdentificationSet>();
					while ((i < pisLen)){
						PropertyIdentificationSet pis = (PropertyIdentificationSet)pr.getPropertyIdentificationSet().get(i);
						if(pis != null) {
							allCandPis.add(pis);
							String strNo = pis.getAtribute("StreetNo");
							strNo = (strNo == null ? "" : strNo);
							String strName = pis.getAtribute("StreetName");
							strName = (strName == null ? "" : strName);
							addrString = strNo + " " + strName;
							addrString = addrString.replaceAll("(?i)&nbsp;?"," ").trim();
							foundAddr = addrString.length() != 0;
							if (foundAddr){
								addressCandidates.add(addrString);
								if (StringUtils.isEmpty(strName)||StringUtils.isEmpty(strNo)){
									uncompleteAddressList.add(addrString);
								}
							}
						}
						i++;
					}
					
					if(isCheckSubdivisionForAddress() ) {
						boolean isUncertainSubdivision = isUncertainSubdivision(allCandPis, sa);
						
						if(isUncertainSubdivision){
					    	
					    	String refSubdiv = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
					    	if(StringUtils.isNotEmpty(refSubdiv)) {
					    		addressCandidates.add(refSubdiv);
					    	}
					    }
					}
					
				}
			} else {
				// if the address string is empty, then turn it into a null, to simplify further code
	//			if(addrString != null && addrString.length() == 0){
	//				addrString = "";
	//			}
				addressCandidates.add(addrString);				
			}
		}
		
		
		
		
		// test to see if all addresses have street number and street name
		for (String string : uncompleteAddressList) {
			if (addressCandidates.size() > 1) {
				addressCandidates.remove(string);
			}
		}
		
		if(addressCandidates.isEmpty()) {
			addressCandidates.add("");
		}
		return addressCandidates;
	}
	
	/**
	 * Gets reference address.
	 * 
	 * @return Reference address as a string.
	 */
	protected String getReferenceAddressString() {
		
		String streetNo = sa.getAtribute(SearchAttributes.P_STREETNO);
		String streetDir = sa.getAtribute(SearchAttributes.P_STREETDIRECTION);
		String streetName = sa.getAtribute(SearchAttributes.P_STREETNAME);
		String streetSufix = sa.getAtribute(SearchAttributes.P_STREETSUFIX);
		String streetPostDirection = sa.getAtribute(SearchAttributes.P_STREET_POST_DIRECTION);
		String streetUnit = sa.getAtribute(SearchAttributes.P_STREETUNIT);
		
		boolean addUnitIdentifierType = false;
		if(StringUtils.isNotEmpty(streetUnit)) {
			if(!streetUnit.trim().startsWith("#")) {
				addUnitIdentifierType = true;
			}
		}
		
		String addrString = 
			(streetNo == null ? "" : streetNo) + " " +
			(streetDir == null ? "" : streetDir) + " " +
			(streetName == null ? "" : streetName) + " " +
			(streetSufix == null ? "" : streetSufix) + " " +
			(streetPostDirection == null ? "" : streetPostDirection) + " " +
			(streetUnit == null ? "" : (addUnitIdentifierType?" # ":"") + streetUnit);
			
		return addrString;
	}
	
	@Override
    public String getFilterName(){
    	return "Filter by Address";
    }
	
	@Override
	public String getFilterCriteria(){
    	String result = "Addr='" + getReferenceAddressString() + "'";
    	if (ignoreUnitOnUniqueResult && isUniqueResult) {
    		result += " (ignoring unit if unique candidate)";
    	}
    	
    	if (isIgnoreUnitOnEmpty()){
    		result += " (ignoring unit if empty on reference)";
    	}
		return result.replace(")(", ", ").replaceAll("\\s+"," ");
    }

	public boolean isIgnoreUnitOnEmpty() {
		return ignoreUnitOnEmpty;
	}

	public void setIgnoreUnitOnEmpty(boolean ignoreUnitOnEmpty) {
		this.ignoreUnitOnEmpty = ignoreUnitOnEmpty;
	}
	
	public boolean isCheckSubdivisionForAddress() {
		return checkSubdivisionForAddress;
	}

	public void setCheckSubdivisionForAddress(boolean checkSubdivisionForAddress) {
		this.checkSubdivisionForAddress = checkSubdivisionForAddress;
	}
	
	public boolean isIgnoreUnitOnUniqueResult() {
		return ignoreUnitOnUniqueResult;
	}

	public void setIgnoreUnitOnUniqueResult(boolean ignoreUnitOnUniqueResult) {
		this.ignoreUnitOnUniqueResult = ignoreUnitOnUniqueResult;
	}

	/**
	 * If either the ref subdivion is empty or all the candidates subdivisions are empty we signal this 
	 * @param allCandPis
	 * @param saRef
	 * @return
	 */
	private boolean isUncertainSubdivision(List<PropertyIdentificationSet> allCandPis, SearchAttributes saRef) {
		for (int i = 0; i < allCandPis.size(); i++)
		{
			PropertyIdentificationSet pisCand =
				(PropertyIdentificationSet)allCandPis.get(i);
			if(!isEmptySubdivision(pisCand))
				return false;
		}
		return true;
	}
	
	private boolean isEmptySubdivision(PropertyIdentificationSet pisRef) {
		if(StringUtils.isEmpty(pisRef.getAtribute("SubdivisionSection")) &&
				StringUtils.isEmpty(pisRef.getAtribute("SubdivisionPhase")) &&
				StringUtils.isEmpty(pisRef.getAtribute("SubdivisionCode")) &&
				StringUtils.isEmpty(pisRef.getAtribute("SubdivisionLotNumber")) &&
				StringUtils.isEmpty(pisRef.getAtribute("SubdivisionUnit")) &&
				StringUtils.isEmpty(pisRef.getAtribute("SubdivisionBlock")) )
			return true;
		return false;
	}

	public boolean isEmptyRefAddress() {
		return emptyRefAddress;
	}

	public void setEmptyRefAddress(boolean emptyRefAddress) {
		this.emptyRefAddress = emptyRefAddress;
	}
	
	public List<String> getCandidates(ParsedResponse row) {
		return getCandidateAddress(row);	
	}
	
}
