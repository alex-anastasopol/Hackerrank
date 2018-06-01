package ro.cst.tsearch.titledocument.abstracts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.property.PinI.PinType;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.taxutils.BondAndAssessment;
import com.stewart.ats.base.taxutils.BondAndAssessmentI;
import com.stewart.ats.base.taxutils.Installment;
import com.stewart.ats.base.taxutils.InstallmentI;
import com.stewart.ats.base.taxutils.PriorYearsDelinquent;
import com.stewart.ats.base.taxutils.PriorYearsDelinquentI;
import com.stewart.ats.base.taxutils.TaxRedemption;
import com.stewart.ats.base.taxutils.TaxRedemptionI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.searchsites.client.TaxSiteData;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;

/**
 * @author Cristian Stochina
 */
public class TaxUtilsNew {
	
	public final int DEFAULT_TAX_YEAR;
	{
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, 10);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.add(Calendar.DAY_OF_MONTH,-1);
		Date payDate = cal.getTime();
		Calendar current = Calendar.getInstance();
		Date currentDate = current.getTime();
		if(currentDate.after(payDate)){
			DEFAULT_TAX_YEAR = cal.get(Calendar.YEAR);
		}
		else{
			DEFAULT_TAX_YEAR = cal.get(Calendar.YEAR)-1;
		}
	}
	
	public final Date DEFAULT_CNTY_PAY_DATE;
	{
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, DEFAULT_TAX_YEAR);
		cal.set(Calendar.MONTH, 9);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		DEFAULT_CNTY_PAY_DATE= cal.getTime();
	}
	
	public final Date DEFAULT_CITY_PAY_DATE;
	{
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, DEFAULT_TAX_YEAR);
		cal.set(Calendar.MONTH, 9);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		DEFAULT_CITY_PAY_DATE= cal.getTime();
	}
	
	public final  Date DEFAULT_CNTY_DUE_DATE;
	{
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, DEFAULT_TAX_YEAR+1);
		cal.set(Calendar.MONTH, 2);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		DEFAULT_CNTY_DUE_DATE= cal.getTime();
	}
	
	public final  Date DEFAULT_CITY_DUE_DATE;
	{
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, DEFAULT_TAX_YEAR+1);
		cal.set(Calendar.MONTH, 2);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		DEFAULT_CITY_DUE_DATE= cal.getTime();
	}
	
	private Search global = null;
	private String chapterPath = null;

	// tax status constants
	public static final int TAX_NOT_APLICABE = -1;
	public static final int TAX_IN_PAY_PERIOD = 1;
	public static final int TAX_OUT_OF_PAY_PERIOD = 0;

	private TaxDocumentI doc;

	private static Calendar currentCal = Calendar .getInstance();
	
	/* -- START values from DataBase , county specific values-- */
	private java.util.Date payDate = null;
	private java.util.Date dueDate = null;
	private int taxYearMode = TaxSiteData.TAX_YEAR_PD_YEAR;
	/* -- END values from DataBase , county specific values-- */

	// the current tax year is always before or equals with current year
	private int currentTaxYear = DEFAULT_TAX_YEAR;

	// can be one of the above constants
	private int status = TAX_NOT_APLICABE;

	private double totalDelinquent = 0.0d;//Double.MIN_VALUE;
	
	/**
	 * 
	 * @param searchId
	 * @param type
	 * @param pathToTaxDocument 
	 */
	public TaxUtilsNew(long searchId, DType type,String stateAbrev,String countyName, TaxDocumentI doc) {
		this(searchId, type, stateAbrev, countyName, doc, false);
	}
	
	public TaxUtilsNew(long searchId, DType type,String stateAbrev,String countyName, TaxDocumentI doc, boolean interpretTRDocAsEP) {

		if (searchId <= 0) {
			throw new RuntimeException("Ilegal SearchId provided: " + searchId);
		}
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		int commId = currentInstance.getCommunityId();
		global = currentInstance.getCrtSearchContext();
		
		if( doc==null ){
			doc = getFirstCheckedTaxDocument(type);
		}

		if (interpretTRDocAsEP) {
			if (doc != null) {
				doc.setType(DType.TAX);
			}
		}
		
		this.doc = doc;
		
		if(!(type == DType.CITYTAX || type == DType.TAX)){
			throw new RuntimeException("Ilegal TAX type provided: " + type);
		}
		if( type == DType.CITYTAX ) {
			payDate = DEFAULT_CITY_PAY_DATE;
			dueDate = DEFAULT_CITY_DUE_DATE;
		}
		else if ( type == DType.TAX ){
			payDate = DEFAULT_CNTY_PAY_DATE;
			dueDate  = DEFAULT_CNTY_DUE_DATE;
		}
		
		Date payDateTemp = null;
		Date dueDateTemp = null;
		
		if(doc!=null && !doc.isUploaded()){
			DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(commId, (int)doc.getSiteId());
			if(dat != null) {
				payDateTemp = dat.getPayDate();
				dueDateTemp = dat.getDueDate();
				taxYearMode = dat.getTaxYearMode();
			}
		} else {
			payDateTemp = HashCountyToIndex.getPayDate(commId, stateAbrev,countyName,type);
			dueDateTemp = HashCountyToIndex.getDueDate(commId, stateAbrev,countyName,type);
			taxYearMode = HashCountyToIndex.getTaxYearMode(commId, stateAbrev,countyName,type);
		}
		if(payDateTemp !=null){
			payDate = payDateTemp;
		}
		if(dueDateTemp !=null){
			dueDate  = dueDateTemp;
		}
		if(doc != null && !doc.isUploaded()){
			double amtDue = doc.getAmountDue();
			double splitPaymAmt = doc.getSplitPaymentAmount();
			if (splitPaymAmt > 0.0 && amtDue != splitPaymAmt){
				Calendar cal = Calendar.getInstance();;
				cal.setTime(dueDate);
				cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) - 150);
				dueDate = cal.getTime();
			}
		}
		
		Struct1 str1 = calculateCurrentTaxYear(this.payDate, this.dueDate, this.taxYearMode, this.doc);
		this.currentTaxYear = str1.currentTaxYear;
		if (str1.payDate!=null) {
			this.payDate = str1.payDate;
		}
		if (str1.dueDate!=null) {
			this.dueDate = str1.dueDate;
		}
		
		Struct2 str2 = calculateTotalDelinquent(this.status, this.dueDate, this.payDate, this.doc, this.currentTaxYear);
		if (str2.status!=TAX_NOT_APLICABE) {
			this.status = str2.status;
		}
		if (str2.totalDelinquent!=0.0d) {
			this.totalDelinquent = str2.totalDelinquent;
		}
		
	}
	
	public static Struct1 calculateCurrentTaxYear(Date payDate, Date dueDate, int taxYearMode, TaxDocumentI doc) {
		
		Struct1 result = new Struct1();
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(payDate);		
		result.currentTaxYear = cal.get(Calendar.YEAR);
		
		//if year(PD) == year(DD) than currentTaxYear = year(PD) - 1
		if(taxYearMode == TaxSiteData.TAX_YEAR_PD_YEAR_MINUS_1) {
			result.currentTaxYear--;
		} else if(taxYearMode == TaxSiteData.TAX_YEAR_PD_YEAR_PLUS_1) {
			result.currentTaxYear++;
		}
		
		if(doc!=null ) {
			if(doc.getDueDate() != null) {
				result.dueDate = doc.getDueDate();
			}
			if(doc.getPayDate() != null) {
				result.payDate = doc.getPayDate();
			}
			if(doc.getFoundYear() != SimpleChapterUtils.UNDEFINED_YEAR) {
				result.currentTaxYear = doc.getFoundYear();
			} else {
				if(doc.getPayDate() != null) {
				//let's be sure payDate did not change
					cal.setTime(payDate);		
					result.currentTaxYear = cal.get(Calendar.YEAR);
					
					//if year(PD) == year(DD) than currentTaxYear = year(PD) - 1
					if(taxYearMode == TaxSiteData.TAX_YEAR_PD_YEAR_MINUS_1) {
						result.currentTaxYear--;
					} else if(taxYearMode == TaxSiteData.TAX_YEAR_PD_YEAR_PLUS_1) {
						result.currentTaxYear++;
					}
				}
			}
		}
		
		return result;
	}
	
	public static Struct2 calculateTotalDelinquent(int status, Date dueDate, Date payDate, TaxDocumentI doc, int currentTaxYear) {
		
			Struct2 result = new TaxUtilsNew.Struct2();
			
			if (doc!=null) {
				result.status = status;
				
				Date CD = currentCal.getTime();
				if(CD.after(dueDate)){ 			// Current Date  > Due Date
					result.status = 0;
				}
				else if(CD.after(payDate)){		// Current Date  between PayDate and Due Date
					result.status = 1;
				}
							
				if( (doc.getFoundYear()<currentTaxYear) || ( (result.status == 0) && (doc.getFoundYear()==currentTaxYear) ) ){
					result.totalDelinquent = doc.getFoundDelinquent()+ doc.getAmountDue();
				}
				else{
					result.totalDelinquent = doc.getFoundDelinquent();
				}
				if(	result.totalDelinquent<0	){
					result.totalDelinquent = 0;
				}
			}
			
			return result;
	}
	
	
	private TaxDocumentI getFirstCheckedTaxDocument(DType type){
		DocumentsManagerI manager = global.getDocManager();
		try{
			manager.getAccess();
			Collection<DocumentI>  taxDocs = manager.getDocumentsWithType( true, type );
			ArrayList<DocumentI> list = new ArrayList<DocumentI>(taxDocs);
			Collections.sort(list, new Comparator<DocumentI>(){
				@Override
				public int compare(DocumentI o1, DocumentI o2) {
					if("TR".equalsIgnoreCase(o1.getDataSource())&&!"TR".equalsIgnoreCase(o2.getDataSource())){
						return -1;
					}else if("TR".equalsIgnoreCase(o2.getDataSource())&&!"TR".equalsIgnoreCase(o1.getDataSource())){
						return 1;
					}
					return 0;
				}
			});
			if(list.size()>0){
				return  (TaxDocumentI)list.get(0);
			}
		}
		finally{
			manager.releaseAccess();
		}
		return null;
	}

	public int getFoundYear() {
		if(doc==null){
			return DEFAULT_TAX_YEAR;
		}
		return doc.getFoundYear();
	}

	public double getBaseAmount() {
		if(doc==null){
			return 0;
		}
		return doc.getBaseAmount();
	}

	public double getAmountPaid() {
		if(doc==null){
			return 0;
		}
		return doc.getAmountPaid();
	}

	public double getAmountDue() {
		if(doc==null){
			return 0;
		}
		double amountDue = doc.getAmountDue();
		return ( amountDue<0 ) ? 0 : amountDue; 
	}
	
	public double getBaseAmountEP() {
		if(doc==null){
			return 0;
		}
		return doc.getBaseAmountEP();
	}

	public double getAmountDueEP() {
		if(doc==null){
			return 0;
		}
		double amountDueEP = doc.getAmountDueEP();
		return ( amountDueEP<0 ) ? 0 : amountDueEP; 
		//is not logic to have negative amount due 
	}

	public double getTotalDelinquentEP(){
		if(doc==null){
			return 0;
		}
		return doc.getTotalDelinquentEP(); 
	}
	
	public boolean hasCityInformation() {
		if( doc==null ){
			return false;
		}
		return doc.isCountyTaxAndHasCityInformationAlso(); 
	}
	
	public double getFoundDelinguent() {
		if( doc==null ){
			return 0;
		}
		return doc.getFoundDelinquent();
	}

	public int getCurrentTaxYear() {
		return currentTaxYear;
	}

	public void setCurrentTaxYear(int currentTaxYear) {
		this.currentTaxYear = currentTaxYear;
	}

	public java.util.Date getPayDate() {
		return payDate;
	}

	public void setPayDate(java.util.Date payDate) {
		this.payDate = payDate;
	}

	public java.util.Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(java.util.Date dueDate) {
		this.dueDate = dueDate;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int taxStatus) {
		this.status = taxStatus;
	}

	public int getCountyTaxVolume() {
		int taxVolume = Integer.MIN_VALUE;
		try{ taxVolume = doc.getTaxVolume(); }catch(Exception e){}
		return taxVolume;
	}

	public String getParcelId() {
		String parcelId ="";
		try{ parcelId = (doc.getProperties().toArray(new PropertyI[1]))[0].getPin(PinType.PID); }catch(Exception e){}
		return parcelId;
	}

	public String getChapterPath() {
		return chapterPath;
	}

	public void setChapterPath(String chapterPath) {
		this.chapterPath = chapterPath;
	}
	
	

	public double getTotalDelinquent() {
		return totalDelinquent;
	}

	public void setTotalDelinquent(double totalDelinquent) {
		this.totalDelinquent = totalDelinquent;
	}

	public InstallmentI getInstalment(int poz) {
		if( doc==null ){
			return null;
		}
		ArrayList<Installment> instalments =doc.getInstallments();
		return ((instalments==null)? null : instalments.get(poz));
	}
	
	public int getInstalmentsSize() {
		if( doc==null ){
			return 0;
		}
		ArrayList<Installment> instalments =doc.getInstallments();
		return (( instalments==null )? 0 :instalments.size());
	}
	
	/**
	 * Installments for Special Assessments
	 * @return
	 */
	public InstallmentI getSpecialAssessmentInstalment(int poz) {
		if (doc == null){
			return null;
		}
		ArrayList<Installment> installments = doc.getSpecialAssessmentInstallments();
		return ((installments == null) ? null : installments.get(poz));
	}
	
	/**
	 * Installments for Special Assessments
	 * @return
	 */
	public int getSpecialAssessmentInstallmentsSize(){
		if (doc == null){
			return 0;
		}
		ArrayList<Installment> installments = doc.getSpecialAssessmentInstallments();
		return ((installments == null ) ? 0 : installments.size());
	}

	public BondAndAssessmentI getBond(int poz) {
		if( doc==null ){
			return null;
		}
		ArrayList<BondAndAssessment> bonds = doc.getBonds();
		return ((bonds==null)? null: bonds.get(poz));
	}
	
	public int getBondsSize() {
		if( doc==null ){
			return 0;
		}
		ArrayList<BondAndAssessment> bonds = doc.getBonds();
		return (( bonds==null )? 0: bonds.size());
	}
	
	public PriorYearsDelinquentI getPriorYear(int poz) {
		if( doc==null ){
			return null;
		}
		ArrayList<PriorYearsDelinquent> priorYears =doc.getPriorYears();
		return ((priorYears==null)? null : priorYears.get(poz));
	}
	
	public int getPriorYearsSize() {
		if( doc==null ){
			return 0;
		}
		ArrayList<PriorYearsDelinquent> priorYears =doc.getPriorYears();
		return  ((priorYears==null )? 0 : priorYears.size()) ;
	}
	
	public TaxRedemptionI getRedemtion(int poz) {
		if( doc==null ){
			return null;
		}
		ArrayList<TaxRedemption> redemtions =doc.getRedemtions();
		return ((redemtions==null)? null : redemtions.get(poz));
	}
	
	public int getRedemtionsSize() {
		if( doc==null ){
			return 0;
		}
		ArrayList<TaxRedemption> redemtions =doc.getRedemtions();
		return  ((redemtions==null )? 0 : redemtions.size()) ;
	}
	
	public String getSaleDate() {
		if(doc==null){
			return "";
		}
		return doc.getSaleDate();
	}

	public String getSaleNo() {
		if(doc==null){
			return "";
		}
		return doc.getSaleNo();
	}

	public String getAreaCode() {
		String areaCode ="";
		try{ areaCode = (doc.getProperties().toArray(new PropertyI[1]))[0].getAreaCode(); }catch(Exception e){}
		return areaCode;
	}	

	public String getAreaName() {
		String areaName ="";
		try{ areaName = (doc.getProperties().toArray(new PropertyI[1]))[0].getAreaName(); }catch(Exception e){}
		return areaName;
	}

	public double getExemptionAmount() {
		if(doc==null){
			return 0;
		}
		return doc.getExemptionAmount();
	}

	public String getTaxYearDescription() {
		if(doc==null){
			return "";
		}
		return doc.getTaxYearDescription();
	}

	public double getAssessedValueLand() {
		if(doc==null){
			return 0;
		}
		return doc.getAppraisedValueLand();
	}

	public double getAssessedValueImprovements() {
		if(doc==null){
			return 0;
		}
		return doc.getAppraisedValueImprovements();
	}
	
	public void setDoc(TaxDocumentI doc) {
		this.doc = doc;
	}
	
	public TaxDocumentI getDoc() {
		return doc;
	}
	
	public static class Struct1 {
		int currentTaxYear;
		Date payDate = null;
		Date dueDate = null;
		public int getCurrentTaxYear() {
			return currentTaxYear;
		}
	}
	
	public static class Struct2 {
		int status = TAX_NOT_APLICABE;
		double totalDelinquent = 0.0d;
		public double getTotalDelinquent() {
			return totalDelinquent;
		}
	}
	
}
