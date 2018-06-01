package ro.cst.tsearch.servers.types;

import java.io.IOException;
import java.util.ArrayList;
import sun.misc.BASE64Decoder;
import java.util.List;

import org.apache.axis2.AxisFault;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.lang.StringUtils;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.name.NameFormaterI;
import com.stewart.ats.connection.nextace.NextAceConn;
import com.stewart.ats.connection.nextace.NextAceConnI;
import com.stewart.ats.connection.nextace.NextAceUtils;
import com.stewart.ats.smartupload.server.SmartUploadServer;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;

public class NextAceNA extends TSServer{

	private static final long serialVersionUID = -919814229415030758L;
	
	private final NextAceConnI conn;
	
	private final String user;
	
	private final String password;
	
	// Create an instance of HttpClient.
    final private HttpClient client = new HttpClient();
    {
    	client.setConnectionTimeout(30000);
    	client.setTimeout(30000);
    }
	 
	public NextAceNA(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) throws AxisFault {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
		
		int commId = InstanceManager.getManager().getCurrentInstance(searchId).getCommunityId();
		DataSite site = HashCountyToIndex.getDateSiteForMIServerID(commId, mid);
		
		conn = new NextAceConn(site);
		
		user = SitesPasswords.getInstance().getPasswordValue(String.valueOf(commId), "NextAce", "user");
		password = SitesPasswords.getInstance().getPasswordValue(String.valueOf(commId), "NextAce", "password");
		mSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
	}

	public NextAceNA(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
		conn = null;
		user= null;
		password = null;
		mSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
	}
	
	@Override
	protected ServerResponse SearchBy(boolean bResetQuery, TSServerInfoModule module, Object sd)throws ServerResponseException {
		return searchByInternal(bResetQuery, module, sd);
	}
	
	@Override
	protected DownloadImageResult saveImage(ImageI image)throws ServerResponseException {
		String imageLink = image.getLink(0);
		
		if(imageLink!=null){
			byte []responseBody = SmartUploadServer.downloadByteContentFromLink(imageLink, client);
		    if(responseBody !=null){ 
		    	String xml = new String(responseBody);
		    	int start = xml.indexOf("<content>");
		    	int stop = xml.indexOf("</content>");
		    	if(start>0 && stop>0 && start+9<stop){
		    		String base64Content = xml.substring(start+9,stop);
		    		BASE64Decoder decoder = new BASE64Decoder();
		    		try {
						byte[] decodedBytes = decoder.decodeBuffer(base64Content);
						if(decodedBytes.length>0){
							DownloadImageResult result = new DownloadImageResult();
							result.setContentType("application/pdf");
							result.setImageContent(decodedBytes);
							result.setStatus(DownloadImageResult.Status.OK);
							return result;
						}
					}catch (IOException e) {
						e.printStackTrace();
					}
		    	}
		    }
		}
		
		return null;
	}
	
	private ServerResponse searchByInternal(boolean bResetQuery, TSServerInfoModule module, Object sd)throws ServerResponseException {
		final int MOD_IDX = module.getModuleIdx();
		
		switch (MOD_IDX) {
			case TSServerInfo.NAME_MODULE_IDX: //this is search by order
			try {
				SearchAttributes sa = getSearchAttributes();
				final String xmlOrder = NextAceUtils.createOrder(sa, user, password);
				String response = conn.placeOrderXML(xmlOrder);
				
				String code = "";
				String mess = "";
								
				if(StringUtils.isNotBlank(response)){
					response = response.replaceAll("(?i)&lt;(/?)error", "<$1error");
					String message = ro.cst.tsearch.utils.StringUtils.extractParameter(response, "(?i)<error>([^<]+)</error>");
					
					if(StringUtils.isNotBlank(message)){
						String []messageParts = message.split(":");
						if (messageParts.length>1) {
							code = messageParts[0].trim();
							for(int i=1;i<messageParts.length;i++){
								mess += messageParts[i];
							}
							mess = mess.trim();
						}else if(messageParts.length==1){
							code = messageParts[0].trim();
						}
					}
				}
				
				if("0".equalsIgnoreCase(code)){
					System.out.println("<-><-><-> NEXTACE ORDER PLACED <-><-><->");
					SearchLogger.info("<font color=\"green\">NEXTACE ORDER PLACED APN:"+NextAceUtils.prepareApnPerCounty(sa)+" OWNER:"+sa.getOwners().getFullNames(NameFormaterI.PosType.FML, ";")+" ADDRESS:"+sa.getAddress().shortFormString()+" </font>", searchId);
					DBManager.lockSearchExternal(searchId);
				}else{
					String error = "NEXTACE ORDER NOT PLACED Code:"+code +" Message:"+mess;
					System.out.println("<-><-><-> "+error+" <-><-><->");
					SearchLogger.info("<br/><font color=\"red\">"+error+" </font>", searchId);
				}
				
				System.err.println(response); 
				
			} catch (Exception e) {
				e.printStackTrace();
				String error = "NEXTACE ORDER NOT PLACED: I/O CONNECTION ERROR (PLEASE CHECK AGENT IN SEARCH PAGE)";
				System.out.println("<-><-><-> "+error+" <-><-><->");
				SearchLogger.info("<font color=\"red\">"+error+" </font>", searchId);
			}
			break;
	
			default:
			break;
		}
		
		return new ServerResponse();
	}
	
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		modules.add(new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX)));
		serverInfo.setModulesForAutoSearch(modules);
	}

}
