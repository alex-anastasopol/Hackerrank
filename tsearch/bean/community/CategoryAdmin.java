/**
 * @(#) CommunityAdmin.java 1.3 12/14/2000
 *
 * Copyright 1999-2000 CornerSoft Technologies SRL.All rights reserved.
 *
 * This software is proprietary information of CornerSoft Technologies.
 * Use is subject to license terms.
 */
package ro.cst.tsearch.bean.community;
import java.util.Hashtable;

import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.TSParameters;
import ro.cst.tsearch.utils.URLMaping;
public class CategoryAdmin
{
	
    public boolean isSelected(String s, String def){
	
	if(s!=null && s.equals(def))
	    return true;
	return false;
    }

    public String getDeleteHref(String formName){
	return "javascript:window.document.forms."+formName+"." 
	    + TSOpCode.OPCODE + ".value=" + TSOpCode.DEL_CATEGORY 
	    + ";window.document.forms."+formName+".action='"
		+ URLMaping.path + URLMaping.COMMUNITY_DISPCH 
	    + "';window.document."+formName+".submit()";
    }

    public String getSaveHref(String formName){
	return "javascript:if(reqFieldsFilled()){window.document.forms."+formName+"." 
	    + TSOpCode.OPCODE + ".value=" + TSOpCode.SAVE_CATEGORY
	    + ";window.document.forms."+formName+".action='"
		+ URLMaping.path + URLMaping.COMMUNITY_DISPCH 
	    + "';window.document."+formName+".submit()}"; 
    }

    public String getCategAddHref(String formName){
	return "javascript:window.document.forms."+formName+"." 
	    + TSOpCode.OPCODE + ".value=" + TSOpCode.ADD_CATEGORY
	    + ";window.document.forms."+formName+".action='"
		+ URLMaping.path + URLMaping.CATEGORY_PAGE_ADD
	    + "';window.document."+formName+".submit()";
    }
    
}

 








