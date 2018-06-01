package ro.cst.tsearch.webservices;

import static ro.cst.tsearch.utils.XmlUtils.getChildren;
import static ro.cst.tsearch.utils.XmlUtils.getNodeValue;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.utils.StringUtils;

public class ReportIssueService extends AbstractService {

	/**
	 * Constructor
	 * @param appId
	 * @param userName
	 * @param password
	 * @param order
	 */
	public ReportIssueService(String appId, String userName, String password, String order, String dateReceived){
		super(appId, userName, password, order, dateReceived);		
	}
	
	public class ErrorMessage{
		
		public List<String> errorList = new LinkedList<String>();
		public List<String> warningList = new LinkedList<String>();
		public long searchId = -1;
		
		/**
		 * Parse the error message
		 * @param orderDoc
		 */
		public ErrorMessage(Document orderDoc){
			
			// isolate the order
			Node order = null;
			for(Node child: getChildren(orderDoc)){
				if("ats".equalsIgnoreCase(child.getNodeName())){
					for(Node grand: getChildren(child)){
						if("reportIssue".equalsIgnoreCase(grand.getNodeName())){
							order = grand;
							break;
						} else {
							warnings.add("Node ignored: " + grand.getNodeName());
						}
					}
				} else {
					warnings.add("Node ignored: " + child.getNodeName());
				}
			}			
			if(order == null){
				errors.add("Could not find <ats><reportError>");
				return;
			}
			
			// process the order		
			for(Node child: getChildren(order)){
				String childName = child.getNodeName();
				if("searchId".equalsIgnoreCase(childName)){
					String sid = getNodeValue(child);
					if(sid.matches("\\d+")){
						searchId = Long.parseLong(sid);
					} else {
						errors.add("Search id must be an number: " + sid);
					}  
				} else if("errors".equalsIgnoreCase(childName)){
					for(Node grand: getChildren(child)){
						String grandName = grand.getNodeName();
						if("error".equalsIgnoreCase(grandName)){
							String err = getNodeValue(grand);
							if(!StringUtils.isEmpty(err)){
								errorList.add(err);
							}
						} else {
							warnings.add("Node ignored: " + grandName);
						}
					}
				} else if("warnings".equalsIgnoreCase(childName)){
					for(Node grand: getChildren(child)){
						String grandName = grand.getNodeName();
						if("warning".equalsIgnoreCase(grandName)){
							String warn = getNodeValue(grand);
							if(!StringUtils.isEmpty(warn)){
								warningList.add(warn);
							}
						} else {
							warnings.add("Node ignored: " + grandName);
						}
					}
				} else {
					warnings.add("Node ignored: " + childName);
				}
			}
			
			// verify that we did get the searchId
			if(searchId == -1){
				errors.add("Search id not provided");
			}
		}

		/**
		 * Get list of emails for a agent or abstractor
		 * @param field either "agent_id" or "abstract_id"
		 * @param searchId
		 * @return
		 */
		private Set<String> getEmails(String field, long searchId){
			SimpleJdbcTemplate template = DBManager.getSimpleTemplate();
			long userId = template.queryForLong("SELECT " + field + " FROM ts_search WHERE id = ?", searchId);
			String email = template.queryForObject("SELECT email FROM ts_user WHERE user_id = ?", String.class, userId);			
			Set<String> emails = new LinkedHashSet<String>();
			if(email != null){
				for(String em: email.split(",")){
					if(em.length() != 0 && !"N/A".equalsIgnoreCase(em)){
						emails.add(em);
					}
				}
			}
			return emails;
		}
		
		/**
		 * 
		 * @return
		 */
		private String getEmailBody(String agentFileNo, String abstrFileNo){
			
			StringBuilder sb = new StringBuilder();
			sb.append("<font size=\"2\" face=\"Arial\">");
			sb.append("Application <b>" + appId + "</b> reported an issue with the following search:<br/>");
			sb.append("<li>internal search id: <b>" + searchId + "</b></li>");
			sb.append("<li>agent file id: <b>" + agentFileNo + "</b></li>");
			sb.append("<li>abstractor file id: <b>" + abstrFileNo + "</b></li>");
			sb.append("<br/><br/>");
	        // add errors
	        if(errorList.size() != 0){
	        	sb.append("The following <b>Errors</b> were reported:<br/>");
	        	for(String error: errorList){
	        		sb.append("<li>" + error + "</li>");
	        	}
	        	sb.append("<br/><br/>");	        	
	        }
	        
	        // add warnings
	        if(warningList.size() != 0){
	        	sb.append("The following <b>Warnings</b> were reported:<br/>");
	        	for(String warning: warningList){
	        		sb.append("<li>" + warning + "</li>");
	        	}
	        	sb.append("<br/>");
	        }
	        sb.append("</font>");
			return sb.toString();
		}
		
		/**
		 * Notify the agent, abstractor and support
		 */
		public void notifyParties(){
			
			String from = MailConfig.getMailFrom();
			Set<String> ccs = new HashSet<String>();
			try {
				ccs = getEmails("abstract_id", searchId);
			} catch (Exception e){
				warnings.add("Could not obtain abstractor email");
			}
			Set<String> tos = new HashSet<String>();
			try {
				tos = getEmails("agent_id", searchId);
			} catch(Exception e){
				if(ccs.size() == 0){
					errors.add("Could not obtain either agent or abstractor emails");
					return;
				} else {
					tos = ccs;
					ccs = new HashSet<String>();
					warnings.add("Could not obtain agent email");
				}				
				return;
			}			
			
			for(String email: MailConfig.getSupportEmailAddress().split(",")){
				ccs.add(email);
			}
				
			EmailClient email = new EmailClient();
			email.setFrom(from);
			Set<String> usedEmails = new HashSet<String>();
			for(String to: tos){
				if(!usedEmails.contains(to)){
					email.addTo(to);
					usedEmails.add(to);
				}
			}
			for(String cc: ccs){
				if(!usedEmails.contains(cc)){
					email.addCc(cc);
					usedEmails.add(cc);
				}
			}
			String agentFileNo = DBManager.getSimpleTemplate().queryForObject("SELECT agent_fileno FROM ts_search WHERE id = ?", String.class, searchId);
			agentFileNo = agentFileNo.replaceAll("[\"']", "");
			if(StringUtils.isEmpty(agentFileNo)){
				agentFileNo = "empty";
			}
			String abstrFileNo = DBManager.getSimpleTemplate().queryForObject("SELECT abstr_fileno FROM ts_search WHERE id = ?", String.class, searchId);
			if(StringUtils.isEmpty(abstrFileNo)){
				abstrFileNo = "empty";
			}
			String subject = "Issue related to search " + agentFileNo + " / " + abstrFileNo;
			email.setSubject(subject);			
			email.setContent(getEmailBody(agentFileNo, abstrFileNo), "text/html");
			email.sendNow();			
		}
		
	}
	
	@Override
	public void process() {

		// check for errors from constructor
		if(errors.size() > 0){
			return;
		}
		
		try{
			ErrorMessage error = new ErrorMessage(orderDoc);

			// check for errors 
			if(errors.size() > 0){
				return;
			}

			// check if the search exists
			boolean searchExists = DBManager.getSimpleTemplate().queryForInt("SELECT count(*) FROM ts_search WHERE id=?", error.searchId) == 1;
			if(!searchExists){
				errors.add("Search with id=" + error.searchId + " was nout found");
				return;
			}
			
			// perform the notification
			error.notifyParties();
			
		} catch(Exception e){
			e.printStackTrace();
			errors.add("internal error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

}
