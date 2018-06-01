/*
 * Created on Jun 2, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.module;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.tokenlist.NameTokenList;
import ro.cst.tsearch.search.tokenlist.TokenList;
import ro.cst.tsearch.search.tokenlist.iterator.NameTokenListIterator;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.MostCommonName;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PinI.PinType;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SavedFromType;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class NameModuleStatesIterator extends ModuleStatesIterator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected static final Category logger = Logger.getLogger(NameModuleStatesIterator.class);
	
	private boolean addMCNDefault = false;
	private int scoreForNameSearch ;
	private int scoreForCommonNameSearch ;

	private String companyTypeName = "";

	private String personalTypeName = "";
	
	protected int numberOfYearsAllowed = 1;
	
	public NameModuleStatesIterator(long searchId) {
		super(searchId);
	}

	protected void initInitialState(TSServerInfoModule initial) {
		super.initInitialState(initial);
	}

	protected void setupStrategy() {
		NameTokenList owner = new NameTokenList("", "", "");
		NameTokenList spouse = new NameTokenList("", "", ""); 
		NameTokenListIterator si = new NameTokenListIterator(owner, spouse);
		si.setLastNameNotEmpty(true);
		si.init();
		setStrategy(si);
	}

	public Object current() {
		Search s = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		s.clearRecursiveAnalisedLinks();
		if (s.getSearchType() == Search.GO_BACK_ONE_LEVEL_SEARCH){
			s.clearVisitedLinks();
		}
		Object currentObject = getStrategy().current();
		TSServerInfoModule crtState = new TSServerInfoModule(initialState);
		boolean hasBusinessAndPersonField=false;
		String last = "";
		String first = "";
		String middle = "";
		String ssn4 = "";
		
		boolean hasSsn = false;
		for (int i = 0; i < crtState.getFunctionCount(); i++) {
			TSServerInfoFunction fct = crtState.getFunction(i);
			int iteratorType = fct.getIteratorType();
			switch(iteratorType){
				case FunctionStatesIterator.ITERATOR_TYPE_SSN:
					hasSsn = true;
					break;
			}
			if(hasSsn) {
				break;
			}
		}
		
		boolean isCompany = false; 
		NameI currentObjectNameI = null;
		int numberToCutFromYear = 0;
		
		if (currentObject instanceof Name) {
			currentObjectNameI = (Name) currentObject;
			last = currentObjectNameI.getLastName();
			first = currentObjectNameI.getFirstName();
			middle = currentObjectNameI.getMiddleName();
			if(hasSsn) {
				ssn4 = currentObjectNameI.getSsn4Decoded();
			}
			isCompany = currentObjectNameI.isCompany();
		} else if (currentObject instanceof CombinedNameIterator.CombinedObject) {
			currentObjectNameI = ((CombinedNameIterator.CombinedObject) currentObject).getNameObject();
			
			last = currentObjectNameI.getLastName();
			first = currentObjectNameI.getFirstName();
			middle = currentObjectNameI.getMiddleName();
			if(hasSsn) {
				ssn4 = currentObjectNameI.getSsn4Decoded();
			}
			isCompany = currentObjectNameI.isCompany();
			
			numberToCutFromYear = ((CombinedNameIterator.CombinedObject) currentObject).getNumberToCutFromYear();
			
			List<Integer> yearsAlreadySaved = new LinkedList<Integer>();
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
			for (int i = 0; i < crtState.getFunctionCount(); i++) {
				TSServerInfoFunction fct = crtState.getFunction(i);
				int itType = fct.getIteratorType();
				if (itType == FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR){
					
					String year = fct.getParamValue();
					if (StringUtils.isNotEmpty(year)){
						int yearInt = Integer.parseInt(year);
						if (yearsAlreadySaved.contains(yearInt - numberToCutFromYear)){
							numberToCutFromYear = 0;
						}
						fct.setParamValue(Integer.toString(yearInt - numberToCutFromYear));
						break;
					}	
				}
			}
			
		} else {
			NameTokenList ntl = (NameTokenList) currentObject;
			last = TokenList.getString(ntl.getLastName());
			first = TokenList.getString(ntl.getFirstName());
			middle = TokenList.getString(ntl.getMiddleName());
		}
		for (int i = 0; i < crtState.getFunctionCount(); i++) {
			TSServerInfoFunction fct = crtState.getFunction(i);
			int itType = fct.getIteratorType();
			if ( (itType==FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME)&&("".equals(first)||first==null)&&("".equals(middle)||middle==null)){
				fct.setParamValue(last);
				hasBusinessAndPersonField=true;
				break;	
			}
		}
		
		String ssn4Encoded = "";
		if (currentObjectNameI!=null) {
			ssn4Encoded = currentObjectNameI.getSsn4Encoded();
		}
		
		isCompany = isCompany || (StringUtils.isEmpty(first)&&StringUtils.isEmpty(middle)&&StringUtils.isEmpty(ssn4Encoded)&&!StringUtils.isEmpty(last));
		
		if(!hasBusinessAndPersonField){		
				for (int i = 0; i < crtState.getFunctionCount(); i++) {
					TSServerInfoFunction fct = crtState.getFunction(i);
					int iteratorType = fct.getIteratorType();
					switch(iteratorType){
						case FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE:
							fct.setParamValue(last);
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE:
							if(!isCompany){
								fct.setParamValue(middle);
							} else {
								fct.setParamValue("");
							}
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE:
							setFirstNameOnFunction(first, isCompany, fct);
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE:
							String val = last + " " + first;
							fct.setParamValue(val.trim());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE:
							
							String value = "";
							if(!StringUtils.isEmpty(last)){ value += last + " "; }
							if(!StringUtils.isEmpty(first)){ value += first + " "; }
							if(!StringUtils.isEmpty(middle)){ value += middle; }
							fct.setParamValue(value.trim());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_LCF_NAME_FAKE:
							if(!first.equals("")){
								fct.setParamValue(last + ", " + first);
							}else{
								fct.setParamValue(last);
							}				
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_FML_NAME_FAKE:
							
							String fmlValue = "";
							if(!StringUtils.isEmpty(first)){ fmlValue += first + " "; }
							if(!StringUtils.isEmpty(middle)){ fmlValue += middle + " "; }
							if(!StringUtils.isEmpty(last)){ fmlValue += last; }
							fct.setParamValue(fmlValue.trim());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_SCORE:
							if( MostCommonName.isMCLastName( last ) ){
								fct.setParamValue( getScoreForCommonNameSearch()+"" );
							}
							else{
								fct.setParamValue( getScoreForNameSearch()+"" );
							}
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_SSN:
							fct.setParamValue(ssn4);
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_OE_RESULT_TYPE:
							fct.setParamValue(getSearchAttribute(SearchAttributes.LD_PARCELNO_MAP));
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE:
							if(isCompany){
								fct.setParamValue(getCompanyTypeName());
							}else{
								fct.setParamValue(getPersonalTypeName());
							}
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE:
							if(currentObjectNameI != null && 
									currentObjectNameI.getNameFlags().isNewFromOrder() && 
									s.isSearchProductTypeOfUpdate()) {
								String saKey = fct.getSaKey();
								Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
								DocumentsManagerI documentsManagerI = search.getDocManager();
								if(SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY.equals(saKey)) {
									Calendar cal = Calendar.getInstance();
									cal.add(Calendar.YEAR, - Products.UPDATE_NAME_ORDER_SEARCH_OFFSET_YEARS);
									fct.setSaKey("");
									fct.setParamValue(new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(cal.getTime()));
									forceStartViewDate(documentsManagerI, cal.getTime());
								} else if(SearchAttributes.FROMDATE_MM_DD_YYYY.equals(saKey)) {
									Calendar cal = Calendar.getInstance();
									cal.add(Calendar.YEAR, - Products.UPDATE_NAME_ORDER_SEARCH_OFFSET_YEARS);
									fct.setSaKey("");
									fct.setParamValue(FormatDate.getDateFormat(FormatDate.PATTERN_MMddyyyy).format(cal.getTime()));
									forceStartViewDate(documentsManagerI, cal.getTime());
								} else if(SearchAttributes.FROMDATE_MM_DD_YY.equals(saKey)) {
									Calendar cal = Calendar.getInstance();
									cal.add(Calendar.YEAR, - Products.UPDATE_NAME_ORDER_SEARCH_OFFSET_YEARS);
									fct.setSaKey("");
									fct.setParamValue(FormatDate.getDateFormat(FormatDate.PATTERN_MMddyy).format(cal.getTime()));
									forceStartViewDate(documentsManagerI, cal.getTime());
								} else if(SearchAttributes.FROMDATE_MMM_DD_YYYY.equals(saKey)) {
									Calendar cal = Calendar.getInstance();
									cal.add(Calendar.YEAR, - Products.UPDATE_NAME_ORDER_SEARCH_OFFSET_YEARS);
									fct.setSaKey("");
									fct.setParamValue(FormatDate.getDateFormat(FormatDate.PATTERN_MMMddcyyyy).format(cal.getTime()));
									forceStartViewDate(documentsManagerI, cal.getTime());
								} else if(SearchAttributes.FROMDATE.equals(saKey)) {
									Calendar cal = Calendar.getInstance();
									cal.add(Calendar.YEAR, - Products.UPDATE_NAME_ORDER_SEARCH_OFFSET_YEARS);
									fct.setSaKey("");
									fct.setParamValue(SearchAttributes.DEFAULT_DATE_PARSER.format(cal.getTime()));
									forceStartViewDate(documentsManagerI, cal.getTime());
								} else if(SearchAttributes.FROMDATE_DD.equals(saKey)) {
									Calendar cal = Calendar.getInstance();
									cal.add(Calendar.YEAR, - Products.UPDATE_NAME_ORDER_SEARCH_OFFSET_YEARS);
									fct.setSaKey("");
									fct.setParamValue(FormatDate.getDateFormat(FormatDate.PATTERN_DD).format(cal.getTime()));
									forceStartViewDate(documentsManagerI, cal.getTime());
								} else if(SearchAttributes.FROMDATE_MM.equals(saKey)) {
									Calendar cal = Calendar.getInstance();
									cal.add(Calendar.YEAR, - Products.UPDATE_NAME_ORDER_SEARCH_OFFSET_YEARS);
									fct.setSaKey("");
									fct.setParamValue(FormatDate.getDateFormat(FormatDate.PATTERN_MM).format(cal.getTime()));
									forceStartViewDate(documentsManagerI, cal.getTime());
								} else if(SearchAttributes.FROMDATE_YEAR.equals(saKey)) {
									Calendar cal = Calendar.getInstance();
									cal.add(Calendar.YEAR, - Products.UPDATE_NAME_ORDER_SEARCH_OFFSET_YEARS);
									fct.setSaKey("");
									fct.setParamValue(FormatDate.getDateFormat(FormatDate.PATTERN_yyyy).format(cal.getTime()));
									forceStartViewDate(documentsManagerI, cal.getTime());
								} else if(SearchAttributes.LAST_REAL_TRANSFER_DATE_MMDD.equals(saKey)
										|| SearchAttributes.LAST_REAL_TRANSFER_DATE_YYYY.equals(saKey)
										|| SearchAttributes.LAST_TRANSFER_DATE_MMDDYYYY.equals(saKey)
										|| SearchAttributes.LIEN_DATE_MMDD.equals(saKey)
										|| SearchAttributes.LIEN_DATE_YYYY.equals(saKey)) {
									Date fromDate = null;
									if(SearchAttributes.LAST_REAL_TRANSFER_DATE_MMDD.equals(saKey)
											|| SearchAttributes.LAST_REAL_TRANSFER_DATE_YYYY.equals(saKey)
											|| SearchAttributes.LAST_TRANSFER_DATE_MMDDYYYY.equals(saKey)) {
										
										try {
											documentsManagerI.getAccess();
											TransferI transfer = documentsManagerI.getLastRealTransfer();
											if(transfer != null) {
												fromDate = transfer.getRecordedDate();
											}
										} catch (Exception e) {
											e.printStackTrace();
										} finally {
											documentsManagerI.releaseAccess();
										}
									} else {
										fromDate = search.getTsrViewFilterForProduct(Products.LIENS_PRODUCT);
									}
									
									
									Calendar cal = Calendar.getInstance();
									cal.add(Calendar.YEAR, - Products.UPDATE_NAME_ORDER_SEARCH_OFFSET_YEARS);
									
									if(fromDate != null) {
										if(fromDate.before(cal.getTime())) {
											fromDate = cal.getTime();
										}
									} else {
										fromDate = cal.getTime();
									}
									
									fct.setSaKey("");
									SimpleDateFormat sdf = null;
									if(SearchAttributes.LAST_TRANSFER_DATE_MMDDYYYY.equals(saKey)) {
										sdf = SearchAttributes.DATE_FORMAT_MMddyyyy;
									} else if(SearchAttributes.LAST_REAL_TRANSFER_DATE_MMDD.equals(saKey)
											|| SearchAttributes.LIEN_DATE_MMDD.equals(saKey)) {
										sdf = new SimpleDateFormat( "MM/dd" );
									} else {
										sdf = new SimpleDateFormat( "yyyy" );
									}
									fct.setParamValue(sdf.format(fromDate));
									forceStartViewDate(documentsManagerI, fromDate);
								}
							}
							break;
					}
		  }	
		}
		
		return crtState;
	}

	public void setFirstNameOnFunction(String firstName, boolean isCompany,
			TSServerInfoFunction fct) {
		if(!isCompany){
			fct.setParamValue(firstName);
		} else {
			fct.setParamValue("");
		}
	}

	/**
	 * Force start view date in TSRIndex in case it is not manually modified
	 * @param documentsManagerI
	 * @param newDate
	 */
	private void forceStartViewDate(DocumentsManagerI documentsManagerI, Date newDate) {
		if(documentsManagerI == null) {
			return;
		}
		documentsManagerI.getAccess();
		try {
			if(documentsManagerI.isFieldModified(DocumentsManager.Fields.START_VIEW_DATE)) {
				return ;
			}
			if(documentsManagerI.getStartViewDate().after(newDate)) {
				documentsManagerI.setStartViewDate(newDate);
			}
		} finally {
			documentsManagerI.releaseAccess();
		}
		
	}

	private int getScoreForNameSearch() {
		return scoreForNameSearch;
	}

	public String getCompanyTypeName(){
		return companyTypeName;
	}
	
	public String getPersonalTypeName(){
		return personalTypeName;
	}
	
	public void setCompanyTypeName(String name){
		 companyTypeName = name;
	}
	
	public void setPersonalTypeName(String name){
		personalTypeName = name;
	}
	
	private int getScoreForCommonNameSearch() {
		return scoreForCommonNameSearch;
	}

	public int getNumberOfYearsAllowed(){
		return numberOfYearsAllowed;
	}
	
	public void setNumberOfYearsAllowed(int numberOfYearsAllowed){
		this.numberOfYearsAllowed = numberOfYearsAllowed;
	}
	
	protected List<String> auxData = new ArrayList<String>();

	public List<String> getAuxData() {
		return auxData;
	}
	public void setAuxData(List<String> l) {
		auxData = l;
	}

	public void setAddMCNDefault(boolean add){
		addMCNDefault = add;
	}

	public boolean getAddMCNDefault(){
		return addMCNDefault;
	}

	public void setScoreForNameSearch(int scoreForNameSearch) {
		this.scoreForNameSearch = scoreForNameSearch;
	}

	public void setScoreForCommonNameSearch(int scoreForCommonNameSearch) {
		this.scoreForCommonNameSearch = scoreForCommonNameSearch;
	}
}
