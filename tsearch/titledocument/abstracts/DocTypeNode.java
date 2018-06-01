package ro.cst.tsearch.titledocument.abstracts;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;

import com.advantagetitlesearch.ats.services.ats2.DocumentTypeItem;
import com.stewart.ats.base.document.BoilerPlateObject;
import com.stewart.ats.base.document.BoilerPlateObject.BPType;

public class DocTypeNode {
	
	public static DocTypeNode allDocTypes = new DocTypeNode("", null, "");
	public static DocTypeNode allDocTypesTemp = new DocTypeNode("", null, "");
	public static HashMap<String, Vector<String>> categories = new HashMap<String, Vector<String>>();
	public static HashMap<String, HashMap<String, DocumentTypeItem>> cachedDocumentTypeForWS = null;
	
	public static int STATE			=	0;
	public static int COUNTY		=	1;
	public static int CATEGORY		=	2;
	public static int SUBCATEGORY	=	3;
	
	public static int ROOT			=	4;
	
	String name;
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	
	Set<String> bpcodes;
	
	public Set<String> getBpcodes() {
		return bpcodes;
	}
	public void setBpcodes(LinkedHashSet<String> bpcodes) {
		this.bpcodes = bpcodes;
	}
	
	private int copyOnMerge = -1;

	public int getCopyOnMerge() {
		return copyOnMerge;
	}
	public void setCopyOnMerge(int copyOnMerge) {
		this.copyOnMerge = copyOnMerge;
	}

	int type;
	public void setType(int type) {
		this.type = type;
	}
	public int getType() {
		return type;
	}
	
	int id;
	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return id;
	}
	
	DocTypeNode parent = null;
	public void setParent(DocTypeNode parent) {
		this.parent = parent;
	}
	public DocTypeNode getParent() {
		return parent;
	}
	
	Hashtable items = new Hashtable();
	public Object get(String key) {
		return items.get(key);
	}
	public void put(String key, Object value) {
		items.put(key, value);
	}
	public void put(String key) {
		items.put(key, "");
	}
	public void putAll(Map map) {
		items.putAll(map);
	}
	public Enumeration keys() {
		return items.keys();
	}
	public Set keySet() {
		return items.keySet();
	}
	public Hashtable getAll() {
		return items;
	}
	
	public DocTypeNode(String name, DocTypeNode parent, String type) {
		
		this.name = name;
		this.parent = parent;
		this.bpcodes = new LinkedHashSet<String>();
		
		if ("STATE".equals(type))
			this.type = STATE;
		else if ("COUNTY".equals(type))
			this.type = COUNTY;
		else if ("CATEGORY".equals(type))
			this.type = CATEGORY;
		else if ("SUBCATEGORY".equals(type))
			this.type = SUBCATEGORY;
		
		try {
		
			if ( this.type == STATE )
				id = State.getStateFromAbv( name ).getStateId().intValue();
			else if ( this.type == COUNTY )
				id = County.getCounty( name, parent.getName() ).getCountyId().intValue();
			else if ( this.type == CATEGORY )
				id = DocumentTypes.getCategoryID(name);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public DocTypeNode(DocTypeNode source) {
		
		this.name = source.name;
		this.parent = source.parent;
		
		this.type = source.type;
		this.id = source.id;
		this.bpcodes = new LinkedHashSet<String>();
		if(source.getBpcodes() != null) {
			bpcodes.addAll(source.getBpcodes());
		}
		this.copyOnMerge = source.copyOnMerge;
	}
	
	public static DocTypeNode getDocTypeNode(String stateKey) {
		
		return (DocTypeNode) allDocTypes.get( stateKey.toUpperCase() );
	}
			
	public static DocTypeNode getDocTypeNode(String stateKey, String countyKey) {
		
		String newKey = DBManager.getDocTypeCounty(stateKey + countyKey);
		if(newKey.length() < 2)
			return null;
		String newStateKey = newKey.substring(0, 2);
		String newCountyKey = newKey.substring(2, newKey.length());
		
		if(!newStateKey.equals(stateKey) || !newCountyKey.equals(countyKey)){
			stateKey = newStateKey;
			countyKey = newCountyKey;	
		}
		
		if(allDocTypes.getAll().isEmpty()) {
			//force doctype load - do not removed - we need to trigger instantiation on class DocumentTypes
			DocumentTypes.loadDocType();
		}
		
		DocTypeNode state = (DocTypeNode) allDocTypes.get( stateKey.toUpperCase() );
		
		if ( state != null ) {
			
			DocTypeNode county = (DocTypeNode) state.get( countyKey.toUpperCase() );
			
			if ( county != null )
				return county;
		}
		
		state = (DocTypeNode) allDocTypes.get( "XX" );
		if ( state != null ) {
			DocTypeNode county = (DocTypeNode) state.get( "DEFAULT" );
			if ( county != null )
				return county;
		}

		return null;
	}
	
	public static DocTypeNode getState(State state) {
		
		DocTypeNode node = DocTypeNode.getDocTypeNode( state.getStateAbv() );
		
		return node;
	}

	public static DocTypeNode getCounty(County county) {
		
		DocTypeNode node = DocTypeNode.getDocTypeNode( county.getState().getStateAbv(), county.getName() );
		
		return node;
	}
	
	public static DocTypeNode getCategory(County county, String docType) {
		
		if ( county != null && county.getState() != null ) {

			// cautam nodul cu State/County
			DocTypeNode node = DocTypeNode.getDocTypeNode( county.getState().getStateAbv(), county.getName() );
	        
			if (node != null) {
	        
				// iteram peste categorii
				Hashtable allCategories = (Hashtable) node.getAll();
		        Enumeration e = allCategories.keys();
		        while ( e.hasMoreElements() ) {
		        	
		        	String key = (String) e.nextElement();
		        	
		        	DocTypeNode category = (DocTypeNode) allCategories.get(key);
		        	
		        	DocTypeNode allDocTypes = (DocTypeNode) category.get("ALL");
		        	if ( allDocTypes != null ) {
		        		
		        		// cautam categoria care are docType-ul respectiv
		        		if ( allDocTypes.get( docType ) != null )
		        			return category;
		        	}
		        }
			}
		}
        
        return null;
	}
	
	public static ArrayList<String> getSubcategoryForCategory(County county,final String cat11) {

		ArrayList<String> ret = new ArrayList<String>();
		//try / catch to catch all possible errors
		try{
			// cautam nodul cu State/County
			DocTypeNode node = DocTypeNode.getDocTypeNode( county.getState().getStateAbv(), county.getName() );
	        
			if (node != null) {
	        
				// iteram peste categorii
				Hashtable allCategories = (Hashtable) node.getAll();
		        Enumeration e = allCategories.keys();
		        while ( e.hasMoreElements() ) {
		        	
		        	String key = (String) e.nextElement();
		        	
		        	if ("ALL".equals(key))
						continue;
		        	if(cat11.equals(key)||cat11.toUpperCase().equals(key)){
			        	DocTypeNode category = (DocTypeNode) allCategories.get(key);
			        	DocTypeNode allDocTypes = (DocTypeNode) category.get("ALL");
			        	if ( allDocTypes != null ) {
			        		
			        		// cautam categoria care are docType-ul respectiv
			        			// iteram peste subcategorii
			        			Hashtable allSubcategories = (Hashtable) category.getAll();
			        			e = allSubcategories.keys();
			        			
			        			while ( e.hasMoreElements() ) {
			        				
			        				key = (String) e.nextElement();
			        				
			        				if(!"ALL".equalsIgnoreCase(key)){
			        					ret.add(key);
			        				}
			        				
			        			}
			        	}
			        }
		        }
			}
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		
        return ret;
	}
	
	public static DocTypeNode getSubcategory(County county, String docType) {
		
		//try / catch to catch all possible errors
		try{
			// cautam nodul cu State/County
			DocTypeNode node = DocTypeNode.getDocTypeNode( county.getState().getStateAbv(), county.getName() );
	        
			if (node != null) {
	        
				// iteram peste categorii
				Hashtable allCategories = (Hashtable) node.getAll();
		        Enumeration e = allCategories.keys();
		        while ( e.hasMoreElements() ) {
		        	
		        	String key = (String) e.nextElement();
		        	
		        	if ("ALL".equals(key))
						continue;
		        	
		        	DocTypeNode category = (DocTypeNode) allCategories.get(key);
		        	
		        	DocTypeNode allDocTypes = (DocTypeNode) category.get("ALL");
		        	if ( allDocTypes != null ) {
		        		
		        		// cautam categoria care are docType-ul respectiv
		        		if ((allDocTypes.get( docType ) != null) || (allDocTypes.get( docType.toUpperCase() ) != null)) {
		        			
		        			// iteram peste subcategorii
		        			Hashtable allSubcategories = (Hashtable) category.getAll();
		        			e = allSubcategories.keys();
		        			
		        			while ( e.hasMoreElements() ) {
		        				
		        				key = (String) e.nextElement();
		        				
		        				if(!"ALL".equalsIgnoreCase(key)){
			        				DocTypeNode subcategory = (DocTypeNode) allSubcategories.get(key);
			        				
			        				Hashtable docTypes = subcategory.getAll();
			        				
			        				if ( (docTypes.get( docType ) != null) || (docTypes.get( docType.toUpperCase() ) != null)) {
			        					
			        					return subcategory;
			        				}
		        				}
		        				
		        			}
		        		}
		        	}
		        }
			}
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		
        return null;
	}
	
	public static DocTypeNode getSubcategoryNode(County county, String category, String subcategory) {
		
		//try / catch to catch all possible errors
		try{
			// cautam nodul cu State/County
			DocTypeNode node = DocTypeNode.getDocTypeNode( county.getState().getStateAbv(), county.getName() );
	        
			if (node != null) {
	        
				// iteram peste categorii
				Hashtable allCategories = (Hashtable) node.getAll();
		        Enumeration e = allCategories.keys();
		        while ( e.hasMoreElements() ) {
		        	
		        	String key = (String) e.nextElement();
		        	
		        	if (!key.equals(category))
						continue;
		        	
		        	DocTypeNode categoryNode = (DocTypeNode) allCategories.get(key);
		        	
		        	DocTypeNode subcategoryNode = (DocTypeNode)categoryNode.get(subcategory);
		        	
		        	if(subcategoryNode != null) {
        				return subcategoryNode;
        			}
		        	
		        }
			}
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		
        return null;
	}
	
	public String toString() {
		return (parent != null ? parent.toString() + "/" : "") + name; 
	}
	/**
	 * Parses the given string, splits using "|" separator and adds each token to current list on this node
	 * @param bpcodesAsString list of tokens separated by "|"
	 */
	public void addBpCodes(String bpcodesAsString) {
		if(this.bpcodes == null) {
			this.bpcodes = new LinkedHashSet<String>();
		}
		if(StringUtils.isNotBlank(bpcodesAsString)) {
			String[] codes = bpcodesAsString.split("\\|");
			for (String code : codes) {
				bpcodes.add(code);
			}
		}
	}
	/**
	 * Copies the given codes to current list on this node
	 * @param bpcodes set of codes to be added
	 */
	public void addBpCodes(Set<String> bpcodes) {
		if(this.bpcodes == null) {
			this.bpcodes = new LinkedHashSet<String>();
		}
		if(bpcodes != null) {
			this.bpcodes.addAll(bpcodes);
		}
	}
//	/**
//	 * Adds {@link BoilerPlateObject}s to the given document and also sets the type CATEGORY/SUBCATEGORY
//	 * @param document
//	 */
//	public void fillDocument(DocumentI document) {
//		if(getType() == SUBCATEGORY) {
//			if(getBpcodes() != null) {
//				for (String bpcode : getBpcodes()) {
//					BoilerPlateObject boilerPlateObject = new BoilerPlateObject(bpcode, BPType.SUBCATEGORY);
//					document.addBPCode(boilerPlateObject);
//				}
//			}
//			if(getParent() != null) {
//				getParent().fillDocument(document);
//			}
//		} else if(getType() == CATEGORY) {
//			if(getBpcodes() != null) {
//				for (String bpcode : getBpcodes()) {
//					BoilerPlateObject boilerPlateObject = new BoilerPlateObject(bpcode, BPType.CATEGORY);
//					document.addBPCode(boilerPlateObject);
//				}
//			}
//		}
//	}
	/**
	 * Adds {@link BoilerPlateObject}s to the given map and also sets the type CATEGORY/SUBCATEGORY
	 * @param boilerPlateCodeMap
	 */
	public void fillCodes(Map<String, BoilerPlateObject> boilerPlateCodeMap) {
		if(getType() == SUBCATEGORY) {
			if(getBpcodes() != null) {
				for (String bpcode : getBpcodes()) {
					BoilerPlateObject boilerPlateObject = new BoilerPlateObject(bpcode, BPType.SUBCATEGORY);
					boilerPlateCodeMap.put(boilerPlateObject.getBpCode(), boilerPlateObject);
				}
			}
			if(getParent() != null) {
				getParent().fillCodes(boilerPlateCodeMap);
			}
		} else if(getType() == CATEGORY) {
			if(getBpcodes() != null) {
				for (String bpcode : getBpcodes()) {
					BoilerPlateObject boilerPlateObject = new BoilerPlateObject(bpcode, BPType.CATEGORY);
					boilerPlateCodeMap.put(boilerPlateObject.getBpCode(), boilerPlateObject);
				}
			}
		}
	}
	
	/**
	 * Some subcategories are allowed to be copied when merging with a prior/base search
	 * @return true only if the subcategory is allowed to be copied
	 */
	public boolean allowCopyOnMerge() {
		int allow = -1;
		if(getType() == SUBCATEGORY) {
			allow = getCopyOnMerge();
			if(allow == -1) {
				if(getParent() != null) {
					allow = getParent().getCopyOnMerge();
				}
			}
		} else if (getType() == CATEGORY) {
			allow = getCopyOnMerge();
		}
		if(allow > 0) {
			return true;
		}
		return false;
	}
}
