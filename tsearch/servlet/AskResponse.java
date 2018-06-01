/**
 * 
 */
package ro.cst.tsearch.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringEscapeUtils;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.loadBalServ.LBNotification;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.types.ASKServer;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.servers.types.TSServersFactory;


/**
 * @author Dumitru Bacrau
 *
 */
public class AskResponse extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public AskResponse() {
		// TODO Auto-generated constructor stub
	}

	public final void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		// treat the case in which request has been internally forwaded to this servlet
		String xmlData = (String)request.getAttribute("xmlData");
		if(xmlData != null){
			PrintWriter out = response.getWriter();
			try {
				int viServerID = (int)TSServersFactory.getSiteId("MI", "Washtenaw", "AK");
				String serverId = "" + viServerID;
				String key = serverId.substring(0, serverId.length() - 2);
				String p1 = HashCountyToIndex.getCountyIndex(key);
				String p2 = "" + GWTDataSite.AK_TYPE;
			    ASKServer ask = (ASKServer)TSServersFactory.GetServerInstance(viServerID, p1, p2, Search.SEARCH_NONE);				
				ask.process(xmlData);
				out.println("Processed OK!");
			} catch (Exception e){
				e.printStackTrace();
				out.println("Processing error: " + StringEscapeUtils.escapeHtml(e.toString()));
			}
			return;
		}
		
		BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
		PrintWriter out = response.getWriter();

		String contentType = request.getContentType();
		
		String folderName = (BaseServlet.FILES_PATH).replace("\\","/") + "ask/requests/";
		(new File(folderName)).mkdirs();	
		
		String[] params = new String[2];
		params[0] = "ASK Possible Response Received";
		params[1] = "Something was sent to AskResponse Servlet\nStay tooned for more information";
	
		LBNotification.sendNotification(LBNotification.MISC_MESSAGE, null, params);
		
		if("text/xml".equals(contentType)){				
					
			String fileName =  folderName + System.currentTimeMillis() + ".xml";
			StringBuilder sb = new StringBuilder();
			PrintWriter pw = new PrintWriter(fileName);
			
			try{
				response.setContentType("text/xml");
				out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
				out.println("<response>\r\n");
				out.print("<orig_request>\r\n");		
				for(String line=in.readLine(); line!= null; line=in.readLine()){
					out.println(StringEscapeUtils.escapeXml(line));
					pw.println(line);
					sb.append(line + "\r\n");
				}
				out.println("</orig_request>\r\n");
				out.println("<outcome>SUCCESS</outcome>\r\n");
				out.println("</response>\r\n");			
				
			}finally{
				pw.close();
			}
						
			try {
				params[0] = "ASK Response Received";
				params[1] = sb.toString();
			
				LBNotification.sendNotification(LBNotification.MISC_MESSAGE, null, params);
				ASKServer ask = new ASKServer("","","","",Search.SEARCH_NONE, -1);	//dummy initialization
				
				ask.process(sb.toString());
			} catch (Exception e){
				// possible exceptions raised at ask test resposonses 
				e.printStackTrace();
			}
			
		} else {
			
			String fileName =  folderName + System.currentTimeMillis() + ".txt";
			PrintWriter pw = new PrintWriter(fileName);
			
			try{
				response.setContentType("text/html");
				out.println("<font color=\"red\"><b>Expecting XML POST Data!</b></font>");
				pw.println("Content-Type:" + contentType);
				out.println("<br><b>Received Content-Type:</b>" + contentType);

				out.println("<br><b>Received POST data:</b>");	
				pw.print("PostData=");
				for(String line=in.readLine(); line!= null; line=in.readLine()){
					out.println(StringEscapeUtils.escapeHtml(line));
					pw.println(line);
				}
				out.println();
				
				out.println("<br>Bye!");
				
			}finally{
				pw.close();
			}
			
		}
	}


}
