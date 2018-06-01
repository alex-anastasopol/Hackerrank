package ro.cst.tsearch.extractor.xml;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.w3c.dom.Element;

public class ParserException extends Exception {

    protected Element ruleElement;

    public ParserException(Throwable thr, Element el) {
        super(thr);
        ruleElement=el;
    }

    public ParserException(Element el) {
        ruleElement=el;
    }

    public void printStackTrace(PrintStream ps) {
        PrintWriter pw=new PrintWriter(ps);
        printStackTrace(pw);
        try {
        	if(ps != System.out && ps != System.err) {
        		pw.close();	
        	}
        }catch(Exception e) {
        	e.printStackTrace();
        }
    }

    public void printStackTrace(PrintWriter pw) {
        pw.println("Rule tree that caused the exception :");
        XMLUtils.write(ruleElement, pw, "");
        super.printStackTrace(pw);
    }
}
