/**
 * @(#) CommunityAdmin.java 1.3 12/14/2003
 *
 * Copyright 1999-2003 CornerSoft Technologies SRL.All rights reserved.
 *
 * This software is proprietary information of CornerSoft Technologies.
 * Use is subject to license terms.
 */
package ro.cst.tsearch.community;

import java.math.BigDecimal;

import ro.cst.tsearch.data.DataAttribute;
import ro.cst.tsearch.database.DatabaseData;

/**
 * Class used in order to define attributes for category entities
 * .
 *
 * @version     1.30 August 2003
 */
public class CategoryAttributes extends DataAttribute{

	
	public static final String CATEGORY_ID   = "CATEG_ID";
	public static final String CATEGORY_NAME = "CATEG_NAME";	
	public static final String COMM_DEFAULT_CATEGORY="Main";
    
    // attributes section
    public static final int ID   = 0;
    public static final int NAME = 1;
    // end attributes section
	protected int getAttrCount() {

		return NAME + 1;

	}

    /** a basic constructor used for special circumstances */
    public CategoryAttributes() {}
	public CategoryAttributes(String name) {
		setNAME(name);
	}
    
	public CategoryAttributes(DatabaseData data, int row) {
		setID(data,row);
		setNAME(data,row);
	}
    
	/**
	 * @return
	 */
	public BigDecimal getID() {
		return new BigDecimal((Integer)getAttribute(ID));
	}
	public  void setID(BigDecimal value) {
		setAttribute(ID,value);
	}
	public  void setID(DatabaseData data, int row) {
		setAttribute(ID, data, CATEGORY_ID, row);
	}
	
	/**
	 * @return
	 */
	public String getNAME() {
		return (String)getAttribute(NAME);
	}
	public  void setNAME(String value) {
		setAttribute(NAME,value);
	}
	public  void setNAME(DatabaseData data, int row) {
		setAttribute(NAME, data, CATEGORY_NAME, row);
	}	

}



