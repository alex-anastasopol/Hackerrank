package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;

public class SplittableAction extends BaseAction {
    // axe
    public final static int VERTICAL    = 0;
    public final static int HORIZONTAL  = 1;

    protected int axe, columns;

    public SplittableAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    public void initialize() throws Exception {
        // axe
        String saxe=element.getAttribute("AXE");
        if (saxe.length()==0)
            axe=VERTICAL;
        else if (saxe.equals("vertical"))
             axe=VERTICAL;
        else if (saxe.equals("horizontal"))
             axe=HORIZONTAL;
        else
            throw new Exception("Unknown axe : "+saxe);
        // columns
        String scol=element.getAttribute("COLUMNS");
        try {
            columns=Integer.parseInt(scol);
        } catch (NumberFormatException e) {
            throw new XMLSyntaxRuleException("COLUMNS attribute must be an int");
        }
        if (columns<=0)
            throw new XMLSyntaxRuleException("COLUMNS attribute must be positive");
    }

    public Object process() throws Exception {
        initialize();
        Element el=XMLUtils.getFirstElement(element);
        Object o=ActionFactory.createAction(this, XMLUtils.getFirstElement(element)).processException();
        if (!(o instanceof ResultTable))
            throw new XMLSyntaxRuleException("Cannot apply SPLITTABLE on smth else than a table");
        ResultTable a=(ResultTable)o;
        if (columns>a.getHead().length)
            throw new XMLRuleException("COLUMNS attribute must be less than actual number of columns");
        if (axe==HORIZONTAL)
            return ResultTable.splitHorizontal(a, columns);
        else // VERTICAL
            return ResultTable.splitVertical(a, columns);
    }

    public static void main (String args[]) throws Exception {
    }
}
