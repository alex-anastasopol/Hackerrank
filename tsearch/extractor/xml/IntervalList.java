package ro.cst.tsearch.extractor.xml;

import java.util.BitSet;
import java.util.StringTokenizer;

import org.apache.log4j.Category;

public class IntervalList {

	protected static final Category logger= Category.getInstance(IntervalList.class.getName());
	
    protected BitSet items=new BitSet();
    protected int from=-1;

    public void setInterval(String s) throws XMLSyntaxRuleException {
        StringTokenizer st=new StringTokenizer(s, ",");
        while (st.hasMoreTokens()) {
            setItem(st.nextToken());
        }
    }

    protected void setItem(String s) throws XMLSyntaxRuleException {
        int dashIdx=s.indexOf("-");
        try {
            if (dashIdx>=0) {
                String s1=s.substring(0, dashIdx).trim();
                String s2=s.substring(dashIdx+1).trim();
                if (s1.length()==0)
                    items.set(0, Integer.parseInt(s2)+1);
                else if (s2.length()==0) 
                    from=Integer.parseInt(s1);
                else
                    items.set(Integer.parseInt(s1), Integer.parseInt(s2)+1);
            }
            else
                items.set(Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            throw new XMLSyntaxRuleException("Invalid interval : \""+s+"\"");
        } catch (IndexOutOfBoundsException e) {
            throw new XMLSyntaxRuleException("StartIndex is larger than EndIndex : \""+s+"\"");
        }
    }

    public boolean getItem(int i) {
        if (from!=-1 && i>=from)
            return true;
        else
            return items.get(i);
    }

    public static void main (String args[]) throws Exception {
        IntervalList l=new IntervalList();
        l.setInterval("-3, 6-, 5-7, 0");
        for (int i=0; i<10; i++)
            if (l.getItem(i))
                logger.info(""+i);
    }
}
