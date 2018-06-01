package ro.cst.tsearch.templates;

import java.io.File;
import java.util.ArrayList;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.bridge.XBridge;
import com.sun.star.bridge.XBridgeFactory;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.connection.XConnection;
import com.sun.star.connection.XConnector;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.frame.TerminationVetoException;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStorable;
import com.sun.star.frame.XTerminateListener;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XEventListener;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.style.XStyle;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.text.XFootnote;
import com.sun.star.text.XFootnotesSupplier;
import com.sun.star.text.XSimpleText;
import com.sun.star.text.XText;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextTable;
import com.sun.star.text.XTextTablesSupplier;
import com.sun.star.uno.Any;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XCloseable;

/**
 * Helper class, collected from web....
 *
 */
public class SOfficeConnection {
	
	public static void init(String host, int port) {
		SOfficeConnection.host = host;
		SOfficeConnection.port = port;
	}
	
	private static boolean connect() {
		try {
    		xRemoteContext = Bootstrap.createInitialComponentContext(null); 
        	
            Object x = xRemoteContext.getServiceManager().createInstanceWithContext(
                    "com.sun.star.connection.Connector", xRemoteContext);

            XConnector xConnector = (XConnector)UnoRuntime.queryInterface(XConnector.class, x);
            XConnection connection = xConnector.connect("socket,host=" + host + ",port=" + port);
            
            x = xRemoteContext.getServiceManager().createInstanceWithContext(
                    "com.sun.star.bridge.BridgeFactory", xRemoteContext);
            
            XBridgeFactory xBridgeFactory = (XBridgeFactory)UnoRuntime.queryInterface(
                    XBridgeFactory.class, x);
            
            bridge = xBridgeFactory.createBridge("", "urp", connection, null);

            // event for closing connection
            addBridgeDisposeListener(new XEventListener() {
				public void disposing(EventObject arg0) {
		    		close();
				}
			});
            
            x = bridge.getInstance("StarOffice.ServiceManager");
            
            XMultiComponentFactory xRemoteServiceManager = (XMultiComponentFactory)
                    UnoRuntime.queryInterface(XMultiComponentFactory.class, x);
           
            XPropertySet xProperySet = (XPropertySet)
            		UnoRuntime.queryInterface(XPropertySet.class, xRemoteServiceManager);
                
            Object oDefaultContext = xProperySet.getPropertyValue("DefaultContext");
                
            XComponentContext xOfficeComponentContext = (XComponentContext)UnoRuntime.queryInterface(
           			XComponentContext.class, oDefaultContext);
           
           	desktop = xRemoteServiceManager.createInstanceWithContext(
        		   "com.sun.star.frame.Desktop", xOfficeComponentContext);
           	
    		xComponentLoader = (XComponentLoader)UnoRuntime.queryInterface( 
    			    XComponentLoader.class, desktop);
           	
           	XDesktop xDesktop = (XDesktop)UnoRuntime.queryInterface(XDesktop.class, desktop);
           
           	// event for closing soffice program, and answer is exception that will keep alive it
           	xDesktop.addTerminateListener(new XTerminateListener() {

				public void notifyTermination(EventObject e) {
				}

				public void queryTermination(EventObject e) throws TerminationVetoException {
					throw new TerminationVetoException();
				}

				public void disposing(EventObject e) {
				}
           		
           	});

           	return true;
    	} catch (Exception e) {
    		clear();
    		
    		return false;
    	}
	}
	
	private static void addBridgeDisposeListener(XEventListener event) {
        XComponent xComponent = (XComponent)UnoRuntime.queryInterface(XComponent.class, bridge);
        xComponent.addEventListener(event);
	}

	public static XComponent newDocument(String document, PropertyValue[] loadProps) {
		if (xComponentLoader == null) {
			close();
			connect();
		}
		
		try {
        	return xComponentLoader.loadComponentFromURL("private:factory/" + document,
        			"_blank", 0, loadProps);
        } catch (Exception e) {
        	return null;
        }
	}
	
	public static XComponent loadDocument(File file, PropertyValue[] loadProps) {
		if (xComponentLoader == null) {
			close();
			connect();
		}

		try  {
			StringBuilder filename = new StringBuilder(file.toURI().toString());
			filename.insert(6, "//");
    		
			return xComponentLoader.loadComponentFromURL(filename.toString(), "_blank", 0, loadProps);
        } catch (Exception e) {
        	return null;
        }
	}
	
	public static boolean storeDocument(XComponent xDocument) {
		try {
			XStorable xStorable = (XStorable)UnoRuntime.queryInterface(XStorable.class, xDocument);
			xStorable.store();
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static  boolean storeAsDocument(XComponent xDocument, File file, PropertyValue[] storeProps) {
		try {
			XStorable xStorable = (XStorable)UnoRuntime.queryInterface(XStorable.class, xDocument);
			
			StringBuilder filename = new StringBuilder(file.toURI().toString());
			filename.insert(6, "//");

			xStorable.storeAsURL(filename.toString(), storeProps);
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean storeToDocument(XComponent xDocument, File file, PropertyValue[] storeProps) {
		try {
			XStorable xStorable = (XStorable)UnoRuntime.queryInterface(XStorable.class, xDocument);
			
			StringBuilder filename = new StringBuilder(file.toURI().toString());
			filename.insert(6, "//");
			
        	xStorable.storeToURL(filename.toString(), storeProps);
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public static boolean closeDocument(XComponent xDocument) {
		try {
			XModel xModel = (XModel)UnoRuntime.queryInterface(XModel.class, xDocument);
			XCloseable xCloseable = (XCloseable)UnoRuntime.queryInterface(XCloseable.class, xModel);

			xCloseable.close(true);
				
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public static ArrayList<String[]> getFormatedText(File file) {
		ArrayList<String[]> res = new ArrayList<String[]>();
		
		PropertyValue[] loadProps = new PropertyValue[2]; 
		loadProps[0] = new PropertyValue(); 
		loadProps[0].Name = "Hidden"; 
		loadProps[0].Value = new Boolean(true);  
		loadProps[1] = new PropertyValue(); 
		loadProps[1].Name = "ReadOnly"; 
		loadProps[1].Value = new Boolean(true);  

		XComponent xDocument = SOfficeConnection.loadDocument(file, loadProps);
		XModel xModel = null;

		if (xDocument != null) {
			try {
				xModel = (XModel) UnoRuntime.queryInterface(XModel.class, xDocument);
			
				XTextDocument xTextDocument = (XTextDocument)UnoRuntime.queryInterface(XTextDocument.class, xModel);
				XText xText = xTextDocument.getText();
				
				XTextTablesSupplier xTablesSupplier = (XTextTablesSupplier) UnoRuntime.queryInterface(
				                 XTextTablesSupplier.class, xTextDocument );

				XNameAccess xNamedTables = xTablesSupplier.getTextTables();

				XIndexAccess xIndexedTables = (XIndexAccess) UnoRuntime.queryInterface(
				                 XIndexAccess.class, xNamedTables);

				boolean find = false;
				for (int i = 0; i < xIndexedTables.getCount() && !find; i++) {
					Object table = xIndexedTables.getByIndex(i);
					XTextTable xTable = (XTextTable) UnoRuntime.queryInterface(XTextTable.class, table);
					int rowCnt = xTable.getRows().getCount();
					int colCnt = xTable.getColumns().getCount();

					if (rowCnt == 1 && colCnt == 2) {
						XText xCellText = (XText) UnoRuntime.queryInterface(XText.class, xTable.getCellByName("A1"));

						String[] data = new String[3];
						data[0] = "Jnep_DOI";
						data[1] = "en";
						data[2] = xCellText.getString().trim().replace("\'", "\\\'");

		            	if (data[2] != null && data[2] != "" && data[2].length() > 0)
		            		res.add(data);
	
						xCellText = (XText) UnoRuntime.queryInterface(XText.class, xTable.getCellByName("B1"));
	
			        	data = new String[3];
		            	data[0] = "Jnep_PACS";
		            	data[1] = "en";
		            	data[2] = xCellText.getString().trim().replace("\'", "\\\'");
	
		            	if (data[2] != null && data[2] != "" && data[2].length() > 0)
		            		res.add(data);
		            	
		            	find = true;
					}
				}				

				XStyleFamiliesSupplier xStyleFamiliesSupplier = (XStyleFamiliesSupplier) UnoRuntime.queryInterface( 
						XStyleFamiliesSupplier.class, xTextDocument); 
					       
				XNameAccess xStyleFamilies = xStyleFamiliesSupplier.getStyleFamilies(); 
					       
				XNameContainer xPageStyles = (XNameContainer) UnoRuntime.queryInterface( 
						XNameContainer.class, xStyleFamilies.getByName("PageStyles")); 
					       
				XStyle xStyle = (XStyle) UnoRuntime.queryInterface( 
						XStyle.class, xPageStyles.getByName("First Page"));
					       
				XPropertySet xStyleProps = (XPropertySet) UnoRuntime.queryInterface( 
						XPropertySet.class, xStyle);				

			    Boolean bIsHeaderOn = (Boolean) xStyleProps.getPropertyValue("HeaderIsOn"); 
			            
		        if (bIsHeaderOn.booleanValue()) {
					XText xHeaderText = (XText) UnoRuntime.queryInterface(XText.class, xStyleProps.getPropertyValue("HeaderText"));

		        	String[] data = new String[3];
	            	data[0] = "Jnep_Header";
	            	data[1] = "en";
	            	data[2] = xHeaderText.getString().trim().replace("\'", "\\\'");

	            	if (data[2] != null && data[2] != "" && data[2].length() > 0)
	            		res.add(data);
		        }

		        XFootnotesSupplier xFootnoteSupplier = (XFootnotesSupplier) UnoRuntime.queryInterface(
		             XFootnotesSupplier.class, xTextDocument );
		         
		        XIndexAccess xFootnotes = ( XIndexAccess ) UnoRuntime.queryInterface (
		        		XIndexAccess.class, xFootnoteSupplier.getFootnotes());		        
		        
		        for (int i = 0; i < xFootnotes.getCount(); i++) {
		            XFootnote xNumbers = ( XFootnote ) UnoRuntime.queryInterface ( 
		                XFootnote.class, xFootnotes.getByIndex(i));
		        	
		            XSimpleText xSimple = (XSimpleText ) UnoRuntime.queryInterface (
		                XSimpleText.class, xNumbers);
		            
	            	String[] data = new String[4];
	            	data[0] = "Jnep_Email";
	            	data[1] = "en";
	            	data[2] = xSimple.getString().trim().replace("\'", "\\\'");
	            	data[3] = xNumbers.getAnchor().getString().trim().replace("\'", "\\\'");

	            	if (data[2] != null && data[2] != "" && data[2].length() > 0)
	            		res.add(data);
		        }
		        
				XEnumerationAccess xParaAccess = (XEnumerationAccess) UnoRuntime.queryInterface(
		            XEnumerationAccess.class, xText);

		        XEnumeration xParaEnum = xParaAccess.createEnumeration();
		 
		        // check all paragraphs
		        boolean pageOffset = false;
		        
		        while (xParaEnum.hasMoreElements()) {
		            Object oParagraph = xParaEnum.nextElement();
		        	XServiceInfo xInfo = (XServiceInfo) UnoRuntime.queryInterface(
		                XServiceInfo.class, oParagraph);

		            if (xInfo.supportsService("com.sun.star.text.Paragraph")) {
		            	XTextRange xTextRange1 = (XTextRange)UnoRuntime.queryInterface(
		            			XTextRange.class, oParagraph);
		            	
		            	XPropertySet xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, oParagraph);

		            	
		            	if (!pageOffset) {
			            	String[] data = new String[4];
			            	data[0] = "Jnep_Footer";
			            	data[1] = "en";
			            	data[2] = xPropertySet.getPropertyValue("PageNumberOffset") + "";

			            	if (data[2] != null && data[2] != "" && data[2].length() > 0)
			            		res.add(data);

			            	pageOffset = true;
		            	}
		            	
		            	String[] data = new String[5];
		            	data[0] = xPropertySet.getPropertyValue("ParaStyleName") + "";
		            	data[1] = getLocale(oParagraph);
		            	if (data[1] == null) 
		            		data[1] = ((com.sun.star.lang.Locale) xPropertySet.getPropertyValue("CharLocale")).Language;
		            	data[2] = xTextRange1.getString().trim().replace("\'", "\\\'");

		            	if (data[0].equals("Jnep_References")) {
		            		String[] link = getHyperLink(oParagraph);
		            		
		            		if (link != null) {
		            			data[3] = link[0];
		            			data[4] = link[1];
		            		}
		            	}

		            	if (data[0].equals("Jnep_Autors")) {
		            		data[2] = "";
		            		
		        	        XEnumerationAccess xEnumerationAccess = (XEnumerationAccess)UnoRuntime.queryInterface(
		                    		XEnumerationAccess.class, oParagraph);
		                    XEnumeration xEnumeration = xEnumerationAccess.createEnumeration();
		                    
		                    while (xEnumeration.hasMoreElements()) {
		                    	XTextRange xTextRange = (XTextRange)UnoRuntime.queryInterface(XTextRange.class,
		                    			xEnumeration.nextElement());
		                    	
		                    	XPropertySet xPropertySet1 = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTextRange);
		                    	
		                    	Object d = ((Any) xPropertySet1.getPropertyValue("Footnote")).getObject();
		                    	if (d != null && d instanceof XFootnote) {
		                    		data[2] += "[" + ((XFootnote) d).getAnchor().getString() + "]";
		                    	} else {
		                    		data[2] += xTextRange.getString();
		                    	}
		                    }
		            	}
		            	
		            	if (data[2] != null && data[2] != "" && data[2].length() > 0)
		            		res.add(data);
		            } else {
		            	
		            }
		        }
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			SOfficeConnection.closeDocument(xDocument);
		}

		return res;
	}
	
	private static String[] getHyperLink(Object oParagraph) throws Exception {
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
        	
        	if (link[0] != null && link[0].length() > 0) return link;
        }
        
        return null;
	}
	
	private static String getLocale(Object oParagraph) throws Exception {
        XEnumerationAccess xEnumerationAccess = (XEnumerationAccess)UnoRuntime.queryInterface(
        		XEnumerationAccess.class, oParagraph);
        XEnumeration xEnumeration = xEnumerationAccess.createEnumeration();
        XPropertySet xPropertySet = null;
        
		while (xEnumeration.hasMoreElements()) {
        	XTextRange xTextRange = (XTextRange)UnoRuntime.queryInterface(
        			XTextRange.class, xEnumeration.nextElement());
        	
        	xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,
        			xTextRange);
        	
    		if (xTextRange.getString().length() > 2)
    			return ((com.sun.star.lang.Locale) xPropertySet.getPropertyValue("CharLocale")).Language; 
        }
		
		return null;
	}
	
	public static boolean close() {
		// if already closed
		if (bridge == null) return true;
		
		try {
			XComponent xComponent = (XComponent)UnoRuntime.queryInterface(XComponent.class, bridge);
            xComponent.dispose();
            
            clear();

    		return true;
		} catch (Exception e) {
        	clear();
        	
        	return false;
		}
	}

	private static void clear() {
		bridge = null;
		desktop = null;
		xRemoteContext = null;
		xComponentLoader = null;
	}
	
	
	private static XBridge bridge = null;
	private static Object desktop = null;
	private static XComponentContext xRemoteContext = null;
	private static XComponentLoader xComponentLoader = null;
	private static String host = null;
	private static int port = 0;
}