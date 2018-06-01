package ro.cst.tsearch.servlet.user;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityManager;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.utils.ParameterParser;
import ro.cst.tsearch.utils.PasswordGenerator;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.user.UserI;
import com.stewart.ats.user.UserManagerI;

public class ContactUs extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
	throws IOException, ServletException {
        doRequest(request, response);
    }
    public void doPost(HttpServletRequest request, HttpServletResponse response) 
	throws IOException, ServletException {
        doRequest(request, response);
    }
    
    public void doRequest(HttpServletRequest request, HttpServletResponse response) 
    	throws IOException, ServletException {
        
        ParameterParser pp = new ParameterParser(request);
        
        int opCode = pp.getIntParameter(TSOpCode.OPCODE, -1);
        
        
        if (opCode == TSOpCode.USER_NEW_ACCOUNT) {

            //////////////////////////////////////
        	BufferedReader in = new BufferedReader(
        	        new FileReader(new File(
        	                getServletConfig().getServletContext().getRealPath("/") + File.separator +
        	                "WEB-INF" + File.separator 
        	                + "classes" + File.separator
        	                + "mailtemplate" + File.separator 
        	                + "NewUser.template")));
        	StringBuffer out = new StringBuffer("");
            String line = in.readLine();                            
            while(line != null) {
                out.append(line);
                line = in.readLine();
            }            
            in.close();
        	//////////////////////////////////////
            String body = out.toString();
            
            
            int product = pp.getIntParameter("Product", -1);
        	String queryType = "";
        	if (product == 1) {
        	    queryType = "New user";
        	} else if (product == 2) {
        	    queryType = "Lost password";
        	}
        	
        	String stateName = "";
        	try {
        	    State state = State.getState(new BigDecimal(pp.getIntParameter("P_STATE")));
        	    stateName = state.getName();
        	} catch (Exception e) {}
        	
            
            body = body.replaceAll("@@FirstName@@", pp.getStringParameter("FirstName",""));
            body = body.replaceAll("@@State@@", stateName);
            body = body.replaceAll("@@LastName@@", pp.getStringParameter("LastName",""));
            body = body.replaceAll("@@Zip@@", pp.getStringParameter("ZipCode",""));
            body = body.replaceAll("@@Title@@", pp.getStringParameter("Title",""));
            body = body.replaceAll("@@PhoneNumber@@", pp.getStringParameter("Phone",""));
            body = body.replaceAll("@@Organization@@", pp.getStringParameter("Organization",""));
            body = body.replaceAll("@@FaxNumber@@", pp.getStringParameter("Fax",""));
            body = body.replaceAll("@@StreetAddress@@", pp.getStringParameter("Address_1",""));
            body = body.replaceAll("@@E-mailAddress@@", pp.getStringParameter("Email",""));
            body = body.replaceAll("@@Address_2@@", pp.getStringParameter("Address_2",""));
            body = body.replaceAll("@@QueryType@@", queryType);
            body = body.replaceAll("@@City@@", pp.getStringParameter("City",""));
            
            int commID = pp.getIntParameter(RequestParams.USER_COMMUNITYID, -1);
            
            try {
                
    			Properties props = System.getProperties();
    			props.put("mail.smtp.host", MailConfig.getMailSmtpHost());
    			Session session = Session.getDefaultInstance(props, null);
    			MimeMessage msg = new MimeMessage(session);
    			
    			InternetAddress fromAddress = null;
    			try {
    			    fromAddress = new InternetAddress(pp.getStringParameter("Email",""));
    			} catch (Exception ex) {
    			    fromAddress = new InternetAddress(MailConfig.getMailFrom());
    			}
    			msg.setFrom(fromAddress);
    			msg.setSubject(queryType + " request.");
    			
    			InternetAddress[] toAddress = null;
    			if (commID == -1) {
    			    // nu stim comunitatea, trimitem catre cine?
    			    toAddress = InternetAddress.parse(MailConfig.getSupportEmailAddress());
    			} else {
    			    // stim din ce comunitate vine, trimitem la commadmin
    			    CommunityAttributes ca = CommunityManager.getCommunity(commID);
    			    UserAttributes commAdmin = UserManager.getUser(ca.getCOMM_ADMIN());
    			    toAddress = InternetAddress.parse(commAdmin.getEMAIL());
    			}
                msg.setRecipients(javax.mail.Message.RecipientType.TO, toAddress);
    			
    			msg.setContent(body, "text/html");
    			Transport.send(msg);
    			
    		} catch(Exception e) {e.printStackTrace();}
            
    		response.sendRedirect(URLMaping.path + URLMaping.LOGIN_PAGE
    		        + ( commID == -1 ? 
    		                "" : "?" + RequestParams.USER_COMMUNITYID + "=" + commID)
    		                );
        } else if(opCode == TSOpCode.USER_RESET_PASSWORD) {
        	String userName = pp.getStringParameter("username","");
        	String email = pp.getStringParameter("emailRecover","");
        	if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(email)) {
        		response.sendRedirect(URLMaping.path + URLMaping.USER_NEW_OR_RECOVER + "?" + RequestParams.ERROR_TYPE + "=1");
        		return;
        	}
        	UserManagerI userManagerI = com.stewart.ats.user.UserManager.getInstance();
        	try {
				userManagerI.getAccess();
				
				UserI user = userManagerI.getUser(userName);
				if(user== null ) {
					response.sendRedirect(URLMaping.path + URLMaping.USER_NEW_OR_RECOVER + "?" + RequestParams.ERROR_TYPE + "=1");
	        		return;
				}
				if(user.isHidden()) {
					response.sendRedirect(URLMaping.path + URLMaping.USER_NEW_OR_RECOVER + "?" + RequestParams.ERROR_TYPE + "=2");
	        		return;
				} else {
				
					if(user.getEmail().trim().equalsIgnoreCase(email.trim())) {
						
						String randomPassword = PasswordGenerator.getRandomPassword(DBManager.getConfigByNameAsInt("password.random.length",6));
						
						String emailBody = FileUtils.readFileToString(new File(
								getServletConfig().getServletContext().getRealPath("/") + File.separator +
	        	                "WEB-INF" + File.separator 
	        	                + "classes" + File.separator
	        	                + "mailtemplate" + File.separator 
	        	                + "resetPassword.template"));
						emailBody = emailBody.replaceAll("@@username@@",userName);
						emailBody = emailBody.replaceAll("@@password@@",randomPassword);
						EmailClient emailClient = new EmailClient();
						emailClient.setSubject("Password reset on Advantage Title Solutions");
						emailClient.addTo(email);
						emailClient.setContent(emailBody,"text/html");
						
						if(user.updatePassword(randomPassword)) {
							emailClient.sendNow();
						}
						
						
					} else {
						response.sendRedirect(URLMaping.path + URLMaping.USER_NEW_OR_RECOVER + "?" + RequestParams.ERROR_TYPE + "=1");
		        		return;
					}
				} 
				
			} catch (Throwable t) {
				t.printStackTrace();
			} finally {
				if(userManagerI != null) {
					userManagerI.releaseAccess();
				}
			}
        	
        }
        
        response.sendRedirect(URLMaping.path + URLMaping.LOGIN_PAGE);
    }
}
