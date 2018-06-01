package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.*;
import org.apache.log4j.Category;

public class FindAction extends BaseAction implements NodeProcessor {
	protected static final Category logger= Category.getInstance(FindAction.class.getName());
	
    public final static int FIRST   = 0;
    public final static int LAST    = 1;
    public final static int ALL     = 2;
    public final static int SINGLE  = 3;
    public final static int COUNT   = 4;
    public final static int NO      = 5;
    public final static int LIST    = 6;
    public final static int INDEX   = 7;

    protected int scope, ret, retIndex, count;
    protected String condition;
    protected Object val;
    protected IntervalList intervalList;
    protected String alt;

    public FindAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    protected String solveReferences(String s, boolean isInteger) throws Exception { //works only with tmp keys from xmlExtractor.def
        int i;
        while ((i=s.indexOf('@'))!=-1) {
            int j;
            for (j=i+1; j<s.length(); j++)
                if (!Character.isLetterOrDigit(s.charAt(j)))
                    break;
            Object o=xmlExtractor.def.get(s.substring(i+1, j));
            if (o==null)
                throw new XMLOptionalRuleException("Definition \""+s.substring(i+1, j)+"\" not found");
            if (isInteger && !(o instanceof Integer))
                throw new XMLSyntaxRuleException("Definition \""+s.substring(i+1, j)+"\" must be an Integer");
            s=s.substring(0, i)+o+s.substring(j);
        }
        return s;
    }

    public void initialize() throws Exception {
        // scope
        scope=NodeTracking.parseScope(element.getAttribute("SCOPE"));
        // return
        String sret=element.getAttribute("RETURN");
        if (sret.length()==0)
            ret=FIRST;
        else if (sret.equals("first"))
             ret=FIRST;
        else if (sret.equals("last"))
             ret=LAST;
        else if (sret.equals("all"))
             ret=ALL;
        else if (sret.equals("single"))
             ret=SINGLE;
        else if (sret.equals("count"))
             ret=COUNT;
        else if (sret.equals("index"))
             ret=INDEX;
        else {
            sret=solveReferences(sret, true);
            if (sret.indexOf(',')>0 || sret.indexOf('-')>0) {
                ret=LIST;
                try {
                    intervalList=new IntervalList();
                    intervalList.setInterval(sret);
                } catch (XMLSyntaxRuleException e) {
                    throw new XMLOptionalRuleException("Invalid RETURN attribute of node FIND : \""+sret+"\"", e);
                }

            } else {
                ret=NO;
                try {
                    retIndex=Integer.parseInt(sret);
                } catch (NumberFormatException e) {
                     throw new XMLOptionalRuleException("Invalid RETURN attribute of node FIND : \""+sret+"\"", e);
                }
            }
        }
        // cond
        Node cond=null;
        NodeList nl=element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++)
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE && nl.item(i).getNodeName().equals("COND")) {
                cond=nl.item(i);
                break;
            }
        String scond=element.getAttribute("COND");
        scond=scond.length()==0?null:scond;
        if (scond!=null && cond!=null)
            throw new XMLSyntaxRuleException("A FIND not cannot have a 'cond' attribute and a 'COND' child node");
        if (scond==null && cond==null)
            throw new XMLSyntaxRuleException("Condition missing in node FIND");
        if (scond!=null)
            condition=scond;
        else {
            Node cdata=null;
            nl=cond.getChildNodes();
            for (int i=0; i<nl.getLength(); i++)
                if (nl.item(i).getNodeType()==Node.CDATA_SECTION_NODE) {
                    cdata=nl.item(i);
                    break;
                }
            if (cdata==null)
                throw new XMLSyntaxRuleException("'COND' node does not have a CDATA node; you should put the condition in the attribute if it does not contain offending XML structures");
            condition=cdata.getNodeValue();
        }
        condition = solveReferences(condition, false);
        // alt
        alt=element.getAttribute("ALTERNATE");
        alt=alt.length()>0?alt:null;
        if (alt != null && alt.equals("null"))
        	alt="";
    }

    protected Element getInnerAction() throws Exception {
        NodeList nl=element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++)
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE && !nl.item(i).getNodeName().equals("COND"))
                return (Element)nl.item(i);
        throw new XMLSyntaxRuleException("There is no inner node into FIND node");
    }

    protected Object initValue(Object ob) throws Exception {
        if (ret==ALL || ret==LIST) {
            if (!(ob instanceof MultipleArray))
                ob=new MultipleArray(ob);
            ((MultipleArray)ob).setAddArray();
        }
        return ob; 
    }

    protected Object retriveSingleValue(Object o) throws Exception {
        if (!(o instanceof Node)) {
/*logger.info(">>> "+o.getClass());
if (o instanceof MultipleArray)
    logger.info("> > "+((MultipleArray)o).dim);*/
            throw new XMLSyntaxRuleException("FIND can be applied only to nodes");
        }
        // init val
        val=null;
        count=0;
        switch (ret) {
            case ALL:
            case LIST:
                val=new ArrayList();
                break;
        }
        Node n=(Node)o;
        NodeTracking nt=new NodeTracking(scope);
        nt.trackNodes(n, this);
        // finalize val
        switch (ret) {
            case FIRST:
            case LAST:
            case SINGLE:
                if (val==null)
                	if (alt != null)
                		val = alt;
                	else 
                		throw new XMLOptionalRuleException("FIND action have not found any node");
                break;
            case ALL:
            case LIST:
                if (((ArrayList)val).size()==0)
                    throw new XMLOptionalRuleException("FIND action have not found any node");
	        	val=new MultipleArray(val, 1);
                break;
            case NO:
                if (val==null)
                    throw new XMLOptionalRuleException("There are only "+count+" nodes, #"+retIndex+" not found");
                break;
            case COUNT:
                val=new Integer(count);
                break;
            case INDEX:
                if (val==null)
                    throw new XMLOptionalRuleException("FIND action have not found any node");
                Node nc=(Node)val;
                if (nc.getNodeType()!=Node.ELEMENT_NODE)
                    throw new XMLRuleException("FIND INDEX can be applied only to tags");
                int idx=0;
                NodeList nl=nc.getParentNode().getChildNodes();
                int i;
                for (i=0; i<nl.getLength(); i++)
                    if (nl.item(i).getNodeType()==Node.ELEMENT_NODE && 
                        nl.item(i).getNodeName().equals(nc.getNodeName())) {
                        idx++;
                        if (nl.item(i)==nc)
                            break;
                    }
                val=new Integer(idx);
                break;
        }
        return val;
    }

    public boolean nodeProcess(Node n) throws Exception {
        if (!SearchExpression.evalExpression(condition, n))
            return true;
        count++;
        switch (ret) {
            case FIRST:
                val=n;
                return false;
            case LAST:
                val=n;
                return true;
            case ALL:
                ((ArrayList)val).add(n);
                return true;
            case LIST:
                if (intervalList.getItem(count))
                    ((ArrayList)val).add(n);
                return true;
            case SINGLE:
            case INDEX:
                if (val!=null)
                    throw new XMLRuleException("FIND SINGLE assertion failed");
                val=n;
                return true;
            case COUNT:
                return true;
            case NO:
                if (count==retIndex) {
                    val=n;
                    return false;
                }
                return true;
            default:
                throw new XMLSyntaxRuleException("Invalid scope : "+scope);
        }
    }
}
