package ro.cst.tsearch.templates;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.connection.NoConnectException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XIndexAccess;
import com.sun.star.document.XDocumentInsertable;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XCloseable;
import com.sun.star.util.XReplaceDescriptor;
import com.sun.star.util.XReplaceable;

public class OfficeDocumentContents implements TemplateContents {
	
	protected static final Logger	logger				= Logger.getLogger(OfficeDocumentContents.class);
	
	XTextDocument doc;
	
	XComponent xComponent;
	XIndexAccess tagIndex;
	XTextRange xtr;
	String what = "";
	int currentMatchIndex = -1;
	
	public static Boolean triedAutoStart = false; 
	private static int triedFailedCount = 0;
	
	public OfficeDocumentContents(XTextDocument doc) {
		this.doc = doc;
	}
	
	public OfficeDocumentContents(String filename) throws Exception {
	
		long startTime = System.currentTimeMillis();
		
		if (filename.indexOf("private:") != 0) {
			filename = "file:///" + (new File(filename)).getCanonicalPath().replaceAll("\\\\", "/");
		}
			
		// Load a Writer document, which will be automatically displayed
		PropertyValue[] loadProps=new PropertyValue[2];
		//MediaType
		loadProps[0]=new PropertyValue();
		loadProps[0].Name="Hidden";
		loadProps[0].Value=Boolean.TRUE;
		loadProps[1] = new PropertyValue();
		loadProps[1].Name = "NoRestore";
		loadProps[1].Value = Boolean.TRUE;
					
		XComponentLoader xcomponentloader = null;
		
		synchronized(OfficeDocumentContents.class) {
		
			try {
				xcomponentloader = connect();
				logger.info("connect OfficeDocumentContents(" + filename + ") took " + (System.currentTimeMillis() - startTime));
			}catch(NoConnectException nce) {
				
				/* Try to auto-start open office */
				try {
					
						if(!triedAutoStart) {
							triedAutoStart = true;
							
							String templateOpenofficeRestartTokens = ServerConfig.getTemplateOpenofficeRestartTokens();
							if(templateOpenofficeRestartTokens != null) {
								String[] commands = templateOpenofficeRestartTokens.split("\\s*;\\s*");
								for (String command : commands) {
									try {
										String[] tokens = command.split("\\s*,\\s*");
										int tokensLength = tokens.length;
										if(tokensLength > 1) {
											long sleep = Long.parseLong(tokens[tokensLength - 1]);
											ClientProcessExecutor cpe = new ClientProcessExecutor( Arrays.copyOf(tokens, tokensLength - 1 ) , false, false );
								            cpe.start();
								            Thread.sleep(sleep);
										}
									} catch (Exception e) {
										logger.error("Cannot run tokens " + command, e);
									}
								}
							}
							logger.info("tried start OfficeDocumentContents(" + filename + ") took " + (System.currentTimeMillis() - startTime));
				            xcomponentloader = connect();
				            
				            logger.info("Open office re-started second connect OfficeDocumentContents(" + filename + ") took " + (System.currentTimeMillis() - startTime));
				            /* Seems to be started successfully */
				            triedAutoStart = false;
						}else {
							throw nce;
						}
					
				}catch(NoConnectException e) {
					triedFailedCount++;
					
					if(triedFailedCount % 5 == 0) {
						//reset tried start
						triedAutoStart = false;
					}
					if(!URLMaping.INSTANCE_DIR.equals("local")){
						StringWriter sw = new StringWriter();
				        PrintWriter pw = new PrintWriter(sw);
				        new Throwable().fillInStackTrace().printStackTrace(pw);
				        
						String message = "Cannot connect to OpenOffice. Try it manually \n" +
										"triedFailedCount " + triedFailedCount +
										 "\n\nException follows : \n" +
										 sw.toString();
						
						EmailClient email = new EmailClient();
						email.addTo(MailConfig.getExceptionEmail());
						email.setSubject("OpenOffice exception");
						email.addContent(message);
						email.sendAsynchronous();
						logger.error("Cannot reconnect to open office", nce);
						throw nce;
					}
					
				}catch(Exception e) {
					logger.error("Unexpected exception while tring to reconnet to office", e);
					throw e;
				}
			}
		
			xComponent = xcomponentloader.loadComponentFromURL(filename, "_blank", 0, loadProps);
			doc= (XTextDocument) UnoRuntime.queryInterface(XTextDocument.class, xComponent);
			if (doc==null) throw new Exception("Invalid document: " + filename);
		}
		
		logger.info("full OfficeDocumentContents(" + filename + ") took " + (System.currentTimeMillis() - startTime));
	}
	
	@Override
	public boolean find() {
		currentMatchIndex++;
		try  {
			xtr = ((XTextRange) UnoRuntime.queryInterface(XTextRange.class,tagIndex.getByIndex(currentMatchIndex)));
		}catch(Exception e) {
			return false;
		}
		return currentMatchIndex <= tagIndex.getCount();
	}

	@Override
	public void findAll(Pattern p) {
		findAll(p.toString());
	}

	@Override
	public void findAll(String what) {
		this.what = what;
		XReplaceable xr = createXReplaceable();
		XReplaceDescriptor xrd = createReplaceDescriptor(xr,what,"",true);
		tagIndex = xr.findAll(xrd);
		currentMatchIndex=-1;
	}
	
	@Override
	public String group() throws Exception {
		return xtr.getString();
	}
	
	@Override
	public void replaceCurrentMatch(String replacement) {
		try {
			xtr.setString( group().replaceAll(what.replace("[:digit:]","\\d"), replacement) );
		}catch(Exception e) {
			e.printStackTrace();
			logger.error("replaceCurrentMatch " + replacement, e);
		}
	}

	public void replaceCurrentMatchWithProperties(String replacement, String propertyName, Object propertyValue) {
		
		
		replaceCurrentMatch(replacement.replace("\\", "\\\\").replace("$", "\\$"));
		XPropertySet xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xtr);
		try {
			xPropertySet.setPropertyValue(propertyName,propertyValue);/*"CharWeight",  new Float(com.sun.star.awt.FontWeight.BOLD));*/
		}catch(Exception e) {
			e.printStackTrace();
			logger.error("replaceCurrentMatchWithProperties " + replacement, e);
		}
		
	}
	
	@Override
	public void replaceCurrentMatchEscape(String replacement) {
		replaceCurrentMatch(replacement.replace("\\", "\\\\").replace("$", "\\$"));
	}
	
	@Override
	public void resetFind() {
		XReplaceable xr = createXReplaceable();
		XReplaceDescriptor xrd = createReplaceDescriptor(xr,what,"",true);
		tagIndex = xr.findAll(xrd);
		currentMatchIndex=-1;
	}
	
	@Override
	public void replaceAll(String what, String replacement) {
		XReplaceable xr = createXReplaceable();
		XReplaceDescriptor xrd = createReplaceDescriptor(xr,what,replacement,true);
		xr.replaceAll(xrd);
	}

	@Override
	public String toString() {
		return doc.getText().getString();
	}
	
	@Override
	public void saveToFile(String filename) throws Exception  {
		saveToFile(filename, filename.substring(filename.lastIndexOf('.')+1,filename.length()));
	}
	
	@Override
	public void saveToFile(String filename, String extension) throws Exception {
		if (filename.indexOf("private:") != 0) {
			java.io.File outFile = new java.io.File(filename);
			StringBuffer sTmp = new StringBuffer("file:///");
			sTmp.append(outFile.getCanonicalPath().replaceAll("\\\\", "/"));
			filename = sTmp.toString();
		}	
		
		PropertyValue[] lProperties = new PropertyValue[2];
		lProperties[0] = new PropertyValue();
		lProperties[0].Name = "Overwrite";
		lProperties[0].Value = Boolean.TRUE;
		lProperties[1] = new PropertyValue();
		lProperties[1].Name = "FilterName";
		
		String saveFilter = "";
		if ( AddDocsTemplates.knownExtensions.containsKey(extension)) {
			saveFilter = AddDocsTemplates.knownExtensions.get(extension);
		}else if(extension.toLowerCase().equals("pdf")) {
			saveFilter = "writer_pdf_Export";
		}else {
			throw new TemplatesException("No Format found for this extension");
		}			

		lProperties[1].Value = saveFilter;
		
		XStorable xStore = (XStorable) UnoRuntime.queryInterface(XStorable.class,doc);
		
		xStore.storeToURL(filename, lProperties);
	}
	
	
	public void save() {
		XStorable xStore = (XStorable) UnoRuntime.queryInterface(XStorable.class,doc);
		try {
			xStore.store();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Cannot save doc template", e);
		}
	}
	
	public XReplaceable createXReplaceable() {
		XReplaceable xReplaceable = null;
		xReplaceable = (com.sun.star.util.XReplaceable) UnoRuntime.queryInterface(com.sun.star.util.XReplaceable.class,doc);
		return xReplaceable;
	}
	
	public XReplaceDescriptor createReplaceDescriptor(XReplaceable xReplaceable, String what, String replacement, Boolean isRegex ) {
		XReplaceDescriptor xReplaceDescriptor = null;		
		xReplaceDescriptor =xReplaceable.createReplaceDescriptor();
		xReplaceDescriptor.setSearchString(what);
		xReplaceDescriptor.setReplaceString(replacement);
		try{
			xReplaceDescriptor.setPropertyValue("SearchRegularExpression", isRegex);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return xReplaceDescriptor;
	}

	public XComponent getXComponent() {
		return xComponent;
	}

	public void setXComponent(XComponent component) {
		xComponent = component;
	}

	/**
	 * Get all the links from the document
	 * This does not include links from tables (yet). For that check out http://www.oooforum.org/forum/viewtopic.phtml?t=19013
	 * @return
	 */
	public Map<XTextRange,XPropertySet> getLinks() {
		
		Map<XTextRange,XPropertySet> links = new HashMap<XTextRange, XPropertySet>();
		
		try {	
			
			XEnumerationAccess xEnumerationAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, doc.getText());
			XEnumeration xParagraphEnumeration = null;
			XTextContent xTextElement = null;
			XEnumerationAccess xParaEnumerationAccess = null;
			XEnumeration xTextPortionEnum;
			xParagraphEnumeration = xEnumerationAccess.createEnumeration();

			// Loop through all paragraphs of the document
			// http://www.oooforum.org/forum/viewtopic.phtml?t=5804
			while (xParagraphEnumeration.hasMoreElements()) {
				xTextElement = (XTextContent) UnoRuntime.queryInterface(XTextContent.class,xParagraphEnumeration.nextElement());
				XServiceInfo xServiceInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, xTextElement);

				// check if the current paragraph really a paragraph or an anchor of a frame or picture
				if (xServiceInfo.supportsService("com.sun.star.text.Paragraph")) {
//					XTextRange xTextRange = xTextElement.getAnchor();

					// create another enumeration to get all text portions of the paragraph
//					xParaEnumerationAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, xTextElement);
					xParaEnumerationAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, xServiceInfo);
					xTextPortionEnum = xParaEnumerationAccess.createEnumeration();

					while (xTextPortionEnum.hasMoreElements()) {
						
						XTextRange xTextRange = (XTextRange)UnoRuntime.queryInterface(XTextRange.class, xTextPortionEnum.nextElement());
			        	
			        	XPropertySet xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTextRange);
			        	
			        	try {
			        		String link = (String)xPropertySet.getPropertyValue("HyperLinkURL");
			        		if(!StringUtils.isEmpty(link)) {
			        			System.out.println("Found link");
								links.put(xTextRange, xPropertySet);
							}
			        	} catch (Exception e) {
							e.printStackTrace();
							logger.error("Cannot get object with property HyperLinkURL", e);
						}
			        	
			        	
			        	//link[1] = xTextRange.getString();
			        	
			        	//if (link[0] != null && link[0].length() > 0) 
			        		
						
						/*
						
						Object nextElement = xTextPortionEnum.nextElement();
						XTextContent xTextPortion = (XTextContent) UnoRuntime
								.queryInterface(XTextContent.class, nextElement);
						
						if(xTextPortion != null) {
						
							//XTextRange xTextRange1 = xTextPortion.getAnchor();
							
							XPropertySet xPropertySet = (XPropertySet) UnoRuntime .queryInterface(XPropertySet.class, xTextPortion);
							
							try {
								String link = (String) xPropertySet.getPropertyValue("HyperLinkURL");
								if(!StringUtils.isEmpty(link)) {
									links.put(xTextPortion,xPropertySet);
								}
							}catch(Exception e1) {
								e1.printStackTrace();
							}
						} else {
							System.err.println("xTextPortion is null");
							System.err.println("nextElement: " + nextElement);
						}
						*/
					}
				} else {
					// System.out.println(
					// "The text portion isn't a text paragraph" );
				}
			}
		} catch (Exception e) {
	         e.printStackTrace();
	         logger.error("Exception in getLinks()", e);
	     }
		return links;
	}	
	
	public static XComponentLoader connect () throws Exception {
		
		String connString = "uno:socket,host=localhost,port=8100;urp;StarOffice.ServiceManager";
		XComponentContext xcomponentcontext = com.sun.star.comp.helper.Bootstrap
				.createInitialComponentContext(null);
		XMultiComponentFactory xmulticomponentfactory = xcomponentcontext
				.getServiceManager();
		Object objectUrlResolver = xmulticomponentfactory
				.createInstanceWithContext(
				"com.sun.star.bridge.UnoUrlResolver",
				xcomponentcontext);
		XUnoUrlResolver xurlresolver = (XUnoUrlResolver) UnoRuntime
				.queryInterface(XUnoUrlResolver.class, objectUrlResolver);
		Object objectInitial = xurlresolver.resolve(connString);
		xmulticomponentfactory = (XMultiComponentFactory) UnoRuntime
				.queryInterface(XMultiComponentFactory.class, objectInitial);
		XPropertySet xpropertysetMultiComponentFactory = (XPropertySet) UnoRuntime
				.queryInterface(XPropertySet.class, xmulticomponentfactory);
		Object objectDefaultContext = xpropertysetMultiComponentFactory
				.getPropertyValue("DefaultContext");
		xcomponentcontext = (XComponentContext) UnoRuntime.queryInterface(
				XComponentContext.class, objectDefaultContext);
		XComponentLoader xcomponentloader = (XComponentLoader) UnoRuntime
				.queryInterface(XComponentLoader.class,
						xmulticomponentfactory.createInstanceWithContext(
								"com.sun.star.frame.Desktop",
								xcomponentcontext));
		return xcomponentloader;
	}
	
	public void closeOO () {
		if(xComponent!=null) {
			closeOO(xComponent);
			xComponent = null;
		}
	}
	
	
	public static void closeOO ( XComponent xComponent)
	{
		try 
		{
			XCloseable xCloseable = (XCloseable) UnoRuntime.queryInterface(
					XCloseable.class, xComponent);
			if (xCloseable != null) {
				xCloseable.close(false);
			} else {
				xComponent.dispose();
			}
		}catch (Exception e) { /*e.printStackTrace()*/;}
	}
		
	@Override
	protected void finalize() throws Throwable {
		/*closeOO();*/
		super.finalize();
	}

	public XTextDocument getDoc() {
		return doc;
	}

	public void setDoc(XTextDocument doc) {
		this.doc = doc;
	}
	
	@Override
	public void replaceCurrentMatchEscapeHtml(String replacement) {
		
		File tempFile = FileUtils.createTemporaryFileWithContent(
				 ("<html> <head> </head> <body> " + replacement + " </body> </html> ").getBytes()
				);
		XText text = getDoc().getText(); 
		XTextCursor textCurs = text.createTextCursor(); 
		textCurs = text.createTextCursorByRange(xtr);
		XDocumentInsertable docInsertable = (XDocumentInsertable) UnoRuntime.queryInterface(XDocumentInsertable.class, textCurs); 
		StringBuffer sUrl = new StringBuffer("file:///"); 
		try {
			sUrl.append(tempFile.getCanonicalPath().replace('\\', '/'));
			docInsertable.insertDocumentFromURL(sUrl.toString(), new PropertyValue[0]);
		
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			org.apache.commons.io.FileUtils.deleteQuietly(tempFile);
		}
		
	}
	
	public static void main(String[] args) {
		
//		int nrParagrafe = 0;
		try {
			OfficeDocumentContents oodoc = new OfficeDocumentContents("C:\\Users\\Andrei\\Desktop\\MO.doc");
			
			
			oodoc.findAll(Pattern.compile("<#[^#]*?/#>"));
			
			Pattern patKey=null;
			String key = "TSD_Patriots";
			patKey = Pattern.compile("<#[ \t]*"+ key.replace("(", "\\(").replace(")", "\\)") +"[ \t]*/#>");
			
			oodoc.findAll(patKey);
			while( oodoc.find()){
				
				//oodoc.replaceCurrentMatchEscape("replData");
				
				File tempFile = new File("C:\\Users\\Andrei\\Desktop\\122.html");
				
				
				XTextDocument textDoc = oodoc.getDoc(); 
				XText text = textDoc.getText(); 
				XTextCursor textCurs = text.createTextCursor(); 
				//oodoc.xtr.
				// Getting the cursor on the document 
				textCurs = text.createTextCursorByRange(oodoc.xtr);
				
				XDocumentInsertable docInsertable = (XDocumentInsertable) UnoRuntime.queryInterface(XDocumentInsertable.class, textCurs); 
				
				//XDocumentInsertable docInsertable = (XDocumentInsertable) UnoRuntime.queryInterface(XDocumentInsertable.class, textCurs); 

				StringBuffer sUrl = new StringBuffer("file:///"); 
				sUrl.append(tempFile.getCanonicalPath().replace('\\', '/')); 

				docInsertable.insertDocumentFromURL(sUrl.toString(), new PropertyValue[0]); 
				
				
				
			}
			oodoc.saveToFile("C:\\Users\\Andrei\\Desktop\\MO2.doc");
			OfficeDocumentContents.closeOO(((OfficeDocumentContents)oodoc).getXComponent());
			
			/*
			Map<XTextContent,XPropertySet> links = new HashMap<XTextContent, XPropertySet>();
			
			try {	
				
				XEnumerationAccess xEnumerationAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, oodoc.getDoc().getText());
				XEnumeration xParagraphEnumeration = null;
				XTextContent xTextElement = null;
				XEnumerationAccess xParaEnumerationAccess = null;
				XEnumeration xTextPortionEnum;
				xParagraphEnumeration = xEnumerationAccess.createEnumeration();

				
				
				// Loop through all paragraphs of the document
				// http://www.oooforum.org/forum/viewtopic.phtml?t=5804
				while (xParagraphEnumeration.hasMoreElements()) {
					xTextElement = (XTextContent) UnoRuntime.queryInterface(XTextContent.class,xParagraphEnumeration.nextElement());
					XServiceInfo xServiceInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, xTextElement);

					// check if the current paragraph really a paragraph or an anchor of a frame or picture
					if (xServiceInfo.supportsService("com.sun.star.text.Paragraph")) {
						nrParagrafe ++;
						
						
						String[] hyperLink = getHyperLink(xServiceInfo);
						
						System.out.println(hyperLink);
						
					}
				}
			} catch (Exception e) {
		         e.printStackTrace();
		     }
			System.out.println("Nr Paragrafe: " + nrParagrafe);
			System.out.println("gpog");
			*/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected static String[] getHyperLink(Object oParagraph) throws Exception {
		String[] link = new String[2];
		
        XEnumerationAccess xEnumerationAccess = (XEnumerationAccess)UnoRuntime.queryInterface(
        		XEnumerationAccess.class, oParagraph);
        XEnumeration xEnumeration = xEnumerationAccess.createEnumeration();
        
        while (xEnumeration.hasMoreElements()) {
        	XTextRange xTextRange = (XTextRange)UnoRuntime.queryInterface(XTextRange.class,
        			xEnumeration.nextElement());
        	
        	XPropertySet xPropertySet1 = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTextRange);
        	
        	link[0] = xPropertySet1.getPropertyValue("HyperLinkURL") + "";
        	link[1] = xTextRange.getString();
        	
        	if (link[0] != null && link[0].length() > 0) 
        		return link;
        }
        
        return null;
	}
	
	
}
