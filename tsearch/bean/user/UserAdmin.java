package ro.cst.tsearch.bean.user; 

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Random;

import org.apache.log4j.Logger;

import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserFilter;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.MultipartParameterParser;
import ro.cst.tsearch.utils.ParameterParser;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.TSParameters;
import ro.cst.tsearch.utils.URLMaping;

public class UserAdmin{

	private static final Logger logger = Logger.getLogger(UserAdmin.class);
	
	private UserFilter uf = new UserFilter(UserFilter.SORT_FULLNAME, UserFilter.SORT_ASC); 


	/**
	* This function is defined within the {@link ro.cst.vems.bean.utils.Template Template} interface
	* so needs to be implemented.
	* @param section is refering the section id (e.g. wnew, projects, documents etc.)
	* @param subsection is refering the subsection id within the specified <code>section</code>(e.g. project details, posting new timereports etc.)
	* @return the href for the 'help' link
	*/
	public String getHelpHref(String section, String subsection) {
		return "javascript:help_page('" + section + "','" + subsection + "');";
	}




	public String getBackHref() {
		/*return "javascript:this.location.href="
			+ "document.forms[0]."
			+ VPOParameters.RET_PAGE
			+ ".value;";*/
		return "javascript:history.back();";
	}


	public void setUserFilter(String sortCriteria, String sortOrder) {
		uf.setSortCriteria(sortCriteria);
		uf.setSortOrder(sortOrder);
	}
	public UserFilter getUserFilter() {
		return this.uf;
	}

	/**
	 * Build the action for creating the ViewUser page.
	 * Parameters:
	 *   - String user_id : the user identifier
	 *   - String formName : the name of the source form.
	 */
	@SuppressWarnings("deprecation")
	public String getViewHref(String user_id, long searchId, String commViewed,String formName) {

		return "javascript:window.document."
			+ formName
			+ ".action = '"
			+ URLMaping.path + URLMaping.USER_VIEW
			+ "?"
			+ UserAttributes.USER_ID
			+ "="
			+ URLEncoder.encode(URLEncoder.encode(user_id))
			+ "&"
			+ RequestParams.SEARCH_ID			
			+ "="
			+ searchId
			+  "&"
			+ CommunityAttributes.COMMUNITY_VIEWED
			+ "="
			+ commViewed		
			+ "&"
			+ TSOpCode.OPCODE
			+ "="
			+ TSOpCode.USER_VIEW
			+ "';window.document."
			+ formName
			+ ".submit();";
	}

	/**
	 * Build the action for creating the EditUser page.
	 * Parameters:
	 *   - String login : the user identifier
	 *   - String formName : the name of the source form.
	 */
	public String getEditHref(String login, String formName) {
		return "javascript:"
			+ "document."
			+ formName
			+ ".action = '"
			+ URLMaping.path + URLMaping.USER_EDIT
			+ "'; document."
			+ formName
			+ "."
			+ UserAttributes.USER_ID
			+ ".value = '"
			+ login
			+ "'; document."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value = '"
			+ TSOpCode.USER_VIEW
			+ "'; document."
			+ formName
			+ ".submit();";
	}

	/**
	 * Build the action for creating the EditUser page.
	 * Parameters:
	 *   - String user_id : the user id
	 *   - String formName : the name of the source form.
	 * This reference is built from the ViewUser page.
	 */
	public String getEditVHref(String user_id, String formName, String searchId, int opCode) {

		return "window.document."
			+ formName
			+ ".action = '"
			+ URLMaping.path + URLMaping.USER_EDIT
			+ "?"
			+ UserAttributes.USER_ID
			+ "="
			+ URLEncoder.encode(user_id)
			+ "&"
			+ TSOpCode.OPCODE
			+ "="
			+ opCode
			+ "&"
			+ RequestParams.SEARCH_ID
			+ "="
			+ searchId
			+ "';window.document."
			+ formName
			+ ".submit();";
	}

	/**
	 * Build the action to delete one or more users.
	 * Parameters:
	 *   - String formName : the name of the source form.
	 * This reference is built from UserList page.
	 */
	public String getDeleteHref(String formName) {
		return "javascript: window.document.forms."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.USERS_DEL
			+ ";window.document."
			+ formName
			+ ".action= '"
			+ URLMaping.path + URLMaping.UserDispacher
			//+ VPOPages.getContextPath() + JspConstants.USERS_PAGE_USERLIST 
			+"';window.document." + formName + ".submit();";
	}

	/**
	 * Build the action to delete a specific user.
	 * Parameters:
	 *   - String user_id: the user identifier
	 *   - String formName : the name of the source form.
	 * This reference is built from UserList page.
	 */
	public String getDeleteUHref(String formName) {
		return "javascript:if(checkSelected()){window.document.forms."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.USER_DEL
			+ ";window.document."
			+ formName
			+ ".action ='"
			+ URLMaping.path + URLMaping.UserDispacher
			+ "';confirmDelete();}";
	}

	/**
	 * Clear a form.
	 */
	public String getClearHref(String formName) {
		return "javascript:alert('clearTheForm');";
	}

	public String getApplyHrefVPO(String formName) {
		return "javascript:if(checkedRequiredFields()){window.document.forms."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.USERS_ADD_APPLY
			+ ";window.document."
			+ formName
			+ ".action ='"
			+ URLMaping.path + URLMaping.UserDispacher
			+ "?nocache="
			+ System.currentTimeMillis()
			+ "';window.document."
			+ formName
			+ ".submit();}";
	}

	public String getSubmitHrefVPO(String formName) {
		return "javascript:if(checkedRequiredFields()){window.document.forms."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.USER_ADD_SAVE
			+ ";window.document."
			+ formName
			+ ".action ='"
			+ URLMaping.path + URLMaping.UserDispacher			
			+ "';window.document."
			+ formName
			+ ".submit();}";
	}
	public String getResetHrefVPO(String formName) {
		return "javascript:window.document." + formName + ".reset();";
	}
	
	/**
	 * Build the action for creating a new user
	 * Parameters:
	 *   - String formName : the name of the source form.
	 */
	public String getSubmitHref(String formName) {
		return "javascript:if(checkedRequiredFields()){window.document."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.SAVE_RESOURCE
			+ ";window.document."
			+ formName
			+ ".action ='"
			+ URLMaping.path + URLMaping.USER_VIEW			
			+ "';window.document."
			+ formName
			+ ".submit()}";

	}
	public String getApplyHref(String formName) {
		return "javascript:if(checkedRequiredFields()){window.document."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.USER_ADD_APPLY
			+ ";window.document."
			+ formName
			+ ".action ='"
			+ URLMaping.path + URLMaping.UserDispacher
			+ "?nocache="
			+ System.currentTimeMillis()
			+ "';window.document."
			+ formName
			+ ".submit()}";

	}

	public String usersOpCodeDispacher(
		int opCode,
		MultipartParameterParser mpp,
		ParameterParser pp,
		String user_id,
		String comm_id,long searchId)
		throws BaseException {
		logger.debug("User Dispacher routine...");
		String ret = "OK";
		UserFilter ufind = getUserFilter();
		UserAttributes cua = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
		
		int defaultpages = UserUtils.getUserPages(cua);
		String userPages = "";
		try {
			userPages =
				mpp.getMultipartStringParameter(
					UserAttributes.USER_PAGES,
					new Integer(defaultpages).toString());
		} catch (Exception e) {

			userPages =
				pp.getStringParameter(
					UserAttributes.USER_PAGES,
					new Integer(defaultpages).toString());
		}
		UserUtils.setUserPages(userPages, cua);
		try {
			switch (opCode) {
				case TSOpCode.SAVE_RESOURCE :
					break;
				case TSOpCode.USER_ADD_SAVE :
					//ret = UserManager.addResource(mpp, user_id);
					break;
				case TSOpCode.USERS_ADD_APPLY :
					//ret = addResource(mpp, user_id);
					break;					
				case TSOpCode.USERS_SORTNAME :
					if (this.uf.getSortOrder().equals(UserFilter.SORT_ASC))
						this.setUserFilter(
							UserFilter.SORT_NAME,
							UserFilter.SORT_DESC);
					else
						this.setUserFilter(
							UserFilter.SORT_NAME,
							UserFilter.SORT_ASC);
					break;
				case TSOpCode.USERS_SORTFULLNAME :
					if (this.uf.getSortOrder().equals(UserFilter.SORT_ASC))
						this.setUserFilter(
							UserFilter.SORT_FULLNAME,
							UserFilter.SORT_DESC);
					else
						this.setUserFilter(
							UserFilter.SORT_FULLNAME,
							UserFilter.SORT_ASC);
					break;
				case TSOpCode.USERS_SORT_LASTLOGIN :
					if (this.uf.getSortOrder().equals(UserFilter.SORT_ASC))
						this.setUserFilter(
							UserFilter.LAST_LOGIN,
							UserFilter.SORT_DESC);
					else
						this.setUserFilter(
							UserFilter.LAST_LOGIN,
							UserFilter.SORT_ASC);
					break;
				case TSOpCode.USERS_LIKE :
					String userslike =
						pp.getStringParameter(TSParameters.USERLIKE, "$");
					if (userslike.charAt(0) == '$') {
						ufind.setLikeFlag(false);
						ufind.setFindFlag(false);
					} else {
						ufind.setFindFlag(false);
						ufind.setLikeFlag(true);
						ufind.setSortLike(userslike);
					}
					break;
				case TSOpCode.USERS_USERFIND :
					String userfind =
						pp.getStringParameter(TSParameters.USERFIND, "(All)");
					int findrole =
						pp.getIntParameter(TSParameters.FINDROLE, -1);
					String findskill =
						pp.getStringParameter(TSParameters.FINDSKILL, "-1");

					ufind.setFindFlag(true);
					ufind.setLikeFlag(false);
					if (userfind.equals("(All)"))
						userfind = "";
					ufind.setUserFind(userfind, findrole, findskill);
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			ret = "Please fill all required fields!";
		}
		return ret;
	}

	/**
	 * Build the action for downloading user resume
	 * Parameters:
	 *   - BigDecimal user_id : the user identifier
	 */
	public String getResumeHref(BigDecimal user_id) {

		String[] params =
			{
				TSParameters.ACTION,
				"DOWNLOAD_RESUME",
				UserAttributes.USER_ID,
				user_id.toString()
			};

		return URLMaping.getAbsoluteURL(URLMaping.GET_PHOTO, params);
	}

	/**
	 * Build the action for downloading user resume
	 * Parameters:
	 *   - BigDecimal user_id : the user identifier
	 */
	public String getPhotoHref(String login) {

		String[] params =
			{
				TSParameters.ACTION,
				"VIEW_PICTURE",
				UserAttributes.USER_ID,
				login,
				"update",
				new Integer(new Random().nextInt()).toString()
			};

		return URLMaping.getAbsoluteURL(URLMaping.GET_PHOTO, params);
	}

	public String getDeleteFromCommHref(String formName) {
		return "javascript:if(checkSelected()){window.document.forms."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.USERS_DEL
			+";window.document." + formName + ".action= '"		
			+ URLMaping.path + URLMaping.USER_LIST
			+ "';confirmDelete();}";
	}

	public String getUserAddHref(String formName) {
		StringBuilder builder = new StringBuilder();
		builder.append("javascript:");
		builder.append("if (checkIfCommunitySelected()){");
		builder.append("window.document.forms.");
		builder.append(formName);
		builder.append(".");
		builder.append(TSOpCode.OPCODE);
		builder.append(".value=");
		builder.append(TSOpCode.USER_ADD);
		builder.append(";window.document.");
		builder.append(formName);
		builder.append(".action = '");
		builder.append(URLMaping.path);
		builder.append(URLMaping.USER_ADD);
		builder.append("';window.document.");
		builder.append(formName);
		builder.append(".submit();}");
		return builder.toString();
	}

	public String getUserListOrderNameHref(String formName) {
		return "javascript:if(checkifNull()){window.document."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.USERS_SORTNAME
			+ ";window.document.forms."
			+ formName
			+ ".action = '"
			+ URLMaping.path + URLMaping.USER_LIST
			+ "';window.document."
			+ formName
			+ ".submit()}";
	}

	public String getUserListOrderFullNameHref(String formName) {
		return "javascript:if(checkifNull()){window.document."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.USERS_SORTFULLNAME
			+ ";window.document.forms."
			+ formName
			+ ".action = '"
			+ URLMaping.path + URLMaping.USER_LIST			
			+ "';window.document."
			+ formName
			+ ".submit()}";
	}

	public String getUserListOrderLastLoginHref(String formName) {
		return "javascript:if(checkifNull()){window.document."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.USERS_SORT_LASTLOGIN
			+ ";window.document.forms."
			+ formName
			+ ".action = '"
			+ URLMaping.path + URLMaping.USER_LIST
			+ "';window.document."
			+ formName
			+ ".submit()}";
	}

	public String getUserListLikeHref(String formName, char letter) {
		return "javascript:if(checkifNull()){window.document."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.USERS_LIKE
			+ ";window.document."
			+ formName
			+ "."
			+ TSParameters.USERLIKE
			+ ".value="
			+ "'"
			+ letter
			+ "'"
			+ ";window.document."
			+ formName
			+ "."
			+ TSParameters.LASTPAGE
			+ ".value= 0"
			+ ";window.document."
			+ formName
			+ "."
			+ TSParameters.USERFIND
			+ ".value='(All)'"
			+ ";window.document.forms."
			+ formName
			+ ".action = '"
			+ URLMaping.path + URLMaping.USER_LIST			
			+ "';window.document."
			+ formName
			+ ".submit()}";
	}
	public String getUserListUserFindNameHref(String formName) {
		return "javascript:if(checkifNull()){window.document."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.USERS_USERFIND
			+ ";window.document."
			+ formName
			+ "."
			+ TSParameters.LASTPAGE
			+ ".value= 0"
			+ ";window.document."
			+ formName
			+ "."
			+ TSParameters.USERLIKE
			+ ".value= '%'"
			+ ";window.document.forms."
			+ formName
			+ ".action = '"
			+ URLMaping.path + URLMaping.USER_LIST			
			+ "';window.document."
			+ formName
			+ ".submit()}";
	}
	public String getUserPageHref(String formName, int i) {
		return "javascript:if(checkifNull()){window.document."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.USERS_PAGE
			+ ";window.document."
			+ formName
			+ "."
			+ TSParameters.LASTPAGE
			+ ".value="
			+ i
			+ ";window.document.forms."
			+ formName
			+ ".action = '"
			+ URLMaping.path + URLMaping.USER_LIST			
			+ "';window.document."
			+ formName
			+ ".submit()}";
	}
	public String getChangeCommHref(String formName) {
		return "javascript:if(checkifNull()){window.document."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.USERS_COMMVIEW
			+ ";window.document."
			+ formName
			+ "."
			+ TSParameters.LASTPAGE
			+ ".value= 0"
			+ ";window.document.forms."
			+ formName
			+ ".action = '"
			+ URLMaping.path + URLMaping.USER_LIST			
			+ "';window.document."
			+ formName
			+ ".submit()}";
	}
	
	/**
	 * Build the action to hide one or more users.
	 * Parameters:
	 *   - String formName : the name of the source form.
	 * This reference is built from UserList page.
	 */	
	public String getUserHideHref(String formName){
		return "javascript:if(checkSelected()){window.document.forms."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.USER_HIDE
			+ ";window.document."
			+ formName
			+ ".action ='"
			+ URLMaping.path + URLMaping.UserDispacher
			+ "';confirmHide();}";
	}
	
	/**
	 * Build the action to unhide one or more users.
	 * Parameters:
	 *   - String formName : the name of the source form.
	 * This reference is built from UserList page.
	 */	
	public String getUserUnhideHref(String formName){
		return "javascript:if(checkSelected()){window.document.forms."
			+ formName
			+ "."
			+ TSOpCode.OPCODE
			+ ".value="
			+ TSOpCode.USER_UNHIDE
			+ ";window.document."
			+ formName
			+ ".action ='"
			+ URLMaping.path + URLMaping.UserDispacher
			+ "';confirmUnhide();}";		
	}
	
	/**
	 * Build the action for creating the EditMyAts page.
	 * Parameters:
	 *   - String user_id : the user id
	 *   - String formName : the name of the source form.
	 * This reference is built from the ViewMyA page.
	 */
	public String getMyATSEditVHref(BigDecimal user_id, String formName, String searchId, int opCode) {

		return "window.document."
			+ formName
			+ ".action = '"
			+ URLMaping.path + URLMaping.MY_ATS_EDIT
			+ "?"
			+ UserAttributes.USER_ID
			+ "="
			+ user_id
			+ "&"
			+ TSOpCode.OPCODE
			+ "="
			+ opCode
			+ "&"
			+ RequestParams.SEARCH_ID
			+ "="
			+ searchId
			+ "';window.document."
			+ formName
			+ ".submit();";
	}
}
