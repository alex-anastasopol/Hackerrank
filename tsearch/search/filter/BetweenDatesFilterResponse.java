package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.NameI;

public class BetweenDatesFilterResponse extends FilterResponse {

	private static final long serialVersionUID = -729948961512671537L;

	private final Date fromDate;
	private final Date fromDateOrderUpdate;
	private final Date toDate;
	private TSServerInfoModule module;
	private int productType = 0;
	private boolean forceFromDateOrderUpdate = false;
	
	public BetweenDatesFilterResponse(long searchId) {
		super(searchId);	
		fromDate = Util.dateParser3(getSearchAttribute(SearchAttributes.FROMDATE));
		toDate = Util.dateParser3(getSearchAttribute(SearchAttributes.TODATE));
		setThreshold(new BigDecimal("0.90"));
		productType = getSearch().getProductId();
		if (Products.isOneOfUpdateProductType(productType)) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, - Products.UPDATE_NAME_ORDER_SEARCH_OFFSET_YEARS);
			fromDateOrderUpdate = cal.getTime();
		} else {
			fromDateOrderUpdate = fromDate;
		}
	}

	/**
	 * Creates a filter with default ToDate and forces the fromDate
	 * @param searchId
	 * @param fromDate the start of the interval accepted
	 */
	public BetweenDatesFilterResponse(long searchId, Date fromDate) {
		super(searchId);
		
		this.fromDate = fromDate;
		this.fromDateOrderUpdate = fromDate;
		
		this.toDate = Util.dateParser3(getSearchAttribute(SearchAttributes.TODATE));
		setThreshold(new BigDecimal("0.95"));
	}
	
	public BetweenDatesFilterResponse(long searchId, TSServerInfoModule module) {
		this(searchId);	
		this.module = module;
	}
		
	/**
	 * Stores docTypes which automatically pass
	 */
	private Set<String> ignoredDocTypes = new HashSet<String>();
	
	/**
	 * Stores docTypes which automatically pass
	 */
	private Set<String> ignoredServerDocTypes = new HashSet<String>();
	
	/**
	 * Add an ignored docType
	 * @param docType
	 */
	public void addIgnoredServerDocType(String docType){
		ignoredServerDocTypes.add(docType);
	}
	
	/**
	 * Add an ignored docType
	 * @param docType
	 */
	public void addIgnoredDocType(String docType){
		ignoredDocTypes.add(docType);
	}
		
	/**
	 * Tests if candidate is after or equal to reference
	 * @param cand
	 * @param ref
	 * @return
	 */
	private boolean isAfterOrEqual(Date cand, Date ref){
		if(cand == null || ref == null){
			return true;
		}
		return !cand.before(ref);
	}
	
	public TSServerInfoModule getModule() {
		return module;
	}

	public void setModule(TSServerInfoModule module) {
		this.module = module;
	}

	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		
		// pass if no sale data set
		if(row.getSaleDataSet() == null || row.getSaleDataSet().size() == 0){
			return ATSDecimalNumberFormat.ONE;
		}		
		SaleDataSet sds = (SaleDataSet)row.getSaleDataSet().elementAt(0);		
        
		// check docType
        if(ignoredDocTypes.size() != 0){
	        String docType = sds.getAtribute("DocumentType");      
	        if(!StringUtils.isEmpty(docType)){        
	        	
	        	for(String ignoredServerDocType: ignoredServerDocTypes){
	        		if(docType.equalsIgnoreCase(ignoredServerDocType)){
	        			return ATSDecimalNumberFormat.ONE;
	        		}
	        	}
	        	
	        	docType = DocumentTypes.getDocumentCategory(docType, searchId);
	        	for(String ignoredDocType: ignoredDocTypes){
	        		if(docType.equalsIgnoreCase(ignoredDocType)){
	        			return ATSDecimalNumberFormat.ONE;
	        		}
	        	}
	        	
	        }
        }  
        
        // check interval
        Date recDate = Util.dateParser3(sds.getAtribute("RecordedDate"));
        Date insDate = Util.dateParser3(sds.getAtribute("InstrumentDate"));
        boolean passed = true;
        
        // do not use date if in 1900
        if(insDate != null && insDate.getYear() != 0){
        	passed = isAfterOrEqual(insDate, getDateFromForFiltering()) && isAfterOrEqual(toDate, insDate); 
        } 
        if(!passed && recDate != null && recDate.getYear() != 0){
        	passed = isAfterOrEqual(recDate, getDateFromForFiltering()) && isAfterOrEqual(toDate, recDate);
        }
     
        return passed ? ATSDecimalNumberFormat.ONE : ATSDecimalNumberFormat.ZERO;
	}
	
	private Date getDateFromForFiltering() {
		if(isForceFromDateOrderUpdate() && Products.isOneOfUpdateProductType(productType)) {
			return fromDateOrderUpdate;
		}
		if(module != null && Products.isOneOfUpdateProductType(productType)) {
			ArrayList<ModuleStatesIterator> iterators = module.getModuleStatesItList();
			ConfigurableNameIterator nameIterator = null;
			if(iterators != null) {
				for (ModuleStatesIterator moduleStatesIterator : iterators) {
					if(moduleStatesIterator instanceof ConfigurableNameIterator) {
						nameIterator = (ConfigurableNameIterator)moduleStatesIterator;
						break;
					}
				}
			}
			if(nameIterator != null) {
				Object currentIterationObject = nameIterator.getCurrentStrategyItem();
				if (currentIterationObject instanceof NameI) {
					NameI nameSearched = (NameI) currentIterationObject;
					if(nameSearched.getNameFlags().isNewFromOrder() ) {
						return fromDateOrderUpdate;
					}
				}
			}
		}
		
		return fromDate;
	}
	
	@Override
	public String getFilterCriteria(){
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		Date fromDate = getDateFromForFiltering();
		String extra = "";
		if(fromDate != null && !this.fromDate.equals(fromDateOrderUpdate) && fromDate.equals(fromDateOrderUpdate)) {
			extra  = "([new name received in order] default ATS value -> current date - " + Products.UPDATE_NAME_ORDER_SEARCH_OFFSET_YEARS + " years)";
		}
		String from = (fromDate != null) ? sdf.format(fromDate)  + extra : "-";
		String to =  (toDate != null) ? sdf.format(toDate) : "-";
		String dt = (ignoredDocTypes.size() != 0) ? " excl doc types " + ignoredDocTypes : "";
		String dt1 = (ignoredDocTypes.size() != 0) ? ", excl server doc types " + ignoredServerDocTypes: "";
		return "Date between " + from + " and " + to + " " + dt + dt1;		  
	}
	
	/**
	 * Factory function
	 * @param searchId
	 * @return
	 */
	public static BetweenDatesFilterResponse getDefaultIntervalFilter(long searchId){
		BetweenDatesFilterResponse filter = new BetweenDatesFilterResponse(searchId);
		if(!InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().isUpdate()) {
			filter.addIgnoredDocType("PLAT");
			filter.addIgnoredDocType("RESTRICTION");
			filter.addIgnoredDocType("EASEMENT");
			filter.addIgnoredDocType("CCER");
			filter.addIgnoredServerDocType("TUBCARD");
		}
		filter.setThreshold(new BigDecimal("0.90"));
		return filter;
	}
	
	/**
	 * Factory function used to create a filter that knows the module it acts upon<br>
	 * Usually used for NameModule in order to filter name search different 
	 * @param searchId the id of the search
	 * @param module the module it acts upon
	 * @return a BetweenDatesFilterResponse filter fully configured
	 */
	public static BetweenDatesFilterResponse getDefaultIntervalFilter(long searchId, TSServerInfoModule module){
		BetweenDatesFilterResponse filter = new BetweenDatesFilterResponse(searchId, module);
		if(!InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().isUpdate()) {
			filter.addIgnoredDocType("PLAT");
			filter.addIgnoredDocType("RESTRICTION");
			filter.addIgnoredDocType("EASEMENT");
			filter.addIgnoredDocType("CCER");
			filter.addIgnoredServerDocType("TUBCARD");
		}
		filter.setThreshold(new BigDecimal("0.90"));
		return filter;
	}

	/**
	 * Checks is the flag forcing the use of <code>fromDateOrderUpdate</code> only in case of Update product is enabled
	 * @return <code>true</code> only if the flag is enabled
	 */
	public boolean isForceFromDateOrderUpdate() {
		return forceFromDateOrderUpdate;
	}

	/**
	 * Forces the use of <code>fromDateOrderUpdate</code> only in case of Update product
	 * @param forceFromDateOrderUpdate
	 * @return 
	 */
	public BetweenDatesFilterResponse setForceFromDateOrderUpdate(boolean forceFromDateOrderUpdate) {
		this.forceFromDateOrderUpdate = forceFromDateOrderUpdate;
		return this;
	}
	

}
