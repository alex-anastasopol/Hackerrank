package ro.cst.tsearch.servers.types;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.loadBalServ.ServerInfoSingleton;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.searchsites.client.Util;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.threads.deadlock.ThreadDeadlockDetector;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;


public class TSServersFactory {
	
	static final long serialVersionUID = 10000000;
	
	public static final String DEFAULT_PATH_TO_TS_SERVERS_TYPES = "ro.cst.tsearch.servers.types.";
	
    public static String msRequestSolverName = URLMaping.path
            + URLMaping.URL_CONN_READER_SERVLET;

    //name and path (ifneeded) of the class which made the call and which will
    // solve any link from jsp()
    //the names and values which identify this server for the creator, this val
    // is passed by creator and is used internal only when the server create a
    // html link to itself
    public static final  String msPrmNameLink = "Link";

    //the parameter name wich identify that is a call from a link which server
    // itself built and it know how to solve it
    public final static String msSitePath = "/title-search"; //site path
   
    private static final Category logger = Category.getInstance(TSServersFactory.class.getName());

    public static boolean isAssesor(int ct) {
        return "AO".equals(getType(ct)) || "IS".equals(getType(ct)) || "NB".equals(getType(ct)) || GWTDataSite.PRI_TYPE==getServerTypeFromSiteId(ct);
    }

    public static boolean isRegister(int ct) {
        return "RO".equals(getType(ct))||"RV".equals(getType(ct))||"OR".equals(getType(ct))
        ||"LA".equals(getType(ct))||"AD".equals(getType(ct))||"TS".equals(getType(ct))
        ||"PI".equals(getType(ct))||"TP".equals(getType(ct))||"SK".equals(getType(ct))||"AC".equals(getType(ct))
        ||"DT".equals(getType(ct))||"ST".equals(getType(ct))||"DG".equals(getType(ct));
    }

    public static boolean isCountyTax(int ct) {
        return ("TR".equals(getType(ct)) || isCountyNTNTax(ct) || isCountyTR2Tax(ct));
    }

    public static boolean isCountyNTNTax(int ct) {
        return "NTN".equals(getType(ct));
    }
    
    public static boolean isCountyTR2Tax(int ct) {
        return "TR2".equals(getType(ct));
    }
    
    public static boolean isCountyTUTax(int ct) {
        return "TU".equals(getType(ct));
    }
    
    public static boolean isCityTax(int ct) {
        return StringUtils.isNotEmpty(getType(ct)) && getType(ct).startsWith("Y");
    }

    public static boolean isDailyNews(int ct) {
        return "DN".equals(getType(ct));
    }

    public static boolean isPatriots(int ct) {
        return "PA".equals(getType(ct));
    }
    
    public static boolean isUniformCommercialCode(int ct) {
        return "CC".equals(getType(ct));
    }
    
    public static boolean isCourts(int ct) {
        return "CO".equals(getType(ct));
    }
    
    public static boolean isPacer(int ct) {
        return "PC".equals(getType(ct));
    }
    
    public static boolean isOrbit(int ct) {
        return "OR".equals(getType(ct));
    }

    public static boolean isStewartPrior(int ct) {
        return "PF".equals(getType(ct));
    }
    
    public static boolean isDataTrace(int ct) {
        return "DT".equals(getType(ct));
    }
    
    public static boolean isLA(int ct) {
        return "LA".equals(getType(ct)) || "AD".equals(getType(ct));
    }
   
    public static String getType(int ct) {
        return HashCountyToIndex.getDateSiteForMIServerID(-1, ct).getSiteTypeAbrev();
    }

    /**
     * Returns the site abbreviation for a given siteType
     * @param siteTypeAbbreviation
     * @return
     */
    public static int getSiteTypeId(String siteTypeAbbreviation) {
        String []allABR = Search.getReadOnlyServerTypesAbrev();
        if(siteTypeAbbreviation == null ){
        	return 0;
        }   
        for(int i=0;i<allABR.length;i++){
        	if(   siteTypeAbbreviation.equalsIgnoreCase(allABR[i])    ){
        		return i+1;
        	}
        }
        return 0;
    }

    public static long getSiteIdfromCountyandServerTypeId(int countyid,
            int srvtypeid) {
        return countyid * 100 + srvtypeid;
    }
    
    /**
     * Given a valid siteId will return the code for the corresponding server 
     * (for example for NB will return 25)<br>
     * If the siteId is not valid it will return -1
     * @param siteId
     * @return a valid server type or -1
     */
    public static int getServerTypeFromSiteId(int siteId) {
    	if(siteId > 100) {
    		return (siteId - 1)%100;
    	}
    	return -1;
    }

    public static long getCountyId(String stateCounty){
    	        
        String stateName = stateCounty.substring(0, 2);
	    String countyName = stateCounty.substring(2, stateCounty.length());

	    try {
            return County.getCounty(countyName, stateName).getCountyId().longValue();
        } catch (BaseException e) {
            logger.error("[TSServersFactory] Error getting ID for county name : " + stateCounty);
            e.printStackTrace();
            return -1;
        }
    }
    
    public static long getSiteId(String stateAbbreviation, String countyName, String siteType) {
        long rez = 0;

	    try {
            rez = County.getCounty(countyName, stateAbbreviation).getCountyId().longValue()* 100 + getSiteTypeId(siteType);
        } catch (BaseException e) {
            logger.error("[TSServersFactory] Error getting ID from site name : " 
            		+ "stateAbbreviation = " + stateAbbreviation + 
            		" ,countyName = " + countyName + 
            		", siteType = " + siteType );
            e.printStackTrace();
        }
        
        return rez;
    }
    /**
     * Build sites idS for a given countyId.
     * @param countyId
     * @return
     */
    public static List<Long> getSiteIds(int countyId) {
    	List<Long> rez = new LinkedList<Long>();
    	String []allABR = Search.getReadOnlyServerTypesAbrev();
    	    	
    	for (String site : allABR) {
    		rez.add( Long.valueOf(countyId) * 100 + getSiteTypeId(site) ) ;
		}
    	
        return rez;
    }

    
    public static String getSiteName(long id) {
    	DataSite data =  HashCountyToIndex.getDateSiteForMIServerID(-1,  (int)id );
    	String result = data.getName();
        logger.info("[TSServersFactory] Site id : " + id+ " has name : " + result);
        return result;
    }
	
    public static Class IdToClass(int id) throws Exception {
    	DataSite data =  HashCountyToIndex.getDateSiteForMIServerID(-1,  id );
        String classFullName =  constructFullName(data.getTsServerClassName());
        Class c = Class.forName(classFullName);
        return c;
    }

    private TSServersFactory() {}

    public static TSServerInfo GetServerInfo(int viServerID,long searchId) {
        return GetServerInstance(viServerID, "", "",searchId).getDefaultServerInfo();
    }

    public static TSInterface GetServerInstance(String sP1, String sP2,long searchId)
            throws NumberFormatException, BaseException {
    	
    	int p1 = 79, p2 = 0;
    	
    	try { p1 = Integer.parseInt(sP1); } catch (Exception e) {e.printStackTrace();}
    	try { p2 = Integer.parseInt(sP2); } catch (Exception e) {e.printStackTrace();}
    	
        int viServerID = HashCountyToIndex.getServerFactoryID(p1, p2);
        
        return GetServerInstance(viServerID, sP1, sP2,searchId);
    }

    public static String getTSServerInstanceClassName(int commId, String sP1, String sP2) throws NumberFormatException, BaseException {
    	
    	int p1 = 79, p2 = 0;
    	
    	try { p1 = Integer.parseInt(sP1); } catch (Exception e) {e.printStackTrace();}
    	try { p2 = Integer.parseInt(sP2); } catch (Exception e) {e.printStackTrace();}
    	
        int viServerID = HashCountyToIndex.getServerFactoryID(p1, p2);
        DataSite data =  HashCountyToIndex.getDateSiteForMIServerID(commId,  viServerID );
        
        return data.getTsServerClassName();
        
    }
    
    
    public static String getTSServerName(int commId, String sP1, String sP2) throws NumberFormatException, BaseException {
    	
    	int p1 = 79, p2 = 0;
    	
    	try { p1 = Integer.parseInt(sP1); } catch (Exception e) {e.printStackTrace();}
    	try { p2 = Integer.parseInt(sP2); } catch (Exception e) {e.printStackTrace();}
    	
        int viServerID = HashCountyToIndex.getServerFactoryID(p1, p2);
        DataSite data =  HashCountyToIndex.getDateSiteForMIServerID( commId, viServerID );
        
        return data.getName();
        
    }
    
    public static String getCrtServerName(long searchId, boolean isParentSite) throws BaseException{
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		Search search = currentInstance.getCrtSearchContext();
		String p1 = isParentSite?search.getP1ParentSite():search.getP1();
		String p2 = isParentSite?search.getP2ParentSite():search.getP2();
		return TSServersFactory.getTSServerName(currentInstance.getCommunityId(), p1, p2);
	}

	public static String getTSServerInstanceClassName(int commId, int viServerID) throws NumberFormatException, BaseException {
		
	    DataSite data =  HashCountyToIndex.getDateSiteForMIServerID( commId, viServerID );
	    
	    return data.getTsServerClassName();
	    
	}
	
	
	public static String getTSServerName(int commId, int viServerID) throws NumberFormatException, BaseException {
		
	    DataSite data =  HashCountyToIndex.getDateSiteForMIServerID( commId, viServerID );
	    
	    return data.getName();
	    
	}
    
     private  static String constructFullName(String className){
    	 
    	 if( className==null ){
    		 return null;
    	 }
    	int poz = className.lastIndexOf(".");
    	if(poz>=0){
    		return className;
    	}
    	else{
    		return DEFAULT_PATH_TO_TS_SERVERS_TYPES + className;
    	}
    	
    } 
    
	 public static TSInterface GetServerInstance(int viServerID, long searchId) {
		 return GetServerInstance(viServerID, "", "",searchId) ;
	 }
     
    public static TSInterface GetServerInstance(int viServerID, String sP1, String sP2,long searchId) {
 
        String msServerID = URLConnectionReader.PRM_NAME_P1 + "=" + sP1 + "&"+ URLConnectionReader.PRM_NAME_P2 + "=" + sP2;

        TSInterface rtTSInterface = null;
        Class serverClass = null;
        Class parameterTypes[] = { String.class, String.class, String.class, String.class, long.class, int.class};
        Object parameters[] = { msRequestSolverName, msSitePath, msServerID, msPrmNameLink, searchId, viServerID};

        int commId = 0;
        
        try {
        	
        	commId = InstanceManager.getManager().getCommunityId(searchId);
        	
        	DataSite data =  HashCountyToIndex.getDateSiteForMIServerID( 
        			commId, 
        			viServerID );
        	if(data==null){
        		try {
    				// errors
    				String from = MailConfig.getMailFrom();
    				String to = DBManager.getConfigByName("replication.notification.email");
    				String cc = MailConfig.getSupportEmailAddress();
    				if(URLMaping.INSTANCE_DIR.startsWith("local")) {
    					cc = "";
    				}
    				String subject = "ERROR: " + viServerID+" does not exist in search sites configuration";
    				
    				EmailClient email = new EmailClient();
    				email.setFrom(from);
    				if (!StringUtils.isEmpty(to)) {
    					email.addTo(to);
    					email.addCc(cc);
    				} else {
    					email.addTo(cc);
    				}
    				email.setSubject(subject);
    				email.setContent(subject + " \n searchId = " + 
    						searchId + " \n P1=" + sP1 + " \n P2 = " + sP2 + "\n\n" +
    						ServerInfoSingleton.getThreadInfoRepresentation(ThreadDeadlockDetector.getMbean().getThreadInfo(Thread.currentThread().getId(), Integer.MAX_VALUE)), "text/html");
    				
    				email.sendNow();
    				
    			} catch (Exception exc) {
    				logger.error(
    						"ro.cst.tsearch.servers.types.TSServersFactory.GetServerInstance(int, String, String, long) msServerID = " + 
    						msServerID + " searchId = " + searchId, 
    						exc);
    			}
        		throw new RuntimeException(viServerID+" does not exist in search sites configuration");
        	}
        	
			
            String classFullName =  constructFullName(data.getTsServerClassName());
            logger.debug( classFullName);
            if ( !(!data.getLink().isEmpty() && data.getTsServerClassName().isEmpty())){
            	try{
            		serverClass = Class.forName( classFullName);
                    rtTSInterface = (TSInterface) serverClass.getConstructor(parameterTypes).newInstance(parameters);
                    rtTSInterface.setServerID(viServerID);
                    if(StringUtils.isEmpty(sP1) || StringUtils.isEmpty(sP2)) {
                    	rtTSInterface.setMsServerId("p1="+data.getIndex()+"&p2="+data.getP2());
                    }
            	}catch (NoClassDefFoundError cnfe ){
            		cnfe.printStackTrace();
            	}
            }else{//there is a need that need to be satisfied: 
            	  //a site defined only with a link in Search sites can have it's PArent Site accesed.
              	  //see http://ldap.cst.ro/bugzilla/show_bug.cgi?id=5441#c2
            	if (Util.isSiteEnabled(data.getEnabled(commId))){
            		rtTSInterface = new TSServer(msRequestSolverName, msSitePath, msServerID, msPrmNameLink, searchId, viServerID);
            	}
            }
        } catch (Exception e) {
        	logger.error("Error while creating server for parameters: viServerID = [" + 
        			viServerID + "], sP1 = [" + 
        			sP1 + "], sP2 = [" + 
        			sP2 + "], searchId = [" + 
        			searchId + "]", e);
        	if(e instanceof InvocationTargetException){
        		InvocationTargetException te = (InvocationTargetException)e;
        		te.getTargetException().printStackTrace();
        		System.err.println(" ------ end InvocationTargetException -------");
        	}
            e.printStackTrace();
        }
        
        return rtTSInterface;
    }
    
}