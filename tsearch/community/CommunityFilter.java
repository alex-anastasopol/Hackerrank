/*
 * @(#)CommunityFilter.java 1.30 2000/08/15
 * Copyright (c) 1998-2000 CornerSoft Technologies, SRL
 * Bucharest, Romania
 * All Rights Reserved.
 *
 * This software is he confidential and proprietary information of
 * CornerSoft Technologies, SRL. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in accordance
 * with the terms of the license agreement you entered into with CST.
 */
package ro.cst.tsearch.community;


/** 
 * Control queries to the community database.
 */
public class CommunityFilter {

    // constants definition area
    public static final String NAME_ALL         = "*";
    public static final String DESTINATION_ALL  = "community";
    public static final String CATEGORY_ALL     = "*";

    public static final String SORT_NAME        = CommunityAttributes.COMMUNITY_NAME;
    public static final String SORT_TYPE        = "type";

    public static final String BACK_UP          = "false";
    public static final String DELETED          = "false";
    // end constants definition area

    private String sortCrt       	= SORT_NAME;
    private String sDestination  	= DESTINATION_ALL;
    private String sName         	= NAME_ALL;
    private String sCategory     	= CATEGORY_ALL;
    private String sDeleted      	= DELETED;
    private String sBackedUp     	= BACK_UP;
     boolean adminComm 			= false;

    /**Default constructor*/
    public CommunityFilter(){
    }
    /**Set the SortCriteria for this filter.
     * @param sort_crt the sort creteria.
     *Can be:
     * {@link #SORT_NAME}
     */
    public void setSortCriteria(String sort_crt){
        sortCrt = sort_crt;
    }
    /**This method it is not used.*/
    public void setCategory(String category){
        sCategory = category;
    }
    /**This method it is not used.*/
    public void setName(String name){
        sName = name;
    }
    /** Set the deleted flag for this filter.
     * Possible values:
     * {@link #DELETED}, "true"
     */
    public void setDeleted(String deleted){
        sDeleted = deleted;
    }
    /**HAVE TO BE DONNE*/
    public void setBacketUp(String backetup){
        sBackedUp = backetup;
    }
    /**This method it is not used.*/
    
    public void setAdminCommuntiesFlag(boolean flag){
	this.adminComm = flag;
    }

    /**Get the sort creteria for this filter*/
    public String getSortCriteria(){
        return sortCrt;
    }
    /**This method it is not used.*/
    public String getCategory(){
        return sCategory;
    }
    /**This method it is not used.*/
    public String getName(){
        return sName;
    }
    /**This method it is not used.*/
    public String getDestination(){
        return sDestination;
    }
    /**Get the deleted flag for this filter*/
    public String getDeleted(){
        return sDeleted;
    }
    /**HAVE TO BE DONNE*/
    public String getBacketUp(){
        return sBackedUp;
    }
    public boolean getAdminCommuntiesFlag(){
	return this.adminComm ;
    }

}











