package ro.cst.tsearch.servlet;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.AccessControlException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;

import ro.cst.a2pdf.A2PDF;
import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.reports.invoice.InvoiceServlet;
import ro.cst.tsearch.threads.GPThread;
import ro.cst.tsearch.titledocument.TSDManager;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.gwtpages.imagecount.server.ImageCountPageServer;

public class SendEmailServlet extends BaseServlet 
{
	private static final long serialVersionUID = 1L;


	protected static final Category logger = Category.getInstance(SendEmailServlet.class.getName());
	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	private static String parAppDirectory = "title-search";
	private static String parServerName = rbc.getString("app.url");
	String SEPARATOR=",";
	String parParameters = "";
	String parSendEmailServletURL;
	String parProposeFrom;
	String parProposeTo;
	String parProposeSubject;
	String parScriptClose;
	String parSessionCookie;
	int parCurComId;
	String strHtmlText;
	//transforma pagina curenta de lucru (html) intr-un String
	private String getHtmlText(String metoda,String curenturl) {
		String strErr;
		try {      		
			String strURLTosend;
			if (metoda.toUpperCase().equals("POST"))
				strURLTosend = curenturl;
			else {
				strURLTosend =curenturl;
			}
			if(strURLTosend.contains("tsdindexpage.jsp")) {
				try {
					strURLTosend = strURLTosend.replace("newtsdi/tsdindexpage.jsp", "TSDIndexPage/viewDescription.jsp");
					parParameters = parParameters+"&view=3&viewOrder=1&showFileId=true";
				} catch(Exception e) {}
			}
			strURLTosend = strURLTosend.replace( "http://ats.cst-us.com" , rbc.getString( "app.url" ));
			URL urlToSend = new URL(strURLTosend);
			URLConnection urlConnToSend;
			urlConnToSend = urlToSend.openConnection();
			urlConnToSend.setDoInput(true);
			urlConnToSend.setAllowUserInteraction(false);
			if (metoda.toUpperCase().equals("POST")) {
				//System.err.println("getHtmlText2");
				urlConnToSend.setDoOutput(true);
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream(512);
				//System.err.println("getHtmlText3");
				PrintWriter outURL = new PrintWriter(byteStream, true);
				outURL.print(parParameters);
				outURL.flush();
				//System.err.println("getHtmlText4");
				String lengthString = String.valueOf(byteStream.size());
				urlConnToSend.setRequestProperty("Content-Length", lengthString);
				urlConnToSend.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				urlConnToSend.setRequestProperty("Cookie", parSessionCookie);
				//System.err.println("getHtmlText5");
				byteStream.writeTo(urlConnToSend.getOutputStream());
			} else {
				urlConnToSend.setDoOutput(false);
				urlConnToSend.setRequestProperty("Content-Type", "text/html");
				urlConnToSend.setRequestProperty("Cookie", parSessionCookie);
			}
			urlConnToSend.connect();
			BufferedReader in = new BufferedReader(new InputStreamReader(urlConnToSend.getInputStream()));
			String inputLine = "";
			int BUFFER_SIZE_KB = 100; // dimensiune buffer in Kb
			char cb[] = new char[BUFFER_SIZE_KB * 1024];

			String allContent = "";
			String tempContent = "";
			int nc = 0;
			int totalnc = 0;
			int maxLen = BUFFER_SIZE_KB * 1024;
			nc = in.read(cb, 0, maxLen);
			while (nc >= 0) {
				tempContent = new String(cb, 0, nc);
				allContent += tempContent;
				totalnc = totalnc + nc;
				nc = in.read(cb, 0, maxLen);
			}
			in.close();
			if(curenturl.contains("tsdindexpage.jsp")) {
				try {
					Matcher m = Pattern.compile("searchId=([0-9]*)",Pattern.CASE_INSENSITIVE).matcher(parParameters);
					m.find();
					Search search = InstanceManager.getManager().getCurrentInstance(Long.parseLong(m.group(1))).getCrtSearchContext();
					HashMap<String,String> templates = new HashMap<String,String>();
					try {
						List<CommunityTemplatesMapper> userTemplates = UserUtils.getUserTemplates(search.getAgent().getID().longValue(),-1, 0, search.getProductId() );
						for(CommunityTemplatesMapper ctm : userTemplates) {
							templates.put(String.valueOf(ctm.getId()), ctm.getPath());						
						}
					}catch(Exception e) { }
					String tsrIndex = GPThread.createTsrIndexHtmlContents(false, search, "", templates, new ArrayList<String>(), null);
					allContent = allContent.replaceFirst("(?i)<iframe.*?</iframe>", tsrIndex );		
				} catch(Exception e) {
					e.printStackTrace();
				}
			} else if (curenturl.contains("reports_image_count.jsp")) {
				
				Matcher m = Pattern.compile("searchId=([0-9]*)",Pattern.CASE_INSENSITIVE).matcher(parParameters);
				m.find();
				long searchId = Long.parseLong(m.group(1));
				Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				Date startDate = (Date)search.getAdditionalInfo(ImageCountPageServer.START_DATE_KEY);
				Date endDate = (Date)search.getAdditionalInfo(ImageCountPageServer.END_DATE_KEY);
				@SuppressWarnings("unchecked")
				List<String> selectedDS = (List<String>)search.getAdditionalInfo(ImageCountPageServer.SELECTED_DS_KEY);
				@SuppressWarnings("unchecked")
				List<String> selectedCommunities = (List<String>)search.getAdditionalInfo(ImageCountPageServer.SELECTED_COMMUNITIES_KEY);
				
				DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
				String dates = "<b>From:</b> " + df.format(startDate) + "&nbsp;" +
					           "<b>To:</b> " + df.format(endDate);
				allContent = allContent.replaceFirst("(?is)(<div\\s+[^<>]*id=\"slotCommandUp\"[^<>]*>)\\s*(</div>)", "$1" + dates + "$2");
					
				ImageCountPageServer imageCountPageServer = new ImageCountPageServer();
				String table = imageCountPageServer.getHTMLtable(searchId, startDate, endDate, selectedDS, selectedCommunities);
									
				allContent = allContent.replaceFirst("(?is)(<div\\s+id=\"mainReportTable\">)\\s*(</div>)\\s*</td>",
						"$1" + table + "$2</td>");
			}
			return allContent;
		} catch (MalformedURLException e) {
			strErr = "Could not open URL !";
			return "";
		} catch (IOException e) {
			strErr = "Could not connect !";
			return "";
		} catch (AccessControlException e) {
			strErr = "Can only connect to the server !";
			return "";  
		}
	}

	private String formatHtmlBody(String strHtml) { 
		String allBody = "";		
		String appDirSimple = "\"/" + parAppDirectory;
		String appDirFull = "\"" + parServerName + "/" + parAppDirectory;

		appDirSimple = "/" + parAppDirectory;
		appDirFull = parServerName + "/" + parAppDirectory;		
		strHtml = replaceLinks(strHtml,appDirSimple, appDirFull);

		allBody += strHtml;
		return allBody; 
	}



	private String replaceLinks(String strHtml, String searchTxt, String replacement) {
		if (searchTxt.length() == 0)
			return strHtml;
		if (strHtml.length() == 0)
			return strHtml;
		String retStr = strHtml.replaceAll(searchTxt, replacement);
		return retStr;
	}

	public void doPost(HttpServletRequest request,
			HttpServletResponse response)
	throws IOException, ServletException
	{
		doRequest(request, response);
	}

	/*doRequest - este functia care transmite email-uri campurile de mail sunt luate din 
	 * emaill.jsp 
	 * mailul poate fi transmis prin atasare de pdf sau de pagina curenta
	 *  String -ul txtBody este o variabila care memoreaza pagina curenta pentru
	 *   transmiterea pagini de lucru  prin mail
	 *   transmiterea se realizeaza printr-un tread separat
	 * */	

	@Override
	synchronized public void doRequest(HttpServletRequest request,
			HttpServletResponse response)
	throws IOException, ServletException
	{
		PrintWriter out = response.getWriter();
		request.getParameterNames();
		String txtTo = request.getParameter("txtTo") ;
		String txtSubject = request.getParameter("txtSubject") ;
		String txtMessageText = request.getParameter("txtMessageText") ;
		String txtFrom=request.getParameter("txtFrom") ;
		String txtCc=request.getParameter("txtCc") ;
		String txtBcc=request.getParameter("txtBcc") ;
		String sursa=request.getParameter("sursa") ;
		String assignTo =request.getParameter("assignTo") ;
		String searchh=request.getParameter("searchId") ;
		String txtFile ="";
		String sPSfile ="";
		if (StringUtils.isEmpty(txtFrom)) {
			txtFrom = MailConfig.getMailFrom();
		}
		String metod = "POST";
		String  cururl= request.getParameter("CurPageURL") ;
		parParameters = request.getParameter("parameters");
		parSessionCookie=request.getParameter("sessionCookie");
		String fileSubject=cururl;
		HttpSession session=request.getSession();
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		Search global= (Search) currentUser.getSearch( request);
		String action = "";
		try {
			action = request.getParameter(RequestParams.ACTION);
		}catch(Exception ignored) {}
		
		if(action.equals(InvoiceServlet.ACTION_MULTIPLE_AGENT_INVOICES)) {
			String searchId = "-1";
			int agent = -1;
			String[] agentIds = new String[0];
			String invoice = "";
			String[] formats = new String[0];
			String subject = "";
			String to="";
			String from ="";
			String text="";
			int skip = 0;			
			
			try {	
				searchId = request.getParameter(RequestParams.SEARCH_ID);
				agent = Integer.parseInt(request.getParameter("agent"));
				agentIds = request.getParameter("agentIds").split(",");
				invoice = request.getParameter("invoice");
				formats = request.getParameter("formats").split(",");
				subject = request.getParameter("subject");
				from = request.getParameter("from");
				skip = Integer.parseInt(request.getParameter("skip"));
				
				if(skip==1) {
					agent++;
					//this does not return, actually it jumps to the finally {...} block
					return; 
				}
				
				Long currentAgentId = Long.parseLong(agentIds[agent++]);
				UserAttributes uaCurrentAgent = UserUtils.getUserFromId(currentAgentId);
				
				try {
					Long nextAgentId = Long.parseLong(agentIds[agent]);
					UserAttributes uaNextAgent = UserUtils.getUserFromId(nextAgentId);
					to = uaNextAgent.getEMAIL(); 
					subject = "Invoice for " + uaNextAgent.getUserFullName();
					
			    	for(String format : formats) {
			    		String file = invoice + "_" + uaNextAgent.getID() + format;
			    		String shortName = file.substring(file.lastIndexOf(File.separator)+ File.separator.length());
	       				text+= shortName + " ";
			    	}
				}catch(Exception ignored) {}
				
				EmailClient email=new EmailClient();
				
		    	for(String format : formats) {
		    		String file = invoice + "_" + uaCurrentAgent.getID() + format;
		    		String shortName = file.substring(file.lastIndexOf(File.separator)+ File.separator.length());
       				email.addAttachment(file,shortName,shortName);
		    	}
		    			    	
		    	email.setFrom(txtFrom);
				email.addTo(txtTo);
				email.setSubject(txtSubject);
				email.addContent(txtMessageText);

				email.sendAsynchronous();
				logger.info(" Send Mail  started ");
				return;
			}catch(Exception e) {
			}
			finally {
				//if we have more agents to send them invoices
				if(agent!=agentIds.length)
					forward(request, response, 
							URLMaping.MULTIPLE_EMAIL
								+ "?searchId=" + URLEncoder.encode(searchId, "UTF-8")
								+ "&agentIds=" + URLEncoder.encode((new Vector<String>(Arrays.asList(agentIds))).toString().replaceAll("[\\[\\]\\s]", ""), "UTF-8")
								+ "&invoice=" + URLEncoder.encode(invoice, "UTF-8")
								+ "&formats=" + URLEncoder.encode((new Vector<String>(Arrays.asList(formats))).toString().replaceAll("[\\[\\]\\s]", ""), "UTF-8")
								+ "&subject=" + URLEncoder.encode(subject, "UTF-8")
								+ "&from=" + URLEncoder.encode(from, "UTF-8")
								+ "&to=" + URLEncoder.encode(to, "UTF-8")
								+ "&text=" +URLEncoder.encode(text, "UTF-8")
								+ "&agent="+agent
								+ "&" + RequestParams.ACTION + "="
								+ InvoiceServlet.ACTION_MULTIPLE_AGENT_INVOICES
								);
				else 
					forward(request, response, URLMaping.MULTIPLE_EMAIL
							+ "?" + RequestParams.ACTION + "=" 
							+ InvoiceServlet.ACTION_MULTIPLE_AGENT_INVOICES_CLOSE
							);
			}
			return;
		}
		
		if(assignTo!=null && assignTo.equals("1")) {
			
			boolean success = false;
			
			try { 
				EmailClient email=new EmailClient();
			
	        	UserAttributes commAdmin = InstanceManager.getManager().getCurrentInstance(Long.parseLong(searchh)).getCurrentUser();
				String fromAddress = "";
				try {
				    fromAddress = commAdmin.getEMAIL();
				} catch (Exception ex) {
				    fromAddress = MailConfig.getMailFrom();
				}
				
		    	String searchIds = request.getParameter("assignSearchIds");
		    	String[] ids = searchIds.split(",");
		    	for(String id : ids) {
		    		byte orderFileAsByteArray[] = DBManager.getSearchOrderLogs(Long.parseLong(id), FileServlet.VIEW_ORDER, false);
        			if(orderFileAsByteArray!=null) {
        				String orderFile = new String(orderFileAsByteArray);
        				email.addAttachmentContaints(id+".html",orderFile);
        			}
		    	}
		    			    	
		    	
		    	txtFrom = MailConfig.getMailFrom();
				email.setFrom(txtFrom);
				email.addTo(txtTo);
				email.setSubject(txtSubject);
				email.addContent(txtMessageText);

				email.sendAsynchronous();
				logger.info(" Send Mail  started ");
		    	success = true;
			}catch(Exception e) {
				e.printStackTrace();				
			}
			
			out.println("<HTML><BODY>");
			if(success)
				out.println("<script language='javascript'> window.close(); </script>");
			else
				PrintError(out,"Could not send e-mail to agents/abstractors");
			out.println("</BODY></HTML>");
			return;
		}
		

		if(sursa.compareTo("1")==0)	{
			fileSubject=fileSubject.substring(fileSubject.lastIndexOf(File.separator)+File.separator.length());

			if(fileSubject.length()>4){
				fileSubject=fileSubject.substring(0,fileSubject.length()-4);
			}
			fileSubject=fileSubject+searchh+".HTML";
			fileSubject="/"+fileSubject;
			String sir=fileSubject.replaceAll("\b", "_");
			fileSubject=sir;
		}


		try{
			String displayFileName = request.getParameter("pdfFile");
			String[] files = displayFileName.split(",");

			if(files.length>1){
				txtSubject = txtSubject.substring(0, txtSubject.length()-2);
			}
			
			/*Send E-mail section
			 * */
			EmailClient email=new EmailClient();
			email.setFrom(txtFrom);
			email.addTo(txtTo);
			email.addBcc(txtBcc);
			email.addCc(txtCc);
			email.setSubject(txtSubject);
			email.addContent(txtMessageText);
			if((parParameters.contains("&f="))||(parParameters.contains("?f="))){
				email.addAttachment(BaseServlet.FILES_PATH+parParameters.replaceAll("(?is).*?[&?]+f=([^&]*).*","$1"));
			}
			else{
				if(!"".equalsIgnoreCase(cururl)){
					String tmpConts=formatHtmlBody(getHtmlText(metod, cururl));
					String attName = txtSubject+".html";
					attName = attName.replaceAll("'", "");
					attName = attName.replace("/", "");
					email.addAttachmentContaints(attName, tmpConts);
				}
			}
			email.addAttachment(txtFile);
			email.addAttachment(sPSfile);
			email.addAttachment(displayFileName);
			email.sendAsynchronous();
			logger.info(" Send Mail  started ");

			/*Send E-mail section end
			 * */
			
			String REAL_PATH = getServletConfig().getServletContext().getRealPath("/");

			try {
				out.println("<html><head><LINK media=screen href=\""+URLMaping.STYLESHEETS_DIR+"/default.css\" type=\"text/css\" rel=stylesheet>\n</head><body bgcolor=\"#FFFFFB\" leftmargin=\"0\" topmargin=\"0\" marginwidth=\"0\" marginheight=\"0\"><b>");
				BufferedReader in = new BufferedReader(new FileReader(REAL_PATH+URLMaping.EMAIL_OK_PAGE));
				String str;		        
				while ((str = in.readLine()) != null) {

					if(str.contains("%%txtTo%%")){
						str = str.replace("%%txtTo%%", txtTo);
					}

					if(str.contains("%%txtCc%%")){
						str = str.replace("%%txtCc%%", txtCc);
					}

					if(str.contains("%%txtBcc%%")){
						str = str.replace("%%txtBcc%%", txtBcc);
					}

					if(str.contains("%%txtFrom%%")){
						str = str.replace("%%txtFrom%%", txtFrom);
					}
					if(str.contains("%%txtSubject%%")){
						str = str.replace("%%txtSubject%%", txtSubject);
					}		        	
					if(str.contains("%%txtMessageText%%")) {
						str = str.replace("%%txtMessageText%%", txtMessageText);
					}		            
					out.println(str);
				}
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}		
		}

		catch (Exception e)
		{
			e.printStackTrace();
			PrintError (out,e.getMessage());
		}
		out.println("</body></html>");



	}

	public void PrintError(PrintWriter out, String msg)
	{
		out.println("<br>The following error ocurred:");
		out.println("</b><hr>");
		out.println(msg);
		out.println("<br><br><hr><a href=\"javascript:history.go(-1);\">Back</a>");
	}

}