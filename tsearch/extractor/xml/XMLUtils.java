package ro.cst.tsearch.extractor.xml;

import org.xml.sax.*;
import org.w3c.dom.*;


import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.*;

public class XMLUtils {

    static  {
        
        System.setProperty( "javax.xml.parsers.DocumentBuilderFactory", 
        	"org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        
    }
    
    private static Pattern whiteSpaceRemoval=Pattern.compile(">\\s+<");

    public static String nodeToString(Node n) {
        switch (n.getNodeType()) {
            case 1:
                return "E:"+n.getNodeName();
            case 2:
                return "A:"+n.getNodeName()+":"+n.getNodeValue();
            case 3:
                return "T:"+n.getNodeValue();
        }
        return "";
    }

    public static Document read(String s) throws Exception {
        try{
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return db.parse(new InputSource(new StringReader(s)));
        }
        catch(Exception e){
        	e.printStackTrace();
        }
        return null;
    }

    public static Document read(File file, String path) throws Exception {
    	return read(file, path, false);
    }
    
    public static Document read(File file, String path, boolean debug) throws Exception {
        final String fpath=path;
        StringBuffer sb=new StringBuffer();
        BufferedReader rd=new BufferedReader(new FileReader(file));
        String s;
        while ((s=rd.readLine())!=null) {
            sb.append(s);
            if(debug) {
            	sb.append("\n");
            }
        }
        rd.close();
        
        InputSource in = null;
        if(debug) {
        	in = new InputSource(new StringReader(sb.toString()));
        } else {
        	s=whiteSpaceRemoval.matcher(sb.toString()).replaceAll("><");
        	in = new InputSource(new StringReader(s));
        }

        in.setSystemId("file://"+file.getAbsolutePath());
        Document d = null;
        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        
        dbf.setValidating(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException,IOException {
                    if (systemId.equals("file://E:/work/xml/test.dtd")) {
                        // return a special input source
                        return new InputSource(new BufferedReader(new FileReader(fpath+File.separator+"extractor.dtd")));
                    } else if (systemId.equals("file://E:/work/xml/typedef.dtd")) {
                        // return a special input source
                        return new InputSource(new BufferedReader(new FileReader(fpath+File.separator+"doctype.dtd")));
                    } else if (systemId.equals("file://testcase.dtd")) {
                        return new InputSource(new BufferedReader(new FileReader(fpath+File.separator+"testcase.dtd")));
                    } else if  (systemId.equals("file://TSDtemplate.dtd")) {
						return new InputSource(new BufferedReader(new FileReader(fpath+File.separator+"TSDtemplate.dtd")));
                    } else {
                        // use the default behaviour
                        return null;
                    } 
                }
            });
        db.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException exception) throws SAXException {
                    throw exception;
                }
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
        try {
        	//IOUtil.copy(in.getByteStream(), (OutputStream) new FileOutputStream("c:\\caca.txt"));
            d = db.parse(in);
        } catch (SAXParseException e) {
            System.err.println("line:"+e.getLineNumber()+", col:"+e.getColumnNumber()+ ", cause:"+e.getCause()+", exception:"+e.getException());
            
            try {
				in.setByteStream(new FileInputStream(file));
				d = db.parse(in);
			} catch (SAXParseException eParseExceptionInternal) {
				System.err.println(
						"line:" + eParseExceptionInternal.getLineNumber()+
						", col:" + eParseExceptionInternal.getColumnNumber()+ 
						", cause:" + eParseExceptionInternal.getCause()+
						", exception:" + eParseExceptionInternal.getException());
			}
            
            
            throw e;
        }
        return d;
    }

    public static void write(Document d, String fileName) throws IOException {
        PrintWriter pw=new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        write(d, pw);
        pw.close();
    }

    public static void write(Document d, PrintWriter w) {
        write(d.getDocumentElement(), w, "");
    }

    public static String write(Node n) throws IOException {
        StringWriter sw=new StringWriter();
        PrintWriter pw=new PrintWriter(sw);
        write(n, pw, null);
        pw.close();
        sw.close();
        return sw.toString();
    }

    protected static void write(Node n, PrintWriter w, String indent) {
        String start=indent==null?"":indent, newIndent=indent==null?null:indent+"  ";
        switch (n.getNodeType()) {
            case Node.ELEMENT_NODE:
                w.print(start+"<"+n.getNodeName());
                NamedNodeMap nnm=((Element)n).getAttributes();
                for (int i=0; i<nnm.getLength(); i++) {
                    Attr a=(Attr)nnm.item(i);
                    w.print(" "+a.getNodeName()+"=\""+a.getNodeValue()+"\"");
                }
                ArrayList children=new ArrayList();
                NodeList nl=n.getChildNodes();
                for (int i=0; i<nl.getLength(); i++) {
                    int type=nl.item(i).getNodeType();
                    if (type==Node.ELEMENT_NODE || type==Node.TEXT_NODE ||
                        type==Node.CDATA_SECTION_NODE)
                        children.add(nl.item(i));
                }
                if (children.size()>0) {
                    w.print(">\n");
                    Iterator it=children.iterator();
                    while (it.hasNext()) {
                        write((Node)it.next(), w, newIndent);
                    }
                    w.print(start+"</"+n.getNodeName()+">\n");
                } else {
                    w.print("/>\n");
                }
                break;
            case Node.TEXT_NODE:
                w.print(start+n.getNodeValue()+"\n");
                break;
            case Node.CDATA_SECTION_NODE:
                w.print(start+"<![CDATA["+n.getNodeValue()+"]]>\n");
        }
    }

    public static void writeHTML(Document d, PrintWriter w) {
        writeHTML(d.getDocumentElement(), w, "");
    }

    public static String writeHTML(Node n) throws IOException {
        StringWriter sw=new StringWriter();
        PrintWriter pw=new PrintWriter(sw);
        writeHTML(n, pw, null);
        pw.close();
        sw.close();
        return sw.toString();
    }

    protected static void writeHTML(Node n, PrintWriter w, String indent) {
        String start=indent==null?"":indent, newIndent=indent==null?null:indent+"  ";
        switch (n.getNodeType()) {
            case Node.ELEMENT_NODE:
                w.print(start+"<"+n.getNodeName());
                NamedNodeMap nnm=((Element)n).getAttributes();
                for (int i=0; i<nnm.getLength(); i++) {
                    Attr a=(Attr)nnm.item(i);
                    w.print(" "+a.getNodeName()+"=\""+a.getNodeValue()+"\"");
                }
                w.print(">\n");
                NodeList nl=n.getChildNodes();
                for (int i=0; i<nl.getLength(); i++) {
                    int type=nl.item(i).getNodeType();
                    if (type==Node.ELEMENT_NODE || type==Node.TEXT_NODE ||
                        type==Node.CDATA_SECTION_NODE)
                        writeHTML(nl.item(i), w, newIndent);
                }
                w.print(start+"</"+n.getNodeName()+">\n");
                break;
            case Node.TEXT_NODE:
                w.print(start+n.getNodeValue()+"\n");
                break;
            case Node.CDATA_SECTION_NODE:
                w.print(start+"<![CDATA["+n.getNodeValue()+"]]>\n");
        }
    }

    public static Element getFirstElement(Element el) throws Exception {
        return getNthElement(el, 1);
    }

    public static Element getSecondElement(Element el) throws Exception {
        return getNthElement(el, 2);
    }

    public static Element getNthElement(Element el, int n) throws Exception {
        NodeList nl=el.getChildNodes();
        for (int i=0; i<nl.getLength(); i++)
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE)
                if (--n==0)
                    return (Element)nl.item(i);
        throw new Exception("Element "+el.getNodeName()+" does not have "+n+" child");
    }

    public static Map readMap(Element el) throws Exception {
        Map r=new HashMap();
        NodeList nl=el.getChildNodes();
        for (int i=0; i<nl.getLength(); i++)
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE) {
                Element eli=(Element)nl.item(i);
                String key=eli.getAttribute("KEY");
                String value=eli.getAttribute("VALUE");
                String param=eli.getAttribute("PARAM");
                r.put(key, new String[]{value, param});
            }
        return r;
    }

    public static void main (String args[]) throws Exception {
        Document d=XMLUtils.read(new File("E:\\ORG\\ECLIPSE\\workspace\\tsearch\\WEB-INF\\classes\\doctype\\doctype.xml"), 
                   						  "E:\\ORG\\ECLIPSE\\workspace\\tsearch\\WEB-INF\\classes\\doctype");
        StringWriter sw=new StringWriter();
        PrintWriter pw=new PrintWriter(sw);
        write(d, pw);
        pw.close();
        System.out.println(sw);
    }
}
