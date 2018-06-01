package ro.cst.tsearch.bean;

public class InvoiceATS2FABean {
	private String partyBranch;
	private String fileNum;
	private String party_Customer;
	private String adrsLines_Customer;
	private String city_Customer;
	private String stateAbbr_Customer;
	private String zip_Customer;
	private String invcDate;
	private String ATSProduct;
	private String tranDtlAmt;
	private String buyerName;
	private String sellerName;
	private String adrsLines_Property;
	private String city_Property;
	private String county_Property;
	private String stateAbbr_Property;
	private String zip_Property;
	private String operatingAccountingID;
	private String plantInvoice;
	private String agentLoginName;
	public String dataSource = "";
	public String imageCount = "";
	
	public InvoiceATS2FABean(String partyBranch, 
			String fileNum, 
			String party_Customer, 
			String adrsLines_Customer, 
			String city_Customer, 
			String stateAbbr_Customer, 
			String zip_Customer, 
			String invcDate, 
			String opAccID, 
			String ATSproduct, 
			String tranDtlAmt, 
			String buyerName, 
			String sellerName, 
			String adrsLines_Property, 
			String city_Property, 
			String county_Property, 
			String stateAbbr_Property, 
			String zip_Property,
			String plantInvoice, 
			String agentLoginName,
			String dataSource,
			String imageCount) {
		super();
		this.partyBranch = partyBranch;
		this.fileNum = fileNum;
		this.party_Customer = party_Customer;
		this.adrsLines_Customer = adrsLines_Customer;
		this.city_Customer = city_Customer;
		this.stateAbbr_Customer = stateAbbr_Customer;
		this.zip_Customer = zip_Customer;
		this.invcDate = invcDate;
		this.ATSProduct = ATSproduct;
		this.tranDtlAmt = tranDtlAmt;
		this.buyerName = buyerName;
		this.sellerName = sellerName;
		this.adrsLines_Property = adrsLines_Property;
		this.city_Property = city_Property;
		this.county_Property = county_Property;
		this.stateAbbr_Property = stateAbbr_Property;
		this.zip_Property = zip_Property;
		this.operatingAccountingID = opAccID;
		this.plantInvoice = plantInvoice;
		this.agentLoginName = agentLoginName;
		this.dataSource = dataSource;
		this.imageCount = imageCount;
	}

	public String getOperatingAccountingID(){
		return this.operatingAccountingID;
	}
	
	public void setOperatingAccountingID(String opAccID){
		this.operatingAccountingID = opAccID;
	}
	
	public String getAdrsLines_Customer() {
		return adrsLines_Customer;
	}

	public void setAdrsLines_Customer(String adrsLines_Customer) {
		this.adrsLines_Customer = adrsLines_Customer;
	}

	public String getAdrsLines_Property() {
		return adrsLines_Property;
	}

	public void setAdrsLines_Property(String adrsLines_Property) {
		this.adrsLines_Property = adrsLines_Property;
	}

	public String getATSProduct() {
		return ATSProduct;
	}

	public void setATSProduct(String product) {
		ATSProduct = product;
	}

	public String getBuyerName() {
		return buyerName;
	}

	public void setBuyerName(String buyerName) {
		this.buyerName = buyerName;
	}

	public String getCity_Customer() {
		return city_Customer;
	}

	public void setCity_Customer(String city_Customer) {
		this.city_Customer = city_Customer;
	}

	public String getCity_Property() {
		return city_Property;
	}

	public void setCity_Property(String city_Property) {
		this.city_Property = city_Property;
	}

	public String getFileNum() {
		return fileNum;
	}

	public void setFileNum(String fileNum) {
		this.fileNum = fileNum;
	}

	public String getInvcDate() {
		return invcDate;
	}

	public void setInvcDate(String invcDate) {
		this.invcDate = invcDate;
	}

	public String getParty_Customer() {
		return party_Customer;
	}

	public void setParty_Customer(String party_Customer) {
		this.party_Customer = party_Customer;
	}

	public String getPartyBranch() {
		return partyBranch;
	}

	public void setPartyBranch(String partyBranch) {
		this.partyBranch = partyBranch;
	}

	public String getSellerName() {
		return sellerName;
	}

	public void setSellerName(String sellerName) {
		this.sellerName = sellerName;
	}

	public String getStateAbbr_Customer() {
		return stateAbbr_Customer;
	}

	public void setStateAbbr_Customer(String stateAbbr_Customer) {
		this.stateAbbr_Customer = stateAbbr_Customer;
	}

	public String getStateAbbr_Property() {
		return stateAbbr_Property;
	}

	public void setStateAbbr_Property(String stateAbbr_Property) {
		this.stateAbbr_Property = stateAbbr_Property;
	}

	public String getTranDtlAmt() {
		return tranDtlAmt;
	}

	public void setTranDtlAmt(String tranDtlAmt) {
		this.tranDtlAmt = tranDtlAmt;
	}

	public String getZip_Customer() {
		return zip_Customer;
	}

	public void setZip_Customer(String zip_Customer) {
		this.zip_Customer = zip_Customer;
	}

	public String getZip_Property() {
		return zip_Property;
	}

	public void setZip_Property(String zip_Property) {
		this.zip_Property = zip_Property;
	}

	public String getCounty_Property() {
		return county_Property;
	}

	public void setCounty_Property(String county_Property) {
		this.county_Property = county_Property;
	}

	public String getPlantInvoice() {
		return plantInvoice;
	}

	public void setPlantInvoice(String plantInvoice) {
		this.plantInvoice = plantInvoice;
	}

	public String getAgentLoginName() {
		return agentLoginName;
	}

	public void setAgentLoginName(String agentLoginName) {
		this.agentLoginName = agentLoginName;
	}

	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public String getImageCount() {
		return imageCount;
	}

	public void setImageCount(String imageCount) {
		this.imageCount = imageCount;
	}
	
	
}
