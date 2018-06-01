package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;
import org.apache.log4j.Category;

public class TableAction extends BaseAction {
	
	protected static final Category logger= Category.getInstance(TableAction.class.getName());
    // axe
    public final static int HORIZONTAL  = 0;
    public final static int VERTICAL    = 1;
    // header
    public final static int FIRST       = 0;

    protected int axe, header;
    protected boolean oneRow;

    public TableAction(BaseAction ba, Element el,long searchId) {
        super(ba, el, searchId);
    }

    public void initialize() throws Exception {
        // axe
        String saxe=element.getAttribute("AXE");
        if (saxe.length()==0)
            axe=VERTICAL;
        else if (saxe.equals("horizontal"))
             axe=HORIZONTAL;
        else if (saxe.equals("vertical"))
             axe=VERTICAL;
        else
            throw new Exception("Unknown axe : "+saxe);
        // header
        String sheader=element.getAttribute("HEADER");
        if (sheader.length()==0)
            header=FIRST;
        else if (sheader.equals("first"))
            header=FIRST;
        else 
            throw new Exception("Unknown header : "+sheader);
        // oneRow
        oneRow=element.getAttribute("ONEROW").equals("yes");
    }

    protected Object retriveValue(Object ob) throws Exception {
        if (!(ob instanceof MultipleArray)) 
            throw new XMLSyntaxRuleException("Cannot apply TABLE on smth else than a MultipleArray");
        MultipleArray m=(MultipleArray)ob;
        if (!oneRow && m.dim!=2)
            throw new Exception("The array does not have 2 dimensions");
        ResultTable rt=new ResultTable();
        List l1=(List)m.val; 
        if (l1.size()==0) {
            rt.setHead(new String[0]);
            rt.setBody(new String[0][0]);
        } else {
            String[][] a=new String[l1.size()][((List)l1.get(0)).size()];
//logger.debug("a.len="+a.length);
            for (int i=0; i<l1.size(); i++) {
                List l2=(List)l1.get(i);
//logger.debug("list["+i+"].size="+l2.size());
                if (l2.size()!=a[0].length) {
                    throw new XMLOptionalRuleException("The table rows' length is not constant");
                }
                for (int j=0; j<l2.size(); j++){
                    if (!(l2.get(j) instanceof String))
                        throw new Exception("The elements should be strings");
                    a[i][j]=(String)l2.get(j);
                }
            }

            if (axe==HORIZONTAL)
                a=transpose(a);
            rt.setHead(a[0]);
            if (oneRow && a.length==1) {
                String[][] b=new String[1][a[0].length];
                Arrays.fill(b[0], "");
                rt.setBody(b);
            } else {
                String[][] b=new String[a.length-1][];
                for (int i=0; i<b.length; i++)
                    b[i]=a[i+1];
                rt.setBody(b);
            }
        }
        rt.setReadOnly();
        return rt;
    }

    protected String[][] transpose(String[][] v) {
        int n=v[0].length;
        String[][] r=new String[n][v.length];
        for (int i=0; i<v.length; i++)
            for (int j=0; j<n; j++)
                r[j][i]=v[i][j];
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
