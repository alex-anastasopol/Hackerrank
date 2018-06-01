package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;

public class ModifytableAction extends BaseAction {
    protected ResultTable resulTable;

    public ModifytableAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    public ResultTable getTable() {
        return resulTable;
    }

    public Object process() throws Exception {
        NodeList nl=element.getChildNodes();
        Object o=ActionFactory.createAction(this, (Element)nl.item(0)).processException();
        if (!(o instanceof ResultTable))
            throw new XMLSyntaxRuleException("First element must be a table in MODIFYTABLE");
        resulTable=(ResultTable)o;
        resulTable.setReadWrite();
        for (int i=1; i<nl.getLength(); i++) 
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE) {
                ActionFactory.createAction(this, (Element)nl.item(i)).processException();
            }
        resulTable.setReadOnly();
        return resulTable;
    }
}
