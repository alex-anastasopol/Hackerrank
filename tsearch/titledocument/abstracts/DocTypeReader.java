package ro.cst.tsearch.titledocument.abstracts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.extractor.xml.XMLUtils;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.StartsWithFileFilter;
import ro.cst.tsearch.utils.StringUtils;

public class DocTypeReader {

	protected static final Logger logger= Logger.getLogger(DocTypeReader.class);
    protected String context = "";
    protected Map<Integer, Map<Integer, Set<String>>> inter=new HashMap<Integer, Map<Integer, Set<String>>>();

    public DocTypeReader(String c) {
        context=c;
    }

    public Document readFile(String dir) throws Exception {
        try {
        	if (StringUtils.isEmpty(context)){
        		String path = DocTypeReader.class.getClassLoader().getResource("").getPath()+ "dt";
				return XMLUtils.read(new File(path + "/doctype.xml"), 
                        path);
        	}else{
        		return XMLUtils.read(new File(context+dir+"/doctype.xml"), 
                        context+dir);
        	}
            
        } catch (SAXParseException e) {
        	e.printStackTrace();
        	throw new Exception("line:"+e.getLineNumber()+", col:"+e.getColumnNumber()+
                               ", exception:"+e.getException(), e);
        }
    }

    public void read(Document d) throws Exception {
        Node root=d.getDocumentElement();
        NodeList nl=root.getChildNodes();
        for (int i=0; i<nl.getLength(); i++) 
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE) {
                Element state=(Element)nl.item(i);
                Map<Integer, Set<String>> m;
                if (state.getNodeName().equals("DEFAULT")) {
                    m=readCounty(0, state);
                    inter.put(new Integer(0), m);
                } else {
                    String stateName=state.getAttribute("NAME");
                    NodeList nl2=state.getChildNodes();
                    for (int j=0; j<nl2.getLength(); j++)
                        if (nl2.item(j).getNodeType()==Node.ELEMENT_NODE) {
                            Element county=(Element)nl2.item(j);
                            String countyName=county.getAttribute("NAME");
                            int id=getCountyID(stateName, countyName);
                            m=readCounty(id, county);
                            inter.put(new Integer(id), m);
                        }
                }
            }
    }

    protected Map<Integer, Set<String>> readCounty(int id, Element el) throws Exception {
        Map<Integer, Set<String>> r=new HashMap<Integer, Set<String>>();
        NodeList nl=el.getChildNodes();
        for (int i=0; i<nl.getLength(); i++)
            if (nl.item(i).getNodeType()==Node.ELEMENT_NODE) {
                Element categ=(Element)nl.item(i);
                int categID=DocumentTypes.getCategoryID(categ.getAttribute("NAME"));
                Set<String> s=readCategory(categ);
                String ext=categ.getAttribute("EXTENDS");
                if (ext.length()>0) {
                    int[] idExt=getExtendedCategory(ext);
                    Set<String> s2;
                    try {
                        s2=((inter.get(new Integer(idExt[0]))).get(new Integer(idExt[1])));
                    } catch (Exception e) {
                        throw new Exception("ExtendedCategory "+ext+" not defined", e);
                    }
                    if (s2!=null)
                        s.addAll(s2);
                }
                r.put(new Integer(categID), s);
            }
        return r;
    }

    protected Set<String> readCategory(Node n) throws Exception {
        Set<String> s=new HashSet<String>();
        NodeList nl=n.getChildNodes();
        for (int i=0; i<nl.getLength(); i++) {
            Node ni=nl.item(i);
            if (ni.getNodeType()==Node.ELEMENT_NODE && ni.getNodeName()=="DOCTYPE") {
                if (!ni.hasChildNodes())
                    s.add("");
                else
                    s.add(ni.getFirstChild().getNodeValue().trim().replaceAll("\\s{2,}", " ").toUpperCase());
            }
        }
        return s;
    }

    protected int getCountyID(String state, String county) throws Exception {
        int id=County.getCounty(county, state).getCountyId().intValue();
        if (id==0)
            throw new Exception("Invalid pair : "+state+":"+county);
        return id;
    }

    protected int[] getExtendedCategory(String s) throws Exception {
        int[] r=new int[2];
        try {
            s=s.toUpperCase();
            int p1=s.indexOf(":");
            String state=s.substring(0, p1);
            String categ;
            if (state.equals("DEFAULT")) {
                r[0]=0;
                categ=s.substring(p1+1);
            } else {
                int p2=s.indexOf(":", p1+1);
                String county=s.substring(p1+1, p2);
                r[0]=County.getCounty(county, state).getCountyId().intValue();
                categ=s.substring(p2+1);
            }
            r[1]=DocumentTypes.getCategoryID(categ);
        } catch (Exception e) {
            throw new Exception("Invalid ExtendedCategory : "+s+"\n Usage = <state>:<county>:<categ>", e);
        }
        return r;
    }

    public void fill() throws Exception {
        Iterator<Integer> it=inter.keySet().iterator();
        while (it.hasNext()) {
            Integer countyId=(Integer)it.next();
            Map<Integer, Set<String>> m = inter.get(countyId);
            Map<String, Integer> m2=new HashMap<String, Integer>();
            Iterator<Integer> itm=m.keySet().iterator();
            while (itm.hasNext()) {
                Integer categId= itm.next();
                Set<String> s= m.get(categId);
                Iterator<String> its=s.iterator();
                while (its.hasNext()) {      
                    String docType= its.next();
                    if (m2.containsKey(docType)) {
                        String def;
                        if (countyId.intValue()==0)
                            def="'DEFAULT'";
                        else {
                            County c=County.getCounty(new BigDecimal(countyId.toString()));
                            // to get the state, the DB proc must be modified
                            def=c.getName();
                        }
                        throw new Exception("Document type \""+docType+"\" for county "+def+" defined in two categories : "+
                                            DocumentTypes.CATEGORY_NAME[((Integer)m2.get(docType)).intValue()]+" and "+
                                            DocumentTypes.CATEGORY_NAME[((Integer)categId).intValue()]);
                    }
                    m2.put(docType, categId);
                }
            }
            DocumentTypes.docTypes.put(countyId, m2);
        }
        inter=null;
    }

    public static boolean check(File file, Writer w) {
        String s;
    	PrintWriter out=new PrintWriter(w);
        try {
            DocTypeReader dtr=new DocTypeReader(BaseServlet.REAL_PATH);
            Document d;
            try {
                d=XMLUtils.read(file, BaseServlet.REAL_PATH + ServerConfig.getDoctypeFilePath());
            } catch (SAXParseException e) {
                throw new Exception("line:"+e.getLineNumber()+", col:"+e.getColumnNumber()+
                                   ", exception:"+e.getException(), e);
            }
            dtr.read(d);
            dtr.fill();
        } catch (Exception e) {
            StringWriter sw=new StringWriter();
            PrintWriter pw=new PrintWriter(sw);
            pw.println("<H2>ERROR :</H2><br>");
            e.printStackTrace(pw);
            pw.close();
            s=sw.toString();
            s=s.replaceAll("\n", "\n<br>");
        	out.println(s);
			return false;            
        }
        s="It seems to be ok, please send it to the development team for double-check.";
        out.println(s);
        return true;
    }
    
    public static boolean checkGeneratedDocType(File file, Writer w) {
        String s;
    	PrintWriter out=new PrintWriter(w);
        try {
            try {
                XMLUtils.read(generateDocTypeCheckFile(BaseServlet.REAL_PATH + ServerConfig.getDoctypeFilePath(), file), BaseServlet.REAL_PATH + ServerConfig.getDoctypeFilePath());
            } catch (SAXParseException e) {
                throw new Exception("line:"+e.getLineNumber()+", col:"+e.getColumnNumber()+
                                   ", exception:"+e.getException(), e);
            }
        } catch (Exception e) {
            StringWriter sw=new StringWriter();
            PrintWriter pw=new PrintWriter(sw);
            pw.println("<H2>ERROR :</H2><br>");
            e.printStackTrace(pw);
            pw.close();
            s=sw.toString();
            s=s.replaceAll("\n", "\n<br>");
        	out.println(s);
        	
        	logger.error("Uploaded doctype has errors!");
			return false;            
        }
        s="It seems to be ok, please send it to the development team for double-check.";
        out.println(s);
        
        logger.info("Uploaded doctype checked OK!");
        return true;
    }
    
    public void addElement(Node node, DocTypeNode currentDocTypeNode, DocTypeNode allDoctypesNode) {
    	
    	if ( node.getNodeType() == Node.ELEMENT_NODE ) {

    		Element element = (Element) node;
    		
    		if (element.hasChildNodes()) {
		    	// parcurgere copii
		    	NodeList nodeList = element.getChildNodes();
		    	for (int i = 0; i < nodeList.getLength(); i++) {
		    		
		    		Node child = nodeList.item(i);
		    		
		    		if ( child.getNodeType() == Node.TEXT_NODE ) {
	
		    			// add docType
		    			currentDocTypeNode.put(child.getNodeValue().trim().replaceAll("\\s{2,}", " ").toUpperCase());
		    			currentDocTypeNode.put(child.getNodeValue().trim().replaceAll("\\s+", "").toUpperCase());
		    			
		    			// add docType to parent.all (to improve search)
		    			DocTypeNode all = (DocTypeNode) currentDocTypeNode.parent.get("ALL");
		    			if ( all == null ) {
		    				all = new DocTypeNode("ALL", currentDocTypeNode.parent, "ALL");
		    				currentDocTypeNode.parent.put("ALL", all);
		    			}
		    			all.put(child.getNodeValue().trim().replaceAll("\\s{2,}", " ").toUpperCase());
		    			all.put(child.getNodeValue().trim().replaceAll("\\s+", "").toUpperCase());
		        		
		    		} else if ( child.getNodeType() == Node.ELEMENT_NODE ) {
		    			
		    			String name = element.getAttribute("NAME");
		    			
		    			if ("".equals(name)) {
		    				addElement( child, currentDocTypeNode, allDoctypesNode );
		    				continue;
		    			}
		    				
		    			// create node
	    				DocTypeNode childDocTypeNode = (DocTypeNode) currentDocTypeNode.get(name);
	    				if (childDocTypeNode == null) {
	    					childDocTypeNode = new DocTypeNode(name, currentDocTypeNode, element.getNodeName());
	    					currentDocTypeNode.put(name, childDocTypeNode);
	    				}
	    				
	    				childDocTypeNode.addBpCodes(element.getAttribute("BPCODE"));
	    				String copyOnMerge = element.getAttribute("COPY_ON_MERGE");
	    				if(org.apache.commons.lang.StringUtils.isNotBlank(copyOnMerge)) {
	    					if( ("1".equals(copyOnMerge) || "true".equalsIgnoreCase(copyOnMerge))) {
	    						childDocTypeNode.setCopyOnMerge(1);
	    					} else {
	    						childDocTypeNode.setCopyOnMerge(0);
	    					}
	    				}
		    			
		    			// add base docTypes
		    			String extend = element.getAttribute("EXTENDS");
		    			if ( !"".equals(extend) ) {

		        			String category = extend.replaceAll("(.*?):(.*)", "$2");
		        			DocTypeNode baseDocTypeNode = (DocTypeNode) allDoctypesNode.get(category);
		        			
		        			if (baseDocTypeNode != null) {
		        				copyElements(childDocTypeNode, baseDocTypeNode);
		        				childDocTypeNode.addBpCodes(baseDocTypeNode.getBpcodes());
		        				if(childDocTypeNode.getCopyOnMerge() == -1) {
		        					childDocTypeNode.setCopyOnMerge(baseDocTypeNode.getCopyOnMerge());
		        				}
		        			}
		    			}
		    			
		    			// add subItems
		    			addElement( child, childDocTypeNode, allDoctypesNode );
		    			
		    			// copy all docTypes from child node
		    			if (childDocTypeNode != null) {
		    				
			    			DocTypeNode allCurrent = (DocTypeNode) currentDocTypeNode.get("ALL");
			    			DocTypeNode allChild = (DocTypeNode) childDocTypeNode.get("ALL");
			    			
			    			if ( allChild != null ) {
			    				
			    				if (allCurrent == null) {
			    					allCurrent = new DocTypeNode("ALL", currentDocTypeNode, "ALL");
				    				currentDocTypeNode.put("ALL", allCurrent); 
			    				}
			    					
			    				allCurrent.putAll(allChild.getAll());
			    			}
		    			}
		    			
		    		} else {
		    			String ignoredText = child.toString();
		    			if(!ignoredText.contains("#comment:")) {
		    				System.err.println("Ignored node " + child.toString());
		    			}
		    		}
		    	}
		    	
    		} else {

    			// add extends
    			String extend = element.getAttribute("EXTENDS");
    			String name = element.getAttribute("NAME");
    			
    			if ( !"".equals(extend) ) {

        			String category = extend.replaceAll("(.*?):(.*)", "$2");
        			DocTypeNode baseDocTypeNode = (DocTypeNode) allDoctypesNode.get(category);
        			
        			if (baseDocTypeNode != null) {
        				
        				DocTypeNode childDocTypeNode = (DocTypeNode) currentDocTypeNode.get(name);
	    				
	    				if (childDocTypeNode == null) {

	    					childDocTypeNode = new DocTypeNode(name, currentDocTypeNode, element.getNodeName());
	    					
	    					currentDocTypeNode.put(name, childDocTypeNode);
	    				}
	    				childDocTypeNode.addBpCodes(element.getAttribute("BPCODE"));
	    				String copyOnMerge = element.getAttribute("COPY_ON_MERGE");
	    				if(org.apache.commons.lang.StringUtils.isNotBlank(copyOnMerge)) {
	    					if(	("1".equals(copyOnMerge) || "true".equalsIgnoreCase(copyOnMerge))) {
	    						childDocTypeNode.setCopyOnMerge(1);
	    					} else {
	    						childDocTypeNode.setCopyOnMerge(0);
	    					}
	    				}
        				copyElements(childDocTypeNode, baseDocTypeNode);
        				childDocTypeNode.addBpCodes(baseDocTypeNode.getBpcodes());
        				if(childDocTypeNode.getCopyOnMerge() == -1) {
        					childDocTypeNode.setCopyOnMerge(baseDocTypeNode.getCopyOnMerge());
        				}
        			}
    			}
    			
    		}
    	}
    }
    
    public void copyElements(DocTypeNode currentDocTypeNode, DocTypeNode baseDocTypeNode) {
    	
    	Enumeration e = baseDocTypeNode.keys();
    	
    	while (e.hasMoreElements()) {
    		
    		String key = (String) e.nextElement();
    		
    		Object value = baseDocTypeNode.get(key);
    		
    		if ( value instanceof DocTypeNode ) {
    			
    			DocTypeNode childDocTypeNode = (DocTypeNode) currentDocTypeNode.get(key);
    			
    			if ( childDocTypeNode == null ) {
    				childDocTypeNode = new DocTypeNode( (DocTypeNode) value);
    				childDocTypeNode.setParent(currentDocTypeNode);
    				currentDocTypeNode.put(key, childDocTypeNode);
    			}
    			
    			copyElements(childDocTypeNode, (DocTypeNode) value );
    		
    		} else {
    			
    			currentDocTypeNode.put(key);
    		}
    		
    	}
    }
    
    

    public static void main (String args[]) throws Exception {
    	testReadDoctype("D:\\workspace2\\TS_main\\web");
//        try {
//            DocTypeReader dtr=new DocTypeReader("D:\\workspace2\\TS_main\\web");
////            Document d=dtr.readFile(ServerConfig.getDoctypeFilePath());
////            
//            //Node root = d.getDocumentElement();
//        	//dtr.addElement((Element) root, DocTypeNode.allDocTypes);
////        	
////            dtr.read(d);
////            dtr.fill();\
//        	
//        	Document d = dtr.readGeneratedFile(ServerConfig.getDoctypeFilePath());
//        	
//        	System.out.println("It seems to be ok, cross your fingers.");
//        	
//        } catch (Exception e) {
//            e.printStackTrace();
//            
//            System.err.println("BAD user!!!");
//        }
//        
//        printDocTypes(DocumentTypes.docTypes);
        
//        logger.info(DocumentTypes.checkDocumentType("DB+Q C", DocumentTypes.TRANSFER_INT));
//        logger.info(((Map)DocumentTypes.docTypes.get(new Integer(11790))).get("DB+Q C"));
//    	splitDocType();
    }
    
    private static void testReadDoctype(String location) {
    	try {
            DocTypeReader dtr=new DocTypeReader(location);
        	
        	Document d;
			try {
				d = dtr.readGeneratedFile(ServerConfig.getDoctypeFilePath(), false);
			} catch (Exception e) {
				d = dtr.readGeneratedFile(ServerConfig.getDoctypeFilePath(), true);
			}
        	
        	Node root = d.getDocumentElement();
        	
        	DocTypeNode allDocTypesTemp = new DocTypeNode("", null, "");
            
            dtr.addElement((Element) root, allDocTypesTemp, allDocTypesTemp);
        	
            System.out.println("It seems to be ok, cross your fingers.");
            
        } catch (Exception e) {
            e.printStackTrace();
            
            System.err.println("BAD user!!!");
        }
    	
    	
        
    	
    }
    
    protected static void printDocTypes(Map<Integer, Map<String, Integer>> m) throws Exception {
        Iterator<Integer> it=m.keySet().iterator();
        while (it.hasNext()) {
            Integer countyId=(Integer)it.next();
            County c=County.getCounty(new BigDecimal(countyId.toString()));
            //logger.info("---------------  County : "+c.getName()+"-------------------------------");
            Map<String, Integer> m2=m.get(countyId);
            Iterator<String> it2=m2.keySet().iterator();
            while (it2.hasNext()) {
                String doctype=it2.next();
                System.out.println("County "+c.getName()+", id="+countyId+"  :  "+doctype+" -> "+DocumentTypes.CATEGORY_NAME[((Integer)m2.get(doctype)).intValue()]);
            }
        }
    }

	public Document readGeneratedFile(String dir, boolean debug) throws Exception {
		try {
			String path = "";

			if (StringUtils.isEmpty(context)) {
				path = DocTypeReader.class.getClassLoader().getResource("").getPath() + "dt";
			} else {
				path = context + dir;
			}

			generateDocTypeFile(path);

			return XMLUtils.read(new File(path + File.separator + "generated_doctype.xml"), path, debug);
		} catch (SAXParseException e) {
			e.printStackTrace();
			throw new Exception("line:" + e.getLineNumber() + ", col:" + e.getColumnNumber() +
					", exception:" + e.getException(), e);
		}
	}
	
	public void generateDocTypeFile(String dir) {
		try {
			File fStart = null;
			File fDefault = null;
			File fEnd = null;

			File fDir = new File(dir);

			// look for files
			File[] files = fDir.listFiles(new StartsWithFileFilter("doctypeSTART"));

			if (files.length == 0) {
				logger.error("Could not find doctypeSTART.xml in " + dir + "!!!");
				throw new FileNotFoundException("Could not find doctypeSTART.xml in " + dir + "!!!");
			}

			fStart = files[0];

			files = fDir.listFiles(new StartsWithFileFilter("doctypeDEFAULT"));

			if (files.length == 0) {
				logger.error("Could not find doctypeDEFAULT.xml in " + dir + "!!!");
				throw new FileNotFoundException("Could not find doctypeDEFAULT.xml in " + dir + "!!!");
			}

			fDefault = files[0];

			files = fDir.listFiles(new StartsWithFileFilter("doctypeEND"));

			if (files.length == 0) {
				logger.error("Could not find doctypeEND.xml in " + dir + "!!!");
				throw new FileNotFoundException("Could not find doctypeEND.xml in " + dir + "!!!");
			}

			fEnd = files[0];

			files = fDir.listFiles(new StartsWithFileFilter("doctypeFor"));

			// if(files.length == 0){
			// logger.error("Could not find doctype for any state in " + dir + "!!!");
			// throw new FileNotFoundException("Could not find doctype for any state in " + dir + "!!!");
			// }

			List<File> fileList = new ArrayList<File>(Arrays.asList(files));

			fileList.add(0, fStart);
			fileList.add(1, fDefault);
			fileList.add(fEnd);

			File genetatedDoctype = new File(dir + File.separator + "generated_doctype.xml");

			if(genetatedDoctype.exists()){
				genetatedDoctype.delete();
			}
			
			genetatedDoctype.createNewFile();
			
			// generate doctype
			for (File f : fileList) {
				FileUtils.writeLines(genetatedDoctype, FileUtils.readLines(f), true);
			}

			logger.info("Doctype generated successfully in " + dir + "!!!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static File generateDocTypeCheckFile(String dir, File fileToCheck) {
		try {
			File fStart = null;
			File fDefault = null;
			File fEnd = null;

			File fDir = new File(dir);

			// look for files
			File[] files = fDir.listFiles(new StartsWithFileFilter("doctypeSTART"));

			if (files.length == 0) {
				logger.error("Could not find doctypeSTART.xml in " + dir + "!!!");
				throw new FileNotFoundException("Could not find doctypeSTART.xml in " + dir + "!!!");
			}

			fStart = files[0];

			files = fDir.listFiles(new StartsWithFileFilter("doctypeDEFAULT"));

			if (files.length == 0) {
				logger.error("Could not find doctypeDEFAULT.xml in " + dir + "!!!");
				throw new FileNotFoundException("Could not find doctypeDEFAULT.xml in " + dir + "!!!");
			}

			fDefault = files[0];

			files = fDir.listFiles(new StartsWithFileFilter("doctypeEND"));

			if (files.length == 0) {
				logger.error("Could not find doctypeEND.xml in " + dir + "!!!");
				throw new FileNotFoundException("Could not find doctypeEND.xml in " + dir + "!!!");
			}

			fEnd = files[0];

			List<File> fileList = new ArrayList<File>();

			fileList.add(fStart);
			if(!"doctypeDEFAULT.xml".equals(fileToCheck.getName())) {
				fileList.add(fDefault);	
			}
			fileList.add(fileToCheck);
			fileList.add(fEnd);

			File genetatedCheckDoctype = new File(dir + File.separator + "generated_checkfile.xml");

			if(genetatedCheckDoctype.exists()){
				genetatedCheckDoctype.delete();
			}
			
			genetatedCheckDoctype.createNewFile();
			
			// generate doctype
			for (File f : fileList) {
				FileUtils.writeLines(genetatedCheckDoctype, FileUtils.readLines(f), true);
			}

			logger.info("Check Doctype generated successfully in " + dir + "!!!");
			
			return genetatedCheckDoctype;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * splits doctype.xml form str by states only for local use
	 */
	private static void splitDocType(){
		try {
			String docName = "doctype.xml";
			String path = "D:\\Serv\\workspace\\TSEARCH\\src\\dt" + File.separator;

			File f = new File(path + docName);

			List<String> lines = IOUtils.readLines(new FileInputStream(f));

			StringBuffer buf = new StringBuffer();

			String state = "";
			
			boolean flag = false;
			
			for (String line : lines) {
				buf.append(line+"\n");
				if (line.contains("<TYPEDEF>")) {
					File start = new File(path + "doctypeSTART.xml");
					IOUtils.write(buf.toString(), new FileOutputStream(start));
					buf = new StringBuffer();
				} else if (line.contains("</TYPEDEF>")) {
					File end = new File(path + "doctypeEND.xml");
					IOUtils.write(buf.toString(), new FileOutputStream(end));
					buf = new StringBuffer();
				} else if (line.contains("</DEFAULT>")) {
					File start = new File(path + "doctypeDEFAULT.xml");
					IOUtils.write(buf.toString(), new FileOutputStream(start));
					buf = new StringBuffer();
				} else if (line.trim().matches("(?ism)<STATE NAME=\"\\w+\">")) {
					state = line.replaceAll("<STATE NAME=\"(\\w+)\">", "$1").trim();
				} else if (line.contains("</STATE>")) {
					flag = true;
					continue;
				} else if(flag){
					File start = new File(path + "doctypeFor"+state+".xml");
					IOUtils.write(buf.toString(), new FileOutputStream(start));
					buf = new StringBuffer();
					flag = false;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
}
