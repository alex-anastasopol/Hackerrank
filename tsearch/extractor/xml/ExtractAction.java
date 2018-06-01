package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;

public class ExtractAction extends BaseAction {
    
    public ExtractAction(XMLExtractor xmle, Element el,long searchId) {
        super(xmle, el,searchId);
    }

    public Object process() throws Exception {
        NodeList nl=element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++)
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE)
                ActionFactory.createAction(this, (Element)nl.item(i)).processException();
        return null;
    }
}


