package ro.cst.tsearch.search.filter.newfilters.pin;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.property.PinI.PinType;

import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

public class PinFilterResponse extends FilterResponse {
	
	private boolean startWith = false;
	
	private boolean igNoreZeroes = false;
	
	private boolean igNoreStrartingZeroes = false;
	
	private boolean ignorePinForPreferentialDoctype = false;
	
	protected Set<String> preferentialDoctypes = new HashSet<String>(){
		private static final long serialVersionUID = 1L;
	{
		add(DocumentTypes.PLAT);
		add(DocumentTypes.EASEMENT);
		add(DocumentTypes.RESTRICTION);
	}};
	
	/**
	 * Serialization 
	 */
	private static final long serialVersionUID = 5643403685740313495L;
	
	/**
	 * Main log category.
	 */
	protected static final Category logger = Logger.getLogger(PinFilterResponse.class);
	
	/**
	 * Details log category.
	 */
	protected static final Category loggerDetails = Logger.getLogger(Log.DETAILS_PREFIX + PinFilterResponse.class.getName());
	/**
	 * Parcel number.
	 */
	protected Set<String> parcelNumber;


	/**
	 * Constructor
	 * 
	 * @param instrumentlNumber Parcel number to be matched.
	 */
	public PinFilterResponse(String saKey, long searchId) {
		super(saKey, searchId);
		threshold = new BigDecimal("0.90");
	}
	
	/**
	 * Obtain candidate PINs 
	 * @param row
	 * @return candidate PINs
	 */
	@SuppressWarnings("unchecked")
	protected Set<String> getCandPins(ParsedResponse row){
		
		Set<String> pins = new HashSet<String>();
		
		
		if(row.getPropertyIdentificationSet() == null || row.getPropertyIdentificationSetCount() == 0){
			
			if(row.getDocument() != null) {
				DocumentI document = row.getDocument();
				Set<PropertyI> properties = document.getProperties();
				if(properties != null) {
					for (PropertyI propertyI : properties) {
						String pin = propertyI.getPin(PinType.PID);
						if(!StringUtils.isEmpty(pin)) {
							pins.add(pin.trim());
						}
					}
				}
			}
			
			return pins;
		}
		
		for(PropertyIdentificationSet pis: (Vector<PropertyIdentificationSet>)row.getPropertyIdentificationSet()){
			String pin = pis.getAtribute("ParcelID");
			if(!StringUtils.isEmpty(pin)){
				pins.add(pin.trim());
			}
		}
		
		return pins;
	}
	
	/**
	 * Obtain reference PIN
	 * @return
	 */
	protected Set<String> getRefPin(){
		if(parcelNumber!=null){
			return parcelNumber;
		}
		Set<String> ret = new HashSet<String>();
		String saParcel = sa.getAtribute(saKey).trim();
		if(org.apache.commons.lang.StringUtils.isNotBlank(saParcel)) {
			ret.addAll(Arrays.asList(saParcel.split(",")));
		}
		return ret;
	}
	
	/**
	 * Score response for parcel number.
	 * 
	 * @see ro.cst.tsearch.search.filter.FilterResponse#getScoreOneRow(ro.cst.tsearch.servers.response.ParsedResponse)
	 */
	public BigDecimal getScoreOneRow(ParsedResponse row){
		double score = getScoreOneRowInternal(row);
		loggerDetails.debug("match [" + getRefPin() + "] vs [" + getCandPins(row) + "] score=" + score);		
        IndividualLogger.info( "match [" + getRefPin()+ "] vs [" + getCandPins(row) + "] score=" + score,searchId );
		return new BigDecimal(score);
	}
	
	/**
	 * Expand a set of pins to also contain the number-and-letter-only version
	 * @param input
	 * @return
	 */
	private Set<String> expand(Set<String> input){
		Set<String> output = new HashSet<String>();
		for(String val: input){
			val = val.trim().toLowerCase();
			output.add(val);
			if(igNoreZeroes){
				output.add(val.replaceAll("(?i)[^A-Z0-9]+", "").replaceAll("[0]+", ""));
			}
			else if(igNoreStrartingZeroes){
				output.add(val.replaceAll("(?i)[^A-Z0-9]+", "".replaceAll("^[0]+", "")));
			}else{
				output.add(val.replaceAll("(?i)[^A-Z0-9]+", ""));
			}
		}
		return output;
	}
	
	/**
	 * Compute score for one row only 
	 * @param row
	 * @return
	 */
	private double getScoreOneRowInternal(ParsedResponse row){
		
		// no reference - score 1
		Set<String> refPins = getRefPin();
		if(allEmpty(refPins)){
			return 1.0d;
		}

		// no candidate - score 1
		Set<String> candPins = getCandPins(row);
		if(candPins.size() ==0 ){
			return 1.0d;
		}		
		
		// expand ref and cand
		refPins = expand(refPins);
		candPins = expand(candPins);
		
		candPins = prepareCandPin(candPins);
		if (ignorePinForPreferentialDoctype){
        	String serverDoctype = null;
        	try {
        		serverDoctype = ((ro.cst.tsearch.servers.response.SaleDataSet)row.getSaleDataSet().get(0)).getAtribute("DocumentType").trim();
        		serverDoctype = DocumentTypes.getDocumentCategory(serverDoctype, searchId);
        	} catch (Exception e) {}
        	if(StringUtils.isNotEmpty(serverDoctype) && preferentialDoctypes.contains(serverDoctype)) {
        		return 1.0d;
        	}
        } 
        
        if(startWith){
			// see if we have a match
			for(String ref: refPins){
				for(String candstr:candPins){
					if(ref.startsWith(candstr)||candstr.startsWith(ref)){
						return 1.0d;
					}
				}
			}
		}
		else{
			// see if we have a match
			for(String ref: refPins){
				if(candPins.contains(ref)){
					return 1.0d;
				}
			}
			
			for(String candStr:candPins){
				if(refPins.contains(candStr)){
					return 1.0d;
				}
			}	
		}
		
		// nothing found - score 0
		return 0.0d;
		
	}
	
	public static boolean allEmpty(Set<String> refPins) {
		for(String s:refPins){
			if(!StringUtils.isEmpty(s)){
				return false;
			}
		}
		return true;
	}

	protected Set<String> prepareCandPin(Set<String> candPins){
		
		return candPins;
	}

	@Override
    public String getFilterName(){
    	return "Filter by PIN";
    }
	
	@Override
	public String getFilterCriteria(){
		Set<String> refPin = getRefPin();
		if(refPin.isEmpty()) {
			return "PIN='No PIN Available To Test - All Documents Pass'"; 
		}
		String ret = "PIN=";
		for(String s:refPin){
			ret+= s+", ";
		}
		if(ret.endsWith(", ")){
			ret = ret.substring(0,ret.length()-1);
		}
		
		if (ignorePinForPreferentialDoctype){
			ret += " (Ignore PIN for doctypes: " + preferentialDoctypes.toString() + ")";
		}
	
		
		return ret;
	}

	public boolean isStartWith() {
		return startWith;
	}

	public void setStartWith(boolean startWith) {
		this.startWith = startWith;
	}

	public boolean isIgNoreZeroes() {
		return igNoreZeroes;
	}

	public void setIgNoreZeroes(boolean igNoreZeroes) {
		this.igNoreZeroes = igNoreZeroes;
	}
	
	public Set<String> getParcelNumber() {
		return parcelNumber;
	}

	public void setParcelNumber(Set<String> parcelNumber) {
		this.parcelNumber = parcelNumber;
	}
	
	public boolean isIgNoreStrartingZeroes() {
		return igNoreStrartingZeroes;
	}

	public void setIgNoreStrartingZeroes(boolean igNoreStrartingZeroes) {
		this.igNoreStrartingZeroes = igNoreStrartingZeroes;
	}
	
	public boolean getIgnorePinForPreferentialDoctype() {
		return ignorePinForPreferentialDoctype;
	}

	public void setIgnorePinPreferentialDoctype(boolean ignorePinForPreferentialDoctype) {
		this.ignorePinForPreferentialDoctype = ignorePinForPreferentialDoctype;
	}
}
