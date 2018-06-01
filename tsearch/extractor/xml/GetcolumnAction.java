package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;

public class GetcolumnAction extends BaseAction {

    protected String name, param;

    public GetcolumnAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    public void initialize() throws Exception {
        // name
        name=element.getAttribute("NAME");
        // param
        param=element.getAttribute("PARAM");
    }

    public Object process() throws Exception {
        initialize();
        BaseAction modifyTableAction;
        for (modifyTableAction=parent; modifyTableAction!=null && !(modifyTableAction instanceof ModifytableAction); modifyTableAction=modifyTableAction.parent);
        if (modifyTableAction==null)
            throw new XMLSyntaxRuleException("GETCOLUMN should have a MODIFYTABLE ancestor");
        return new MultipleArray(Arrays.asList(((ModifytableAction)modifyTableAction).getTable().getColumn(name, param)), 1);
    }
}
