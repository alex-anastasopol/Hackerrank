/*
 * Created on Jul 30, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.servlet.user;

import java.io.IOException;
import java.math.BigDecimal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;

/**
 * @author nae
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class UserAdmin extends BaseServlet{
	public void doRequest(
		HttpServletRequest request,
		HttpServletResponse response)
		throws IOException, ServletException {
			try{
				UserAttributes newUser = new UserAttributes();			
				newUser.setLOGIN(UserAttributes.TSADMIN);
				newUser.setPASSWD("tsadmin");
				newUser.setFIRSTNAME("TitleSearch");
				newUser.setLASTNAME("Administrator");
				newUser.setMIDDLENAME(" ");
				newUser.setEMAIL("tsearcg@cst.ro");
				newUser.setCOMMID(new BigDecimal(1));
				newUser.setCOMPANYID(new BigDecimal(1));
				newUser.setGROUP(new BigDecimal(1));					
				//UserManager.addUser(newUser);				
			}catch(Exception e){
				e.printStackTrace();
			}
	}	
}
