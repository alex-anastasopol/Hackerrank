package ro.cst.tsearch.extractor.xml;

import java.lang.reflect.*;
import org.w3c.dom.*;

public class GenericAction extends BaseAction {

    protected String name;

    public GenericAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    protected void initialize() throws Exception {
        // name
        name=element.getAttribute("NAME");
    }

    public Object process() throws Exception {
        initialize();
        Method method;
        try {
            method=Class.forName("ro.cst.tsearch.extractor.xml.GenericFunctions").getMethod(name, new Class[]{ResultMap.class,long.class});
        } catch (NoSuchMethodException e) {
            throw new XMLSyntaxRuleException("Function \""+name+"\" not found");
        }
        method.invoke(null, new Object[]{xmlExtractor.def,new Long(searchId)});
        return null;
    }
}
