package ro.cst.tsearch.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.lang.reflect.*;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servlet.BaseServlet;

public class TokenizersTest {
	
	private static String inputLbl = "input";
	private static String referenceLbl = "parsed_reference";
	private static String methodLbl = "method";
	private static String displayLbl = "display";
	
	public static String unitTestFolder = "unit_tests";
	public static String unitTestRootFolder = BaseServlet.REAL_PATH + ".." + File.separator + unitTestFolder;
	public static String testcaseFolderIn = unitTestRootFolder + File.separator + "parser" + File.separator + "tokenizers";
	public static String testcaseFolderOut = unitTestRootFolder + File.separator + "output" + File.separator + "parser" + File.separator + "tokenizers";
	
	public class TestCase {
		
		BufferedReader input = null;
		String testcaseName = null;
		File reference = null;
		File output = null;
		Method method = null; 
		Method display = null;		
				
		TestCase(String fileName){
			try {
				BufferedReader in = new BufferedReader(new FileReader(fileName));
				String line; 
				String inputStr = "";
				String refStr = "";
				String methodStr = "";
				String displayStr = "";
				while((line = in.readLine()) != null){						
					if (line.startsWith(inputLbl+"=")){
						inputStr = line.substring(inputLbl.length()+1); 
					} else if (line.startsWith(referenceLbl+"=")){
						refStr = line.substring(referenceLbl.length()+1);
					} else if (line.startsWith(methodLbl+"=")){
						methodStr = line.substring(methodLbl.length()+1);
					} else if (line.startsWith(displayLbl+"=")){
						displayStr = line.substring(displayLbl.length()+1);
					}
				}
				if (inputStr.length() != 0 && refStr.length() != 0 && methodStr.length() != 0 && displayStr.length() != 0){
					String parent=fileName.substring(0, fileName.lastIndexOf("/"));
					input = new BufferedReader(new FileReader(parent.concat("/").concat(inputStr)));
					testcaseName = fileName.substring(fileName.lastIndexOf("/")+1, fileName.length()-3);
					reference = new File(parent.concat("/").concat(refStr));
					method = Class.forName("ro.cst.tsearch.extractor.xml.GenericFunctions").getMethod(methodStr, new Class[]{ResultMap.class, String.class});
					display = Class.forName("ro.cst.tsearch.extractor.xml.ResultMap").getMethod(displayStr, new Class[]{});
				}
				
			} catch (FileNotFoundException e){}
			  catch (IOException e) {} 
			  catch (ClassNotFoundException e) {}
			  catch (NoSuchMethodException e) {}
		}
		
		public BufferedReader getInput(){
			return input;
		}
		
		public String getTestcaseName(){
			return testcaseName;
		}
				
		public File getReference(){
			return reference;
		}
		
		public Method getMethod(){
			return method;
		}
		
		public Method getDisplay(){
			return display;
		}
		
		public void setOutput(File f){
			output = f;
		}
		
		public File getOutput(){
			return output;
		}
		
		public boolean isValid(){
			return (input != null && reference != null && method != null && display != null);
		}
	}
	
	private static void findTestCases(String path, List<TestCase> testcases){
		
		File crt = new File(path);
		File names[] = crt.listFiles();
				
		for (int i=0; i<names.length; i++){
			String name = names[i].getName();
			if (names[i].isDirectory()){				
				new File(names[i].getPath().replace(testcaseFolderIn, testcaseFolderOut).replace("\\", "/")).mkdir();
				findTestCases(path.concat("/").concat(name), testcases);				
			} else {
				if (name.endsWith(".tc")){
					TestCase test = new TokenizersTest().new TestCase(path.concat("/").concat(name));
					if (test != null){
						testcases.add(test);
					}
				}
			}			 
		}
	}
		
	public static List<TestCase> test() throws Exception {
		String outputFolder;
		String outputFileName;
		
		List<TestCase> testcases = new ArrayList<TestCase>();
		findTestCases(testcaseFolderIn, testcases);
		
		Iterator<TestCase> iter = testcases.iterator();
		while (iter.hasNext()){
			TestCase test = iter.next();
			if (test.isValid()){
				String line;
				BufferedReader in = test.getInput();	
				String refFileName = test.getReference().getName();
				outputFolder = test.getReference().getParent().replace(testcaseFolderIn, testcaseFolderOut).replace("\\", "/");
				outputFileName = outputFolder.concat("/").concat(refFileName.substring(0, refFileName.lastIndexOf("."))).concat("_parsed_new.txt");
				PrintWriter out = new PrintWriter(new FileWriter(outputFileName, false));
				
				while((line = in.readLine()) != null){
					ResultMap rm = new ResultMap();
					test.getMethod().invoke(null, new Object[]{rm, line});	
					String a[] = line.split("\\s*@@\\s*");
					for (int i=0; i<a.length; i++)
						out.println(a[i]);	
					out.println(test.getDisplay().invoke(rm, new Object[]{}));
					out.println();	
					out.flush();
				}				
				in.close();
				out.close();
		        test.setOutput(new File(outputFileName));
			} 
		}	
		return testcases;
	}
	
	public static void main(String[] args) throws Exception {
    	test();						
	}

}
