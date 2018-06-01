package ro.cst.tsearch.search.filter.newfilters.misc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.property.PropertyI;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class DistrictFilter extends FilterResponse {

	private static final long serialVersionUID = 1L;

	public DistrictFilter(long searchId) {
		super(searchId);
		setThreshold(ATSDecimalNumberFormat.ONE);
	}

	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		Set<String> candDistSet = getCandidateDistricts(row);
		candDistSet = clean(candDistSet);
		if(allEmpty(candDistSet)){
			return ATSDecimalNumberFormat.ONE;
		}
		
		Set<String> refDistSet = getRefDistricts();
		refDistSet = clean(refDistSet);
		if(allEmpty(refDistSet)){
			return ATSDecimalNumberFormat.ONE;
		}
		
		for(String candStr : candDistSet) {
			if(refDistSet.contains(candStr)) {
				return ATSDecimalNumberFormat.ONE;
			}
		}	
		
		return ATSDecimalNumberFormat.ZERO;
	}
	
	private Set<String> getRefDistricts() {
		Set<String> districts = new HashSet<String>();
		districts.addAll(Arrays.asList(sa.getAtribute(SearchAttributes.LD_DISTRICT).trim().split(",")));
		return districts;
	}
	
	@SuppressWarnings("unchecked")
	private Set<String> getCandidateDistricts(ParsedResponse row) {
		Set<String> districts = new HashSet<String>();
		
		if(row.getPropertyIdentificationSet() != null && row.getPropertyIdentificationSetCount() > 0) {
			for(PropertyIdentificationSet pis: (Vector<PropertyIdentificationSet>)row.getPropertyIdentificationSet()){
				String distr = pis.getAtribute("District");
				if(!StringUtils.isEmpty(distr)){
					districts.add(distr.trim());
				}
			}
		}
		
		if(!districts.isEmpty()) {
			return districts;
		}

		if(row.getDocument() != null) {
			DocumentI document = row.getDocument();
			Set<PropertyI> properties = document.getProperties();
			if(properties != null) {
				for (PropertyI propertyI : properties) {
					String distr = propertyI.getDistrict();
					if(!StringUtils.isEmpty(distr)) {
						districts.add(distr.trim());
					}
				}
			}
		}
		
		return districts;
	}
	
	private static boolean allEmpty(Set<String> refDistSet) {
		for(String s : refDistSet){
			if(!StringUtils.isEmpty(s)){
				return false;
			}
		}
		return true;
	}
	
	private Set<String> clean(Set<String> input){
		Set<String> output = new HashSet<String>();
		for(String val: input){
			val = val.replaceFirst("^0+", "").trim();
			try {
				val = "" + Roman.parseRoman(val);
			} catch(Exception e) {
				// not a roman number
			}
			if(StringUtils.isNotEmpty(val)) {
				output.add(val);
			}
		}
		return output;
	}
	
	@Override
	public String getFilterName() {
		return "Filter by District";
	}
	
	@Override
	public String getFilterCriteria() {
		String ret = "District='";
		for(String s : getRefDistricts()){
			ret += s + ", ";
		}
		ret = ret.replaceFirst(", $", "'");
		
		return ret;
	}
}
