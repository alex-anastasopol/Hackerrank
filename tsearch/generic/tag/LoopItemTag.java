
package ro.cst.tsearch.generic.tag;

import javax.servlet.jsp.JspTagException;

import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.utils.DBConstants;

public class LoopItemTag 
    extends GenericTag {
    
    protected String field;
    public void setField(String field) { this.field = field; }
    public String getField() {return field;}

    protected String param;
    public void setParam(String param) { this.param = param; }
    public String getParam() {return param;}

    protected LoopTag lt;
    public LoopTag getLoopTag() { return lt; };
    public void setLoopTag(LoopTag lt) { this.lt = lt; };
    
    protected String defaultValue = "";
    public void setDefaultValue(String defaultValue){this.defaultValue = defaultValue;}
    public String getDefaultValue(){return defaultValue;}

    private int currentIndex = 0;
    public void setCurrentIndex(String str) {
        try{ currentIndex = Integer.parseInt(str); } catch (NumberFormatException nfe){;}; 
    }
   
    public int getCurrentIndex(){
    		return currentIndex;
    }

    /**
     * translates the value loaded from the looptag (using a dictionary for
     * example)
     * is this class just return the received value or the default value
     * 
     * @param s
     * @return String
     */
    protected String translateItem(String s){
    	s = s.trim();
    	if (s.length() != 0 ){
    		return s;
    	}
    	else{
    		if (defaultValue.length() != 0)
    			return defaultValue;
    		else
    			return s;
    	}
    }
    
    /**
     * converts the object to its string value taking into accout the
     * database default values BLANK_VALUE_XXXXXX
     * @param a
     * @return String
     */
    private String getStringValue(Object a){
    	
    	Class cl = a.getClass();
    	
    	if (cl==Double.class || cl==double.class) {
    		if (a.equals(new Double(DBConstants.BLANK_VALUE_DOUBLE)))
    			return "";
    		else
    			return a.toString();
    	} else if (cl==Integer.class || cl==int.class) {
    		if (a.equals(new Integer(DBConstants.BLANK_VALUE_INT)))
    			return "";
    		else
    			return a.toString();
   		} else if (cl==Long.class || cl==long.class) {
			if (a.equals(new Long(DBConstants.BLANK_VALUE_LONG)))
				return "";
			else
				return a.toString();
    	} else
    		return a.toString();
    }
    
    

    public int doStartTag() {
        try {
            lt = (LoopTag)findAncestorWithClass(this, LoopTag.class);
            if(lt == null) 
                throw new JspTagException("LoopItemTag outside LoopTag");

            String s;
            String methodName = Util.toTitleCase(field);
            try {
                
                if ("currentIndex".equals(field) ) {
                    initialize();
                    loadAttribute("currentIndex");
                    pageContext.getOut().print(String.valueOf(currentIndex));            
                    return(SKIP_BODY);                    
                }
                Object a = null;
                if (param==null){
                    a = this.getClass().getMethod("get" + methodName, null).invoke(this, null);
                } else {     
                    a = this.getClass().getMethod("get" + methodName, new Class[]{String.class}).invoke(this, new Object[]{param});
                }
                if (a==null) {
                    throw new NoSuchMethodException();
                }
                
            	s = translateItem(getStringValue(a));                
            }
            catch (NoSuchMethodException e) {
                Object o = lt.getCurrent();
                if(o == null)
                    throw e;
            	Object a = null;                    
                if (param==null){
                    a = o.getClass().getMethod("get" + methodName, null).invoke(o, null);
                } else {   
                    a = o.getClass().getMethod("get" + methodName, new Class[]{String.class}).invoke(o, new Object[]{param});
                }    
            	s = translateItem(getStringValue(a));
            }
            
            pageContext.getOut().print(s);
        	//defaultValue = "";            
            return(SKIP_BODY);
        }

        catch (Exception e) {
            e.printStackTrace();
            //It's not necessary to log this exception
            //Util.log("Error in LoopItemTag: " + e);
            //LogManager.getInstance().logEvent(new LogEvent(LogInfo.TYPE_DEBUG,"Error in LoopItemTag: " + e));  
        }

        
        return(SKIP_BODY);
    }
}
