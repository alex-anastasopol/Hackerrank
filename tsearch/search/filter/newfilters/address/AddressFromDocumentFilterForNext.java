package ro.cst.tsearch.search.filter.newfilters.address;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.search.address.AddressMatcher;
import ro.cst.tsearch.search.address.AddressStringUtils;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.property.PropertyI;


public class AddressFromDocumentFilterForNext extends AddressFilterForNext {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public AddressFromDocumentFilterForNext(String key,long searchid){
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
	
	public List<String> getCandidateAddress(ParsedResponse pr)
	{
		List<String> addressCandidates = new ArrayList<String>(); 
		// try to get the address token list from the parsed response		
		String addrString = pr.getAddressString();
		// if the address token list was not found in parsed response, then get it from pis
		if(addrString == null) {
			// trying to extract address from PIS structure - check all elements from PIS until an address is found (fix for bug #2638)
			int pisLen = pr.getPropertyIdentificationSet().size(); 
			if(pisLen > 0){
				int i = 0;
				boolean foundAddr =  false;
				while ((i < pisLen)){
					PropertyIdentificationSet pis = (PropertyIdentificationSet)pr.getPropertyIdentificationSet().get(i);
					if(pis != null) {
						String strName = pis.getAtribute("StreetName");
						strName = (strName == null ? "" : strName);
						addrString = strName;
						addrString = addrString.replaceAll("(?i)&nbsp;?"," ").trim();
						foundAddr = addrString.length() != 0;
						if (foundAddr){
							addressCandidates.add(addrString);
						}
					}
					i++;
				}
			}
		}else{
			//if the address string is empty, then turn it into a null, to simplify further code
			//if(addrString != null && addrString.length() == 0){
			//addrString = "";
			//}	
			addressCandidates.add(addrString); 
		}
		return addressCandidates;
	}
	
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		BigDecimal score= new BigDecimal(0);

		// String candAddressString =getCandidateAddress(row);
		
		List<String> candidateAddress = getCandidateAddress(row);
		if (candidateAddress.size()==0) {
			score = ATSDecimalNumberFormat.ONE; // no candidate address =>
												// score = 1.0
		}
		
		for (String candAddressString : candidateAddress) {
			if (StringUtils.isEmpty(candAddressString)) {
				score = ATSDecimalNumberFormat.ONE; // no candidate address =>
													// score = 1.0
			} else if (emptyRefAddress) {
				score = ATSDecimalNumberFormat.ONE; // no reference address =>
													// score = 1.0
			} else {
				StandardAddress candAddress = new StandardAddress(candAddressString);

				candAddress.addressElements[0] = null;
				candAddress.addressElements[1] = null;
				candAddress.addressElements[3] = null;
				candAddress.addressElements[4] = null;
				candAddress.addressElements[5] = null;
				candAddress.addressElements[6] = null;
				// if the weights were not already set (a filter is created for
				// each query, so we do not need to set them for each row)
				if (weightsNotSet) {
					try {
						DataSite server = null;
						if (miServerID > 0) {
							server = HashCountyToIndex.getDateSiteForMIServerID(getSearch().getCommId(), miServerID);
						} else {
							server = HashCountyToIndex.getCrtServer(searchId, false);
						}

						addressMatcher.setTokenWeights(server.getAddressTokenWeights());
						addressMatcher.setMissingValues(server.getAddressTokenMissing());
						weightsNotSet = false;
						// addressLogger.logString("Modified weights for server "
						// + serverName + "|" +
						// SearchSites.createStringFromDoubleArray(server.getAddressTokenWeights())
						// + "| | " +
						// SearchSites.createStringFromDoubleArray(server.getAddressTokenMissing())
						// + "|");
					} catch (Exception e) { // if there's an exception, we
											// simply use the pre-existing
											// weights
						e.printStackTrace();
					}
				}
				
				score = new BigDecimal(Math.max(score.doubleValue(), new BigDecimal(addressMatcher.matches(candAddress)).doubleValue()));

				if (score.compareTo(getThreshold()) < 0) {
					candAddress = getShortAddressFromDocument(row.getDocument());
					if (candAddress != null) {
						candAddress.addressElements[0] = null;
						candAddress.addressElements[1] = null;
						candAddress.addressElements[3] = null;
						candAddress.addressElements[4] = null;
						candAddress.addressElements[5] = null;
						candAddress.addressElements[6] = null;

						score = new BigDecimal(Math.max(score.doubleValue(), new BigDecimal(addressMatcher.matches(candAddress)).doubleValue()));
					}
				}
			}
		}
		return score;
	}

	private StandardAddress getShortAddressFromDocument(DocumentI documentI) {
		if(documentI == null) {
			return null;
		}
		StandardAddress address = new StandardAddress("");
		Set<PropertyI> properties = documentI.getProperties();
		for (PropertyI property : properties) {
			address.addressElements[2] = (property.getAddress().getStreetName() + " " + property.getAddress().getSuffix()).trim();
			break;
		}
		
		return address;
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
