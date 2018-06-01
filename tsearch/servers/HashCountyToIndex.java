
package ro.cst.tsearch.servers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityManager;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.NotImplFeatureException;
import ro.cst.tsearch.searchsites.client.GWTCommunitySite;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.searchsites.client.SearchSitesBuckets;
import ro.cst.tsearch.searchsites.client.SearchSitesBuckets.SearchSitesBucket;
import ro.cst.tsearch.searchsites.client.TaxSiteData;
import ro.cst.tsearch.searchsites.client.Util;
import ro.cst.tsearch.searchsites.client.Util.SiteDataCompact;
import ro.cst.tsearch.servers.bean.CommunitySite;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.bean.SiteTypeEntry;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.settings.Settings;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;

import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

public class HashCountyToIndex {
	
	private static final Category logger = Logger.getLogger(HashCountyToIndex.class);
	
	public static final int ANY_COMMUNITY = -1;
	public static final String ALL_STATE	= "ALL";
	public static final String ALL_COUNTY	= "ALL";
	public static final int ANY_PRODUCT	= -1;
	
	private static final HashMap<String, String> allCounties = new HashMap<String, String>();

	private static final HashMap<String, String> indexCountyToIndexCity = new HashMap<String, String>();

	private static final Map<String, DataSite> allSites = new HashMap<String, DataSite>();
	
	/**
	 * This map, hold info link this:
	 * [CountyID][Map[miServerId,DataSite]]
	 */
	private static final HashMap<Integer, HashMap<String, DataSite>> allSitesByLocation = 
		new HashMap<Integer, HashMap<String, DataSite>>();

	private static final HashMap<String, String> allCountiesInvertWithEPMapping = new HashMap<String, String>();

	public static List<SiteTypeEntry> treeSetSiteTypes = null;
	
	private static Map<String, Integer> serverAbbrevSiteTypeMap = null;
	private static Map<Integer, String> siteTypeServerAbbrevSiteTypeMap = null;
	private static Map<Integer, String> siteTypeServerDescriptionMap = null;
	
	private static final Map<String,TaxDateStructure> mapTaxDate = new HashMap<String, TaxDateStructure>();

	// does not contains EP because EP has 2 and TR the same 2
	private static final HashMap<Integer, Integer> mapP2ToSiteType = new HashMap<Integer, Integer>();

	private static final HashMap<Integer, Integer> mapSiteTypeToP2 = new HashMap<Integer, Integer>();
	
	private static final HashSet<String> roLikeForStarterOrders = new HashSet<String>(){
		private static final long serialVersionUID = 1L;
	{
		add("AD");
		add("CO");
		add("DT");
		add("DG");
		add("LA");
		add("RO");
		add("RV");
		add("TS");
		add("SK");
		add("AC");
		add("TP");
		add("SF");
		add("PI");
		add("ST");
	}};
	
	public static HashSet<String> getRolikeforstarterorders() {
		return roLikeForStarterOrders;
	}

	public static HashMap<String, String> getIndexcountytoindexcity() {
		return indexCountyToIndexCity;
	}

//	// first reference to HashCountyToIndex
//	static {
//		fillDataAboutAllImplementedCountyFromDatabase();
//	}

	public static class TaxDateStructure{
		Date 	dueDate;
		Date 	payDate;
		int		taxYearMode;
	}
	
	private static synchronized Map<String, DataSite> getAllSites() {
		return allSites;
	}
	
//	private static Map<String, DataSite> getAllSites(int commId) {
//		if(commId == ANY_COMMUNITY) {
//			commId = getAllSites().keySet().iterator().next();
//		}
//		Map<String, DataSite> sitesForCommunity = getAllSites().get(commId);
//		if(sitesForCommunity == null) {
//			fillAllSites(commId, false);
//			sitesForCommunity = getAllSites().get(commId);
//		}
//		return sitesForCommunity;
//	}
	
	
	/**
	 * Get all registered DataSite for a given commId and countyId. 
	 * @param commId
	 * @param countyId
	 * @return
	 */
	public static List<DataSite> getAllSites(int commId, int countyId) {
		Map<String, DataSite> sitesForCommunity = getAllSites();
		List<DataSite> sitesForCountyId = new LinkedList<DataSite>(); 
	
		List<Long> siteIds = TSServersFactory.getSiteIds(countyId);
		for (Long siteId : siteIds) {
			DataSite dataSite = sitesForCommunity.get(siteId.toString());
			if (dataSite!=null){
				sitesForCountyId.add(dataSite);
			}
		}
		return sitesForCountyId;
	}

	public static String[] getServerTypesDescription() {
		SiteTypeEntry o[] = treeSetSiteTypes.toArray(new SiteTypeEntry[0]);
		String[] str = new String[o.length];
		for (int i = 0; i < o.length; i++) {
			str[i] =  o[i].getDescription();
		}
		return str;
	}

	public static String[] getServerTypesAbrev() {
		Object o[] = treeSetSiteTypes.toArray();
		String[] str = new String[o.length];
		for (int i = 0; i < o.length; i++) {
			str[i] = ((SiteTypeEntry) o[i]).getSiteAbrev();
		}
		return str;
	}

	public static Long[] getImplementedSitesIDsAndFillHashMap(int commId, HashMap<Long, String> hm) {

		boolean justIds = (hm == null);
		Set<Long> longIds = new LinkedHashSet<Long>();
		for (String siteId : getAllSites().keySet()) {
			longIds.add(Long.parseLong(siteId));
			if(!justIds) {
				hm.put(Long.parseLong(siteId), getAllSites().get(siteId).getName());
			}
		}
		return longIds.toArray(new Long[longIds.size()]);
	}

	

	public synchronized static void fillDataAboutAllImplementedCountyFromDatabase() {
		
		long startTime = System.currentTimeMillis();
		fillTreeSetSiteTypes();
		fillSiteTypeToP2();
		fillIndexCountyToIndexCity();
		fillAllCounties();
		
		mapTaxDate.clear();
		mapTaxDate.putAll(readTaxDatesTable());
		
		logger.info("fillDataAboutAllImplementedCountyFromDatabase: initial fill " + (System.currentTimeMillis() - startTime));
		
		getAllSites().clear();
		fillAllSites();
		
//		if(URLMaping.INSTANCE_DIR.startsWith("local")) {
//			long crtTime = System.currentTimeMillis();
//			fillAllSites(3, true);
//			logger.info("fillDataAboutAllImplementedCountyFromDatabase: fillAllSites commId: " + 3 + " took " + (System.currentTimeMillis() - crtTime));
//			crtTime = System.currentTimeMillis();
//			fillAllSites(4, false);
//			logger.info("fillDataAboutAllImplementedCountyFromDatabase: fillAllSites commId: " + 4 + " took " + (System.currentTimeMillis() - crtTime));
//		} else if(URLMaping.INSTANCE_DIR.startsWith("ats01") 
//				|| URLMaping.INSTANCE_DIR.startsWith("beta")
//				|| URLMaping.INSTANCE_DIR.startsWith("atsdev")
////				|| URLMaping.INSTANCE_DIR.startsWith("atsstg")
//				|| URLMaping.INSTANCE_DIR.startsWith("atspre")) {
//			fillAllSites(3, true);
//			fillAllSites(4, false);
//		} else {
//			boolean fillAllCountiesInvertWithEPMapping = true;
//			for (Integer commId : CommunityManager.getAllCommunityIds(false)) {
//				long crtTime = System.currentTimeMillis();
//				fillAllSites(commId, fillAllCountiesInvertWithEPMapping);
//				fillAllCountiesInvertWithEPMapping = false;
//				logger.info("fillDataAboutAllImplementedCountyFromDatabase: fillAllSites commId: " + commId + " took " + (System.currentTimeMillis() - crtTime));
//			}
//		}
		
		logger.info("fillDataAboutAllImplementedCountyFromDatabase: all took " + (System.currentTimeMillis() - startTime));
		
	}

	/**
	 * Returns the site data for the given community and serverId (as long)
	 * @param commId id of the community
	 * @param miServerID if of the server (as long)
	 * @return the corresponding DataSite or null if not found
	 */
	public static DataSite getDateSiteForMIServerID(int commId, long miServerID) {
		return getDateSiteForMIServerID(String.valueOf(miServerID));
	}

	/**
	 * Returns the site data for the given community and serverId (as string)<br>
	 * If commId is -1 then the first dataSite is returned and the enabled area is nullified
	 * @param commId id of the community
	 * @param miServerID if of the server (as string)
	 * @return the corresponding DataSite or null if not found
	 */
	public static DataSite getDateSiteForMIServerID(String miServerID) {
		Map<String, DataSite> infoForCommunity = getAllSites();
		if(infoForCommunity != null) {
			return infoForCommunity.get(miServerID);
		}
		return null;
	}

	public static Map<String, String> getAllCounty() {
		return allCounties;
	}

	private static String getCountyIndex(SearchAttributes sa)
			throws BaseException {
		String county = sa.getAtribute(SearchAttributes.P_COUNTY);
		return getCountyIndex(county);
	}

	public static long getCountyId(String countyIndex) {
		// daca sunt in caz de city TAX schimb countyIndex cu cel bun
		Set setEntry = indexCountyToIndexCity.keySet();
		Iterator itK = setEntry.iterator();
		while (itK.hasNext()) {
			String c = (String) itK.next();
			String v = (String) indexCountyToIndexCity.get(c);
			if (v.equals(countyIndex)) {
				countyIndex = c;
			}
		}
		setEntry = allCounties.keySet();
		itK = setEntry.iterator();
		while (itK.hasNext()) {
			String c = (String) itK.next();
			String v = (String) allCounties.get(c);
			if (v.equals(countyIndex)) {
				try {
					return Long.parseLong(c);
				} catch (Exception e) {
					e.printStackTrace(System.err);
					break;
				}
			}
		}

		return -1;
	}

	// id de county
	public static String getCountyIndex(String key) throws BaseException {
		if (allCounties.containsKey(key)) {
			return (String) allCounties.get(key);
		} else {
			throw new NotImplFeatureException(
					"This county  is not yet available for Advanced Title Search. "
							+ "Please ask your service provider for details regarding the scheduled availability.");
		}
	}

	public static void setSearchServer(HttpServletRequest request,
			Search global, boolean isParentSite) throws BaseException {
		int cityChecked = isParentSite ? global.getCityCheckedParentSite()
				: global.getCitychecked();
		try {
			cityChecked = new Integer(request.getParameter("cityChecked"))
					.intValue();

		} catch (NumberFormatException nfe) {
		}
		setSearchServer(global, cityChecked, isParentSite);
	}

	public static void setSearchServer(Search global, int cityChecked)
			throws BaseException {
		setSearchServer(global, cityChecked, false);
	}

	public static void setSearchServer(Search global, int cityChecked,
			boolean isParentSite) throws BaseException {

		String P1 = getCountyIndex(global.getSa());
		String P2 = Integer.toString(mapSiteTypeToP2.get(cityChecked));
		if (isParentSite) {
			global.setP1ParentSite(P1);
			global.setP2ParentSite(P2);
			global.setCityCheckedParentSite(cityChecked);
		} else {
			global.setP1(P1);
			global.setP2(P2);
			global.setCitychecked(cityChecked);
		}
		if (cityChecked == 2) { // special case for EP
			String newP1 = indexCountyToIndexCity.get(P1);
			if (newP1 != null) {
				if (isParentSite)
					global.setP1ParentSite(newP1);
				else
					global.setP1(newP1);
			} else {
				logger
						.error("<><><>\n HasCountyToIndex --- setSearchServer newP1 = null  searcId = "
								+ global.getID()
								+ " P1 =  "
								+ P1
								+ " \n<><><>\n");
			}
		}
		if (InstanceManager.getManager().getCurrentInstance(global.getID())
				.getCurrentUser() != null) {
			// save current county
			Settings.manipulateAttribute(Settings.SEARCH_MODULE,
					Settings.LAST_COUNTY,
					new Integer(global.getSa().getAtribute(
							SearchAttributes.P_COUNTY)).intValue(),
					InstanceManager.getManager().getCurrentInstance(
							global.getID()).getCurrentUser().getID());
			// save current state
			Settings.manipulateAttribute(Settings.SEARCH_MODULE,
					Settings.LAST_STATE, new Integer(global.getSa()
							.getAtribute(SearchAttributes.P_STATE)).intValue(),
					InstanceManager.getManager().getCurrentInstance(
							global.getID()).getCurrentUser().getID());
		}
	}

	public static void setSearchServerAfterFactoryId(Search global,
			int serverFactoryId) {
		
		int commId = -1;
		try{
			commId = InstanceManager.getManager().getCurrentInstance(global.getID()).getCurrentCommunity().getID().intValue();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		DataSite dat = getDateSiteForMIServerID(commId, serverFactoryId);
		boolean error = false;
		String index = dat.getIndex();
		String P2 = dat.getP2();

		if (index == null || P2 == null ) {
			error = true;
		} else {
			try {
				global.setCitychecked(dat.getSiteType());
				global.setP1(index);
				global.setP2(P2);
			} catch (Exception e) {
				e.printStackTrace();
				error = true;
			}
		}
		if (error) {
			logger.error("Not valid server code: serverFactoryId =  ["
					+ serverFactoryId + "]");
		}
	}

	public static int getServerFactoryID(int p1, int p2) throws BaseException {

		int iID = 0;
		boolean isEP = indexCountyToIndexCity.values().contains(
				p1 + "")
				&& p2 == 2;

		String dbCountyIdStr = (isEP) ? allCountiesInvertWithEPMapping.get(""
				+ p1) : allCountiesInvertWithEPMapping.get("0" + p1);
		int dbCountyId = Integer.parseInt(dbCountyIdStr);
		int site_type = 0;

		if (isEP) {
			site_type = TSServersFactory.getSiteTypeId("YA");
		} else if (p2 == 2) {
			site_type = TSServersFactory.getSiteTypeId("TR");
		} else {
			site_type = mapP2ToSiteType.get(p2) + 1;
		}

		iID = (int) TSServersFactory.getSiteIdfromCountyandServerTypeId(
				dbCountyId, site_type);

		if (iID == 0) {
			logger.error("Not valid server code: serverType =  [" + p2
					+ "]; serverIndex = [" + p1 + "]");
			throw new NotImplFeatureException(
					"This server is not yet available for search. "
							+ "Please ask your service provider for details regarding the scheduled availability.");
		}
		return iID;
	}

	public static void addNewSitesOrUpdates(int commId, Vector<GWTDataSite> changedSites, Vector<GWTCommunitySite> changedCommunitySites)
			throws Exception {
		Set<Integer> updatedCountiesServerId = new HashSet<Integer>();
		
		if(changedSites != null) {
			for (GWTDataSite gwtDataSite : changedSites) {
				try {
					int miServerId = addNewSiteOrUpdate(gwtDataSite);
					if(miServerId > 0) {
						updatedCountiesServerId.add(miServerId);
					}
				} catch (Exception e) {
					logger.error("Cannot addNewSiteOrUpdate " + gwtDataSite, e);
				}
			}
		}
		
		boolean noSitesUpdates = updatedCountiesServerId.isEmpty();
		
		if(changedCommunitySites != null) {
			for (GWTCommunitySite gwtCommunitySite : changedCommunitySites) {
				int miServerId = updateCommunitySite(gwtCommunitySite);
				if(miServerId > 0 && noSitesUpdates) {
					synchronized (HashCountyToIndex.class) {
						DataSite dataSite = getAllSites().get(Integer.toString(miServerId));
						dataSite.getCommunityActivation().put(gwtCommunitySite.getCommId(), gwtCommunitySite.getEnableStatus());
					}
					
				}
			}
		}
		
		if (!noSitesUpdates) {
			Search.initSitesConfiguration();
		}
	}
	
	private static final String SQL_INSERT_NEW_ENABLE_STATUS = "INSERT INTO " + 
			DBConstants.TABLE_COMMUNITY_SITES + " VALUES (?,?,?,?,?) ";
	
	private static final String SQL_UPDATE_NEW_ENABLE_STATUS = "UPDATE " + DBConstants.TABLE_COMMUNITY_SITES + 
			" SET " + DBConstants.FIELD_COMMUNITY_SITES_ENABLE_STATUS + " = ? " + 
			" WHERE " + DBConstants.FIELD_COMMUNITY_SITES_COMMUNITY_ID + " = ? " +
			" AND " + DBConstants.FIELD_COMMUNITY_SITES_COUNTY_ID + " = ? " +
			" AND " + DBConstants.FIELD_COMMUNITY_SITES_SITE_TYPE + " = ? " +
			" AND " + DBConstants.FIELD_COMMUNITY_SITES_CITY_TYPE_P2 + " = ? ";
		
	private static int updateCommunitySite(GWTCommunitySite gwtCommunitySite) {
		
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		
		int siteType = getServerTypeByAbbreviation(gwtCommunitySite.getSiteTypeAbrev());
		int p2 = mapSiteTypeToP2.get(siteType);
		
		int rowsAffected = sjt.update(SQL_UPDATE_NEW_ENABLE_STATUS, 
				gwtCommunitySite.getEnableStatus(), 
				gwtCommunitySite.getCommId(), 
				gwtCommunitySite.getCountyId(), 
				siteType, 
				p2);
		
		if(rowsAffected > 0) {
			return (int)TSServersFactory.getSiteIdfromCountyandServerTypeId(gwtCommunitySite.getCountyId(), siteType + 1);
		}
		
		return -1;
	}
	
	public static int addNewSiteOrUpdate(GWTDataSite siteData) {
		
		
		int updatedSites = 0;
		int updatedTaxRows = 0;
		
		boolean isTaxType = siteData.isTaxLikeSite();
		
		String stateAbrv = siteData.getStateAbrv();
		String countyName = siteData.getCountyName();
		String cityName = null;
		TaxSiteData taxSite = null;
		if (siteData instanceof TaxSiteData){
			taxSite = (TaxSiteData) siteData;
			cityName = taxSite.getCityName();
		}
		
		String type = siteData.getSiteTypeAbrv();
		String parserFileNameSuffix = siteData.getParserFileNameSuffix();
		String classFilename = siteData.getClassFilename();
		// countyName = countyName .replaceAll(" ","&nbsp;");
		boolean isCityTax = Search.getReadOnlyServerTypesAbrev()[2]
				.equals(type);
		String siteName = stateAbrv + countyName + type;

		String link = siteData.getLink();
		int maxSess = siteData.getMaxSessions();
		int tbr = siteData.getTimeBetweenRequests();
		int atbr = siteData.getAbsTimeBetweenRequests();
		int connType = siteData.getConnType();
		int searchTimeout = siteData.getSearchTimeout();
		int conTimeout = siteData.getConnectionTimeout();
		if (conTimeout < 0)
			conTimeout = -1;
		String classConFileName = siteData.getClassConnFilename();
		String passowrdCode = siteData.getPasswordCode();

		int mrprs = siteData.getMaxRequestsPerSecond();
		int units = siteData.getUnits();
		int numberOfYears = siteData.getNumberOfYears();
		int certified = siteData.getSiteCertified(); 
		
		String adressToken = siteData.getAdressToken();
		String adressTokenMiss = siteData.getAdressTokenMiss();
		String alternateLink = siteData.getAlternateLink();
		int autopos = siteData.getAutpos();

		int siteType = getServerTypeByAbbreviation(type);
		siteData.setSiteType(siteType);
		
		int p2 = mapSiteTypeToP2.get(siteType);

		boolean isAssessorOrTaxType = (GWTDataSite.isAssessorLike(siteType) || GWTDataSite.isTaxLike(siteType));
		
		int countyDbId = -1;
		if(siteData.getCountyId() > 0) {
			countyDbId = siteData.getCountyId();
		} else {
			try {
				countyDbId = County.getCounty(countyName,stateAbrv).getCountyId().intValue();
				siteData.setCountyId(countyDbId);
			} catch (BaseException e) {
				throw new RuntimeException("Cannot get county for state " + stateAbrv + " and countyName " + countyName, e);
			}
		}

		String countyIndexStr = "";

		
		String serverFactoryIdStr = TSServersFactory
				.getSiteIdfromCountyandServerTypeId(countyDbId, siteType + 1)
				+ "";
		boolean isCountyImplemented = ((countyIndexStr = allCounties.get(countyDbId + "")) != null);
		boolean isSiteImplemented = getDateSiteForMIServerID(serverFactoryIdStr) != null;

		int countyIndex = -1;
		String countyIndexStrWithout0 = "-1" + countyIndex;
		if (isCountyImplemented) {
			countyIndex = Integer.parseInt(countyIndexStr);
			countyIndexStrWithout0 = "" + countyIndex;
		}
		
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		
		if (isSiteImplemented) {

			Vector<DataSite> vec = HashCountyToIndex.getAllDataSites(ANY_COMMUNITY, 
					stateAbrv, 
					countyName, 
					false, ANY_PRODUCT);

			int max = 0;
			for (int k = 0; k < vec.size(); k++) {
				int val = vec.get(k).getAutpos();
				if (max < val) {
					max = val;
				}
			}

			if (adressToken == null || adressToken == "") {
				adressToken = Util.DEFAULT_ADDRESS_TOKEN;
				autopos = max + 1;
			}
			if (adressTokenMiss == null || adressTokenMiss == "") {
				adressTokenMiss = Util.DEFAULT_ADDRESS_TOKEN_MISS;
				autopos = max + 1;
			}
			
			autopos = SearchSitesBuckets.getSiteOrderIndexInBucket(siteType, autopos);
			
			
			if (isCityTax) {
				String fakeIndex = indexCountyToIndexCity.get(countyIndexStr);
				if(fakeIndex == null) {
					setSiteAsCityTax(sjt, countyDbId, countyIndexStrWithout0);
				}
			}
			
			//4 debug
			if(autopos <= 0 && !ArrayUtils.contains(SearchSitesBuckets.ASSESSOR_LIKE_SITES, siteType)){
				String message = "From update: siteType=" + siteType + " idCounty=" + countyDbId + " max=" + max + " autopos=" + autopos;
				
				Log.sendEmail(MailConfig.getMailLoggerStatusAddress(), "[Search Sites Warning]", message);
			}
			
			StringBuilder sql = new StringBuilder();
			sql.append(" UPDATE ts_sites  SET  id_county=");
			sql.append(countyDbId);
			sql.append(",site_type=" + siteType);
			sql.append(",p2=" + p2);
			sql.append(",parser_file_name_suffix= ?,");
			sql.append("tsserver_class_name= ?,");
			sql.append("link= ?, ");
			sql.append("connection_time_out=" + conTimeout);
			sql.append(",search_time_out=" + searchTimeout);
			sql.append(",time_between_requests=" + tbr);
			sql.append(",max_requests_per_second="+mrprs);
			sql.append(",time_unit="+units);
			sql.append(",password_code= ?,");
			sql.append("conn_class_name=?,");
			sql.append("max_sess="+maxSess);
			sql.append(",adress_token = ?,");
			sql.append("adress_token_miss = ?,");
			sql.append("automatic_position = " + autopos);
			sql.append(",alternateLink = ?");
			sql.append(isTaxType? ",city_name = '" + StringUtils.defaultIfEmpty(cityName, "") + "'": "");					
			sql.append(",conn_type=" + connType);
			sql.append("," + DBConstants.FIELD_SITES_EFFECTIVE_START_DATE + "=?");
			sql.append("," + DBConstants.FIELD_SITES_NUMBER_OF_YEARS + "=" + (isAssessorOrTaxType ? numberOfYears : 0));
			sql.append("," + DBConstants.FIELD_SITES_IS_ENDBLED+"="+certified);
			sql.append(" WHERE id_county = " + countyDbId);
			sql.append(" AND site_type=" + siteType);
			String stm = sql.toString();
			
			try {
				updatedSites = sjt.update(stm,parserFileNameSuffix,classFilename,link,passowrdCode,
						classConFileName,adressToken,adressTokenMiss,alternateLink,
						siteData.getEffectiveStartDate());
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			if(isTaxType){
				
				Map<String,TaxDateStructure> map = readTaxDatesTable();
				try {
					TaxDateStructure tdata =  map.get(siteName);
					if(tdata != null){
						if(taxSite.getDueDate() == null || taxSite.getPayDate() == null){
							updatedTaxRows = DBManager.getSimpleTemplate().update(
									"DELETE FROM ts_tax_dates WHERE name = ? ", 
									siteName);
						} else {
							updatedTaxRows = DBManager.getSimpleTemplate().update(
									"UPDATE ts_tax_dates SET  dueDate =? , payDate =?, " + DBConstants.FIELD_TAX_TAX_YEAR_MODE +" =?  WHERE name = ? ", 
									taxSite.getDueDate(),
									taxSite.getPayDate(),
									taxSite.getTaxYearMode(), 
									siteName);
						}
					}
					else{
						if(taxSite.getDueDate() != null && taxSite.getPayDate() != null) {
							updatedTaxRows = DBManager.getSimpleTemplate().update(
									"INSERT INTO  ts_tax_dates (name , dueDate, payDate, " + DBConstants.FIELD_TAX_TAX_YEAR_MODE +" ) VALUES (?,?,?, ?)", 
									siteName,
									taxSite.getDueDate(),
									taxSite.getPayDate(),
									taxSite.getTaxYearMode()
									);
						}
					}
				}catch(Exception e) {
					e.printStackTrace();
				}
				
			}
		} else {
			Vector<DataSite> vec = HashCountyToIndex.getAllDataSites(
					ANY_COMMUNITY, 
					stateAbrv, 
					countyName, 
					false, ANY_PRODUCT);
			
			SearchSitesBucket sb = SearchSitesBuckets.getSiteBucket(siteType);
			
			List<Integer> bucketSites = new Vector<Integer>();
			
			if(sb!=null){
				bucketSites = sb.getSites();
			}
			
			int max = 0;
			for (int k = 0; k < vec.size(); k++) {
				DataSite d = vec.get(k);
				
				int val = d.getAutpos(); 
				if (max < val && (bucketSites.size() > 0 ? bucketSites.contains(d.getSiteTypeInt()) : true)) {
					max = val;
				}
			}
			autopos = SearchSitesBuckets.getSiteOrderIndexInBucket(siteType, max + 1);
			adressToken = Util.DEFAULT_ADDRESS_TOKEN;
			adressTokenMiss = Util.DEFAULT_ADDRESS_TOKEN_MISS;
			if (!isCountyImplemented) {// insert with county implemented for
										// cityTax
				countyIndexStr = setCountyAsImplemented(sjt, countyDbId) + "";
			}
			if (isCityTax) {
				setSiteAsCityTax(sjt, countyDbId, countyIndexStrWithout0);
			}
			
			//4 debug
			if(autopos <= 0 && !ArrayUtils.contains(SearchSitesBuckets.ASSESSOR_LIKE_SITES, siteType)){
				String message = "From insert: siteType=" + siteType + " idCounty=" + countyDbId + " max=" + max + " autopos=" + autopos;
				
				Log.sendEmail(MailConfig.getMailLoggerStatusAddress(), "[Search Sites Warning]", message);
			}
			
			String stm = "insert into ts_sites (id_county,site_type,p2,parser_file_name_suffix,tsserver_class_name"
					+ ",link,connection_time_out,search_time_out,time_between_requests,max_requests_per_second,time_unit,password_code,conn_class_name,max_sess"
					+ " ,automatic_position , adress_token , adress_token_miss , conn_type, abs_time_between_requests, city_name, " + DBConstants.FIELD_SITES_EFFECTIVE_START_DATE 
					+ "," + DBConstants.FIELD_SITES_NUMBER_OF_YEARS + "," + DBConstants.FIELD_SITES_IS_ENDBLED + ") values  ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
			
			updatedSites = sjt.update(stm,countyDbId,siteType,p2,parserFileNameSuffix,classFilename,link,conTimeout,searchTimeout,tbr,
						mrprs, units, passowrdCode, classConFileName,maxSess,autopos,adressToken,adressTokenMiss,connType,atbr, cityName, 
						siteData.getEffectiveStartDate(), numberOfYears, certified );
			
			List<Integer> commIds = CommunityManager.getAllCommunityIds(true);
			for (Integer commIdToInsert : commIds) {
				
				Integer enableStatus = siteData.getCommunityActivation().get(commIdToInsert);
				if(enableStatus == null) {
					enableStatus = 0;
				}
				
				sjt.update(SQL_INSERT_NEW_ENABLE_STATUS, commIdToInsert, countyDbId, siteType, p2, enableStatus);
			}
		}
		
		if(updatedSites + updatedTaxRows > 0) {
			return (int)TSServersFactory.getSiteIdfromCountyandServerTypeId(siteData.getCountyId(), siteData.getSiteType() + 1);
		} 
		return -1;
	}

//	public static void addNewSiteOrUpdate(int commId, GWTDataSite siteData) throws Exception {
//
//		if(siteData.getCommId()==-1) {
//		
//			boolean isTaxType = siteData.isTaxLikeSite();
//			
//			String stateAbrv = siteData.getStateAbrv();
//			String countyName = siteData.getCountyName();
//			String cityName = null;
//			if (siteData instanceof TaxSiteData){
//				TaxSiteData taxSite = (TaxSiteData) siteData;
//				cityName = taxSite.getCityName();
//			}
//			
//			String type = siteData.getSiteTypeAbrv();
//			String parserFileNameSuffix = siteData.getParserFileNameSuffix();
//			String classFilename = siteData.getClassFilename();
//			// countyName = countyName .replaceAll(" ","&nbsp;");
//			boolean isCityTax = Search.getReadOnlyServerTypesAbrev()[2]
//					.equals(type);
//			String siteName = stateAbrv + countyName + type;
//	
//			String link = siteData.getLink();
//			int maxSess = siteData.getMaxSessions();
//			int tbr = siteData.getTimeBetweenRequests();
//			int atbr = siteData.getAbsTimeBetweenRequests();
//			int connType = siteData.getConnType();
//			int searchTimeout = siteData.getSearchTimeout();
//			int conTimeout = siteData.getConnectionTimeout();
//			if (conTimeout < 0)
//				conTimeout = -1;
//			String classConFileName = siteData.getClassConnFilename();
//			String passowrdCode = siteData.getPasswordCode();
//	
//			int mrprs = siteData.getMaxRequestsPerSecond();
//			int units = siteData.getUnits();
//			int numberOfYears = siteData.getNumberOfYears();
//			int certified = siteData.getSiteCertified(); 
//			
//			String adressToken = siteData.getAdressToken();
//			String adressTokenMiss = siteData.getAdressTokenMiss();
//			String alternateLink = siteData.getAlternateLink();
//			int autopos = siteData.getAutpos();
//	
//			DBConnection conn = null;
//			try {
//				conn = ConnectionPool.getInstance().requestConnection();
//			} catch (Exception e) {
//				logger
//						.error("EROR -- fillDataAboutAllImplementedCountyFromDatabase  exception cause: "
//								+ e.getCause()
//								+ " exception message "
//								+ e.getMessage());
//				try {
//					ConnectionPool.getInstance().releaseConnection(conn);
//				} catch (Exception ex) {
//				}
//				throw new Exception("Could not access ConnectionPoll to database !");
//			}
//	
//			try { 
//				int siteType = getServerTypeByAbbreviation(type);
//				String p2Str = mapSiteTypeToP2.get(siteType + "");
//				int p2 = Integer.parseInt(p2Str);
//		
//				boolean isAssessorOrTaxType = (GWTDataSite.isAssessorLike(siteType) || GWTDataSite.isTaxLike(siteType));
//				
//				int countyDbId = -1;
//				countyDbId = County.getCounty(countyName,stateAbrv).getCountyId().intValue();
//		
//				if (countyDbId <= 0) {
//					throw new Exception("Could not access database !");
//				}
//		
//				String countyIndexStr = "";
//		
//				
//				String serverFactoryIdStr = TSServersFactory
//						.getSiteIdfromCountyandServerTypeId(countyDbId, siteType + 1)
//						+ "";
//				boolean isCountyImplemented = ((countyIndexStr = allCounties.get(countyDbId + "")) != null);
//				boolean isSiteImplemented = getDateSiteForMIServerID(serverFactoryIdStr) != null;
//		
//				int countyIndex = -1;
//				String countyIndexStrWithout0 = "-1" + countyIndex;
//				if (isCountyImplemented) {
//					countyIndex = Integer.parseInt(countyIndexStr);
//					countyIndexStrWithout0 = "" + countyIndex;
//				}
//				if (isSiteImplemented) {
//		
//					Vector<DataSite> vec = HashCountyToIndex.getAllDataSites(ANY_COMMUNITY, 
//							stateAbrv, 
//							countyName, 
//							false, ANY_PRODUCT);
//		
//					int max = 0;
//					for (int k = 0; k < vec.size(); k++) {
//						int val = vec.get(k).getAutpos();
//						if (max < val) {
//							max = val;
//						}
//					}
//		
//					if (adressToken == null || adressToken == "") {
//						adressToken = Util.DEFAULT_ADDRESS_TOKEN;
//						autopos = max + 1;
//					}
//					if (adressTokenMiss == null || adressTokenMiss == "") {
//						adressTokenMiss = Util.DEFAULT_ADDRESS_TOKEN_MISS;
//						autopos = max + 1;
//					}
//					
//					autopos = SearchSitesBuckets.getSiteOrderIndexInBucket(siteType, autopos);
//					
//					//4 debug
//					if(autopos <= 0 && !ArrayUtils.contains(SearchSitesBuckets.ASSESSOR_LIKE_SITES, siteType)){
//						String message = "From update: siteType=" + siteType + " idCounty=" + countyDbId + " max=" + max + " autopos=" + autopos;
//						
//						Log.sendEmail(MailConfig.getMailLoggerStatusAddress(), "[Search Sites Warning]", message);
//					}
//					
//					StringBuilder sql = new StringBuilder();
//					sql.append(" UPDATE ts_sites  SET  id_county=");
//					sql.append(countyDbId);
//					sql.append(",site_type=" + siteType);
//					sql.append(",p2=" + p2);
//					sql.append(",parser_file_name_suffix= ?,");
//					sql.append("tsserver_class_name= ?,");
//					sql.append("link= ?, ");
//					sql.append("connection_time_out=" + conTimeout);
//					sql.append(",search_time_out=" + searchTimeout);
//					sql.append(",time_between_requests=" + tbr);
//					sql.append(",max_requests_per_second="+mrprs);
//					sql.append(",time_unit="+units);
//					sql.append(",password_code= ?,");
//					sql.append("conn_class_name=?,");
//					sql.append("max_sess="+maxSess);
//					sql.append(",adress_token = ?,");
//					sql.append("adress_token_miss = ?,");
//					sql.append("automatic_position = " + autopos);
//					sql.append(",alternateLink = ?");
//					sql.append(isTaxType? ",city_name = '" + StringUtils.defaultIfEmpty(cityName, "") + "'": "");					sql.append(",conn_type=" + connType);
//					sql.append("," + DBConstants.FIELD_SITES_EFFECTIVE_START_DATE + "=?");
//					sql.append("," + DBConstants.FIELD_SITES_NUMBER_OF_YEARS + "=" + (isAssessorOrTaxType ? numberOfYears : 0));
//					sql.append("," + DBConstants.FIELD_SITES_IS_ENDBLED+"="+certified);
//					sql.append(" WHERE id_county = " + countyDbId);
//					sql.append(" AND site_type=" + siteType);
//					String stm = sql.toString();
//					
//					try {
//						DBManager.getSimpleTemplate().update(stm,parserFileNameSuffix,classFilename,link,passowrdCode,
//								classConFileName,adressToken,adressTokenMiss,alternateLink,
//								siteData.getEffectiveStartDate());
//					}catch(Exception e) {
//						e.printStackTrace();
//					}
//					
//					if(isTaxType){
//						Map<String,TaxDateStructure> map = readTaxDatesTable();
//						try {
//							TaxDateStructure tdata =  map.get(siteName);
//							if(tdata != null){
//								if(((TaxSiteData)siteData).getDueDate() == null || ((TaxSiteData)siteData).getPayDate() == null){
//									DBManager.getSimpleTemplate().update(
//											"DELETE FROM ts_tax_dates WHERE name = ? ", 
//											siteName);
//								} else {
//									DBManager.getSimpleTemplate().update(
//											"UPDATE ts_tax_dates SET  dueDate =? , payDate =?, " + DBConstants.FIELD_TAX_TAX_YEAR_MODE +" =?  WHERE name = ? ", 
//											((TaxSiteData)siteData).getDueDate(),
//											((TaxSiteData)siteData).getPayDate(),
//											((TaxSiteData)siteData).getTaxYearMode(), 
//											siteName);
//								}
//							}
//							else{
//								DBManager.getSimpleTemplate().update(
//										"INSERT INTO  ts_tax_dates (name , dueDate, payDate, " + DBConstants.FIELD_TAX_TAX_YEAR_MODE +" ) VALUES (?,?,?, ?)", 
//										siteName,
//										((TaxSiteData)siteData).getDueDate(),
//										((TaxSiteData)siteData).getPayDate(),
//										((TaxSiteData)siteData).getTaxYearMode()
//										);
//							}
//						}catch(Exception e) {
//							e.printStackTrace();
//						}
//						
//					}
//				} else {
//					Vector<DataSite> vec = HashCountyToIndex.getAllDataSites(
//							ANY_COMMUNITY, 
//							stateAbrv, 
//							countyName, 
//							false, ANY_PRODUCT);
//					
//					SearchSitesBucket sb = SearchSitesBuckets.getSiteBucket(siteType);
//					
//					List<Integer> bucketSites = new Vector<Integer>();
//					
//					if(sb!=null){
//						bucketSites = sb.getSites();
//					}
//					
//					int max = 0;
//					for (int k = 0; k < vec.size(); k++) {
//						DataSite d = vec.get(k);
//						
//						int val = d.getAutpos(); 
//						if (max < val && (bucketSites.size() > 0 ? bucketSites.contains(d.getSiteTypeInt()) : true)) {
//							max = val;
//						}
//					}
//					autopos = SearchSitesBuckets.getSiteOrderIndexInBucket(siteType, max + 1);
//					adressToken = Util.DEFAULT_ADDRESS_TOKEN;
//					adressTokenMiss = Util.DEFAULT_ADDRESS_TOKEN_MISS;
//					if (!isCountyImplemented) {// insert with county implemented for
//												// cityTax
//						countyIndexStr = setCountyAsImplemented(conn, countyDbId) + "";
//					}
//					if (isCityTax) {
//						setSiteAsCityTax(conn, countyDbId, countyIndexStrWithout0);
//					}
//					
//					//4 debug
//					if(autopos <= 0 && !ArrayUtils.contains(SearchSitesBuckets.ASSESSOR_LIKE_SITES, siteType)){
//						String message = "From insert: siteType=" + siteType + " idCounty=" + countyDbId + " max=" + max + " autopos=" + autopos;
//						
//						Log.sendEmail(MailConfig.getMailLoggerStatusAddress(), "[Search Sites Warning]", message);
//					}
//					
//					String stm = "insert into ts_sites (id_county,site_type,p2,parser_file_name_suffix,tsserver_class_name"
//							+ ",link,connection_time_out,search_time_out,time_between_requests,max_requests_per_second,time_unit,password_code,conn_class_name,max_sess"
//							+ " ,automatic_position , adress_token , adress_token_miss , conn_type, abs_time_between_requests, city_name, " + DBConstants.FIELD_SITES_EFFECTIVE_START_DATE 
//							+ "," + DBConstants.FIELD_SITES_NUMBER_OF_YEARS + "," + DBConstants.FIELD_SITES_IS_ENDBLED + ") values  ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
//					SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
//					sjt.update(stm,countyDbId,siteType,p2,parserFileNameSuffix,classFilename,link,conTimeout,searchTimeout,tbr,
//								mrprs, units, passowrdCode, classConFileName,maxSess,autopos,adressToken,adressTokenMiss,connType,atbr, cityName, 
//								siteData.getEffectiveStartDate(), numberOfYears, certified );
//					
//					List<Integer> commIds = CommunityManager.getAllCommunityIds(true);
//					int enableStatus = siteData.getEnabled();
//					for (Integer commIdToInsert : commIds) {
//						sjt.update(SQL_INSERT_NEW_ENABLE_STATUS, commIdToInsert, countyDbId, siteType, p2, enableStatus);
//					}
//				}
//			}
//			finally {
//				try {
//					ConnectionPool.getInstance().releaseConnection(conn);
//				} catch (Exception ex) {}
//			}
//		
//		} else  {
//			String stateAbrv = siteData.getStateAbrv();
//			String countyName = siteData.getCountyName();
//			String type = siteData.getSiteTypeAbrv();
//			int comm = siteData.getCommId();
//			int siteType = getServerTypeByAbbreviation(type);
//			String p2Str = mapSiteTypeToP2.get(siteType + "");
//			int p2 = Integer.parseInt(p2Str);
//	
//	
//			int countyDbId = -1;
//			countyDbId = County.getCounty(countyName,stateAbrv).getCountyId().intValue();
//	
//			if (countyDbId <= 0) {
//				throw new Exception("Could not access database !");
//			}
//			
//			DBManager.getSimpleTemplate().update(SQL_UPDATE_NEW_ENABLE_STATUS, siteData.getEnabled(), comm, countyDbId, siteType, p2);
//		}
//	}

	private static void fillTreeSetSiteTypes() {
		try {
			String stm = "select  site_type, site_abrev , p2 ,description  from ts_map_site_type_to_p2 order by site_type";
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			treeSetSiteTypes = sjt.query(stm, new SiteTypeEntry());
			Map<String, Integer> serverAbbrevSiteTypeMapInternal = new LinkedHashMap<String, Integer>();
			Map<Integer, String> siteTypeServerAbbrevMapInternal = new LinkedHashMap<Integer, String>();
			Map<Integer, String> siteTypeServerDescriptionMapInternal = new LinkedHashMap<Integer, String>();
			for (SiteTypeEntry siteTypeEntry : treeSetSiteTypes) {
				serverAbbrevSiteTypeMapInternal.put(siteTypeEntry.getSiteAbrev(), siteTypeEntry.getSiteType());
				siteTypeServerAbbrevMapInternal.put(siteTypeEntry.getSiteType(), siteTypeEntry.getSiteAbrev());
				siteTypeServerDescriptionMapInternal.put(siteTypeEntry.getSiteType(), siteTypeEntry.getDescription());
			}
			serverAbbrevSiteTypeMap = serverAbbrevSiteTypeMapInternal;
			siteTypeServerAbbrevSiteTypeMap = siteTypeServerAbbrevMapInternal;
			siteTypeServerDescriptionMap = siteTypeServerDescriptionMapInternal;
			
		} catch (Exception e) {
			logger.error("Error while loading ts_map_site_type_to_p2 table",e);
		}
	}
	
	private static void fillSiteTypeToP2() {
		String sqlSTR = "SELECT site_type, p2 FROM ts_map_site_type_to_p2";
		try {
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			List<Map<String, Object>> list = sjt.queryForList(sqlSTR);
			for (Map<String, Object> map : list) {
				Integer key = (Integer)map.get("p2");
				Integer value = (Integer)map.get("site_type");
				// special EP cases
				if ( key.intValue() != 2) {
					mapP2ToSiteType.put(key, value);
				}
				mapSiteTypeToP2.put(value, key);
			}
			
		} catch (Exception e) {
			logger.error("ERROR in fillSiteTypeToP2", e);
		} 
		
	}
	
	private static void fillIndexCountyToIndexCity() {
		String sqlSTR = "SELECT real_county_index, fake_county_index FROM "+ DBConstants.TABLE_MAP_FAKE_COUNTY_INDEX_FOR_EP;
		try {
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			List<Map<String, Object>> list = sjt.queryForList(sqlSTR);
			for (Map<String, Object> map : list) {
				String key = "0" + map.get("real_county_index");
				String value = map.get("fake_county_index").toString();
				indexCountyToIndexCity.put(key, value);
			}

		} catch (Exception e) {
			logger.error("ERROR in fillIndexCountyToIndexCity", e);
		}
		
	}
	
	private static void fillAllCounties() {
		String sqlSTR = " SELECT  " + DBConstants.FIELD_COUNTY_ID + " , "
				+ DBConstants.FIELD_COUNTY_REAL_INDEX_COUNTY + " FROM "
				+ DBConstants.TABLE_COUNTY + " WHERE  "
				+ DBConstants.FIELD_COUNTY_REAL_INDEX_COUNTY + " IS NOT  NULL";
		try {
			
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			List<Map<String, Object>> list = sjt.queryForList(sqlSTR);
			for (Map<String, Object> map : list) {
				String key = map.get(DBConstants.FIELD_COUNTY_ID).toString();
				String value = "0" + map.get(DBConstants.FIELD_COUNTY_REAL_INDEX_COUNTY);
				allCounties.put(key, value);
			}
			
		} catch (Exception e) {
			logger.error("ERROR in fillAllCounties", e);
		}
		
	}
	
	public static final String SQL_SELECT_SITE = 
			" SELECT  a." + DBConstants.FIELD_SITES_ID_COUNTY 
			+ " , a." + DBConstants.FIELD_SITES_SITE_TYPE 
			+ " , a." + DBConstants.FIELD_SITES_P2
			+ " , b. " + DBConstants.FIELD_COUNTY_REAL_INDEX_COUNTY
			+ " , c. stateabv"
			+ " ,b.name"
			+ " ,a.parser_file_name_suffix"
			+ " ,a.tsserver_class_name"
			+ " ,a.conn_class_name"
			+ " ,a.connection_time_out"
			+ " ,a.search_time_out "
			+ " ,a.time_between_requests "
			+ " ,a.max_requests_per_second "
			+ " ,a.time_unit "
			+ " ,a.max_sess "
			+ " ,a.link "
			+ " ,a.adress_token "
			+ " ,a.adress_token_miss "
			+ " ,a.automatic_position "
			+ " ,b.countyFIPS"
			+ " ,c.stateFIPS, a.conn_type, a.abs_time_between_requests"
			+ " ,a.alternateLink"
			+ " ,a.city_name"
			+ ", a." + DBConstants.FIELD_SITES_EFFECTIVE_START_DATE
			+ ", b." + DBConstants.FIELD_COUNTY_DOCTYPE
			+ ", a." + DBConstants.FIELD_SITES_NUMBER_OF_YEARS
			+ ", a." + DBConstants.FIELD_SITES_IS_ENDBLED
			
			+ " FROM "
			+ DBConstants.TS_SITES + " a  , "
			+ DBConstants.TABLE_COUNTY + " b, "
			+ DBConstants.TABLE_STATE + " c "
			+ " WHERE a." + DBConstants.FIELD_SITES_ID_COUNTY + " = b." + DBConstants.FIELD_COUNTY_ID
			+ " AND c.id = b.state_id"
			;
	
	private static synchronized void fillAllSites() {
		SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
		
		
		String siteName = null;
		long backupServerId = -10;
		try {
			
			long startTMS = System.currentTimeMillis();
			List<DataSite> allSitesList = sjt.query(SQL_SELECT_SITE, new DataSite());
			long endTMS = System.currentTimeMillis();
			logger.info("fillDataAboutAllImplementedCountyFromDatabase: fillAllSites sql took miliseconds " + ((endTMS - startTMS)));
			
			
			for (DataSite dataSite : allSitesList) {
				int countyId = dataSite.getCountyId();
				long miServerId = TSServersFactory.getSiteIdfromCountyandServerTypeId(countyId, dataSite.getSiteType() + 1);
				backupServerId = miServerId;
				siteName = dataSite.getName();

				//fill tax info
				TaxDateStructure taxStr = mapTaxDate.get(siteName);
				if(taxStr != null){
					dataSite.setDueDate(taxStr.dueDate);
					dataSite.setPayDate(taxStr.payDate);
					dataSite.setTaxYearMode(taxStr.taxYearMode);
				}
				
				//put new information
				getAllSites().put(Long.toString(miServerId), dataSite);
				
				//because ...
				allCountiesInvertWithEPMapping.put(
						dataSite.getIndex(), 
						String.valueOf(countyId));
				
				//--- start fill allSitesByLocation structure 
				HashMap<String, DataSite> sitesForStateCountyKey = allSitesByLocation.get(countyId);
				if(sitesForStateCountyKey == null) {
					sitesForStateCountyKey = new HashMap<String, DataSite>();
					allSitesByLocation.put(countyId, sitesForStateCountyKey);
				}
				sitesForStateCountyKey.put(String.valueOf(miServerId), dataSite);
				//--- end fill allSitesByLocation structure 
				
			}
			
			logger.info("fillDataAboutAllImplementedCountyFromDatabase: fillAllSites code took miliseconds " + ((System.currentTimeMillis() - endTMS)));
			
//			for (DataSite dataSite : allSitesList) {
//				int commId = dataSite.getCommId();
//				long miServerId = TSServersFactory.getSiteIdfromCountyandServerTypeId(dataSite.getCountyId(),
//						Integer.parseInt(dataSite.getCityChecked()) + 1);
//				siteName = dataSite.getName();
//				backupServerId = miServerId;
//				Map<String, DataSite> sitesForCommunity = getAllSites().get(commId);
//				if(sitesForCommunity == null) {
//					sitesForCommunity = Collections.synchronizedMap(new HashMap<String, DataSite>());
//					getAllSites().put(commId, sitesForCommunity);
//				}
//				
//				TaxDateStructure taxStr = mapTaxDate.get(siteName);
//				
//				if(taxStr != null){
//					dataSite.setDueDate(taxStr.dueDate);
//					dataSite.setPayDate(taxStr.payDate);
//					dataSite.setTaxYearMode(taxStr.taxYearMode);
//				}
//				
//				sitesForCommunity.put(String.valueOf(miServerId), dataSite);
//				
//				if(fillAllCountiesInvertWithEPMapping) {
//					allCountiesInvertWithEPMapping.put(
//							dataSite.getIndex(), 
//							String.valueOf(dataSite.getCountyId()));
//				}
//				
//				
//				//--- start fill allSitesByLocation structure 
//				String stateNameAbrev = dataSite.getSTCounty();
//				HashMap<Integer, HashMap<String, DataSite>> sitesForStateCountyKey = allSitesByLocation.get(stateNameAbrev);
//				if(sitesForStateCountyKey == null) {
//					sitesForStateCountyKey = new HashMap<Integer, HashMap<String, DataSite>>();
//					allSitesByLocation.put(stateNameAbrev, sitesForStateCountyKey);
//				}
//				HashMap<String, DataSite> allSitesByLocationAndCommunity = sitesForStateCountyKey.get(commId);
//				if(allSitesByLocationAndCommunity == null) {
//					allSitesByLocationAndCommunity = new HashMap<String, DataSite>();
//					sitesForStateCountyKey.put(commId, allSitesByLocationAndCommunity);
//				}
//				allSitesByLocationAndCommunity.put(String.valueOf(miServerId), dataSite);
//				//--- end fill allSitesByLocation structure 
//			}
			
		} catch (Exception e) {
//			logger.error("ERROR in fillDataAboutAllImplementedCountyFromDatabase commId =[" + 
//					commIdToLoad + "] and siteName = [" + 
//					siteName + "] and backupServerId = [" + 
//					backupServerId + "]", e);
			logger.error("ERROR in fillDataAboutAllImplementedCountyFromDatabase siteName = [" + 
					siteName + "] and backupServerId = [" + 
					backupServerId + "]", e);
		}
		
		
		long startTime = System.currentTimeMillis();
		
		List<CommunitySite> communitySites = sjt.query("SELECT * FROM " + DBConstants.TABLE_COMMUNITY_SITES + " WHERE " + DBConstants.FIELD_COMMUNITY_SITES_ENABLE_STATUS + " != 0 ", 
				new CommunitySite());
		logger.info("fillDataAboutAllImplementedCountyFromDatabase: fillCommSites sql took miliseconds " + ((System.currentTimeMillis() - startTime)));
		startTime = System.currentTimeMillis();
		//group each Site with community settings (by serverId - the key used in allSites)
		Map<Long, List<CommunitySite>> communitySitesGrouped = new HashMap<>();
		for (CommunitySite communitySite : communitySites) {
			Long serverId = communitySite.getServerId();
			List<CommunitySite> community = communitySitesGrouped.get(serverId);
			if(community == null) {
				community = new ArrayList<CommunitySite>();
				communitySitesGrouped.put(serverId, community);
			}
			community.add(communitySite);
		}
		
		for (String serverId : allSites.keySet()) {
			DataSite dataSite = allSites.get(serverId);
			List<CommunitySite> list = communitySitesGrouped.get(Long.valueOf(serverId));
			if(list == null) {
				dataSite.getCommunityActivation().clear();
			} else {
				dataSite.getCommunityActivation().clear();
				for (CommunitySite communitySite : list) {
					dataSite.getCommunityActivation().put(communitySite.getCommId(), communitySite.getEnableStatus());
				}
				
			}
		}
		
		logger.info("fillDataAboutAllImplementedCountyFromDatabase: fillCommSites code took miliseconds " + ((System.currentTimeMillis() - startTime)));
	}

	private static void setSiteAsCityTax(SimpleJdbcTemplate sjt, int countyDbId, String countyIndex) {
		String stm = "SELECT max(fake_county_index)  FROM ts_map_fake_county_index_for_ep";
		int max = sjt.queryForInt(stm);
		stm = "INSERT INTO ts_map_fake_county_index_for_ep (real_county_index,fake_county_index) values ( ?,? )";
		sjt.update(stm, countyIndex, max + 1);
	}

	private static int setCountyAsImplemented(SimpleJdbcTemplate sjt, int countyDbId) {

		String stm = "SELECT max(real_index_county)  FROM " + DBConstants.TABLE_COUNTY + " where real_index_county is not null";
		int realIndexCounty = sjt.queryForInt(stm);
		int nextFreeIndex = getNextCountyIndex(realIndexCounty);
		stm = " UPDATE " + DBConstants.TABLE_COUNTY + "  SET  real_index_county= ? WHERE id = ? ";
		sjt.update(stm, nextFreeIndex, countyDbId);
		return nextFreeIndex;
	}

	private static int getNextCountyIndex(int max) {
		int ret = max+1;
		//the values between 200 and 5000 are reserved for fakes
		if( ret>=200 && ret<=5000 ){
			ret = 5001;
		}
		return ret;
	}

	public static Collection<DataSite> getSiteDataSetCollection() {
		Map<String, DataSite> allSitesForCommunity = getAllSites();
		if(allSitesForCommunity != null) {
			return allSitesForCommunity.values();
		}
		return null;
	}

	public static ArrayList<String> getAllServersNames(int commId, String stateAbrev, 
			String countyName, boolean onlyAutomaticEnabled, int productId) {
		ArrayList<String> vec = new ArrayList<String>();
		Collection<DataSite> datCollection = getSiteDataSetCollection();
		ArrayList<DataSite> arrayList = new ArrayList<DataSite>(datCollection);
		try{
		Collections.sort(arrayList);
		}catch(Exception e){}
		for (DataSite cur:arrayList) {
			if (cur.getStateAbbreviation().equals(stateAbrev) && cur.getCountyName().equals(countyName)) {
				if(!onlyAutomaticEnabled || Util.isSiteEnabledAutomaticForProduct(productId, cur.getEnabled(commId))){
					vec.add( cur.getName() );
				}
			}
		}
		return vec;
	}
	
	/**
	 * If automaticEnabled is <b>true</b> we return only sites enabled in general and in automatic.<br>
	 * A site can be enabled in automatic but disabled generally and we do not return them
	 * @param commId
	 * @param stateAbrev
	 * @param countyName
	 * @param automaticEnabled
	 * @param productId
	 * @return
	 */
	public static Vector<DataSite> getAllDataSites(int commId, String stateAbrev,
			String countyName, boolean automaticEnabled, int productId) {
		Vector<DataSite> vec = new Vector<DataSite>();

		Collection<DataSite> datCollection = getSiteDataSetCollection();
		
		for (DataSite cur:datCollection) {
			if (cur.getStateAbrev().equals(stateAbrev) && cur.getCountyName().equals(countyName)) {
				if(!automaticEnabled || 
						(cur.isEnableSite(commId) && Util.isSiteEnabledAutomaticForProduct(productId, cur.getEnabled(commId))) ){
					vec.add( cur);
					 
				}
			}
		}
		Collections.sort(vec);
		return vec;
	}
	
	
	/**
	 * If isEnabled is <b>true</b> we return only sites enabled in general<br> otherwise we return all sites.
	 * @param commId
	 * @param stateAbrev
	 * @param countyName
	 * @param automaticEnabled
	 * @param productId
	 * @return
	 */
	public static Vector<DataSite> getAllDataSites(int commId, String stateAbrev,
			String countyName, boolean isEnabled) {
		Vector<DataSite> vec = new Vector<DataSite>();
		
		Collection<DataSite> datCollection = getSiteDataSetCollection();
		
		for (DataSite cur:datCollection) {
			if (cur.getStateAbrev().equals(stateAbrev) && cur.getCountyName().equals(countyName)) {
				if((isEnabled ? cur.isEnableSite(commId) : true)){
					vec.add(cur);
				}
			}
		}
	
		return vec;
	}
	
	public static Util.SiteDataCompact getAllDataSitesForSearchSites(int commId, String stateAbrev,
			String countyName, String serverType) {
		
		/* Community -> State -> County -> STCountySRV -> GwtDataSite map */
			Map<String,
				Map<String,
					Map<String,
						GWTDataSite>>> sitesData = new HashMap<String,Map<String,Map<String,GWTDataSite>>>();	
		 
		addDataSitesForSearchSites(sitesData,stateAbrev,countyName);	
		
		SiteDataCompact sdc = new SiteDataCompact();
		sdc.setSitesData(sitesData);
		sdc.setSiteType(getServerTypeByAbbreviation(serverType));
		return sdc;
	}
	
	public static final int ATSS_INT = 0; // starter
	public static final int ATSE_INT = 1; // express
	public static final int ATS_INT = 2; // full
	public static final int ATSB_INT = 3; // Base Search saved
	public static final int ATSP_INT = 4; // Base Search not saved
	
	
	public static int getStarterTypeInt(int commId, String stateAbrev, String countyName, int productId){
		Vector<DataSite> allSites = getAllDataSites(commId, stateAbrev, countyName, false);
		
		Vector<DataSite> allRoLikeSites = getAllRoLikeSites(allSites);
		
		boolean starter = false;
		boolean express = false;
		boolean full = false;
		
		boolean checkOtherDataSources = checkOtherDataSources(commId, allSites, productId);
		
		if (checkOtherDataSources) {
			starter = true;
			
			for (DataSite ds : allRoLikeSites) {
				if(!ds.isEnableSite(commId)){
					continue;
				}
				
				boolean isTSServer = StringUtils.isNotBlank(ds.getTsServerClassName());
				
				if (Util.isSiteEnabledAutomaticForProduct(productId, ds.getEnabled(commId)) &&
						Util.isSiteCertified(ds.getSiteEnabled())) {
					full = true;
					break;
				}

				if ((Util.isSiteEnabledAutomaticForProduct(productId, ds.getEnabled(commId)) &&
						!Util.isSiteCertified(ds.getSiteEnabled())) || (isTSServer && !Util.isSiteEnabledAutomaticForProduct(productId, ds.getEnabled(commId)))) {
					express = true;
				}
			}
		}
		
		if(full){
			return ATS_INT;
		} else if(express){
			return ATSE_INT;
		} else if(starter){
			return ATSS_INT;
		}
		
		return -1;
	}
	
	private static Vector<DataSite> getAllRoLikeSites(Vector<DataSite> allSites) {
		Vector<DataSite> allRoLikeSites = new Vector<DataSite>();
		
		for(DataSite ds : allSites){
			if(GWTDataSite.REAL_RO_LIKE_TYPE_LIST.contains(ds.getSiteTypeInt())){
				allRoLikeSites.add(ds);
			}
		}
		
		return allRoLikeSites;
	}
	
	private static boolean checkOtherDataSources(int commId, Vector<DataSite> allSites, int productId) {
		boolean hasCertifiedAO = false;
		boolean hasCertifiedTR = false;
		boolean hasCertifiedATS = false;
		boolean hasCertifiedSSF = false;
		boolean hasCertifiedPA = false;
		
		for (DataSite ds : allSites) {
			if (ds.isEnableSite(commId) && Util.isSiteCertified(ds.getSiteEnabled()) && Util.isSiteEnabledAutomaticForProduct(productId, ds.getEnabled(commId))) {
				if (ds.isAssessorSite()) {
					hasCertifiedAO = true;
				} else if (ds.isTaxLikeSite()) {
					hasCertifiedTR = true;
				} else if (ds.isATSSite()) {
					hasCertifiedATS = true;
				} else if (ds.isSSFSite()) {
					hasCertifiedSSF = true;
				} else if (ds.isPASite()) {
					hasCertifiedPA = true;
				}
			}
			
			if(hasCertifiedAO && hasCertifiedTR && hasCertifiedATS && hasCertifiedSSF && hasCertifiedPA){
				break;
			}
		}
		
		return (hasCertifiedAO || hasCertifiedTR) && hasCertifiedATS && hasCertifiedSSF && hasCertifiedPA; // task 8599 comment 5
	}

	public static final String ATSS_STRING = "ATSS";
	public static final String ATSE_STRING = "ATSE";
	public static final String ATS_STRING = "ATS";
	public static final String ATSB_STRING = "ATSB";
	public static final String ATSP_STRING = "ATSP";
	
	public static String getStarterTypeString(int commId, String stateAbrev, String countyName, int productId) {
		switch (getStarterTypeInt(commId, stateAbrev, countyName, productId)) {
		default:
			return "";
		case ATSS_INT:
			return ATSS_STRING;
		case ATSE_INT:
			return ATSE_STRING;
		case ATS_INT:
			return ATS_STRING;
		}
	}
	
	private static synchronized void addDataSitesForSearchSites(Map<String,Map<String,Map<String,GWTDataSite>>> sitesData, String stateAbrev,String countyName) {
		
		Collection<DataSite> datCollection  = getSiteDataSetCollection();
		
		for (DataSite cur: datCollection) {
			String name = cur.getName();
			String stateNameAbrev = cur.getStateAbrev();
			String thisCountyName = cur.getCountyName().replaceAll("&nbsp;", " ");
			
			boolean allStates = ALL_STATE.equals(stateAbrev);
			boolean allCounty = ALL_COUNTY.equals(countyName);
			boolean thisState =  stateAbrev.equalsIgnoreCase(stateNameAbrev);
			boolean thisStateCounty = thisState && thisCountyName.equals(countyName); 
			if(allStates || (thisState && allCounty) || thisStateCounty){
				GWTDataSite site = cur.toSiteData();
				if(!sitesData.containsKey(stateAbrev)) {
					sitesData.put(stateAbrev, new HashMap<String,Map<String,GWTDataSite>>());
				}
				
				Map<String,Map<String,GWTDataSite>> stateMap = sitesData.get(stateAbrev);
				
				if(!stateMap.containsKey(thisCountyName)) {
					stateMap.put(thisCountyName, new HashMap<String,GWTDataSite>());
				}
				
				(stateMap.get(thisCountyName)).put(name,site);
				
			}
		}
		
	}
	
	
    private static Map<String,TaxDateStructure> readTaxDatesTable(){
    	final Map<String,TaxDateStructure> map = new HashMap<String,TaxDateStructure>();
	     DBManager.getJdbcTemplate().query("SELECT * FROM ts_tax_dates", 
	    		 new RowCallbackHandler() { 
		      		public void processRow(ResultSet rs) throws SQLException {
		      			TaxDateStructure  t = new TaxDateStructure();
		      			t.dueDate = ro.cst.tsearch.generic.Util.dateParser3(rs.getString(2));
		      			t.payDate = ro.cst.tsearch.generic.Util.dateParser3(rs.getString(3));
		      			t.taxYearMode = rs.getInt(4);
		      			map.put(rs.getString(1), t);
		      		}
		  		});  
	     return map;
    }
    
    public static Date getPayDate(int commId, long serverId){
    	DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(commId, (int)serverId);
        if(dat==null){
        	return null;
        }
        return dat.getPayDate();
    }
    
    public static int getTaxYearMode(int commId, long serverId){
    	DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(commId, (int)serverId);
        if(dat==null){
        	return TaxSiteData.TAX_YEAR_PD_YEAR;
        }
        return dat.getTaxYearMode();
    }
    
    public static String getCityName(int commId, long serverId){
    	DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(commId, (int)serverId);
        if(dat==null){
        	return null;
        }
        return dat.getCityName();
    }
    
    public static String getCityName(int commId,String stateAbrev,String countyName,int siteType){
    	long serverId = TSServersFactory.getSiteId(stateAbrev, countyName, HashCountyToIndex.getServerTypesAbrev()[siteType]);
    	DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(commId, (int)serverId);
        if(dat==null){
        	return null;
        }
        return dat.getCityName();
    }
    
    public static String getCityName(int commId,String stateAbrev,String countyName,String dataSource){
    	long serverId = TSServersFactory.getSiteId(stateAbrev, countyName, dataSource);
    	DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(commId, (int)serverId);
        if(dat==null){
        	return null;
        }
        return dat.getCityName();
    }
    
    
    public static Date getPayDate(int commId, String stateAbrev,String countyName,int siteType){
    	long serverId = TSServersFactory.getSiteId(stateAbrev, countyName, HashCountyToIndex.getServerTypesAbrev()[siteType]);
        DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(commId, (int)serverId);
        if(dat==null){
        	return null;
        }
        return dat.getPayDate();    
    }
    
    public static Date getPayDate(int commId, String stateAbrev,String countyName,DType type){
    	if (	type == DType.TAX	){
    		return getPayDate(commId,stateAbrev,countyName,GWTDataSite.TR_TYPE);
    	}
    	else if(type == DType.CITYTAX	){
    		return getPayDate(commId,stateAbrev,countyName,GWTDataSite.YA_TYPE);
    	}  
    	return null;
    }
    
    public static Date getDueDate(int commId, long serverId){
    	DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(commId, (int)serverId);
    	if(dat==null){ //we do not have EP site
    		return null;
    	}
    	return dat.getDueDate();
    }
    
    public static Date getDueDate(int commId, String stateAbrev,String countyName,int siteType){
    	long serverId = TSServersFactory.getSiteId(stateAbrev, countyName, HashCountyToIndex.getServerTypesAbrev()[siteType]);
        DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(commId, (int)serverId);
        if(dat==null){
        	return null;
        }
        return dat.getDueDate();
    }
    
    public static int getTaxYearMode(int commId, String stateAbrev,String countyName,int siteType){
    	long serverId = TSServersFactory.getSiteId(stateAbrev, countyName, HashCountyToIndex.getServerTypesAbrev()[siteType]);
        DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(commId, (int)serverId);
        if(dat==null){
        	return TaxSiteData.TAX_YEAR_PD_YEAR;
        }
        return dat.getTaxYearMode();
    }
    
    public static int getTaxYearMode(int commId, String stateAbrev,String countyName,DType type){
    	if (	type == DType.TAX	){
    		return getTaxYearMode(commId,stateAbrev,countyName,GWTDataSite.TR_TYPE);
    	}
    	else if(type == DType.CITYTAX	){
    		return getTaxYearMode(commId,stateAbrev,countyName,GWTDataSite.YA_TYPE);
    	}  
    	return TaxSiteData.TAX_YEAR_PD_YEAR;
    }
    
    
    public static Date getDueDate(int commId, String stateAbrev,String countyName,DType type){
    	if (type == DType.TAX){
    		return getDueDate(commId,stateAbrev,countyName,GWTDataSite.TR_TYPE);
    	}
    	else if(type == DType.CITYTAX){
    		return getDueDate(commId,stateAbrev,countyName,GWTDataSite.YA_TYPE);
    	}  
    	return null;
    }
    
    public static DataSite getDataSite(int commId, int p1, int p2) throws BaseException{
    	
    	int viServerID = HashCountyToIndex.getServerFactoryID(p1, p2);
    	DataSite data =  HashCountyToIndex.getDateSiteForMIServerID(String.valueOf(viServerID) );
    	if (data == null){
    		throw new BaseException("I can't find data site for server ID " + viServerID);
    	}
    	return data;
    }
    
    public static DataSite getCrtServer(long searchId, boolean isParentSite) throws BaseException{
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		Search search = currentInstance.getCrtSearchContext();
		String p1 = isParentSite?search.getP1ParentSite():search.getP1();
		String p2 = isParentSite?search.getP2ParentSite():search.getP2();
		int viServerID = HashCountyToIndex.getServerFactoryID(Integer.parseInt(p1), Integer.parseInt(p2));
		return  HashCountyToIndex.getDateSiteForMIServerID( String.valueOf(viServerID));
		
	}
    
    public static DataSite getServer(int countyId, String siteAbbreviation) {
		
    	if (countyId > 0
    			&& ro.cst.tsearch.utils.StringUtils.isNotEmpty(siteAbbreviation)) {
    		
			HashMap<String, DataSite> sitesForCommunity =  getCountiesMap(countyId);
			
			if (sitesForCommunity != null) {
				for (DataSite datasite : sitesForCommunity.values()) {
					if(datasite.getSiteTypeAbrev().equals(siteAbbreviation)) {
						return datasite;
					}
				}
			}
    	}

		return null;
		
	}

	public static Category getLogger() {
		return logger;
	}
	
	public static synchronized Map<String, DataSite> getAllSitesExternal() {
		return Collections.unmodifiableMap(allSites);
	}
	
	/**
	 * Returns the server position in automatic order (does not check if it is enabled or not)
	 * @param commId
	 * @param currentDataSite
	 * @return
	 */
    public static int getServerIndex(int commId, DataSite currentDataSite) {
    	if (currentDataSite != null ) {
    		return getServerIndex(currentDataSite.getCountyId(), currentDataSite.getSiteTypeInt());
    	}
    	return 0;
    }
    
    /**
     * Returns the server position in automatic order (does not check if it is enabled or not)
     * @param commId community to check
     * @param stCountyKey STCounty key (for example ILCook)
     * @param siteType the id from database (for example 25 for NDB)
     * @return
     */
    public static int getServerIndex(int countyId, int siteType) {
    	if (countyId > 0) {

    		HashMap<String, DataSite> sitesForCommunity =  getCountiesMap(countyId);
			
			if (sitesForCommunity != null) {
				ArrayList<DataSite> siteArray = new ArrayList<DataSite>(sitesForCommunity.values());
				Collections.sort(siteArray);
				int i = 0;
				for (DataSite datasite : siteArray) {
					if(siteType == datasite.getSiteTypeInt()) {
						return i;
					}
					i++;
				}
			}
    	}
    	return 0;
    }
    
    public static DataSite getNextServer(int productId, int commId, DataSite currentDataSite) {
    	
    	if (currentDataSite != null) {
    		
			HashMap<String, DataSite> sitesForCommunity =  getCountiesMap(currentDataSite.getCountyId());
			ArrayList<DataSite> siteArray = new ArrayList<DataSite>(sitesForCommunity.values());
			Collections.sort(siteArray);
			
			for (int i = 0; i < siteArray.size() - 1; i++) {
				
				DataSite searchServer = siteArray.get(i);
				DataSite nextSearchServer = siteArray.get(i + 1);
				
				if (currentDataSite.getName().equals(searchServer.getName())) {
                    if ( Util.isSiteEnabled(nextSearchServer.getEnabled(commId)) && 
                    		Util.isSiteEnabledAutomaticForProduct(productId, nextSearchServer.getEnabled(commId)))  {
                    	return nextSearchServer;
                    } else {
                        return getNextServer(productId, commId, nextSearchServer);
                    }
				}
			}
    	}
    	
    	return null;
    }
    
    public static int getFirstServer(int productId, int commId, int countyId ) {
		
		if (countyId > 0) {
			
			HashMap<String, DataSite> sitesForCommunity =  getCountiesMap(countyId);
			if(sitesForCommunity != null) {
				ArrayList<DataSite> allSites = new ArrayList<DataSite>(sitesForCommunity.values());
				Collections.sort(allSites);
				
				for (DataSite dataSite : allSites) {
					if (Util.isSiteEnabled(dataSite.getEnabled(commId)) && Util.isSiteEnabledAutomaticForProduct(productId, dataSite.getEnabled(commId)))
						return dataSite.getSiteTypeInt();
				}
			}
		}
		
		return 0;
	}
    
    public static Map<String, DataSite> getServers(int countyId) {
		
		if (countyId > 0) {			
			return getCountiesMap(countyId);
		}
		
		return null;
	}
    
	public static HashMap<String, DataSite> getCountiesMap(int countyId) {

		HashMap<String, DataSite> sitesForStateCountyKey = allSitesByLocation.get(countyId);
		
		return sitesForStateCountyKey;
	}

	/**
	 * Returns the siteType by abbreviation (for example for NB it returns 25)
	 * @param serverAbbreviation
	 * @return
	 */
	public static int getServerTypeByAbbreviation(String serverAbbreviation) {
		try {
			Integer result = serverAbbrevSiteTypeMap.get(serverAbbreviation);
			if(result != null) {
				return result;
			}
			return -1;
		} catch (Exception e) {
			logger.error("Error while getting server type", e);
		}
		return -1;
	}
	
	/**
	 * Returns the serverAbbreviation by siteType (for example for 25 it returns NB)
	 * @param siteType
	 * @return
	 */
	public static String getServerAbbreviationByType(int siteType) {
		try {
			String serverAbbreviation = siteTypeServerAbbrevSiteTypeMap.get(siteType);
			if(serverAbbreviation != null) {
				return serverAbbreviation;
			}
		} catch (Exception e) {
			logger.error("Error while getting server type", e);
		}
		return null;
	}

	/**
	 * Returns the serverDescription by siteType (for example for 37 it returns Register Office)
	 * @param siteType
	 * @return
	 */
	public static String getServerDescriptionByType(int siteType) {
		try {
			String serverDescription = siteTypeServerDescriptionMap.get(siteType);
			if (serverDescription != null) {
				return serverDescription;
			}
		} catch (Exception e) {
			logger.error("Error while getting server type", e);
		}
		return null;
	}
	
    public static void main(String[] args) {
		for(Object couuntyO:County.getCountyList().values()){
			if(couuntyO instanceof County){
				
			}
		}
	}

	public static boolean isNameBootstrapEnabled(int commId, int countyId, String siteAbbreviation) {
		DataSite data = getServer(countyId, siteAbbreviation);
		return (data!=null && Util.isSiteEnabledNameBootstrap(data.getEnabled(commId)));		
	}
	public static boolean isAddressBootstrapEnabled(int commId, int countyId, String siteAbbreviation) {
		DataSite data = getServer(countyId, siteAbbreviation);
		return (data!=null && Util.isSiteEnabledAddressBootstrap(data.getEnabled(commId)));		
	}
	public static boolean isLegalBootstrapEnabled(int commId, int countyId, String siteAbbreviation) {
		DataSite data = getServer(countyId, siteAbbreviation);
		return (data!=null && Util.isSiteEnabledLegalBootstrap(data.getEnabled(commId)));		
	}

	public static boolean isNameBootstrapEnabled(int commId, long siteId) {
		DataSite data = getDateSiteForMIServerID(commId, siteId);
		return (data!=null && Util.isSiteEnabledNameBootstrap(data.getEnabled(commId)));		
	}
	public static boolean isAddressBootstrapEnabled(int commId, long siteId) {
		DataSite data = getDateSiteForMIServerID(commId, siteId);
		return (data!=null && Util.isSiteEnabledAddressBootstrap(data.getEnabled(commId)));		
	}
	public static boolean isLegalBootstrapEnabled(int commId, long siteId) {
		DataSite data = getDateSiteForMIServerID(commId, siteId);
		return (data!=null && Util.isSiteEnabledLegalBootstrap(data.getEnabled(commId)));		
	}
}
