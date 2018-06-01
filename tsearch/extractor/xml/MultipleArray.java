package ro.cst.tsearch.extractor.xml;

import java.util.*;
import org.w3c.dom.*;
import org.apache.log4j.Category;
public class MultipleArray {
	
	protected static final Category logger= Category.getInstance(MultipleArray.class.getName());
	
    public static final int ADD_FIRST = 0;
    public static final int ADD_LAST = -1;

    protected Object val;
    protected int dim=0;
    protected boolean addArray=false;

    protected int retDim, card;
    
    protected MultipleArray() {
    }

    protected MultipleArray(Object o, int d) {
        val=o;
        dim=d;
    }

    public MultipleArray(Object o) throws Exception {
        if (o==null)
            throw new Exception("Initialization with null value");
        val=o;
    }

    public Object duplicate() throws Exception { // clone
    	MultipleArray ret=new MultipleArray(null, dim);
        ret.addArray=addArray;
        ret.val=deepCopy(val);
        return ret;
    }

    protected Object deepCopy(Object src) throws Exception {
        if (src instanceof List) {
            List lsrc=(List)src;
            List ret=new ArrayList(lsrc.size());
            Iterator it=lsrc.iterator();
            while (it.hasNext()) {
                ret.add(deepCopy(it.next()));
            }
            return ret;
        } else if (src instanceof Node ||
                   src instanceof String) {
            return src;
        } else {
            throw new Exception("Clone for "+src.getClass()+" not supported");
        }
    }

    public void setAddArray() {
        addArray=true;
    }

    public String toString() {
        StringBuffer sb=new StringBuffer();
        sb.append("dim : ");
        sb.append(dim);
        sb.append('\n');
        sb.append(val);
        sb.append('\n');
        return sb.toString();
    }

    public void process(ArrayProcessor ap) throws Exception {
        process(ap, 0);
    }

    public void process(ArrayProcessor ap, int card) throws Exception {
        retDim=0;
        this.card=card;
//logger.info("dim="+dim+", card="+card);
        val=process(ap, val, dim);
//logger.info("dim="+dim+", card="+card+", retDim="+retDim);
        dim=dim-card+retDim;
    }

    protected Object process(ArrayProcessor ap, Object v, int level) throws Exception {
//logger.info("level="+level+", card="+card);
        if (level==card) {
            if (card==0) {
//logger.info("class : "+v.getClass());
                Object o=ap.process(v);
                if (o==null)
                    throw new Exception("Null added object");
                if (o instanceof MultipleArray) {
                    MultipleArray m=(MultipleArray)o;
                    if (retDim==0)
                        retDim=m.dim;
                    else if (m.dim!=retDim) 
                        throw new Exception("Different dimentions : "+retDim+" and "+m.dim);
                    return m.val;
                }
                return o;
            } else {
                Object o=ap.process(new MultipleArray(v, card));
                if (o==null)
                    throw new Exception("Null added object");
                if (o instanceof MultipleArray) {
                    MultipleArray m=(MultipleArray)o;
                    if (retDim==0)
                        retDim=m.dim;
                    else if (m.dim!=retDim) 
                        throw new Exception("Different dimentions : "+retDim+" and "+m.dim);
                	return m.val;
                }
                return o;
            }
        } else {
            List lv=(List)v;
            for (int i=0; i<lv.size(); i++) {
                Object o=process(ap, lv.get(i), level-1);
                if (o instanceof MultipleArray)
                    o=((MultipleArray)o).val;
                lv.set(i, o);
            }
            return v;
        }
    }

    public void transpose() throws Exception {
        if (dim!=2)
            throw new Exception("The array should have 2 dimentions");
        List a=(List)val;
        if (a.size()==0)
            return;
        int n=((List)a.get(0)).size();
        List r=new ArrayList();
        for (int i=0; i<n; i++) {
            List r2=new ArrayList();
            for (int j=0; j<a.size(); j++)
                r2.add(((List)a.get(j)).get(i));
            r.add(r2);
        }
        val=r;
    }

    public void addRow(MultipleArray m, int pos) throws Exception {
        if (dim-1!=m.dim)
            throw new XMLSyntaxRuleException("Incompatible dimentions, parent: "+dim+" child: "+m.dim);
//        if (m.dim<1)
//            throw new XMLSyntaxRuleException("The added row should be an array");
        if (val != null) {
	        List l=(List)val;
	        if (pos==ADD_LAST)
	            l.add(m.val);
	        else
	            l.add(pos, m.val);
        } else {
        	List l = new ArrayList();
        	l.add(m.val);
        	val = l;
        }
    }

    public static void main (String args[]) throws Exception {

        MultipleArray a=new MultipleArray(Arrays.asList(new List[]{
                                          Arrays.asList(new String[]{"a", "b", "c"}), 
                                          Arrays.asList(new String[]{"1", "2", "3"})}),2);
        logger.info(a);
        a.transpose();
        logger.info(a);
    }
}
