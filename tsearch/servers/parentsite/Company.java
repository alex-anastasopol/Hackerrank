package ro.cst.tsearch.servers.parentsite;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import ro.cst.tsearch.data.DataAttribute;
import ro.cst.tsearch.database.DatabaseData;

public class Company extends DataAttribute {

	public static final String COMPANY_ID = "ID";
	public static final String COMPANY_NAME = "NAME";
	public static final String COMPANY_NAMEEXT = "NAMEEXT";
	public static final String COMPANY_STREETADR = "STREETADR";
	public static final String COMPANY_CITY = "CITY";
	public static final String COMPANY_COUNTY = "COUNTY";
	public static final String COMPANY_STATE = "STATE";
	public static final String COMPANY_ZIP = "ZIP";
	public static final String COMPANY_FAX = "FAX";
	public static final String COMPANY_EMAIL = "EMAIL";
	public static final String COMPANY_PHONE1 = "PHONE1";
	public static final String COMPANY_PHONE2 = "PHONE2";
	public static final String COMPANY_FILENO = "FILE_NO";

	public static final int ID = 0;
	public static final int NAME = 1;
	public static final int NAMEEXT = 2;
	public static final int STREETADR = 3;
	public static final int CITY = 4;
	public static final int COUNTY = 5;
	public static final int STATE = 6;
	public static final int ZIP = 7;
	public static final int FAX = 8;
	public static final int EMAIL = 9;
	public static final int PHONE1 = 10;
	public static final int PHONE2 = 11;
	public static final int FILENO = 12;

	protected int getAttrCount() {
		return FILENO + 1;
	}

	public static List<String> companySuffixes = new ArrayList<String>();
	public static String companySuffixesRegex = "";
	static {
		companySuffixes.add("ASSC");
		companySuffixes.add("ASSN");
		companySuffixes.add("ASSOC");
		companySuffixes.add("BK");
		companySuffixes.add("C P A");
		companySuffixes.add("CO");
		companySuffixes.add("COMP");
		companySuffixes.add("S");
		companySuffixes.add("D");
		companySuffixes.add("F");
		companySuffixes.add("DBA");
		companySuffixes.add("DDS");
		companySuffixes.add("DF");
		companySuffixes.add("G.P.");
		companySuffixes.add("G. P.");
		companySuffixes.add("G.P");
		companySuffixes.add("G. P");
		companySuffixes.add("HUD");
		companySuffixes.add("INC");
		companySuffixes.add("L.P.");
		companySuffixes.add("L.P");
		companySuffixes.add("L P");
		companySuffixes.add("LL");
		companySuffixes.add("LLC");
		companySuffixes.add("LLC.");
		companySuffixes.add("L.L.C");
		companySuffixes.add("LLP");
		companySuffixes.add("L/T");
		companySuffixes.add("L/E");
		companySuffixes.add("LND");
		companySuffixes.add("LP");
		companySuffixes.add("LTD");
		companySuffixes.add("LTM");
		companySuffixes.add("LMT");
		companySuffixes.add("M.D");
		companySuffixes.add("MGMT");
		companySuffixes.add("MGT");
		companySuffixes.add("MD");
		companySuffixes.add("MKT");
		companySuffixes.add("MGMNT");
		companySuffixes.add("N. A.");
		companySuffixes.add("N.A.");
		companySuffixes.add("N. A");
		companySuffixes.add("N.A");
		companySuffixes.add("NA");
		companySuffixes.add("N A");
		companySuffixes.add("NATLBK");
		companySuffixes.add("P.C");
		companySuffixes.add("PA");
		companySuffixes.add("P A");
		companySuffixes.add("P.A");
		companySuffixes.add("P.A.");
		companySuffixes.add("PAT");
		companySuffixes.add("PC");
		companySuffixes.add("PI");
		companySuffixes.add("PIT");
		companySuffixes.add("PLC");
		companySuffixes.add("PLLC");
		companySuffixes.add("POA");
		companySuffixes.add("PPT");
		companySuffixes.add("PTN");
		companySuffixes.add("PRTNSHP");
		companySuffixes.add("PTNRS");
		companySuffixes.add("PTNRSHIP.");
		companySuffixes.add("PTNRSHIP");
		companySuffixes.add("PTNRSHP");
		companySuffixes.add("PTNSHP");
		companySuffixes.add("PTNERS");
		companySuffixes.add("QTIP");
		companySuffixes.add("R.C");
		companySuffixes.add("REC");
		companySuffixes.add("RES");
		companySuffixes.add("R/T");
		companySuffixes.add("SN");
		companySuffixes.add("SPA");
		companySuffixes.add("ST");
		companySuffixes.add("SUPP");
		companySuffixes.add("SVC");
		companySuffixes.add("SVCS");
		companySuffixes.add("SYS");
		companySuffixes.add("TIC");
		companySuffixes.add("TIITF");
		companySuffixes.add("TR");
		companySuffixes.add("TRS");
		companySuffixes.add("TRST");
		companySuffixes.add("TRUT");
		companySuffixes.add("TTEES");
		companySuffixes.add("TRU");
		companySuffixes.add("TRUS");
		companySuffixes.add("UP");
		companySuffixes.add("US");
		companySuffixes.add("U S");
		companySuffixes.add("U.S.");
		companySuffixes.add("U. S.");
		companySuffixes.add("UT");
		companySuffixes.add("VLG");
		companySuffixes.add("L L C");
		companySuffixes.add("LP");
		companySuffixes.add("RW");
		companySuffixes.add("RWY");
		companySuffixes.add("PLG");
		companySuffixes.add("T V A");
		companySuffixes.add("MPHS");
		companySuffixes.add("I");
		companySuffixes.add("II");
		companySuffixes.add("III");
		companySuffixes.add("IV");
		companySuffixes.add("V");
		companySuffixes.add("VI");
		companySuffixes.add("VII");
		companySuffixes.add("VIII");
		companySuffixes.add("IX");
		companySuffixes.add("X");
		companySuffixes.add("XI");
		companySuffixes.add("XII");
		companySuffixes.add("XIII");
		companySuffixes.add("XIV");
		companySuffixes.add("XV");
		
		companySuffixesRegex = "\\b(";
		for(String suffix : companySuffixes) {
			suffix = suffix.replaceAll("\\.", "\\\\.");
			suffix = suffix.replaceAll(" ", "\\\\s");
			companySuffixesRegex += "|"+suffix;
		}
		companySuffixesRegex = companySuffixesRegex.replaceFirst("\\|", "");
		companySuffixesRegex += ")\\b";
	}
	public static Pattern companySuffixesPattern = Pattern.compile(companySuffixesRegex,Pattern.CASE_INSENSITIVE);
	
	public Company() { 
	}
	
	public Company(DatabaseData data, int row) {

		setID(data, row);
		setNAME(data, row);
		setNAMEEXT(data, row);
		setSTREETADR(data, row);
		setCITY(data, row);
		setCOUNTY(data, row);
		setSTATE(data, row);
		setZIP(data, row);
		setFAX(data, row);
		setEMAIL(data, row);
		setPHONE1(data, row);
		setPHONE2(data, row);
		setFILENO(data, row);
	}

	//ID = 0;
	public BigDecimal getID() {
		return (BigDecimal) getAttribute(ID);
	}

	public void setID(DatabaseData data, int row) {
		setAttribute(ID, data, COMPANY_ID, row);
	}

	public void setID(BigDecimal value) {
		setAttribute(ID, value);
	}

	//NAME = 1;
	public String getNAME() {
		return transformNull((String) getAttribute(NAME));
	}

	public void setNAME(DatabaseData data, int row) {
		setAttribute(NAME, data, COMPANY_NAME, row);
	}

	public void setNAME(String value) {
		setAttribute(NAME, value);
	}

	//NAMEEXT = 2;
	public String getNAMEEXT() {
		return transformNull((String) getAttribute(NAMEEXT));
	}

	public void setNAMEEXT(DatabaseData data, int row) {
		setAttribute(NAMEEXT, data, COMPANY_NAMEEXT, row);
	}

	public void setNAMEEXT(String value) {
		setAttribute(NAMEEXT, value);
	}

	//STREETADR = 3;
	public String getSTREETADR() {
		return transformNull((String) getAttribute(STREETADR));
	}

	public void setSTREETADR(DatabaseData data, int row) {
		setAttribute(STREETADR, data, COMPANY_STREETADR, row);
	}

	public void setSTREETADR(String value) {
		setAttribute(STREETADR, value);
	}

	//CITY = 4;
	public String getCITY() {
		return transformNull((String) getAttribute(CITY));
	}

	public void setCITY(DatabaseData data, int row) {
		setAttribute(CITY, data, COMPANY_CITY, row);
	}

	public void setCITY(String value) {
		setAttribute(CITY, value);
	}

	//COUNTY = 5;
	public BigDecimal getCOUNTY() {
		return (BigDecimal) getAttribute(COUNTY);
	}

	public void setCOUNTY(DatabaseData data, int row) {
		setAttribute(COUNTY, data, COMPANY_COUNTY, row);
	}

	public void setCOUNTY(BigDecimal value) {
		setAttribute(COUNTY, value);
	}

	//STATE = 6;
	public BigDecimal getSTATE() {
		return (BigDecimal) getAttribute(STATE);
	}

	public void setSTATE(DatabaseData data, int row) {
		setAttribute(STATE, data, COMPANY_STATE, row);
	}

	public void setSTATE(BigDecimal value) {
		setAttribute(STATE, value);
	}

	//ZIP = 7;
	public String getZIP() {
		return transformNull((String) getAttribute(ZIP));
	}

	public void setZIP(DatabaseData data, int row) {
		setAttribute(ZIP, data, COMPANY_ZIP, row);
	}

	public void setZIP(String value) {
		setAttribute(ZIP, value);
	}

	//FAX = 8;
	public String getFAX() {
		return transformNull((String) getAttribute(FAX));
	}

	public void setFAX(DatabaseData data, int row) {
		setAttribute(FAX, data, COMPANY_FAX, row);
	}

	public void setFAX(String value) {
		setAttribute(FAX, value);
	}

	//EMAIL = 9;
	public String getEMAIL() {
		return transformNull((String) getAttribute(EMAIL));
	}

	public void setEMAIL(DatabaseData data, int row) {
		setAttribute(EMAIL, data, COMPANY_EMAIL, row);
	}

	public void setEMAIL(String value) {
		setAttribute(EMAIL, value);
	}

	//PHONE1 = 10;
	public String getPHONE1() {
		return transformNull((String) getAttribute(PHONE1));
	}

	public void setPHONE1(DatabaseData data, int row) {
		setAttribute(PHONE1, data, COMPANY_PHONE1, row);
	}

	public void setPHONE1(String value) {
		setAttribute(PHONE1, value);
	}

	//PHONE2 = 11;
	public String getPHONE2() {
		return transformNull((String) getAttribute(PHONE2));
	}

	public void setPHONE2(DatabaseData data, int row) {
		setAttribute(PHONE2, data, COMPANY_PHONE2, row);
	}

	public void setPHONE2(String value) {
		setAttribute(PHONE2, value);
	}

	//FILENO = 12
	public String getFILENO() {
		return transformNull((String) getAttribute(FILENO));
	}

	public void setFILENO(DatabaseData data, int row) {
		setAttribute(FILENO, data, COMPANY_FILENO, row);
	}

	public void setFILENO(String value) {
		setAttribute(FILENO, value);
	}
}
