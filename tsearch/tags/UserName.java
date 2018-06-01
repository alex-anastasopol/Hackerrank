package ro.cst.tsearch.tags;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import ro.cst.tsearch.data.User;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;

public class UserName extends TagSupport {

	public int doStartTag() {
		
	String name = "";
	User u = (User) pageContext.getSession().getAttribute(SessionParams.CURRENT_USER);
	if (u != null){
		UserAttributes ua = u.getUserAttributes();
		name += ua.getFIRSTNAME().toUpperCase();
		name += " ";
		name += ua.getLASTNAME().toUpperCase();
	}
	JspWriter writer = pageContext.getOut();
	try {
		writer.print(StringUtils.HTMLEntityEncode(name));
	} catch (IOException e) {
		e.printStackTrace();
	}
	
	return SKIP_BODY;
	}
}
