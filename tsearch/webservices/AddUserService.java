package ro.cst.tsearch.webservices;

import static ro.cst.tsearch.utils.XmlUtils.getChildren;
import static ro.cst.tsearch.utils.XmlUtils.getNodeValue;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.stewart.ats.tsrindex.client.template.TemplatesInitUtils;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.data.DataException;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.utils.StringUtils;

/**
 * Add user service
 * @author radu bacrau
 */
public class AddUserService extends AbstractService {
	
	/**
	 * Constructor
	 * @param appId
	 * @param userName
	 * @param password
	 * @param order
	 */
	public AddUserService(String appId, String userName, String password, String order, String dateReceived){
		super(appId, userName, password, order, dateReceived);		
	}

	private class NewUser { 

		public String companyName = null;
		public String companyCity = null;
		public String workPhone = null;
		public String email = null;
		
		public String companyAddress = null;
		public String companyState = null;
		public String companyZip = null;
		public String accountingId = null;
		
		public String login = null;
		public String firstName = null;
		public String lastName = null;
		public String password = null;
		
		/**
		 * Check a field  
		 * @param name
		 * @param value
		 * @param minLen
		 * @param maxLen
		 * @param regex
		 */
		private void checkField(String name, String value, int minLen, int maxLen, String regex){
			if(value == null){
				errors.add(name + " is required");
				return;
			}
			if(value.length() < minLen || value.length() > maxLen){
				errors.add(name + " must have between " + minLen + " and " + maxLen + " chars");
				return;
			}
			if(regex != null && !value.matches(regex)){
				errors.add(name + " does not match regex " + regex);
				return;
			}
		}
		
		/**
		 * Check the parsed fields
		 * @return
		 */
		public boolean check() {
			
			if(StringUtils.isEmpty(companyAddress)){
				companyAddress = "N/A";
			}
			if(StringUtils.isEmpty(companyState)){
				companyState = "N/A";
			}
			if(StringUtils.isEmpty(companyZip)){
				companyZip = "N/A";
			}
			if(StringUtils.isEmpty(accountingId)){
				accountingId = "N/A";
			}
			
			int initialErrorCount = errors.size();

			checkField("Company Name", companyName, 1, 300, null);
			checkField("Company City", companyCity, 1, 30, null);
			checkField("Work Phone", workPhone, 1, 20, "[-0-9 \\)\\(+/]+");
			checkField("Email", email, 1, 200, null);
			checkField("Login", login, 1, 20, "[\\w\\.@]+");
			checkField("First Name", firstName, 1, 50, null);
			checkField("Last Name", lastName, 1, 50, null);
			checkField("Password", password, 6, 20, "[A-Za-z0-9!@#$%^&*]+");
			checkField("Address", companyAddress, 0, 50, ".*");
			checkField("State", companyState, 0, 30, ".*");
			checkField("Zip", companyZip, 0, 20, ".*");
			checkField("Accounting Id", accountingId, 0, 50, ".*");
			
			return errors.size() != initialErrorCount;
		}
		
		/**
		 * Parse a new user from an XML document
		 * @param orderDoc
		 */
		public NewUser(Document orderDoc){
			
			// isolate the order
			Node order = null;
			for(Node child: getChildren(orderDoc)){
				if("ats".equalsIgnoreCase(child.getNodeName())){
					for(Node grand: getChildren(child)){
						if("addUser".equalsIgnoreCase(grand.getNodeName())){
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
				errors.add("Could not find <ats><addUser>");
				return;
			}
			
			// process the order		
			for(Node child: getChildren(order)){
				String childName = child.getNodeName();
				if("company".equalsIgnoreCase(childName)){
					for(Node grand: getChildren(child)){
						String grandName = grand.getNodeName();
						if("name".equalsIgnoreCase(grandName)){
							companyName = getNodeValue(grand);
						} else if("city".equalsIgnoreCase(grandName)){
							companyCity = getNodeValue(grand);
						} else if("address".equalsIgnoreCase(grandName)){
							companyAddress = getNodeValue(grand);
						} else if("state".equalsIgnoreCase(grandName)){
							companyState = getNodeValue(grand);
					    } else if("zip".equalsIgnoreCase(grandName)){
					    	companyZip = getNodeValue(grand);
					    } else if("accountingid".equalsIgnoreCase(grandName)){
					    	accountingId = getNodeValue(grand);
					    } else {
							warnings.add("Node ignored: " + grandName);
						}
					}
				} else if("contact".equalsIgnoreCase(childName)){
					for(Node grand: getChildren(child)){
						String grandName = grand.getNodeName();
						if("work_phone".equalsIgnoreCase(grandName)){
							workPhone = getNodeValue(grand);
						} else if("email".equalsIgnoreCase(grandName)){
							email = getNodeValue(grand);
						} else {
							warnings.add("Node ignored: " + grandName);
						}
					}
				} else if("personal".equalsIgnoreCase(childName)){
					for(Node grand: getChildren(child)){
						String grandName = grand.getNodeName();
						if("login".equalsIgnoreCase(grandName)){
							login = getNodeValue(grand);
						} else if("password".equalsIgnoreCase(grandName)){
							password = getNodeValue(grand);
						} else if("first_name".equalsIgnoreCase(grandName)){
							firstName = getNodeValue(grand);
						} else if("last_name".equalsIgnoreCase(grandName)){
							lastName = getNodeValue(grand);
						}else {
							warnings.add("Node ignored: " + grandName);
						}
					}					
				} else {
					warnings.add("Node ignored: " + childName);
				}
			}
		}

	}
		
 
	@Override
	public void process(){
		
		// check for errors from constructor
		if(errors.size() > 0){
			return;
		}
		
		try{
			// parse user info from order document
			NewUser nu = new NewUser(orderDoc);
			
			// check for other errors
			if(errors.size() == 0){
				nu.check();
			}
			
			// check if errors appeared
			if(errors.size() > 0){
				return;
			}
			
			// create the user
			createUser(nu);
			
		}catch(Exception e){
			e.printStackTrace();
			errors.add("internal error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
	
	/**
	 * Obtain community id for the new user
	 * @return
	 */
	private long getCommId(){
		String sql = "SELECT comm_name, comm_id FROM ts_community";
		ParameterizedRowMapper<Long> rm = new ParameterizedRowMapper<Long>() {		    
	        public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
	        	String name = rs.getString("comm_name");
	        	Long id = rs.getLong("comm_id");
	        	if(name.toLowerCase().contains(ServerConfig.getString("web.services.community").toLowerCase())){
	        		return id;
	        	} else {
	        		return -1L;
	        	}
	        }
	    };		
	    long retVal = -1;
	    for(long id : DBManager.getSimpleTemplate().query(sql, rm)){
	    	if(id == -1){
	    		continue;
	    	}
	    	if(retVal == -1){
	    		retVal = id;
	    	} else {
	    		errors.add("Could not uniquely identify the community");
	    	}
	    }
		return retVal;
	}
	
	/**
	 * Obtain the list of templates for new user
	 * @param commId
	 * @return tsd and titledesk template ids, comma separated
	 */
	private String getTemplates(long commId){
				
		String sql = "SELECT template_id, path FROM ts_community_templates WHERE comm_id = ? ORDER BY template_id";
		ParameterizedRowMapper<String[]> rm = new ParameterizedRowMapper<String[]>() {		    
	        public String[] mapRow(ResultSet rs, int rowNum) throws SQLException {
	        	return new String[]{"" + rs.getLong("template_id"), rs.getString("path")};
	        }
	    };		
	    Set<String> templates = new HashSet<String>();
		for(String[] template: DBManager.getSimpleTemplate().query(sql, rm, commId)){
			String id = template[0];
			String fileName = template[1];
			String fileNameLowercase = fileName.toLowerCase();
			
			if(fileName.startsWith(TemplatesInitUtils.TEMPLATE_TSD_START) && 
			    (fileNameLowercase.endsWith(".html") || fileNameLowercase.endsWith(".htm"))){
				templates.add(id);
			} else if(fileNameLowercase.contains("titledesk") && fileNameLowercase.endsWith(".ats")){
				templates.add(id);
			} else if(fileName.contains(TemplatesInitUtils.TEMPLATE_BP_CONTAINS) 
					|| fileName.contains(TemplatesInitUtils.TEMPLATE_CB_CONTAINS)){
				templates.add(id);
			}
		}
		String retVal = "";
		for(String template: templates){
			retVal += (" " + template);
		}
		return retVal.trim().replace(" ", ",");
	}
	
	/**
	 * Create user
	 * @param nu
	 */
	private void createUser(NewUser nu){

		// obtain the community id
		long commId = -1;
		try {
			commId = getCommId();
		}catch(RuntimeException re){
			errors.add("Could not identify the community");
		}
		if(errors.size() > 0){
			return;
		}
		
		// obtain the templates
		String templates = "";
		try {
			templates = getTemplates(commId);
		} catch(RuntimeException re){
			warnings.add("Could not autoselect templates");			
		}
					
		// create and fill UserAttributes
		UserAttributes ua = new UserAttributes();
		ua.setID(new BigDecimal(-1)); 
		ua.setLOGIN(nu.login);       
		ua.setPASSWD(nu.password);   
		ua.setLASTNAME(nu.lastName);  
		ua.setFIRSTNAME(nu.firstName);  
		ua.setMIDDLENAME("");       
		ua.setCOMPANY(nu.companyName);  
		ua.setEMAIL(nu.email);      
		ua.setALTEMAIL("N/A");      
		ua.setPHONE(nu.workPhone);  
		ua.setALTPHONE("N/A");      
		ua.setICQ("N/A");        
		ua.setAOL("N/A");        
		ua.setYAHOO("N/A");      
		ua.setWADDRESS(nu.companyAddress);     
		ua.setWCITY(nu.companyCity);   
		ua.setWSTATE(nu.companyState);        
		ua.setWZCODE(nu.companyZip);       
		ua.setWCONTRY("N/A");      
		ua.setWCOMPANY("-6");  // GMT -6
		ua.setEDITEDBY(null);      
		ua.setGROUP(new BigDecimal(64));  // AGENT       
		ua.setLASTLOGIN(new BigDecimal(System.currentTimeMillis()));   
		ua.setDELETED(new BigDecimal(0));  
		ua.setUMESSAGES(null);       
		ua.setLASTCOMM(null);        
		ua.setPCARD_ID("N/A"); 
		ua.setWCARD_ID(nu.accountingId); 
		ua.setDATEOFBIRTH(-631152000000L);   
		ua.setPLACE("N/A");       
		ua.setPADDRESS(null);     
		ua.setPLOCATION("N/A");   
		ua.setHPHONE("N/A");      
		ua.setMPHONE("N/A");      
		ua.setPAGER("N/A");       
		ua.setINSTANT_MESSENGER("Yahoo");  
		ua.setMESSENGER_NUMBER("N/A");      
		ua.setHCITY("N/A");        
		ua.setHSTATE("N/A");       
		ua.setHZIPCODE("N/A");     
		ua.setHCOUNTRY("N/A");     
		ua.setCOMMID(new BigDecimal(commId));      
		ua.setCOMPANYID(new BigDecimal(1));   
		ua.setAGENTID(null);       
		ua.setSTATE_ID("0");       				
		ua.setSTREETNO("N/A");      
		ua.setSTREETDIRECTION("N/A"); 
		ua.setSTREETNAME("N/A");      
		ua.setSTREETSUFFIX("N/A");    
		ua.setSTREETUNIT("N/A");      
		ua.setDISTRIBUTION_TYPE("0");  
		ua.setDISTRIBUTION_MODE("1");  
		ua.setADDRESS("N/A");       
		ua.setC2ARATEINDEX(new BigDecimal(1)); 
		ua.setATS2CRATEINDEX(new BigDecimal(1)); 
		ua.setRATINGFROMDATE(new Date(new Date().getTime() - 24 * 60 * 60 * 1000));  // yesterday 
		ua.setTEMPLATES(templates);  
		ua.setASSIGN_MODE("0");
		ua.setSINGLE_SEAT("0");
		ua.setPROFILE_READ_ONLY(0);  
		ua.setINTERACTIVE(false);
		ua.setAUTO_ASSIGN_SEARCH_LOCKED(true);
		ua.setOUTSOURCE(UserAttributes.OS_DISABLED);
		ua.setAUTO_UPDATE("0");
		ua.setOTHER_FILES_ON_SSF("0");
		
		// add the user
		try {
			UserManager.addUser(ua, null, null, -1);
		}catch(BaseException be){
			errors.add(be.getMessage().replace("!", ""));
		}catch(DataException de){
			errors.add(de.getMessage().replace("!", ""));
		}
		
	}
	
}
