package ro.cst.tsearch.database;

import static ro.cst.tsearch.utils.DBConstants.FIELD_ATSFOLDER_CONTENTS;
import static ro.cst.tsearch.utils.DBConstants.FIELD_ATSFOLDER_FILENAME;
import static ro.cst.tsearch.utils.DBConstants.FIELD_FILTER_TESTFILES_CONTENT;
import static ro.cst.tsearch.utils.DBConstants.FIELD_FILTER_TESTFILES_ID;
import static ro.cst.tsearch.utils.DBConstants.FIELD_FILTER_TESTFILES_NAME;
import static ro.cst.tsearch.utils.DBConstants.FIELD_LBS_SOURCES_ADDRESS;
import static ro.cst.tsearch.utils.DBConstants.FIELD_LBS_SOURCES_CLNT_COMMNAME;
import static ro.cst.tsearch.utils.DBConstants.FIELD_LBS_SOURCES_CLNT_USERNAME;
import static ro.cst.tsearch.utils.DBConstants.FIELD_LBS_SOURCES_ENABLE;
import static ro.cst.tsearch.utils.DBConstants.FIELD_LBS_SOURCES_ID;
import static ro.cst.tsearch.utils.DBConstants.FIELD_LBS_SOURCES_NETMASK;
import static ro.cst.tsearch.utils.DBConstants.FIELD_LBS_SOURCES_REDIRECT_ADDRESS;
import static ro.cst.tsearch.utils.DBConstants.FIELD_PARTIES_CATEGORY;
import static ro.cst.tsearch.utils.DBConstants.FIELD_PARTIES_TYPE;
import static ro.cst.tsearch.utils.DBConstants.FIELD_PASSWORD_MACHINE_NAME;
import static ro.cst.tsearch.utils.DBConstants.FIELD_PASSWORD_NAME;
import static ro.cst.tsearch.utils.DBConstants.FIELD_PASSWORD_SITE;
import static ro.cst.tsearch.utils.DBConstants.FIELD_PASSWORD_VALUE;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SEARCH_DATA_CONTEXT;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SEARCH_DATA_DATESTRING;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SEARCH_DATA_SEARCHID;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SEARCH_DATA_VERSION;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SERVER_ALIAS;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SERVER_ENABLED;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SERVER_ID;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SERVER_IP_ADDRESS;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SERVER_IP_MASK;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SERVER_NAME;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SERVER_PATH;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SUBDIVISIONS_IL_KANE_CODE;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SUBDIVISIONS_IL_KANE_NAME;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SUBDIVISIONS_IL_KANE_PLAT_DOC;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SUBDIVISIONS_MACOMB_AREA;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SUBDIVISIONS_MACOMB_CODE;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SUBDIVISIONS_MACOMB_ID;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SUBDIVISIONS_MACOMB_NAME;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SUBDIVISIONS_MACOMB_PHASE;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SUBDIVISIONS_MACOMB_TYPEID;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SUBDIVISIONS_OAKLAND_AREA;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SUBDIVISIONS_OAKLAND_CODE;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SUBDIVISIONS_OAKLAND_ID;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SUBDIVISIONS_OAKLAND_NAME;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SUBDIVISIONS_OAKLAND_TYPEID;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SUBDIVISIONS_OAKLAND_TYPE_ID;
import static ro.cst.tsearch.utils.DBConstants.REPORTS_HEIGHT;
import static ro.cst.tsearch.utils.DBConstants.REPORTS_WIDTH;
import static ro.cst.tsearch.utils.DBConstants.SEARCH_NOT_SAVED;
import static ro.cst.tsearch.utils.DBConstants.SEARCH_PAGE_HEIGHT;
import static ro.cst.tsearch.utils.DBConstants.SEARCH_PAGE_WIDTH;
import static ro.cst.tsearch.utils.DBConstants.TABLE_ATS_FOLDER_FILES;
import static ro.cst.tsearch.utils.DBConstants.TABLE_CITY;
import static ro.cst.tsearch.utils.DBConstants.TABLE_COMMUNITY;
import static ro.cst.tsearch.utils.DBConstants.TABLE_COMMUNITY_TEMPLATES;
import static ro.cst.tsearch.utils.DBConstants.TABLE_COUNTY;
import static ro.cst.tsearch.utils.DBConstants.TABLE_DUE_DATE;
import static ro.cst.tsearch.utils.DBConstants.TABLE_FILTER_TEST_FILES;
import static ro.cst.tsearch.utils.DBConstants.TABLE_INTERFACE_SETTINGS;
import static ro.cst.tsearch.utils.DBConstants.TABLE_LBS_SOURCES;
import static ro.cst.tsearch.utils.DBConstants.TABLE_LOCKED_SEARCHES;
import static ro.cst.tsearch.utils.DBConstants.TABLE_MIMACOMBCO_PARTIES;
import static ro.cst.tsearch.utils.DBConstants.TABLE_MIWAYNEPR_PARTIES;
import static ro.cst.tsearch.utils.DBConstants.TABLE_PASSWORDS;
import static ro.cst.tsearch.utils.DBConstants.TABLE_PAYRATE;
import static ro.cst.tsearch.utils.DBConstants.TABLE_PROPERTY;
import static ro.cst.tsearch.utils.DBConstants.TABLE_SEARCH;
import static ro.cst.tsearch.utils.DBConstants.TABLE_SEARCH_DATA;
import static ro.cst.tsearch.utils.DBConstants.TABLE_SERVER;
import static ro.cst.tsearch.utils.DBConstants.TABLE_STATE;
import static ro.cst.tsearch.utils.DBConstants.TABLE_SUBDIVISIONS;
import static ro.cst.tsearch.utils.DBConstants.TABLE_SUBDIVISIONS_HAMILTON;
import static ro.cst.tsearch.utils.DBConstants.TABLE_SUBDIVISIONS_IL_KANE;
import static ro.cst.tsearch.utils.DBConstants.TABLE_SUBDIVISIONS_MACOMB;
import static ro.cst.tsearch.utils.DBConstants.TABLE_SUBDIVISIONS_OAKLAND;
import static ro.cst.tsearch.utils.DBConstants.TABLE_SUBDIVISIONS_OAKLAND_TYPE;
import static ro.cst.tsearch.utils.DBConstants.TABLE_TESTCASE;
import static ro.cst.tsearch.utils.DBConstants.TABLE_TESTSUITE;
import static ro.cst.tsearch.utils.DBConstants.TABLE_USER;
import static ro.cst.tsearch.utils.DBConstants.TABLE_USER_COUNTY;
import static ro.cst.tsearch.utils.DBConstants.TABLE_USER_RATING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchFlags.CREATION_SOURCE_TYPES;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.AutomaticTester.AutomaticSearchJob;
import ro.cst.tsearch.AutomaticTester.AutomaticTesterManager;
import ro.cst.tsearch.AutomaticTester.TestSuite;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityManager;
import ro.cst.tsearch.community.CommunityProducts;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.connection.dasl.DTError;
import ro.cst.tsearch.data.CountyCommunityManager;
import ro.cst.tsearch.data.CountyState;
import ro.cst.tsearch.data.DataAttribute;
import ro.cst.tsearch.data.DueDate;
import ro.cst.tsearch.data.GenericCounty;
import ro.cst.tsearch.data.GenericState;
import ro.cst.tsearch.data.ILKaneSubdivisions;
import ro.cst.tsearch.data.OaklandSubdivisions;
import ro.cst.tsearch.data.Payrate;
import ro.cst.tsearch.data.PayrateConstants;
import ro.cst.tsearch.data.SearchData;
import ro.cst.tsearch.data.SearchToArchive;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.data.Subdivisions;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.procedures.TableReportProcedure;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.database.rowmapper.CountyDefaultLegalMapper;
import ro.cst.tsearch.database.rowmapper.DocumentIndexMapper;
import ro.cst.tsearch.database.rowmapper.FVSMapper;
import ro.cst.tsearch.database.rowmapper.HoaInfoMapper;
import ro.cst.tsearch.database.rowmapper.LoadSearchContextMapper;
import ro.cst.tsearch.database.rowmapper.NoteMapper;
import ro.cst.tsearch.database.rowmapper.NoteMapper.TYPE;
import ro.cst.tsearch.database.rowmapper.PayrateATSSettingsMapper;
import ro.cst.tsearch.database.rowmapper.ProductsMapper;
import ro.cst.tsearch.database.rowmapper.SearchDatesRowMapper;
import ro.cst.tsearch.database.rowmapper.SearchExternalFlag;
import ro.cst.tsearch.database.rowmapper.SearchIdRowMapper;
import ro.cst.tsearch.database.rowmapper.SearchUpdateMapper;
import ro.cst.tsearch.database.transactions.InsertSearchFilesTransaction;
import ro.cst.tsearch.database.transactions.NextIdTransaction;
import ro.cst.tsearch.database.transactions.OverrideAbstractorTransaction;
import ro.cst.tsearch.database.transactions.RemoveKStatusTransaction;
import ro.cst.tsearch.database.transactions.SaveSearchTransaction;
import ro.cst.tsearch.database.transactions.SetKStatusTransaction;
import ro.cst.tsearch.database.transactions.SetSearchOwnerTransaction;
import ro.cst.tsearch.database.transactions.UpdateArchivedSearchedTransaction;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.SaveSearchException;
import ro.cst.tsearch.exceptions.UpdateDBException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.loadBalServ.LoadBalancingStatus;
import ro.cst.tsearch.loadBalServ.ServerInfoEntry;
import ro.cst.tsearch.loadBalServ.ServerSourceEntry;
import ro.cst.tsearch.propertyInformation.PossessionEntity;
import ro.cst.tsearch.reports.invoice.InvoicedSearch;
import ro.cst.tsearch.search.filter.testnamefilter.GenericNameFilterTestFiles;
import ro.cst.tsearch.searchsites.client.FilterUtils;
import ro.cst.tsearch.searchsites.client.IncreaseDueOrPayDate;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.CountyWithState;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servers.types.CertificationDateManager;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.servlet.DistributedMutex;
import ro.cst.tsearch.servlet.FileServlet;
import ro.cst.tsearch.servlet.download.DownloadFile;
import ro.cst.tsearch.settings.InterfaceSettings;
import ro.cst.tsearch.threads.AsynchSearchLogSaverThread;
import ro.cst.tsearch.threads.CommAdminNotifier;
import ro.cst.tsearch.threads.GPThread;
import ro.cst.tsearch.threads.MonitoringService;
import ro.cst.tsearch.tsr.PrefixFilenameFilter;
import ro.cst.tsearch.user.AgentAttributes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.user.UserRates;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FileLogger;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.Formater;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.SearchReserved;
import ro.cst.tsearch.utils.SharedDriveUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.SubdivisionMatcher;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.utils.ZipUtils;
import ro.cst.tsearch.webservices.PlaceOrderService;

import com.mchange.v2.c3p0.impl.NewProxyCallableStatement;
import com.stewart.ats.archive.ArchiveEntry;
import com.stewart.datatree.DataTreeStruct;
import com.stewartworkplace.starters.ssf.profile.ProfileMapper;
import com.stewartworkplace.starters.ssf.profile.ProfileStruct;

public class DBManager {
    
	private static final int NR_MAX_TRY_LOCK = ServerConfig.getNumberMaxTryLock(5) ;
	
	/**
    * @param seqName - the name of the table that keeps the sequences
    * @return next sequence available for that table
    */
    private static final Logger logger = Logger.getLogger(DBManager.class);
    
    protected static final Logger loggerLocal = PlaceOrderService.loggerLocal;
    
    private static GenericCounty[] allCounties = new GenericCounty[0];
    private static boolean allCountiesLoaded = false;
    private static GenericState[] allStates = new GenericState[0];
    private static boolean allStatesLoaded = false;    
    
    public static final Pattern stringForDatePattern = Pattern.compile("\\sr([0-9]+|n)\\s", Pattern.CASE_INSENSITIVE);
    public static final Pattern pattNeedByDate = Pattern.compile("(?i)\\b(?:(?:Need\\s*By\\s*Date)|(?:Date\\s*Needed))\\s*:\\s*(\\d{1,2})(?:/|-)(\\d{1,2})(?:/|-)(\\d{1,4})\\b", Pattern.CASE_INSENSITIVE);
    public static final Pattern pattAdditInstr = Pattern.compile("(?i)\\bAdditional\\s*Instructions\\s*:\\s*R(\\d+)\\b", Pattern.CASE_INSENSITIVE);
    public static final Pattern pattRush = Pattern.compile("(?i)\\bAdditional\\s*Instructions\\s*:\\s*(Rush)\\b", Pattern.CASE_INSENSITIVE);
    public static final Pattern pattNoRush = Pattern.compile("(?i)\\bAdditional\\s*Instructions\\s*:\\s*(No Rush)\\b", Pattern.CASE_INSENSITIVE);
    public static final String viewImageRegex = "(?is)<a [^\\>]+>View image.*?</a>";
    public static final String displayDocRegex = "(?is)<a [^\\>]+>Display Doc.*?</a>";
    
    // default port value
	private static final int appPort = ServerConfig.getAppPortFromAppUrl(80);
		
	/**
	 * Obtain application pot
	 * @return port
	 */
	public static int getAppPort(){        
        return appPort;
	}    
	
    public enum PartySites {
    	MIMACOMBCO,
    	MIWAYNEPR
    }   
    private static Map<PartySites, Map<String, Integer>> allParties =  new EnumMap<PartySites, Map<String, Integer>>(PartySites.class);
    private static Map <PartySites, Boolean> allPartiesLoaded = new EnumMap<PartySites, Boolean>(PartySites.class);;
    public static Map <PartySites, String> partyTables = new EnumMap<PartySites, String>(PartySites.class);
    static{
    	allParties.put(PartySites.MIMACOMBCO, new HashMap<String, Integer>());
    	allParties.put(PartySites.MIWAYNEPR, new HashMap<String, Integer>());    	    	
    	allPartiesLoaded.put(PartySites.MIMACOMBCO, false);
    	allPartiesLoaded.put(PartySites.MIWAYNEPR, false);
    	partyTables.put(PartySites.MIMACOMBCO, TABLE_MIMACOMBCO_PARTIES);
    	partyTables.put(PartySites.MIWAYNEPR, TABLE_MIWAYNEPR_PARTIES);
    }
            
    private static String dbOffset = null;    
    
    public static long getLastId(DBConnection conn) throws BaseException{
    	DatabaseData dbData = conn.executeSQL("SELECT LAST_INSERT_ID()");
    	return (new BigDecimal(dbData.getValue(1, 0).toString()).longValue());
    	
    }
    //just for TABLE_SEARCH
    public static long getNextId(String tableName){
    	if(!tableName.equalsIgnoreCase(TABLE_SEARCH)){
    		System.err.println("TABLE NAME IS INVALID...");
    		logger.debug("TABLE NAME IS INVALID...");
    		logger.error("TABLE NAME IS INVALID...");
    		logger.info("TABLE NAME IS INVALID...");
    		return 0;
    	}
    	Object result = getTransactionTemplate().execute(new NextIdTransaction());    	
    	return ((Long)result).longValue();
    }
    
    /**
     * 
     * @param tableName the name of the table from which you want a new id
     * @param param the value of a needed param
     * @return
     */
    public static long getNextId(String tableName, String param){
    	long id = 0;
    	DBConnection conn = null;
    	String stm = null;
    	try {
			conn = ConnectionPool.getInstance().requestConnection();
			if(tableName.equalsIgnoreCase(TABLE_COMMUNITY_TEMPLATES)){
				//CommunityAttributes ca = InstanceManager.getCurrentInstance().getCurrentCommunity();
				stm = "INSERT INTO " + TABLE_COMMUNITY_TEMPLATES + " (COMM_ID, LAST_UPDATE) VALUES ( ?, NOW())";
			}
			else throw new BaseException("TABLE NAME IS INVALID...");
			
			PreparedStatement pstmt = conn.prepareStatement( stm );
			pstmt.setString( 1, param);
			pstmt.executeUpdate();

			id = getLastId(conn);
			pstmt.close();
			logger.debug("DBManager: getNextSearchId... dummy insert... just for getting an id...");
		} catch (Exception e) {
			logger.error(e);
		} finally {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch (BaseException e) {
                logger.error(e);
            }
        }
    	
    	return id;
    }
    
    
    public static String getDBOffset(){
    	if(dbOffset==null){
    		DBConnection conn = null;
    		dbOffset = " INTERVAL ";
    		String sql = "select substr(@@global.time_zone,1,3) ";
    		try {
				conn = ConnectionPool.getInstance().requestConnection();
				DatabaseData dbData = conn.executeSQL(sql);
				if (dbData.getValue(1, 0) != null)
					dbOffset += ((String)dbData.getValue(1, 0));
				else
					dbOffset += "0";
				dbOffset += " HOUR ";
			} catch (BaseException e) {
				dbOffset = "INTERVAL 0 HOUR";
				logger.error(e.getMessage());
				logger.error(e.getStackTrace());
			} finally {
	             try {
	                 ConnectionPool.getInstance().releaseConnection(conn);
	             } catch (BaseException e) {
	                 logger.error(e);
	             }
	         }
    	}
    	return dbOffset;
    }
    
     public static String getShortNameSubdHamilton(String subd)  {

         String shortSubd = "";
         
         String stm = "SELECT short_name FROM "+TABLE_SUBDIVISIONS_HAMILTON + " WHERE upper(name) = ? "; 
         
         
         try {
        	 shortSubd = DBManager.getSimpleTemplate().queryForObject(stm, String.class, subd.toUpperCase());
		} catch (Exception e) {
			logger.error(e);
		}
        
         return shortSubd; 
     }
     
	private static String subdHamilton = null; 

	public static String getComboSubdHamilton() {

		if (subdHamilton != null)
			return subdHamilton;

		DBConnection conn = null;
		StringBuffer comboSubd = new StringBuffer(
				"<SELECT NAME=\"ctl00$MainContent$ddlSubDivisions\">");

		try {

			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData dbData = conn.executeSQL("select short_name, name from "
							+ TABLE_SUBDIVISIONS_HAMILTON);

			for (int i = 0; i < dbData.getRowNumber(); i++/*
														 * =dbData.getRowNumber()
														 * /800
														 */) // test only
			{

				comboSubd.append("<OPTION value='"
						+ (String) dbData.getValue(1, i) + "'>"
						+ (String) dbData.getValue(2, i) + "</OPTION>\r\n");

			}
		} catch (Exception e) {
			logger.error(e);
		} finally {
			try {
				ConnectionPool.getInstance().releaseConnection(conn);
			} catch (BaseException e) {
				logger.error(e);
			}
		}
		comboSubd.append("</SELECT>");

		subdHamilton = comboSubd.toString();

		return subdHamilton;

	}
    
     public static String getHamiltonSubdivByCode( String code ) {
    	 if( "".equals( code ) ) {
    		 return "";
    	 }
    	 
    	 String stm = "SELECT  name FROM "+TABLE_SUBDIVISIONS_HAMILTON + " WHERE  short_name = ? ";
    	 String hamiltonSubdiv = "";
    	 
         try {
        	 hamiltonSubdiv = DBManager.getSimpleTemplate().queryForObject(stm, String.class, code);
		} catch (Exception e) {
			logger.error(e);
		}
		
		return hamiltonSubdiv;
     }
    
    
    public static GenericCounty[] getAllCountiesSortedWithoutDuplicate() {

		GenericCounty all[] = getAllCounties();
		TreeSet<GenericCounty> t = new TreeSet<GenericCounty>();
		t.addAll(Arrays.asList(all));
		all = (GenericCounty[]) t.toArray(new GenericCounty[0]);
		return all;

	}
    
    public static GenericCounty[] getAllCounties(){
        
        if (allCountiesLoaded)
            return allCounties;
            
        DBConnection conn = null;
        GenericCounty all[] = new GenericCounty[0];
        
        String stm = " select a.ID, a.NAME, a.STATE_ID, a.COUNTYFIPS from "+TABLE_COUNTY+" a order by a.NAME"; 
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData data = conn.executeSQL(stm);
            
            all = new GenericCounty[data.getRowNumber()];
            for (int i = 0; i<data.getRowNumber(); i++){
                all[i] = new GenericCounty();
                all[i].setId(Long.parseLong(data.getValue(1,i).toString()));
                all[i].setName((String)data.getValue(2,i));
                all[i].setStateId(Long.parseLong(data.getValue(3,i).toString()));
                all[i].setCountyFips((String)data.getValue(4,i));
            }               
            
        } catch (Exception e) {
            logger.error(e);
        }finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }
            
        }
        
        allCounties = all;
        allCountiesLoaded = true;
        return all;
    }
    
    
    
    public static GenericCounty getCountyForId(long id){
        
        GenericCounty[] all = getAllCounties();
        GenericCounty result = all[0];
        for (int i = 0; i < all.length; i++)
            if (all[i].getId() == id)
                return all[i];
        return result;
    }
    
    public static GenericCounty getCountyForNameAndStateId(String name, long stateId){
        
        GenericCounty[] all = getAllCounties();
        for (int i = 0; i < all.length; i++)
            if (all[i].getStateId() == stateId && name.equalsIgnoreCase(all[i].getName()))
                return all[i];
        return null;
    }
    
    public static GenericCounty getCountyForNameAndStateIdStrict(String name, long stateId,boolean replaceSpace){
        for(GenericCounty county: getAllCounties() ){
         if(replaceSpace){
        	 if (county.getStateId() == stateId && (name.replaceAll(" ","")).equalsIgnoreCase(county.getName().replaceAll(" ","")))
                 return county; 
         }
         else{
        	 if (county.getStateId() == stateId && name.equalsIgnoreCase(county.getName()))
                return county;
         }
        }
       return null;
    }
    
    public static GenericCounty getCountyForFipsAndStateIdStrict(String countyFips, long stateId){
    	for (GenericCounty county : getAllCounties()){
    		if (county.getStateId() == stateId && countyFips.equals(county.getCountyFips())){
    			return county;
    		}
        }
       
    	return null;
    }
    
 private static void sendWarningMailMessage( Search search, String warning, String  message){
		
    	//notify technical support
	 	CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(search.getSearchID());
	 	
		UserAttributes currentUser = currentInstance.getCurrentUser();
		CommunityAttributes comAtr = currentInstance.getCurrentCommunity();
		
        try {
        	
        	String htmlBody = "<html>"+"<body>";
        	htmlBody += message;
        	htmlBody  += "</body>" +"</html>";
        	
			Properties props = System.getProperties();
			props.put("mail.smtp.host", MailConfig.getMailSmtpHost());
			javax.mail.Session session = javax.mail.Session.getDefaultInstance(props,null);
			
			MimeMessage msg = new MimeMessage(session);
			
			InternetAddress fromAddress = null;
			try {
			    fromAddress = new InternetAddress(currentUser.getEMAIL());
			} catch (Exception ex) {
			    fromAddress = new InternetAddress(MailConfig.getMailFrom());
			}
			
			String userFullName ="not determined";
			try{userFullName = currentUser.getUserFullName();}catch(Exception e){}
			
			String comunity = "not determined";
			try{comunity  = comAtr.getNAME();}catch(Exception e){}
			
			msg.setFrom(fromAddress);
			
			msg.setSubject( search.getSa().getAtribute("ORDERBY_FILENO") +" "+ warning + " USER: "+ userFullName  + " Comunity: "+comunity+" !");
			msg.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(MailConfig.getSupportEmailAddress()));
			msg.setRecipients(javax.mail.Message.RecipientType.CC, InternetAddress.parse(MailConfig.getExceptionEmail()));
			msg.setContent(htmlBody, "text/html");
			Transport.send(msg);
			
		} catch(Exception e){
			e.printStackTrace();
		}
	}
    
    public static void zipAndSaveSearchToDB( Search context ) throws Exception{
		try{
			synchronized( context ){
				byte[] searchContext = ZipUtils.zipContext( context.getSearchDir() , context, true );
			    if( searchContext.length > Search.MAX_CONTEXT_FILE_SIZE ){
			    	context.setMaxContextEvent( true );			    	
			    	Search.sendWarningMailMessage( context, " - Search exceeded maximum size ordered by agent " );
			    }
			    else{
			    	DBManager.saveCurrentSearchData( context, searchContext );
			    }		
			}
		}
		catch(Exception e){
			e.printStackTrace();
			try{
				Search.sendWarningMailMessage( context, " - Search context can't be saved " );
			}
			catch(Exception e1){
				e1.printStackTrace();
			}
			loggerLocal.error(" DBmanager  --- zipAndSaveSearchToDB "+
					" Search origin: "+context.getSa().getAtribute(SearchAttributes.SEARCH_ORIGIN)+""+
					" AbstrFileNo: "+ context.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO)+""+
					" AgentFileNo: "+ context.getSa().getAtribute(SearchAttributes.ORDERBY_FILENO)+"\n"+
					ExceptionUtils.getFullStackTrace(e)+"\n"
					);
			throw e;
		}
	}
    
    
    public static GenericState[] getAllStates(){
        
        if (allStatesLoaded)
            return allStates;
            
        DBConnection conn = null;
        GenericState all[] = new GenericState[0];
        
        String stm = " select a.ID, a.NAME, a.STATEABV, a.stateFIPS from "+ TABLE_STATE +" a order by a.NAME"; 
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();            
            DatabaseData data = conn.executeSQL(stm);
            
            all = new GenericState[data.getRowNumber()];
            for (int i = 0; i<data.getRowNumber(); i++){
                all[i] = new GenericState();
                all[i].setId(Long.parseLong(data.getValue(1,i).toString()));
                all[i].setName((String)data.getValue(2,i));
                all[i].setStateAbv((String)data.getValue(3,i));
                all[i].setStateFips(Integer.parseInt(data.getValue(4,i).toString()));
            }               
            
            
        } catch (BaseException e) {
            logger.error(e);
        } finally {
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }   
        
        allStates = all;
        allStatesLoaded = true;
        
        return all;
    }
    
    /**
     * If you have stateFips as String pass parameters as: stateFipsString and 0
     * If you have stateFips as int pass parameters as: null and stateFipsInt
     * @param stateFips
     * @param stateFipsInt
     * @return
     */
    public static GenericState getStateForFips(String stateFipsString, int stateFipsInt){
        
    	int stateFips = 0;
    	if (org.apache.commons.lang.StringUtils.isEmpty(stateFipsString) && stateFipsInt > 0){
    		stateFips = stateFipsInt;
    	}
    	if (org.apache.commons.lang.StringUtils.isNotEmpty(stateFipsString) && stateFipsInt == 0){
    		stateFips = Integer.parseInt(stateFipsString);
    	}
    	
    	GenericState[] all = getAllStates();
        GenericState result = all[0];
        
        for (int i = 0; i < all.length; i++){
            if (all[i].getStateFips() == stateFips)
                return all[i];
        }
        
        return result;
    }

    public static GenericState getStateForId(long id){
        
        GenericState[] all = getAllStates();
        GenericState result = all[0];
        for (int i = 0; i < all.length; i++)
            if (all[i].getId() == id)
                return all[i];
        return result;
    }
    
    public static GenericState getStateForAbv(String abv){
        GenericState[] all = getAllStates();
        for (int i = 0; i < all.length; i++)
            if (abv.equals(all[i].getStateAbv()))
                return all[i];
        return null;
    }
    
    public static GenericState getStateForAbvStrict(String abv){        
        abv = abv.toUpperCase();
        for (GenericState state: getAllStates()){
        	if(abv.equals(state.getStateAbv())){
        		return state;
        	}
        }
        return null;
    }
    
    public static Map<String, Integer> getAllParties(PartySites site){
        
    	if (allPartiesLoaded.get(site))
    		return allParties.get(site);
    		
        DBConnection conn = null;
        Map<String, Integer> all = new HashMap<String, Integer>();
        
        String stm = " select a." + FIELD_PARTIES_TYPE +  ", a." + FIELD_PARTIES_CATEGORY + " from "+ partyTables.get(site) +" a"; 
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();            
            DatabaseData data = conn.executeSQL(stm);
            
            for (int i = 0; i<data.getRowNumber(); i++){
                all.put(data.getValue(1,i).toString(), new Integer(data.getValue(2,i).toString()));
            }               
            
            
        } catch (BaseException e) {
            logger.error(e);
        } finally {
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }   
        
        allParties.put(site, all);
        allPartiesLoaded.put(site, true);
        
        return all;
    }
    
    /**
     * Classifies a party type from a CO or PR document. Possible types are stored into the database.
     * A party type from database is associated with one of the categories: 0 (unknown yet), 1 (grantor), 2 (grantee). 
     * If the party type is not found in database or it's category is unknown (0), the party type category will be defaulted to grantor (1).   
     * @param type	Party Type
     * @param site	Site
     * @return Party Type Category (possible values: 1 for grantor, 2 for grantee)
     */
    public static int getPartyType(String type, PartySites site){
        int result = 1;
        if (type == null || type.length() == 0)
        	return result;
        
        Map<String, Integer> all = getAllParties(site);
        Integer val = all.get(type);
        if (val != null){
        	result = val.intValue();
            if (result == 0){
            	result = 1;
            }        	
        }
        return result;
    }
    
    /**
     * holder for lazy initialization
     * @author radu bacrau
     */
    private static class AllCountiesForStateHolder{
    	
    	static final Map<Integer,List<CountyWithState>> stateCountyList = loadAllCountiesForState();
    	static final List<CountyWithState> allCounties = new ArrayList<CountyWithState>();
        
    	static{
        	for(List<CountyWithState> crtStateList: stateCountyList.values()){
        		allCounties.addAll(crtStateList);
        	}
        }
        
    	private static Map<Integer,List<CountyWithState>> loadAllCountiesForState(){
            
    		Map<Integer,List<CountyWithState>> stateCountyList = new HashMap<Integer,List<CountyWithState>>();
        	
        	String stm = " select a.ID, a.NAME," +
                         " b.STATEABV, a.STATE_ID from "+ TABLE_COUNTY +" a, "+ 
                         TABLE_STATE +" b where a.STATE_ID = b.ID " + 
                         "order by a.NAME";

        	DBConnection conn = null;
    		try {		
    			conn = ConnectionPool.getInstance().requestConnection();
    			DatabaseData data = conn.executeSQL(stm);
    		    			
    			for (int i = 0; i<data.getRowNumber(); i++){
    				
    				int cntyId = Integer.valueOf(data.getValue(1,i).toString());
    				String name = (String)data.getValue(2,i);
    			    String state = (String)data.getValue(3,i);
    			    int stateId = Integer.valueOf(data.getValue(4,i).toString());
    			    
    			    List<CountyWithState> crtStateList = stateCountyList.get(stateId);
    			    if(crtStateList == null){
    			    	crtStateList = new ArrayList<CountyWithState>();
    			    }
    			    crtStateList.add(new CountyWithState(cntyId, name, state, stateId));
    			    stateCountyList.put(stateId, crtStateList);
    			}       

    		} catch (BaseException e) {
    			logger.error(e);
    		} finally {
    			try{                
    				ConnectionPool.getInstance().releaseConnection(conn);
    			}catch(BaseException e){
    				logger.error(e);
    			}           
    		}
    		
    		return stateCountyList;
        }    	
    }
    
    /**
     * 
     * @param stateIdArray
     * @return
     */
    public static List<CountyWithState> getAllCountiesForState(int[] stateIdArray){
        
    	// determine if all states are required
    	boolean allStates = false;
        for(int stateId: stateIdArray){
            if(stateId == -1){
                allStates = true;
                break;
            }
        }
        
        List<CountyWithState> counties = null;
        if(allStates == true){
        	// if all states then return them all
        	counties = AllCountiesForStateHolder.allCounties;        	
        } else {
        	// accumulate counties from all states asked for
        	counties = new ArrayList<CountyWithState>();
        	for(int id: stateIdArray){
        		List<CountyWithState> stateList = AllCountiesForStateHolder.stateCountyList.get(id);
        		if(stateList != null){
        			counties.addAll(stateList);
        		} else {
        			throw new RuntimeException("Invalid county id!!!");
        		}
        	}        	
        }
        return counties;
        
    }
    
    
    public static List<CountyWithState> getAllCountiesForStateAbrev(String stateAbrev){
    	if(stateAbrev.equals(FilterUtils.ALL)){
    		return getAllCountiesForState(-1);
    	}
    	GenericState state = getStateForAbv(stateAbrev);
    	if(state == null) {
    		return new ArrayList<CountyWithState>();
    	}
    	long stateId = state.getId();
    	return getAllCountiesForState((int)stateId);
    }
    
    /**
     * 
     * @param stateId
     * @return
     */
    public static List<CountyWithState> getAllCountiesForState(int stateId){
        return getAllCountiesForState(new int[]{stateId});
    }

    /**
     * holder for lazy initialization
     * @author radu bacrau
     */
    private static class AllStatesForSelectHolder{
    	static final State[] states = loadAllStatesForSelect();
    }
    
    public static State[] getAllStatesForSelect(){
    	return AllStatesForSelectHolder.states;
    }
    
    private static State[] loadAllStatesForSelect(){
                
        DBConnection conn = null;
        State allStates[] = new State [0];
        
        String stm = " select a.ID, a.STATEABV from "+ TABLE_STATE +" a order by a.NAME";
        BigDecimal temp1;
        String temp2;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData data = conn.executeSQL(stm);
            
            allStates = new State[data.getRowNumber()];
            if (logger.isDebugEnabled())
                logger.debug("DBManager getAllStatesForSelect# length: " + data.getRowNumber());
            for (int i = 0; i<data.getRowNumber(); i++){
                allStates[i] = new State();
                temp1 = new BigDecimal(data.getValue(1,i).toString());
                temp2 = (String)data.getValue(2,i);
                allStates[i].setStateId(temp1);
                allStates[i].setStateAbv(temp2);
            }               
            
        } catch (BaseException e) {
            logger.error(e);
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
                
        return allStates;
    }

    //get the name filter test file
    public static byte[] getTestFileContentsFromDb(BigDecimal n ){
    	
    	DBConnection conn = null;
    	
    	byte[] fileContents = null;
    	
    	try {
    		PreparedStatement pstmt;    
            conn = ConnectionPool.getInstance().requestConnection();
            
           	String sqlPhrase = "SELECT " + FIELD_FILTER_TESTFILES_CONTENT + " FROM " +
								TABLE_FILTER_TEST_FILES + " WHERE " + FIELD_FILTER_TESTFILES_ID + " = '" + n.toString() + "'";
            	
           	pstmt = conn.prepareStatement(sqlPhrase);
   			ResultSet rs = pstmt.executeQuery();
   			if(rs.next()){
   				fileContents = rs.getBytes(1);
   			}            
    			
    			pstmt.close();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
    	
    	return fileContents;
    }
    
    
	public static void saveFile(InputStream fc, String fn) throws BaseException {
		SimpleJdbcTemplate conn = null;
		int slength = 0;
		try {
			slength = fc.available();
		} catch (IOException e){
			throw new BaseException(e.getMessage());
		}
		byte[] b = new byte[slength];
		String sql = "insert into " 
						+ TABLE_FILTER_TEST_FILES 
						+ "(" 
						+ FIELD_FILTER_TESTFILES_NAME 
						+ ", " 
						+ FIELD_FILTER_TESTFILES_CONTENT 
						+") values (?, ?)";
		PreparedStatementCreatorFactory pstat = new PreparedStatementCreatorFactory(sql);
		pstat.addParameter(new SqlParameter(DBConstants.FIELD_FILTER_TESTFILES_NAME, Types.VARCHAR));
		pstat.addParameter(new SqlParameter(DBConstants.FIELD_FILTER_TESTFILES_CONTENT, Types.BLOB));
		try {
			fc.read(b);
		} catch (IOException e){
			throw new BaseException(e.getMessage());
		}
		Object[] params = new Object[]{ fn, b};
		try {
			conn = ConnectionPool.getInstance().getSimpleTemplate();
			conn.getJdbcOperations().update(pstat.newPreparedStatementCreator(params));
		} catch (Exception e) {
			e.printStackTrace();
			throw new BaseException("Error saving the file into database!");
		} 
	}

    //I should get a list of file for select
    public static GenericNameFilterTestFiles[] loadAllFilesForSelect(){
        
        DBConnection conn = null;
        GenericNameFilterTestFiles allFiles[] = new GenericNameFilterTestFiles [0];
        
        String stm = " select " + FIELD_FILTER_TESTFILES_ID + ", " + FIELD_FILTER_TESTFILES_NAME + " from "+ TABLE_FILTER_TEST_FILES +" order by " + FIELD_FILTER_TESTFILES_NAME;
        BigDecimal temp1;
        String temp2;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData data = conn.executeSQL(stm);
            
            allFiles = new GenericNameFilterTestFiles[data.getRowNumber()];
            if (logger.isDebugEnabled())
                logger.debug("DBManager getAllTestFilesForSelect# length: " + data.getRowNumber());
            for (int i = 0; i<data.getRowNumber(); i++){
                allFiles[i] = new GenericNameFilterTestFiles();
                temp1 = new BigDecimal(data.getValue(1,i).toString());
                temp2 = (String)data.getValue(2,i);
                allFiles[i].setFileId(temp1);
                allFiles[i].setName(temp2);
            }               
            
        } catch (BaseException e) {
            logger.error(e);
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
                
        return allFiles;
    }
   
    
    public static UserAttributes[] getAllAbstractorsForSelect(String commIds){

        UserAttributes allAbstractors[] = new UserAttributes [0];
        
        if (Util.isParameterValid(commIds)) {
        	String commIdsUnique = commIds;
            if (commIdsUnique.contains(","))
            	commIdsUnique = "-2";
            String stm = " select USER_ID, FIRST_NAME, LAST_NAME, LOGIN from "+ TABLE_USER +" where (COMM_ID IN ($COMMIDS$) or " + commIdsUnique + " = -1) and GID != 64 and " + UserAttributes.USER_HIDDEN + "=0 order by FIRST_NAME, LAST_NAME";
            
            try {
                List<UserAttributes> result = null;
                result = DBManager.getSimpleTemplate().query(stm.replace("$COMMIDS$", commIds), new UserAttributes());
                allAbstractors = new UserAttributes [result.size()];
                allAbstractors = result.toArray(allAbstractors);
            } catch (Exception e) {
            	logger.error(e);
            }
        }
        
        return allAbstractors;
    }
    
    public static UserAttributes[] getAllAbstractorsForSelect(String commIds, int groupId){
        
        DBConnection conn = null;
        UserAttributes allAbstractors[] = new UserAttributes [0];
        
        if (Util.isParameterValid(commIds)) {
        	String commIdsUnique = commIds;
            if (commIdsUnique.contains(","))
            	commIdsUnique = "-2";
            String stm = " select " + DBConstants.FIELD_USER_ID +
             			", " + DBConstants.FIELD_USER_FIRST_NAME + 
             			", " + DBConstants.FIELD_USER_LAST_NAME + 
             			", " + DBConstants.FIELD_USER_LOGIN +
             			" from "+ TABLE_USER + " u " +
            			" LEFT JOIN " + TABLE_COMMUNITY + " d ON d." + DBConstants.FIELD_COMMUNITY_COMM_ID + " = u." + DBConstants.FIELD_USER_COMM_ID +
            			" where (u." + DBConstants.FIELD_USER_COMM_ID + " IN ($COMMIDS$) or "  + commIdsUnique + " = -1) " + 
            			" and (d." +DBConstants.FIELD_COMMUNITY_CATEG_ID  + " = " + groupId + " or " + groupId + " = -1) " +
             			"and GID != 64 and " + UserAttributes.USER_HIDDEN + "=0 " +
            			" order by " + DBConstants.FIELD_USER_FIRST_NAME + ", " +DBConstants.FIELD_USER_LAST_NAME;
            
            try {
                
                conn = ConnectionPool.getInstance().requestConnection();
                DatabaseData data = conn.executeSQL(stm.replace("$COMMIDS$", commIds));
                
                allAbstractors = new UserAttributes [data.getRowNumber()];
                for (int i = 0; i<data.getRowNumber(); i++){
                    allAbstractors[i] = new UserAttributes();
                    allAbstractors[i].setID(new BigDecimal(data.getValue(1,i).toString()));
                    allAbstractors[i].setFIRSTNAME((String)data.getValue(2,i));
                    allAbstractors[i].setLASTNAME((String)data.getValue(3,i));
                    allAbstractors[i].setLOGIN((String)data.getValue(4,i));
                }               
                
            } catch (BaseException e) {
                logger.error(e);
            } finally{
                try{
                    ConnectionPool.getInstance().releaseConnection(conn);
                }catch(BaseException e){
                    logger.error(e);
                }           
            }
        }
        
        return allAbstractors;
    }    

    public static AgentAttributes[] getAllAgentsForSelect(String commIds, String compName){

        AgentAttributes allAgents[] = new AgentAttributes [0]; 
        String filterCompany = "";

        if (Util.isParameterValid(commIds)) {
        	if(StringUtils.isEmpty(compName)){
            	filterCompany = "";
            } else {
            	filterCompany = " AND COMPANY = ?  ";
            }
            
            String commIdsUnique = commIds;
            if (commIdsUnique.contains(","))
            	commIdsUnique = "-2";
            String stm = " select USER_ID, FIRST_NAME, LAST_NAME, COMPANY  from "
            	+ TABLE_USER +" where GID = 64 and " + UserAttributes.USER_HIDDEN + "=0 and (COMM_ID IN ($COMMIDS$) " 
                + " or " 
                + commIdsUnique 
                + " = -1) " 
                + filterCompany
                + " order by COMPANY,FIRST_NAME, LAST_NAME ";

            try {
            
            List<AgentAttributes> result = null;
                    
            if(StringUtils.isEmpty(compName))
            	result = DBManager.getSimpleTemplate().query(stm.replace("$COMMIDS$", commIds), new AgentAttributes());
            else 
            	result = DBManager.getSimpleTemplate().query(stm.replace("$COMMIDS$", commIds), new AgentAttributes(),compName);
            
            allAgents = new AgentAttributes [result.size()];
            allAgents = result.toArray(allAgents);
            } catch (Exception e) {
                logger.error(e);
            }
        }
        
        return allAgents; 
    }
    
    public static List<AgentAttributes> getAllCompaniesAndAgentsForSelect(String commIds, int groupId){
    	List<AgentAttributes> result = null;
    	
    	if (Util.isParameterValid(commIds)) {
    		String commIdsUnique = commIds;
            if (commIdsUnique.contains(","))
            	commIdsUnique = "-2";
            String stm = " select USER_ID, FIRST_NAME, LAST_NAME, " + DBConstants.FIELD_USER_COMPANY + 
            	" from "+ TABLE_USER + " u " +
            	" LEFT JOIN " + TABLE_COMMUNITY + " d ON d." + DBConstants.FIELD_COMMUNITY_COMM_ID + " = u." + DBConstants.FIELD_USER_COMM_ID +
            	" where " + DBConstants.FIELD_USER_COMPANY + " IS NOT NULL AND " + DBConstants.FIELD_USER_GID + " = 64 and " + UserAttributes.USER_HIDDEN + "=0 and " + 
            	"(u." + DBConstants.FIELD_USER_COMM_ID + " IN ($COMMIDS$) or " + commIdsUnique + " = -1) " +
            	" AND (d." +DBConstants.FIELD_COMMUNITY_CATEG_ID  + " = ? or ? = -1) " +
            	" order by " + DBConstants.FIELD_USER_COMPANY;
            
            try {
            	result = getSimpleTemplate().query(stm.replace("$COMMIDS$", commIds), new AgentAttributes(), groupId, groupId);    
            } catch (Exception e) {
                logger.error("Error while loading getAllCompaniesAndAgentsForSelect", e);
            }
    	}
    	
    	return result;
    }
    /**
     * Finds all companies in a selected community.
     * @param commId
     * @return
     */
    public static String[] getAllCompaniesForSelect(String commIds){
    	String[] companies = new String[0];
    	DBConnection conn = null;
        
    	if (Util.isParameterValid(commIds)) {
    		String commIdsUnique = commIds;
            if (commIdsUnique.contains(","))
            	commIdsUnique = "-2";
        	String stm = " select " + DBConstants.FIELD_USER_COMPANY + " COMP  from "+ TABLE_USER +" where GID = 64 and " + UserAttributes.USER_HIDDEN + "=0 and (COMM_ID IN ($COMMIDS$) " 
                + " or " 
                + commIdsUnique 
                + " = -1) group by COMP order by COMP ";
            
            try {
                
                conn = ConnectionPool.getInstance().requestConnection();
                DatabaseData data = conn.executeSQL(stm.replace("$COMMIDS$", commIds));
                companies = new String[data.getRowNumber()];
                for (int i = 0; i<data.getRowNumber(); i++){
                    companies[i] = (String)data.getValue("COMP", i);
                }               
                
            } catch (BaseException e) {
                logger.error(e);
            } finally{
                try{
                    ConnectionPool.getInstance().releaseConnection(conn);
                }catch(BaseException e){
                    logger.error(e);
                }           
            }
    	}
    	
    	return companies;
    }
    
    public static String[] getAllCompaniesForSelect(String commIds, int groupId){
    	String[] companies = new String[0];
    	DBConnection conn = null;
        
    	if (Util.isParameterValid(commIds)) {
    		String commIdsUnique = commIds;
            if (commIdsUnique.contains(","))
            	commIdsUnique = "-2";
        	String stm = " select " + DBConstants.FIELD_USER_COMPANY + " COMP " + 
            	" from "+ TABLE_USER + " u " +
            	" LEFT JOIN " + TABLE_COMMUNITY + " d ON d." + DBConstants.FIELD_COMMUNITY_COMM_ID + " = u." + DBConstants.FIELD_USER_COMM_ID +
            	" where " + DBConstants.FIELD_USER_COMPANY + " IS NOT NULL AND " + DBConstants.FIELD_USER_GID + " = 64 and " + UserAttributes.USER_HIDDEN + "=0 and " + 
            	"(u." + DBConstants.FIELD_USER_COMM_ID + " IN ($COMMIDS$) or " + commIdsUnique + " = -1) " +
            	" AND (d." +DBConstants.FIELD_COMMUNITY_CATEG_ID  + " = " + groupId + " or " + groupId + " = -1) " +
            	" group by COMP order by COMP ";
            
            try {
                
                conn = ConnectionPool.getInstance().requestConnection();
                DatabaseData data = conn.executeSQL(stm.replace("$COMMIDS$", commIds));
                companies = new String[data.getRowNumber()];
                for (int i = 0; i<data.getRowNumber(); i++){
                    companies[i] = (String)data.getValue("COMP", i);
                }               
                
            } catch (BaseException e) {
                logger.error(e);
            } finally{
                try{
                    ConnectionPool.getInstance().releaseConnection(conn);
                }catch(BaseException e){
                    logger.error(e);
                }           
            }
    	}
    	
    	return companies;
    }
    
    /**
     * Finds all users from a category
     * @param categId
     * @return
     */
    public static UserAttributes[] getAllUsersFromCategory(int categId){
    	DBConnection conn = null;
        UserAttributes allUsers[] = new UserAttributes [0];
        
        String stm = " select u.USER_ID, u.FIRST_NAME, u.LAST_NAME, u.LOGIN from "+ 
        	TABLE_USER +" u JOIN " + TABLE_COMMUNITY + " c ON u.COMM_ID = c. COMM_ID " + 
        	"where c.categ_id = " + categId + 
        	" order by FIRST_NAME, LAST_NAME";
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData data = conn.executeSQL(stm);
            
            allUsers = new UserAttributes [data.getRowNumber()];
            for (int i = 0; i<data.getRowNumber(); i++){
            	allUsers[i] = new UserAttributes();
            	allUsers[i].setID(new BigDecimal(data.getValue(1,i).toString()));
            	allUsers[i].setFIRSTNAME((String)data.getValue(2,i));
            	allUsers[i].setLASTNAME((String)data.getValue(3,i));
            	allUsers[i].setLOGIN((String)data.getValue(4,i));
            }               
            
        } catch (BaseException e) {
            logger.error(e);
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
        
        
        return allUsers;
    }
    
    public static long insertProperty(PossessionEntity pe){
        
        DBConnection conn = null;
        
        long propertyId = -1;
        
        
        String lotNo=pe.getLegalLotNo();
        if(lotNo!=null){
            if(lotNo.length()>50){
                lotNo=lotNo.substring(0,50);
            }
        }
        else {
            lotNo="";
        }
        
        String stm = " insert into "+ TABLE_PROPERTY +" ( ADDRESS_NO, ADDRESS_DIRECTION, ADDRESS_NAME, ADDRESS_SUFFIX, ADDRESS_UNIT, " +
                    "CITY, COUNTY_ID, STATE_ID, ZIP, INSTRUMENT, PARCEL_ID, PLATBOOK, PAGE, SUBDIVISION, LOTNO, isBootstrapped )" +
                    "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) ";

        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            

    		PreparedStatement pstmt = conn.prepareStatement( stm );
    		int k = 1; 
    		
    		pstmt.setString( k++, pe.getAddressNo());
    		pstmt.setString( k++, pe.getAddressDirection());
    		pstmt.setString( k++, pe.getAddressName());
    		pstmt.setString( k++, pe.getAddressSuffix());
    		pstmt.setString( k++, pe.getAddressUnit());
    		pstmt.setString( k++, pe.getAddressCity());
    		pstmt.setLong( k++, pe.getCountyId());
    		pstmt.setLong( k++, pe.getStateId());
    		pstmt.setString( k++, pe.getAddressZip());
    		pstmt.setString( k++, pe.getLegalInstrument());
    		pstmt.setString( k++, pe.getLegalParcelId());
    		pstmt.setString( k++, pe.getLegalPlatBook());
    		pstmt.setString( k++, pe.getLegalPage());
    		pstmt.setString( k++, pe.getLegalSubdivision());
    		pstmt.setString( k++, lotNo);
    		pstmt.setInt( k++, pe.getIsBootstrapped());
    		pstmt.executeUpdate();
    		
            propertyId = getLastId(conn);
            pstmt.close();
        } catch (Exception e) {
            logger.error(e);
            Log.sendExceptionViaEmail(e);
            logger.error("DBManager insertProperty# SQL: " + stm);
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }   
        
        return propertyId;
    }
    
    public static long updateProperty(PossessionEntity pe){
        
        long propertyId = pe.getId();
        
        String lotNo=pe.getLegalLotNo();
        if(lotNo!=null){
            if(lotNo.length()>50){
                lotNo=lotNo.substring(0,50);
            }
        }
        else {
            lotNo="";
        }
        
        String stm = " update "+ TABLE_PROPERTY +" SET " +
        		"ADDRESS_NO = ? " +
        		",ADDRESS_DIRECTION = ? " + 
        		",ADDRESS_NAME = ? " + 
        		",ADDRESS_SUFFIX = ? " +  
        		",ADDRESS_UNIT = ? " + 
        		",CITY = ? " + 
        		",COUNTY_ID = ? " +
        		",STATE_ID = ? " +
        		",ZIP = ? " + 
        		",INSTRUMENT = ? " + 
        		",PARCEL_ID = ? " + 
        		",PLATBOOK = ? " + 
        		",PAGE = ? " + 
        		",SUBDIVISION = ? " + 
        		",LOTNO = ? " +
        		",isBootstrapped = ? " +
        		" WHERE ID = ? ";
        try {
			DBManager.getSimpleTemplate().update(stm,pe.getAddressNo(),pe.getAddressDirection(),pe.getAddressName(),pe.getAddressSuffix(),pe.getAddressUnit(),
					pe.getAddressCity(),pe.getCountyId(),pe.getStateId(),pe.getAddressZip(),pe.getLegalInstrument(),pe.getLegalParcelId(),pe.getLegalPlatBook(),
					pe.getLegalPage(),pe.getLegalSubdivision(),lotNo, pe.getIsBootstrapped(), propertyId );
		} catch (Exception e) {
	          Log.sendExceptionViaEmail(e, "DBManager insertProperty# SQL: " + stm + " Parameters: "+ pe.getAddressNo()+ " " +pe.getAddressDirection()+ " " +pe.getAddressName()+ " " +pe.getAddressSuffix()+ " " +pe.getAddressUnit()+ " " +
						pe.getAddressCity()+ " " +pe.getCountyId()+ " " +pe.getStateId()+ " " +pe.getAddressZip()+ " " +pe.getLegalInstrument()+ " " +pe.getLegalParcelId()+ " " +pe.getLegalPlatBook()+ " " +
						pe.getLegalPage()+ " " +pe.getLegalSubdivision()+ " " +lotNo+ " " +propertyId);
	          logger.error("DBManager insertProperty# SQL: " + stm + " Parameters: "+ pe.getAddressNo()+ " " +pe.getAddressDirection()+ " " +pe.getAddressName()+ " " +pe.getAddressSuffix()+ " " +pe.getAddressUnit()+ " " +
						pe.getAddressCity()+ " " +pe.getCountyId()+ " " +pe.getStateId()+ " " +pe.getAddressZip()+ " " +pe.getLegalInstrument()+ " " +pe.getLegalParcelId()+ " " +pe.getLegalPlatBook()+ " " +
						pe.getLegalPage()+ " " +pe.getLegalSubdivision()+ " " +lotNo+ " " +propertyId, e);
		}
		
        return propertyId;
    }
    
    public static boolean haveSearchContext(long searchId){
                
        String stm = "select count(*) from "+ 
        	DBConstants.TABLE_SEARCH_FLAGS + " where " + 
        	DBConstants.FIELD_SEARCH_FLAGS_ID + " = " + searchId + " and " + DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + "=0";
        
		try {
			return DBManager.getSimpleTemplate().queryForLong(stm) > 0;
		} catch (Exception e) {
			 logger.error(e);
		}
        
		return false;
    }
    
    public static void setSearchOwnerLockedUnlocked(long searchID, long userId, boolean locked) {
    	if (locked == true)
    	{
    		setSearchOwner(searchID, userId, -1);
    	}
    	else
    	{
            setSearchOwnerLockedUnlocked(searchID, userId, -1, false);
    	}
    }

    
    public static void setSearchOwner(long searchID, long userId) {
        FileLogger.info( "setSearchOwner(" + searchID + ", " + userId + ") start", FileLogger.SEARCH_OWNER_LOG );
        setSearchOwner(searchID, userId, -1);
        FileLogger.info( "setSearchOwner(" + searchID + ", " + userId + ") end", FileLogger.SEARCH_OWNER_LOG );
    }
    
    public static void setSearchOwner(Search search, long userId) {
    	FileLogger.info( "setSearchOwner( searchId = " + search.getID() + ", userId = " + userId 
    			+ ", abstractorId = -1) start", FileLogger.SEARCH_OWNER_LOG );
        
		SetSearchOwnerTransaction transaction = new SetSearchOwnerTransaction(search, userId, -1, true);
    	getTransactionTemplate().execute(transaction);
        
    	FileLogger.info( "setSearchOwner(" + search.getID() + ", " + userId + ", -1) end", FileLogger.SEARCH_OWNER_LOG );
        
    }
    
    public static int getSearchStatus(long id){
        
        String stm = " select status from "+ TABLE_SEARCH +" where ID=" + id;
        
		try {
			return DBManager.getSimpleTemplate().queryForInt(stm);
		} catch (Exception e) {
			 logger.error(e);
		}
		
        return 0;
    }
    
    public static boolean overrideAbstractor( String list, UserAttributes ua, String[] abstractorIdArray, boolean overrideAgent) {
        DBConnection conn = null;
        DatabaseData data;
        
        if( abstractorIdArray == null ){
        	return false;
        }
        
        if( abstractorIdArray.length == 0 ){
        	return false;
        }
        String newList = "";
        try{
	        if (UserUtils.isTSAdmin(ua) || UserUtils.isCommAdmin(ua)||UserUtils.isTSCAdmin(ua)) {
	        	String sql = " SELECT " + 
	        		DBConstants.FIELD_SEARCH_FLAGS_ID + " from " + 
	        		DBConstants.TABLE_SEARCH_FLAGS + " WHERE " +
	        		DBConstants.FIELD_SEARCH_FLAGS_ID + " in ( ";
	        	
	        	String[] listElements = list.split(",");
	        	
	        	for(int i=1;i<=listElements.length;i++) {
	        		if(i!=listElements.length) sql += " ?, ";
	        		else sql += " ? ";
	        	}
	        	 
	            sql += " ) and tsr_created = 0 ";
	        	
	        	conn = ConnectionPool.getInstance().requestConnection();
                
	    		PreparedStatement pstmt = conn.prepareStatement( sql );
	    		for(int i=1;i<=listElements.length;i++)
	    			pstmt.setString( i, listElements[i-1]);
	    		
	    		data = conn.executePrepQuery(pstmt);	
                
                for (int i = 0; i < data.getRowNumber(); i++) {
					newList += data.getValue(1, i).toString();
					newList += ",";
				}
                if(newList.length()>0)
                	newList = newList.substring(0,newList.length()-1);
               
	    		pstmt.close();	
	        }
	    } catch (Exception e) {
	        logger.error(e);
	        e.printStackTrace();
	        return false;
	    }
	    finally{
	        try{
	            ConnectionPool.getInstance().releaseConnection(conn);
	        }catch(BaseException e){
	            logger.error(e);
	        }           
	    }
	    if(newList.length()==0)
     	   return false;
	    
	    Object result = Boolean.FALSE;
	    try {
	    	result = getTransactionTemplate().execute(new OverrideAbstractorTransaction(abstractorIdArray[0],newList,overrideAgent));
	    	
		    if(overrideAgent) {
    	    	//also update the agent field in the search
			    String[] searchIds = list.split(",");
			    for (int i = 0; i < searchIds.length; i++) {
					long currentSearchId = Long.parseLong(searchIds[i]);
					Search search = SearchManager.getSearchFromDisk(currentSearchId);
					search.setAgent(UserUtils.getUserFromId(Long.parseLong(abstractorIdArray[0])));
					Search.saveSearch(search);
			    }
		    }
		    
	    }
	    catch (Exception e) {
			e.printStackTrace();
		}
        return ((Boolean)result).booleanValue();
	    
    }

    public static void setSearchOwnerLockedUnlocked(long searchID, long userId, long abstractorId, boolean locked) {
        
    	FileLogger.info( "setSearchOwner( searchId = " + searchID + ", userId = " + userId + ", abstractorId = " + abstractorId + ") start", FileLogger.SEARCH_OWNER_LOG );
    	
    	SetSearchOwnerTransaction transaction = new SetSearchOwnerTransaction(searchID,userId,abstractorId,locked);
    	getTransactionTemplate().execute(transaction);
    	
        FileLogger.info( "setSearchOwner(" + searchID + ", " + userId + ", " + abstractorId + ") end", FileLogger.SEARCH_OWNER_LOG );
    }
    
    public static void setSearchOwner(long searchID, long userId, long abstractorId) {
        FileLogger.info( "setSearchOwner( searchId = " + searchID + ", userId = " + userId + ", abstractorId = " + abstractorId + ") start", FileLogger.SEARCH_OWNER_LOG );
        
        SetSearchOwnerTransaction transaction = new SetSearchOwnerTransaction(searchID,userId,abstractorId,true);
    	getTransactionTemplate().execute(transaction);
        
    	FileLogger.info( "setSearchOwner(" + searchID + ", " + userId + ", " + abstractorId + ") end", FileLogger.SEARCH_OWNER_LOG );
    }
    
	public static int saveCurrentSearchLockedUnlocked(User currentUser, Search global,
			int tsrCreated, HttpServletRequest request, boolean locked)
			throws SaveSearchException {
		if (locked) {
			return saveCurrentSearch(currentUser, global, tsrCreated, request, null, true);
		} else {
			return saveCurrentSearchLockedUnlocked(currentUser, global, tsrCreated, request, null, locked);
		}
	}
    
    public static int saveCurrentSearch(User currentUser, Search global, 
            int tsrCreated, HttpServletRequest request) 
                    throws SaveSearchException{
        return saveCurrentSearch(currentUser, global, tsrCreated, request, null,true);
    }
    
    public static int saveCurrentSearchLockedUnlocked(User currentUser, Search global, 
            int tsrCreated, HttpServletRequest request, Search oldSearch, boolean locked) 
                    throws SaveSearchException{
    	if (locked==true)
    	{
            return saveCurrentSearch(currentUser, global, tsrCreated, request, oldSearch,true);
    	}
    	else
    	{
            String contextPath = URLMaping.path;// request.getContextPath();
            
            String pdfFileName = null;
            try {
                pdfFileName = request.getParameter("PDFFileNameShort");
            } catch (Exception ignored) {}
            
            String tsrSentTo = null;
            try {
                tsrSentTo = request.getParameter("emailSentTo");
            } catch (Exception ignored) {}
            
            return saveCurrentSearchLockedUnlocked(currentUser, global, tsrCreated, pdfFileName, contextPath, tsrSentTo, oldSearch, false,true);
    	}
    	
    }
    
    public static int saveCurrentSearch(User currentUser, Search global, 
            int tsrCreated, HttpServletRequest request, Search oldSearch, boolean createTSRHtmlIndex) 
                    throws SaveSearchException{
        
        String contextPath = URLMaping.path;// request.getContextPath();
        
        String pdfFileName = null;
        try {
            pdfFileName = request.getParameter("PDFFileNameShort");
        } catch (Exception ignored) {}
        
        String tsrSentTo = null;
        try {
            tsrSentTo = request.getParameter("emailSentTo");
        } catch (Exception ignored) {}
        
        return saveCurrentSearch(currentUser, global, tsrCreated, pdfFileName, contextPath, tsrSentTo, oldSearch, createTSRHtmlIndex);
    }
    
    public static int saveCurrentSearch(User currentUser, Search global, 
            int tsrCreated, String pdfFileName, String contextPath, String tsrSentTo) 
                        throws SaveSearchException{
        return saveCurrentSearch(currentUser, global, 
                tsrCreated, pdfFileName, contextPath, tsrSentTo, null,true);
    }
    
    
    public static int saveCurrentSearch(User currentUser, Search global, 
            int tsrCreated, String pdfFileName, String contextPath, String tsrSentTo, Search oldSearch, boolean createTSRHtmlIndex)
    		throws SaveSearchException
    {
    	return saveCurrentSearchLockedUnlocked(currentUser, global, 
                tsrCreated, pdfFileName, contextPath, tsrSentTo, oldSearch, true,createTSRHtmlIndex);
    }

    
    public static int saveCurrentSearchLockedUnlocked(User currentUser, Search global, 
            int tsrCreated, String pdfFileName, String contextPath, String tsrSentTo, Search oldSearch, Boolean locked, boolean createTSRHtmlIndex) 
                        throws SaveSearchException{
        
        SearchAttributes sa = global.getSa();
        UserAttributes currentUserAttributes = currentUser.getUserAttributes();
        //this value should be set ONLY for created TSRs. for those, this field has no importance.

        int currentStatus = global.getTS_SEARCH_STATUS();
        
        if ("0".equals(global.getSa().getAtribute(SearchAttributes.P_COUNTY))
                || "0".equals(global.getSa().getAtribute(SearchAttributes.P_STATE)))
            currentStatus = Search.SEARCH_STATUS_N;
        
        
        //setting the possesion entity fields from search attributes
        PossessionEntity propertyData = new PossessionEntity();
        propertyData.setAddressNo(sa.getAtribute(SearchAttributes.P_STREETNO));
        propertyData.setAddressDirection(sa.getAtribute(SearchAttributes.P_STREETDIRECTION));
        propertyData.setAddressName(sa.getAtribute(SearchAttributes.P_STREETNAME));
        propertyData.setAddressSuffix(sa.getAtribute(SearchAttributes.P_STREETSUFIX));
        propertyData.setAddressUnit(sa.getAtribute(SearchAttributes.P_STREETUNIT));
        propertyData.setAddressCity(sa.getAtribute(SearchAttributes.P_CITY));
        propertyData.setAddressZip(sa.getAtribute(SearchAttributes.P_ZIP));

        String P_STATE_ATTRIBUTE  = sa.getAtribute(SearchAttributes.P_STATE);
        if ( !"".equals(P_STATE_ATTRIBUTE) && P_STATE_ATTRIBUTE != null )
            propertyData.setStateId(Long.parseLong(P_STATE_ATTRIBUTE));

        String P_COUNTY_ATTRIBUTE = sa.getAtribute(SearchAttributes.P_COUNTY);
        if ( !"".equals(P_COUNTY_ATTRIBUTE) && P_COUNTY_ATTRIBUTE != null )
            propertyData.setCountyId(Long.parseLong(P_COUNTY_ATTRIBUTE));

        propertyData.setLegalInstrument(sa.getAtribute(SearchAttributes.LD_INSTRNO));
        propertyData.setLegalParcelId(sa.getAtribute(SearchAttributes.LD_PARCELNO));
        propertyData.setLegalPlatBook(sa.getAtribute(SearchAttributes.LD_BOOKNO));
        propertyData.setLegalPage(sa.getAtribute(SearchAttributes.LD_PAGENO));
        propertyData.setLegalSubdivision(sa.getAtribute(SearchAttributes.LD_SUBDIVISION));
        propertyData.setLegalLotNo(sa.getAtribute(SearchAttributes.LD_LOTNO));
        propertyData.setIsBootstrapped(sa.addressBootstrappedCode());
        
        //inserting the property in db
        BigDecimal propertyId = global.getPropertyId();
        try {
        	if ( propertyId != null ) {
        		propertyData.setId( propertyId.longValue() );
        		updateProperty(propertyData);
        	} else {
        		propertyId = new BigDecimal( insertProperty(propertyData) );
        		global.setPropertyId(propertyId);
        	}
        } catch (Exception e2) {
            logger.error(e2);
        }
        
        //setting the fields for the search record
        long agentId = -1;
        Long agentCommunityId = null; 
        try {
        	if ( global.getAgent() != null ) {
        		agentId = global.getAgent().getID().longValue();
        		agentCommunityId = global.getAgent().getCOMMID().longValue();
        	}
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        long abstractorId = currentUserAttributes.getID().longValue();
        int abstractorGroupId = currentUserAttributes.getGROUP().intValue();
        String agentFileNo = DataAttribute.transformBlank(sa.getAtribute(SearchAttributes.ORDERBY_FILENO));
        String abstractorFileNo = "";
        
        //forming the TSR file link
        String userFolder = ServerConfig.getUserFolder();
        
        String tsrLink = "";
        if (pdfFileName != null) {
              
            abstractorFileNo = pdfFileName;
            
            if(global.getAgent() == null) {
                abstractorFileNo = abstractorFileNo.replaceAll("\\.pdf", ".tiff");
                pdfFileName = pdfFileName.replaceAll("\\.pdf", ".tiff");
            }
            
             tsrLink = contextPath 
                                    + "/fs?f=TSD" + File.separator + userFolder 
                                    + currentUserAttributes.getLOGIN() 
                                    + File.separator + pdfFileName +"&SSFLINK="+global.getTsrLink() ;   
        } else {
            if (sa.getAbstractorFileName() == null || "".equals(sa.getAbstractorFileName())) {
                sa.setAbstractorFileName(global);
            }
            tsrLink = contextPath 
                                + "/fs?f=TSD" + File.separator + userFolder 
                                + currentUserAttributes.getLOGIN() 
                                + File.separator + sa.getAbstractorFileName();
            abstractorFileNo = sa.getAbstractorFileName();          
        }
                
        //adding the TSR sent to field
        if (tsrSentTo == null) tsrSentTo = "N/A";
                            
        //long timestamp = System.currentTimeMillis();
        
        //String mysqlDate = new FormatDate(FormatDate.TIMESTAMP).getDate(timestamp);
        
        int searchType = Integer.parseInt(sa.getAtribute(SearchAttributes.SEARCH_PRODUCT));
        
        //testing if user sets a particular search value for this search
        boolean newPayrateSet = false;
        double newPayrate = -1;
        try {
            String payrateStr = sa.getAtribute(SearchAttributes.PAYRATE_NEW_VALUE);
            payrateStr = payrateStr.replaceAll("\\$", "");
            payrateStr = payrateStr.trim();
            newPayrate = Double.parseDouble(payrateStr);
            newPayrateSet = true;
        } catch (Exception e) {
            newPayrateSet = false;
        }
        
        //finding current payrate
        Calendar cal = Calendar.getInstance();
        long countyAttribute = -1;
        try
        {
            countyAttribute = Long.parseLong(P_COUNTY_ATTRIBUTE);
        }catch(Exception e) {}
        
        Payrate currentPayrate = getCurrentPayrateForCounty(
            InstanceManager.getManager().getCurrentInstance(global.getID()).getCurrentCommunity().getID().longValue(), 
            countyAttribute, cal.getTime(), currentUserAttributes);
        if (newPayrateSet) {
            double rate = 0.0;

            //UserRates ur = UserManager.getUserRates(currentUserAttributes.getID().longValue(), new Date(System.currentTimeMillis()));
            
            //must use agent's community to agent rate
            UserRates ur = UserManager.getUserRates(global.getAgent().getID().longValue(), countyAttribute, new Date(System.currentTimeMillis()));
            if (ur != null) {
                rate = ur.getC2ARATEINDEX();
            } else {
                rate = 1;
            }
            //this is an arbitrary date, that should be less then the actual date for this community
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.MONTH, 1);
            calendar.set(Calendar.YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 1);
            calendar.set(Calendar.MINUTE, 1);
            calendar.set(Calendar.SECOND, 1);
            currentPayrate.setStartDate(calendar.getTime());
            calendar.set(Calendar.MONTH, 2);
            currentPayrate.setEndDate(calendar.getTime());
            switch (searchType) {
			case Products.FULL_SEARCH_PRODUCT:
				if(rate == 0) {
					currentPayrate.setSearchValue(0);
				} else {
					currentPayrate.setSearchValue(newPayrate/rate);
				}
				break;
			case Products.CURRENT_OWNER_PRODUCT:
				if(rate == 0) {
					currentPayrate.setCurrentOwnerValue(0);
				} else {
					currentPayrate.setCurrentOwnerValue(newPayrate/rate);
				}
				break;	
			case Products.CONSTRUCTION_PRODUCT:
				if(rate == 0) {
					currentPayrate.setConstructionValue(0);
				} else {
					currentPayrate.setConstructionValue(newPayrate/rate);
				}
				break;
			case Products.COMMERCIAL_PRODUCT:
				if(rate == 0) {
					currentPayrate.setCommercialValue(0);
				} else {
					currentPayrate.setCommercialValue(newPayrate/rate);
				}
				break;
			case Products.REFINANCE_PRODUCT:
				if(rate == 0) {
					currentPayrate.setRefinanceValue(0);
				} else {
					currentPayrate.setRefinanceValue(newPayrate/rate);
				}
				break;
			case Products.OE_PRODUCT:
				if(rate == 0) {
					currentPayrate.setOEValue(0);
				} else {
					currentPayrate.setOEValue(newPayrate/rate);
				}
				break;	
			case Products.LIENS_PRODUCT:
				if(rate == 0) {
					currentPayrate.setLiensValue(0);
				} else {
					currentPayrate.setLiensValue(newPayrate/rate);
				}
				break;
			case Products.ACREAGE_PRODUCT:
				if(rate == 0) {
					currentPayrate.setAcreageValue(0);
				} else {
					currentPayrate.setAcreageValue(newPayrate/rate);
				}
				break;
			case Products.SUBLOT_PRODUCT:
				if(rate == 0) {
					currentPayrate.setSublotValue(0);
				} else {
					currentPayrate.setSublotValue(newPayrate/rate);
				}
				break;
			case Products.UPDATE_PRODUCT:
				if(rate == 0) {
					currentPayrate.setUpdateValue(0);
				} else {
					currentPayrate.setUpdateValue(newPayrate/rate);
				}
			case Products.FVS_PRODUCT:
				if(rate == 0) {
					currentPayrate.setFvsValue(0);
				} else {
					currentPayrate.setFvsValue(newPayrate/rate);
				}
			default:
				logger.error("Error determining the corrent type of product. Unknown type: " + searchType);
				if(rate == 0) {
					currentPayrate.setSearchValue(0);
				} else {
					currentPayrate.setSearchValue(newPayrate/rate);
				}
				break;
			}
            
            currentPayrate = getCurrentPayrateForCounty(DBManager.insertPayrate(currentPayrate,currentUserAttributes), currentPayrate);
        }
        //finding current user rating id
        long rateId = 0;
        
        //agent rating id
        long agentRateId = -1;
        
        UserRates ur = UserManager.getUserRates(abstractorId, propertyData.getCountyId(), cal.getTime());
        if (ur != null) 
            rateId = ur.getIdCounty( new BigDecimal(propertyData.getCountyId()) ).longValue(); 

        UserRates agentUr = UserManager.getUserRates( agentId, propertyData.getCountyId(), cal.getTime() );
        if( agentUr != null )
        {
            agentRateId = agentUr.getIdCounty( new BigDecimal(propertyData.getCountyId()) ).longValue();
        }
        
        long commId = InstanceManager.getManager().getCurrentInstance(global.getID()).getCurrentCommunity().getID().longValue();
        
        CommunityAttributes commAttr = null;   
        
        try {
			commAttr = CommunityManager.getCommunity(commId);
		} catch (BaseException shouldNotHappen) {shouldNotHappen.printStackTrace();}
		    
        String additionalInformation = sa.getAtribute(SearchAttributes.ADDITIONAL_INFORMATION);
        String addInfoSpc = " "+additionalInformation+" ";
			    
        Date dueDateFromSearch = global.getSearchDueDate();
        String dueDate = null;
        if (dueDateFromSearch == null){
        	dueDateFromSearch = global.getTSROrderDate();
        	if (dueDateFromSearch == null){
        		dueDateFromSearch = new Date(System.currentTimeMillis());
        	}
        	dueDate = computeMysqlDueDate(dueDateFromSearch, addInfoSpc, commAttr.getDEFAULTSLA().intValue(), global.getID()); 
        } else {
        	Calendar cald = Calendar.getInstance();
	    	cald.setTime(dueDateFromSearch);
        	dueDate = "str_to_date( '" + cald.get(Calendar.DAY_OF_MONTH) + "-" + (cald.get(Calendar.MONTH) + 1) + "-" + cald.get(Calendar.YEAR) + " 12:00:00" 
        						+ "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
        }
                   
        if ("".equals(additionalInformation)) 
            additionalInformation = null;
        else
            additionalInformation = Formater.doubleQuotes(additionalInformation);
        	
    	SaveSearchTransaction saveTransaction = new SaveSearchTransaction(global);
    	saveTransaction.setSearchId(global.getID());
    	saveTransaction.setTsrCreated(tsrCreated);
    	saveTransaction.setAgentId(agentId);
    	saveTransaction.setAgentCommunityId(agentCommunityId);
    	saveTransaction.setAbstractorId(abstractorId);
    	saveTransaction.setAbstractorGroupId(abstractorGroupId);
    	saveTransaction.setAgentFileNo(Formater.doubleQuotes(agentFileNo));
    	saveTransaction.setAbstractorFileNo(Formater.doubleQuotes(abstractorFileNo));
    	saveTransaction.setSearchType(searchType);
    	saveTransaction.setPayrateId(currentPayrate.getId());
    	saveTransaction.setTsrLink(Formater.doubleQuotes(tsrLink));
    	saveTransaction.setTsrSentTo(Formater.doubleQuotes(tsrSentTo));
    	saveTransaction.setUserRatingId(rateId);
    	saveTransaction.setAgentRatingId(agentRateId);
    	saveTransaction.setTsrFolder(global.getSearchDir().replace(ServerConfig.getFilePath(),""));
//    	saveTransaction.setTsrFolder(
//    			global.getSearchDir()
//    			.replaceAll("[\\\\]+", Matcher.quoteReplacement(File.separator))
//    			.replace( ServerConfig.getFilePath().replaceAll("[\\\\]+", Matcher.quoteReplacement(File.separator)) , ""));
    	saveTransaction.setCommId(commId);
    	saveTransaction.setNoteClob(null);
    	saveTransaction.setNoteStatus(-1);
    	saveTransaction.setLegalDescription(Formater.doubleQuotes(sa.getAtribute(SearchAttributes.LEGAL_DESCRIPTION)));
    	saveTransaction.setLegalDescriptionStatus(Integer.parseInt(sa.getAtribute(SearchAttributes.LEGAL_DESCRIPTION_STATUS)));
    	saveTransaction.setOldSearch(oldSearch);
    	saveTransaction.setStatus(currentStatus);
    	saveTransaction.setPropertyId(propertyId.longValue());
    	saveTransaction.setDueDate(dueDate);
    	
    	if(locked)
    		saveTransaction.setCheckedBy(1);
    	else
    		saveTransaction.setCheckedBy(0);
    	
    	boolean isSearchReused = (tsrCreated==1?false:isSearchReused(global.getID(),tsrLink));
    	String originalTsrFileName = null;
    	if(isSearchReused) {
    		originalTsrFileName = getOriginalTsrFileName(global.getID());
    	}
    	logger.info(global.getID() + ": Before saving the search - first try");
    	Integer saveResult = (Integer)getTransactionTemplate().execute(saveTransaction);
    	logger.info(global.getID() + ": After saving the search - first try: " + saveResult);
    	 
    	//false is returned only if the save was not completed because of mutual exclusion
    	if(saveResult == SaveSearchTransaction.STATUS_FAIL_MUTUAL_EXCLUSION_VIOLATION) {
    		return saveResult;
    	} else if(saveResult == SaveSearchTransaction.STATUS_FAIL_TIMEOUT) {
    		try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				logger.error("This is weird, I'm just sleeping...", e);
			}
			logger.error(global.getID() + ": Before saving the search - second try - SaveSearchTransaction.STATUS_FAIL_TIMEOUT");
	    	saveResult = (Integer)getTransactionTemplate().execute(saveTransaction);
	    	logger.error(global.getID() + ": After saving the search - second try - SaveSearchTransaction.STATUS_FAIL_TIMEOUT: " + saveResult);
	    	if(saveResult == SaveSearchTransaction.STATUS_FAIL_MUTUAL_EXCLUSION_VIOLATION) {
	    		return saveResult;
	    	}
    	} else if(saveResult == SaveSearchTransaction.STATUS_FAIL_OTHER_EXCEPTION) {
    		logger.error(global.getID() + ": ERROR - some exception occured while executing transaction");
    	}
    	if (saveResult == SaveSearchTransaction.STATUS_SUCCES 
    			&& org.apache.commons.lang.StringUtils.isNotEmpty(additionalInformation)){
	    	
    		List<NoteMapper> additionInfoAlreadySaved = NoteMapper.getSearchNoteByType(global.getID(), TYPE.FROM_ADD_INFO.getValue());
    		if (additionInfoAlreadySaved != null && additionInfoAlreadySaved.size() > 0) {
    			try {
					NoteMapper.updateSearchNoteByType(global.getID(), TYPE.FROM_ADD_INFO.getValue(), Formater.doubleQuotes(additionalInformation));
				} catch (Exception e) {
					logger.error(global.getID() + ": ERROR - some exception occured while trying to update note from Aditional Information");
				}
			} else{
		    	NoteMapper newNote = new NoteMapper();
		    	newNote.setNoteNote(Formater.doubleQuotes(additionalInformation));
		    	newNote.setNoteOperation(TYPE.FROM_ADD_INFO.getValue());
		    	newNote.setNoteSearchId(global.getID());
		    	newNote.setNoteTimestamp(Calendar.getInstance().getTime());
		    	newNote.setNoteUserId(currentUserAttributes.getID().intValue());
		    	try {
					NoteMapper.setSearchNote(newNote);
				} catch (Exception e) {
					logger.error(global.getID() + ": ERROR - some exception occured while saving note from Aditional Information");
				}
    		}
    	}
    	
    	/**
         * when first saving a reopened search we have to:
         * delete the TSR (if created)
         * delete TSR log and status field from the database
         * delete TSD fileName
         * write in log "Reopened Search Saved"
         */
        if(isSearchReused){
        	deleteTSRFiles(originalTsrFileName);
        	deteleTSRLogs(global);
        	unsetTSRDate(global);
        	//global.setTSDFileName("");
        	global.setTsriLink("");
        	global.getSa().setReopenSearch(true);
        	SearchLogger.info("</div><div>", global.getID());
        	SearchLogger.info("<BR><B>Saved Reopened Search</B> " + SearchLogger.getTimeStamp(global.getSearchID()) 
        			+ (currentUser.getBrowserVersion()!=null?" using browser: <span class=\"timestamp\">" + currentUser.getBrowserVersion() + "</span>":"")
        			+ ".<BR>",	global.getID());
        	SearchLogger.info("</div><hr/>", global.getID());
        }
        
        try{
        	
//        	String tsrIndexFileName = global.getSearchDir() + File.separator + "tsrIndexFile.html";  
//        	File tsrIndexFile = new File(tsrIndexFileName);
//        	if(tsrCreated != Search.SEARCH_TSR_CREATED || !tsrIndexFile.exists()) {
//	        	PrintWriter pw = new PrintWriter(tsrIndexFile);
//	        	
//	        	HashMap<String,String> templatesMap = new HashMap<String,String>();
//	        	try {
//		        	List<CommunityTemplatesMapper> templ = UserUtils.getUserTemplates(global.getAgent().getID().longValue(),-1, UserUtils.FILTER_BOILER_PLATES_EXCLUDE, global.getProductId());
//		        	for(CommunityTemplatesMapper cmt : templ) {
//		        		templatesMap.put(cmt.getName(), cmt.getPath());
//		        	}
//	        	}catch(Exception ignored) {}
//	        	if(createTSRHtmlIndex){
//		        	String str = GPThread.createTsrIndexHtmlContents(false,global,contextPath, templatesMap, new ArrayList<String>(), null);
//		
//		        	pw.print("<html><head><title>TSR Index Page</title></head><body>\n");
//		        	pw.print(str);
//		        	pw.print("</body></html>");
//		        	pw.flush();
//		        	pw.close();
//	        	}
//        	}
        	
        	if(tsrCreated != Search.SEARCH_TSR_CREATED) {
				HashMap<String, String> templatesMap = new HashMap<String, String>();
				try {
					List<CommunityTemplatesMapper> templ = UserUtils.getUserTemplates(global.getAgent().getID().longValue(), -1,
							UserUtils.FILTER_BOILER_PLATES_EXCLUDE, global.getProductId());
					for (CommunityTemplatesMapper cmt : templ) {
						templatesMap.put(cmt.getName(), cmt.getPath());
					}
				} catch (Exception ignored) {
				}
	    		StringBuilder sb = new StringBuilder("<html><head><title>TSR Index Page</title></head><body>\n");
	    		sb.append(GPThread.createTsrIndexHtmlContents(false,global,contextPath, templatesMap, new ArrayList<String>(), null))
	    			.append("</body></html>");
	    		try {
					AsynchSearchLogSaverThread.writeTsrILogFile(global, sb.toString());
				} catch (Exception e) {
					logger.error("Cannot write TsrILog for search " + global.getID(), e);
				}
        	}
        }catch(Exception ignored){
        	ignored.printStackTrace();
        }
        
    	//now let's insert the three files
        InsertSearchFilesTransaction insertSearchFilesTransaction = 
        	new InsertSearchFilesTransaction(global);
        getTransactionTemplate().execute(insertSearchFilesTransaction);
        
        return saveResult;
        
    }    

    /**
     * Deletes a search with the given tsrCreated flag
     * @param searchId
     * @param tsrCreated
     * @return number or rows affected or -1 if error
     */
	public static int deleteSearch(long searchId, int tsrCreated) {
		try {
			return getSimpleTemplate().update(
					"delete from " + TABLE_SEARCH + " where " + DBConstants.FIELD_SEARCH_ID + " in " + "(select "
							+ DBConstants.FIELD_SEARCH_FLAGS_ID + " from " + DBConstants.TABLE_SEARCH_FLAGS 
							+ " where "
							+ DBConstants.FIELD_SEARCH_FLAGS_ID + " = ? and " 
							+ DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + " = ? )", 
					searchId, 
					tsrCreated);
		} catch (DataAccessException e) {
			logger.error(e);
			return -1;
		}
	}
    
    public static String findTsrServer(String fileName){
    	String sql = "SELECT a.ip_address,c.timestamp from ts_server a, tsr_files b, tsr_files_status c " + 
                     "WHERE b.file_name='" + (StringUtils.oraEscape(fileName.replace("\\", "\\\\") ))+ "' AND c.file_id=b.file_id AND c.file_status=0 AND a.enabled=1 AND a.id=c.instance_id ORDER BY timestamp";
		ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>() {		    
	        public String mapRow(ResultSet rs, int rowNum) throws SQLException {
	        	return rs.getString("ip_address");
	        }
	    };	
	    
    	List<String> ips = getSimpleTemplate().query(sql, mapper);
    	if(ips != null && ips.size() != 0){    		
    		return "http://" + ips.get(0);
    	} else {
    		return null;
    	}
     }
 
            
    
    public static int updateDiscountRatio(String list, float value){
        
    	if(list.length()==0){
    		logger.debug("DBManager updateDiscountRatio# exiting...");
    		return 0;
    	}

    	list = StringUtils.makeValidNumberList(list);
    	
        int rows = 0;
        String sql = "update " + TABLE_SEARCH + " SET " + 
        	" discount_ratio = discount_ratio * " + value +
        	" where id in ( " + list + " )";
        
		try {
			rows = DBManager.getSimpleTemplate().update(sql);
		} catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
            return -1;
		}

        if (logger.isDebugEnabled())   
        	logger.debug("DBManager updateDiscountRatio# exiting...");
        return rows;
    }
    
    /**
     * resets the discountRatio for the given ids (in TS_SEARCH)
     * @param list the list of ids
     * @param value the discount value that will be set
     * @return
     */
    public static int resetDiscountRatio(String list, float value){
        
    	if(list.length()==0){
    		logger.debug("DBManager updateDiscountRatio# exiting...");
    		return 0;
    	}
    	
    	list = StringUtils.makeValidNumberList(list);
    	
    	int rows = 0;
        String sql = "update " + TABLE_SEARCH + " SET " + 
        	" discount_ratio = " + value +
        	" where id in ( " + list + " )";
        
		try {
			rows = DBManager.getSimpleTemplate().update(sql);
		} catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
            return -1;
		}

        if (logger.isDebugEnabled())   
        	logger.debug("DBManager updateDiscountRatio# exiting...");
        return rows;
    }
    
    public static int updateSearchesInvoiceStatus(String list, String field, int value){
        
    	if(list.length()==0){
    		logger.debug("DBManager updateSearchesInvoiceStatus# exiting...");
    		return 0;
    	}
    	
        DBConnection conn = null;
        
        String sql = "call setField(?, ?, ?, ?, ?)";
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            
            NewProxyCallableStatement cs1 = (NewProxyCallableStatement)conn.prepareCall(sql);
            cs1.setString(1, DBConstants.TABLE_SEARCH_FLAGS);
            cs1.setString(2, field);
            cs1.setString(3, DBConstants.FIELD_SEARCH_FLAGS_ID);
            cs1.setString(4, StringUtils.makeValidNumberList(list));
            cs1.setInt(5, value);
            conn.executeCallableStatement(cs1);
            
        } catch (Exception e) {
            logger.error(e);
            return -1;
        }
        finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
        

        if (logger.isDebugEnabled())   logger.debug("DBManager updateSearchesInvoiceStatus# exiting...");
        return 0;
    }
    
    /**
     * Sets the given status for the searches in the list if forceDelete is false or sets the status to 0 if forceDelete is true
     * @param list
     * @param field database field to set
     * @param forceDelete if true ignores the given status
     * @return the number of rows affected
     */
    public static int updateSearchesInvoiceStatus(HashSet<InvoicedSearch> list, String field, boolean forceDelete) {
    	if(list.size() == 0) {
    		return 0;
    	} else {
    		int result = 0;
    		if(forceDelete) {
    			String searches = "";
    			for (InvoicedSearch invoicedSearch : list) {
    				if(searches.length() > 0) {
						searches += "," + invoicedSearch.getSearchId();
					} else {
						searches += invoicedSearch.getSearchId();
					}
    			}
    			result += updateSearchesInvoiceStatus(searches, field, 0);
    		} else {
	    		HashMap<Integer, HashSet<Long>> toUpdateList = new HashMap<Integer, HashSet<Long>>();
	    		for (InvoicedSearch invoicedSearch : list) {
					HashSet<Long> singleList = toUpdateList.get(invoicedSearch.getInvoiced());
					if(singleList == null) {
						singleList = new HashSet<Long>();
						toUpdateList.put(invoicedSearch.getInvoiced(), singleList);
					}
					singleList.add(invoicedSearch.getSearchId());
				}
	    		
	    		for (Integer status : toUpdateList.keySet()) {
	    			String searches = "";
					for (Long searchId : toUpdateList.get(status)) {
						if(searches.length() > 0) {
							searches += "," + searchId;
						} else {
							searches += searchId;
						}
					}
					result += updateSearchesInvoiceStatus(searches, field, status);
				}
    		}
    		return result;
    	}
    	
    }

    /**
     * updates user rating for searches with id in list
     * using the user attributes sent as parameter
     */
    public static void updateAbstractorRateId( Vector<Long> toUpdateRate, UserAttributes ua )
    {
    	if(toUpdateRate.size() == 0) {
    		return;
    	}
        String stm = "SELECT a." + DBConstants.FIELD_PROPERTY_COUNTY_ID + ", b." + DBConstants.FIELD_SEARCH_ID +
        		" FROM "+ TABLE_PROPERTY +" a, "+ TABLE_SEARCH +" b " +
        		" WHERE a.ID = b.PROPERTY_ID AND " +
        		" B.ID in ( " + org.apache.commons.lang.StringUtils.join(toUpdateRate.iterator(),",")+ " ) ";
        DBConnection conn = null;
        DatabaseData dbData = null;
        Calendar cal = Calendar.getInstance();
        try {

            conn = ConnectionPool.getInstance().requestConnection();
            dbData = conn.executeSQL( stm );
            if( dbData.getRowNumber() > 0 )
            {
                for( int i = 0 ; i < dbData.getRowNumber() ; i ++ )
                {
                    BigInteger countyId = (BigInteger) dbData.getValue( 1, i );
                    BigInteger iD = (BigInteger) dbData.getValue( 2, i );

                    UserRates abstractorRates = UserManager.getUserRates( ua.getID().longValue(), countyId.longValue(), cal.getTime() );
                    
                    if( abstractorRates != null )
                    {
                        BigDecimal countyPayrateId = abstractorRates.getIdCounty(new BigDecimal(countyId) );
                        stm = "UPDATE "+ TABLE_SEARCH +
                        	" SET " + DBConstants.FIELD_SEARCH_USER_RATING_ID + " = " + countyPayrateId.longValue() + 
                        	" WHERE ID = " + iD.longValue();
                        conn.executeSQL( stm );
                        conn.commit();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
    }
    
    public static boolean setKStatus(Vector<Long> toClose, UserAttributes ua, String closedDate, boolean keepAbstractor, long countyId) {
    	if(toClose == null || toClose.size() == 0){
    		return true;
    	}
        long rateId = 0;
        UserRates ur = UserManager.getUserRates(ua.getID().intValue(), countyId, Calendar.getInstance().getTime());
        if (ur != null) 
            rateId = ur.getID().longValue();
        long abstractorId = -1;
        if( !keepAbstractor )
            abstractorId = ua.getID().intValue();
        
        try {
        	
        	Vector<Long> toClosePart = null;
        	for (Long searchId : toClose) {
				if(toClosePart == null) {
					toClosePart = new Vector<Long>();
				}
				toClosePart.add(searchId);
				if(toClosePart.size() ==  25) {
					SetKStatusTransaction transaction = new SetKStatusTransaction(
		            		abstractorId, toClosePart, closedDate, rateId);
		            getTransactionTemplate().execute(transaction);
		            toClosePart = null;
				}
			}
        	
        	if(toClosePart != null) {
        		SetKStatusTransaction transaction = new SetKStatusTransaction(
	            		abstractorId, toClosePart, closedDate, rateId);
	            getTransactionTemplate().execute(transaction);
        	}
        	
        
        } catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    public static boolean deleteSearch(String list, UserAttributes ua) throws BaseException, UpdateDBException{
        DBConnection conn = null;
        DatabaseData data;
        list = StringUtils.makeValidNumberList(list);
        
        if (UserUtils.isTSAdmin(ua)) {
        	
            String sql = "delete from "+ TABLE_SEARCH +" where " + 
            DBConstants.FIELD_SEARCH_ID + " in (" + list + ")";
            
            logger.debug("DBManager deleteSearches(as TSAdmin)# SQL1: " + sql + " " + list);

            
    		try {
    			DBManager.getSimpleTemplate().update(sql);
    		} catch (Exception e) {
    			e.printStackTrace();
    			logger.error(e);
    		}
    		try {
				resetFVSFlagWhenReopenOrDeleteSearch(-1, list, false);
			} catch (Exception e) {
				e.printStackTrace();
    			logger.error(e);
			}
			logger.debug("DBManager deleteSearches(as TSAdmin)# exiting...");
            return true;
        }
        
        if (UserUtils.isCommAdmin(ua)||UserUtils.isTSCAdmin( ua)) {
            String stm = "select a." + 
            	DBConstants.FIELD_SEARCH_ID + ", " +
            	DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + ", " + 
            	DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + ", " + 
            	DBConstants.FIELD_SEARCH_STATUS + ", " + 
            	DBConstants.FIELD_SEARCH_FLAGS_WAS_OPENED + " from "+ 
            	TABLE_SEARCH + " a JOIN " + 
            	DBConstants.TABLE_SEARCH_FLAGS + " b ON a." +
            	DBConstants.FIELD_SEARCH_ID + " = b." + 
            	DBConstants.FIELD_SEARCH_FLAGS_ID + " where a." + 
            	DBConstants.FIELD_SEARCH_ID + " in (" + list + ")"; 
            
            try {
                
                conn = ConnectionPool.getInstance().requestConnection();
                data = conn.executeSQL(stm);
                //boolean couldBeDel = true;
                //boolean locked = false;
                int checkedBy = -1;
                for (int i = 0; i<data.getRowNumber(); i++){
                    if (((Long)data.getValue(2, i)).intValue() == 1) {
                        //couldBeDel = false;
                        throw new UpdateDBException("This search is completed and you have no rights to delete it!");
                    }
                    try {
                        checkedBy = ((Integer)data.getValue(3, i)).intValue();
                    } catch (Exception e) {
                        checkedBy = -1;
                    }
                    if ( checkedBy > 0) {
                        //locked = true;
                        throw new UpdateDBException("This search is locked  and can not be deleted! Unlock it first!");
                    }
                    
                    if( ((Integer)data.getValue(4, i)).intValue() == 1 )
                    {
                        // T state searches
                        
                        if( ((Integer)data.getValue(5, i)).intValue() == 1 )
                        {
                            // opened T state searches ( state T D )
                            
                            throw new UpdateDBException( "The search has already been restored and cannot be deleted!" );
                        }
                    }
                }
                String sql = "delete from "+ TABLE_SEARCH +" where " + 
                DBConstants.FIELD_SEARCH_ID + " in (" + list + ")";
                conn.executeSQL(sql);
                conn.commit();
                
                try {
    				resetFVSFlagWhenReopenOrDeleteSearch(-1, list, false);
    			} catch (Exception e) {
    				e.printStackTrace();
        			logger.error(e);
    			}
                
            } catch (BaseException e) {
                logger.error(e);
            } catch (SQLException e) {
                logger.error(e);
            }finally{
                try{
                    ConnectionPool.getInstance().releaseConnection(conn);
                }catch(BaseException e){
                    logger.error(e);
                }
                
            }   
            return true;
        }
        
        return false; 
    }
    
    
    private static final String SQL_UPDATE_SEARCHES_CONFIRMATION = 
    	"UPDATE " + DBConstants.TABLE_SEARCH_FLAGS + " " + 
		"SET " + DBConstants.FIELD_SEARCH_FLAGS_CONFIRMED + " = 1 " + 
		"WHERE " + DBConstants.FIELD_SEARCH_FLAGS_ID + " IN ( ? ) ";
    private static final String SQL_SELECT_SEARCHES_CONFIRMATION = 
    		"SELECT " + DBConstants.FIELD_SEARCH_ID + 
			" FROM " + DBConstants.TABLE_SEARCH + 
			" WHERE "+ DBConstants.FIELD_SEARCH_ABSTRACT_FILENO + " IN ( xxxxxxxxxx )";
    private static final int CHUNK_SIZE_SEARCHES_CONFIRMATION = 5;
    public static int updateSearchesConfirmation(String[] list){        
    	
    	Vector<String> chunks = new Vector<String>();
        String ids = "";
        try {        	
            if (list.length > 0){
                for(int i=0; i<list.length; i++){
                	ids += "" + list[i] + ",";
                	if ((i+1) % CHUNK_SIZE_SEARCHES_CONFIRMATION == 0){
                		chunks.add(ids.substring(0, ids.length() - 1));
                		ids = "";
                	}
                }
                if(!ids.isEmpty())
                	chunks.add(ids.substring(0, ids.length() - 1));
            }
        	SimpleJdbcTemplate sjt = ConnectionPool.getInstance().getSimpleTemplate();
            StringBuilder allSearches = new StringBuilder();
            for (String chunk : chunks) {
            	String arrayChunk[] = chunk.split(",");
            	String temp = "?";
            	for (int i = 1; i < arrayChunk.length; i++) {
					temp += ",?";
				}
            	final PreparedStatementCreatorFactory psFactory = new PreparedStatementCreatorFactory(
            			SQL_SELECT_SEARCHES_CONFIRMATION.replace("xxxxxxxxxx", temp));
            	for (int i = 0; i < arrayChunk.length; i++) {
					psFactory.addParameter(new SqlParameter(Types.VARCHAR));
				}
            	
            	List searchIds = sjt.getJdbcOperations().query(
            			psFactory.newPreparedStatementCreator(arrayChunk), 
            			new SearchIdRowMapper());
            	for (Object object : searchIds) {
            		allSearches.append(((SearchIdRowMapper)object).getSearchId() + ",");
				}
				
			}
            
            if(allSearches.length() > 0){
            	sjt.update(SQL_UPDATE_SEARCHES_CONFIRMATION.replace("?", allSearches.substring(0, allSearches.length()-1)));
            }
        
        } catch (Exception e) { 
            e.printStackTrace();
        }
        return 0;
    }   
    
    public static int deleteSearches(String list){
    	
    	list = StringUtils.makeValidNumberList(list);
    	
        String sql = "delete from "+ TABLE_SEARCH +" where " + 
        	DBConstants.FIELD_SEARCH_ID + " in " + 
        	"( select " + DBConstants.FIELD_SEARCH_FLAGS_ID + " from " +
        	DBConstants.TABLE_SEARCH_FLAGS + " where " + 
        	DBConstants.FIELD_SEARCH_FLAGS_ID + " in (" + list + ") AND " + 
        	DBConstants.FIELD_SEARCH_FLAGS_INVOICED + " = 0 AND " + 
        	DBConstants.FIELD_SEARCH_FLAGS_PAID + " = 0)";
        
        
        if (logger.isDebugEnabled())   logger.debug("DBManager deleteSearches# SQL1: " + sql );

		try {
			if (list.length() > 0) 
				DBManager.getSimpleTemplate().update(sql);
		} catch (Exception e) {
			  logger.error(e);
	          return -1;
		}
		
        if (logger.isDebugEnabled())   logger.debug("DBManager deleteSearches# exiting...");
        return 0;
    }
    public static int deleteSearchesCommAdmin(String list){
    
        String sql = "delete from "+ TABLE_SEARCH +" where " + 
	    	DBConstants.FIELD_SEARCH_ID + " in " + 
	    	"( select " + DBConstants.FIELD_SEARCH_FLAGS_ID + " from " +
	    	DBConstants.TABLE_SEARCH_FLAGS + " where " + 
	    	DBConstants.FIELD_SEARCH_FLAGS_ID + " in (" + list + ") AND " + 
	    	DBConstants.FIELD_SEARCH_FLAGS_INVOICED_CADM + " = 0 AND " + 
	    	DBConstants.FIELD_SEARCH_FLAGS_PAID_CADM + " = 0)";
        
        if (logger.isDebugEnabled())   logger.debug("DBManager deleteSearchesCommAdmin# SQL1: " + sql);

		try {
			if (list.length() > 0) 
				DBManager.getSimpleTemplate().update(sql);
		} catch (Exception e) {
			  logger.error(e);
	          return -1;
		}
		
        if (logger.isDebugEnabled())   logger.debug("DBManager deleteSearches# exiting...");
        return 0;
    }

    public static long insertPayrate(Payrate payr, UserAttributes ua){
                
        long sid = 0;

        String sDate;
        String mysqlDate = new FormatDate(FormatDate.TIMESTAMP).getDate(payr.getStartDate());
        sDate = "str_to_date( '" + mysqlDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
    
        String now = "str_to_date( '" + 
        	new FormatDate(FormatDate.TIMESTAMP).getDate(Calendar.getInstance().getTime()) + "' , '" + 
        	FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
        
        String sql = " insert into "+ TABLE_PAYRATE +" ( START_DATE, END_DATE,COMMUNITY_ID, COUNTY_ID, " +
                    " SEARCH_VALUE, UPDATE_VALUE,  SEARCH_COST, UPDATE_COST,  " +
                    "CURRENTOWNER_VALUE, CURRENTOWNER_COST, REFINANCE_VALUE, REFINANCE_COST, " +
                    " CONSTRUCTION_VALUE, CONSTRUCTION_COST, COMMERCIAL_VALUE, COMMERCIAL_COST, " +
                    " OE_VALUE, OE_COST, LIENS_VALUE, LIENS_COST, " +
                    " ACREAGE_VALUE, ACREAGE_COST, SUBLOT_VALUE, SUBLOT_COST, " +                    
                    " USER_LOGIN, USER_IP, USER_TIMESTAMP, index_a2c, index_c2a, fvs_value, fvs_cost ) VALUES ( " + 
                        sDate + ", " + "str_to_date( '31-12-2100 00:00:00', '%d-%m-%Y %H:%i:%S' )" + 
                    ","   + payr.getCommId() + ", " +  payr.getCountyId() +
                    ", " + payr.getSearchValue() + ", " + payr.getUpdateValue() +  
                    ", " + payr.getSearchCost() + ", " + payr.getUpdateCost() +                      
                    ", " + payr.getCurrentOwnerValue() + ", " + payr.getCurrentOwnerCost() + 
                    ", " + payr.getRefinanceValue() + ", " + payr.getRefinanceCost() +
                    ","  + payr.getConstructionValue() +", "+payr.getConstructionCost() +
                    ","  + payr.getCommercialValue() +", "+payr.getCommercialCost() +
                    ", " + payr.getOEValue() +", " + payr.getOECost() +
                    ", " +payr.getLiensValue() +", " + payr.getLiensCost() +
                    ", " +payr.getAcreageValue() +", "+payr.getAcreageCost() +
                    ", " +payr.getSublotValue() +", "+payr.getSublotCost() +
                    ", ?, ? " +                     
                    ", " + now + ",?,? " +
                    ", " + payr.getFvsValue() + ", " + payr.getFvsCost() + ") ";
        if (logger.isDebugEnabled())   logger.debug("DBManager insertPayrate# SQL: " + sql + " " + ua.getLOGIN() +" " + ua.getUserLoginIp());
        
        DBConnection conn = null;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            
    		PreparedStatement pstmt = conn.prepareStatement( sql );
    		pstmt.setString( 1, ua.getLOGIN());
    		pstmt.setString( 2, ua.getUserLoginIp());
    		pstmt.setDouble(3, payr.getIndexA2C());
    		pstmt.setDouble(4, payr.getIndexC2A());
    		pstmt.executeUpdate();
    		

            sid = getLastId(conn);
            pstmt.close();
        } catch (Exception e) {
            logger.error(e);
            return -1;
        } finally {
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }


        //if (logger.isDebugEnabled())   logger.debug("DBManager insertPayrate# exiting...");
        return sid;
    }

    public static int getCountyIdForPropertyId(long propertyId){
                
        logger.debug("DBManager getCountyIdForProperty# params: ");

        String sql = " select COUNTY_ID from "+ TABLE_PROPERTY +" where ID = " + propertyId;
        if (logger.isDebugEnabled())   logger.debug("DBManager getCountyIdForProperty# SQL: " + sql);
        int countyId = 0;
        
		try {
			countyId = DBManager.getSimpleTemplate().queryForInt(sql);
		} catch (Exception e) {
			logger.error(e);
		}

        if (logger.isDebugEnabled())   logger.debug("DBManager getCountyIdForProperty# exiting...");
        return countyId;
    }

    public static Payrate getCurrentPayrateForPropertyId(long commId, long propertyId, Date time, UserAttributes ua){

        return getCurrentPayrateForCounty(commId, getCountyIdForPropertyId(propertyId), time, ua);
    }       

    
    /**
     * This is a bad code duplicate, no alternatives :(<br>
     * this is the solution for the case when abstractor choose another payrate for his search<br>
     * the realcurrentPayrate is the payrate that is currently used by this abstractor<br>
     * and for the moment is not used; it was set for enhanced support
     * @param payrateId
     * @param realCurrentPayrate
     * @return
     */
    public static Payrate getCurrentPayrateForCounty(long payrateId, Payrate realCurrentPayrate){
                
        Payrate payr = new Payrate();
        
        String sql = " select * from "+ TABLE_PAYRATE +" where ID = " + payrateId;
        
        DBConnection conn = null;
        DatabaseData data;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            data = conn.executeSQL(sql);
            
            if (data.getRowNumber() > 0) {
                payr.setId(((BigInteger)data.getValue(1,0)).longValue());
                payr.setSearchValue(((Float)data.getValue(4,0)).doubleValue());
                payr.setUpdateValue(((Float)data.getValue(5,0)).doubleValue());
                payr.setCommId(((Float)data.getValue(6,0)).longValue());
                payr.setSearchCost(((Float)data.getValue(7,0)).doubleValue());
                payr.setUpdateCost(((Float)data.getValue(8,0)).doubleValue());    
                payr.setCountyId(((Long)data.getValue(11,0)).longValue());                
                payr.setCurrentOwnerValue( ((Float)data.getValue(12,0)).doubleValue() );
                payr.setCurrentOwnerCost( ((Float)data.getValue(13,0)).doubleValue() );
                payr.setRefinanceValue( ((Float)data.getValue(14,0)).doubleValue() );
                payr.setRefinanceCost( ((Float)data.getValue(15,0)).doubleValue() );
                payr.setConstructionValue(((Float)data.getValue(18,0)).doubleValue());
                payr.setConstructionCost(((Float)data.getValue(19,0)).doubleValue());
                payr.setCommercialValue(((Float)data.getValue(20,0)).doubleValue());      
                payr.setCommercialCost(((Float)data.getValue(21,0)).doubleValue());     
                payr.setOEValue(((Float)data.getValue(22,0)).doubleValue());    
                payr.setOECost(((Float)data.getValue(23,0)).doubleValue());
                payr.setLiensValue(((Float)data.getValue(24,0)).doubleValue());
                payr.setLiensCost(((Float)data.getValue(25,0)).doubleValue());
                payr.setAcreageValue(((Float)data.getValue(26,0)).doubleValue());
                payr.setAcreageCost(((Float)data.getValue(27,0)).doubleValue());
                payr.setSublotValue(((Float)data.getValue(28,0)).doubleValue());
                payr.setSublotCost(((Float)data.getValue(29,0)).doubleValue());
                payr.setIndexA2C(((Float)data.getValue(Payrate.FIELD_INDEX_A2C,0)));
                payr.setIndexC2A(((Float)data.getValue(Payrate.FIELD_INDEX_C2A,0)));
                payr.setFvsValue(((Float) data.getValue(Payrate.FIELD_FVS_C2A, 0)));
                payr.setFvsCost(((Float) data.getValue(Payrate.FIELD_FVS_A2C, 0)));
               
            }

        } catch (Exception e) {
            logger.error("getCurrentPayrateForCounty: payrateId = " + payrateId, e);
            Log.sendExceptionViaEmail(e, "getCurrentPayrateForCounty: payrateId = " + payrateId);
        }
        finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
        return payr;
    }
    
    public static Payrate getCurrentPayrateForCounty(long commId, long countyId, Date time, UserAttributes ua){
                
        Payrate payr = new Payrate();
        //logger.debug("DBManager getCurrentPayrateForCounty# params: ");

        String sDate;
        String mysqlDate = new FormatDate(FormatDate.TIMESTAMP).getDate(time);
        
        sDate = "str_to_date( '" + mysqlDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
    
        String sql = " select * from "+ TABLE_PAYRATE +" where START_DATE <= " + 
                         sDate + " and COMMUNITY_ID = " + commId + " and COUNTY_ID = " + countyId + " order by START_DATE desc, ID desc limit 1";
        //logger.debug("DBManager getCurrentPayrateForCounty# SQL: " + sql);
        
        DBConnection conn = null;
        DatabaseData data;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            data = conn.executeSQL(sql);
            
            if (data.getRowNumber() > 0) {
                payr.setId(Long.parseLong(data.getValue(1,0).toString()));
                //payr.setStartDate(FormatDate.getDateFromFormatedString((String)data.getValue(2,0), FormatDate.TIMESTAMP));
                payr.setSearchValue(((Float)data.getValue(4,0)).doubleValue());
                payr.setUpdateValue(((Float)data.getValue(5,0)).doubleValue());
                payr.setCommId(((Float)data.getValue(6,0)).longValue());
                payr.setSearchCost(((Float)data.getValue(7,0)).doubleValue());
                payr.setUpdateCost(((Float)data.getValue(8,0)).doubleValue());    
                payr.setCountyId(((Long)data.getValue(11,0)).longValue());                
                payr.setCurrentOwnerValue( ((Float)data.getValue(12,0)).doubleValue() );
                payr.setCurrentOwnerCost( ((Float)data.getValue(13,0)).doubleValue() );
                payr.setRefinanceValue( ((Float)data.getValue(14,0)).doubleValue() );
                payr.setRefinanceCost( ((Float)data.getValue(15,0)).doubleValue() );
                payr.setConstructionValue(((Float)data.getValue(18,0)).doubleValue());
                payr.setConstructionCost(((Float)data.getValue(19,0)).doubleValue());
                payr.setCommercialValue(((Float)data.getValue(20,0)).doubleValue());      
                payr.setCommercialCost(((Float)data.getValue(21,0)).doubleValue());     
                payr.setOEValue(((Float)data.getValue(22,0)).doubleValue());    
                payr.setOECost(((Float)data.getValue(23,0)).doubleValue());
                payr.setLiensValue(((Float)data.getValue(24,0)).doubleValue());
                payr.setLiensCost(((Float)data.getValue(25,0)).doubleValue());
                payr.setAcreageValue(((Float)data.getValue(26,0)).doubleValue());
                payr.setAcreageCost(((Float)data.getValue(27,0)).doubleValue());
                payr.setSublotValue(((Float)data.getValue(28,0)).doubleValue());
                payr.setSublotCost(((Float)data.getValue(29,0)).doubleValue());
                payr.setIndexA2C(((Float)data.getValue(Payrate.FIELD_INDEX_A2C,0)));
                payr.setIndexC2A(((Float)data.getValue(Payrate.FIELD_INDEX_C2A,0)));
                payr.setFvsValue(((Float) data.getValue(Payrate.FIELD_FVS_C2A, 0)));
                payr.setFvsCost(((Float) data.getValue(Payrate.FIELD_FVS_A2C, 0)));
            }
            else{
                //computing the Date of the interval start
                Calendar cal = Calendar.getInstance();
                cal.setTime(time);
                cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
                cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
                cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        
                payr.setStartDate(cal.getTime());
                payr.setCountyId(countyId);
                payr.setCommId(commId );
                payr.setId(DBManager.insertPayrate(payr,ua));
            }
            //ci.releaseDBConnection();
        } catch (Exception e) {
            logger.error(e);
            Log.sendExceptionViaEmail(e, sql);
            
            try {
	            data = conn.executeSQL(sql);
	            
	            if (data.getRowNumber() > 0) {
	                payr.setId(Long.parseLong(data.getValue(1,0).toString()));
	                //payr.setStartDate(FormatDate.getDateFromFormatedString((String)data.getValue(2,0), FormatDate.TIMESTAMP));
	                payr.setSearchValue(((Float)data.getValue(4,0)).doubleValue());
	                payr.setUpdateValue(((Float)data.getValue(5,0)).doubleValue());
	                payr.setCommId(((Float)data.getValue(6,0)).longValue());
	                payr.setSearchCost(((Float)data.getValue(7,0)).doubleValue());
	                payr.setUpdateCost(((Float)data.getValue(8,0)).doubleValue());    
	                payr.setCountyId(((Long)data.getValue(11,0)).longValue());                
	                payr.setCurrentOwnerValue( ((Float)data.getValue(12,0)).doubleValue() );
	                payr.setCurrentOwnerCost( ((Float)data.getValue(13,0)).doubleValue() );
	                payr.setRefinanceValue( ((Float)data.getValue(14,0)).doubleValue() );
	                payr.setRefinanceCost( ((Float)data.getValue(15,0)).doubleValue() );
	                payr.setConstructionValue(((Float)data.getValue(18,0)).doubleValue());
	                payr.setConstructionCost(((Float)data.getValue(19,0)).doubleValue());
	                payr.setCommercialValue(((Float)data.getValue(20,0)).doubleValue());      
	                payr.setCommercialCost(((Float)data.getValue(21,0)).doubleValue());     
	                payr.setOEValue(((Float)data.getValue(22,0)).doubleValue());    
	                payr.setOECost(((Float)data.getValue(23,0)).doubleValue());
	                payr.setLiensValue(((Float)data.getValue(24,0)).doubleValue());
	                payr.setLiensCost(((Float)data.getValue(25,0)).doubleValue());
	                payr.setAcreageValue(((Float)data.getValue(26,0)).doubleValue());
	                payr.setAcreageCost(((Float)data.getValue(27,0)).doubleValue());
	                payr.setSublotValue(((Float)data.getValue(28,0)).doubleValue());
	                payr.setSublotCost(((Float)data.getValue(29,0)).doubleValue());
	                payr.setIndexA2C(((Float)data.getValue(Payrate.FIELD_INDEX_A2C,0)));
	                payr.setIndexC2A(((Float)data.getValue(Payrate.FIELD_INDEX_C2A,0)));
	                payr.setFvsValue(((Float) data.getValue(Payrate.FIELD_FVS_C2A, 0)));
	                payr.setFvsCost(((Float) data.getValue(Payrate.FIELD_FVS_A2C, 0)));
	            } else{
	                //computing the Date of the interval start
	                Calendar cal = Calendar.getInstance();
	                cal.setTime(time);
	                cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
	                cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
	                cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
	        
	                payr.setStartDate(cal.getTime());
	                payr.setCountyId(countyId);
	                payr.setCommId(commId );
	                payr.setId(DBManager.insertPayrate(payr,ua));
	            }
            
            } catch (Exception e2) {
            	logger.error(e2);
                Log.sendExceptionViaEmail(e2, "Second Try: "  + sql);
			}
            
            
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }

        //logger.debug("DBManager getCurrentPayrateForCounty# exiting...");
        return payr;
    }

    public static Payrate[] getCurrentPayratesForCommunityAndState(long commId, long stateId, Date time){
        
        if (logger.isDebugEnabled())   
        	logger.debug("DBManager getPayratesForCommunity# params: " + commId + "/ " + stateId + "/ " + time);
        if(stateId == -1) {
        	return new Payrate[0];
        }

        String mysqlDate = new FormatDate(FormatDate.TIMESTAMP).getDate(time);
        String sDate = "str_to_date( '" + mysqlDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";

        String sql = "select a.ID," +
        	" DATE_FORMAT(a.START_DATE, '%d-%m-%Y %H:%i:%S') START_DATE, " +
	        " DATE_FORMAT(a.END_DATE, '%d-%m-%Y %H:%i:%S') END_DATE, " +
	        " a.COMMUNITY_ID, x.ID county_id, " +
	        " x.NAME, y.STATEABV," +
	        " DATE_FORMAT(i.DUE_DATE , '%d-%m-%Y %H:%i:%S') DUE_DATE, " +
	        " DATE_FORMAT(i.CITY_DUE_DATE, '%d-%m-%Y %H:%i:%S') CITY_DUE_DATE, " +	        
	        " a.SEARCH_VALUE, a.SEARCH_COST, a.UPDATE_VALUE, a.UPDATE_COST, " +	     	       
	        " a.CURRENTOWNER_VALUE, a.CURRENTOWNER_COST, a.REFINANCE_VALUE, a.REFINANCE_COST, " +
	        " a.CONSTRUCTION_VALUE, a.CONSTRUCTION_COST, a.COMMERCIAL_VALUE, a.COMMERCIAL_COST, " +
	        " a.OE_VALUE, a.OE_COST, a.LIENS_VALUE, a.LIENS_COST, " +
	        " a.ACREAGE_VALUE, a.ACREAGE_COST, a.SUBLOT_VALUE, a.SUBLOT_COST, " +
	        " cc." + DBConstants.FIELD_COUNTY_COMMUNITY_DEFAULT_CERTIFICATION_DATE_OFFSET + ", a.index_c2a, a.index_a2c, " +
	        " cc." + DBConstants.FIELD_COUNTY_COMMUNITY_DEFAULT_OFFICIAL_START_DATE_OFFSET + ", a.fvs_value, a.fvs_cost " +
	        " from "+ TABLE_COUNTY +" x JOIN "+ TABLE_STATE +" y ON y.id = x.state_id " +
	        	"LEFT JOIN " + 
	        			DBConstants.TABLE_COUNTY_COMMUNITY + " cc on (cc." + 
	        			DBConstants.FIELD_COUNTY_COMMUNITY_COUNTY_ID + " = x.id and cc." + 
	        			DBConstants.FIELD_COUNTY_COMMUNITY_COMMUNITY_ID + " = " + commId + ") " +
	        	"LEFT JOIN "+ TABLE_DUE_DATE +" i ON (x.id = i.COUNTY_ID and i.COMM_ID=" +commId + ") " + 
	        "LEFT JOIN (select f.* from "+ TABLE_PAYRATE +" f," +
	        "(select max(c.id) id, c.county_id from "+ TABLE_PAYRATE +" c, " +
	            "(select max(start_date) start_date, county_id from "+ TABLE_PAYRATE +" " +
	            	"where community_id = " + commId + " and start_date <= " + sDate + " group by county_id) d " + 
	            "where c.start_date = d.start_date and c.COMMUNITY_ID = " + commId + " and c.county_id = d.county_id group by c.county_id) b " +
	            "where f.id = b.id) a ON x.id = a.COUNTY_ID " + 
	        "where  x.state_id = " + stateId +
	        " order by x.NAME";
        if (logger.isDebugEnabled())   
        	logger.debug("DBManager getPayratesForCommunity# SQL: " + sql);
   
        return getSimpleTemplate().query(sql, new PayrateATSSettingsMapper()).toArray(new Payrate[0]);
        
    }

    public static Payrate[] getPayrateHistoryForCounty(long commId, long countyId){
                
        Payrate[] payr = new Payrate[0];
        if (logger.isDebugEnabled())   logger.debug("DBManager getPayrateHistoryForCounty# params: " + commId + "/ " + countyId);

        String sql = " select a.ID, DATE_FORMAT(a.START_DATE, '%d-%m-%Y %H:%i:%S'), " +
                    "DATE_FORMAT(a.END_DATE, '%d-%m-%Y %H:%i:%S'), a.COMMUNITY_ID, a.COUNTY_ID, " +
                    " x.NAME, y.STATEABV, "+
                    "a.SEARCH_VALUE,  a.SEARCH_COST, a.UPDATE_VALUE, a.UPDATE_COST, " +                    
                    "a.CURRENTOWNER_VALUE, a.CURRENTOWNER_COST, a.REFINANCE_VALUE, a.REFINANCE_COST, " +
                    "a.CONSTRUCTION_VALUE, a.CONSTRUCTION_COST, a.COMMERCIAL_VALUE, a.COMMERCIAL_COST,  " +
                    "a.OE_VALUE, a.OE_COST, a.LIENS_VALUE, a.LIENS_COST, " +
                    "a.ACREAGE_VALUE, a.ACREAGE_COST, a.SUBLOT_VALUE, a.SUBLOT_COST , a.index_c2a, a.index_a2c, " +
                    "a.FVS_VALUE, a.FVS_COST " + 
                    "from "+ TABLE_PAYRATE +" a, "+ TABLE_COUNTY +" x, "+ TABLE_STATE +" y " +
                    "where a.COUNTY_ID = " + countyId + " and community_id = " + commId + 
                    " and a.COUNTY_ID = x.id and y.id = x.state_id order by START_DATE desc, ID desc";
        if (logger.isDebugEnabled())   logger.debug("DBManager getPayrateHistoryForCounty# SQL: " + sql);
        
        DBConnection conn = null;
        DatabaseData data;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            data = conn.executeSQL(sql);
            
            payr = new Payrate [data.getRowNumber()];
            if (logger.isDebugEnabled())   logger.debug("DBManager getPayrateHistoryForCounty# results number: " + data.getRowNumber());
            for (int i = 0; i<data.getRowNumber(); i++){
                payr[i] = new Payrate();
                payr[i].setId(((BigInteger)data.getValue(1,i)).longValue());
                payr[i].setStartDate(FormatDate.getDateFromFormatedString((String)data.getValue(2,i), FormatDate.TIMESTAMP));
                payr[i].setCommId(((Float)data.getValue(4,i)).longValue());
                payr[i].setCountyId(((Long)data.getValue(5,i)).longValue());
                payr[i].setCountyName((String)data.getValue(6,i));
                payr[i].setStateAbv((String)data.getValue(7,i));      
                payr[i].setSearchValue(((Float)data.getValue(8,i)).doubleValue());
                payr[i].setSearchCost(((Float)data.getValue(9,i)).doubleValue());
                payr[i].setUpdateValue(((Float)data.getValue(10,i)).doubleValue());
                payr[i].setUpdateCost(((Float)data.getValue(11,i)).doubleValue());
                payr[i].setCurrentOwnerValue(((Float)data.getValue(12,i)).doubleValue());
                payr[i].setCurrentOwnerCost(((Float)data.getValue(13,i)).doubleValue());
                payr[i].setRefinanceValue(((Float)data.getValue(14,i)).doubleValue());
                payr[i].setRefinanceCost(((Float)data.getValue(15,i)).doubleValue());
                payr[i].setConstructionValue(((Float)data.getValue(16,i)).doubleValue());
                payr[i].setConstructionCost(((Float)data.getValue(17,i)).doubleValue());
                payr[i].setCommercialValue(((Float)data.getValue(18,i)).doubleValue());
                payr[i].setCommercialCost(((Float)data.getValue(19,i)).doubleValue());
                payr[i].setOEValue(((Float)data.getValue(20,i)).doubleValue());
                payr[i].setOECost(((Float)data.getValue(21,i)).doubleValue());
                payr[i].setLiensValue(((Float)data.getValue(22,i)).doubleValue());
                payr[i].setLiensCost(((Float)data.getValue(23,i)).doubleValue());
                payr[i].setAcreageValue(((Float)data.getValue(24,i)).doubleValue());
                payr[i].setAcreageCost(((Float)data.getValue(25,i)).doubleValue());
                payr[i].setSublotValue(((Float)data.getValue(26,i)).doubleValue());
                payr[i].setSublotCost(((Float)data.getValue(27,i)).doubleValue());
                payr[i].setIndexA2C(((Float)data.getValue(Payrate.FIELD_INDEX_A2C,i)));
                payr[i].setIndexC2A(((Float)data.getValue(Payrate.FIELD_INDEX_C2A,i)));
                payr[i].setFvsValue(((Float)data.getValue(Payrate.FIELD_FVS_C2A, i)));
                payr[i].setFvsCost(((Float)data.getValue(Payrate.FIELD_FVS_A2C, i)));
            }
            
        } catch (Exception e) {
            logger.error(e);
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }

        if (logger.isDebugEnabled())   logger.debug("DBManager getPayrateHistoryForCounty# exiting...");
        return payr;
    }
    public static Payrate[] getPayrateHistoryForCity(long commId, long  reportState, long  countyIdX)
    {
        Payrate[] payr = new Payrate[0];
        if (logger.isDebugEnabled())   logger.debug("DBManager getPayrateHistoryForCounty# params: Comm: " + commId + "/ State: " + reportState + "/ Cnty: " + countyIdX);
        
        String sql ="select a.ID, a.NAME, date_format(a.CITY_DUE_DATE , '%d-%m-%Y %H:%i:%S'), a.COUNTY_ID " + 
                    "from "+ TABLE_CITY +" a where a.COUNTY_ID = " + countyIdX + 
                    " order by NAME";

                if (logger.isDebugEnabled())   logger.debug("DBManager getPayrateHistoryForCounty# SQL: " + sql);
    
                DBConnection conn = null;
                DatabaseData data;
        
                try {
            
                    conn = ConnectionPool.getInstance().requestConnection();
                    data = conn.executeSQL(sql);
            
                    payr = new Payrate [data.getRowNumber()];
                    if (logger.isDebugEnabled())   logger.debug("DBManager getPayrateHistoryForCounty# results number: " + data.getRowNumber());
                    for (int i = 0; i<data.getRowNumber(); i++){
                        payr[i] = new Payrate();
                        payr[i].setId(((BigDecimal)data.getValue(1,i)).longValue());
                        payr[i].setCityID(((BigDecimal)data.getValue(1,i)).longValue());
                        payr[i].setCityName((String)data.getValue(2,i));
                        payr[i].setCityDue(FormatDate.getDateFromFormatedStringGMT((String)data.getValue(3,i), FormatDate.TIMESTAMP));
                        payr[i].setCountyId(((BigDecimal)data.getValue(4,i)).longValue());                      
                    }
            
                } 
                catch (Exception e) 
                {
                    logger.error(e);
                } 
                finally
                {
                    try
                    {
                        ConnectionPool.getInstance().releaseConnection(conn);
                    }
                    catch(BaseException e)
                    {
                        logger.error(e);
                    }           
                }
        
        return payr;
        
        
    }
    public static long insertDueDate(DueDate dueDate){
                
        long sid = 0;
        

        String sDate;
        String mysqlDate = new FormatDate(FormatDate.TIMESTAMP).getDate(dueDate.getDueDate());
        sDate = "str_to_date( '" + mysqlDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
        
        String cityMysqlDate = new FormatDate(FormatDate.TIMESTAMP).getDate(Util.getDefaultDueDate());
        String citySDate = "str_to_date( '" + cityMysqlDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
    
        String sql = "insert into "+ TABLE_DUE_DATE +" (COUNTY_ID, COMM_ID, DUE_DATE, CITY_DUE_DATE ) VALUES ( " + 
                        dueDate.getCountyId() + ", " + dueDate.getCommId() + ", " + sDate + ", " + citySDate + " ) ";
        if (logger.isDebugEnabled())   if (logger.isDebugEnabled())   logger.debug("DBManager insertDueDate# SQL: " + sql);
        
        DBConnection conn = null;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            conn.executeSQL(sql);
            sid = getLastId(conn);
            
        } catch (Exception e) {
            logger.error(e);
            return -1;
        }
        finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }

        if (logger.isDebugEnabled())   logger.debug("DBManager insertDueDate# exiting...");
        return sid;
    }

    public static DueDate getDueDateForCounty(long commId, long countyId){
                
        DueDate dueDate = new DueDate();
        //if (logger.isDebugEnabled())   logger.debug("DBManager getDueDateForCounty# params: " + commId + "/ " + countyId);

        String sql = " select a.ID, a.COUNTY_ID, a.COMM_ID, DATE_FORMAT(a.DUE_DATE, '%d-%m-%Y %H:%i:%S') " +
                    "from "+ TABLE_DUE_DATE +" a " +
                    "where a.COUNTY_ID = " + countyId + " and a.comm_id = " + commId; 
        //if (logger.isDebugEnabled())   logger.debug("DBManager getDueDateForCounty# SQL: " + sql);
        
        DBConnection conn = null;
        DatabaseData data;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            data = conn.executeSQL(sql);
            
            if (data != null && data.getRowNumber() > 0){
                dueDate.setId(Long.parseLong(data.getValue(1,0).toString()));
                dueDate.setCommId(Long.parseLong(data.getValue(2,0).toString()));
                dueDate.setCountyId(Long.parseLong(data.getValue(3,0).toString()));
                dueDate.setDueDate(FormatDate.getDateFromFormatedString((String)data.getValue(4,0), FormatDate.TIMESTAMP));
            }
            else{
                dueDate.setCommId(commId);
                dueDate.setCountyId(countyId);
                dueDate.setDueDate(Util.getDefaultDueDate());
                dueDate.setId(DBManager.insertDueDate(dueDate));
            }
            
        } catch (Exception e) {
            logger.error(e);
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }

        //TODO: aici merge un cache
        //if (logger.isDebugEnabled())   logger.debug("DBManager getDueDateForCounty# exiting...");
        return dueDate;
    }

    public static Date getDueDateForCity(long commId, long countyId){
                    
        DueDate dueDate = new DueDate();
        //if (logger.isDebugEnabled())   logger.debug("DBManager getDueDateForCounty# params: " + commId + "/ " + countyId);

        String sql = " select a.ID, a.COUNTY_ID, a.COMM_ID, date_format(a.CITY_DUE_DATE, '%d-%m-%Y %H:%i:%S') " +
                    "from "+ TABLE_DUE_DATE +" a " +
                    "where a.COUNTY_ID = " + countyId + " and a.comm_id = " + commId; 
        //if (logger.isDebugEnabled())   logger.debug("DBManager getDueDateForCounty# SQL: " + sql);
        
        DBConnection conn = null;
        DatabaseData data;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            data = conn.executeSQL(sql);
            
            if (data != null && data.getRowNumber() > 0){
                dueDate.setId(Long.parseLong(data.getValue(1,0).toString()));
                dueDate.setCommId(Long.parseLong(data.getValue(2,0).toString()));
                dueDate.setCountyId(Long.parseLong(data.getValue(3,0).toString()));
                dueDate.setDueDate(FormatDate.getDateFromFormatedString((String)data.getValue(4,0), FormatDate.TIMESTAMP));
            }
            else{
                dueDate.setCommId(commId);
                dueDate.setCountyId(countyId);
                dueDate.setDueDate(Util.getDefaultDueDate());
                dueDate.setId(DBManager.insertDueDate(dueDate));
            }
            
        } catch (Exception e) {
            logger.error(e);
        }
        finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }

        //TODO: aici merge un cache
        //if (logger.isDebugEnabled())   logger.debug("DBManager getDueDateForCounty# exiting...");
        return dueDate.getDueDate();
    }
    
    public static DueDate getDueDateForCity1(long commId, long countyId){
                
        DueDate dueDate = new DueDate();
        if (logger.isDebugEnabled())   logger.debug("DBManager getDueDateForCounty# params: " + commId + "/ " + countyId);

        String sql = " select a.ID, a.COUNTY_ID, a.COMM_ID, date_format(a.CITY_DUE_DATE, '%d-%m-%Y %H:%i:%S') " +
                    "from "+ TABLE_DUE_DATE +" a " +
                    "where a.COUNTY_ID = " + countyId + " and a.comm_id = " + commId; 
        if (logger.isDebugEnabled())   logger.debug("DBManager getDueDateForCounty# SQL: " + sql);
        
        DBConnection conn = null;
        DatabaseData data;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            data = conn.executeSQL(sql);
            
            if (data != null && data.getRowNumber() > 0){
                dueDate.setId(Long.parseLong(data.getValue(1,0).toString()));
                dueDate.setCommId(Long.parseLong(data.getValue(2,0).toString()));
                dueDate.setCountyId(Long.parseLong(data.getValue(3,0).toString()));
                dueDate.setDueDate(FormatDate.getDateFromFormatedString((String)data.getValue(4,0), FormatDate.TIMESTAMP));
            }
            else{
                dueDate.setCommId(commId);
                dueDate.setCountyId(countyId);
                dueDate.setDueDate(Util.getDefaultDueDate());
                dueDate.setId(DBManager.insertDueDate(dueDate));
            }

        } catch (Exception e) {
            logger.error(e);
        } finally {
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }

        if (logger.isDebugEnabled())   logger.debug("DBManager getDueDateForCounty# exiting...");
        return dueDate;
    }

    public static HashMap<Long, Integer> getDueDatesForCommunity(long commId, 
    		Vector<DueDate> countyDates, Vector<DueDate> cityDates){
    	
    	String sql = " select a.ID, a.COUNTY_ID, a.COMM_ID, " + 
    		"date_format(a.CITY_DUE_DATE, '%d-%m-%Y %H:%i:%S'), " +
    		"date_format(a.DUE_DATE, '%d-%m-%Y %H:%i:%S') " +
		    "from "+ TABLE_DUE_DATE +" a " +
		    "where a.comm_id = " + commId; 
		
		DBConnection conn = null;
		DatabaseData data;
		//this will maintain county-position corespondance
		HashMap<Long, Integer> counties = new HashMap<Long, Integer>();
		try {
		
			conn = ConnectionPool.getInstance().requestConnection();
			data = conn.executeSQL(sql);
			
			for (int i = 0; i < data.getRowNumber(); i++) {
				DueDate countyDD = new DueDate();
				countyDD.setId((Long)data.getValue(1,0));
				countyDD.setCommId((Long)data.getValue(2,0));
				countyDD.setCountyId((Long)data.getValue(3,0));
				countyDD.setDueDate(FormatDate.getDateFromFormatedString((String)data.getValue(5,0), FormatDate.TIMESTAMP));
				
				DueDate cityDD = new DueDate();
				cityDD.setId((Long)data.getValue(1,0));
				cityDD.setCommId((Long)data.getValue(2,0));
				cityDD.setCountyId((Long)data.getValue(3,0));
				cityDD.setDueDate(FormatDate.getDateFromFormatedString((String)data.getValue(4,0), FormatDate.TIMESTAMP));
				
				counties.put((Long)data.getValue(3,0), i);
			}
		
		
		} catch (Exception e) {
			logger.error(e);
		} finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    logger.error(e);
			}           
		}

    	return counties;
    }

    public static Date getDueDateForCountyAndCommunity(long commId, long countyId){
        
        return getDueDateForCounty(commId, countyId).getDueDate();
    }

    public static DueDate updateDueDateForCounty(long commId, long countyId, Date dDate){
        
        DueDate dueDate = getDueDateForCounty( commId, countyId);
        if (logger.isDebugEnabled())   logger.debug("DBManager updateDueDateForCounty# params: " + commId + "/ " + countyId);

        String mysqlDate = new FormatDate(FormatDate.TIMESTAMP).getDate(dDate);
        String sDate = "str_to_date( '" + mysqlDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
        
        String sql = " update "+ TABLE_DUE_DATE +" set DUE_DATE = " + sDate +
                    " where ID = " + dueDate.getId(); 
        if (logger.isDebugEnabled())   logger.debug("DBManager updateDueDateForCounty# SQL: " + sql);
        
        DBConnection conn = null;       
        
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            conn.executeSQL(sql);
            conn.commit();
            
        } catch (Exception e) {
            logger.error(e);
        }
        finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }

        if (logger.isDebugEnabled())   logger.debug("DBManager updateDueDateForCounty# exiting...");
        return dueDate;
    }
    public static DueDate updateCityDueDateForCounty(long commId, long cityId, Date dDate){
        
        DueDate dueDate = new DueDate();
    //  dueDate = getDueDateForCity1( commId, countyId);
        if (logger.isDebugEnabled())   logger.debug("DBManager updateDueDateForCity# params: " + commId + "/ " + cityId);

        String sDate;
        String mysqlDate = new FormatDate(FormatDate.TIMESTAMP).getDate(dDate);
        sDate = "str_to_date( '" + mysqlDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
    
        String sql = " update "+ TABLE_DUE_DATE +" set CITY_DUE_DATE = " + sDate +
                    " where COUNTY_ID = " + cityId; 
        if (logger.isDebugEnabled())   logger.debug("DBManager updateDueDateForCity# SQL: " + sql);
        
        DBConnection conn = null;  
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            conn.executeSQL(sql);
            conn.commit();

        } catch (Exception e) {
            logger.error(e);
        } finally {
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }

        if (logger.isDebugEnabled())   logger.debug("DBManager updateDueDateForCity# exiting...");
        return dueDate;
    }
    
    public static int updateCountyAndCityDueDates(long commId, long countyId, 
    		Date countyDate, Date cityDate){
    	int rowsAffected = 0;
    	if (logger.isDebugEnabled())   logger.debug("DBManager updateDueDateForCity# params: " + commId + "/ " + countyId);

        //String countySDate;
        //String countyDateString = new FormatDate(FormatDate.TIMESTAMP).getDate(countyDate);
        //countySDate = "str_to_date( '" + countyDateString + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
        
        String citySDate;
        String cityDateString = new FormatDate(FormatDate.TIMESTAMP).getDate(cityDate);
        citySDate = "str_to_date( '" + cityDateString + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
    
        String sql = " update "+ TABLE_DUE_DATE +" set CITY_DUE_DATE = " + citySDate + ", " + 
                    " where COUNTY_ID = " + countyId + " and comm_id = " + commId; 
        if (logger.isDebugEnabled())   logger.debug("DBManager updateDueDateForCity# SQL: " + sql);
        
        DBConnection conn = null;  
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            conn.executeSQL(sql);
            conn.commit();

        } catch (Exception e) {
            logger.error(e);
        } finally {
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
    	return rowsAffected;
    }
    
    
    private static final String SQL_GET_SEARCH_TSR_FOLDER = " select " + DBConstants.FIELD_SEARCH_TSR_FOLDER + " from "+ TABLE_SEARCH +" where " + DBConstants.FIELD_SEARCH_ID + " = ?";
    
    /**
     * Returns the full path of the search context. files.path from server is concatenated with the relative path stored on database<br>
     * In the end the path will contain only OS specific separators and will end in one
     * @param searchId
     * @return
     */
    public static String getSearchTSRFolder(long searchId) {
        
        String tsrFolder = null;
		try {
			tsrFolder = getSimpleTemplate().queryForObject(SQL_GET_SEARCH_TSR_FOLDER, String.class, searchId);
			if(org.apache.commons.lang.StringUtils.isNotBlank(tsrFolder)) {
				String filesPath = ServerConfig.getFilePath().replace("\\", "/").replaceAll("/{2,}", "/");
				String formatedTsrFolder = tsrFolder.replace("\\", "/").replaceAll("/{2,}", "/");
				
				if(!formatedTsrFolder.toLowerCase().contains(filesPath.toLowerCase())) {
					logger.info("Old Style TsrFolder: [" + ServerConfig.getFilePath() + File.separator + tsrFolder + "] and new Style TsrFolder: [" + filesPath + formatedTsrFolder + "]");
            		tsrFolder = filesPath + formatedTsrFolder;
				}
			}
		} catch (Exception e) {
			logger.error("Cannot read TSRFolder for searchid " + searchId, e);
		}

//        String stm = " select TSR_FOLDER from "+ TABLE_SEARCH +" where ID = " + searchId;
//                
//        DBConnection conn = null;
//        
//        try {
//            
//            conn = ConnectionPool.getInstance().requestConnection();
//            PreparedStatement ps = conn.prepareStatement(stm);
//            
//            DatabaseData data = conn.executePrepQuery(ps);
//            if (logger.isDebugEnabled()) {   
//            	logger.debug("DBManager getSearchTSRFolder# searchId : " + searchId + " got data size : " + data.getRowNumber());
//            }
//            for (int i = 0; i < data.getRowNumber(); i++){
//                tsrFolder = (String) data.getValue(1,i); 
//                if(!StringUtils.isEmpty(tsrFolder)) {
//                	String ref = ServerConfig.getFilePath().replace("\\", "/").replaceAll("/{2,}", "/").toLowerCase();
//                    String can = tsrFolder.replace("\\", "/").replaceAll("/{2,}", "/").toLowerCase();
//                    if(!can.contains(ref)){
//                    	if("andrei".equals(ServerConfig.getSourceComputer())) {
//                    		tsrFolder = tsrFolder.replaceAll("\\\\+","\\\\");
//    	                	if(tsrFolder.startsWith("D:\\data\\ATSFolder\\")) {
//    	                		tsrFolder = tsrFolder.replace("D:\\data\\ATSFolder\\", "D:\\work\\ATSFolder\\");
//    	                	} else {
//    	                		tsrFolder = ServerConfig.getFilePath() + File.separator + tsrFolder;	
//    	                	}
//                    	
//                    	} else {
//                    		tsrFolder = ServerConfig.getFilePath() + File.separator + tsrFolder;
//                    	}
//                    }	
//                } else {
//                	tsrFolder = null;
//                }
//            }               
//        } catch (Exception e) {
//            logger.error("ERROR in getSearchTSRFolder!", e);
//        } finally{
//            try{
//                ConnectionPool.getInstance().releaseConnection(conn);
//            }catch(BaseException e){
//                logger.error(e);
//            }           
//        }
//        
//        if (logger.isDebugEnabled())   
//        	logger.debug("DBManager getSearchTSRFolder# exiting...");
		if(tsrFolder != null) {
			//make it Unix or Windows like
			tsrFolder = tsrFolder.replace("\\", "/").replaceAll("/{2,}", "/").replace("/", File.separator);
			if(!tsrFolder.endsWith(File.separator)) {
				tsrFolder = tsrFolder + File.separator;
			}
		}
        return tsrFolder;
        
    }
    
    public static SearchToArchive[] getSearchesToArchive(long date){
                
        SearchToArchive allSearches[] = new SearchToArchive [0];
        if (logger.isDebugEnabled())   logger.debug("DBManager getSearchesToArchive# date : " + date);

        String stm = " select a.ID, ABSTR_FILENO, TSR_FILE_LINK from "+ TABLE_SEARCH +" a JOIN " + 
        	DBConstants.TABLE_SEARCH_FLAGS + " b ON a.ID = b." + 
        	DBConstants.FIELD_SEARCH_FLAGS_ID + " where " + 
        	DBConstants.FIELD_SEARCH_FLAGS_ARCHIVED + " = 0 and SDATE < ?";
                
        DBConnection conn = null;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            
            PreparedStatement ps = conn.prepareStatement(stm);
            ps.setDate(1, new java.sql.Date(date));
            DatabaseData data = conn.executePrepQuery(ps);
            allSearches = new SearchToArchive [data.getRowNumber()];
            if (logger.isDebugEnabled())   logger.debug("DBManager getSearchesToArchive# data size : " + data.getRowNumber());
            for (int i = 0; i<data.getRowNumber(); i++){
                allSearches[i] = new SearchToArchive();
                allSearches[i].setId(((BigDecimal)data.getValue(1,i)).intValue());
                allSearches[i].setTSRname((String)data.getValue(2,i));
                allSearches[i].setTSRlink((String)data.getValue(3,i));
            }               
            
        } catch (Exception e) {
            logger.error(e);
        }
        finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
        
        if (logger.isDebugEnabled())   logger.debug("DBManager getSearchesToArchive# exiting...");
        return allSearches;
    }

    public static void updateArchivedSearches(Vector<SearchToArchive> v){
        logger.debug("DBManager updateArchivedSearches# vector length: " + v.size());
        for (SearchToArchive archive : v) {
        	UpdateArchivedSearchedTransaction updateTransaction = new UpdateArchivedSearchedTransaction(archive);
            getTransactionTemplate().execute(updateTransaction);	
		}
                
        logger.debug("DBManager updateArchivedSearches# exiting...");
    }

    public static int getTSRGenerationStatus( long searchID )
    {
        String sqlQuery = "SELECT " + 
        	DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + " FROM "+ 
        	DBConstants.TABLE_SEARCH_FLAGS + " WHERE " +
        	DBConstants.FIELD_SEARCH_FLAGS_ID + " = ?" ;
        
        try {
        	return getSimpleTemplate().queryForInt(sqlQuery, searchID);
        } catch( Exception e ) {
            logger.error("Error while reading tsr_created flag for searchId = " + searchID, e);
        }

        return -1;
    }
    
    /**
     * Daca obiectul returnat este instanceOf UserAttributes => searchul e <b>checkat</b> 
     * de userul pentru care corespunde obiectul UserAttributes returnat
     * <br>
     * <br>
     * Daca se returneaza obiect instanceOf Boolean cu valoarea false 
     * => searchul <b>nu e available</b>, fiind checkat de <i>nu se stie cine :)))</i>
     * <br>
     * <br>
     * Daca se returneaza obiect instanceOf Boolean cu valoarea true 
     * => searchul e <b>available</b>.
     * 
     * @param search
     * @return Object
     */
    
    public static final int CHECK_OWNER = 0;
    public static final int CHECK_AVAILABLE = 1;
    public static final int CHECK_REUSE = 2;
    
    
    public static final int SEARCH_AVAILABLE                = 0;
    public static final int SEARCH_NOT_FOUND                = 1;
    public static final int SEARCH_LOCKED_BY_OTHER          = 2;
    public static final int SEARCH_WAS_DELETED              = 3;
    public static final int SEARCH_WAS_UNLOCKED             = 4;
    public static final int SEARCH_AUTOMATIC_IN_PROGRESS    = 5;
    public static final int SEARCH_LOCKED_BY_OTHER_REMOTE   = 7;
    public static final int SEARCH_WAS_CLOSED				= 8;
    public static final int SEARCH_LOCKED_EXTERNAL			= 9;
    
    public static class SearchAvailabitily {
        public int status = SEARCH_NOT_FOUND;
        public UserAttributes checkedBy = null;
        public boolean tsrInProgress = false;
        
        public SearchAvailabitily(int status, UserAttributes checkedBy, boolean tsrInProgress) {
            this.status = status;
            this.checkedBy = checkedBy;
            this.tsrInProgress = tsrInProgress;
        }
        
        public String getErrorMessage() {
            
            String errorMessage = null;
            
            if (tsrInProgress){
            	errorMessage = "TSR generation in progress.";
            }
            
            if (status == SEARCH_NOT_FOUND) {
                errorMessage = "Search not found.";
            } else if (status == SEARCH_LOCKED_BY_OTHER) {
                errorMessage = "This search has been locked by " + UserUtils.getUserFullName(checkedBy) + "!";
            } else if (status == SEARCH_WAS_DELETED) {
                errorMessage = "This search is no longer available.";
            } else if (status == SEARCH_WAS_UNLOCKED) {
                errorMessage = "It is not your search any more. It has been unlocked and can be taken by anybody!";
            } else if (status == SEARCH_AUTOMATIC_IN_PROGRESS) {
                errorMessage = "Automatic search in progress.";
            } else if (status == SEARCH_LOCKED_BY_OTHER_REMOTE) {
                errorMessage = "This search has been locked remotely on other server";
            } else if (status == SEARCH_WAS_CLOSED){
            	errorMessage = "This search is closed. It can only be unlock by an admin";
            }else if (status == SEARCH_LOCKED_EXTERNAL){
            	errorMessage = "This search is locked and waiting response from an outsource system (NextAce/TDI) !";
            }
            
            return errorMessage;
        }
    }

    public static SearchAvailabitily checkAvailability(long id, long currentUserId, int checkType, boolean ignoreClosed) {
    	return checkAvailability( id, currentUserId, checkType, false, ignoreClosed );
    }
    
    public static SearchAvailabitily checkAvailability(long id, long currentUserId, int checkType, boolean disableRemote, boolean ignoreClosed) {
        
        int status = SEARCH_NOT_FOUND;
        boolean tsrInProgress = false;
        UserAttributes checkedBy = null;
        
        if(DBManager.isLockedSearchExternal(id)){
        	 return new SearchAvailabitily(SEARCH_LOCKED_EXTERNAL, checkedBy, tsrInProgress);
        }
        
        FileLogger.info( "User id = " + currentUserId + " check availability for search id = " + id, FileLogger.SEARCH_OWNER_LOG );
        
        String sqlPhr = "SELECT " + 
        	DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + ", " + 
        	DBConstants.FIELD_SEARCH_ABSTRACT_ID + ", a." + 
        	DBConstants.FIELD_SEARCH_STATUS + ", " + 
        	DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " FROM "+ 
        	TABLE_SEARCH +" a JOIN " + 
        	DBConstants.TABLE_SEARCH_FLAGS + " b ON a." + 
        	DBConstants.FIELD_SEARCH_ID + " = b." + 
        	DBConstants.FIELD_SEARCH_FLAGS_ID + " WHERE a." + 
        	DBConstants.FIELD_SEARCH_ID + " = " + id;
        
        int checkedById = -1;
        int ownerId = -1;
        
        DBConnection conn = null;
        
        
      
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData data = conn.executeSQL(sqlPhr);
            
            if (data.getRowNumber() > 0) {
            	Integer statusObj = (Integer)data.getValue(3, 0);
            	if(statusObj!=null){
            		if(statusObj.intValue()==SEARCH_NOT_SAVED){
            			FileLogger.info( "Availability for Search id " + id + " SEARCH AVAILABLE ", FileLogger.SEARCH_OWNER_LOG );
                    	status = SEARCH_AVAILABLE;
                    	return new SearchAvailabitily(status, checkedBy,tsrInProgress);
            		} else if((Boolean)data.getValue(4, 0) && !ignoreClosed){
            			FileLogger.info( "Availability for Search id " + id + " SEARCH IS CLOSED ", FileLogger.SEARCH_OWNER_LOG );
            			status = SEARCH_WAS_CLOSED;
                    	return new SearchAvailabitily(status, checkedBy,tsrInProgress);
            		}
            		
            	} 
                
	                if (data.getValue("CHECKED_BY", 0) != null) {
	                    checkedById = Integer.parseInt(data.getValue("CHECKED_BY", 0).toString());
	                }
	                
	                if( checkedById > 1 )
	                {
	                    checkedById = 1;
	                }
	                
	                if (data.getValue("ABSTRACT_ID", 0) != null) {
	                    ownerId = Integer.parseInt(data.getValue("ABSTRACT_ID", 0).toString());
	                } 
	                
	                FileLogger.info( "Availability for Search id = " + id + "  owner = " + ownerId + " checkedBy = " + checkedById, FileLogger.SEARCH_OWNER_LOG );
	                
	                if (checkType == CHECK_AVAILABLE) { // vrea sa il deschida din rapoarte
	                    
	                    FileLogger.info( "OPEN FROM REPORTS: Availability for Search id = " + id + "  owner = " + ownerId + " checkedBy = " + checkedById, FileLogger.SEARCH_OWNER_LOG );
	                    
	                    if ( ( checkedById == 1 && ownerId == currentUserId ) || checkedById == 0)
	                    {// doar daca este deja luat de el sau nu apartine nimanui
	                        FileLogger.info( "Availability for Search id = " + id + "  owner = " + ownerId + " AVAILABLE ", FileLogger.SEARCH_OWNER_LOG );
	                        status = SEARCH_AVAILABLE;
	                        
	                        //search available on this server
	                        //check if available on other servers
	                        
	                        boolean reserved = false;
	                        boolean collisionDetected = false;
	                        int remoteSearchLockAttempt = 0;
	                        
	                        while( remoteSearchLockAttempt < NR_MAX_TRY_LOCK ){
	                        
	                        	//check availability in the database
	                        	if( !disableRemote ){
	                        		SearchAvailabitily searchAvail = getOpenAvailabilityRemote( id, currentUserId );
	                        		status = searchAvail.status;
	                        		
	                        		if( searchAvail.status !=  SEARCH_AVAILABLE ){
	                        			status = SEARCH_LOCKED_BY_OTHER_REMOTE;
	                        			
	                        			break;
	                        		}
	                        	}
	                        	
	                        	reserved = false;
	                        	collisionDetected = false;
	                        	
		                        synchronized( DistributedMutex.mutexLock ){
		                        	if( !SearchReserved.getInstance().isReserved( id ) ){
		                        		logger.info( "Remote Locking: locked local search " + id );
		                        		SearchReserved.getInstance().reserveSearch( id );
		                        		
		                        		reserved = true;
		                        	}
		                        	else{
		                        		if( !disableRemote ){
		                        			
			                        		collisionDetected = true;
			                        		
			                        		remoteSearchLockAttempt++;
			                        		
		                        		}
		                        	}
		                        }
		                        
		                        if( !disableRemote ){
		                        	if( reserved ){
				                        boolean availableOnOtherServers = DistributedMutex.remoteCheckAvailability( currentUserId, id );
				                        if( !availableOnOtherServers ){
				                        	status = SEARCH_LOCKED_BY_OTHER_REMOTE;
				                        	
				                        	collisionDetected = true;
				                        	
				                        	remoteSearchLockAttempt ++;
				                        }
				                        else{
				                        	break;
				                        }
		                        	}
		                        	
			                        if( collisionDetected ){
			                        	logger.info( "Remote Locking: detected collision for search " + id );
			                        	synchronized ( DistributedMutex.mutexLock ) {
			                        		logger.info( "Remote Locking: unlocked search " + id );
											SearchReserved.getInstance().clearReserved( id );
										}
			                        	
			                        	//back off
			                        	DistributedMutex.backOff(remoteSearchLockAttempt);
			                        }
		                        }
		                        else{
		                        	if( reserved ){
		                        		break;
		                        	}
		                        	else{
		                        		status = SEARCH_LOCKED_BY_OTHER_REMOTE;
		                        		break;
		                        	}
		                        }
	                        }
	                        //i can't get it in NR_MAX_TRY_LOCK 
	                        if(remoteSearchLockAttempt == NR_MAX_TRY_LOCK){
	                        	 status = SEARCH_AVAILABLE;
	                        }
	                    }
	                    else if (checkedById == -1)
	                    {// daca e in automatic search
	                        FileLogger.info( "Availability for Search id = " + id + "  owner = " + ownerId + " AUTOMATIC IN PROGRESS ", FileLogger.SEARCH_OWNER_LOG );
	                        status = SEARCH_AUTOMATIC_IN_PROGRESS;
	                    }
	                    else if (checkedById == -2)
	                    {// daca e in generare de TSR
	                        FileLogger.info( "Availability for Search id = " + id + "  owner = " + ownerId + " TSR GENERATION IN PROGRESS ", FileLogger.SEARCH_OWNER_LOG );
	                        status = SEARCH_AVAILABLE;
	                        tsrInProgress = true;
	                    }
	                    else { // e luat de altu
	                        status = SEARCH_LOCKED_BY_OTHER;
	                        FileLogger.info( "Search id = " + id + " locked by other " + ownerId, FileLogger.SEARCH_OWNER_LOG );
	                        checkedBy = UserUtils.getUserFromId(new BigDecimal(ownerId));
	                    }
	                    
	                } else if (checkType == CHECK_OWNER || checkType == CHECK_REUSE) { 
	                	// vrea sa salveze search-ul explicit sau implicit (din automatic, upload sau parentSite)
	                    
	                    FileLogger.info( "SAVE SEARCH AVAILABILITY: Availability for Search id = " + 
	                    		id + "  owner = " + ownerId + " checkedBy = " + checkedById, FileLogger.SEARCH_OWNER_LOG );
	                    
	                    // daca e luat de el sau se afla in operatia  AutomaticSearch
	                    if ( (( (checkedById == -1 || checkedById == 1) && currentUserId == ownerId )) || (checkedById == 0 && checkType == CHECK_REUSE && currentUserId == ownerId))
	                    {
	                        FileLogger.info( "Availability for Search id = " + id + "  owner = " + ownerId + " AVAILABLE ", FileLogger.SEARCH_OWNER_LOG );
	                        status = SEARCH_AVAILABLE;
	                    }
	                    else if (checkedById == -2){
	                    	//daca e in generare de TSR
	                        FileLogger.info( "Availability for Search id = " + id + "  owner = " + ownerId + " TSR GENERATION IN PROGRESS ", FileLogger.SEARCH_OWNER_LOG );
	                        status = SEARCH_AVAILABLE;
	                        tsrInProgress = true; 
	                    }
	                    else {
	                        if (checkedById == 0)
	                        {
	                        	if(checkType == CHECK_REUSE){
	                        		FileLogger.info( "Availability(CHECK REUSE) for Search id = " + id + "  owner = " + ownerId + " AVAILABLE ", FileLogger.SEARCH_OWNER_LOG );
	    	                        status = SEARCH_AVAILABLE;
	                        	} else {
		                            FileLogger.info( "Availability for Search id = " + id + "  owner = " + ownerId + " UNLOCKED ", FileLogger.SEARCH_OWNER_LOG );
		                            status = SEARCH_WAS_UNLOCKED;
	                        	}
	                        }
	                        else {
	                            FileLogger.info( "Availability for Search id = " + id + " LOCKED by  owner = " + ownerId, FileLogger.SEARCH_OWNER_LOG );
	                            status = SEARCH_LOCKED_BY_OTHER;
	                            checkedBy = UserUtils.getUserFromId(new BigDecimal(ownerId));
	                        }
	                    }
	                
            	}
            } else {
                // pentru cazul in care verificarea vine din rapoarte, si apare cazul in care dupa ce se da 
                // createTSR si rapoartele inca sunt pe ecran, contextul este sters din baza
                // iar cererea care vine pentru el o sa intoarca false.
                if (checkType == CHECK_AVAILABLE) {
                    FileLogger.info( "Availability for Search id " + id + " SEARCH WAS DELETED ", FileLogger.SEARCH_OWNER_LOG );
                    status = SEARCH_WAS_DELETED;
                } else {
                    // searchul este nou si nu este inca salvat in baza de date. (CHECK_OWNER)
                    FileLogger.info( "Availability for Search id " + id + " SEARCH AVAILABLE ", FileLogger.SEARCH_OWNER_LOG );
                    status = SEARCH_AVAILABLE;
                }
            }
            
        } catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
        } finally {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch(BaseException e) {
                logger.error(e);
            }           
        }        
        return new SearchAvailabitily(status, checkedBy, tsrInProgress);
    }
    
    
    private static final String SQL_GET_ABSTR_FILENO = "SELECT " + 
    		DBConstants.FIELD_SEARCH_ABSTRACT_FILENO + " FROM " + 
    		DBConstants.TABLE_SEARCH + " WHERE " + 
    		DBConstants.FIELD_SEARCH_ID + " = ?";
    public static String getSearchFileNo(long searchId) {
    	try {
			return DBManager.getSimpleTemplate().queryForObject(SQL_GET_ABSTR_FILENO, String.class, searchId);
		} catch (Exception e) {
			logger.error(e);
		}
        return null;
    }
    
    private static final String SQL_GET_ABSTR_FILENO_AND_SOURCEID = "SELECT t." + 
    		DBConstants.FIELD_SEARCH_ABSTRACT_FILENO  + ", s." +
    		DBConstants.TABLE_FIELD_SO_ORDER_ID + " FROM " + 
    		DBConstants.TABLE_SEARCH  + " t LEFT JOIN " +
    		DBConstants.TABLE_SEARCH_EXTERNAL_FLAGS  + " s ON t." +
    		DBConstants.FIELD_SEARCH_ID + " = s." +
    		DBConstants.TABLE_FIELD_SEARCH_ID + " WHERE t." + 
    		DBConstants.FIELD_SEARCH_ID + " = ?";
    public static String[] getSearchFileNoAndSourceID(long searchId) {
    	String[] result = new String[2];
    	try {
			List<Map<String, Object>> list = getSimpleTemplate().queryForList(SQL_GET_ABSTR_FILENO_AND_SOURCEID, searchId);
			if (list.size()==1) {
				Map <String, Object> map = list.get(0);
				result[0] = (String)map.get(DBConstants.FIELD_SEARCH_ABSTRACT_FILENO);
				result[1] = (String)map.get(DBConstants.TABLE_FIELD_SO_ORDER_ID);
			}
		} catch (Exception e) {
			logger.error(e);
		}
        return result;
    }
    
    public static SearchData[] getSearches(int state) {
        
        SearchData allSearches[] = new SearchData[0];
        
        String stm = " select a.ID, ABSTRACT_ID from " + TABLE_SEARCH + " a JOIN " +
        	DBConstants.TABLE_SEARCH_FLAGS + " b ON a.ID = b." + 
        	DBConstants.FIELD_SEARCH_FLAGS_ID +" where CHECKED_BY = ?";
        	
		try {
			List<SearchData> result = null;
			result = DBManager.getSimpleTemplate().query(stm, new SearchData(),state);
			allSearches = new SearchData [result.size()];
			allSearches = result.toArray(allSearches);
			} catch (Exception e) {
	        	e.printStackTrace();
	            logger.error(e);
			}
        
        if (logger.isDebugEnabled())   
            logger.debug("DBManager getSearchesToArchive# exiting...");
        
        return allSearches;
    }
    
    public static String getSearchNote(long searchDBId, HashMap<String, String> hm) {

        String sql = "SELECT NOTE_CLOB, NOTE_STATUS FROM "+ TABLE_SEARCH +" WHERE ID = " + searchDBId;
        
        DBConnection conn = null;
        
        try {
        
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData data = conn.executeSQL(sql);
            
            if (data.getRowNumber() > 0 && data.getValue("NOTE_CLOB", 0) != null && data.getValue("NOTE_STATUS", 0) != null) {

                try {
                    
                    String note = (String) data.getValue("NOTE_CLOB", 0);
                    hm.put("NOTE_STATUS", data.getValue("NOTE_STATUS", 0).toString());
                    //ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    //IOUtil.copy(clob.getAsciiStream(), baos);
                    
                    //return new String(baos.toByteArray());
                    return note;
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } 
            
        } catch (NumberFormatException e) {
            logger.error(e);
        } catch (BaseException be) {
            logger.error(be);
        } finally {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch(BaseException e) {
                logger.error(e);
            }           
        }
        
        return "";
    }
     
    /**
     * Get search due date
     * @param searchDBId
     * @return
     */
    public static Date getSearchDueDate(long searchDBId){
    	try {
	    	Date date = getSimpleTemplate().queryForObject("SELECT due_date FROM ts_search WHERE id= ?", Date.class, searchDBId);	    	
	    	return date;
    	} catch (EmptyResultDataAccessException erdae){
    		logger.error("No Due Date for search: " + searchDBId);
    	} catch(Exception e){
    		System.err.println("Don't worry!!!");
    		logger.error(e);
    		e.printStackTrace();
    		System.err.println("Don't worry!!!");
    	}    	
    	return null;
    }
    
    // MM/dd/yyy
    public static void setSearchDueDate(long searchDBId, String dueDate){
    	Date date = null;
    	try{
    		date = new SimpleDateFormat("MMM dd,yyyy").parse(dueDate);   
    	} catch (ParseException e){
    		try {
    			date = new SimpleDateFormat("MM/dd/yyyy").parse(dueDate);
    		} catch(ParseException e1){
    			logger.error(e1);
    			e1.printStackTrace();    		
    		}
    	}
    	try {
    		date.setHours(13);
    		String mysqlDate = new FormatDate(FormatDate.TIMESTAMP).getDate(date/*.getTime() + 5*60*60*100)*/);
            String sDate = "str_to_date( '" + mysqlDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
    		getSimpleTemplate().update("UPDATE ts_search SET due_date=" + sDate + "WHERE id=" + searchDBId);    		

    	} catch(RuntimeException e){
    		logger.error(e);
    		e.printStackTrace();
    	}
    }
    
    public static void setSearchNote(long searchDBId, String note, String noteStatus) {
        
        String sql = "UPDATE "+ TABLE_SEARCH +" SET NOTE_CLOB= ? , NOTE_STATUS= ? WHERE ID = ?";
                
		try {
			DBManager.getSimpleTemplate().update(sql,note,noteStatus,searchDBId);
		} catch (Exception e) {
			 logger.error(e);
		}
    }
    
    public static int addSubdivision(final int county, final String subdivision) {
        		
    	Object result = getTransactionTemplate().execute(new TransactionCallback() {
   		 public Object doInTransaction(TransactionStatus status) {
		    int count = 0;
   		    try{
   		        String sqlInsert = "INSERT INTO "+ TABLE_SUBDIVISIONS +" (name, county) VALUES (?,?)";
   		        String sqlSelect = "SELECT count(*) FROM "+ TABLE_SUBDIVISIONS +" WHERE name= ? and county= ? ";

   				count = DBManager.getSimpleTemplate().queryForInt(sqlSelect,subdivision,county);
   				
   				if(count==0)
   					DBManager.getSimpleTemplate().update(sqlInsert,subdivision,county);
   	        	
   		    }catch(RuntimeException e){ 
   		    	// failure - rollback
   		    	logger.error(e);
   		    	status.setRollbackOnly();    		    	
   		    }
   		    
   		    return count;
   	    }
   	});
    	
        return (Integer)result;
    }
    
    /**
     * Insert an Macomb subdiv data object
     */
    public static void addMacombSubdivision(Subdivisions obj){
    	
    	String sqlInsert   = "INSERT INTO "+TABLE_SUBDIVISIONS_MACOMB + " ( " +
    						FIELD_SUBDIVISIONS_MACOMB_NAME +", " + FIELD_SUBDIVISIONS_MACOMB_CODE +
    						", " + FIELD_SUBDIVISIONS_MACOMB_AREA + ", " + FIELD_SUBDIVISIONS_MACOMB_PHASE +
    						", " + FIELD_SUBDIVISIONS_MACOMB_TYPEID + " ) " +
    						 " VALUES (?, ?, ?, ?, ?)";
   	
		try {
			DBManager.getSimpleTemplate().update(sqlInsert,obj.getName(),obj.getCode(),obj.getArea(),obj.getPhase(),obj.getTypeId());
		} catch (Exception e) {
			logger.error(e);
		}
		
    }
    
    /**
     * Insert an OaklandSubdivision data object
     * @param OaklandSubdivisions obj
     */
    public static void addOaklandSubdivision(OaklandSubdivisions obj){
    	
    	String sqlInsert   = "INSERT INTO "+TABLE_SUBDIVISIONS_OAKLAND+
    						 		" VALUES (?, ?, ?, ?, ?)";
		try {
			DBManager.getSimpleTemplate().update(sqlInsert,obj.getId(),obj.getCode(),obj.getArea(),obj.getName(),obj.getTypeId());
		} catch (Exception e) {
			 logger.error(e);
		}
    }
    
    
    public static void deleteSubdivisions( int county )
    {
        DBConnection conn = null;
               
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            if( county != SubdivisionMatcher.TN_HAMILTON ){
            	conn.executeSQL( "delete from "+ TABLE_SUBDIVISIONS +" where county = " + county );
            }
            else{
            	conn.executeSQL( "delete from "+ TABLE_SUBDIVISIONS_HAMILTON);
            }
            conn.commit();
            
        } catch (Exception e) {
            logger.error(e);
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(Exception e){
                logger.error(e);
            }           
        }
    }
   
    /*
     * Deletes Macomb subdivisions
     */ 
     public static void deleteMacombSubdivisions()
     {
         DBConnection conn = null;
                
         try {
             
             conn = ConnectionPool.getInstance().requestConnection();
             conn.executeSQL( "delete from "+TABLE_SUBDIVISIONS_OAKLAND);
             conn.commit();
             
         } catch (Exception e) {
             logger.error(e);
         } finally{
             try{
                 ConnectionPool.getInstance().releaseConnection(conn);
             }catch(Exception e){
                 logger.error(e);
             }           
         }
     } 
     
     /*
      * Deletes Kane subdivisions
      */ 
      public static void deleteKaneSubdivisions()
      {
          DBConnection conn = null;
                 
          try {
              
              conn = ConnectionPool.getInstance().requestConnection();
              conn.executeSQL( "delete from " + TABLE_SUBDIVISIONS_IL_KANE);
              conn.commit();
              
          } catch (Exception e) {
              logger.error(e);
          } finally{
              try{
                  ConnectionPool.getInstance().releaseConnection(conn);
              }catch(Exception e){
                  logger.error(e);
              }           
          }
      } 
    
   /*
    * Deletes Oakland subdivisions
    */ 
    public static void deleteOaklandSubdivisions()
    {
        DBConnection conn = null;
               
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            conn.executeSQL( "delete from "+TABLE_SUBDIVISIONS_OAKLAND);
            conn.commit();
            
        } catch (Exception e) {
            logger.error(e);
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(Exception e){
                logger.error(e);
            }           
        }
    } 
    
    
    public static String[] getSubdivisions(int county){
        
        DBConnection conn = null;
        String allSubdivisions[] = new String[0]; 
        
        String stm = " select NAME from "+ TABLE_SUBDIVISIONS +" where county = " + county;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData data = conn.executeSQL(stm);
            
            allSubdivisions = new String [data.getRowNumber()];
            
            for (int i = 0; i < data.getRowNumber(); i++){
                Object obj = data.getValue(1,i);
                if (obj != null)
                    allSubdivisions[i] = (String) obj;
            }               
            
        } catch (BaseException e) {
            logger.error(e);
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
        
        return allSubdivisions;
    }
    
    public static Subdivisions[] getMacombSubdivisions(String name, int Type) {
    	
    	Subdivisions[] macombSub = new Subdivisions[0];
   	 
    	String stm = "SELECT "+FIELD_SUBDIVISIONS_MACOMB_ID +
   	 			  ","+FIELD_SUBDIVISIONS_MACOMB_CODE +
   	 			  ","+FIELD_SUBDIVISIONS_MACOMB_AREA +
   	 			  ","+FIELD_SUBDIVISIONS_MACOMB_TYPEID +
   	 			  ","+FIELD_SUBDIVISIONS_MACOMB_NAME +
   	 			  ","+FIELD_SUBDIVISIONS_MACOMB_PHASE +   	 			  
   	 			  " FROM "+TABLE_SUBDIVISIONS_MACOMB+" o " +
   	 			  " WHERE "+ FIELD_SUBDIVISIONS_OAKLAND_NAME+" LIKE ?" +
   		 		  " AND "+FIELD_SUBDIVISIONS_OAKLAND_TYPEID + "= ? " +
   		 		  " ORDER BY "+FIELD_SUBDIVISIONS_OAKLAND_NAME+" ASC";
   	 	logger.info(stm);
   	 		
		try {
			List<Subdivisions> result = DBManager.getSimpleTemplate().query(stm, new Subdivisions(),"%"+name + "%",Type);
			macombSub = new Subdivisions [result.size()];
			macombSub = result.toArray(macombSub);
		} catch (Exception e) {
			logger.error(e);
		}
			
		return macombSub;
    }
    
    /**
     * Returns all IL Kane subdivisions info
     * @param String name
     * @return ILKaneSubdivisions[] data object
     */
    
    public static ILKaneSubdivisions[] getILKaneSubdivisions(String name, int Type) {
    	
    	ILKaneSubdivisions[] kaneSub = new ILKaneSubdivisions[0];
   	 
    	String stm = "SELECT "+FIELD_SUBDIVISIONS_IL_KANE_CODE +
   	 			  ","+FIELD_SUBDIVISIONS_IL_KANE_NAME +
   	 			  ","+FIELD_SUBDIVISIONS_IL_KANE_PLAT_DOC +	 			  
   	 			  " FROM "+TABLE_SUBDIVISIONS_IL_KANE+" o " +
   	 			  " WHERE "+ FIELD_SUBDIVISIONS_IL_KANE_CODE+" LIKE ?" +
   		 		  " ORDER BY "+FIELD_SUBDIVISIONS_IL_KANE_NAME+" ASC";
   	 	logger.info(stm);
   	 		
		try {
			List<ILKaneSubdivisions> result = DBManager.getSimpleTemplate().query(stm, new ILKaneSubdivisions(),"%"+name + "%");
			kaneSub = new ILKaneSubdivisions [result.size()];
			kaneSub = result.toArray(kaneSub);
		} catch (Exception e) {
			logger.error(e);
		}
			
		return kaneSub;
    }
    
    /**
     * Returns all IL Kane subdivisions info
     * @param String name
     * @return ILKaneSubdivisions[] data object
     */
    
    public static String getILKanePlatDocBySubCode(String subdivCode) {
    	
    	String platDoc = "";
    	ILKaneSubdivisions kaneSub = new ILKaneSubdivisions();
   	 
    	String stm = "SELECT " +FIELD_SUBDIVISIONS_IL_KANE_CODE +
				  	  ","+FIELD_SUBDIVISIONS_IL_KANE_NAME +
		 			  ","+FIELD_SUBDIVISIONS_IL_KANE_PLAT_DOC +	 	 			  
	   	 			  " FROM "+TABLE_SUBDIVISIONS_IL_KANE+" o " +
	   	 			  " WHERE "+ FIELD_SUBDIVISIONS_IL_KANE_CODE+" = ?";
   	 	logger.info(stm);
   	 		
		try {
			List<ILKaneSubdivisions> result = DBManager.getSimpleTemplate().query(stm, new ILKaneSubdivisions(), subdivCode);
			if (result.size() == 1) {
				platDoc = result.get(0).getPlatDoc();
			}
			if (platDoc == null){
				platDoc = "";
			}
		} catch (Exception e) {
			logger.error(e);
		}
			
		return platDoc;
    }
    
    /**
     * Returns all oakland subdivisions info
     * @param String name
     * @return OaklandSubdivisions[] data object
     */
    public static OaklandSubdivisions[] getOaklandSubdivisions(String name, int Type) {
    	    	
    	OaklandSubdivisions[] oakSub = new OaklandSubdivisions[0];
	 
    	String stm = "SELECT o."+FIELD_SUBDIVISIONS_OAKLAND_ID +
			",o."+FIELD_SUBDIVISIONS_OAKLAND_CODE +
			",o."+FIELD_SUBDIVISIONS_OAKLAND_AREA +
			",o."+FIELD_SUBDIVISIONS_OAKLAND_TYPEID +
			",o."+FIELD_SUBDIVISIONS_OAKLAND_NAME + 
			" FROM "+TABLE_SUBDIVISIONS_OAKLAND+" o " +
			" JOIN "+TABLE_SUBDIVISIONS_OAKLAND_TYPE +
			" ot ON o."+FIELD_SUBDIVISIONS_OAKLAND_TYPEID + "=ot." +FIELD_SUBDIVISIONS_OAKLAND_TYPE_ID +
			" AND o."+FIELD_SUBDIVISIONS_OAKLAND_NAME+" LIKE ?" +
			" AND o."+FIELD_SUBDIVISIONS_OAKLAND_TYPEID + "= ? " +
			" ORDER BY "+FIELD_SUBDIVISIONS_OAKLAND_NAME+" ASC";
 
    	logger.info(stm);
    	 	
    	try {
    		List<OaklandSubdivisions> result = DBManager.getSimpleTemplate().query(stm, new OaklandSubdivisions(),"%"+name + "%",Type);
    		oakSub = new OaklandSubdivisions [result.size()];
    		oakSub = result.toArray(oakSub);
    	} catch (Exception e) {
    		logger.error(e);
    	}
			    
    	return oakSub;
    }
    
    public static String updateSearchLegalDescription(Search search) {

        String sql = "SELECT " + 
        	DBConstants.FIELD_SEARCH_DATA_BLOB_LEGAL_DESCR + ", " + 
        	DBConstants.FIELD_SEARCH_FLAGS_LEGAL_DESCR_STATUS + " FROM "+ 
        	DBConstants.TABLE_SEARCH_FLAGS +" a JOIN " + 
        	DBConstants.TABLE_SEARCH_DATA_BLOB + " b ON a." + 
        	DBConstants.FIELD_SEARCH_FLAGS_ID + " = b. " + 
        	DBConstants.FIELD_SEARCH_DATA_BLOB_ID + " WHERE a." + 
        	DBConstants.FIELD_SEARCH_FLAGS_ID + " = " + search.getID();
        
        DBConnection conn = null;
        
        try {
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData data = conn.executeSQL(sql);
            
            if (data.getRowNumber() > 0) {

                String legalDescription = "";
                if (data.getValue("LEGAL_DESCRIPTION", 0) != null) {
                    legalDescription = (String)data.getValue("LEGAL_DESCRIPTION", 0);
                }
                
                String legalDescriptionStatus = "0";
                if (data.getValue("LEGAL_DESCRIPTION_STATUS", 0) != null)
                    legalDescriptionStatus = data.getValue("LEGAL_DESCRIPTION_STATUS", 0).toString();
                
                search.getSa().setAtribute(SearchAttributes.LEGAL_DESCRIPTION, legalDescription);
                search.getSa().setAtribute(SearchAttributes.LEGAL_DESCRIPTION_STATUS, legalDescriptionStatus);
            } 
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch(BaseException e) {
                logger.error(e);
            }           
        }
        
        return "";
    }
    
    public static void setSearchLegalDescription(Search search, String legalDescription, int legalDescriptionStatus) {
        
        String sql = "UPDATE "+ 
        	DBConstants.TABLE_SEARCH_FLAGS + " SET LEGAL_DESCRIPTION_STATUS = " + legalDescriptionStatus + " WHERE " + 
        	DBConstants.FIELD_SEARCH_FLAGS_ID + " = " + search.getID();
        
        DBConnection conn = null;
        
        try {
                    	
            legalDescription=legalDescription.replaceAll("" + ((char) 186),"&#176;"); //replace '�'
            legalDescription=legalDescription.replaceAll("" + ((char) 176),"&#176;"); // replace '�'
            legalDescription=legalDescription.replaceAll("[" + ((char) 147) + ((char) 148) + "]","\""); //replace '��'
            
            char c = 0x092;
            legalDescription = legalDescription.replace(c, '\'');
            c=147;
            legalDescription = legalDescription.replaceAll(c+"", "\"");
            c=148;
            legalDescription = legalDescription.replaceAll(c+"", "\"");
            
            c=145;
            legalDescription = legalDescription.replaceAll(c+"", "'");
            
            c=146;
            legalDescription = legalDescription.replaceAll(c+"", "'");
            
            conn = ConnectionPool.getInstance().requestConnection();
            
            conn.executeSQL(sql);
            conn.commit();
            
            conn.setAutoCommit(false);
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT LEGAL_DESCRIPTION FROM "+ 
            		DBConstants.TABLE_SEARCH_DATA_BLOB +" WHERE " + 
            		DBConstants.FIELD_SEARCH_DATA_BLOB_ID + " = " + search.getID() + " FOR UPDATE");

            if (rs.next()) {
                String stm = "UPDATE "+ 
                		DBConstants.TABLE_SEARCH_DATA_BLOB +" SET LEGAL_DESCRIPTION = ? WHERE " + 
                		DBConstants.FIELD_SEARCH_DATA_BLOB_ID + " = " + search.getID();
                
        		PreparedStatement pstmt = conn.prepareStatement( stm );
        		pstmt.setString( 1, legalDescription);
        		pstmt.executeUpdate();
        		pstmt.close();
            }
            
            rs.close();
            stmt.close();
            
            conn.commit();
            
            search.getSa().setAtribute(SearchAttributes.LEGAL_DESCRIPTION, legalDescription);
            search.getSa().setAtribute(SearchAttributes.LEGAL_DESCRIPTION_STATUS, String.valueOf(legalDescriptionStatus));

        } catch (Exception e) {
            logger.error(e);
        } finally {
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
    }
    
    @SuppressWarnings("unchecked")
	public static Hashtable getTestSuiteData( )
    {
        DBConnection conn = null;
        
        Hashtable testSuiteData = new Hashtable();
        
        String stm = " select * FROM "+ TABLE_TESTSUITE ;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData data = conn.executeSQL(stm);
            
            if( data.getRowNumber() > 0 )
            {
                for( int i = 0 ; i < data.getRowNumber() ; i ++ )
                {
                    BigDecimal testSuiteId = new BigDecimal(data.getValue( 1, i ).toString());
                    String testSuiteName = (String) data.getValue( 2, i );
                    String testSuiteDescription = (String) data.getValue( 3, i );
                    
                    TestSuite testSuite = new TestSuite( testSuiteName, testSuiteDescription, true );
                    testSuite.setId( testSuiteId.longValue() );
                    testSuiteData.put( testSuite.getName(), testSuite );
                }
            }
            
        } catch (BaseException e) {
            logger.error(e);
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
        
        return testSuiteData;
    }
    
    public static Hashtable getTestCaseSuites( )
    {
        
        DBConnection conn = null;
        Hashtable allTestSuites = getTestSuiteData();
        String stm = " select * FROM "+ TABLE_TESTCASE;
        AutomaticTesterManager ATM = AutomaticTesterManager.getInstance();
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData data = conn.executeSQL(stm);
            
            if( data.getRowNumber() > 0 )
            {
                for( int i = 0 ; i < data.getRowNumber() ; i ++ )
                {
                    //BigDecimal testCaseId = new BigDecimal(data.getValue( 1, i ).toString());
                    String testCaseFileName = (String) data.getValue( 2, i );
                    Long testSuiteId = (Long) data.getValue( 3, i );
                    Integer enabled = (Integer) data.getValue( 4,i );
                    
                    if( testSuiteId != null )
                    {
                        //if the entry is a part of a test suite, search for the AutomaticSearchJob object
                        AutomaticSearchJob ASJ = ATM.getJob( testCaseFileName );
                        
                        if( ASJ != null )
                        {
                            //we have a job
                            
                            if( enabled.intValue() == 0 )
                            {
                                ASJ.stop();
                            }
                            else
                            {
                                ASJ.start();
                            }
                            
                            Enumeration allTestSuiteKeys = allTestSuites.keys();
                            while( allTestSuiteKeys.hasMoreElements() )
                            {
                                String testSuiteName = (String) allTestSuiteKeys.nextElement();
                                TestSuite tempTestSuite = (TestSuite) allTestSuites.get( testSuiteName );
                                
                                if( testSuiteId.longValue() == tempTestSuite.getId() )
                                {
                                    tempTestSuite.addJob( ASJ );
                                }
                            }
                        }
                    }
                    
                }
            }
            else
            {
                //no data found, init the DB using AutomaticTesterManager
                Vector testJobs = ATM.getJobs();
                
                
                
                for( int i = 0 ; i < testJobs.size() ; i ++ )
                {
                    AutomaticSearchJob ASJ = (AutomaticSearchJob) testJobs.elementAt( i );
                    
                    stm = "INSERT INTO "+ TABLE_TESTCASE +" ( FILENAME, ENABLED ) VALUES( ?, " + (ASJ.isActive() ? 1 : 0) + " )";
            		
                    PreparedStatement pstmt = conn.prepareStatement( stm );
            		pstmt.setString( 1, ASJ.getXMLFile());
            		pstmt.executeUpdate();
            		pstmt.close();
                }
            }
            
        } catch (Exception e) {
            logger.error(e);
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
        
        return allTestSuites;
    }
    
    public static long newTestSuite( String name, String description )
    {
        DBConnection conn = null;
        
        long testSuiteId = 0;
        
        String stm = "INSERT INTO "+ TABLE_TESTSUITE +" ( TESTSUITE_NAME, TESTSUITE_DESCRIPTION ) VALUES ( ?, ? )";

        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
    		
            PreparedStatement pstmt = conn.prepareStatement( stm );
    		pstmt.setString( 1, name);
    		pstmt.setString( 2, description);
    		pstmt.executeUpdate();         
    		pstmt.close();
    		
            testSuiteId = getLastId(conn);
            
        } catch (Exception e) {
            logger.error(e);
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
        
        return testSuiteId;
    }
    
    public static void setTestSuiteJob( String jobFilename, long testSuiteId )
    {        
        String stm = "UPDATE "+ TABLE_TESTCASE +" SET TESTSUITE_ID = " + (testSuiteId >= 0 ? testSuiteId + "" : "null") + "  WHERE FILENAME = ? ";

        
		try {
			DBManager.getSimpleTemplate().update(stm,jobFilename);
		} catch (Exception e) {
			 logger.error(e);
		}

    }
    
    public static void insertTestcase( AutomaticSearchJob ASJ )
    {
        DBConnection conn = null;

        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            String stm =  "SELECT * FROM "+ TABLE_TESTCASE +" WHERE FILENAME =  ?";
                       
            PreparedStatement pstmt = conn.prepareStatement( stm );
            pstmt.setString( 1, ASJ.getXMLFile());
            DatabaseData data = conn.executePrepQuery(pstmt);
            pstmt.close();
            
            if( data.getRowNumber() == 0 )
            {
                //insert a new entry if it does not exist
            	stm = "INSERT INTO "+ TABLE_TESTCASE +" ( FILENAME, ENABLED ) VALUES( ?, " + (ASJ.isActive() ? 1 : 0) + " )";
        		
            	PreparedStatement pstmt1 = conn.prepareStatement( stm );
        		pstmt1.setString( 1, ASJ.getXMLFile());
        		pstmt1.executeUpdate();
        		pstmt1.close();

            }
            else
            {
                //already found an entry
                
                stm = "UPDATE "+ TABLE_TESTCASE +" SET TESTSUITE_ID = null WHERE FILENAME = ? ";
            	
                PreparedStatement pstmt1 = conn.prepareStatement( stm );
        		pstmt1.setString( 1, ASJ.getXMLFile());
        		pstmt1.executeUpdate();
        		pstmt1.close();
            }
            
        } catch (Exception e) {
            logger.error(e);
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
    }
    
    public static void deleteTestCase( AutomaticSearchJob ASJ )
    {
        String stm = "DELETE FROM "+ TABLE_TESTCASE +" WHERE FILENAME = ? ";
        
        try {
			DBManager.getSimpleTemplate().update(stm,ASJ.getXMLFile());
		} catch (Exception e) {
			 logger.error(e);
		}
    }
    
    public static void deleteTestSuite( TestSuite testSuite )
    {
        DBConnection conn = null;

        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            conn.executeSQL( "DELETE FROM "+ TABLE_TESTSUITE +" WHERE TESTSUITE_ID = " + testSuite.getId() );
            
            conn.executeSQL( "UPDATE "+ TABLE_TESTCASE +" SET TESTSUITE_ID = null WHERE TESTSUITE_ID = " + testSuite.getId() );
            
        } catch (BaseException e) {
            logger.error(e);
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
    }
    
    /**
     * 
     * @param searchId - the id of the search for which we need to find the fee
     * @param isTSAdmin - the fee will be Community to Agent for Commadmin (isTSAdmin = false) or ATS to Community for TSAdmin (isTSAdmin = true)
     * @return the fee for the specified search or 0 if no search is found in the database
     */
    public static double getSearchFee( long searchId, int productType,  boolean isTSAdmin )
    {
    	
    	DBConnection conn = null;
    	String sql = "call getSearchFeeType(?,?,?)";
    	
    	try {
    		
    		conn = ConnectionPool.getInstance().requestConnection();    		
    		NewProxyCallableStatement call = (NewProxyCallableStatement)conn.prepareCall(sql);
    		call.setLong(1, searchId);
    		call.setInt(2, productType);
    		call.setInt(3, isTSAdmin?1:0);
    		
			DatabaseData data = conn.executeCallableStatementWithResult(call);
    		
            if( data.getRowNumber() > 0 )
            {
                return Double.parseDouble(data.getValue(1, 0).toString());
            }
			
    	} catch (Exception e){
			e.printStackTrace();
		}	finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
		 return 0;
    }
    
     
    /**
     * Computes the searchfee (the discount is also applied)
     * @param data database data where we have everything we need (hopefully)
     * @param i the possition in the database data
     * @param ua the user that needs the fee computed
     * @return the searchFee
     */
    public static double getSearchFee(DatabaseData data, int i, boolean isTSAdmin) {
    	try {
    	
	    	Integer productType = Integer.valueOf(data.getValue("PRODID", i).toString());
	    	Float rateField = (Float)data.getValue("RATEFIELD", i);
	    	Float discountRatio = (Float)data.getValue("DISCOUNTRATIO", i);
	    	long invoicedCode = (Long)data.getValue("invoicedField",i);
	    	int sourceCreationType = (Integer) data.getValue("sourceCreationType", i);
	    	boolean isClosedSearch = (Boolean)data.getValue(DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED, i);
	    	if(invoicedCode != InvoicedSearch.SEARCH_NOT_INVOICED) {
	    		if(DBSearch.getCreationSourceTypeFromDatabaseStatus(sourceCreationType).equals(CREATION_SOURCE_TYPES.REOPENED)) {
	    			return ATSDecimalNumberFormat.formats(0);
	    		}
	    		if(isClosedSearch) {
	    			return ATSDecimalNumberFormat.formats(0);
	    		}
	    	}
	    	
	    	if(rateField==null) {
	    		rateField = 1f;
	    	}
	    	if(discountRatio==null){
	    		discountRatio = 1f; 
	    		logger.debug("discountRatio is null, using no discount");
	    	}
	    	if(!isTSAdmin){
	    		discountRatio = 1f;	//using no discount if not tsadmin
	    	}
	    	Float productValue = null;
	    	Float indexValue = null;
	    	
	    	boolean isFinished = (Long)data.getValue("tsr_created",i) == 1;
            indexValue = (Float)data.getValue("INDEXVALUE", i);
	    	
	    	if( productType == Products.FULL_SEARCH_PRODUCT )
	        {
	            productValue = (Float)data.getValue("FULLVALUE", i);
	        }
	        else if( productType == Products.UPDATE_PRODUCT )
	        {
	        	 productValue = (Float)data.getValue("UPDATEVALUE", i);
	        }
	        else if( productType == Products.REFINANCE_PRODUCT )
	        {
	        	 productValue = (Float)data.getValue("REFVALUE", i);
	        }
	        else if( productType == Products.CURRENT_OWNER_PRODUCT )
	        {
	        	 productValue = (Float)data.getValue("CRTOVALUE", i);
	        }
	        else if( productType == Products.CONSTRUCTION_PRODUCT )
	        {
	        	 productValue = (Float)data.getValue("CSTRVALUE", i);
	        }
	        else if( productType == Products.COMMERCIAL_PRODUCT)
	        {
	        	 productValue = (Float)data.getValue("COMMVALUE", i);
	        }
	        else if( productType == Products.OE_PRODUCT)
	        {
	        	 productValue = (Float)data.getValue("OEVALUE", i);
	        }
	        else if( productType == Products.LIENS_PRODUCT)
	        {
	        	 productValue = (Float)data.getValue("LIENVALUE", i);
	        }
	        else if( productType == Products.ACREAGE_PRODUCT)
	        {
	        	 productValue = (Float)data.getValue("ACRVALUE", i);
	        }
	        else if( productType == Products.SUBLOT_PRODUCT)
	        {
	        	 productValue = (Float)data.getValue("SUBVALUE", i);
	        }
	        else if( productType == Products.FVS_PRODUCT)
	        {
	        	 productValue = (Float)data.getValue("FVSVALUE", i);
	        }
	    	if(productValue==null){		//if we cannot calculate the fee will leave it unset
	    		System.err.println("naspa");
	    		return -1;
	    	}
	    	
	    	
	    	if(isFinished) {
	    		if(invoicedCode == InvoicedSearch.SEARCH_INVOICED_INDEX ||
	    				invoicedCode == InvoicedSearch.SEARCH_INVOICED_INDEX_AND_FINISHED) {
	    			if(indexValue == null) {
			    		indexValue = 0.5f;
			    	}
	    			
	    			return ATSDecimalNumberFormat.formats((1 - indexValue) * productValue * rateField * discountRatio);
	    			 
	    		} else {
	    			return ATSDecimalNumberFormat.formats(productValue * rateField * discountRatio);
	    		}
	    			
	    	} else {
	    		if(indexValue == null) {
		    		indexValue = 0.5f;
		    	}
	    		return ATSDecimalNumberFormat.formats(indexValue * productValue * rateField * discountRatio);

	    	}
	    } catch (Exception e){
    		logger.debug("Not setting searchFee for now... it will be set later individually");
    		logger.debug(e.getMessage());
    		e.printStackTrace();
    	}
		return -1;
	}
    
    public static double getSearchFee(Map source, boolean isTSAdmin, boolean substractIndexValue) {
    	try {
        	
	    	int productType = ((Long)source.get(TableReportProcedure.FIELD_PROD_ID)).intValue();
	    	Float rateField = (Float)source.get(TableReportProcedure.FIELD_RATE_ID);
	    	Float discountRatio = (Float)source.get(TableReportProcedure.FIELD_DISCOUNT);
	    	long invoicedCode = (Long)source.get(TableReportProcedure.FIELD_INVOICED_FIELD); 
	    	int sourceCreationType = (Integer) source.get(TableReportProcedure.FIELD_SOURCE_CREATION_TYPE);
	    	Object isCloseObject = source.get(TableReportProcedure.FIELD_IS_CLOSED);
	    	boolean isClosedSearch = false;
	    	if(isCloseObject instanceof Integer) {
	    		isClosedSearch = (Integer)isCloseObject==1;
	    	} else if (isCloseObject instanceof Boolean) {
	    		isClosedSearch = (Boolean)isCloseObject;
	    	} else {
	    		throw new RuntimeException("Could not determine correct object type for isClosedSearch flag");
	    	}
	    	boolean wasOpened = (Integer)source.get(TableReportProcedure.FIELD_WAS_OPENED)==1;
	    	if(!wasOpened) {
	    		return ATSDecimalNumberFormat.formats(0);
	    	}
	    	if(invoicedCode != InvoicedSearch.SEARCH_NOT_INVOICED) {
	    		if(DBSearch.getCreationSourceTypeFromDatabaseStatus(sourceCreationType).equals(CREATION_SOURCE_TYPES.REOPENED)) {
	    			return ATSDecimalNumberFormat.formats(0);
	    		}
	    		if(isClosedSearch) {
	    			return ATSDecimalNumberFormat.formats(0);
	    		}
	    	}
	    	
	    	if(rateField==null) {
	    		rateField = 1f;
	    	}
	    	if(discountRatio==null){
	    		discountRatio = 1f; 
	    		logger.debug("discountRatio is null, using no discount");
	    	}
	    	if(!isTSAdmin){
	    		discountRatio = 1f;	//using no discount if not tsadmin
	    	}
	    	Float productValue = null;
	    	Float indexValue = null;
	    	
	    	boolean isFinished = ((Long)source.get(DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED)) > 0 || 
	    		(Integer)source.get(DBConstants.FIELD_SEARCH_STATUS) == Search.SEARCH_STATUS_K;
            
            indexValue = (Float)source.get("indexValue");
	    	
	    	if( productType == Products.FULL_SEARCH_PRODUCT )
	        {
	            productValue = (Float)source.get("fullValue");
	        }
	        else if( productType == Products.UPDATE_PRODUCT )
	        {
	        	 productValue = (Float)source.get("updateValue");
	        }
	        else if( productType == Products.REFINANCE_PRODUCT )
	        {
	        	 productValue = (Float)source.get("refValue");
	        }
	        else if( productType == Products.CURRENT_OWNER_PRODUCT )
	        {
	        	 productValue = (Float)source.get("crtOValue");
	        }
	        else if( productType == Products.CONSTRUCTION_PRODUCT )
	        {
	        	 productValue = (Float)source.get("cstrValue");
	        }
	        else if( productType == Products.COMMERCIAL_PRODUCT)
	        {
	        	 productValue = (Float)source.get("commValue");
	        }
	        else if( productType == Products.OE_PRODUCT)
	        {
	        	 productValue = (Float)source.get("oeValue");
	        }
	        else if( productType == Products.LIENS_PRODUCT)
	        {
	        	 productValue = (Float)source.get("lienValue");
	        }
	        else if( productType == Products.ACREAGE_PRODUCT)
	        {
	        	 productValue = (Float)source.get("acrValue");
	        }
	        else if( productType == Products.SUBLOT_PRODUCT)
	        {
	        	 productValue = (Float)source.get("subValue");
	        } 
	        else if( productType == Products.FVS_PRODUCT)
	        {
	        	 productValue = (Float)source.get("fvsValue");
	        }
	    	if(productValue==null){		//if we cannot calculate the fee will leave it unset
	    		System.err.println("naspa");
	    		return -1;
	    	}
	    	
	    	
	    	if(isFinished) {
	    		if(substractIndexValue && (invoicedCode == InvoicedSearch.SEARCH_INVOICED_INDEX ||
	    				invoicedCode == InvoicedSearch.SEARCH_INVOICED_INDEX_AND_FINISHED)) {
	    			if(indexValue == null) {
			    		indexValue = 0.5f;
			    	}
	    			if(productValue > indexValue) {
	    				return ATSDecimalNumberFormat.formats((1 - indexValue) * productValue * rateField * discountRatio);
	    			} else {
	    				return ATSDecimalNumberFormat.formats(0);
	    			} 
	    		} else {
	    			return ATSDecimalNumberFormat.formats(productValue * rateField * discountRatio);
	    		}
	    			
	    	} else {
	    		if(indexValue == null) {
		    		indexValue = 0.5f;
		    	}
	    		return ATSDecimalNumberFormat.formats(indexValue * productValue * rateField * discountRatio);

	    	}
	    	
	    	
	    	
	    	
	    	
	    } catch (Exception e){
    		logger.debug("Not setting searchFee for now... it will be set later individually");
    		logger.debug(e.getMessage());
    		e.printStackTrace();
    		
    		
    	}
		return -1;
    }
    
    
    
    /**
     * 
     * @param searchId - the id of the search for which we want to find the product type
     * @return the product id
     */
    public static int getProductIdFromSearch( long searchId )
    {
        try {    
            return DBManager.getSimpleTemplate().queryForInt(" select SEARCH_TYPE from "+ TABLE_SEARCH +" where " + 
            		DBConstants.FIELD_SEARCH_ID + " = ?", searchId);
        } catch (Exception e) {
            logger.error("Error while reading product type for search: " + searchId, e);
        }
        return 0;
    }
    
    
    
    public static String getProductNameFromSearch(long searchId){
    	
    	
    	   int productType = getProductIdFromSearch(searchId);
    	   long commId     = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getID().longValue();
    	   Products products = CommunityProducts.getProduct(commId);
    	   return products.getProductName(productType).trim();
    
    }
    
    /**
     * Returns the name for a productId in a community
     * @param commId
     * @param productId
     * @return
     */
    public static String getProductNameFromCommunity(long commId, int productId){
    	Products products = CommunityProducts.getProduct(commId);
    	return products.getProductName(productId).trim();
    }
    
    /**
     * updates was_opened field for the searchId received
     * @param searchId
     */
    public static void setDbSearchWasOpened(long searchId)
    {
        String sql = "update "+ DBConstants.TABLE_SEARCH_FLAGS +" set WAS_OPENED = 1 where " + 
        	DBConstants.FIELD_SEARCH_FLAGS_ID + " = " + searchId;
        
        DBConnection conn = null;
        
        try {
        
            conn = ConnectionPool.getInstance().requestConnection();
            conn.executeSQL(sql);
            conn.commit();
        } catch (Exception be) {
            logger.error(be);
        } finally {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch(BaseException e) {
                logger.error(e);
            }           
        }
    }
    
    
    
    public static boolean getDbSearchWasOpened( long searchId )
    {
        String sql = "select WAS_OPENED from "+ 
        	DBConstants.TABLE_SEARCH_FLAGS + " where " + 
        	DBConstants.FIELD_SEARCH_FLAGS_ID + " = " + searchId;
        
        DatabaseData dbData = null;
        
        DBConnection conn = null;
        
        try {
        
            conn = ConnectionPool.getInstance().requestConnection();
            dbData = conn.executeSQL(sql);
            
            if( dbData.getRowNumber() > 0 )
            {
                if( Integer.parseInt(dbData.getValue(1,0).toString()) == 1 )
                {
                    return true;
                }
            }
            
        } catch (Exception be) {
            logger.error(be);
        } finally {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch(BaseException e) {
                logger.error(e);
            }           
        }
        
        return false;
    }
    
    /**
     * updates forReview field for the searchId received
     * @param searchId
     */
    public static void setDbSearchForReview(long searchId, int value)
    {
        String sql = "update "+ DBConstants.TABLE_SEARCH_FLAGS +" set forReview = " + value + " where " + 
        	DBConstants.FIELD_SEARCH_FLAGS_ID + " = " + searchId;
        
        DBConnection conn = null;
        
        try {
        
            conn = ConnectionPool.getInstance().requestConnection();
            conn.executeSQL(sql);
            conn.commit();
        } catch (Exception be) {
            logger.error(be);
        } finally {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch(BaseException e) {
                logger.error(e);
            }           
        }
    }
    
    public static boolean getDbSearchForReview( long searchId )
    {
        String sql = "select forReview from "+ 
        	DBConstants.TABLE_SEARCH_FLAGS + " where " + 
        	DBConstants.FIELD_SEARCH_FLAGS_ID + " = " + searchId;
        
        DatabaseData dbData = null;
        
        DBConnection conn = null;
        
        try {
        
            conn = ConnectionPool.getInstance().requestConnection();
            dbData = conn.executeSQL(sql);
            
            if( dbData.getRowNumber() > 0 )
            {
                if( Boolean.valueOf((Boolean) dbData.getValue(1,0)).booleanValue())
                {
                    return true;
                }
            }
            
        } catch (Exception be) {
            logger.error(be);
        } finally {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch(BaseException e) {
                logger.error(e);
            }           
        }
        
        return false;
    }
    
    public static void deleteAllowedCountyListUser( BigDecimal userId )
    {
        DBConnection conn = null;

        try{
            conn = ConnectionPool.getInstance().requestConnection();
            
            conn.executeSQL( "delete from "+ TABLE_USER_COUNTY +" where USER_ID = " + userId.longValue() );
            conn.commit();
        } catch (Exception be) {
            logger.error(be);
            be.printStackTrace();
        } finally {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch(BaseException e) {
                logger.error(e);
            }           
        }
    }
    
    
    
    public static Vector<County> loadAllowedCountiesForUser( BigDecimal userId )
    {
        DBConnection conn = null;
        DatabaseData dbData = null;
        Vector<County> allowed = new Vector<County>();
        
        try{
            conn = ConnectionPool.getInstance().requestConnection();
            
            dbData = conn.executeSQL( "select COUNTY_ID from "+ TABLE_USER_COUNTY +" where USER_ID = " + userId.longValue() );
            
            if( dbData != null && dbData.getRowNumber() > 0 )
            {
                for( int i = 0 ; i < dbData.getRowNumber() ; i ++ )
                {
                    BigDecimal countyId = new BigDecimal(dbData.getValue( 1, i ).toString());
                    
                    County allowedCounty = County.getCounty( countyId );
                    if( allowedCounty != null )
                    {
                        allowed.add( allowedCounty );
                    }
                }
            }
            
        } catch (Exception be) {
            logger.error(be);
            be.printStackTrace();
        } finally {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch(BaseException e) {
                logger.error(e);
            }           
        }
        
        return allowed;
    }
    
    public static void unlockSearch( long searchID ) {
        
        DBConnection conn = null;
            
        try {
            
            String querySql = "UPDATE " + DBConstants.TABLE_SEARCH_FLAGS + " SET " +
            	DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + " = 0 WHERE " +
            	DBConstants.FIELD_SEARCH_FLAGS_ID + " = " + searchID;

            conn = ConnectionPool.getInstance().requestConnection();
            conn.executeSQL( querySql );
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
    }
    
    public static String getAgentEmailFromSearchID( BigDecimal ID )
    {
        String email = "N/A";
        
        DBConnection conn = null;
        DatabaseData dbData = null;
        
        try {
            
            String querySql = "select b.EMAIL from "+ TABLE_SEARCH +" a, "+ TABLE_USER +" b where a.AGENT_ID = b.USER_ID and a.ID = " + ID.toString();

            conn = ConnectionPool.getInstance().requestConnection();
            dbData = conn.executeSQL( querySql );
            if( dbData.getRowNumber() > 0 )
            {
                email = (String) dbData.getValue( 1, 0 );
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
        
        return email;
    }
    
    private static final String SELECT_AGENT_LOGIN = "select b.login from "+ TABLE_SEARCH +" a, "+ TABLE_USER +" b where a.AGENT_ID = b.USER_ID and a.ID = ?"; 
    
    public static String getAgentLoginFromSearchID( long searchId ){
    	return DBManager.getSimpleTemplate().queryForObject(SELECT_AGENT_LOGIN, String.class, searchId);
    }
    
    public static Vector<BigDecimal> getDistinctUserRatings( String columnName, int[] stateId, String[] userIdsStringArray )
    {        
        DBConnection conn = null;
        DatabaseData dbData = null;
        
        Vector<BigDecimal> userRatings = new Vector<BigDecimal>();
        int i = 0;
        String stateIdList = "";
        
        for( i = 0 ; i < stateId.length ; i ++ )
        {
        	if(stateId[i] <= 0) {
        		stateIdList = null;
        		break;
        	}
            stateIdList += stateId[i];
            
            if( i != stateId.length - 1 )
            {
                stateIdList += ", ";
            }
        }
        
        String usersAsString = null;
        if(userIdsStringArray == null || userIdsStringArray.length == 0) {
        	usersAsString = "-2121";
        } else {
        	usersAsString = org.apache.commons.lang.StringUtils.join(userIdsStringArray, ",");
        }
        
        try {
        	
        	String querySql = "select DISTINCT(" + sqlColumnName(columnName) + ") from "+ TABLE_USER_RATING + " r ";
        	if(StringUtils.isNotEmpty(stateIdList)) {
        		stateIdList = StringUtils.makeValidNumberList(stateIdList);
        		querySql += " JOIN " +
        				"(select max(ri.start_date) start_date, ri.county_id from ts_user_rating ri where ri.user_id in ( " + usersAsString + 
        				") and ri.county_id in (select id from " + DBConstants.TABLE_COUNTY + " where state_id in (" + stateIdList + ")) " +
        				" group by ri.county_id) j1 on j1.start_date = r.start_date ";
        		querySql += " where r.COUNTY_ID in ( select ID from "+ TABLE_COUNTY +" where STATE_ID in ( " + stateIdList + " ) )";
               	querySql += " and r." + DBConstants.FIELD_USER_RATING_USER_ID + " in (" + usersAsString + 
               		") and r.county_id = j1.county_id";
        	} else {
        		querySql += " JOIN " +
				"(select max(ri.start_date) start_date, ri.county_id from ts_user_rating ri where ri.user_id in ( " + usersAsString + 
				") group by ri.county_id) j1 on j1.start_date = r.start_date " +
				" where r." + DBConstants.FIELD_USER_RATING_USER_ID + " in ( " + usersAsString + 
				") and r.county_id = j1.county_id";
        	}
        	
            

            conn = ConnectionPool.getInstance().requestConnection();
            dbData = conn.executeSQL( querySql );
            if( dbData.getRowNumber() > 0 )
            {
                for( i = 0 ; i < dbData.getRowNumber() ; i ++ )
                {
                	Object value=dbData.getValue( 1, i );
                	if(value!=null){
                		userRatings.add( new BigDecimal(value.toString()) );
                	}
                	else{
                		//the defaul rate is 1
                		//userRatings.add( new BigDecimal("1") );
                	}
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
        
        return userRatings;
    }
        
    public static void revertUserRating(String startDate){

    	DBConnection conn = null;
        DatabaseData dbDataMax = null;
        DatabaseData dbDataRow = null;
        
        String selectMax = "SELECT USER_ID FROM " + TABLE_USER_RATING + 
        	" WHERE start_date > str_to_date( ?,'%d/%m/%Y')";  

        String selectRow;
        String selectUpdate;
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            
    		PreparedStatement pstmt = conn.prepareStatement( selectMax );
    		pstmt.setString( 1, startDate);
    		dbDataMax = conn.executePrepQuery(pstmt);	
    		pstmt.close();
            
            logger.info( dbDataMax.getRowNumber() + " users" );
            if( dbDataMax.getRowNumber() > 0 )
            {
            	for (int i = 0; i < dbDataMax.getRowNumber(); i++) {
            		System.out.println("id: " + dbDataMax.getValue(1,i).toString());	
            		selectRow = "select * from ts_user_rating where user_id = ? and start_date < str_to_date( ?,'%d/%c/%Y') order by id desc";
            		
            		PreparedStatement pstmt1 = conn.prepareStatement( selectRow );
            		pstmt1.setString( 1, dbDataMax.getValue(1,i).toString());
            		pstmt1.setString( 2, startDate);
            		dbDataRow = conn.executePrepQuery(pstmt1);	
            		pstmt1.close();
            		
            		if(dbDataRow.getRowNumber()>0){
            			selectUpdate = "insert into ts_user_rating values(" +
            				dbDataRow.getValue(1,0).toString() + "," + 
            				" date_format(now(), '%d/%m/%Y') " + "," +
            				dbDataRow.getValue(3,0).toString() + "," +
            				dbDataRow.getValue(4,0).toString() + "," +
            				" SEQ_USER_RATE.nextval " + "," +
            				dbDataRow.getValue(6,0).toString() + ")";
            			
            			logger.info( selectUpdate );
            			
            			conn.executeSQL(selectUpdate);
            		}
				}
                
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
        
    }

	
    private static final String SQL_LOAD_SEARCH_DATA_CONTEXT = "SELECT " + 
    		FIELD_SEARCH_DATA_CONTEXT + ", " + 
    		FIELD_SEARCH_DATA_VERSION + ", " + 
    		DBConstants.FIELD_SEARCH_SDATE + ", " + 
    		"CONVERT_TZ(s.sdate, '+00:00', @@session.time_zone) sdate, " + 
			"CONVERT_TZ(s.sdate - interval 1 day, '+00:00', @@session.time_zone) sdate_backup " + 
    		" FROM " + TABLE_SEARCH_DATA + " sd join " + TABLE_SEARCH + " s on sd." + FIELD_SEARCH_DATA_SEARCHID + " = s." + DBConstants.FIELD_SEARCH_ID + 
    		" JOIN " + DBConstants.TABLE_SEARCH_FLAGS + " f on s." + DBConstants.FIELD_SEARCH_ID + " = f." + DBConstants.FIELD_SEARCH_FLAGS_ID +  
    		" WHERE s." + DBConstants.FIELD_SEARCH_ID + " = ?";
    private static final String SQL_LOAD_SEARCH_DATA_INFO = "SELECT " + 
    	DBConstants.FIELD_SEARCH_FLAGS_TO_DISK + ", " + 
    	DBConstants.FIELD_SEARCH_SDATE + 
    	", " + DBConstants.FIELD_SEARCH_FLAGS_LOG_ORIGINAL_LOCATION + 
    	" FROM " + 
    	DBConstants.TABLE_SEARCH + " s JOIN " + 
		DBConstants.TABLE_SEARCH_FLAGS + " f ON s." + DBConstants.FIELD_SEARCH_ID + " = f." + DBConstants.FIELD_SEARCH_FLAGS_ID +  
		" WHERE s." + DBConstants.FIELD_SEARCH_ID + " = ?";
    public static byte[] loadSearchDataFromDB( long searchId){
    	return loadSearchDataFromDB(searchId, true);
    }
    
    public static final String SQL_LOAD_SEARCH_FROM_DB = "SELECT " + 
    		FIELD_SEARCH_DATA_CONTEXT + ", " + 
    		FIELD_SEARCH_DATA_VERSION + ", " + 
    		DBConstants.FIELD_SEARCH_SDATE + " unformated_sdate, " + 
    		"CONVERT_TZ(s.sdate, '+00:00', @@session.time_zone) sdate, " + 
			"CONVERT_TZ(s.sdate - interval 1 day, '+00:00', @@session.time_zone) sdate_backup, " +
			DBConstants.FIELD_SEARCH_FLAGS_TO_DISK +  
    		" FROM " + TABLE_SEARCH_DATA + " sd join " + TABLE_SEARCH + " s on sd." + FIELD_SEARCH_DATA_SEARCHID + " = s." + DBConstants.FIELD_SEARCH_ID + 
    		" JOIN " + DBConstants.TABLE_SEARCH_FLAGS + " f on s." + DBConstants.FIELD_SEARCH_ID + " = f." + DBConstants.FIELD_SEARCH_FLAGS_ID +  
    		" WHERE s." + DBConstants.FIELD_SEARCH_ID + " = ?";
    
    public static byte[] loadSearchDataFromDB( long searchId, boolean checkArchive ){
    	
    	byte[] contextData = null;
    	
    	try {
    		
    		LoadSearchContextMapper contextMapper = getSimpleTemplate().queryForObject(SQL_LOAD_SEARCH_FROM_DB, new LoadSearchContextMapper(), searchId);
    		
    		if(ServerConfig.getSearchContextEnableStatus() != 0) {
    			String generatedIndexKey = searchId + "_v" + org.apache.commons.lang.StringUtils.leftPad(Integer.toString(contextMapper.getVersion()), 6, '0') + ".zip";
				String sharedDrivePath = SharedDriveUtils.getSearchContextFolder(searchId, contextMapper.getSDate());
				if(sharedDrivePath != null) {
					
					{
						File documentIndexFile = new File(sharedDrivePath + generatedIndexKey);
						if(documentIndexFile.exists()) {
							contextData = org.apache.commons.io.FileUtils.readFileToByteArray(documentIndexFile);
						} else {
							logger.error("loadSearchDataFromDB searchid " + searchId + " is null for shared path " + sharedDrivePath + generatedIndexKey);
						}
					}
					
					if(contextData == null) {
						//check to see if gmt offset broke our link
						sharedDrivePath = SharedDriveUtils.getSearchContextFolder(searchId, contextMapper.getSDateBackup());
						if(sharedDrivePath != null) {
							
							{
								File documentIndexFile = new File(sharedDrivePath + generatedIndexKey);
								if(documentIndexFile.exists()) {
									contextData = org.apache.commons.io.FileUtils.readFileToByteArray(documentIndexFile);
								} else {
									logger.error("loadSearchDataFromDB_backup searchid " + searchId + " is null for shared path " + sharedDrivePath + generatedIndexKey);
								}
							}
						}
					}
					
				} else {
					logger.error("loadSearchDataFromDB sharedDrivePath for searchid " + searchId + " is null");
				}
			
    		}
    		
    		if(contextData == null) {
    			contextData = contextMapper.getDocument();
    		}
    		
    		if(contextData == null && checkArchive && contextMapper.getToDisk() > 0) {
    			//we must load it from the database
            	String archivePath = null;
            	try {
            		archivePath = ArchiveEntry.getArchiveCompletePath(searchId, contextMapper.getUnformattedSDate());
            		
            		contextData = org.apache.commons.io.FileUtils.readFileToByteArray(new File(archivePath));
            		
            	} catch (Exception e) {
    				logger.error("Failed to load context from archive for search " + 
    						searchId + ". The path tried is " + archivePath, e );
    			}
    		}
    		
			
        } catch (Exception e) {
        	e.printStackTrace();
        	logger.error("loadSearchDataFromDB for searchId " + searchId, e);
        	Log.sendExceptionViaEmail(
					MailConfig.getExceptionEmail(), 
					"Cannot loadSearchDataFromDB for searchId " + searchId, 
					e, 
					"SearchId used: " + searchId);
        }
        
        return contextData;
    }
    
    public static long getSearchDBVersion( long searchId ){
    	try {
    		return getSimpleTemplate().queryForInt("SELECT " + FIELD_SEARCH_DATA_VERSION + " FROM " +
									TABLE_SEARCH_DATA + " WHERE " + FIELD_SEARCH_DATA_SEARCHID + " = ?", searchId);
        } catch (Exception e) {
        	e.printStackTrace();
        	logger.error("getSearchDBVersion for searchId " + searchId, e);
        }
        
        return 0;
    }
    
    public static long getCommunityForSearch(long id) {
		
		String sql = "select " + 
			DBConstants.FIELD_SEARCH_COMM_ID + " from "+ 
			TABLE_SEARCH +" where " + 
			DBConstants.FIELD_SEARCH_ID + " = " + id;
        long commId = -1;
        DatabaseData dbData = null;
        
        DBConnection conn = null;
        
        try {
        
            conn = ConnectionPool.getInstance().requestConnection();
            dbData = conn.executeSQL(sql);
            
            if( dbData.getRowNumber() > 0 )
            {
                commId = ((BigInteger)dbData.getValue(1,0)).longValue();
            }
            
        } catch (Exception be) {
            logger.error(be);
        } finally {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch(BaseException e) {
                logger.error(e);
            }           
        }
        return commId;
	}
    public static void saveCurrentSearchData( Search search, byte[] searchContext ) throws Exception{
    	
    	int MAX_SEARCH_CONTEXT_VERSIONS_TO_KEEP = ServerConfig.getSearchContextVersionsToKeep();
    	
    	long searchId = search.getID();
    	long versionNo = search.getSavedVersion();
    	
    	SearchLogger.info("Saving version " + versionNo + " " + SearchLogger.getTimeStampAndLocation(searchId) + "...", searchId);
    	
        logger.info( "Saving Context to database..." );
    	
        loggerLocal.info(" Saving Context to database... searchId = "+searchId+ " versionNo  = " + versionNo );
        
    	GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
    	
    	int day = cal.get( GregorianCalendar.DAY_OF_MONTH );
    	int month = cal.get( GregorianCalendar.MONTH ) + 1;
    	int year = cal.get( GregorianCalendar.YEAR );
    	
    	//create the table if it does not exist
    	DBConnection conn = null;         
    	
    	boolean executeUpdate = true;
    	
    	long t1 = System.currentTimeMillis();
    	
    	
        try {
        	
        	 if( searchId<=0 ){
             	throw new RuntimeException("Invalid search ID "+ searchId);
             }
            
            conn = ConnectionPool.getInstance().requestConnection();
            
            try{
            	DatabaseData searchInfo = conn.executeSQL( "SELECT " + FIELD_SEARCH_DATA_DATESTRING + " FROM " +
            												TABLE_SEARCH_DATA + " WHERE " + FIELD_SEARCH_DATA_SEARCHID + " = '" + searchId + "'");
            	
            	if( searchInfo.getRowNumber() <= 0 ){
            		executeUpdate = false;
            	}
            }
            catch( Exception e ){
            	e.printStackTrace();
            	executeUpdate = false;
            }
            
            //insert data to table
            String sqlPhrase = "";
            PreparedStatement pstmt;
            if( !executeUpdate ){
            	//insert
            	
            	if(ServerConfig.getSearchContextEnableStatus() != 2) {
            		sqlPhrase = "INSERT INTO " + TABLE_SEARCH_DATA + " ( `" + FIELD_SEARCH_DATA_SEARCHID + "`," +
							"`" + FIELD_SEARCH_DATA_DATESTRING + "`, " +
							"`" + FIELD_SEARCH_DATA_VERSION + "`, " +
							"`" + FIELD_SEARCH_DATA_CONTEXT + "` ) " + 
								" VALUES ( ?, ?, ?, ? ) ";	
            	} else {
            		sqlPhrase = "INSERT INTO " + TABLE_SEARCH_DATA + " ( `" + FIELD_SEARCH_DATA_SEARCHID + "`," +
							"`" + FIELD_SEARCH_DATA_DATESTRING + "`, " +
							"`" + FIELD_SEARCH_DATA_VERSION +  "` ) " + 
								" VALUES ( ?, ?, ? ) ";
            	}
            	
            	
            	            	
            	pstmt = conn.prepareStatement(sqlPhrase);
            	
            	pstmt.setLong( 1, searchId );
            	pstmt.setString( 2, year + "_" + month + "_" + day );
            	pstmt.setLong( 3 , versionNo );
            	if(ServerConfig.getSearchContextEnableStatus() != 2) {
            		pstmt.setBytes(4, searchContext);
            	}
            	
            }
            else {
            	//update
            	if(ServerConfig.getSearchContextEnableStatus() != 2) {
            		sqlPhrase = "UPDATE " + TABLE_SEARCH_DATA + " SET " + FIELD_SEARCH_DATA_CONTEXT + " = ?, " + FIELD_SEARCH_DATA_VERSION + " = ? WHERE " + FIELD_SEARCH_DATA_SEARCHID + " = " + searchId;
            	} else {
            		sqlPhrase = "UPDATE " + TABLE_SEARCH_DATA + " SET " + FIELD_SEARCH_DATA_VERSION + " = ? WHERE " + FIELD_SEARCH_DATA_SEARCHID + " = " + searchId;
            	}
            	
            	pstmt = conn.prepareStatement(sqlPhrase);
            	if(ServerConfig.getSearchContextEnableStatus() != 2) {
            		pstmt.setBytes(1, searchContext);
            		pstmt.setLong( 2 , versionNo );
            	} else {
            		pstmt.setLong( 1 , versionNo );
            	}
            	
            }
            
            pstmt.executeUpdate();
            pstmt.close();
            conn.commit();
            
            
            if(ServerConfig.getSearchContextEnableStatus() != 0) {
            	String generatedIndexKey = searchId + "_v" + org.apache.commons.lang.StringUtils.leftPad(Long.toString(versionNo), 6, '0');
            	String sharedDrivePath = SharedDriveUtils.getSearchContextFolder(search);
            	if(org.apache.commons.lang.StringUtils.isNotBlank(sharedDrivePath)) {
            		String toSaveFileName = sharedDrivePath + generatedIndexKey + ".zip";
            		
            		try {
    					//let's do the new save
    					{
    						org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(toSaveFileName), searchContext );
    						
    						String[] fileList = new File(sharedDrivePath).list();
    						Arrays.sort(fileList);
    						for (int i = 0; i < fileList.length - MAX_SEARCH_CONTEXT_VERSIONS_TO_KEEP; i++) {
    							org.apache.commons.io.FileUtils.deleteQuietly(new File(sharedDrivePath + fileList[i]));
							}
    					}
    				} catch (Exception e) {
    					logger.error("Cannot saveSearchContext to " + toSaveFileName, e);
    					String documentIndexBackupLocalFolder = SharedDriveUtils.getSearchContextBackupLocalFolder(search);
    					if(org.apache.commons.lang.StringUtils.isNotBlank(documentIndexBackupLocalFolder)) {
    						try {
    							org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(documentIndexBackupLocalFolder + generatedIndexKey + ".zip"), searchContext);
    						} catch (IOException e1) {
    							logger.error("Cannot saveSearchContext to backup folder " + documentIndexBackupLocalFolder + generatedIndexKey + ".zip", e);
    						}
    					}
    				}
            		
            	}
            }
            
            logger.info( "Search context save to database took " + ( System.currentTimeMillis() - t1 ) / 1000 );
        } catch (Exception e) {
        	e.printStackTrace();
        	String message = "SearchID: "+ searchId+"\n";
        	message += "VersionNo: " +versionNo+"\n";
        	message += "year_month_day: " +year + "_" + month + "_" + day+"\n";
        	message += "searchId: " +searchId+"\n";
        	message += ExceptionUtils.getFullStackTrace(e);
        	loggerLocal.error(" Could not save  Context to database... searchId = "+searchId+ " versionNo  = " + versionNo +"\n"+ ExceptionUtils.getFullStackTrace(e));
        	sendWarningMailMessage( InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext(), " Could not save the search ", message);
        	throw e;
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
        loggerLocal.info(" Saved with success  Context to database... searchId = "+searchId+ " versionNo  = " + versionNo );
    }
    
    /**
     * Moved contets of ATSFolder configuration files to database
     * 
     * function reads file contents from database
     * 
     * @param fileName - file that we want to "read"
     * @return file contents
     */
    public static byte[] getFileContentsFromDb( String comm_id,String  tablename){
    	
    	DBConnection conn = null;
    	
    	byte[] fileContents = null;
    	
    	try {
    		PreparedStatement pstmt;    
            conn = ConnectionPool.getInstance().requestConnection();
            
           	String sqlPhrase = "SELECT " + "privacy_file" + " FROM " +
           	tablename + " WHERE " + "comm_id" + " = ? ";

           	pstmt = conn.prepareStatement(sqlPhrase);
           	pstmt.setString( 1, comm_id);
   			ResultSet rs = pstmt.executeQuery();

   			if(rs.next()){
   				fileContents = rs.getBytes(1);
   			}            
    			
    			pstmt.close();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
    	
    	return fileContents;
    }
    

    
    
    public static byte[] getXMLFileContentsFromDb( String fileName ){
    	
    	DBConnection conn = null;
    	
    	byte[] fileContents = null;
    	
    	try {
    		PreparedStatement pstmt;    
            conn = ConnectionPool.getInstance().requestConnection();
            
           	String sqlPhrase = "SELECT " + DBConstants.FIELD_DP_DATA_FILE + " FROM " +  
           	DBConstants.TABLE_DESCRIPTIBLE + " WHERE " +DBConstants.FIELD_DP_FILE_NAME + " = ?";
            	
           	pstmt = conn.prepareStatement(sqlPhrase);
         	pstmt.setString( 1, fileName);
   			ResultSet rs = pstmt.executeQuery();

   			if(rs.next()){
   				fileContents = rs.getBytes(1);
   			}            
    			
    			pstmt.close();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
    	
    	return fileContents;
    }
    
  public static LinkedList<String> getXMLFileList(  ){
    	
    	DBConnection conn = null;
    	LinkedList<String> listFile = new LinkedList<String>();
    //	byte[] fileContents = null;
    	
    	try {
    		PreparedStatement pstmt;    
            conn = ConnectionPool.getInstance().requestConnection();
            
           	String sqlPhrase = "SELECT "+DBConstants.FIELD_DP_FILE_NAME+"  FROM " +  
           	DBConstants.TABLE_DESCRIPTIBLE ;
            	
           	pstmt = conn.prepareStatement(sqlPhrase);
   			ResultSet rs = pstmt.executeQuery();

   			while(rs.next()){
   				listFile.add( new String( rs.getBytes(1)));
   	   			}            
    			
    			pstmt.close();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
    	
    	return listFile;
    }
    

    /**
     * Moved contets of ATSFolder configuration files to database
     * 
     * function reads file contents from database
     * 
     * @param fileName - file that we want to "read"
     * @return file contents
     */
    public static byte[] getFileContentsFromDb( String fileName ){
    	
    	DBConnection conn = null;
    	
    	byte[] fileContents = null;
    	
    	try {
    		PreparedStatement pstmt;    
            conn = ConnectionPool.getInstance().requestConnection();
            
           	String sqlPhrase = "SELECT " + FIELD_ATSFOLDER_CONTENTS + " FROM " +
								TABLE_ATS_FOLDER_FILES + " WHERE " + FIELD_ATSFOLDER_FILENAME + " = ?";
            	
           	pstmt = conn.prepareStatement(sqlPhrase);
           	pstmt.setString( 1, fileName);
   			ResultSet rs = pstmt.executeQuery();

   			if(rs.next()){
   				fileContents = rs.getBytes(1);
   			}            
    			
    			pstmt.close();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
    	
    	return fileContents;
    }
    
    /**
     * Writes file contents to ts_atsfolder table from database
     * 
     * @param fileName - file that we want to write to database
     */
    public static void writeFileContentsToDb( String fileName, byte[] fileContents ){
    	//check if we have data
    	byte[] fileData = getFileContentsFromDb( fileName );
    	boolean update = false;
    	if( fileData != null ){
    		//we already have the file --> insert
    		update = true;
    	}
    	
    	DBConnection conn = null;
    	
    	try {
    		PreparedStatement pstmt;    
            conn = ConnectionPool.getInstance().requestConnection();
            
           	String sqlPhrase = "";
           	if( !update ){
           		sqlPhrase = "INSERT INTO " + TABLE_ATS_FOLDER_FILES + " (`" + FIELD_ATSFOLDER_FILENAME +
           					"`, `" + FIELD_ATSFOLDER_CONTENTS + "`) VALUES (?, ?)";
           		
               	pstmt = conn.prepareStatement(sqlPhrase);
               	
               	pstmt.setString( 1 , fileName);
            	pstmt.setBytes(2, fileContents);
           	}
           	else{
           		sqlPhrase = "UPDATE " + TABLE_ATS_FOLDER_FILES + " SET " + FIELD_ATSFOLDER_CONTENTS + " = ? WHERE "
           		+ FIELD_ATSFOLDER_FILENAME + " = ? ";
           		
               	pstmt = conn.prepareStatement(sqlPhrase);
               	pstmt.setBytes( 1 , fileContents);
               	pstmt.setString(2, fileName);
           	}

            pstmt.executeUpdate();
            pstmt.close();
            conn.commit();
            
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
    	
    }
    
    public static void writeXMLFileContentsToDb( String fileName, byte[] fileContents ){
    	//check if we have data
    	byte[] fileData = getXMLFileContentsFromDb( fileName );
    	boolean update = false;
    	if( fileData != null ){
    		//we already have the file --> insert
    		update = true;
    	}
    	
    	DBConnection conn = null;
    	
    	try {
    		PreparedStatement pstmt;    
            conn = ConnectionPool.getInstance().requestConnection();
            
           	String sqlPhrase = "";
           	if( !update ){
           		sqlPhrase = "INSERT INTO " +  DBConstants.TABLE_DESCRIPTIBLE + " (`" +DBConstants.FIELD_DP_FILE_NAME +
           					"`, `" + DBConstants.FIELD_DP_DATA_FILE + "`) VALUES (?, ?)";
           		
               	pstmt = conn.prepareStatement(sqlPhrase);
               	
               	pstmt.setString( 1 , fileName);
            	pstmt.setBytes(2, fileContents);
           	}
           	else{
           		sqlPhrase = "UPDATE " +  DBConstants.TABLE_DESCRIPTIBLE + " SET " +DBConstants.FIELD_DP_DATA_FILE + " = ? WHERE "
           		+ DBConstants.FIELD_DP_FILE_NAME + " = ?";
           		
               	pstmt = conn.prepareStatement(sqlPhrase);
               	pstmt.setBytes( 1 , fileContents);
               	pstmt.setString( 2, fileName);
           	}

            pstmt.executeUpdate();
            pstmt.close();
            conn.commit();
            
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
    	
    }
    

    /**
     * Updates ats folder file contents to the database
     * @param fileAbsolutPath - full path of the file to be updated
     */
    public static void updateAtsFileContents( String fileAbsolutePath ){
    	
    	File fileToUpdate = new File( fileAbsolutePath );
    	
    	String fileName = fileToUpdate.getName();
    	
    	String fileContents = FileUtils.readFile( fileAbsolutePath );
    	
    	writeFileContentsToDb( fileName, fileContents.getBytes() );
    	
    }
    
    /**
     * loads dimensions from database for the browser sizes
     */
    public static List<InterfaceSettings.Dimensions> getInterfaceSettings(){
    	List<InterfaceSettings.Dimensions> dime = new ArrayList<InterfaceSettings.Dimensions>();
    	
    	DBConnection conn = null;
    	DatabaseData dbData = null;
    	
    	try {   
            conn = ConnectionPool.getInstance().requestConnection();
            
            dbData = conn.executeSQL( "SELECT " + SEARCH_PAGE_HEIGHT + ", " + SEARCH_PAGE_WIDTH + ", " + REPORTS_HEIGHT + ", " + REPORTS_WIDTH +
            							" FROM " + TABLE_INTERFACE_SETTINGS );
            
            double sp_height = (Double) dbData.getValue(1, 0);
            double sp_width = (Double) dbData.getValue(2, 0); 
            double rep_height = (Double) dbData.getValue(3, 0); 
            double rep_width  = (Double) dbData.getValue(4, 0); 
            	
            InterfaceSettings.Dimensions dim = new InterfaceSettings.Dimensions(sp_height, sp_width, rep_height, rep_width);
            dime.add(dim);
            
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
    	
    	return dime;
    }
    
    /**
     * update dimensions into database
     * @param dimensions - values fo the dimensions
     */
    public static void updateDimensions(InterfaceSettings.Dimensions dimensions){  	
    	try {              
            String query = "";            
            	query = "UPDATE " + TABLE_INTERFACE_SETTINGS +
            			" SET " + SEARCH_PAGE_HEIGHT + " = ? " + ", " + SEARCH_PAGE_WIDTH + " = ? " + ", " + REPORTS_HEIGHT + " = ? " + ", " + REPORTS_WIDTH + " = ? " ;
            	
            	try {
            		DBManager.getSimpleTemplate().update(query, dimensions.sp_height, dimensions.sp_width, dimensions.rep_height, dimensions.rep_width);
            	} catch (Exception e) {
            		logger.error("Error on insert!", e);
				}
    	} catch (Exception e) {
    		logger.error("Main error on update.", e);
		}
    }

    
    /**
     * loads passwords from database for this current machine
     */
    public static Map<String,SitesPasswords.Password> getPasswords(){
;    	Map<String, SitesPasswords.Password> passwords = new TreeMap<String, SitesPasswords.Password>();
    	
    	DBConnection conn = null;
    	DatabaseData dbData = null;
    	
    	try {   
            conn = ConnectionPool.getInstance().requestConnection();
            
            dbData = conn.executeSQL( "SELECT " + FIELD_PASSWORD_SITE + ", " + FIELD_PASSWORD_NAME + ", " + FIELD_PASSWORD_VALUE +
            							", " + DBConstants.FIELD_PASSWORD_COMMUNITY_ID +
            							" FROM " + TABLE_PASSWORDS + " WHERE " + FIELD_PASSWORD_MACHINE_NAME + 
            							" = '" + URLMaping.INSTANCE_DIR + "'");
            
            for( int i = 0 ; i < dbData.getRowNumber() ; i ++ ){
            	
            	String site = (String) dbData.getValue( 1 , i);
            	String name = (String) dbData.getValue( 2,  i);
            	String passwordValue = (String) dbData.getValue( 3 , i);            	
				String value =StringUtils.isEmpty(passwordValue)?"":passwordValue.replaceAll("\\s", "");				
            	String communityId = ((BigInteger) dbData.getValue(4, i)).toString();
            	//TODO trebuie scoasa de aici partea asta
            	SitesPasswords.Password password = new SitesPasswords.Password(site, name, value, communityId, null);
            	//logger.debug(password);
            	passwords.put(communityId + "."+site + "." + name, password);            	
            }
            
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
    	
    	return passwords;
    }
    
    public static Map<String,SitesPasswords.Password> getPasswords(String communityId,String siteName, boolean queryForFilter){
    	Map<String, SitesPasswords.Password> passwords = new TreeMap<String, SitesPasswords.Password>();
    	
    	DBConnection conn = null;
    	DatabaseData dbData = null;
    	
    	try {   
            conn = ConnectionPool.getInstance().requestConnection();
            
            StringBuffer communityWhereCondition = new StringBuffer((communityId==null||communityId.equalsIgnoreCase("NULL"))? 
					" AND " + DBConstants.FIELD_PASSWORD_COMMUNITY_ID + " IS " + communityId :
			" AND " + DBConstants.FIELD_PASSWORD_COMMUNITY_ID + "=" +communityId);
            
            if (siteName!=null){
            	communityWhereCondition.append("AND " + FIELD_PASSWORD_SITE + " = '" + siteName + "'");
            }
            
			StringBuffer sqlPhrase = new StringBuffer( "SELECT DISTINCT "  
            								+ FIELD_PASSWORD_SITE + ", " 
            								+ FIELD_PASSWORD_NAME );
			
            					if(!queryForFilter){
            							//deletes the DISTINCT word from select 
            						    sqlPhrase.delete(7, 16);
            							sqlPhrase.append(", "
                								+ FIELD_PASSWORD_VALUE +", "
                								+ DBConstants.FIELD_PASSWORD_COMMUNITY_ID);
            					}
            					
            					sqlPhrase.append(
            							" FROM " + TABLE_PASSWORDS 
            							+ " WHERE " + FIELD_PASSWORD_MACHINE_NAME +" = '" + URLMaping.INSTANCE_DIR + "'" + 
            							communityWhereCondition
            							);
            					
            					if(!queryForFilter){
            					sqlPhrase.append(
            							 " GROUP BY " +  FIELD_PASSWORD_SITE
            							+ " ORDER BY "  + FIELD_PASSWORD_SITE);
            					}	
            					
			dbData = conn.executeSQL( sqlPhrase.toString());
            
            for( int i = 0 ; i < dbData.getRowNumber() ; i ++ ){
            	String site = (String) dbData.getValue( 1 , i);
            	String name = (String) dbData.getValue( 2,  i);
            	String commId="";
            	if(!queryForFilter){
            		String value =(String) dbData.getValue( 3 , i);
                	commId =((BigInteger) dbData.getValue( 4 , i)).toString();
            	}
            	//TODO de scos treaba asta de aici.
            	passwords.put(commId+"."+site + "." + name, new SitesPasswords.Password(site, null, null, null, null));            	
            }
            
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
    	
    	return passwords;
    }
    
    /**
     * loads passwords from database for this current machine and for a specified communityId
     * @param queryForFilter TODO
     */
    public static Map<String,SitesPasswords.Password> getPasswords(String communityId, boolean queryForFilter){
    	return getPasswords(communityId, null, queryForFilter);
    }
    
    
    public static void deletePassword(String communityID, String site, String name){
    	DBConnection conn = null;
    	
        String sql = "DELETE FROM " + TABLE_PASSWORDS +
		" WHERE " + FIELD_PASSWORD_MACHINE_NAME + " = '" + URLMaping.INSTANCE_DIR + "' " +
		" AND " + FIELD_PASSWORD_SITE + " = ? " +
		" AND " + FIELD_PASSWORD_NAME + " = ? " +
		" AND " + DBConstants.FIELD_COMMUNITY_COMM_ID + " = ? "
		;
		
		try {
			DBManager.getSimpleTemplate().update(sql,site,name,communityID);
		} catch (Exception e) {
			e.printStackTrace();
		//	 logger.error(e);
		}
    }
    
    /**
     * update a password into database
     * @param passwordName - name of the password
     * @param passwordValue - value of the password
     */
    public static void writePassword(SitesPasswords.Password password, boolean isNew ){  	
    	try {              
            String query = "";            
            if( isNew ){
            	query = "INSERT INTO " + TABLE_PASSWORDS + "( `" + 
            							 FIELD_PASSWORD_MACHINE_NAME + "`, `" +
            							 FIELD_PASSWORD_SITE + "`, `" +
            							 FIELD_PASSWORD_NAME + "`, `" + 
            							 FIELD_PASSWORD_VALUE + "`, " +
            							 DBConstants.FIELD_PASSWORD_COMMUNITY_ID + ") VALUES( " +
//            							 URLMaping.INSTANCE_DIR + "', " +
            							 "?, ?, ?, ?, ? )";
            		ServerInfoEntry[] serversNoLoad = (ServerInfoEntry[]) DBManager.getServers();
            		HashSet<String> added = new HashSet<String>();
            		for (ServerInfoEntry serverInfoEntry : serversNoLoad) {
            			added.add(serverInfoEntry.getAlias());
            			try {
            				DBManager.getSimpleTemplate().update(query, serverInfoEntry.getAlias(),
            					password.site,password.name,password.value,password.communityId);
            			} catch (Exception e) {
							logger.error("Error on insert!", e);
						}
					}
            		if(!added.contains(URLMaping.INSTANCE_DIR)) {
            			try {
            				DBManager.getSimpleTemplate().update(query, URLMaping.INSTANCE_DIR,
            					password.site,password.name,password.value,password.communityId);
            			} catch (Exception e) {
							logger.error("Error on insert!", e);
						}
            		}
            }
            else{
            	query = "UPDATE " + TABLE_PASSWORDS +
            			" SET " + FIELD_PASSWORD_VALUE + " = ? " +
            			" WHERE " + FIELD_PASSWORD_MACHINE_NAME + " = '" + URLMaping.INSTANCE_DIR + "' " +
            			" AND " + FIELD_PASSWORD_NAME + " = ? " +
            	        " AND " + FIELD_PASSWORD_SITE + " = ?" +
            	        " AND " + DBConstants.FIELD_PASSWORD_COMMUNITY_ID + " = ?";
            	
            	if(DBManager.getSimpleTemplate().update(query, password.value, password.name, password.site, password.communityId) == 0) {
            		query = "INSERT INTO " + TABLE_PASSWORDS + "( `" + 
						 FIELD_PASSWORD_MACHINE_NAME + "`, `" +
						 FIELD_PASSWORD_SITE + "`, `" +
						 FIELD_PASSWORD_NAME + "`, `" + 
						 FIELD_PASSWORD_VALUE + "`, " +
						 DBConstants.FIELD_PASSWORD_COMMUNITY_ID + ") VALUES( " +
						 URLMaping.INSTANCE_DIR + "', " +
						 "?, ?, ?, ? )";
            		try {
            			DBManager.getSimpleTemplate().update(query, password.site,password.name,password.value,password.communityId);
            		} catch (Exception e) {
            			logger.error("Error on insert!", e);
					}
            	}
            }
    	} catch (Exception e) {
    		logger.error("Main error on insert. isNew = " + isNew, e);
		}
    }

    /**
     * Updates database setting payrates from <b>fromComm</b> to <b>toComm</b> 
     * @param fromComm
     * @param toComm
     * @param startDate
     */
	public static void exportPayrates(int fromComm, int toComm, Calendar startDate, UserAttributes ua) {
		String sql = 
			" select a.* " +
			"from "+ TABLE_PAYRATE +" a " + 
			" where a.community_id = " + fromComm + " and a.start_date <= now() " + 
			" order by a.start_date desc, a.id desc";
                
        DBConnection conn = null;
        DatabaseData data;
        HashMap<Long, Payrate> fromPayrates = new HashMap<Long, Payrate>();
        try {   
            conn = ConnectionPool.getInstance().requestConnection();
            data = conn.executeSQL(sql);
            
            for (int i = 0; i < data.getRowNumber(); i++) {
            	Long countyId = (Long)data.getValue(11,i);
            	//if the county is by any chance null or if we've already found that county we ignore the payrate
            	if(countyId!=null && fromPayrates.get(countyId)==null){
					Payrate payr = new Payrate();
	            	payr.setId(Long.parseLong(data.getValue(1,i).toString()));
	                //payr.setStartDate(FormatDate.getDateFromFormatedString((String)data.getValue(2,i), FormatDate.TIMESTAMP));
	                payr.setCommId(((Float)data.getValue(6,i)).longValue());
	                payr.setCountyId(countyId);
	                //setting only the fields that corespond to the current user
	                if(ua.isTSAdmin()){
	                	payr.setSearchCost(((Float)data.getValue(7,i)).doubleValue());
		                payr.setUpdateCost(((Float)data.getValue(8,i)).doubleValue());
		                payr.setCurrentOwnerCost( ((Float)data.getValue(13,i)).doubleValue() );
		                payr.setRefinanceCost( ((Float)data.getValue(15,i)).doubleValue() );
		                payr.setConstructionCost(((Float)data.getValue(19,i)).doubleValue());
		                payr.setCommercialCost(((Float)data.getValue(21,i)).doubleValue());
		                payr.setOECost(((Float)data.getValue(23,i)).doubleValue());
		                payr.setLiensCost(((Float)data.getValue(25,i)).doubleValue());
		                payr.setAcreageCost(((Float)data.getValue(27,i)).doubleValue());
		                payr.setSublotCost(((Float)data.getValue(29,i)).doubleValue());	
		                payr.setIndexA2C(((Float)data.getValue(Payrate.FIELD_INDEX_A2C,0)));
		                payr.setFvsCost(((Float)data.getValue(Payrate.FIELD_FVS_A2C, 0)));
		                
		                
	                } else {
	                	payr.setSearchValue(((Float)data.getValue(4,i)).doubleValue());
		                payr.setUpdateValue(((Float)data.getValue(5,i)).doubleValue());
		                payr.setCurrentOwnerValue( ((Float)data.getValue(12,i)).doubleValue() );
		                payr.setRefinanceValue( ((Float)data.getValue(14,i)).doubleValue() );
		                payr.setConstructionValue(((Float)data.getValue(18,i)).doubleValue());
		                payr.setCommercialValue(((Float)data.getValue(20,i)).doubleValue());      
		                payr.setOEValue(((Float)data.getValue(22,i)).doubleValue());    
		                payr.setLiensValue(((Float)data.getValue(24,i)).doubleValue());
		                payr.setAcreageValue(((Float)data.getValue(26,i)).doubleValue());
		                payr.setSublotValue(((Float)data.getValue(28,i)).doubleValue());
		                payr.setIndexC2A(((Float)data.getValue(Payrate.FIELD_INDEX_C2A,0)));
		                payr.setFvsValue(((Float)data.getValue(Payrate.FIELD_FVS_C2A, 0)));
	                }
	                
	                fromPayrates.put(countyId, payr);
            	}
			}
            exportPayrates(fromPayrates, toComm, startDate, ua);
            exportDueDates(fromComm, toComm);
            
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
	}

	/**
	 * Exports payrates from a given HashMap to a community, using the given startdate
	 * @param payrates
	 * @param commId
	 * @param time
	 */
	public static void exportPayrates(HashMap<Long, Payrate> fromPayrates, 
			long commId, 
			Calendar startDate,
			UserAttributes ua) {
		//first must check if there are any payrates set for the given community that will not be updated
		//with these data, I will set those payrates to a default value
		String sql = " select a.* " +
			"from "+ TABLE_PAYRATE +" a " + 
			" where a.community_id = " + commId + " and a.start_date <= now() " + 
			" order by a.start_date desc, a.id desc";
		
	    DBConnection conn = null;
	    DatabaseData data;
	    
	    HashMap<Long, Payrate> toPayrates = null;
	    try {   
	        conn = ConnectionPool.getInstance().requestConnection();
	        data = conn.executeSQL(sql);
	        toPayrates = new HashMap<Long, Payrate>();
	        
	        for (int i = 0; i < data.getRowNumber(); i++) {
	        	Long countyId = (Long)data.getValue(11,i);
	        	if(countyId!=null && toPayrates.get(countyId)==null){
					Payrate payr = new Payrate();
	            	payr.setId(Long.parseLong(data.getValue(1,i).toString()));
	                //payr.setStartDate(FormatDate.getDateFromFormatedString((String)data.getValue(2,i), FormatDate.TIMESTAMP));
	                payr.setCommId(((Float)data.getValue(6,i)).longValue());
	                payr.setCountyId(countyId);
	                
	                //setting all the payrates and i will determine later which fields i will use 
                	payr.setSearchCost(((Float)data.getValue(7,i)).doubleValue());
	                payr.setUpdateCost(((Float)data.getValue(8,i)).doubleValue());
	                payr.setCurrentOwnerCost( ((Float)data.getValue(13,i)).doubleValue() );
	                payr.setRefinanceCost( ((Float)data.getValue(15,i)).doubleValue() );
	                payr.setConstructionCost(((Float)data.getValue(19,i)).doubleValue());
	                payr.setCommercialCost(((Float)data.getValue(21,i)).doubleValue());
	                payr.setOECost(((Float)data.getValue(23,i)).doubleValue());
	                payr.setLiensCost(((Float)data.getValue(25,i)).doubleValue());
	                payr.setAcreageCost(((Float)data.getValue(27,i)).doubleValue());
	                payr.setSublotCost(((Float)data.getValue(29,i)).doubleValue());	
                
                	payr.setSearchValue(((Float)data.getValue(4,i)).doubleValue());
	                payr.setUpdateValue(((Float)data.getValue(5,i)).doubleValue());
	                payr.setCurrentOwnerValue( ((Float)data.getValue(12,i)).doubleValue() );
	                payr.setRefinanceValue( ((Float)data.getValue(14,i)).doubleValue() );
	                payr.setConstructionValue(((Float)data.getValue(18,i)).doubleValue());
	                payr.setCommercialValue(((Float)data.getValue(20,i)).doubleValue());      
	                payr.setOEValue(((Float)data.getValue(22,i)).doubleValue());    
	                payr.setLiensValue(((Float)data.getValue(24,i)).doubleValue());
	                payr.setAcreageValue(((Float)data.getValue(26,i)).doubleValue());
	                payr.setSublotValue(((Float)data.getValue(28,i)).doubleValue());
	                
	                payr.setIndexA2C(((Float)data.getValue(Payrate.FIELD_INDEX_A2C,0)));
	                payr.setIndexC2A(((Float)data.getValue(Payrate.FIELD_INDEX_C2A,0)));
	                payr.setFvsValue(((Float)data.getValue(Payrate.FIELD_FVS_C2A, 0)));
	                payr.setFvsCost(((Float)data.getValue(Payrate.FIELD_FVS_A2C, 0)));
                
	                toPayrates.put(countyId, payr);
	        	}
			}
	        
	        // at this point I should have in "toPayrates" only the latest values of payrate for destination community
	        //now I must do a union between fromPayrates and toPayrates and also take into account the user's attribute
	        
	        //check every payrate in fromPayrate and if we have something in fromPayrate we use those fields
	        Set<Long> fromKeys = fromPayrates.keySet();
	        for (Iterator iter = fromKeys.iterator(); iter.hasNext();) {
				Long county = (Long) iter.next();
				Payrate payrate = fromPayrates.get(county);
				Payrate toPayrate = toPayrates.get(county); 
				if(toPayrate==null){
					//the payrate wasn't previosly set in the destination community 
					//so I will use the default values for the fields currently not set
					if(ua.isTSAdmin()){
						payrate.setSearchValue(PayrateConstants.SEARCH_VALUE);
		                payrate.setUpdateValue(PayrateConstants.UPDATE_COST);
		                payrate.setCurrentOwnerValue(PayrateConstants.CURRENTOWNER_VALUE);
		                payrate.setRefinanceValue(PayrateConstants.REFINANCE_VALUE);
		                payrate.setConstructionValue(PayrateConstants.CONSTRUCTION_VALUE);
		                payrate.setCommercialValue(PayrateConstants.COMMERCIAL_VALUE);      
		                payrate.setOEValue(PayrateConstants.OE_VALUE);    
		                payrate.setLiensValue(PayrateConstants.LIENS_VALUE);
		                payrate.setAcreageValue(PayrateConstants.ACREAGE_VALUE);
		                payrate.setSublotValue(PayrateConstants.SUBLOT_VALUE);
		                payrate.setIndexC2A(PayrateConstants.INDEX_C2AGENT);
		                payrate.setFvsValue(PayrateConstants.FVS_VALUE);
					} else {
						Map implemented = HashCountyToIndex.getAllCounty();
						if(implemented.get(county.toString())==null){
							// if the county is not implemented 
				            payrate.setSearchCost(PayrateConstants.SEARCH_COST_UNINPLEMENTED);
							payrate.setUpdateCost(PayrateConstants.UPDATE_COST_UNINPLEMENTED);				
							payrate.setCurrentOwnerCost( PayrateConstants.CURRENTOWNER_COST_UNINPLEMENTED );
							payrate.setRefinanceCost( PayrateConstants.REFINANCE_COST_UNINPLEMENTED );
							payrate.setConstructionCost(PayrateConstants.CONSTRUCTION_COST_UNINPLEMENTED);
							payrate.setCommercialCost(PayrateConstants.COMMERCIAL_COST_UNINPLEMENTED);
							payrate.setOECost(PayrateConstants.OE_COST_UNINPLEMENTED);
							payrate.setLiensCost(PayrateConstants.LIENS_COST_UNINPLEMENTED);
							payrate.setAcreageCost(PayrateConstants.ACREAGE_COST_UNINPLEMENTED);
							payrate.setSublotCost(PayrateConstants.SUBLOT_COST_UNINPLEMENTED);
							payrate.setIndexA2C(PayrateConstants.INDEX_A2C);
							payrate.setFvsCost(PayrateConstants.FVS_COST_UNINPLEMENTED);
						} else {
				            payrate.setSearchCost(PayrateConstants.SEARCH_COST);
							payrate.setUpdateCost(PayrateConstants.UPDATE_COST);				
							payrate.setCurrentOwnerCost( PayrateConstants.CURRENTOWNER_COST );
							payrate.setRefinanceCost( PayrateConstants.REFINANCE_COST );
							payrate.setConstructionCost(PayrateConstants.CONSTRUCTION_COST);
							payrate.setCommercialCost(PayrateConstants.COMMERCIAL_COST);
							payrate.setOECost(PayrateConstants.OE_COST);
							payrate.setLiensCost(PayrateConstants.LIENS_COST);
							payrate.setAcreageCost(PayrateConstants.ACREAGE_COST);
							payrate.setSublotCost(PayrateConstants.SUBLOT_COST);
							payrate.setIndexC2A(PayrateConstants.INDEX_C2AGENT);
							payrate.setFvsCost(PayrateConstants.FVS_COST);
						}
						
					} //end if(ua.isTSAdmin())
					
				} else {
					//the payrate was set in the destination community so i will use those fields to complete the payrate
					if(ua.isTSAdmin()){
						payrate.setSearchValue(toPayrate.getSearchValue());
		                payrate.setUpdateValue(toPayrate.getUpdateValue());
		                payrate.setCurrentOwnerValue(toPayrate.getCurrentOwnerValue());
		                payrate.setRefinanceValue(toPayrate.getRefinanceValue());
		                payrate.setConstructionValue(toPayrate.getConstructionValue());
		                payrate.setCommercialValue(toPayrate.getCommercialValue());      
		                payrate.setOEValue(toPayrate.getOEValue());    
		                payrate.setLiensValue(toPayrate.getLiensValue());
		                payrate.setAcreageValue(toPayrate.getAcreageValue());
		                payrate.setSublotValue(toPayrate.getSublotValue());
		                payrate.setIndexC2A(toPayrate.getIndexC2A());
		                payrate.setFvsValue(toPayrate.getFvsValue());
					} else {
						payrate.setSearchCost(toPayrate.getSearchCost());
		                payrate.setUpdateCost(toPayrate.getUpdateCost());
		                payrate.setCurrentOwnerCost(toPayrate.getCurrentOwnerCost());
		                payrate.setRefinanceCost(toPayrate.getRefinanceCost());
		                payrate.setConstructionCost(toPayrate.getConstructionCost());
		                payrate.setCommercialCost(toPayrate.getCommercialCost());      
		                payrate.setOECost(toPayrate.getOECost());    
		                payrate.setLiensCost(toPayrate.getLiensCost());
		                payrate.setAcreageCost(toPayrate.getAcreageCost());
		                payrate.setSublotCost(toPayrate.getSublotCost());
		                payrate.setIndexA2C(toPayrate.getIndexA2C());
		                payrate.setFvsCost(toPayrate.getFvsCost());
					}
				}
			}
	        //now we have in the fromPayrates the correct payrates that are to be exported
	        //we must check if there are some payrates in toPayrates that were not processed
	        //these payrates should be set to the default value and added in the fromPayrates object
	        
	        Set<Long> toKeys = toPayrates.keySet();
	        HashMap<Long, Payrate> tempPayrates = new HashMap<Long, Payrate>();
	        for (Iterator iter = toKeys.iterator(); iter.hasNext();) {
				Long toKey = (Long) iter.next();
				//if i don't find the key in the fromPayrates it means i will have to set this payrate to the default value
				Payrate fromPayrate = fromPayrates.get(toKey);
				Payrate toPayrate = toPayrates.get(toKey);
				if(fromPayrate==null){
					//must set some fields in toPayrate to default value and insert the payrate in the fromPayrates hashmap
					if(ua.isTSAdmin()){
						Map implemented = HashCountyToIndex.getAllCounty();
						if(implemented.get(toKey.toString())==null){
							// if the county is not implemented 
				            toPayrate.setSearchCost(PayrateConstants.SEARCH_COST_UNINPLEMENTED);
							toPayrate.setUpdateCost(PayrateConstants.UPDATE_COST_UNINPLEMENTED);				
							toPayrate.setCurrentOwnerCost( PayrateConstants.CURRENTOWNER_COST_UNINPLEMENTED );
							toPayrate.setRefinanceCost( PayrateConstants.REFINANCE_COST_UNINPLEMENTED );
							toPayrate.setConstructionCost(PayrateConstants.CONSTRUCTION_COST_UNINPLEMENTED);
							toPayrate.setCommercialCost(PayrateConstants.COMMERCIAL_COST_UNINPLEMENTED);
							toPayrate.setOECost(PayrateConstants.OE_COST_UNINPLEMENTED);
							toPayrate.setLiensCost(PayrateConstants.LIENS_COST_UNINPLEMENTED);
							toPayrate.setAcreageCost(PayrateConstants.ACREAGE_COST_UNINPLEMENTED);
							toPayrate.setSublotCost(PayrateConstants.SUBLOT_COST_UNINPLEMENTED);	
							toPayrate.setIndexA2C(PayrateConstants.INDEX_A2C);
							toPayrate.setFvsCost(PayrateConstants.FVS_COST_UNINPLEMENTED);
						} else {
				            toPayrate.setSearchCost(PayrateConstants.SEARCH_COST);
							toPayrate.setUpdateCost(PayrateConstants.UPDATE_COST);				
							toPayrate.setCurrentOwnerCost( PayrateConstants.CURRENTOWNER_COST );
							toPayrate.setRefinanceCost( PayrateConstants.REFINANCE_COST );
							toPayrate.setConstructionCost(PayrateConstants.CONSTRUCTION_COST);
							toPayrate.setCommercialCost(PayrateConstants.COMMERCIAL_COST);
							toPayrate.setOECost(PayrateConstants.OE_COST);
							toPayrate.setLiensCost(PayrateConstants.LIENS_COST);
							toPayrate.setAcreageCost(PayrateConstants.ACREAGE_COST);
							toPayrate.setSublotCost(PayrateConstants.SUBLOT_COST);
							toPayrate.setIndexA2C(PayrateConstants.INDEX_A2C);
							toPayrate.setFvsCost(PayrateConstants.FVS_COST);
						}
					} else {
						toPayrate.setSearchValue(PayrateConstants.SEARCH_VALUE);
		                toPayrate.setUpdateValue(PayrateConstants.UPDATE_COST);
		                toPayrate.setCurrentOwnerValue(PayrateConstants.CURRENTOWNER_VALUE);
		                toPayrate.setRefinanceValue(PayrateConstants.REFINANCE_VALUE);
		                toPayrate.setConstructionValue(PayrateConstants.CONSTRUCTION_VALUE);
		                toPayrate.setCommercialValue(PayrateConstants.COMMERCIAL_VALUE);      
		                toPayrate.setOEValue(PayrateConstants.OE_VALUE);    
		                toPayrate.setLiensValue(PayrateConstants.LIENS_VALUE);
		                toPayrate.setAcreageValue(PayrateConstants.ACREAGE_VALUE);
		                toPayrate.setSublotValue(PayrateConstants.SUBLOT_VALUE);
		                toPayrate.setIndexC2A(PayrateConstants.INDEX_C2AGENT);
		                toPayrate.setFvsValue(PayrateConstants.FVS_VALUE);
					}
					tempPayrates.put(toKey, toPayrate);
				}
			}
	        fromPayrates.putAll(tempPayrates);		//add the new payrates that should have the default value
            // ----------------------------------
            fromKeys = fromPayrates.keySet();
            for (Iterator iter = fromKeys.iterator(); iter.hasNext();) {
				Payrate payr = fromPayrates.get((Long) iter.next());
				payr.setStartDate(startDate.getTime());
				payr.setCommId(commId);
				DBManager.insertPayrate(payr, ua);
			}
            
	        
	    } catch (Exception e) {
        	e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
		
	}
	
	/**
	 * Export due dates from community fromComm to community toComm
	 * @param fromComm source 
	 * @param toComm destination
	 */
	public static void exportDueDates(int fromComm, int toComm) {
		
		String sql;
	    DBConnection conn = null;
	    DatabaseData data;
	    String sCountyDate, sCityDate;
        
	    try {   
	        conn = ConnectionPool.getInstance().requestConnection();
	        sql = " delete from "+ TABLE_DUE_DATE +" where comm_id = " + toComm; 
	        conn.executeUpdate(sql);	//deleting all the old data
	        
	        sql = " select a.ID, a.COUNTY_ID, a.COMM_ID, " + 
				"DATE_FORMAT(a.DUE_DATE, '%d-%m-%Y %H:%i:%S'), " +
				"DATE_FORMAT(a.CITY_DUE_DATE, '%d-%m-%Y %H:%i:%S') " +
		        "from "+ TABLE_DUE_DATE +" a " +
		        "where a.comm_id = " + fromComm;
	        
	        data = conn.executeSQL(sql);
	        int rows = data.getRowNumber();
	        //if we have data for this community
	        if(rows>0){
	        	for (int i = 0; i < rows; i++) {
					sCountyDate	= "str_to_date( '" + (String)data.getValue(4,i) + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
					sCityDate 	= "str_to_date( '" + (String)data.getValue(5,i) + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
					
					sql = "insert into " + TABLE_DUE_DATE + " (COUNTY_ID, COMM_ID, DUE_DATE, CITY_DUE_DATE ) VALUES ( " + 
                    	(BigInteger)data.getValue(2, i) + ", " + toComm + ", " + sCountyDate + ", " + sCityDate + " ) ";
					conn.executeUpdate(sql);;
					
				}
	        }
	    } catch (Exception e) {
	    	e.printStackTrace();
	    } finally {
	        if (conn != null) {
	            try {
	                ConnectionPool.getInstance().releaseConnection(conn);
	            } catch (Exception e) {}
	        }
	    }
	}
	
	/**
	 * Import due dates from payrates data to community "toComm"
	 * @param payrates
	 * @param toComm
	 */
	public static void importDueDates(HashMap<Long, Payrate> payrates, long toComm){
		String sql;
	    DBConnection conn = null;
	    String sCountyDate, sCityDate;

	    try {   
	        conn = ConnectionPool.getInstance().requestConnection();
	        sql = " delete from "+ TABLE_DUE_DATE +" where comm_id = " + toComm; 
	        conn.executeUpdate(sql);	//deleting all the old data
	        Set<Long> counties = payrates.keySet();
	        int rows = counties.size();
	        //if we have data for this community
	        if(rows>0){
	        	FormatDate formatDate = new FormatDate("dd-MM-yyyy HH:mm:ss");
	        	for (Iterator iter = counties.iterator(); iter.hasNext();) {
					Payrate payr = payrates.get((Long) iter.next());
					
					sCountyDate	= "str_to_date( '" + formatDate.getDate(payr.getDueDate()) + "' , '%d-%m-%Y %H:%i:%S' )";
					sCityDate 	= "str_to_date( '" + formatDate.getDate(payr.getCityDueDate()) + "' , '%d-%m-%Y %H:%i:%S' )";
					
					sql = "insert into " + TABLE_DUE_DATE + " (COUNTY_ID, COMM_ID, DUE_DATE, CITY_DUE_DATE ) VALUES ( " + 
                    	payr.getCountyId() + ", " + toComm + ", " + sCountyDate + ", " + sCityDate + " ) ";
					if(payr.getCountyId() > 0) {
						conn.executeUpdate(sql);
					}	
				}
	        }
	    } catch (Exception e) {
	    	e.printStackTrace();
	    } finally {
	        if (conn != null) {
	            try {
	                ConnectionPool.getInstance().releaseConnection(conn);
	            } catch (Exception e) {}
	        }
	    }
	}
	
	public static Vector<Payrate> getLatestPayratesForCommunity(long commId, Date time) {
		String sessionOffset = getDBOffset();
		String sql = 
			" select " +
			" a.id, a.start_date, a.end_date, " + 
			"DATE_FORMAT(b.due_date + " + sessionOffset + ", '%d-%m-%Y %H:%i:%S'), " +
			"DATE_FORMAT(b.city_due_date + " + sessionOffset + ", '%d-%m-%Y %H:%i:%S'), " + 
			"a.community_id, a.county_id, " +
			"a.search_value, a.search_cost, a.update_value, a.update_cost, " + 
			"a.currentowner_value, a.currentowner_cost, a.refinance_value, a.refinance_cost, " + 
			"a.construction_value, a.construction_cost, a.commercial_value, a.commercial_cost, " +
			"a.oe_value, a.oe_cost, a.liens_value, a.liens_cost, " +
			"a.acreage_value, a.acreage_cost, a.sublot_value, a.sublot_cost, " + Payrate.FIELD_INDEX_A2C + ", " + Payrate.FIELD_INDEX_C2A +
			", " + Payrate.FIELD_FVS_C2A + ", " +  Payrate.FIELD_FVS_A2C +
		" from "+ TABLE_PAYRATE +" a " + 
		" LEFT JOIN " + TABLE_DUE_DATE + " b ON a.county_id = b.county_id and a.community_id=b.comm_id " +
		" where community_id = " + commId + " and start_date <= now() " + 
		" order by start_date desc, id desc";
            
	    DBConnection conn = null;
	    DatabaseData data;
	    Vector<Payrate> fromPayrates = null;
	    HashMap<Long, Boolean> fromFound = null;
	    try {   
	        conn = ConnectionPool.getInstance().requestConnection();
	        data = conn.executeSQL(sql);
	        fromPayrates = new Vector<Payrate>();
	        fromFound = new HashMap<Long, Boolean>();
	        
	        for (int i = 0; i < data.getRowNumber(); i++) {
	        	Long countyId = (Long)data.getValue(7,i);
	        	if(countyId!=null && fromFound.get(countyId)==null){
					Payrate payr = new Payrate();
	            	payr.setId(Long.parseLong(data.getValue(1,i).toString()));
	            	payr.setCommId(((Float)data.getValue(6,i)).longValue());
	            	payr.setCountyId(countyId);
	                //payr.setStartDate(FormatDate.getDateFromFormatedString((String)data.getValue(2,i), FormatDate.TIMESTAMP));
	                payr.setSearchValue(((Float)data.getValue(8,i)).doubleValue());
	                payr.setSearchCost(((Float)data.getValue(9,i)).doubleValue());	                
	                payr.setUpdateValue(((Float)data.getValue(10,i)).doubleValue());	                	                
	                payr.setUpdateCost(((Float)data.getValue(11,i)).doubleValue());
	                	                
	                payr.setCurrentOwnerValue( ((Float)data.getValue(12,i)).doubleValue() );
	                payr.setCurrentOwnerCost( ((Float)data.getValue(13,i)).doubleValue() );
	                payr.setRefinanceValue( ((Float)data.getValue(14,i)).doubleValue() );
	                payr.setRefinanceCost( ((Float)data.getValue(15,i)).doubleValue() );
	                payr.setConstructionValue( ((Float)data.getValue(16,i)).doubleValue() );
	                payr.setConstructionCost( ((Float)data.getValue(17,i)).doubleValue() );
	                payr.setCommercialValue( ((Float)data.getValue(18,i)).doubleValue() );
	                payr.setCommercialCost( ((Float)data.getValue(19,i)).doubleValue() );	
	                payr.setOEValue( ((Float)data.getValue(20,i)).doubleValue() );
	                payr.setOECost( ((Float)data.getValue(21,i)).doubleValue() );	 
	                payr.setLiensValue( ((Float)data.getValue(22,i)).doubleValue() );
	                payr.setLiensCost( ((Float)data.getValue(23,i)).doubleValue() );	
	                payr.setAcreageValue( ((Float)data.getValue(24,i)).doubleValue() );
	                payr.setAcreageCost( ((Float)data.getValue(25,i)).doubleValue() );	  
	                payr.setSublotValue( ((Float)data.getValue(26,i)).doubleValue() );
	                payr.setSublotCost( ((Float)data.getValue(27,i)).doubleValue() );	 
	                payr.setIndexA2C(((Float)data.getValue(Payrate.FIELD_INDEX_A2C,i)));
	                payr.setIndexC2A(((Float)data.getValue(Payrate.FIELD_INDEX_C2A,i)));
	                payr.setFvsValue(((Float) data.getValue(Payrate.FIELD_FVS_C2A, i)));
	                payr.setFvsCost(((Float) data.getValue(Payrate.FIELD_FVS_A2C, i)));
	                
	                if(data.getValue(4, i)!=null)
	                	payr.setDueDate(FormatDate.getDateFromFormatedStringGMT((String)data.getValue(4,i), FormatDate.TIMESTAMP));
	                if(data.getValue(5, i)!=null)
	                	payr.setCityDueDate(FormatDate.getDateFromFormatedStringGMT((String)data.getValue(5,i), FormatDate.TIMESTAMP));
	                fromFound.put(countyId, true);
	                fromPayrates.add(payr);
	        	}
			}
	    } catch (Exception e) {
	    	e.printStackTrace();
	    } finally {
	        if (conn != null) {
	            try {
	                ConnectionPool.getInstance().releaseConnection(conn);
	            } catch (Exception e) {}
	        }
	    }
		return fromPayrates;
	}

	//-------------- ADDDED GENERAL PURPOSE FUNCTION TO ACCESS MySql Data ---- //
	
	
	/**
	 * Fills a vector with data retrieved from database.Each record is a hashmap with values from a  table row
	 * Warning: not SQL injection safe
	 *  @param tableName
	 *  @param where
	 *  @param orderBy
	 *  return Vector data 
	 */
	 public static Vector fetchData(String tableName,String where, String orderBy){
		 
	     String sql 		= "SELECT * FROM "+tableName
								+ ((where!=null)?" WHERE "+where:"")
								+ ((orderBy!=null)?" ORDER BY "+orderBy:"");
		return executeSelect(sql);
	 }
	
	
	 /**  
     *  Fill a hashmap with values received from specified table with corresponding id
     *  Warning: not SQL injection safe
     * @param templateId
     * @return
     */
    public static HashMap fetchOneRow(int id, String tableName,String tableIdColumn){
    	
    	String   sql       =  "SELECT * FROM " + tableName 
		   					  +  " WHERE " +tableIdColumn +" = " + id;	
    	return (HashMap)executeSelect(sql).get(0);
    }
	
    
    /**
     * Executes a query string.Returns a vector of hashmaps
     * @param sql
     * @return Vector data
     */
    public static Vector executeSelect (String sql){
    	
   	 Vector<HashMap<String, Object>> data 	= new Vector<HashMap<String, Object>>();
	 HashMap<String, Object> map = null;
	 DBConnection conn = null;
	 DatabaseData dbData;
	 String columnName;	 
	 logger.debug(sql);
	  try{
		  conn  = ConnectionPool.getInstance().requestConnection();
		     dbData   = conn.executeSQL(sql);
				for (int i = 0; i<dbData.getRowNumber(); i++){
					map                = new HashMap<String, Object>();
					for (int j=0;j<dbData.getColumnNumber();j++){
						columnName = dbData.getColumnName(j+1).toLowerCase();							
						map.put(columnName, dbData.getValue(columnName,i));
				     }						
					data.add(map);
				}
	  } catch (BaseException e) {
			logger.error(e);
		} finally {
	        try {
	            ConnectionPool.getInstance().releaseConnection(conn);
	        } catch (BaseException e) {
	            logger.error(e);
	        }
       }
	return data;
    	
    }
    
    
	/**
	 * Removes data from specified table based on where condition
	 * If where is empty, then delete process has no effect (to avoid accidentally mass deletes)
	 * @param tableName String
	 * @param where String
	 */
    public static boolean executeDelete (String tableName,String where){
    	
    	DBConnection conn = null;
    	String sql = "";
    	boolean deleteWithSucces = true;
    	try {
			conn = ConnectionPool.getInstance().requestConnection();  
			
			sql      = "DELETE FROM " + tableName 
					   + " WHERE " + ((where.length()>1)?where:"1=2");
			logger.info(sql);
			conn.executeSQL(sql); 
    	}
			catch (BaseException e) {
				logger.error(e);
				deleteWithSucces = false;
			} finally {
		        try {
		            ConnectionPool.getInstance().releaseConnection(conn);
		        } catch (BaseException e) {
		            logger.error(e);
		      }
		  }
	return deleteWithSucces;
    }
    
    
    /**
     * Executes an update on the specified database.
     * If where is empty, then update process has no effect (to avoid accidentally mass updates)
     * Please make sure that the 'where' condition is sql-injection safe
     * @param tableName String
     * @param where String
     * @param fieldsAndValues String
     */
    public static boolean executeUpdate(String tableName, String where, HashMap fieldsAndValues){
    	
    	boolean updateWithSucces = true;    	
    	Set set     				= fieldsAndValues.entrySet();
    	Iterator it					= set.iterator();
    	String sql 				= "";
    	String updateString	= "";
    	Object[] values = new Object[set.size()];
    	int i=0;    	
    	
 	    while (it.hasNext())
	    {
		   Map.Entry me = (Map.Entry)it.next();
		   updateString  += me.getKey().toString()+"= ?,";
		   values[i++] = fieldsAndValues.get(me.getKey()).toString();
	    }
	   
	   //removes last comma
	    updateString    = updateString.substring(0,updateString.length()-1);

	    sql    = " UPDATE "+tableName
		 + " SET " + updateString
		 + " WHERE " + ((where.length()>1)?where:"1=2");
	    logger.info(sql);

		try {
			DBManager.getSimpleTemplate().update(sql, values);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e);
			updateWithSucces = false;
		}
    		
     return updateWithSucces;
    }
 
 /**
  * Executes an insert on the specified tableName
  * Please make sure that fieldsAndValues is sql-injection safe
  * @param tableName
  * @param fieldsAndValues
  */ 
public static boolean executeInsert(String tableName, HashMap fieldsAndValues){
	
	boolean insertWithSucces = true;
	DBConnection conn = null;    	
	Set set     				= fieldsAndValues.entrySet();
	Iterator it					= set.iterator();
	String sql 				= "";
	String fields			= "";
	String values			= "";
	   while (it.hasNext())
	   {
		   Map.Entry me   = (Map.Entry)it.next();
		   fields	  += me.getKey().toString()+",";
		   values     +=fieldsAndValues.get(me.getKey()).toString()+",";
	   }
	   //removes last comma
	   fields      = fields.substring(0,fields.length()-1);
	   values    = values.substring(0,values.length()-1);
	   
   	  try {
		 conn = ConnectionPool.getInstance().requestConnection();
		 sql    = " INSERT INTO "+tableName
				 + " ( " + fields +")"
				 + " VALUES (" + values + ")";				 
		 logger.info(sql);
		 
	 	conn.executeSQL(sql);
	}
	   catch (BaseException e) {
		      logger.error(e);
		      insertWithSucces = false;
	    } finally {
	        try {
	            ConnectionPool.getInstance().releaseConnection(conn);
	        } catch (BaseException e) {
	            logger.error(e);
	        }
     }  
	return insertWithSucces;
}

/**
 * Adds slashes when insert String values
 * @param value
 * @return escaped String
 */
public static String escape(String value){		
	
	   String escapedValue = value.replaceAll("'", "''");	 
	   			 escapedValue = escapedValue.replaceAll("\\\\", "\\\\\\\\"); 	   			 
	 return 	 "'" + escapedValue +"'";
}   

/**
 * Guard against SQL injection:
 * Clean up the table name (remove potentialy dangerous characters) 
 * @param tableName
 * @return tableName without unsafe SQL characters
 */
public static String sqlTableName(String tableName) {
	String removeFirstChars = "[^a-zA-Z]*";
	tableName = tableName.replaceFirst(removeFirstChars, "");
	String removeChars = "([^a-zA-Z0-9_])";
	return tableName.replaceAll(removeChars, "");
}

/**
 * Guard against SQL injection:
 * Clean up the column name (remove potentialy dangerous characters) 
 * @param columnName
 * @return columnName without unsafe SQL characters
 */
public static String sqlColumnName(String columnName) {
	String removeFirstChars = "[^a-zA-Z]*";
	columnName = columnName.replaceFirst(removeFirstChars, "");
	String removeChars = "([^a-zA-Z0-9_])";
	return columnName.replaceAll(removeChars, "");
}

/**
 * Guard against SQL injection:
 * Returns one of the follwing words: ASC, DESC, ASCENDING, DESCENDING
 * @param orderType
 * @return a valid orderType
 */
public static String sqlOrderType(String orderType) {
	if(orderType.equalsIgnoreCase("ASC") || orderType.equalsIgnoreCase("DESC") || 
			orderType.equalsIgnoreCase("ASCENDING") || orderType.equalsIgnoreCase("DESCENDING")) return orderType;
	else return "ASC";
}	
	/**
	 * Returns an array containing all searches id (id column in TS_SEARCH) in the given interval, for searches that have been invoiced
	 * @param countyId
	 * @param abstractorId
	 * @param agentId
	 * @param stateId
	 * @param fromDay
	 * @param fromMonth
	 * @param fromYear
	 * @param toDay
	 * @param toMonth
	 * @param toYear
	 * @param commId
	 * @param isAdmin
	 * @return
	 */
	public static long[] getSearchesFromIntervalInvoiced( int[] countyId, int[] abstractorId, int[] agentId, int[] stateId, 
			String[] compName,
			int fromDay, int fromMonth, int fromYear, 
			int toDay, int toMonth, int toYear, 
			int commId, int isAdmin){
		DBConnection conn = null;
        String sql;
        sql = "call getIntervalSearchesToBeDiscounted(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        long[] searchIds = new long[0];
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            
            NewProxyCallableStatement cs = (NewProxyCallableStatement)conn.prepareCall(sql);
            
            cs.setString(1, "," + Util.getStringFromArray(countyId) + ",");
            cs.setString(2, "," + Util.getStringFromArray(abstractorId) + ",");
            cs.setString(3, "," + Util.getStringFromArray(agentId) + ",");
            cs.setString(4, "," + Util.getStringFromArray(stateId) + ",");
            cs.setString(5, StringUtils.convertStringToHexString("," + Util.getStringFromStringArray(compName) + ","));
            cs.setInt(6, fromDay);
            cs.setInt(7, fromMonth);
            cs.setInt(8, fromYear);
            cs.setInt(9, toDay);
            cs.setInt(10, toMonth);
            cs.setInt(11, toYear);
            cs.setInt(12, commId);
            cs.setInt(13, isAdmin);
            
            
            DatabaseData data = conn.executeCallableStatementWithResult(cs);
            searchIds = new long[data.getRowNumber()];
            if(data.getRowNumber()!=0){
            	for (int i = 0; i < searchIds.length; i++) {
					searchIds[i] = ((BigInteger)data.getValue("ID", i)).longValue();
				}
            }
            
            
        } catch (Exception e) {
        	e.printStackTrace();
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
        
        
		return searchIds;
	}
	
	public static Vector<Long> getSearchesWithContext(Vector<Long> searches) {
		Vector<Long> exists = new Vector<Long>();
		
		if(searches.size()==0){
			return exists;
		}
		DBConnection conn = null;
		
		Long[] searchesArray = new Long[searches.size()];
		searchesArray = searches.toArray(searchesArray);
		
        String stm = "select a." + DBConstants.FIELD_SEARCH_ID + 
        	" from " + TABLE_SEARCH + " a " +
        	" join " + DBConstants.TABLE_SEARCH_FLAGS + " sf " +
        	" on a." + DBConstants.FIELD_SEARCH_ID + " = sf." + DBConstants.FIELD_SEARCH_FLAGS_ID + 
        	" where a." + DBConstants.FIELD_SEARCH_ID + " in (" + Util.getStringFromLongArray(searchesArray) + ") " +
        	" and " + DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + " = 0 ";
        
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData data = conn.executeSQL(stm);

            for (int i = 0; i < data.getRowNumber() ; i++) {
				long searchId = ((BigInteger)data.getValue(1, i)).longValue();
				if(searchId>0){
					exists.add(searchId);
				}
			}
            
        } catch (BaseException e) {
            logger.error(e);
            e.printStackTrace();
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
		return exists;
	}
/*
	public static PresenceTestSettings getGlobalSettingsPresence(String serverAlias){
		JdbcTemplate jdbct = ConnectionPool.getInstance().getTemplate();
		Collection serverSettings= jdbct.query(
			    "select * from " + DBConstants.TABLE_PRESENCE_TEST_GLOBAL_SETTING,
			    new RowMapper() {

			        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			            PresenceTestSettings sp = new PresenceTestSettings(-1, rs.getInt("run_interval"),
			            		rs.getInt("retries"), -1,
			            		rs.getObject("start_inactive") == null? -1:rs.getInt("start_inactive"),
			            		rs.getObject("stop_inactive")== null? -1 :rs.getInt("stop_inactive")		);
			            return sp;
			        }
			    });
		if (serverSettings.size() != 1){
			return new PresenceTestSettings();
		} else {
			return (PresenceTestSettings)serverSettings.iterator().next();
		}
	}
	
	public static Collection<TestSettingsI> getSettingsPresence(String serverAlias){
		JdbcTemplate jdbct = ConnectionPool.getInstance().getTemplate();
		Collection<TestSettingsI> serverSettings= jdbct.query(
		    "select * from " + DBConstants.TABLE_PRESENCE_TEST_SITE_SETTING + " where server = " + DBManager.getServerIdByAlias(serverAlias),
		    new RowMapper() {

		        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
		            PresenceTestSettings sp = new PresenceTestSettings(rs.getInt("test_id"),
		            		rs.getInt("run_interval"), rs.getInt("retries"), rs.getInt("group_by"), 
		            		rs.getObject("start_inactive") == null? -1:rs.getInt("start_inactive"),
		            		rs.getObject("stop_inactive")== null? -1 :rs.getInt("stop_inactive")	);
		            return sp;
		        }
		    });
		return serverSettings;
	}
	
	
	public static Collection<LogEntryI> getLastStatusPresence(String serverAlias){
		JdbcTemplate jdbct = ConnectionPool.getInstance().getTemplate();
		Collection<LogEntryI> serverSettings= jdbct.query(
			    "select test_id, test_date, error_type from " + DBConstants.TABLE_PRESENCE_TEST_LOG 
			    + " where log_id in (select max(log_id) from " + DBConstants.TABLE_PRESENCE_TEST_LOG 
			    + " group by test_id having server= " + DBManager.getServerIdByAlias(serverAlias) + ")",
			    new RowMapper() {

			        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			            LogEntry le = new LogEntry(rs.getInt("test_id"), 
			            							rs.getLong("test_date"),
			            							rs.getInt("error_type"));
			            return le;
			        }
			    });
			return serverSettings;
	}
	
	public static int getServerIdByAlias(String alias){
		return ConnectionPool.getInstance().getTemplate().queryForInt("select id from " + DBConstants.TABLE_SERVER + " where alias ='"+alias+"'");
	}
*/	
	public static ServerSourceEntry[] getServerSources() {
		ServerSourceEntry[] serverSources = new ServerSourceEntry[0];
		String sql = "SELECT * FROM " + TABLE_LBS_SOURCES + 
			" ORDER BY " + FIELD_LBS_SOURCES_ID + " ASC ";
		
		DBConnection conn = null;
		try {
            
            conn  = ConnectionPool.getInstance().requestConnection();
            DatabaseData data = conn.executeSQL(sql);
            String temp = null;
            serverSources = new ServerSourceEntry[data.getRowNumber()];
            for (int i = 0; i < data.getRowNumber() ; i++) {
				ServerSourceEntry sse = new ServerSourceEntry();
				sse.setAddress((String)data.getValue(FIELD_LBS_SOURCES_ADDRESS, i));
				temp = (String)data.getValue(FIELD_LBS_SOURCES_CLNT_COMMNAME, i);
				if(temp!=null)
					sse.setClientCommname(temp);
				temp = (String)data.getValue(FIELD_LBS_SOURCES_CLNT_USERNAME, i);
				if(temp!=null)
					sse.setClientUsername(temp);
				sse.setEnable((Integer)data.getValue(FIELD_LBS_SOURCES_ENABLE, i));
				sse.setId((Integer)data.getValue(FIELD_LBS_SOURCES_ID, i));
				sse.setNetmask((Integer)data.getValue(FIELD_LBS_SOURCES_NETMASK, i));
				sse.setRedirectAddress((String)data.getValue(FIELD_LBS_SOURCES_REDIRECT_ADDRESS, i));
				serverSources[i] = sse;
			}
            
        } catch (BaseException e) {
            logger.error(e);
            e.printStackTrace();
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }
		
		
		return serverSources;
	}
	
	public static ServerInfoEntry[] getServersNoLoad(boolean includeDisabledServers) {
		ServerInfoEntry[] servers = new ServerInfoEntry[0];
		
		String sql = "SELECT * FROM " + TABLE_SERVER + 
			" WHERE " + FIELD_SERVER_ALIAS + " != '" + URLMaping.INSTANCE_DIR + "' " +
			(includeDisabledServers ? "" : " AND ( " + FIELD_SERVER_ENABLED + " != 0 OR " + DBConstants.FIELD_SERVER_CHECK_SEARCH_ACCESS + " = 1 ) ") +
			" ORDER BY " + FIELD_SERVER_ID + " ASC ";
		
		DBConnection conn = null;
		try {
            
            conn  = ConnectionPool.getInstance().requestConnection();
            DatabaseData data = conn.executeSQL(sql);
            servers = new ServerInfoEntry[data.getRowNumber()];
            for (int i = 0; i < data.getRowNumber() ; i++) {
				ServerInfoEntry sie = new ServerInfoEntry();
				sie.setId((Integer)data.getValue(FIELD_SERVER_ID, i));
				sie.setIp((String)data.getValue(FIELD_SERVER_IP_ADDRESS, i));
				sie.setEnabled((Integer)data.getValue(FIELD_SERVER_ENABLED,i));
				sie.setCheckSearchAccess((Boolean)data.getValue(DBConstants.FIELD_SERVER_CHECK_SEARCH_ACCESS,i));
				sie.setHostName((String)data.getValue(FIELD_SERVER_NAME, i));
				sie.setAlias((String)data.getValue(FIELD_SERVER_ALIAS, i));
				sie.setIpMask((Integer)data.getValue(FIELD_SERVER_IP_MASK, i));
				sie.setPath((String)data.getValue(FIELD_SERVER_PATH, i));
				sie.setLoadFactor(LoadBalancingStatus.BAD_LOAD);
				sie.setTimestamp(Calendar.getInstance().getTime());
				servers[i] = sie;
			}
            
        } catch (BaseException e) {
            logger.error(e);
            e.printStackTrace();
        } finally{
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                logger.error(e);
            }           
        }

		return servers;
	}
	
	public static ServerInfoEntry[] getServers() {
		
		List<ServerInfoEntry> serversInfo = getServersInfo();
		
		String status = MonitoringService.getServersStatus();
		if(status!=null){
			String[] arrayStatus = status.split(";");
			
			for (ServerInfoEntry entry : serversInfo) {
				for (int j = 0; j < arrayStatus.length; j+=3) {
					try{
						 if(entry.getId()==Integer.parseInt(arrayStatus[j])){
							 entry.setLoadFactor(Float.parseFloat(arrayStatus[j+1]));
							 entry.setTimestamp(new Date(Long.parseLong(arrayStatus[j+2])));
							 break; 
						 } 
					} catch (Exception e) {
						logger.error("getServer: " + e.getMessage());
					}
				}
			}
			
			
		}
		return serversInfo.toArray(new ServerInfoEntry[serversInfo.size()]);
	}
	
	public static byte[] getSearchOrderLogs( long searchId, int viewOption, boolean checkArchive ){
		byte[] data = null;
		DBConnection conn = null;
		boolean isArchive = false;
		boolean isLogInTable = false;
    	Date startDate = null;
    	String column = "";
    	
    	try {
    		
			switch( viewOption ){
            case FileServlet.VIEW_ORDER:
            	column = "searchOrder";
            	data = AsynchSearchLogSaverThread.readOrderLogFromSamba(searchId);
            	break;
            case FileServlet.VIEW_LOG:
            case FileServlet.VIEW_LOG_OLD_STYLE:
            	column = "searchLog";
				if (ServerConfig.isEnableLogInSamba() && viewOption != FileServlet.VIEW_LOG_OLD_STYLE) {
					int dbValueForLogOriginalLocation = 0;
					try {
						dbValueForLogOriginalLocation = getSimpleTemplate().queryForInt("SELECT " + DBConstants.FIELD_SEARCH_FLAGS_LOG_ORIGINAL_LOCATION
								+ " FROM " + DBConstants.TABLE_SEARCH_FLAGS
								+ " WHERE " + DBConstants.FIELD_SEARCH_FLAGS_ID + " = ?", searchId);
					} catch (Exception e) {
						e.printStackTrace();
						String noLogError = "Cannot find value for " + DBConstants.FIELD_SEARCH_FLAGS_LOG_ORIGINAL_LOCATION +
								" in table " + DBConstants.TABLE_SEARCH_FLAGS + " for searchId " + searchId;
						logger.error(noLogError, e);
						Log.sendExceptionViaEmail(MailConfig.getExceptionEmail(),
								"Getting " + DBConstants.FIELD_SEARCH_FLAGS_LOG_ORIGINAL_LOCATION + " from database failed", e, noLogError);
					}
					isLogInTable = dbValueForLogOriginalLocation == ServerConfig.getLogInTableVersion();
				}
				break;
            case FileServlet.VIEW_INDEX:
            	column = "searchIndex";
            	data = AsynchSearchLogSaverThread.readTsriLogFromSamba(searchId);
            	break;
            default:
            	return null;
            }
			
			if(data != null) {
				return data;
			}
            
            if(isLogInTable) {
            	long startTMS = System.currentTimeMillis();
            	if(ServerConfig.isEnableLogInSamba()) {
            		data = AsynchSearchLogSaverThread.readSearchLogFromSamba(searchId);
            	} 
				
            	long endTMS = System.currentTimeMillis();
				logger.info("timeSpent in getSearchOrderLogsInternal from searchid " + searchId  + " in miliseconds "  + ((endTMS - startTMS))  + (ServerConfig.isEnableLogInSamba()?" samba":" table"));
            } else {
            
	        	String sqlPhrase = "SELECT " + column + " FROM " +
									DBConstants.TABLE_SEARCH_DATA_BLOB + " WHERE " + 
									DBConstants.FIELD_SEARCH_DATA_BLOB_ID + " = " + searchId ;
	        	
	        	PreparedStatement pstmt;    
				conn = ConnectionPool.getInstance().requestConnection();
	        	
	        	pstmt = conn.prepareStatement(sqlPhrase);
				ResultSet rs = pstmt.executeQuery();
	
				if(rs.next() && (data = rs.getBytes(1)) != null){
					data = ZipUtils.unZipFile(searchId,data);
					pstmt.close();
				} else if(checkArchive){
					pstmt.close();
					pstmt = conn.prepareStatement(SQL_LOAD_SEARCH_DATA_INFO);
					pstmt.setLong(1, searchId);
					rs = pstmt.executeQuery();
					if(rs.next()) {
						isArchive = (rs.getInt(DBConstants.FIELD_SEARCH_FLAGS_TO_DISK) > 0)?true:false;
						startDate = rs.getTimestamp(DBConstants.FIELD_SEARCH_SDATE);
					}
					
					pstmt.close();
				} else {
					pstmt.close();
					SearchLogger.getLogger().info("getSearchOrderLogs no log found searchId=" + searchId + ", column=" + column + ", checkArchive=" + checkArchive);
				}
	    		
	    	}
    		
    		
        } catch (Exception e) {
        	e.printStackTrace();
        	SearchLogger.getLogger().error("getSearchOrderLogs searchId=" + searchId + ", column=" + column + ", checkArchive=" + checkArchive, e);
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {}
            }
        }
        
        if(isArchive) {
        	boolean smbEnabled = ServerConfig.isArchiveDestinationSmbEnabled();
        	String archivePath = null;
        	try {
        		switch( viewOption ){
	                case FileServlet.VIEW_LOG:
	                case FileServlet.VIEW_LOG_OLD_STYLE:
	                	archivePath = ArchiveEntry.getSearchLogCompletePath(searchId, startDate);
	                	break;
	                case FileServlet.VIEW_INDEX:
	                	archivePath = ArchiveEntry.getTsrIndexLogCompletePath(searchId, startDate);
	                	break;
	                default:
	                	return data;
        		}
        		
        		data = org.apache.commons.io.FileUtils.readFileToByteArray(new File(archivePath));
        		
        		data = ZipUtils.unZipFile(searchId,data);
        	} catch (Exception e) {
				logger.error("Failed to load log from archive for search " + 
						searchId + ". The path tried is " + archivePath, e );
			}
        }
		return data;
	}
	
    /**
     * Convenience method for obtaining the Spring JDBC Template
     * @return
     */
    public static SimpleJdbcTemplate getSimpleTemplate(){
    	return ConnectionPool.getInstance().getSimpleTemplate();
    }
    
    /**
     * Convenience method for obtaining the Spring JDBC Template
     * @return
     */
    public static JdbcTemplate getJdbcTemplate(){
    	return ConnectionPool.getInstance().getTemplate();
    }
    
    /**
     * Convenience method for accessing Spring Transaction Template
     * @return
     */
    public static TransactionTemplate getTransactionTemplate(){
    	return ConnectionPool.getInstance().getTransactionTemplate();
    }
    
    /**
     * Convenience method for obtaining the Spring JDBC Named Parameter Template
     * @return
     */
    public static NamedParameterJdbcTemplate getNamedParameterJdbcTemplate(){
    	return ConnectionPool.getInstance().getNamedParameterJdbcTemplate();
    }

    /**
     * Convenience method for obtaining the Spring JDBC Insert
     * @return
     */
    public static SimpleJdbcInsert getSimpleJdbcInsert(){
    	return ConnectionPool.getInstance().getSimpleJdbcInsert();
    }
    
    // used by getServersInfo
    private static final String 
    GET_SERV_INFO_SQL = "SELECT * FROM " + TABLE_SERVER + " ORDER BY " + FIELD_SERVER_ID + " ASC ";
    
    /**
     * Query DB for list of servers
     * @return list with the servers info
     */
    public static List<ServerInfoEntry> getServersInfo(){
		
	    return getSimpleTemplate().query(GET_SERV_INFO_SQL, new ServerInfoEntry());			
    }
        
    /**
     * Obtain mysql code for inserting a date into db
     * @return
     */
    public static String getMysqlTimeStamp(){
    	return "str_to_date('" + 
    			new FormatDate(FormatDate.TIMESTAMP).getDate(Calendar.getInstance().getTime()) + "' , '" +  
    				FormatDate.translateToMysql(FormatDate.TIMESTAMP) + 
    			"')";
    }
    
    /**
    * Obtain mysql code for inserting a date into db
    * @return
    */
	public static String getMysqlTimeStamp(String dateString){
	   return "str_to_date('" + dateString + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "')";
	}
    
    public static DBManager.SearchAvailabitily getOpenAvailabilityRemote( long id, long currentUserId ){
    	
        int status = SEARCH_LOCKED_BY_OTHER_REMOTE;
        UserAttributes checkedBy = null;
        boolean tsrInProgress = false;
        
    	String sqlPhr = "SELECT " + 
    		DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + ", " + 
    		DBConstants.FIELD_SEARCH_ABSTRACT_ID + ", a." +
    		DBConstants.FIELD_SEARCH_STATUS +" FROM "+ 
    		TABLE_SEARCH + " a JOIN " +
    		DBConstants.TABLE_SEARCH_FLAGS + " b ON a." +
    		DBConstants.FIELD_SEARCH_ID + " = b." + 
    		DBConstants.FIELD_SEARCH_FLAGS_ID + " WHERE a.ID = " + id;
        
        int checkedById = -1;
        int ownerId = -1;
        
        DBConnection conn = null;
        
        
      
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            DatabaseData data = conn.executeSQL(sqlPhr);
            
            if (data.getRowNumber() > 0) {
            	
            	if(data.getValue(3, 0)!=null){
            		if(Long.parseLong(data.getValue(3, 0).toString())==SEARCH_NOT_SAVED){
            			FileLogger.info( "Availability for Search id " + id + " SEARCH AVAILABLE ", FileLogger.SEARCH_OWNER_LOG );
                    	status = SEARCH_AVAILABLE;
                    	return new SearchAvailabitily(status, checkedBy, tsrInProgress);
            		}
            	} 
                
                if (data.getValue("CHECKED_BY", 0) != null) {
                    checkedById = Integer.parseInt(data.getValue("CHECKED_BY", 0).toString());
                }
                
                if( checkedById > 0 )
                {
                    checkedById = 1;
                }
                
                if (data.getValue("ABSTRACT_ID", 0) != null) {
                    ownerId = Integer.parseInt(data.getValue("ABSTRACT_ID", 0).toString());
                } 
                
                FileLogger.info( "Availability for Search id = " + id + "  owner = " + ownerId + " checkedBy = " + checkedById, FileLogger.SEARCH_OWNER_LOG );
                    
                FileLogger.info( "OPEN FROM REPORTS: Availability for Search id = " + id + "  owner = " + ownerId + " checkedBy = " + checkedById, FileLogger.SEARCH_OWNER_LOG );
	                    
                if ( ( checkedById == 1 && ownerId == currentUserId ) || checkedById == 0)
                {// doar daca este deja luat de el sau nu apartine nimanui
                    FileLogger.info( "Availability for Search id = " + id + "  owner = " + ownerId + " AVAILABLE ", FileLogger.SEARCH_OWNER_LOG );
                    status = SEARCH_AVAILABLE;
                }
            }
	    } catch (Exception e) {
	        logger.error(e);
	    } finally {
	        try {
	            ConnectionPool.getInstance().releaseConnection(conn);
	        } catch(BaseException e) {
	            logger.error(e);
	        }           
	    }
	    
	    return new SearchAvailabitily(status, checkedBy, tsrInProgress);
    }
    
    /**
     * Set community products 
     * It should be run at application start
     */
    public static DatabaseData getCommunitiesProducts(){
    	
    	String sql = "select cp.comm_id, cp.product_id, cp.product_name,  cp.shortName as product_short_name, cp.tsrViewFilter, cp.searchFrom "+
    					    "from " + DBConstants.TABLE_COMMUNITY_PRODUCTS + " cp inner join ts_community c on cp.comm_id = c.comm_id "+
    					    "UNION "+      
    					    "select c.comm_id, sp.product_id, sp.alias as product_name, sp.shortName as product_short_name, '', '' "+
    					    "from ts_community c, " + DBConstants.TABLE_SEARCH_PRODUCTS + " sp "+
    					    "where c.comm_id not in (select comm_id from " + DBConstants.TABLE_COMMUNITY_PRODUCTS + ") order by comm_id";
    	
    	DBConnection conn = null;
    	DatabaseData data = null;
    	
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
            data = conn.executeSQL(sql);
            

	    } catch (Exception e) {
	        logger.error(e);
	    } finally {
	        try {
	            ConnectionPool.getInstance().releaseConnection(conn);
	        } catch(BaseException e) {
	            logger.error(e);
	        }           
	    }
    	
	    return data;
    }
    
    /**
     * Updates community products
     *
     */
    public static void updateCommunityProducts(final long commId,final Vector<Vector<Object>> allValues){

    	getTransactionTemplate().execute(new TransactionCallback() {
      		 public Object doInTransaction(TransactionStatus status) {
      		    try{
      		    	String sqlDelete      = "delete from " + DBConstants.TABLE_COMMUNITY_PRODUCTS + " where comm_id='"+commId+"'";
      		    	String insertScript   = "insert into " + DBConstants.TABLE_COMMUNITY_PRODUCTS + " (comm_id, product_id, product_name,shortName,tsrViewFilter, searchFrom) values";
      		    	     				
      				Vector<Object> paramValues = new Vector<Object>(allValues.size()*4);
      				
      				for (Iterator<Vector<Object>> iter = allValues.iterator(); iter.hasNext();) {
						Vector<Object> insertValues = (Vector<Object>) iter.next();
						insertScript += "(?,?,?,?,?,?),";

						for (Iterator iterator = insertValues.iterator(); iterator.hasNext();) {
							Object value = (Object) iterator.next();
							paramValues.add(value);
						}
					}
      				insertScript=insertScript.substring(0,insertScript.length()-1);
      				      				
      				
      				DBManager.getSimpleTemplate().update(sqlDelete);
      				DBManager.getSimpleTemplate().update(insertScript,paramValues.toArray());
      	        	
      		    }catch(RuntimeException e){ 
      		    	// failure - rollback
      		    	logger.error(e);
      		    	status.setRollbackOnly();    		    	
      		    }
      		    return 0;
      	    }
      	});   	
    }
    
    
    
    /**
     * Get available products from the database
     * @param sortByOrder
     * @return
     */
    public static List<ProductsMapper> getProducts(boolean sortByOrder){
    	    	
    	String SQL_GET_PRODUCTS = "select * from " + DBConstants.TABLE_SEARCH_PRODUCTS + " sp ";
    	if (sortByOrder){
    		SQL_GET_PRODUCTS += " order by sp." + DBConstants.FIELD_SEARCH_PRODUCTS_ORDER;
    	} else{
    		SQL_GET_PRODUCTS += " order by sp." + DBConstants.FIELD_SEARCH_PRODUCTS_ID;
    	}
    	
      	List<ProductsMapper> products = getSimpleTemplate().query(SQL_GET_PRODUCTS, new ProductsMapper());
	    
      	return products;
    }
    
    
    private static final String SQL_GET_CONFIG_BY_NAME = "SELECT " + DBConstants.FIELD_CONFIGS_VALUE + 
		" FROM " + DBConstants.TABLE_CONFIGS + 
		" WHERE " + DBConstants.FIELD_CONFIGS_NAME + " = ?";
    /**
     * Returns the value for that configuration parameter in TS_CONFIG
     * @param name
     * @return
     */
    public static String getConfigByName(String name){
        try {            
        	return getSimpleTemplate().queryForObject(SQL_GET_CONFIG_BY_NAME, String.class, name);
	    } catch (Exception e) {
	        logger.error("Error while reading configuration for key " + name, e);
	    }
	    return null;
    }
    
    /**
     * Returns the value for that configuration parameter in TS_CONFIG as int or the defaultValue is not available
     * @param key
     * @param defaultValue
     * @return
     */
    public static int getConfigByNameAsInt(String key, int defaultValue) {
		try {
			return Integer.parseInt(getConfigByName(key));
		} catch (Exception e) {
			logger.error("Error while converting config [" + key + 
					"]to Integer value. Returning default value " + defaultValue,e);
		}
		return defaultValue;
	}
    
    
    /**
     * Retrieves community id associated to the specified tsrFileId
     * @param shortedTSRFile  
     * @return
     */
    public static long getCommunityForTSRFile(String shortedTSRFile){
    	long commId = -1;
    	
    	String sql      =  "SELECT comm_id FROM "+DBConstants.TABLE_SEARCH+
    							  " WHERE tsr_file_link LIKE ?";

    	DBConnection conn = null;
    	DatabaseData data = null;
    	
        try {
            
            conn = ConnectionPool.getInstance().requestConnection();
    		PreparedStatement pstmt = conn.prepareStatement( sql );
    		pstmt.setString( 1, "%" + shortedTSRFile + "%");
    		data = conn.executePrepQuery(pstmt);	
    		pstmt.close();	
    		
            if(data.getRowNumber()>0){	//there should be only one entry
            	return Long.parseLong(data.getValue("comm_id", 0).toString());
            }

	    } catch (Exception e) {
	        logger.error(e);
	    } finally {
	        try {
	            ConnectionPool.getInstance().releaseConnection(conn);
	        } catch(BaseException e) {
	            logger.error(e);
	        }           
	    }
    return commId;
    }
    
    public static boolean isTSRFileInCommunity(String fileName, long commId) {
    	String sql =  "SELECT COUNT(*) FROM "+DBConstants.TABLE_SEARCH+
		  " WHERE comm_id = " + commId + " AND lower(tsr_file_link) LIKE ?";

		try {
			if ( DBManager.getSimpleTemplate().queryForInt(sql,"%" + fileName.toLowerCase() + "%") > 0 ) return true;
		} catch (Exception e) {
			 logger.error(e);
		}
		return false;
    }
    
    private static final String SQL_SEARCH_FOR_UPDATE = 
        	"SELECT s." + DBConstants.FIELD_SEARCH_ID + 
        	", s." + DBConstants.FIELD_SEARCH_TYPE + 
        	", s." + DBConstants.FIELD_SEARCH_TSR_DATE + 
        	" FROM " + DBConstants.TABLE_SEARCH + 
        		" s JOIN " + DBConstants.TABLE_PROPERTY + 
        			" p ON s." + DBConstants.FIELD_SEARCH_PROPERTY_ID + " = p." + DBConstants.FIELD_PROPERTY_ID + 
        		" JOIN " + DBConstants.TABLE_SEARCH_FLAGS + 
        			" f ON f." + DBConstants.FIELD_SEARCH_FLAGS_ID + " = s." + DBConstants.FIELD_SEARCH_ID + 
        	" WHERE s." + DBConstants.FIELD_SEARCH_COMM_ID + " = ? "  +
        	" AND f." + DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + " = 1 " +
        	" AND f." + DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = 0 " + 
        	" AND (s." + DBConstants.FIELD_SEARCH_AGENT_ID + " = ? OR -1 = ?) " + 
        	" AND p." + DBConstants.FIELD_PROPERTY_COUNTY_ID + " = ? " +
        	" AND s." + DBConstants.FIELD_SEARCH_TSR_FILE_LINK + " LIKE ?" +
        	" AND s." + DBConstants.FIELD_SEARCH_TYPE + " != ?";
    private static final String SQL_SEARCH_FOR_UPDATE_WITH_AGENT_FILENO = 
    	"SELECT s." + DBConstants.FIELD_SEARCH_ID + 
    	", s." + DBConstants.FIELD_SEARCH_TYPE + 
    	", s." + DBConstants.FIELD_SEARCH_TSR_DATE + 
    	" FROM " + DBConstants.TABLE_SEARCH + 
    		" s JOIN " + DBConstants.TABLE_PROPERTY + 
    			" p ON s." + DBConstants.FIELD_SEARCH_PROPERTY_ID + " = p." + DBConstants.FIELD_PROPERTY_ID + 
    		" JOIN " + DBConstants.TABLE_SEARCH_FLAGS + 
    			" f ON f." + DBConstants.FIELD_SEARCH_FLAGS_ID + " = s." + DBConstants.FIELD_SEARCH_ID + 
    	" WHERE s." + DBConstants.FIELD_SEARCH_COMM_ID + " = ? "  +
    	" AND f." + DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + " = 1 " +
    	" AND f." + DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = 0 " + 
    	" AND (s." + DBConstants.FIELD_SEARCH_AGENT_ID + " = ? OR -1 = ?) " + 
    	" AND p." + DBConstants.FIELD_PROPERTY_COUNTY_ID + " = ? " +
    	" AND (s." + DBConstants.FIELD_SEARCH_AGENT_FILENO + " = ? OR s." + DBConstants.FIELD_SEARCH_AGENT_FILENO + " = ? )" +
    	" AND s." + DBConstants.FIELD_SEARCH_TSR_FILE_LINK + " LIKE ?" +
    	" AND s." + DBConstants.FIELD_SEARCH_TYPE + " != ?";
    private static final String SQL_SEARCH_FOR_UPDATE_GUID = 
        	"SELECT s." + DBConstants.FIELD_SEARCH_ID + 
        	", s." + DBConstants.FIELD_SEARCH_TYPE + 
        	", s." + DBConstants.FIELD_SEARCH_TSR_DATE + 
        	" FROM " + DBConstants.TABLE_SEARCH + 
        		" s JOIN " + DBConstants.TABLE_PROPERTY + 
        			" p ON s." + DBConstants.FIELD_SEARCH_PROPERTY_ID + " = p." + DBConstants.FIELD_PROPERTY_ID + 
        		" JOIN " + DBConstants.TABLE_SEARCH_FLAGS + 
        			" f ON f." + DBConstants.FIELD_SEARCH_FLAGS_ID + " = s." + DBConstants.FIELD_SEARCH_ID + 
        		" JOIN " + DBConstants.TABLE_SEARCH_EXTERNAL_FLAGS + 
        			" sef ON sef." + SearchExternalFlag.FIELD_SEARCH_ID + " = s." + DBConstants.FIELD_SEARCH_ID + 
        	" WHERE s." + DBConstants.FIELD_SEARCH_COMM_ID + " = ? "  +
        	" AND f." + DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + " = 1 " +
        	" AND f." + DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = 0 " + 
        	" AND p." + DBConstants.FIELD_PROPERTY_COUNTY_ID + " = ? " +
        	" AND sef." + SearchExternalFlag.FIELD_SO_ORDER_ID + " = ?";
    /**
     * Please be sure to first get the update list by SO GUID using ro.cst.tsearch.database.DBManager.getSearchIdsForUpdate(String, long, String)
     * @param fileName
     * @param commId
     * @param countyId
     * @param agentFileId
     * @return
     */
    public static List<SearchUpdateMapper> getSearchIdsForUpdate(
    		String fileName, long commId, String countyId, String agentFileId) {
    	try {
    		if(ServerConfig.isCheckAgentFileNoInUpdate()) {
    			/**
    			 * We need tht doubleQuotes and transfromBlank because this is how it's done on saving the search
    			 */
    			return getSimpleTemplate().query(SQL_SEARCH_FOR_UPDATE_WITH_AGENT_FILENO, new SearchUpdateMapper(), 
	    				commId, -1000, -1, countyId, Formater.doubleQuotes(DataAttribute.transformBlank(agentFileId)), agentFileId, "%" + fileName + "%", Products.FVS_PRODUCT);
    		} else {
	    		return getSimpleTemplate().query(SQL_SEARCH_FOR_UPDATE, new SearchUpdateMapper(), 
	    				commId, -1000, -1, countyId, "%" + fileName + "%", Products.FVS_PRODUCT);
    		}
    	} catch (Exception e) {
			e.printStackTrace();
		}
    	return new ArrayList<SearchUpdateMapper>();
    }
    
    /**
     * Takes the GUID and searches for a completed order (TSR and not closed) with that GUID 
     * @param soToUpdateGuid
     * @param commId
     * @param countyId
     * @return list of SearchUpdateMappers (empty if not found or error)
     */
    public static List<SearchUpdateMapper> getSearchIdsForUpdate(
    		String soToUpdateGuid, long commId, String countyId) {
    	try {
    		if(org.apache.commons.lang.StringUtils.isNotBlank(soToUpdateGuid)) {
	    		return getSimpleTemplate().query(SQL_SEARCH_FOR_UPDATE_GUID, 
	    				new SearchUpdateMapper(), 
	    				commId, 
	    				countyId, 
	    				soToUpdateGuid);
    		}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		}
    	return new ArrayList<SearchUpdateMapper>();
    }
    
    
    
    private static final String SQL_SEARCH_FOR_FVS_UPDATE_WITH_SEARCHID = 
        	"SELECT s." + DBConstants.FIELD_SEARCH_ID + 
        	", s." + DBConstants.FIELD_SEARCH_TYPE + 
        	", s." + DBConstants.FIELD_SEARCH_TSR_DATE + 
        	" FROM " + DBConstants.TABLE_SEARCH + 
        		" s JOIN " + DBConstants.TABLE_PROPERTY + 
        			" p ON s." + DBConstants.FIELD_SEARCH_PROPERTY_ID + " = p." + DBConstants.FIELD_PROPERTY_ID + 
        		" JOIN " + DBConstants.TABLE_SEARCH_FLAGS + 
        			" f ON f." + DBConstants.FIELD_SEARCH_FLAGS_ID + " = s." + DBConstants.FIELD_SEARCH_ID + 
        	" WHERE s." + DBConstants.FIELD_SEARCH_ID + " = ? "  +
        	" AND f." + DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + " = 1 " +
        	" AND f." + DBConstants.FIELD_SEARCH_FLAGS_IS_CLOSED + " = 0 ";
    
    public static List<SearchUpdateMapper> getSearchIdsForFVSUpdate(long searchId) {
    	try {
	    	return getSimpleTemplate().query(SQL_SEARCH_FOR_FVS_UPDATE_WITH_SEARCHID, new SearchUpdateMapper(), searchId);
    	} catch (Exception e) {
			e.printStackTrace();
		}
    	return new ArrayList<SearchUpdateMapper>();
    }
    
    /**
     * Deletes the context for this search (if exists)
     * @param searchId
     */
	public static void deleteSearchContext(long searchId) {
		String sql = "DELETE FROM " + TABLE_SEARCH_DATA + 
		" WHERE " + FIELD_SEARCH_DATA_SEARCHID + " = " + searchId;
		DBConnection conn = null;
    	
        try {
            conn = ConnectionPool.getInstance().requestConnection();
            conn.executeUpdate(sql);
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            ConnectionPool.getInstance().releaseConnection(conn);
	        } catch(BaseException e) {
	            logger.error(e);
	        }           
	    }
		
	}
	
	private static Map<String, String> docTypeMap = new ConcurrentHashMap<String, String>();
	
	public static void cleanDocTypeMap(){
		docTypeMap.clear();
	}
	
	/**
	 * Get the <state_abbrev><county_name> to be used for getting the doctypes
	 * If DB has null value, then return the provided stateCounty
	 * Simple caching is implemented, so changing it requires reloading the application  for now
	 * @param stateCounty
	 * @return stateCounty to be used for docTypes
	 */
	public static String getDocTypeCounty(String stateCounty){
		
		String savedData = docTypeMap.get(stateCounty);
		
		// check the cache
		if(savedData != null){
	    	return savedData;
	    }
	    
		// compute the docType
		String result = null;
		try{		
			long countyId = TSServersFactory.getCountyId(stateCounty);
			String sql = "SELECT docType FROM " + DBConstants.TABLE_COUNTY + " WHERE id = ?";
			result = getSimpleTemplate().queryForObject(sql, String.class, countyId);			
	    }catch(RuntimeException e){
	    	// swallow error in case the DB has not been updated yet
	    	result = null;
	    }
	    
	    // default to input stateCounty value
	    if(result == null) result = stateCounty;
	    
	    // add to cache
	    docTypeMap.put(stateCounty, result);
	    
		return result;
		
	}
	
	/**
	 * SET TSR creation in progress status for the current search
	 * @param searchId
	 */
	public static void setSearchTSRCreationInProgress(long searchId){
		
		DBConnection conn = null;
		try{
		   conn = ConnectionPool.getInstance().requestConnection();
           String sql = " UPDATE " + DBConstants.TABLE_SEARCH_FLAGS + " SET "
        	+   DBConstants.FIELD_SEARCH_FLAGS_CHECKED_BY + " = -2 "
        	+ "WHERE "+DBConstants.FIELD_SEARCH_FLAGS_ID + " = "+searchId;
           conn.executeSQL(sql);
           conn.commit();		    
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            ConnectionPool.getInstance().releaseConnection(conn);
	        } catch(BaseException e) {
	            logger.error(e);
	        }           
	    }
	}
	
	/**
	 * Function used by PDFShow.jsp
	 * @param initalFileName
	 * @return link for tsr or completed template file
	 */	
	public static String getRealFileLink(String initalFileName){
				
		// construct relative file name
		String relFileName = initalFileName;
		if(relFileName.startsWith(URLMaping.path + "/")){
			relFileName = relFileName.replace(URLMaping.path + "/", "");
		}
		
		// if file is on hdd, return local link
		String hddFileName = ServerConfig.getFilePath() + relFileName;
		if(FileUtils.existPath(hddFileName)){
			return URLMaping.path + "/DownloadFileAs?pdfFile=" + initalFileName;
		}
		
		// if file is not found on other server, use local link which will display "not found"
		String server = findTsrServer(relFileName);
		if(server == null){
			return URLMaping.path + "/DownloadFileAs?pdfFile=" + initalFileName;
		}
		
		// return link on remote server
		return server + ":" + getAppPort() + "/title-search/fs?f=" + relFileName + "&searchId=" + Search.SEARCH_NONE;
		
	}
	
	/**
	 * Return current date + n hours in dd-MM-yyy HH:mm:ss format
	 * @param hours
	 * @return
	 */
	public static String addHours(Date tsrOrderDate, int hours) {

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(tsrOrderDate);
	//	calendar.add(Calendar.HOUR, hours + 5); // +5 to compensate for GMT offset of crt machine
		
		Calendar newDate = CommAdminNotifier.addHoursIgnoreWeekends(calendar,hours);
		String month = Integer.toString(newDate.get(Calendar.MONTH) + 1);
		String day = Integer.toString(newDate.get(Calendar.DAY_OF_MONTH));
		String year = Integer.toString(newDate.get(Calendar.YEAR));
		if (Integer.parseInt(day) < 10) {
			day = "0" + day;
		}
		String date = day + "-" + month + "-" + year;
		
		//SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
		return date;
	}
	
	/**
	 * Compute due date based on requirements string and current community sla
	 * @param info requirements string
	 * @param sla current community sla in hours
	 * @return due date formatted for mysql
	 */
	private static String computeMysqlDueDate(Date tsrOrderDate, String info, int sla, long searchId){
		Date needByDate = getSearchDueDate(searchId);
		String date;
		if(needByDate != null){
			date = (new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")).format(needByDate);				
		} else {
			date = computeDueDate(tsrOrderDate, info, sla);	
		}
		return "str_to_date( '" + date + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
	}
	
	private static String computeDueDate(Date tsrOrderDate, String info, int sla){
		// try to find Additional Instructions: Rn
		Matcher matcher = pattAdditInstr.matcher(info);
		String date = null;
		if(matcher.find()){
			int days = Integer.valueOf(matcher.group(1));
			date = addHours(tsrOrderDate, days * 24) + " 12:00:00";
		}
		
		// try to find Additional Instructions: Rush
		if(date == null){
			matcher = pattRush.matcher(info);
			if(matcher.find()){
				date = addHours(tsrOrderDate, 0) + " 12:00:00";
			}
		}
		
		// try to find Need By Date: mm/dd/yyy
		if(date == null){			
			matcher = pattNeedByDate.matcher(info);			
			if(matcher.find()){
				
				// extract month, day, year
				String month = matcher.group(1);
				String day = matcher.group(2);
				String year = matcher.group(3);
				 
				// fix month, day, year
				if(month.length() == 1){ 
					month = "0" + month; 
				}
				if(day.length() == 1){ 
					day = "0" + day; 
				}
				if(year.length() != 4){ 
					year = "2000".substring(0, 4 - year.length()) + year;
				}
				date = day + "-" + month + "-" + year + " 12:00:00";				
			}
		}
		
		// use the default sla
		if(date == null){
			date = addHours(tsrOrderDate, sla) + " 12:00:00"; 
		}
		
		return date;
	}
	
	public static String getNeedByDate(Search search){
		
		long searchId = search.getID();
 		String date = getNeedByDateFromDB(searchId);
		if(StringUtils.isEmpty(date)){
			 long commId = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getID().longValue();   
		     CommunityAttributes commAttr = null;
		     try {
				commAttr = CommunityManager.getCommunity(commId);
			 } catch (BaseException shouldNotHappen) {
				 shouldNotHappen.printStackTrace();
			 	 throw new RuntimeException("Can not construct community for commid= "+commId);
			 }
			 int sla = commAttr.getDEFAULTSLA().intValue();
			 Date tsrOrderDate = search.getTSROrderDate();
	        	if (tsrOrderDate == null){
	        		tsrOrderDate = new Date(System.currentTimeMillis());
	        	}
			 date = computeDueDate(tsrOrderDate, " ",sla);
		}
		int poz=-1;
		if((poz=date.indexOf(" "))>0){
			date = date.substring(0,poz);
		}
		return date;
	}
	
	/**
     * Return need by Date from database
     *
     */
    private static String getNeedByDateFromDB(long searchId){
    	String sql = "SELECT DUE_DATE  FROM " + TABLE_SEARCH + " WHERE ID = " + searchId;;
    	String result = null;
    	try {
			result = getSimpleTemplate().queryForObject(sql,String.class);
		} catch (Exception e) {
			//System.err.println("Exception caught!!! Don't worry!");
			//e.printStackTrace();
			//System.err.println("Exception caught!!! Don't worry!");
		}
	    return result;    	
    }
	
	public static void setSearchStatus(long searchId, int status)
    {
        String sql = "update "+ DBConstants.TABLE_SEARCH +" set " + 
        	DBConstants.FIELD_SEARCH_STATUS + " = " + status + " where " + 
        	DBConstants.FIELD_SEARCH_ID + " = " + searchId;
        
        DBConnection conn = null;
        
        try {
            conn = ConnectionPool.getInstance().requestConnection();
            conn.executeUpdate(sql);
        } catch (Exception be) {
            be.printStackTrace();
        } finally {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch(BaseException e) {
                logger.error(e);
            }           
        }
    }

	public static void uncloseSearch(Vector<Long> toUnclose) {
		try {
			if(toUnclose == null || toUnclose.size() == 0) {
				return;
			}
			getTransactionTemplate().execute(new RemoveKStatusTransaction(toUnclose));
		} catch (Exception e) {
			logger.error(
					"Error while unclosing searches " + 
						org.apache.commons.lang.StringUtils.join(toUnclose.iterator(), ","), 
					e);
		}
		
	}

	public static void setSearchNoteStatus(long searchDBId, int noteStatus) {
        
        String sql = "UPDATE "+ TABLE_SEARCH +" SET NOTE_STATUS = ? WHERE ID = ? AND NOTE_STATUS < ?";
                
		try {
			DBManager.getSimpleTemplate().update(sql, noteStatus, searchDBId, noteStatus);
		} catch (Exception e) {
			 logger.error(e);
		}
    }

    /**
	 * Unimplemented yet
	 * @param sourceId
	 * @param destinationId
	 */
	public static void copySearchContext(long sourceId, long destinationId) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Determines if this is the first time a save is done on the search specified by the given id
	 * @param id the searchId of the search
	 * @param tsrFileLink the link that will be set after the save
	 * @return
	 */
	private static boolean isSearchReused(long id, String tsrFileLink) {
		String originalTsrFileLink = null;
		try {
			originalTsrFileLink = getSimpleTemplate().queryForObject(SQL_GET_TSR_FILE_NAME, String.class, id);
		} catch (Exception e) {
			return false;
		}
		if(StringUtils.isEmpty(originalTsrFileLink))
			return false;
		int indexSSF = originalTsrFileLink.indexOf("&SSFLINK=");
		if(indexSSF > 0) {
			originalTsrFileLink = originalTsrFileLink.substring(0, indexSSF);
		}
		if(originalTsrFileLink.equals(tsrFileLink))
			return false;
		//if the original file name does not have an extension it means we've just changed the name of the search so it is not a reopen 
		if(DownloadFile.getSpecificExt(originalTsrFileLink) == DownloadFile.EXT_UNKNOWN)
			return false;
		if(!originalTsrFileLink.contains(".") && !tsrFileLink.contains(".")) 
			return false;
		return true;
	}
	
	/**
	 * Used in ro.cst.tsearch.database.DBManager.deteleTSRLogs(Search)
	 */
	private static final String SQL_DELETE_TSR_STATUS = "UPDATE " + 
		DBConstants.TABLE_SEARCH_FLAGS + " SET " + 
		DBConstants.FIELD_SEARCH_FLAGS_INDEX_STATUS + " = 0 WHERE " + 
		DBConstants.FIELD_SEARCH_FLAGS_ID + " = ?";
	/**
	 * Used in ro.cst.tsearch.database.DBManager.deteleTSRLogs(Search)
	 */
	private static final String SQL_DELETE_TSR_LOG = "UPDATE " + 
		DBConstants.TABLE_SEARCH_DATA_BLOB + " SET " + 
		DBConstants.FIELD_SEARCH_DATA_BLOB_INDEX + " = null WHERE " + 
		DBConstants.FIELD_SEARCH_FLAGS_ID + " = ?";
	/**
	 * Deletes the TSR log file and status field from the database for the specified search
	 * @param search
	 */
    private static void deteleTSRLogs(Search search) {
		try {
			getSimpleTemplate().update(SQL_DELETE_TSR_STATUS, search.getID());
		} catch (Exception e) {
			System.err.println("DELETING TSR_STATUS FAILED for search " + search.getID());
			e.printStackTrace();
			logger.error("DELETING TSR_STATUS FAILED for search " + search.getID());
		}
		try {
			getSimpleTemplate().update(SQL_DELETE_TSR_LOG, search.getID());
		} catch (Exception e) {
			System.err.println("DELETING TSR_LOG FAILED for search " + search.getID());
			e.printStackTrace();
			logger.error("DELETING TSR_LOG FAILED for search " + search.getID());
		}
		
	}

    /**
	 * Used in ro.cst.tsearch.database.DBManager.isSearchReused(long, String) method
	 * Used in ro.cst.tsearch.database.DBManager.deleteTSRFiles(Search)
	 */
    private static final String SQL_GET_TSR_FILE_NAME = "SELECT " + 
    	DBConstants.FIELD_SEARCH_TSR_FILE_LINK + " FROM " + 
    	DBConstants.TABLE_SEARCH + " WHERE " + 
    	DBConstants.FIELD_SEARCH_ID + " = ?";
    /**
     * Deletes the files associated with this search from the local search file.
     * For now this does not replicate on other servers.
     * Files affected are the TSD and the xml containing the search Attributes for the server.
     * @param search
     * @param contextPath
     */
	private static void deleteTSRFiles(String originalTsrFileLink) {
		try {
			int indexOfSlash = -1;
			if(originalTsrFileLink.contains("/")) {
				indexOfSlash = originalTsrFileLink.lastIndexOf("/");
			} else if(originalTsrFileLink.contains("\\")){
				indexOfSlash = originalTsrFileLink.lastIndexOf("\\");
			}
			
			String folderName = originalTsrFileLink.substring(0, indexOfSlash);
			String prefixFilter = originalTsrFileLink.substring(indexOfSlash +1 );
			File tsrFolder = new File(folderName);
			String[] files = tsrFolder.list(new PrefixFilenameFilter(prefixFilter));
			for (int i = 0; i < files.length; i++) {
				File file = new File(tsrFolder + File.separator + files[i]);
				if(file.exists())
					file.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	private static String getOriginalTsrFileName(long id) {
		String originalTsrFileName = null;
		try {
    		originalTsrFileName = getSimpleTemplate().queryForObject(SQL_GET_TSR_FILE_NAME, String.class, id);
    		originalTsrFileName = originalTsrFileName.replaceAll("(.)*/fs\\?f=", "");
    		int indexSSF = originalTsrFileName.indexOf("&SSFLINK=");
    		if(indexSSF > 0) {
    			originalTsrFileName = originalTsrFileName.substring(0, indexSSF);
    		}
    		originalTsrFileName = ServerConfig.getFilePath() + originalTsrFileName;
    		originalTsrFileName = FileUtils.removeFileExtention(originalTsrFileName);
		} catch (Exception e) {
		}
		return originalTsrFileName;
	}
	
	private static void unsetTSRDate(Search global) {
		try {
			String sql = "UPDATE " + DBConstants.TABLE_SEARCH + " set " + 
	        	DBConstants.FIELD_SEARCH_TSR_DATE + " = null, " +
	        	DBConstants.FIELD_SEARCH_REPORTS_DATE + " = " + DBConstants.FIELD_SEARCH_SDATE + ", " +
	        	DBConstants.FIELD_SEARCH_TIME_ELAPSED + " = 0 where " + 
	        	DBConstants.FIELD_SEARCH_ID + " = " + global.getID();
			getSimpleTemplate().update(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
    /**
	 * Used in ro.cst.tsearch.database.DBManager.getDefaultLegalTemplates()
	 * The '?' placeholder should be replaced with the list of county Ids
	 * Returns all the templates currently used for the default legal description
	 */
	private static final String SQL_GET_DEFAULT_LD_TEMPLATES = "SELECT "
																+ DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_ID + ","
																+ DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_DEFAULT_LD +","
																+ DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_OVERWRITE_OCR_LEGAL + ","
																+ DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_DEFAULT_LD_CONDO +","
																+ DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_OVERWRITE_OCR_LEGAL_CONDO
																+ " FROM " + DBConstants.TABLE_COUNTY_LEGAL_TEMPALTES 
																;

	public static List<CountyDefaultLegalMapper> getDefaultLegalTemplates() {
		try {
			return getSimpleTemplate().query(SQL_GET_DEFAULT_LD_TEMPLATES, new CountyDefaultLegalMapper());
		}catch(Exception erdae) {
			return new ArrayList<CountyDefaultLegalMapper>();
		}
	}
		
    /**
	 * Used in ro.cst.tsearch.database.DBManager.getCountyDefaultLegal(String)
	 * The '?' placeholder should be replaced with the list of county Ids
	 * Returns the default legal description for the given county ids
	 */
	private static final String SQL_GET_DEFAULT_LD_FOR_COUNTIES = "SELECT "
						+ DBConstants.FIELD_COUNTY_COMMUNITY_COUNTY_ID + ","
						+ "c."+DBConstants.FIELD_COUNTY_COMMUNITY_TEMPLATE_ID + ","
						+ "ld." +DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_ID + ","
						+ DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_DEFAULT_LD +","
						+ DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_OVERWRITE_OCR_LEGAL + ","
						+ DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_DEFAULT_LD_CONDO +","
						+ DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_OVERWRITE_OCR_LEGAL_CONDO 
						+ " FROM " + DBConstants.TABLE_COUNTY_LEGAL_TEMPALTES + " ld, "
								   + DBConstants.TABLE_COUNTY_COMMUNITY + " c "
						+ " WHERE " 
							+ DBConstants.FIELD_COUNTY_COMMUNITY_COMMUNITY_ID  + " = ? "
							+ " AND " + DBConstants.FIELD_COUNTY_COMMUNITY_COUNTY_ID + " IN ($COUNTIES$) "
							+ " AND  c." + DBConstants.FIELD_COUNTY_COMMUNITY_TEMPLATE_ID + " = ld." + DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_ID ;
						; 
		
	public static List<CountyDefaultLegalMapper> getCountyDefaultLegal(String countyIds, int commId) {
		try {
			List<CountyDefaultLegalMapper> list = getSimpleTemplate().query(SQL_GET_DEFAULT_LD_FOR_COUNTIES.replace("$COUNTIES$",StringUtils.makeValidNumberList(countyIds)), new CountyDefaultLegalMapper(),commId);
			return list;
		}catch(EmptyResultDataAccessException erdae) {
			return new ArrayList<CountyDefaultLegalMapper>();
		}
	}
	
	/**
	 * Used in ro.cst.tsearch.database.DBManager.setDefaultLdTemplates(String,String)
	 * Updates the default LD for the given countyIds
	 */
	private static final String SQL_UPDATE_DEFAULT_LD_FOR_COUNTIES = "UPDATE "
													 	+ DBConstants.TABLE_COUNTY_COMMUNITY
													 	+ " SET "
													 	+ DBConstants.FIELD_COUNTY_COMMUNITY_TEMPLATE_ID + " = ? "
													 	+ " WHERE "
													 	+ DBConstants.FIELD_COUNTY_COMMUNITY_COMMUNITY_ID  +" = ?"
													 	+ " AND "+DBConstants.FIELD_COUNTY_COMMUNITY_COUNTY_ID+ " IN (:countyIds)";
	
	public static int setDefaultLdTemplates(String countyIds, String defaultLd, boolean overwriteOCRLegal, int commId, 
			List<Long> templateIds, List<String> defaultCondoLds) {	
		int nr = 0;
		try {
			countyIds = StringUtils.makeValidNumberList(countyIds);
			SimpleJdbcInsert sji = DBManager.getSimpleJdbcInsert()
							   				.withTableName(DBConstants.TABLE_COUNTY_LEGAL_TEMPALTES)
							   				.usingGeneratedKeyColumns(DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_ID);

			String []ids = countyIds.split("[,]");
			
			for (String countyIdStr:ids) {
				List<CountyDefaultLegalMapper> defaultLds = DBManager.getCountyDefaultLegal(countyIdStr,commId);
				CountyDefaultLegalMapper cdm = defaultLds.get(0);	//there is only one county at once (county IDs were split)
				String defaultLdCondo = cdm.getDefaultLdCondo();
				boolean overwriteOCRLegalCondo = cdm.isOverWriteOCRLegalCondo();
				
				Map<String, Object> parameters = new HashMap<String, Object>(1);
			   	parameters.put(DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_DEFAULT_LD, defaultLd);
			   	parameters.put(DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_OVERWRITE_OCR_LEGAL, overwriteOCRLegal);
			   	parameters.put(DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_DEFAULT_LD_CONDO, defaultLdCondo);
			   	parameters.put(DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_OVERWRITE_OCR_LEGAL_CONDO, overwriteOCRLegalCondo);
			   	nr++;
			   	Number templateId = sji.executeAndReturnKey( parameters );
			   	templateIds.add((Long)templateId.longValue());
			   	defaultCondoLds.add(defaultLdCondo);
			   	
			   	DBManager.getSimpleTemplate().update( SQL_UPDATE_DEFAULT_LD_FOR_COUNTIES.replaceAll(":countyIds",countyIdStr), templateId , commId);
			   	
			   	try{
			   		int countyId = Integer.parseInt(countyIdStr.trim());
			   		CountyCommunityManager.getInstance().getCountyCommunityMapper(countyId, commId).setTemplateId(templateId.intValue());
		   		}catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return nr;	
	}
	
	public static int setDefaultLdTemplatesCondo(String countyIds, String defaultLdCondo, boolean overwriteOCRLegalCondo, int commId, 
			List<Long> templateIds, List<String> defaultLds) {	
		int nr = 0;
		try {
			countyIds = StringUtils.makeValidNumberList(countyIds);
			SimpleJdbcInsert sji = DBManager.getSimpleJdbcInsert()
							   				.withTableName(DBConstants.TABLE_COUNTY_LEGAL_TEMPALTES)
							   				.usingGeneratedKeyColumns(DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_ID);

			String []ids = countyIds.split("[,]");
			
			for (String countyIdStr:ids) {
				List<CountyDefaultLegalMapper> defaultCondoLds = DBManager.getCountyDefaultLegal(countyIdStr, commId);
				CountyDefaultLegalMapper cdm = defaultCondoLds.get(0);	//there is only one county at once (county IDs were split)
				String defaultLd = cdm.getDefaultLd();
				boolean overwriteOCRLegal = cdm.isOverWriteOCRLegal();
				
				Map<String, Object> parameters = new HashMap<String, Object>(1);
			   	parameters.put(DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_DEFAULT_LD, defaultLd);
			   	parameters.put(DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_OVERWRITE_OCR_LEGAL, overwriteOCRLegal);
			   	parameters.put(DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_DEFAULT_LD_CONDO, defaultLdCondo);
			   	parameters.put(DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_OVERWRITE_OCR_LEGAL_CONDO, overwriteOCRLegalCondo);
			   	nr++;
			   	Number templateId = sji.executeAndReturnKey( parameters );
			   	templateIds.add((Long)templateId.longValue());
			   	defaultLds.add(defaultLd);
			   	
			   	DBManager.getSimpleTemplate().update( SQL_UPDATE_DEFAULT_LD_FOR_COUNTIES.replaceAll(":countyIds",countyIdStr), templateId , commId);
			   	
			   	try{
			   		int countyId = Integer.parseInt(countyIdStr.trim());
			   		CountyCommunityManager.getInstance().getCountyCommunityMapper(countyId, commId).setTemplateId(templateId.intValue());
		   		}catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return nr;	
	}
	
	/**
	 * Used in ro.cst.tsearch.database.DBManager.deleteUnusedDefaultLdTemplates()
	 * Deletes all the unused templates from the database
	 */
	private static final String SQL_CLEAN_DEFAULT_LD = "DELETE "
														+ "FROM " + DBConstants.TABLE_COUNTY_LEGAL_TEMPALTES 
														+ " WHERE "  
															+ DBConstants.FIELD_COUNTY_LEGAL_TEMPLATES_ID
															+ " NOT IN ( "
																+ " SELECT DISTINCT "
																+ DBConstants.FIELD_COUNTY_COMMUNITY_TEMPLATE_ID
																+ " FROM " + DBConstants.TABLE_COUNTY_COMMUNITY + ")";
	

	
	public static int deleteUnusedDefaultLdTemplates() {
		return DBManager.getSimpleTemplate().update(SQL_CLEAN_DEFAULT_LD);
	}
			
	/**
	 * Used in ro.cst.tsearch.database.DBManager.setDefaultCertificationDateOffset()
	 * Updates the default certification date offset for the given countyIds
	 */
	private static final String SQL_UPDATE_DEFAULT_CERTIFICATION_DATE = "UPDATE "
				 	+ DBConstants.TABLE_COUNTY_COMMUNITY
				 	+ " SET "
				 	+ DBConstants.FIELD_COUNTY_COMMUNITY_DEFAULT_CERTIFICATION_DATE_OFFSET + " = ? "
				 	+ " WHERE "
				 	+ DBConstants.FIELD_COUNTY_COMMUNITY_COUNTY_ID + " IN (:countyIds) and "
				 	+ DBConstants.FIELD_COUNTY_COMMUNITY_COMMUNITY_ID + " = ?"
				 	;

	public static void setDefaultCertificationDateOffset(String countyIds,String offset, int commId) {
		countyIds = StringUtils.makeValidNumberList(countyIds);
		DBManager.getSimpleTemplate().update(SQL_UPDATE_DEFAULT_CERTIFICATION_DATE.replaceAll(":countyIds",countyIds), offset, commId);
	}
	
	/**
	 * Used in ro.cst.tsearch.database.DBManager.getDefaultCertificationDateOffset(int)
	 * Gets the default certification date offset for the given countyId
	 */
	private static final String SQL_GET_DEFAULT_CERTIFICATION_DATE = " SELECT " 
					+ DBConstants.FIELD_COUNTY_COMMUNITY_DEFAULT_CERTIFICATION_DATE_OFFSET
					+ " FROM "
					+ DBConstants.TABLE_COUNTY_COMMUNITY
					+ " WHERE " 
					+ DBConstants.FIELD_COUNTY_COMMUNITY_COUNTY_ID + " = ? and " 
					+ DBConstants.FIELD_COUNTY_COMMUNITY_COMMUNITY_ID + " = ?";
	/**
	 * Better use cached value from CountyCommunityManager 
	 * @param countyId
	 * @param commId
	 * @return
	 */
	public static int getDefaultCertificationDateOffset(int countyId, int commId) {
		try {
			return DBManager.getSimpleTemplate().queryForInt(SQL_GET_DEFAULT_CERTIFICATION_DATE,String.class,countyId, commId);
		}catch(Exception e){
			return CertificationDateManager.DEFAULT_CERTIFICATION_DATE_OFFSET;
		}
	}
	
	/**
	 * Used in ro.cst.tsearch.database.DBManager.setDefaultOfficialStartDateOffset()
	 * Updates the default official start date offset for the given countyIds
	 */
	private static final String SQL_UPDATE_DEFAULT_OFFICIAL_START_DATE = "UPDATE "
				 	+ DBConstants.TABLE_COUNTY_COMMUNITY
				 	+ " SET "
				 	+ DBConstants.FIELD_COUNTY_COMMUNITY_DEFAULT_OFFICIAL_START_DATE_OFFSET + " = ? "
				 	+ " WHERE "
				 	+ DBConstants.FIELD_COUNTY_COMMUNITY_COUNTY_ID + " IN (:countyIds) and "
				 	+ DBConstants.FIELD_COUNTY_COMMUNITY_COMMUNITY_ID + " = ? "
				 	;

	public static void setDefaultOfficialStartDateOffset(String countyIds,String offset, int commId) {
		countyIds = StringUtils.makeValidNumberList(countyIds);
		DBManager.getSimpleTemplate().update(SQL_UPDATE_DEFAULT_OFFICIAL_START_DATE.replaceAll(":countyIds",countyIds), offset, commId);
	}
	
	/**
	 * Used in ro.cst.tsearch.database.DBManager.getDefaultOfficialStartDateOffset(int)
	 * Gets the default official start date offset for the given countyId
	 */
	private static final String SQL_GET_DEFAULT_OFFICIAL_START_DATE = " SELECT " 
					+ DBConstants.FIELD_COUNTY_COMMUNITY_DEFAULT_OFFICIAL_START_DATE_OFFSET
					+ " FROM "
					+ DBConstants.TABLE_COUNTY_COMMUNITY
					+ " WHERE " 
					+ DBConstants.FIELD_COUNTY_COMMUNITY_COUNTY_ID + " = ? and "
					+ DBConstants.FIELD_COUNTY_COMMUNITY_COMMUNITY_ID + " = ?"
					;
	
	/**
	 * Better use cached value from CountyCommunityManager 
	 * @param countyId
	 * @param commId
	 * @return
	 */
	public static int getDefaultOfficialStartDateOffset(int countyId, int commId) {
		try {
			return DBManager.getSimpleTemplate().queryForInt(SQL_GET_DEFAULT_OFFICIAL_START_DATE, countyId, commId);
		}catch(Exception e){
			logger.error("Problem reading official start date offset for countyId:" + countyId + "_" + commId, e);
			return SearchAttributes.YEARS_BACK;
		}
	}
	
	public static void main(String[] args) {
		for(GenericState state : getAllStates()) {
//			System.out.println("EXEC sp_rename N'[dbo].[documents_" + state.getStateAbv() + "].[dataSourceType]', N'category', 'COLUMN'");
//			System.out.println("GO");
//
//			System.out.println("ALTER TABLE [dbo].[documents_" + state.getStateAbv() + "]");
//			System.out.println("ADD [subcategory] varchar(60) NULL");
//			System.out.println("GO");
//			System.out.println();
			
			System.out.println("ALTER TABLE documents_" + state.getStateAbv() + " ADD recordedDate date NULL; ");
			
		}
	}

	
	
	public static int addThreadsStackTrace( String content) {
		SimpleJdbcInsert sji = DBManager.getSimpleJdbcInsert().withTableName(DBConstants.TABLE_THREAD_LOGS).usingGeneratedKeyColumns("id");
		Map<String, Object> parameters = new HashMap<String, Object>(1);
		Date logTime = new Date();
		parameters.put(DBConstants.FIELD_THREAD_LOGS_DATE, logTime);
	   	int generatedId = sji.executeAndReturnKey(parameters).intValue();
	   	String threadLogFolder = SharedDriveUtils.getThreadLogFolder(logTime, false);
	   	String savedFileName = null;
	   	if(threadLogFolder != null) {
	   		String fileName = new SimpleDateFormat(FormatDate.PATTERN_MMddyyyy_HHmmss).format(logTime) + "_" + URLMaping.INSTANCE_DIR + ".txt";
	   		String toSaveFileName = threadLogFolder + fileName;
	   		try {
				//let's do the new save
				
				org.apache.commons.io.FileUtils.writeStringToFile(new File(toSaveFileName), content );
				
				savedFileName = toSaveFileName;
			} catch (Exception e) {
				logger.error("Cannot addThreadsStackTrace to " + toSaveFileName, e);
				String documentIndexBackupLocalFolder = SharedDriveUtils.getThreadLogFolder(logTime, true);
				if(org.apache.commons.lang.StringUtils.isNotBlank(documentIndexBackupLocalFolder)) {
					try {
						org.apache.commons.io.FileUtils.writeStringToFile(new File(documentIndexBackupLocalFolder + fileName), content);
						savedFileName = documentIndexBackupLocalFolder + fileName;
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
	   	} else {
	   		logger.error("Cannot addThreadsStackTrace because I can't find path");
	   	}
	   	if(savedFileName != null) {
	   		getSimpleTemplate().update(
	   				"update " + DBConstants.TABLE_THREAD_LOGS + 
	   				" set " + DBConstants.FIELD_THREAD_LOGS_CONTENT + " = ? where " + DBConstants.FIELD_THREAD_LOGS_ID + " = ?", 
	   				savedFileName, 
	   				generatedId);
	   	}
		return generatedId;
	}
	
	
	
	public static final String SQL_DELETE_DOCUMENT_INDEX = "delete from " + DBConstants.TABLE_DOCUMENTS_INDEX + " where id = ?"; 
	
	public static int addDocumentIndex( String content, Search search) {
		
		int enableType = ServerConfig.getDocumentIndexEnableStatus();
		
		SimpleJdbcInsert sji = DBManager.getSimpleJdbcInsert().withTableName(DBConstants.TABLE_DOCUMENTS_INDEX).usingGeneratedKeyColumns("id");
		Map<String, Object> parameters = new HashMap<String, Object>(1);
		content = content.replaceAll(viewImageRegex, "");
		content = content.replaceAll(displayDocRegex, "");
	   	parameters.put("searchId", search.getID());
	   	byte[] zipString = ZipUtils.zipString(content);
	   	if(enableType != 2) {
	   		parameters.put("document", zipString);
	   	}
	   	int generatedIndexKey = sji.executeAndReturnKey(parameters).intValue();
	   	
	   	if(enableType != 0) {
		   	String sharedDrivePath = SharedDriveUtils.getDocumentIndexFolder(search);
		   	logger.info("Generated path for id " + generatedIndexKey + " is " + sharedDrivePath);
		   	if(org.apache.commons.lang.StringUtils.isNotBlank(sharedDrivePath)) {
		   		String toSaveFileName = sharedDrivePath + generatedIndexKey + ".zip";
		   		try {
					//let's do the new save
					org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(toSaveFileName), zipString );
				} catch (Exception e) {
					logger.error("Cannot addDocumentIndex to " + toSaveFileName, e);
					String documentIndexBackupLocalFolder = SharedDriveUtils.getDocumentIndexBackupLocalFolder(search);
					if(org.apache.commons.lang.StringUtils.isNotBlank(documentIndexBackupLocalFolder)) {
						try {
							org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(documentIndexBackupLocalFolder + generatedIndexKey + ".zip"), zipString);
						} catch (IOException e1) {
							logger.error("Cannot addDocumentIndex to backup folder " + documentIndexBackupLocalFolder + generatedIndexKey + ".zip", e);
						}
					}
				}
		   	}
	   	}
	   	
		return generatedIndexKey;
	}
	
	public static int updateDocumentIndex( int id, String index, Search search) {
		
		int enableType = ServerConfig.getDocumentIndexEnableStatus();
		
		String stm = " update " + DBConstants.TABLE_DOCUMENTS_INDEX + " set  document=?,searchid=? where id=?";
		index = index.replaceAll(viewImageRegex, "");
		byte[] zipString = ZipUtils.zipString(index);
		int updatedRows = 0;
		if(enableType != 2) {
			updatedRows = getSimpleTemplate().update(stm, zipString, search.getID(), id);
		}
		if(enableType != 0) {
			String sharedDrivePath = SharedDriveUtils.getDocumentIndexFolder(search);
		   	if(org.apache.commons.lang.StringUtils.isNotBlank(sharedDrivePath)) {
		   		String toSaveFileName = sharedDrivePath + id + ".zip";
		   		try {
					//let's do the new save
					org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(toSaveFileName), zipString);
					
				} catch (Exception e) {
					logger.error("Cannot updateDocumentIndex to " + toSaveFileName, e);
					String documentIndexBackupLocalFolder = SharedDriveUtils.getDocumentIndexBackupLocalFolder(search);
					if(org.apache.commons.lang.StringUtils.isNotBlank(documentIndexBackupLocalFolder)) {
						try {
							org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(documentIndexBackupLocalFolder + id + ".zip"), zipString);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
		   	}
		}
		return updatedRows;
	}
	
	public static int deleteDocumentIndex(Search search, int documentIndexId) {
		int updatedRows = getSimpleTemplate().update(SQL_DELETE_DOCUMENT_INDEX, documentIndexId);
		String sharedDrivePath = SharedDriveUtils.getDocumentIndexFolder(search);
	   	if(org.apache.commons.lang.StringUtils.isNotBlank(sharedDrivePath)) {
	   		String toSaveFileName = sharedDrivePath + documentIndexId + ".zip";
	   		try {
				//let's do the new delete
				org.apache.commons.io.FileUtils.deleteQuietly(new File(toSaveFileName));
			} catch (Exception e) {
				logger.error("Cannot deleteDocumentIndex to " + toSaveFileName, e);
			}
	   	}
		return updatedRows;
	}
	
	public static String getDocumentIndex(int id){
		String result = null;
		int enableType = ServerConfig.getDocumentIndexEnableStatus();
		try {
			List<DocumentIndexMapper> documentRows = getSimpleTemplate().query(
					"select di.*, "
					+ "CONVERT_TZ(s.sdate, '+00:00', @@session.time_zone) sdate, "
					+ "CONVERT_TZ(s.sdate - interval 1 day, '+00:00', @@session.time_zone) sdate_backup, "
					+ "sdate unformated_sdate from " 
							+ DBConstants.TABLE_DOCUMENTS_INDEX	+ " di join ts_search s on di.searchid = s.id where di.id=?", 
					new DocumentIndexMapper(), 
					id);
			
			if (documentRows.size() > 0){
				DocumentIndexMapper documentIndex = documentRows.get(0);
				
				long searchId =  documentIndex.getSearchId();
				
				if(enableType != 0) {
					String documentIndexFilePath = SharedDriveUtils.getDocumentIndexFile(searchId, documentIndex.getSDate(), id);
					if(documentIndexFilePath != null) {
						
						{
							File documentIndexFile = new File(documentIndexFilePath);
							if(documentIndexFile.exists()) {
								byte[] byteArray = org.apache.commons.io.FileUtils.readFileToByteArray(documentIndexFile);
								result = ZipUtils.unzipString(byteArray);
							} else {
								logger.error("documentIndexFile for document index id " + id + " does not exist for path " + documentIndexFilePath);
							}
						}
						
						if(result == null) {
							//check to see if gmt offset broke our link
							documentIndexFilePath = SharedDriveUtils.getDocumentIndexFile(searchId, documentIndex.getSDateBackup(), id);
							if(documentIndexFilePath != null) {
								
								{
									File documentIndexFile = new File(documentIndexFilePath);
									if(documentIndexFile.exists()) {
										byte[] byteArray = org.apache.commons.io.FileUtils.readFileToByteArray(documentIndexFile);
										result = ZipUtils.unzipString(byteArray);
									} else {
										logger.error("documentIndexFile for document index id " + id + " does not exist for path " + documentIndexFilePath);
									}
								}
							}
						}
						
					} else {
						logger.error("documentIndexFilePath for id " + id + " is null");
					}
				}
				
				if(result == null) {
					logger.error("Could not find disk file for document index id " + id + " on search_id " + searchId + " with sdate " + documentIndex.getSDate());
					byte[] document = documentIndex.getDocument();
		
					if(document == null) {
						String archivePath = ArchiveEntry.getDocumentIndexCompletePath(searchId, id, documentIndex.getUnformattedSDate());
						
						boolean smbEnabled = ServerConfig.isArchiveDestinationSmbEnabled();
						
	        			document = org.apache.commons.io.FileUtils.readFileToByteArray(new File(archivePath));
						
						result = ZipUtils.unzipString(document);
						
						if(result != null ) {
							result = result.replaceAll(viewImageRegex, "");		//backup test
							result = result.replaceAll(displayDocRegex, "");	//backup test
						} else {
							logger.error("Could not find disk file for document index id " + id + " for path " + archivePath);
						}
						
						if(enableType != 2) {
							try {
								String stm = " update "	+ DBConstants.TABLE_DOCUMENTS_INDEX	+ " set  document=? where id=?";
								getSimpleTemplate().update(stm, ZipUtils.zipString(result), id);
							} catch (Exception e) {
								logger.error("Failed to update to DB the Document Index from archive for search " + 
					        			searchId + ". The path tried is " + archivePath, e );
							}
						}
							
					} else {
						result = ZipUtils.unzipString(document);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(result != null ) {
			result = result.replaceAll(viewImageRegex, "");		//backup test
			result = result.replaceAll(displayDocRegex, "");	//backup test
		}
		return result;
	}
	
	public static byte[] getDocumentIndexArchived(long searchId, int id){
		byte[] document = null;
		try {
			document = getSimpleTemplate().queryForObject("select document from " + DBConstants.TABLE_DOCUMENTS_INDEX + " where id=?", byte[].class, id);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("getDocumentIndexArchived for searchId " + searchId + " and document index id " + id, e);
		}
		document = ZipUtils.unZipFile(searchId, document);
		
		return document;
	}
	
	public static List<Map<String, Object>> getDocumentIndexIds(long searchId){
		List<Map<String, Object>> documentIds = null;
		try {
			documentIds = new ArrayList<Map<String, Object>>();
			documentIds = getSimpleTemplate().queryForList("select id from " 
							+ DBConstants.TABLE_DOCUMENTS_INDEX + " where searchid=?", searchId);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return documentIds;
	}
	
	public static List<Map<String, Object>> getDocumentIndexIdsNotNull(long searchId){
		List<Map<String, Object>> documentIds = null;
		try {
			documentIds = new ArrayList<Map<String, Object>>();
			documentIds = getSimpleTemplate().queryForList("select id from " 
							+ DBConstants.TABLE_DOCUMENTS_INDEX + " where searchid=? and (document is not null or content is not null)", searchId);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return documentIds;
	}

	
	private static final String SQL_DELETE_SF_DOCUMENT_INDEX = "delete from " + DBConstants.TABLE_SF_DOCUMENTS_INDEX + " where id = ?"; 
	
	public static int addSfDocumentIndex( String content, Search search) {
		int enableType = ServerConfig.getSsfDocumentIndexEnableStatus();
		
		SimpleJdbcInsert sji = DBManager.getSimpleJdbcInsert().withTableName(DBConstants.TABLE_SF_DOCUMENTS_INDEX).usingGeneratedKeyColumns("id");
		Map<String, Object> parameters = new HashMap<String, Object>(1);
	   	parameters.put("searchId", search.getID());
	   	
	   	byte[] zipString = ZipUtils.zipString(content);
	   	if(enableType != 2) {
	   		parameters.put("document", zipString);
	   	}
	   	
	   	int generatedIndexKey = sji.executeAndReturnKey(parameters).intValue();
	   	
	   	if(enableType != 0) {
		   	String sharedDrivePath = SharedDriveUtils.getSsfDocumentIndexFolder(search);
		   	if(org.apache.commons.lang.StringUtils.isNotBlank(sharedDrivePath)) {
		   		String toSaveFileName = sharedDrivePath + generatedIndexKey + ".zip";
		   		try {
					//let's do the new save
					org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(toSaveFileName), zipString );
				} catch (Exception e) {
					logger.error("Cannot addSsfDocumentIndex to " + toSaveFileName, e);
					String documentIndexBackupLocalFolder = SharedDriveUtils.getSsfDocumentIndexBackupLocalFolder(search);
					if(org.apache.commons.lang.StringUtils.isNotBlank(documentIndexBackupLocalFolder)) {
						try {
							org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(documentIndexBackupLocalFolder + generatedIndexKey + ".zip"), zipString);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
		   	}
	   	}
	   	
	   	return generatedIndexKey;
	}
	
	public static int updateSfDocumentIndex( long id, String index, Search search) {
		String stm = " update " + DBConstants.TABLE_SF_DOCUMENTS_INDEX + " set  document=?,searchid=? where id=?";
		int enableType = ServerConfig.getSsfDocumentIndexEnableStatus();
		
		byte[] zipString = ZipUtils.zipString(index);
		int updatedRows = 0;
		if(enableType != 2) {
			updatedRows = getSimpleTemplate().update(stm, ZipUtils.zipString(index), search.getID(), id);
		}
		if(enableType != 0) {
			String sharedDrivePath = SharedDriveUtils.getSsfDocumentIndexFolder(search);
		   	if(org.apache.commons.lang.StringUtils.isNotBlank(sharedDrivePath)) {
		   		String toSaveFileName = sharedDrivePath + id + ".zip";
		   		try {
					//let's do the new save
					org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(toSaveFileName), zipString);
				} catch (Exception e) {
					logger.error("Cannot updateDocumentIndex to " + toSaveFileName, e);
					String documentIndexBackupLocalFolder = SharedDriveUtils.getSsfDocumentIndexBackupLocalFolder(search);
					if(org.apache.commons.lang.StringUtils.isNotBlank(documentIndexBackupLocalFolder)) {
						try {
							org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(documentIndexBackupLocalFolder + id + ".zip"), zipString);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
		   	}
		}
		return updatedRows;
	}
	
	public static int deleteSfDocumentIndex(Search search, long documentIndexId) {
		int updatedRows = getSimpleTemplate().update(SQL_DELETE_SF_DOCUMENT_INDEX, documentIndexId);
		String sharedDrivePath = SharedDriveUtils.getSsfDocumentIndexFolder(search);
	   	if(org.apache.commons.lang.StringUtils.isNotBlank(sharedDrivePath)) {
	   		String toSaveFileName = sharedDrivePath + documentIndexId + ".zip";
	   		try {
				//let's do the new delete
				org.apache.commons.io.FileUtils.deleteQuietly(new File(toSaveFileName));
			} catch (Exception e) {
				logger.error("Cannot deleteDocumentIndex to " + toSaveFileName, e);
			}
	   	}
	   	return updatedRows;
	}
	
	public static String getSfDocumentIndex(long id){
		String result = null;
		int enableType = ServerConfig.getSsfDocumentIndexEnableStatus();
		
		try {
			
			SearchDatesRowMapper searchDatesRowMapper = getSimpleTemplate().queryForObject(
					"select s.id, "
					+ "CONVERT_TZ(s.sdate, '+00:00', @@session.time_zone) sdate, "
					+ "CONVERT_TZ(s.sdate - interval 1 day, '+00:00', @@session.time_zone) sdate_backup "
					+ "from ts_sf_documents_index  sdi join ts_search s on s.id = sdi.searchid where sdi.id = ?", 
					new SearchDatesRowMapper(), 
					id);
			
			if(enableType != 0) {
				String documentIndexFilePath = SharedDriveUtils.getSsfDocumentIndexFile(
						searchDatesRowMapper.getSearchId(), 
						searchDatesRowMapper.getSdate(), id);
				if(documentIndexFilePath != null) {
					
					{
						File documentIndexFile = new File(documentIndexFilePath);
						if(documentIndexFile.exists()) {
							byte[] byteArray = org.apache.commons.io.FileUtils.readFileToByteArray(documentIndexFile);
							result = ZipUtils.unzipString(byteArray);
						} else {
							logger.error("getSfDocumentIndex for document index id " + id + " with path " + documentIndexFilePath + " does not exist" );
						}
					}
					
					if(result == null) {
						documentIndexFilePath = SharedDriveUtils.getSsfDocumentIndexFile(
								searchDatesRowMapper.getSearchId(), 
								searchDatesRowMapper.getSdateBackup(), id);
						if(documentIndexFilePath != null) {
							
							{
								File documentIndexFile = new File(documentIndexFilePath);
								if(documentIndexFile.exists()) {
									byte[] byteArray = org.apache.commons.io.FileUtils.readFileToByteArray(documentIndexFile);
									result = ZipUtils.unzipString(byteArray);
								} else {
									logger.error("getSfDocumentIndex for document index id " + id + " with path " + documentIndexFilePath + " does not exist" );
								}
							}
						}
					}
					
				} else {
					logger.error("getSfDocumentIndex for document index id " + id + " does not have a file path " );
				}
			}
			
			if(result == null) {
				logger.error("getSfDocumentIndex for document index id retrying old mechanism" );
				byte[] document = getSimpleTemplate().queryForObject("select document from " + DBConstants.TABLE_SF_DOCUMENTS_INDEX + " where id=?", byte[].class, id);
				result = ZipUtils.unzipString(document);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("getSfDocumentIndex for document index id " + id + " hit an exception", e );
		}
		if(org.apache.commons.lang.StringUtils.isEmpty(result)) {
			logger.error("getSfDocumentIndex Could not read ssf documetent index for id " + id);
		}
		return result;
	}
	
	public static Date getStartDate(long searchID) {
		return getSimpleTemplate().queryForObject("select sdate from ts_search where id="+searchID, Date.class);
	}

	/**
	 * @return the logger
	 */
	public static Logger getLogger() {
		return logger;
	}
	/*
	public static void saveLogs(String table, final LinkedList<LogEntryI> log, String server) throws DataAccessException{
		final int serverId = getServerIdByAlias(server);
		ConnectionPool.getInstance().getTemplate().batchUpdate("insert into " + table 
						+ " (test_id, test_date, error_type, server) values (?, ?, ?, ?)", 
						new BatchPreparedStatementSetter(){
							public int getBatchSize(){
								return log.size();
							}
							
							public void setValues(PreparedStatement ps, int row) throws SQLException{
								ps.setInt(1, log.get(row).getTestId());
								ps.setLong(2, log.get(row).getTestDate());
								ps.setInt(3, log.get(row).getErrorCode());
								ps.setInt(4, serverId);
							}
						});
	}
	*/
	private static final String SQL_GET_TSR_CREATION_DATE = 
		"SELECT " + DBConstants.FIELD_SEARCH_TSR_DATE + 
		" FROM " + DBConstants.TABLE_SEARCH + " WHERE " + DBConstants.FIELD_SEARCH_ID + " = ?"; 
	
	public static Date getTSRCreationDate(long searchId) {
		try {
			return DBManager.getSimpleTemplate().queryForObject(SQL_GET_TSR_CREATION_DATE, Date.class, searchId);
		} catch (Exception e) {
			logger.error("Error while trying to read TSR creation date", e);
		}
		return null;
	}
	
	private static final String SQL_INSERT_DATATREE = 
		"insert into ts_data_tree_status ( " + 
			"id, " + 
			"subCountyId, " + 
			"indexType, " + 
			"beginDate, " + 
			"endDate, " + 
			"firstDoc, " + 
			"lastDoc, " + 
			"docType, " + 
			"docFormat, " + 
			"docFiledNames, " +
			"rangeInfo, " + 
			"idDescr, " + 
			"stateFips, " + 
			"countyFips, " +
			"companyType " +
			") VALUES ( ?,?,?,?,?,?,?,?,?,?,?,?,?,?,? ) ";
	
	private static final String DELETE_DATATREE_PROFILE = "delete from ts_data_tree_status";
	
	public static boolean updateProfileForDataTree(final List<DataTreeStruct> data) {
		try {
			//RandomAccessFile rand = new RandomAccessFile(new File("c:\\recordTypes.txt"),"rw");
			DBManager.getSimpleTemplate().update(DELETE_DATATREE_PROFILE);
			for(DataTreeStruct struct:data){
				//rand.write((struct.getStateFips()+" "+ struct.getCountyFips()+" "+struct.getSubCountyId()+" "+struct.getDataTreeDocType()+" "+"\n").getBytes());
				
				DBManager.getSimpleTemplate().update(SQL_INSERT_DATATREE, struct.getDataTreeId(), struct.getSubCountyId(), 
						struct.getIndexType(),struct.getBeginDate(), struct.getEndDate(),struct.getFirstDoc(),struct.getLastDoc(),
						struct.getDataTreeDocType(),struct.getDocFormat(),struct.getDocFiledNames(),struct.getRangeInfo(),struct.getIdDescr(),struct.getStateFips(),
						struct.getCountyFips(),struct.getCompanyType());
			}
			//rand.close();
		} catch (Exception e) {
			DBManager.getLogger().error(" Function = updateProfileForDataTree error",e);
			return false;
		}
		return true;
	}
	
	private static final String SELECT_DATATREE_PROFILE = "select * from ts_data_tree_status";
	
	public static List<DataTreeStruct> getProfileForDataTree(){
		try {
			return DBManager.getSimpleTemplate().query(SELECT_DATATREE_PROFILE, new ParameterizedRowMapper<DataTreeStruct>(){
				@Override
				public DataTreeStruct mapRow(ResultSet rs, int arg1) throws SQLException {
					DataTreeStruct struct = new DataTreeStruct(rs.getInt("id"));
					struct.setCountyFips(rs.getString("countyFips"));
					struct.setStateFips(rs.getString("stateFips"));
					struct.setSubCountyId(rs.getString("subCountyId"));
					try{
						struct.setBeginDate(rs.getDate("beginDate"));
					}catch(Exception e){e.printStackTrace();}
					try{
						struct.setEndDate(rs.getDate("endDate"));
					}catch(Exception e){e.printStackTrace();}
					
					struct.setCompanyType(rs.getString("companyType"));
					struct.setDataTreeDocType(rs.getString("docType"));
					struct.setDocFiledNames(rs.getString("docFiledNames"));
					struct.setDocFormat(rs.getString("docFormat"));
					struct.setFirstDoc(rs.getString("firstDoc"));
					struct.setIdDescr(rs.getString("idDescr"));
					struct.setIndexType(rs.getString("indexType"));
					struct.setLastDoc(rs.getString("lastDoc"));
					struct.setRangeInfo(rs.getString("rangeInfo"));
					return struct;
				}
			});
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static final String GET_COUNTY_STATE_PROFILE_SQL = "select * from indexProfile";
	
	public  static List<ProfileStruct> getStateCountyProfileList(){
		return getJdbcTemplate().query(GET_COUNTY_STATE_PROFILE_SQL, new ProfileMapper());
	}
	
	public static final String SQL_INCREASE_DUE_DATE_BY_YEARS = "UPDATE " + DBConstants.TABLE_TAX_DATES + 
		" SET " + DBConstants.FIELD_TAX_DATES_DUE_DATE + 
			" = date_format((str_to_date(" + DBConstants.FIELD_TAX_DATES_DUE_DATE + ", '%m/%d/%Y') + interval ? year),'%m/%d/%Y') " +
		" where " + DBConstants.FIELD_TAX_DATES_SITE_NAME + " = ?";
	public static final String SQL_INCREASE_PAY_DATE_BY_YEARS = "UPDATE " + DBConstants.TABLE_TAX_DATES + 
		" SET " + DBConstants.FIELD_TAX_DATES_PAY_DATE + 
			" = date_format((str_to_date(" + DBConstants.FIELD_TAX_DATES_PAY_DATE + ", '%m/%d/%Y') + interval ? year),'%m/%d/%Y') " +
		" where " + DBConstants.FIELD_TAX_DATES_SITE_NAME + " = ?";
	
	public static int updateDueOrPayDate(String key, int fieldType, int amount) {
		try {
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			if(fieldType == IncreaseDueOrPayDate.FIELD_DUE_DATE_YEAR_INCREASE) {
				return sjt.update(SQL_INCREASE_DUE_DATE_BY_YEARS, amount, key);
			} else if(fieldType == IncreaseDueOrPayDate.FIELD_PAY_DATE_YEAR_INCREASE) {
				return sjt.update(SQL_INCREASE_PAY_DATE_BY_YEARS, amount, key);
			}
		} catch (Throwable t) {
			logger.error("Error while running updateDueOrPayDate with key = " + key + 
					", fieldType = " + fieldType + ", amount = " + amount, t);
		}
		return 0;
	}

	public static final String SQL_GET_DUE_DATE = "SELECT "	+ DBConstants.FIELD_TAX_DATES_DUE_DATE
		+ " FROM " + DBConstants.TABLE_TAX_DATES + " WHERE " + DBConstants.FIELD_TAX_DATES_SITE_NAME + " = ?";
	
	public static final String SQL_GET_PAY_DATE = "SELECT " + DBConstants.FIELD_TAX_DATES_PAY_DATE
		+ " FROM " + DBConstants.TABLE_TAX_DATES + " WHERE " + DBConstants.FIELD_TAX_DATES_SITE_NAME + " = ?";
	
	public static String getDueOrPayDateBySiteName(String name, String typeOfDate) {
		try {
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			if(typeOfDate == DBConstants.FIELD_TAX_DATES_DUE_DATE) {
				return sjt.queryForObject(SQL_GET_DUE_DATE, String.class, name);
			} else if(typeOfDate == DBConstants.FIELD_TAX_DATES_PAY_DATE) {
				return sjt.queryForObject(SQL_GET_PAY_DATE, String.class, name);
			}
		} catch (Throwable t) {
			logger.error("Error while running getDueOrPayDate for name = " + name );
		}
		return null;
	}
	
	public static List<Map<String, Object>> getHoaCondoData(String SQL, String query, Object querry_params) {
		try {

			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			return sjt.queryForList(SQL + query + ";",querry_params);
			
		} catch (Throwable t) {
			logger.error("Error while getting HOA data ", t);
		}
		return null;
	}
	
	public static boolean insertHOAData(Map<String, Object> query_params) {
		try {

			SimpleJdbcInsert sjt = DBManager.getSimpleJdbcInsert().withTableName(HoaInfoMapper.TABLE_HOA_INFO);

			sjt.execute(query_params);
			
		} catch (Throwable t) {
			DBManager.getLogger().error("Error while updating HOA data ", t);
			return false;
		}
		return true;
	}
	
	public static final String SQL_GET_DISTRICT_NAME = "SELECT " + DBConstants.FIELD_CODE
	+ " FROM " + DBConstants.TABLE_DISTRICTS_NV_CLARK + " WHERE " + DBConstants.FIELD_DISTRICT + " = ?";
	
	public static String getNVClarkDistrictName(String district) {
		try {
			SimpleJdbcTemplate sjt = DBManager.getSimpleTemplate();
			return sjt.queryForObject(SQL_GET_DISTRICT_NAME, String.class, district);
		} catch (Throwable t) {
			logger.error("Error while running getNVClarkDistrictName for district = " + district );
		}
		return null;
	}
	
	public static DTError getDTErrorForCode(String code) {
		String sql = "select * from " + DBConstants.TABLE_DT_ERROR_CODE
			+ " where " + DBConstants.FIELD_DT_ERROR_CODE_ERROR_CODE + " = ?";
		
		ParameterizedRowMapper<DTError> mapper = new ParameterizedRowMapper<DTError>() {
			@Override
			public DTError mapRow(ResultSet rs, int arg1) throws SQLException {
				DTError error = new DTError();
				error.setErrorCode(rs.getString(DBConstants.FIELD_DT_ERROR_CODE_ERROR_CODE));
				error.setLevel(rs.getString(DBConstants.FIELD_DT_ERROR_CODE_LEVEL));
				error.setType(rs.getString(DBConstants.FIELD_DT_ERROR_CODE_TYPE));
				error.setSummary(rs.getString(DBConstants.FIELD_DT_ERROR_CODE_SUMMARY));
				error.setExplanation(rs.getString(DBConstants.FIELD_DT_ERROR_CODE_EXPLANATION));
				error.setAlternate_message(rs.getString(DBConstants.FIELD_DT_ERROR_CODE_ALTERNATE_MESSAGE));
				
				return error;
			}
		};
		
		try {
			List<DTError> errorList = getSimpleTemplate().query(sql, mapper, code);
			if(!errorList.isEmpty()) {
				return errorList.get(0);
			}
		} catch(Exception e) {
			logger.error("Error while getting DT errors ", e);
		}
		
		return null;
	}
	
	static final String SQL_LOCK_SEARCH_EXTERNAL = "INSERT INTO "+ TABLE_LOCKED_SEARCHES +" (searchId) VALUES (?)";
	static final String SQL_IS_LOCKED_SEARCH_EXTERNAL = "SELECT count(*) FROM "+ TABLE_LOCKED_SEARCHES +" WHERE searchId=?";
	static final String SQL_UNLOCK_SEARCH_EXTERNAL = "DELETE FROM "+ TABLE_LOCKED_SEARCHES +" WHERE searchId=?";
	
	public static void lockSearchExternal(long serachId){
		getSimpleTemplate().update(SQL_LOCK_SEARCH_EXTERNAL, serachId);
	}
	
	public static boolean isLockedSearchExternal(long searchId){
		return getSimpleTemplate().queryForInt(SQL_IS_LOCKED_SEARCH_EXTERNAL,searchId)>0;
	}
	
	public static void unlockSearchExternal(long searchId){
		getSimpleTemplate().update(SQL_UNLOCK_SEARCH_EXTERNAL, searchId);
	}
	
	static final String SQL_GET_SEARCH_OPEN_DATE = "SELECT " + DBConstants.FIELD_SEARCH_OPEN_DATE + " FROM " + TABLE_SEARCH + " WHERE "+ DBConstants.FIELD_SEARCH_ID + " =? "; 
	static final String SQL_UPDATE_SEARCH_OPEN_DATE = "UPDATE " + TABLE_SEARCH + " SET " + DBConstants.FIELD_SEARCH_OPEN_DATE + " = ? WHERE "+ DBConstants.FIELD_SEARCH_ID + " =? ";
	static final String SQL_GET_SEARCH_TSR_INITIAL_DATE = "SELECT " + DBConstants.FIELD_SEARCH_TSR_INITIAL_DATE + " FROM " + TABLE_SEARCH + " WHERE "+ DBConstants.FIELD_SEARCH_ID + " =? "; 
	static final String SQL_UPDATE_SEARCH_TSR_INITIAL_DATE = "UPDATE " + TABLE_SEARCH + " SET " + DBConstants.FIELD_SEARCH_TSR_INITIAL_DATE + " = ? WHERE "+ DBConstants.FIELD_SEARCH_ID + " =? ";
	
	public static String getSearchOpenDate(long searchId, Date date){
		try{
			Date d = getSimpleTemplate().queryForObject(SQL_GET_SEARCH_OPEN_DATE, Date.class, searchId);
				
			if(d !=null){
				if(date!=null)
					date = d;
				return d.toString();
			}
		} catch (Exception e) {
			System.err.println("Problem getting open date for search "+ searchId);
			e.printStackTrace();
		}
		
		return "";
	}
	
	public static void updateSearchOpenDate(long searchId, Date date){
		try{
			if(date == null){
				return;
			}
			
			String openDate = new FormatDate(FormatDate.TIMESTAMP).getLocalDate(date);
    		openDate = "str_to_date( '" + openDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
    		
    		getSimpleTemplate().update(SQL_UPDATE_SEARCH_OPEN_DATE.replaceFirst("\\?", openDate), searchId);
			
		} catch (Exception e) {
			System.err.println("Problem getting open date for search "+ searchId);
			e.printStackTrace();
		}
	}
	
	public static String getSearchTSRInitialDate(long searchId, Date date){
		try{
			Date d = getSimpleTemplate().queryForObject(SQL_GET_SEARCH_TSR_INITIAL_DATE, Date.class, searchId);
				
			if(d !=null){
				if(date!=null)
					date = d;
				return d.toString();
			}
		} catch (Exception e) {
			System.err.println("Problem getting TSR initial date for search "+ searchId);
			e.printStackTrace();
		}
		
		return "";
	}
	
	public static void updateSearchTSRInitialDate(long searchId, Date date){
		try{
			if(date==null){
				return;
			}
			
			String initialDate = new FormatDate(FormatDate.TIMESTAMP).getLocalDate(date);
    		initialDate = "str_to_date( '" + initialDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
    		
    		getSimpleTemplate().update(SQL_UPDATE_SEARCH_TSR_INITIAL_DATE.replaceFirst("\\?", initialDate), searchId);
			
		} catch (Exception e) {
			System.err.println("Problem getting TSR initial date for search "+ searchId);
			e.printStackTrace();
		}
	}
	
	public static Vector<CountyState> getCounties(long stateId)
	throws BaseException{
		
		if (logger.isDebugEnabled())   
        	logger.debug("DBManager getCounties# param: " + stateId);
		
		Vector<CountyState> v = new Vector<CountyState>();
		if (stateId==-1) {
			return v;
		}
		
		GenericState stateForId = getStateForId(stateId);
		
		if(stateForId == null) {
			return v;
		}
		
		String stateAbr = stateForId.getStateAbv();
		
		GenericCounty[] all = getAllCounties();
        for (int i = 0; i < all.length; i++) {
        	GenericCounty genericCounty = all[i];
            if (genericCounty.getStateId() == stateId) {
            	CountyState county = new CountyState();
				county.setCountyId(genericCounty.getId());
				county.setCountyName(genericCounty.getName());
				county.setStateAbv(stateAbr);
				v.add(county);
            }
        }
        return v;
	}
	
	static final String SQL_WAS_FVS_FLAGGED_BEFORE 					= "SELECT search_id FROM "+ DBConstants.TABLE_FVS_DATA +" WHERE search_id = ?";
	static final String SQL_RESET_FVS_FLAG_SEARCH 					= "UPDATE " + DBConstants.TABLE_FVS_DATA + " SET flag = ?, flag_date = ? WHERE search_id = ?";
	static final String SQL_SET_FVS_FLAG_SEARCH 					= "UPDATE " + DBConstants.TABLE_FVS_DATA + " SET flag = ?, flag_date = ?, updates_runned = ? WHERE search_id = ?";
	static final String SQL_SET_RESET_FVS_FLAG_SEARCH_WITH_INSERT 	= "INSERT INTO "+ DBConstants.TABLE_FVS_DATA +" (search_id, flag, abstr_fileno, run_time, flag_date, comm_id, agent_id, updates_runned, county_id)"
																		+" VALUES (?,?,?,?,?,?,?,?,?)";
	static final String SQL_UPDATE_FVS_STATUS 						= "UPDATE " + DBConstants.TABLE_SEARCH_FLAGS + " SET forFVS = ? WHERE search_id = ?";
	static final String SQL_DELETE_FVS_ENTRY 						= "DELETE FROM " + DBConstants.TABLE_FVS_DATA + " WHERE " + DBConstants.FIELD_FVS_SEARCH_ID + " in (?)";
	
	
	public static void resetFVSFlagWhenReopenOrDeleteSearch(long search_id, String searchIds, boolean updateStatus) throws BaseException, UpdateDBException{
		if (updateStatus){
			getSimpleTemplate().update(SQL_UPDATE_FVS_STATUS, 0, search_id);
		}
		if (org.apache.commons.lang.StringUtils.isNotEmpty(searchIds)){
			getSimpleTemplate().update(SQL_DELETE_FVS_ENTRY, searchIds);
		} else if (search_id != -1){
			getSimpleTemplate().update(SQL_DELETE_FVS_ENTRY, search_id);
		}
	}
	
	public static void setFVSFlagSearch(long search_id, UserAttributes ua, boolean resetFVSFlag) throws BaseException, UpdateDBException{
		
		long searchId = -1;
		
		try {
			searchId = getSimpleTemplate().queryForLong(SQL_WAS_FVS_FLAGGED_BEFORE, search_id);
		} catch (Exception e) {}
		
		Calendar cal = Calendar.getInstance();
		
		if (searchId == -1){
			Map<String,Object> search = getSimpleTemplate().queryForMap("SELECT *, p.county_id FROM "+ DBConstants.TABLE_SEARCH 
					+ " s JOIN " + DBConstants.TABLE_PROPERTY + " p ON s.property_id = p.id  WHERE s.id = ?", search_id);
			if (search != null){
				Date date = (Date) search.get("sdate");
				Calendar cals = Calendar.getInstance();
				cals.setTime(date);
				String sTime  = cals.get(Calendar.HOUR_OF_DAY) + ":" + cals.get(Calendar.MINUTE) + ":" + cals.get(Calendar.SECOND);

				getSimpleTemplate().update(SQL_SET_RESET_FVS_FLAG_SEARCH_WITH_INSERT, search_id, "Y", search.get("file_id"), sTime, 
															cal.getTime(), search.get("comm_id"), search.get("agent_id"), 0, search.get("county_id"));
				
				getSimpleTemplate().update(SQL_UPDATE_FVS_STATUS, 1, search_id);
			}
		} else{
			if (resetFVSFlag){
				getSimpleTemplate().update(SQL_RESET_FVS_FLAG_SEARCH, null, cal.getTime(), search_id);
				getSimpleTemplate().update(SQL_UPDATE_FVS_STATUS, 0, search_id);
			} else{
				getSimpleTemplate().update(SQL_SET_FVS_FLAG_SEARCH, "Y", cal.getTime(), 0, search_id);
				getSimpleTemplate().update(SQL_UPDATE_FVS_STATUS, 1, search_id);
			}			
		}
	}
	
	public static List<FVSMapper> getFVSFlaggedSearches() throws BaseException, UpdateDBException{
		
		List<FVSMapper> flaggedSearches = getSimpleTemplate().query(FVSMapper.SQL_SELECT_FVS_FLAGGED_SEARCHES, new FVSMapper(), "Y");
		
		return flaggedSearches;
	}
	
	public static int getFVSUpdatesRunned(long search_id) throws BaseException, UpdateDBException{
		
		int updatesRunned = getSimpleTemplate().queryForInt(FVSMapper.SQL_SELECT_FVS_UPDATES_RUNNED, search_id);
		
		return updatesRunned;
	}
	static final String SQL_UPDATE_FVS_LAST_SCHEDULED_RUNNED = "UPDATE " + DBConstants.TABLE_FVS_DATA + 
													" SET " + DBConstants.FIELD_FVS_UPDATES_RUNNED + " = ?, " + DBConstants.FIELD_FVS_FLAG + "= 'X' " +
														"WHERE " + DBConstants.FIELD_FVS_SEARCH_ID + " = ? ";
	static final String SQL_UPDATE_FVS_RUNNED = "UPDATE " + DBConstants.TABLE_FVS_DATA + 
													" SET " + DBConstants.FIELD_FVS_UPDATES_RUNNED + " = ?" +
														"WHERE " + DBConstants.FIELD_FVS_SEARCH_ID + " = ? ";
	public static void updateFVSUpdatesRunned(int updatesRunned, long search_id, boolean lastScheduled) throws BaseException, UpdateDBException{
		
		if (lastScheduled){
			getSimpleTemplate().update(SQL_UPDATE_FVS_LAST_SCHEDULED_RUNNED, updatesRunned, search_id);
			getSimpleTemplate().update(SQL_UPDATE_FVS_STATUS, 0, search_id);
		} else{
			getSimpleTemplate().update(SQL_UPDATE_FVS_RUNNED, updatesRunned, search_id);
		}
	}
	
	public static List<Map<String,Object>> getCommSites(int commId, String countyId){		
		
		String sql = "SELECT " + DBConstants.FIELD_COMMUNITY_SITES_SITE_TYPE 
				+ " FROM " + DBConstants.TABLE_COMMUNITY_SITES 
				+ " WHERE " + DBConstants.FIELD_COMMUNITY_SITES_COUNTY_ID + " =? " 
					+ " AND " + DBConstants.FIELD_COMMUNITY_SITES_COMMUNITY_ID + " = ? "
					+ " AND " + DBConstants.FIELD_COMMUNITY_SITES_ENABLE_STATUS + " != 0";
		
		List<Map<String,Object>> siteTypesPerCounty = DBManager.getSimpleTemplate().queryForList(sql, countyId, commId);
		
		StringBuffer siteTypeList = new StringBuffer();
		for (Map<String,Object> map : siteTypesPerCounty){
			siteTypeList.append(map.get("site_type")).append(", ");
		}
		
		String siteTypes = siteTypeList.toString().replaceFirst(",\\s*$", "");
		
		String lastSql = "SELECT s.id_county, m.site_type, m.site_abrev, m.description, s.is_enabled, s.city_name, s.effective_start_date "
					+ " FROM " + DBConstants.TABLE_MAP_SITE_TYPE_TO_P2 + " m JOIN " + DBConstants.TS_SITES + " s ON (m.site_type = s.site_type) " 
					+ " WHERE s.id_county = ? AND s.site_type IN (" + siteTypes + ") AND m.site_type IN (" + siteTypes + ") GROUP BY site_type ORDER BY site_type";
		
		List<Map<String,Object>> siteTypesPer = DBManager.getSimpleTemplate().queryForList(lastSql, countyId);
		
		return siteTypesPer;
	}
		
	static final String GET_ALL_BASE_SEARCHES_FOR_COMMUNITY_STATE_COUNTY = "SELECT s." + DBConstants.FIELD_SEARCH_ID + " FROM " + DBConstants.TABLE_SEARCH + " s "
			+ " JOIN " + DBConstants.TABLE_SEARCH_FLAGS + " sf ON sf." + DBConstants.FIELD_SEARCH_FLAGS_ID + " = s." + DBConstants.FIELD_SEARCH_ID
			+ " LEFT JOIN " + DBConstants.TABLE_USER + " u ON u." + DBConstants.FIELD_USER_ID + " = s." + DBConstants.FIELD_SEARCH_AGENT_ID
			+ " JOIN " + DBConstants.TABLE_COMMUNITY + " c ON c." + DBConstants.FIELD_COMMUNITY_COMM_ID + " = s." + DBConstants.FIELD_SEARCH_COMM_ID
			+ " RIGHT JOIN " + DBConstants.TABLE_PROPERTY + " p ON p." + DBConstants.FIELD_PROPERTY_ID + " = s." + DBConstants.FIELD_SEARCH_PROPERTY_ID
			+ " WHERE sf." + DBConstants.FIELD_SEARCH_FLAGS_STARTER + " = 1"
				+ " AND s." + DBConstants.FIELD_SEARCH_COMM_ID + " = ?"
				+ " AND p." + DBConstants.FIELD_PROPERTY_STATE_ID + " = ?"
				+ " AND p." + DBConstants.FIELD_PROPERTY_COUNTY_ID + " = ?";
	
	static final String GET_ALL_NON_BASE_SEARCHES_FOR_COMMUNITY_STATE_COUNTY = "SELECT s." + DBConstants.FIELD_SEARCH_ID + " FROM " + DBConstants.TABLE_SEARCH + " s "
			+ " JOIN " + DBConstants.TABLE_SEARCH_FLAGS + " sf ON sf." + DBConstants.FIELD_SEARCH_FLAGS_ID + " = s." + DBConstants.FIELD_SEARCH_ID
			+ " LEFT JOIN " + DBConstants.TABLE_USER + " u ON u." + DBConstants.FIELD_USER_ID + " = s." + DBConstants.FIELD_SEARCH_AGENT_ID
			+ " JOIN " + DBConstants.TABLE_COMMUNITY + " c ON c." + DBConstants.FIELD_COMMUNITY_COMM_ID + " = s." + DBConstants.FIELD_SEARCH_COMM_ID
			+ " RIGHT JOIN " + DBConstants.TABLE_PROPERTY + " p ON p." + DBConstants.FIELD_PROPERTY_ID + " = s." + DBConstants.FIELD_SEARCH_PROPERTY_ID
			+ " WHERE sf." + DBConstants.FIELD_SEARCH_FLAGS_STARTER + " = 0"
				+ " AND s." + DBConstants.FIELD_SEARCH_COMM_ID + " = ?"
				+ " AND p." + DBConstants.FIELD_PROPERTY_STATE_ID + " = ?"
				+ " AND p." + DBConstants.FIELD_PROPERTY_COUNTY_ID + " = ?";

	public static List<Map<String,Object>> getAllBaseOrNonBaseSearchesForCommunityStateCounty(int commId, String stateId, String countyId, boolean base) throws BaseException, UpdateDBException{
			
		List<Map<String,Object>> allBaseSearches = new ArrayList<Map<String,Object>>();
		
		if (base){
			allBaseSearches = getSimpleTemplate().queryForList(GET_ALL_BASE_SEARCHES_FOR_COMMUNITY_STATE_COUNTY, commId, stateId, countyId);
		} else{
			allBaseSearches = getSimpleTemplate().queryForList(GET_ALL_NON_BASE_SEARCHES_FOR_COMMUNITY_STATE_COUNTY, commId, stateId, countyId);
		}
			
		return allBaseSearches;
	}
	
	private static final String SQL_SEARCH_FOR_WEBSERVICE_GUID = 
        "SELECT s." + DBConstants.FIELD_SEARCH_ID + 
        	" FROM " + DBConstants.TABLE_SEARCH + " s " + 
        		" JOIN " + DBConstants.TABLE_SEARCH_EXTERNAL_FLAGS + 
        			" sef ON sef." + SearchExternalFlag.FIELD_SEARCH_ID + " = s." + DBConstants.FIELD_SEARCH_ID + 
        	" WHERE " +
        		" sef." + SearchExternalFlag.FIELD_SO_ORDER_ID + " = ?";
	
	public static long getSearchIdBasedOnGuid(String guid) {
    	try {
    		if (org.apache.commons.lang.StringUtils.isNotBlank(guid)) {
	    		return getSimpleTemplate().queryForLong(SQL_SEARCH_FOR_WEBSERVICE_GUID, guid);
    		}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		}
    	return -1;
    }
}