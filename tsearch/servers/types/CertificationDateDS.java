package ro.cst.tsearch.servers.types;

import java.text.SimpleDateFormat;
import java.util.Date;

import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.utils.FormatDate;

/**
 * 
 * @author mihaiB
 */
public class CertificationDateDS {

	public static enum CDType {
		GI,
		CT,
		IN,
		PI,
		NA
	}
	
	private Date certificationDateDS = null;
	
	private int siteType = 0;
	
	private boolean skipInCalculation = false;
	
	private CDType type;

	public CertificationDateDS(Date certificationDateDS, int siteType){
		this.certificationDateDS = certificationDateDS;
		this.siteType = siteType;
	}
	
	public Date getCertificationDateDS() {
		return certificationDateDS;
	}

	public void setCertificationDateDS(Date certificationDateDS) {
		this.certificationDateDS = certificationDateDS;
	}

	public int getSiteType() {
		return siteType;
	}

	public void setSiteType(int siteType) {
		this.siteType = siteType;
	}
	public boolean isSkipInCalculation() {
		return skipInCalculation;
	}
	public void setSkipInCalculation(boolean skipInCalculation) {
		this.skipInCalculation = skipInCalculation;
	}
	public CDType getType() {
		if(type == null) {
			return CDType.NA;
		}
		return type;
	}
	public void setType(CDType type) {
		this.type = type;
	}

	public String toHtml() {
		SimpleDateFormat format = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY);
		StringBuilder html = new StringBuilder();
		html.append(HashCountyToIndex.getServerAbbreviationByType(getSiteType()));
		if(!CDType.NA.equals(getType())) {
			html.append("/").append(getType().toString().toLowerCase());
		}
		html.append(": ").append(format.format(getCertificationDateDS()));
		return html.toString();
	}
	
}
