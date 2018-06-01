package ro.cst.tsearch.servers;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @author george
 */
public class PassManager implements Serializable {
    
    static final long serialVersionUID = 10000000;
    
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	
	@SuppressWarnings("unused")
	private transient HashMap allUsers = new HashMap();
	@SuppressWarnings("unused")
	private transient HashMap allPass = new HashMap();
	@SuppressWarnings("unused")
	private transient HashMap lastIndex = new HashMap();
	
	public PassManager() {
//		Enumeration serversKeys = ResourceBundle.getBundle(URLMaping.SERVER_PASSWORDS).getKeys();
//		String serverName = "", usersPasswords = "";
//		String tempUser = "";
//		
//		while (serversKeys.hasMoreElements()) {
//			serverName = serversKeys.nextElement().toString();
//			usersPasswords = ResourceBundle.getBundle(URLMaping.SERVER_PASSWORDS).getString(serverName);
//			StringTokenizer st = new StringTokenizer(usersPasswords, ";");
//			Vector users = new Vector();
//			Vector passwords = new Vector();
//			while(st.hasMoreElements()) {
//				tempUser = st.nextToken().toString();
//				users.add(tempUser.substring(0, tempUser.indexOf(":")));
//				passwords.add(tempUser.substring(tempUser.indexOf(":") + 1));
//			}
//			allUsers.put(serverName, users);
//			allPass.put(serverName, passwords);
//		}
	}
	
//	public Hashtable getNextSeq(String serverNmae) {
//		serverNmae = serverNmae.substring(serverNmae.lastIndexOf(".") + 1);
//		Hashtable userAndPass = new Hashtable();
//		int pos = 0;
//		if (lastIndex.get(serverNmae) != null) {
//			try {
//				pos = Integer.parseInt(lastIndex.get(serverNmae).toString());
//			} catch (NumberFormatException e1) {
//				pos = 0;
//			}
//		}
//		if (pos + 1 >= ((Vector) allUsers.get(serverNmae)).size())
//			pos = 0;
//		userAndPass.put(USERNAME, ((Vector) allUsers.get(serverNmae)).elementAt(pos));
//		userAndPass.put(PASSWORD, ((Vector) allPass.get(serverNmae)).elementAt(pos));
//		pos ++;
//		lastIndex.put(serverNmae, new Integer(pos));
//		return userAndPass;
//	}
	
}
