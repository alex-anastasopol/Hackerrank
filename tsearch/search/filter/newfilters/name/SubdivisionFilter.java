package ro.cst.tsearch.search.filter.newfilters.name;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Set;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.Name;

public class SubdivisionFilter extends GenericNameFilter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private boolean loadFromAdditionalInfo = true;
	private boolean loadFromSearchPage = true;
	private boolean loadFromOrder = true;
	private String additionalInfoKey = AdditionalInfoKeys.SUBDIVISION_NAME_SET;
	
	
	public SubdivisionFilter(long searchId) {
		super(searchId);
		super.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
		super.setThreshold(new BigDecimal(NameFilterFactory.NAME_FILTER_THRESHOLD));
		setUseSubdivisionNameAsReference(true);
		setIgnoreSufix(false);
		setUseSubdivisionNameAsCandidat(true);
		setThreshold(new BigDecimal(NameFilterFactory.NAME_FILTER_THRESHOLD_FOR_SUBDIVISION));
		setInitAgain(true);
	}
	
	public boolean isLoadFromAdditionalInfo() {
		return loadFromAdditionalInfo;
	}

	public void setLoadFromAdditionalInfo(boolean loadFromAdditionalInfo) {
		this.loadFromAdditionalInfo = loadFromAdditionalInfo;
	}

	public boolean isLoadFromSearchPage() {
		return loadFromSearchPage;
	}

	public void setLoadFromSearchPage(boolean loadFromSearchPage) {
		this.loadFromSearchPage = loadFromSearchPage;
	}

	public boolean isLoadFromOrder() {
		return loadFromOrder;
	}

	public void setLoadFromOrder(boolean loadFromOrder) {
		this.loadFromOrder = loadFromOrder;
	}

	@SuppressWarnings("unchecked")
	public void init(){
		
		Search search = getSearch();
		
		setRef.clear();
		companyNameRef.clear();
		if(isLoadFromAdditionalInfo()) {
			Object additionInfoContent = search.getAdditionalInfo(additionalInfoKey);
			if(additionInfoContent != null) {
				if(additionInfoContent instanceof Set) {
					for (Iterator<String> iterator = ((Set<String>) additionInfoContent).iterator(); 
							iterator.hasNext();) {
						String subdivName = iterator.next();
						if(!StringUtils.isEmpty(subdivName)){
							setRef.add(new Name("","", subdivName.toUpperCase() ));
							companyNameRef.add(subdivName.toUpperCase());
						}
					}
				}
			}
		}
		
		if(isLoadFromOrder()) {
			String subdivName = search.getSa().getValidatedSubdivisionName();
			if(StringUtils.isNotEmpty(subdivName)) {
				setRef.add(new Name("","", subdivName.toUpperCase() ));
				companyNameRef.add(subdivName.toUpperCase());
			}
		}
		
		if(isLoadFromSearchPage()) {
			String subdivName = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_NAME);
			if(StringUtils.isNotEmpty(subdivName)) {
				setRef.add(new Name("","", subdivName.toUpperCase() ));
				companyNameRef.add(subdivName.toUpperCase());
			}
		}	
	}
	

}
