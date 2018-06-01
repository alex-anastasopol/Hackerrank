package ro.cst.tsearch.extractor.xml; 

import org.w3c.dom.*;
import java.util.regex.*;

import org.apache.log4j.Category;

public class SearchItem implements NodeProcessor {
	
	protected static final Category logger= Category.getInstance(SearchItem.class.getName());
	
    protected int scope;
    protected int type;
    protected String name, value;
    protected boolean ignoreCase, removeWhiteSpaces, content, regex;
    protected boolean result;

    public SearchItem(String val) throws Exception {
        // val = <scope><type>:<name>[:<val>[:<param>]]
        // extract scope
        scope=NodeTracking.parseScope(val);
        val=val.substring(NodeTracking.scopes[scope].length());
        int i1, i2, i3;
        for (i1=val.indexOf(':'); i1>0 && val.charAt(i1-1)=='\\'; )
            i1=val.indexOf(':', i1+1);
        if (i1!=-1) {
            for (i2=val.indexOf(':', i1+1); i2!=-1 && val.charAt(i2-1)=='\\'; )
                i2=val.indexOf(':', i2+1);
            if (i2!=-1) {
                for (i3=val.indexOf(':', i2+1); i3!=-1 && val.charAt(i3-1)=='\\';)
                    i3=val.indexOf(':', i3+1);
            }
            else
                i3=-1;
        } else
            i2=i3=-1;
        // extract type
        if (i1!=0) {
            String stype=val.substring(0, i1==-1?val.length():i1);
            if (stype.equals("tag"))
                type=Node.ELEMENT_NODE;
            else if (stype.equals("text"))
                type=Node.TEXT_NODE;
            else if (stype.equals("attr"))
                type=Node.ATTRIBUTE_NODE;
            else
                throw new XMLSyntaxRuleException("Invalid item type : \""+stype+"\"");
        } else { // default type
            if (i2==-1 || i3==i2+1)
                type=Node.ELEMENT_NODE;
            else if (i2==i1+1)
                type=Node.TEXT_NODE;
            else 
                type=Node.ATTRIBUTE_NODE;
        }
        // set default param
        if (i3==-1)
            switch (type) {
                case Node.ELEMENT_NODE:
                    ignoreCase=true;
                    break;
                case Node.ATTRIBUTE_NODE:
                    ignoreCase=true;
                    break;
                case Node.TEXT_NODE:
                    removeWhiteSpaces=true;
                    content=true;
                    break;
            }
        // extract name
        if (i1==-1)
            name="";
        else if (i2==-1)
            name=val.substring(i1+1);
        else 
            name=val.substring(i1+1, i2);
        // extract value
        if (i2==-1)
            value="";
        else if (i3==-1) 
            value=val.substring(i2+1);
        else
            value=val.substring(i2+1, i3);
        value=value.replaceAll("\\\\:", ":");
        // extract param
        String param;
        if (i3==-1)
            param="";
        else {        	
        	param=val.substring(i3+1);
        	param = param.replaceFirst("([a-z]+)\\b.*", "$1");
        }
        char[] pma=param.toCharArray();
        for (int i=0; i<pma.length; i++)
            switch (pma[i]) {
                case 'i':
                    ignoreCase=true;
                    break;
                case 'w':
                    removeWhiteSpaces=true;
                    break;
                case 'r':
                    regex=true;
                    break;
                case 'c':
                    content=true;
                    break;
                default:
                    throw new XMLSyntaxRuleException("Unknown parameter : "+pma[i]);
            }
    }

    public static void main (String args[]) throws Exception {
        SearchItem si=new SearchItem(".::(?\\:Grantor)|(?\\:Debtor):ircw");
        logger.info(si);    
    }

    public String toString() {
        StringBuffer sb=new StringBuffer();
        sb.append("scope = ");
        sb.append(NodeTracking.scopes[scope]);
        sb.append("\ntype = ");
        switch (type) {
            case Node.ELEMENT_NODE:
                sb.append("tag");
                break;
            case Node.ATTRIBUTE_NODE:
                sb.append("attr");
                break;
            case Node.TEXT_NODE:
                sb.append("text");
                break;
        }
        sb.append("\nname = ");
        sb.append(name);
        sb.append("\nvalue = ");
        sb.append(value);
        sb.append("\nignoreCase = ");
        sb.append(ignoreCase);
        sb.append("\nremoveWhiteSpaces = ");
        sb.append(removeWhiteSpaces);
        sb.append("\ncontent = ");
        sb.append(content);
        sb.append("\nregex = ");
        sb.append(regex);
        sb.append("\n");
        return sb.toString();
    }

    public boolean evaluate(Node n) throws Exception {
        result=false;
        NodeTracking nt=new NodeTracking(scope);
        nt.trackNodes(n, this);
        return result;
    }

    public boolean nodeProcess(Node n) throws Exception {
        switch (scope) {
            case NodeTracking.CURRENT_NODE:
            case NodeTracking.PARENT:
                result=test(n);
                return false;
            case NodeTracking.CHILDREN:
            case NodeTracking.ANCESTORS:
            case NodeTracking.DESCENDANTS:
            case NodeTracking.NEXT_BROTHERS:
            case NodeTracking.PREV_BROTHERS:
                return !(result=test(n));
            default:
                throw new XMLSyntaxRuleException("Invalid scope : "+scope);
        }
    }

    protected boolean test(Node n) throws Exception {
        if (n.getNodeType()!=type)
            return false;
        switch (type) {
            case Node.ELEMENT_NODE:
                return compare(name, n.getNodeName());
            case Node.TEXT_NODE:
                return compare(value, n.getNodeValue());
            default:  // Node.ATTRIBUTE_NODE
                return compare(name, n.getNodeName()) && compare(value, n.getNodeValue());
        }
    }
    
    protected boolean compare(String a, String b) {
        return compare(a, b, ignoreCase, removeWhiteSpaces, regex, content);
    }

    public static boolean compare(String a, String b, String pm) throws Exception {
        boolean ignoreCase=false, removeWhiteSpaces=false, content=false, regex=false;
        char[] pma=pm.toCharArray();
        for (int i=0; i<pma.length; i++)
            switch (pma[i]) {
                case 'i':
                    ignoreCase=true;
                    break;
                case 'w':
                    removeWhiteSpaces=true;
                    break;
                case 'r':
                    regex=true;
                    break;
                case 'c':
                    content=true;
                    break;
                default:
                    throw new Exception("Unknown parameter : "+pma[i]);
            }
        return compare(a, b, ignoreCase, removeWhiteSpaces, regex, content);
    }

    protected static boolean compare(String a, String b, boolean bignoreCase, boolean bremoveWhiteSpaces, boolean bregex, boolean bcontent) {
        if (a.length()==0)
            return true;
        a=a.trim();
        b=b.trim();
        
        if (bignoreCase && !bregex) {
            a=a.toUpperCase();
            b=b.toUpperCase();
        }
        if (bremoveWhiteSpaces) {
            a=bregex?a:a.replaceAll("\\s", "");
            b=b.replaceAll("\\s", "");
        }
        if (bregex) {
            if (bcontent) {
                return Pattern.compile(a).matcher(b).find();
            } else {
                return b.matches(a);
            }
        } else {
            if (bcontent) {
                return b.indexOf(a)!=-1;
            } else {
                return b.equals(a);
            }
        }
    }
}
