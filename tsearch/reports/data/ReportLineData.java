package ro.cst.tsearch.reports.data;

import java.math.BigDecimal;
import java.util.HashSet;

import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;

public class ReportLineData {

	private long noOfSearches;
	private long allNoOfSearches;
	private long noOfUpdates;
	private long noOfFVS;
	private long noOfPureUpdates;
	private long noOfCurrentOwner;
	private long noOfRefinance;
	private long noOfConstruction;
	private long noOfCommercial;
	private long noOfOE;
	private long noOfLiens;
	private long noOfAcreage;
	private long noOfSublot;
	private long noOfIndex;
	
	
	private long noOfCounties = Long.MIN_VALUE;
	private long noOfAbstractors = Long.MIN_VALUE;
	private long noOfAgents = Long.MIN_VALUE;
	private double incomeSearches;
	private double incomeUpdates;
	private double incomeFVS;
	private double incomePureUpdates;
	//private double incomeArchives;
	private double incomeCurrentOwner;
	private double incomeRefinance;
	private double incomeConstruction;
	private double incomeCommercial;
	private double incomeOE;
	private double incomeLiens;
	private double incomeAcreage;
	private double incomeSublot;
	private double incomeIndex;
	//private double incomeDocRetrieval;
	private double incomePaid;
	private String intervalName;
	private HashSet<Long> countyIds;
	private HashSet<Long> agentIds;
	private HashSet<Long> abstractorIds;
	
	public ReportLineData() {
		countyIds = new HashSet<Long>();
		agentIds = new HashSet<Long>();
		abstractorIds = new HashSet<Long>();
	}
	
	public String getFormattedIncomeSearches() {
		return ATSDecimalNumberFormat.format(new BigDecimal(incomeSearches));
	}
	public double getIncomeSearches() {
		return incomeSearches;
	}

	public String getFormattedIncomeUpdates() {
		return ATSDecimalNumberFormat.format(new BigDecimal(incomeUpdates));
	}
	public double getIncomeUpdates() {
		return incomeUpdates;
	}

	public double getIncomePureUpdates() {
		return incomePureUpdates;
	}
	
	public String getFormattedIncomeCurrentOwner() {
		return ATSDecimalNumberFormat.format(new BigDecimal(incomeCurrentOwner));
	}
	public double getIncomeCurrentOwner() {
		return incomeCurrentOwner;
	}

	public String getFormattedIncomeRefinance() {
		return ATSDecimalNumberFormat.format(new BigDecimal(incomeRefinance));
	}
	public double getIncomeRefinance() {
		return incomeRefinance;
	}

	
	public String getFormattedIncomeConstruction(){
		return ATSDecimalNumberFormat.format(new BigDecimal(incomeConstruction));
	}
	
	public double getIncomeConstruction(){
		return incomeConstruction;
	}
	
	public String getFormattedIncomeCommercial(){
		return ATSDecimalNumberFormat.format(new BigDecimal(incomeCommercial));		
	}
	
	public double getIncomeCommercial (){
		return incomeCommercial;
	}
	
	public String getFormattedIncomeOE(){
		return ATSDecimalNumberFormat.format(new BigDecimal(incomeOE));
	}
	
	public double getIncomeOE(){
		return incomeOE;
	}
	
	public String getFormattedIncomeLiens(){
		return ATSDecimalNumberFormat.format(new BigDecimal(incomeLiens));
	}
	
	public double getIncomeLiens(){
		return incomeLiens;
	}
	
	public String getFormattedIncomeAcreage(){
		return ATSDecimalNumberFormat.format( new BigDecimal(incomeAcreage));
	}
	
	public double getIncomeAcreage(){
		return incomeAcreage;
	}
	
	public String getFormattedIncomeSublot(){
		return ATSDecimalNumberFormat.format(new BigDecimal(incomeSublot));
	}
	
	public double getIncomeSublot(){
		return incomeSublot;
	}
	

	public String getFormattedTotalIncome() {
		return ATSDecimalNumberFormat.format(new BigDecimal(
				incomeUpdates + incomeSearches +  incomeCurrentOwner + incomeRefinance + 
				incomeConstruction + incomeCommercial + incomeOE + incomeLiens + 
				incomeAcreage + incomeSublot + incomeIndex + incomeFVS));
	}
	public double getTotalIncome() {
		return incomeUpdates + incomeSearches +  incomeCurrentOwner + incomeRefinance + 
		incomeConstruction + incomeCommercial + incomeOE + incomeLiens + 
		incomeAcreage + incomeSublot + incomeIndex + incomeFVS;
	}

	public double getTotalIncomePureUpdates() {
		return incomePureUpdates + incomeSearches +  incomeCurrentOwner + incomeRefinance + incomeConstruction + incomeCommercial + incomeOE + incomeLiens + incomeAcreage + incomeSublot;
	}	
	
	public long getNoOfAbstractors() {
		return abstractorIds.size();
	}

	public long getNoOfCounties() {
		return countyIds.size();
	}

	public long getNoOfSearches() {
		return noOfSearches;
	}

	public long getAllNoOfSearches() {
		return allNoOfSearches;
	}


	public long getNoOfCurrentOwner()
	{
	    return noOfCurrentOwner;
	}

	public long getNoOfConstruction()
	{
	    return noOfConstruction;
	}

	public long getNoOfCommercial()
	{
	    return noOfCommercial;
	}
	
	public long getNoOfRefinance()
	{
	    return noOfRefinance;
	}	
	
	public long getNoOfOE()
	{
	    return noOfOE;
	}

	public long getNoOfLiens()
	{
	    return noOfLiens;
	}
	
	public long getNoOfAcreage()
	{
	    return noOfAcreage;
	}		
	
	public long getNoOfSublot()
	{
	    return noOfSublot;
	}		
	
	
	
	public long getNoOfUpdates() {
		return noOfUpdates;
	}
	
	public long getNoOfPureUpdates() {
		return noOfPureUpdates;
	}
	
	public void setNoOfPureUpdates(long l) {
		noOfPureUpdates = l;
	}	

	public long getTotalNo() {
		return noOfUpdates + noOfSearches + noOfRefinance + noOfCurrentOwner + noOfConstruction + noOfCommercial + noOfOE + noOfLiens + noOfAcreage + noOfSublot + noOfFVS;
	}
	
	public long getTotalNoPureUpdates() {
		return noOfUpdates + noOfSearches + noOfRefinance + noOfCurrentOwner + noOfConstruction + noOfCommercial + noOfOE + noOfLiens + noOfAcreage + noOfSublot + noOfFVS;
	}	

	public void setIncomeSearches(double l) {
		incomeSearches = l;
	}

	public void setIncomeUpdates(double l) {
		incomeUpdates = l;
	}

	public void setIncomePureUpdates(double l) {
		incomePureUpdates = l;
	}

	public void setIncomeCurrentOwner(double l) {
		incomeCurrentOwner = l;
	}
	
	public void setIncomeRefinance(double l) {
		incomeRefinance = l;
	}

	public void setIncomeConstruction (double l){
		incomeConstruction = l;
	}
	
	public void setIncomeCommercial (double l){
		incomeCommercial = l;
	}
	
	public void setIncomeOE(double l){
		incomeOE = l;
	}
	
	public void setIncomeLiens(double l){
		incomeLiens = l;
	}
	
	public void setIncomeAcreage (double l){
		incomeAcreage = l;
	}
	
	public void setIncomeSublot(double l){
		incomeSublot = l;
	}
	
	public void setNoOfAbstractors(long l) {
		noOfAbstractors = l;
	}

	public void setNoOfCounties(long l) {
		noOfCounties = l;
	}

	public void setNoOfSearches(long l) {
		noOfSearches = l;
	}

	public void setAllNoOfSearches(long l) {
		allNoOfSearches = l;
	}

	public void setNoOfUpdates(long l) {
		noOfUpdates = l;
	}

	public void setNoOfCurrentOwner(long l) {
		noOfCurrentOwner = l;
	}

	public void setNoOfRefinance(long l) {
		noOfRefinance = l;
	}

	public void setNoOfConstruction(long l){
		noOfConstruction = l;
	}
	
	public void setNoOfCommercial(long l){
		noOfCommercial = l;
	}
	
	public void setNoOfOE(long l){
		noOfOE = l;
	}
	
	public void setNoOfLiens (long l){
		noOfLiens = l;
	}
	
	public void setNoOfAcreage (long l){
		noOfAcreage = l;
	}
	
	public void setNoOfSublot (long l){
		noOfSublot = l;
	}
	
	public String getIntervalName() {
		return intervalName;
	}

	public void setIntervalName(String string) {
		intervalName = string;
	}

	public long getNoOfAgents() {
		return agentIds.size();
	}

	public void setNoOfAgents(long l) {
		noOfAgents = l;
	}

	public String getFormattedIncomePaid() {
		return ATSDecimalNumberFormat.format(new BigDecimal(incomePaid));
	}
	public String getFormattedIncomeNotPaid() {
		return ATSDecimalNumberFormat.format(new BigDecimal(getTotalIncome() - incomePaid));
	}
	public double getIncomePaid() {
		return incomePaid;
	}

	public void setIncomePaid(double incomePaid) {
		this.incomePaid = incomePaid;
	}
	/**
	 * @return the noOfIndex
	 */
	public long getNoOfIndex() {
		return noOfIndex;
	}
	/**
	 * @param noOfIndex the noOfIndex to set
	 */
	public void setNoOfIndex(long noOfIndex) {
		this.noOfIndex = noOfIndex;
	}
	/**
	 * @return the incomeIndex
	 */
	public double getIncomeIndex() {
		return incomeIndex;
	}
	/**
	 * @param incomeIndex the incomeIndex to set
	 */
	public void setIncomeIndex(double incomeIndex) {
		this.incomeIndex = incomeIndex;
	}
	public String getFormattedIncomeIndex(){
		return ATSDecimalNumberFormat.format(new BigDecimal(incomeIndex));
	}
	
	public boolean addCountyId(long countyId) {
		return countyIds.add(countyId);
	}
	public boolean addAgentId(long agentId) {
		return agentIds.add(agentId);
	}
	public boolean addAbstractorId(long abstractorId) {
		return abstractorIds.add(abstractorId);
	}

	public void addNumberOfSearchesFor(int productType, int incrementValue) {
		if(productType == Products.FULL_SEARCH_PRODUCT) {
			setNoOfSearches(getNoOfSearches() + incrementValue);
		} else if(productType == Products.ACREAGE_PRODUCT) {
			setNoOfAcreage(getNoOfAcreage() + incrementValue);
		} else if(productType == Products.COMMERCIAL_PRODUCT) {
			setNoOfCommercial(getNoOfCommercial() + incrementValue);
		} else if(productType == Products.CONSTRUCTION_PRODUCT) {
			setNoOfConstruction(getNoOfConstruction() + incrementValue);
		} else if(productType == Products.INDEX_PRODUCT) {
			setNoOfIndex(getNoOfIndex() + incrementValue);
			setAllNoOfSearches(getAllNoOfSearches() + incrementValue);
		} else if(productType == Products.LIENS_PRODUCT) {
			setNoOfLiens(getNoOfLiens() + incrementValue);
		} else if(productType == Products.OE_PRODUCT) {
			setNoOfOE(getNoOfOE() + incrementValue);
		} else if(productType == Products.REFINANCE_PRODUCT) {
			setNoOfRefinance(getNoOfRefinance() + incrementValue);
		} else if(productType == Products.SUBLOT_PRODUCT) {
			setNoOfSublot(getNoOfSublot() + incrementValue);
		} else if(productType == Products.UPDATE_PRODUCT) {
			setNoOfUpdates(getNoOfUpdates() + incrementValue);
		} else if(productType == Products.CURRENT_OWNER_PRODUCT) {
			setNoOfCurrentOwner(getNoOfCurrentOwner() + incrementValue);
		} else if(productType == Products.FVS_PRODUCT) {
			setNoOfFVS(getNoOfFVS() + incrementValue);
		}
		
	}

	public void addPriceOfSearchesFor(int productType, double searchFee, boolean isPaid) {
		if(productType == Products.FULL_SEARCH_PRODUCT) {
			setIncomeSearches(getIncomeSearches() + searchFee);
		} else if(productType == Products.ACREAGE_PRODUCT) {
			setIncomeAcreage(getIncomeAcreage() + searchFee);
		} else if(productType == Products.COMMERCIAL_PRODUCT) {
			setIncomeCommercial(getIncomeCommercial() + searchFee);
		} else if(productType == Products.CONSTRUCTION_PRODUCT) {
			setIncomeConstruction(getIncomeConstruction() + searchFee);
		} else if(productType == Products.INDEX_PRODUCT) {
			setIncomeIndex(getIncomeIndex() + searchFee);
		} else if(productType == Products.LIENS_PRODUCT) {
			setIncomeLiens(getIncomeLiens() + searchFee);
		} else if(productType == Products.OE_PRODUCT) {
			setIncomeOE(getIncomeOE() + searchFee);
		} else if(productType == Products.REFINANCE_PRODUCT) {
			setIncomeRefinance(getIncomeRefinance() + searchFee);
		} else if(productType == Products.SUBLOT_PRODUCT) {
			setIncomeSublot(getIncomeSublot() + searchFee);
		} else if(productType == Products.UPDATE_PRODUCT) {
			setIncomeUpdates(getIncomeUpdates() + searchFee);
		} else if(productType == Products.CURRENT_OWNER_PRODUCT) {
			setIncomeCurrentOwner(getIncomeCurrentOwner() + searchFee);
		} else if (productType == Products.FVS_PRODUCT) {
			setIncomeFVS(getIncomeFVS() + searchFee);
		}
		if (isPaid) {
			setIncomePaid(getIncomePaid() + searchFee);
		}
		
	}

	public void addLine(ReportLineData lineData) {
		setAllNoOfSearches(getAllNoOfSearches() + lineData.getAllNoOfSearches());
		countyIds.addAll(lineData.countyIds);
		abstractorIds.addAll(lineData.abstractorIds);
		agentIds.addAll(lineData.agentIds);
		setNoOfAcreage(getNoOfAcreage() + lineData.getNoOfAcreage());
		setNoOfCommercial(getNoOfCommercial() + lineData.getNoOfCommercial());
		setNoOfConstruction(getNoOfConstruction() + lineData.getNoOfConstruction());
		setNoOfCurrentOwner(getNoOfCurrentOwner() + lineData.getNoOfCurrentOwner());
		setNoOfIndex(getNoOfIndex() + lineData.getNoOfIndex());
		setNoOfLiens(getNoOfLiens() + lineData.getNoOfLiens());
		setNoOfOE(getNoOfOE() + lineData.getNoOfOE());
		setNoOfRefinance(getNoOfRefinance() + lineData.getNoOfRefinance());
		setNoOfSearches(getNoOfSearches() + lineData.getNoOfSearches());
		setNoOfSublot(getNoOfSublot() + lineData.getNoOfSublot());
		setNoOfUpdates(getNoOfUpdates() + lineData.getNoOfUpdates());
		setNoOfFVS(getNoOfFVS() + lineData.getNoOfFVS());
		setIncomeAcreage(getIncomeAcreage() + lineData.getIncomeAcreage());
		setIncomeCommercial(getIncomeCommercial() + lineData.getIncomeCommercial());
		setIncomeConstruction(getIncomeConstruction() + lineData.getIncomeConstruction());
		setIncomeCurrentOwner(getIncomeCurrentOwner() + lineData.getIncomeCurrentOwner());
		setIncomeIndex(getIncomeIndex() + lineData.getIncomeIndex());
		setIncomeLiens(getIncomeLiens() + lineData.getIncomeLiens());
		setIncomeOE(getIncomeOE() + lineData.getIncomeOE());
		setIncomeRefinance(getIncomeRefinance() + lineData.getIncomeRefinance());
		setIncomeSearches(getIncomeSearches() + lineData.getIncomeSearches());
		setIncomeSublot(getIncomeSublot() + lineData.getIncomeSublot());
		setIncomeUpdates(getIncomeUpdates() + lineData.getIncomeUpdates());
		setIncomeFVS(getIncomeFVS() + lineData.getIncomeFVS());
		setIncomePaid(getIncomePaid() + lineData.getIncomePaid());
	}

	public String getFormattedIncomeFVS() {
		return ATSDecimalNumberFormat.format(new BigDecimal(incomeFVS));
	}
	
	public double getIncomeFVS() {
		return incomeFVS;
	}

	public void setIncomeFVS(double incomeFVS) {
		this.incomeFVS = incomeFVS;
	}

	public long getNoOfFVS() {
		return noOfFVS;
	}

	public void setNoOfFVS(long noOfFVS) {
		this.noOfFVS = noOfFVS;
	}
	


}
