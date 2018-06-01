package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.XmlUtils.applyTransformation;
import static ro.cst.tsearch.utils.XmlUtils.xpathQuery;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http.HTTPSiteManager;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.SaveSearchException;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.XMLExtractor;
import ro.cst.tsearch.extractor.xml.XMLUtils;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.loadBalServ.LBNotification;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.titledocument.abstracts.Chapter;
import ro.cst.tsearch.titledocument.abstracts.FidelityTSD;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.utils.XStreamManager;
import ro.cst.tsearch.utils.XmlUtils;
import ro.cst.tsearch.utils.ZipUtils;

public class ASKServer extends TSServer{
	
	private static final long serialVersionUID = 1L;

	private static final Category logger = Category.getInstance(ASKServer.class.getName());
	
	private String originalAskResponse						= null;
	private String askResponse	 							= null;
	private Node xmlDocument 								= null;
	private Search search									= null;
	
	private static final int RO_TYPE = 10;
    private static final int TX_TYPE = 20;
    
    private static final String CLASSES_FOLDER  = BaseServlet.REAL_PATH + /*File.separator + */"WEB-INF" + File.separator + "classes" + File.separator;
    private static final String RESOURCE_FOLDER = CLASSES_FOLDER + "resource" + File.separator + "ASK" + File.separator; 
    private static final String RULES_FOLDER    = CLASSES_FOLDER + "rules";
	
    private static final String TITLEDOC_STYLESHEET_FILE_NAME = RESOURCE_FOLDER + "titledoc_stylesheet.xsl";
    private static final String TAXINFO_STYLESHEET_FILE_NAME  = RESOURCE_FOLDER + "taxinfo_stylesheet.xsl";       
	
	private static final String RO_RULES_FILENAME = RULES_FOLDER + File.separator + "ASK-Title.xml";
	private static final String TX_RULES_FILENAME = RULES_FOLDER + File.separator + "ASK-Tax.xml";
	
    /**
	 * Parsing rules
	 */
	protected transient Document roRules = null;
	protected transient Document txRules = null;
	
	/**
	 * XSLT Stylesheets 
	 */
	protected transient String titleStyle = null;
	protected transient String taxStyle   = null;  
	
	/**
	 * Send XML order to the ASK server 
	 * @param order
	 * @param address
	 */
	public static void sendXmlOrder(String order, String address) {

		String ip = getIp(address);
		int port = getPort(address);

		PostMethod method = new PostMethod(address);
		method.setRequestEntity(new StringRequestEntity(order));
		method.addRequestHeader("Pragma", "no-cache");
		method.addRequestHeader("Content-Type", "text/xml");
		HostConfiguration hostConfiguration = new HostConfiguration();
		hostConfiguration.setHost(ip, port);		
		
		try {
			HttpConnection conn = new HttpConnection(hostConfiguration);
			conn.open();
			method.execute(new HttpState(), conn);
			//String[] params = new String[2];
			//params[0] = "ASK Order Sent";
			//params[1] = "Order sent to " + ip + ":" + port + "(" + address + ")" + "\n" + order;
			//LBNotification.sendNotification(LBNotification.MISC_MESSAGE, null, params);
			logger.info("ASK order sent to " + address);
			
		} catch (Exception e) {
			String[] params = new String[2];
			params[0] = "ASK Order Send ERROR";
			params[1] = "Order was not sent to " + ip + ":" + port + "(" + address + ")";
			LBNotification.sendNotification(LBNotification.MISC_MESSAGE, null, params);
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 * @param mid
	 */
	public ASKServer(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink,long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,searchId, mid);
		asynchronous = true;
	}
			
	@SuppressWarnings("unchecked")
	public void process(String origAskResponse){
		
		originalAskResponse = origAskResponse;		
		if (origAskResponse.length() == 0){
			sendError("Empty string received from ASK!");
			return;
		}		
			
		// replace all tags to lower case to avoid incoherences
		Pattern p = Pattern.compile("(<.+?>)");
		Matcher m = p.matcher(origAskResponse);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
		     m.appendReplacement(sb, m.group(1).toLowerCase());
		 }
		 m.appendTail(sb);
		askResponse = sb.toString();
		
		// parse the document
		xmlDocument = XmlUtils.parseXml(askResponse);
		
		try{			
			
			// identifies the search corresponding to order response
			long searchId = getSearchIdFromResponse();
			if (searchId == -1){
				String error = "ASK File ID not provided or incorrect!";
				sendError(error);
				logger.error(error);				
				return;
			}
			this.searchId = searchId;
			

			logger.info("Response received from ASK.");
			
			// load the search
			search = loadSearch(searchId);
			if (search == null){
				String error = "Error: unable to identify search for searchID=" + String.valueOf(searchId);
				sendError(error);
				logger.error(error);
				SearchLogger.info(error + "<br/>", searchId);
				return;
			}
			
			SearchLogger.info("Response received from <font color=\"blue\">ASK</font>.<br/>", searchId);			
			SearchLogger.info("Search opened.<br/>", searchId);
			logger.info("Search opened: " + searchId);
			
			// extracts documents list from response and parse them
			ServerResponse sr = parseDocumentList();
						
			// save documents
		    for (ParsedResponse pr: (Vector<ParsedResponse>)sr.getParsedResponse().getResultRows()) {
		    	try{
		    		String fileName = (String) pr.getAttribute(ParsedResponse.ASK_FNAME);
		    		int typeInt = (Integer) pr.getAttribute(ParsedResponse.ASK_TYPE);
		    		String type = "Register";
		    		if(typeInt == TX_TYPE){
		    			type = "County Tax";
		    		}		    				    		
		    		saveDocument(pr, type, fileName);		    		
		    	}catch(RuntimeException e){
		    		e.printStackTrace();
		    		String error = "Error saving doc!";
		    		logger.error(error, e);
		    		SearchLogger.info(error + "<br/>", searchId);
		    	}		    	
		    }
						
		} catch (Exception e) {
			String error = "Error processing ASK response!";
			sendError(error + e);
			SearchLogger.info(error + "<br/>", searchId);
			logger.error(error + ":" + originalAskResponse , e);
		}

		// save search
		try{
			Search.saveSearch(search);		
			SearchLogger.info("Search saved after processing <font color=\"blue\">ASK</font> response.", searchId);
			logger.info("Search saved after procesing ASK response: " + searchId);
		} catch(SaveSearchException e){
			String error = "Error saving search after processing <font color=\"blue\">ASK</font> response!";
			sendError(error + e);
			SearchLogger.info(error + "<br/>", searchId);
			logger.error("Error saving search after processing ASK response!", e);			
		}
		
		SearchLogger.info("<br/><hr/>", searchId);
	}
	
	
	/**
	 * Save a document in the current search
	 * @param pr parsed response
	 * @param type document type: Register, County Tax, 
	 * @param fileName the file name, without extension and path
	 */
	@SuppressWarnings("unchecked")
	protected void saveDocument(ParsedResponse pr, String type, String instName){
		
		mSearch = search;
		
		// make sure the folder exists
		(new File(search.getSearchDir() + type)).mkdirs();
		
		// fix image link
		String html = pr.getResponse();
		
		//<a href="http://www.ask-services.com/eService/?MODE=DisplayImageFile$uid=5601$pw=pepsi$ALIAS=SC_PIN$RECORD=50872"><b>View image</b></a>
		if(html.contains("<b>View image</b>")){
			
			String linkRegex = "(?i)<a href=\"(http://[^\"]+)\"><b>View image</b></a>";	
			String link = CreatePartialLink(TSConnectionURL.idGET)  + StringUtils.extractParameter(html, linkRegex);
			
			String ext = ".tiff";

			// rewrite html link
			html = html.replaceAll(linkRegex, "@@@@");
			html = html.replace("@@@@", "<a href='" + link + "&imgfname=" + instName + ext + "'><b>View image</b></a>");
			
		}
		
		// save file to disk
		String fileName = search.getSearchDir() + type + File.separator + instName + ".html";
		FileUtils.writeTextFile(fileName, "<html><body>" + html + "</body></html>");
		
		// add to RO docs
		if("Register".equals(type)){
			search.addRODoc(fileName);
		}
		
		// add to the list of saved docs
	    if(!fileName.contains("Tax") && !fileName.contains("Assessor")){                        	
	        Vector<SaleDataSet> sales = (Vector<SaleDataSet>) pr.infVectorSets.get("SaleDataSet");                        	
	        if(sales != null) for(SaleDataSet sds: sales){                        		
	            String inst = sds.getAtribute("InstrumentNumber");
	            String book = sds.getAtribute("Book");
	            String page = sds.getAtribute("Page");                        		
	            if(!StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)){
	                search.addSavedInst(book + "_" + page);
	            }                        		
	            if(!StringUtils.isEmpty(inst)){
	            	search.addSavedInst(inst);
	            }                        		
	        }
	    } 
		
	    // bootstrap 
	    if("County Tax".equals(type)){
	    	Vector<PropertyIdentificationSet> props = (Vector<PropertyIdentificationSet>) pr.infVectorSets.get("PropertyIdentificationSet");
	    	if(props != null)for(PropertyIdentificationSet pis: props){
	    		boostrapAddress(search, pis);
	    	}
	    }
	    
	    // create and add "special" chapters to chapter map
	    if("County Tax".equals(type)){
	    	
	    	/*Chapter c = (Chapter)FidelityTSD.newCountyTaxChapter(search.getSa(), pr);
	    	
	    	// set src type	    	
	    	c.setSRCTYPE("TX");
	    	
	    	// set instrument number
	    	Vector<PropertyIdentificationSet> pisVect = (Vector<PropertyIdentificationSet>) pr.infVectorSets.get("PropertyIdentificationSet");
	    	if(pisVect != null && pisVect.size() != 0){
	    		String pid = pisVect.get(0).getAtribute("ParcelID");
	    		if(!StringUtils.isEmpty(pid)){
	    			c.setINSTNO(pid);
	    		}
	    	}
	    	search.getChaptersMap().put(fileName, c);*/	    		
	    }
	    
	    SearchLogger.info("Saved doc: " + instName + " type: " + type + ".<br/>", searchId);
	    logger.info("Saved doc: " + instName + " type: " + type + ".");
	}
	
	/**
	 * Bootstrap address
	 * @param search
	 * @param pis
	 */
	private static void boostrapAddress(Search search, PropertyIdentificationSet pis) {

    	SearchAttributes sa = search.getSa();
    	    	
    	String newStNo = pis.getAtribute("StreetNo").toUpperCase();
    	String newStName = pis.getAtribute("StreetName").toUpperCase();
    	
    	// bootstrap if we have both street name and number
    	if(!StringUtils.isEmpty(newStNo) && !StringUtils.isEmpty(newStName)){

    		String origStNo = sa.getAtribute(SearchAttributes.P_STREETNO).toUpperCase();
        	String origStName = sa.getAtribute(SearchAttributes.P_STREETNAME).toUpperCase();

    		// set the ptentially new street number        	
    		if(!newStNo.equals(origStNo)){
    			sa.setAtribute(SearchAttributes.P_STREETNO, newStNo);
    		}
    		
    		// extract street name
    		StandardAddress stdAddress = new StandardAddress((newStNo + " " + newStName).toUpperCase().trim());    		
    		String newStNa = stdAddress.getAddressElement(StandardAddress.STREET_NAME);
    		
    		// set the new street name
    		if(!newStNa.equals(origStName)){
    			sa.setAtribute(SearchAttributes.P_STREET_FULL_NAME, newStName);    			
    		}			
    	}

    }
	
	/**
	 * 
	 * @param doc
	 * @param type
	 * @param addr
	 * @return
	 */
	private Node getDocument(Node doc, String type, String addr){
		
		String docStr = XmlUtils.createXML(doc);
		StringBuilder fullDoc = new StringBuilder("<?xml version=\"1.0\"?>");
		
		fullDoc.append("<order>");
		
		// Non plats
		if (!type.equals("platmap")){
			if (addr != null)
				fullDoc.append(addr);
			// Non taxes
			if (!type.equals("taxes")){			
				fullDoc.append("<").append(type).append(">");
				fullDoc.append(docStr);
				fullDoc.append("</").append(type).append(">");
			// Taxes	
			} else {
				NodeList nl = xpathQuery(xmlDocument, "//taxmaps");
				if (nl != null && nl.getLength() != 0){
					fullDoc.append(XmlUtils.createXML(nl.item(0)));
				}
				fullDoc.append(docStr);
			}
		// Plats	
		} else {
			fullDoc.append(docStr);
		}
		fullDoc.append("</order>");
		
		return XmlUtils.parseXml(fullDoc.toString());
	}
	
	/**
	 * 
	 * @param type
	 * @param addr
	 * @return
	 */
	private List<Node> getTypeDocuments(String type, String addr){
		
		List<Node> allDocs = new ArrayList<Node>();
		NodeList nl = xpathQuery(xmlDocument, "//" + type);
		if (nl != null && nl.getLength() != 0){
			NodeList docs = null;
			if (!type.equals("taxes") && !type.equals("platmap")){
				docs = xpathQuery(xmlDocument, "//" + type + "/document");
			} else {
				docs = nl;
			}	
			if (docs != null){
				int len = docs.getLength();
				for (int i=0; i<len; i++){
					allDocs.add(getDocument(docs.item(i), type, addr));
				}	
			}
		}
		return allDocs;
	}
	
	/**
	 * 
	 * @param node
	 * @param doctype
	 * @return
	 */
	private String getFileName(Node node, int doctype){
		String fileName = "";
		
		if (doctype != 5){ // for all doc types except taxes extract book&page or instr#
			String instr = "", liber = "", page = "";
			NodeList libers = xpathQuery(node, "//liber");
			if (libers != null &&  libers.item(0) != null){
				liber = XmlUtils.getNodeCdataOrText(libers.item(0));
				NodeList pages = xpathQuery(node, "//page");
				if (pages != null &&  pages.item(0) != null){
					page = XmlUtils.getNodeCdataOrText(pages.item(0));
				}
			}
			if (!"".equals(liber) && !"".equals(page)){
				fileName = liber+"_"+page; 
			} else {
				NodeList instrs = xpathQuery(node, "//instrument");
				if (instrs != null &&  instrs.item(0) != null){
					instr = XmlUtils.getNodeCdataOrText(instrs.item(0));
					if (!"".equals(instr)){
						fileName = instr;
					}
				}
			}
		} else { // for taxes extract PIN
			String pin = "";
			NodeList pins = xpathQuery(node, "//taxid");
			if (pins != null &&  pins.item(0) != null){
				pin = XmlUtils.getNodeCdataOrText(pins.item(0));
				if (!"".equals(pin)){
					fileName = pin;
				}
			}
		}
		
		if("".equals(fileName)){
			fileName = "UNKNOWN-" + XmlUtils.createXML(node).hashCode();
			String warn = "Cannot decide instrument number! Used '" + fileName + "' as instrument.";
			logger.warn(warn + ":" +  XmlUtils.createXML(node));
			SearchLogger.info(warn, searchId);
		}
        		
		fileName = fileName.replaceAll("[^-\\w]", "-");
		
		return fileName;
	}
	
	/**
	 * Extract all documents from response. Create and populate the server response.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private ServerResponse parseDocumentList() {
		
		// load rules
		loadRules();
		
		// get Order/address - it will be appended to each doc
		NodeList addr = xpathQuery(xmlDocument, "/order/address");
		String addrStr = null;
		if (addr != null && addr.item(0) != null){
			addrStr = XmlUtils.createXML(addr.item(0));
			NodeList jurisdiction = xpathQuery(xmlDocument, "/order/jurisdiction");
			if (jurisdiction != null && jurisdiction.item(0) != null){
				addrStr += XmlUtils.createXML(jurisdiction.item(0));
			}
		}
				
		// extract all documents from response: Right_of_Access, Riparian, Deeds, Mortgage_Information, Additional_Documents, Taxes, Plats
		List<Node> roas = null;
		List<Node> ripars = null;
		List<Node> deeds = null;
		List<Node> mtgs = null;
		List<Node> adds = null;
		List<Node> tax = null;
		List<Node> plats = null;
		try{
			roas = getTypeDocuments("right_of_access", addrStr);
			ripars = getTypeDocuments("riparian", addrStr);
			deeds = getTypeDocuments("deeds", addrStr);
			mtgs = getTypeDocuments("mortgage_information", addrStr);
			adds = getTypeDocuments("additional_documents", addrStr);
			tax = getTypeDocuments("taxes", addrStr);	
			plats = getTypeDocuments("platmap", addrStr);
		} catch(RuntimeException e){
    		logger.error("ASK parsing exception", e);
    		SearchLogger.info("ASK parse exception", searchId);
    	}
		
		// create and populate the server response
    	ServerResponse sr = new ServerResponse();                    	
        Vector<ParsedResponse> parsedRows = new Vector<ParsedResponse>();
        List[] nlists = new List[]{roas, ripars, deeds, mtgs, adds, tax, plats};
		Integer types[] = new Integer[]{RO_TYPE, RO_TYPE, RO_TYPE, RO_TYPE, RO_TYPE, TX_TYPE, RO_TYPE};
        for(int i=0; i<nlists.length; i++){
        	for (Node node: (List<Node>)nlists[i] ){ 
	            	                       	
        		// determine the xml rules and xslt stylesheet
    	    	String style;
    	    	Document rules = null;
    	    	switch(types[i]){
    		    	case TX_TYPE:
    		    		style = taxStyle;
    		    		rules = txRules;
    		    		break;
    		    	case RO_TYPE:
    		    		style = titleStyle;
    		    		rules = roRules;
    		    		break;
    		    	default:
    		    		throw new RuntimeException("type: " + types[i] + " not known!");
    	    	}
        	    	
    	    	// create the parsed response
        		ParsedResponse item = new ParsedResponse();

    	    	// set html rendering    	    	
    	    	String itemHtml = applyTransformation(node, style);    	
    	    	item.setResponse(itemHtml);
        	    	
    	    	// fill infsets
    	    	XMLExtractor.parseXmlDoc(item, (Document)node, rules, search.getSearchID());
        	    	
    	    	// get document type
    	    	String docType = "";
    	    	for(int k=0; k<item.getSaleDataSetsCount(); k++){
    	    		docType = item.getSaleDataSet(k).getAtribute("DocumentType");
    	    		if(!StringUtils.isEmpty(docType)){
    	    			break;
    	    		}
    	    	}
    	    	docType = docType.replaceAll("\\s*","");
    	    	
	            // set filename          
        		String fileName = getFileName(node, i) + docType;
    	    	item.setAttribute(ParsedResponse.ASK_FNAME, fileName);	    	
				item.setAttribute(ParsedResponse.ASK_TYPE, types[i]);        	
        	            	    
				// get the image link
				NodeList link = xpathQuery(node, "//tif_url");
				if (link != null && link.item(0) != null){
					item.addImageLink(new ImageLinkInPage(XmlUtils.getNodeCdataOrText(link.item(0)) + "&imgfname=" + fileName + ".tiff", fileName + ".tiff"));
				} else {
					link = xpathQuery(node, "//file_url");
					if (link != null && link.item(0) != null){
						item.addImageLink(new ImageLinkInPage(XmlUtils.getNodeCdataOrText(link.item(0)) + "&imgfname=" + fileName + ".tiff", fileName + ".tiff"));
					}
				}
				
				// add to vector of parsed responses
	            parsedRows.add(item);
		            
	        }
        }
        sr.getParsedResponse().setResultRows(parsedRows);
        sr.setResult("");
		
		return sr;
	}
		
	/**
	 * 
	 * @return
	 */
	private long getSearchIdFromResponse(){
		
		if (askResponse.length() == 0 || xmlDocument == null)
			return -1;
		
		// extract fileID value
		NodeList node = XmlUtils.xpathQuery(xmlDocument, "//customer_number");
		if(node == null || node.item(0) == null)
			return -1;
		String fileID = XmlUtils.getNodeCdataOrText(node.item(0));
		if (fileID.length() == 0)
			return -1;
		
		int idStart = fileID.lastIndexOf('_');
		if (idStart == -1)
			return -1;
		
		String searchIdStr = fileID.substring(idStart+1); 
		if (searchIdStr.length() == 0)
			return -1;
		
		long searchId;
		try {
			searchId = Long.parseLong(searchIdStr);
		} catch (Exception e){
			searchId = -1;
		}	
		return searchId;
	}

	/**
	 * load rules from disk
	 */
	private void loadRules(){
		
		// do not load twice
		if(roRules != null){ 
			return; 
		}
		
		// load
		try{
			roRules = XMLUtils.read(new File(RO_RULES_FILENAME), RULES_FOLDER);
			txRules = XMLUtils.read(new File(TX_RULES_FILENAME), RULES_FOLDER);
			titleStyle = FileUtils.readTextFile(TITLEDOC_STYLESHEET_FILE_NAME);
			taxStyle   = FileUtils.readTextFile(TAXINFO_STYLESHEET_FILE_NAME);  			
		}catch(Exception e){
			throw new RuntimeException("Error reading rules!");
		}		
	}	
	
	/**
	 * Extract IP from an address 
	 * @param address
	 * @return
	 */
	private static String getIp(String address) {
		String ip = address;
		if (ip.indexOf("http://") >= 0)
			ip = ip.substring(ip.indexOf("http://") + 7, ip.length());
		if (ip.indexOf(":") > 0)
			ip = ip.substring(0, ip.indexOf(":"));
		else if (ip.indexOf("/") > 0)
			ip = ip.substring(0, ip.indexOf("/"));
		return ip;
	}

	/**
	 * Extract port from an address
	 * @param address
	 * @return
	 */
	private static int getPort(String address) {
		String port = "80";
		if (address.indexOf("http://") >= 0)
			address = address.substring(address.indexOf("http://") + 7, address.length());
		if (address.indexOf(":") > 0) {
			if (address.indexOf("/") > 0)
				port = address.substring(address.indexOf(":") + 1, address.indexOf("/"));
			else
				port = address.substring(address.indexOf(":") + 1);
		}
		try {
			return Integer.parseInt(port);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 80;
	}
	

	private static Search loadSearch(long searchId) {
		return SearchManager.getSearchFromDisk(searchId);
	}

	
	/**
	 * 
	 * @param msg
	 */
	private void sendError(String msg){
		msg = msg + "\n" + originalAskResponse;
		String from = MailConfig.getMailFrom();
		String support = MailConfig.getExceptionEmail();
		Util.sendMail(from, support, null, null, "ASK error", msg);	
		System.err.println(msg);
		logger.error(msg);
	}
	
	/**
	 * Never used
	 */
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {	
	}

	/**
	 * Never used
	 */
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		serverInfo.setModulesForAutoSearch(l);
	}

	/**
	 * Never used
	 */
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo tsServerInfo = new TSServerInfo(1);
		return tsServerInfo;
	}
	
	@Override
    public ServerResponse GetLink(String vsRequest, boolean vbEncoded) throws ServerResponseException {
		
		String link = StringUtils.extractParameter(vsRequest, "Link=(.*)");
		String imgFname = StringUtils.extractParameter(vsRequest, "[?&]imgfname=(.*)");
		
		if(StringUtils.isEmpty(link) || StringUtils.isEmpty(imgFname)){
			return super.GetLink(vsRequest, vbEncoded);
		}
		link = link.replaceFirst("[&?]imgfname=.*", "");
				
		
     	// construct fileName
    	String folderName = getCrtSearchDir() + "Register" + File.separator;
		new File(folderName).mkdirs();
    	String fileName = folderName + imgFname;
    	
    	// retrieve the image
    	for(int i=0; i<2; i++){
    		if(retrieveImage(link, fileName)){   		
    			break;
    		}
    	}
    	
		// write the image to the client web-browser
    	String contentType = "image/tiff";
		boolean imageOK = writeImageToClient(fileName, contentType);
		
		// image not retrieved
		if(!imageOK){ 
	        // return error message
			ParsedResponse pr = new ParsedResponse();
			pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
			throw new ServerResponseException(pr);			
		}
		
		// return solved response
		return ServerResponse.createSolvedResponse(); 
		
	}
	
	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
		String link = image.getLink().replaceFirst("[&?]imgfname=.*", "");
    	// retrieve the image
    	for(int i=0; i<2; i++){
    		if(retrieveImage(link, image.getPath())){
    			byte b[] = FileUtils.readBinaryFile(image.getPath());
    			return new DownloadImageResult(DownloadImageResult.Status.OK, b, image.getContentType());
    		}
    	}
    	return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
	}
	
	/**
	 * 
	 * @param link
	 * @param filename
	 * @return
	 */
	private boolean retrieveImage(String link, String  fileName){
		
		FileUtils.CreateOutputDir(fileName);
		
		if(FileUtils.existPath(fileName)){
			return true;
		}
		
		HTTPSiteInterface site = HTTPSiteManager.pairHTTPSiteForTSServer(
				"ILCookIM", 
				searchId, 
				(int)TSServersFactory.getSiteId("IL", "Cook", "IM"));
		
		// download the image
		HTTPResponse httpResponse = site.process(new HTTPRequest(link));
		
		httpResponse.getContentLenght();
		httpResponse.getContentType();
		
    	FileUtils.writeStreamToFile(httpResponse.getResponseAsStream(), fileName);
    	
		return FileUtils.existPath(fileName);
	}
	
}
