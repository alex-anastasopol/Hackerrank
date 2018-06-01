package ro.cst.tsearch.searchsites.client;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GWTCommunitySite implements IsSerializable, Cloneable {
	private int commId;
	private int countyId;
	private int siteType;
	private int cityTypeP2;
	private int enableStatus;
	
	private String stateAbrev;
	private String countyName;
	private String siteTypeAbrev;
	
	public int getCommId() {
		return commId;
	}
	public void setCommId(int commId) {
		this.commId = commId;
	}
	public int getCountyId() {
		return countyId;
	}
	public void setCountyId(int countyId) {
		this.countyId = countyId;
	}
	public int getSiteType() {
		return siteType;
	}
	public void setSiteType(int siteType) {
		this.siteType = siteType;
	}
	public int getCityTypeP2() {
		return cityTypeP2;
	}
	public void setCityTypeP2(int cityTypeP2) {
		this.cityTypeP2 = cityTypeP2;
	}
	public int getEnableStatus() {
		return enableStatus;
	}
	public void setEnableStatus(int enableStatus) {
		this.enableStatus = enableStatus;
	}
	public String getStateAbrev() {
		return stateAbrev;
	}
	public void setStateAbrev(String stateAbrev) {
		this.stateAbrev = stateAbrev;
	}
	public String getCountyName() {
		return countyName;
	}
	public void setCountyName(String countyName) {
		this.countyName = countyName;
	}
	public String getSiteTypeAbrev() {
		return siteTypeAbrev;
	}
	public void setSiteTypeAbrev(String siteTypeAbrev) {
		this.siteTypeAbrev = siteTypeAbrev;
	}
}
