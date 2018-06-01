package ro.cst.tsearch.servers.bean;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.searchsites.client.TaxSiteData;
import ro.cst.tsearch.searchsites.client.Util;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.connection.domain.ConnectionSettings;


public class DataSite implements Comparable<DataSite>, ParameterizedRowMapper<DataSite>, Cloneable {
	
	protected static final Category logger = Logger.getLogger(DataSite.class); 

	private ConnectionSettings connSettings;
	
	private String cityName = null;
	
	private Date payDate = null;
	
	private Date dueDate = null;
	
	private int taxYearMode = TaxSiteData.TAX_YEAR_PD_YEAR;
	
	public static final int HTTP_CONNECTION = 2;

	public static final int HTTP_CONNECTION_2 = 3;
	
	public static final int HTTP_CONNECTION_3 = 4;

	private int connType = HTTP_CONNECTION;

	private int countyId = 0;
	
	private String countyFIPS = "";

	private String stateFIPS = "";

	//private String name = "";
	private String stateAbbreviation = "";
	private String countyName = "";
	
	private String P2 = "";

	private String index = "";

	/**
	 * This is really siteType as it is found in database (for example 25 for NB)
	 */
	private int cityChecked; // site type

	private String parserFilenameSufix = "";

	private String tsServerClassName = "";

	private String classConnFilename = "";

	private String passwordCode = "";

//	private Integer enabled = 0;
	
	/**
	 * used for starter type, holds certified field
	 */
	private Integer siteEnabled = 0;

	private int autpos = 0;
	
//	private int commId = 0;
	
	private Map<Integer, Integer> communityActivation;

	private int searchTimeout = -1;
	
	private Date effectiveStartDate = null;

	private String adressToken     = Util.DEFAULT_ADDRESS_TOKEN;
	private String adressTokenMiss = Util.DEFAULT_ADDRESS_TOKEN_MISS;
	
	private String alternateLink = null;
	private String docType = null;
	
	private int numberOfYears = 0;
	
	public DataSite() {
		connSettings = new ConnectionSettings();
		communityActivation = new HashMap<Integer, Integer>();
	}
	
	public String getStateAbrev(){
		return stateAbbreviation;
	}
	
	/**
	 * 
	 * @return County Name (e.g. Hillsborough, Bexar ... )
	 */
	public String getCountyName(){
		return countyName;
	}
	
	/**
	 * Returns the siteType abbreviation (ex: NB, AO, RO)
	 * @return
	 */
	public String getSiteTypeAbrev(){
		return HashCountyToIndex.getServerAbbreviationByType(getSiteTypeInt());
	}
	
	public String getSiteDescription(){
		return HashCountyToIndex.getServerDescriptionByType(getSiteTypeInt());
	}
	
	public GWTDataSite toSiteData() {
		
		String siteTypeAbrev = getSiteTypeAbrev();
		
		GWTDataSite data = null;
		if(isTaxLikeSite()){
			data = new TaxSiteData(stateAbbreviation, countyName, siteTypeAbrev, countyId);
		} else {
			data = new GWTDataSite(stateAbbreviation, countyName, siteTypeAbrev, countyId);
		}
		
		data.setSiteType(getSiteType());
		data.setParserFileNameSuffix(parserFilenameSufix);
		data.setClassFilename(tsServerClassName);
		data.setClassConnFilename(classConnFilename);
		data.setLink(connSettings.getBaseLink());
		data.setMaxSessions(connSettings.getMaxSessions());
		data.setConnectionTimeout(connSettings.getConnectionTimeout());
		data.setSearchTimeout(searchTimeout);
		data.setMaxRequestsPerSecond(connSettings.getMaxRequestsPerTimeInterval());
		data.setUnits(connSettings.getTimeInterval());
		data.setPasswordCode(passwordCode);
		data.setTimeBetweenRequests(connSettings.getTimeBetweenRequests());
		data.setAdressToken(adressToken);
		data.setAdressTokenMiss(adressTokenMiss);
		data.setAlternateLink(alternateLink);
		data.setAutpos(autpos);
		data.setConnType(connType);
		data.setAbsTimeBetweenRequests(connSettings.getAbsTimeBetweenRequests());
		data.setType(getSiteType());
		data.setEffectiveStartDate(getEffectiveStartDate());
		data.setDocType(getDocType());
		data.setSiteCertified(siteEnabled);
		
		if(data instanceof TaxSiteData){
			TaxSiteData taxSiteData = (TaxSiteData)data;
			taxSiteData.setCityName(getCityName());
			SimpleDateFormat sdf = new SimpleDateFormat ("MM/dd/yyyy");
			if(payDate!=null){
				taxSiteData.setPayDate(sdf .format(payDate));
			}
			if(dueDate!=null){
				taxSiteData.setDueDate(sdf .format(dueDate));
			}
			taxSiteData.setTaxYearMode(getTaxYearMode());
			taxSiteData.setNumberOfYears(numberOfYears);
		} else if (data.isAssessorLikeSite()){
			data.setNumberOfYears(numberOfYears);
		}
		data.getCommunityActivation().putAll(getCommunityActivation());
		
		return data;
	}
	
	public String getIndex() {
		return index;
	}

	/**
	 * Returns the name as STCountynameAbr<br>
	 * ex: FLOrangeDT
	 * @return name as STCountynameAbr
	 */
	public String getName() {
		return stateAbbreviation + countyName + getSiteTypeAbrev();
	}

	public String getP2() {
		return P2;
	}

	public void setP2(String p2) {
		P2 = p2;
	}

	public int getCityChecked() {
		return cityChecked;
	}
	/**
	 * siteType as it is found in database (for example 25 for NB)
	 * @return siteType
	 */
	public int getSiteType() {
		return getCityChecked();
	}
	
	/**
	 * siteType as it is found in database (for example 25 for NB)
	 * @return siteType
	 */
	public int getSiteTypeInt() {
		return getCityChecked();
	}

	public String getTsServerClassName() {
		return tsServerClassName;
	}

	public String getParserFilenameSufix() {
		return parserFilenameSufix;
	}

	public String getClassConnFilename() {
		return classConnFilename;
	}

	public void setClassConnFilename(String classConnFilename) {
		this.classConnFilename = classConnFilename;
	}

	public int getConnectionTimeout() {
		return connSettings.getConnectionTimeout();
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connSettings.setConnectionTimeout(connectionTimeout) ;
	}

	public Integer getEnabled(int commId) {
		Integer enabled = communityActivation.get(commId);
		if(enabled == null) {
			return 0;
		}
		return enabled;
	}

	public void setEnabled(Integer enabled, int commId) {
		communityActivation.put(commId, enabled);
	}
	
	/**
	 * Get certified flag value
	 * @return
	 */
	public Integer getSiteEnabled() {
		return siteEnabled;
	}

	/**
	 * Set certified value
	 * @param enabled
	 */
	public void setSiteEnabled(Integer enabled) {
		this.siteEnabled = enabled;
	}

	public String getLink() {
		return connSettings.getBaseLink().trim();
	}
	/**
	 * For a link like <i>http://www.gsasa.dsa.com/somepage</i> the method returns <b>http://www.gsasa.dsa.com/</b>
	 * @return the home link to the server
	 */
	public String getServerHomeLink() {
		String fullLink = connSettings.getBaseLink();
		int endIndex = 0;
		if(fullLink.startsWith("http")) {
			endIndex = fullLink.indexOf("/", 8);
			fullLink = fullLink.substring(0,endIndex + 1);
		}
		return fullLink;
	}
	
	/**
	 * For a link like <i>http://www.gsasa.dsa.com/somepage/login.php</i> the method returns <b>http://www.gsasa.dsa.com/somepage/</b>
	 * @return 
	 */
	public String getServerRelativeLink() {
		String fullLink = connSettings.getBaseLink();
		int endIndex = 0;
		if(fullLink.startsWith("http")) {
			endIndex = fullLink.lastIndexOf("/");
			if(endIndex > 7) {
				fullLink = fullLink.substring(0,endIndex + 1);
			}
			if(!fullLink.endsWith("/")) {
				fullLink += "/";
			}
		}
		return fullLink;
	}
	
	/**
	 * For a link like <i>http://www.gsasa.dsa.com/somepage/some_other_page.php</i> the method returns <b>/somepage/some_other_page.php</b>
	 * @return the server destination page
	 */
	public String getServerDestinationPage() {
		String fullLink = connSettings.getBaseLink().replaceAll("https?://", "");
		int endIndex = fullLink.indexOf("/");
		if(endIndex > 0) {
			return fullLink.substring(endIndex).trim();
		}
		return "";
	}
	
	/**
	 * For a link like <i>http://www.gsasa.dsa.com/somepage/some_other_page.php</i> the method returns <b>www.gsasa.dsa.com</b>
	 * @return the server address
	 */
	public String getServerAddress() {
		String fullLink = connSettings.getBaseLink().replaceAll("https?://", "");
		int endIndex = fullLink.indexOf("/");
		if(endIndex > 0) {
			return fullLink.substring(0,endIndex);
		}
		return "";
	}

	public void setLink(String link) {
		this.connSettings.setBaseLink(link) ;
	}

	public int getMaxRequestsPerSecond() {
		return connSettings.getMaxRequestsPerTimeInterval();
	}

	public void setMaxRequestsPerSecond(int maxRequestsPerSecond) {
		this.connSettings.setMaxRequestsPerTimeInterval(maxRequestsPerSecond);
	}

	public int getMaxSessions() {
		return connSettings.getMaxSessions();
	}

	public void setMaxSessions(int maxSessions) {
		this.connSettings.setMaxSessions(maxSessions);
	}

	public String getPasswordCode() {
		return passwordCode;
	}

	public void setPasswordCode(String passwordCode) {
		this.passwordCode = passwordCode;
	}

	public int getSearchTimeout() {
		return searchTimeout;
	}

	public void setSearchTimeout(int searchTimeout) {
		this.searchTimeout = searchTimeout;
	}

	public int getUnits() {
		return connSettings.getTimeInterval();
	}

	public void setUnits(int units) {
		this.connSettings.setUnit(units);
	}

	public void setCityChecked(int cityChecked) {
		this.cityChecked = cityChecked;
	}
	

	public void setIndex(String index) {
		this.index = index;
	}

	public void setParserFilenameSufix(String parserFilenameSufix) {
		this.parserFilenameSufix = parserFilenameSufix;
	}

	public void setTsServerClassName(String tsServerClassName) {
		this.tsServerClassName = tsServerClassName;
	}

	public int getTimeBetweenRequests() {
		return connSettings.getTimeBetweenRequests();
	}

	public void setTimeBetweenRequests(int timeBetweenRequests) {
		this.connSettings.setTimeBetweenRequests(timeBetweenRequests);
	}

	public int getAbsTimeBetweenRequests() {
		return connSettings.getAbsTimeBetweenRequests();
	}

	public void setAbsTimeBetweenRequests(int absTimeBetweenRequests) {
		this.connSettings.setAbsTimeBetweenRequests(absTimeBetweenRequests);
	}

	public String getAdressToken() {
		return adressToken;
	}
	
	public double [] getAddressTokenWeights(){
		return Util.createDoubleArrayFromString(getAdressToken());
	}

	public void setAdressToken(String adressToken) {
		this.adressToken = adressToken;
	}

	public String getAdressTokenMiss() {
		return adressTokenMiss;
	}
	
	public double [] getAddressTokenMissing(){
		return Util.createDoubleArrayFromString(getAdressTokenMiss());
	}

	public void setAdressTokenMiss(String adressTokenMiss) {
		this.adressTokenMiss = adressTokenMiss;
	}
	
	public String getAlternateLink() {
		return alternateLink;
	}

	public void setAlternateLink(String alternateLink) {
		this.alternateLink = alternateLink;
	}
	
	public String getDocumentServerLink() {
		if(StringUtils.isEmpty(getAlternateLink())) {
			return getLink();
		} 
		return getAlternateLink();
	}
	
	public int getAutpos() {
		return autpos;
	}

	public void setAutpos(int autpos) {
		this.autpos = autpos;
	}

	public String getCountyFIPS() {
		return countyFIPS;
	}

	public void setCountyFIPS(String countyFIPS) {
		this.countyFIPS = countyFIPS;
	}

	public String getStateFIPS() {
		return stateFIPS;
	}

	public void setStateFIPS(String stateFIPS) {
		this.stateFIPS = stateFIPS;
	}

	public void setConnType(int connType) {
		this.connType = connType;
	}

	public int getConnType() {
		return connType;
	}

	public boolean isAssessorSite(){
		return GWTDataSite.isAssessorLike(getSiteType());
	}
	
	public boolean isTaxLikeSite(){
		return GWTDataSite.isTaxLike( getSiteType() );
	}
	
	public boolean isCountyTaxLikeSite(){
		return GWTDataSite.isCountyTaxLike(getSiteType());
	}
	
	public boolean isCityTaxLikeSite(){
		return GWTDataSite.isCityTaxLike(getSiteType());
	}
	
	public boolean isRoLikeSite(){
		return TSServer.isRoLike( getSiteType() );
	}

	public boolean isATSSite(){
		return GWTDataSite.isATSSite(getSiteType());
	}
	
	public boolean isSSFSite(){
		return GWTDataSite.isSSFSite(getSiteType());
	}
	
	public boolean isPASite(){
		return GWTDataSite.isPASite(getSiteType());
	}
	
	public Date getPayDate() {
		return payDate;
	}

	public void setPayDate(Date payDate) {
		this.payDate = payDate;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}
	
	public String getCityName() {
		return cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public boolean isRoLikeForStarterOrders(){
		return HashCountyToIndex.getRolikeforstarterorders().contains(getSiteTypeAbrev());
	}
	
//	public int getCommId() {
//		return commId;
//	}
//
//	public void setCommId(int commId) {
//		this.commId = commId;
//	}

	public int getCountyId() {
		return countyId;
	}

	public void setCountyId(int countyId) {
		this.countyId = countyId;
	}

	public String getCountyIdAsString() {
		return Integer.toString(countyId);
	}
	
	@Override
	public int compareTo(DataSite dat) {

		if (getAutpos() > dat.getAutpos()) {
			return 1;
		} else if (getAutpos() < dat.getAutpos()) {
			return -1;
		} else if (getSiteTypeInt() > dat.getSiteTypeInt()) {
			return 1;
		} else if (getSiteTypeInt() < dat.getSiteTypeInt()) {
			return -1;
		} else {
			return 0;
		}
	}

	@Override
	public DataSite mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		String vec[] = HashCountyToIndex.getServerTypesAbrev();
		int dbCountyId = resultSet.getInt(DBConstants.FIELD_SITES_ID_COUNTY);
		int siteType = resultSet.getInt(DBConstants.FIELD_SITES_SITE_TYPE);
		int P2 = resultSet.getInt(DBConstants.FIELD_SITES_P2);
		int real_index_county = -1;
		try {
			real_index_county = resultSet.getInt(DBConstants.FIELD_COUNTY_REAL_INDEX_COUNTY);
		} catch (Exception e) {
			HashCountyToIndex.getLogger().error("Exception for county " + dbCountyId+ ". No real_index_county", e);
			throw new RuntimeException("COUNTY INDEX IS NULL");
		}
		String stateNameAbv = resultSet.getString(DBConstants.FIELD_STATE_ABV);
		String countyName = resultSet.getString(DBConstants.FIELD_COUNTY_NAME).replaceAll("&nbps;", " ");

		
		int connTimeOut = -1;
		int maxSess = 0;
		int searchTimeOut = 0;
		int tbr = 0;
		int mrps = 0;
		int units = 1000;
//		int is_enabled = 1;
		int numberOfYears = 1;

		String connClassName = "";
		try {
			connClassName = resultSet.getString("conn_class_name");
		} catch (Exception e) {
		}
		if (connClassName == null || "null".equals(connClassName)) {
			connClassName = "";
		}
		
		try {	connTimeOut = resultSet.getInt("connection_time_out");	} catch (Exception e) {}
		try {	searchTimeOut = resultSet.getInt("search_time_out");	} catch (Exception e) {}
		try {	tbr = resultSet.getInt("time_between_requests");		} catch (Exception e) {}
		try {	mrps = resultSet.getInt("max_requests_per_second");		} catch (Exception e) {}
		try {	units = resultSet.getInt("time_unit");					} catch (Exception e) {}
		try {	maxSess = resultSet.getInt("max_sess");					} catch (Exception e) {}
//		try {	
//			is_enabled = resultSet.getInt(DBConstants.FIELD_COMMUNITY_SITES_ENABLE_STATUS);
//		} catch (Exception e) {
//			logger.error("Error reading enable status",e);
//		}
		try {	numberOfYears = resultSet.getInt("number_of_years"); } catch (Exception e) {}
		
		String adressToken = resultSet.getString("adress_token");
		if (adressToken == null || adressToken.contains("null") || adressToken.isEmpty()) {
			adressToken = "1.0,1.0,1.0,1.0,1.0,1.0,1.0";
		}
		String adressTokenMiss = resultSet.getString("adress_token_miss");
		if (adressTokenMiss == null || adressTokenMiss.contains("null") || 	adressTokenMiss.isEmpty()) {
			adressTokenMiss = "0.0,0.0,0.0,0.0,0.0,0.0,0.0";
		}
		
		int autopos = 0;
		try {
			autopos = resultSet.getInt("automatic_position");
		} catch (Exception e) {
		}

		int connType = DataSite.HTTP_CONNECTION;
		try {
			connType = resultSet.getInt("conn_type");
		} catch (Exception e) {
		}

		int atbr = 0;
		try {
			atbr = resultSet.getInt("abs_time_between_requests");
		} catch (Exception e) {
		}

		String realIndexStr = null;
		Object fake = HashCountyToIndex.getIndexcountytoindexcity().get("0"+ real_index_county);
		if (fake != null && vec[siteType].equalsIgnoreCase("YA")) {
			realIndexStr = fake + "";
		} else {
			realIndexStr = "0" + real_index_county;
		}
		
		int siteEnabled = 0;
		
		try{
			siteEnabled = resultSet.getInt(DBConstants.FIELD_SITES_IS_ENDBLED);
		}catch(Exception e){
			e.printStackTrace();
		}

		DataSite dat = new DataSite();
		dat.setStateAbbreviation(stateNameAbv);
		dat.setCountyName(countyName);
		dat.setCityChecked(siteType);
		dat.setP2(String.valueOf(P2));
		dat.setIndex(realIndexStr);
		dat.setParserFilenameSufix(resultSet.getString("parser_file_name_suffix"));
		dat.setTsServerClassName(resultSet.getString("tsserver_class_name"));
		dat.setClassConnFilename(connClassName);
		dat.setConnectionTimeout(connTimeOut);
		dat.setTimeBetweenRequests(tbr);
		dat.setSearchTimeout(searchTimeOut);
		dat.setMaxRequestsPerSecond(mrps);
		dat.setUnits(units);
		dat.setMaxSessions(maxSess);
		dat.setLink(resultSet.getString("link"));
		dat.setAutpos(autopos);
//		dat.setEnabled(is_enabled);
		dat.setAdressToken(adressToken);
		dat.setAdressTokenMiss(adressTokenMiss);
		dat.setAlternateLink(resultSet.getString("alternateLink"));
		dat.setCountyFIPS(resultSet.getString("countyFIPS"));
		dat.setStateFIPS(resultSet.getString("stateFIPS"));
		dat.setConnType(connType);
		dat.setAbsTimeBetweenRequests(atbr);
		dat.setCountyId(dbCountyId);
//		dat.setCommId(resultSet.getInt(DBConstants.FIELD_COMMUNITY_SITES_COMMUNITY_ID));
		dat.setCityName(resultSet.getString("city_name"));
		dat.setSiteEnabled(siteEnabled);
		Object effectiveStartDate = resultSet.getObject(DBConstants.FIELD_SITES_EFFECTIVE_START_DATE);
		if(effectiveStartDate != null) {
			dat.setEffectiveStartDate((Date)effectiveStartDate);
		}
		
		String docType = null;
		try { docType = resultSet.getString(DBConstants.FIELD_COUNTY_DOCTYPE); } catch (Exception e) {}
		
		dat.setDocType(docType);
		dat.setNumberOfYears(numberOfYears);
		return dat;
	}
	
	public boolean isEnabledNameDerivation(int commId) {
		return Util.isSiteEnabledDerivation(getEnabled(commId));
	}
	public boolean isEnableSite(int commId) {
		return Util.isSiteEnabled(getEnabled(commId));
	}
	public boolean isEnabledNameBootstrapping(int commId) {
		return Util.isSiteEnabledNameBootstrap(getEnabled(commId));
	}
	public boolean isEnabledAddressBootstrapping(int commId) {
		return Util.isSiteEnabledAddressBootstrap(getEnabled(commId));
	}
	public boolean isEnabledLegalBootstrapping(int commId) {
		return Util.isSiteEnabledLegalBootstrap(getEnabled(commId));
	}
	
	public boolean isEnabledLinkOnly(int commId) {
		return Util.isSiteEnabledLinkOnly(getEnabled(commId));
	}
	
	/**
	 * 
	 * @param productId
	 * @param commId
	 * @return
	 */
	public boolean isEnabledAutomatic(int productId, int commId) {
		return Util.isSiteEnabledAutomaticForProduct(productId, getEnabled(commId));
	}
	
	/**
	 * Check if include document information in the final TSR is activated<br>
	 * Information should be image and if image is missing then document index 
	 * @param commId
	 * @return
	 */
	public boolean isEnabledIncludeInTsr(int commId) {
		return Util.isSiteEnabledIncludeInTsr(getEnabled(commId));
	}
	
	/**
	 * Check if include image is enabled
	 * @param commId
	 * @return
	 */
	public boolean isEnabledIncludeImageInTsr(int commId) {
		return Util.isSiteEnabledIncludeImageInTsr(getEnabled(commId));
	}
	
	public int getType() {
		return getSiteTypeInt();
	}

	@Override
	public DataSite clone() {
		try{
			return ( (DataSite)super.clone() );
		}
		catch(CloneNotSupportedException e){
			throw new RuntimeException(e);
		}
	}

	public Date getEffectiveStartDate() {
		return effectiveStartDate;
	}

	public void setEffectiveStartDate(Date effectiveStartDate) {
		this.effectiveStartDate = effectiveStartDate;
	}

	public int getTaxYearMode() {
		return taxYearMode;
	}

	public void setTaxYearMode(int taxYearMode) {
		this.taxYearMode = taxYearMode;
	}
	
	/**
	 * Returns a string formated like STCountyName where ST is State Abbreviation and CountyName is the name of the county
	 * @return the formatted STCountyName or empty if countyId is not valid
	 */
	public String getSTCounty() {
		return stateAbbreviation + countyName;
	}

	/**
	 * 
	 * @return State abbreviation (e.g: TX, CA, FL ... ) 
	 */
	public String getStateAbbreviation() {
		return stateAbbreviation;
	}

	public void setStateAbbreviation(String stateAbbreviation) {
		this.stateAbbreviation = stateAbbreviation;
	}

	public void setCountyName(String countyName) {
		this.countyName = countyName;
	}

	public String getDocType() {
		return docType;
	}

	public void setDocType(String docType) {
		this.docType = docType;
	}

	public int getNumberOfYears(){
		return numberOfYears;
	}
	
	public void setNumberOfYears(int numberOfYears){
		this.numberOfYears = numberOfYears;
	}
	public ConnectionSettings getConnSettings() {
		return connSettings;
	}

	/**
	 * Used like <code>is(CountyConstants.NV_Clark, GWTDataSite.TR_TYPE)</code> 
	 * @param countyId
	 * @param siteId
	 * @return
	 */
	public boolean is(int countyId, int siteId) {
		return getSiteTypeInt() == siteId && getCountyId() == countyId;
	}
	
	/**
	 * Returns the current tax year based on the pay date and the tax year mode<br>
	 * If pay date is null, it returns 0
	 * @return current tax year or 0 if pay date is not found
	 */
	public int getCurrentTaxYear() {
		
		if(payDate == null) {
			return 0;
		}
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(payDate);		
		int currentTaxYear = cal.get(Calendar.YEAR);
		
		if(taxYearMode == TaxSiteData.TAX_YEAR_PD_YEAR_MINUS_1) {
			currentTaxYear--;
		} else if(taxYearMode == TaxSiteData.TAX_YEAR_PD_YEAR_PLUS_1) {
			currentTaxYear++;
		}
		
		return currentTaxYear;
	}

	public int getCityCheckedInt() {
		return getSiteType();
	}
	
	public Map<Integer, Integer> getCommunityActivation() {
		return communityActivation;
	}
	
	public long getServerId() {
		return TSServersFactory.getSiteIdfromCountyandServerTypeId(getCountyId(), getSiteType() + 1);
	}
	
}
