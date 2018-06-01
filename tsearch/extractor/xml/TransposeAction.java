package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;

public class TransposeAction extends BaseAction {

    protected boolean recursive;

    public TransposeAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    protected void initialize() throws Exception {
        // recursive
        recursive=element.getAttribute("RECURSIVE").equals("yes");
    }

    protected Object retriveValue(Object ob) throws Exception {
        //ob=initValue(ob);
        if (!(ob instanceof MultipleArray))
            throw new XMLSyntaxRuleException("TRANSPOSE can be applied only to arrays");
        MultipleArray m=(MultipleArray)ob;
        if (m.dim<2) 
            throw new XMLSyntaxRuleException("TRANSPOSE can be applied only to a matrix");
        if (m.dim>2 && !recursive)
            throw new XMLSyntaxRuleException("RECURSIVE is set to \"no\"");
        if (m.dim>2 && recursive) {
            m.process(this, 2);
            return m;
        } else {
            return retriveSingleValue(m);
        }
    }

    public Object process(Object o) throws Exception {
        return retriveSingleValue(o);
    }

    protected Object retriveSingleValue(Object o) throws Exception {
        MultipleArray m=((MultipleArray)o);
        m.transpose();
        return m;
    }
}
