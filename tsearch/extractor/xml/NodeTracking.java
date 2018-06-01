package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;

public class NodeTracking {
    public static final int CURRENT_NODE   = 0;
    public static final int PARENT         = 1;
    public static final int CHILDREN       = 2;
    public static final int ANCESTORS      = 3;
    public static final int DESCENDANTS    = 4;
    public static final int PREV_BROTHERS  = 5;
    public static final int NEXT_BROTHERS  = 6;
    public static final String scopes[]={".", "..", "/", "^", "//", "<", ">"};

    protected int scope;

    public NodeTracking(int dir) {
        scope=dir;
    }

    public NodeTracking(String dir) throws Exception {
        scope=parseScope(dir);
    }

    public void trackNodes(Node n, NodeProcessor np) throws Exception {
        switch (scope) {
            case CURRENT_NODE:
                filterNodeProcess(n, np);
                break;
            case PARENT:
                filterNodeProcess(n.getParentNode(), np);
                break;
            case CHILDREN:
                boolean gotoKids=true;
                if (n.getNodeType()==Node.ELEMENT_NODE) {
                    NamedNodeMap nnm=((Element)n).getAttributes();
                    for (int i=0; i<nnm.getLength(); i++)
                        if (!filterNodeProcess(nnm.item(i), np)) {
                            gotoKids=false;
                            break;
                        }
                }
                if (gotoKids) {
                    NodeList nl=n.getChildNodes();
                    for (int i=0; i<nl.getLength(); i++)
                        if (!filterNodeProcess(nl.item(i), np))
                            break;
                }
                break;
            case ANCESTORS:
                for (Node p=n.getParentNode(); p.getNodeType()!=Node.DOCUMENT_NODE; p=p.getParentNode())
                    if (!filterNodeProcess(p, np))
                        break;
                break;
            case DESCENDANTS:
                trackDescendants(n, np);
                break;
            case PREV_BROTHERS:
                for (Node p=n.getPreviousSibling(); p!=null; p=p.getPreviousSibling())
                    if (!filterNodeProcess(p, np))
                        break;
                break;
            case NEXT_BROTHERS:
                for (Node p=n.getNextSibling(); p!=null; p=p.getNextSibling()) {
                    if (!filterNodeProcess(p, np))
                        break;
                }
                break;
        }
    }

    protected boolean trackDescendants(Node n, NodeProcessor np) throws Exception {
        if (n.getNodeType()==Node.ELEMENT_NODE) {
            NamedNodeMap nnm=((Element)n).getAttributes();
            for (int i=0; i<nnm.getLength(); i++)
                if (!filterNodeProcess(nnm.item(i), np))
                    return false;
        }
        NodeList nl=n.getChildNodes();
        for (int i=0; i<nl.getLength(); i++) {
            Node c=nl.item(i);
            if (!filterNodeProcess(c, np))
                return false;
            if (!trackDescendants(c, np))
                return false;
        }
        return true;
    }
    
    protected boolean filterNodeProcess(Node n, NodeProcessor np) throws Exception {
        int type=n.getNodeType();
        if (type==Node.ELEMENT_NODE || 
            type==Node.TEXT_NODE || 
            type==Node.ATTRIBUTE_NODE)
            return np.nodeProcess(n);
        return true;
    }

    public static int parseScope(String s) throws Exception {
        if (s.startsWith(".."))
            return PARENT;
        if (s.startsWith("//"))
            return DESCENDANTS;
        if (s.startsWith("/"))
            return CHILDREN;
        if (s.startsWith("^"))
            return ANCESTORS;
        if (s.startsWith("."))
            return CURRENT_NODE;
        if (s.startsWith("<"))
            return PREV_BROTHERS;
        if (s.startsWith(">"))
            return NEXT_BROTHERS;
        throw new XMLSyntaxRuleException("Undefined scope : \""+s+"\"");
    }

}
