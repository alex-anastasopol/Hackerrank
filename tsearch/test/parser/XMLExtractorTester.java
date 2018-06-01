package ro.cst.tsearch.test.parser;

import java.io.File;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.XMLExtractor;
import ro.cst.tsearch.utils.FileUtils;

public class XMLExtractorTester {
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//modify these values
		String testFile = "d:\\workspace\\results3.html";
		String real_path = "d:\\workspace\\TSEARCH_RESIN\\web\\";
		String xmlPath = real_path + File.separator +  "../src/rules/ILDuPageRO.xml";

//sp-m02-5120-00-00012
//		XStreamManager.getInstance().prettyPrint(xmle.getDefinitions(), path+ File.separator + "test1.xml");
//		
		String t = FileUtils.readFile(testFile); 
    	 XMLExtractor xmle = new XMLExtractor(t,
               new Object[] { Long.valueOf(-1) }, real_path,Search.SEARCH_NONE, "", true);
		try {
			xmle.process(xmlPath);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		ResultMap m = xmle.getDefinitions();
		System.out.println(m);
				

	}

}



/*
package ro.cst.tsearch.test.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.w3c.dom.Document;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.XMLExtractor;
import ro.cst.tsearch.extractor.xml.XMLUtils;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.test.testparsare;
import ro.cst.tsearch.utils.FileUtils;

public class XMLExtractorTester {
	
	
	
	
	public static void main(String[] args) throws FileNotFoundException, Exception {
		//modify these values
		String testFile = "d:\\workspace\\14_04\\Calculator\\war\\test.xml";//"d:\\workspace\\results3.html";
		
		String real_path = "d:\\workspace\\14_04\\TSEARCH\\web\\";
		String xmlPath = real_path + File.separator +  "../src/rules/GenericDASLDT.xml";
//		D:\workspace\14_04\TSEARCH\src\rules\GenericDASLDT.xml
//sp-m02-5120-00-00012
//		XStreamManager.getInstance().prettyPrint(xmle.getDefinitions(), path+ File.separator + "test1.xml");
//		
		String t = FileUtils.readFile(testFile); 
//    	 XMLExtractor xmle = new XMLExtractor(t,
//               new Object[] { Long.valueOf(-1) }, real_path,Search.SEARCH_NONE, "", true);
		Document rules = XMLUtils.read(xmlPath);
		 
    	FileReader fileReader = new FileReader(testFile);
		String s= testparsare.getStringFromFile(fileReader);
		
		ParsedResponse pr= new ParsedResponse();
//		XMLExtractor.parseXmlDoc(pr, XMLUtils.read(s), rules, -1);
//		XMLExtractor xmle = new XMLExtractor(rules,parseDoc,111); 
    		 
    	XMLExtractor xmle = new XMLExtractor(rules, XMLUtils.read(s),-1);
//    	 XMLUtils.read(s)
		try {
			xmle.process(xmlPath);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		ResultMap m = xmle.getDefinitions();
		System.out.println(m);
		
				

	}

}
*/