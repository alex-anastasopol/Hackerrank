package ro.cst.tsearch.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tools.codec.Base64Decoder;
import org.w3c.tools.codec.Base64FormatException;
import org.xml.sax.SAXException;

import ro.cst.tsearch.servers.bean.DASLSimpleInstrumentInfo;
import ro.cst.tsearch.servers.bean.LegalDescription;
import ro.cst.tsearch.servers.response.ParsedResponse;

/**
 * 
 * @author radu bacrau
 */
public class XmlUtils {

	protected static final Logger logger = Logger.getLogger(XmlUtils.class);
	
	private static final XPathFactory factory = XPathFactory.newInstance();
	
	/**
	 * Parses an XML string
	 * @param string
	 * @return parent Node
	 * @throws RuntimeException in case anything went wrong
	 */
	public static Document parseXml(File file) throws RuntimeException  {
		try{
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document doc = builder.parse(new FileInputStream(file));
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
	 * Parses an XML string
	 * @param string
	 * @return parent Node
	 * @throws RuntimeException in case anything went wrong
	 */
	public static Document parseXml(String string, String encoding) throws RuntimeException  {
		try{
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document doc = builder.parse(new ByteArrayInputStream(string.getBytes(encoding)));
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
	 * Parses an XML string
	 * @param string
	 * @return parent Node
	 * @throws RuntimeException in case anything went wrong
	 */
	public static Document parseXml(String string) throws RuntimeException  {
		
		if (string != null) {
			string = string.replaceAll("/&#x0;", ""); //B 3092
			string = string.replaceAll("&#x0;", "");
			string = string.replaceAll("&#x1;", "");
			string = string.replaceAll("&#x8;", "");
			
		}
		try{
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document doc = builder.parse(new ByteArrayInputStream(string.getBytes()));
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
	 * Create XML text document from Node
	 * @param node 
	 * @return XML text
	 */
	@Deprecated
	public static String createXML(Node node){		

		StringBuilder sb = new StringBuilder();
		String name = node.getNodeName();
		String value = getNodeValue(node);

		if(!"".equals(value)){
			sb.append("<" + name + ">");sb.append(StringEscapeUtils.escapeXml(value)); sb.append("</" + name + ">");
		} else{
			sb.append("<" + name + ">");
			for(Node child: getChildren(node)){
				if(child.getNodeType()!=Node.TEXT_NODE)
					sb.append(createXML(child));

			}
			sb.append("</" + name + ">");
		}
		return sb.toString();
	}
	
	/**
	 * Create String representation of a document without deleting attributes
	 * @param node
	 * @return
	 */
	public static String createXMLString(Node node, boolean omitXMLDeclaration){
		
		StringWriter sw = new StringWriter();
		
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			if (omitXMLDeclaration){
				t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			}
			t.transform(new DOMSource(node), new StreamResult(sw));
			
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}

	    return sw.toString();
	}
	
	/**
	 * Create Html representation of a document
	 * @param node
	 * @return
	 */
	public static String createHtml(Node node){
		return "<pre>" + createHtml(node, "") + "</pre>";
	}
	
	/**
	 * Create Html representation of a node
	 * @param node
	 * @param prefix
	 * @return
	 */
	private static String createHtml(Node node, String prefix){
		
		StringBuilder sb = new StringBuilder();
		String name = node.getNodeName();
		String value = getNodeValue(node);

		if(!"".equals(value)){
			sb.append(prefix);
			sb.append("<font color=\"blue\">&lt;" + name + "&gt;</font>");
			sb.append(StringEscapeUtils.escapeXml(value));
			sb.append("<font color=\"blue\">&lt;/" + name + "&gt;</font>");
			sb.append("<br/>");
		} else{			
			sb.append(prefix);
			if(node.getFirstChild() == null){
				sb.append("<font color=\"blue\">&lt;" + name + "/&gt;</font><br/>");
			} else {
				sb.append("<font color=\"blue\">&lt;" + name + "&gt;</font>");
				sb.append("<br/>");
				String newPrefix = prefix + "    ";
				for(Node child: getChildren(node)){
					if(child.getNodeType()!=Node.TEXT_NODE)
						sb.append(createHtml(child, newPrefix));
				}
				sb.append(prefix);
				sb.append("<font color=\"blue\">&lt;/" + name + "&gt;</font>");
				sb.append("<br/>");
			}
		}
		return sb.toString();		
	}
	
	/**
	 * Create Document from Node
	 * @param node
	 * @return Document
	 * TODO: right now it turns node into text, then parses it. There has to be a better way
	 */
	public static Document createDocument(Node node){
		return parseXml("<?xml version=\"1.0\"?>" + createXML(node));
	}
	
	/**
	 * get value of a node, either the CDATA od the text
	 * @param node
	 * @return value
	 */
	public static String getNodeCdataOrText(Node node){
		Node child = node.getFirstChild();
		if(child == null){ 
			return ""; 
		} else if(child.getNodeType() != Node.CDATA_SECTION_NODE && child.getNodeType() != Node.TEXT_NODE){
			return "";
		} else {
			return child.getNodeValue().trim();
		}
	}
	
	
	// Get all e elements directly under an elem1 element
	// xpath = "//elem1/e";
	public static NodeList getNodesForPath(Node node,String xPath){
		NodeList nodelist=null;
		try {
			nodelist = org.apache.xpath.XPathAPI.selectNodeList(node, xPath);
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return nodelist; 
	}
	
	public static NodeList getAllNodesForPath(Node node,String path) {
		return getNodesForPath(node, "//"+path);
	}
	
	/**
	 * Apply xpath over an XML  
	 * @param doc XML document
	 * @param query XPATH query
	 * @return list of nodes that match the query
	 * @throws RuntimeException in case the xpath expression is not valid
	 */
	public static NodeList xpathQuery(Node doc, String query) {
		try{
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
	 * Decodes a Base64 encoded string 
	 * @param input string to be decoded
	 * @return decoded string
	 * @throws RuntimeException in case anything goes wrong
	 */
	public static String decodeBase64(String input) throws RuntimeException{
		InputStream is = new ByteArrayInputStream(input.getBytes());
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try{
			(new Base64Decoder(is, os)).process();		
		} catch(Base64FormatException e){
			throw new RuntimeException(e);
		} catch(IOException e){
			throw new RuntimeException(e);
		}
		return os.toString();		
	}
	
	/**
	 * Decodes a Base64 encoded string 
	 * @param input string to be decoded
	 * @return decoded string
	 * @throws RuntimeException in case anything goes wrong
	 */
	public static byte[]  decodeBase64(byte[] bytes) throws RuntimeException{
		InputStream is = new ByteArrayInputStream(bytes);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try{
			(new Base64Decoder(is, os)).process();	
			return os.toByteArray();
		} catch(Base64FormatException e){
			throw new RuntimeException(e);
		} catch(IOException e){
			throw new RuntimeException(e);
		}finally{
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Decodes a Base64 encoded string into a file
	 * @param input string to be decoded
	 * @param outputFileName name of the output file
	 * @throws RuntimeException in case anything goes wrong
	 */
	public static void decodeBase64(String input, String outputFileName) throws RuntimeException{
		InputStream is = new ByteArrayInputStream(input.getBytes());
		OutputStream os = null;
		try{
			os = new FileOutputStream(outputFileName);
			(new Base64Decoder(is, os)).process();
		} catch(FileNotFoundException e){
			throw new RuntimeException(e);
		} catch(Base64FormatException e){
			throw new RuntimeException(e);
		} catch(IOException e){
			throw new RuntimeException(e);
		} finally {
			try{ if(os != null) {os.close();}} catch(IOException e){}
		}
	}
	
	/**
	 * return an iterable object with all children of a DOM node
	 * @param doc
	 * @return iterable object with all children
	 */
	public static Iterable<Node> getChildren(final Node doc){
		return new Iterable<Node>() {			
			public Iterator<Node> iterator(){ 
				List<Node> nodeList = new ArrayList<Node>();
				Node child = doc.getFirstChild();
				while (child != null){
					nodeList.add(child);
					child = child.getNextSibling();
				}			
				return nodeList.iterator();
			}			
		};
	}
	
	/**
	 * get value of a node
	 * @param node
	 * @return value
	 */
	public static String getNodeValue(Node node){
		Node child = node.getFirstChild();
		if(child == null){ 
			return ""; 
		} else if(child.getNodeType() != Node.TEXT_NODE){
			return "";
		} else {
			return child.getNodeValue().trim();
		}
	}	
	
	/**
	 * Applies a transformation to an XML document
	 * @param document XML document
	 * @param stylesheet XSLT stylesheet
	 * @return result of transformation
	 * @throws RuntimeException
	 */
	public static String applyTransformation(Node document, String stylesheet) throws RuntimeException {
		try{
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer(new StreamSource(new StringReader(stylesheet)));
			StringWriter outputWriter = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(outputWriter));
			return outputWriter.toString();
		}catch(TransformerConfigurationException e){
			throw new RuntimeException(e);
		}catch(TransformerException e){
			throw new RuntimeException(e);
		}
	}
	
    /**
     * Find a node and return its value or CDATA value
     * if several nodes found, last one is taken into consideration
     * @param doc DOM node
     * @param xpath node selection expression
     * @return value of that node - empty string if not found
     */
    public static String findNodeValue(Node doc,String xpath){
    	
    	// find doc node
    	NodeList docNoNodes = xpathQuery(doc, xpath);
    	if(docNoNodes.getLength() != 1){
    		return "";
    	}    	
    	
    	// find doc text
    	String inst = XmlUtils.getNodeCdataOrText(docNoNodes.item(0));
    	
    	
    	return inst;
    }
    
    /**
	 * make a fast query for a simple  XPATH with max 3 levels
	 * accept formats like Instument/Book
	 */
    public static String findFastNodeValue(Node node,String simpleXPATH){
		String a[] = simpleXPATH.split("[/]");
		if(a!=null){
			if(a.length == 1){
				for(Node n1: getChildren(node)){
					if(n1.getNodeName().equals(a[0])){
						return getNodeValue(n1);
					}
				}
			}
			else if( a.length == 2){
				for(Node n1: getChildren(node)){
					if(n1.getNodeName().equals(a[0])){
						for(Node n2: getChildren(n1)){
							if(a[1].equals(n2.getNodeName())){
								return getNodeValue(n2);
							}
						}
					}
				}
			} else if( a.length == 3){
				for(Node n1: getChildren(node)){
					if(n1.getNodeName().equals(a[0])){
						for(Node n2: getChildren(n1)){
							if(a[1].equals(n2.getNodeName())){
								for(Node n3: getChildren(n2)){
									if(a[2].equals(n3.getNodeName())){
										return getNodeValue(n3);
									}
								}
							}
						}
					}
				}
			}
		}		
		return null;
	}
    
    public static Document createDomDocument() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.newDocument();
            return doc;
        } catch (ParserConfigurationException e) {
        }
        return null;
    }

    /**
     * Copies only if it has a result for the sourceQuery and copies only the first from the destinationQuery
     * @param doc
     * @param sourceQuery
     * @param destinationQuery
     */
    public static void moveNode(Node doc, String sourceQuery, String destinationQuery) {
		NodeList sourceNodeList = XmlUtils.xpathQuery(doc, sourceQuery);
		if (sourceNodeList.getLength()==1){
			NodeList legalDescritpionNodes = XmlUtils.xpathQuery(doc, destinationQuery);
			if (legalDescritpionNodes.getLength()==1){
				Element elToBeCopied = (Element)sourceNodeList.item(0);
				Node sourceNode = legalDescritpionNodes.item(0);
				sourceNode.appendChild(elToBeCopied);
			}
		}
	}
    /**
     *  This copies a value from an existing node to a value of a @param newNodeName.If the value already exists it's overwritten
     *  The queries do not start with "/".
     * @param doc
     * @param sourceQuery the path from XML where the node with the tag name @param newNodeName will be put 
     * @param destinationQuery the path from where we get the text value to be copied to  @param newNodeName
     * @param newNodeName
     */
    public static void copyNodeValue(Node doc, String sourceQuery, String destinationQuery, String newNodeName) {
		NodeList sourceNodeList = XmlUtils.xpathQuery(doc, sourceQuery);
		if (sourceNodeList!=null && sourceNodeList.getLength()==1){
			Element elToBeCopied = (Element)sourceNodeList.item(0);
			if(elToBeCopied ==null){
				return;
			}
			Node firstChild =  elToBeCopied.getFirstChild();
			if(firstChild ==null){
				return;
			}
			
			Document document = createDomDocument();
			Element newNode = document.createElement(newNodeName);
			
			String nodeValue =  firstChild.getNodeValue();
			
			newNode.appendChild( document.createTextNode(nodeValue));
			
			NodeList legalDescritpionNodes = XmlUtils.xpathQuery(doc, destinationQuery);
			for (int i=0; i< legalDescritpionNodes.getLength(); i++) {
					Node sourceNode = legalDescritpionNodes.item(i);
					Document destinationDoc = sourceNode.getOwnerDocument();
					Node newElement = destinationDoc.importNode(newNode,true);
					
					NodeList alreadyContainsNode = xpathQuery(sourceNode, destinationQuery+ "/" + newNodeName);
					boolean contains=false;
					for (int j=0; (j< alreadyContainsNode.getLength()) && (!contains);j++){
						Node item = alreadyContainsNode.item(j);
						if (nodeValue.equals(item.getFirstChild().getNodeValue())){
							contains =true;
						}	
					}
					if(!contains){
						  sourceNode.appendChild(newElement);
					}	  
				}
			if (legalDescritpionNodes.getLength()==0){
				NodeList sourceQuerryRoot = xpathQuery(doc, "..");
				Node rootItem = sourceQuerryRoot.item(0);
				Document destinationDoc = rootItem.getOwnerDocument();
				Element newElement = destinationDoc.createElement(destinationQuery);
				Node newElementToAdd = destinationDoc.importNode(newNode, true);
				newElementToAdd.setTextContent(nodeValue);
				newElement.appendChild(newElementToAdd);
				rootItem.appendChild(newElement);
			}
		}
	}
    
    /**
	 * Appends a value from a @param sourceNode with @param sourceQuery to a @param
	 * destinationNode with a @param destinationQuery. If destinationNode doen't
	 * exist it will be created.Works only with single values.The new node value
	 * will be contaied the old value and the new value separated by a
	 * space i.e. "val1 val2". The queries do not start with "/";
	 * 
	 * @param sourceNode
	 * @param destinationNode
	 * @param sourceQuery
	 * @param destinationQuery
	 */
	public static void appendValueToNode(Node sourceNode, Node destinationNode, String sourceQuery,
			String destinationQuery) {
		NodeList sourceNodeList = XmlUtils.xpathQuery(sourceNode, sourceQuery);

		// test that source node has value
		if (sourceNodeList.getLength() == 1) {
			NodeList destinationNodes = XmlUtils.xpathQuery(destinationNode, destinationQuery);
			// we already have value in destination
			Element elToBeAdded = (Element) sourceNodeList.item(0);
			String elToBeAddedValue = elToBeAdded.getFirstChild().getNodeValue();
			String newValue =  elToBeAddedValue;
			if (destinationNodes.getLength() == 1) {
					Element elToBeAppended = (Element) destinationNodes.item(0);
					if (elToBeAppended.hasChildNodes()){
						Node firstChild = elToBeAppended.getFirstChild();
						String elToBeAppendedValue = firstChild.getNodeValue();
						newValue = elToBeAppendedValue + " " + elToBeAddedValue;
					}
				elToBeAppended.setTextContent(newValue);
			} else {//we do not have value in destination so we have to copy it from source
				int lastIndexOf = destinationQuery.lastIndexOf("/");
				String newDestQuery = (lastIndexOf>-1)? destinationQuery.substring(0,lastIndexOf): destinationQuery;
				//check to see if we have the required hierarchy
				Element createElementsForPath = createElementsForPath(destinationNode, newDestQuery);
				createElementsForPath.setTextContent(elToBeAddedValue);
				
			}
		}
	}

	public static Element createElementsForPath(Node root, String xmlPathToBeBuilt){
		int beginIndex = xmlPathToBeBuilt.indexOf("/");
		Document ownerDocument = root.getOwnerDocument();
		if (beginIndex>0){
			String currantTag = xmlPathToBeBuilt.substring(0,beginIndex);
			String substring = xmlPathToBeBuilt.substring( beginIndex+1);
			if (ownerDocument.getElementsByTagName(currantTag).getLength()==0){
				Element currentElem = ownerDocument.createElement(currantTag);
				root.appendChild(currentElem);
				createElementsForPath(root, substring);
				return currentElem;
			}else{
				beginIndex = substring.indexOf("/");
				substring = substring.substring(beginIndex+1);
				return createElementsForPath(root, substring);
				
			}
		}else{
			Element currentElem = ownerDocument.createElement(xmlPathToBeBuilt);
			root.appendChild(currentElem);
			return currentElem; 
		}
	}
	
	public static void mergeLegalDescriptionsNodes(Node sourceNode, Node destinationNode){
		NodeList parties = XmlUtils.xpathQuery(destinationNode, "Instrument/LegalDescriptions");
		NodeList partiesToAdd = XmlUtils.xpathQuery(sourceNode, "Instrument/LegalDescriptions");

		NodeList instrumentNode = XmlUtils.xpathQuery(destinationNode, "Instrument");
		Set<String> alreadyFound = new HashSet<String>();
		for (int i = 0; i < parties.getLength(); i++) {
			Node currentNode = parties.item(i);
			String xmlRepresentation = XmlUtils.createXML(currentNode).toLowerCase();
			if(!alreadyFound.contains(xmlRepresentation)) {
				alreadyFound.add(xmlRepresentation);
			} else {
				if(instrumentNode.getLength() >0  && instrumentNode.item(0) != null) {
					instrumentNode.item(0).removeChild(currentNode);
				}
			}
		}
		
		for (int i = 0; i < partiesToAdd.getLength(); i++) {
			Node currentNode = partiesToAdd.item(i);
			String xmlRepresentation = XmlUtils.createXML(currentNode).toLowerCase();
			if(!alreadyFound.contains(xmlRepresentation)) {
				if(instrumentNode.getLength() >0  && instrumentNode.item(0) != null) {
					instrumentNode.item(0).appendChild(currentNode.cloneNode(true));
				}
				alreadyFound.add(xmlRepresentation);
			}
		}
		
		parties = XmlUtils.xpathQuery(destinationNode, "LegalDescription");
		partiesToAdd = XmlUtils.xpathQuery(sourceNode, "LegalDescription");
		
		alreadyFound.clear();
		for (int i = 0; i < parties.getLength(); i++) {
			Node currentNode = parties.item(i);
			String xmlRepresentation = XmlUtils.createXML(currentNode).toLowerCase();
			if(!alreadyFound.contains(xmlRepresentation)) {
				alreadyFound.add(xmlRepresentation);
			} else {
				destinationNode.removeChild(currentNode);
			}
		}
		
		for (int i = 0; i < partiesToAdd.getLength(); i++) {
			Node currentNode = partiesToAdd.item(i);
			String xmlRepresentation = XmlUtils.createXML(currentNode).toLowerCase();
			if(!alreadyFound.contains(xmlRepresentation)) {
				destinationNode.appendChild(currentNode.cloneNode(true));
				alreadyFound.add(xmlRepresentation);
			}
		}
		
		//mergeLogicalLegalDescriptions(destinationNode);
	}
	
	
	public static DASLSimpleInstrumentInfo loadSimpleResponse(Node doc) {
		DASLSimpleInstrumentInfo simpleResponse = new DASLSimpleInstrumentInfo();
		simpleResponse.setBook(XmlUtils.findFastNodeValue(doc, "Instrument/Book"));
		simpleResponse.setPage(XmlUtils.findFastNodeValue(doc, "Instrument/Page"));
		simpleResponse.setType(XmlUtils.findFastNodeValue(doc, "Instrument/Type"));
		simpleResponse.setIntrumentNo(XmlUtils.findFastNodeValue(doc, "Instrument/DocumentNumber"));
		
		String recordedDate = XmlUtils.findNodeValue( doc, "Instrument/RecordedDate/Date" );
		if( StringUtils.isEmpty(recordedDate) ){
			recordedDate = XmlUtils.findNodeValue( doc, "Instrument/PostedDate/Date" );
			if( StringUtils.isEmpty(recordedDate) ){
				recordedDate = XmlUtils.findNodeValue( doc, "Instrument/RecordedDate" );
			}
		}
		
		simpleResponse.setDateString(recordedDate);
		return simpleResponse;
	}

	
	public static void mergeLogicalLegalDescriptions(Node destinationNode){
		//we have no identic legal description; this was solved before;
		NodeList legalDescriptionNodes = XmlUtils.xpathQuery(destinationNode, "LegalDescription");
		
		Map<LegalDescription,List<Map<String,Node>>> ldMap = new HashMap<LegalDescription,List<Map<String,Node>>>();
		List<Node> nodesToRemove = new ArrayList<Node>();
		//we put all the legal descriptions in ldCollection in order for them to be checked for equality
		for (int i = 0; i < legalDescriptionNodes.getLength(); i++) {
			Node currentNode = legalDescriptionNodes.item(i);
			DASLSimpleInstrumentInfo currentInstrument = loadSimpleResponse(destinationNode);
			LegalDescription currentLegalDescription = loadLegalDescription(currentNode, currentInstrument);
			String currentXmlString = XmlUtils.createXML(currentNode).toLowerCase();
			if (ldMap.get(currentLegalDescription)==null){
				List<Map<String,Node>> l  = new ArrayList<Map<String,Node>>();
				Map<String, Node> hashMap = new HashMap<String,Node>();
				hashMap.put(currentXmlString, currentNode);
				l.add(hashMap);
				ldMap.put(currentLegalDescription, l);
			}else{
				List<Map<String,Node>> l = ldMap.get(currentLegalDescription);
				
				Map<String, Node> map = l.get(0);
				
				Node destNode = (Node) map.values().toArray()[0];
				String query = "LegalDescription/LotBlock/LotThrough";
				XmlUtils.appendValueToNode(currentNode, destNode, query, query);
				query = "LegalDescription/LotBlock/BlockThrough";
				XmlUtils.appendValueToNode(currentNode, destNode, query, query);
				query = "RemarksCopy";
				XmlUtils.appendValueToNode(currentNode, destNode, query, query);
				nodesToRemove.add(currentNode);
			}
		}
		
		//remove the nodes that were merged
		for (Node node : nodesToRemove) {
			destinationNode.removeChild(node);
		}
		
	}
	
	public static LegalDescription loadLegalDescription(Node node, DASLSimpleInstrumentInfo simpleInstrument){
		String propertyID = XmlUtils.findFastNodeValue(node, "PropertyID");
		String acreage = XmlUtils.findFastNodeValue(node, "Acreage");
		String platName = XmlUtils.findFastNodeValue(node, "Plat/Plat_Name");
		String platBook = XmlUtils.findFastNodeValue(node, "Plat/Plat_Book");
		String platPage = XmlUtils.findFastNodeValue(node, "Plat/Plat_Page");
		String lot = XmlUtils.findFastNodeValue(node, "LotBlock/Lot");
		String block = XmlUtils.findFastNodeValue(node, "LotBlock/Block");
		String lotThrough = XmlUtils.findFastNodeValue(node, "LotBlock/LotThrough");
		String blockThrough = XmlUtils.findFastNodeValue(node, "LotBlock/BlockThrough");
		String propertyType = XmlUtils.findFastNodeValue(node, "PropertyType");
		String remarksCopy = XmlUtils.findFastNodeValue(node, "RemarksCopy");
		
//		findFastNodeValue  
		LegalDescription newInstance = LegalDescription.newInstance(simpleInstrument, propertyID, propertyType);
		if(newInstance != null) {
			newInstance.addFirstKey(platBook, platPage, lot, block);
			newInstance.addSecondKey(acreage, platName);
			newInstance.setBlockThrough(blockThrough);
			newInstance.setLotThrough(lotThrough);
		}
		
		return newInstance;
	}
	
	
    /**
     * Replaces unwanted characters in a string that contains xml;
     * e.g. & with &amp;  
     */
    public static String cleanXml(String xmlString){   
    	return xmlString.replaceAll("(?i)&(?!(?:amp|lt|gt|quot|apos|#176))", "&amp;");
    }
}
