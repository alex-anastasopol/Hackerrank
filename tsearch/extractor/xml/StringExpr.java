package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;
import java.util.regex.*;

import org.apache.log4j.Category;

public class StringExpr {
	
	protected static final Category logger= Category.getInstance(StringExpr.class.getName());
	
    public static int EQUALS=0;

    protected int op;
    protected String name, param;

    public StringExpr(String val) throws Exception {
        // val = <op>(<name>|<val>)[:<param>]
        // extract op
        if (val.startsWith("=")) {
            op=EQUALS;
        } else
            throw new Exception("Unsupported format : "+val);

        int i1;
        for (i1=val.indexOf(':'); i1>0 && val.charAt(i1-1)=='\\'; )
            i1=val.indexOf(':', i1+1);
        // extract name
        if (i1==-1)
            name=val.substring(1);
        else 
            name=val.substring(1, i1);
        name=name.replaceAll("\\\\:", ":");
        // extract param
        if (i1==-1)
            param="";
        else
            param=val.substring(i1+1);
    }

    public static void main (String args[]) throws Exception {
        //logger.info(new StringExpr(args[0]).evaluate(args[1], new ResultMap())); //TO DO A
    }

    public String toString() {
        StringBuffer sb=new StringBuffer();
        sb.append("op = '='");
        sb.append("\nname/value = ");
        sb.append(name);
        sb.append("\nparam = ");
        sb.append(param);
        sb.append("\n");
        return sb.toString();
    }

    public boolean evaluate(String n, ResultMap m) throws Exception {
        String s=name;
        Object o=m.get(s);
        if (o!=null && o instanceof String)
            s=(String)o;
        return SearchItem.compare(s, n, param);
    }
}
