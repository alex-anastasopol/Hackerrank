package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;

public class DefaultAction extends BaseAction {
    
    public DefaultAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    protected Object process() throws Exception {
        initialize();
        Object ret = null; 
        NodeList nl=element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++) 
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE) {
                ret = ActionFactory.createAction(this, (Element)nl.item(i)).processException();
            }
        return ret;
    }
}

    
