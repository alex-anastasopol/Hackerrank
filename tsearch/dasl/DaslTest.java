package ro.cst.tsearch.dasl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Category;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tools.codec.Base64Decoder;
import org.w3c.tools.codec.Base64FormatException;
import org.xml.sax.SAXException;

import ro.cst.tsearch.connection.dasl.DaslConnection;
import ro.cst.tsearch.connection.dasl.ILCookLAQueryBuilder;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.StringUtils;

/**
 * Simple test of DASL connection
 * @author radu bacrau
 */
public class DaslTest {
	 
	public static void main(String[] args) throws Exception {
		
	/*	String query = FileUtils.readXMLFile("d:/query.xml");
		
		String response = DaslConnection.getDataSynch(query);
		
		/*
		String id = StringUtils.extractParameter(response,"<OrderBusinessId>(\\d+)</OrderBusinessId>");
		if(!id.matches("\\d+")){
			throw new RuntimeException("Order No not found!");
		}
		int orderId = Integer.valueOf(id);
		
		while(true){
			response = DaslConnection.getOrder(orderId);
			String status = StringUtils.extractParameter(response,"<OrderStatusBusinessId>(\\d+)</OrderStatusBusinessId>");
			if("2".equals(status)){
				System.out.println("Querying again ...");
			} else {
				break;
			}
			try{
				TimeUnit.SECONDS.sleep(10);
			}catch(InterruptedException e){
				throw new RuntimeException("Interrupted!");
			}
		}
		
		
		FileUtils.writeTextFile("d:/response.xml", response);
		*/
	}
	
	public static void main6(String[] args) throws Exception {
	/*	
		org.apache.log4j.BasicConfigurator.configure();
		
		args = new String[]{"331152"};
		
		for(String name: args){
		
			String result = FileUtils.readXMLFile(name + ".response.xml");
			
			Node doc = parseXMLDocument(result);
			NodeList nl = xpathQuery(doc, "//ReportBinary");
			for(int i=0; i<nl.getLength(); i++) {
	
		        Node node = nl.item(i);
		        String coded = getNodeCdataValue(node);
		        String decoded = decodeBase64(coded);
		        FileUtils.writeTextFile(name + ".response.txt", decoded);	        
			}
		}*/
	}
	
	public static void main5(String [] args){
			args = new String[]{
					"506707",
					"508809",
					"TM24774",
					"TM24825",
					"TM24915",
					"TM24918",
					"TM24981",
					"TM25032",
					"TM25035"
			};
		
		for(String name: args){		
			String result = FileUtils.readXMLFile("d:/" + name + ".xml");
			FileUtils.writeTextFile("d:/" + name + ".proc.xml", result);        
		}
	
	}
	
	protected static final Category logger = Category.getInstance(DaslTest.class);	
	/**/
	public static void main7(String[] args) throws Exception {
		/*/String docNo = "0020733859";
		//String docNo = "0612802002";
		String docNo =   "0025255765";
		
		//String query = ILCookLAQueryBuilder.buildImageQuery(docNo);
		String query = "";
		
		query = query.replaceAll("@@UserName@@", "spatel");
		query = query.replaceAll("@@ClientId@@", "4486");
		query = query.replaceAll("@@CountyFIPS@@", "031");
		query = query.replaceAll("@@StateFIPS@@", "17");
		query = query.replaceAll("@@ImageProductId@@", "205");
		query = query.replaceAll("@@ImageProviderId@@", "9");
		
		String result = DaslConnection.getDataSynch(query);
		
		FileUtils.writeTextFile("images/" + docNo + ".query.xml", query);
		FileUtils.writeTextFile("images/" + docNo + ".response.xml", result);		
			
		Node doc = parseXMLDocument(result);
		NodeList nl = xpathQuery(doc, "//Content");
		for(int i=0; i<nl.getLength(); i++) {
	
	        Node node = nl.item(i);
	        String coded = getNodeCdataValue(node);
	        FileUtils.writeTextFile("images/" + docNo + ".response.coded.tiff", coded);	
	        
	       InputStream is = new FileInputStream("images/" + docNo + ".response.coded.tiff");
	       OutputStream os = new FileOutputStream("images/" + docNo + ".response.tiff");
	       
	       (new Base64Decoder(is, os)).process();
	        
	        //String decoded = decodeBase64(coded);
	        //FileUtils.writeTextFile("images/" + docNo + ".response.tiff", decoded);	        
		}*/
		
	}

	
	/*private static class TestQuery implements Runnable{
		
		private String name;
		
		public TestQuery(String name){
			this.name = name;
			(new File(name)).mkdirs();
		}
		
		public static int getWaitTime(int lastVal){
			
			if(lastVal < 5){
				return 5;
			}
			if(lastVal < 10){
				return 10;
			}
			if(lastVal < 20){
				return 20;
			}
			if(lastVal <30){
				return 30;
			}
			if(lastVal < 45){
				return 45;
			}
			return 60;
			
		}
		
		public void run(){	
			/*
			long startTime = System.currentTimeMillis();			
			
			try{				
			
				String query = FileUtils.readXMLFile(name + ".xml");
				String result = DaslConnection.getDataSynch(query);			
				
				String idString = StringUtils.extractParameter(result,"<OrderBusinessId>(\\d+)</OrderBusinessId>");
				
				
				if(!idString.matches("\\d+")){
					FileUtils.writeTextFile(name + "/" + name + "_Response_Single.xml", result);
					return;
				} else {
					FileUtils.writeTextFile(name + "/" + name + "_Response_First.xml", result);
				}
				
				int orderId = Integer.valueOf(idString);
				
				int waitTime = 0;
				String prevFileName = null;
				String prevResult = "";
				String fileName = null;
				
				for(int i=0; i<240; i++){
				
					result = DaslConnection.getOrder(orderId);
					if(result.equals(prevResult)){
						if(prevFileName != null){
							new File(prevFileName).delete();
						}
					}
					prevResult = result;
					long time = (System.currentTimeMillis() - startTime) / 1000;					
					String status = StringUtils.extractParameter(result,"<OrderStatusBusinessId>(\\d+)</OrderStatusBusinessId>");
					logger.info(name + ":" + orderId + ": time:" + time + "status=" + status);
					
					if("".equals(status)){
						FileUtils.writeTextFile( name + "/" + name + "_Response_Final_" + time + ".xml", result);
						
						Node doc = parseXMLDocument(result);
						NodeList nl = xpathQuery(doc, "//ReportBinary");
						for(int j=0; i<nl.getLength(); j++) {

					        Node node = nl.item(j);
					        String coded = getNodeCdataValue(node);
					        String decoded = decodeBase64(coded);
					        FileUtils.writeTextFile(name + "/" + name + "_Response_Final_" + time + ".txt", decoded);	        
						}				
						break;
						
					} else {
						fileName = name + "/" + name + "_Response_" + time + ".xml";
						FileUtils.writeTextFile(fileName, result);
					}
					// wait 1 minute
					waitTime = getWaitTime(waitTime);
					logger.info(name + ":" + orderId + ": wait " + waitTime + " seconds ...");
					Thread.sleep(waitTime * 1000);
					
					prevFileName = fileName;
				}					
				
			}catch(Exception e){
				logger.error(e);
				throw new RuntimeException(e);
			}
		}
	}
	
	public static void main2(String[] args) throws Exception {
		/*
		org.apache.log4j.BasicConfigurator.configure();
		
		//String testNames[] = {"GeneralIndex_Business", "GeneralIndex_Individual_DateDocFilter", "GeneralIndex_Individual", "GeneralIndex_Trust"};
		//String testNames[] = {"31-36-412-039-0000", "31-36-412-039-0000", "14-07-115-046-0000", "15-15-310-014-0000", "name1", "name2" };
		//String testNames[] = {"14-07-115-046-0000.date-filter"};
		//String testNames[] = {"32-29-103-014-0000"};
		//String testNames[] = {"14-07-115-046-0000.doctype-filter"};
		//String testNames[] = {"name1.doc-filter","14-07-115-046-0000.doc-filter"};
		//String testNames[] = {"trust-2"};
		String testNames[] = {
				"14-07-115-046-0000",
				"14-07-115-046-0000.date-filter",
				"14-07-115-046-0000.doc-filter",
				"15-15-310-014-0000",
				"31-36-412-039-0000",
				"32-29-103-014-0000",
				"name1",
				"name1.doc-filter",
				"name2",
				"trust-1",
				"trust-2"
		};
		
		for(String testName: testNames){
			Thread t = new Thread(new TestQuery(testName));
			t.start();
		}*/
	//}

	//static String prefix = "GeneralIndex_Individual";
	/*static String prefix = "APN_Search";
	static String REQUEST_FILE_NAME = prefix + ".xml";
	static String RESULT_FILE_NAME =  prefix + "_Response.xml";
	static String RESULT_FILE_NAME2 =  prefix + "_Response.txt";

	public static void main(String[] args) {
		
		org.apache.log4j.BasicConfigurator.configure();
		
		String result = DaslConnection.executeQuery(FileUtils.readXMLFile(REQUEST_FILE_NAME));
		FileUtils.writeTextFile(RESULT_FILE_NAME, result);
		
		//String result = FileUtils.readXMLFile(RESULT_FILE_NAME);
		
		Node doc = parseXMLDocument(result);
		NodeList nl = xpathQuery(doc, "//ReportBinary");
		for(int i=0; i<nl.getLength(); i++) {

	        Node node = nl.item(i);
	        String coded = getNodeCdataValue(node);
	        String decoded = decodeBase64(coded);
	        FileUtils.writeTextFile(RESULT_FILE_NAME2, decoded);	        
		}
		
	}

 */
	/**
	 * 
	 * @param input
	 * @return
	 * @throws RuntimeException
	 */
	public static String decodeBase64(String input) throws RuntimeException{
		try{
			return (new Base64Decoder(input)).processString();
		}catch(Base64FormatException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Parses an XML string
	 * @param string
	 * @return parent Node
	 * @throws RuntimeException in case anything went wrong
	 */
	public static Node parseXMLDocument(String string) throws RuntimeException  {
		try{
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Node doc = builder.parse(new ByteArrayInputStream(string.getBytes()));
			return doc;
		}catch(SAXException e){
			logger.error(e);
			throw new RuntimeException(e); 
		}catch(IOException e){
			logger.error(e);
			throw new RuntimeException(e);
		} catch(ParserConfigurationException e){
			logger.error(e);
			throw new RuntimeException(e);
		}		
	}
	
	/**
	 * Apply xpath over an XML  
	 * @param doc XML document
	 * @param query XPATH query
	 * @return list of nodes that match the query
	 * @throws RuntimeException
	 */
	public static NodeList xpathQuery(Node doc, String query) throws RuntimeException {
		try{
			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			XPathExpression expr = xpath.compile(query);
			NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
			return nodes;
		}catch(XPathExpressionException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * get value of a node
	 * @param node
	 * @return value
	 */
	private static String getNodeCdataValue(Node node){
		Node child = node.getFirstChild();
		if(child == null){ 
			return ""; 
		} else if(child.getNodeType() != Node.CDATA_SECTION_NODE && child.getNodeType() != Node.TEXT_NODE){
			return "";
		} else {
			return child.getNodeValue().trim();
		}
	}
	
	/**
	 * get value of a node
	 * @param node
	 * @return value
	 */
	private static String getNodeTextValue(Node node){
		Node child = node.getFirstChild();
		if(child == null){ 
			return ""; 
		} else if(child.getNodeType() != Node.TEXT_NODE){
			return "";
		} else {
			return child.getNodeValue().trim();
		}
	}
}
