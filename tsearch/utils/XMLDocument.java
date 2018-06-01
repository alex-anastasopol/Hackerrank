package ro.cst.tsearch.utils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class XMLDocument {

	Node doc;
	protected XPath xpath;
	
	public XMLDocument(String fileName) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
		
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		//domFactory.setNamespaceAware(true); // never forget this!
		DocumentBuilder builder = domFactory.newDocumentBuilder();

		doc = builder.parse( new FileInputStream(fileName) );
		
		// create xpath instance
		XPathFactory factory = XPathFactory.newInstance();
		xpath = factory.newXPath();
		
	}
	
	public XMLDocument(InputStream is) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
		
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		//domFactory.setNamespaceAware(true); // never forget this!
		DocumentBuilder builder = domFactory.newDocumentBuilder();

		doc = builder.parse(is);
		
		// create xpath instance
		XPathFactory factory = XPathFactory.newInstance();
		xpath = factory.newXPath();
	}
	
	public XMLDocument(Node node) {
		
		doc = node;
		
		// create xpath instance
		XPathFactory factory = XPathFactory.newInstance();
		xpath = factory.newXPath();
	}
	
	/*
	 * Get first record matching the xpath query parameter
	 */
	public XMLRecord getFirst( String query ) throws XPathExpressionException {
		
		XPathExpression expr = xpath.compile(query);
		
		NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		
		if (nodes.getLength() > 0)
			return new XMLRecord( nodes.item(0) ) ;
		
		return null;
	}
	
	/*
	 * Get all records matching the xpath query parameter
	 */
	public XMLRecord[] getAll( String query ) throws XPathExpressionException {
		
		XPathExpression expr = xpath.compile(query);
		
		NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		
		ArrayList<XMLRecord> list = new ArrayList<XMLRecord>();
		
		for (int i = 0; i < nodes.getLength(); i++) {
			
			Node node = nodes.item(i);

			/*ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				Transformer t = TransformerFactory.newInstance().newTransformer();
				t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				t.transform(new DOMSource(node), new StreamResult(out));
			} catch (Exception e) {
				e.printStackTrace();
			}
			String xmlString = out.toString();*/

			XMLRecord record = new XMLRecord(node);
			
			list.add( record );
		}
		
		return list.toArray( new XMLRecord[ list.size() ] );
	}
	
	
	
	
	
	
	
	
	public static void main(String[] args) 
	throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {

		// open xml file
		XMLDocument document = 
			new XMLDocument("WEB-INF\\classes\\com\\stewart\\rei\\webservice\\DASL_Query\\response\\DataTrace_Request_By_Owner_Name_Response.txt");
		
		// query xml
		String query = "//TitleRecord";
		
		XMLRecord record = document.getFirst(query);
		
		// use data
		if ( record != null )
			System.out.println( record.toString() );
		else
			System.out.println( "Record not found" );
	}
}
