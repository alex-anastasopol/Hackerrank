package ro.cst.tsearch.servers.response;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.utils.XmlUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;


public class OCRParsedDataStruct implements Cloneable{
	
	//vector with elements of type StructBookPage
	private Vector<StructBookPage> bookPageVector = null;
	
	//vector with all instrument numbers
	private Vector<Instrument> instrumentList = null;
	
	/**
	 * cache for String legal constructed using the legal vector
	 */
	private volatile String legalDescription = "";
	
	private Vector<String> legalDescriptionVector = null;
	private Vector<String> addressVector = null;
	
	private String xmlContents = "";
	
	private String legalDescImageName = null;
	
	
	private String vestingInfoGrantee = "";
	
	private String vestingInfoGrantor = "";
	
	private Vector<String> mortgageGranteeLender = null;
	private Vector<String> mortgageGranteeTrustee = null;
	private Vector<String> mortgageGrantor = null;
	
	private Vector<String> nameRefGrantee = null;
	private Vector<String> nameRefGranteeTrustee = null;
	private Vector<String> nameRefGrantor = null;
	
	private Vector<String> amountVector = null;
	
	private String loanNumber = null;
	
	private String documentType = "";
	
	private String recordedDate = "";
	private String instrumentDate = "";
	private String instrumentNumber = "";
	private String book = "";
	private String page = "";
		
	private StringBuilder infoCollected = new StringBuilder();
	
	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	
	private boolean insertLink;
	
	private boolean isCondo;
	
	private Vector<String> unitVector = null;
	
	private OCRParsedDataStruct (boolean insertLink){
		nameRefGrantee = new Vector<String>();
		nameRefGranteeTrustee = new Vector<String>();
		nameRefGrantor = new Vector<String>();
		legalDescriptionVector = new  Vector<String>();
		instrumentList = new Vector<Instrument>();
		bookPageVector = new Vector<StructBookPage>();
		amountVector = new Vector<String>();
		mortgageGranteeLender = new Vector<String>();
		mortgageGranteeTrustee = new Vector<String>();
		mortgageGrantor = new Vector<String>();
		this.insertLink = insertLink;
		isCondo = false;
		unitVector = new Vector<String>();
		addressVector = new Vector<String>();
	}
	
	@Deprecated
	public static class StructBookPage{
		public String book;
		public String page;
		private String type;
		
		public StructBookPage(String book,String page){
			this.book=book;
			this.page=page;
			//this.type=type;
		}

		public String getType() {
			if(type==null){
				type="";
			}
			return type;
		}
		
		public void setType(String type) {
			this.type=type;
		}

		public String getBook() {
			return book;
		}

		public String getPage() {
			return page;
		}
		
		@Deprecated
		public InstrumentI toInstrument(){
			com.stewart.ats.base.document.Instrument i = new com.stewart.ats.base.document.Instrument(book,page,"","",SimpleChapterUtils.UNDEFINED_YEAR);
			return i;
		}
		
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append("StructBookPage[");
			buffer.append("book = ").append(book);
			buffer.append(" page = ").append(page);
			buffer.append(" type = ").append(type);
			buffer.append("]");
			return buffer.toString();
		}
	}
	
	public static OCRParsedDataStruct getDataFromXML (String xmlFileName,long searchId, boolean usLinks) throws FileNotFoundException,IOException{
		Document ocrDocument = XmlUtils.parseXml(new File(xmlFileName));
		return getDataFromXML(ocrDocument, searchId, usLinks);
	}
	
	public static OCRParsedDataStruct getDataFromXML (Document ocrDocument,long searchId, boolean usLinks) throws FileNotFoundException,IOException{
		
		OCRParsedDataStruct parsedData	=	new OCRParsedDataStruct (usLinks);
		
		StringWriter writer = new StringWriter();
		try {
			Transformer xformer = TransformerFactory.newInstance().newTransformer();
			Source source = new DOMSource(ocrDocument);
	        xformer.transform(source, new StreamResult( writer ));
		} catch (Exception e) {e.printStackTrace();} 

		parsedData.setXmlContents( writer .toString() );
		parsedData.loadLegalDescription (ocrDocument, searchId);
		parsedData.loadDocumentReferences (ocrDocument, searchId);
		parsedData.loadDocumentType (ocrDocument);
		parsedData.loadVestingInfo (ocrDocument);
		parsedData.loadMortgageInfo (ocrDocument);
		parsedData.loadNameReference (ocrDocument);
		parsedData.loadAmount(ocrDocument);
		parsedData.loadLoanNumber(ocrDocument);
		parsedData.loadDates(ocrDocument);
		parsedData.loadInstrument(ocrDocument);
		parsedData.loadUnits(ocrDocument);
		parsedData.loadAddress(ocrDocument, searchId);
		
		return parsedData;
	}

	public boolean loadXmlData(String xmlFileName, long searchId){

		try {
			Document ocrDocument = XmlUtils.parseXml(new File(xmlFileName));
			
			
			BufferedInputStream xmlFileStream	=	new BufferedInputStream (new FileInputStream(xmlFileName));
			
			int dim 	= 	xmlFileStream.available();
			byte[] bytesData	=	new byte[dim];
			
			xmlFileStream.read(bytesData);
			xmlFileStream.close();
	
			setXmlContents( new String(bytesData) );
			loadLegalDescription (ocrDocument,searchId);
			loadDocumentReferences (ocrDocument, searchId);
			loadDocumentType (ocrDocument);
			loadVestingInfo (ocrDocument);
			loadMortgageInfo (ocrDocument);
			loadNameReference (ocrDocument);
			loadAmount(ocrDocument);
			loadDates(ocrDocument);
			loadInstrument(ocrDocument);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public void reloadLegalDescription(long searchId) {
		Document ocrDocument = XmlUtils.parseXml(getXmlContents());
		legalDescriptionVector.clear();
		loadLegalDescription(ocrDocument, searchId);
	}
	
	/**
	 * loads all legal description tags from the ocrDocument into the legalDescriptionVector
	 * @param ocrDocument
	 * @param searchId 
	 */
	public void loadLegalDescription(Document ocrDocument, long searchId) {
		NodeList legals = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/LEGAL-DESCRIPTION");
		
		for (int i = 0; i < legals.getLength(); i++) {
			String legalValue = "";
			try {
				NamedNodeMap attributes = legals.item(i).getAttributes();
				NodeList legalItems = legals.item(i).getChildNodes();
				
				String isCondo = attributes.getNamedItem("isCondo").getNodeValue();
				if(isCondo.equals("true")) {
					setCondo(true);
				}
				
				for(int j = 0; j< legalItems.getLength(); j++) {
					if(legalItems.item(j).getChildNodes().getLength()>0) {
						legalValue += addLink(legalItems.item(j),searchId);
							
					}else {
						legalValue += legalItems.item(j).getNodeValue();
					}
						
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			if(!StringUtils.isEmpty(legalValue)){
				addLegalDescription(legalValue);
			}
			
		}
	}
	
	/**
	 * loads all address tags from the ocrDocument into the addressVector
	 * @param ocrDocument
	 * @param searchId 
	 */
	public void loadAddress(Document ocrDocument, long searchId) {
		NodeList addresses = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/PROPERTY-ADDRESS/ADDRESS/@value");
		
		for (int i = 0; i < addresses.getLength(); i++) {
			String addressValue = "";
			try {
				NodeList addressItems = addresses.item(i).getChildNodes();
				
				for(int j = 0; j< addressItems.getLength(); j++) {
					addressValue += addressItems.item(j).getNodeValue();	
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			if(!StringUtils.isEmpty(addressValue)){
				addAddress(addressValue);
			}
			
		}
	}
	
	private String addLink(Node linkInfo, long searchId) {
		String link = "";
		String text = XmlUtils.findFastNodeValue(linkInfo, "TEXT");
		
		if(insertLink){	
			String book = XmlUtils.findFastNodeValue(linkInfo, "BKNO");
			String page = XmlUtils.findFastNodeValue(linkInfo, "PGNO");
			String type = XmlUtils.findFastNodeValue(linkInfo, "TYPE");
			
			
			Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			DocumentsManagerI docManager = search.getDocManager();
			try {
				docManager.getAccess();
				List<DocumentI> docs = docManager.getDocumentsFlexible(book, page, type);
				for(DocumentI doc : docs) {
					if(doc.is(DType.ROLIKE) && doc.hasImage()) {
						link = "<a href=\""+DocumentUtils.createImageLinkWithDummyParameter(doc, search)+"\">"+text+"</a>";
					}
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				docManager.releaseAccess();
			}
		}
		
		if(link.isEmpty()) {
			link = text;
		}
		return link;
		
	}
	 
	private void loadDocumentReferences(Document ocrDocument, long searchId) {
		NodeList bookPages = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/DOCUMENT-REFERENCES");
		
		for (int i = 0; i < bookPages.getLength(); i++) {
			for (Node node : XmlUtils.getChildren(bookPages.item(i))) {
				Node reference = node.getNextSibling();
				if(reference != null) {
					NamedNodeMap attributes = reference.getAttributes();
					if(reference.getNodeName().equals("BOOK-PAGE")) {
						if(attributes!=null) {
							Node book = attributes.getNamedItem("book");
							Node page = attributes.getNamedItem("page");
							Node type = attributes.getNamedItem("type");
							String bookValue = null;
							String pageValue = null;
							String typeValue = null;
							if(book != null)
								bookValue = book.getNodeValue();
							if(page != null)
								pageValue = page.getNodeValue();
							if(type != null)
								typeValue = type.getNodeValue();
							else
								typeValue = "";
							if( !StringUtils.isEmpty(bookValue) && !StringUtils.isEmpty(pageValue) ){
								OCRParsedDataStruct.StructBookPage struct = 
									new OCRParsedDataStruct.StructBookPage (bookValue, pageValue);
								if(typeValue == null)
									typeValue = "";
								if(typeValue.isEmpty()) {
									struct.setType("");
								} else {
									struct.setType(typeValue);
								}
								bookPageVector.add(struct);
							}
						}
					} else if(reference.getNodeName().equals("INSTRUMENT")){
						if(attributes!=null) {
							try {
								Node instrument = attributes.getNamedItem("number");
								if(instrument != null) {
									String instrumentValue = instrument.getNodeValue();
									if(!StringUtils.isEmpty(instrumentValue)){
										Instrument tempInstr = new Instrument();
										tempInstr.setInstrumentNo( instrumentValue );
										
											Node year = attributes.getNamedItem("year");
											if(year != null) {
												String instrumentYear = year.getNodeValue();
												if(!StringUtils.isEmpty(instrumentYear)) {
													tempInstr.setExtraInfo("Year", Integer.parseInt(instrumentYear));
												}
											}
										
										instrumentList.add(tempInstr);
									}
								}
							}catch(Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * loads the document type from the ocrDocument
	 * @param ocrDocument
	 */
	private void loadDocumentType(Document ocrDocument) {
		try {
			NodeList nodeList = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/DOCUMENT-TYPE/@type");
			if(nodeList.getLength() > 0) {
				setDocumentType( nodeList.item(0).getNodeValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void loadLoanNumber(Document ocrDocument) {
		try {
			NodeList nodeList = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/LOAN-NUMBER/@value");
			if(nodeList.getLength() > 0) {
				setLoanNumber( nodeList.item(0).getNodeValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void loadDates(Document ocrDocument) {
		try {
			Node item = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/RECORDED-DATE/@date").item(0);
			if(item != null) {
				setRecordedDate(item.getNodeValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Node item = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/DOCUMENT-DATE/@date").item(0);
			if(item != null) {
				setInstrumentDate(item.getNodeValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void loadInstrument(Document ocrDocument) {
		try {
			NodeList nodeList = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/INSTRUMENT-NUMBER/INSTRUMENT/@number");
			if(nodeList.getLength() > 0) {
				setInstrumentNumber(nodeList.item(0).getNodeValue());
			}
			
			nodeList = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/INSTRUMENT-NUMBER/BOOK-PAGE/@book");
			if(nodeList.getLength() > 0) {
				setBook(nodeList.item(0).getNodeValue());
			}
			
			nodeList = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/INSTRUMENT-NUMBER/BOOK-PAGE/@page");
			if(nodeList.getLength() > 0) {
				setPage(nodeList.item(0).getNodeValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * loads the vesting information from the ocrDocument
	 * @param ocrDocument
	 */
	private void loadVestingInfo(Document ocrDocument) {
		try {
			Node item = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/VESTING-INFORMATION/GRANTEE/@name").item(0);
			if(item != null) {
				setVestingInfoGrantee( item.getNodeValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			Node item = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/VESTING-INFORMATION/GRANTOR/@name").item(0);
			if(item != null) {
				setVestingInfoGrantor( item.getNodeValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * loads the mortgage information from the ocrDocument
	 * @param ocrDocument
	 */
	private void loadMortgageInfo(Document ocrDocument) {
		try {
			NodeList grantees = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/MORTGAGE-INFORMATION/GRANTEE");
			for (int i = 0; i < grantees.getLength(); i++) {
				if(grantees.item(i) != null ) {
					String name = null;
					String value = null;
					NodeList granteesName = XmlUtils.xpathQuery(grantees.item(i), "@name");
					if(granteesName != null && granteesName.getLength()>0 && granteesName.item(0) != null) {
						name = granteesName.item(0).getNodeValue();
					}
					NodeList granteesType = XmlUtils.xpathQuery(grantees.item(i), "@type");
					if(granteesType != null && granteesType.getLength()>0 && granteesType.item(0) != null) {
						value = granteesType.item(0).getNodeValue();
					}
					
					if(!StringUtils.isEmpty(value) && value.equals("trustee")) {
						addTrustee(name);
					} else {
						addLender(name);
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			NodeList grantors = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/MORTGAGE-INFORMATION/GRANTOR/@name");
			for (int i = 0; i < grantors.getLength(); i++) {
				if(grantors.item(i) != null) {
					addMortgageGrantor(grantors.item(i).getNodeValue());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	private void loadNameReference(Document ocrDocument) {
		NodeList grantorsList = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/NAME-REFERENCES/GTORNAME/@value");
		for (int i = 0; i < grantorsList.getLength(); i++) {
			try {
				nameRefGrantor.add(grantorsList.item(i).getNodeValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		/*NodeList granteesList = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/NAME-REFERENCES/GTEENAME/@value");
		for (int i = 0; i < granteesList.getLength(); i++) {
			try {
				nameRefGrantee.add(granteesList.item(i).getNodeValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}*/
		
		NodeList grantees = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/NAME-REFERENCES/GTEENAME");
		for (int i = 0; i < grantees.getLength(); i++) {
			if(grantees.item(i) != null ) {
				String name = null;
				String value = null;
				NodeList granteesName = XmlUtils.xpathQuery(grantees.item(i), "@value");
				if(granteesName != null && granteesName.getLength()>0 && granteesName.item(0) != null) {
					name = granteesName.item(0).getNodeValue();
				}
				NodeList granteesType = XmlUtils.xpathQuery(grantees.item(i), "@type");
				if(granteesType != null && granteesType.getLength()>0 && granteesType.item(0) != null) {
					value = granteesType.item(0).getNodeValue();
				}
				
				if(!StringUtils.isEmpty(value) && value.equals("trustee")) {
					nameRefGranteeTrustee.add(name);
				} else {
					nameRefGrantee.add(name);
				}
			
			
			}
		}
		
		
	}
	
	/**
	 * loads the amounts found in the ocrDocument
	 * @param ocrDocument
	 */
	private void loadAmount(Document ocrDocument) {
		NodeList amountList = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/AMOUNTS/AMOUNT/@value");
		for (int i = 0; i < amountList.getLength(); i++) {
			try {
				amountVector.add( amountList.item(i).getNodeValue());   
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private void loadUnits(Document ocrDocument) {
		NodeList unitList = XmlUtils.xpathQuery(ocrDocument, "/OCR-RESULTS/UNITS/UNIT/@number");
		for (int i = 0; i < unitList.getLength(); i++) {
			try {
				unitVector.add( unitList.item(i).getNodeValue());   
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		
		/*String fileName = "E:\\Bugs\\Bug 6854 - OCR - use fields UNIT and isCondo\\exemplu_1.xml";
		try {
			OCRParsedDataStruct ocrData = OCRParsedDataStruct.getDataFromXML(fileName, -2, false);
			System.err.println(ocrData.toStringNice("\n", true));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		//System.out.println(convertPdfToTiff("D:\\R83499.pdf"));
		
	}
	

	public Vector<StructBookPage> getBookPageVector() {
		return this.bookPageVector;
	}

	public Vector<Instrument> getInstrumentVector() {
		return this.instrumentList;
	}
	
	
	public String getVestingInfoGrantee() {
		return vestingInfoGrantee;
	}
	
	/**
	 * @param vestingInfoGrantee the vestingInfoGrantee to set
	 */
	public void setVestingInfoGrantee(String vestingInfoGrantee) {
		this.vestingInfoGrantee = vestingInfoGrantee;
	}

	synchronized public String getLegalDescription() {
		if( StringUtils.isEmpty(legalDescription) ){
			StringBuffer legalAll = new StringBuffer();
			Vector<String> ax = getLegalDescriptionVector();
			if( ax != null && ax.size()>0 ){
				for (String legal : ax) {
					if(!StringUtils.isEmpty(legal)){
						legalAll.append(legal);
						legalAll.append("\n");
					}
				}
			}
			legalDescription = legalAll.toString();
		}
		return  legalDescription;
	}
	
	synchronized public String getLegalDescriptionFormated() {
		StringBuffer legalAll = new StringBuffer();
		Vector<String> ax = getLegalDescriptionVector();		
		HashSet<String> usedLegals = new HashSet<String>();
		if( ax != null && ax.size()>0 ){						
			for (String legal : ax) {
				String tempLegal = legal.replaceAll("\\s+", "").toLowerCase();
				if(!tempLegal.isEmpty() && !usedLegals.contains(tempLegal)) {
					usedLegals.add(tempLegal);
					legalAll.append(legal);
					if(!legal.endsWith("\n")){
						legalAll.append("\n");
					}
				}
			}
		}
			
		return  legalAll.toString();
	}
	
	public String getXmlContents(){
		return this.xmlContents;
	}
	
	public void setXmlContents( String xmlContents ){
		this.xmlContents = xmlContents;
	}
	
	/*public static String convertTiffToJpeg( String initialImageLink ){
		
		String outputfile = initialImageLink.replaceAll("(?i)\\.tif", ".png");
		String outputDir = outputfile.substring( 0 , outputfile.lastIndexOf(File.separator) + 1);
		
		
		String tiff2pngCommand = rbc.getString("tiff2png.CommandString").trim();
		
		String[] exec = {
				tiff2pngCommand,
				"-verbose",
				"-destdir",
				outputDir,
				initialImageLink};

		//System.err.println("(((((((((((( "+ exec+"))))))))))))))))))))))))))))))))))");
		
		int k=0;
		ClientProcessExecutor cpe = null;
		try{
	        cpe = new ClientProcessExecutor( exec, true, true);
	        cpe.start();
	
			k = cpe.getReturnValue();
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		
		if(!new File(outputfile).exists()){ //for windows
			String[] exec1 = {
					tiff2pngCommand,
					"-verbose",
					"-destdir",
					outputDir.replace("\\", "/"),
					initialImageLink.replace("\\", "/")};
			try{
		        cpe = new ClientProcessExecutor( exec1, true, true);
		        cpe.start();
				k = cpe.getReturnValue();
			}
			catch( Exception e ){
				e.printStackTrace();
			}
		}
		
		System.err.println("convertTiffToJpeg finished with code: " + k);
		
		return outputfile;
	}*/
	
	public static String convertPdfToTiff( String initialImageLink ){

		String gsCommand = rbc.getString("GS.CommandString").trim();
		String outputfile = initialImageLink.replaceAll("(?i)\\.pdf", ".tiff");
		
		String[] exec = {
				gsCommand,
			"-sPDFPassword=",
			"-sDEVICE=tiffg4",
			"-dNOPAUSE",
			"-sOutputFile=" + outputfile,
			initialImageLink,
			"-dBATCH"};

		try{
	        ClientProcessExecutor cpe = new ClientProcessExecutor( exec, true, true);
	        cpe.start();
			//int k = cpe.getReturnValue();
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		
		return outputfile;
	}
	
	public String getLegalDescImage(){
		return legalDescImageName;
	}
	
	public void setLegalDescImage( String legalDescImageName ){
		this.legalDescImageName = legalDescImageName;
	}

	
	public String getVestingInfoGrantor() {
		return vestingInfoGrantor;
	}
	
	/**
	 * @param vestingInfoGrantor the vestingInfoGrantor to set
	 */
	public void setVestingInfoGrantor(String vestingInfoGrantor) {
		this.vestingInfoGrantor = vestingInfoGrantor;
	}


	public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	/**
	 * Grantee names with no type or lenders
	 * @return
	 */
	public Vector<String> getNameRefGrantee() {
		return nameRefGrantee;
	}

	public void setNameRefGrantee(Vector<String> nameRefGrantee) {
		this.nameRefGrantee = nameRefGrantee;
	}

	/**
	 * @return the nameRefGranteeTrustee
	 */
	public Vector<String> getNameRefGranteeTrustee() {
		return nameRefGranteeTrustee;
	}

	/**
	 * @param nameRefGranteeTrustee the nameRefGranteeTrustee to set
	 */
	public void setNameRefGranteeTrustee(Vector<String> nameRefGranteeTrustee) {
		this.nameRefGranteeTrustee = nameRefGranteeTrustee;
	}

	public Vector<String> getNameRefGrantor() {
		return nameRefGrantor;
	}

	public void setNameRefGrantor(Vector<String> nameRefGrantor) {
		this.nameRefGrantor = nameRefGrantor;
	}
	
	/**
	 * Adds a new Legal Description 
	 * @param legalDescription
	 * @return
	 */
	public boolean addLegalDescription(String legalDescription){
		return legalDescriptionVector.add(legalDescription);
	}
	
	/**
	 * @return the legalDescriptionVector
	 */
	public Vector<String> getLegalDescriptionVector() {
		return legalDescriptionVector;
	}
	/**
	 * @param legalDescriptionVector the legalDescriptionVector to set
	 */
	public void setLegalDescriptionVector(Vector<String> legalDescriptionVector) {
		this.legalDescriptionVector = legalDescriptionVector;
	}
	
	/**
	 * Adds a new Address 
	 * @param legalDescription
	 * @return
	 */
	public boolean addAddress(String address){
		return addressVector.add(address);
	}
	
	/**
	 * @return the addressVector
	 */
	public Vector<String> getAddressVector() {
		return addressVector;
	}
	/**
	 * @param addressVector the addressVector to set
	 */
	public void setAddressVector(Vector<String> addressVector) {
		this.addressVector = addressVector;
	}
	
	/**
	 * @return the amountVector
	 */
	public Vector<String> getAmountVector() {
		return amountVector;
	}
	/**
	 * @param amountVector the amountVector to set
	 */
	public void setAmountVector(Vector<String> amountVector) {
		this.amountVector = amountVector;
	}
	
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("OCRParsedDataStruct[");
		buffer.append("\n bookPageVector = ").append(bookPageVector);
		buffer.append("\n documentType = ").append(documentType);
		buffer.append("\n instrumentList = ").append(instrumentList);
		buffer.append("\n legalDescImageName = ").append(legalDescImageName);
		buffer.append("\n legalDescriptionVector = ").append(
				legalDescriptionVector);
		buffer.append("\n nameRefGrantee = ").append(nameRefGrantee);
		buffer.append("\n nameRefGranteeTrustee = ").append(nameRefGranteeTrustee);
		buffer.append("\n nameRefGrantor = ").append(nameRefGrantor);
		buffer.append("\n vestingInfoGrantee = ").append(vestingInfoGrantee);
		buffer.append("\n vestingInfoGrantor = ").append(vestingInfoGrantor);
		buffer.append("\n amountVector = ").append(amountVector);
		buffer.append("\n loanNumber = ").append(loanNumber);
		buffer.append("]");
		return buffer.toString();
	}
	

	
	/**
	 * Creates a string representation of the object, with the given lineSeparator. 
	 * Can show or hide the fact that some information is missing 
	 * @param lineSeparator
	 * @param showNotFoundInfo flag used to show or not which fields are not completed
	 * @return
	 */
	public String toStringNice(String lineSeparator, boolean showNotFoundInfo){
		StringBuilder logHere = new StringBuilder();
		String aux = null;
		boolean foundMortgageInfo = false;
		Vector<StructBookPage> bookPages = getBookPageVector();
		Iterator<StructBookPage> ocrBookPagesIterator = bookPages.iterator();
		while( ocrBookPagesIterator.hasNext() ){
			StructBookPage ocrBookPageStruct = (StructBookPage) ocrBookPagesIterator.next();
			logHere.append((logHere.length()>0?"":lineSeparator) + "Found reference book = <b>" + 
					ocrBookPageStruct.book + "</b> reference page = <b>" + 
					ocrBookPageStruct.page + "</b> reference type = <b>" +
					ocrBookPageStruct.getType() + "</b>" + lineSeparator);
			String page = ocrBookPageStruct.getPage();
			if(page.contains("-") && page.trim().matches("(?is)\\d+-\\d+")){
				logHere.append((logHere.length()>0?"":lineSeparator) + 
						"Range will not be expanded. Using just the range start interval value." + lineSeparator);
				ocrBookPageStruct.page = page.substring(0, page.indexOf("-"));
			}
		}
		
		Vector<Instrument> instruments = getInstrumentVector();
		Iterator<Instrument> ocrInstrumentIterator = instruments.iterator();
		while( ocrInstrumentIterator.hasNext() ){
			Instrument ocrInstrument = (Instrument) ocrInstrumentIterator.next();
			logHere.append((logHere.length()>0?"":lineSeparator) + 
					"Found reference instrument = <b>" + ocrInstrument.getInstrumentNo() + "</b>" + lineSeparator);
		}
		
		if(!StringUtils.isEmpty(recordedDate)) {
			logHere.append((logHere.length()>0?"":lineSeparator) +"OCR: Found recorded date: " + recordedDate + "</b>" + lineSeparator);
		}
		
		if(!StringUtils.isEmpty(instrumentDate)) {
			logHere.append((logHere.length()>0?"":lineSeparator) +"OCR: Found instrument date: " + instrumentDate + "</b>" + lineSeparator);
		}
		
		if(!StringUtils.isEmpty(instrumentNumber)) {
			logHere.append((logHere.length()>0?"":lineSeparator) +"OCR: Found instrument number: " + instrumentNumber + "</b>" + lineSeparator);
		}
		
		if(!StringUtils.isEmpty(book)) {
			logHere.append((logHere.length()>0?"":lineSeparator) +"OCR: Found book : " + book + "</b>" + lineSeparator);
		}
		
		if(!StringUtils.isEmpty(page)) {
			logHere.append((logHere.length()>0?"":lineSeparator) +"OCR: Found page : " + page + "</b>" + lineSeparator);
		}
		
		Vector<String> ax = getLegalDescriptionVector();
		if( ax != null && ax.size()>0 ){
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Found legal description(s): " + lineSeparator);
			aux = "";
			int i = 0;
			for (String legal : ax) {
				aux += legal + "\n";
				i++;
				logHere.append((logHere.length()>0?"":lineSeparator) +"OCR - Legal " + i + ": <b>" + legal + "</b>" + lineSeparator);
			}
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: isCondo = " + isCondo + lineSeparator);
		}
		else if(showNotFoundInfo) {
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Legal description not found " + lineSeparator);
		}
		
		Vector<String> unitVector = getUnitVector();
		if(unitVector != null && !unitVector.isEmpty()) {
			String units = "";
			for(String unit : unitVector) {
				units += ", " + unit;
			}
			units = units.replaceFirst(", ", "");
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Found units: " + units + lineSeparator);
		} else if(showNotFoundInfo) {
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Units not found" + lineSeparator);
		}
		
		aux = getVestingInfoGrantee();
		if( !"".equals(aux)){
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Found Vesting info Grantee = <b>" + aux + "</b>" + lineSeparator);
		}
		else if(showNotFoundInfo) {
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Vesting info Grantee not found" + lineSeparator);
		}
		
		aux = getVestingInfoGrantor();
		if( !"".equals(aux)){
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Found Vesting info Grantor = <b>" + aux + "</b>" + lineSeparator);
		}
		else if(showNotFoundInfo) {
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Vesting info Grantor not found" + lineSeparator);
		}
		
		//--------------------------
		
		ax = getMortgageGranteeLender();
		if( ax != null && ax.size()>0 ){
			aux = getMortgageGranteeLender().firstElement();
			if( !"".equals(aux)){
				logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Found Mortgage info Lender = <b>" + aux + "</b>" + lineSeparator);
			} else if(showNotFoundInfo) {
				logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Mortgage Lender not found " + lineSeparator);
			}
		}
		else if(showNotFoundInfo) {
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Mortgage Lender not found " + lineSeparator);
		}
		
		ax = getMortgageGranteeTrustee();
		if( ax != null && ax.size()>0 ){
			aux = getMortgageGranteeTrustee().firstElement();
			if( !"".equals(aux)){
				logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Found Mortgage info Trustee = <b>" + aux + "</b>" + lineSeparator);
			} else if(showNotFoundInfo) {
				logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Mortgage Trustee not found " + lineSeparator);
			}
			
		}
		else if(showNotFoundInfo) {
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Mortgage Trustee not found " + lineSeparator);
		}
		
		ax = getMortgageGrantor();
		if( ax != null && ax.size()>0 ){
			aux = getMortgageGrantor().firstElement();
			if( !"".equals(aux)){
				logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Found Mortgage info Grantor = <b>" + aux + "</b>" + lineSeparator);
			} else if(showNotFoundInfo) {
				logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Mortgage info Grantor not found " + lineSeparator);
			}
		}
		else if(showNotFoundInfo) {
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Mortgage info Grantor not found" + lineSeparator);
		}
		
		//-------------------------
		
		aux = getDocumentType();
		if( !"".equals(aux)){
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Found Document Type = <b>" + aux + "</b>" + lineSeparator);
		}
		else if(showNotFoundInfo) {
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Document Type not found" + lineSeparator);
		}

		ax = getNameRefGranteeTrustee();
		if( ax!=null&&ax.size()>0){
		   for (int i = 0; i < ax.size(); i++) {
			   foundMortgageInfo = true;
			   logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Found Name-Reference Grantee Trustee" +
					   " = <b>" +ax.get(i) + "</b>" + lineSeparator);
		   }			
		}
		else if(showNotFoundInfo) {
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Name-Reference Grantee Trustee not found " + lineSeparator);
		}

		
		ax = getNameRefGrantee();
		if( ax!=null&&ax.size()>0){
		   for (int i = 0; i < ax.size(); i++) {
			   logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Found Name-Reference Grantee" +
					   (foundMortgageInfo?" Lender":"") + " = <b>" +ax.get(i) + "</b>" + lineSeparator);
		   }			
		}
		else if(showNotFoundInfo) {
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Name-Reference Grantee " +
					(foundMortgageInfo?"Lender ":"") + "not found " + lineSeparator);
		}
		
		ax = getNameRefGrantor();
		if( ax!=null&&ax.size()>0){
			for (int i = 0; i < ax.size(); i++) {
				logHere.append((logHere.length()>0?"":lineSeparator) + 
						"OCR: Found Name-Reference Grantor = <b>" + ax.get(i) + "</b>" + lineSeparator);
			}
		}
		else if(showNotFoundInfo) {
			
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Name-Reference Grantor not found " + lineSeparator);
		}
		ax = getAddressVector();
		if (ax != null && ax.size() > 0){
			logHere.append((logHere.length() > 0 ? "" : lineSeparator) + "OCR: Found Address(es): " + lineSeparator);
			int i = 0;
			for (String address : ax) {
				i++;
				logHere.append("OCR - Address " + i + ": <b>" + address + "</b>"  + lineSeparator);
			}
		} else if(showNotFoundInfo) {
			logHere.append((logHere.length() > 0 ? "" : lineSeparator) + "OCR: Address not found " + lineSeparator);
		}
		
		ax = getAmountVector();
		if( ax != null && ax.size()>0 ){
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Found Amount(s): " + lineSeparator);
			int i = 0;
			for (String amount : ax) {
				i++;
				logHere.append("OCR - Amount " + i + ": <b>" + amount + "</b>"  + lineSeparator);
			}
		}
		else if(showNotFoundInfo) {
			logHere.append((logHere.length() > 0 ?"":lineSeparator) + "OCR: Amount not found " + lineSeparator);
		}
		aux = getLoanNumber();
		if( !"".equals(aux)){
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Found Loan Number = <b>" + aux + "</b>" + lineSeparator);
		}
		else if(showNotFoundInfo) {
			logHere.append((logHere.length()>0?"":lineSeparator) + "OCR: Loan Number not found" + lineSeparator);
		}
		
		return logHere.toString();
	}
	
	/**
	 * @return the legalDescImageName
	 */
	public String getLegalDescImageName() {
		return legalDescImageName;
	}
	/**
	 * @param legalDescImageName the legalDescImageName to set
	 */
	public void setLegalDescImageName(String legalDescImageName) {
		this.legalDescImageName = legalDescImageName;
	}
	/**
	 * @param bookPageVector the bookPageVector to set
	 */
	public void setBookPageVector(Vector<StructBookPage> bookPageVector) {
		this.bookPageVector = bookPageVector;
	}
	

	@Override
	public OCRParsedDataStruct clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		// to be implemented
		try {
			return (OCRParsedDataStruct) super.clone();
		} catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	public double getAmountSum() {
		if(getAmountVector() == null || getAmountVector().size() == 0)
			return 0;
		double amount = 0;
		for (String amountString : getAmountVector()) {
			try {
				amount += Double.parseDouble(amountString.replaceAll("[$, ]", ""));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return amount;
	}
		
	public void addLoggableInfo(String info){
		if(infoCollected == null){
			infoCollected = new StringBuilder();
		}
		infoCollected.append(info);
	}
	
	public String getLoggableInfo(){
		if(infoCollected == null)
			return "";
		return infoCollected.toString();
	}
	
	public void clearLoggableInfo(){
		infoCollected = new StringBuilder();
	}

	/**
	 * @return the loanNumber
	 */
	public String getLoanNumber() {
		if(loanNumber == null)
			return "";
		return loanNumber;
	}

	/**
	 * @param loanNumber the loanNumber to set
	 */
	public void setLoanNumber(String loanNumber) {
		this.loanNumber = loanNumber;
	}

	/**
	 * @return the mortgageGranteeLender
	 */
	public Vector<String> getMortgageGranteeLender() {
		return mortgageGranteeLender;
	}

	/**
	 * @param mortgageGranteeLender the mortgageGranteeLender to set
	 */
	public void setMortgageGranteeLender(Vector<String> mortgageGranteeLender) {
		this.mortgageGranteeLender = mortgageGranteeLender;
	}

	/**
	 * @return the mortgageGranteeTrustee
	 */
	public Vector<String> getMortgageGranteeTrustee() {
		return mortgageGranteeTrustee;
	}

	/**
	 * @param mortgageGranteeTrustee the mortgageGranteeTrustee to set
	 */
	public void setMortgageGranteeTrustee(Vector<String> mortgageGranteeTrustee) {
		this.mortgageGranteeTrustee = mortgageGranteeTrustee;
	}

	/**
	 * @return the mortgageGrantor
	 */
	public Vector<String> getMortgageGrantor() {
		return mortgageGrantor;
	}

	/**
	 * @param mortgageGrantor the mortgageGrantor to set
	 */
	public void setMortgageGrantor(Vector<String> mortgageGrantor) {
		this.mortgageGrantor = mortgageGrantor;
	}
	
	
	
	public String getRecordedDate() {
		return recordedDate;
	}

	public void setRecordedDate(String recordedDate) {
		this.recordedDate = recordedDate;
	}

	public String getInstrumentDate() {
		return instrumentDate;
	}

	public void setInstrumentDate(String instrumentDate) {
		this.instrumentDate = instrumentDate;
	}

	public String getInstrumentNumber() {
		return instrumentNumber;
	}

	public void setInstrumentNumber(String instrumentNumber) {
		this.instrumentNumber = instrumentNumber;
	}

	public String getBook() {
		return book;
	}

	public void setBook(String book) {
		this.book = book;
	}

	public String getPage() {
		return page;
	}

	public void setPage(String page) {
		this.page = page;
	}

	
	public boolean addLender(String name){
		if(name == null)
			return false;
		if(getMortgageGranteeLender() == null)
			setMortgageGranteeLender( new Vector<String>());
		return getMortgageGranteeLender().add(name);
	}
	
	public boolean addTrustee(String name){
		if(name == null)
			return false;
		if(getMortgageGranteeTrustee() == null)
			setMortgageGranteeTrustee(new Vector<String>());
		return getMortgageGranteeTrustee().add(name);
	}
	
	public boolean addMortgageGrantor(String name){
		if(name == null)
			return false;
		if(getMortgageGrantor() == null)
			setMortgageGrantor(new Vector<String>());
		return getMortgageGrantor().add(name);
	}

	public boolean isCondo() {
		return isCondo;
	}

	public void setCondo(boolean isCondo) {
		this.isCondo = isCondo;
	}

	public Vector<String> getUnitVector() {
		return unitVector;
	}

	public void setUnitVector(Vector<String> unitVector) {
		this.unitVector = unitVector;
	}
	
	public void compareAndLog(OCRParsedDataStruct oldOcrData, long searchId){
		
		if (this.vestingInfoGrantee.compareTo(oldOcrData.vestingInfoGrantee) != 0){
			SearchLogger.info("</div><div>In Smart Viewer, Vesting Info Grantee <b>" + oldOcrData.vestingInfoGrantee + "</b> was changed with <b>" + 
					this.vestingInfoGrantee + "</b> " + SearchLogger.getTimeStamp(searchId) + ".<br><div>", searchId);
		}
		if (this.vestingInfoGrantor.compareTo(oldOcrData.vestingInfoGrantor) != 0){
			SearchLogger.info("</div><div>In Smart Viewer, Vesting Info Grantor <b>" + oldOcrData.vestingInfoGrantor + "</b> was changed with <b>" + 
					this.vestingInfoGrantor + "</b> " + SearchLogger.getTimeStamp(searchId) + ".<br><div>", searchId);
		}
		if (this.documentType.compareTo(oldOcrData.documentType) != 0){
			SearchLogger.info("</div><div>In Smart Viewer, Document Type <b>" + oldOcrData.documentType + "</b> was changed with <b>" + 
					this.documentType + "</b> " + SearchLogger.getTimeStamp(searchId) + ".<br><div>", searchId);
		}
		if (this.instrumentDate.compareTo(oldOcrData.instrumentDate) != 0){
			SearchLogger.info("</div><div>In Smart Viewer, Instrument Date <b>" + oldOcrData.instrumentDate + "</b> was changed with <b>" + 
					this.instrumentDate + "</b> " + SearchLogger.getTimeStamp(searchId) + ".<br><div>", searchId);
		}
		if (this.recordedDate.compareTo(oldOcrData.recordedDate) != 0){
			SearchLogger.info("</div><div>In Smart Viewer, Recorded Date <b>" + oldOcrData.recordedDate + "</b> was changed with <b>" + 
					this.recordedDate + "</b> " + SearchLogger.getTimeStamp(searchId) + ".<br><div>", searchId);
		}
		if (this.instrumentNumber.compareTo(oldOcrData.instrumentNumber) != 0){
			SearchLogger.info("</div><div>In Smart Viewer, Instrument Number <b>" + oldOcrData.instrumentNumber + "</b> was changed with <b>" + 
					this.instrumentNumber + "</b> " + SearchLogger.getTimeStamp(searchId) + ".<br><div>", searchId);
		}
		if (this.amountVector.toString().compareTo(oldOcrData.amountVector.toString()) != 0){
			SearchLogger.info("</div><div>In Smart Viewer, the Amount(s) <b>" + oldOcrData.amountVector.toString() + "</b> was changed to <b>" + 
					this.amountVector.toString() + "</b> " + SearchLogger.getTimeStamp(searchId) + ".<br><div>", searchId);
		}
		if (this.book.compareTo(oldOcrData.book) != 0){
			SearchLogger.info("</div><div>In Smart Viewer, Book <b>" + oldOcrData.book + "</b> was changed with <b>" + 
					this.book + "</b> " + SearchLogger.getTimeStamp(searchId) + ".<br><div>", searchId);
		}
		if (this.page.compareTo(oldOcrData.page) != 0){
			SearchLogger.info("</div><div>In Smart Viewer, Page <b>" + oldOcrData.page + "</b> was changed with <b>" + 
					this.page + "</b> " + SearchLogger.getTimeStamp(searchId) + ".<br><div>", searchId);
		}
	}
}
