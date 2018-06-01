package ro.cst.tsearch.reports.data;

import java.util.Date;
import java.util.Vector;

import ro.cst.tsearch.SearchFlags;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.NameI;

public class InvoiceXmlData {
	
	private String abstrFileNo;
	private long searchId;
	private long id;
	private int productType;
	private int invoiced;
	private String productTypeName;
	private double searchFee;
	private Date doneTime;
	private String plantInvoice;
	
	private String agentCompany;
	private String agentLastName;
	private String agentFirstName;
	private String agentLogin;
	//private String agentStrName;
	//private String agentStrSuffix;
	//private String agentStrUnit;
	private String agentWorkAddress;
	private String agentCity;
	private String agentZip;
	private String agentStateAbv;
	private Long agentId;
	
	private Vector<NameI> owners = new Vector<NameI>();
	private Vector<NameI> buyers = new Vector<NameI>();
		
	private String propertyNo;
	private String propertyDirection;
	private String propertyName;
	private String propertySuffix;
	private String propertyUnit;
	private String propertyCity;
	private String propertyZip;
	private String propertyStateAbv;
	private String propertyCounty;
	
	private String operatingAccountingID;
	public String dataSource = "";
	public String imageCount = "";
	
	private String requestCountDescription = "";
	
	private SearchFlags searchFlags = new SearchFlags();	
	
	public String getOperatingAccountingID(){
		return operatingAccountingID;
	}
	
	public void setOperatingAccountingID(String opAccId){
		operatingAccountingID = opAccId;
	}
	public String getAbstrFileNo() {
		return abstrFileNo;
	}
	public void setAbstrFileNo(String abstrFileNo) {
		this.abstrFileNo = StringUtils.transformNull(abstrFileNo);
	}
	public String getAgentCity() {
		return agentCity;
	}
	public void setAgentCity(String agentCity) {
		this.agentCity = StringUtils.transformNull(agentCity);
	}
	public String getAgentCompany() {
		return agentCompany;
	}
	public void setAgentCompany(String agentCompany) {
		this.agentCompany = StringUtils.transformNull(agentCompany);
	}
	public String getAgentStateAbv() {
		return agentStateAbv;
	}
	public void setAgentStateAbv(String agentStateAbv) {
		this.agentStateAbv = StringUtils.transformNull(agentStateAbv);
	}
	public String getAgentWorkAddress() {
		return agentWorkAddress;
	}
	public void setAgentWorkAddress(String agentWorkAddress) {
		this.agentWorkAddress = StringUtils.transformNull(agentWorkAddress);
	}
	public String getAgentZip() {
		return agentZip;
	}
	public void setAgentZip(String agentZip) {
		this.agentZip = StringUtils.transformNull(agentZip);
	}
	public String getProductTypeName() {
		return productTypeName;
	}
	public void setProductTypeName(String productTypeName) {
		this.productTypeName = productTypeName;
	}
	public int getProductType() {
		return productType;
	}
	public void setProductType(int productType) {
		this.productType = productType;
	}
	public String getPropertyCity() {
		return propertyCity;
	}
	public void setPropertyCity(String propertyCity) {
		this.propertyCity = StringUtils.transformNull(propertyCity);
	}
	public String getPropertyDirection() {
		return propertyDirection;
	}
	public void setPropertyDirection(String propertyDirection) {
		this.propertyDirection = StringUtils.transformNull(propertyDirection);
	}
	public String getPropertyName() {
		return propertyName;
	}
	public void setPropertyName(String propertyName) {
		this.propertyName = StringUtils.transformNull(propertyName);
		this.propertyName = this.propertyName.replaceAll("'", "");
	}
	public String getPropertyNo() {
		return propertyNo;
	}
	public void setPropertyNo(String propertyNo) {
		this.propertyNo = StringUtils.transformNull(propertyNo);
	}
	public String getPropertyStateAbv() {
		return propertyStateAbv;
	}
	public void setPropertyStateAbv(String propertyStateAbv) {
		this.propertyStateAbv = StringUtils.transformNull(propertyStateAbv);
	}
	public String getPropertySuffix() {
		return propertySuffix;
	}
	public void setPropertySuffix(String propertySuffix) {
		this.propertySuffix = StringUtils.transformNull(propertySuffix);
	}
	public String getPropertyUnit() {
		return propertyUnit;
	}
	public void setPropertyUnit(String propertyUnit) {
		this.propertyUnit = StringUtils.transformNull(propertyUnit);
	}
	public String getPropertyZip() {
		return propertyZip;
	}
	public void setPropertyZip(String propertyZip) {
		this.propertyZip = StringUtils.transformNull(propertyZip);
	}
	public double getSearchFee() {
		return searchFee;
	}
	public void setSearchFee(double searchFee) {
		this.searchFee = searchFee;
	}
	public Date getDoneTime() {
		return doneTime;
	}
	public void setDoneTime(Date doneTime) {
		this.doneTime = doneTime;
	}
	public long getSearchId() {
		return searchId;
	}
	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}
	public String getBuyerName() {
		StringBuilder sb = new StringBuilder();
		for (NameI name : buyers) {
			sb.append(name.getFullName() + "; ");
		}
		if(sb.length()>1)
			return sb.substring(0, sb.length() - 2);
		else
			return "";
	}
	public String getOwnersName() {
		StringBuilder sb = new StringBuilder();
		for (NameI name : owners) {
			sb.append(name.getFullName() + "; ");
		}
		if(sb.length()>1)
			return sb.substring(0, sb.length() - 2);
		else
			return "";
	}
	public String getPropertyAddress() {
		return getPropertyNo() + " " + getPropertyName() + " " + getPropertySuffix();
	}
	
	public String getPropertyCounty() {
		return propertyCounty;
	}
	public void setPropertyCounty(String propertyCounty) {
		this.propertyCounty = StringUtils.transformNull(propertyCounty);
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

	public String getAgentFirstName() {
		return agentFirstName;
	}
	public void setAgentFirstName(String agentFirstName) {
		this.agentFirstName = StringUtils.transformNull(agentFirstName);
	}
	public String getAgentLastName() {
		return agentLastName;
	}
	public void setAgentLastName(String agentLastName) {
		this.agentLastName = StringUtils.transformNull(agentLastName);
	}
	public String getAgentName() {
		return getAgentFirstName() + " " + getAgentLastName();
	}

	public String getPlantInvoice() {
		return plantInvoice;
	}

	public void setPlantInvoice(String plantInvoice) {
		this.plantInvoice = plantInvoice;
	}
	
	public void setAgentId(Long agentId) {
		this.agentId = agentId;
	}
	public Long getAgentId() {
		return agentId;
	}
	
	public void addOwnerName(NameI name){
		owners.add(name);
	}
	public void addBuyerName(NameI name){
		buyers.add(name);
	}

	/**
	 * @return the invoiced
	 */
	public int getInvoiced() {
		return invoiced;
	}

	/**
	 * @param invoiced the invoiced to set
	 */
	public void setInvoiced(int invoiced) {
		this.invoiced = invoiced;
	}
	public SearchFlags getSearchFlags() {
		if(searchFlags == null)
			searchFlags = new SearchFlags();
		return searchFlags;
	}

	public void setSearchFlags(SearchFlags searchFlags) {
		this.searchFlags = searchFlags;
	}

	public String getAgentLogin() {
		return agentLogin;
	}

	public void setAgentLogin(String agentLogin) {
		this.agentLogin = agentLogin;
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

	public String getRequestCountDescription() {
		return requestCountDescription;
	}

	public void setRequestCountDescription(String requestCount) {
		this.requestCountDescription = requestCount;
	}
	
	
}
