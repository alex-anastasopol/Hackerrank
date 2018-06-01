/*
 * @(#)UserFilter.java 1.30 2000/08/16
 * Copyright (c) 1998-2000 CornerSoft Technologies, SRL
 * Bucharest, Romania
 * All Rights Reserved.
 *
 * This software is he confidential and proprietary information of
 * CornerSoft Technologies, SRL. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in accordance
 * with the terms of the license agreement you entered into with CST.
 */

package ro.cst.tsearch.user;

import java.util.Vector;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.DBConstants;
import org.apache.log4j.Category;

/**
 * This class handles filters for searching users through Database in
 * Title Search application.
 *
 * @version     1.30 August 2000
 * @authors     Carmen Irimie, Ciprian Alecsandru
 */
public class UserFilter {

	// constants definition area
	public static final String NAME_ALL = "*";
	public static final String SORT_NAME = UserAttributes.USER_LOGIN;
	public static final String SORT_FULLNAME = UserAttributes.USER_LASTNAME;
	public static final String SORT_FIRST_LAST_NAME = " concat(" + DBConstants.TABLE_USER + "." + UserAttributes.USER_FIRSTNAME + ",' '," + DBConstants.TABLE_USER + "." + UserAttributes.USER_LASTNAME + ") ";
	public static final String SORT_FIRSTNAME = UserAttributes.USER_FIRSTNAME;
	public static final String LAST_LOGIN = UserAttributes.USER_LASTLOGIN;

	public static final String DELETED = "0";
	public static final String GROUP_ALL = "*";
	public static final String SORT_DESC = "DESC";
	public static final String SORT_ASC = "ASC";
	public static final String SORT_LIKE = "LIKE";

	// end constants definition area

	/** a sorting criteria variable, by default is seted to userName*/
	private String sortCrt = SORT_NAME;
	private String sDeleted = DELETED;
	private String sName = NAME_ALL;
	private String sGroup = GROUP_ALL;
	private String sortOrder = SORT_ASC;
	private String sortLike = "";
	private String sortUserFind = "";
	private String joinCondition = "";
	private String joinTables = "";

	/*  private String sfindRole	= "";
	    private String sfindSkill   = "";*/

	private boolean special = false;
	private boolean like = false;
	private boolean userFind = false;
	
	/**
	 * The findUserValues is used to store the values of the parameters 
	 * of the sql query (which will contain '?' instead of the parameters) 
	 */
	private Vector<String> findUserValues = new Vector<String>();
	private String sortLikeValue= "";

	protected static final Category logger= Category.getInstance(UserFilter.class.getName());
	
	public UserFilter() {
	}

	public UserFilter(String sortCriteria, String sortOrder) {
		this.setSortCriteria(sortCriteria);
		this.setSortOrder(sortOrder);
	}

	public void setName(String name) {
		sName = name;
	}

	public void setSortCriteria(String sort_crt) {
		sortCrt = "upper(" + DBConstants.TABLE_USER + "." + DBManager.sqlColumnName(sort_crt) + ")";
	}

	public void setDeleted(String delete) {
		sDeleted = delete;
	}

	public void setGroup(String group) {
		sGroup = group;
	}
	public void setSortOrder(String order) {
		sortOrder = DBManager.sqlOrderType(order);
	}
	public void setSortLike(String like) {
		this.sortLikeValue = like + "%";
		sortLike =
			"(lower("
				+ DBConstants.TABLE_USER
				+ "."
				+ UserFilter.SORT_FULLNAME
				+ ")"
				+ " LIKE  lower(?"
				+ "))";
	}

	public void setSpecialFlag(boolean special) {
		this.special = special;
	}

	public void setLikeFlag(boolean like) {
		this.like = like;
	}

	public void setFindFlag(boolean userFindFlag) {
		this.userFind = userFindFlag;
	}

	public void setUserFind(String findUser, int findRole, String findSkill) {

		String filter;
		String findUsers[];
		findUsers = findUser.split(" ");
		filter = "";
		
		this.findUserValues.clear();
		
		if (!findUser.equals("")) { 
			filter =
				filter
					+ " AND (lower("
					+ DBConstants.TABLE_USER
					+ "."
					+ UserFilter.SORT_NAME
					+ ")"
					+ " LIKE  lower(?)"
					+ " OR (";
			for (int i = 0; i < findUsers.length; i++){
				filter += "lower(" + UserFilter.SORT_FIRST_LAST_NAME + ") like " +
						"lower(?) ";
				if (i != findUsers.length -1){
					filter += " AND ";
				}
			}
			filter += " )) ";
			
			this.findUserValues.add("%"+findUser+"%");
			for (int i = 0; i < findUsers.length; i++) {
				 this.findUserValues.add("%"+findUsers[i]+"%");
			}
		}
		if (findRole != -1) {
			filter = filter + 
			" AND "
				+ DBConstants.TABLE_USER
				+ "."
				+ GroupAttributes.GROUP_ID
				+ " = "
				+ new Integer(findRole);			
		}
		
		/*if (findRole != -1) {
			filter = filter + /*(filter.equals ("")? "AND ":"  " )
			" AND "
				+ DBConstants.TABLE_USER_COMMUNITY
				+ "."
				+ DBConstants.GROUP_ID
				+ " = "
				+ new Integer(findRole);

		}*/

		/*if (!findSkill.equals("-1")) {

			filter = filter + 
			" AND "
				+ DataConstants.TABLE_SKILLS
				+ "."
				+ DataConstants.SKILLS_NAME
				+ " = '"
				+ findSkill
				+ "'";

		}
*/
		setJoinCondition(findUser, findRole, findSkill);
		setJoinTables(findUser, findRole, findSkill);
		this.sortUserFind = filter;

	}

	public void setJoinCondition(
		String findUser,
		int findRole,
		String findSkill) {
		String joinCond = "";
		/*if (!findSkill.equals("-1")) {
			joinCond =
				" AND "
					+ DBConstants.TABLE_USER
					+ "."
					+ UserAttributes.USER_ID
					+ " = "
					+ DataConstants.TABLE_USER_SKILLS
					+ "."
					+ DataConstants.USER_ID
					+ " AND " 
					+ DBConstants.TABLE_USER
					+ "."
					+ UserAttributes.USER_ID
					+ " = "
					+ DataConstants.TABLE_COMMUNITY_USER_SKILLS 
					+ "."
					+ DataConstants.USER_ID
					+ " AND "
					+ DataConstants.TABLE_USER_SKILLS
					+ "."
					+ DataConstants.SKILLS_ID
					+ " = "
					+ DataConstants.TABLE_SKILLS
					+ "."
					+ DataConstants.SKILLS_ID
					+ " AND "
					+ DataConstants.TABLE_SKILLS
					+ "."
					+ DataConstants.SKILLS_ID
					+ " = "
					+ DataConstants.TABLE_COMMUNITY_USER_SKILLS 
					+ "."
					+ DataConstants.SKILLS_ID;
		}*/
		if (findRole != -1) {
			joinCond =
				joinCond
					+ " AND "
					+ DBConstants.TABLE_USER_COMMUNITY
					+ "."
					+ UserAttributes.USER_ID
					+ " = "
					+ DBConstants.TABLE_USER
					+ "."
					+ UserAttributes.USER_ID;
		}

		this.joinCondition = joinCond; 
		if (logger.isDebugEnabled())
			logger.debug("Join Condition in find user filter.." + joinCond + "\n");

	}
	public String getJoinCondition() {
		return this.joinCondition;
	}

	public void setJoinTables(
		String findUser,
		int findRole,
		String findSkill) {
		String filter = "";
		if (!findSkill.equals("-1")) {
			/*filter =
				", "
					+ DataConstants.TABLE_USER_SKILLS
					+ ", "
					+ DataConstants.TABLE_SKILLS
					+ ", "
					+ DataConstants.TABLE_COMMUNITY_USER_SKILLS;*/
		}
		this.joinTables = filter;
	}

	public String getJoinTables() {
		return this.joinTables;
	}

	public String getName() {
		return sName;
	}

	public String getSortCriteria() {
		return sortCrt;
	}
	public String getSortOrder() {
		return sortOrder;
	}
	public String getSortLike() {
		return sortLike;
	}

	public boolean getSpecialFlag() {
		return special;
	}

	public boolean getLikeFlag() {
		return like;
	}

	public boolean getFindFlag() {
		return this.userFind;
	}
	public String getUserFind() {
		return sortUserFind;
	}

	public String getDeleted() {
		return sDeleted;
	}

	public String getGroup() {
		return sGroup;
	}

	public Vector<String> getFindUserValues() {
		return this.findUserValues; 
	}
	
	public void setFindUserValues(Vector<String> findUser) {
		this.findUserValues = findUser;
	}
	
	public String getSortLikeValue() {
		return this.sortLikeValue; 
	}
	
	public void setSortLikeValue(String sortLikeValue) {
		this.sortLikeValue = sortLikeValue;
	}
}
