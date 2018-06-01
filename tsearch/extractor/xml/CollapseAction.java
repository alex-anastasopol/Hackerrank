package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;

public class CollapseAction extends BaseAction {

    protected String separator;
    protected boolean pad;

    public CollapseAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    protected void initialize() throws Exception {
        // separator
        separator=element.getAttribute("SEPARATOR");
        if (separator.indexOf("\\n") != -1)
            separator=separator.replaceAll("\\\\n", "\n");
        // pad
        pad=element.getAttribute("PAD").equals("yes");        
    }
    
    private String collapseArray(MultipleArray a) throws Exception{
    	Object o;
        Iterator it=((List)a.val).iterator();
        StringBuffer ret=new StringBuffer();
        boolean add=false;
        while (it.hasNext()) {
            o=it.next();
            if (!(o instanceof String))
                throw new XMLSyntaxRuleException("Elements should be strings");
            if (add)
                ret.append(separator);
            String valToAppend = (String)o;
            valToAppend = pad?valToAppend:valToAppend.trim();
            ret.append(valToAppend);
            add=true;
        }
        return ret.toString();
    }

    public Object process() throws Exception {
        initialize();
        Element el=XMLUtils.getFirstElement(element);
        Object o=ActionFactory.createAction(this, XMLUtils.getFirstElement(element)).processException();
        if (!(o instanceof MultipleArray))
            throw new XMLSyntaxRuleException("Cannot apply COLLAPSE on smth else than a array");
        MultipleArray a=(MultipleArray)o;
        if (a.dim == 1)
        	return collapseArray(a);
        else if (a.dim == 2){
        	List l = new ArrayList();
        	Iterator it=((List)a.val).iterator();
            while (it.hasNext()) {
                MultipleArray ar = new MultipleArray(it.next(), 1);
                l.add(collapseArray(ar));
            }
            return new MultipleArray(l, 1);
            
        } else        	
            throw new XMLSyntaxRuleException("Number of dimensions should be 1");                       
    }
}
