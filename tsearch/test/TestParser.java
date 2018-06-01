package ro.cst.tsearch.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.servers.functions.TXGenericAO;
import ro.cst.tsearch.utils.StringUtils;

public class TestParser {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
    	
		//String inputFileName = "d:/a.txt";
		//String outputFileName = "d:/b.txt";
		
		String inputFileName = "C:\\Documents and Settings\\l\\Desktop\\TXNuecesAO\\names.csv";
		String outputFileName = "C:\\Documents and Settings\\l\\Desktop\\TXNuecesAO\\names_out.csv";
		
		BufferedReader in = new BufferedReader(new FileReader(inputFileName));
		PrintWriter out = new PrintWriter(new FileWriter(outputFileName, false));
		
		
		StringFormats.parseNameNashville("KREMERS JACK & DORIS");
		
		String line;
		String outLine;
		int i=0;
		while( (line = in.readLine()) != null){
//			outLine = TX
//			out.println(line);
//			System.out.println("Input:[" + line + "]; Output:[" + outLine + "]; " );
			//String[] items = line.split("#####");
			i++;
			ResultMap m = new ResultMap();
			//line =line.replaceAll("\\\\n", "\n");
			//line =line.replaceAll("&amp;", "&");
			//double amount = TSServer.parseMortgageAmount(line);
			//String amount = WordsToNumbers.transformAmountFromWords(line);
			//m.put("tmpLegal", line);
			line = line.replaceAll("\"", "");
			m.put("tmpOwner", line);
			TXGenericAO.partyNamesTXBexarIS(m, -1, 1);
			
			out.println(line);
			out.println("=====================================================");
			out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++");
			//out.println(m.display
			//out.println(m.displayParty());
			//out.println(m.displayAddress());
			out.println(m.displayOwner());
			out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++");
		}
		in.close();
		out.close();
	
	}
	
	private static void testLegals() throws Exception {
    	String inputFileName = "d:/t/remarks-okaloosa-rv-cleand.txt";
    	String outputFileName = "d:/t/remarks-okaloosa-rv-parsed.txt";
    	
    	BufferedReader in = new BufferedReader(new FileReader(inputFileName));
		PrintWriter out = new PrintWriter(new FileWriter(outputFileName, false));
		
		String line = "";
		while((line = in.readLine()) != null){
			String legal = line;
			ResultMap rm = new ResultMap();
			rm.put("PropertyIdentificationSet.PropertyDescription", legal);
			GenericFunctions.legalFLOkaloosaDASLRV(rm, -1, true);
			out.println(line);
			out.println(rm.displayLegal());
			out.println();	
		}	
		out.flush();
		out.close();
	}
	
	private static void testRemarks() throws Exception {
    	String inputFileName = "d:/remarks.raw";
    	String outputFileName = "d:/remarks-parsed.raw";
    	
    	BufferedReader in = new BufferedReader(new FileReader(inputFileName));
		PrintWriter out = new PrintWriter(new FileWriter(outputFileName, false));
		
		String line = "";
		while((line = in.readLine()) != null){
			String legal = line;
			ResultMap rm = new ResultMap();
			rm.put("PropertyIdentificationSet.PropertyDescription", legal);
			//GenericFunctions.legalRemarksFLEscambiaRV(rm, -1);
			out.println(line);
			out.println(rm.displayLegal());
			out.println();	
		}		
	}
	
    public static void test() throws Exception {    	
    	testLegals();
    	//testRemarks();
    }
    
    
    public static void append(String fileName, String message) throws Exception {
		PrintWriter out = new PrintWriter(new FileWriter(fileName, true));
		out.println(message);
		out.println();
		out.close();    	
    }

    public static void printSample(ResultMap m) throws Exception {
   	
    	String id = "unknown";
    	String book = (String)m.get("SaleDataSet.Book");
    	String page = (String)m.get("SaleDataSet.Page");
    	String parcelId = (String)m.get("PropertyIdentificationSet.ParcelID");
    	if(!StringUtils.isEmpty(parcelId)){
    		id = parcelId;
    	}
    	
    	if(!StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)){
    		id = book + "_" + page;
    	}
    	
    	String legal = (String)m.get("PropertyIdentificationSet.PropertyDescription");    	
    	if(!StringUtils.isEmpty(legal)){
    		append("d:/legals-okaloosa-rv.txt",  legal);
    	}
    	
    	String remarks = (String)m.get("SaleDataSet.Remarks");
    	if(!StringUtils.isEmpty(remarks)){
    		append("d:/remarks-okaloosa-rv.txt",  remarks);
    	}
    	
    }

}
