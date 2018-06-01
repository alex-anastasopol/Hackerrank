package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;

public class DefineAction extends BaseAction {

    protected String name;
    protected boolean optional;
    protected Map map;
    protected String alt;

    public DefineAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    protected void initialize() throws Exception {
        // name
        name=element.getAttribute("NAME");
        // optional
        String sopt=element.getAttribute("OPTIONAL");
        optional=sopt.equals("yes");
        // alt
        alt=element.getAttribute("ALTERNATE");
        alt=alt.length()>0?alt:null;
        if (alt != null && alt.equals("null"))
        	alt="";
        // map
        NodeList nl=element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++)
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE && nl.item(i).getNodeName().equals("MAP")) 
                map=XMLUtils.readMap((Element)nl.item(i));
    }

    protected Element getInnerAction() throws Exception {
        NodeList nl=element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++)
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE && !nl.item(i).getNodeName().equals("MAP"))
                return (Element)nl.item(i);
        throw new XMLSyntaxRuleException("There is no inner element in this DefineAction");
    }
    
    protected Object retriveValue(Object o) throws Exception {
        if (o instanceof ResultTable) {
            if (map==null)
                throw new XMLSyntaxRuleException("There should be a mapping for a table");
            ((ResultTable)o).setMap(map);
        }
        xmlExtractor.getDefinitions().put(name, o);
        return o;
    }

    protected Object process() throws Exception {
        initialize();
        BaseAction na=ActionFactory.createAction(this, getInnerAction());
        try {
            Object o=na.processException();
            return retriveValue(o);
        } catch (ParserException e) {
            if (e.getCause() instanceof XMLOptionalRuleException) {
                if (optional)
                    return null;
                if (alt!=null) 
                    return alt;
            }
            throw e;
        }
    }
}
