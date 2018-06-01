package ro.cst.tsearch.parentsitedescribe;

import java.io.File;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ResourceBundle;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.servers.bean.SearchManager;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.URLMaping;

public class DSMXMLReader {

	static public String DIRECTOR = "";
	public static boolean readBD = false;
	protected static Hashtable<String, DefaultServerInfoMap> cache = null;
	public static Hashtable<String, String> stringPage = null;
	private static LinkedList<String> listCache = null;
	public static LinkedList<String> listFile = null;
	static {
		BuildCache();
	}

	private static void BuildCache() {

		listFile = new LinkedList<String>();

		cache = new Hashtable<String, DefaultServerInfoMap>();
		stringPage = new Hashtable<String, String>();
		listCache = new LinkedList<String>();

		ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
		DSMXMLReader.DIRECTOR = ro.cst.tsearch.servlet.BaseServlet.REAL_PATH
				+ rbc.getString("parentsite.xml.path").trim();
		File f = new File(DIRECTOR);
		String listF[] = null;
		listF = f.list();

		if (listF.length > 0) {
			for (int i = 0; i < listF.length; i++) {

				DSMXMLReader temp = new DSMXMLReader(listF[i]);
				if (!listF[i].contains("svn")) {
					listFile.add(listF[i]);
					DSMXMLReader.cache.put(listF[i], temp.readXML());
					// DSMXMLReader.cache.put(list[i], readToStart(list[i]))
					System.err
							.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> AM SCRIS FISIERU   >>>   "
									+ listF[i] + "    <<<   in    CACHE");

				}

			}
		}
	}

	public static boolean isSite(String siteName) {
		if (DSMXMLReader.cache.get(siteName + ".xml") == null) {
			return false;
		} else {
			return true;
		}
	}

	private Document document;
	private String stringFile = null;

	public DSMXMLReader(String stringFile) {
		super();
		this.stringFile = stringFile;
	}

	private HtmlControlMap readHtml(Node node) {
		HtmlControlMap controlHtml = new HtmlControlMap();
		String tmp;
		boolean inNode = false;
		NodeList listh = node.getChildNodes();
		for (int i = 0; i < listh.getLength(); i++) {

			// controlHtml = new HtmlControlMap();
			if (listh.item(i).getNodeName().compareToIgnoreCase("controlType") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					inNode = true;
					controlHtml.setControlType(Integer.parseInt(listh.item(i)
							.getFirstChild().getNodeValue()));
				}
			}

			if (listh.item(i).getNodeName().compareToIgnoreCase("name") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					inNode = true;
					controlHtml.setName(listh.item(i).getFirstChild()
							.getNodeValue());
				}
			}

			if (listh.item(i).getNodeName().compareToIgnoreCase("label") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					inNode = true;
					controlHtml.setLabel(listh.item(i).getFirstChild()
							.getNodeValue());
				}
			}

			if (listh.item(i).getNodeName().compareToIgnoreCase("colStart") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					inNode = true;
					controlHtml.setColStart(Integer.parseInt(listh.item(i)
							.getFirstChild().getNodeValue()));
				}
			}

			if (listh.item(i).getNodeName().compareToIgnoreCase("colEnd") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					inNode = true;
					controlHtml.setColEnd(Integer.parseInt(listh.item(i)
							.getFirstChild().getNodeValue()));
				}
			}

			if (listh.item(i).getNodeName().compareToIgnoreCase("rowStart") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					inNode = true;
					controlHtml.setRowStart(Integer.parseInt(listh.item(i)
							.getFirstChild().getNodeValue()));
				}
			}

			if (listh.item(i).getNodeName().compareToIgnoreCase("rowEnd") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					inNode = true;
					controlHtml.setRowEnd(Integer.parseInt(listh.item(i)
							.getFirstChild().getNodeValue()));
				}
			}

			if (listh.item(i).getNodeName().compareToIgnoreCase("size") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					inNode = true;
					controlHtml.setSize(Integer.parseInt(listh.item(i)
							.getFirstChild().getNodeValue()));
				}
			}
			if (listh.item(i).getNodeName().compareToIgnoreCase("defaultValue") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					tmp = listh.item(i).getFirstChild().getNodeValue();
					inNode = true;
					if (tmp.compareToIgnoreCase("null") == 0) {
						controlHtml.setDefaultValue(null);
					} else {
						controlHtml.setDefaultValue(tmp);
					}
				} else {
					controlHtml.setDefaultValue("");
				}
				controlHtml.setDefValue(controlHtml.getDefaultValue());
			}

			if (listh.item(i).getNodeName().compareToIgnoreCase("tssFunction") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					inNode = true;
					controlHtml.setTssFunction(Integer.parseInt(listh.item(i)
							.getFirstChild().getNodeValue()));
				}
			}

			if (listh.item(i).getNodeName().compareToIgnoreCase("FieldNote") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					inNode = true;
					controlHtml.setFieldNote(listh.item(i).getFirstChild()
							.getNodeValue());
				}
			}
			if (listh.item(i).getNodeName().compareToIgnoreCase("SelectValue") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					inNode = true;
					controlHtml.setComboValue(listh.item(i).getFirstChild()
							.getNodeValue());
				}
			}

			if (listh.item(i).getNodeName()
					.compareToIgnoreCase("valueRequired") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					tmp = listh.item(i).getFirstChild().getNodeValue();
					inNode = true;
					if (tmp.compareToIgnoreCase("true") == 0) {
						controlHtml.setValueRequired(true);
					} else {
						controlHtml.setValueRequired(false);
					}
				}
			}

			if (listh.item(i).getNodeName().compareToIgnoreCase("requiredExcl") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					tmp = listh.item(i).getFirstChild().getNodeValue();
					inNode = true;
					if (tmp.compareToIgnoreCase("true") == 0) {
						controlHtml.setRequiredExcl(true);
					} else {
						controlHtml.setRequiredExcl(false);
					}
				}
			}

			if (listh.item(i).getNodeName()
					.compareToIgnoreCase("requiredCritical") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					tmp = listh.item(i).getFirstChild().getNodeValue();
					inNode = true;
					if (tmp.compareToIgnoreCase("true") == 0) {
						controlHtml.setRequiredCritical(true);
					} else {
						controlHtml.setRequiredCritical(false);
					}
				}
			}

			if (listh.item(i).getNodeName()
					.compareToIgnoreCase("horizontalRadioButton") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					tmp = listh.item(i).getFirstChild().getNodeValue();
					inNode = true;
					if (tmp.compareToIgnoreCase("true") == 0) {
						controlHtml.setHorizontalRadioButton(true);
					} else {
						controlHtml.setHorizontalRadioButton(false);
					}
				}
			}

			if (listh.item(i).getNodeName().compareToIgnoreCase("justifyField") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					tmp = listh.item(i).getFirstChild().getNodeValue();
					inNode = true;
					if (tmp.compareToIgnoreCase("true") == 0) {
						controlHtml.setJustifyField(true);
					} else {
						controlHtml.setJustifyField(false);
					}
				}
			}

			if (listh.item(i).getNodeName()
					.compareToIgnoreCase("radioDefaultSelection") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					inNode = true;
					controlHtml.setRadioDefaultChecked(listh.item(i)
							.getFirstChild().getNodeValue());
				}
			}

			if (listh.item(i).getNodeName().compareToIgnoreCase("JSFunction") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					inNode = true;
					controlHtml.setJSFunction(listh.item(i).getFirstChild()
							.getNodeValue());
				}
			}

			if (listh.item(i).getNodeName().compareToIgnoreCase("HtmlString") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					inNode = true;
					controlHtml.setHtmlString(listh.item(i).getFirstChild()
							.getNodeValue());
				}
			}

			if (listh.item(i).getNodeName().compareToIgnoreCase("HiddenParam") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					tmp = listh.item(i).getFirstChild().getNodeValue();
					inNode = true;
					if (tmp.compareToIgnoreCase("true") == 0) {
						controlHtml.setHiddenparam(true);
					} else {
						controlHtml.setHiddenparam(false);
					}
				}
			}

			if (listh.item(i).getNodeName().compareToIgnoreCase("extraClass") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					inNode = true;
					controlHtml.setExtraClass(listh.item(i).getFirstChild()
							.getNodeValue());
				}
			}
			
			if (listh.item(i).getNodeName().compareToIgnoreCase("defaultOnReplicate") == 0) {
				if (listh.item(i).getFirstChild().getNodeValue() != null) {
					tmp = listh.item(i).getFirstChild().getNodeValue();
					inNode = true;
					if (tmp.compareToIgnoreCase("true") == 0) {
						controlHtml.setDefaultOnReplicate(true);
					} 
				}
			}

		}
		if (inNode) {
			return controlHtml;
		} else {
			return null;
		}
	}

	public LinkedList<Param> readParam(Node node) {

		LinkedList<Param> listParam = new LinkedList<Param>();
		NodeList listp = node.getChildNodes();
		Param parametru = null;
		NodeList listp2 = null;
		// NodeList listp3=null;
		for (int i = 0; i < listp.getLength(); i++) {
			parametru = new Param();
			boolean isPara = false;
			listp2 = listp.item(i).getChildNodes();
			for (int j = 0; j < listp2.getLength(); j++) {
				if (listp2.item(j).getNodeName().compareToIgnoreCase("NAME") == 0) {
					if (listp2.item(j).getFirstChild().getNodeValue() != null) {
						isPara = true;
						parametru.setName(listp2.item(j).getFirstChild()
								.getNodeValue().toString());
					}
				}
				if (listp2.item(j).getNodeName().compareToIgnoreCase("TYPE") == 0) {
					if (listp2.item(j).getFirstChild().getNodeValue() != null) {
						isPara = true;
						parametru.setType(listp2.item(j).getFirstChild()
								.getNodeValue().toString());
					}
				}
				if (listp2.item(j).getNodeName().compareToIgnoreCase("VALUE") == 0) {
					if (listp2.item(j).getFirstChild().getNodeValue() != null) {
						isPara = true;
						parametru.setValue(listp2.item(j).getFirstChild()
								.getNodeValue().toString());
					}
				}
				if (listp2.item(j).getNodeName().compareToIgnoreCase("PARCEL") == 0) {
					if (listp2.item(j).getFirstChild().getNodeValue() != null) {
						isPara = true;
						parametru.setParcelID(Integer.parseInt(listp2.item(j)
								.getFirstChild().getNodeValue()));
					}
				}
				if (listp2.item(j).getNodeName()
						.compareToIgnoreCase("ITERATOR") == 0) {
					if (listp2.item(j).getFirstChild().getNodeValue() != null) {
						isPara = true;
						parametru.setIteratorType(Integer.parseInt(listp2
								.item(j).getFirstChild().getNodeValue()));
					}
				}
				if (listp2.item(j).getNodeName().compareToIgnoreCase("KEY") == 0) {
					if (listp2.item(j).getFirstChild().getNodeValue() != null) {
						isPara = true;
						parametru.setSaKey(listp2.item(j).getFirstChild()
								.getNodeValue());
					}
				}

				if (listp2.item(j).getNodeName()
						.compareToIgnoreCase("VALIDATION") == 0) {
					if (listp2.item(j).getFirstChild().getNodeValue() != null) {
						isPara = true;
						parametru.setValidationType(listp2.item(j)
								.getFirstChild().getNodeValue());
					}
				}

				if (listp2.item(j).getNodeName()
						.compareToIgnoreCase("hiddenparamname") == 0) {
					if (listp2.item(j).getFirstChild().getNodeValue() != null) {
						isPara = true;
						parametru.setHiddenName(listp2.item(j).getFirstChild()
								.getNodeValue());
					}
				}
				if (listp2.item(j).getNodeName()
						.compareToIgnoreCase("hiddenparamvalue") == 0) {
					if (listp2.item(j).getFirstChild().getNodeValue() != null) {
						isPara = true;
						parametru.setHiddenValue(listp2.item(j).getFirstChild()
								.getNodeValue());
					}
				}

			}
			if (isPara) {
				listParam.add(parametru);
				isPara = false;
			}
			// function.add(parametru);
		}
		return listParam;
	}

	public FunctionMap readFunction(Node node) {
		FunctionMap function = new FunctionMap();
		NodeList listf = node.getChildNodes();
		for (int i = 0; i < listf.getLength(); i++) {
			if (listf.item(i).getNodeName()
					.compareToIgnoreCase("FunctionsCount") == 0) {
				if (listf.item(i).getFirstChild().getNodeValue() != null) {
					function.setFunctionsCount(Integer.parseInt(listf.item(i)
							.getFirstChild().getNodeValue()));
				}
			}
			if (listf.item(i).getNodeName().compareToIgnoreCase("moduleIndex") == 0) {
				if (listf.item(i).getFirstChild().getNodeValue() != null) {
					function.setModuleIndex(Integer.parseInt(listf.item(i)
							.getFirstChild().getNodeValue()));
				}
			}
			
			if(listf.item(i).getNodeName().compareToIgnoreCase("moduleOrder")==0){
				if(listf.item(i).getFirstChild().getNodeValue()!=null){
					function.setModuleOrder(Integer.parseInt(listf.item(i).getFirstChild().getNodeValue()));
				}
			}
			
			if(listf.item(i).getNodeName().compareToIgnoreCase("searchType")==0){
				if(listf.item(i).getFirstChild().getNodeValue()!=null){
					function.setSearchType(listf.item(i).getFirstChild().getNodeValue());
				}
			}
			
			if (listf.item(i).getNodeName()
					.compareToIgnoreCase("destinationPage") == 0) {
				if (listf.item(i).getFirstChild().getNodeValue() != null) {
					function.setDestinationPage(listf.item(i).getFirstChild()
							.getNodeValue());
				}
			}
			if (listf.item(i).getNodeName()
					.compareToIgnoreCase("destinationMethod") == 0) {
				if (listf.item(i).getFirstChild().getNodeValue() != null) {
					function.setDestinationMethod(Integer.parseInt(listf
							.item(i).getFirstChild().getNodeValue()));
				}
			}
			if (listf.item(i).getNodeName().compareToIgnoreCase("setName") == 0) {
				if (listf.item(i).getFirstChild().getNodeValue() != null) {
					function.setSetName(listf.item(i).getFirstChild()
							.getNodeValue());
				}
			}
			if (listf.item(i).getNodeName().compareToIgnoreCase("setParcelId") == 0) {
				if (listf.item(i).getFirstChild().getNodeValue() != null) {
					function.setSetParcelId(Integer.parseInt(listf.item(i)
							.getFirstChild().getNodeValue()));
				}
			}
			if (listf.item(i).getNodeName().compareToIgnoreCase("setKey") == 0) {
				if (listf.item(i).getFirstChild().getNodeValue() != null) {
					function.setSetKey(listf.item(i).getFirstChild()
							.getNodeValue());
				}
			}
			if (listf.item(i).getNodeName().compareToIgnoreCase("mouleType") == 0) {
				if (listf.item(i).getFirstChild().getNodeValue() != null) {
					function.setMoule(Integer.parseInt(listf.item(i)
							.getFirstChild().getNodeValue()));
				}
			}
			if (listf.item(i).getNodeName().compareToIgnoreCase("visible") == 0) {
				if (listf.item(i).getFirstChild().getNodeValue() != null) {
					if (listf.item(i).getFirstChild().getNodeValue()
							.compareToIgnoreCase("true") == 0) {
						function.setVisible(true);
					} else {
						function.setVisible(false);
					}
				}
			}
			if (listf.item(i).getNodeName().compareToIgnoreCase("visibleFor") == 0) {
				try {
					if (listf.item(i).getFirstChild().getNodeValue() != null) {
						if (listf.item(i).getFirstChild().getNodeValue()
								.compareToIgnoreCase("alladmin") == 0) {
							function.setVisibleFor(SearchManager.ALL_ADMIN);
						} else if (listf.item(i).getFirstChild().getNodeValue()
								.compareToIgnoreCase("comadmin") == 0) {
							function.setVisibleFor(SearchManager.COMMADMIN);
						} else if (listf.item(i).getFirstChild().getNodeValue()
								.compareToIgnoreCase("tscadmin") == 0) {
							function.setVisibleFor(SearchManager.TSCADMIN);
						} else if (listf.item(i).getFirstChild().getNodeValue()
								.compareToIgnoreCase("tsadmin") == 0) {
							function.setVisibleFor(SearchManager.TSADMIN);
						} else {
							function.setVisibleFor(SearchManager.ALL);
						}
					}
				} catch (Exception e) {
					function.setVisibleFor(SearchManager.ALL);
				}
			}

			if (listf.item(i).getNodeName().compareToIgnoreCase("PARAMETRI") == 0) {
				function.add(readParam(listf.item(i)));

			}

			if (listf.item(i).getNodeName()
					.compareToIgnoreCase("ELSEPARAMETRI") == 0) {
				function.addElse(readParam(listf.item(i)));

			}

		}
		// module.add(function);
		return function;
	}

	private PageZoneMap readPageZone(Node node) {
		PageZoneMap zoneMap = new PageZoneMap();
		NodeList listr = node.getChildNodes();
		for (int i = 0; i < listr.getLength(); i++) {
			if (listr.item(i).getNodeName().compareToIgnoreCase("name") == 0) {
				if (listr.item(i).getFirstChild().getNodeValue() != null) {
					zoneMap.setName(listr.item(i).getFirstChild()
							.getNodeValue());
				}
			}

			if (listr.item(i).getNodeName().compareToIgnoreCase("label") == 0) {
				if (listr.item(i).getFirstChild().getNodeValue() != null) {
					zoneMap.setLabel(listr.item(i).getFirstChild()
							.getNodeValue());
				}
			}

			if (listr.item(i).getNodeName().compareToIgnoreCase("orientation") == 0) {
				if (listr.item(i).getFirstChild().getNodeValue() != null) {
					zoneMap.setOrientation(Integer.parseInt(listr.item(i)
							.getFirstChild().getNodeValue()));
				}
			}

			if (listr.item(i).getNodeName()
					.compareToIgnoreCase("alternativeCSS") == 0) {
				if (listr.item(i).getFirstChild().getNodeValue() != null) {
					zoneMap.setAlternativeCSS(listr.item(i).getFirstChild()
							.getNodeValue());
				}
			}
			if (listr.item(i).getNodeName().compareToIgnoreCase("width") == 0) {
				if (listr.item(i).getFirstChild().getNodeValue() != null) {
					if (listr.item(i).getFirstChild().getNodeValue()
							.compareToIgnoreCase("null") == 0) {
						zoneMap.setWidth(null);
					} else {
						zoneMap.setWidth(new Integer(listr.item(i)
								.getFirstChild().getNodeValue()));
					}
				}
			}

			if (listr.item(i).getNodeName().compareToIgnoreCase("height") == 0) {
				if (listr.item(i).getFirstChild().getNodeValue() != null) {
					if (listr.item(i).getFirstChild().getNodeValue()
							.compareToIgnoreCase("null") == 0) {
						zoneMap.setHeight(null);
					} else {
						zoneMap.setHeight(new Integer(listr.item(i)
								.getFirstChild().getNodeValue()));
					}
				}
			}

			if (listr.item(i).getNodeName()
					.compareToIgnoreCase("typeOfMeasure") == 0) {
				if (listr.item(i).getFirstChild().getNodeValue() != null) {
					zoneMap.setTypeOfMeasure(Integer.parseInt(listr.item(i)
							.getFirstChild().getNodeValue()));
				}
			}
			if (listr.item(i).getNodeName().compareToIgnoreCase("Separator") == 0) {
				if (listr.item(i).getFirstChild().getNodeValue() != null) {
					zoneMap.setSeparator(listr.item(i).getFirstChild()
							.getNodeValue());
				}
			}
			if (listr.item(i).getNodeName().compareToIgnoreCase("extraButton") == 0) {
				if (listr.item(i).getFirstChild().getNodeValue() != null) {
					zoneMap.setExtraButton(listr.item(i).getFirstChild()
							.getNodeValue());
				}
			}

			if (listr.item(i).getNodeName().compareToIgnoreCase("formTarget") == 0) {
				if (listr.item(i).getFirstChild().getNodeValue() != null) {
					zoneMap.setFormTarget(listr.item(i).getFirstChild()
							.getNodeValue());
				}
			}

			if (listr.item(i).getNodeName().compareTo("isRoot") == 0) {
				if (listr.item(i).getFirstChild().getNodeValue() != null) {
					String isroot;
					isroot = listr.item(i).getFirstChild().getNodeValue();
					if (isroot.compareTo("true") == 0) {
						zoneMap.setRoot(true);
					} else {
						zoneMap.setRoot(false);
					}
				}
			}

			if (listr.item(i).getNodeName().compareTo("border") == 0) {
				if (listr.item(i).getFirstChild().getNodeValue() != null) {
					String isroot;
					isroot = listr.item(i).getFirstChild().getNodeValue();
					if (isroot.compareTo("true") == 0) {
						zoneMap.setBorder(true);
					} else {
						zoneMap.setBorder(false);
					}
				}
			}
			
			
			if (listr.item(i).getNodeName().compareToIgnoreCase("customFormValidation") == 0) {
				if (listr.item(i).getFirstChild().getNodeValue() != null) {
					zoneMap.setCustomFormValidation(listr.item(i).getFirstChild().getNodeValue());
				}
			}

			/*
			 * if(listr.item(i).getNodeName().compareToIgnoreCase("SetHiddenParam"
			 * )==0){ if(listr.item(i).getFirstChild().getNodeValue()!=null){
			 * zoneMap
			 * .setHiddenParam(listr.item(i).getFirstChild().getNodeValue()); }
			 * }
			 */

			if (listr.item(i).getNodeName().compareTo("HTML") == 0) {
				HtmlControlMap list = readHtml(listr.item(i));
				if (list != null) {
					zoneMap.add(list);
				}

			}

		}
		// module.add(zoneMap);
		return zoneMap;
	}

	private ModuleXMLMap readZone(Node node, int indexModule) {
		ModuleXMLMap module = new ModuleXMLMap();
		NodeList list1 = node.getChildNodes();
		for (int i = 0; i < list1.getLength(); i++) {

			if (list1.item(i).getNodeName().compareTo("FUNCTION") == 0) {
				module.addFunction(readFunction(list1.item(i)));
			}

			if (list1.item(i).getNodeName().compareTo("pagezone") == 0) {
				module.addZone(readPageZone(list1.item(i)));

			}
		}
		// stract.addModule(module);
		return module;
	}

	public DefaultServerInfoMap readXML(boolean isfile) {

		DefaultServerInfoMap stract = null;
		stract = new DefaultServerInfoMap();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			// byte[] byteData = DBManager.getXMLFileContentsFromDb(
			// this.stringFile );
			File f = new File(DIRECTOR + this.stringFile);

			DSMXMLReader.listCache.add(this.stringFile);
			DocumentBuilder builder = factory.newDocumentBuilder();

			document = builder.parse(f);

			NodeList list = null;
			if (document.getElementsByTagName("SERVERADRESS") != null) {
				list = document.getElementsByTagName("SERVERADRESS");
				if (list.item(0) != null) {
					stract.setServerAdress(list.item(0).getFirstChild()
							.getNodeValue());
				}
			}
			if (document.getElementsByTagName("SERVERIP") != null) {
				list = document.getElementsByTagName("SERVERIP");
				if (list.item(0) != null) {
					stract.setServerIp(list.item(0).getFirstChild()
							.getNodeValue());
				}
			}
			if (document.getElementsByTagName("SERVERLINK") != null) {
				list = document.getElementsByTagName("SERVERLINK");
				if (list.item(0) != null) {
					stract.setServerLink(list.item(0).getFirstChild()
							.getNodeValue());
				}
			}

			if (document.getElementsByTagName("GENERICSITE") != null) {
				list = document.getElementsByTagName("GENERICSITE");
				if (list.item(0) != null) {
					stract.setGenericSite(list.item(0).getFirstChild()
							.getNodeValue());
				}
			}

			list = document.getElementsByTagName("zone");

			for (int i = 0; i < list.getLength(); i++) {
				stract.addModule(readZone(list.item(i), i));
			}

		} catch (Exception sxe) {
			sxe.printStackTrace();
		}

		// DSMXMLReader.cache.put(this.stringFile,stract);
		return stract;

	}

	public DefaultServerInfoMap readXML() {

		DefaultServerInfoMap stract = null;

		if ((stract = DSMXMLReader.cache.get(this.stringFile)) != null) {

			return stract;
		}
		if (stract == null) {
			stract = new DefaultServerInfoMap();
			stract = readXML(true);

			DSMXMLReader.cache.put(this.stringFile, stract);
		}

		return stract;

	}

	public static LinkedList<String> getListCache() {
		return listCache;
	}

	public static void replaceCache(String key, DefaultServerInfoMap serverM) {
		/*
		 * boolean isKeyFile = false; for (Enumeration e = cache.keys();
		 * e.hasMoreElements();) { if
		 * (e.nextElement().toString().compareToIgnoreCase(key) == 0) {
		 * isKeyFile = true; }
		 * 
		 * }
		 */
		DSMXMLReader temp = new DSMXMLReader(key);
		cache.put(key, temp.readXML(true));

	}

	public static LinkedList<String> getFileList() {
		LinkedList<String> fileList = new LinkedList<String>();

		String tmpList[] = null;
		File f = new File(DIRECTOR);
		tmpList = f.list();
		if (tmpList.length > 0) {
			for (int i = 0; i < tmpList.length; i++) {
				fileList.add(tmpList[i]);

			}
		}

		return fileList;
	}

}
