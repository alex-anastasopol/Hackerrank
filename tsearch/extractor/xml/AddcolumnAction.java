package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.lang.reflect.*;
import java.util.*;

public class AddcolumnAction extends BaseAction {

    protected String name;
    protected boolean fill;  

    public AddcolumnAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    public void initialize() throws Exception {
        // name
        name=element.getAttribute("NAME");
        fill=element.getAttribute("FILL").equals("yes");
    }

    protected Object retriveValue(Object o) throws Exception {
    	MultipleArray m;
        ResultTable rt=((ModifytableAction)parent).getTable();
        if (!(o instanceof MultipleArray)){
        	if (!fill){
        		throw new XMLSyntaxRuleException("ADDCOLUMN can be applied only to arrays");
        	} else { //if fill is set then the column is filled with an unique value
        		int size = rt.getLength();
        		if (size == 0){ // if table to add column to contains only the header, and no data row, create an empty data row, so the add operation can be executed 
        			size = 1;
        			int headLen = rt.getHead().length;
        			String [][] a = new String[1][headLen];
        			for (int i=0; i<headLen; i++){
        				a[0][i] = "";
        			}
        			rt.setBody(a);
        		}
        		List l = new ArrayList();
        		for (int i=0; i<size; i++)
        			l.add(o);
        		m = new MultipleArray(l, 1);
        	}
        } else {
        	m=(MultipleArray)o;
        }
        if (m.dim!=1)
            throw new XMLSyntaxRuleException("ADDCOLUMN can be applied only to single dimension arrays");
        if (!(parent instanceof ModifytableAction))
            throw new XMLSyntaxRuleException("ADDCOLUMN's parent should be a MODIFYTABLE");

        List li=(List)m.val;
        String[] a=new String[li.size()];
        for (int i=0; i<a.length; i++) {
            if (!(li.get(i) instanceof String))
                throw new XMLSyntaxRuleException("The array's elements should be strings");
            a[i]=(String)li.get(i);
        }

        rt.addColumn(name, a);
        return null;
    }
}
