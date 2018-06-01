package ro.cst.tsearch.bean.community;

import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

public class HideCommunity
{
	public String getHideCommHref(String formName){
	return "javascript:window.document.forms."+formName+"." 
	    + TSOpCode.OPCODE + ".value=" + TSOpCode.HIDE_COMMUNITY 
	    + ";window.document.forms."+formName+".action='"
		+ URLMaping.path + URLMaping.COMMUNITY_DISPCH 
	    + "';window.document."+formName+".submit()";
    }

    public String getUnhideCommHref(String formName){
    return "javascript:window.document.forms."+formName+"." 
	    + TSOpCode.OPCODE + ".value=" + TSOpCode.UNHIDE_COMMUNITY 
	    + ";window.document.forms."+formName+".action='"
		+ URLMaping.path + URLMaping.COMMUNITY_DISPCH 
	    + "';window.document."+formName+".submit()"; 
    }
}
