package ro.cst.tsearch.search.filter.newfilters.address;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.address.AddressMatcher;
import ro.cst.tsearch.search.address.AddressStringUtils;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.StringUtils;


public class AddressFilterForNext extends AddressFilterResponse2 {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static final int LOW_THRESHOLD		= 10;
	protected int passedDocumentsStopLimit = -1;
	protected int miServerID = -1;
	

	public AddressFilterForNext(String key,long searchid){
		super(key,searchid);
		refAddress = new StandardAddress(getReferenceAddressString());	
		refAddress.addressElements[0] = null;
		refAddress.addressElements[1] = null;
		refAddress.addressElements[3] = null;
		refAddress.addressElements[4] = null;
		refAddress.addressElements[5] = null;
		refAddress.addressElements[6] = null;
		addressMatcher = new AddressMatcher(refAddress);
		emptyRefAddress = AddressStringUtils.isEmptyAddress(refAddress);
	}

	public int getMiServerID() {
		return miServerID;
	}

	public void setMiServerID(int miServerID) {
		this.miServerID = miServerID;
	}

	protected void analyzeResult(ServerResponse sr, Vector rez) throws ServerResponseException
    {           
    	int initialCount = sr.getParsedResponse().getInitialResultsCount();
    	int passedDocuments = rez.size();
    	if(passedDocumentsStopLimit == -1) {
    	
	    	int threshold = Math.min(LOW_THRESHOLD, initialCount/2);
	        if( (initialCount - passedDocuments) >= threshold)
	        {
	            //filtered --> stop, do not go to next results
	            sr.setError(ServerResponse.NOT_PERFECT_MATCH_WARNING_FIRST);
	        }
    	} else {
    		if( passedDocuments <= passedDocumentsStopLimit)
	        {
	            //filtered --> stop, do not go to next results
	            sr.setError(ServerResponse.NOT_PERFECT_MATCH_WARNING_FIRST);
	        }
    	}
    }
	
	@Override
	public String getFilterCriteria(){
		if(refAddress != null && refAddress.addressElements!= null && refAddress.addressElements.length > 2) {
			return "StreetName='" + refAddress.addressElements[2] + "'";
		} else {
			return "StreetName='" + getReferenceAddressString() + "'";
		}
    }
	
	/**
	 * Gets reference address.
	 * 
	 * @return Reference address as a string.
	 */
	/*private String getReferenceAddressString() {
		return sa.getAtribute(SearchAttributes.P_STREETNAME);
	}*/
	
	public List<String> getCandidateAddress(ParsedResponse pr){
		// try to get the address token list from the parsed response
		List<String> candidateAddress = (new ArrayList<String>());
//		candidateAddress.add(addrString);
		
		String addrString = pr.getAddressString();
		// if the address token list was not found in parsed response, then get it from pis
		if(addrString == null) {
			// trying to extract address from PIS structure - check all elements from PIS until an address is found (fix for bug #2638)
			int pisLen = pr.getPropertyIdentificationSet().size(); 
			if(pisLen > 0){
				int i = 0;
				boolean foundAddr =  false;
				while ((i < pisLen) ){
					PropertyIdentificationSet pis = (PropertyIdentificationSet)pr.getPropertyIdentificationSet().get(i);
					if(pis != null) {
						String strName = pis.getAtribute("StreetName");
						strName = (strName == null ? "" : strName);
						addrString = strName;
						addrString = addrString.replaceAll("(?i)&nbsp;?"," ").trim();
						foundAddr = addrString.length() != 0;
						if (foundAddr ){
							candidateAddress.add(addrString);
						}
					}
					i ++;
				}
			}
		}else{
			candidateAddress.add(addrString);
		}
		// if the address string is empty, then turn it into a null, to simplify further code
//		if(addrString != null && addrString.length() == 0){
//			addrString = "";
//		}
		
		return candidateAddress;
	}
	
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		BigDecimal score = new BigDecimal(0);

		for (String candAddressString : getCandidateAddress(row)) {
			if ( StringUtils.isEmpty(candAddressString) ){
				score = ATSDecimalNumberFormat.ONE; // no candidate address => score = 1.0
			}else if (emptyRefAddress){				
				score = ATSDecimalNumberFormat.ONE; // no reference address => score = 1.0
			}else{	
				StandardAddress candAddress = new StandardAddress(candAddressString);
				
				candAddress.addressElements[0] = null;
				candAddress.addressElements[1] = null;
				candAddress.addressElements[3] = null;
				candAddress.addressElements[4] = null;
				candAddress.addressElements[5] = null;
				candAddress.addressElements[6] = null;
				// if the weights were not already set (a filter is created for each query, so we do not need to set them for each row)
				if(weightsNotSet){
					try{
						DataSite server = null;
						if(miServerID > 0) {
							server = HashCountyToIndex.getDateSiteForMIServerID(getSearch().getCommId(), miServerID);
						} else {
							server = HashCountyToIndex.getCrtServer(searchId, false);
						}
								
						addressMatcher.setTokenWeights(server.getAddressTokenWeights());
						addressMatcher.setMissingValues(server.getAddressTokenMissing());
						weightsNotSet = false;
						//addressLogger.logString("Modified weights for server " + serverName + "|" + SearchSites.createStringFromDoubleArray(server.getAddressTokenWeights()) + "| | " + SearchSites.createStringFromDoubleArray(server.getAddressTokenMissing()) + "|");
					}catch(Exception e){ // if there's an exception, we simply use the pre-existing weights
						e.printStackTrace();
					}
				}
				score = new BigDecimal(Math.max(score.doubleValue(), (new BigDecimal(addressMatcher.matches(candAddress))).doubleValue()));			
			}	
			
			// use the logger from ro.cst.tsearch.search.address.AddressMatcher
			//addressLogger.logString("|" + refAddress +	"| vs |" +	candAddress + "| = |" + score + "|");
		}		
		return score;
	}

	public int getPassedDocumentsStopLimits() {
		return passedDocumentsStopLimit;
	}

	/**
	 * When the number of passed document drops to this level the filter will stop the next query
	 * @param passedDocumentsStopLimit
	 */
	public void setPassedDocumentsStopLimit(int passedDocumentsStopLimit) {
		this.passedDocumentsStopLimit = passedDocumentsStopLimit;
	}
}
