package ro.cst.tsearch.servers.functions;

import org.apache.commons.lang.StringUtils;

public class PersonBean {
	private String name = "";
	private String dob = "";
	private String vin = "";
	private String plate = "";
	private String informationSource = "";
	private String informationSourceKey = "";
	private String recordId = "";
	private String ed = "";

	public String getName() {
		return name;
	}

	public String getUniqueID() {
		boolean isNotEmpty = StringUtils.isNotEmpty(getInformationSourceKey()) && StringUtils.isNotEmpty(getRecordId())
				&& StringUtils.isNotEmpty(getEd());
		return (isNotEmpty) ? getInformationSourceKey() + "_" + getRecordId() + "_" + getEd() : "";
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDob() {
		return dob;
	}

	public void setDob(String dob) {
		this.dob = dob;
	}

	public String getInformationSource() {
		return informationSource;
	}

	public void setInformationSource(String informationSource) {
		this.informationSource = informationSource;
	}

	public String getInformationSourceKey() {
		return informationSourceKey;
	}

	public void setInformationSourceKey(String informationSourceKey) {
		this.informationSourceKey = informationSourceKey;
	}

	public String getRecordId() {
		return recordId;
	}

	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}

	public String getEd() {
		return ed;
	}

	public void setEd(String ed) {
		this.ed = ed;
	}

	public void setVin(String vin) {
		this.vin = vin;
	}

	public String getVin() {
		return vin;
	}

	public void setPlate(String plate) {
		this.plate = plate;
	}

	public String getPlate() {
		return plate;
	}
}

