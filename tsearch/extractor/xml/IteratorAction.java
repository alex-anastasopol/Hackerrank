package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;

public class IteratorAction extends BaseAction {

    protected String name;
    protected Object crtVal;
    protected int card;
    protected BaseAction op;

    public IteratorAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    public Object getCurrentValue() {
        return crtVal;
    }

    public void initialize() throws Exception {
        // name
        name=element.getAttribute("NAME");
        // item
        String item=element.getAttribute("ITEM");
        if (item.length()==0)
             card=0; // default single
        else if (item.equals("single"))
             card=0;
        else if (item.equals("array"))
             card=1;
        else if (item.equals("matrix"))
             card=2;
        else if (item.equals("cube"))
             card=3;
        else if (item.equals("hypercube"))
            throw new XMLSyntaxRuleException("ARE YOU NUTS ?! HYPERCUBE ?!? WERE DO YOU THINK YOU ARE, STARTREK ?!? ");
        else
            throw new Exception("Unknown item : "+item);
    }

    protected Object process() throws Exception {
        initialize();
        BaseAction na=ActionFactory.createAction(this, getInnerAction());
        op=ActionFactory.createAction(this, getOperatorAction());
        Object o=na.processException();
        if (!(o instanceof MultipleArray))
            throw new XMLSyntaxRuleException("The inner node should result in a MultipleArray");
        MultipleArray m=(MultipleArray)o;
        if (m.dim<1) 
            throw new XMLSyntaxRuleException("The inner node should result in a MultipleArray");
        
        m.process(this, card);
        return m;
    }

    public Object process(Object o) throws Exception {
        crtVal=o;
        return op.processException();
    }
/*
    protected Object process() throws Exception {
        initialize();
        BaseAction na=ActionFactory.createAction(this, getInnerAction());
        BaseAction nf=ActionFactory.createAction(this, getOperatorAction());
        Object o=na.processException();
        if (!(o instanceof MultipleArray))
            throw new XMLSyntaxRuleException("The inner node should result in a MultipleArray");
        MultipleArray m=(MultipleArray)o;
        if (m.dim<2) 
            throw new XMLSyntaxRuleException("The inner node should result in a MultipleArray");
        List ret=new ArrayList();
        Iterator it=((List)m.val).iterator();
        while (it.hasNext()) {
            crtVal=it.next();
            ret.add(nf.processException());
        }

        return new MultipleArray(ret, 1);
    }
*/

    protected Element getOperatorAction() throws Exception {
        try {
            return XMLUtils.getSecondElement(element);
        } catch (Exception e) {
            String name=this.getClass().toString();
            name=name.substring(name.lastIndexOf('.')+1, name.indexOf("Action")).toUpperCase();
            throw new XMLSyntaxRuleException("There is no second inner element in this "+name);
        }
    }
}
