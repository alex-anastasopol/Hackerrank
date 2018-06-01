/**
 * 
 */
package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.response.InfSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/**
 *@author radu bacrau
 *
 *@deprecated  Use {RejectAlreadySavedDocumentsFilterResponse}
 */
@Deprecated
public class RejectAlreadyPresentFilterResponse extends FilterResponse {

	/**
	 * 
	 */
	static final long serialVersionUID = 10000000;
	
	/**
	 * @param searchId
	 */
	public RejectAlreadyPresentFilterResponse(long searchId) {
		super(searchId);
		setThreshold(new BigDecimal("0.95"));
	}

	/**
	 * @param key
	 * @param searchId
	 */
	public RejectAlreadyPresentFilterResponse(String key, long searchId) {
		super(key, searchId);
		setThreshold(new BigDecimal("0.95"));
	}

	public static String getRecordedYear(InfSet sds){
		String date = sds.getAtribute("RecordedDate");
		String year = StringUtils.extractParameter(date, "(\\d{4})$");
		return year;
	}
	
	/**
	 * @param sa1
	 * @param key
	 * @param searchId
	 */
	public RejectAlreadyPresentFilterResponse(SearchAttributes sa1, String key, long searchId) {
		super(sa1, key, searchId);
	}
	
	private boolean useInstr = true;
	private boolean useBookPage = true;
	private boolean useYearInstr = true;
	
	public void setUseYearInstr(boolean useYearInstr) {
		this.useYearInstr = useYearInstr;
	}

	public void setUseBookPage(boolean useBookPage) {
		this.useBookPage = useBookPage;
	}

	public void setUseInstr(boolean useInstr) {
		this.useInstr = useInstr;
	}
	
	
	@Override
	@SuppressWarnings("unchecked") // for Vector<PropertyIdentificationSet>
	public BigDecimal getScoreOneRow(ParsedResponse row) {
			
		double finalScore = 1.00d;
		if(row.getSaleDataSet().size() > 0){
			
			Search search = 
			InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			
			SaleDataSet sds = (SaleDataSet)row.getSaleDataSet().elementAt(0);			
			String inst = sds.getAtribute("InstrumentNumber");
			String book = sds.getAtribute("Book");	
			String page = sds.getAtribute("Page");
			String year = getRecordedYear(sds);			
			if( 
					(useInstr && search.hasSavedInst(inst) && (!StringUtils.isEmpty(inst))) || 
					(useBookPage && search.hasSavedBookPage(book, page)&& (!StringUtils.isEmpty(book))&& (!StringUtils.isEmpty(page))) || 
					(useYearInstr && search.hasSavedYearInst(year, inst)&& (!StringUtils.isEmpty(year))&& (!StringUtils.isEmpty(inst)))
					
			){
				finalScore = 0.00d;
			}
			
		}		
		return new BigDecimal(finalScore);
	}
	
	@Override
    public String getFilterName(){
    	return "Filter Out Existing Documents";
    }
    
	@Override
	public String getFilterCriteria(){
    	return "Already saved docs";
    }

}
