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
import java.util.Random;

import org.apache.commons.lang.StringEscapeUtils;

import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.TSParameters;
import ro.cst.tsearch.utils.URLMaping;

public class CommunityAdmin
{
	
    
    private Hashtable commexpanded = new Hashtable();

    public boolean isExpanded(String name){
    	return commexpanded.containsKey(name) || commexpanded.containsKey(StringEscapeUtils.unescapeHtml(name));
    }
    
    public void setExpanded(String name){
	commexpanded.put(name, "true");
    }

    public void setNotExpanded(String name){
	commexpanded.remove(name);
    }
    
    public void initExpanded(){
	commexpanded = new Hashtable();
    }
    
    public String getExp_Coll(String formName, String name){
	return "javascript:window.document.forms."+formName+"." + 
	    TSOpCode.OPCODE + ".value="+TSOpCode.COMM_EXP_COLL + 
	    ";window.document.forms."+formName+"." +
	    TSParameters.COMM_EXPANDED + ".value='" + name +
	    "';window.document.forms."+formName+".submit()";
    }
    

    

    public String getSelectHref(String formName){
	return "javascript:window.document.forms."+formName+"." + 
	    TSOpCode.OPCODE + ".value=" + TSOpCode.SELECT_COMMUNITY  
	    + ";window.document."+formName+".submit();";
    }

    public boolean isSelected(String s, String def){
	
	if(s!=null && s.equals(def))
	    return true;
	return false;
    }

    public String getViewHref(String formName, String comm_name){
	return "javascript:window.document.forms."+formName+"."+
	    CommunityAttributes.COMMUNITY_ID + ".value='" + comm_name +
	    "';window.document.forms."+formName+".action='" + 
	    URLMaping.path + URLMaping.COMMUNITY_PAGE_VIEW + 
	    "';window.document.forms."+formName+".submit();";
    }

    public String getEditHref(String formName){
	return "javascript:window.document.forms."+formName+"." 
	    + TSOpCode.OPCODE + ".value=" + TSOpCode.COMM_EDIT
	    + ";window.document."+formName+".action = '"
 	    +  URLMaping.path + URLMaping.COMMUNITY_PAGE_EDIT
	    + "';window.document."+formName+".submit()";
    }
    
    public String getDeleteHref(String formName){
	return "javascript:window.document.forms."+formName+"." 
	    + TSOpCode.OPCODE + ".value=" + TSOpCode.COMM_DEL
	    + ";window.document."+formName+".action = '"
 	    + URLMaping.path + URLMaping.COMMUNITY_DISPCH
	    + "';window.document.forms."+formName+".submit();";
    }


    public String getSaveHref(String formName){
	return "javascript:if(reqFieldsFilled()){document."
	+	formName+"." + TSOpCode.OPCODE + ".value=" 
	+ 	TSOpCode.SAVE_COMMUNITY
	    + ";window.document."+formName+".action = '" 
	    + URLMaping.path + URLMaping.COMMUNITY_DISPCH
	    + "';window.document."+formName+".submit()}";
    }
    
    public String getSavePolicyHref(String formName){
    	return "javascript:document."
    	+	formName+"." + TSOpCode.OPCODE + ".value=" 
    	+ 	TSOpCode.MANAGE_POLICYS
    	    + ";window.document."+formName+".action = '" 
    	    + URLMaping.path + URLMaping.COMMUNITY_DISPCH
    	    + "';window.document."+formName+".submit()";
        }
    
	public String getAddCommHref(String formName){
	return "javascript:if(reqFieldsFilled()){document."
	+	formName+"." + TSOpCode.OPCODE + ".value=" 
	+ 	TSOpCode.NEW_COMMUNITY
		+ ";window.document."+formName+".action = '" 
		+ URLMaping.path + URLMaping.COMMUNITY_DISPCH
		+ "';window.document."+formName+".submit()}";
	}   
    public String getCategAdminHref(String formName){
	return "javascript:window.document.forms."+formName + "."
	    + TSOpCode.OPCODE + ".value=" + TSOpCode.VIEW_CATEGORY
	    	    + ";window.document."+ formName+".action = '"
 	    + URLMaping.path + URLMaping.CATEGORY_PAGE_ADMIN
	    + "';window.document."+formName+".submit()";
    }

    public String getCommAddHref(String formName){
	return "javascript:window.document.forms."+ formName + "."
	    + TSOpCode.OPCODE + ".value=" + TSOpCode.NEW_COMMUNITY
	    	    + ";window.document."+formName+".action = '"
	    + URLMaping.path  + URLMaping.COMMUNITY_PAGE_ADD
	    + "';window.document."+formName+".submit()";
    }
       
}

 








