package ro.cst.tsearch.titledocument.abstracts;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateContants;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;

import com.gwt.utils.client.UtilsAtsGwt;

public class DocumentTypes {

	public static int ASSESSOR_TAX_SUBTYPE		   = 0;
    public static int PLAT_INT                     = 1;
    public static int TRANSFER_INT                 = 2;
    public static int MORTGAGE_INT                 = 3;
    public static int RELEASE_INT                  = 4;
    public static int ASSIGNMENT_INT               = 5;
    public static int EASEMENT_INT                 = 6;
    public static int RESTRICTION_INT              = 7;
    public static int LIEN_INT                     = 8;
    public static int COMPLAINT_INT				   = 9;
	public static int COURT_INT					   = 10;
    public static int MISCELANOUS_INT 			   = 11;
    public static int TSR_TYPE                     = 12;
    public static int CORPORATION_INT        	   = 13;
	public static int AFFIDAVIT_INT          	   = 14;
	public static int APPOINTMENT_INT        	   = 15;
	public static int CITY_TAX_SUBTYPE             = 16;
	public static int COUNTY_TAX_SUBTYPE           = 17;
	public static int MASTERDEED_SUBTYPE		   = 18;
	public final static int TAX_TYPE                          = 19;
    public final static int RECORDED_TITLE_DOCS_TYPES_GROUP   = 20;
    public final static int UNSATISFIED_DEEDS_TYPES_GROUP     = 21;
    public final static int CONVENANTS_TYPES_GROUP             = 22;
    public final static int ADDITIONAL_MATTERS_TYPES_GROUP    = 23;
    public final static int OTHERFILES_INT    = 24;
    public final static int RE_RECORDEDMORTGAGE_INT    = 25;
    public final static int ASSIGNMENTOFRENTSANDORLEASES_INT    = 26;
    public final static int RE_RECORDEDASSIGNMENT_INT    = 27;
    public final static int LEASE_INT    = 28;
    public final static int CCER_INT    = 29;
			
    //category
	public final static String								ASSESSOR							= "ASSESSOR";
	public final static String								RELEASE								= "RELEASE";
	public final static String								AFFIDAVIT							= "AFFIDAVIT";
	public final static String								MISCELLANEOUS						= "MISCELLANEOUS";
	public final static String								APPOINTMENT							= "APPOINTMENT";
	public final static String								ASSIGNMENT							= "ASSIGNMENT";
	public final static String								COMPLAINT							= "COMPLAINT";
	public final static String								MASTERDEED							= "MASTERDEED";
	public final static String								PLAT								= "PLAT";
	public final static String								TRANSFER							= "TRANSFER";
	public final static String								EASEMENT							= "EASEMENT";
	public final static String								RESTRICTION							= "RESTRICTION";
	public final static String								LIEN								= "LIEN";
	public final static String								MORTGAGE							= "MORTGAGE";
	public final static String								COURT								= "COURT";
	public final static String								CONDOMINIUM							= "CONDOMINIUM";
	public final static String								SUBSTITUTION						= "SUBSTITUTION";
	public final static String								RQNOTICE							= "RQNOTICE";
	public final static String								ASSUMPTION							= "ASSUMPTION";
	public final static String								CITYTAX								= "CITYTAX";
	public final static String								COUNTYTAX							= "COUNTYTAX";
	public final static String								OTHERFILES							= "OTHER-FILE";								// ex PRIORFILES
	
//	public final static String SFPRIORFILES = "SFPRIORFILES";
	public final static String PRIORFILE = "PRIORFILE";
	public final static String PATRIOTS = "PATRIOTS";
	public final static String LEASE = "LEASE";
	public final static String CCER = "CCER";
	public final static String BY_LAWS = "BY-LAWS";
	
	public final static String HOA = "HOA";
	public final static String WHITEPAGES = "WHITEPAGES";
	public final static String DRIVERDATA = "DRIVERDATA";
	public final static String LEXISNEXIS = "LEXISNEXIS";
	public final static String STATEBAR = "STATEBAR";
	
	//subcategory
	public final static String RE_REC_TRANSFER = "RE-RECORDED TRANSFER";
	public final static String WARRANTY_DEED = "Warranty Deed";
	public final static String DEED = "DEED";
	public final static String VENDORS_LIEN = "Vendors Lien";
	
	public static String SUBORDINATION  =  	"SUBORDINATION";
	public static String MODIFICATION   =  	"MODIFICATION";
	public static String CORPORATION 	=	"CORPORATION";
	public static String REQUEST_OF_NOTICE_OF_SALE   =  "REQUEST OF NOTICE OF SALE";
	public static final String SUBCATEGORY_FEDERAL_TAX_LIEN = "Federal Tax Lien";
	public static final String SUBCATEGORY_CHILD_SUPPORT_LIEN = "Child Support Lien";
	
	public final static String OTHER_FILE_EXC = "Other Exc";
	public final static String OTHER_FILE_REQ = "Other Req";
	public final static String OTHER_FILE_ESTATE = "Estate";
	public final static String OTHER_FILE_COMMENT = "Comment";
	public final static String OTHER_FILE_RESULTSHEET = "ResultSheet";
	
	public final static Set<String> OTHER_FILE_SPECIAL_SUBCATEGORIES = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
	
	public final static String PRIORFILE_PRIOR_FILE = "Prior File";
	public final static String PRIORFILE_BASE_FILE = "Base File";
	public final static String PRIORFILE_PRIOR_SEARCH = "Prior Search";
	public final static String PRIORFILE_BASE_SEARCH = "Base Search";
	
	protected static final Logger logger= Logger.getLogger(DocumentTypes.class);

    public final static String DAVIDSON_COUNTY          = "019";
    public final static String SHELBY_COUNTY            = "079";
    
    
    public final static Map<Integer, Map<String, Integer>> docTypes = new HashMap<Integer, Map<String, Integer>>(); 


	protected static final String[] CATEGORY_NAME=
	{
		"ASSESSOR",
		"PLAT",
		"TRANSFER",
		"MORTGAGE",
		"RELEASE",
		"ASSIGNMENT",
		"EASEMENT",
		"RESTRICTION",
		"LIEN",
		"COMPLAINT",
		"COURT",
		"MISCELLANEOUS",
		"TSR",
		"CORPORATION",
		"AFFIDAVIT",
		"APPOINTMENT",
		"CITYTAX",
		"COUNTYTAX",
		"MASTERDEED",
		"PARCELMAP",
		"RECORDMAP",
		"TRACTMAP",
		"PATRIOTS",
        "MODIFICATION",
		"SUBORDINATION",
		"SUBSTITUTION",
		"ASSUMPTION",
		"RQNOTICE",
		"BY-LAWS",
		"MASTERDEEDANDBY-LAWS",
		OTHERFILES,
		PRIORFILE,
		"LEASE",
		"CCER",
		"HOA",
		"WHITEPAGES",
		"DRIVERDATA",
		"LEXISNEXIS",
		"STATEBAR"
	};
	
	public static final String[] REGISTER_CATEGORY_NAME=
	{
		"PLAT",
		"TRANSFER",
		"MORTGAGE",
		"RELEASE",
		"ASSIGNMENT",
		"EASEMENT",
		"RESTRICTION",
		"LIEN",
		"COMPLAINT",
		"COURT",
		"MISCELLANEOUS",
		"CORPORATION",
		"AFFIDAVIT",
		"APPOINTMENT",
		"MASTERDEED",
		"PARCELMAP",
		"RECORDMAP",
		"TRACTMAP",
		"PATRIOTS",
		"MODIFICATION",
		"SUBORDINATION",
		"SUBSTITUTION",
		"ASSUMPTION",
		"RQNOTICE",
		"BY-LAWS",
		"MASTERDEEDANDBY-LAWS",
		"OTHER-FILE",
		PRIORFILE,
		"LEASE",
		"CCER",
		"HOA",
		"WHITEPAGES",
		"DRIVERDATA",
		"LEXISNEXIS",
		"STATEBAR"
	};
	
    public static int getCategoryID(String name) throws Exception {
        name=name.toUpperCase();
        for (int i=0; i<CATEGORY_NAME.length; i++)
            if (CATEGORY_NAME[i].equalsIgnoreCase(name))
                return i;
        throw new Exception("Category \""+name+"\" not found");
    }
	
    
    public static void loadDocType(){
    	 try {
             logger.debug( "Reading document types from file" );
             
             DocTypeReader dtr = new DocTypeReader(BaseServlet.REAL_PATH);
             Document d = dtr.readGeneratedFile(ServerConfig.getDoctypeFilePath(), false);
             Node root = d.getDocumentElement();
             
             DocTypeNode.allDocTypesTemp = new DocTypeNode("", null, "");
             
             dtr.addElement((Element) root, DocTypeNode.allDocTypesTemp, DocTypeNode.allDocTypesTemp);
             
             HashMap<String, Vector<String>> newCategories = new HashMap<String, Vector<String>>();
             
             NodeList children = root.getFirstChild().getChildNodes();
             for (int i = 0; i < children.getLength(); i++) {
 				Node child = children.item(i);
 				if(child.getNodeType()==Node.ELEMENT_NODE){
 					String name = ((Element)child).getAttribute("NAME");
 					Vector<String> subcategs = new Vector<String>();
 					NodeList subcategoriesNodeList = child.getChildNodes();
 					for (int j = 0; j < subcategoriesNodeList.getLength(); j++) {
 						Node subcategory = subcategoriesNodeList.item(j);
 						if(subcategory.getNodeType()==Node.ELEMENT_NODE){
 							subcategs.add(((Element)subcategory).getAttribute("NAME"));
 						}
 					}
 					newCategories.put(name, subcategs);
 				}
 			 }
             
             logger.debug( "Document types read from file" );
             
             logger.debug( ">>> Set new document types" );
             DocTypeNode.categories = new HashMap<String, Vector<String>>(newCategories);
             DocTypeNode.allDocTypes = DocTypeNode.allDocTypesTemp;
             DBManager.cleanDocTypeMap();
		     DocTypeNode.cachedDocumentTypeForWS = null;
             logger.debug( "Loaded new document types <<<" );
             
         } catch (Exception e) {
             logger.error(e);
             Log.sendEmail(e);
             
             e.printStackTrace();
         }
    }
    
    static {
    	
    	OTHER_FILE_SPECIAL_SUBCATEGORIES.add(OTHER_FILE_EXC);
    	OTHER_FILE_SPECIAL_SUBCATEGORIES.add(OTHER_FILE_REQ);
    	OTHER_FILE_SPECIAL_SUBCATEGORIES.add(OTHER_FILE_COMMENT);
    	OTHER_FILE_SPECIAL_SUBCATEGORIES.add(OTHER_FILE_ESTATE);
    	
       loadDocType();
    }

    public static Set getAllTypes(long searchId) {
        return getAllTypes(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getCountyId().intValue());
    }
    
    public static Set getAllTypes(int countyID) {
        
    	try {
    		
            /*Integer countyId=new Integer(countyID);
            Map countyDocType = (Map)docTypes.get(countyId);
            if(countyDocType==null)								
                countyDocType = (Map)docTypes.get(new Integer(0));  // default DocTypes;
            return countyDocType.keySet();*/
    		
    		DocTypeNode county = DocTypeNode.getCounty(County.getCounty(countyID));

    		DocTypeNode allDocTypes = (DocTypeNode) county.get("ALL");
    		
    		return allDocTypes.keySet();
            
        } catch (Exception e) {
            return new HashSet();
        }
    }

    private static Pattern plus=Pattern.compile(".*\\+(.*)");
    
    private static Map<String, String[]> doctypes = new HashMap<String, String[]>();
    public static String removeAllDocTypes(String s,long searchId) {
        //logger.debug( "remove all document types s = " + s );
    	String county = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv() + 
    		InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName(); 
    	String [] array = null;
    	synchronized(doctypes){
    		if (doctypes != null){
    			array = doctypes.get(county);
    		}
    	}
    	if (array == null){
	        Set init=getAllTypes(searchId);
	        Set li=new TreeSet(new Comparator(){
	                   public int compare(Object o1, Object o2) {
	                       return -((String)o1).compareToIgnoreCase((String)o2);
	                   }
	                });
	        Iterator it=init.iterator();
	        while (it.hasNext()) {
	            String type=(String)it.next();
	            Matcher ma=plus.matcher(type);
	            if (ma.find())
	                type=ma.group(1);
	            li.add(type);
	        }
	        init=null;
	        it=li.iterator();
	        array = (String[])li.toArray(new String[0]);
	        // sort the doctype array in descending order of the doctype length so we will remove the longest doctype first
	        // otherwise, if both "LTD POWER OF ATTY" and "POWER OF ATTY" are valid doctypes and legal="ExtDesc: POWER OF ATTY", 
	        //then first "POWER OF ATTY" is removed and "LTD" never get to be removed  
	        Arrays.sort(array, new Comparator<String>(){
	        	public int compare(String s1, String s2){
	        		if(s1.length() != s2.length()){
	        			return s2.length() - s1.length();
	        		} else {
	        			return s2.compareTo(s1);
	        		}
	        	}
	        });
	        
	        synchronized(doctypes){
	        	doctypes.put(county, array);
	        }
    	}
    	
        for (int i=0; i<array.length; i++){
            s=s.replaceAll("(?i)(^|[ .])"+Pattern.quote(array[i])+"($|[ .])", " ").replaceAll("\\s{2,}", " ").trim();
        }

        //logger.debug( "removed all document types s = " + s );
        return s.replaceAll("\\s+", " ").trim();
    }

    public static void main (String args[]) {
        //getAllCategoriesAndSubcategories(new String[]{"TN","FL"});
    	File folder = new File("D:\\workspace2\\TS_main\\src\\dt");
    	
    	String[] keepTitleCase = new String[] {" Of "," Or "," To "," For "," By "," And "," In "," From "};
    	String[] newTitleCase = new String[] {" of "," or "," to "," for "," by "," and "," in "," from "};
    	Pattern subcatPattern = Pattern.compile("(?is)(<\\s*SUBCATEGORY[^>]*NAME\\s*=\\s*\")([^\"]+)(\"[^>]*>)");
    	
		for(File file: folder.listFiles()) {
			if(file.isFile() && file.getPath().endsWith("xml")) {
				try {
					String fileContent = FileUtils.readFileToString(file);
					
					Matcher matcher = subcatPattern.matcher(fileContent);
					
					StringBuffer result = new StringBuffer();
					while (matcher.find()) {
						
						String origSubcateg = matcher.group(2).replaceAll("\\\\", "\\\\\\\\").replaceAll("\\$", "\\\\\\$");
						
						String subcategory = UtilsAtsGwt.toTitleCase(origSubcateg);
						
						//stupid temporary fix...
			    		// it would be best to take the subcategories in the corect format directly from doctype.xml
			    		if(subcategory.equalsIgnoreCase("Miami-dade Disclosure Report")) {
			    			subcategory = "Miami-Dade Disclosure Report";
			    		} else if(subcategory.equalsIgnoreCase("All Matters Including But Not Limited To An Easement")){
			    			subcategory = "All matters including but not limited to an Easement";
			    		} else if(subcategory.equalsIgnoreCase("All Matters Set Out In Document Including But Not Limited To An Easement")){
			    			subcategory = "All matters set out in document including but not limited to an Easement";
			    		} else if(subcategory.equalsIgnoreCase("Items Possibly Extinguished By Foreclosure")){
			    			subcategory = "Items possibly extinguished by foreclosure";
			    		} else if(subcategory.equalsIgnoreCase("All Matters Set Out In Document Including But Not Limited To Restrictions")){
			    			subcategory = "All matters set out in document including but not limited to restrictions";
			    		} else if(subcategory.equalsIgnoreCase("CC&Rs in a Deed")){
			    			subcategory = "CC&Rs in a Deed";
			    		} else if(subcategory.equalsIgnoreCase("Modification of Cc&rs")){
			    			subcategory = "Modification of CC&Rs";
			    		} else if(subcategory.equalsIgnoreCase("Ammendment of Cc&rs")){
			    			subcategory = "Ammendment of CC&Rs";
			    		} else if(subcategory.equalsIgnoreCase("ModUCC")){
			    			subcategory = "ModUCC";
			    		} else if(subcategory.equalsIgnoreCase("Platte-Clay Electric")){
			    			subcategory = "Platte-Clay Electric";
			    		} else if(subcategory.equalsIgnoreCase("MO Public Svs")){
			    			subcategory = "MO Public Svs";
			    		} else if(subcategory.equalsIgnoreCase("Foreclosing Mortgage (KS)")){
			    			subcategory = "Foreclosing Mortgage (KS)";
			    		} else if(subcategory.equalsIgnoreCase("Foreclosed Mortgage (KS)")){
			    			subcategory = "Foreclosed Mortgage (KS)";
			    		} else if(subcategory.equalsIgnoreCase("O&G Lease")){
			    			subcategory = "O&G Lease";
			    		} else if(subcategory.equalsIgnoreCase("Declaration of Homes Association with Restrictions")){
			    			subcategory = "Declaration of Homes Association with Restrictions";
			    		} else if(subcategory.equalsIgnoreCase("Deed with Restrictions")){
			    			subcategory = "Deed with Restrictions";
			    			
			    		}
			    		
			    		for (int i = 0; i < keepTitleCase.length; i++) {
			    			if(subcategory.contains(keepTitleCase[i])) {
			    				subcategory = subcategory.replaceAll(keepTitleCase[i], newTitleCase[i]);
			    			}
						}
						
						matcher.appendReplacement(result, 
								matcher.group(1).replaceAll("\\\\", "\\\\\\\\").replaceAll("\\$", "\\\\\\$") + 
								subcategory + 
								matcher.group(3).replaceAll("\\\\", "\\\\\\\\").replaceAll("\\$", "\\\\\\$"));
					}
					matcher.appendTail(result);
					
					System.out.println("Ready file " + file.getAbsolutePath());
					FileUtils.writeStringToFile(file, result.toString());
					System.out.println("DONE     file " + file.getAbsolutePath());
					
				} catch (Exception e) {
					System.err.println("ERROR file " + file.getAbsolutePath());
					e.printStackTrace();
				}
				
				
				
			}
		}
    }
	
    public static boolean checkDocumentType (String docType, int docTypeToCheck, int docSubTypeToCheck,long searchId) {
        return checkDocumentType(docType, docSubTypeToCheck,null,searchId);
    }

    public static boolean checkDocumentTypeIsGenericPlat (String docType,long searchId) {
        return checkDocumentType(docType, CONVENANTS_TYPES_GROUP,null,searchId);
    }

	public static boolean checkDocumentType (String docType, int docTypeToCheck, Search global,long searchId) {
		
		if(global==null){
			global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		}
		
		boolean result = false;
	//	logger.info( "checking document type docType = " + docType + " docTypeToCheck =  " + docTypeToCheck );
		docType = docType.toUpperCase();
		switch (docTypeToCheck){
			case TAX_TYPE :
				result = checkDocumentType (docType, CITY_TAX_SUBTYPE,null,searchId) || 
						  checkDocumentType (docType, COUNTY_TAX_SUBTYPE,null,searchId) ||
						  checkDocumentType (docType, ASSESSOR_TAX_SUBTYPE,null,searchId);
				break;			
			case RECORDED_TITLE_DOCS_TYPES_GROUP :
				result = checkDocumentType (docType, TRANSFER_INT,null,searchId);
				break;
			case UNSATISFIED_DEEDS_TYPES_GROUP   :
				result = checkDocumentType (docType, MORTGAGE_INT,null,searchId) ||
						 checkDocumentType (docType, ASSIGNMENT_INT,null,searchId);
				break;
			case CONVENANTS_TYPES_GROUP          :
				result = checkDocumentType (docType, PLAT_INT,null,searchId)        ||
						 checkDocumentType (docType, RESTRICTION_INT,null,searchId) ||
						 checkDocumentType (docType, CCER_INT,null,searchId) ||
						 checkDocumentType (docType, EASEMENT_INT,null,searchId);
				break;
			case ADDITIONAL_MATTERS_TYPES_GROUP  :
				result = ( !checkDocumentType (docType, RECORDED_TITLE_DOCS_TYPES_GROUP,null,searchId) ) &&
						 ( !checkDocumentType (docType, UNSATISFIED_DEEDS_TYPES_GROUP,null,searchId)   ) &&
						 ( !checkDocumentType (docType, CONVENANTS_TYPES_GROUP,null,searchId)          );
				break;
			//type, not group
			default:
				
				
			
				// uneori parsarea "da gres" si baga mai multe spatii pentru docType-urile din mai multe cuvinte.
				docType = docType.replaceAll("\\s+", " ");
				County currentCounty =null;
				
				//apel facut din contextul BaseServlet
				if(global==null){
					currentCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty();
				}
				else{
					try{
						currentCounty  = County.getCounty((int)HashCountyToIndex.getCountyId(global.getP1()));	
					}
					catch(BaseException e){
						e.printStackTrace();
					}
					
				}
					
				
				DocTypeNode category = DocTypeNode.getCategory(currentCounty, docType);
				
				if (category != null && category.id == docTypeToCheck) {
		        	result = true;
		        	break;
				}
                else if( category == null )
                {
                	if( "ASSESSOR".equalsIgnoreCase( docType ) && docTypeToCheck == ASSESSOR_TAX_SUBTYPE )
                    {
                        result = true;
                        break;
                    } 
                    else if( "CNTYTAX".equalsIgnoreCase( docType ) && docTypeToCheck == COUNTY_TAX_SUBTYPE )
                    {
                        result = true;
                        break;
                    } 
                    else if ( "CITYTAX".equalsIgnoreCase( docType ) && docTypeToCheck == CITY_TAX_SUBTYPE) 
                    {
                    	result = true;
                    	break;
                    } 
                    else if ( docTypeToCheck == TAX_TYPE ) 
                    {
                    	result = true;
                    	break;
                    }
                    else if("MORTGAGE".equalsIgnoreCase(docType )&& docTypeToCheck == MORTGAGE_INT ){
                    	result = true;
                    	break;
                    }
                    else if(DocumentTypes.LIEN.equalsIgnoreCase(docType )&& docTypeToCheck == DocumentTypes.LIEN_INT){
                    	result = true;
                    	break;
                    }	
                    else if("COURT".equalsIgnoreCase(docType )&& docTypeToCheck == DocumentTypes.COURT_INT){
                    	result = true;
                    	break;
                    }	
                    else if("PLAT".equalsIgnoreCase(docType )&& docTypeToCheck == DocumentTypes.PLAT_INT){
                    	result = true;
                    	break;
                    }	
                    else if("TRANSFER".equalsIgnoreCase(docType )&& docTypeToCheck == DocumentTypes.TRANSFER_INT){
                    	result = true;
                    	break;
                    }	
                    else if("RELEASE".equalsIgnoreCase(docType )&& docTypeToCheck == DocumentTypes.RELEASE_INT){
                    	result = true;
                    	break;
                    }
                    else if("RESTRICTION".equalsIgnoreCase(docType )&& docTypeToCheck == DocumentTypes.RESTRICTION_INT){
                    	result = true;
                    	break;
                    }
                    else if("COMPLAINT".equalsIgnoreCase(docType )&& docTypeToCheck == DocumentTypes.COMPLAINT_INT){
                    	result = true;
                    	break;
                    }
                    else if("MISCELANOUS".equalsIgnoreCase(docType )&& docTypeToCheck == DocumentTypes.MISCELANOUS_INT){
                    	result = true;
                    	break;
                    }
                    else if("APPOINTMENT".equalsIgnoreCase(docType )&& docTypeToCheck == DocumentTypes.APPOINTMENT_INT){
                    	result = true;
                    	break;
                    }
                    else if ("MASTERDEED".equalsIgnoreCase(docType) && docTypeToCheck == DocumentTypes.MASTERDEED_SUBTYPE){
                    	result = true;
                    	break;
                    }
                    else if("AFFIDAVIT".equalsIgnoreCase(docType )&& docTypeToCheck == DocumentTypes.AFFIDAVIT_INT){
                    	result = true;
                    	break;
                    }
                    else if("EASEMENT".equalsIgnoreCase(docType )&& docTypeToCheck == DocumentTypes.EASEMENT_INT){
                    	result = true;
                    	break;
                    }
                    else if("ASSIGNMENT".equalsIgnoreCase(docType )&& docTypeToCheck == DocumentTypes.ASSIGNMENT_INT){
                    	result = true;
                    	break;
                    }
                    else if("LEASE".equalsIgnoreCase(docType )&& docTypeToCheck == DocumentTypes.LEASE_INT){
                    	result = true;
                    	break;
                    }
                    else if("CCER".equalsIgnoreCase(docType )&& docTypeToCheck == DocumentTypes.CCER_INT){
                    	result = true;
                    	break;
                    }
                }
			
			    /*Map countyDocType = null;
			    
				try {
				    Integer countyId=new Integer(InstanceManager.getCurrentInstance().getCurrentCounty().getCountyId().intValue());
					countyDocType = (Map)docTypes.get(countyId);
				} catch (Exception e) {
				    e.printStackTrace();
				}

				if(countyDocType==null)								
					countyDocType = (Map)docTypes.get(new Integer(0));  // default DocTypes;
				//logger.info("Document Type to check:..." + docType + "\n");
				if(countyDocType.get(docType)==null) {
					return false;
                }
				Integer categ = (Integer)countyDocType.get(docType);                
                if((categ != null) && (categ.intValue()==docTypeToCheck)) {
                    result = true;
					break;
				}*/
				
				
		}
		//logger.info("checkDocumentType("+docType+", "+docTypeToCheck+") = "+result);
		return result;
	}

    public static boolean isRegisterDocType(String docType,long searchId){
    	String category = getDocumentCategory(docType,searchId);
    	for(int i=0;i<REGISTER_CATEGORY_NAME.length;i++){
			if(REGISTER_CATEGORY_NAME[i].equalsIgnoreCase(category))
				return true;
    	}
    	return false;
    	
    }
    
    public static boolean isReleaseDocType(String docType,long searchId){
		return "RELEASE".equalsIgnoreCase(getDocumentCategory(docType,searchId));
    }

    public static boolean isLienDocType(String docType,long searchId){
		return "LIEN".equalsIgnoreCase(getDocumentCategory(docType,searchId));
    }
    
    public static boolean isMortgageDocType(String docType,long searchId){
		return "MORTGAGE".equalsIgnoreCase(getDocumentCategory(docType,searchId));
    }
    
    public static boolean isCourtDocType(String docType,long searchId){
		return "COURT".equalsIgnoreCase(getDocumentCategory(docType,searchId));
    }
    
    public static boolean isAssignDocType(String docType, long searchId) {
    	return "ASSIGNMENT".equalsIgnoreCase(getDocumentCategory(docType, searchId));
    }
    
    public static boolean isTransferDocType(String docType, long searchId) {
    	return "TRANSFER".equalsIgnoreCase(getDocumentCategory(docType, searchId));
    }
    
    public static boolean isPlatDocType(String docType, long searchId) {
    	return "PLAT".equalsIgnoreCase(getDocumentCategory(docType, searchId));
    }
    
    public static boolean isAppointDocType(String docType, long searchId) {
    	return "APPOINTMENT".equalsIgnoreCase(getDocumentCategory(docType, searchId));
    }
    
    public static boolean isEaseDocType(String docType, long searchId) {
    	return "EASEMENT".equalsIgnoreCase(getDocumentCategory(docType, searchId));
    }
    
    public static boolean isModificationDocType(String docType, long searchId) {
    	return "MODIFICATION".equalsIgnoreCase(getDocumentCategory(docType, searchId));
    }
    
    
        
    /*public static boolean isJudgementDocType(String docType){
    	return ("JUDGMENT".equalsIgnoreCase(docType) || "JU - Judgment".equalsIgnoreCase(docType));
    }*/
    
    public static boolean isJudgementDocType(String docType,long searchId, boolean isParentSite){
    	return "JUDGMENT".equalsIgnoreCase(DocumentTypes.getDocumentSubcategory(docType, null,searchId, isParentSite));
    }
    
    public static boolean isLisPendensDocType(String docType, long searchId, boolean isParentSite){
    	return "LIS PENDENS".equalsIgnoreCase(DocumentTypes.getDocumentSubcategory(docType, null, searchId, isParentSite));
    }
    
    public static boolean isFederalTaxLienDocType(String docType,long searchId, boolean isParentSite){
    	return "FEDERAL TAX LIEN".equalsIgnoreCase(DocumentTypes.getDocumentSubcategory(docType, null,searchId, isParentSite));
    }
    
    public static boolean isStateTaxLienDocType(String docType,long searchId, boolean isParentSite){
    	return "STATE TAX LIEN".equalsIgnoreCase(DocumentTypes.getDocumentSubcategory(docType, null,searchId, isParentSite));
    }

    public static boolean isCondominiumDocType(String docType,long searchId, boolean isParentSite){
    	return "CONDOMINIUM".equalsIgnoreCase(DocumentTypes.getDocumentSubcategory(docType, null,searchId, isParentSite));
    }
    
    public static boolean isMasterDeedDocType(String docType,long searchId){
    	return "MASTERDEED".equalsIgnoreCase(DocumentTypes.getDocumentCategory(docType,searchId));
    }
    
    public static boolean isMechanicLienDocType(String docType, long searchId, boolean isParentSite){
    	return "MECHANIC LIEN".equalsIgnoreCase(DocumentTypes.getDocumentSubcategory(docType, null,searchId, isParentSite));
    }
    
    public static boolean isNoticeOfIntentDocType(String docType, long searchId, boolean isParentSite){
    	return "NOTICE OF INTENT".equalsIgnoreCase(DocumentTypes.getDocumentSubcategory(docType, null,searchId, isParentSite));
    }
    
    public static boolean isTaxWarrantDocType(String docType, long searchId, boolean isParentSite){
    	return "TAX WARRANT".equalsIgnoreCase(DocumentTypes.getDocumentSubcategory(docType, null,searchId, isParentSite));
    }
    
    public static boolean isOfDocType(String docType, String docTypes[],long searchId){
    	if(docTypes == null) { return true; }
    	String category = getDocumentCategory(docType,searchId);
    	for(int i=0; i<docTypes.length; i++){
    		if(docTypes[i].equalsIgnoreCase(category)){
    			return true;
    		}
    	}
    	return false;
    }
    
    public static boolean isOfSubDocType(String subDocType, String subDocTypes[],long searchId){
    	if (subDocTypes == null) { return true; }
    	String subCategory = getDocumentSubcategory(subDocType, searchId);
    	for (int i = 0; i < subDocTypes.length; i++){
    		if (subDocTypes[i].equalsIgnoreCase(subCategory)){
    			return true;
    		}
    	}
    	return false;
    }

    public static String getDocumentCategory(String docType,long searchId){
    	if(docType==null){
    		return CATEGORY_NAME[MISCELANOUS_INT];
    	}
        County currentCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty();
        return getDocumentCategory(docType, currentCounty);
    }
    
    public static String getDocumentCategory(String docType, County county){
    	if(docType==null){
    		return CATEGORY_NAME[MISCELANOUS_INT];
    	}
    	docType = docType.replaceAll("\\s+", " ").toUpperCase();
        DocTypeNode category = DocTypeNode.getCategory(county, docType);
        if (category != null){
        	return CATEGORY_NAME[category.getId()];
        } else {
            for( int i = 0 ; i < CATEGORY_NAME.length ; i ++ )
            {
                if( CATEGORY_NAME[i].equalsIgnoreCase( docType ) )
                    return CATEGORY_NAME[i];
            }
            
            if( "CNTYTAX".equalsIgnoreCase( docType ) )
                return "COUNTYTAX";
        }
        return CATEGORY_NAME[MISCELANOUS_INT];
    }
    
    private static ArrayList<String> toTitleCase(List<String> all){
    	ArrayList<String> newList = new ArrayList<String>();
    	for(String s:all){
    		String tempString = UtilsAtsGwt.toTitleCase(s);
    		
    		newList.add(tempString);
    	}
    	return newList;
    }
    
    public static ArrayList<String> getSubcategoryForCategory(final String category, long searchId){
    	ArrayList<String> allSubs = DocTypeNode.getSubcategoryForCategory(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty(), category);
    	if(allSubs .size()==0){
    		allSubs.add(category);
    	}
    	
    	return allSubs;
    	
    	/*
    	
    	ArrayList<String> tempData = toTitleCase(allSubs);
    	ArrayList<String> result = new ArrayList<String>();
    	String[] keepTitleCase = new String[] {" Of "," Or "," To "," For "," By "," And "," In "," From "};
    	String[] newTitleCase = new String[] {" of "," or "," to "," for "," by "," and "," in "," from "};
    	for (String subcategory : tempData) {
    		//stupid temporary fix...
    		// it would be best to take the subcategories in the corect format directly from doctype.xml
    		if(subcategory.equalsIgnoreCase("Miami-dade Disclosure Report")) {
    			subcategory = "Miami-Dade Disclosure Report";
    		} else if(subcategory.equalsIgnoreCase("All Matters Including But Not Limited To An Easement")){
    			subcategory = "All matters including but not limited to an Easement";
    		} else if(subcategory.equalsIgnoreCase("All Matters Set Out In Document Including But Not Limited To An Easement")){
    			subcategory = "All matters set out in document including but not limited to an Easement";
    		} else if(subcategory.equalsIgnoreCase("Items Possibly Extinguished By Foreclosure")){
    			subcategory = "Items possibly extinguished by foreclosure";
    		} else if(subcategory.equalsIgnoreCase("All Matters Set Out In Document Including But Not Limited To Restrictions")){
    			subcategory = "All matters set out in document including but not limited to restrictions";
    		} else if(subcategory.equalsIgnoreCase("CC&Rs in a Deed")){
    			subcategory = "CC&Rs in a Deed";
    		} else if(subcategory.equalsIgnoreCase("Modification of Cc&rs")){
    			subcategory = "Modification of CC&Rs";
    		} else if(subcategory.equalsIgnoreCase("Ammendment of Cc&rs")){
    			subcategory = "Ammendment of CC&Rs";
    		} else if(subcategory.equalsIgnoreCase("ModUCC")){
    			subcategory = "ModUCC";
    		}
    		
    		for (int i = 0; i < keepTitleCase.length; i++) {
    			if(subcategory.contains(keepTitleCase[i])) {
    				subcategory = subcategory.replaceAll(keepTitleCase[i], newTitleCase[i]);
    			}
			}
    		
    		result.add(subcategory);
		}
    	return result;
    	*/
    }
    
    public static TreeMap<String, TreeMap<String, TreeMap<String, TreeSet<String>>>> getAllCategoriesAndSubcategoriesForStateAndCounty(String ... stateAbr){
    	
    	//TreeMap<State, TreeMap<County, TreeMap<Category, TreeSet<Subcategory>>>>
    	
    	TreeMap<String, TreeMap<String, TreeMap<String, TreeSet<String>>>> result = new TreeMap<String, TreeMap<String, TreeMap<String, TreeSet<String>>>>();
    	ArrayList<String> statesAbbrevList = new ArrayList<String>();
    	
    	if (stateAbr.length == 0){
    		Set<String> states = DocTypeNode.allDocTypes.getAll().keySet();
    		for (String state : states) {
    			if (state.length() == 2){
	    			for (StateContants.STATE_ABBREV value : StateContants.STATE_ABBREV.values()){
						if (value.toString().equals(state)){
							statesAbbrevList.add(state);
						}
	    			}
    			}
			}    		
    	} else{
    		for (String state : stateAbr){
    			statesAbbrevList.add(state);
			}
    	}
    	
    	if (statesAbbrevList.size() > 0){
    		for (String stateAbbrev : statesAbbrevList){
				
    			TreeMap<String, TreeMap<String, TreeSet<String>>> counties = new TreeMap<String, TreeMap<String, TreeSet<String>>>();
    			
    			//find state node
    			DocTypeNode stateNode = DocTypeNode.getDocTypeNode(stateAbbrev);
    			
    			if (stateNode != null){
    			
    				Hashtable allCounties = (Hashtable) stateNode.getAll();
    				//go through each county
    		        Enumeration e = allCounties.keys();
    		        while (e.hasMoreElements()){
    		        	
    		        	String countyName = (String) e.nextElement();

    		        	if ("ALL".equals(countyName)){
    		        		continue;
    		        	}
    		        	//find the county node
    		        	DocTypeNode county = (DocTypeNode) allCounties.get(countyName);
    		        	
    		        	Hashtable allCategories = (Hashtable)county.getAll();
    		        	
    		        	//we have allCategories
    		        	Enumeration eCategories = allCategories.keys();
    		        	
    		        	TreeMap<String, TreeSet<String>> categories = new TreeMap<String, TreeSet<String>>();
    		        	
    		        	while(eCategories.hasMoreElements()){
    		        		
    		        		String categoryName = (String)eCategories.nextElement();
    		        	
    		        		if(categoryName.equals("ALL")) {
    		        			continue;
    		        		}
    		        		
    		        		DocTypeNode category = null;
    		        		if (allCategories.get(categoryName) instanceof DocTypeNode){
    							category = (DocTypeNode) allCategories.get(categoryName);
    						} else{
    							System.out.println(allCategories.get(categoryName));
    						}
    			        	
    			        	Hashtable subcategories = (Hashtable)category.getAll();
    			        	Enumeration eSubcategs = subcategories.keys();
    			        	
    			        	while (eSubcategs.hasMoreElements()){
    			        		String keySubcateg = (String)eSubcategs.nextElement();
    			        		if (keySubcateg.equals("ALL"))
    			        			continue;
    			        		
    			        		TreeSet<String> subcategoriesValues = categories.get(categoryName);
    			        		if (subcategoriesValues == null){
    			        			subcategoriesValues = new TreeSet<String>();
    			        			categories.put(categoryName, subcategoriesValues);
    			        		}
    			        		
    		        			if (!subcategoriesValues.contains(keySubcateg)){
    			        			subcategoriesValues.add(keySubcateg);
    			        		}		        		
    			        	}
    		        	}
    		        	counties.put(countyName, categories);
    		        }
    			}
    			result.put(stateAbbrev, counties);
    		}
		}
    
    	return result;
    }
    /**
     * Return all categories and subcategories for the given state abbreviations
     * @param stateAbr
     * @return
     */
    public static TreeMap<String, TreeSet<String>> getAllCategoriesAndSubcategories(String ... stateAbr){
    	TreeMap<String, TreeSet<String>> result = new TreeMap<String, TreeSet<String>>();
    	
    	if(stateAbr.length > 0) {
    		Set<String> defaultCategories = DocTypeNode.categories.keySet();
    		for (String categoryName : defaultCategories) {
    			TreeSet<String> subcategoriesValues = result.get(categoryName);
        		if(subcategoriesValues == null) {
        			subcategoriesValues = new TreeSet<String>();
        			result.put(categoryName, subcategoriesValues);
        		}
        		
        		subcategoriesValues.addAll(DocTypeNode.categories.get(categoryName));
			}
    	}
    	
    	for (int i = 0; i < stateAbr.length; i++) {
			//find state node
			DocTypeNode stateNode = DocTypeNode.getDocTypeNode(stateAbr[i]);
			
			if(stateNode != null) {
			
				Hashtable allCounties = (Hashtable) stateNode.getAll();
				//go through each county
		        Enumeration e = allCounties.keys();
		        while ( e.hasMoreElements() ) {
		        	
		        	String countyName = (String) e.nextElement();
		        	
		        	
		        	
		        	if("ALL".equals(countyName)) {
		        		continue;
		        	}
		        	//find the county node
		        	DocTypeNode county = (DocTypeNode) allCounties.get(countyName);
		        	
		        	Hashtable allCategories = (Hashtable)county.getAll();
		        	
		        	//we have allCategories
		        	Enumeration eCategories = allCategories.keys();
		        	
		        	while(eCategories.hasMoreElements()) {
		        		String categoryName = (String)eCategories.nextElement();
		        	
		        		if(categoryName.equals("ALL")) {
		        			continue;
		        		}
		        		
		        		DocTypeNode category = null;
		        		if (allCategories.get(categoryName) instanceof DocTypeNode) {
							category = (DocTypeNode) allCategories.get(categoryName);
							
						} else {
							System.out.println(allCategories.get(categoryName));
						}
			        	
			        	Hashtable subcategories = (Hashtable)category.getAll();
			        	
			        	Enumeration eSubcategs = subcategories.keys();
			        	
			        	while(eSubcategs.hasMoreElements()){
			        		String keySubcateg = (String)eSubcategs.nextElement();
			        		if(keySubcateg.equals("ALL"))
			        			continue;
			        		TreeSet<String> subcategoriesValues = result.get(categoryName);
			        		if(subcategoriesValues == null) {
			        			subcategoriesValues = new TreeSet<String>();
			        			result.put(categoryName, subcategoriesValues);
			        		}
			        		
			        		
		        			if(!subcategoriesValues.contains(keySubcateg)) {
			        			subcategoriesValues.add(keySubcateg);
			        		}		        		
			        	}
		        	}
		        	
		        }
			}
			
			
		}
    	
    	return result;
    }
    
    public static String getDocumentSubcategory(String docType,long searchId){
    	
    	if(StringUtils.isEmpty(docType)){
    		return getDocumentCategory(docType, searchId);
    	}
    	
    	// uneori parsarea "da gres" si baga mai multe spatii pentru docType-urile din mai multe cuvinte.
    	docType = docType.replaceAll("\\s+", " ").toUpperCase();
        	
        County currentCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty();
        
        DocTypeNode subcategory = DocTypeNode.getSubcategory(currentCounty, docType);
        
        if (subcategory != null)
        	return subcategory.getName();
        
        return UtilsAtsGwt.toTitleCase(CATEGORY_NAME[MISCELANOUS_INT]);
    }
    
    /**
     * This get's the Category for the given doctype and county<br>
     * No search needed - you can cache the county
     * @param docType
     * @param county
     * @return
     */
    public static String getDocumentSubcategory(String docType, County county){
    	docType = docType.replaceAll("\\s+", " ").toUpperCase();
        DocTypeNode subcategory = DocTypeNode.getSubcategory(county, docType);
        if (subcategory != null)
        	return subcategory.getName();
        return UtilsAtsGwt.toTitleCase(CATEGORY_NAME[MISCELANOUS_INT]);
    }
    
    public static String getDocumentSubcategory(String docType,Search global,long searchId){
    	return getDocumentSubcategory(docType, global, searchId, false);
    }
    public static String getDocumentSubcategory(String docType,Search global,long searchId, boolean isParentSite){
    	if(global == null){
			global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
    	}
        County currentCounty = null;
        try{
        	String p1 = isParentSite?global.getP1ParentSite():global.getP1();
        	currentCounty  = County.getCounty((int)HashCountyToIndex.getCountyId(p1));;
        }
        catch(Exception e){
        	e.printStackTrace(System.err);
        }
        
        if(currentCounty == null){
        	currentCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty();
        }
        
        DocTypeNode subcategory = DocTypeNode.getSubcategory(currentCounty, docType);
        if (subcategory != null)
        	return subcategory.getName();
        
        return UtilsAtsGwt.toTitleCase(CATEGORY_NAME[MISCELANOUS_INT]);
    }
    
    public static String cleanStringFromCategories(String input){
    	for (int i = 0; i < CATEGORY_NAME.length; i++) {
			input = input.replaceAll(CATEGORY_NAME[i], "");
		}
    	return input;
    }
    
    public static String[] getAllAvailableCategories() {
    	return CATEGORY_NAME.clone();
    }
    
    public static boolean isMortgageRelated(String category) {
    	if(category.equalsIgnoreCase(DocumentTypes.ASSIGNMENT)
    			|| category.equalsIgnoreCase(DocumentTypes.APPOINTMENT)
    			|| category.equalsIgnoreCase(DocumentTypes.SUBORDINATION)
    			|| category.equalsIgnoreCase(DocumentTypes.MODIFICATION)
    			|| category.equalsIgnoreCase(DocumentTypes.RQNOTICE)
    			|| category.equalsIgnoreCase(DocumentTypes.MISCELLANEOUS)
    			|| category.equalsIgnoreCase(DocumentTypes.SUBSTITUTION)
    			|| category.equalsIgnoreCase(DocumentTypes.ASSUMPTION)
    			|| category.equalsIgnoreCase(DocumentTypes.RELEASE)) {
    		return true;
    	}
    	return false;
    }

    /**
     * Used to get a list of real transfer sub-categories based on state/county
     * @param stateId
     * @param countyId
     * @return the list
     */
	public static String[] getRealTransferSubcategories(int stateId, int countyId) {
		String[] realTransferSubcategories = null;
		
		switch (countyId) {
		case CountyConstants.IL_Cook:
			realTransferSubcategories = new String[]{
					DocumentTypes.TRANSFER, 
					DocumentTypes.RE_REC_TRANSFER,
					DocumentTypes.WARRANTY_DEED,
					DocumentTypes.DEED,
					"Conservators Deed",
					"Conserv Deed Tenancy Entirety",
					"Conserv Deed Joint Tenancy",
					"DEED IN TRUST",
					"Deed Joint Tenancy",
					"Deed By Entirety",
					"Executor's Deed",
					"Executor's Deed Joint Tenancy",
					"Executors Deed Ten By Entirety",
					"Guardians Deed",
					"Guardians Deed Joint Tenancy",
					"Guardians Deed Ten By Entirety",
					"Tax Deed",
					"Trustee's Deed",
					"Warranty Deed By Entirety",
					"Warranty Deed Joint Tenancy"
			};
			break;
		case CountyConstants.IL_Kane:
			realTransferSubcategories = new String[]{
					DocumentTypes.TRANSFER, 
					DocumentTypes.RE_REC_TRANSFER,
					DocumentTypes.WARRANTY_DEED,
					DocumentTypes.DEED,
					"Deed In Trust",
					"Tax Deed"
					
			};
			break;
		case CountyConstants.IL_Will:
			realTransferSubcategories = new String[]{
					DocumentTypes.TRANSFER, 
					DocumentTypes.RE_REC_TRANSFER,
					DocumentTypes.WARRANTY_DEED,
					DocumentTypes.DEED,
					"Special Warranty Deed",
					"Tax Deed",
					"Trustee's Deed"
			};
			break;
		default:
			break;
		}
		
		
		if(realTransferSubcategories == null) {
			switch (stateId) {
			case StateContants.TX:
				realTransferSubcategories = new String[]{
						DocumentTypes.TRANSFER, 
						DocumentTypes.RE_REC_TRANSFER,
						DocumentTypes.WARRANTY_DEED,
						DocumentTypes.DEED,
						DocumentTypes.VENDORS_LIEN};
				break;

			default:
				realTransferSubcategories = new String[]{
						DocumentTypes.TRANSFER, 
						DocumentTypes.RE_REC_TRANSFER,
						DocumentTypes.WARRANTY_DEED,
						DocumentTypes.DEED};
				break;
			}
		}
		
		return realTransferSubcategories;
	}
    
}
