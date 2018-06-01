package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;

public class CrtvalAction extends BaseAction {

    protected String name;

    public CrtvalAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    public void initialize() throws Exception {
        // name
        name=element.getAttribute("NAME");
    }

    public Object process() throws Exception {
        initialize();
        Object o=getIterator().getCurrentValue();
        if (o instanceof MultipleArray)
            o=((MultipleArray)o).duplicate();
        return o;
    }

    protected IteratorAction getIterator() throws Exception {
        BaseAction crt;
        for (crt=parent; crt!=null; crt=crt.parent)
            if (crt instanceof IteratorAction && (name.equals(((IteratorAction)crt).name)))
                break;
        if (crt==null)
            throw new XMLSyntaxRuleException("CRTVAL node should have a parent ITERATOR rule");
        return (IteratorAction)crt;
    }
}
