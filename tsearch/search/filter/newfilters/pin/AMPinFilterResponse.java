package ro.cst.tsearch.search.filter.newfilters.pin;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/**
*
* filter for the following counties from NV: Clark (including City of North Las Vegas, City of Las Vegas and City of Henderson), 
* Douglas and Washoe (including City of Reno)
* 
* the filter takes into consideration the length of the PIN
*
*/

public class AMPinFilterResponse extends PinFilterResponse {
	
	private static final long serialVersionUID = 1029384756L;
	
	private static final int CLARK_PIN_LENGTH = 11;
	private static final int DOUGLAS_PIN_LENGTH = 12;
	private static final int WASHOE_PIN_LENGTH = 8;
	
	private String crtCounty;
	private int crt_county_pin_length = 0;
	
	public AMPinFilterResponse(String saKey, long searchId) {
		super(saKey, searchId);
		crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();;
		if (crtCounty.toLowerCase().equals("clark")) crt_county_pin_length = CLARK_PIN_LENGTH;
		else if (crtCounty.toLowerCase().equals("douglas")) crt_county_pin_length = DOUGLAS_PIN_LENGTH;
		else if (crtCounty.toLowerCase().equals("washoe")) crt_county_pin_length = WASHOE_PIN_LENGTH;
	}
	
	@Override
	public String getFilterCriteria(){
		return "PIN length=" + crt_county_pin_length + " (if the PIN contains the \"_\" character, the part of PIN from the beginning to this character)";
	}
	
	@SuppressWarnings("unchecked")
	protected Set<String> getCandPins(ParsedResponse row){
		
		Set<String> pins = new HashSet<String>();
		
		if(row.getPropertyIdentificationSet() == null || row.getPropertyIdentificationSetCount() == 0){
			return pins;
		}
		
		//the PIN is a concatenation of Parcel # , "_" and District #
		//but the filter takes into consideration only Parcel # value 
		for(PropertyIdentificationSet pis: (Vector<PropertyIdentificationSet>)row.getPropertyIdentificationSet()){
			String pin = pis.getAtribute("ParcelID");
			if(!StringUtils.isEmpty(pin)){
				int index = pin.indexOf("_");
				if (index>-1) pin = pin.substring(0, index);
				pins.add(pin.trim());
			}
		}
		
		return pins;
	}
	
	public BigDecimal getScoreOneRow(ParsedResponse row){
		double score = getScoreOneRowInternal(row);
		loggerDetails.debug("match [" + getRefPin() + "] vs [" + getCandPins(row) + "] score=" + score);		
        IndividualLogger.info( "match [" + getRefPin()+ "] vs [" + getCandPins(row) + "] score=" + score,searchId );
		return new BigDecimal(score);
	}
	
	private double getScoreOneRowInternal(ParsedResponse row){
		Set<String> candPins = getCandPins(row);
		if (candPins.size()!=1) return 0.0d;		//a row has only a PIN
		Iterator<String> it = candPins.iterator();
		String pin = it.next();
		if (pin.length()==crt_county_pin_length) return 1.0d; 
		return 0.0d;
	}
}
