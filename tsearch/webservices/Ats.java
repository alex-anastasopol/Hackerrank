package ro.cst.tsearch.webservices;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Access point for ATS web services
 * @author dumitru bacrau
 */
public class Ats {
	
	private static final Pattern AGENT_PATTERN = Pattern.compile("(?i)<agent_file_id>([^<]+)</agent_file_id>");
	private static final Pattern ABSTRACTOR_PATTERN = Pattern.compile("(?i)<community_file_id>([^<]+)</community_file_id>");
	private static final Pattern OG_GUID_PATTERN = Pattern.compile("(?i)<StewartOrdersGUID>([^<]+)</StewartOrdersGUID>");
	
	
	/**
	 * Place an order 
	 * @param appId
	 * @param userName
	 * @param password
	 * @param order
	 * @return
	 */
	public String placeOrder(String appId, String userName, String password, String order){		
		
		String logPrefix = new SimpleDateFormat("MMddyyyy_HHmmss_S").format(new Date()) + " - placeOrder - ";
		
		String fileNo = "";
		String ogFileId = null;
		boolean goodAtLeastToLog = false;
		try{
			goodAtLeastToLog = (appId!=null&&userName!=null&&password!=null&&order!=null);
			if(goodAtLeastToLog){
				fileNo = extractFilesNo(order);
				fileNo= fileNo==null?"":fileNo;
				
				Matcher matcher = OG_GUID_PATTERN.matcher(order);
				if(matcher.find()) {
					ogFileId = matcher.group(1);
				}
				
				PlaceOrderService.loggerLocal.info(logPrefix + ": Received order appId= "+appId+" userName= "+userName+" password= "+password+" fileno"+fileNo + " OG_GUID = [" + ogFileId + "]");
			}
		} catch (Exception e) {
			PlaceOrderService.loggerLocal.error(logPrefix + ": Something happend while trying to get fileNo", e);
		}
		
		
		
		PlaceOrderService placeOrder = new PlaceOrderService(appId, userName, password, order, logPrefix);		
		placeOrder.process();
		String response = placeOrder.getStatus();
		placeOrder.sendNotification();			
		placeOrder.logOrder("placeOrder", appId, userName, password, order, response);
		
		if(goodAtLeastToLog){
			PlaceOrderService.loggerLocal.info(logPrefix + ": Responded to order appId= "+appId+" userName= "+userName+" password= "+password+" fileno"+fileNo);
		}
		
		return response;
    }
	
	private static String extractFilesNo(String order) {
		Matcher matAgent 		= AGENT_PATTERN.matcher(order);
		Matcher matAbstractor	= ABSTRACTOR_PATTERN.matcher(order);
		String agentFileNo = "";
		String abstractorFileNo = "";
		if(matAgent.find()){
			agentFileNo = matAgent.group(1);
		}
		if(matAbstractor.find()){
			abstractorFileNo = matAbstractor.group(1);
		}
		return agentFileNo+" / "+abstractorFileNo;
	}

	/**
	 * Add a user
	 * @param appId
	 * @param userName
	 * @param password
	 * @param order
	 * @return
	 */
	public String addUser(String appId, String userName, String password, String order, String dateReceived){
		AddUserService addUser = new AddUserService(appId, userName, password, order, dateReceived);
		addUser.process();
		String response = addUser.getStatus();
		addUser.logOrder("addUser", appId, userName, password, order, response);
		return response;
	}
	
	/**
	 * Add a dataSource
	 * @param appId
	 * @param userName
	 * @param password
	 * @param order
	 * @return
	 */
	public String getDataSources(String appId, String userName, String password, String order, String dateReceived){
		GetDataSourcesService addDataSources = new GetDataSourcesService(appId, userName, password, order, dateReceived);
		addDataSources.process();
		
		String response = addDataSources.getResult();
		addDataSources.logOrder("addDataSources", appId, userName, password, order, response);
		
		return response;
	}
	
	/**
	 * Report an issue
	 * @param appId
	 * @param userName
	 * @param password
	 * @param order
	 * @return
	 */
	public String reportIssue(String appId, String userName, String password, String order, String dateReceived){
		ReportIssueService reportIssue = new ReportIssueService(appId, userName, password, order, dateReceived);
		reportIssue.process();
		String response = reportIssue.getStatus();
		reportIssue.logOrder("reportIssue", appId, userName, password, order, response);
		return response;
	}
}
