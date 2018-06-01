package ro.cst.tsearch.searchsites.client;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GWTDataSite implements IsSerializable, Cloneable {

	public static final int AO_TYPE = 0;
	public static final int TR_TYPE = 1;
	public static final int YA_TYPE = 2; 
	public static final int RO_TYPE = 3;
	public static final int DN_TYPE = 4;
	public static final int PA_TYPE = 5;
	public static final int CC_TYPE = 6;
	public static final int CO_TYPE = 7;
	public static final int PC_TYPE = 8;
	public static final int OR_TYPE = 9;
	public static final int MS_TYPE = 10;
	public static final int PR_TYPE = 11;
	public static final int PF_TYPE = 12;
	public static final int DT_TYPE = 13;
	public static final int LA_TYPE = 14;
	public static final int IS_TYPE = 15;
	public static final int IM_TYPE = 16;
	public static final int AK_TYPE = 17;
	public static final int DL_TYPE = 18; // DASL generic parent site
	public static final int RV_TYPE = 19; //Red vision 
	public static final int TU_TYPE = 20;
	public static final int AD_TYPE = 21;
	public static final int TS_TYPE = 22;
	public static final int SK_TYPE = 23;
	public static final int TP_TYPE = 24;
	public static final int NB_TYPE = 25;
	public static final int ST_TYPE = 26;
	public static final int SF_TYPE = 27;
	public static final int YB_TYPE = 28;
	public static final int YC_TYPE = 29;
	public static final int YD_TYPE = 30;
	public static final int YE_TYPE = 31;
	public static final int YF_TYPE = 32;
	public static final int PI_TYPE = 33;
	public static final int SB_TYPE = 34;
	public static final int DG_TYPE = 35;
	public static final int YG_TYPE = 36;
	public static final int R2_TYPE = 37;
	public static final int AC_TYPE = 38;
	public static final int HO_TYPE = 39;
	public static final int WP_TYPE = 40;
	public static final int LN_TYPE = 41;
	public static final int NR_TYPE = 42;
	public static final int DD_TYPE = 43;
	public static final int FD_TYPE = 44;
	public static final int DI_TYPE = 45;
	public static final int OI_TYPE = 46;	
	public static final int AM_TYPE = 47;
	public static final int DR_TYPE = 48;
	public static final int BS_TYPE = 49;
	public static final int	BT_TYPE = 50;
	public static final int	IL_TYPE = 51;
	public static final int	LW_TYPE	= 52;
	public static final int	MH_TYPE	= 53;
	public static final int PD_TYPE = 54;
	public static final int AOM_TYPE = 55;
	public static final int NA_TYPE = 56;
	public static final int ADI_TYPE = 57;
	public static final int ATI_TYPE = 58;
	public static final int MC_TYPE = 59;
	public static final int NETR_TYPE = 60;
	public static final int SPS_TYPE = 61;
	public static final int VU_TYPE = 62;
	public static final int NTN_TYPE = 63;
	public static final int ATS_TYPE = 64;
	public static final int SRC_TYPE = 65;
	public static final int MERS_TYPE = 66;
	public static final int GM_TYPE = 67;
	public static final int COM_TYPE = 68;
	public static final int DMV_TYPE = 69;
	public static final int BOR_TYPE = 70;
	public static final int PRI_TYPE = 71;
	public static final int TR2_TYPE = 72;
	public static final int RVI_TYPE = 73; //Red Vision Image
	
	/*
	 * If you add a new site please put it in the correct Bucket in SearchSitesBucket
	 * Also never remove this comment
	 */
	
	public static final int[] ASSESSOR_LIKE_SITES = {AM_TYPE, AO_TYPE, IS_TYPE, NB_TYPE, PRI_TYPE};
	
	public static final int[] TAX_LIKE_SITES = { TR_TYPE, TR2_TYPE, YA_TYPE, TU_TYPE, 
		YB_TYPE, YC_TYPE, YD_TYPE, YE_TYPE, YF_TYPE, YG_TYPE, 
		NTN_TYPE, // National TaxNet
		BOR_TYPE
	};
	public static final int[] COUNTYTAX_LIKE_SITES = {  TR_TYPE, TR2_TYPE, TU_TYPE, NTN_TYPE, BOR_TYPE};
	public static final int[] CITYTAX_LIKE_SITES = { YA_TYPE, YB_TYPE, YC_TYPE , YD_TYPE, YE_TYPE, YF_TYPE, YG_TYPE};
	public static final int[] RO_LIKE_TYPE = {
		RO_TYPE, 
		RV_TYPE, 
		DT_TYPE, 
		LA_TYPE,
		AD_TYPE, 
		TS_TYPE, 
		OR_TYPE, 
		TP_TYPE, 
		ST_TYPE,
		SK_TYPE,
		SF_TYPE, 
		R2_TYPE, 
		PI_TYPE, 
		AC_TYPE, 
		PC_TYPE, 
		SB_TYPE, 
		OI_TYPE, 
		HO_TYPE, 
		WP_TYPE, 
		LN_TYPE, 
		NR_TYPE, 
		DD_TYPE, 
		PD_TYPE, 
		ADI_TYPE, 
		DG_TYPE,
		ATI_TYPE,
		SRC_TYPE,
		GM_TYPE,
		IM_TYPE	//I need IM so  that I can number the images
											};
	
	public static final Integer[] REAL_RO_LIKE_TYPE = {
		RO_TYPE, 
		RV_TYPE, 
		DT_TYPE, 
		LA_TYPE,
		AD_TYPE, 
		TS_TYPE, 
		OR_TYPE, 
		TP_TYPE, 
		ST_TYPE,
		SK_TYPE,
		R2_TYPE, 
		PI_TYPE, 
		AC_TYPE, 
		ADI_TYPE, 
		DG_TYPE,
		ATI_TYPE,
		SRC_TYPE,
		CC_TYPE,
		NA_TYPE,
		IM_TYPE	
		};
	
	public static final List<Integer> REAL_RO_LIKE_TYPE_LIST;
	
	static{
		REAL_RO_LIKE_TYPE_LIST = Arrays.asList(REAL_RO_LIKE_TYPE);
	}
	
	/**
	 * This should contain only ROlike sites that will appear in the Image Count Report<br>
	 * Also the position is important since this is used to draw the table
	 * 
	 */
	public static final int[] RO_LIKE_IMAGE_COUNT = {
		RO_TYPE,
		ST_TYPE,
		R2_TYPE, 
		PI_TYPE, 
		AC_TYPE, 
		PC_TYPE,
		RV_TYPE, 
		DT_TYPE, 
		LA_TYPE, 
		TS_TYPE, 
		OR_TYPE, 
		TP_TYPE,
		SK_TYPE,
		DG_TYPE,
		ATI_TYPE,
		IM_TYPE,
		SF_TYPE
	};
	
	public static final int HTTP_CONNECTION = 2;
	public static final int HTTP_CONNECTION_2 = 3;
	public static final int HTTP_CONNECTION_3 = 4;
	
	public static final int NUMBER_OF_TAX_YEARS = 1;
	
	protected String community = "";
	
//	protected int commId = -1;
	
	protected String stateAbrv = "";

	protected String countyName = "";
	
	protected int countyId;
	
	protected String siteTypeAbrv = "";
	protected int siteType;

	protected String parserFileNameSuffix = "";

	protected String classFilename = "";

	protected String classConnFilename = "";

	protected String link = "";

	protected int maxSessions = 0;

	protected int connectionTimeout = -1;

	protected int searchTimeout = 900000;

	protected int timeBetweenRequests = 0;

	protected int absTimeBetweenRequests = 0;

	protected int maxRequestsPerSecond = 0;

	protected int units = 1000;

	protected int connType = HTTP_CONNECTION_3;
	
	protected  int type = -1;

	protected boolean isNewRow = false;

	// for password inheritance
	protected String passwordCode = "";

	// position in automatic
	protected int autpos = 0;

	protected String adressToken = Util.DEFAULT_ADDRESS_TOKEN;

	protected String adressTokenMiss = Util.DEFAULT_ADDRESS_TOKEN_MISS;

	protected String defaultCertificationDateOffset = null;
	protected String alternateLink = null;
	protected Date effectiveStartDate = null;
	
	protected String docType = null;
	
	protected int numberOfYears = 0;
	
	private Map<Integer, Integer> communityActivation;
	
	int	siteCertified = 0;

	public String toString() {
		return stateAbrv + countyName + siteTypeAbrv + "\n"
				+ parserFileNameSuffix + " " + classFilename + "\n" + link
				+ " " + connectionTimeout;
	}

	@Override
	public int hashCode() {
		return (countyName + siteTypeAbrv + stateAbrv).hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!((o instanceof GWTDataSite)||(o instanceof TaxSiteData))) {
			return false;
		}
		GWTDataSite dat = (GWTDataSite) o;
		return (dat.countyName.equals(countyName)
				&& dat.getSiteTypeAbrv().equals(siteTypeAbrv) && dat
				.getStateAbrv().equals(stateAbrv));
	}
	
	public boolean equals(GWTCommunitySite communitySite) {
		if(communitySite == null) {
			return false;
		}
		return communitySite.getCountyId() == countyId
				&& communitySite.getSiteTypeAbrev().equals(siteTypeAbrv)
				;
	}

	// must be used just for equals objects
	public boolean isChanged(Object o) {
		if (!(o instanceof GWTDataSite)) {
			return true;
		}
		GWTDataSite dat = (GWTDataSite) o;
		return ((!dat.parserFileNameSuffix.equals(parserFileNameSuffix))
				|| (!dat.classFilename.equals(classFilename))
				|| (!dat.classConnFilename.equals(classConnFilename))
				|| (!dat.link.equals(link)) || (dat.maxSessions != maxSessions)
				|| (dat.connectionTimeout != connectionTimeout)
				|| (dat.searchTimeout != searchTimeout)
				|| (dat.timeBetweenRequests != timeBetweenRequests)
				|| (dat.maxRequestsPerSecond != maxRequestsPerSecond)
//				|| (dat.connType != connType)
				|| (dat.absTimeBetweenRequests != absTimeBetweenRequests)
				|| (dat.siteCertified != siteCertified)
				|| (	
						(dat.effectiveStartDate != null && !dat.effectiveStartDate.equals(effectiveStartDate)) || 
						(dat.effectiveStartDate == null && effectiveStartDate != null) 
					)
		);
	}

	public static class CountyNameComparator implements Comparator<GWTDataSite> {
		public int compare(GWTDataSite o1, GWTDataSite o2) {
			if(o1.countyName.compareTo(o2.countyName) == 0)
				return new Integer(o1.autpos).compareTo(o2.autpos);
			
			return o1.countyName.compareTo(o2.countyName);
		}
	}
	public static class CommunityComparator implements Comparator<GWTDataSite> {
		public int compare(GWTDataSite o1, GWTDataSite o2) {
			return o1.community.compareTo( o2.community);
		}
	}
	
	public static class StateAbrvComparator implements Comparator<GWTDataSite> {
		public int compare(GWTDataSite o1, GWTDataSite o2) {
			return o1.stateAbrv.compareTo( o2.stateAbrv );
		}
	}

	public static class SiteAbrvComparator implements Comparator<GWTDataSite> {
		public int compare(GWTDataSite o1, GWTDataSite o2) {
			return o1.siteTypeAbrv.compareTo( o2.siteTypeAbrv );
		}
	}

	public static class ParserFilenameComparator implements Comparator<GWTDataSite> {
		public int compare(GWTDataSite o1, GWTDataSite o2) {
			return  o1.parserFileNameSuffix.compareTo(o2.parserFileNameSuffix);
		}
	}

	public static class LinkComparator implements Comparator<GWTDataSite> {
		public int compare(GWTDataSite o1, GWTDataSite o2) {
			return o1.link.compareTo(o2.link);
		}
	}

	public static class ClassFilenameComparator implements Comparator<GWTDataSite> {
		public int compare(GWTDataSite o1, GWTDataSite o2) {
			return o1.classFilename.compareTo(o2.classFilename);
		}
	}

	public static class ClassConnFilenameComparator implements Comparator<GWTDataSite> {
		public int compare(GWTDataSite o1, GWTDataSite o2) {
			return o1.classConnFilename.compareTo(o2.classConnFilename);
		}
	}

	public static class AutomaticOrderComparator implements Comparator<GWTDataSite> {
		public int compare(GWTDataSite o1, GWTDataSite o2) {
			if(o1.getAutpos() > o2.getAutpos()) {
				return 1;
			} else if(o1.getAutpos() < o2.getAutpos()) {
				return -1;
			} else if(o1.getType() > o2.getType()) {
				return 1;
			} else if(o1.getType() < o2.getType()) {
				return -1;
			} else {
				return 0;
			}
			
		}
	}

	public GWTDataSite() {
		super();
		communityActivation = new HashMap<Integer, Integer>();
	}

	public GWTDataSite(String stateAbrv, String countyName, String siteTypeAbrv,
			boolean isNewentry, int countyId) {
		this(stateAbrv, countyName, siteTypeAbrv, "", "", "", "", 0, 30000,
				900000, 0, 1000, countyId);
		this.isNewRow = isNewentry;
	}

	public GWTDataSite(String stateAbrv, String countyName, String siteTypeAbrv, int countyId) {
		this(stateAbrv, countyName, siteTypeAbrv, "", "", "", "", 0, 30000,
				900000, 0, 1000, countyId);
	}

	protected GWTDataSite(String stateAbrv, String countyName, String siteTypeAbrv,
			String parserFileNameSuffix, String classFilename,
			String classConnfileName, String link, int maxSessions,
			int connectionTimeout, int searchTimeout, int maxRequestsPerSecond,
			int units, int countyId) {
		this();
		this.stateAbrv = stateAbrv;
		this.countyName = countyName;
		this.siteTypeAbrv = siteTypeAbrv;
		this.parserFileNameSuffix = parserFileNameSuffix;
		this.classFilename = classFilename;
		this.classConnFilename = classConnfileName;
		this.link = link;
		this.maxSessions = maxSessions;
		this.connectionTimeout = connectionTimeout;
		this.searchTimeout = searchTimeout;
		this.maxRequestsPerSecond = maxRequestsPerSecond;
//		this.enabled = enabled;
		this.countyId = countyId;
	}

	public GWTDataSite(String selectedCommunityValue, String state, String county, String type2, boolean b, int countyId) {
		this(state,county,type2,b, countyId);
		this.community = selectedCommunityValue;
	}

	public String getClassFilename() {
		return classFilename;
	}

	public void setClassFilename(String classFilename) {
		this.classFilename = classFilename;
	}

	public String getParserFileNameSuffix() {
		return parserFileNameSuffix;
	}

	public void setParserFileNameSuffix(String parserFileNameSuffix) {
		this.parserFileNameSuffix = parserFileNameSuffix;
	}

	public String getCountyName() {
		return countyName;
	}

	public void setCountyName(String countyName) {
		this.countyName = countyName;
	}

	public String getSiteTypeAbrv() {
		return siteTypeAbrv;
	}

	public void setSiteTypeAbrv(String siteTypeAbrv) {
		this.siteTypeAbrv = siteTypeAbrv;
	}

	public String getStateAbrv() {
		return stateAbrv;
	}

	public void setStateAbrv(String stateAbrv) {
		this.stateAbrv = stateAbrv;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getClassConnFilename() {
		return classConnFilename;
	}

	public void setClassConnFilename(String classConnFilename) {
		this.classConnFilename = classConnFilename;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getMaxRequestsPerSecond() {
		return maxRequestsPerSecond;
	}

	public void setMaxRequestsPerSecond(int maxRequestsPerSecond) {
		this.maxRequestsPerSecond = maxRequestsPerSecond;
	}

	public int getMaxSessions() {
		return maxSessions;
	}

	public void setMaxSessions(int maxSessions) {
		this.maxSessions = maxSessions;
	}

	public int getSearchTimeout() {
		return searchTimeout;
	}

	public void setSearchTimeout(int searchTimeout) {
		this.searchTimeout = searchTimeout;
	}

	public int getUnits() {
		return units;
	}

	public void setUnits(int units) {
		this.units = units;
	}

	public String getPasswordCode() {
		return passwordCode;
	}

	public void setPasswordCode(String passwordCode) {
		this.passwordCode = passwordCode;
	}

	public int getTimeBetweenRequests() {
		return timeBetweenRequests;
	}

	public void setTimeBetweenRequests(int timeBetweenRequests) {
		this.timeBetweenRequests = timeBetweenRequests;
	}

	public int getAbsTimeBetweenRequests() {
		return absTimeBetweenRequests;
	}

	public void setAbsTimeBetweenRequests(int absTimeBetweenRequests) {
		this.absTimeBetweenRequests = absTimeBetweenRequests;
	}

	public int getAutpos() {
		return autpos;
	}

	public void setAutpos(int autpos) {
		this.autpos = autpos;
	}

	public String getAdressToken() {
		return adressToken;
	}

	public void setAdressToken(String adressToken) {
		this.adressToken = adressToken;
	}

	public String getAdressTokenMiss() {
		return adressTokenMiss;
	}

	public void setAdressTokenMiss(String adressTokenMiss) {
		this.adressTokenMiss = adressTokenMiss;
	}
	
	public String getDefaultCertificationDateOffset() {
		return defaultCertificationDateOffset;
	}

	public void setDefaultCertificationDateOffset(String certificationDate) {
		this.defaultCertificationDateOffset = certificationDate;
	}
	
	public String getAlternateLink() {
		return alternateLink;
	}

	public void setAlternateLink(String alternateLink) {
		this.alternateLink = alternateLink;
	}

	public boolean isNewRow() {
		return isNewRow;
	}

	public void setNewRow(boolean isNewRow) {
		this.isNewRow = isNewRow;
	}

	public void setConnType(int connType) {
		this.connType = connType;
	}

	public int getConnType() {
		return connType;
	}
	
	public boolean isAssessorLikeSite(){
		return isAssessorLike(type);
	}
	
	public boolean isTaxLikeSite(){
		return isTaxLike(type);
	}

	public boolean isRoLikeSite(){
		return isRoLike(type);
	}
	
	public boolean isRealRoLikeSite(){
		return isRealRoLike(type);
	}
	
	public  int getType() {
		return type;
	}

	public  void setType(int type) {
		this.type = type;
	}
	
	public String getCommunity() {
		return community;
	}

	public void setCommunity(String community) {
		this.community = community;
	}

	
	public static boolean isAssessorLike(int type){
		for(int i=0; i < GWTDataSite.ASSESSOR_LIKE_SITES.length; i++){
			if(type == GWTDataSite.ASSESSOR_LIKE_SITES[i]){
				return true;
			}
		}
		return false;
	}
	
	public static boolean isTaxLike(int type){
		for(int i=0;i<GWTDataSite.TAX_LIKE_SITES.length;i++){
			if(type == GWTDataSite.TAX_LIKE_SITES[i]){
				return true;
			}
		}
		return false;
	}
	
	public static boolean isCountyTaxLike(int type){
		for(int i = 0; i < GWTDataSite.COUNTYTAX_LIKE_SITES.length; i++){
			if (type == GWTDataSite.COUNTYTAX_LIKE_SITES[i]){
				return true;
			}
		}
		return false;
	}
	
	public static boolean isCityTaxLike(int type){
		for(int i = 0; i < GWTDataSite.CITYTAX_LIKE_SITES.length; i++){
			if (type == GWTDataSite.CITYTAX_LIKE_SITES[i]){
				return true;
			}
		}
		return false;
	}
	
	public static boolean isRoLike(int type){
		for(int i=0;i<RO_LIKE_TYPE.length;i++){
			if(RO_LIKE_TYPE[i]==type ){
				return true;
			}
		}
		return false;
	}
	
	public static boolean isRealRoLike(int type){
		for (int i = 0; i < REAL_RO_LIKE_TYPE.length; i++){
			if (REAL_RO_LIKE_TYPE[i] == type){
				return true;
			}
		}
		return false;
	}

	public static boolean isATSSite(int type){
		if (ATS_TYPE == type){
			return true;
		}
		return false;
	}
	
	public static boolean isSSFSite(int type){
		if (SF_TYPE == type){
			return true;
		}
		return false;
	}
	public static boolean isPASite(int type){
		if (PA_TYPE == type){
			return true;
		}
		return false;
	}
	
	public Date getEffectiveStartDate() {
		return effectiveStartDate;
	}
	
	public String getEffectiveStartDateString() {
		if(effectiveStartDate == null) {
			return "";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		return sdf.format(effectiveStartDate);
	}

	public void setEffectiveStartDate(Date effectiveStartDate) {
		this.effectiveStartDate = effectiveStartDate;
	}

	public int getNumberOfYears(){
		if ((isAssessorLikeSite() || isTaxLikeSite()) && numberOfYears == 0){
			numberOfYears = 1;
		}
		return numberOfYears;
	}
	
	public void setNumberOfYears(int numberOfYears){
		this.numberOfYears = numberOfYears;
	}
	
	public String getDocType() {
		return docType;
	}
	
	public String getDocTypeToShow() {
		if(docType == null) {
			return "Default";
		}
		return docType;
	}

	public void setDocType(String docType) {
		this.docType = docType;
	}

	public void setSiteCertified(int siteEnabled) {
		this.siteCertified = siteEnabled;
	}
	
	public int getSiteCertified() {
		return this.siteCertified;
	}
	public int getCountyId() {
		return countyId;
	}
	public void setCountyId(int countyId) {
		this.countyId = countyId;
	}
	
	public Map<Integer, Integer> getCommunityActivation() {
		return communityActivation;
	}
	public void setSiteType(int siteType) {
		this.siteType = siteType;
	}
	public int getSiteType() {
		return siteType;
	}
}
