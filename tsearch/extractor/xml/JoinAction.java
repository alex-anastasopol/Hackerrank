package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;

import org.apache.log4j.Category;

public class JoinAction extends BaseAction {
	
	protected static final Category logger= Category.getInstance(JoinAction.class.getName());
    // axe
    public final static int HORIZONTAL  = 0;
    public final static int FULLH       = 1;
    public final static int VERTICAL    = 2;

    protected int axe;
    protected boolean fill;

    public JoinAction(BaseAction ba, Element el,long searchId) {
        super(ba, el, searchId);
    }

    public void initialize() throws Exception {
        // axe
        String saxe=element.getAttribute("AXE");
        if (saxe.length()==0)
            axe=FULLH;
        else if (saxe.equals("horizontal"))
             axe=HORIZONTAL;
        else if (saxe.equals("fullH"))
             axe=FULLH;
        else if (saxe.equals("vertical"))
             axe=VERTICAL;
        else
            throw new Exception("Unknown axe : "+saxe);

        // fill
        fill=element.getAttribute("FILL").equals("yes");
        //logger.info("fill = "+fill);
    }

    public Object process() throws Exception {
        initialize();
        List list=new ArrayList();
        NodeList nl=element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++) 
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE) {
            	Object o = null;
            	try{
            		o = ActionFactory.createAction(this, (Element)nl.item(i)).processException();
            	} catch (ParserException e) {
            		if (e.getCause() instanceof XMLOptionalRuleException) {
            			continue;
            		} else {
            			throw e;
            		}            		
            	}
            	if (o == null)
            		continue;
                if (!(o instanceof ResultTable) && !(o instanceof ResultTable[]) && !(o instanceof MultipleArray))
//                if (!(o instanceof ResultTable) && !(o.getClass().isArray() && o.getClass().getComponentType()))
                    throw new XMLSyntaxRuleException("Cannot apply JOIN on smth else than a table");
                if (o instanceof ResultTable[]) {
                    ResultTable[] a=(ResultTable[])o;
                    for (int j=0; j<a.length; j++)
                        list.add(a[j]);
                } if (o instanceof MultipleArray) {
                	List a=(ArrayList)((MultipleArray)o).val;
                	Iterator it=a.iterator();
                	if (it.hasNext()){
                		Object tbl = it.next();
                		if(!(tbl instanceof ResultTable))
                			throw new XMLSyntaxRuleException("Cannot apply JOIN on smth else than a table");
                		list.add(tbl);
                	}
                    while (it.hasNext()) {
                        list.add(it.next());
                    }
                } else
                    list.add(o);
            }
        if (list.size()<1)
            //throw new XMLSyntaxRuleException("Cannot apply JOIN on less than 2 tables");
        	return null;
        Iterator it=list.iterator();
        ResultTable r=(ResultTable)it.next();
        while (it.hasNext())
            if (axe==FULLH) {
                r=ResultTable.joinHorizontalFull(r, (ResultTable)it.next());
            } else if (axe==HORIZONTAL) {
                r=ResultTable.joinHorizontal(r, (ResultTable)it.next());
            } else if (axe==VERTICAL) {
                r=ResultTable.joinVertical(r, (ResultTable)it.next(), fill);
            }
        return r;
    }

    public static void main (String args[]) throws Exception {
        String[][] v={{"1", "2"}, {"3", "4"}, {"5", "6"}};
        //v=(String[][])subArray(v, 1, v.length);
        for (int i=0; i<v.length; i++) {
            for (int j=0; j<v[i].length; j++)
                logger.info(v[i][j]+" ");
            logger.info("");
        }
    }
}
