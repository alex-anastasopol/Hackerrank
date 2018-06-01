package ro.cst.tsearch.servers.response;

public class TaxHistorySet extends InfSet {

	private static final long serialVersionUID = 550027592873148666L;

	public TaxHistorySet() {
        super(
        		new String[]{
	        		"Year", 			  //found year current (the most recent) tax (fiscal) year recorded in the tax document
	        		"TaxYearDescription", // used on California DT tax to store "2007-08" 
	        		"TaxBillNumber", 	//bill number
	                "TotalDue", 		//current tax year total due (for the found year)  usually: (baseAmout-deductions-amountPaid+penalties+interests) // amount due din gui
	                "AmountPaid",       //amount paid for found tax year
	                "BaseAmount",		//
	                "PriorDelinquent", 	//
	                "DatePaid",  			
	                "PenaltyCurrentYear", 
	                "CurrentYearDue", 
	                "ReceiptDate",		//receipt date
	                "ReceiptNumber",   
	                "ReceiptAmount", 
	                "TaxVolume",
	                "TaxSaleDate",
	                "TaxSaleNumber",
	                "TaxExemptionAmount",
	                "TotalDueEP", // used for TNShelbyTR when we have city taxes in TR document
	                "PriorDelinquentEP", 	//used for TNShelbyTR when we have city taxes in TR document
	                "TaxYearEPfromTR", 	//used for TNShelbyTR when we have city taxes in TR document
	                "BaseAmountEP",  //used for TNShelbyTR when we have city taxes in TR document
	                "BaseAmountEP",  //used for TNShelbyTR when we have city taxes in TR document
	                "SplitPaymentAmount",  //used for TXBexarTR
        		});
	}
	
	
	public enum TaxHistorySetKey{
		YEAR("Year"),
		TAX_YEAR_DESCRIPTION("TaxYearDescription"),
		TAX_BILL_NUMBER("TaxBillNumber"),
		TOTAL_DUE("TotalDue"),
		AMOUNT_PAID("AmountPaid"),
		BASE_AMOUNT("BaseAmount"),
		PRIOR_DELINQUENT("PriorDelinquent"),
		DATE_PAID("DatePaid"),
		PENALTY_CURRENT_YEAR("PenaltyCurrentYear"),
		CURRENT_YEAR_DUE("CurrentYearDue"),
		RECEIPT_DATE("ReceiptDate"),
		RECEIPT_NUMBER("ReceiptNumber"),
		RECEIPT_AMOUNT("ReceiptAmount"),
		TAX_VOLUME("TaxVolume"),
		TAX_SALE_DATE("TaxSaleDate"),
		TAX_SALE_NUMBER("TaxSaleNumber"),
		TAX_EXEMPTION_AMOUNT("TaxExemptionAmount"),
		TOTAL_DUE_EP("TotalDueEP"),
		PRIOR_DELINQUENT_EP("PriorDelinquentEP"),
		TAX_YEAR_EPFROM_TR("TaxYearEPfromTR"),
		BASE_AMOUNT_EP("BaseAmountEP"),
		SPLIT_PAYMENT_AMOUNT("SplitPaymentAmount");
		
		String keyName;
		TaxHistorySetKey(String keyName){
		   this.shortKeyName = keyName;	
		   this.keyName = "TaxHistorySet."+keyName;
		}
	       
	    public String getKeyName() {
	    	return keyName;
	    }
	    
	    String shortKeyName;
	    public String getShortKeyName() {
            return shortKeyName;
	    }


	}
}
