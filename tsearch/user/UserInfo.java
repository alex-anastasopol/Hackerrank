package ro.cst.tsearch.user;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.stewart.ats.base.search.DocumentsManagerI;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.TSDIndexPage;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.threads.ASThread;
import ro.cst.tsearch.utils.InstanceManager;

public class UserInfo 
{
	private static final Logger logger = Logger.getLogger(UserInfo.class);
	
	public static final String DB_LAST_COUNTY = "LAST_COUNTY";
	public static final String DB_LAST_STATE = "LAST_STATE";
	public static final String DB_LAST_START_DATE = "LAST_START_DATE";
	public static final String DB_MODULE_SEARCH = "SEARCH";
	public static final String DB_MODULE_SETTINGS = "SETTINGS";
	
	public static final String DB_USER_ID_FIELD = "USER_ID";
	public static final String DB_ATTRIBUTE_FIELD = "ATTRIBUTE";
	public static final String DB_VALUE_FIELD = "VAR_VALUE";
	public static final String DB_MODULE_FIELD = "MODULE";
	
	public static final String DB_SETTINGS_TABLE = "TS_SETTINGS";
	
	private Search global = null;
	
	public UserInfo(Search global) 
	{
		this.global = global;
		setInfo();
	}
	
	private Date startDate = null;
	private int county = -1;
	private int state = -1;
	private int agentId = -1;
	private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
	private SimpleDateFormat sdfBackup = new SimpleDateFormat("MM/dd/yyyy");
	
	public boolean setInfo() 
	{
	
		if (global == null || global.getID() == Search.SEARCH_NONE)
		{
			return false;
		}
		
		try {
			county = Integer.parseInt(global.getSa().getAtribute(SearchAttributes.P_COUNTY));
			state = Integer.parseInt(global.getSa().getAtribute(SearchAttributes.P_STATE));
			try{
				startDate = sdf.parse(global.getSa().getAtribute(SearchAttributes.FROMDATE));
			}catch(ParseException pe){
				startDate = sdfBackup.parse(global.getSa().getAtribute(SearchAttributes.FROMDATE));
			}
			
		} catch (Exception e) {}
		
		UserAttributes currentAgent = global.getAgent();
		
		if (currentAgent != null
				&& global.isAllowGetAgentInfoFromDB()) 
		{
			try {
				// verificam daca are setate valori pentru STATE/COUNTY/STARTDATE
				HashMap agentData = getUserInfo(currentAgent.getID().intValue());
				if (checkDataIntegrityInHash(agentData))
				{
					county = Integer.parseInt((String) agentData.get(DB_LAST_COUNTY));
					state = Integer.parseInt((String) agentData.get(DB_LAST_STATE));
					try{
						startDate = sdf.parse((String) agentData.get(DB_LAST_START_DATE));
					}catch(ParseException pe){
						startDate = sdfBackup.parse((String) agentData.get(DB_LAST_START_DATE));
					}
					//	daca am gasit tot la agent...terminam
					setData();
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e);
				return false;
			}
		}
		
		UserAttributes currentAbstractor = global.getSa().getAbstractorObject();
		
		
		// este permisa numai o singura cerere spre BD in cadrul unui search
		// pentru a nu fi influentat de schimbari facute in paralel
		
		if (currentAbstractor != null
				&& global.isAllowGetAgentInfoFromDB()) 
		{
			try {
				//	verificam daca are setate valori pentru STATE/COUNTY/STARTDATE
				HashMap abstractorData = getUserInfo(currentAbstractor.getID().intValue());
				if (checkDataIntegrityInHash(abstractorData))
				{
					
					county = Integer.parseInt((String) abstractorData.get(DB_LAST_COUNTY));
					state = Integer.parseInt((String) abstractorData.get(DB_LAST_STATE));
					Object startDateObject = abstractorData.get(DB_LAST_START_DATE);
					if (startDateObject instanceof Date)
					{
						startDate = (Date) startDateObject;
					}
					else if (startDateObject instanceof String) 
					{
						try{
							startDate = sdf.parse((String) startDateObject);
						}catch(ParseException ps){
							startDate = sdfBackup.parse((String) startDateObject);
						}
					}
					if (currentAbstractor.getAGENTID() != null)
					{
						agentId = currentAbstractor.getAGENTID().intValue();
					}
					
					//	daca am gasit tot la abstractor...terminam
					setData();
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e);
				return false;
			}
		}
		startDate = null;
		setData();		
		return true;
	}
	
	public void setData() {
		
		ASThread asThread = ASMaster.getSearch(global);
		
		boolean automaticSearch = (global.getSearchType() == Search.AUTOMATIC_SEARCH && global.getSearchStarted()) 
					|| (asThread != null && asThread.isAlive());
		
		boolean hasDocuments = false;
		DocumentsManagerI doc = global.getDocManager();
		try{
			doc.getAccess();	
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			if(doc != null) {
				hasDocuments = doc.size()>0;
				doc.releaseAccess();
			} else {
				hasDocuments = false;
			}
		}
		if (!automaticSearch 
				&& !hasDocuments
				&& global.searchCycle == 0
				&& global.isAllowGetAgentInfoFromDB()) 
		{
			
			SearchAttributes sa = global.getSa();
			if (sa != null) 
			{
				synchronized(global) 
				{
					global.setAllowGetAgentInfoFromDB(false);
					try {
						startDate = sa.setDefaultFromDate();
					} catch (Exception e) {
						logger.error("Some error while setting official start date", e);
						if (startDate != null)
						{
							sa.setAtribute(SearchAttributes.FROMDATE, sdf.format(startDate));
						}
					}
					
				}
			}
		}
	}
	
	public boolean checkDataIntegrityInHash(HashMap inHash) {
		if (inHash.get(DB_LAST_COUNTY) == null)
		{
			return false;
		}
		if (inHash.get(DB_LAST_STATE) == null)
		{
			return false;
		}
		if (inHash.get(DB_LAST_START_DATE) == null)
		{
			if (!global.isOrderedSearch())
			{
				return false;
			}
			else
			{
				try 
				{
					startDate = sdf.parse(global.getSa().getAtribute(SearchAttributes.FROMDATE));
					inHash.put(DB_LAST_START_DATE, startDate);
				} 
				catch (Exception e) {return false;}
			}
		}
		return true;
	}

	////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////
	//
	//			DATA SECTION
	//
	////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 * Get saved information about agent from database
	 * 
	 * @param userId
	 * @return HashMap with <b>key</b> name of the attribute and <b>value</b> 
	 * value from database
	 */
	private HashMap getUserInfo(int userId) {
		
		DBConnection conn = null;
		
		HashMap userData = new HashMap();
		
		String stm = " select * from " +
					DB_SETTINGS_TABLE +
					" where " +
					DB_USER_ID_FIELD +
					" = " +
					userId;
		
		try {
		    
		    conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData data = conn.executeSQL(stm);
			
			for (int i = 0; i < data.getRowNumber(); i++){
				userData.put(data.getValue(DB_ATTRIBUTE_FIELD, i), data.getValue(DB_VALUE_FIELD, i));
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
		
		return userData;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return Returns the agentId.
	 */
	public int getAgentId() {
		return agentId;
	}

	/**
	 * @return Returns the county.
	 */
	public int getCounty() {
		return county;
	}

	/**
	 * @return Returns the startDate.
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @return Returns the state.
	 */
	public int getState() {
		return state;
	}
}
