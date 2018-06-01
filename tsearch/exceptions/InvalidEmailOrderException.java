
package ro.cst.tsearch.exceptions;

/**
 * 
 * @author radu bacrau
 *
 */
public class InvalidEmailOrderException extends Exception{
	
	public static final long serialVersionUID = 10000;
	
	boolean undefinedState = false;
	boolean undefinedCounty = false; 
	boolean invalidUser = false;
	boolean parsingIssue = false;
	String userName = null;
	boolean updNoFID = false;
	boolean updWrongFID = false;
	String fid = null;
	String fidAgent = null;
	boolean invalidPassword = false;
	
	public InvalidEmailOrderException(String message){
		super(message);
	}
	
	public void setInvalidUser(boolean invalidUser){
		this.invalidUser = invalidUser;
	}
	
	public void setUndefinedCounty(boolean undefinedCounty){
		this.undefinedCounty = undefinedCounty;
	}
	
	public void setUndefinedState(boolean undefinedState){
		this.undefinedState = undefinedState;
	}
	
	public void setUserName(String userName){
		this.userName = userName;
	}
	
	public void setUpdNoFID(boolean updNoFID){
		this.updNoFID = updNoFID;
	}
	
	public void setUpdWrongFID(boolean updWrongFID){
		this.updWrongFID = updWrongFID;
	}
	
	public void setFid(String fid){
		this.fid = fid;
	}
	public void setFidAgent(String fidAgent){
		this.fidAgent = fidAgent;
	}
	
	public void setInvalidPassword(boolean invalidPassword){
		this.invalidPassword = invalidPassword;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getCausesDescription(){
		String retVal = "";
		if(undefinedState){
			retVal += "    - State not defined\n";
		}
		if(undefinedCounty){
			retVal += "    - County not defined\n";
		}
		if(invalidUser){
			if(userName!= null){
				retVal += ("    - User invalid: " + userName + "\n");	
			}else{
				retVal += ("    - User not defined\n");
			}
		}
		if(invalidPassword){
			retVal += ("    - Invalid password for user: " + userName + "\n");	
		}
		if(parsingIssue){
			retVal += "    - Parsing issue\n";
		}
		if(updNoFID){
			retVal += "    - Requested an update with no abstractor file ID\n";
		}
		if(updWrongFID){
			retVal += "    - Requested an update, but abstractor file ID [" + fid + "] and agent file ID [" + fidAgent + "] does not belong to a completed search\n";
		}
		return retVal;
	}
}