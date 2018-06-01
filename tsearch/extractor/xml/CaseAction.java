package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;

public class CaseAction extends BaseAction {
    protected String cond;

    public CaseAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    protected void initialize() throws Exception {
        // cond
        cond=element.getAttribute("COND");
    }

    protected Object process() throws Exception {
        initialize();
        Object op=getSwitch().getOp();
        boolean b=SearchExpression.evalExpression(cond, op, xmlExtractor.def);
        if (b) {
            NodeList nl=element.getChildNodes();
            Object ret = null;
            for (int i=0; i<nl.getLength(); i++) 
                if (nl.item(i).getNodeType()==Node.ELEMENT_NODE) {
                    ret = ActionFactory.createAction(this, (Element)nl.item(i)).processException();                   
                }
            if (ret == null){
            	return new Boolean(true);
            } else {
            	return ret;
            }
        }
        return null;
    }

    protected SwitchAction getSwitch() throws Exception {
        BaseAction crt;
        for (crt=parent; crt!=null; crt=crt.parent)
            if (crt instanceof SwitchAction)
                break;
        if (crt==null)
            throw new XMLSyntaxRuleException("CASE node should have a parent SWITCH rule");
        return (SwitchAction)crt;
    }
}

    
