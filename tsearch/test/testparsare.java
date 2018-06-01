package ro.cst.tsearch.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.XMLExtractor;
import ro.cst.tsearch.extractor.xml.XMLUtils;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.types.TSServersFactory;

import java.math.BigDecimal;

import org.w3c.dom.Document;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.UserValidation;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.emailOrder.MailOrder;
import ro.cst.tsearch.utils.Tidy;


public class testparsare {
    
    public static String context = "D:\\workspace.2\\TSEARCH";
    public static String path = context + "\\src\\ro\\cst\\tsearch\\test\\";
    
    public static void main (String args[]) throws Exception {

        if (args.length<2) {
            System.err.println("Usage : java test <site> <file.html>");
            System.exit(1);
        }

        BufferedReader in=new BufferedReader(new FileReader(path + args[0]+"\\"+args[1]));
        StringBuffer sb=new StringBuffer();
        String s;
        while ((s=in.readLine())!=null) {
            sb.append(s);
            sb.append('\n');
        }
        in.close();        
        s=sb.toString();
        //s = Tidy.tidyParse(s,null);		//only for HTML input files 
        //Document ruleDoc=XMLUtils.read(new File("test.xml"));

//        XMLExtractor xmle=new XMLExtractor(s, new Object[]{
//                                           new Long(TSServersFactory.getSiteId(args[2]))},
//                                           "E:\\workspace.subversion\\TSEARCH\\");     

       
        String xmlRulesLocation = context + File.separator + "WEB-INF" + File.separator + "classes" + File.separator + "rules";      
       // Document rules = XMLUtils.read(new File(xmlRulesLocation + File.separator + "inter\\"+ args[2]), xmlRulesLocation); //intermediare
        Document rules = XMLUtils.read(new File(xmlRulesLocation + File.separator + args[2]), xmlRulesLocation);		// finale
    
        // html
//        XMLExtractor xmle = new XMLExtractor(s, rules, context, -1, args[0]);        
//        xmle.process();        	        
//        ParsedResponse pr=new ParsedResponse();
//        //System.out.println(xmle.getDefinitions().get("SaleDataSet"));
//        Bridge b=new Bridge(pr, xmle.getDefinitions());
//        b.importData();
//        b.printParsedResponse(System.out);
//        System.exit(0);
        
        // xml
        ParsedResponse pr=new ParsedResponse();
        XMLExtractor.parseXmlDoc(pr, XMLUtils.read(s), rules, -1);
	            
    }
}
