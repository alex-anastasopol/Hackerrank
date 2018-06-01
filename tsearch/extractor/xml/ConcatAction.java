package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;

public class ConcatAction extends BaseAction {

	protected String defaultValue;
	
    public ConcatAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }
    
    protected void initialize() throws Exception {
        // defaultValue
    	defaultValue = element.getAttribute("DEFAULT");
        if (defaultValue != null && defaultValue.equals("null"))
        	defaultValue = "";
    }

    public Object process() throws Exception {
    	initialize();
        StringBuffer sb=new StringBuffer();
        NodeList nl=element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++) 
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE) {
            	Object o = null;
            	try{
            		o = ActionFactory.createAction(this, (Element)nl.item(i)).processException();
            	} catch (ParserException e) {
                    if (e.getCause() instanceof XMLOptionalRuleException) {
                    	o = defaultValue;
                    }
            	}
            	if (!(o instanceof String))
                    throw new XMLSyntaxRuleException("Cannot apply CONCAT on smth else than a string");
                sb.append((String)o);
            }
        return sb.toString();
    }
}
