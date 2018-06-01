package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;

public class NodeAction extends BaseAction {

    protected String name;
    protected boolean optional;

    public NodeAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    protected void initialize() throws Exception {
        // name
        name=element.getAttribute("NAME");
        // optional
        optional=element.getAttribute("OPTIONAL").equals("yes");
    }

    public Object process() throws Exception {
        initialize();
        if (name.equals("root"))
            return xmlExtractor.getParseDocument().getDocumentElement();
        else {
            Object o=xmlExtractor.getDefinitions().get(name);
            if (o instanceof MultipleArray)
                o=((MultipleArray)o).duplicate();
            if (o==null)
                if (optional)
                    throw new XMLOptionalRuleException("Undefined node "+name);
                else
                    throw new XMLRuleException("Undefined node "+name);
            return o;
        }
    }
}
