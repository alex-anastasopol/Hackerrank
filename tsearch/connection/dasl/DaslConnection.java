package ro.cst.tsearch.connection.dasl;

import java.rmi.RemoteException;

import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.ErrorRequestBean;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;

import com.stewart.rei.webservice.ServiceStub;


public class DaslConnection {
	
	
	protected static final Logger logger = Logger.getLogger(DaslConnection.class);

	/**
	 * timeout for SOAP service - default 10 min 
	 */
	protected   int SOAP_TIMEOUT = 5 * 60 * 1000; 
	
	/**
	 * SOAP serv
	 */
	protected  ServiceStub service = null;
	
	public  DaslConnection(int miServerID) {
		try{
			DataSite dat =(DataSite)HashCountyToIndex.getDateSiteForMIServerID(HashCountyToIndex.ANY_COMMUNITY, miServerID);
			SOAP_TIMEOUT = dat .getConnectionTimeout();
			service  = new ServiceStub(dat.getLink());
			service._getServiceClient().getOptions().setTimeOutInMilliSeconds(SOAP_TIMEOUT);
			updateStub(service, 300, 200);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * place an order
	 * @param query
	 * @param errorRequestBean 
	 * @return response string or null if error occured
	 */
	public String getDataSynch(String query, ErrorRequestBean errorRequestBean){
		try{
			ServiceStub.GetDataSynch method = new ServiceStub.GetDataSynch();
			method.setRequest(query);
			ServiceStub.GetDataSynchResponse response =  service.getDataSynch(method);
			return response.getGetDataSynchResult();
		}catch(RemoteException e){
			logger.error("getDataSynch() error!", e);
			errorRequestBean.setThrowable(e);
			return null;
		}catch(RuntimeException e){
			logger.error("getDataSynch() error!", e);
			errorRequestBean.setThrowable(e);
			return null;
		}
	}
	
	private  void updateStub(org.apache.axis2.client.Stub stub, int maxTotal, int maxPerHost) { 
        MultiThreadedHttpConnectionManager httpConnectionManager = new MultiThreadedHttpConnectionManager(); 
        HttpConnectionManagerParams params = httpConnectionManager.getParams(); 
        if (params == null) { 
            params = new HttpConnectionManagerParams(); 
            httpConnectionManager.setParams(params); 
        } 
        params.setMaxTotalConnections(maxTotal); 
        params.setDefaultMaxConnectionsPerHost(maxPerHost); 
        HttpClient httpClient = new HttpClient(httpConnectionManager); 
        ServiceClient serviceClient = stub._getServiceClient(); 
        ConfigurationContext context = serviceClient.getServiceContext().getConfigurationContext(); 
        context.setProperty(HTTPConstants.CACHED_HTTP_CLIENT, httpClient); 
    } 
	
	/**
	 * query for an order
	 * @param id
	 * @return response string or null if error occured
	 */
	public String getOrder(int id){
		try{
			ServiceStub.GetOrder method = new ServiceStub.GetOrder();
			method.setOrderBusinessId(id);
			ServiceStub.GetOrderResponse response =  service.getOrder(method);
			return response.getGetOrderResult();
		}catch(RemoteException e){
			logger.error("getOrder() error!", e);
			return null;
		}catch(RuntimeException e){
			logger.error("getOrder() error!", e);
			return null;
		}		
	}
}
