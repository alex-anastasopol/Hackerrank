package ro.cst.tsearch.connection.dasl;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.ErrorRequestBean;
import ro.cst.tsearch.database.rowmapper.RequestCount;
import ro.cst.tsearch.datatrace.Utils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author radu bacrau
 * @author cristi stochina
 */
public class DASLConnSite implements DaslConnectionSiteInterface {

	protected static final Category logger = Logger.getLogger(DaslSite.class);

	private DaslConnection  daslConnection = null ;
	
	private static final int RETRY_PLACE_ORDER = 2;  // times
	private static final int DELAY_PLACE_ORDER = 2;  // seconds	
	
	// local testing cache
	public ResponseCache cache = null;
	
	// site name. value will be overwritten
	public String siteName = getClass().getSimpleName();
	
	private int miServerID = 0;
	
	DataSite dat = null;
	
	/**
	 * constructor
	 */
	public  DASLConnSite(int miServerID ){		
 	
		this.miServerID = miServerID;
		
		dat = (DataSite)HashCountyToIndex .getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
		daslConnection = new DaslConnection(this.miServerID);
		// construct site data
		this.siteName = dat.getName();
		cache = ResponseCache.setupResponseCache(siteName);
	}	

	
	/**
	 * Perform a search
	 * @param xmlQuery
	 * @param searchId 
	 * @return
	 */
	private DaslResponse performSearchInternal(String xmlQuery, long searchId){
		 
		DaslResponse response = null;
		String res = "";
		
		// try to place the order 
		int id = ORDER_ERROR;
		for(int i=0; i<RETRY_PLACE_ORDER; i++){
			
			ErrorRequestBean errorRequestBean = new ErrorRequestBean();
			if(xmlQuery.contains("GET ORDER STATUS")){
				int orderId = -1;
				try{orderId = Integer.parseInt(StringUtils.extractParameter(xmlQuery, "[$](\\d+)[$]"));}catch(Exception e){}
				return queryOrder(orderId);
			}else{
				// place the order
				xmlQuery = xmlQuery.replaceAll("([\n\r])[\n\r \t]+", "$1");	
				res = daslConnection.getDataSynch(xmlQuery, errorRequestBean);
				try {
					Search s = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
					if (s.getAdditionalInfo(RequestCount.REQUEST_COUNT_TYPE) != null && dat != null)
						s.countRequest(dat.getSiteTypeInt(), (Integer) s.getAdditionalInfo(RequestCount.REQUEST_COUNT_TYPE));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
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
				
				errorRequestBean.setSearchId(searchId);
				errorRequestBean.setRequest(xmlQuery.replaceAll(">\\s+<", "><"));
				errorRequestBean.setRequestDate(Calendar.getInstance().getTime());
				errorRequestBean.setErrorMessage("Response is null");
				errorRequestBean.saveToDatabase();
				
			} else {
				
				
				
				
				String idString = StringUtils.extractParameter(res,"<PlantEffectiveDates>(\\d+)</PlantEffectiveDates>");
				
				if("".equals(idString)){
					if(!res.contains("<Errors>") && !res.contains("<Error>")){
						id = ORDER_COMPLETED;
						logger.info("Dasl order completed: ORDER_COMPLETED");
					} else {
						String message = StringUtils.extractParameter(res,"(?i)<Message>([^<]+)</Message>");
						errorRequestBean.setErrorMessage(message);
						logger.error("Dasl order rejected: " + message);
						id = ORDER_REJECTED;
					}
				} else if(res.contains("<Errors>") || res.contains("<Error>")){
					String message = StringUtils.extractParameter(res,"<Message>([^<]+)</Message>");
					errorRequestBean.setErrorMessage(message);
					logger.error("Dasl order rejected: " + message);
					id = ORDER_REJECTED;
				}
				else if(!idString.matches("\\d+")){
					String message = StringUtils.extractParameter(res,"<Message>([^<]+)</Message>");
					errorRequestBean.setErrorMessage(message);
					logger.error("Dasl order rejected: " + message);
					id = ORDER_REJECTED;
				} else {
					id  = Integer.valueOf(idString);
					logger.info("Dasl order accepted: " + id);
				}
				if(id == ORDER_REJECTED) {
					errorRequestBean.setSearchId(searchId);
					errorRequestBean.setRequest(xmlQuery.replaceAll(">\\s+<", "><"));
					errorRequestBean.setResponse(res.replaceAll(">\\s+<", "><"));
					errorRequestBean.setRequestDate(Calendar.getInstance().getTime());
					errorRequestBean.saveToDatabase();
				}
				
				break;
			}
			
			// sleep and try again
			try{
				TimeUnit.SECONDS.sleep(DELAY_PLACE_ORDER);
			}catch(InterruptedException ignored){}
		}
		
		// check whether the order was placed
		if(id == ORDER_REJECTED || id == ORDER_ERROR){
			response = new DaslResponse();
			if(res!=null){
				response.xmlResponse = res;
			}
			response.status = ORDER_ERROR;
			logger.error("Error placing order. Status = " + id);
			return response;
		}
		
		// check whether the order was completed
		if(id == ORDER_COMPLETED){
			response = new DaslResponse();
			
			String certificationDate = StringUtils.extractParameter(res,"(?i)<PlantEffectiveDates>[\\s]*<PropertyIndex>[\\s]*<ToValue>[\\s]*<Date>([^<]+)</Date>");
			if( StringUtils.isEmpty( certificationDate ) ){
				certificationDate = StringUtils.extractParameter(res,"(?i)<PlantEffectiveDates>[\\s]*<GeneralIndex>[\\s]*<ToValue>[\\s]*<Date>([^<]+)</Date>");
			}
			if( StringUtils.isEmpty( certificationDate ) ){
				certificationDate = StringUtils.extractParameter(res,"(?i)<PlantEffectiveDates>[\\s]*<PropertyIndex>[\\s]*<FromValue>[\\s]*<Date>[^<]*</Date>[\\s]*<Time>[^<]*</Time>[\\s]*</FromValue><ToValue>[\\s]*<Date>([^<]+)</Date>");
			}
			if( StringUtils.isEmpty( certificationDate ) ){
				certificationDate = StringUtils.extractParameter(res,"(?i)<PlantEffectiveDates>[\\s]*<GeneralIndex>[\\s]*<FromValue>[\\s]*<Date>[^<]*</Date>[\\s]*<Time>[^<]*</Time>[\\s]*</FromValue><ToValue>[\\s]*<Date>([^<]+)</Date>");
			}
			
			if( !StringUtils.isEmpty(certificationDate) ){
				response.certificationDate = certificationDate;
			}
			
			if("1/1/1".equals(response.certificationDate)){
				response.certificationDate = "";
			}
			
			/*if(!StringUtils.isEmpty(response.certificationDate)){
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DAY_OF_MONTH, -60);
				Date  date = cal.getTime();
				
				Date date1 = Calendar.getInstance().getTime();
				try{
					date1 = sdf.parse(response.certificationDate);
				}
				catch(Exception e){};
				
				if(date1.before(date)){
					response.certificationDate = "";
				}
			}*/
			
			response.status = ORDER_COMPLETED;
			response.xmlResponse = res;
			response.id = 0;
			return response;
		}		
		
		// return the response
		return response;
	}

	public DaslResponse queryOrder(int id){
		 
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
	
	@Override
	public ro.cst.tsearch.connection.dasl.DaslSite.DaslResponse performSearch(String query, long searchId) {
		DaslResponse daslResponse = null;
		// check the cache if it is enabled
		if(cache != null){
			String cached = cache.getResponse(query);
			if(cached != null){
				daslResponse = new DaslResponse();
				daslResponse.status = ORDER_COMPLETED;
				daslResponse.xmlResponse = cached;				
			}
		}
		
		// perform search
		if(daslResponse == null){		
			daslResponse = performSearchInternal(query, searchId);		
			// update cache
			if(cache != null){
				if(daslResponse.status == ORDER_COMPLETED){
					cache.putResponse(query, daslResponse.xmlResponse, daslResponse.id);
				}
			}
		}
		// return result
		return daslResponse;
	}


	//this function is unused in this implementation
	public String performImageSearch(String xmlQuery) {
		return null;
	}

	@Override
	public boolean continueSeach() {
		// TODO Auto-generated method stub
		return true;
	}
	

}

