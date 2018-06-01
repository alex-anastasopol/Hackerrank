
package ro.cst.tsearch.generic.tag;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.Tag;

import com.stewart.ats.generic.filter.SecurityRequestWrapper;

import ro.cst.tsearch.generic.AttributeNotFoundException;
import ro.cst.tsearch.generic.AttributeParser;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.utils.ParameterNotFoundException;
import ro.cst.tsearch.utils.ParameterParser;


public class GenericTag
    extends BodyTagSupport {

    public static final String PARENT_TAG_DELIM  = "parenttag";
    public static final String LOOP_TAG_DELIM    = "looptag";
    public static final String REQUEST_DELIM     = "request";
    public static final String SESSION_DELIM     = "session";
    
    protected String separator = ",";
    public void setSeparator(String s) { separator = s; }
    public String getSeparator() { return separator; }
    
    //the default is : parent,loop,request,session
    protected String loadAttributeFrom = PARENT_TAG_DELIM 
                                        + separator 
                                        + LOOP_TAG_DELIM
                                        + separator 
                                        + REQUEST_DELIM
                                        + separator 
                                        + SESSION_DELIM ;
                                        
    public void setLoadAttributeFrom(String s) { loadAttributeFrom = s; }
    public String getLoadAttributeFrom() { return loadAttributeFrom; }
        
    protected ParameterParser pp;
    public ParameterParser getParameterParser() { return pp; }
    public void setParameterParser(ParameterParser pp) { this.pp = pp; }

    protected AttributeParser ap;
    public AttributeParser getAttributeParser() { return ap; }
    public void setAttributeParser(AttributeParser ap) { this.ap = ap; }

    protected HttpServletRequest req;
    public HttpServletRequest getRequest() { return req; };
    public void setRequest(HttpServletRequest req) { this.req = req; };

    protected HttpSession ses;
    public HttpSession getSession() { return ses; };
    public void setSession(HttpSession ses) { this.ses = ses; };

    private Hashtable replacements = new Hashtable();
    public Enumeration getReplacementsName() { return replacements.keys(); }
    public void setReplacement(String name, String value) {
        replacements.put(name, value);
    }
    public void setReplacement(Hashtable ht) {
        replacements = ht;
    }
    public String getReplacement(String name) {
        return (String)replacements.get(name);
    }


    public String getReplacementDelimiter() { return "@@"; };


    public void initialize(){
        req = new SecurityRequestWrapper((HttpServletRequest)pageContext.getRequest());
        ses = pageContext.getSession();
        pp = new ParameterParser(req);
        ap = new AttributeParser(ses);
    }
 
    /**
     * load in given attribute
     * search  in :
     *      parent tags 
     *      request
     *      session
     * @param name is the name of the method to be invoked
     * Note: use this method when you expect to find something 
     */
     
    public void loadAttribute(String name) throws Exception {
        loadAttribute(name, null);                            
    }

	/**
	  * load in given attribute
	  * search  in :
	  *      parent tags
	  *      request
	  *      session
	  * @param name is the name of the method to be invoked
	  * Note: use this method when you expect to find something
	  */

	 public void loadAttribute(String name, String param) throws Exception {

		 //the default is : parent,loop,request,session
		 String[] values = Util.split(separator, loadAttributeFrom);

		 boolean found = false;
		 for(int i=0;((i<values.length) && (!found)); i++) {
			 if (PARENT_TAG_DELIM.equals(values[i])) {
				 found = loadAttributeFromParentTag(name, param);
			 } else if (LOOP_TAG_DELIM.equals(values[i])) {
				 found = loadAttributeFromLoopTag(name, param);
			 } else if (REQUEST_DELIM.equals(values[i])) {
				 found = loadAttributeFromRequest(name);
			 } else if (SESSION_DELIM.equals(values[i])) {
				 found = loadAttributeFromSession(name);
			 } else {
				 //nothing
			 }
		 }

	 }


    public void doBodyReplacements(BodyContent body)
        throws Exception {
        //Util.log("GenericTag: body=" + body );            
        //Util.log("GenericTag: body.getString()=" + body.getString() );
        //Util.log("GenericTag: replacements=" + replacements );
        body.getEnclosingWriter().print(Util.replaceTags(getReplacementDelimiter(),
                                                         body.getString(),
                                                         replacements));

    }
    
    private boolean loadAttributeFromParentTag(String name) {
    	return loadAttributeFromParentTag(name, null);
    }
    
    private boolean loadAttributeFromParentTag(String name, String param) {

        String v = null;
        String methodName = Util.toTitleCase(name);

        //try in parent tags
        Tag parent = getParent();
        while(parent != null) {
            try {
                Method met;
                if (param != null)
                	met = parent.getClass().getMethod("get" + methodName, new Class[]{String.class});
                else
            		met = parent.getClass().getMethod("get" + methodName, null);
                Class clMet = met.getReturnType();
                if ( 
                        (clMet==boolean.class) 
                        || (clMet==int.class)
                        || (clMet==long.class)
                        || (clMet==double.class)
                    ) {
                    	if (param != null)
                    		v = String.valueOf(met.invoke(parent, new Object[]{param}));
                    	else
                        	v = String.valueOf(met.invoke(parent, null));
                } else {                        
                    //v = (String)parent.getClass().getMethod("get" + methodName, null).invoke(parent, null);
                    if (param != null)
                    	v = (String)met.invoke(parent, new Object[]{param});
                    else
                    	v = (String)met.invoke(parent, null);
                }                    
                if(v != null){
                    this.getClass().getMethod("set" + methodName,
                        new Class[]{Class.forName("java.lang.String")}).invoke(this, new Object[]{v});
                    return true;
                }

            }
            catch (NoSuchMethodException ex) {  }
            catch (ClassNotFoundException ex) {  }            
            catch (IllegalAccessException ex) {  }
            catch (InvocationTargetException ex) {  }
            

            parent = parent.getParent();
        }
        return false;
    }
    
    private boolean loadAttributeFromLoopTag(String name) {
    	return loadAttributeFromLoopTag(name, null);
    }

    private boolean loadAttributeFromLoopTag(String name, String param) {

        String v = null;
        String methodName = Util.toTitleCase(name);

        //try on loop tag if exists
        try {
            LoopTag lt = (LoopTag)findAncestorWithClass(this, LoopTag.class);
            if (lt!=null) {
                Object o = lt.getCurrent();                
                if(o != null) {
                    Method met;
                    if (param != null)                	
                    	met = o.getClass().getMethod("get" + methodName, new Class[]{String.class});
                    else
                		met = o.getClass().getMethod("get" + methodName, null);
                    Class clMet = met.getReturnType();
                    if ( 
                            (clMet==boolean.class) 
                            || (clMet==int.class)
                            || (clMet==long.class)
                            || (clMet==double.class)
                        ) {
                        	if (param != null)
                        		v = String.valueOf(met.invoke(o, new Object[]{param}));
                            else                        		
                        		v = String.valueOf(met.invoke(o, null));
                    } else {  
                    	if (param != null)                      
                        	v = (String)met.invoke(o, new Object[]{param});
                        else
                        	v = (String)met.invoke(o, null);
                    }                    
                    if(v != null){
                        this.getClass().getMethod("set" + methodName,
                            new Class[]{Class.forName("java.lang.String")}).invoke(this, new Object[]{v});
                        return true;
                    }

                }                            
            }
        } catch (NoSuchMethodException ex) {  
        } catch (ClassNotFoundException ex) {  
        } catch (IllegalAccessException ex) {  
        } catch (InvocationTargetException ex) {  }

        return false;        
    }    

    private boolean loadAttributeFromRequest(String name) {

        String v = null;
        String methodName = Util.toTitleCase(name);

        //try in request
        try {
            // try to get the parameter from the attributes first
            v = (String)req.getAttribute(name);
            //try to get from request the attribute with
            //the given name
            if (v==null) {
                v = pp.getMultipleStringParameter(name);
            }
            if(v != null){
                this.getClass().getMethod("set" + methodName,
                    new Class[]{Class.forName("java.lang.String")}).invoke(this, new Object[]{v});
                return true;
            }
        }
        catch (ParameterNotFoundException ex) {}
        catch (NoSuchMethodException ex) { }
        catch (ClassNotFoundException ex) { }        
        catch (IllegalAccessException ex) {  }
        catch (InvocationTargetException ex) {  }

        return false;
    }
    
    private boolean loadAttributeFromSession(String name) {

        String v = null;
        String methodName = Util.toTitleCase(name);

        //try in session
        try {
            v = ap.getStringAttribute(name);
            if(v != null){
                this.getClass().getMethod("set" + methodName,
                    new Class[]{Class.forName("java.lang.String")}).invoke(this, new Object[]{v});
                return true;
            }
        }
        catch (AttributeNotFoundException ex) {}
        catch (ClassCastException ex) {}
        catch (NoSuchMethodException ex) { }
        catch (ClassNotFoundException ex) { }
        catch (IllegalAccessException ex) {  }
        catch (InvocationTargetException ex) {  }
        
        return false;
    }
    
}
