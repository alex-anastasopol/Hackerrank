package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;

public class SplitAction extends BaseAction {
    protected String separator;
    protected boolean pad;

    public SplitAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    public void initialize() throws Exception {
        // separator
        separator=element.getAttribute("SEPARATOR");
        if (separator.length()==0)
            throw new XMLSyntaxRuleException("Invalid SEPARATOR attribute of node SPLIT : \"\"");
        // pad
        pad=element.getAttribute("PAD").equals("yes");
    }

    protected Object retriveSingleValue(Object o) throws Exception {
        if (!(o instanceof String))
            throw new XMLSyntaxRuleException("SPLIT can be applied only to strings : "+o.getClass());
        //return new MultipleArray(Arrays.asList(((String)o).split("\\s*"+separator+"\\s*")), 1);
        String s=(String)o;
        if (s.length()==0)
            return new MultipleArray(new ArrayList(0), 1);
        String[] v=s.split((!pad?"\\s*":"")+separator+(!pad?"\\s*":""));
        ArrayList al=new ArrayList();
        for (int i=0; i<v.length; i++)
            al.add(v[i]);
        return new MultipleArray(al, 1);
    }
}

