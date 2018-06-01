package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;

public class ArrayAction extends BaseAction {

    protected boolean compose;
    protected String alt = null;
    public ArrayAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    protected void initialize() throws Exception {
        // compose
        compose=element.getAttribute("COMPOSE").equals("yes");
        // alt
        alt=element.getAttribute("ALTERNATE");
        alt=alt.length()>0?alt:null;
        if (alt != null && alt.equals("null"))
        	alt="";
    }

    public Object process() throws Exception {
        initialize();
        NodeList nl=element.getChildNodes();
        int dim=-1;
        List retl=new ArrayList();
        for (int i=0; i<nl.getLength(); i++) 
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE) {
            	Object o = null;
            	try {
            		o=ActionFactory.createAction(this, (Element)nl.item(i)).processException();
            	} catch (ParserException e){
            		if (e.getMessage().equals("ro.cst.tsearch.extractor.xml.XMLOptionalRuleException: FIND action have not found any node")
            			&& alt != null){
            			o = alt;
            		} else {
            			throw e;
            		}
            	}
                if (o instanceof MultipleArray) {
                    MultipleArray ma=(MultipleArray)o;
                    int maDim=compose?ma.dim-1:ma.dim;
                    if (dim==-1)
                        dim=maDim;
                    else if (dim!=maDim) 
                        throw new XMLSyntaxRuleException("Different dimentions : "+dim+" and "+maDim);
                    if (compose)
                        retl.addAll((List)ma.val);
                    else
                        retl.add(ma.val);
                } else {
                    if (!(o instanceof String))
                        throw new XMLSyntaxRuleException("Cannot apply ARRAY on smth else than strings or MultipleArays");
                    String s=(String)o;
                    if (dim==-1)
                        dim=0;
                    else if (dim!=0) 
                        throw new XMLSyntaxRuleException("Different dimentions : "+dim+" and 0");
                    retl.add(s);
                }
            }
        return new MultipleArray(retl, dim+1);
    }
}
