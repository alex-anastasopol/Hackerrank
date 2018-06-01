package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;

public class ConstAction extends BaseAction {

    public ConstAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    public Object process() throws Exception {
        Node n=element.getFirstChild();
        return n==null?" ":n.getNodeValue();
    }
}
