/**
 * 
 */
package ro.cst.tsearch.webservices;

import static org.apache.commons.lang.StringEscapeUtils.escapeXml;
import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.XmlUtils;

/**
 * @author RBacrau
 *
 */
public abstract class AbstractService {

	protected static final Logger logger = Logger.getLogger(Ats.class);	

	protected String appId;
	protected String logPrefix;
	protected Document orderDoc = null;
	protected String order = null;
	protected String successMessage = "success";
	protected List<String> errors = new LinkedList<String>();
	protected List<String> warnings = new LinkedList<String>();
	
	/**
	 * Constructor
	 * @param appId
	 * @param userName
	 * @param password
	 * @param order
	 */
	public AbstractService(String appId, String userName, String password, String order, String logPrefix){
		
		// save the parameters
		this.appId = appId;
		this.logPrefix = logPrefix;
				
		// check username
		if(isEmpty(userName)){ 
			errors.add("Empty userName"); 
		}		
		// check password
		if(isEmpty(password)){ 
			errors.add("Empty password");
		}
		
		// check username/password
	
		String refPassword = ServerConfig.getString("web.services." + userName.toLowerCase() + ".password", null);
		
		if(refPassword == null || !refPassword.equalsIgnoreCase(password)){
			errors.add("Invalid WebService username/password: " + userName + "/" + password);
		}

		// check appId
		if(isEmpty(appId)){
			errors.add("Empty appId");
		}		
		// check and try to parse order
		if(isEmpty(order)){
			errors.add("Empty order");
		}
		
		this.order = order;
		
		// parse the order
		try{
			order = order.replaceAll(">[\\n\\r\\t\\s]+<", "><");
			orderDoc = XmlUtils.parseXml(order);
		}catch(RuntimeException e){
			logger.error(logPrefix + "Internal error parsing order", e);
			errors.add("Internal error parsing order: " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}		
	}

	/**
	 * Return XML representation of the order placement status
	 * @return
	 */
	public String getStatus(){
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("<ats><response>");
		if(errors.size() == 0){
			sb.append("<order_status>" + successMessage + "</order_status>");
		} else {
			sb.append("<order_status>error</order_status>");
			sb.append("<errors>");
			//Bug 3559
			try {
				//errors.add(0,"Could not create order in ATS.  Please check your original order in AIM For Windows.");
				errors.add(errors.size(),"Could not create order in ATS.  Please check your original order in AIM For Windows.");
			} catch(Exception e) {}
			for(String error: errors){
				sb.append("<error>" + escapeXml(error)+ "</error>");
			}
			
			sb.append("</errors>");
		}
		if(warnings.size() != 0){
			sb.append("<warnings>");
			for(String warning: warnings){
				sb.append("<warning>" + escapeXml(warning)+ "</warning>");
			}
			sb.append("</warnings>");			
		}
		sb.append("</response></ats>");
		return sb.toString();		
	}
	
	/**
	 * order processing
	 */
	abstract public void process();
		
	/**
	 * 
	 * @param appId
	 * @param userName
	 * @param order
	 * @return
	 */
	protected void logOrder(String message, String appId, String userName, String password, String order, String response){
		
		logger.info(logPrefix + message + ": appId=" + appId + ", userName=" + userName + ", password=" + password + "");
		
		try {
			
			StringBuffer path = new StringBuffer();
			
			path.append((ServerConfig.getFilePath()).replace("\\","/")).append("web-services/requests/").append(message);
			
			Calendar cal = Calendar.getInstance();
			
			String thisYear = FormatDate.getDateFormat(FormatDate.PATTERN_yyyy).format(cal.getTime());
			path.append("/").append(thisYear);

			String currentMonth = FormatDate.getDateFormat(FormatDate.PATTERN_MM).format(cal.getTime());
			path.append("/").append(currentMonth);
			
			String currentDay = FormatDate.getDateFormat(FormatDate.PATTERN_DD).format(cal.getTime());
			path.append("/").append(currentDay);
			
			String thisDay = FormatDate.getDateFormat(FormatDate.PATTERN_yyyy_MM_dd).format(cal.getTime());
			
			StringBuffer fileName = new StringBuffer();
			String thisTime = new SimpleDateFormat(FormatDate.DISC_FORMAT_1_TIME).format(cal.getTime());
			thisTime = thisTime.replaceAll("(?is):", "_");
			
			fileName.append(path.toString()).append("/").append(message).append("_").append(thisDay).append("_").append(thisTime).append("_").append(System.nanoTime()).append(".xml");
			
			StringBuffer data = new StringBuffer();

			data.append("service=").append(message).append("\n");
			data.append("\n");
			data.append("appId=").append(appId).append("\n");
			data.append("userName=").append(userName).append("\n");
			data.append("password=").append(password).append("\n");
			data.append("logPrefix=").append(logPrefix).append("\n");
			data.append("\n");
			data.append("order=").append(order).append("\n");
			data.append("\n");
			data.append("response=").append(response).append("\n");
			
			FileUtils.writeStringToFile(new File(fileName.toString()), data.toString());
						
		} catch(IOException e){
			e.printStackTrace();
			logger.error(logPrefix + "Problem writing order to disk", e);
		}
	}
	
}
