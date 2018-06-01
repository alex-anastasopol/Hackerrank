package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;

public class ModifyarrayAction extends BaseAction {
    protected MultipleArray ma;

    public ModifyarrayAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    public MultipleArray getArray() {
        return ma;
    }
    
    public void setArray(MultipleArray array) {
        ma = array;
    }    

    public Object process() throws Exception {
        NodeList nl=element.getChildNodes();
        Object o=ActionFactory.createAction(this, (Element)nl.item(0)).processException();
        if (!(o instanceof MultipleArray))
            throw new XMLSyntaxRuleException("First element must be an array in MODIFYARRAY");
        if (ma == null) {
        	ma=(MultipleArray)o;
        }
        for (int i=1; i<nl.getLength(); i++) 
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE) {
                ActionFactory.createAction(this, (Element)nl.item(i)).processException();
            }
        return ma;
    }
}
