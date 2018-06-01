package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;

public class SearchExpression {

    protected static int matchBrace(String s, int openBraceIndex) throws Exception {
        int pos=openBraceIndex+1;
        for (int h=1; h>0; ) {
            int b=s.indexOf('{', pos);
            b=b==-1?Integer.MAX_VALUE:b;
            int e=s.indexOf('}', pos);
            e=e==-1?Integer.MAX_VALUE:e;
            if (b<e) {
                h++;
                pos=b+1;
            } else if (e<b) {
                h--;
                pos=e+1;
            } else
                throw new XMLSyntaxRuleException("Unbalanced expresion : "+s);
        }
        return pos-1;
    }
    
    public static boolean evalExpression(String expr, Object n, ResultMap m) throws Exception {
        expr=expr.trim();
        // (<expr>)
        int b;
        while ((b=expr.indexOf("{"))!=-1) {
            int e=matchBrace(expr, b);
            expr=expr.substring(0, b)+(evalExpression(expr.substring(b+1, e), n, m)?"T":"F")+
                expr.substring(e+1);
        }
        // &&, ||
        int and=expr.indexOf("&&");
        and=and==-1?Integer.MAX_VALUE:and;
        int or=expr.indexOf("||");
        or=or==-1?Integer.MAX_VALUE:or;
        if (and<or) {
            return evalExpression(expr.substring(0, and), n, m) &&
                   evalExpression(expr.substring(and+2), n, m);
        } else if (or<and) {
            return evalExpression(expr.substring(0, or), n, m) ||
                   evalExpression(expr.substring(or+2), n, m);
        }
        // !
        if (expr.charAt(0)=='!')
            return !evalExpression(expr.substring(1), n, m);
        // T, F
        if (expr.equals("T"))
            return true;
        if (expr.equals("F"))
            return false;
        // <item>
        if (n==null)
        	return false;
        
        if (n instanceof Node)
            return new SearchItem(expr).evaluate((Node)n);
        else if (n instanceof String)
            return new StringExpr(expr).evaluate((String)n, m);
        else
            throw new Exception("Invalid Item type : "+n.getClass());
    }
    
    public static boolean evalExpression(String expr, Node n) throws Exception {
        expr=expr.trim();
        // (<expr>)
        int b;
        while ((b=expr.indexOf("{"))!=-1) {
            int e=matchBrace(expr, b);
            expr=expr.substring(0, b)+(evalExpression(expr.substring(b+1, e), n)?"T":"F")+
                expr.substring(e+1);
        }
        // &&, ||
        int and=expr.indexOf("&&");
        and=and==-1?Integer.MAX_VALUE:and;
        int or=expr.indexOf("||");
        or=or==-1?Integer.MAX_VALUE:or;
        if (and<or) {
            return evalExpression(expr.substring(0, and), n) &&
                   evalExpression(expr.substring(and+2), n);
        } else if (or<and) {
            return evalExpression(expr.substring(0, or), n) ||
                   evalExpression(expr.substring(or+2), n);
        }
        // !
        if (expr.charAt(0)=='!')
            return !evalExpression(expr.substring(1), n);
        // T, F
        if (expr.equals("T"))
            return true;
        if (expr.equals("F"))
            return false;
        // <item>
        return new SearchItem(expr).evaluate(n);
    }
}
