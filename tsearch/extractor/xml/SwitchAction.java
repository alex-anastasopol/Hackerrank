package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;

public class SwitchAction extends BaseAction {
    protected Object op;
    protected boolean found=false;

    public SwitchAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    public Object getOp() {
        return op;
    }

    public Object process() throws Exception {
        NodeList nl=element.getChildNodes();
        op=ActionFactory.createAction(this, (Element)nl.item(0)).processException();
        for (int i=1; i<nl.getLength(); i++) 
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE) {
                Object o=ActionFactory.createAction(this, (Element)nl.item(i)).processException();
                if (o!=null)
                    return o;
            }
        return null;
    }
}
