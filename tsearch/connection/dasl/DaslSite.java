
package ro.cst.tsearch.connection.dasl;

import static ro.cst.tsearch.datatrace.Utils.fillXMLParameter;
import static ro.cst.tsearch.datatrace.Utils.fillXMLTemplate;
import static ro.cst.tsearch.utils.XmlUtils.xpathQuery;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.ErrorRequestBean;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.database.rowmapper.RequestCount;
import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.threads.ASThread;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XmlUtils;

/**
 * 
 * @author radu bacrau
 */
public class DaslSite implements DaslConnectionSiteInterface {

	protected static final Category logger = Logger.getLogger(DaslSite.class);

	private DaslConnection  daslConnection = null ;
	
	private static final int RETRY_PLACE_ORDER = 2;  // times
	private static final int DELAY_PLACE_ORDER = 2;  // seconds	
	private static final int RETRY_GET_IMAGE_ORDER = 2;  // times
	private static final int DELAY_GET_IMAGE_ORDER = 2;  // seconds		
	private static final int RETRY_GET_ORDER   = 30; // times
	private static final int DELAY_GET_ORDER   = 30; // seconds
	private static final int MAX_SEQ_ERRS      = 4;  // times
	
	// connection parameters
	public Map<String,String> connectionParameters = new HashMap<String,String>();

	// local testing cache
	public ResponseCache cache = null;
	
	// site name. value will be overwritten
	public String siteName = getClass().getSimpleName();
	
	private static final String[] POSSIBLE_PARAMS = {
		"CountyFIPS",
		"StateFIPS",
		"ProviderId",
		"ProductId",
		"ImageProductId"
	};
	
	protected String[] getPossibleParams(){
		return POSSIBLE_PARAMS;
	}
	
	private int miServerID = 0;
	private long searchId;
	
	DataSite dat = null;
	
	/**
	 * constructor
	 */
	public DaslSite(int miServerID, long searchId ){		
 	
		this.miServerID = miServerID;
		this.searchId =searchId;
		
		dat = (DataSite)HashCountyToIndex .getDateSiteForMIServerID(
				InstanceManager.getManager().getCommunityId(searchId), 
				miServerID);
		daslConnection = new DaslConnection(this.miServerID);
		// construct site data
		this.siteName = dat.getName();
		cache = ResponseCache.setupResponseCache(siteName);
		
		// add the connection parameters
		for(String key: POSSIBLE_PARAMS){
			String value = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), siteName, key);
			connectionParameters.put(key,value);
		}
	}	
	
	
	
	/**
	 * Fill the connection parameters 
	 * @param xmlQuery
	 * @return xmlQuery with connections parameters filled
	 */
	private String fillConnectionParameters(String xmlQuery){
		
		DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
				InstanceManager.getManager().getCommunityId(searchId), 
				miServerID);
		if(dat.getName().endsWith("AO")||dat.getName().endsWith("NB")){
			xmlQuery = fillXMLParameter(xmlQuery, "IgnoreWorkflow", "true");
			xmlQuery = fillXMLParameter(xmlQuery, "SequenceNumber", "1");
			xmlQuery = xmlQuery.replaceAll("(?i)<ClientTransactionReference>[^<]*</ClientTransactionReference>",""/*"<ClientTransactionReference/>"*/);
			xmlQuery = xmlQuery.replaceAll("[<]ClientReference[ ]*[/][>]","");
		}
		else{
			xmlQuery = xmlQuery.replaceAll("(?i)<IgnoreWorkflow>[^<]*</IgnoreWorkflow>","<IgnoreWorkflow/>");
			xmlQuery = xmlQuery.replaceAll("(?i)<SequenceNumber>[^<]*</SequenceNumber>","<SequenceNumber/>");
		}
		
		// fill the "fixed" parameters from the site parameters
		xmlQuery = fillXMLTemplate(xmlQuery, connectionParameters);
		
		// fill the "variable" parameters from the passwords module
		String userName = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), siteName, "UserName");
		String  clientId = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), siteName, "ClientId");		
		xmlQuery = fillXMLParameter(xmlQuery, "UserName", userName);
		xmlQuery = fillXMLParameter(xmlQuery, "ClientId", clientId);
		
		// fill timestamp
		String timeStamp =  new SimpleDateFormat("MM/dd/yyyy").format(new Date());
		xmlQuery = fillXMLParameter(xmlQuery, "TimeStamp", timeStamp);
		
		
		// return
		return xmlQuery;
	}
	
	/**
	 * Query the status of an order
	 * @param id the id of the order
	 * @return structure with response
	 *           status = ORDER_PLACED | ORDER_ERROR | ORDER_COMPLETED
	 */
	private DaslResponse queryOrder(int id){
		
		String xmlResponse = daslConnection.getOrder(id);
		
		DaslResponse retVal = new DaslResponse();
		retVal.id = id;
		retVal.xmlResponse = xmlResponse;
		
		if(xmlResponse == null){
			retVal.status = ORDER_ERROR;
		} else {
			String status = StringUtils.extractParameter(xmlResponse,"<OrderStatusBusinessId>(\\d+)</OrderStatusBusinessId>");
			if("12".equals(status)){
				retVal.status = ORDER_ERROR;	
			} else if("2".equals(status) || "18".equals(status) || "6".equals(status)){
				retVal.status = ORDER_PLACED;
			} else {
				retVal.status = ORDER_COMPLETED;
			}
		}
				
		return retVal;
	}

	/**
	 * Perform a search
	 * @param xmlQuery
	 * @return
	 */
	private DaslResponse performSearchInternal(String xmlQuery){
		
		DaslResponse response = null;
		String res = "";
		
		// try to place the order
		int id = ORDER_ERROR;
		for(int i=0; i<RETRY_PLACE_ORDER; i++){
			
			// fill parameters
			xmlQuery = fillConnectionParameters(xmlQuery);
			
			ErrorRequestBean errorRequestBean = new ErrorRequestBean();
			
			try {
				Search s = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				if (s.getAdditionalInfo(RequestCount.REQUEST_COUNT_TYPE) != null)
					s.countRequest(dat.getSiteTypeInt(), (Integer) s.getAdditionalInfo(RequestCount.REQUEST_COUNT_TYPE));
			} catch (Exception e) {
				e.printStackTrace();
			} 
			
			res = daslConnection.getDataSynch(xmlQuery, errorRequestBean);
			if (Utils.isJvmArgumentTrue("debugForATSProgrammer") || logger.isEnabledFor(Level.TRACE)){
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("y_M_d_k_m_s");
				String format = simpleDateFormat.format(new Date(System.currentTimeMillis()));
				StringUtils.toFile("c:\\dump\\request"+ format + ".xml", xmlQuery);
				if(res != null) {
					StringUtils.toFile("c:\\dump\\response"+ format +".xml", res);
				}
			}
			// analyze the response
			if(res == null){
				id = ORDER_ERROR;
			} else {
				String idString = StringUtils.extractParameter(res,"<OrderBusinessId>(\\d+)</OrderBusinessId>");
				if(res.contains("<Error")){
					id = ORDER_ERROR;
				} else if("".equals(idString)){					
					id = ORDER_COMPLETED;
				} else if(!idString.matches("\\d+")){
					id = ORDER_REJECTED;
				} else {
					id  = Integer.valueOf(idString);
				}
				break;
			}
			
			// sleep and try again
			try{
				TimeUnit.SECONDS.sleep(DELAY_PLACE_ORDER);
			}catch(InterruptedException ignored){}
		}

		// check whether the order was completed
		if(id == ORDER_COMPLETED){
			response = new DaslResponse();
			response.status = ORDER_COMPLETED;
			response.xmlResponse = res;
			response.id = 0;
			logger.info("DASL Order completed!");
			return response;
		}
		
		// check whether the order was placed
		if(id == ORDER_REJECTED || id == ORDER_ERROR){
			response = new DaslResponse();
			response.status = ORDER_ERROR;
			logger.error("Error placing order. Status = " + id);
			return response;
		}
		
		// log order accepted
		logger.info("Dasl order accepted: " + id);
		
		// retrive the order
		int seqErrs = 0;
		for(int i=0; i<RETRY_GET_ORDER && continueSeach(); i++){
			response = queryOrder(id);
			// check if everything went OK
			if(response.status == ORDER_COMPLETED){
				break;
			}
			// count the errors
			if(response.status == ORDER_ERROR){				
				seqErrs++;
				if(seqErrs >= MAX_SEQ_ERRS){
					break;
				}				
			} else{
				seqErrs = 0;
			}
			// wait and try again
			try{
				TimeUnit.SECONDS.sleep(DELAY_GET_ORDER);
			}catch(InterruptedException ignored){}
		}
		
		if(response == null) {
			response = new DaslResponse();
			response.status = ORDER_ERROR;
		}
		// return the response
		return response;
	}
	
	/* (non-Javadoc)
	 * @see ro.cst.tsearch.connection.dasl.DaslSiteInterface#performSearch(java.lang.String)
	 */
	@Override
	public DaslResponse performSearch(String query, long searchId){
		
		DaslResponse daslResponse = null;

		String procQuery = null;
		
		// check the cache if it is enabled
		if(cache != null){
			
			procQuery = query.replaceAll("<DateRange><FromValue><Date>../../....</Date></FromValue><ToValue><Date>../../....</Date></ToValue></DateRange>","<DateRange/>");
			procQuery = procQuery.replaceFirst("<ClientTransactionReference>([^<]*)</ClientTransactionReference>","<ClientTransactionReference/>");
			procQuery = procQuery.replaceFirst("<ClientReference>([^<]*)</ClientReference>","<ClientReference/>");
			procQuery = procQuery.replaceFirst("<TimeStamp>([^<]*)</TimeStamp>","<TimeStamp/>");
			
			String cached = cache.getResponse(procQuery);
			if(cached != null){
				daslResponse = new DaslResponse();
				daslResponse.status = ORDER_COMPLETED;
				daslResponse.xmlResponse = cached;				
			}
		}
		
		// perform search
		if(daslResponse == null){		
			daslResponse = performSearchInternal(query);		
			// update cache
			if(cache != null){
				if(daslResponse.status == ORDER_COMPLETED){
					cache.putResponse(procQuery, daslResponse.xmlResponse, daslResponse.id);
				}
			}
		}
		if(daslResponse.xmlResponse != null){
			daslResponse.xmlResponse = daslResponse.xmlResponse.replaceAll(">(\\s|[\\n\\r])+<","><");
		}
		// return result
		return daslResponse;
	}

	/**
	 * Search for an image
	 * @param xmlQuery
	 * @return response or null if errors appeared
	 */
	@Override
	public String performImageSearch(String xmlQuery){
		xmlQuery = fillConnectionParameters(xmlQuery);
		
		if(cache != null){
			String response = cache.getResponse(xmlQuery);
			if(response != null){
				return response;
			}
		}
		
    	for(int i=0; i<RETRY_GET_IMAGE_ORDER; i++){
    		ErrorRequestBean errorRequestBean = new ErrorRequestBean();
    		try {
    			String response = daslConnection .getDataSynch(xmlQuery, errorRequestBean);
    			if(cache != null && response != null && !"".equals(response)){
    				try {
    					//Trying to check if the content is here or not
    					Node doc = XmlUtils.parseXml(response);
    					NodeList nl = xpathQuery(doc, "//Content");
    					if(nl.getLength() == 0){
    						errorRequestBean.setSearchId(searchId);
    						errorRequestBean.setErrorMessage("No //Content in the respose");
    						errorRequestBean.setRequest(xmlQuery.replaceAll(">\\s+<", "><"));
    						errorRequestBean.setResponse(response.replaceAll(">\\s+<", "><"));
    						errorRequestBean.setRequestDate(Calendar.getInstance().getTime());
    						errorRequestBean.saveToDatabase();
    					}
    				} catch (Exception e) {
						logger.error("Error while checking content for image search", e);
					}
    				
    				
    				cache.putResponse(xmlQuery, response, -1);
    			}
    			return response;
    		}catch(RuntimeException e){
    			logger.error(e);
    			errorRequestBean.setSearchId(searchId);
				errorRequestBean.setRequest(xmlQuery.replaceAll(">\\s+<", "><"));
				errorRequestBean.setThrowable(e);
				errorRequestBean.setRequestDate(Calendar.getInstance().getTime());
				errorRequestBean.saveToDatabase();
    		}
    		try{
    			TimeUnit.SECONDS.sleep(DELAY_GET_IMAGE_ORDER);
    		}catch(InterruptedException e){
    			logger.error(e);
    		}
    	}
    	return null;
	}
	
	public String getCurrentCommunityId(){
		CommunityAttributes communityAttributes = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity();
		String string = communityAttributes.getID().toString();
   		return  string;
	}

	/**
     * get current search. used to make code smaller/clearer
     */

    protected Search getSearch(){
    	return InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
    }

	@Override
	public boolean continueSeach() { 
		
        ASThread thread = ASMaster.getSearch(searchId);
        long maxTimeOut = 3600000;
        if ("3711".equals(getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY))){
        	maxTimeOut *= 5;
        }
        boolean continueSearch = true;
        if (thread != null) {
             continueSearch = thread.isStarted() && (System.currentTimeMillis() - thread.getStartTime() < maxTimeOut); // 1 ora per search
             if(continueSearch) {
            	 if (thread.getSkipCurrentSite()) {
            		 continueSearch = false;
             		logger.error("Server: " + this.getClass().toString() + " skip site!");
             		
            	 }
             }
        }
        return continueSearch;
		    
	}
	
}

