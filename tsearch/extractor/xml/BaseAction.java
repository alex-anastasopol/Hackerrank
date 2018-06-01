package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;

public class BaseAction implements ArrayProcessor {
    
    protected XMLExtractor xmlExtractor;
    protected BaseAction parent;
    protected Element element;

    protected long searchId = -1;
    
    public BaseAction(BaseAction ba, Element el,long searchId) {
    	this.searchId = searchId;
        parent=ba;
        xmlExtractor=ba.xmlExtractor;
        element=el;
    }

    public BaseAction(XMLExtractor xmle, Element el,long searchId) {
        xmlExtractor=xmle;
        element=el;
        this.searchId = searchId;
    }

    protected Object retriveValue(Object ob) throws Exception {
        ob=initValue(ob);
        if (ob instanceof MultipleArray) {
            MultipleArray m=(MultipleArray)ob;
            m.process(this);
            return m;
        } else 
            return retriveSingleValue(ob);
    }

    protected Object initValue(Object ob) throws Exception {
        return ob;
    }

    public Object process(Object o) throws Exception {
        return retriveSingleValue(o);
    }

    protected Object retriveSingleValue(Object o) throws Exception {
        return o;
    }

    protected Element getInnerAction() throws Exception {
        try {
            return XMLUtils.getFirstElement(element);
        } catch (Exception e) {
            String name=this.getClass().toString();
            name=name.substring(name.lastIndexOf('.')+1, name.indexOf("Action")).toUpperCase();
            throw new XMLSyntaxRuleException("There is no inner element in this "+name);
        }
    }

    protected void initialize() throws Exception {;}

    protected Object process() throws Exception {
        initialize();
        BaseAction na=ActionFactory.createAction(this, getInnerAction());
        Object o=na.processException();
        return retriveValue(o);
    }

    public Object processException() throws Exception {
        try {
            return process();
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException(e, element);
        }
    }
}

