package ro.cst.tsearch.search.filter.newfilters.legal;

import java.math.BigDecimal;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.StringUtils;

public class MunicipalityFilterResponse extends FilterResponse {

	private static final long serialVersionUID = 1476102816337472631L;

	public MunicipalityFilterResponse(long searchId) {
		super(searchId);
		setThreshold(new BigDecimal("0.80"));
	}

	protected String getReferenceMunicipality(){
		return sa.getAtribute(SearchAttributes.P_MUNICIPALITY);
	}
	
	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		
		String refMun = getReferenceMunicipality();
		if(StringUtils.isEmpty(refMun)){
			return ATSDecimalNumberFormat.ONE;
		}
		if(row.getPropertyIdentificationSetCount() == 0){
			return ATSDecimalNumberFormat.ONE;
		}
		
		boolean foundSomething = false;
		double score = 0.0d;
		for(int i=0; i<row.getPropertyIdentificationSetCount(); i++){
			PropertyIdentificationSet pis = row.getPropertyIdentificationSet(i);
			String candMun = pis.getAtribute("MunicipalJurisdiction");
			if(StringUtils.isEmpty(candMun)){
				continue;
			}
			foundSomething = true;			
			candMun = candMun.toLowerCase();
			refMun = refMun.toLowerCase();
			double crtScore = 0.0d;
			if(refMun.contains(candMun) || candMun.contains(refMun)){
				crtScore = 1.0d;
			} else {
				crtScore = GenericNameFilter.computeScoreForStrings(candMun, refMun);
			}
			if(crtScore > score){
				score = crtScore;
			}
		}
		
		if(foundSomething){
			return new BigDecimal(score);
		} else {
			return ATSDecimalNumberFormat.ONE;
		} 
	}

	@Override
	public String getFilterCriteria() {
		return "Municipality='" + getReferenceMunicipality() + "'";
	}

	@Override
	public String getFilterName() {
		return super.getFilterName();
	}

	
}
