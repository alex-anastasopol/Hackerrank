package ro.cst.tsearch.connection.gateway;

import java.io.File;
import java.util.Collection;

import org.apache.commons.lang.StringEscapeUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.StringUtils;
import com.stewart.orderproduction.ATS.AtsToGateway.AtsToGatewayStub;

/**
 *@author radu bacrau
 *@author cristian stochina - port class to axis 2
 */
public class EcorGatewayClient {

	private EcorGatewayClient() {}
	
	private static class Response {
		public final boolean success;
		public final String message;
		
		public Response(boolean success, String message){
			this.success = success;
			this.message = message;
		}
	}

	private static AtsToGatewayStub  stub =  null;
	static{
		try{
			stub = new AtsToGatewayStub(ServerConfig.getEcorGatewayLink());
		}catch (Exception e) {
			
		}
	}
	/**
	 * Send an ats template to gateway  
	 * @param fileName
	 * @param searchId
	 * @param agentId
	 * @return
	 */
	private static Response sendFile(String fileName, long searchId, String agentId){		
		
		String message;
		boolean success;
		
		try {
			
			String user = ServerConfig.getEcorGatewayUser();
			String password = ServerConfig.getEcorGatewayPassword();
			int timeout = ServerConfig.getEcorGatewayTimeout();			
			stub._getServiceClient().getOptions().setTimeOutInMilliSeconds(timeout * 1000);
			
			String data = new String(FileUtils.readBinaryFile(fileName));

			// attach searchId
			String firstTag = StringUtils.extractParameter(data, "(?i)<([0-9a-z]+)>");
			if(StringUtils.isEmpty(firstTag)){
				throw new RuntimeException("File does not seem to be an XML file. Could not find a tag");
			}
			data = data.replaceFirst("(?i)(<[0-9a-z]+>)", "$1<searchId>" + searchId + "</searchId>");
			data = data.replaceAll("(?i)<\\?xml.*?\\?>","");
			// add another XML layer
			data = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ATS2AIMfields><atsFile>" + 
					data + 
					"</atsFile></ATS2AIMfields>";
			
			AtsToGatewayStub.Submit method = new AtsToGatewayStub.Submit();
			method.setAgentID(agentId);
			method.setUserID(user);
			method.setPassword(password);
			method.setData(data);
			
			AtsToGatewayStub.SubmitResponse res = stub.submit(method);
			String result = res.getSubmitResult();
			if(result.contains("IsAccepted=\"True\"")){
				success = true;
				message = "Success";
			} else {
				success = false;
				message =  StringUtils.extractParameter(result, "(?i)<error[^>]*>([^<]*)</error>");
			}
			
		}catch (Throwable e) {
			success = false;
			message = e.getMessage();
		}
		
		if(!success){
			message = "Error: " + message;
		}
		
		return new Response(success, message);
			
	}
	
	/**
	 * Try to upload the ats file to ecor
	 * @param search
	 * @param fileNames
	 * @return message to be displayed in the notification email
	 */
	public static String upload(Search search, Collection<String> fileNames){
		
		
		// check that we have exactly one .ats file
		String fileName = "";
		boolean found = false;		
		for(String crtFileName: fileNames){
			if(crtFileName.endsWith(".ats")){
				if(!found){
					found = true;
					fileName = crtFileName;
				} else {
					return "<font color='red'>Error: more than one .ats files found!</font>";
				}
			}
		}
		if(!found){
			return "<font color='red'>Error: no .ats file found!</font>";
		}
		
		if(search.getSa().isUpdate()){
			return "<font color='red'>Update files are not automatically uploaded into AFW.  This file must be uploaded manually if desired.</font>";
		}
		
		// check that we have agent id
		String agentId = search.getSa().getAtribute(SearchAttributes.ECORE_AGENT_ID);
		if(StringUtils.isEmpty(agentId)){
			return "<font color='red'>Error: agent id not found for the current search!</font>";
		}
		
		Response response = sendFile(fileName, search.getID(), agentId);

		String file = "File " + StringEscapeUtils.escapeHtml(new File(fileName).getName());  
		if(response.success){
			return "<font color='green'>" + file + " was uploaded succesfully.</font>"; 
		} else {
			return "<font color='red'>" + file + " was not uploaded succesfully! " + StringEscapeUtils.escapeHtml(response.message) + "</font>";			
		}
	}
}
