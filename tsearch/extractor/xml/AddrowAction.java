package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.lang.reflect.*;
import java.util.*;

public class AddrowAction extends BaseAction {
    protected int position;

    public AddrowAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    public void initialize() throws Exception {
        // position
        String spos=element.getAttribute("POSITION");
        if (spos.length()==0 || spos.equals("last"))
            position=MultipleArray.ADD_LAST;
        else if (spos.equals("first"))
            position=MultipleArray.ADD_FIRST;
    }

    protected Object retriveValue(Object o) throws Exception {
    	MultipleArray m;
        if (!(o instanceof MultipleArray)) {
        	m = new MultipleArray(o, 0);
//            throw new XMLSyntaxRuleException("ADDROW can be applied only to arrays");
        }
        else {
        	m=(MultipleArray)o;
        }
//        if (m.dim<1)
//            throw new XMLSyntaxRuleException("ADDROW can be applied only to arrays");
        
        BaseAction crt;
        for (crt=parent; crt!=null; crt=crt.parent)
            if (crt instanceof ModifyarrayAction)
                break;
        if (crt==null)
            throw new XMLSyntaxRuleException("ADDROW's parent should be a MODIFYARRAY");       
        
        //if (!(parent instanceof ModifyarrayAction))
        //    throw new XMLSyntaxRuleException("ADDROW's parent should be a MODIFYARRAY");
        MultipleArray pm=((ModifyarrayAction)crt).getArray();
        if (pm == null) {
        	//pm = (MultipleArray)m.duplicate();
        	pm = new MultipleArray();
        	pm.dim = m.dim+1;
        	pm.addRow(m, position);
        	//pm = new MultipleArray(m.val, m.dim+1);
        	((ModifyarrayAction)crt).setArray(pm);
        } else {
	        if (pm.dim-1!=m.dim)
	            throw new XMLSyntaxRuleException("Incompatible dimentions, parent: "+pm.dim+" child: "+m.dim);
	        pm.addRow(m, position);
        }
        return new Boolean(true);
    }
}
