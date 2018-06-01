/**
 * 
 */
package ro.cst.tsearch.search.module;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.property.PinI.PinType;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SavedFromType;

/**
 * @author mihaib
 * 
 */ 
public class MultipleYearIterator extends GenericRuntimeIterator<Integer> {
	
	public static final long serialVersionUID = 1000L;
	private int maxYearsAllowed = 1;
	protected int currentTaxYear = -1;
	
	/**
	 * @param searchId
	 * @param maxYearsAllowed
	 */
	public MultipleYearIterator(long searchId, int maxYearsAllowed, int currentTaxYear) {
		super(searchId);
		this.searchId = searchId;
		this.maxYearsAllowed = maxYearsAllowed;
		this.currentTaxYear = currentTaxYear;
	}
	
	/**
	 * @param searchId
	 */
	public MultipleYearIterator(long searchId) {
		super(searchId);
		this.searchId = searchId;
	}	

	@Override
	public List<Integer> createDerrivations() {
		
		List<Integer> derivYears = new LinkedList<Integer>();
		List<Integer> yearsAlreadySaved = new LinkedList<Integer>();
		int yearRef = Calendar.getInstance().get(Calendar.YEAR);
		TSServerInfoModule initial = this.getInitial();
		
		Search global = getSearch();
		if (global != null){
			String parcelNO = global.getSa().getAtribute(SearchAttributes.LD_PARCELNO);
			
			DType dType = null;
			DataSite datasite = global.getCrtDataSite(global.isParentSiteSearch());
			if (datasite.isAssessorSite()){
				dType = DType.ASSESOR;
			} else if (datasite.isCountyTaxLikeSite()){
				dType = DType.TAX;
			} if (datasite.isCityTaxLikeSite()){
				dType = DType.CITYTAX;
			}
			
			DocumentsManagerI manager = global.getDocManager();
			try{
				manager.getAccess();
				List<DocumentI> list = manager.getDocumentsWithType(true, dType);
				for (DocumentI document : list) {
					if (document.isSavedFrom(SavedFromType.AUTOMATIC)){
						Set<PropertyI> properties = document.getProperties();
						if (properties != null) {
							for (PropertyI propertyI : properties) {
								String pin = propertyI.getPin(PinType.PID);
								if (StringUtils.isNotEmpty(pin) && pin.equals(parcelNO)){
									int year = document.getYear();
									if (year != SimpleChapterUtils.UNDEFINED_YEAR){
										yearsAlreadySaved.add(year);
									}
								}
							}
						}
					}
				}
			}
			finally {
				manager.releaseAccess();
			}
		}
			
		for (TSServerInfoFunction tssFunc : initial.getFunctionList()) {
			if (SearchAttributes.CURRENT_TAX_YEAR.equals(tssFunc.getSaKey())){
				String year = tssFunc.getParamValue();
				if (StringUtils.isNotEmpty(year)){
					try {
						yearRef = Integer.parseInt(year);
						if (!yearsAlreadySaved.contains(yearRef)){
							derivYears.add(yearRef);
						}
					} catch (Exception e) {
						logger.error("Error loading year from module ", e);
					}
					break;
				} else if (currentTaxYear != -1){
					if (!yearsAlreadySaved.contains(currentTaxYear)){
						derivYears.add(currentTaxYear);
					}
					break;
				} else{
					if (!yearsAlreadySaved.contains(currentTaxYear)){
						derivYears.add(yearRef);
					}
					break;
				}
			}
		}
		
		if (maxYearsAllowed > 1){
			for (int i = 1; i < maxYearsAllowed; i++){
				yearRef--;
				if (!yearsAlreadySaved.contains(yearRef)){
					derivYears.add(yearRef);
				}
			}
		}
		return derivYears;
	}
	
	@Override
	protected void loadDerrivation(TSServerInfoModule module, Integer year) {
		
		for (Object functionObject : module.getFunctionList()) {
			if (functionObject instanceof TSServerInfoFunction) {
				TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
				if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR) {
					function.setParamValue(year + "");
				}
			}
		}
	}
	
}
