package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;

import java.lang.reflect.*;
import java.util.regex.*;

import org.apache.log4j.Category;

public class GetvalueAction extends BaseAction {

	protected static final Category logger= Category.getInstance(GetvalueAction.class.getName());
	
    protected String regex, regexrepl, formatFunction, attr;
    protected boolean replaceAll, pad;
    protected Method method;

    public GetvalueAction(BaseAction ba, Element el,long searchId) {
        super(ba, el,searchId);
    }

    public void initialize() throws Exception {
        // regex
        Node nregex=null;
        NodeList nl=element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++)
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE && nl.item(i).getNodeName().equals("REGEX")) {
                nregex=nl.item(i);
                break;
            }
        String sregex=element.getAttribute("REGEX");
        sregex=sregex.length()==0?null:sregex;
        if (sregex!=null && nregex!=null)
            throw new XMLSyntaxRuleException("A GETVALUE node not cannot have a 'regex' attribute and a 'REGEX' child node");
        if (sregex!=null)
            regex=sregex;
        else if (nregex!=null) {
            Node cdata=null;
            nl=nregex.getChildNodes();
            for (int i=0; i<nl.getLength(); i++)
                if (nl.item(i).getNodeType()==Node.CDATA_SECTION_NODE) {
                    cdata=nl.item(i);
                    break;
                }
            if (cdata==null)
                throw new XMLSyntaxRuleException("'REGEX' node does not have a CDATA node; you should put the expression in the attribute if it does not contain offending XML structures");
            regex=cdata.getNodeValue();
        }
        // regexrepl
        Node nregexrepl=null;
        nl=element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++)
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE && nl.item(i).getNodeName().equals("REGEXREPL")) {
                nregexrepl=nl.item(i);
                break;
            }
        String sregexrepl=element.getAttribute("REGEXREPL");
        sregexrepl=sregexrepl.length()==0?null:sregexrepl;
        if (sregexrepl!=null && nregexrepl!=null)
            throw new XMLSyntaxRuleException("A GETVALUE node not cannot have a 'regexrepl' attribute and a 'REGEXREPL' child node");
        if ((sregexrepl!=null || nregexrepl!=null) && regex==null)
            throw new XMLSyntaxRuleException("A GETVALUE node not cannot have a 'regexrepl' attribute (or a 'REGEXREPL' child node) and not have regex");
        if (sregexrepl!=null)
            regexrepl=sregexrepl;
        else if (nregexrepl!=null) {
            Node cdata=null;
            nl=nregexrepl.getChildNodes();
            for (int i=0; i<nl.getLength(); i++)
                if (nl.item(i).getNodeType()==Node.CDATA_SECTION_NODE) {
                    cdata=nl.item(i);
                    break;
                }
            if (cdata==null)
                throw new XMLSyntaxRuleException("'REGEXREPL' node does not have a CDATA node; you should put the expression in the attribute if it does not contain offending XML structures");
            regexrepl=cdata.getNodeValue();
        } else {
            regexrepl="$1";
        }
        if (regexrepl.equals("null"))
            regexrepl="";
        // format function
        if (element.getAttribute("FORMAT").length()!=0) {
            formatFunction=element.getAttribute("FORMAT");
            try {
                method=Class.forName("ro.cst.tsearch.extractor.xml.StringFormats").getMethod(formatFunction, new Class[]{String.class});
            } catch (NoSuchMethodException e) {
                throw new XMLSyntaxRuleException("FORMAT \""+formatFunction+"\" not found");
            }
        }
        // replaceAll
        replaceAll=element.getAttribute("REPLALL").equals("yes");
        // pad
        pad=element.getAttribute("PAD").equals("yes");
        // attr
        attr = element.getAttribute("ATTR");
        
    }

    protected Element getInnerAction() throws Exception {
        NodeList nl=element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++)
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE && !nl.item(i).getNodeName().equals("REGEX"))
                return (Element)nl.item(i);
        throw new XMLSyntaxRuleException("There is no inner node into GETVALUE node");
    }

    protected Object retriveSingleValue(Object ob) throws Exception {
        String ret;
        if (ob instanceof Node) {
            Node n=(Node)ob;
            switch (n.getNodeType()) {            
                case Node.ELEMENT_NODE:
                	if (attr == null || attr.length() == 0)
                		ret=XMLUtils.write((Node)ob);
                	else {
                		NamedNodeMap nnm =((Element)ob).getAttributes();
                		ret = "";
                        for (int i=0; i<nnm.getLength(); i++)
                            if(nnm.item(i).getNodeName().equalsIgnoreCase(attr)){
                            	ret = nnm.item(i).getNodeValue();
                            	break;
                            }
                	}
                		//ret = ((Element)ob).getAttribute(attr);
                    break;
                case Node.TEXT_NODE:
                    ret=n.getNodeValue();
                    ret=pad?ret:ret.trim();
                    break;
                default:
                    throw new XMLRuleException("Unknown node type");
            }
        } else if (ob instanceof Integer) {
            ret=((Integer)ob).toString();
        } else if (ob instanceof String) {
            ret=(String)ob;
        } else
            throw new XMLSyntaxRuleException("Cannot apply GETVALUE on smth else than a Node, an Integer or a String");
        if (regex!=null) {
            regex=replaceVar(regex);
            regexrepl=replaceVar(regexrepl);
//            logger.error("ret=>"+ret+"<, regex=>"+regex+"<, regexrepl=>"+regexrepl+"<, replaceAll="+replaceAll);
            if (replaceAll)
                ret=ret.replaceAll(regex, regexrepl);
            else
                ret=ret.replaceFirst(regex, regexrepl);
            ret=pad?ret:ret.trim();
//            logger.error(ret);
        }
        if (formatFunction!=null) {
            ret=(String)method.invoke(null, new Object[]{ret});
        }
        return ret;
    }

    protected static Pattern var=Pattern.compile("@(.+)@");
    protected String replaceVar(String s) throws Exception {
        for (Matcher m=var.matcher(s); m.find(); m=var.matcher(s)) {
            String name=m.group(1);
            Object o=xmlExtractor.getDefinitions().get(name);
            if (o==null)
                throw new XMLOptionalRuleException("Undefined node "+name);
            if (!(o instanceof String))
                throw new XMLSyntaxRuleException("Node "+name+" should be a String");
            s=s.substring(0, m.start())+(String)o;
            if (s.length()>m.end()+1)
                s+=s.substring(m.end()+1);
        }
        return s;
    }
}
