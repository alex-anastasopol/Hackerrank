package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;

public class UndefineAction extends BaseAction {

    protected String name;

    public UndefineAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }
    
    protected void initialize() throws Exception {
        // name
        name=element.getAttribute("NAME");
    }

    public Object process() throws Exception {
        initialize();
        xmlExtractor.getDefinitions().remove(name);
        return null;
    }
}
