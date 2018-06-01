package ro.cst.tsearch;

import java.util.Date;


public class DataSources{

	/**
	 * @author MihaiB
	 */
	
	private int siteTypeInt;
	private String siteType = "";
	private String description = "";
	private Date effectiveStartDate;
	private String runType = "";
	
	public DataSources() {
	}
	
	public DataSources(int siteTypeInt, String siteType, String description, Date effectiveStartDate){
		this.setSiteTypeInt(siteTypeInt);
		this.setSiteType(siteType);
		this.setDescription(description);
		this.setEffectiveStartDate(effectiveStartDate);
	}
	
	public DataSources(int siteTypeInt, String siteType, String description){
		this.setSiteTypeInt(siteTypeInt);
		this.setSiteType(siteType);
		this.setDescription(description);
	}

	public String getSiteType() {
		return siteType;
	}

	public void setSiteType(String siteType) {
		this.siteType = siteType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getSiteTypeInt() {
		return siteTypeInt;
	}

	public void setSiteTypeInt(int siteTypeInt) {
		this.siteTypeInt = siteTypeInt;
	}

	public Date getEffectiveStartDate() {
		return effectiveStartDate;
	}

	public void setEffectiveStartDate(Date effectiveStartDate) {
		this.effectiveStartDate = effectiveStartDate;
	}

	public String getRunType() {
		if (runType == null){
			runType = "";
		}
		return runType;
	}

	public void setRunType(String runType) {
		this.runType = runType;
	}
	
}
