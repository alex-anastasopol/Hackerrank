package ro.cst.tsearch.generic.tag;

import java.util.*;
import javax.servlet.jsp.tagext.*;

import ro.cst.tsearch.generic.Util;

public class RequestParametersTag 
    extends GenericTag {
    
    private String defKeys = null;
    public void setDefKeys(String s) { defKeys = s; }
    public String getDefKeys() { return defKeys; }
    
    private String defValues = "";
    public void setDefValues(String s) { defValues = s; }
    public String getDefValues() { return defValues; }

    private String separator = ",";
    public void setSeparator(String s) { separator = s; }
    public String getSeparator() { return separator; }

    // this param is used to enable request attribute search
    private boolean reqAttribute = false;
    public void setReqAttribute(String reqAttribute) { 
        this.reqAttribute = ((reqAttribute.toUpperCase().equals("TRUE"))?true:false);
    }    
    public String getReqAttribute(){
        return ""+reqAttribute;
    }


    public static final String RECEIVED ="received_" ; 
    public static final String EMPTY    ="empty_" ; 
    
    public int doStartTag() {
        try { 
            initialize();
            
            if(defKeys != null && !defKeys.trim().equals("")) {
                String[] keys = Util.split(separator, defKeys);
                String[] values = Util.split(separator, defValues);
                for(int i=0; i<keys.length; i++) {
                    if(values.length > i) {
                        setReplacement(keys[i], values[i]);
                        setReplacement(RECEIVED+keys[i], "false");
                        setReplacement(EMPTY+keys[i], "true");
                    } else if(values.length <= 0) {
                        setReplacement(keys[i], "");
                        setReplacement(RECEIVED+keys[i], "false");
                        setReplacement(EMPTY+keys[i], "true");
                    } else {
                        setReplacement(keys[i], values[values.length-1]);
                        setReplacement(RECEIVED+keys[i], "false");
                        setReplacement(EMPTY+keys[i], "true"); 
                    }
                }
            }

            Enumeration e = req.getParameterNames();
            while(e.hasMoreElements()) {
                String param = (String) e.nextElement();
                String[] values = req.getParameterValues(param);
                if(values.length > 0){
                    setReplacement(param, values[0]);
                    setReplacement(RECEIVED+param, "true");
                    if ( (values[0]!=null) && (values[0].length()>0) ) {
                        setReplacement(EMPTY+param, "false");
                    }                        

                }
                for(int i=0; i<values.length; i++) {
                    setReplacement(param+i, values[i]);
                    setReplacement(RECEIVED+param+i, "true");
                    if ( (values[i]!=null) && (values[i].length()>0) ) {
                        setReplacement(EMPTY+param+i, "false");
                    }                        
                    
                }
            }            
            if (reqAttribute) {
                e = req.getAttributeNames();
                while(e.hasMoreElements()){
                    String param = (String) e.nextElement();
                    
                    /*
                     * HTTPS bugfix
                     * HTTPS requests have extra attributes that are not all Strings
                     */
                    Object paramValue = req.getAttribute( param );

                    if( ! (paramValue instanceof String) ){
                    	continue;
                    }
                    
                    String value = (String) paramValue;
                    setReplacement(param, value);
                    setReplacement(RECEIVED+param, "true");
                    if ( (value!=null) && (value.length()>0) ) {
                        setReplacement(EMPTY+param, "false");
                    }                        
                }
            }
            return(EVAL_BODY_BUFFERED);
        }

        catch (Exception e) {
            Util.log("Error in CurrentItem tag: " + e);
            e.printStackTrace();
        }
        
        
        return(SKIP_BODY);
    }

    public int doAfterBody() {
        try {
            BodyContent body = getBodyContent();
            doBodyReplacements(body);
            return(SKIP_BODY);
        }

        catch (Exception e) {
            Util.log("Error in RequestParametersTag.doAfterBody: " + e);
        }

        return(SKIP_BODY);
    }
}
