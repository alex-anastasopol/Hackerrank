
package ro.cst.tsearch.generic.tag;

//import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.generic.Util;

import java.util.*;
import javax.servlet.jsp.tagext.*;

import java.lang.reflect.*;

import org.apache.log4j.Logger;

public abstract class LoopTag extends GenericTag {

	private static final Logger logger = Logger.getLogger(LoopTag.class);
	
    protected Object[] objectList;
    protected int currentIdx;
    public Object[] getObjectList() { return objectList; }
    public Object getCurrent()      { return objectList[currentIdx]; }
    public String getCurrentIndex()    { return new Integer(currentIdx).toString(); }
    public boolean getIsFirstIndex()    { 
        return (currentIdx==0)?true:false;
    }
    public boolean getIsLastIndex()    { 
        return (currentIdx==objectList.length-1)?true:false;
    }

    public boolean getSameLikePrevious(String methodName)    { 
        if (getIsFirstIndex()) {
            return false;
        }       
        Object previous = objectList[currentIdx-1];
        Object current = objectList[currentIdx];
        return compare(methodName, previous, current) ;
    }

    public boolean getSameLikeNext(String methodName)    { 
        if (getIsLastIndex()) {
            return false;
        }       
        Object current = objectList[currentIdx];
        Object next = objectList[currentIdx+1];
        return compare(methodName, current, next) ;
    }


    private boolean compare(String methodName, Object obj1, Object obj2)    { 

        try {        
            logger.info("XXXXXXXXXXXXXXXXXXX methodName=" + methodName);
            String objValue1, objValue2;
            
            Method met = obj1.getClass().getMethod("get" + methodName, null);
            Class clMet = met.getReturnType();
            if ( 
                    (clMet==boolean.class) 
                    || (clMet==int.class)
                    || (clMet==long.class)
                    || (clMet==double.class)
                ) {
                
                objValue1 = String.valueOf(met.invoke(obj1, null));
            } else {                        
                objValue1 = (String)met.invoke(obj1, null);
            }                    
    
            met = obj2.getClass().getMethod("get" + methodName, null);
            clMet = met.getReturnType();
            if ( 
                    (clMet==boolean.class) 
                    || (clMet==int.class)
                    || (clMet==long.class)
                    || (clMet==double.class)
                ) {
                
                objValue2 = String.valueOf(met.invoke(obj2, null));
            } else {                        
                objValue2 = (String)met.invoke(obj2, null);
            }                    
			logger.info("XXXXXXXXXXXXXXXXXXX objValue1=" + objValue1);
			logger.info("XXXXXXXXXXXXXXXXXXX objValue2=" + objValue2);
            if (objValue2.equals(objValue1)) {
                return true;
            } 
        }                 
        catch (InvocationTargetException ex) {}
        catch (IllegalAccessException ex) {}
        catch (ClassCastException ex) {}
        catch (NoSuchMethodException ex) { }

        return false;
       
    }

    protected abstract Object[] createObjectList() throws Exception;

    protected void setCurrentValues()
        throws Exception {

        Object o = getCurrent();

        Enumeration e = getReplacementsName();
        while(e.hasMoreElements()) {
            String attr = (String)e.nextElement();
            String v = null;
            String methodName = Util.toTitleCase(attr);
            try {
                Object a = this.getClass().getMethod("get" + methodName, null).invoke(this, null);
                if (a==null) {
                    throw new NoSuchMethodException();
                }                                
                v = a.toString();
            }
            catch (NoSuchMethodException ex) { };

            if(v == null) {
                try {
                    v = o.getClass().getMethod("get" + methodName, null).invoke(o, null).toString();
                }
                catch (NoSuchMethodException ex) { };
            }

            if(v != null) setReplacement(attr, v);
        }
    }


    public int doStartTag() {
        try {
            initialize();

            currentIdx = 0;
            objectList = createObjectList();

            if(objectList == null)
                objectList = new Object[0];

            if(objectList.length == 0)
                return (SKIP_BODY);

            setCurrentValues();

            return(EVAL_BODY_BUFFERED);
            //return(EVAL_BODY_INCLUDE);
        }

        catch (Exception e) {
            e.printStackTrace();
			logger.error("Error in LoopTag.doStartTag: " + e);
        }

        return(SKIP_BODY);
    }


    public int doAfterBody() {
        try {

            BodyContent body = getBodyContent();
            doBodyReplacements(body);

            body.clearBody();

            currentIdx++;
            if(currentIdx >= objectList.length)
                return(SKIP_BODY);

            setCurrentValues();

            return (EVAL_BODY_AGAIN);
            //return(EVAL_BODY_BUFFERED);
        }

        catch (Exception e) {
            e.printStackTrace();
			logger.error("Error in LoopTag.doAfterBody: " + e);
        }

        return(SKIP_BODY);
    }

    public int doEndTag() {
        try {
            if(objectList.length == 0)
                return(SKIP_BODY);

            BodyContent body = getBodyContent();
            doBodyReplacements(body);
            body.clearBody();
        }

        catch (Exception e) {
            e.printStackTrace();
			logger.error("Error in LoopTag.doEndTag: " + e);
        }
        
        return (EVAL_PAGE);
    }
}
